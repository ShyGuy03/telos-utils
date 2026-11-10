package com.telosutils;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoubleSupplier;

/**
 * The settings menu: a wide, draggable two-column panel with toggles, sliders and buttons, styled
 * after Melinoe's click GUI. Drag the title bar to move it, hover an option for help.
 */
public class DpsConfigScreen extends Screen {
    private static final int COL_W = 184;
    private static final int GAP = 8;
    private static final int W = COL_W * 2 + GAP + 8;
    private static final int HEADER = 20;
    private static final int PAD = 4;
    private static final long TOOLTIP_DELAY_MS = 400;

    private final Screen parent;
    private final List<Row> left = new ArrayList<>();
    private final List<Row> right = new ArrayList<>();
    private final List<Row> footer = new ArrayList<>();
    private final List<Row> all = new ArrayList<>();

    private int px;
    private int py;
    private boolean draggingPanel;
    private double dragDX;
    private double dragDY;
    private int scroll;
    private int bodyTop;
    private int bodyH;
    private int contentH;

    private Row hovered;
    private long hoverStart;
    private Slider activeSlider;
    private KeybindRow listening;

    public DpsConfigScreen(Screen parent) {
        super(Component.literal("Telos Utils"));
        this.parent = parent;
        build();
        all.addAll(left);
        all.addAll(right);
        all.addAll(footer);
    }

    private void build() {
        DpsConfig c = TelosUtilsClient.CONFIG;

        // Left column: the DPS meter and how it tracks damage.
        left.add(new Section("DPS meter"));
        left.add(new Toggle("Show DPS", "Shows the DPS card on screen.",
                () -> c.hudEnabled, v -> c.hudEnabled = v));
        left.add(new Toggle("Peak DPS", "The highest DPS you reached during the fight, checked once a second.",
                () -> c.showPeak, v -> c.showPeak = v));
        left.add(new Toggle("Average DPS", "Your average DPS, checked once a second. Seconds where you weren't hitting anything don't count.",
                () -> c.showAvg, v -> c.showAvg = v));
        left.add(new Toggle("Total damage", "All damage dealt during the fight.",
                () -> c.showTotal, v -> c.showTotal = v));
        left.add(new Toggle("Fight time", "How long the current fight has lasted.",
                () -> c.showTime, v -> c.showTime = v));
        left.add(new Toggle("Chat summary", "Posts a summary in chat when a fight ends.",
                () -> c.chatSummary, v -> c.chatSummary = v));
        left.add(new Section("Tracking"));
        left.add(new Slider("DPS window", "Live DPS is the damage from the last N seconds divided by N.",
                1, 15, 1, () -> c.windowSeconds, v -> c.windowSeconds = (int) Math.round(v),
                v -> Math.round(v) + "s"));
        left.add(new Slider("Fight ends after", "A fight ends after this many seconds without dealing damage.",
                2, 30, 1, () -> c.fightTimeoutSeconds, v -> c.fightTimeoutSeconds = (int) Math.round(v),
                v -> Math.round(v) + "s"));
        left.add(new Slider("Range", "Only damage numbers within this many blocks of you count.",
                4, 64, 1, () -> c.range, v -> c.range = (int) Math.round(v),
                v -> Math.round(v) + " blocks"));

        // Right column: bosses, layout and controls.
        right.add(new Section("Bosses"));
        right.add(new Toggle("Boss HP", "Shows the boss's name and health, read from the boss bar, with marks where its phases change.",
                () -> c.showBossHp, v -> c.showBossHp = v));
        right.add(new Toggle("Phase warnings", "Pops up a warning when a boss is about to change phase, and announces the new phase.",
                () -> c.phaseWarnings, v -> c.phaseWarnings = v));
        right.add(new Slider("Warn within", "How close to a phase change (in boss HP %) the warning appears. 5% means it shows from 71% for a phase at 66%.",
                1, 15, 0.5, () -> c.warnWithin, v -> c.warnWithin = (float) v,
                v -> String.format(Locale.ROOT, "%.1f%%", v)));
        right.add(new Toggle("Warning sound", "Plays a ping when the warning appears and a bell when the new phase starts.",
                () -> c.phaseSound, v -> c.phaseSound = v));
        right.add(new Section("Layout"));
        right.add(new Slider("Background", "How see-through the cards' backgrounds are.",
                0, 100, 5, () -> c.bgOpacity, v -> c.bgOpacity = (int) Math.round(v),
                v -> Math.round(v) + "%"));
        right.add(new Buttons(new Button("Edit HUD layout",
                () -> Minecraft.getInstance().setScreen(new HudEditorScreen(this)))));
        right.add(new Section("Controls"));
        right.add(new KeybindRow("Menu key", "The key that opens this menu. Click, then press a key. Esc cancels."));
        right.add(new Toggle("Debug", "Prints every floating text and boss bar the mod sees in chat. Useful for reporting problems.",
                () -> c.debug, v -> c.debug = v));

        footer.add(new Buttons(
                new Button("Reset stats", TelosUtilsClient.TRACKER::reset),
                new Button("Done", this::onClose)));
    }

