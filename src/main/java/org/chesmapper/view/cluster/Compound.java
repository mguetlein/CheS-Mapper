package org.chesmapper.view.cluster;

import java.awt.Color;
import java.util.BitSet;
import java.util.HashMap;

import javax.swing.ImageIcon;
import javax.vecmath.Vector3f;

import org.apache.commons.lang3.StringEscapeUtils;
import org.chesmapper.map.dataInterface.CompoundData;
import org.chesmapper.map.dataInterface.CompoundProperty;
import org.chesmapper.map.dataInterface.NominalProperty;
import org.chesmapper.map.dataInterface.NumericProperty;
import org.chesmapper.map.dataInterface.SingleCompoundPropertyOwner;
import org.chesmapper.map.main.Settings;
import org.chesmapper.view.gui.MainPanel.Translucency;
import org.chesmapper.view.gui.View;
import org.chesmapper.view.gui.ViewControler;
import org.chesmapper.view.gui.ViewControler.Style;
import org.chesmapper.view.gui.Zoomable;
import org.mg.javalib.gui.DoubleNameListCellRenderer.DoubleNameElement;
import org.mg.javalib.util.ColorUtil;
import org.mg.javalib.util.ObjectUtil;

public class Compound implements Zoomable, Comparable<Compound>, DoubleNameElement, SingleCompoundPropertyOwner
{
	private BitSet bitSet;
	private BitSet dotModeHideBitSet;
	private BitSet dotModeDisplayBitSet;

	private int jmolIndex;
	private CompoundData compoundData;

	private Translucency translucency = Translucency.None;
	private String label = null;
	private boolean showHoverBox = false;
	private boolean showActiveBox = false;
	private String smarts = null;

	private HashMap<String, BitSet> smartsMatches;

	private String compoundColor;
	private String highlightColorString;
	private Color highlightColorText;
	private String lastHighlightColorString;

	private Vector3f spherePosition;
	private CompoundProperty highlightCompoundProperty;
	private Style style;
	private CompoundProperty descriptorProperty = null;
	private boolean sphereVisible;
	private boolean lastFeatureSphereVisible;
	private boolean visible = true;
	private boolean featureSortingEnabled = true;

	private float diameter = -1;

	public final Vector3f origCenter;
	public final Vector3f origDotPosition;

	public Compound(int jmolIndex, CompoundData compoundData)
	{
		this.jmolIndex = jmolIndex;
		this.compoundData = compoundData;
		if (View.instance != null)
		{
			bitSet = View.instance.getCompoundBitSet(getJmolIndex());
			dotModeHideBitSet = View.instance.getDotModeHideBitSet(bitSet);
			dotModeDisplayBitSet = View.instance.getDotModeDisplayBitSet(bitSet);
			origCenter = new Vector3f(View.instance.getAtomSetCenter(bitSet));
			origDotPosition = new Vector3f(View.instance.getAtomSetCenter(getDotModeDisplayBitSet()));
		}
		else
		{
			//for export without graphics
			origCenter = null;
			origDotPosition = null;
		}
		smartsMatches = new HashMap<String, BitSet>();
		setDescriptor(ViewControler.COMPOUND_INDEX_PROPERTY);
	}

	public String getFormattedValue(CompoundProperty property)
	{
		return compoundData.getFormattedValue(property);
	}

	@Override
	public String getStringValue(NominalProperty property)
	{
		return compoundData.getStringValue(property);
	}

	public Double getDoubleValue(NumericProperty property)
	{
		return compoundData.getDoubleValue(property);
	}

	public int getJmolIndex()
	{
		return jmolIndex;
	}

	public int getOrigIndex()
	{
		return compoundData.getOrigIndex();
	}

	public static class DisplayName implements Comparable<DisplayName>
	{
		String valDisplay;
		@SuppressWarnings("rawtypes")
		Comparable valCompare[];
		Integer compareIndex;
		String name;

