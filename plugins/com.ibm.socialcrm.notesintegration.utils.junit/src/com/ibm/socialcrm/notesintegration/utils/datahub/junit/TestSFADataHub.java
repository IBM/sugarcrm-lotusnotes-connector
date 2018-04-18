package com.ibm.socialcrm.notesintegration.utils.datahub.junit;

/****************************************************************
 * IBM OpenSource
 *
 * (C) Copyright IBM Corp. 2012
 *
 * Licensed under the Apache License v2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 ***************************************************************/

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import junit.framework.TestCase;

import com.ibm.socialcrm.notesintegration.utils.datahub.SFADataHub;
import com.ibm.socialcrm.notesintegration.utils.datahub.SFADataShare;

public class TestSFADataHub extends TestCase {
	private SFADataShare<String, String> dataShare1;
	private SFADataShare<String, String> dataShare2;
	private String NAMESPACE_1 = "namespace1"; //$NON-NLS-1$
	private String NAMESPACE_2 = "namespace2"; //$NON-NLS-1$

	public void testBasicValues() {
		dataShare1.put("food", "burger");
		assertEquals(dataShare1.get("food"), "burger");
		assertEquals(dataShare2.get("food"), null);

		dataShare2.put("food", "turkey");
		assertEquals(dataShare2.get("food"), "turkey");
		assertEquals(dataShare1.get("food"), "burger");
	}

	public void testListeners() {
		SFADataShare childShare = new SFADataShare<String, String>("childShare");
		dataShare1.addChildShare(childShare);

		final boolean[] listenersFired = new boolean[] { false, false };

		// Ensure we only get notified for events we care about
		PropertyChangeListener pcl1 = new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				listenersFired[0] = true;
			}
		};
		childShare.addPropertyChangeListener(pcl1);

		PropertyChangeListener pcl2 = new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				listenersFired[1] = true;
			}
		};
		childShare.addPropertyChangeListener("foo", pcl2);
		childShare.put("food", "burger");

		assertEquals(listenersFired[0], true);
		assertEquals(listenersFired[1], false);

		childShare.put("foo", "bar");
		assertEquals(listenersFired[1], true);

		// Reset the listeners
		listenersFired[0] = false;
		listenersFired[1] = false;
		childShare.removePropertyChangeListener(pcl1);
		childShare.removePropertyChangeListener("foo", pcl2);

		// Add one to the parent
		childShare.put("foo", "bar2");
		// Ensure the listeners were properly removed and did not fire
		assertEquals(listenersFired[0], false);
		assertEquals(listenersFired[1], false);
		dataShare1.addPropertyChangeListener(pcl1);
		childShare.put("foo", "bar3");
		// Ensure pcl1 fired and we caught the change event from the child datashare
		assertEquals(listenersFired[0], true);

		listenersFired[0] = false;
		// Ensure listeners fire on remove
		childShare.remove("foo");
		assertEquals(listenersFired[0], true);

		// Test that if we listen for a specific property at the top level, we don't get false events if a property from our child
		// with the same name fires an event.
		listenersFired[0] = false;
		listenersFired[1] = false;
		dataShare1.removePropertyChangeListener(pcl1);
		dataShare1.addPropertyChangeListener("foo", pcl1);

		childShare.put("foo", "bar4");
		assertEquals(listenersFired[0], false); // Should not have fired since we changed foo in a child datashare... i.e., not the parent namespace
		dataShare1.put("foo", "bar4");
		assertEquals(listenersFired[0], true); // This one should have fired.
	}

	public void testClear() {
		SFADataShare childShare = new SFADataShare<String, String>("childShare");
		dataShare1.addChildShare(childShare);

		dataShare1.put("key1", "value1");
		childShare.put("key2", "value2");

		// Make sure the basic put works
		assertEquals(dataShare1.get("key1"), "value1");
		assertEquals(childShare.get("key2"), "value2");

		dataShare1.clear();

		// Make sure the data was cleared
		assertEquals(dataShare1.get("key1"), null);
		assertEquals(childShare.get("key2"), null);
	}

	@Override
	protected void setUp() throws Exception {
		dataShare1 = new SFADataShare<String, String>(NAMESPACE_1);
		dataShare2 = new SFADataShare<String, String>(NAMESPACE_2);

		SFADataHub.getInstance().addDataShare(dataShare1);
		SFADataHub.getInstance().addDataShare(dataShare2);
	}

	@Override
	protected void tearDown() throws Exception {
		SFADataHub.getInstance().removeDataShare(NAMESPACE_1);
		SFADataHub.getInstance().removeDataShare(NAMESPACE_2);

		dataShare1 = null;
		dataShare2 = null;
	}

}
