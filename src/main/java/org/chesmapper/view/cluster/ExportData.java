package org.chesmapper.view.cluster;

import java.awt.BorderLayout;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.chesmapper.map.alg.embed3d.ThreeDEmbedder;
import org.chesmapper.map.alg.embed3d.WekaPCA3DEmbedder;
import org.chesmapper.map.alg.embed3d.r.Sammon3DEmbedder;
import org.chesmapper.map.data.ClusteringData;
import org.chesmapper.map.data.DatasetFile;
import org.chesmapper.map.dataInterface.CompoundData;
import org.chesmapper.map.dataInterface.CompoundProperty;
import org.chesmapper.map.dataInterface.CompoundPropertyUtil;
import org.chesmapper.map.dataInterface.NominalProperty;
import org.chesmapper.map.dataInterface.NumericProperty;
import org.chesmapper.map.main.CheSMapping;
import org.chesmapper.map.main.Settings;
import org.chesmapper.map.workflow.MappingWorkflow;
import org.chesmapper.map.workflow.MappingWorkflow.DescriptorSelection;
import org.chesmapper.map.workflow.MappingWorkflow.FragmentSettings;
import org.chesmapper.view.gui.LaunchCheSMapper;
import org.mg.javalib.gui.Message;
import org.mg.javalib.gui.MessageLabel;
import org.mg.javalib.io.SDFUtil;
import org.mg.javalib.util.ArrayUtil;
import org.mg.javalib.util.DoubleKeyHashMap;
import org.mg.javalib.util.FileUtil;
import org.mg.javalib.util.ObjectUtil;
import org.mg.javalib.util.StringUtil;

public class ExportData
{
	public static void exportAll(Clustering clustering, CompoundProperty compoundDescriptorFeature, Script script)
	{
		List<Integer> l = new ArrayList<Integer>();
		for (Compound m : clustering.getCompounds(false))
			l.add(m.getOrigIndex());
		exportCompoundsWithOrigIndices(clustering, l, compoundDescriptorFeature, script);
	}

	public static void exportClusters(Clustering clustering, int clusterIndices[],
			CompoundProperty compoundDescriptorFeature)
	{
		List<Integer> l = new ArrayList<Integer>();
		for (int i = 0; i < clusterIndices.length; i++)
			for (Compound m : clustering.getCluster(clusterIndices[i]).getCompounds())
				l.add(m.getOrigIndex());
		exportCompoundsWithOrigIndices(clustering, l, compoundDescriptorFeature, null);
	}

	public static void exportCompoundsWithOrigIndices(Clustering clustering, List<Integer> compoundOrigIndices,
			CompoundProperty compoundDescriptorFeature)
	{
		exportCompoundsWithOrigIndices(clustering, ArrayUtil.toPrimitiveIntArray(compoundOrigIndices),
				compoundDescriptorFeature, null);
	}

	public static void exportCompoundsWithOrigIndices(Clustering clustering, List<Integer> compoundOrigIndices,
			CompoundProperty compoundDescriptorFeature, Script script)
	{
		exportCompoundsWithOrigIndices(clustering, ArrayUtil.toPrimitiveIntArray(compoundOrigIndices),
				compoundDescriptorFeature, script);
	}

	public static void exportCompoundsWithOrigIndices(Clustering clustering, int compoundOrigIndices[],
			CompoundProperty compoundDescriptorFeature)
	{
		exportCompoundsWithOrigIndices(clustering, compoundOrigIndices, compoundDescriptorFeature, null);
	}

	public static class Script
	{
		String dest;
		boolean allFeatures;
		public boolean skipEqualValues;
		public double skipNullValueRatio;

		public Script(String dest, boolean allFeatures, boolean skipEqualValues, double skipNullValueRatio)
		{
			this.dest = dest;
			this.allFeatures = allFeatures;
			this.skipEqualValues = skipEqualValues;
			this.skipNullValueRatio = skipNullValueRatio;
		}
	}

