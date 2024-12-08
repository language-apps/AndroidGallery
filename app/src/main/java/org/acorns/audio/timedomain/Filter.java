/*
 * Filter.java
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

package org.acorns.audio.timedomain;

import java.util.Arrays;

import org.acorns.audio.AudioDefaults;

/**
 *
 * Class to create and apply a variety of time domain filters
 */
public class Filter
{
	private static final double EPSELON = 0.00001;

    /** Constructor
     * 
     * Note: The complex indices are assumed zero because we are in the
     *       time domain. It is a complex array to simplify possible
     *       subsequent FFT calculations.
     */
    public Filter()   {}

    /**Performs a pre-emphasis on complex data to emphasize the higher frequencies
     *         The algorithm computes y(n) = x(n)-factor * x(n-1)
     *
     *  @param complex array of complex numbers (the imaginary part are zeroes)
     *  @param windowSize size of window
     *  @param prior ending value from the previous window
     *  @return new prior value
     */
    @SuppressWarnings("unused")
    public static double preEmphasizer(double[] complex, int windowSize, double prior)
    {
        float factor = AudioDefaults.getEmphasisFactor();
        double newPrior =  complex[2*windowSize-2];

        for (int i=windowSize*2-2;i>=2;i-=2) complex[i] -= complex[i-2]*factor;
        complex[0] -= factor * prior;
        return newPrior;
    }


    /** Create a smoothing window for a frame
     *  @return the frame after applying the window function
     */
    public double[] makeWindowFilter(int windowSize)
    {
        int type = AudioDefaults.geWindowType();
        switch (type)
        {
            case AudioDefaults.BLACKMAN:
                return createBlackmanWindow(windowSize);
            case AudioDefaults.HAMMING:
                return createHammingWindow(windowSize);
            case AudioDefaults.HANNING:
                return createHanningWindow(windowSize);
            case AudioDefaults.NOWINDOW:
                return null;
            default: throw new NumberFormatException();
        }
    }
    
    

    /** Apply the window to the signal (complex version)
     *
     * @param size The size of the window
     * @param window The window filter
     * @param complex The complex array of signal values (real part = 0)
     * @return The windowed signal
     */
    public double[] applyWindow(int size, double[] window, double[] complex)
    {
        if (window==null) return complex;
        // The real numbers are in the even indices 0,2,4...
        // The imaginary values are all zeroes at this point
        for(int i=0;i<size*2;i+=2)
            complex[i]=complex[i]*window[i/2];
        return complex;
    }
    
    /** Apply the window to the signal (real version)
    *
    * @param window The window filter
    * @param samples The complex array of signal values (real part = 0)
    * @return The windowed signal
    */
    @SuppressWarnings({"unused", "SameParameterValue"})
    public double[] applyWindow(double[] window, double[] samples)
    {
    	if (window==null) return  window;
        for(int i=0;i<window.length;i++)
            samples[i] = samples[i]*window[i];
        return samples;
    }

    /** Create a hamming window to  minimize the signal boundary discontinuities
     *  @param filterSize the size of the filter
     *  @return the hamming window function.
     */
    public static double[] createHammingWindow(int filterSize)
    {
        double[] window = new double[filterSize];
        double c = 2*Math.PI / (filterSize - 1);
        for (int h=0; h<filterSize; h++)
            window[h] = 0.54 - 0.46*Math.cos(c*h);
        return window;
    }

    /** Create a Blackman window to minimize the boundary signal discontinuities
     *  @param filterSize the size of the filter
     *  @return the Blackman window function.
     */
    public static double[] createBlackmanWindow(int filterSize){
        double[] window = new double[filterSize];
        double c = 2*Math.PI / (filterSize - 1);
        for (int h=0; h<filterSize; h++)
            window[h] = 0.42 - 0.5*Math.cos(c*h) + 0.08*Math.cos(2*c*h);
        return window;
    }

