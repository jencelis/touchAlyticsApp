package com.project.touchalytics;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

public class Fruit {

    public float x, y;
    private float velocityX, velocityY;
    private float gravity = 0.8f;
    private int radius = 100;
    private int color;

    private boolean sliced = false;
    private int sliceAlpha = 255;


    public enum FruitType {
        APPLE, BANANA, COCONUT, STARFRUIT, BOMB
    }


    private FruitType type;
    private boolean isBomb = false;

    // image support
    private static Context context;
    private Bitmap wholeImage;
    private Bitmap slicedImage;
    private boolean useImages = true;



    private static Bitmap appleWhole = null;
    private static Bitmap appleSliced = null;
    private static Bitmap bananaWhole = null;
    private static Bitmap bananaSliced = null;
    private static Bitmap coconutWhole = null;
    private static Bitmap coconutSliced = null;
    private static Bitmap starfruitWhole = null;
    private static Bitmap starfruitSliced = null;
    private static Bitmap bombWhole = null;
    private static Bitmap bombSliced = null;
    private static boolean imagesLoaded = false;


    // Static method to set context (call this once from GameView)
    public static void setContext(Context ctx) {
        context = ctx;
    }

    public Fruit(float x, float y, float velocityX, float velocityY, int color) {
        this(x, y, velocityX, velocityY, color, FruitType.APPLE);
    }

    public Fruit(float x, float y, float velocityX, float velocityY, int color, FruitType type) {
        this.x = x;
        this.y = y;
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.color = color;
        this.type = type;
        this.isBomb = (type == FruitType.BOMB);

        // Set different sizes for different fruits
        switch (type) {
            case COCONUT:
                this.radius = 110;  // Coconuts are bigger
                break;
            case STARFRUIT:
                this.radius = 95;   // Starfruit medium
                break;
            case BANANA:
                this.radius = 105;  // Bananas are long/big
                break;
            case APPLE:
                this.radius = 90;   // Apples medium
                break;
            case BOMB:
                this.radius = 95;   // Bombs medium
                this.color = Color.BLACK;
                break;
            default:
                this.radius = 100;  // Default
        }


        // Load images
        loadImages();
    }

    private void loadImages() {
        if (context == null) {
            useImages = false;
            return;
        }

        // ===== LOAD ONLY ONCE (STATIC) =====
        if (!imagesLoaded) {
            try {
                appleWhole = BitmapFactory.decodeResource(context.getResources(), R.drawable.apple);
                appleSliced = BitmapFactory.decodeResource(context.getResources(), R.drawable.apple_sliced);
                bananaWhole = BitmapFactory.decodeResource(context.getResources(), R.drawable.banana);
                bananaSliced = BitmapFactory.decodeResource(context.getResources(), R.drawable.banana_sliced);
                coconutWhole = BitmapFactory.decodeResource(context.getResources(), R.drawable.coconut);
                coconutSliced = BitmapFactory.decodeResource(context.getResources(), R.drawable.coconut_sliced);
                starfruitWhole = BitmapFactory.decodeResource(context.getResources(), R.drawable.starfruit);
                starfruitSliced = BitmapFactory.decodeResource(context.getResources(), R.drawable.starfruit_sliced);
                bombWhole = BitmapFactory.decodeResource(context.getResources(), R.drawable.bomb);
                bombSliced = BitmapFactory.decodeResource(context.getResources(), R.drawable.bomb);

                imagesLoaded = true;
                System.out.println("✓ All images loaded once (static)");
            } catch (Exception e) {
                e.printStackTrace();
                useImages = false;
            }
        }
        // ===================================

        // Assign to instance variables
        switch (type) {
            case APPLE:
                wholeImage = appleWhole;
                slicedImage = appleSliced;
                break;
            case BANANA:
                wholeImage = bananaWhole;
                slicedImage = bananaSliced;
                break;
            case COCONUT:
                wholeImage = coconutWhole;
                slicedImage = coconutSliced;
                break;
            case STARFRUIT:
                wholeImage = starfruitWhole;
                slicedImage = starfruitSliced;
                break;
            case BOMB:
                wholeImage = bombWhole;
                slicedImage = bombSliced;
                break;
        }

        useImages = (wholeImage != null && slicedImage != null);
    }


