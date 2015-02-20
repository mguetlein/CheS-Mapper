package org.chesmapper.view.cluster;

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.vecmath.Vector3f;

import org.chesmapper.map.alg.embed3d.CorrelationProperty;
import org.chesmapper.map.alg.embed3d.EqualPositionProperty;
import org.chesmapper.map.alg.embed3d.Random3DEmbedder;
import org.chesmapper.map.appdomain.AppDomainComputer;
import org.chesmapper.map.data.ClusteringData;
import org.chesmapper.map.data.CompoundDataImpl;
import org.chesmapper.map.dataInterface.ClusterData;
import org.chesmapper.map.dataInterface.CompoundData;
import org.chesmapper.map.dataInterface.CompoundProperty;
import org.chesmapper.map.dataInterface.CompoundPropertyOwner;
import org.chesmapper.map.dataInterface.CompoundPropertyUtil;
import org.chesmapper.map.dataInterface.DefaultNumericProperty;
import org.chesmapper.map.dataInterface.NominalProperty;
import org.chesmapper.map.dataInterface.NumericProperty;
import org.chesmapper.map.dataInterface.SubstructureSmartsType;
import org.chesmapper.map.main.Settings;
import org.chesmapper.map.main.TaskProvider;
import org.chesmapper.map.weka.Predictor;
import org.chesmapper.map.weka.Predictor.PredictionResult;
import org.chesmapper.view.gui.View;
import org.chesmapper.view.gui.Zoomable;
import org.mg.javalib.gui.CheckBoxSelectDialog;
import org.mg.javalib.util.ArrayUtil;
import org.mg.javalib.util.CountedSet;
import org.mg.javalib.util.ListUtil;
import org.mg.javalib.util.SelectionModel;
import org.mg.javalib.util.Vector3fUtil;
import org.mg.javalib.util.VectorUtil;

public class ClusteringImpl implements Zoomable, Clustering
{
	private Vector<Cluster> clusters;
	private ClusteringData clusteringData;

	SelectionModel clusterActive;
	SelectionModel clusterWatched;
	SelectionModel compoundActive;
	SelectionModel compoundWatched;

	boolean suppresAddEvent = false;
	Vector<PropertyChangeListener> listeners;

	public static final String CLUSTER_ADDED = "cluster_added";
	public static final String CLUSTER_REMOVED = "cluster_removed";
	public static final String CLUSTER_MODIFIED = "cluster_modified";
	public static final String CLUSTER_NEW = "cluster_new";
	public static final String CLUSTER_CLEAR = "cluster_clear";
	public static final String PROPERTY_ADDED = "property_added";

	boolean dirty = true;
	int numCompounds = -1;
	private boolean superimposed = false;
	float superimposedDiameter;
	float nonSuperimposedDiameter;

	BitSet bitSetAll;
	HashMap<Cluster, Integer> clusterIndices;
	HashMap<Integer, Compound> jmolCompoundIndexToCompound;
	HashMap<Integer, Cluster> jmolCompoundIndexToCluster;
	HashMap<CompoundProperty, Integer> numMissingValues;
	HashMap<CompoundProperty, Integer> numDistinctValues;

	ClusteringValues clusteringValues = new ClusteringValues(this);

	List<Compound> filteredCompoundList;
	List<Compound> filteredCompoundListIncludingMultiClusteredCompounds;

	Vector3f superimposedCenter;
	Vector3f nonSuperimposedCenter;

	Boolean endpointDataset;
	Boolean filledEndpointDataset;

	CompoundProperty highlightProperty;
	Color highlightColorText;

	JitteringProvider jittering;

	public ClusteringImpl()
	{
		listeners = new Vector<PropertyChangeListener>();
		init();
	}

	public void init()
	{
		clusterActive = new SelectionModel();
		clusterWatched = new SelectionModel();
		compoundActive = new SelectionModel(true);
		compoundWatched = new SelectionModel(true);
		clusters = new Vector<Cluster>();
		numMissingValues = new HashMap<CompoundProperty, Integer>();
		numDistinctValues = new HashMap<CompoundProperty, Integer>();
	}

	public void addListener(PropertyChangeListener l)
	{
		listeners.add(l);
	}

	public void addListenerFirst(PropertyChangeListener l)
	{
		listeners.insertElementAt(l, 0);
	}

	public void fire(String event, Object oldValue, Object newValue)
	{
		if (!suppresAddEvent)
			for (PropertyChangeListener l : listeners)
				l.propertyChange(new PropertyChangeEvent(this, event, oldValue, newValue));
	}

	private Cluster addSingleCluster(ClusterData clusterData)
	{
		Cluster c = new Cluster(clusterData);
		@SuppressWarnings("unchecked")
		Vector<Cluster> old = (Vector<Cluster>) VectorUtil.clone(ClusteringImpl.this.clusters);
		clusters.add(c);
		dirty = true;
		fire(CLUSTER_ADDED, old, clusters);
		return c;
	}