    private static int heightOf(List<Row> rows) {
        int h = 0;
        for (Row r : rows) h += r.height();
        return h;
    }

    @Override
    protected void init() {
        DpsConfig c = TelosUtilsClient.CONFIG;
        contentH = PAD * 2 + Math.max(heightOf(left), heightOf(right)) + 4 + heightOf(footer);
        int panelH = HEADER + contentH;
        px = c.menuX >= 0 ? c.menuX : (width - W) / 2;
        py = c.menuY >= 0 ? c.menuY : Math.max(8, (height - panelH) / 2);
        clampPanel();
    }

    private void clampPanel() {
        px = Math.clamp(px, 0, Math.max(0, width - W));
        py = Math.clamp(py, 0, Math.max(0, height - HEADER - 20));
        bodyTop = py + HEADER;
        bodyH = Math.min(contentH, height - bodyTop - 6);
        scroll = Math.clamp(scroll, 0, Math.max(0, contentH - bodyH));
    }

    /** Positions every row: two columns side by side, then the footer across the full width. */
    private void layout() {
        int top = bodyTop + PAD - scroll;
        int y = top;
        for (Row r : left) {
            r.place(px + 4, y, COL_W);
            y += r.height();
        }
        int leftBottom = y;
        y = top;
        for (Row r : right) {
            r.place(px + 4 + COL_W + GAP, y, COL_W);
            y += r.height();
        }
        y = Math.max(leftBottom, y) + 4;
        for (Row r : footer) {
            r.place(px + 4, y, W - 8);
            y += r.height();
        }
    }

