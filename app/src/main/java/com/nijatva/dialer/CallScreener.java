package com.nijatva.dialer;

import android.content.SharedPreferences;
import android.telecom.Call;
import android.telecom.CallScreeningService;
import java.util.HashSet;
import java.util.Set;

/* loaded from: classes.dex */
public class CallScreener extends CallScreeningService {
    @Override // android.telecom.CallScreeningService
    public void onScreenCall(Call.Details details) {
        try {
            String schemeSpecificPart = details.getHandle() != null ? details.getHandle().getSchemeSpecificPart() : "";
            StringBuilder sb = new StringBuilder();
            for (char c : schemeSpecificPart.toCharArray()) {
                if (Character.isDigit(c)) {
                    sb.append(c);
                }
            }
            SharedPreferences sharedPreferences = getSharedPreferences("mydialer", 0);
            Set<String> stringSet = sharedPreferences.getStringSet("blocked", null);
            if (stringSet == null) {
                stringSet = new HashSet<>();
            }
            boolean z = sb.length() > 0 && stringSet.contains(sb.toString());
            if (!z && sb.length() >= 3 && sharedPreferences.getBoolean("block_140", false) && sb.charAt(0) == '1' && sb.charAt(1) == '4' && sb.charAt(2) == '0') {
                z = true;
            }
            if (!z && sharedPreferences.getBoolean("block_scammers", true) && sb.length() > 0) {
                Set<String> spamSet = sharedPreferences.getStringSet("spam_numbers", null);
                if (spamSet != null && spamSet.contains(sb.toString())) {
                    z = true;
                }
            }
            if (!z && sharedPreferences.getBoolean("block_promo", false) && sb.length() > 0) {
                Set<String> ops = sharedPreferences.getStringSet("operator_codes", null);
                if (ops != null) {
                    for (String s : ops) {
                        int oi = s.indexOf('|');
                        if (oi > 0 && s.substring(0, oi).equals(sb.toString())) {
                            z = true;
                            break;
                        }
                    }
                }
                if (!z && sb.length() >= 4) {
                    Set<String> pats = sharedPreferences.getStringSet("spam_patterns", null);
                    if (pats != null) {
                        for (String p : pats) {
                            int pi = p.indexOf('|');
                            if (pi >= 2 && sb.toString().startsWith(p.substring(0, pi))) {
                                z = true;
                                break;
                            }
                        }
                    }
                }
            }
            if (!z && sb.length() == 0 && sharedPreferences.getBoolean("reject_hidden", false)) {
                z = true;
            }
            if (z) {
                respondToCall(details, new CallScreeningService.CallResponse.Builder().setDisallowCall(true).setRejectCall(true).setSkipCallLog(false).setSkipNotification(true).build());
            } else {
                respondToCall(details, new CallScreeningService.CallResponse.Builder().build());
            }
        } catch (Exception unused) {
        }
    }
}
