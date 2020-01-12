package me.hexian000.callrecorder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.File;

import static me.hexian000.callrecorder.CallRecorder.LOG_TAG;

public class DeleteReceiver extends BroadcastReceiver {
	public static final String EXTRA_PATH = "path";

	@Override
	public void onReceive(Context context, Intent intent) {
		final String path = intent.getStringExtra(EXTRA_PATH);
		if (path == null) {
			return;
		}
		try {
			final File f = new File(path);
			if (!f.delete()) {
				Log.e(LOG_TAG, "delete file failed: " + path);
			}
		} catch (SecurityException ex) {
			Log.e(LOG_TAG, "delete file exception: " + path, ex);
		}
	}
}
