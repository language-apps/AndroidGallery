package com.acorns.acornsmobile;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.acorns.utilities.Util;

import java.io.File;

public class BrowserActivity extends AppCompatActivity
{
    public static String TAG = "Browser Activity";

    WebView web;
    Menu menu;
    boolean hideSaveButton = false;
    boolean main = false;
    LessonHandler handler;
    Uri uri;

    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.browser);
        Toolbar toolbar = findViewById(R.id.browserToolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setIcon(R.mipmap.ic_launcher);
        actionBar.setDisplayUseLogoEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.show();

        web = findViewById(R.id.webView);
        handler = new LessonHandler(this, web);

        try
        {
            Intent intent = getIntent();
            String action = intent.getAction();
            String type = intent.getType();
            String scheme = intent.getScheme();
            Log.v(TAG, "Scheme = " + scheme);

            main = intent.getBooleanExtra("Main",  false);


            if (action!=null && action.equals(Intent.ACTION_SEND))
            {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                {
                    uri = getIntent().getParcelableExtra(Intent.EXTRA_STREAM, Uri.class);
                } else
                {
                    uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                }

                if (uri!=null && type!=null && type.equals("text/plain"))
                {
                    String urlPath = intent.getStringExtra(Intent.EXTRA_TEXT);
                    uri = Uri.parse(urlPath);
                }

                /* Otherwise either
                 *    application/octetStream or
                 *    application/zip
                 *    application/x-zip
                 */
            }
            else
            {
                uri = intent.getData();
            }

            File root = handler.getRootFolder();

            hideSaveButton = false;
            String uriPath;
            if (uri != null)
            {
                uriPath = uri.getPath();
                if (uriPath==null)
                    throw new NullPointerException("Null URL");
                else if (!(uriPath.endsWith(".acorns")
                        || uriPath.endsWith(".html")))
                {
                    String name = getContentName(getContentResolver(), uri);
                    boolean validFileName = handler.verifyFileName(name); //matchSuffix(name, "acorns *\\(([^()]+)\\) *\\.zip");
                    if (name==null || !validFileName)

                       throw new IllegalArgumentException
                               ("Illegal file type: " + name);
                }
                else if (uriPath.startsWith(root.getPath()))
                    hideSaveButton = true;

                handler.download(uri);
            }
        }
        catch (Exception e)
        {
            Util.makeMessage(this, e.getMessage(), true);
        }
    }

    /** Get file name from URI
     *
     * @param resolver Content resolver
     * @param uri URI passed from the intent
     * @return The URI file name
     */
    public static String getContentName(ContentResolver resolver, Uri uri)
    {
        Cursor cursor = resolver.query(uri, null, null, null, null);
        if (cursor==null) return null;
        cursor.moveToFirst();
        int nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
        String name = null;
        if (nameIndex >= 0) {
            name = cursor.getString(nameIndex);
        }
        cursor.close();
        return name;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        handler.interrupt();
        try
        {
            handler.join();
        }
        catch (InterruptedException e)
        {
            Log.v(TAG, " Interrupted");
            return;
        }
        Log.v(TAG, "Destroy Complete");
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.browser, menu);
        this.menu = menu;
        if (hideSaveButton)
            hide();

        return true;
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item)
    {
        try
        {
            int itemId = item.getItemId();
            if (itemId == R.id.action_save)
            {
                LessonHandler handler = new LessonHandler(this, web);

                if (!handler.save()) {
                    Util.errorToast(this, R.string.cannot_save);
                    return true;
                }

                vibrate();
                hide();
                return true;
            }
            else if (itemId == R.id.action_launch) {
                if (main) finish();
                else {
                    Intent intent = new Intent(this, com.acorns.acornsmobile.MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
                return true;
            }
            else
            {
                    return super.onOptionsItemSelected(item);
            }
        }
        catch (Exception e)
        {
            Util.makeMessage(this, e.toString(), true);
        }
        return true;
    }

    /** Vibrate the phone for the length of time specified (milliseconds) **/
    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    private void vibrate()
    {
        Vibrator vibrator;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)  // S is SDK 31
        {
            VibratorManager vibratorManager = (VibratorManager) getSystemService(VIBRATOR_MANAGER_SERVICE);
            vibrator = vibratorManager.getDefaultVibrator();
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
        }
        else
        {
            vibrator = (Vibrator) (getSystemService(VIBRATOR_SERVICE));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)  // Oreo is SDK 26
            {
                vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(500);
            }
        }
    }

    public void hide()
    {
        if (menu!=null)
        {
            MenuItem item = menu.getItem(0);
            item.setVisible(false);
        }
    }

}	// End of BrowserActivity class
