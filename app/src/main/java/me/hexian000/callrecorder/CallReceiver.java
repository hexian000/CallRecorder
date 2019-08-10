package me.hexian000.callrecorder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import static me.hexian000.callrecorder.CallRecorder.LOG_TAG;

public class CallReceiver extends BroadcastReceiver {
	private static String lastState = "";

	@Override
	public void onReceive(Context context, Intent intent) {
		if (!TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(intent.getAction())) {
			return;
		}
		if (!intent.hasExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)) {
			return;
		}

		final String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
		if (lastState.equals(state)) {
			return;
		}
		lastState = state;

		final String number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
		final boolean start = TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state);
		Log.d(LOG_TAG, "PHONE_STATE " + state + " " + number + " " + start);

		final CallRecorder app = (CallRecorder) context.getApplicationContext();
		final Intent i = new Intent(app, AudioRecordService.class);
		i.putExtra(AudioRecordService.EXTRA_NUMBER, number);
		if (start) {
			if (app.isRecording()) {
				Toast.makeText(context, R.string.record_busy, Toast.LENGTH_LONG).show();
				return;
			}
			context.startForegroundService(i);
		} else {
			context.stopService(i);
		}
	}
}
