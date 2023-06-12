package com.Someone117.backup;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.example.backup.R;

public class MainActivity extends AppCompatActivity {
    private static boolean done = true;
    public static final String CHANNEL_ID = "hi";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

        Button button = findViewById(R.id.button);
        button.setOnClickListener(v -> {
            BackupReceiver.manual(this);
        });


        if(done) { // do not reschedule alarm
            BackupScheduler.scheduleBackup(this);
            done = false;
        }
    }

}
