package com.acorns.acornsmobile;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;

import androidx.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Looper;
import android.util.Log;
import android.view.Window;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.acorns.utilities.Util;

public class LessonHandler extends Thread implements Serializable {
	private final static String TAG = "LessonHandler";

	private final static int BROWSE = 0;
	private final static int SAVE = 1;
	private final static int ASSETS = 2;

	protected AppCompatActivity activity;
	private String fileName;
	private Uri uri;
	private int option;
	private Handler handler;

	private WebPageHandler webHandler;
	protected ProgressBar progress;
	protected TextView textView;

	protected Dialog dialog;

	LessonHandler(AppCompatActivity activity, WebView web) {
		this(activity);
		webHandler = new WebPageHandler(activity, web);
	}

	LessonHandler(AppCompatActivity activity) {
		this.activity = activity;
		webHandler = null;

		dialog = new Dialog(activity);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(R.layout.progress_bar);
		dialog.setCancelable(false);

		progress = dialog.findViewById(R.id.progressBar);
		progress.setProgress(0);

		textView = dialog.findViewById(R.id.textView);
	}

	/**
	 * Obtain a list of all the lessons in the acorns mobile gallery
	 */
	Vector<String> fileList() {
		try {
			File folder = getRootFolder();
			Vector<String> list = new Vector<>();
			return fileList(folder, list);
		} catch (Exception e) {
			return new Vector<>();
		}
	}

	/**
	 * Obtain a list of lesson files in the gallery sub directories
	 *
	 * @param folder Root folder
	 * @param list   ArrayList containing the resulting list of files
	 */
	private Vector<String> fileList(File folder, Vector<String> list) {
		if (folder == null) return new Vector<>();
		File[] listOfFiles = folder.listFiles();
		if (listOfFiles == null) return list;

		String fileName, dirName;

		for (File file : listOfFiles) {
			if (file.isDirectory()) {
				fileName = getWebFile(file);
				if (fileName == null || fileName.isEmpty()) continue;

				String lessonName = getLessonName(fileName);

				dirName = file.getName();
				if (dirName.equals(lessonName)) {
					list.add(fileName);
				}
			}
		}

		// implementing lambda expression
		Collections.sort(list, String::compareToIgnoreCase);
		return list;

	}    // End of directoryList() method

	/**
	 * Method to delete a lesson from the acorns gallery
	 */
	void deleteLesson(File lesson) throws IOException {
		File root = lesson.getParentFile();
		if (root == null) return;

		deleteFileOrDirectory(root);

		File compressed = new File(root.getCanonicalPath() + ".acorns");
		deleteFileOrDirectory(compressed);
	}

	/**
	 * Archive lesson for the gallery
	 *
	 * @return true if save initiated, false otherwise
	 */
	boolean save() {
		File file = getCompressedTempAcornsLessonFile();
		if (!file.exists()) return false;

		option = SAVE;
		start();
		Log.v(TAG, "Save thread started");
		return true;
	}

	/**
	 * Download and display an acorns lesson
	 *
	 * @param uri The URI object describing the picture
	 */
	void download(Uri uri) {
		this.uri = uri;
		this.fileName = null;
		this.handler = new Handler(Looper.getMainLooper());
		option = BROWSE;


		Log.v(TAG, "Uri = " + uri.toString());

		ContentResolver resolver = activity.getContentResolver();
		Cursor cursor = resolver.query(uri, null, null, null, null);

		if (cursor != null) {
			int fileNameColumnIndex = cursor.getColumnIndex("_display_name");
			cursor.moveToFirst();

			if (fileNameColumnIndex != -1) {
				fileName = cursor.getString(fileNameColumnIndex);
			} else {
				String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
				fileName = "Email_" + timeStamp + ".png";
			}
			cursor.close();
		}


		if (fileName == null) {
			fileName = uri.toString();
				Log.i(TAG, "File exists = " + new File(fileName).exists());
				if (!fileName.endsWith("/"))
					fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
		}

		if (fileName.endsWith(".html") || verifyFileName(fileName))
		{
			if (fileName.endsWith(".html")) {
				File file = getCompressedTempAcornsLessonFile();
				if (file.exists()) deleteFileOrDirectory(file);

				String filePath = uri.toString();
				webHandler.showPage(filePath);
				Log.v(TAG, "Web page displayed");
				return;
			} else {
				fileName = fileName.substring(0, fileName.lastIndexOf(".acorns"));
				textView.setText(activity.getString(R.string.downloading, fileName));
				dialog.show();
			}
		} else {
			Util.errorToast(activity, R.string.illegal_file);
			Log.v(TAG, "Illegal file");
			return;
		}

		start();
		Log.v(TAG, "Download thread started");
	}

