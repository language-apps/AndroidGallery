/*
 * SpellCheck.java
 *
 *   @author  HarveyD
 *   @version 3.00 Beta
 *
 *   Copyright 2007-2015, all rights reserved
 */

package org.acorns.language;

import java.util.ArrayList;

/** Compute the distance between a word and how it is spelled */
@SuppressWarnings("unused")
public class SpellCheck
{
	   // Offsets to audio difference results
	   public static final int SOURCE_FRAMES = 0;
	   public static final int TARGET_FRAMES = 1;
	   public static final int DIFFERENCE = 2;
	   private static final int RESULT_LEN = 3;

	   // Offsets into audio cost array elements
	   private static final int X = 0;
	   private static final int Y = 1;
	   private static final int NUMERATOR = 2;
	   private static final int DENOMINATOR = 3;
	   
	   static final int BOUNDARY = -1;	// Boundary frame
	   static final int SILENCE = -2;	// Silence frame

	   // Dynamic Time Warp cost formulas
	   private static final int[][] TYPE1 
	        = { {-1, -1, 7,  4}, {-1, 0, 1, 1}, {0, -1, 1, 1}, 
	          };
	   
	   private static final int[][] TYPE2
	   		= { {-1, -1, 7, 4}, {-1, 0, 1, 1}, {0, -1, 1, 1}, 
		        {-2, -1, 3, 1}, {-1, -2, 3, 1} 
		      };
	   
	   private static final int[][] TYPE5
	   		= { {-1, -1, 7, 4}, {-1,  0, 1, 1}, { 0, -1, 1, 1},
		   		{-2, -1, 3, 1}, {-1, -2, 3, 1},
		   		{-2,  0, 2, 1}, {-3, -1, 4, 1}, { 0, -2, 2, 1}, {-1, -3, 4, 1}
		   	  };
	   
	   private static final int[][] TYPE6
			= { {-1, -1, 7, 4}, {-1,  0, 1, 1}, { 0, -1, 1, 1},
	  		    {-2, -1, 3, 1}, {-1, -2, 3, 1},
	  		    {-3, -2, 5, 1}, {-2, -3, 5, 1}
	  	      };

	   /* The active dynamic time warp approach */
	   private static final int[][] COST = TYPE6;

   /** Method to compute the distance between two strings
    *  @param source first source string
    *  @param target second target string
    *  @return the integer distance between the two strings
    */
   public static int editDistance(String source, String target)
   { 
      int sLen = source.length(), tLen = target.length();
      if (sLen==0) return tLen;
      if (tLen==0) return sLen;
     
      // d is a table with lenStr1+1 rows and lenStr2+1 columns
      int[][] d = new int[sLen+1][tLen+1];
     
      // Initialize the arrays
      for (int i=0; i<=sLen; i++) d[i][0] = i;
      for (int j=1; j<=tLen; j++) d[0][j] = j;
     
      // Perform the algorithm
      int cost, costDelete, costInsert, costSubstitute, costTranspose;
      for (int i=1; i<=sLen; i++)
      {  for (int j=1; j<=tLen; j++)
         {  if (source.charAt(i-1)==target.charAt(j-1)) cost = 0;
            else cost = 1;
           
            costDelete     = d[i-1][j] + 1;
            costInsert     = d[i][j-1] + 1;
            costSubstitute = d[i-1][j-1] + cost;   
           
            d[i][j] = costDelete;
            if (d[i][j] > costInsert)     d[i][j] = costInsert;
            if (d[i][j] > costSubstitute) d[i][j] = costSubstitute;
            if(i > 1 && j > 1 && source.charAt(i-1) == target.charAt(j-2)
                              && source.charAt(i-2) == target.charAt(j-1))
            {               
               costTranspose = d[i-2][j-2] + cost;
               if (d[i][j] > costTranspose) d[i][j] = costTranspose;
            }
         }     // End of inner j loop
      }        // End of outer i loop
     return d[sLen][tLen];
   }  
   
   /** Method to compute the distance between clusters of two audio recordings
    *  @param source first source array of integers
    *  @param target second target array of integers
    *  @return {source compressed length, target compressed length, difference}
    */
   public static double[] audioDistance(int[] source, int[] target)
   { 
	   source = removeDuplicates(source);
	   target = removeDuplicates(target);
	   
      int len = source.length, tLen = target.length;
      if (len==0) return new double[]{len, tLen, tLen};
      if (tLen==0) return new double[]{len, tLen, len};
     
      // d is a table with lenStr1+1 rows and lenStr2+1 columns
      double[][] d = new double[len+1][tLen+1];
     
      // Initialize the arrays
      for (int i=0; i<=len; i++) d[i][0] = i;
      for (int j=1; j<=tLen; j++) d[0][j] = j;
     
      // Perform the algorithm
      double cost, minCost;
      int num, denom, x, y;
      for (int i=1; i<=len; i++)
      {  for (int j=1; j<=tLen; j++)
         {  
    	    minCost = 0;
    	    for (int k=0; k<COST.length; k++)
    	    {
    	    	x = COST[k][X] + i;
       	    	if (x<0) x = 0;
       	     
       	    	y = COST[k][Y] + j;
       	    	if (y<0) y = 0;

       	    	num = COST[k][NUMERATOR];
       	    	denom = COST[k][DENOMINATOR];
 
       	    	if (COST[k][X]<0 && COST[k][Y]<0) 
       	    	{
       	    		cost = (source[x]==target[y]) ? d[x][y] : d[x][y] + (double)num/denom;
       	    		if (k==0) minCost = cost;
       	    	}
       	    	else 
       	    		cost = d[x][y] + (double)num/denom; 
    	    	
    	    	if (minCost > cost) minCost = cost;
    	    	if (minCost==0) break;
    	    }
            d[i][j] = minCost;
         }     // End of inner j loop
      }        // End of outer i loop

      double[] result = new double[RESULT_LEN];
      result[SOURCE_FRAMES] = len;
      result[TARGET_FRAMES] = tLen;
      result[DIFFERENCE] = d[len][tLen];
      return result;
   }  
   
   /** Remove duplicate values from an array of integers
    * 
    * @param data Array of integers, possibly with duplicate values
    * @return array of integers with duplicates removed and any values less than zero
    */
   private static int[] removeDuplicates(int[] data)
   {
	   if (data==null || data.length==0) return new int[0];
	   
	   int len = data.length;
	   ArrayList<Integer> newData = new ArrayList<>();
	   for (int i=0; i<len; i++)
	   {
		   if (data[i]>=0 || i==0) newData.add(data[i]);
		   else if (data[i]!=data[i-1]) newData.add(data[i]);
	   }
	   
	   len = newData.size();
	   int[] result = new int[len];
	   for (int i=0; i<len; i++)   result[i] = newData.get(i);
	   return result;
   }

   
}  // End of Spell Check class

