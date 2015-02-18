package org.chesmapper.view.gui.table;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.chesmapper.map.data.DistanceUtil;
import org.chesmapper.map.dataInterface.CompoundData;
import org.chesmapper.map.dataInterface.CompoundProperty;
import org.chesmapper.map.dataInterface.NominalProperty;
import org.chesmapper.map.main.ScreenSetup;
import org.chesmapper.map.main.Settings;
import org.chesmapper.view.cluster.Cluster;
import org.chesmapper.view.cluster.ClusterController;
import org.chesmapper.view.cluster.Clustering;
import org.chesmapper.view.cluster.Compound;
import org.chesmapper.view.cluster.CompoundFilterImpl;
import org.chesmapper.view.cluster.Clustering.SelectionListener;
import org.chesmapper.view.gui.ComponentSize;
import org.chesmapper.view.gui.GUIControler;
import org.chesmapper.view.gui.ViewControler;
import org.mg.javalib.dist.NonNullSimilartiy;
import org.mg.javalib.dist.SimilarityMeasure;
import org.mg.javalib.dist.SimpleMatchingSimilartiy;
import org.mg.javalib.dist.TanimotoSimilartiy;
import org.mg.javalib.gui.BlockableFrame;
import org.mg.javalib.util.ArrayUtil;
import org.mg.javalib.util.CountedSet;
import org.mg.javalib.util.DoubleArraySummary;
import org.mg.javalib.util.ListUtil;
import org.mg.javalib.util.ObjectUtil;
import org.mg.javalib.util.StringUtil;
import org.mg.javalib.util.SwingUtil;

public class TreeView extends BlockableFrame
{
	private static int ICON_W = 50;
	private static int ICON_H = 50;

	protected ViewControler viewControler;
	protected Clustering clustering;
	protected ClusterController clusterControler;
	protected GUIControler guiControler;
	protected boolean selfUpdate;

	HashMap<Compound, MyNode> map = new HashMap<Compound, TreeView.MyNode>();
	HashMap<Cluster, MyNode> mapCluster = new HashMap<Cluster, TreeView.MyNode>();

	JTree tree;

	boolean tanimotoWorking;

	public static enum Crit
	{
		size, uniform, activity, nullRatio;
	}

	//	static HashMap<Cluster, Double> weightedTox = new HashMap<>();

	public class MyNode extends DefaultMutableTreeNode
	{
		Cluster cluster;
		List<Compound> leafs;
		List<MyNode> clusterNodes;
		private CompoundProperty compoundProperty;
		DoubleArraySummary avgUniformity;
		DoubleArraySummary avgActivity;
		DoubleArraySummary avgNullRatio;
		Double uniformity;
		Double activity;
		Double nullRatio;
		int numAllNull = -1;
		String feature;

		public MyNode(Object o, String feature)
		{
			super(o);
			if (o instanceof Compound)
				map.put((Compound) o, this);
			this.feature = feature;
		}

		public MyNode(String s, String feature, CompoundProperty p)
		{
			this(s, feature);
			this.compoundProperty = p;
		}

		public Cluster getCluster()
		{
			if (leafs == null)
				getLeafs();
			return cluster;
		}

		@SuppressWarnings("unchecked")
		public List<Compound> getLeafs()
		{
			if (leafs == null)
			{
				leafs = new ArrayList<Compound>();
				for (int i = 0; i < getChildCount(); i++)
					if (getChildAt(i).isLeaf())
						leafs.add((Compound) ((MyNode) getChildAt(i)).getUserObject());
					else
						leafs = ListUtil.concat(leafs, ((MyNode) getChildAt(i)).getLeafs());
				cluster = clustering.getUniqueClusterForCompounds(ArrayUtil.toArray(leafs));
				if (cluster != null && cluster.getNumCompounds() != leafs.size())
					cluster = null;
				if (cluster != null)
					mapCluster.put(cluster, this);
			}
			return leafs;
		}

		TanimotoSimilartiy tanimotoSim = new TanimotoSimilartiy();
		SimpleMatchingSimilartiy simpleMatchingSim = new SimpleMatchingSimilartiy();
		NonNullSimilartiy nonNullSim = new NonNullSimilartiy();

		public Double getStructuralSimilarity()
		{
			if (tanimotoWorking)
				return getSimilarity(clustering.getFeatures(), tanimotoSim);

			List<Double> distances = new ArrayList<Double>();
			List<Compound> compounds = getLeafs();
			for (int i = 0; i < compounds.size() - 1; i++)
				for (int j = i + 1; j < compounds.size(); j++)
					distances.add(clustering.getFeatureDistance(compounds.get(i).getOrigIndex(), compounds.get(j)
							.getOrigIndex()));
			return DoubleArraySummary.create(distances).getMean();
		}

