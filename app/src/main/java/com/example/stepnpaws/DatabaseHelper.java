package com.example.stepnpaws;

import static android.content.ContentValues.TAG;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "pixel_pet.db";
    private static final int DATABASE_VERSION = 4;

    public static final String TABLE_USER = "UserData";
    public static final String TABLE_PETS = "Pets";
    public static final String TABLE_BACKGROUNDS = "Backgrounds";
    public static final String TABLE_STEP_HISTORY = "StepHistory";

    // Mood constants
    public static final String MOOD_HAPPY = "Happy";
    public static final String MOOD_NEUTRAL = "Neutral";
    public static final String MOOD_SAD = "Sad";
    public static final String MOOD_EXCITED = "Excited";
    public static final String MOOD_SLEEPY = "Sleepy";

    // Step thresholds for pet mood changes
    private static final int STEPS_FOR_HAPPY = 5000;
    private static final int STEPS_FOR_EXCITED = 8000;
    private static final int STEPS_FOR_NEUTRAL = 2000;

    // For pets table
    public static final String COLUMN_PET_IMAGE = "image_res";

    // For backgrounds table
    public static final String COLUMN_BG_IMAGE = "image_res";


    // Experience needed per level
    private static final int BASE_XP_FOR_LEVEL = 1000;
    private static final float LEVEL_MULTIPLIER = 1.5f;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // User data (points, current pet)
        db.execSQL("CREATE TABLE " + TABLE_USER + " (id INTEGER PRIMARY KEY, steps INTEGER, accumulated_steps INTEGER, current_pet TEXT, current_background TEXT)");

        // Pets owned - Now includes image_res column
        db.execSQL("CREATE TABLE " + TABLE_PETS + " (id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT, mood TEXT, level INTEGER, experience REAL, " +
                COLUMN_PET_IMAGE + " INTEGER)");

        // Backgrounds owned - Now includes image_res column
        db.execSQL("CREATE TABLE " + TABLE_BACKGROUNDS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT, " +
                COLUMN_BG_IMAGE + " INTEGER)");

        // Step history
        db.execSQL("CREATE TABLE " + TABLE_STEP_HISTORY + " (id INTEGER PRIMARY KEY AUTOINCREMENT, date TEXT, steps INTEGER)");
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 5) {
            // Completely drop and recreate tables to ensure proper schema
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_USER);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_PETS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_BACKGROUNDS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_STEP_HISTORY);
            onCreate(db);
        }
    }

    // Insert step history
    public void insertStepHistory(String date, int steps) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("date", date);
        values.put("steps", steps);
        db.insert(TABLE_STEP_HISTORY, null, values);
        db.close();
    }

    // Record daily steps and update today's history
    public void recordDailySteps(int steps) {
        // Get today's date in YYYY-MM-DD format
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = dateFormat.format(new Date());

        // Check if an entry for today already exists
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_STEP_HISTORY + " WHERE date = ?",
                new String[]{today});

        if (cursor.getCount() > 0) {
            // Update today's entry
            db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("steps", steps);
            db.update(TABLE_STEP_HISTORY, values, "date = ?", new String[]{today});
        } else {
            // Insert new entry for today
            insertStepHistory(today, steps);
        }

        cursor.close();
        db.close();

        // Also update pet mood and experience based on steps
        updatePetMoodBySteps(steps);
        String currentPet = getCurrentPetName();
        addExperienceToPet(currentPet,steps);
    }

    // Fetch all step history
    public Cursor getAllStepHistory() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_STEP_HISTORY + " ORDER BY date DESC", null);
    }

    // Get steps for the last 7 days
    public Cursor getWeeklyStepHistory() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_STEP_HISTORY +
                " ORDER BY date DESC LIMIT 7", null);
    }

    public void insertOrUpdateUser(int steps, String currentPet) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("id", 1); // always user ID = 1
        values.put("steps", steps);

        // Get current accumulated steps
        int accumulatedSteps = 0;
        Cursor cursor = db.rawQuery("SELECT accumulated_steps FROM " + TABLE_USER + " WHERE id = 1", null);
        if (cursor.moveToFirst()) {
            accumulatedSteps = cursor.getInt(0);
        }
        cursor.close();

        // Add today's steps to accumulated steps
        values.put("accumulated_steps", accumulatedSteps + steps);
        values.put("current_pet", currentPet);

        // Get current background WITHOUT closing the database
        String currentBg = null;
        cursor = db.rawQuery("SELECT current_background FROM " + TABLE_USER + " WHERE id = 1", null);
        if (cursor.moveToFirst()) {
            currentBg = cursor.getString(0);
        }
        cursor.close();

        // Preserve the current background
        if (currentBg != null) {
            values.put("current_background", currentBg);
        } else {
            values.put("current_background", "default_bg");
        }

        // Use REPLACE to insert or update
        db.insertWithOnConflict(TABLE_USER, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();

        // Update pet mood and experience
        recordDailySteps(steps);
    }
    public Cursor getUser() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_USER + " WHERE id = 1", null);
    }

    // ----- PET MANAGEMENT METHODS -----

    // Add a new pet to the database
    public long addPet(String name, int imageRes) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("mood", MOOD_NEUTRAL);
        values.put("level", 1);
        values.put("experience", 0.0f);
        values.put(COLUMN_PET_IMAGE, imageRes); // Add this line

        long petId = db.insert(TABLE_PETS, null, values);
        db.close();
        return petId;
    }

    // Get all pets
    public Cursor getAllPets() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT id, name, mood, level, experience, " + COLUMN_PET_IMAGE +
                " FROM " + TABLE_PETS, null);
    }

    // Get a specific pet by name
    public Cursor getPetByName(String name) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_PETS + " WHERE name = ?",
                new String[]{name});
    }

    // Update pet mood
    public void updatePetMood(String petName, String mood) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("mood", mood);

        db.update(TABLE_PETS, values, "name = ?", new String[]{petName});
        db.close();
    }

    // Update pet mood based on steps
    public void updatePetMoodBySteps(int steps) {
        // Get current pet
        String currentPet = getCurrentPetName();
        if (currentPet == null) return;

        String newMood;

        // Determine mood based on step count
        if (steps >= STEPS_FOR_EXCITED) {
            newMood = MOOD_EXCITED;
        } else if (steps >= STEPS_FOR_HAPPY) {
            newMood = MOOD_HAPPY;
        } else if (steps >= STEPS_FOR_NEUTRAL) {
            newMood = MOOD_NEUTRAL;
        } else {
            newMood = MOOD_SAD;
        }

        // Update pet mood
        updatePetMood(currentPet, newMood);
    }

    // Get current pet name
    public String getCurrentPetName() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT current_pet FROM " + TABLE_USER + " WHERE id = 1", null);

        String petName = null;
        if (cursor.moveToFirst()) {
            petName = cursor.getString(0);
        }

        cursor.close();
        db.close();
        return petName;
    }

    // Add experience points to current pet
    public void addExperienceToPet(String petName, float expToAdd) {
        if (petName == null) return;

        SQLiteDatabase db = this.getWritableDatabase();

        try {
            // Get current EXP and level
            Cursor cursor = db.rawQuery("SELECT level, experience FROM " + TABLE_PETS +
                    " WHERE name = ?", new String[]{petName});

            if (cursor.moveToFirst()) {
                int currentLevel = cursor.getInt(0);
                float currentExp = cursor.getFloat(1);
                float newExp = currentExp + expToAdd;

                // Check for level up
                float xpNeeded = calculateXpForLevel(currentLevel + 1);
                int newLevel = currentLevel;

                while (newExp >= xpNeeded) {
                    newLevel++;
                    newExp -= xpNeeded;
                    xpNeeded = calculateXpForLevel(newLevel + 1);
                }

                // Update the pet
                ContentValues values = new ContentValues();
                values.put("level", newLevel);
                values.put("experience", newExp);
                db.update(TABLE_PETS, values, "name = ?", new String[]{petName});
            }
            cursor.close();
        } catch (Exception e) {
            Log.e(TAG, "Error adding experience", e);
        } finally {
            db.close();
        }
    }

    // Calculate XP needed for a specific level
    private float calculateXpForLevel(int level) {
        if (level <= 1) return 0; // Level 1 starts at 0 XP
        return BASE_XP_FOR_LEVEL * (float)Math.pow(LEVEL_MULTIPLIER, level - 2);
    }

    public float getPetLevelProgress(String petName) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT level, experience FROM " + TABLE_PETS +
                " WHERE name = ?", new String[]{petName});

        float progress = 0f;

        if (cursor.moveToFirst()) {
            int currentLevel = cursor.getInt(0);
            float currentXp = cursor.getFloat(1);

            // This is the mistake - xpForCurrentLevel should be referring to
            // the XP required to reach the current level, not the next level
            float xpForNextLevel = calculateXpForLevel(currentLevel + 1);

            // Calculate progress as a percentage of the way to the next level
            progress = (currentXp / xpForNextLevel) * 100f;

            Log.d("DBHelper", "Level: " + currentLevel + " XP: " + currentXp +
                    " Next Level XP: " + xpForNextLevel + " Progress: " + progress + "%");
        }
        cursor.close();
        db.close();
        return progress;
    }
    // Add a new background
