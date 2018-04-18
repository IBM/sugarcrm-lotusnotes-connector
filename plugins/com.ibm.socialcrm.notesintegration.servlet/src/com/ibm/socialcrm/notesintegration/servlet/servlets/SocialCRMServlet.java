package com.ibm.socialcrm.notesintegration.servlet.servlets;

/****************************************************************
 * IBM OpenSource
 *
 * (C) Copyright IBM Corp. 2012
 *
 * Licensed under the Apache License v2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 ***************************************************************/

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SocialCRMServlet extends HttpServlet {
	/**
	 * Serial UID
	 */
	private static final long serialVersionUID = -2141375946112563556L;

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doPost(request, response);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		System.out.println(request.getRequestURL());

		response.setStatus(200);
		String responseStr = "Replace me with some code that actually does something useful";
		OutputStream out = response.getOutputStream();
		out.write(responseStr.getBytes());

		out.flush();
		out.close();

	}

}
