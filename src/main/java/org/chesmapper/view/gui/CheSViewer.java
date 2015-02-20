package org.chesmapper.view.gui;

import java.awt.Dimension;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.chesmapper.map.data.ClusteringData;
import org.chesmapper.map.main.ScreenSetup;
import org.chesmapper.map.main.Settings;
import org.chesmapper.view.cluster.ClusterController;
import org.chesmapper.view.cluster.Clustering;
import org.chesmapper.view.gui.swing.ComponentFactory;
import org.jmol.util.DefaultLogger;
import org.jmol.util.Logger;
import org.mg.javalib.gui.BlockableFrame;
import org.mg.javalib.gui.property.IntegerProperty;
import org.mg.javalib.gui.property.Property;
import org.mg.javalib.util.ColorUtil;
import org.mg.javalib.util.ScreenUtil;
import org.mg.javalib.util.SwingUtil;
import org.mg.javalib.util.ThreadUtil;

public class CheSViewer implements GUIControler
{
	static
	{
		Logger.setLogger(new DefaultLogger()
		{
			@Override
			protected void log(PrintStream out, int level, String txt, Throwable e)
			{
				super.log(out, level, txt != null ? "Jmol > " + txt : null, e);
			}
		});
	}

	//	static
	//	{
	//		Settings.LOGGER.warn(JmolConstants.version);
	//	}

	BlockableFrame frame;
	ClusterPanel clusterPanel;
	Clustering clustering;
	MenuBar menuBar;
	boolean undecorated = false;
	Dimension oldSize;
	Point oldLocation;
	boolean messagesBlocked;
	List<PropertyChangeListener> listeners = new ArrayList<PropertyChangeListener>();

	private static CheSViewer instance;

	public static interface PostStartModifier
	{
		public void modify(GUIControler gui, ViewControler view, ClusterController clusterControler,
				Clustering clustering);
	}

