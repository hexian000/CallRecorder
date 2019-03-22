package me.hexian000.callrecorder;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static me.hexian000.callrecorder.CallRecorder.LOG_TAG;

public class MainActivity extends Activity {
	private static final int PERMISSIONS_REQUEST_CODE = 0;
	private static final String[] CALL_RECORD_PERMISSIONS = new String[]{
			"android.permission.CAPTURE_AUDIO_OUTPUT",
			"android.permission.READ_PRIVILEGED_PHONE_STATE",
			"android.permission.WRITE_EXTERNAL_STORAGE",
			"android.permission.RECORD_AUDIO",
			"android.permission.READ_PHONE_STATE",
	};
	private static final String[] RECORD_PERMISSIONS = new String[]{
			"android.permission.WRITE_EXTERNAL_STORAGE",
			"android.permission.RECORD_AUDIO",
	};

	private MediaRecorder recorder = null;
	private String writingFile = null;

	@Override
	protected void onPause() {
		stopMicRecord();
		super.onPause();
	}

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

			startMicRecord();
		} else {
			stopMicRecord();
		}
	}

	private void startMicRecord() {
		recorder = new MediaRecorder();
		recorder.setAudioChannels(1);
		recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
		recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
		recorder.setAudioEncodingBitRate(192000);

		File dir = new File(
				Environment.getExternalStoragePublicDirectory(
						Environment.DIRECTORY_MUSIC) + "/Recorder");
		if (!dir.exists()) {
			if (!dir.mkdirs()) {
				Log.e(LOG_TAG, "cannot mkdirs: " + dir.getAbsolutePath());
				return;
			}
		}

		final String file = dir.getAbsolutePath() + "/" +
				new SimpleDateFormat("yyyy-MM-dd'T'HH_mm_ss.SSSZZ", Locale.getDefault())
						.format(new Date()) + ".m4a";
		recorder.setOutputFile(file);

		try {
			recorder.prepare();
		} catch (IOException e) {
			Log.e(LOG_TAG, "MediaRecorder.prepare()", e);
		}
		Log.i(LOG_TAG, "start: " + file);
		try {
			recorder.start();
		} catch (Throwable ex) {
			Log.e(LOG_TAG, "MediaRecorder.start()", ex);
		}
		writingFile = file;
		Toast.makeText(this, R.string.record_begin, Toast.LENGTH_SHORT).show();
	}

	private void stopMicRecord() {
		if (recorder != null) {
			recorder.stop();
			recorder.release();
			recorder = null;
			Log.i(LOG_TAG, "stop");
			final String toastText = String.format(Locale.getDefault(),
					getResources().getString(R.string.record_end),
					writingFile);
			writingFile = null;
			Toast.makeText(this, toastText, Toast.LENGTH_SHORT).show();
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
