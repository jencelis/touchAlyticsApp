package com.project.touchalytics;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
public class Splatter {

    private ArrayList<Particle> particles;
    private Random random;


    private static final int MAX_PARTICLES = 50;  // Reduced from 80-100
    private static final int CLEANUP_THRESHOLD = 40;  // Clean up early


    public Splatter() {
        particles = new ArrayList<>(MAX_PARTICLES);  // Pre-allocate
        random = new Random();
    }

    public void createSplatter(float x, float y, int color, int particleCount) {

        if (particles.size() >= MAX_PARTICLES) {
            return;  // Don't create if at limit
        }

        // Reduce particle count if close to limit
        int actualCount = Math.min(particleCount, MAX_PARTICLES - particles.size());
        actualCount = Math.min(actualCount, 6);  // Never create more than 6 at once


        for (int i = 0; i < actualCount; i++) {
            float angle = random.nextFloat() * 360;
            float speed = 5 + random.nextFloat() * 10;  // Reduced from 15

            float velocityX = (float) Math.cos(Math.toRadians(angle)) * speed;
            float velocityY = (float) Math.sin(Math.toRadians(angle)) * speed;

            float size = 5 + random.nextFloat() * 8;  // Reduced from 10

            particles.add(new Particle(x, y, velocityX, velocityY, size, color));
        }
    }

    public void update() {
        // Early cleanup
        if (particles.size() > CLEANUP_THRESHOLD) {
            // Remove oldest particles first
            particles.removeIf(p -> p.alpha <= 50 || p.y > 3000);
        }


        Iterator<Particle> iterator = particles.iterator();
        while (iterator.hasNext()) {
            Particle particle = iterator.next();
            particle.update();

            // Remove faded or off-screen particles
            if (particle.alpha <= 0 || particle.y > 2500) {
                iterator.remove();
            }
        }
    }

    public void draw(Canvas canvas, Paint paint) {
        // Only draw visible particles
        for (Particle particle : particles) {
            if (particle.alpha > 20) {  // Don't draw nearly invisible particles
                particle.draw(canvas, paint);
            }
        }
    }

    public void clear() {
        particles.clear();
    }

    public int getParticleCount() {
        return particles.size();
    }

    private class Particle {
        float x, y;
        float velocityX, velocityY;
        float size;
        int color;
        int alpha = 255;
        float gravity = 0.6f;  // Increased from 0.5 (falls faster)

        Particle(float x, float y, float velocityX, float velocityY, float size, int color) {
            this.x = x;
            this.y = y;
            this.velocityX = velocityX;
            this.velocityY = velocityY;
            this.size = size;
            this.color = color;
        }

        void update() {
            x += velocityX;
            y += velocityY;
            velocityY += gravity;
            velocityX *= 0.96f;  // More air resistance (was 0.98)

            // faster fade
            alpha -= 18;  // Much faster (was 8-12)


            if (alpha < 0) alpha = 0;
            size *= 0.95f;  // Shrink faster (was 0.97)
        }

        void draw(Canvas canvas, Paint paint) {
            paint.setColor(color);
            paint.setAlpha(alpha);
            canvas.drawCircle(x, y, size, paint);
            paint.setAlpha(255);
        }
    }
}