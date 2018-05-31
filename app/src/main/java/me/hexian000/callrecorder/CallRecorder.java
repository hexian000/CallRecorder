package me.hexian000.callrecorder;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.util.Log;

public class CallRecorder extends Application {
	public static final String LOG_TAG = CallRecorder.class.getSimpleName();
	public MediaRecorder mediaRecorder = null;

	public boolean isEnabled() {
		final Context context = getApplicationContext();
		final ComponentName receiverName = new ComponentName(context, CallReceiver.class);
		return PackageManager.COMPONENT_ENABLED_STATE_ENABLED == context
				.getPackageManager()
				.getComponentEnabledSetting(receiverName);
	}

	public void setEnabled(boolean enabled) {
		final Context context = getApplicationContext();
		final ComponentName receiverName = new ComponentName(context, CallReceiver.class);
		final PackageManager pm = context.getPackageManager();
		pm.setComponentEnabledSetting(receiverName,
				enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
						PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
				PackageManager.DONT_KILL_APP);

		if (!enabled && mediaRecorder != null) {
			mediaRecorder.stop();
			mediaRecorder.release();
			mediaRecorder = null;
			Log.i(LOG_TAG, "stop due to disable");
		}
	}
}
