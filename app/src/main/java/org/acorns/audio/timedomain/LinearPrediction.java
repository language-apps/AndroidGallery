/* LinearPrediction.java
 * 
 *   @author  HarveyD
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

import org.acorns.audio.AudioDefaults;

/** Class to apply linear prediction to a signal
 * 
 * Contributions by: David Minne, Brian Smith, and Dan Harvey in Spring 2011
 * Edited by: Dan Harvey, Summer 2011
 */
public class LinearPrediction 
{
	private final static double EPSELON = 0.00001;
	
    private double[] clip;
    private Filter filter;
    
    private int windowSize, windowShift;

    public LinearPrediction(double[] clip, double sampleRate)
    {
        this.clip = clip;
        
        filter = new Filter();
        
        windowSize = (int)((sampleRate/1000)*AudioDefaults.getWindowSize());
        windowShift = (int)((sampleRate/1000)*AudioDefaults.getWindowShift());
    }
    
    /** Compute LPC coefficients on a portion of an audio signal
     * 
     * @param data The audio signal
     * @param P The number of coefficients
     * @param start The starting point in the signal
     * @param end The ending point of the signal
     * @return coefficient array (or null if not possible)
     */
    public static double[] getLPCCoefficients(double[] data, int P, int start, int end)
    {
        if (end + P>=data.length) end = data.length;

        // If at the end of the signal, just return all zeroes.
        if (end<start) return new double[P];

        double[][] lpcArray = createLPCArray(data, AudioDefaults.TOEPLITZ, P, start, end);
        if (lpcArray==null) return null;
        
        double[] coefficients= new double[lpcArray.length];
        solveToeplitz(lpcArray, coefficients);
        return coefficients;
    }

    /** Perform linear prediction on a portion of audio
     *  @return double array if successful, null otherwise
     */
	@SuppressWarnings("unused")
    public double[] lpc()
    {
        int type = AudioDefaults.getLPCType();
        int P = AudioDefaults.getLPCCoefficients();
        
        double[] linearPrediction = new double[clip.length];
        System.arraycopy(clip, 0, linearPrediction, 0, P);

        for(int i=0, start=P, end=windowSize + P;
                     end<clip.length;
                     i++,start=i*windowShift,end=start+windowSize)
        {
            double[][] lpcArray = createLPCArray(clip, type, P,start, end);
            if (lpcArray==null) return null;
            
            double[] coefficients=
                 computeLPCCoefficients(type==AudioDefaults.TOEPLITZ, lpcArray);
            if (coefficients==null) return null;

           double[] frameLPC = filter.convolute(clip, coefficients, start, end);
           
           // Minimize amplification or attenuation of signal.
           double clipSum = sumAmplitudes(clip, start, end);
           double frameSum = sumAmplitudes(frameLPC, 0, end-start);
           if (frameSum!=0)
           {
        	   // Don't filter when too much distortion
	           if (Math.abs(1.0 - clipSum/frameSum) > .1)
	           {
	        	   System.arraycopy(clip, start, linearPrediction, start, end-start);
	        	   continue;
	           }

	           for (int s=0; s<end-start; s++)
	        	   frameLPC[s] = frameLPC[s] * clipSum / frameSum;
	           
           }
           System.arraycopy(frameLPC, 0, linearPrediction, start, end-start);
        }
        return linearPrediction;
    }
    
    /** Normalized log LPC prediction error (Rabiner Chapter 10)
	 *     
	 * @param P The number of LPC coefficients
	 * @param coefficients The LPC coefficients
	 * @param signal The audio signal
	 * @return The normalized prediction error in decibels
	 */
	public static double getNormalizedLPCError(int P, double[] coefficients, double[] signal)
	{
		int L = signal.length;
		
		double lpcError = getLPCError(P, coefficients, signal);
		double R0 = computeRK(0,  signal);
		
		double result =  10 * Math.log10(EPSELON + lpcError/L); 
		result -= 10 * Math.log10(EPSELON + R0/L);
		return result;
	}
	
    /** LPC prediction error (Rabiner Chapter 9)
	 *     
	 * @param P The number of LPC coefficients
	 * @param coefficients The LPC coefficients
	 * @param signal The audio signal
	 * @return The normalized prediction error in decibels
	 */
	private static double getLPCError(int P, double[] coefficients, double[] signal)
	{
		double R0 = computeRK(0, signal);
		double predictEnergy = 0;
		
		for (int k=1; k<=P; k++)
		{
			predictEnergy += computeRK(k, signal) * coefficients[k-1];
		}
		
		return Math.abs(R0 - predictEnergy);
	}
	
