package org.chesmapper.view.gui.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.plaf.basic.BasicSliderUI;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import org.chesmapper.map.main.ScreenSetup;
import org.chesmapper.map.main.Settings;
import org.chesmapper.view.gui.LaunchCheSMapper;
import org.chesmapper.view.gui.ViewControler.Style;
import org.mg.javalib.gui.BorderImageIcon;
import org.mg.javalib.gui.DescriptionListCellRenderer;
import org.mg.javalib.gui.LinkButton;
import org.mg.javalib.gui.SimpleImageIcon;
import org.mg.javalib.gui.StringImageIcon;
import org.mg.javalib.util.ColorUtil;
import org.mg.javalib.util.SwingUtil;

public class ComponentFactory
{
	public static Color BACKGROUND;
	public static Color FOREGROUND;
	public static Color FOREGROUND_DISABLED;
	public static Color BORDER_FOREGROUND;
	public static Color LIST_SELECTION_FOREGROUND;
	public static Color LIST_ACTIVE_BACKGROUND;
	public static Color LIST_WATCH_BACKGROUND;

	public static Color FOREGROUND_FOR_BACKGROUND_BLACK = new Color(250, 250, 250);
	public static Color FOREGROUND_FOR_BACKGROUND_WHITE = new Color(5, 5, 5);
	public static final int DISABLED_TRANSPARENT = 100; //0 = fully transparent, 255= opaque

	public static Color LIST_ACTIVE_BACKGROUND_BLACK = new Color(51, 102, 255);
	public static Color LIST_WATCH_BACKGROUND_BLACK = LIST_ACTIVE_BACKGROUND_BLACK.darker().darker();
	public static Color LIST_ACTIVE_BACKGROUND_WHITE = new Color(101, 152, 255);
	public static Color LIST_WATCH_BACKGROUND_WHITE = LIST_ACTIVE_BACKGROUND_WHITE.brighter().brighter();

	private static boolean backgroundBlack = true;

	public static boolean isBackgroundBlack()
	{
		return backgroundBlack;
	}

	static
	{
		setBackgroundBlack(backgroundBlack);
	}

	public static void setBackgroundBlack(Boolean b)
	{
		backgroundBlack = b;
		FOREGROUND = getForeground(b);
		FOREGROUND_DISABLED = ColorUtil.transparent(FOREGROUND, DISABLED_TRANSPARENT);
		if (b)
		{
			BACKGROUND = Color.BLACK;
			BORDER_FOREGROUND = FOREGROUND;
			LIST_SELECTION_FOREGROUND = Color.WHITE;
			LIST_ACTIVE_BACKGROUND = LIST_ACTIVE_BACKGROUND_BLACK;
			LIST_WATCH_BACKGROUND = LIST_WATCH_BACKGROUND_BLACK;
		}
		else
		{
			BACKGROUND = Color.WHITE;
			BORDER_FOREGROUND = Color.LIGHT_GRAY;
			LIST_SELECTION_FOREGROUND = Color.BLACK;
			LIST_ACTIVE_BACKGROUND = LIST_ACTIVE_BACKGROUND_WHITE;
			LIST_WATCH_BACKGROUND = LIST_WATCH_BACKGROUND_WHITE;
		}
		updateComponents();
	}

	public static Color getForeground(boolean blackBackground)
	{
		if (blackBackground)
			return FOREGROUND_FOR_BACKGROUND_BLACK;
		else
			return FOREGROUND_FOR_BACKGROUND_WHITE;
	}

	private static List<PropertyChangeListener> updateUIListeners = new ArrayList<PropertyChangeListener>();

	public static void updateComponents()
	{
		if (updateUIListeners != null)
			for (PropertyChangeListener p : updateUIListeners)
				p.propertyChange(new PropertyChangeEvent(Settings.TOP_LEVEL_FRAME, "updateComponents", false, true));
		if (Settings.TOP_LEVEL_FRAME != null)
			SwingUtilities.updateComponentTreeUI(Settings.TOP_LEVEL_FRAME);
	}

