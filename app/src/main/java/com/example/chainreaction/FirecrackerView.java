package com.example.chainreaction;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FirecrackerView extends View {
    private List<Particle> particles;
    private Paint paint;
    private Random random;
    private ValueAnimator animator;
    private boolean isAnimating;
    private long lastBurstTime;
    private static final long BURST_INTERVAL = 1000; // Time between bursts in milliseconds

    public FirecrackerView(Context context) {
        super(context);
        init();
    }

    public FirecrackerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        particles = new ArrayList<>();
        paint = new Paint();
        random = new Random();
        isAnimating = false;
        lastBurstTime = 0;
    }

    public void startFirecrackerAnimation() {
        if (isAnimating) {
            return;
        }
        isAnimating = true;
        particles.clear();
        lastBurstTime = System.currentTimeMillis();

        // Start animation
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(120000);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();
                updateParticles(value);
                
                // Check if it's time for a new burst
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastBurstTime >= BURST_INTERVAL) {
                    createBurst(true);  // Left side burst
                    createBurst(false); // Right side burst
                    lastBurstTime = currentTime;
                }
                
                invalidate();
            }
        });
        animator.start();
    }

    private void createBurst(boolean isLeftSide) {
        int numParticles = 150; // Half of total particles per burst
        float startX = isLeftSide ? 0 : getWidth();
        float startY = getHeight() / 2;
        
        for (int i = 0; i < numParticles; i++) {
            float angle = isLeftSide ? 
                random.nextFloat() * 180 - 90 : // Left side: -90 to 90 degrees
                random.nextFloat() * 180 + 90;  // Right side: 90 to 270 degrees
            float speed = random.nextFloat() * 40 + 10;
            float size = random.nextFloat() * 12 + 3;
            int color = getRandomFirecrackerColor();
            particles.add(new Particle(startX, startY, angle, speed, size, color));
        }
    }

    private void updateParticles(float progress) {
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle particle = particles.get(i);
            particle.update(progress);
            // Add some randomness to particle movement
            particle.x += random.nextFloat() * 2 - 1;
            particle.y += random.nextFloat() * 2 - 1;
            
            // Remove particles that are too old or out of bounds
            if (particle.progress > 0.8f || 
                particle.x < -100 || particle.x > getWidth() + 100 ||
                particle.y < -100 || particle.y > getHeight() + 100) {
                particles.remove(i);
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (Particle particle : particles) {
            paint.setColor(particle.color);
            paint.setAlpha((int) (255 * (1 - particle.progress)));
            canvas.drawCircle(particle.x, particle.y, particle.size, paint);
        }
    }

    private int getRandomFirecrackerColor() {
        int[] colors = {
            Color.RED,
            Color.YELLOW,
            0xFFFFA500, // Orange
            Color.WHITE,
            Color.CYAN
        };
        return colors[random.nextInt(colors.length)];
    }

    private static class Particle {
        float x, y;
        float angle;
        float speed;
        float size;
        int color;
        float progress;

        Particle(float startX, float startY, float angle, float speed, float size, int color) {
            this.x = startX;
            this.y = startY;
            this.angle = angle;
            this.speed = speed;
            this.size = size;
            this.color = color;
            this.progress = 0;
        }

        void update(float progress) {
            this.progress = progress;
            float radians = (float) Math.toRadians(angle);
            x += Math.cos(radians) * speed * (1 - progress);
            y += Math.sin(radians) * speed * (1 - progress);
        }
    }
} 