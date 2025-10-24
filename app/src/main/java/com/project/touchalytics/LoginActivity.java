package com.project.touchalytics;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Patterns;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Single-Activity auth flow:
 *  - Login screen (activity_login.xml)
 *  - Sign Up screen (activity_register.xml)
 * We swap layouts inside this activity and wire their events here.
 */
public class LoginActivity extends AppCompatActivity {

    // Common (used per-screen)
    private TextInputLayout emailLayout, passwordLayout;
    private TextInputEditText emailInput, passwordInput;
    private MaterialButton primaryButton;
    private TextView forgotPasswordLink, createAccountLink, loginRedirectLink, privacyNote;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showLoginScreen(); // default entry
    }

    // -------------------- LOGIN --------------------

    private void showLoginScreen() {
        setContentView(R.layout.activity_login);

        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("");

        emailLayout = findViewById(R.id.emailLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);

        primaryButton = findViewById(R.id.loginButton);
        primaryButton.setOnClickListener(v -> login());

        forgotPasswordLink = findViewById(R.id.forgotPasswordLink);
        createAccountLink = findViewById(R.id.createAccountLink);
        privacyNote = findViewById(R.id.privacyNote);

        // Inline clickable "Privacy Policy"
        makePrivacySpan(privacyNote);

        createAccountLink.setOnClickListener(v -> showRegisterScreen());
        forgotPasswordLink.setOnClickListener(v ->
                Snackbar.make(v, "Forgot password tapped", Snackbar.LENGTH_SHORT).show());
    }

    private void login() {
        // clear old errors
        emailLayout.setError(null);
        passwordLayout.setError(null);

        String email = emailInput.getText() == null ? "" : emailInput.getText().toString().trim();
        String password = passwordInput.getText() == null ? "" : passwordInput.getText().toString();

        boolean hasError = false;

        if (email.isEmpty()) {
            emailLayout.setError("Email is required");
            hasError = true;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError("Enter a valid email");
            hasError = true;
        }

        if (password.isEmpty()) {
            passwordLayout.setError("Password is required");
            hasError = true;
        } else if (password.length() < 6) {
            passwordLayout.setError("Minimum 6 characters");
            hasError = true;
        }

        if (hasError) return;

        // TODO: Replace with real auth call. For now, proceed to MainMenu.
        Snackbar.make(primaryButton, "Logging in…", Snackbar.LENGTH_SHORT).show();
        startActivity(new Intent(this, MainMenuActivity.class));
    }

    // -------------------- REGISTER --------------------

    private void showRegisterScreen() {
        setContentView(R.layout.activity_register);

        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("");

        // Register fields
        TextInputLayout nameLayout = findViewById(R.id.nameLayout);
        TextInputEditText nameInput = findViewById(R.id.nameInput);

        emailLayout = findViewById(R.id.emailLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);

        primaryButton = findViewById(R.id.registerButton);
        loginRedirectLink = findViewById(R.id.loginRedirectLink);

        // Submit registration (dummy). After validation, return to Login screen.
        primaryButton.setOnClickListener(v -> {
            nameLayout.setError(null);
            emailLayout.setError(null);
            passwordLayout.setError(null);

            String name = nameInput.getText() == null ? "" : nameInput.getText().toString().trim();
            String email = emailInput.getText() == null ? "" : emailInput.getText().toString().trim();
            String password = passwordInput.getText() == null ? "" : passwordInput.getText().toString();

            boolean hasError = false;

            if (name.isEmpty()) {
                nameLayout.setError("Name is required");
                hasError = true;
            }

            if (email.isEmpty()) {
                emailLayout.setError("Email is required");
                hasError = true;
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailLayout.setError("Enter a valid email");
                hasError = true;
            }

            if (password.isEmpty()) {
                passwordLayout.setError("Password is required");
                hasError = true;
            } else if (password.length() < 6) {
                passwordLayout.setError("Minimum 6 characters");
                hasError = true;
            }

            if (hasError) return;

            // Simulate successful account creation
            Snackbar.make(primaryButton, "Account created! Please log in.", Snackbar.LENGTH_LONG).show();
            showLoginScreen(); // Return to login so the user can sign in
        });

        // Back to login
        loginRedirectLink.setOnClickListener(v -> showLoginScreen());
    }

    // -------------------- Helpers --------------------

    private void makePrivacySpan(TextView targetView) {
        String full = getString(R.string.privacy_sentence);
        String linkText = "Privacy Policy";
        int start = full.indexOf(linkText);
        int end = start + linkText.length();

        SpannableString ss = new SpannableString(full);
        if (start >= 0) {
            ClickableSpan click = new ClickableSpan() {
                @Override public void onClick(@NonNull View widget) {
                    // TODO: open privacy page/activity here (if we want, otherwise just for show)
                    Snackbar.make(widget, "Privacy Policy tapped", Snackbar.LENGTH_SHORT).show();
                }
            };
            ss.setSpan(click, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ss.setSpan(new ForegroundColorSpan(
                    ContextCompat.getColor(this, R.color.hub_primary)
            ), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ss.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        targetView.setText(ss);
        targetView.setMovementMethod(LinkMovementMethod.getInstance());
    }
}
