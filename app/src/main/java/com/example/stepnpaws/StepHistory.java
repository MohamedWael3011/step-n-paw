package com.example.stepnpaws;

public class StepHistory {
    private String date;
    private int steps;

    public StepHistory(String date, int steps) {
        this.date = date;
        this.steps = steps;
    }

    public String getDate() {
        return date;
    }

    public int getSteps() {
        return steps;
    }
}