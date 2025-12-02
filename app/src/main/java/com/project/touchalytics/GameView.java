package com.project.touchalytics;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Build;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.util.Log;
import android.util.AttributeSet;
import android.graphics.Typeface;
import androidx.core.content.res.ResourcesCompat;

public class  GameView extends SurfaceView implements Runnable {

    private static final String TAG = "FruitNinja";
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


    // Dynamic difficulty variables
    private float baseSpawnDelay = 1000f;  // Starting spawn delay in ms
    private float currentSpawnDelay;       // Current adjusted spawn delay
    private float minSpawnDelay = 300f;    // Minimum spawn delay (maximum difficulty)
    private float baseFruitSpeed = 25f;    // Starting upward velocity
    private float currentFruitSpeed;       // Current adjusted fruit speed
    private float maxFruitSpeed = 40f;     // Maximum fruit speed
    private float difficultyMultiplier = 1.0f;  // Overall difficulty scale

    // Spawn control
    private long lastFruitSpawn;
    private int spawnDelay = 1000;

    // Arcade mode timer
    private long gameStartTime = System.currentTimeMillis();
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

    private float rainbowHue = 0f;

    // Sound effects
    private SoundPool soundPool;
    private int sliceSound1;
    private int sliceSound2;
    private int sliceSound3;
    private int bombSound;
    private int crackSound;
    private int penaltySound;

    // Background music
    private MediaPlayer backgroundMusic;
    private MediaPlayer menuMusic;
    private MediaPlayer arcadeMusic;
    private MediaPlayer gameOverMusic;

    // Audio control
    private boolean soundEnabled = true;
    private boolean musicEnabled = true;
    private boolean menuMusicStarted = false;

    // Golden Apple powerup (Arcade only)
    private boolean isDoublePoints = false;
    private long doublePointsStartTime;
    private int doublePointsDuration = 5000;  // 5 seconds

    // Custom fonts
    private Typeface titleFont;    // For big text like "FRUIT NINJA"
    private Typeface gameFont;     // For gameplay text like scores
    private Typeface buttonFont;   // For buttons

// Context field is already declared:
// private Context context;

    public GameView(Context context) {
        super(context);
        this.context = context;
        init(context);
    }

