package org.acorns.audio.timedomain;

/*

The original has been modified by Dan Harvey for for interation with ACORNS.
The original copyright notice follows.

Copyright (c) 2009-2011
       Speech Group at Informatik 5, Univ. Erlangen-Nuremberg, GERMANY
       Korbinian Riedhammer
       Stefan Hollos  http://www.exstrom.com/stefan/stefan.html
       Richard Hollos http://www.exstrom.com/richard/richard.html
               
       This file is part of the Java Speech Toolkit (JSTK).

       The JSTK is free software: you can redistribute it and/or modify
       it under the terms of the GNU General Public License as published by
       the Free Software Foundation, either version 3 of the License, or
       (at your option) any later version.

       The JSTK is distributed in the hope that it will be useful,
       but WITHOUT ANY WARRANTY; without even the implied warranty of
       MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
       GNU General Public License for more details.

       You should have received a copy of the GNU General Public License
       along with the JSTK. If not, see <http://www.gnu.org/licenses/>.
       
       The algorithms in this file were ported from C from Stefan and Richard 
       Hollos at http://www.exstrom.com/journal/sigproc.
*/

import java.util.Locale;
import androidx.annotation.NonNull;

/**
* A Butterworth low/high pass and band pass/reject filter. The implementation
* is based on a C version from http://www.exstrom.com/journal/sigproc
*
* @author sikoried
*/
public class Butterworth
{
    private int n; // Filter order
    private double [] a; // IIR filter coefficients
    private double [] b; // FIR filter coefficients
    
    private final static int N = 4;
    
    /**
     * Generate a Butterworth low/high pass filter at the given cutoff frequency
     *
     * @param freq in Hz / frame rate (.00 -> .05)
     * @param lowp true for lowpass, false for high pass
     */
    public Butterworth(double freq, boolean lowp) 
    {
       this.n = N;
	   freq *= 2;
	   
       double scale = computeScale(n, freq, lowp);
       double [] b = computeB(n, lowp);
       for (int i = 0; i < b.length; ++i) b[i] *= scale;
		  
       double [] a = computeA(n, freq);
       setCoefficients(b, a);
    }

    /**
     * Generate a Butterworth band pass/reject filter at the given cutoff
     * frequencies.
     *  @param freq1 in Hz / frame rate (.00 -> 0.5)
     * @param freq2 in Hz / frame rate (.00 -> 0.5)
     * @param pass true for bandpass, false for bandreject
     */
    @SuppressWarnings("unused")
    Butterworth(double freq1, double freq2, boolean pass)
    {
       this.n = N;
       freq1 *= 2;
       freq2 *= 2;
       
       double scale = computeScale(n, freq1, freq2, pass);
       double [] b = computeB(n, freq1, freq2, pass);
       for (int i = 0; i < b.length; ++i)  b[i] *= scale;
           
       double [] a = computeA(n, freq1, freq2, pass);
       setCoefficients(b, a);
    }

    /**
     * Compute the B coefficients for low/high pass. The cutoff frequency is not
     * required.
     *
     * @param n Filter order
     * @param lowp low pass true, high pass false
     * @return The filtered signal
     */
    private static double [] computeB(int n, boolean lowp) 
	  {
       double [] ccof = new double [n + 1];
     
       ccof[0] = 1;
       ccof[1] = n;
     
       for (int i = 2; i < n/2 + 1; ++i) 
		  {
          ccof[i] = (n-i+1) * ccof[i-1]/i;
          ccof[n-i] = ccof[i];
       }
     
       ccof[n - 1] = n;
       ccof[n] = 1;

       if (!lowp) 
		  {
          for (int i = 1; i < n + 1; i += 2)  ccof[i] = -ccof[i];
       }
       return ccof;
    }

