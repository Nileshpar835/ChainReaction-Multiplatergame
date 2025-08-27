package com.example.chainreaction;

public class Cell {
    private int atomCount;
    private int ownerPlayerId;
    private int maxCapacity;
    private int row;
    private int col;
    private int clickCount;

    public Cell(int row, int col, int maxCapacity) {
        this.row = row;
        this.col = col;
        this.atomCount = 0;
        this.ownerPlayerId = -1;
        this.maxCapacity = maxCapacity;
        this.clickCount = 0;
    }

    public int getAtomCount() {
        return atomCount;
    }

    public void setAtomCount(int atomCount) {
        this.atomCount = atomCount;
    }

    public int getOwnerPlayerId() {
        return ownerPlayerId;
    }

    public void setOwnerPlayerId(int ownerPlayerId) {
        this.ownerPlayerId = ownerPlayerId;
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public boolean isFull() {
        return atomCount >= maxCapacity;
    }

    public void addAtom(int playerId) {
        if (atomCount == 0) {
            ownerPlayerId = playerId;
        }
        atomCount++;
    }

    public void reset() {
        atomCount = 0;
        ownerPlayerId = -1;
        clickCount = 0;
    }

    public int getClickCount() {
        return clickCount;
    }

    public void incrementClickCount() {
        clickCount++;
    }

    public void resetClickCount() {
        clickCount = 0;
    }
}