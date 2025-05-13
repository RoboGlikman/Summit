package com.example.summit;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.navigation.Navigation;

import com.chaquo.python.PyException;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Objects;
import java.util.Queue;

/**
 * The main fragment of the Summit application, providing functionalities for recording audio
 * and viewing saved summaries.
 */
public class MainFragment extends Fragment {
    private boolean isRecording = false;
    private int seconds, minutes, milliseconds, hours;
    private long millisecond, startTime, timeBuff, updateTime = 0L;
    private Handler handler;
    private TextView timeSpanTv;
    private static final String PERMISSION_RECORD_AUDIO = Manifest.permission.RECORD_AUDIO;
    private static final String PERMISSION_POST_NOTIFICATIONS = Manifest.permission.POST_NOTIFICATIONS;
    private String recognizedText = "";

    /**
     * A runnable that updates the stopwatch UI with the elapsed time.
     */
    private final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            millisecond = SystemClock.uptimeMillis() - startTime;
            updateTime = timeBuff + millisecond;
            seconds = (int) (updateTime / 1000);
            minutes = seconds / 60;
            seconds = seconds % 60;
            hours = minutes / 60;
            milliseconds = (int) (updateTime % 1000);

            timeSpanTv.setText(MessageFormat.format("{0}:{1}:{2}", hours, minutes, String.format(Locale.getDefault(), "%02d", seconds)));
            handler.postDelayed(this, 0);
        }
    };

    /**
     * An ActivityResultLauncher for requesting runtime permissions.
     */
    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean result) {
                    // Handle permission result if needed
                }
            }
    );

    /**
     * Called to have the fragment instantiate its user interface view.
     * This is optional, and non-null if the fragment does not provide a UI.
     *
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment,
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to. The fragment should not add the view itself
     * but this can be used to generate LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     * @return The View for the fragment's UI, or null.
     */
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.main_fragment, container, false);

        Button recordBtn = root.findViewById(R.id.record_btn);
        Button viewAllSumsBtn = root.findViewById(R.id.view_all_sums_btn);

        handler = new Handler(Looper.getMainLooper());
        timeSpanTv = root.findViewById(R.id.timespan_tv);

        IntentFilter filter1 = new IntentFilter("com.example.summit.RECORDING_DONE");
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(doneReceiver, filter1);

        IntentFilter filter2 = new IntentFilter("com.example.summit.GOT_RESULT");
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(resultReceiver, filter2);

        recordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isRecording) {
                    timeBuff += millisecond;
                    handler.removeCallbacks(runnable);
                    resetStopwatch();

                    isRecording = false;

                    Intent serviceIntent = new Intent(getActivity(), RecordingService.class);
                    serviceIntent.putExtra("action", "STOP_RECORDING");
                    requireActivity().startForegroundService(serviceIntent);

                    LocalBroadcastManager.getInstance(requireContext()).registerReceiver(resultReceiver, new IntentFilter("com.example.summit.GOT_RESULT"));

                } else {
                    requestPermissionLauncher.launch(PERMISSION_RECORD_AUDIO);
                    if (Build.VERSION.SDK_INT >= 33)
                        requestPermissionLauncher.launch(PERMISSION_POST_NOTIFICATIONS);

                    if (ContextCompat.checkSelfPermission(getActivity(), PERMISSION_RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                            && ContextCompat.checkSelfPermission(getActivity(), PERMISSION_POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {

                        recordBtn.setText(R.string.recording);

                        startTime = SystemClock.uptimeMillis();
                        handler.postDelayed(runnable, 0);
                        isRecording = true;

                        // Unregister resultReceiver before starting a new recording
                        try {
                            LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(resultReceiver);
                        } catch (IllegalArgumentException e) {
                            // Receiver might not be registered yet, ignore
                        }

                        Intent serviceIntent = new Intent(getActivity(), RecordingService.class);
                        serviceIntent.putExtra("action", "START_RECORDING");
                        requireActivity().startForegroundService(serviceIntent);

                        Log.d("MainFragment", "started foreground service");
                    }
                }
            }
        });

        viewAllSumsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(v).navigate(R.id.action_mainFragment_to_viewSumsFragment);
            }
        });

        return root;
    }

    /**
     * A BroadcastReceiver that listens for the "com.example.summit.GOT_RESULT" broadcast,
     * which contains the transcribed text from the ongoing recording.
     */
    private final BroadcastReceiver resultReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.example.summit.GOT_RESULT".equals(intent.getAction())) {
                String tempRecognizedText = intent.getStringExtra("recognizedText");
                recognizedText += " " + tempRecognizedText;
            }
        }
    };

    /**
     * A BroadcastReceiver that listens for the "com.example.summit.RECORDING_DONE" broadcast,
     * triggered when the recording service finishes processing the audio. It then navigates to the save summary fragment.
     */
    private final BroadcastReceiver doneReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.example.summit.RECORDING_DONE".equals(intent.getAction())) {
                Log.d("MainFragment", "recognizedText, done receiver: " + recognizedText);
                if (recognizedText != null) {

                    Bundle bundle = new Bundle();
                    bundle.putString("Text", recognizedText);
                    recognizedText = "";

                    try {
                        Navigation.findNavController(requireActivity(), R.id.nav_host_fragment).navigate(R.id.action_mainFragment_to_saveSumFragment, bundle);
                    } catch (IllegalArgumentException e) {
                        Log.e("MainFragment", "Navigation Failed: " + e.getMessage());
                    }
                }
            }
        }
    };

    /**
     * Resets the stopwatch timer to its initial state (00:00:00).
     */
    private void resetStopwatch() {
        millisecond = 0L;
        startTime = 0L;
        timeBuff = 0L;
        updateTime = 0L;
        hours = 0;
        minutes = 0;
        seconds = 0;
        milliseconds = 0;
        timeSpanTv.setText("00:00:00");
    }

    /**
     * Called when the fragment is paused. Unregisters the BroadcastReceivers to prevent
     * leaks and unnecessary operations when the fragment is not in the foreground.
     */
    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(doneReceiver);
        try {
            LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(resultReceiver);
        } catch (IllegalArgumentException e) {
            // Receiver might not have been registered
        }
    }

    /**
     * Called when the fragment is no longer in use. This is called after {@link #onStop()}
     * and before {@link #onDetach()}. Unregisters all BroadcastReceivers.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(doneReceiver);
        try {
            LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(resultReceiver);
        } catch (IllegalArgumentException e) {
            // Receiver might not have been registered
        }
    }

}