    /**
     * Compute the B coefficients for a band pass/reject. Both cutoff frequencies
     * need to be specified as radians.
     * @param n The filter order
     * @param f1 frequency in radians (2 * hz / samplef)
     * @param f2 frequency in radians (2 * hz / samplef)
     * @param pass Band pass true; band reject false
     * @return The computed coefficients
     */
    private static double [] computeB(int n, double f1, double f2, boolean pass) {
            double [] ccof = new double [2*n + 1];
            if (pass) {
                    double [] tcof = computeB(n, false);

                    for (int i = 0; i < n; ++i) {
                            ccof[2*i] = tcof[i];
                            ccof[2*i + 1] = 0.;
                    }

                    ccof[2*n] = tcof[n];
            } else {
                    double alpha = -2. * Math.cos(Math.PI * (f2 + f1) / 2.) / Math.cos(Math.PI * (f2 - f1) / 2.);

                    ccof[0] = 1.;
                    ccof[1] = alpha;
                    ccof[2] = 1.;
                   
                    for (int i = 1; i < n; ++i) {
                            ccof[2 * i + 2] += ccof[2 * i];
                            for (int j = 2 * i; j > 1; --j)
                                    ccof[j + 1] += alpha * ccof[j] + ccof[j - 1];

                            ccof[2] += alpha * ccof[1] + 1.0;
                            ccof[1] += alpha;
                    }
            }
           
            return ccof;
    }

    /**
     * Compute the A coefficients for a low/high pass for the given frequency
     * @param n Filter order
     * @param f frequency in radians (2 * hz / samplef)
     * @return Computed coefficients
     */
    private static double [] computeA(int n, double f) {
            double parg;    // pole angle
            double sparg;   // sine of the pole angle
            double cparg;   // cosine of the pole angle
            double a;               // workspace variable
            double [] rcof = new double [2 * n]; // binomial coefficients

            double theta = Math.PI * f;
            double st = Math.sin(theta);
            double ct = Math.cos(theta);

            for (int k = 0; k < n; ++k) {
                    parg = Math.PI * (double) (2*k + 1) / (double) (2*n);
                    sparg = Math.sin(parg);
                    cparg = Math.cos(parg);
                    a = 1. + st * sparg;
                    rcof[2 * k] = -ct / a;
                    rcof[2 * k + 1] = -st * cparg / a;
            }

            // compute the binomial
            double [] temp = binomialMult(rcof);
           
            // we only need the n+1 coefficients
            double [] dcof = new double [n + 1];
            dcof[0] = 1.0;
            dcof[1] = temp[0];
            dcof[2] = temp[2];
            for (int k = 3; k < n + 1; ++k)
                    dcof[k] = temp[2*k - 2];

            return dcof;
    }

    /**
     * Compute the A coefficients for a band pass/reject
     * @param n Filter order
     * @param f1 frequency in radians (2 * hz / samplef)
     * @param f2 frequency in radians (2 * hz / samplef)
     * @param pass band pass true; reject false
     * @return Computed coefficients
     */
    private static double [] computeA(int n, double f1, double f2, boolean pass) {
            double parg;    // pole angle
            double sparg;   // sine of pole angle
            double cparg;   // cosine of pole angle
            double a;               // workspace variables

            double cp = Math.cos(Math.PI * (f2 + f1) / 2.);
            double theta = Math.PI * (f2 - f1) / 2.;
            double st = Math.sin(theta);
            double ct = Math.cos(theta);
            double s2t = 2. * st * ct; // sine of 2*theta
            double c2t = 2. * ct * ct - 1.0; // cosine of 2*theta

            double [] rcof = new double [2*n]; // z^-2 coefficients
            double [] tcof = new double [2*n]; // z^-1 coefficients

            for (int k = 0; k < n; ++k) {
                    parg = Math.PI * (double) (2 * k + 1) / (double) (2 * n);
                    sparg = Math.sin(parg);
                    cparg = Math.cos(parg);
                    a = 1.0 + s2t * sparg;
                    rcof[2*k] = c2t / a;
                    rcof[2*k + 1] = (pass ? 1. : -1.) * s2t * cparg / a;
                    tcof[2*k] = -2.0 * cp * (ct + st * sparg) / a;
                    tcof[2*k + 1] = (pass ? -2. : 2.) * cp * st * cparg / a;
            }

            // compute trinomial
            double [] temp = trinomialMult(tcof, rcof);
           
            // we only need the 2n+1 coefficients
            double [] dcof = new double [2*n + 1];
            dcof[0] = 1.0;
            dcof[1] = temp[0];
            dcof[2] = temp[2];
            for (int k = 3; k < 2*n + 1; ++k)
                    dcof[k] = temp[2 * k - 2];
           
            return dcof;
    }

