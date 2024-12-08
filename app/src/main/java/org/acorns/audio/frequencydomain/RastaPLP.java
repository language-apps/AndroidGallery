/*
 * RastaPLP.java
 *
 *   @author  harveyd
 *   Dan Harvey - Professor of Computer Science
 *   Southern Oregon University, 1250 Siskiyou Blvd., Ashland, OR 97520-5028
 *   harveyd@sou.edu
 *   @version 1.00
 *
 *   Copyright 2010, all rights reserved
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
package org.acorns.audio.frequencydomain;

import org.acorns.audio.AudioDefaults;
import org.acorns.audio.timedomain.LinearPrediction;
import org.acorns.data.HistoryBuffer;

/** Class to extract perceptual linear perception features from an audio signal */
public class RastaPLP
{
	/** No temporal filtering */
	public final static int NONE = 0;
	
	/** Hermansky Rasta temporal filtering */
	@SuppressWarnings("unused")
	private final double EPSELON = 10e-45;  // Constant to prevent log 0

	@SuppressWarnings("unused")
	public final static int HERMANSKY= 1;
	
	/** ARMA temporal filtering */
	@SuppressWarnings("unused")
	public final static int ARMA = 2;
	
	/** Position of the RASTA pole in the Hermansky IIR filter
	 *  His Original paper used 0.98, but 0.94, according to later papers, is optimal
	 */
	private final static double POLE_POSITION = 0.94;
	
	/** Equal loudness filter (use HTK for bandwidths > 5000) boolean filter */
	private final static boolean HTK = false;

	// FIR part of the temporal filter: Hermansky or ARMA
	// 
	private final static double[][] firCoefficients 
		= new double[][]{{}, {0.2, 0.1, 0.0, -0.1, -0.2}, {.2, .2, .2}};
	
	// Pole position of the Hermansky filter: Hermansky or ARMA 
	private final static double[][] iirCoefficients 
	    = new double[][]{ {}, {POLE_POSITION}, {.2, .2} };
	
	// Circular buffers of previous frames processed
	private HistoryBuffer historyX, historyY;
	
	// Gain computed from the PLP process
	private double gain;
	private double normalizedError;
	private double[] iir, fir;
	
	private PLPFilterBank filterBank;
	private double[] equalLoudnessFilter;
	private DiscreteCosineTransform dct; // Discrete cosine object
	
	public RastaPLP(double sampleRate, int FFTSize, int filterType)
	{
		// Create the Bark filter bank
    	double minFreq = AudioDefaults.getMinFreq();
    	double maxFreq = AudioDefaults.getMaxFreq();
    	filterBank = new PLPFilterBank(sampleRate, FFTSize, minFreq, maxFreq);
    	
    	// Create a history buffer for the original unfiltered temporal frames
    	fir = firCoefficients[filterType];
    	iir = iirCoefficients[filterType];
    	historyX = new HistoryBuffer(fir.length);
    	historyY = new HistoryBuffer(iir.length);
    	
    	// Initialize equal loudness array
    	int numberFilters = filterBank.getNumberFilters();
        equalLoudnessFilter = new double[numberFilters];
        
        double centerFreq;
        for (int freq = 0; freq < numberFilters; freq++) 
        {
            centerFreq = filterBank.getCenterFrequency(freq);
            equalLoudnessFilter[freq] = equalLoudnessScaleFactor(centerFreq, HTK);
        }		
        
        // Cosine values for inverse discrete cosine transform
        dct = new DiscreteCosineTransform(numberFilters, numberFilters/2);
	}
	
    /** Compute the RASTA PLP coefficients 
     * 
     * @param spectrum The frequency spectrum
     * @param P Number of LPC coefficients
     * @return The Rasta PLP Coefficients after the autocorrelation
     */
    @SuppressWarnings("ConstantConditions")
    public double[] computeRastaPLP(double[] spectrum, int P)
    {
    	double[] spectralArray;

        // First compute critical band filter output
        spectralArray = filterBank.applyFilter(spectrum);
        
        // Perform RASTA temporal filtering
        spectralArray = temporalFilter(spectralArray);
        
        // Then scale the critical band outness for equal loudness
        for (int freq=0; freq<spectralArray.length; freq++)
        {
           spectralArray[freq] *= equalLoudnessFilter[freq];
        }
        
        spectralArray = powerLaw(spectralArray);  
        double[] timeDomain = dct.applyBackTransform(spectralArray, new double[spectralArray.length/2]);
        
        // Linear prediction coefficients for LPC
        double[] coefficients 
            = LinearPrediction.getLPCCoefficients(timeDomain, P, 0, timeDomain.length);
        
        gain = LinearPrediction.getGain(P, coefficients, timeDomain);
        normalizedError = LinearPrediction.getNormalizedLPCError(P, coefficients, timeDomain);
	    return coefficients;
    }
    
    /** Return the gain computed from the PLP process */
    public double getGain()
    {
    	return gain;
    }
    
