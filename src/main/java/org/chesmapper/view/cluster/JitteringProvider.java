package org.chesmapper.view.cluster;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.vecmath.Vector3f;

import org.chesmapper.map.dataInterface.CompoundData;
import org.chesmapper.map.main.Settings;
import org.chesmapper.view.gui.LaunchCheSMapper;
import org.mg.javalib.gui.MessagePanel;
import org.mg.javalib.jitter.Jittering;
import org.mg.javalib.jitter.NNComputer;
import org.mg.javalib.util.ArrayUtil;
import org.mg.javalib.util.DoubleKeyHashMap;
import org.mg.javalib.util.MathUtil;
import org.mg.javalib.util.SetUtil;
import org.mg.javalib.util.SwingUtil;

public class JitteringProvider
{
	/**
	 * stores the compound-set that has been used for this jittering level
	 */
	private List<Set<Compound>> sets = new ArrayList<Set<Compound>>();
	/**
	 * stores the level that has last been used for a compound-set
	 */
	private HashMap<Set<Compound>, Integer> levels = new HashMap<Set<Compound>, Integer>();
	private DoubleKeyHashMap<CompoundData, Integer, Vector3f> positions = new DoubleKeyHashMap<CompoundData, Integer, Vector3f>();
	private int currentLevel;
	private float[] minDistances;
	private static JitteringProvider staticInstance;
	public static final int STEPS = 10;

	public JitteringProvider(Clustering c)
	{
		staticInstance = null;

		/*
		 * jittering in ches-mapper has fixed number of steps
		 * each step has a min-distance that compounds should have
		 * the min-distance is computed based on the entire data, within interval:
		 * [ min-min-dist , min-min-dist + delta(max-min-distance,min-min-dist)/2 ]
		 * the interval chunks are not equi-distant but using a log-scale  
		 */
		minDistances = new float[STEPS + 1];
		Vector3f v[] = new Vector3f[c.getCompounds().size()];
		for (int i = 0; i < v.length; i++)
			v[i] = c.getCompounds().get(i).getPosition();
		NNComputer nn = new NNComputer(v);
		nn.computeNaive();
		float dist = nn.getMinMinDist();
		float add = (nn.getMaxMinDist() - nn.getMinMinDist()) * 0.5f;
		double log[] = ArrayUtil.toPrimitiveDoubleArray(MathUtil.logBinning(STEPS, 1.2));
		for (int i = 1; i <= STEPS; i++)
			minDistances[i] = dist + add * (float) log[i];
		Settings.LOGGER
				.info("Initiated min-distances per level for jittering: " + ArrayUtil.toNiceString(minDistances));
	}

	public Vector3f getPosition(CompoundData c, int level)
	{
		if (level == 0)
			return c.getPosition();
		if (positions.containsKeyPair(c, level))
			return positions.get(c, level);
		else
			return c.getPosition();
	}

	public static Vector3f getPosition(CompoundData c)
	{
		if (staticInstance == null)
			return c.getPosition();
		else
			return staticInstance.getPosition(c, staticInstance.currentLevel);
	}

	private Jittering createFromCompounds(List<Compound> compounds, int level, float minDist)
	{
		List<CompoundData> d = new ArrayList<CompoundData>();
		for (Compound c : compounds)
			d.add(c.getCompoundData());
		return create(d, level, minDist);
	}

	private Jittering create(List<CompoundData> d, int level, float minDist)
	{
		if (level <= 0)
			throw new IllegalArgumentException();
		Vector3f v[] = new Vector3f[d.size()];
		if (level == 1)
			for (int i = 0; i < v.length; i++)
				v[i] = new Vector3f(d.get(i).getPosition());
		else
			for (int i = 0; i < v.length; i++)
				v[i] = new Vector3f(JitteringProvider.this.getPosition(d.get(i), level - 1));
		return new Jittering(v, minDist, new Random());
	}

	public void updateJittering(int level, Set<Compound> compounds)
	{
		Settings.LOGGER.info("Set jittering level to " + level);
		if (level == 0)
		{
			//do nothing, use orig positions
		}
		else if (sets.size() > level && SetUtil.isSubSet(sets.get(level), compounds))
		{
			//do nothing, positions have already been computed
		}
		else
		{
			jitter(compounds, level);
			while (sets.size() < level + 1)
				sets.add(null);
			sets.set(level, compounds);
		}
		while (sets.size() > level + 1)
			sets.remove(level + 1);
		levels.put(compounds, level);

		staticInstance = this;
		currentLevel = level;
	}

	private void jitter(Set<Compound> c, int level)
	{
		jitter(new ArrayList<Compound>(c), level);
	}

	private void jitter(List<Compound> c, final int level)
	{
		Settings.LOGGER.info("Compute jittering for " + c.size() + " compounds for level " + level);
		Jittering j = createFromCompounds(c, level, minDistances[level]);
		j.jitter();
		for (int i = 0; i < c.size(); i++)
			positions.put(c.get(i).getCompoundData(), level, j.getPosition(i));
	}

	public int getJitteringResetLevel(Set<Compound> compounds)
	{
		int currentLevel = Math.max(0, (sets.size() - 1));
		//		System.err.println("current jittering level is " + currentLevel);
		int lastLevelForSet = levels.containsKey(compounds) ? levels.get(compounds) : 0;
		//		System.err.println("last jittering level for this set is " + lastLevelForSet);
		if (lastLevelForSet == currentLevel)
		{
			if (lastLevelForSet == 0)
				return -1;
			//			System.err.println("current jittering level is equal, check if sets are complient");
			if (SetUtil.isSubSet(sets.get(currentLevel), compounds))
			{
				//				System.err.println("yes, nothing todo");
				return -1;
			}
			else
			{
				//				System.err.println("no, check lower level");
				currentLevel = -1;
			}
		}
		//		else
		//			System.err.println("current jittering level differs");
		for (int j = currentLevel; j >= 1; j--)
		{
			//			System.err.println("check if this set is subset of level " + j);
			if (SetUtil.isSubSet(sets.get(j), compounds))
			{
				//				System.err.println("yes, use level " + j);
				return j;
			}
		}
		//		System.err.println("not compatible, reset to 0");
		return 0;
	}

	private static boolean showWarning = true;

	public static void showJitterWarning()
	{
		if (showWarning)
		{
			MessagePanel p = new MessagePanel();
			p.addWarning(Settings.text("spread.warning"), Settings.text("spread.warning.details"));
			SwingUtil.showInDialog(p, "Warning", new Dimension(600, 300), null, Settings.TOP_LEVEL_FRAME);
			showWarning = false;
		}
	}

	public static void main(String[] args)
	{
		LaunchCheSMapper.init();
		showJitterWarning();
	}
}
