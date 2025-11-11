package com.project.touchalytics;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;

public class GameView extends SurfaceView implements Runnable {

    private Thread gameThread;
    private SurfaceHolder holder;
    private volatile boolean isPlaying;
    private Canvas canvas;
    private Paint paint;

    // Game objects
    private ArrayList<Fruit> fruits;
    private SwipePath swipePath;
    private Random random;
    private Splatter splatter;
    private Bitmap logo;


    // Screen dimensions
    private int screenWidth;
    private int screenHeight;

    // Game state
    private enum GameState {
        MENU, PLAYING, PAUSED, GAME_OVER
    }
    private GameState gameState;
    private GameMode gameMode;

    // Game variables
    private int score;
    private int lives;
    private int combo;
    private int maxCombo;
    private long lastSliceTime;
    private long comboTimeout = 1000; // 1 second to maintain combo (lessen it???)

    private float speedMultiplier = 1.0f; //speed increase

    // Spawn control
    private long lastFruitSpawn;
    private int spawnDelay = 1000;

    // Arcade mode timer
    private long gameStartTime;
    private long gameDuration = 60000; // 60 seconds for arcade
    private long timeRemaining;

    // High scores
    private SharedPreferences prefs;
    private int highScoreClassic;
    private int highScoreArcade;

    // FPS
    private long fps;

    // Menu buttons
    private RectF classicButton;
    private RectF arcadeButton;
    private RectF restartButton;
    private RectF menuButton;

    // Context
    private Context context;

    //Background image
    private Bitmap bgImage;
    private Rect bgRect;


    public GameView(Context context) {
        super(context);
        this.context = context;
        holder = getHolder();
        paint = new Paint();
        paint.setAntiAlias(true);
        fruits = new ArrayList<>();
        swipePath = new SwipePath();
        random = new Random();
        splatter = new Splatter();

        Fruit.setContext(context);

        // Load high scores
        try {
            prefs = context.getSharedPreferences("FruitNinjaPrefs", Context.MODE_PRIVATE);
            highScoreClassic = prefs.getInt("highScoreClassic", 0);
            highScoreArcade = prefs.getInt("highScoreArcade", 0);
        } catch (Exception e) {
            e.printStackTrace();
            highScoreClassic = 0;
            highScoreArcade = 0;
        }
        // ===== LOAD LOGO =====
        try {
            logo = BitmapFactory.decodeResource(
                    getResources(),
                    R.drawable.logo1
            );
            System.out.println("Logo loaded successfully!");
        } catch (Exception e) {
            System.out.println("Logo failed to load");
            e.printStackTrace();
            logo = null;  // Will fallback to text in case of error
        }


        gameState = GameState.MENU;
        gameMode = GameMode.CLASSIC; // Set default mode
        resetGame();

        //load background image
        loadbg();
    }

    private void resetGame() {
        fruits.clear();
        splatter.clear();

        System.gc();   //optimization

        score = 0;
        lives = 3;
        combo = 0;
        maxCombo = 0;
        lastFruitSpawn = System.currentTimeMillis();
        lastSliceTime = System.currentTimeMillis();

        if (gameMode == GameMode.ARCADE) {
            gameStartTime = System.currentTimeMillis();
            spawnDelay = 700; // Faster spawning
        } else {
            spawnDelay = 1000;
        }
    }

    //load background image method
    private void loadbg() {
        try {
            // Load the image from drawable
            bgImage = BitmapFactory.decodeResource(
                    getResources(),
                    R.drawable.game_bg
            );
        } catch (Exception e) {
            e.printStackTrace();
            bgImage = null;  // Fall back to solid color if image fails :(
        }
    }

    private void intializeBg() {
        if (screenWidth == 0 || screenHeight == 0) return;
        if (bgImage == null) return;

        // Create scaled bitmap to match screen size
        bgImage = Bitmap.createScaledBitmap(
                bgImage,
                screenWidth,
                screenHeight,
                true  // filter for better quality
        );
    }

