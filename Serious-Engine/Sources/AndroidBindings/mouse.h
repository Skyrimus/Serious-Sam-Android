#pragma once

#define ACTION_DOWN 0
#define ACTION_UP 1
#define ACTION_OUTSIDE 4
#define ACTION_MOVE 2
#define ACTION_HOVER_MOVE 7
#define ACTION_SCROLL 8
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

static int last_state;
bool scrollFinished = false;


int TranslateButton(int state) {
	if (state & BUTTON_PRIMARY) {
		return BUTTON_LEFT;
	} else if (state & BUTTON_SECONDARY) {
		return BUTTON_RIGHT;
	} else if (state & BUTTON_TERTIARY) {
		return BUTTON_MIDDLE;
	} else if (state & BUTTON_FORWARD) {
		return BUTTON_X1;
	} else if (state & BUTTON_BACK) {
		return BUTTON_X2;
	} else {
		return 0;
	}
}

void changeWeapon(int weapon) {
	if (weapon == 1) {
		if (g_cb.g_IncomingControls.bWeaponNext == 0) {
			g_cb.g_IncomingControls.bWeaponNext = 1;
		} else {
			g_cb.g_IncomingControls.bWeaponNext = 1;
			g_cb.g_IncomingControls.bWeaponNext = 0;
		}
		//CPrintF("Button right = %d\n", g_cb.g_IncomingControls.bWeaponNext);
	} else {
		if (g_cb.g_IncomingControls.bWeaponPrev == 0) {
			g_cb.g_IncomingControls.bWeaponPrev = 1;
		} else {
			g_cb.g_IncomingControls.bWeaponPrev = 1;
			g_cb.g_IncomingControls.bWeaponPrev = 0;
		}
		//CPrintF("Button left = %d\n", g_cb.g_IncomingControls.bWeaponPrev);
	}
	return;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_github_aarcangeli_serioussamandroid_MainActivity_nSendMouseNative(JNIEnv *env, jobject obj, jint state, jint action, jfloat scroll) {
  pthread_mutex_lock(&g_mySeriousMutex);
    int changes;
	int button;
    switch(action) {
        case ACTION_DOWN:
		    changes = state & ~last_state;
            button = TranslateButton(changes);	
			//CPrintF("Mouse button clicked - %d\n", button);
			if (button == 1) {
			g_cb.g_IncomingControls.bFire = 1;
			}
            break;
        case ACTION_UP:
		    changes = state & ~last_state;
            button = TranslateButton(changes);
			//CPrintF("Mouse button released\n");
			if (button == 0) {
			g_cb.g_IncomingControls.bFire = 0;
			}
            break;
        case ACTION_SCROLL:
			if (scroll > 0.0f) {
				//CPrintF("Mouse scrolling up\n");
				//g_cb.g_IncomingControls.bWeaponNext = 1;
				changeWeapon(1);
				} 

			if (scroll < 0.0f) {
				//CPrintF("Mouse scrolling down\n");
				//g_cb.g_IncomingControls.bWeaponPrev = 1;
				changeWeapon(2);	
				}

				//CPrintF("g_cb.g_IncomingControls.bWeaponNext= %f\n", g_cb.g_IncomingControls.bWeaponNext);
				//CPrintF("g_cb.g_IncomingControls.bWeaponPrev= %f\n", g_cb.g_IncomingControls.bWeaponPrev);
				//CPrintF("Mouse scroll= %f\n", scroll); 
				break;
        default:
            break;
    }
  pthread_mutex_unlock(&g_mySeriousMutex);
}
