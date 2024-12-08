package org.acorns.data;

/*
 *   @author  HarveyD
 *   Dan Harvey - Professor of Computer Science
 *   Southern Oregon University, 1250 Siskiyou Blvd., Ashland, OR 97520-5028
 *   harveyd@sou.edu
 *   @version 1.00
 *
 *   Copyright 2014, all rights reserved
 *
 * This software is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * To receive a copy of the GNU Lesser General Public write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */



import org.acorns.audio.NormalizeFrames;
import org.acorns.audio.AudioDefaults;
import org.acorns.audio.timedomain.TimeDomain;
import org.acorns.audio.frequencydomain.FastFourierTransform;
import org.acorns.audio.frequencydomain.RastaPLP;
import org.acorns.audio.timedomain.Butterworth;
import org.acorns.audio.timedomain.Filter;
import org.acorns.audio.timedomain.LinearPrediction;
import org.acorns.audio.timedomain.ResampleAudio;

import android.graphics.Point;
import android.util.Log;
import java.util.Locale;

import androidx.annotation.NonNull;

public class AudioFeatures 
{
	private static final String TAG = "AudioFeatures";
	
	// The number of LPC coefficients - Default = 8
	private static final int P = AudioDefaults.getLPCCoefficients();
	
	// The number of CEPSTRAL coefficients to model the vocal track (normally: f + sample rate/1000)
    private static final int C = AudioDefaults.getCepstrumLength();
	
	// The temporal filtering algorithm,
	private static final int TEMPORAL_FILTER = RastaPLP.NONE;
	
	// Number of iterations for convergence of variance and skew statistics of features
	private static final int NORMALIZE_LOOP = 3; 
	
	// Normalized feature frame rate - Default = 16000 frames per second
	private static final float FRAME_RATE = AudioDefaults.getFrameRate();   

	// Window (frame) size - Default = 25.625 ms
	private static final int WSIZE = (int)(FRAME_RATE * AudioDefaults.getWindowSize()/1000); 

	// Window (frame) step (overlap) - Default = 10 ms
	private static final int WSTEP = (int)(FRAME_RATE * AudioDefaults.getWindowShift()/1000); 
	
	// Minimum cutoff for the band pass Butterworth filter
	private static final int MIN_FREQUENCY = 30;

	// FFT size for the frame rate
	private static final int FFTSIZE = 1<<(32 - Integer.numberOfLeadingZeros(WSIZE - 1));

	// Define the parameter to control degree of dynamic feature linear regression curve fitting
	private final static int D = 1;   // Curve fitting loop goes from -D to +D

	/** Starting offset to array of Linear Prediction coefficients */
	private static final int LPC_COEFFICIENTS = 0;

	/* Easier symbol access to specific LPC_COEFFICIENTS */
	public static final int LPC0 = LPC_COEFFICIENTS;
	public static final int LPC1 = LPC_COEFFICIENTS + 1;
	public static final int LPC2 = LPC_COEFFICIENTS + 2;
	public static final int LPC3 = LPC_COEFFICIENTS + 3;
	public static final int LPC4 = LPC_COEFFICIENTS + 4;
	public static final int LPC5 = LPC_COEFFICIENTS + 5;
	public static final int LPC6 = LPC_COEFFICIENTS + 6;
	public static final int LPC7 = LPC_COEFFICIENTS + 7;

	public static final int LPC_ERROR = LPC_COEFFICIENTS + P;
	private static final int ZERO_CROSS = LPC_ERROR + 1;
	private static final int SPECTRAL_FLUX = ZERO_CROSS + 1;

	/** Number of audio features */
	public static final int FEATURES_LENGTH = SPECTRAL_FLUX + 1;
	
	/** Number of audio features including delta and delta delta values */
	private static final int FEATURE_ARRAY_LENGTH = FEATURES_LENGTH * 3;
	

