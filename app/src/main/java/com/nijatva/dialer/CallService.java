package com.nijatva.dialer;

import android.content.Intent;
import android.telecom.Call;
import android.telecom.InCallService;

/* loaded from: classes.dex */
public class CallService extends InCallService {
    @Override // android.telecom.InCallService
    public void onCallAdded(Call call) {
        super.onCallAdded(call);
        CallState.addCall(call);
        Intent intent = new Intent(this, (Class<?>) InCallActivity.class);
        intent.addFlags(805306368);
        startActivity(intent);
        try {
            if (call.getState() == 1 && getSharedPreferences("mydialer", 0).getBoolean("announce_caller", false)) {
                String number = call.getDetails().getHandle() != null ? call.getDetails().getHandle().getSchemeSpecificPart() : "";
                if (number != null && !number.trim().isEmpty()) {
                    announceCaller(number);
                }
            }
        } catch (Exception unused) {
        }
    }

    private void announceCaller(String number) {
        String display = number;
        try {
            android.database.Cursor c = getContentResolver().query(android.net.Uri.withAppendedPath(
                    android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI, android.net.Uri.encode(number)),
                    new String[]{"display_name"}, null, null, null);
            if (c != null) {
                if (c.moveToFirst()) {
                    display = c.getString(0);
                }
                c.close();
            }
        } catch (Exception unused) {
        }
        final String spoken = "Incoming call from " + display;
        try {
            final android.speech.tts.TextToSpeech[] holder = new android.speech.tts.TextToSpeech[1];
            holder[0] = new android.speech.tts.TextToSpeech(this, new android.speech.tts.TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if (status == 0) {
                        try {
                            holder[0].speak(spoken, 1, null, "caller");
                        } catch (Exception unused) {
                        }
                    }
                }
            });
        } catch (Exception unused) {
        }
    }

    @Override // android.telecom.InCallService
    public void onCallRemoved(Call call) {
        super.onCallRemoved(call);
        CallState.removeCall(call);
        InCallActivity.finishIfNoCalls();
    }
}
