package com.project.touchalytics;

/**
 * Contains constant values used throughout the application.
 */
public class Constants {

    /**
     * The base URL of the server.
     * Change to the IP address and port of your TouchalyticsServer.
     */
    public static final String SERVER_BASE_URL = "10.128.13.109";

    /**
     * The URL of the home website for scrolling.
     */
    public static final String HOME_WEBSITE = "https://www.nytimes.com/";

    /**
     * The minimum number of strokes required for training.
     * This should match the value in TouchalyticsServer.
     */
    public static final Integer MIN_STROKE_COUNT = 50;

    /**
     * The minimum number of taps required for training.
     */
    public static final Integer MIN_TAP_COUNT = 20;
}
