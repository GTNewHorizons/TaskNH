package com.eldrinn.tasknh.gui;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import net.minecraft.util.StatCollector;

/**
 * Uniform line breaker for lang files, matching the GT5-Unofficial standard so resource packs and translations can
 * break a tooltip into several lines.
 */
public class LangSplit {

    /** The line breaker, written as a literal backslash-n in lang files. */
    public static final String LB = "\\n";

    private LangSplit() {}

    /** Splits the translated text into lines. */
    public static List<String> splitLocalized(String key) {
        return Arrays.asList(
            StatCollector.translateToLocal(key)
                .split(Pattern.quote(LB)));
    }
}
