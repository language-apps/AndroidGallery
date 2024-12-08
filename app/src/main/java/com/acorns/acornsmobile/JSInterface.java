package com.acorns.acornsmobile;

import java.io.File;
import java.util.Hashtable;
import java.util.Set;

import android.util.Log;
import android.webkit.JavascriptInterface;
import android.os.Process;

import org.acorns.data.AudioData;
import org.acorns.data.AudioFeatures;
import org.acorns.language.AudioCompare;
import org.acorns.language.SpellCheck;
import org.acorns.utilities.Util;
import org.json.JSONArray;

import androidx.appcompat.app.AppCompatActivity;

public class JSInterface 
{
	public final static String TAG = "JSInterface";

	// Time to wait till interrupted thread terminates (milliseconds)
	private final static int JOIN_WAIT = 0;
	
	// Time to wait till cluster assignments ready (milliseconds)
	private final static int CLUSTER_WAIT = 3000;

	// Audio compare result codes
	private final static int EXCEPTION = -4;
	private final static int NO_FILE   = -3;
	private final static int NO_DATA   = -2;
	private final static int LOADING   = -1;
	private final static int INCORRECT =  0;
	private final static int CLOSE     =  1;
	private final static int CORRECT   =  2;
	
	private final static double CORRECT_DISTANCE = 0.35;
	private final static double CLOSE_DISTANCE = 0.45;
	
    private AudioData audio;
    private final String lessonFolder;
    private final AppCompatActivity activity;
    
    private final Hashtable<String, ExtractFeatures> audioThreads;
    private ExtractFeatures extractRecordingFeaturesThread;

    /** Instantiate the interface and set the context */
    JSInterface(AppCompatActivity activity, String lessonFolder)
    {
    	this.activity = activity;
    	this.lessonFolder = lessonFolder;
    	    	
        if (audio==null) 
        	audio = new AudioData(activity);
        
        audioThreads = new Hashtable<>();
    }

	@JavascriptInterface
    @SuppressWarnings("unused")
    public void recordAudio()
	{
		try
		{
			killExtractRecordingThread();
			audio.recordAudio();
			
		}
		catch (Exception e) 
		{
			Util.exceptionToast(activity, e);
		}
	}
	
	@JavascriptInterface
    @SuppressWarnings("unused")
	public void playAudio()
	{
		try
		{
			stopAudioThread();
			audio.playAudio();
		}
		catch (Exception e)
		{
			Util.exceptionToast(activity, e);
		}
	}
	
	@JavascriptInterface
    @SuppressWarnings("unused")
    public void stopAudio()
    {
        stopAudioThread();
    }

	private void stopAudioThread()
	{
		try
		{
			audio.stopAudio();
			
			if (audioThreads.isEmpty()) return;
			if (!audio.isRecorded()) return;
			
			if (extractRecordingFeaturesThread!=null)
			{
				if (extractRecordingFeaturesThread.isAlive()) return;
				if (extractRecordingFeaturesThread.getClusterAssignments() != null) return;
			}
			
			extractRecordingFeaturesThread = new ExtractFeatures(audio);
			extractRecordingFeaturesThread.start();
		}
		catch (Exception e) 
		{
			Util.exceptionToast(activity, e);
		}
	}
	
	@JavascriptInterface
    @SuppressWarnings("unused")
    public void loadAudioFiles(String jsonData)
	{
		if (activity instanceof MainActivity) return;
		
		try
		{
			JSONArray data = new JSONArray(jsonData);
			int length = data.length();
			String jsAudio;
			
			audioThreads.clear();
			ExtractFeatures extract;
			for (int i=0; i<length; i++)
			{
				jsAudio = data.getString(i);
				extract = new ExtractFeatures(jsAudio);
				audioThreads.put(jsAudio, extract);
				extract.start();
			}
		}
		catch (Exception e)
		{
			Util.exceptionToast(activity, e);
		}
	}
	
	@JavascriptInterface
    @SuppressWarnings("unused")
    public void reset()
	{
		if (activity instanceof MainActivity) return;
		
		Set<String> keySet = audioThreads.keySet();
		int size = keySet.size();
		
		String[] keys = new String[size];
		keys = keySet.toArray(keys);
		
		String key;
		ExtractFeatures thread;
		for (int  i=0; i<size; i++)
		{
			Log.i(TAG, "Interrupting thread " + (i+1) + " of " + size);
			try
			{
				key = keys[i];
				thread = audioThreads.get(key);
				
				if (thread!=null && thread.isAlive())
				{
					thread.interrupt();
					thread.join(JOIN_WAIT);
					if (thread.isAlive()) 
						throw new Exception("Thread is still alive");
				}
			}
			catch (Exception e)
			{
				Util.exceptionToast(activity, e);
			}
		}
		
		audioThreads.clear();
		killExtractRecordingThread();
		
		audio.stopAudio();
		audio.clearTimeDomain();
	}
	
