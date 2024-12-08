package org.acorns.phonemes;

import java.util.Arrays;
import java.util.Locale;

/** Class to perform operations on matrices */
public class Matrix  
{
	private static final double MIN = 1.0E-6;
	
	/** Matrix subtraction
	 * 
	 * param a first two dimension matrix
	 * @param b second two dimension matrix
	 * @return a - b
	 */
	@SuppressWarnings("unused")
	public static double[][] minus(double[][] a, double[][] b)
	{
		double[][] out = new double[a.length][];
		for (int i=0; i<a.length; i++)
		{
			out[i] = new double[a[i].length];
			for (int j=0; j<a[i].length; j++)
			{
				out[i][j] = a[i][j] - b[i][j];
			}
		}
		return out;
	}
	
	/** Matrix addition
	 * 
	 * param a first two dimension matrix
	 * @param b second two dimension matrix
	 * @return a + b
	 */
	@SuppressWarnings("unused")
	public static double[][] plus(double[][] a, double[][] b)
	{
		double[][] out = new double[a.length][];
		for (int i=0; i<a.length; i++)
		{
			out[i] = new double[a[i].length];
			for (int j=0; j<a[i].length; j++)
			{
				out[i][j] = a[i][j] + b[i][j];
			}
		}
		return out;
	}
	
	/** Perform a matrix inversion operation
	 * 
	 * @param A The input matrix
	 * @return The inverted matrix (or null if A is singular)
	 */
	@SuppressWarnings("\tManualArrayCopy")
	static double[][] invert(double[][] A)
	{
		if (A==null) return null;
	    int n = A.length;
		double[][] inverse = new double[n][];
		for (int row=0; row<n; row++)
		{
			inverse[row] = A[row].clone();
		}
		
	    int[] row = new int[n], col = new int[n];
	    double[] temp = new double[n];
	    
	    int hold , I_pivot , J_pivot;
	    double pivot, abs_pivot;

	    // set up row and column interchange vectors
	    for(int k=0; k<n; k++) { row[k] = col[k] = k ;  }
	    
	    // begin main reduction loop
	    for(int k=0; k<n; k++)
	    {
	        // find largest element for pivot
	    	pivot = inverse[row[k]][col[k]] ;
		    I_pivot = J_pivot = k;
		    for(int i=k; i<n; i++)
		    {
		       for(int j=k; j<n; j++)
		       {
		          abs_pivot = Math.abs(pivot) ;
		          if(Math.abs(inverse[row[i]][col[j]]) > abs_pivot)
		          {
		              I_pivot = i ;
		              J_pivot = j ;
		              pivot = inverse[row[i]][col[j]] ;
		          }
		       }
	        }
		    
		    if(Math.abs(pivot) < 1.0E-10)   { return null; }
		    
		    hold = row[k];
		    row[k]= row[I_pivot];
		    row[I_pivot] = hold ;
		    
		    hold = col[k];
		    col[k]= col[J_pivot];
		    col[J_pivot] = hold ;
		    
	        // reduce about pivot
		    inverse[row[k]][col[k]] = 1.0 / pivot ;
		    for(int j=0; j<n; j++)
		    {
		    	if(j != k)
		    	{
		    		inverse[row[k]][col[j]] = inverse[row[k]][col[j]] * inverse[row[k]][col[k]];
		    	}
		    }
		    
		    // inner reduction loop
		    for(int i=0; i<n; i++)
		    {
		    	if(k != i)
		    	{
		    		for(int j=0; j<n; j++)
		    		{
		    			if( k != j )
		    			{
		    				inverse[row[i]][col[j]] = inverse[row[i]][col[j]] - inverse[row[i]][col[k]] *
	                                   inverse[row[k]][col[j]] ;
		    			}
		    		}
		    		inverse[row[i]][col [k]] = - inverse[row[i]][col[k]] * inverse[row[k]][col[k]] ;
		    	}
		    }
	    }	// end main reduction loop

	    // unscramble rows
	    for(int j=0; j<n; j++)
	    {
	    	for(int i=0; i<n; i++) { temp[col[i]] = inverse[row[i]][j]; }
	    	for(int i=0; i<n; i++) 	{ inverse[i][j] = temp[i]; }
	    }
	    
	    // unscramble columns
	    for(int i=0; i<n; i++)
	    {
	    	for(int j=0; j<n; j++) 	{ temp[row[j]] = inverse[i][col[j]];	}
	    	for(int j=0; j<n; j++) 	{ inverse[i][j] = temp[j]; }
	    }
	    return inverse;
	    
	} // end invert
	
