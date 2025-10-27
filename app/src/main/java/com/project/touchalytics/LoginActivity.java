package com.project.touchalytics;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;

/**
 * Activity for user login.
 * Allows users to input their ID and proceed to the main application.
 */


public class LoginActivity extends AppCompatActivity {

    Button loginButton;
    EditText userIDInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);
        setSupportActionBar(findViewById(R.id.toolbar));

        loginButton = findViewById(R.id.loginButton);
        loginButton.setOnClickListener(v -> login());

        userIDInput = findViewById(R.id.userIDInput);

    }

    /**
     * Handles the login process.
     * Validates the user ID and starts the MainActivity if the ID is valid.
     * Displays error messages using Snackbar if the input is invalid.
     */
    public void login() {
        String userIDText = userIDInput.getText().toString().trim();

        if (userIDText.isEmpty()) {
            Snackbar.make(loginButton,"User ID cannot be empty", Snackbar.LENGTH_SHORT).show();
            return;
        }
        int userID;
        try {
            userID = Integer.parseInt(userIDText);
            if (userID <= 0) {
                Snackbar.make(loginButton,"User ID must be a positive number", Snackbar.LENGTH_SHORT).show();
                return;
            }

        } catch (NumberFormatException e) {
            Snackbar.make(loginButton,"Invalid input. Please enter a valid number.", Snackbar.LENGTH_SHORT).show();
            return;
        }



        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_USER_ID, userID);
        startActivity(intent);
    }
}
