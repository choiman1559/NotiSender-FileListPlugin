package com.noti.plugin.filer;

import android.content.Context;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.noti.plugin.filer.db.ReFileConst;
import com.noti.plugin.filer.db.RemoteFolderDoc;
import com.noti.plugin.listener.PluginHostInject;
import com.noti.plugin.process.PluginAction;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FileListWorker extends PluginHostInject {

    public static final String IPC_SOCKET_PREFIX = "ReFileIPC_%d";
    public static final String ACTION_REQUEST_FILE_LIST = "request_file_list";
    public static final String ACTION_RESPONSE_FILE_LIST = "response_file_list";
    public static final String ACTION_REQUEST_UPLOAD = "request_file_upload";
    public static final String ACTION_RESPONSE_UPLOAD = "response_file_upload";
    public static final String ACTION_REQUEST_METADATA = "request_file_metadata";
    public static final String ACTION_RESPONSE_METADATA = "response_file_metadata";

    @Override
    public void onHostInject(Context context, @NonNull String dataType, @Nullable String deviceInfo, @Nullable String arg) {
        super.onHostInject(context, dataType, deviceInfo, arg);
        Log.d("ddd", dataType);
        switch (dataType) {
            case ACTION_REQUEST_FILE_LIST:
                if (deviceInfo != null && arg != null) {
                    String[] args = arg.split("\\|");
                    runFileListWork(context, deviceInfo, Integer.parseInt(args[0]), args[1].equals("true"));
                }
                break;

            case ACTION_REQUEST_UPLOAD:
                if (deviceInfo != null && arg != null) {
                    String[] args = arg.split("\\|");
                    runFileUploadWork(context, deviceInfo, args[0]);
                }
                break;

            case ACTION_REQUEST_METADATA:
                if (deviceInfo != null && arg != null) {
                    runFileHashWork(context, deviceInfo, arg);
                }
                break;
        }
    }

    public void runFileListWork(Context context, @Nullable String deviceInfo, int indexMaximumSize, boolean indexHiddenFiles) {
        String internalPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        File[] allExternalFilesDirs = ContextCompat.getExternalFilesDirs(context, null);
        Map<String, Object> drives = new HashMap<>();

        for (File filesDir : allExternalFilesDirs) {
            if (filesDir != null) {
                int nameSubPos = filesDir.getAbsolutePath().lastIndexOf("/Android/data");
                if (nameSubPos > 0) {
                    String filesDirName = filesDir.getAbsolutePath().substring(0, nameSubPos);
                    RemoteFolderDoc remoteFolderDoc = new RemoteFolderDoc(indexMaximumSize, indexHiddenFiles, new File(filesDirName));

                    if (filesDirName.equals(internalPath)) {
                        drives.put(ReFileConst.DATA_TYPE_INTERNAL_STORAGE, remoteFolderDoc.getLists());
                    } else {
                        String[] dividerArr = filesDirName.split("/");
                        drives.put(dividerArr[dividerArr.length - 1], remoteFolderDoc.getLists());
                    }
                }
            }
        }

        drives.put(ReFileConst.DATA_TYPE_LAST_MODIFIED, Calendar.getInstance().getTimeInMillis());
        final String finalFileListString = new JSONObject(drives).toString();
        final String ipcChannelName = String.format(Locale.getDefault(), IPC_SOCKET_PREFIX, finalFileListString.hashCode());

        new Thread(() -> {
            try (LocalServerSocket serverSocket = new LocalServerSocket(ipcChannelName)) {
                while (true) {
                    LocalSocket clientSocket = serverSocket.accept();
                    if (clientSocket.isConnected()) {
                        OutputStream outputStream = clientSocket.getOutputStream();
                        outputStream.write(finalFileListString.getBytes());
                        clientSocket.close();
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
        PluginAction.responseHostApiInject(context, deviceInfo, ACTION_RESPONSE_FILE_LIST, ipcChannelName);
    }

    public void runFileUploadWork(Context context, @Nullable String deviceInfo, String filePath) {
        File targetFile = new File(filePath);
        final String ipcChannelName = String.format(Locale.getDefault(), IPC_SOCKET_PREFIX, filePath.hashCode());

        if (targetFile.exists() && targetFile.isFile() && targetFile.canRead()) {
            new Thread(() -> {
                try (FileInputStream fileInputStream = new FileInputStream(targetFile);
                     LocalServerSocket serverSocket = new LocalServerSocket(ipcChannelName)) {
                    while (true) {
                        LocalSocket clientSocket = serverSocket.accept();
                        if (clientSocket.isConnected()) {
                            OutputStream outputStream = clientSocket.getOutputStream();
                            byte[] buf = new byte[8192];
                            int length;
                            while ((length = fileInputStream.read(buf)) != -1) {
                                outputStream.write(buf, 0, length);
                            }
                            clientSocket.close();
                            break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    PluginAction.responseHostApiInject(context, deviceInfo, ACTION_RESPONSE_UPLOAD, filePath + "|Error: " + e);
                }
            }).start();
            PluginAction.responseHostApiInject(context, deviceInfo, ACTION_RESPONSE_UPLOAD, filePath + "|" + ipcChannelName);
        } else {
            PluginAction.responseHostApiInject(context, deviceInfo, ACTION_RESPONSE_UPLOAD, filePath + "|Error: File not exists or not readable!");
        }
    }

    public void runFileHashWork(Context context, @Nullable String deviceInfo, String filePath) {
        try {
            File file = new File(filePath);
            PluginAction.responseHostApiInject(context, deviceInfo, ACTION_RESPONSE_METADATA, filePath + "|" + getFileMD5Hash(file));
        } catch (Exception e) {
            PluginAction.responseHostApiInject(context, deviceInfo, ACTION_RESPONSE_METADATA, filePath + "|Error: File not exists or not readable!");
        }
    }

    private static String getFileMD5Hash(File file) throws NoSuchAlgorithmException, IOException {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        InputStream is = new FileInputStream(file);
        byte[] buffer = new byte[8192];
        int read;
        while ((read = is.read(buffer)) > 0) {
            digest.update(buffer, 0, read);
        }

        is.close();
        byte[] md5sum = digest.digest();
        BigInteger bigInt = new BigInteger(1, md5sum);
        return bigInt.toString(16);
    }
}
