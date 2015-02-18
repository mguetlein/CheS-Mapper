package org.chesmapper.view.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import org.chesmapper.map.dataInterface.CompoundProperty;
import org.chesmapper.map.dataInterface.CompoundPropertyUtil;
import org.chesmapper.map.dataInterface.FragmentProperty;
import org.chesmapper.map.dataInterface.NominalProperty;
import org.chesmapper.map.dataInterface.NumericProperty;
import org.chesmapper.map.gui.SmartsViewDialog;
import org.chesmapper.map.main.ScreenSetup;
import org.chesmapper.map.main.Settings;
import org.chesmapper.view.cluster.Cluster;
import org.chesmapper.view.cluster.ClusterController;
import org.chesmapper.view.cluster.Clustering;
import org.chesmapper.view.cluster.ClusteringImpl;
import org.chesmapper.view.cluster.Compound;
import org.chesmapper.view.cluster.CompoundFilter;
import org.chesmapper.view.cluster.CompoundFilterImpl;
import org.chesmapper.view.cluster.Clustering.SelectionListener;
import org.chesmapper.view.gui.swing.ComponentFactory;
import org.chesmapper.view.gui.swing.TransparentViewPanel;
import org.chesmapper.view.gui.swing.ComponentFactory.ClickableLabel;
import org.chesmapper.view.gui.swing.ComponentFactory.DimensionProvider;
import org.chesmapper.view.gui.util.Highlighter;
import org.mg.javalib.freechart.AbstractFreeChartPanel;
import org.mg.javalib.freechart.HistogramPanel;
import org.mg.javalib.freechart.StackedBarPlot;
import org.mg.javalib.freechart.FreeChartPanel.ChartMouseSelectionListener;
import org.mg.javalib.util.ArrayUtil;
import org.mg.javalib.util.ColorUtil;
import org.mg.javalib.util.CountedSet;
import org.mg.javalib.util.DefaultComparator;
import org.mg.javalib.util.ObjectUtil;
import org.mg.javalib.util.SequentialWorkerThread;
import org.mg.javalib.util.StringUtil;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.Sizes;

public class ChartPanel extends JPanel
{
	Clustering clustering;
	ClusterController clusterControler;
	ViewControler viewControler;
	GUIControler guiControler;

	String plotKey;
	CompoundProperty property;

	private DimensionProvider maxLabelWidth = new DimensionProvider()
	{
		@Override
		public Dimension getPreferredSize(Dimension orig)
		{
			return new Dimension(Math.min(guiControler.getComponentMaxWidth(0.2), orig.width),
					featureNameLabel.getHeight());
		}
	};

	private JPanel featurePanel;
	private JLabel featureNameLabelHeader = ComponentFactory.createViewLabel("<html><b>Feature:</b><html>");
	private JLabel featureNameLabel = ComponentFactory.createViewLabel("", maxLabelWidth);
	private JLabel featureSetLabel = ComponentFactory.createViewLabel("", maxLabelWidth);
	private JLabel featureValuesLabel = ComponentFactory.createViewLabel("", maxLabelWidth);
	private JLabel featureValuesLabelHeader = ComponentFactory.createViewLabel("Values:");
	private JLabel featureDescriptionLabel = ComponentFactory.createViewLabel("", maxLabelWidth);
	private JLabel featureDescriptionLabelHeader = ComponentFactory.createViewLabel("Description:");
	private JLabel featureSmartsLabelHeader = ComponentFactory.createViewLabel("Smarts:");
	private JLabel featureSmartsLabel = ComponentFactory.createViewLabel("", maxLabelWidth);
	private JLabel featureMappingLabelHeader = ComponentFactory.createViewLabel("Usage:");
	private JLabel featureMappingLabel = ComponentFactory.createViewLabel("", maxLabelWidth);
	private JLabel featureMissingLabelHeader = ComponentFactory.createViewLabel("Missing values:");
	private JLabel featureMissingLabel = ComponentFactory.createViewLabel("");
	private JLabel labelList[] = { featureNameLabelHeader, featureNameLabel, featureSetLabel, featureValuesLabel,
			featureValuesLabelHeader, featureDescriptionLabel, featureDescriptionLabelHeader, featureSmartsLabel,
			featureSmartsLabelHeader, featureMappingLabel, featureMappingLabelHeader, featureMissingLabel,
			featureMissingLabelHeader };

