package com.noti.plugin.filer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.noti.plugin.Plugin;

import java.util.Objects;

public class OnBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if(Objects.equals(intent.getAction(), Intent.ACTION_BOOT_COMPLETED) && Plugin.getInstanceAllowNull() == null) {
            Application.initPlugin(context);
        }
    }
}
