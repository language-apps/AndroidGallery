/*
 * Copyright 2004-2006 DFKI GmbH, but significantly modified by Dan Harvey 2014.
 * All Rights Reserved.  Use is subject to license terms.
 *uil
 * This file is part of MARY TTS.
 *
 * MARY TTS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.acorns.audio.frequencydomain;

import org.acorns.audio.timedomain.Filter;

/**
 * @author Marc Schr&ouml and Dan Harvey;
 *
 */
public class LowPassFilter 
{
    private int sliceLength;
    private int kernelLength;
    private FrameProvider frameProvider;
    private double[] transformedIR;
    
    /**
     * Create a new lowpass filter with the given a normalized cutoff frequency 
     * 
     * @param cutoff the cutoff frequency of the low pass filter,
     * 
     * The cutoff is expressed as a fraction of the sampling rate (0, 0.5].
     * For example, with a sampling rate of 16000 Hz and a desired cutoff frequency of 
     * 4000 Hz, the cutoff Frequency would have to be 0.25. 
     */
    public LowPassFilter(double cutoff) 
    {
        double normalizedTransitionBandwidth = 0.01; // transition bandwidth
        
        int kernelLength = (int) (4/normalizedTransitionBandwidth);
        if (kernelLength % 2==0) kernelLength++; // Kernel length must be odd
        
        if (cutoff <= 0 || cutoff >= 0.5) {
            throw new IllegalArgumentException("Normalised cutoff frequency must be between 0 and 0.5, got " + cutoff);
        }
        
		double[] window = Filter.createBlackmanWindow(kernelLength);
		double[] kernel = Filter.makeWindowedSincLowPassFilter(cutoff, window, kernelLength);

        // determine the length of the slices by which the signal will be consumed:
        // this is the distance to the second next power of two, so that the slice
        // will be at least as long as the kernel.
        int sliceLength = nextPowerOfTwoAbove(2*kernelLength) - kernelLength;

        if (kernel == null)
        {
            kernel = new double[0];
        }

        this.kernelLength = kernel.length;
        this.sliceLength = sliceLength;
        transformedIR = new double[sliceLength+kernel.length];
        System.arraycopy(kernel, 0, transformedIR, 0, kernel.length);

        // Save the complex FFT transform of the impulseResponse
        realTransform(transformedIR, false);
    }

    /**
     * Apply this filter to the given input signal. This method filters the entire signal,
     * and returns the entire filtered signal. For long signals, it is better to use apply(DoubleDataSource).
     * @param signal the signal to which this filter should be applied
     * @return the filtered signal.
     */
    public double[] apply(double[] signal)
    {
        int frameLength = sliceLength+kernelLength;
        
        // Need to start with zero padding of length impulseResponseLength:
        double[] paddedSource = new double[kernelLength + signal.length];
        System.arraycopy(signal, 0, paddedSource, kernelLength, signal.length);

        int headCutoff = kernelLength/2;
        int tailCutoff = kernelLength - headCutoff;

        // discard the initial padding of impulseResponseLength/2:
        frameProvider = new FrameProvider(paddedSource, frameLength, sliceLength, headCutoff, tailCutoff);
        
        // Assume worst case size
        double[] target = new double[signal.length+kernelLength]; 
        int targetPosition = 0, nRead;
        while ( (nRead=processBlock(target, targetPosition)) > 0)
        {
        	if (Thread.currentThread().isInterrupted()) return null;
            targetPosition += nRead;
        } 
        
        double[] result = new double[targetPosition];
        System.arraycopy(target, 0, result, 0, targetPosition);
        return result;
    }

    private static int nextPowerOfTwoAbove(int N)
    {
        return 1<<(int) Math.ceil(Math.log(N)/Math.log(2));
    }
    
