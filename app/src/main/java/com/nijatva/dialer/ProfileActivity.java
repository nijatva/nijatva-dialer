package com.nijatva.dialer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Outline;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Locale;

/* loaded from: classes.dex */
public class ProfileActivity extends Activity {
    private TextView avatar;
    private boolean hideOn = false;
    private String realPhone = "";
    private SharedPreferences prefs;
    private int genderSel = 0;

    private String pref(String key) {
        return this.prefs.getString(key, "");
    }

    private void saveAll(boolean close) {
        SharedPreferences.Editor edit = this.prefs.edit();
        edit.putString("profile_name", ((EditText) findViewById(R.id.etProfileName)).getText().toString().trim());
        String phoneText = ((EditText) findViewById(R.id.etProfilePhone)).getText().toString().trim();
        if (this.hideOn && phoneText.equals(maskNumber(this.realPhone))) {
            phoneText = this.realPhone;
        }
        edit.putString("profile_number", phoneText);
        edit.putString("profile_email", ((EditText) findViewById(R.id.etProfileEmail)).getText().toString().trim());
        edit.putString("profile_birthdate", ((EditText) findViewById(R.id.etBirthdate)).getText().toString().trim());
        edit.putString("profile_location", ((EditText) findViewById(R.id.etLocation)).getText().toString().trim());
        edit.putString("profile_bio", ((EditText) findViewById(R.id.etBio)).getText().toString().trim());
        edit.putString("profile_google", ((EditText) findViewById(R.id.etGoogle)).getText().toString().trim());
        edit.putString("profile_social1", ((EditText) findViewById(R.id.etSocial1)).getText().toString().trim());
        edit.putString("profile_facebook", ((EditText) findViewById(R.id.etFacebook)).getText().toString().trim());
        edit.putString("profile_social2", ((EditText) findViewById(R.id.etSocial2)).getText().toString().trim());
        edit.putString("profile_language", ((TextView) findViewById(R.id.tvLanguage)).getText().toString().trim());
        edit.putInt("profile_gender", this.genderSel);
        edit.commit();
        Toast.makeText(this, R.string.profile_saved, 0).show();
        setResult(-1);
        if (close) {
            finish();
        }
    }

    private void loadFields() {
        ((EditText) findViewById(R.id.etProfileName)).setText(pref("profile_name"));
        this.realPhone = pref("profile_number");
        this.hideOn = this.prefs.getBoolean("hide_number", false);
        ((EditText) findViewById(R.id.etProfilePhone)).setText(this.hideOn ? maskNumber(this.realPhone) : this.realPhone);
        ((EditText) findViewById(R.id.etProfileEmail)).setText(pref("profile_email"));
        ((EditText) findViewById(R.id.etBirthdate)).setText(pref("profile_birthdate"));
        ((EditText) findViewById(R.id.etLocation)).setText(pref("profile_location"));
        ((EditText) findViewById(R.id.etBio)).setText(pref("profile_bio"));
        ((EditText) findViewById(R.id.etGoogle)).setText(pref("profile_google"));
        ((EditText) findViewById(R.id.etSocial1)).setText(pref("profile_social1"));
        ((EditText) findViewById(R.id.etFacebook)).setText(pref("profile_facebook"));
        ((EditText) findViewById(R.id.etSocial2)).setText(pref("profile_social2"));
        String lang = pref("profile_language");
        if (lang != null && !lang.trim().isEmpty()) {
            ((TextView) findViewById(R.id.tvLanguage)).setText(lang);
        }
        this.genderSel = this.prefs.getInt("profile_gender", 0);
        tintGenderPills();
        tintAvatarRings();
        showAvatar();
    }

    private String maskNumber(String str) {
        if (str == null) {
            return "";
        }
        String trim = str.trim();
        if (trim.length() <= 5) {
            return "*****";
        }
        return "***** " + trim.substring(trim.length() - 5);
    }

