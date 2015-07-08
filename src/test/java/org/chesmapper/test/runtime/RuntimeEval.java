package org.chesmapper.test.runtime;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;

import org.chesmapper.map.alg.Algorithm;
import org.chesmapper.map.alg.align3d.ThreeDAligner;
import org.chesmapper.map.alg.build3d.OpenBabel3DBuilder;
import org.chesmapper.map.alg.build3d.ThreeDBuilder;
import org.chesmapper.map.alg.cluster.DatasetClusterer;
import org.chesmapper.map.alg.cluster.WekaClusterer;
import org.chesmapper.map.alg.embed3d.ThreeDEmbedder;
import org.chesmapper.map.alg.embed3d.r.SMACOF3DEmbedder;
import org.chesmapper.map.alg.embed3d.r.TSNEFeature3DEmbedder;
import org.chesmapper.map.data.DatasetFile;
import org.chesmapper.map.gui.FeatureWizardPanel.FeatureInfo;
import org.chesmapper.map.workflow.ClustererProvider;
import org.chesmapper.map.workflow.EmbedderProvider;
import org.chesmapper.test.MappingAndExportTest;
import org.chesmapper.test.MappingAndExportTest.DatasetConfig;
import org.chesmapper.test.TestLauncher;
import org.chesmapper.test.TestLauncher.MappingTest;
import org.mg.javalib.datamining.ResultSet;
import org.mg.javalib.datamining.ResultSet.LatexTableSettings;
import org.mg.javalib.gui.Messages;
import org.mg.javalib.gui.binloc.Binary;
import org.mg.javalib.gui.property.Property;
import org.mg.javalib.util.ArrayUtil;
import org.mg.javalib.util.FileUtil;
import org.mg.javalib.util.HashUtil;

import weka.clusterers.EM;

@SuppressWarnings("unchecked")
public class RuntimeEval
{
	public static DatasetConfig D_PBDE = new DatasetConfig("PBDE_LogVP.flat.sdf", 34, new String[] {}, null, "");
	public static DatasetConfig D_CACO = new DatasetConfig("caco2.flat.sdf", 100, new String[] {}, null, "");
	public static DatasetConfig D_COX = new DatasetConfig("cox2.flat.sdf", 467, new String[] {}, null, "");
	public static DatasetConfig D_CPDB = new DatasetConfig("CPDBAS_v5d_1547_20Nov2008.ob.sdf", 1508, new String[] {},
			null, "");

	// ....................................................................................

	public static boolean OVERWRITE_CACHE = true;

	public static TestLauncher.MappingTest MODE = TestLauncher.MappingTest.mapping_runtime_embedding;

	public static final DatasetConfig[] DATASETS = new DatasetConfig[] { D_PBDE, D_CACO, D_COX, D_CPDB };//,  };

	// ....................................................................................

	//	static String FILES[] = new String[] { "PBDE_LogVP.flat.sdf", "caco2.flat.sdf", "cox2.flat.sdf",
	//			"CPDBAS_v5d_1547_20Nov2008.ob.sdf" };

	public static void main(String[] args)
	{
		//		MODE = TestLauncher.MappingTest.mapping_runtime_builder;
		TestLauncher.MAPPING_TEST = MODE;
		new MappingAndExportTest().test();
	}

	ResultSet resultSet = new ResultSet();
	Set<String> algNames = new LinkedHashSet<>();

	private static final String EMBED_QUAL = "Q";
	private static final String NUM_FEATURES = "#";

	static class RuntimeResult implements Serializable
	{
		private static final long serialVersionUID = 2L;

		long runtime;
		int numCompounds;
		Integer numFeatures;
		String embQual;
		Integer numClusters;

		public RuntimeResult(int numCompounds, String embQual, Algorithm[] algs)
		{
			this.numCompounds = numCompounds;
			this.runtime = findForMode(algs).getRuntime();
			this.numFeatures = ((PseudoFeatureAlgorithm) find(algs, PseudoFeatureAlgorithm.class)).getNumFeatures();
			this.embQual = embQual;
			this.numClusters = ((DatasetClusterer) find(algs, DatasetClusterer.class)).getClusters().size();
		}

		public RuntimeResult()
		{
		}

		public static RuntimeResult skipped()
		{
			RuntimeResult r = new RuntimeResult();
			r.runtime = -1L;
			return r;
		}
	}

	private static File runtimeCacheFile = new File("runtimeCache");
	private static HashMap<Integer, RuntimeResult> cache;

