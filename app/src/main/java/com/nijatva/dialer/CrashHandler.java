package com.nijatva.dialer;

import android.content.Context;
import android.content.Intent;
import android.os.Process;

/* Catches every uncaught crash, saves the stack trace and opens the M3 crash screen */
public class CrashHandler implements Thread.UncaughtExceptionHandler {
    private final Context ctx;
    private final Thread.UncaughtExceptionHandler original;

    public CrashHandler(Context context) {
        this.ctx = context.getApplicationContext();
        this.original = Thread.getDefaultUncaughtExceptionHandler();
    }

    @Override // java.lang.Thread.UncaughtExceptionHandler
    public void uncaughtException(Thread thread, Throwable throwable) {
        try {
            java.io.File f = new java.io.File(this.ctx.getFilesDir(), "last_crash.txt");
            java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter(f));
            pw.println("Thread: " + thread.getName());
            pw.println("App: Nijatva Dialer (build " + appBuild() + ")");
            pw.println();
            throwable.printStackTrace(pw);
            pw.close();
            Intent it = new Intent(this.ctx, CrashDialogActivity.class);
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            this.ctx.startActivity(it);
            Thread.sleep(250L);
        } catch (Throwable t) {
            try {
                if (this.original != null) {
                    this.original.uncaughtException(thread, throwable);
                    return;
                }
            } catch (Throwable t2) {
            }
        }
        Process.killProcess(Process.myPid());
        System.exit(10);
    }

    private String appBuild() {
        try {
            return String.valueOf(this.ctx.getPackageManager().getPackageInfo(this.ctx.getPackageName(), 0).versionCode);
        } catch (Exception e) {
            return "?";
        }
    }
}
