package com.project.touchalytics.data;

import androidx.annotation.NonNull;

/**
 * Represents a single point in a touch gesture.
 * Contains information about the coordinates, timestamp, pressure, size,
 * and contact ellipse dimensions (major and minor axes in pixels).
 */
public class TouchPoint {
    float x;
    float y;
    long timestamp;
    float pressure;
    float size;
    float touchMajor;
    float touchMinor;

    /**
     * Constructs a new TouchPoint object.
     *
     * @param x The x-coordinate of the touch point.
     * @param y The y-coordinate of the touch point.
     * @param timestamp The time at which the touch event occurred, in milliseconds.
     * @param pressure The pressure of the touch event.
     * @param size The size of the touch event.
     * @param touchMajor The major axis of the touch contact area in pixels.
     * @param touchMinor The minor axis of the touch contact area in pixels.
     */
    TouchPoint(float x, float y, long timestamp, float pressure, float size, float touchMajor, float touchMinor) {
        this.x = x;
        this.y = y;
        this.timestamp = timestamp;
        this.pressure = pressure;
        this.size = size;
        this.touchMajor = touchMajor;
        this.touchMinor = touchMinor;
    }

    /**
     * Returns a string representation of the TouchPoint object.
     *
     * @return A string containing the x, y, timestamp, pressure, size, touchMajor, and touchMinor of the touch point.
     */
    @NonNull
    @Override
    public String toString() {
        return "TouchPoint{" +
                "x=" + x +
                ", y=" + y +
                ", timestamp=" + timestamp +
                ", pressure=" + pressure +
                ", size=" + size +
                ", touchMajor=" + touchMajor +
                ", touchMinor=" + touchMinor +
                '}';
    }
}