package com.mikaling.indecision;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class BroadcastService extends Service {
    private final static String TAG = "BroadcastService";

    public static final String COUNTDOWN_BR = "com.mikaling.indecision.countdown_br";
    private static final String CHANNEL_ID = "12";
    Intent bi = new Intent(COUNTDOWN_BR);
    private long millisInFuture;

    CountDownTimer cdt = null;

    @Override
    public void onCreate() {
        super.onCreate();

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        final PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        // Create notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_access_alarm_black_24dp)
                .setContentTitle("Get busy!")
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        Notification notification = builder.build();

        Log.i(TAG, "Starting timer...");



        cdt = new CountDownTimer(3000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                Log.i(TAG, "Countdown seconds remaining: " + millisUntilFinished / 1000);
                bi.putExtra("countdown", millisUntilFinished);
                sendBroadcast(bi);
            }

            @Override
            public void onFinish() {
                Log.i(TAG, "Timer finished");


                // Create notification
                NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_access_alarm_black_24dp)
                        .setContentTitle("Time's up!")
                        .setContentIntent(pendingIntent)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                Notification notification = builder.build();

                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());


                // Show notification when timer completes
                // (notificationId is a unique int for each notification that you must define)
                notificationManager.notify(13, builder.build());

                bi.putExtra("finished", true);
                sendBroadcast(bi);
                SharedPreferences preferences = getApplicationContext().getSharedPreferences("prefs", 0);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean("finished", true);
                editor.commit();
                stopForeground(true);
            }
        }.start();



        // Start foreground service with ID of 12 and a specified notification as the ongoing notification
        startForeground(12, notification);
    }

    @Override
    public void onDestroy() {

        //cdt.cancel();
//        Log.i(TAG, "Timer cancelled");
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        millisInFuture = intent.getLongExtra("length", 10_000);
//        millisInFuture = 10_000;
        Log.i(TAG, "timer length service" + millisInFuture);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }


}
