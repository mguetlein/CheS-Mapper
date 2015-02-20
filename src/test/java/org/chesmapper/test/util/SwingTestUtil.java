package org.chesmapper.test.util;

import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.text.JTextComponent;

import org.chesmapper.view.gui.swing.ComponentFactory.ClickableLabel;
import org.junit.Assert;
import org.mg.javalib.gui.BlockableFrame;
import org.mg.javalib.gui.Selector;
import org.mg.javalib.util.ScreenUtil;
import org.mg.javalib.util.SwingUtil;
import org.mg.javalib.util.ThreadUtil;

public class SwingTestUtil
{
	public static void clickButton(final AbstractButton b)
	{
		SwingUtil.invokeAndWait(new Runnable()
		{
			@Override
			public void run()
			{
				b.doClick();
			}
		});
	}

	public static void setText(final JTextField tf, final String text)
	{
		SwingUtil.invokeAndWait(new Runnable()
		{
			@Override
			public void run()
			{
				tf.setText(text);
			}
		});
	}

	public static JFrame getOnlyVisibleFrame()
	{
		JFrame f = null;
		for (Window w : Window.getWindows())
			if (w instanceof JFrame && w.isShowing())
			{
				if (f != null)
					throw new IllegalStateException("num frames > 1");
				f = (JFrame) w;
			}
		return f;
	}

	public static JDialog getOnlyVisibleDialog(Window owner)
	{
		JDialog d = null;
		for (Window w : Window.getWindows())
		{
			//			System.out.println("found " + w + " " + w.getOwner());
			if (w instanceof JDialog && w.isShowing() && ((JDialog) w).getOwner() == owner)
			{
				if (d != null)
					throw new IllegalStateException("num dialogs > 1");
				d = (JDialog) w;
			}
		}
		return d;
	}

	public static JDialog getVisibleDialog(Window owner, String title)
	{
		for (Window w : Window.getWindows())
			if (w instanceof JDialog && w.isShowing() && ((JDialog) w).getOwner() == owner
					&& ((JDialog) w).getTitle().equals(title))
				return (JDialog) w;
		return null;
	}

	public static void assertErrorDialog(Window owner, String titleMatch, String contentMatch)
	{
		SwingTestUtil.waitForGUI(250);
		JDialog d = getOnlyVisibleDialog(owner);
		Assert.assertNotNull("no visible dialog found with owner " + owner, d);
		Assert.assertTrue("title is '" + d.getTitle() + "', does not match '" + titleMatch + "'",
				d.getTitle().matches("(?i).*" + titleMatch + ".*"));
		//		String content = getAllText(d);
		//		String contentMatchRegexp = "(?i).*" + contentMatch + ".*";
		//		boolean b = content.matches(contentMatchRegexp);
		Assert.assertTrue("content is '" + getAllText(d) + "', does not contain '" + contentMatch + "'", getAllText(d)
				.toLowerCase().contains(contentMatch.toLowerCase()));
		JButton close = getButton(d, "Close");
		Assert.assertNotNull(close);
		SwingTestUtil.clickButton(close);
		Assert.assertFalse(d.isShowing());
		SwingTestUtil.waitForGUI(250);
	}

	public static void waitForGUI(long milliseconds)
	{
		SwingUtil.waitForAWTEventThread();
		ThreadUtil.sleep(milliseconds);
		SwingUtil.waitForAWTEventThread();
	}

	public static JMenuItem getMenuItem(JMenuBar menu, String text)
	{
		for (int i = 0; i < menu.getMenuCount(); i++)
		{
			JMenuItem m = getMenuItem(menu.getMenu(i), text);
			if (m != null)
				return m;
		}
		return null;
	}

	private static JMenuItem getMenuItem(JMenu menu, String text)
	{
		for (Component c : menu.getMenuComponents())
		{
			if (c instanceof JMenu)
			{
				JMenuItem m = getMenuItem((JMenu) c, text);
				if (m != null)
					return m;
			}
			else if (c instanceof JMenuItem && ((JMenuItem) c).getText().equals(text))
			{
				return (JMenuItem) c;
			}
		}
		return null;
	}

	public static JButton getButton(Container owner, String text)
	{
		return (JButton) getAbstractButton(owner, text);
	}

	public static JCheckBox getCheckBox(Container owner, String text)
	{
		return (JCheckBox) getAbstractButton(owner, text);
	}

	public static JRadioButton getRadioButton(Container owner, String text)
	{
		return (JRadioButton) getAbstractButton(owner, text);
	}

	public static JMenuItem getMenuItem(Container owner, String text)
	{
		return (JMenuItem) getAbstractButton(owner, text);
	}

