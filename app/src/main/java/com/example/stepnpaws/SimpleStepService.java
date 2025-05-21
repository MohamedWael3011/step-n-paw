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

    import java.text.SimpleDateFormat;
    import java.util.Date;
    import java.util.Locale;

    public class SimpleStepService extends Service implements SensorEventListener {
        private SensorManager sensorManager;
        private Sensor stepSensor;
        private DatabaseHelper dbHelper;
        private int totalSteps = 0;
        private int previousTotalSteps = 0;

        @Override
        public void onCreate() {
            super.onCreate();

            // Initialize database helper
            dbHelper = new DatabaseHelper(this);

            // Initialize step counter
            SharedPreferences prefs = getSharedPreferences("StepPrefs", MODE_PRIVATE);
            previousTotalSteps = prefs.getInt("previousTotalSteps", 0);

            // Set up sensor
            sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            if (sensorManager != null) {
                stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
                if (stepSensor != null && checkPermissions()) {
                    sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
                }
            }

            // Create a notification channel for Android 8.0+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        "step_channel",
                        "Step Counter",
                        NotificationManager.IMPORTANCE_LOW);

                NotificationManager notificationManager = getSystemService(NotificationManager.class);
                notificationManager.createNotificationChannel(channel);
            }

            // Start as a foreground service with notification
            Intent notificationIntent = new Intent(this, HomeActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                    notificationIntent, PendingIntent.FLAG_IMMUTABLE);

            Notification notification = new NotificationCompat.Builder(this, "step_channel")
                    .setContentTitle("StepN'Paws")
                    .setContentText("Counting steps in background")
                    .setSmallIcon(android.R.drawable.ic_menu_compass)
                    .setContentIntent(pendingIntent)
                    .build();

            startForeground(1, notification);
        }

        private boolean checkPermissions() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                return checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) 
                        == PackageManager.PERMISSION_GRANTED;
            }
            return true;
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            return START_STICKY; // Restart if killed
        }

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            int totalSteps = (int) event.values[0];
            SharedPreferences prefs = getSharedPreferences("StepPrefs", MODE_PRIVATE);
            int previousTotalSteps = prefs.getInt("previousTotalSteps", 0);
            int currentSteps;

            // Get today's date
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String today = dateFormat.format(new Date());
            String lastDate = prefs.getString("lastDate", "");

            // Check if it's a new day
            if (!lastDate.equals(today)) {
                // New day: reset previousTotalSteps to current total steps
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("lastDate", today);
                editor.putInt("previousTotalSteps", totalSteps);
                editor.apply();
                currentSteps = 0;
            } else {
                // Same day: calculate current steps
                currentSteps = totalSteps - previousTotalSteps;
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
            if (sensorManager != null) {
                sensorManager.unregisterListener(this);
            }
        }
    }