		public Double getToxSimilarity()
		{
			return getSimilarity(activityProps, simpleMatchingSim);
		}

		public Double getWeightedTox()
		{
			//			if (!weightedTox.containsKey(cluster))
			//			{
			//				List<Double> toxWeight = new ArrayList<Double>();
			//				for (Cluster c : clustering.getClusters())
			//					toxWeight.add(mapCluster.get(c).getToxNonMissing());
			//				if (toxWeight.contains(null))
			//					throw new IllegalStateException();
			//				Double normalizedToxWeight[] = ArrayUtil.normalize(ArrayUtil.toArray(toxWeight), false);
			//
			//				int i = 0;
			//				for (Cluster c : clustering.getClusters())
			//					if (mapCluster.get(c).getToxSimilarity() == null)
			//						weightedTox.put(c, null);
			//					else
			//						weightedTox.put(c, normalizedToxWeight[i++] * mapCluster.get(c).getToxSimilarity());
			//			}
			//			return weightedTox.get(cluster);
			if (getToxSimilarity() == null)
				return null;
			else
				return getToxSimilarity() * getToxNonMissing();
		}

		public Double getToxNonMissing()
		{
			return getSimilarity(activityProps, nonNullSim);
		}

		private Double getSimilarity(List<CompoundProperty> props, SimilarityMeasure<Boolean> sim)
		{
			List<CompoundData> cd = new ArrayList<CompoundData>();
			for (Compound c : getLeafs())
				cd.add(c.getCompoundData());
			return DistanceUtil.similarity(cd, props, sim);
		}

		public double getUniformity()
		{
			if (uniformity == null)
			{
				double sumU = 0;
				double sumA = 0;
				int count = 0;
				int sumInclNull = 0;
				int countNull = 0;
				numAllNull = 0;

				for (CompoundProperty p : activityProps)
				{
					//if (p.toString().endsWith("_filled"))
					CountedSet<String> set = new CountedSet<String>();
					for (Compound comp : getLeafs())
						set.add(comp.getStringValue((NominalProperty) p));
					if (set.getNumValues() != 3 && set.getNumValues() != 2 && set.getNumValues() != 1)
						throw new Error(set.toString());
					countNull += set.getNullCount();
					sumInclNull += set.getSum(true);
					set.remove(null);
					if (set.getNumValues() > 0)
					{
						//						double variance;
						//						if (set.size() == 1)
						//							variance = 0;
						//						else
						//						{
						//							Variance v = new Variance();
						//							variance = v.evaluate(new double[] { set.getCount(set.values().get(0)),
						//									set.getCount(set.values().get(1)) });
						//						}
						double uniformity = set.getMaxCount(true) / (double) set.sum();
						sumU += uniformity;

						if (set.values().get(0).equals("1"))
							sumA += uniformity;
						else if (set.values().get(0).equals("0"))
							sumA += 1 - uniformity;
						else
							throw new Error();
						count++;
					}
					else
						numAllNull++;
				}
				nullRatio = countNull / (double) sumInclNull;
				uniformity = sumU / (double) count;
				activity = sumA / (double) count;
			}
			return uniformity;
		}

		public List<MyNode> getClusterNodes()
		{
			return getClusterNodes(null);
		}

		@SuppressWarnings("unchecked")
		public List<MyNode> getClusterNodes(final Integer size)
		{
			if (clusterNodes == null)
			{
				clusterNodes = new ArrayList<MyNode>();
				for (int i = 0; i < getChildCount(); i++)
					if (getChildAt(i).isLeaf())
					{
						//do nothing
					}
					else if (((MyNode) getChildAt(i)).getCluster() != null)
						clusterNodes.add(((MyNode) getChildAt(i)));
					else
						clusterNodes = ListUtil.concat(clusterNodes, ((MyNode) getChildAt(i)).getClusterNodes());
			}
			if (size == null)
				return clusterNodes;
			else
			{
				return ListUtil.filter(clusterNodes, new ListUtil.Filter<MyNode>()
				{
					@Override
					public boolean accept(MyNode n)
					{
						return n.getCluster().getNumCompounds() == size;
					}
				});
			}
		}

		public DoubleArraySummary getAvg(Crit crit)
		{
			return getAvg(crit, null);
		}

