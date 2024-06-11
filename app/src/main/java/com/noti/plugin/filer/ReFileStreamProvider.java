package com.noti.plugin.filer;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.os.ParcelFileDescriptor.AutoCloseOutputStream;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Objects;

public class ReFileStreamProvider extends ContentProvider {

    public static final HashMap<String, String> fileListMap = new HashMap<>();
    public static final HashMap<String, FileInputStream> inputStreamMap = new HashMap<>();

    private static final String AUTHORITY = "com.noti.plugin.filer.ReFileStreamProvider";
    private static final int FILE = 1;
    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        uriMatcher.addURI(AUTHORITY, "*", FILE);
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode) throws FileNotFoundException {
        PowerUtils powerUtils = PowerUtils.getInstance(getContext());
        if(powerUtils != null) {
            powerUtils.acquire();
        }

        int match = uriMatcher.match(uri);
        if (match != FILE) {
            throw new FileNotFoundException("Unsupported URI: " + uri);
        }

        try {
            String[] ipcChannelUri = uri.toString().split("/");
            String ipcChannelName = ipcChannelUri[ipcChannelUri.length - 1];

            String ipcType = ipcChannelName.split("_")[0];
            ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createReliablePipe();
            Thread transportThread;

            switch (ipcType) {
                case FileListWorker.IPC_PROVIDER_DATA_PREFIX -> transportThread = new FileTransferThread(ipcChannelName, new AutoCloseOutputStream(pipe[1]));
                case FileListWorker.IPC_PROVIDER_LIST_PREFIX -> transportThread = new ByteArrayTransferThread(ipcChannelName, new AutoCloseOutputStream(pipe[1]));
                default -> throw new FileNotFoundException("Unsupported IPC type: " + ipcChannelName);
            }

            transportThread.start();
            if(pipe[1].canDetectErrors()) {
                pipe[1].checkError();
            }

            return pipe[0];
        } catch (IOException e) {
            e.printStackTrace();
            throw new FileNotFoundException("Error creating pipe");
        }
    }

    private static class ByteArrayTransferThread extends Thread {
        private final OutputStream outputStream;
        private final byte[] data;
        private final String channelName;

        ByteArrayTransferThread(String channelName, OutputStream outputStream) {
            this.channelName = channelName;
            this.outputStream = outputStream;
            this.data = Objects.requireNonNull(ReFileStreamProvider.fileListMap.get(channelName)).getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public void run() {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
            byte[] buffer = new byte[8192];
            int bytesRead;
            try {
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    inputStream.close();
                    outputStream.close();
                    ReFileStreamProvider.fileListMap.remove(channelName);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static class FileTransferThread extends Thread {
        private final OutputStream outputStream;
        private final FileInputStream inputStream;
        private final String channelName;

        FileTransferThread(String channelName, OutputStream outputStream) {
            this.channelName = channelName;
            this.outputStream = outputStream;
            this.inputStream = ReFileStreamProvider.inputStreamMap.get(channelName);
        }

        @Override
        public void run() {
            Log.d("ddd", "ReFileTransferThread.FileTransferThread");
            try {
                byte[] buf = new byte[8192];
                int length;
                while ((length = inputStream.read(buf)) != -1) {
                    outputStream.write(buf, 0, length);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    inputStream.close();
                    outputStream.close();
                    ReFileStreamProvider.inputStreamMap.remove(channelName);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Log.d("ddd", "ReFileTransferThread.FileTransferThread22222");
            }
        }
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public String getType(@NonNull Uri uri) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}