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
    private static final int MAX_BARS = 3;

    private static Field eventsField;
    private static boolean unavailable;
    private static final Set<UUID> logged = new HashSet<>();

    private BossHp() {}

    /** Health of each live boss bar, 0..1, in on-screen order. */
    static List<Float> bars() {
        List<Float> out = new ArrayList<>();
        Map<UUID, LerpingBossEvent> events = events();
        if (events == null) return out;
        for (Map.Entry<UUID, LerpingBossEvent> entry : events.entrySet()) {
            LerpingBossEvent bar = entry.getValue();
            float progress = bar.getProgress();
            if (TelosUtilsClient.CONFIG.debug && logged.add(entry.getKey())) {
                System.out.println("[TelosUtils debug] boss bar " + bar.getName() + " at " + progress);
            }
            if (progress > 0f) out.add(progress);
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
