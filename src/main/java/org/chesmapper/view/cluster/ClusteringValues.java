package org.chesmapper.view.cluster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.chesmapper.map.dataInterface.CompoundGroupWithProperties;
import org.chesmapper.map.dataInterface.CompoundProperty;
import org.chesmapper.map.dataInterface.CompoundPropertyOwner;
import org.chesmapper.map.dataInterface.CompoundPropertySpecificity;
import org.chesmapper.map.dataInterface.NominalProperty;
import org.chesmapper.map.dataInterface.NumericProperty;
import org.chesmapper.map.main.TaskProvider;
import org.mg.javalib.util.ArraySummary;
import org.mg.javalib.util.ArrayUtil;
import org.mg.javalib.util.CountedSet;
import org.mg.javalib.util.DoubleArraySummary;
import org.mg.javalib.util.DoubleKeyHashMap;
import org.mg.javalib.util.ListUtil;

public class ClusteringValues
{
	HashMap<CompoundProperty, ArraySummary> summarys = new HashMap<CompoundProperty, ArraySummary>();
	HashMap<NominalProperty, ArraySummary> formattedSummarys = new HashMap<NominalProperty, ArraySummary>();
	DoubleKeyHashMap<CompoundPropertyOwner, CompoundProperty, Double> normalizedValues = new DoubleKeyHashMap<CompoundPropertyOwner, CompoundProperty, Double>();
	DoubleKeyHashMap<CompoundPropertyOwner, CompoundProperty, Double> specificity = new DoubleKeyHashMap<CompoundPropertyOwner, CompoundProperty, Double>();
	DoubleKeyHashMap<CompoundPropertyOwner, CompoundProperty, Double> normalizedLogValues = new DoubleKeyHashMap<CompoundPropertyOwner, CompoundProperty, Double>();

	HashMap<CompoundProperty, double[]> specNumVals = new HashMap<CompoundProperty, double[]>();
	DoubleKeyHashMap<CompoundGroupWithProperties, CompoundProperty, double[]> specNumClusterVals = new DoubleKeyHashMap<CompoundGroupWithProperties, CompoundProperty, double[]>();

	Clustering clustering;

	public ClusteringValues(Clustering clustering)
	{
		this.clustering = clustering;
	}

	void clear()
	{
		normalizedLogValues.clear();
		normalizedValues.clear();
		specificity.clear();
		summarys.clear();
		formattedSummarys.clear();
	}

	private synchronized void updateNormalizedNumericValues(final NumericProperty p)
	{
		Double d[] = new Double[clustering.getCompounds(true).size()];
		int i = 0;
		for (Compound m : clustering.getCompounds(true))
			d[i++] = m.getDoubleValue(p);
		summarys.put(p, DoubleArraySummary.create(d));
		Double valNorm[] = ArrayUtil.normalize(d, false);
		Double valNormLog[] = ArrayUtil.normalizeLog(d, false);
		specNumVals.put(p, ArrayUtil.toPrimitiveDoubleArray(ArrayUtil.removeNullValues(valNorm)));

		normalizedValues.put(clustering, p, DoubleArraySummary.create(valNorm).getMean());
		normalizedLogValues.put(clustering, p, DoubleArraySummary.create(valNormLog).getMean());
		HashMap<Cluster, List<Double>> clusterVals = new HashMap<Cluster, List<Double>>();
		HashMap<Cluster, List<Double>> clusterValsLog = new HashMap<Cluster, List<Double>>();
		for (Cluster c : clustering.getClusters())
		{
			clusterVals.put(c, new ArrayList<Double>());
			clusterValsLog.put(c, new ArrayList<Double>());
		}
		i = 0;
		for (Compound m : clustering.getCompounds(true))
		{
			normalizedValues.put(m, p, valNorm[i]);
			normalizedLogValues.put(m, p, valNormLog[i]);
			if (valNorm[i] != null)
				for (Cluster c : clustering.getClusters())
					if (c.contains(m))
					{
						clusterVals.get(c).add(valNorm[i]);
						clusterValsLog.get(c).add(valNormLog[i]);
					}
			i++;
		}
		for (Cluster c : clustering.getClusters())
		{
			normalizedValues.put(c, p, DoubleArraySummary.create(clusterVals.get(c)).getMean());
			normalizedLogValues.put(c, p, DoubleArraySummary.create(clusterValsLog.get(c)).getMean());
			specNumClusterVals.put(c, p, ArrayUtil.toPrimitiveDoubleArray(clusterVals.get(c)));
		}
	}

