package me.hexian000.callrecorder;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

class Utils {
	static String makePath(final String path) throws IOException {
		final File dir = new File(path);
		if (!dir.exists()) {
			if (!dir.mkdirs()) {
				throw new IOException("cannot mkdirs: " + dir.getAbsolutePath());
			}
		}
		return dir.getAbsolutePath();
	}

	static String sanitizeFileName(final String desiredName) {
		return desiredName.replaceAll("[\\\\/:*?\"<>|]", "_");
	}

	static String nowISO8601() {
		return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZ", Locale.getDefault())
				.format(new Date());
	}
}