	/** statistics array row for holding mean */
	private final int MEAN = 0;
	/** statistics array row for holding standard deviation */
	private final int STD = 1;
	/** statistics array row for holding variance */
	@SuppressWarnings({"FieldCanBeLocal"})
	private final int VARIANCE = 2;
	/** statistics array row for holding skew */
	@SuppressWarnings({"FieldCanBeLocal"})
	private final int SKEW = 3;
	/** statistics array row for holding kirtosis */
	@SuppressWarnings({"FieldCanBeLocal"})
	private final int KIRTOSIS = 4;

	// Instance Variables
	private double[][] features;     // Computed features for each audio frame

	/** Constructor to create a feature list of part of an audio signal
	 * 
	 * @param bounds Starting and ending offsets
	 * @param audio Audio sound file
	 */
	public AudioFeatures(Point bounds, AudioData audio, boolean norm, boolean cvn, boolean cep)
	{
    	updateFeatures(bounds, audio, norm, cvn, cep);
	}
	
	public static double getFrameRate() { return FRAME_RATE; }
	public static int getWindowStep() { return WSTEP; }
	
	/** Constructor to create a feature list of the audio signal 
	 * 
	 * @param audio Object containing the audio signal
	 * @param norm true to perform mean normalization
	 * @param cvn true to perform CVN and skew normalization, in addition to CMN
	 * @param cep true if to convert LPC parameters to CEPSTRALS
	 */
	@SuppressWarnings({"unused"})
    public AudioFeatures(AudioData audio, boolean norm, boolean cvn, boolean cep)
    {
    	updateFeatures(null, audio, norm, cvn, cep);
    }
    
    /** Get frames of features extracted from raw data */
	@SuppressWarnings({"unused"})
    public double[][] getFrames()
    {
    	return features;
    }
    
    /** extract features from signal
     * 
     * @param bounds offsets to audio signal (-1,-1) or null implies all
     * @param audio SoundData audio object
     * @param norm true to perform mean normalization
     * @param cvn true to perform CVN and skew normalization, in addition to cmn
	 * @param cep true if to convert LPC parameters to CEPSTRALS
     */
    private void updateFeatures(Point bounds, AudioData audio, boolean norm, boolean cvn, boolean cep)
    {
    	// Cannot extract features from an audio with nothing recorded
    	if (audio.isRecorded())
    	{  
	   		// Get time domain data from the audio object
	        double[] samples = audio.getTimeDomain(bounds);   
            TimeDomain.removeDC(samples);
	        
			// Normalize the sample rate to accommodate recordings at differing rates
    		double oldRate = audio.getFrameRate();
    		if (FRAME_RATE != oldRate)
    		{
    			Log.i(TAG, "Resampling audio");
    			double ratio = FRAME_RATE / oldRate;
    			bounds = new Point(bounds.x, bounds.y);
    			if (bounds.x!=-1) bounds.x *= ratio;
    			if (bounds.y!=-1) bounds.y *= ratio;
    			samples = ResampleAudio.changeFrameRate(samples, (int)oldRate, (int)FRAME_RATE);
    	       	if (Thread.currentThread().isInterrupted()) return;
			}
	   	
	        // Apply a BUTTERWORTH band pass filter
	        Butterworth btw = new Butterworth(MIN_FREQUENCY/FRAME_RATE, false);
	        samples = btw.applyFilter(samples, true);

	        extractFeatures(samples, norm, cvn, cep);
    	}
    }		// End of updateFeatures()
    
