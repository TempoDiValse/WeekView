package com.alamkanak.weekview.sample;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.buttonKotlin).setOnClickListener((View v) -> {
            Intent intent = new Intent(MainActivity.this, KotlinActivity.class);
            startActivity(intent);
        });
    }

}
