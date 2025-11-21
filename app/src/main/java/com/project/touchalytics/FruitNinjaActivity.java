package com.project.touchalytics;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class FruitNinjaActivity extends AppCompatActivity implements MainActivity.TouchAnalyticsListener {

    public static final String EXTRA_USER_ID = "userID";
    public static final String LOG_TAG = "FruitNinjaActivity";

    private TextView statusMessage;
    private TextView statusMatchedCount;
    private TextView statusNotMatchedCount;
    private TextView statusStrokeCount;
    private TextView statusStrokeCountMin;

    private MainActivity touchManager;
    private int userID;
    private boolean freeMode = false;
    private GameView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fruit_ninja);

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

        freeMode = getIntent().getBooleanExtra("freeMode", false);
        Log.i(LOG_TAG, "Logged In as UserID: " + userID);

        // Initialize views before the touch manager
        statusMessage = findViewById(R.id.statusMessage);
        statusMatchedCount = findViewById(R.id.statusMatchedCount);
        statusNotMatchedCount = findViewById(R.id.statusNotMatchedCount);
        statusStrokeCount = findViewById(R.id.statusStrokeCount);
        statusStrokeCountMin = findViewById(R.id.statusStrokeCountMin);
        gameView = findViewById(R.id.game_view);

        // Now initialize the touch manager
        touchManager = MainActivity.getInstance();

        if (freeMode) {
            // FREE MODE: no stroke caps, no DB writes
            touchManager.initialize(
                    this,
                    userID,
                    this,
                    0,      // minStrokes ignored in free mode
                    0L,     // start at 0
                    true    // freeMode = true
            );
        } else {
            // TRAINING MODE

            // Total swipe count across ALL phases (if provided by LoginActivity)
            // If launched from NewsMedia directly (no extra), default is -1 and we treat it as "start phase at 0".
            long totalSwipeCount = getIntent().getLongExtra(MainActivity.EXTRA_STROKE_COUNT, -1L);

            long initialPhaseCount;
            if (totalSwipeCount >= 0L) {
                // Fruit Ninja is phase 2:
                //  - phase 1 (NewsMedia) uses the first NEWS_MEDIA_MIN_STROKE_COUNT swipes
                //  - the rest (up to FRUIT_NINJA_MIN_STROKE_COUNT) belong to this phase
                long raw = totalSwipeCount - Constants.NEWS_MEDIA_MIN_STROKE_COUNT; // subtract 30

                initialPhaseCount = Math.max(
                        0L,
                        Math.min(raw, Constants.FRUIT_NINJA_MIN_STROKE_COUNT)
                );
            } else {
                // No global swipe count passed → start this phase at 0 strokes
                initialPhaseCount = 0L;
            }

            touchManager.initialize(
                    this,
                    userID,
                    this,
                    Constants.FRUIT_NINJA_MIN_STROKE_COUNT,
                    initialPhaseCount,
                    false   // freeMode = false
            );
        }


        updateStatusBar(touchManager.getStrokeCount(),
                touchManager.getMatchedCount(),
                touchManager.getNotMatchedCount());

        gameView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                v.performClick();
            }
            touchManager.handleTouchEvent(event);
            gameView.onTouchEvent(event); // Pass the event to the game view
            return true;
        });
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

    @SuppressLint("SetTextI18n")
    private void updateStatusBar(long strokeCount, int matchedCount, int notMatchedCount) {
        if (strokeCount < Constants.FRUIT_NINJA_MIN_STROKE_COUNT) { // Enrollment
            statusMatchedCount.setVisibility(View.GONE);
            statusNotMatchedCount.setVisibility(View.GONE);
            statusStrokeCount.setVisibility(View.VISIBLE);
            statusStrokeCountMin.setVisibility(View.VISIBLE);

            statusMessage.setText("Swipe Enrollment Phase");
            statusStrokeCountMin.setText("/" + Constants.FRUIT_NINJA_MIN_STROKE_COUNT);
            statusStrokeCount.setText(String.valueOf(strokeCount));

        } else if (freeMode) { // Verification
            statusStrokeCount.setVisibility(View.GONE);
            statusStrokeCountMin.setVisibility(View.GONE);
            statusMatchedCount.setVisibility(View.VISIBLE);
            statusNotMatchedCount.setVisibility(View.VISIBLE);

            statusMessage.setText("Swipe Verification Phase");
            statusMatchedCount.setText(String.valueOf(matchedCount));
            statusNotMatchedCount.setText(String.valueOf(notMatchedCount));
        }
        else{
            statusStrokeCount.setVisibility(View.GONE);
            statusStrokeCountMin.setVisibility(View.GONE);
            statusMatchedCount.setVisibility(View.GONE);
            statusNotMatchedCount.setVisibility(View.GONE);
            statusMessage.setText("");
        }
    }

    @Override
    public void onStrokeCountUpdated(long newCount) {
        updateStatusBar(newCount, touchManager.getMatchedCount(), touchManager.getNotMatchedCount());

        if (!freeMode && newCount >= Constants.FRUIT_NINJA_MIN_STROKE_COUNT) {
            showTrainingCompleteDialog();
        }
    }


    private void showTrainingCompleteDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Training Complete")
                .setMessage("You have completed the swipe training for this app. Please proceed to the next training session.")
                .setPositiveButton("Proceed", (dialog, which) -> {
                    Intent intent = new Intent(FruitNinjaActivity.this, WordleActivity.class);
                    intent.putExtra(WordleActivity.EXTRA_USER_ID, userID);
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
    protected void onPause() {
        super.onPause();
        gameView.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        gameView.resume();
    }
}