		public DoubleArraySummary getAvg(Crit crit, Integer clusterSize)
		{
			List<MyNode> nodes = getClusterNodes(clusterSize);
			if (nodes.size() == 0)
				return null;
			else
			{
				double d[] = new double[nodes.size()];
				for (int i = 0; i < d.length; i++)
				{
					MyNode n = nodes.get(i);
					switch (crit)
					{
						case size:
							d[i] = n.getCluster().getNumCompounds();
							break;
						case uniform:
							d[i] = n.getUniformity();
						case activity:
							d[i] = n.getActivity();
						case nullRatio:
							d[i] = n.getNullRatio();
						default:
							break;
					}
				}
				return DoubleArraySummary.create(d);
			}
		}

		public double getActivity()
		{
			if (activity == null)
				getUniformity();
			return activity;
		}

		public double getNullRatio()
		{
			if (nullRatio == null)
				getUniformity();
			return nullRatio;
		}

		public int getNumAllNull()
		{
			if (numAllNull == -1)
				getUniformity();
			return numAllNull;
		}

		public int getNumOffspring()
		{
			return getLeafs().size();
		}

		public CompoundProperty getCompoundProperty()
		{
			return compoundProperty;
		}

		public String getName()
		{
			return super.toString();
		}

		List<String> features;

		public List<String> getFeatures()
		{
			if (features != null)
				return features;

			List<String> f;
			if (isRoot())
				f = new ArrayList<String>();
			else if (isLeaf())
				f = ((MyNode) getParent()).getFeatures();

			else
			{
				f = ((MyNode) getParent()).getFeatures();
				if (feature == null)
					throw new IllegalStateException();
				f.add(feature);
			}
			if (getCluster() != null)
				features = f;
			return f;
		}

		@Override
		public String toString()
		{
			if (isLeaf())
				return super.toString();
			else
			{
				//				if (this == getRoot())
				//				{
				//					for (Integer cSize : new Integer[] { null, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12 })
				//					{
				//						System.out.print(cSize + ",");
				//						System.out.print(getAvg(Crit.size, cSize) + ",");
				//						System.out.print(getClusterNodes(cSize).size() + ",");
				//						System.out.print(getAvg(Crit.activity, cSize) + ",");
				//						System.out.print(getAvg(Crit.uniform, cSize) + ",");
				//						System.out.print(getAvg(Crit.nullRatio, cSize));
				//						System.out.println();
				//					}
				//					System.out.println();
				//
				//				}

				String simStruct = "";
				if (getCluster() != null)
				{
					Double d = getStructuralSimilarity();
					if (tanimotoWorking)
						simStruct = " simStruct:" + StringUtil.formatDouble(d);
					else
						simStruct = " distStruct:" + StringUtil.formatDouble(d);
				}

				return //"<html>"
						//+ 
				super.toString() + " "
						+ (cluster != null ? (cluster.getName() + " ") : "")
						//+ (cluster != null ? ("<b>" + cluster.getName() + "</b> ") : "")
						+ "(#"
						+ getNumOffspring()
						+ ") active:"
						+ StringUtil.formatDouble(getActivity())
						//+ " uniform:"+ StringUtil.formatDouble(getUniformity())
						+ " missing:"
						+ StringUtil.formatDouble(getNullRatio())
						//						+ " #all-missing:"
						//						+ getNumAllNull()
						+ simStruct
						+ (getCluster() != null ? (" simTox:" + StringUtil.formatDouble(getToxSimilarity())) : "")
						+ (getCluster() != null ? (" wTox:" + StringUtil.formatDouble(getWeightedTox())) : "");
				//						+ (getClusterNodes().size() > 0 ? (" num:" + getClusterNodes().size() + " avg-act:"
				//								+ getAvg(Crit.activity) + " avg-unif:" + getAvg(Crit.uniform) + " avg-null:"
				//								+ getAvg(Crit.nullRatio) + " avg-size:" + getAvg(Crit.size)) : "") + "</html>";
			}
		}
	}

	private List<CompoundProperty> activityProps;