	public static void exportCompoundsWithOrigIndices(Clustering clustering, int compoundOrigIndices[],
			CompoundProperty compoundDescriptorFeature, Script script)
	{
		CompoundProperty selectedProps[];
		if (script != null && script.allFeatures)
		{
			List<CompoundProperty> availableProps = new ArrayList<CompoundProperty>();
			for (CompoundProperty p : clustering.getProperties())
				availableProps.add(p);
			for (CompoundProperty p : clustering.getFeatures())
				availableProps.add(p);
			for (CompoundProperty p : clustering.getAdditionalProperties())
				if (p instanceof DistanceToProperty)
					availableProps.add(p); // when scripting do not add embedding stress
			if (availableProps.size() == 0) // no features to select
				selectedProps = new CompoundProperty[0];
			selectedProps = ArrayUtil.toArray(CompoundProperty.class, availableProps);
		}
		else
		{
			selectedProps = ArrayUtil.toArray(CompoundProperty.class, clustering.selectPropertiesAndFeaturesWithDialog(
					"Select features for SDF/CSV export", null, true, true, true, true));
			if (selectedProps == null)//pressed cancel
				return;
		}

		DoubleKeyHashMap<Integer, String, Object> featureValues = new DoubleKeyHashMap<Integer, String, Object>();
		for (Integer j : compoundOrigIndices)
		{
			if (clustering.numClusters() > 1)
			{
				if (clustering.isClusterAlgorithmDisjoint())
				{
					Compound m = null;
					for (Cluster c : clustering.getClusters())
						for (Compound mm : c.getCompounds())
							if (mm.getOrigIndex() == j)
							{
								m = mm;
								break;
							}
					featureValues.put(j, (clustering.getClusterAlgorithm() + " cluster assignement").replace(' ', '_'),
							clustering.getClusterIndexForCompound(m));
				}
				else
				{
					for (Cluster c : clustering.getClusters())
					{
						if (!c.containsNotClusteredCompounds())
						{
							Compound m = null;
							for (Compound mm : c.getCompounds())
								if (mm.getOrigIndex() == j)
								{
									m = mm;
									break;
								}
							featureValues.put(j, (clustering.getClusterAlgorithm() + " " + c.getName()).replace(' ',
									'_'), m == null ? 0 : 1);

						}
					}
				}
			}

			for (CompoundProperty p : selectedProps)
				if (!p.getName().matches("(?i)smiles"))
				{
					String prop = CompoundPropertyUtil.propToExportString(p);
					Object val;
					if (p instanceof NumericProperty)
					{
						val = clustering.getCompounds().get(j).getDoubleValue((NumericProperty) p);
						if (val != null && ((NumericProperty) p).isInteger())
							val = StringUtil.formatDouble((Double) val, 0);
					}
					else
						val = clustering.getCompounds().get(j).getStringValue((NominalProperty) p);
					if (val == null)
						val = "";
					featureValues.put(j, prop, val);
				}
		}
		List<String> skipRedundant = new ArrayList<String>();
		for (CompoundProperty p : CompoundPropertyUtil.getRedundantFeatures(ArrayUtil.toList(selectedProps),
				compoundOrigIndices).keySet())
			skipRedundant.add(CompoundPropertyUtil.propToExportString(p));
		List<String> skipUniform = new ArrayList<String>();
		List<String> skipNull = new ArrayList<String>();
		List<String> skip = new ArrayList<String>();
		if (featureValues.keySet1().size() > 0 && featureValues.keySet2(compoundOrigIndices[0]).size() > 0)
			for (String prop : featureValues.keySet2(compoundOrigIndices[0]))
			{
				boolean uniform = true;
				int nullValueCount = 0;
				boolean first = true;
				Object val = null;
				for (Integer j : compoundOrigIndices)
				{
					Object newVal = featureValues.get(j, prop);
					if ((newVal == null || newVal.equals("") || new Double(Double.NaN).equals(newVal)))
						nullValueCount++;
					if (first)
					{
						first = false;
						val = newVal;
					}
					else
					{
						if (!ObjectUtil.equals(val, newVal))
							uniform = false;
					}
				}
				if (uniform && compoundOrigIndices.length > 1)
					skipUniform.add(prop);
				if (nullValueCount > 0)
				{
					double ratio = 0;
					if (compoundOrigIndices.length > 1)
						ratio = nullValueCount / (double) compoundOrigIndices.length;
					if (script == null || ratio > script.skipNullValueRatio)
					{
						if (script != null)
							Settings.LOGGER.info("null value ratio " + ratio + " > " + script.skipNullValueRatio
									+ ", skipping from export: " + prop + " ");
						skipNull.add(prop);
					}
				}
			}
		if (skipUniform.size() > 0)
		{
			boolean doSkip;
			if (script != null)
				doSkip = script.skipEqualValues;
			else
			{
				String msg = skipUniform.size()
						+ "/"
						+ featureValues.keySet2(compoundOrigIndices[0]).size()
						+ " feature/s have equal values for each compound.\nThese feature/s contain no information to distiguish between compounds.\nSkip from export?";
				int sel = JOptionPane.showConfirmDialog(Settings.TOP_LEVEL_FRAME, msg, "Skip feature",
						JOptionPane.YES_NO_OPTION);
				doSkip = sel == JOptionPane.YES_OPTION;
			}
			if (doSkip)
				for (String p : skipUniform)
				{
					if (skipNull.contains(p))
						skipNull.remove(p);
					if (skipRedundant.contains(p))
						skipRedundant.remove(p);
					Settings.LOGGER.info("uniform values, skipping from export: " + p + " ");
					skip.add(p);
				}
		}
		if (skipNull.size() > 0)
		{
			boolean doSkip;
			if (script != null)
				doSkip = true;
			else
			{
				String msg = skipNull.size()
						+ "/"
						+ (featureValues.keySet2(compoundOrigIndices[0]).size() - skip.size())
						+ " feature/s have missing values.\nMissing values might cause problems when post-processing the exported compounds.\nSkip from export?";
				int sel = JOptionPane.showConfirmDialog(Settings.TOP_LEVEL_FRAME, msg, "Skip feature",
						JOptionPane.YES_NO_OPTION);
				doSkip = sel == JOptionPane.YES_OPTION;
			}
			if (doSkip)
				for (String p : skipNull)
				{
					if (script == null)
						Settings.LOGGER.info("null values, skipping from export: " + p + " ");
					if (skipRedundant.contains(p))
						skipRedundant.remove(p);
					skip.add(p);
				}
		}
		if (skipRedundant.size() > 0)
		{
			boolean doSkip;
			if (script != null)
				doSkip = Settings.SKIP_REDUNDANT_FEATURES;
			else
			{
				String msg = skipRedundant.size()
						+ "/"
						+ (featureValues.keySet2(compoundOrigIndices[0]).size() - skip.size())
						+ " feature/s are redundant.\nThe information encoded in these feature/s is already provided by other feature/s.\nSkip from export?";
				int sel = JOptionPane.showConfirmDialog(Settings.TOP_LEVEL_FRAME, msg, "Skip feature",
						JOptionPane.YES_NO_OPTION);
				doSkip = sel == JOptionPane.YES_OPTION;
			}
			if (doSkip)
				for (String p : skipRedundant)
				{
					if (script == null)
						Settings.LOGGER.info("redundant values, skipping from export: " + p + " ");
					skip.add(p);
				}
		}
		for (String p : skip)
		{
			for (Integer j : compoundOrigIndices)
				featureValues.remove(j, p);
		}

		String dest;
		if (script != null)
			dest = script.dest;
		else
		{
			String dir = clustering.getOrigLocalPath();
			if (dir == null)
				dir = System.getProperty("user.home");
			JFileChooser f = new JFileChooser(dir);//origSDFFile);
			f.setDialogTitle("Save to SDF/CSV file (according to filename extension)");
			JPanel p = new JPanel(new BorderLayout());
			MessageLabel m = new MessageLabel(
					Message.infoMessage("By default the data is exportet in SDF format. Add '.csv' to the filename to export in CSV format."));
			m.setBorder(new EmptyBorder(5, 5, 5, 5));
			p.add(m, BorderLayout.NORTH);
			f.setAccessory(p);
			//			MessagePanel p = new MessagePanel();
			//			p.add

			int i = f.showSaveDialog(Settings.TOP_LEVEL_FRAME);
			if (i != JFileChooser.APPROVE_OPTION)
				return;
			dest = f.getSelectedFile().getAbsolutePath();
			if (!f.getSelectedFile().exists() && !FileUtil.getFilenamExtension(dest).matches("(?i)sdf")
					&& !FileUtil.getFilenamExtension(dest).matches("(?i)csv"))
				dest += ".sdf";
			if (new File(dest).exists())
			{
				if (JOptionPane.showConfirmDialog(Settings.TOP_LEVEL_FRAME, "File '" + dest
						+ "' already exists, overwrite?", "Warning", JOptionPane.YES_NO_OPTION,
						JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION)
					return;
			}
		}
		boolean csvExport = FileUtil.getFilenamExtension(dest).matches("(?i)csv");
		// file may be overwritten, and then reloaded -> clear
		DatasetFile.clearFiles(dest);
		if (csvExport)
		{
			List<String> feats = new ArrayList<String>();
			for (Integer j : compoundOrigIndices)
				if (featureValues.keySet1().size() > 0 && featureValues.keySet2(j) != null)
					for (String feat : featureValues.keySet2(j))
						if (!feats.contains(feat))
							feats.add(feat);
			File file = new File(dest);
			try
			{
				BufferedWriter b = new BufferedWriter(new FileWriter(file));
				Set<String> featNames = new HashSet<String>();
				b.write("\"SMILES\"");
				for (Object feat : feats)
				{
					b.write(",\"");
					String featName = feat.toString();
					int mult = 2;
					while (featNames.contains(featName))
						featName = feat.toString() + "_" + (mult++);
					featNames.add(featName);
					b.write(featName);
					b.write("\"");
				}
				b.write("\n");
				for (Integer compoundIndex : compoundOrigIndices)
				{
					CompoundData c = clustering.getCompounds().get(compoundIndex);
					b.write("\"");
					b.write(c.getSmiles());
					b.write("\"");
					for (String feat : feats)
					{
						b.write(",\"");
						Object val = featureValues.get(compoundIndex, feat);
						String s = val == null ? "" : val.toString();
						if (s.contains("\""))
						{
							System.err.println("csv export: replacing \" with ' for feature " + feat + " and value "
									+ s);
							s = s.replace('"', '\'');
						}
						b.write(s);
						b.write("\"");
					}
					b.write("\n");
				}
				b.close();
			}
			catch (IOException e)
			{
				throw new Error(e);
			}
		}
		else
		{
			HashMap<Integer, Object> newTitle = null;
			if (compoundDescriptorFeature != null)
			{
				newTitle = new HashMap<Integer, Object>();
				for (Integer j : compoundOrigIndices)
				{
					Object val;
					if (compoundDescriptorFeature instanceof NumericProperty)
					{
						val = clustering.getCompounds().get(j)
								.getDoubleValue((NumericProperty) compoundDescriptorFeature);
						if (val != null && ((NumericProperty) compoundDescriptorFeature).isInteger())
							val = StringUtil.formatDouble((Double) val, 0);
					}
					else
						val = clustering.getCompounds().get(j)
								.getStringValue((NominalProperty) compoundDescriptorFeature);
					if (val == null)
						val = "";
					featureValues.put(j, CompoundPropertyUtil.propToExportString(compoundDescriptorFeature), val);
					newTitle.put(j, val);
				}
			}
			SDFUtil.filter(clustering.getOrigSDFile(), dest, compoundOrigIndices, featureValues, true, newTitle);
			//SDFUtil.filter(clustering.getSDFile(), dest, compoundOrigIndices, featureValues, true, newTitle);
		}

		String msg = "Successfully exported " + compoundOrigIndices.length + " compounds to\n" + dest;
		if (script != null)
			System.out.println("\n" + msg);
		else
			JOptionPane
					.showMessageDialog(Settings.TOP_LEVEL_FRAME, msg, "Export done", JOptionPane.INFORMATION_MESSAGE);
	}

	public static void scriptExport(String datasetFile, DescriptorSelection features,
			FragmentSettings fragmentSettings, String outfile, boolean keepUniform, double missingRatio,
			List<Integer> distanceToCompounds, boolean euclideanDistance)
	{
		ThreeDEmbedder embed = null;
		if (distanceToCompounds != null && euclideanDistance)
			embed = WekaPCA3DEmbedder.INSTANCE;
		else if (distanceToCompounds != null && !euclideanDistance)
		{
			embed = Sammon3DEmbedder.INSTANCE;
			((Sammon3DEmbedder) embed).enableTanimoto();
		}

		Properties props = MappingWorkflow.createMappingWorkflow(datasetFile, features, fragmentSettings, null, embed);
		CheSMapping mapping = MappingWorkflow.createMappingFromMappingWorkflow(props, "");
		ClusteringData clusteringData = mapping.doMapping();
		ClusteringImpl clustering = new ClusteringImpl();
		clustering.newClustering(clusteringData);
		if (distanceToCompounds != null)
			for (Integer i : distanceToCompounds)
				clustering.addDistanceToCompoundFeature(clustering.getCompoundWithJmolIndex(i));

		scriptExport(clustering, outfile, keepUniform, missingRatio);
	}

	public static void scriptExport(Clustering clustering, String outfile, boolean keepUniform, double missingRatio)
	{
		//		LaunchCheSMapper.start(mapping);
		//		while (CheSViewer.getFrame() == null || CheSViewer.getClustering() == null)
		//			ThreadUtil.sleep(100);
		//		ExportData.exportAll(CheSViewer.getClustering(), null, new Script(outfile, true, true, true));

		ExportData.exportAll(clustering, null, new Script(outfile, true, !keepUniform, missingRatio));
		//LaunchCheSMapper.exit(CheSViewer.getFrame());
		LaunchCheSMapper.exit(null);
	}

	public static void main(String[] args)
	{
		LaunchCheSMapper.init();

		//String input = "/home/martin/data/valium.csv";
		String input = "/home/martin/data/caco2.sdf";
		scriptExport(input, DescriptorSelection.autoSelectIntegrated(), null, "/tmp/data.csv", false, 0.1, null, false);

	}

}
