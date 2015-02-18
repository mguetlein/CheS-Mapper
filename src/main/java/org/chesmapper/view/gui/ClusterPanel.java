package org.chesmapper.view.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.LayoutManager;
import java.awt.image.BufferedImage;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.OverlayLayout;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import org.chesmapper.map.data.ClusteringData;
import org.chesmapper.map.main.ScreenSetup;
import org.chesmapper.view.cluster.ClusterController;
import org.chesmapper.view.cluster.Clustering;
import org.chesmapper.view.gui.swing.ComponentFactory;
import org.chesmapper.view.gui.swing.TransparentViewLabel;
import org.mg.javalib.util.StringUtil;
import org.mg.javalib.util.ThreadUtil;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class ClusterPanel extends JPanel
{
	GUIControler guiControler;
	JPanel messagePanel;
	JLabel messageLabel;
	private MainPanel mainPanel;

	public ClusterPanel(GUIControler guiControler)
	{
		this.guiControler = guiControler;

		mainPanel = new MainPanel(guiControler);

		LayoutManager layout = new OverlayLayout(this);
		setLayout(layout);

		FormLayout l = new FormLayout("center:pref:grow", "center:pref:grow");
		messagePanel = new JPanel(l);// new BorderLayout());
		messagePanel.setOpaque(false);
		messageLabel = ComponentFactory.createTransparentViewLabel();
		((TransparentViewLabel) messageLabel).setAlpha(150); // less translucent for better readability
		messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
		messageLabel.setOpaque(true);
		//		messageLabel.setBackground(Settings.TRANSPARENT_BACKGROUND);
		CellConstraints cc = new CellConstraints();
		messagePanel.add(messageLabel, cc.xy(1, 1));
		messagePanel.setVisible(false);
		add(messagePanel);

		JPanel allPanelsContainer = new JPanel(new BorderLayout());
		allPanelsContainer.setOpaque(false);
		ClusterListPanel clusterListPanel = new ClusterListPanel(mainPanel.getClustering(), mainPanel, mainPanel,
				guiControler);
		allPanelsContainer.add(clusterListPanel, BorderLayout.WEST);
		add(allPanelsContainer, BorderLayout.WEST);

		final int gap = 20;
		JPanel infoAndChartContainer = new JPanel(new BorderLayout(0, gap));
		infoAndChartContainer.setOpaque(false);

		final int top = 25;
		final int bottom = 25;

		final InfoPanel infoPanel = new InfoPanel(mainPanel, mainPanel, mainPanel.getClustering(), guiControler);
		final JPanel chartContainer = new JPanel(new BorderLayout())
		{
			@Override
			public Dimension getPreferredSize()
			{
				// to "push back" the table
				int increasedHeight = (ClusterPanel.this.getHeight() - (gap + top + bottom + 40))
						- infoPanel.getPreferredTableHeight();
				if (increasedHeight < 0)
					return super.getPreferredSize();
				else
					return new Dimension(10, Math.max(super.getPreferredSize().height, increasedHeight));
			}
		};
		ChartPanel chartPanel = new ChartPanel(mainPanel.getClustering(), mainPanel, mainPanel, guiControler);
		chartPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
		chartContainer.setOpaque(false);
		JPanel chartWrapperPanel = new JPanel(new BorderLayout());
		chartWrapperPanel.setOpaque(false);
		chartWrapperPanel.add(chartPanel, BorderLayout.EAST);
		chartContainer.add(chartWrapperPanel, BorderLayout.SOUTH);

		infoAndChartContainer.add(infoPanel, BorderLayout.EAST);
		infoAndChartContainer.add(chartContainer, BorderLayout.SOUTH);
		infoAndChartContainer.setBorder(new EmptyBorder(top, 25, bottom, 25));

		allPanelsContainer.add(infoAndChartContainer);

		//		SwingUtil.setDebugBorder(chartPanel, Color.LIGHT_GRAY);
		//		SwingUtil.setDebugBorder(allPanelsContainer, Color.RED);
		//		SwingUtil.setDebugBorder(infoAndChartContainer, Color.GREEN);
		//		SwingUtil.setDebugBorder(chartContainer, Color.ORANGE);
		//		SwingUtil.setDebugBorder(infoPanel, Color.BLUE);
		//		SwingUtil.setDebugBorder(clusterListPanel, Color.MAGENTA);

		add(mainPanel);
		setOpaque(false);
	}

	public void paint(Graphics g)
	{
		super.paint(g);

		BufferedImage ic = ComponentFactory.getCheSMapperStringImage();
		g.drawImage(ic, getWidth() - ic.getWidth(), getHeight() - ic.getHeight(), this);
	}

	public void init(ClusteringData clusteredDataset)
	{
		mainPanel.init(clusteredDataset);
	}

	public Clustering getClustering()
	{
		return mainPanel.getClustering();
	}

	String currentMsg;

	public void showMessage(final String msg)
	{
		currentMsg = msg;
		Thread th = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				messagePanel.setIgnoreRepaint(true);
				messageLabel.setFont(messageLabel.getFont().deriveFont(ScreenSetup.INSTANCE.getFontSize() + 8f));
				messageLabel.setText(msg);
				FontMetrics metrics = messageLabel.getFontMetrics(messageLabel.getFont());
				messagePanel.setSize(metrics.stringWidth(msg), metrics.getHeight());
				messagePanel.setIgnoreRepaint(false);
				messagePanel.setVisible(true);
				// show message between 2.4 and 6.4 seconds depending on the number of words (<=4 words 2.4s, 7 words 4.8s, >=9 words 6.4s)
				long sleep = Math.max(2400, Math.min(6400, StringUtil.numOccurences(msg, " ") * 800));
				ThreadUtil.sleep(sleep);
				if (msg == currentMsg)
					messagePanel.setVisible(false);
			}
		});
		th.start();
	}

	public ViewControler getViewControler()
	{
		return mainPanel;
	}

	public ClusterController getClusterControler()
	{
		return mainPanel;
	}

}
