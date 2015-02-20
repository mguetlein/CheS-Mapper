package org.chesmapper.view.gui;

import java.awt.BorderLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;

import org.chesmapper.map.main.Settings;
import org.chesmapper.view.cluster.Clustering;

import com.jgoodies.forms.factories.ButtonBarFactory;

public class ColorEditor extends JDialog
{
	ViewControler viewControler;
	boolean okPressed = false;
	ColorEditorPanel panels[];

	public ColorEditor(Window owner, String title, ViewControler viewControler, Clustering clustering)
	{
		super(owner, title);
		setModal(true);
		this.viewControler = viewControler;

		JTabbedPane tabbedPane = new JTabbedPane();
		panels = new ColorEditorPanel[] { new ColorEditorPanel.ClusterColorEditorPanel(viewControler, clustering),
				new ColorEditorPanel.NumericColorEditorPanel(viewControler, clustering),
				new ColorEditorPanel.NominalColorEditorPanel(viewControler, clustering),
				new ColorEditorPanel.SmartsColorEditorPanel(viewControler, clustering) };
		int idx = 0;
		for (ColorEditorPanel p : panels)
		{
			if (p.isEnabledInDataset())
			{
				tabbedPane.add(p.getName(), p);
				if (p.isPropertySelectedInViewer())
					tabbedPane.setSelectedIndex(idx);
				idx++;
			}
		}

		final JButton ok = new JButton("OK");
		final JButton cancel = new JButton("Cancel");
		ActionListener l = new ActionListener()
		{

			@Override
			public void actionPerformed(ActionEvent e)
			{
				okPressed = e.getSource() == ok;
				ColorEditor.this.setVisible(false);
			}
		};
		ok.addActionListener(l);
		cancel.addActionListener(l);

		JPanel panel = new JPanel(new BorderLayout(10, 10));
		panel.add(tabbedPane, BorderLayout.CENTER);
		panel.add(ButtonBarFactory.buildOKCancelBar(ok, cancel), BorderLayout.SOUTH);
		panel.setBorder(new EmptyBorder(10, 10, 10, 10));
		add(panel);
		pack();
		if (Settings.TOP_LEVEL_FRAME.getHeight() < getHeight())
			setSize(getWidth(), Settings.TOP_LEVEL_FRAME.getHeight());
		setLocationRelativeTo(owner);
		setVisible(true);
	}

	public static void start(ViewControler viewControler, Clustering clustering)
	{
		ColorEditor col = new ColorEditor(Settings.TOP_LEVEL_FRAME, "Change highlighting colors", viewControler,
				clustering);
		if (col.okPressed)
			for (ColorEditorPanel p : col.panels)
				if (p.isEnabledInDataset())
					p.applyChanges();
	}
}
