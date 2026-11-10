package com.telosutils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Telos draws damage numbers with its own resource-pack font, where every digit is a private-use
 * character. This reads that font's definition from the loaded resource pack and turns those
 * characters back into plain text, e.g. [start]2[space]2[space].[space]3[end] -> "22.3".
 */
final class TelosFont {
    static final Identifier TEXT_FONT = Identifier.fromNamespaceAndPath("telos", "text");
    private static final Identifier TEXT_FONT_JSON = Identifier.fromNamespaceAndPath("telos", "font/text.json");

    /** Result of decoding: the readable text, and whether any of it was drawn in the Telos font. */
    record Decoded(String text, boolean usedTelosFont) {}

    /** Codepoint -> glyph name ("0".."9", "a".."z", "dot", "start", ...). Built lazily from the resource pack. */
    private static Map<Integer, String> glyphs;

    private TelosFont() {}

    /** Forget the loaded font so it is re-read; call when the server's resource pack may have changed. */
    static void invalidate() {
        glyphs = null;
    }

    static Decoded decode(Component text) {
        Map<Integer, String> map = glyphs();
        StringBuilder out = new StringBuilder();
        boolean[] usedTelos = {false};

        text.visit((style, part) -> {
            boolean telos = style.getFont() instanceof FontDescription.Resource r && r.id().equals(TEXT_FONT);
            if (!telos) {
                // Characters from other Telos fonts (e.g. telos:spaces) are layout tweaks, not text.
                if (!(style.getFont() instanceof FontDescription.Resource r && r.id().getNamespace().equals("telos"))) {
                    out.append(part);
                }
                return Optional.empty();
            }
            usedTelos[0] = true;
            part.codePoints().forEach(cp -> out.append(glyphToText(map.get(cp))));
            return Optional.empty();
        }, Style.EMPTY);

        return new Decoded(out.toString(), usedTelos[0]);
    }

    private static String glyphToText(String glyph) {
        if (glyph == null) return "";
        return switch (glyph) {
            case "dot" -> ".";
            case "dash" -> "-";
            case "slash" -> "/";
            case "space" -> " ";
            default -> glyph.length() == 1 ? glyph : ""; // start/end/frame/inner etc. are decoration
        };
    }

    private static Map<Integer, String> glyphs() {
        if (glyphs == null) glyphs = load();
        return glyphs;
    }

    private static Map<Integer, String> load() {
        Map<Integer, String> map = new HashMap<>();
        // Use every pack that defines the font, lowest priority first, so the server pack wins.
        for (Resource res : Minecraft.getInstance().getResourceManager().getResourceStack(TEXT_FONT_JSON)) {
            try (Reader reader = res.openAsReader()) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                for (JsonElement el : root.getAsJsonArray("providers")) {
                    JsonObject provider = el.getAsJsonObject();
                    if (!"bitmap".equals(provider.get("type").getAsString()) || !provider.has("file")) continue;

                    // Telos uses one image per glyph, so the file name tells us which character it is.
                    StringBuilder chars = new StringBuilder();
                    for (JsonElement row : provider.getAsJsonArray("chars")) chars.append(row.getAsString());
                    int[] cps = chars.codePoints().filter(cp -> cp != 0).toArray();
                    if (cps.length != 1) continue;

                    String file = provider.get("file").getAsString();
                    String name = file.substring(file.lastIndexOf('/') + 1).replace(".png", "");
                    map.put(cps[0], name);
                }
            } catch (Exception e) {
                System.out.println("[TelosUtils] Could not read Telos font from " + res.sourcePackId() + ": " + e);
            }
        }
        System.out.println("[TelosUtils] Loaded " + map.size() + " Telos font glyphs");
        return map;
    }
}
