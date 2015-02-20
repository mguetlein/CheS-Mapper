package org.chesmapper.test;

import java.awt.Point;
import java.awt.Robot;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;

import org.chesmapper.map.alg.cluster.WekaClusterer;
import org.chesmapper.map.alg.embed3d.WekaPCA3DEmbedder;
import org.chesmapper.map.main.CheSMapping;
import org.chesmapper.map.main.Settings;
import org.chesmapper.map.property.PropertySetProvider;
import org.chesmapper.map.workflow.MappingWorkflow;
import org.chesmapper.map.workflow.MappingWorkflow.DescriptorSelection;
import org.chesmapper.test.util.SwingTestUtil;
import org.chesmapper.view.gui.LaunchCheSMapper;
import org.chesmapper.view.gui.swing.ComponentFactory.ClickableLabel;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.mg.javalib.gui.BlockableFrame;
import org.mg.javalib.util.ArrayUtil;
import org.mg.javalib.util.ListUtil;
import org.mg.javalib.util.ScreenUtil;
import org.mg.javalib.util.ThreadUtil;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ViewerTest
{
	static BlockableFrame viewer;
	public static String DATA_DIR = "data/";
	static Random random = new Random();

	static String dataset = "basicTestSet.sdf";
	int numClusters = 8;
	int numCompounds = 16;
	int numCompoundsInClusters[] = { 2, 3, 2, 5, 1, 1, 1, 1 };

	static
	{
		if (viewer == null)
		{
			LaunchCheSMapper.init();
			LaunchCheSMapper.setExitOnClose(false);
			Settings.TOP_LEVEL_FRAME_SCREEN = 0;
			Properties props = MappingWorkflow.createMappingWorkflow(DATA_DIR + dataset, DescriptorSelection.select(
					PropertySetProvider.PropertySetShortcut.integrated, "logD,rgyr,HCPSA,fROTB", null, null, null),
					null, WekaClusterer.WEKA_CLUSTERER[0], WekaPCA3DEmbedder.INSTANCE);
			final CheSMapping mapping = MappingWorkflow.createMappingFromMappingWorkflow(props, "");
			mapping.getDatasetFile();
			mapping.doMapping();
			System.out.println("start");
			Thread th = new Thread(new Runnable()
			{
				public void run()
				{
					LaunchCheSMapper.start(mapping);
				}
			});
			th.start();

			//			Thread th = new Thread(new Runnable()
			//			{
			//				public void run()
			//				{
			//					LaunchCheSMapper.main(new String[] { "--no-properties" });
			//				}
			//			});
			//			th.start();
			//			while (SwingTestUtil.getOnlyVisibleFrame() == null)
			//				ThreadUtil.sleep(50);
			//			SwingTestUtil.waitForGUI(250);
			//			Assert.assertNotNull(Settings.TOP_LEVEL_FRAME);
			//			Assert.assertTrue(Settings.TOP_LEVEL_FRAME.isVisible());
			//			CheSMapperWizard wizard = (CheSMapperWizard) Settings.TOP_LEVEL_FRAME;
			//			JButton nextButton = SwingTestUtil.getButton(wizard, "Next");
			//			Assert.assertTrue(wizard.getCurrentPanel() instanceof DatasetWizardPanel);
			//			DatasetWizardPanel panel = (DatasetWizardPanel) wizard.getCurrentPanel();
			//			JTextField textField = SwingTestUtil.getOnlyTextField(panel);
			//			JButton buttonLoad = SwingTestUtil.getButton(panel, "Load Dataset");
			//			textField.setText(DATA_DIR + dataset);
			//			buttonLoad.doClick();
			//			while (wizard.isBlocked())
			//			{
			//				ThreadUtil.sleep(250);
			//				System.out.println("waiting for panel to stop loading");
			//			}
			//			nextButton.doClick();
			//			nextButton.doClick();
			//			FeatureWizardPanel panel2 = (FeatureWizardPanel) wizard.getCurrentPanel();
			//			@SuppressWarnings("unchecked")
			//			Selector<PropertySetCategory, CompoundPropertySet> selector = (Selector<PropertySetCategory, CompoundPropertySet>) SwingTestUtil
			//					.getOnlySelector(panel2);
			//			selector.setCategorySelected(PropertySetProvider.INSTANCE.getIntegratedCategory(), true);
			//			JButton startButton = SwingTestUtil.getButton(wizard, "Start mapping");
			//			startButton.doClick();
			//			while (wizard.isVisible())
			//			{
			//				ThreadUtil.sleep(250);
			//				System.out.println("waiting for wizard to close");
			//			}
			//			while (SwingTestUtil.getOnlyVisibleDialog(null) == null)
			//			{
			//				System.out.println("waiting for loading dialog to show");
			//				ThreadUtil.sleep(250);
			//			}
			SwingTestUtil.waitForGUI(250);
			loadingDialog();

			viewer = (BlockableFrame) Settings.TOP_LEVEL_FRAME;
			Assert.assertTrue("Wrong title: " + viewer.getTitle(), viewer.getTitle().contains(dataset));
			Assert.assertTrue("Wrong title: " + viewer.getTitle(), viewer.getTitle().contains("CheS-Mapper"));
		}
		if (viewer == null)
			System.exit(0);
	}

	private static void loadingDialog()
	{
		Window w[] = Window.getWindows();
		JDialog d = null;
		for (Window window : w)
			if (window instanceof JDialog && window.isShowing())
			{
				Assert.assertNull(d);
				d = (JDialog) window;
				break;
			}
			else
				System.out.println(window);
		Assert.assertNotNull(d);
		while (d.isVisible())
		{
			Assert.assertTrue(d.getTitle().matches(".*[0-9]++%.*"));
			ThreadUtil.sleep(250);
			System.out.println("waiting for loading dialog to close");
		}
		SwingTestUtil.waitForGUI(250);

		while (SwingTestUtil.getOnlyVisibleFrame() == null)
		{
			ThreadUtil.sleep(250);
			System.out.println("waiting for viewer to show up");
		}
		SwingTestUtil.waitForGUI(250);
		Assert.assertNotNull(Settings.TOP_LEVEL_FRAME);
		Assert.assertTrue(Settings.TOP_LEVEL_FRAME.isVisible());
	}

	private JMenuItem getPopupMenuItem(String text)
	{
		try
		{
			Robot r = new Robot(ScreenUtil.getGraphicsDevice(ScreenUtil.getScreen(viewer)));
			Point loc = viewer.getLocationOnScreen();
			r.mouseMove(viewer.getWidth() / 2 + (int) loc.getX(), viewer.getHeight() / 2 + (int) loc.getY());
			SwingTestUtil.waitForGUI(250);
			r.mousePress(InputEvent.BUTTON3_MASK);
			r.mouseRelease(InputEvent.BUTTON3_MASK);
			SwingTestUtil.waitForGUI(250);
			JPopupMenu popup = SwingTestUtil.getOnlyPopupMenu(viewer);
			return SwingTestUtil.getMenuItem(popup, text);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}

	@SuppressWarnings("rawtypes")
	@Test
	public void test1ClusterAndCompoundList()
	{
		List<JList> lists = ListUtil.cast(JList.class, SwingTestUtil.getComponents(viewer, JList.class));
		Assert.assertEquals(lists.size(), 2);

		JList clusterList = lists.get(0);
		JList compoundList = lists.get(1);
		Assert.assertEquals(clusterList.getModel().getSize() - 1, numClusters);

		Assert.assertFalse(compoundList.isShowing());
		SwingTestUtil.clickList(clusterList, 0);
		SwingTestUtil.waitWhileBlocked(viewer, "waiting to switch to single compound selection", false);
		Assert.assertTrue(compoundList.isShowing());
		Assert.assertEquals(compoundList.getModel().getSize(), numCompounds);
		int randomCompound = random.nextInt(Math.min(10, numCompounds));
		SwingTestUtil.clickList(compoundList, randomCompound);
		SwingTestUtil.waitWhileBlocked(viewer, "waiting to zoom into compound");

		int numCompoundsInCluster = compoundList.getModel().getSize();
		ClickableLabel lab = SwingTestUtil.getVisibleClickableLabel(viewer, SwingConstants.TOP);
		SwingTestUtil.clickXButton(lab);
		SwingTestUtil.waitWhileBlocked(viewer, "waiting to zoom out of compound to cluster");
		if (compoundList.isShowing())
		{
			Assert.assertTrue(numCompoundsInCluster > 1);
			SwingTestUtil.clickXButton(lab);
			SwingTestUtil.waitWhileBlocked(viewer, "waiting to zoom out of cluster");
		}
		else
			Assert.assertTrue(numCompoundsInCluster == 1);
		Assert.assertFalse(compoundList.isShowing());
		Assert.assertFalse(lab.isShowing());

		int idx[] = ArrayUtil.indexArray(numClusters);
		ArrayUtil.scramble(idx);
		boolean singleCompoundCluster = false;
		boolean multiCompoundCluster = false;

		for (int j = 0; j < numClusters; j++)
		{
			int i = idx[j];
			if (multiCompoundCluster && singleCompoundCluster)
				break;
			if (numCompoundsInClusters[i] == 1 && singleCompoundCluster)
				continue;
			if (numCompoundsInClusters[i] > 1 && multiCompoundCluster)
				continue;

			Assert.assertFalse(compoundList.isShowing());
			Assert.assertFalse(viewer.isBlocked());
			SwingTestUtil.clickList(clusterList, i + 1);
			SwingTestUtil.waitWhileBlocked(viewer, "waiting to zoom into cluster");
			Assert.assertTrue(compoundList.isShowing());
			Assert.assertEquals(compoundList.getModel().getSize(), numCompoundsInClusters[i]);
			randomCompound = random.nextInt(Math.min(10, numCompoundsInClusters[i]));
			SwingTestUtil.clickList(compoundList, randomCompound);
			SwingTestUtil.waitWhileBlocked(viewer, "waiting to zoom into compound");

			lab = SwingTestUtil.getVisibleClickableLabel(viewer, SwingConstants.TOP);
			SwingTestUtil.clickXButton(lab);
			SwingTestUtil.waitWhileBlocked(viewer, "waiting to zoom out of compound to cluster");
			if (compoundList.isShowing())
			{
				multiCompoundCluster = true;
				Assert.assertTrue(numCompoundsInClusters[i] > 1);
				SwingTestUtil.clickXButton(lab);
				SwingTestUtil.waitWhileBlocked(viewer, "waiting to zoom out of cluster");
			}
			else
			{
				singleCompoundCluster = true;
				Assert.assertTrue(numCompoundsInClusters[i] == 1);
			}
			Assert.assertFalse(compoundList.isShowing());
			Assert.assertFalse(lab.isShowing());
		}
	}

	@SuppressWarnings("rawtypes")
	@Test
	public void test2filterAndDistance()
	{
		List<JList> lists = ListUtil.cast(JList.class, SwingTestUtil.getComponents(viewer, JList.class));
		Assert.assertEquals(lists.size(), 2);
		JList clusterList = lists.get(0);
		JList compoundList = lists.get(1);
		Assert.assertFalse(compoundList.isShowing());
		SwingTestUtil.clickList(clusterList, 0);
		SwingTestUtil.waitWhileBlocked(viewer, "waiting to switch to single compound selection", false);
		Assert.assertTrue(compoundList.isShowing());

		int randomCompound = random.nextInt(Math.min(10, numCompounds));
		SwingTestUtil.clickList(compoundList, randomCompound, true);
		JButton filterButton = SwingTestUtil.getButtonWithName(viewer, "filter-button");
		Assert.assertFalse(filterButton.isVisible());
		List<Integer> selected = new ArrayList<Integer>();
		selected.add(randomCompound);
		int numFiltered = 2 + random.nextInt(5);
		while (selected.size() < numFiltered)
		{
			SwingTestUtil.waitForGUI(100);
			randomCompound = random.nextInt(Math.min(10, numCompounds));
			if (selected.contains(randomCompound))
				continue;
			SwingTestUtil.clickList(compoundList, randomCompound, true);
			selected.add(randomCompound);
		}
		SwingTestUtil.waitForGUI(100);
		filterButton = SwingTestUtil.getButtonWithName(viewer, "filter-button");
		Assert.assertTrue(filterButton.isVisible());
		SwingTestUtil.clickButton(filterButton);
		SwingTestUtil.waitWhileBlocked(viewer, "wait for filter", true);

		Assert.assertEquals(compoundList.getModel().getSize(), numFiltered);
		randomCompound = random.nextInt(numFiltered);
		SwingTestUtil.moveToList(compoundList, randomCompound);

		SwingTestUtil.waitForGUI(100);
		SwingTestUtil.pressKey(viewer, KeyEvent.VK_D, false, true);
		SwingTestUtil.waitWhileBlocked(viewer, "computing distance", false);

		SwingTestUtil.waitForGUI(100);
		JComboBox featureCombo = SwingTestUtil.getOnlyComboBox(viewer, true);
		String s = featureCombo.getSelectedItem().toString();
		Assert.assertTrue(s + " != Euclidean distance to ...", s.startsWith("Euclidean distance to "));

		ClickableLabel lab = SwingTestUtil.getVisibleClickableLabel(viewer, SwingConstants.TOP);
		SwingTestUtil.clickXButton(lab);
		SwingTestUtil.waitWhileBlocked(viewer, "wait for remove filter", false);

		SwingTestUtil.waitForGUI(100);
		Assert.assertEquals(compoundList.getModel().getSize(), numCompounds);
		featureCombo.setSelectedIndex(1);
		s = featureCombo.getSelectedItem().toString();
		Assert.assertTrue(s + " != Cluster", s.equals("Cluster"));

		SwingTestUtil.waitForGUI(100);
		lab = SwingTestUtil.getVisibleClickableLabel(viewer, SwingConstants.TOP);
		SwingTestUtil.clickXButton(lab);
		SwingTestUtil.waitWhileBlocked(viewer, "deslect single compound selection", false);
		SwingTestUtil.waitWhileBlocked(viewer, "computing distance", false);

		SwingTestUtil.waitForGUI(100);
		Assert.assertFalse(compoundList.isShowing());
	}
	//	@Test
	//	public void test2Export()
	//	{
	//		final JMenuItem export = SwingTestUtil.getMenuItem(viewer.getJMenuBar(), "Export cluster/s");
	//		Assert.assertNotNull(export);
	//		SwingUtilities.invokeLater(new Runnable()
	//		{
	//			@Override
	//			public void run()
	//			{
	//				export.getAction().actionPerformed(new ActionEvent(this, -1, ""));
	//			}
	//		});
	//		JDialog dialog = null;
	//		while ((dialog = SwingTestUtil.getOnlyVisibleDialog(viewer)) == null)
	//		{
	//			ThreadUtil.sleep(200);
	//			System.out.println("waiting for cluster dialog");
	//		}
	//		Assert.assertTrue(dialog.getTitle().equals("Export Cluster/s"));
	//		JList list = SwingTestUtil.getOnlyList(dialog);
	//		Assert.assertEquals(list.getModel().getSize(), numClusters);
	//
	//		JCheckBox selectAll = SwingTestUtil.getCheckBox(dialog, "Select all");
	//		selectAll.doClick();
	//		JButton buttonOK = SwingTestUtil.getButton(dialog, "OK");
	//		buttonOK.doClick();
	//		Assert.assertFalse(dialog.isVisible());
	//
	//		dialog = null;
	//		while ((dialog = SwingTestUtil.getOnlyVisibleDialog(viewer)) == null)
	//		{
	//			ThreadUtil.sleep(200);
	//			System.out.println("waiting for filechooser dialog");
	//		}
	//		Assert.assertEquals(dialog.getTitle(), "Save");
	//		JTextField textField = SwingTestUtil.getOnlyTextField(dialog);
	//		String tmpFile = "/tmp/destinationfile.sdf";
	//		Assert.assertFalse(new File(tmpFile).exists());
	//		textField.setText(tmpFile);
	//
	//		final JButton save = SwingTestUtil.getButton(dialog, "Save");
	//		SwingUtilities.invokeLater(new Runnable()
	//		{
	//			@Override
	//			public void run()
	//			{
	//				save.doClick();
	//			}
	//		});
	//		while (!new File(tmpFile).exists())
	//		{
	//			ThreadUtil.sleep(200);
	//			System.out.println("waiting for sdf file");
	//		}
	//		ThreadUtil.sleep(200);
	//		Assert.assertEquals(SDFUtil.countCompounds(tmpFile), numCompounds);
	//		File f = new File(tmpFile);
	//		f.delete();
	//	}

	//	@Test
	//	public void test3Exit()
	//	{
	//		JMenuItem exit = getPopupMenuItem("Exit");
	//		Assert.assertNotNull("Exit popup menu item not found", exit);
	//		exit.doClick();
	//		Assert.assertFalse(true);
	//	}
}