    /** Create a HANNING window to  minimize the signal boundary discontinuities
     *  @param filterSize the size of the filter
     *  @return the HANNING window function.
     */
    private static double[] createHanningWindow(int filterSize)
    {
        double[] window = new double[filterSize];
        double c = 2*Math.PI / (filterSize - 1);
        for (int h=0; h<filterSize; h++)
            window[h] = 0.5 - 0.5*Math.cos(c*h);
        return window;
    }
    
    /** Create a Sync band pass window filter.
     * @param cutOff Frequencies above this value pass (0->0.5).
     * @param F Filter size
     * @param window window type to apply
     * @return low pass filter or null if none
     */
    public static double[] makeWindowedSincLowPassFilter(double cutOff, double[] window, int F)
    {
        if (window==null) return null;

        if (F%2==0) F++; // FIR low pass filter must be an odd length
        
        int middle = (F-1)/2;
        double[] kernel = new double[F];
        double sum = 0;
        double cutoff = 2 * Math.PI * cutOff;

        for (int i=0; i<middle; i++) 
        {
            kernel[i] = Math.sin(cutoff*(i-middle))/(i-middle) * window[i];
            kernel[F-i-1] = kernel[i];
            sum += 2*kernel[i];
        }
        
        kernel[middle] = cutoff;
        sum += kernel[middle];
        
        // Normalize total area to 1.00:
        for (int i=0; i<F; i++) 
        {
            kernel[i] /= sum;
        }
        return kernel;
    }

    /** Create a Sync band pass window filter.
     * @param lowFreq Frequencies above this value pass (0->0.5).
     * @param highFreq Frequencies below this value pass (0->0.5).
     * @param F Filter size
     * @param window type of window
     * @return band pass filter or null if none
     */
    private static double[] makeWindowedSincFilter(double lowFreq, double highFreq, int F, double[] window)
    {
        // Create function for hamming or blackman window
        if (window==null) return null;

        if (F%2==0) F++;

        // Create the two filters.
        // complex.length>>1 is the number of complex numbers divided by 2.
        // highFreq/(complex.length>>2) should be less than 0.5.
        double cutOffHigh = 2 * Math.PI * highFreq;
        double cutOffLow  = 2 * Math.PI * lowFreq;
        int middle = F/2;
        double highSum = 0, lowSum = 0;
        double[] highPass = new double[F];
        double[] lowPass  = new double[F];
        for (int f=0; f<F; f++)
        {
            if (f==middle)
            {
                highPass[f] = cutOffHigh;
                lowPass[f]  = cutOffLow;
            }
            else
            {  highPass[f] = Math.sin(cutOffHigh * (f-middle))/(f-middle);
               lowPass[f]  = Math.sin(cutOffLow  * (f-middle))/(f-middle);
            }
            
            highPass[f] *= window[f];
            lowPass[f]  *= window[f];

            highSum     += highPass[f];
            lowSum      += lowPass[f];
        }

        // Normalize the second filter and convert it to high pass.
        double[] bandPass = new double[F];
        for (int f=0; f<F; f++)
        {
            if (highSum!=0) highPass[f] /= -highSum;
            if (lowSum!=0)  lowPass[f]  /= lowSum;
        }
        highPass[middle] += 1.0;
 
        if (lowFreq<=0) return highPass;
        if (highFreq>=0.5) return lowPass;
        
        // Combine the two filters and convert form band-reject to band-pass.
        for (int f=0; f<F; f++)
        {
            bandPass[f] = -lowPass[f] - highPass[f];
        }
        bandPass[middle] +=1;
        return bandPass;

    }   // End of makeSincFilter()

