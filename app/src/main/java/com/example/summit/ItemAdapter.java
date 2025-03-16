package com.example.summit;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ItemViewHolder> {
    public final List<Item> items;

    public ItemAdapter(List<Item> items) {
        this.items = items;
    }

    interface ItemsListener {
        void onItemClick(int pos, View view);
    }
    ItemsListener listener;

    public void setListener(ItemsListener listener) {
        this.listener = listener;
    }

    public class ItemViewHolder extends RecyclerView.ViewHolder{
        TextView summaryNameTv;
        TextView summaryDateTv;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            this.summaryNameTv = itemView.findViewById(R.id.summaryItemNameTv);
            this.summaryDateTv = itemView.findViewById(R.id.summaryItemDateTv);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null){
                        listener.onItemClick(getAdapterPosition(), v);
                    }
                }
            });
        }
    }

    public Item getItemAtPosition(int pos){return items.get(pos);}

    public void deleteItem(int pos){
        this.items.remove(pos);
        notifyItemRemoved(pos);
    }


    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_layout, parent, false);
        return new ItemViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        Item item = items.get(position);
        holder.summaryNameTv.setText(item.getSummaryName());
        holder.summaryDateTv.setText(item.getSummaryDate());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}
