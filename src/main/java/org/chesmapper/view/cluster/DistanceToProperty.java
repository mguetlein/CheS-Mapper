package org.chesmapper.view.cluster;

import org.chesmapper.map.alg.DistanceMeasure;
import org.chesmapper.map.dataInterface.DefaultNumericProperty;

public class DistanceToProperty extends DefaultNumericProperty
{
	Compound comp;
	DistanceMeasure measure;

	public DistanceToProperty(Compound comp, DistanceMeasure measure, Double[] vals)
	{
		super(null, "Distance based on features and distance mesure that have been used for embedding", vals);
		this.comp = comp;
		this.measure = measure;
	}

	@Override
	public String getName()
	{
		if (measure != DistanceMeasure.UNKNOWN_DISTANCE)
			return measure + " distance to " + comp;
		else
			return "Distance to " + comp;
	}

	public Compound getCompound()
	{
		return comp;
	}
}
