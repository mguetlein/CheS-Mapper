package org.chesmapper.view.gui.util;

import java.util.HashMap;

import org.chesmapper.map.dataInterface.SubstructureSmartsType;

public class SubstructureHighlighter extends Highlighter
{
	private SubstructureSmartsType type;

	private static HashMap<SubstructureSmartsType, SubstructureHighlighter> INSTANCES = new HashMap<SubstructureSmartsType, SubstructureHighlighter>();

	public static SubstructureHighlighter create(SubstructureSmartsType type)
	{
		if (!INSTANCES.containsKey(type))
			INSTANCES.put(type, new SubstructureHighlighter(type));
		return INSTANCES.get(type);
	}

	private SubstructureHighlighter(SubstructureSmartsType type)
	{
		super(type.getName());
		this.type = type;
	}

	public SubstructureSmartsType getType()
	{
		return type;
	}
}