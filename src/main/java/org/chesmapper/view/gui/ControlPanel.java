package org.chesmapper.view.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;

import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.chesmapper.view.cluster.ClusterController;
import org.chesmapper.view.cluster.Clustering;
import org.chesmapper.view.cluster.ClusteringImpl;
import org.chesmapper.view.cluster.Compound;
import org.chesmapper.view.cluster.JitteringProvider;
import org.chesmapper.view.cluster.Clustering.SelectionListener;
import org.chesmapper.view.gui.ViewControler.HighlightSorting;
import org.chesmapper.view.gui.ViewControler.Style;
import org.chesmapper.view.gui.swing.ComponentFactory;
import org.chesmapper.view.gui.swing.TransparentViewPanel;
import org.chesmapper.view.gui.swing.ComponentFactory.ClickableLabel;
import org.chesmapper.view.gui.swing.ComponentFactory.StyleButton;
import org.chesmapper.view.gui.util.CompoundPropertyHighlighter;
import org.chesmapper.view.gui.util.Highlighter;
import org.mg.javalib.gui.DescriptionListCellRenderer;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.Sizes;

public class ControlPanel extends JPanel
{
	boolean selfUpdate = false;

	StyleButton buttonWire;
	StyleButton buttonBalls;
	StyleButton buttonDots;

	JSlider sliderSize;
	ClickableLabel buttonPlusSize;
	ClickableLabel buttonMinusSize;

	JSlider sliderJitter;
	ClickableLabel buttonPlusJitter;
	ClickableLabel buttonMinusJitter;

	JComboBox<Highlighter> highlightCombobox;
	JComboBox<HighlightSorting> highlightMinMaxCombobox;
	ClickableLabel buttonClearFeature;

	ViewControler viewControler;
	Clustering clustering;
	GUIControler guiControler;
	ClusterController clusterControler;

	public ControlPanel(ViewControler viewControler, ClusterController clusterControler, Clustering clustering,
			GUIControler guiControler)
	{
		this.viewControler = viewControler;
		this.clusterControler = clusterControler;
		this.clustering = clustering;
		this.guiControler = guiControler;

		buildLayout();
		addListeners();
	}

	private void buildLayout()
	{
		buttonWire = new StyleButton("Wireframe", true, Style.wireframe);
		buttonBalls = new StyleButton("Balls & Sticks", false, Style.ballsAndSticks);
		buttonDots = new StyleButton("Dots", false, Style.dots);

		ButtonGroup g = new ButtonGroup();
		for (StyleButton b : new StyleButton[] { buttonWire, buttonBalls, buttonDots })
		{
			g.add(b);
			b.setOpaque(false);
			b.setFocusable(false);
		}
		updateSelectedStyle();

		buttonPlusSize = ComponentFactory.createPlusViewButton();
		buttonMinusSize = ComponentFactory.createMinusViewButton();

		buttonPlusJitter = ComponentFactory.createPlusViewButton();
		buttonMinusJitter = ComponentFactory.createMinusViewButton();
		buttonMinusJitter.setEnabled(viewControler.getJitteringLevel() > 0);

		sliderSize = ComponentFactory.createViewSlider(0, viewControler.getCompoundSizeMax(),
				viewControler.getCompoundSize());
		sliderSize.setPreferredSize(new Dimension(100, sliderSize.getPreferredSize().height));

		sliderJitter = ComponentFactory.createViewSlider(0, JitteringProvider.STEPS, viewControler.getJitteringLevel());
		sliderJitter.setPreferredSize(new Dimension(50, sliderJitter.getPreferredSize().height));

		highlightCombobox = ComponentFactory.createViewComboBox(Highlighter.class);
		loadHighlighters();

		highlightCombobox.setSelectedItem(viewControler.getHighlighter());

		highlightMinMaxCombobox = ComponentFactory.createViewComboBox(HighlightSorting.values());

		highlightMinMaxCombobox.setSelectedItem(viewControler.getHighlightSorting());
		highlightMinMaxCombobox.setVisible(false);

		buttonClearFeature = ComponentFactory.createCrossViewButton();

		JPanel p = new TransparentViewPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
		p.add(ComponentFactory.createViewLabel("Size: "));
		p.add(buttonMinusSize);
		p.add(sliderSize);
		p.add(buttonPlusSize);
		p.add(new JLabel("  "));
		p.add(ComponentFactory.createViewLabel("Spread: "));
		p.add(buttonMinusJitter);
		p.add(sliderJitter);
		p.add(buttonPlusJitter);
		p.setBorder(new EmptyBorder(5, 5, 2, 5));

		JPanel p1 = new TransparentViewPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
		//		p1.add(ComponentFactory.createViewLabel("Style: "));
		p1.add(buttonWire);
		p1.add(new JLabel(""));
		p1.add(buttonBalls);
		p1.add(new JLabel(""));
		p1.add(buttonDots);

		JPanel p2 = new TransparentViewPanel();
		p2.add(ComponentFactory.createViewLabel("Feature:"));
		p2.add(highlightCombobox);
		p2.add(highlightMinMaxCombobox);
		JPanel clear = new JPanel(new BorderLayout());
		clear.setOpaque(false);
		clear.add(buttonClearFeature);
		clear.setBorder(new EmptyBorder(0, 1, 0, 0));
		p2.add(clear);

		DefaultFormBuilder b = new DefaultFormBuilder(new FormLayout("left:p"));
		b.setLineGapSize(Sizes.pixel(0));
		b.append(p1);
		b.append(p);
		b.append(p2);
		b.getPanel().setOpaque(false);

		setLayout(new BorderLayout());
		add(b.getPanel(), BorderLayout.WEST);
		setOpaque(false);
	}