	public synchronized void update()
	{
		if (!dirty)
			return;

		numCompounds = 0;
		for (Cluster c : clusters)
			numCompounds += c.getNumCompounds();

		if (View.instance != null) // for export without graphics
		{
			bitSetAll = new BitSet();
			for (Cluster c : clusters)
				bitSetAll.or(c.getBitSet());
		}

		int count = 0;
		clusterIndices = new HashMap<Cluster, Integer>();
		jmolCompoundIndexToCluster = new HashMap<Integer, Cluster>();
		jmolCompoundIndexToCompound = new HashMap<Integer, Compound>();
		for (Cluster c : clusters)
		{
			clusterIndices.put(c, count++);
			for (Compound m : c.getCompounds())
			{
				if (jmolCompoundIndexToCompound.get(m.getJmolIndex()) != null)
					throw new Error("duplicate compund index! " + m.getJmolIndex());
				jmolCompoundIndexToCompound.put(m.getJmolIndex(), m);
				jmolCompoundIndexToCluster.put(m.getJmolIndex(), c);
			}
		}

		filteredCompoundListIncludingMultiClusteredCompounds = new ArrayList<Compound>();
		for (Cluster c : clusters)
			for (Compound mm : c.getCompounds())
				filteredCompoundListIncludingMultiClusteredCompounds.add(mm);

		filteredCompoundList = new ArrayList<Compound>();
		HashSet<Integer> compoundIndex = new HashSet<Integer>();
		for (Cluster c : clusters)
			for (Compound mm : c.getCompounds())
			{
				if (!compoundIndex.contains(mm.getOrigIndex()))
				{
					filteredCompoundList.add(mm);
					compoundIndex.add(mm.getOrigIndex());
				}
			}

		numMissingValues.clear();
		numDistinctValues.clear();

		clusteringValues.clear();
		compoundSelections.clear();

		endpointDataset = null;
		filledEndpointDataset = null;

		dirty = false;
	}

	public boolean isClusterActive()
	{
		return clusterActive.getSelected() != -1;
	}

	public boolean isClusterWatched()
	{
		return clusterWatched.getSelected() != -1;
	}

	public boolean isCompoundWatched()
	{
		return compoundWatched.getSelected() != -1;
	}

	public boolean isCompoundActiveFromCluster(int cluster)
	{
		int sel[] = compoundActive.getSelectedIndices();
		if (sel.length == 0)
			return false;
		for (int compoundJmolIndex : sel)
			if (getClusterIndexForJmolIndex(compoundJmolIndex) == cluster)
				return true;
		return false;
	}

	public boolean isCompoundWatchedFromCluster(int cluster)
	{
		int sel[] = compoundWatched.getSelectedIndices();
		if (sel.length == 0)
			return false;
		for (int compoundJmolIndex : sel)
			if (getClusterIndexForJmolIndex(compoundJmolIndex) == cluster)
				return true;
		return false;
	}

	public SelectionModel getClusterActive()
	{
		return clusterActive;
	}

	public SelectionModel getClusterWatched()
	{
		return clusterWatched;
	}

	public SelectionModel getCompoundActive()
	{
		return compoundActive;
	}

	public SelectionModel getCompoundWatched()
	{
		return compoundWatched;
	}

	public Cluster getClusterForCompound(Compound compound)
	{
		update();
		return jmolCompoundIndexToCluster.get(compound.getJmolIndex());
	}

	public Cluster getClusterForJmolIndex(int jmolIndex)
	{
		update();
		return jmolCompoundIndexToCluster.get(jmolIndex);
	}

	public int indexOf(Cluster cluster)
	{
		update();
		return clusterIndices.get(cluster);
	}

	public int getClusterIndexForCompound(Compound compound)
	{
		return indexOf(getClusterForCompound(compound));
	}

	public int getClusterIndexForJmolIndex(int jmolIndex)
	{
		return indexOf(getClusterForJmolIndex(jmolIndex));
	}

	public Compound getCompoundWithJmolIndex(int jmolIndex)
	{
		update();
		return jmolCompoundIndexToCompound.get(jmolIndex);
	}

	public Compound[] getCompoundsWithJmolIndices(int[] idx)
	{
		Compound c[] = new Compound[idx.length];
		for (int i = 0; i < c.length; i++)
			c[i] = getCompoundWithJmolIndex(idx[i]);
		return c;
	}

	public int[] getJmolIndicesWithCompounds(Compound[] c)
	{
		int idx[] = new int[c.length];
		for (int i = 0; i < c.length; i++)
			idx[i] = c[i].getJmolIndex();
		return idx;
	}

	public synchronized int numMissingValues(CompoundProperty p)
	{
		update();
		if (!numMissingValues.containsKey(p))
		{
			int num = 0;
			for (Cluster c : clusters)
				num += c.numMissingValues(p);
			numMissingValues.put(p, num);
		}
		return numMissingValues.get(p);
	}

	public synchronized int numDistinctValues(CompoundProperty p)
	{
		update();
		if (!numDistinctValues.containsKey(p))
		{
			int numDistinct;
			if (p instanceof NumericProperty)
				numDistinct = CompoundPropertyUtil.computeNumDistinct(getDoubleValues((NumericProperty) p));
			else
				numDistinct = CompoundPropertyUtil.computeNumDistinct(getStringValues((NominalProperty) p, null));
			numDistinctValues.put(p, numDistinct);
		}
		return numDistinctValues.get(p);
	}

	@Override
	public int getNumCompounds()
	{
		return numCompounds();
	}

	public int numCompounds()
	{
		update();
		return numCompounds;
	}

	@Override
	public CountedSet<String> getNominalSummary(NominalProperty p)
	{
		return clusteringValues.getNominalSummary(p);
	}

	public int numClusters()
	{
		return clusters.size();
	}

	public BitSet getBitSetAll()
	{
		update();
		return bitSetAll;
	}

