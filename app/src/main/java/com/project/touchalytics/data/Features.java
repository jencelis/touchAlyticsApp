package com.project.touchalytics.data;

/**
 * Represents the features extracted from a single touch stroke.
 * This data is used for user authentication and analytics.
 */
public class Features {
    private int userID;
    private float strokeDuration;
    private float midStrokeArea;
    private float midStrokePressure;
    private float directionEndToEnd;
    private float averageDirection;
    private float averageVelocity;
    private float pairwiseVelocityPercentile;
    private float startX;
    private float stopX;
    private float startY;
    private float stopY;

    /**
     * Default constructor for Features.
     */
    public Features() {}

    /**
     * Gets the user ID associated with this stroke.
     * @return The user ID.
     */
    public int getUserID() {
        return userID;
    }

    /**
     * Sets the user ID for this stroke.
     * @param userID The user ID.
     */
    public void setUserID(int userID) {
        this.userID = userID;
    }

    /**
     * Gets the duration of the stroke in milliseconds.
     * @return The stroke duration.
     */
    public float getStrokeDuration() {
        return strokeDuration;
    }

    /**
     * Sets the duration of the stroke.
     * @param strokeDuration The stroke duration in milliseconds.
     */
    public void setStrokeDuration(float strokeDuration) {
        this.strokeDuration = strokeDuration;
    }

    /**
     * Gets the area covered by the touch during the middle of the stroke.
     * @return The mid-stroke area.
     */
    public float getMidStrokeArea() {
        return midStrokeArea;
    }

    /**
     * Sets the mid-stroke area.
     * @param midStrokeArea The mid-stroke area.
     */
    public void setMidStrokeArea(float midStrokeArea) {
        this.midStrokeArea = midStrokeArea;
    }

    /**
     * Gets the pressure applied during the middle of the stroke.
     * @return The mid-stroke pressure.
     */
    public float getMidStrokePressure() {
        return midStrokePressure;
    }

    /**
     * Sets the mid-stroke pressure.
     * @param midStrokePressure The mid-stroke pressure.
     */
    public void setMidStrokePressure(float midStrokePressure) {
        this.midStrokePressure = midStrokePressure;
    }

    /**
     * Gets the end-to-end direction of the stroke.
     * @return The end-to-end direction.
     */
    public float getDirectionEndToEnd() {
        return directionEndToEnd;
    }

    /**
     * Sets the end-to-end direction of the stroke.
     * @param directionEndToEnd The end-to-end direction.
     */
    public void setDirectionEndToEnd(float directionEndToEnd) {
        this.directionEndToEnd = directionEndToEnd;
    }

    /**
     * Gets the average direction of the stroke.
     * @return The average direction.
     */
    public float getAverageDirection() {
        return averageDirection;
    }

    /**
     * Sets the average direction of the stroke.
     * @param averageDirection The average direction.
     */
    public void setAverageDirection(float averageDirection) {
        this.averageDirection = averageDirection;
    }

    /**
     * Gets the average velocity of the stroke.
     * @return The average velocity.
     */
    public float getAverageVelocity() {
        return averageVelocity;
    }

    /**
     * Sets the average velocity of the stroke.
     * @param averageVelocity The average velocity.
     */
    public void setAverageVelocity(float averageVelocity) {
        this.averageVelocity = averageVelocity;
    }

    /**
     * Gets the specified percentile of pairwise velocities within the stroke.
     * @return The pairwise velocity percentile.
     */
    public float getPairwiseVelocityPercentile() {
        return pairwiseVelocityPercentile;
    }

    /**
     * Sets the pairwise velocity percentile.
     * @param pairwiseVelocityPercentile The pairwise velocity percentile.
     */
    public void setPairwiseVelocityPercentile(float pairwiseVelocityPercentile) {
        this.pairwiseVelocityPercentile = pairwiseVelocityPercentile;
    }

    /**
     * Gets the starting X-coordinate of the stroke.
     * @return The starting X-coordinate.
     */
    public float getStartX() {
        return startX;
    }

    /**
     * Sets the starting X-coordinate of the stroke.
     * @param startX The starting X-coordinate.
     */
    public void setStartX(float startX) {
        this.startX = startX;
    }

    /**
     * Gets the ending X-coordinate of the stroke.
     * @return The ending X-coordinate.
     */
    public float getStopX() {
        return stopX;
    }

    /**
     * Sets the ending X-coordinate of the stroke.
     * @param stopX The ending X-coordinate.
     */
    public void setStopX(float stopX) {
        this.stopX = stopX;
    }

    /**
     * Gets the starting Y-coordinate of the stroke.
     * @return The starting Y-coordinate.
     */
    public float getStartY() {
        return startY;
    }

    /**
     * Sets the starting Y-coordinate of the stroke.
     * @param startY The starting Y-coordinate.
     */
    public void setStartY(float startY) {
        this.startY = startY;
    }

    /**
     * Gets the ending Y-coordinate of the stroke.
     * @return The ending Y-coordinate.
     */
    public float getStopY() {
        return stopY;
    }

    /**
     * Sets the ending Y-coordinate of the stroke.
     * @param stopY The ending Y-coordinate.
     */
    public void setStopY(float stopY) {
        this.stopY = stopY;
    }
}
