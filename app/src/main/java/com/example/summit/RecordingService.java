package com.example.summit;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

public class RecordingService extends Service {
    public static final String ACTION_START = "START_RECORDING";
    public static final String ACTION_STOP = "STOP_RECORDING";
    private static final String NOTIFICATION_CHANNEL_ID = "speech_service_channel";
    private static final int NOTIFICATION_ID = 1234; // Use the ID you're already using
    private SpeechRecognizer speechRecognizer;
    private boolean isRecording = false;
    private boolean hasProcessedResult = false;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                Log.d("SpeechRecognizer", "Ready for speech...");
            }

            @Override
            public void onBeginningOfSpeech() {
                Log.d("SpeechRecognizer", "Speech has started...");
            }

            @Override
            public void onRmsChanged(float rmsdB) {

            }

            @Override
            public void onBufferReceived(byte[] buffer) {

            }

            @Override
            public void onEndOfSpeech() {
                Log.d("SpeechRecognizer", "End of speech...");
            }

            @Override
            public void onError(int error) {
                String errorMessage = "Speech recognition error: ";
                switch (error) {
                    case SpeechRecognizer.ERROR_AUDIO:
                        errorMessage += "Audio recording error.";
                        break;
                    case SpeechRecognizer.ERROR_CLIENT:
                        errorMessage += "Client side error.";
                        break;
                    case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                        errorMessage += "Insufficient permissions.";
                        break;
                    case SpeechRecognizer.ERROR_NETWORK:
                        errorMessage += "Network error.";
                        break;
                    case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                        errorMessage += "Network timeout.";
                        break;
                    case SpeechRecognizer.ERROR_NO_MATCH:
                        errorMessage += "No speech input matched.";
                        break;
                    case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                        errorMessage += "Speech recognizer is busy.";
                        break;
                    case SpeechRecognizer.ERROR_SERVER:
                        errorMessage += "Server error.";
                        break;
                    case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                        errorMessage += "No speech input in time.";
                        break;
                    default:
                        errorMessage += "Unknown error.";
                }
                Log.e("SpeechRecognizer", errorMessage);

                String finalErrorMessage = errorMessage;
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(RecordingService.this, finalErrorMessage, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String recognizedText = matches.get(0);
                    Log.d("SpeechRecognizer", "recognized text: " + recognizedText);
                    Intent intent = new Intent("com.example.summit.GOT_RESULT");
                    intent.putExtra("recognizedText", recognizedText);
                    LocalBroadcastManager.getInstance(RecordingService.this).sendBroadcast(intent);
                    hasProcessedResult = true;
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                ArrayList<String> partialMatches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (partialMatches != null && !partialMatches.isEmpty()) {
                    String partialText = partialMatches.get(0);
                    Log.d("SpeechRecognizer", "Partial result: " + partialText);
                }
            }

            @Override
            public void onEvent(int eventType, Bundle params) {

            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if (intent.hasExtra("action")) {
                String action = intent.getStringExtra("action");
                if (ACTION_START.equals(action)) {
                    startRecording();
                    startForegroundService(); // Start foreground here
                } else if (ACTION_STOP.equals(action)) {
                    stopRecording();
                }
            }
        }
        return START_STICKY;
    }

    private void startForegroundService() {
        if (Build.VERSION.SDK_INT < 34) {
            startForeground(NOTIFICATION_ID, getNotification());
        } else {
            startForeground(NOTIFICATION_ID, getNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
        }
        Log.d("SpeechRecognizer", "started foreground explicitly");
    }

    private void startRecording() {
        if (isRecording) return;
        isRecording = true;

        Intent recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
        speechRecognizer.startListening(recognizerIntent);
        Log.d("SpeechRecognizer", "Started listening...");
    }

    private void stopRecording() {
        if (!isRecording) return;
        isRecording = false;
        speechRecognizer.stopListening();

        stopForeground(false); // Keep notification
        stopServiceSafely();
    }

    public void stopServiceSafely() {
        Log.d("RecordingService", "stopServiceSafely called. hasProcessedResult: " + hasProcessedResult);
        if (hasProcessedResult) {
            sendRecordingDone();
            stopSelf();
        } else {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Log.d("RecordingService", "Delayed stop check. hasProcessedResult: " + hasProcessedResult);
                if (!isRecording) { // Check if recording hasn't restarted
                    sendRecordingDone();
                    stopSelf();
                }
            }, 500);
        }
    }

    private Notification getNotification() {
        Intent stopIntent = new Intent(this, RecordingService.class);
        stopIntent.putExtra("action", ACTION_STOP); // Use putExtra to send the stop command
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this,
                0,
                stopIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_CANCEL_CURRENT
        );

        return new NotificationCompat.Builder(this, "notif id") // Use the same channel ID
                .setContentTitle("Summit")
                .setContentText("Recording lecture...")
                .setSmallIcon(R.drawable.baseline_mic_24)
                .setOngoing(true)
                .addAction(R.drawable.baseline_mic_off_24, "Stop", stopPendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    "notif id", // Use the same channel ID you use in onCreate
                    "Main Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private void sendRecordingDone() {
        Log.d("SpeechRecognizer", "Sending RECORDING_DONE broadcast");
        Intent intent = new Intent("com.example.summit.RECORDING_DONE");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        hasProcessedResult = false;

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}