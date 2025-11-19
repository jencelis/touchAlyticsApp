package com.project.touchalytics;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class EnterSecurityCodeActivity extends AppCompatActivity {

    private TextInputLayout codeLayout;
    private TextInputEditText codeInput;
    private MaterialButton submitButton;
    private String email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enter_security_code);

        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("");

        codeLayout = findViewById(R.id.codeLayout);
        codeInput = findViewById(R.id.codeInput);
        submitButton = findViewById(R.id.submitButton);

        email = getIntent().getStringExtra("email");

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String securityCode = codeInput.getText().toString().trim();
                codeLayout.setError(null);

                if (securityCode.length() != 6) {
                    codeLayout.setError("Please enter a 6-digit security code");
                    return;
                }

                // TODO: Verify the security code from the server
                boolean isCodeCorrect = securityCode.equals("123456"); // Dummy check

                if (isCodeCorrect) {
                    Intent intent = new Intent(EnterSecurityCodeActivity.this, ResetPasswordActivity.class);
                    intent.putExtra("email", email);
                    startActivity(intent);
                } else {
                    codeLayout.setError("Incorrect security code");
                    // Redirect back to the previous page as requested
                    Toast.makeText(EnterSecurityCodeActivity.this, "Incorrect security code, redirecting...", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(EnterSecurityCodeActivity.this, ForgotPasswordActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        });
    }
}
