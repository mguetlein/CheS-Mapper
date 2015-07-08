package org.chesmapper.test.util;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;

import org.chesmapper.map.alg.Algorithm;
import org.chesmapper.map.alg.align3d.NoAligner;
import org.chesmapper.map.alg.align3d.ThreeDAligner;
import org.chesmapper.map.alg.build3d.ThreeDBuilder;
import org.chesmapper.map.alg.build3d.UseOrigStructures;
import org.chesmapper.map.alg.cluster.DatasetClusterer;
import org.chesmapper.map.alg.embed3d.ThreeDEmbedder;
import org.chesmapper.map.data.DatasetFile;
import org.chesmapper.map.data.fragments.MatchEngine;
import org.chesmapper.map.dataInterface.CompoundPropertySet;
import org.chesmapper.map.gui.CheSMapperWizard;
import org.chesmapper.map.gui.ClusterWizardPanel;
import org.chesmapper.map.gui.DatasetWizardPanel;
import org.chesmapper.map.gui.EmbedWizardPanel;
import org.chesmapper.map.gui.wizard.AbstractWizardPanel;
import org.chesmapper.map.main.CheSMapping;
import org.chesmapper.map.main.PropHandler;
import org.chesmapper.map.property.PropertySetCategory;
import org.chesmapper.map.property.PropertySetProvider;
import org.chesmapper.map.workflow.ClustererProvider;
import org.chesmapper.map.workflow.DatasetLoader;
import org.chesmapper.map.workflow.EmbedderProvider;
import org.chesmapper.map.workflow.MappingWorkflow;
import org.chesmapper.map.workflow.MappingWorkflow.DescriptorSelection;
import org.chesmapper.map.workflow.MappingWorkflow.FragmentSettings;
import org.chesmapper.map.workflow.SimpleViewAlgorithmProvider;
import org.chesmapper.test.WizardTest;
import org.junit.Assert;
import org.mg.javalib.gui.LinkButton;
import org.mg.javalib.gui.Selector;
import org.mg.javalib.gui.WizardPanel;
import org.mg.javalib.gui.property.Property;
import org.mg.javalib.gui.property.PropertyComponent;
import org.mg.javalib.util.DoubleKeyHashMap;
import org.mg.javalib.util.FileUtil;
import org.mg.javalib.util.ListUtil;
import org.mg.javalib.util.SwingUtil;
import org.mg.javalib.util.ThreadUtil;

public class MappingCreator
{
	public static Set<String> tmpfiles = new HashSet<String>();

	public enum Mode
	{
		StoreAndLoadProps, DirectlyUseAlgorithms, RestartWizardWithProps, ConfigureWizard;
	}

	public static class IllegalSettingException extends IllegalArgumentException
	{
		public IllegalSettingException(String msg)
		{
			super(msg);
		}
	}

	public static CheSMapping create(Mode mode, String dataset, DescriptorSelection feats, FragmentSettings frags,
			DatasetClusterer clust, ThreeDEmbedder emb, DoubleKeyHashMap<Algorithm, String, Object> algorithmProps,
			String mappingKey)
	{
		return create(mode, dataset, feats, frags, clust, emb, algorithmProps, mappingKey, null, null);
	}

	public static CheSMapping create(Mode mode, String dataset, DescriptorSelection feats, FragmentSettings frags,
			DatasetClusterer clust, ThreeDEmbedder emb, DoubleKeyHashMap<Algorithm, String, Object> algorithmProps,
			String mappingKey, ThreeDBuilder builder, ThreeDAligner align)
	{
		if (mode == Mode.StoreAndLoadProps)
		{
			if (builder != null || align != null)
				throw new IllegalStateException("not yet implemented");
			applyAlgorithmProps(clust, emb, algorithmProps);
			return storeAndLoadProps(dataset, feats, frags, clust, emb, mappingKey);
		}
		if (mode == Mode.DirectlyUseAlgorithms)
		{
			applyAlgorithmProps(clust, emb, algorithmProps);
			return directlyUseAlgorithms(dataset, feats, frags, clust, emb, builder, align);
		}
		if (mode == Mode.RestartWizardWithProps)
			return restartWizardWithProps(feats, mappingKey);
		if (mode == Mode.ConfigureWizard)
		{
			if (builder != null || align != null)
				throw new IllegalStateException("not yet implemented");
			return configureWizard(dataset, feats, frags, clust, emb, algorithmProps);
		}
		else
			throw new IllegalArgumentException();
	}

