package com.example.chainreaction;

public class Player {
    private int id;
    private int color;
    private boolean isActive;
    private int atomCount;
    private String name;

    public Player(int id, int color, String name) {
        this.id = id;
        this.color = color;
        this.isActive = true;
        this.atomCount = 0;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public int getColor() {
        return color;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public int getAtomCount() {
        return atomCount;
    }

    public void setAtomCount(int atomCount) {
        this.atomCount = atomCount;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}