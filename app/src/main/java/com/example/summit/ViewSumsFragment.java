package com.example.summit;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ViewSumsFragment extends Fragment {
    private List<Item> items;
    private ItemAdapter adapter;
    private final FragmentActivity CURRENT_ACTIVITY = getActivity();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.view_sums_fragment, container, false);

        RecyclerView recyclerView = root.findViewById(R.id.recycler);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(CURRENT_ACTIVITY));

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d("ViewSumsFragment", "retrieving db items...");
                items = ItemsDB.getInstance(CURRENT_ACTIVITY).itemDAO().getItems();
            }
        });
        thread.start();

        try {
            thread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        adapter = new ItemAdapter(items);

        adapter.setListener(new ItemAdapter.ItemsListener() {
            @Override
            public void onItemClick(int pos, View view) {
                final String summaryName = items.get(pos).getSummaryName();
                final String summaryText = items.get(pos).getSummaryText();
                final String summaryDate = items.get(pos).getSummaryDate();

                Bundle bundle = new Bundle();
                bundle.putString("SummaryName", summaryName);
                bundle.putString("SummaryText", summaryText);
                bundle.putString("SummaryDate", summaryDate);

                Navigation.findNavController(view).navigate(R.id.action_viewSumsFragment_to_showSumFragment, bundle);
            }
        });

        recyclerView.setAdapter(adapter);
        ItemTouchHelper helper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                AlertDialog.Builder builder = new AlertDialog.Builder(CURRENT_ACTIVITY);
                Item item = adapter.getItemAtPosition(viewHolder.getAdapterPosition());

                builder.setTitle(R.string.delete).setMessage(R.string.ruSure).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                ItemsDB.getInstance(CURRENT_ACTIVITY).itemDAO().deleteItem(item);
                            }
                        }).start();
                        adapter.deleteItem(viewHolder.getAdapterPosition());
                    }
                }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        recyclerView.setAdapter(adapter);
                    }
                }).setCancelable(false).show();
            }
        });

        helper.attachToRecyclerView(recyclerView);

        return root;
    }
}
