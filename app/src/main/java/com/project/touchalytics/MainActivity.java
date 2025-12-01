package com.project.touchalytics;

import static com.project.touchalytics.Constants.SERVER_BASE_URL;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

import com.project.touchalytics.data.Features;
import com.project.touchalytics.data.Stroke;

import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

import com.google.gson.JsonObject;
import com.project.touchalytics.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


/**
 * Touch analytics manager (singleton, NOT an Android Activity).
 *
 * - Collects strokes from training UIs (NewsMediaActivity, FruitNinjaActivity, WordleActivity)
 * - Extracts Features
 * - Tracks per-phase strokeCount
 * - Sends strokes to the Python server in TRAINING mode only
 * - In FREE mode: still tracks strokes locally, but does NOT send to server
 */
public class MainActivity {

    public static final String EXTRA_STROKE_COUNT = "extra_stroke_count";

    private static final String TAG = "SocketClient";
    private static final int SERVER_PORT = 7000; // socket port
    private static final int AUTH_SERVER_PORT = 5000; // Flask app port
    public static final String LOG_TAG = "TouchAnalyticsManager";

    private static MainActivity instance;

    private Integer userID;
    private long strokeCount = 0L;
    private Stroke currentStroke;

    // These can be wired up later using the Python response
    private int matchedCount = 0;
    private int notMatchedCount = 0;

    // Per-phase minimum for “training complete” (e.g., 30, 40, 20)
    private int minStrokeCount = Constants.MIN_STROKE_COUNT; // default fallback

    // If true, we do NOT send strokes to the DB and we do NOT cap by minStrokeCount
    private boolean freeMode = false;

    private TouchAnalyticsListener listener;

    public interface TouchAnalyticsListener {
        void onStrokeCountUpdated(long newCount);
        void onVerificationResult(boolean matched, int matchedCount, int notMatchedCount);
        void onError(String message);
    }

    public interface SwipeCountCallback {
        void onResult(long totalCount);
        void onError(String message);
    }


    private MainActivity() { }

    public static synchronized MainActivity getInstance() {
        if (instance == null) {
            instance = new MainActivity();
        }
        return instance;
    }

    // ----------------------------------------------------------------------
    // Initialization overloads
    // ----------------------------------------------------------------------

    /**
     * Legacy initializer: defaults to training-style with global MIN_STROKE_COUNT,
     * strokeCount starts from 0, freeMode=false.
     */
    public void initialize(Context context, int userId, TouchAnalyticsListener listener) {
        initialize(context, userId, listener, Constants.MIN_STROKE_COUNT, 0L, false);
    }

    /**
     * Training initializer with a per-phase minStrokes cap.
     * strokeCount starts from 0, freeMode=false.
     */
    public void initialize(Context context,
                           int userId,
                           TouchAnalyticsListener listener,
                           int minStrokes) {
        initialize(context, userId, listener, minStrokes, 0L, false);
    }

    /**
     * Training initializer with explicit starting strokeCount
     * (e.g., when resuming a phase from a server-derived total).
     * freeMode=false.
     */
    public void initialize(Context context,
                           int userId,
                           TouchAnalyticsListener listener,
                           int minStrokes,
                           long initialStrokeCount) {
        initialize(context, userId, listener, minStrokes, initialStrokeCount, false);
    }

