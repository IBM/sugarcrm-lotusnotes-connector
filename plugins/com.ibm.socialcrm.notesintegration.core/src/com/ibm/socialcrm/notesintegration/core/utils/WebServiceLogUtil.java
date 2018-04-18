package com.ibm.socialcrm.notesintegration.core.utils;

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

import java.io.ByteArrayOutputStream;

import org.apache.abdera.protocol.client.ClientResponse;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.FileRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.json.JSONObject;

import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;

public class WebServiceLogUtil {

	public static String getDebugMsg(PostMethod post, String output, String uri, int maxRetry, int currRetryNum) {
		return getDebugMsg(post, null, output, uri, maxRetry, currRetryNum);
	}

	public static String getDebugMsg(PostMethod post, Part[] parts, String output, String uri, int maxRetry, int currRetryNum) {

		StringBuffer sb = new StringBuffer(ConstantStrings.EMPTY_STRING);

		// ========================================================
		sb.append(ConstantStrings.NEW_LINE).append("*************** web service uri: ").append((uri == null ? ConstantStrings.EMPTY_STRING //$NON-NLS-1$  
				: uri));
		if (maxRetry > 0) {
			sb.append(ConstantStrings.NEW_LINE).append("(This is retry #").append(String.valueOf(currRetryNum)) //$NON-NLS-1$  
					.append(", Max. retry # allowed : ").append(String.valueOf(maxRetry)).append(")"); //$NON-NLS-1$ //$NON-NLS-2$
		}

		if (post != null) {

			// ========================================================
			NameValuePair[] requests = post.getParameters();
			if (requests != null && requests.length > 0) {
				sb.append(ConstantStrings.NEW_LINE).append("PARAMETERS ..."); //$NON-NLS-1$  

				for (int i = 0; i < requests.length; i++) {
					NameValuePair modifiedNameValuePair = new NameValuePair(requests[i].getName(), requests[i].getValue());
					if (modifiedNameValuePair.getName() != null && modifiedNameValuePair.getName().equalsIgnoreCase("password")) //$NON-NLS-1$
					{
						// As a warning, if password is null or blank, display ##NULL## or ##BLANK##
						if (modifiedNameValuePair.getValue() == null) {
							modifiedNameValuePair.setValue("##NULL##"); //$NON-NLS-1$
						} else if (modifiedNameValuePair.getValue().equals(ConstantStrings.EMPTY_STRING) || modifiedNameValuePair.getValue().equals(ConstantStrings.SPACE)) {
							modifiedNameValuePair.setValue("##BLANK##"); //$NON-NLS-1$
						} else {

							modifiedNameValuePair.setValue("######"); //$NON-NLS-1$
						}
					}
					sb.append(ConstantStrings.NEW_LINE).append(ConstantStrings.SPACE).append(ConstantStrings.SPACE).append(modifiedNameValuePair);
				}
			} else {
				RequestEntity requestEntity = post.getRequestEntity();
				if (requestEntity != null && requestEntity instanceof StringRequestEntity) {
					sb.append(ConstantStrings.NEW_LINE).append("STRINGREQUESTENTITY ..."); //$NON-NLS-1$  

					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					try {
						((StringRequestEntity) requestEntity).writeRequest(baos);

						if (baos != null) {
							String[] s1 = baos.toString().split(ConstantStrings.AMPERSAND);
							for (int i = 0; i < s1.length; i++) {
								String name = ConstantStrings.EMPTY_STRING;
								String value = ConstantStrings.EMPTY_STRING;
								String[] s2 = s1[i].split(ConstantStrings.EQUALS);
								name = s2[0];
								value = s2[1];
								NameValuePair modifiedNameValuePair = new NameValuePair(name, value);
								if (modifiedNameValuePair.getName() != null && modifiedNameValuePair.getName().equalsIgnoreCase("password")) //$NON-NLS-1$
								{
									modifiedNameValuePair.setValue("######"); //$NON-NLS-1$
								}
								sb.append(ConstantStrings.NEW_LINE).append(ConstantStrings.SPACE).append(ConstantStrings.SPACE).append(modifiedNameValuePair);
							}
						} else {
							sb.append(ConstantStrings.NEW_LINE).append(ConstantStrings.SPACE).append(ConstantStrings.SPACE).append("-- NULL --"); //$NON-NLS-1$
						}

					} catch (Exception e) {
						e.printStackTrace();
					}

				} else
				// For association
				if (requestEntity != null && requestEntity instanceof MultipartRequestEntity) {
					sb.append(ConstantStrings.NEW_LINE).append("MULTIPARTREQUESTENTITY ..."); //$NON-NLS-1$  

					if (parts != null) {
						for (int i = 0; i < parts.length; i++) {
							sb.append(ConstantStrings.NEW_LINE).append("  --------"); //$NON-NLS-1$ 
							ByteArrayOutputStream baos = new ByteArrayOutputStream();
							try {
								parts[i].send(baos);
								if (baos != null) {
									String[] ss = baos.toString().split(ConstantStrings.NEW_LINE);
									int passwordIndex = -1;
									for (int j = 0; j < ss.length; j++) {
										if (j == 0 || (ss[j] == null || ss[j].equals(ConstantStrings.EMPTY_STRING) || ss[j].equals("\r"))) { //$NON-NLS-1$ 
										} else {
											String value = ss[j];

											if (passwordIndex > -1) {
												value = "######"; //$NON-NLS-1$
												sb.append(ConstantStrings.NEW_LINE).append(ConstantStrings.SPACE).append(ConstantStrings.SPACE).append(value);
												break;
											} else {
												sb.append(ConstantStrings.NEW_LINE).append(ConstantStrings.SPACE).append(ConstantStrings.SPACE).append(value);
											}
											passwordIndex = value.toLowerCase().indexOf("password"); //$NON-NLS-1$ 
										}
									}
								}

							} catch (Exception e) {
								e.printStackTrace();
							}
						}

					}

				} else
				// for File Upload
				if (requestEntity != null && requestEntity instanceof FileRequestEntity) {
					sb.append(ConstantStrings.NEW_LINE).append("FILEREQUESTENTITY ..."); //$NON-NLS-1$  

					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					try {
						((FileRequestEntity) requestEntity).writeRequest(baos);

						if (baos != null) {
							String[] s1 = baos.toString().split(ConstantStrings.AMPERSAND);
							for (int i = 0; i < s1.length; i++) {
								String name = ConstantStrings.EMPTY_STRING;
								String value = ConstantStrings.EMPTY_STRING;
								String[] s2 = s1[i].split(ConstantStrings.EQUALS);
								name = s2[0];
								value = s2[1];
								NameValuePair modifiedNameValuePair = new NameValuePair(name, value);
								if (modifiedNameValuePair.getName() != null && modifiedNameValuePair.getName().equalsIgnoreCase("password")) //$NON-NLS-1$
								{
									modifiedNameValuePair.setValue("######"); //$NON-NLS-1$
								} else if (modifiedNameValuePair.getName() != null && modifiedNameValuePair.getName().equalsIgnoreCase("arguments") && modifiedNameValuePair.getValue() != null) //$NON-NLS-1$
								{
									modifiedNameValuePair.setValue(modifiedNameValuePair.getValue().substring(0, Math.min(modifiedNameValuePair.getValue().length(), 100)) + " ..."); //$NON-NLS-1$
								}
								sb.append(ConstantStrings.NEW_LINE).append(ConstantStrings.SPACE).append(ConstantStrings.SPACE).append(modifiedNameValuePair);
							}
						} else {
							sb.append(ConstantStrings.NEW_LINE).append(ConstantStrings.SPACE).append(ConstantStrings.SPACE).append("-- NULL --"); //$NON-NLS-1$
						}
					} catch (Exception e) {
						e.printStackTrace();
					}

				} else {
					sb.append(ConstantStrings.NEW_LINE).append("PARAMETERS ..."); //$NON-NLS-1$  
					sb.append(ConstantStrings.NEW_LINE).append(ConstantStrings.SPACE).append(ConstantStrings.SPACE).append("-- NULL --"); //$NON-NLS-1$
				}
			}

			// ========================================================

			Header[] headers = post.getResponseHeaders();
			sb.append(ConstantStrings.NEW_LINE).append("HEADERS ..."); //$NON-NLS-1$
			if (headers != null && headers.length > 0) {
				for (int i = 0; i < headers.length; i++) {
					sb.append(ConstantStrings.NEW_LINE).append(ConstantStrings.SPACE).append(ConstantStrings.SPACE).append(headers[i].getName()).append(" - ").append(headers[i].getValue()); //$NON-NLS-1$
				}
			} else {
				sb.append(ConstantStrings.NEW_LINE).append(ConstantStrings.SPACE).append(ConstantStrings.SPACE).append("-- NULL --"); //$NON-NLS-1$
			}

			sb.append(ConstantStrings.NEW_LINE).append("ResponseContentLength: ").append(post.getResponseContentLength()); //$NON-NLS-1$

			sb.append(ConstantStrings.NEW_LINE).append("StatusCode: ").append(post.getStatusLine() == null ? "n/a" : post.getStatusCode()); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append(ConstantStrings.NEW_LINE).append("statustext: ").append(post.getStatusLine() == null ? "n/a" : post.getStatusText()); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append(ConstantStrings.NEW_LINE).append("StatusLine: ").append(post.getStatusLine() == null ? "n/a" : post.getStatusLine()); //$NON-NLS-1$ //$NON-NLS-2$

			sb.append(ConstantStrings.NEW_LINE).append("proxyAuthState: ").append(post.getProxyAuthState()); //$NON-NLS-1$
			sb.append(ConstantStrings.NEW_LINE).append("hostAuthState: ").append(post.getHostAuthState()); //$NON-NLS-1$

		}
		// ========================================================

		if (output != null) {

			if (!output.equals(ConstantStrings.EMPTY_STRING)) {
				try {
					JSONObject jsonObject = new JSONObject(output);
					sb.append(ConstantStrings.NEW_LINE).append("JSON response ..."); //$NON-NLS-1$
					sb.append(ConstantStrings.NEW_LINE).append(jsonObject.toString());

				} catch (Exception e) {
					sb.append(ConstantStrings.NEW_LINE).append("Response ..."); //$NON-NLS-1$
					sb.append(ConstantStrings.NEW_LINE).append(output);
				}
			} else {
				sb.append(ConstantStrings.NEW_LINE).append("post.getResponseBodyAsString is an empty string"); //$NON-NLS-1$
			}
		} else {
			sb.append(ConstantStrings.NEW_LINE).append("post.getResponseBodyAsString is null"); //$NON-NLS-1$
		}
		sb.append(ConstantStrings.NEW_LINE).append(ConstantStrings.NEW_LINE);

		return sb.toString();
	}

