package org.chesmapper.test;

import java.io.File;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;

import org.chesmapper.map.alg.align3d.ThreeDAligner;
import org.chesmapper.map.alg.build3d.ThreeDBuilder;
import org.chesmapper.map.alg.cluster.DatasetClusterer;
import org.chesmapper.map.alg.embed3d.ThreeDEmbedder;
import org.chesmapper.map.dataInterface.CompoundPropertySet;
import org.chesmapper.map.gui.AlignWizardPanel;
import org.chesmapper.map.gui.Build3DWizardPanel;
import org.chesmapper.map.gui.CheSMapperWizard;
import org.chesmapper.map.gui.ClusterWizardPanel;
import org.chesmapper.map.gui.DatasetWizardPanel;
import org.chesmapper.map.gui.EmbedWizardPanel;
import org.chesmapper.map.gui.FeatureWizardPanel;
import org.chesmapper.map.main.Settings;
import org.chesmapper.map.property.PropertySetCategory;
import org.chesmapper.map.property.PropertySetProvider;
import org.chesmapper.test.util.SwingTestUtil;
import org.chesmapper.view.gui.LaunchCheSMapper;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.mg.javalib.gui.Selector;
import org.mg.javalib.util.ThreadUtil;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class WizardTest
{
	public static String DATA_DIR = "data/";

	static CheSMapperWizard wizard;
	static JButton nextButton;
	static JButton prevButton;
	static JButton closeButton;
	static JButton startButton;

	public WizardTest()
	{
		if (wizard == null)
		{
			//LaunchCheSMapper.init(Locale.US, ScreenSetup.DEFAULT, false, true);
			//LaunchCheSMapper.setExitOnClose(false);
			Thread th = new Thread(new Runnable()
			{
				public void run()
				{
					LaunchCheSMapper.main(new String[] { "--no-properties" });
				}
			});
			th.start();
			while (SwingTestUtil.getOnlyVisibleFrame() == null)
			{
				System.out.println("wait for wizard");
				ThreadUtil.sleep(500);
			}
			ThreadUtil.sleep(1000);
			System.out.println("start wizard test");
			wizard = (CheSMapperWizard) Settings.TOP_LEVEL_FRAME;
			nextButton = SwingTestUtil.getButton(wizard, "Next");
			prevButton = SwingTestUtil.getButton(wizard, "Previous");
			closeButton = SwingTestUtil.getButton(wizard, "Close");
			startButton = SwingTestUtil.getButton(wizard, "Start mapping");
			Assert.assertTrue(closeButton.isEnabled());
			Assert.assertFalse(startButton.isEnabled());
			Assert.assertTrue(wizard.getCurrentPanel() instanceof DatasetWizardPanel);
		}
	}

	private void waitForLoadingDialogToClose()
	{
		waitForLoadingDialogToClose(wizard);
	}

	public static void waitForLoadingDialogToClose(CheSMapperWizard wizard)
	{
		JDialog d = null;
		do
		{
			d = SwingTestUtil.getOnlyVisibleDialog(wizard);
			if (d != null)
				Assert.assertEquals(d.getTitle(), "Loading dataset file");
			System.out.println("wait for waiting dialog to close");
			SwingTestUtil.waitForGUI(50);
		}
		while (d != null);
		SwingTestUtil.waitForGUI(50);
	}

	public static void selectFile(DatasetWizardPanel panel, String file)
	{
		final JButton buttonOpen = SwingTestUtil.getButton(panel, "Open file");
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				buttonOpen.doClick();
			}
		});
		SwingTestUtil.waitForGUI(50);

		JFileChooser fc = SwingTestUtil.getFileChooser();
		fc.setSelectedFile(new File(file));
		SwingTestUtil.waitForGUI(50);
		fc.approveSelection();
	}

	@Test
	public void test1DatasetPanel()
	{
		Assert.assertFalse(wizard.isBlocked());
		DatasetWizardPanel panel = (DatasetWizardPanel) wizard.getCurrentPanel();

		selectFile(panel, "jklsfdjklajklsfdauioes");
		Assert.assertFalse(nextButton.isEnabled());
		waitForLoadingDialogToClose();
		SwingTestUtil.assertErrorDialog(wizard, "ERROR - Loading dataset file", "not found");
		SwingTestUtil.waitWhileBlocked(wizard, "loading non existing file", false);

		selectFile(panel, DATA_DIR + "broken_smiles.csv");
		Assert.assertFalse(nextButton.isEnabled());
		waitForLoadingDialogToClose();
		SwingTestUtil.assertErrorDialog(wizard, "ERROR - Loading dataset file", "illegal smiles");
		SwingTestUtil.waitWhileBlocked(wizard, "loading errornous file", false);

		selectFile(panel, DATA_DIR + "sdf_with_broken_compound.sdf");
		if (new File(DATA_DIR + "sdf_with_broken_compound_cleaned.sdf").exists())
			new File(DATA_DIR + "sdf_with_broken_compound_cleaned.sdf").delete();
		Assert.assertFalse(new File(DATA_DIR + "sdf_with_broken_compound_cleaned.sdf").exists());
		waitForLoadingDialogToClose();
		Assert.assertTrue(wizard.isBlocked());
		SwingTestUtil.waitForGUI(1000);
		Assert.assertTrue(wizard.isBlocked());
		JDialog d = SwingTestUtil.getOnlyVisibleDialog(wizard);
		Assert.assertNotNull(d);
		Assert.assertTrue(d.getTitle().contains("faulty"));
		JButton b = SwingTestUtil.getButton(d, "Yes");
		JButton b2 = SwingTestUtil.getButton(d, "No");
		Assert.assertNotNull(b);
		Assert.assertNotNull(b2);
		SwingTestUtil.clickButton(b);
		Assert.assertTrue(wizard.isBlocked());
		SwingTestUtil.waitForGUI(1000);
		Assert.assertTrue(wizard.isBlocked());
		JDialog d2 = SwingTestUtil.getOnlyVisibleDialog(wizard);
		Assert.assertNotNull(d2);
		Assert.assertTrue(d2.getTitle().contains("Save"));
		JButton b3 = SwingTestUtil.getButton(d2, "Save");
		Assert.assertNotNull(b3);
		SwingTestUtil.clickButton(b3);
		waitForLoadingDialogToClose();
		Assert.assertTrue(nextButton.isEnabled());
		Assert.assertTrue(new File(DATA_DIR + "sdf_with_broken_compound_cleaned.sdf").exists());

		Assert.assertFalse(wizard.isBlocked());
		selectFile(panel, DATA_DIR + "basicTestSet.sdf");
		waitForLoadingDialogToClose();
		Assert.assertFalse(wizard.isBlocked());
		Assert.assertTrue(nextButton.isEnabled());

		nextButton.doClick();
		Assert.assertTrue(wizard.getCurrentPanel() instanceof Build3DWizardPanel);
	}

	@Test
	public void test2Create3DPanel()
	{
		Build3DWizardPanel panel = (Build3DWizardPanel) wizard.getCurrentPanel();
		Assert.assertTrue(prevButton.isEnabled());
		Assert.assertTrue(nextButton.isEnabled());

		JList<?> list = SwingTestUtil.getOnlyList(panel);
		for (int i = 0; i < list.getModel().getSize(); i++)
			Assert.assertEquals(list.getModel().getElementAt(i), ThreeDBuilder.BUILDERS[i]);

		nextButton.doClick();
		Assert.assertTrue(wizard.getCurrentPanel() instanceof FeatureWizardPanel);
	}

	@Test
	public void test3ExtractFeaturesPanel()
	{
		FeatureWizardPanel panel = (FeatureWizardPanel) wizard.getCurrentPanel();
		Assert.assertTrue(prevButton.isEnabled());
		Assert.assertTrue(nextButton.isEnabled());

		@SuppressWarnings("unchecked")
		Selector<PropertySetCategory, CompoundPropertySet> selector = (Selector<PropertySetCategory, CompoundPropertySet>) SwingTestUtil
				.getOnlySelector(panel);
		CompoundPropertySet set[] = selector.getSelected();
		Assert.assertTrue(set.length == 0);

		noFeatures();

		selector.setCategorySelected(PropertySetProvider.INSTANCE.getIntegratedCategory(), true);
		set = selector.getSelected();
		Assert.assertTrue(set.length == 5);

		nextButton.doClick();
		Assert.assertTrue(wizard.getCurrentPanel() instanceof ClusterWizardPanel);
	}

	private void noFeatures()
	{
		nextButton.doClick();
		Assert.assertTrue(wizard.getCurrentPanel() instanceof ClusterWizardPanel);
		ClusterWizardPanel panel = (ClusterWizardPanel) wizard.getCurrentPanel();

		JRadioButton radio = SwingTestUtil.getRadioButton(panel, "No");
		Assert.assertTrue(radio.isShowing());
		Assert.assertTrue(radio.isSelected());
		Assert.assertTrue(nextButton.isEnabled());

		JRadioButton radio2 = SwingTestUtil.getRadioButton(panel, "Yes");
		Assert.assertTrue(radio2.isShowing());
		Assert.assertFalse(radio2.isSelected());
		radio2.doClick();
		Assert.assertFalse(nextButton.isEnabled());

		radio.doClick();
		Assert.assertTrue(radio.isSelected());
		Assert.assertTrue(nextButton.isEnabled());

		nextButton.doClick();
		Assert.assertTrue(wizard.getCurrentPanel() instanceof EmbedWizardPanel);
		EmbedWizardPanel panel2 = (EmbedWizardPanel) wizard.getCurrentPanel();
		radio = SwingTestUtil.getRadioButton(panel2, "No");
		Assert.assertTrue(radio.isShowing());
		Assert.assertFalse(radio.isSelected());
		Assert.assertFalse(nextButton.isEnabled());
		radio.doClick();
		Assert.assertTrue(radio.isSelected());
		Assert.assertTrue(nextButton.isEnabled());
		Assert.assertTrue(startButton.isEnabled());

		prevButton.doClick();
		prevButton.doClick();
		Assert.assertTrue(wizard.getCurrentPanel() instanceof FeatureWizardPanel);
	}

	@Test
	public void test4ClusterPanel()
	{
		ClusterWizardPanel panel = (ClusterWizardPanel) wizard.getCurrentPanel();
		Assert.assertTrue(prevButton.isEnabled());
		Assert.assertTrue(nextButton.isEnabled());

		JButton toggleButton = SwingTestUtil.getButton(panel, "Advanced >>");

		JRadioButton radio = SwingTestUtil.getRadioButton(panel, "Yes");
		Assert.assertTrue(radio.isShowing());
		radio.doClick();
		Assert.assertTrue(radio.isSelected());
		Assert.assertTrue(nextButton.isEnabled());

		toggleButton.doClick();
		Assert.assertTrue(toggleButton.getText().equals("<< Simple"));
		Assert.assertFalse(radio.isShowing());
		JList<?> list = SwingTestUtil.getOnlyList(panel);
		for (int i = 0; i < list.getModel().getSize(); i++)
			Assert.assertEquals(list.getModel().getElementAt(i), DatasetClusterer.CLUSTERERS[i]);

		toggleButton.doClick();
		Assert.assertTrue(toggleButton.getText().equals("Advanced >>"));
		Assert.assertTrue(radio.isShowing());

		nextButton.doClick();
		Assert.assertTrue(wizard.getCurrentPanel() instanceof EmbedWizardPanel);
	}

	@Test
	public void test5EmbedPanel()
	{
		EmbedWizardPanel panel = (EmbedWizardPanel) wizard.getCurrentPanel();
		Assert.assertTrue(prevButton.isEnabled());
		Assert.assertTrue(nextButton.isEnabled());

		JButton toggleButton = SwingTestUtil.getButton(panel, "Advanced >>");

		JRadioButton radio = SwingTestUtil.getRadioButton(panel, "Yes (recommended, applies 'PCA 3D Embedder (WEKA)')");
		Assert.assertTrue(radio.isShowing());
		radio.doClick();
		Assert.assertTrue(radio.isSelected());
		Assert.assertTrue(nextButton.isEnabled());

		toggleButton.doClick();
		Assert.assertTrue(toggleButton.getText().equals("<< Simple"));
		Assert.assertFalse(radio.isShowing());
		JList<?> list = SwingTestUtil.getOnlyList(panel);
		for (int i = 0; i < list.getModel().getSize(); i++)
			Assert.assertEquals(list.getModel().getElementAt(i), ThreeDEmbedder.EMBEDDERS[i]);

		toggleButton.doClick();
		Assert.assertTrue(toggleButton.getText().equals("Advanced >>"));
		Assert.assertTrue(radio.isShowing());

		nextButton.doClick();
		Assert.assertTrue(wizard.getCurrentPanel() instanceof AlignWizardPanel);
	}

	@Test
	public void test6AlignPanel()
	{
		AlignWizardPanel panel = (AlignWizardPanel) wizard.getCurrentPanel();
		Assert.assertTrue(prevButton.isEnabled());
		Assert.assertFalse(nextButton.isEnabled());

		JList<?> list = SwingTestUtil.getOnlyList(panel);
		for (int i = 0; i < list.getModel().getSize(); i++)
			Assert.assertEquals(list.getModel().getElementAt(i), ThreeDAligner.ALIGNER[i]);
	}

	@Test
	public void test7CloseWizard() throws Exception
	{
		closeButton.doClick();
		Assert.assertFalse(wizard.isVisible());
	}
}
