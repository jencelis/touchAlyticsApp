package com.project.touchalytics;

import static com.project.touchalytics.Constants.SERVER_BASE_URL;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
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

import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Main activity of the application.
 * Handles user interaction with a WebView, collects touch data,
 * and communicates with Firebase and a backend server for enrollment and verification.
 */
public class MainActivity {

    private static final String TAG = "SocketClient";
//    private static final String SERVER_BASE_URL = "10.128.13.109"; // <-- Replace with your PC's LAN IP
    private static final int SERVER_PORT = 5000;
    private TextView textView;


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

            sendToPython(features);
        }
    }
    private JSONObject featuresToJSON(Features features) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("userID", features.getUserID());
            obj.put("strokeDuration", features.getStrokeDuration());
            obj.put("midStrokeArea", features.getMidStrokeArea());
            obj.put("midStrokePressure", features.getMidStrokePressure());
            obj.put("directionEndToEnd", features.getDirectionEndToEnd());
            obj.put("averageDirection", features.getAverageDirection());
            obj.put("averageVelocity", features.getAverageVelocity());
            obj.put("pairwiseVelocityPercentile", features.getPairwiseVelocityPercentile());
            obj.put("startX", features.getStartX());
            obj.put("stopX", features.getStopX());
            obj.put("startY", features.getStartY());
            obj.put("stopY", features.getStopY());
            obj.put("touchArea", features.getTouchArea());
            obj.put("maxVelo", features.getMaxVelocity());
            obj.put("minVelo", features.getMinVelocity());
            obj.put("aveAccel", features.getAverageAcceleration());
            obj.put("aveDecel", features.getAverageDeceleration());
            obj.put("trajLength", features.getTrajectoryLength());
            obj.put("curvature", features.getCurvature());
            obj.put("veloVariance", features.getVelocityVariance());
            obj.put("angleChangeRate", features.getAngleChangeRate());
            obj.put("maxPress", features.getMaxPressure());
            obj.put("minPress", features.getMinPressure());
            obj.put("initPress", features.getInitPressure());
            obj.put("pressChangeRate", features.getPressureChangeRate());
            obj.put("pressVariance", features.getPressureVariance());


            //Not sure if these are implemented yet but here for later use

//            obj.put("maxIdleTime", features.getMaxIdleTime());
//            obj.put("straightnessRatio", features.getStraightnessRatio());
//            obj.put("aveTouchArea", features.getAverageTouchArea());
//            obj.put("xDisplacement", features.getXDisplacement());
//            obj.put("yDisplacement", features.getYDisplacement());

        } catch (Exception e) {
            e.printStackTrace();
        }
        return obj;
    }

    private void sendToPython(Features features) {
        new Thread(() -> {
            try {
                Socket socket = new Socket(SERVER_BASE_URL, SERVER_PORT);
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                DataInputStream dis = new DataInputStream(socket.getInputStream());

                // Convert Features to JSON string
                String jsonString = featuresToJSON(features).toString();

                // Send raw UTF-8 bytes
                byte[] bytes = jsonString.getBytes("UTF-8");
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

