package org.chesmapper.test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.chesmapper.map.alg.Algorithm;
import org.chesmapper.map.alg.cluster.DatasetClusterer;
import org.chesmapper.map.alg.cluster.NoClusterer;
import org.chesmapper.map.alg.cluster.r.DynamicTreeCutHierarchicalRClusterer;
import org.chesmapper.map.alg.embed3d.AbstractRTo3DEmbedder;
import org.chesmapper.map.alg.embed3d.Random3DEmbedder;
import org.chesmapper.map.alg.embed3d.ThreeDEmbedder;
import org.chesmapper.map.alg.embed3d.WekaPCA3DEmbedder;
import org.chesmapper.map.alg.embed3d.r.Sammon3DEmbedder;
import org.chesmapper.map.data.ClusteringData;
import org.chesmapper.map.data.DatasetFile;
import org.chesmapper.map.data.fragments.MatchEngine;
import org.chesmapper.map.dataInterface.CompoundPropertySet;
import org.chesmapper.map.dataInterface.CompoundPropertySet.Type;
import org.chesmapper.map.dataInterface.CompoundPropertyUtil;
import org.chesmapper.map.dataInterface.FragmentPropertySet;
import org.chesmapper.map.main.BinHandler;
import org.chesmapper.map.main.CheSMapping;
import org.chesmapper.map.main.Settings;
import org.chesmapper.map.property.CDKFingerprintSet;
import org.chesmapper.map.property.FminerPropertySet;
import org.chesmapper.map.property.OBFingerprintSet;
import org.chesmapper.map.property.PropertySetProvider;
import org.chesmapper.map.property.PropertySetProvider.PropertySetShortcut;
import org.chesmapper.map.workflow.ClustererProvider;
import org.chesmapper.map.workflow.MappingWorkflow.DescriptorSelection;
import org.chesmapper.map.workflow.MappingWorkflow.FragmentSettings;
import org.chesmapper.test.util.MappingCreator;
import org.chesmapper.test.util.MappingCreator.IllegalSettingException;
import org.chesmapper.test.util.MappingCreator.Mode;
import org.chesmapper.view.cluster.Clustering;
import org.chesmapper.view.cluster.ClusteringImpl;
import org.chesmapper.view.cluster.ExportData;
import org.chesmapper.view.gui.LaunchCheSMapper;
import org.junit.Assert;
import org.junit.Test;
import org.mg.javalib.gui.property.Property;
import org.mg.javalib.io.SDFUtil;
import org.mg.javalib.util.ArrayUtil;
import org.mg.javalib.util.DoubleKeyHashMap;
import org.mg.javalib.util.FileUtil;
import org.mg.javalib.util.FileUtil.CSVFile;
import org.mg.javalib.util.ListUtil;
import org.mg.javalib.util.ObjectUtil;
import org.mg.javalib.util.ThreadUtil;

public class MappingAndExportTest
{
	static
	{
		LaunchCheSMapper.init();
		LaunchCheSMapper.setExitOnClose(false);
		String version = BinHandler.getOpenBabelVersion();
		if (!version.equals("2.3.2"))
			throw new IllegalStateException("tests require obenbabel version 2.3.2, is: " + version);
	}

	static class DatasetConfig
	{
		String name;
		int size;
		String integratedNonMissing[];
		String integratedMissing[];
		String integratedFeature;

		public DatasetConfig(String name, int size, String[] integratedNonMissing, String[] integratedMissing,
				String integratedFeature)
		{
			this.name = name;
			this.size = size;
			this.integratedNonMissing = integratedNonMissing;
			this.integratedMissing = integratedMissing;
			this.integratedFeature = integratedFeature;
			if (this.integratedMissing == null)
				this.integratedMissing = new String[0];
		}
	}

