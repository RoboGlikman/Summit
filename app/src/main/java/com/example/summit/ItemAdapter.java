package com.example.summit;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * An adapter for displaying a list of {@link Item} objects in a {@link RecyclerView}.
 */
public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ItemViewHolder> {
    /**
     * The list of items to be displayed by the adapter.
     */
    public final List<Item> items;

    /**
     * Constructs a new ItemAdapter with the provided list of items.
     *
     * @param items The list of {@link Item} objects to display.
     */
    public ItemAdapter(List<Item> items) {
        this.items = items;
    }

    /**
     * Interface definition for a callback to be invoked when an item in the RecyclerView is clicked.
     */
    interface ItemsListener {
        /**
         * Called when an item in the RecyclerView is clicked.
         *
         * @param pos  The adapter position of the clicked item.
         * @param view The itemView that was clicked.
         */
        void onItemClick(int pos, View view);
    }

    /**
     * Listener to handle item click events.
     */
    ItemsListener listener;

    /**
     * Sets the listener to be notified when an item is clicked.
     *
     * @param listener The {@link ItemsListener} to set.
     */
    public void setListener(ItemsListener listener) {
        this.listener = listener;
    }

    /**
     * A ViewHolder that provides references to the views for each {@link Item} in the list.
     */
    public class ItemViewHolder extends RecyclerView.ViewHolder {
        /**
         * TextView to display the name of the summary.
         */
        TextView summaryNameTv;
        /**
         * TextView to display the date of the summary.
         */
        TextView summaryDateTv;

        /**
         * Constructs a new ItemViewHolder.
         *
         * @param itemView The view representing a single item in the list.
         */
        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            this.summaryNameTv = itemView.findViewById(R.id.summaryItemNameTv);
            this.summaryDateTv = itemView.findViewById(R.id.summaryItemDateTv);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onItemClick(getAdapterPosition(), v);
                    }
                }
            });
        }
    }

    /**
     * Returns the {@link Item} at the specified adapter position.
     *
     * @param pos The adapter position of the item to retrieve.
     * @return The {@link Item} at the given position.
     */
    public Item getItemAtPosition(int pos) {
        return items.get(pos);
    }

    /**
     * Removes the item at the specified adapter position from the list and notifies the adapter of the removal.
     *
     * @param pos The adapter position of the item to delete.
     */
    public void deleteItem(int pos) {
        this.items.remove(pos);
        notifyItemRemoved(pos);
    }


    /**
     * Called when RecyclerView needs a new {@link ItemViewHolder} of the given type to represent an item.
     *
     * @param parent   The ViewGroup into which the new View will be added after it is bound to an adapter position.
     * @param viewType The view type of the new View.
     * @return A new {@link ItemViewHolder} that holds a View of the given view type.
     * @see #getItemViewType(int)
     * @see #onBindViewHolder(ItemViewHolder, int)
     */
    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_layout, parent, false);
        return new ItemViewHolder(v);
    }

    /**
     * Called by RecyclerView to display the data at the specified position. A new {@link ItemViewHolder} is created
     * or an existing one is re-used for this view.
     *
     * @param holder   The ViewHolder which should be updated to represent the contents of the item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        Item item = items.get(position);
        holder.summaryNameTv.setText(item.getSummaryName());
        holder.summaryDateTv.setText(item.getSummaryDate());
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of items in this adapter.
     */
    @Override
    public int getItemCount() {
        return items.size();
    }
}