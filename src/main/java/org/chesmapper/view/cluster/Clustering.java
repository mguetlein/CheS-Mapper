package org.chesmapper.view.cluster;

import java.awt.Color;
import java.beans.PropertyChangeListener;
import java.util.List;

import org.chesmapper.map.alg.embed3d.CorrelationProperty;
import org.chesmapper.map.alg.embed3d.EqualPositionProperty;
import org.chesmapper.map.dataInterface.CompoundData;
import org.chesmapper.map.dataInterface.CompoundGroupWithProperties;
import org.chesmapper.map.dataInterface.CompoundProperty;
import org.chesmapper.map.dataInterface.CompoundPropertyOwner;
import org.chesmapper.map.dataInterface.NominalProperty;
import org.chesmapper.map.dataInterface.NumericProperty;
import org.chesmapper.map.weka.Predictor.PredictionResult;

public interface Clustering extends CompoundGroupWithProperties
{
	void addListener(PropertyChangeListener propertyChangeListener);

	int getNumClusters();

	int numClusters();

	List<Cluster> getClusters();

	boolean isClusterActive();

	boolean isClusterWatched();

	Cluster getCluster(int i);

	int indexOf(Cluster cluster);

	Cluster getActiveCluster();

	Cluster getWatchedCluster();

	int getActiveClusterIdx();

	int getWatchedClusterIdx();

	boolean isCompoundActive();

	boolean isCompoundWatched();

	boolean isCompoundActive(Compound c);

	Compound[] getActiveCompounds();

	Compound getActiveCompound();

	int[] getActiveCompoundsJmolIdx();

	Compound getWatchedCompound();

	Compound[] getWatchedCompounds();

	int[] getWatchedCompoundsJmolIdx();

	Cluster getClusterForCompound(Compound c);

	List<CompoundProperty> selectPropertiesAndFeaturesWithDialog(String title, CompoundProperty preselected,
			boolean addSmiles, boolean addEmbeddingStress, boolean addActivityCliffs, boolean addDistanceTo);

	List<CompoundProperty> getPropertiesAndFeatures();

	List<CompoundProperty> getProperties();

	List<CompoundProperty> getFeatures();

	List<Compound> getCompounds(boolean includingMultiClusteredCompounds);

	Cluster getUniqueClusterForCompounds(Compound[] c);

	String getOrigLocalPath();

	String getOrigSDFile();

	String getSDFile();

	boolean isClusterAlgorithmDisjoint();

	String getClusterAlgorithm();

	int getClusterIndexForCompound(Compound m);

	List<CompoundData> getCompounds();

	void chooseClustersToExport(CompoundProperty compoundDescriptor);

	void chooseCompoundsToExport(CompoundProperty compoundDescriptor);

	Double[] getDoubleValues(NumericProperty p);

	String[] getStringValues(NominalProperty p, Compound m);

	String getSummaryStringValue(CompoundProperty p, boolean b);

	int numMissingValues(CompoundProperty p);

	String getName();

	Double getNormalizedLogDoubleValue(CompoundPropertyOwner m, NumericProperty p);

	Double getNormalizedDoubleValue(CompoundPropertyOwner m, NumericProperty p);

	double getSpecificity(Compound compound, CompoundProperty p);

	double getSpecificity(Cluster cluster, CompoundProperty p);

	double getSpecificity(CompoundSelection sel, CompoundProperty p);

	int getNumCompounds(boolean includingMultiClusteredCompounds);

	int getNumUnfilteredCompounds(boolean includingMultiClusteredCompounds);

	String getEmbedAlgorithm();

	String getEmbedQuality();

	List<CompoundProperty> getAdditionalProperties();

	CorrelationProperty getEmbeddingQualityProperty();

	EqualPositionProperty getEqualPosProperty();

	Compound getCompoundWithJmolIndex(int convertRowIndexToModel);

	int numDistinctValues(CompoundProperty p);

	public void addSelectionListener(SelectionListener l);

	public abstract static class SelectionListener
	{
		public void clusterActiveChanged(Cluster c)
		{
		}

		public void clusterWatchedChanged(Cluster c)
		{
		}

		public void compoundActiveChanged(Compound c[])
		{
		}

		public void compoundWatchedChanged(Compound c[])
		{
		}
	}

	boolean isBMBFRealEndpointDataset(boolean b);

	CompoundProperty addDistanceToCompoundFeature(Compound c);

	CompoundProperty addSALIFeatures(CompoundProperty c);

	void predict();

	void addPredictionFeature(CompoundProperty clazz, PredictionResult p);

	NumericProperty addLogFeature(NumericProperty p);

	public CompoundSelection getCompoundSelection(Compound[] c);

	boolean isRandomEmbedding();

	CompoundProperty getHighlightProperty();

	Color getHighlightColorText();

	Double getFeatureDistance(int origIndex, int origIndex2);

	boolean isSkippingRedundantFeatures();

	boolean isBigDataMode();

	void computeAppDomain();

	boolean doCheSMappingWarningsExist();

	void showCheSMappingWarnings();

}