	private synchronized void updateNormalizedNumericSelectionValues(NumericProperty p, CompoundSelection s)
	{
		if (!normalizedValues.containsKeyPair(s.getCompounds()[0], p))
			updateNormalizedValues(p);

		List<Double> clusterVals = new ArrayList<Double>();
		List<Double> clusterValsLog = new ArrayList<Double>();
		for (Compound m : s.getCompounds())
		{
			if (getNormalizedDoubleValue(m, p) != null)
			{
				clusterVals.add(getNormalizedDoubleValue(m, p));
				clusterValsLog.add(getNormalizedLogDoubleValue(m, p));
			}
		}
		normalizedValues.put(s, p, DoubleArraySummary.create(clusterVals).getMean());
		normalizedLogValues.put(s, p, DoubleArraySummary.create(clusterValsLog).getMean());
		specNumClusterVals.put(s, p, ArrayUtil.toPrimitiveDoubleArray(clusterVals));
	}

	private synchronized double numericClusterSpec(CompoundGroupWithProperties c, NumericProperty p)
	{
		if (!specificity.containsKeyPair(c, p))
		{
			if (!summarys.containsKey(p))
				updateNormalizedNumericValues(p);
			if (c.getNumCompounds() == 0)
				specificity.put(c, p, CompoundPropertySpecificity.NO_SPEC_AVAILABLE);
			else
				specificity.put(
						c,
						p,
						CompoundPropertySpecificity.numericMultiSpecificty(specNumClusterVals.get(c, p),
								specNumVals.get(p)));
		}
		return specificity.get(c, p);
	}

	private synchronized double numericCompoundSpec(Compound m, NumericProperty p)
	{
		if (!specificity.containsKeyPair(m, p))
		{
			if (!summarys.containsKey(p))
				updateNormalizedNumericValues(p);
			if (normalizedValues.get(m, p) == null)
				specificity.put(m, p, CompoundPropertySpecificity.NO_SPEC_AVAILABLE);
			else
				specificity.put(
						m,
						p,
						CompoundPropertySpecificity.numericSingleSpecificty(normalizedValues.get(m, p),
								specNumVals.get(p)));
		}
		return specificity.get(m, p);
	}

	HashMap<CompoundProperty, List<String>> specNomVals = new HashMap<CompoundProperty, List<String>>();
	HashMap<CompoundProperty, long[]> specNomCounts = new HashMap<CompoundProperty, long[]>();

	private synchronized void updateNormalizedNominalValues(final NominalProperty p)
	{
		String s[] = new String[clustering.getCompounds(true).size()];
		int i = 0;
		for (Compound m : clustering.getCompounds(true))
			s[i++] = m.getStringValue(p);
		CountedSet<String> set = CountedSet.create(s);
		summarys.put(p, set);
		CountedSet<String> fSet = set.copy();
		for (String key : fSet.values())
			fSet.rename(key, p.getFormattedValue(key));
		fSet.setToBack(p.getFormattedNullValue());
		formattedSummarys.put(p, fSet);

		specNomVals.put(p, set.values());
		specNomCounts.put(p, CompoundPropertySpecificity.nominalCounts(specNomVals.get(p), set));
	}