    /**
     *     Calculates the Fourier transform of a set of n real-valued data points. Replaces this data (which
    is stored in array data[1..n]) by the positive frequency half of its complex Fourier transform.
    The real-valued first and last components of the complex transform are returned as elements
    data[1] and data[2], respectively. n must be a power of 2. This routine also calculates the
    inverse transform of a complex data array if it is the transform of real data. (Result in this case
    must be multiplied by 2/n.)
     * @param data array to which Fourier transform should be applied
     * @param inverse Compute Fourier transform or its inverse
     */
    private static void realTransform(double[] data, boolean inverse)
    {
  
        double c1 = 0.5;
        int n = data.length;
        
        double TWOPI = 2*Math.PI;

        double twoPi = -TWOPI;
        if (inverse) twoPi = TWOPI;
        double delta = twoPi / n;
        double wStepReal = Math.cos(delta);
        double wStepImag = Math.sin(delta);
        double wReal = wStepReal;
        double wImag = wStepImag;

        double c2;
        if (!inverse) {
            c2 = -0.5;
            transform(data, false); //The forward transform is here.
        } else {
            c2=0.5; //Otherwise set up for an inverse transform
        }
        int n4 = n>>2;
        for (int i=1;i<n4;i++) { //Case i=0 done separately below.
            int twoI = i<<1;
            int twoIPlus1 = twoI + 1;
            int nMinusTwoI = n - twoI;
            int nMinusTwoIPlus1 = nMinusTwoI + 1;
            double h1r = c1*(data[twoI]+data[nMinusTwoI]); //The two separate transforms are separated out of data.
            double h1i = c1*(data[twoIPlus1]-data[nMinusTwoIPlus1]); 
            double h2r = -c2*(data[twoIPlus1]+data[nMinusTwoIPlus1]);
            double h2i = c2*(data[twoI]-data[nMinusTwoI]);
            // Here they are recombined to form the true transform of the original real data.
            data[twoI] = h1r+wReal*h2r-wImag*h2i;
            data[twoIPlus1]=h1i+wReal*h2i+wImag*h2r;
            data[nMinusTwoI]=h1r-wReal*h2r+wImag*h2i;
            data[nMinusTwoIPlus1] = -h1i+wReal*h2i+wImag*h2r;
            // Next w is computed by complex multiplication with wStep
            // exp(i*(phi+delta)) = exp(i*phi)*exp(i*delta)
            double oldWReal = wReal;
            wReal = oldWReal*wStepReal - wImag*wStepImag;
            wImag = oldWReal*wStepImag + wImag*wStepReal;

        }
        if (!inverse) {
            double tmp = data[0];
            //Squeeze the first and last data together to get them all within the original array.
            data[0] += data[1];
            data[1] = tmp - data[1];
            data[n/2+1] = -data[n/2+1];
        } else { // inverse
            double tmp = data[0];
            data[0] = 0.5*(tmp+data[1]);
            data[1] = 0.5*(tmp-data[1]);
            data[n/2+1] = -data[n/2+1];
            transform(data, true);
        } 
    }
    
    private static double[] cosDelta;
    private static double[] sinDelta;
    
    static {
        double TWOPI = 2*Math.PI;
        int N = 32;
        cosDelta = new double[N];
        sinDelta = new double[N];
        for (int i=1;i<N; i++) {
            double delta = -TWOPI / (1<<i);
            cosDelta[i] = Math.cos(delta);
            sinDelta[i] = Math.sin(delta);
        }
    }

    /**
     * Carry out the FFT or inverse FFT, and return the result in the same arrays given
     * as parameters. This works exactly like #transform(real, imag, boolean),
     * but data is represented differently: the even indices of the input array
     * hold the real part, the odd indices the imag part of each complex number.
     * 
     * @param realAndImag the array of complex numbers to transform
     * @param inverse whether to calculate the FFT or the inverse FFT.
     */
    private static void transform(double[] realAndImag, boolean inverse)
    {
        if (realAndImag == null)
            throw new NullPointerException("Received null argument");
        int N = realAndImag.length>>1;
        int halfN = N>>1;
        // Re-order arrays for FFT via bit-inversion
        int iReverse = 0;
        for (int i=0; i<N; i++) 
        {
            if (i > iReverse) 
            {
                //System.err.println("Swapping " + Integer.toBinaryString(i) + " with " + Integer.toBinaryString(iReverse));
                int twoi = i<<1;
                int twoi1 = twoi+1;
                int twoirev = iReverse<<1;
                int twoirev1 = twoirev+1;
                double tmpReal = realAndImag[twoi];
                double tmpImag = realAndImag[twoi1];
                realAndImag[twoi] = realAndImag[twoirev];
                realAndImag[twoi1] = realAndImag[twoirev1];
                realAndImag[twoirev] = tmpReal;
                realAndImag[twoirev1] = tmpImag;
            }
            // Calculate iReverse for next round:
            int b = halfN;
            while (b>=1 && iReverse>=b) 
            {
                iReverse -= b;
                b >>= 1;
            }
            iReverse += b;
        }
        
        // Now real and imag are in the right order for the FFT.
        // FFT:
        // Look at blocks of increasing length blockLength;
        // in each block, pair the nth and the nPrime-th = (n+blockLength/2)th
        // element, and combine them using a factor w = exp(n*delta*I),
        // delta = (-) 2*PI/blockLength.
        for (int blockLength = 2, powerOfTwo=1; blockLength <= N; blockLength <<= 1, powerOfTwo++) 
        {
            double wStepReal = cosDelta[powerOfTwo];
            double wStepImag = sinDelta[powerOfTwo];
            if (inverse) wStepImag = -wStepImag;
            double wReal = 1;
            double wImag = 0;
            int halfBlockLength = blockLength>>1;
            for (int n=0; n<halfBlockLength; n++) 
            {
                // Do this for all blocks at once:
                for (int i=n; i<N; i+=blockLength) 
                {
                    int j = i+halfBlockLength;
                    // And now combine the ith and the jth element,
                    // according to s(n)=s_even(n)+ w_n*s_odd(n)
                    // where w_n = exp(-2*PI*I*n/blockLength)
                    // s[i] = s[i] + w*s[j]
                    // w_j = w_(i+halfBlockLength) = w_i*exp(-PI*I) = -w_i
                    // => s[j] = s[i] - w*s[j]
                    int twoi = i<<1;
                    int twoi1 = twoi+1;
                    int twoj = j<<1;
                    int twoj1 = twoj+1;
                    double tmpReal = wReal*realAndImag[twoj] - wImag*realAndImag[twoj1];
                    double tmpImag = wReal*realAndImag[twoj1] + wImag*realAndImag[twoj];
                    realAndImag[twoj] = realAndImag[twoi] - tmpReal;
                    realAndImag[twoj1] = realAndImag[twoi1] - tmpImag;
                    realAndImag[twoi] += tmpReal;
                    realAndImag[twoi1] += tmpImag;
                }
                // Next w is computed by complex multiplication with wStep
                // exp(i*(phi+delta)) = exp(i*phi)*exp(i*delta)
                double oldWReal = wReal;
                wReal = oldWReal*wStepReal - wImag*wStepImag;
                wImag = oldWReal*wStepImag + wImag*wStepReal;
            }
        }
        // For the inverse transform, scale down the resulting
        // signal by a factor of 1/N:
        if (inverse) {
            for (int i=0; i<realAndImag.length; i++) {
                realAndImag[i] /= N;
            }
        }
    }

