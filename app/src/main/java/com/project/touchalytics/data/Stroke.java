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

    /**
     * Calculates the minimum non-zero instantaneous velocity between consecutive points in the stroke.
     * Velocity is computed as distance / timeDelta for each consecutive pair (pixels per millisecond).
     * Zero-velocity segments (no movement or non-positive dt) are ignored.
     *
     * @return The minimum positive velocity during the stroke. Returns 0 if there are fewer than
     *         2 points or if no positive velocities are observed.
     */
    public float calculateMinVelocity() {
        if (points.size() < 2) {
            return 0;
        }

        float minVelocity = Float.MAX_VALUE;

        for (int i = 1; i < points.size(); i++) {
            float distance = calculateDistance(points.get(i - 1), points.get(i));
            long timeDelta = points.get(i).timestamp - points.get(i - 1).timestamp;

            // Only consider segments with positive time and movement
            if (timeDelta > 0) {
                float velocity = distance / timeDelta; // pixels/ms
                if (velocity > 0f && velocity < minVelocity) {
                    minVelocity = velocity;
                }
            }
        }

        // If we never found a positive velocity, return 0
        return (minVelocity == Float.MAX_VALUE) ? 0 : minVelocity;
    }

    /**
     * Calculates the average positive acceleration (increase in instantaneous velocity)
     * between consecutive segments of the stroke.
     * Acceleration is computed as:
     *   a = (v2 - v1) / ((dt1 + dt2) / 2)
     * where v = distance / dt for each segment, and dt is in milliseconds.
     *
     * Units: pixels per millisecond squared (px/ms^2).
     *
     * @return The average positive acceleration. Returns 0 if the stroke has fewer than 3 points
     *         or if no positive accelerations are observed.
     */
    public float calculateAverageAcceleration() {
        if (points.size() < 3) {
            return 0f;
        }

        // Build per-segment velocities and time deltas (only where dt > 0)
        List<Float> velocities = new ArrayList<>();
        List<Float> dts = new ArrayList<>();

        for (int i = 1; i < points.size(); i++) {
            float distance = calculateDistance(points.get(i - 1), points.get(i));
            long dtMs = points.get(i).timestamp - points.get(i - 1).timestamp;
            if (dtMs > 0) {
                velocities.add(distance / dtMs);        // pixels/ms
                dts.add((float) dtMs);                  // ms
            }
        }

        if (velocities.size() < 2) {
            return 0f;
        }

        float sumAcc = 0f;
        int countAcc = 0;

        for (int i = 0; i < velocities.size() - 1; i++) {
            float v1 = velocities.get(i);
            float v2 = velocities.get(i + 1);
            float dt1 = dts.get(i);
            float dt2 = dts.get(i + 1);
            float denom = (dt1 + dt2) * 0.5f;          // average dt, in ms

            if (denom > 0f) {
                float a = (v2 - v1) / denom;           // px/ms^2
                if (a > 0f) {
                    sumAcc += a;
                    countAcc++;
                }
            }
        }

        return countAcc > 0 ? (sumAcc / countAcc) : 0f;
    }

    /**
     * Calculates the average deceleration magnitude (rate of velocity decrease)
     * between consecutive segments of the stroke.
     * Acceleration is computed as:
     *   a = (v2 - v1) / ((dt1 + dt2) / 2)
     * and only negative values (decelerations) are considered; their magnitudes are averaged.
     *
     * Units: pixels per millisecond squared (px/ms^2).
     *
     * @return The average deceleration magnitude. Returns 0 if the stroke has fewer than 3 points
     *         or if no decelerations are observed.
     */
    public float calculateAverageDeceleration() {
        if (points.size() < 3) {
            return 0f;
        }

        // Build per-segment velocities and time deltas (only where dt > 0)
        List<Float> velocities = new ArrayList<>();
        List<Float> dts = new ArrayList<>();

        for (int i = 1; i < points.size(); i++) {
            float distance = calculateDistance(points.get(i - 1), points.get(i));
            long dtMs = points.get(i).timestamp - points.get(i - 1).timestamp;
            if (dtMs > 0) {
                velocities.add(distance / dtMs);        // pixels/ms
                dts.add((float) dtMs);                  // ms
            }
        }

        if (velocities.size() < 2) {
            return 0f;
        }

        float sumDec = 0f;
        int countDec = 0;

        for (int i = 0; i < velocities.size() - 1; i++) {
            float v1 = velocities.get(i);
            float v2 = velocities.get(i + 1);
            float dt1 = dts.get(i);
            float dt2 = dts.get(i + 1);
            float denom = (dt1 + dt2) * 0.5f;          // average dt, in ms

            if (denom > 0f) {
                float a = (v2 - v1) / denom;           // px/ms^2
                if (a < 0f) {
                    sumDec += Math.abs(a);
                    countDec++;
                }
            }
        }

        return countDec > 0 ? (sumDec / countDec) : 0f;
    }

    /**
     * Calculates the total distance traveled along the stroke path.
     * This is the sum of Euclidean distances between consecutive points.
     * @return The total path length in pixels. Returns 0 if the stroke has fewer than 2 points.
     */
    public float calculateTrajectoryLength() {
        if (points.size() < 2) return 0;

        float totalDistance = 0;
        for (int i = 1; i < points.size(); i++) {
            totalDistance += calculateDistance(points.get(i - 1), points.get(i));
        }
        return totalDistance;
    }

    /**
     * Computes the perpendicular distance from a point to the line segment
     * defined by start and end points, in pixels.
     * If the segment is degenerate (start == end), returns distance to start.
     *
     * @param p The point whose distance to the segment is measured.
     * @param start The starting point of the segment.
     * @param end The ending point of the segment.
     * @return The perpendicular distance in pixels.
     */
    private float perpendicularDistanceToSegment(TouchPoint p, TouchPoint start, TouchPoint end) {
        float dx = end.x - start.x;
        float dy = end.y - start.y;
        float lengthSq = dx * dx + dy * dy;

        if (lengthSq == 0f) { // start and end coincide
            return calculateDistance(p, start);
        }

        // Project p onto the line (start->end), clamped to the segment [0,1]
        float t = ((p.x - start.x) * dx + (p.y - start.y) * dy) / lengthSq;
        t = Math.max(0f, Math.min(1f, t));

        float projX = start.x + t * dx;
        float projY = start.y + t * dy;

        float diffX = p.x - projX;
        float diffY = p.y - projY;

        return (float) Math.hypot(diffX, diffY);
    }


    /**
     * Calculates the average path deviation (curvature) from the straight line
     * connecting the first and last points of the stroke.
     * The deviation is computed as the average perpendicular distance of all
     * interior points to the end-to-end line segment.
     *
     * Units: pixels.
     *
     * @return The average deviation in pixels. Returns 0 if the stroke has fewer than 3 points.
     */
    public float calculateAveragePathDeviation() {
        if (points.size() < 3) {
            return 0f;
        }

        TouchPoint start = points.get(0);
        TouchPoint end = points.get(points.size() - 1);

        float sum = 0f;
        int count = 0;

        // Only interior points contribute to deviation
        for (int i = 1; i < points.size() - 1; i++) {
            sum += perpendicularDistanceToSegment(points.get(i), start, end);
            count++;
        }

        return count > 0 ? (sum / count) : 0f;
    }

    /**
     * Calculates a scale-invariant velocity variance using the squared coefficient of variation (CV^2).
     * This is variance(v) / mean(v)^2, which is unitless and invariant to overall speed scaling.
     *
     * Steps:
     *  - Build per-segment speeds v = distance / dt (px/ms).
     *  - Ignore segments with very small dt or tiny movement to reduce jitter.
     *  - Compute sample mean and sample variance (Welford's algorithm).
     *  - Return CV^2 = variance / (mean*mean).
     *
     * @return Unitless scale-invariant velocity variance (CV^2). Returns 0 if insufficient data.
     */
    public float calculateVelocityVariance() {
        if (points.size() < 2) {
            return 0f;
        }

        // Minimal guards (tune lightly if needed)
        final float MIN_DT_MS   = 5f;  // ignore dt < 5 ms
        final float MIN_DIST_PX = 2f;  // ignore movement < 2 px

        // First pass: collect valid speeds and compute mean/variance via Welford
        int n = 0;
        float mean = 0f;
        float m2   = 0f;

        for (int i = 1; i < points.size(); i++) {
            float dist = calculateDistance(points.get(i - 1), points.get(i));
            long  dtMs = points.get(i).timestamp - points.get(i - 1).timestamp;

            if (dtMs <= 0) continue;
            if (dtMs < (long) MIN_DT_MS) continue;
            if (dist < MIN_DIST_PX) continue;

            float v = dist / (float) dtMs; // px/ms

            // Welford updates
            n += 1;
            float delta  = v - mean;
            mean += delta / n;
            float delta2 = v - mean;
            m2 += delta * delta2;
        }

        if (n < 2 || mean <= 0f) {
            return 0f;
        }

        float var = m2 / (n - 1);      // sample variance (px/ms)^2
        return var / (mean * mean);    // CV^2 (unitless), scale-invariant
    }

    /**
     * Calculates the direction change frequency ("angleChangeRate") using a classic
     * Freeman chain-code approach:
     *
     * Steps:
     *  1) Resample the stroke at a fixed spatial step (e.g., 5 px) to reduce speed effects.
     *  2) Quantize each step's heading into one of 8 compass bins (0..7).
     *  3) Count direction changes when the quantized bin changes and persists
     *     for a few steps (debounce) to avoid jitter double-counting.
     *  4) Normalize by total duration => changes per second.
     *
     * Units: changes per second.
     *
     * @return Direction change frequency (changes/s). Returns 0 if insufficient data.
     */
    public float calculateAngleChangeRate() {
        // Need at least 3 raw points to form 2 steps
        if (points.size() < 3) return 0f;

        // --- Parameters ---
        final float STEP_PX        = 5f;  // spatial resampling step
        final int   PERSIST_STEPS  = 2;   // require new bin to persist this many steps
        final float MIN_STEP_PX    = 1.0f;

        // 1) Resample path by distance (approx. equal-length steps)
        List<ResampledPoint> sp = resampleByDistance(STEP_PX);
        if (sp.size() < 3) return 0f;

        // 2) Quantize headings into 8-direction Freeman chain-code bins
        final int m = sp.size();
        int[] bins = new int[m - 1];
        for (int i = 1; i < m; i++) {
            float dx = sp.get(i).x - sp.get(i - 1).x;
            float dy = sp.get(i).y - sp.get(i - 1).y;
            float len = (float) Math.hypot(dx, dy);
            if (len < MIN_STEP_PX) {
                bins[i - 1] = bins[Math.max(0, i - 2)]; // repeat previous bin if step too small
            } else {
                float ang = (float) Math.atan2(dy, dx); // [-π, π]
                bins[i - 1] = quantizeDir8(ang);
            }
        }

        // 3) Count debounced direction transitions
        int changes = 0;
        int currentBin = bins[0];
        int pendingBin = currentBin;
        int persistCount = 0;

        for (int i = 1; i < bins.length; i++) {
            int b = bins[i];
            if (b == currentBin) {
                // same direction, reset any pending switch
                pendingBin = currentBin;
                persistCount = 0;
            } else {
                // potential change: require it to persist PERSIST_STEPS
                if (b == pendingBin) {
                    persistCount++;
                } else {
                    pendingBin = b;
                    persistCount = 1;
                }
                if (persistCount >= PERSIST_STEPS) {
                    changes++;
                    currentBin = pendingBin;
                    persistCount = 0;
                }
            }
        }

        // 4) Normalize by duration
        float durationMs = points.get(points.size() - 1).timestamp - points.get(0).timestamp;
        if (durationMs <= 0f) return 0f;
        return (changes * 1000f) / durationMs;
    }

    /**
     * Quantizes an angle (radians) into one of 8 compass bins (0..7).
     * Bin 0 centered on 0 rad (east), increasing counter-clockwise.
     */
    private int quantizeDir8(float ang) {
        // Map [-π, π] to [0, 2π), then to 8 bins
        if (ang < 0) ang += (float) (2.0 * Math.PI);
        float sector = (float) (2.0 * Math.PI / 8.0);
        int bin = (int) Math.floor((ang + sector * 0.5f) / sector);
        if (bin >= 8) bin = 0;
        return bin;
    }

    /**
     * Helper container for resampled points.
     */
    private static class ResampledPoint {
        final float x;
        final float y;
        final long tMs;
        final float p; // normalized pressure

        ResampledPoint(float x, float y, long tMs, float p) {
            this.x = x; this.y = y; this.tMs = tMs; this.p = p;
        }
    }

    /**
     * Resamples the stroke polyline at approximately fixed spatial steps.
     * Preserves timing by linearly interpolating timestamps along segments.
     * Also interpolates pressure between points.
     *
     * @param stepPx Desired step length in pixels.
     * @return List of resampled points (>= 2 if there was movement).
     */
    private List<ResampledPoint> resampleByDistance(float stepPx) {
        List<ResampledPoint> out = new ArrayList<>();
        if (points.isEmpty()) return out;

        TouchPoint p0 = points.get(0);
        out.add(new ResampledPoint(p0.x, p0.y, p0.timestamp, p0.pressure));

        float carry = 0f;

        for (int i = 1; i < points.size(); i++) {
            TouchPoint a = points.get(i - 1);
            TouchPoint b = points.get(i);
            float segDx = b.x - a.x;
            float segDy = b.y - a.y;
            float segLen = (float) Math.hypot(segDx, segDy);
            if (segLen <= 0f) continue;

            float ux = segDx / segLen;
            float uy = segDy / segLen;

            float placedFromA = 0f;
            while (placedFromA + (stepPx - carry) <= segLen) {
                float d = placedFromA + (stepPx - carry);
                float rx = a.x + ux * d;
                float ry = a.y + uy * d;

                float ratio = d / segLen;
                long rt = a.timestamp + (long) ((b.timestamp - a.timestamp) * ratio);
                float rp = a.pressure + (b.pressure - a.pressure) * ratio;

                out.add(new ResampledPoint(rx, ry, rt, rp));

                placedFromA = d;
                carry = 0f;
            }

            float remaining = segLen - placedFromA;
            carry = Math.min(stepPx, carry + remaining);
        }

        TouchPoint last = points.get(points.size() - 1);
        ResampledPoint tail = out.get(out.size() - 1);
        if (tail.x != last.x || tail.y != last.y) {
            out.add(new ResampledPoint(last.x, last.y, last.timestamp, last.pressure));
        }

        return out;
    }

    /**
     * Calculates the pressure change frequency ("pressureChangeRate") as the average
     * absolute difference in pressure between consecutive resampled points,
     * normalized by total duration.
     *
     * Steps:
     *  1) Resample the stroke by distance (e.g., 5 px steps) to remove speed bias.
     *  2) Compute absolute pressure differences between consecutive samples.
     *  3) Sum them, divide by duration to yield an average change rate (Δpressure/sec).
     *
     * Units: changes per second (Δpressure per second).
     *
     * @return The average rate of pressure change (changes/s). Returns 0 if insufficient data.
     * Assumption: Pressure is in [0,1].
     */
    public float calculatePressureChangeRate() {
        if (points.size() < 3) return 0f;

        // --- Parameters ---
        final float STEP_PX = 5f;   // resampling step (pixels)

        // 1) Resample stroke by distance (pressure interpolated)
        List<ResampledPoint> sp = resampleByDistance(STEP_PX);
        if (sp.size() < 3) return 0f;

        // 2) Compute total absolute change in pressure
        float totalChange = 0f;
        for (int i = 1; i < sp.size(); i++) {
            totalChange += Math.abs(sp.get(i).p - sp.get(i - 1).p);
        }

        // 3) Normalize by total stroke duration
        float durationMs = points.get(points.size() - 1).timestamp - points.get(0).timestamp;
        if (durationMs <= 0f) return 0f;

        // Convert ms → s to report in changes per second
        return (totalChange * 1000f) / durationMs;
    }

    /**
     * Calculates the pressure variability ("pressureVariance") as the sample variance
     * of per-point pressures across the stroke using Welford's online algorithm.
     *
     * Notes:
     *  - Simple approach (no resampling): uses all recorded touch samples as-is.
     *  - With pressure assumed in [0,1], the variance is in [0, 0.25].
     *
     * @return The sample variance of pressure (unitless). Returns 0 if fewer than 2 points.
     * Assumption: Pressure is in [0,1].
     */
    public float calculatePressureVariance() {
        if (points.size() < 2) return 0f;

        int n = 0;
        float mean = 0f;
        float m2 = 0f;

        for (TouchPoint p : points) {
            float x = p.pressure;
            n += 1;
            float delta = x - mean;
            mean += delta / n;
            float delta2 = x - mean;
            m2 += delta * delta2;
        }

        return n > 1 ? (m2 / (n - 1)) : 0f; // sample variance
    }

    /**
     * Calculates the maximum pressure recorded during the stroke.
     * @return The highest pressure observed. Returns 0 if the stroke has no points.
     * Assumption: Stick to a 0-1 range, however we are aware different devices can have
     *             different pressure ranges, for simplicity we will ignore this
     */
    public float calculateMaxPressure() {
        if (points.isEmpty()) return 0;

        float max = points.get(0).pressure;
        for (TouchPoint p : points) {
            if (p.pressure > max) {
                max = p.pressure;
            }
        }
        return max;
    }

    /**
     * Calculates the minimum pressure recorded during the stroke.
     * @return The lowest pressure observed. Returns 0 if the stroke has no points
     * Assumption: Stick to a 0-1 range, however we are aware different devices can have
     *             different pressure ranges, for simplicity we will ignore this
     */
    public float calculateMinPressure() {
        if (points.isEmpty()) return 0;

        float min = points.get(0).pressure;
        for (TouchPoint p : points) {
            if (p.pressure < min) {
                min = p.pressure;
            }
        }
        return min;
    }

    /**
     * Calculates the initial pressure recorded during the stroke.
     * @return The initial pressure observed. Returns 0 if the stroke has no points
     * Assumption: Stick to a 0-1 range, however we are aware different devices can have
     *             different pressure ranges, for simplicity we will ignore this
     */
    public float calculateInitPressure() {
        if (points.isEmpty()) return 0;
        return points.get(0).pressure;
    }

}
