package com.simonboy77;

import java.awt.Color;
import java.awt.image.BufferedImage;

import net.runelite.api.Client;
import net.runelite.client.ui.overlay.infobox.InfoBox;
import net.runelite.client.ui.overlay.infobox.InfoBoxPriority;

public class ChanceInfoBox extends InfoBox {
    private final Client client;
    private final SurvivalChancePlugin plugin;
    private final SurvivalChanceConfig config;
    private final PlayerState playerState;
    private final int resultId;

    public ChanceInfoBox(Client client, SurvivalChancePlugin plugin, SurvivalChanceConfig config, PlayerState playerState, int resultId, BufferedImage icon)
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

    private String getStringFromChance()
    {
        double chance = this.playerState.getResultChance(this.resultId);

        if(chance == 100.0) {
            return String.format("%d", (int)chance);
        }
        else if(chance >= 10.0) {
            return String.format("%.1f", chance);
        }
        else if(chance == 0.0) {
            return String.format("%.1f", chance);
        }
        else { // chance < 10.0
            return String.format("%.2f", chance);
        }
    }

    @Override
    public String getText()
    {
        switch(this.resultId)
        {
            case HitResult.RESULT_SURVIVE:
            case HitResult.RESULT_DEATH: {
                return this.getStringFromChance();
            }
            case HitResult.RESULT_ESCAPE: {
                if(this.playerState.isWearingEscapeItem()) {
                    return this.getStringFromChance();
                } // else { return "": }
            } break;
            case HitResult.USED_PHOENIX: {
                if(this.playerState.isWearingPhoenix()) {
                    return this.getStringFromChance();
                } // else { return ""; }
            } break;
        }

        return "";
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
        switch(this.resultId) {
            case HitResult.RESULT_SURVIVE: {
                return (this.config.showSurvivalChance() && this.playerState.isInCombat());
            }
            case HitResult.RESULT_DEATH: {
                return (this.config.showDeathChance() && this.playerState.isInCombat());
            }
            case HitResult.RESULT_ESCAPE: {
                if (this.playerState.isWearingEscapeItem()) {
                    return (this.config.showEscapeChance() && this.playerState.isInCombat());
                } else {
                    SurvivalChanceConfig.WarningShow warning = this.config.warnEscapeItem();
                    if (warning == SurvivalChanceConfig.WarningShow.ALWAYS) {
                        return true;
                    } else if (warning == SurvivalChanceConfig.WarningShow.IN_COMBAT) {
                        return this.playerState.isInCombat();
                    } // else { return false; }
                }
            } break;
            case HitResult.USED_PHOENIX: {
                if (this.playerState.isWearingPhoenix()) {
                    return (this.config.showPhoenixChance() && this.playerState.isInCombat());
                } else {
                    SurvivalChanceConfig.WarningShow warning = this.config.warnPhoenix();
                    if (warning == SurvivalChanceConfig.WarningShow.ALWAYS) {
                        return true;
                    } else if (warning == SurvivalChanceConfig.WarningShow.IN_COMBAT) {
                        return this.playerState.isInCombat();
                    } // else { return false }
                }
            } break;
        }

        return false;
    }

    @Override
    public String getName()
    {
        switch(this.resultId)
        {
            case HitResult.RESULT_SURVIVE: return "Chance of Survival";
            case HitResult.RESULT_DEATH: return "Chance of Death";
            case HitResult.RESULT_ESCAPE: {
                if(this.playerState.isWearingEscapeItem()) {
                    return "Chance of Escape";
                }
                else {
                    return "You are not wearing an escape item!";
                }
            }
            case HitResult.USED_PHOENIX: {
                if(this.playerState.isWearingPhoenix()) {
                    return "Chance of using Phoenix";
                }
                else {
                    return "You are not wearing a phoenix necklace!";
                }
            }
        }

        return "";
    }

    public int getResultId()
    {
        return this.resultId;
    }
}
