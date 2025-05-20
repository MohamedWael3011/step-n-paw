package com.example.stepnpaws;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.function.Consumer;

public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.ViewHolder> {

    private List<InventoryItem> items;
    private Consumer<InventoryItem> onItemClick;

    public InventoryAdapter(List<InventoryItem> items, Consumer<InventoryItem> onItemClick) {
        this.items = items;
        this.onItemClick = onItemClick;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_inventory, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        InventoryItem item = items.get(position);
        holder.nameText.setText(item.getName());
        holder.typeText.setText(item.getType());
        holder.itemImage.setImageResource(item.getImageRes());

        holder.itemView.setOnClickListener(v -> onItemClick.accept(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView itemImage;
        TextView nameText, typeText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            itemImage = itemView.findViewById(R.id.itemImage);
            nameText = itemView.findViewById(R.id.itemName);
            typeText = itemView.findViewById(R.id.itemType);
        }
    }
}