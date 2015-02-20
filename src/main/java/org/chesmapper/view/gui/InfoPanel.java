package org.chesmapper.view.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import org.chesmapper.map.data.CDKCompoundIcon;
import org.chesmapper.map.dataInterface.CompoundProperty;
import org.chesmapper.map.dataInterface.CompoundPropertyOwner;
import org.chesmapper.map.dataInterface.CompoundPropertyUtil;
import org.chesmapper.map.dataInterface.SubstructureSmartsType;
import org.chesmapper.view.cluster.Cluster;
import org.chesmapper.view.cluster.ClusterController;
import org.chesmapper.view.cluster.Clustering;
import org.chesmapper.view.cluster.ClusteringImpl;
import org.chesmapper.view.cluster.Compound;
import org.chesmapper.view.cluster.CompoundSelection;
import org.chesmapper.view.cluster.SALIProperty;
import org.chesmapper.view.cluster.Clustering.SelectionListener;
import org.chesmapper.view.gui.ViewControler.FeatureFilter;
import org.chesmapper.view.gui.ViewControler.Style;
import org.chesmapper.view.gui.swing.ComponentFactory;
import org.chesmapper.view.gui.util.Highlighter;
import org.mg.javalib.gui.BorderImageIcon;
import org.mg.javalib.gui.MultiImageIcon;
import org.mg.javalib.gui.MultiImageIcon.Layout;
import org.mg.javalib.gui.MultiImageIcon.Orientation;
import org.mg.javalib.util.ImageLoader;
import org.mg.javalib.util.ListUtil;
import org.mg.javalib.util.SwingUtil;
import org.mg.javalib.util.ThreadUtil;
import org.mg.javalib.util.ImageLoader.Image;

public class InfoPanel extends JPanel
{
	private static final boolean USE_GLOBAL_MIN_WIDTH = true;

	private static double getMaxSizeTotal()
	{
		return ComponentSize.INFO_TABLE.getValue() * 0.01;
	}

	private static double getMaxSizeCol(int col, boolean interactive)
	{
		if (interactive)
		{
			if (col == 0)
				return getMaxSizeTotal() * 0.60;
			else
				return 100.0;
		}
		else
		{
			return getMaxSizeTotal() * 0.5;
		}

	}

	Clustering clustering;
	ClusterController clusterControler;
	ViewControler viewControler;
	GUIControler guiControler;

	JPanel clusterCompoundPanel;
	CardLayout clusterCompoundLayout;
	final static String CARD_CLUSTERING = "clustering";
	final static String CARD_CLUSTER = "cluster";
	final static String CARD_SELECTION = "selection";
	final static String CARD_COMPOUND = "compound";
	String currentCard;
	int globalMinWidth;

	HashMap<String, TablePanel> cardToPanel = new HashMap<String, TablePanel>();

	JPanel compoundIconPanel;
	JLabel compoundIconLabel = new JLabel("");

	boolean selfUpdate = false;

	static class Info
	{
		String name;
		Object value;
		Highlighter highlighter = Highlighter.DEFAULT_HIGHLIGHTER;

		public Info(String name, Object value)
		{
			this.name = name;
			this.value = value;
		}

		public Info(String name, Object value, Highlighter highlighter)
		{
			this(name, value);
			this.highlighter = highlighter;
		}

		@Override
		public String toString()
		{
			return name;
		}

		public static int find(List<Info> l, Highlighter h)
		{
			if (l == null || h == Highlighter.DEFAULT_HIGHLIGHTER)
				return -1;
			for (int i = 0; i < l.size(); i++)
				if (l.get(i).highlighter == h)
					return i;
			return -1;
		}
	}

	public class ClusteringTablePanel extends TablePanel
	{
		protected Clustering selected;

