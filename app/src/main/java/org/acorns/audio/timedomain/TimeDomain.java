/*
 * TimeDomain.java - class to set convert to and from audio to time domain.
 *
 *   @author  HarveyD
 *   @version 3.00 Beta
 *
 *   Copyright 2007-2015, all rights reserved
 */
package org.acorns.audio.timedomain;

public class TimeDomain 
{
   private static final double EPSELON = 0.00001;

   /** Method to remove the DC component from an audio signal
    * 
    * @param clip Audio signal to remove clip
    */
   @SuppressWarnings("ForLoopReplaceableByForEach")

   public static void removeDC(double[] clip) 
   {
      if (clip==null) return;
       
      double sum = 0;
      for (int i=0; i<clip.length; i++)
      {
          sum+= clip[i];
      }
      
      if (Math.abs(sum)<.005) return;
        
      int average = (int)Math.round(sum / clip.length);
      for (int i=0; i<clip.length; i++)
      {
          clip[i] -= average;
      }
   }
   
   /** Method to compute the number of zero crossings in a signal window
    * 
    * @param clip The audio signal
    * @param wStart The start of the  signal window
    * @param wEnd The end of the signal window
    * @param inc increment by one (real), or two (complex)
    */
   public static int zeroCrossings(double[] clip, int wStart, int wEnd, int inc)
   {
        int crossings = 0;
        for (int i=wStart; i<wEnd; i+=inc)
        {
            if (i<wStart+inc) continue;
            if (clip[i]*clip[i-inc]<0) crossings++;
        }
        return crossings;    
   }  
   
   /** Method to compute the average DECIBEL level of a frame
    * 
    * @param clip The audio signal
    * @param wStart The start of the  signal window
    * @param wEnd The end of the signal window
    * @param inc increment by one (real), or two (complex)
    */
   @SuppressWarnings("unused")
   public static double decibelLevel(double[] clip, int wStart, int wEnd, int inc)
   {
	 double total = 0;
	 for (int i=wStart; i<wEnd; i+=inc)
	 {
		 total += clip[i]*clip[i];
	 }
	 
	 int length = (wEnd - wStart)/inc;
		
	 if (total>EPSELON)
     {
		 return 10 * Math.log10(total/length);
		 
     }
	 else total = EPSELON;
	 
	 return total;
   }
}
