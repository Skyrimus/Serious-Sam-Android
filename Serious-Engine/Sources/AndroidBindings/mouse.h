#pragma once

#include <time.h>

#define ACTION_DOWN 0
#define ACTION_UP 1
#define ACTION_OUTSIDE 4
#define ACTION_MOVE 2
#define ACTION_HOVER_MOVE 7
#define ACTION_SCROLL 8
#define ACTION_BUTTON_PRESS 11
#define ACTION_BUTTON_RELEASE 12
#define BUTTON_PRIMARY 1
#define BUTTON_SECONDARY 2
#define BUTTON_TERTIARY 4
#define BUTTON_BACK 8
#define BUTTON_FORWARD 16
#define BUTTON_LEFT     1
#define BUTTON_MIDDLE   2
#define BUTTON_RIGHT    3
#define BUTTON_X1       4
#define BUTTON_X2       5
#define KEYCODE_W		51
#define KEYCODE_A		29
#define KEYCODE_S		47
#define KEYCODE_D		32
#define KEYCODE_SPACE	62

void simButtonPress(BOOL &action) {
    action = 1;
}

void changeWeapon(int weapon) {
    if (weapon == 1) {
        simButtonPress(g_cb.g_IncomingControls.bWeaponNext);
    } else {
        simButtonPress(g_cb.g_IncomingControls.bWeaponPrev);
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_github_aarcangeli_serioussamandroid_MainActivity_nSendMouseNative(JNIEnv *env, jclass clazz, jint state, jint action, jfloat scroll) {
    static int s_lastScrollDir = 0;
    static long long s_lastScrollMs = 0;
    pthread_mutex_lock(&g_inputMutex);

    if (action == ACTION_DOWN || action == ACTION_UP
            || action == ACTION_MOVE || action == ACTION_HOVER_MOVE
            || action == ACTION_BUTTON_PRESS
            || action == ACTION_BUTTON_RELEASE) {
        g_cb.g_IncomingControls.bFire = (state & BUTTON_PRIMARY) != 0;
        g_cb.g_IncomingControls.bUse = (state & BUTTON_SECONDARY) != 0;
        g_cb.g_IncomingControls.bReload = (state & BUTTON_TERTIARY) != 0;
    } else if (action == ACTION_SCROLL) {
        // Fire at most once per physical wheel notch (debounce noisy devices).
        int dir = (scroll > 0.0f) ? 1 : (scroll < 0.0f ? -1 : 0);
        if (dir != 0) {
            struct timespec ts;
            clock_gettime(CLOCK_MONOTONIC, &ts);
            long long nowMs = (long long)ts.tv_sec * 1000LL + ts.tv_nsec / 1000000LL;
            const long long kDebounceMs = 200;
            if (!(dir == s_lastScrollDir && (nowMs - s_lastScrollMs) < kDebounceMs)) {
                changeWeapon(dir > 0 ? 1 : 2);
                s_lastScrollDir = dir;
                s_lastScrollMs = nowMs;
            }
        }
    }

    pthread_mutex_unlock(&g_inputMutex);
}

