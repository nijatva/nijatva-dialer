package com.nijatva.dialer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

/* loaded from: classes.dex */
public class SplashActivity extends Activity {
    private String readCrash() {
        try {
            File file = new File(getFilesDir(), "crash.txt");
            if (!file.exists()) {
                return null;
            }
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            StringBuilder sb = new StringBuilder();
            while (true) {
                String readLine = bufferedReader.readLine();
                if (readLine == null || sb.length() > 1200) {
                    break;
                }
                sb.append(readLine);
                sb.append('\n');
            }
            bufferedReader.close();
            file.delete();
            return sb.length() == 0 ? null : sb.toString();
        } catch (Exception unused) {
            return null;
        }
    }


    public void saveCrash(Throwable th) {
        try {
            FileWriter fileWriter = new FileWriter(new File(getFilesDir(), "crash.txt"));
            fileWriter.write(th.getClass().getName() + ": " + th.getMessage() + "\n");
            StackTraceElement[] stackTrace = th.getStackTrace();
            for (int i = 0; i < stackTrace.length && i < 15; i++) {
                fileWriter.write("  at " + stackTrace[i].toString() + "\n");
            }
            fileWriter.close();
        } catch (Exception unused) {
        }
    }


    public void launchMain() {
        new Handler().postDelayed(new Runnable() {
            @Override // java.lang.Runnable
            public void run() {
                if (!SplashActivity.this.getSharedPreferences("mydialer", 0).getBoolean("onboarding_done", false)) {
                    SplashActivity.this.startActivity(new Intent(SplashActivity.this, (Class<?>) OnboardingActivity.class));
                } else {
                    SplashActivity.this.startActivity(new Intent(SplashActivity.this, (Class<?>) MainActivity.class));
                }
                SplashActivity.this.finish();
            }
        }, 1200L);
    }

    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_splash);
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override // java.lang.Thread.UncaughtExceptionHandler
            public void uncaughtException(Thread thread, Throwable th) {
                SplashActivity.this.saveCrash(th);
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(10);
            }
        });
        String readCrash = readCrash();
        if (readCrash != null) {
            TextView textView = new TextView(this);
            textView.setText(readCrash);
            textView.setTextSize(11.0f);
            textView.setTypeface(Typeface.MONOSPACE);
            ScrollView scrollView = new ScrollView(this);
            scrollView.setPadding(30, 20, 30, 20);
            scrollView.addView(textView);
            new AlertDialog.Builder(this).setTitle("Nijatva Dialer crash report").setView(scrollView).setCancelable(false).setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override // android.content.DialogInterface.OnClickListener
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                    SplashActivity.this.launchMain();
                }
            }).show();
            return;
        }
        launchMain();
    }
}