		public String toString(boolean html, Color highlightColor)
		{
			StringBuffer b = new StringBuffer();
			if (html)
				b.append(StringEscapeUtils.escapeHtml4(name));
			else
				b.append(name);
			if (valDisplay != null && !valDisplay.equals(name))
			{
				if (html)
				{
					b.append(":&nbsp;");
					if (highlightColor != null)
						b.append("<font color='" + ColorUtil.toHtml(highlightColor) + "'>");
					b.append("<i>");
					b.append(StringEscapeUtils.escapeHtml4(valDisplay));
					b.append("</i>");
					if (highlightColor != null)
						b.append("</font>");
				}
				else
				{
					b.append(": ");
					b.append(valDisplay);
				}
			}
			return b.toString();
		}

		@Override
		public int compareTo(DisplayName d)
		{
			return compareTo(d, true);
		}

		public int compareTo(DisplayName d, boolean featureSortingEnabled)
		{
			if (featureSortingEnabled && valCompare != null)
			{
				for (int j = 0; j < valCompare.length; j++)
				{
					int i = ObjectUtil.compare(valCompare[j], d.valCompare[j]);
					if (i != 0)
						return i;
				}
			}
			if (compareIndex != null)
				return compareIndex.compareTo(d.compareIndex);
			// if nothing is selected, compound should be sorted according to identifier
			return name.compareTo(d.name);
		}
	}

	private DisplayName displayName = new DisplayName();

	@Override
	public String toString()
	{
		return getFirstName();
	}

	public String toStringWithValue()
	{
		return displayName.toString(false, null);
	}

	@Override
	public String getFirstName()
	{
		return displayName.name;
	}

	@Override
	public String getSecondName()
	{
		if (ObjectUtil.equals(displayName.valDisplay, displayName.name))
			return null;
		else
			return displayName.valDisplay;
	}

	@Override
	public int compareTo(Compound m)
	{
		return displayName.compareTo(m.displayName, featureSortingEnabled);
	}

	public String getSmiles()
	{
		return compoundData.getSmiles();
	}

	public Translucency getTranslucency()
	{
		return translucency;
	}

	public void setTranslucency(Translucency translucency)
	{
		this.translucency = translucency;
	}

	public String getLabel()
	{
		return label;
	}

	public void setLabel(String label)
	{
		this.label = label;
	}

	public boolean isShowHoverBox()
	{
		return showHoverBox;
	}

	public void setShowHoverBox(boolean showBox)
	{
		this.showHoverBox = showBox;
	}

	public boolean isShowActiveBox()
	{
		return showActiveBox;
	}

	public void setShowActiveBox(boolean showBox)
	{
		this.showActiveBox = showBox;
	}

	public String getHighlightedSmarts()
	{
		return smarts;
	}

	public void setHighlightedSmarts(String smarts)
	{
		this.smarts = smarts;
	}

	public void moveTo(Vector3f clusterPos)
	{
		Vector3f center = new Vector3f(View.instance.getAtomSetCenter(getBitSet()));
		Vector3f dest = new Vector3f(clusterPos);
		dest.sub(center);
		View.instance.setAtomCoordRelative(dest, getBitSet());
	}

	public Vector3f getPosition(boolean scaled)
	{
		Vector3f v = new Vector3f(JitteringProvider.getPosition(compoundData));
		if (scaled)
			v.scale(ClusteringUtil.SCALE);
		return v;
	}

	public Vector3f getPosition()
	{
		return getPosition(true);
	}

	public BitSet getSmartsMatch(String smarts)
	{
		//compute match dynamically
		if (!smartsMatches.containsKey(smarts))
		{
			Settings.LOGGER.info("smarts-matching smarts: " + smarts + " smiles: " + getSmiles());
			smartsMatches.put(smarts, View.instance.getSmartsMatch(smarts, bitSet));
		}
		return smartsMatches.get(smarts);
	}

	public void setCompoundColor(String colorString)
	{
		this.compoundColor = colorString;
	}

	public String getCompoundColor()
	{
		return compoundColor;
	}

