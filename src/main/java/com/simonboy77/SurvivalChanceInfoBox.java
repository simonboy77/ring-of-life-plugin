package com.simonboy77;

import java.awt.Color;
import java.awt.image.BufferedImage;

import net.runelite.api.Client;
import net.runelite.client.ui.overlay.infobox.InfoBox;
import net.runelite.client.ui.overlay.infobox.InfoBoxPriority;

public class SurvivalChanceInfoBox extends InfoBox {
    private final Client client;
    private final SurvivalChancePlugin plugin;
    private final SurvivalChanceConfig config;
    private final PlayerState playerState;
    private final int resultId;

    public SurvivalChanceInfoBox(Client client, SurvivalChancePlugin plugin, SurvivalChanceConfig config, PlayerState playerState, int resultId, BufferedImage icon)
    {
        super(icon, plugin);

        this.client = client;
        this.plugin = plugin;
        this.config = config;
        this.playerState = playerState;
        this.resultId = resultId;

        setPriority(InfoBoxPriority.HIGH);
        setTooltip(this.getName());
    }

    @Override
    public String getText()
    {
        switch(this.resultId)
        {
            case HitResult.RESULT_SURVIVE:
            case HitResult.RESULT_ESCAPE:
            case HitResult.RESULT_DEATH:
            case HitResult.USED_PHOENIX: {
                double chance = this.playerState.getResultChance(this.resultId);

                if(chance == 100.0) {
                    return String.format("%d", (int)chance);
                }
                else if(chance >= 10.0) {
                    return String.format("%.1f", chance);
                }
                else { // chance < 10.0
                    return String.format("%.2f", chance);
                }
            }

            default: return "";
        }
    }

    @Override
    public Color getTextColor()
    {
        switch(this.resultId)
        {
            case HitResult.RESULT_SURVIVE: return Color.GREEN;
            case HitResult.RESULT_ESCAPE: return Color.YELLOW;
            case HitResult.RESULT_DEATH: return Color.RED;
            case HitResult.USED_PHOENIX: return Color.ORANGE;
            default: return Color.WHITE;
        }
    }

    @Override
    public boolean render()
    {
        if(this.playerState.isInCombat())
        {
            switch(this.resultId)
            {
                case HitResult.RESULT_SURVIVE: return this.config.showSurvivalChance();
                case HitResult.RESULT_ESCAPE: return this.config.showEscapeChance();
                case HitResult.RESULT_DEATH: return this.config.showDeathChance();
                case HitResult.USED_PHOENIX: return this.config.showPhoenixChance();
            }
        }

        return false;
    }

    @Override
    public String getName()
    {
        switch(this.resultId)
        {
            case HitResult.RESULT_SURVIVE: return "Chance of Survival";
            case HitResult.RESULT_ESCAPE: return "Chance of Escape";
            case HitResult.RESULT_DEATH: return "Chance of Death";
            case HitResult.USED_PHOENIX: return "Chance of using Phoenix";
            default: return "";
        }
    }
}
