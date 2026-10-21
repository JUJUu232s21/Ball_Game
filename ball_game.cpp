#include <jni.h>
#include <vector>
#include <ctime>
#include <cstdlib>
#include <algorithm>
#include <android/log.h>
#define LOG_TAG "BallGameNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
struct Ball {
    float x, y;
    float vy;
    float radius;
    bool isGrounded;
    int jumpCount;
};
struct Obstacle {
    float x, y;
    float width, height;
    bool active;
};
class Game {
public:
    Ball ball;
    std::vector<Obstacle> obstacles;
    float screenWidth, screenHeight;
    float gravity = 3000.0f;
    float jumpForce = -1000.0f;
    float gameSpeed = 900.0f;
    bool gameOver = false;
    float spawnTimer = 0;
    int score = 0;
    Game(float w, float h) : screenWidth(w), screenHeight(h) {
        ball = {200.0f, h - 150.0f, 0, 40.0f, true, 0};
        spawnTimer = 1.0f;
        obstacles.reserve(10);
        srand(time(nullptr));
    }
    void update(float dt) {
        if (gameOver) return;
        ball.vy += gravity * dt;
        ball.y += ball.vy * dt;
        float groundY = screenHeight - 150.0f;
        if (ball.y > groundY) {
            ball.y = groundY;
            ball.vy = 0;
            ball.isGrounded = true;
            ball.jumpCount = 0;
        }
        spawnTimer -= dt;
        if (spawnTimer <= 0) {
            bool isLarge = (rand() % 10) > 6;
            float obsW = isLarge ? 140.0f : 70.0f;
            float obsH = isLarge ? 220.0f : 120.0f;
            obstacles.push_back({screenWidth, groundY + ball.radius - obsH + 30.0f, obsW, obsH, true});
            spawnTimer = 1.2f + (rand() % 100) / 100.0f;
        }
        for (auto& obs : obstacles) {
            obs.x -= gameSpeed * dt;
            float closestX = std::max(obs.x, std::min(ball.x, obs.x + obs.width));
            float closestY = std::max(obs.y, std::min(ball.y, obs.y + obs.height));
            float distanceX = ball.x - closestX;
            float distanceY = ball.y - closestY;
            float distanceSquared = (distanceX * distanceX) + (distanceY * distanceY);
            if (distanceSquared < (ball.radius * ball.radius)) {
                gameOver = true;
                break;
            }
            if (obs.active && obs.x + obs.width < ball.x) {
                obs.active = false;
                score++;
                gameSpeed += 5.0f;
            }
        }
        obstacles.erase(std::remove_if(obstacles.begin(), obstacles.end(),
            [](const Obstacle& o) { return o.x + o.width < -100; }), obstacles.end());
    }
    void jump() {
        if (ball.isGrounded) {
            ball.vy = jumpForce;
            ball.isGrounded = false;
            ball.jumpCount = 1;
        } else if (ball.jumpCount < 2) {
             ball.vy = jumpForce;
             ball.jumpCount = 2;
        }
    }
    void reset() {
        ball = {200.0f, screenHeight - 150.0f, 0, 40.0f, true, 0};
        obstacles.clear();
        spawnTimer = 1.0f;
        gameOver = false;
        score = 0;
        gameSpeed = 900.0f;
    }
};
static Game* gameInstance = nullptr;
extern "C" JNIEXPORT void JNICALL
Java_semyon_android_1studio_ball_1game_MainActivity2_initNativeGame(JNIEnv* env, jobject thiz, jfloat width, jfloat height) {
    if (!gameInstance) {
        gameInstance = new Game(width, height);
    } else {
        gameInstance->screenWidth = width;
        gameInstance->screenHeight = height;
    }
}
extern "C" JNIEXPORT void JNICALL
Java_semyon_android_1studio_ball_1game_MainActivity2_updateAndGetState(JNIEnv* env, jobject thiz, jfloat dt, jfloatArray stateArray) {
    if (!gameInstance) return;
    gameInstance->update(dt);
    int obsCount = (int)gameInstance->obstacles.size();
    float temp[100];
    temp[0] = gameInstance->ball.x;
    temp[1] = gameInstance->ball.y;
    temp[2] = gameInstance->ball.radius;
    temp[3] = (float) gameInstance->score;
    temp[4] = gameInstance->gameOver ? 1.0f : 0.0f;
    temp[5] = (float) obsCount;
    for (int i = 0; i < obsCount; i++) {
        int baseIdx = 6 + i * 4;
        if (baseIdx + 3 >= 100) break;
        temp[baseIdx]     = gameInstance->obstacles[i].x;
        temp[baseIdx + 1] = gameInstance->obstacles[i].y;
        temp[baseIdx + 2] = gameInstance->obstacles[i].width;
        temp[baseIdx + 3] = gameInstance->obstacles[i].height;
    }
    int elementsToCopy = 6 + obsCount * 4;
    if (elementsToCopy > 100) elementsToCopy = 100;
    env->SetFloatArrayRegion(stateArray, 0, elementsToCopy, temp);
}
extern "C" JNIEXPORT void JNICALL
Java_semyon_android_1studio_ball_1game_MainActivity2_nativeJump(JNIEnv* env, jobject thiz) {
    if (gameInstance) gameInstance->jump();
}
extern "C" JNIEXPORT void JNICALL
Java_semyon_android_1studio_ball_1game_MainActivity2_nativeReset(JNIEnv* env, jobject thiz) {
    if (gameInstance) gameInstance->reset();
}
extern "C" JNIEXPORT jboolean JNICALL
Java_semyon_android_1studio_ball_1game_MainActivity2_nativeIsGameOver(JNIEnv* env, jobject thiz) {
    if (gameInstance) return (jboolean)gameInstance->gameOver;
    return (jboolean)false;
}
