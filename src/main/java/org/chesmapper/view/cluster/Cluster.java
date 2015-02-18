package org.chesmapper.view.cluster;

import java.awt.Color;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import javax.vecmath.Vector3f;

import org.chesmapper.map.dataInterface.ClusterData;
import org.chesmapper.map.dataInterface.CompoundData;
import org.chesmapper.map.dataInterface.CompoundGroupWithProperties;
import org.chesmapper.map.dataInterface.CompoundProperty;
import org.chesmapper.map.dataInterface.NominalProperty;
import org.chesmapper.map.dataInterface.NumericProperty;
import org.chesmapper.map.dataInterface.SubstructureSmartsType;
import org.chesmapper.view.cluster.Compound.DisplayName;
import org.chesmapper.view.gui.View;
import org.chesmapper.view.gui.ViewControler.HighlightSorting;
import org.mg.javalib.gui.DoubleNameListCellRenderer.DoubleNameElement;
import org.mg.javalib.util.ArrayUtil;
import org.mg.javalib.util.CountedSet;
import org.mg.javalib.util.ObjectUtil;
import org.mg.javalib.util.Vector3fUtil;

public class Cluster extends ZoomableCompoundGroup implements CompoundGroupWithProperties, DoubleNameElement,
		Comparable<Cluster>
{
	private ClusterData clusterData;

	HashMap<String, List<Compound>> compoundsOrderedByPropterty = new HashMap<String, List<Compound>>();

	private boolean watched;
	private CompoundProperty highlightProp;
	private HighlightSorting highlightSorting;
	private boolean showLabel = false;

	public Cluster(org.chesmapper.map.dataInterface.ClusterData clusterData)
	{
		this.clusterData = clusterData;
		List<Compound> c = new ArrayList<Compound>();
		int count = 0;
		for (CompoundData d : clusterData.getCompounds())
			c.add(new Compound(clusterData.getCompoundClusterIndices().get(count++), d));
		setCompounds(c);

		displayName.name = getName() + " (#" + getNumCompounds() + ")";
		displayName.compareIndex = clusterData.getOrigIndex();

		if (View.instance != null) // for export without graphics
			update();
	}

	public void setFilter(CompoundFilter filter)
	{
		super.setFilter(filter);
		List<Integer> origIndices = new ArrayList<Integer>();
		if (filter != null)
			for (Compound c : getCompounds())
				if (filter.accept(c))
					origIndices.add(c.getOrigIndex());
		clusterData.setFilter(filter == null ? null : origIndices);
		updateDisplayName();
	}

	boolean alignedCompoundsCalibrated = false;

	public void updatePositions()
	{
		// the actual compound position that is stored in compound is never changed (depends only on scaling)
		// this method moves all compounds to the cluster position

		// recalculate non-superimposed diameter
		update();

		if (!clusterData.isAligned())
		{
			// the compounds have not been aligned
			// the compounds may have a center != 0, calibrate to 0
			for (Compound m : getCompounds())
				m.moveTo(new Vector3f(0f, 0f, 0f));
		}
		else
		{
			// the compounds are aligned, cannot calibrate to 0, this would brake the alignment
			// however, the compound center may have an offset, calculate and remove
			if (!alignedCompoundsCalibrated)
			{
				Vector3f[] origCenters = new Vector3f[getCompounds().size()];
				for (int i = 0; i < origCenters.length; i++)
					origCenters[i] = getCompounds().get(i).origCenter;
				Vector3f center = Vector3fUtil.center(origCenters);
				for (int i = 0; i < origCenters.length; i++)
					getCompounds().get(i).origCenter.sub(center);
				alignedCompoundsCalibrated = true;
			}
			for (Compound m : getCompounds())
				m.moveTo(m.origCenter);
		}

		// translate compounds to the cluster position
		View.instance.setAtomCoordRelative(getCenter(true), getBitSet());
	}

	private DisplayName displayName = new DisplayName();
	private Color highlightColorText;

	public String getName()
	{
		return clusterData.getName();
	}

	@Override
	public String toString()
	{
		return getName();
	}

	public String toStringWithValue()
	{
		return displayName.toString(false, null);
	}

	@Override
	public String getFirstName()
	{
		return displayName.name;
	}

	@Override
	public String getSecondName()
	{
		if (ObjectUtil.equals(displayName.valDisplay, displayName.name))
			return null;
		else
			return displayName.valDisplay;
	}

	public String getSummaryStringValue(CompoundProperty property, boolean html)
	{
		return clusterData.getSummaryStringValue(property, html);
	}

	public CountedSet<String> getNominalSummary(NominalProperty p)
	{
		return clusterData.getNominalSummary(p);
	}

	public String getAlignAlgorithm()
	{
		return clusterData.getAlignAlgorithm();
	}

	protected void update()
	{
		if (getOrigSize() != getNumCompounds())
			displayName.name = getName() + " (#" + getNumCompounds() + "/" + getOrigSize() + ")";
		else
			displayName.name = getName() + " (#" + getNumCompounds() + ")";
		super.update();
	}

	public List<Compound> getCompoundsInOrder(final CompoundProperty property, HighlightSorting sorting)
	{
		String key = property + "_" + sorting;
		if (!compoundsOrderedByPropterty.containsKey(key))
		{
			List<Compound> c = new ArrayList<Compound>();
			for (Compound compound : getCompounds())
				c.add(compound);
			final HighlightSorting finalSorting;
			if (sorting == HighlightSorting.Median)
				finalSorting = HighlightSorting.Max;
			else
				finalSorting = sorting;
			Collections.sort(c, new Comparator<Compound>()
			{
				@Override
				public int compare(Compound o1, Compound o2)
				{
					int res;
					if (o1 == null)
					{
						if (o2 == null)
							res = 0;
						else
							res = 1;
					}
					else if (o2 == null)
						res = -1;
					else if (property instanceof NumericProperty)
					{
						Double d1 = o1.getDoubleValue((NumericProperty) property);
						Double d2 = o2.getDoubleValue((NumericProperty) property);
						if (d1 == null)
						{
							if (d2 == null)
								res = 0;
							else
								res = 1;
						}
						else if (d2 == null)
							res = -1;
						else
							res = d1.compareTo(d2);
					}
					else
						res = (o1.getStringValue((NominalProperty) property) + "").compareTo(o2
								.getStringValue((NominalProperty) property) + "");
					return (finalSorting == HighlightSorting.Max ? -1 : 1) * res;
				}
			});
			if (sorting == HighlightSorting.Median)
			{
				//				Settings.LOGGER.warn("max order: ");
				//				for (Compound mm : m)
				//					Settings.LOGGER.warn(mm.getStringValue(property) + " ");
				//				Settings.LOGGER.warn();

				/**
				 * median sorting:
				 * - first order by max to compute median
				 * - create a dist-to-median array, sort compounds according to that array
				 */
				Compound medianCompound = c.get(c.size() / 2);
				//				Settings.LOGGER.warn(medianCompound.getStringValue(property));
				double distToMedian[] = new double[c.size()];
				if (property instanceof NumericProperty)
				{
					Double med = medianCompound.getDoubleValue((NumericProperty) property);
					for (int i = 0; i < distToMedian.length; i++)
					{
						Double d = c.get(i).getDoubleValue((NumericProperty) property);
						if (med == null)
						{
							if (d == null)
								distToMedian[i] = 0;
							else
								distToMedian[i] = Double.MAX_VALUE;
						}
						else if (d == null)
							distToMedian[i] = Double.MAX_VALUE;
						else
							distToMedian[i] = Math.abs(med - d);
					}
				}
				else
				{
					String medStr = medianCompound.getStringValue((NominalProperty) property);
					for (int i = 0; i < distToMedian.length; i++)
						distToMedian[i] = Math.abs((c.get(i).getStringValue((NominalProperty) property) + "")
								.compareTo(medStr + ""));
				}
				int order[] = ArrayUtil.getOrdering(distToMedian, true);
				Compound a[] = new Compound[c.size()];
				Compound s[] = ArrayUtil.sortAccordingToOrdering(order, c.toArray(a));
				c = ArrayUtil.toList(s);

				//				Settings.LOGGER.warn("med order: ");
				//				for (Compound mm : m)
				//					Settings.LOGGER.warn(mm.getStringValue(property) + " ");
				//				Settings.LOGGER.warn();
			}
			compoundsOrderedByPropterty.put(key, c);
		}
		//		Settings.LOGGER.warn("in order: ");
		//		for (Compound m : order.get(key))
		//			Settings.LOGGER.warn(m.getCompoundOrigIndex() + " ");
		//		Settings.LOGGER.warn("");
		return compoundsOrderedByPropterty.get(key);
	}

	public String getSubstructureSmarts(SubstructureSmartsType type)
	{
		return clusterData.getSubstructureSmarts(type);
	}

	public void removeWithJmolIndices(int[] compoundJmolIndices)
	{
		System.out.println("to remove from cluster: " + ArrayUtil.toString(compoundJmolIndices));
		List<Compound> toDel = new ArrayList<Compound>();
		int[] toDelIndex = new int[compoundJmolIndices.length];

		int count = 0;
		for (int i : compoundJmolIndices)
		{
			Compound c = getCompoundWithJmolIndex(i);
			toDel.add(c);
			toDelIndex[count++] = getIndex(c);
		}
		BitSet bs = new BitSet();
		for (Compound m : toDel)
		{
			bs.or(m.getBitSet());
			getCompounds().remove(m);
		}
		View.instance.hide(bs);

		compoundsOrderedByPropterty.clear();
		clusterData.remove(toDelIndex);
		update();
	}

	public boolean isWatched()
	{
		return watched;
	}

	public void setWatched(boolean watched)
	{
		this.watched = watched;
	}

	public void setHighlighProperty(CompoundProperty highlightProp, Color highlightColorText)
	{
		this.highlightProp = highlightProp;
		this.highlightColorText = highlightColorText;
		updateDisplayName();
	}

	private void updateDisplayName()
	{
		displayName.valDisplay = null;
		displayName.valCompare = null;
		if (highlightProp != null)
		{
			if (highlightProp instanceof NumericProperty)
				displayName.valCompare = new Double[] { getDoubleValue((NumericProperty) highlightProp) };
			else
			{
				String mode = getNominalSummary((NominalProperty) highlightProp).getMode(false);
				String domain[] = ((NominalProperty) highlightProp).getDomain();
				boolean invertSecondBinaryVal = false;
				if (domain.length == 2 && ArrayUtil.indexOf(domain, mode) == 1)
					invertSecondBinaryVal = true;
				CountedSet<String> set = getNominalSummary((NominalProperty) highlightProp);
				/**
				 * Clusters with nominal feature values should be sorted as follows:
				 * 1. according to the mode (the most common feature value)
				 * 2. within equal modes, according to how pure the cluster is with respect to the mode (ratio of compounds with this feature value)
				 * 3. within equal ratios, according to size (and therefore according to number of compounds with this feature value)
				 * 4. within equal size, according to cluster index   
				 */
				displayName.valCompare = new Comparable[] { mode,
						(invertSecondBinaryVal ? 1 : -1) * (set.getMaxCount(false) / (double) (set.getSum(false))),
						(invertSecondBinaryVal ? 1 : -1) * set.getSum(false) };
			}
			displayName.valDisplay = getFormattedValue(highlightProp);
		}
	}

	public CompoundProperty getHighlightProperty()
	{
		return highlightProp;
	}

	public void setHighlightSorting(HighlightSorting highlightSorting)
	{
		this.highlightSorting = highlightSorting;
	}

	public HighlightSorting getHighlightSorting()
	{
		return highlightSorting;
	}

	public String[] getStringValues(NominalProperty property, Compound excludeCompound)
	{
		return getStringValues(property, excludeCompound, false);
	}

	public String[] getStringValues(NominalProperty property, Compound excludeCompound, boolean formatted)
	{
		List<String> l = new ArrayList<String>();
		for (Compound c : getCompounds())
			if (c != excludeCompound && c.getStringValue(property) != null)
				l.add(formatted ? c.getFormattedValue(property) : c.getStringValue(property));
		String v[] = new String[l.size()];
		return l.toArray(v);
	}

	public Double[] getDoubleValues(NumericProperty property)
	{
		Double v[] = new Double[getCompounds().size()];
		for (int i = 0; i < v.length; i++)
			v[i] = getCompounds().get(i).getDoubleValue(property);
		return v;
	}

	public void setShowLabel(boolean showLabel)
	{
		this.showLabel = showLabel;
	}

	public boolean isShowLabel()
	{
		return showLabel;
	}

	public int numMissingValues(CompoundProperty p)
	{
		return clusterData.numMissingValues(p);
	}

	public boolean containsNotClusteredCompounds()
	{
		return clusterData.containsNotClusteredCompounds();
	}

	@Override
	public Double getDoubleValue(NumericProperty p)
	{
		return clusterData.getDoubleValue(p);
	}

	@Override
	public String getFormattedValue(CompoundProperty p)
	{
		return clusterData.getFormattedValue(p);
	}

	public Color getHighlightColorText()
	{
		return highlightColorText;
	}

	@Override
	public int compareTo(Cluster m)
	{
		return displayName.compareTo(m.displayName);
	}

}