		@Override
		public List<Info> getAdditionalInfo()
		{
			List<Info> map = new ArrayList<Info>();
			map.add(new Info("Num compounds", clustering.getNumCompounds(false)));
			if (clustering.getEqualPosProperty() != null)
			{
				map.add(new Info("Distinct 3D positions", clustering.getEqualPosProperty().numDistinct3DPositions(),
						viewControler.getHighlighter(clustering.getEqualPosProperty())));
			}
			map.add(new Info("Cluster algorithm", clustering.getClusterAlgorithm(), Highlighter.CLUSTER_HIGHLIGHTER));
			//			if (clustering.getNumClusters() > 1)
			//				map.add(new Info("Num clusters", clustering.getNumClusters(), Highlighter.CLUSTER_HIGHLIGHTER));
			Highlighter stress = clustering.getEmbeddingQualityProperty() != null ? viewControler
					.getHighlighter(clustering.getEmbeddingQualityProperty()) : Highlighter.DEFAULT_HIGHLIGHTER;
			map.add(new Info("3D Embedding", clustering.getEmbedAlgorithm(), stress));
			map.add(new Info("3D Embedding Quality", clustering.getEmbedQuality(), stress));
			return map;
		}

		@Override
		public String getType()
		{
			return "Dataset";
		}

		@Override
		public String getName()
		{
			return clustering.getName();
		}

		@Override
		public int compare(CompoundProperty p1, CompoundProperty p2)
		{
			return 0;
		}
	}

	public class ClusterTablePanel extends TablePanel
	{
		@Override
		public List<Info> getAdditionalInfo()
		{
			List<Info> map = new ArrayList<Info>();
			map.add(new Info("Num compounds", ((Cluster) selected).getNumCompounds()));
			for (SubstructureSmartsType t : SubstructureSmartsType.values())
				if (((Cluster) selected).getSubstructureSmarts(t) != null)
				{
					map.add(new Info(viewControler.getHighlighter(t).toString(), ((Cluster) selected)
							.getSubstructureSmarts(t), viewControler.getHighlighter(t)));
					break;
				}
			return map;
		}

		@Override
		public String getType()
		{
			return "Cluster";
		}

		@Override
		public String getName()
		{
			return ((Cluster) selected).toString();
		}

		@Override
		public int compare(CompoundProperty p1, CompoundProperty p2)
		{
			return Double.compare(clustering.getSpecificity(((Cluster) selected), p1),
					clustering.getSpecificity(((Cluster) selected), p2));
		}
	}

	public class SelectionTablePanel extends TablePanel
	{
		@Override
		public List<Info> getAdditionalInfo()
		{
			List<Info> map = new ArrayList<Info>();
			map.add(new Info("Num compounds", ((CompoundSelection) selected).getNumCompounds()));
			return map;
		}

		@Override
		public String getType()
		{
			return "Selection";
		}

		@Override
		public String getName()
		{
			return ((CompoundSelection) selected).toString();
		}

		@Override
		public int compare(CompoundProperty p1, CompoundProperty p2)
		{
			return Double.compare(clustering.getSpecificity(((CompoundSelection) selected), p1),
					clustering.getSpecificity(((CompoundSelection) selected), p2));
		}
	}

	public class CompoundTablePanel extends TablePanel
	{
		@Override
		public List<Info> getAdditionalInfo()
		{
			List<Info> map = new ArrayList<Info>();
			map.add(new Info("Smiles", ((Compound) selected).getSmiles()));
			return map;
		}

		@Override
		public String getType()
		{
			return "Compound";
		}

		@Override
		public String getName()
		{
			return selected.toString();
		}

		@Override
		public int compare(CompoundProperty p1, CompoundProperty p2)
		{
			return Double.compare(clustering.getSpecificity((Compound) selected, p1),
					clustering.getSpecificity((Compound) selected, p2));
		}
	}

	public static final String FEATURES_ROW = "Features";

	public static final HashMap<String, ImageIcon> icons = new HashMap<String, ImageIcon>();

	public static ImageIcon getSortFilterIcon(boolean sorted, boolean filtered, boolean black)
	{
		String key = sorted + "#" + filtered + "#" + black;
		if (!icons.containsKey(key))
		{
			ImageIcon img1 = null;
			ImageIcon img2 = null;
			if (sorted)
				if (black)
					img1 = ImageLoader.getImage(Image.sort_bar14_black);
				else
					img1 = ImageLoader.getImage(Image.sort_bar14);
			if (filtered)
				if (black)
					img2 = ImageLoader.getImage(Image.filter14_black);
				else
					img2 = ImageLoader.getImage(Image.filter14);
			if (img1 == null)
			{
				img1 = img2;
				img2 = null;
			}
			ImageIcon img;
			if (img2 == null)
				img = img1;
			else
				img = new MultiImageIcon(img1, img2, Layout.horizontal, Orientation.center, 5);
			if (img == null)
				if (black)
					img = ImageLoader.getImage(Image.down14_black);
				else
					img = ImageLoader.getImage(Image.down14);
			img = new BorderImageIcon(img, 1, ComponentFactory.FOREGROUND, new Insets(1, 3, 1, 3));
			icons.put(key, img);
		}
		return icons.get(key);
	}

