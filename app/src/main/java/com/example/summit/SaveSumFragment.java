package com.example.summit;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.chaquo.python.PyException;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A fragment that allows the user to save a generated summary.
 * It displays the summary text and provides an input field for the user to name the summary.
 */
public class SaveSumFragment extends Fragment {
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
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.save_sum_fragment, container, false);

        TextInputEditText summaryNameEt = root.findViewById(R.id.enterSummaryNameEt);
        TextView summaryTv = root.findViewById(R.id.summaryTextSaveTv);
        Button saveBtn = root.findViewById(R.id.saveBtn);
        Button sumBtn = root.findViewById(R.id.sumBtn);

        String text = getArguments().getString("Text");

        summaryTv.setText("Recognized text:\n" + text);

        sumBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!Python.isStarted()) {
                    Python.start(new AndroidPlatform(getActivity()));
                }

                Python py = Python.getInstance();
                PyObject mainFunction = py.getModule("main").get("main");
                String summaryText = "";
                try {
                    Toast.makeText(getActivity(), "This may take a few seconds.", Toast.LENGTH_SHORT).show();
                    summaryText = mainFunction.call(text, "eng_Latn").toString();
                    summaryTv.setText("Summary text:\n" + summaryText);

                } catch (PyException e) {
                    Toast.makeText(getActivity(), R.string.too_many_api_calls, Toast.LENGTH_SHORT).show();
                }

            }
        });
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get the summary name entered by the user
                final String summaryName = summaryNameEt.getText().toString();
                // Check if the summary name is empty
                if (summaryName.isEmpty()) {
                    Toast.makeText(getActivity(), R.string.please_enter_name, Toast.LENGTH_SHORT).show();
                } else {
                    // Execute database operation on a background thread
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            // Get the current date in the specified format
                            SimpleDateFormat ft = new SimpleDateFormat(getString(R.string.date_format));
                            String currentDate = ft.format(new Date());

                            // Create a new Item object with the summary details
                            final Item item = new Item(summaryTv.getText().toString(), summaryName, currentDate);

                            // Get the database instance and the ItemDao to add the new item
                            ItemsDB.getInstance(getActivity()).itemDAO().addItem(item);
                        }
                    }).start();

                    // Navigate back to the main fragment after saving
                    Navigation.findNavController(v).navigate(R.id.action_saveSumFragment_to_mainFragment);
                }
            }
        });

        return root;
    }
}