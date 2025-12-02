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
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class NewsMediaActivity extends AppCompatActivity implements MainActivity.TouchAnalyticsListener {

    public static final String EXTRA_USER_ID = "userID";
    public static final String LOG_TAG = "NewsMediaActivity";
    private static final String DEFAULT_NEWS_URL = "https://www.clarkson.edu/news-events";
    private FloatingActionButton fabNext;
    private FloatingActionButton fabPrevious;

    private TextView statusMessage;
    private TextView statusMatchedCount;
    private TextView statusNotMatchedCount;
    private TextView statusStrokeCount;
    private TextView statusStrokeCountMin;

    private ImageView statusMatchedSym;
    private ImageView statusNotMatchedSym;

    private WebView webView;

    private MainActivity touchManager;
    private int userID;
    private boolean freeMode = false;

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

        // --- READ FREE MODE FLAG HERE ---
        freeMode = getIntent().getBooleanExtra("freeMode", false);
        Log.i(LOG_TAG, "Logged In as UserID: " + userID + " | freeMode=" + freeMode);


        // Initialize views before the touch manager
        webView = findViewById(R.id.webView);
        statusMessage = findViewById(R.id.statusMessage);
        statusMatchedCount = findViewById(R.id.statusMatchedCount);
        statusNotMatchedCount = findViewById(R.id.statusNotMatchedCount);
        statusStrokeCount = findViewById(R.id.statusStrokeCount);
        statusStrokeCountMin = findViewById(R.id.statusStrokeCountMin);
        statusMatchedSym = findViewById(R.id.statusMatchedSym);
        statusNotMatchedSym = findViewById(R.id.statusNotMatchedSym);
        fabNext = findViewById(R.id.fabNext);
        fabPrevious = findViewById(R.id.fabPrevious);

        // Now initialize the touch manager
        touchManager = MainActivity.getInstance();

        if (freeMode) {
            // FREE MODE: no stroke caps, no DB writes
            touchManager.initialize(
                    this,
                    userID,
                    this,
                    0,      // min strokes ignored in free mode
                    0L,     // start phase at 0
                    true    // free mode ON
            );
        } else {
            // TRAINING MODE
            long totalSwipeCount = getIntent().getLongExtra(MainActivity.EXTRA_STROKE_COUNT, 0L);

            long initialPhaseCount = Math.min(
                    totalSwipeCount,
                    Constants.NEWS_MEDIA_MIN_STROKE_COUNT
            );

            touchManager.initialize(
                    this,
                    userID,
                    this,
                    Constants.NEWS_MEDIA_MIN_STROKE_COUNT,
                    initialPhaseCount,
                    false   // free mode OFF
            );
        }

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
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
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
        if (strokeCount < Constants.NEWS_MEDIA_MIN_STROKE_COUNT) { // Enrollment
            statusMatchedCount.setVisibility(View.GONE);
            statusNotMatchedCount.setVisibility(View.GONE);
            statusNotMatchedSym.setVisibility(View.GONE);
            statusMatchedSym.setVisibility(View.GONE);
            statusStrokeCount.setVisibility(View.VISIBLE);
            statusStrokeCountMin.setVisibility(View.VISIBLE);

            statusMessage.setText("Swipe Enrollment Phase");
            statusStrokeCountMin.setText("/" + Constants.NEWS_MEDIA_MIN_STROKE_COUNT);
            statusStrokeCount.setText(String.valueOf(strokeCount));

        } else if (freeMode){ // Verification
            statusStrokeCount.setVisibility(View.GONE);
            statusStrokeCountMin.setVisibility(View.GONE);
            statusMatchedCount.setVisibility(View.VISIBLE);
            statusNotMatchedCount.setVisibility(View.VISIBLE);
            statusNotMatchedSym.setVisibility(View.VISIBLE);
            statusMatchedSym.setVisibility(View.VISIBLE);

            statusMessage.setText("Swipe Verification Phase");
            statusMatchedCount.setText(String.valueOf(matchedCount));
            statusNotMatchedCount.setText(String.valueOf(notMatchedCount));
        }
        else{
            statusStrokeCount.setVisibility(View.GONE);
            statusStrokeCountMin.setVisibility(View.GONE);
            statusMatchedCount.setVisibility(View.GONE);
            statusNotMatchedCount.setVisibility(View.GONE);
            statusNotMatchedSym.setVisibility(View.GONE);
            statusMatchedSym.setVisibility(View.GONE);
            statusMessage.setText("");
        }
    }

    @Override
    public void onStrokeCountUpdated(long newCount) {
        updateStatusBar(newCount, touchManager.getMatchedCount(), touchManager.getNotMatchedCount());

        if (!freeMode && newCount >= Constants.NEWS_MEDIA_MIN_STROKE_COUNT) {
            showTrainingCompleteDialog();
        }
    }


    private void showTrainingCompleteDialog() {
        // Instead of assuming phase 1 is done, confirm with the server
        MainActivity.getInstance().fetchStoredSwipeCount(userID,
                new MainActivity.SwipeCountCallback() {
                    @Override
                    public void onResult(long totalCount) {
                        runOnUiThread(() -> handleSwipeCountResult(totalCount));
                    }

                    @Override
                    public void onError(String message) {
                        runOnUiThread(() -> {
                            Toast.makeText(NewsMediaActivity.this,
                                    "Could not verify training status: " + message,
                                    Toast.LENGTH_LONG).show();
                        });
                    }
                });
    }

    /**
     * Decide what to do based on the total number of stored swipes.
     *
     * For NewsMedia (phase 1) we only care about meeting N1:
     *  - If totalCount >= N1: phase 1 requirement satisfied → go to FruitNinja.
     *  - If totalCount < N1: show a message and restart NewsMedia to finish training.
     */
    private void handleSwipeCountResult(long totalCount) {
        final int N1 = Constants.NEWS_MEDIA_MIN_STROKE_COUNT; // e.g. 30

        if (totalCount >= N1) {
            // ✅ Phase 1 (NewsMedia) requirement satisfied: proceed to FruitNinja
            String msg = "Please continue with the next training phase.";

            new AlertDialog.Builder(this)
                    .setTitle("News Feed Training Complete")
                    .setMessage(msg)
                    .setPositiveButton("Go to Fruit Ninja", (dialog, which) -> {
                        Intent intent = new Intent(NewsMediaActivity.this, FruitNinjaActivity.class);
                        intent.putExtra(FruitNinjaActivity.EXTRA_USER_ID, userID);

                        // Pass the global total so phase 2 can compute its own initial count
                        intent.putExtra(MainActivity.EXTRA_STROKE_COUNT, totalCount);
                        startActivity(intent);
                        finish();
                    })
                    .setCancelable(false)
                    .show();

            return;
        }

        // Not enough swipes stored on server for this phase
        String msg = "The server reports only " + totalCount +
                " stored training swipes, but " + N1 +
                " are required for the News feed training.\n\n" +
                "You will need to redo this training phase.";

        new AlertDialog.Builder(this)
                .setTitle("News Feed Training Incomplete")
                .setMessage(msg)
                .setPositiveButton("Continue", (dialog, which) -> {
                    Intent intent = getIntent();  // reuse same intent
                    intent.putExtra(MainActivity.EXTRA_STROKE_COUNT, totalCount);

                    finish();
                    startActivity(intent);
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
}

