package com.telosutils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Reads the boss bars shown at the top of the screen. Telos sets each bar's fill to the boss's
 * remaining health, so the fill amount is the boss HP percentage (0% means it's dead).
 */
final class BossHp {
    /** A live boss bar: which boss it is (null if unknown), its health 0..1, and its phase thresholds. */
    record BossBar(UUID id, String name, float hp, BossData.Phase[] phases) {}

    private static final int MAX_BARS = 3;

    private static Field eventsField;
    private static boolean unavailable;
    private static final Set<UUID> logged = new HashSet<>();

    private BossHp() {}

    /** Every live boss bar, in on-screen order. */
    static List<BossBar> bars() {
        List<BossBar> out = new ArrayList<>();
        Map<UUID, LerpingBossEvent> events = events();
        if (events == null) return out;
        for (LerpingBossEvent bar : events.values()) {
            float progress = bar.getProgress();
            int hash = bar.getName().hashCode();
            String name = BossData.nameOf(hash);
            if (TelosUtilsClient.CONFIG.debug && logged.add(bar.getId())) {
                String msg = "boss bar " + (name != null ? name : "UNKNOWN") + " hash=" + hash + " hp=" + progress;
                System.out.println("[TelosUtils debug] " + msg + " name=" + bar.getName());
                if (Minecraft.getInstance().player != null) {
                    Minecraft.getInstance().player.sendSystemMessage(
                            net.minecraft.network.chat.Component.literal("§7[DPS debug] " + msg));
                }
            }
            if (progress > 0f) out.add(new BossBar(bar.getId(), name, progress, BossData.phasesOf(name)));
            if (out.size() == MAX_BARS) break;
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static Map<UUID, LerpingBossEvent> events() {
        if (unavailable) return null;
        try {
            if (eventsField == null) {
                eventsField = BossHealthOverlay.class.getDeclaredField("events");
                eventsField.setAccessible(true);
            }
            return (Map<UUID, LerpingBossEvent>) eventsField.get(Minecraft.getInstance().gui.getBossOverlay());
        } catch (ReflectiveOperationException | RuntimeException e) {
            unavailable = true;
            System.out.println("[TelosUtils] Boss HP unavailable, could not read boss bars: " + e);
            return null;
        }
    }
}
