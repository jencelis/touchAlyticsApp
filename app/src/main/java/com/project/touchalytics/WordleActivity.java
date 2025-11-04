package com.project.touchalytics;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;

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

import java.util.HashMap;
import java.util.Locale;
import java.util.Random;

public class WordleActivity extends AppCompatActivity
        implements SpellCheckerSession.SpellCheckerSessionListener {

    private static final int ROWS = 6;
    private static final int COLS = 5;

    private final TextView[][] cells = new TextView[ROWS][COLS];

    private final String[] ANSWERS = new String[]{
            // Small starter list — expand as you like (must be 5 letters, lowercase)
            "about","other","which","their","there","first","would","these","could","sound",
            "thing","think","right","place","three","green","apple","lemon","grape","melon",
            "chair","table","candy","sweet","spice","ghost","plain","crane","trace","adore"
    };

    private String target;          // chosen word
    private int currentRow = 0;
    private int currentCol = 0;
    private boolean gameOver = false;

    // ---- Spell checker (optional) ----
    private SpellCheckerSession spellSession;
    private String pendingGuess = null;
    private boolean waitingOnSpell = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wordle);

        GridLayout boardGrid = findViewById(R.id.boardGrid);

        // Build board cells programmatically for consistent sizing
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
                lp.width = 0;                            // use column weight
                lp.height = 0;                           // use row weight
                lp.columnSpec = GridLayout.spec(c, 1f);  // equal column weight
                lp.rowSpec = GridLayout.spec(r, 1f);     // equal row weight
                lp.setMargins(pad, pad, pad, pad);
                tv.setLayoutParams(lp);

                boardGrid.addView(tv);
                cells[r][c] = tv;
            }
        }

        // Keyboard
        wireKeyboard();

        // Pick target
        target = ANSWERS[new Random().nextInt(ANSWERS.length)];

        // Try to get a system spell-checker (if none, we accept everything)
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

    // ---------------- Keyboard building ----------------

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

    /** Special keys get icons and wider weight; edges get smaller margins than inner gaps. */
    private void addKeyButton(LinearLayout row, String label, float weight,
                              boolean isSpecial, boolean isFirst, boolean isLast) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setIncludeFontPadding(false);
        b.setSingleLine(true);
        b.setBackground(ContextCompat.getDrawable(this, R.drawable.key_bg));

        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                b, 12, isSpecial ? 18 : 16, 1, TypedValue.COMPLEX_UNIT_SP);

        // logical code we’ll pass to onKeyPress
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
            b.setText(label); // letter
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

        // Use the logical code (not the visual label) for click handling
        b.setTag(code);
        b.setOnClickListener(v -> onKeyPress((String) v.getTag()));

        row.addView(b);
    }

    private void wireKeyboard() {
        LinearLayout row1 = findViewById(R.id.row1);
        LinearLayout row2 = findViewById(R.id.row2);
        LinearLayout row3 = findViewById(R.id.row3);

        prepRow(row1); prepRow(row2); prepRow(row3);

        addKeysToRow(row1, "QWERTYUIOP", 1f);

        // Row 2 with subtle indent
        addSpacer(row2, 0.5f);
        addKeysToRow(row2, "ASDFGHJKL", 1f);
        addSpacer(row2, 0.5f);

        // Row 3 with wide Enter/Del and tiny side spacers
        addKeyButton(row3, "ENTER", 1.7f, true, true, false);
        addSpacer(row3, 0.25f);
        addKeysToRow(row3, "ZXCVBNM", 1f);
        addSpacer(row3, 0.25f);
        addKeyButton(row3, "DEL", 1.7f, true, false, true);
    }

    private float dp(float d) {
        return d * getResources().getDisplayMetrics().density;
    }

    // ---------------- Gameplay ----------------

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
        // letters
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

        // Collect guess
        StringBuilder sb = new StringBuilder();
        for (int c = 0; c < COLS; c++) {
            sb.append(cells[currentRow][c].getText().toString().toLowerCase(Locale.US));
        }
        String guess = sb.toString();

        // If a spell checker exists, ask it; otherwise accept immediately.
        if (spellSession != null && !waitingOnSpell) {
            waitingOnSpell = true;
            pendingGuess = guess;
            spellSession.getSuggestions(new TextInfo(guess), 5);
            return; // wait for callback
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
        // Classic two-pass coloring with frequency map (to handle duplicates correctly)
        char[] g = guess.toCharArray();
        char[] a = answer.toCharArray();

        HashMap<Character, Integer> freq = new HashMap<>();
        for (char ch : a) {
            freq.put(ch, freq.getOrDefault(ch, 0) + 1);
        }

        int[] state = new int[COLS]; // 2=correct, 1=present, 0=absent
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

    // Simple “New Game” button
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

    // ---------------- SpellChecker callbacks ----------------

    @Override
    public void onGetSuggestions(final SuggestionsInfo[] results) {
        boolean looksValid = true; // default to permissive

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
            // Accept if it's in dictionary OR the service suggests the same word.
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
                // keep row/col so the user can edit
            }
            pendingGuess = null;
        });
    }

    @Override
    public void onGetSentenceSuggestions(SentenceSuggestionsInfo[] results) {
        // Not used
    }
}
