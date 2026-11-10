package com.telosutils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;

/**
 * Watches for newly spawned floating-text entities near the player, reads damage numbers
 * from them, and keeps live DPS, peak DPS, average DPS and per-fight totals.
 *
 * <p>Live DPS is the damage in the last {@code windowSeconds} divided by that window. Once a second
 * that value is sampled; peak is the highest sample and average is the mean of the samples taken
 * while you were actively hitting, so standing around mid-fight doesn't drag the average down.
 */
public class DpsTracker {
    private record Hit(long time, double amount) {}

    /** A summary of a finished fight. */
    public record FightResult(double seconds, double total, double avg, double peak) {}

    /** Text entities may get their text a tick or two after spawning; wait this long before giving up. */
    private static final int MAX_PENDING_TICKS = 5;
    /** Samples are only counted toward the average if you dealt damage within this long. */
    private static final long ACTIVE_GAP_MS = 2000;
    private static final long SAMPLE_MS = 1000;

    private final ArrayDeque<Hit> recent = new ArrayDeque<>();
    private final Map<Integer, Integer> pending = new HashMap<>();
    private final Set<Integer> done = new HashSet<>();
    private ClientLevel lastWorld;

    private long fightStart = -1;
    private long lastHit = -1;
    private long nextSample;
    private double fightTotal;
    private double peakDps;
    private double sampleSum;
    private int sampleCount;
    private FightResult lastFight;

    public void tick(Minecraft client) {
        ClientLevel world = client.level;
        LocalPlayer player = client.player;
        if (world == null || player == null) {
            lastWorld = null;
            return;
        }
        DpsConfig cfg = TelosUtilsClient.CONFIG;
        long now = System.currentTimeMillis();

        // New world or dimension: treat everything already loaded as old so static holograms are never counted.
        if (world != lastWorld) {
            lastWorld = world;
            pending.clear();
            done.clear();
            recent.clear();
            fightStart = -1;
            TelosFont.invalidate(); // the server's resource pack may have changed
            for (Entity e : world.entitiesForRendering()) done.add(e.getId());
            return;
        }

        Set<Integer> present = new HashSet<>();
        double maxDistSq = (double) cfg.range * cfg.range;

        for (Entity e : world.entitiesForRendering()) {
            int id = e.getId();
            present.add(id);
            if (done.contains(id)) continue;

            if (e instanceof Player || e.distanceToSqr(player) > maxDistSq) {
                done.add(id);
                continue;
            }

            Component text = textOf(e);
            TelosFont.Decoded decoded = text == null ? null : TelosFont.decode(text);
            String raw = decoded == null ? "" : decoded.text();
            // Damage you deal is drawn in the Telos font; plain-text numbers (like damage you take) are skipped.
            OptionalDouble dmg = decoded != null && decoded.usedTelosFont()
                    ? DamageParser.parse(raw)
                    : OptionalDouble.empty();

            if (dmg.isPresent()) {
                done.add(id);
                pending.remove(id);
                addHit(now, dmg.getAsDouble());
                debug(client, text, raw, dmg);
            } else {
                int waited = pending.merge(id, 1, Integer::sum);
                if (waited > MAX_PENDING_TICKS) {
                    done.add(id);
                    pending.remove(id);
                    if (!raw.isEmpty()) debug(client, text, raw, dmg);
                }
            }
        }

        // Forget entities that have despawned.
        done.retainAll(present);
        pending.keySet().retainAll(present);

        while (!recent.isEmpty() && now - recent.peekFirst().time() > cfg.windowSeconds * 1000L) recent.pollFirst();

        if (fightStart >= 0) {
            while (now >= nextSample) {
                nextSample += SAMPLE_MS;
                if (now - lastHit <= ACTIVE_GAP_MS) {
                    double dps = currentDps(now);
                    sampleSum += dps;
                    sampleCount++;
                    peakDps = Math.max(peakDps, dps);
                }
            }
            if (now - lastHit > cfg.fightTimeoutSeconds * 1000L) endFight(client);
        }
    }

    private static Component textOf(Entity e) {
        if (e instanceof Display.TextDisplay td) {
            Display.TextDisplay.TextRenderState state = td.textRenderState();
            return state == null ? null : state.text();
        }
        if (e.hasCustomName()) return e.getCustomName();
        return null;
    }

    private void addHit(long now, double amount) {
        if (fightStart < 0) {
            fightStart = now;
            nextSample = now + SAMPLE_MS;
            fightTotal = 0;
            peakDps = 0;
            sampleSum = 0;
            sampleCount = 0;
        }
        lastHit = now;
        fightTotal += amount;
        recent.addLast(new Hit(now, amount));
    }

    private void endFight(Minecraft client) {
        double seconds = Math.max(1.0, (lastHit - fightStart) / 1000.0);
        lastFight = new FightResult(seconds, fightTotal, avgDps(), peakDps);
        if (TelosUtilsClient.CONFIG.chatSummary && client.player != null) {
            client.player.sendSystemMessage(Component.literal(String.format(Locale.ROOT,
                    "§a[DPS] §7Fight over: §f%ss §7| §f%s §7dmg | avg §f%s §7| peak §f%s",
                    fmtTime(seconds), fmt(fightTotal), fmt(lastFight.avg()), fmt(peakDps))));
        }
        fightStart = -1;
        recent.clear();
    }

    private void debug(Minecraft client, Component text, String raw, OptionalDouble dmg) {
        if (!TelosUtilsClient.CONFIG.debug || client.player == null) return;
        String result = dmg.isPresent() ? "counted " + fmt(dmg.getAsDouble()) : "ignored";
        client.player.sendSystemMessage(Component.literal("§7[DPS debug] '" + raw + "' -> " + result));
        System.out.println("[TelosUtils debug] " + text + " -> " + result); // full styled component, in latest.log
    }

    public void reset() {
        recent.clear();
        fightStart = -1;
        fightTotal = 0;
        peakDps = 0;
        sampleSum = 0;
        sampleCount = 0;
        lastFight = null;
    }

    public boolean inFight() { return fightStart >= 0; }

    /** Damage in the rolling window divided by the window (or by the fight length, early in a fight). */
    public double currentDps(long now) {
        if (fightStart < 0) return 0;
        double sum = 0;
        for (Hit h : recent) sum += h.amount();
        double windowSec = TelosUtilsClient.CONFIG.windowSeconds;
        double elapsedSec = (now - fightStart) / 1000.0;
        return sum / Math.max(1.0, Math.min(windowSec, elapsedSec));
    }

    /** Mean of the once-a-second samples taken while actively hitting. */
    public double avgDps() {
        if (sampleCount == 0) return fightTotal / Math.max(1.0, (lastHit - fightStart) / 1000.0);
        return sampleSum / sampleCount;
    }

    public double peakDps() { return Math.max(peakDps, sampleCount == 0 ? currentDps(System.currentTimeMillis()) : 0); }
    public double fightTotal() { return fightTotal; }
    public double fightSeconds(long now) { return fightStart < 0 ? 0 : (now - fightStart) / 1000.0; }
    public FightResult lastFight() { return lastFight; }

    static String fmt(double v) {
        if (v >= 1_000_000) return String.format(Locale.ROOT, "%.2fM", v / 1_000_000);
        if (v >= 10_000) return String.format(Locale.ROOT, "%.1fk", v / 1_000);
        if (v >= 100) return String.format(Locale.ROOT, "%.0f", v);
        return String.format(Locale.ROOT, "%.1f", v);
    }

    static String fmtTime(double seconds) {
        return String.format(Locale.ROOT, "%.1f", seconds);
    }
}
