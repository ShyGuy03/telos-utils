package com.telosutils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Boss name, HP % and a bar with tick marks where the boss changes phase. */
final class BossHud extends HudElement {
    static final BossHud INSTANCE = new BossHud();

    private static final int PAD = 4;
    private static final int BAR = 2;
    private static final int HP_BAR_H = 4;
    private static final int MIN_W = 110;

    private BossHud() {
        super("Boss HP", "Boss health from the boss bar, with marks where its phases change.");
    }

    @Override boolean enabled() { return TelosUtilsClient.CONFIG.showBossHp; }
    @Override int rawX() { return TelosUtilsClient.CONFIG.bossX; }
    @Override int rawY() { return TelosUtilsClient.CONFIG.bossY; }
    @Override float scale() { return TelosUtilsClient.CONFIG.bossScale; }

    @Override
    void setPosition(int x, int y) {
        TelosUtilsClient.CONFIG.bossX = x;
        TelosUtilsClient.CONFIG.bossY = y;
    }

    @Override void setScale(float scale) { TelosUtilsClient.CONFIG.bossScale = scale; }

    @Override
    void reset() {
        setPosition(4, 50);
        setScale(1.0f);
    }

    @Override
    protected int[] drawContent(GuiGraphicsExtractor g, boolean example) {
        List<BossHp.BossBar> bars = example
                ? List.of(new BossHp.BossBar(UUID.randomUUID(), "Ophanim", 0.634f, BossData.phasesOf("Ophanim")))
                : BossHp.bars();
        if (bars.isEmpty()) return null;

        Font font = Minecraft.getInstance().font;
        int lineH = font.lineHeight + 2;
        int blockH = lineH + HP_BAR_H + 3;

        int innerW = MIN_W;
        boolean anyNext = false;
        for (BossHp.BossBar b : bars) {
            innerW = Math.max(innerW, font.width(title(b)) + 8 + font.width(percent(b)));
            String next = nextPhase(b);
            if (next != null) {
                innerW = Math.max(innerW, font.width(next));
                anyNext = true;
            }
        }
        if (anyNext) blockH += lineH - 1;
        int w = BAR + PAD * 2 + innerW;
        int h = PAD * 2 + blockH * bars.size() - 3;

        card(g, w, h, hpColor(bars.get(0).hp()));

        int tx = BAR + PAD;
        int ty = PAD;
        for (BossHp.BossBar b : bars) {
            int color = hpColor(b.hp());
            String title = title(b);
            g.text(font, title, tx, ty, Ui.MINT, true);
            String pct = percent(b);
            g.text(font, pct, tx + innerW - font.width(pct), ty, color, true);

            int barY = ty + lineH;
            Ui.roundRect(g, tx, barY, innerW, HP_BAR_H, 2, Ui.CONTROL);
            int filled = Math.round(innerW * b.hp());
            if (filled > 0) Ui.roundRect(g, tx, barY, filled, HP_BAR_H, 2, color);
            for (BossData.Phase p : b.phases()) {
                int px = tx + Math.round(innerW * p.percent() / 100f);
                boolean passed = b.hp() * 100 < p.percent();
                g.fill(px, barY - 1, px + 1, barY + HP_BAR_H + 1, passed ? 0x80FFFFFF : Ui.WHITE);
            }
            String next = nextPhase(b);
            if (next != null) g.text(font, next, tx, barY + HP_BAR_H + 3, Ui.TEXT_FAINT, true);
            ty += blockH;
        }
        return new int[] {w, h};
    }

    private static String title(BossHp.BossBar b) {
        return b.name() != null ? b.name() : "Boss";
    }

    /** "Next: Phase 3: Clock at 60%", or null when there are no phase changes left. */
    private static String nextPhase(BossHp.BossBar b) {
        for (BossData.Phase p : b.phases()) {
            if (b.hp() * 100 > p.percent()) return "Next: " + p.label() + " at " + p.percent() + "%";
        }
        return null;
    }

    private static String percent(BossHp.BossBar b) {
        return String.format(Locale.ROOT, "§l%.1f%%", b.hp() * 100);
    }

    /** Green above half health, yellow above 20%, red below. */
    static int hpColor(float hp) {
        if (hp > 0.5f) return Ui.ACCENT;
        if (hp > 0.2f) return 0xFFFFD34F;
        return 0xFFFF5555;
    }
}
