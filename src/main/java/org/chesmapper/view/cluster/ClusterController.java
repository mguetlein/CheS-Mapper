package org.chesmapper.view.cluster;

import java.util.List;

public interface ClusterController
{
	public void clearClusterActive(boolean animate, boolean clearCompoundActive);

	public void clearClusterWatched();

	public void clearCompoundActive(boolean animate);

	public void clearCompoundWatched();

	public void setClusterActive(Cluster c, boolean animate, boolean clearCompoundActive);

	public void setClusterWatched(Cluster c);

	public void setCompoundActive(Compound c, boolean animate);

	public void setCompoundActive(Compound[] c, boolean animate);

	public void toggleCompoundActive(Compound c);

	public void setCompoundWatched(Compound... c);

	public CompoundFilter getCompoundFilter();

	public void applyCompoundFilter(List<Compound> compounds, boolean accept);

	public void setCompoundFilter(CompoundFilter filter, boolean animate);

	public void useSelectedCompoundsAsFilter(boolean animate);

	public void removeCompounds(Compound[] c);

	public void removeCluster(Cluster... c);

	public void chooseClustersToRemove();

	public void chooseCompoundsToRemove();

	public void chooseClustersToFilter();

	public void chooseCompoundsToFilter();

	public void newClustering();

}