	/**
	 * Unzip acorns lesson into a temporary folder
	 *
	 * @param zipFile folder containing acorns lessons
	 * @param folder  directory for root of uncompressed Acorns lessons
	 */
	private void unpackAcornsLesson(File zipFile, File folder) {

		byte[] buffer = new byte[1024];

		try {
			deleteFileOrDirectory(folder);
			makeDirectory(folder);

			// Initialize the progress bar
			ZipFile zip = new ZipFile(zipFile);
			final int entries = zip.size();
			zip.close();

			progress.setMax(entries);

			//get the zip file content
			ZipInputStream zis =
					null;
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
				zis = new ZipInputStream(Files.newInputStream(zipFile.toPath()));
			}

			//get the first zipped file list entry
            assert zis != null;
            ZipEntry ze = zis.getNextEntry();

			String fileName;
			File newFile, parent;
			int len, count = 0;
			while (ze != null && !isInterrupted())
			{
				final int status = ++count;

				Runnable handleIt = () ->
						progress.setProgress(status);

				handler.post(handleIt);
				fileName = ze.getName().replace('\\', '/');
				newFile = new File(folder, fileName);

				if (ze.isDirectory()) {
					makeDirectory(newFile);
				} else {
					String parentFile = newFile.getParent();
					if (parentFile == null)
						parentFile = "";

					parent = new File(parentFile);
					makeDirectory(parent);

					FileOutputStream fos = new FileOutputStream(newFile);

					while ((len = zis.read(buffer)) > 0) {
						fos.write(buffer, 0, len);
					}

					fos.close();
				}

				ze = zis.getNextEntry();
			}

			zis.closeEntry();
			zis.close();
			Runnable handleIt = () ->
					progress.setMax(entries);
			handler.post(handleIt);
			if (isInterrupted())
				Log.v(TAG, "Temp lesson creation interrupted");
			else
				Log.v(TAG, "Created temp lesson");

		} catch (IOException ex) {
			Log.v(TAG, ex.toString());
		}
	}

	/**
	 * Create compressed acorn lesson file from stream
	 */
	private File copyFile(BufferedInputStream bis)
			throws IOException {
		File destination = this.getCompressedTempAcornsLessonFile();

		byte[] buffer = new byte[1024];
		DataOutputStream fos
				= null;
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
			fos = new DataOutputStream(Files.newOutputStream(destination.toPath()));
		}

		int len, count = 0;
		final int available = bis.available();
		progress.setProgress(0);
		progress.setMax(available);
		while ((len = bis.read(buffer)) > 0 && !isInterrupted()) {
			count += len;
			final int status = count;

			Runnable postIt = () ->
					progress.setProgress(status);
			handler.post(postIt);
            assert fos != null;
            fos.write(buffer, 0, len);
		}

        assert fos != null;
        fos.close();
		Runnable postIt = () ->
				progress.setProgress(available);
		handler.post(postIt);
		return destination;
	}

	/**
	 * Method to remove a directory that is not empty
	 *
	 * @param file The name of a file or directory
	 */
	private void deleteFileOrDirectory(@NonNull File file) {
		if (!file.exists()) return;

		if (file.isDirectory()) {
			File[] files = file.listFiles();
			if (files != null) {
				for (File c : files)
					deleteFileOrDirectory(c);
			}
		}

		/* Skip files that cannot be deleted */
		if (!file.exists()) return;

		final File to = new File(file.getAbsolutePath() + System.currentTimeMillis());
		if (!file.renameTo(to)) {
			Log.v(TAG, "Couldn't rename " + file.getName() + " to " + to.getName());
		}
		boolean success = to.delete();
		if (!success) {
			String message = String.format("Couldn't delete %s", file.getName());
			Util.messageToast(activity, message);

		}
	}

	private void makeDirectory(File file) {
		if (file.exists()) return;

		boolean success = file.mkdirs();
		if (!success) {
			Log.v(TAG, "Could not create " + file.getName());
		}
	}

	/**
	 * Do the loading/saving of a picture in a background thread
	 */
	public void run() {
		try {
			switch (option) {
				case BROWSE:
					displayLesson();
					break;

				case SAVE:
					saveLesson();
					break;

				case ASSETS:
					if (!copyAssets())
						Toast.makeText(activity, "load lessons failed",
								Toast.LENGTH_SHORT).show();
			}
		} catch (Exception e) {
			Util.exceptionMessage(activity, e);
		}

		if (dialog.isShowing()) dialog.dismiss();
	}

	/**
	 * Download and display a picture
	 */
	private void displayLesson() throws IOException {
		try {
			Log.i(TAG, "display lesson method starts");
			InputStream stream;
			String filePath = uri.toString();
			filePath = filePath.replace("dropbox.", "dl.overconscientious.");
			Log.v(TAG, "filePath = " + filePath);

			if (filePath.startsWith("http"))
			{
				URL url = new URL(filePath);
				Log.v(TAG, "url = " + url);
				HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
				httpConnection.setDoInput(true);
				httpConnection.connect();
				Log.v(TAG, "url connected");
				stream = httpConnection.getInputStream();
			} else {
				stream = activity.getContentResolver().openInputStream(uri);
			}

			if (stream == null)
				throw new FileNotFoundException();

			BufferedInputStream bis = new BufferedInputStream(stream);
			Log.v(TAG, "input stream established");

			// Copy from input stream to a temporary compressed location
			File tempCompressedFile = null;
			if (!isInterrupted())
				tempCompressedFile = copyFile(bis);

			// Unpack the acorns lesson file for display
			if (!isInterrupted() && tempCompressedFile != null) {
				unpackAcornsLesson(tempCompressedFile, getTempFolder());
			}
			stream.close();
			bis.close();
			if (isInterrupted()) {
				Log.v(TAG, "unzip interrupted");
				return;
			} else
				Log.v(TAG, "lesson unzipped");

			Runnable runIt = () ->
			{
				String url = getWebFile(getTempFolder());
				webHandler.showPage(url);
				Log.v(TAG, "Show web page " + url);
			};

			activity.runOnUiThread(runIt);
		} catch (FileNotFoundException fnf) {
			fileName = java.net.URLDecoder.decode(fileName, "UTF-8");
			throw new FileNotFoundException(activity.getString(R.string.file_not_found, fileName));

		} catch (SecurityException e) {
			fileName = java.net.URLDecoder.decode(fileName, "UTF-8");
			throw new SecurityException(activity.getString(R.string.permission_denied, fileName));

		} catch (Error e) {
			fileName = java.net.URLDecoder.decode(fileName, "UTF-8");
			throw new IOException(activity.getString(R.string.couldnt_display) + " " + fileName + ": " + e.getMessage());
		}

		if (isInterrupted()) {
			Log.v(TAG, "display interrupted");
		}
		Log.v(TAG, "display " + fileName + " complete");
	}    // End of display

	/**
	 * Copy assets built-in sample lessons to the application gallery
	 */
	private boolean copyAssets()
	{
		AssetManager assetManager = activity.getAssets();
		String[] assets;
		try {
			assets = assetManager.list("samples");
			for (int i = 0; i < Objects.requireNonNull(assets).length; i++) {
				File destination = new File(getRootFolder(), assets[i]);
				if (destination.exists())
					deleteFileOrDirectory(destination);

				if (!copyAssetFolder("samples/" + assets[i], destination))
					throw new IOException();
			}
			return true;
		} catch (IOException e)
		{
			return false;
		}
	}

	/**
	 * Save image in the application gallery in external storage
	 */
	private void saveLesson() throws IOException {
		// Find the name of the file of lessons
		File lessonRoot = getTempFolder();
		String webFile = getWebFile(lessonRoot);
		if (webFile == null || webFile.length() <= ".html".length())
			throw new IOException(activity.getString(R.string.lesson_not_found));

		// Set the acorns root lesson folder in the gallery
		String newLessonName = getLessonName(webFile);
		File destination = new File(getRootFolder(), newLessonName);
		if (destination.exists())
			deleteFileOrDirectory(destination);

		boolean success = lessonRoot.renameTo(destination);
		if (!success) {
			String message
					= "Couldn't rename " + lessonRoot.getName() + " to " + destination.getName();
			Util.messageToast(activity, message);
		}

		File newWebFile = new File(destination, newLessonName + ".html");
		Uri uri = Uri.fromFile(newWebFile);
		webHandler.showPage(uri.toString());
	}

	/** Copy assets acorns lesson folder to the acorns gallery */
	private boolean copyAssetFolder(String srcName, File destination) {
		try {
			boolean result;
			String[] fileList = activity.getAssets().list(srcName);
			if (fileList == null) return false;

			if (fileList.length == 0) {
				result = copyAssetFile(srcName, destination);
			} else {
				result = destination.mkdirs();
				for (String filename : fileList) {
					String dstName = destination.getAbsolutePath();
					File newDest = new File(dstName + File.separator + filename);
					result &= copyAssetFolder(srcName + File.separator + filename, newDest);
				}
			}
			return result;
		} catch (IOException e) {
			return false;
		}
	}

	/** Copy a single file from an Acorns lesson to the acorns gallery */
	@SuppressWarnings("all")
	private boolean copyAssetFile(String srcName, File outFile) {
		try {
			InputStream in = activity.getAssets().open(srcName);
			OutputStream out = new FileOutputStream(outFile);
			byte[] buffer = new byte[1024];
			int read;
			while ((read = in.read(buffer)) != -1) {
				out.write(buffer, 0, read);
			}
			in.close();
			out.close();
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * Get the URI string for the web page link to the current lesson
	 *
	 * @param folder The folder containing the web page
	 * @return The canonical URI to the web page link
	 * Returns "" if no web page was found
	 */
	private String getWebFile(File folder) {
		try {
			File[] listOfFiles = folder.listFiles();
			if (listOfFiles == null) return null;

			String path;
			for (File file : listOfFiles) {
				if (file.isDirectory()) continue;
				path = file.getName();
				if (path.endsWith(".html"))
					return "file://" + file.getCanonicalPath();
			}
			return "";
		} catch (IOException e) {
			return "";
		}
	}


	/**
	 * Get root directory for creating and accessing files in the gallery
	 */
	File getRootFolder() {
		File rootFile;
		if (isInternalStorage()) {
			rootFile = activity.getFilesDir();
		} else {
			rootFile = activity.getExternalFilesDir(null);
		}

		File file = new File(rootFile, File.separator);
		if (!file.exists()) {
			boolean result = file.mkdirs();
			if (!result)
				Log.v(TAG, "Couldn't make directory " + rootFile);
		}
		return rootFile;
	}

	/**
	 * Determine if application is to write to internal storage
	 */
	public boolean isInternalStorage() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
		String key = activity.getString(R.string.pref_storage_key);
		String defaultValue = activity.getString(R.string.pref_storage_default_value);
		String value = prefs.getString(key, defaultValue);
		Log.v(TAG, "Storage preference key/value = " + key + "/" + value);

		if (value.equals("true"))
		{
			String state = Environment.getExternalStorageState();
			if (!(Environment.MEDIA_MOUNTED.equals(state))) // available for reading and writing
			{

				return false;
			}
		}
		return value.equals("false");
	}

	/**
	 * Get the name of temporary folder/file names of acorns lessons
	 */
	private String getTempFileName()
	{
		return activity.getString(R.string.acorns_temp);
	}

	/**
	 * Get the File object for temporary storage of acorns lessons
	 */
	private File getTempFolder() {
		File tempFile = new File(getRootFolder(), getTempFileName());
		if (!tempFile.exists()) {
			if (!tempFile.mkdirs()) {
				Log.v(TAG, "Could not create " + tempFile.getName());
			}
		}
		return tempFile;
	}

	/**
	 * Get the File object for compressed acorns lessons
	 */
	private File getCompressedTempAcornsLessonFile() {
		File folder = getRootFolder();
		return new File(folder, getTempFileName() + ".acorns");
	}

	/**
	 * Get name of current lesson
	 *
	 * @param fileName Path to HTML file
	 * @return The name of the lesson
	 */
	private String getLessonName(String fileName) {
		int startOffset = fileName.lastIndexOf("/") + 1;
		int lastOffset = fileName.lastIndexOf(".html");
		return fileName.substring(startOffset, lastOffset);
	}

	/** Verify file is one that Acorns can process
	 *
	 * @param name The name of the file to be processed
	 * @return true if value, false otherwise
	 */
	public boolean verifyFileName(String name)
	{
		if (name==null) return false;
		boolean suffixWithparens = matchSuffix(name, activity.getResources().getString(R.string.regularFileNameSuffix));
		boolean suffixAcornsZip = name.endsWith(activity.getResources().getString(R.string.fileNamesuffix));
		boolean normalEnding = name.endsWith(".acorns");
		return (normalEnding || suffixWithparens || suffixAcornsZip);
	}
	/** Method to check for a matching regular expression suffix
	 *
	 * @param text The full text to match the suffix
	 * @param suffix A regular expression of the suffix
	 * @return true if match, false otherwise
	 */
	private static boolean matchSuffix(String text, String suffix)
	{
		if (text == null || suffix == null) {
			return false;
		}
		String regex = ".*" + suffix + "$";
		return text.matches(regex);
	}

	public void copySampleLessons() throws InterruptedException {
		option = ASSETS;
		start();
		join();
	}
}