package com.example.stepnpaws;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.Manifest;
import android.os.Build;
import android.os.IBinder;
import android.content.pm.PackageManager;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.util.Log;

public class SimpleStepService extends Service implements SensorEventListener {
    private static final String TAG = "SimpleStepService";
    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "step_counter_channel";
    
    private SensorManager sensorManager;
    private Sensor stepSensor;
    private DatabaseHelper dbHelper;
    private int totalSteps = 0;
    private int previousTotalSteps = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service onCreate called");

        // Initialize database helper
        dbHelper = new DatabaseHelper(this);

        // Initialize step counter
        SharedPreferences prefs = getSharedPreferences("StepPrefs", MODE_PRIVATE);
        previousTotalSteps = prefs.getInt("previousTotalSteps", 0);
        Log.d(TAG, "Previous total steps: " + previousTotalSteps);

        // Create notification and start as foreground service immediately
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());
        Log.d(TAG, "Service started in foreground");
        
        // Set up sensor
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            Log.d(TAG, "Step sensor available: " + (stepSensor != null));
            
            if (stepSensor != null && checkPermissions()) {
                Log.d(TAG, "Registering sensor listener");
                sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
            } else {
                Log.e(TAG, "Cannot register sensor listener. Sensor: " + (stepSensor != null) + 
                    ", Permissions: " + checkPermissions());
            }
        } else {
            Log.e(TAG, "SensorManager is null");
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Step Counter Service",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Tracks steps in the background");
            
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                Log.d(TAG, "Notification channel created");
            }
        }
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, HomeActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        flags |= PendingIntent.FLAG_IMMUTABLE;

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, flags);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("StepN'Paws")
                .setContentText("Counting your steps")
                .setSmallIcon(R.drawable.ic_launcher_foreground) // Use an icon from your app
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private boolean checkPermissions() {
        boolean hasPermission = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            hasPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) 
                    == PackageManager.PERMISSION_GRANTED;
            Log.d(TAG, "Activity Recognition permission status: " + hasPermission);
        }
        return hasPermission;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand called");
        return START_STICKY; // Restart if killed
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_STEP_COUNTER) {
            return;
        }
        
        int totalSteps = (int) event.values[0];
        Log.d(TAG, "Sensor event received. Total steps: " + totalSteps);
        
        SharedPreferences prefs = getSharedPreferences("StepPrefs", MODE_PRIVATE);
        int previousTotalSteps = prefs.getInt("previousTotalSteps", 0);
        int currentSteps;

        // Get today's date
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = dateFormat.format(new Date());
        String lastDate = prefs.getString("lastDate", "");

        // Check if it's a new day
        if (!lastDate.equals(today)) {
            Log.d(TAG, "New day detected. Resetting steps.");
            // New day: reset previousTotalSteps to current total steps
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("lastDate", today);
            editor.putInt("previousTotalSteps", totalSteps);
            editor.apply();
            currentSteps = 0;
        } else {
            // Same day: calculate current steps
            currentSteps = totalSteps - previousTotalSteps;
            Log.d(TAG, "Current steps today: " + currentSteps);
        }

        // Update database with today's steps
        String currentPet = dbHelper.getCurrentPetName();
        dbHelper.insertOrUpdateUser(currentSteps, currentPet);
        dbHelper.recordDailySteps(currentSteps);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service onDestroy called");
        if (sensorManager != null) {
            Log.d(TAG, "Unregistering sensor listener");
            sensorManager.unregisterListener(this);
        }
    }
}