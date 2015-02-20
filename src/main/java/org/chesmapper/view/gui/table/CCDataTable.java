package org.chesmapper.view.gui.table;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.table.DefaultTableModel;

import org.apache.commons.lang3.text.WordUtils;
import org.chesmapper.map.dataInterface.CompoundProperty;
import org.chesmapper.map.dataInterface.NumericProperty;
import org.chesmapper.view.cluster.ClusterController;
import org.chesmapper.view.cluster.Clustering;
import org.chesmapper.view.cluster.Compound;
import org.chesmapper.view.gui.ViewControler;
import org.mg.javalib.gui.ClickMouseOverTable;

public abstract class CCDataTable extends DataTable
{
	protected int nonPropColumns;
	protected ClickMouseOverTable table;
	protected int sortColumn = 1;

	public static void show(ViewControler viewControler, ClusterController clusterControler, Clustering clustering,
			boolean cluster)
	{
		List<CompoundProperty> props = clustering.selectPropertiesAndFeaturesWithDialog("Select features to show in "
				+ (cluster ? "cluster" : "compound") + " table.", viewControler.getHighlightedProperty(), false,
				!cluster, !cluster, !cluster);
		if (props != null)
		{
			if (cluster)
				new ClusterTable(viewControler, clusterControler, clustering, props);
			else
				new CompoundTable(viewControler, clusterControler, clustering, props);
		}
	}

	public CCDataTable(ViewControler viewControler, ClusterController clusterControler, Clustering clustering,
			List<CompoundProperty> props)
	{
		super(viewControler, clusterControler, clustering, props);
	}

	public abstract String getExtraColumn();

	public abstract boolean addAdditionalProperties();

	protected void updateFeatureSelection()
	{
		if (selfUpdate)
			return;
		selfUpdate = true;

		if (viewControler.getHighlightedProperty() != null)
		{
			CompoundProperty prop = viewControler.getHighlightedProperty();
			int idx = props.indexOf(prop);
			if (idx != -1)
				sortColumn = idx + nonPropColumns;
		}
		List<RowSorter.SortKey> sortKeys = new ArrayList<RowSorter.SortKey>();
		sortKeys.add(new RowSorter.SortKey(sortColumn, SortOrder.ASCENDING));
		sorter.setSortKeys(sortKeys);

		selfUpdate = false;
	}

	@Override
	protected DefaultTableModel createTableModel()
	{
		DefaultTableModel model = new DefaultTableModel()
		{
			@Override
			public Class<?> getColumnClass(int columnIndex)
			{
				if (columnIndex == 0)
					return Integer.class;
				if (columnIndex == 1)
					return Compound.class;
				if (columnIndex >= nonPropColumns && props.get(columnIndex - nonPropColumns) instanceof NumericProperty)
					return Double.class;
				return String.class;
			}

			@Override
			public boolean isCellEditable(int row, int column)
			{
				return false;
			}
		};
		model.addColumn("");
		model.addColumn(WordUtils.capitalize(getItemName()));
		model.addColumn(getExtraColumn());
		nonPropColumns = model.getColumnCount();

		for (CompoundProperty p : props)
			model.addColumn(p);

		return model;
	}

	protected void updateTableFromSelection(boolean active, int... selected)
	{
		if (selfUpdate)
			return;
		selfUpdate = true;

		if (active)
			table.getClickSelectionModel().clearSelection();
		else
			table.getSelectionModel().clearSelection();

		if (selected.length > 1 || (selected.length == 1 && selected[0] != -1))
		{
			int idx = -1;
			for (int i : selected)
			{
				idx = sorter.convertRowIndexToView(i);
				if (active)
					table.getClickSelectionModel().setSelected(idx, false);
				else
					table.getSelectionModel().addSelectionInterval(idx, idx);
			}
			table.scrollRectToVisible(new Rectangle(table.getCellRect(idx, 0, true)));
		}
		selfUpdate = false;
	}
}
