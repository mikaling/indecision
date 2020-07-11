package com.mikaling.indecision;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Random;

public class MainActivity extends AppCompatActivity {


    private SQLiteDatabase sqLiteDatabase;
    private TaskAdapter taskAdapter;
    SharedPreferences preferences;

    public static final String TAG = "MainActivity";
    private static final String CHANNEL_ID = "12"; // Notification Channel ID
    private TextView timeRemaining;
    private TextView chosenTask;
    private TextInputLayout addNewTask;
    private TextInputEditText addNewTaskEditText;
    private FloatingActionButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preferences = getApplicationContext().getSharedPreferences("prefs", 0);
        TaskDBHelper taskDBHelper = new TaskDBHelper(this);
        sqLiteDatabase = taskDBHelper.getWritableDatabase();

        fab = findViewById(R.id.floatingActionButton);
        // Disable FAB clicks if there are no tasks
        disableButton();

        timeRemaining = findViewById(R.id.timeRemaining);
        chosenTask = findViewById(R.id.chosenTask);


        RecyclerView recyclerView = findViewById(R.id.recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        taskAdapter = new TaskAdapter(this, getAllItems());
        recyclerView.setAdapter(taskAdapter);

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                removeItem((long) viewHolder.itemView.getTag());
            }
        }).attachToRecyclerView(recyclerView);

        addNewTaskEditText = findViewById(R.id.addNewTaskEditText);

        addNewTask = findViewById(R.id.addNewTask);
        addNewTask.setEndIconOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addToDatabase();
            }
        });

        addNewTaskEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    addToDatabase();
                    handled = true;
                }
                return handled;
            }
        });


        createNotificationChannel();
    }

    private void disableButton() {
        Cursor cursor = getAllItems();
        fab.setEnabled(true);
        Intent i = getIntent();

//        if (preferences.getBoolean("finished", false)) {
//            fab.setEnabled(true);
//        }

        if ((cursor.getCount() == 0) || !(preferences.getBoolean("finished", false))) {
            fab.setEnabled(false);
        }
    }

    private void removeItem(long id) {
        sqLiteDatabase.delete(TaskContract.TaskEntry.TABLE_NAME,
                TaskContract.TaskEntry._ID + "=" + id,
                null);
        taskAdapter.swapCursor(getAllItems());
        disableButton();
    }

    private void removeItem(int id) {
        sqLiteDatabase.delete(TaskContract.TaskEntry.TABLE_NAME,
                TaskContract.TaskEntry._ID + "=" + id,
                null);
        taskAdapter.swapCursor(getAllItems());
        disableButton();
    }

    private void addToDatabase() {
        // Add task to database and show in RecyclerView

        if (addNewTaskEditText.getText().toString().trim().length() == 0) {
            return; // Do not add to database if EditText is empty
        }

        String taskName = addNewTaskEditText.getText().toString();
        ContentValues cv = new ContentValues();
        cv.put(TaskContract.TaskEntry.COLUMN_NAME, taskName);
        sqLiteDatabase.insert(TaskContract.TaskEntry.TABLE_NAME, null, cv);
        taskAdapter.swapCursor(getAllItems());

        addNewTaskEditText.getText().clear();
//        fab.setEnabled(true);
            disableButton();
    }


    private Cursor getAllItems() {
        return sqLiteDatabase.query(
                TaskContract.TaskEntry.TABLE_NAME,
                null,
                null,
                null,
                null,
                null,
                TaskContract.TaskEntry.COLUMN_TIMESTAMP + " DESC"
        );
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getBooleanExtra("finished", false)) {
                removeItem((int) chosenTask.getTag());
                chosenTask.setText("");
            }
            Log.i(TAG, "received broadcast");
            // Call method to update timer TetView when broadcasts are received
            updateCountDownText(intent);

//            if (preferences.getBoolean("finished", false)) {
//                fab.setEnabled(true);
//            }
        }
    };


    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(broadcastReceiver, new IntentFilter(BroadcastService.COUNTDOWN_BR));
        Log.i(TAG, "Register broadcast receiver");

        if (preferences.contains("chosenTask")) {
            chosenTask.setText(preferences.getString("chosenTask", null));
            chosenTask.setTag(preferences.getInt("chosenTaskId", 0));
        }

        if (preferences.getBoolean("finished", false)) {
            removeItem((int) chosenTask.getTag());
            chosenTask.setText("");
            fab.setEnabled(true);
        }

        disableButton();

    }

    @Override
    protected void onPause() {
        super.onPause();


        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("chosenTask", chosenTask.getText().toString());
        editor.putInt("chosenTaskId", (int) chosenTask.getTag());
        editor.commit();

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
        Log.i(TAG, "stopped");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
//        stopService(new Intent(this, BroadcastService.class));
        Log.i(TAG, "destroyed");
        super.onDestroy();
    }


    private void updateCountDownText(Intent intent) {
        if (intent.getExtras() != null) {
            long millisUntilFinished = intent.getLongExtra("countdown", 0);
            int hours = (int) (millisUntilFinished / 3_600_000);
            int minutes = (int) ((millisUntilFinished / 60_000) % 60);
            int seconds = (int) ((millisUntilFinished / 1000) % 60);
            String time = String.format("%01d:%02d:%02d", hours, minutes, seconds);
            timeRemaining.setText(time);
            Log.i(TAG, "updated UI");
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void startTimer(View view) {
        Intent serviceIntent = new Intent(this, BroadcastService.class);

        chooseRandomTask();
        serviceIntent.putExtra("chosenTask", chosenTask.getText());
        stopService(serviceIntent);
        startForegroundService(serviceIntent);
        Log.i(TAG, "Started service");

        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("finished", false);
        editor.commit();

        fab.setEnabled(false);
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

    private void chooseRandomTask() {

        TaskDBHelper taskDBHelper = new TaskDBHelper(this);
        sqLiteDatabase = taskDBHelper.getReadableDatabase();
        Cursor cursor1 = sqLiteDatabase.rawQuery(
                "SELECT " + TaskContract.TaskEntry.COLUMN_NAME +
                        ", " + TaskContract.TaskEntry._ID +
                        " FROM " + TaskContract.TaskEntry.TABLE_NAME, null);

        int randomMax = cursor1.getCount() - 1;
        int randomMin = 0;
        Random random = new Random();
        int row = random.nextInt(randomMax + 1 - randomMin) + randomMin;


        if (cursor1.moveToPosition(row)) {
            chosenTask.setText(cursor1.getString(0));
            int id = cursor1.getInt(1);
            chosenTask.setTag(id);
        }


    }
}
