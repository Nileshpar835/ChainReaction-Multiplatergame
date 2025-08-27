package com.example.chainreaction;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

public class Atom {
    private float x, y;
    private float radius;
    private float rotation;
    private int color;
    private Paint paint;
    private Path atomPath;

    public Atom(float x, float y, float radius, int color) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.color = color;
        this.rotation = 0;

        paint = new Paint();
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);

        createAtomPath();
    }

    private void createAtomPath() {
        atomPath = new Path();
        float innerRadius = radius * 0.6f;

        // Create electron orbits
        for (int i = 0; i < 3; i++) {
            float angle = (float) (i * Math.PI * 2 / 3);
            float orbitX = x + (float) Math.cos(angle) * innerRadius;
            float orbitY = y + (float) Math.sin(angle) * innerRadius;

            // Draw electron
            atomPath.addCircle(orbitX, orbitY, radius * 0.2f, Path.Direction.CW);
        }

        // Add nucleus
        atomPath.addCircle(x, y, radius * 0.4f, Path.Direction.CW);
    }

    public void update() {
        rotation += 5; // Rotation speed
        if (rotation >= 360) {
            rotation = 0;
        }
    }

    public void draw(Canvas canvas) {
        canvas.save();
        canvas.rotate(rotation, x, y);
        canvas.drawPath(atomPath, paint);
        canvas.restore();
    }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
        createAtomPath();
    }

    public void setColor(int color) {
        this.color = color;
        paint.setColor(color);
    }
}