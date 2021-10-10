/*
 * Copyright (C) 2021, Aslam Anver
 * https://github.com/aslamanver/android-backup-task
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.ActivityManager;
import android.content.Context;

import com.cba.payable.BuildConfig;
import com.mpos.mvp.presenter.ProfilePresenter;
import com.mpos.util.PLog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class BackupTask {

    private static final String DATA_PATH = "/data/data/" + BuildConfig.APPLICATION_ID;
    private static final String BACKUP_PATH = "/sdcard/.backups/" + BuildConfig.APPLICATION_ID;
    private static Thread scheduleBackupThread;

    public static void copyFileOrDirectory(String srcDir, String dstDir) {
        copyFileOrDirectory(srcDir, dstDir, true);
    }

    private static void copyFileOrDirectory(String srcDir, String dstDir, boolean initial) {
        try {
            File src = new File(srcDir);
            File dst = initial ? new File(dstDir) : new File(dstDir, src.getName());
            if (initial && dst.exists()) deleteFiles(dst);
            if (src.isDirectory()) {
                String files[] = src.list();
                int filesLength = files.length;
                for (int i = 0; i < filesLength; i++) {
                    String src1 = (new File(src, files[i]).getPath());
                    String dst1 = dst.getPath();
                    copyFileOrDirectory(src1, dst1, false);
                }
            } else {
                copyFile(src, dst);
            }
        } catch (Exception e) {
            // e.printStackTrace();
        }
    }

    private static void copyFile(File sourceFile, File destFile) throws IOException {
        if (!destFile.getParentFile().exists()) destFile.getParentFile().mkdirs();
        if (!destFile.exists()) destFile.createNewFile();
        FileChannel source = null;
        FileChannel destination = null;
        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
        } finally {
            if (source != null) {
                source.close();
            }
            if (destination != null) {
                destination.close();
            }
        }
    }

    private static void deleteFiles(File file) {
        if (file.exists()) {
            if (file.isDirectory()) {
                for (File child : file.listFiles()) deleteFiles(child);
            }
            file.delete();
        }
    }

    public static boolean reloadSharedPreference(Context context, String name) {
        return context.getSharedPreferences(name, Context.MODE_MULTI_PROCESS)
                .edit()
                .putBoolean("RELOAD", true)
                .commit();
    }

    public static void reloadSharedPreference(Context context) {
        File src = new File(DATA_PATH + "/shared_prefs");
        if (src.isDirectory()) {
            String files[] = src.list();
            for (int i = 0; i < files.length; i++) {
                if (files[i].contains(".xml")) {
                    reloadSharedPreference(context, files[i].replace(".xml", ""));
                }
            }
        }
    }

    public static void backup() {
        copyFileOrDirectory(DATA_PATH, BACKUP_PATH, true);
    }

    public static void restore(Context context) {
        copyFileOrDirectory(BACKUP_PATH, DATA_PATH);
        reloadSharedPreference(context);
        PLog.e("BackupTask", "Restore completed.");
    }

    public static void clear() {
        deleteFiles(new File(BACKUP_PATH));
    }

    public static boolean isDataFound() {
        return new File(BACKUP_PATH).exists();
    }

    public static void scheduleBackup(Context context) {
        scheduleBackup(context, 5000);
    }

    public static void scheduleBackup(Context context, int millis) {
        if (scheduleBackupThread != null) scheduleBackupThread.interrupt();
        scheduleBackupThread = new Thread(() -> {
            try {
                Thread.sleep(millis);
                if (ProfilePresenter.getPrefInstance(context).activeMerchantType() != ProfilePresenter.NO_LOGIN) {
                    BackupTask.backup();
                    PLog.e("BackupTask", "BackupTask.scheduleBackup()");
                }
                scheduleBackupThread = null;
            } catch (InterruptedException e) {
                // e.printStackTrace();
            }
        });
        scheduleBackupThread.start();
    }

    public static void clearData(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(context.ACTIVITY_SERVICE);
        activityManager.clearApplicationUserData();
    }
}
