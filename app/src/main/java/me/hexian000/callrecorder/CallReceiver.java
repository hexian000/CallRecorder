package me.hexian000.callrecorder;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.nio.file.Paths;

import static me.hexian000.callrecorder.CallRecorder.LOG_TAG;

public class CallReceiver extends BroadcastReceiver {
	private static String lastState = "";
	private static MediaRecorder recorder = null;

	private static String getContactDisplayNameByNumber(final ContentResolver resolver,
	                                                    final String number) {
		final Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
				Uri.encode(number));
		try (final Cursor contactLookup = resolver.query(uri, new String[]{
				BaseColumns._ID, ContactsContract.PhoneLookup.DISPLAY_NAME
		}, null, null, null)) {
			if (contactLookup != null && contactLookup.getCount() > 0) {
				contactLookup.moveToNext();
				return contactLookup.getString(contactLookup.getColumnIndex(
						ContactsContract.Data.DISPLAY_NAME));
			}
		}

		return null;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		if (!TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(intent.getAction())) {
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

		if (start) {
			try {
				startRecording(context, number);
			} catch (Exception ex) {
				if (recorder != null) {
					recorder.reset();
					recorder.release();
					recorder = null;
				}
				Log.e(LOG_TAG, "unexpected exception: ", ex);
			}
		} else {
			stopRecording(context);
		}
	}

	private void startRecording(final Context context, final String number) throws IOException {
		if (recorder != null) {
			Log.w(LOG_TAG, "startRecording when already recording");
			return;
		}

		recorder = new MediaRecorder();
		recorder.setAudioChannels(1);
		recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_CALL);
		recorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_WB);
		recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_WB);

		final String dirPath = Utils.makePath(Paths.get(
				Environment.getExternalStorageDirectory().getAbsolutePath(),
				"CallRecorder"
		).toString());

		final String file;
		{
			String name = Utils.nowISO8601() + "_" + number;

			final String contact = getContactDisplayNameByNumber(context.getContentResolver(),
					number);
			if (contact != null) {
				name += "_" + contact;
			}

			name += ".amr";
			file = Paths.get(dirPath, Utils.sanitizeFileName(name)).toString();
		}
		recorder.setOutputFile(file);

		recorder.setOnErrorListener((mr, what, extra) ->
				Log.e(LOG_TAG, "MediaRecorder.onError " + what + " " + extra));
		recorder.setOnInfoListener((mr, what, extra) ->
				Log.i(LOG_TAG, "MediaRecorder.onInfo " + what + " " + extra));

		try {
			recorder.prepare();
		} catch (IOException e) {
			Log.e(LOG_TAG, "MediaRecorder.prepare()", e);
		}

		Log.i(LOG_TAG, "start: " + file);
		for (int retry = 0; ; retry++) {
			try {
				recorder.start();
				recorder.getMaxAmplitude();
				break;
			} catch (Exception ex) {
				Log.e(LOG_TAG, "MediaRecorder.start()", ex);
			}
			if (retry < 3) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException ex) {
					Log.w(LOG_TAG, "Interrupted", ex);
					return;
				}
			} else {
				Log.e(LOG_TAG, "MediaRecorder.start() failed over 3 times");
				return;
			}
		}
		Toast.makeText(context, R.string.record_begin, Toast.LENGTH_SHORT).show();
	}

	private void stopRecording(final Context context) {
		if (recorder == null) {
			Log.w(LOG_TAG, "stopRecording when not recording");
			return;
		}
		recorder.stop();
		final int maxAmplitude = recorder.getMaxAmplitude();
		if (maxAmplitude > 0) {
			Log.i(LOG_TAG, "stop, maxAmplitude=" + maxAmplitude);
		} else {
			Log.w(LOG_TAG, "stop, maxAmplitude=" + maxAmplitude);
		}
		recorder.release();
		recorder = null;
		Toast.makeText(context, context.getResources().getString(R.string.record_end), Toast.LENGTH_SHORT).show();
	}
}
