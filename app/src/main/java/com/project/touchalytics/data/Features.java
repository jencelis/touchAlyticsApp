package com.project.touchalytics.data;

import androidx.annotation.NonNull;

/**
 * Represents the features extracted from a single touch stroke.
 * This data is used for user authentication and analytics.
 */
public class Features {
    private int userID;
    private float strokeDuration;
    private float startToEndDistance;
    private float averageVelocity;
    private float initialVelocity;
    private float finalVelocity;
    private float midStrokePressure;
    private float midStrokeArea;
    private float midStrokeToFirstThirdDisplacement;
    private float midStrokeToLastThirdDisplacement;
    private float firstThirdToLastThirdDisplacement;
    private float firstThirdVelocity;
    private float midStrokeVelocity;
    private float lastThirdVelocity;
    private float accelerationAtFirstThird;
    private float accelerationAtMidPoint;
    private float accelerationAtLastThird;
    private float jerkAtFirstThird;
    private float jerkAtMidPoint;
    private float jerkAtLastThird;
    private float angleAtFirstThird;
    private float angleAtMidPoint;
    private float angleAtLastThird;
    private float totalAngleTraversed;
    private float averageDirectionalChange;
    private float directionalChangeRatio;
    private float curvatureAtFirstThird;
    private float curvatureAtMidPoint;
    private float curvatureAtLastThird;
    private float deviceOrientation;
    private float strokeStraightness;
    private float screenEntryAndExitPoints;


    /**
     * Default constructor for Features.
     */
    public Features() {}

    public int getUserID() {
        return userID;
    }

    public void setUserID(int userID) {
        this.userID = userID;
    }

    public float getStrokeDuration() {
        return strokeDuration;
    }

    public void setStrokeDuration(float strokeDuration) {
        this.strokeDuration = strokeDuration;
    }

    public float getStartToEndDistance() {
        return startToEndDistance;
    }

    public void setStartToEndDistance(float startToEndDistance) {
        this.startToEndDistance = startToEndDistance;
    }

    public float getAverageVelocity() {
        return averageVelocity;
    }

    public void setAverageVelocity(float averageVelocity) {
        this.averageVelocity = averageVelocity;
    }

    public float getInitialVelocity() {
        return initialVelocity;
    }

    public void setInitialVelocity(float initialVelocity) {
        this.initialVelocity = initialVelocity;
    }

    public float getFinalVelocity() {
        return finalVelocity;
    }

    public void setFinalVelocity(float finalVelocity) {
        this.finalVelocity = finalVelocity;
    }

    public float getMidStrokePressure() {
        return midStrokePressure;
    }

    public void setMidStrokePressure(float midStrokePressure) {
        this.midStrokePressure = midStrokePressure;
    }

    public float getMidStrokeArea() {
        return midStrokeArea;
    }

    public void setMidStrokeArea(float midStrokeArea) {
        this.midStrokeArea = midStrokeArea;
    }

    public float getMidStrokeToFirstThirdDisplacement() {
        return midStrokeToFirstThirdDisplacement;
    }

    public void setMidStrokeToFirstThirdDisplacement(float midStrokeToFirstThirdDisplacement) {
        this.midStrokeToFirstThirdDisplacement = midStrokeToFirstThirdDisplacement;
    }

    public float getMidStrokeToLastThirdDisplacement() {
        return midStrokeToLastThirdDisplacement;
    }

    public void setMidStrokeToLastThirdDisplacement(float midStrokeToLastThirdDisplacement) {
        this.midStrokeToLastThirdDisplacement = midStrokeToLastThirdDisplacement;
    }

    public float getFirstThirdToLastThirdDisplacement() {
        return firstThirdToLastThirdDisplacement;
    }

    public void setFirstThirdToLastThirdDisplacement(float firstThirdToLastThirdDisplacement) {
        this.firstThirdToLastThirdDisplacement = firstThirdToLastThirdDisplacement;
    }

    public float getFirstThirdVelocity() {
        return firstThirdVelocity;
    }

    public void setFirstThirdVelocity(float firstThirdVelocity) {
        this.firstThirdVelocity = firstThirdVelocity;
    }

    public float getMidStrokeVelocity() {
        return midStrokeVelocity;
    }

    public void setMidStrokeVelocity(float midStrokeVelocity) {
        this.midStrokeVelocity = midStrokeVelocity;
    }

    public float getLastThirdVelocity() {
        return lastThirdVelocity;
    }

    public void setLastThirdVelocity(float lastThirdVelocity) {
        this.lastThirdVelocity = lastThirdVelocity;
    }

    public float getAccelerationAtFirstThird() {
        return accelerationAtFirstThird;
    }

    public void setAccelerationAtFirstThird(float accelerationAtFirstThird) {
        this.accelerationAtFirstThird = accelerationAtFirstThird;
    }

    public float getAccelerationAtMidPoint() {
        return accelerationAtMidPoint;
    }

    public void setAccelerationAtMidPoint(float accelerationAtMidPoint) {
        this.accelerationAtMidPoint = accelerationAtMidPoint;
    }

    public float getAccelerationAtLastThird() {
        return accelerationAtLastThird;
    }

    public void setAccelerationAtLastThird(float accelerationAtLastThird) {
        this.accelerationAtLastThird = accelerationAtLastThird;
    }

    public float getJerkAtFirstThird() {
        return jerkAtFirstThird;
    }

