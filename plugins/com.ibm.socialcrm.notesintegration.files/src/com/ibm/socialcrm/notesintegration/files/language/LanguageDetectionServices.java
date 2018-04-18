package com.ibm.socialcrm.notesintegration.files.language;

/****************************************************************
 * IBM Confidential
 * 
 * SFA050-Collaboration Source Materials
 * 
 * (C) Copyright IBM Corp. 2012
 * 
 * The source code for this program is not published or otherwise divested of its trade secrets, irrespective of what has been deposited with the U.S. Copyright Office
 * 
 ***************************************************************/

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;

import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

import com.cybozu.labs.langdetect.Detector;
import com.cybozu.labs.langdetect.DetectorFactory;
import com.cybozu.labs.langdetect.Language;
import com.ibm.socialcrm.notesintegration.files.FilesPluginActivator;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils;


public class LanguageDetectionServices {
	private static String DEFAULT_LANGUAGE = "en"; //$NON-NLS-1$
	private static String PROFILES_DIR = "langdetectprofiles"; //$NON-NLS-1$
	private static LanguageDetectionServices _instance = null;
	// private static String LOCAL_PROFILES_DIR = null;
	private boolean _isLoaded = false;

	public LanguageDetectionServices() {
		loadProfile();
	}

	public String getLanguage(String documentName, InputStream is) {
		String language = DEFAULT_LANGUAGE;
		ArrayList<Language> probabilities = null;
		if (is != null) {

			long startMills = System.currentTimeMillis();
			try {
				Detector detector = DetectorFactory.create();
				detector.append(getContent(is));
				language = detector.detect();
				probabilities = detector.getProbabilities();
			} catch (Exception e) {
				e.printStackTrace();
			}
			long endMills = System.currentTimeMillis();

			System.out.println("\nDocument:" + documentName + ", language  ==> " + language + ", probabilities ==> " //$NON-NLS-1$  //$NON-NLS-2$  //$NON-NLS-3$
					+ probabilities + " ... " + (endMills - startMills) + " milliSeconds ..."); //$NON-NLS-1$  //$NON-NLS-2$
		}
		return language;
	}

	private String getContent(InputStream is) {

		StringBuilder s = new StringBuilder();
		char[] buf = new char[2048];
		try {
			Reader r = new InputStreamReader(is, "UTF-8"); //$NON-NLS-1$

			while (true) {
				int n = r.read(buf);
				if (n < 0)
					break;
				s.append(buf, 0, n);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return s.toString();

	}

	public static LanguageDetectionServices getInstance() {
		if (_instance == null) {

			_instance = new LanguageDetectionServices();

		}
		return _instance;
	}

	public boolean isProfileDirectoryLoaded() {
		return _isLoaded;
	}

	private void loadProfile() {
		try {
			DetectorFactory.loadProfile(getProfile());
			_isLoaded = true;
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/*
	 * Return the profile directory... Language Detection's language profiles are included in this plugin's langdetectprofiles directory. In runtime, they are included in the plugin's jar file. The
	 * parameter to the DetectorFactory.loadProfile()has to be the absolute path of the profile directory, and because this method can not handle a file path pointing to files within a jar, so we're
	 * going to copy the profiles to the local directory, and pass this local directory to the DetectorFactory.loadProfile() method.
	 */
	private String getProfile() {
		boolean isOK = true;

		String tempDir = GenericUtils.getUniqueTempDir();

		final File tempDirectory = new File(tempDir);
		tempDirectory.mkdirs();
		System.out.println("profile temp directory:" + tempDir); //$NON-NLS-1$

		Bundle bundle = Platform.getBundle(FilesPluginActivator.PLUGIN_ID);
		Enumeration<URL> en = bundle.findEntries(PROFILES_DIR, "*", true); //$NON-NLS-1$
		while (en.hasMoreElements()) {
			URL url = en.nextElement();
			String pathFromBase = url.getPath().substring(PROFILES_DIR.length() + 1);
			String toFileName = tempDir + pathFromBase;
			File toFile = new File(toFileName);

			if (pathFromBase.lastIndexOf('/') == pathFromBase.length() - 1) {
				// This is a directory - create it and recurse
				if (!toFile.mkdir()) {
					{
						isOK = false;
					}
				}
			} else {
				// This is a file - copy it
				try {
					copy(url.openStream(), toFile);

				} catch (Exception e) {
					isOK = false;
					e.printStackTrace();
				}
			}
		}

		return tempDir;

	}

	private void copy(InputStream in, File toFile) throws Exception {
		FileOutputStream to = null;
		try {
			to = new FileOutputStream(toFile);
			byte[] buffer = new byte[4096];
			int bytesRead;

			while ((bytesRead = in.read(buffer)) != -1)
				to.write(buffer, 0, bytesRead); // write
		} catch (Exception e) {

			throw new Exception(e);

		} finally {

			if (in != null)
				try {
					in.close();
				} catch (Exception e) {
					e.printStackTrace();
					throw new Exception(e);
				}
			if (to != null)
				try {
					to.close();
				} catch (Exception e) {
					e.printStackTrace();
					throw new Exception(e);
				}
		}
	}

}