    /** LPC gain (Rabiner Chapter 9)
	 *     
	 * @param P The number of LPC coefficients
	 * @param coefficients The LPC coefficients
	 * @param signal The audio signal
	 * @return The normalized prediction error in decibels
	 */
	public static double getGain(int P, double[] coefficients, double[] signal)
	{
		return Math.sqrt(getLPCError(P, coefficients, signal));
	}
	
    /** Method to compute R(j) for Toeplitz auto correlation
     * @param clip The audio signal
     * @param start The starting point of the clip
     * @param end The ending point of the clip
     * @param j the first R index
     * @return The calculated R value
     * 
     */
    private static double R(double[] clip, int start, int end, int j)
    {
    	double total = 0.0;
    	for (int index = start; index < end; index++)
    	{
    		if (index + j < clip.length)
    			total += clip[index] * clip[index + j];
    	}
    	return total;
    }
    
	/** Compute RK given k and a frame of a predicted signal 
	 * 
	 * @param k The K index value
	 * @param frame The predicted frame of a signal
	 * @return computed RK value
	 */
	private static double computeRK(int k, double[] frame)
	{
		double L = frame.length;
		if (L<=0) return 0;
		
		double RK = 0;
		for (int m=0; m < L- k; m++)
		{
			RK += frame[m] * frame[m+k];
		}
		return RK;
	}
	
    
    /** Compute the absolute sum of samples in a frame
     * 
     * @param signal A PCM signal array
     * @param start Starting offset
     * @param end Ending offset
     * @return absolute sum of amplitude values
     */
    private double sumAmplitudes(double[] signal, int start, int end)
    {
    	double sum = 0;
    	for (int i=start; i<end; i++)
    	{  sum += Math.abs(signal[i]); }
    	return sum;
    }

    /** Create a LPC double dimension array from a portion of a signal
     * @param signal The time domain signal
     * @param type of linear prediction (PERIODIC, CHOLESKI, TOEPLITZ)
     * @param P The number of LPC coefficients
     * @param first The initial signal offset
     * @param last The ending signal offset
     * @return The filled in PxP+1 array for solving the LPC equations
     */ 
    private static double[][] createLPCArray
	            (double[] signal, int type, int P, int first, int last)
	{
           double[][] matrix = null;

           switch(type)
           {   case AudioDefaults.TOEPLITZ:
                  matrix = new double[P][P+1];

        	   	  // Compute the Toepllitz R values
        	   	  double[] RValues = new double[P+1];
        	   	  for (int j=0; j<=P; j++)
        	   		  RValues[j] = R(signal, first, last, j);
        	   	  
        	   	  // Fill the independent variable constant rows and column values
        	   	  int index;
        	   	  for (int row=0; row<P; row++)
        	   	  {
        	   		  for (int col=0; col<P; col++)
        	   		  {
        	   			  index = (row - col >= 0) ? row-col : col - row;
        	   			  matrix[row][col] = RValues[index];
        	   		  }
        	   	  }
        	   	  
        	   	  // Fill the last column with the constant values
        	   	  for (int row=0; row<matrix.length; row++)
        	   	  {
        	   		  matrix[row][P] = RValues[row+1];
        	   	  }
        	   	  
                  break;
               
	           case AudioDefaults.CHOLESKI:
	              matrix = new double[P][P+1];
                  for (int row=1; row<=P; row++)
                  {
                     for (int col=1; col<=P; col++)
                     {
                        matrix[row - 1][col - 1] 
                                  = theta(signal,first,last,row,col);
                     }
                     matrix[row-1][P] = theta(signal, first, last, row, 0);
                  }
 		          break;
           }  // end switch
		return matrix;
    } // end computeLPCArray()
    
    /** Method to compute THETA i,j (Sum 1 through P, clip[n-j] * clip[n-k]

     * @param clip The audio signal
     * @param start The starting point of the clip
     * @param end The ending point of the clip
     * @param j the first theta index
     * @return The calculated theta value
     */
    private static double theta(double[] clip, int start, int end, int j, int k)
    {
        double sum = 0;
        for (int n=start; n<end; n++) 
        { 
        	if (n-j >= 0 && n-k >= 0) 
        		sum += clip[n-j] * clip[n-k]; }
        return sum;        
    }
    
