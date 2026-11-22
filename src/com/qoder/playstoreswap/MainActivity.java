package com.qoder.playstoreswap;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MainActivity extends Activity {

    private final Handler ui = new Handler();
    private final StringBuilder logText = new StringBuilder();

    private TextView logView;
    private Button goButton;
    private RadioGroup versionGroup;
    private RadioGroup methodGroup;
    private String selectedVersion = "5.12.10";
    private boolean rootMethod = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        int pad = dp(16);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(0xFF10161A);

        TextView title = new TextView(this);
        title.setText("LyanoPatchInstaller V2");
        title.setTextSize(20);
        title.setTextColor(0xFFECF0F1);
        root.addView(title);

        TextView desc = new TextView(this);
        desc.setText("Choose an install method:\n"
                + "\u2022 Root: replaces the real Play Store in /system/priv-app/Phonesky, "
                + "wipes its data and reboots.\n"
                + "\u2022 No root: installs the build as a separate normal app "
                + "(com.lyano.playstore.patch), so the existing Play Store stays. Both can coexist.");
        desc.setTextSize(14);
        desc.setTextColor(0xFF9AA7AD);
        desc.setPadding(0, dp(8), 0, 0);
        root.addView(desc);

        TextView pickMethod = new TextView(this);
        pickMethod.setText("Install method:");
        pickMethod.setTextSize(14);
        pickMethod.setTextColor(0xFFECF0F1);
        pickMethod.setPadding(0, dp(12), 0, dp(2));
        root.addView(pickMethod);

        methodGroup = new RadioGroup(this);
        methodGroup.setOrientation(RadioGroup.VERTICAL);
        final String[] methods = {
                "Root - replace system Play Store (reboot)",
                "No root - install as second store"
        };
        for (int i = 0; i < methods.length; i++) {
            RadioButton rb = new RadioButton(this);
            rb.setId(100 + i);
            rb.setText(methods[i]);
            rb.setTextColor(0xFFD5DDE0);
            methodGroup.addView(rb);
        }
        ((RadioButton) methodGroup.getChildAt(0)).setChecked(true);
        methodGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                rootMethod = checkedId == 100;
                goButton.setText(rootMethod ? "Replace Play Store & Reboot" : "Install Second Play Store");
                append("Method: " + methods[checkedId - 100] + "\n");
            }
        });
        root.addView(methodGroup);

        TextView pick = new TextView(this);
        pick.setText("Patched Play Store build:");
        pick.setTextSize(14);
        pick.setTextColor(0xFFECF0F1);
        pick.setPadding(0, dp(12), 0, dp(2));
        root.addView(pick);

        versionGroup = new RadioGroup(this);
        versionGroup.setOrientation(RadioGroup.VERTICAL);
        final String[] versions = {"5.12.10", "4.4.22", "3.4.7"};
        for (int i = 0; i < versions.length; i++) {
            RadioButton rb = new RadioButton(this);
            rb.setId(i + 1);
            rb.setText(versions[i] + (i == 0 ? "  (newest)" : i == 1 ? "  (older)" : "  (oldest)"));
            rb.setTextColor(0xFFD5DDE0);
            versionGroup.addView(rb);
        }
        ((RadioButton) versionGroup.getChildAt(0)).setChecked(true);
        versionGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                selectedVersion = versions[checkedId - 1];
                append("Selected build: Play Store " + selectedVersion + "\n");
            }
        });
        root.addView(versionGroup);

        logView = new TextView(this);
        logView.setTextSize(12);
        logView.setTextColor(0xFF7FD1A8);
        logView.setTypeface(Typeface.MONOSPACE);
        logView.setBackgroundColor(0xFF07090B);
        logView.setPadding(dp(8), dp(8), dp(8), dp(8));
        logView.setMovementMethod(new ScrollingMovementMethod());
        LinearLayout.LayoutParams logLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        logLp.topMargin = dp(12);
        logLp.bottomMargin = dp(12);
        root.addView(logView, logLp);

        goButton = new Button(this);
        goButton.setText("Replace Play Store & Reboot");
        goButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                begin();
            }
        });
        root.addView(goButton);

        setContentView(root);
        append("Root method is selected. Tap the button to continue.\n");
    }

    private void begin() {
        final String version = selectedVersion;
        final boolean useRoot = rootMethod;
        goButton.setEnabled(false);
        versionGroup.setEnabled(false);
        methodGroup.setEnabled(false);
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

        File script = new File(getCacheDir(), "swap.sh");
        FileOutputStream out = new FileOutputStream(script);
        out.write(buildScript(apk.getAbsolutePath()).getBytes("UTF-8"));
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
                + "\n"
                + "echo \"[3/5] Installing bundled APK into /system/priv-app/Phonesky\"\n"
                + "cp '" + apkPath + "' /data/local/tmp/playstore.apk\n"
                + "mkdir -p /system/priv-app/Phonesky\n"
                + "cp /data/local/tmp/playstore.apk /system/priv-app/Phonesky/Phonesky.apk\n"
                + "chmod 755 /system/priv-app/Phonesky\n"
                + "chmod 644 /system/priv-app/Phonesky/Phonesky.apk\n"
                + "chown 0:0 /system/priv-app/Phonesky/Phonesky.apk\n"
                + "if [ ! -s /system/priv-app/Phonesky/Phonesky.apk ]; then\n"
                + "  echo \"ERROR: copy into /system failed (out of space?). Aborting.\"\n"
                + "  exit 1\n"
                + "fi\n"
                + "rm -f /data/local/tmp/playstore.apk\n"
                + "\n"
                + "echo \"[4/5] Fixing SELinux context\"\n"
                + "restorecon -R /system/priv-app/Phonesky 2>/dev/null || chcon u:object_r:system_file:s0 /system/priv-app/Phonesky /system/priv-app/Phonesky/Phonesky.apk\n"
                + "ls -l /system/priv-app/Phonesky\n"
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
                versionGroup.setEnabled(true);
                methodGroup.setEnabled(true);
            }
        });
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
