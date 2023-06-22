package com.simonboy77;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("survival-chance")
public interface SurvivalChanceConfig extends Config
{
	@ConfigItem(
			keyName = "hitTurns",
			name = "Turn Amount",
			description = "How many turns ahead to calculate",
			position = 1
	)
	@Range(
			min = 1,
			max = 10
	)
	default int hitTurns() { return 1; }

	@ConfigItem(
			keyName = "showSurvivalChance",
			name = "Show Survival Chance",
			description = "Show the chance of survival after the selected amount of turns",
			position = 2
	)
	default boolean showSurvivalChance() { return true; }

	@ConfigItem(
			keyName = "showEscapeChance",
			name = "Show Escape Chance",
			description = "Show the chance of triggering an escape after the selected amount of turns",
			position = 3
	)
	default boolean showEscapeChance() { return true; }

	@ConfigItem(
			keyName = "showDeathChance",
			name = "Show Death Chance",
			description = "Show the chance of dying after the selected amount of turns",
			position = 4
	)
	default boolean showDeathChance() { return true; }

	@ConfigItem(
			keyName = "showPhoenixChance",
			name = "Show Phoenix Usage Chance",
			description = "Show the chance of using your phoenix after the selected amount of turns",
			position = 5
	)
	default boolean showPhoenixChance() { return true; }

	enum WarningShow
	{
		ALWAYS,
		IN_COMBAT,
		NEVER
	};

	@ConfigItem(
			keyName = "warnEscapeItem",
			name = "Escape Item Warning",
			description = "Display a warning when not wearing an escape item (ring of life, defence cape or escape crystal)",
			position = 6
	)
	default WarningShow warnEscapeItem() { return WarningShow.IN_COMBAT; }

	@ConfigItem(
			keyName = "warnPhoenix",
			name = "Phoenix Warning",
			description = "Display a warning when not wearing a phoenix necklace",
			position = 7
	)
	default WarningShow warnPhoenix() { return WarningShow.IN_COMBAT; }
}
