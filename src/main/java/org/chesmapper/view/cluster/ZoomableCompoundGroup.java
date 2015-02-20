package org.chesmapper.view.cluster;

import java.util.BitSet;
import java.util.List;
import java.util.Vector;

import javax.vecmath.Vector3f;

import org.chesmapper.view.gui.Zoomable;
import org.mg.javalib.util.ArrayUtil;
import org.mg.javalib.util.Vector3fUtil;

public class ZoomableCompoundGroup implements Zoomable
{
	private Vector<Compound> origCompounds;
	private Vector<Compound> filteredCompounds;

	private BitSet bitSet;
	private BitSet dotModeDisplayBitSet;

	private boolean superimposed = false;
	private float superimposeDiameter;
	private float nonSuperimposeDiameter;
	private Vector3f superimposeCenter;
	private Vector3f nonSuperimposeCenter;
	private boolean allCompoundsHaveSamePosition = false;

	protected void setCompounds(List<Compound> compounds)
	{
		this.origCompounds = new Vector<Compound>(compounds);
		this.filteredCompounds = new Vector<Compound>(compounds);
	}

	public int getIndex(Compound compound)
	{
		return filteredCompounds.indexOf(compound);
	}

	public Compound getCompound(int index)
	{
		return filteredCompounds.get(index);
	}

	public int getNumCompounds()
	{
		return filteredCompounds.size();
	}

	public int getOrigSize()
	{
		return origCompounds.size();
	}

	public boolean contains(Compound compound)
	{
		return filteredCompounds.contains(compound);
	}

	public List<Compound> getCompounds()
	{
		return filteredCompounds;
	}

	public boolean isSuperimposed()
	{
		return superimposed;
	}

	public boolean isSpreadable()
	{
		return !allCompoundsHaveSamePosition;
	}

	@Override
	public Vector3f getCenter(boolean superimposed)
	{
		if (superimposed)
			return superimposeCenter;
		else
			return nonSuperimposeCenter;
	}

	@Override
	public float getDiameter(boolean superimposed)
	{
		if (superimposed)
			return superimposeDiameter;
		else
			return nonSuperimposeDiameter;
	}

	public void setSuperimposed(boolean superimposed)
	{
		this.superimposed = superimposed;
	}

	public boolean isFilteredOut(CompoundFilter filter)
	{
		for (Compound c : origCompounds)
			if (filter == null || filter.accept(c))
				return false;
		return true;
	}

	public void setFilter(CompoundFilter filter)
	{
		filteredCompounds = new Vector<Compound>();
		for (Compound c : origCompounds)
			if (filter == null || filter.accept(c))
				filteredCompounds.add(c);
		update();
	}

	protected void update()
	{
		bitSet = new BitSet();
		for (Compound m : filteredCompounds)
			bitSet.or(m.getBitSet());

		dotModeDisplayBitSet = new BitSet();
		for (Compound m : filteredCompounds)
			dotModeDisplayBitSet.or(m.getDotModeDisplayBitSet());

		if (bitSet.cardinality() == 0)
			return;

		// updating (unscaled!) cluster position
		// this is only needed in case a compound was removed
		Vector3f[] positions = ArrayUtil.toArray(ClusteringUtil.getCompoundPositions(this));
		superimposeCenter = Vector3fUtil.center(positions);
		nonSuperimposeCenter = Vector3fUtil.centerConvexHull(positions);

		superimposeDiameter = -1;
		for (Compound m : filteredCompounds)
			if (m.isVisible())
				superimposeDiameter = Math.max(superimposeDiameter, m.getDiameter());

		// recompute diameter, depends on the scaling
		positions = ArrayUtil.toArray(ClusteringUtil.getCompoundPositions(this));
		float maxCompoundDist = Vector3fUtil.maxDist(positions);
		allCompoundsHaveSamePosition = maxCompoundDist == 0;

		// nonSuperimposeDiameter ignores size of compounds, just uses the compound positions
		// for very small diameter: should be at least as big as superimposed one
		nonSuperimposeDiameter = Math.max(superimposeDiameter, maxCompoundDist);
	}

	public BitSet getBitSet()
	{
		return bitSet;
	}

	public BitSet getDotModeDisplayBitSet()
	{
		return dotModeDisplayBitSet;
	}

	public boolean containsCompoundWithJmolIndex(int compoundIndex)
	{
		return getCompoundWithJmolIndex(compoundIndex) != null;
	}

	public Compound getCompoundWithJmolIndex(int compoundIndex)
	{
		for (Compound m : filteredCompounds)
			if (m.getJmolIndex() == compoundIndex)
				return m;
		return null;
	}
}
