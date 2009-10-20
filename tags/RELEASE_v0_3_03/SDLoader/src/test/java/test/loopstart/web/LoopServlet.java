/*
 * Copyright 2005-2009 the original author or authors.
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
package test.loopstart.web;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.Assert;

public class LoopServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private String n;

	@Override
	public void init() throws ServletException {
		n = "hogehoge";
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		Assert.assertEquals(n, "hogehoge");
		resp.getWriter().write(
				req.getParameter("loop") + req.getAttribute("filter"));

	}

	@Override
	public void destroy() {
		Assert.assertEquals(n, "hogehoge");
		n = null;
	}
}