	public synchronized void clear()
	{
		checkNoSelection();

		@SuppressWarnings("unchecked")
		final Vector<Cluster> old = (Vector<Cluster>) VectorUtil.clone(ClusteringImpl.this.clusters);
		clusters.removeAllElements();
		clusteringData = null;
		View.instance.zap(true, true, true);
		filteredCompoundList.clear();
		filteredCompoundListIncludingMultiClusteredCompounds.clear();
		clusteringValues.clear();
		compoundSelections.clear();
		dirty = true;

		jittering = null;

		getClusterActive().clearSelection();
		fire(CLUSTER_CLEAR, old, clusters);
	}

	public void removeCluster(final Cluster... clusters)
	{
		checkNoSelection();

		@SuppressWarnings("unchecked")
		Vector<Cluster> old = (Vector<Cluster>) VectorUtil.clone(ClusteringImpl.this.clusters);
		for (Cluster c : clusters)
		{
			View.instance.hide(c.getBitSet());
			ClusteringImpl.this.clusters.remove(c);
		}
		dirty = true;
		updatePositions();
		if (getNumClusters() == 1)
			getClusterActive().setSelected(0);
		fire(CLUSTER_REMOVED, old, clusters);
	}

	public Cluster getCluster(int clusterIndex)
	{
		if (clusterIndex < 0)
			return null;
		return clusters.get(clusterIndex);
	}

	public Vector<Cluster> getClusters()
	{
		return clusters;
	}

	@SuppressWarnings("unchecked")
	public void newClustering(ClusteringData d)
	{
		//		@SuppressWarnings("unchecked")
		Vector<Cluster> old = (Vector<Cluster>) VectorUtil.clone(ClusteringImpl.this.clusters);
		suppresAddEvent = true;

		clusteringData = d;

		{
			String filename = d.getSDF();
			TaskProvider.debug("Load dataset into Jmol");

			if (View.instance != null) // for export without graphics
			{
				View.instance.loadCompoundFromFile(null, filename, null, null, false, null, null, 0);
				if (d.getNumCompounds(true) != View.instance.getCompoundCount())
					throw new Error("illegal num compounds, loaded by Jmol: " + View.instance.getCompoundCount()
							+ " != from wizard: " + d.getNumCompounds(true));
			}
		}

		for (int i = 0; i < d.getNumClusters(); i++)
			addSingleCluster(d.getCluster(i));

		{
			//data sanity checks
			List<Integer> jmol = new ArrayList<Integer>();
			List<Integer> orig = new ArrayList<Integer>();
			for (int i = 0; i < d.getNumClusters(); i++)
			{
				jmol = ListUtil.concat(jmol, d.getCluster(i).getCompoundClusterIndices());
				orig = ListUtil.concat(orig, d.getCluster(i).getCompoundOrigIndices());
			}
			int num_mult = clusteringData.getNumCompounds(true);
			int num_uniq = clusteringData.getNumCompounds(false);
			if (jmol.size() != orig.size())
				throw new IllegalStateException("internal error, num compounds #1 " + jmol.size() + ", " + orig.size());
			if (jmol.size() != num_mult)
				throw new IllegalStateException("internal error, num compounds #2 " + jmol.size() + ", " + num_mult);
			if (isClusterAlgorithmDisjoint())
			{
				if (num_mult != num_uniq)
					throw new IllegalStateException("internal error, num compounds #3 " + num_mult + ", " + num_uniq);
			}
			else
			{
				if (num_mult <= num_uniq)
					throw new IllegalStateException("internal error, num compounds #4 " + num_mult + ", " + num_uniq);
				orig = ListUtil.uniqValue(orig);
				if (orig.size() != num_uniq)
					throw new IllegalStateException("internal error, num compounds #5 " + orig.size() + ", " + num_uniq);
				jmol = ListUtil.uniqValue(jmol);
				if (jmol.size() != num_mult)
					throw new IllegalStateException("internal error, num compounds #6 " + jmol.size() + ", " + num_mult);
			}
			Collections.sort(jmol);
			Collections.sort(orig);
			int i;
			for (i = 0; i < num_uniq; i++)
				if (i != jmol.get(i) || i != orig.get(i))
					throw new IllegalStateException("internal error, num compounds #7 " + i + ", " + jmol.get(i) + ", "
							+ orig.get(i));
			if (!isClusterAlgorithmDisjoint())
				for (; i < num_mult; i++)
					if (i != jmol.get(i))
						throw new IllegalStateException("internal error, num compounds #8 " + i + ", " + jmol.get(i));
		}

		update();
		if (View.instance != null) // for export without graphics
		{
			TaskProvider.update(90, "Loading graphics");
			updatePositions();
		}

		suppresAddEvent = false;

		fire(CLUSTER_ADDED, old, clusters);
		fire(CLUSTER_NEW, old, clusters);

		if (View.instance != null) // for export without graphics
			View.instance.scriptWait("hover off");
	}

