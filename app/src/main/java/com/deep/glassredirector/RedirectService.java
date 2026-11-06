package com.deep.glassredirector;

import android.app.AppOpsManager;
import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;
import android.util.Log;

public class RedirectService extends Service {
    private static final String TAG = "GlassRedirector";
    private static final String AMAZON_HOME = "com.amazon.tv.launcher";
    private static final String GLASS_PACKAGE = "com.fares.glass";
    private static final long POLL_INTERVAL_MS = 700L;
    private static final long MIN_REDIRECT_INTERVAL_MS = 1800L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private UsageStatsManager usageStatsManager;
    private long lastQueryTime;
    private long lastHandledAmazonEvent;
    private long lastRedirectAt;
    private boolean running;

    private final Runnable poller = new Runnable() {
        @Override
        public void run() {
            if (!running) return;
            pollUsageEvents();
            handler.postDelayed(this, POLL_INTERVAL_MS);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        // Start at now so old Amazon Home events do not trigger an immediate redirect.
        lastQueryTime = System.currentTimeMillis();
        running = true;
        handler.post(poller);
        Log.i(TAG, "Redirect monitoring started");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!hasUsageAccess()) {
            Log.w(TAG, "Usage Access missing; stopping service");
            stopSelf();
            return START_NOT_STICKY;
        }
        return START_STICKY;
    }

    private boolean hasUsageAccess() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(), getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    private void pollUsageEvents() {
        if (!hasUsageAccess()) {
            Log.w(TAG, "Usage Access revoked; stopping service");
            stopSelf();
            return;
        }

        long now = System.currentTimeMillis();
        // A small overlap avoids missing events that arrive at a polling boundary.
        long begin = Math.max(0L, lastQueryTime - 1200L);
        try {
            UsageEvents events = usageStatsManager.queryEvents(begin, now);
            UsageEvents.Event event = new UsageEvents.Event();
            while (events != null && events.hasNextEvent()) {
                events.getNextEvent(event);
                if (event.getEventType() != UsageEvents.Event.MOVE_TO_FOREGROUND) continue;
                if (!AMAZON_HOME.equals(event.getPackageName())) continue;

                long eventTime = event.getTimeStamp();
                if (eventTime <= lastHandledAmazonEvent) continue;
                lastHandledAmazonEvent = eventTime;

                if (now - lastRedirectAt < MIN_REDIRECT_INTERVAL_MS) continue;
                redirectToGlass();
                lastRedirectAt = now;
            }
            lastQueryTime = now;
        } catch (SecurityException e) {
            Log.e(TAG, "Usage events access failed", e);
            stopSelf();
        } catch (Exception e) {
            Log.e(TAG, "Error while checking foreground app", e);
        }
    }

    private void redirectToGlass() {
        Intent launch = getPackageManager().getLaunchIntentForPackage(GLASS_PACKAGE);
        if (launch == null) {
            Log.e(TAG, "Glass Launcher is not installed");
            return;
        }
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        try {
            startActivity(launch);
            Log.i(TAG, "Amazon Home detected; brought Glass Launcher forward");
        } catch (Exception e) {
            Log.e(TAG, "Could not bring Glass Launcher forward", e);
        }
    }

    @Override
    public void onDestroy() {
        running = false;
        handler.removeCallbacks(poller);
        Log.i(TAG, "Redirect monitoring stopped");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