	public static String getDebugMsg(ClientResponse resp, String uri, int status) {
		StringBuffer sb = new StringBuffer(ConstantStrings.EMPTY_STRING);

		// ========================================================
		sb.append(ConstantStrings.NEW_LINE).append("*************** Connections API");

		if (resp == null) {
			sb.append(ConstantStrings.NEW_LINE).append("For uri: ").append((uri == null ? ConstantStrings.EMPTY_STRING //$NON-NLS-1$  
					: uri));
			sb.append(ConstantStrings.NEW_LINE).append("Response: null, status was set to " + status);
		} else {
			sb.append(ConstantStrings.NEW_LINE).append("Response uri:").append(resp.getUri());
			sb.append(ConstantStrings.NEW_LINE).append("Response Method:").append(resp.getMethod());
			sb.append(ConstantStrings.NEW_LINE).append("Response Status:").append(resp.getStatus());
			sb.append(ConstantStrings.NEW_LINE).append("Response Content Status Text:").append(resp.getStatusText());
			sb.append(ConstantStrings.NEW_LINE).append("Response Type:").append(resp.getType().name());
			sb.append(ConstantStrings.NEW_LINE).append("Response Location:").append(resp.getLocation());
			sb.append(ConstantStrings.NEW_LINE).append("Response Allow:").append(resp.getAllow());
			sb.append(ConstantStrings.NEW_LINE).append("Response Content Length:").append(resp.getContentLength());
			String[] headerNames = resp.getHeaderNames();
			if (headerNames != null && headerNames.length > 0) {
				for (int i = 0; i < headerNames.length; i++) {
					String headerName = headerNames[i];
					sb.append(ConstantStrings.NEW_LINE).append("Header ").append(headerName).append(" :").append(resp.getHeader(headerName));
				}
			}

		}

		return sb.toString();
	}
}
