package com.Someone117.backup;

import android.content.Context;
import android.os.Environment;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.example.backup.R;
import com.jcraft.jsch.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Vector;

import static com.Someone117.backup.MainActivity.CHANNEL_ID;

public class NetworkTask implements Runnable {
    private final Context id;
    NetworkTask(Context a) {
        this.id = a;
    }
    @Override
    public void run() {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(id);


        // connect to ssh server
        //ToDo: fill in your information
        JSch jsch = new JSch();
        String host = "host.location";
        String username = "username";
        String password = "password";
        java.util.Properties config = new java.util.Properties();
        config.put("StrictHostKeyChecking", "no");
        //ToDo: end fill in your information

        File rootsd = Environment.getExternalStorageDirectory();
        //ToDo fill in your information
        File local = new File(rootsd.getAbsolutePath() + "/local/file/location");
        String remoteFolderPath = "/remote/folder";
        //ToDo: end fill in your information
        ArrayList<File> fileList = getFiles(local);

        try{
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
            if(fileList.size() > 0) {
                String[] files = new String[fileList.size()];
                for(int i = 0; i < fileList.size(); i++) {
                    files[i] = fileList.get(i).getAbsolutePath();
                }
                // notify the user that files are uploading
                NotificationCompat.Builder builder = new NotificationCompat.Builder(id, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_launcher_background)
                        .setContentTitle("Uploading files")
                        .setContentText(fileList.size() + " files to upload")
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT);

                    int PROGRESS_MAX = files.length;
                    int PROGRESS_CURRENT = 0;
                    builder.setProgress(PROGRESS_MAX, PROGRESS_CURRENT, false);
                    notificationManager.notify(1, builder.build());

                    // upload the files
                for(int i = 0; i < files.length; i++) {
                    channel.put(files[i], remoteFolderPath, ChannelSftp.APPEND);
                    PROGRESS_CURRENT = i;
                    builder.setProgress(PROGRESS_MAX, PROGRESS_CURRENT, false)
                            .setContentText(i + " files done out of " + fileList.size() + " files");

                    notificationManager.notify(1, builder.build());
                }
                builder.setContentText("Upload Complete")
                        .setProgress(0,0,false);
                notificationManager.notify(1, builder.build());
            } else {
                NotificationCompat.Builder builder = new NotificationCompat.Builder(id, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_launcher_background)
                        .setContentTitle("No files to upload")
                        .setContentText("")
                        .setPriority(NotificationCompat.PRIORITY_MIN);

                notificationManager.notify(1, builder.build());
            }

            // Disconnect and close the SSH session and channel
            channel.disconnect();
            session.disconnect();
        } catch (SftpException | JSchException e) {
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

    // Remove files that were already uploaded
    private static void removeDuplicates(ArrayList<File> fileList, Vector<ChannelSftp.LsEntry> compare2) {
        for(int i = fileList.size() - 1; i >= 0; i--) {
            if(fileList.get(i).getName().startsWith(".trashed")) { //ToDo: some phones use this to denote trash, remove if you want
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
}
