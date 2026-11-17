package com.nijatva.dialer;

import android.content.Context;
import android.app.Activity;
import android.app.role.RoleManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.telecom.TelecomManager;
import android.view.View;

public class OnboardingActivity extends Activity {
    private void openMain() {
        getSharedPreferences("mydialer", 0).edit().putBoolean("onboarding_done", true).commit();
        startActivity(new Intent(this, (Class<?>) MainActivity.class));
        finish();
    }

    private void requestDefault() {
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                RoleManager roleManager = (RoleManager) getSystemService(RoleManager.class);
                if (roleManager != null && roleManager.isRoleAvailable("android.app.role.DIALER")) {
                    startActivityForResult(roleManager.createRequestRoleIntent("android.app.role.DIALER"), 201);
                    return;
                }
            }
            Intent intent = new Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER);
            intent.putExtra("android.telecom.extra.CHANGE_DEFAULT_DIALER_PACKAGE_NAME", getPackageName());
            startActivityForResult(intent, 202);
        } catch (Exception unused) {
            openMain();
        }
    }

    @Override // android.app.Activity
    protected void onActivityResult(int i, int i2, Intent intent) {
        super.onActivityResult(i, i2, intent);
        openMain();
    }

    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_onboarding);
        findViewById(R.id.onbSetDefault).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                OnboardingActivity.this.requestDefault();
            }
        });
        findViewById(R.id.onbClose).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                OnboardingActivity.this.finishAffinity();
            }
        });
        findViewById(R.id.onbCancel).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                OnboardingActivity.this.openMain();
            }
        });
    }
    private int themeGen;

    @Override // android.view.ContextThemeWrapper
    protected void attachBaseContext(Context context) {
        super.attachBaseContext(ThemeUtil.wrap(context));
        this.themeGen = ThemeUtil.generation;
    }

    @Override // android.app.Activity
    protected void onResume() {
        super.onResume();
        if (this.themeGen != ThemeUtil.generation) {
            recreate();
        }
    }

}
