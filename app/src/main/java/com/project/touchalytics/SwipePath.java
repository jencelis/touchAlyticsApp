package com.project.touchalytics;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import java.util.ArrayList;
import android.util.Log;

public class SwipePath {

    private ArrayList<Point> points;
    private Path path;
    private boolean isActive;
    private int fadeAlpha = 255;

    // TESTING:
   // private int totalSwipes = 0;
   // private int earlyExits = 0;         // Hits found in first 5 segments
    //private static int totalHits = 0;      // Successful collisions
   // private static int totalMisses = 0;    // Paths that didn't hit this fruit
  // private static int totalUserSwipes = 0;


    public SwipePath() {
        points = new ArrayList<>();
        path = new Path();
        isActive = false;
    }

    public void startPath(float x, float y) {
        points.clear();
        points.add(new Point(x, y));
        path.reset();
        path.moveTo(x, y);
        isActive = true;
        fadeAlpha = 255;
    }

    public void addPoint(float x, float y) {
        if (isActive) {
            points.add(new Point(x, y));
            path.lineTo(x, y);

            // Keep only recent points to prevent memory issues
            if (points.size() > 50) {
                points.remove(0);
            }
        }
    }

    public void endPath() {
        isActive = false;

        /* TESTING ONLY
        // count actual user swipes, not intersects() calls
        if (points.size() >= 2) {
            totalUserSwipes++;
            Log.d("SwipePath", String.format(
                    "=== USER SWIPE #%d ENDED === path_length=%d points",
                    totalUserSwipes, points.size()
            ));
        }*/
    }

    public void draw(Canvas canvas, Paint paint) {
        if (points.size() > 1) {
            // Draw the swipe trail
            paint.setColor(Color.WHITE);
            paint.setStrokeWidth(10);
            paint.setStyle(Paint.Style.STROKE);
            paint.setAlpha(fadeAlpha);
            canvas.drawPath(path, paint);

            // Fade out when not active
            if (!isActive) {
                fadeAlpha -= 15;
                if (fadeAlpha < 0) {
                    fadeAlpha = 0;
                    points.clear();
                    path.reset();
                }
            }

            // Reset paint
            paint.setStyle(Paint.Style.FILL);
            paint.setAlpha(255);
        }
    }

    public boolean intersects(Fruit fruit) {
        if (points.size() < 2) return false;

        //long startTime = System.nanoTime();
        int totalSegments = points.size() - 1;

        // Check if any line segment in the swipe path intersects with the fruit
        for (int i = 0; i < points.size() - 1; i++) {
            Point p1 = points.get(i);
            Point p2 = points.get(i + 1);

            if (lineCircleIntersection(p1.x, p1.y, p2.x, p2.y,
                    fruit.x, fruit.y, fruit.getRadius())) {

                /* RELEVANT FOR TESTING ONLY
                // HIT detected
                long endTime = System.nanoTime();
                double duration = (endTime - startTime) / 1_000_000.0;

                // Track early exits (first 5 segments = indices 0-4)
                boolean isEarlyExit = (i < 5);
                if (isEarlyExit) {
                    earlyExits++;
                }
                totalHits++;

                Log.d("SwipePath", String.format(
                        "HIT | segment=%d/%d | path_len=%d | time=%.5fms | early=%s | earlyRate=%.1f%%",
                        i+1, totalSegments, points.size(), duration, isEarlyExit,
                        (100.0 * earlyExits / totalHits)
                ));
                 */

                return true;
            }
        }
        /* RELEVANT FOR TESTING ONLY
        // MISS - only reached if loop completes without hit
        long endTime = System.nanoTime();
        double duration = (endTime - startTime) / 1_000_000.0;
        totalMisses++;

        Log.d("SwipePath", String.format(
                "MISS | checked_all=%d | path_len=%d | time=%.5fms",
                totalSegments, points.size(), duration
        ));

         */

        return false;
    }
    // Check if a line segment intersects with a circle
    private boolean lineCircleIntersection(float x1, float y1, float x2, float y2,
                                           float cx, float cy, float radius) {
        // Calculate the closest point on the line segment to the circle center
        float dx = x2 - x1;
        float dy = y2 - y1;
        float fx = x1 - cx;
        float fy = y1 - cy;

        float a = dx * dx + dy * dy;
        // Handle edge case: start and end points are the same; a = 0
        // Treat as point instead of a line segment
        if (a == 0) {
            // Check if the point is inside the circle
            float distSquared = fx * fx + fy * fy;
            return distSquared <= radius * radius;
        }

        float b = 2 * (fx * dx + fy * dy);
        float c = (fx * fx + fy * fy) - radius * radius;

        float discriminant = b * b - 4 * a * c;

        if (discriminant < 0) {
            return false; // No intersection
        }

        discriminant = (float) Math.sqrt(discriminant);

        float t1 = (-b - discriminant) / (2 * a);
        float t2 = (-b + discriminant) / (2 * a);

        // Check if intersection is within the line segment (t between 0 and 1)
        return (t1 >= 0 && t1 <= 1) || (t2 >= 0 && t2 <= 1);
    }

    // Inner class to represent a point
    private class Point {
        float x, y;

        Point(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    public void clear() {
        points.clear();
        path.reset();
    }


}