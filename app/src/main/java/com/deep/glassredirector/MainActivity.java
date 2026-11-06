package com.deep.glassredirector;

import android.app.Activity;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Process;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        int pad = dp(28);
        layout.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("Glass Home Redirector");
        title.setTextSize(25);
        layout.addView(title);

        TextView description = new TextView(this);
        description.setText("When Fire OS brings Amazon Home to the foreground, this helper brings Glass Launcher back. Amazon's launcher remains installed and enabled.");
        description.setTextSize(17);
        description.setPadding(0, dp(14), 0, dp(18));
        layout.addView(description);

        status = new TextView(this);
        status.setTextSize(16);
        status.setPadding(0, 0, 0, dp(18));
        layout.addView(status);

        Button permissionButton = new Button(this);
        permissionButton.setText("Open Usage Access settings");
        permissionButton.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
            } catch (Exception e) {
                status.setText("Could not open Usage Access settings. Grant access with the ADB command in README.md.");
            }
        });
        layout.addView(permissionButton);

        Button startButton = new Button(this);
        startButton.setText("Start redirect monitoring");
        startButton.setOnClickListener(v -> startMonitoring());
        layout.addView(startButton);

        Button stopButton = new Button(this);
        stopButton.setText("Stop redirect monitoring");
        stopButton.setOnClickListener(v -> {
            stopService(new Intent(this, RedirectService.class));
            status.setText("Monitoring stopped. Amazon Home will behave normally.");
        });
        layout.addView(stopButton);

        Button glassButton = new Button(this);
        glassButton.setText("Open Glass Launcher now");
        glassButton.setOnClickListener(v -> openGlass());
        layout.addView(glassButton);

        setContentView(layout);
        refreshStatus();
        if (hasUsageAccess()) {
            startMonitoring();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    private void startMonitoring() {
        if (!hasUsageAccess()) {
            status.setText("Usage Access is not granted yet. Open Usage Access settings, enable it for Glass Home Redirector, then return and tap Start.");
            return;
        }
        startService(new Intent(this, RedirectService.class));
        status.setText("Monitoring is running. Press the remote's Home button to test.");
    }

    private boolean hasUsageAccess() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(), getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    private void refreshStatus() {
        if (status == null) return;
        status.setText(hasUsageAccess()
                ? "Usage Access: granted. Monitoring starts when this app is opened."
                : "Usage Access: not granted. Grant it before starting the helper.");
    }

    private void openGlass() {
        Intent launch = getPackageManager().getLaunchIntentForPackage("com.fares.glass");
        if (launch == null) {
            status.setText("Glass Launcher package com.fares.glass was not found.");
            return;
        }
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(launch);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
