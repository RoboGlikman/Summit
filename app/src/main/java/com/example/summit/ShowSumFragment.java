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

public class ShowSumFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.show_sum_fragment, container, false);

        TextView summaryNameTv = root.findViewById(R.id.summaryNameShowTv);
        TextView summaryDateTv = root.findViewById(R.id.summaryDateShowTv);
        TextView summaryTextTv = root.findViewById(R.id.summaryTextShowTv);

        FloatingActionButton floatingActionButton = root.findViewById(R.id.shareSumFab);

        String summaryName = getArguments().getString("SummaryName");
        String summaryDate = getArguments().getString("SummaryDate");
        String summaryText = getArguments().getString("SummaryText");

        summaryNameTv.setText(summaryName);
        summaryDateTv.setText(summaryDate);
        summaryTextTv.setText(summaryText);

        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendEmail(summaryName, summaryText);
            }
        });
        return root;
    }

    public void sendEmail(String subject, String content){
        Intent intent = new Intent(Intent.ACTION_SEND);

        intent.setType("message/rfc822"); //* Ensures it's for email apps
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, content);

        try {
            getActivity().startActivity(Intent.createChooser(intent, getString(R.string.send_email_using)));
        } catch (ActivityNotFoundException ex){
            Toast.makeText(getActivity(), R.string.no_email_client_installed, Toast.LENGTH_SHORT).show();
        }

    }
}