	static DatasetConfig D_INCHI = new DatasetConfig("compounds_inchi.csv", 7, new String[] { "name" }, null, "");
	static DatasetConfig D_SDF = new DatasetConfig(
			"12compounds.sdf",
			12,
			new String[] { "DSSTox_RID", "DSSTox_CID", "DSSTox_Generic_SID", "DSSTox_FileID", "STRUCTURE_Formula",
					"STRUCTURE_MolecularWeight", "STRUCTURE_ChemicalType", "STRUCTURE_TestedForm_DefinedOrganic",
					"STRUCTURE_Shown", "TestSubstance_ChemicalName", "TestSubstance_CASRN",
					"TestSubstance_Description", "STRUCTURE_ChemicalName_IUPAC", "STRUCTURE_SMILES",
					"STRUCTURE_Parent_SMILES", "STRUCTURE_InChI", "STRUCTURE_InChIKey", "StudyType", "Endpoint",
					"Species", "ChemClass_ERB", "ER_RBA", "LOG_ER_RBA", "ActivityScore_NCTRER",
					"ActivityOutcome_NCTRER", "ActivityCategory_ER_RBA", "ActivityCategory_Rationale_ChemClass_ERB",
					"F1_Ring", "F2_AromaticRing", "F3_PhenolicRing", "F4_Heteroatom", "F5_Phenol3nPhenyl",
					"F6_OtherKeyFeatures", "LOGP" },
			new String[] { "Mean_ER_RBA_ChemClass" },
			"F1_Ring,F2_AromaticRing,F3_PhenolicRing,F4_Heteroatom,F5_Phenol3nPhenyl,F6_OtherKeyFeatures,LOGP,STRUCTURE_MolecularWeight");
	static DatasetConfig D_SMI = new DatasetConfig("demo.smi", 10, new String[] {}, null, "");
	static DatasetConfig D_CSV = new DatasetConfig("caco2_20.csv", 20, new String[] { "name", "caco2", "logD", "rgyr",
			"HCPSA", "fROTB" }, null, "logD,rgyr,HCPSA,fROTB");

	static class FeatureConfig
	{
		String shortName;
		String featureNames[];
		Integer minFreq;
		private MatchEngine matchEngine = MatchEngine.OpenBabel;
		HashMap<DatasetConfig, Integer> numFragments = new HashMap<DatasetConfig, Integer>();

		@Override
		public String toString()
		{
			String s = shortName;
			if (minFreq != null)
				s += " f:" + minFreq + " m:" + matchEngine;
			return s;
		}

		public FeatureConfig(String shortName, String[] featureNames)
		{
			this.shortName = shortName;
			this.featureNames = featureNames;
		}

		public FeatureConfig(String shortName, String[] featureNames, int minFreq, MatchEngine matchEngine)
		{
			this.shortName = shortName;
			this.featureNames = featureNames;
			this.minFreq = minFreq;
			if (matchEngine == null)
				throw new IllegalStateException("do not set match engine to null");
			this.matchEngine = matchEngine;
		}
	}

	final static DatasetConfig datasets[];
	final static DatasetClusterer clusterers[];
	final static ThreeDEmbedder embedders[];
	final static PropertySetShortcut featureTypes[];
	final static int minFreq[];
	final static MatchEngine matchEngines[];
	final static MappingCreator.Mode mappingMode[];
	final static Boolean testCaching;
	final static DoubleKeyHashMap<Algorithm, String, Object> algorithmProps = new DoubleKeyHashMap<Algorithm, String, Object>();
	static
	{
		algorithmProps.put(new ClustererProvider().getYesAlgorithm(), "minNumClusters", 3);
		algorithmProps.put(DynamicTreeCutHierarchicalRClusterer.INSTANCE,
				"Minimum number of compounds in each cluster (minClusterSize)", 2);
		//not testing random-3d-seed, 
		//configure-wizard currently does not change backup-alg properties, and not visible in simple view
		//algorithmProps.put(Random3DEmbedder.INSTANCE, "Random seed", 2);
		algorithmProps.put(Sammon3DEmbedder.INSTANCE, "Maximum number of iterations (niter)", 10);
	}

