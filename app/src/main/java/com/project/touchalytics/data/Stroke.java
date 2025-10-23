package com.project.touchalytics.data;

import android.view.MotionEvent;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single continuous touch gesture (stroke) made by the user.
 * It contains a list of {@link TouchPoint} objects that make up the stroke,
 * along with methods to calculate various features of the stroke.
 */
public class Stroke {

    long startTime;
    long endTime;

    List<TouchPoint> points;

    /**
     * Constructs a new Stroke object with an empty list of touch points.
     */
    public Stroke(){
        this.points = new ArrayList<>();
    }

    /**
     * Gets the start time of the stroke.
     * @return The start time in milliseconds.
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * Sets the start time of the stroke.
     * @param startTime The start time in milliseconds.
     */
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    /**
     * Gets the end time of the stroke.
     * @return The end time in milliseconds.
     */
    public long getEndTime() {
        return endTime;
    }

    /**
     * Sets the end time of the stroke.
     * @param endTime The end time in milliseconds.
     */
    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    /**
     * Adds a new {@link TouchPoint} to the stroke based on a {@link MotionEvent}.
     * @param event The MotionEvent to extract touch data from.
     */
    public void addPointWithEvent(MotionEvent event) {
        TouchPoint point = new TouchPoint(
                event.getX(),
                event.getY(),
                event.getEventTime(),
                event.getPressure(),
                event.getSize(),
                event.getTouchMajor(),
                event.getTouchMinor()
        );
        points.add(point);
    }

    /**
     * Gets the list of {@link TouchPoint} objects that make up this stroke.
     * @return A list of {@link TouchPoint} objects.
     */
    public List<TouchPoint> getPoints(){
        return points;
    }

    /**
     * Returns a string representation of the stroke, including its duration and constituent points.
     * @return A string detailing the stroke.
     */
    @NonNull
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Stroke was:").append(endTime-startTime).append("milliseconds\n");

