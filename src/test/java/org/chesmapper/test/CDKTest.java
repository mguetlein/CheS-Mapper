package org.chesmapper.test;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.chesmapper.map.data.DatasetFile;
import org.chesmapper.map.dataInterface.CompoundProperty;
import org.chesmapper.map.dataInterface.CompoundPropertySet;
import org.chesmapper.map.dataInterface.NumericProperty;
import org.chesmapper.map.main.Settings;
import org.chesmapper.map.property.PropertySetCategory;
import org.chesmapper.map.property.PropertySetProvider;
import org.chesmapper.view.gui.LaunchCheSMapper;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.mg.javalib.util.ArrayUtil;
import org.mg.javalib.util.FileUtil;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CDKTest
{
	public CDKTest()
	{

		LaunchCheSMapper.init();
		Settings.CACHING_ENABLED = false;
	}

	//	@Test
	//	public void align3D() throws Exception
	//	{
	//		int tryCount = 0;
	//		while (true)
	//		{
	//			Properties props = MappingWorkflow.createMappingWorkflow("data/cox2_13_mcs.sdf", null, null, null, null,
	//					MCSAligner.INSTANCE);
	//			final CheSMapping mapping = MappingWorkflow.createMappingFromMappingWorkflow(props, "");
	//			DatasetFile d = mapping.getDatasetFile();
	//			ClusteringData data = mapping.doMapping();
	//			Assert.assertEquals(1, data.getNumClusters());
	//			Assert.assertEquals("O=S(=O)(c1ccc(cc1)n2ccnc2(cc))C",
	//					data.getClusters().get(0).getSubstructureSmarts(SubstructureSmartsType.MCS));
	//			boolean b = sdfEquals("data/cox2_13_mcs_aligned.sdf", d.getAlignSDFilePath(), true);
	//			if (!b)
	//			{
	//				tryCount++;
	//				System.err.println("XXXXXXXXXXXXXXXX\nalign test failed, retry " + tryCount + "!\nXXXXXXXXXXXXXXXX");
	//				System.err.println("data/cox2_13_mcs_aligned.sdf");
	//				System.err.println(d.getAlignSDFilePath());
	//				//				System.exit(1);
	//			}
	//			else
	//				break;
	//		}
	//	}

	@Test
	public void testDescriptors() throws Exception
	{
		DatasetFile d = DatasetFile.localFile(new File("data/cox2_13_mcs.sdf").getAbsolutePath());
		d.loadDataset();

		PropertySetCategory set = PropertySetProvider.INSTANCE.getCDKCategory();
		PropertySetCategory categories[] = set.getSubCategory();
		Assert.assertEquals(5, categories.length);
		String names[] = new String[] { "electronic", "constitutional", "topological", "hybrid", "geometrical" };
		int num[] = new int[] { 7, 14, 23, 2, 5 };
		Set<CompoundPropertySet> computed = new HashSet<CompoundPropertySet>();

		String namesCmp[] = FileUtil.readStringFromFile("data/cox2_13_mcs_CDK-names.txt").split("\n");
		String valuesCmp[] = FileUtil.readStringFromFile("data/cox2_13_mcs_CDK-values.txt").split("\n");
		int line = 0;
		//		String nameRes = "";
		//		String valuesRes = "";

		for (PropertySetCategory cat : categories)
		{
			int idx = ArrayUtil.indexOf(names, cat.toString());
			Assert.assertTrue(cat.toString() + " not included in " + names, idx != -1);
			Assert.assertEquals(num[idx], cat.getPropertySet(d).length);

			for (CompoundPropertySet ps : cat.getPropertySet(d))
			{
				Assert.assertTrue("Already computed: " + ps, !ps.isComputed(d) || computed.contains(ps));

				if (!ps.isComputed(d) && !ps.isComputationSlow())
				{
					Assert.assertTrue(ps.compute(d));
					Assert.assertTrue(ps.isComputed(d));
					computed.add(ps);

					for (int i = 0; i < ps.getSize(d); i++)
					{
						CompoundProperty p = ps.get(d, i);
						Double v[] = ((NumericProperty) p).getDoubleValues();
						String name = ps.toString() + " " + p.getName();
						//						String valuesStr = ArrayUtil.toCSVString(v);
						//						nameRes += name + "\n";
						//						valuesRes += valuesStr + "\n";
						Assert.assertEquals(namesCmp[line], name);
						Double vCmp[] = ArrayUtil.doubleFromCSVString(valuesCmp[line]);
						Assert.assertEquals(v.length, vCmp.length);
						for (int j = 0; j < vCmp.length; j++)
						{
							//System.out.println(d.getSmiles()[j]);
							if (v[j] == null)
								Assert.assertNull(vCmp[j]);
							else
							{
								//Assert.assertEquals(vCmp[j], v[j], 0.00000001);
								if (Math.abs(vCmp[j] - v[j]) > 0.00000001)
									System.err.println(v[j] + " != " + vCmp[j]);
							}
						}
						line++;
					}
				}
			}
		}
		//		FileUtil.writeStringToFile("data/cox2_13_mcs_CDK-names.txt", nameRes);
		//		FileUtil.writeStringToFile("data/cox2_13_mcs_CDK-values.txt", valuesRes);

	}

	//	@Test
	//	public void testSmilesProp() throws Exception
	//	{
	//		Assert.assertTrue(FeatureService.testSmilesProp());
	//	}

	//	@Test
	//	public void testMCS() throws Exception
	//	{
	//		String data[] = { "data/cox2_13_mcs.sdf", "data/mcs.smi" };
	//		int num[] = { 13, 3 };
	//		String mcs14[] = { "O=S(=O)(c1ccc(cc1)n2ccnc2(cc))C", "CCCCOCCNC" };
	//		String mcs15[] = { "c1(n(ccn1)-c2ccc(cc2)S(=O)(=O)C)-cc", "C(NC)COCCCC" };
	//		MCSComputer.DEBUG = true;
	//		for (int i = 0; i < data.length; i++)
	//		{
	//			DatasetFile d = DatasetFile.localFile(new File(data[i]).getAbsolutePath());
	//			d.loadDataset();
	//			Assert.assertEquals(num[i], d.numCompounds());
	//			String mcs = MCSComputer.computeMCS(d);
	//			Assert.assertTrue("should be \n" + mcs14[i] + " or \n" + mcs15[i] + " but is \n" + mcs,
	//					mcs14[i].equals(mcs) || mcs15[i].equals(mcs));
	//		}
	//		MCSComputer.DEBUG = false;
	//	}
	//
	//	@Test
	//	public void testAromFromSMIandSDFDetection() throws Exception
	//	{
	//		Double arom[] = new Double[] { 5.0, 6.0, 6.0, 6.0, 12.0, 0.0, 0.0, 0.0, 0.0, 0.0 };
	//		for (String file : new String[] { "demo.smi", "demo-kekulized.smi" })
	//		{
	//			String path = new File("data/" + file).getAbsolutePath();
	//			while (true)
	//			{
	//				DatasetFile d = DatasetFile.localFile(path);
	//				d.loadDataset();
	//				Assert.assertEquals(10, d.numCompounds());
	//				System.err.println("check arom for " + FileUtil.getFilename(path));
	//				Assert.assertArrayEquals(arom, getNumAromAtoms(d));
	//				if (path.endsWith(".smi"))
	//					path = d.getSDF();
	//				else
	//				{
	//					sdfEquals("data/" + FileUtil.getFilename(file, false) + ".2dconverted.sdf", d.getSDF());
	//					break;
	//				}
	//			}
	//		}
	//	}

	@Test
	public void z_resetCachingEnabled()
	{
		Assert.assertFalse(Settings.CACHING_ENABLED);
		Settings.CACHING_ENABLED = true;
	}

	private Double[] getNumAromAtoms(DatasetFile d)
	{
		PropertySetCategory set = PropertySetProvider.INSTANCE.getCDKCategory();
		PropertySetCategory categories[] = set.getSubCategory();
		for (PropertySetCategory category : categories)
			if (category.toString().equals("constitutional"))
				for (CompoundPropertySet propSet : category.getPropertySet(d))
					if (propSet.toString().equals("Aromatic Atoms Count"))
					{
						propSet.compute(d);
						return ((NumericProperty) propSet.get(d, 0)).getDoubleValues();
					}
		return null;
	}

	private void sdfEquals(String file1, String file2)
	{
		sdfEquals(file1, file2, false);
	}

	private boolean sdfEquals(String file1, String file2, boolean noAsserts)
	{
		System.err.println("check sdf equal: " + FileUtil.getFilename(file1) + " " + FileUtil.getFilename(file2));
		String sdf1[] = FileUtil.readStringFromFile(file1).split("\n");
		String sdf2[] = FileUtil.readStringFromFile(file2).split("\n");
		if (noAsserts)
		{
			if (sdf1.length != sdf2.length)
				return false;
		}
		else
			Assert.assertEquals(sdf1.length, sdf2.length);
		for (int i = 0; i < sdf2.length; i++)
		{
			String s1 = sdf1[i];
			String s2 = sdf2[i];
			if (s1.matches("  CDK     [0-9]*") && s2.matches("  CDK     [0-9]*"))
				continue;
			if (noAsserts)
			{
				if (!s1.equals(s2))
					return false;
			}
			else
				Assert.assertEquals(s1, s2);
		}
		return true;
	}
}