	/** Method to extract features from a time domain array in complex format
	 * 
	 * @param samples array of samples (even indices - real, odd indices - imaginary)
	 * @param norm true if to perform mean normalization
	 * @param cvn true if to perform variance and skew normalization, in addition to mean normalization
	 * @param cep true if to convert LPC parameters to CEPSTRALS
	 * @return feature list
	 * 
	 */
	@SuppressWarnings("UnusedReturnValue")
	private double[][] extractFeatures(double[] samples, boolean norm, boolean cvn, boolean cep)
	{
    	// Real (non complex and not filtered) representation of the signal
        double[] complex = new double[samples.length*2];
        for (int s=0; s<samples.length; s++)  {  complex[s*2] = samples[s]; }
        
         // Create windowing function
        Filter filter = new Filter();
        double[] hammingFilter = Filter.createHammingWindow(WSIZE);
        
        FastFourierTransform fourier = new FastFourierTransform(FFTSIZE);
        double[] fftWindow = new double[2*FFTSIZE];		// Frequency domain window
        double[] spectrum = new double[FFTSIZE];
    	double[] prevSpectrum = null;

        int startFrame, endFrame;

        // Instantiate the object for computing the RASTA PLP of each frame
        RastaPLP rasta = new RastaPLP(FRAME_RATE, FFTSIZE, TEMPORAL_FILTER);
        
        // Create array references
        double[] power, rastaCoefficients;
        
        int frames = samples.length / WSTEP;
        if (frames*WSTEP + WSIZE > samples.length) frames--;
        
        features = new double[frames][FEATURE_ARRAY_LENGTH];
        if (frames==0) return features;

        for (int frame = 0; frame< frames; frame++)
        {
        	if (Thread.currentThread().isInterrupted()) 
        	{
        		Log.d(TAG, "Processed " + frame + " of " + frames);
        		return null;
        	}
        	
           	startFrame = 2*frame*WSTEP;
        	endFrame = startFrame + WSIZE*2;
        	if (endFrame>complex.length) 
        	{
        		endFrame = complex.length; 
        	}
        	
        	/* Clear data left over from the previous frame */
        	if (endFrame - startFrame != WSIZE*2)
        	{
        		for (int s=0; s<2*WSIZE; s+=2)
        		{
        			fftWindow[s] = fftWindow[s+1] = 0;	
        		}
        	}
    		for (int s=WSIZE; s<2*FFTSIZE; s++) fftWindow[s] = 0;

        	/* Load windows for processing the next frame */
        	System.arraycopy(complex, startFrame, fftWindow, 0, endFrame - startFrame);
     
            // Compute the number of zero crossings per window step size
            features[frame][ZERO_CROSS]   	     
       	    		= TimeDomain.zeroCrossings(fftWindow, 0, WSTEP*2, 2);

        	// Apply the window to the frame
        	fftWindow = filter.applyWindow(WSIZE, hammingFilter, fftWindow);

        	// Apply the Fast Fourier Transform
        	fftWindow = fourier.fft(fftWindow);

            // Get spectrum power (square of amplitudes)
        	power = FastFourierTransform.powerSpectrum(fftWindow, null);
        	
        	// Compute spectral flux
        	for  (int i=0; i<power.length; i++)
        	{
        		spectrum[i] = Math.sqrt(power[i]);
        	}
            features[frame][SPECTRAL_FLUX] = computeFlux(spectrum, prevSpectrum);
            prevSpectrum = spectrum.clone();

         	// Compute the Rasta PLP coefficients
            // Linear prediction coefficients for LPC
        	rastaCoefficients = rasta.computeRastaPLP(power, (C==P) ? P-1 : P);
            if (rastaCoefficients !=null)
            {
            	features[frame][LPC_COEFFICIENTS] = rastaCoefficients[0];
            	System.arraycopy(rastaCoefficients, 1, features[frame], LPC_COEFFICIENTS + 1, P-1);

                // Compute projected linear prediction error of original window 
                features[frame][LPC_ERROR] = rasta.getNormalizedError();
            }
            
            if (cep)
            {
               	rastaCoefficients = LinearPrediction.lpcToCepstral(P-1, P, rastaCoefficients, rasta.getGain());
               	System.arraycopy(rastaCoefficients, 0, features[frame], LPC_COEFFICIENTS, P );
            }
        	
         }
 
        // Calculate delta values
        calculateDynamicFeatures(LPC_COEFFICIENTS);
        
        // Calculate delta delta values
        calculateDynamicFeatures(LPC_COEFFICIENTS + FEATURES_LENGTH);
        
        // Normalize cepstrum mean (0), variance (1), skew (0)
    	int start = LPC_COEFFICIENTS;
    	int end = FEATURES_LENGTH; 
    	if (norm) normalizeFeatures(start, end, cvn);
        
    	// Normalize delta mean (0), variance (1), skew (0)
     	start += FEATURES_LENGTH;
    	end += FEATURES_LENGTH; 
    	if (norm) normalizeFeatures(start, end, cvn);

    	// Normalize delta-delta mean (0), variance (1), skew (0)
      	start += FEATURES_LENGTH;
      	end += FEATURES_LENGTH;
    	if (norm) normalizeFeatures(start, end, cvn);
    	
 	    return features;
	}

