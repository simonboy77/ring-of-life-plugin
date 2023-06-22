package com.simonboy77;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;

import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.TitleComponent;

class SurvivalChanceOverlay extends OverlayPanel {
    private final Client client;
    private final SurvivalChancePlugin plugin;
    private final SurvivalChanceConfig config;

    @Inject
    private SurvivalChanceOverlay(final Client client, final SurvivalChancePlugin plugin, final SurvivalChanceConfig config)
    {
        super(plugin);
        setPosition(OverlayPosition.TOP_LEFT);

        this.client = client;
        this.plugin = plugin;
        this.config = config;

        addMenuEntry(MenuAction.RUNELITE_OVERLAY, "Reset", "Ring of Life Overlay");
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if(config.showInfoBox() == SurvivalChanceConfig.ShowInfoBox.ALWAYS)
        {
            panelComponent.getChildren().add(TitleComponent.builder().text("Ring of Life").color(Color.WHITE).build());
        }

        return super.render(graphics);
    }
}
