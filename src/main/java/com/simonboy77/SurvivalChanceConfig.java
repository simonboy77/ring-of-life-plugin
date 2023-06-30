package com.simonboy77;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("survival-chance")
public interface SurvivalChanceConfig extends Config
{
	@ConfigItem(
			keyName = "secondsOfCombat",
			name = "Seconds of Combat",
			description = "How many seconds of combat to calculate",
			position = 1
	)
	@Range(
			min = 1,
			max = 30
	)
	default int secondsOfCombat() { return 4; }

	@ConfigItem(
			keyName = "showSurvivalChance",
			name = "Show Survival Chance",
			description = "Show the chance of survival after the selected amount of seconds",
			position = 2
	)
	default boolean showSurvivalChance() { return true; }

	@ConfigItem(
			keyName = "showEscapeChance",
			name = "Show Escape Chance",
			description = "Show the chance of triggering an escape after the selected amount of seconds",
			position = 3
	)
	default boolean showEscapeChance() { return true; }

	@ConfigItem(
			keyName = "showDeathChance",
			name = "Show Death Chance",
			description = "Show the chance of dying after the selected amount of seconds",
			position = 4
	)
	default boolean showDeathChance() { return true; }

	@ConfigItem(
			keyName = "showPhoenixChance",
			name = "Show Phoenix Usage Chance",
			description = "Show the chance of triggering your phoenix after the selected amount of seconds",
			position = 5
	)
	default boolean showPhoenixChance() { return true; }

	@ConfigItem(
			keyName = "showRedemptionChance",
			name = "Show Redemption Usage Chance",
			description = "Show the chance of triggering redemption after the selected amount of seconds",
			position = 6
	)
	default boolean showRedemptionChance() { return true; }

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
			position = 7
	)
	default WarningShow warnEscapeItem() { return WarningShow.IN_COMBAT; }

	@ConfigItem(
			keyName = "warnPhoenix",
			name = "Phoenix Warning",
			description = "Display a warning when not wearing a phoenix necklace",
			position = 8
	)
	default WarningShow warnPhoenix() { return WarningShow.IN_COMBAT; }

	@ConfigItem(
			keyName = "warnRedemption",
			name = "Redemption Warning",
			description = "Display a warning when redemption is not active",
			position = 9
	)
	default WarningShow warnRedemption() { return WarningShow.IN_COMBAT; }

	@ConfigItem(
			keyName = "altEscapeIcon",
			name = "Alternative Escape Icon",
			description = "Use the alternative version of the escape icon",
			position = 10
	)
	default boolean altEscapeIcon() { return false; }
}