	public static void show(ClusteringData clusteringData, final PostStartModifier mod)
	{
		if (!SwingUtilities.isEventDispatchThread())
			throw new IllegalStateException("GUI updates only in event dispatch thread plz");

		//		if (instance == null)
		instance = new CheSViewer(clusteringData);
		//		else
		//		{
		//			instance.clusterPanel.init(clusteringData);
		//			if (!instance.isFullScreen() && !instance.frame.isVisible())
		//				instance.frame.setVisible(true);
		//		}
		if (mod != null)
		{
			Thread th = new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					SwingUtil.waitForAWTEventThread();
					ThreadUtil.sleep(2000);
					mod.modify(instance, instance.clusterPanel.getViewControler(),
							instance.clusterPanel.getClusterControler(), CheSViewer.getClustering());
				}
			});
			th.start();
		}
	}

	private CheSViewer(ClusteringData clusteredDataset)
	{
		oldSize = ScreenSetup.INSTANCE.getViewerSize();
		if (oldSize == null)
			throw new Error();
		oldLocation = null;

		clusterPanel = new ClusterPanel(this);

		//		oldSize = new Dimension(1024, 768);
		//oldLocation = new Point(0, 0);

		clusterPanel.init(clusteredDataset);
		clustering = clusterPanel.getClustering();
		menuBar = new MenuBar(this, clusterPanel.getViewControler(), clusterPanel.getClusterControler(), clustering);

		KeyboardFocusManager kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
		kfm.addKeyEventDispatcher(new KeyEventDispatcher()
		{
			@Override
			public boolean dispatchKeyEvent(KeyEvent e)
			{
				KeyStroke keyStroke = KeyStroke.getKeyStrokeForEvent(e);
				return Actions.instance().performAction(e.getSource(), keyStroke, !frame.isUndecorated());
			}
		});

		for (IntegerProperty integerProperty : ComponentSize.MAX_WIDTH)
		{
			integerProperty.addPropertyChangeListener(new PropertyChangeListener()
			{
				@Override
				public void propertyChange(PropertyChangeEvent evt)
				{
					fireEvent(PROPERTY_VIEWER_SIZE_CHANGED);
				}
			});
		}

		setFullScreen(false, true);
	}

	@Override
	public void setFullScreen(boolean b)
	{
		setFullScreen(b, false);
	}

	private void setFullScreen(boolean b, boolean force)
	{
		if (force || frame.isUndecorated() != b)
		{
			if (frame != null)
			{
				frame.dispose();
				// f.removeAll();
				frame.setVisible(false);
			}
			if (b)
			{
				oldSize = frame.getSize();
				oldLocation = frame.getLocation();
				show(b, ScreenSetup.INSTANCE.getFullScreenSize(), new Point(0, 0));
			}
			else
			{
				show(b, oldSize, oldLocation);
			}
			fireEvent(PROPERTY_FULLSCREEN_CHANGED);
		}
	}

	@Override
	public boolean isFullScreen()
	{
		if (frame == null)
			return false;
		else
			return frame.isUndecorated();
	}

	public void updateTitle(Clustering c)
	{
		if (frame != null)
			frame.setTitle((c.getName() == null ? "" : (c.getName() + " - ")) + Settings.TITLE + " ("
					+ Settings.VERSION_STRING + ")");
	}

	private void show(boolean undecorated, Dimension size, Point location)
	{
		//		Settings.LOGGER.info("showing - size: " + size);

		if (clustering == null)
			throw new Error("clustering is null");

		boolean showMsg = true;
		if (frame == null)
		{
			frame = new BlockableFrame(true);
			Settings.TOP_LEVEL_FRAME = frame;
			frame.addComponentListener(new ComponentAdapter()
			{
				public void componentMoved(ComponentEvent e)
				{
					Settings.TOP_LEVEL_FRAME_SCREEN = ScreenUtil.getScreen(frame);
				}

				public void componentResized(ComponentEvent e)
				{
					ComponentFactory.updateComponents();
					fireEvent(PROPERTY_VIEWER_SIZE_CHANGED);
				}
			});
			frame.getContentPane().add(clusterPanel);
			frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
			frame.addWindowListener(new WindowAdapter()
			{
				@Override
				public void windowClosing(WindowEvent e)
				{
					super.windowClosing(e);
					LaunchCheSMapper.exit(frame);
				};
			});
			frame.setIconImage(Settings.CHES_MAPPER_IMAGE.getImage());
			showMsg = false;
		}

		updateTitle(clustering);
		frame.setUndecorated(undecorated);
		frame.setJMenuBar(undecorated ? null : menuBar);

		if (undecorated)
		{
			frame.setSize(size);//the fullscreen size was just derived from the current screen (if not overriden manually)
			ScreenUtil.getGraphicsDevice(Settings.TOP_LEVEL_FRAME_SCREEN).setFullScreenWindow(frame);
			frame.setResizable(false);
			frame.setAlwaysOnTop(true);
		}
		else
		{
			frame.setSize(size);
			if (location == null)
				ScreenSetup.INSTANCE.centerOnScreen(frame);
			else
				frame.setLocation(location);
			frame.setResizable(true);
			frame.setAlwaysOnTop(false);
		}

		frame.setVisible(true);
		Settings.TOP_LEVEL_FRAME_SCREEN = ScreenUtil.getScreen(frame);
		if (showMsg)
		{
			if (frame.isUndecorated())
				showMessage("Press 'ESCAPE' to leave fullscreen mode");
			else
				showMessage("Press 'ALT+ENTER' for fullscreen mode");
		}
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				frame.toFront();
			}
		});
	}

	public void showMessage(String msg)
	{
		if (!messagesBlocked && clusterPanel != null && clusterPanel.isVisible())
			clusterPanel.showMessage(msg);
		Settings.LOGGER.debug(msg);
	}

	@Override
	public JPopupMenu getPopup()
	{
		return menuBar.getPopup();
	}

	//	@Override
	//	public void handleKeyEvent(KeyEvent e)
	//	{
	//		menuBar.handleKeyEvent(e);
	//	}

	@Override
	public void addPropertyChangeListener(PropertyChangeListener l)
	{
		listeners.add(l);
	}

	private void fireEvent(String prop)
	{
		for (PropertyChangeListener ll : listeners)
			ll.propertyChange(new PropertyChangeEvent(this, prop, false, true));
	}

	@Override
	public void block(String blocker)
	{
		if (frame != null)
			frame.block(blocker);
	}

	@Override
	public boolean isBlocked()
	{
		return frame.isBlocked();
	}

	@Override
	public boolean isVisible()
	{
		return frame != null && frame.isVisible();
	}

	@Override
	public void unblock(String blocker)
	{
		if (frame != null)
		{
			frame.unblock(blocker);
		}
	}

	@Override
	public int getComponentMaxWidth(double pct)
	{
		int w;
		if (frame != null)
			w = frame.getWidth();
		else
			w = oldSize.width;
		return getComponentMaxSize(w, pct);
	}

	@Override
	public int getComponentMaxHeight(double pct)
	{
		int h;
		if (frame != null)
			h = frame.getHeight();
		else
			h = oldSize.height;
		return getComponentMaxSize(h, pct);
	}

	private int getComponentMaxSize(int size, double pct)
	{
		int s = (int) (size * pct);
		if (ScreenSetup.INSTANCE.isFontSizeLarge())
			s = (int) Math.min(size, s * 1.1); // allow a bit larger components when font size is large
		return s;
	}

	public static JFrame getFrame()
	{
		if (instance == null)
			return null;
		else
			return instance.frame;
	}

	public static Clustering getClustering()
	{
		if (instance == null)
			return null;
		else
			return instance.clustering;
	}

	@Override
	public void blockMessages()
	{
		messagesBlocked = true;
	}

	@Override
	public void unblockMessages()
	{
		messagesBlocked = false;
	}

	String selectedString;

	@Override
	public String getSelectedString()
	{
		return selectedString;
	}

	@Override
	public void setSelectedString(String s)
	{
		selectedString = s;
	}

	HashMap<Property, JComponent> propComps = new HashMap<Property, JComponent>();
	HashMap<Property, Boolean> propHideComp = new HashMap<Property, Boolean>();
	boolean accentuate = false;

	@Override
	public void registerSizeComponent(Property p, JComponent c)
	{
		propComps.put(p, c);
	}

	@Override
	public boolean isAccentuateComponents()
	{
		return accentuate;
	}

	@Override
	public void setAccentuateSizeComponents(boolean b)
	{
		for (Property p : propComps.keySet())
		{
			JComponent c = propComps.get(p);
			if (b == true)
			{
				accentuate = true;
				if (!c.isVisible())
				{
					c.setVisible(true);
					propHideComp.put(p, true);
				}
				else
					propHideComp.put(p, false);
				//				SwingUtil.setDebugBorder(c, p.getHighlightColor());
				c.setOpaque(true);
				c.setBackground(ColorUtil.transparent(p.getHighlightColor(), 100));
			}
			else
			{
				accentuate = false;
				//				SwingUtil.removeDebugBorder(c);
				c.setOpaque(false);
				if (propHideComp.get(p))
				{
					c.setVisible(false);
					propHideComp.put(p, false);
				}

			}
		}
		fireEvent(PROPERTY_VIEWER_SIZE_CHANGED);
	}
}