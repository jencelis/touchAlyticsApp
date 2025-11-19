package com.project.touchalytics;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import java.util.ArrayList;

public class SwipePath {

    private ArrayList<Point> points;
    private Path path;
    private boolean isActive;
    private int fadeAlpha = 255;

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

        // Check if any line segment in the swipe path intersects with the fruit
        for (int i = 0; i < points.size() - 1; i++) {
            Point p1 = points.get(i);
            Point p2 = points.get(i + 1);

            if (lineCircleIntersection(p1.x, p1.y, p2.x, p2.y,
                    fruit.x, fruit.y, fruit.getRadius())) {
                return true;
            }
        }
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
}