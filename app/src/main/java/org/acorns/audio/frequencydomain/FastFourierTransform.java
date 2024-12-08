/*
 * FastFourierTransform.java
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
import org.acorns.audio.timedomain.Filter;

import android.graphics.Point;

public class FastFourierTransform
{
    /** Size of each FFT window */
    private final static int FFT_SIZE = 0;
    /** Step (in samples) between FFT windows */
    private final static int FFT_STEP = 1;
    /** Number of FFT windows in signal */
    private final static int FFT_WINDOWS = 2;

	private static final double EPSELON = 0.00001;

    private double[] sines;

    /** Creates a new instance of the SignalAnalysis class
     *  @param N Size of maximum Fourier window
     *
     */
    public FastFourierTransform(int N)
    {
        // Insure that N is a power of 2
        N--;
        for (int i=1; i<=16; i*=2) N |= N>>i;
        N++;

        // Pre-calculate the sine values
        double factor = 2 * Math.PI / N;
        sines = new double[N];
        for (int f=0; f<N; f++)  { sines[f] = Math.sin(factor * f); }
    }   // End of SignalAnalysys() constructor
    
    /** Perform a fast Fourier transform on the complex data
     *  @return A double array where the even indices are real and the
     *          odd indices are imaginary
     */
    public double[] fft(double[] complex)
    {
        // Compute the size of the FFT (N) and its log base 2 (M).
        int N = complex.length /2;
        int M = (int)(Math.log(N) / Math.log(2.0));
        if (2<<(M-1) != N || N<4) return null;

        // Perform the butterfly algorithm to suffle array entries
        // This avoid recursion overhead associated with copying and creating
        //   odd and even sub arrays.
        int j = N, k, endButterfly = 2*(N - 1);
        double tempReal, tempImag;
        for (int i=2; i<endButterfly; i+=2)
        {
            if (i < j)
            {   // Swap positions in the array (from lower to upper places).
                tempReal     = complex[j];
                tempImag     = complex[j+1];
                complex[j]   = complex[i];
                complex[j+1] = complex[i+1];
                complex[i]   = tempReal;
                complex[i+1] = tempImag;
            }
            k = N;
            while (k>=2 & j >= k)
            { j -= k;
              k >>= 1;
            }
            j += k;
        }

        // Perform the fft calculations.
        // fftSubGroupGap is distance between fft subgroups.
        // gap is the difference between corresponding odd and even elements.
        int gap,   fftSubGroupGap, kInc = N/2;
        double realW, imagW;

        for (int stage=1; stage<=M; stage++)
        {
            // 2* elements in the stage. (complex numbers require two doubles).
            fftSubGroupGap = 2<<stage;          // 4, 8, 16, ...
            gap            = fftSubGroupGap>>1; // 2, 4, 8, ...

            // Loops for sub-fft calculations.
            // The outer loop is for each sub-fft group.
            // The inner group is to combine elements within a group.

            for (int even=0; even<complex.length; even+=fftSubGroupGap)
            {   k = 0; // Index into the trig lookup table.
                for (int element=even; element<(even+gap); element+=2)
                {
                   // Look up e^2PIki/N avoiding trig calculations here.
                   realW = sines[(k+(N>>2))%N];   // cos(2PIk/N);
                   imagW = -sines[k%N];           // -sin(2PIk/N);
                   k += kInc;                  // position for next look up.

                   // Complex multiplication of the odd entry of the subgroup
                   //   with (e^2PIi/N)^k = (cos(2PI/N) - i * sin(2*PI/N)^k
                   j = (element + gap);
                   tempReal = realW * complex[j] - imagW   * complex[j+1];
                   tempImag = realW * complex[j+1] + imagW * complex[j];

                   // Adjust the odd entry (subtract: the fft is periodic).
                   complex[j]   = complex[element] - tempReal;
                   complex[j+1] = complex[element+1] -tempImag;

                   //Adjust the even entry.
                   complex[element]   += tempReal;
                   complex[element+1] += tempImag;

            }   }
            kInc >>= 1;
        }
        return complex;
    }   // End of fft()

    /** Perform an inverse fast Fourier transform on the frequency domain
     *  @param fftData complex array of doubles representing the frequency domain
     *  @return inverse FFT containing the time domain
     */
    private double[] ifft(double[] fftData)
    {
        if (fftData==null) return null;
        int N = fftData.length / 2;

        fftData = fft(fftData);
        if (fftData==null) return null;

        double[] timeDomain = new double[fftData.length/2];
        int destination = timeDomain.length - 1;
        for (int s=2; s<fftData.length; s+=2)
        {  timeDomain[destination--] = fftData[s]/N;  }
        timeDomain[0] = fftData[0]/N;
        return timeDomain;
    }   // End of ifft()
 
    /**Performs the power spectrum on the outputted FFT values
     * @param fftData complex array of values calculated by FFT
     * @param spectrum array to hold power spectrum or null to create it
     * @return A double array that the sum of the real and complex values squared
     */
    public static double[] powerSpectrum(double [] fftData, double[] spectrum)
    {
        double[] powerSpectrum = spectrum;
        if (spectrum==null) powerSpectrum  = new double[fftData.length/2];
        double   real, imag;

        for(int i=0;i<fftData.length;i++)
        {   real = fftData[i];
            imag = fftData[++i];
            powerSpectrum[i/2] = (real * real) + (imag * imag);
        }
        return powerSpectrum;
    }
    
    /** Return the energy of a audio window
     * 
     * @param powerSpectrum The power spectrum of the FFT
     * @return calculated energy
     */
    @SuppressWarnings("unused")
    public double getEnergy(double[] powerSpectrum)
    {
        double totalEnergy = 0;
        for (double energy: powerSpectrum)
        {
            totalEnergy += energy;
        }
        return totalEnergy;
    }
    
    /** Return the number of fast Fourier bins */
    @SuppressWarnings("unused")
    public int getFourierSize() { return sines.length; }

    /** Compute the frequency amplitudes of the spectrum
     *
     * @param fftData The complex array of frequencies
     *                  (even indices:real, odd indices:imaginary)
     * @param amps The array to store resulting amplitudes
     * @param dcb true for decibels, false if linear amplitudes
     * @param N Size of non zero window values
     * returns An array of positive frequency amplitudes where index 0 is
     * the maximum amplitude, and the frequency i is at index i.
     */
    @SuppressWarnings("unused")
    public static void amplitudes(double[] fftData, double[] amps, boolean dcb, int N)
    {
    	if (N<0) N = amps.length;
    	double[] power = powerSpectrum(fftData, null);
        double factor;
        amps[0] = 0;
        int index = 1;
        for (int i=0; i<power.length/2; i++)
        {
           if (dcb)
           { 
        	  // The fft returns values not scaled to 2/N
        	  factor = 4.0 / (N * N);
        	  amps[index] = 10 * Math.log10(.00001 + power[i] * factor);
              if (amps[index]<0) amps[index] = 0;
           }
           else  { amps[index] = Math.sqrt(power[i]); }
           if (amps[index]>amps[0]) amps[0]=amps[index];
           index++;
        }
    }
    
	/** Compute the entropy of part of a spectrum
	 * 
	 * @param spectrum The frequency domain power spectrum of the signal
	 * @return The computed entropy
	 */
    @SuppressWarnings("unused")
	public double getEntropy(double[] spectrum, int startBin, int endBin)
	{
		double probability, entropy = 0, total = 0;
		
		for (int i=startBin; i<endBin; i++)  total += spectrum[i];
		
		for (int i=startBin; i<endBin; i++)
		{
			probability = (total!=0)? spectrum[i] / total : 0;
			entropy += probability * Math.log(EPSELON + probability)/Math.log(2.0);
		}
		return -entropy;		
	}
	


    /** Get parameters for performing FFT algorithm
     *
     * @param win  true if signal is to be windowed
     * @param frameRate Number of samples per second
     * @param frames Total number of frames in signal
     * @param point Selected region of signal
     * @return integer array [FFT_SIZE] size of each FFT window,
     *                       [FFT_STEP] signal step between windows
     *                       [FFT_WINDOWS] number of windows
     */
     @SuppressWarnings({"SuspiciousNameCombination", "unused"})
    public static int[] getFFTParams
            (boolean win, double frameRate, int frames, Point point)
    {
        // Compute the number of samples
        if (point == null) point = new Point(-1,-1);
        int samples = frames;
        if (point.x<0) point.x = 0;
        if (point.y<0 || point.y>samples) point.y = samples;
        if (point.y<point.x)
        {   int temp = point.x;
            point.x = point.y;
            point.y = temp;
        }
        samples = point.y - point.x;

        double[] wParams
              = {AudioDefaults.getWindowSize(), AudioDefaults.getWindowShift()};

        int rows = 1, step = 0, FFTSize;
        if (win)
        {
            wParams = AudioDefaults.getSpectrographParams();
            step = (int)(frameRate * wParams[1] / 1000);
            rows = (samples + step - 1) / step;
        }

        // Get FFT size rounded up to next power of 2.
        FFTSize = (int)(frameRate * wParams[0] / 1000);
        FFTSize--;
        for (int i=1; i<=16; i*=2) FFTSize |= FFTSize>>i;
        FFTSize++;

        if (!win)
        {   int size = samples;
            size--;
            for (int i=1; i<=32; i*=2) size |= size>>i;
            size++;

            if (size < FFTSize) size = FFTSize;
            FFTSize = size;
        }

        int[] FFTParams = new int[3];
        FFTParams[FFT_SIZE] = FFTSize;
        FFTParams[FFT_STEP] = step;
        FFTParams[FFT_WINDOWS] = rows;
        return FFTParams;
    }
    
    /** Filter the signal based on the spectral subtraction algorithm
     * 
     * @param complex Audio signal
     * @param params parameters
     * @return Filtered signal
     */
    @SuppressWarnings({"unused"})
    public double[] spectralSubtraction(double[] complex, int[] params)
    {
        int step = params[FFT_STEP] * 2;
        int FFTSize = params[FFT_SIZE];

        double[] window = new double[FFTSize*2];
        double[] minAmplitude = new double[FFTSize];
        double[][] windows = new double[params[FFT_WINDOWS]][FFTSize*2];
        double[] amplitudeData = new double[complex.length/2];
        double amplitude, ratio = 1;
        
        Filter filter = new Filter();
        double[] windowFilter = filter.makeWindowFilter(FFTSize);
        int rows = 0, startWindow;

        // Compute FFT on each window and compute minimum FFT bin values
        for (int offset=0; offset<complex.length; offset += step)
        {
           if (rows == windows.length) break;
           startWindow = offset;
           if (startWindow + FFTSize*2 - 1 >= complex.length)
           {
               startWindow = complex.length - FFTSize*2;
           }
           
           System.arraycopy(complex, startWindow, window, 0, FFTSize*2);
           window = filter.applyWindow(FFTSize, windowFilter, window);
           window = fft(window);  // Perform the FFT
           if (window==null) return null;

           // Update minimum spectrum values
           double angle, x, y, energy = 0;
           for (int bin=0; bin<window.length; bin+=2)
           {
       	      x = window[bin];
       	      y = window[bin+1];
       	      if (x!=0) 
       		  {
       			  angle = -Math.atan(y/x);
       			  window[bin] = x * Math.cos(angle) -y* Math.sin(angle);
       			  window[bin+1] = x * Math.sin(angle) + y* Math.cos(angle);
       		  }
       	      else 
       	      {
       	    	  window[bin] = window[bin+1];
       	    	  window[bin+1] = 0;
              }
       	      
        	  amplitude = Math.abs(window[bin]);
        	  energy+= amplitude;
        	  if (startWindow==0 || amplitude<minAmplitude[bin/2])
        	  {
        		   minAmplitude[bin/2] = Math.abs(window[bin]);
        	  }
           }
           
           double temp = 0;
           for (double amp: minAmplitude)
           {
               temp += amp;
           }
           
           // Ratio of new energy to the original.
           if (energy - temp > 0)  ratio = energy / (energy - temp);
           
           System.arraycopy(window, 0, windows[rows], 0, FFTSize*2);
           rows++;
           if (startWindow + step + FFTSize*2 - 1 >= complex.length) break;
        }
        
        for (int w=0; w<rows; w++)
        {
        	// Perform SPECTRAL subtraction
        	for (int bin=0; bin<windows[w].length; bin+=2)
        	{
        		if (Math.abs(1.0* windows[w][bin]) < minAmplitude[bin/2])
        				System.out.println("error");
        		if (windows[w][bin]>0) windows[w][bin] -= minAmplitude[bin/2];
        		else windows[w][bin] += minAmplitude[bin/2];
        	}
        	
        	// Perform inverse FFT
            windows[w] = ifft(windows[w]);
        }


        // Perform overlap and add to recompute the signal
        int row = 0;
        for (int offset=0; offset<complex.length/2; offset += step/2)
        {
           if (row == rows) break;
           startWindow = offset;
           if (startWindow + FFTSize - 1 >= complex.length/2)
           {
               startWindow = complex.length - FFTSize;
           }
           
           for (int w=0; w<FFTSize; w++)
           {
        	   if (windows[row]!=null)
        	       amplitudeData[startWindow+w] += windows[row][w] * ratio;
           }
           row++;
        }
     	return amplitudeData;
    }
    
    /**
     * 
     * @param hertz unwarped frequency in hertz
     * 
     * <pre>
     *    The standard formula: 13 atan(0.00076*f) + 3.5 atan((f/7500)^2)
     *    Is only an approximation and has no invoice
     *    
     *    Many speech recognition systems use the formula in this method
     *    because it has an invoice. The formula varies between using
     *    constants (6,600) to (7,700). The latter is more faithful to 
     *    the bark scale. The one used here compresses frequencies more,
     *    and therefore is more sensitive to the lower bands.
     *    
     *    If one uses the (7,700) version, make sure to alter the inverse 
     *    from (6,300) to (7,350)
     *    
     * </pre>
     * @param mel true if warping to MEL, false if bark
     * @return warped frequency
     */
    @SuppressWarnings("SameParameterValue")
    static double warp(double hertz, boolean mel)
    {
    	if (mel)
    	{
    		return (2595.0 * (Math.log10(1.0+hertz/700)));
    	}
    	else
    	{
            double x = hertz / 600;
            return (6.0 * Math.log(x + Math.sqrt(x * x + 1)));    		
    	}
    	
    }
    
    /**
     * 
     * @param warpedFreq unwarped frequency in hertz
     * @param mel true if un warping from MEL, false if bark
     * @return unwarped frequency
     */
    @SuppressWarnings("SameParameterValue")
    static double unwarp(double warpedFreq, boolean mel)
    {
    	
    	if (mel)
    	{
    		return (700.0 * (Math.pow(10.0, (warpedFreq /2595.0)) - 1.0));
    	}
    	else
    	{
  	       double x = warpedFreq / 6.0;
           return (300.0 * (Math.exp(x) - Math.exp(-x)));
    	}
    	
    }
}       // End of FastFourierTransform class
