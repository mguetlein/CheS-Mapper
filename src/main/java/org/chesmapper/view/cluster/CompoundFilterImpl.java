package org.chesmapper.view.cluster;

import java.util.List;

import org.mg.javalib.util.ListUtil;

public class CompoundFilterImpl implements CompoundFilter
{
	private String desc;
	private List<Compound> compounds;

	public CompoundFilterImpl(Clustering clustering, List<Compound> compounds, String additionalDesc)
	{
		this.compounds = compounds;
		desc = "Show " + compounds.size() + "/" + clustering.getNumUnfilteredCompounds(false) + " compounds";
		if (additionalDesc != null)
			desc += " (" + additionalDesc + ")";
	}

	public static CompoundFilterImpl combine(Clustering clustering, CompoundFilter filter1, CompoundFilter filter2)
	{
		return new CompoundFilterImpl(clustering, ListUtil.cut2(((CompoundFilterImpl) filter1).compounds,
				((CompoundFilterImpl) filter2).compounds), null);
	}

	public String toString()
	{
		return desc;
	}

	public boolean accept(Compound c)
	{
		return compounds.contains(c);
	}

}
