/*
 * ResampleAudio.java
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

import org.acorns.audio.frequencydomain.LowPassFilter;


public class ResampleAudio 
{
	/**
	  * DownSampling given Audio signal 
	  * @param samples array of PCM data
	  * @param currentSamplingRate current sample rate
	  * @param targetSamplingRate desired sample rate
	  * @return down sampled signal
	  */   
	 public static double[] changeFrameRate(double[] samples, int currentSamplingRate, int targetSamplingRate)
	 {
		if (targetSamplingRate == currentSamplingRate) return samples;
		
        if(targetSamplingRate > currentSamplingRate)
        {
        	// Up sample first and then appropriately downsample
        	double ratio = (double)targetSamplingRate/currentSamplingRate;
        	int N = 1<<(int) Math.ceil(Math.log(ratio)/Math.log(2));
        	
        	double[] upSamples = new double[samples.length * N];
        	for (int i=0; i<samples.length; i++)
        	{
        		upSamples[i*N] = samples[i];
        	}
        	samples = upSamples;
        	currentSamplingRate *= N;
        }
        
        // **** Filtering to Remove Aliasing ****** 
        double filterCutof = 0.5 * (double) targetSamplingRate/currentSamplingRate;
        //System.out.println("filterCutof: "+filterCutof);
        LowPassFilter filter = new LowPassFilter(filterCutof);
        
        samples = filter.apply(samples);
       	if (Thread.currentThread().isInterrupted()) return null;

       	double duration = (double) samples.length / currentSamplingRate;
        //System.out.println("duration: "+duration);
        int newSampleLen = (int) Math.floor(duration * targetSamplingRate) ;
        //System.out.println("New Sample Length: "+newSampleLen);
        double fraction = (double)currentSamplingRate / targetSamplingRate;
        //System.out.println("Fraction: "+fraction);
        
        int value = (samples.length == newSampleLen) ? -1 : 0;
        double[] newSignal = new double[newSampleLen + value];
        for(int i=0;i<newSignal.length;i++)
        {
        	if (Thread.currentThread().isInterrupted()) return null;

            double posIdx =  fraction * i;
            int nVal = (int) Math.floor(posIdx);
            double diffVal = posIdx - nVal;
            
            // Linear Interpolation 
            newSignal[i] = (diffVal * samples[nVal+1]) + ((1 - diffVal) * samples[nVal]);
            
        }
        return newSignal;
	 }
	 
}   // End of ResampleAudio class
