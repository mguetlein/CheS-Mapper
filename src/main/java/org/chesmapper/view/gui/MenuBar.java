package org.chesmapper.view.gui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.ToolTipManager;

import org.chesmapper.map.main.ScreenSetup;
import org.chesmapper.map.main.Settings;
import org.chesmapper.view.cluster.ClusterController;
import org.chesmapper.view.cluster.Clustering;
import org.chesmapper.view.gui.ViewControler.HighlightMode;

public class MenuBar extends JMenuBar
{
	static interface MyMenuItem
	{
		public boolean isMenu();

		public Action getAction();
	}

	static class DefaultMyMenuItem implements MyMenuItem
	{
		Action action;

		public DefaultMyMenuItem(Action action)
		{
			this.action = action;
		}

		@Override
		public boolean isMenu()
		{
			return false;
		}

		@Override
		public Action getAction()
		{
			return action;
		}
	}

	static class MyMenu implements MyMenuItem
	{
		String name;
		Vector<MyMenuItem> items = new Vector<MyMenuItem>();

		public MyMenu(String name, Action... actions)
		{
			this.name = name;
			for (Action action : actions)
				items.add(new DefaultMyMenuItem(action));
		}

		public MyMenu(String name, MyMenuItem... items)
		{
			this.name = name;
			for (MyMenuItem item : items)
				this.items.add(item);
		}

		public MyMenu(String name, Action actions[], MyMenuItem... items)
		{
			this(name, actions, items, null);
		}

		public MyMenu(String name, Action actions[], MyMenuItem items[], int insertMenusAt[])
		{
			this.name = name;
			if (insertMenusAt == null)
			{
				for (Action action : actions)
					this.items.add(new DefaultMyMenuItem(action));
				for (MyMenuItem item : items)
					this.items.add(item);
			}
			else
			{
				int menuIndex = 0;
				int actionIndex = 0;
				for (int i = 0; i < actions.length + items.length; i++)
				{
					if (menuIndex < insertMenusAt.length && i == insertMenusAt[menuIndex])
					{
						this.items.add(items[menuIndex]);
						menuIndex++;
					}
					else
					{
						this.items.add(new DefaultMyMenuItem(actions[actionIndex]));
						actionIndex++;
					}
				}

			}
		}

		@Override
		public boolean isMenu()
		{
			return true;
		}

		@Override
		public Action getAction()
		{
			return null;
		}
	}

	static class MyMenuBar
	{
		Vector<MyMenu> menus = new Vector<MyMenu>();
		List<Action> actions = new ArrayList<Action>();

		public MyMenuBar(MyMenu... menus)
		{
			for (MyMenu m : menus)
				this.menus.add(m);

			for (MyMenu m : menus)
			{
				for (MyMenuItem i : m.items)
				{
					if (!i.isMenu())
						actions.add(i.getAction());
					else
						for (MyMenuItem ii : ((MyMenu) i).items)
							actions.add(ii.getAction());
				}
			}
		}
	}

	GUIControler guiControler;
	ViewControler viewControler;
	Clustering clustering;

	MyMenuBar menuBar;

	private final static String SPHERE_SETTINGS_MENU = Settings.text("action.highlight-sphere.settings");
	private final static String DATA_MENU = "Dataset properties";

	public MenuBar(GUIControler guiControler, ViewControler viewControler, ClusterController clusterControler,
			Clustering clustering)
	{
		this.guiControler = guiControler;
		this.viewControler = viewControler;
		this.clustering = clustering;

		Actions a = new Actions(guiControler, viewControler, clusterControler, clustering);

		MyMenu dataMenu = new MyMenu(DATA_MENU, a.getDataActions());
		MyMenu fileMenu = new MyMenu("File", a.getFileActions(), new MyMenu[] { dataMenu }, new int[] { 1 });
		MyMenu filterMenu = new MyMenu("Filter", a.getFilterActions());
		MyMenu removeMenu = new MyMenu("Remove", a.getRemoveActions());
		MyMenu exportMenu = new MyMenu("Export", a.getExportActions());
		MyMenu editMenu = new MyMenu("Edit", a.getEditActions(), filterMenu, removeMenu, exportMenu);
		MyMenu viewMenu = new MyMenu("View", a.getViewActions());
		MyMenu highlightSphereMenu = new MyMenu(SPHERE_SETTINGS_MENU, a.getHighlightSphereActions());
		MyMenu highlightMenu = new MyMenu("Highlighting", a.getHighlightActions(), highlightSphereMenu);
		MyMenu helpMenu = new MyMenu("Help", a.getHelpActions());
		menuBar = new MyMenuBar(fileMenu, editMenu, viewMenu, highlightMenu, helpMenu);

		buildMenu();
	}

