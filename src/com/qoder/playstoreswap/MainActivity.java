package com.qoder.playstoreswap;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.ScrollingMovementMethod;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MainActivity extends Activity {

    private static final int DISABLED_TEXT = 0x66FFFFFF;
    private static final int RIPPLE_LIGHT = 0x1A000000;
    private static final int RIPPLE_ON_PRIMARY = 0x40FFFFFF;
    private static final int UNCHECKED_CONTROL = 0xFF9AA5A1;

    private final Handler ui = new Handler();
    private final StringBuilder logText = new StringBuilder();

    private TextView logView;
    private Button goButton;
    private RadioGroup versionGroup;
    private RadioGroup methodGroup;
    private String selectedVersion = "5.12.10";
    private boolean rootMethod = true;

    private int colorPrimary;
    private int colorText;
    private int colorSecondary;
    private int colorSelected;
    private int colorSurface;
    private int colorLogBg;
    private int colorLogText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        colorPrimary = getResources().getColor(R.color.primary);
        colorText = getResources().getColor(R.color.text_primary);
        colorSecondary = getResources().getColor(R.color.text_secondary);
        colorSelected = getResources().getColor(R.color.selected_row);
        colorSurface = getResources().getColor(R.color.surface);
        colorLogBg = getResources().getColor(R.color.log_bg);
        colorLogText = getResources().getColor(R.color.log_text);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(getResources().getColor(R.color.window_bg));

        root.addView(buildAppBar(),
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(64)));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(4));
        content.addView(buildMethodCard());
        content.addView(buildVersionCard());
        content.addView(buildDeviceCard());

        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(true);
        scroll.setClipToPadding(false);
        scroll.addView(content, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));
        scroll.setPadding(0, 0, 0, dp(4));
        root.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 2f));

        goButton = buildPrimaryButton();
        goButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                begin();
            }
        });
        LinearLayout actions = new LinearLayout(this);
        actions.setPadding(dp(16), dp(8), dp(16), dp(4));
        actions.addView(goButton, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        root.addView(actions, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout output = new LinearLayout(this);
        output.setOrientation(LinearLayout.VERTICAL);
        output.setPadding(dp(16), dp(8), dp(16), dp(16));
        output.addView(overline("Output"));
        logView = new TextView(this);
        logView.setTextSize(12);
        logView.setTextColor(colorLogText);
        logView.setTypeface(Typeface.MONOSPACE);
        logView.setPadding(dp(12), dp(10), dp(12), dp(10));
        GradientDrawable logBg = new GradientDrawable();
        logBg.setColor(colorLogBg);
        logBg.setCornerRadius(dp(12));
        logView.setBackground(logBg);
        logView.setMovementMethod(new ScrollingMovementMethod());
        output.addView(logView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        root.addView(output, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
        append("Root method is selected. Tap the button to continue.\n");
    }

    private LinearLayout buildAppBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackgroundColor(colorPrimary);
        bar.setElevation(dp(4));
        bar.setPadding(dp(20), 0, dp(20), 0);

        ImageView mark = new ImageView(this);
        mark.setImageResource(R.drawable.ic_launcher_foreground);
        mark.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams markLp = new LinearLayout.LayoutParams(dp(44), dp(44));
        markLp.rightMargin = dp(10);
        bar.addView(mark, markLp);

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);

        TextView title = new TextView(this);
        title.setText("LyanoPatchInstaller");
        title.setTextSize(19);
        title.setTextColor(0xFFFFFFFF);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        titles.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("V2.8  \u00b7  PLAY STORE SWAPPER");
        subtitle.setTextSize(11);
        subtitle.setTextColor(0xFFB7E4D0);
        subtitle.setLetterSpacing(0.12f);
        subtitle.setPadding(0, dp(1), 0, 0);
        titles.addView(subtitle);

        bar.addView(titles, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return bar;
    }

    private LinearLayout buildMethodCard() {
        LinearLayout card = card();
        card.addView(overline("Install method"));

        methodGroup = new RadioGroup(this);
        methodGroup.setOrientation(RadioGroup.VERTICAL);
        String[] titles = {"Root install", "No-root install"};
        String[] details = {
                "Replaces the system Play Store, then reboots.",
                "Adds a second store next to the existing one."
        };
        for (int i = 0; i < titles.length; i++) {
            RadioButton rb = option(100 + i, titles[i], details[i]);
            methodGroup.addView(rb, rowParams());
        }
        ((RadioButton) methodGroup.getChildAt(0)).setChecked(true);
        methodGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                rootMethod = checkedId == 100;
                goButton.setText(rootMethod ? "Replace Play Store & Reboot" : "Install Second Play Store");
                append("Method: " + (rootMethod
                        ? "Root - replace system Play Store"
                        : "No root - second store") + "\n");
            }
        });
        card.addView(methodGroup);
        return card;
    }

    private LinearLayout buildVersionCard() {
        LinearLayout card = card();
        card.addView(overline("Play Store build"));

        versionGroup = new RadioGroup(this);
        versionGroup.setOrientation(RadioGroup.VERTICAL);
        final String[] versions = {"5.12.10", "4.4.22", "3.4.7"};
        final String[] versionInfo = {
                "Minimum Android 2.3  \u00b7  2015  \u00b7  newest",
                "Minimum Android 2.2  \u00b7  2013  \u00b7  older",
                "Minimum Android 2.2  \u00b7  2012  \u00b7  oldest"
        };
        for (int i = 0; i < versions.length; i++) {
            RadioButton rb = option(i + 1, versions[i], versionInfo[i]);
            versionGroup.addView(rb, rowParams());
        }
        ((RadioButton) versionGroup.getChildAt(0)).setChecked(true);
        versionGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                selectedVersion = versions[checkedId - 1];
                append("Selected build: Play Store " + selectedVersion + "\n");
            }
        });
        card.addView(versionGroup);
        return card;
    }

    private LinearLayout buildDeviceCard() {
        LinearLayout card = card();
        card.addView(overline("This device"));

        TextView device = new TextView(this);
        device.setText("Android " + Build.VERSION.RELEASE + "  \u00b7  API " + Build.VERSION.SDK_INT);
        device.setTextSize(14);
        device.setTextColor(colorText);
        device.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        card.addView(device);

        TextView note = new TextView(this);
        note.setText("Unofficial helper. Root remounts /system and wipes the stock Play Store data; "
                + "the no-root clone cannot install or update other apps.");
        note.setTextSize(12);
        note.setTextColor(colorSecondary);
        note.setLineSpacing(dp(2), 1f);
        note.setPadding(0, dp(4), 0, dp(4));
        card.addView(note);
        return card;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(colorSurface);
        bg.setCornerRadius(dp(16));
        card.setBackground(bg);
        card.setElevation(dp(1));
        card.setPadding(dp(16), dp(14), dp(16), dp(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(12);
        card.setLayoutParams(lp);
        return card;
    }

    private TextView overline(String text) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setAllCaps(true);
        label.setTextSize(11);
        label.setLetterSpacing(0.12f);
        label.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        label.setTextColor(colorPrimary);
        label.setPadding(dp(2), 0, 0, dp(6));
        return label;
    }

    private RadioButton option(int id, String title, String detail) {
        RadioButton rb = new RadioButton(this);
        rb.setId(id);

        SpannableStringBuilder text = new SpannableStringBuilder();
        text.append(title);
        text.setSpan(new StyleSpan(Typeface.BOLD), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        text.setSpan(new AbsoluteSizeSpan(15, true), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        text.setSpan(new ForegroundColorSpan(colorText), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        text.append("\n");
        int start = text.length();
        text.append(detail);
        text.setSpan(new AbsoluteSizeSpan(13, true), start, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        text.setSpan(new ForegroundColorSpan(colorSecondary), start, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        rb.setText(text);

        rb.setLineSpacing(dp(1), 1f);
        rb.setPadding(dp(12), dp(10), dp(12), dp(10));
        rb.setButtonTintList(new ColorStateList(
                new int[][]{{android.R.attr.state_checked}, {}},
                new int[]{colorPrimary, UNCHECKED_CONTROL}));

        GradientDrawable row = new GradientDrawable();
        row.setCornerRadius(dp(10));
        row.setColor(new ColorStateList(
                new int[][]{{android.R.attr.state_checked}, {}},
                new int[]{colorSelected, 0x00000000}));
        rb.setBackground(new RippleDrawable(ColorStateList.valueOf(RIPPLE_LIGHT), row, null));
        return rb;
    }

    private LinearLayout.LayoutParams rowParams() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(2);
        return lp;
    }

    private Button buildPrimaryButton() {
        Button button = new Button(this);
        button.setText("Replace Play Store & Reboot");
        button.setAllCaps(false);
        button.setTextSize(15);
        button.setLetterSpacing(0.02f);
        button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        button.setTextColor(new ColorStateList(
                new int[][]{{-android.R.attr.state_enabled}, {}},
                new int[]{DISABLED_TEXT, 0xFFFFFFFF}));
        button.setMinHeight(dp(48));
        button.setPadding(dp(24), dp(10), dp(24), dp(10));
        button.setElevation(dp(2));
        button.setStateListAnimator(null);

        GradientDrawable fill = new GradientDrawable();
        fill.setColor(colorPrimary);
        fill.setCornerRadius(dp(24));
        button.setBackground(new RippleDrawable(ColorStateList.valueOf(RIPPLE_ON_PRIMARY), fill, null));
        return button;
    }

    private void begin() {
        final String version = selectedVersion;
        final boolean useRoot = rootMethod;
        goButton.setEnabled(false);
        setOptionsEnabled(false);
        append("\nPreparing bundled APK (Play Store " + version + ")...\n");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (useRoot) {
                        runRootFlow(version);
                    } else {
                        runNoRootFlow(version);
                    }
                } catch (Exception e) {
                    append("\nERROR: " + e + "\n");
                } finally {
                    enableButton();
                }
            }
        }, "install-runner").start();
    }

    private void runRootFlow(String version) throws IOException {
        String assetName = version + ".apk";
        File apk = new File(getCacheDir(), "playstore-" + assetName);
        copyAsset(assetName, apk);
        append("Bundled APK: Play Store " + version + ", " + (apk.length() / (1024 * 1024)) + " MB\n");
        runScript("swap.sh", buildScript(apk.getAbsolutePath()));
    }

    private void runScript(String name, String content) throws IOException {
        File script = new File(getCacheDir(), name);
        FileOutputStream out = new FileOutputStream(script);
        out.write(content.getBytes("UTF-8"));
        out.close();
        append("Requesting root...\n\n");
        runRoot("sh " + script.getAbsolutePath());
    }

    private void runNoRootFlow(String version) throws IOException {
        String assetName = "clone-" + version + ".apk";
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (dir == null) dir = new File(Environment.getExternalStorageDirectory(), "Download");
        if (!dir.isDirectory() && !dir.mkdirs()) {
            throw new IOException("cannot create " + dir);
        }
        File apk = new File(dir, "LyanoPlayStore-" + version + "-clone.apk");
        copyAsset(assetName, apk);
        append("Second-store APK: Play Store " + version + ", " + (apk.length() / (1024 * 1024)) + " MB\n");
        append("Package: com.lyano.playstore.patch (separate from the real Play Store)\n");
        append("Saved to: " + apk.getAbsolutePath() + "\n");
        append("Opening the system package installer...\n");

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(apk), "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);

        append("\nTap \"Install\" in the installer and wait for it to finish.\n");
        append("If Play Protect blocks it, tap \"Install anyway\".\n");
        append("If nothing opens, allow \"Unknown sources\" in Settings > Security, then retry.\n");
        append("\nNote: as a normal app the clone cannot install or update other apps.\n");
    }

    private void copyAsset(String assetName, File dest) throws IOException {
        InputStream in = getAssets().open(assetName);
        FileOutputStream out = new FileOutputStream(dest);
        byte[] buf = new byte[64 * 1024];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        in.close();
        out.close();
    }

    private String buildScript(String apkPath) {
        return ""
                + "echo \"[1/5] Remounting /system read-write\"\n"
                + "mount -o rw,remount /system 2>/dev/null || mount -o remount,rw /system 2>/dev/null || busybox mount -o remount,rw /system 2>/dev/null || mount -o remount,rw \"$(mount | awk '$3==\"/system\"{print $1; exit}')\" /system 2>/dev/null\n"
                + "if ! touch /system/.rwtest 2>/dev/null; then\n"
                + "  echo \"ERROR: /system is still read-only. Aborting.\"\n"
                + "  exit 1\n"
                + "fi\n"
                + "rm -f /system/.rwtest\n"
                + "\n"
                + "echo \"[2/5] Uninstalling existing Play Store\"\n"
                + "pm uninstall com.android.vending 2>&1\n"
                + "rm -rf /data/app/com.android.vending-*\n"
                + "rm -rf /data/data/com.android.vending\n"
                + "rm -rf /data/dalvik-cache/*com.android.vending*\n"
                + "rm -rf /data/dalvik-cache/*/*com.android.vending*\n"
                + "rm -rf /system/priv-app/Phonesky /system/priv-app/Phonesky.apk /system/priv-app/Phonesky.odex\n"
                + "rm -rf /system/app/Phonesky.apk /system/app/Phonesky.odex /system/app/Vending.apk /system/app/Vending.odex\n"
                + "\n"
                + "echo \"[3/5] Installing bundled APK as the system Play Store\"\n"
                + "if [ -d /system/priv-app ]; then\n"
                + "  TARGET=/system/priv-app/Phonesky/Phonesky.apk\n"
                + "else\n"
                + "  TARGET=/system/app/Phonesky.apk\n"
                + "fi\n"
                + "echo \"Target: $TARGET\"\n"
                + "case \"$TARGET\" in\n"
                + "  /system/priv-app/*) mkdir -p /system/priv-app/Phonesky 2>/dev/null ;;\n"
                + "  *) mkdir -p /system/app 2>/dev/null ;;\n"
                + "esac\n"
                + "if ! cat '" + apkPath + "' > \"$TARGET\" 2>/dev/null; then\n"
                + "  echo \"ERROR: copy into /system failed (out of space?). Aborting.\"\n"
                + "  exit 1\n"
                + "fi\n"
                + "chmod 755 /system/priv-app/Phonesky 2>/dev/null\n"
                + "chmod 644 \"$TARGET\"\n"
                + "chown 0:0 \"$TARGET\"\n"
                + "if [ ! -s \"$TARGET\" ]; then\n"
                + "  echo \"ERROR: installed file is empty. Aborting.\"\n"
                + "  exit 1\n"
                + "fi\n"
                + "\n"
                + "echo \"[4/5] Fixing SELinux context\"\n"
                + "restorecon -R /system/priv-app/Phonesky 2>/dev/null || chcon u:object_r:system_file:s0 \"$TARGET\" 2>/dev/null\n"
                + "ls -l \"$TARGET\"\n"
                + "sync\n"
                + "\n"
                + "echo \"[5/5] Rebooting\"\n"
                + "reboot\n";
    }

    private void runRoot(String command) {
        Process process = null;
        try {
            process = new ProcessBuilder("su", "-c", command).redirectErrorStream(true).start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                append(line + "\n");
            }
            int code = process.waitFor();
            append("\nExit code: " + code + "\n");
            if (code != 0) {
                append("Script failed. See the log above.\n");
            }
        } catch (Exception e) {
            append("\nERROR: could not run su (" + e + ").\nIs the phone rooted and is the root prompt allowed?\n");
        } finally {
            if (process != null) process.destroy();
        }
    }

    private void append(final String text) {
        ui.post(new Runnable() {
            @Override
            public void run() {
                logText.append(text);
                logView.setText(logText);
                if (logView.getLayout() != null) {
                    int scroll = logView.getLayout().getLineTop(logView.getLineCount()) - logView.getHeight();
                    if (scroll > 0) logView.scrollTo(0, scroll);
                }
            }
        });
    }

    private void enableButton() {
        ui.post(new Runnable() {
            @Override
            public void run() {
                goButton.setEnabled(true);
                setOptionsEnabled(true);
            }
        });
    }

    private void setOptionsEnabled(boolean enabled) {
        float alpha = enabled ? 1f : 0.45f;
        methodGroup.setAlpha(alpha);
        versionGroup.setAlpha(alpha);
        for (int i = 0; i < methodGroup.getChildCount(); i++) {
            methodGroup.getChildAt(i).setEnabled(enabled);
        }
        for (int i = 0; i < versionGroup.getChildCount(); i++) {
            versionGroup.getChildAt(i).setEnabled(enabled);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