	public synchronized void updatePositions()
	{
		update();

		ClusteringUtil.updateScaleFactor(this);

		//		SwingUtil.invokeAndWait(new Runnable()
		//		{
		//			public void run()
		//			{
		getClusterWatched().clearSelection();
		getCompoundWatched().clearSelection();
		//			}
		//		});

		View.instance.suspendAnimation("updating clustering positions");
		for (Cluster c : clusters)
		{
			if (!superimposed)
				setClusterOverlap(c, true, null);
			c.updatePositions();
			if (!superimposed)
				setClusterOverlap(c, false, null);
		}
		View.instance.proceedAnimation("updating clustering positions");

		Vector3f[] positions = ArrayUtil.toArray(ClusteringUtil.getClusterPositions(this));
		superimposedCenter = Vector3fUtil.centerConvexHull(positions);

		// take only cluster points into account (ignore cluster sizes)
		superimposedDiameter = Vector3fUtil.maxDist(positions);
		// needed for very small distances / only one cluster : diameter should be at least as big as cluster diameter
		for (Cluster c : clusters)
			superimposedDiameter = Math.max(superimposedDiameter, c.getDiameter(true));

		// take only compound positions into account (ignore compound sizes)
		positions = ClusteringUtil.getCompoundPositions(this);
		nonSuperimposedCenter = Vector3fUtil.centerConvexHull(positions);
		nonSuperimposedDiameter = Vector3fUtil.maxDist(positions);

		//if all is filtered apart from compounds that share a single position nonSuperimposeDiameter would be 0
		nonSuperimposedDiameter = Math.max(nonSuperimposedDiameter, superimposedDiameter);
	}

	/**
	 * toggles compound positions between compound position (overlap=false) and cluster position (overlap=true) 
	 * 
	 * @param clusters
	 * @param overlap
	 * @param anim
	 */
	public void setClusterOverlap(List<Cluster> clusters, boolean overlap, View.AnimationSpeed anim)
	{
		List<Vector3f> compoundPositions = new ArrayList<Vector3f>();
		List<BitSet> bitsets = new ArrayList<BitSet>();

		for (Cluster cluster : clusters)
		{
			if (cluster.isSuperimposed() != overlap)
			{
				for (int i = 0; i < cluster.getNumCompounds(); i++)
				{
					bitsets.add(cluster.getCompound(i).getBitSet());

					// destination is compound position
					Vector3f pos = cluster.getCompound(i).getPosition();
					// compound is already at cluster position, sub to get relative vector
					pos.sub(cluster.getCenter(true));
					if (overlap)
						pos.scale(-1);
					compoundPositions.add(pos);
				}
			}
			cluster.setSuperimposed(overlap);
		}
		View.instance.setAtomCoordRelative(compoundPositions, bitsets, anim);
	}

	/**
	 * not animated
	 * 
	 * @param comp
	 * @param enable
	 */
	public void moveForDotMode(Compound comp, boolean enable)
	{
		// move to compound center ...
		Vector3f pos = new Vector3f(comp.origCenter);
		// ... from single atom position
		pos.sub(comp.origDotPosition);
		// reverse if neccessary
		if (!enable)
			pos.scale(-1);
		View.instance.setAtomCoordRelative(pos, comp.getDotModeDisplayBitSet());
	}

	public void setClusterOverlap(Cluster cluster, boolean overlap, View.AnimationSpeed anim)
	{
		List<Cluster> l = new ArrayList<Cluster>();
		l.add(cluster);
		setClusterOverlap(l, overlap, anim);
	}

	public int[] clusterChooser(String title, String description)
	{
		int clusterIndex = getClusterActive().getSelected();
		if (clusterIndex == -1)
			clusterIndex = getClusterWatched().getSelected();

		Cluster c[] = new Cluster[numClusters()];
		for (int i = 0; i < c.length; i++)
			c[i] = getCluster(i);
		boolean b[] = new boolean[numClusters()];
		if (clusterIndex != -1)
			b[clusterIndex] = true;

		return CheckBoxSelectDialog.selectIndices(Settings.TOP_LEVEL_FRAME, title, description, c, b);
	}

	public int[] selectJmolIndicesWithCompoundChooser(String title, String description)
	{
		int clusterIndex = getClusterActive().getSelected();
		if (clusterIndex == -1)
			clusterIndex = getClusterWatched().getSelected();

		List<Compound> l = new ArrayList<Compound>();
		List<Boolean> lb = new ArrayList<Boolean>();

		for (int i = 0; i < numClusters(); i++)
		{
			Cluster c = getCluster(i);
			for (int j = 0; j < c.getNumCompounds(); j++)
			{
				l.add(c.getCompound(j));
				lb.add(clusterIndex == -1 || clusterIndex == i);
			}
		}
		Compound m[] = new Compound[l.size()];
		int selectedIndices[] = CheckBoxSelectDialog.selectIndices(Settings.TOP_LEVEL_FRAME, title, description,
				l.toArray(m), ArrayUtil.toPrimitiveBooleanArray(lb));
		return selectedIndices;
	}

	public void chooseClustersToExport(CompoundProperty compoundDescProp)
	{
		int[] indices = clusterChooser("Export Cluster/s",
				"Select the clusters you want to export. The compounds will be stored in a single SDF/CSV file.");
		if (indices != null)
			ExportData.exportClusters(this, indices, compoundDescProp);
	}

	public void chooseCompoundsToExport(CompoundProperty compoundDescProp)
	{
		int indices[] = selectJmolIndicesWithCompoundChooser("Export Compounds/s",
				"Select the compounds you want to export. The compounds will be stored in a single SDF/CSV file.");
		if (indices == null)
			return;
		List<Integer> l = new ArrayList<Integer>();
		for (int i = 0; i < indices.length; i++)
			l.add(getCompoundWithJmolIndex(indices[i]).getOrigIndex());
		ExportData.exportCompoundsWithOrigIndices(this, l, compoundDescProp);
	}

	public String getName()
	{
		if (clusteringData != null)
			return clusteringData.getName();
		else
			return null;
	}

	public String getFullName()
	{
		if (clusteringData != null)
			return clusteringData.getFullName();
		else
			return null;
	}

	public String getOrigSDFile()
	{
		return clusteringData.getOrigSDF();
	}

	public String getSDFile()
	{
		return clusteringData.getSDF();
	}