	/** Multiply two matrices 
	 * 
	 * @param A Input matrix
	 * @param B Input matrix
	 * @return A * B Matrix
	 */
	@SuppressWarnings("unused")
	static double[][] multiply(double[][] A, double[][] B) throws ArrayIndexOutOfBoundsException
	{
        int mA = A.length;
        int nB = B[0].length;
        int nK = A[0].length;

        double[][] C = new double[mA][nB];
        for (double[] row:C)
        {
        	Arrays.fill(row, 0.0);
		}

        for (int row = 0; row < mA; row++)
        {
            for (int col = 0; col < nB; col++)
            {
                for (int k = 0; k < nK; k++)
                {
                    C[row][col] += (A[row][k] * B[k][col]);
                }
            }
        }
        return C;
    }	
	/** Create NxN identity matrix
	 * 
	 * @param n Size of desired matrix
	 */
	@SuppressWarnings("unused")
	public static double[][] identity(int n)
	  {
		  double[][] A = new double[n][n];

		  for(int i=0; i<n; i++)
		  {
			  Arrays.fill(A[i], 0.0);
			  A[i][i] = 1.0;
		  }
		  return A;
	  } // end identity
	  
	  /** Compare two matrices
	   * 
	   * @param A First input matrix
	   * @param B Second input matrix
	   * @return true if equal, false otherwise
	   */
	  @SuppressWarnings("unused")
	  public static boolean isEqual(double[][] A, double[][] B)
	  {
		  for (int row=0; row<A.length; row++)
		  {
			  for (int col=0; col<A.length; col++)
			  {
				  if (Math.abs(A[row][col] - B[row][col]) > 1E-10) return false;
			  }
		  }
		  return true;
	  }
	  
	  /** Compute the determinant of a square matrix
	   * 
	   * @param data The input NxN matrix
	   * @return computed result
	   */
      static double determinant(double[][] data)
      {
			if (data==null || data.length==0) return 0.0;

		    // Clone the original array so the original is preserved				
            int row;
			double[][] clone = new double[data.length][];
			for (row=0; row<data.length; row++)
			{
				clone[row] = data[row].clone();
			}
			data = clone;

         double temp, total = 1.0, ratio;
         for (int col=0; col<data.length; col++)
         {
            // Swap row with non zero first coefficient if necessary
            if (Math.abs(data[col][col])<=MIN)
            {
               for (row=col+1; row<data.length; row++)
               {
                  if (Math.abs(data[row][col]) > MIN)
                  {
                     for (int c=col; c<data.length; c++)
                     {
                        temp = data[row][c];
                        data[row][c] = data[col][c];
                        data[col][c] = temp;
                     }
					 total *= -1; // Swapping rows changes the sign
                     break;
                  }
               }  // End for row
               if (row>=data.length) 
            	   return Double.NaN;
            }  // End if
         
	         // For each row, reduce the first coefficient to zero
	         for (row = col+1; row<data.length; row++)
	         {
	            ratio = data[row][col] / data[col][col];
	            for (int c = col; c<data.length; c++)
	            {
	               data[row][c] -= data[col][c] * ratio;
	            }
	         }
	         total *= data[col][col];
			}  // End for col
			
			return total;
      }     // End of Determinant method
      
      /** Create a string representation of the matrix */
	  @SuppressWarnings({"unused", "ForLoopReplaceableByForEach"})
	  public static String toString(double[][] matrix)
      {
    	  StringBuilder builder = new StringBuilder();
    	  String buffer;
    	  for (int row=0; row<matrix.length; row++)
    	  {
    	     for (int col=0; col<matrix[0].length; col++)
    	     {
    	    	 buffer = String.format(Locale.getDefault(), "%8.2f ", matrix[row][col] );
    	    	 builder.append(buffer);
    	     }
    	     builder.append("\n");
    	  }
    	  return builder.toString();
      }
      
      /** Create a transpose matrix 
       * 
       * @param A Original matrix
       * @return The transpose of A
       */
	  @SuppressWarnings("unused")
      public static double[][] transpose (double[][] A) 
      {
          int m = A.length, n = A[0].length;
          double[][] C = new double[n][m];
          for (int i = 0; i < m; i++) 
          {
             for (int j = 0; j < n; j++) 
             {
                C[j][i] = A[i][j];
             } 
          }
          return C;
       }

} // End of Matrix class
