package com.example.sos;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.util.ArrayList;

public class ServiceMine extends Service implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Vibrator vibrator;
    private FusedLocationProviderClient fusedLocationClient;
    private SmsManager smsManager;

    private long lastShakeTime;
    private static final int SHAKE_THRESHOLD = 12;
    private String myLocation = "Location not available";

    private boolean isRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();

        smsManager = SmsManager.getDefault();
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        fetchLocation();
    }

    private void fetchLocation() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        myLocation = "https://maps.google.com/maps?q="
                                + location.getLatitude() + ","
                                + location.getLongitude();
                    }
                });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null && intent.getAction() != null &&
                intent.getAction().equalsIgnoreCase("STOP")) {

            stopSelf();
            return START_NOT_STICKY;
        }

        startForegroundService();
        registerSensor();

        return START_STICKY;
    }

    private void startForegroundService() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel =
                    new NotificationChannel("MYID",
                            "SOS Channel",
                            NotificationManager.IMPORTANCE_DEFAULT);

            NotificationManager manager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);

            Intent notificationIntent =
                    new Intent(this, MainActivity.class);

            PendingIntent pendingIntent =
                    PendingIntent.getActivity(this, 0,
                            notificationIntent,
                            PendingIntent.FLAG_IMMUTABLE);

            Notification notification =
                    new Notification.Builder(this, "MYID")
                            .setContentTitle("SOS Monitoring Active")
                            .setContentText("Shake phone to send emergency alert")
                            .setSmallIcon(R.drawable.siren)
                            .setContentIntent(pendingIntent)
                            .build();

            startForeground(1, notification);
            isRunning = true;
        }
    }

    private void registerSensor() {
        if (accelerometer != null) {
            sensorManager.registerListener(this,
                    accelerometer,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER)
            return;

        long currentTime = System.currentTimeMillis();

        if ((currentTime - lastShakeTime) < 5000)
            return;

        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        double acceleration = Math.sqrt(x * x + y * y + z * z);

        if (acceleration > SHAKE_THRESHOLD) {

            Log.d("SHAKE", "Shake detected");

            lastShakeTime = currentTime;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                        VibrationEffect.createOneShot(
                                500,
                                VibrationEffect.DEFAULT_AMPLITUDE));
            }

            sendSOS();
        }
    }

    private void sendSOS() {

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {

            Log.d("SMS", "Permission missing");
            return;
        }

        fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
        ).addOnSuccessListener(location -> {

            String locationLink;

            if (location != null) {
                locationLink = "https://maps.google.com/maps?q="
                        + location.getLatitude() + ","
                        + location.getLongitude();
            } else {
                locationLink = "Location not available";
            }

            DatabaseHelper db = new DatabaseHelper(this);
            ArrayList<ContactModel> contacts = db.fetchData();

            if (contacts.isEmpty()) {
                Log.d("SMS", "No contacts found");
                return;
            }

            SharedPreferences sp =
                    getSharedPreferences("message", MODE_PRIVATE);
            String customMsg = sp.getString("msg", null);

            for (ContactModel c : contacts) {

                String message;

                if (customMsg != null) {
                    message = "Hey " + c.getName() + " " + customMsg
                            + "\n\nLocation:\n" + locationLink;
                } else {
                    message = "Hey " + c.getName()
                            + " I am in DANGER! Please help.\n\nLocation:\n"
                            + locationLink;
                }

                smsManager.sendTextMessage(
                        c.getNumber(),
                        null,
                        message,
                        null,
                        null);

                Log.d("SMS", "Sent to: " + c.getNumber());
            }
        });
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}