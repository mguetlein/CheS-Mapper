package org.chesmapper.view.gui;

import java.awt.Color;
import java.beans.PropertyChangeListener;
import java.util.HashMap;

import javax.swing.JComponent;

import org.chesmapper.map.dataInterface.CompoundProperty;
import org.chesmapper.map.dataInterface.CompoundPropertyOwner;
import org.chesmapper.map.dataInterface.CompoundPropertyUtil;
import org.chesmapper.map.dataInterface.DefaultNominalProperty;
import org.chesmapper.map.dataInterface.NominalProperty;
import org.chesmapper.map.dataInterface.NumericProperty;
import org.chesmapper.map.dataInterface.SubstructureSmartsType;
import org.chesmapper.map.dataInterface.CompoundPropertyUtil.NominalColoring;
import org.chesmapper.view.cluster.Clustering;
import org.chesmapper.view.gui.util.Highlighter;
import org.mg.javalib.gui.property.ColorGradient;
import org.mg.javalib.util.ArrayUtil;

public interface ViewControler
{
	public enum Style
	{
		wireframe, ballsAndSticks, dots
	}

	public enum DisguiseMode
	{
		solid, translucent, invisible
	}

	public static enum HighlightMode
	{
		ColorCompounds, Spheres;
	}

	public static final ColorGradient DEFAULT_COLOR_GRADIENT = new ColorGradient(
			CompoundPropertyUtil.getHighValueColor(), Color.WHITE, CompoundPropertyUtil.getLowValueColor());

	public Color getHighlightColor(CompoundPropertyOwner m, CompoundProperty p, boolean textColor);

	public Color getHighlightColor(CompoundPropertyOwner m, CompoundProperty p, boolean textColor,
			boolean blackBackground);

	public DisguiseMode getDisguiseUnHovered();

	public DisguiseMode getDisguiseUnZoomed();

	public void setDisguiseUnHovered(DisguiseMode hide);

	public void setDisguiseUnZoomed(DisguiseMode hide);

	public void resetView();

	public boolean isSpinEnabled();

	public void setSpinEnabled(boolean spinEnabled);

	public boolean canChangeCompoundSize(boolean larger);

	public void changeCompoundSize(boolean larger);

	public int getCompoundSize();

	public int getCompoundSizeMax();

	public void setCompoundSize(int compoundSize);

	public HighlightMode getHighlightMode();

	public void setHighlightMode(HighlightMode mode);

	public void setSphereSize(double size);

	public void setSphereTranslucency(double translucency);

	public Style getStyle();

	public void setStyle(Style style);

	public HashMap<String, Highlighter[]> getHighlighters();

	public void setHighlighter(Highlighter highlighter);

	public void setHighlighter(Highlighter highlighter, boolean showMessage);

	public void setHighlighter(CompoundProperty prop);

	public void setHighlighter(SubstructureSmartsType type);

	public Highlighter getHighlighter();

	public Highlighter getHighlighter(SubstructureSmartsType type);

	public Highlighter getHighlighter(CompoundProperty p);

	public CompoundProperty getHighlightedProperty();

	public void setSuperimpose(boolean superimpose);

	public boolean isSuperimpose();

	public boolean isAllClustersSpreadable();

	public boolean isSingleClusterSpreadable();

	public boolean isHideHydrogens();

	public void setHideHydrogens(boolean b);

	public static final String PROPERTY_HIGHLIGHT_CHANGED = "propertyHighlightChanged";
	public static final String PROPERTY_SHOW_HYDROGENS = "propertyShowHydrogens";
	public static final String PROPERTY_NEW_HIGHLIGHTERS = "propertyNewHighlighters";
	public static final String PROPERTY_DENSITY_CHANGED = "propertyDensityChanged";
	public static final String PROPERTY_SUPERIMPOSE_CHANGED = "propertySuperimposeChanged";
	public static final String PROPERTY_DISGUISE_CHANGED = "propertyDisguiseChanged";
	public static final String PROPERTY_SPIN_CHANGED = "propertySpinChanged";
	public static final String PROPERTY_BACKGROUND_CHANGED = "propertyBackgroundChanged";
	public static final String PROPERTY_FONT_SIZE_CHANGED = "propertyFontSizeChanged";
	public static final String PROPERTY_COMPOUND_DESCRIPTOR_CHANGED = "propertyCompoundDescriptorChanged";
	public static final String PROPERTY_HIGHLIGHT_MODE_CHANGED = "propertyHighlightModeChanged";
	public static final String PROPERTY_HIGHLIGHT_COLORS_CHANGED = "propertyHighlightColorsChanged";
	public static final String PROPERTY_ANTIALIAS_CHANGED = "propertyAntialiasChanged";
	public static final String PROPERTY_HIGHLIGHT_LAST_FEATURE = "propertyHighlightLastFeature";
	public static final String PROPERTY_STYLE_CHANGED = "propertyStyleChanged";
	public static final String PROPERTY_FEATURE_FILTER_CHANGED = "propertyFeatureFilterChanged";
	public static final String PROPERTY_FEATURE_SORTING_CHANGED = "propertyFeatureSortingChanged";
	public static final String PROPERTY_COMPOUND_FILTER_CHANGED = "propertyCompoundFilterChanged";
	public static final String PROPERTY_SINGLE_COMPOUND_SELECTION_ENABLED = "propertySingleCompoundSelectionEnabled";
	public static final String PROPERTY_JITTERING_CHANGED = "propertyJitteringChanged";

