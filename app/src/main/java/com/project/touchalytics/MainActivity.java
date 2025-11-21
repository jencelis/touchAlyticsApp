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

    public interface TouchAnalyticsListener {
        void onStrokeCountUpdated(long newCount);
        void onVerificationResult(boolean matched, int matchedCount, int notMatchedCount);
        void onError(String message);
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
                if (currentStroke != null) {
                    currentStroke.setEndTime(event.getEventTime());
                    completeStroke();
                }
                currentStroke = null;
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
