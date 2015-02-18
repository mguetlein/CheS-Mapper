package org.chesmapper.view.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.vecmath.Vector3f;

import org.chesmapper.map.data.ClusteringData;
import org.chesmapper.map.dataInterface.CompoundGroupWithProperties;
import org.chesmapper.map.dataInterface.CompoundProperty;
import org.chesmapper.map.dataInterface.CompoundPropertyOwner;
import org.chesmapper.map.dataInterface.CompoundPropertyUtil;
import org.chesmapper.map.dataInterface.FragmentProperty;
import org.chesmapper.map.dataInterface.NominalProperty;
import org.chesmapper.map.dataInterface.NumericProperty;
import org.chesmapper.map.dataInterface.SingleCompoundPropertyOwner;
import org.chesmapper.map.dataInterface.SubstructureSmartsType;
import org.chesmapper.map.dataInterface.CompoundPropertyUtil.NominalColoring;
import org.chesmapper.map.gui.CheSMapperWizard;
import org.chesmapper.map.main.ScreenSetup;
import org.chesmapper.map.main.Settings;
import org.chesmapper.map.main.TaskProvider;
import org.chesmapper.view.cluster.Cluster;
import org.chesmapper.view.cluster.ClusterController;
import org.chesmapper.view.cluster.Clustering;
import org.chesmapper.view.cluster.ClusteringImpl;
import org.chesmapper.view.cluster.ClusteringUtil;
import org.chesmapper.view.cluster.Compound;
import org.chesmapper.view.cluster.CompoundFilter;
import org.chesmapper.view.cluster.CompoundFilterImpl;
import org.chesmapper.view.cluster.JitteringProvider;
import org.chesmapper.view.gui.View.AnimationSpeed;
import org.chesmapper.view.gui.swing.ComponentFactory;
import org.chesmapper.view.gui.util.CompoundPropertyHighlighter;
import org.chesmapper.view.gui.util.HighlightAutomatic;
import org.chesmapper.view.gui.util.Highlighter;
import org.chesmapper.view.gui.util.SortFilterDialog;
import org.chesmapper.view.gui.util.SubstructureHighlighter;
import org.jmol.adapter.smarter.SmarterJmolAdapter;
import org.jmol.api.JmolAdapter;
import org.jmol.api.JmolSimpleViewer;
import org.mg.javalib.gui.property.ColorGradient;
import org.mg.javalib.task.Task;
import org.mg.javalib.task.TaskDialog;
import org.mg.javalib.util.ArrayUtil;
import org.mg.javalib.util.ColorUtil;
import org.mg.javalib.util.ObjectUtil;
import org.mg.javalib.util.StringUtil;
import org.mg.javalib.util.SwingUtil;
import org.mg.javalib.util.ThreadUtil;

public class MainPanel extends JPanel implements ViewControler, ClusterController
{
	GUIControler guiControler;
	JmolPanel jmolPanel;
	View view;
	private ClusteringImpl clustering;
	private boolean spinEnabled = false;
	private boolean hideHydrogens = true;
	private Style style = Style.wireframe;
	private NominalColoring nominalColoring = NominalColoring.TwoThirdMode;

	DisguiseMode disguiseUnHovered = DisguiseMode.solid;
	DisguiseMode disguiseUnZoomed = DisguiseMode.translucent;

	CompoundProperty compoundDescriptorProperty = null;
	List<JComponent> ignoreMouseMovementPanels = new ArrayList<JComponent>();

	private String getStyleString()
	{
		if (ScreenSetup.INSTANCE.isFontSizeLarge())
		{
			switch (style)
			{
				case wireframe:
					return "spacefill 0; wireframe 0.08";
				case ballsAndSticks:
					return "wireframe 35; spacefill 21%";
				case dots:
					return "spacefill 85%";
			}
			throw new IllegalStateException("WTF");
		}
		else
		{
			switch (style)
			{
				case wireframe:
					return "spacefill 0; wireframe 0.02";
				case ballsAndSticks:
					return "wireframe 25; spacefill 15%";
				case dots:
					return "spacefill 70%";
			}
			throw new IllegalStateException("WTF");
		}
	}

	private Color getHighlightColor(Highlighter h, CompoundPropertyOwner m, CompoundProperty p, boolean textColor)
	{
		if (h == Highlighter.CLUSTER_HIGHLIGHTER)
			if (m instanceof Compound)
				return CompoundPropertyUtil.getClusterColor(clustering.getClusterIndexForCompound((Compound) m));
			else
				return null;
		else
			return getHighlightColor(m, p, textColor);
	}

	public Color getHighlightColor(CompoundPropertyOwner m, CompoundProperty p, boolean textColor)
	{
		return getHighlightColor(m, p, textColor, isBlackgroundBlack());
	}

	public Color getHighlightColor(CompoundPropertyOwner m, CompoundProperty p, boolean textColor,
			boolean blackBackground)
	{
		if (m == null)
			throw new IllegalStateException();

		if (p == null || p.isUndefined())
			return textColor ? ComponentFactory.getForeground(blackBackground) : null;
		else if (p instanceof NominalProperty)
		{
			String val;
			if (m instanceof CompoundGroupWithProperties)
				val = CompoundPropertyUtil.getNominalHighlightValue((NominalProperty) p,
						((CompoundGroupWithProperties) m).getNominalSummary((NominalProperty) p), nominalColoring);
			else if (m instanceof SingleCompoundPropertyOwner)
				val = ((SingleCompoundPropertyOwner) m).getStringValue((NominalProperty) p);
			else
				throw new IllegalStateException();
			if (val == null)
				return textColor ? ComponentFactory.getForeground(blackBackground) : CompoundPropertyUtil
						.getNullValueColor();
			else
				return CompoundPropertyUtil.getNominalColor((NominalProperty) p, val);
		}
		else if (p instanceof NumericProperty)
		{
			try
			{
				if (m.getDoubleValue((NumericProperty) p) == null)
					return textColor ? ComponentFactory.getForeground(blackBackground) : CompoundPropertyUtil
							.getNullValueColor();
				double val = clustering.getNormalizedDoubleValue(m, (NumericProperty) p);
				if (Double.isNaN(val) || Double.isInfinite(val))
					throw new NullPointerException("not null, but nan or infinite");
				ColorGradient grad;
				if (((NumericProperty) p).getHighlightColorGradient() != null)
					grad = ((NumericProperty) p).getHighlightColorGradient();
				else
					grad = DEFAULT_COLOR_GRADIENT;
				if (!blackBackground && grad.getMed().equals(Color.WHITE))
				{
					if (textColor)
						grad = new ColorGradient(grad.high, Color.GRAY, grad.low);
					else if (isAntialiasEnabled())
						grad = new ColorGradient(grad.high, Color.WHITE, grad.low);
					else
						grad = new ColorGradient(grad.high, Color.LIGHT_GRAY, grad.low);
				}
				return grad.getColor(val);
			}
			catch (NullPointerException e)
			{
				System.err.println("Nullpointer exception in getHighlightColor: " + e.getMessage() + "\nproperty: " + p
						+ " prop-owner: " + m + " get-double-value:" + m.getDoubleValue((NumericProperty) p)
						+ " get-normalized-double-value: "
						+ clustering.getNormalizedDoubleValue(m, (NumericProperty) p)
						+ " get-log-normalized-double-value: "
						+ clustering.getNormalizedLogDoubleValue(m, (NumericProperty) p));
				e.printStackTrace();
				return null;
			}
		}
		else
			throw new IllegalStateException();
	}

	@Override
	public void setNominalColoring(NominalColoring nominalColoring)
	{
		if (this.nominalColoring != nominalColoring)
		{
			this.nominalColoring = nominalColoring;
			updateAllClustersAndCompounds(true);
			fireViewChange(PROPERTY_HIGHLIGHT_COLORS_CHANGED);
			String msg = "Color groups of nominal feature values according to ";
			if (nominalColoring == NominalColoring.TwoThirdMode)
				msg += "most frequent value >= 66%";
			else if (nominalColoring == NominalColoring.Mode)
				msg += "most frequent value";
			else if (nominalColoring == NominalColoring.ActiveValueIncluded)
				msg += "active value";
			guiControler.showMessage(msg);
		}
	}

	public NominalColoring getNominalColoring()
	{
		return nominalColoring;
	}

	public static enum Translucency
	{
		None, ModerateWeak, ModerateStrong, Strong;
	}

	private String getColorSuffixTranslucent(Translucency t)
	{
		if (t == Translucency.None)
			return "";
		double translucency[] = null;
		if (style == Style.wireframe)
			translucency = new double[] { -1, 0.4, 0.6, 0.8 };
		else if (style == Style.ballsAndSticks)
			translucency = new double[] { -1, 0.5, 0.7, 0.9 };
		else if (style == Style.dots)
			translucency = new double[] { -1, 0.5, 0.7, 0.9 };
		else
			throw new IllegalStateException("WTF");
		double trans = translucency[ArrayUtil.indexOf(Translucency.values(), t)];
		//		if (Settings.SCREENSHOT_SETUP)
		//			trans = 0.99;// Math.min(0.95, trans + 0.15);
		//		Settings.LOGGER.warn(trans);
		return "; color translucent " + trans;
	}

	HashMap<String, Highlighter[]> highlighters;
	Highlighter selectedHighlighter = null;
	Highlighter lastSelectedHighlighter = null;
	CompoundProperty selectedHighlightCompoundProperty = null;
	boolean highlighterLabelsVisible = false;
	HighlightSorting highlightSorting = HighlightSorting.Median;
	HighlightAutomatic highlightAutomatic;
	HighlightMode highlightMode = HighlightMode.ColorCompounds;
	boolean antialiasEnabled = ScreenSetup.INSTANCE.isAntialiasOn();
	boolean highlightLastFeatureEnabled = false;
	FeatureFilter featureFilter = FeatureFilter.None;
	boolean featureSortingEnabled = true;

	public Clustering getClustering()
	{
		return clustering;
	}

	public boolean isSpinEnabled()
	{
		return spinEnabled;
	}

	private int spinSpeed = 3;

	public void setSpinEnabled(boolean spin)
	{
		setSpinEnabled(spin, false);
	}

	private void setSpinEnabled(boolean spinEnabled, boolean force)
	{
		if (this.spinEnabled != spinEnabled || force)
		{
			this.spinEnabled = spinEnabled;
			view.setSpinEnabled(spinEnabled, spinSpeed);
			fireViewChange(PROPERTY_SPIN_CHANGED);
			guiControler.showMessage((spinEnabled ? "Enable" : "Disable") + " spinning.");
		}
	}

	@Override
	public void increaseSpinSpeed(boolean increase)
	{
		if (spinEnabled && (increase || spinSpeed > 2))
		{
			if (increase)
				spinSpeed++;
			else
				spinSpeed--;
			view.setSpinEnabled(spinEnabled, spinSpeed);
			guiControler.showMessage((increase ? "Increase" : "Decrease") + " spin speed to " + spinSpeed + ".");
		}
	}

	boolean singleCompoundSelection = false;

	@Override
	public void setSingleCompoundSelection(boolean b)
	{
		if (singleCompoundSelection != b)
		{
			singleCompoundSelection = b;
			if (singleCompoundSelection)
				fireViewChange(PROPERTY_SINGLE_COMPOUND_SELECTION_ENABLED);
			guiControler.showMessage((singleCompoundSelection ? "Single compound" : "Cluster") + " selection enabled.");
		}
	}

	@Override
	public boolean isSingleCompoundSelection()
	{
		return singleCompoundSelection;
	}