    private void showAvatar() {
        Bitmap bitmap = null;
        try {
            File file = new File(getFilesDir(), "profile.jpg");
            if (file.exists()) {
                bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            }
        } catch (Exception unused) {
        }
        if (bitmap != null) {
            this.avatar.setText("");
            this.avatar.setBackground(new BitmapDrawable(getResources(), bitmap));
            try {
                this.avatar.setOutlineProvider(new ViewOutlineProvider() {
                    @Override // android.view.ViewOutlineProvider
                    public void getOutline(View view, Outline outline) {
                        outline.setOval(0, 0, view.getWidth(), view.getHeight());
                    }
                });
                this.avatar.setClipToOutline(true);
                return;
            } catch (Exception unused2) {
                return;
            }
        }
        this.avatar.setClipToOutline(false);
        String av = this.prefs.getString("profile_avatar", "0");
        this.avatar.setText("");
        if ("1".equals(av)) {
            this.avatar.setBackgroundResource(R.drawable.ic_avatar_male);
        } else if ("2".equals(av)) {
            this.avatar.setBackgroundResource(R.drawable.ic_avatar_female);
        } else if ("3".equals(av)) {
            this.avatar.setBackgroundResource(R.drawable.bg_avatar_placeholder);
        } else {
            GradientDrawable gradientDrawable = new GradientDrawable();
            gradientDrawable.setShape(1);
            gradientDrawable.setColor(-3129167);
            this.avatar.setBackground(gradientDrawable);
            String string = this.prefs.getString("profile_name", "");
            if (string == null || string.trim().isEmpty()) {
                this.avatar.setText("");
                this.avatar.setBackgroundResource(R.drawable.bg_avatar_placeholder);
            } else {
                this.avatar.setText(string.trim().substring(0, 1).toUpperCase(Locale.US));
            }
        }
    }

    private void tintGenderPills() {
        int[] ids = {R.id.pillMale, R.id.pillFemale, R.id.pillOther};
        for (int i = 0; i < 3; i++) {
            TextView textView = (TextView) findViewById(ids[i]);
            boolean active = this.genderSel == (i + 1);
            textView.setBackgroundResource(active ? R.drawable.bg_chip_active : R.drawable.bg_chip_inactive);
            textView.setTextColor(active ? -1 : getResources().getColor(R.color.text_hint));
        }
    }

    private void tintAvatarRings() {
        String av = this.prefs.getString("profile_avatar", "0");
        Object[][] sets = {{Integer.valueOf(R.id.optMale), "1"}, {Integer.valueOf(R.id.optFemale), "2"}, {Integer.valueOf(R.id.optNeutral), "3"}};
        for (Object[] set : sets) {
            findViewById(((Integer) set[0]).intValue()).setBackgroundResource(set[1].equals(av) ? R.drawable.bg_avatar_ring_green : 0);
        }
    }

    private void pickAvatar(String code) {
        this.prefs.edit().putString("profile_avatar", code).commit();
        try {
            new File(getFilesDir(), "profile.jpg").delete();
        } catch (Exception unused) {
        }
        tintAvatarRings();
        showAvatar();
    }