    /** Create a Sync band pass window filter (using default window).
     * @param lowFreq Frequencies above this value pass (0->0.5).
     * @param highFreq Frequencies below this value pass (0->0.5).
     * @param F Filter size
     * @return band pass filter or null if none
     */
    @SuppressWarnings("unused")
    public double[] makeWindowedSincFilter(double lowFreq, double highFreq, int F)
    {
        // Create function for default window (e.g. Hamming or Blackman)
        double[] window = makeWindowFilter(F);
        return makeWindowedSincFilter(lowFreq, highFreq, F, window);
    }   // End of makeSincFilter()

    /** Create time domain brick wall band pass filter
     * 
     * @param lowFreq Frequencies above this cutoff pass (0->0.5)
     * @param highFreq Frequencies below this cutoff fail (0->0.5)
     * @param F Filter length
     * @return filter coefficients or null if none
     * 
     * Note if lowFreq < 0, this highFreq becomes the cutoff
     * 		if highFreq < 0, this becomes a low pass filter
     * 		if (low and high < 0, return null;
     */
    @SuppressWarnings("unused")
    public static double[] makeSincFilter(double lowFreq, double highFreq, int F)
    {
    	if (lowFreq<0 && highFreq<0) return null;
        if (lowFreq > highFreq) return null;
        if (F%2==0) F++;

        double cutOffHigh = 2 * Math.PI * highFreq;
        double cutOffLow  = 2 * Math.PI * lowFreq;
        int middle = F/2;
        double highSum = 0, lowSum = 0;
        double[] highPass = new double[F];
        double[] lowPass  = new double[F];
        for (int f=0; f<F; f++)
        {
            if (f==middle)
            {
                highPass[f] = cutOffHigh;
                lowPass[f]  = cutOffLow;
            }
            else
            {  highPass[f] = Math.sin(cutOffHigh * (f-middle))/(f-middle);
               lowPass[f]  = Math.sin(cutOffLow  * (f-middle))/(f-middle);
            }
            highSum     += highPass[f];
            lowSum      += lowPass[f];
        }

        // Normalize the second filter and convert it to highpass.
        double[] bandPass = new double[F];
        for (int f=0; f<F; f++)
        {
            if (highSum!=0) highPass[f] /= -highSum;
            if (lowSum!=0)  lowPass[f]  /= lowSum;
        }
        highPass[middle] += 1.0;
 
        if (lowFreq<=0) return highPass;
        if (highFreq>=8000 || highFreq<0) return lowPass;
        
        // Combine the two filters and convert form band-reject to band-pass.
        for (int f=0; f<F; f++)
        {
            bandPass[f] = -lowPass[f] - highPass[f];
        }
        bandPass[middle] +=1;
        return bandPass;
    }

    	
    /** Convolve the time domain data with a band pass frequency filter
     *
     * @param bandPass The band pass filter
     * @param complex The array of complex numbers (real parts are zero)
     * @return filtered data
     */
    @SuppressWarnings({"UnusedReturnValue", "unused"})
    public double[] applyFilterComplex(double[] bandPass, double[] complex)
    {
        if (bandPass==null)  return complex;
        if (complex.length/2 < bandPass.length) return complex;

        double[] filteredData = new double[complex.length];

        int samples = complex.length / 2, s2;
        for (int s=bandPass.length - 1; s<samples; s++)
        {   s2 = s<<1;
            filteredData[s2] = filteredData[s2+1] = 0.0;
            for (int f=0; f<bandPass.length; f++)
            {  filteredData[s2] += complex[s2-(f<<1)]*bandPass[f];  }
        }
        // Move filtered data back to sample array (front portion is cleared).
        return filteredData;
    }
    
	/** Convolve a filter with a portion of a signal
	 * @param signal The signal to filter
	 * @param filter The filter to convolve with signal
	 * @return The filtered signal
	 */
	double[] convolute (double[] signal, double[] filter,
                                  int first, int last)
     { 
       int size = last - first + filter.length - 1; int offset;
       double[] filteredSignal = new double[size];

       for (int i=first; i<(first+size); i++) {
            for (int j=0; j<filter.length; j++) {
                 offset = i - j;
                 if (offset<first) break;
                 if (offset>=last)
                 {  j = i - last + 1;
                     offset = i - j;
                 }
                 filteredSignal[i-first] += signal[offset] * filter[j];
               }
        }
           return filteredSignal;
    } // end convolute

