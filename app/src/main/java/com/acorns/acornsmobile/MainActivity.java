package com.acorns.acornsmobile;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.GridView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.Vector;

import org.acorns.utilities.Util;

public class MainActivity extends AppCompatActivity
{
	private final static String TAG = "AcornsMobile";
	
	private Vector<String> files;
	private GridView gridView;
	private LessonAdapter adapter;
	private LessonHandler handler;

	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);

		getWindow().requestFeature(Window.FEATURE_ACTION_BAR);

		AudioManager audiomanager= (AudioManager) getSystemService(AUDIO_SERVICE);
		float actualVolume = 15;
		float maxVolume = 15;
		if (audiomanager!=null)
        {
            actualVolume = (float) audiomanager.getStreamVolume (AudioManager.STREAM_MUSIC);
            maxVolume= (float) audiomanager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        }
		Log.v(TAG, actualVolume + "/" + maxVolume);
		setContentView(R.layout.activity_main);
		
        // Initialize default preference first time
		PreferenceManager.setDefaultValues(this, R.xml.options, false);

        gridView = findViewById(R.id.gridview);
        
		int columns = 1;
		gridView.setNumColumns(columns);
		handler = new LessonHandler(this);
		files = handler.fileList();

		// If nothing to display, give user the option to load samples.
		if (files.isEmpty())
		{
			Intent myIntent = new Intent(this, SampleActivity.class);
			this.sampleActivityResultLauncher.launch(myIntent);
		}
		
        // Display the gallery
		int fontSize = getGridPreference();
        adapter = new LessonAdapter(this, files, fontSize);
        gridView.setAdapter(adapter);

		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		ActionBar actionBar = getSupportActionBar();
		assert actionBar != null;
		actionBar.setIcon(R.mipmap.ic_launcher);
		actionBar.setDisplayUseLogoEnabled(true);
		actionBar.setDisplayShowCustomEnabled(true);
		actionBar.setDisplayShowTitleEnabled(true);
		actionBar.setDisplayShowHomeEnabled(true);
		actionBar.show();
 	}

	@Override
    public void onConfigurationChanged(@NonNull Configuration newConfig)
	{
        super.onConfigurationChanged(newConfig);

       // setContentView(R.layout.activity_main);
        gridView = findViewById(R.id.gridview);

        int columns = 1;
        gridView.setNumColumns(columns);

        if (handler==null)
			handler = new LessonHandler(this);

        files = handler.fileList();

        // Display the gallery
		int fontSize = getGridPreference();
        adapter = new LessonAdapter(this, files, fontSize);
        gridView.setAdapter(adapter);
    }


	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);

		MenuItem item = menu.getItem(0);
		Log.d(TAG, "set flag for " + item.getTitle());
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item)
	{
		int selected;
    	try
    	{
			int itemId = item.getItemId();
			if (itemId == R.id.action_discard)
			{
				selected = adapter.getSelectedLesson();
				if (selected < 0) {
					Util.errorToast(this, R.string.nothing_selected);
				} else {
					vibrate(600);
					String path = files.get(selected);
					path = path.substring("file://".length());
					File file = new File(path);

					LessonHandler handler = new LessonHandler(this);
					handler.deleteLesson(file);
					files.remove(selected);
					updateAdapter();
				}
				return true;
			} else if (itemId == R.id.action_settings)
			{
				getSupportFragmentManager()
						.beginTransaction()
						.addToBackStack("settings")
						.replace(android.R.id.content, new PreferenceOptionsFragment())
						.commit();
				return true;
			}
			else if (itemId == R.id.action_display)
			{
				selected = adapter.getSelectedLesson();
				if (selected < 0) {
					Util.errorToast(this, R.string.nothing_selected);
				} else {
					Log.v(TAG, "Display");
					vibrate(500);
					String lesson = files.get(selected);
					Uri lessonUri = Uri.parse(lesson);
					Intent shareIntent = new Intent();
					shareIntent.setAction(Intent.ACTION_SEND);
					shareIntent.putExtra(Intent.EXTRA_STREAM, lessonUri);
					shareIntent.setType("application/octet-stream");
					shareIntent.putExtra("Main", true);

					ComponentName component = new ComponentName(this, com.acorns.acornsmobile.BrowserActivity.class);
					shareIntent.setComponent(component);
					startActivity(shareIntent);
				}
				return true;
			}
			else if (itemId == R.id.action_email)
			{
				Intent emailIntent =
						Intent.makeMainSelectorActivity(
								Intent.ACTION_MAIN,
								Intent.CATEGORY_APP_EMAIL);
				emailIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
				emailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(emailIntent);
				finish();
				return true;
			}
			else if (itemId == R.id.action_web_site) {
				Uri uriUrl = Uri.parse("http://google.com/");
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, uriUrl);
				browserIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
				browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(browserIntent);
				finish();
				return true;
			}
			else {
	            return super.onOptionsItemSelected(item);
		    }
    	}
    	catch (Exception e)
    	{
           	Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
    	}
    	return true;
    }

	/** Vibrate the phone for the length of time specified (milliseconds) **/
	@SuppressWarnings({"deprecation", "RedundantSuppression"})
	private void vibrate(int length)
	{
		Vibrator vibrator;

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)  // S is SDK 31
		{
			Log.v(TAG, "vibration");

			VibratorManager vibratorManager = (VibratorManager) getSystemService(VIBRATOR_MANAGER_SERVICE);
			vibrator = vibratorManager.getDefaultVibrator();
			vibrator.vibrate(VibrationEffect.createOneShot(length, VibrationEffect.DEFAULT_AMPLITUDE));
		}
		else
		{
			vibrator = (Vibrator) (getSystemService(VIBRATOR_SERVICE));

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)  // Oreo is SDK 26
			{
				vibrator.vibrate(VibrationEffect.createOneShot(length, VibrationEffect.DEFAULT_AMPLITUDE));
			} else {
				vibrator.vibrate(length);
			}
		}
	}

	/** Get number of fontSize for grid items */
	private int getGridPreference()
	{
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String key = getResources().getString(R.string.pref_size_key);
        String defaultValue = getResources().getString(R.string.pref_size_default_value);
       	String value = prefs.getString(key, defaultValue);
       	Log.v(TAG, "Size preference key/value = " + key + "/" + value);
       	try
       	{
       		return Integer.parseInt(value, 10);
       	}
       	catch (Exception e)
       	{
       		return Integer.parseInt(defaultValue, 10);
       	}
    }

    /** Force the gridView to redisplay */
    public void refreshAdapter()
    {
    	adapter.notifyDataSetChanged();
    	gridView.invalidate();
    }
    
    public void updateAdapter()
    {
		refreshAdapter();
		int fontSize = getGridPreference();
        //gridView.setNumColumns(1);
        adapter.updateDisplay(fontSize, files);
    }

    public void updateFileList()
    {
		handler = new LessonHandler(this);
		files = handler.fileList();

		// Display the gallery
		int fontSize = getGridPreference();
		adapter = new LessonAdapter(this, files, fontSize);
		gridView.setAdapter(adapter);
    }

	/** Launch and respond when the sample activity completes */
	ActivityResultLauncher<Intent> sampleActivityResultLauncher = registerForActivityResult(
			new ActivityResultContracts.StartActivityForResult(),
			new ActivityResultCallback<ActivityResult>() {
				public void onActivityResult(ActivityResult result) {
					int flag = result.getResultCode();
					if (flag==RESULT_CANCELED)
					{
						Toast.makeText(getApplicationContext(), "Could not load sample lessons", Toast.LENGTH_LONG).show();
						return;
					}
					else {
						updateFileList();
					}
				}
			});
}	// End of MainActivity class