	private ClickableLabel clearSelectedFeatureButton;

	HashMap<String, AbstractFreeChartPanel> cardContents = new HashMap<String, AbstractFreeChartPanel>();
	JPanel cardPanel;
	AbstractFreeChartPanel currentChartPanel;

	SequentialWorkerThread workerThread = new SequentialWorkerThread();

	public ChartPanel(Clustering clustering, ViewControler viewControler, ClusterController clusterControler,
			GUIControler guiControler)
	{
		this.clusterControler = clusterControler;
		this.clustering = clustering;
		this.viewControler = viewControler;
		this.guiControler = guiControler;

		buildLayout();
		addListeners();
		update(true);
	}

	private void buildLayout()
	{
		for (JLabel l : labelList)
		{
			l.setHorizontalAlignment(SwingConstants.LEFT);
			l.setVerticalAlignment(SwingConstants.TOP);
			l.setBorder(new EmptyBorder(0, 0, 4, 0));
		}
		clearSelectedFeatureButton = ComponentFactory.createCrossViewButton();
		JPanel nameLabelAndClearButton = new JPanel(new BorderLayout());
		nameLabelAndClearButton.setOpaque(false);
		nameLabelAndClearButton.add(featureNameLabel);
		nameLabelAndClearButton.add(clearSelectedFeatureButton, BorderLayout.EAST);

		DefaultFormBuilder b = new DefaultFormBuilder(new FormLayout("p,3dlu,p"));
		b.setLineGapSize(Sizes.pixel(0));
		b.append(featureNameLabelHeader);
		b.append(nameLabelAndClearButton);
		b.nextLine();
		b.append("");
		b.append(featureSetLabel);
		b.nextLine();
		b.append(featureValuesLabelHeader);
		b.append(featureValuesLabel);
		b.nextLine();
		b.append(featureDescriptionLabelHeader);
		b.append(featureDescriptionLabel);
		b.nextLine();
		b.append(featureSmartsLabelHeader);
		b.append(featureSmartsLabel);
		b.nextLine();
		b.append(featureMappingLabelHeader);
		b.append(featureMappingLabel);
		b.nextLine();
		b.append(featureMissingLabelHeader);
		b.append(featureMissingLabel);
		featurePanel = b.getPanel();
		featurePanel.setOpaque(false);
		JPanel viewFeaturePanel = new TransparentViewPanel();
		viewFeaturePanel.add(featurePanel);
		JPanel wrappedFeaturePanel = new JPanel(new BorderLayout());
		wrappedFeaturePanel.setOpaque(false);
		wrappedFeaturePanel.add(viewFeaturePanel, BorderLayout.EAST);

		setLayout(new BorderLayout(0, 0));
		add(wrappedFeaturePanel, BorderLayout.NORTH);
		cardPanel = new TransparentViewPanel(new CardLayout())
		{
			public Dimension getPreferredSize()
			{
				Dimension dim = super.getPreferredSize();
				dim.width = Math.min(dim.width, guiControler.getComponentMaxWidth(0.33));
				dim.height = Math.min(dim.height, Math.min(guiControler.getComponentMaxHeight(0.25), dim.width));
				return dim;
			}
		};
		viewControler.addIgnoreMouseMovementComponents(cardPanel);
		add(cardPanel, BorderLayout.CENTER);
		setOpaque(false);

		//		SwingUtil.setDebugBorder(viewFeaturePanel, Color.RED);
		//		SwingUtil.setDebugBorder(wrappedFeaturePanel, Color.MAGENTA);
		//		SwingUtil.setDebugBorder(cardPanel, Color.CYAN);
	}

