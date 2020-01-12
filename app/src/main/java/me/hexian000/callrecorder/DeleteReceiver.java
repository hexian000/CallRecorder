package me.hexian000.callrecorder;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.File;

import static android.content.Context.NOTIFICATION_SERVICE;
import static me.hexian000.callrecorder.CallRecorder.LOG_TAG;

public class DeleteReceiver extends BroadcastReceiver {
	public static final String EXTRA_START_ID = "startId";
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
				return;
			}
			final NotificationManager notificationManager =
					(NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
			final int startId = intent.getIntExtra(EXTRA_START_ID, 0);
			notificationManager.cancel(startId);
		} catch (Exception ex) {
			Log.e(LOG_TAG, "DeleteReceiver exception: " + path, ex);
		}
	}
}
