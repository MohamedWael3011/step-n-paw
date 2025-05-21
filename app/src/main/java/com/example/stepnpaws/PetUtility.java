package com.example.stepnpaws;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

/**
 * Utility class to handle pet animations, mood-based behaviors, and level-related features
 */
public class PetUtility {

    private static final String TAG = "PetUtility";

    /**
     * Apply animation to pet based on its mood
     * @param petImage The ImageView containing the pet sprite
     * @param mood The pet's current mood
     */
    public static void animatePet(ImageView petImage, String mood) {
        // Clear any existing animations
        petImage.clearAnimation();

        ObjectAnimator scaleY = ObjectAnimator.ofFloat(petImage, "scaleY", 1f, 1.05f);
        scaleY.setDuration(1000);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setRepeatMode(ValueAnimator.REVERSE);

        switch (mood) {
            case DatabaseHelper.MOOD_EXCITED:
                // Excited animation
                ObjectAnimator jump = ObjectAnimator.ofFloat(petImage, "translationY", 0f, -20f);
                jump.setDuration(500);
                jump.setRepeatCount(ValueAnimator.INFINITE);
                jump.setRepeatMode(ValueAnimator.REVERSE);
                jump.start();
                break;

            case DatabaseHelper.MOOD_SAD:
                // Sad animation
                petImage.setRotation(-5f);
                ObjectAnimator rotate = ObjectAnimator.ofFloat(petImage, "rotation", -5f, 5f);
                rotate.setDuration(2000);
                rotate.setRepeatCount(ValueAnimator.INFINITE);
                rotate.setRepeatMode(ValueAnimator.REVERSE);
                rotate.start();
                break;

            case DatabaseHelper.MOOD_SLEEPY:
                // Sleepy animation
                ObjectAnimator alpha = ObjectAnimator.ofFloat(petImage, "alpha", 0.8f, 1f);
                alpha.setDuration(1500);
                alpha.setRepeatCount(ValueAnimator.INFINITE);
                alpha.setRepeatMode(ValueAnimator.REVERSE);
                alpha.start();
                break;
        }

        scaleY.start();
    }

    /**
     * Get animation resource ID based on mood
     * Note: You need to create these animation resources in your drawable folder
     */

    /**
     * Apply color tint to the text based on mood
     */
    public static void styleMoodText(TextView textView, String mood) {
        int colorResId;

        switch (mood) {
            case DatabaseHelper.MOOD_HAPPY:
                colorResId = android.R.color.holo_green_dark;
                break;
            case DatabaseHelper.MOOD_EXCITED:
                colorResId = android.R.color.holo_orange_light;
                break;
            case DatabaseHelper.MOOD_SAD:
                colorResId = android.R.color.holo_blue_dark;
                break;
            case DatabaseHelper.MOOD_SLEEPY:
                colorResId = android.R.color.darker_gray;
                break;
            default: // MOOD_NEUTRAL
                colorResId = android.R.color.holo_purple;
                break;
        }

        textView.setTextColor(ContextCompat.getColor(textView.getContext(), colorResId));
        textView.setText("Mood: " + mood);

    }

    /**
     * Update pet UI with current pet data
     */
    public static void updatePetUI(Context context, ImageView petImage, TextView nameText,
                                   TextView moodText, TextView levelText, View levelProgressView) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        String currentPet = dbHelper.getCurrentPetName();

        if (currentPet == null) {
            Log.e(TAG, "No current pet found");
            return;
        }

        Cursor petCursor = dbHelper.getPetByName(currentPet);

        if (petCursor.moveToFirst()) {
            String name = petCursor.getString(petCursor.getColumnIndexOrThrow("name"));
            String mood = petCursor.getString(petCursor.getColumnIndexOrThrow("mood"));
            int level = petCursor.getInt(petCursor.getColumnIndexOrThrow("level"));

            // Update UI elements
            nameText.setText(name);
            levelText.setText("Level: " + level);
            styleMoodText(moodText, mood);

            // Animate pet based on mood
            animatePet(petImage, mood);

            // Update level progress if view provided
            if (levelProgressView != null && levelProgressView instanceof android.widget.ProgressBar) {
                android.widget.ProgressBar progressBar = (android.widget.ProgressBar) levelProgressView;
                float progress = dbHelper.getPetLevelProgress(name);
                progressBar.setProgress((int) progress);
            }
        }

