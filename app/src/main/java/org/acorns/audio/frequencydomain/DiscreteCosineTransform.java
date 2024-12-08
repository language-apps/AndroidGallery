/*
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

package org.acorns.audio.frequencydomain;

public class DiscreteCosineTransform 
{
    private double[] cosines;
    private double[][] forwardCosines, backCosines;
	
    private int coefficients, spectrumSize;
    
    private double sqrt_1_div_n;
    private double sqrt_2_div_n;
    
    /** Produce a MEL Cosine transform filter
     * @param spectrumSize size of input spectrum
     * @param coefficients size of output spectrum
     */
    DiscreteCosineTransform(int spectrumSize, int coefficients)
    {
    	this.coefficients = coefficients;
    	this.spectrumSize = spectrumSize;
    	
    	sqrt_1_div_n = Math.sqrt(1.0 / spectrumSize); 
    	sqrt_2_div_n = Math.sqrt(2.0 / spectrumSize);
    	
        cosines = new double[4*spectrumSize];
        for (int i=0; i<cosines.length; i++)
        {
        	cosines[i] = Math.cos(Math.PI * i / (2.0*spectrumSize));
        }
        
        if ( (spectrumSize&spectrumSize-1)==0 && spectrumSize==coefficients)
		{
 		    int dimension = (int)(Math.log(coefficients)/Math.log(2));
			forwardCosines = new double[dimension][];
			backCosines = new double[dimension][];
			int index = -1;
			for (int n=1; n<coefficients; n*=2)
			{
			    forwardCosines[++index] = new double[n];
			    backCosines[index] = new double[n];
				double phase = 0;
				for (int j=0; j<n; j++)
				{
	               forwardCosines[index][j] = 0.5 / Math.cos(Math.PI/n * (phase +0.25));
	               backCosines[index][j] = Math.cos(Math.PI/n + phase);
	               phase += 0.5;
				}
			}
		}
    }    

    @SuppressWarnings("unused")
    public double[] forwardDCT(double[] block)  { return fastDCT(block,forwardCosines); }
    
    
    /** Calculate the discrete transform in O(N lg N) time
     * 
     * Restriction: The spectrum length must be a power of two
     * 
     * @param block The input spectrum
     * @return The computed DCT transform
     */
	private double[] fastDCT(double[] block, double[][] cosineArray)
	{   
	 	int N = block.length;
		double[] front = new double[N];
		double[] back = new double[N];
		double[] temp;

		System.arraycopy(block, 0, front, 0, N);
	    
	    int j = N, step = 1, gap, shift, index = cosineArray.length - 1;
		while(j > 1)
	    {
	        N = N >> 1;
	        gap = 0;
	        for(int x = 0; x < step; x++)
	        {               
	            for(int i= 0; i<N; i++)
	            {
	                shift = gap + (N<<1) - i - 1;
	                back[gap + i] = front[gap + i] + front[shift];
	                back[gap + N + i] = (front[gap + i] - front[shift]) * cosineArray[index][i];
	            }
	            gap += N<<1;
	        }
	 
	        temp = front;
	        front = back;
	        back = temp;
	        j = j >> 1;
	        step = step << 1;
		    index--;
	    }
		
	    step = step >> 2;
	    j = j << 1;
	    N <<= 1;
	    while(j < block.length)
	    {
	        gap = 0;
	        for(int x = 0; x < step; x++)
	        {                   
	            for(int i = 0; i<N - 1; i++)
	            {               
	                back[gap + (i << 1)] = front[gap + i];              
	                back[gap + (i << 1) + 1] = front[gap+N+i] + front[gap+N+i+1];                
	            }
	            back[gap + ((N - 1) << 1)] = front[gap + N - 1];  
	            back[gap + (N << 1) - 1] = front[gap + (N << 1) - 1];   
	            gap += (N<<1);
	        }
	
	        temp = front;
	        front = back;
	        back = temp;
	        j = j << 1;
	        step = step >> 1;
	        N <<= 1;
	    }
	
		front[0] *= sqrt_1_div_n;
		for (int i=1; i<N; i++) { front[i] *= sqrt_2_div_n;	}
		return front;
	}
	
   /** Perform an inverse fast cosine transform on the frequency domain
    *  @param signal Array of doubles representing the frequency domain
    *  @return inverse DCT containing the time domain
    */
   @SuppressWarnings("unused")
   public double[] backDCT(double[] signal)
	{
        if (signal==null) return null;
        
        signal = fastDCT(signal, backCosines);
        return signal;
	}
    
    /** Apply the forward DCT to a given spectrum (Used for the MEL spectrum)
     * @param spectrum the spectrum data
     * @param transform array to hold the output array
     * @return result of applying the discrete Cosine transform
     */
	@SuppressWarnings({"ConstantConditions", "unused"})
	public double[] applyForwardTransform(double[] spectrum, double[] transform)
    {
        int index;
        for (int i = 0; i <coefficients; i++)
        {   if (coefficients > 0)
            {   
                for (int j = 0; j < spectrumSize; j++)
                {  
                    index = i * (2 * j + 1) % (4*spectrumSize); 
                	transform[i] += (spectrum[j] * cosines[index]); 
                }
                transform[i] *= (i==0)?sqrt_1_div_n : sqrt_2_div_n;
            }
        }
        return transform;
    }
    
    /** Apply the backward DCT to a given spectrum (Used for the PLP spectrum)
     * @param spectrum the spectrum data
     * @param transform array to hold the output array
     * @return result of applying the discrete Cosine transform
     */
    double[] applyBackTransform(double[] spectrum, double[] transform)
    {
        int index;
         for (int i = 0; i < coefficients; i++) 
        {
	        for (int j = 0; j < spectrumSize; j++) 
	        {
                index = i * (2 * j + 1) % (4*spectrumSize); 
            	transform[i] += (spectrum[j] * cosines[index] * ((j==0) ? sqrt_1_div_n : sqrt_2_div_n) ); 
	        }
	        transform[i] /= spectrumSize;
        }
        return transform;
    	
    }
}       // End of DiscreteCosineTransform
