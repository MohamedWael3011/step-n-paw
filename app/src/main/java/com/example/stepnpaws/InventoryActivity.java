package com.example.stepnpaws;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class InventoryActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private RecyclerView petsRecyclerView;
    private RecyclerView backgroundsRecyclerView;
    private TextView currentPetText;
    private TextView currentBgText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);

        dbHelper = new DatabaseHelper(this);
        currentPetText = findViewById(R.id.currentPetText);
        currentBgText = findViewById(R.id.currentBgText);

        setupPetsInventory();
        setupBackgroundsInventory();
        updateCurrentItems();
    }

    private void setupPetsInventory() {
        petsRecyclerView = findViewById(R.id.petsRecyclerView);
        Cursor cursor = dbHelper.getAllPets();

        List<InventoryItem> pets = new ArrayList<>(); // Initialize pets list
        while (cursor.moveToNext()) {
            String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
            int imageRes = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PET_IMAGE));
            pets.add(new InventoryItem(name, "Pet", imageRes));
        }
        cursor.close();

        InventoryAdapter petsAdapter = new InventoryAdapter(pets, item -> {
            dbHelper.insertOrUpdateUser(getCurrentSteps(), item.getName());
            updateCurrentItems();
            setResult(RESULT_OK);
            Toast.makeText(this, "Now using: " + item.getName(), Toast.LENGTH_SHORT).show();
        });

        petsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        petsRecyclerView.setAdapter(petsAdapter);
    }

    private void setupBackgroundsInventory() {
        backgroundsRecyclerView = findViewById(R.id.backgroundsRecyclerView);

        List<InventoryItem> backgrounds = new ArrayList<>();

        // Add default background as first item
        backgrounds.add(new InventoryItem("default_bg", "Default Background", R.drawable.default_bg));

        // Add purchased backgrounds
        Cursor cursor = dbHelper.getAllBackgrounds();
        while (cursor.moveToNext()) {
            String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
            int imageRes = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_BG_IMAGE));
            backgrounds.add(new InventoryItem(name, "Background", imageRes));
        }
        cursor.close();

        InventoryAdapter bgAdapter = new InventoryAdapter(backgrounds, item -> {
            dbHelper.setCurrentBackground(item.getName());
            updateCurrentItems();
            Toast.makeText(this, "Background set to: " + item.getName(), Toast.LENGTH_SHORT).show();
        });


        backgroundsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        backgroundsRecyclerView.setAdapter(bgAdapter);
    }

    private void updateCurrentItems() {
        String currentPet = dbHelper.getCurrentPetName();
        currentPetText.setText("Current Pet: " + (currentPet != null ? currentPet : "None"));

        String currentBg = dbHelper.getCurrentBackground();
        currentBgText.setText("Current Background: " + (currentBg != null ? currentBg : "None"));
    }

    private int getCurrentSteps() {
        Cursor cursor = dbHelper.getUser();
        int steps = 0;
        if (cursor.moveToFirst()) {
            steps = cursor.getInt(cursor.getColumnIndexOrThrow("steps"));
        }
        cursor.close();
        return steps;
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateCurrentItems();
    }
}