    /**
     * Full initializer: can be used for both TRAINING and FREE modes.
     *
     *  - TRAINING: freeMode=false, minStrokes = phase cap (e.g., 30/40/20)
     *  - FREE:     freeMode=true, minStrokes is ignored (we don't cap)
     */
    public void initialize(Context context,
                           int userId,
                           TouchAnalyticsListener listener,
                           int minStrokes,
                           long initialStrokeCount,
                           boolean freeMode) {

        this.userID = userId;
        this.listener = listener;
        this.freeMode = freeMode;

        if (this.userID == null || this.userID < 0) {
            Toast.makeText(context.getApplicationContext(),
                    "Invalid User ID for Touch Analytics.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (freeMode) {
            // No training cap in free mode
            this.minStrokeCount = Integer.MAX_VALUE;
        } else {
            this.minStrokeCount = minStrokes;
        }

        Log.i(LOG_TAG,
                "TouchAnalyticsManager initialized for UserID: " + userID +
                        " | minStrokes=" + this.minStrokeCount +
                        " | initialStrokeCount=" + initialStrokeCount +
                        " | freeMode=" + this.freeMode);

        // Set counters
        if (freeMode) {
            // In free mode, pretend global training is complete
            // so every phase sees strokeCount >= its own threshold
            this.strokeCount = Constants.MIN_STROKE_COUNT;  // e.g. 90
        } else {
            this.strokeCount = initialStrokeCount;
        }
        this.matchedCount = 0;
        this.notMatchedCount = 0;

        if (this.listener != null) {
            this.listener.onStrokeCountUpdated(strokeCount);
        }
    }

    public void reset() {
        strokeCount = 0L;
        matchedCount = 0;
        notMatchedCount = 0;
        minStrokeCount = Constants.MIN_STROKE_COUNT;
        freeMode = false;
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

    public boolean isFreeMode() {
        return freeMode;
    }

    // ----------------------------------------------------------------------
    // Touch handling
    // ----------------------------------------------------------------------

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

    /**
     * Build Features from the current stroke, bump the local strokeCount,
     * notify the UI listener, and:
     *
     *  - TRAINING mode: send features to the Python server UNTIL the per-phase cap is reached.
     *  - FREE mode: do NOT send to the server (no DB writes).
     */
    private void completeStroke() {
        if (currentStroke == null) return;

        // ---- TRAINING cap: do not exceed this phase's stroke limit ----
        if (!freeMode && strokeCount >= minStrokeCount) {
            Log.i(LOG_TAG,
                    "Phase stroke cap reached (" + minStrokeCount +
                            "). Ignoring additional stroke for this phase.");
            return; // do not build/send
        }

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

        // LOCAL progress
        if (!freeMode) {
            strokeCount++;
            Log.i(LOG_TAG, "New stroke count (this phase): " + strokeCount +
                    " | freeMode=" + freeMode);
        } else {
            Log.i(LOG_TAG, "Free mode active: stroke count NOT incremented.");
        }

        if (listener != null) {
            listener.onStrokeCountUpdated(strokeCount);
        }


        // TRAINING ONLY: send to Python
        if (!freeMode) {
            sendToPython(features);
        } else {
            Log.i(LOG_TAG, "Free mode active: NOT sending features to server/DB.");
            sendForAuthentication(features);
        }
    }


    /**
     * Ask the Python server how many strokes are currently stored for this user.
     *
     * Protocol (simple):
     *   Request:  "FCOUNT|<userID>"
     *   Response: "<totalCount>"   (e.g. "87" or "90")
     *
     * NOTE: callback is invoked from the background thread.
     *       Activities should wrap it in runOnUiThread().
     */
    public void fetchStoredSwipeCount(int userId, SwipeCountCallback callback) {
        new Thread(() -> {

            try {
                // Small delay to let any in-flight FSTORE inserts finish
                Thread.sleep(300);  // 300 ms is usually enough; tweak if needed
            } catch (InterruptedException ignored) {
            }
            try {
                Socket socket = new Socket(SERVER_BASE_URL, SERVER_PORT);
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                DataInputStream dis = new DataInputStream(socket.getInputStream());

                // Build and send: FCOUNT|<userID>
                String payload = "FCOUNT|" + userId;
                byte[] bytes = payload.getBytes("UTF-8");
                dos.write(bytes);
                dos.flush();

                // Read response: plain number as text
                byte[] buffer = new byte[1024];
                int read = dis.read(buffer);
                if (read <= 0) {
                    if (callback != null) {
                        callback.onError("Empty response from server while fetching swipe count.");
                    }
                } else {
                    String response = new String(buffer, 0, read, "UTF-8").trim();
                    Log.i(TAG, "Server Response (FCOUNT): " + response);
                    try {
                        long totalCount = Long.parseLong(response);
                        if (callback != null) {
                            callback.onResult(totalCount);
                        }
                    } catch (NumberFormatException nfe) {
                        nfe.printStackTrace();
                        if (callback != null) {
                            callback.onError("Unexpected FCOUNT response: " + response);
                        }
                    }
                }

                dos.close();
                dis.close();
                socket.close();

            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "Error fetching stored swipe count from Python server", e);
                if (callback != null) {
                    callback.onError("Failed to contact server for swipe count.");
                }
            }
        }).start();
    }



    private JSONObject featuresToJSON(Features features) {
        JSONObject obj = new JSONObject();
        try {
            // ---- EXACTLY MATCHING DB COLUMN NAMES ----
            obj.put("userID", features.getUserID());
            obj.put("strokeDuration", features.getStrokeDuration());
            obj.put("midStrokeArea", features.getMidStrokeArea());
            obj.put("midStrokePress", features.getMidStrokePressure());

            obj.put("dirEndToEnd", features.getDirectionEndToEnd());
            obj.put("aveDir", features.getAverageDirection());
            obj.put("aveVelo", features.getAverageVelocity());
            obj.put("pairwiseVeloPercent", features.getPairwiseVelocityPercentile());

            obj.put("startX", features.getStartX());
            obj.put("startY", features.getStartY());
            obj.put("stopX", features.getStopX());
            obj.put("stopY", features.getStopY());

            obj.put("touchArea", features.getTouchArea());
            obj.put("maxVelo", features.getMaxVelocity());
            obj.put("minVelo", features.getMinVelocity());

            obj.put("accel", features.getAverageAcceleration());
            obj.put("decel", features.getAverageDeceleration());

            obj.put("trajLength", features.getTrajectoryLength());
            obj.put("curvature", features.getCurvature());
            obj.put("veloVariance", features.getVelocityVariance());
            obj.put("angleChangeRate", features.getAngleChangeRate());

            obj.put("maxPress", features.getMaxPressure());
            obj.put("minPress", features.getMinPressure());
            obj.put("initPress", features.getInitPressure());
            obj.put("pressChangeRate", features.getPressureChangeRate());
            obj.put("pressVariance", features.getPressureVariance());

            obj.put("maxIdleTime", features.getMaxIdleTime());
            obj.put("straightnessRatio", features.getStraightnessRatio());

            obj.put("xDisplacement", features.getXDis());
            obj.put("yDisplacement", features.getYDis());
            obj.put("aveTouchArea", features.getAverageTouchArea());

        } catch (Exception e) {
            e.printStackTrace();
        }
        return obj;
    }

    /**
     * Sends a single stroke's Features to the Python socket server on SERVER_PORT.
     *
     *   FSTORE|{ ...features JSON... }
     */
    private void sendToPython(Features features) {
        new Thread(() -> {
            try {
                Socket socket = new Socket(SERVER_BASE_URL, SERVER_PORT);
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                DataInputStream dis = new DataInputStream(socket.getInputStream());

                // Convert Features to JSON string
                String jsonString = featuresToJSON(features).toString();

                // Prefix with FSTORE| so the server can route it
                String payload = "FSTORE|" + jsonString;
                byte[] bytes = payload.getBytes("UTF-8");

                dos.write(bytes);
                dos.flush();

                // Read response (optional)
                byte[] buffer = new byte[1024];
                int read = dis.read(buffer);
                if (read > 0) {
                    String response = new String(buffer, 0, read, "UTF-8");
                    Log.i(TAG, "Server Response (features): " + response);
                }

                dos.close();
                dis.close();
                socket.close();

            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "Error sending features to Python server", e);
            }
        }).start();
    }

