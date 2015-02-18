package org.chesmapper.view.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.chesmapper.map.main.ScreenSetup;
import org.chesmapper.view.cluster.Cluster;
import org.chesmapper.view.cluster.ClusterController;
import org.chesmapper.view.cluster.Clustering;
import org.chesmapper.view.cluster.ClusteringImpl;
import org.chesmapper.view.cluster.Compound;
import org.chesmapper.view.cluster.Clustering.SelectionListener;
import org.chesmapper.view.gui.swing.ComponentFactory;
import org.chesmapper.view.gui.swing.TransparentViewPanel;
import org.chesmapper.view.gui.swing.ComponentFactory.ClickableLabel;
import org.mg.javalib.gui.DoubleNameListCellRenderer;
import org.mg.javalib.gui.MouseOverList;
import org.mg.javalib.util.ArrayUtil;
import org.mg.javalib.util.ImageLoader;

public class CompoundListPanel extends JPanel
{
	JScrollPane listScrollPane;
	MouseOverList list;
	DefaultListModel<Compound> listModel;
	DoubleNameListCellRenderer listRenderer;

	boolean selfBlock = false;

	Clustering clustering;
	ClusterController clusterControler;
	ViewControler viewControler;
	GUIControler guiControler;

	ClickableLabel clearSelectedButton;
	JButton filterButton;

	public CompoundListPanel(Clustering clustering, ClusterController clusterControler, ViewControler controler,
			GUIControler guiControler)
	{
		this.clustering = clustering;
		this.clusterControler = clusterControler;
		this.viewControler = controler;
		this.guiControler = guiControler;

		buildLayout();

		updateList();
		installListeners(controler);
	}

