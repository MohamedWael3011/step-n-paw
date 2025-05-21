package com.example.stepnpaws;

import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class PetRoomActivity extends AppCompatActivity {

    private ImageView petImage;
    private TextView petStatusText;
    private ProgressBar petExpProgress;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pet_room);

        // Initialize views
        petImage = findViewById(R.id.petImage);
        petStatusText = findViewById(R.id.petStatusText);
        petExpProgress = findViewById(R.id.petExpProgress);
        dbHelper = new DatabaseHelper(this);

        setupInteractionButtons();
        updatePetUI();
    }

    private void setupInteractionButtons() {
        // Set up all interaction buttons
        findViewById(R.id.btnPet).setOnClickListener(v -> {
            interactWithPet("pet");
        });

        findViewById(R.id.btnFeed).setOnClickListener(v -> {
            interactWithPet("feed");
        });

        findViewById(R.id.btnPlay).setOnClickListener(v -> {
            interactWithPet("play");
        });

        findViewById(R.id.btnRest).setOnClickListener(v -> {
            interactWithPet("rest");
        });
    }


    private long lastInteractionTime = 0;
    private static final long INTERACTION_COOLDOWN = 500; // 0.5 seconds

    private void interactWithPet(String action) {
        // Check cooldown
        if (System.currentTimeMillis() - lastInteractionTime < INTERACTION_COOLDOWN) {
            return;
        }
        lastInteractionTime = System.currentTimeMillis();

        String currentPet = dbHelper.getCurrentPetName();
        if (currentPet == null) return;

        // Perform interaction
        PetUtility.interactWithPet(this, action);

        // Update UI with a slight delay to ensure DB operations complete
        updatePetUI();
        showQuickStatus(action);

        // Add a small delay and update again to ensure progress bar updates
        petExpProgress.postDelayed(() -> {
            updatePetUI();
        }, 1000);
    }

    private void showQuickStatus(String action) {
        String currentPet = dbHelper.getCurrentPetName();
        if (currentPet != null) {
            petStatusText.setText(PetUtility.getInteractionMessage(currentPet, action));
            petStatusText.setAlpha(1f);
            petStatusText.postDelayed(() -> petStatusText.setAlpha(0f), 1000);
        }
    }

    private void updatePetUI() {
        String currentPet = dbHelper.getCurrentPetName();
        if (currentPet != null) {
            Cursor petCursor = dbHelper.getPetByName(currentPet);
            if (petCursor.moveToFirst()) {
                // Get all pet data from cursor
                String name = petCursor.getString(petCursor.getColumnIndexOrThrow("name"));
                String mood = petCursor.getString(petCursor.getColumnIndexOrThrow("mood"));
                int level = petCursor.getInt(petCursor.getColumnIndexOrThrow("level"));
                float experience = petCursor.getFloat(petCursor.getColumnIndexOrThrow("experience"));
                int imageRes = petCursor.getInt(petCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PET_IMAGE));

                // Calculate progress
                float progress = dbHelper.getPetLevelProgress(currentPet);
                petExpProgress.post(() -> {
                    petExpProgress.setProgress((int)progress);
                });

                // Update all UI elements
                petStatusText.setText(name + " (Lvl " + level + ")");
                petImage.setImageResource(imageRes);  // Set the correct pet image

                Log.d("PetRoom", "Updated UI - " +
                        name + " | Lvl: " + level +
                        " | Exp: " + experience +
                        " | Progress: " + progress + "%");
            }
            petCursor.close();
        } else {
            Log.d("PetRoom", "No current pet selected");
        }
    }
}
