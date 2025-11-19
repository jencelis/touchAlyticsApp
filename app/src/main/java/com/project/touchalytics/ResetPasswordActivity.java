package com.project.touchalytics;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ResetPasswordActivity extends AppCompatActivity {

    private TextInputLayout newPasswordLayout;
    private TextInputEditText newPasswordInput;
    private TextInputLayout confirmPasswordLayout;
    private TextInputEditText confirmPasswordInput;
    private MaterialButton submitButton;
    private String email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("");

        newPasswordLayout = findViewById(R.id.newPasswordLayout);
        newPasswordInput = findViewById(R.id.newPasswordInput);
        confirmPasswordLayout = findViewById(R.id.confirmPasswordLayout);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        submitButton = findViewById(R.id.submitButton);

        email = getIntent().getStringExtra("email");

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newPassword = newPasswordInput.getText().toString().trim();
                String confirmPassword = confirmPasswordInput.getText().toString().trim();
                newPasswordLayout.setError(null);
                confirmPasswordLayout.setError(null);

                if (newPassword.isEmpty()) {
                    newPasswordLayout.setError("Please fill out all fields");
                    return;
                }

                if (confirmPassword.isEmpty()) {
                    confirmPasswordLayout.setError("Please fill out all fields");
                    return;
                }

                Pattern pattern = Pattern.compile("[\\p{Punct}]");
                Matcher matcher = pattern.matcher(newPassword);
                boolean containsPunctuation = matcher.find();
                boolean containsUppercase = newPassword.chars().anyMatch(Character::isUpperCase);
                boolean containsNumber = newPassword.chars().anyMatch(Character::isDigit);

                if (newPassword.length() < 8) {
                    newPasswordLayout.setError("Minimum 8 characters");
                    return;
                } else if (!containsPunctuation) {
                    newPasswordLayout.setError("Password must contain special character!");
                    return;
                } else if (!containsUppercase) {
                    newPasswordLayout.setError("Password must contain at least one uppercase!");
                    return;
                } else if (!containsNumber) {
                    newPasswordLayout.setError("Password must contain at least one number!");
                    return;
                }

                if (!newPassword.equals(confirmPassword)) {
                    confirmPasswordLayout.setError("Passwords do not match");
                    return;
                }

                // TODO: Check if the new password is the same as the old one
                boolean isSameAsOld = false; // Dummy value

                if (isSameAsOld) {
                    newPasswordLayout.setError("New password cannot be the same as the old one");
                    return;
                }

                // TODO: Update the password in the database
                Toast.makeText(ResetPasswordActivity.this, "Password reset successfully", Toast.LENGTH_SHORT).show();

                // Redirect to the login page
                Intent intent = new Intent(ResetPasswordActivity.this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });
    }
}
