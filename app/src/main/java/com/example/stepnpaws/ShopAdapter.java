package com.example.stepnpaws;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.function.Consumer;

public class ShopAdapter extends RecyclerView.Adapter<ShopAdapter.ViewHolder> {

    private List<ShopItem> items;
    private Consumer<ShopItem> onItemClick;

    private DatabaseHelper dbHelper;

    public ShopAdapter(List<ShopItem> items, Consumer<ShopItem> onItemClick, DatabaseHelper dbHelper) {
        this.items = items;
        this.onItemClick = onItemClick;
        this.dbHelper = dbHelper;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_shop, parent, false);
        return new ViewHolder(view);

    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ShopItem item = items.get(position);
        holder.nameText.setText(item.getName());
        holder.descriptionText.setText(item.getDescription());
        holder.priceText.setText(String.valueOf(item.getPrice()));
        holder.itemImage.setImageResource(item.getImageRes());

        // Check ownership status using dbHelper
        boolean isOwned = item.getType().equals("pet")
                ? dbHelper.hasPet(item.getName())
                : dbHelper.hasBackground(item.getName());

        if (isOwned) {
            holder.buyButton.setText("OWNED");
            holder.buyButton.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
            holder.buyButton.setEnabled(false);
        } else {
            holder.buyButton.setText("BUY");
            holder.buyButton.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.colorPrimary)));
            holder.buyButton.setEnabled(true);
            holder.buyButton.setOnClickListener(v -> onItemClick.accept(item));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }




    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView itemImage;
        TextView nameText, descriptionText, priceText;
        Button buyButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            itemImage = itemView.findViewById(R.id.itemImage);
            nameText = itemView.findViewById(R.id.itemName);
            descriptionText = itemView.findViewById(R.id.itemDescription);
            priceText = itemView.findViewById(R.id.itemPrice);
            buyButton = itemView.findViewById(R.id.buyButton);
        }
    }
}