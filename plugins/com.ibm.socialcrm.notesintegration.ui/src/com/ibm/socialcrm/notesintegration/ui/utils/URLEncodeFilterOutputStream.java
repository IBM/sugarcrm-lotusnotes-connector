package com.ibm.socialcrm.notesintegration.ui.utils;

/****************************************************************
 * IBM OpenSource
 *
 * (C) Copyright IBM Corp. 2012
 *
 * Licensed under the Apache License v2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 ***************************************************************/

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;

import com.ibm.socialcrm.notesintegration.ui.UiPluginActivator;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;

public class URLEncodeFilterOutputStream extends FilterOutputStream {
	public boolean applyFilter = true;

	public URLEncodeFilterOutputStream(OutputStream out) {
		super(out);
	}

	@Override
	public void write(byte[] bytes, int off, int len) throws IOException {
		if (applyFilter) {
			byte[] newBytes = urlEncodeBytes(bytes);
			// Same reason as for the write(byte[]) method.
			boolean oldFilter = applyFilter;
			applyFilter = false;
			super.write(newBytes, off, newBytes.length);
			applyFilter = oldFilter;
		} else {
			super.write(bytes, off, len);
		}
	}

	@Override
	public void write(byte[] bytes) throws IOException {
		if (applyFilter) {
			byte[] newBytes = urlEncodeBytes(bytes);
			// This can result in a call to write(byte[], int, int) so we don't want to double encode
			// NOT Thread safe! Shouldn't matter though as only one write method should be called at a time
			// from a given thread.
			boolean oldFilter = applyFilter;
			applyFilter = false;
			super.write(newBytes);
			applyFilter = oldFilter;
		} else {
			super.write(bytes);
		}
	}

	/**
	 * URL encodes the list of bytes and returns an array of url encoded bytes
	 * 
	 * @param bytes
	 * @return
	 */
	private byte[] urlEncodeBytes(byte[] bytes) {
		try {
			bytes = URLEncoder.encode(new String(bytes), "UTF-8").getBytes(); //$NON-NLS-1$
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, UiPluginActivator.PLUGIN_ID);
		}

		return bytes;
	}

	public void setApplyFilter(boolean applyFilter) {
		this.applyFilter = applyFilter;
	}

}
