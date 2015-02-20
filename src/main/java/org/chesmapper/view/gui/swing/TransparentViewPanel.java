package org.chesmapper.view.gui.swing;

import java.awt.Color;
import java.awt.LayoutManager;

import javax.swing.JPanel;

public class TransparentViewPanel extends JPanel
{
	private Color background;

	public TransparentViewPanel()
	{
		super();
	}

	public TransparentViewPanel(LayoutManager l)
	{
		super(l);
	}

	public void updateUI()
	{
		super.updateUI();
		setBackground(ComponentFactory.BACKGROUND);
	}

	public void setBackground(Color col)
	{
		background = new Color(col.getRed(), col.getGreen(), col.getBlue(), 100);
	}

	public Color getBackground()
	{
		return background;
	}
}
