package org.chesmapper.test;

import org.junit.internal.TextListener;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.mg.javalib.util.ArrayUtil;
import org.mg.javalib.util.TimeFormatUtil;

public class TestLauncher
{
	public static enum MappingTest
	{
		gui_wizard, gui_viewer, gui_both, mapping_single, mapping_wizard, mapping_cache, mapping_all, mapping_debug,
		cdk_test
	}

	public static MappingTest MAPPING_TEST;// = MappingTest.all_gui;

	public static void main(String[] args)
	{
		MappingTest test = null;
		try
		{
			if (args[0].equals("debug"))
				test = MappingTest.mapping_cache;
			else
				test = MappingTest.valueOf(args[0]);
		}
		catch (Exception e)
		{
			System.err.println("possible params: " + ArrayUtil.toString(MappingTest.values(), "|", "", "", ""));
			System.exit(1);
		}
		JUnitCore junit = new JUnitCore();
		junit.addListener(new TextListener(System.out));
		Result result = null;
		if (test == MappingTest.gui_both)
			result = junit.run(WizardTest.class, ViewerTest.class);
		else if (test == MappingTest.gui_wizard)
			result = junit.run(WizardTest.class);
		else if (test == MappingTest.gui_viewer)
			result = junit.run(ViewerTest.class);
		else if (test.toString().startsWith("mapping_"))
		{
			MAPPING_TEST = test;
			result = junit.run(MappingAndExportTest.class);
		}
		else if (test == MappingTest.cdk_test)
			result = junit.run(CDKTest.class);
		System.out.println("");
		System.out.println("Number of test failures: " + result.getFailureCount() + ", Runtime: "
				+ TimeFormatUtil.format(result.getRunTime()));
		for (Failure failure : result.getFailures())
		{
			System.out.println(failure.toString());
		}
		System.exit(0);
	}
}