        for (TouchPoint p : points) {
            sb.append(p.toString()).append("\n");
        }
        return sb.toString();
    }

    /**
     * Adds a new {@link TouchPoint} to the stroke with specified feature values.
     * @param x x coordinate of the point.
     * @param y y coordinate of the point.
     * @param timestamp timestamp of the point.
     * @param pressure pressure of the point.
     * @param size size of the point.
     */
    public void addPointWithFeatures(float x, float y, long timestamp, float pressure, float size) {
        TouchPoint point = new TouchPoint(x, y, timestamp, pressure, size, 0f, 0f);
        points.add(point);
    }

    /**
     * Calculates the area of the bounding box covering the middle portion of the stroke.
     * @return The area of the mid-stroke bounding box. Returns 0 if the stroke is empty.
     */
    public float calculateMidStrokeArea() {

        if (points.isEmpty()) return 0;

        float minX = points.get(0).x;
        float maxX = points.get(0).x;

        float minY = points.get(0).y;
        float maxY = points.get(0).y;

        for (TouchPoint point : points) {
            if (point.x < minX) minX = point.x;
            if (point.x > maxX) maxX = point.x;

            if (point.y < minY) minY = point.y;
            if (point.y > maxY) maxY = point.y;
        }

        return (maxX - minX) * (maxY - minY);  // Bounding box area
    }

    /**
     * Calculates the average pixel area of touch contact across the stroke.
     * Uses the contact ellipse from MotionEvent (touchMajor/touchMinor) in pixels:
     * area = π * (touchMajor / 2) * (touchMinor / 2).
     *
     * @return The average touch contact area in pixels^2. Returns 0 if no usable points exist.
     */
    public float calculateAverageTouchArea() {

        if (points.isEmpty()) return 0;

        double totalArea = 0;
        int validCount = 0;

        for (TouchPoint point : points) {
            if (point.touchMajor > 0 && point.touchMinor > 0) {
                double area = Math.PI * (point.touchMajor / 2.0) * (point.touchMinor / 2.0);
                totalArea += area;
                validCount++;
            }
        }

        if (validCount == 0) return 0;

        return (float) (totalArea / validCount);
    }

    /**
     * Calculates the Euclidean distance between two {@link TouchPoint} objects.
     * @param p1 The first touch point.
     * @param p2 The second touch point.
     * @return The distance between the two points.
     */
    private float calculateDistance(TouchPoint p1, TouchPoint p2) {
        return (float) Math.sqrt(Math.pow(p2.x - p1.x, 2) + Math.pow(p2.y - p1.y, 2));
    }

    /**
     * Calculates the specified percentile of pairwise velocities between consecutive points in the stroke.
     * @param percentile The percentile to calculate (0-100).
     * @return The velocity at the specified percentile. Returns 0 if there are fewer than 2 points or if all time deltas are zero.
     */
    public float calculatePairwiseVelocityPercentile(int percentile) {

        if (points.size() < 2) {
            return 0;
        }

        List<Float> velocities = new ArrayList<>();

        for (int i = 1; i < points.size(); i++) {
            float distance = calculateDistance(points.get(i-1), points.get(i));
            float timeDelta = points.get(i).timestamp - points.get(i-1).timestamp;

            if (timeDelta > 0) {
                velocities.add(distance / timeDelta);
            }
        }

        if (velocities.isEmpty()) {
            return 0;
        }

        velocities.sort(Float::compare);

        int index = Math.round((velocities.size() - 1) * percentile / 100.0f);

        return velocities.get(index);
    }

    /**
     * Calculates the average pressure applied during the middle portion (25th to 75th percentile) of the stroke.
     * @return The average pressure in the middle of the stroke. Returns 0 if the stroke has fewer than 3 points.
     */
    public float calculateMidStrokePressure() {
        if (points.size() < 3) return 0;

        int midStart = points.size() / 4;
        int midEnd = 3 * points.size() / 4;

        float totalPressure = 0;
        int count = 0;
        for (int i = midStart; i <= midEnd; i++) {
            totalPressure += points.get(i).pressure;
            count++;
        }

        return totalPressure / count;
    }

    /**
     * Calculates the direction of the stroke from its start point to its end point.
     * The direction is represented as an angle in radians.
     * @return The end-to-end direction of the stroke in radians. Returns 0 if the stroke has fewer than 2 points.
     */
    public float calculateDirectionEndToEnd() {
        if (points.size() < 2) return 0;

        TouchPoint start = points.get(0);
        TouchPoint end = points.get(points.size() - 1);

        return (float) Math.atan2(end.y - start.y, end.x - start.x);
    }

    /**
     * Gets the X-coordinate of the first point in the stroke.
     * @return The starting X-coordinate.
     */
    public float getStartX() {
        return points.get(0).x;
    }

    /**
     * Gets the X-coordinate of the last point in the stroke.
     * @return The ending X-coordinate.
     */
    public float getStopX() {
        return points.get(points.size() - 1).x;
    }

    /**
     * Gets the Y-coordinate of the first point in the stroke.
     * @return The starting Y-coordinate.
     */
    public float getStartY() {
        return points.get(0).y;
    }

    /**
     * Gets the Y-coordinate of the last point in the stroke.
     * @return The ending Y-coordinate.
     */
    public float getStopY() {
        return points.get(points.size() - 1).y;
    }

    /**
     * Calculates the average direction of the stroke by averaging the direction between consecutive points.
     * The direction is represented as an angle in radians.
     * @return The average direction of the stroke in radians. Returns 0 if the stroke has fewer than 2 points.
     */
    public float calculateAverageDirection() {
        if (points.size() < 2) return 0;

        float totalDirection = 0;
        int count = 0;

        for (int i = 1; i < points.size(); i++) {
            totalDirection += (float) Math.atan2(points.get(i).y - points.get(i-1).y, points.get(i).x - points.get(i-1).x);
            count++;
        }

        return totalDirection / count;
    }

    /**
     * Calculates the average velocity of the stroke.
     * This is the total distance traveled divided by the total time taken.
     * @return The average velocity of the stroke. Returns 0 if the stroke has fewer than 2 points.
     */
    public float calculateAverageVelocity() {
        if (points.size() < 2) return 0;

        float totalDistance = 0;
        for (int i = 1; i < points.size(); i++) {
            totalDistance += calculateDistance(points.get(i-1), points.get(i));
        }

        long totalTime = points.get(points.size() - 1).timestamp - points.get(0).timestamp;
        if (totalTime == 0) return 0; // Avoid division by zero
        return totalDistance / totalTime;
    }

    /**
     * Calculates the maximum instantaneous velocity between consecutive points in the stroke.
     * Velocity is computed as distance / timeDelta for each consecutive pair (pixels per millisecond).
     * @return The peak velocity during the stroke. Returns 0 if there are fewer than 2 points or all time deltas are non-positive.
     */
    public float calculateMaxVelocity() {
        if (points.size() < 2) {
            return 0;
        }

        float maxVelocity = 0;

        for (int i = 1; i < points.size(); i++) {
            float distance = calculateDistance(points.get(i - 1), points.get(i));
            float timeDelta = points.get(i).timestamp - points.get(i - 1).timestamp;

            if (timeDelta > 0) {
                float velocity = distance / timeDelta; // pixels/ms
                if (velocity > maxVelocity) {
                    maxVelocity = velocity;
                }
            }
        }

        return maxVelocity;
    }

}
