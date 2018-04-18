package com.ibm.socialcrm.notesintegration.servlet.servlets;

/****************************************************************
 * IBM Confidential
 *
 * SFA050-Collaboration Source Materials
 *
 * (C) Copyright IBM Corp. 2012
 *
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has been
 * deposited with the U.S. Copyright Office
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