    // ---- Rendering ----

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, 0x70000000);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        Font font = Minecraft.getInstance().font;
        clampPanel();
        layout();

        int totalH = HEADER + bodyH;
        Ui.roundRect(g, px + 2, py + 3, W, totalH, 5, 0x50000000); // drop shadow
        Ui.roundRect(g, px, py, W, totalH, 5, 0xF2141414);
        Ui.roundRect(g, px, py, W, HEADER + 4, 5, Ui.PANEL);
        g.fill(px, py + HEADER, px + W, py + HEADER + 1, Ui.ACCENT);

        String title = "§lTelos Utils";
        g.text(font, title, px + (W - font.width(title)) / 2, py + 6, Ui.WHITE, true);

        Row nowHovered = null;
        g.enableScissor(px, bodyTop + 1, px + W, bodyTop + bodyH);
        // Divider between the two columns.
        int divX = px + 4 + COL_W + GAP / 2;
        g.fill(divX, bodyTop + PAD + 2 - scroll, divX + 1,
                bodyTop + PAD - scroll + Math.max(heightOf(left), heightOf(right)), 0xFF242424);
        for (Row r : all) {
            boolean visible = r.y + r.height() > bodyTop && r.y < bodyTop + bodyH;
            if (!visible) continue;
            boolean hover = Ui.inside(mouseX, mouseY, r.x, r.y, r.w, r.height())
                    && mouseY > bodyTop && mouseY < bodyTop + bodyH;
            if (hover) nowHovered = r;
            r.render(g, font, mouseX, mouseY, hover);
        }
        g.disableScissor();

        if (contentH > bodyH) {
            int trackH = bodyH - 4;
            int thumbH = Math.max(12, trackH * bodyH / contentH);
            int thumbY = bodyTop + 2 + (trackH - thumbH) * scroll / (contentH - bodyH);
            g.fill(px + W - 3, bodyTop + 2, px + W - 1, bodyTop + 2 + trackH, Ui.CONTROL);
            g.fill(px + W - 3, thumbY, px + W - 1, thumbY + thumbH, Ui.ACCENT);
        }

        if (nowHovered != hovered) {
            hovered = nowHovered;
            hoverStart = System.currentTimeMillis();
        }
        if (hovered != null && hovered.desc != null && activeSlider == null && !draggingPanel
                && System.currentTimeMillis() - hoverStart > TOOLTIP_DELAY_MS) {
            drawTooltip(g, font, hovered.desc, mouseX, mouseY);
        }
    }

    private void drawTooltip(GuiGraphicsExtractor g, Font font, String text, int mouseX, int mouseY) {
        List<String> lines = wrap(font, text, 160);
        int tw = 0;
        for (String l : lines) tw = Math.max(tw, font.width(l));
        int bw = tw + 12;
        int bh = lines.size() * (font.lineHeight + 1) + 10;
        int bx = mouseX + 12;
        if (bx + bw > width - 2) bx = mouseX - 12 - bw;
        int by = Math.clamp(mouseY + 8, 2, Math.max(2, height - bh - 2));
        Ui.outlinedRoundRect(g, bx, by, bw, bh, 4, Ui.CONTROL, Ui.ACCENT);
        int ty = by + 6;
        for (String l : lines) {
            g.text(font, l, bx + 6, ty, Ui.WHITE, false);
            ty += font.lineHeight + 1;
        }
    }

    private static List<String> wrap(Font font, String text, int maxW) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (font.width(candidate) > maxW && !line.isEmpty()) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (!line.isEmpty()) lines.add(line.toString());
        return lines;
    }

    // ---- Input ----

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
        double mx = e.x();
        double my = e.y();
        if (listening != null) {
            listening = null; // clicking anywhere cancels key capture
            return true;
        }
        if (e.button() == 0 && Ui.inside(mx, my, px, py, W, HEADER)) {
            draggingPanel = true;
            dragDX = px - mx;
            dragDY = py - my;
            return true;
        }
        if (my > bodyTop && my < bodyTop + bodyH) {
            for (Row r : all) {
                if (Ui.inside(mx, my, r.x, r.y, r.w, r.height()) && r.click(mx, my, e.button())) return true;
            }
        }
        return super.mouseClicked(e, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
        if (draggingPanel) {
            px = (int) Math.round(e.x() + dragDX);
            py = (int) Math.round(e.y() + dragDY);
            clampPanel();
            return true;
        }
        if (activeSlider != null) {
            activeSlider.setFromMouse(e.x());
            return true;
        }
        return super.mouseDragged(e, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent e) {
        draggingPanel = false;
        activeSlider = null;
        return super.mouseReleased(e);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        if (Ui.inside(mx, my, px, bodyTop, W, bodyH)) {
            scroll -= (int) Math.round(v * 14);
            clampPanel();
            return true;
        }
        return super.mouseScrolled(mx, my, h, v);
    }

    @Override
    public boolean keyPressed(KeyEvent e) {
        if (listening != null) {
            if (!e.isEscape()) {
                TelosUtilsClient.OPEN_MENU_KEY.setKey(InputConstants.getKey(e));
                KeyMapping.resetMapping();
                Minecraft.getInstance().options.save();
            }
            listening = null;
            return true;
        }
        if (TelosUtilsClient.OPEN_MENU_KEY.matches(e)) {
            onClose();
            return true;
        }
        return super.keyPressed(e);
    }

    @Override
    public void onClose() {
        DpsConfig c = TelosUtilsClient.CONFIG;
        c.menuX = px;
        c.menuY = py;
        c.save();
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---- Rows ----

    private abstract static class Row {
        final String label;
        final String desc;
        int x;
        int y;
        int w;

        Row(String label, String desc) {
            this.label = label;
            this.desc = desc;
        }

        void place(int x, int y, int w) {
            this.x = x;
            this.y = y;
            this.w = w;
        }

        abstract int height();

        abstract void render(GuiGraphicsExtractor g, Font font, int mx, int my, boolean hover);

        boolean click(double mx, double my, int button) {
            return false;
        }
    }

    private static class Section extends Row {
        Section(String title) {
            super(title.toUpperCase(Locale.ROOT), null);
        }

        @Override
        int height() {
            return 15;
        }

        @Override
        void render(GuiGraphicsExtractor g, Font font, int mx, int my, boolean hover) {
            g.text(font, label, x + 4, y + 5, Ui.MINT, false);
            int lx = x + 4 + font.width(label) + 5;
            g.fill(lx, y + 9, x + w - 4, y + 10, 0xFF2C2C2C);
        }
    }

    private static class Toggle extends Row {
        private final BooleanSupplier get;
        private final Consumer<Boolean> set;

        Toggle(String label, String desc, BooleanSupplier get, Consumer<Boolean> set) {
            super(label, desc);
            this.get = get;
            this.set = set;
        }

        @Override
        int height() {
            return 16;
        }

        @Override
        void render(GuiGraphicsExtractor g, Font font, int mx, int my, boolean hover) {
            if (hover) g.fill(x, y, x + w, y + height(), 0x0FFFFFFF);
            boolean on = get.getAsBoolean();
            g.text(font, label, x + 4, y + 4, on ? Ui.WHITE : Ui.TEXT_DIM, false);

            int tw = 20;
            int th = 10;
            int tx = x + w - 4 - tw;
            int ty = y + 3;
            int fill = on ? Ui.ACCENT : (hover ? Ui.CONTROL_HOVER : Ui.CONTROL);
            int border = on ? Ui.ACCENT : 0xFF3C3C3C;
            Ui.outlinedRoundRect(g, tx, ty, tw, th, 5, fill, border);
            Ui.circle(g, on ? tx + tw - 5 : tx + 5, ty + 5, 3, Ui.WHITE);
        }

        @Override
        boolean click(double mx, double my, int button) {
            if (button != 0) return false;
            set.accept(!get.getAsBoolean());
            return true;
        }
    }

    private class Slider extends Row {
        private final double min;
        private final double max;
        private final double step;
        private final DoubleSupplier get;
        private final DoubleConsumer set;
        private final DoubleFunction<String> format;

        Slider(String label, String desc, double min, double max, double step,
               DoubleSupplier get, DoubleConsumer set, DoubleFunction<String> format) {
            super(label, desc);
            this.min = min;
            this.max = max;
            this.step = step;
            this.get = get;
            this.set = set;
            this.format = format;
        }

        @Override
        int height() {
            return 25;
        }

        @Override
        void render(GuiGraphicsExtractor g, Font font, int mx, int my, boolean hover) {
            if (hover) g.fill(x, y, x + w, y + height(), 0x0FFFFFFF);
            double v = get.getAsDouble();
            g.text(font, label, x + 4, y + 3, Ui.WHITE, false);
            String value = format.apply(v);
            g.text(font, value, x + w - 4 - font.width(value), y + 3, Ui.TEXT_DIM, false);

            double pct = (v - min) / (max - min);
            int tx = x + 4;
            int tw = w - 8;
            int ty = y + 15;
            Ui.roundRect(g, tx, ty, tw, 4, 2, Ui.CONTROL);
            int filled = (int) Math.round(tw * pct);
            if (filled > 0) Ui.roundRect(g, tx, ty, filled, 4, 2, Ui.ACCENT);
            boolean big = hover || activeSlider == this;
            Ui.circle(g, tx + filled, ty + 2, big ? 5 : 4, Ui.WHITE);
        }

        @Override
        boolean click(double mx, double my, int button) {
            if (button != 0) return false;
            activeSlider = this;
            setFromMouse(mx);
            return true;
        }

        void setFromMouse(double mx) {
            double pct = Math.clamp((mx - (x + 4)) / (w - 8), 0.0, 1.0);
            double v = min + Math.round(pct * (max - min) / step) * step;
            set.accept(Math.clamp(v, min, max));
        }
    }

    private record Button(String label, Runnable action) {}

    private static class Buttons extends Row {
        private final Button[] buttons;

        Buttons(Button... buttons) {
            super(null, null);
            this.buttons = buttons;
        }

        @Override
        int height() {
            return 22;
        }

        private int buttonW() {
            return (w - 8 - (buttons.length - 1) * 4) / buttons.length;
        }

        @Override
        void render(GuiGraphicsExtractor g, Font font, int mx, int my, boolean hover) {
            int bw = buttonW();
            for (int i = 0; i < buttons.length; i++) {
                int bx = x + 4 + i * (bw + 4);
                boolean over = Ui.inside(mx, my, bx, y + 3, bw, 16);
                Ui.outlinedRoundRect(g, bx, y + 3, bw, 16, 4, over ? Ui.CONTROL_HOVER : Ui.CONTROL, Ui.ACCENT);
                String l = buttons[i].label();
                g.text(font, l, bx + (bw - font.width(l)) / 2, y + 7, Ui.WHITE, false);
            }
        }

        @Override
        boolean click(double mx, double my, int button) {
            if (button != 0) return false;
            int bw = buttonW();
            for (int i = 0; i < buttons.length; i++) {
                int bx = x + 4 + i * (bw + 4);
                if (mx >= bx && mx < bx + bw) {
                    buttons[i].action().run();
                    return true;
                }
            }
            return false;
        }
    }

    private class KeybindRow extends Row {
        KeybindRow(String label, String desc) {
            super(label, desc);
        }

        @Override
        int height() {
            return 18;
        }

        @Override
        void render(GuiGraphicsExtractor g, Font font, int mx, int my, boolean hover) {
            if (hover) g.fill(x, y, x + w, y + height(), 0x0FFFFFFF);
            g.text(font, label, x + 4, y + 5, Ui.WHITE, false);
            boolean capturing = listening == this;
            String key = capturing ? "press a key..." : TelosUtilsClient.OPEN_MENU_KEY.getTranslatedKeyMessage().getString();
            int bw = font.width(key) + 12;
            int bx = x + w - 4 - bw;
            Ui.outlinedRoundRect(g, bx, y + 2, bw, 14, 4, Ui.CONTROL, Ui.ACCENT);
            g.text(font, key, bx + 6, y + 5, capturing ? 0xFFFFFF55 : Ui.WHITE, false);
        }

        @Override
        boolean click(double mx, double my, int button) {
            if (button != 0) return false;
            listening = this;
            return true;
        }
    }
}
