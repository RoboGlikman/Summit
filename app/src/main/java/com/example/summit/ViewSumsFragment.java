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

/**
 * A fragment that displays a list of saved summaries. It uses a RecyclerView to show the summaries
 * and allows users to click on an item to view its details or swipe to delete it.
 */
public class ViewSumsFragment extends Fragment {
    private List<Item> items;
    private ItemAdapter adapter;

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
        View root = inflater.inflate(R.layout.view_sums_fragment, container, false);

        RecyclerView recyclerView = root.findViewById(R.id.recycler);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        // Fetch the list of saved items from the database on a background thread
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d("ViewSumsFragment", "retrieving db items...");
                items = ItemsDB.getInstance(getActivity()).itemDAO().getItems();
            }
        });
        thread.start();

        // Wait for the database retrieval thread to complete
        try {
            thread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // Initialize the ItemAdapter with the retrieved list of items
        adapter = new ItemAdapter(items);

        // Set a listener for item clicks in the RecyclerView
        adapter.setListener(new ItemAdapter.ItemsListener() {
            @Override
            public void onItemClick(int pos, View view) {
                // Get the details of the clicked item
                final String summaryName = items.get(pos).getSummaryName();
                final String summaryText = items.get(pos).getSummaryText();
                final String summaryDate = items.get(pos).getSummaryDate();

                // Create a Bundle to pass the item details to the ShowSumFragment
                Bundle bundle = new Bundle();
                bundle.putString("SummaryName", summaryName);
                bundle.putString("SummaryText", summaryText);
                bundle.putString("SummaryDate", summaryDate);

                // Navigate to the ShowSumFragment, passing the item details
                Navigation.findNavController(view).navigate(R.id.action_viewSumsFragment_to_showSumFragment, bundle);
            }
        });

        // Set the adapter for the RecyclerView
        recyclerView.setAdapter(adapter);

        // Implement swipe-to-delete functionality using ItemTouchHelper
        ItemTouchHelper helper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false; // Drag and drop is not supported
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // Show an AlertDialog to confirm the deletion
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                Item item = adapter.getItemAtPosition(viewHolder.getAdapterPosition());

                builder.setTitle(R.string.delete).setMessage(R.string.ruSure).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Delete the item from the database on a background thread
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                ItemsDB.getInstance(getActivity()).itemDAO().deleteItem(item);
                            }
                        }).start();
                        // Remove the item from the adapter and update the RecyclerView
                        adapter.deleteItem(viewHolder.getAdapterPosition());
                    }
                }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // If the user cancels, re-bind the adapter to prevent the item from being removed visually
                        recyclerView.setAdapter(adapter);
                    }
                }).setCancelable(false).show();
            }
        });

        // Attach the ItemTouchHelper to the RecyclerView
        helper.attachToRecyclerView(recyclerView);

        return root;
    }
}