    /**
     * Compute the scale factor for the b coefficients for given low/high pass
     * filter.
     *
     * @param n Filter order
     * @param f frequency in radians (2 * hz / samplef)
     * @param lowp low pass true; high pass false
     * @return Computed coefficients
     */
    private static double computeScale(int n, double f, boolean lowp) 
    {
        double omega = Math.PI * f;
        double fomega = Math.sin(omega);
        double parg0 = Math.PI / (double)(2*n);
       
        double sf = 1.;
        for (int k = 0; k < n/2; ++k )
            sf *= 1.0 + fomega * Math.sin((double)(2*k+1)*parg0);

        fomega = (lowp) ? Math.sin(omega / 2.0):Math.cos(omega / 2.0);

        if (n % 2 == 1)
            sf *= fomega + (lowp ? Math.cos(omega / 2.0) : Math.sin(omega / 2.));
        sf = Math.pow( fomega, n ) / sf;

        return sf;
    }
   
    /**
     * Compute the scale factor for the b coefficients for the given band
     * pass/reject filter
     * @param n Filter order
     * @param f1 frequency in radians (2 * hz / samplef)
     * @param f2 frequency in radians (2 * hz / samplef)
     * @param pass band pass - true; band reject - false
     * @return Computed coefficient scale factor
     */
    private static double computeScale(int n, double f1, double f2, boolean pass)
    {
        double parg;      // pole angle
        double sparg;     // sine of pole angle
        double cparg;     // cosine of pole angle
        double a, b, c;   // workspace variables

        double tt = Math.tan(Math.PI * (f2 - f1) / 2.);
        if (pass)
            tt = 1. / tt;
       
        double sfr = 1.;
        double sfi = 0.;

        for (int k = 0; k < n; ++k ) {
                parg = Math.PI * (double)(2*k+1)/(double)(2*n);
                sparg = tt + Math.sin(parg);
                cparg = Math.cos(parg);
                a = (sfr + sfi)*(sparg - cparg);
                b = sfr * sparg;
                c = -sfi * cparg;
                sfr = b - c;
                sfi = a - b - c;
        }

        return 1. / sfr;
    }
   
    /**
     *  Multiply a series of binomials and returns the coefficients of the
     *  resulting polynomial. The multiplication has the following form:<b/>
     *  
     *  (x+p[0])*(x+p[1])*...*(x+p[n-1]) <b/>
     *  
     *  The p[i] coefficients are assumed to be complex and are passed to the
     *  function as an array of doubles of length 2n.<b/>
     *  
     *  The resulting polynomial has the following form:<b/>
     *  
     *  x^n + a[0]*x^n-1 + a[1]*x^n-2 + ... +a[n-2]*x + a[n-1] <b/>
     *  
     *  The a[i] coefficients can in general be complex but should in most cases
     *  turn out to be real. The a[i] coefficients are returned by the function
     *  as an array of doubles of length 2n.
     *  
     * @param p array of doubles where p[2i], p[2i+1] (i=0...n-1) is assumed to be the real, imaginary part of the i-th binomial.
     * @return coefficients a: x^n + a[0]*x^n-1 + a[1]*x^n-2 + ... +a[n-2]*x + a[n-1]
     */
    private static double [] binomialMult(double [] p) 
    {
       int n = p.length / 2;
       double [] a = new double [2 * n];

       for (int i = 0; i < n; ++i) 
       {
           for (int j = i; j > 0; --j) 
           {
              a[2*j] += p[2*i] * a[2*(j-1)] - p[2*i+1] * a[2*(j-1) + 1];
              a[2*j+1] += p[2*i] * a[2*(j-1)+1] + p[2*i+1] * a[2*(j-1)];
           }
           a[0] += p[2*i];
           a[1] += p[2*i+1];
      }
      return a;
 }