	private static void applyAlgorithmProps(DatasetClusterer clust, ThreeDEmbedder emb,
			DoubleKeyHashMap<Algorithm, String, Object> algorithmProps)
	{
		if (algorithmProps != null)
			for (Algorithm alg : algorithmProps.keySet1())
				for (Algorithm alg2 : new Algorithm[] { clust, emb })
					if (alg == alg2)
					{
						for (String prop : algorithmProps.keySet2(alg))
						{
							boolean found = false;
							for (final Property p : alg.getProperties())
							{
								if (p.getName().equals(prop))
								{
									found = true;
									System.err.println("setting " + p.getName() + " of " + alg.getName() + " from "
											+ p.getValue() + " to " + algorithmProps.get(alg, p.getName()));
									Assert.assertNotEquals(p.getValue(), algorithmProps.get(alg, p.getName()));
									p.setValue(algorithmProps.get(alg, p.getName()));

								}
							}
							if (!found)
								throw new IllegalStateException();
						}
					}
	}

	private static void switchAdvancedSimple(WizardPanel p, boolean simple)
	{
		JButton toAdvanced = SwingTestUtil.getButton(p, "Advanced >>");
		JButton toSimple = SwingTestUtil.getButton(p, "<< Simple");
		JButton press = null;
		if (simple)
			press = toSimple;
		else
			press = toAdvanced;
		if (press != null)
			SwingTestUtil.clickButton(press);
	}

