package org.chesmapper.view.gui;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.chesmapper.map.dataInterface.CompoundProperty;
import org.chesmapper.map.dataInterface.NumericProperty;
import org.chesmapper.map.dataInterface.CompoundPropertyUtil.NominalColoring;
import org.chesmapper.map.main.Settings;
import org.chesmapper.map.workflow.MappingWorkflow;
import org.chesmapper.view.cluster.Cluster;
import org.chesmapper.view.cluster.ClusterController;
import org.chesmapper.view.cluster.Clustering;
import org.chesmapper.view.cluster.Compound;
import org.chesmapper.view.cluster.ExportData;
import org.chesmapper.view.cluster.Clustering.SelectionListener;
import org.chesmapper.view.gui.ViewControler.FeatureFilter;
import org.chesmapper.view.gui.ViewControler.HighlightMode;
import org.chesmapper.view.gui.table.CCDataTable;
import org.chesmapper.view.gui.table.FeatureTable;
import org.chesmapper.view.gui.table.TreeView;
import org.chesmapper.view.gui.util.HideUnselectedDialog;
import org.chesmapper.view.gui.util.SALIDialog;
import org.mg.javalib.gui.TextPanel;
import org.mg.javalib.gui.property.IntegerProperty;
import org.mg.javalib.gui.property.Property;
import org.mg.javalib.gui.property.PropertyPanel;
import org.mg.javalib.util.ArrayUtil;
import org.mg.javalib.util.CollectionUtil;
import org.mg.javalib.util.SwingUtil;

public class Actions
{
	public static final String TOOLTIP = "tooltip";

	private static Actions instance;

	public static Actions instance()
	{
		return instance;
	}

	public Actions(GUIControler guiControler, ViewControler viewControler, ClusterController clusterControler,
			Clustering clustering)
	{
		this.guiControler = guiControler;
		this.viewControler = viewControler;
		this.clustering = clustering;
		this.clusterControler = clusterControler;

		buildActions();
		setAccelerators();
		update();
		installListeners();

		instance = this;
	}

	private GUIControler guiControler;
	private ViewControler viewControler;
	private Clustering clustering;
	private ClusterController clusterControler;

	private final static String DATA_CLUSTER_TABLE = "file-cluster-table";
	private final static String DATA_COMPOUND_TABLE = "file-compound-table";
	private final static String DATA_FEATURE_TABLE = "file-feature-table";
	private final static String[] DATA_ACTIONS = { DATA_CLUSTER_TABLE, DATA_COMPOUND_TABLE, DATA_FEATURE_TABLE };

	private final static String FILE_NEW = "file-new";
	private final static String FILE_EXIT = "file-exit";
	private final static String[] FILE_ACTIONS = { FILE_NEW, FILE_EXIT };

	private final static String FILTER_SELECTED = "filter-selected";
	private final static String FILTER_UNSELECTED = "filter-unselected";
	private final static String FILTER_CLUSTERS = "filter-clusters";
	private final static String FILTER_COMPOUNDS = "filter-compounds";
	private final static String[] FILTER_ACTIONS = { FILTER_SELECTED, FILTER_UNSELECTED, FILTER_CLUSTERS,
			FILTER_COMPOUNDS };

	private final static String REMOVE_SELECTED = "remove-selected";
	private final static String REMOVE_UNSELECTED = "remove-unselected";
	private final static String REMOVE_CLUSTERS = "remove-clusters";
	private final static String REMOVE_COMPOUNDS = "remove-compounds";
	private final static String[] REMOVE_ACTIONS = { REMOVE_SELECTED, REMOVE_UNSELECTED, REMOVE_CLUSTERS,
			REMOVE_COMPOUNDS };

	private final static String EDIT_SHOW_DISTANCE = "edit-show-distance";
	private final static String EDIT_SHOW_SALI = "edit-show-sali";
	private final static String EDIT_SUPERIMPOSE = "edit-superimpose";
	private final static String EDIT_SELECT_LAST_FEATURE = "edit-select-last-feature";
	private final static String EDIT_LOG_TRANSFORM = "edit-log-transform";
	private final static String[] EDIT_ACTIONS = { EDIT_SUPERIMPOSE, EDIT_SHOW_DISTANCE, EDIT_SHOW_SALI,
			EDIT_SELECT_LAST_FEATURE, EDIT_LOG_TRANSFORM };

	private final static String EXPORT_SELECTED = "export-selected";
	private final static String EXPORT_UNSELECTED = "export-unselected";
	private final static String EXPORT_CLUSTERS = "export-clusters";
	private final static String EXPORT_COMPOUNDS = "export-compounds";
	private final static String EXPORT_IMAGE = "export-image";
	private final static String EXPORT_WORKFLOW = "export-workflow";
	private final static String[] EXPORT_ACTIONS = { EXPORT_SELECTED, EXPORT_UNSELECTED, EXPORT_CLUSTERS,
			EXPORT_COMPOUNDS, EXPORT_IMAGE, EXPORT_WORKFLOW };

	private final static String VIEW_HOME = "view-home";
	private final static String VIEW_FULL_SCREEN = "view-full-screen";
	private final static String VIEW_DRAW_HYDROGENS = "view-draw-hydrogens";
	private final static String VIEW_SPIN = "view-spin";
	private final static String VIEW_BLACK_WHITE = "view-black-white";
	private final static String VIEW_ANTIALIAS = "view-antialias";
	private final static String VIEW_COMPOUND_DESCRIPTOR = "view-compound-descriptor";
	private final static String VIEW_HIDE_UNSELECTED_DIALOG = "view-hide-unselected";
	private final static String VIEW_OPEN_SORT_FILTER_DIALOG = "view-open-sort-filter-dialog";
	private final static String VIEW_SIZE_DIALOG = "view-size-dialog";
	private final static String[] VIEW_ACTIONS = { VIEW_HOME, VIEW_FULL_SCREEN, VIEW_DRAW_HYDROGENS, VIEW_SPIN,
			VIEW_BLACK_WHITE, VIEW_ANTIALIAS, VIEW_COMPOUND_DESCRIPTOR, VIEW_OPEN_SORT_FILTER_DIALOG,
			VIEW_HIDE_UNSELECTED_DIALOG, VIEW_SIZE_DIALOG };

