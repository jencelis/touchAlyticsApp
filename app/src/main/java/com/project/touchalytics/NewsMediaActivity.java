package com.project.touchalytics;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class NewsMediaActivity extends AppCompatActivity implements MainActivity.TouchAnalyticsListener {

    public static final String EXTRA_USER_ID = "EXTRA_USER_ID";
    public static final String LOG_TAG = "NewsMediaActivity";
    private static final String DEFAULT_NEWS_URL = "https://www.clarkson.edu/news-events";
    private static final int NEWS_MEDIA_MIN_STROKE_COUNT = 30;

    private FloatingActionButton fabNext;
    private FloatingActionButton fabPrevious;

    private TextView statusMessage;
    private TextView statusMatchedCount;
    private TextView statusNotMatchedCount;
    private TextView statusStrokeCount;
    private TextView statusStrokeCountMin;

    private WebView webView;

    private MainActivity touchManager;
    private int userID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news_media);

        if (savedInstanceState != null) {
            userID = savedInstanceState.getInt(EXTRA_USER_ID, -1);
        } else {
            userID = getIntent().getIntExtra(EXTRA_USER_ID, -1);
        }

        if (userID < 0) {
            Toast.makeText(this, "Invalid User ID. Closing application.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Log.i(LOG_TAG, "Logged In as UserID: " + userID);

        // Initialize views before the touch manager
        webView = findViewById(R.id.webView);
        statusMessage = findViewById(R.id.statusMessage);
        statusMatchedCount = findViewById(R.id.statusMatchedCount);
        statusNotMatchedCount = findViewById(R.id.statusNotMatchedCount);
        statusStrokeCount = findViewById(R.id.statusStrokeCount);
        statusStrokeCountMin = findViewById(R.id.statusStrokeCountMin);
        fabNext = findViewById(R.id.fabNext);
        fabPrevious = findViewById(R.id.fabPrevious);

        // Now initialize the touch manager
        touchManager = MainActivity.getInstance();
        touchManager.initialize(this, userID, this, NEWS_MEDIA_MIN_STROKE_COUNT);

        initializeWebView();

        updateStatusBar(touchManager.getStrokeCount(), touchManager.getMatchedCount(), touchManager.getNotMatchedCount());

        fabNext.setEnabled(webView.canGoForward());
        fabPrevious.setEnabled(webView.canGoBack());

        fabNext.setOnClickListener(v -> webView.goForward());
        fabPrevious.setOnClickListener(v -> webView.goBack());
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(EXTRA_USER_ID, userID);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        userID = savedInstanceState.getInt(EXTRA_USER_ID);
    }

    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    private void initializeWebView() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon){
                super.onPageStarted(view, url, favicon);
                fabNext.setEnabled(webView.canGoForward());
                fabPrevious.setEnabled(webView.canGoBack());
            }
        });

        webView.loadUrl(DEFAULT_NEWS_URL);

        webView.setOnTouchListener((v, event) -> {
            touchManager.handleTouchEvent(event);
            return false;
        });
    }

    @SuppressLint("SetTextI18n")
    private void updateStatusBar(long strokeCount, int matchedCount, int notMatchedCount) {
        if (strokeCount < NEWS_MEDIA_MIN_STROKE_COUNT) { // Enrollment
            statusMatchedCount.setVisibility(View.GONE);
            statusNotMatchedCount.setVisibility(View.GONE);
            statusStrokeCount.setVisibility(View.VISIBLE);
            statusStrokeCountMin.setVisibility(View.VISIBLE);

            statusMessage.setText("Swipe Enrollment Phase");
            statusStrokeCountMin.setText("/" + NEWS_MEDIA_MIN_STROKE_COUNT);
            statusStrokeCount.setText(String.valueOf(strokeCount));

        } else { // Verification
            statusStrokeCount.setVisibility(View.GONE);
            statusStrokeCountMin.setVisibility(View.GONE);
            statusMatchedCount.setVisibility(View.VISIBLE);
            statusNotMatchedCount.setVisibility(View.VISIBLE);

            statusMessage.setText("Swipe Verification Phase");
            statusMatchedCount.setText(String.valueOf(matchedCount));
            statusNotMatchedCount.setText(String.valueOf(notMatchedCount));
        }
    }

    @Override
    public void onStrokeCountUpdated(long newCount) {
        updateStatusBar(newCount, touchManager.getMatchedCount(), touchManager.getNotMatchedCount());
        if (newCount >= NEWS_MEDIA_MIN_STROKE_COUNT) {
            showTrainingCompleteDialog();
        }
    }

    private void showTrainingCompleteDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Training Complete")
                .setMessage("You have completed the swipe training for this app. Please proceed to the next training session.")
                .setPositiveButton("Proceed", (dialog, which) -> {
                    Intent intent = new Intent(NewsMediaActivity.this, FruitNinjaActivity.class);
                    intent.putExtra(NewsMediaActivity.EXTRA_USER_ID, userID);
                    startActivity(intent);
                    finish();
                })
                .setCancelable(false)
                .show();
    }


    @Override
    public void onVerificationResult(boolean matched, int matchedCount, int notMatchedCount) {
        updateStatusBar(touchManager.getStrokeCount(), matchedCount, notMatchedCount);
        String message = matched ? "User matched!" : "User not matched!";
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onTapCountUpdated(long newCount) {
        // Not used in this activity
    }

    @Override
    public void onTapVerificationResult(boolean matched, int matchedCount, int notMatchedCount) {
        // Not used in this activity
    }
}