	/** Method to scale the features and normalize the skew
	 *    Because variance and skew cannot be perfectly normalized
	 *      the method iterates a few times to converge on a better result
	 * 
	 * @param start starting feature to normalize
	 * @param end  ending feature to normalize
	 * @param cvn true to perform variance and skew normalization, in addition to mean normalization
	 */
	@SuppressWarnings("ConstantConditions")
	private void normalizeFeatures(int start, int end, boolean cvn)
	{
		// Iterate to converge mean to 0, variance to 1, and skew to 0
		for (int n=0; n<NORMALIZE_LOOP; n++)
		{
			// Normalize mean of featues to 0
	       features = NormalizeFrames.meanNormalization(features, start, end);
	       if (!cvn) return;
	        
	        // Normalize variances of features unity for better low SNR recognition
	        features = NormalizeFrames.varianceNormalization(features, start, end);
	        // Normalize skew of features to 0 for better Normal Distribution fit
	      	features = NormalizeFrames.skewNormalization(features, start, end);
		}	
	}
	
	/** absolute difference between frames (1 Norm)
	 * 
	 * @param current Data from current frame
	 * @param previous Data from previous frame (or null)
	 */
	private double computeFlux(double[] current, double[] previous)
	{
		double total = 0.0;
		for (int i=0; i<current.length; i++)
		{
			total += (previous==null) ? Math.abs(current[i]) : Math.abs(current[i]-previous[i]);
		}
		return total;
	}

	/** Calculate the delta value for a set of feature
	 * 
	 * @param featureOffset The frame number containing the feature
	 */
	private void calculateDynamicFeatures(int featureOffset)
	{
		int start, end;
		double numerator, denominator;
		
        // Update the delta and delta delta totals
        for (int frame=0; frame<features.length; frame++)
        {
        	for (int feature=0; feature<FEATURES_LENGTH; feature++)
        	{
        		numerator = denominator = 0;
            	start = (frame<D)? -frame: - D;
            	end = (frame>=features.length - D) ? features.length - frame - 1: +D;
        		for (int d= start; d<= end; d++)
        		{
            		numerator +=  d * features[frame + d][feature + featureOffset];
            		denominator += d * d;
        			
        		}
        		if (denominator !=0)
        			features[frame][feature + featureOffset + FEATURES_LENGTH] 
        		          = numerator / denominator;
        	}
        }
	}		// End of calculateDynamicFeatures()


	/** Compute averages for all features and all frames */
	public double[][] computeAverages()
	{
	   	int[] stats = new int[FEATURE_ARRAY_LENGTH];
		for (int feature=0; feature<FEATURE_ARRAY_LENGTH; feature++)
		{
			stats[feature] = feature;
		}
		return computeAverages(stats);		
	}
	
	/** Compute averages for all frames, but selected features
	 * 
	 * @param offsets Offsets to desired features
	 * @return Computed selected averages
	 */
	private double[][] computeAverages(int[] offsets)
	{
		int[] frames = new int[features.length];
		for (int f=0; f<features.length; f++)  frames[f] = f;
		return computeAverages(frames, offsets);
	}
    