	private void addListeners()
	{
		viewControler.addViewListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				if (evt.getPropertyName().equals(ViewControler.PROPERTY_HIGHLIGHT_CHANGED)
						|| evt.getPropertyName().equals(ViewControler.PROPERTY_HIGHLIGHT_CHANGED)
						|| evt.getPropertyName().equals(ViewControler.PROPERTY_HIGHLIGHT_COLORS_CHANGED)
						|| evt.getPropertyName().equals(ViewControler.PROPERTY_COMPOUND_FILTER_CHANGED))
				{
					update(false);
				}
				else if (evt.getPropertyName().equals(ViewControler.PROPERTY_BACKGROUND_CHANGED))
				{
					if (currentChartPanel != null)
						currentChartPanel.setForegroundColor(ComponentFactory.FOREGROUND);
				}
				else if (evt.getPropertyName().equals(ViewControler.PROPERTY_FONT_SIZE_CHANGED))
				{
					update(false);
				}
			}
		});
		clustering.addListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				if (evt.getPropertyName().equals(ClusteringImpl.CLUSTER_MODIFIED))
				{
					update(true);
				}
				else if (evt.getPropertyName().equals(ClusteringImpl.CLUSTER_REMOVED)
						|| evt.getPropertyName().equals(ClusteringImpl.CLUSTER_CLEAR)
						|| evt.getPropertyName().equals(ClusteringImpl.CLUSTER_ADDED))
				{
					cardPanel.removeAll();
					cardContents.clear();
					update(true);
				}
			}
		});
		clustering.addSelectionListener(new SelectionListener()
		{
			@Override
			public void compoundWatchedChanged(Compound[] c)
			{
				update(false);
			}

			@Override
			public void compoundActiveChanged(Compound[] c)
			{
				update(false);
			}

			@Override
			public void clusterWatchedChanged(Cluster c)
			{
				update(false);
			}

			@Override
			public void clusterActiveChanged(Cluster c)
			{
				update(false);
			}
		});

		featureSmartsLabel.setIcon(ComponentFactory.createViewStringImageIcon("show"));
		featureSmartsLabel.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (property instanceof FragmentProperty)
				{
					SmartsViewDialog.show(Settings.TOP_LEVEL_FRAME, ((FragmentProperty) property).getSmarts(),
							Settings.TOP_LEVEL_FRAME.getWidth(), Settings.TOP_LEVEL_FRAME.getHeight());
				}
			}
		});

		clearSelectedFeatureButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				viewControler.setHighlighter(Highlighter.DEFAULT_HIGHLIGHTER);
			}
		});
	}

	private static String getKey(Cluster c, CompoundProperty p, Compound m[], CompoundFilter f, int fontSize)
	{
		String mString = "";
		for (Compound compound : m)
			mString = mString.concat(compound.toString());
		String cString = "";
		if (p instanceof FragmentProperty)
			cString += CompoundPropertyUtil.HIGHILIGHT_MATCH_COLORS;
		if (p instanceof NominalProperty)
			cString += ((NominalProperty) p).getHighlightColorSequence();
		return (c == null ? "null" : c.getName()) + "_" + p.toString() + "_" + cString + " " + mString.hashCode() + "_"
				+ (f == null ? "" : f.hashCode()) + "_" + fontSize;
	}

	static boolean selfUpdate = false;
	HashSet<Compound> selfUpdateCompounds = null;

	private abstract class CompoundSelector implements ChartMouseSelectionListener
	{
		protected abstract boolean hasSelectionCriterionChanged();

		protected abstract void updateSelectionCriterion();

		protected abstract String selectionCriterionToString(CompoundProperty p);

		protected abstract boolean isSelected(Compound m, CompoundProperty p);

		@Override
		public void hoverEvent()
		{
			handleEvent(true, false, false);
		}

		@Override
		public void clickEvent(boolean ctrlDown, boolean doubleClick)
		{
			viewControler.clearMouseMoveWatchUpdates(false);
			handleEvent(false, ctrlDown, doubleClick);
		}

		private void handleEvent(final boolean hover, final boolean ctrlDown, final boolean doubleClick)
		{
			if (selfUpdate)
			{
				return;
			}
			selfUpdate = true;
			Thread th = new Thread(new Runnable()
			{
				public void run()
				{
					clusterControler.clearClusterWatched();
					try
					{
						if (!hasSelectionCriterionChanged() && hover)
							return;
						updateSelectionCriterion();

						CompoundProperty prop = viewControler.getHighlightedProperty();
						final HashSet<Compound> comps = new HashSet<Compound>();
						Iterable<Compound> compounds;
						if (clustering.isClusterActive())
							compounds = clustering.getActiveCluster().getCompounds();
						else
							compounds = clustering.getCompounds(false);
						for (Compound compound : compounds)
							if (isSelected(compound, prop))
								comps.add(compound);

						if (hover)
						{
							if (ObjectUtil.equals(selfUpdateCompounds, comps))
								return;
							selfUpdateCompounds = comps;

							if (comps.size() == 0)
								viewControler.clearMouseMoveWatchUpdates(true);
							else
								viewControler.doMouseMoveWatchUpdates(new Runnable()
								{
									@Override
									public void run()
									{
										clusterControler.setCompoundWatched(ArrayUtil.toArray(comps));
									}
								});
						}
						else
						{
							if (comps.size() == 0)
								clusterControler.clearCompoundActive(true);
							else
							{
								if (ctrlDown)
								{
									for (Compound compound : clustering.getActiveCompounds())
										if (comps.contains(compound))
											comps.remove(compound);
										else
											comps.add(compound);
								}

								if (doubleClick)
								{
									CompoundFilter compoundFilter = new CompoundFilterImpl(clustering,
											new ArrayList<Compound>(comps), selectionCriterionToString(prop));
									clusterControler.setCompoundFilter(compoundFilter, true);
								}
								else
								{
									if (comps.size() == 0)
										clusterControler.clearCompoundActive(true);
									else
										clusterControler.setCompoundActive(ArrayUtil.toArray(comps), true);
								}
							}
						}
					}
					finally
					{
						workerThread.addJob(new Runnable()
						{
							@Override
							public void run()
							{
								selfUpdate = false;
							}
						}, "selfUpdate false after chart was created!");
					}
				}
			});
			th.start();
		}
	}

	double selectedMin = 1.0;
	double selectedMax = 0.0;

	private class NumericCompoundSelector extends CompoundSelector
	{
		HistogramPanel hist;

		public NumericCompoundSelector(HistogramPanel hist)
		{
			this.hist = hist;
		}

		@Override
		public String toString()
		{
			return "selector " + hist.hashCode();
		}

		@Override
		protected boolean hasSelectionCriterionChanged()
		{
			return selectedMin != hist.getSelectedMin() || selectedMax != hist.getSelectedMax();
		}

		@Override
		protected void updateSelectionCriterion()
		{
			selectedMin = hist.getSelectedMin();
			selectedMax = hist.getSelectedMax();
		}

		@Override
		protected String selectionCriterionToString(CompoundProperty p)
		{
			return StringUtil.formatDouble(selectedMin) + " <= " + p + " <= " + StringUtil.formatDouble(selectedMax);
		}

		@Override
		protected boolean isSelected(Compound m, CompoundProperty p)
		{
			Double d = m.getDoubleValue((NumericProperty) p);
			return d != null && d >= selectedMin && d <= selectedMax;
		}
	}

	private abstract class PlotData
	{
		AbstractFreeChartPanel plot;

		public AbstractFreeChartPanel getPlot()
		{
			return plot;
		}
	}

	private class NumericPlotData extends PlotData
	{
		List<String> captions;
		List<double[]> vals;

		public NumericPlotData(Cluster c, NumericProperty p, Compound m[], int fontsize)
		{
			Double v[] = clustering.getDoubleValues(p);
			captions = new ArrayList<String>();
			vals = new ArrayList<double[]>();
			captions.add("Dataset");
			vals.add(ArrayUtil.toPrimitiveDoubleArray(ArrayUtil.removeNullValues(v)));
			if (c != null)
			{
				captions.add(c.toString());
				vals.add(ArrayUtil.toPrimitiveDoubleArray(ArrayUtil.removeNullValues(c.getDoubleValues(p))));
			}

			Double mVals[] = new Double[m.length];
			boolean notNull = false;
			for (int i = 0; i < mVals.length; i++)
			{
				mVals[i] = m[i].getDoubleValue(p);
				notNull |= mVals[i] != null;
			}
			if (m.length > 0 && notNull)
			{
				if (m.length == 1)
					captions.add(m[0].toString());
				else
					captions.add("Selected compounds");
				vals.add(ArrayUtil.toPrimitiveDoubleArray(ArrayUtil.removeNullValues(mVals)));
			}

			plot = new HistogramPanel(null, null, null, "#compounds", captions, vals, 20);
			configurePlotColors(plot, c, m, p, fontsize);
			SwingUtilities.invokeLater(new Runnable()// wait to add listerner, so that chart has finished painting 
					{
						@Override
						public void run()
						{
							plot.addSelectionListener(new NumericCompoundSelector((HistogramPanel) plot));
						}
					});
		}
	}

	String selectedCategory;

	private class NominalCompoundSelector extends CompoundSelector
	{
		StackedBarPlot bar;

		public NominalCompoundSelector(StackedBarPlot bar)
		{
			this.bar = bar;
		}

		@Override
		public String toString()
		{
			return "selector " + bar.hashCode();
		}

		@Override
		protected boolean hasSelectionCriterionChanged()
		{
			return !ObjectUtil.equals(selectedCategory, bar.getSelectedCategory());
		}

		@Override
		protected void updateSelectionCriterion()
		{
			selectedCategory = bar.getSelectedCategory();
		}

		@Override
		protected String selectionCriterionToString(CompoundProperty p)
		{
			return p + " = " + selectedCategory;
		}

		@Override
		protected boolean isSelected(Compound m, CompoundProperty p)
		{
			return selectedCategory != null && selectedCategory.equals(m.getFormattedValue(p));
		}
	}

	private class NominalPlotData extends PlotData
	{
		LinkedHashMap<String, List<Double>> data;
		String vals[];

		public NominalPlotData(Cluster c, NominalProperty p, Compound ms[], int fontsize)
		{
			// TODO rewrite this code
			// * add counted-set for compounds as well?
			// * remove old getStringValues() method (second param is not needed)
			// * fix order

			String v[] = clustering.getStringValues(p, null);
			CountedSet<String> datasetSet = CountedSet.fromArray(v);
			List<String> datasetValues = datasetSet.values(new DefaultComparator<String>());

			CountedSet<String> clusterSet = null;
			if (c != null)
			{
				v = c.getStringValues(p, null);
				clusterSet = CountedSet.fromArray(v);
			}

			data = new LinkedHashMap<String, List<Double>>();
			double compoundCounts[] = new double[datasetValues.size()];
			int count = 0;
			for (String o : datasetValues)
			{
				for (Compound m : ms)
					if (ObjectUtil.equals(m.getStringValue(p), o))
						compoundCounts[count]++;
				count++;
			}
			if (ms.length == 1)
				data.put(ms[0].toString(), ArrayUtil.toList(compoundCounts));
			else if (ms.length > 1)
				data.put("Selected compounds", ArrayUtil.toList(compoundCounts));
			if (c != null)
			{
				List<Double> clusterCounts = new ArrayList<Double>();
				for (String o : datasetValues)
					clusterCounts.add((double) clusterSet.getCount(o));
				data.put(c.toString(), clusterCounts);
			}
			List<Double> datasetCounts = new ArrayList<Double>();
			for (String o : datasetValues)
				datasetCounts.add((double) datasetSet.getCount(o));
			data.put("Dataset", datasetCounts);

			vals = new String[datasetValues.size()];
			//			datasetValues.toArray(vals);
			for (int i = 0; i < vals.length; i++)
				vals[i] = p.getFormattedValue(datasetValues.get(i));

			plot = new StackedBarPlot(null, null, "#compounds", StackedBarPlot.convertTotalToAdditive(data), vals);
			configurePlotColors(plot, c, ms, p, fontsize);
			SwingUtilities.invokeLater(new Runnable() // wait to add listerner, so that chart has finished painting 
					{
						@Override
						public void run()
						{
							plot.addSelectionListener(new NominalCompoundSelector((StackedBarPlot) plot));
						}
					});

		}
	}

	private void configurePlotColors(AbstractFreeChartPanel chartPanel, Cluster cluster, Compound[] compounds,
			CompoundProperty property, int fontsize)
	{
		int dIndex = -1;
		int cIndex = -1;
		int mIndex = -1;

		if (cluster == null)
		{
			if (compounds.length == 0)
				dIndex = 0;
			else
			{
				mIndex = 0;
				dIndex = 1;
			}
		}
		else
		{
			if (compounds.length == 0)
			{
				cIndex = 0;
				dIndex = 1;
			}
			else
			{
				mIndex = 0;
				cIndex = 1;
				dIndex = 2;
			}
		}

		if (chartPanel instanceof StackedBarPlot)
		{
			Color cols[] = CompoundPropertyUtil.getNominalColors((NominalProperty) property);
			if (dIndex == 0)
			{
				chartPanel.setSeriesColor(0, ColorUtil.grayscale(cols[0]));
				((StackedBarPlot) chartPanel).setSeriesCategoryColors(0, cols);
			}
			else if (dIndex == 1)
			{
				chartPanel.setSeriesColor(1, ColorUtil.grayscale(cols[0].darker().darker().darker()));
				chartPanel.setSeriesColor(0, ColorUtil.grayscale(cols[0]).brighter());

				((StackedBarPlot) chartPanel).setSeriesCategoryColors(1,
						ColorUtil.darker(ColorUtil.darker(ColorUtil.darker(cols))));
				((StackedBarPlot) chartPanel).setSeriesCategoryColors(0, ColorUtil.brighter(cols));
			}
			else
			{
				chartPanel.setSeriesColor(2, ColorUtil.grayscale(cols[0].darker().darker().darker()));
				chartPanel.setSeriesColor(1, ColorUtil.grayscale(cols[0]).brighter());
				chartPanel.setSeriesColor(0, ColorUtil.grayscale(cols[0]).brighter().brighter().brighter().brighter());

				((StackedBarPlot) chartPanel).setSeriesCategoryColors(2,
						ColorUtil.darker(ColorUtil.darker(ColorUtil.darker(cols))));
				((StackedBarPlot) chartPanel).setSeriesCategoryColors(1, ColorUtil.brighter(cols));
				((StackedBarPlot) chartPanel).setSeriesCategoryColors(0,
						ColorUtil.brighter(ColorUtil.brighter(ColorUtil.brighter(ColorUtil.brighter(cols)))));
			}
		}
		else
		{
			if (cIndex == -1)
				chartPanel.setSeriesColor(dIndex, CompoundPropertyUtil.getNumericChartColor());
			else
			{
				chartPanel.setSeriesColor(dIndex, CompoundPropertyUtil.getNumericChartColor().darker().darker()
						.darker());
				chartPanel.setSeriesColor(cIndex, CompoundPropertyUtil.getNumericChartColor().brighter());
			}

			if (mIndex != -1)
				chartPanel.setSeriesColor(mIndex, CompoundPropertyUtil.getNumericChartHighlightColor());
		}

		chartPanel.setOpaqueFalse();
		chartPanel.setForegroundColor(ComponentFactory.FOREGROUND);
		chartPanel.setShadowVisible(false);
		chartPanel.setIntegerTickUnits();
		chartPanel.setBarWidthLimited();
		chartPanel.setFontSize(fontsize);
	}

	private void update(final boolean force)
	{
		if (!SwingUtilities.isEventDispatchThread())
			throw new IllegalStateException("GUI updates only in event dispatch thread plz");

		if (clustering.getNumClusters() == 0)
		{
			setVisible(false);
			return;
		}

		CompoundProperty prop = viewControler.getHighlightedProperty();
		Cluster c = clustering.getActiveCluster();
		if (c == null)
			c = clustering.getWatchedCluster();
		if (clustering.getNumClusters() == 1)
			c = null;

		Compound comps[] = clustering.getActiveCompounds();
		if (comps.length == 0)
			comps = clustering.getWatchedCompounds();

		if (prop instanceof NominalProperty && comps.length == 1)
		{
			//does NOT help much in terms of visualisation for 1 compound (color code should be enough)
			//difficult to realize in terms of color brightness
			comps = new Compound[0];
		}

		String key;
		if (prop == null)
			key = "";
		else
			key = getKey(c, prop, comps, clusterControler.getCompoundFilter(), ScreenSetup.INSTANCE.getFontSize());

		if (force || property != prop || !plotKey.equals(key))
		{
			property = prop;
			plotKey = key;

			if (property == null)
				setVisible(false);
			else
			{
				final CompoundProperty fProperty = property;
				final String fPlotKey = plotKey;

				final Cluster fCluster = c;
				final Compound fCompounds[] = comps;
				final int fFontSize = ScreenSetup.INSTANCE.getFontSize();

				workerThread.addJob(new Runnable()
				{
					public void run()
					{
						if (fProperty != property || !fPlotKey.equals(plotKey))
							return;

						if (force && cardContents.containsKey(plotKey))
							cardContents.remove(plotKey);
						if (!cardContents.containsKey(plotKey))
						{
							PlotData d = null;
							if (!fProperty.isUndefined())
							{
								if (fProperty instanceof NominalProperty)
									d = new NominalPlotData(fCluster, (NominalProperty) fProperty, fCompounds,
											fFontSize);
								else if (fProperty instanceof NumericProperty)
									d = new NumericPlotData(fCluster, (NumericProperty) fProperty, fCompounds,
											fFontSize);
							}
							if (d != null)
							{
								cardContents.put(plotKey, d.getPlot());
								cardPanel.add(d.getPlot(), plotKey);
							}
						}

						if (fProperty != property || !fPlotKey.equals(plotKey))
							return;
						setIgnoreRepaint(true);

						featureNameLabel.setText(fProperty.toString());
						//hack, do not show integrated features, cdk features or openbabel features
						if (fProperty.getCompoundPropertySet() != null
								&& fProperty.getCompoundPropertySet().isSizeDynamic())
						{
							featureSetLabel.setText(fProperty.getCompoundPropertySet().toString());
							featureSetLabel.setVisible(true);
						}
						else
							featureSetLabel.setVisible(false);
						featureValuesLabel.setText("<html>" + clustering.getSummaryStringValue(fProperty, true)
								+ "</html>");
						featureDescriptionLabel.setText(fProperty.getDescription() + "");
						featureDescriptionLabel.setVisible(fProperty.getDescription() != null);
						featureDescriptionLabelHeader.setVisible(fProperty.getDescription() != null);
						if (fProperty instanceof FragmentProperty)
						{
							featureSmartsLabel.setText(((FragmentProperty) fProperty).getSmarts() + "");
							featureSmartsLabel.setVisible(true);
							featureSmartsLabelHeader.setVisible(true);
						}
						else
						{
							featureSmartsLabel.setVisible(false);
							featureSmartsLabelHeader.setVisible(false);
						}
						String usage;
						if (fProperty.getCompoundPropertySet() != null
								&& fProperty.getCompoundPropertySet().isSelectedForMapping())
						{
							if (fProperty.numDistinctValues() <= 1)
								usage = "Ignored for mapping (equal value for each compound)";
							else if (fProperty.getRedundantProp() != null)
								usage = "Ignored for mapping (redundant to " + fProperty.getRedundantProp() + ")";
							else
								usage = "Used for mapping";
						}
						else
							usage = "NOT used for mapping";
						featureMappingLabel.setText(usage);
						featureMissingLabelHeader.setVisible(clustering.numMissingValues(fProperty) > 0);
						featureMissingLabel.setVisible(clustering.numMissingValues(fProperty) > 0);
						featureMissingLabel.setText(clustering.numMissingValues(fProperty) + "");

						if (cardContents.containsKey(plotKey))
						{
							cardPanel.setVisible(true);
							currentChartPanel = cardContents.get(plotKey);
							currentChartPanel.setFontSize(ScreenSetup.INSTANCE.getFontSize());
							((CardLayout) cardPanel.getLayout()).show(cardPanel, plotKey);
						}
						else
							cardPanel.setVisible(false);
						revalidate();
						setVisible(true);
						setIgnoreRepaint(false);
						repaint();
					}
				}, "update chart");
			}
		}
	}
}