    /**
     *  Multiply a series of trinomials and returns the coefficients of the
     *  resulting polynomial. The multiplication has the following form:<b/>
     *  
     *  (x^2 + b[0]x + c[0])*(x^2 + b[1]x + c[1])*...*(x^2 + b[n-1]x + c[n-1]) <b/>
     *  
     *  The b[i], c[i] coefficients are assumed to be complex and are passed to
     *  the function as an array of doubles of length 2n.<b/>
     *  
     *  The resulting polynomial has the following form:<b/>
     *  
     *  x^2n + a[0]*x^2n-1 + a[1]*x^2n-2 + ... +a[2n-2]*x + a[2n-1] <b/>
     *  
     *  The a[i] coefficients can in general be complex but should in most cases
     *  turn out to be real. The a[i] coefficients are returned by the function
     *  as an array of doubles of length 2n.
     *  
     * @param b array of doubles where b[2i], b[2i+1] (i=0...n-1) is assumed to be the real, imaginary part of the i-th binomial.
     * @param c second array for the multiply
     * @return coefficients a: x^2n + a[0]*x^2n-1 + a[1]*x^2n-2 + ... +a[2n-2]*x + a[2n-1]
     */
     
  private static double [] trinomialMult(double [] b, double [] c) 
  {
      int n = b.length / 2;
      double [] a = new double [4 * n];

      a[0] = b[0];
      a[1] = b[1];
      a[2] = c[0];
      a[3] = c[1];

      for (int i = 1; i < n; ++i) 
      {
          a[2*(2*i+1)] += c[2*i] * a[2*(2*i-1)] - c[2*i+1]
                        * a[2*(2*i-1)+1];
          a[2*(2*i+1)+1] += c[2*i]*a[2*(2*i-1)+ 1]
                        + c[2*i+1]*a[2*(2*i-1)];

          for (int j = 2 * i; j > 1; --j) 
          {
             a[2*j] += b[2*i] * a[2*(j-1)] - b[2*i + 1]
                          * a[2*(j-1)+1] + c[2*i] * a[2*(j-2)]
                          - c[2*i+1] * a[2*(j-2)+1];
             a[2*j+1] += b[2*i] * a[2*(j-1)+1] + b[2*i+1]
                          * a[2*(j-1)] + c[2*i] * a[2*(j-2)+ 1]
                          + c[2*i+1] * a[2*(j-2)];
          }

          a[2] += b[2*i]*a[0]-b[2*i+1] * a[1] + c[2*i];
          a[3] += b[2*i]*a[1]+b[2*i+1] * a[0] + c[2*i+1];
          a[0] += b[2*i];
          a[1] += b[2*i+ 1];
     }

         return a;
    }

    /**
     * Set the filter coefficients stored in Matlab style. If a[0] not equal 1,
     * the filter coefficients are normalized by a[0].
     *
     * @param b The FIR coefficients
     * @param a The IIR coefficients
     */
    private void setCoefficients(double [] b, double [] a) 
    {
       this.b = b;
       this.a = a;
     
       if (a[0] != 1.) 
       {
           for (int i = 1; i < a.length; ++i)  a[i] /= a[0];
           for (int i = 0; i < b.length; ++i)  b[i] /= a[0];
      }
    }
    
    /** Get the filter coefficients
     * 
     * @return The Butterworth filter coefficients (or null if they don't exist)
     * 
     * 		A two x coefficient array. The first entry has the
     * 		FIR coefficients, the second has the IIR coefficients.
     */
    @SuppressWarnings("unused")
    public double[][] getCoefficients()
    {
    	if (a==null || b==null) return null;
    	
    	double[][] coefficients = new double[2][];
    	coefficients[0] = b.clone();
    	coefficients[1] = a.clone();
    	return coefficients;
    }
    
    /** Apply the Butterworth filter to an audio signal
     * 
     * @param signal The audio signal
     * @param real true if real data, false if complex data
     * @return the filtered signal
     */
    public double[] applyFilter(double[] signal, boolean real)
    {
    	return Filter.convolution(signal, b, a, real);
    }
    
    /**  Output string representation of this object */
    @SuppressWarnings({"ForLoopReplaceableByForEach"})
    public @NonNull String toString()
    {
       StringBuilder build = new StringBuilder("N = " + n + "\n");
       for (int i=0; i<a.length; i++)
       {
           build.append(String.format(Locale.getDefault(),"%8.3f ", a[i]));
       }
       build.append("\n");

       for (int i=0; i<b.length; i++) 
            build.append(String.format(Locale.getDefault(), "%8.3f ", b[i]));
       build.append("\n");
       return build.toString();                
    }

}
