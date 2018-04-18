package com.ibm.socialcrm.notesintegration.utils.widgets;

import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Listener;

import com.ibm.socialcrm.notesintegration.utils.GenericUtils;

/**
 * This is a proxy for a Combo or CCombo. We support both Windows and Mac. Combo works better on Windows and CCombo works better on Mac. So this class will create the appropriate object for the
 * current platform and expose the necessary methods that are available in both Combo and CCombo.
 * 
 * This class doesn't cover all the methods in Combo and CCombo, so add them as necessary.
 */
public class SFACombo {

	private Combo combo;
	private CCombo ccombo;

	public SFACombo(Composite parent, int style) {
		if (GenericUtils.isMac()) {
			ccombo = new CCombo(parent, style);
		} else {
			combo = new Combo(parent, style);
		}
	}

	public void setLayoutData(Object data) {
		if (GenericUtils.isMac()) {
			ccombo.setLayoutData(data);
		} else {
			combo.setLayoutData(data);
		}
	}

	public void addFocusListener(FocusListener listener) {
		if (GenericUtils.isMac()) {
			ccombo.addFocusListener(listener);
		} else {
			combo.addFocusListener(listener);
		}
	}

	public void removeFocusListener(FocusListener listener) {
		if (GenericUtils.isMac()) {
			ccombo.removeFocusListener(listener);
		} else {
			combo.removeFocusListener(listener);
		}
	}

	public void addKeyListener(KeyListener listener) {
		if (GenericUtils.isMac()) {
			ccombo.addKeyListener(listener);
		} else {
			combo.addKeyListener(listener);
		}
	}

	public void removeKeyListener(KeyListener listener) {
		if (GenericUtils.isMac()) {
			ccombo.removeKeyListener(listener);
		} else {
			combo.removeKeyListener(listener);
		}

	}

	public String getText() {
		if (GenericUtils.isMac()) {
			return ccombo.getText();
		} else {
			return combo.getText();
		}
	}

	public void removeAll() {
		if (GenericUtils.isMac()) {
			ccombo.removeAll();
		} else {
			combo.removeAll();
		}
	}

	public void setFocus() {
		if (GenericUtils.isMac()) {
			ccombo.setFocus();
		} else {
			combo.setFocus();
		}
	}

	public void addSelectionListener(SelectionListener listener) {
		if (GenericUtils.isMac()) {
			ccombo.addSelectionListener(listener);
		} else {
			combo.addSelectionListener(listener);
		}
	}

	public void removeSelectionListener(SelectionListener listener) {
		if (GenericUtils.isMac()) {
			ccombo.removeSelectionListener(listener);
		} else {
			combo.removeSelectionListener(listener);
		}
	}

	public void addModifyListener(ModifyListener listener) {
		if (GenericUtils.isMac()) {
			ccombo.addModifyListener(listener);
		} else {
			combo.addModifyListener(listener);
		}
	}

	public void removeModifyListener(ModifyListener listener) {
		if (GenericUtils.isMac()) {
			ccombo.removeModifyListener(listener);
		} else {
			combo.removeModifyListener(listener);
		}
	}

	public boolean getListVisible() {
		if (GenericUtils.isMac()) {
			return ccombo.getListVisible();
		} else {
			return combo.getListVisible();
		}
	}

	public void setListVisible(boolean visible) {
		if (GenericUtils.isMac()) {
			ccombo.setListVisible(visible);
		} else {
			combo.setListVisible(visible);
		}
	}

	public int getSelectionIndex() {
		if (GenericUtils.isMac()) {
			return ccombo.getSelectionIndex();
		} else {
			return combo.getSelectionIndex();
		}
	}

	public Object getData() {
		if (GenericUtils.isMac()) {
			return ccombo.getData();
		} else {
			return combo.getData();
		}
	}

	public void setData(Object obj) {
		if (GenericUtils.isMac()) {
			ccombo.setData(obj);
		} else {
			combo.setData(obj);
		}
	}

	public void setText(String text) {
		if (GenericUtils.isMac()) {
			ccombo.setText(text);
		} else {
			combo.setText(text);
		}
	}

	public void redraw() {
		if (GenericUtils.isMac()) {
			ccombo.redraw();
		} else {
			combo.redraw();
		}
	}

