package com.nijatva.dialer;

import android.content.Context;
import java.util.HashSet;
import java.util.Set;

/* Local spam memory with categories, series/prefix patterns, operator codes and seeding */
public class SpamUtil {
    public static final String CAT_SPAMMER = "SCAMMER";
    public static final String CAT_FRAUD = "FINANCIAL FRAUD";
    public static final String CAT_TELE = "TELEMARKETER";
    public static final String CAT_ROBO = "ROBOCALL";

    public static String digits(String str) {
        if (str == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (char c : str.toCharArray()) {
            if (Character.isDigit(c)) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static boolean isSpam(Context context, String number) {
        return getCategory(context, number) != null;
    }

    /* Exact spam match: returns the saved category, null if not spam */
    public static String getCategory(Context context, String number) {
        String d = digits(number);
        if (d.isEmpty()) {
            return null;
        }
        Set<String> set = context.getSharedPreferences("mydialer", 0).getStringSet("spam_numbers", null);
        if (set == null || !set.contains(d)) {
            return null;
        }
        Set<String> cats = context.getSharedPreferences("mydialer", 0).getStringSet("spam_cats", null);
        if (cats != null) {
            for (String s : cats) {
                int i = s.indexOf('|');
                if (i > 0 && s.substring(0, i).equals(d)) {
                    return s.substring(i + 1);
                }
            }
        }
        return CAT_SPAMMER;
    }

    /* Operator short-code (exact) then series/prefix match: returns label, null if no match */
    public static String getSuspected(Context context, String number) {
        String d = digits(number);
        if (isSpam(context, number)) {
            return null;
        }
        Set<String> ops = context.getSharedPreferences("mydialer", 0).getStringSet("operator_codes", null);
        if (ops != null) {
            for (String s : ops) {
                int oi = s.indexOf('|');
                if (oi > 0 && s.substring(0, oi).equals(d)) {
                    return s.substring(oi + 1);
                }
            }
        }
        if (d.length() < 4) {
            return null;
        }
        Set<String> pats = context.getSharedPreferences("mydialer", 0).getStringSet("spam_patterns", null);
        if (pats == null) {
            return null;
        }
        String best = null;
        int bestLen = 0;
        for (String p : pats) {
            int i = p.indexOf('|');
            if (i <= 0) {
                continue;
            }
            String prefix = p.substring(0, i);
            if (prefix.length() >= 2 && d.startsWith(prefix) && prefix.length() > bestLen) {
                best = p.substring(i + 1);
                bestLen = prefix.length();
            }
        }
        return best;
    }

    public static void add(Context context, String number) {
        addWithCategory(context, number, CAT_SPAMMER);
    }

    public static void addWithCategory(Context context, String number, String cat) {
        String d = digits(number);
        if (d.isEmpty()) {
            return;
        }
        Set<String> old = context.getSharedPreferences("mydialer", 0).getStringSet("spam_numbers", null);
        HashSet<String> set = old == null ? new HashSet<>() : new HashSet<>(old);
        set.add(d);
        context.getSharedPreferences("mydialer", 0).edit().putStringSet("spam_numbers", set).commit();
        Set<String> oldCats = context.getSharedPreferences("mydialer", 0).getStringSet("spam_cats", null);
        HashSet<String> cs = oldCats == null ? new HashSet<>() : new HashSet<>(oldCats);
        for (String s : new HashSet<>(cs)) {
            if (s.startsWith(d + "|")) {
                cs.remove(s);
            }
        }
        cs.add(d + "|" + cat);
        context.getSharedPreferences("mydialer", 0).edit().putStringSet("spam_cats", cs).commit();
        if (context.getSharedPreferences("mydialer", 0).getBoolean("spam_to_contacts", false)) {
            saveAsContact(context, cat + " - " + number, number);
        }
    }

    public static void addPattern(Context context, String prefix, String cat) {
        String d = digits(prefix);
        if (d.length() < 2) {
            return;
        }
        Set<String> old = context.getSharedPreferences("mydialer", 0).getStringSet("spam_patterns", null);
        HashSet<String> ps = old == null ? new HashSet<>() : new HashSet<>(old);
        for (String s : new HashSet<>(ps)) {
            if (s.startsWith(d + "|")) {
                ps.remove(s);
            }
        }
        ps.add(d + "|" + cat);
        context.getSharedPreferences("mydialer", 0).edit().putStringSet("spam_patterns", ps).commit();
    }

    public static void remove(Context context, String number) {
        String d = digits(number);
        if (d.isEmpty()) {
            return;
        }
        Set<String> old = context.getSharedPreferences("mydialer", 0).getStringSet("spam_numbers", null);
        if (old == null) {
            return;
        }
        HashSet<String> set = new HashSet<>(old);
        set.remove(d);
        context.getSharedPreferences("mydialer", 0).edit().putStringSet("spam_numbers", set).commit();
        Set<String> oldCats = context.getSharedPreferences("mydialer", 0).getStringSet("spam_cats", null);
        if (oldCats != null) {
            HashSet<String> cs = new HashSet<>(oldCats);
            for (String s : new HashSet<>(cs)) {
                if (s.startsWith(d + "|")) {
                    cs.remove(s);
                }
            }
            context.getSharedPreferences("mydialer", 0).edit().putStringSet("spam_cats", cs).commit();
        }
    }

    /* Save a labelled number into the local Phone contacts (skips if it already exists) */
    public static void saveAsContact(Context context, String label, String number) {
        try {
            android.database.Cursor c = context.getContentResolver().query(android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI, new String[]{"contact_id"}, "data1=?", new String[]{number}, null);
            if (c != null) {
                boolean exists = c.moveToFirst();
                c.close();
                if (exists) {
                    return;
                }
            }
            java.util.ArrayList<android.content.ContentProviderOperation> ops = new java.util.ArrayList<>();
            ops.add(android.content.ContentProviderOperation.newInsert(android.provider.ContactsContract.RawContacts.CONTENT_URI)
                    .withValue("account_type", (String) null).withValue("account_name", (String) null).build());
            ops.add(android.content.ContentProviderOperation.newInsert(android.provider.ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference("raw_contact_id", 0)
                    .withValue("mimetype", "vnd.android.cursor.item/name")
                    .withValue("data1", label).build());
            ops.add(android.content.ContentProviderOperation.newInsert(android.provider.ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference("raw_contact_id", 0)
                    .withValue("mimetype", "vnd.android.cursor.item/phone_v2")
                    .withValue("data1", number)
                    .withValue("data2", Integer.valueOf(2)).build());
            context.getContentResolver().applyBatch(android.provider.ContactsContract.AUTHORITY, ops);
        } catch (Exception e) {
        }
    }

    /* One-time seeds: TRAI 140 telemarketer series + operator short codes */
    public static void seed(Context context) {
        try {
            if (!context.getSharedPreferences("mydialer", 0).getBoolean("spam_seed_done", false)) {
                addPattern(context, "140", CAT_TELE);
                context.getSharedPreferences("mydialer", 0).edit().putBoolean("spam_seed_done", true).commit();
            }
        } catch (Exception e) {
        }
        try {
            if (!context.getSharedPreferences("mydialer", 0).getBoolean("op_seed_done", false)) {
                HashSet<String> ops = new HashSet<>();
                ops.add("121|Airtel Care");
                ops.add("123|Operator Service");
                ops.add("198|Complaint Helpline");
                ops.add("199|Operator Care");
                ops.add("1977|Jio Care");
                ops.add("1503|BSNL Care");
                context.getSharedPreferences("mydialer", 0).edit().putStringSet("operator_codes", ops).commit();
                context.getSharedPreferences("mydialer", 0).edit().putBoolean("op_seed_done", true).commit();
            }
        } catch (Exception e2) {
        }
    }
}