	/** Apply the filter to the audio signal
	 * 
	 * @param signal The buffer of samples
	 * @param a The iir coefficients
	 * @param b the fir coefficients
	 * @param real true if a real array, false if a complex array
	 * @return filtered signal
	 */
	@SuppressWarnings({"UnusedReturnValue", "unused"})
	static double[] convolution(double[] signal, double[] b, double[] a, boolean real)
	{
	   int increment = (real)? 1: 2;
	   double[] output = new double[signal.length + (b.length - 1)*increment];
	   
	   for (int i = 0; i < signal.length; i += increment) 
	   {
	       for (int j = 0; j < b.length; j++)
	       {
	    	   if (i-j*increment>=0) output[i] += b[j]*signal[i - j*increment];
	       }
	       
	       if (a!=null)
	       {
	           for (int j = 1; j < a.length; j ++)
	           {
	               if (i-j*increment>=0) output[i] -= a[j] * output[i - j*increment];
	           }
	       }
	   }
	   return output;
	}

	// Local methods for extracting features
	/** Compute the difference between adjacent signal samples
	 * 
	 * @param signal The signal to auto correlate
	 * @return The total absolute difference
	 */
	@SuppressWarnings("unused")
	public double autoCorrelate(double[] signal)
	{
		if (signal==null || signal.length == 0) return 0;
		
		double deltaSum = autoCorrelate(signal, 0, 1);
		double correlate0 = autoCorrelate(signal, 0, 0);
		double correlate1 = autoCorrelate(signal, 1, 1);
		return deltaSum / Math.sqrt(correlate0 * correlate1);
	}


	/** Compute pitch lag for constants i and j
	 * 
	 * @param signal The audio signal
	 * @param i The first lag 
	 * @param j The second lag
	 * @return The pitch lag value
	 */
	
	private double autoCorrelate(double[] signal, int i, int j)
	{
		double sum = 0;
		int N = signal.length;
		for (int n=0; n<N; n++)
		{
			try
			{
				sum += signal[n-i] * signal[n-j];
			}
			catch (ArrayIndexOutOfBoundsException e)
            {
                e.printStackTrace();
            }
		}
		return sum;
	}
	
	/** Get the fractal dimension of an audio speech window
	 * 		Uses the Higuchi Fractal Dimension (HFD) technique
	 * 
	 * @param x The audio signal
	 * @return The calculated fractal dimension
	 */
	@SuppressWarnings("unused")
	public double getHiguchiFractalDimension(double[] x)
	{
		// Create Higuchi Array
		int KMAX = x.length/4;
		double[] L = new double[KMAX];
		for (int k=1; k<=KMAX; k++)
		{
			L[k-1] = Lk(x, k)/k;
		}
		
		// Find slope of L using a least squares linear best fit
		double slope = bestFit(L, KMAX);
		return Math.log10(Math.abs(EPSELON + slope));
	}
	
	/** Compute L(k) for the Higuchi Fractal Dimension (HFD) calculation
	 * 
	 */
	private double Lk(double[] x, int k)
	{
		double sum = 0;
		for (int m=1; m<=k; m++)
		{
			sum += LmK(x, m, k);
		}
		return sum / k;
	}
	
	/** Compute L(m, k) for Higuchi Fractal Dimension (HFD) calculation
	 * 		Consists of a difference summation, 
	 * 			which is based on: x[m], x[m+k], x[m+2k], x[m+(N-m)/k*k]
	 * 
	 * @param x The audio signal window
	 * @param m The m component of the index
	 * @param k The k component of the index
	 * @return The Higuchi L(m,k) result
	 */
    @SuppressWarnings("IntegerDivisionInFloatingPointContext")
	private double LmK(double[] x, int m, int k)
	{
		int N = x.length;
		double sum = 0;
		for (int i=1; i<=(N-m)/k; i++)
		{
			sum += Math.abs(x[m - 1 + i*k] - x[m - 1 +(i-1)*k]);
		}
		return sum * (N - 1) / ((N - m)/k * k);
	}