    /** Return the normalized LPC error */
    public double getNormalizedError()
    {
    	return normalizedError;
    }
    
	
    /**
     * This function return the equal loudness preemphasis factor for a frequency. 
     *  The old formula for low sampling rates is given by
     * <p/>
     * HZ formula (2 * PI * f = w)
     * E(w) = (f^2 + 1.44e6)*f^4 / { (f^2 + 1.6e5) ^ 2 * (f^2 + 9.61e6) }
     * Radians per second formula
     * E(w) = (w^2+56.8e6)*w^4/ { (w^2+6.3e6)^2(w^2+0.38e9) }
     * <p/>
     * This from HTK, applies when the NYQUEST frequency is greater than 5KHZ.
     * However, Nyquist frequency of what? If it is the spectrum, then the
     * nyquest frequency is half of windows/second. When trying this version,
     * the output produced was not acceptable (much too low). Both Sphinx 4,
     * and the Berkeley C version use the first formula. 
     * <p/>
     * E(w) = (w^2+56.8e6)*w^4/{ (w^2+6.3e6)^2 * (w^2+0.38e9) * (w^6+9.58e26) }
     * <p/>
     * where w is frequency in radians/second
     * @param freq frequency in hz
     * @param htk Use HTK formula
     */
	@SuppressWarnings("SameParameterValue")
    private double equalLoudnessScaleFactor(double freq, boolean htk) 
    {
    	double w = freq * 2 * Math.PI;
    	
    	// Calculate the common part
    	double wSquared = w * w;
    	double wFourth = Math.pow(w,  4);
    	double numerator = (wSquared + 56.8e6) * wFourth;
    	double denominator = Math.pow((wSquared + 6.3e6), 2) * (wSquared + 0.38e9);
    	if (htk)  
    	{
    		double wSixth = wSquared * wFourth;
    		denominator *= (wSixth + 9.58e26);
    	}
    	return (denominator==0) ? 0 : numerator / denominator;
    }

    /**
     * Applies the intensity loudness power law to the bark filter outputs 
     * 
     * This operation approximates simulates the non-linear relationship between 
     * sound intensity and perceived loudness. This operation reduces 
     * spectral amplitudes of the critical bands and enables all-pole LPC modeling with
     * relatively low order AR filters.
     * 
     * @param spectrum The spectrim to apply power law to
     */
    private double[] powerLaw(double[] spectrum) 
    {
        for (int i = 0; i < spectrum.length; i++) 
        {
            spectrum[i] = Math.pow(spectrum[i], 1.0 / 3.0);
        }
        return spectrum;
    }
    
    /** Apply the Hermansky band pass filter
     *     Transfer: H(Z) = 0.1 * (2 + z^-1 - z^-3 - 2*z^-4)/(z^-4 * (1-0.94 z^-1))
     *     
     *     Note: initial value 0.98 was later optimized to 0.94 by Hermansky
     *     
     * @param spectralArray input spectrum
     * @return filtered spectrum
     */
    private double[] temporalFilter(double[] spectralArray)
    {
    	// Add the new untouched frame to the history buffer
    	historyX.add(spectralArray);  
    	
    	// Move to CEPSTRAL domain
		double EPSELON = 10e-45;  // Constant to prevent log 0
		for ( int freq=0; freq<spectralArray.length; freq++)
    	{
			if(spectralArray[freq] < EPSELON)
			{
				spectralArray[freq] = EPSELON;
			}

    		spectralArray[freq] = Math.log(spectralArray[freq]);
    	}
	    
    	// Other temporal filtering (e.g. Median smoothing) can be done here
    	// Nothing done so far
    	
    	// Don't apply the Rasta bandpass till the history buffer fills up.
    	if (historyX.isFull())
    	{
	    	// Perform the band pass filter of multiple frames (Per Hermansky algorithm).
	    	spectralArray = bandPass(spectralArray);
    	}

    	// Now add the filtered spectrum to the output history
    	historyY.add(spectralArray.clone());

       	// Move back to frequency domain
    	for ( int freq=0; freq<spectralArray.length; freq++)
    	{
            if (spectralArray[freq] < Math.log(Double.MAX_VALUE))
            {
        		spectralArray[freq] = Math.exp(spectralArray[freq]);
            }
            else
            {
                spectralArray[freq] = Double.MAX_VALUE;
            }
    	}
    	return spectralArray;
    }
    
    /** Implement the RASTA bandpass filter
     * 
     * @param spectralArray The current frame
     * @return filtered spectral array
     */
    private double[] bandPass(double[] spectralArray)
    {
    	if (fir.length==0) return spectralArray;
    	
    	double[] filteredSpectrum = new double[spectralArray.length];
    	double[] firHistory, iirHistory;
     	
    	for (int num = 0; num<fir.length; num++)
    	{

    		// Filter the current signal (FIR part) 
    		firHistory = (double[])historyX.get(num);
    		
    		// Perform the FIR convolution
    		for (int feature=0; feature<spectralArray.length; feature++)
    		{
    			filteredSpectrum[feature] += fir[num] * firHistory[feature]; 
    		}
        }
    	
  		// Perform the IIR convolution
      	for (int num = 0; num<iir.length; num++)
    	{

    		// Filter the current signal (FIR part) 
    		iirHistory = (double[])historyY.get(num);
    		
    		// Perform the IIR convolution
    		for (int feature=0; feature<spectralArray.length; feature++)
    		{
    			filteredSpectrum[feature] += iir[num] * iirHistory[feature]; 
    		}
        }
    	return filteredSpectrum;
    	
    }   // End of bandPass()
 
}  // End of Rasta PLP class
