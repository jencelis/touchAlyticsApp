package com.project.touchalytics;

/**
 * Contains constant values used throughout the application.
 */
public class Constants {

    /**
     * The base IP of the server.
     * Change to the IP address of your TouchalyticsServer.
     */
    public static final String SERVER_BASE_URL = "128.153.221.6";

    /**
     * The base port of the flask server.
     * Change to the flask port of your TouchalyticsServer.
     */
    public static final int AUTH_SERVER_PORT = 5000;

    /**
     * The base port of the socket server.
     * Change to the socket port of your TouchalyticsServer.
     */
    public static final int SERVER_PORT = 7000;
    
    /**
     * The URL of the home website for scrolling.
     */
    public static final String HOME_WEBSITE = "https://www.nytimes.com/";

    /**
     * The minimum number of strokes required for training.
     * This should match the value in TouchalyticsServer.
     */
    public static final Integer MIN_STROKE_COUNT = 90;

    /**
     * The minimum number of strokes required for wordle training.
     */
    public static final int MIN_W_STROKE_COUNT = 20;
    /**
     * The minimum number of strokes required for fruit ninja training.
     */
    public static final int FRUIT_NINJA_MIN_STROKE_COUNT = 40;
    /**
     * The minimum number of strokes required for new fee training.
     */
    public static final int NEWS_MEDIA_MIN_STROKE_COUNT = 30;
}
