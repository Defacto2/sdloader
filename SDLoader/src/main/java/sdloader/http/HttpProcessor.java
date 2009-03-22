/*
 * Copyright 2005-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package sdloader.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Calendar;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;

import sdloader.SDLoader;
import sdloader.constants.LineSpeed;
import sdloader.javaee.InternalWebApplication;
import sdloader.javaee.ServletMapping;
import sdloader.javaee.constants.JavaEEConstants;
import sdloader.javaee.impl.FilterChainImpl;
import sdloader.javaee.impl.HttpServletRequestImpl;
import sdloader.javaee.impl.HttpServletResponseImpl;
import sdloader.javaee.impl.ServletContextImpl;
import sdloader.log.SDLoaderLog;
import sdloader.log.SDLoaderLogFactory;
import sdloader.util.FastByteArrayOutputStream;
import sdloader.util.IOUtil;
import sdloader.util.QoSInputStream;
import sdloader.util.QoSOutputStream;
import sdloader.util.WebUtil;

/**
 * ソケット接続に対して、処理を行います. リクエスト解析＞サーブレット呼び出し＞レスポンスの順に 処理を行います。
 * 
 * @author c9katayama
 */
public class HttpProcessor extends Thread {
	private static final SDLoaderLog log = SDLoaderLogFactory
			.getLog(HttpProcessor.class);

	private int socketTimeout = 60 * 1000;

	private int keepAliveTimeout = 15 * 1000;// Apache 15

	private int keppAliveMaxRequests = 100;// Apache 100

	private Socket socket;

	private SDLoader sdLoader;

	private int lineSpeed;

	private boolean stop;

	private boolean started;

	private boolean wait = true;

	public HttpProcessor(String name) {
		super(name);
		setDaemon(true);
	}

	public void setSocketTimeout(int socketTimeout) {
		this.socketTimeout = socketTimeout;
	}

	public void setKeepAliveTimeout(int keepAliveTimeout) {
		this.keepAliveTimeout = keepAliveTimeout;
	}

	public void setKeppAliveMaxRequests(int keppAliveMaxRequests) {
		this.keppAliveMaxRequests = keppAliveMaxRequests;
	}

	public void process(Socket socket, SDLoader loader) {
		synchronized (this) {
			this.socket = socket;
			this.sdLoader = loader;
			lineSpeed = sdLoader.getSDLoaderConfig().getConfigInteger(
					SDLoader.KEY_SDLOADER_LINE_SPEED, LineSpeed.NO_LIMIT);
			if (started) {
				notify();
			} else {
				wait = false;
			}
		}
	}

	void stopProcessor() {
		synchronized (this) {
			stop = true;
			if (started) {
				notify();
			} else {
				wait = false;
			}
		}
	}

	public void run() {
		while (!stop) {
			try {
				synchronized (this) {
					started = true;
					if (wait) {
						wait();
					}
					wait = true;
				}
			} catch (InterruptedException e) {
				continue;
			}
			if (stop) {
				return;
			}
			processSocket();
		}
	}

	protected void processSocket() {
		InputStream is = null;
		OutputStream os = null;
		try {
			socket.setTcpNoDelay(true);
			is = socket.getInputStream();
			os = socket.getOutputStream();
			int requestCount = 1;
			while (true) {
				ProcessScopeContext.init();
				try {
					ProcessScopeContext.getContext().setAttribute(
							SDLoader.class, sdLoader);
					int timeout = (requestCount == 1) ? socketTimeout
							: keepAliveTimeout;
					socket.setSoTimeout(timeout);
					HttpRequest httpRequest = processReadRequest(is);
					processServlet(httpRequest, os, requestCount);
					if (httpRequest.getHeader().isKeepAlive()) {
						requestCount++;
					} else {
						break;
					}
				} finally {
					ProcessScopeContext.destroy();
				}
			}
		} catch (SocketTimeoutException e) {
			log.debug("socket timeout.");
		} catch (SocketException e) {
			log.debug("socket close.");
		} catch (Throwable t) {
			log.error(t.getMessage(), t);
		} finally {
			IOUtil.flushNoException(os);
			IOUtil.closeNoException(is);
			IOUtil.closeNoException(os);
			IOUtil.closeSocketNoException(socket);
		}
		is = null;
		os = null;
		socket = null;
		SDLoader localLoader = this.sdLoader;
		sdLoader = null;
		localLoader.returnProcessor(this);
	}

