package org.chesmapper.view.gui.util;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.border.EmptyBorder;

import org.chesmapper.map.main.Settings;
import org.chesmapper.view.cluster.Clustering;
import org.chesmapper.view.gui.Actions;
import org.chesmapper.view.gui.ViewControler;
import org.chesmapper.view.gui.ViewControler.FeatureFilter;
import org.mg.javalib.gui.TextPanel;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.FormLayout;

public class SortFilterDialog extends JDialog
{

	private SortFilterDialog(final ViewControler viewControler, Clustering clustering)
	{
		super(Settings.TOP_LEVEL_FRAME, "Sort and filter features", true);

		DefaultFormBuilder builder = new DefaultFormBuilder(new FormLayout("fill:p:grow,10px,fill:p:grow"));

		TextPanel tp1 = new TextPanel("The feature list on the right side of the viewer can be sorted and filtered.\n");
		builder.append(tp1, 3);

		final JCheckBox sortBox = new JCheckBox("Sort features according to specificity ("
				+ Actions.getToggleSortingKey() + ")");
		sortBox.setSelected(viewControler.isFeatureSortingEnabled());
		sortBox.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				viewControler.setFeatureSortingEnabled(sortBox.isSelected());
			}
		});
		builder.append(sortBox, 3);

		TextPanel tp2 = new TextPanel(
				"(The sorting is only available if clusters or compounds are selected, not for the entire dataset. "
						+ "The features are sorted according to specficity, i.e. the most important features are at the top. See online documentation for details.)\n");
		builder.append(tp2, 3);

		final JComboBox<FeatureFilter> filterCombo = new JComboBox<FeatureFilter>(FeatureFilter.validValues(clustering));
		filterCombo.setSelectedItem(viewControler.getFeatureFilter());
		filterCombo.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				viewControler.setFeatureFilter((FeatureFilter) filterCombo.getSelectedItem());
			}
		});
		filterCombo.setRenderer(new DefaultListCellRenderer()
		{
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
					boolean cellHasFocus)
			{
				return super.getListCellRendererComponent(list, ((FeatureFilter) value).niceString(), index,
						isSelected, cellHasFocus);
			}
		});
		builder.append(new JLabel("Filter features (" + Actions.getFilterFeaturesKey() + "):"));
		builder.append(filterCombo);

		JButton close = new JButton("Close");
		close.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				SortFilterDialog.this.setVisible(false);
			}
		});
		builder.append(ButtonBarFactory.buildCloseBar(close));
		builder.setBorder(new EmptyBorder(10, 10, 10, 10));

		setLayout(new BorderLayout());
		add(builder.getPanel());

		viewControler.addViewListener(new PropertyChangeListener()
		{

			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				if (!SortFilterDialog.this.isVisible())
					return;
				if (evt.getPropertyName().equals(ViewControler.PROPERTY_FEATURE_SORTING_CHANGED))
					sortBox.setSelected(viewControler.isFeatureSortingEnabled());
				else if (evt.getPropertyName().equals(ViewControler.PROPERTY_FEATURE_FILTER_CHANGED))
					filterCombo.setSelectedItem(viewControler.getFeatureFilter());
			}
		});

		pack();
		tp1.setPreferredWith(Math.max(300, sortBox.getPreferredSize().width));
		tp2.setPreferredWith(Math.max(300, sortBox.getPreferredSize().width));
		pack();
		setLocationRelativeTo(getOwner());
	}

	public static void showDialog(ViewControler viewControler, Clustering clustering)
	{
		SortFilterDialog d = new SortFilterDialog(viewControler, clustering);
		d.setVisible(true);
	}
}
