package org.chesmapper.view.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.UUID;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.chesmapper.map.dataInterface.CompoundProperty;
import org.chesmapper.map.dataInterface.CompoundPropertyUtil;
import org.chesmapper.map.dataInterface.FragmentProperty;
import org.chesmapper.map.dataInterface.NominalProperty;
import org.chesmapper.map.dataInterface.NumericProperty;
import org.chesmapper.view.cluster.Clustering;
import org.chesmapper.view.gui.util.CompoundPropertyHighlighter;
import org.chesmapper.view.gui.util.Highlighter;
import org.mg.javalib.gui.CheckBoxSelectPanel;
import org.mg.javalib.gui.ColorSequenceEditor;
import org.mg.javalib.gui.property.ColorGradient;
import org.mg.javalib.gui.property.ColorGradientProperty;
import org.mg.javalib.gui.property.ColorProperty;
import org.mg.javalib.gui.property.PropertyPanel;
import org.mg.javalib.util.ArrayUtil;

public abstract class ColorEditorPanel extends JPanel
{
	CompoundPropertySelector selector;
	ViewControler viewControler;
	Clustering clustering;

	public abstract String getName();

	public abstract boolean hasSelector();

	public abstract boolean isEnabledInDataset();

	public abstract boolean accept(CompoundProperty p);

	public abstract boolean isPropertySelectedInViewer();

	public abstract void applyChanges();

	public ColorEditorPanel(ViewControler viewControler, Clustering clustering)
	{
		this.viewControler = viewControler;
		this.clustering = clustering;

		setLayout(new BorderLayout(10, 10));

		if (hasSelector())
		{
			selector = new CompoundPropertySelector(viewControler);
			add(selector, BorderLayout.NORTH);
		}
	}

	class CompoundPropertySelector extends JPanel
	{
		CheckBoxSelectPanel features;
		boolean hasProps = true;

		public CompoundPropertySelector(ViewControler viewControler)
		{
			ArrayList<CompoundProperty> props = new ArrayList<CompoundProperty>();
			int currentPropIdx = -1;
			CompoundProperty currentProp = viewControler.getHighlightedProperty();
			for (Highlighter hs[] : viewControler.getHighlighters().values())
				for (Highlighter h : hs)
					if (h instanceof CompoundPropertyHighlighter)
					{
						CompoundProperty p = ((CompoundPropertyHighlighter) h).getProperty();
						if (accept(p))
						{
							if (p.equals(currentProp))
								currentPropIdx = props.size();
							props.add(p);
						}
					}
			if (props.size() == 0)
				hasProps = false;
			else
			{
				boolean selected[] = new boolean[props.size()];
				if (currentPropIdx != -1)
					selected[currentPropIdx] = true;
				features = new CheckBoxSelectPanel(null, ArrayUtil.toArray(CompoundProperty.class, props), selected);
				setLayout(new BorderLayout());
				add(features);
			}
		}

		public void addListener(PropertyChangeListener l)
		{
			if (features != null)
				features.addListener(l);
		}

		public boolean hasProps()
		{
			return hasProps;
		}

		public CompoundProperty[] getSelected()
		{
			if (features != null && features.getSelectedIndices().length > 0)
				return ArrayUtil.cast(CompoundProperty.class, features.getSelectedValues());
			else
				return new CompoundProperty[0];
		}
	}

	public static class NumericColorEditorPanel extends ColorEditorPanel
	{
		@Override
		public String getName()
		{
			return "Numeric properties";
		}

		@Override
		public boolean accept(CompoundProperty p)
		{
			return p instanceof NumericProperty;
		}

		@Override
		public boolean hasSelector()
		{
			return true;
		}

		@Override
		public boolean isEnabledInDataset()
		{
			return selector.hasProps;
		}

		@Override
		public boolean isPropertySelectedInViewer()
		{
			return selector.getSelected().length > 0;
		}

		@Override
		public void applyChanges()
		{
			NumericProperty props[] = ArrayUtil.cast(NumericProperty.class, selector.getSelected());
			if (props.length > 0)
				viewControler.setHighlightColors(colorGradientProperty.getValue(), props);
		}

		JCheckBox logHighlighting;
		ColorGradientProperty colorGradientProperty;

		public NumericColorEditorPanel(final ViewControler viewControler, Clustering clus)
		{
			super(viewControler, clus);

			if (!isEnabledInDataset())
				return;

			setBorder(new EmptyBorder(10, 10, 10, 10));
			JPanel p = new JPanel(new BorderLayout(10, 10));

			ColorGradient grad;
			if (isPropertySelectedInViewer()
					&& ((NumericProperty) viewControler.getHighlightedProperty()).getHighlightColorGradient() != null)
				grad = ((NumericProperty) viewControler.getHighlightedProperty()).getHighlightColorGradient();
			else
				grad = ViewControler.DEFAULT_COLOR_GRADIENT;
			colorGradientProperty = new ColorGradientProperty("Color gradient", "Color gradient"
					+ UUID.randomUUID().toString(), grad);
			PropertyPanel p2 = new PropertyPanel(colorGradientProperty);
			p.add(p2, BorderLayout.CENTER);
			add(p, BorderLayout.CENTER);
		}
	}

	public static class NominalColorEditorPanel extends ColorEditorPanel
	{
		@Override
		public String getName()
		{
			return "Nominal properties";
		}

		@Override
		public boolean accept(CompoundProperty p)
		{
			return !p.isUndefined() && p instanceof NominalProperty && !(p instanceof FragmentProperty);
		}