	/** Compute statistics on the features object
	 * 
	 * @param frames the frames of interest
	 * @param offsets Array of which features are of interest
	 * @return Computed averages
	 */
	@SuppressWarnings("ForLoopReplaceableByForEach")
	private double[][] computeAverages(int[] frames, int[] offsets)
	{
		double[][] totals = new double[5][offsets.length];
		
		int N = frames.length;
		if (N==0) return totals;
		
		// Total each feature across the frames
		int frame;
		for (int index=0; index < N; index++)
		{
			frame = frames[index];
			if (frame<0 || frame>=features.length) return totals; 
			
			for (int feature=0; feature<offsets.length; feature++)
			{
				totals[MEAN][feature] += features[frame][offsets[feature]];
			}
		}
		
		// Compute the means
		for (int feature = 0; feature<offsets.length; feature++)
		{
			totals[MEAN][feature]/= N;
		}
		if (N<2) return totals;
		
		// Compute totals for variance, stdev, skew, and kirtosis
		double delta;
		for (int index=0; index < N; index++)
		{
			frame = frames[index];

			for (int feature=0; feature<offsets.length; feature++)
			{
				delta = (features[frame][offsets[feature]] - totals[MEAN][feature]);
				totals[VARIANCE][feature] += delta * delta;
				if (N>2) totals[SKEW][feature] += Math.pow(delta, 3);
				if (N>3) totals[KIRTOSIS][feature] += Math.pow(delta,  4);
			}
		}
		
		// Complete the calculations (using Excel formulas)
		double stdev, factor;
		for (int feature=0; feature<offsets.length; feature++)
		{
			totals[VARIANCE][feature] = totals[VARIANCE][feature] /= N - 1;
			totals[STD][feature] = stdev = Math.sqrt(totals[VARIANCE][feature]);
			if (N > 2 && stdev != 0)
			{
				factor = 1.0 * N / ((N-1)*(N-2));
				totals[SKEW][feature] = factor * totals[SKEW][feature] / Math.pow(stdev, 3);
			}
			if (N > 3 && stdev !=0)
			{
				factor = 1.0 * N * (N+1) / ((N-1)*(N-2)*(N-3));
				totals[KIRTOSIS][feature] = factor * totals[KIRTOSIS][feature] / Math.pow(stdev, 4);
				factor = 3.0 * (N-1)*(N-1) / ((N-2)*(N-3));
				totals[KIRTOSIS][feature] -= factor;
			}
		}
		return totals;
	}
	
	/** Get the statistical averages for all frames of the audio
	 * 
	 * @param averages The statistical averages in deviation units of the speech features
	 */
	public double[][] getStatisticsSTD(double[][] averages)
	{
		Point bounds = new Point(0, features.length - 1);
		return getStatisticsSTD(bounds, averages);
	}
	
	/** Get the statistical averages for the selected audio frames
	 * 
	 * @param bounds - bounds.x = starting speech frame, bounds.y = The ending speech frame
	 * @param averages The statistical averages in deviation units of the speech features
	 */
	private double[][] getStatisticsSTD(Point bounds, double[][] averages)
	{
		int startSpeech = bounds.x;
		int endSpeech = bounds.y;
		
		double[][] results = new double[endSpeech -startSpeech + 1][averages[0].length + 2];
		
		double value, mean, std, units;
		int    feature;
		for (int frame=startSpeech; frame<=endSpeech; frame++)
		{
			results[frame-startSpeech][0] = frame*WSTEP;
			for (int stat=0; stat<averages[0].length; stat++)
			{
				feature = stat;
				value = features[frame][feature];
	
				mean = averages[MEAN][stat];
				std = averages[STD][stat];
				units = (value - mean) / std;
				results[frame-startSpeech][stat+1] = units;
			}
		}
		return results;
	}
    
