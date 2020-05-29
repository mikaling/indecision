package com.mikaling.indecision;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";
    private static final String CHANNEL_ID = "12";
    private TextView timeRemaining;
    private TextInputLayout addNewTask;
    private CountDownTimer countDownTimer;
    private boolean running;
    // Timer length in milliseconds
    private long milliseconds = 30000;
    private long millisecondsLeft;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        timeRemaining = findViewById(R.id.timeRemaining);
        addNewTask = findViewById(R.id.addNewTask);
        addNewTask.setEndIconOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Add task to database and show in RecyclerView
                Toast.makeText(MainActivity.this, "End icon clicked", Toast.LENGTH_SHORT).show();
            }
        });

//        startService(new Intent(this, BroadcastService.class));
//        Log.i(TAG, "Started service");

        createNotificationChannel();
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "received broadcast");
            // Call method to update timer TetView when broadcasts are received
            updateCountDownText(intent);
        }
    };


    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(broadcastReceiver, new IntentFilter(BroadcastService.COUNTDOWN_BR));
        Log.i(TAG, "Register broadcast receiver");
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(broadcastReceiver);
        Log.i(TAG, "Unregistered broadcast receiver");
    }

    @Override
    protected void onStop() {
        try {
            unregisterReceiver(broadcastReceiver);
        } catch (Exception e) {
            // Receiver was probably stopped in onPause()
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
//        stopService(new Intent(this, BroadcastService.class));
//        Log.i(TAG, "Stopped service");
        super.onDestroy();
    }

    private void updateCountDownText(Intent intent) {
        if (intent.getExtras() != null) {
            long millisUntilFinished = intent.getLongExtra("countdown", 0);
            int minutes = (int) (millisUntilFinished / 60000);
            int seconds = (int) (millisUntilFinished / 1000);
            String time = String.format("%02d:%02d", minutes, seconds);
            timeRemaining.setText(time);
            Log.i(TAG, "updated UI");
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void startTimer(View view) {
        stopService(new Intent(this, BroadcastService.class));
        startForegroundService(new Intent(this, BroadcastService.class));
        Log.i(TAG, "Started service");
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

}
