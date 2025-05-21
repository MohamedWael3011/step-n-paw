package com.example.stepnpaws;

import static com.example.stepnpaws.DatabaseHelper.COLUMN_BG_IMAGE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.mikhaellopez.circularprogressbar.CircularProgressBar;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity {
    private static final String TAG = "HomeActivity";
    private static final int PERMISSION_REQUEST_CODE = 123;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // Permission granted, start the service
                    startStepService();
                } else {
                    // Permission denied, show error
                    Toast.makeText(this, "Permission required for step counting", Toast.LENGTH_LONG).show();
                }
            });

    DatabaseHelper dbHelper;
    private SensorManager sensorManager;
    private Sensor stepSensor;
    private boolean isSensorPresent = false;
    private int totalSteps = 0;
    private int previousTotalSteps = 0;

    private CircularProgressBar stepProgressBar;
    private TextView stepCountText;
    private TextView stepText;
    private TextView petName;
    private TextView petMood;
    private TextView petLevel;
    private ImageView petImage;
    private ImageView roomBackground;
    private ProgressBar petExpProgress;

    private void loadInitialStepCount() {
        // Get the saved previousTotalSteps value
        SharedPreferences prefs = getSharedPreferences("StepPrefs", MODE_PRIVATE);
        previousTotalSteps = prefs.getInt("previousTotalSteps", 0);

        // Check if we need to initialize for a new day
        String lastDate = prefs.getString("lastDate", "");
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        if (!lastDate.equals(today)) {
            // New day: reset previousTotalSteps to current total steps
            if (totalSteps > 0) {
                previousTotalSteps = totalSteps;
                prefs.edit()
                    .putInt("previousTotalSteps", previousTotalSteps)
                    .putString("lastDate", today)
                    .apply();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            // Refresh all UI elements
            updateBackground();
            loadUserData();
        }
    }

    SensorEventListener stepListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            totalSteps = (int) event.values[0];

            // This is the key part - subtract the previous total to get today's steps
            int currentSteps = totalSteps - previousTotalSteps;

            // Update the step count text and progress view
            stepCountText.setText(String.valueOf(currentSteps));
            stepProgressBar.setProgress(currentSteps);

            // Get current pet name
            String currentPet = dbHelper.getCurrentPetName();

            // Save current steps and update pet mood/exp
            dbHelper.insertOrUpdateUser(currentSteps, currentPet);
            dbHelper.addExperienceToPet(currentPet, currentSteps);

            // Refresh pet UI with updated info
            updatePetDisplay(currentPet, currentSteps);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };

    @SuppressLint("CutPasteId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Find all UI components
        stepProgressBar = findViewById(R.id.stepProgressBar);
        stepCountText = findViewById(R.id.stepCountText);
        stepText = findViewById(R.id.stepText);
        petImage = findViewById(R.id.petImage);
        roomBackground = findViewById(R.id.roomBackground);
        petName = findViewById(R.id.petName);
        petMood = findViewById(R.id.petMood);
        petLevel = findViewById(R.id.petLevel);
        TextView currentDate = findViewById(R.id.current_date);
        petExpProgress = findViewById(R.id.petExpProgress);

        // Set up step sensor
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        if (sensorManager != null) {
            stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            isSensorPresent = (stepSensor != null);
            loadInitialStepCount();
        }

        if (!isSensorPresent) {
            showSensorError();
        }

        stepProgressBar.setProgressMax(10000);

        // Set current date
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
        currentDate.setText(dateFormat.format(new Date()));

        dbHelper = new DatabaseHelper(this);

        // Initialize default data if first run
        dbHelper.checkAndInitializeDefaultData();

        // Load user data
        loadUserData();

        // Set button click listeners
        setupButtonClickListeners();

        // Make pet image clickable for quick interaction
        petImage.setOnClickListener(v -> {
            PetUtility.interactWithPet(this, "pet");
            // Refresh pet UI after interaction
            PetUtility.updatePetUI(this, petImage, petName, petMood, petLevel, petExpProgress);
        });

        // Check and request permissions before starting service
        checkAndRequestPermissions();
    }

    private void checkAndRequestPermissions() {
        Log.d(TAG, "Checking permissions...");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            boolean hasPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                    == PackageManager.PERMISSION_GRANTED;
            Log.d(TAG, "Activity Recognition permission status: " + hasPermission);
            
            if (!hasPermission) {
                Log.d(TAG, "Requesting Activity Recognition permission");
                requestPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION);
            } else {
                Log.d(TAG, "Permission already granted, starting service");
                startStepService();
            }
        } else {
            Log.d(TAG, "Android version < Q, no permission needed");
            startStepService();
        }
    }

    private void startStepService() {
        Log.d(TAG, "Starting step service...");
        Intent serviceIntent = new Intent(this, SimpleStepService.class);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Log.d(TAG, "Using startForegroundService() for Android O+");
                startForegroundService(serviceIntent);
            } else {
                Log.d(TAG, "Using startService() for pre-Android O");
                startService(serviceIntent);
            }
            Log.d(TAG, "Service started successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error starting service: " + e.getMessage(), e);
            Toast.makeText(this, "Error starting step counter service: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showSensorError() {
        stepCountText.setText("0");
        stepText.setText("Step counter not available");
        Toast.makeText(this, "Step sensor not found on this device", Toast.LENGTH_LONG).show();
    }

    private void updateBackground() {
        String currentBg = dbHelper.getCurrentBackground();
        Log.d(TAG, "Updating background to: " + currentBg);

        if (currentBg == null) {
            roomBackground.setImageResource(R.drawable.default_bg);
            return;
        }

        switch(currentBg) {
            case "Green":
                roomBackground.setImageResource(R.drawable.bg_green);
                break;
            case "Pink":
                roomBackground.setImageResource(R.drawable.bg_pink);
                break;
            case "Orange":
                roomBackground.setImageResource(R.drawable.bg_orange);
                break;
            case "Brown":
                roomBackground.setImageResource(R.drawable.bg_brown);
                break;
            case "Blue":
                roomBackground.setImageResource(R.drawable.bg_blue);
                break;
            case "default_bg":
                roomBackground.setImageResource(R.drawable.default_bg);
                break;
            default:
                // Check if it's a purchased background
                Cursor cursor = dbHelper.getBackgroundByName(currentBg);
                if (cursor != null && cursor.moveToFirst()) {
                    int resId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_BG_IMAGE));
                    roomBackground.setImageResource(resId);
                } else {
                    roomBackground.setImageResource(R.drawable.default_bg);
                }
                if (cursor != null) cursor.close();
        }
    }
    private void loadUserData() {
        // Fetch and display user data
        Cursor userCursor = dbHelper.getUser();
        if (userCursor.moveToFirst()) {
            int steps = userCursor.getInt(userCursor.getColumnIndexOrThrow("steps"));
            String currentPetName = dbHelper.getCurrentPetName();

            // Update UI with step data
            stepCountText.setText(String.valueOf(steps));
            stepProgressBar.setProgress(steps);

            // Update pet UI
            updatePetDisplay(currentPetName, steps);

            Log.d(TAG, "Steps: " + steps + " | Current Pet: " + currentPetName);
        }
        updateBackground();
        userCursor.close();
    }

    private void updatePetDisplay(String petName, int steps) {
        if (petName == null) return;

        Cursor petCursor = dbHelper.getPetByName(petName);
        if (petCursor.moveToFirst()) {
            // Get pet details
            String name = petCursor.getString(petCursor.getColumnIndexOrThrow("name"));
            String mood = petCursor.getString(petCursor.getColumnIndexOrThrow("mood"));
            int level = petCursor.getInt(petCursor.getColumnIndexOrThrow("level"));
            float experience = petCursor.getFloat(petCursor.getColumnIndexOrThrow("experience"));
            int imageRes = petCursor.getInt(petCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PET_IMAGE));

            // Update UI elements
            this.petName.setText(name);
            this.petLevel.setText("Lvl: " + level);
            this.petImage.setImageResource(imageRes);

            // Rest of your existing code...
            PetUtility.styleMoodText(petMood, mood);
            float progress = dbHelper.getPetLevelProgress(name);
            petExpProgress.setProgress((int) progress);
            PetUtility.animatePet(petImage, mood);
            dbHelper.updatePetMoodBySteps(steps);
        }
        petCursor.close();
    }

    private void setupButtonClickListeners() {
        Button btnPetRoom = findViewById(R.id.btn_pet_room);
        btnPetRoom.setOnClickListener(v -> {
            startActivity(new Intent(this, PetRoomActivity.class));
        });

        Button btnShop = findViewById(R.id.btn_shop);
        btnShop.setOnClickListener(v -> {
            startActivity(new Intent(this, ShopActivity.class));
        });

        Button btnInventory = findViewById(R.id.btn_inventory);
        btnInventory.setOnClickListener(v -> {
            startActivityForResult(new Intent(this, InventoryActivity.class), 1);
        });

        Button btnStepHistory = findViewById(R.id.btn_step_history);
        btnStepHistory.setOnClickListener(v -> {
            startActivity(new Intent(this, StepHistoryActivity.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
        checkDailyReset();
        if (isSensorPresent) {
            Log.d(TAG, "Registering sensor listener");
            sensorManager.registerListener(stepListener, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            Log.d(TAG, "Sensor not present");
        }
        loadUserData();
        updateBackground();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause called");
        if(isSensorPresent) {
            Log.d(TAG, "Unregistering sensor listener");
            sensorManager.unregisterListener(stepListener);
        }
    }

    // Helper to reset steps at midnight (you'll need to implement proper daily reset logic)
    private void checkDailyReset() {
        // This now only updates the UI, actual step reset is handled by the service
        SharedPreferences prefs = getSharedPreferences("StepPrefs", MODE_PRIVATE);
        String lastDate = prefs.getString("lastDate", "");
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        if (!lastDate.equals(today)) {
            // Update UI elements for new day
            stepCountText.setText("0");
            stepProgressBar.setProgress(0);
        }
    }
}