    public void setJerkAtFirstThird(float jerkAtFirstThird) {
        this.jerkAtFirstThird = jerkAtFirstThird;
    }

    public float getJerkAtMidPoint() {
        return jerkAtMidPoint;
    }

    public void setJerkAtMidPoint(float jerkAtMidPoint) {
        this.jerkAtMidPoint = jerkAtMidPoint;
    }

    public float getJerkAtLastThird() {
        return jerkAtLastThird;
    }

    public void setJerkAtLastThird(float jerkAtLastThird) {
        this.jerkAtLastThird = jerkAtLastThird;
    }

    public float getAngleAtFirstThird() {
        return angleAtFirstThird;
    }

    public void setAngleAtFirstThird(float angleAtFirstThird) {
        this.angleAtFirstThird = angleAtFirstThird;
    }

    public float getAngleAtMidPoint() {
        return angleAtMidPoint;
    }

    public void setAngleAtMidPoint(float angleAtMidPoint) {
        this.angleAtMidPoint = angleAtMidPoint;
    }

    public float getAngleAtLastThird() {
        return angleAtLastThird;
    }

    public void setAngleAtLastThird(float angleAtLastThird) {
        this.angleAtLastThird = angleAtLastThird;
    }

    public float getTotalAngleTraversed() {
        return totalAngleTraversed;
    }

    public void setTotalAngleTraversed(float totalAngleTraversed) {
        this.totalAngleTraversed = totalAngleTraversed;
    }

    public float getAverageDirectionalChange() {
        return averageDirectionalChange;
    }

    public void setAverageDirectionalChange(float averageDirectionalChange) {
        this.averageDirectionalChange = averageDirectionalChange;
    }

    public float getDirectionalChangeRatio() {
        return directionalChangeRatio;
    }

    public void setDirectionalChangeRatio(float directionalChangeRatio) {
        this.directionalChangeRatio = directionalChangeRatio;
    }

    public float getCurvatureAtFirstThird() {
        return curvatureAtFirstThird;
    }

    public void setCurvatureAtFirstThird(float curvatureAtFirstThird) {
        this.curvatureAtFirstThird = curvatureAtFirstThird;
    }

    public float getCurvatureAtMidPoint() {
        return curvatureAtMidPoint;
    }

    public void setCurvatureAtMidPoint(float curvatureAtMidPoint) {
        this.curvatureAtMidPoint = curvatureAtMidPoint;
    }

    public float getCurvatureAtLastThird() {
        return curvatureAtLastThird;
    }

    public void setCurvatureAtLastThird(float curvatureAtLastThird) {
        this.curvatureAtLastThird = curvatureAtLastThird;
    }

    public float getDeviceOrientation() {
        return deviceOrientation;
    }

    public void setDeviceOrientation(float deviceOrientation) {
        this.deviceOrientation = deviceOrientation;
    }

    public float getStrokeStraightness() {
        return strokeStraightness;
    }

    public void setStrokeStraightness(float strokeStraightness) {
        this.strokeStraightness = strokeStraightness;
    }

    public float getScreenEntryAndExitPoints() {
        return screenEntryAndExitPoints;
    }

    public void setScreenEntryAndExitPoints(float screenEntryAndExitPoints) {
        this.screenEntryAndExitPoints = screenEntryAndExitPoints;
    }

    @NonNull
    @Override
    public String toString() {
        return "Features{" +
                "userID=" + userID +
                ", strokeDuration=" + strokeDuration +
                ", startToEndDistance=" + startToEndDistance +
                ", averageVelocity=" + averageVelocity +
                ", initialVelocity=" + initialVelocity +
                ", finalVelocity=" + finalVelocity +
                ", midStrokePressure=" + midStrokePressure +
                ", midStrokeArea=" + midStrokeArea +
                ", midStrokeToFirstThirdDisplacement=" + midStrokeToFirstThirdDisplacement +
                ", midStrokeToLastThirdDisplacement=" + midStrokeToLastThirdDisplacement +
                ", firstThirdToLastThirdDisplacement=" + firstThirdToLastThirdDisplacement +
                ", firstThirdVelocity=" + firstThirdVelocity +
                ", midStrokeVelocity=" + midStrokeVelocity +
                ", lastThirdVelocity=" + lastThirdVelocity +
                ", accelerationAtFirstThird=" + accelerationAtFirstThird +
                ", accelerationAtMidPoint=" + accelerationAtMidPoint +
                ", accelerationAtLastThird=" + accelerationAtLastThird +
                ", jerkAtFirstThird=" + jerkAtFirstThird +
                ", jerkAtMidPoint=" + jerkAtMidPoint +
                ", jerkAtLastThird=" + jerkAtLastThird +
                ", angleAtFirstThird=" + angleAtFirstThird +
                ", angleAtMidPoint=" + angleAtMidPoint +
                ", angleAtLastThird=" + angleAtLastThird +
                ", totalAngleTraversed=" + totalAngleTraversed +
                ", averageDirectionalChange=" + averageDirectionalChange +
                ", directionalChangeRatio=" + directionalChangeRatio +
                ", curvatureAtFirstThird=" + curvatureAtFirstThird +
                ", curvatureAtMidPoint=" + curvatureAtMidPoint +
                ", curvatureAtLastThird=" + curvatureAtLastThird +
                ", deviceOrientation=" + deviceOrientation +
                ", strokeStraightness=" + strokeStraightness +
                ", screenEntryAndExitPoints=" + screenEntryAndExitPoints +
                '}';
    }
}
