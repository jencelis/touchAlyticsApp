package com.project.touchalytics;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.textservice.SentenceSuggestionsInfo;
import android.view.textservice.SpellCheckerSession;
import android.view.textservice.SuggestionsInfo;
import android.view.textservice.TextInfo;
import android.view.textservice.TextServicesManager;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;

import java.util.HashMap;
import java.util.Locale;
import java.util.Random;

public class WordleActivity extends AppCompatActivity
        implements SpellCheckerSession.SpellCheckerSessionListener, MainActivity.TouchAnalyticsListener {

    private static final int ROWS = 6;
    private static final int COLS = 5;
    static final String EXTRA_USER_ID = "userID";

    private final TextView[][] cells = new TextView[ROWS][COLS];

    private final String[] ANSWERS = new String[]{
            "about","other","which","their","there","first","would","these","could","sound",
            "thing","think","right","place","three","green","apple","lemon","grape","melon",
            "chair","table","candy","sweet","spice","ghost","plain","crane","trace","adore"
    };

    private String target;
    private int currentRow = 0;
    private int currentCol = 0;
    private boolean gameOver = false;

    private SpellCheckerSession spellSession;
    private String pendingGuess = null;
    private boolean waitingOnSpell = false;

    private MainActivity touchManager;
    private int userId;
    private boolean freeMode = false;

    private TextView statusMessage, statusStrokeCount, statusStrokeCountMin, statusMatchedCount, statusNotMatchedCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wordle);

        userId = getIntent().getIntExtra(EXTRA_USER_ID, -1);
        if (userId < 0) {
            Toast.makeText(this, "Invalid User ID. Closing application.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        freeMode = getIntent().getBooleanExtra("freeMode", false);

        // Initialize views before the touch manager
        statusMessage = findViewById(R.id.ta_statusMessage);
        statusStrokeCount = findViewById(R.id.ta_statusTapCount);
        statusStrokeCountMin = findViewById(R.id.ta_statusTapCountMin);
        statusMatchedCount = findViewById(R.id.ta_statusMatchedCount);
        statusNotMatchedCount = findViewById(R.id.ta_statusNotMatchedCount);

        // Now initialize the touch manager
        touchManager = MainActivity.getInstance();

        if (freeMode) {
            // FREE MODE: no stroke caps, no DB writes
            touchManager.initialize(
                    this,
                    userId,
                    this,
                    0,      // minStrokes ignored in freeMode
                    0L,     // start at 0
                    true    // freeMode = true
            );
        } else {
            // TRAINING MODE

            // Total swipe count across ALL phases (if provided by LoginActivity)
            // If launched from FruitNinja directly (no extra), default is -1 and we treat it as "start phase at 0".
            long totalSwipeCount = getIntent().getLongExtra(MainActivity.EXTRA_STROKE_COUNT, -1L);

            long initialPhaseCount;
            if (totalSwipeCount >= 0L) {
                // Wordle is phase 3:
                //  - phase 1 (NewsMedia) uses the first NEWS_MEDIA_MIN_STROKE_COUNT swipes
                //  - phase 2 (FruitNinja) uses the next FRUIT_NINJA_MIN_STROKE_COUNT swipes
                //  - the rest (up to MIN_W_STROKE_COUNT) belong to this phase
                long raw = totalSwipeCount
                        - Constants.NEWS_MEDIA_MIN_STROKE_COUNT
                        - Constants.FRUIT_NINJA_MIN_STROKE_COUNT;

                initialPhaseCount = Math.max(
                        0L,
                        Math.min(raw, Constants.MIN_W_STROKE_COUNT)
                );
            } else {
                // No global swipe count passed → start this phase at 0 strokes
                initialPhaseCount = 0L;
            }

            touchManager.initialize(
                    this,
                    userId,
                    this,
                    Constants.MIN_W_STROKE_COUNT,
                    initialPhaseCount,
                    false   // freeMode = false
            );
        }


        updateStatusBar(
                touchManager.getStrokeCount(),
                touchManager.getMatchedCount(),
                touchManager.getNotMatchedCount()
        );

        GridLayout boardGrid = findViewById(R.id.boardGrid);

        boardGrid.setRowCount(ROWS);
        boardGrid.setColumnCount(COLS);
        int pad = (int) (getResources().getDisplayMetrics().density * 4);

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                TextView tv = new TextView(this);
                tv.setText("");
                tv.setTextAppearance(this, R.style.TileText);
                tv.setBackground(getDrawable(R.drawable.tile_bg));
                tv.setPadding(0, pad, 0, pad);
                tv.setGravity(Gravity.CENTER);

                GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
                lp.width = 0;
                lp.height = 0;
                lp.columnSpec = GridLayout.spec(c, 1f);
                lp.rowSpec = GridLayout.spec(r, 1f);
                lp.setMargins(pad, pad, pad, pad);
                tv.setLayoutParams(lp);

                boardGrid.addView(tv);
                cells[r][c] = tv;
            }
        }

        wireKeyboard();

        target = ANSWERS[new Random().nextInt(ANSWERS.length)];

        TextServicesManager tsm =
                (TextServicesManager) getSystemService(TEXT_SERVICES_MANAGER_SERVICE);
        if (tsm != null) {
            spellSession = tsm.newSpellCheckerSession(null, Locale.US, this, true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (spellSession != null) spellSession.close();
    }

    private void prepRow(LinearLayout row) {
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, (int) dp(2), 0, (int) dp(2));
    }

    private void addSpacer(LinearLayout row, float weight) {
        Space s = new Space(this);
        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, weight);
        s.setLayoutParams(lp);
        row.addView(s);
    }

    private void addKeysToRow(LinearLayout row, String chars, float weightPerKey) {
        int n = chars.length();
        for (int i = 0; i < n; i++) {
            String label = String.valueOf(chars.charAt(i));
            boolean isFirst = (i == 0);
            boolean isLast  = (i == n - 1);
            addKeyButton(row, label, weightPerKey, false, isFirst, isLast);
        }
    }

    private void addKeyButton(LinearLayout row, String label, float weight,
                              boolean isSpecial, boolean isFirst, boolean isLast) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setIncludeFontPadding(false);
        b.setSingleLine(true);
        b.setBackground(ContextCompat.getDrawable(this, R.drawable.key_bg));

        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                b, 12, isSpecial ? 18 : 16, 1, TypedValue.COMPLEX_UNIT_SP);

        String code;

        if ("ENTER".equals(label)) {
            b.setText("⏎");
            b.setContentDescription("Enter");
            code = "ENTER";
        } else if ("DEL".equals(label)) {
            b.setText("⌫");
            b.setContentDescription("Delete");
            code = "DEL";
        } else {
            b.setText(label);
            code = label.toUpperCase(Locale.US);
        }

        int innerGap = (int) dp(6);
        int edgeGap  = (int) dp(2);
        int left  = isFirst ? edgeGap : innerGap / 2;
        int right = isLast  ? edgeGap : innerGap / 2;

        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, weight);
        lp.setMargins(left, (int) dp(3), right, (int) dp(3));
        b.setLayoutParams(lp);

        b.setPadding((int) dp(6), (int) dp(8), (int) dp(6), (int) dp(8));

        b.setTag(code);
        b.setOnClickListener(v -> onKeyPress((String) v.getTag()));

        b.setOnTouchListener((v, event) -> {
            touchManager.handleTouchEvent(event);
            return false;
        });

        row.addView(b);
    }

    private void wireKeyboard() {
        LinearLayout row1 = findViewById(R.id.row1);
        LinearLayout row2 = findViewById(R.id.row2);
        LinearLayout row3 = findViewById(R.id.row3);

        prepRow(row1); prepRow(row2); prepRow(row3);

        addKeysToRow(row1, "QWERTYUIOP", 1f);

        addSpacer(row2, 0.5f);
        addKeysToRow(row2, "ASDFGHJKL", 1f);
        addSpacer(row2, 0.5f);

        addKeyButton(row3, "ENTER", 1.7f, true, true, false);
        addSpacer(row3, 0.25f);
        addKeysToRow(row3, "ZXCVBNM", 1f);
        addSpacer(row3, 0.25f);
        addKeyButton(row3, "DEL", 1.7f, true, false, true);
    }

    private float dp(float d) {
        return d * getResources().getDisplayMetrics().density;
    }

    private void onKeyPress(String code) {
        if (gameOver) return;

        if ("ENTER".equals(code)) {
            submitRow();
            return;
        }
        if ("DEL".equals(code)) {
            handleDelete();
            return;
        }
        if (code.length() == 1 && Character.isLetter(code.charAt(0))) {
            handleLetter(code.toUpperCase(Locale.US));
        }
    }

    private void handleLetter(String letter) {
        if (currentCol < COLS && currentRow < ROWS) {
            cells[currentRow][currentCol].setText(letter);
            currentCol++;
        }
    }

    private void handleDelete() {
        if (currentCol > 0) {
            currentCol--;
            cells[currentRow][currentCol].setText("");
        }
    }

    private void submitRow() {
        if (currentCol < COLS) {
            Toast.makeText(this, "Need 5 letters", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (int c = 0; c < COLS; c++) {
            sb.append(cells[currentRow][c].getText().toString().toLowerCase(Locale.US));
        }
        String guess = sb.toString();

        if (spellSession != null && !waitingOnSpell) {
            waitingOnSpell = true;
            pendingGuess = guess;
            spellSession.getSuggestions(new TextInfo(guess), 5);
            return;
        }

        acceptGuess(guess);
    }

    private void acceptGuess(String guess) {
        colorRow(guess, target);

        if (guess.equals(target)) {
            Toast.makeText(this, "You got it!", Toast.LENGTH_LONG).show();
            gameOver = true;
            return;
        }

        currentRow++;
        currentCol = 0;

        if (currentRow == ROWS) {
            gameOver = true;
            Toast.makeText(this, "Out of tries! Word was: " +
                    target.toUpperCase(Locale.US), Toast.LENGTH_LONG).show();
        }
    }

    private void colorRow(String guess, String answer) {
        char[] g = guess.toCharArray();
        char[] a = answer.toCharArray();

        HashMap<Character, Integer> freq = new HashMap<>();
        for (char ch : a) {
            freq.put(ch, freq.getOrDefault(ch, 0) + 1);
        }

        int[] state = new int[COLS];
        for (int i = 0; i < COLS; i++) {
            if (g[i] == a[i]) {
                state[i] = 2;
                freq.put(g[i], freq.get(g[i]) - 1);
            }
        }

        for (int i = 0; i < COLS; i++) {
            if (state[i] == 0) {
                char ch = g[i];
                if (freq.getOrDefault(ch, 0) > 0) {
                    state[i] = 1;
                    freq.put(ch, freq.get(ch) - 1);
                }
            }
        }

        for (int i = 0; i < COLS; i++) {
            TextView tv = cells[currentRow][i];
            if (state[i] == 2) {
                tv.setBackground(getDrawable(R.drawable.tile_correct));
                setKeyboardTint(g[i], R.color.correct);
            } else if (state[i] == 1) {
                tv.setBackground(getDrawable(R.drawable.tile_present));
                setKeyboardTint(g[i], R.color.present);
            } else {
                tv.setBackground(getDrawable(R.drawable.tile_absent));
                setKeyboardTint(g[i], R.color.absent);
            }
        }
    }

    private void setKeyboardTint(char ch, int colorRes) {
        int color = getColor(colorRes);

        int[] rows = new int[]{R.id.row1, R.id.row2, R.id.row3};
        for (int rowId : rows) {
            ViewGroup row = findViewById(rowId);
            for (int i = 0; i < row.getChildCount(); i++) {
                View v = row.getChildAt(i);
                if (v instanceof Button) {
                    Button b = (Button) v;
                    String t = b.getText().toString().toUpperCase(Locale.US);
                    if (t.length() == 1 && t.charAt(0) == Character.toUpperCase(ch)) {
                        Integer existing = (Integer) b.getTag(R.id.key_color_tag);
                        int rank = rankColor(colorRes);
                        int existingRank = existing == null ? -1 : existing;
                        if (rank > existingRank) {
                            b.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                            b.setTag(R.id.key_color_tag, rank);
                        }
                    }
                }
            }
        }
    }

    private int rankColor(int colorRes) {
        if (colorRes == R.color.absent) return 0;
        if (colorRes == R.color.present) return 1;
        if (colorRes == R.color.correct) return 2;
        return -1;
    }

    public void onNewGame(View v) {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                TextView tv = cells[r][c];
                tv.setText("");
                tv.setBackground(getDrawable(R.drawable.tile_bg));
            }
        }
        resetKeyboardTints();

        target = ANSWERS[new Random().nextInt(ANSWERS.length)];
        currentRow = 0;
        currentCol = 0;
        gameOver = false;
    }

    private void resetKeyboardTints() {
        int[] rows = new int[]{R.id.row1, R.id.row2, R.id.row3};
        for (int rowId : rows) {
            ViewGroup row = findViewById(rowId);
            for (int i = 0; i < row.getChildCount(); i++) {
                View v = row.getChildAt(i);
                if (v instanceof Button) {
                    Button b = (Button) v;
                    b.getBackground().clearColorFilter();
                    b.setTag(R.id.key_color_tag, null);
                }
            }
        }
    }

    @Override
    public void onGetSuggestions(final SuggestionsInfo[] results) {
        boolean looksValid = true;

        if (results != null && results.length > 0) {
            SuggestionsInfo info = results[0];
            int attrs = info.getSuggestionsAttributes();

            boolean inDict = (attrs & SuggestionsInfo.RESULT_ATTR_IN_THE_DICTIONARY) != 0;
            boolean suggestedSame = false;
            for (int i = 0; i < info.getSuggestionsCount(); i++) {
                String s = info.getSuggestionAt(i);
                if (s != null && pendingGuess != null &&
                        s.equalsIgnoreCase(pendingGuess)) {
                    suggestedSame = true;
                    break;
                }
            }
            looksValid = inDict || suggestedSame;
        }

        final boolean finalLooksValid = looksValid;
        runOnUiThread(() -> {
            waitingOnSpell = false;
            if (pendingGuess == null) return;

            if (finalLooksValid) {
                acceptGuess(pendingGuess);
            } else {
                Toast.makeText(this, "That doesn't look like a real word.", Toast.LENGTH_SHORT).show();
            }
            pendingGuess = null;
        });
    }

    @Override
    public void onGetSentenceSuggestions(SentenceSuggestionsInfo[] results) { /* Not used */ }

    @Override
    public void onStrokeCountUpdated(long newCount) {
        updateStatusBar(newCount, touchManager.getMatchedCount(), touchManager.getNotMatchedCount());

        if (!freeMode && newCount >= Constants.MIN_W_STROKE_COUNT) {
            showTrainingCompleteDialog();
        }
    }


    private void showTrainingCompleteDialog() {
        // Instead of blindly assuming training is done, ask the server
        MainActivity.getInstance().fetchStoredSwipeCount(userId,
                new MainActivity.SwipeCountCallback() {
                    @Override
                    public void onResult(long totalCount) {
                        runOnUiThread(() -> handleSwipeCountResult(totalCount));
                    }
                    @Override
                    public void onError(String message) {
                        runOnUiThread(() -> {
                            Toast.makeText(WordleActivity.this,
                                    "Could not verify training status: " + message,
                                    Toast.LENGTH_LONG).show();
                        });
                    }
                });
    }


    /**
     * Decide where to send the user based on the total number of stored swipes.
     *
     * - If totalCount >= MIN_STROKE_COUNT (e.g. 90): mark training complete and go to Main Menu.
     * - If totalCount < 90: show an error message and redirect to the correct phase
     *   with MainActivity.EXTRA_STROKE_COUNT so that phase can top off.
     */
    private void handleSwipeCountResult(long totalCount) {
        final int N1 = Constants.NEWS_MEDIA_MIN_STROKE_COUNT;      // e.g. 30
        final int N2 = Constants.FRUIT_NINJA_MIN_STROKE_COUNT;     // e.g. 40
        final int N3 = Constants.MIN_W_STROKE_COUNT;               // e.g. 20
        final int TOTAL_REQUIRED = Constants.MIN_STROKE_COUNT;     // e.g. 90

        if (totalCount >= TOTAL_REQUIRED) {
            // ✅ Everything is good: mark training complete and go to main menu
            SharedPreferences prefs = getSharedPreferences("training_status", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("training_complete", true);
            editor.apply();

            new AlertDialog.Builder(this)
                    .setTitle("All Training Complete")
                    .setMessage("You have completed all required training swipes. You can now freely explore the app.")
                    .setPositiveButton("Go to Main Menu", (dialog, which) -> {
                        Intent intent = new Intent(WordleActivity.this, MainMenuActivity.class);
                        intent.putExtra("userID", userId);
                        startActivity(intent);
                        finish();
                    })
                    .setCancelable(false)
                    .show();
            return;
        }

        // Not enough swipes stored on server: tell the user and send them back
        String msg = "The server reports only " + totalCount +
                " stored training swipes, but " + TOTAL_REQUIRED +
                " are required. We will reopen the remaining training phases.";

        new AlertDialog.Builder(this)
                .setTitle("Training Incomplete")
                .setMessage(msg)
                .setPositiveButton("Continue Training", (dialog, which) -> {
                    Intent intent;
                    if (totalCount < N1) {
                        // Need more NewsMedia swipes (phase 1)
                        intent = new Intent(WordleActivity.this, NewsMediaActivity.class);
                        intent.putExtra(NewsMediaActivity.EXTRA_USER_ID, userId);
                    } else if (totalCount < N1 + N2) {
                        // Need more FruitNinja swipes (phase 2)
                        intent = new Intent(WordleActivity.this, FruitNinjaActivity.class);
                        intent.putExtra(FruitNinjaActivity.EXTRA_USER_ID, userId);
                    } else {
                        // Need more Wordle swipes (phase 3)
                        intent = new Intent(WordleActivity.this, WordleActivity.class);
                        intent.putExtra(WordleActivity.EXTRA_USER_ID, userId);
                    }

                    // Pass the global total so that phase can compute its own initial count
                    intent.putExtra(MainActivity.EXTRA_STROKE_COUNT, totalCount);
                    startActivity(intent);
                    finish();
                })
                .setCancelable(false)
                .show();
    }



    @Override
    public void onVerificationResult(boolean matched, int matchedCount, int notMatchedCount) {
        updateStatusBar(touchManager.getStrokeCount(), matchedCount, notMatchedCount);
    }

    private void updateStatusBar(long strokeCount, int matchedCount, int notMatchedCount) {
        if (statusMessage == null) return;

        if (strokeCount < Constants.MIN_W_STROKE_COUNT) {
            statusMessage.setText("Stroke Enrollment Phase");
            statusStrokeCount.setVisibility(View.VISIBLE);
            statusStrokeCountMin.setVisibility(View.VISIBLE);
            statusMatchedCount.setVisibility(View.GONE);
            statusNotMatchedCount.setVisibility(View.GONE);

            statusStrokeCount.setText(String.valueOf(strokeCount));
            statusStrokeCountMin.setText("/" + Constants.MIN_W_STROKE_COUNT);
        } else if (freeMode){
            statusMessage.setText("Stroke Verification Phase");
            statusStrokeCount.setVisibility(View.GONE);
            statusStrokeCountMin.setVisibility(View.GONE);
            statusMatchedCount.setVisibility(View.VISIBLE);
            statusNotMatchedCount.setVisibility(View.VISIBLE);

            statusMatchedCount.setText(String.valueOf(matchedCount));
            statusNotMatchedCount.setText(String.valueOf(notMatchedCount));
        }
        else{
            statusStrokeCount.setVisibility(View.GONE);
            statusStrokeCountMin.setVisibility(View.GONE);
            statusMatchedCount.setVisibility(View.GONE);
            statusNotMatchedCount.setVisibility(View.GONE);
            statusMessage.setText("");
        }
    }

    @Override
    public void onError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