	private void checkNoSelection()
	{
		if (compoundActive.getSelected() != -1 || compoundWatched.getSelected() != -1
				|| clusterWatched.getSelected() != -1 || (clusterActive.getSelected() != -1 && getNumClusters() > 1))
			throw new IllegalStateException("clear selection before");
	}

	public void removeCompoundsWithJmolIndices(int jmolIndices[])
	{
		checkNoSelection();

		LinkedHashMap<Cluster, List<Integer>> toDel = new LinkedHashMap<Cluster, List<Integer>>();

		// assign indices to clusters
		for (int i = 0; i < jmolIndices.length; i++)
		{
			Cluster c = getCluster(getClusterIndexForJmolIndex(jmolIndices[i]));
			List<Integer> l = toDel.get(c);
			if (l == null)
			{
				l = new ArrayList<Integer>();
				toDel.put(c, l);
			}
			l.add(jmolIndices[i]);
		}

		// delete clusterwise
		boolean clusterModified = false;
		Cluster clusToDel[] = new Cluster[0];
		for (Cluster c : toDel.keySet())
		{
			int indices[] = ArrayUtil.toPrimitiveIntArray(toDel.get(c));
			if (indices.length == c.getNumCompounds())
				clusToDel = ArrayUtil.concat(Cluster.class, clusToDel, new Cluster[] { c });
			else
			{
				c.removeWithJmolIndices(indices);
				dirty = true;
				clusterModified = true;
			}
		}
		if (clusToDel.length > 0)
			removeCluster(clusToDel);
		if (clusterModified)
		{
			updatePositions();
			fire(CLUSTER_MODIFIED, null, null);
		}
	}

	public SubstructureSmartsType getSubstructureSmartsType()
	{
		return clusteringData.getThreeDAligner().getSubstructureSmartsType();
	}

	public List<CompoundProperty> getFeatures()
	{
		return clusteringData.getFeatures();
	}

	public List<CompoundProperty> getProperties()
	{
		return clusteringData.getProperties();
	}

	public List<CompoundData> getCompounds()
	{
		return clusteringData.getCompounds();
	}

	public int getNumClusters()
	{
		return clusters.size();
	}

	public int getNumCompounds(boolean includingMultiClusteredCompounds)
	{
		return getCompounds(includingMultiClusteredCompounds).size();
	}

	@Override
	public int getNumUnfilteredCompounds(boolean includingMultiClusteredCompounds)
	{
		return clusteringData.getNumCompounds(includingMultiClusteredCompounds);
	}

	public List<Compound> getCompounds(boolean includingMultiClusteredCompounds)
	{
		update();
		if (includingMultiClusteredCompounds)
			return filteredCompoundListIncludingMultiClusteredCompounds;
		else
			return filteredCompoundList;
	}

	public String[] getStringValues(NominalProperty property, Compound excludeCompound)
	{
		return getStringValues(property, excludeCompound, false);
	}

	public String[] getStringValues(NominalProperty property, Compound excludeCompound, boolean formatted)
	{
		List<String> l = new ArrayList<String>();
		for (Compound m : getCompounds(false))
			if (m != excludeCompound && m.getStringValue(property) != null)
				l.add(formatted ? m.getFormattedValue(property) : m.getStringValue(property));
		String v[] = new String[l.size()];
		return l.toArray(v);
	}

	public Double[] getDoubleValues(NumericProperty property)
	{
		Double v[] = new Double[getNumCompounds(false)];
		int i = 0;
		for (Compound m : getCompounds(false))
			v[i++] = m.getDoubleValue(property);
		return v;
	}

	public String getClusterAlgorithm()
	{
		return clusteringData.getDatasetClusterer().getName();
	}

	public boolean isClusterAlgorithmDisjoint()
	{
		return clusteringData.getDatasetClusterer().isDisjointClusterer();
	}

	public String getEmbedAlgorithm()
	{
		return clusteringData.getThreeDEmbedder().getName();
	}

	public String getEmbedQuality()
	{
		return clusteringData.getEmbedQuality();
	}

	@Override
	public boolean isRandomEmbedding()
	{
		return clusteringData != null && clusteringData.getThreeDEmbedder() instanceof Random3DEmbedder;
	}

	@Override
	public Vector3f getCenter(boolean superimposed)
	{
		if (superimposed)
			return superimposedCenter;
		else
			return nonSuperimposedCenter;
	}

	@Override
	public float getDiameter(boolean superimposed)
	{
		if (superimposed)
			return superimposedDiameter;
		else
			return nonSuperimposedDiameter;
	}

	@Override
	public boolean isSuperimposed()
	{
		return superimposed;
	}

	public void setSuperimposed(boolean superimposed)
	{
		this.superimposed = superimposed;
	}

	public synchronized Double getNormalizedDoubleValue(CompoundPropertyOwner m, NumericProperty p)
	{
		return clusteringValues.getNormalizedDoubleValue(m, p);
	}

	public synchronized Double getNormalizedLogDoubleValue(CompoundPropertyOwner m, NumericProperty p)
	{
		return clusteringValues.getNormalizedLogDoubleValue(m, p);
	}

	@Override
	public Double getDoubleValue(NumericProperty p)
	{
		return clusteringValues.getDoubleValue(p);
	}

	@Override
	public String getFormattedValue(CompoundProperty p)
	{
		return getSummaryStringValue(p, false);
	}

	public String getOrigLocalPath()
	{
		return clusteringData.getOrigLocalPath();
	}

