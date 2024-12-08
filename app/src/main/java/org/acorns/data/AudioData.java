package org.acorns.data;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

import com.acorns.acornsmobile.AudioPermission;
import com.acorns.acornsmobile.R;

import org.acorns.utilities.Util;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.os.Process;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class AudioData 
{
	private final static String TAG = "SoundData";

	// Record 16 bit pulse code modulation
	private static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;

	// 10 milliseconds = 1000 MICRO Seconds
	private static final long TEN_MILLIS = 10000;
	
	// Amount of time to wait till join (10 milliseconds)
	private static final long JOIN_WAIT = 150;
	
	// Maximum seconds in a recording
	private static final int MAX_SECONDS = 10;
	
	// Size of output buffer for playback
	private static final int OUTPUT_BUFFER_SIZE = 32768;
	
    /** Record rate in frames per second. */
    private int sampleRate;
    
	/** Time domain for loaded audio */
	private short[] audioData;
	
	/** Thread to handle audio recording */
	private static Thread recordThread = null;
	private Thread readThread = null, playThread = null; 
	
	private final AppCompatActivity activity;

	/** Creates a new audio recording/play back object */
	public AudioData(AppCompatActivity activity)
	{
	  super();
	  this.activity = activity;
	  
	  
	  audioData = null;
	  sampleRate = 0;
	}
	

	/** Determine if audio data is present */
	public boolean isRecorded()
	{
		return audioData!=null;
	}
	
	/** Return frames per second */
	double getFrameRate() { return sampleRate; }
	
	/** Access the audio PCM data 
	 * 
	 * @param range Starting (range.x) and ending (range.y) frame (-1 means all)  
	 * @return time domain signal
	 */
	public double[] getTimeDomain(Point range)
	{
		range = setFrameRange(range);
		if (range==null) return null;
		 
		int len = range.y-range.x;
		double[] audio = new double[len];
		
		for (int i=range.x; i<range.y; i++)
		{
			audio[i] = audioData[i];
		}
		return audio;
	}
	
	/** Access the audio PCM data in complex form 
	 * 
	 * @param range Starting (range.x) and ending (range.y) frame (-1 means all)  
	 * @return complex time domain signal
	 */
	@SuppressWarnings("unused")
	public double[] getComplexTimeDomain(Point range)
	{
		range = setFrameRange(range);
		if (range==null) return null;
		 
		int len = range.y-range.x+1;
		double[] audio = new double[len*2];
		for (int i=range.x; i<range.y; i++)
		{
			audio[2*i] = audioData[i];
			audio[2*i+1] = 0;
		}
		return audio;
	}

	/** Update the time domain data 
	 * 
	 * @param newAudio Altered PCM data
	 * @param sampleRate Possibly modified sample rate (-1 if leave alone).
	 */
	@SuppressWarnings("unused")
	public void setTimeDomain(double[] newAudio, int sampleRate)
	{
		if (sampleRate>=0 && sampleRate<4000) 
		{
			Util.errorToast(activity, R.string.Illegal_sample_rate);
			return;
		}
		
		audioData = new short[newAudio.length];
		for (int i=0; i<newAudio.length; i++)
		{
			audioData[i] = (short)newAudio[i];
		}

		if (sampleRate<0) return; 
		this.sampleRate = sampleRate;
	}
	
	public void clearTimeDomain()
	{
		audioData = null;
	}

	/** Update the time domain data from a complex array 
	 * 
	 * @param newAudio Altered PCM data
	 * @param sampleRate Possibly modified sample rate (-1 if leave alone).
	 */
	@SuppressWarnings("unused")
	public void setComplexTimeDomain(double[] newAudio, int sampleRate)
	{
		audioData = new short[newAudio.length/2];
		for (int i=0; i<newAudio.length; i++)
		{
			audioData[i] = (short)newAudio[2*i];
		}
		if (sampleRate>=0) this.sampleRate = sampleRate;
	}
	

	/** Define part of the audio PCM data that is of interest
	 * 
	 * @param range The audio frames specified by the caller
	 * @return Adjusted range for use in class methods (or null if no audio)
	 */
	@SuppressWarnings("SuspiciousNameCombination")
	private Point setFrameRange(Point range)
	{
		if (audioData== null) return null;
		if (range==null) range = new Point(-1,-1);
		if (range.x==-1) range.x = 0;
		if (range.y==-1) range.y = audioData.length;
		if (range.y < range.x) return new Point(range.y, range.x); 
		return new Point(range); 
	}
	
	/** Determine if any of the audio threads are active */
	public synchronized boolean isActive()
	{
		if (isActive(recordThread)) 
			return true;
		if (isActive(readThread)) 
			return true;

		return isActive(playThread);
	}
	
	/** Determine if a single audio thread is active */
	private boolean isActive(Thread thread)
	{
		return (thread!=null && thread.isAlive());
	}

	/** Return the number of audio frames */
	public int getFrames()
	{
		if (audioData==null) return 0;
		return audioData.length;
	}

	/** Replay recorded or file-based audio */
	public synchronized void playAudio() 
	{
		if (isActive(recordThread)) 
			stopAudio(recordThread);
		if (isActive()) 
		{
			Util.errorToast(activity, R.string.Audio_system_busy);
			return;
		}
		
		if (getFrames()==0)  
		{
			Util.errorToast(activity, R.string.No_audio_to_play);
			return;
		}
		
		playThread = new PlayThread();
		playThread.start();
	}
	
	/** Stops a recording or file read operation	 */
	public synchronized void stopAudio() 
	{
		stopAudio(recordThread);
		stopAudio(playThread);
		stopAudio(readThread);
	}
	
	/** Method to stop a particular running thread */
	private void stopAudio(Thread thread)
	{
		try
		{
			if (thread!=null && thread.isAlive())
			{
				thread.interrupt();
				while (thread.isAlive()) 
				{
					thread.join(JOIN_WAIT);
					if (thread.isAlive())
						Log.e(TAG, "Thread still alive: " + thread);
				}
				if (thread== recordThread) recordThread = null;
				if (thread== playThread)   playThread = null;
				if (thread== readThread)   readThread = null;
			}
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Starts a new recording.
	 */
	public synchronized void recordAudio() 
	{
		if (isActive(recordThread)) stopAudio(recordThread);
		if (isActive(playThread)) stopAudio(playThread);
		if (isActive()) 
		{
			Util.errorToast(activity, R.string.Audio_system_busy);
			return;
		}
		
		recordThread = new RecordThread();
		recordThread.start();
	}
	
	/** Read an audio file from disk */
	public synchronized void read(File file) throws IOException
	{
		if (isActive(recordThread)) stopAudio(recordThread);
		if (isActive()) 
		{
			Util.errorToast(activity, R.string.Audio_system_busy);
			return;
		}
		
        MediaExtractor extractor = new MediaExtractor();
       	FileInputStream inputStream = new FileInputStream(file);
		extractor.setDataSource(inputStream.getFD());
        extractor.selectTrack(0);
        
        readThread = new ReadThread(extractor, inputStream);
        readThread.start();
	}
	
	public synchronized void waitForRead()
	{
		if (readThread==null) return;
		
		try
		{
			Log.v(TAG, "Waiting for read");
			if (isActive(readThread)) 
			{
				readThread.join();
				Log.v(TAG, "Read operation completed");
			}
		}
		catch (InterruptedException e)
		{
			stopAudio(readThread);
			Thread.currentThread().interrupt();
			Log.v(TAG, "Read interrupted");
		}
	}
	
	private class ReadThread extends Thread
	{
		MediaExtractor extractor;
		FileInputStream inputStream;
		
		ReadThread(MediaExtractor extractor, FileInputStream inputStream)
		{
			this.extractor = extractor;
			this.inputStream = inputStream;
		}
		
		@Override
		public @NonNull String toString() { return "ReadThread"; }

		@Override
		public synchronized void run()
		{
			Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

			InterruptedException interrupt = new InterruptedException("Read operation cancelled");
			MediaCodec codec = null;
			ByteArrayOutputStream bos = null;
			boolean endian = true;
			try
			{
			    MediaFormat format = extractor.getTrackFormat(0);
			    sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
			    if (sampleRate <=0) 
			    	sampleRate = format.getInteger(MediaFormat.KEY_FRAME_RATE);
			    
			    format.setInteger(MediaFormat.KEY_FRAME_RATE, 16000);
			    format.setInteger(MediaFormat.KEY_SAMPLE_RATE, 16000);
			    
			    String mime = format.getString(MediaFormat.KEY_MIME);
			        
			    // the actual decoder
				if (mime==null)
					throw new Exception();

				codec = MediaCodec.createDecoderByType(mime);
			    int channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT); 

			    codec.configure(format, null /* surface */, null /* crypto */, 0 /* flags */);
			    codec.start();
			    
		        bos = new ByteArrayOutputStream();
			       
				// Decode the input file
				boolean outputEOS = false, inputEOS = false;
		    	int bufferIndex;
		    	long time = 0;
		        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

		        Log.v(TAG, "Reading file");
		        while (!outputEOS) 
		        {
					if (isInterrupted()) throw interrupt;
		            if (!inputEOS) 
		            {
		                bufferIndex = codec.dequeueInputBuffer(TEN_MILLIS);
		                if (bufferIndex >= 0) 
		                {
		                	// Read encoded data
							ByteBuffer dstBuf = codec.getInputBuffer(bufferIndex);

							int sampleSize = (dstBuf==null) ? -1 :
		                        extractor.readSampleData(dstBuf, 0 /* offset */);
	
		                    if (sampleSize < 0)  
		                    { 
		                    	inputEOS = true;
		                    	sampleSize = 0;
		                    }
		                    else
							{
								time = extractor.getSampleTime();
							}


		                    // Feed encoded data to the decoder
		                    codec.queueInputBuffer(
		                            bufferIndex,
		                            0 /* offset */,
		                            sampleSize,
		                            time,
		                            inputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);
		                    
		                    if (!inputEOS) 
		                    {
		                        extractor.advance();
		                    }
		                }
		                else
		                {
		                	Log.e(TAG, "No input buffer available");
		                }
		                
		            }	// of if end of encoded input
	
		            // Decode to PCM
		            int index = codec.dequeueOutputBuffer(info, TEN_MILLIS);
		            if (index >= 0)  // Normal return is >=0
		            {
		            	// Get one of the decoder output buffers
		                ByteBuffer buffer = codec.getOutputBuffer(index);
						assert buffer != null;
						endian = buffer.order() == ByteOrder.BIG_ENDIAN;
	
		                // Decode and write to the byte array output stream
		                final byte[] pcm = new byte[info.size];
		                buffer.get(pcm);
		                if(pcm.length > 0) { bos.write(pcm, 0, pcm.length); }
		                buffer.clear();
		                
		                // Return the buffer to the codec object
		                codec.releaseOutputBuffer(index, false /* no rendering */);
		                
		                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) 
		                {
		                    outputEOS = true;
		                }
		            } 
		            else  // Handle possible error codes (when index < 0) 
		            {
						if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED)
						{
							format = codec.getOutputFormat();
							channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
							format.setInteger(MediaFormat.KEY_FRAME_RATE, 16000);
							format.setInteger(MediaFormat.KEY_SAMPLE_RATE, 16000);

							mime = format.getString(MediaFormat.KEY_MIME);
							Log.i(TAG, "audio format: " + format + " " + mime);
						}
						else if (index == MediaCodec.INFO_TRY_AGAIN_LATER) {
							Log.d(TAG, "Try again later");
						}
						else
						{
								Log.d(TAG, "decoding error: ed " + index);
						}
		            }
		        }		// End while
		        
		        Log.d(TAG, "Decoding complete");
		        
		        /* Convert byte stream to array of PCM data */
		        int frames = bos.size() /(2*channels);
		        byte[] bytes = bos.toByteArray();
		        audioData = new short[frames];
		        for (int b=0; b<frames*channels; b+=channels)
		        {
		        	// Assume BIG ENDIAN
		        	if (endian)
		        	{
		        		audioData[b/channels] = (short)(bytes[2*b]*256 + bytes[2*b+1]);
		        	}
		        	else
		        	{
		        	    // Android devices are little endian
		        		audioData[b/channels] = (short)(bytes[2*b+1]*256 + bytes[2*b]);
		        	}
		        }
			}
			catch (InterruptedException e) 
			{
				audioData = null;
				Util.exceptionToast(activity, interrupt);
			}
			catch (Exception e)
			{
				Util.exceptionToast(activity, e);
			}
			finally
			{
		        // Release resources
				try
				{
		        	inputStream.close();
		        	if (codec==null || bos==null) throw new Exception();
		        	codec.stop();
		        	codec.release();
		        	bos.close();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				
				this.notify();
				Log.v(TAG, "File read: notifying");
			}
			
		}	// End run()
	}		// End ReadThread class
	
	/** Thread to handle recording audio */
	private class RecordThread extends Thread
	{
		int  bSize;
		
		RecordThread()
		{
			bSize = 0;
		}
		
		@Override
		public @NonNull String toString() { return "RecordThread"; }
		
		@Override
		public void run() 
		{
			try
			{
				Process.setThreadPriority
					(Process.THREAD_PRIORITY_URGENT_AUDIO);
	
				AudioRecord recorder = setAudioParameters();
				if (recorder==null)
				{
					Util.errorToast(activity, R.string.No_audio_support);
					return;
				}
				
				ArrayList<short[]> audioStream = new ArrayList<>();
				int bufferRead, frames = 0;
				int maxSeconds = sampleRate * MAX_SECONDS;
				short[] readBuffer = new short[bSize], temp;
	
				recorder.startRecording();
				while (!isInterrupted()) 
				{ 
					bufferRead = recorder.read(readBuffer, 0, bSize);
					if (bufferRead == AudioRecord.ERROR_INVALID_OPERATION) 
					{
						Log.d("read()", "Returned AudioRecord.ERROR_INVALID_OPERATION");
						return;
					} 
					else if (bufferRead == AudioRecord.ERROR_BAD_VALUE) 
					{
						Log.d("read()", "returned AudioRecord.ERROR_BAD_VALUE");
						return;
					} 

					temp = new short[bufferRead];
					System.arraycopy(readBuffer, 0, temp, 0, bufferRead);
					frames += bufferRead;
					audioStream.add(temp);
					
					if (frames > maxSeconds)
						interrupt(); 
				}
				
				// Close resources�
				recorder.stop();
				recorder.release();

				// Copy recorded audio into an internal PCM array
				audioData = new short[frames];
				int offset = 0;
				for (short[] buffer: audioStream)
				{
					System.arraycopy(buffer, 0, audioData, offset, buffer.length);
					offset+= buffer.length;
				}
				Log.v(TAG, "audio recording completed");
			}
			catch (Exception e)
			{
				Util.exceptionToast(activity, e);
			}
			
		}	// End run()
		
		/** Look for supported recording parameters */
		private AudioRecord setAudioParameters()
		{
			AudioPermission.requestPermission(activity); // Request audio permission if not done already.

			// Array of possible sampling rates
			int channels = AudioFormat.CHANNEL_IN_MONO;
			
			int[] samplingRates = {16000, 44100, 22050, 8000, 11025};
			for (int sample:samplingRates)
			{
				try
				{
					bSize = AudioRecord.getMinBufferSize(sample,
							channels, ENCODING);
					bSize *= 10;

					AudioRecord record = null;
					if (ContextCompat.checkSelfPermission(activity,
							Manifest.permission.RECORD_AUDIO)
							!= PackageManager.PERMISSION_GRANTED) {

						Log.d("Recorder",
								"Audio recording permissiobn not granted");
						return record;
					}
					else {
						record = new AudioRecord
								(MediaRecorder.AudioSource.MIC, sample,
										channels, ENCODING, bSize);
					}

					if (record.getState() == AudioRecord.STATE_INITIALIZED)
					{
						Log.d("Recorder", 
								"Audio recorder initialised at " + record.getSampleRate());
						
						// Initialize audio parameters
						sampleRate = sample;
						return record;
					}
					record.release();
				}
				catch (IllegalArgumentException e)	{ /* Try the next one */ }
			}
			return null;
			
		}	// End of setAudioParameters() 

	}		// End of nested RecordThread class
	
	private class PlayThread extends Thread
	{
		PlayThread() {}

		@Override
		public @NonNull String toString() { return "PlayThread"; }

		@Override
		public void run()
		{
			Process.setThreadPriority
				(Process.THREAD_PRIORITY_URGENT_AUDIO);

			if (audioData==null || audioData.length==0)
			{
				Util.errorToast(activity, R.string.No_audio_to_play);
				return;
			}

			int channels = AudioFormat.CHANNEL_OUT_MONO;

			AudioAttributes attributes = new AudioAttributes.Builder()
					.setLegacyStreamType (AudioManager.STREAM_VOICE_CALL)
					.build();

			AudioFormat format = new AudioFormat.Builder()
					.setSampleRate(sampleRate)
					.setEncoding(AudioFormat.ENCODING_PCM_16BIT)
					.setChannelMask(channels)
					.build();

			// zero Session Id forces one to be created
			AudioTrack player = new AudioTrack(attributes,
					format, OUTPUT_BUFFER_SIZE, AudioTrack.MODE_STREAM, 0);
			try
			{
				player.play();
				short[] temp = audioData.clone();
				int len, shortLen = OUTPUT_BUFFER_SIZE;
				for (int offset=0; offset<audioData.length; offset+=shortLen)
				{
					if (isInterrupted())
					{
						player.stop();
						player.release();
						return;
					}
					len = (offset + shortLen>audioData.length) ? temp.length - offset : shortLen;
					player.write(temp,  offset,  len);
				}
			}
			catch (Exception e)
			{
				Util.exceptionToast(activity, e);
			}

			player.stop();
			player.release();

		}	// End of run()
	}		// End PlayThread class

	@Override
	public @NonNull AudioData clone()
	{
	   AudioData newObject;
       try
       {
       	   newObject = (AudioData)super.clone();
	   }
	   catch (Exception e)
	   {
	   	   newObject = new AudioData(activity);
	   	   Util.exceptionToast(activity, e);
	   }

       if (isRecorded())
       {   if (audioData!=null)
              newObject.audioData = audioData.clone();
       }
       return newObject;
 	}
	

	
}	// End of SoundData class