	static
	{
		DatasetConfig datasetsReduced[] = new DatasetConfig[] { D_CSV, D_INCHI };
		DatasetConfig datasetsAll[] = new DatasetConfig[] { D_CSV, D_INCHI, D_SDF, D_SMI };

		DatasetClusterer clusterersAll[] = new DatasetClusterer[] { NoClusterer.INSTANCE,
				new ClustererProvider().getYesAlgorithm(), DynamicTreeCutHierarchicalRClusterer.INSTANCE };
		ThreeDEmbedder embeddersAll[] = new ThreeDEmbedder[] { Random3DEmbedder.INSTANCE,
				WekaPCA3DEmbedder.INSTANCE_NO_PROBS, Sammon3DEmbedder.INSTANCE };

		PropertySetShortcut featureTypesAll[] = PropertySetShortcut.values();
		int minFreqAll[] = new int[] { 0, 1, 2 };
		MatchEngine matchEnginesAll[] = new MatchEngine[] { MatchEngine.OpenBabel, MatchEngine.CDK };

		PropertySetShortcut featureTypesMany[] = { PropertySetShortcut.integrated, PropertySetShortcut.ob,
				PropertySetShortcut.benigniBossa, PropertySetShortcut.cdkFunct, PropertySetShortcut.obMACCS };
		int minFreqMany[] = minFreqAll;
		MatchEngine matchEnginesMany[] = matchEnginesAll;

		PropertySetShortcut featureTypesFew[] = { PropertySetShortcut.integrated, PropertySetShortcut.ob,
				PropertySetShortcut.benigniBossa };
		int minFreqFew[] = new int[] { 0, 2 };
		MatchEngine matchEnginesFew[] = new MatchEngine[] { MatchEngine.OpenBabel };

		if (TestLauncher.MAPPING_TEST == TestLauncher.MappingTest.mapping_single)
		{
			// check if test is running
			datasets = new DatasetConfig[] { D_CSV, D_INCHI };
			clusterers = new DatasetClusterer[] { NoClusterer.INSTANCE };
			embedders = new ThreeDEmbedder[] { Random3DEmbedder.INSTANCE };
			featureTypes = new PropertySetShortcut[] { PropertySetShortcut.ob };
			minFreq = null;
			matchEngines = null;
			mappingMode = new MappingCreator.Mode[] { MappingCreator.Mode.DirectlyUseAlgorithms };
			testCaching = false;
		}
		else if (TestLauncher.MAPPING_TEST == TestLauncher.MappingTest.mapping_wizard)
		{
			// checks if wizard configuration produces correct results
			// mapping1: from command line (stores it to global props)
			// mapping2: restarting the wizard based on global props
			// mapping3: configuring the wizard w/o global props
			// checks all mappings for correct results and equal results to previous mappings
			// caching is always enabled
			datasets = datasetsReduced;
			clusterers = clusterersAll;
			embedders = embeddersAll;
			featureTypes = featureTypesFew;
			minFreq = minFreqFew;
			matchEngines = matchEnginesFew;
			mappingMode = new Mode[] { Mode.StoreAndLoadProps, Mode.RestartWizardWithProps, Mode.ConfigureWizard };
			testCaching = false;
		}
		else if (TestLauncher.MAPPING_TEST == TestLauncher.MappingTest.mapping_cache)
		{
			// checks if re-runing algorithms and caching produces correct results 
			// mapping1: from command line (stores it to global props) w/o caching
			// mapping2: directly from algorithm (w/o props) w/o caching
			// mapping3: from command line with caching
			// checks all mappings for correct results and equal results to previous mappings
			datasets = datasetsReduced;
			clusterers = clusterersAll;
			embedders = embeddersAll;
			featureTypes = featureTypesMany;
			minFreq = minFreqMany;
			matchEngines = matchEnginesMany;
			mappingMode = new Mode[] { Mode.StoreAndLoadProps, Mode.DirectlyUseAlgorithms, Mode.DirectlyUseAlgorithms };
			testCaching = true;
		}
		else if (TestLauncher.MAPPING_TEST == TestLauncher.MappingTest.mapping_all)
		{
			// combination of wizard and cache check
			// with two more datasets and more features
			datasets = datasetsAll;
			clusterers = clusterersAll;
			embedders = embeddersAll;
			featureTypes = featureTypesAll;
			minFreq = minFreqAll;
			matchEngines = matchEnginesAll;
			mappingMode = new Mode[] { Mode.StoreAndLoadProps, Mode.DirectlyUseAlgorithms, Mode.DirectlyUseAlgorithms,
					Mode.RestartWizardWithProps, Mode.ConfigureWizard };
			testCaching = true;
		}
		else if (TestLauncher.MAPPING_TEST == TestLauncher.MappingTest.mapping_debug)
		{
			throw new IllegalStateException();
		}
		else
			throw new IllegalStateException();
	}