    /** Method to compute the LPC coefficients
     * @param array two dimension array (Px(P+1))
     * @return The coefficients to use for linear prediction
     */
    private static double[] computeLPCCoefficients(boolean toeplitz, double[][]array)
    {  
        int N = array.length;
        double[] coefficients = new double [N];

        if (toeplitz) 
        {
        	solveToeplitz(array, coefficients);
        }
        else
        {
	        // Perform the CHOLESKY decomposition
        	double[][] LT;
        	try
        	{
	           LT = cholesky(array); // Not positive definite
        	}
        	catch (Exception e) { return null; }
	
	        double[] y = new double[N];
	        for (int i = 0; i < N; i++)
	        {  
	           y[i] = array[i][N];
	           for (int j = 0; j < i; j++)  { y[i] -= LT[i][ j] * y[j];  }
  	           if (LT[i][i]==0) throw new ArithmeticException();
	           y[i] /= LT[i][ i];
	        }
	
	        // Backward solve
	        for (int i = N - 1; i >= 0; i--) 
	        {
	           coefficients[i] = y[i];
	           for (int j = i + 1; j < N; j++)
	           {
	                coefficients[i] -= LT[j][i] * coefficients[j];
	           }
  	           if (LT[i][i]==0) throw new ArithmeticException();
	           coefficients[i] /= LT[i][i];
	        }
        }
        return coefficients;
   }
    
    /** Solve the toeplitz two-dimension matrix
     * 
     * @param array Two dimension matrix to solve
     * @param coefficients Two the solution to return
     * @return alpha value (energy in the Frame - Levinson Durbin alpha)
     */
    @SuppressWarnings({"ManualArrayCopy", "unused", "UnusedReturnValue"})
    private static double[] solveToeplitz(double[][] array, double[] coefficients)
    {
        double[][] a = new double[array.length+1][array.length+1];
        double[] k = new double[array.length+1];

        double alpha = array[0][0];

	    for (int step = 1; step <= array.length; step++)
	    {
            k[step] = array[step-1][array[step-1].length-1];
	        for (int i = 1; i < step;i++)
                    k[step] -= a[step - 1][i] * array[step - i][0];

	        if (alpha==0) return coefficients;

	        k[step] /= alpha;
	        alpha *= (1-Math.pow(k[step],2));
	        a[step][step] = k[step];
	        for (int j = 1; j <= step - 1; j++)
                    a[step][j] = a[step-1][j] - (k[step]*a[step-1][step-j]);
	     }
	    
	     // Loads the final values into the answer array
	     for (int i = 1; i < a.length; i++) 
	     {
            coefficients[i-1] = a[a.length-1][i];
	     }     
	     return coefficients;
    }
  
    /** Compute the CHOLESKY decomposition algorithm
     * 
     * @param A The input matrix (note: It is NxN+1, the last column is ignored)
     * @return A decomposed matrix
     */
    private static double[][] cholesky(double[][] A) 
    {
       int N  = A.length; double[][] L = new double[N][N];

       for (int i = 0; i < N; i++)  
       {
         for (int j = 0; j <= i; j++) 
         {
            double sum = 0.0;
            for (int k = 0; k < j; k++) 
            {
                sum += L[i][k] * L[j][k];
            }
            if (i == j) L[i][i] = Math.sqrt(A[i][i] - sum);
            else        L[i][j] = 1.0 / L[j][j] * (A[i][j] - sum);}

           if ((L[i][i]) <= 0) 
           {
              throw new RuntimeException
                  ("matrix is not positive definite");
           }
       }
       return L;
    }  //  End of Cholesky()
    
    
    /* This routine computes the cepstral coefficients from LPC parameteres
     * This algorithm originated from the MathWorks DSP System Toolbox
     *  given the prediction vector.
     *  
     */  
    public static double[] lpcToCepstral( int P, int C, double[] lpc, double gain)
	{
		double[] cepstral = new double[C];
	
	    cepstral[0] = (gain<EPSELON) ? EPSELON : Math.log(gain);
	    for (int m=1; m<=P; m++)
	    {
	    	if (m>=cepstral.length) break;
	    	cepstral[m] = lpc[m-1];
	    	for (int k=1; k<m; k++)
	    	{
	    		cepstral[m] += k * cepstral[k] * lpc[m-k-1];
	    	}
	    	cepstral[m] /= m;
	    }
	    
	    for (int m=P+1; m<C; m++)
	    {
	    	cepstral[m] = 0;
	    	for (int k=m-P; k<m; k++)
	    	{
	    		cepstral[m] += k * cepstral[k] * lpc[m-k-1];
	    	}
	    	cepstral[m] /= m;
	    }
    	return cepstral;
	}
   
}   // End of LinearPrediction class
