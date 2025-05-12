package com.example.summit;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

/**
 * A fragment that displays the details of a saved summary, including its name, date, and text content.
 * It also provides an option to share the summary via email.
 */
public class ShowSumFragment extends Fragment {
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
        View root = inflater.inflate(R.layout.show_sum_fragment, container, false);

        TextView summaryNameTv = root.findViewById(R.id.summaryNameShowTv);
        TextView summaryDateTv = root.findViewById(R.id.summaryDateShowTv);
        TextView summaryTextTv = root.findViewById(R.id.summaryTextShowTv);

        FloatingActionButton floatingActionButton = root.findViewById(R.id.shareSumFab);

        // Retrieve the summary details passed as arguments
        String summaryName = getArguments().getString("SummaryName");
        String summaryDate = getArguments().getString("SummaryDate");
        String summaryText = getArguments().getString("SummaryText");

        // Display the summary details in the respective TextViews
        summaryNameTv.setText(summaryName);
        summaryDateTv.setText(summaryDate);
        summaryTextTv.setText(summaryText);

        // Set an OnClickListener for the FloatingActionButton to handle sharing the summary
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Call the method to send the summary via email
                sendEmail(summaryName, summaryText);
            }
        });
        return root;
    }

    /**
     * Sends an email containing the summary subject and content.
     * It uses an Intent to open an email client, allowing the user to send the information.
     *
     * @param subject The subject of the email (which is the summary name).
     * @param content The body of the email (which is the summary text).
     */
    public void sendEmail(String subject, String content) {
        Intent intent = new Intent(Intent.ACTION_SEND);

        intent.setType("message/rfc822"); //* Ensures it's for email apps
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, content);

        try {
            // Start an activity to choose an email client
            getActivity().startActivity(Intent.createChooser(intent, getString(R.string.send_email_using)));
        } catch (ActivityNotFoundException ex) {
            // Display a Toast message if no email client is installed
            Toast.makeText(getActivity(), R.string.no_email_client_installed, Toast.LENGTH_SHORT).show();
        }

    }
}