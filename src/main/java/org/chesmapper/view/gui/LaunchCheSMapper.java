package org.chesmapper.view.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.chesmapper.map.alg.build3d.AbstractReal3DBuilder;
import org.chesmapper.map.alg.build3d.OpenBabel3DBuilder;
import org.chesmapper.map.alg.build3d.AbstractReal3DBuilder.AutoCorrect;
import org.chesmapper.map.data.CDKCompoundIcon;
import org.chesmapper.map.data.ClusteringData;
import org.chesmapper.map.data.DatasetFile;
import org.chesmapper.map.data.fragments.MatchEngine;
import org.chesmapper.map.dataInterface.CompoundPropertyUtil;
import org.chesmapper.map.dataInterface.NumericProperty;
import org.chesmapper.map.gui.CheSMapperWizard;
import org.chesmapper.map.main.BinHandler;
import org.chesmapper.map.main.CheSMapping;
import org.chesmapper.map.main.PropHandler;
import org.chesmapper.map.main.ScreenSetup;
import org.chesmapper.map.main.Settings;
import org.chesmapper.map.main.TaskProvider;
import org.chesmapper.map.property.CDKDescriptor;
import org.chesmapper.map.property.PropertySetProvider;
import org.chesmapper.map.workflow.DatasetLoader;
import org.chesmapper.map.workflow.MappingWorkflow;
import org.chesmapper.map.workflow.MappingWorkflow.DescriptorSelection;
import org.chesmapper.map.workflow.MappingWorkflow.FragmentSettings;
import org.chesmapper.view.cluster.ClusterController;
import org.chesmapper.view.cluster.Clustering;
import org.chesmapper.view.cluster.Compound;
import org.chesmapper.view.cluster.ExportData;
import org.chesmapper.view.gui.CheSViewer.PostStartModifier;
import org.chesmapper.view.gui.ViewControler.HighlightMode;
import org.chesmapper.view.gui.ViewControler.Style;
import org.chesmapper.view.gui.util.CompoundPropertyHighlighter;
import org.chesmapper.view.gui.util.Highlighter;
import org.mg.javalib.gui.property.ColorGradient;
import org.mg.javalib.task.Task;
import org.mg.javalib.task.TaskDialog;
import org.mg.javalib.util.ArrayUtil;
import org.mg.javalib.util.FileUtil;
import org.mg.javalib.util.IntegerUtil;
import org.mg.javalib.util.ScreenUtil;
import org.mg.javalib.util.StringLineAdder;
import org.mg.javalib.util.SwingUtil;
import org.mg.javalib.util.ThreadUtil;
import org.mg.javalib.weka.WekaPropertyUtil;

public class LaunchCheSMapper
{
	private static boolean initialized = false;
	private static boolean showWarningDialogOnStartUp = true;

	public static void init()
	{
		init(true);
	}

	public static void init(boolean preLoadWeka)
	{
		init(Locale.US, ScreenSetup.DEFAULT, true, preLoadWeka);
	}

