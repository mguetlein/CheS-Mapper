package org.chesmapper.view.gui;

import java.awt.Color;

import org.mg.javalib.gui.property.IntegerProperty;

public class ComponentSize
{
	public static IntegerProperty ICON_2D = new IntegerProperty("Compound image", 15, 5, 40);
	public static IntegerProperty ICON_2D_DOTS = new IntegerProperty("Compound image (style 'Dots')", 20, 5, 40);
	public static IntegerProperty CLUSTER_LIST = new IntegerProperty("Cluster list", 15, 5, 40);
	public static IntegerProperty COMPOUND_LIST = new IntegerProperty("Compound list", 20, 5, 40);
	public static IntegerProperty INFO_TABLE = new IntegerProperty("Info table", 20, 5, 40);

	public static IntegerProperty[] MAX_WIDTH = new IntegerProperty[] { CLUSTER_LIST, COMPOUND_LIST, ICON_2D,
			ICON_2D_DOTS, INFO_TABLE };

	static
	{
		ICON_2D.setHighlightColor(Color.YELLOW);
		ICON_2D_DOTS.setHighlightColor(Color.YELLOW);
		INFO_TABLE.setHighlightColor(Color.CYAN);
		CLUSTER_LIST.setHighlightColor(Color.GREEN);
		COMPOUND_LIST.setHighlightColor(Color.MAGENTA);
	}

}
