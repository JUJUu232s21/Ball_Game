import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import androidx.annotation.NonNull;
public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private Thread gameThread;
    private volatile boolean isRunning;
    private final SurfaceHolder holder;
    private final MainActivity2 activity;
    private final Paint ballPaint;
    private final Paint obstaclePaint;
    private final Paint textPaint;
    private final Paint groundPaint;
    private final float[] gameState = new float[100];
    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        activity = (MainActivity2) context;
        holder = getHolder();
        holder.addCallback(this);
        ballPaint = new Paint();
        ballPaint.setColor(Color.WHITE);
        ballPaint.setAntiAlias(true);
        obstaclePaint = new Paint();
        obstaclePaint.setColor(Color.BLUE);
        textPaint = new Paint();
        textPaint.setColor(Color.BLUE);
        textPaint.setTextSize(60);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        textPaint.setTextAlign(Paint.Align.LEFT);
        groundPaint = new Paint();
        groundPaint.setColor(Color.DKGRAY);
    }
    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        activity.initNativeGame(getWidth(), getHeight());
        if (gameThread != null && gameThread.isAlive()) {
            isRunning = false;
            try {
                gameThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        isRunning = true;
        gameThread = new Thread(this);
        gameThread.start();
    }
    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {}
    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        isRunning = false;
        if (gameThread != null) {
            try {
                gameThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    @Override
    public void run() {
        long lastTime = System.nanoTime();
        while (isRunning) {
            long now = System.nanoTime();
            float deltaTime = (now - lastTime) / 1000000000.0f;
            lastTime = now;
            
            boolean gameOver = activity.getIsGameOver();
            boolean paused = activity.getIsPaused();
            
            if (!gameOver && !paused) {
                activity.updateAndGetState(deltaTime, gameState);
            }
            
            if (holder.getSurface().isValid()) {
                Canvas canvas = holder.lockCanvas();
                if (canvas != null) {
                    drawGame(canvas);
                    holder.unlockCanvasAndPost(canvas);
                }
            }
            long sleepTime = 16 - (System.nanoTime() - now) / 1000000;
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    private void drawGame(Canvas canvas) {
        canvas.drawColor(Color.parseColor("#121212"));
        float ballX = gameState[0];
        float ballY = gameState[1];
        float ballR = gameState[2];
        int score = (int) gameState[3];
        boolean nativeGameOver = gameState[4] > 0.5f;
        int obsCount = (int) gameState[5];
        for (int i = 0; i < obsCount; i++) {
            int baseIdx = 6 + i * 4;
            float ox = gameState[baseIdx];
            float oy = gameState[baseIdx + 1];
            float ow = gameState[baseIdx + 2];
            float oh = gameState[baseIdx + 3];
            float cornerRadius = 30f;
            canvas.drawRoundRect(ox, oy, ox + ow, oy + oh, cornerRadius, cornerRadius, obstaclePaint);
            canvas.drawRect(ox, oy + oh - cornerRadius, ox + ow, oy + oh, obstaclePaint);
        }
        canvas.drawRect(0, getHeight() - 150 + ballR, getWidth(), getHeight(), groundPaint);
        canvas.drawCircle(ballX, ballY, ballR, ballPaint);
        canvas.drawText("Счёт: " + score, 50, 100, textPaint);
        
        if (nativeGameOver && !activity.getIsGameOver()) {
            activity.showGameOverDialog(score);
        }
    }
}
