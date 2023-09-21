package com.Someone117.backup;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import com.example.backup.R;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    public static final String CHANNEL_ID = "hi";
    public final NetworkTask networkTask = new NetworkTask(this);

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences preferences = getPreferences(MODE_PRIVATE);

        CharSequence name = "My Channel";
        String description = "My Channel Description";
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
        channel.setDescription(description);
        // Register the channel with the system; you can't change the importance
        // or other notification behaviors after this
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            int MY_PERMISSIONS_REQUEST_BACKGROUND_LOCATION = 66;
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, MY_PERMISSIONS_REQUEST_BACKGROUND_LOCATION);
        }


        SharedPreferences.Editor editor = preferences.edit();

        TextView location = findViewById(R.id.disk);
        location.setText(preferences.getString("disk", ""));
        location.setOnFocusChangeListener((v, hasFocus) -> {
            editor.putString("disk", location.getText().toString());
            editor.apply();
        });

        TextView ssid = findViewById(R.id.ssid);
        ssid.setText(preferences.getString("ssid", ""));
        ssid.setOnFocusChangeListener((v, hasFocus) -> {
            editor.putString("ssid", ssid.getText().toString());
            editor.apply();

        });

        TextView ip = findViewById(R.id.ipAddress);
        ip.setText(preferences.getString("ip", ""));
        ip.setOnFocusChangeListener((v, hasFocus) -> {
            editor.putString("ip", ip.getText().toString());
            editor.apply();
        });

        TextView userName = findViewById(R.id.sshUsername);
        userName.setText(preferences.getString("userName", ""));
        userName.setOnFocusChangeListener((v, hasFocus) -> {
            editor.putString("userName", userName.getText().toString());
            editor.apply();
        });

        TextView pw = findViewById(R.id.password);
        pw.setText(preferences.getString("pw", ""));
        pw.setOnFocusChangeListener((v, hasFocus) -> {
            editor.putString("pw", pw.getText().toString());
            editor.apply();
        });

        Button button = findViewById(R.id.button);
        button.setOnClickListener(v -> {
            Record r = new Record(userName.getText().toString().trim(), pw.getText().toString().trim(), ip.getText().toString().trim(), ssid.getText().toString().trim(), location.getText().toString().trim());
            backup(r);
        });
    }

    public void backup(Record record) {
        // get ssid of network
        WifiManager wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        String ssid = wifiInfo.getSSID();

        // check if the phone is on the right network
        if (ssid.equals("\"" + record.NetworkSSID + "\"")) {
            ExecutorService executor;

            executor = Executors.newFixedThreadPool(1);

            // Create and submit a network task for execution
            networkTask.setRecord(record);
            executor.submit(networkTask);
        } else {
            // notify the user that they are not on the correct Wi-Fi network
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_background)
                    .setContentTitle("Incorrect Wifi")
                    .setContentText(ssid)
                    .setPriority(NotificationCompat.PRIORITY_MIN);


            notificationManager.notify(0, builder.build());
        }
    }

    public static class Record {
        public String username;
        public String password;
        public String ipAddress;
        public String NetworkSSID;

        public String location;

        public Record(String username, String password, String ipAddress, String networkSSID, String location) {
            this.username = username;
            this.password = password;
            this.ipAddress = ipAddress;
            this.NetworkSSID = networkSSID;
            this.location = location;
        }
    }

}