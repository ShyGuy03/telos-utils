package com.telosutils;

import java.util.Map;

/**
 * Which boss a boss bar belongs to, and where its phases change.
 *
 * <p>Bosses are recognised by the hash of their boss-bar name ({@code Component.hashCode()}).
 * The hash table comes from Melinoe's boss data (https://github.com/House-Hades/Melinoe, LGPL-3.0).
 */
final class BossData {
    private BossData() {}

    /** Boss-bar name hash -> boss name. */
    private static final Map<Integer, String> NAMES = Map.ofEntries(
            Map.entry(-342545608, "Anubis"),
            Map.entry(-1370503157, "Cog Sentinel"),
            Map.entry(-1253632898, "Astaroth"),
            Map.entry(-168181711, "Chungus"),
            Map.entry(-1343349613, "Freddy"),
            Map.entry(-168176906, "Glumi"),
            Map.entry(1368623635, "Illarius"),
            Map.entry(-1254008649, "Lotil"),
            Map.entry(-1907114029, "Oozul"),
            Map.entry(1368934038, "Tidol"),
            Map.entry(-1622056066, "Valus"),
            Map.entry(-1240191621, "Hollowbane"),
            Map.entry(-1048713371, "Claus"),
            Map.entry(-1370656917, "Warden"),
            Map.entry(-1370655956, "Herald"),
            Map.entry(-1370654995, "Reaper"),
            Map.entry(-1370654034, "Defender"),
            Map.entry(-167372549, "Eddie"),
            Map.entry(-1381648356, "Zhum"),
            Map.entry(-1659173216, "Drayruk"),
            Map.entry(-973820290, "Miraj"),
            Map.entry(230865898, "Khufu"),
            Map.entry(-167370627, "Choji"),
            Map.entry(291455870, "Flora"),
            Map.entry(-1019671711, "Malfas"),
            Map.entry(-1659169372, "Heptavius"),
            Map.entry(-1659167450, "Arctic Colossus"),
            Map.entry(-1659168411, "Oog"),
            Map.entry(-1659166489, "Magnus"),
            Map.entry(-1253198526, "Pyro"),
            Map.entry(-1621485232, "Thalor"),
            Map.entry(-167373510, "Ashenclaw"),
            Map.entry(832610318, "Corvack"),
            Map.entry(-1254007688, "Omnipotent"),
            Map.entry(-708336010, "Prismara"),
            Map.entry(-1621744702, "Thalassar"),
            Map.entry(-422985676, "Golden Freddy"),
            Map.entry(290925398, "Chronos"),
            Map.entry(-342534076, "Kurvaros"),
            Map.entry(-1643392642, "Silex"),
            Map.entry(-1382454635, "Loa"),
            Map.entry(1824190226, "Solarflare"),
            Map.entry(2131893865, "Orion & Osiris"),
            Map.entry(-828991878, "Aetheris"),
            Map.entry(1420701227, "Malthar"),
            Map.entry(254038329, "Raphael"),
            Map.entry(-1253581965, "Voided Omnipotent"),
            Map.entry(-132746136, "Valerion"),
            Map.entry(-707883379, "Mithrion"),
            Map.entry(-829226362, "Nebula"),
            Map.entry(-132585649, "Ophanim"),
            Map.entry(-132915272, "True Ophan"),
            Map.entry(230903377, "Sylvaris"),
            Map.entry(-1622067598, "Asmodeus"),
            Map.entry(-1643406096, "Seraphim"),
            Map.entry(-1643245609, "True Seraph"),
            Map.entry(1301379752, "Unrest"),
            Map.entry(-1329808852, "Apostle"),
            Map.entry(-1329807891, "Hierophant"),
            Map.entry(-132588532, "Cherubim")
    );

    /** A phase change: the HP % where it happens and the wiki's name for the phase that starts. */
    record Phase(int percent, String label) {}

    private static Phase p(int percent, String label) {
        return new Phase(percent, label);
    }

    /** Shorthand for bosses whose phases are just numbered: phase 2 starts at the first %, and so on. */
    private static Phase[] numbered(int... percents) {
        Phase[] out = new Phase[percents.length];
        for (int i = 0; i < percents.length; i++) out[i] = new Phase(percents[i], "Phase " + (i + 2));
        return out;
    }

