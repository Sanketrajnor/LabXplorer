package com.example.labxplorer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button visualizeButton = findViewById(R.id.btnVisualize);
        visualizeButton.setOnClickListener(v -> {
            try {
                // Navigate to AR Visualization Activity
                Intent intent = new Intent(MainActivity.this, CaptureActivity.class);
                startActivity(intent);
            } catch (Exception e) {
                Log.e("MainActivity", "Error starting AR Visualization", e);
            }
        });
    }
}
