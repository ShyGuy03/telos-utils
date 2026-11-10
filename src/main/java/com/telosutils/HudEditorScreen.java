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

import java.util.Locale;

/**
 * Move and resize every HUD element, like Melinoe's HUD editor: drag to move, scroll (or +/-) over
 * an element to resize it, hold Shift to snap to the centre lines, arrow keys to nudge, R to reset.
 */
public class HudEditorScreen extends Screen {
    private static final int SNAP = 6;
    private static final int GUIDE = 0x8800FFFF;

    private final Screen parent;
    private HudElement dragging;
    private HudElement selected;
    private double dragDX;
    private double dragDY;

    public HudEditorScreen(Screen parent) {
        super(Component.literal("Edit HUD layout"));
        this.parent = parent;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, 0x50000000);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        Font font = Minecraft.getInstance().font;

        if (shiftDown()) {
            g.fill(width / 2, 0, width / 2 + 1, height, GUIDE);
            g.fill(0, height / 2, width, height / 2 + 1, GUIDE);
        }

        HudElement hovered = hovered(mouseX, mouseY);
        for (HudElement e : Huds.ALL) {
            if (!e.enabled()) continue;
            e.draw(g, true);
            boolean active = e == hovered || e == dragging;
            g.outline(e.x() - 1, e.y() - 1, e.lastWidth + 2, e.lastHeight + 2, active ? Ui.WHITE : 0x60FFFFFF);
        }

        // Name and scale of the element under the mouse, like Melinoe.
        HudElement info = dragging != null ? dragging : hovered;
        if (info != null) {
            String label = "§l" + info.name + " §r§7" + String.format(Locale.ROOT, "%.1fx", info.scale());
            int lx = info.x();
            int ly = info.y() - font.lineHeight - 3;
            if (ly < 2) ly = info.y() + info.lastHeight + 3;
            g.text(font, label, lx, ly, Ui.MINT, true);
        }

        String[] help = {
                "§lEdit HUD layout",
                "Drag to move  •  Scroll or +/- over an element to resize  •  Shift to snap to centre",
                "Arrow keys nudge  •  R resets the element under the mouse  •  Esc to go back",
        };
        int y = height - 12 - help.length * 11;
        for (String line : help) {
            g.centeredText(font, line, width / 2, y, line.startsWith("§l") ? Ui.MINT : Ui.TEXT_DIM);
            y += 11;
        }
    }

    private HudElement hovered(double mx, double my) {
        for (int i = Huds.ALL.size() - 1; i >= 0; i--) {
            HudElement e = Huds.ALL.get(i);
            if (e.enabled() && e.isOver(mx, my)) return e;
        }
        return null;
    }

    private boolean shiftDown() {
        var window = Minecraft.getInstance().getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    private void moveTo(HudElement e, double x, double y) {
        int w = e.lastWidth;
        int h = e.lastHeight;
        if (shiftDown()) {
            if (Math.abs(x + w / 2.0 - width / 2.0) < SNAP) x = width / 2.0 - w / 2.0;
            if (Math.abs(y + h / 2.0 - height / 2.0) < SNAP) y = height / 2.0 - h / 2.0;
        }
        e.setPosition((int) Math.round(Math.clamp(x, 0, Math.max(0, width - w))),
                (int) Math.round(Math.clamp(y, 0, Math.max(0, height - h))));
    }

    private static void rescale(HudElement e, double amount) {
        e.setScale((float) (Math.round(Math.clamp(e.scale() + amount, 0.5, 4.0) * 10) / 10.0));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
        HudElement over = hovered(e.x(), e.y());
        if (e.button() == 0 && over != null) {
            dragging = over;
            selected = over;
            dragDX = over.x() - e.x();
            dragDY = over.y() - e.y();
            return true;
        }
        return super.mouseClicked(e, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
        if (dragging != null) {
            moveTo(dragging, e.x() + dragDX, e.y() + dragDY);
            return true;
        }
        return super.mouseDragged(e, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent e) {
        dragging = null;
        return super.mouseReleased(e);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        HudElement over = hovered(mx, my);
        if (over != null) {
            rescale(over, Math.signum(v) * 0.1);
            selected = over;
            return true;
        }
        return super.mouseScrolled(mx, my, h, v);
    }

    @Override
    public boolean keyPressed(KeyEvent e) {
        Minecraft mc = Minecraft.getInstance();
        double mx = mc.mouseHandler.getScaledXPos(mc.getWindow());
        double my = mc.mouseHandler.getScaledYPos(mc.getWindow());
        HudElement target = hovered(mx, my);
        if (target == null) target = selected;
        if (target == null) return super.keyPressed(e);

        switch (e.key()) {
            case GLFW.GLFW_KEY_EQUAL, GLFW.GLFW_KEY_KP_ADD -> rescale(target, 0.1);
            case GLFW.GLFW_KEY_MINUS, GLFW.GLFW_KEY_KP_SUBTRACT -> rescale(target, -0.1);
            case GLFW.GLFW_KEY_LEFT -> moveTo(target, target.x() - 1, target.y());
            case GLFW.GLFW_KEY_RIGHT -> moveTo(target, target.x() + 1, target.y());
            case GLFW.GLFW_KEY_UP -> moveTo(target, target.x(), target.y() - 1);
            case GLFW.GLFW_KEY_DOWN -> moveTo(target, target.x(), target.y() + 1);
            case GLFW.GLFW_KEY_R -> target.reset();
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
