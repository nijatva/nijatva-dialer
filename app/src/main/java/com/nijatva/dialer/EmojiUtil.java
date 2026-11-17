package com.nijatva.dialer;

import android.content.Context;
import java.util.HashSet;
import java.util.Set;

/* Custom friend emojis: 7 preset choices, auto emojis for favourites/blocked */
public class EmojiUtil {
    public static final String[] PRESETS = {"\u2764\uFE0F", "\uD83D\uDE0A", "\uD83D\uDE02", "\uD83D\uDD25", "\u2B50", "\uD83D\uDCBC", "\uD83D\uDEAB"};
    public static final String FAV = "\u2764\uFE0F";
    public static final String BLOCKED = "\uD83D\uDE21";

    private static Set<String> raw(Context ctx) {
        try {
            Set<String> set = ctx.getSharedPreferences("mydialer", 0).getStringSet("contact_emojis", null);
            return set != null ? set : new HashSet<String>();
        } catch (Exception unused) {
            return new HashSet<String>();
        }
    }

    public static String get(Context ctx, String number) {
        if (number == null) {
            return "";
        }
        String prefix = number + "|";
        for (String e : raw(ctx)) {
            if (e != null && e.startsWith(prefix)) {
                return e.substring(prefix.length());
            }
        }
        return "";
    }

    public static void set(Context ctx, String number, String emoji) {
        if (number == null) {
            return;
        }
        String prefix = number + "|";
        Set<String> out = new HashSet<>();
        for (String e : raw(ctx)) {
            if (e != null && !e.startsWith(prefix)) {
                out.add(e);
            }
        }
        if (emoji != null && !emoji.isEmpty()) {
            out.add(prefix + emoji);
        }
        ctx.getSharedPreferences("mydialer", 0).edit().putStringSet("contact_emojis", out).commit();
    }

    /* Emoji to display: blocked wins (auto), then manual choice */
    public static String display(Context ctx, String number) {
        if (number == null) {
            return "";
        }
        try {
            Set<String> blocked = ctx.getSharedPreferences("mydialer", 0).getStringSet("blocked", null);
            if (blocked != null && blocked.contains(number)) {
                return BLOCKED;
            }
        } catch (Exception unused) {
        }
        return get(ctx, number);
    }
}
