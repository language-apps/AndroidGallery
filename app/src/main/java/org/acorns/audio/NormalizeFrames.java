/*
 *   NormalizeFrames.java - Class to normalize the features from an array of audio feature frames
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

package org.acorns.audio;

public class NormalizeFrames 
{
	/** Perform mean normalize (subtract feature mean from each frame)
	 * 
	 * @param features Array of feature frames
	 * @param start Starting feature
	 * @param end Ending feature
	 * @return Normalized feature array (same as the parameter passed)
	 */
	@SuppressWarnings("ForLoopReplaceableByForEach")
    public static double[][] meanNormalization(double[][] features, int start, int end)
    {
        double mean;

        //calculate the sum of each MFCC column
        for (int column = start; column<end; column++)
        {
     	   mean = 0;
     	   for (int row=0; row<features.length; row++)
     	   {
     		   mean += features[row][column];
     	   }
     	   mean = mean / features.length;
     	   
     	   for (int row=0; row<features.length; row++)
     	   {
     		   features[row][column] -= mean;
     	   }
        }
        return features;
    }   // end of meanNormalization

    /** Variance Normalization: Scale each feature to [-1,1] by dividing each feature
     *      by the feature's variance over all frames
     *      
     *  Note: Assumes a zero mean
     *
     *  @param features CEPSTRUM coefficient array for each frame of a recording
	 *  @param start Starting feature
	 *  @param end Ending feature
	 *  @return Normalized feature array (same as the parameter passed)
     */
	@SuppressWarnings("ForLoopReplaceableByForEach")
	public static double[][] varianceNormalization(double[][] features, int start, int end)
    {
       double variance;
       if (features.length==1) return features;

       //calculate the sum of each MFCC column
       for (int column = start; column<end; column++)
       {
    	   variance = 0;
    	   for (int row=0; row<features.length; row++)
    	   {
    		   variance += features[row][column] * features[row][column];
    	   }
    	   variance /= (features.length - 1);
    	   
    	   for (int row=0; row<features.length; row++)
    	   {
    		   if (variance!=0) features[row][column] /= Math.sqrt(variance);
    	   }
       }
       return features;
    }  // End of varianceNormalization()
    
    /** Normalization of skew (third moment) for each feature over all frames. Transforms
     *  the distribution to be more normal.
     *  
     *  Note: Assumes a zero mean and unity variance
     *
     *  @param features CEPSTRUM coefficient array for each frame of a recording
	 *  @param start Starting feature
	 *  @param end Ending feature
	 *  @return Normalized feature array (same as the parameter passed)
     */
	@SuppressWarnings("ForLoopReplaceableByForEach")
	public static double[][] skewNormalization(double[][] features, int start, int end)
    {
        double momentN, momentNPlus1, momentNMinus1, value;
        double[] coefficients = new double[end - start];
        int N = 3;

        //calculate the sum of each MFCC column
        for (int column = start; column<end; column++)
        {
     	   momentN = momentNPlus1 = momentNMinus1 = 0;
     	   for (int row=0; row<features.length; row++)
     	   {
     		   value = features[row][column];
     		   momentN += Math.pow(value, N);
     		   momentNPlus1 += Math.pow(value, N+1);
     		   momentNMinus1 += Math.pow(value, N-1);
     	   }
     	   if (momentNPlus1 != momentNMinus1)
     	      coefficients[column-start] = -momentN / (3 * (momentNPlus1 - momentNMinus1));
        }

    	double a;

    	//calculate the sum of each MFCC column
        for (int column = start; column<end; column++)
        {
           a = coefficients[column-start];
     	   for (int row=0; row<features.length; row++)
     	   {
     		  
     		   value = features[row][column];
     		   features[row][column] = a * value * value + value - a;
     	   }
        }        
        return features;
    }   // End of skewNormalization()
    


}  // End of NormalizeFrames class
