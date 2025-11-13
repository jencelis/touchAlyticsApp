package com.project.touchalytics;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.JsonObject;
import com.project.touchalytics.data.Features;
import com.project.touchalytics.data.Stroke;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity {

    public static final String LOG_TAG = "TouchAnalyticsManager";

    private static MainActivity instance;

    private DatabaseReference database;
    private RetrofitClient.ApiService apiService;

    private Integer userID;
    private long strokeCount = 0L;
    private Stroke currentStroke;

    private int matchedCount = 0;
    private int notMatchedCount = 0;
    private int minStrokeCount = Constants.MIN_STROKE_COUNT; // Default

    private TouchAnalyticsListener listener;

    // TAP
    private long tapCount = 0;
    private long lastTapEndTime = -1L;
    private TapModel tapModel = new TapModel();
    private int tapMatchedCount = 0;
    private int tapNotMatchedCount = 0;

    public interface TouchAnalyticsListener {
        void onStrokeCountUpdated(long newCount);
        void onVerificationResult(boolean matched, int matchedCount, int notMatchedCount);
        void onError(String message);
        void onTapCountUpdated(long newCount);
        void onTapVerificationResult(boolean matched, int matchedCount, int notMatchedCount);
    }

    private MainActivity() { }

    public static synchronized MainActivity getInstance() {
        if (instance == null) {
            instance = new MainActivity();
        }
        return instance;
    }

    public void initialize(Context context, int userId, TouchAnalyticsListener listener) {
        initialize(context, userId, listener, Constants.MIN_STROKE_COUNT);
    }

    public void initialize(Context context, int userId, TouchAnalyticsListener listener, int minStrokes) {
        this.userID = userId;
        this.listener = listener;
        this.minStrokeCount = minStrokes;

        if (this.userID < 0) {
            Toast.makeText(context, "Invalid User ID for Touch Analytics.", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.i(LOG_TAG, "TouchAnalyticsManager initialized for UserID: " + userID);

        FirebaseApp.initializeApp(context.getApplicationContext());
        database = FirebaseDatabase.getInstance().getReference();
        apiService = RetrofitClient.getClient().create(RetrofitClient.ApiService.class);
        getFirebaseStrokeCount();
    }

    public void reset() {
        strokeCount = 0L;
        matchedCount = 0;
        notMatchedCount = 0;
        minStrokeCount = Constants.MIN_STROKE_COUNT;

        tapCount = 0;
        lastTapEndTime = -1L;
        tapModel = new TapModel();
        tapMatchedCount = 0;
        tapNotMatchedCount = 0;

        Log.i(LOG_TAG, "TouchAnalyticsManager state has been reset.");
    }

    public long getStrokeCount() {
        return strokeCount;
    }

    public int getMatchedCount() {
        return matchedCount;
    }

    public int getNotMatchedCount() {
        return notMatchedCount;
    }

    public long getTapCount() { return tapCount; }

    public int getTapMatchedCount() { return tapMatchedCount; }

    public int getTapNotMatchedCount() { return tapNotMatchedCount; }

    public void handleTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
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
                if (currentStroke != null && currentStroke.getPoints().size() > 3) {
                    currentStroke.setEndTime(event.getEventTime());
                    completeStroke();
                }
                currentStroke = null;
                break;
        }
    }

    public void handleTapEvent(MotionEvent event, View view, String logicalCode) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                view.setTag(R.id.ta_down, TapCapture.fromDown(event, view));
                break;
            case MotionEvent.ACTION_UP:
                TapCapture down = (TapCapture) view.getTag(R.id.ta_down);
                if (down != null) {
                    TapFeatures tf = TapFeatures.fromUp(
                            down, event, view, logicalCode, userID, lastTapEndTime);
                    onTapRecorded(tf);
                    lastTapEndTime = tf.endTimeMs;
                    view.setTag(R.id.ta_down, null);
                }
                break;
        }
    }

    private void getFirebaseStrokeCount() {
        strokeCount = 0L;
        if (listener != null) {
            listener.onStrokeCountUpdated(strokeCount);
        }
    }

    private void completeStroke() {
        Features features = new Features();

        features.setUserID(userID);
        features.setStrokeDuration(currentStroke.getEndTime() - currentStroke.getStartTime());
        features.setStartToEndDistance(currentStroke.calculateStartToEndDistance());
        features.setAverageVelocity(currentStroke.calculateAverageVelocity());
        features.setInitialVelocity(currentStroke.calculateInitialVelocity());
        features.setFinalVelocity(currentStroke.calculateFinalVelocity());
        features.setMidStrokePressure(currentStroke.calculateMidStrokePressure());
        features.setMidStrokeArea(currentStroke.calculateMidStrokeArea());
        features.setMidStrokeToFirstThirdDisplacement(currentStroke.calculateMidStrokeToFirstThirdDisplacement());
        features.setMidStrokeToLastThirdDisplacement(currentStroke.calculateMidStrokeToLastThirdDisplacement());
        features.setFirstThirdToLastThirdDisplacement(currentStroke.calculateFirstThirdToLastThirdDisplacement());
        features.setFirstThirdVelocity(currentStroke.calculateFirstThirdVelocity());
        features.setMidStrokeVelocity(currentStroke.calculateMidStrokeVelocity());
        features.setLastThirdVelocity(currentStroke.calculateLastThirdVelocity());
        features.setAccelerationAtFirstThird(currentStroke.calculateAccelerationAtFirstThird());
        features.setAccelerationAtMidPoint(currentStroke.calculateAccelerationAtMidPoint());
        features.setAccelerationAtLastThird(currentStroke.calculateAccelerationAtLastThird());
        features.setJerkAtFirstThird(currentStroke.calculateJerkAtFirstThird());
        features.setJerkAtMidPoint(currentStroke.calculateJerkAtMidPoint());
        features.setJerkAtLastThird(currentStroke.calculateJerkAtLastThird());
        features.setAngleAtFirstThird(currentStroke.calculateAngleAtFirstThird());
        features.setAngleAtMidPoint(currentStroke.calculateAngleAtMidPoint());
        features.setAngleAtLastThird(currentStroke.calculateAngleAtLastThird());
        features.setTotalAngleTraversed(currentStroke.calculateTotalAngleTraversed());
        features.setAverageDirectionalChange(currentStroke.calculateAverageDirectionalChange());
        features.setDirectionalChangeRatio(currentStroke.calculateDirectionalChangeRatio());
        features.setCurvatureAtFirstThird(currentStroke.calculateCurvatureAtFirstThird());
        features.setCurvatureAtMidPoint(currentStroke.calculateCurvatureAtMidPoint());
        features.setCurvatureAtLastThird(currentStroke.calculateCurvatureAtLastThird());
        features.setDeviceOrientation(currentStroke.getDeviceOrientation());
        features.setStrokeStraightness(currentStroke.calculateStrokeStraightness());
        features.setScreenEntryAndExitPoints(currentStroke.calculateScreenEntryAndExitPoints());

        Log.i(LOG_TAG, "Collected Features: " + features.toString());

        if (strokeCount < minStrokeCount) {
            Log.i(LOG_TAG, "Enrolling stroke. Adding to Firebase.");
            database.child(String.valueOf(userID)).push().setValue(features);
            strokeCount++;
            Log.i(LOG_TAG, "New stroke count: " + strokeCount);

            if (listener != null) {
                listener.onStrokeCountUpdated(strokeCount);
            }
        } else {
            Log.i(LOG_TAG, "Verifying stroke. Sending features to server.");
            sendToServer(features);
        }
    }

    private void sendToServer(@NonNull Features features) {
        Call<JsonObject> response = apiService.sendFeatures(userID, features);

        response.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {

                    boolean prediction = response.body().get("match").getAsBoolean();
                    String message = response.body().get("message").getAsString();
                    Log.i(LOG_TAG, "Features sent successfully: " + message);

                    if(prediction) {
                        matchedCount++;
                    } else {
                        notMatchedCount++;
                    }

                    if (listener != null) {
                        listener.onVerificationResult(prediction, matchedCount, notMatchedCount);
                    }
                } else {
                    Log.e(LOG_TAG, "Server returned an error: " + response.message());
                    if (listener != null) {
                        listener.onError("Server error: " + response.message());
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                Log.e(LOG_TAG, "Failed to send features: " + t.getMessage());
                if (listener != null) {
                    listener.onError("Failed to send features: " + t.getMessage());
                }
            }
        });
    }

    private void onTapRecorded(TapFeatures tf) {
        if (tapCount < Constants.MIN_TAP_COUNT) {
            tapModel.addEnrollment(tf);
            tapCount++;
            if (tapCount == Constants.MIN_TAP_COUNT) {
                tapModel.finalizeModel();
                if(listener != null) listener.onTapCountUpdated(tapCount);
            }
            if (listener != null) {
                listener.onTapCountUpdated(tapCount);
            }
            return;
        }

        // Verification phase
        boolean good = tapModel.isGoodTap(tf);
        if (good) tapMatchedCount++; else tapNotMatchedCount++;
        if (listener != null) {
            listener.onTapVerificationResult(good, tapMatchedCount, tapNotMatchedCount);
        }
    }

    private static class TapCapture {
        final long downTimeMs;
        final float downPressure;
        final float downSize;
        final int downPointers;
        final float localDownX, localDownY;

        private TapCapture(long t, float p, float s, int c, float lx, float ly) {
            downTimeMs = t; downPressure = p; downSize = s; downPointers = c; localDownX = lx; localDownY = ly;
        }
        static TapCapture fromDown(MotionEvent e, View v) {
            return new TapCapture(
                    e.getEventTime(),
                    e.getPressure(),
                    e.getSize(),
                    e.getPointerCount(),
                    e.getX(), e.getY()
            );
        }
    }

    private static class TapFeatures {
        int userId;
        String key;
        long startTimeMs;
        long endTimeMs;
        long durationMs;
        float downPressure, upPressure, avgPressure;
        float downSize, upSize, avgSize;
        float centerX, centerY;
        float tapX, tapY;
        float distFromCenterPx;
        long interTapMs;
        int downPointers, upPointers;

        static TapFeatures fromUp(TapCapture d, MotionEvent up, View keyView, String logicalCode,
                                  int userId, long prevEndTimeMs) {
            TapFeatures f = new TapFeatures();
            f.userId = userId;
            f.key = logicalCode;
            f.startTimeMs = d.downTimeMs;
            f.endTimeMs = up.getEventTime();
            f.durationMs = Math.max(0, f.endTimeMs - f.startTimeMs);

            f.downPressure = d.downPressure;
            f.upPressure = up.getPressure();
            f.avgPressure = (f.downPressure + f.upPressure) / 2f;

            f.downSize = d.downSize;
            f.upSize = up.getSize();
            f.avgSize = (f.downSize + f.upSize) / 2f;

            int[] loc = new int[2];
            keyView.getLocationOnScreen(loc);
            float keyLeft = loc[0];
            float keyTop  = loc[1];
            float keyCx = keyLeft + keyView.getWidth() / 2f;
            float keyCy = keyTop  + keyView.getHeight() / 2f;
            f.centerX = keyCx;
            f.centerY = keyCy;

            float upX = keyLeft + up.getX();
            float upY = keyTop  + up.getY();
            f.tapX = upX;
            f.tapY = upY;

            float dx = f.tapX - f.centerX;
            float dy = f.tapY - f.centerY;
            f.distFromCenterPx = (float) Math.hypot(dx, dy);

            f.interTapMs = (prevEndTimeMs > 0) ? (f.endTimeMs - prevEndTimeMs) : -1;

            f.downPointers = d.downPointers;
            f.upPointers = up.getPointerCount();

            return f;
        }
    }

    private static class TapModel {
        private final ArrayList<TapFeatures> train = new ArrayList<>();
        private boolean frozen = false;

        // stats
        private double mDur, sDur;
        private double mPress, sPress;
        private double mSize, sSize;
        private double mDist, sDist;
        private double mInter, sInter;

        void addEnrollment(TapFeatures f) {
            if (frozen) return;
            train.add(f);
        }

        void finalizeModel() {
            frozen = true;
            mDur = mean(train, t -> t.durationMs);
            mPress = mean(train, t -> t.avgPressure);
            mSize = mean(train, t -> t.avgSize);
            mDist = mean(train, t -> t.distFromCenterPx);
            mInter = mean(train, t -> t.interTapMs >= 0 ? t.interTapMs : 0);

            sDur = std(train, t -> t.durationMs, mDur);
            sPress = std(train, t -> t.avgPressure, mPress);
            sSize = std(train, t -> t.avgSize, mSize);
            sDist = std(train, t -> t.distFromCenterPx, mDist);
            sInter = std(train, t -> t.interTapMs >= 0 ? t.interTapMs : mInter, mInter);
        }

        boolean isGoodTap(TapFeatures f) {
            if (!frozen || train.isEmpty()) return true;
            final double K = 2.5;

            boolean durOK   = withinK(f.durationMs, mDur, sDur, K);
            boolean pressOK = withinK(f.avgPressure, mPress, sPress, K);
            boolean sizeOK  = withinK(f.avgSize, mSize, sSize, K);
            boolean distOK  = withinK(f.distFromCenterPx, mDist, sDist, K);
            boolean interOK = f.interTapMs < 0 || withinK(f.interTapMs, mInter, sInter, K);

            return durOK && pressOK && sizeOK && distOK && interOK;
        }

        private static double mean(ArrayList<TapFeatures> xs, ToDouble x) {
            if (xs.isEmpty()) return 0;
            double s = 0;
            for (TapFeatures t : xs) s += x.get(t);
            return s / xs.size();
        }
        private static double std(ArrayList<TapFeatures> xs, ToDouble x, double m) {
            if (xs.size() <= 1) return 1.0;
            double s = 0;
            for (TapFeatures t : xs) {
                double d = x.get(t) - m;
                s += d * d;
            }
            return Math.sqrt(s / (xs.size() - 1));
        }
        private static boolean withinK(double v, double m, double s, double k) {
            if (s <= 1e-6) return true;
            return Math.abs(v - m) <= k * s;
        }

        private interface ToDouble { double get(TapFeatures t); }
    }
}