    private void sendForAuthentication(Features features) {

        // Build JSON using DB column / REQUIRED_FEATURES names
        JsonObject payload = new JsonObject();

        payload.addProperty("userID", features.getUserID());
        payload.addProperty("strokeDuration", features.getStrokeDuration());
        payload.addProperty("midStrokeArea", features.getMidStrokeArea());
        payload.addProperty("midStrokePress", features.getMidStrokePressure());

        payload.addProperty("dirEndToEnd", features.getDirectionEndToEnd());
        payload.addProperty("aveDir", features.getAverageDirection());
        payload.addProperty("aveVelo", features.getAverageVelocity());
        payload.addProperty("pairwiseVeloPercent", features.getPairwiseVelocityPercentile());

        payload.addProperty("startX", features.getStartX());
        payload.addProperty("startY", features.getStartY());
        payload.addProperty("stopX", features.getStopX());
        payload.addProperty("stopY", features.getStopY());

        payload.addProperty("touchArea", features.getTouchArea());
        payload.addProperty("maxVelo", features.getMaxVelocity());
        payload.addProperty("minVelo", features.getMinVelocity());

        payload.addProperty("accel", features.getAverageAcceleration());
        payload.addProperty("decel", features.getAverageDeceleration());

        payload.addProperty("trajLength", features.getTrajectoryLength());
        payload.addProperty("curvature", features.getCurvature());
        payload.addProperty("veloVariance", features.getVelocityVariance());
        payload.addProperty("angleChangeRate", features.getAngleChangeRate());

        payload.addProperty("maxPress", features.getMaxPressure());
        payload.addProperty("minPress", features.getMinPressure());
        payload.addProperty("initPress", features.getInitPressure());
        payload.addProperty("pressChangeRate", features.getPressureChangeRate());
        payload.addProperty("pressVariance", features.getPressureVariance());

        payload.addProperty("maxIdleTime", features.getMaxIdleTime());
        payload.addProperty("straightnessRatio", features.getStraightnessRatio());

        payload.addProperty("xDisplacement", features.getXDis());
        payload.addProperty("yDisplacement", features.getYDis());
        payload.addProperty("aveTouchArea", features.getAverageTouchArea());

        Log.i(TAG, "Auth request JSON (Retrofit): " + payload.toString());

        RetrofitClient.ApiService apiService =
                RetrofitClient.getClient().create(RetrofitClient.ApiService.class);

        Call<JsonObject> call = apiService.sendFeatures(features.getUserID(), payload);

        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                boolean matched = false;
                String message = "No message";

                if (response.isSuccessful() && response.body() != null) {
                    JsonObject json = response.body();

                    if (json.has("match")) {
                        matched = "true".equalsIgnoreCase(json.get("match").getAsString());
                    }
                    if (json.has("message")) {
                        message = json.get("message").getAsString();
                    }
                } else {
                    matched = false;
                    try {
                        if (response.errorBody() != null) {
                            String err = response.errorBody().string();
                            Log.w(TAG, "Auth error body: " + err);
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Failed to read error body", e);
                    }
                    message = "HTTP " + response.code() + " during auth";
                }

                // Update counters
                if (matched) {
                    matchedCount++;
                } else {
                    notMatchedCount++;
                }

                Log.i(TAG, "Auth result: matched=" + matched +
                        " | matchedCount=" + matchedCount +
                        " | notMatchedCount=" + notMatchedCount +
                        " | message=" + message);

                if (listener != null) {
                    // Retrofit callbacks are on main thread → safe for UI
                    listener.onVerificationResult(matched, matchedCount, notMatchedCount);

                    if (!matched && response.code() >= 500) {
                        listener.onError("Server error during verification: " + message);
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "Retrofit authentication failed", t);
                if (listener != null) {
                    listener.onError("Failed to contact authentication server.");
                }
            }
        });
    }



}