    public void update() {
        if (!sliced) {
            // Apply gravity
            velocityY += gravity;

            // Update position
            x += velocityX;
            y += velocityY;
        } else {
            // Fade out animation when sliced
            sliceAlpha -= 10;
            if (sliceAlpha < 0) sliceAlpha = 0;
        }
    }

    public void draw(Canvas canvas, Paint paint) {
        paint.setAlpha(sliceAlpha);

        if (useImages && wholeImage != null && slicedImage != null) {
            // Draw with images
            drawWithImages(canvas, paint);
        } else {
            // Fallback to circles
            drawWithCircles(canvas, paint);
        }

        // Reset alpha
        paint.setAlpha(255);
    }

    private void drawWithImages(Canvas canvas, Paint paint) {
        // Choose which image to draw
        Bitmap imageToDraw = sliced ? slicedImage : wholeImage;

        if (imageToDraw == null) return;

        // Calculate the rectangle to draw the image in
        Rect destRect = new Rect(
                (int)(x - radius),      // left
                (int)(y - radius),      // top
                (int)(x + radius),      // right
                (int)(y + radius)       // bottom
        );

        // Draw the image
        canvas.drawBitmap(imageToDraw, null, destRect, paint);
    }

    private void drawWithCircles(Canvas canvas, Paint paint) {
        // Original circle drawing code (fallback if images don't load)
        paint.setColor(color);

        if (!sliced) {
            // Draw whole fruit/bomb
            canvas.drawCircle(x, y, radius, paint);

            if (isBomb) {
                // Draw bomb fuse
                paint.setColor(Color.RED);
                paint.setStrokeWidth(8);
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawLine(x, y - radius, x, y - radius - 30, paint);
                paint.setStyle(Paint.Style.FILL);

                // Draw "X" on bomb
                paint.setColor(Color.RED);
                paint.setStrokeWidth(10);
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawLine(x - 30, y - 30, x + 30, y + 30, paint);
                canvas.drawLine(x - 30, y + 30, x + 30, y - 30, paint);
                paint.setStyle(Paint.Style.FILL);
            } else {
                // Add a small highlight for 3D effect
                paint.setColor(0xFFFFFFFF);
                paint.setAlpha(100);
                canvas.drawCircle(x - radius / 3, y - radius / 3, radius / 3, paint);
            }
        } else {
            if (isBomb) {
                // Draw explosion effect
                paint.setColor(Color.RED);
                paint.setAlpha(sliceAlpha);
                canvas.drawCircle(x, y, radius * 1.5f, paint);
                paint.setColor(Color.YELLOW);
                canvas.drawCircle(x, y, radius * 1.2f, paint);
                paint.setColor(Color.rgb(255, 165, 0));
                canvas.drawCircle(x, y, radius * 0.8f, paint);
            } else {
                // Draw sliced fruit halves
                canvas.drawCircle(x - 20, y, radius * 0.8f, paint);
                canvas.drawCircle(x + 20, y, radius * 0.8f, paint);
            }
        }
    }

    public void slice() {
        sliced = true;
    }

    public boolean isSliced() {
        return sliced;
    }

    public boolean sliceAnimationComplete() {
        return sliceAlpha <= 0;
    }

    public float getRadius() {
        return radius;
    }

    public boolean isBomb() {
        return isBomb;
    }

    public FruitType getType() {
        return type;
    }


    public int getPoints() {
        switch (type) {
            case COCONUT:
                return 15;  // Hardest to crack = most points!
            case STARFRUIT:
                return 12;  // Rare = good points
            case BANANA:
                return 10;  // Normal points
            case APPLE:
                return 10;  // Normal points
            default:
                return 10;
        }
    }

    public int getSplatterColor() {
        switch (type) {
            case APPLE:
                return Color.rgb(220, 20, 60);  // Crimson red juice
            case BANANA:
                return Color.rgb(255, 215, 0);  // Golden yellow juice
            case COCONUT:
                return Color.rgb(245, 245, 245); // White coconut milk
            case STARFRUIT:
                return Color.rgb(255, 215, 100); // Yellow-orange juice
            case BOMB:
                return Color.rgb(50, 50, 50);   // Dark smoke
            default:
                return Color.RED;
        }
    }
}