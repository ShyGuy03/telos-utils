package com.telosutils;

import java.util.OptionalDouble;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Turns a floating-text string like "1,234", "§c-532" or "12.5k!" (already decoded from the Telos font) into a number. */
final class DamageParser {
    private static final Pattern FORMAT_CODES = Pattern.compile("§.");
    // The text must be essentially just a number: optional symbols, digits, optional k/m suffix, optional symbols.
    private static final Pattern NUMBER =
            Pattern.compile("^[^0-9A-Za-z]*([0-9][0-9,]*(?:\\.[0-9]+)?)\\s*([kKmM]?)[^0-9A-Za-z]*$");

    private DamageParser() {}

    static OptionalDouble parse(String raw) {
        if (raw == null) return OptionalDouble.empty();
        String s = FORMAT_CODES.matcher(raw).replaceAll("").trim();
        if (s.isEmpty() || s.length() > 24) return OptionalDouble.empty();
        if (s.contains("+")) return OptionalDouble.empty(); // "+50" is usually healing, not damage

        Matcher m = NUMBER.matcher(s);
        if (!m.matches()) return OptionalDouble.empty();

        double value;
        try {
            value = Double.parseDouble(m.group(1).replace(",", ""));
        } catch (NumberFormatException e) {
            return OptionalDouble.empty();
        }
        String suffix = m.group(2).toLowerCase();
        if (suffix.equals("k")) value *= 1_000;
        if (suffix.equals("m")) value *= 1_000_000;

        return value > 0 ? OptionalDouble.of(value) : OptionalDouble.empty();
    }
}