	@Override
	public List<CompoundProperty> getAdditionalProperties()
	{
		return clusteringData.getAdditionalProperties();
	}

	public CorrelationProperty getEmbeddingQualityProperty()
	{
		return clusteringData.getEmbeddingQualityProperty();
	}

	@Override
	public EqualPositionProperty getEqualPosProperty()
	{
		return clusteringData.getEqualPosProperty();
	}

	//	public CompoundProperty[] getAppDomainProperties()
	//	{
	//		return clusteringData.getAppDomainProperties();
	//	}
	//
	//	public List<CompoundProperty> getDistanceToProperties()
	//	{
	//		return clusteringData.getDistanceToProperties();
	//	}

	@Override
	public double getSpecificity(CompoundSelection c, CompoundProperty p)
	{
		return clusteringValues.getSpecificity(c, p);
	}

	@Override
	public double getSpecificity(Cluster c, CompoundProperty p)
	{
		return clusteringValues.getSpecificity(c, p);
	}

	public double getSpecificity(Compound m, CompoundProperty p)
	{
		return clusteringValues.getSpecificity(m, p);
	}

	public String getSummaryStringValue(CompoundProperty p, boolean html)
	{
		return clusteringValues.getSummaryStringValue(p, html);
	}

	public void initFeatureNormalization()
	{
		clusteringValues.initFeatureNormalization();
	}

	public Cluster getUniqueClusterForCompounds(Compound[] c)
	{
		Cluster c1 = null;
		for (int i = 0; i < c.length; i++)
		{
			Cluster c2 = getClusterForCompound(c[i]);
			if (c1 == null || c1 == c2)
				c1 = c2;
			else
				return null;
		}
		return c1;
	}

	public Cluster getUniqueClusterForJmolIndices(int[] c)
	{
		Cluster c1 = null;
		for (int i = 0; i < c.length; i++)
		{
			Cluster c2 = getClusterForJmolIndex(c[i]);
			if (c1 == null || c1 == c2)
				c1 = c2;
			else
				return null;
		}
		return c1;
	}

	public Cluster getExactClusterForCompounds(List<Compound> compounds)
	{
		int idx[] = new int[compounds.size()];
		for (int i = 0; i < idx.length; i++)
			idx[i] = compounds.get(i).getJmolIndex();
		return getExactClusterForJmolIndices(idx);
	}

	public Cluster getExactClusterForJmolIndices(int[] jmolIndices)
	{
		Cluster c1 = null;
		for (int i = 0; i < jmolIndices.length; i++)
		{
			Cluster c2 = getClusterForJmolIndex(jmolIndices[i]);
			if (c1 == null || c1 == c2)
				c1 = c2;
			else
				return null;
		}
		if (c1.getNumCompounds() == jmolIndices.length)
			return c1;
		else
			return null;
	}

	/**
	 * warning: requires to update compound positions after compounds are made visible again
	 * 
	 * @param filter
	 */
	public void setCompoundFilter(CompoundFilter filter)
	{
		for (Cluster c : clusters)
			c.setFilter(filter);
		clusteringValues.clear();
		dirty = true;
		//		updatePositions();
	}

	public int getNumOrigCompounds(boolean includingMultiClusteredCompounds)
	{
		return clusteringData.getNumCompounds(includingMultiClusteredCompounds);
	}

	@Override
	public Cluster getActiveCluster()
	{
		return getCluster(getClusterActive().getSelected());
	}

	@Override
	public int getActiveClusterIdx()
	{
		return getClusterActive().getSelected();
	}

	@Override
	public Cluster getWatchedCluster()
	{
		return getCluster(getClusterWatched().getSelected());
	}

	@Override
	public int getWatchedClusterIdx()
	{
		return getClusterWatched().getSelected();
	}

	@Override
	public boolean isCompoundActive()
	{
		return getCompoundActive().getSelected() != -1;
	}

	@Override
	public boolean isCompoundActive(Compound c)
	{
		return getCompoundActive().isSelected(c.getJmolIndex());
	}

	@Override
	public Compound[] getActiveCompounds()
	{
		return getCompoundsWithJmolIndices(getCompoundActive().getSelectedIndices());
	}

	@Override
	public Compound getActiveCompound()
	{
		return getCompoundWithJmolIndex(getCompoundActive().getSelected());
	}

	@Override
	public int[] getActiveCompoundsJmolIdx()
	{
		return getCompoundActive().getSelectedIndices();
	}

	@Override
	public Compound getWatchedCompound()
	{
		return getCompoundWithJmolIndex(getCompoundWatched().getSelected());
	}

	@Override
	public Compound[] getWatchedCompounds()
	{
		return getCompoundsWithJmolIndices(getCompoundWatched().getSelectedIndices());
	}

	@Override
	public int[] getWatchedCompoundsJmolIdx()
	{
		return getCompoundWatched().getSelectedIndices();
	}

	@Override
	public List<CompoundProperty> getPropertiesAndFeatures()
	{
		return ListUtil.concat(getProperties(), getFeatures());
	}

