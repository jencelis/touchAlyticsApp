package com.project.touchalytics;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;

public class MainMenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        MaterialCardView cardNews = findViewById(R.id.cardNews);
        MaterialCardView cardFruit = findViewById(R.id.cardFruit);
        MaterialCardView cardWordle = findViewById(R.id.cardWordle);

        cardNews.setOnClickListener(v ->
                startActivity(new Intent(this, NewsMediaActivity.class)));

        cardFruit.setOnClickListener(v ->
                startActivity(new Intent(this, FruitNinjaActivity.class)));

        cardWordle.setOnClickListener(v ->
                startActivity(new Intent(this, WordleActivity.class)));
    }
}

