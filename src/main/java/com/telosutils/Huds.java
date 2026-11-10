package com.telosutils;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;

/** Every HUD element, and the hook that draws them each frame. */
final class Huds {
    static final List<HudElement> ALL = List.of(DpsHud.INSTANCE, BossHud.INSTANCE, PhaseWarning.INSTANCE);

    private Huds() {}

    static void render(GuiGraphicsExtractor g, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        if (mc.screen instanceof HudEditorScreen) return; // the editor draws its own previews
        for (HudElement e : ALL) {
            if (e.enabled()) e.draw(g, false);
        }
    }
}
