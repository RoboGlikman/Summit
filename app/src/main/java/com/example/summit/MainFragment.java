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

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.Objects;

public class MainFragment extends Fragment {
    boolean isRecording = false;
    int seconds, minutes, milliseconds, hours;
    long millisecond, startTime, timeBuff, updateTime = 0L;
    private Handler handler;
    private TextView timeSpanTv;
    private static final String PERMISSION_RECORD_AUDIO = Manifest.permission.RECORD_AUDIO;
    private static final String PERMISSION_POST_NOTIFICATIONS = Manifest.permission.POST_NOTIFICATIONS;
    private ImageView recordImage;
    private Button viewAllSumsBtn;
    private String recognizedText = "";
    private View root; //! CHANGED, check debug
    private final FragmentActivity CURRENT_ACTIVITY = getActivity();
    private final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            millisecond = SystemClock.uptimeMillis()  - startTime;
            updateTime = timeBuff + millisecond;
            seconds = (int)(updateTime / 1000);
            minutes = seconds / 60;
            seconds = seconds % 60;
            hours = minutes / 60;
            milliseconds = (int) (updateTime % 1000);

            timeSpanTv.setText(MessageFormat.format("{0}:{1}:{2}", hours, minutes, String.format(Locale.getDefault(), "%02d", seconds)));
            handler.postDelayed(this, 0);
        }
    };

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean result) {
                }
            }
    );


    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.main_fragment, container, false); //! CHANGED, check debug

        recordImage = root.findViewById(R.id.recordImg);
        viewAllSumsBtn = root.findViewById(R.id.view_all_sums_btn);

        handler = new Handler(Looper.getMainLooper());
        timeSpanTv = root.findViewById(R.id.time_span_tv);

        IntentFilter filter1 = new IntentFilter("com.example.summit.RECORDING_DONE");
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(doneReceiver, filter1);

        IntentFilter filter2 = new IntentFilter("com.example.summit.GOT_RESULT");
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(resultReceiver, filter2);

        recordImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    if (isRecording) {
                        recordImage.setImageResource(R.drawable.record_img);

                        timeBuff += millisecond;
                        handler.removeCallbacks(runnable);
                        resetStopwatch();

                        isRecording = false;

                        Intent serviceIntent = new Intent(CURRENT_ACTIVITY, RecordingService.class);
                        serviceIntent.putExtra("action", "STOP_RECORDING");
                        requireActivity().startForegroundService(serviceIntent);

                    } else {
                        requestPermissionLauncher.launch(PERMISSION_RECORD_AUDIO);
                        if (Build.VERSION.SDK_INT >= 33)
                            requestPermissionLauncher.launch(PERMISSION_POST_NOTIFICATIONS);

                        if (ContextCompat.checkSelfPermission(CURRENT_ACTIVITY, PERMISSION_RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                        && ContextCompat.checkSelfPermission(CURRENT_ACTIVITY, PERMISSION_POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED){

                            recordImage.setImageResource(R.drawable.stop_record_btn);

                            startTime = SystemClock.uptimeMillis();
                            handler.postDelayed(runnable, 0);
                            isRecording = true;

                            Intent serviceIntent = new Intent(CURRENT_ACTIVITY, RecordingService.class);
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

    private final BroadcastReceiver resultReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.example.summit.GOT_RESULT".equals(intent.getAction())) {
                String tempRecognizedText = intent.getStringExtra("recognizedText");
                Log.d("MainFragment", "recognized text: " + tempRecognizedText);
                recognizedText += " " + tempRecognizedText;
            }
        }
    };

    private final BroadcastReceiver doneReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.example.summit.RECORDING_DONE".equals(intent.getAction())) {
                Log.d("MainFragment", "recognizedText, done receiver: " + recognizedText);
                if (recognizedText != null){
                    /*
                    if (!Python.isStarted()){
                    Python.start(new AndroidPlatform(CURRENT_ACTIVITY));
                    }

                    Python py = Python.getInstance();
                    PyObject mainFunction = py.getModule("main").get("main");
                    String summaryText = mainFunction.call(recognizedText, "eng_Latn").toString();
                    */
                    Bundle bundle = new Bundle();
                    bundle.putString("SummaryText", recognizedText); //! CHANGE recognized text to summaryText
                    Navigation.findNavController(root).navigate(R.id.action_mainFragment_to_saveSumFragment, bundle);
                }

            }
        }
    };

    private void resetStopwatch(){
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


    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(doneReceiver);
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(resultReceiver);
    }
}