	/** Create the title for toString output
	 * 
	 * @param offsets array of offsets of the desired features
	 * @param detail true if this is for detailed data rather then summary totals
	 */
	@SuppressWarnings("ForLoopReplaceableByForEach")
	private static String title(int[] offsets, boolean detail)
	{
     	// Initialize text for the toString output
    	String[] toStringText = new String[FEATURE_ARRAY_LENGTH];

    	for (int i = 0; i< P; i++)
    	{
    		toStringText[i] = "LPC" + i;
    		toStringText[i+FEATURES_LENGTH] = "DLPC" + i;
    		toStringText[i+2*FEATURES_LENGTH] = "DDLPC" + i;
    	}

    	toStringText[LPC_ERROR] = "LPErr";
    	toStringText[LPC_ERROR + FEATURES_LENGTH] = "DLPErr";
    	toStringText[LPC_ERROR + 2 * FEATURES_LENGTH] = "DDLPErr";

    	// Create string representation of the audio frame data
		StringBuilder build = new StringBuilder();
		String spaces = "        ", text;
	
        if (detail) 	build.append("#### ");
        else build.append("     ");
        
		for (int feature = 0; feature<offsets.length; feature++)
		{
			text = spaces + toStringText[offsets[feature]];
			build.append(String.format("%s ", text.substring(text.length()-10)));
		}
		build.append("\n");
		return build.toString();		
	}

	@Override
	public @NonNull	String toString()
	{
		int[] offsets = new int[FEATURE_ARRAY_LENGTH];
		for (int feature=0; feature<FEATURE_ARRAY_LENGTH; feature++)
		{
			offsets[feature] = feature;
		}
		
		return toString(new Point(-1,-1), offsets);
	}

	/** Create string representation of audio signal between starting and ending samples
	 * 
	 * @param bounds The starting and ending offsets (-1,-1) means entire signal
	 * @param offsets The features of interest
	 * @return String representation of the frames in question
	 */
    private String toString(Point bounds, int[] offsets)
    {
		if (bounds == null) bounds = new Point(-1,-1);
		
		int start = (bounds.x<0) ? 0 : bounds.x / WSTEP;
		int end = (bounds.y + WSTEP - 1)/WSTEP;
		if (end > features.length)  end = features.length;
		
		if (end<start) return "No frames within the specified bounds";

		int[] frames = new int[end-start];
		for (int frame=start; frame<end; frame++)
		{
			frames[frame-start] = frame;
		}
		return toString(frames, offsets);
    }
    
	/** Create a string representation of the selected features
	 * 
	 * @param offsets An array of feature offsets
	 * @param frames array of frames of interest (if null, all frames)
	 * @return The String result
	 * 
	 */
	@SuppressWarnings("ForLoopReplaceableByForEach")
	private String toString(int[] frames, int[] offsets)
	{  
		StringBuilder build = new StringBuilder();
		build.append(title(offsets, true));
	
		int frame;
		double number;
		for (int index=0; index<frames.length; index++)
		{
			frame = frames[index];
			if (frame<0 || frame>=features.length) continue;
			
			build.append(
			String.format(Locale.getDefault(), "%4d", frame));
			for (int feature=0; feature<offsets.length; feature++)
			{
				number = features[frame][offsets[feature]];
				build.append(String.format(Locale.getDefault(), "%11.3f", number));
			}
			build.append("\n");
		}
		
		// Create the statistical totals
		build.append("\n");
		build.append(title(offsets, false));
		double[][] totals = computeAverages(frames, offsets);
		for (int line=0; line<totals.length; line++)
		{
			build.append("    ");
			for (int feature=0; feature<offsets.length; feature++)
			{
				number =  totals[line][feature];
				build.append(String.format(Locale.getDefault(), "%11.3f", number));
			}
			build.append("\n");
			
		}
		return build.toString();
	}
    
}	// End of AudioFeatures class
