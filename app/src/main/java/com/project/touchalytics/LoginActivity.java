package com.project.touchalytics;

import static com.project.touchalytics.Constants.SERVER_BASE_URL;

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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;



/**
 * Single-Activity auth flow:
 *  - Login screen (activity_login.xml)
 *  - Sign Up screen (activity_register.xml)
 *  - Verify screen (activity_verify.xml)
 * We swap layouts inside this activity and wire their events here.
 */


public class LoginActivity extends AppCompatActivity {
    private Integer receivedToken = null;

//    private static final String SERVER_IP = "10.128.13.109"; // <-- Replace with your PC's LAN IP
    private static final int SERVER_PORT = 7000;
    // Common (used per-screen)
    private TextInputLayout emailLayout, passwordLayout, confirmPasswordLayout;
    private TextInputEditText emailInput, passwordInput, confirmPasswordInput;
    private MaterialButton primaryButton;
    private TextView forgotPasswordLink, createAccountLink, loginRedirectLink, privacyNote;

    // Holds the email, password entered during registration (to use on Verify)
    private String pendingEmail = null;
    private String pendingPass = null;

    // Holds the userID and swipeCount received from the server
    private Integer userID = null;
    private Integer swipeCount = 0;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showLoginScreen(); // default entry
        // showRegisterScreen("");
    }


    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            // Fallback: never return the raw password; if hashing fails, send empty hash
            return "";
        }
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

        createAccountLink.setOnClickListener(v -> showRegisterScreen(""));
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

        // Call server to CHECK credentials
        sendLoginCredentialsToServer(email, password);
    }

    // -------------------- REGISTER --------------------

    private void showRegisterScreen(String extraError) {
        setContentView(R.layout.activity_register);

        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("");

        // Register fields

        TextView globalErrorText = findViewById(R.id.globalErrorText);

        emailLayout = findViewById(R.id.emailLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        confirmPasswordLayout = findViewById(R.id.confirmPasswordLayout);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);

        primaryButton = findViewById(R.id.registerButton);
        loginRedirectLink = findViewById(R.id.loginRedirectLink);

        if (extraError != null && !extraError.trim().isEmpty()) {
            globalErrorText.setVisibility(View.VISIBLE);
            globalErrorText.setText(extraError);
        }


        // Submit registration (dummy). After validation, go to Verify screen.
        primaryButton.setOnClickListener(v -> {

            emailLayout.setError(null);
            passwordLayout.setError(null);
            confirmPasswordLayout.setError(null);
            globalErrorText.setVisibility(View.GONE);

            String email = emailInput.getText() == null ? "" : emailInput.getText().toString().trim();
            String password = passwordInput.getText() == null ? "" : passwordInput.getText().toString();
            String confirmPassword = confirmPasswordInput.getText() == null ? "" : confirmPasswordInput.getText().toString();

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

            if (confirmPassword.isEmpty()) {
                confirmPasswordLayout.setError("Password confirmation is required");
                hasError = true;
            } else if (!password.equals(confirmPassword)) {
                confirmPasswordLayout.setError("Passwords do not match");
                hasError = true;
            }

            if (hasError) return;

            // Save for later use (verify + credentials store)
            pendingEmail = email;
            pendingPass = password;

            // Now actually contact the server to check + send email
            sendEmailToServer(pendingEmail);

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

        TextInputLayout codeLayout = findViewById(R.id.codeLayout);
        TextInputEditText codeInput = findViewById(R.id.codeInput);
        primaryButton = findViewById(R.id.continueButton);

        TextView changeEmailLink = findViewById(R.id.changeEmailLink);

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
                sendNewCredentialsToServer(pendingEmail,pendingPass);
            }
            else
            {
                showRegisterScreen("Error: Invalid Token");
            }

        });

        changeEmailLink.setOnClickListener(v -> {
            Snackbar.make(v, "Change email", Snackbar.LENGTH_SHORT).show();
            showRegisterScreen("");
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
        URL url = new URL("http://" + SERVER_BASE_URL + ":5000/listen");
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

    private void sendEmailToServer(String email) {
        new Thread(() -> {
            try {
                Socket socket = new Socket(SERVER_BASE_URL, SERVER_PORT);
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                DataInputStream dis = new DataInputStream(socket.getInputStream());

                // Send the email as raw UTF-8
                byte[] bytes = email.getBytes("UTF-8");
                dos.write(bytes);
                dos.flush();

                // Read response
                byte[] buffer = new byte[1024];
                int read = dis.read(buffer);
                String response = new String(buffer, 0, read, "UTF-8");
                System.out.println("Server Response (email): " + response);

                dos.close();
                dis.close();
                socket.close();

                // Parse JSON and update UI on main thread
                JSONObject json = new JSONObject(response);
                String status = json.optString("status", "");

                runOnUiThread(() -> {
                    if ("exists".equals(status)) {
                        // Email already registered
                        showRegisterScreen("Error: Email already in use");
                    } else if ("ok".equals(status)) {
                        // New email, token sent – go to Verify screen
                        receivedToken = json.optInt("token", 0);
                        Snackbar.make(primaryButton,
                                "Verification code sent to " + pendingEmail,
                                Snackbar.LENGTH_LONG
                        ).show();
                        showVerifyScreen();
                    } else {
                        // DB error or unexpected response
                        showRegisterScreen("Error: Server issue, please try again.");
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        showRegisterScreen("Error: Could not contact server.")
                );
            }
        }).start();
    }

    private void sendNewCredentialsToServer(String email, String password) {
        new Thread(() -> {
            try {
                String hashedPassword = sha256(password);

                String deviceId = android.provider.Settings.Secure.getString(
                        getContentResolver(),
                        android.provider.Settings.Secure.ANDROID_ID
                );

                String payload = "STORE|" + email + "|" + hashedPassword + "|" + deviceId;

                Socket socket = new Socket(SERVER_BASE_URL, SERVER_PORT);
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                DataInputStream dis = new DataInputStream(socket.getInputStream());

                byte[] bytes = payload.getBytes("UTF-8");
                dos.write(bytes);
                dos.flush();

                byte[] buffer = new byte[1024];
                int read = dis.read(buffer);
                String response = new String(buffer, 0, read, "UTF-8");
                System.out.println("Server Response (STORE): " + response);

                dos.close();
                dis.close();
                socket.close();

                // Parse JSON and store userID
                JSONObject json = new JSONObject(response);
                String status = json.optString("status", "");

                if ("stored".equals(status)) {
                    int idFromServer = json.optInt("userID", -1);
                    if (idFromServer != -1) {
                        // store on the activity field
                        userID = idFromServer;
                        System.out.println("Stored userID in LoginActivity: " + userID);
                    }
                } else {
                    System.out.println("STORE failed or returned unexpected status: " + status);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void sendLoginCredentialsToServer(String email, String password) {
        new Thread(() -> {
            try {
                // 1. Hash password with SHA-256 (same as registration)
                String hashedPassword = sha256(password);

                // 2. Grab device ID
                String deviceId = android.provider.Settings.Secure.getString(
                        getContentResolver(),
                        android.provider.Settings.Secure.ANDROID_ID
                );

                // 3. Build CHECK payload
                String payload = "CHECK|" + email + "|" + hashedPassword + "|" + deviceId;

                // 4. Open socket and send
                Socket socket = new Socket(SERVER_BASE_URL, SERVER_PORT);
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                DataInputStream dis = new DataInputStream(socket.getInputStream());

                dos.write(payload.getBytes("UTF-8"));
                dos.flush();

                // 5. Read response
                byte[] buffer = new byte[1024];
                int read = dis.read(buffer);
                String response = new String(buffer, 0, read, "UTF-8");
                System.out.println("Server Response (CHECK): " + response);

                dos.close();
                dis.close();
                socket.close();

                // 6. Parse JSON and update UI
                JSONObject json = new JSONObject(response);
                String status = json.optString("status", "");

                runOnUiThread(() -> {
                    if ("good".equals(status)) {
                        int idFromServer = json.optInt("userID", -1);
                        int featureCount = json.optInt("features", -1);

                        // store in class fields
                        userID = idFromServer;
                        swipeCount = featureCount;

                        // PRINT THEM SO YOU CAN VERIFY
                        System.out.println("Parsed userID from server: " + userID);
                        System.out.println("Parsed swipeCount from server: " + swipeCount);

                        Snackbar.make(primaryButton, "Login successful", Snackbar.LENGTH_SHORT).show();

                        Intent intent = new Intent(this, MainMenuActivity.class);
                        if (userID != null) {
                            intent.putExtra("userID", userID);
                        }
                        intent.putExtra("featureCount", featureCount);
                        startActivity(intent);
                    } else if ("error".equals(status)) {
                        String message = json.optString("message", "Invalid email or password.");
                        passwordLayout.setError("Invalid email or password");
                        Snackbar.make(primaryButton, message, Snackbar.LENGTH_LONG).show();
                    } else {
                        Snackbar.make(primaryButton,
                                "Unexpected server response. Please try again.",
                                Snackbar.LENGTH_LONG
                        ).show();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Snackbar.make(primaryButton, "Login failed: cannot reach server.", Snackbar.LENGTH_LONG).show()
                );
            }
        }).start();
    }


}