	public static synchronized void init(Locale locale, ScreenSetup screenSetup, boolean loadProps, boolean preLoadWeka)
	{
		if (initialized)
		{
			System.err.println("init only once!");
			return;
		}
		initialized = true;
		ScreenSetup.INSTANCE = screenSetup;

		Settings.LOGGER.info("Starting CheS-Mapper at " + new Date());
		Settings.LOGGER.info("OS is '" + System.getProperty("os.name") + "'");
		Settings.LOGGER.info("Java runtime version is '" + System.getProperty("java.runtime.version") + "'");
		Locale.setDefault(Locale.US);

		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				// takes some time, do this rightaway in extra thread
				CDKDescriptor.loadDescriptors();
			}
		}).start();
		if (preLoadWeka)
			new Thread(new Runnable()
			{
				public void run()
				{
					// takes some time, do this rightaway in extra thread
					WekaPropertyUtil.initWekaStuff();
				}
			}).start();

		PropHandler.init(loadProps);
		BinHandler.init();
	}

	public static long propertyModificationTime()
	{
		return PropHandler.modificationTime();
	}

	@SuppressWarnings("static-access")
	private static Option option(char charOpt, String longOpt, String description)
	{
		return OptionBuilder.withLongOpt(longOpt).withDescription(description).create(charOpt);
	}

	@SuppressWarnings("static-access")
	private static Option paramOption(char charOpt, String longOpt, String description, String paramName)
	{
		return OptionBuilder.withLongOpt(longOpt).withDescription(description).hasArgs(1).withArgName(paramName)
				.create(charOpt);
	}

	@SuppressWarnings("static-access")
	private static Option longParamOption(String longOpt, String description, String paramName)
	{
		return OptionBuilder.withLongOpt(longOpt).withDescription(description).hasArgs(1).withArgName(paramName)
				.create();
	}

	@SuppressWarnings("static-access")
	private static Option longOption(String longOpt, String description)
	{
		return OptionBuilder.withLongOpt(longOpt).withDescription(description).create();
	}

	public static void main(String args[])
	{
		if (args != null && args.length == 1 && args[0].equals("debug"))
		{
			//			setExitOnClose(false);
			//			init();
			//			start();
			//			ThreadUtil.sleep(10);
			//			SwingUtil.waitWhileVisible(CheSViewer.getFrame());
			//			System.err.println("X\nX\nX\nX\nX\nX\nSecond run\nX\nX\nX\nX\nX\nX\n");
			//			start();
			//			ThreadUtil.sleep(10);
			//			SwingUtil.waitWhileVisible(CheSViewer.getFrame());
			//			System.exit(0);
			args = new String[] { "-e", "-d", "/home/martin/workspace/CheS-Map-Test/data/3compounds.sdf", "-f", "ob",
					"--rem-missing-above-ratio", "1", "-o", "/tmp/test.csv" };

			//			args = "-s -f ob -d /home/martin/data/Tox21/TOX21S_v2a_8193_22Mar2012_cleanded.half2.sdf --big-data"
			//					.split(" ");

			//Settings.CACHING_ENABLED = false;
			//args = ("-s -d /home/martin/data/caco2.sdf -f integrated -i caco2").split(" ");
			//args = ("-x -d /home/martin/data/caco2.sdf -f integrated -i caco2 -o /home/martin/data/caco-workflow.ches").split(" ");
			//args = ("-w /tmp/delme.ches").split(" ");
			//			args = ("-r -e -d /home/martin/data/caco2.sdf -f cdk -o /tmp/caco-ob-features.csv --rem-missing-above-ratio 0.05")
			//					.split(" ");
			//			args = ("-e -d /home/martin/data/caco2.sdf -n 1 -f obFP3,obFP4,obMACCS -o /tmp/caco-fp-features.csv")
			//					.split(" ");
			//			args = ("-e -d /home/martin/data/caco2.sdf -n 1 -f cdk,obFP3,obFP4,obMACCS -o /tmp/caco-pcfp-features.csv")
			//					.split(" ");

			//			args = ("-r -e -d /home/martin/data/mixed.smi -f cdk -o /tmp/mixed-features.csv --rem-missing-above-ratio 0.05")
			//					.split(" ");

			//		args = ("-e -d /home/martin/workspace/BMBF-MLC/data/dataZ.sdf -f cdk -o /dev/null").split(" ");
			//			args = "-z -d /home/martin/workspace/BMBF-MLC/data/dataR.smi -o /home/martin/workspace/BMBF-MLC/data/dataR.sdf"
			//					.split(" ");
			//			args = "-n -d /home/martin/workspace/BMBF-MLC/data/dataR.smi -o /home/martin/workspace/BMBF-MLC/data/dataR.inchi"
			//					.split(" ");
			//			args = " -z -k -d /home/martin/workspace/BMBF-MLC/predictions/00e884588b8a6ba666fbdf29e9a75eda.smi -o /home/martin/workspace/BMBF-MLC/predictions/00e884588b8a6ba666fbdf29e9a75eda.sdf"
			//					.split(" ");
			//			args = "-e -n 10 -u -d /home/martin/data/caco2.sdf -f fminer -o /home/martin/tmp/delme.csv".split(" ");
			//			args = "-e -m -u -d /home/martin/workspace/BMBF-MLC/predictions/9712985d2d3cd4b067bcd77590ab10f0.sdf -f obFP3 -o /home/martin/tmp/delme.csv"
			//					.split(" ");
			//			args = "-z -k -d /home/martin/workspace/BMBF-MLC/predictions/aa53ca0b650dfd85c4f59fa156f7a2cc.smi -o /home/martin/workspace/BMBF-MLC/predictions/aa53ca0b650dfd85c4f59fa156f7a2cc.sdf"
			//					.split(" ");
			//args = "-z -d /tmp/test.smi -o /tmp/res.sdf".split(" ");

			//			args = "-e -d /home/martin/workspace/BMBF-MLC/data/dataR.sdf -f cdk,ob -o /home/martin/workspace/BMBF-MLC/features/dataR_PC.csv"
			//					.split(" ");
			//			args = ArrayUtil
			//					.toArray(StringUtil
			//							.split("-x -d /home/martin/data/test.csv -f integrated -i cas,cluster -a cluster -c \"Manual Cluster Assignment\" -q \"property-Cluster feature=cluster\" -o /tmp/delme.ches",
			//									' ')); // cannot use .split(" ") to respect quotes
			//args = "-e -d data/dataY.sdf -f cdk,ob -o features/dataY_PC2.sdf".split(" ");
			//args = "-w /home/martin/data/presentation/demo-ob-descriptors.ches".split(" ");
			//args = "-e  -d data/dataR.sdf -f obFP3 -o features/dataR_FP3.csv".split(" ");
			//			args = "-e  -d data/dataR.sdf -f fminer -n 20 -o features/dataR_fminer.csv".split(" ");
			//			args = "-y sxga+ -w /home/martin/data/presentation/demo-ob-descriptors.ches --font-size 20 --compound-style ballsAndSticks --compound-size 35 --highlight-mode Spheres --hide-compounds none"
			//					.split(" ");
			//args = "-y sxga+ -w /home/martin/data/presentation/demo-ob-descriptors.ches".split(" ");
			//			args = "-e -m -u -d predictions/068df623e8c42c1f01d9d04b93aebb4a.sdf -f cdk,ob,obFP3,obFP4,obMACCS -o predictions/068df623e8c42c1f01d9d04b93aebb4a_PCFP1.csv"
			//					.split(" ");

			//			args = "-y sxga+ -w /home/martin/data/presentation/cox2-clustered-aligned.ches --font-size 20 --compound-style ballsAndSticks --compound-size 15 --endpoint-highlight IC50_uM"
			//					.split(" ");
			//		args = "-h".split(" ");
			//args = "-z -d /home/martin/data/cor/test.smi -o /home/martin/data/cor/test.ches3d.sdf".split(" ");

			//			args = ArrayUtil
			//					.toArray(StringUtil
			//							.split("-x -d /home/martin/workspace/BMBF-MLC/pct/clusters_VarianceReduction/dataC_noV_Ca15-20c20_FP1.data.csv -o /home/martin/workspace/BMBF-MLC/pct/clusters_VarianceReduction/dataC_noV_Ca15-20c20_FP1.data.ches -f integrated -b \"OB-MACCS:N,OB-MACCS:OCO,OB-MACCS:O=A>1,OB-MACCS:CH3 > 2  (&...),OB-FP3:alkylaryl ether,OB-FP3:carboxylic acid,OB-FP4:Heteroaromatic,OB-MACCS:ACH2AACH2A,OB-FP4:1,3-Tautomerizable,OB-MACCS:Onot%A%A,OB-FP3:aldehyde or ketone,OB-MACCS:A$A!N,OB-FP4:Rotatable_bond,OB-MACCS:ACH2AAACH2A,OB-FP3:aryl,OB-FP4:Amine,OB-MACCS:C=O,OB-FP4:Hetero_N_basic_no_H,OB-FP3:HBD,OB-MACCS:AA(A)(A)A,OB-MACCS:NN,OB-MACCS:X!A$A,OB-MACCS:QA(Q)Q,OB-MACCS:S,OB-MACCS:NA(A)A,OB-MACCS:ACH2N,OB-MACCS:NAAO,OB-MACCS:O > 3 (&...),OB-MACCS:QAAAAA@1,OB-MACCS:N > 1,OB-FP4:Vinylogous_carbonyl_or_carboxyl_derivative,OB-MACCS:XA(A)A,OB-FP4:Primary_carbon,OB-FP4:Imidoylhalide_cyclic,OB-MACCS:NH2,OB-MACCS:Anot%A%Anot%A,OB-MACCS:NC(N)N,OB-FP4:Heterocyclic,OB-MACCS:QH > 1,OB-FP4:1,5-Tautomerizable,OB-MACCS:O > 2,OB-MACCS:CH3,OB-FP3:aniline,OB-FP3:nitro,OB-FP3:Ring,OB-MACCS:ACH2CH2A > 1,OB-MACCS:QO,OB-FP4:Alkylchloride,OB-MACCS:C=C,OB-FP4:Quaternary_carbon,OB-MACCS:C=C(C)C,OB-FP4:Vinylogous_ester,OB-MACCS:CH3AAACH2A,OB-MACCS:CH3AACH2A,OB-MACCS:S=A,OB-FP3:cation,OB-MACCS:8M Ring or larger. This only handles up to ring sizes of 14,OB-MACCS:BR,OB-MACCS:F,OB-MACCS:A!CH2!A,OB-MACCS:CH3CH2A,OB-MACCS:A$A!O > 1 (&...),OB-MACCS:OC(C)C,OB-FP4:Conjugated_double_bond,OB-FP4:Hetero_O,OB-MACCS:A!A$A!A,OB-FP4:Alkene,OB-MACCS:3M Ring,OB-FP4:Aldehyde,OB-MACCS:QCH2A>1 (&...),OB-MACCS:CH3ACH2A,OB-MACCS:ACH2O,OB-MACCS:CL\" -a leaf,level1,level2,level3,level4,level5,level6,level7,level8,level9,level10,level11,level12,level13,level14,level15,level16,level17,level18 -c \"Manual Cluster Assignment\" -q \"property-Cluster feature=leaf\"",
			//									' '));

			//args = "--add-obsolete-pc-features -e -m -u -d /home/martin/workspace/BMBF-MLC/predictions/305ff369acf4040ed85912a59924d042.sdf -f ob,obFP3,obFP4,obMACCS -o /home/martin/workspace/BMBF-MLC/predictions/305ff369acf4040ed85912a59924d042_MAN2.csv"
			//		.split(" ");

			//args = "-e -d /home/martin/data/pbde/PBDE_LogVP.ob3d.sdf -f obFP2 -o /tmp/delme.csv".split(" ");

			//			args = "-w /media/martin/Windows7_OS/Users/martin/Downloads/ches-mapper-presentation/demo-ob-descriptors.ches"
			//					.split(" ");
			//			args = "-y sxga+ -w /media/martin/Windows7_OS/Users/martin/Downloads/ches-mapper-presentation/demo-ob-descriptors.ches --font-size 18 --compound-style ballsAndSticks --compound-size 35 --highlight-mode Spheres --hide-compounds none"
			//					.split(" ");
			//			args = "-y sxga+ -w /media/martin/Windows7_OS/Users/martin/Downloads/ches-mapper-presentation/hamster.ches --font-size 18 --predict"
			//					.split(" ");

			//			try
			//			{
			//				File f = File.createTempFile("test", "bla");
			//				FileUtil.writeStringToFile(f.getAbsolutePath(), "test");
			//				System.out.println("created tmp: " + f);
			//				File f2 = new File(System.getProperty("user.home") + File.separator + ".ches-mapper" + File.separator
			//						+ "cache" + File.separator + "delme");
			//				System.out.println("rename to: " + f2);
			//				if (!FileUtil.robustRenameTo(f.getAbsolutePath(), f2.getAbsolutePath()))
			//					throw new Error("failed!");
			//				if (f.exists() || !f2.exists())
			//					throw new Error("failed2!");
			//				System.out.println("great, deleting: " + f2);
			//				f2.delete();
			//				System.out.println("done");
			//				System.exit(0);
			//			}
			//			catch (IOException e)
			//			{
			//				e.printStackTrace();
			//			}
		}

		StringLineAdder examples = new StringLineAdder();
		examples.add("Examples");
		examples.add("* directly start viewer caco2.sdf dataset with all integrated features apart from the endpoint feature caco2");
		examples.add("  -s -d data/caco2.sdf -f integrated -i caco2");
		examples.add("* export workflow-file for caco2.sdf dataset with all integrated features apart from the endpoint feature caco2");
		examples.add("  -x -d data/caco2.sdf -f integrated -i caco2 -o data/caco-workflow.ches");
		examples.add("* directly start ches-mapper with workflow-file");
		examples.add("  -w data/caco-workflow.ches");
		examples.add("* export open-babel descriptors for caco2.sdf dataset");
		examples.add("  -e -d data/caco2.sdf -f ob -o data/caco2-ob-features.csv");

		Options options = new Options();
		options.addOption(paramOption('y', "screen-setup",
				"for expert users, should be one of debug|screenshot|video|small_screen", "setup-mode"));
		options.addOption(longParamOption("window-size", "window size in <width>x<height>", "window-size"));

		options.addOption(option('p', "no-properties",
				"for expert users, prevent ches-mapper from reading property file with saved settings"));
		options.addOption(option('h', "help", "show this help output"));

		options.addOption(option('e', "export-features",
				"exports features (from dataset -d and features -f to outfile -o)"));
		options.addOption(longOption("add-obsolete-pc-features",
				"exports obsolete pc features for backward compatibility"));
		options.addOption(option('m', "match-fingerprints",
				"sets min-freq to 1 and mines omnipresent fingerprint features (eclusive with min frequency)"));
		options.addOption(option('r', "enable-mixture-handling",
				"enableds mixture handling for physico-chemical descriptors"));
		options.addOption(paramOption('n', "fp-min-frequency",
				"sets min-frequency for fingerprints (eclusive with match-fingerprints)", "fp-min-frequency"));
		options.addOption(option('u', "keep-uniform-values",
				"exports features including features with uniform feature values"));
		options.addOption(longParamOption("rem-missing-above-ratio",
				"remove features from export with too much missing values (0 <= missing-ratio <=1)", "missing-ratio"));
		options.addOption(option('x', "export-workflow",
				"creates a workflow-file (from dataset -d and features -f to outfile -o)"));
		options.addOption(paramOption(
				'q',
				"export-properties",
				"for experts only: additional property that are directly written into the worflow file (e.g.: k1=v1,k2=v2)",
				"export-properties"));
		options.addOption(option('s', "start-viewer", "directly starts the viewer (from dataset -d and features -f)"));
		options.addOption(paramOption('w', "start-workflow", "directly starts the viewer", "workflow-file"));

		options.addOption(paramOption('d', "dataset-file",
				"input file for export-features, export-workflow, start-viewer", "dataset-file"));
		options.addOption(paramOption('o', "outfile", "output file for export-features, export-workflow", "outfile"));
		options.addOption(paramOption(
				'f',
				"features",
				"specify features (comma seperated) : "
						+ ArrayUtil.toString(PropertySetProvider.PropertySetShortcut.values(), ",", "", "", ""),
				"features"));
		options.addOption(longParamOption("integrated-features",
				"comma seperated list of feature-names that should be used (from features -f)", "integrated-features"));
		options.addOption(longParamOption("ignore-features",
				"comma seperated list of integrated feature-names that should be ignored (from features -f)",
				"ignored-features"));
		options.addOption(longParamOption("numeric-features",
				"comma seperated list of integrated feature-names that should be interpreted as numeric",
				"numeric-features"));
		options.addOption(longParamOption("nominal-features",
				"comma seperated list of integrated feature-names that should be interpreted as nominal",
				"nominal-features"));
		//		List<String> clusterNames = new ArrayList<String>();
		//		for (Algorithm a : DatasetClusterer.CLUSTERERS)
		//			clusterNames.add(a.getName());
		//		options.addOption(paramOption('c', "cluster-algorithm",
		//				"specify cluster algorithm: " + ListUtil.toString(clusterNames, ", "), "cluster-algorithm"));
		options.addOption(paramOption('c', "cluster-algorithm", "specify cluster algorithm", "cluster-algorithm"));

		options.addOption(paramOption('t', "fix-3d-sdf-file",
				"replaces corrupt structures in input-file -t with structures from input-file -d, saves to outfile -o",
				"corrupt-3d-sdf-file"));
		options.addOption(paramOption(
				'v',
				"fix-3d-sdf-file-external",
				"replaces corrupt structures in input-file -v with structures derieved with external-script from smi-file -d, saves to outfile -o",
				"corrupt-3d-sdf-file"));

		options.addOption(option(
				'z',
				"compute-3d",
				"uses openbabel to compute a SDF file (-o) for the input-file -d (no auto-correction of openbabel errors like in gui, use -t or -v)"));
		options.addOption(paramOption('k', "depict-2d", "depicts 2d images for each compound in dataset file -d",
				"output-folder"));
		//		options.addOption(option('n', "compute-inchi", "computes inchi for dataset file -d, saves to outfile -o"));

		options.addOption(longParamOption("font-size", "change initial font size", "font-size"));
		options.addOption(longParamOption("compound-style", "change initial style", "compound-style"));
		options.addOption(longParamOption("compound-size", "change initial compound size", "compound-size"));
		options.addOption(longParamOption("highlight-mode", "change initial highlight mode", "highlight-mode"));
		options.addOption(longOption("predict", "predict activity"));
		//		options.addOption(longParamOption("hide-compounds", "change initial hide-compounds mode", "hide compounds"));
		options.addOption(longParamOption("endpoint-highlight",
				"enable endpoint-highlighting (log + reverse) for a feature", "endpoint-highlight feature"));
		options.addOption(longParamOption("select-compounds", "pre select compound/s (comma seperated compound index)",
				"selected compound/s"));
		options.addOption(longOption("background-white", "set background to white"));
		options.addOption(longOption("full-screen", "start in full-screen"));
		options.addOption(longOption("big-data",
				"start in big-data mode (show data points instead of compound structures)"));
		options.addOption(longOption("no-cache", "disable caching of embedding results"));

		options.addOption(longParamOption("display-no", "set number of display to start application on",
				"number of display"));

		options.addOption(longParamOption("autocorrect-3d", "corrects ob3d result (when using -z), possible values: "
				+ ArrayUtil.toString(AutoCorrect.values(), ",", "", "", ""), "autocorrect-3d"));
		options.addOption(longParamOption("distance-to",
				"adds distance to compound/s to export of features (-e), comma separated compound index",
				"compound indices"));
		options.addOption(longParamOption("distance-measure",
				"configure distance measure for distance-to option (euclidean or tanimoto)", "compound indices"));

		options.addOption(longOption("verbose", "print more messages"));
		options.addOption(longParamOption("keep-redundant",
				"keep-redundant features for mapping and exporting (default: false)", "true/false"));

		options.addOption(longOption("no-warning-dialog", "does not show mapping warning dialog on startup"));

		CommandLineParser parser = new BasicParser();
		try
		{
			final CommandLine cmd = parser.parse(options, args);

			PostStartModifier mod = new PostStartModifier()
			{
				@Override
				public void modify(final GUIControler gui, final ViewControler view,
						ClusterController clusterControler, final Clustering clustering)
				{
					if (cmd.hasOption("font-size"))
					{
						final Integer font = IntegerUtil.parseInteger(cmd.getOptionValue("font-size"));
						SwingUtilities.invokeLater(new Runnable()
						{
							public void run()
							{
								view.setFontSize(font);
							}
						});
						ThreadUtil.sleep(2000);
					}
					if (cmd.hasOption("compound-style"))
					{
						final Style style = Style.valueOf(cmd.getOptionValue("compound-style"));
						SwingUtilities.invokeLater(new Runnable()
						{
							public void run()
							{
								view.setStyle(style);
							}
						});
						ThreadUtil.sleep(2000);
					}
					if (cmd.hasOption("compound-size"))
					{
						final Integer size = IntegerUtil.parseInteger(cmd.getOptionValue("compound-size"));
						SwingUtilities.invokeLater(new Runnable()
						{
							public void run()
							{
								view.setCompoundSize(size);
							}
						});
						ThreadUtil.sleep(2000);
					}
					if (cmd.hasOption("highlight-mode"))
					{
						final HighlightMode mode = HighlightMode.valueOf(cmd.getOptionValue("highlight-mode"));
						SwingUtilities.invokeLater(new Runnable()
						{
							public void run()
							{
								view.setHighlightMode(mode);
							}
						});
						ThreadUtil.sleep(2000);
					}
					if (cmd.hasOption("background-white"))
					{
						SwingUtil.invokeAndWait(new Runnable()
						{
							@Override
							public void run()
							{
								view.setBackgroundBlack(false);
							}
						});
						ThreadUtil.sleep(2000);
					}
					//					if (cmd.hasOption("hide-compounds"))
					//					{
					//						DisguiseMode mode = DisguiseMode.valueOf(cmd.getOptionValue("hide-compounds"));
					//						view.setTranslucentCompounds(mode);
					//						ThreadUtil.sleep(2000);
					//					}
					if (cmd.hasOption("select-compounds"))
					{
						List<Compound> compounds = new ArrayList<Compound>();
						for (String idx : cmd.getOptionValue("select-compounds").split(","))
						{
							Integer index = Integer.parseInt(idx);
							compounds.add(clustering.getCompoundWithJmolIndex(index));
						}
						clusterControler.setCompoundActive(ArrayUtil.toArray(compounds), true);
						ThreadUtil.sleep(2000);
					}
					if (cmd.hasOption("endpoint-highlight"))
					{
						NumericProperty p = null;
						for (Highlighter h[] : view.getHighlighters().values())
							for (Highlighter hi : h)
								if (hi instanceof CompoundPropertyHighlighter)
									if (((CompoundPropertyHighlighter) hi).getProperty().toString()
											.equals(cmd.getOptionValue("endpoint-highlight")))
										p = (NumericProperty) ((CompoundPropertyHighlighter) hi).getProperty();
						if (p == null)
							throw new Error("feature not found: " + cmd.getOptionValue("endpoint-highlight"));
						p = clustering.addLogFeature(p);
						view.setHighlightColors(new ColorGradient(new Color(100, 255, 100), Color.WHITE,
								CompoundPropertyUtil.getHighValueColor()), new NumericProperty[] { p });
						ThreadUtil.sleep(2000);
					}
					if (cmd.hasOption("full-screen"))
					{
						SwingUtil.invokeAndWait(new Runnable()
						{
							@Override
							public void run()
							{
								gui.setFullScreen(true);
							}
						});
						ThreadUtil.sleep(2000);
					}
					if (cmd.hasOption("predict"))
					{
						SwingUtilities.invokeLater(new Runnable()
						{
							public void run()
							{
								gui.block("predict");
								try
								{
									clustering.predict();
								}
								finally
								{
									gui.unblock("predict");
								}
							}
						});
						ThreadUtil.sleep(2000);
					}
				}
			};

			if (cmd.hasOption('h'))
			{
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("java (-Xmx1g) -jar ches-mapper(-complete).jar", options);
				System.out.println("\n" + examples);
				System.exit(0);
			}

			if (cmd.hasOption("verbose"))
				TaskProvider.setPrintVerbose(true);
			if (cmd.hasOption("no-warning-dialog"))
				showWarningDialogOnStartUp = false;

			ScreenSetup screenSetup;
			if (cmd.hasOption('y'))
			{
				if (cmd.getOptionValue('y').equals("screenshot"))
					screenSetup = ScreenSetup.SCREENSHOT;
				else if (cmd.getOptionValue('y').equals("video"))
					screenSetup = ScreenSetup.VIDEO;
				else if (cmd.getOptionValue('y').equals("small_screen"))
					screenSetup = ScreenSetup.SMALL_SCREEN;
				else if (cmd.getOptionValue('y').equals("sxga+"))
					screenSetup = ScreenSetup.SXGA_PLUS;
				else if (cmd.getOptionValue('y').equals("default"))
					screenSetup = ScreenSetup.DEFAULT;
				else
					throw new Error("illegal screen setup-arg: " + cmd.getOptionValue('y'));
			}
			else
				screenSetup = ScreenSetup.DEFAULT;
			if (cmd.hasOption("window-size"))
			{
				String sc = cmd.getOptionValue("window-size");
				try
				{
					String s[] = sc.split("x");
					Dimension dim = new Dimension(Integer.parseInt(s[0]), Integer.parseInt(s[1]));
					screenSetup.setWizardSize(dim);
					screenSetup.setViewerSize(dim);
					screenSetup.setFullScreenSize(dim);
				}
				catch (Exception e)
				{
					throw new IllegalArgumentException("window-size should be <width>x<height> is: " + sc);
				}
			}

			boolean loadProperties = true;
			if (cmd.hasOption('p'))
				loadProperties = false;

			init(Locale.US, screenSetup, loadProperties, true);
			if (cmd.hasOption("keep-redundant"))
			{
				String o = cmd.getOptionValue("keep-redundant");
				if (o.equals("true"))
				{
					Settings.LOGGER
							.warn("redundant features will be used for mapping (option keep-redundant was given on startup)");
					Settings.SKIP_REDUNDANT_FEATURES = false;
				}
				else if (o.equals("false"))
				{
					Settings.LOGGER
							.warn("redundant features will be NOT used for mapping (option keep-redundant was given on startup)");
					Settings.SKIP_REDUNDANT_FEATURES = true;
				}
				else
					throw new IllegalArgumentException("value for keep-redundant should be true/false, but is " + o);
			}

			if (cmd.hasOption("display-no"))
			{
				Settings.TOP_LEVEL_FRAME_SCREEN = Integer.parseInt(cmd.getOptionValue("display-no"));
				if (ScreenUtil.getNumMonitors() <= Settings.TOP_LEVEL_FRAME_SCREEN)
				{
					Settings.LOGGER.warn("only " + ScreenUtil.getNumMonitors()
							+ " monitor/s found (ignoring display-no = " + Settings.TOP_LEVEL_FRAME_SCREEN + ")");
					Settings.TOP_LEVEL_FRAME_SCREEN = ScreenUtil.getLargestScreen();
				}
			}

			if (cmd.hasOption('r'))
			{
				Settings.DESC_MIXTURE_HANDLING = true;
				Settings.LOGGER.warn("mixture handling enabled");
			}
			if (cmd.hasOption("big-data"))
			{
				Settings.BIG_DATA = true;
				Settings.LOGGER.warn("big data mode enabled");
			}
			if (cmd.hasOption("no-cache"))
			{
				Settings.CACHING_ENABLED = false;
				Settings.LOGGER.warn("caching disabled");
			}

			if (cmd.hasOption('e')) // export features
			{
				if (cmd.hasOption("add-obsolete-pc-features"))
					Settings.CDK_SKIP_SOME_DESCRIPTORS = false;

				String infile = cmd.getOptionValue('d');
				String outfile = cmd.getOptionValue('o');
				String featureNames = cmd.getOptionValue('f');
				if (infile == null || outfile == null || featureNames == null)
					throw new ParseException(
							"please give dataset-file (-d) and features (-f) and outfile (-o) for feature export");
				DescriptorSelection features = DescriptorSelection.select(cmd.getOptionValue('f'),
						cmd.getOptionValue("integrated-features"), cmd.getOptionValue("ignore-features"),
						cmd.getOptionValue("numeric-features"), cmd.getOptionValue("nominal-features"));
				FragmentSettings fragmentSettings = null;
				if (cmd.hasOption('m'))
				{
					if (cmd.hasOption('n'))
						throw new IllegalArgumentException("exclusive settings n + m");
					fragmentSettings = new FragmentSettings(1, false, MatchEngine.OpenBabel);
				}
				else if (cmd.hasOption('n'))
					fragmentSettings = new FragmentSettings(Integer.parseInt(cmd.getOptionValue('n')), true,
							MatchEngine.OpenBabel);
				double missingRatio = 0;
				if (cmd.hasOption("rem-missing-above-ratio"))
					missingRatio = Double.parseDouble(cmd.getOptionValue("rem-missing-above-ratio"));

				List<Integer> compounds = new ArrayList<Integer>();
				if (cmd.hasOption("distance-to"))
					for (String idx : cmd.getOptionValue("distance-to").split(","))
						compounds.add(Integer.parseInt(idx));
				boolean euclidean = true;
				if (compounds.size() > 0 && cmd.hasOption("distance-measure"))
				{
					if (cmd.getOptionValue("distance-measure").equals("euclidean"))
						euclidean = true;
					else if (cmd.getOptionValue("distance-measure").equals("tanimoto"))
						euclidean = false;
					else
						throw new IllegalAccessError("unknown distance measure: "
								+ cmd.getOptionValue("distance-measure"));
				}
				ExportData.scriptExport(infile, features, fragmentSettings, outfile, cmd.hasOption('u'), missingRatio,
						compounds, euclidean);
			}
			else if (cmd.hasOption('x')) // export workflow
			{
				String infile = cmd.getOptionValue('d');
				String outfile = cmd.getOptionValue('o');
				String featureNames = cmd.getOptionValue('f');
				if (infile == null || outfile == null || featureNames == null)
					throw new ParseException(
							"please give dataset-file (-d) and features (-f) and outfile (-o) for workflow export");
				DescriptorSelection features = DescriptorSelection.select(cmd.getOptionValue('f'),
						cmd.getOptionValue("integrated-features"), cmd.getOptionValue("ignore-features"),
						cmd.getOptionValue("numeric-features"), cmd.getOptionValue("nominal-features"));
				MappingWorkflow.createAndStoreMappingWorkflow(infile, outfile, features, null,
						MappingWorkflow.clustererFromName(cmd.getOptionValue('c')), cmd.getOptionValue('q'));
			}
			else if (cmd.hasOption('w'))
			{
				CheSMapping mapping = MappingWorkflow.createMappingFromMappingWorkflow(cmd.getOptionValue('w'));
				start(mapping, mod);
			}
			else if (cmd.hasOption('s')) // direct start
			{
				String infile = cmd.getOptionValue('d');
				String featureNames = cmd.getOptionValue('f');
				CheSMapping mapping;
				if (infile == null && featureNames == null)
				{
					mapping = MappingWorkflow.createMappingFromMappingWorkflow(PropHandler.getProperties());
				}
				else
				{
					if (infile == null || featureNames == null)
						throw new ParseException("please give dataset-file (-d) and features (-f) to start viewer");
					DescriptorSelection features = DescriptorSelection.select(cmd.getOptionValue('f'),
							cmd.getOptionValue("integrated-features"), cmd.getOptionValue("ignore-features"),
							cmd.getOptionValue("numeric-features"), cmd.getOptionValue("nominal-features"));
					Properties workflow = MappingWorkflow.createMappingWorkflow(infile, features, null);
					mapping = MappingWorkflow.createMappingFromMappingWorkflow(workflow);
				}
				start(mapping, mod);
			}
			else if (cmd.hasOption('t'))
			{
				String infile = cmd.getOptionValue('d');
				String outfile = cmd.getOptionValue('o');
				if (infile == null || outfile == null)
					throw new ParseException("please give correct-2d-sdf-file (-d) and outfile (-o) for sdf-3d-fix");
				AbstractReal3DBuilder.check3DSDFile(cmd.getOptionValue('t'), infile, outfile, null);
			}
			else if (cmd.hasOption('v'))
			{
				String infile = cmd.getOptionValue('d');
				String outfile = cmd.getOptionValue('o');
				if (infile == null || outfile == null || !infile.endsWith("smi"))
					throw new ParseException(
							"please give correct-smi-file (-d) and outfile (-o) for sdf-3d-fix with external script");
				AbstractReal3DBuilder.check3DSDFileExternal(cmd.getOptionValue('v'), infile, outfile, null);
			}
			else if (cmd.hasOption('z'))
			{
				String infile = cmd.getOptionValue('d');
				String outfile = cmd.getOptionValue('o');
				if (infile == null || outfile == null)
					throw new ParseException("please give dataset-file (-d) and outfile (-o) for compute-3d");

				DatasetFile d = new DatasetLoader(false).load(infile);
				if (d == null)
					throw new Error("Could not load dataset file " + infile);
				if (cmd.hasOption('k'))
					CDKCompoundIcon.createIcons(d, cmd.getOptionValue('k'));

				OpenBabel3DBuilder builder = OpenBabel3DBuilder.INSTANCE;
				if (cmd.hasOption("autocorrect-3d"))
					builder.setAutoCorrect(AutoCorrect.valueOf(cmd.getOptionValue("autocorrect-3d")));
				else
					builder.setAutoCorrect(AutoCorrect.disabled);
				try
				{
					builder.build3D(d);
					if (!FileUtil.copy(builder.get3DSDFile(), outfile))
						throw new Error("Could not copy 3D-File to outfile " + outfile);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
			else if (cmd.hasOption('k'))
			{
				String infile = cmd.getOptionValue('d');
				if (infile == null)
					throw new ParseException("please give dataset-file (-d) for depict-2d");
				DatasetFile d = new DatasetLoader(false).load(infile);
				if (d == null)
					throw new Error("Could not load dataset file " + infile);
				CDKCompoundIcon.createIcons(d, cmd.getOptionValue('k'));
			}
			//			else if (cmd.hasOption('n'))
			//			{
			//				String infile = cmd.getOptionValue('d');
			//				String outfile = cmd.getOptionValue('o');
			//				if (infile == null || outfile == null)
			//					throw new ParseException("please give dataset-file (-d) and outfile (-o) for compute-3d");
			//				DatasetWizardPanel p = new DatasetWizardPanel(false);
			//				p.load(infile, true);
			//				if (p.getDatasetFile() == null)
			//					throw new Error("Could not load dataset file " + infile);
			//				String inichi[] = OBWrapper.computeInchiFromSmiles(
			//						BinHandler.BABEL_BINARY.getSisterCommandLocation("obabel"), p.getDatasetFile().getSmiles());
			//				FileUtil.writeStringToFile(outfile, ArrayUtil.toString(inichi, "\n", "", "", "") + "\n");
			//			}
			else
				start(null, mod);
		}
		catch (ParseException e)
		{
			System.out.println();
			System.out.flush();
			e.printStackTrace();
			System.err.flush();
			System.out.println("\nCould not parse command line options\n" + e.getMessage() + "\n");
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("java (-Xmx1g) -jar ches-mapper(-complete).jar", options);
			System.out.println("\n" + examples);
			System.exit(1);
		}
	}

	private static boolean exitOnClose = true;

	public static void setExitOnClose(boolean exit)
	{
		exitOnClose = exit;
	}

	public static void exit(JFrame f)
	{
		if (exitOnClose)
			System.exit(0);
		else
		{
			if (f != null && f.isVisible())
				f.setVisible(false);
			if (Settings.TOP_LEVEL_FRAME != null && Settings.TOP_LEVEL_FRAME != f
					&& Settings.TOP_LEVEL_FRAME.isVisible())
				Settings.TOP_LEVEL_FRAME.setVisible(false);
		}
	}

	public static void start()
	{
		start(null);
	}

	public static void start(CheSMapping mapping)
	{
		start(mapping, null);
	}

	public static void start(final CheSMapping preMapping, final PostStartModifier mod)
	{
		if (!initialized)
			throw new IllegalStateException("not initialized!");

		CheSMapping mapping = preMapping;
		if (mapping == null)
		{
			CheSMapperWizard wwd = null;
			while (wwd == null || wwd.getReturnValue() == CheSMapperWizard.RETURN_VALUE_IMPORT)
			{
				wwd = new CheSMapperWizard(null);
				SwingUtil.waitWhileVisible(wwd);
			}
			if (wwd.getReturnValue() == CheSMapperWizard.RETURN_VALUE_FINISH)
				mapping = wwd.getChesMapping();
		}
		if (mapping == null) //wizard cancelled
		{
			exit(null);
			return;
		}

		Task task = TaskProvider.initTask("Chemical space mapping of " + mapping.getDatasetFile().getName());
		TaskDialog waitingDialog = new TaskDialog(task, Settings.TOP_LEVEL_FRAME_SCREEN);
		final ClusteringData clusteringData = mapping.doMapping();
		if (clusteringData == null) //mapping failed
		{
			TaskProvider.removeTask();
			start();
			return;
		}
		clusteringData.setCheSMappingWarningOwner(waitingDialog);

		try
		{ // starting Viewer
			SwingUtil.invokeAndWait(new Runnable()
			{
				@Override
				public void run()
				{
					CheSViewer.show(clusteringData, mod);
				}
			});
			while (!CheSViewer.getFrame().isShowing())
				ThreadUtil.sleep(100);

			waitingDialog.setShowWarningDialog(showWarningDialogOnStartUp);
			waitingDialog.setWarningDialogOwner(CheSViewer.getFrame());
			task.finish();
		}
		catch (Throwable e)
		{
			Settings.LOGGER.error(e);
			TaskProvider.failed("Could not load viewer", e);
			System.gc();
			start();
		}
		finally
		{
			TaskProvider.removeTask();
		}
	}
}
