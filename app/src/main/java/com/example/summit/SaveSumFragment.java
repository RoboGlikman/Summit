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

import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Date;

public class SaveSumFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.save_sum_fragment, container, false);

        TextInputEditText summaryNameEt = root.findViewById(R.id.enterSummaryNameEt);
        TextView summaryTv = root.findViewById(R.id.summaryTextSaveTv);
        Button saveBtn = root.findViewById(R.id.saveBtn);

        String summaryText = getArguments().getString("SummaryText");
        summaryTv.setText(summaryText);

        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String summaryName = summaryNameEt.getText().toString();
                if (summaryName.isEmpty()){
                    Toast.makeText(getActivity(), R.string.please_enter_name, Toast.LENGTH_SHORT).show();
                } else {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            SimpleDateFormat ft = new SimpleDateFormat(getString(R.string.date_format));
                            String currentDate = ft.format(new Date());

                            final Item item = new Item(summaryText, summaryName, currentDate);

                            ItemsDB.getInstance(getActivity()).itemDAO().addItem(item);
                        }
                    }).start();

                    Navigation.findNavController(v).navigate(R.id.action_saveSumFragment_to_mainFragment);
                }
            }
        });

        return root;
    }
}