	static
	{
		if (algorithmProps != null)
		{
			boolean warn = false;
			for (Algorithm alg : algorithmProps.keySet1())
			{
				boolean match = false;
				for (Algorithm alg2 : ArrayUtil.concat(Algorithm.class, clusterers, embedders))
					if (alg == alg2)
					{
						match = true;
						break;
					}
				if (!match)
				{
					if (!warn)
					{
						warn = true;
						System.err.println("XXXXXXXXXXXXXXXXXXXXXXXX");
					}
					System.err.println("Algorithm Prop not used in this test: " + alg.getName());
				}
			}
			if (warn)
			{
				System.err.println("XXXXXXXXXXXXXXXXXXXXXXXX");
				ThreadUtil.sleep(5000);
			}
		}
	}

	static List<FeatureConfig> features = new ArrayList<FeatureConfig>();

	static
	{
		//		for (PropertySetShortcuts c : new PropertySetShortcuts[] { PropertySetShortcuts.ob,
		//				PropertySetShortcuts.cdk })
		for (PropertySetShortcut c : featureTypes)
		{
			if (c == PropertySetShortcut.fminer)
				continue;

			List<String> props = new ArrayList<String>();
			CompoundPropertySet sets[] = null;
			if (c != PropertySetShortcut.integrated)
			{
				sets = PropertySetProvider.INSTANCE.getDescriptorSets(null, c);
				for (CompoundPropertySet set : sets)
				{
					if ((c == PropertySetShortcut.cdk || c == PropertySetShortcut.ob) && set.getType() != Type.NUMERIC)
						continue;
					if (!set.isSizeDynamic())
						for (int i = 0; i < set.getSize(null); i++)
							props.add(CompoundPropertyUtil.propToExportString(set.get(null, i)));
				}
			}
			if (sets == null || !(sets[0] instanceof FragmentPropertySet))
			{
				features.add(new FeatureConfig(c.toString(), ListUtil.toArray(String.class, props)));
			}
			else
			{
				for (int minF : minFreq)
				{
					for (MatchEngine matchE : matchEngines)
					{
						if (sets[0] instanceof CDKFingerprintSet && matchE == MatchEngine.OpenBabel)
							continue;
						if (sets[0] instanceof FminerPropertySet && matchE == MatchEngine.CDK)
							continue;
						if (sets[0] instanceof OBFingerprintSet && matchE == MatchEngine.CDK)
							continue;

						FeatureConfig f = new FeatureConfig(c.toString(), ListUtil.toArray(String.class, props), minF,
								matchE);
						features.add(f);

						if (c == PropertySetShortcut.benigniBossa)
						{
							if (minF == 0)
							{
								if (matchE == MatchEngine.OpenBabel)
								{
									f.numFragments.put(D_INCHI, 0);
									f.numFragments.put(D_SDF, 1);
									f.numFragments.put(D_SMI, 2);
									f.numFragments.put(D_CSV, 6);
								}
								else if (matchE == MatchEngine.CDK)
								{
									f.numFragments.put(D_CSV, 3);
									f.numFragments.put(D_INCHI, 0);
									f.numFragments.put(D_SMI, 1);
									f.numFragments.put(D_SDF, 0);
								}
							}
							else if (minF == 1)
							{
								if (matchE == MatchEngine.OpenBabel)
								{
									f.numFragments.put(D_INCHI, 0);
									f.numFragments.put(D_SDF, 1);
									f.numFragments.put(D_CSV, 6);
									f.numFragments.put(D_SMI, 2);
								}
								else if (matchE == MatchEngine.CDK)
								{
									f.numFragments.put(D_INCHI, 0);
									f.numFragments.put(D_SMI, 1);
									f.numFragments.put(D_SDF, 0);
									f.numFragments.put(D_CSV, 3);
								}
							}
							else if (minF == 2)
							{
								if (matchE == MatchEngine.OpenBabel)
								{
									f.numFragments.put(D_INCHI, 0);
									f.numFragments.put(D_SDF, 1);
								}
								else if (matchE == MatchEngine.CDK)
								{
									f.numFragments.put(D_INCHI, 0);
									f.numFragments.put(D_SMI, 1);
									f.numFragments.put(D_SDF, 0);
								}
							}
						}
						else if (c == PropertySetShortcut.obFP2)
						{
							if (minF == 0)
							{
								f.numFragments.put(D_CSV, 825);
								f.numFragments.put(D_SMI, 432);
							}
							else if (minF == 1)
							{
								f.numFragments.put(D_CSV, 825);
								f.numFragments.put(D_SMI, 432);
							}
							else if (minF == 2)
							{
							}
						}
						else if (c == PropertySetShortcut.obFP3)
						{
							if (minF == 0)
							{
							}
							else if (minF == 1)
							{
								f.numFragments.put(D_INCHI, 8);
							}
							else if (minF == 2)
							{
								f.numFragments.put(D_INCHI, 8);
							}
						}
						else if (c == PropertySetShortcut.obFP4)
						{
							if (minF == 0)
							{
								f.numFragments.put(D_INCHI, 19);
								f.numFragments.put(D_SMI, 48);
							}
							else if (minF == 1)
							{
								f.numFragments.put(D_INCHI, 19);
								f.numFragments.put(D_SMI, 48);
							}
							else if (minF == 2)
							{
							}
						}
						else if (c == PropertySetShortcut.obMACCS)
						{
							if (minF == 0)
							{
								f.numFragments.put(D_SMI, 111);
							}
							else if (minF == 1)
							{
								f.numFragments.put(D_SMI, 111);
							}
							else if (minF == 2)
							{
							}
						}
						else if (c == PropertySetShortcut.cdkFunct)
						{
							if (minF == 0)
							{
								f.matchEngine = MatchEngine.CDK;
								f.numFragments.put(D_INCHI, 6);
								f.numFragments.put(D_CSV, 40);
							}
							else if (minF == 1)
							{
								f.matchEngine = MatchEngine.CDK;
								f.numFragments.put(D_SDF, 14);
								f.numFragments.put(D_INCHI, 6);
								f.numFragments.put(D_CSV, 38);
							}
							else if (minF == 2)
							{
							}
						}
					}
				}
			}
		}
	}