// Update your addBackground method
    public long addBackground(String name, int imageRes) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put(COLUMN_BG_IMAGE, imageRes);
        long id = db.insert(TABLE_BACKGROUNDS, null, values);
        db.close();
        return id;
    }

    // Get all backgrounds
    public Cursor getAllBackgrounds() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT id, name, " + COLUMN_BG_IMAGE +
                " FROM " + TABLE_BACKGROUNDS, null);
    }

    // Check if app is first run and initialize default data
    public void checkAndInitializeDefaultData() {
        SQLiteDatabase db = this.getWritableDatabase();

        try {
            // Check if user exists
            Cursor userCursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_USER, null);
            userCursor.moveToFirst();
            int userCount = userCursor.getInt(0);
            userCursor.close();

            // Check if pets exist
            Cursor petCursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_PETS, null);
            petCursor.moveToFirst();
            int petCount = petCursor.getInt(0);
            petCursor.close();

            // Initialize default pet if needed
            if (petCount == 0) {
                ContentValues petValues = new ContentValues();
                petValues.put("name", "Cat Pet");
                petValues.put("mood", MOOD_NEUTRAL);
                petValues.put("level", 1);
                petValues.put("experience", 0.0f);
                petValues.put(COLUMN_PET_IMAGE, R.drawable.pet_cat);
                db.insert(TABLE_PETS, null, petValues);
            }

            if (userCount == 0) {
                ContentValues userValues = new ContentValues();
                userValues.put("id", 1);
                userValues.put("steps", 0);
                userValues.put("current_pet", "Cat Pet");
                userValues.put("current_background", "default_bg"); // Set default background
                db.insert(TABLE_USER, null, userValues);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error initializing data", e);
        } finally {
            db.close();
        }
    }    // Add these to DatabaseHelper.java
    public boolean hasPet(String petName) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT 1 FROM " + TABLE_PETS + " WHERE name = ?",
                new String[]{petName});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    public boolean hasBackground(String bgName) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT 1 FROM " + TABLE_BACKGROUNDS + " WHERE name = ?",
                new String[]{bgName});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }


    // Add this method to set current background
    public void setCurrentBackground(String bgName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("current_background", bgName);
        int rowsAffected = db.update(TABLE_USER, values, "id = 1", null);
        db.close();
        Log.d(TAG, "Background set to: " + bgName + " (rows affected: " + rowsAffected + ")");
    }

    // Add this method to get current background
    public String getCurrentBackground() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT current_background FROM " + TABLE_USER + " WHERE id = 1", null);
        String bgName = null;
        if (cursor.moveToFirst()) {
            bgName = cursor.getString(0);
        }
        Log.e(TAG, "MYYY BG: "  +  bgName);

        cursor.close();
        db.close();
        return bgName;
    }



    public Cursor getBackgroundByName(String bgName) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_BACKGROUNDS + " WHERE name = ?",
                new String[]{bgName});
    }

    public int getAccumulatedSteps() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT accumulated_steps FROM " + TABLE_USER + " WHERE id = 1", null);
        int steps = 0;
        if (cursor.moveToFirst()) {
            steps = cursor.getInt(0);
        }
        cursor.close();
        return steps;
    }

}