package org.chesmapper.view.gui.util;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JRadioButton;
import javax.swing.border.EmptyBorder;

import org.chesmapper.map.main.Settings;
import org.chesmapper.view.cluster.Clustering;
import org.chesmapper.view.gui.ViewControler;
import org.chesmapper.view.gui.ViewControler.DisguiseMode;
import org.mg.javalib.gui.TextPanel;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.FormLayout;

public class HideUnselectedDialog extends JDialog
{
	private HideUnselectedDialog(final ViewControler viewControler, final Clustering clustering)
	{
		super(Settings.TOP_LEVEL_FRAME, Settings.text("hide-unselected.title"), true);

		DefaultFormBuilder builder = new DefaultFormBuilder(new FormLayout("fill:p:grow"));

		TextPanel tp1 = new TextPanel(Settings.text("hide-unselected.unzoomed-info"));
		builder.append(tp1);

		final JRadioButton zoomInvisible = new JRadioButton(Settings.text("hide-unselected.invisible"));
		final JRadioButton zoomTranslucent = new JRadioButton(Settings.text("hide-unselected.translucent"));
		final JRadioButton zoomSolid = new JRadioButton(Settings.text("hide-unselected.solid"));
		JRadioButton zoomButtons[] = new JRadioButton[] { zoomInvisible, zoomTranslucent, zoomSolid };
		ButtonGroup group = new ButtonGroup();
		for (JRadioButton r : zoomButtons)
		{
			group.add(r);
			builder.append(r);
		}
		if (viewControler.getDisguiseUnZoomed() == DisguiseMode.invisible)
			zoomInvisible.setSelected(true);
		else if (viewControler.getDisguiseUnZoomed() == DisguiseMode.translucent)
			zoomTranslucent.setSelected(true);
		else
			zoomSolid.setSelected(true);

		builder.append(" ");//add gap

		TextPanel tp2 = new TextPanel(Settings.text("hide-unselected.unhovered-info"));
		builder.append(tp2);

		final JRadioButton hoverTranslucent = new JRadioButton(Settings.text("hide-unselected.translucent"));
		final JRadioButton hoverSolid = new JRadioButton(Settings.text("hide-unselected.solid"));
		JRadioButton hoverButtons[] = new JRadioButton[] { hoverTranslucent, hoverSolid };
		ButtonGroup group2 = new ButtonGroup();
		for (JRadioButton r : hoverButtons)
		{
			group2.add(r);
			builder.append(r);
		}
		if (viewControler.getDisguiseUnHovered() == DisguiseMode.translucent)
			hoverTranslucent.setSelected(true);
		else
			hoverSolid.setSelected(true);

		ActionListener l = new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent a)
			{
				if (a.getSource() == zoomInvisible)
					viewControler.setDisguiseUnZoomed(DisguiseMode.invisible);
				else if (a.getSource() == zoomTranslucent)
					viewControler.setDisguiseUnZoomed(DisguiseMode.translucent);
				else if (a.getSource() == zoomSolid)
					viewControler.setDisguiseUnZoomed(DisguiseMode.solid);
				else if (a.getSource() == hoverTranslucent)
					viewControler.setDisguiseUnHovered(DisguiseMode.translucent);
				else if (a.getSource() == hoverSolid)
					viewControler.setDisguiseUnHovered(DisguiseMode.solid);
			}
		};
		for (JRadioButton r : zoomButtons)
			r.addActionListener(l);
		for (JRadioButton r : hoverButtons)
			r.addActionListener(l);

		JButton close = new JButton("Close");
		close.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				HideUnselectedDialog.this.setVisible(false);
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
				if (!HideUnselectedDialog.this.isVisible())
					return;
			}
		});

		pack();
		tp1.setPreferredWith(300);
		tp2.setPreferredWith(300);
		pack();
		setLocationRelativeTo(getOwner());
	}

	public static void showDialog(ViewControler viewControler, Clustering clustering)
	{
		HideUnselectedDialog d = new HideUnselectedDialog(viewControler, clustering);
		d.setVisible(true);
	}
}