	private JMenuItem createItemFromAction(Action a)
	{
		JMenuItem m;
		if (a.getValue("is-radio-buttion") != null && (Boolean) a.getValue("is-radio-buttion"))
			m = new JRadioButtonMenuItem(a)
			{
				@Override
				public void updateUI()
				{
					super.updateUI();
					setFont(getFont().deriveFont((float) ScreenSetup.INSTANCE.getFontSize()));
				}
			};
		else if (a.getValue(Action.SELECTED_KEY) instanceof Boolean)
			m = new JCheckBoxMenuItem(a)
			{
				@Override
				public void updateUI()
				{
					super.updateUI();
					setFont(getFont().deriveFont((float) ScreenSetup.INSTANCE.getFontSize()));
				}
			};
		else
			m = new JMenuItem(a)
			{
				@Override
				public void updateUI()
				{
					super.updateUI();
					setFont(getFont().deriveFont((float) ScreenSetup.INSTANCE.getFontSize()));
				}
			};
		m.setToolTipText((String) a.getValue(Actions.TOOLTIP));
		m.setFont(m.getFont().deriveFont((float) ScreenSetup.INSTANCE.getFontSize()));
		ToolTipManager.sharedInstance().setDismissDelay(10000);
		ToolTipManager.sharedInstance().registerComponent(m);
		return m;
	}

	private void createMenuListener(final JMenu m)
	{
		if (m.getText().equals(SPHERE_SETTINGS_MENU))
		{
			m.setEnabled(viewControler.getHighlightMode() == HighlightMode.Spheres);
			viewControler.addViewListener(new PropertyChangeListener()
			{
				@Override
				public void propertyChange(PropertyChangeEvent evt)
				{
					if (evt.getPropertyName().equals(ViewControler.PROPERTY_HIGHLIGHT_MODE_CHANGED))
						m.setEnabled(viewControler.getHighlightMode() == HighlightMode.Spheres);
				}
			});
		}
	}

	private void buildMenu()
	{
		for (MyMenu m : menuBar.menus)
		{
			JMenu menu = new JMenu(m.name)
			{
				@Override
				public void updateUI()
				{
					super.updateUI();
					setFont(getFont().deriveFont((float) ScreenSetup.INSTANCE.getFontSize()));
				}
			};
			for (MyMenuItem i : m.items)
			{
				if (!i.isMenu())
					menu.add(createItemFromAction(i.getAction()));
				else if (i instanceof MyMenu)
				{
					JMenu mm = new JMenu(((MyMenu) i).name)
					{
						@Override
						public void updateUI()
						{
							super.updateUI();
							setFont(getFont().deriveFont((float) ScreenSetup.INSTANCE.getFontSize()));
						}
					};
					ButtonGroup group = new ButtonGroup();
					for (MyMenuItem ii : ((MyMenu) i).items)
					{
						JMenuItem item = createItemFromAction(ii.getAction());
						if (item instanceof JRadioButtonMenuItem)
							group.add(item);
						mm.add(item);
					}
					createMenuListener(mm);
					menu.add(mm);
				}
			}
			add(menu);
		}
	}

	public JPopupMenu getPopup()
	{
		final List<JMenuItem> items = new ArrayList<JMenuItem>();
		JPopupMenu p = new JPopupMenu()
		{
			public void updateUI()
			{
				super.updateUI();
				for (JMenuItem m : items)
					m.updateUI();
			}
		};
		boolean first = true;
		for (MyMenu m : menuBar.menus)
		{
			if (!first)
				p.addSeparator();
			else
				first = false;
			for (MyMenuItem i : m.items)
			{
				if (!i.isMenu())
				{
					JMenuItem item = createItemFromAction(i.getAction());
					items.add(item);
					p.add(item);
				}
				else
				{
					JMenu mm = new JMenu(((MyMenu) i).name)
					{
						@Override
						public void updateUI()
						{
							super.updateUI();
							setFont(getFont().deriveFont((float) ScreenSetup.INSTANCE.getFontSize()));
						}
					};
					items.add(mm);
					mm.setFont(mm.getFont().deriveFont((float) ScreenSetup.INSTANCE.getFontSize()));
					for (MyMenuItem ii : ((MyMenu) i).items)
					{
						JMenuItem item = createItemFromAction(ii.getAction());
						items.add(item);
						mm.add(item);
					}
					createMenuListener(mm);
					p.add(mm);
				}
			}
		}
		return p;
	}

	//	/**
	//	 * somehow the accelerate key registration does not work reliably, do that manually
	//	 * 
	//	 * @param e
	//	 */
	//	public void handleKeyEvent(KeyEvent e)
	//	{
	//		//		Settings.LOGGER.warn("handle key event " + KeyEvent.getKeyText(e.getKeyCode()) + " "
	//		//				+ KeyEvent.getKeyModifiersText(e.getModifiers()) + " " + e.getKeyCode() + " " + e.getModifiers());
	//		for (Action action : menuBar.actions)
	//		{
	//			if (((AbstractAction) action).isEnabled())
	//			{
	//				KeyStroke k = (KeyStroke) action.getValue(Action.ACCELERATOR_KEY);
	//				if (k != null)
	//				{
	//					if (e.getKeyCode() == k.getKeyCode() && ((k.getModifiers() & e.getModifiers()) != 0))
	//					{
	//						//							Settings.LOGGER.warn("perform " + action.toString());
	//						action.actionPerformed(new ActionEvent(this, -1, ""));
	//					}
	//					else
	//					{
	//						//							Settings.LOGGER.warn("no match: " + KeyEvent.getKeyText(k.getKeyCode()) + " "
	//						//									+ KeyEvent.getKeyModifiersText(k.getModifiers()) + " " + k.getKeyCode() + " "
	//						//									+ k.getModifiers());
	//					}
	//				}
	//			}
	//		}
	//	}

}
