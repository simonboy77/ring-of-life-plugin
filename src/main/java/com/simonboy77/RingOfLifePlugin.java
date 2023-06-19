// https://github.com/molo-pl/runelite-plugins/tree/life-saving-jewellery
// https://github.com/donth77/loot-lookup-plugin/blob/master/src/main/java/com/lootlookup/osrswiki/WikiScraper.java

package com.simonboy77;

import com.google.inject.Provides;

import javax.annotation.Nullable;
import javax.inject.Inject;

// TODO remove those import blah.blah.* statements
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import net.runelite.client.ui.overlay.OverlayManager;

import static com.simonboy77.WikiScraper.getMonsterMaxHit; // huh?
import static net.runelite.api.ItemID.RING_OF_LIFE;
import static net.runelite.api.ItemID.DEFENCE_CAPE;
import static net.runelite.api.ItemID.PHOENIX_NECKLACE;

@Slf4j
@PluginDescriptor(
	name = "Ring of Life"
)
public class RingOfLifePlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private RingOfLifeOverlay overlay;

	@Inject
	private RingOfLifeConfig config;

	private PlayerState playerState;

	@Override
	protected void startUp() throws Exception
	{
		overlayManager.add(overlay);
		playerState = new PlayerState(client);
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(overlay);
	}

	private void log(String text)
	{
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", text, null);
	}

	@Subscribe
	public void onInteractingChanged(InteractingChanged event)
	{
		// (healthRatio == 0) == opponent died , (healthRatio == -1) == npc has no health
		if (event.getSource() == client.getLocalPlayer())  {
			if(event.getTarget().getHealthRatio() > 0) {
				playerState.setOpponent(event.getTarget());
				playerState.calcSurvivalChance(4, 3);
			}
		}
		else if(event.getTarget() == client.getLocalPlayer())
		{
			if(event.getSource().getHealthRatio() > 0) {
				playerState.setOpponent(event.getSource());
				playerState.calcSurvivalChance(4, 3);
			}
		}
	}

	@Subscribe
	public void onGameTick(GameTick gameTick)
	{
		// client.getLocalPlayer().getInteracting() == null
		if (playerState.curOpponent != null &&
				playerState.curOpponent.getInteracting() != client.getLocalPlayer()) {
			playerState.setOpponent(null);
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged statChanged)
	{
		Skill skill = statChanged.getSkill();
		if(skill == Skill.HITPOINTS || skill == Skill.DEFENCE || skill == Skill.MAGIC) {
			playerState.calcSurvivalChance(4, 3);
		}
	}

	/*@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded menuEntryAdded)
	{
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", counter + " onMenuEntryAdded", null);
		counter++;
	}*/

	// This gets called after login
	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		// Check if changed container is equipment
		if(event.getItemContainer().getId() == InventoryID.EQUIPMENT.getId())
		{
			playerState.wearingRingOfLife = event.getItemContainer().contains(RING_OF_LIFE);
			playerState.wearingDefenceCape = event.getItemContainer().contains(DEFENCE_CAPE);
			playerState.wearingPhoenixNecklace = event.getItemContainer().contains(PHOENIX_NECKLACE);
		}
	}

	@Provides
	RingOfLifeConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(RingOfLifeConfig.class);
	}
}