    private int processBlock(double[] target, int targetPos)
    {
        double[] frame = frameProvider.getNextFrame();
        if (frame == null) 
        { 
            return 0;  // Finished processing
        }
        
        double[] convResult = convolve_FD(frame, transformedIR);            
        int toCopy = sliceLength;
        if (frameProvider.validSamplesInFrame() < sliceLength)
            toCopy = frameProvider.validSamplesInFrame();
        
        //System.err.println("Copying " + toCopy);
        // Overlap-save approach:
        // always ignore the first "impulseResponseLength" samples in convResult,
        // because they are contaminated due to circular convolution:
        System.arraycopy(convResult, kernelLength, target, targetPos, toCopy);
        return toCopy;
    }
    
    /**
     * Compute the convolution of two signals, by multiplying them in the frequency domain.
     * This is a specialised version of the core method, requiring two signals of equal length,
     * which must be a power of two, and not checking
     * for pollution arising from the assumed periodicity of both signals.
     * In this version, the first signal is provided in the time domain, while the second is
     * already transformed into the frequency domain. 
     * @param signal1 the first input signal, in the time domain
     * @param fft2 the complex transform of the second signal, in the frequency domain
     * fft[0] = real[0], fft[1] = real[N/2], fft[2*i] = real[i], fft[2*i+1] = imag[i] for 1<=i<N/2
     * @return the convolved signal, of the same length as the two input signals
     * @throws IllegalArgumentException if the two input signals do not have the same length.
     */
    private static double[] convolve_FD(final double[] signal1, final double[] fft2)
    {
        if (signal1 == null || fft2 == null)
            throw new NullPointerException("Received null argument");
        
        if (signal1.length != fft2.length)
            throw new IllegalArgumentException("Arrays must be equal length");
        
        int N = signal1.length;
        
        double[] fft1 = new double[N];
        System.arraycopy(signal1, 0, fft1, 0, N);
        
        // Transform signal to frequency domain
        realTransform(fft1, false);
        
        // Multiply in the frequency domain,
        fft1[0] = fft1[0] * fft2[0]; // because imag[0] is 0
        fft1[1] = fft1[1] * fft2[1]; // and fft1[1] is actually real[N/2]
        for (int i=2; i<N; i+=2) 
        {
            double temp = fft1[i];
            fft1[i] = fft1[i]*fft2[i] - fft1[i+1]*fft2[i+1];
            fft1[i+1] = temp*fft2[i+1] + fft1[i+1]*fft2[i];
        }

        // Transform back to time domain
        realTransform(fft1, true);
        return fft1;
    }
}		// End of LowPassFilter class

