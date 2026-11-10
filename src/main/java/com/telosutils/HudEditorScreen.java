package com.telosutils;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Move and resize the DPS overlay, like Melinoe's HUD editor: drag to move, scroll (or +/-) to
 * resize, hold Shift to snap to the centre lines, arrow keys to nudge.
 */
public class HudEditorScreen extends Screen {
    private static final int SNAP = 6;
    private static final int GUIDE = 0x8800FFFF;

    private final Screen parent;
    private boolean dragging;
    private double dragDX;
    private double dragDY;

    public HudEditorScreen(Screen parent) {
        super(Component.literal("Edit DPS HUD"));
        this.parent = parent;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, 0x50000000);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        DpsConfig c = TelosUtilsClient.CONFIG;
        Font font = Minecraft.getInstance().font;

        if (shiftDown()) {
            g.fill(width / 2, 0, width / 2 + 1, height, GUIDE);
            g.fill(0, height / 2, width, height / 2 + 1, GUIDE);
        }

        DpsHud.draw(g, c.hudX, c.hudY, c.hudScale, true);
        boolean hover = isOverHud(mouseX, mouseY);
        g.outline(c.hudX - 1, c.hudY - 1, DpsHud.lastWidth + 2, DpsHud.lastHeight + 2,
                hover || dragging ? Ui.WHITE : 0x80FFFFFF);

        String[] help = {
                "§lEdit DPS HUD",
                "Drag to move  •  Scroll or +/- to resize  •  Shift to snap to centre",
                "Arrow keys nudge  •  R resets  •  Esc to go back",
        };
        int y = height - 12 - help.length * 11;
        for (String line : help) {
            g.centeredText(font, line, width / 2, y, line.startsWith("§l") ? Ui.MINT : Ui.TEXT_DIM);
            y += 11;
        }
        String scale = String.format("%.1fx", c.hudScale);
        g.text(font, scale, c.hudX, c.hudY + DpsHud.lastHeight + 3, Ui.TEXT_DIM, true);
    }

    private boolean isOverHud(double mx, double my) {
        DpsConfig c = TelosUtilsClient.CONFIG;
        return Ui.inside(mx, my, c.hudX, c.hudY, DpsHud.lastWidth, DpsHud.lastHeight);
    }

    private boolean shiftDown() {
        var window = Minecraft.getInstance().getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    private void moveTo(double x, double y) {
        DpsConfig c = TelosUtilsClient.CONFIG;
        int w = DpsHud.lastWidth;
        int h = DpsHud.lastHeight;
        if (shiftDown()) {
            if (Math.abs(x + w / 2.0 - width / 2.0) < SNAP) x = width / 2.0 - w / 2.0;
            if (Math.abs(y + h / 2.0 - height / 2.0) < SNAP) y = height / 2.0 - h / 2.0;
        }
        c.hudX = (int) Math.round(Math.clamp(x, 0, Math.max(0, width - w)));
        c.hudY = (int) Math.round(Math.clamp(y, 0, Math.max(0, height - h)));
    }

    private void rescale(double amount) {
        DpsConfig c = TelosUtilsClient.CONFIG;
        c.hudScale = (float) (Math.round(Math.clamp(c.hudScale + amount, 0.5, 3.0) * 10) / 10.0);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
        DpsConfig c = TelosUtilsClient.CONFIG;
        if (e.button() == 0 && isOverHud(e.x(), e.y())) {
            dragging = true;
            dragDX = c.hudX - e.x();
            dragDY = c.hudY - e.y();
            return true;
        }
        return super.mouseClicked(e, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
        if (dragging) {
            moveTo(e.x() + dragDX, e.y() + dragDY);
            return true;
        }
        return super.mouseDragged(e, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent e) {
        dragging = false;
        return super.mouseReleased(e);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        rescale(Math.signum(v) * 0.1);
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent e) {
        DpsConfig c = TelosUtilsClient.CONFIG;
        switch (e.key()) {
            case GLFW.GLFW_KEY_EQUAL, GLFW.GLFW_KEY_KP_ADD -> rescale(0.1);
            case GLFW.GLFW_KEY_MINUS, GLFW.GLFW_KEY_KP_SUBTRACT -> rescale(-0.1);
            case GLFW.GLFW_KEY_LEFT -> moveTo(c.hudX - 1, c.hudY);
            case GLFW.GLFW_KEY_RIGHT -> moveTo(c.hudX + 1, c.hudY);
            case GLFW.GLFW_KEY_UP -> moveTo(c.hudX, c.hudY - 1);
            case GLFW.GLFW_KEY_DOWN -> moveTo(c.hudX, c.hudY + 1);
            case GLFW.GLFW_KEY_R -> {
                c.hudX = 4;
                c.hudY = 4;
                c.hudScale = 1.0f;
            }
            default -> {
                return super.keyPressed(e);
            }
        }
        return true;
    }

    @Override
    public void onClose() {
        TelosUtilsClient.CONFIG.save();
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