	private void installListeners(final ViewControler controler)
	{
		clustering.addListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				if (evt.getPropertyName().equals(ClusteringImpl.CLUSTER_MODIFIED)
						|| evt.getPropertyName().equals(ClusteringImpl.CLUSTER_CLEAR))
				{
					updateList();
				}
			}
		});

		clustering.addSelectionListener(new SelectionListener()
		{

			@Override
			public void compoundWatchedChanged(Compound[] c)
			{
				if (selfBlock)
					return;
				selfBlock = true;
				if (c.length == 0)
					list.clearSelection();
				else
					list.setSelectedValue(c[0], true);
				selfBlock = false;
			}

			@Override
			public void compoundActiveChanged(Compound[] c)
			{
				if (!selfBlock)
				{
					selfBlock = true;
					updateActiveCompoundSelection();
					selfBlock = false;
				}
				updateClearButton();
				updateFilterButton();
			}

			@Override
			public void clusterWatchedChanged(Cluster c)
			{
				updateList();
			}

			@Override
			public void clusterActiveChanged(Cluster c)
			{
				updateList();
				updateClearButton();
				updateFilterButton();
			}
		});

		list.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(final MouseEvent e)
			{
				if (selfBlock)
					return;
				guiControler.block("click compound");
				selfBlock = true;
				viewControler.clearMouseMoveWatchUpdates(false);

				Thread th = new Thread(new Runnable()
				{
					public void run()
					{
						try
						{
							int idx = list.getLastSelectedIndex();
							Compound m = (Compound) listModel.elementAt(idx);
							if (m == null)
								throw new IllegalStateException();
							if (e.isControlDown())
								clusterControler.toggleCompoundActive(m);
							else if (e.isShiftDown() && clustering.isCompoundActive())
							{
								int startIdx = listModel.indexOf(clustering.getActiveCompound());
								int min, max;
								if (startIdx < idx)
								{
									min = startIdx + 1;
									max = idx;
								}
								else
								{
									min = idx;
									max = startIdx - 1;
								}
								List<Compound> comps = new ArrayList<Compound>();
								for (int i = min; i <= max; i++)
									comps.add((Compound) listModel.elementAt(i));
								for (Compound c : clustering.getActiveCompounds())
									if (!comps.contains(c))
										comps.add(c);
								clusterControler.setCompoundActive(ArrayUtil.toArray(comps), true);
							}
							else
							{
								if (clustering.isCompoundActive(m))
									clusterControler.clearCompoundActive(true);
								else
									clusterControler.setCompoundActive(m, true);
							}

						}
						finally
						{
							selfBlock = false;
							SwingUtilities.invokeLater(new Runnable()
							{
								public void run()
								{
									guiControler.unblock("click compound");
								}
							});
						}
					}
				});
				th.start();
			}
		});

		list.addListSelectionListener(new ListSelectionListener()
		{
			@Override
			public void valueChanged(ListSelectionEvent e)
			{
				if (selfBlock)
					return;
				selfBlock = true;
				int index = list.getSelectedIndex();
				if (index == -1)
				{
					//compoundWatched.clearSelection();
				}
				else
				{
					final Compound watched = (Compound) listModel.elementAt(index);
					controler.doMouseMoveWatchUpdates(new Runnable()
					{
						public void run()
						{
							clusterControler.clearClusterWatched();
							clusterControler.setCompoundWatched(watched);
						}
					});
				}
				selfBlock = false;
			}
		});

		controler.addViewListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				if (evt.getPropertyName().equals(ViewControler.PROPERTY_COMPOUND_DESCRIPTOR_CHANGED)
						|| evt.getPropertyName().equals(ViewControler.PROPERTY_HIGHLIGHT_CHANGED)
						|| evt.getPropertyName().equals(ViewControler.PROPERTY_FEATURE_SORTING_CHANGED)
						|| evt.getPropertyName().equals(ViewControler.PROPERTY_COMPOUND_FILTER_CHANGED))

					updateList();
				else if (evt.getPropertyName().equals(ViewControler.PROPERTY_SINGLE_COMPOUND_SELECTION_ENABLED))
				{
					updateList();
					updateClearButton();
					updateFilterButton();
				}
				else if (evt.getPropertyName().equals(ViewControler.PROPERTY_FONT_SIZE_CHANGED))
					updateListSize();
			}
		});

		guiControler.addPropertyChangeListener(new PropertyChangeListener()
		{

			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				if (evt.getPropertyName().equals(GUIControler.PROPERTY_VIEWER_SIZE_CHANGED))
					updateListSize();
			}
		});

		filterButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				clusterControler.applyCompoundFilter(ArrayUtil.toList(clustering.getActiveCompounds()), true);
			}
		});

		clearSelectedButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				Thread noAwt = new Thread(new Runnable()
				{
					public void run()
					{
						if (clustering.isClusterActive() && clustering.getNumClusters() > 1
								&& clustering.getActiveCluster().getCompounds().size() == 1)
							clusterControler.clearClusterActive(true, true);
						else if (clustering.isCompoundActive())
							clusterControler.clearCompoundActive(true);
						else if (clustering.isClusterActive() && clustering.getNumClusters() > 1)
							clusterControler.clearClusterActive(true, true);
						else if (viewControler.isSingleCompoundSelection())
						{
							viewControler.setSingleCompoundSelection(false);
							SwingUtilities.invokeLater(new Runnable()
							{
								public void run()
								{
									updateList();
									updateClearButton();
								}
							});
						}
					}
				});
				noAwt.start();
			}
		});
	}

	private void updateFilterButton()
	{
		filterButton.setVisible(listScrollPane.isVisible() && clustering.getActiveCompounds().length > 1
				&& clustering.getActiveCompounds().length < clustering.getNumCompounds(false) - 1);
	}

	private void updateClearButton()
	{
		clearSelectedButton
				.setVisible(listScrollPane.isVisible()
						&& ((clustering.isClusterActive() && clustering.getNumClusters() > 1)
								|| clustering.isCompoundActive() || viewControler.isSingleCompoundSelection()));
	}

	@SuppressWarnings("unchecked")
	private void buildLayout()
	{
		listModel = new DefaultListModel<Compound>();

		list = new MouseOverList(listModel);
		list.setClearOnExit(false);

		listRenderer = new DoubleNameListCellRenderer(listModel)
		{
			@Override
			public void updateUI()
			{
				super.updateUI();
				if (getFontLabel1() != null)
				{
					setFontLabel1(getFontLabel1().deriveFont((float) ScreenSetup.INSTANCE.getFontSize()));
					setFontLabel2(getFontLabel2().deriveFont((float) ScreenSetup.INSTANCE.getFontSize()));
					list.setFixedCellHeight(getRowHeight());
				}
			}

			@SuppressWarnings("rawtypes")
			public Component getListCellRendererComponent(JList list, Object value, int i, boolean isSelected,
					boolean cellHasFocus)
			{
				super.getListCellRendererComponent(list, value, i, isSelected, cellHasFocus);

				Compound c = (Compound) value;
				setOpaque(isSelected || clustering.isCompoundActive(c));

				setForeground(ComponentFactory.FOREGROUND);
				if (clustering.isCompoundActive(c))
				{
					setBackground(ComponentFactory.LIST_ACTIVE_BACKGROUND);
					setForeground(ComponentFactory.LIST_SELECTION_FOREGROUND);
				}
				else if (isSelected)
				{
					setBackground(ComponentFactory.LIST_WATCH_BACKGROUND);
					setForeground(ComponentFactory.LIST_SELECTION_FOREGROUND);
				}
				else
				{
					setForegroundLabel2(c.getHighlightColorText());
				}
				return this;
			}

		};
		listRenderer.setFontLabel2(listRenderer.getFontLabel2().deriveFont(Font.ITALIC));
		list.setCellRenderer(listRenderer);
		list.setOpaque(false);
		list.setFocusable(false);

		JPanel p = new JPanel(new BorderLayout(6, 6));
		p.setOpaque(false);

		listScrollPane = ComponentFactory.createViewScrollpane(list);
		guiControler.registerSizeComponent(ComponentSize.COMPOUND_LIST, listScrollPane);
		JPanel listWrapped = new TransparentViewPanel(new BorderLayout());
		listWrapped.add(listScrollPane);
		p.add(listWrapped);

		clearSelectedButton = ComponentFactory.createCrossViewButton();
		filterButton = ComponentFactory.createViewButton(ImageLoader.getImage(ImageLoader.Image.filter14_black),
				ImageLoader.getImage(ImageLoader.Image.filter14));
		filterButton.setName("filter-button");//for tests
		JPanel buttonWrapped = new TransparentViewPanel(new BorderLayout(6, 6));
		buttonWrapped.add(clearSelectedButton);
		buttonWrapped.add(filterButton, BorderLayout.SOUTH);
		JPanel removeButtonPanel = new JPanel(new BorderLayout());
		removeButtonPanel.setOpaque(false);
		removeButtonPanel.add(buttonWrapped, BorderLayout.NORTH);
		p.add(removeButtonPanel, BorderLayout.EAST);
		clearSelectedButton.setVisible(false);
		filterButton.setVisible(false);

		setLayout(new BorderLayout());
		add(p);
		setOpaque(false);
	}

	private void updateActiveCompoundSelection()
	{
		if (clustering.isCompoundActive())
			list.setSelectedValue(clustering.getActiveCompound(), true);
		else
			list.clearSelection();
	}

	private void updateList()
	{
		if (!SwingUtilities.isEventDispatchThread())
			throw new IllegalStateException("GUI updates only in event dispatch thread plz");

		selfBlock = true;
		setVisible(true);

		setIgnoreRepaint(true);
		if (clustering.getNumClusters() == 0)
		{
			listScrollPane.setVisible(false);
			clearSelectedButton.setVisible(false);
			filterButton.setVisible(false);
		}
		else if (!clustering.isClusterActive() && !viewControler.isSingleCompoundSelection())
		{
			listScrollPane.setVisible(false);
		}
		else
		{
			listModel.removeAllElements();

			listScrollPane.setPreferredSize(null);
			Compound m[] = new Compound[viewControler.isSingleCompoundSelection() ? clustering.getCompounds(false)
					.size() : clustering.getActiveCluster().getCompounds().size()];

			int i = 0;
			for (Compound mod : (viewControler.isSingleCompoundSelection() ? clustering.getCompounds(false)
					: clustering.getActiveCluster().getCompounds()))
			{
				m[i++] = mod;
				if (mod.getDisplayName() == null)
					throw new IllegalStateException("display name for compound is nil, check order of listeners");
			}
			for (Compound comp : (viewControler.isSingleCompoundSelection() ? clustering.getCompounds(false)
					: clustering.getActiveCluster().getCompounds()))
				comp.setFeatureSortingEnabled(true);//viewControler.isFeatureSortingEnabled());
			for (Compound compound : m)
				if (compound == null)
					throw new IllegalStateException();
			if (m.length > 1)
				Arrays.sort(m);

			for (Compound compound : m)
				listModel.addElement(compound);
			updateActiveCompoundSelection();
			updateListSize();
			listScrollPane.setVisible(true);
		}
		setIgnoreRepaint(false);
		revalidate();
		repaint();
		selfBlock = false;
	}

	private void updateListSize()
	{
		int rowCount = (guiControler.getComponentMaxHeight(1) / listRenderer.getRowHeight()) / 3;

		double ratioVisible = rowCount / (double) listModel.getSize();
		if (ratioVisible <= 0.5)
		{
			// if less then 50% of elements is visible increase by up to 50%
			double ratioIncrease = 0.5 - ratioVisible;
			rowCount += (int) (ratioIncrease * rowCount);
		}

		list.setVisibleRowCount(rowCount);

		if (viewControler.getHighlightedProperty() != null)
		{
			// features values are shown on the right, restrict long compound names to show feature values without scroll pane
			listRenderer
					.setMaxl1Width(guiControler.getComponentMaxWidth((ComponentSize.COMPOUND_LIST.getValue() - 5) * 0.01));
		}
		else
		{
			// no features values are shown on the right -> long names can be written out, scroll-pane will show up
			listRenderer.setMaxl1Width(Integer.MAX_VALUE);
		}

		listScrollPane.setPreferredSize(null);
		int width = guiControler.getComponentMaxWidth(ComponentSize.COMPOUND_LIST.getValue() * 0.01);
		if (!guiControler.isAccentuateComponents())
			width = Math.min(width, listScrollPane.getPreferredSize().width);
		listScrollPane.setPreferredSize(new Dimension(width, listScrollPane.getPreferredSize().height));
		listScrollPane.revalidate();
		listScrollPane.repaint();
	}
}