	//	static class UIUtil
	//	{
	//		static HashMap<String, Color> orig = new HashMap<String, Color>();
	//
	//		public static void set(String v, Color c)
	//		{
	//			if (!orig.containsKey(v))
	//				orig.put(v, UIManager.getColor(v));
	//			UIManager.put(v, new ColorUIResource(c));
	//		}
	//		public static void unset()
	//		{
	//			for (String v : orig.keySet())
	//				UIManager.put(v, new ColorUIResource(orig.get(v)));
	//		}
	//	}
	//	private static void setViewUIColors()
	//	{
	//		UIUtil.set("ScrollBar.background", BACKGROUND);
	//		UIUtil.set("ScrollBar.foreground", BORDER_FOREGROUND);
	//		UIUtil.set("ScrollBar.thumbHighlight", BORDER_FOREGROUND);
	//		UIUtil.set("ScrollBar.thumbShadow", BORDER_FOREGROUND.darker());
	//		UIUtil.set("ScrollBar.thumbDarkShadow", BORDER_FOREGROUND.darker().darker());
	//		UIUtil.set("ScrollBar.thumb", BACKGROUND);
	//		UIUtil.set("ScrollBar.track", BACKGROUND);
	//		UIUtil.set("ScrollBar.trackHighlight", BACKGROUND);
	//		UIUtil.set("ComboBox.buttonBackground", BACKGROUND);
	//		UIUtil.set("ComboBox.buttonShadow", BORDER_FOREGROUND.darker());
	//		UIUtil.set("ComboBox.buttonDarkShadow", BORDER_FOREGROUND.darker().darker());
	//		UIUtil.set("ComboBox.buttonHighlight", BORDER_FOREGROUND);
	//	}
	//	private static void unsetViewUIColors()
	//	{
	//		UIUtil.unset();
	//	}

	public static JTextField createUneditableViewTextField()
	{
		JTextField l = new JTextField()
		{
			public void updateUI()
			{
				super.updateUI();
				setForeground(FOREGROUND);
				setFont(getFont().deriveFont((float) ScreenSetup.INSTANCE.getFontSize()));
				setBorder(null);
			}

			@Override
			public void setText(String t)
			{
				super.setText(t);
				setToolTipText(t);
			}
		};
		l.setFocusable(false);
		l.setEditable(false);
		l.setBorder(null);
		l.setOpaque(false);
		return l;
	}

	public static JLabel createTransparentViewLabel()
	{
		return new TransparentViewLabel();
	}

	public static JLabel createViewLabel()
	{
		return createViewLabel("");
	}

	public static JLabel createViewLabel(String text)
	{
		return createViewLabel(text, null);
	}

	public static JLabel createViewLabel(String text, ImageIcon iconBlack, ImageIcon iconWhite)
	{
		return createViewLabel(text, null, iconBlack, iconWhite);
	}

	public static JLabel createViewLabel(String text, DimensionProvider preferredSize)
	{
		return createViewLabel(text, preferredSize, null, null);
	}

	public static interface DimensionProvider
	{
		public Dimension getPreferredSize(Dimension orig);
	}

	public static JLabel createViewLabel(String text, final DimensionProvider preferredSize, final ImageIcon iconBlack,
			final ImageIcon iconWhite)
	{
		JLabel l = new JLabel(text)
		{
			public void updateUI()
			{
				super.updateUI();
				setForeground(FOREGROUND);
				setFont(getFont().deriveFont((float) ScreenSetup.INSTANCE.getFontSize()));
				if (iconBlack != null)
					setIcon(isBackgroundBlack() ? iconBlack : iconWhite);
			}

			@Override
			public void setText(String text)
			{
				super.setText(text);
				setToolTipText(text);
			}

			@Override
			public Dimension getPreferredSize()
			{
				if (preferredSize == null)
					return super.getPreferredSize();
				else
					return preferredSize.getPreferredSize(super.getPreferredSize());
			}
		};
		if (iconBlack != null)
			l.setIcon(isBackgroundBlack() ? iconBlack : iconWhite);
		l.setToolTipText(text);
		return l;
	}