    private void showBirthdatePicker() {
        Calendar calendar = Calendar.getInstance();
        String current = ((EditText) findViewById(R.id.etBirthdate)).getText().toString().trim();
        if (current.length() == 10) {
            try {
                calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(current.substring(0, 2)));
                calendar.set(Calendar.MONTH, Integer.parseInt(current.substring(3, 5)) - 1);
                calendar.set(Calendar.YEAR, Integer.parseInt(current.substring(6, 10)));
            } catch (Exception unused) {
            }
        }
        new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override // android.app.DatePickerDialog.OnDateSetListener
            public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                ProfileActivity.this.findViewById(R.id.etBirthdate);
                ((EditText) ProfileActivity.this.findViewById(R.id.etBirthdate)).setText(String.format(Locale.US, "%02d/%02d/%04d", Integer.valueOf(day), Integer.valueOf(month + 1), Integer.valueOf(year)));
            }
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showLanguagePicker() {
        final String[] langs = {"English", "Hindi", "Bengali", "Tamil", "Telugu", "Marathi", "Gujarati", "Other"};
        String current = ((TextView) findViewById(R.id.tvLanguage)).getText().toString().trim();
        int checked = -1;
        for (int i = 0; i < langs.length; i++) {
            if (langs[i].equals(current)) {
                checked = i;
            }
        }
        final TextView target = (TextView) findViewById(R.id.tvLanguage);
        new AlertDialog.Builder(this).setTitle(R.string.language_pref).setSingleChoiceItems(langs, checked, new DialogInterface.OnClickListener() {
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialogInterface, int i) {
                target.setText(langs[i]);
                dialogInterface.dismiss();
            }
        }).setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener) null).show();
    }

    @Override // android.app.Activity
    protected void onActivityResult(int i, int i2, Intent intent) {
        super.onActivityResult(i, i2, intent);
        if (i != 300 || i2 != -1 || intent == null || intent.getData() == null) {
            return;
        }
        try {
            InputStream openInputStream = getContentResolver().openInputStream(intent.getData());
            FileOutputStream fileOutputStream = new FileOutputStream(new File(getFilesDir(), "profile.jpg"));
            byte[] bArr = new byte[8192];
            while (true) {
                int read = openInputStream.read(bArr);
                if (read <= 0) {
                    fileOutputStream.close();
                    openInputStream.close();
                    showAvatar();
                    return;
                }
                fileOutputStream.write(bArr, 0, read);
            }
        } catch (Exception unused) {
            Toast.makeText(this, R.string.bg_failed, 0).show();
        }
    }

    @Override // android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_profile);
        this.prefs = getSharedPreferences("mydialer", 0);
        findViewById(R.id.profileBack).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                ProfileActivity.this.finish();
            }
        });
        this.avatar = (TextView) findViewById(R.id.profileAvatar);
        loadFields();
        this.avatar.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                try {
                    ProfileActivity.this.startActivityForResult(new Intent("android.intent.action.PICK", MediaStore.Images.Media.EXTERNAL_CONTENT_URI), 300);
                } catch (Exception unused) {
                    Toast.makeText(ProfileActivity.this, R.string.bg_failed, 0).show();
                }
            }
        });
        findViewById(R.id.optMale).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                ProfileActivity.this.pickAvatar("1");
            }
        });
        findViewById(R.id.optFemale).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                ProfileActivity.this.pickAvatar("2");
            }
        });
        findViewById(R.id.optNeutral).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                ProfileActivity.this.pickAvatar("3");
            }
        });
        int[] pills = {R.id.pillMale, R.id.pillFemale, R.id.pillOther};
        for (int i = 0; i < 3; i++) {
            final int idx = i + 1;
            findViewById(pills[i]).setOnClickListener(new View.OnClickListener() {
                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    ProfileActivity.this.genderSel = (ProfileActivity.this.genderSel == idx) ? 0 : idx;
                    ProfileActivity.this.tintGenderPills();
                }
            });
        }
        findViewById(R.id.etBirthdate).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                ProfileActivity.this.showBirthdatePicker();
            }
        });
        findViewById(R.id.tvLanguage).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                ProfileActivity.this.showLanguagePicker();
            }
        });
        findViewById(R.id.btnSaveProfile).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                ProfileActivity.this.saveAll(true);
            }
        });
        findViewById(R.id.btnSaveTop).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                ProfileActivity.this.saveAll(true);
            }
        });
        findViewById(R.id.btnCancelProfile).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                ProfileActivity.this.loadFields();
                Toast.makeText(ProfileActivity.this, R.string.cancel_changes, 0).show();
            }
        });
        findViewById(R.id.profileSettingsBtn).setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                try {
                    ProfileActivity.this.startActivity(new Intent(ProfileActivity.this, (Class<?>) SettingsActivity.class));
                } catch (Throwable unused) {
                }
            }
        });
    }
    private int themeGen;

    @Override // android.app.ContextThemeWrapper
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