	private static AbstractButton getAbstractButton(Container owner, String text)
	{
		for (Component c : owner.getComponents())
		{
			if (c instanceof AbstractButton && ((AbstractButton) c).getText() != null
					&& ((AbstractButton) c).getText().equals(text))
				return (AbstractButton) c;
			else if (c instanceof JComponent)
			{
				AbstractButton b = getAbstractButton((JComponent) c, text);
				if (b != null)
					return b;
			}
		}
		return null;
	}

	public static JTextField getTextFieldWithName(Container owner, String name)
	{
		return (JTextField) getComponentWithName(owner, name, JTextField.class);
	}

	public static JButton getButtonWithName(Container owner, String name)
	{
		return (JButton) getComponentWithName(owner, name, JButton.class);
	}

	private static JComponent getComponentWithName(Container owner, String name, Class<?> clazz)
	{
		for (Component c : owner.getComponents())
		{
			if (clazz.isInstance(c) && ((JComponent) c).getName() != null && ((JComponent) c).getName().equals(name))
				return (JComponent) c;
			else if (c instanceof JComponent)
			{
				JComponent b = getComponentWithName((JComponent) c, name, clazz);
				if (b != null)
					return b;
			}
		}
		return null;
	}

	public static String getAllText(Container owner)
	{
		String content = "";
		String sep = "";//"\n"
		for (JComponent comp : getComponents(owner, JTextComponent.class))
			content += ((JTextComponent) comp).getText() + sep;
		for (JComponent comp : getComponents(owner, JLabel.class))
			content += ((JLabel) comp).getText() + sep;
		for (JComponent comp : getComponents(owner, AbstractButton.class))
			content += ((AbstractButton) comp).getText() + sep;
		return content;
	}

	public static JTextField getOnlyTextField(Container owner)
	{
		return (JTextField) getOnlyComponent(owner, JTextField.class);
	}

	public static JList<?> getOnlyList(Container owner)
	{
		return (JList<?>) getOnlyComponent(owner, JList.class);
	}

	public static JButton getOnlyButton(Container owner)
	{
		return (JButton) getOnlyComponent(owner, JButton.class);
	}

	public static JPopupMenu getOnlyPopupMenu(Container owner)
	{
		return (JPopupMenu) getOnlyComponent(owner, JPopupMenu.class);
	}

	public static Selector<?, ?> getOnlySelector(Container owner)
	{
		return (Selector<?, ?>) getOnlyComponent(owner, Selector.class);
	}

	public static JSpinner getOnlySpinner(Container owner)
	{
		return (JSpinner) getOnlyComponent(owner, JSpinner.class);
	}

	public static JCheckBox getOnlyCheckBox(Container owner)
	{
		return (JCheckBox) getOnlyComponent(owner, JCheckBox.class);
	}

	public static JComboBox<?> getOnlyComboBox(Container owner)
	{
		return getOnlyComboBox(owner, false);
	}

	public static JComboBox<?> getOnlyComboBox(Container owner, boolean onlyVisible)
	{
		return (JComboBox<?>) getOnlyComponent(owner, JComboBox.class, onlyVisible);
	}

	public static ClickableLabel getVisibleClickableLabel(Container owner, final int swingConstantOrientation)
	{
		List<JComponent> list = getComponents(owner, ClickableLabel.class, true);
		if (list.size() == 0)
			return null;
		Collections.sort(list, new Comparator<JComponent>()
		{
			@Override
			public int compare(JComponent o1, JComponent o2)
			{
				if (swingConstantOrientation == SwingConstants.TOP)
					return o1.getLocationOnScreen().y - o2.getLocationOnScreen().y;
				else
					throw new IllegalStateException();
			}
		});
		return (ClickableLabel) list.get(0);
	}

	private static JComponent getOnlyComponent(Container owner, Class<?> clazz)
	{
		return getOnlyComponent(owner, clazz, false);
	}

	private static JComponent getOnlyComponent(Container owner, Class<?> clazz, boolean onlyVisible)
	{
		List<JComponent> list = getComponents(owner, clazz, onlyVisible);
		if (list.size() != 1)
			throw new IllegalStateException("num " + (onlyVisible ? "visible " : "") + "compounds found " + list.size());
		return list.get(0);
	}

	public static List<JComponent> getComponents(Container owner, Class<?> clazz)
	{
		return getComponents(owner, clazz, false);
	}