        petCursor.close();
    }

    /**
     * Handle interaction with pet based on mood
     */
    public static void interactWithPet(Context context, String action) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        String currentPet = dbHelper.getCurrentPetName();

        if (currentPet == null) {
            Toast.makeText(context, "No pet to interact with!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get current level and EXP before adding more
        Cursor cursor = dbHelper.getPetByName(currentPet);
        cursor.moveToFirst();
        int currentLevel = cursor.getInt(cursor.getColumnIndexOrThrow("level"));
        float currentExp = cursor.getFloat(cursor.getColumnIndexOrThrow("experience"));
        String mood = cursor.getString(cursor.getColumnIndexOrThrow("mood"));
        cursor.close();

        // Grant different EXP amounts based on interaction type
        float expToAdd = 0;
        switch (action.toLowerCase()) {
            case "pet":
                expToAdd = 10f;
                break;
            case "feed":
                expToAdd = 15f;
                break;
            case "play":
                expToAdd = 20f;
                break;
            case "rest":
                expToAdd = 5f;
                break;
        }

        Log.d("PetUtility", "Adding " + expToAdd + " EXP to " + currentPet);


        dbHelper.addExperienceToPet(currentPet, expToAdd);


        // Different responses based on pet mood and action
        String response = "";

        switch (action.toLowerCase()) {
            case "pet":
                if (mood.equals(DatabaseHelper.MOOD_HAPPY) || mood.equals(DatabaseHelper.MOOD_EXCITED)) {
                    response = currentPet + " purrs happily!";
                } else if (mood.equals(DatabaseHelper.MOOD_SAD)) {
                    response = currentPet + " feels a little better.";
                    dbHelper.updatePetMood(currentPet, DatabaseHelper.MOOD_NEUTRAL);
                } else if (mood.equals(DatabaseHelper.MOOD_SLEEPY)) {
                    response = currentPet + " opens one eye but continues napping.";
                } else {
                    response = currentPet + " enjoys the attention.";
                }
                break;

            case "feed":
                if (mood.equals(DatabaseHelper.MOOD_SAD)) {
                    response = currentPet + " eats slowly but feels better.";
                    dbHelper.updatePetMood(currentPet, DatabaseHelper.MOOD_NEUTRAL);
                } else if (mood.equals(DatabaseHelper.MOOD_SLEEPY)) {
                    response = currentPet + " wakes up for food!";
                } else {
                    response = currentPet + " enjoys the meal!";
                    if (mood.equals(DatabaseHelper.MOOD_NEUTRAL)) {
                    }
                }
                break;

            case "play":
                if (mood.equals(DatabaseHelper.MOOD_SLEEPY)) {
                    response = currentPet + " is too tired to play right now.";
                } else if (mood.equals(DatabaseHelper.MOOD_SAD)) {
                    response = currentPet + " reluctantly plays with you.";
                } else {
                    response = currentPet + " jumps around excitedly!";
                }
                break;

            case "rest":
                response = currentPet + " takes a nap.";
                dbHelper.updatePetMood(currentPet, DatabaseHelper.MOOD_SLEEPY);
                break;
        }

        Toast.makeText(context, response, Toast.LENGTH_SHORT).show();

    }

    public static String getInteractionMessage(String petName, String action) {
        if (petName == null) return "No pet to interact with!";

        switch (action.toLowerCase()) {
            case "pet": return petName + " loves the attention!";
            case "feed": return petName + " is happily munching!";
            case "play": return petName + " is having fun!";
            case "rest": return petName + " is getting some rest.";
            default: return "Interacting with " + petName;
        }
    }
}