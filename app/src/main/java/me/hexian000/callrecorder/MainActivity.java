package me.hexian000.callrecorder;

import android.app.Activity;
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
	private static final String[] DESIRED_PERMISSIONS = new String[]{
			"android.permission.CAPTURE_AUDIO_OUTPUT",
			"android.permission.READ_PRIVILEGED_PHONE_STATE",
			"android.permission.WRITE_EXTERNAL_STORAGE",
			"android.permission.RECORD_AUDIO",
			"android.permission.PROCESS_OUTGOING_CALLS",
			"android.permission.READ_PHONE_STATE",
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		final CallRecorder app = (CallRecorder) getApplicationContext();
		final ToggleButton toggle = findViewById(R.id.toggleCallRecord);
		toggle.setChecked(app.isEnabled());
	}

	public void ToggleCallRecord(View v) {
		final CallRecorder app = (CallRecorder) getApplicationContext();
		final ToggleButton toggle = (ToggleButton) v;
		if (toggle.isChecked()) {
			grantPermissions(checkPermissions(DESIRED_PERMISSIONS));

			if (checkPermissions(DESIRED_PERMISSIONS).length > 0) {
				toggle.setChecked(false);
				Toast.makeText(this, R.string.no_permission, Toast.LENGTH_SHORT).show();
				return;
			}

			app.setEnabled(true);
		} else {
			app.setEnabled(false);
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