	public MainPanel(GUIControler guiControler)
	{
		this.guiControler = guiControler;
		jmolPanel = new JmolPanel();

		setLayout(new BorderLayout());
		add(jmolPanel);

		clustering = new ClusteringImpl();
		clustering.addListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				if (evt.getPropertyName().equals(ClusteringImpl.CLUSTER_NEW)
						|| evt.getPropertyName().equals(ClusteringImpl.CLUSTER_CLEAR))
				{
					setSuperimpose(false);
					jitteringLevel = 0;
					MainPanel.this.guiControler.updateTitle(clustering);
				}
			}
		});
		view = View.init(jmolPanel, guiControler, this, clustering);
		highlightAutomatic = new HighlightAutomatic(this, clustering);

		setBackgroundBlack(ComponentFactory.isBackgroundBlack(), true);

		// mouse listener to click atoms or clusters (zoom in)
		jmolPanel.addMouseListener(new MouseAdapter()
		{
			JPopupMenu popup;

			public void mouseClicked(final MouseEvent e)
			{
				//				MainPanel.this.guiControler.block("handle click");

				clearMouseMoveWatchUpdates(false);
				if (SwingUtilities.isLeftMouseButton(e))
				{
					Thread th = new Thread(new Runnable()
					{
						public void run()
						{
							int atomIndex = view.findNearestAtomIndex(e.getX(), e.getY());

							if (atomIndex != -1)
							{
								Compound c = clustering.getCompoundWithJmolIndex(view.getAtomCompoundIndex(atomIndex));
								boolean selectCluster = !clustering.isClusterActive() && !e.isControlDown();
								boolean selectCompound = clustering.isClusterActive() || e.isShiftDown()
										|| singleCompoundSelection;

								if (selectCompound)
								{
									if (e.isControlDown())
										toggleCompoundActive(c);
									else
										setCompoundActive(c, true);
								}
								else if (selectCluster) // compound is not selected
								{
									setClusterActive(clustering.getClusterForCompound(c), true, false);
								}
							}
							else if (e.getClickCount() > 1)
							{
								if (clustering.isClusterActive())
								{
									if (clustering.isCompoundActive()
											&& clustering.getActiveCluster().getNumCompounds() > 1)
										clearCompoundActive(true);
									else
										clearClusterActive(true, true);
								}
								else
									clearCompoundActive(true);
							}
						}
					});
					th.start();
				}
				else if (SwingUtilities.isRightMouseButton(e))
				{
					if (popup == null)
						popup = MainPanel.this.guiControler.getPopup();
					else
						popup.updateUI();
					popup.show(e.getComponent(), e.getX(), e.getY());
				}
			}
		});

		jmolPanel.addMouseMotionListener(new MouseAdapter()
		{
			public void mouseMoved(final MouseEvent ev)
			{
				updateMouse(ev.getPoint(), ev.isShiftDown());
			}
		});
	}

	Thread mouseMoveUpdateThread;
	boolean mouseMoveUpdate = false;
	Runnable mouseMoveRunnable = null;

	@Override
	public void clearMouseMoveWatchUpdates(boolean clearWatched)
	{
		if (mouseMoveUpdateThread != null)
			synchronized (mouseMoveUpdateThread)
			{
				this.mouseMoveRunnable = null;
				mouseMoveUpdate = false;
			}
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				clearCompoundWatched();
				clearClusterWatched();
			}
		});
	}

	@Override
	public void doMouseMoveWatchUpdates(Runnable run)
	{
		if (mouseMoveUpdateThread == null)
		{
			mouseMoveUpdateThread = new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					while (true)
					{
						Runnable r = null;
						synchronized (mouseMoveUpdateThread)
						{
							if (mouseMoveUpdate)
								r = mouseMoveRunnable;
						}
						ThreadUtil.sleep(35);
						synchronized (mouseMoveUpdateThread)
						{
							if (mouseMoveUpdate && r == mouseMoveRunnable)//check for override by new event 
							{
								final Runnable fr = r;
								SwingUtilities.invokeLater(new Runnable()
								{
									public void run()
									{
										fr.run();
									}
								});
								mouseMoveUpdate = false;
							}
						}
						ThreadUtil.sleep(10);
					}
				}
			});
			mouseMoveUpdateThread.start();
		}
		synchronized (mouseMoveUpdateThread)
		{
			mouseMoveRunnable = run;
			mouseMoveUpdate = true;
		}
	}

	Point mousePos;

	@Override
	public void updateMouseSelection(boolean shiftDown)
	{
		updateMouse(mousePos, shiftDown);
	}

	private void updateMouse(Point mousePos, final boolean shiftDown)
	{
		if (guiControler.isBlocked())
			return;

		this.mousePos = mousePos;

		for (JComponent c : ignoreMouseMovementPanels)
		{
			if (c.isVisible())
			{
				Point p = SwingUtilities.convertPoint(MainPanel.this, mousePos, c);
				if (p.x >= 0 && p.y >= 0 && p.x <= c.getWidth() && p.y <= c.getHeight())
					return;
			}
		}
		final int atomIndex = view.findNearestAtomIndex(mousePos.x, mousePos.y);
		if (atomIndex == -1)
			clearMouseMoveWatchUpdates(true);
		else
			doMouseMoveWatchUpdates(new Runnable()
			{
				@Override
				public void run()
				{
					if (!clustering.isClusterActive() && !shiftDown && !singleCompoundSelection)
					{
						clustering.getCompoundWatched().clearSelection();
						//clustering.getCompoundActive().clearSelection();
						clustering.getClusterWatched().setSelected(
								(clustering.getClusterIndexForJmolIndex(view.getAtomCompoundIndex(atomIndex))));
					}
					else
					{
						clustering.getClusterWatched().clearSelection();
						clustering.getCompoundWatched().setSelected(view.getAtomCompoundIndex(atomIndex));
					}
				}
			});
	}

	@Override
	public void addIgnoreMouseMovementComponents(JComponent c)
	{
		this.ignoreMouseMovementPanels.add(c);
	}

	public Style getStyle()
	{
		return style;
	}

	private boolean putSpheresBackOn = false;

	public void setStyle(Style style)
	{
		if (this.style != style)
		{
			if (style == Style.dots && highlightMode == HighlightMode.Spheres)
			{
				setHighlightMode(HighlightMode.ColorCompounds);
				putSpheresBackOn = true;
			}
			else if ((style == Style.ballsAndSticks || style == Style.wireframe) && putSpheresBackOn)
			{
				setHighlightMode(HighlightMode.Spheres);
				putSpheresBackOn = false;
			}
			guiControler.block("changing style");
			this.style = style;
			updateAllClustersAndCompounds(true);//force because of bounding boxes
			fireViewChange(PROPERTY_STYLE_CHANGED);
			if (style == Style.ballsAndSticks)
				guiControler.showMessage("Draw compounds with balls (atoms) and sticks (bonds).");
			else if (style == Style.wireframe)
				guiControler.showMessage("Draw compounds with wireframes (shows only bonds).");
			else if (style == Style.dots)
				guiControler.showMessage("Draw compounds as dots.");
			guiControler.unblock("changing style");
		}
	}

	@Override
	public HashMap<String, Highlighter[]> getHighlighters()
	{
		return highlighters;
	}

	@Override
	public Highlighter getHighlighter()
	{
		return selectedHighlighter;
	}

	@Override
	public void setHighlighter(Highlighter highlighter)
	{
		setHighlighter(highlighter, true);
	}

	@Override
	public void setHighlighter(Highlighter highlighter, boolean showMessage)
	{
		if (this.selectedHighlighter != highlighter)
		{
			guiControler.block("set highlighter");
			lastSelectedHighlighter = selectedHighlighter;
			selectedHighlighter = highlighter;
			if (selectedHighlighter instanceof CompoundPropertyHighlighter)
				selectedHighlightCompoundProperty = ((CompoundPropertyHighlighter) highlighter).getProperty();
			else
				selectedHighlightCompoundProperty = null;

			updateAllClustersAndCompounds(true);//force to make sure that sphere positions is right (otherwise - dots - change size - b&s - feature failes)
			fireViewChange(PROPERTY_HIGHLIGHT_CHANGED);
			if (showMessage)
			{
				String lastMsg = ".";
				if (highlightLastFeatureEnabled && lastSelectedHighlighter != null
						&& lastSelectedHighlighter != Highlighter.DEFAULT_HIGHLIGHTER)
				{
					if (lastSelectedHighlighter == Highlighter.CLUSTER_HIGHLIGHTER)
						lastMsg = " (flattened spheroid highlights cluster assignement).";
					else if (lastSelectedHighlighter instanceof CompoundPropertyHighlighter)
						lastMsg = " (flattened spheroid highlights '" + lastSelectedHighlighter + "').";
				}
				if (highlighter == Highlighter.DEFAULT_HIGHLIGHTER)
					guiControler.showMessage("Disable highlighting" + lastMsg);
				else if (highlighter == Highlighter.CLUSTER_HIGHLIGHTER)
					guiControler.showMessage("Highlight cluster assignement" + lastMsg);
				else if (highlighter instanceof CompoundPropertyHighlighter)
					guiControler.showMessage("Highlight feature values of '" + highlighter + "'" + lastMsg);
			}
			guiControler.unblock("set highlighter");
		}
	}

	@Override
	public void setHighlighter(CompoundProperty prop)
	{
		Highlighter high = getHighlighter(prop);
		if (high != null)
			setHighlighter(high);
	}

	@Override
	public Highlighter getHighlighter(SubstructureSmartsType type)
	{
		for (Highlighter hs[] : highlighters.values())
			for (Highlighter h : hs)
				if (h instanceof SubstructureHighlighter && ((SubstructureHighlighter) h).getType() == type)
					return h;
		return null;
	}

	@Override
	public Highlighter getHighlighter(CompoundProperty p)
	{
		for (Highlighter hs[] : highlighters.values())
			for (Highlighter h : hs)
				if (h instanceof CompoundPropertyHighlighter && ((CompoundPropertyHighlighter) h).getProperty() == p)
					return h;
		return null;
	}

	@Override
	public CompoundProperty getHighlightedProperty()
	{
		return selectedHighlightCompoundProperty;
	}

	@Override
	public void setHighlighter(SubstructureSmartsType type)
	{
		Highlighter high = getHighlighter(type);
		if (high != null)
			setHighlighter(high);
	}

	@Override
	public void setSelectLastSelectedHighlighter()
	{
		setHighlighter(lastSelectedHighlighter);
	}

	@Override
	public boolean isHighlighterLabelsVisible()
	{
		return highlighterLabelsVisible;
	}

	@Override
	public void setHighlighterLabelsVisible(boolean selected)
	{
		if (this.highlighterLabelsVisible != selected)
		{
			highlighterLabelsVisible = selected;
			updateAllClustersAndCompounds(false);
			fireViewChange(PROPERTY_HIGHLIGHT_CHANGED);
			if (selected)
				guiControler.showMessage("Show label for each compound feature value.");
			else
				guiControler.showMessage("Do not show label for each compound feature value.");
		}
	}

	@Override
	public void setHighlightSorting(HighlightSorting sorting)
	{
		if (this.highlightSorting != sorting)
		{
			highlightSorting = sorting;
			updateAllClustersAndCompounds(false);
			fireViewChange(PROPERTY_HIGHLIGHT_CHANGED);
			if (sorting == HighlightSorting.Max)
				guiControler
						.showMessage("Highlight superimposed cluster using the compound with the maximum feature value.");
			else if (sorting == HighlightSorting.Median)
				guiControler
						.showMessage("Highlight superimposed cluster using the compound with the median feature value.");
			else if (sorting == HighlightSorting.Min)
				guiControler
						.showMessage("Highlight superimposed cluster using the compound with the minimum feature value.");
		}
	}

	@Override
	public HighlightSorting getHighlightSorting()
	{
		return highlightSorting;
	}

	private static double boxTranslucency = 0.05;

	/**
	 * udpates all clusters
	 */
	private void updateAllClustersAndCompounds(boolean forceUpdate)
	{
		if (forceUpdate || selectedHighlightCompoundProperty != clustering.getHighlightProperty())
			clustering.setHighlighProperty(selectedHighlightCompoundProperty,
					getHighlightColor(clustering, selectedHighlightCompoundProperty, true));

		//		int a = getClustering().getClusterActive().getSelected();
		for (int j = 0; j < clustering.numClusters(); j++)
			updateCluster(j, forceUpdate);
		updateCompoundVisibilty(forceUpdate);
		for (Compound m : clustering.getCompounds(true))
			if (m.isVisible())
				updateCompound(m.getJmolIndex(), forceUpdate);
	}

	private void updateCluster(int clusterIndex, boolean forceUpdate)
	{
		Cluster c = clustering.getCluster(clusterIndex);
		boolean watch = clustering.getClusterActive().getSelected() == -1
				&& clusterIndex == clustering.getClusterWatched().getSelected() && c.getNumCompounds() > 0;
		if (forceUpdate || watch != c.isWatched())
		{
			c.setWatched(watch);
			if (watch)
			{
				view.select(style == Style.dots ? c.getDotModeDisplayBitSet() : c.getBitSet());
				view.scriptWait("boundbox { selected }");
				view.scriptWait("boundbox off");
				//				view.scriptWait("font bb" + clusterIndex + " " + View.FONT_SIZE);
				view.scriptWait("draw ID bb" + clusterIndex + " BOUNDBOX color "
						+ ColorUtil.toJMolString(ComponentFactory.LIST_WATCH_BACKGROUND) + " translucent "
						+ boxTranslucency + " MESH NOFILL \"" + c.toStringWithValue() + "\"");

				//				jmolPanel.repaint(); // HACK to avoid label display errors
			}
			else
				view.scriptWait("draw bb" + clusterIndex + " off");
		}
		if (forceUpdate || selectedHighlightCompoundProperty != c.getHighlightProperty())
			c.setHighlighProperty(selectedHighlightCompoundProperty,
					getHighlightColor(c, selectedHighlightCompoundProperty, true));
		c.setHighlightSorting(highlightSorting);
	}

	private void updateCompoundVisibilty(boolean forceUpdate)
	{
		BitSet toDisplay = new BitSet();
		BitSet toHide = new BitSet();
		int activeCluster = clustering.getClusterActive().getSelected();

		for (Compound m : clustering.getCompounds(true))
		{
			boolean visible;
			if (activeCluster != -1)
			{
				// there is a active cluster
				if (clustering.getClusterIndexForJmolIndex(m.getJmolIndex()) != activeCluster)
					// the compound is not in the active cluster
					visible = false;
				else
				{ // compound is in the active cluster
					if (clustering.getCompoundActive().isSelected(m.getJmolIndex())
							|| clustering.getCompoundWatched().isSelected(m.getJmolIndex()))
						visible = true;
					else if (clustering.getCompoundActive().getNumSelected() == 1 && zoomToSingleSelectedCompound)
					{
						// a single different compound is selected
						visible = disguiseUnZoomed != DisguiseMode.invisible;
					}
					else
						visible = true;
				}
			}
			else
				// there is no active cluster, therefore compound cannot be hidden
				visible = true;
			if (compoundFilter != null && !compoundFilter.accept(m))
				visible = false;

			if (!visible)
			{
				// this is required for invisible compounds as well, e.g. for the compound-list and sorting stuff
				m.setHighlightCompoundProperty(selectedHighlightCompoundProperty);
			}
			if (!visible && (m.isVisible() || forceUpdate))
			{
				if (m.isSphereVisible() || forceUpdate)
				{
					m.setSphereVisible(false);
					view.hideSphere(m);
				}
				if (m.isShowActiveBox() || forceUpdate)
				{
					m.setShowActiveBox(false);
					view.scriptWait("draw bb" + m.getJmolIndex() + "a OFF");
				}
				if (m.isShowHoverBox() || forceUpdate)
				{
					m.setShowHoverBox(false);
					view.scriptWait("draw bb" + m.getJmolIndex() + "h OFF");
				}
				m.setVisible(visible);
				toHide.or(style == Style.dots ? m.getDotModeDisplayBitSet() : m.getBitSet());
			}
			else if (visible && (!m.isVisible() || forceUpdate))
			{
				m.setVisible(visible);
				toDisplay.or(style == Style.dots ? m.getDotModeDisplayBitSet() : m.getBitSet());
			}
		}
		view.hide(toHide);
		view.display(toDisplay);
	}

	public static boolean vectorEquals(Vector3f o1, Vector3f o2)
	{
		if (o1 == null)
			return o2 == null;
		else if (o2 == null)
			return false;
		else
			return o1.x == o2.x && o1.y == o2.y && o1.z == o2.z;
	}

	/**
	 * updates single compound
	 * forceUpdate = true -> everything is reset (independent of compound is part of active cluster or if single props have changed)
	 * 
	 * shows/hides box around compound
	 * show/hides compound label
	 * set compound translucent/opaque
	 * highlight substructure in compound 
	 */
	private void updateCompound(int compoundJmolIndex, boolean forceUpdate)
	{
		int clus = clustering.getClusterIndexForJmolIndex(compoundJmolIndex);
		Cluster c = clustering.getCluster(clus);
		Compound m = clustering.getCompoundWithJmolIndex(compoundJmolIndex);
		if (m == null)
		{
			Settings.LOGGER.warn("compound is null!");
			return;
		}
		int activeCluster = clustering.getClusterActive().getSelected();
		int watchedCluster = clustering.getClusterWatched().getSelected();

		boolean showHoverBox = false;
		boolean showHoverBoxLabels = false;
		boolean showActiveBox = false;
		boolean showLabel = false;
		boolean translucent = false;

		// inside the active cluster
		if (clus == activeCluster)
		{
			if (!clustering.getCompoundWatched().isSelected(compoundJmolIndex)
					&& !clustering.getCompoundActive().isSelected(compoundJmolIndex))
			{
				int numWatched = clustering.getCompoundWatched().getNumSelected();
				int numActive = clustering.getCompoundActive().getNumSelected();

				if (numWatched > 0 || numActive > 1 || (numActive == 1 && !(view.getZoomTarget() instanceof Compound)))
					translucent |= (disguiseUnHovered == DisguiseMode.translucent || c.isSuperimposed());
				if (numActive == 1 && view.getZoomTarget() instanceof Compound)
					translucent |= disguiseUnZoomed == DisguiseMode.translucent;
			}

			if (selectedHighlighter instanceof CompoundPropertyHighlighter)
				showLabel = true;

			if (clustering.getCompoundWatched().isSelected(compoundJmolIndex) && !c.isSuperimposed())
			{
				showHoverBox = true;
				showHoverBoxLabels = clustering.getCompoundWatched().getNumSelected() == 1;
			}
			if (clustering.getCompoundActive().isSelected(compoundJmolIndex) && !c.isSuperimposed())
				showActiveBox = true;
		}
		else
		{
			List<Compound> compounds;
			if (selectedHighlighter instanceof CompoundPropertyHighlighter)
				compounds = c.getCompoundsInOrder(((CompoundPropertyHighlighter) selectedHighlighter).getProperty(),
						highlightSorting);
			else
				compounds = c.getCompounds();

			if (selectedHighlightCompoundProperty != null
					&& (compounds.indexOf(m) == 0 || !clustering.isSuperimposed()))
				showLabel = true;

			if (clustering.isSuperimposed())
				translucent = (compounds.indexOf(m) > 0);
			else if (disguiseUnHovered == DisguiseMode.translucent)
			{
				if (clustering.isCompoundWatched() || clustering.isCompoundActive())
				{
					translucent = !(clustering.getCompoundWatched().isSelected(compoundJmolIndex) || clustering
							.getCompoundActive().isSelected(compoundJmolIndex));
				}
				else if (watchedCluster == -1 || selectedHighlighter == Highlighter.CLUSTER_HIGHLIGHTER)
					translucent = false;
				else
					translucent = (clus != watchedCluster);
			}

			if (clustering.getCompoundWatched().isSelected(compoundJmolIndex) && !c.isSuperimposed())
			{
				showHoverBox = true;
				showHoverBoxLabels = clustering.getCompoundWatched().getNumSelected() == 1;
			}
			if (clustering.getCompoundActive().isSelected(compoundJmolIndex) && !c.isSuperimposed())
				showActiveBox = true;
		}

		String smarts = null;
		if (style != Style.dots)
		{
			if (selectedHighlighter instanceof SubstructureHighlighter)
				smarts = c.getSubstructureSmarts(((SubstructureHighlighter) selectedHighlighter).getType());
			else if (selectedHighlighter instanceof CompoundPropertyHighlighter
					&& ((CompoundPropertyHighlighter) selectedHighlighter).getProperty() instanceof FragmentProperty)
				smarts = ((FragmentProperty) ((CompoundPropertyHighlighter) selectedHighlighter).getProperty())
						.getSmarts();
		}

		if (!highlighterLabelsVisible)
			showLabel = false;

		Color highlightColorCompound = getHighlightColor(selectedHighlighter, m, selectedHighlightCompoundProperty,
				false);
		Color highlightColorText = getHighlightColor(selectedHighlighter, m, selectedHighlightCompoundProperty, true);

		String highlightColorString = highlightColorCompound == null ? null : "color "
				+ ColorUtil.toJMolString(highlightColorCompound);
		String compoundColor;
		if (highlightMode == HighlightMode.Spheres)
			compoundColor = "color cpk";
		else
			compoundColor = highlightColorCompound == null ? "color cpk" : "color "
					+ ColorUtil.toJMolString(highlightColorCompound);
		if (compoundColor.equals("color cpk") && style == Style.dots)
			compoundColor = "color "
					+ ColorUtil.toJMolString(isBlackgroundBlack() ? Color.LIGHT_GRAY.brighter() : Color.GRAY);

		boolean sphereVisible = (highlightMode == HighlightMode.Spheres && highlightColorString != null);
		boolean lastFeatureSphereVisible = sphereVisible && highlightLastFeatureEnabled;

		Translucency translucency;
		if (translucent)
		{
			if (clustering.isSuperimposed())
			{
				translucency = Translucency.Strong;
				if (highlightMode == HighlightMode.Spheres)
					sphereVisible = false;
			}
			else
			{
				if (clus == activeCluster)
				{
					if (c.getNumCompounds() <= 5)
						translucency = Translucency.ModerateWeak;
					else if (c.getNumCompounds() <= 15)
						translucency = Translucency.ModerateStrong;
					else
						translucency = Translucency.Strong;
				}
				else
					translucency = Translucency.ModerateStrong;
			}
		}
		else
			translucency = Translucency.None;

		boolean styleUpdate = style != m.getStyle();
		boolean styleDotUpdate = (style == Style.dots && m.getStyle() != Style.dots)
				|| (style != Style.dots && m.getStyle() == Style.dots);
		boolean compoundUpdate = styleUpdate || translucency != m.getTranslucency()
				|| !ObjectUtil.equals(compoundColor, m.getCompoundColor());
		//showLabel is enough because superimpose<->not-superimpose is not tracked in compound
		boolean checkLabelUpdate = showLabel || (!showLabel && m.getLabel() != null)
				|| !ObjectUtil.equals(compoundColor, m.getCompoundColor())
				|| selectedHighlightCompoundProperty != m.getHighlightCompoundProperty();
		boolean sphereUpdate = sphereVisible != m.isSphereVisible()
				|| (lastFeatureSphereVisible != m.isLastFeatureSphereVisible())
				|| (sphereVisible && (translucency != m.getTranslucency() || !ObjectUtil.equals(highlightColorString,
						m.getHighlightColorString())));
		boolean spherePositionUpdate = sphereVisible && !vectorEquals(m.getPosition(), m.getSpherePosition());
		boolean hoverBoxUpdate = showHoverBox != m.isShowHoverBox();
		boolean activeBoxUpdate = showActiveBox != m.isShowActiveBox();
		boolean smartsUpdate = compoundUpdate || smarts != m.getHighlightedSmarts();

		m.setCompoundColor(compoundColor);
		m.setStyle(style);
		m.setTranslucency(translucency);
		m.setHighlightCompoundProperty(selectedHighlightCompoundProperty);
		m.setHighlightColor(highlightColorString, highlightColorText);
		m.setSpherePosition(m.getPosition());
		m.setSphereVisible(sphereVisible);
		m.setLastFeatureSphereVisible(lastFeatureSphereVisible);
		m.setShowHoverBox(showHoverBox);
		m.setShowActiveBox(showActiveBox);

		if (styleDotUpdate)// || forceUpdate)
		{
			if (style == Style.dots)
			{
				view.hide(m.getDotModeHideBitSet());
				clustering.moveForDotMode(m, true);
			}
			else
			{
				clustering.moveForDotMode(m, false);
				view.display(m.getDotModeHideBitSet());
			}
		}

		// SET SELECTION
		if (style == Style.dots)
			view.select(m.getDotModeDisplayBitSet());
		else
			view.select(m.getBitSet());

		if (forceUpdate || styleUpdate)
			view.scriptWait(getStyleString());

		if (forceUpdate || compoundUpdate)
			view.scriptWait(compoundColor + getColorSuffixTranslucent(translucency));

		if (forceUpdate || sphereUpdate || spherePositionUpdate)
		{
			if (sphereVisible)
				view.showSphere(m, lastFeatureSphereVisible, forceUpdate || spherePositionUpdate);
			else
				view.hideSphere(m);
		}

		if (forceUpdate || hoverBoxUpdate)
		{
			if (showHoverBox)
			{
				if (style == Style.dots)
					view.scriptWait("boundbox { selected } { 2 2 2 }");
				else
					view.scriptWait("boundbox { selected }");
				view.scriptWait("boundbox off");
				//				view.scriptWait("font bb" + m.getJmolIndex() + "h " + View.FONT_SIZE);
				String label = "";
				if (showHoverBoxLabels)
					label = " \"" + m.toStringWithValue() + "\"";
				view.scriptWait("draw ID bb" + m.getJmolIndex() + "h BOUNDBOX color "
						+ ColorUtil.toJMolString(ComponentFactory.LIST_WATCH_BACKGROUND) + " translucent "
						+ boxTranslucency + " MESH NOFILL" + label);

				//				jmolPanel.repaint(); // HACK to avoid label display errors
			}
			else
				view.scriptWait("draw bb" + m.getJmolIndex() + "h OFF");
		}

		if (forceUpdate || activeBoxUpdate)
		{
			if (showActiveBox)
			{
				if (style == Style.dots)
					view.scriptWait("boundbox { selected } { 2 2 2 }");
				else
					view.scriptWait("boundbox { selected }");
				view.scriptWait("boundbox off");
				//				view.scriptWait("font bb" + m.getJmolIndex() + "a " + View.FONT_SIZE);
				view.scriptWait("draw ID bb" + m.getJmolIndex() + "a BOUNDBOX color "
						+ ColorUtil.toJMolString(ComponentFactory.LIST_ACTIVE_BACKGROUND) + " translucent "
						+ boxTranslucency + " MESH NOFILL");

				//				jmolPanel.repaint(); // HACK to avoid label display errors
			}
			else
			{
				view.scriptWait("draw bb" + m.getJmolIndex() + "a OFF");
			}
		}

		// CHANGES JMOL SELECTION !!!
		if (forceUpdate || smartsUpdate)
		{
			boolean match = false;
			if (smarts != null && smarts.length() > 0)
			{
				BitSet matchBitSet = m.getSmartsMatch(smarts);
				if (matchBitSet.cardinality() > 0)
				{
					match = true;
					if (m.getHighlightedSmarts() != null)
					{
						// first draw whole compound red again, to remove old match
						if (highlightMode == HighlightMode.Spheres)
							view.scriptWait("color cpk" + getColorSuffixTranslucent(translucency));
						else
							view.scriptWait(compoundColor + getColorSuffixTranslucent(translucency));
					}
					m.setHighlightedSmarts(smarts);
					view.select(matchBitSet);
					view.scriptWait("color " + View.convertColor(CompoundPropertyUtil.HIGHILIGHT_MATCH_COLORS[2])
							+ getColorSuffixTranslucent(translucency));
				}
			}
			if (!match)// || forceUpdate || translucentUpdate)
			{
				m.setHighlightedSmarts(null);
				if (highlightMode == HighlightMode.Spheres)
					view.scriptWait("color cpk" + getColorSuffixTranslucent(translucency));
				else
					view.scriptWait(compoundColor + getColorSuffixTranslucent(translucency));
			}
		}

		// MAY CHANGE JMOL SELECTION !!!
		if (forceUpdate || checkLabelUpdate)
		{
			String labelString = null;
			if (showLabel)
			{
				if (activeCluster == -1 && clustering.isSuperimposed())
				{
					labelString = c.getName() + " - "
							+ ((CompoundPropertyHighlighter) selectedHighlighter).getProperty() + ": "
							+ c.getSummaryStringValue(selectedHighlightCompoundProperty, false);
				}
				else
				{
					CompoundProperty p = ((CompoundPropertyHighlighter) selectedHighlighter).getProperty();
					Object val = clustering.getCompoundWithJmolIndex(compoundJmolIndex).getFormattedValue(p);
					//				Settings.LOGGER.warn("label : " + i + " : " + c + " : " + val);
					labelString = ((CompoundPropertyHighlighter) selectedHighlighter).getProperty() + ": " + val;
				}
			}

			// CHANGES JMOL SELECTION!!
			if (forceUpdate || !ObjectUtil.equals(labelString, m.getLabel()))
			{
				m.setLabel(labelString);

				view.selectFirstCarbonAtom(m.getBitSet());
				//				BitSet empty = new BitSet(bs.length());
				//				empty.set(bs.nextSetBit(0));
				//				view.select(empty);

				if (showLabel)
				{
					view.scriptWait("set fontSize " + ScreenSetup.INSTANCE.getFontSize());
					view.scriptWait("label \"" + labelString + "\"");
				}
				else
				{
					//				Settings.LOGGER.warn("label : " + i + " : " + c + " : off");
					view.scriptWait("label OFF");
				}
			}
		}
	}

	public void init(ClusteringData clusteredDataset)
	{
		if (clustering.numClusters() > 0)
			throw new IllegalStateException("only done once at the start");
		clustering.newClustering(clusteredDataset);
		clustering.initFeatureNormalization();

		clustering.getClusterActive().addListenerFirst(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent e)
			{
				int cIndexOldArray[] = ((int[]) e.getOldValue());
				int cIndexOld = cIndexOldArray.length == 0 ? -1 : cIndexOldArray[0];
				singleCompoundSelection = false;
				updateClusterSelection(clustering.getClusterActive().getSelected(), cIndexOld, true);
			}
		});
		clustering.getClusterWatched().addListenerFirst(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent e)
			{
				int cIndexOldArray[] = ((int[]) e.getOldValue());
				int cIndexOld = cIndexOldArray.length == 0 ? -1 : cIndexOldArray[0];
				updateClusterSelection(clustering.getClusterWatched().getSelected(), cIndexOld, false);
			}
		});
		clustering.getCompoundActive().addListenerFirst(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent e)
			{
				updateCompoundActiveSelection(((int[]) e.getNewValue()), ((int[]) e.getOldValue()));
			}
		});
		clustering.getCompoundWatched().addListenerFirst(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent e)
			{
				updateAllClustersAndCompounds(false);
			}
		});
		clustering.addListenerFirst(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				if (evt.getPropertyName().equals(ClusteringImpl.CLUSTER_ADDED))
					updateClusteringNew();
				else if (evt.getPropertyName().equals(ClusteringImpl.CLUSTER_REMOVED))
					updateClusterRemoved();
				else if (evt.getPropertyName().equals(ClusteringImpl.CLUSTER_MODIFIED))
					updateAllClustersAndCompounds(true);
				else if (evt.getPropertyName().equals(ClusteringImpl.PROPERTY_ADDED))
					newHighlighters(false);
			}
		});
		updateClusteringNew();
	}

	private void newHighlighters(boolean init)
	{
		Highlighter[] h = new Highlighter[] { Highlighter.DEFAULT_HIGHLIGHTER };
		if (clustering.getNumClusters() > 1)
			h = ArrayUtil.concat(Highlighter.class, h, new Highlighter[] { Highlighter.CLUSTER_HIGHLIGHTER });
		if (clustering.getAdditionalProperties() != null)
			for (CompoundProperty p : clustering.getAdditionalProperties())
				h = ArrayUtil.concat(Highlighter.class, h, new Highlighter[] { CompoundPropertyHighlighter.create(p) });

		if (clustering.getSubstructureSmartsType() != null)
			h = ArrayUtil.concat(Highlighter.class, h,
					new Highlighter[] { SubstructureHighlighter.create(clustering.getSubstructureSmartsType()) });

		highlighters = new LinkedHashMap<String, Highlighter[]>();
		highlighters.put("", h);

		List<CompoundProperty> props = clustering.getProperties();
		CompoundPropertyHighlighter[] featureHighlighters = new CompoundPropertyHighlighter[props.size()];
		int fCount = 0;
		for (CompoundProperty p : props)
			featureHighlighters[fCount++] = CompoundPropertyHighlighter.create(p);
		highlighters.put("Features NOT selected for mapping", featureHighlighters);

		props = clustering.getFeatures();
		featureHighlighters = new CompoundPropertyHighlighter[props.size()];
		fCount = 0;
		for (CompoundProperty p : props)
			featureHighlighters[fCount++] = CompoundPropertyHighlighter.create(p);
		highlighters.put("Features selected for mapping", featureHighlighters);

		if (init)
		{
			if (clustering.getNumClusters() > 1)
				selectedHighlighter = Highlighter.CLUSTER_HIGHLIGHTER;
			else
				selectedHighlighter = Highlighter.DEFAULT_HIGHLIGHTER;
			selectedHighlightCompoundProperty = null;
			lastSelectedHighlighter = selectedHighlighter;
			highlightAutomatic.init();
		}
		fireViewChange(PROPERTY_NEW_HIGHLIGHTERS);
	}

	private void updateClusteringNew()
	{
		newHighlighters(true);

		updateAllClustersAndCompounds(true);

		view.scriptWait("frame " + view.getCompoundNumberDotted(0) + " "
				+ view.getCompoundNumberDotted(clustering.numCompounds() - 1));

		setSpinEnabled(isSpinEnabled(), true);
		if (clustering.isBigDataMode() && style != Style.dots)
			setStyle(Style.dots);

		initCompoundDescriptor();

		view.suspendAnimation("new clustering");
		if (!isAllClustersSpreadable())
			setSuperimpose(true);
		if (clustering.getNumClusters() == 1)
			clustering.getClusterActive().setSelected(0);
		else
			view.zoomTo(clustering, null);
		view.proceedAnimation("new clustering");

		if (clustering.isRandomEmbedding())
			guiControler.showMessage(clustering.getName());
		else
			guiControler.showMessage("Chemical space mapping of " + clustering.getName());
	}

	private void updateClusterRemoved()
	{
		updateAllClustersAndCompounds(true);
		if (clustering.getNumClusters() <= 1)
			newHighlighters(selectedHighlighter == Highlighter.CLUSTER_HIGHLIGHTER);
		if (clustering.getNumClusters() == 1)
			clustering.getClusterActive().setSelected(0);
	}

	int ctrlHintCount = 3;

	private void updateCompoundActiveSelection(int mIndex[], int mIndexOld[])
	{
		int activeCluster = clustering.getClusterActive().getSelected();

		Zoomable currentZoom = view.getZoomTarget();
		final Zoomable z;
		if (!clustering.isSuperimposed())
		{
			if (mIndex.length == 1)
			{ // one compound selected
				if (zoomToSingleSelectedCompound)
					z = clustering.getCompoundWithJmolIndex(mIndex[0]);
				else
					z = null;
			}
			else if (mIndex.length > 1)
			{ // more than one compound selected -> zoom out if necessary
				if (currentZoom instanceof Compound)
					z = clustering.getUniqueClusterForJmolIndices(mIndex);
				else
					z = null;
			}
			else
			{ // no compound selected -> zoom back to cluster of previously selected compound
				if (currentZoom instanceof Compound && mIndexOld.length > 0 && activeCluster != -1)
					z = clustering.getClusterForJmolIndex(mIndexOld[0]);
				else
					z = null;
			}
		}
		else
			z = null;

		if (z == null || z instanceof Cluster)
			updateAllClustersAndCompounds(false);
		if (z != null)
		{
			runInBackground(new Runnable()
			{
				public void run()
				{
					if (view.getZoomTarget() != z && view.isAnimated())
					{
						if (z instanceof Compound)
						{
							String msg = "Zoom to compound '" + z + "'.";
							if (ctrlHintCount > 0)
							{
								msg = "<html><p align=\"center\">"
										+ msg
										+ "<br><span style=\"font-size:"
										+ (int) (ScreenSetup.INSTANCE.getFontSize() * 0.8)
										+ "px\">Hold down 'Ctrl'-key to select (multiple) compounds without zooming in.</span></p></html>";
								ctrlHintCount--;
							}
							guiControler.showMessage(msg);
						}
						else if (clustering.getNumClusters() == 1)
							guiControler.showMessage("Zoom out to show all compounds.");
						else
							guiControler.showMessage("Zoom to cluster '" + z + "'.");
						view.zoomTo(z, AnimationSpeed.SLOW);
					}
					if (z instanceof Compound) // zooming in, update afterwards
					{
						updateAllClustersAndCompounds(false);
					}
				}
			});
		}

	}

	/**
	 * the cluster selection has changed
	 * active=true -> the selection change is about active/inactive
	 * active=false -> the selection change is about watched/not-watched
	 * 
	 * @param cIndex
	 * @param cIndexOld
	 * @param activeClusterChanged
	 */
	private void updateClusterSelection(int cIndex, final int cIndexOld, final boolean activeClusterChanged)
	{
		// ignore watch updates when a cluster is active
		if (!activeClusterChanged && clustering.isClusterActive())
			return;

		if (clustering.getNumClusters() == 0)
			return;

		if (!activeClusterChanged)
			updateAllClustersAndCompounds(false);
		else
		{
			// reset to cluster before zooming out
			boolean updated = highlightAutomatic.resetClusterHighlighter(activeClusterChanged);
			if (!updated && cIndexOld != -1)
				//zooming away from cluster, update before
				updateAllClustersAndCompounds(false);

			if (cIndex == -1)
				guiControler.showMessage("Zoom out to show all clusters.");
			else if (clustering.getNumClusters() == 1)
				guiControler.showMessage("Zoom out to show all compounds.");
			else
				guiControler.showMessage("Zoom to cluster '" + clustering.getCluster(cIndex) + "'.");

			if (guiControler.isVisible())
				runInBackground(new Runnable()
				{

					@Override
					public void run()
					{
						zoomAndSuperimpose(true, false);
						SwingUtil.invokeAndWait(new Runnable()
						{
							@Override
							public void run()
							{
								boolean updated = highlightAutomatic.resetDefaultHighlighter(activeClusterChanged);
								if (!updated && cIndexOld == -1)
									//zooming away from clustering into cluster, update after
									updateAllClustersAndCompounds(false);
							}
						});
					}
				});
			else
			{
				zoomAndSuperimpose(false, false);
				updated = highlightAutomatic.resetDefaultHighlighter(activeClusterChanged);
				if (!updated && cIndexOld == -1)
					//zooming away from clustering into cluster, update after
					updateAllClustersAndCompounds(false);
			}
		}
	}

	static class JmolPanel extends JPanel
	{
		JmolSimpleViewer viewer;
		JmolAdapter adapter;

		JmolPanel()
		{
			adapter = new SmarterJmolAdapter();
			viewer = JmolSimpleViewer.allocateSimpleViewer(this, adapter);
		}

		public JmolSimpleViewer getViewer()
		{
			return viewer;
		}

		final Dimension currentSize = new Dimension();
		final Rectangle rectClip = new Rectangle();

		final Dimension dimSize = new Dimension();

		public void paint(Graphics g)
		{
			////code for old version
			//			getSize(currentSize);
			//			g.getClipBounds(rectClip);
			//			if (g != null && currentSize != null && rectClip != null)
			//				viewer.renderScreenImage(g, currentSize, rectClip);

			getSize(dimSize);
			if (dimSize.width == 0)
				return;
			viewer.renderScreenImage(g, dimSize.width, dimSize.height);
		}
	}

	@Override
	public boolean isSuperimpose()
	{
		return clustering.isSuperimposed();
	}

	@Override
	public void setSuperimpose(boolean superimpose)
	{
		if (clustering.isSuperimposed() != superimpose)
		{
			clustering.setSuperimposed(superimpose);
			if (superimpose)
				guiControler.showMessage("Move compounds to cluster center.");
			else
				guiControler.showMessage("Move compounds to compound positions.");

			if (guiControler.isVisible())
				runInBackground(new Runnable()
				{

					@Override
					public void run()
					{
						zoomAndSuperimpose(true, true);
						updateAllClustersAndCompounds(false);
					}
				});
			else
			{
				zoomAndSuperimpose(false, false);
				updateAllClustersAndCompounds(false);
			}
		}
	}

	@Override
	public boolean isAllClustersSpreadable()
	{
		for (Cluster c : clustering.getClusters())
			if (c.isSpreadable())
				return true;
		return false;
	}

	@Override
	public boolean isSingleClusterSpreadable()
	{
		if (!clustering.isClusterActive())
			throw new IllegalStateException();
		return clustering.getCluster(clustering.getClusterActive().getSelected()).isSpreadable();
	}

	@Override
	public boolean isAntialiasEnabled()
	{
		return antialiasEnabled;
	}

	@Override
	public void setAntialiasEnabled(boolean b)
	{
		if (this.antialiasEnabled != b)
		{
			this.antialiasEnabled = b;
			view.setAntialiasOn(antialiasEnabled);
			fireViewChange(PROPERTY_ANTIALIAS_CHANGED);
			guiControler.showMessage((b ? "Enable" : "Disable") + " antialiasing.");
		}
	}

	private void zoomAndSuperimpose(boolean animateZoom, boolean animateSuperimpose)
	{
		if (animateZoom || animateSuperimpose)
		{
			SwingUtil.checkNoAWTEventThread();
			if (guiControler.isVisible() && !guiControler.isBlocked())
				throw new IllegalStateException("gui not blocked");
		}

		final List<Cluster> c = new ArrayList<Cluster>();
		final Cluster activeCluster;
		final boolean superimpose = clustering.isSuperimposed();

		final boolean setAntialiasBackOn;
		if (antialiasEnabled && view.isAntialiasOn())
		{
			setAntialiasBackOn = true;
			view.setAntialiasOn(false);
		}
		else
			setAntialiasBackOn = false;

		if (animateSuperimpose && superimpose && clustering.getCompoundActive().getNumSelected() > 0)
		{
			if (animateSuperimpose)
				view.suspendAnimation("deselect for superimpose");
			SwingUtil.invokeAndWait(new Runnable()
			{
				@Override
				public void run()
				{
					clustering.getCompoundActive().clearSelection();
				}
			});
			if (animateSuperimpose)
				view.proceedAnimation("deselect for superimpose");
		}

		if (clustering.isClusterActive())
		{
			activeCluster = clustering.getCluster(clustering.getClusterActive().getSelected());
			if (activeCluster.isSuperimposed() != superimpose)
				c.add(activeCluster);
		}
		else
		{
			for (Cluster cluster : clustering.getClusters())
				if (cluster.isSuperimposed() != superimpose)
					c.add(cluster);
			activeCluster = null;
		}

		SwingUtil.invokeAndWait(new Runnable()
		{
			@Override
			public void run()
			{
				clustering.getCompoundWatched().clearSelection();
			}
		});

		if (!superimpose && animateSuperimpose) // zoom out before spreading (explicitly fetch non-superimpose diameter)
		{
			if (!animateZoom)
				view.suspendAnimation("zoom");
			if (activeCluster == null)
				view.zoomTo(clustering, AnimationSpeed.SLOW, false);
			else
				view.zoomTo(activeCluster, AnimationSpeed.SLOW, false);
			if (!animateZoom)
				view.proceedAnimation("zoom");
		}

		if (animateSuperimpose) // for superimposition or un-superimposition, hide shperes manually before moving compounds
			for (Compound compound : clustering.getCompounds(true))
				if (compound.isSphereVisible())
				{
					compound.setSphereVisible(false);
					view.hideSphere(compound);
				}

		if (c.size() > 0)
		{
			if (!animateSuperimpose)
				view.suspendAnimation("superimpose");
			clustering.setClusterOverlap(c, superimpose, View.AnimationSpeed.SLOW);
			if (!animateSuperimpose)
				view.proceedAnimation("superimpose");
		}

		if (superimpose || !animateSuperimpose) // zoom in after superimposing
		{
			final Zoomable zoom = activeCluster == null ? clustering : activeCluster;
			if (!animateZoom)
				view.suspendAnimation("zoom");
			view.zoomTo(zoom, AnimationSpeed.SLOW);
			if (!animateZoom)
				view.proceedAnimation("zoom");
		}

		if (setAntialiasBackOn && !view.isAntialiasOn())
		{
			ThreadUtil.sleep(200);
			view.setAntialiasOn(true);
		}
		SwingUtil.invokeAndWait(new Runnable()
		{
			@Override
			public void run()
			{
				fireViewChange(PROPERTY_SUPERIMPOSE_CHANGED);
				fireViewChange(PROPERTY_DENSITY_CHANGED);
			}
		});
	}

	List<PropertyChangeListener> viewListeners = new ArrayList<PropertyChangeListener>();

	@Override
	public void addViewListener(PropertyChangeListener l)
	{
		viewListeners.add(l);
	}

	private void fireViewChange(String prop)
	{
		SwingUtil.checkIsAWTEventThread();
		for (PropertyChangeListener l : viewListeners)
			l.propertyChange(new PropertyChangeEvent(this, prop, "old", "new"));
	}

	@Override
	public boolean canJitter()
	{
		if (isSuperimpose())
			return false;
		if (clustering.getActiveCompounds().length == 1 && zoomToSingleSelectedCompound)
			return false;
		if (clustering.isClusterActive() && clustering.getActiveCluster().getNumCompounds() < 2)
			return false;
		else if (!clustering.isClusterActive() && clustering.getNumCompounds() < 2)
			return false;
		return true;
	}

	@Override
	public boolean canChangeCompoundSize(boolean larger)
	{
		if (larger && ClusteringUtil.COMPOUND_SIZE == ClusteringUtil.COMPOUND_SIZE_MAX)
			return false;
		if (!larger && ClusteringUtil.COMPOUND_SIZE == 0)
			return false;
		Cluster activeCluster = clustering.getCluster(clustering.getClusterActive().getSelected());
		if (activeCluster == null)
			return true;
		else
			return !clustering.isSuperimposed();
	}

	@Override
	public void changeCompoundSize(boolean larger)
	{
		if (larger && ClusteringUtil.COMPOUND_SIZE < ClusteringUtil.COMPOUND_SIZE_MAX)
		{
			ClusteringUtil.COMPOUND_SIZE++;
			updateDensity(true);
		}
		else if (!larger && ClusteringUtil.COMPOUND_SIZE > 0)
		{
			ClusteringUtil.COMPOUND_SIZE--;
			updateDensity(false);
		}
	}

	@Override
	public void setCompoundSize(int compoundSize)
	{
		if (ClusteringUtil.COMPOUND_SIZE != compoundSize)
		{
			boolean increased = ClusteringUtil.COMPOUND_SIZE < compoundSize;
			ClusteringUtil.COMPOUND_SIZE = compoundSize;
			updateDensity(increased);
		}
	}

	@Override
	public HighlightMode getHighlightMode()
	{
		return highlightMode;
	}

	@Override
	public void setHighlightMode(HighlightMode mode)
	{
		if (highlightMode != mode)
		{
			highlightMode = mode;
			if (selectedHighlighter != Highlighter.DEFAULT_HIGHLIGHTER)
				updateAllClustersAndCompounds(true);
			fireViewChange(PROPERTY_HIGHLIGHT_MODE_CHANGED);
			if (mode == HighlightMode.Spheres)
				guiControler.showMessage("Highlight compound feature values with spheres.");
			else if (mode == HighlightMode.ColorCompounds)
				guiControler.showMessage("Highlight compound feature values by changing atom and bond colors.");
		}
	}

	@Override
	public boolean isHighlightLastFeatureEnabled()
	{
		return highlightLastFeatureEnabled;
	}

	@Override
	public void setHighlightLastFeatureEnabled(boolean b)
	{
		if (highlightLastFeatureEnabled != b)
		{
			highlightLastFeatureEnabled = b;
			if (highlightMode == HighlightMode.Spheres && selectedHighlighter != Highlighter.DEFAULT_HIGHLIGHTER)
				updateAllClustersAndCompounds(true);
			fireViewChange(PROPERTY_HIGHLIGHT_LAST_FEATURE);
			String lastMsg = ".";
			if (highlightLastFeatureEnabled && lastSelectedHighlighter != null
					&& lastSelectedHighlighter != Highlighter.DEFAULT_HIGHLIGHTER)
			{
				if (lastSelectedHighlighter == Highlighter.CLUSTER_HIGHLIGHTER)
					lastMsg = " (flattened spheroid highlights cluster assignement).";
				else if (lastSelectedHighlighter instanceof CompoundPropertyHighlighter)
					lastMsg = " (flattened spheroid highlights '" + lastSelectedHighlighter + "').";
			}
			guiControler.showMessage((b ? "Enable" : "Disable") + " highlighting of last selected feature" + lastMsg);
		}
	}

	@Override
	public void setSphereSize(double size)
	{
		if (view.sphereSize != size)
		{
			boolean increase = view.sphereSize < size;
			view.sphereSize = size;
			if (highlightMode == HighlightMode.Spheres && selectedHighlighter != Highlighter.DEFAULT_HIGHLIGHTER)
				updateAllClustersAndCompounds(true);
			guiControler.showMessage((increase ? "Increase" : "Descrease") + " sphere size to "
					+ StringUtil.formatDouble(size) + ".");
		}
	}

	@Override
	public void setSphereTranslucency(double translucency)
	{
		if (view.sphereTranslucency != translucency)
		{
			boolean increase = view.sphereTranslucency < translucency;
			view.sphereTranslucency = translucency;
			if (highlightMode == HighlightMode.Spheres && selectedHighlighter != Highlighter.DEFAULT_HIGHLIGHTER)
				updateAllClustersAndCompounds(true);
			guiControler.showMessage((increase ? "Increase" : "Descrease") + " sphere translucency to "
					+ StringUtil.formatDouble(translucency) + ".");
		}
	}

	@Override
	public int getCompoundSizeMax()
	{
		return ClusteringUtil.COMPOUND_SIZE_MAX;
	}

	@Override
	public int getCompoundSize()
	{
		return ClusteringUtil.COMPOUND_SIZE;
	}

	int jitteringLevel = 0;

	@Override
	public int getJitteringLevel()
	{
		return jitteringLevel;
	}

	private void resetJittering(HashSet<Compound> compounds, boolean skipPositionUpdate)
	{
		int idx = clustering.getJitteringResetLevel(compounds);
		if (idx != -1)
			setJitteringLevel(idx, compounds, true, skipPositionUpdate);
	}

	@Override
	public void setJitteringLevel(int level)
	{
		JitteringProvider.showJitterWarning();
		List<Compound> compounds;
		if (clustering.isClusterActive())
			compounds = clustering.getActiveCluster().getCompounds();
		else
			compounds = clustering.getCompounds(false);
		setJitteringLevel(level, new HashSet<Compound>(compounds), false, false);
	}

	private void setJitteringLevel(final int level, final HashSet<Compound> compounds, final boolean force,
			final boolean skipPositionUpdate)
	{
		if (jitteringLevel != level || force)
		{
			if (force)
				SwingUtil.checkNoAWTEventThread();
			else
				SwingUtil.checkIsAWTEventThread();

			if (!force)
				guiControler.block("jitter");
			Thread th = new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					try
					{
						final boolean inc = level > jitteringLevel;
						if (jitteringLevel == level && force)
							clustering.updateJittering(level, compounds);
						else
						{
							if (inc)
							{
								while (jitteringLevel != level)
								{
									jitteringLevel++;
									clustering.updateJittering(jitteringLevel, compounds);
								}
							}
							else
							{
								jitteringLevel = level;
								clustering.updateJittering(jitteringLevel, compounds);
							}
						}
						SwingUtil.invokeAndWait(new Runnable()
						{
							@Override
							public void run()
							{
								Compound active[] = new Compound[0];
								if (clustering.isClusterActive())
								{
									active = clustering.getActiveCompounds();
									if (active.length > 0)
										clustering.getCompoundActive().clearSelection();
								}
								if (!skipPositionUpdate)
									clustering.updatePositions();
								if (!force)
								{
									if (level == 0)
										guiControler
												.showMessage("Disable spreading (move compounds back to calculated positions).");
									else
										guiControler.showMessage((inc ? "Increase" : "Descrease") + " spread level to "
												+ level + " (move close compounds apart).");
								}
								fireViewChange(PROPERTY_JITTERING_CHANGED);
								if (!force)
								{
									view.suspendAnimation("jitter");
									if (clustering.isClusterActive())
										view.centerAt(clustering.getActiveCluster());
									else
										view.centerAt(clustering);
									view.proceedAnimation("jitter");
								}
								for (Compound compound : clustering.getCompounds(true))
									if (compound.isSphereVisible())
										view.showSphere(compound, compound.isLastFeatureSphereVisible(), true);
								if (active.length > 0)
								{
									if (active.length == 1) // do not zoom in
										toggleCompoundActive(active[0], false);
									else
										setCompoundActive(active, false);
								}
							}
						});
					}
					catch (Exception e)
					{
						Settings.LOGGER.error("jittering failed");
						Settings.LOGGER.error(e);
					}
					finally
					{
						if (!force)
							SwingUtilities.invokeLater(new Runnable()
							{
								@Override
								public void run()
								{
									guiControler.unblock("jitter");
								}
							});
					}
				}
			});
			if (!force)
				th.start();
			else
				th.run();
		}
	}

	private void updateDensity(boolean increased)
	{
		Cluster activeCluster = clustering.getCluster(clustering.getClusterActive().getSelected());
		if (activeCluster != null && clustering.isSuperimposed())
			throw new IllegalStateException("does not make sense, because superimposed!");

		Compound active[] = new Compound[0];
		if (activeCluster != null)
		{
			active = clustering.getActiveCompounds();
			if (active.length > 0)
				clustering.getCompoundActive().clearSelection();
		}

		view.suspendAnimation("change density");
		clustering.updatePositions();

		if (activeCluster != null)
		{
			Settings.LOGGER.info("zooming out - cluster");
			view.centerAt(activeCluster);
		}
		else
		{
			Settings.LOGGER.info("zooming out - home");
			view.centerAt(clustering);
		}
		for (Compound compound : clustering.getCompounds(true))
			if (compound.isSphereVisible())
				view.showSphere(compound, compound.isLastFeatureSphereVisible(), true);
		if (active.length > 0)
		{
			if (active.length == 1) // do not zoom in
				toggleCompoundActive(active[0]);
			else
				setCompoundActive(active, false);
		}

		view.proceedAnimation("change density");

		fireViewChange(PROPERTY_DENSITY_CHANGED);
		guiControler.showMessage((increased ? "Increase" : "Descrease") + " compound size to "
				+ ClusteringUtil.COMPOUND_SIZE + ".");
	}

	@Override
	public DisguiseMode getDisguiseUnHovered()
	{
		return disguiseUnHovered;
	}

	@Override
	public DisguiseMode getDisguiseUnZoomed()
	{
		return disguiseUnZoomed;
	}

	@Override
	public void setDisguiseUnHovered(DisguiseMode disguiseUnHovered)
	{
		if (this.disguiseUnHovered != disguiseUnHovered)
		{
			if (disguiseUnHovered == DisguiseMode.invisible)
				throw new IllegalArgumentException();
			this.disguiseUnHovered = disguiseUnHovered;
			updateAllClustersAndCompounds(false);
			fireViewChange(PROPERTY_DISGUISE_CHANGED);
			if (disguiseUnHovered == DisguiseMode.translucent)
				guiControler
						.showMessage("When a compound (or cluster) is selected via mouse-over, draw other compounds translucent.");
			else
				guiControler.showMessage("Ignore mouse-over selection, draw unselected compounds solid.");
		}
	}

	@Override
	public void setDisguiseUnZoomed(DisguiseMode disguiseUnZoomed)
	{
		if (this.disguiseUnZoomed != disguiseUnZoomed)
		{
			this.disguiseUnZoomed = disguiseUnZoomed;
			updateAllClustersAndCompounds(false);
			fireViewChange(PROPERTY_DISGUISE_CHANGED);
			String msg = "When the view has zoomed in to a single compound, ";
			if (disguiseUnZoomed == DisguiseMode.solid)
				msg += "draw other compounds solid.";
			else if (disguiseUnZoomed == DisguiseMode.translucent)
				msg += "draw other compounds translucent.";
			else if (disguiseUnZoomed == DisguiseMode.invisible)
				msg += "do not draw other compounds.";
			guiControler.showMessage(msg);
		}
	}

	@Override
	public boolean isHideHydrogens()
	{
		return hideHydrogens;
	}

	@Override
	public void setHideHydrogens(boolean b)
	{
		this.hideHydrogens = b;
		view.hideHydrogens(b);
		if (b)
			guiControler.showMessage("Hide hydrogens.");
		else
			guiControler.showMessage("Draw hydrogens (if available in the dataset file).");
	}

	@Override
	public boolean isBlackgroundBlack()
	{
		return ComponentFactory.isBackgroundBlack();
	}

	@Override
	public void setBackgroundBlack(boolean backgroundBlack)
	{
		setBackgroundBlack(backgroundBlack, false);
	}

	public void setBackgroundBlack(boolean backgroundBlack, boolean forceUpdate)
	{
		if (backgroundBlack != ComponentFactory.isBackgroundBlack() || forceUpdate)
		{
			guiControler.block("changing background");
			try
			{
				ComponentFactory.setBackgroundBlack(backgroundBlack);
				view.setBackground(ComponentFactory.BACKGROUND);
				updateAllClustersAndCompounds(true); // force to assign new colors
				fireViewChange(PROPERTY_BACKGROUND_CHANGED);
				guiControler.showMessage("Background color set to " + (backgroundBlack ? "black." : "white."));
			}
			finally
			{
				guiControler.unblock("changing background");
			}
		}
	}

	@Override
	public void setFontSize(int font)
	{
		if (font != ScreenSetup.INSTANCE.getFontSize())
		{
			boolean wasLarge = ScreenSetup.INSTANCE.isFontSizeLarge();
			boolean increase = ScreenSetup.INSTANCE.getFontSize() < font;
			ScreenSetup.INSTANCE.setFontSize(font);
			if (wasLarge != ScreenSetup.INSTANCE.isFontSizeLarge())
				updateAllClustersAndCompounds(true);
			ComponentFactory.updateComponents();
			fireViewChange(PROPERTY_FONT_SIZE_CHANGED);
			guiControler.showMessage((increase ? "Increase" : "Descrease") + " font size to "
					+ ScreenSetup.INSTANCE.getFontSize() + ".");
		}
	}

	@Override
	public int getFontSize()
	{
		return ScreenSetup.INSTANCE.getFontSize();
	}

	@Override
	public void increaseFontSize(boolean increase)
	{
		setFontSize(ScreenSetup.INSTANCE.getFontSize() + (increase ? 1 : -1));
	}

	@SuppressWarnings("unchecked")
	private void initCompoundDescriptor()
	{
		//		if (compoundDescriptorProperty == COMPOUND_INDEX_PROPERTY)
		//compoundDescriptorProperty = null;
		//		else if (!clustering.getFeatures().contains(compoundDescriptorProperty)
		//				&& !clustering.getProperties().contains(compoundDescriptorProperty))
		/**
		 * !!!!until compound-property rewrite the old integrated property cannot be retained!!!
		 */
		compoundDescriptorProperty = null;
		if (compoundDescriptorProperty == null)
		{
			for (String names : new String[] { "(?i)^name$", "(?i).*name.*", "(?i)^id$", "(?i).*id.*", "(?i)^cas$",
					"(?i).*cas.*", "(?i).*title.*" })
			{
				for (List<CompoundProperty> props : new List[] { clustering.getProperties(), clustering.getFeatures() })
				{
					for (CompoundProperty p : props)
					{
						// cond 1 : prop-name has to match
						// cond 2 : distinct values are > 75% of the dataset size
						// cond 3 : if its the id column, it has to either non-numeric or numeric & integer
						if (p.toString().matches(names)
								&& clustering.numDistinctValues(p) > (clustering.numCompounds() * 3 / 4.0)
								&& (!names.equals("(?i)^id$") || !names.equals("(?i).*id.*")
										|| !(p instanceof NumericProperty) || ((NumericProperty) p).isInteger()))
						{
							compoundDescriptorProperty = p;
							break;
						}
					}
					if (compoundDescriptorProperty != null)
						break;
				}
				if (compoundDescriptorProperty != null)
					break;
			}
		}
		if (compoundDescriptorProperty == null)
			compoundDescriptorProperty = COMPOUND_INDEX_PROPERTY;
		for (Compound m : clustering.getCompounds(true))
			m.setDescriptor(compoundDescriptorProperty);
	}

	@Override
	public void setCompoundDescriptor(CompoundProperty prop)
	{
		if (compoundDescriptorProperty != prop)
		{
			compoundDescriptorProperty = prop;
			for (Compound m : clustering.getCompounds(true))
				m.setDescriptor(compoundDescriptorProperty);
			fireViewChange(PROPERTY_COMPOUND_DESCRIPTOR_CHANGED);
			guiControler.showMessage("Set compound identifier to feature value of '" + prop + "'.");
		}
	}

	@Override
	public CompoundProperty getCompoundDescriptor()
	{
		return compoundDescriptorProperty;
	}

	@Override
	public void setHighlightColors(ColorGradient g, NumericProperty props[])
	{
		boolean fire = false;
		for (NumericProperty p : props)
		{
			if (p == selectedHighlightCompoundProperty && (!g.equals(p.getHighlightColorGradient())))
				fire = true;
			p.setHighlightColorGradient(g);
		}
		if (fire)
		{
			updateAllClustersAndCompounds(true);
			fireViewChange(PROPERTY_HIGHLIGHT_COLORS_CHANGED);
			guiControler.showMessage("Change color gradient for highlighting.");
		}
	}

	@Override
	public void setHighlightColors(Color[] g, NominalProperty[] props)
	{
		boolean fire = false;
		for (NominalProperty p : props)
		{
			if (p == selectedHighlightCompoundProperty
					&& (p.getHighlightColorSequence() == null || !ArrayUtil.equals(g, p.getHighlightColorSequence())))
				fire = true;
			p.setHighlightColorSequence(g);
		}
		if (fire)
		{
			updateAllClustersAndCompounds(true);
			fireViewChange(PROPERTY_HIGHLIGHT_COLORS_CHANGED);
			guiControler.showMessage("Change color sequence for highlighting.");
		}
	}

	@Override
	public void setClusterColors(Color[] sequence)
	{
		if (!ArrayUtil.equals(CompoundPropertyUtil.CLUSTER_COLORS, sequence))
		{
			CompoundPropertyUtil.CLUSTER_COLORS = sequence;
			if (getHighlighter() == Highlighter.CLUSTER_HIGHLIGHTER)
			{
				updateAllClustersAndCompounds(true);
				fireViewChange(PROPERTY_HIGHLIGHT_COLORS_CHANGED);
			}
			guiControler.showMessage("Change color sequence for cluster highlighting.");
		}
	}

	@Override
	public void setHighlightMatchColors(Color[] colors)
	{
		if (!ArrayUtil.equals(CompoundPropertyUtil.HIGHILIGHT_MATCH_COLORS, colors))
		{
			CompoundPropertyUtil.HIGHILIGHT_MATCH_COLORS = colors;
			if (selectedHighlightCompoundProperty instanceof FragmentProperty)
			{
				updateAllClustersAndCompounds(true);
				fireViewChange(PROPERTY_HIGHLIGHT_COLORS_CHANGED);
			}
			guiControler.showMessage("Change colors for SMARTS match highlighting.");
		}
	}

	@Override
	public FeatureFilter getFeatureFilter()
	{
		return featureFilter;
	}

	@Override
	public void setFeatureFilter(FeatureFilter filter)
	{
		if (featureFilter != filter)
		{
			this.featureFilter = filter;
			fireViewChange(PROPERTY_FEATURE_FILTER_CHANGED);
			if (featureFilter == FeatureFilter.None)
				guiControler.showMessage("Show all features in feature list.");
			else if (featureFilter == FeatureFilter.NotSelectedForMapping)
				guiControler.showMessage("Show only features that are NOT used by mapping in feature list.");
			else if (featureFilter == FeatureFilter.SelectedForMapping)
				guiControler.showMessage("Show only features that are used by mapping in feature list.");
			else if (featureFilter == FeatureFilter.Filled)
				guiControler.showMessage("Show only filled endpoint features (ends with '_filled').");
			else if (featureFilter == FeatureFilter.Real)
				guiControler.showMessage("Show only real endpoint features (ends with '_real').");
			else if (featureFilter == FeatureFilter.Endpoints)
				guiControler.showMessage("Show only endpoint features (does not end with '_real', but exists).");
		}
	}

	@Override
	public boolean isFeatureSortingEnabled()
	{
		return featureSortingEnabled;
	}

	@Override
	public void setFeatureSortingEnabled(boolean b)
	{
		if (featureSortingEnabled != b)
		{
			featureSortingEnabled = b;
			fireViewChange(PROPERTY_FEATURE_SORTING_CHANGED);
			guiControler.showMessage((b ? "Enable" : "Disable") + " feature sorting according to specificity.");
		}
	}

	public void showSortFilterDialog()
	{
		SortFilterDialog.showDialog(this, clustering);
	}

	List<Runnable> backgroundJobs = new ArrayList<Runnable>();

	public void runInBackground(final Runnable r)
	{
		if (!guiControler.isVisible())
			throw new IllegalStateException("do not animate if gui not visible!");
		backgroundJobs.add(r);
		guiControler.block("run " + r.hashCode());
		Thread th = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				r.run();
				backgroundJobs.remove(r);
				SwingUtilities.invokeLater(new Runnable()
				{
					@Override
					public void run()
					{
						guiControler.unblock("run " + r.hashCode());
					}
				});
			}
		});
		th.start();
	}

	public void waitForBackground()
	{
		SwingUtil.checkNoAWTEventThread();
		while (backgroundJobs.size() > 0)
			ThreadUtil.sleep(10);
	}

	// cluster controler

	@Override
	public void setClusterActive(final Cluster c, final boolean animate, boolean clearCompoundActive)
	{
		if (c.getNumCompounds() == 0)
			throw new IllegalStateException("cluster size is null, remove filter before");
		if (clearCompoundActive)
		{
			boolean anim = animate;
			if (clustering.getActiveCluster() != null && clustering.getActiveCluster().getNumCompounds() == 1)
				anim = false; // do not animate if there is only one compound in the cluster
			clearCompoundActive(anim);
		}
		if (clustering.getNumClusters() == 1)
			return;
		//		clearClusterWatched();

		resetJittering(new HashSet<Compound>(c.getCompounds()), false);

		if (!animate)
			view.suspendAnimation("set cluster active");
		SwingUtil.invokeAndWait(new Runnable()
		{
			@Override
			public void run()
			{
				clustering.getClusterActive().setSelected(clustering.indexOf(c));
				if (!animate)
					view.proceedAnimation("set cluster active");
			}
		});
		if (animate)
			waitForBackground();
		//			view.waitForAnimation();
	}

	@Override
	public void setClusterWatched(final Cluster c)
	{
		if (clustering.getNumClusters() == 1)
			return;
		SwingUtil.invokeAndWait(new Runnable()
		{
			@Override
			public void run()
			{
				clustering.getClusterWatched().setSelected(clustering.indexOf(c));
			}
		});
	}

	@Override
	public void setCompoundActive(Compound c, boolean animate)
	{
		setCompoundActive(new Compound[] { c }, animate);
	}

	@Override
	public void setCompoundActive(Compound[] c, boolean animate)
	{
		if (getCompoundFilter() != null)
			for (Compound comp : c)
				if (!getCompoundFilter().accept(comp))
					throw new IllegalStateException("compound is filtered out, remove filter before");

		boolean zoomedToCluster = false;
		Cluster clust = clustering.getUniqueClusterForCompounds(c);
		if (clust == null && clustering.isClusterActive())
			clearClusterActive(true, false);
		else if (c.length == 1 && clust != null && clustering.getActiveCluster() != clust)
		{
			setClusterActive(clust, animate, true);
			zoomedToCluster = true;
		}

		final boolean anim;
		if (!animate)
			anim = false;
		else
		{
			if (zoomedToCluster && clustering.getActiveCluster().getNumCompounds() == 1)
				anim = false; // do not animate if there is only one compound in the cluster
			else
				anim = true;
		}
		if (!anim)
			view.suspendAnimation("set compound active");
		final int idx[] = new int[c.length];
		for (int i = 0; i < idx.length; i++)
			idx[i] = c[i].getJmolIndex();
		zoomToSingleSelectedCompound = true;
		SwingUtil.invokeAndWait(new Runnable()
		{
			@Override
			public void run()
			{
				clustering.getCompoundActive().setSelectedIndices(idx);
				if (!anim)
					view.proceedAnimation("set compound active");
			}
		});
		if (anim)
			waitForBackground(); //view.waitForAnimation();
	}

	@Override
	public void toggleCompoundActive(final Compound c)
	{
		toggleCompoundActive(c, true);
	}

	private void toggleCompoundActive(final Compound c, boolean viaCtrlDown)
	{
		if (getCompoundFilter() != null)
			if (!getCompoundFilter().accept(c))
				throw new IllegalStateException("compound is filtered out, remove filter before");
		if (viaCtrlDown)
			ctrlHintCount = 0;

		SwingUtil.invokeAndWait(new Runnable()
		{
			@Override
			public void run()
			{
				//		clearCompoundWatched();
				zoomToSingleSelectedCompound = false;
				clustering.getCompoundActive().setSelectedInverted(c.getJmolIndex());
			}
		});
	}

	@Override
	public void setCompoundWatched(Compound... c)
	{
		final int idx[] = new int[c.length];
		for (int i = 0; i < idx.length; i++)
			idx[i] = c[i].getJmolIndex();
		SwingUtil.invokeAndWait(new Runnable()
		{
			@Override
			public void run()
			{
				clustering.getCompoundWatched().setSelectedIndices(idx);
			}
		});
	}

	@Override
	public void clearClusterActive(final boolean animate, boolean clearCompoundActive)
	{
		if (clearCompoundActive)
		{
			boolean anim = animate;
			if (clustering.getActiveCluster() != null && clustering.getActiveCluster().getNumCompounds() == 1)
				anim = false; // do not animate if there is only one compound in the cluster
			clearCompoundActive(anim);
		}
		if (clustering.getNumClusters() == 1)
			return;
		clearClusterWatched();

		resetJittering(new HashSet<Compound>(clustering.getCompounds(false)), false);

		if (!animate)
			view.suspendAnimation("clear cluster active");
		SwingUtil.invokeAndWait(new Runnable()
		{
			@Override
			public void run()
			{
				clustering.getClusterActive().clearSelection();
				if (!animate)
					view.proceedAnimation("clear cluster active");
			}
		});
		if (animate)
			waitForBackground();// view.waitForAnimation();
	}

	@Override
	public void clearClusterWatched()
	{
		if (clustering.getNumClusters() == 1)
			return;
		SwingUtil.invokeAndWait(new Runnable()
		{
			@Override
			public void run()
			{
				clustering.getClusterWatched().clearSelection();
			}
		});
	}

	@Override
	public void clearCompoundActive(final boolean animate)
	{
		clearCompoundWatched();
		if (!animate)
			view.suspendAnimation("clear compound active");
		SwingUtil.invokeAndWait(new Runnable()
		{
			@Override
			public void run()
			{
				clustering.getCompoundActive().clearSelection();
				if (!animate)
					view.proceedAnimation("clear compound active");
			}
		});
		if (animate)
			waitForBackground();// view.waitForAnimation();
	}

	@Override
	public void clearCompoundWatched()
	{
		SwingUtil.invokeAndWait(new Runnable()
		{
			@Override
			public void run()
			{
				clustering.getCompoundWatched().clearSelection();
			}
		});
	}

	@Override
	public CompoundFilter getCompoundFilter()
	{
		return compoundFilter;
	}

	@Override
	public void applyCompoundFilter(final List<Compound> compounds, final boolean accept)
	{
		if (compounds.size() == clustering.numCompounds())
		{
			JOptionPane.showMessageDialog(Settings.TOP_LEVEL_FRAME, "Cannot hide all compounds of the dataset.");
			return;
		}
		Thread th = new Thread(new Runnable()
		{
			public void run()
			{
				List<Compound> c;
				if (accept)
					c = compounds;
				else
				{
					c = new ArrayList<Compound>();
					for (Compound compound : clustering.getCompounds(true))
						if (!compounds.contains(compound))
							c.add(compound);
				}
				CompoundFilter compoundFilter = new CompoundFilterImpl(clustering, c, null);
				setCompoundFilter(compoundFilter, true);
			}
		});
		th.start();
	}

	@Override
	public void setCompoundFilter(final CompoundFilter filter, final boolean animate)
	{
		if (filter == null && compoundFilter == null || filter == compoundFilter)
			return;
		clearClusterActive(animate, true);
		if (!animate)
			view.suspendAnimation("change compound filter");

		final CompoundProperty p = selectedHighlightCompoundProperty;
		if (selectedHighlightCompoundProperty != null) // otherwise spheres will screw up
			selectedHighlightCompoundProperty = null;

		final CompoundFilter f;
		if (compoundFilter != null && filter != null)
			f = CompoundFilterImpl.combine(clustering, filter, compoundFilter);
		else
			f = filter;
		SwingUtil.invokeAndWait(new Runnable()
		{
			@Override
			public void run()
			{
				guiControler.block("update filter");
				compoundFilter = f;
				clustering.setCompoundFilter(null);
				updateAllClustersAndCompounds(f == null); // force to get cluster-colors right
			}
		});
		if (filter == null) // has to be done outside of awt thread
			resetJittering(new HashSet<Compound>(clustering.getCompounds(false)), true);
		SwingUtil.invokeAndWait(new Runnable()
		{
			@Override
			public void run()
			{
				clustering.updatePositions(); // update cluster position stuff after compounds are visible again
				if (f != null)
				{
					clustering.setCompoundFilter(f);
					updateAllClustersAndCompounds(true);
					clustering.updatePositions(); // update cluster position stuff after compounds are visible again
				}
				guiControler.showMessage((f != null ? "Enable" : "Disable") + " compound filter.");

				Runnable r = new Runnable()
				{
					@Override
					public void run()
					{
						if (clustering.isClusterActive())
							view.zoomTo(clustering.getCluster(clustering.getClusterActive().getSelected()),
									AnimationSpeed.SLOW);
						else
							view.zoomTo(clustering, AnimationSpeed.SLOW);
						SwingUtil.invokeAndWait(new Runnable()
						{
							@Override
							public void run()
							{
								if (p != null)
								{
									selectedHighlightCompoundProperty = p;
									updateAllClustersAndCompounds(false);
								}
								fireViewChange(PROPERTY_COMPOUND_FILTER_CHANGED);
							}
						});
					}
				};
				if (animate)
					runInBackground(r);
				else
				{
					SwingUtil.invokeAndWait(r);
					view.proceedAnimation("change compound filter");
				}
				guiControler.unblock("update filter");
			}
		});
		if (animate)
			waitForBackground();
	}

	@Override
	public void useSelectedCompoundsAsFilter(boolean animate)
	{
		List<Compound> c = new ArrayList<Compound>();
		for (int i : clustering.getCompoundActive().getSelectedIndices())
			c.add(clustering.getCompoundWithJmolIndex(i));
		CompoundFilter compoundFilter = new CompoundFilterImpl(clustering, c, null);
		setCompoundFilter(compoundFilter, animate);
	}

	private CompoundFilter compoundFilter = null;
	private boolean zoomToSingleSelectedCompound = true;

	// ------------------------------------------

	@Override
	public void newClustering()
	{
		guiControler.block("new clustering");

		Thread noAWTThread = new Thread(new Runnable()
		{
			public void run()
			{
				try
				{
					JFrame top = Settings.TOP_LEVEL_FRAME;
					CheSMapperWizard wwd = null;
					while (wwd == null || wwd.getReturnValue() == CheSMapperWizard.RETURN_VALUE_IMPORT)
					{
						wwd = new CheSMapperWizard(top, 0);
						wwd.setCloseButtonText("Cancel");
						Settings.TOP_LEVEL_FRAME = top;
						SwingUtil.waitWhileVisible(wwd);
					}
					if (wwd.getReturnValue() == CheSMapperWizard.RETURN_VALUE_FINISH)
					{
						guiControler.blockMessages();
						clearClusterActive(true, true);
						SwingUtil.invokeAndWait(new Runnable()
						{
							@Override
							public void run()
							{
								clustering.clear();
								compoundFilter = null;
							}
						});

						final Task task = TaskProvider.initTask("Chemical space mapping of "
								+ wwd.getChesMapping().getDatasetFile().getName());
						final TaskDialog taskDialog = new TaskDialog(task, Settings.TOP_LEVEL_FRAME);
						final ClusteringData d = wwd.getChesMapping().doMapping();
						SwingUtil.invokeAndWait(new Runnable()
						{
							@Override
							public void run()
							{
								if (d != null)
								{
									d.setCheSMappingWarningOwner(taskDialog);
									guiControler.unblockMessages();
									clustering.newClustering(d);
									clustering.initFeatureNormalization();
									task.finish();
								}
								TaskProvider.removeTask();
							}
						});
					}
				}
				finally
				{
					SwingUtil.invokeAndWait(new Runnable()
					{
						@Override
						public void run()
						{
							guiControler.unblockMessages();
							guiControler.unblock("new clustering");
						}
					});
				}
			}
		});
		noAWTThread.start();

	}

	@Override
	public void chooseClustersToFilter()
	{
		int[] indices = clustering.clusterChooser("Hide Cluster/s",
				"Select the clusters you want to temporarily remove (the original dataset is not modified).");
		if (indices == null)
			return;
		List<Compound> l = new ArrayList<Compound>();
		for (int i = 0; i < indices.length; i++)
			for (Compound compound : clustering.getCluster(indices[i]).getCompounds())
				l.add(compound);
		applyCompoundFilter(l, false);
	}

	@Override
	public void chooseCompoundsToFilter()
	{
		int[] indices = clustering.selectJmolIndicesWithCompoundChooser("Hide Compounds/s",
				"Select the compounds you want to temporarily remove (the original dataset is not modified).");
		if (indices == null)
			return;
		List<Compound> l = new ArrayList<Compound>();
		for (int i = 0; i < indices.length; i++)
			l.add(clustering.getCompoundWithJmolIndex(indices[i]));
		applyCompoundFilter(l, false);
	}

	@Override
	public void chooseClustersToRemove()
	{
		int[] indices = clustering.clusterChooser("Remove Cluster/s",
				"Select the clusters you want to remove (the original dataset is not modified).");
		if (indices != null)
		{
			if (indices.length == clustering.numClusters())
			{
				JOptionPane.showMessageDialog(Settings.TOP_LEVEL_FRAME, "Cannot remove all clusters from the dataset.");
				return;
			}
			Cluster c2[] = new Cluster[indices.length];
			for (int i = 0; i < indices.length; i++)
				c2[i] = clustering.getCluster(indices[i]);
			clearClusterActive(false, false);
			clustering.removeCluster(c2);
		}
	}

	@Override
	public void chooseCompoundsToRemove()
	{
		int[] indices = clustering.selectJmolIndicesWithCompoundChooser("Remove Compounds/s",
				"Select the compounds you want to remove (the original dataset is not modified).");
		if (indices == null)
			return;
		if (indices.length == clustering.numCompounds())
		{
			JOptionPane.showMessageDialog(Settings.TOP_LEVEL_FRAME, "Cannot remove all compounds from the dataset.");
			return;
		}
		clearClusterActive(false, false);
		clustering.removeCompoundsWithJmolIndices(indices);
	}

	@Override
	public void removeCluster(final Cluster... c)
	{
		if (c.length == clustering.numClusters())
		{
			JOptionPane.showMessageDialog(Settings.TOP_LEVEL_FRAME, "Cannot remove all clusters from the dataset.");
			return;
		}
		clearClusterActive(true, true);
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				clustering.removeCluster(c);
			}
		});
	}

	@Override
	public void removeCompounds(final Compound[] c)
	{
		if (c.length == clustering.numCompounds())
		{
			JOptionPane.showMessageDialog(Settings.TOP_LEVEL_FRAME, "Cannot remove all compounds from the dataset.");
			return;
		}
		clearClusterActive(true, true);
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				clustering.removeCompoundsWithJmolIndices(clustering.getJmolIndicesWithCompounds(c));
			}
		});
	}

	@Override
	public boolean isShowClusteringPropsEnabled()
	{
		return true;
	}

	@Override
	public void resetView()
	{
		guiControler.block("zooming home");
		Thread th = new Thread(new Runnable()
		{
			public void run()
			{
				if (clustering.getNumClusters() > 1 && clustering.getActiveCluster() != null)
					clearClusterActive(true, true);
				else if (clustering.getActiveCompound() != null)
					clearCompoundActive(true);
				else
					view.zoomTo(clustering, AnimationSpeed.SLOW);

				SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						if (clustering.getNumClusters() > 1)
						{
							setHighlighter(Highlighter.CLUSTER_HIGHLIGHTER);
							highlightAutomatic.init();
						}
						else
							setHighlighter(Highlighter.DEFAULT_HIGHLIGHTER);
						//				else
						//					guiControler.setFullScreen(!guiControler.isFullScreen());
						guiControler.unblock("zooming home");
					}
				});
			}
		});
		th.start();
	}
}
