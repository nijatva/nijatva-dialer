package com.nijatva.dialer;

import android.telecom.Call;
import java.util.ArrayList;

/* loaded from: classes.dex */
public class CallState {
    static final ArrayList<Call> calls = new ArrayList<>();
    static boolean justEnded = false;
    static String lastCallNumber = null;
    static boolean lastCallMissed = false;


    public static void addCall(Call call) {
        ArrayList<Call> arrayList = calls;
        if (arrayList.contains(call)) {
            return;
        }
        arrayList.add(call);
    }


    public static Call primary() {
        ArrayList<Call> arrayList = calls;
        if (arrayList.isEmpty()) {
            return null;
        }
        return arrayList.get(0);
    }


    public static void removeCall(Call call) {
        calls.remove(call);
    }
}
