package me.hexian000.callrecorder;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
	private static final int PERMISSIONS_REQUEST_CALL = 0;
	private static final int PERMISSIONS_REQUEST_MIC = 1;
	private static final String[] CALL_RECORD_PERMISSIONS = new String[]{
			Manifest.permission.CAPTURE_AUDIO_OUTPUT,
			Manifest.permission.WRITE_EXTERNAL_STORAGE,
			Manifest.permission.RECORD_AUDIO,
			Manifest.permission.READ_PHONE_STATE,
			Manifest.permission.READ_CONTACTS,
			Manifest.permission.READ_CALL_LOG,
	};
	private static final String[] MIC_RECORD_PERMISSIONS = new String[]{
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
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	private void update() {
		if (app.isEnabled() && checkPermissions(CALL_RECORD_PERMISSIONS).length > 0) {
			app.setEnabled(false);
		}

		final ToggleButton toggleCallRecord = findViewById(R.id.toggleCallRecord);
		toggleCallRecord.setChecked(app.isEnabled());
	}

	public void ToggleCallRecord(View v) {
		final ToggleButton toggle = (ToggleButton) v;
		if (toggle.isChecked()) {
			final String[] perms = checkPermissions(CALL_RECORD_PERMISSIONS);
			if (perms.length > 0) {
				toggle.setChecked(false);
				grantPermissions(PERMISSIONS_REQUEST_CALL, perms);
				return;
			}

			app.setEnabled(true);
		} else {
			app.setEnabled(false);
		}
	}

	public void OnMicRecord(View v) {
		final String[] perms = checkPermissions(MIC_RECORD_PERMISSIONS);
		if (perms.length > 0) {
			grantPermissions(PERMISSIONS_REQUEST_MIC, perms);
			return;
		}

		if (app.isRecording()) {
			Toast.makeText(this, R.string.record_busy, Toast.LENGTH_LONG).show();
			return;
		}

		final Intent intent = new Intent(getApplicationContext(), AudioRecordService.class);
		startForegroundService(intent);

		Toast.makeText(this, R.string.record_begin, Toast.LENGTH_SHORT).show();
	}

	@NonNull
	private String[] checkPermissions(@NonNull final String[] desired) {
		final List<String> permissions = new ArrayList<>();
		for (String perm : desired) {
			if (checkSelfPermission(perm) != PackageManager.PERMISSION_GRANTED) {
				Log.d(CallRecorder.LOG_TAG, perm);
				permissions.add(perm);
			}
		}

		return permissions.toArray(new String[]{});
	}

	private void grantPermissions(final int code, @NonNull final String[] permissions) {
		if (permissions.length > 0) {
			requestPermissions(permissions, code);
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
	                                       @NonNull int[] grantResults) {
		for (int result : grantResults) {
			if (result != PackageManager.PERMISSION_GRANTED) {
				Toast.makeText(this, R.string.no_permission, Toast.LENGTH_SHORT).show();
				return;
			}
		}
		switch (requestCode) {
		case PERMISSIONS_REQUEST_CALL: {
			final ToggleButton toggle = findViewById(R.id.toggleCallRecord);
			toggle.setChecked(true);
			ToggleCallRecord(toggle);
		}
		break;
		case PERMISSIONS_REQUEST_MIC: {
			final Button button = findViewById(R.id.buttonMicRecord);
			OnMicRecord(button);
		}
		break;
		}
	}
}