	@Override
	public List<CompoundProperty> selectPropertiesAndFeaturesWithDialog(String title, CompoundProperty preselected,
			boolean addSmiles, boolean addEmbeddingStress, boolean addActivityCliffs, boolean addDistanceTo)
	{
		List<CompoundProperty> props = new ArrayList<CompoundProperty>();
		for (CompoundProperty p : getAdditionalProperties())
		{
			if (addEmbeddingStress && p instanceof CorrelationProperty)
				props.add(p);
			else if (addActivityCliffs && p instanceof SALIProperty)
				props.add(p);
			else if (addDistanceTo && p instanceof DistanceToProperty)
				props.add(p);
		}
		for (CompoundProperty p : getProperties())
			if (addSmiles || (p.getCompoundPropertySet() == null || !p.getCompoundPropertySet().isSmiles()))
				props.add(p);
		for (CompoundProperty p : getFeatures())
			props.add(p);
		if (props.size() == 0)
			return props;
		else
		{
			boolean selection[] = new boolean[props.size()];
			if (preselected == null)
				Arrays.fill(selection, true);
			else
			{
				for (int i = 0; i < props.size(); i++)
					if (preselected == props.get(i))
						selection[i] = true;
			}
			Object sel[] = CheckBoxSelectDialog.select(Settings.TOP_LEVEL_FRAME, title, null,
					ArrayUtil.toArray(CompoundProperty.class, props), selection);
			if (sel == null)
				return null;
			return ArrayUtil.toList(ArrayUtil.cast(CompoundProperty.class, sel));
		}
	}