	protected HttpRequest processReadRequest(InputStream is) throws IOException {
		try {
			HttpRequestReader reader = new HttpRequestReader(
					createInputStream(is));
			HttpRequest httpRequest = new HttpRequest(reader);
			log.debug("request read start.");
			httpRequest.readRequest();
			log.debug("request read success.");
			if (log.isDebugEnabled()) {
				log.debug("<REQUEST_HEADER>\n"
						+ httpRequest.getHeader().getRequestHeader());
			}
			return httpRequest;
		} finally {
			if (socket.isClosed()) {
				throw new SocketException();
			} else {
				socket.setSoTimeout(socketTimeout);
			}
		}
	}

	protected InputStream createInputStream(InputStream is) {
		if (lineSpeed <= LineSpeed.NO_LIMIT) {
			return is;
		} else {
			return new QoSInputStream(is, lineSpeed);
		}
	}

	protected void processServlet(HttpRequest httpRequest, OutputStream os,
			int requestCount) throws Throwable {

		HttpHeader header = httpRequest.getHeader();
		String requestURI = header.getRequestURI();
		InternalWebApplication webapp = sdLoader.getWebAppManager().findWebApp(
				requestURI);
		HttpServletRequestImpl request = createServletRequestImp(httpRequest,
				webapp);
		HttpServletResponseImpl response = new HttpServletResponseImpl();
		ServletContextImpl servletContextImpl = webapp.getServletContext();

		String contextPath = webapp.getContextPath();
		String resourcePath = WebUtil.getResourcePath(contextPath, requestURI);
		// contextpathだけのパターン (/testのようなパターン）の場合、contextpathに/をつけてリダイレクト
		if (!requestURI.equals("/") && resourcePath == null) {
			response.setStatus(HttpConst.SC_MOVED_TEMPORARILY);
			resourcePath = requestURI + "/";
			String host = request.getHeader(HttpConst.HOST);
			if (host == null) {
				host = request.getLocalName() + ":" + request.getLocalPort();
			}
			response.addHeader(HttpConst.LOCATION, WebUtil.buildRequestURL(
					request.getScheme(), host, resourcePath).toString());
			setDefaultResponseHeader(servletContextImpl, request, response,
					requestCount);
			processRequestEnd(response, os);
			return;
		}

		ServletMapping mapping = webapp.findServletMapping(resourcePath);
		if (mapping == null) {
			WebUtil.writeNotFoundPage(response);
			setDefaultResponseHeader(servletContextImpl, request, response,
					requestCount);
			processRequestEnd(response, os);
			return;
		}

		Servlet servlet = webapp.findServlet(mapping.getServletName());
		request.setServletPath(WebUtil.getServletPath(mapping.getUrlPattern(),
				resourcePath));
		request.setPathInfo(WebUtil.getPathInfo(mapping.getUrlPattern(),
				resourcePath));

		// class loader
		ClassLoader webClassLoader = webapp.getWebAppClassLoader();
		ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(webClassLoader);
			ProcessScopeContext processScopeContext = ProcessScopeContext
					.getContext();
			processScopeContext.setRequest(request);
			processScopeContext.setResponse(response);
			request.intoScope();
			// service
			try {
				String servletName = mapping.getServletName();
				List<Filter> filterList = webapp.findFilters(resourcePath,
						servletName, JavaEEConstants.DISPATCHER_TYPE_REQUEST);
				if (filterList.size() > 0) {
					Filter[] filters = (Filter[]) filterList
							.toArray(new Filter[] {});
					FilterChainImpl filterChain = new FilterChainImpl(filters,
							servlet);
					filterChain.doFilter(request, response);
				} else {
					servlet.service(request, response);
				}
			} catch (ServletException se) {
				log.error(se.getMessage(), se);
				WebUtil.writeInternalServerErrorPage(request,response,se);
			} catch (IOException ioe) {
				log.error(ioe.getMessage(), ioe);
				WebUtil.writeInternalServerErrorPage(request,response,ioe);
			}
			setDefaultResponseHeader(servletContextImpl, request, response,
					requestCount);
			processRequestEnd(response, os);
		} finally {
			request.destroy();
			Thread.currentThread().setContextClassLoader(oldLoader);
		}
	}

	private HttpServletRequestImpl createServletRequestImp(
			HttpRequest httpRequest, InternalWebApplication webApp) {
		HttpServletRequestImpl request = new HttpServletRequestImpl(
				httpRequest, webApp, sdLoader.getSessionManager());

		request.setServerPort(sdLoader.getPort());
		request.setLocalPort(socket.getLocalPort());
		request.setLocalAddr(socket.getLocalAddress().getHostAddress());
		request.setLocalName(socket.getLocalAddress().getHostName());

		request.setRemotePort(socket.getPort());
		request.setRemoteAddr(socket.getInetAddress().getHostAddress());
		request.setRemoteHost(socket.getInetAddress().getHostName());

		request.setScheme("http");

		String uriEncoding = sdLoader.getSDLoaderConfig()
				.getConfigStringIgnoreExist(
						HttpRequest.KEY_REQUEST_URI_ENCODING);
		if (uriEncoding != null) {
			request.setUriEncoding(uriEncoding);
		}
		return request;
	}

	protected void setDefaultResponseHeader(
			ServletContextImpl servletContextImpl,
			HttpServletRequestImpl request, HttpServletResponseImpl response,
			int requestCount) throws IOException {
		response.setHeader(HttpConst.DATE, WebUtil.formatHeaderDate(Calendar
				.getInstance().getTime()));
		response.setHeader(HttpConst.SERVER, sdLoader.getServerName());

		// session
		HttpSession session = request.getSession(false);
		if (session != null && servletContextImpl != null) {
			String sessionId = session.getId();
			Cookie sessionCookie = new Cookie(HttpConst.SESSIONID_KEY,
					sessionId);
			sessionCookie.setPath(servletContextImpl.getContextPath());
			response.addCookie(sessionCookie);
		}

		// Keep-Alive
		if (request.getHeader().isKeepAlive()
				&& requestCount < keppAliveMaxRequests) {
			response.addHeader(HttpConst.KEEPALIVE, "timeout="
					+ (int) keepAliveTimeout / 1000 + ", max="
					+ keppAliveMaxRequests);
			response.addHeader(HttpConst.CONNECTION, HttpConst.KEEPALIVE);
		} else {
			response.addHeader(HttpConst.CONNECTION, HttpConst.CLOSE);
		}
		// Cache Control
		if (sdLoader.getSDLoaderConfig().getConfigBoolean(
				HttpResponse.KEY_RESPONSE_USE_NOCACHE_MODE, false)) {
			response.setHeader("Pragma", "no-cache");
			response.setDateHeader("Expires", 1L);
			response.setHeader("Cache-Control", "no-cache");
			response.addHeader("Cache-Control", "no-store");
		}

		// Content-Length
		// Chunked以外はセット
		HttpHeader resHeader = response.getResponseHeader();
		String transferEncoding = resHeader
				.getHeaderValue(HttpConst.TRANSFERENCODING);
		if (transferEncoding == null
				|| !transferEncoding.equalsIgnoreCase(HttpConst.CHUNKED)) {
			response.setHeader(HttpConst.CONTENTLENGTH, String.valueOf(response
					.getBodySize()));
		}
	}

	protected void processRequestEnd(HttpServletResponseImpl response,
			OutputStream os) throws IOException {
		HttpHeader resHeader = response.getResponseHeader();
		os = createOutputStream(os);

		byte[] headerData = resHeader.buildResponseHeader().getBytes();
		os.write(headerData);
		if (log.isDebugEnabled()) {
			log.debug("<RESPONSE_HEADER>\n" + new String(headerData));
		}
		os.write(HttpConst.CRLF_STRING.getBytes());// Separator
		FastByteArrayOutputStream bodyData = response.getBodyData();
		if (bodyData != null) {
			bodyData.writeTo(os);
		}
		os.flush();
	}

	protected OutputStream createOutputStream(OutputStream out) {
		if (lineSpeed <= LineSpeed.NO_LIMIT) {
			return out;
		} else {
			return new QoSOutputStream(out, lineSpeed);
		}
	}
}
