package com.Someone117.backup;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.example.backup.R;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.Someone117.backup.MainActivity.*;

public class BackupReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // get ssid of network
        WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        String ssid = wifiInfo.getSSID();

        // check if the phone is on the right network
        if(ssid.equals("\"network ssid\"")) {
            ExecutorService executor;

            executor = Executors.newFixedThreadPool(1);

            // Create and submit a network task for execution
            NetworkTask networkTask = new NetworkTask(context);
            executor.submit(networkTask);
        } else {
            // notify the user that they are not on the correct Wi-Fi network
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_background)
                    .setContentTitle("Incorrect Wifi")
                    .setContentText("No Uploads Completed")
                    .setPriority(NotificationCompat.PRIORITY_MIN);


            notificationManager.notify(0, builder.build());
        }
    }
}
