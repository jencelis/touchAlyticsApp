package com.project.touchalytics;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class FruitNinjaActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Placeholder—replace with your game view later
        TextView tv = new TextView(this);
        tv.setText("Fruit Ninja – Coming Soon!");
        tv.setTextSize(24f);
        tv.setPadding(32, 32, 32, 32);
        setContentView(tv);
    }
}
