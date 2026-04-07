package com.github.aarcangeli.serioussamandroid;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class GamepadBindingsActivity extends Activity {
	private SharedPreferences preferences;
	private BindingsAdapter adapter;
	private String pendingAction;
	private AlertDialog captureDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_gamepad_bindings);

		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		adapter = new BindingsAdapter();

		ListView listView = findViewById(R.id.gamepad_bindings_list);
		listView.setAdapter(adapter);
		listView.setOnItemClickListener((parent, view, position, id) -> beginCapture(GamepadBindings.ACTIONS[position]));

		Button resetButton = findViewById(R.id.gamepad_bindings_reset);
		resetButton.setOnClickListener(v -> resetToDefaults());
	}

	private void beginCapture(String action) {
		pendingAction = action;
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(GamepadBindings.getActionTitleRes(action)));
		builder.setMessage(R.string.gamepad_bindings_capture_message);
		builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				pendingAction = null;
				captureDialog = null;
			}
		});
		builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				pendingAction = null;
				captureDialog = null;
			}
		});
		captureDialog = builder.create();
		captureDialog.setCanceledOnTouchOutside(false);
		captureDialog.setOnKeyListener((dialog, keyCode, event) -> {
			if (pendingAction == null
					|| event.getAction() != KeyEvent.ACTION_DOWN
					|| event.getRepeatCount() != 0
					|| !isControllerSource(event.getSource())) {
				return false;
			}
			if (GamepadBindings.isAssignableKeyCode(keyCode)) {
				assignBinding(pendingAction, keyCode);
				return true;
			}
			return false;
		});
		captureDialog.show();
	}

	private void finishCapture() {
		if (captureDialog != null) {
			captureDialog.dismiss();
			captureDialog = null;
		}
		pendingAction = null;
	}

	private void resetToDefaults() {
		SharedPreferences.Editor editor = preferences.edit();
		GamepadBindings.resetToDefaults(editor);
		editor.apply();
		adapter.notifyDataSetChanged();
		Toast.makeText(this, R.string.gamepad_bindings_reset_done, Toast.LENGTH_SHORT).show();
	}

	private void assignBinding(String action, int keyCode) {
		if (action == null || !GamepadBindings.isAssignableKeyCode(keyCode)) {
			return;
		}

		SharedPreferences.Editor editor = preferences.edit();
		for (String otherAction : GamepadBindings.ACTIONS) {
			if (!otherAction.equals(action) && GamepadBindings.getBoundKeyCode(preferences, otherAction) == keyCode) {
				editor.putInt(GamepadBindings.getPreferenceKey(otherAction), KeyEvent.KEYCODE_UNKNOWN);
			}
		}
		editor.putInt(GamepadBindings.getPreferenceKey(action), keyCode);
		editor.apply();

		adapter.notifyDataSetChanged();
		Toast.makeText(this,
				getString(R.string.gamepad_bindings_assigned,
						getString(GamepadBindings.getActionTitleRes(action)),
						GamepadBindings.getButtonLabel(this, keyCode)),
				Toast.LENGTH_SHORT).show();
		finishCapture();
	}

	private boolean isControllerSource(int source) {
		return ((source & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK)
				|| ((source & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD)
				|| ((source & InputDevice.SOURCE_DPAD) == InputDevice.SOURCE_DPAD);
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if (pendingAction != null
				&& event.getAction() == KeyEvent.ACTION_DOWN
				&& event.getRepeatCount() == 0
				&& isControllerSource(event.getSource())
				&& GamepadBindings.isAssignableKeyCode(event.getKeyCode())) {
			assignBinding(pendingAction, event.getKeyCode());
			return true;
		}
		return super.dispatchKeyEvent(event);
	}

	@Override
	public boolean dispatchGenericMotionEvent(MotionEvent event) {
		if (pendingAction == null || !isControllerSource(event.getSource())) {
			return super.dispatchGenericMotionEvent(event);
		}

		if (event.getActionMasked() != MotionEvent.ACTION_MOVE) {
			return super.dispatchGenericMotionEvent(event);
		}

		if (event.getAxisValue(MotionEvent.AXIS_LTRIGGER) > 0.5f) {
			assignBinding(pendingAction, KeyEvent.KEYCODE_BUTTON_L2);
			return true;
		}
		if (event.getAxisValue(MotionEvent.AXIS_RTRIGGER) > 0.5f) {
			assignBinding(pendingAction, KeyEvent.KEYCODE_BUTTON_R2);
			return true;
		}
		if (event.getAxisValue(MotionEvent.AXIS_HAT_X) < -0.5f) {
			assignBinding(pendingAction, KeyEvent.KEYCODE_DPAD_LEFT);
			return true;
		}
		if (event.getAxisValue(MotionEvent.AXIS_HAT_X) > 0.5f) {
			assignBinding(pendingAction, KeyEvent.KEYCODE_DPAD_RIGHT);
			return true;
		}
		if (event.getAxisValue(MotionEvent.AXIS_HAT_Y) < -0.5f) {
			assignBinding(pendingAction, KeyEvent.KEYCODE_DPAD_UP);
			return true;
		}
		if (event.getAxisValue(MotionEvent.AXIS_HAT_Y) > 0.5f) {
			assignBinding(pendingAction, KeyEvent.KEYCODE_DPAD_DOWN);
			return true;
		}

		return super.dispatchGenericMotionEvent(event);
	}

	private class BindingsAdapter extends BaseAdapter {
		@Override
		public int getCount() {
			return GamepadBindings.ACTIONS.length;
		}

		@Override
		public Object getItem(int position) {
			return GamepadBindings.ACTIONS[position];
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;
			if (view == null) {
				view = LayoutInflater.from(GamepadBindingsActivity.this).inflate(android.R.layout.simple_list_item_2, parent, false);
			}

			String action = GamepadBindings.ACTIONS[position];
			TextView title = view.findViewById(android.R.id.text1);
			TextView summary = view.findViewById(android.R.id.text2);
			title.setText(GamepadBindings.getActionTitleRes(action));
			summary.setText(GamepadBindings.getButtonLabel(GamepadBindingsActivity.this,
					GamepadBindings.getBoundKeyCode(preferences, action)));
			return view;
		}
	}
}