	static Set<String> tmpfiles = new HashSet<String>();
	static DoubleKeyHashMap<String, FeatureConfig, Integer> numFeatures = new DoubleKeyHashMap<String, FeatureConfig, Integer>();
	static HashMap<String, String> featureNames = new HashMap<String, String>();
	static HashMap<String, String> positions = new HashMap<String, String>();
	static HashMap<String, String> outfiles = new HashMap<String, String>();

	@Test
	public void test()
	{
		try
		{
			Random r = new Random();
			ArrayUtil.scramble(datasets, r);
			ListUtil.scramble(features, r);
			ArrayUtil.scramble(clusterers, r);
			ArrayUtil.scramble(embedders, r);

			int max = mappingMode.length * datasets.length * features.size() * clusterers.length * embedders.length;
			int count = 0;

			int dIdx = 0;
			for (DatasetConfig data : datasets)
			{
				int mapIdx = 0;
				for (MappingCreator.Mode mapMode : mappingMode)
				{
					if (testCaching)
					{
						// the first two mapping mode runs are not cached, to ensure the calculated result is equal
						// from the third run on, caching is enabled, to ensure the cached result is equal
						Settings.CACHING_ENABLED = mapIdx > 1;
					}

					DatasetFile dataset = null;
					numFeatures.clear();

					int fIdx = 0;
					for (FeatureConfig feat : features)
					{
						int cIdx = 0;
						for (DatasetClusterer clust : clusterers)
						{
							int eIdx = 0;
							for (ThreeDEmbedder emb : embedders)
							{
								// algorithms are singletons, reset values to test proper adjustment
								for (Algorithm alg : new Algorithm[] { clust, emb })
									if (alg.getProperties() != null)
										for (Property p : alg.getProperties())
											p.setValue(p.getDefaultValue());
								// check that no gui is used
								if (ArrayUtil.indexOf(mappingMode, MappingCreator.Mode.ConfigureWizard) == -1
										&& ArrayUtil.indexOf(mappingMode, MappingCreator.Mode.RestartWizardWithProps) == -1)
									Assert.assertNull(Settings.TOP_LEVEL_FRAME);

								count++;
								String msg = count + "/" + max + ":\n ";
								msg += "data  (" + (dIdx + 1) + "/" + datasets.length + "): " + data.name + "\n ";
								msg += "map   (" + (mapIdx + 1) + "/" + mappingMode.length + "): " + mapMode + "\n ";
								msg += "feat  (" + (fIdx + 1) + "/" + features.size() + "): " + feat + "\n ";
								msg += "clust (" + (cIdx + 1) + "/" + clusterers.length + "): " + clust.getName()
										+ "\n ";
								msg += "emb   (" + (eIdx + 1) + "/" + embedders.length + "): " + emb.getName();
								System.err.println("\n================================================\n" + msg
										+ "\n------------------------------------------------");

								boolean testClustEmbedCombi = (clusterers.length == 1 && embedders.length == 1)
										|| (clust == NoClusterer.INSTANCE && emb != Random3DEmbedder.INSTANCE)
										|| (clust != NoClusterer.INSTANCE && emb == Random3DEmbedder.INSTANCE);
								if (!testClustEmbedCombi)
									System.err.println("skipping cluster - embedding combination");
								else
								{
									DescriptorSelection feats = DescriptorSelection.select(feat.shortName,
											data.integratedFeature, null, null, null);
									FragmentSettings frags = null;
									if (feat.minFreq != null)
									{
										if (feat.minFreq == 0)
											frags = new FragmentSettings(1, false, feat.matchEngine);
										else
											frags = new FragmentSettings(feat.minFreq, true, feat.matchEngine);
									}

									String mapKey = dIdx + "." + fIdx + "." + cIdx + "." + eIdx;
									try
									{
										CheSMapping mapping = MappingCreator.create(mapMode, "data/" + data.name,
												feats, frags, clust, emb, algorithmProps, mapKey);
										dataset = mapping.getDatasetFile();

										ClusteringData clusteringData = mapping.doMapping();
										ClusteringImpl clustering = new ClusteringImpl();
										clustering.newClustering(clusteringData);

										checkFeatures(data, feat, feats, mapping, clustering);

										DatasetClusterer usedClust = clusteringData.getDatasetClusterer();
										checkClusters(clust, usedClust, clustering);

										ThreeDEmbedder usedEmb = clusteringData.getThreeDEmbedder();
										checkEmbed(mapKey, emb, usedEmb, mapping, clusteringData, clustering);

										checkExport(data, feat, mapKey, mapIdx, usedClust, clustering);
									}
									catch (IllegalSettingException e)
									{
										System.err.println(e.getMessage());
									}
								}
								eIdx++;
							}
							cIdx++;
						}
						fIdx++;
					}
					mapIdx++;
					if (dataset != null)
						dataset.clear();
				}
				dIdx++;
			}
			System.err.println("\n" + count + "/" + max + " tests done");
		}
		finally
		{
			int count = 0;
			int delCount = 0;
			for (String f : ListUtil.concat(new ArrayList<String>(tmpfiles), new ArrayList<String>(
					MappingCreator.tmpfiles)))
			{
				try
				{
					if (new File(f).delete())
						delCount++;
					count++;
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
			System.err.println("deleted " + delCount + "/" + count + " tmp-files");
		}
	}

	private void checkFeatures(DatasetConfig data, FeatureConfig feat, DescriptorSelection feats, CheSMapping mapping,
			Clustering clustering)
	{
		DatasetFile dataset = mapping.getDatasetFile();

		// check features
		Assert.assertEquals(feats.getFilteredFeatures(dataset).size(), mapping.getNumFeatureSets());
		System.err.println(data.name + " " + feat.shortName + " " + feat.minFreq + " " + feat.matchEngine
				+ " num-features:" + clustering.getFeatures().size());
		if (feat.numFragments.containsKey(data))
			Assert.assertTrue(clustering.getFeatures().size() == feat.numFragments.get(data));
		else if (feat.shortName.equals(PropertySetShortcut.integrated.toString())
				&& ObjectUtil.equals(data.integratedFeature, ""))
			Assert.assertTrue(clustering.getFeatures().size() == 0);
		else
			Assert.assertTrue(clustering.getFeatures().size() > 0);

		if (feat.minFreq != null)
		{
			String key = feat.shortName + feat.matchEngine;
			numFeatures.put(key, feat, clustering.getFeatures().size());
			FeatureConfig minFreq[] = ArrayUtil.toArray(new ArrayList<FeatureConfig>(numFeatures.keySet2(key)));
			for (int i = 0; i < minFreq.length - 1; i++)
			{
				for (int j = i + 1; j < minFreq.length; j++)
				{
					Assert.assertNotEquals(minFreq[i].minFreq, minFreq[j].minFreq);
					System.err.println("minF: " + minFreq[i].minFreq + ", num-features: "
							+ numFeatures.get(key, minFreq[i]));
					System.err.println("minF2: " + minFreq[j].minFreq + ", num-features: "
							+ numFeatures.get(key, minFreq[j]));
					if (minFreq[i].numFragments.containsKey(data) && minFreq[j].numFragments.containsKey(data))
					{
						//do nothing, is checked explicitely
					}
					else if (minFreq[i].minFreq < minFreq[j].minFreq)
						Assert.assertTrue(numFeatures.get(key, minFreq[i]) > numFeatures.get(key, minFreq[j]));
					else
						Assert.assertTrue(numFeatures.get(key, minFreq[i]) < numFeatures.get(key, minFreq[j]));
				}
			}
		}
	}

	private void checkClusters(DatasetClusterer clust, DatasetClusterer usedClust, Clustering clustering)
	{
		// check clustering result
		if (clustering.getFeatures().size() == 0)
			Assert.assertEquals(NoClusterer.INSTANCE, usedClust);
		else
			Assert.assertEquals(clust, usedClust);
		if (usedClust != NoClusterer.INSTANCE)
		{
			Assert.assertTrue(clustering.getNumClusters() > 1);
		}
		else
		{
			Assert.assertTrue(clustering.getNumClusters() == 1);
		}
	}

	private void checkEmbed(String mapKey, ThreeDEmbedder emb, ThreeDEmbedder usedEmb, CheSMapping mapping,
			ClusteringData clusteringData, Clustering clustering)
	{
		if (clustering.getFeatures().size() == 0
				|| (emb instanceof AbstractRTo3DEmbedder && mapping.getEmbedException() != null
						&& clustering.getFeatures().size() <= 10 && mapping.getEmbedException().getMessage()
						.contains("Too few unique data points")))
			Assert.assertEquals(Random3DEmbedder.INSTANCE, usedEmb);
		else
			Assert.assertEquals("Embedding should have been performed with " + emb + ", instead used: " + usedEmb, emb,
					usedEmb);
		if (usedEmb == Random3DEmbedder.INSTANCE)
		{
			System.err.println("using random embedding instead");
			Assert.assertNull(clusteringData.getEmbeddingQualityProperty());
		}
		else
		{
			Assert.assertNotNull(clusteringData.getEmbeddingQualityProperty());
		}
		if (!positions.containsKey(mapKey))
		{
			featureNames.put(mapKey, ListUtil.toString(clustering.getFeatures()));
			positions.put(mapKey, ListUtil.toString(usedEmb.getPositions()));
		}
		else
		{
			System.err.println("checking that features and positions are equal " + mapKey);
			Assert.assertEquals(featureNames.get(mapKey), ListUtil.toString(clustering.getFeatures()));
			Assert.assertEquals(positions.get(mapKey), ListUtil.toString(usedEmb.getPositions()));
		}
	}

	private void checkExport(DatasetConfig data, FeatureConfig feat, String mapKey, int mapIdx,
			DatasetClusterer usedClust, Clustering clustering)
	{
		String nonMissingValueProps[] = new String[0];
		for (String output : new String[] { "csv", "sdf" })
		{
			String outfile = "/tmp/" + data.name + "." + mapIdx + "." + mapKey + "." + output;
			ExportData.scriptExport(clustering, outfile, true, 1.0);
			tmpfiles.add(outfile);
			String key = mapKey + output;
			if (!outfiles.containsKey(key))
				outfiles.put(key, outfile);
			else
			{
				System.err.println("checking that outfiles are equal " + outfiles.get(key) + " and " + outfile);
				Assert.assertEquals(FileUtil.getMD5String(outfiles.get(key)), FileUtil.getMD5String(outfile));
			}

			String[] clusterProp = new String[0];
			if (usedClust != NoClusterer.INSTANCE)
				clusterProp = new String[] { (usedClust.getName() + " cluster assignement").replace(' ', '_') };
			if (output.equals("csv"))
				nonMissingValueProps = verifyExportResultCSV(outfile, data.size,
						ArrayUtil.concat(String.class, data.integratedNonMissing, clusterProp),
						ArrayUtil.concat(String.class, feat.featureNames, data.integratedMissing));
			else
				verifyExportResultSDF(outfile, data.size, nonMissingValueProps);
		}
	}

	public String[] verifyExportResultCSV(String csvFile, int numCompounds, String nonMissingValueProps[],
			String potentiallyMissingValueProps[])
	{
		CSVFile f = FileUtil.readCSV(csvFile, ",");
		Assert.assertEquals(numCompounds, f.content.size() - 1);
		Assert.assertEquals("SMILES", f.getHeader()[0]);

		List<String> nonMissingValueProperties = new ArrayList<String>();
		for (String p : nonMissingValueProps)
		{
			Assert.assertNotEquals("not found: " + p + ", " + ArrayUtil.toString(f.getHeader()), f.getColumnIndex(p),
					-1);
			Assert.assertTrue("has missing: " + p, ArrayUtil.indexOf(f.getColumn(p), null) == -1);
			nonMissingValueProperties.add(p);
		}

		for (String p : potentiallyMissingValueProps)
		{
			Assert.assertNotEquals("not found: " + p + ", " + ArrayUtil.toString(f.getHeader()), f.getColumnIndex(p),
					-1);
			if (ArrayUtil.indexOf(f.getColumn(p), null) == -1)
				nonMissingValueProperties.add(p);
			else
				System.err.println("missing: " + p);
		}

		System.err.println("csv checked! " + nonMissingValueProps.length + " " + potentiallyMissingValueProps.length);
		return ListUtil.toArray(String.class, nonMissingValueProperties);
	}

	public void verifyExportResultSDF(String sdfFile, int numCompounds, String nonMissingValueProps[])
	{
		String s[] = SDFUtil.readSdf(sdfFile);
		Assert.assertEquals(numCompounds, s.length);
		for (int i = 0; i < s.length; i++)
			for (String p : nonMissingValueProps)
				Assert.assertTrue("not found: " + p + "\n" + s[i], s[i].indexOf("<" + p + ">") != -1);
		System.err.println("sdf checked! " + nonMissingValueProps.length);
	}
}
