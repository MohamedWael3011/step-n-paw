package com.example.stepnpaws;

import android.database.Cursor;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ShopActivity extends AppCompatActivity {

    private RecyclerView shopRecyclerView;
    private ShopAdapter shopAdapter;
    private DatabaseHelper dbHelper;
    private TextView stepBalanceText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop);

        dbHelper = new DatabaseHelper(this);
        stepBalanceText = findViewById(R.id.stepBalanceText);
        shopRecyclerView = findViewById(R.id.shopRecyclerView);

        setupShopItems();
        updateStepBalance();
    }

    private void setupShopItems() {
        List<ShopItem> shopItems = new ArrayList<>();

        // Add pets
        shopItems.add(new ShopItem("Dog Pet", "A loyal canine companion", 5000, R.drawable.pet_dog, "pet"));
        shopItems.add(new ShopItem("Bird Pet", "A cheerful flying friend", 4000, R.drawable.pet_bird, "pet"));

        // Add backgrounds
        shopItems.add(new ShopItem("Green", "Green scenery", 3000, R.drawable.bg_green, "background"));
        shopItems.add(new ShopItem("Pink", "Pink view", 3500, R.drawable.bg_pink, "background"));

        shopAdapter = new ShopAdapter(shopItems, this::onItemPurchased, dbHelper);
        shopRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        shopRecyclerView.setAdapter(shopAdapter);
    }

    private void onItemPurchased(ShopItem item) {
        int userSteps = getCurrentStepBalance();

        // Check if already owned
        if ((item.getType().equals("pet") && dbHelper.hasPet(item.getName())) ){
            Toast.makeText(this, "You already own this pet!", Toast.LENGTH_SHORT).show();
            return;
        }

        if ((item.getType().equals("background") && dbHelper.hasBackground(item.getName()))) {
            Toast.makeText(this, "You already own this background!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (userSteps >= item.getPrice()) {

            // Add to inventory
            if (item.getType().equals("pet")) {
                dbHelper.addPet(item.getName(), item.getImageRes());
                Toast.makeText(this, "New pet unlocked: " + item.getName(), Toast.LENGTH_SHORT).show();
            } else {
                dbHelper.addBackground(item.getName(), item.getImageRes());
                dbHelper.setCurrentBackground(item.getName());
                Toast.makeText(this, "New background equipped: " + item.getName(), Toast.LENGTH_SHORT).show();
            }

            // Refresh the adapter
            shopAdapter.notifyDataSetChanged();
        } else {
            Toast.makeText(this, "You need " + (item.getPrice() - userSteps) + " more steps!", Toast.LENGTH_SHORT).show();
        }
    }

    private int getCurrentStepBalance() {
        return dbHelper.getAccumulatedSteps();
    }

    private void updateStepBalance() {
        int steps = getCurrentStepBalance();
        stepBalanceText.setText("Total Steps: " + steps);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStepBalance();
    }
}