	private static CheSMapping configureWizard(String dataset, DescriptorSelection feats, final FragmentSettings frags,
			DatasetClusterer clust, ThreeDEmbedder emb, final DoubleKeyHashMap<Algorithm, String, Object> algorithmProps)
	{
		//		Assert.assertNull(Settings.TOP_LEVEL_FRAsME);
		for (PropertySetProvider.PropertySetShortcut feat : feats.getShortcuts())
			if (feat != PropertySetProvider.PropertySetShortcut.integrated)
				for (CompoundPropertySet set : PropertySetProvider.INSTANCE.getDescriptorSets(null, feat))
					if (set.isHiddenFromGUI())
						throw new IllegalSettingException("skipping wizard test - cannot use gui for hidden feature: "
								+ set);

		if (new File(PropHandler.getPropertiesFile()).exists())
		{
			new File(PropHandler.getPropertiesFile()).delete();
			Assert.assertFalse(new File(PropHandler.getPropertiesFile()).exists());
			PropHandler.forceReload();
		}

		final CheSMapperWizard wwd = new CheSMapperWizard(null);
		while (SwingTestUtil.getOnlyVisibleFrame() == null)
		{
			System.out.println("wait for wizard to show");
			ThreadUtil.sleep(50);
		}
		WizardTest.waitForLoadingDialogToClose(wwd);

		//		ThreadUtil.sleep(20000);
		System.out.println("start wizard test");
		JButton startButton = SwingTestUtil.getButton(wwd, "Start mapping");
		JButton nextButton = SwingTestUtil.getButton(wwd, "Next");

		// dataset
		WizardTest.selectFile((DatasetWizardPanel) wwd.getCurrentPanel(), dataset);
		SwingTestUtil.waitWhileBlocked(wwd, "wait while blocked, loading dataset", false);
		DatasetFile datasetFile = ((DatasetWizardPanel) wwd.getCurrentPanel()).getDatasetFile();
		Assert.assertNotNull(datasetFile);
		while (!nextButton.isEnabled())
		{
			SwingTestUtil.waitForGUI(50);
			System.out.println("wait fo next button");
		}
		nextButton.doClick();

		//3d
		Assert.assertTrue(nextButton.isEnabled());
		nextButton.doClick();

		//features
		@SuppressWarnings("unchecked")
		final Selector<PropertySetCategory, CompoundPropertySet> selector = (Selector<PropertySetCategory, CompoundPropertySet>) SwingTestUtil
				.getOnlySelector(wwd.getCurrentPanel());
		selector.clearSelection(true);
		Assert.assertTrue(selector.getSelected() == null || selector.getSelected().length == 0);
		selector.setSelected(ListUtil.toArray(CompoundPropertySet.class, feats.getFilteredFeatures(datasetFile)), true);
		if (frags != null)
		{
			//			final JList<?> l = SwingTestUtil.getOnlyList(selector);
			SwingUtil.invokeAndWait(new Runnable()
			{
				public void run()
				{
					selector.highlight(PropertySetProvider.INSTANCE.getStructuralFragmentCategory());
					//l.setSelectedIndex(0);
				}
			});
			LinkButton link = null;
			List<JComponent> li = SwingTestUtil.getComponents(wwd.getCurrentPanel(), LinkButton.class);
			for (JComponent c : li)
				if (((LinkButton) c).getText().matches(".*Settings for fragments.*"))
				{
					link = (LinkButton) c;
					break;
				}
			final LinkButton fLink = link;
			fLink.doAction();
			SwingUtil.waitForAWTEventThread();
			final JDialog d = SwingTestUtil.getOnlyVisibleDialog(wwd);
			Assert.assertNotNull(d);
			SwingUtil.invokeAndWait(new Runnable()
			{
				public void run()
				{
					JSpinner sp = SwingTestUtil.getOnlySpinner(d.getContentPane());
					Assert.assertEquals(sp.getValue(), 10);
					sp.setValue(frags.getMinFreq());
				}
			});
			SwingUtil.invokeAndWait(new Runnable()
			{
				public void run()
				{
					JCheckBox cb = SwingTestUtil.getOnlyCheckBox(d.getContentPane());
					Assert.assertTrue(cb.isSelected());
					if (cb.isSelected() != frags.isSkipOmnipresent())
						SwingTestUtil.clickButton(cb);
				}
			});
			SwingUtil.invokeAndWait(new Runnable()
			{
				public void run()
				{
					JComboBox<?> box = SwingTestUtil.getOnlyComboBox(d.getContentPane());
					Assert.assertTrue(box.getSelectedItem().toString().equals(MatchEngine.OpenBabel.toString()));
					if (!box.getSelectedItem().toString().equals(frags.getMatchEngine().toString()))
						box.setSelectedIndex(1 - box.getSelectedIndex());
					Assert.assertTrue(box.getSelectedItem().toString().equals(frags.getMatchEngine().toString()));
				}
			});
			JButton closeButton = SwingTestUtil.getButton(d, "Close");
			SwingTestUtil.clickButton(closeButton);
			//((FeatureWizardPanel) wwd.getCurrentPanel()).getFragmentPropPanel();
		}
		Assert.assertTrue(nextButton.isEnabled());
		nextButton.doClick();

		//clusterer && embedder
		LinkedHashMap<Algorithm, SimpleViewAlgorithmProvider> map = new LinkedHashMap<Algorithm, SimpleViewAlgorithmProvider>();
		map.put(clust, new ClustererProvider());
		map.put(emb, new EmbedderProvider());
		for (final Algorithm alg : map.keySet())
		{
			SimpleViewAlgorithmProvider prov = map.get(alg);
			boolean unselected = false;

			if (alg == prov.getYesAlgorithm() || alg == prov.getNoAlgorithm())
			{
				switchAdvancedSimple(wwd.getCurrentPanel(), true);
				List<JComponent> r = SwingTestUtil.getComponents(wwd.getCurrentPanel(), JRadioButton.class);
				Assert.assertTrue(r.size() == 2 && ((JRadioButton) r.get(1)).getText().equals("No"));
				if (alg == prov.getYesAlgorithm())
					SwingTestUtil.clickButton((JRadioButton) r.get(0));
				else
					SwingTestUtil.clickButton((JRadioButton) r.get(1));
				if (!nextButton.isEnabled())
				{
					unselected = true;
					SwingTestUtil.clickButton((JRadioButton) r.get(1));
				}
			}
			else
			{
				switchAdvancedSimple(wwd.getCurrentPanel(), false);
				JList<?> list = SwingTestUtil.getOnlyList(wwd.getCurrentPanel());
				int idx = -1;
				for (int i = 0; i < list.getModel().getSize(); i++)
					if (list.getModel().getElementAt(i) == alg)
					{
						idx = i;
						break;
					}
				Assert.assertTrue(idx > 0);
				list.setSelectedIndex(idx);
				if (!nextButton.isEnabled())
				{
					unselected = true;
					list.setSelectedIndex(0);
				}
			}
			if (unselected)
				System.err.println("selected " + prov.getNoAlgorithm() + " instead of " + alg
						+ " because of no next-button disabled");
			else if (alg.getProperties() != null && algorithmProps.containsKey(alg))
				for (String prop : algorithmProps.keySet2(alg))
				{
					boolean found = false;
					for (final Property p : alg.getProperties())
					{
						if (p.getName().equals(prop))
						{
							found = true;
							System.err.println("setting " + p.getName() + " of " + alg.getName() + " from "
									+ p.getValue() + " to " + algorithmProps.get(alg, p.getName()));
							Assert.assertNotEquals(p.getValue(), algorithmProps.get(alg, p.getName()));

							//							// this has to be done even if random is not selected, as num-feature may be too low
							//							if (alg == Random3DEmbedder.INSTANCE && p.getName().equals("Random seed"))
							//								//cannot be done in gui, do this directly manually
							//								alg.getRandomSeedProperty().setValue(algorithmProps.get(alg, p.getName()));
							SwingUtil.invokeAndWait(new Runnable()
							{
								public void run()
								{
									PropertyComponent pc = ((AbstractWizardPanel) wwd.getCurrentPanel())
											.getComponentForProperty(p);
									Object v = algorithmProps.get(alg, p.getName());
									if (v instanceof Integer)
										((JSpinner) pc).setValue((Integer) v);
									else
										throw new IllegalStateException("not yet implemented");
								}
							});
						}
					}
					if (!found)
						throw new IllegalStateException();
				}

			Assert.assertTrue(nextButton.isEnabled());
			nextButton.doClick();
		}

		Assert.assertTrue(startButton.isEnabled());
		SwingTestUtil.clickButton(startButton);
		while (wwd.isShowing())
		{
			System.out.println("wait for wizard to close");
			ThreadUtil.sleep(50);
		}
		CheSMapping mapping = wwd.getChesMapping();
		return mapping;
	}

