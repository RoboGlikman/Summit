package com.example.summit;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

public class RecordingService extends Service {
    public static final String ACTION_START = "START_RECORDING";
    public static final String ACTION_STOP = "STOP_RECORDING";
    private SpeechRecognizer speechRecognizer;
    private boolean isRecording = false;
    private boolean isStopping = false;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();

        if (Build.VERSION.SDK_INT < 34) startForeground(1234, getNotification());
        else startForeground(1234, getNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
        Log.d("SpeechRecognizer", "started foreground");

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
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {

                    String recognizedText = matches.get(0); // Get the first result

                    Log.d("SpeechRecognizer", "recognized text: " + recognizedText);
                    Intent intent = new Intent("com.example.summit.GOT_RESULT");
                    intent.putExtra("recognizedText", recognizedText);

                    LocalBroadcastManager.getInstance(RecordingService.this).sendBroadcast(intent);
                }

                if (isStopping) {
                    sendRecordingDone();
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                ArrayList<String> partialMatches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (partialMatches != null && !partialMatches.isEmpty()) {
                    String partialText = partialMatches.get(0); // Get the first partial result
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
                } else if (ACTION_STOP.equals(action)) {
                    stopRecording();
                }
            }
        }
        return START_STICKY;
    }

    private void startRecording() {
        if (isRecording) return;
        isRecording = true;

        // Set up the recognizer intent for Speech-to-Text
        Intent recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
        speechRecognizer.startListening(recognizerIntent);

    }

    private void stopRecording() {
        if (!isRecording) return;
        isRecording = false;
        isStopping = true;
        speechRecognizer.stopListening();

        stopForeground(true);
        stopSelf();


    }

    private Notification getNotification() {
        return new NotificationCompat.Builder(this, "notif id")
                .setContentTitle("Summit")
                .setContentText("Recording lecture...")
                .setSmallIcon(R.drawable.baseline_mic_24)
                .setOngoing(true)
                .build();
    }

    private void createNotificationChannel() {
        NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT > 26){
            NotificationChannel channel = new NotificationChannel("notif id", "Main Channel", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void sendRecordingDone() {
        Log.d("SpeechRecognizer", "Sending RECORDING_DONE broadcast");
        Intent intent = new Intent("com.example.summit.RECORDING_DONE");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        isStopping = false;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


}