	/** Perform linear regression on the supplied array
	 * 
	 * @param y Linear regression input array
	 * @param MAX number of points
	 * @return slope of best fit line
	 */
	private double bestFit(double[] y, int MAX)
	{	
		// Compute the X�X matrix (aij)
		double a11 = MAX * (MAX+1) * (2*MAX+1)/6; // sum xi*xi = d
		double a12 = MAX * (MAX + 1) / 2; // sum xi = b = c

		
		// Compute sum yi and sum (xi * yi) where  bi = X�y
		double sumXY=0, sumY=0;
		for (int i=0; i<MAX; i++)	
		{   
			sumXY += (i+1) * y[i];   
			sumY += y[i];    
		}
		
		// Return (X�X)-1 * X�y, where (X�X)-1 is the inverse of X�X
		double numerator = -a12*sumY + MAX*sumXY ; // -c*sumXY+a*sumY
		double denominator =  a11 * MAX - a12 * a12;// ad - bc
	    return (denominator == 0) ? 0 : numerator / denominator;
	}


	/** Get the fractal dimension of an audio speech window
	 * 		Uses the Katz Fractal Dimension (HFD) technique
	 * 		d = maximum distance from the mean value; L = sum of differences
	 * 
	 * @param x The audio signal
	 * @return The calculated fractal dimension
	 */
    @SuppressWarnings("unused")
	public double getKatzFractalDimension(double[] x)
	{
		int N = x.length;
		if (N<=1) return 0;
		
		double L = 0, diff, D = 0;
		for (int i=1; i<x.length; i++)
		{
			L += Math.abs(x[i] - x[i-1]);
			diff = Math.abs(x[0]-x[i]);
			if (diff> D) D = diff;
		}
		if (D<EPSELON || L<EPSELON) return 0;
		return Math.log10(N-1)/ (Math.log10(D/L) + Math.log10(N-1));
	}
	
	/** Get the fractal dimension of an audio speech window
	 * 		Uses the Box Counting Fractal Dimension (HFD) technique
	 * 
	 * @param signal The audio signal
	 * @param B The number of boxes to use
	 * @return The calculated fractal dimension
	 */
    @SuppressWarnings({"ForLoopReplaceableByForEach", "unused"})
	public double getBoxCountingFractalDimension(double[] signal, int B)
	{
		double max = signal[0];
		double min = signal[0];
		
		// find min and max of the audio wave
		for (int m = 1; m < signal.length; m++)
		{
			if(signal[m] > max)	max = signal[m];
			if(signal[m] < min)	min = signal[m];
		}
		
		int[][] color = new int[signal.length][signal.length];
		for(int i = 0; i < color.length; i++)
		{
			Arrays.fill(color[i], -1); // Initial color
		}

		double [] boxCounts = new double[B];
		
		int x, y, boxes, count;
		double boxSize;
		for (int index = 0; index < B; index++)
		{
			count  = 0;
			boxes = 1<<index;
			boxSize = (double)signal.length / boxes;
			for (int offset = 0; offset < signal.length; offset++)
			{
				x = (int)(offset / boxSize);
				if (x==boxes) x--;
				y = (int) (boxes * (signal[offset] - min) / (max - min));
				if (y == boxes) y--;
				
				// Add to count only if not included in the current iteration
				if(color[x][y] < index)
				{
					color[x][y] = index;  
					count++;
				}	
			}			
			boxCounts[index] = count;
		}

		double slope = bestFit(boxCounts, B);
		return Math.log10(Math.abs(EPSELON + slope));
	}
	
}   // End of Filter class
