package org.chesmapper.view.gui.table;

import java.awt.Color;
import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;

import org.chesmapper.map.dataInterface.CompoundProperty;
import org.chesmapper.map.dataInterface.NominalProperty;
import org.chesmapper.map.dataInterface.NumericProperty;
import org.chesmapper.view.cluster.ClusterController;
import org.chesmapper.view.cluster.Clustering;
import org.chesmapper.view.cluster.Compound;
import org.chesmapper.view.cluster.Clustering.SelectionListener;
import org.chesmapper.view.gui.ViewControler;
import org.chesmapper.view.gui.swing.ComponentFactory;
import org.mg.javalib.gui.ClickMouseOverTable;
import org.mg.javalib.gui.ClickMouseOverTable.ClickMouseOverRenderer;
import org.mg.javalib.util.StringUtil;

public class CompoundTable extends CCDataTable
{
	CompoundTable(ViewControler viewControler, ClusterController clusterControler, Clustering clustering,
			List<CompoundProperty> props)
	{
		super(viewControler, clusterControler, clustering, props);
	}

	@Override
	protected JTable createTable()
	{
		table = new ClickMouseOverTable(tableModel)
		{
			public Component prepareRenderer(TableCellRenderer renderer, int row, int column)
			{
				Component c = super.prepareRenderer(renderer, row, column);
				if (c instanceof JComponent)
				{
					JComponent jc = (JComponent) c;
					jc.setToolTipText(getValueAt(row, 1).toString());
				}
				return c;
			}
		};
		sorter = new TableRowSorter<DefaultTableModel>();
		table.setRowSorter(sorter);
		sorter.setModel(tableModel);

		table.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		int count = 0;
		for (Compound m : clustering.getCompounds(false))
		{
			Object o[] = new Object[tableModel.getColumnCount()];
			int i = 0;
			o[i++] = ++count;
			o[i++] = m;
			o[i++] = m.getSmiles();
			if (i != nonPropColumns)
				throw new Error();
			for (CompoundProperty p : props)
				if (p instanceof NumericProperty)
					o[i++] = m.getDoubleValue((NumericProperty) p);
				else
					o[i++] = m.getStringValue((NominalProperty) p);
			tableModel.addRow(o);
		}

		clustering.addSelectionListener(new SelectionListener()
		{
			@Override
			public void compoundActiveChanged(Compound[] c)
			{
				if (!isVisible())
					return;
				updateTableFromSelection(true, clustering.getActiveCompoundsJmolIdx());
			}

			@Override
			public void compoundWatchedChanged(Compound[] c)
			{
				if (!isVisible())
					return;
				updateTableFromSelection(false, clustering.getWatchedCompoundsJmolIdx());
			}
		});

		viewControler.addViewListener(new PropertyChangeListener()
		{

			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				if (!isVisible())
					return;
				if (evt.getPropertyName().equals(ViewControler.PROPERTY_HIGHLIGHT_CHANGED))
					table.repaint();
			}
		});

		table.getClickSelectionModel().addListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent e)
			{
				updateCompoundFromTable(table.getClickSelectionModel().getSelectedIndices(), true);
			}
		});

		table.getSelectionModel().addListSelectionListener(new ListSelectionListener()
		{
			@Override
			public void valueChanged(ListSelectionEvent e)
			{
				if (e.getValueIsAdjusting())
					return;
				updateCompoundFromTable(table.getSelectedRows(), false);
			}
		});
		ClickMouseOverRenderer renderer = new ClickMouseOverTable.ClickMouseOverRenderer(table)
		{
			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
					boolean hasFocus, int row, int column)
			{
				Object val;
				//Compound m = ((Compound) table.getValueAt(sorter.convertRowIndexToView(row), 1));
				Compound m = ((Compound) table.getValueAt(row, 1));
				//				System.out.println(row + " " + m);
				CompoundProperty p = null;

				if (column >= nonPropColumns)
				{
					p = props.get(column - nonPropColumns);
					if (addSpecificityInfo())
						val = m.getFormattedValue(p) + " (" + StringUtil.formatDouble(clustering.getSpecificity(m, p))
								+ ")";
					else
						val = m.getFormattedValue(p);
				}
				else
					val = value;
				Component comp = super.getTableCellRendererComponent(table, val, isSelected, hasFocus, row, column);

				if (column >= nonPropColumns)
				{
					Color col = viewControler.getHighlightColor(m, p, true, false);
					setForeground(col);
				}
				else
				{
					setForeground(Color.BLACK);
				}
				return comp;
			}
		};
		renderer.clickSelectedBackground = ComponentFactory.LIST_ACTIVE_BACKGROUND_WHITE;
		renderer.mouseOverSelectedBackground = ComponentFactory.LIST_WATCH_BACKGROUND_WHITE;

		//		renderer.clickSelectedBackground = ComponentFactory.LIST_ACTIVE_BACKGROUND;
		//		renderer.mouseOverSelectedBackground = ComponentFactory.LIST_WATCH_BACKGROUND;
		//		renderer.background = ComponentFactory.BACKGROUND;
		//		renderer.clickSelectedForeground = ComponentFactory.LIST_SELECTION_FOREGROUND;
		//		renderer.mouseOverSelectedForeground = ComponentFactory.LIST_SELECTION_FOREGROUND;
		//		renderer.foreground = ComponentFactory.FOREGROUND;
		table.setDefaultRenderer(Object.class, renderer);
		table.setDefaultRenderer(Integer.class, renderer);
		table.setDefaultRenderer(Double.class, renderer);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		table.setGridColor(ComponentFactory.BACKGROUND);

		updateTableFromSelection(true, clustering.getActiveCompoundsJmolIdx());
		updateTableFromSelection(false, clustering.getWatchedCompoundsJmolIdx());
		updateFeatureSelection();

		return table;
	}

	@Override
	protected boolean addSpecificityInfo()
	{
		return false;
	}

	@Override
	protected String getItemName()
	{
		return "compound";
	}

	@Override
	public String getExtraColumn()
	{
		return "SMILES";
	}

	@Override
	public boolean addAdditionalProperties()
	{
		return true;
	}

	@Override
	protected String getShortName()
	{
		return "compound-table";
	}

	private void updateCompoundFromTable(int tableSelection[], final boolean active)
	{
		if (selfUpdate)
			return;
		selfUpdate = true;

		if (tableSelection == null)
			tableSelection = new int[0];
		final int row[] = tableSelection;
		final Compound[] comps = new Compound[row.length];
		for (int i = 0; i < comps.length; i++)
			comps[i] = clustering.getCompoundWithJmolIndex(sorter.convertRowIndexToModel(row[i]));
		Thread th = new Thread(new Runnable()
		{
			public void run()
			{
				if (active)
				{
					if (row.length == 0)
						clusterControler.clearCompoundActive(true);
					else
						clusterControler.setCompoundActive(comps, true);
				}
				else
				{
					if (row.length == 0)
						clusterControler.clearCompoundWatched();
					else
						clusterControler.setCompoundWatched(comps);
				}
				selfUpdate = false;
			}
		});
		th.start();
	}
}
