package semyon.android_studio.ball_game;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;
import android.view.MotionEvent;
import androidx.annotation.NonNull;
import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
public class MainActivity2 extends AppCompatActivity {
    static {
        System.loadLibrary("ball_game");
    }
    public native void initNativeGame(float width, float height);
    public native void updateAndGetState(float dt, float[] stateArray);
    public native void nativeJump();
    public native void nativeReset();
    public native boolean nativeIsGameOver();
    private volatile boolean isGameOver = false;
    private volatile boolean isDialogShowing = false;
    private volatile boolean isPaused = false;
    private AlertDialog currentDialog;
    private MediaPlayer mediaPlayer;
    public boolean getIsGameOver() {
        return isGameOver;
    }
    public boolean getIsPaused() {
        return isPaused;
    }
    private SoundPool soundPool;
    private int soundClick;
    private int soundPauseClick;
    private boolean isSoundLoaded = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main2);

        WindowInsetsControllerCompat windowInsetsController = ViewCompat.getWindowInsetsController(getWindow().getDecorView());
        if (windowInsetsController != null) {
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
            windowInsetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> insets);

        findViewById(R.id.pauseButton).setOnClickListener(v -> {
            if (!isGameOver && !isPaused && !isDialogShowing) {
                if (isSoundLoaded) {
                    soundPool.play(soundPauseClick, 1.0f, 1.0f, 0, 0, 1.0f);
                }
                showPauseDialog();
            }
        });

        mediaPlayer = MediaPlayer.create(this, R.raw.f88756);
        if (mediaPlayer != null) {
            mediaPlayer.setLooping(true);
            mediaPlayer.start();
        }

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(1)
                .setAudioAttributes(audioAttributes)
                .build();

        soundPool.setOnLoadCompleteListener((soundPool, sampleId, status) -> {
            if (status == 0) {
                isSoundLoaded = true;
            }
        });

        soundClick = soundPool.load(this, R.raw.weak, 1);
        soundPauseClick = soundPool.load(this, R.raw.button_click, 1);
        
        isGameOver = nativeIsGameOver();

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (!isGameOver && !isPaused && !isDialogShowing) {
                    showPauseDialog();
                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (!isDialogShowing) {
                nativeJump();
            }
        }
        return super.onTouchEvent(event);
    }

    public void setGameOver(boolean gameOver) {
        this.isGameOver = gameOver;
    }

    public void showPauseDialog() {
        if (isDialogShowing) return;
        isDialogShowing = true;
        isPaused = true;
        runOnUiThread(() -> {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
            }
            if (currentDialog != null && currentDialog.isShowing()) {
                currentDialog.dismiss();
            }
            currentDialog = new AlertDialog.Builder(MainActivity2.this, R.style.CustomAlertDialog)
                    .setTitle("Пауза")
                    .setMessage("Игра приостановлена, при выходе из игры очки будут потеряны!")
                    .setCancelable(false)
                    .setPositiveButton("Продолжить", (d, which) -> {
                        if (isSoundLoaded) {
                            soundPool.play(soundPauseClick, 1.0f, 1.0f, 0, 0, 1.0f);
                        }
                        isPaused = false;
                        isDialogShowing = false;
                        if (mediaPlayer != null) {
                            mediaPlayer.start();
                        }
                    })
                    .setNegativeButton("Выйти", (d, which) -> {
                        if (isSoundLoaded) {
                            soundPool.play(soundPauseClick, 1.0f, 1.0f, 0, 0, 1.0f);
                        }
                        nativeReset();
                        finish();
                        overridePendingTransition(0, 0);
                    })
                    .create();
            currentDialog.setOnShowListener(dialogInterface -> {
                currentDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.blue));
                currentDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.blue));
            });
            currentDialog.show();
        });
    }

    public void showGameOverDialog(int finalScore) {
        if (isDialogShowing) return;
        isDialogShowing = true;
        isGameOver = true;
        isPaused = true;
        
        if (isSoundLoaded) {
            soundPool.play(soundClick, 1.0f, 1.0f, 0, 0, 1.0f);
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                }
                
                if (currentDialog != null && currentDialog.isShowing()) {
                    currentDialog.dismiss();
                }

                SharedPreferences sharedPref = getSharedPreferences("my_settings", Context.MODE_PRIVATE);
                String savedText = sharedPref.getString("user_score", "0");
                try {
                    if (Integer.parseInt(savedText) < finalScore) {
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putString("user_score", String.valueOf(finalScore));
                        editor.apply();
                    }
                } catch (NumberFormatException e) {
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString("user_score", String.valueOf(finalScore));
                    editor.apply();
                }

                currentDialog = new AlertDialog.Builder(MainActivity2.this, R.style.CustomAlertDialog)
                        .setTitle("Игра окончена!")
                        .setMessage("Ваш итоговый счёт: " + finalScore)
                        .setCancelable(false)
                        .setPositiveButton("Играть снова", (d, which) -> {
                            if (isSoundLoaded) {
                                soundPool.play(soundPauseClick, 1.0f, 1.0f, 0, 0, 1.0f);
                            }
                            nativeReset();
                            isGameOver = false;
                            isPaused = false;
                            isDialogShowing = false;
                            if (mediaPlayer != null) {
                                mediaPlayer.seekTo(0);
                                mediaPlayer.start();
                            }
                        })
                        .setNegativeButton("Выйти", (d, which) -> {
                            if (isSoundLoaded) {
                                soundPool.play(soundPauseClick, 1.0f, 1.0f, 0, 0, 1.0f);
                            }
                            nativeReset();
                            finish();
                            overridePendingTransition(0, 0);
                        })
                        .create();
                currentDialog.setOnShowListener(dialogInterface -> {
                    currentDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.blue));
                    currentDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.blue));
                });
                currentDialog.show();
            }
        });

    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("isGameOver", isGameOver);
        outState.putBoolean("isPaused", isPaused);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        isGameOver = savedInstanceState.getBoolean("isGameOver");
        isPaused = savedInstanceState.getBoolean("isPaused");
        isDialogShowing = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        isGameOver = nativeIsGameOver();
        if (isGameOver && !isDialogShowing) {
            float[] state = new float[100];
            updateAndGetState(0, state);
            showGameOverDialog((int) state[3]);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!isGameOver && !isPaused && !isDialogShowing) {
            showPauseDialog();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