	/**
	 * use the standard export/workflow way that stores to global props to have this settings available in the wizard
	 */
	public static CheSMapping storeAndLoadProps(String dataset, DescriptorSelection feats, FragmentSettings frags,
			DatasetClusterer clust, ThreeDEmbedder emb, String mappingKey)
	{
		Properties props = MappingWorkflow.createMappingWorkflow(dataset, feats, frags, clust, emb);
		CheSMapping mapping = MappingWorkflow.createMappingFromMappingWorkflow(props, "");

		String dest = "/tmp/props" + mappingKey;
		if (new File(dest).exists())
			System.err.println("overwriting already existing file: " + dest);
		FileUtil.copy(PropHandler.getPropertiesFile(), dest);
		System.err.println("stored props at: " + dest);
		tmpfiles.add(dest);

		return mapping;
	}

	private static CheSMapping restartWizardWithProps(DescriptorSelection feats, String mappingKey)
	{
		//		Assert.assertNull(Settings.TOP_LEVEL_FRAME);
		for (PropertySetProvider.PropertySetShortcut feat : feats.getShortcuts())
			if (feat != PropertySetProvider.PropertySetShortcut.integrated)
				for (CompoundPropertySet set : PropertySetProvider.INSTANCE.getDescriptorSets(null, feat))
					if (set.isHiddenFromGUI())
						throw new IllegalSettingException("skipping wizard test - cannot use gui for hidden feature: "
								+ set);

		String dest = "/tmp/props" + mappingKey;
		System.err.println("use prop file from " + dest);
		Assert.assertTrue(new File(dest).exists());
		FileUtil.robustRenameTo(dest, PropHandler.getPropertiesFile());
		Assert.assertFalse(new File(dest).exists());
		tmpfiles.remove(dest);
		PropHandler.forceReload();

		final CheSMapperWizard wwd = new CheSMapperWizard(null);
		while (SwingTestUtil.getOnlyVisibleFrame() == null)
		{
			System.out.println("wait for wizard to show");
			ThreadUtil.sleep(50);
		}
		SwingTestUtil.waitForGUI(50);
		//		ThreadUtil.sleep(20000);
		System.out.println("start wizard test");
		JButton startButton = SwingTestUtil.getButton(wwd, "Start mapping");
		JButton nextButton = SwingTestUtil.getButton(wwd, "Next");
		while (!startButton.isEnabled())
		{
			//			ThreadUtil.sleep(1000);
			Assert.assertTrue(nextButton.isEnabled());
			while (nextButton.isEnabled())
			{
				SwingTestUtil.clickButton(nextButton);
				//				ThreadUtil.sleep(1000);
			}
			if (wwd.getCurrentPanel() instanceof ClusterWizardPanel
					|| wwd.getCurrentPanel() instanceof EmbedWizardPanel)
			{
				System.err.println("unselect "
						+ ((wwd.getCurrentPanel() instanceof ClusterWizardPanel) ? "clustering" : "embedding")
						+ " in wizard");
				JButton toggleButton = SwingTestUtil.getButton(wwd.getCurrentPanel(), "<< Simple");
				if (toggleButton != null)
				{
					JList<?> list = SwingTestUtil.getOnlyList(wwd.getCurrentPanel());
					Assert.assertTrue(list.getSelectedIndex() != 0);
					list.setSelectedIndex(0);
				}
				else
				{
					toggleButton = SwingTestUtil.getButton(wwd.getCurrentPanel(), "Advanced >>");
					Assert.assertNotNull(toggleButton);
					JRadioButton radio = SwingTestUtil.getRadioButton(wwd.getCurrentPanel(), "No");
					Assert.assertTrue(radio.isShowing());
					Assert.assertFalse(radio.isSelected());
					radio.doClick();
					Assert.assertTrue(radio.isSelected());
				}
				//				ThreadUtil.sleep(1000);
				Assert.assertTrue(nextButton.isEnabled());
			}
			else
				Assert.fail("not clustering or embedding panel");
		}
		Assert.assertTrue(startButton.isEnabled());
		SwingTestUtil.clickButton(startButton);
		while (wwd.isShowing())
		{
			System.out.println("wait for wizard to close");
			ThreadUtil.sleep(50);
		}
		//		ThreadUtil.sleep(1000);
		CheSMapping mapping = wwd.getChesMapping();
		//		Settings.TOP_LEVEL_FRAME = null;
		return mapping;
	}

	/**
	 * direct way
	 */
	private static CheSMapping directlyUseAlgorithms(String dataset, DescriptorSelection feats, FragmentSettings frags,
			DatasetClusterer clust, ThreeDEmbedder emb)
	{
		return directlyUseAlgorithms(dataset, feats, frags, clust, emb, UseOrigStructures.INSTANCE, NoAligner.INSTANCE);
	}

	private static CheSMapping directlyUseAlgorithms(String dataset, DescriptorSelection feats, FragmentSettings frags,
			DatasetClusterer clust, ThreeDEmbedder emb, ThreeDBuilder builder, ThreeDAligner align)

	{
		// use direct way without props, both should yield equal results
		// (doing this instead of comparing mapping directly because algorithms are singletons)
		DatasetFile d = new DatasetLoader(false).load(dataset);
		if (frags != null)
			frags.apply(d);
		return new CheSMapping(d, ListUtil.toArray(CompoundPropertySet.class, feats.getFilteredFeatures(d)), clust,
				builder, emb, align);
	}

}
