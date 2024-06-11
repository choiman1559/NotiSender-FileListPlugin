package com.noti.plugin.filer;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.PowerManager;

import androidx.core.content.ContextCompat;

import com.noti.plugin.Plugin;

public class Application extends android.app.Application {
    @Override
    public void onCreate() {
        super.onCreate();
        initPlugin(this);
    }

    public static void initPlugin(Context context) {
        Plugin plugin = Plugin.init(new PluginResponses());

        plugin.setPluginDescription("Plugin for Implement Remote File Functions");
        plugin.setAppPackageName(context.getPackageName());
        plugin.setSettingClass(SettingActivity.class);
        plugin.setPluginTitle("Remote File Plugin");
        plugin.setRequireSensitiveAPI(false);

        try {
            plugin.setPluginHostInject(FileListWorker.class);
        } catch (Exception e) {
            e.printStackTrace();
        }

        checkPermission(context);
    }

    public static boolean checkPermission(Context context) {
        boolean isPermissionReady = checkFilePermission(context) && checkPowerPermission(context);
        Plugin.getInstance().setPluginReady(isPermissionReady);
        return isPermissionReady;
    }

    public static boolean checkPowerPermission(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        return Build.VERSION.SDK_INT < 23 || pm.isIgnoringBatteryOptimizations(context.getPackageName());
    }

    public static boolean checkFilePermission(Context context) {
        boolean isPermissionGranted = false;
        if (Build.VERSION.SDK_INT >= 30 && Environment.isExternalStorageManager()) {
            isPermissionGranted = true;
        } else if (Build.VERSION.SDK_INT > 28 &&
                (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                        || ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)) {
            isPermissionGranted = true;
        } else if (Build.VERSION.SDK_INT <= 28) {
            isPermissionGranted = true;
        }

        return isPermissionGranted;
    }
}
