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
    private float touchArea; // average pixel contact area (px^2)
    private float directionEndToEnd;
    private float averageDirection;
    private float averageVelocity;
    private float maxVelocity; // peak instantaneous velocity (pixels/ms)
    private float minVelocity; // minimum non-zero instantaneous velocity (pixels/ms)
    private float velocityVariance; // variance of segment velocities ((pixels/ms)^2)
    private float pairwiseVelocityPercentile;
    private float startX;
    private float stopX;
    private float startY;
    private float stopY;
    private float averageAcceleration; // average positive acceleration (pixels/ms^2)
    private float averageDeceleration; // average deceleration magnitude (pixels/ms^2)
    private float trajectoryLength; // total distance traveled along stroke path (pixels)
    private float curvature; // average deviation from straight line (pixels)
    private float angleChangeRate; // direction change frequency (changes/s)

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
     * Gets the average touch contact area in pixels^2.
     * @return The average touch contact area (px^2).
     */
    public float getTouchArea() {
        return touchArea;
    }

    /**
     * Sets the average touch contact area in pixels^2.
     * @param touchArea The average touch contact area (px^2).
     */
    public void setTouchArea(float touchArea) {
        this.touchArea = touchArea;
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
     * Gets the peak instantaneous velocity of the stroke.
     * @return The maximum velocity (pixels/ms).
     */
    public float getMaxVelocity() {
        return maxVelocity;
    }

    /**
     * Sets the peak instantaneous velocity of the stroke.
     * @param maxVelocity The maximum velocity (pixels/ms).
     */
    public void setMaxVelocity(float maxVelocity) {
        this.maxVelocity = maxVelocity;
    }

    /**
     * Gets the minimum non-zero instantaneous velocity of the stroke.
     * @return The minimum positive velocity (pixels/ms). Returns 0 if none observed.
     */
    public float getMinVelocity() {
        return minVelocity;
    }

    /**
     * Sets the minimum non-zero instantaneous velocity of the stroke.
     * @param minVelocity The minimum positive velocity (pixels/ms).
     */
    public void setMinVelocity(float minVelocity) {
        this.minVelocity = minVelocity;
    }

    /**
     * Gets the variance of instantaneous velocities within the stroke.
     * Units: (pixels/ms)^2.
     * @return The velocity variance.
     */
    public float getVelocityVariance() {
        return velocityVariance;
    }

    /**
     * Sets the variance of instantaneous velocities within the stroke.
     * Units: (pixels/ms)^2.
     * @param velocityVariance The velocity variance.
     */
    public void setVelocityVariance(float velocityVariance) {
        this.velocityVariance = velocityVariance;
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

    /**
     * Gets the average positive acceleration of the stroke.
     * Units: pixels/ms^2.
     * @return The average acceleration.
     */
    public float getAverageAcceleration() {
        return averageAcceleration;
    }

    /**
     * Sets the average positive acceleration of the stroke.
     * Units: pixels/ms^2.
     * @param averageAcceleration The average acceleration.
     */
    public void setAverageAcceleration(float averageAcceleration) {
        this.averageAcceleration = averageAcceleration;
    }

    /**
     * Gets the average deceleration magnitude of the stroke.
     * Units: pixels/ms^2.
     * @return The average deceleration magnitude.
     */
    public float getAverageDeceleration() {
        return averageDeceleration;
    }

    /**
     * Sets the average deceleration magnitude of the stroke.
     * Units: pixels/ms^2.
     * @param averageDeceleration The average deceleration magnitude.
     */
    public void setAverageDeceleration(float averageDeceleration) {
        this.averageDeceleration = averageDeceleration;
    }

    /**
     * Gets the total trajectory length of the stroke.
     * This represents the total distance traveled along the stroke path.
     * Units: pixels.
     * @return The total path length of the stroke.
     */
    public float getTrajectoryLength() {
        return trajectoryLength;
    }

    /**
     * Sets the total trajectory length of the stroke.
     * This represents the total distance traveled along the stroke path.
     * Units: pixels.
     * @param trajectoryLength The total path length of the stroke.
     */
    public void setTrajectoryLength(float trajectoryLength) {
        this.trajectoryLength = trajectoryLength;
    }

    /**
     * Gets the average path deviation (curvature) from the straight line connecting
     * the first and last points of the stroke.
     * Units: pixels.
     * @return The average deviation in pixels.
     */
    public float getCurvature() {
        return curvature;
    }

    /**
     * Sets the average path deviation (curvature) from the straight line connecting
     * the first and last points of the stroke.
     * Units: pixels.
     * @param curvature The average deviation in pixels.
     */
    public void setCurvature(float curvature) {
        this.curvature = curvature;
    }

    /**
     * Gets the direction change frequency ("angleChangeRate") of the stroke.
     * Units: changes per second.
     * @return The direction change frequency.
     */
    public float getAngleChangeRate() {
        return angleChangeRate;
    }

    /**
     * Sets the direction change frequency ("angleChangeRate") of the stroke.
     * Units: changes per second.
     * @param angleChangeRate The direction change frequency.
     */
    public void setAngleChangeRate(float angleChangeRate) {
        this.angleChangeRate = angleChangeRate;
    }
}
