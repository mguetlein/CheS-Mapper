package org.chesmapper.view.gui.util;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.chesmapper.view.cluster.ClusteringImpl;
import org.chesmapper.view.gui.ViewControler;

public class HighlightAutomatic
{
	boolean automatic = false;
	ViewControler controler;
	ClusteringImpl clustering;

	public HighlightAutomatic(ViewControler controler, ClusteringImpl clustering)
	{
		this.controler = controler;
		this.clustering = clustering;

		controler.addViewListener(new PropertyChangeListener()
		{

			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				if (evt.getPropertyName().equals(ViewControler.PROPERTY_HIGHLIGHT_CHANGED))
				{
					update();
				}
			}
		});
		init();
	}

	public void init()
	{
		automatic = true;
	}

	private void update()
	{
		if (controler.getHighlighter() == Highlighter.CLUSTER_HIGHLIGHTER && !clustering.isClusterActive())
			automatic = true;
		else
			automatic = false;
	}

	public boolean resetClusterHighlighter(boolean activeClusterChanged)
	{
		if (automatic && controler.getHighlighter() == Highlighter.DEFAULT_HIGHLIGHTER && activeClusterChanged
				&& clustering.getNumClusters() > 1 && !clustering.isClusterActive())
		{
			controler.setHighlighter(Highlighter.CLUSTER_HIGHLIGHTER, false);
			automatic = true;
			return true;
		}
		return false;
	}

	public boolean resetDefaultHighlighter(boolean activeClusterChanged)
	{
		if (automatic && controler.getHighlighter() == Highlighter.CLUSTER_HIGHLIGHTER && activeClusterChanged
				&& clustering.getNumClusters() > 1 && clustering.isClusterActive())
		{
			controler.setHighlighter(Highlighter.DEFAULT_HIGHLIGHTER, false);
			automatic = true;
			return true;
		}
		return false;
	}

}