	public void setForeground(Color foreground) {
		if (GenericUtils.isMac()) {
			ccombo.setForeground(foreground);
		} else {
			combo.setForeground(foreground);
		}
	}

	public Color getForeground() {
		if (GenericUtils.isMac()) {
			return ccombo.getForeground();
		} else {
			return combo.getForeground();
		}
	}

	public void setBackground(Color background) {
		if (GenericUtils.isMac()) {
			ccombo.setBackground(background);
		} else {
			combo.setBackground(background);
		}
	}

	public Color getBackground() {
		if (GenericUtils.isMac()) {
			return ccombo.getBackground();
		} else {
			return combo.getBackground();
		}
	}

	public void setFont(Font font) {
		if (GenericUtils.isMac()) {
			ccombo.setFont(font);
		} else {
			combo.setFont(font);
		}
	}

	public Point getSelection() {
		if (GenericUtils.isMac()) {
			return ccombo.getSelection();
		} else {
			return combo.getSelection();
		}
	}

	public void setSelection(Point selection) {
		if (GenericUtils.isMac()) {
			ccombo.setSelection(selection);
		} else {
			combo.setSelection(selection);
		}
	}

	public void setItems(String[] items) {
		if (GenericUtils.isMac()) {
			ccombo.setItems(items);
		} else {
			combo.setItems(items);
		}
	}

	public void select(int selection) {
		if (GenericUtils.isMac()) {
			ccombo.select(selection);
		} else {
			combo.select(selection);
		}
	}
	
	public Font getFont() {
		if (GenericUtils.isMac()) {
			return ccombo.getFont();
		} else {
			return combo.getFont();
		}
	}

	

	public boolean isDisposed() {
		if (GenericUtils.isMac()) {
			return ccombo.isDisposed();
		} else {
			return combo.isDisposed();
		}
	}
	
	public void setEnabled(boolean b) {
		if (GenericUtils.isMac()) {
			ccombo.setEnabled(b);
		} else {
			combo.setEnabled(b);
		}
	}
	
	public void clearSelection( ) {
		if (GenericUtils.isMac()) {
			ccombo.clearSelection();
		} else {
			combo.clearSelection();
		}
	}
	
	public void deselectAll( ) {
		if (GenericUtils.isMac()) {
			ccombo.deselectAll();
		} else {
			combo.deselectAll();
		}
	}
	
	
	public String[] getItems() {
		if (GenericUtils.isMac()) {
			return ccombo.getItems();
		} else {
			return combo.getItems();
		}
	}
	
	public Rectangle getBounds() {
		if (GenericUtils.isMac()) {
			return ccombo.getBounds();
		} else {
			return combo.getBounds();
		}
	}
	
	
	public void addMouseListener(MouseListener listener) {
		if (GenericUtils.isMac()) {
			ccombo.addMouseListener(listener);
		} else {
			combo.addMouseListener(listener);
		}
	}
	
	public void addVerifyListener(VerifyListener listener) {
		if (GenericUtils.isMac()) {
			ccombo.addVerifyListener(listener);
		} else {
			combo.addVerifyListener(listener);
		}
	}
	
	
	public void addDisposeListener(DisposeListener listener) {
		if (GenericUtils.isMac()) {
			ccombo.addDisposeListener( listener);
		} else {
			combo.addDisposeListener( listener);
		}
	}
	
	public void addListener(int type, Listener listener) {
		if (GenericUtils.isMac()) {
			ccombo.addListener(type, listener);
		} else {
			combo.addListener(type, listener);
		}
	}
	
	//==========
	
	public void removeMouseListener(MouseListener listener) {
		if (GenericUtils.isMac()) {
			ccombo.removeMouseListener(listener);
		} else {
			combo.removeMouseListener(listener);
		}
	}
	
	public void removeVerifyListener(VerifyListener listener) {
		if (GenericUtils.isMac()) {
			ccombo.removeVerifyListener(listener);
		} else {
			combo.removeVerifyListener(listener);
		}
	}
	
	
	public void removeDisposeListener(DisposeListener listener) {
		if (GenericUtils.isMac()) {
			ccombo.removeDisposeListener( listener);
		} else {
			combo.removeDisposeListener( listener);
		}
	}
	
	public void removeListener(int type, Listener listener) {
		if (GenericUtils.isMac()) {
			ccombo.removeListener(type, listener);
		} else {
			combo.removeListener(type, listener);
		}
	}
	
}