	static
	{
		try
		{
			if (runtimeCacheFile.exists())
			{
				FileInputStream fin = new FileInputStream(runtimeCacheFile);
				ObjectInputStream ois = new ObjectInputStream(fin);
				cache = (HashMap<Integer, RuntimeResult>) ois.readObject();
				ois.close();
				System.err.println("loading cache with " + cache.size() + " entries");
			}
			else
				cache = new HashMap<>();
		}
		catch (ClassNotFoundException | IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	private int getKey(String datasetName, Algorithm... algs)
	{
		int h = HashUtil.hashCode(MODE.toString(), ArrayUtil.push(getNames(algs), datasetName));
		System.err.println(h + " -- " + MODE + " " + ArrayUtil.toString(getNames(algs)) + " " + datasetName);
		return h;
	}

	public boolean isCached(String datasetName, Algorithm... algs)
	{
		boolean skip = false;
		Algorithm alg = findForMode(algs);
		switch (MODE)
		{
			case mapping_runtime_embedding:
			case mapping_runtime_embedding2:
				skip = alg instanceof TSNEFeature3DEmbedder || alg instanceof SMACOF3DEmbedder;
				break;
			case mapping_runtime_builder:
				skip = (datasetName.startsWith("CPDB") || datasetName.startsWith("caco2_20"))
						&& alg instanceof OpenBabel3DBuilder;
				break;
			case mapping_runtime_clustering:
				skip = datasetName.startsWith("CPDB") && alg instanceof WekaClusterer
						&& ((WekaClusterer) alg).getWekaClusterer() instanceof EM;
				break;
			case mapping_runtime_features:
				skip = datasetName.startsWith("CPDB") && alg.getName().contains("moss");
				break;
			default:
		}
		if (skip)
		{
			System.err.println("skip runtime");
			setResult(datasetName, algs, RuntimeResult.skipped(), false);
			return true;
		}

		Integer k = getKey(datasetName, algs);
		if (!OVERWRITE_CACHE && cache.containsKey(k))
		{
			System.err.println("is cached");// + mode + " " + datasetName + " " + algName + " " + k);
			setResult(datasetName, algs, cache.get(k), false);
			return true;
		}

		System.err.println("is not cached");
		return false;
	}

	public void setResult(String datasetName, int numCompounds, String embQual, Algorithm... algs)
	{
		setResult(datasetName, algs, new RuntimeResult(numCompounds, embQual, algs), true);
	}

	private void setResult(String datasetName, Algorithm[] algs, RuntimeResult res, boolean save)
	{
		int result = -1;
		for (int i = 0; i < resultSet.getNumResults(); i++)
		{
			if (resultSet.getResultValue(i, "Dataset").toString().equals(datasetName))
			{
				result = i;
				break;
			}
		}
		if (result == -1)
			result = resultSet.addResult();

		resultSet.setResultValue(result, "Dataset", datasetName);
		if (res.numCompounds > 0 && MODE == MappingTest.mapping_runtime_builder)
			resultSet.setResultValue(result, "Compounds", res.numCompounds);
		String algName = getAlgorithmName(findForMode(algs));
		algNames.add(algName);
		if (res.numFeatures != null
				&& (MODE == MappingTest.mapping_runtime_embedding || MODE == MappingTest.mapping_runtime_embedding2))
			resultSet.setResultValue(result, "Features", res.numFeatures);
		if (res.numClusters != null && (MODE == MappingTest.mapping_runtime_align))
			resultSet.setResultValue(result, "Cluster", res.numClusters);
		resultSet.setResultValue(result, algName, res.runtime);
		//		if (res.numFeatures != null && (MODE == MappingTest.mapping_runtime_features))
		//			resultSet.setResultValue(result, algName + " " + NUM_FEATURES, res.numFeatures);
		if (res.embQual != null
				&& (MODE == MappingTest.mapping_runtime_embedding || MODE == MappingTest.mapping_runtime_embedding2))
			resultSet.setResultValue(result, algName + " " + EMBED_QUAL, res.embQual);

		if (save)
		{
			Integer k = getKey(datasetName, algs);
			System.err.println("caching");// " + mode + " " + datasetName + " " + algName + " " + k);
			cache.put(k, res);
			try
			{
				ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(runtimeCacheFile));
				oos.writeObject(cache);
				oos.close();
			}
			catch (IOException e)
			{
				throw new RuntimeException(e);
			}
		}
	}

	public void print()
	{
		resultSet.setNicePropery("Compounds", "Size");

		//		resultSet.setNicePropery("CDK 3D Structure Generation", "CDK");
		//		resultSet.setNicePropery("OpenBabel 3D Structure Generation", "OpenBabel");

		//		resultSet.setNicePropery("CDKPropertiesCreator Constitutional", "CDK constitutional");
		//		resultSet.setNicePropery("CDKPropertiesCreator WithoutIonizationPotential", "CDK all");
		//		resultSet.setNicePropery("OBFingerprintCreator", "All OpenBabel Fingerprints");
		//		resultSet.setNicePropery("OBCarcMutRulesCreator", "52 Smarts OpenBabel");
		//		resultSet.setNicePropery("CDKCarcMutRulesCreator", "52 Smarts CDK OLD");
		//		resultSet.setNicePropery("CDKCarcMutRulesCreator2", "52 Smarts CDK");
		//		resultSet.setNicePropery("OBDescriptorCreator", "OpenBabel Descriptors");
		//		resultSet.setNicePropery("CDKFingerprintCreator", "CDK Bio-Activity Fragments");
		//
		//		resultSet.movePropertyBack("OBFingerprintCreator");
		//		resultSet.movePropertyBack("CDKFingerprintCreator");
		//		resultSet.movePropertyBack("OBCarcMutRulesCreator");
		//		resultSet.movePropertyBack("CDKCarcMutRulesCreator");
		//		resultSet.movePropertyBack("CDKCarcMutRulesCreator2");
		//
		//		resultSet.removePropery("CDKCarcMutRulesCreator");

		//		resultSet.setNicePropery("Hierarchical (WEKA)", "Hierarch (WEKA)");
		//		resultSet.setNicePropery("Hierarchical (R)", "Hierarch (R)");
		//		resultSet.setNicePropery("Hierarchical - Dynamic Tree Cut (R)", "Hierarch - Dynamic Tree Cut (R)");
		//		resultSet.setNicePropery("FarthestFirst (WEKA)", "Farthest First (WEKA)");
		//		resultSet.setNicePropery("SimpleKMeans (WEKA)", "Simple k-Means (WEKA)");
		//		resultSet.setNicePropery("k-Means - Cascade (WEKA)", "k-Means - Cascade (WEKA) *");
		//		resultSet.setNicePropery("k-Means - Cascade (WEKA) 3-5 r3", "k-Means - Cascade (WEKA) **");
		//		resultSet.setNicePropery("Expectation Maximization (WEKA)", "EM (WEKA) ***");
		//		resultSet.setNicePropery("Expectation Maximization (WEKA) 5", "EM (WEKA) ****");

		////				resultSet.setNicePropery("TSNE 3D Embedder (R) 1000", "TSNE 3D Embedder (R)*");
		////				resultSet.setNicePropery("TSNE 3D Embedder (R) 200", "TSNE 3D Embedder (R)**");
		//		resultSet.setNicePropery("SMACOF 3D Embedder (R) 150", "SMACOF 3D Embedder (R)*");
		//		resultSet.setNicePropery("SMACOF 3D Embedder (R) 30", "SMACOF 3D Embedder (R)**");

		for (String algName : algNames)
			resultSet.setNicePropery(algName, shortAlg.get(algName));
		for (String algName : algNames)
			resultSet.setNicePropery(algName + " " + NUM_FEATURES, NUM_FEATURES);
		for (String algName : algNames)
			resultSet.setNicePropery(algName + " " + EMBED_QUAL, EMBED_QUAL);
		//		resultSet.setNicePropery("SMACOF 3D Embedder (R) 150 " + EMBED_QUAL, EMBED_QUAL);
		//		resultSet.setNicePropery("SMACOF 3D Embedder (R) 30 " + EMBED_QUAL, EMBED_QUAL);
		//		resultSet.setNicePropery("SMACOF 3D Embedder (R) 10 " + EMBED_QUAL, EMBED_QUAL);

		//		resultSet.removePropery("SMACOF 3D Embedder (R) 10 r²");
		//		resultSet.removePropery("SMACOF 3D Embedder (R) 10");
		//
		//		resultSet.movePropertyBack("SMACOF 3D Embedder (R) 30");
		//		resultSet.movePropertyBack("SMACOF 3D Embedder (R) 30 r²");

		for (String p : resultSet.getProperties())
		{
			if (p.equals("Compounds") || p.equals("Cluster"))
				resultSet.toInt(p);
			else if (!p.equals("Dataset") && !p.equals("Features") && !p.equals("Cluster") && !p.endsWith(EMBED_QUAL)
					&& !p.endsWith(NUM_FEATURES))
				resultSet.toLong(p);
		}

		for (String p : resultSet.getProperties())
		{
			if (p.endsWith(EMBED_QUAL))
			{
				for (int i = 0; i < resultSet.getNumResults(); i++)
				{
					String q = resultSet.getResultValue(i, p).toString();
					int idx = q.lastIndexOf("(Pearson: ") + "(Pearson: ".length();
					resultSet.setResultValue(i, p, q.substring(idx, q.length() - 1));
				}
			}
		}

		for (int i = 0; i < resultSet.getNumResults(); i++)
		{
			String s = resultSet.getResultValue(i, "Dataset").toString();
			if (s.indexOf(".") != -1)
				resultSet.setResultValue(i, "Dataset", s.substring(0, s.indexOf(".")));
			s = resultSet.getResultValue(i, "Dataset").toString();
			if (s.indexOf("_") != -1)
				resultSet.setResultValue(i, "Dataset", s.substring(0, s.indexOf("_")));

			s = resultSet.getResultValue(i, "Dataset").toString();
			if (s.equals("caco2"))
				s = "Caco2";
			else if (s.equals("cox2"))
				s = "COX2";
			resultSet.setResultValue(i, "Dataset", s);
		}

		ResultSet algNameSet = new ResultSet();
		for (String name : algNames)
		{
			if (!shortAlg.containsKey(name))
			{
				System.err.println("missing:\n" + name);
				System.exit(1);
			}
			int idx = algNameSet.addResult();
			algNameSet.setResultValue(idx, "Algorithm", shortAlg.get(name));
			algNameSet.setResultValue(idx, "Full name", longAlg.containsKey(name) ? longAlg.get(name) : name);
		}
		algNameSet.setNicePropery("Algorithm", "");
		algNameSet.setNicePropery("Full name", "");

		File dest = new File("/home/martin/documents/diss/latex/new_tex/runtime_tables/tmp/" + MODE + ".tex");
		StringBuffer b = new StringBuffer();
		b.append("\n\\begin{table}\n\\small\n");
		if (MODE == MappingTest.mapping_runtime_clustering || MODE == MappingTest.mapping_runtime_embedding2)
			b.append("\\renewcommand{\\tabcolsep}{2px} %too wide otherwise\n");

		LatexTableSettings settings = new LatexTableSettings();
		settings.headerBold = true;
		settings.renderTime = true;
		b.append(resultSet.toLatexTable(settings) + "\n");
		settings = new LatexTableSettings();
		settings.firstRowBold = true;
		b.append(algNameSet.toLatexTable(settings) + "\n");
		b.append("\\caption{" + MODE.toString().replaceAll("_", " ") + "}\n\\end{table}\n");
		System.out.println(b.toString());
		System.out.println("\nlatex written to:\n" + dest);
		FileUtil.writeStringToFile(dest.getAbsolutePath(), b.toString());

		System.out.println();
		System.out.println(resultSet.toNiceString(0, true, true));
		System.out.println();
		System.out.println(algNameSet.toNiceString());

		//		System.out.println(resultSet.toMediaWikiString(true, true, true));
	}

	//	public void store()
	//	{
	//		File resultSetFile = new File(STATS_DIR + mode + ".txt");
	//		
	////		if (resultSetFile.exists())
	////			resultSet = ResultSetIO.parseFromFile(resultSetFile);
	////		else
	//			resultSet = new ResultSet();
	//
	//	   save();
	//				}
	//			}
	//		}
	//
	//		System.err.flush();
	//		print();
	//	}

	private static Algorithm find(Algorithm algs[], Class<?> clazz)
	{
		for (int i = 0; i < algs.length; i++)
			if (clazz.isAssignableFrom(algs[i].getClass()))
				return algs[i];
		return null;
	}

	private static Algorithm findForMode(Algorithm algs[])
	{
		switch (MODE)
		{
			case mapping_runtime_builder:
				return find(algs, ThreeDBuilder.class);
			case mapping_runtime_clustering:
				return find(algs, DatasetClusterer.class);
			case mapping_runtime_embedding:
			case mapping_runtime_embedding2:
				return find(algs, ThreeDEmbedder.class);
			case mapping_runtime_features:
			case mapping_runtime_features_ob:
				return find(algs, PseudoFeatureAlgorithm.class);
			case mapping_runtime_align:
				return find(algs, ThreeDAligner.class);
			default:
				throw new IllegalArgumentException();
		}
	}

	private static String[] getNames(Algorithm... algs)
	{
		String s[] = new String[algs.length];
		for (int i = 0; i < s.length; i++)
			s[i] = getAlgorithmName(algs[i]);
		return s;
	}

	private static String getAlgorithmName(Algorithm a)
	{
		if (a == new ClustererProvider().getYesAlgorithm())
			return a.getName() + " DEFAULT";
		else if (a == new EmbedderProvider().getYesAlgorithm())
			return a.getName() + " DEFAULT";
		else
			return a.getName();
	}

	public static class PseudoFeatureAlgorithm implements Algorithm
	{
		String name;
		long runtime;
		int numFeatures;

		public PseudoFeatureAlgorithm(String name, long runtime, int numFeatures)
		{
			this.name = name;
			this.runtime = runtime;
			this.numFeatures = numFeatures;
		}

		@Override
		public long getRuntime()
		{
			return runtime;
		}

		@Override
		public String getName()
		{
			return name;
		}

		public int getNumFeatures()
		{
			return numFeatures;
		}

		@Override
		public Property[] getProperties()
		{
			return null;
		}

		@Override
		public String getDescription()
		{
			return null;
		}

		@Override
		public Binary getBinary()
		{
			return null;
		}

		@Override
		public Messages getMessages(DatasetFile dataset, FeatureInfo featureInfo, DatasetClusterer clusterer)
		{
			return null;
		}

		@Override
		public Messages getProcessMessages()
		{
			return null;
		}

		@Override
		public Property getRandomSeedProperty()
		{
			return null;
		}

		@Override
		public Property getRandomRestartProperty()
		{
			return null;
		}

		@Override
		public void update(DatasetFile dataset)
		{
		}
	}

	static HashMap<String, String> shortAlg = new HashMap<>();
	static HashMap<String, String> longAlg = new HashMap<>();
	static
	{
		shortAlg.put("Maximum Common Subgraph (MCS) Aligner", "MCS");
		shortAlg.put("Maximum Structural Fragment Aligner", "Max Fragment");

		shortAlg.put("CDK 3D Structure Generation", "CDK");
		shortAlg.put("OpenBabel 3D Structure Generation", "OpenBabel");

		shortAlg.put("k-Means - Cascade (WEKA) DEFAULT", "CkM*");
		longAlg.put("k-Means - Cascade (WEKA) DEFAULT", "k-Means - Cascade (WEKA) -- Default Clusterer");
		shortAlg.put("SimpleKMeans (WEKA)", "kM");
		shortAlg.put("k-Means - Cascade (WEKA)", "CkM");
		shortAlg.put("FarthestFirst (WEKA)", "FF");
		shortAlg.put("Expectation Maximization (WEKA)", "EM");
		shortAlg.put("Cobweb (WEKA)", "CW");
		shortAlg.put("Hierarchical (WEKA)", "H");
		shortAlg.put("k-Means (R)", "kM R");
		shortAlg.put("k-Means - Cascade (R)", "CkM R");
		shortAlg.put("Hierarchical (R)", "H R");
		shortAlg.put("Hierarchical - Dynamic Tree Cut (R)", "DC R");

		shortAlg.put("PCA 3D Embedder (WEKA) DEFAULT", "PCA*");
		longAlg.put("PCA 3D Embedder (WEKA) DEFAULT", "PCA 3D Embedder (WEKA) -- Default 3D-Embedder");
		shortAlg.put("PCA 3D Embedder (R)", "PCA R");
		shortAlg.put("Sammon 3D Embedder (R)", "SM R");
		shortAlg.put("Sammon 3D Embedder (R) Tanimoto", "SM-T R");

		shortAlg.put("ob", "PC OB");
		longAlg.put("ob", "14 PC Descriptors created with Open Babel");
		shortAlg.put("cdk", "PC");
		longAlg.put("cdk", "271 PC Descriptors created with CDK");
		shortAlg.put("maccs f:-1 m:OpenBabel", "MC OB");
		longAlg.put("maccs f:-1 m:OpenBabel", "166 MACCS structural fragments matched with Open Babel");
		shortAlg.put("maccs f:-1 m:CDK", "MC");
		longAlg.put("maccs f:-1 m:CDK", "166 MACCS structural fragments matched with CDK");
		shortAlg.put("cdkFunct f:-1 m:CDK", "Funct");
		longAlg.put("cdkFunct f:-1 m:CDK", "307 Function groups matched with CDK");
		shortAlg.put("cdkBioAct f:-1 m:CDK", "Bio");
		longAlg.put("cdkBioAct f:-1 m:CDK", "4860 Klekota-Roth Biological Activity fragments matched with CDK");
		shortAlg.put("moss f:-1 m:OpenBabel", "MoSS");
		longAlg.put("moss f:-1 m:OpenBabel", "MoSS - Molecular Substructure Miner (10\\% min freq)");
		shortAlg.put("obFP2 f:-1 m:OpenBabel", "FP2 OB");
		longAlg.put("obFP2 f:-1 m:OpenBabel", "Linear fragments (FP2) mined with Open Babel (10\\% min freq)");
	}

}
