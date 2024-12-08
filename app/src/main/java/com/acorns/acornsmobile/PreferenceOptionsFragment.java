package com.acorns.acornsmobile;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Color;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import java.util.Objects;

public class PreferenceOptionsFragment extends PreferenceFragmentCompat
		implements OnSharedPreferenceChangeListener
{

	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey)
	{
		addPreferencesFromResource(R.xml.options);

		String TAG = "Preference";
		Log.v(TAG, "preferences");
	}

	@SuppressLint("ResourceAsColor")
	@NonNull
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater
			, ViewGroup container, Bundle savedInstanceState)
	{
	    View view = super.onCreateView(inflater, container, savedInstanceState);
        int color = ContextCompat.getColor(view.getContext(), R.color.light);
		view.setBackgroundColor(color);


		ViewGroup.LayoutParams layout
	    	= new ViewGroup.LayoutParams(
	    			ViewGroup.LayoutParams.MATCH_PARENT,
	    			ViewGroup.LayoutParams.WRAP_CONTENT);
	    view.setLayoutParams(layout);

		return view;
	}

	   @Override
	    public void onResume()
	   {
	       super.onResume();
	       // Listen for changes
		   PreferenceScreen preferenceScreen = Objects.requireNonNull(getPreferenceScreen());
		   Objects.requireNonNull(preferenceScreen.getSharedPreferences()).registerOnSharedPreferenceChangeListener(this);

		}

	    @Override
	    public void onPause()
	    {
	        super.onPause();
	        // Stop listening for changes
	        Objects.requireNonNull(getPreferenceScreen()
					.getSharedPreferences()).unregisterOnSharedPreferenceChangeListener(this);
	    }

	    @Override
	    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
	    {
	        MainActivity activity = (MainActivity)getActivity();
	    	if (activity!=null)
			{
				activity.updateFileList();
				activity.onBackPressed();
			}
	    }
}
