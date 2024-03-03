package com.noti.plugin.filer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.app.ActivityCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

public class SettingActivity extends AppCompatActivity {

    MaterialButton Permit_File;

    ActivityResultLauncher<Intent> startAllFilesPermit = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (Build.VERSION.SDK_INT >= 30 && Environment.isExternalStorageManager()) {
            setButtonCompleted(Permit_File);
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        boolean isPermissionReady = Application.checkPermission(this);

        Permit_File = findViewById(R.id.Permit_File);
        Permit_File.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= 30) {
                Uri uri = Uri.parse("package:" + BuildConfig.APPLICATION_ID);
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri);
                startAllFilesPermit.launch(intent);
            } else if (Build.VERSION.SDK_INT > 28) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 101);
            }
        });

        if(isPermissionReady) {
            setButtonCompleted(Permit_File);
        }

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener((v) -> this.finish());
    }

    @SuppressLint("SetTextI18n")
    void setButtonCompleted(MaterialButton button) {
        button.setEnabled(false);
        button.setText("Sms Access Permitted");
        button.setIcon(AppCompatResources.getDrawable(SettingActivity.this, R.drawable.baseline_check_24));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int foo : grantResults) {
            if (requestCode == 101 && foo == PackageManager.PERMISSION_GRANTED) {
                setButtonCompleted(Permit_File);
            }
        }
    }
}
