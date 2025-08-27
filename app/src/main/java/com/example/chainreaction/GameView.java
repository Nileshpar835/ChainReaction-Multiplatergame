package com.example.chainreaction;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.os.Handler;
import android.os.Looper;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class GameView extends View {
    private GameLogic gameLogic;
    private Paint cellPaint;
    private Paint textPaint;
    private float cellWidth;
    private float cellHeight;
    private Map<String, List<Atom>> atoms;
    private Handler handler;
    private boolean isAnimating;
    private List<ExplosionAnimation> explosionAnimations;
    private Map<String, Float> atomRotationAngles = new HashMap<>(); // For rotation animation
    private float rotationSpeed = 3.0f; // Increased rotation speed for more visible motion

    private static class Atom {
        float x, y;
        float radius;
        int color;
        float targetX, targetY;
        float targetRadius;
        boolean isAnimating;

        Atom(float x, float y, float radius, int color) {
            this.x = x;
            this.y = y;
            this.radius = 0;
            this.color = color;
            this.targetX = x;
            this.targetY = y;
            this.targetRadius = radius;
            this.isAnimating = true;
        }

        void update() {
            if (isAnimating) {
                radius += (targetRadius - radius) * 0.2f;
                if (Math.abs(radius - targetRadius) < 0.1f) {
                    radius = targetRadius;
                    isAnimating = false;
                }
            }
        }

        void draw(Canvas canvas, Paint paint) {
            paint.setColor(color);
            canvas.drawCircle(x, y, radius, paint);
        }
    }

    private static class ExplosionAnimation {
        float x, y;
        float radius;
        float maxRadius;
        int color;
        boolean isActive;

        ExplosionAnimation(float x, float y, float maxRadius, int color) {
            this.x = x;
            this.y = y;
            this.radius = 0;
            this.maxRadius = maxRadius;
            this.color = color;
            this.isActive = true;
        }

        void update() {
            radius += maxRadius / 5;
            if (radius >= maxRadius) {
                isActive = false;
            }
        }

        void draw(Canvas canvas, Paint paint) {
            if (isActive) {
                paint.setColor(color);
                paint.setAlpha((int)(255 * (1 - radius/maxRadius)));
                canvas.drawCircle(x, y, radius, paint);
            }
        }
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        cellPaint = new Paint();
        cellPaint.setStyle(Paint.Style.STROKE);
        cellPaint.setColor(Color.BLACK);
        cellPaint.setStrokeWidth(2f);

        textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(40f);

        atoms = new HashMap<>();
        explosionAnimations = new ArrayList<>();
        handler = new Handler(Looper.getMainLooper());
        isAnimating = false;
    }

    public void setGameLogic(GameLogic gameLogic) {
        this.gameLogic = gameLogic;
        // Reset all animations and state
        atoms.clear();
        explosionAnimations.clear();
        atomRotationAngles.clear();
        stopAnimation();
        if (gameLogic != null) {
            updateAtoms();
        }
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (gameLogic != null) {
            cellWidth = (float) w / gameLogic.getBoard()[0].length;
            cellHeight = (float) h / gameLogic.getBoard().length;
            textPaint.setTextSize(Math.min(cellWidth, cellHeight) * 0.4f);
            updateAtoms();
        }
    }

    public void updateAtoms() {
        atoms.clear();
        Cell[][] board = gameLogic.getBoard();
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                Cell cell = board[i][j];
                if (cell.getAtomCount() > 0) {
                    String key = i + "," + j;
                    List<Atom> cellAtoms = new ArrayList<>();
                    float centerX = j * cellWidth + cellWidth / 2;
                    float centerY = i * cellHeight + cellHeight / 2;
                    float atomRadius = Math.min(cellWidth, cellHeight) * 0.15f;
                    int color = cell.getOwnerPlayerId() >= 0 ?
                            gameLogic.getPlayers().get(cell.getOwnerPlayerId()).getColor() :
                            Color.GRAY;

                    int atomCount = cell.getAtomCount();
                    // Initialize rotation angle for this cell if not exists
                    if (!atomRotationAngles.containsKey(key)) {
                        atomRotationAngles.put(key, 0f);
                    }
                    
                    // Initial positions (will be updated for rotation in animation loop)
                    for (int k = 0; k < atomCount; k++) {
                        cellAtoms.add(new Atom(centerX, centerY, atomRadius, color));
                    }
                    atoms.put(key, cellAtoms);
                }
            }
        }
        updateAtomPositionsForAnimation();
        startAnimation();
        invalidate();
    }

    private void updateAtomPositionsForAnimation() {
        for (String key : atoms.keySet()) {
            List<Atom> cellAtoms = atoms.get(key);
            if (cellAtoms.isEmpty()) continue;
            
            // Get cell info
            String[] parts = key.split(",");
            int i = Integer.parseInt(parts[0]);
            int j = Integer.parseInt(parts[1]);
            float centerX = j * cellWidth + cellWidth / 2;
            float centerY = i * cellHeight + cellHeight / 2;
            float atomRadius = Math.min(cellWidth, cellHeight) * 0.15f;
            int atomCount = cellAtoms.size();
            
            // Get current rotation angle
            float angleOffset = atomRotationAngles.getOrDefault(key, 0f);
            
            if (atomCount == 1) {
                cellAtoms.get(0).x = centerX;
                cellAtoms.get(0).y = centerY;
            } else if (atomCount == 2) {
                float offset = atomRadius * 1.2f;
                float angle1 = (float)Math.toRadians(angleOffset);
                float angle2 = (float)Math.toRadians(angleOffset + 180);
                cellAtoms.get(0).x = centerX + (float)Math.cos(angle1) * offset;
                cellAtoms.get(0).y = centerY + (float)Math.sin(angle1) * offset;
                cellAtoms.get(1).x = centerX + (float)Math.cos(angle2) * offset;
                cellAtoms.get(1).y = centerY + (float)Math.sin(angle2) * offset;
            } else if (atomCount == 3) {
                float offset = atomRadius * 1.2f;
                for (int k = 0; k < 3; k++) {
                    float angle = (float)Math.toRadians(angleOffset + k * 120);
                    cellAtoms.get(k).x = centerX + (float)Math.cos(angle) * offset;
                    cellAtoms.get(k).y = centerY + (float)Math.sin(angle) * offset;
                }
            }
        }
    }

    public void startExplosionAnimation(int row, int col) {
        // Update atoms immediately before starting animation
        updateAtoms();

        float centerX = col * cellWidth + cellWidth / 2;
        float centerY = row * cellHeight + cellHeight / 2;
        float maxRadius = Math.max(cellWidth, cellHeight) * 1.5f;

        Cell cell = gameLogic.getBoard()[row][col];
        int color = cell.getOwnerPlayerId() >= 0 ?
                gameLogic.getPlayers().get(cell.getOwnerPlayerId()).getColor() :
                Color.GRAY;

        explosionAnimations.add(new ExplosionAnimation(centerX, centerY, maxRadius, color));
        startAnimation();
    }

    private void startAnimation() {
        if (!isAnimating) {
            isAnimating = true;
            handler.post(new Runnable() {
                @Override
                public void run() {
                    boolean hasActiveAnimations = false;

                    // Update rotation angles
                    for (String key : atomRotationAngles.keySet()) {
                        float currentAngle = atomRotationAngles.get(key);
                        currentAngle += rotationSpeed;
                        if (currentAngle >= 360f) {
                            currentAngle -= 360f;
                        }
                        atomRotationAngles.put(key, currentAngle);
                        hasActiveAnimations = true;
                    }

                    // Update atom positions for rotation
                    updateAtomPositionsForAnimation();

                    // Update atom animations
                    for (List<Atom> cellAtoms : atoms.values()) {
                        for (Atom atom : cellAtoms) {
                            atom.update();
                            if (atom.isAnimating) {
                                hasActiveAnimations = true;
                            }
                        }
                    }

                    // Update explosion animations
                    for (ExplosionAnimation anim : explosionAnimations) {
                        anim.update();
                        if (anim.isActive) {
                            hasActiveAnimations = true;
                        }
                    }

                    // Remove completed explosion animations
                    explosionAnimations.removeIf(anim -> !anim.isActive);

                    if (hasActiveAnimations) {
                        invalidate();
                        handler.postDelayed(this, 16); // ~60 FPS
                    } else {
                        isAnimating = false;
                    }
                }
            });
        }
    }

    public void stopAnimation() {
        isAnimating = false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (gameLogic == null) return;

        // Draw grid
        Cell[][] board = gameLogic.getBoard();
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                float left = j * cellWidth;
                float top = i * cellHeight;
                float right = left + cellWidth;
                float bottom = top + cellHeight;
                canvas.drawRect(left, top, right, bottom, cellPaint);
            }
        }

        // Draw atoms
        Paint atomPaint = new Paint();
        atomPaint.setAntiAlias(true);
        for (List<Atom> cellAtoms : atoms.values()) {
            for (Atom atom : cellAtoms) {
                atom.draw(canvas, atomPaint);
            }
        }

        // Draw explosion animations on top
        Paint explosionPaint = new Paint();
        explosionPaint.setStyle(Paint.Style.FILL);
        explosionPaint.setAntiAlias(true);
        for (ExplosionAnimation anim : explosionAnimations) {
            anim.draw(canvas, explosionPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && gameLogic != null && !gameLogic.isProcessingExplosion()) {
            int col = (int) (event.getX() / cellWidth);
            int row = (int) (event.getY() / cellHeight);

            if (gameLogic.placeAtom(row, col)) {
                updateAtoms();
                invalidate();
            }
            return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAnimation();
    }
}