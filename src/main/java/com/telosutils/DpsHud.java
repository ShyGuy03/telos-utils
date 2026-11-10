package com.telosutils;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** The DPS overlay: a dark rounded card with a green accent bar, in Melinoe's style. */
public final class DpsHud {
    private record Stat(String label, String value) {}

    private static final int PAD = 4;
    private static final int BAR = 2;
    private static final int GAP = 10;
    private static final int BOSS_BAR_H = 3;

    /** Size of the HUD the last time it was drawn, in screen (GUI) pixels including scale. */
    static int lastWidth;
    static int lastHeight;

    private DpsHud() {}

    public static void render(GuiGraphicsExtractor g, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        DpsConfig cfg = TelosUtilsClient.CONFIG;
        if (!cfg.hudEnabled || mc.player == null || mc.options.hideGui) return;
        if (mc.screen instanceof HudEditorScreen) return; // the editor draws its own preview
        draw(g, cfg.hudX, cfg.hudY, cfg.hudScale, false);
    }

    /** Draws the HUD at (x, y); with {@code example} it shows sample numbers for the editor. */
    static void draw(GuiGraphicsExtractor g, int x, int y, float scale, boolean example) {
        DpsConfig cfg = TelosUtilsClient.CONFIG;
        DpsTracker t = TelosUtilsClient.TRACKER;
        Font font = Minecraft.getInstance().font;
        long now = System.currentTimeMillis();

        String headline;
        String headlineNote = null;
        List<List<Stat>> rows = new ArrayList<>();
        boolean dimValues = false;

        if (example) {
            headline = "24.6k";
            rows.add(pair(cfg.showPeak ? new Stat("Peak", "31.2k") : null, cfg.showAvg ? new Stat("Avg", "22.9k") : null));
            rows.add(pair(cfg.showTotal ? new Stat("Total", "1.04M") : null, cfg.showTime ? new Stat("Time", "45.3s") : null));
        } else if (t.inFight()) {
            headline = DpsTracker.fmt(t.currentDps(now));
            rows.add(pair(cfg.showPeak ? new Stat("Peak", DpsTracker.fmt(t.peakDps())) : null,
                    cfg.showAvg ? new Stat("Avg", DpsTracker.fmt(t.avgDps())) : null));
            rows.add(pair(cfg.showTotal ? new Stat("Total", DpsTracker.fmt(t.fightTotal())) : null,
                    cfg.showTime ? new Stat("Time", DpsTracker.fmtTime(t.fightSeconds(now)) + "s") : null));
        } else if (t.lastFight() != null) {
            DpsTracker.FightResult f = t.lastFight();
            headline = "--";
            headlineNote = "last fight";
            dimValues = true;
            rows.add(pair(cfg.showPeak ? new Stat("Peak", DpsTracker.fmt(f.peak())) : null,
                    cfg.showAvg ? new Stat("Avg", DpsTracker.fmt(f.avg())) : null));
            rows.add(pair(cfg.showTotal ? new Stat("Total", DpsTracker.fmt(f.total())) : null,
                    cfg.showTime ? new Stat("Time", DpsTracker.fmtTime(f.seconds()) + "s") : null));
        } else {
            headline = "--";
            headlineNote = "waiting...";
        }
        rows.removeIf(List::isEmpty);

        List<Float> bosses = !cfg.showBossHp ? List.of() : example ? List.of(0.624f) : BossHp.bars();
        List<String> bossLabels = new ArrayList<>();
        List<String> bossValues = new ArrayList<>();
        for (int i = 0; i < bosses.size(); i++) {
            bossLabels.add(bosses.size() > 1 ? "Boss " + (i + 1) + " " : "Boss HP ");
            bossValues.add(String.format(Locale.ROOT, "%.1f%%", bosses.get(i) * 100));
        }

        // Column widths so the second stat on each row lines up.
        int col1 = 0;
        int col2 = 0;
        for (List<Stat> row : rows) {
            col1 = Math.max(col1, statWidth(font, row.get(0)));
            if (row.size() > 1) col2 = Math.max(col2, statWidth(font, row.get(1)));
        }
        int rowsWidth = col1 + (col2 > 0 ? GAP + col2 : 0);

        String dpsLabel = "DPS ";
        String headlineText = "§l" + headline;
        int headWidth = font.width(dpsLabel) + font.width(headlineText)
                + (headlineNote != null ? 6 + font.width(headlineNote) : 0);

        int bossWidth = 0;
        for (int i = 0; i < bosses.size(); i++) {
            bossWidth = Math.max(bossWidth, font.width(bossLabels.get(i)) + font.width(bossValues.get(i)));
        }

        int lineH = font.lineHeight + 2;
        int bossH = lineH + BOSS_BAR_H + 2;
        int innerW = Math.max(Math.max(headWidth, rowsWidth), Math.max(bossWidth, bosses.isEmpty() ? 0 : 70));
        int w = BAR + PAD * 2 + innerW;
        int h = PAD * 2 + lineH * (1 + rows.size()) - 2 + bosses.size() * bossH + (bosses.isEmpty() ? 0 : 6);

        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(scale, scale);

        int bg = Ui.withAlpha(Ui.PANEL, cfg.bgOpacity * 255 / 100);
        Ui.roundRect(g, 0, 0, w, h, 3, bg);
        g.fill(0, 2, BAR, h - 2, Ui.ACCENT); // inset so it sits inside the rounded corners

        int tx = BAR + PAD;
        int ty = PAD;
        g.text(font, dpsLabel, tx, ty, Ui.MINT, true);
        int hx = tx + font.width(dpsLabel);
        g.text(font, headlineText, hx, ty, Ui.WHITE, true);
        if (headlineNote != null) {
            g.text(font, headlineNote, hx + font.width(headlineText) + 6, ty, Ui.TEXT_FAINT, true);
        }

        int valueColor = dimValues ? Ui.TEXT_DIM : Ui.WHITE;
        for (List<Stat> row : rows) {
            ty += lineH;
            drawStat(g, font, row.get(0), tx, ty, valueColor);
            if (row.size() > 1) drawStat(g, font, row.get(1), tx + col1 + GAP, ty, valueColor);
        }

        if (!bosses.isEmpty()) {
            ty += lineH + 1;
            g.fill(tx, ty, tx + innerW, ty + 1, 0xFF2C2C2C); // divider between DPS stats and boss HP
            ty += 3;
            for (int i = 0; i < bosses.size(); i++) {
                float hp = bosses.get(i);
                int color = hpColor(hp);
                String label = bossLabels.get(i);
                g.text(font, label, tx, ty, Ui.MINT, true);
                g.text(font, bossValues.get(i), tx + font.width(label), ty, color, true);

                int barY = ty + lineH - 1;
                Ui.roundRect(g, tx, barY, innerW, BOSS_BAR_H, 1, Ui.CONTROL);
                int filled = Math.round(innerW * hp);
                if (filled > 0) Ui.roundRect(g, tx, barY, filled, BOSS_BAR_H, 1, color);
                ty += bossH;
            }
        }

        g.pose().popMatrix();

        lastWidth = Math.round(w * scale);
        lastHeight = Math.round(h * scale);
    }

    /** Green above half health, yellow above 20%, red below. */
    private static int hpColor(float hp) {
        if (hp > 0.5f) return Ui.ACCENT;
        if (hp > 0.2f) return 0xFFFFD34F;
        return 0xFFFF5555;
    }

    private static List<Stat> pair(Stat a, Stat b) {
        List<Stat> row = new ArrayList<>(2);
        if (a != null) row.add(a);
        if (b != null) row.add(b);
        return row;
    }

    private static int statWidth(Font font, Stat s) {
        return font.width(s.label() + " ") + font.width(s.value());
    }

    private static void drawStat(GuiGraphicsExtractor g, Font font, Stat s, int x, int y, int valueColor) {
        String label = s.label() + " ";
        g.text(font, label, x, y, Ui.MINT, true);
        g.text(font, s.value(), x + font.width(label), y, valueColor, true);
    }
}
