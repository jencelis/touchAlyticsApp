package com.project.touchalytics.data;

import android.view.MotionEvent;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a single continuous touch gesture (stroke) made by the user.
 * It contains a list of {@link TouchPoint} objects that make up the stroke,
 * along with methods to calculate various features of the stroke.
 */
public class Stroke {

    long startTime;
    long endTime;
    float deviceOrientation;

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

    private float calculateDistance(TouchPoint p1, TouchPoint p2) {
        return (float) Math.sqrt(Math.pow(p2.x - p1.x, 2) + Math.pow(p2.y - p1.y, 2));
    }

    public float calculateStartToEndDistance() {
        if (points.size() < 2) return 0;
        return calculateDistance(points.get(0), points.get(points.size() - 1));
    }

    public float calculateAverageVelocity() {
        if (points.size() < 2) return 0;
        float totalDistance = 0;
        for (int i = 1; i < points.size(); i++) {
            totalDistance += calculateDistance(points.get(i-1), points.get(i));
        }
        long totalTime = points.get(points.size() - 1).timestamp - points.get(0).timestamp;
        return totalTime > 0 ? totalDistance / totalTime : 0;
    }

    public float calculateInitialVelocity() {
        if (points.size() < 2) return 0;
        float distance = calculateDistance(points.get(0), points.get(1));
        long time = points.get(1).timestamp - points.get(0).timestamp;
        return time > 0 ? distance / time : 0;
    }

    public float calculateFinalVelocity() {
        if (points.size() < 2) return 0;
        int last = points.size() - 1;
        float distance = calculateDistance(points.get(last - 1), points.get(last));
        long time = points.get(last).timestamp - points.get(last - 1).timestamp;
        return time > 0 ? distance / time : 0;
    }

    public float calculateMidStrokePressure() {
        if (points.isEmpty()) return 0;
        return points.get(points.size() / 2).pressure;
    }

    public float calculateMidStrokeArea() {
        if (points.isEmpty()) return 0;
        return points.get(points.size() / 2).size;
    }

    public float calculateMidStrokeToFirstThirdDisplacement() {
        if (points.size() < 3) return 0;
        return calculateDistance(points.get(points.size() / 2), points.get(points.size() / 3));
    }

    public float calculateMidStrokeToLastThirdDisplacement() {
        if (points.size() < 3) return 0;
        return calculateDistance(points.get(points.size() / 2), points.get(2 * points.size() / 3));
    }

    public float calculateFirstThirdToLastThirdDisplacement() {
        if (points.size() < 3) return 0;
        return calculateDistance(points.get(points.size() / 3), points.get(2 * points.size() / 3));
    }

    public float calculateFirstThirdVelocity() {
        if (points.size() < 3) return 0;
        int index = points.size() / 3;
        float distance = calculateDistance(points.get(index), points.get(index + 1));
        long time = points.get(index + 1).timestamp - points.get(index).timestamp;
        return time > 0 ? distance / time : 0;
    }

    public float calculateMidStrokeVelocity() {
        if (points.size() < 2) return 0;
        int index = points.size() / 2;
        float distance = calculateDistance(points.get(index), points.get(index + 1));
        long time = points.get(index + 1).timestamp - points.get(index).timestamp;
        return time > 0 ? distance / time : 0;
    }

    public float calculateLastThirdVelocity() {
        if (points.size() < 3) return 0;
        int index = 2 * points.size() / 3;
        float distance = calculateDistance(points.get(index), points.get(index + 1));
        long time = points.get(index + 1).timestamp - points.get(index).timestamp;
        return time > 0 ? distance / time : 0;
    }

    public float calculateAccelerationAtFirstThird() {
        if (points.size() < 4) return 0;
        int index = points.size() / 3;
        float v1 = calculateVelocityAtIndex(index);
        float v2 = calculateVelocityAtIndex(index + 1);
        long time = points.get(index + 1).timestamp - points.get(index).timestamp;
        return time > 0 ? (v2 - v1) / time : 0;
    }

    public float calculateAccelerationAtMidPoint() {
        if (points.size() < 3) return 0;
        int index = points.size() / 2;
        float v1 = calculateVelocityAtIndex(index);
        float v2 = calculateVelocityAtIndex(index + 1);
        long time = points.get(index + 1).timestamp - points.get(index).timestamp;
        return time > 0 ? (v2 - v1) / time : 0;
    }

    public float calculateAccelerationAtLastThird() {
        if (points.size() < 4) return 0;
        int index = 2 * points.size() / 3;
        float v1 = calculateVelocityAtIndex(index);
        float v2 = calculateVelocityAtIndex(index + 1);
        long time = points.get(index + 1).timestamp - points.get(index).timestamp;
        return time > 0 ? (v2 - v1) / time : 0;
    }

    private float calculateVelocityAtIndex(int index) {
        if (index < 0 || index >= points.size() - 1) return 0;
        float distance = calculateDistance(points.get(index), points.get(index + 1));
        long time = points.get(index + 1).timestamp - points.get(index).timestamp;
        return time > 0 ? distance / time : 0;
    }

