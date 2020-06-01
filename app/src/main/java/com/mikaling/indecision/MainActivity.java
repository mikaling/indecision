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

    public static final String TAG = "MainActivity";
    private static final String CHANNEL_ID = "12"; // Notification Channel ID
    private TextView timeRemaining;
    private TextView chosenTask;
    private TextInputLayout addNewTask;
    private TextInputEditText addNewTaskEditText;
    private FloatingActionButton fab;
    // Timer length in milliseconds
    private long milliseconds = 30000;
    private long millisecondsLeft;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TaskDBHelper taskDBHelper = new TaskDBHelper(this);
        sqLiteDatabase = taskDBHelper.getWritableDatabase();

        fab = findViewById(R.id.floatingActionButton);
        // Disable FAB clicks if there are no tasks
        disableButton();

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
                removeItem(viewHolder.itemView.getTag());
            }
        }).attachToRecyclerView(recyclerView);

        timeRemaining = findViewById(R.id.timeRemaining);
        chosenTask = findViewById(R.id.chosenTask);
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

//        startService(new Intent(this, BroadcastService.class));
//        Log.i(TAG, "Started service");

        createNotificationChannel();
    }

    private void disableButton() {
        Cursor cursor = getAllItems();
        if (cursor.getCount() == 0){
            Log.i(TAG, "count = " + cursor.getCount());
            fab.setEnabled(false);
        }
    }

    private void removeItem(Object id) {
        Log.i(TAG, "ID = "+id);
        id = (int) id;
        sqLiteDatabase.delete(TaskContract.TaskEntry.TABLE_NAME,
                TaskContract.TaskEntry._ID + "=" + id,
                null);
        taskAdapter.swapCursor(getAllItems());
        disableButton();
    }

//    private void removeItem(int id) {
//        sqLiteDatabase.delete(TaskContract.TaskEntry.TABLE_NAME,
//                TaskContract.TaskEntry._ID + "=" + id,
//                null);
//        taskAdapter.swapCursor(getAllItems());
//        disableButton();
//    }

    private void addToDatabase() {
        // Add task to database and show in RecyclerView
//        Toast.makeText(MainActivity.this, "End icon clicked", Toast.LENGTH_SHORT).show();

        if (addNewTaskEditText.getText().toString().trim().length() == 0) {
            return; // Do not add to database if EditText is empty
        }

        String taskName = addNewTaskEditText.getText().toString();
        ContentValues cv = new ContentValues();
        cv.put(TaskContract.TaskEntry.COLUMN_NAME, taskName);
        sqLiteDatabase.insert(TaskContract.TaskEntry.TABLE_NAME, null, cv);
        taskAdapter.swapCursor(getAllItems());

        addNewTaskEditText.getText().clear();
        fab.setEnabled(true);
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
                removeItem(chosenTask.getTag());
                chosenTask.setText("");
            }
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

        chooseRandomTask();
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

        // Returns rows in descending order of date created, with first row having the largest id
        Cursor cursor = getAllItems();
        // Move cursor to first row to get largest ID
        cursor.moveToFirst();
        int randomMax = cursor.getInt(cursor.getColumnIndex(TaskContract.TaskEntry._ID));

        // Move cursor to last row to get smallest ID
        cursor.moveToLast();
        int randomMin = cursor.getInt(cursor.getColumnIndex(TaskContract.TaskEntry._ID));
        Random random = new Random();
        int id = random.nextInt(randomMax + 1 - randomMin) + randomMin;


        TaskDBHelper taskDBHelper = new TaskDBHelper(this);
        sqLiteDatabase = taskDBHelper.getReadableDatabase();
        Cursor cursor1 = sqLiteDatabase.rawQuery(
                "SELECT " + TaskContract.TaskEntry.COLUMN_NAME +
                " FROM " + TaskContract.TaskEntry.TABLE_NAME + " WHERE " + TaskContract.TaskEntry._ID +
                " = '" + id + "'", null);
        cursor1.moveToFirst();


        chosenTask.setText(cursor1.getString(0));
        chosenTask.setTag(id);
    }

    private void chooseRandomTime() {

    }

}
