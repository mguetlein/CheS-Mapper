package org.chesmapper.view.gui.util;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.border.EmptyBorder;

import org.chesmapper.map.dataInterface.CompoundProperty;
import org.chesmapper.map.dataInterface.NominalProperty;
import org.chesmapper.map.dataInterface.NumericProperty;
import org.chesmapper.map.main.Settings;
import org.chesmapper.view.cluster.ClusterController;
import org.chesmapper.view.cluster.Clustering;
import org.chesmapper.view.cluster.SALIProperty;
import org.chesmapper.view.cluster.ClusteringImpl.LogProperty;
import org.chesmapper.view.gui.ViewControler;
import org.mg.javalib.gui.TextPanel;
import org.mg.javalib.util.ArrayUtil;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.FormLayout;

public class SALIDialog extends JDialog
{
	private SALIDialog(final ViewControler viewControler, final Clustering clustering, List<CompoundProperty> list)
	{
		super(Settings.TOP_LEVEL_FRAME, Settings.text("action.edit-show-sali"), true);

		DefaultFormBuilder builder = new DefaultFormBuilder(new FormLayout("p,10px,fill:p:grow"));

		TextPanel tp1 = new TextPanel(Settings.text("props.sali.detail", SALIProperty.MIN_ENDPOINT_DEV_STR));
		builder.append(tp1, 3);
		builder.nextLine();

		final JComboBox<CompoundProperty> propCombo = new JComboBox<CompoundProperty>(ArrayUtil.toArray(
				CompoundProperty.class, list));
		if (viewControler.getHighlightedProperty() != null)
		{
			CompoundProperty sel = viewControler.getHighlightedProperty();
			if (list.contains(sel))
				propCombo.setSelectedItem(sel);
		}
		JLabel label = new JLabel("Endpoint:");
		builder.append(label);
		builder.append(propCombo);
		builder.nextLine();

		JButton ok = new JButton("OK");
		JButton close = new JButton("Cancel");
		ok.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				SALIDialog.this.setVisible(false);
				CompoundProperty p = clustering.addSALIFeatures((CompoundProperty) propCombo.getSelectedItem());
				if (p != null)
					viewControler.setHighlighter(p);
			}
		});
		close.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				SALIDialog.this.setVisible(false);
			}
		});
		builder.append(" ");//add gap
		builder.nextLine();
		builder.append(ButtonBarFactory.buildOKCancelBar(ok, close), 3);
		builder.setBorder(new EmptyBorder(10, 10, 10, 10));

		setLayout(new BorderLayout());
		add(builder.getPanel());

		pack();
		tp1.setPreferredWith(Math.max(300, label.getPreferredSize().width + 10 + propCombo.getPreferredSize().width));
		pack();
		setLocationRelativeTo(getOwner());
	}

	public static void showDialog(ViewControler viewControler, Clustering clustering, ClusterController clusterControler)
	{
		if (clusterControler.getCompoundFilter() != null)
		{
			JOptionPane
					.showMessageDialog(
							Settings.TOP_LEVEL_FRAME,
							"Currently, activity cliffs can only be computed for the complete un-filtered dataset. Please remove compound filter first.",
							"Message", JOptionPane.OK_OPTION);
			return;
		}

		List<CompoundProperty> list = new ArrayList<CompoundProperty>();
		for (CompoundProperty p : clustering.getAdditionalProperties())
			if (p instanceof LogProperty)
				list.add(p);
		for (CompoundProperty p : clustering.getProperties())
		{
			if (p instanceof NumericProperty || ((NominalProperty) p).getDomain().length == 2)
				list.add(p);
		}
		if (list.size() == 0)
			JOptionPane
					.showMessageDialog(
							Settings.TOP_LEVEL_FRAME,
							"Currently, only numeric or binary endpoint properties are supported.\nNo such property is available in the dataset.",
							"Message", JOptionPane.OK_OPTION);
		else
		{
			SALIDialog d = new SALIDialog(viewControler, clustering, list);
			d.setVisible(true);
		}
	}
}
