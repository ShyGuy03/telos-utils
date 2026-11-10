package com.telosutils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

/** The DPS card: live DPS on top, then peak/avg and total/time, in Melinoe's style. */
final class DpsHud extends HudElement {
    static final DpsHud INSTANCE = new DpsHud();

    private record Stat(String label, String value) {}

    private static final int PAD = 4;
    private static final int BAR = 2;
    private static final int GAP = 10;

    private DpsHud() {
        super("DPS", "Live, peak and average DPS, total damage and fight time.");
    }

    @Override boolean enabled() { return TelosUtilsClient.CONFIG.hudEnabled; }
    @Override int rawX() { return TelosUtilsClient.CONFIG.hudX; }
    @Override int rawY() { return TelosUtilsClient.CONFIG.hudY; }
    @Override float scale() { return TelosUtilsClient.CONFIG.hudScale; }

    @Override
    void setPosition(int x, int y) {
        TelosUtilsClient.CONFIG.hudX = x;
        TelosUtilsClient.CONFIG.hudY = y;
    }

    @Override void setScale(float scale) { TelosUtilsClient.CONFIG.hudScale = scale; }

    @Override
    void reset() {
        setPosition(4, 4);
        setScale(1.0f);
    }

    @Override
    protected int[] drawContent(GuiGraphicsExtractor g, boolean example) {
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

        int lineH = font.lineHeight + 2;
        int innerW = Math.max(headWidth, rowsWidth);
        int w = BAR + PAD * 2 + innerW;
        int h = PAD * 2 + lineH * (1 + rows.size()) - 2;

        card(g, w, h, Ui.ACCENT);

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
        return new int[] {w, h};
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
