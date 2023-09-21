package com.Someone117.backup;

import android.content.Context;
import android.os.Environment;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.example.backup.R;
import com.jcraft.jsch.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;
import java.util.Vector;

import static com.Someone117.backup.MainActivity.CHANNEL_ID;

public class NetworkTask implements Runnable {
    private final Context id;

    private MainActivity.Record record;

    NetworkTask(Context id) {
        this.id = id;
    }

    // Remove files that were already uploaded
    private static void removeDuplicates(ArrayList<File> fileList, Vector<ChannelSftp.LsEntry> compare2) {
        for (int i = fileList.size() - 1; i >= 0; i--) {
            if (fileList.get(i).getName().startsWith(".trashed")) { //ToDo: some phones use this to denote trash, remove if you want
                fileList.remove(i);
            } else {
                boolean found = false;
                for (ChannelSftp.LsEntry c : compare2) {
                    if (c.getFilename().equals(fileList.get(i).getName())) {
                        found = true;
                        break;
                    }
                }
                if (found) {
                    fileList.remove(i);
                }
            }
        }
    }

    // Get all files from a folder
    public static ArrayList<File> getFiles(final File folder) {
        ArrayList<File> files = new ArrayList<>();
        for (final File fileEntry : Objects.requireNonNull(folder.listFiles())) {
            if (!fileEntry.isDirectory()) {
                files.add(fileEntry);
            }
        }
        return files;
    }

    public static String humanReadableByteCountBin(long bytes) {
        long absB = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
        if (absB < 1024) {
            return bytes + " B";
        }
        long value = absB;
        CharacterIterator ci = new StringCharacterIterator("KMGTPE");
        for (int i = 40; i >= 0 && absB > 0xfffccccccccccccL >> i; i -= 10) {
            value >>= 10;
            ci.next();
        }
        value *= Long.signum(bytes);
        return String.format(Locale.getDefault(), "%.1f %ciB", value / 1024.0, ci.current());
    }


    @Override
    public void run() {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(id);

        // connect to ssh server
        JSch jsch = new JSch();
        String host = record.ipAddress;
        String username = record.username;
        String password = record.password;
        java.util.Properties config = new java.util.Properties();
        config.put("StrictHostKeyChecking", "no");

        File rootsd = Environment.getExternalStorageDirectory();
        File local = new File(rootsd.getAbsolutePath() + "/DCIM/Camera");

        String remoteFolderPath = record.location;
        ArrayList<File> fileList = getFiles(local);

        try {
            Session session = jsch.getSession(username, host, 22);
            // use
            session.setPassword(password);
            session.setConfig(config);
            session.connect();

            ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect();


            // remove files that were already uploaded
            Vector<ChannelSftp.LsEntry> compare = channel.ls(remoteFolderPath);
            removeDuplicates(fileList, compare);

            // Send file
            if (fileList.size() > 0) {
                String[] files = new String[fileList.size()];
                long size = 0;
                for (int i = 0; i < fileList.size(); i++) {
                    files[i] = fileList.get(i).getAbsolutePath();
                    size += Files.size(fileList.get(i).toPath());
                }
                // notify the user that files are uploading
                NotificationCompat.Builder builder = new NotificationCompat.Builder(id, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_launcher_background)
                        .setContentTitle("Uploading files: " + humanReadableByteCountBin(size))
                        .setContentText(fileList.size() + " files to upload")
                        .setPriority(NotificationCompat.PRIORITY_MIN);

                int PROGRESS_MAX = files.length;
                int PROGRESS_CURRENT = 0;
                builder.setProgress(PROGRESS_MAX, PROGRESS_CURRENT, false);
                notificationManager.notify(1, builder.build());

                // upload the files
                for (int i = 0; i < files.length; i++) {
                    channel.put(files[i], remoteFolderPath, ChannelSftp.APPEND);
                    PROGRESS_CURRENT = i;
                    builder.setProgress(PROGRESS_MAX, PROGRESS_CURRENT, false)
                            .setContentText(i + " files done out of " + fileList.size() + " files")
                            .setPriority(NotificationCompat.PRIORITY_MIN);

                    notificationManager.notify(1, builder.build());
                }
                builder.setContentText("Upload Complete")
                        .setProgress(0, 0, false);
                notificationManager.notify(1, builder.build());
            } else {
                NotificationCompat.Builder builder = new NotificationCompat.Builder(id, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_launcher_background)
                        .setContentTitle("No files to upload")
                        .setContentText("")
                        .setPriority(NotificationCompat.PRIORITY_MIN);

                notificationManager.notify(1, builder.build());
            }

//             Disconnect and close the SSH session and channel
            channel.disconnect();
            session.disconnect();
        } catch (SftpException | JSchException | IOException e) {
            e.printStackTrace();
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this.id, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_background)
                    .setContentTitle("Error!")
                    .setContentText(e.getLocalizedMessage())
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);

            // notificationId is a unique int for each notification that you must define
            notificationManager.notify(3, builder.build());

        }

    }

    public void setRecord(MainActivity.Record record) {
        this.record = record;
    }
}
