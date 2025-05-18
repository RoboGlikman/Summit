package com.example.summit;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.content.res.Configuration;
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
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

/**
 * A foreground service responsible for recording audio and transcribing speech to text.
 * It uses the Android SpeechRecognizer API and provides notifications to the user
 * about the recording status. Supports English and Hebrew based on system language.
 */
public class RecordingService extends Service {
    /**
     * Action to start the recording service.
     */
    public static final String ACTION_START = "START_RECORDING";
    /**
     * Action to stop the recording service.
     */
    public static final String ACTION_STOP = "STOP_RECORDING";
    private static final String NOTIFICATION_CHANNEL_ID = "speech_service_channel";
    private static final int NOTIFICATION_ID = 1234; // Use the ID you're already using
    private SpeechRecognizer speechRecognizer;
    private boolean isRecording = false;
    private boolean hasProcessedResult = false;
    private String currentLanguageCode;

    /**
     * Called when the service is first created. This is where you initialize
     * the speech recognizer and set up the recognition listener.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        determineCurrentLanguage();
        initializeSpeechRecognizer();
    }

    /**
     * Determines the current language code based on the system's locale.
     */
    private void determineCurrentLanguage() {
        Locale currentLocale = getResources().getConfiguration().locale;
        String language = currentLocale.getLanguage();
        if ("iw".equals(language)) {
            currentLanguageCode = "iw-IL"; // Hebrew (Israel)
            Log.d("RecordingService", "Current language set to Hebrew.");
        } else {
            currentLanguageCode = "en-US"; // Default to English (United States)
            Log.d("RecordingService", "Current language set to English.");
        }
    }

    /**
     * Initializes the SpeechRecognizer and sets up the recognition listener.
     */
    private void initializeSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                Log.d("SpeechRecognizer", "Ready for speech in " + currentLanguageCode + "...");
            }

            @Override
            public void onBeginningOfSpeech() {
                Log.d("SpeechRecognizer", "Speech started in " + currentLanguageCode + "...");
            }

            @Override
            public void onRmsChanged(float rmsdB) {
                // Optional
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
                // Optional
            }

            @Override
            public void onEndOfSpeech() {
                Log.d("SpeechRecognizer", "End of speech in " + currentLanguageCode + "...");
            }

            @Override
            public void onError(int error) {
                // Error handling remains the same
                String errorMessage = getErrorMessage(error);
                Log.e("SpeechRecognizer", errorMessage);
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(RecordingService.this, errorMessage, Toast.LENGTH_SHORT).show();
                });
                stopServiceSafely();
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String recognizedText = matches.get(0);
                    Log.d("SpeechRecognizer", "Recognized (" + currentLanguageCode + "): " + recognizedText);
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
                    Log.d("SpeechRecognizer", "Partial (" + currentLanguageCode + "): " + partialText);
                }
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
                // Reserved
            }
        });
    }

    /**
     * Called when an action specified by the intent needs to be performed.
     * This is where the service receives commands to start or stop recording.
     *
     * @param intent  The Intent supplied to {@link #startService(Intent)},
     * as given. This may be null if the service is being restarted
     * after its process has gone away.
     * @param flags   Additional data about this start request.
     * @param startId A unique integer representing this specific request to
     * start. Use this to avoid stopping the service based on
     * the wrong {@link #stopSelfResult(int)} call.
     * @return The return value indicates what semantics the system should
     * use for the service's current started state. It may be one of the
     * constants associated with the {@link #START_CONTINUATION_MASK} bits.
     */
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

    /**
     * Starts the service in the foreground, displaying a notification to the user.
     * This is necessary for long-running background tasks like audio recording to prevent
     * the system from killing the service.
     */
    private void startForegroundService() {
        if (Build.VERSION.SDK_INT < 34) {
            startForeground(NOTIFICATION_ID, getNotification());
        } else {
            startForeground(NOTIFICATION_ID, getNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
        }
        Log.d("SpeechRecognizer", "started foreground explicitly");
    }

    /**
     * Starts the speech recognition process. It initializes the RecognizerIntent
     * with the appropriate language model and starts listening in the determined language.
     */
    private void startRecording() {
        if (isRecording) return;
        isRecording = true;

        Intent recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentLanguageCode); // Set the language
        speechRecognizer.startListening(recognizerIntent);
        Log.d("SpeechRecognizer", "Started listening in " + currentLanguageCode + "...");
    }

    /**
     * Stops the speech recognition process. It stops the SpeechRecognizer
     * and removes the foreground notification (allowing the service to be stopped).
     */
    private void stopRecording() {
        if (!isRecording) return;
        isRecording = false;
        speechRecognizer.stopListening();

        stopForeground(false); // Keep notification
        stopServiceSafely();
    }

    /**
     * Stops the service safely after a short delay if no new recording has started
     * and if at least one result has been processed. This ensures that the
     * "RECORDING_DONE" broadcast is sent before the service is destroyed.
     */
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

    /**
     * Builds and returns the notification displayed when the service is running in the foreground.
     * The notification includes an action to stop the recording.
     *
     * @return The foreground service notification.
     */
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
                .setContentText("Recording lecture...") // Consider making this dynamic based on language
                .setSmallIcon(R.drawable.baseline_mic_24)
                .setOngoing(true)
                .addAction(R.drawable.baseline_mic_off_24, "Stop", stopPendingIntent) // Consider making this dynamic based on language
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .build();
    }

    /**
     * Creates a notification channel for displaying foreground service notifications.
     * This is required for Android 8.0 (Oreo) and higher.
     */
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

    /**
     * Sends a local broadcast indicating that the recording process is complete.
     * This allows other parts of the application to be notified when the audio
     * has been processed and results are available (or when the service is stopped).
     */
    private void sendRecordingDone() {
        Log.d("SpeechRecognizer", "Sending RECORDING_DONE broadcast");
        Intent intent = new Intent("com.example.summit.RECORDING_DONE");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        hasProcessedResult = false;
    }

    /**
     * Helper function to get a user-friendly error message from the speech recognizer error code.
     *
     * @param error The speech recognizer error code.
     * @return A human-readable error message.
     */
    private String getErrorMessage(int error) {
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
        return errorMessage;
    }

    /**
     * Called when another component wants to bind with the service (e.g., for RPC).
     * This service does not support binding, so it returns null.
     *
     * @param intent The Intent that was used to bind to this service.
     * @return null, as this service does not allow binding.
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Called when the service is no longer used and is being destroyed.
     * It is important to release resources here, such as the SpeechRecognizer.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
        Log.d("RecordingService", "Service destroyed.");
    }
}