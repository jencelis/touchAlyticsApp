package com.project.touchalytics;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.AsyncTask;
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
import com.project.touchalytics.data.Features;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Single-Activity auth flow:
 *  - Login screen (activity_login.xml)
 *  - Sign Up screen (activity_register.xml)
 *  - Verify screen (activity_verify.xml)
 * We swap layouts inside this activity and wire their events here.
 */


public class LoginActivity extends AppCompatActivity {
    private Integer receivedToken = null;

    private static final String SERVER_IP = "128.153.221.200"; // <-- Replace with your PC's LAN IP
    private static final int SERVER_PORT = 7000;
    // Common (used per-screen)
    private TextInputLayout emailLayout, passwordLayout;
    private TextInputEditText emailInput, passwordInput;
    private MaterialButton primaryButton;
    private TextView forgotPasswordLink, createAccountLink, loginRedirectLink, privacyNote;

    // Holds the email entered during registration (to use on Verify)
    private String pendingEmail = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        showLoginScreen(); // default entry
        showRegisterScreen();
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

        Pattern pattern = Pattern.compile("[\\p{Punct}]");
        Matcher matcher = pattern.matcher(password);

        boolean containsPunctuation = matcher.find();
        boolean containsUppercase = password.chars().anyMatch(Character::isUpperCase);
        boolean containsNumber = password.chars().anyMatch(Character::isDigit);
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
        } else if (password.length() < 8) {
            passwordLayout.setError("Minimum 8 characters");
            hasError = true;
        } else if (!containsPunctuation) {
            passwordLayout.setError("Password must contain special character!");
            hasError = true;
        }
        else if (!containsUppercase) {
            passwordLayout.setError("Password must contain at least one uppercase!");
            hasError = true;
        }
        else if (!containsNumber) {
            passwordLayout.setError("Password must contain at least one number!");
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

        // Submit registration (dummy). After validation, go to Verify screen.
        primaryButton.setOnClickListener(v -> {
            nameLayout.setError(null);
            emailLayout.setError(null);
            passwordLayout.setError(null);

            String name = nameInput.getText() == null ? "" : nameInput.getText().toString().trim();
            String email = emailInput.getText() == null ? "" : emailInput.getText().toString().trim();
            String password = passwordInput.getText() == null ? "" : passwordInput.getText().toString();

            Pattern pattern = Pattern.compile("[\\p{Punct}]");
            Matcher matcher = pattern.matcher(password);

            boolean containsPunctuation = matcher.find();
            boolean containsUppercase = password.chars().anyMatch(Character::isUpperCase);
            boolean containsNumber = password.chars().anyMatch(Character::isDigit);
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
            } else if (password.length() < 8) {
                passwordLayout.setError("Minimum 8 characters");
                hasError = true;
            } else if (!containsPunctuation) {
                passwordLayout.setError("Password must contain special character!");
                hasError = true;
            }
            else if (!containsUppercase) {
                passwordLayout.setError("Password must contain at least one uppercase!");
                hasError = true;
            }
            else if (!containsNumber) {
                passwordLayout.setError("Password must contain at least one number!");
                hasError = true;
            }

            if (hasError) return;

            // Simulate account creation --> send (dummy) code and move to Verify
            pendingEmail = email;
            Snackbar.make(primaryButton, "Verification code sent to " + pendingEmail, Snackbar.LENGTH_LONG).show();
            sendToPython(pendingEmail);
            showVerifyScreen();
        });

        // Back to login
        loginRedirectLink.setOnClickListener(v -> showLoginScreen());
    }

    // -------------------- VERIFY --------------------

    private void showVerifyScreen() {
        setContentView(R.layout.activity_verify);

        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("");

        TextView verifySubtitle = findViewById(R.id.verifySubtitle);
        if (pendingEmail != null) {
            // Optional: show the email inline in the subtitle
            verifySubtitle.setText(getString(R.string.verify_subtitle_with_email, pendingEmail));
        }



//        Intent intent = new Intent(this, MainActivity.class);
//        intent.putExtra(MainActivity.EXTRA_USER_ID, emailInput.toString());
//        startActivity(intent);
        TextInputLayout codeLayout = findViewById(R.id.codeLayout);
        TextInputEditText codeInput = findViewById(R.id.codeInput);
        primaryButton = findViewById(R.id.continueButton);

        TextView resendCodeLink = findViewById(R.id.resendCodeLink);
        TextView changeEmailLink = findViewById(R.id.changeEmailLink);
        new GetTokenTask().execute();

        // Continue -> validate 6-digit code and proceed to MainMenu
        primaryButton.setOnClickListener(v -> {
            codeLayout.setError(null);
            String code = codeInput.getText() == null ? "" : codeInput.getText().toString().trim();

            if (code.length() != 6 || !code.matches("\\d{6}")) {
                codeLayout.setError("Enter the 6-digit code");
                return;
            } else if (code.equals(String.valueOf(receivedToken))) {
                Snackbar.make(primaryButton, "Verified! Welcome.", Snackbar.LENGTH_SHORT).show();
                startActivity(new Intent(this, MainMenuActivity.class));
            }

        });

        resendCodeLink.setOnClickListener(v ->
                Snackbar.make(v, "Resent code to " + (pendingEmail == null ? "your email" : pendingEmail), Snackbar.LENGTH_SHORT).show());

        changeEmailLink.setOnClickListener(v -> {
            Snackbar.make(v, "Change email", Snackbar.LENGTH_SHORT).show();
            showRegisterScreen();
        });
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

    public int getIntegerFromServer() throws Exception {
        URL url = new URL("http://128.153.221.200:5000/get_token");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
//        conn.setConnectTimeout(5000);
//        conn.setReadTimeout(5000);

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream())
        );

        StringBuilder result = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            result.append(line);
        }

        reader.close();
        conn.disconnect();

        // Parse JSON manually
        JSONObject json = new JSONObject(result.toString());
        return json.getInt("token");
    }


    private class GetTokenTask extends AsyncTask<Void, Void, Integer> {

        @Override
        protected Integer doInBackground(Void... voids) {
            try {
                return getIntegerFromServer();  // Your existing function
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Integer token) {
            if (token != null) {
                System.out.println("Received token: " + token);
                // You can store it for later if needed
                receivedToken = token;
            } else {
                System.out.println("Failed to fetch token from server.");
            }
        }
    }

    private void sendToPython(String userInfo) {
        new Thread(() -> {
            try {
                Socket socket = new Socket(SERVER_IP, SERVER_PORT);
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                DataInputStream dis = new DataInputStream(socket.getInputStream());


                // Send raw UTF-8 bytes
                byte[] bytes = userInfo.getBytes("UTF-8");
                dos.write(bytes);  // no writeUTF()
                dos.flush();

                // Optionally send a newline or delimiter if you want to read multiple messages
                // dos.write("\n".getBytes("UTF-8"));
                // dos.flush();

                // Read response (if your server sends one)
                byte[] buffer = new byte[1024];
                int read = dis.read(buffer);
                String response = new String(buffer, 0, read, "UTF-8");
                System.out.println("Server Response: " + response);

                dos.close();
                dis.close();
                socket.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