	@Override
	public void addSelectionListener(final SelectionListener l)
	{
		getClusterActive().addListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				l.clusterActiveChanged(getCluster(getClusterActive().getSelected()));
			}
		});
		getClusterWatched().addListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				l.clusterWatchedChanged(getCluster(getClusterWatched().getSelected()));
			}
		});
		getCompoundActive().addListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				l.compoundActiveChanged(getCompoundsWithJmolIndices(getCompoundActive().getSelectedIndices()));
			}
		});
		getCompoundWatched().addListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				l.compoundWatchedChanged(getCompoundsWithJmolIndices(getCompoundWatched().getSelectedIndices()));
			}
		});
	}

	// hack for BMBF datasets
	@Override
	public boolean isBMBFRealEndpointDataset(boolean filtered)
	{
		if (endpointDataset == null)
		{
			Set<String> realProps = new HashSet<String>();
			Set<String> filledProps = new HashSet<String>();
			Set<String> otherProps = new HashSet<String>();
			for (CompoundProperty p : getProperties())
			{
				String n = p.getName();
				if (n.endsWith("_real"))
					realProps.add(n.substring(0, n.length() - 5));
				else if (n.endsWith("_filled"))
					filledProps.add(n.substring(0, n.length() - 7));
				else
					otherProps.add(n);
			}
			endpointDataset = realProps.size() > 0;
			filledEndpointDataset = filledProps.size() > 0 && filledProps.size() == realProps.size();
			for (String n : realProps)
			{
				if (!otherProps.contains(n))
				{
					endpointDataset = false;
					filledEndpointDataset = false;
					break;
				}
				if (!filledProps.contains(n))
					filledEndpointDataset = false;
			}
		}
		if (filtered)
			return filledEndpointDataset;
		else
			return endpointDataset;
	}

	@Override
	public CompoundProperty addSALIFeatures(CompoundProperty p)
	{
		CompoundProperty oldProps[] = new CompoundProperty[0];
		for (CompoundProperty prop : getAdditionalProperties())
			if (prop instanceof SALIProperty)
				oldProps = ArrayUtil.push(CompoundProperty.class, oldProps, prop);

		Double d[] = new Double[getNumCompounds(true)];
		String domain[] = null;
		if (p instanceof NominalProperty)
		{
			domain = ((NominalProperty) p).getDomain();
			if (domain.length != 2)
				throw new IllegalArgumentException("not yet implemented");
		}

		if (p instanceof NumericProperty)
		{
			int count = 0;
			for (Compound comp : getCompounds(false))
			{
				if (comp.getDoubleValue((NumericProperty) p) != null)
				{
					d[count] = getNormalizedDoubleValue(comp, (NumericProperty) p);
				}
				count++;
			}
		}
		else
		{
			int count = 0;
			for (Compound comp : getCompounds(false))
			{
				if (comp.getStringValue((NominalProperty) p) != null)
					d[count] = (double) ArrayUtil.indexOf(domain, comp.getStringValue((NominalProperty) p));
				count++;
			}
		}

		List<SALIProperty> l = SALIProperty.create(d, clusteringData.getFeatureDistanceMatrix().getValues(),
				p.toString());
		addNewAdditionalProperties(ListUtil.toArray(l), oldProps);
		return l.get(0);
	}

	@Override
	public Double getFeatureDistance(int origIndex, int origIndex2)
	{
		return clusteringData.getFeatureDistance(origIndex, origIndex2);
	}

	@Override
	public CompoundProperty addDistanceToCompoundFeature(Compound comp)
	{
		for (CompoundProperty prop : getAdditionalProperties())
		{
			if (prop instanceof DistanceToProperty && ((DistanceToProperty) prop).getCompound() == comp)
				return prop;
		}
		Double d[] = new Double[getNumUnfilteredCompounds(true)];
		for (int i = 0; i < d.length; i++)
			d[i] = clusteringData.getFeatureDistance(comp.getOrigIndex(), getCompounds().get(i).getOrigIndex());

		DistanceToProperty dp = new DistanceToProperty(comp, clusteringData.getEmbeddingDistanceMeasure(), d);
		addNewAdditionalProperty(dp, null);
		return dp;
	}

	HashMap<NumericProperty, NumericProperty> logProps = new HashMap<NumericProperty, NumericProperty>();

	public static class LogProperty extends DefaultNumericProperty
	{
		public LogProperty(NumericProperty p)
		{
			super("Log(" + p.getName() + ")", "Log conversion (to base 10) of " + p.getName(), ArrayUtil.log(p
					.getDoubleValues()));
		}
	}

	@Override
	public NumericProperty addLogFeature(NumericProperty p)
	{
		if (!logProps.containsKey(p))
		{
			NumericProperty l = new LogProperty(p);
			addNewAdditionalProperty(l, null);
			logProps.put(p, l);
		}
		return logProps.get(p);
	}

	CompoundProperty pred;
	CompoundProperty predMis;

	@Override
	public void predict()
	{
		CompoundProperty clazz = null;
		boolean classification = true;
		for (CompoundProperty p : getProperties())
		{
			if (p.toString().matches("(?i).*activity.*"))
				clazz = p;
			else if (p.toString().equals("LC50_mmol_log"))
			{
				clazz = p;
				classification = false;
			}
		}
		if (clazz == null)
			throw new Error();
		addPredictionFeature(clazz, Predictor.predict(getCompounds(), getFeatures(), clazz, classification));
	}

	@Override
	public void computeAppDomain()
	{
		List<NumericProperty> feats = new ArrayList<NumericProperty>();
		for (CompoundProperty p : getFeatures())
			if (p instanceof NumericProperty && p.numMissingValues() == 0 && p.numDistinctValues() >= 2)
				feats.add((NumericProperty) p);

		//AppDomainComputer appDomain[] = new AppDomainComputer[] { AppDomainHelper.select() };

		List<CompoundProperty> props = new ArrayList<CompoundProperty>();

		AppDomainComputer appDomain[] = AppDomainComputer.APP_DOMAIN_COMPUTERS;
		if (appDomain != null && appDomain.length > 0)
		{
			for (AppDomainComputer ad : appDomain)
			{
				ad.computeAppDomain(getCompounds(), feats, clusteringData.getFeatureDistanceMatrix().getValues());
				props.add(ad.getInsideAppDomainProperty());
				props.add(ad.getPropabilityAppDomainProperty());
			}
		}

		addNewAdditionalProperties(ListUtil.toArray(CompoundProperty.class, props), null);
	}

	@Override
	public void addPredictionFeature(CompoundProperty clazz, PredictionResult p)
	{
		CompoundProperty pred = p.createFeature();
		addNewAdditionalProperty(pred, this.pred);
		this.pred = pred;

		CompoundProperty predMis = p.createMissclassifiedFeature();
		addNewAdditionalProperty(predMis, this.predMis);
		this.predMis = predMis;
	}

	private void addNewAdditionalProperty(CompoundProperty p, CompoundProperty old)
	{
		addNewAdditionalProperties(new CompoundProperty[] { p }, new CompoundProperty[] { old });
	}

	private void addNewAdditionalProperties(CompoundProperty props[], CompoundProperty oldProps[])
	{
		for (CompoundProperty p : props)
		{
			int i = 0;
			for (CompoundData cc : getCompounds())
			{
				CompoundDataImpl c = (CompoundDataImpl) cc;
				if (p instanceof NumericProperty)
				{
					c.setDoubleValue(p, ((NumericProperty) p).getDoubleValues()[i]);
					c.setNormalizedValueCompleteDataset(p, ((NumericProperty) p).getNormalizedValues()[i]);
				}
				else if (p instanceof NominalProperty)
				{
					c.setStringValue(p, ((NominalProperty) p).getStringValues()[i]);
				}
				i++;
			}
		}
		if (oldProps != null)
			for (CompoundProperty old : oldProps)
				clusteringData.getAdditionalProperties().remove(old);
		for (CompoundProperty p : props)
			clusteringData.getAdditionalProperties().add(p);
		dirty = true;
		if (View.instance != null) // for export
			fire(PROPERTY_ADDED, true, false);
	}

	private HashMap<String, CompoundSelection> compoundSelections = new HashMap<String, CompoundSelection>();

	public CompoundSelection getCompoundSelection(Compound[] c)
	{
		StringBuffer key = new StringBuffer();
		for (Compound compound : c)
			key.append(compound.getJmolIndex());
		String k = key.toString();
		if (!compoundSelections.containsKey(k))
		{
			CompoundSelection sel = new CompoundSelection(c);
			clusteringValues.initSelectionNormalization(sel);
			compoundSelections.put(k, sel);
		}
		return compoundSelections.get(k);
	}

	@Override
	public CompoundProperty getHighlightProperty()
	{
		return highlightProperty;
	}

	public void setHighlighProperty(CompoundProperty prop, Color highlightColorText)
	{
		this.highlightProperty = prop;
		this.highlightColorText = highlightColorText;
	}

	@Override
	public Color getHighlightColorText()
	{
		return highlightColorText;
	}

	@Override
	public boolean isSkippingRedundantFeatures()
	{
		return clusteringData.isSkippingRedundantFeatures();
	}

	@Override
	public boolean isBigDataMode()
	{
		return Settings.BIG_DATA;
	}

	public void updateJittering(int level, Set<Compound> compounds)
	{
		if (jittering == null)
			jittering = new JitteringProvider(this);
		jittering.updateJittering(level, compounds);
	}

	public int getJitteringResetLevel(Set<Compound> compounds)
	{
		if (jittering == null)
			return -1;
		return jittering.getJitteringResetLevel(compounds);
	}

	@Override
	public boolean doCheSMappingWarningsExist()
	{
		return clusteringData != null && clusteringData.doCheSMappingWarningsExist();
	}

	@Override
	public void showCheSMappingWarnings()
	{
		clusteringData.showCheSMappingWarnings();
	}
}
