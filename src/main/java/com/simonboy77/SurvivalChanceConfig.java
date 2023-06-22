package com.simonboy77;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("survival-chance")
public interface SurvivalChanceConfig extends Config
{
	enum DropdownExample
	{
		THIS,
		THAT,
		SUCH,
		SO
	};

	enum ShowInfoBox
	{
		ALWAYS,
		IN_COMBAT,
		NEVER
	};

	@ConfigItem(
			keyName = "showInfoBox",
			name = "Show Info Box: ",
			description = "When to show the info box"
	)
	default ShowInfoBox showInfoBox() { return ShowInfoBox.ALWAYS; }

	@ConfigItem(
			keyName = "testCheckBox",
			name = "Test Check Box",
			description = "Testing testing testing"
	)
	default boolean testCheckBox() { return false; }

	@ConfigItem(
			keyName = "usePlayerEquipment",
			name = "Include equipment in calculation",
			description = "Whether to take player equipment into account when calculating"
	)
	default boolean testPlayerEquipment() { return false; }

	@ConfigItem(
		keyName = "greeting",
		name = "Welcome Greeting",
		description = "The message to show to the user when they login"
	)
	default String greeting() { return "Goodbye"; }

	@ConfigItem(
			keyName = "hitTurns",
			name = "Turn Amount",
			description = "How many turns ahead to calculate"
	)
	@Range(
			min = 1,
			max = 10
	)
	default int hitTurns() { return 1; }
}