	//	public static Border createThinBorder()
	//	{
	//		return new MatteBorder(1, 1, 1, 1, FOREGROUND);
	//	}
	//
	//	public static Border createLineBorder(int thickness)
	//	{
	//		return new LineBorder(FOREGROUND, thickness);
	//	}

	public static LinkButton createViewLinkButton(String text)
	{
		LinkButton l = new LinkButton(text)
		{
			public void updateUI()
			{
				super.updateUI();
				setForegroundColor(FOREGROUND);
				setSelectedForegroundColor(LIST_SELECTION_FOREGROUND);
				setFont(getFont().deriveFont((float) ScreenSetup.INSTANCE.getFontSize()));
				setSelectedForegroundFont(getFont().deriveFont((float) ScreenSetup.INSTANCE.getFontSize()));
			}

			@Override
			public void setText(String text)
			{
				super.setText(text);
				setToolTipText(text);
			}
		};
		l.setForegroundColor(FOREGROUND);
		l.setSelectedForegroundColor(LIST_SELECTION_FOREGROUND);
		l.setSelectedForegroundFont(l.getFont());
		l.setFocusable(false);
		return l;
	}

	public static JCheckBox createViewCheckBox(String text)
	{
		JCheckBox c = new JCheckBox(text)
		{
			public void updateUI()
			{
				super.updateUI();
				setForeground(FOREGROUND);
				setFont(getFont().deriveFont((float) ScreenSetup.INSTANCE.getFontSize()));
			}
		};
		c.setFocusable(false);
		return c;
	}

	//	public static JRadioButton createViewRadioButton(String text)
	//	{
	//		JRadioButton r = new JRadioButton(text)
	//		{
	//			public void updateUI()
	//			{
	//				super.updateUI();
	//				setForeground(FOREGROUND);
	//			}
	//		};
	//		return r;
	//	}

	public static class StyleButton extends JRadioButton
	{
		public Style style;

		public StyleButton(String text, boolean selected, Style style)
		{
			super(text, selected);
			this.style = style;
		}