    // 👇 ADD THIS CONSTRUCTOR
    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init(context);
    }

    // Put all your old constructor logic in here
    private void init(Context context) {
        holder = getHolder();
        paint = new Paint();
        paint.setAntiAlias(true);
        fruits = new ArrayList<>();
        swipePath = new SwipePath();
        random = new Random();
        splatter = new Splatter();

        // Load custom fonts
        try {
            titleFont = ResourcesCompat.getFont(context, R.font.bangers);
            gameFont = ResourcesCompat.getFont(context, R.font.bangers);
            buttonFont = ResourcesCompat.getFont(context, R.font.bangers);
        } catch (Exception e) {
            e.printStackTrace();
            // Fonts will fall back to default if loading fails
        }

        Fruit.setContext(context);

        //initialize difficulty variables
        resetDifficulty();

        //initialize sound system
        intializeSounds(context);
        initializeMusic(context);

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
            logo = null;  // Fallback to plain text in case of error
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
        swipePath.clear();

        System.gc(); // Optimization

        score = 0;
        lives = 3;
        combo = 0;
        maxCombo = 0;
        lastFruitSpawn = System.currentTimeMillis();
        lastSliceTime = System.currentTimeMillis();

        if (gameMode == GameMode.ARCADE) {
            gameStartTime = System.currentTimeMillis();
            spawnDelay = 500; // Faster spawning
        } else {
            spawnDelay = 1000;
        }

        resetDifficulty();
    }

    /**
     * Resets difficulty settings to base values
     */
    private void resetDifficulty() {
        currentSpawnDelay = baseSpawnDelay;
        currentFruitSpeed = baseFruitSpeed;
        difficultyMultiplier = 1.0f;
    }

    /**
     * Updates diffiuclty based on game progress
     * Classic Mode: Difficulty Difficulty increases with score
     * Arcade Mode: Difficulty increases with time elapsed
     */
    private void updateDifficulty() {
        if (gameMode == null) return;

        switch (gameMode) {
            case CLASSIC:
                //increase difficulty every 5 points
                //0 points: multiplier = 1.0
                //25 points: multiplier = 1.5
                //50 points: multiplier = 2.0
                difficultyMultiplier = 1.0f + (score / 50.0f);
                break;

            case ARCADE:
                //increase difficulty based on time
                //first 15 seconds: multiplier = 1.0
                //30 seconds: multiplier = 1.5
                //60 seconds: multiplier = 2.0
                long elapseTime = System.currentTimeMillis() - gameStartTime;
                float secondsElapsed = elapseTime / 1000f;
                difficultyMultiplier = 1.0f + Math.min(secondsElapsed / 30f, 2.0f);
                break;
        }

        //apply difficulty to spawn delay
        //as multiplier increase, delay decreases
        currentSpawnDelay = Math.max(baseSpawnDelay / difficultyMultiplier, minSpawnDelay);

        //apply difficulty to fruit speed
        //as multiplier increases, speed increases
        currentFruitSpeed = Math.min(baseFruitSpeed + (difficultyMultiplier - 1.0f) * 10f, maxFruitSpeed);

        // Apply difficulty to music speed
        updateMusicSpeed();


    }

    private void loadbg() {
        try {
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
        // Initialize buttons before checking game state
        if (screenWidth == 0 && getWidth() > 0 && getHeight() > 0) {
            screenWidth = getWidth();
            screenHeight = getHeight();
            initializeButtons();
            intializeBg();
        }

        // Now check game state
        if (gameState != GameState.PLAYING) return;

        splatter.update();

        updateDifficulty();

        // Update timer for Arcade mode
        if (gameMode == GameMode.ARCADE) {
            rainbowHue += 3f;  // Rotate through colors
            if (rainbowHue > 360) rainbowHue = 0;
            timeRemaining = gameDuration - (System.currentTimeMillis() - gameStartTime);
            if (timeRemaining <= 0) {
                gameOver();
                return;
            }
        }

        // Check if double points has expired
        if (isDoublePoints) {  // ← ADD THIS
            long elapsed = System.currentTimeMillis() - doublePointsStartTime;
            if (elapsed >= doublePointsDuration) {
                isDoublePoints = false;
                Log.d(TAG, "Double points ended!");
            }
        }

        // Check combo timeout
        if (System.currentTimeMillis() - lastSliceTime > comboTimeout && combo > 0) {
            combo = 0;
        }

        // Spawn fruits
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFruitSpawn > currentSpawnDelay) {
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
                // Only penalize for missing GOOD fruits (not bombs or broccoli)
                if (!fruit.isBomb() && !fruit.isPenaltyItem() && !fruit.isSliced()) {
                    lives--; // Deduct life for missing a good fruit
                    combo = 0; // Reset combo
                    if (lives <= 0 && gameMode == GameMode.CLASSIC) {
                        gameOver();
                    }
                }
                iterator.remove();
            }

            // Check collision with swipe path
            if (swipePath.intersects(fruit) && !fruit.isSliced()) {

                // STEP 1: Call onSwipeHit() to check if fruit should be removed
                boolean shouldRemove = fruit.onSwipeHit();

                // STEP 2: Get splatter color (BEFORE any if/else blocks)
                int splatterColor;
                switch (fruit.getType()) {
                    case APPLE:
                        splatterColor = Color.rgb(220, 20, 60);  // red
                        break;
                    case COCONUT:
                        splatterColor = Color.rgb(255, 255, 255); // white
                        break;
                    case BANANA:
                        splatterColor = Color.rgb(255, 215, 0);  // yellow
                        break;
                    case STARFRUIT:
                        splatterColor = Color.rgb(152, 251, 152);  // light green
                        break;
                    case BROCCOLI:
                        splatterColor = Color.rgb(34, 139, 34);  // green
                        break;
                    case BOMB:
                        splatterColor = Color.rgb(50, 50, 50);   // Dark smoke
                        break;
                    case GOLDEN_APPLE:
                        splatterColor = Color.rgb(255, 215, 0);  // Golden
                        break;
                    default:
                        splatterColor = Color.RED;
                }

                // STEP 3: Check if fruit should be fully sliced and removed
                if (shouldRemove) {
                    // Fully slice the fruit
                    fruit.slice();
                    splatter.createSplatter(fruit.x, fruit.y, splatterColor, 12);

                    if (fruit.isBomb()) {
                        playBombSound();
                        // Hit a bomb - game over in classic, lose points in arcade
                        if (gameMode == GameMode.CLASSIC) {
                            gameOver();
                        } else {
                            score = Math.max(0, score - 20);
                        }
                        combo = 0;

                    } else if (fruit.getType() == Fruit.FruitType.GOLDEN_APPLE) {  // ← ADD THIS
                        // GOLDEN APPLE HIT - DOUBLE POINTS!
                        playSliceSound();  // Or add special "cha-ching" sound
                        isDoublePoints = true;
                        doublePointsStartTime = System.currentTimeMillis();
                        Log.d(TAG, " DOUBLE POINTS ACTIVATED!");

                    } else if (fruit.isPenaltyItem()) {
                        playPenaltySound();
                        // Hit broccoli - penalty based on game mode
                        if (gameMode == GameMode.CLASSIC) {
                            // Classic: Lose a life
                            lives--;
                            if (lives <= 0) {
                                gameOver();
                            }
                        } else {
                            // Arcade: Lose points
                            score = Math.max(0, score - 15);  // Lose 15 points (can't go below 0)
                        }
                        combo = 0;  // Break combo in both modes

                    } else {
                        // Hit a regular fruit or fully sliced coconut
                        playSliceSound();
                        combo++;
                        if (combo > maxCombo) maxCombo = combo;
                        lastSliceTime = System.currentTimeMillis();

                        int points = fruit.getPoints();
                        if (combo > 1) {
                            points += combo; // Bonus points for combo
                        }
                        score += points;
                    }

                } else {
                    // Coconut was hit but not fully sliced yet
                    // Create smaller splatter for partial hit
                    playCrackSound();
                    splatter.createSplatter(fruit.x, fruit.y, splatterColor, 6);
                    // Don't slice, don't remove, don't add points yet
                }
            }

            // Remove sliced fruits after animation
            if (fruit.isSliced() && fruit.sliceAnimationComplete()) {
                iterator.remove();
            }
        }
    }

    private void drawRainbowEffect(Canvas canvas) {
        if (gameMode != GameMode.ARCADE) return;

        // Convert HSV to RGB for rainbow effect
        int rainbowColor = Color.HSVToColor(new float[]{rainbowHue, 1.0f, 1.0f});

        paint.setColor(rainbowColor);
        paint.setAlpha(40);  // transparent
        paint.setStrokeWidth(20);
        paint.setStyle(Paint.Style.FILL);

        // Draw border around screen
        canvas.drawRect(0, 0, screenWidth, screenHeight, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setAlpha(255);
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
                        if (!menuMusicStarted) {
                            startMenuMusic();
                            menuMusicStarted = true;
                        }
                        break;
                    case PLAYING:
                        drawGame();
                        drawRainbowEffect(canvas);
                        // Draw double points effect
                        if (isDoublePoints) {  // ← ADD THIS
                            drawDoublePointsEffect(canvas);
                        }
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
            int logoWidth = screenWidth / 2 + 150;
            int logoHeight = screenHeight / 2 - 550;
            int logoX = (int) (screenWidth / 2f - logoWidth / 2);
            int logoY = (int) (screenHeight / 4f - logoHeight / 2);

            Rect logoRect = new Rect(logoX, logoY, logoX + logoWidth, logoY + logoHeight);
            canvas.drawBitmap(logo, null, logoRect, paint);
        } else {
            // Fallback to text if logo doesn't load
            if (titleFont != null) paint.setTypeface(titleFont);
            paint.setColor(Color.WHITE);
            paint.setTextSize(120);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("FRUIT NINJA", screenWidth / 2f, screenHeight / 4f, paint);
        }

        // "Select Mode" text
        if (titleFont != null) paint.setTypeface(titleFont);
        paint.setColor(Color.WHITE);
        paint.setTextSize(60);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Select Mode", screenWidth / 2f, screenHeight / 3f + 250, paint);

        // Classic Mode Button
        if (classicButton != null) {
            paint.setColor(Color.rgb(76, 175, 80));
            canvas.drawRoundRect(classicButton, 20, 20, paint);

            // Button title
            if (buttonFont != null) paint.setTypeface(buttonFont);
            paint.setColor(Color.WHITE);
            paint.setTextSize(70);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("CLASSIC", screenWidth / 2f, screenHeight / 2f, paint);

            // Button description
            if (gameFont != null) paint.setTypeface(gameFont);
            paint.setTextSize(40);
            canvas.drawText("Avoid Bombs - 3 Lives", screenWidth / 2f, screenHeight / 2f + 60, paint);
        }

        // Arcade Mode Button
        if (arcadeButton != null) {
            paint.setColor(Color.rgb(244, 67, 54));
            canvas.drawRoundRect(arcadeButton, 20, 20, paint);

            // Button title
            if (buttonFont != null) paint.setTypeface(buttonFont);
            paint.setColor(Color.WHITE);
            paint.setTextSize(70);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("ARCADE", screenWidth / 2f, screenHeight / 2f + 250, paint);

            // Button description
            if (gameFont != null) paint.setTypeface(gameFont);
            paint.setTextSize(40);
            canvas.drawText("60 Seconds - Bonus Fruits", screenWidth / 2f, screenHeight / 2f + 310, paint);
        }

        // High Scores
        if (titleFont != null) paint.setTypeface(titleFont);
        paint.setColor(Color.WHITE);
        paint.setTextSize(50);
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("High Scores:", 50, screenHeight - 200, paint);

        if (gameFont != null) paint.setTypeface(gameFont);
        paint.setTextSize(40);
        canvas.drawText("Classic: " + highScoreClassic, 50, screenHeight - 140, paint);
        canvas.drawText("Arcade: " + highScoreArcade, 50, screenHeight - 90, paint);

        // Reset to defaults
        paint.setTypeface(Typeface.DEFAULT);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawGame() {
        // Draw fruits
        for (Fruit fruit : fruits) {
            fruit.draw(canvas, paint);
        }
        splatter.draw(canvas, paint);
        swipePath.draw(canvas, paint);

        // Draw HUD with custom font
        if (gameFont != null) paint.setTypeface(gameFont);
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

        // Draw combo with title font for dramatic effect
        if (combo > 1) {
            if (titleFont != null) paint.setTypeface(titleFont);
            paint.setTextSize(80);
            paint.setColor(Color.YELLOW);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("COMBO x" + combo + "!", screenWidth / 2f, 150, paint);
        }

        // Draw FPS
        if (gameFont != null) paint.setTypeface(gameFont);
        paint.setColor(Color.WHITE);
        paint.setTextSize(40);
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("FPS: " + fps, 50, screenHeight - 50, paint);

        // Reset to defaults
        paint.setTypeface(Typeface.DEFAULT);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawGameOver() {
        // Draw final fruits (fading out)
        for (Fruit fruit : fruits) {
            fruit.draw(canvas, paint);
        }

        // Semi-transparent overlay
        paint.setColor(Color.argb(200, 0, 0, 0));
        canvas.drawRect(0, 0, screenWidth, screenHeight, paint);

        // Game Over text with dramatic font
        if (titleFont != null) paint.setTypeface(titleFont);
        paint.setColor(Color.RED);
        paint.setTextSize(120);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("GAME OVER", screenWidth / 2f, screenHeight / 4f, paint);

        // Final score
        if (gameFont != null) paint.setTypeface(gameFont);
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
            if (titleFont != null) paint.setTypeface(titleFont);
            paint.setColor(Color.YELLOW);
            paint.setTextSize(60);
            canvas.drawText("NEW HIGH SCORE!", screenWidth / 2f, screenHeight / 3f + 260, paint);
        }

        // Buttons
        if (restartButton != null) {
            paint.setColor(Color.rgb(76, 175, 80));
            canvas.drawRoundRect(restartButton, 20, 20, paint);

            if (buttonFont != null) paint.setTypeface(buttonFont);
            paint.setColor(Color.WHITE);
            paint.setTextSize(60);
            canvas.drawText("NEW GAME", screenWidth / 2f, screenHeight / 2f + 200, paint);
        }

        if (menuButton != null) {
            paint.setColor(Color.rgb(33, 150, 243));
            canvas.drawRoundRect(menuButton, 20, 20, paint);

            if (buttonFont != null) paint.setTypeface(buttonFont);
            paint.setColor(Color.WHITE);
            paint.setTextSize(60);
            canvas.drawText("MENU", screenWidth / 2f, screenHeight / 2f + 350, paint);
        }

        // Reset to defaults
        paint.setTypeface(Typeface.DEFAULT);
        paint.setTextAlign(Paint.Align.LEFT);
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


    /**
     * Initializes the SoundPool for sound effects
     */
    private void intializeSounds(Context context) {
        // Create SoundPool
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            soundPool = new SoundPool.Builder()
                    .setMaxStreams(5)
                    .setAudioAttributes(audioAttributes)
                    .build();
        } else {
            //Fallback for older Android versions
            soundPool = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
        }

        // Load sound effects
        try {
            sliceSound1 = soundPool.load(context, R.raw.slice1, 1);
            sliceSound2 = soundPool.load(context, R.raw.slice2, 1);
            sliceSound3 = soundPool.load(context, R.raw.slice3, 1);
            bombSound = soundPool.load(context, R.raw.explosion, 1);
            crackSound = soundPool.load(context, R.raw.crack, 1);
            penaltySound = soundPool.load(context, R.raw.penalty, 1);
        } catch (Exception e) {
            e.printStackTrace();
            // Sounds fail to load, game will continue without sound
        }
    }

    /**
     * Initializes background music
     */
    private void initializeMusic(Context context) {
        try {
            //Load classic mode music
            backgroundMusic = MediaPlayer.create(context, R.raw.game_music);
            if (backgroundMusic != null) {
                backgroundMusic.setLooping(true);
                backgroundMusic.setVolume(.60f, .60f);
                Log.d(TAG, "✓ Classic music loaded!");
            }

            //Load arcade mode music
            arcadeMusic = MediaPlayer.create(context, R.raw.arcade_music);
            if (arcadeMusic != null) {
                arcadeMusic.setLooping(true);
                arcadeMusic.setVolume(1.0f, 1.0f);
                Log.d(TAG, "✓ Arcade music loaded!");
            }

            //Load menu music
            menuMusic = MediaPlayer.create(context, R.raw.menu_music);
            if (menuMusic != null) {
                menuMusic.setLooping(true);
                menuMusic.setVolume(.50f, .50f);
                Log.d(TAG, "✓ Menu music loaded!");
            }

            //Load game over music
            gameOverMusic = MediaPlayer.create(context, R.raw.game_over);  // ← ADD THIS
            if (gameOverMusic != null) {
                gameOverMusic.setLooping(false);  // Don't loop - play once
                gameOverMusic.setVolume(1.0f, 1.0f);
                Log.d(TAG, "✓ Game over music loaded!");
            } else {
                Log.e(TAG, "✗ Game over music is NULL");
            }
        } catch (Exception e) {
            Log.e(TAG, "✗ Music loading failed!", e);
        }
    }

    /**
     * Plays a random slice sound
     */
    private void playSliceSound() {
        if (!soundEnabled || soundPool == null) return;

        //randomly choose one of the three slice sounds
        int soundId;
        int randomChoice = random.nextInt(3);

        switch (randomChoice) {
            case 0:
                soundId = sliceSound1;
                break;
            case 1:
                soundId = sliceSound2;
                break;
            default:
                soundId = sliceSound3;
                break;

        }

        soundPool.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f);
    }

    /**
     * Plays crack sound (for partial hits)
     */
    private void playCrackSound() {
        if (!soundEnabled || soundPool == null) return;
        soundPool.play(crackSound, 0.8f, 0.8f, 1, 0, 1.0f);
    }

    /**
     * plays penalty sound
     */
    private void playPenaltySound() {
        if (!soundEnabled || soundPool == null) return;
        soundPool.play(penaltySound, 1.0f, 1.0f, 1, 0, 1.0f);
    }

    /**
     * plays bomb explosion sound
     */
    private void playBombSound() {
        if (!soundEnabled || soundPool == null) return;
        soundPool.play(bombSound, 1.0f, 1.0f, 1, 0, 1.0f);
    }

    /**
     * starts new music
     */
    private void startMenuMusic() {
        if (!musicEnabled) {
            Log.d(TAG, "Music disabled");
            return;
        }

        // Stop ALL music first
        stopAllGameMusic();

        if (menuMusic != null && !menuMusic.isPlaying()) {
            menuMusic.start();
            Log.d(TAG, "✓ Menu music started!");
        } else {
            Log.e(TAG, "✗ Menu music failed - null or already playing");
        }
    }

    private void startGameMusic() {
        Log.d(TAG, "=== startGameMusic() called ===");
        Log.d(TAG, "Current gameMode: " + gameMode);
        Log.d(TAG, "musicEnabled: " + musicEnabled);

        if (!musicEnabled) {
            Log.d(TAG, "Music disabled - returning");
            return;
        }

        // Stop ALL music first
        Log.d(TAG, "Stopping all music...");
        stopMenuMusic();
        stopAllGameMusic();

        resetMusicSpeed();

        // Start ONLY the correct music based on mode
        if (gameMode == GameMode.ARCADE) {
            Log.d(TAG, "Should start ARCADE music only");

            // Make sure classic music is NOT playing
            if (backgroundMusic != null && backgroundMusic.isPlaying()) {
                backgroundMusic.pause();
                Log.d(TAG, "Stopped classic music");
            }

            // Start arcade music
            if (arcadeMusic != null) {
                arcadeMusic.start();
                Log.d(TAG, "✓ Arcade music started!");
            } else {
                Log.e(TAG, "✗ Arcade music is NULL!");
            }

        } else if (gameMode == GameMode.CLASSIC) {
            Log.d(TAG, "Should start CLASSIC music only");

            // Make sure arcade music is NOT playing
            if (arcadeMusic != null && arcadeMusic.isPlaying()) {
                arcadeMusic.pause();
                Log.d(TAG, "Stopped arcade music");
            }

            // Start classic music
            if (backgroundMusic != null) {
                backgroundMusic.start();
                Log.d(TAG, "✓ Classic music started!");
            } else {
                Log.e(TAG, "✗ Classic music is NULL!");
            }

        } else {
            Log.e(TAG, "✗ Unknown game mode: " + gameMode);
        }

        Log.d(TAG, "=== startGameMusic() complete ===");
    }

    /**
     * stops all game music (both classic and arcade) - FORCE STOP
     */
    private void stopAllGameMusic() {
        try {
            if (backgroundMusic != null) {
                if (backgroundMusic.isPlaying()) {
                    backgroundMusic.stop();
                    backgroundMusic.prepare();  // Prepare it for next start
                    Log.d(TAG, "Classic music STOPPED");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error stopping classic music", e);
        }

        try {
            if (arcadeMusic != null) {
                if (arcadeMusic.isPlaying()) {
                    arcadeMusic.stop();
                    arcadeMusic.prepare();  // Prepare it for next start
                    Log.d(TAG, "Arcade music STOPPED");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error stopping arcade music", e);
        }
    }


    private void stopGameMusic() {
        stopAllGameMusic();
    }

    private void stopMenuMusic() {
        if (menuMusic != null) {
            if (menuMusic.isPlaying()) {
                menuMusic.pause();
                menuMusic.seekTo(0);
            }
            Log.d(TAG, "Menu music stopped");
        }
    }

    /**
     * pause all music
     */
    public void pauseAllMusic() {
        if (backgroundMusic != null && backgroundMusic.isPlaying()) {
            backgroundMusic.pause();
        }
        if (arcadeMusic != null && arcadeMusic.isPlaying()) {  // ← ADD THIS
            arcadeMusic.pause();
        }
        if (menuMusic != null && menuMusic.isPlaying()) {
            menuMusic.pause();
        }
        if (gameOverMusic != null && gameOverMusic.isPlaying()) {  // ← ADD THIS
            gameOverMusic.pause();
        }
    }

    /**
     * resumes music based on game state
     */
    public void resumeMusic() {
        if (!musicEnabled) return;

        if (gameState == GameState.PLAYING) {
            if (gameMode == GameMode.ARCADE) {
                // Resume arcade music
                if (arcadeMusic != null && !arcadeMusic.isPlaying()) {
                    arcadeMusic.start();
                }
            } else {
                // Resume classic music
                if (backgroundMusic != null && !backgroundMusic.isPlaying()) {
                    backgroundMusic.start();
                }
            }
        } else if (gameState == GameState.MENU) {
            if (menuMusic != null && !menuMusic.isPlaying()) {
                menuMusic.start();
            }
        }
    }


    private void spawnFruit() {
        if (screenWidth == 0 || screenHeight == 0) return;

        int x, startY;
        float velocityX, velocityY;

        // ===== RANDOM SPAWN LOCATIONS (4 types) =====
        int spawnType = random.nextInt(4);

        switch (spawnType) {
            case 0: // Spawn from BOTTOM
                x = random.nextInt(screenWidth - 100) + 50;
                startY = screenHeight + 50;
                velocityX = random.nextFloat() * 6 - 3;
                velocityY = -currentFruitSpeed - random.nextFloat() * 15;
                break;

            case 1: // Spawn from LEFT
                x = -50;
                startY = screenHeight / 2 + random.nextInt(screenHeight / 3);
                velocityX = 10 + random.nextFloat() * 8;
                velocityY = -15 - random.nextFloat() * 10;
                break;

            case 2: // Spawn from RIGHT
                x = screenWidth + 50;
                startY = screenHeight / 2 + random.nextInt(screenHeight / 3);
                velocityX = -10 - random.nextFloat() * 8;
                velocityY = -15 - random.nextFloat() * 10;
                break;

            case 3: // Spawn from TOP (drop down)
                x = random.nextInt(screenWidth - 100) + 50;
                startY = -50;
                velocityX = random.nextFloat() * 10 - 5;
                velocityY = 5 + random.nextFloat() * 5;
                break;

            default:
                x = random.nextInt(screenWidth - 100) + 50;
                startY = screenHeight + 50;
                velocityX = random.nextFloat() * 6 - 3;
                velocityY = -25 - random.nextFloat() * 15;
                break;
        }

        // ===== BOMB SPAWN LOGIC =====
        boolean spawnBomb = false;
        if (gameMode == GameMode.CLASSIC) {
            spawnBomb = random.nextFloat() < 0.15;
        } else {
            spawnBomb = random.nextFloat() < 0.10;
        }

        if (spawnBomb) {
            fruits.add(new Fruit(x, startY, velocityX, velocityY, Color.BLACK, Fruit.FruitType.BOMB));
        } else if (gameMode == GameMode.ARCADE && random.nextInt(100) < 6) {
            // 6% chance - GOLDEN APPLE (Arcade only!)
            fruits.add(new Fruit(x, startY, velocityX, velocityY, Color.rgb(255, 215, 0), Fruit.FruitType.GOLDEN_APPLE));
        } else {
            int itemChance = random.nextInt(100);

            if (itemChance < 15) {
                fruits.add(new Fruit(x, startY, velocityX, velocityY, Color.rgb(34, 139, 34), Fruit.FruitType.BROCCOLI));
            } else {
                Fruit.FruitType[] types = {
                        Fruit.FruitType.APPLE,
                        Fruit.FruitType.COCONUT,
                        Fruit.FruitType.STARFRUIT,
                        Fruit.FruitType.BANANA
                };

                if (gameMode == GameMode.ARCADE && random.nextFloat() < 0.10) {
                    types = new Fruit.FruitType[]{Fruit.FruitType.BANANA};
                }

                Fruit.FruitType type = types[random.nextInt(types.length)];

                int color;
                switch (type) {
                    case APPLE:
                        color = Color.RED;
                        break;
                    case COCONUT:
                        color = Color.rgb(139, 69, 19);
                        break;
                    case STARFRUIT:
                        color = Color.rgb(255, 215, 0);
                        break;
                    case BANANA:
                        color = Color.YELLOW;
                        break;
                    default:
                        color = Color.RED;
                }

                fruits.add(new Fruit(x, startY, velocityX, velocityY, color, type));
            }
        }
    }


    private void gameOver() {
        gameState = GameState.GAME_OVER;
        isDoublePoints = false;
        stopGameMusic();
        startGameOverMusic();
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
                        stopMenuMusic();
                        startGameMusic();
                        menuMusicStarted = false;  // ← Reset flag
                    } else if (arcadeButton != null && arcadeButton.contains(touchX, touchY)) {
                        gameMode = GameMode.ARCADE;
                        gameState = GameState.PLAYING;
                        resetGame();
                        stopMenuMusic();
                        startGameMusic();
                        menuMusicStarted = false;  // ← Reset flag
                    }
                } else if (gameState == GameState.GAME_OVER) {
                    if (restartButton != null && restartButton.contains(touchX, touchY)) {
                        gameState = GameState.PLAYING;
                        stopGameOverMusic();
                        resetGame();
                        startGameMusic();
                    } else if (menuButton != null && menuButton.contains(touchX, touchY)) {
                        gameState = GameState.MENU;
                        fruits.clear();
                        stopGameOverMusic();
                        stopGameMusic();
                        startMenuMusic();
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


    public void releaseResources() {
        // Release SoundPool
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }

        // Release Classic music
        if (backgroundMusic != null) {
            if (backgroundMusic.isPlaying()) {
                backgroundMusic.stop();
            }
            backgroundMusic.release();
            backgroundMusic = null;
        }

        // Release Arcade music - ADD THIS
        if (arcadeMusic != null) {
            if (arcadeMusic.isPlaying()) {
                arcadeMusic.stop();
            }
            arcadeMusic.release();
            arcadeMusic = null;
        }

        // Release Menu music
        if (menuMusic != null) {
            if (menuMusic.isPlaying()) {
                menuMusic.stop();
            }
            menuMusic.release();
            menuMusic = null;
        }

        // Release Game Over music
        if (gameOverMusic != null) {
            if (gameOverMusic.isPlaying()) gameOverMusic.stop();
            gameOverMusic.release();
            gameOverMusic = null;
        }
    }

    /**
     * Adjusts music playback speed based on difficulty multiplier
     */
    private void updateMusicSpeed() {
        if (!musicEnabled) return;
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) {
            // Playback speed control only available on Android 6.0+
            return;
        }

        try {
            // Calculate music speed (1.0x to 1.5x max)
            // difficultyMultiplier ranges from 1.0 to 2.0+
            float musicSpeed = 1.0f + ((difficultyMultiplier - 1.0f) * 0.5f);
            musicSpeed = Math.min(musicSpeed, 1.5f);  // Cap at 1.5x speed

            // Apply to the currently playing music
            if (gameMode == GameMode.ARCADE && arcadeMusic != null && arcadeMusic.isPlaying()) {
                arcadeMusic.setPlaybackParams(arcadeMusic.getPlaybackParams().setSpeed(musicSpeed));
                Log.d(TAG, "Arcade music speed: " + musicSpeed + "x");
            } else if (gameMode == GameMode.CLASSIC && backgroundMusic != null && backgroundMusic.isPlaying()) {
                backgroundMusic.setPlaybackParams(backgroundMusic.getPlaybackParams().setSpeed(musicSpeed));
                Log.d(TAG, "Classic music speed: " + musicSpeed + "x");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to adjust music speed", e);
        }

    }

    /**
     * Resets music playback speed to normal (1.0x)
     */
    private void resetMusicSpeed() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) return;

        try {
            if (backgroundMusic != null) {
                backgroundMusic.setPlaybackParams(backgroundMusic.getPlaybackParams().setSpeed(1.0f));
            }
            if (arcadeMusic != null) {
                arcadeMusic.setPlaybackParams(arcadeMusic.getPlaybackParams().setSpeed(1.0f));
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to reset music speed", e);
        }
    }

    /**
     * Starts game over music
     */
    private void startGameOverMusic() {
        if (!musicEnabled) {
            Log.d(TAG, "Music disabled");
            return;
        }

        // Stop all other music
        stopMenuMusic();
        stopAllGameMusic();

        // Start game over music
        if (gameOverMusic != null) {
            if (gameOverMusic.isPlaying()) {
                gameOverMusic.seekTo(0);  // Restart from beginning
            }
            gameOverMusic.start();
            Log.d(TAG, "✓ Game over music started!");
        } else {
            Log.e(TAG, "✗ Game over music is NULL!");
        }
    }

    /**
     * Stops game over music
     */
    private void stopGameOverMusic() {
        if (gameOverMusic != null && gameOverMusic.isPlaying()) {
            gameOverMusic.pause();
            gameOverMusic.seekTo(0);
            Log.d(TAG, "Game over music stopped");
        }
    }

    /**
     * Draws double points effect overlay
     */
    private void drawDoublePointsEffect(Canvas canvas) {
        // Golden glow overlay
        paint.setColor(Color.rgb(255, 215, 0));  // Gold
        paint.setAlpha(40);  // Semi-transparent
        canvas.drawRect(0, 0, screenWidth, screenHeight, paint);
        paint.setAlpha(255);

        // Calculate remaining time
        long elapsed = System.currentTimeMillis() - doublePointsStartTime;
        long remaining = doublePointsDuration - elapsed;
        float secondsLeft = remaining / 1000f;

        // Draw "2X POINTS" text with pulsing effect
        paint.setColor(Color.rgb(255, 215, 0));

        // Pulsing size effect
        float pulse = (float) Math.sin(System.currentTimeMillis() / 150.0) * 10 + 90;
        paint.setTextSize(pulse);
        paint.setTextAlign(Paint.Align.CENTER);

        // Add shadow for better visibility
        paint.setShadowLayer(10, 0, 0, Color.BLACK);
        canvas.drawText("⭐ 2X POINTS! ⭐", screenWidth / 2f, 300, paint);
        paint.clearShadowLayer();

        // Draw timer
        paint.setTextSize(60);
        canvas.drawText(String.format("%.1fs", secondsLeft), screenWidth / 2f, 380, paint);

        paint.setTextAlign(Paint.Align.LEFT);
    }

}