	public static List<JComponent> getComponents(Container owner, Class<?> clazz, boolean onlyVisible)
	{
		List<JComponent> list = new ArrayList<JComponent>();
		for (Component c : owner.getComponents())
		{
			if (clazz.isInstance(c) && (!onlyVisible || c.isShowing()))
				list.add((JComponent) c);
			else if (c instanceof JComponent)
			{
				for (JComponent b : getComponents((JComponent) c, clazz, onlyVisible))
					list.add(b);
			}
		}
		return list;
	}

	public static void clickList(JList<?> list, int index)
	{
		clickList(list, index, false);
	}

	public static void clickList(JList<?> list, int index, boolean ctrlPressed)
	{
		try
		{
			Robot r = new Robot(ScreenUtil.getGraphicsDevice(ScreenUtil.getScreen((Window) list.getTopLevelAncestor())));
			Point p = list.getLocationOnScreen();
			Point p2 = list.indexToLocation(index);
			r.mouseMove(p.x + p2.x + 5, p.y + p2.y + 5);
			waitForGUI(250);
			if (ctrlPressed)
				r.keyPress(KeyEvent.VK_CONTROL);
			r.mousePress(InputEvent.BUTTON1_MASK);
			r.mouseRelease(InputEvent.BUTTON1_MASK);
			if (ctrlPressed)
				r.keyRelease(KeyEvent.VK_CONTROL);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public static void moveToList(JList<?> list, int index)
	{
		try
		{
			Robot r = new Robot(ScreenUtil.getGraphicsDevice(ScreenUtil.getScreen((Window) list.getTopLevelAncestor())));
			Point p = list.getLocationOnScreen();
			Point p2 = list.indexToLocation(index);
			r.mouseMove(p.x + p2.x + 5, p.y + p2.y + 5);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public static void pressKey(Frame owner, int key, boolean ctrlDown, boolean altDown)
	{
		try
		{
			Robot r = new Robot(ScreenUtil.getGraphicsDevice(ScreenUtil.getScreen(owner)));
			if (ctrlDown)
				r.keyPress(KeyEvent.VK_CONTROL);
			if (altDown)
				r.keyPress(KeyEvent.VK_ALT);
			r.keyPress(key);
			r.keyRelease(key);
			if (altDown)
				r.keyRelease(KeyEvent.VK_ALT);
			if (ctrlDown)
				r.keyRelease(KeyEvent.VK_CONTROL);

		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public static void clickXButton(JComponent lab)
	{
		try
		{
			Robot r = new Robot(ScreenUtil.getGraphicsDevice(ScreenUtil.getScreen((Window) lab.getTopLevelAncestor())));
			Point p = lab.getLocationOnScreen();
			r.mouseMove(p.x + 5, p.y + 5);
			waitForGUI(250);
			r.mousePress(InputEvent.BUTTON1_MASK);
			r.mouseRelease(InputEvent.BUTTON1_MASK);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public static void main(String args[])
	{
		final JList<String> l = new JList<String>(
				"alsjkf asölkfjaölskfj ölaskjflaskdjf iosdfosdf sdk slfdjk aölskfj lskfdj sldkfj".split(" "));
		JScrollPane scroll = new JScrollPane(l);
		SwingUtil.showInDialog(scroll, "test", null, new Runnable()
		{

			@Override
			public void run()
			{
				//				ThreadUtil.sleep(1000);
				SwingTestUtil.clickList(l, 6);

			}
		});

		//		JButton b = new JButton("b");
		//		b.setName("b");
		//		final JPanel p = new JPanel();
		//		p.add(b);
		//		SwingUtil.showInDialog(p, "test", null, new Runnable()
		//		{
		//
		//			@Override
		//			public void run()
		//			{
		//				System.out.println(SwingTestUtil.getButton(p.getTopLevelAncestor(), "b"));
		//
		//			}
		//		});
		System.exit(0);
	}

	public static void waitWhileBlocked(BlockableFrame viewer, String msg)
	{
		waitWhileBlocked(viewer, msg, true);
	}

	public static void waitWhileBlocked(BlockableFrame viewer, String msg, boolean checkBlock)
	{
		SwingTestUtil.waitForGUI(50);
		//SwingUtil.waitForAWTEventThread();
		if (checkBlock)
			Assert.assertTrue(viewer.isBlocked());
		while (viewer.isBlocked())
		{
			SwingTestUtil.waitForGUI(50);
			System.out.println(msg);
		}
	}

	public static JFileChooser getFileChooser()
	{
		for (Window w : Window.getWindows())
			if (w instanceof JDialog && w.isShowing())
				if (((JDialog) w).getContentPane().getComponent(0) instanceof JFileChooser)
					return (JFileChooser) ((JDialog) w).getContentPane().getComponent(0);
		throw new IllegalStateException("no file chooser found!");
	}

}
