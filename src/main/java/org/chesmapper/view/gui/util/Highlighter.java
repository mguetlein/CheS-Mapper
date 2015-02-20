package org.chesmapper.view.gui.util;

public class Highlighter
{
	public static final Highlighter DEFAULT_HIGHLIGHTER = new Highlighter("None (show atom types)");
	public static final Highlighter CLUSTER_HIGHLIGHTER = new Highlighter("Cluster");

	private String name;

	public Highlighter(String name)
	{
		this.name = name;
	}

	public String toString()
	{
		return name;
	}
}