    /** Boss name -> where its phases change, highest HP first (from the Telos Realms wiki). */
    private static final Map<String, Phase[]> PHASES = Map.ofEntries(
            // Dungeon bosses
            Map.entry("Malfas", new Phase[] {p(66, "Phase 2: Minions"), p(33, "Phase 3: Sword")}),
            Map.entry("Heptavius", new Phase[] {p(66, "Phase 2: Minions"), p(33, "Phase 3: Ghastly Arms")}),
            Map.entry("Arctic Colossus", new Phase[] {p(50, "Phase 2: Icicles")}),
            Map.entry("Magnus", new Phase[] {p(66, "Phase 2: Effigies"), p(33, "Phase 3: Exalted")}),
            Map.entry("Pyro", new Phase[] {p(50, "Phase 2: Exalted")}),
            Map.entry("Thalor", new Phase[] {p(33, "Phase 2: Ice Cubes")}),
            Map.entry("Ashenclaw", new Phase[] {p(33, "Phase 2: Hooded")}),
            Map.entry("Corvack", new Phase[] {p(33, "Phase 2: Exalted")}),
            Map.entry("Omnipotent", new Phase[] {p(50, "Phase 2: Enraged")}),
            Map.entry("Prismara", new Phase[] {p(75, "Phase 2: Burrow"), p(35, "Phase 3: Segments")}),
            Map.entry("Thalassar", new Phase[] {p(35, "Phase 2: Sharks")}),
            Map.entry("Kurvaros", new Phase[] {p(33, "Phase 2: Rocks")}),
            Map.entry("Malthar", numbered(75)),
            Map.entry("Silex", new Phase[] {p(80, "Phase 2: Crinx"), p(55, "Phase 4: Tharms & Arahji"), p(30, "Phase 6: Teleports")}),
            Map.entry("Aetheris", new Phase[] {p(66, "Phase 2: Second Form"), p(33, "Phase 3: Final Form")}),
            Map.entry("Solarflare", new Phase[] {p(50, "Phase 2: Exalted")}),
            Map.entry("Raphael", new Phase[] {p(75, "Phase 2: Memorise"), p(50, "Phase 4: Bell"), p(15, "Phase 6: Desperation")}),
            Map.entry("Valerion", new Phase[] {p(50, "Phase 2: Faster"), p(25, "Phase 3: Swords")}),
            Map.entry("Nebula", new Phase[] {p(75, "Phase 1.5: Pillars"), p(40, "Phase 2.5: Pillars")}),
            Map.entry("Sylvaris", new Phase[] {p(75, "Phase 2: Shulker"), p(50, "Phase 3: Thorns"), p(25, "Phase 4: Arrows")}),
            Map.entry("Unrest", new Phase[] {p(75, "Phase 2"), p(50, "Phase 3: Dashes"), p(25, "Phase 4: Small Arena")}),
            Map.entry("Asmodeus", new Phase[] {p(75, "Phase 2: Tornado"), p(50, "Phase 3: Scythe"), p(20, "Phase 4: Quadrants")}),
            Map.entry("Seraphim", new Phase[] {p(80, "Phase 2: Flyovers"), p(50, "Phase 3: QR Code"), p(20, "Phase 5: Desperation")}),
            Map.entry("True Seraph", new Phase[] {p(80, "Phase 2: Flyovers"), p(50, "Phase 3: QR Code"), p(20, "Phase 5: Desperation")}),
            Map.entry("Ophanim", new Phase[] {p(85, "Phase 2"), p(60, "Phase 3: Clock"), p(40, "Phase 4"), p(15, "Phase 5: Grid")}),
            Map.entry("True Ophan", new Phase[] {p(85, "Phase 2"), p(60, "Phase 3: Clock"), p(40, "Phase 4"), p(15, "Phase 5: Grid")}),
            Map.entry("Cherubim", new Phase[] {p(60, "Phase 3: Cherubic Dimension"), p(20, "Phase 5: Final Verses")}),
            Map.entry("Voided Omnipotent", new Phase[] {p(60, "Phase 3: Chase"), p(30, "Phase 4: Bells"), p(15, "Phase 5: Not So Fast")}),
            Map.entry("Oozul", new Phase[] {p(50, "Phase 2: Slimes")}),
            // World bosses
            Map.entry("Chungus", new Phase[] {p(33, "Phase 2: Clones")}),
            Map.entry("Illarius", new Phase[] {p(33, "Phase 2: Battleaxe")}),
            Map.entry("Astaroth", new Phase[] {p(33, "Phase 2: Fissure")}),
            Map.entry("Glumi", new Phase[] {p(33, "Phase 2: Ponds")}),
            Map.entry("Lotil", new Phase[] {p(33, "Phase 2: Enraged")}),
            Map.entry("Tidol", new Phase[] {p(33, "Phase 2: Enraged")}),
            Map.entry("Valus", new Phase[] {p(33, "Phase 2: Djinns")}),
            Map.entry("Anubis", new Phase[] {p(33, "Phase 2: Sigil")}),
            Map.entry("Defender", new Phase[] {p(50, "Phase 2: Exalted")})
    );

    private static final Phase[] NONE = new Phase[0];

    static String nameOf(int barNameHash) {
        return NAMES.get(barNameHash);
    }

    /** Phase changes for a boss, or an empty array if it has none (or we don't know it). */
    static Phase[] phasesOf(String bossName) {
        if (bossName == null) return NONE;
        return PHASES.getOrDefault(bossName, NONE);
    }
}
