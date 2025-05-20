package com.example.stepnpaws;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private List<StepHistory> historyList;

    public HistoryAdapter(List<StepHistory> historyList) {
        this.historyList = historyList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_step_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StepHistory item = historyList.get(position);

        // Format date from YYYY-MM-DD to MM/DD
        String formattedDate = formatDate(item.getDate());
        holder.dateText.setText(formattedDate);
        holder.stepsText.setText(String.valueOf(item.getSteps()));

        // Set progress based on steps
        int progress = Math.min(item.getSteps(), 10000);
        holder.progressBar.setProgress(progress);
    }

    private String formatDate(String dbDate) {
        try {
            SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = dbFormat.parse(dbDate);

            SimpleDateFormat displayFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());
            return displayFormat.format(date);
        } catch (Exception e) {
            return dbDate;
        }
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView dateText, stepsText;
        ProgressBar progressBar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            dateText = itemView.findViewById(R.id.dateText);
            stepsText = itemView.findViewById(R.id.stepsText);
            progressBar = itemView.findViewById(R.id.progressBar);
        }
    }
}