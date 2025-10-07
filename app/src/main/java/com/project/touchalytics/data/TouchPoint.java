package com.project.touchalytics.data;

import androidx.annotation.NonNull;

/**
 * Represents a single point in a touch gesture.
 * Contains information about the coordinates, timestamp, pressure, and size of the touch event.
 */
public class TouchPoint {
    float x;
    float y;
    long timestamp;
    float pressure;
    float size;

    /**
     * Constructs a new TouchPoint object.
     *
     * @param x The x-coordinate of the touch point.
     * @param y The y-coordinate of the touch point.
     * @param timestamp The time at which the touch event occurred, in milliseconds.
     * @param pressure The pressure of the touch event.
     * @param size The size of the touch event.
     */
    TouchPoint(float x, float y, long timestamp, float pressure, float size) {
        this.x = x;
        this.y = y;
        this.timestamp = timestamp;
        this.pressure = pressure;
        this.size = size;
    }

    /**
     * Returns a string representation of the TouchPoint object.
     *
     * @return A string containing the x, y, timestamp, pressure, and size of the touch point.
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
                '}';
    }
}
