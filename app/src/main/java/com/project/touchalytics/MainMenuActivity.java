package com.project.touchalytics;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;

public class MainMenuActivity extends AppCompatActivity {

    private int userID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        userID = getIntent().getIntExtra("userID", -1);

        MaterialCardView cardNews = findViewById(R.id.cardNews);
        MaterialCardView cardFruit = findViewById(R.id.cardFruit);
        MaterialCardView cardWordle = findViewById(R.id.cardWordle);

        cardNews.setOnClickListener(v -> {
            Intent intent = new Intent(this, NewsMediaActivity.class);
            intent.putExtra("userID", userID);
            startActivity(intent);
        });

        cardFruit.setOnClickListener(v -> {
            Intent intent = new Intent(this, FruitNinjaActivity.class);
            intent.putExtra("userID", userID);
            startActivity(intent);
        });

        cardWordle.setOnClickListener(v -> {
            Intent intent = new Intent(this, WordleActivity.class);
            intent.putExtra("userID", userID);
            startActivity(intent);
        });
    }
}
