package org.acorns.utilities;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.style.AbsoluteSizeSpan;
import android.util.Log;
import android.widget.Toast;
import android.app.AlertDialog;

import androidx.appcompat.app.AppCompatActivity;

public class Util
{
	private static final String TAG = "Util";
	
	private static String getString(AppCompatActivity activity, int id)
	{
		return activity.getString(id);
	}

	/** Output toast message in response to a thread occurring exception */
	public static void exceptionToast(final AppCompatActivity activity, final Exception exception)
	{
		Runnable erun = () -> {
			String message = exception.getMessage();
			if (message == null || message.length() == 0)
				message = exception.toString();

			makeToast(activity, message, true);
		};

		activity.runOnUiThread(erun);
	}

	public static void exceptionMessage(final AppCompatActivity activity, final Exception exception)
	{
		Runnable erun = () -> {
			String message = exception.getMessage();
			if (message == null || message.length() == 0)
				message = exception.toString();

			makeMessage(activity, message, true);
		};
		activity.runOnUiThread(erun);
	}
	
	/** Output an error message from a thread */
	public static void errorToast(AppCompatActivity activity, int id)
	{
		messageToast(activity, getString(activity, id));
	}
	
	public static void messageToast(final AppCompatActivity activity, final String message)
	{
		Runnable erun = () -> makeToast(activity, message, false);
		activity.runOnUiThread(erun);
	}

	private static void makeToast(AppCompatActivity activity, String message, boolean e)
	{
		if (message==null) message = "unknown message";
		SpannableStringBuilder biggerText = new SpannableStringBuilder(message);
		biggerText.setSpan(new AbsoluteSizeSpan(24, true), 0, message.length(), 0);
		Context context = activity.getApplicationContext();
        Toast toast = Toast.makeText(context, biggerText, Toast.LENGTH_LONG);
 		toast.show();

        if (e) Log.e(TAG, message);
        else Log.v(TAG, message);
	}

	public static void makeMessage(AppCompatActivity activity, String message, boolean e)
	{
		new AlertDialog.Builder(activity)
				.setTitle("Acorns message")
				.setMessage(message)
				.setPositiveButton("OK", null)
				.show();

		if (e) Log.e(TAG, message);
		else Log.v(TAG, message);
	}

}   // End of Util class
