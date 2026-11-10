package com.telosutils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Something drawn on the HUD that can be moved and scaled in the HUD editor.
 * An x position of -1 means "centred horizontally".
 */
abstract class HudElement {
    final String name;
    final String description;

    /** Size the last time it was drawn, in screen (GUI) pixels including scale. */
    int lastWidth;
    int lastHeight;

    HudElement(String name, String description) {
        this.name = name;
        this.description = description;
    }

    abstract boolean enabled();

    abstract int rawX();

    abstract int rawY();

    abstract float scale();

    abstract void setPosition(int x, int y);

    abstract void setScale(float scale);

    abstract void reset();

    /**
     * Draws at (0, 0) in unscaled coordinates and returns the unscaled {width, height},
     * or null when there's nothing to show. With {@code example}, draws sample content for the editor.
     */
    protected abstract int[] drawContent(GuiGraphicsExtractor g, boolean example);

    int x() {
        if (rawX() >= 0) return rawX();
        int screenW = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        return Math.max(0, (screenW - lastWidth) / 2);
    }

    int y() {
        return rawY();
    }

    final void draw(GuiGraphicsExtractor g, boolean example) {
        float scale = scale();
        g.pose().pushMatrix();
        g.pose().translate(x(), y());
        g.pose().scale(scale, scale);
        int[] size = drawContent(g, example);
        g.pose().popMatrix();
        lastWidth = size == null ? 0 : Math.round(size[0] * scale);
        lastHeight = size == null ? 0 : Math.round(size[1] * scale);
    }

    boolean isOver(double mx, double my) {
        return lastWidth > 0 && Ui.inside(mx, my, x(), y(), lastWidth, lastHeight);
    }

    /** A dark rounded card with a coloured bar down the left side, the shared look of every element. */
    static void card(GuiGraphicsExtractor g, int w, int h, int accent) {
        int bg = Ui.withAlpha(Ui.PANEL, TelosUtilsClient.CONFIG.bgOpacity * 255 / 100);
        Ui.roundRect(g, 0, 0, w, h, 3, bg);
        g.fill(0, 2, 2, h - 2, accent);
    }
}