    public float calculateJerkAtFirstThird() {
        if (points.size() < 5) return 0;
        int index = points.size() / 3;
        float a1 = calculateAccelerationAtIndex(index);
        float a2 = calculateAccelerationAtIndex(index + 1);
        long time = points.get(index + 1).timestamp - points.get(index).timestamp;
        return time > 0 ? (a2 - a1) / time : 0;
    }

    public float calculateJerkAtMidPoint() {
        if (points.size() < 4) return 0;
        int index = points.size() / 2;
        float a1 = calculateAccelerationAtIndex(index);
        float a2 = calculateAccelerationAtIndex(index + 1);
        long time = points.get(index + 1).timestamp - points.get(index).timestamp;
        return time > 0 ? (a2 - a1) / time : 0;
    }

    public float calculateJerkAtLastThird() {
        if (points.size() < 5) return 0;
        int index = 2 * points.size() / 3;
        float a1 = calculateAccelerationAtIndex(index);
        float a2 = calculateAccelerationAtIndex(index + 1);
        long time = points.get(index + 1).timestamp - points.get(index).timestamp;
        return time > 0 ? (a2 - a1) / time : 0;
    }

    private float calculateAccelerationAtIndex(int index) {
        if (index < 0 || index >= points.size() - 2) return 0;
        float v1 = calculateVelocityAtIndex(index);
        float v2 = calculateVelocityAtIndex(index + 1);
        long time = points.get(index + 1).timestamp - points.get(index).timestamp;
        return time > 0 ? (v2 - v1) / time : 0;
    }

    public float calculateAngleAtFirstThird() {
        if (points.size() < 3) return 0;
        int index = points.size() / 3;
        return calculateAngleAtIndex(index);
    }

    public float calculateAngleAtMidPoint() {
        if (points.size() < 2) return 0;
        int index = points.size() / 2;
        return calculateAngleAtIndex(index);
    }

    public float calculateAngleAtLastThird() {
        if (points.size() < 3) return 0;
        int index = 2 * points.size() / 3;
        return calculateAngleAtIndex(index);
    }

    private float calculateAngleAtIndex(int index) {
        if (index < 0 || index >= points.size() - 1) return 0;
        TouchPoint p1 = points.get(index);
        TouchPoint p2 = points.get(index + 1);
        return (float) Math.atan2(p2.y - p1.y, p2.x - p1.x);
    }

    public float calculateTotalAngleTraversed() {
        if (points.size() < 2) return 0;
        float totalAngle = 0;
        for (int i = 1; i < points.size(); i++) {
            totalAngle += Math.abs(calculateAngleAtIndex(i - 1) - calculateAngleAtIndex(i));
        }
        return totalAngle;
    }

    public float calculateAverageDirectionalChange() {
        if (points.size() < 2) return 0;
        return calculateTotalAngleTraversed() / (points.size() - 1);
    }

    public float calculateDirectionalChangeRatio() {
        if (points.size() < 2) return 0;
        float trajectoryLength = calculateTrajectoryLength();
        return trajectoryLength > 0 ? calculateTotalAngleTraversed() / trajectoryLength : 0;
    }

    private float calculateTrajectoryLength() {
        if (points.size() < 2) return 0;
        float length = 0;
        for (int i = 1; i < points.size(); i++) {
            length += calculateDistance(points.get(i-1), points.get(i));
        }
        return length;
    }

    public float calculateCurvatureAtFirstThird() {
        if (points.size() < 3) return 0;
        int index = points.size() / 3;
        return calculateCurvatureAtIndex(index);
    }

    public float calculateCurvatureAtMidPoint() {
        if (points.size() < 3) return 0;
        int index = points.size() / 2;
        return calculateCurvatureAtIndex(index);
    }

    public float calculateCurvatureAtLastThird() {
        if (points.size() < 3) return 0;
        int index = 2 * points.size() / 3;
        return calculateCurvatureAtIndex(index);
    }

    private float calculateCurvatureAtIndex(int index) {
        if (index < 1 || index >= points.size() - 1) return 0;
        float angle1 = calculateAngleAtIndex(index - 1);
        float angle2 = calculateAngleAtIndex(index);
        float distance = calculateDistance(points.get(index - 1), points.get(index));
        return distance > 0 ? Math.abs(angle2 - angle1) / distance : 0;
    }

    public float getDeviceOrientation() {
        return deviceOrientation;
    }

    public void setDeviceOrientation(float deviceOrientation) {
        this.deviceOrientation = deviceOrientation;
    }

    public float calculateStrokeStraightness() {
        if (points.size() < 2) return 0;
        float trajectoryLength = calculateTrajectoryLength();
        if (trajectoryLength == 0) return 0;
        return calculateStartToEndDistance() / trajectoryLength;
    }

    public float calculateScreenEntryAndExitPoints() {
        // This is a placeholder. The actual implementation depends on how you define
        // 'screen entry and exit points' as a single float value.
        // For example, you could concatenate normalized coordinates, but that's not a single float.
        // Let's return a combination of start and end points for now.
        if (points.isEmpty()) return 0;
        TouchPoint start = points.get(0);
        TouchPoint end = points.get(points.size() -1);
        return start.x + start.y + end.x + end.y;
    }
}
