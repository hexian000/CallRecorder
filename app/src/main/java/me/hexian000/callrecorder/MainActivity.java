package me.hexian000.callrecorder;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
	private static final int PERMISSIONS_REQUEST_CODE = 0;
	private static final String[] CALL_RECORD_PERMISSIONS = new String[]{
			Manifest.permission.CAPTURE_AUDIO_OUTPUT,
			Manifest.permission.WRITE_EXTERNAL_STORAGE,
			Manifest.permission.RECORD_AUDIO,
			Manifest.permission.READ_PHONE_STATE,
			Manifest.permission.READ_CONTACTS,
			Manifest.permission.READ_CALL_LOG,
			"android.permission.READ_PRIVILEGED_PHONE_STATE",
	};
	private static final String[] RECORD_PERMISSIONS = new String[]{
			Manifest.permission.WRITE_EXTERNAL_STORAGE,
			Manifest.permission.RECORD_AUDIO,
	};
	private CallRecorder app;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		app = (CallRecorder) getApplicationContext();
	}

	@Override
	protected void onResume() {
		super.onResume();
		update();
		app.mainActivity = this;
	}

	@Override
	protected void onPause() {
		app.mainActivity = null;
		super.onPause();
	}

	void update() {
		final CallRecorder app = (CallRecorder) getApplicationContext();
		final ToggleButton toggleCallRecord = findViewById(R.id.toggleCallRecord);
		toggleCallRecord.setChecked(app.isEnabled());
		final ToggleButton toggleMicRecord = findViewById(R.id.toggleMicRecord);
		toggleMicRecord.setChecked(app.micRecordService != null);
	}

	public void ToggleCallRecord(View v) {
		final CallRecorder app = (CallRecorder) getApplicationContext();
		final ToggleButton toggle = (ToggleButton) v;
		if (toggle.isChecked()) {
			grantPermissions(checkPermissions(CALL_RECORD_PERMISSIONS));

			if (checkPermissions(CALL_RECORD_PERMISSIONS).length > 0) {
				toggle.setChecked(false);
				Toast.makeText(this, R.string.no_permission, Toast.LENGTH_SHORT).show();
				return;
			}

			app.setEnabled(true);
		} else {
			app.setEnabled(false);
		}
	}

	public void ToggleRecord(View v) {
		final ToggleButton toggle = (ToggleButton) v;
		if (toggle.isChecked()) {
			grantPermissions(checkPermissions(RECORD_PERMISSIONS));

			if (checkPermissions(RECORD_PERMISSIONS).length > 0) {
				toggle.setChecked(false);
				Toast.makeText(this, R.string.no_permission, Toast.LENGTH_SHORT).show();
				return;
			}

			final Intent intent = new Intent(getApplicationContext(), MicRecordService.class);
			startForegroundService(intent);
		} else {
			final Intent intent = new Intent(getApplicationContext(), MicRecordService.class);
			stopService(intent);
		}
	}

	private String[] checkPermissions(final String[] desired) {
		final List<String> permissions = new ArrayList<>();
		for (String perm : desired) {
			if (checkSelfPermission(perm) != PackageManager.PERMISSION_GRANTED) {
				Log.d(CallRecorder.LOG_TAG, perm);
				permissions.add(perm);
			}
		}

		return permissions.toArray(new String[]{});
	}

	private void grantPermissions(final String[] permissions) {
		if (permissions.length > 0) {
			requestPermissions(permissions, PERMISSIONS_REQUEST_CODE);
		}
	}
}
