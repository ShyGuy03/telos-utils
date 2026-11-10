package com.telosutils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A pop-up that warns when a boss is about to change phase ("PHASE 2 IN 3.2%"), and briefly
 * announces the new phase once it starts. The warning appears when the boss's HP is within
 * {@code warnWithin} percent above one of its phase thresholds.
 */
final class PhaseWarning extends HudElement {
    static final PhaseWarning INSTANCE = new PhaseWarning();

    private record Upcoming(String boss, BossData.Phase phase, float distance) {}

    private static final int WARN = 0xFFFFD34F;
    private static final int PHASE_START = 0xFFFF5555;
    private static final long FLASH_MS = 2500;

    private final Map<UUID, Float> lastHp = new HashMap<>();
    private final Set<String> warned = new HashSet<>();
    private String flashBoss;
    private BossData.Phase flashPhase;
    private long flashUntil;

    private PhaseWarning() {
        super("Phase warning", "Pops up when a boss is close to changing phase.");
    }

    @Override boolean enabled() { return TelosUtilsClient.CONFIG.phaseWarnings; }
    @Override int rawX() { return TelosUtilsClient.CONFIG.warnX; }
    @Override int rawY() { return TelosUtilsClient.CONFIG.warnY; }
    @Override float scale() { return TelosUtilsClient.CONFIG.warnScale; }

    @Override
    void setPosition(int x, int y) {
        TelosUtilsClient.CONFIG.warnX = x;
        TelosUtilsClient.CONFIG.warnY = y;
    }

    @Override void setScale(float scale) { TelosUtilsClient.CONFIG.warnScale = scale; }

    @Override
    void reset() {
        setPosition(-1, 60);
        setScale(1.0f);
    }

    /** Runs every tick: plays the sounds and notices when a phase starts. */
    void tick(Minecraft mc) {
        DpsConfig cfg = TelosUtilsClient.CONFIG;
        if (!cfg.phaseWarnings || mc.player == null) {
            lastHp.clear();
            warned.clear();
            return;
        }
        List<BossHp.BossBar> bars = BossHp.bars();
        Set<UUID> live = new HashSet<>();
        for (BossHp.BossBar b : bars) {
            live.add(b.id());
            float hp = b.hp() * 100;
            Float prev = lastHp.put(b.id(), hp);
            for (BossData.Phase phase : b.phases()) {
                int t = phase.percent();
                if (prev != null && prev > t && hp <= t) {
                    flashBoss = b.name();
                    flashPhase = phase;
                    flashUntil = System.currentTimeMillis() + FLASH_MS;
                    playSound(mc, true);
                } else if (hp > t && hp - t <= cfg.warnWithin && warned.add(b.id() + ":" + t)) {
                    playSound(mc, false);
                }
            }
        }
        lastHp.keySet().retainAll(live);
        warned.removeIf(key -> live.stream().noneMatch(id -> key.startsWith(id.toString())));
    }

    private static void playSound(Minecraft mc, boolean phaseStarted) {
        if (!TelosUtilsClient.CONFIG.phaseSound) return;
        if (phaseStarted) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BELL, 1.0f));
        } else {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING, 1.5f));
        }
    }

    /** The closest phase change the boss is about to reach, if it's within the warning distance. */
    private static Upcoming upcoming(List<BossHp.BossBar> bars) {
        float within = TelosUtilsClient.CONFIG.warnWithin;
        Upcoming best = null;
        for (BossHp.BossBar b : bars) {
            float hp = b.hp() * 100;
            for (BossData.Phase phase : b.phases()) {
                float distance = hp - phase.percent();
                if (distance > 0 && distance <= within && (best == null || distance < best.distance())) {
                    best = new Upcoming(b.name(), phase, distance);
                }
            }
        }
        return best;
    }

    @Override
    protected int[] drawContent(GuiGraphicsExtractor g, boolean example) {
        long now = System.currentTimeMillis();
        String title;
        String subtitle;
        int color;

        if (example) {
            title = "⚠ PHASE 3: CLOCK IN 3.2%";
            subtitle = "Ophanim · at 60%";
            color = WARN;
        } else if (now < flashUntil && flashPhase != null) {
            title = flashPhase.label().toUpperCase(Locale.ROOT) + "!";
            subtitle = (flashBoss != null ? flashBoss : "Boss") + " dropped below " + flashPhase.percent() + "%";
            color = PHASE_START;
        } else {
            Upcoming u = upcoming(BossHp.bars());
            if (u == null) return null;
            title = String.format(Locale.ROOT, "⚠ %s IN %.1f%%", u.phase().label().toUpperCase(Locale.ROOT), u.distance());
            subtitle = (u.boss() != null ? u.boss() : "Boss") + " · at " + u.phase().percent() + "%";
            color = WARN;
        }

        Font font = Minecraft.getInstance().font;
        float titleScale = 1.5f;
        String boldTitle = "§l" + title;
        int titleW = Math.round(font.width(boldTitle) * titleScale);
        int titleH = Math.round(font.lineHeight * titleScale);
        int pad = 6;
        int w = Math.max(titleW, font.width(subtitle)) + pad * 2;
        int h = pad * 2 + titleH + 3 + font.lineHeight;

        // Pulse the border so it catches your eye.
        int pulse = example ? 255 : (int) (150 + 105 * Math.sin(now / 120.0));
        int bg = Ui.withAlpha(Ui.PANEL, Math.max(170, TelosUtilsClient.CONFIG.bgOpacity * 255 / 100));
        Ui.roundRect(g, 0, 0, w, h, 4, Ui.withAlpha(color, pulse));
        Ui.roundRect(g, 1, 1, w - 2, h - 2, 3, bg);

        g.pose().pushMatrix();
        g.pose().translate((w - titleW) / 2f, pad);
        g.pose().scale(titleScale, titleScale);
        g.text(font, boldTitle, 0, 0, color, true);
        g.pose().popMatrix();

        g.text(font, subtitle, (w - font.width(subtitle)) / 2, pad + titleH + 3, Ui.TEXT_DIM, true);
        return new int[] {w, h};
    }
}
