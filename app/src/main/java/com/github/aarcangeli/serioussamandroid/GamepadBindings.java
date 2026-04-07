package com.github.aarcangeli.serioussamandroid;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.KeyEvent;

public final class GamepadBindings {
	public static final String ACTION_FIRE = "Fire";
	public static final String ACTION_USE = "Use";
	public static final String ACTION_JUMP = "Jump";
	public static final String ACTION_CROUCH = "Crouch";
	public static final String ACTION_PREV_WEAPON = "PrevWeapon";
	public static final String ACTION_NEXT_WEAPON = "NextWeapon";
	public static final String ACTION_SERIOUS_BOMB = "SeriousBomb";
	public static final String ACTION_COMPUTER = "Computer";
	public static final String ACTION_PAUSE_MENU = "PauseMenu";

	public static final String[] ACTIONS = {
			ACTION_FIRE,
			ACTION_USE,
			ACTION_JUMP,
			ACTION_CROUCH,
			ACTION_PREV_WEAPON,
			ACTION_NEXT_WEAPON,
			ACTION_SERIOUS_BOMB,
			ACTION_COMPUTER,
			ACTION_PAUSE_MENU
	};

	private GamepadBindings() {
	}

	public static String getPreferenceKey(String action) {
		return "gamepad_bind_" + action;
	}

	public static int getDefaultKeyCode(String action) {
		if (ACTION_FIRE.equals(action)) {
			return KeyEvent.KEYCODE_BUTTON_R2;
		}
		if (ACTION_USE.equals(action)) {
			return KeyEvent.KEYCODE_BUTTON_X;
		}
		if (ACTION_JUMP.equals(action)) {
			return KeyEvent.KEYCODE_BUTTON_A;
		}
		if (ACTION_CROUCH.equals(action)) {
			return KeyEvent.KEYCODE_BUTTON_THUMBL;
		}
		if (ACTION_PREV_WEAPON.equals(action)) {
			return KeyEvent.KEYCODE_BUTTON_L1;
		}
		if (ACTION_NEXT_WEAPON.equals(action)) {
			return KeyEvent.KEYCODE_BUTTON_R1;
		}
		if (ACTION_SERIOUS_BOMB.equals(action)) {
			return KeyEvent.KEYCODE_BUTTON_Y;
		}
		if (ACTION_COMPUTER.equals(action)) {
			return KeyEvent.KEYCODE_BACK;
		}
		if (ACTION_PAUSE_MENU.equals(action)) {
			return KeyEvent.KEYCODE_BUTTON_START;
		}
		return KeyEvent.KEYCODE_UNKNOWN;
	}

	public static int getBoundKeyCode(SharedPreferences preferences, String action) {
		return preferences.getInt(getPreferenceKey(action), getDefaultKeyCode(action));
	}

	public static void resetToDefaults(SharedPreferences.Editor editor) {
		for (String action : ACTIONS) {
			editor.putInt(getPreferenceKey(action), getDefaultKeyCode(action));
		}
	}

	public static int getActionTitleRes(String action) {
		if (ACTION_FIRE.equals(action)) {
			return R.string.gamepad_action_fire;
		}
		if (ACTION_USE.equals(action)) {
			return R.string.gamepad_action_use;
		}
		if (ACTION_JUMP.equals(action)) {
			return R.string.gamepad_action_jump;
		}
		if (ACTION_CROUCH.equals(action)) {
			return R.string.gamepad_action_crouch;
		}
		if (ACTION_PREV_WEAPON.equals(action)) {
			return R.string.gamepad_action_prev_weapon;
		}
		if (ACTION_NEXT_WEAPON.equals(action)) {
			return R.string.gamepad_action_next_weapon;
		}
		if (ACTION_SERIOUS_BOMB.equals(action)) {
			return R.string.gamepad_action_serious_bomb;
		}
		if (ACTION_COMPUTER.equals(action)) {
			return R.string.gamepad_action_computer;
		}
		if (ACTION_PAUSE_MENU.equals(action)) {
			return R.string.gamepad_action_pause_menu;
		}
		return 0;
	}

	public static boolean isAssignableKeyCode(int keyCode) {
		switch (keyCode) {
			case KeyEvent.KEYCODE_BUTTON_A:
			case KeyEvent.KEYCODE_BUTTON_B:
			case KeyEvent.KEYCODE_BUTTON_X:
			case KeyEvent.KEYCODE_BUTTON_Y:
			case KeyEvent.KEYCODE_BUTTON_L1:
			case KeyEvent.KEYCODE_BUTTON_R1:
			case KeyEvent.KEYCODE_BUTTON_L2:
			case KeyEvent.KEYCODE_BUTTON_R2:
			case KeyEvent.KEYCODE_BUTTON_THUMBL:
			case KeyEvent.KEYCODE_BUTTON_THUMBR:
			case KeyEvent.KEYCODE_BUTTON_SELECT:
			case KeyEvent.KEYCODE_BUTTON_START:
			case KeyEvent.KEYCODE_BUTTON_MODE:
			case KeyEvent.KEYCODE_DPAD_UP:
			case KeyEvent.KEYCODE_DPAD_DOWN:
			case KeyEvent.KEYCODE_DPAD_LEFT:
			case KeyEvent.KEYCODE_DPAD_RIGHT:
			case KeyEvent.KEYCODE_BACK:
				return true;
			default:
				return false;
		}
	}

	public static String getButtonLabel(Context context, int keyCode) {
		switch (keyCode) {
			case KeyEvent.KEYCODE_BUTTON_A:
				return "A";
			case KeyEvent.KEYCODE_BUTTON_B:
				return "B";
			case KeyEvent.KEYCODE_BUTTON_X:
				return "X";
			case KeyEvent.KEYCODE_BUTTON_Y:
				return "Y";
			case KeyEvent.KEYCODE_BUTTON_L1:
				return "L1";
			case KeyEvent.KEYCODE_BUTTON_R1:
				return "R1";
			case KeyEvent.KEYCODE_BUTTON_L2:
				return "L2";
			case KeyEvent.KEYCODE_BUTTON_R2:
				return "R2";
			case KeyEvent.KEYCODE_BUTTON_THUMBL:
				return "L3";
			case KeyEvent.KEYCODE_BUTTON_THUMBR:
				return "R3";
			case KeyEvent.KEYCODE_BUTTON_START:
				return "Start";
			case KeyEvent.KEYCODE_BUTTON_SELECT:
				return "Select";
			case KeyEvent.KEYCODE_BUTTON_MODE:
				return "Mode";
			case KeyEvent.KEYCODE_DPAD_UP:
				return context.getString(R.string.gamepad_button_dpad_up);
			case KeyEvent.KEYCODE_DPAD_DOWN:
				return context.getString(R.string.gamepad_button_dpad_down);
			case KeyEvent.KEYCODE_DPAD_LEFT:
				return context.getString(R.string.gamepad_button_dpad_left);
			case KeyEvent.KEYCODE_DPAD_RIGHT:
				return context.getString(R.string.gamepad_button_dpad_right);
			case KeyEvent.KEYCODE_BACK:
				return context.getString(R.string.gamepad_button_back);
			case KeyEvent.KEYCODE_UNKNOWN:
			default:
				return context.getString(R.string.gamepad_button_unassigned);
		}
	}
}