	private void killExtractRecordingThread()
	{
		try
		{
			if (extractRecordingFeaturesThread!=null
					&& extractRecordingFeaturesThread.isAlive())
			{	
				extractRecordingFeaturesThread.interrupt();
				extractRecordingFeaturesThread.join(JOIN_WAIT);
			}
		}
		catch (Exception e)
        {
            Util.exceptionToast(activity, e);
        }
		extractRecordingFeaturesThread = null;
	}
	
	@JavascriptInterface
    @SuppressWarnings("unused")
    public void outLog(String message)
	{
		Log.e(TAG, message);
	}
	
	@JavascriptInterface
    @SuppressWarnings("unused")
    public int compare(String json)
	{
		stopAudioThread();
		if (!audio.isRecorded()) return NO_DATA;
		if (audio.isActive()) return LOADING;

		if (extractRecordingFeaturesThread==null) return NO_DATA;

		try
		{
			if (extractRecordingFeaturesThread.isAlive())
			{
				extractRecordingFeaturesThread.join(CLUSTER_WAIT);
				extractRecordingFeaturesThread.join();
			}
	
			int[] audioClusters = extractRecordingFeaturesThread.getClusterAssignments();
			if (audioClusters==null) return NO_DATA;
		
			JSONArray data = new JSONArray(json);
			String key = data.getString(0);
			
			ExtractFeatures extract = audioThreads.get(key);
			if (extract==null) return NO_FILE;
			extract.join();

			int[] fileClusters = extract.getClusterAssignments();
			if (fileClusters==null)  return NO_DATA;
				
			int low = 0, high = fileClusters.length;
			double rate;
			if (data.length()>1)
			{
				try
				{
					low = data.getInt(1);
					high = data.getInt(2);
					rate = data.getInt(3);
					
					// Convert from time to selected frame window indices
					low = (int)( low * AudioFeatures.getFrameRate() 
							/ (rate * AudioFeatures.getWindowStep()));
					
					if (low>fileClusters.length) return INCORRECT; 
					
					high =(int)( high * AudioFeatures.getFrameRate() 
							/ (rate * AudioFeatures.getWindowStep()));
					
					if (high>fileClusters.length) return INCORRECT;
					if (low>=high) return INCORRECT;
					
				}
				catch (Exception e) 
				{
					Util.exceptionToast(activity, e);
				}
			}
			int[] fileRange = new int[high-low];
			System.arraycopy(fileClusters, low, fileRange, 0, high-low);
			
			double[] result 
			    = SpellCheck.audioDistance(audioClusters, fileRange);
			
			double frames = result[SpellCheck.SOURCE_FRAMES] 
					+ result[SpellCheck.TARGET_FRAMES]; 
			double  distance = result[SpellCheck.DIFFERENCE]; 
			Log.i(TAG, "Difference = " + (distance/frames) );
			
			if (distance/frames < CORRECT_DISTANCE) return CORRECT;
			if (distance/frames < CLOSE_DISTANCE) return CLOSE;
			return INCORRECT;
		}
		catch (Exception e)
		{
			Util.exceptionToast(activity, e);
			return EXCEPTION;
		}
	}
	
	private class ExtractFeatures extends Thread
	{
		private final static String INTERRUPTED = "Extraction Interrupted: ";

		private String fileName;
		private AudioData audio;
		
		private int[] clusters;
		private double[] pcmData;


		
		ExtractFeatures(AudioData audio)
		{
			this.audio = audio;
			this.fileName = null;
			clusters = null;
			audio.stopAudio();
		}
		
		ExtractFeatures(String fileName)
		{
			this.fileName = fileName;
			clusters = null;
			audio = null;
		}
		
		@SuppressWarnings("unused")
		public double[] getPCMData() { return pcmData; }
		
		@Override
		public void run()
		{
			Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
			try
			{
				if (audio==null)
				{
					audio = new AudioData(activity);
					File file = new File(lessonFolder, fileName);
					audio.read(file);
					audio.waitForRead();
					
					pcmData = audio.getTimeDomain(null);
					if (isInterrupted())
					{
						Log.i(TAG, INTERRUPTED + fileName);
						return;
					}
				}
				else if (!audio.isRecorded())
				{
					return;
				}
				
				if (fileName==null) fileName = "Audio Recording";
		        Log.i(TAG, "Starting to process frames " + fileName);

				AudioCompare compare = new AudioCompare(activity, audio);
		        Log.i(TAG, "Processed Frames " + fileName);
		        

				if (isInterrupted())
				{
					Log.i(TAG, INTERRUPTED + fileName);
					return;
				}

				Log.i(TAG, "Assigning clusters " + fileName);
				clusters = compare.assignClusters(null,  audio);
				if (isInterrupted())
				{
					Log.i(TAG, INTERRUPTED);
					return;
				}
				Log.i(TAG, "clusters assigned " + fileName);
				
				Log.i(TAG, "Extraction thread complete " + fileName);
			}
			catch (Exception e)
			{
				Util.exceptionToast(activity, e);
			}
		}
		
		int[] getClusterAssignments()
		{
			return clusters;
		}
		
	}   // End of ExtractFeatures nested thread
	
}   // End of JSInterface class