	private final static String HIGHLIGHT_COLORS = "highlight-colors";
	private final static String HIGHLIGHT_MODE = "highlight-mode";
	private final static String HIGHLIGHT_LABELS = "highlight-labels";
	private final static String HIGHLIGHT_LAST_FEATURE = "highlight-last-feature";
	private final static String HIGHLIGHT_DECR_SPHERE_SIZE = "highlight-decr-sphere-size";
	private final static String HIGHLIGHT_INCR_SPHERE_SIZE = "highlight-incr-sphere-size";
	private final static String HIGHLIGHT_DECR_SPHERE_TRANSLUCENCY = "highlight-decr-sphere-translucency";
	private final static String HIGHLIGHT_INCR_SPHERE_TRANSLUCENCY = "highlight-incr-sphere-translucency";
	private final static String[] HIGHLIGHT_ACTIONS = { HIGHLIGHT_COLORS, HIGHLIGHT_LABELS, HIGHLIGHT_MODE,
			HIGHLIGHT_LAST_FEATURE };
	private final static String[] HIGHLIGHT_SPHERE_ACTIONS = { HIGHLIGHT_DECR_SPHERE_SIZE, HIGHLIGHT_INCR_SPHERE_SIZE,
			HIGHLIGHT_DECR_SPHERE_TRANSLUCENCY, HIGHLIGHT_INCR_SPHERE_TRANSLUCENCY };

	private final static String HELP_DOCU = "help-docu";
	private final static String HELP_WARNINGS = "help-warnings";
	private final static String HELP_ABOUT = "help-about";
	private final static String[] HELP_ACTIONS = { HELP_DOCU, HELP_WARNINGS, HELP_ABOUT };

	private final static String HIDDEN_UPDATE_MOUSE_SELECTION_PRESSED = "hidden-update-mouse-selection-pressed";
	private final static String HIDDEN_UPDATE_MOUSE_SELECTION_RELEASED = "hidden-update-mouse-selection-released";
	private final static String HIDDEN_DECR_COMPOUND_SIZE = "hidden-decr-compound-size";
	private final static String HIDDEN_INCR_COMPOUND_SIZE = "hidden-incr-compound-size";
	private final static String HIDDEN_ENABLE_JMOL_POPUP = "enable-jmol-popup";
	private final static String HIDDEN_INCR_SPIN_SPEED = "incr-spin-speed";
	private final static String HIDDEN_DECR_SPIN_SPEED = "decr-spin-speed";
	private final static String HIDDEN_INCR_FONT_SIZE = "incr-font-size";
	private final static String HIDDEN_DECR_FONT_SIZE = "decr-font-size";
	private final static String HIDDEN_FILTER_FEATURES = "filter-features";
	private final static String HIDDEN_TOGGLE_SORTING = "toggle-sorting";
	private final static String HIDDEN_TREE = "tree";
	private final static String HIDDEN_COPY = "copy";
	private final static String HIDDEN_PREDICT = "predict";
	private final static String HIDDEN_APP_DOMAIN = "app-domain";
	private final static String HIDDEN_TOGGLE_NOM_HIGHLIGHT = "toggle-nom-highlight";
	private final static String[] HIDDEN_ACTIONS = { HIDDEN_UPDATE_MOUSE_SELECTION_PRESSED,
			HIDDEN_UPDATE_MOUSE_SELECTION_RELEASED, HIDDEN_DECR_COMPOUND_SIZE, HIDDEN_INCR_COMPOUND_SIZE,
			HIDDEN_ENABLE_JMOL_POPUP, HIDDEN_INCR_SPIN_SPEED, HIDDEN_DECR_SPIN_SPEED, HIDDEN_INCR_FONT_SIZE,
			HIDDEN_DECR_FONT_SIZE, HIDDEN_FILTER_FEATURES, HIDDEN_TOGGLE_SORTING, HIDDEN_TREE, HIDDEN_COPY,
			HIDDEN_PREDICT, HIDDEN_APP_DOMAIN, HIDDEN_TOGGLE_NOM_HIGHLIGHT };

	private HashMap<String, Action> actions = new LinkedHashMap<String, Action>();

	private static HashMap<String, KeyStroke> keys = new HashMap<String, KeyStroke>();

	static
	{
		keys.put(DATA_CLUSTER_TABLE, KeyStroke.getKeyStroke(KeyEvent.VK_1, ActionEvent.ALT_MASK));
		keys.put(DATA_COMPOUND_TABLE, KeyStroke.getKeyStroke(KeyEvent.VK_2, ActionEvent.ALT_MASK));
		keys.put(DATA_FEATURE_TABLE, KeyStroke.getKeyStroke(KeyEvent.VK_3, ActionEvent.ALT_MASK));
		keys.put(FILE_NEW, KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.ALT_MASK));
		keys.put(FILE_EXIT, KeyStroke.getKeyStroke(KeyEvent.VK_F4, ActionEvent.ALT_MASK));

		keys.put(EDIT_SHOW_DISTANCE, KeyStroke.getKeyStroke(KeyEvent.VK_D, ActionEvent.ALT_MASK));
		keys.put(EDIT_SUPERIMPOSE, KeyStroke.getKeyStroke(KeyEvent.VK_U, ActionEvent.ALT_MASK));
		keys.put(EDIT_SELECT_LAST_FEATURE, KeyStroke.getKeyStroke(KeyEvent.VK_A, ActionEvent.ALT_MASK));
		keys.put(EDIT_LOG_TRANSFORM, KeyStroke.getKeyStroke(KeyEvent.VK_L, ActionEvent.ALT_MASK));