	private void addListeners()
	{
		ActionListener l = new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				if (selfUpdate)
					return;
				viewControler.setStyle(((StyleButton) e.getSource()).style);
			}
		};
		for (StyleButton b : new StyleButton[] { buttonWire, buttonBalls, buttonDots })
			b.addActionListener(l);

		ActionListener l2 = new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				viewControler.changeCompoundSize(e.getSource() == buttonPlusSize);
			}
		};
		buttonPlusSize.addActionListener(l2);
		buttonMinusSize.addActionListener(l2);

		ActionListener l3 = new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				if (e.getSource() == buttonPlusJitter && viewControler.getJitteringLevel() < JitteringProvider.STEPS)
					viewControler.setJitteringLevel(viewControler.getJitteringLevel() + 1);
				else if (viewControler.getJitteringLevel() > 0)
					viewControler.setJitteringLevel(viewControler.getJitteringLevel() - 1);
			}
		};
		buttonPlusJitter.addActionListener(l3);
		buttonMinusJitter.addActionListener(l3);

		sliderSize.addChangeListener(new ChangeListener()
		{
			@Override
			public void stateChanged(ChangeEvent e)
			{
				if (selfUpdate)
					return;
				JSlider source = (JSlider) e.getSource();
				if (!source.getValueIsAdjusting())
				{
					viewControler.setCompoundSize((int) source.getValue());
				}
			}
		});

		sliderJitter.addChangeListener(new ChangeListener()
		{
			@Override
			public void stateChanged(ChangeEvent e)
			{
				if (selfUpdate)
					return;
				JSlider source = (JSlider) e.getSource();
				if (!source.getValueIsAdjusting())
				{
					viewControler.setJitteringLevel((int) source.getValue());
				}
			}
		});

		highlightCombobox.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (selfUpdate)
					return;
				SwingUtilities.invokeLater(new Runnable()
				{
					@Override
					public void run()
					{
						viewControler.setHighlighter((Highlighter) highlightCombobox.getSelectedItem());
					}
				});
			}
		});

		highlightMinMaxCombobox.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (selfUpdate)
					return;
				SwingUtilities.invokeLater(new Runnable()
				{
					@Override
					public void run()
					{
						viewControler.setHighlightSorting((HighlightSorting) highlightMinMaxCombobox.getSelectedItem());
					}
				});
			}
		});

		buttonClearFeature.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				viewControler.setHighlighter(Highlighter.DEFAULT_HIGHLIGHTER);
			}
		});

		clustering.addSelectionListener(new SelectionListener()
		{
			@Override
			public void compoundActiveChanged(Compound[] c)
			{
				updateComboStuff();
				updateJitteringButtons();
			}
		});

		clustering.addListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				if (evt.getPropertyName().equals(ClusteringImpl.CLUSTER_NEW))
					updateSelectedStyle();
			}
		});

		viewControler.addViewListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				// Settings.LOGGER.println("fire updated " +
				// evt.getPropertyName());
				if (evt.getPropertyName().equals(ViewControler.PROPERTY_HIGHLIGHT_CHANGED))
				{
					updateComboStuff();
				}
				else if (evt.getPropertyName().equals(ViewControler.PROPERTY_SUPERIMPOSE_CHANGED))
				{
					updateComboStuff();
					updateJitteringButtons();
				}
				else if (evt.getPropertyName().equals(ViewControler.PROPERTY_NEW_HIGHLIGHTERS))
				{
					loadHighlighters();
					updateComboStuff();
				}
				else if (evt.getPropertyName().equals(ViewControler.PROPERTY_DENSITY_CHANGED))
				{
					selfUpdate = true;
					buttonPlusSize.setEnabled(viewControler.canChangeCompoundSize(true));
					buttonMinusSize.setEnabled(viewControler.canChangeCompoundSize(false));
					sliderSize.setEnabled(viewControler.canChangeCompoundSize(false)
							|| viewControler.canChangeCompoundSize(true));
					sliderSize.setValue(viewControler.getCompoundSize());
					selfUpdate = false;
				}
				else if (evt.getPropertyName().equals(ViewControler.PROPERTY_FONT_SIZE_CHANGED))
					updateComboSize();
				else if (evt.getPropertyName().equals(ViewControler.PROPERTY_STYLE_CHANGED))
					updateSelectedStyle();
				else if (evt.getPropertyName().equals(ViewControler.PROPERTY_JITTERING_CHANGED))
					updateJitteringButtons();
			}
		});

		guiControler.addPropertyChangeListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				if (evt.getPropertyName().equals(GUIControler.PROPERTY_VIEWER_SIZE_CHANGED))
				{
					updateComboSize();
				}
			}
		});
	}

	private void updateJitteringButtons()
	{
		selfUpdate = true;
		boolean enable = viewControler.canJitter();
		buttonPlusJitter.setEnabled(enable && viewControler.getJitteringLevel() < JitteringProvider.STEPS);
		buttonMinusJitter.setEnabled(enable && viewControler.getJitteringLevel() > 0);
		sliderJitter.setEnabled(enable);
		sliderJitter.setValue(viewControler.getJitteringLevel());
		selfUpdate = false;
	}

	private void updateSelectedStyle()
	{
		selfUpdate = true;
		for (StyleButton b : new StyleButton[] { buttonWire, buttonBalls, buttonDots })
		{
			b.setSelected(viewControler.getStyle() == b.style);
			if (clustering.isBigDataMode())
				b.setEnabled(b == buttonDots);
			else
				b.setEnabled(true);
		}
		selfUpdate = false;
	}

	private void updateComboStuff()
	{
		if (!SwingUtilities.isEventDispatchThread())
			throw new IllegalStateException("GUI updates only in event dispatch thread plz");

		selfUpdate = true;
		if (((DefaultComboBoxModel<Highlighter>) highlightCombobox.getModel()).getIndexOf(viewControler
				.getHighlighter()) == -1)
			new IllegalStateException("cannot find " + viewControler.getHighlighter() + " in highlighter combo box")
					.printStackTrace();
		highlightCombobox.setSelectedItem(viewControler.getHighlighter());
		highlightMinMaxCombobox.setSelectedItem(viewControler.getHighlightSorting());
		boolean featHighSel = ((Highlighter) highlightCombobox.getSelectedItem()) instanceof CompoundPropertyHighlighter;
		highlightMinMaxCombobox.setVisible(featHighSel && viewControler.isSuperimpose()
				&& !clustering.isClusterActive());
		buttonClearFeature.setVisible(viewControler.getHighlighter() != Highlighter.DEFAULT_HIGHLIGHTER);
		selfUpdate = false;
	}

	private void loadHighlighters()
	{
		selfUpdate = true;
		((DefaultComboBoxModel<Highlighter>) highlightCombobox.getModel()).removeAllElements();
		(((DescriptionListCellRenderer) highlightCombobox.getRenderer())).clearDescriptions();

		HashMap<String, Highlighter[]> h = viewControler.getHighlighters();
		if (h != null)
		{
			int index = 0;
			for (String desc : h.keySet())
			{
				if (desc != null && desc.length() > 0)
					(((DescriptionListCellRenderer) highlightCombobox.getRenderer())).addDescription(index, desc);
				index += h.get(desc).length;
				for (Highlighter hh : h.get(desc))
					highlightCombobox.addItem(hh);
			}
		}
		updateComboSize();
		selfUpdate = false;
	}

	private void updateComboSize()
	{
		highlightCombobox.setPreferredSize(null);
		Dimension dim = highlightCombobox.getPreferredSize();
		int width = Math.min(dim.width, guiControler.getComponentMaxWidth(0.33));
		// System.out.println(width);
		highlightCombobox.setPreferredSize(new Dimension(width, dim.height));
		highlightCombobox.setMaximumRowCount(Math.min(highlightCombobox.getItemCount(), 15));
		highlightCombobox.revalidate();
	}

}
