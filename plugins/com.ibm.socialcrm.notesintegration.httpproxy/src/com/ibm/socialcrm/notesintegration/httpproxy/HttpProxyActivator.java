package com.ibm.socialcrm.notesintegration.httpproxy;

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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.bouncycastle.jce.X509Principal;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IStartup;
import org.osgi.framework.BundleContext;

import com.ibm.socialcrm.notesintegration.core.CorePluginActivator;
import com.ibm.socialcrm.notesintegration.core.utils.UpdateSelectionsBroadcaster;
import com.ibm.socialcrm.notesintegration.ui.UiPluginActivator;
import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.socialcrm.notesintegration.utils.UtilsPluginNLSKeys;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;

/**
 * The activator class controls the plug-in life cycle
 */
public class HttpProxyActivator extends Plugin implements IStartup {

	public static final String TLS = "TLS";

	// The plug-in ID
	public static final String PLUGIN_ID = "com.ibm.socialcrm.notesintegration.httpproxy"; //$NON-NLS-1$

	// The shared instance
	private static HttpProxyActivator plugin;

	// This password is just used for the internal keystore we autogenerate. We just use this to work
	// around Firefox blocking local http communication. Doesn't really matter that this is hardcoded.
	private static final String KEY_STORE_PASSW0RD = "passw0rd"; //$NON-NLS-1$

	private final String PROMPTED_FOR_CERT = "com.ibm.socialcrm.sugarwidget.promptedForCert"; //$NON-NLS-1$

