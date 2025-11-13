package com.project.touchalytics;

import android.content.Intent;
import android.content.SharedPreferences;
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

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout emailLayout, passwordLayout;
    private TextInputEditText emailInput, passwordInput;
    private MaterialButton primaryButton;
    private TextView forgotPasswordLink, createAccountLink, loginRedirectLink, privacyNote;
    private String pendingEmail = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showLoginScreen();
    }

    private void showLoginScreen() {
        setContentView(R.layout.activity_login);

        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("");

        emailLayout = findViewById(R.id.emailLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        primaryButton = findViewById(R.id.loginButton);
        forgotPasswordLink = findViewById(R.id.forgotPasswordLink);
        createAccountLink = findViewById(R.id.createAccountLink);
        privacyNote = findViewById(R.id.privacyNote);

        primaryButton.setOnClickListener(v -> login());
        createAccountLink.setOnClickListener(v -> showRegisterScreen());
        forgotPasswordLink.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });
        makePrivacySpan(privacyNote);
    }

    private void login() {
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

        Snackbar.make(primaryButton, "Logging in…", Snackbar.LENGTH_SHORT).show();
        MainActivity.getInstance().reset();
        clearTrainingStatus();
        checkTrainingStatus(123);
    }

    private void showRegisterScreen() {
        setContentView(R.layout.activity_register);

        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("");

        TextInputLayout nameLayout = findViewById(R.id.nameLayout);
        TextInputEditText nameInput = findViewById(R.id.nameInput);
        emailLayout = findViewById(R.id.emailLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        TextInputLayout confirmPasswordLayout = findViewById(R.id.confirmPasswordLayout);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        TextInputEditText confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        primaryButton = findViewById(R.id.registerButton);
        loginRedirectLink = findViewById(R.id.loginRedirectLink);

        primaryButton.setOnClickListener(v -> {
            nameLayout.setError(null);
            emailLayout.setError(null);
            passwordLayout.setError(null);
            confirmPasswordLayout.setError(null);

            String name = nameInput.getText() == null ? "" : nameInput.getText().toString().trim();
            String email = emailInput.getText() == null ? "" : emailInput.getText().toString().trim();
            String password = passwordInput.getText() == null ? "" : passwordInput.getText().toString();
            String confirmPassword = confirmPasswordInput.getText() == null ? "" : confirmPasswordInput.getText().toString();

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
            if (confirmPassword.isEmpty()) {
                confirmPasswordLayout.setError("Confirm your password");
                hasError = true;
            } else if (!password.equals(confirmPassword)) {
                confirmPasswordLayout.setError("Passwords do not match");
                hasError = true;
            }

            if (hasError) return;

            pendingEmail = email;
            Snackbar.make(primaryButton, "Verification code sent to " + pendingEmail, Snackbar.LENGTH_LONG).show();
            showVerifyScreen();
        });

        loginRedirectLink.setOnClickListener(v -> showLoginScreen());
    }

    private void showVerifyScreen() {
        setContentView(R.layout.activity_verify);

        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("");

        TextView verifySubtitle = findViewById(R.id.verifySubtitle);
        if (pendingEmail != null) {
            verifySubtitle.setText(getString(R.string.verify_subtitle_with_email, pendingEmail));
        }

        TextInputLayout codeLayout = findViewById(R.id.codeLayout);
        TextInputEditText codeInput = findViewById(R.id.codeInput);
        primaryButton = findViewById(R.id.continueButton);
        TextView resendCodeLink = findViewById(R.id.resendCodeLink);
        TextView changeEmailLink = findViewById(R.id.changeEmailLink);

        primaryButton.setOnClickListener(v -> {
            codeLayout.setError(null);
            String code = codeInput.getText() == null ? "" : codeInput.getText().toString().trim();

            if (code.length() != 6 || !code.matches("\\d{6}")) {
                codeLayout.setError("Enter the 6-digit code");
                return;
            }

            Snackbar.make(primaryButton, "Verified! Welcome.", Snackbar.LENGTH_SHORT).show();
            MainActivity.getInstance().reset();
            clearTrainingStatus();
            checkTrainingStatus(123);
        });

        resendCodeLink.setOnClickListener(v ->
                Snackbar.make(v, "Resent code to " + (pendingEmail == null ? "your email" : pendingEmail), Snackbar.LENGTH_SHORT).show());

        changeEmailLink.setOnClickListener(v -> {
            Snackbar.make(v, "Change email", Snackbar.LENGTH_SHORT).show();
            showRegisterScreen();
        });
    }

    private void clearTrainingStatus() {
        SharedPreferences prefs = getSharedPreferences("training_status", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("training_complete");
        editor.apply();
    }

    private void checkTrainingStatus(int userId) {
        SharedPreferences prefs = getSharedPreferences("training_status", MODE_PRIVATE);
        boolean trainingComplete = prefs.getBoolean("training_complete", false);

        Intent intent;
        if (trainingComplete) {
            intent = new Intent(this, MainMenuActivity.class);
        } else {
            intent = new Intent(this, NewsMediaActivity.class);
        }
        intent.putExtra(NewsMediaActivity.EXTRA_USER_ID, userId);
        startActivity(intent);
        finish();
    }

    private void makePrivacySpan(TextView targetView) {
        String full = getString(R.string.privacy_sentence);
        String linkText = "Privacy Policy";
        int start = full.indexOf(linkText);
        int end = start + linkText.length();

        SpannableString ss = new SpannableString(full);
        if (start >= 0) {
            ClickableSpan click = new ClickableSpan() {
                @Override public void onClick(@NonNull View widget) {
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
