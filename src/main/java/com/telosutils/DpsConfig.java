package com.telosutils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Settings, changed from the menu or /dps commands and saved to config/telosutils.json. */
public class DpsConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("telosutils.json");
    /** Where settings were saved before the mod was renamed; read once if the new file doesn't exist yet. */
    private static final Path OLD_FILE = FabricLoader.getInstance().getConfigDir().resolve("telosdps.json");

    // Display
    public boolean hudEnabled = true;
    public boolean showPeak = true;
    public boolean showAvg = true;
    public boolean showTotal = true;
    public boolean showTime = true;
    public boolean showBossHp = true;
    public boolean chatSummary = true;
    public float hudScale = 1.0f;
    /** HUD background opacity, 0-100. */
    public int bgOpacity = 80;
    public int hudX = 4;
    public int hudY = 4;

    // Boss HP display
    public int bossX = 4;
    public int bossY = 50;
    public float bossScale = 1.0f;

    // Phase warnings
    public boolean phaseWarnings = true;
    /** Warn when the boss is within this many percent above a phase change. */
    public float warnWithin = 5.0f;
    public boolean phaseSound = true;
    /** -1 means centred horizontally. */
    public int warnX = -1;
    public int warnY = 60;
    public float warnScale = 1.0f;

    // Tracking
    /** Only damage numbers within this many blocks of you are counted. */
    public int range = 16;
    /** Rolling window used for the live DPS number. */
    public int windowSeconds = 5;
    /** A fight ends after this long with no new damage numbers. */
    public int fightTimeoutSeconds = 5;
    public boolean debug = false;

    // Menu
    public int menuX = -1;
    public int menuY = -1;

    public static DpsConfig load() {
        Path source = Files.exists(FILE) ? FILE : OLD_FILE;
        if (Files.exists(source)) {
            try (Reader reader = Files.newBufferedReader(source)) {
                DpsConfig cfg = GSON.fromJson(reader, DpsConfig.class);
                if (cfg != null) return cfg;
            } catch (Exception e) {
                System.out.println("[TelosUtils] Could not read " + source + ", using defaults: " + e);
            }
        }
        return new DpsConfig();
    }

    public void save() {
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(FILE)) {
                GSON.toJson(this, writer);
            }
        } catch (Exception e) {
            System.out.println("[TelosUtils] Could not save " + FILE + ": " + e);
        }
    }
}
