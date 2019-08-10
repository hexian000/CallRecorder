package me.hexian000.callrecorder;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

class Utils {
	@NonNull
	private static String makePath(@NonNull final String path) throws IOException {
		final File dir = new File(path);
		if (!dir.exists()) {
			if (!dir.mkdirs()) {
				throw new IOException("cannot mkdirs: " + dir.getAbsolutePath());
			}
		}
		return dir.getAbsolutePath();
	}

	@NonNull
	private static String sanitizeFileName(@NonNull final String desiredName) {
		return desiredName.replaceAll("[\\\\/:*?\"<>|]", "_");
	}

	@NonNull
	private static String nowISO8601() {
		return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZ", Locale.getDefault())
				.format(new Date());
	}

	@NonNull
	static String formatDuration(long millis) {
		final long seconds = millis / 1000;
		if (seconds < 3600) {
			return String.format(Locale.getDefault(), "%02d:%02d",
					seconds / 60, seconds % 60);
		}
		return String.format(Locale.getDefault(), "%d:%02d:%02d",
				seconds / 3600, (seconds % 3600) / 60, seconds % 60);
	}

	@Nullable
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

	@NonNull
	private static String makeCallRecFileName(@Nullable final String contact,
	                                          @NonNull final String number) {
		String name = Utils.nowISO8601() + "_" + number;
		if (contact != null) {
			name += "_" + contact;
		}
		name += ".amr";
		return name;
	}

	@NonNull
	static String makeCallFilePath(final Context context, @NonNull final String number)
			throws IOException {
		final String dirPath = makePath(Paths.get(
				Environment.getExternalStorageDirectory().getAbsolutePath(),
				"CallRecorder"
		).toString());
		final ContentResolver resolver = context.getContentResolver();
		final String displayName = getContactDisplayNameByNumber(resolver, number);
		final String fileName = sanitizeFileName(makeCallRecFileName(displayName, number));
		return Paths.get(dirPath, fileName).toString();
	}

	@NonNull
	static String makeMicFilePath() throws IOException {
		final String dirPath = makePath(Paths.get(
				Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
				           .getAbsolutePath(), "Recorder").toString());
		final String fileName = sanitizeFileName(nowISO8601() + ".m4a");
		return Paths.get(dirPath, fileName).toString();
	}
}