    @Override
    public void run() {
        while (isPlaying) {
            long startTime = System.currentTimeMillis();

            update();
            draw();

            long timeThisFrame = System.currentTimeMillis() - startTime;
            if (timeThisFrame > 0) {
                fps = 1000 / timeThisFrame;
            }

            try {
                Thread.sleep(25); // ~40 FPS
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void update() {
        // Initialize buttons BEFORE checking game state
        if (screenWidth == 0 && getWidth() > 0 && getHeight() > 0) {
            screenWidth = getWidth();
            screenHeight = getHeight();
            initializeButtons();
            intializeBg();
        }

        // Now check game state
        if (gameState != GameState.PLAYING) return;

        splatter.update();

        // Update timer for Arcade mode
        if (gameMode == GameMode.ARCADE) {
            timeRemaining = gameDuration - (System.currentTimeMillis() - gameStartTime);
            if (timeRemaining <= 0) {
                gameOver();
                return;
            }
        }

        // Check combo timeout
        if (System.currentTimeMillis() - lastSliceTime > comboTimeout && combo > 0) {
            combo = 0;
        }

        // Spawn fruits
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFruitSpawn > spawnDelay) {
            spawnFruit();
            lastFruitSpawn = currentTime;
        }

        // Update fruits
        Iterator<Fruit> iterator = fruits.iterator();
        while (iterator.hasNext()) {
            Fruit fruit = iterator.next();
            fruit.update();

            // Remove fruits that fall off screen
            if (fruit.y > screenHeight + 100) {
                iterator.remove();
                if (!fruit.isBomb() && !fruit.isSliced()) {
                    lives--; // Lost a life for missing a fruit
                    combo = 0; // Reset combo
                    if (lives <= 0) {
                        gameOver();
                    }
                }
            }

            // Check collision with swipe path
            if (swipePath.intersects(fruit) && !fruit.isSliced()) {
                fruit.slice();

                // Get splatter color based on fruit type
                int splatterColor;
                switch (fruit.getType()) {
                    case APPLE:
                        splatterColor = Color.rgb(220, 20, 60);  // Red juice
                        break;
                    case COCONUT:
                        splatterColor = Color.rgb(255, 20, 147); // Pink juice
                        break;
                    case BANANA:
                        splatterColor = Color.rgb(255, 215, 0);  // Yellow juice
                        break;
                    case STARFRUIT:
                        splatterColor = Color.rgb(148, 0, 211);  // Purple juice
                        break;
                    case BOMB:
                        splatterColor = Color.rgb(50, 50, 50);   // Dark smoke
                        break;
                    default:
                        splatterColor = Color.RED;
                }

                splatter.createSplatter(fruit.x, fruit.y, splatterColor, 8); // 15 particles reduced for optimization

                if (fruit.isBomb()) {
                    // Hit a bomb - game over in Classic, lose points in Arcade
                    if (gameMode == GameMode.CLASSIC) {
                        gameOver();
                    } else {
                        score = Math.max(0, score - 20); // Lose 20 points
                    }
                    combo = 0;
                } else {
                    // Hit a fruit
                    combo++;
                    if (combo > maxCombo) maxCombo = combo;
                    lastSliceTime = System.currentTimeMillis();

                    int points = fruit.getPoints();
                    if (combo > 1) {
                        points += combo; // Bonus points for combo
                    }
                    score += points;
                }
            }

            // Remove sliced fruits after animation
            if (fruit.isSliced() && fruit.sliceAnimationComplete()) {
                iterator.remove();
            }
        }
    }

    private void draw() {
        if (!holder.getSurface().isValid()) return;

        try {
            canvas = holder.lockCanvas();
            if (canvas == null) return;

            // Draw background
            if (bgImage != null) {
                canvas.drawBitmap(bgImage, 0, 0, paint);
            } else {
                canvas.drawColor(Color.rgb(135, 206, 235));
            }

            // Show loading if not initialized
            if (screenWidth == 0) {
                paint.setColor(Color.WHITE);
                paint.setTextSize(60);
                paint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText("Loading...", canvas.getWidth() / 2f, canvas.getHeight() / 2f, paint);
            } else {
                switch (gameState) {
                    case MENU:
                        drawMenu();
                        break;
                    case PLAYING:
                        drawGame();
                        break;
                    case GAME_OVER:
                        drawGameOver();
                        break;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (canvas != null) {
                holder.unlockCanvasAndPost(canvas);
            }
        }
    }

    private void drawMenu() {
        // ===== DRAW LOGO =====
        if (logo != null) {
            int logoWidth = screenWidth/2 + 150;
            int logoHeight = screenHeight/2 - 550;
            int logoX = (int)(screenWidth / 2f - logoWidth / 2);
            int logoY = (int)(screenHeight / 4f - logoHeight / 2);

            Rect logoRect = new Rect(logoX, logoY, logoX + logoWidth, logoY + logoHeight);
            canvas.drawBitmap(logo, null, logoRect, paint);
        } else {
            // Fallback to text if logo doesn't load
            paint.setColor(Color.WHITE);
            paint.setTextSize(120);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("FRUIT NINJA", screenWidth / 2f, screenHeight / 4f, paint);
        }

        paint.setTextSize(60);
        canvas.drawText("Select Mode", (screenWidth / 2f - 200), (screenHeight /3f + 250), paint);

        // Classic Mode Button
        if (classicButton != null) {
            paint.setColor(Color.rgb(76, 175, 80));
            canvas.drawRoundRect(classicButton, 20, 20, paint);
            paint.setColor(Color.WHITE);
            paint.setTextSize(70);
            canvas.drawText("CLASSIC", screenWidth / 2.5f, screenHeight / 2f , paint);
            paint.setTextSize(40);
            canvas.drawText("Avoid Bombs - 3 Lives", screenWidth / 3.1f, screenHeight / 2f + 60, paint);
        }

        // Arcade Mode Button
        if (arcadeButton != null) {
            paint.setColor(Color.rgb(244, 67, 54));
            canvas.drawRoundRect(arcadeButton, 20, 20, paint);
            paint.setColor(Color.WHITE);
            paint.setTextSize(70);
            canvas.drawText("ARCADE", screenWidth / 2.5f, screenHeight / 2f +250, paint);
            paint.setTextSize(40);
            canvas.drawText("60 Seconds - Bonus Fruits", screenWidth / 3.2f, screenHeight / 2f + 310, paint);
        }

        // High Scores
        paint.setTextSize(50);
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("High Scores:", 50, screenHeight - 200, paint);
        paint.setTextSize(40);
        canvas.drawText("Classic: " + highScoreClassic, 50, screenHeight - 140, paint);
        canvas.drawText("Arcade: " + highScoreArcade, 50, screenHeight - 90, paint);

        paint.setTextAlign(Paint.Align.LEFT); // Reset alignment
    }

    private void drawGame() {
        // Draw fruits
        for (Fruit fruit : fruits) {
            fruit.draw(canvas, paint);
        }

        splatter.draw(canvas, paint);

        // Draw swipe path
        swipePath.draw(canvas, paint);

        // Draw HUD
        paint.setColor(Color.WHITE);
        paint.setTextSize(60);
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("Score: " + score, 50, 100, paint);

        if (gameMode == GameMode.CLASSIC) {
            canvas.drawText("Lives: " + lives, 50, 200, paint);
        } else {
            int secondsLeft = (int) (timeRemaining / 1000);
            canvas.drawText("Time: " + secondsLeft + "s", 50, 200, paint);
        }

        // Draw combo
        if (combo > 1) {
            paint.setTextSize(80);
            paint.setColor(Color.YELLOW);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("COMBO x" + combo + "!", screenWidth / 2f, 150, paint);
        }

        // Draw FPS (for debugging)
        paint.setColor(Color.WHITE);
        paint.setTextSize(40);
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("FPS: " + fps, 50, screenHeight - 50, paint);

        paint.setTextAlign(Paint.Align.LEFT); // Reset alignment
    }

    private void drawGameOver() {
        // Draw final fruits (fading out)
        for (Fruit fruit : fruits) {
            fruit.draw(canvas, paint);
        }

        // Semi-transparent overlay
        paint.setColor(Color.argb(200, 0, 0, 0));
        canvas.drawRect(0, 0, screenWidth, screenHeight, paint);

        // Game Over text
        paint.setColor(Color.RED);
        paint.setTextSize(120);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("GAME OVER", screenWidth / 2f, screenHeight / 4f, paint);

        // Final score
        paint.setColor(Color.WHITE);
        paint.setTextSize(70);
        canvas.drawText("Final Score: " + score, screenWidth / 2f, screenHeight / 3f + 100, paint);

        // Max combo
        if (maxCombo > 1) {
            paint.setTextSize(50);
            canvas.drawText("Best Combo: x" + maxCombo, screenWidth / 2f, screenHeight / 3f + 180, paint);
        }

        // High score notification
        boolean isHighScore = false;
        if (gameMode == GameMode.CLASSIC && score > highScoreClassic) {
            isHighScore = true;
            highScoreClassic = score;
            if (prefs != null) {
                prefs.edit().putInt("highScoreClassic", highScoreClassic).apply();
            }
        } else if (gameMode == GameMode.ARCADE && score > highScoreArcade) {
            isHighScore = true;
            highScoreArcade = score;
            if (prefs != null) {
                prefs.edit().putInt("highScoreArcade", highScoreArcade).apply();
            }
        }

        if (isHighScore) {
            paint.setColor(Color.YELLOW);
            paint.setTextSize(60);
            canvas.drawText("NEW HIGH SCORE!", screenWidth / 2f, screenHeight / 3f + 260, paint);
        }

        // Buttons
        if (restartButton != null) {
            paint.setColor(Color.rgb(76, 175, 80));
            canvas.drawRoundRect(restartButton, 20, 20, paint);
            paint.setColor(Color.WHITE);
            paint.setTextSize(60);
            canvas.drawText("NEW GAME", screenWidth / 2f, screenHeight / 2f + 200, paint);
        }

        if (menuButton != null) {
            paint.setColor(Color.rgb(33, 150, 243));
            canvas.drawRoundRect(menuButton, 20, 20, paint);
            paint.setColor(Color.WHITE);
            paint.setTextSize(60);
            canvas.drawText("MENU", screenWidth / 2f, screenHeight / 2f + 350, paint);
        }

        paint.setTextAlign(Paint.Align.LEFT); // Reset alignment
    }

    private void initializeButtons() {
        if (screenWidth == 0 || screenHeight == 0) return;

        float buttonWidth = screenWidth * 0.7f;
        float buttonHeight = 200;
        float centerX = screenWidth / 2f;

        classicButton = new RectF(
                centerX - buttonWidth / 2,
                screenHeight / 2f - 100,
                centerX + buttonWidth / 2,
                screenHeight / 2f + buttonHeight - 100
        );

        arcadeButton = new RectF(
                centerX - buttonWidth / 2,
                screenHeight / 2f + 150,
                centerX + buttonWidth / 2,
                screenHeight / 2f + buttonHeight + 150
        );

        restartButton = new RectF(
                centerX - buttonWidth / 2,
                screenHeight / 2f + 100,
                centerX + buttonWidth / 2,
                screenHeight / 2f + buttonHeight + 100
        );

        menuButton = new RectF(
                centerX - buttonWidth / 2,
                screenHeight / 2f + 250,
                centerX + buttonWidth / 2,
                screenHeight / 2f + buttonHeight + 200
        );
    }


    private void spawnFruit() {
        if (screenWidth == 0 || screenHeight == 0) return;

        int x, startY;
        float velocityX, velocityY;

        // ===== RANDOM SPAWN LOCATIONS (4 types) =====
        int spawnType = random.nextInt(4);

        switch (spawnType) {
            case 0: // Spawn from BOTTOM (classic fruit ninja style)
                x = random.nextInt(screenWidth - 100) + 50;
                startY = screenHeight + 50;
                velocityX = random.nextFloat() * 6 - 3;         // -3 to 3 (drift)
                velocityY = -25 - random.nextFloat() * 15;      // -25 to -40 (shoot up!)
                break;

            case 1: // Spawn from LEFT side
                x = -50;  // Off-screen left
                startY = screenHeight / 2 + random.nextInt(screenHeight / 3);
                velocityX = 10 + random.nextFloat() * 8;        // 10 to 18 (fast right)
                velocityY = -15 - random.nextFloat() * 10;      // -15 to -25 (arc up)
                break;

            case 2: // Spawn from RIGHT side
                x = screenWidth + 50;  // Off-screen right
                startY = screenHeight / 2 + random.nextInt(screenHeight / 3);
                velocityX = -10 - random.nextFloat() * 8;       // -10 to -18 (fast left)
                velocityY = -15 - random.nextFloat() * 10;      // -15 to -25 (arc up)
                break;

            case 3: // Spawn from TOP (drop down)
                x = random.nextInt(screenWidth - 100) + 50;
                startY = -50;  // Off-screen top
                velocityX = random.nextFloat() * 10 - 5;        // -5 to 5 (drift)
                velocityY = 5 + random.nextFloat() * 5;         // 5 to 10 (drop slowly)
                break;

            default: // Fallback to bottom spawn
                x = random.nextInt(screenWidth - 100) + 50;
                startY = screenHeight + 50;
                velocityX = random.nextFloat() * 6 - 3;
                velocityY = -25 - random.nextFloat() * 15;
                break;
        }

        // ===== BOMB SPAWN LOGIC =====
        boolean spawnBomb = false;
        if (gameMode == GameMode.CLASSIC) {
            spawnBomb = random.nextFloat() < 0.15;  // 15% chance in Classic
        } else {
            spawnBomb = random.nextFloat() < 0.10;  // 10% chance in Arcade
        }

        if (spawnBomb) {
            // Spawn a bomb
            fruits.add(new Fruit(x, startY, velocityX, velocityY, Color.BLACK, Fruit.FruitType.BOMB));
        } else {
            // ===== FRUIT TYPE SELECTION =====
            Fruit.FruitType[] types = {
                    Fruit.FruitType.APPLE,
                    Fruit.FruitType.COCONUT,
                    Fruit.FruitType.STARFRUIT,
                    Fruit.FruitType.BANANA
            };

            // Bonus: 10% chance for guaranteed banana in Arcade mode
            if (gameMode == GameMode.ARCADE && random.nextFloat() < 0.10) {
                types = new Fruit.FruitType[]{Fruit.FruitType.BANANA};
            }

            // Pick random fruit type
            Fruit.FruitType type = types[random.nextInt(types.length)];

            // colors for fallback circles
            int color;
            switch (type) {
                case APPLE:
                    color = Color.RED;
                    break;
                case COCONUT:
                    color = Color.rgb(139, 69, 19);  // Brown
                    break;
                case STARFRUIT:
                    color = Color.rgb(255, 215, 0);  // Gold/yellow
                    break;
                case BANANA:
                    color = Color.YELLOW;
                    break;
                default:
                    color = Color.RED;
            }

            // Create and add the fruit
            fruits.add(new Fruit(x, startY, velocityX, velocityY, color, type));
        }
    }


    private void gameOver() {
        gameState = GameState.GAME_OVER;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float touchX = event.getX();
        float touchY = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (gameState == GameState.MENU) {
                    if (classicButton != null && classicButton.contains(touchX, touchY)) {
                        gameMode = GameMode.CLASSIC;
                        gameState = GameState.PLAYING;
                        resetGame();
                    } else if (arcadeButton != null && arcadeButton.contains(touchX, touchY)) {
                        gameMode = GameMode.ARCADE;
                        gameState = GameState.PLAYING;
                        resetGame();
                    }
                } else if (gameState == GameState.GAME_OVER) {
                    if (restartButton != null && restartButton.contains(touchX, touchY)) {
                        gameState = GameState.PLAYING;
                        resetGame();
                    } else if (menuButton != null && menuButton.contains(touchX, touchY)) {
                        gameState = GameState.MENU;
                        fruits.clear();
                    }
                } else if (gameState == GameState.PLAYING) {
                    swipePath.startPath(touchX, touchY);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (gameState == GameState.PLAYING) {
                    swipePath.addPoint(touchX, touchY);
                }
                break;
            case MotionEvent.ACTION_UP:
                if (gameState == GameState.PLAYING) {
                    swipePath.endPath();
                }
                break;
        }
        return true;
    }

    public void pause() {
        isPlaying = false;
        try {
            if (gameThread != null) {
                gameThread.join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void resume() {
        isPlaying = true;
        gameThread = new Thread(this);
        gameThread.start();
    }
}