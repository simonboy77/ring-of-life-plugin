package com.simonboy77;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class RingOfLifePluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(RingOfLifePlugin.class);
		RuneLite.main(args);
	}
}