	public abstract class TablePanel extends JPanel
	{
		final static String CARD_FIX = "fix-card";
		final static String CARD_INTERACT = "interact-card";

		int minWidth_fix = -1;
		int minWidth_interact = -1;
		JTable table_fix = ComponentFactory.createTable(true);
		DefaultTableModel tableModel_fix;
		JTable table_interact = ComponentFactory.createTable(true);
		DefaultTableModel tableModel_interact;
		JScrollPane tableScroll_interact;
		CardLayout cardLayout;

		protected CompoundPropertyOwner selected;
		boolean interactive;
		public boolean showProps;

		public abstract String getType();

		public abstract String getName();

		public abstract List<Info> getAdditionalInfo();

		public abstract int compare(CompoundProperty p1, CompoundProperty p2);

		public TablePanel()
		{
			cardLayout = new CardLayout();
			setLayout(cardLayout);
			setOpaque(false);

			table_interact.setAutoscrolls(false);
			tableScroll_interact = ComponentFactory.createViewScrollpane(table_interact);
			tableModel_interact = (DefaultTableModel) table_interact.getModel();
			tableModel_interact.addColumn("Feature");
			tableModel_interact.addColumn("Value");
			add(tableScroll_interact, CARD_INTERACT);

			class MyListSelectionListener implements ListSelectionListener
			{
				JTable table;

				public MyListSelectionListener(JTable table)
				{
					this.table = table;
				}

				@Override
				public void valueChanged(ListSelectionEvent e)
				{
					if (selfUpdate)
						return;
					selfUpdate = true;

					int r = table.getSelectedRow();
					guiControler.setSelectedString(table.getValueAt(r, 0) + " : " + table.getValueAt(r, 1));
					if (r != -1 && table.getValueAt(r, 0) instanceof CompoundProperty)
						viewControler.setHighlighter((CompoundProperty) table.getValueAt(r, 0));
					else if (r != -1 && table.getValueAt(r, 0) instanceof Info)
						viewControler.setHighlighter(((Info) table.getValueAt(r, 0)).highlighter);
					//					else
					//						viewControler.setHighlighter(Highlighter.DEFAULT_HIGHLIGHTER);
					selfUpdate = false;

					if (table.getValueAt(r, 0) == FEATURES_ROW)
						viewControler.showSortFilterDialog();
				}
			}
			table_fix.getSelectionModel().addListSelectionListener(new MyListSelectionListener(table_fix));
			table_interact.getSelectionModel().addListSelectionListener(new MyListSelectionListener(table_interact));

			table_fix.setAutoscrolls(false);
			tableModel_fix = (DefaultTableModel) table_fix.getModel();
			tableModel_fix.addColumn("Feature");
			tableModel_fix.addColumn("Value");
			JPanel p = new JPanel(new BorderLayout());
			p.setOpaque(false);
			p.add(table_fix);
			ComponentFactory.setViewScrollPaneBorder(p);
			add(p, CARD_FIX);

			DefaultTableCellRenderer renderer = new ComponentFactory.FactoryTableCellRenderer(true)
			{
				public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
						boolean hasFocus, int row, int column)
				{
					if (column == 0 && value instanceof CompoundProperty)
						value = CompoundPropertyUtil.stripExportString((CompoundProperty) value);
					JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
							column);
					c.setIcon(null);
					if (row == 0 && column == 0)
						c.setFont(c.getFont().deriveFont(Font.BOLD));
					else if (column == 0 && value == FEATURES_ROW)
						c.setFont(c.getFont().deriveFont(Font.BOLD));
					else if (column == 1 && table.getValueAt(row, 0) == FEATURES_ROW)
					{
						boolean sorted = viewControler.isFeatureSortingEnabled() && !(selected instanceof Clustering);
						boolean filtered = viewControler.getFeatureFilter() != FeatureFilter.None;
						boolean black = viewControler.isBlackgroundBlack();
						c.setIcon(getSortFilterIcon(sorted, filtered, black));
					}
					else if (column == 1 && table.getValueAt(row, 0) instanceof CompoundProperty)
					{
						c.setFont(c.getFont().deriveFont(Font.ITALIC));
						if (isSelected == false)// && !(selected instanceof Clustering))
						{
							Color col = viewControler.getHighlightColor(selected,
									(CompoundProperty) table.getValueAt(row, 0), true);
							setForeground(col);
						}
					}
					return c;
				};
			};
			table_fix.setDefaultRenderer(Object.class, renderer);
			table_interact.setDefaultRenderer(Object.class, renderer);
		}

		public void update()
		{
			CompoundProperty selectedP = viewControler.getHighlightedProperty();

			cardLayout.show(this, interactive ? CARD_INTERACT : CARD_FIX);
			DefaultTableModel model = (interactive ? tableModel_interact : tableModel_fix);
			JTable table = (interactive ? table_interact : table_fix);

			while (model.getRowCount() > 0)
				model.removeRow(0);
			// wrap title in info object to select default highlighter when clicking on Dataset/Cluster/Compound
			model.addRow(new Object[] { new Info(getType(), null), getName() });

			List<Info> additionalInfo = getAdditionalInfo();
			if (additionalInfo != null)
				for (Info i : additionalInfo)
					model.addRow(new Object[] { i, i.value });

			model.addRow(new Object[] { "", "" });
			model.addRow(new Object[] { FEATURES_ROW, "" });

			//model.addRow(new String[] { "Accented by:", clustering.getAccent(m) });

			List<CompoundProperty> props = getPropList();
			if (viewControler.isFeatureSortingEnabled())
				Collections.sort(props, new Comparator<CompoundProperty>()
				{
					@Override
					public int compare(CompoundProperty o1, CompoundProperty o2)
					{
						return TablePanel.this.compare(o1, o2);
					}
				});

			int rowOffset = model.getRowCount();
			if (showProps)
				for (CompoundProperty p : props)
				{
					Object o[] = new Object[2];
					o[0] = p;
					o[1] = selected.getFormattedValue(p);
					model.addRow(o);
				}

			int width = ComponentFactory.packColumn(table, 0, 2,
					guiControler.getComponentMaxWidth(getMaxSizeCol(0, interactive)), true);
			width += ComponentFactory.packColumn(table, 1, 2,
					guiControler.getComponentMaxWidth(getMaxSizeCol(1, interactive)));
			if (USE_GLOBAL_MIN_WIDTH)
				globalMinWidth = Math.max(globalMinWidth, width);
			else if (interactive)
				minWidth_interact = Math.max(minWidth_interact, width);
			else
				minWidth_fix = Math.max(minWidth_fix, width);

			table.setPreferredScrollableViewportSize(table.getPreferredSize());

			if (showProps && selectedP != null && props.indexOf(selectedP) != -1)
			{
				int pIndex = rowOffset + props.indexOf(selectedP);
				table.setRowSelectionInterval(pIndex, pIndex);
				table.scrollRectToVisible(new Rectangle(table.getCellRect(pIndex, 0, true)));
			}
			else
			{
				int infoIdx = Info.find(additionalInfo, viewControler.getHighlighter());
				if (infoIdx != -1)
				{
					int r = infoIdx + 1;
					table.setRowSelectionInterval(r, r);
					table.scrollRectToVisible(new Rectangle(table.getCellRect(r, 0, true)));
				}
				else
					table.scrollRectToVisible(new Rectangle(table.getCellRect(0, 0, true)));
			}
		}

		public int getPreferredTableHeight()
		{
			return interactive ? table_interact.getPreferredSize().height : table_fix.getPreferredSize().height;
		}
	}

	private void update()
	{
		update(false);
	}

	private void update(boolean force)
	{
		if (!SwingUtilities.isEventDispatchThread())
			throw new IllegalStateException("GUI updates only in event dispatch thread plz");

		if (selfUpdate)
			return;
		selfUpdate = true;
		String card = null;
		CompoundPropertyOwner selected = null;
		boolean interactive = false;
		boolean showProps = true;

		int numCompoundsWatched = clustering.getWatchedCompounds().length;
		int numCompoundsActive = clustering.getActiveCompounds().length;

		if (numCompoundsWatched + numCompoundsActive > 0)
		{
			if (numCompoundsWatched > 1 || (numCompoundsWatched == 0 && numCompoundsActive > 1))
			{
				//selection
				card = CARD_SELECTION;
				Compound c[];
				if (numCompoundsWatched > 1)
				{
					c = clustering.getWatchedCompounds();
					interactive = false;
				}
				else
				{
					c = clustering.getActiveCompounds();
					interactive = true;
				}
				selected = clustering.getCompoundSelection(c);
			}
			else
			{
				card = CARD_COMPOUND;
				if (clustering.isCompoundWatched())
				{
					selected = clustering.getWatchedCompound();
					interactive = clustering.isCompoundActive() && selected == clustering.getActiveCompound();
				}
				else if (clustering.isCompoundActive())
				{
					selected = clustering.getActiveCompound();
					interactive = true;
				}
			}
		}
		else if (clustering.isClusterActive() && clustering.numClusters() > 1)
		{
			card = CARD_CLUSTER;
			selected = clustering.getActiveCluster();
			interactive = true;
		}
		else if (clustering.isClusterWatched() && clustering.numClusters() > 1)
		{
			card = CARD_CLUSTER;
			selected = clustering.getWatchedCluster();
			interactive = false;
		}
		else
		{
			card = CARD_CLUSTERING;
			interactive = viewControler.isShowClusteringPropsEnabled();
			showProps = viewControler.isShowClusteringPropsEnabled();
			selected = clustering;
		}

		if (card == null || clustering.getNumClusters() == 0 || viewControler.getHighlighters() == null)
		{
			currentCard = null;
			clusterCompoundPanel.setVisible(false);
			compoundIconPanel.setVisible(false);
		}
		else
		{
			if (force || card != currentCard || selected != cardToPanel.get(card).selected
					|| interactive != cardToPanel.get(card).interactive || showProps != cardToPanel.get(card).showProps)
			{
				currentCard = card;
				cardToPanel.get(currentCard).selected = selected;
				cardToPanel.get(currentCard).interactive = interactive;
				cardToPanel.get(currentCard).showProps = showProps;

				clusterCompoundPanel.setIgnoreRepaint(true);
				clusterCompoundLayout.show(clusterCompoundPanel, card);
				cardToPanel.get(currentCard).update();

				if (card == CARD_COMPOUND || guiControler.isAccentuateComponents())
				{
					compoundIconLabel.setVisible(false);
					compoundIconPanel.setVisible(true);
					final Compound fCompound = card == CARD_COMPOUND ? (Compound) selected : null;
					Thread th = new Thread(new Runnable()
					{
						public void run()
						{
							SwingUtilities.invokeLater(new Runnable()
							{
								public void run()
								{
									ImageIcon icon = null;
									if (guiControler.isAccentuateComponents()
											|| (currentCard == CARD_COMPOUND && cardToPanel.get(currentCard).selected == fCompound))
									{
										int size = getIconWidth();
										if (fCompound != null)
											icon = fCompound.getIcon(viewControler.isBlackgroundBlack(), size, size,
													true);
										else
											icon = CDKCompoundIcon.createDemoIcon(viewControler.isBlackgroundBlack(),
													size, size, Layout.vertical, true);
									}
									if (guiControler.isAccentuateComponents()
											|| (currentCard == CARD_COMPOUND && cardToPanel.get(currentCard).selected == fCompound))// after icon is loaded
									{
										compoundIconLabel.setIcon(icon);
										compoundIconLabel.setVisible(true);
									}
								}
							});
						}
					});
					th.start();
				}
				else
				{
					compoundIconPanel.setVisible(false);
				}
				clusterCompoundPanel.setPreferredSize(null);
				clusterCompoundPanel.setIgnoreRepaint(false);
				clusterCompoundPanel.setVisible(true);
			}
		}
		selfUpdate = false;
	}

	private int getIconWidth()
	{
		return guiControler.getComponentMaxWidth((viewControler.getStyle() == Style.dots ? ComponentSize.ICON_2D_DOTS
				: ComponentSize.ICON_2D).getValue() * 0.01);
	}

	public InfoPanel(ViewControler viewControler, ClusterController clusterControler, Clustering clustering,
			GUIControler guiControler)
	{
		this.clusterControler = clusterControler;
		this.viewControler = viewControler;
		this.clustering = clustering;
		this.guiControler = guiControler;

		clustering.addListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				//				updateDataset();
				if (evt.getPropertyName().equals(ClusteringImpl.CLUSTER_NEW))
					resetMinSizes();
				update(true);
			}
		});

		buildLayout();
		clustering.addSelectionListener(new SelectionListener()
		{
			@Override
			public void compoundWatchedChanged(Compound[] c)
			{
				update();
			}

			@Override
			public void compoundActiveChanged(Compound[] c)
			{
				update();
			}

			@Override
			public void clusterWatchedChanged(Cluster c)
			{
				update();
			}

			@Override
			public void clusterActiveChanged(Cluster c)
			{
				update();
			}
		});

		viewControler.addViewListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				if (evt.getPropertyName().equals(ViewControler.PROPERTY_HIGHLIGHT_CHANGED)
						|| evt.getPropertyName().equals(ViewControler.PROPERTY_NEW_HIGHLIGHTERS)
						|| evt.getPropertyName().equals(ViewControler.PROPERTY_BACKGROUND_CHANGED)
						|| evt.getPropertyName().equals(ViewControler.PROPERTY_STYLE_CHANGED))
				{
					update(true);
				}
				else if (evt.getPropertyName().equals(ViewControler.PROPERTY_FEATURE_FILTER_CHANGED)
						|| evt.getPropertyName().equals(ViewControler.PROPERTY_FONT_SIZE_CHANGED))
				{
					//					updateDatasetPanelSize();
					resetMinSizes();
					update(true);
				}
				else if (evt.getPropertyName().equals(ViewControler.PROPERTY_FEATURE_SORTING_CHANGED)
						|| evt.getPropertyName().equals(ViewControler.PROPERTY_COMPOUND_FILTER_CHANGED))
				{
					update(true);
				}
			}
		});

		guiControler.addPropertyChangeListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				if (evt.getPropertyName().equals(GUIControler.PROPERTY_VIEWER_SIZE_CHANGED))
					update(true);
			}
		});

		Thread th = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				while (true)
				{
					ThreadUtil.sleep(100);
					if ((InfoPanel.this.clustering.isClusterWatched() || InfoPanel.this.clustering.isCompoundWatched())
							&& SwingUtil.isMouseInside(clusterCompoundPanel))
					{
						InfoPanel.this.viewControler.clearMouseMoveWatchUpdates(true);
					}
				}
			}
		});
		th.start();
	}

	private void resetMinSizes()
	{
		globalMinWidth = 0;
		for (String k : cardToPanel.keySet())
		{
			cardToPanel.get(k).minWidth_fix = 0;
			cardToPanel.get(k).minWidth_interact = 0;
		}
		preferredTableHeight = 0;
	}

	private void buildLayout()
	{
		compoundIconPanel = new JPanel(new BorderLayout());
		compoundIconPanel.setOpaque(false);
		compoundIconPanel.add(compoundIconLabel, BorderLayout.NORTH);
		guiControler.registerSizeComponent(ComponentSize.ICON_2D, compoundIconLabel);

		clusterCompoundLayout = new CardLayout();
		clusterCompoundPanel = new JPanel(clusterCompoundLayout)
		{
			@Override
			public Dimension getPreferredSize()
			{
				if (cardToPanel.get(currentCard) == null)
					return new Dimension(0, 0);
				Dimension dim = super.getPreferredSize();
				int maxWidth = guiControler.getComponentMaxWidth(getMaxSizeTotal());
				if (guiControler.isAccentuateComponents())
					dim.width = maxWidth;
				else
				{
					int minWidth;
					if (USE_GLOBAL_MIN_WIDTH)
						minWidth = globalMinWidth;
					else if (cardToPanel.get(currentCard).interactive)
						minWidth = cardToPanel.get(currentCard).minWidth_interact;
					else
						minWidth = cardToPanel.get(currentCard).minWidth_fix;
					dim.width = Math.min(dim.width, Math.min(maxWidth, minWidth));
				}
				return dim;
			}
		};
		guiControler.registerSizeComponent(ComponentSize.INFO_TABLE, clusterCompoundPanel);
		clusterCompoundPanel.setOpaque(false);
		clusterCompoundPanel.setVisible(false);

		cardToPanel.put(CARD_CLUSTERING, new ClusteringTablePanel());
		cardToPanel.put(CARD_CLUSTER, new ClusterTablePanel());
		cardToPanel.put(CARD_SELECTION, new SelectionTablePanel());
		cardToPanel.put(CARD_COMPOUND, new CompoundTablePanel());
		for (String k : cardToPanel.keySet())
			clusterCompoundPanel.add(cardToPanel.get(k), k);

		JPanel iconAndClusterCompoundPanel = new JPanel(new BorderLayout(5, 5));
		iconAndClusterCompoundPanel.add(compoundIconPanel, BorderLayout.WEST);
		iconAndClusterCompoundPanel.add(clusterCompoundPanel, BorderLayout.CENTER);
		iconAndClusterCompoundPanel.setOpaque(false);

		setLayout(new BorderLayout(0, 20));
		JPanel datasetPanelContainer = new JPanel(new BorderLayout());
		datasetPanelContainer.setOpaque(false);
		add(iconAndClusterCompoundPanel, BorderLayout.EAST);

		setOpaque(false);
	}

	HashSet<String> propNames = new HashSet<String>();

	private List<CompoundProperty> getPropList()
	{
		if (propNames.size() == 0)
			for (CompoundProperty p : clustering.getPropertiesAndFeatures())
				propNames.add(p.toString());

		List<CompoundProperty> props = null;
		if (viewControler.getFeatureFilter() == FeatureFilter.SelectedForMapping)
			props = new ArrayList<CompoundProperty>(clustering.getFeatures());
		else if (viewControler.getFeatureFilter() == FeatureFilter.UsedForMapping)
		{
			props = new ArrayList<CompoundProperty>();
			for (CompoundProperty p : clustering.getFeatures())
				if (p.numDistinctValues() > 1 && p.getRedundantProp() == null)
					props.add(p);
		}
		else
		{
			props = new ArrayList<CompoundProperty>();
			for (CompoundProperty p : clustering.getProperties())
				if (p.getCompoundPropertySet() == null || !p.getCompoundPropertySet().isSmiles())
					props.add(p);
			for (CompoundProperty p : clustering.getAdditionalProperties())
				if (p instanceof SALIProperty)
					props.add(p);
			if (viewControler.getFeatureFilter() == FeatureFilter.None)
				props = ListUtil.concat(props, clustering.getFeatures());
		}
		if (viewControler.getFeatureFilter() == FeatureFilter.Filled)
			props = ListUtil.filter(props, new ListUtil.Filter<CompoundProperty>()
			{
				public boolean accept(CompoundProperty p)
				{
					return p.toString().endsWith("_filled");
				};
			});
		if (viewControler.getFeatureFilter() == FeatureFilter.Endpoints)
			props = ListUtil.filter(props, new ListUtil.Filter<CompoundProperty>()
			{
				public boolean accept(CompoundProperty p)
				{
					return propNames.contains(p.toString() + "_real");
				};
			});
		if (viewControler.getFeatureFilter() == FeatureFilter.Real)
			props = ListUtil.filter(props, new ListUtil.Filter<CompoundProperty>()
			{
				public boolean accept(CompoundProperty p)
				{
					return p.toString().endsWith("_real");
				};
			});
		return props;
	}

	int preferredTableHeight = -1;

	public int getPreferredTableHeight()
	{
		if (isVisible() && cardToPanel.get(currentCard) != null)
		{
			preferredTableHeight = Math.max(preferredTableHeight, cardToPanel.get(currentCard)
					.getPreferredTableHeight());
			preferredTableHeight = Math.max(preferredTableHeight, compoundIconLabel.getHeight());
			return preferredTableHeight;
		}
		else
			return 0;
	}

}
