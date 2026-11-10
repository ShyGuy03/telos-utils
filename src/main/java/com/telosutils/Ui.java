package com.telosutils;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Drawing helpers and the colour palette, styled after Melinoe's click GUI. */
final class Ui {
    static final int ACCENT = 0xFF1BC561;      // Melinoe green
    static final int MINT = 0xFF7CFFB2;        // Melinoe label colour
    static final int PANEL = 0xFF1A1A1A;       // gray26
    static final int CONTROL = 0xFF262626;     // gray38
    static final int CONTROL_HOVER = 0xFF333333;
    static final int WHITE = 0xFFFFFFFF;
    static final int TEXT_DIM = 0xFFAAAAAA;
    static final int TEXT_FAINT = 0xFF6E6E6E;

    private Ui() {}

    /** A filled rectangle with rounded corners of radius {@code r}. */
    static void roundRect(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, int color) {
        r = Math.min(r, Math.min(w, h) / 2);
        if (r <= 0) {
            g.fill(x, y, x + w, y + h, color);
            return;
        }
        g.fill(x, y + r, x + w, y + h - r, color);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = r - (int) Math.round(Math.sqrt(r * r - dy * dy));
            g.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
            g.fill(x + inset, y + h - 1 - i, x + w - inset, y + h - i, color);
        }
    }

    /** A rounded rectangle with a 1px border in {@code border} around a {@code fill} interior. */
    static void outlinedRoundRect(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, int fill, int border) {
        roundRect(g, x, y, w, h, r, border);
        roundRect(g, x + 1, y + 1, w - 2, h - 2, Math.max(0, r - 1), fill);
    }

    static void circle(GuiGraphicsExtractor g, int cx, int cy, int r, int color) {
        for (int dy = -r; dy < r; dy++) {
            double yc = dy + 0.5;
            int half = (int) Math.round(Math.sqrt(r * r - yc * yc));
            g.fill(cx - half, cy + dy, cx + half, cy + dy + 1, color);
        }
    }

    static int withAlpha(int color, int alpha) {
        return (Math.clamp(alpha, 0, 255) << 24) | (color & 0xFFFFFF);
    }

    static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
