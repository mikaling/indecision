package com.mikaling.indecision;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class MainActivity extends AppCompatActivity {

    private TextView timeRemaining;
    private TextInputLayout addNewTask;
    private CountDownTimer countDownTimer;
    private long milliseconds = 30000;
    private long seconds;
    private long minutes;

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

    }

    public void startTimer(View view) {
        countDownTimer = new CountDownTimer(milliseconds, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                minutes = millisUntilFinished / 60000;
                seconds = millisUntilFinished / 1000;
                String time = String.format("%02d:%02d", minutes, seconds);
                timeRemaining.setText(time);
            }

            @Override
            public void onFinish() {

            }
        }.start();
    }
}