	private synchronized double nominalClusterSpec(CompoundGroupWithProperties c, NominalProperty p)
	{
		if (!specificity.containsKeyPair(c, p))
		{
			if (!summarys.containsKey(p))
				updateNormalizedNominalValues(p);
			if (c.getNumCompounds() == 0)
				specificity.put(c, p, CompoundPropertySpecificity.NO_SPEC_AVAILABLE);
			else
				specificity.put(c, p, CompoundPropertySpecificity.nominalSpecificty(
						CompoundPropertySpecificity.nominalCounts(specNomVals.get(p), c.getNominalSummary(p)),
						specNomCounts.get(p)));
		}
		return specificity.get(c, p);
	}

	private synchronized double nominalCompoundSpec(Compound m, NominalProperty p)
	{
		if (!specificity.containsKeyPair(m, p))
		{
			if (!summarys.containsKey(p))
				updateNormalizedNominalValues(p);
			specificity.put(m, p, CompoundPropertySpecificity.nominalSpecificty(
					CompoundPropertySpecificity.nominalCount(specNomVals.get(p), m.getStringValue(p)),
					specNomCounts.get(p)));
		}
		return specificity.get(m, p);
	}

	private void updateNormalizedValues(CompoundProperty p)
	{
		if (p instanceof NumericProperty)
			updateNormalizedNumericValues((NumericProperty) p);
		else
			updateNormalizedNominalValues((NominalProperty) p);
	}

	private void updateNormalizedSelectionValues(CompoundProperty p, CompoundSelection s)
	{
		if (p instanceof NumericProperty)
			updateNormalizedNumericSelectionValues((NumericProperty) p, s);
		//		else
		//			updateNormalizedNominalSelectionValues(p, s);
	}

	public synchronized double getSpecificity(CompoundGroupWithProperties c, CompoundProperty p)
	{
		if (p instanceof NumericProperty)
			return numericClusterSpec(c, (NumericProperty) p);
		else
			return nominalClusterSpec(c, (NominalProperty) p);
	}

	public synchronized double getSpecificity(Compound m, CompoundProperty p)
	{
		if (p instanceof NumericProperty)
			return numericCompoundSpec(m, (NumericProperty) p);
		else
			return nominalCompoundSpec(m, (NominalProperty) p);
	}

	public synchronized String getSummaryStringValue(CompoundProperty p, boolean html)
	{
		if (!summarys.containsKey(p))
			updateNormalizedValues(p);
		if (p instanceof NominalProperty)
			return formattedSummarys.get(p).toString(html);
		else
			return summarys.get(p).toString(html);
	}

	public synchronized void initFeatureNormalization()
	{
		@SuppressWarnings("unchecked")
		List<CompoundProperty> props = ListUtil.concat(clustering.getProperties(), clustering.getFeatures());
		TaskProvider.debug("Compute feature value statistics");
		for (CompoundProperty p : props)
			updateNormalizedValues(p);
	}

	public synchronized void initSelectionNormalization(CompoundSelection s)
	{
		@SuppressWarnings("unchecked")
		List<CompoundProperty> props = ListUtil.concat(clustering.getProperties(), clustering.getFeatures(),
				clustering.getAdditionalProperties());
		for (CompoundProperty p : props)
			updateNormalizedSelectionValues(p, s);
	}

	public synchronized Double getNormalizedDoubleValue(CompoundPropertyOwner m, NumericProperty p)
	{
		if (!normalizedValues.containsKeyPair(m, p))
			updateNormalizedValues(p);
		return normalizedValues.get(m, p);
	}

	public synchronized Double getNormalizedLogDoubleValue(CompoundPropertyOwner m, NumericProperty p)
	{
		if (!normalizedLogValues.containsKeyPair(m, p))
			updateNormalizedValues(p);
		return normalizedLogValues.get(m, p);
	}

	public Double getDoubleValue(NumericProperty p)
	{
		if (!summarys.containsKey(p))
			updateNormalizedValues(p);
		return ((DoubleArraySummary) summarys.get(p)).getMean();
	}

	@SuppressWarnings("unchecked")
	public CountedSet<String> getNominalSummary(NominalProperty p)
	{
		if (!summarys.containsKey(p))
			updateNormalizedValues(p);
		return (CountedSet<String>) summarys.get(p);
	}
}