	public boolean isHighlighterLabelsVisible();

	public void setHighlighterLabelsVisible(boolean selected);

	public static enum HighlightSorting
	{
		Max, Median, Min;
	}

	public void setHighlightSorting(HighlightSorting sorting);

	public HighlightSorting getHighlightSorting();

	public void addViewListener(PropertyChangeListener l);

	public boolean isBlackgroundBlack();

	public void setBackgroundBlack(boolean backgroudBlack);

	public void increaseFontSize(boolean increase);

	public void setFontSize(int fontsize);

	public int getFontSize();

	static final CompoundProperty COMPOUND_INDEX_PROPERTY = new DefaultNominalProperty(null, "Compound Index",
			"no-desc");
	static final CompoundProperty COMPOUND_SMILES_PROPERTY = new DefaultNominalProperty(null, "Compound SMILES",
			"no-desc");

	public void setCompoundDescriptor(CompoundProperty prop);

	public CompoundProperty getCompoundDescriptor();

	public void addIgnoreMouseMovementComponents(JComponent ignore);

	public void updateMouseSelection(boolean buttonDown);

	public void setHighlightColors(ColorGradient g, NumericProperty props[]);

	public void setHighlightColors(Color g[], NominalProperty props[]);

	public void setClusterColors(Color[] sequence);

	void setHighlightMatchColors(Color[] colors);

	public void setSelectLastSelectedHighlighter();

	public boolean isAntialiasEnabled();

	public void setAntialiasEnabled(boolean b);

	public void setHighlightLastFeatureEnabled(boolean b);

	public boolean isHighlightLastFeatureEnabled();

	public void increaseSpinSpeed(boolean increase);

	public static enum FeatureFilter
	{
		None, NotSelectedForMapping, SelectedForMapping, UsedForMapping, Filled, Real, Endpoints;

		public static FeatureFilter[] validValues(Clustering clustering)
		{
			FeatureFilter f[] = new FeatureFilter[] { None, NotSelectedForMapping, SelectedForMapping };
			if (clustering.isSkippingRedundantFeatures())
				f = ArrayUtil.concat(f, new FeatureFilter[] { UsedForMapping });
			if (clustering.isBMBFRealEndpointDataset(true))
				f = ArrayUtil.concat(f, new FeatureFilter[] { Filled, Real, Endpoints });
			else if (clustering.isBMBFRealEndpointDataset(false))
				f = ArrayUtil.concat(f, new FeatureFilter[] { Real, Endpoints });
			return f;
		}

		public String niceString()
		{
			switch (this)
			{
				case None:
					return "Show all features (no filter)";
				case NotSelectedForMapping:
					return "Show features NOT selected for mapping";
				case SelectedForMapping:
					return "Show features selected for mapping";
				case UsedForMapping:
					return "Show features used for mapping (no redundant/single-valued features)";
				case Filled:
					return "Show '_filled' features";
				case Real:
					return "Show '_real' features";
				case Endpoints:
					return "Show endpoint features";
			}
			throw new IllegalStateException();
		}
	}

	public void setFeatureFilter(FeatureFilter filter);

	public FeatureFilter getFeatureFilter();

	public boolean isFeatureSortingEnabled();

	public void setFeatureSortingEnabled(boolean b);

	public boolean isShowClusteringPropsEnabled();

	public void showSortFilterDialog();

	public void setSingleCompoundSelection(boolean b);

	public boolean isSingleCompoundSelection();

	public void doMouseMoveWatchUpdates(Runnable runnable);

	public void clearMouseMoveWatchUpdates(boolean clearWatched);

	public NominalColoring getNominalColoring();

	public void setNominalColoring(NominalColoring nominalColoringValue);

	public int getJitteringLevel();

	public void setJitteringLevel(int level);

	public boolean canJitter();

	// to remove

	//	public void setZoomToSingleActiveCompounds(boolean b);

	//	public void setCompoundFilter(CompoundFilter filter, boolean animate);
	//
	//	public void useSelectedCompoundsAsFilter(String filterDescription, boolean animate);
	//
	//	public CompoundFilter getCompoundFilter();
}
