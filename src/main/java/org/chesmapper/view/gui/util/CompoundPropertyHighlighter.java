package org.chesmapper.view.gui.util;

import java.util.HashMap;

import org.chesmapper.map.dataInterface.CompoundProperty;

public class CompoundPropertyHighlighter extends Highlighter
{
	private CompoundProperty prop;

	private static HashMap<CompoundProperty, CompoundPropertyHighlighter> INSTANCES = new HashMap<CompoundProperty, CompoundPropertyHighlighter>();

	public static CompoundPropertyHighlighter create(CompoundProperty prop)
	{
		if (!INSTANCES.containsKey(prop))
			INSTANCES.put(prop, new CompoundPropertyHighlighter(prop));
		return INSTANCES.get(prop);
	}

	private CompoundPropertyHighlighter(CompoundProperty prop)
	{
		super(prop.toString());
		this.prop = prop;
	}

	public CompoundProperty getProperty()
	{
		return prop;
	}
}