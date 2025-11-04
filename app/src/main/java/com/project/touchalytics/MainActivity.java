package com.project.touchalytics;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.JsonObject;
import com.project.touchalytics.data.Features;
import com.project.touchalytics.data.Stroke;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.MotionEvent;


import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Main activity of the application.
 * Handles user interaction with a WebView, collects touch data,
 * and communicates with Firebase and a backend server for enrollment and verification.
 */
public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_USER_ID = "EXTRA_USER_ID";
    public static final String LOG_TAG = "MainActivity";

    private FloatingActionButton fabNext;
    private FloatingActionButton fabPrevious;

    private TextView statusMessage;
    private TextView statusMatchedCount;
    private TextView statusNotMatchedCount;
    private TextView statusStrokeCount;
    private TextView statusStrokeCountMin;

    private WebView webView;

    private DatabaseReference database;
    private RetrofitClient.ApiService apiService;

    private Integer userID;
    private long strokeCount = 0L;
    private Stroke currentStroke;

    private int matchedCount = 0;
    private int notMatchedCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(savedInstanceState != null) {
            userID = savedInstanceState.getInt(EXTRA_USER_ID);
        } else {
            userID = getIntent().getIntExtra(EXTRA_USER_ID, -1);
        }
        if (userID < 0) {
            Toast.makeText(this, "Invalid User ID. Closing application.", Toast.LENGTH_SHORT).show();
            finish(); // Close the activity
            return;
        }
        Log.i(LOG_TAG, "Logged In as UserID: " + userID);

        FirebaseApp.initializeApp(this);
        database = FirebaseDatabase.getInstance().getReference();
        apiService = RetrofitClient.getClient().create(RetrofitClient.ApiService.class);
        getFirebaseStrokeCount();

        webView = findViewById(R.id.webView);
        initializeWebView();

        statusMessage = findViewById(R.id.statusMessage);
        statusMatchedCount = findViewById(R.id.statusMatchedCount);
        statusNotMatchedCount = findViewById(R.id.statusNotMatchedCount);
        statusStrokeCount = findViewById(R.id.statusStrokeCount);
        statusStrokeCountMin = findViewById(R.id.statusStrokeCountMin);

        updateStatusBar();

        fabNext = findViewById(R.id.fabNext);
        fabPrevious = findViewById(R.id.fabPrevious);

        fabNext.setEnabled(webView.canGoForward());
        fabPrevious.setEnabled(webView.canGoBack());

        fabNext.setOnClickListener(v -> webView.goForward());
        fabPrevious.setOnClickListener(v -> webView.goBack());
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(EXTRA_USER_ID, userID); // Save userId before rotation or backgrounding
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        userID = savedInstanceState.getInt(EXTRA_USER_ID); // Restore after rotation
    }

    /**
     * Initializes the WebView component with JavaScript enabled and a custom WebViewClient.
     * Loads the home website and sets up a touch listener to capture swipe gestures.
     */
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
        webView.loadUrl(Constants.HOME_WEBSITE);

        webView.setOnTouchListener((v, event) -> {
            handleTouchEvent(event);
            return false;
        });
    }

    /**
     * Updates the status bar display based on the current phase (enrollment or verification)
     * and the collected stroke/match counts.
     */
    @SuppressLint("SetTextI18n")
    private void updateStatusBar() {
        if (strokeCount < Constants.MIN_STROKE_COUNT) { // Enrollment
            statusMatchedCount.setVisibility(View.GONE);
            statusNotMatchedCount.setVisibility(View.GONE);

            statusStrokeCount.setVisibility(View.VISIBLE);
            statusStrokeCountMin.setVisibility(View.VISIBLE);

            statusMessage.setText("Swipe Enrollment Phase");
            statusStrokeCountMin.setText("/" + Constants.MIN_STROKE_COUNT);
            statusStrokeCount.setText(String.valueOf(strokeCount));

        } else { //Verification
            statusStrokeCount.setVisibility(View.GONE);
            statusStrokeCountMin.setVisibility(View.GONE);

            statusMatchedCount.setVisibility(View.VISIBLE);
            statusNotMatchedCount.setVisibility(View.VISIBLE);

            statusMessage.setText("Swipe Verification Phase");
            statusMatchedCount.setText(String.valueOf(matchedCount));
            statusNotMatchedCount.setText(String.valueOf(notMatchedCount));
        }
    }

    /**
     * Handles touch events from the WebView to record swipe gestures.
     * Creates and populates Stroke objects based on ACTION_DOWN, ACTION_MOVE, and ACTION_UP events.
     * @param event The MotionEvent object containing touch data.
     */
    private void handleTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                //Start Swipe Stroke
                currentStroke = new Stroke();
                currentStroke.setStartTime(event.getEventTime());
                currentStroke.addPointWithEvent(event);
                break;
            case MotionEvent.ACTION_MOVE:
                if (currentStroke != null) {
                    currentStroke.addPointWithEvent(event);
                }
                break;
            case MotionEvent.ACTION_UP:
                if (currentStroke.getPoints().size() > 3) {
                    currentStroke.setEndTime(event.getEventTime());
                    completeStroke();
                }
                currentStroke = null;
                break;
        }
    }

    /**
     * Fetches the initial stroke count for the current user from Firebase.
     * Updates the status bar after retrieving the count.
     */
    private void getFirebaseStrokeCount() {
        DatabaseReference userRef = database.child(String.valueOf(userID));

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                strokeCount = dataSnapshot.getChildrenCount();
                Log.i(LOG_TAG, "Number of strokes: " + strokeCount);
                updateStatusBar();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(LOG_TAG, "Error fetching count", databaseError.toException());
                updateStatusBar();
            }
        });
    }

    /**
     * Processes a completed swipe stroke.
     * Extracts features from the stroke and either adds them to Firebase (enrollment phase)
     * or sends them to the server for verification (verification phase).
     */
    private void completeStroke() {
        Features features = new Features();

        features.setUserID(userID);
        features.setStrokeDuration(currentStroke.getEndTime() - currentStroke.getStartTime());
        features.setMidStrokeArea(currentStroke.calculateMidStrokeArea());
        features.setMidStrokePressure(currentStroke.calculateMidStrokePressure());
        features.setDirectionEndToEnd(currentStroke.calculateDirectionEndToEnd());
        features.setAverageDirection(currentStroke.calculateAverageDirection());
        features.setAverageVelocity(currentStroke.calculateAverageVelocity());
        features.setPairwiseVelocityPercentile(currentStroke.calculatePairwiseVelocityPercentile(50));
        features.setStartX(currentStroke.getStartX());
        features.setStopX(currentStroke.getStopX());
        features.setStartY(currentStroke.getStartY());
        features.setStopY(currentStroke.getStopY());
        features.setTouchArea(currentStroke.calculateTotalTouchArea());
        features.setAverageTouchArea(currentStroke.calculateAverageTouchArea());
        features.setMaxVelocity(currentStroke.calculateMaxVelocity());
        features.setMinVelocity(currentStroke.calculateMinVelocity());
        features.setAverageAcceleration(currentStroke.calculateAverageAcceleration());
        features.setAverageDeceleration(currentStroke.calculateAverageDeceleration());
        features.setTrajectoryLength(currentStroke.calculateTrajectoryLength());
        features.setCurvature(currentStroke.calculateAveragePathDeviation());
        features.setVelocityVariance(currentStroke.calculateVelocityVariance());
        features.setAngleChangeRate(currentStroke.calculateAngleChangeRate());
        features.setMaxPressure(currentStroke.calculateMaxPressure());
        features.setMinPressure(currentStroke.calculateMinPressure());
        features.setInitPressure(currentStroke.calculateInitPressure());
        features.setPressureChangeRate(currentStroke.calculatePressureChangeRate());
        features.setPressureVariance(currentStroke.calculatePressureVariance());
        features.setXDis(currentStroke.calculateXDisplacement());
        features.setYDis(currentStroke.calculateYDisplacement());
        features.setMaxIdleTime(currentStroke.calculateMaxIdleTime());
        features.setStraightnessRatio(currentStroke.calculateStraightnessRatio());

        if (strokeCount < Constants.MIN_STROKE_COUNT) {
            Log.i(LOG_TAG, "Adding to Firebase");
            database.child(String.valueOf(userID)).push().setValue(features);
            strokeCount++;
            Log.i(LOG_TAG, "Number of strokes: " + strokeCount);

            updateStatusBar();
        } else {
            Log.i(LOG_TAG, "Sending features to server");
            sendToServer(features);
        }
    }

    /**
     * Sends the extracted features of a swipe to the backend server for verification.
     * Updates the UI based on the server's response (match or no match).
     * @param features The Features object containing data from the swipe.
     */
    public void sendToServer(@NonNull Features features) {
        Call<JsonObject> response = apiService.sendFeatures(userID, features);

        response.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {

                    boolean prediction = response.body().get("match").getAsBoolean();
                    String message = response.body().get("message").getAsString();
                    Log.i(LOG_TAG, "Features sent successfully: " + message);

                    if(prediction) { // matched
                        matchedCount++;
                    } else { //not matched
                        notMatchedCount++;
                    }
                    updateStatusBar();
                } else {
                    Log.e(LOG_TAG, "Server returned an error: " + response.message());
                    Snackbar.make(findViewById(android.R.id.content), "Error: " + response.message(), Snackbar.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                Log.e(LOG_TAG, "Failed to send features: " + t.getMessage());
                Snackbar.make(findViewById(android.R.id.content), "Error: " + t.getMessage(), Snackbar.LENGTH_SHORT).show();
            }
        });
    }

}
