package com.simonboy77;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class SurvivalChancePluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(SurvivalChancePlugin.class);
		RuneLite.main(args);
	}
}