		public void updateUI()
		{
			super.updateUI();
			setForeground(FOREGROUND);
			setFont(getFont().deriveFont((float) ScreenSetup.INSTANCE.getFontSize()));
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> JComboBox<T> createViewComboBox(Class<T> type)
	{
		return createViewComboBox((T[]) Array.newInstance(type, 0));
	}

	public static <T> JComboBox<T> createViewComboBox(T[] items)
	{
		final Font f = new JLabel().getFont();
		DescriptionListCellRenderer r = new DescriptionListCellRenderer()
		{
			@SuppressWarnings("rawtypes")
			@Override
			public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
					boolean cellHasFocus)
			{
				list.setSelectionBackground(LIST_ACTIVE_BACKGROUND);
				list.setSelectionForeground(LIST_SELECTION_FOREGROUND);
				list.setForeground(FOREGROUND);
				list.setBackground(BACKGROUND);
				list.setFont(f.deriveFont((float) ScreenSetup.INSTANCE.getFontSize()));
				return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			}
		};

		JComboBox<T> c = new JComboBox<T>()
		{
			public void updateUI()
			{
				setUI(new BasicComboBoxUI()
				{
					protected JButton createArrowButton()
					{
						JButton button = new BasicArrowButton(BasicArrowButton.SOUTH, BACKGROUND,
								BORDER_FOREGROUND.darker(), BORDER_FOREGROUND.darker().darker(), BORDER_FOREGROUND);
						button.setName("ComboBox.arrowButton");
						button.setFont(f.deriveFont((float) ScreenSetup.INSTANCE.getFontSize()));
						return button;
					}

					protected ComboPopup createPopup()
					{
						return new BasicComboPopup(comboBox)
						{
							protected JScrollPane createScroller()
							{
								JScrollPane sp = new JScrollPane(list,
										ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
										ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
								sp.setHorizontalScrollBar(null);
								sp.setVerticalScrollBar(new JScrollBar(JScrollBar.VERTICAL)
								{
									public void updateUI()
									{
										setUI(new MyScrollBarUI());
									}
								});
								return sp;
							}

							protected void configurePopup()
							{
								super.configurePopup();
								setBorder(new EtchedBorder(BORDER_FOREGROUND, BORDER_FOREGROUND.darker()));
							}
						};
					}
				});
				setForeground(FOREGROUND);
				setBackground(BACKGROUND);
				setBorder(new EtchedBorder(BORDER_FOREGROUND, BORDER_FOREGROUND.darker()));
				setFont(f.deriveFont((float) ScreenSetup.INSTANCE.getFontSize()));
				if (getRenderer() instanceof DescriptionListCellRenderer)
					((DescriptionListCellRenderer) getRenderer())
							.setDescriptionForeground(FOREGROUND.darker().darker());
			}
		};
		for (T object : items)
			c.addItem(object);
		c.setOpaque(false);
		c.setForeground(FOREGROUND);
		c.setBackground(BACKGROUND);
		c.setFont(f.deriveFont((float) ScreenSetup.INSTANCE.getFontSize()));
		r.setDescriptionForeground(FOREGROUND.darker().darker());
		c.setRenderer(r);
		c.setFocusable(false);
		return c;
	}

	public static class FactoryTableCellRenderer extends DefaultTableCellRenderer
	{
		boolean halfTransparent;

		public FactoryTableCellRenderer(boolean halfTransparent)
		{
			this.halfTransparent = halfTransparent;
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
				boolean hasFocus, int row, int column)
		{
			super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
			if (isSelected)
			{
				setBackground(LIST_ACTIVE_BACKGROUND);
				setOpaque(true);
				setForeground(LIST_SELECTION_FOREGROUND);
			}
			else
			{
				if (halfTransparent)
				{
					setBackground(new Color(BACKGROUND.getRed(), BACKGROUND.getGreen(), BACKGROUND.getBlue(), 100));
					setOpaque(true);
				}
				else
					setOpaque(false);
				setForeground(FOREGROUND);
			}
			return this;
		}
	}

	public static JTable createTable()
	{
		return createTable(false);
	}

	public static JTable createTable(boolean halfTransparent)
	{
		DefaultTableModel m = new DefaultTableModel()
		{
			public boolean isCellEditable(int row, int column)
			{
				return false;
			}
		};
		JTable t = new JTable(m)
		{
			public void updateUI()
			{
				super.updateUI();
				setFont(getFont().deriveFont((float) ScreenSetup.INSTANCE.getFontSize()));
				setRowHeight((int) (ScreenSetup.INSTANCE.getFontSize() * 1.7));
			}
		};
		t.setBorder(null);
		t.getTableHeader().setVisible(false);
		t.getTableHeader().setPreferredSize(new Dimension(-1, 0));
		t.setGridColor(new Color(0, 0, 0, 0));
		t.setOpaque(false);
		t.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		t.setDefaultRenderer(Object.class, new FactoryTableCellRenderer(halfTransparent));
		t.setFocusable(false);
		t.updateUI();
		return t;
	}

	public static int packColumn(JTable table, int vColIndex, int margin)
	{
		return packColumn(table, vColIndex, margin, Integer.MAX_VALUE);
	}

	public static int packColumn(JTable table, int vColIndex, int margin, int max)
	{
		return packColumn(table, vColIndex, margin, max, false);
	}

	public static int packColumn(JTable table, int vColIndex, int margin, int max, boolean fixMaxWidth)
	{
		DefaultTableColumnModel colModel = (DefaultTableColumnModel) table.getColumnModel();
		TableColumn col = colModel.getColumn(vColIndex);
		int width = 0;

		// Get width of column header
		TableCellRenderer renderer = col.getHeaderRenderer();
		if (renderer == null)
		{
			renderer = table.getTableHeader().getDefaultRenderer();
		}
		Component comp = renderer.getTableCellRendererComponent(table, col.getHeaderValue(), false, false, 0, 0);
		width = comp.getPreferredSize().width;

		// Get maximum width of column data
		for (int r = 0; r < table.getRowCount(); r++)
		{
			renderer = table.getCellRenderer(r, vColIndex);
			comp = renderer.getTableCellRendererComponent(table, table.getValueAt(r, vColIndex), false, false, r,
					vColIndex);
			width = Math.max(width, comp.getPreferredSize().width);
		}

		// Add margin
		width += 2 * margin;
		if (width > max)
			width = max;

		// Set the width
		col.setPreferredWidth(width);
		if (fixMaxWidth)
		{
			col.setMinWidth(width);
			col.setMaxWidth(width);
		}
		return width;
	}

	static class MyScrollBarUI extends BasicScrollBarUI
	{
		protected JButton createDecreaseButton(int orientation)
		{
			return new BasicArrowButton(orientation, BACKGROUND, BORDER_FOREGROUND.darker(), BORDER_FOREGROUND.darker()
					.darker(), BORDER_FOREGROUND);
		}

		protected JButton createIncreaseButton(int orientation)
		{
			return new BasicArrowButton(orientation, BACKGROUND, BORDER_FOREGROUND.darker(), BORDER_FOREGROUND.darker()
					.darker(), BORDER_FOREGROUND);
		}

		protected void configureScrollBarColors()
		{
			scrollbar.setForeground(BORDER_FOREGROUND);
			scrollbar.setBackground(BACKGROUND);
			thumbHighlightColor = BORDER_FOREGROUND;
			thumbLightShadowColor = BORDER_FOREGROUND.darker();
			thumbDarkShadowColor = BORDER_FOREGROUND.darker().darker();
			thumbColor = BACKGROUND;
			trackColor = BACKGROUND;
			trackHighlightColor = BACKGROUND;
		}
	}

	public static void setViewScrollPaneBorder(JComponent component)
	{
		component.setBorder(new CompoundBorder(new EtchedBorder(BORDER_FOREGROUND, BORDER_FOREGROUND.darker()),
				new EmptyBorder(5, 5, 5, 5)));
	}

	public static JScrollPane createViewScrollpane(JComponent table)
	{
		JScrollPane p = new JScrollPane(table)
		{
			public void updateUI()
			{
				super.updateUI();
				setBorder(new EtchedBorder(BORDER_FOREGROUND, BORDER_FOREGROUND.darker()));
			}
		};
		p.setVerticalScrollBar(new JScrollBar(JScrollBar.VERTICAL)
		{
			public void updateUI()
			{
				setUI(new MyScrollBarUI());
			}
		});
		p.setHorizontalScrollBar(new JScrollBar(JScrollBar.HORIZONTAL)
		{
			public void updateUI()
			{
				setUI(new MyScrollBarUI());
			}
		});
		p.setOpaque(false);
		p.getViewport().setOpaque(false);
		p.setViewportBorder(new EmptyBorder(5, 5, 5, 5));

		return p;
	}

	public static interface PreferredSizeProvider
	{
		public Dimension getPreferredSize();
	}

	public static ClickableLabel createViewButton(String string)
	{
		return createViewButton(string, new Insets(5, 5, 5, 5));
	}

	public static ClickableLabel createViewButton(String string, final Insets insets)
	{
		return createViewButton(string, insets, null);
	}

	public static ClickableLabel createPlusViewButton()
	{
		return createViewButton(null, SimpleImageIcon.plusImageIcon(), new Insets(4, 4, 4, 4), null);
	}

	public static ClickableLabel createMinusViewButton()
	{
		return createViewButton(null, SimpleImageIcon.minusImageIcon(), new Insets(4, 4, 4, 4), null);
	}

	public static ClickableLabel createCrossViewButton()
	{
		return createViewButton(null, SimpleImageIcon.crossImageIcon(), new Insets(4, 4, 4, 4), null);
	}

	public static ClickableLabel createViewButton(String string, final Insets insets, final PreferredSizeProvider prov)
	{
		return createViewButton(string, null, insets, prov);
	}

	public static class ClickableLabel extends JLabel
	{
		public ClickableLabel(String text, ImageIcon icon, int horizontalAlignment)
		{
			super(text, icon, horizontalAlignment);
		}

		Boolean myEnabled = null;

		public void setEnabled(boolean enabled)
		{
			myEnabled = enabled;
			updateUI();
		}

		public void addActionListener(final ActionListener l)
		{
			addMouseListener(new MouseAdapter()
			{
				@Override
				public void mousePressed(MouseEvent e)
				{
					if (myEnabled == null || myEnabled)
						l.actionPerformed(new ActionEvent(ClickableLabel.this, 0, "button clicked"));
				}
			});
		}
	}

	public static ClickableLabel createViewButton(final String string, final SimpleImageIcon icon, final Insets insets,
			final PreferredSizeProvider prov)
	{
		final BorderImageIcon ic;
		if (icon != null)
		{
			icon.setSize((int) (ScreenSetup.INSTANCE.getFontSize() * 0.55));
			ic = new BorderImageIcon(icon, 1, FOREGROUND, insets);
		}
		else
			ic = null;
		ClickableLabel c = new ClickableLabel(string, ic, SwingConstants.LEFT)
		{
			public void updateUI()
			{
				super.updateUI();
				setBackground(BACKGROUND);
				setForeground(FOREGROUND);
				if (ic != null)
				{
					if (myEnabled == null || myEnabled)
					{
						icon.setColor(FOREGROUND);
						ic.setColor(FOREGROUND);
					}
					else
					{
						icon.setColor(FOREGROUND_DISABLED);
						ic.setColor(FOREGROUND_DISABLED);
					}
					icon.setSize((int) (ScreenSetup.INSTANCE.getFontSize() * 0.55));

				}
				else
				{
					setBorder(new CompoundBorder(new LineBorder(FOREGROUND, 1), new EmptyBorder(insets)));
					setFont(getFont().deriveFont((float) ScreenSetup.INSTANCE.getFontSize()));
				}
			}

			@Override
			public Dimension getPreferredSize()
			{
				if (prov == null)
					return super.getPreferredSize();
				else
					return prov.getPreferredSize();
			}
		};
		c.setOpaque(false);
		c.setFocusable(false);
		c.setFont(new JLabel().getFont().deriveFont((float) ScreenSetup.INSTANCE.getFontSize()));
		c.setFocusable(false);
		if (ic != null)
			c.setBorder(new EmptyBorder(0, 0, 1, 1)); // hack: otherwise right and bottom line is missing
		return c;
	}

	public static JButton createViewButton(final ImageIcon blackIcon, final ImageIcon whiteIcon)
	{
		JButton c = new JButton(isBackgroundBlack() ? blackIcon : whiteIcon)
		{
			public void updateUI()
			{
				super.updateUI();
				setForeground(FOREGROUND);
				setBackground(BACKGROUND);
				setBorder(new CompoundBorder(new LineBorder(FOREGROUND, 1), new EmptyBorder(new Insets(1, 1, 1, 1))));
				setIcon(isBackgroundBlack() ? blackIcon : whiteIcon);
			}
		};
		c.setOpaque(false);
		c.setFocusable(false);
		c.setBorder(new CompoundBorder(new LineBorder(FOREGROUND, 1), new EmptyBorder(new Insets(1, 1, 1, 1))));
		return c;
	}

	private static ImageIcon createBasicViewStringImageIcon(String s)
	{
		int size = 10; //dynamically compute size according to orig size does not work well in chartpanel
		if (ScreenSetup.INSTANCE.getFontSize() >= 15)
			size = 12;
		if (ScreenSetup.INSTANCE.getFontSize() >= 21)
			size = 14;
		StringImageIcon sIcon = new StringImageIcon(s, createViewLabel().getFont().deriveFont((float) size), FOREGROUND);
		sIcon.setDrawBorder(true);
		sIcon.setInsets(new Insets(1, 3, 1, 3));
		return sIcon;
	}

	public static Icon createViewStringImageIcon(final String string)
	{
		final ImageIcon icon = new ImageIcon(createBasicViewStringImageIcon(string).getImage());
		updateUIListeners.add(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				icon.setImage(createBasicViewStringImageIcon(string).getImage());
			}
		});
		return icon;
	}

