package org.chesmapper.view.gui.swing;

import java.awt.Color;

import javax.swing.JLabel;

public class TransparentViewLabel extends JLabel
{
	private Color background;
	private int alpha = 100;

	public TransparentViewLabel()
	{
		super();
		setOpaque(true);
	}

	public TransparentViewLabel(String t)
	{
		super(t);
		setOpaque(true);
	}

	public void updateUI()
	{
		super.updateUI();
		setBackground(ComponentFactory.BACKGROUND);
		setForeground(ComponentFactory.FOREGROUND);
	}

	public void setAlpha(int alpha)
	{
		this.alpha = alpha;
		setBackground(background);
	}

	public void setBackground(Color col)
	{
		background = new Color(col.getRed(), col.getGreen(), col.getBlue(), alpha);
	}

	public Color getBackground()
	{
		return background;
	}
}