		keys.put(FILTER_SELECTED, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, ActionEvent.ALT_MASK));
		keys.put(FILTER_UNSELECTED, KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, ActionEvent.ALT_MASK));

		keys.put(EXPORT_IMAGE, KeyStroke.getKeyStroke(KeyEvent.VK_I, ActionEvent.ALT_MASK));
		keys.put(EXPORT_WORKFLOW, KeyStroke.getKeyStroke(KeyEvent.VK_W, ActionEvent.ALT_MASK));

		keys.put(VIEW_HOME, KeyStroke.getKeyStroke("HOME"));
		keys.put(VIEW_DRAW_HYDROGENS, KeyStroke.getKeyStroke(KeyEvent.VK_H, ActionEvent.ALT_MASK));
		//		keys.put(VIEW_HIDE_UNSELECTED, KeyStroke.getKeyStroke(KeyEvent.VK_V, ActionEvent.ALT_MASK));
		keys.put(VIEW_SPIN, KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.ALT_MASK));
		keys.put(VIEW_BLACK_WHITE, KeyStroke.getKeyStroke(KeyEvent.VK_B, ActionEvent.ALT_MASK));
		//		keys.put(VIEW_COMPOUND_DESCRIPTOR, KeyStroke.getKeyStroke(KeyEvent.VK_D, ActionEvent.ALT_MASK));
		keys.put(HIDDEN_ENABLE_JMOL_POPUP, KeyStroke.getKeyStroke(KeyEvent.VK_R, ActionEvent.ALT_MASK));
		keys.put(HIDDEN_UPDATE_MOUSE_SELECTION_PRESSED,
				KeyStroke.getKeyStroke(KeyEvent.VK_SHIFT, ActionEvent.SHIFT_MASK));
		keys.put(HIDDEN_UPDATE_MOUSE_SELECTION_RELEASED, KeyStroke.getKeyStroke(KeyEvent.VK_SHIFT, 0, true));
		keys.put(VIEW_ANTIALIAS, KeyStroke.getKeyStroke(KeyEvent.VK_G, ActionEvent.ALT_MASK));
		keys.put(HIGHLIGHT_COLORS, KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.ALT_MASK));
		keys.put(HIGHLIGHT_LAST_FEATURE, KeyStroke.getKeyStroke(KeyEvent.VK_F, ActionEvent.ALT_MASK));
		keys.put(HIGHLIGHT_MODE, KeyStroke.getKeyStroke(KeyEvent.VK_T, ActionEvent.ALT_MASK));
		keys.put(HIDDEN_DECR_COMPOUND_SIZE, KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, ActionEvent.CTRL_MASK));
		keys.put(HIDDEN_INCR_COMPOUND_SIZE, KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, ActionEvent.CTRL_MASK));
		keys.put(HIGHLIGHT_DECR_SPHERE_SIZE,
				KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, ActionEvent.CTRL_MASK | ActionEvent.SHIFT_MASK));
		keys.put(HIGHLIGHT_INCR_SPHERE_SIZE,
				KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, ActionEvent.CTRL_MASK | ActionEvent.SHIFT_MASK));
		keys.put(HIGHLIGHT_INCR_SPHERE_TRANSLUCENCY,
				KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, ActionEvent.CTRL_MASK | ActionEvent.ALT_MASK));
		keys.put(HIGHLIGHT_DECR_SPHERE_TRANSLUCENCY,
				KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, ActionEvent.CTRL_MASK | ActionEvent.ALT_MASK));
		keys.put(HIDDEN_INCR_SPIN_SPEED, KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, ActionEvent.CTRL_MASK));// | ActionEvent.ALT_MASK));
		keys.put(HIDDEN_DECR_SPIN_SPEED, KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, ActionEvent.CTRL_MASK));// | ActionEvent.ALT_MASK));
		keys.put(HIDDEN_INCR_FONT_SIZE, KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, ActionEvent.ALT_MASK));// | ActionEvent.ALT_MASK));
		keys.put(HIDDEN_DECR_FONT_SIZE, KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, ActionEvent.ALT_MASK));// | ActionEvent.ALT_MASK));
		keys.put(HIDDEN_FILTER_FEATURES, KeyStroke.getKeyStroke(KeyEvent.VK_M, ActionEvent.ALT_MASK));
		keys.put(HIDDEN_TOGGLE_SORTING, KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.ALT_MASK));
		keys.put(HIDDEN_TOGGLE_NOM_HIGHLIGHT, KeyStroke.getKeyStroke(KeyEvent.VK_E, ActionEvent.ALT_MASK));
		keys.put(HIDDEN_TREE, KeyStroke.getKeyStroke(KeyEvent.VK_4, ActionEvent.ALT_MASK));
		keys.put(HIDDEN_COPY, KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK));
		keys.put(HIDDEN_PREDICT, KeyStroke.getKeyStroke(KeyEvent.VK_P, ActionEvent.ALT_MASK));
		keys.put(HIDDEN_APP_DOMAIN, KeyStroke.getKeyStroke(KeyEvent.VK_Z, ActionEvent.ALT_MASK));
	}

	private void updateFullScreenKey()
	{
		if (guiControler.isFullScreen())
			keys.put(VIEW_FULL_SCREEN, KeyStroke.getKeyStroke("ESCAPE"));
		else
			keys.put(VIEW_FULL_SCREEN, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, ActionEvent.ALT_MASK));
	}

	public static String keyStrokeToText(KeyStroke keystroke)
	{
		return KeyEvent.getKeyModifiersText(keystroke.getModifiers()) + " + "
				+ new String(Character.toChars(keystroke.getKeyCode()));
	}

	public static String getIncrDecrFontSizeKeys()
	{
		if (keys.get(HIDDEN_INCR_FONT_SIZE).getKeyCode() == KeyEvent.VK_PLUS
				&& keys.get(HIDDEN_DECR_FONT_SIZE).getKeyCode() == KeyEvent.VK_MINUS
				&& keys.get(HIDDEN_INCR_FONT_SIZE).getModifiers() == keys.get(HIDDEN_DECR_FONT_SIZE).getModifiers())
		{
			return KeyEvent.getKeyModifiersText(keys.get(HIDDEN_INCR_FONT_SIZE).getModifiers()) + " +/-";
		}
		else
			throw new IllegalStateException();
	}

	public static String getToggleSortingKey()
	{
		return keyStrokeToText(keys.get(HIDDEN_TOGGLE_SORTING));
	}

	public static String getFilterFeaturesKey()
	{
		return keyStrokeToText(keys.get(HIDDEN_FILTER_FEATURES));
	}

	private void installListeners()
	{
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
		clustering.addListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				update();
			}
		});
	}

	private void update()
	{
		Compound selectedCompounds[] = new Compound[0];
		Compound selectedCompound = null;
		if (clustering.isCompoundActive())
		{
			selectedCompounds = clustering.getActiveCompounds();
			selectedCompound = clustering.getActiveCompound();
		}
		if (clustering.isCompoundWatched())
		{
			selectedCompounds = ArrayUtil.removeDuplicates(Compound.class,
					ArrayUtil.concat(Compound.class, selectedCompounds, clustering.getWatchedCompounds()));
			selectedCompound = clustering.getWatchedCompound();
		}

		Cluster selectedClusters[] = new Cluster[0];
		if (clustering.numClusters() > 1 && clustering.isClusterWatched())
			selectedClusters = new Cluster[] { clustering.getWatchedCluster() };
		else if (clustering.numClusters() > 1 && clustering.isClusterActive())
			selectedClusters = new Cluster[] { clustering.getActiveCluster() };

		Cluster unselectedClusters[] = new Cluster[0];
		if (selectedClusters.length > 0)
		{
			List<Cluster> unselectedClustersList = new ArrayList<Cluster>(clustering.getClusters());
			for (Cluster cluster : selectedClusters)
				unselectedClustersList.remove(cluster);
			unselectedClusters = ArrayUtil.toArray(Cluster.class, unselectedClustersList);
		}
		Compound unselectedCompounds[] = new Compound[0];
		if (selectedCompounds.length > 0)
		{
			List<Compound> unselectedCompoundsList = new ArrayList<Compound>(clustering.getCompounds(true));
			for (Compound compound : selectedCompounds)
				unselectedCompoundsList.remove(compound);
			unselectedCompounds = ArrayUtil.toArray(Compound.class, unselectedCompoundsList);
		}

		String actionsX[] = new String[] { FILTER_SELECTED, FILTER_UNSELECTED, REMOVE_SELECTED, REMOVE_UNSELECTED,
				EXPORT_SELECTED, EXPORT_UNSELECTED };
		String actionStrings[] = new String[] { "Hide", "Hide", "Remove", "Remove", "Export", "Export" };
		boolean selectedX[] = new boolean[] { true, false, true, false, true, false };

		for (int i = 0; i < selectedX.length; i++)
		{
			AbstractAction action = (AbstractAction) actions.get(actionsX[i]);
			boolean selected = selectedX[i];
			String actionString = actionStrings[i] + " " + (selected ? "selected" : "unselected");
			Cluster clusters[] = selected ? selectedClusters : unselectedClusters;
			Compound compounds[] = selected ? selectedCompounds : unselectedCompounds;
			action.putValue("Compound", new Compound[0]);
			action.putValue("Cluster", new Cluster[0]);

			if (compounds.length > 0)
			{
				action.putValue("Compound", compounds);
				if (compounds.length == 1)
					action.putValue(Action.NAME, actionString + " " + compounds[0]);
				else if (compounds.length > 1)
					action.putValue(Action.NAME, actionString + " " + compounds.length + " compounds");
				action.setEnabled(true);
			}
			else if (clusters.length > 0)
			{
				action.putValue("Cluster", clusters);
				if (clusters.length == 1)
					action.putValue(Action.NAME, actionString + " " + clusters[0].getName());
				else if (clusters.length > 1)
					action.putValue(Action.NAME, actionString + " " + clusters.length + " clusters");
				action.setEnabled(true);
			}
			else
			{
				action.putValue(Action.NAME, actionString + " cluster/compound");
				action.setEnabled(false);
			}
		}

		if (selectedCompounds.length > 0 && !clustering.isRandomEmbedding())
		{
			actions.get(EDIT_SHOW_DISTANCE).setEnabled(true);
			String cName = selectedCompound.toString();
			if (cName.length() > 22)
				cName = cName.substring(0, 20) + "..";
			actions.get(EDIT_SHOW_DISTANCE).putValue(Action.NAME, "Distance to " + cName);
		}
		else
		{
			actions.get(EDIT_SHOW_DISTANCE).putValue(Action.NAME, "Distance to selected compound");
			actions.get(EDIT_SHOW_DISTANCE).setEnabled(false);
		}
		actions.get(EDIT_SHOW_SALI).setEnabled(!clustering.isRandomEmbedding());
		if (clustering.isClusterActive())
			actions.get(EDIT_SUPERIMPOSE).setEnabled(viewControler.isSingleClusterSpreadable());
		else
			actions.get(EDIT_SUPERIMPOSE).setEnabled(viewControler.isAllClustersSpreadable());
		actions.get(HELP_WARNINGS).setEnabled(clustering.doCheSMappingWarningsExist());
	}

	private void filter(AbstractAction action)
	{
		final Compound comps[] = (Compound[]) action.getValue("Compound");
		final Cluster c[] = (Cluster[]) action.getValue("Cluster");
		List<Compound> compounds = new ArrayList<Compound>();
		if (comps.length > 0)
			compounds = ArrayUtil.toList(comps);
		else
			for (Cluster clust : c)
				for (Compound compound : clust.getCompounds())
					compounds.add(compound);
		clusterControler.applyCompoundFilter(compounds, false);
	}

	private void remove(AbstractAction action)
	{
		final Compound comps[] = (Compound[]) action.getValue("Compound");
		final Cluster c[] = (Cluster[]) action.getValue("Cluster");
		Thread th = new Thread(new Runnable()
		{
			public void run()
			{
				if (comps.length > 0)
					clusterControler.removeCompounds(comps);
				else if (c.length > 0)
					clusterControler.removeCluster(c);
			}
		});
		th.start();
	}

	private void export(AbstractAction action)
	{
		final Compound comps[] = (Compound[]) action.getValue("Compound");
		final Cluster c[] = (Cluster[]) action.getValue("Cluster");
		if (comps.length > 0)
		{
			int[] compoundOrigIndices = new int[comps.length];
			for (int i = 0; i < compoundOrigIndices.length; i++)
				compoundOrigIndices[i] = comps[i].getOrigIndex();
			ExportData.exportCompoundsWithOrigIndices(clustering, compoundOrigIndices,
					viewControler.getCompoundDescriptor());
		}
		else if (c.length > 0)
		{
			int[] clusterIndices = new int[c.length];
			for (int i = 0; i < clusterIndices.length; i++)
				clusterIndices[i] = clustering.indexOf(c[i]);
			ExportData.exportClusters(clustering, clusterIndices, viewControler.getCompoundDescriptor());
		}
	}

	private void setAccelerators()
	{
		updateFullScreenKey();
		for (String action : actions.keySet())
		{
			KeyStroke keyStroke = keys.get(action);
			if (keyStroke != null)
				((AbstractAction) actions.get(action)).putValue(Action.ACCELERATOR_KEY, keyStroke);
		}
	}

	private abstract class ActionCreator
	{
		AbstractAction action;

		public ActionCreator(String s)
		{
			this(s, new String[0], new String[0]);
		}

		public ActionCreator(String s, String valueProperty)
		{
			this(s, valueProperty, null);
		}

		public ActionCreator(String s, String valueProperty, String enabledProperty)
		{
			this(s, valueProperty == null ? new String[0] : new String[] { valueProperty },
					enabledProperty == null ? new String[0] : new String[] { enabledProperty });
		}

		public ActionCreator(String s, final String valueProperty[], final String enabledProperty[])
		{
			String name = null;
			String tooltip = null;
			if (ArrayUtil.indexOf(HIDDEN_ACTIONS, s) == -1)
			{
				name = Settings.text("action." + s);
				tooltip = "<html>" + Settings.text("action." + s + ".tooltip").replace("\n", "<br>") + "</html>";
			}

			action = new AbstractAction(name)
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					action();
				}
			};
			action.putValue(TOOLTIP, tooltip);
			action.putValue("is-radio-buttion", isRadio());
			action.setEnabled(isEnabled());

			if ((valueProperty != null && valueProperty.length > 0)
					|| (enabledProperty != null && enabledProperty.length > 0))
			{
				if (valueProperty != null && valueProperty.length > 0)
					action.putValue(Action.SELECTED_KEY, isSelected());
				PropertyChangeListener l = new PropertyChangeListener()
				{
					@Override
					public void propertyChange(PropertyChangeEvent evt)
					{
						if (ArrayUtil.indexOf(valueProperty, evt.getPropertyName()) != -1)
							action.putValue(Action.SELECTED_KEY, isSelected());
						if (ArrayUtil.indexOf(enabledProperty, evt.getPropertyName()) != -1)
							action.setEnabled(isEnabled());
					}
				};
				guiControler.addPropertyChangeListener(l);
				viewControler.addViewListener(l);
			}

			actions.put(s, action);
		}

		public abstract void action();

		protected boolean isRadio()
		{
			return false;
		}

		public boolean isEnabled()
		{
			return true;
		}

		public Boolean isSelected()
		{
			return null;
		}
	}

	//	private abstract class RadioActionCreator extends ActionCreator
	//	{
	//		public RadioActionCreator(String s, String valueProperty)
	//		{
	//			super(s, valueProperty);
	//		}
	//
	//		public RadioActionCreator(String s, String valueProperty, String enabledProperty)
	//		{
	//			super(s, valueProperty, enabledProperty);
	//		}
	//
	//		protected boolean isRadio()
	//		{
	//			return true;
	//		}
	//	}

	private void buildActions()
	{
		new ActionCreator(FILE_NEW)
		{
			@Override
			public void action()
			{
				newClustering();
			}
		};
		new ActionCreator(DATA_CLUSTER_TABLE)
		{
			@Override
			public void action()
			{
				CCDataTable.show(viewControler, clusterControler, clustering, true);
			}
		};
		new ActionCreator(DATA_COMPOUND_TABLE)
		{
			@Override
			public void action()
			{
				CCDataTable.show(viewControler, clusterControler, clustering, false);
			}
		};
		new ActionCreator(DATA_FEATURE_TABLE)
		{
			@Override
			public void action()
			{
				new FeatureTable(viewControler, clusterControler, clustering);
			}
		};
		new ActionCreator(FILE_EXIT)
		{
			@Override
			public void action()
			{
				JFrame f = null;
				if (viewControler instanceof JPanel)
				{
					java.awt.Container top = ((JPanel) viewControler).getTopLevelAncestor();
					if (top instanceof JFrame)
						f = (JFrame) top;
				}
				LaunchCheSMapper.exit(f);
			}
		};
		new ActionCreator(FILTER_SELECTED)
		{
			@Override
			public void action()
			{
				filter((AbstractAction) actions.get(FILTER_SELECTED));
			}
		};
		new ActionCreator(FILTER_UNSELECTED)
		{
			@Override
			public void action()
			{
				filter((AbstractAction) actions.get(FILTER_UNSELECTED));
			}
		};
		new ActionCreator(FILTER_CLUSTERS)
		{
			@Override
			public void action()
			{
				clusterControler.chooseClustersToFilter();
			}
		};
		new ActionCreator(FILTER_COMPOUNDS)
		{
			@Override
			public void action()
			{
				clusterControler.chooseCompoundsToFilter();
			}
		};
		new ActionCreator(REMOVE_SELECTED)
		{
			@Override
			public void action()
			{
				remove((AbstractAction) actions.get(REMOVE_SELECTED));
			}
		};
		new ActionCreator(REMOVE_UNSELECTED)
		{
			@Override
			public void action()
			{
				remove((AbstractAction) actions.get(REMOVE_UNSELECTED));
			}
		};
		new ActionCreator(REMOVE_CLUSTERS)
		{
			@Override
			public void action()
			{
				clusterControler.chooseClustersToRemove();
			}
		};
		new ActionCreator(REMOVE_COMPOUNDS)
		{
			@Override
			public void action()
			{
				clusterControler.chooseCompoundsToRemove();
			}
		};
		new ActionCreator(EXPORT_SELECTED)
		{
			@Override
			public void action()
			{
				export((AbstractAction) actions.get(EXPORT_SELECTED));
			}
		};
		new ActionCreator(EXPORT_UNSELECTED)
		{
			@Override
			public void action()
			{
				export((AbstractAction) actions.get(EXPORT_UNSELECTED));
			}
		};
		new ActionCreator(EXPORT_CLUSTERS)
		{
			@Override
			public void action()
			{
				clustering.chooseClustersToExport(viewControler.getCompoundDescriptor());
			}
		};
		new ActionCreator(EXPORT_COMPOUNDS)
		{
			@Override
			public void action()
			{
				clustering.chooseCompoundsToExport(viewControler.getCompoundDescriptor());
			}
		};
		new ActionCreator(EXPORT_IMAGE)
		{
			@Override
			public void action()
			{
				View.instance.exportImage();
			}
		};
		new ActionCreator(EXPORT_WORKFLOW)
		{
			@Override
			public void action()
			{
				MappingWorkflow.exportMappingWorkflowToFile(MappingWorkflow.exportSettingsToMappingWorkflow());
			}
		};
		new ActionCreator(VIEW_HOME)
		{
			@Override
			public void action()
			{
				viewControler.resetView();
			}
		};
		new ActionCreator(VIEW_FULL_SCREEN, GUIControler.PROPERTY_FULLSCREEN_CHANGED)
		{
			@Override
			public void action()
			{
				guiControler.setFullScreen(!guiControler.isFullScreen());
				setAccelerators();
			}

			@Override
			public Boolean isSelected()
			{
				return guiControler.isFullScreen();
			}
		};
		new ActionCreator(VIEW_DRAW_HYDROGENS, ViewControler.PROPERTY_SHOW_HYDROGENS)
		{
			@Override
			public void action()
			{
				viewControler.setHideHydrogens(!viewControler.isHideHydrogens());
			}

			@Override
			public Boolean isSelected()
			{
				return !viewControler.isHideHydrogens();
			}
		};
		new ActionCreator(VIEW_HIDE_UNSELECTED_DIALOG)
		{
			@Override
			public void action()
			{
				HideUnselectedDialog.showDialog(viewControler, clustering);
			}
		};
		new ActionCreator(VIEW_OPEN_SORT_FILTER_DIALOG)
		{
			@Override
			public void action()
			{
				viewControler.showSortFilterDialog();
			}
		};
		new ActionCreator(VIEW_SIZE_DIALOG)
		{
			IntegerProperty font;

			@Override
			public void action()
			{
				if (font == null)
				{
					font = new IntegerProperty("Font size (" + getIncrDecrFontSizeKeys() + ")",
							viewControler.getFontSize(), 6, 48);
					viewControler.addViewListener(new PropertyChangeListener()
					{
						@Override
						public void propertyChange(PropertyChangeEvent e)
						{
							if (e.getPropertyName().equals(ViewControler.PROPERTY_FONT_SIZE_CHANGED))
								font.setValue(viewControler.getFontSize());
						}
					});
					font.addPropertyChangeListener(new PropertyChangeListener()
					{
						@Override
						public void propertyChange(PropertyChangeEvent evt)
						{
							viewControler.setFontSize(font.getValue());
						}
					});
				}
				guiControler.setAccentuateSizeComponents(true);
				JPanel p = new JPanel(new BorderLayout(10, 10));
				p.add(new PropertyPanel(new Property[] { font }), BorderLayout.NORTH);
				JPanel pp = new JPanel(new BorderLayout(5, 5));
				pp.add(new JLabel("Maximum width in percent:"), BorderLayout.NORTH);
				pp.add(new PropertyPanel(ComponentSize.MAX_WIDTH));
				p.add(pp);
				SwingUtil.showInDialog(p, String.valueOf(action.getValue(Action.NAME)), null, null,
						Settings.TOP_LEVEL_FRAME);
				guiControler.setAccentuateSizeComponents(false);
			}
		};

		new ActionCreator(VIEW_SPIN, ViewControler.PROPERTY_SPIN_CHANGED)
		{
			@Override
			public void action()
			{
				viewControler.setSpinEnabled(!viewControler.isSpinEnabled());
			}

			@Override
			public Boolean isSelected()
			{
				return viewControler.isSpinEnabled();
			}
		};
		new ActionCreator(VIEW_BLACK_WHITE, ViewControler.PROPERTY_BACKGROUND_CHANGED)
		{
			@Override
			public void action()
			{
				viewControler.setBackgroundBlack(!viewControler.isBlackgroundBlack());
			}

			@Override
			public Boolean isSelected()
			{
				return viewControler.isBlackgroundBlack();
			}
		};
		new ActionCreator(VIEW_ANTIALIAS, ViewControler.PROPERTY_ANTIALIAS_CHANGED)
		{
			@Override
			public void action()
			{
				viewControler.setAntialiasEnabled(!viewControler.isAntialiasEnabled());
			}

			@Override
			public Boolean isSelected()
			{
				return viewControler.isAntialiasEnabled();
			}
		};
		new ActionCreator(VIEW_COMPOUND_DESCRIPTOR)
		{
			@Override
			public void action()
			{
				List<CompoundProperty> props = new ArrayList<CompoundProperty>();
				props.add(ViewControler.COMPOUND_INDEX_PROPERTY);
				props.add(ViewControler.COMPOUND_SMILES_PROPERTY);
				for (CompoundProperty compoundProperty : clustering.getProperties())
					props.add(compoundProperty);
				for (CompoundProperty compoundProperty : clustering.getFeatures())
					if (!props.contains(compoundProperty))
						props.add(compoundProperty);
				CompoundProperty selected = viewControler.getCompoundDescriptor();
				CompoundProperty p = SwingUtil.selectFromListWithDialog(props, selected, "Set compound identifier",
						Settings.TOP_LEVEL_FRAME);
				if (p != null)
					viewControler.setCompoundDescriptor(p);
			}
		};
		new ActionCreator(HELP_DOCU)
		{
			@Override
			public void action()
			{
				try
				{
					Desktop.getDesktop().browse(new URI(Settings.HOMEPAGE_DOCUMENTATION));
				}
				catch (Exception ex)
				{
					Settings.LOGGER.error(ex);
				}
			}
		};
		new ActionCreator(HELP_WARNINGS)
		{
			@Override
			public void action()
			{
				if (clustering.doCheSMappingWarningsExist())
					clustering.showCheSMappingWarnings();
			}
		};
		new ActionCreator(HELP_ABOUT)
		{
			@Override
			public void action()
			{
				showAboutDialog();
			}
		};
		new ActionCreator(HIDDEN_ENABLE_JMOL_POPUP)
		{
			@Override
			public void action()
			{
				View.instance.scriptWait("set disablePopupMenu off");
			}
		};
		new ActionCreator(HIDDEN_UPDATE_MOUSE_SELECTION_PRESSED)
		{
			@Override
			public void action()
			{
				viewControler.updateMouseSelection(true);
			}
		};
		new ActionCreator(HIDDEN_UPDATE_MOUSE_SELECTION_RELEASED)
		{
			@Override
			public void action()
			{
				viewControler.updateMouseSelection(false);
			}
		};
		new ActionCreator(HIDDEN_DECR_COMPOUND_SIZE)
		{
			@Override
			public void action()
			{
				if (viewControler.canChangeCompoundSize(false))
					viewControler.changeCompoundSize(false);
			}
		};
		new ActionCreator(HIDDEN_INCR_COMPOUND_SIZE)
		{
			@Override
			public void action()
			{
				if (viewControler.canChangeCompoundSize(true))
					viewControler.changeCompoundSize(true);
			}
		};
		new ActionCreator(HIGHLIGHT_COLORS)
		{
			@Override
			public void action()
			{
				ColorEditor.start(viewControler, clustering);
			}
		};

		new ActionCreator(HIGHLIGHT_LABELS, ViewControler.PROPERTY_HIGHLIGHT_CHANGED)
		{
			@Override
			public void action()
			{
				viewControler.setHighlighterLabelsVisible(!viewControler.isHighlighterLabelsVisible());
			}

			@Override
			public Boolean isSelected()
			{
				return viewControler.isHighlighterLabelsVisible();
			}
		};

		new ActionCreator(HIGHLIGHT_LAST_FEATURE, ViewControler.PROPERTY_HIGHLIGHT_LAST_FEATURE,
				ViewControler.PROPERTY_HIGHLIGHT_MODE_CHANGED)
		{
			@Override
			public void action()
			{
				viewControler.setHighlightLastFeatureEnabled(!viewControler.isHighlightLastFeatureEnabled());
			}

			@Override
			public Boolean isSelected()
			{
				return viewControler.isHighlightLastFeatureEnabled();
			}

			@Override
			public boolean isEnabled()
			{
				return viewControler.getHighlightMode() == HighlightMode.Spheres;
			}
		};

		new ActionCreator(EDIT_SUPERIMPOSE, ViewControler.PROPERTY_SUPERIMPOSE_CHANGED)
		{
			@Override
			public void action()
			{
				viewControler.setSuperimpose(!viewControler.isSuperimpose());
			}

			@Override
			public Boolean isSelected()
			{
				return viewControler.isSuperimpose();
			}

		};
		new ActionCreator(EDIT_SELECT_LAST_FEATURE)
		{
			@Override
			public void action()
			{
				viewControler.setSelectLastSelectedHighlighter();
			}
		};
		new ActionCreator(HIGHLIGHT_MODE, ViewControler.PROPERTY_HIGHLIGHT_MODE_CHANGED,
				ViewControler.PROPERTY_STYLE_CHANGED)
		{
			@Override
			public void action()
			{
				viewControler
						.setHighlightMode((viewControler.getHighlightMode() != HighlightMode.Spheres) ? MainPanel.HighlightMode.Spheres
								: MainPanel.HighlightMode.ColorCompounds);
			}

			@Override
			public Boolean isSelected()
			{
				return viewControler.getHighlightMode() == MainPanel.HighlightMode.Spheres;
			}

			@Override
			public boolean isEnabled()
			{
				return viewControler.getStyle() != ViewControler.Style.dots;
			}
		};
		new ActionCreator(HIGHLIGHT_DECR_SPHERE_SIZE, null, ViewControler.PROPERTY_HIGHLIGHT_MODE_CHANGED)
		{
			@Override
			public void action()
			{
				if (View.instance.sphereSize >= 0.1)
					viewControler.setSphereSize(View.instance.sphereSize - 0.1);
				else
					viewControler.setSphereSize(0);
			}

			@Override
			public boolean isEnabled()
			{
				return viewControler.getHighlightMode() == HighlightMode.Spheres;
			}
		};
		new ActionCreator(HIGHLIGHT_INCR_SPHERE_SIZE, null, ViewControler.PROPERTY_HIGHLIGHT_MODE_CHANGED)
		{
			@Override
			public void action()
			{
				if (View.instance.sphereSize <= 0.9)
					viewControler.setSphereSize(View.instance.sphereSize + 0.1);
				else
					viewControler.setSphereSize(1);
			}

			@Override
			public boolean isEnabled()
			{
				return viewControler.getHighlightMode() == HighlightMode.Spheres;
			}
		};
		new ActionCreator(HIGHLIGHT_DECR_SPHERE_TRANSLUCENCY, null, ViewControler.PROPERTY_HIGHLIGHT_MODE_CHANGED)
		{
			@Override
			public void action()
			{
				if (View.instance.sphereTranslucency >= 0.1)
					viewControler.setSphereTranslucency(View.instance.sphereTranslucency - 0.1);
				else
					viewControler.setSphereTranslucency(0);
			}

			@Override
			public boolean isEnabled()
			{
				return viewControler.getHighlightMode() == HighlightMode.Spheres;
			}
		};
		new ActionCreator(HIGHLIGHT_INCR_SPHERE_TRANSLUCENCY, null, ViewControler.PROPERTY_HIGHLIGHT_MODE_CHANGED)
		{
			@Override
			public void action()
			{
				if (View.instance.sphereTranslucency <= 0.9)
					viewControler.setSphereTranslucency(View.instance.sphereTranslucency + 0.1);
				else
					viewControler.setSphereTranslucency(1);
			}

			@Override
			public boolean isEnabled()
			{
				return viewControler.getHighlightMode() == HighlightMode.Spheres;
			}
		};
		new ActionCreator(HIDDEN_INCR_SPIN_SPEED)
		{
			@Override
			public void action()
			{
				viewControler.increaseSpinSpeed(true);
			}
		};
		new ActionCreator(HIDDEN_DECR_SPIN_SPEED)
		{
			@Override
			public void action()
			{
				viewControler.increaseSpinSpeed(false);
			}
		};
		new ActionCreator(HIDDEN_INCR_FONT_SIZE)
		{
			@Override
			public void action()
			{
				viewControler.increaseFontSize(true);
			}
		};
		new ActionCreator(HIDDEN_DECR_FONT_SIZE)
		{
			@Override
			public void action()
			{
				viewControler.increaseFontSize(false);
			}
		};
		new ActionCreator(HIDDEN_FILTER_FEATURES)
		{
			@Override
			public void action()
			{
				FeatureFilter filters[] = FeatureFilter.validValues(clustering);
				int idx = ArrayUtil.indexOf(filters, viewControler.getFeatureFilter());
				if (idx < filters.length - 1)
					idx++;
				else
					idx = 0;
				viewControler.setFeatureFilter(filters[idx]);
			}
		};
		new ActionCreator(HIDDEN_TOGGLE_SORTING)
		{
			@Override
			public void action()
			{
				viewControler.setFeatureSortingEnabled(!viewControler.isFeatureSortingEnabled());
			}
		};
		new ActionCreator(HIDDEN_TOGGLE_NOM_HIGHLIGHT)
		{
			@Override
			public void action()
			{
				int idx = ArrayUtil.indexOf(NominalColoring.values(), viewControler.getNominalColoring());
				if (idx == NominalColoring.values().length - 1)
					idx = 0;
				else
					idx++;
				viewControler.setNominalColoring(NominalColoring.values()[idx]);
			}
		};
		new ActionCreator(HIDDEN_TREE)
		{
			@Override
			public void action()
			{
				new TreeView(viewControler, clusterControler, clustering, guiControler);
			}
		};
		new ActionCreator(HIDDEN_COPY)
		{
			@Override
			public void action()
			{
				if (guiControler.getSelectedString() != null && guiControler.getSelectedString().length() > 0)
				{
					StringSelection s = new StringSelection(guiControler.getSelectedString());
					Clipboard system = Toolkit.getDefaultToolkit().getSystemClipboard();
					system.setContents(s, s);
					String str = guiControler.getSelectedString();
					guiControler.showMessage("Copy string to clipboard: '"
							+ (str.length() > 50 ? str.substring(0, 48) + ".." : str) + "'");
				}
				else
					guiControler.showMessage("Nothing selected for copying to clipboard");

			}
		};
		new ActionCreator(HIDDEN_PREDICT)
		{
			@Override
			public void action()
			{
				guiControler.block("predict");
				try
				{
					clustering.predict();
				}
				finally
				{
					guiControler.unblock("predict");
				}
			}
		};
		new ActionCreator(HIDDEN_APP_DOMAIN)
		{
			@Override
			public void action()
			{
				guiControler.block("app-domain");
				try
				{
					clustering.computeAppDomain();
				}
				finally
				{
					guiControler.unblock("app-domain");
				}
			}
		};
		new ActionCreator(EDIT_SHOW_DISTANCE)
		{
			@Override
			public void action()
			{
				guiControler.block("distance");
				try
				{
					Compound c = null;
					if (clustering.isCompoundWatched())
						c = clustering.getWatchedCompound();
					else if (clustering.isCompoundActive())
						c = clustering.getActiveCompound();
					viewControler.setHighlighter(clustering.addDistanceToCompoundFeature(c));
				}
				finally
				{
					guiControler.unblock("distance");
				}
			}
		};
		new ActionCreator(EDIT_SHOW_SALI)
		{
			@Override
			public void action()
			{
				SALIDialog.showDialog(viewControler, clustering, clusterControler);
			}
		};
		new ActionCreator(EDIT_LOG_TRANSFORM, null, ViewControler.PROPERTY_HIGHLIGHT_CHANGED)
		{
			@Override
			public void action()
			{
				NumericProperty p = clustering.addLogFeature((NumericProperty) viewControler.getHighlightedProperty());
				if (p != null)
					viewControler.setHighlighter(p);
			}

			@Override
			public boolean isEnabled()
			{
				return viewControler.getHighlightedProperty() instanceof NumericProperty;
			}
		};

	}

	private void newClustering()
	{
		clusterControler.newClustering();
	}

	public static void showAboutDialog()
	{
		TextPanel p = new TextPanel();
		p.addHeading(Settings.TITLE);
		p.addTable(new String[][] { { "Version:", Settings.VERSION_STRING }, { "Homepage:", Settings.HOMEPAGE },
				{ "Contact:", Settings.CONTACT } });
		p.setPreferredWith(600);
		JOptionPane.showMessageDialog(Settings.TOP_LEVEL_FRAME, p, "About " + Settings.TITLE,
				JOptionPane.INFORMATION_MESSAGE, Settings.CHES_MAPPER_IMAGE);
	}

	private Action[] getActions(String actionNames[])
	{
		Action a[] = new Action[actionNames.length];
		for (int i = 0; i < a.length; i++)
			a[i] = actions.get(actionNames[i]);
		return a;
	}

	public Action[] getFileActions()
	{
		return getActions(FILE_ACTIONS);
	}

	public Action[] getDataActions()
	{
		return getActions(DATA_ACTIONS);
	}

	public Action[] getEditActions()
	{
		return getActions(EDIT_ACTIONS);
	}

	public Action[] getExportActions()
	{
		return getActions(EXPORT_ACTIONS);
	}

	public Action[] getRemoveActions()
	{
		return getActions(REMOVE_ACTIONS);
	}

	public Action[] getFilterActions()
	{
		return getActions(FILTER_ACTIONS);
	}

	public Action[] getViewActions()
	{
		return getActions(VIEW_ACTIONS);
	}

	public Action[] getHighlightActions()
	{
		return getActions(HIGHLIGHT_ACTIONS);
	}

	public Action[] getHighlightSphereActions()
	{
		return getActions(HIGHLIGHT_SPHERE_ACTIONS);
	}

	public Action[] getHelpActions()
	{
		return getActions(HELP_ACTIONS);
	}

	public Action[] getHiddenActions()
	{
		return getActions(HIDDEN_ACTIONS);
	}

	public boolean performAction(final Object source, KeyStroke k, boolean onlyHidden)
	{
		for (final String a : (onlyHidden ? HIDDEN_ACTIONS : CollectionUtil.toArray(keys.keySet())))
		{
			KeyStroke keyStroke = keys.get(a);
			if (keyStroke != null && keyStroke.equals(k) && actions.get(a).isEnabled())
			{
				SwingUtilities.invokeLater(new Runnable()
				{
					@Override
					public void run()
					{
						actions.get(a).actionPerformed(new ActionEvent(source, -1, ""));
					}
				});
				return true;
			}
		}
		return false;
	}

	public static void main(String args[])
	{

		KeyStroke keystroke = KeyStroke.getKeyStroke(KeyEvent.VK_D, ActionEvent.ALT_MASK);
		System.out.println(KeyEvent.getKeyModifiersText(keystroke.getModifiers()));
		System.out.println(Character.toChars(keystroke.getKeyCode())[0]);

		//showAboutDialog();
	}
}