	/**
	 * The constructor
	 */
	public HttpProxyActivator() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.Plugins#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.Plugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static HttpProxyActivator getDefault() {
		return plugin;
	}

	@Override
	public void earlyStartup() {


		try {
			// 74684 - check current certificate key length
			boolean isWeakCertificate = checkCertificateLength();
			// 74684 - if weak certificate, remove keystore and certificate so we can re-build it
			if (isWeakCertificate) {
				boolean isCertificateSuccessfullyRemoved = removeCertificate("localsalesconnect");
				if (!isCertificateSuccessfullyRemoved) {
					File keyStoreDir = new File(System.getProperty("user.dir") + "/SalesConnect/"); //$NON-NLS-1$ //$NON-NLS-2$
					UtilsPlugin.getDefault().logInfoMessage(
							"\nError: program can not remove current certificate, please shutdown Notes client, remove the " + keyStoreDir.getAbsolutePath() + "/" + keyStoreDir.getName()
									+ " directory and restart Notes client.", UiPluginActivator.PLUGIN_ID);
					// System.out.println("\nError: program can not remove current certificate, please shutdown Notes client, remove the " + keyStoreDir.getAbsolutePath() + "/" + keyStoreDir.getName()
					// + " directory and restart Notes client.");
				} else {
					UtilsPlugin.getDefault().logInfoMessage(
							"\nSetting prompted_for_cert to recert", UiPluginActivator.PLUGIN_ID);
					// System.out.println("\nSetting prompted_for_cert to recert");
					final Preferences prefs = CorePluginActivator.getDefault().getPluginPreferences();
					prefs.setValue(PROMPTED_FOR_CERT, "recert"); //$NON-NLS-1$					
					CorePluginActivator.getDefault().savePluginPreferences();
				}
			}
			initializeCertificate();
			// setup the socket address
			HttpsServer server = HttpsServer.create();
			// 74684 - fix java.lang.IllegalStateException: SSLContextImpl is not initialized error msg
			// SSLContext sslContext = SSLContext.getInstance(TLS); //$NON-NLS-1$
			final SSLContext sslContext = SSLContext.getInstance(TLS); //$NON-NLS-1$

			KeyStore ks = getKeyStore();

			KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			kmf.init(ks, KEY_STORE_PASSW0RD.toCharArray());

			// setup the trust manager factory
			TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			tmf.init(ks);

			// setup the HTTPS context and parameters
			sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

			server.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
				@Override
				public void configure(HttpsParameters params) {
					try {
						// 67492 - due to poodle fix (63714), we're setting sslcontext default
						// protocol to tlsv1.2... so, this httpserver should not use getDefault()
						// changed to getting tls sslcontext.
						// SSLContext c = SSLContext.getDefault();
						// 74684 - fix java.lang.IllegalStateException: SSLContextImpl is not initialized error msg
						//SSLContext c = SSLContext.getInstance(TLS); //$NON-NLS-1$
						// SSLEngine engine = c.createSSLEngine();
						SSLEngine engine = sslContext.createSSLEngine();

						params.setNeedClientAuth(false);
						params.setCipherSuites(engine.getEnabledCipherSuites());
						params.setProtocols(engine.getEnabledProtocols());

						// get the default parameters
						// 74684 - fix java.lang.IllegalStateException: SSLContextImpl is not initialized error msg
						// SSLParameters defaultSSLParameters = c.getDefaultSSLParameters();
						SSLParameters defaultSSLParameters = sslContext.getDefaultSSLParameters();

						params.setSSLParameters(defaultSSLParameters);
					} catch (Exception e) {
						UtilsPlugin.getDefault().logException(e, PLUGIN_ID);
					}
				}
			});

			server.createContext("/", new HttpHandler() //$NON-NLS-1$
					{
						@Override
						public void handle(HttpExchange exchange) throws IOException {
							String requestMethod = exchange.getRequestMethod();
							// System.out.println("In HTTP Proxy request: " +
							// requestMethod + " " +
							// exchange.getRequestURI().toString());

							String newUrl = "http://localhost:" + UiPluginActivator.getDefault().getHttpPort() //$NON-NLS-1$
									+ exchange.getRequestURI().toString();
							if (requestMethod.equalsIgnoreCase("POST")) //$NON-NLS-1$
							{
								Headers requestHeaders = exchange.getRequestHeaders();
								// create a request to the service we are
								// proxying
								HttpClient client = new HttpClient();
								PostMethod post = new PostMethod(newUrl);

								// add the headers
								for (String aHeaderKey : requestHeaders.keySet()) {
									List<String> values = requestHeaders.get(aHeaderKey);
									for (String aValue : values) {
										post.addRequestHeader(aHeaderKey, aValue);
									}
								}

								client.executeMethod(post);
								InputStream body = post.getResponseBodyAsStream();

								Headers responseHeaders = exchange.getResponseHeaders();
								// copy back the headers from the internal
								// response into the proxy response
								for (Header aHeader : post.getResponseHeaders()) {
									responseHeaders.add(aHeader.getName(), aHeader.getValue());
								}

								OutputStream response = exchange.getResponseBody();

								if (post.getStatusCode() == 200) {
									// copy the body from the internal response
									// to the proxied response
									exchange.sendResponseHeaders(200, 0);
									int b;
									while ((b = body.read()) != -1) {
										response.write(b);
									}
									body.close();
									response.close();
									response.flush();
								} else {
									exchange.sendResponseHeaders(post.getStatusCode(), 0);
									response.close();
									response.flush();
								}

							} else if (requestMethod.equalsIgnoreCase("GET") || requestMethod.equalsIgnoreCase("OPTIONS")) //$NON-NLS-1$ //$NON-NLS-2$
							{
								Headers requestHeaders = exchange.getRequestHeaders();
								// create a request to the service we are
								// proxying
								HttpClient client = new HttpClient();
								GetMethod get = new GetMethod(newUrl);

								// add the headers
								for (String aHeaderKey : requestHeaders.keySet()) {
									List<String> values = requestHeaders.get(aHeaderKey);
									for (String aValue : values) {
										get.addRequestHeader(aHeaderKey, aValue);
									}
								}

								client.executeMethod(get);
								InputStream body = get.getResponseBodyAsStream();

								Headers responseHeaders = exchange.getResponseHeaders();
								// copy back the headers from the internal
								// response into the proxy response
								for (Header aHeader : get.getResponseHeaders()) {
									responseHeaders.add(aHeader.getName(), aHeader.getValue());
								}

								OutputStream response = exchange.getResponseBody();
								if (get.getStatusCode() == 200) {
									// copy the body from the internal response
									// to the proxied response
									exchange.sendResponseHeaders(200, 0);
									int b;
									while ((b = body.read()) != -1) {
										response.write(b);
									}
									body.close();
									response.close();
									response.flush();
								} else {
									exchange.sendResponseHeaders(get.getStatusCode(), 0);
									response.close();
									response.flush();
								}
							}
						}
					});

			server.bind(new InetSocketAddress("localhost", ConstantStrings.HTTP_PROXY_PORT), -1); //$NON-NLS-1$

			UtilsPlugin.getDefault().logInfoMessage("..... starting server .....", UiPluginActivator.PLUGIN_ID);
			// System.out.println("..... starting server .....12");

			server.start();

			
			Job job = new Job(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_UPDATING_LIVE_TEXT_RECOGNIZERS)) {
				@Override
				protected IStatus run(IProgressMonitor arg0) {
					try {
						Thread.sleep(1000);
					} catch (Exception e) {
						e.printStackTrace();
					}
					Display.getDefault().asyncExec(new Runnable() {
						@Override
						public void run() {
							UtilsPlugin.getDefault().logInfoMessage("broadcast trust certificate", UiPluginActivator.PLUGIN_ID);
							// System.out.println("broadcast trust certificate");
							UpdateSelectionsBroadcaster.getInstance().updateCertificate();
						}
					});
					return Status.OK_STATUS;
				}
			};
			job.schedule();

			
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, PLUGIN_ID);
		}
	}

	/**
	 * Creates the SSL certificate used by the local proxy
	 */
	@SuppressWarnings("deprecation")
	private void initializeCertificate() {
		try {
			File keyStoreDir = new File(System.getProperty("user.dir") + "/SalesConnect/"); //$NON-NLS-1$ //$NON-NLS-2$			
			File keyStoreFile = new File(keyStoreDir.getAbsolutePath() + "/salesConnect.jks"); //$NON-NLS-1$
			if (!keyStoreFile.exists()) {
				keyStoreDir.mkdirs();
				keyStoreFile.createNewFile();
				Security.addProvider(new BouncyCastleProvider());

				KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA"); //$NON-NLS-1$

				// 86214 - change key length to 2048
				// 74684 - use 1024-bit key length
				// kpg.initialize(512, new SecureRandom());
				// kpg.initialize(1024, new SecureRandom());
				kpg.initialize(2048, new SecureRandom());


				KeyPair keyPair = kpg.generateKeyPair();

				X509V3CertificateGenerator certGen = new X509V3CertificateGenerator();
				certGen.setSerialNumber(BigInteger.valueOf(System.currentTimeMillis()));
				certGen.setIssuerDN(new X509Principal("CN=localhost, O=IBM, L=, ST=, C=US")); //$NON-NLS-1$
				certGen.setNotBefore(new Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24));
				certGen.setNotAfter(new Date(System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 365 * 10))); // 10 year expiration date
				certGen.setSubjectDN(new X509Principal("CN=localhost, O=IBM, L=, ST=, C=US")); //$NON-NLS-1$
				certGen.setPublicKey(keyPair.getPublic());
				// 86214 - change md5 to sha2
//				certGen.setSignatureAlgorithm("MD5WithRSAEncryption"); //$NON-NLS-1$
				certGen.setSignatureAlgorithm("SHA256WithRSAEncryption"); //$NON-NLS-1$
				
				X509Certificate cert = certGen.generateX509Certificate(keyPair.getPrivate());

				FileOutputStream fos = new FileOutputStream(keyStoreDir.getAbsolutePath() + "/SC.cert"); //$NON-NLS-1$
				fos.write(cert.getEncoded());
				fos.close();

				FileOutputStream fos1 = new FileOutputStream(keyStoreDir.getAbsolutePath() + "/salesConnect.jks"); //$NON-NLS-1$
				KeyStore salesConnectKS = KeyStore.getInstance("JKS"); //$NON-NLS-1$
				salesConnectKS.load(null, KEY_STORE_PASSW0RD.toCharArray());
				salesConnectKS.setKeyEntry("localSalesConnect", keyPair.getPrivate(), KEY_STORE_PASSW0RD.toCharArray(), new java.security.cert.Certificate[]{cert}); //$NON-NLS-1$
				salesConnectKS.store(fos1, KEY_STORE_PASSW0RD.toCharArray()); //$NON-NLS-1$
				fos1.close();

			}
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, PLUGIN_ID);
		}
	}

	private KeyStore getKeyStore() {
		KeyStore ks = null;
		try {
			File keyStore = new File(System.getProperty("user.dir") + "/SalesConnect/salesConnect.jks"); //$NON-NLS-1$ //$NON-NLS-2$
			ks = KeyStore.getInstance("JKS"); //$NON-NLS-1$
			FileInputStream fis = new FileInputStream(keyStore.getAbsolutePath());
			ks.load(fis, KEY_STORE_PASSW0RD.toCharArray());
			fis.close();
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, PLUGIN_ID);
		}
		return ks;
	}

	private boolean checkCertificateLength() {
		boolean isWeakCertificate = false;
		int keylength = 0;

		// the directory hosting the keystore and self-signed certificate
		File keyStoreDir = new File(System.getProperty("user.dir") + "/SalesConnect/"); //$NON-NLS-1$ //$NON-NLS-2$
		// the keystore
		File keyStoreFile = new File(keyStoreDir.getAbsolutePath() + "/salesConnect.jks"); //$NON-NLS-1$
		// the self-signed certificate
		File certFile = new File(keyStoreDir.getAbsolutePath() + "/SC.cert"); //$NON-NLS-1$

		if (keyStoreDir != null && keyStoreDir.exists() && keyStoreFile != null && keyStoreFile.exists() && certFile != null && certFile.exists()) {
			UtilsPlugin.getDefault().logInfoMessage("\n\n========== Certificate existed =================", UiPluginActivator.PLUGIN_ID);
			// System.out.println("\n\n========== Certificate existed =================");
			try {
				Enumeration enumeration = getKeyStore().aliases();
				while (enumeration.hasMoreElements()) {
					String alias = (String) enumeration.nextElement();
					UtilsPlugin.getDefault().logInfoMessage("\nalias name: " + alias, UiPluginActivator.PLUGIN_ID);
					// System.out.println("\nalias name: " + alias);
					Certificate certificate = getKeyStore().getCertificate(alias);
					// System.out.println(certificate.toString());
					if (certificate != null && certificate.getPublicKey() != null) {
						UtilsPlugin.getDefault().logInfoMessage("public key class:" + certificate.getPublicKey().getClass().getName(), UiPluginActivator.PLUGIN_ID);
						// System.out.println("public key class:" + certificate.getPublicKey().getClass().getName());
						if (certificate.getPublicKey() instanceof RSAPublicKey) {
							RSAPublicKey publicKey = (RSAPublicKey) certificate.getPublicKey();
							keylength = publicKey.getModulus().bitLength();
							// 86214 - change key length to 2048
//							if (keylength < 1024) {
							if (keylength < 2048) {
								isWeakCertificate = true;
							}
						}
					}
				}
				UtilsPlugin.getDefault().logInfoMessage("key length:" + keylength + ", isWeakCertifcate=" + isWeakCertificate, UiPluginActivator.PLUGIN_ID);
				// System.out.println("key length:" + keylength + ", isWeakCertifcate=" + isWeakCertificate);
			} catch (Exception e) {
				UtilsPlugin.getDefault().logException(e, PLUGIN_ID);
			}
		}
		return isWeakCertificate;
	}

	private boolean removeCertificate(String aliasX) {
		boolean isCertificateSuccessfullyRemoved = true;
		File keyStoreDir = new File(System.getProperty("user.dir") + "/SalesConnect/"); //$NON-NLS-1$ //$NON-NLS-2$

		try {
			if (keyStoreDir.exists()) {

				File[] files = keyStoreDir.listFiles();

				if (files != null && files.length > 0) {
					for (File f : files) {
						boolean b = f.delete();
						UtilsPlugin.getDefault().logInfoMessage("Removing " + f.getAbsolutePath() + ":" + f.getName() + ", status:" + (b ? "succeeded" : "failed"), UiPluginActivator.PLUGIN_ID);
						// System.out.println("Removing " + f.getAbsolutePath() + ":" + f.getName() + ", status:" + (b ? "succeeded" : "failed"));
						if (!b) {
							isCertificateSuccessfullyRemoved = false;
						}
					}
				}

				files = keyStoreDir.listFiles();
				if (files == null || files.length == 0) {
					boolean b = keyStoreDir.delete();
					UtilsPlugin.getDefault().logInfoMessage("Removing " + keyStoreDir.getAbsolutePath() + ":" + keyStoreDir.getName() + ", status:" + (b ? "succeeded" : "failed"),
							UiPluginActivator.PLUGIN_ID);
					// System.out.println("Removing " + keyStoreDir.getAbsolutePath() + ":" + keyStoreDir.getName() + ", status:" + (b ? "succeeded" : "failed"));
					if (b) {
						UtilsPlugin.getDefault().logInfoMessage("Successfully cleaned up previous certificate... continue creating new certificate process", UiPluginActivator.PLUGIN_ID);
						// System.out.println("Successfully cleaned up previous certificate... continue creating new certificate process");
					} else {
						isCertificateSuccessfullyRemoved = false;
					}
				}

			}
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, PLUGIN_ID);
		}
		return isCertificateSuccessfullyRemoved;
	}
}
