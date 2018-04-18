package com.ibm.socialcrm.notesintegration.sfawebapi;

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

import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;

public class Services extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5370217393752510453L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		String format = request.getParameter("format"); //$NON-NLS-1$
		String responseStr = ""; //$NON-NLS-1$
		String jsonPrefix = "{\"version\":\"1.0\",\"services\": ["; //$NON-NLS-1$
		String jsonSuffix = "]}"; //$NON-NLS-1$
		System.out.println("In Services servlet " + request.getRequestURL());
		if (format != null && format.equals("json")) { //$NON-NLS-1$
			responseStr += jsonPrefix;
			responseStr += "{ \"service\":\"createMeeting\", \"url\":\"https://localhost:" + ConstantStrings.HTTP_PROXY_PORT + "/sfawebapi/createMeeting\"},"; //$NON-NLS-1$  //$NON-NLS-2$
			responseStr += "{ \"service\":\"createEmail\", \"url\":\"https://localhost:" + ConstantStrings.HTTP_PROXY_PORT + "/sfawebapi/createEmail\"}"; //$NON-NLS-1$  //$NON-NLS-2$
			responseStr += jsonSuffix;
			response.setContentType("application/json"); //$NON-NLS-1$
		} else if (format != null && format.equals("jsonp")) { //$NON-NLS-1$
			String callback = request.getParameter("callback"); //$NON-NLS-1$
			responseStr += callback + "(" + jsonPrefix; //$NON-NLS-1$
			responseStr += "{ \"service\":\"createMeeting\", \"url\":\"https://localhost:" + ConstantStrings.HTTP_PROXY_PORT + "/sfawebapi/createMeeting\"},"; //$NON-NLS-1$  //$NON-NLS-2$
			responseStr += "{ \"service\":\"createEmail\", \"url\":\"https://localhost:" + ConstantStrings.HTTP_PROXY_PORT + "/sfawebapi/createEmail\"}"; //$NON-NLS-1$  //$NON-NLS-2$
			responseStr += jsonSuffix + ");"; //$NON-NLS-1$
			response.setContentType("application/json"); //$NON-NLS-1$
		} else {
			// return html
			responseStr += "<html><head><title>SFA WebAPI</title><body>" //$NON-NLS-1$
					+ "<table border=\"1\" cellpadding=\"10\"><tr>" //$NON-NLS-1$
					+ "<td><b>Service</b></td><td><b>Link</b></td><td><b>Description</b></td></tr>" //$NON-NLS-1$
					+ "<tr>" //$NON-NLS-1$
					+ "<td>createMeeting</td><td><a href=\"http://localhost:" //$NON-NLS-1$
					+ request.getLocalPort()
					+ "/sfawebapi/createMeeting\">Link</a></td><td>Service that creates a meeting entry in the Notes calendar.  Requires the sugarid of the related item(s) in the \"relatedId\" parameter, and one of \"Accounts\", \"Contacts\", \"Opportunities\" or \"Leads\" in the \"module\" parameter.</td>" //$NON-NLS-1$
					+ "</tr>" //$NON-NLS-1$
					+ "<tr>" //$NON-NLS-1$
					+ "<td>createEmail</td><td><a href=\"http://localhost:" //$NON-NLS-1$
					+ request.getLocalPort()
					+ "/sfawebapi/createEmail\">Link</a></td><td>Service that creates an email in the Notes mail.  Requires the sugarid of the related item(s) in the \"relatedId\" parameter, and one of \"Accounts\", \"Contacts\", \"Opportunities\"  or \"Leads\" in the \"module\" parameter.</td>" //$NON-NLS-1$
					+ "</tr>" //$NON-NLS-1$
					+ "</table></body></html>"; //$NON-NLS-1$
			response.setContentType("text/html"); //$NON-NLS-1$
		}

		response.setStatus(200);
		System.out.println("generated output:" + responseStr);
		OutputStream out = response.getOutputStream();
		out.write(responseStr.getBytes());
		out.flush();
		out.close();
	}
}
