package com.example.stepnpaws;

import android.database.Cursor;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StepHistoryActivity extends AppCompatActivity {

    private RecyclerView historyRecyclerView;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_step_history);

        dbHelper = new DatabaseHelper(this);
        historyRecyclerView = findViewById(R.id.historyRecyclerView);

        loadStepHistory();
    }

    public void loadStepHistory() {
        Cursor cursor = dbHelper.getAllStepHistory();
        List<StepHistory> historyList = new ArrayList<>();

        while (cursor.moveToNext()) {
            String date = cursor.getString(cursor.getColumnIndexOrThrow("date"));
            int steps = cursor.getInt(cursor.getColumnIndexOrThrow("steps"));
            historyList.add(new StepHistory(date, steps));
        }
        cursor.close();

        // Sort by date (newest first)
        Collections.sort(historyList, (o1, o2) -> o2.getDate().compareTo(o1.getDate()));

        HistoryAdapter adapter = new HistoryAdapter(historyList);
        historyRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        historyRecyclerView.setAdapter(adapter);
    }
}