	public static JTextArea createViewTextArea(String text, boolean editable, boolean wrap)
	{
		JTextArea infoTextArea = new JTextArea(text)
		{
			public void updateUI()
			{
				super.updateUI();
				setForeground(FOREGROUND);
				setFont(getFont().deriveFont((float) ScreenSetup.INSTANCE.getFontSize()));
			}
		};
		//infoTextArea.setFont(infoTextArea.getFont().deriveFont(Font.BOLD));
		if (!editable)
		{
			infoTextArea.setBorder(null);
			infoTextArea.setEditable(false);
			infoTextArea.setOpaque(false);
		}
		infoTextArea.setWrapStyleWord(wrap);
		infoTextArea.setLineWrap(wrap);
		infoTextArea.setFocusable(false);
		return infoTextArea;
	}

	static class MySliderUI extends BasicSliderUI
	{
		public MySliderUI(JSlider b)
		{
			super(b);
			b.setBackground(BACKGROUND);
		}

		protected Color getShadowColor()
		{
			return BORDER_FOREGROUND.darker().darker();
		}

		protected Color getHighlightColor()
		{
			return BORDER_FOREGROUND;
		}
	}

	public static JSlider createViewSlider(int min, int max, int value)
	{
		JSlider s = new JSlider(min, max, value)
		{
			public void updateUI()
			{
				super.updateUI();
				setForeground(FOREGROUND);
				setUI(new MySliderUI(this));
			}
		};
		s.setOpaque(false);
		s.setFocusable(false);
		return s;
	}