		@Override
		public boolean hasSelector()
		{
			return true;
		}

		@Override
		public boolean isEnabledInDataset()
		{
			return selector.hasProps;
		}

		@Override
		public boolean isPropertySelectedInViewer()
		{
			return selector.getSelected().length > 0;
		}

		@Override
		public void applyChanges()
		{
			NominalProperty props[] = ArrayUtil.cast(NominalProperty.class, selector.getSelected());
			if (props.length > 0)
				viewControler.setHighlightColors(seq.getSequence(), props);
		}

		ColorSequenceEditor seq;

		public NominalColorEditorPanel(final ViewControler viewControler, Clustering clus)
		{
			super(viewControler, clus);

			if (!isEnabledInDataset())
				return;

			setBorder(new EmptyBorder(10, 10, 10, 10));
			Color cols[];
			String labels[] = null;
			if (isPropertySelectedInViewer())
				labels = ((NominalProperty) viewControler.getHighlightedProperty()).getDomain();
			if (isPropertySelectedInViewer()
					&& ((NominalProperty) viewControler.getHighlightedProperty()).getHighlightColorSequence() != null)
				cols = ((NominalProperty) viewControler.getHighlightedProperty()).getHighlightColorSequence();
			else
				cols = CompoundPropertyUtil.AVAILABLE_COLORS;
			seq = new ColorSequenceEditor(cols, labels);
			selector.addListener(new PropertyChangeListener()
			{
				@Override
				public void propertyChange(PropertyChangeEvent evt)
				{
					if (!NominalColorEditorPanel.this.getTopLevelAncestor().isVisible())
						return;
					String labels[] = null;
					if (isPropertySelectedInViewer())
						labels = ((NominalProperty) selector.getSelected()[selector.getSelected().length - 1])
								.getDomain();
					seq.updateLabels(labels);
				}
			});
			seq.setPreferredSize(new Dimension(seq.getPreferredSize().width + 30, 200));
			add(seq, BorderLayout.CENTER);
		}
	}

	public static class ClusterColorEditorPanel extends ColorEditorPanel
	{
		@Override
		public String getName()
		{
			return "Cluster property";
		}

		@Override
		public boolean accept(CompoundProperty p)
		{
			return false;
		}

		@Override
		public boolean hasSelector()
		{
			return false;
		}

		@Override
		public boolean isEnabledInDataset()
		{
			return clustering.getNumClusters() >= 2;
		}

		@Override
		public boolean isPropertySelectedInViewer()
		{
			return viewControler.getHighlighter() == Highlighter.CLUSTER_HIGHLIGHTER;
		}

		@Override
		public void applyChanges()
		{
			viewControler.setClusterColors(seq.getSequence());
		}

		ColorSequenceEditor seq;

		public ClusterColorEditorPanel(final ViewControler viewControler, Clustering clus)
		{
			super(viewControler, clus);

			if (!isEnabledInDataset())
				return;

			String labels[] = new String[CompoundPropertyUtil.CLUSTER_COLORS.length];
			for (int i = 0; i < labels.length; i++)
				if (i < clus.getNumClusters())
					labels[i] = clus.getCluster(i).getName();
			seq = new ColorSequenceEditor(CompoundPropertyUtil.CLUSTER_COLORS, labels);
			seq.setPreferredSize(new Dimension(seq.getPreferredSize().width + 30, 200));
			add(seq, BorderLayout.CENTER);
		}
	}

	public static class SmartsColorEditorPanel extends ColorEditorPanel
	{
		@Override
		public String getName()
		{
			return "SMARTS match properties";
		}

		@Override
		public boolean accept(CompoundProperty p)
		{
			return false;
		}

		@Override
		public boolean hasSelector()
		{
			return false;
		}

		Boolean enabled;

		@Override
		public boolean isEnabledInDataset()
		{
			if (enabled == null)
			{
				enabled = false;
				for (CompoundProperty p : clustering.getFeatures())
					if (p instanceof FragmentProperty)
					{
						enabled = true;
						break;
					}
			}
			return enabled;
		}

		@Override
		public boolean isPropertySelectedInViewer()
		{
			return viewControler.getHighlightedProperty() instanceof FragmentProperty;
		}

		@Override
		public void applyChanges()
		{
			viewControler.setHighlightMatchColors(new Color[] { noMatch.getValue(), match.getValue(),
					matchingAtoms.getValue() });
		}

		ColorProperty noMatch;
		ColorProperty match;
		ColorProperty matchingAtoms;

		public SmartsColorEditorPanel(final ViewControler viewControler, Clustering clus)
		{
			super(viewControler, clus);

			if (!isEnabledInDataset())
				return;

			setBorder(new EmptyBorder(10, 10, 10, 10));
			Color cols[] = CompoundPropertyUtil.HIGHILIGHT_MATCH_COLORS;
			Color dCols[] = CompoundPropertyUtil.DEFAULT_HIGHILIGHT_MATCH_COLORS;
			noMatch = new ColorProperty("no-match", UUID.randomUUID().toString(), cols[0], dCols[0]);
			match = new ColorProperty("match", UUID.randomUUID().toString(), cols[1], dCols[1]);
			matchingAtoms = new ColorProperty("matching fragment", UUID.randomUUID().toString(), cols[2], dCols[2]);
			add(new PropertyPanel(noMatch, match, matchingAtoms), BorderLayout.WEST);
		}
	}
}
