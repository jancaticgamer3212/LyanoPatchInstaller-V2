package com.qoder.playstoreswap;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MainActivity extends Activity {

    private final Handler ui = new Handler();
    private final StringBuilder logText = new StringBuilder();

    private TextView logView;
    private Button goButton;

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
        title.setText("Play Store \u2192 /system/priv-app/Phonesky");
        title.setTextSize(20);
        title.setTextColor(0xFFECF0F1);
        root.addView(title);

        TextView desc = new TextView(this);
        desc.setText("Remounts /system read-write, uninstalls the current Play Store and wipes its data, "
                + "installs the bundled 5.12.10 build as a system app in /system/priv-app/Phonesky "
                + "(old .odex files in that folder are removed), then reboots.\n\nRequires root (su).");
        desc.setTextSize(14);
        desc.setTextColor(0xFF9AA7AD);
        desc.setPadding(0, dp(8), 0, 0);
        root.addView(desc);

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
        append("Bundled Play Store 5.12.10 ready. Tap the button to continue.\n");
    }

    private void begin() {
        goButton.setEnabled(false);
        append("\nPreparing bundled APK...\n");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    File apk = new File(getCacheDir(), "playstore.apk");
                    if (!apk.exists() || apk.length() == 0) {
                        InputStream in = getAssets().open("playstore.apk");
                        FileOutputStream out = new FileOutputStream(apk);
                        byte[] buf = new byte[64 * 1024];
                        int n;
                        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                        in.close();
                        out.close();
                    }
                    append("Bundled APK: " + (apk.length() / (1024 * 1024)) + " MB\n");

                    File script = new File(getCacheDir(), "swap.sh");
                    FileOutputStream out = new FileOutputStream(script);
                    out.write(buildScript(apk.getAbsolutePath()).getBytes("UTF-8"));
                    out.close();

                    append("Requesting root...\n\n");
                    runRoot("sh " + script.getAbsolutePath());
                } catch (Exception e) {
                    append("\nERROR: " + e + "\n");
                } finally {
                    enableButton();
                }
            }
        }, "swap-runner").start();
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
            }
        });
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