	public static void main(String args[])
	{
		LaunchCheSMapper.init();
		ComponentFactory.setBackgroundBlack(false);

		JPanel p = new JPanel();
		p.setBackground(BACKGROUND);
		//		p.setPreferredSize(new Dimension(500, 100));
		//		p.add(ComponentFactory.createViewButton("testing"));

		ClickableLabel b = ComponentFactory.createMinusViewButton();
		b.setEnabled(false);
		p.add(b);
		p.add(ComponentFactory.createCrossViewButton());
		p.add(ComponentFactory.createPlusViewButton());

		//		p.add(ComponentFactory.createViewButton("X"), new Insets(0, 5, 0, 5));
		//		p.add(ComponentFactory.createViewButton("X"), new Insets(0, 6, 0, 6));
		//		p.add(ComponentFactory.createViewSlider(0, 100, 33));
		SwingUtil.showInDialog(p, null, null, null, null, 1);

		System.exit(0);
	}

	private static HashMap<String, BufferedImage> chesMapperStringImages = new HashMap<String, BufferedImage>();

	public static BufferedImage getCheSMapperStringImage()
	{
		float fontsize = ScreenSetup.INSTANCE.getFontSize() + 2.0f;
		StringBuffer b = new StringBuffer();
		b.append(fontsize);
		b.append("#");
		b.append(isBackgroundBlack());
		String k = b.toString();
		if (!chesMapperStringImages.containsKey(k))
		{
			StringImageIcon ic = new StringImageIcon("CheS-Mapper", ComponentFactory.createViewLabel("").getFont()
					.deriveFont(Font.BOLD, fontsize), FOREGROUND_DISABLED);
			ic.setBackground(BACKGROUND);
			ic.setInsets(new Insets(3, 3, 3, 3));
			chesMapperStringImages.put(k, (BufferedImage) ic.getImage());

			//			ImageIcon ic2 = ImageLoader.getImage(ImageLoader.Image.ches_mapper_icon);
			//			MultiImageIcon ic3 = new MultiImageIcon(ic, ic2, MultiImageIcon.Layout.horizontal,
			//					MultiImageIcon.Orientation.center, 0);
			//			BufferedImage img = new BufferedImage(ic3.getIconWidth() + 6, ic3.getIconHeight() + 6,
			//					BufferedImage.TYPE_INT_ARGB);
			//			Graphics2D g = img.createGraphics();
			//			//g.setComposite(AlphaComposite.Src);
			//			g.setColor(BACKGROUND);
			//			g.fillRect(0, 0, ic3.getIconWidth() + 6, ic3.getIconHeight() + 6);
			//			ic3.paintIcon(null, g, 3, 3);
			//			g.dispose();
			//			chesMapperStringImages.put(k, img);
		}
		return chesMapperStringImages.get(k);
	}
}