	public TreeView(ViewControler viewControler, ClusterController clusterControler, Clustering clustering,
			GUIControler guiControler)
	{
		super(true);
		this.viewControler = viewControler;
		this.clusterControler = clusterControler;
		this.clustering = clustering;
		this.guiControler = guiControler;

		tanimotoWorking = DistanceUtil.isBoolean(clustering.getFeatures());

		HashSet<String> propNames = new HashSet<String>();
		if (propNames.size() == 0)
			for (CompoundProperty p : clustering.getPropertiesAndFeatures())
				propNames.add(p.toString());
		activityProps = new ArrayList<CompoundProperty>();
		for (CompoundProperty p : clustering.getPropertiesAndFeatures())
			if (propNames.contains(p.toString() + "_real"))
				activityProps.add(p);

		buildTree();
		addListener();

		setLayout(new BorderLayout());
		add(new JScrollPane(tree));

		setTitle("CheS-Mapper Tree-View");
		pack();
		setSize((int) Math.min(getSize().getWidth() + 20, Settings.TOP_LEVEL_FRAME.getWidth()),
				Settings.TOP_LEVEL_FRAME.getHeight() - 50);
		setLocationRelativeTo(Settings.TOP_LEVEL_FRAME);

		setVisible(true);
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				tree.requestFocus();
			}
		});
	}

	private void addListener()
	{
		clustering.addSelectionListener(new SelectionListener()
		{
			@Override
			public void compoundActiveChanged(Compound[] c)
			{
				if (!isVisible())
					return;
				updateSelection(ArrayUtil.first(c), null);
			}

			@Override
			public void clusterActiveChanged(Cluster c)
			{
				if (!isVisible())
					return;
				updateSelection(null, c);
			}
		});

		tree.addTreeSelectionListener(new TreeSelectionListener()
		{

			@Override
			public void valueChanged(TreeSelectionEvent e)
			{
				if (selfUpdate)
					return;
				selfUpdate = true;

				TreeView.this.block("handle selection change");
				Thread th = new Thread(new Runnable()
				{
					@Override
					public void run()
					{
						if (tree.getSelectionPath() == null)
						{
							clusterControler.clearClusterWatched();
							clusterControler.clearCompoundActive(true);
						}
						else
						{
							MyNode node = (MyNode) tree.getSelectionPath().getLastPathComponent();
							if (node.isRoot())
								clusterControler.setCompoundFilter(null, true);
							else if (node.isLeaf())
							{
								Compound c = (Compound) node.getUserObject();
								if (clusterControler.getCompoundFilter() != null
										&& !clusterControler.getCompoundFilter().accept(c))
									clusterControler.setCompoundFilter(null, true);
								clusterControler.setCompoundActive(c, true);
							}
							else if (node.getCluster() != null)
							{
								if (node.getCluster().getNumCompounds() == 0)
									clusterControler.setCompoundFilter(null, true);
								clusterControler.setClusterActive(node.getCluster(), true, true);
							}
							else
								clusterControler.setCompoundFilter(new CompoundFilterImpl(clustering, node.getLeafs(),
										node.getName()), true);
						}

						SwingUtil.invokeAndWait(new Runnable()
						{
							@Override
							public void run()
							{
								TreeView.this.unblock("handle selection change");
								selfUpdate = false;
							}
						});
					}
				});
				th.start();
			}
		});
	}

	private void updateSelection(Compound c, Cluster clust)
	{
		if (selfUpdate)
			return;
		selfUpdate = true;

		tree.setExpandsSelectedPaths(true);

		if (c != null)
		{
			TreePath p = new TreePath(map.get(c).getPath());
			tree.setSelectionPath(p);
			tree.scrollPathToVisible(p);
		}
		else if (clust != null)
		{
			TreePath p = new TreePath(mapCluster.get(clust).getPath());
			tree.setSelectionPath(p);
			tree.scrollPathToVisible(p);
		}
		else
		{
			tree.clearSelection();
		}

		selfUpdate = false;
	}

	public static Icon NO_STRUCTURE = new Icon()
	{
		@Override
		public int getIconHeight()
		{
			return ICON_H;
		}

		@Override
		public int getIconWidth()
		{
			return ICON_W;
		}

		@Override
		public void paintIcon(Component c, Graphics g, int x, int y)
		{
			g.drawString("?", ICON_W / 2 - 5, ICON_H / 2 + 5);
		}
	};

	private void buildTree()
	{
		MyNode root = new MyNode("Root", null);

		List<CompoundProperty> levels = new ArrayList<CompoundProperty>();
		for (CompoundProperty p : clustering.getPropertiesAndFeatures())
			if (p.getName().matches("(?i).*level.*"))
				levels.add(p);

		addNodes(root, levels, clustering.getCompounds(false));
		root.getNumOffspring();

		DefaultTreeModel m = new DefaultTreeModel(root);
		tree = new JTree(m);
		tree.setCellRenderer(new DefaultTreeCellRenderer()
		{
			@Override
			public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
					boolean isLeaf, int row, boolean focused)
			{
				JLabel c = (JLabel) super.getTreeCellRendererComponent(tree, value, selected, expanded, isLeaf, row,
						focused);

				if (isLeaf && ((MyNode) value).getUserObject() instanceof Compound)
				{
					Compound comp = ((Compound) ((MyNode) value).getUserObject());
					c.setText("<html>" + comp.getDisplayName().toString(true, comp.getHighlightColorText()) + "</html>");
					int size = guiControler.getComponentMaxWidth(ComponentSize.ICON_2D.getValue() * 0.01);
					Icon icon = comp.getIcon(false, size, size, false);
					if (icon == null)
						icon = NO_STRUCTURE;
					setIcon(icon);
				}
				((JComponent) c).setBorder(new EmptyBorder(3, 3, 3, 3));
				setFont(getFont().deriveFont((float) ScreenSetup.INSTANCE.getFontSize()));
				return c;
			}
		});

		for (MyNode n : map.values())
			tree.expandPath(new TreePath(((MyNode) ((MyNode) n.getParent()).getParent()).getPath()));

		//		FileUtil.CSVFile csv = new FileUtil.CSVFile();
		//		csv.content = new ArrayList<String[]>();
		//		String header[] = new String[] { "index", "num-compounds", "compounds", "num-features", "features", "active",
		//				"missing", (tanimotoWorking ? "struct-similarity" : "struct-distance"), "tox-similarity",
		//				"weighted-tox-similarity" };
		//		String props[] = new String[activityProps.size()];
		//		for (int i = 0; i < props.length; i++)
		//			props[i] = activityProps.get(i).toString() + "-missing";
		//		header = ArrayUtil.concat(header, props);
		//
		//		csv.content.add(header);
		//		int i = 0;
		//		for (Cluster c : clustering.getClusters())
		//		{
		//			String cmpIdx = "";
		//			for (Compound comp : c.getCompounds())
		//				cmpIdx += comp.getOrigIndex() + ";";
		//			cmpIdx = cmpIdx.substring(0, cmpIdx.length() - 1);
		//			MyNode n = mapCluster.get(c);
		//			int idx = 0;
		//			Object vals[] = new Object[header.length];
		//			for (Object object : new Object[] { (i + 1), c.size(), cmpIdx, n.getFeatures().size(),
		//					ArrayUtil.toString(ArrayUtil.toArray(n.getFeatures()), ";", "", "", ""), n.getActivity(),
		//					n.getNullRatio(), n.getStructuralSimilarity(), n.getToxSimilarity(), n.getWeightedTox() })
		//				vals[idx++] = object;
		//
		//			for (CompoundProperty p : activityProps)
		//			{
		//				vals[idx++] = c.numMissingValues(p) / (double) c.size();
		//			}
		//			csv.content.add(ArrayUtil.toStringArray(vals));
		//			i++;
		//		}
		//		FileUtil.writeCSV("/tmp/cluster.csv", csv, false);
		//
		//		String json = JsonTreeNode.create(root).toJson();
		//		FileUtil.writeStringToFile("/home/martin/documents/tree/tree.json", json);

		// //		for (int i = 0; i < tree.getRowCount(); i++)
		// //			if (!((MyNode) tree.getPathForRow(i).getLastPathComponent()).isLeaf())
		// //				tree.expandRow(i);
	}

	private void addNodes(MyNode node, List<CompoundProperty> p, List<Compound> c)
	{
		if (p.size() == 0)
		{
			for (Compound compound : c)
				node.add(new MyNode(compound, null));
		}
		else
		{
			List<CompoundProperty> pp = new ArrayList<CompoundProperty>(p);
			CompoundProperty prop = pp.remove(0);

			List<String> vals = new ArrayList<String>();
			for (Compound cc : c)
				vals.add(cc.getStringValue((NominalProperty) prop));
			CountedSet<String> set = CountedSet.create(vals);
			for (final String s : set.values())
			{
				String featureWithoutIndex = s.substring(s.indexOf(" ") + 1).trim();
				if (featureWithoutIndex.endsWith(" <= 0"))
					featureWithoutIndex = featureWithoutIndex.substring(0, featureWithoutIndex.length() - 5)
							+ " no-match";
				else if (featureWithoutIndex.endsWith(" > 0"))
					featureWithoutIndex = featureWithoutIndex.substring(0, featureWithoutIndex.length() - 4) + " match";

				MyNode child = new MyNode(prop + "=" + featureWithoutIndex, featureWithoutIndex);
				List<Compound> ccc = new ArrayList<Compound>();
				for (Compound cc : c)
					if (ObjectUtil.equals(cc.getStringValue((NominalProperty) prop), s))
						ccc.add(cc);
				if (set.getNumValues() == 1)
					addNodes(node, pp, ccc);
				else
				{
					node.add(child);
					addNodes(child, pp, ccc);
				}
			}
		}
	}
}