	public void setHighlightColor(String colorString, Color colorText)
	{
		if (!ObjectUtil.equals(highlightColorString, colorString) || lastHighlightColorString == null)
		{
			this.lastHighlightColorString = highlightColorString;
			this.highlightColorString = colorString;
			this.highlightColorText = colorText;
		}
	}

	public Color getHighlightColorText()
	{
		return highlightColorText;
	}

	public String getHighlightColorString()
	{
		return highlightColorString;
	}

	public String getLastHighlightColorString()
	{
		return lastHighlightColorString;
	}

	public Vector3f getSpherePosition()
	{
		return spherePosition;
	}

	public void setSpherePosition(Vector3f spherePosition)
	{
		this.spherePosition = spherePosition;
	}

	public void setHighlightCompoundProperty(CompoundProperty highlightCompoundProperty)
	{
		if (this.highlightCompoundProperty != highlightCompoundProperty)
		{
			displayName.valDisplay = null;
			displayName.valCompare = null;
			if (highlightCompoundProperty != null)
			{
				if (highlightCompoundProperty instanceof NumericProperty)
					displayName.valCompare = new Double[] { getDoubleValue((NumericProperty) highlightCompoundProperty) };
				else
					displayName.valCompare = new String[] { getStringValue((NominalProperty) highlightCompoundProperty) };
				displayName.valDisplay = getFormattedValue(highlightCompoundProperty);
			}
			this.highlightCompoundProperty = highlightCompoundProperty;
			lastHighlightColorString = null;
		}
	}

	public Object getHighlightCompoundProperty()
	{
		return highlightCompoundProperty;
	}

	public void setStyle(Style style)
	{
		this.style = style;
	}

	public Style getStyle()
	{
		return style;
	}

	@Override
	public Vector3f getCenter(boolean superimposed)
	{
		return new Vector3f(View.instance.getAtomSetCenter(bitSet));
	}

	@Override
	public float getDiameter(boolean superimposed)
	{
		return getDiameter();
	}

	public float getDiameter()
	{
		if (diameter == -1)
			diameter = View.instance.getDiameter(bitSet);
		return diameter;
	}

	@Override
	public boolean isSuperimposed()
	{
		return false;
	}

	public ImageIcon getIcon(boolean backgroundBlack, int width, int height, boolean translucent)
	{
		return compoundData.getIcon(backgroundBlack, width, height, translucent);
	}

	public void setDescriptor(CompoundProperty descriptorProperty)
	{
		if (this.descriptorProperty != descriptorProperty)
		{
			displayName.compareIndex = null;
			if (descriptorProperty == ViewControler.COMPOUND_INDEX_PROPERTY)
			{
				displayName.compareIndex = getOrigIndex();
				displayName.name = "Compound " + (getOrigIndex() + 1);
			}
			else if (descriptorProperty == ViewControler.COMPOUND_SMILES_PROPERTY)
				displayName.name = getSmiles();
			else
				displayName.name = getFormattedValue(descriptorProperty);
			this.descriptorProperty = descriptorProperty;
		}
	}

	public boolean isSphereVisible()
	{
		return sphereVisible;
	}

	public void setSphereVisible(boolean sphereVisible)
	{
		this.sphereVisible = sphereVisible;
	}

	public boolean isLastFeatureSphereVisible()
	{
		return lastFeatureSphereVisible;
	}

	public void setLastFeatureSphereVisible(boolean s)
	{
		this.lastFeatureSphereVisible = s;
	}

	public BitSet getBitSet()
	{
		return bitSet;
	}

	public BitSet getDotModeHideBitSet()
	{
		return dotModeHideBitSet;
	}

	public BitSet getDotModeDisplayBitSet()
	{
		return dotModeDisplayBitSet;
	}

	public DisplayName getDisplayName()
	{
		return displayName;
	}

	public boolean isVisible()
	{
		return visible;
	}

	public void setVisible(boolean visible)
	{
		this.visible = visible;
	}

	public void setFeatureSortingEnabled(boolean featureSortingEnabled)
	{
		this.featureSortingEnabled = featureSortingEnabled;
	}

	public CompoundData getCompoundData()
	{
		return compoundData;
	}
}
