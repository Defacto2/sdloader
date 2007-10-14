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
package sdloader.j2ee.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import sdloader.http.HttpConst;
import sdloader.j2ee.WebApplication;
import sdloader.j2ee.imp.ServletContextImp;
import sdloader.j2ee.webxml.ServletMappingTag;
import sdloader.j2ee.webxml.WelcomeFileListTag;
import sdloader.util.WebUtils;

/**
 * ファイル出力サーブレット リクエストパスからファイルを検索し、返します。
 * 
 * @author c9katayama
 */
public class FileSavingServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public static final String PARAM_DOC_ROOT = "docRootPath";

	/**
	 * ドキュメントルート（絶対パス）
	 */
	private String docRootPath;

	private Map mimeTypeMap = new HashMap();

	private WelcomeFileListTag welcomeFileListTag ;
	
	public FileSavingServlet() {
		super();

	}

	public void init() throws ServletException {
		this.docRootPath = getInitParameter(PARAM_DOC_ROOT);
		if (docRootPath == null)
			throw new ServletException("InitParameter [docRootPath] not found.");

		// load mimetype
		initMime();
		
		ServletContextImp servletContext = (ServletContextImp)getServletContext();
		WebApplication app = servletContext.getWebApplication();
		welcomeFileListTag = app.getWebXml().getWebApp().getWelcomeFileList();
	}

	/**
	 * mimeTypeを初期化します。
	 * 
	 * @throws ServletException
	 */
	protected void initMime() throws ServletException {
		InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("sdloader/resource/mime.xml");
		if (is == null)
			throw new ServletException("mime.xml not found.");

		try {
			SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
			parser.parse(is, new MimeParseHandler(this));
		} catch (Exception se) {
			throw new ServletException("Mime parse fail. " + se.getMessage());
		}
	}

	public void addMimeType(String ext, String type) {
		this.mimeTypeMap.put(ext, type);
	}

	protected void doGet(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException {
		doIt(req, res);
	}

	protected void doPost(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException {
		doIt(req, res);
	}

	protected void doIt(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException {
		String uri = req.getPathInfo();
		
		if(uri==null){
			res.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		
		if(uri.startsWith("/WEB-INF/") || uri.endsWith("/WEB-INF")){
			res.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		
		String realPath = this.docRootPath + uri;

		File fileOrDir = new File(realPath);
		if (!fileOrDir.exists()) {
			res.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		if (fileOrDir.isFile()) {
			File file = fileOrDir;
			outputFile(file, req, res);
			return;
		} else{
			if(welcomeFileListTag != null) {
				File dir = fileOrDir;
				processWelcomeFile(dir,req,res);
				return;
			}else{
				res.setStatus(HttpServletResponse.SC_NOT_FOUND);
				return;
			}
		}
	}
	/**
	 * welcomeファイルの処理
	 * welcomeファイルリストのパスに対して、パターンが完全一致するサーブレット
	 * かファイルを検索し、見つかった場合forwardします。
	 * @param dir
	 * @param req
	 * @param res
	 * @throws ServletException
	 * @throws IOException
	 */
	protected void processWelcomeFile(File dir,HttpServletRequest req,HttpServletResponse res) throws ServletException, IOException{
		String basePath = req.getPathInfo();
		if(!basePath.endsWith("/"))
			basePath += "/";
		
		ServletContextImp context = (ServletContextImp)getServletContext();
		WebApplication webapp = context.getWebApplication();
		List welcomeFileList = welcomeFileListTag.getWelcomeFile();
		List servletMappingList = webapp.getWebXml().getWebApp().getServletMapping();
		
		for(Iterator itr = welcomeFileList.iterator();itr.hasNext();){
			String  welcomefileName = (String)itr.next();
			String path = basePath+welcomefileName;
			//find servlet mapping
			for(Iterator mappingItr = servletMappingList.iterator();mappingItr.hasNext();){
				ServletMappingTag mappingTag = (ServletMappingTag)mappingItr.next();				
				int matchType = WebUtils.matchPattern(mappingTag.getUrlPattern(),path);
				if(matchType==WebUtils.PATTERN_EXACT_MATCH){//完全
					context.getRequestDispatcher(path).forward(req,res);
					return;
				}
			}
			//find file
			File welcomeFile = new File(dir,welcomefileName);
			if(welcomeFile.exists() && welcomeFile.isFile()){
				context.getRequestDispatcher(path).forward(req,res);
				return;
			}
		}
		res.setStatus(HttpServletResponse.SC_NOT_FOUND);
		return;
	}
	/**
	 * ファイル出力
	 * @param file
	 * @param req
	 * @param res
	 * @throws IOException
	 */
	protected void outputFile(File file,HttpServletRequest req,HttpServletResponse res) throws IOException{
		Date lastModifyDate = new Date(file.lastModified());
		res.setHeader(HttpConst.LASTMODIFIED,
				WebUtils.formatHeaderDate(lastModifyDate));
		String ifModified = req.getHeader(HttpConst.IFMODIFIEDSINCE);
		// 変更したかどうか
		if (!isModifiedSince(lastModifyDate, ifModified)) {
			res.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
			return;
		}

		FileInputStream fin = new FileInputStream(file);
		ServletOutputStream sout = res.getOutputStream();
		try {
			int size = WebUtils.copyStream(fin, sout);
			setContentType(res, file);
			res.setContentLength(size);
			res.setStatus(HttpServletResponse.SC_OK);
		} finally {
			fin.close();
			sout.flush();
			sout.close();
		}	
	}

	/**
	 * ファイルを変更したかどうか
	 * 
	 * @param fileLastModify
	 * @param ifModifiedSince
	 * @return 変更していた場合true,していない場合false
	 */
	protected boolean isModifiedSince(Date fileLastModify,
			String ifModifiedSince) {
		if (ifModifiedSince == null)
			return true;
		try {
			long ifModifiedTime = WebUtils.parseHeaderDate(ifModifiedSince.trim());
			long lastModifyTime = fileLastModify.getTime();
			if (lastModifyTime <= ifModifiedTime)
				return false;
			else
				return true;
		} catch (NumberFormatException ne) {
			return true;
		} catch (ParseException e) {
			return true;
		}
	}

	protected void setContentType(HttpServletResponse res, File file) {
		String name = file.getName();
		int dotIndex = name.lastIndexOf(".");
		if (dotIndex >= 0) {
			try {
				String ext = name.substring(dotIndex + 1);
				String type = (String) mimeTypeMap.get(ext);
				if (type == null)
					type = (String) mimeTypeMap.get(ext.toLowerCase());
				if (type == null)
					type = (String) mimeTypeMap.get(ext.toUpperCase());

				if (type != null) {
					res.setContentType(type);
					return;
				}
			} catch (Exception e) {
				// ignore
			}
		}
		// default
		res.setContentType("text/html");
	}
}