package org.chesmapper.view.gui;

import java.beans.PropertyChangeListener;

import javax.swing.JComponent;
import javax.swing.JPopupMenu;

import org.chesmapper.view.cluster.Clustering;
import org.mg.javalib.gui.Blockable;
import org.mg.javalib.gui.property.Property;

public interface GUIControler extends Blockable
{
	public void updateTitle(Clustering c);

	public void setFullScreen(boolean b);

	public boolean isFullScreen();

	public JPopupMenu getPopup();

	public void showMessage(String msg);

	//	public void handleKeyEvent(KeyEvent e);

	public static final String PROPERTY_FULLSCREEN_CHANGED = "PROPERTY_FULLSCREEN_CHANGED";
	public static final String PROPERTY_VIEWER_SIZE_CHANGED = "PROPERTY_VIEWER_SIZE_CHANGED";

	public void addPropertyChangeListener(PropertyChangeListener l);

	public int getComponentMaxWidth(double pct);

	public int getComponentMaxHeight(double pct);

	public void blockMessages();

	public void unblockMessages();

	public boolean isVisible();

	public void setSelectedString(String s);

	public String getSelectedString();

	public void setAccentuateSizeComponents(boolean b);

	public void registerSizeComponent(Property p, JComponent c);

	public boolean isAccentuateComponents();

}
