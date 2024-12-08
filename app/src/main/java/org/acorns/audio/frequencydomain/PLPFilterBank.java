package org.acorns.audio.frequencydomain;

/**
 * Defines a PLP filter, match the human auditory critical bands
 */
class PLPFilterBank
{
	/** Indices into the bankIndex array (starting/ending frequencies */
	private final static int BEGIN = 0, END = 1;
	
	/** Number of bark filters */
	private final static int F = 24;

	/** Center frequencies of the frequency bank 
	 *  <pre>
	 *  The standard bands are:
	 *         0,   50,  150,   250,   350  // Bark 0-4
	 *	  ,  450,  570,  700,   840,  1000  // Bark 5-9
	 *	  , 1170, 1370, 1600,  1850,  2150  // Bark 10-14
	 *	  , 2500, 2900, 3400,  4000,  4800  // Bark 15-19
	 *	  , 5800, 7000, 8500, 10500, 13500  // Bark 20-24
	 *
	 *  The bands that apply to this system's
	 *  approximate warping function is: 
	 *         0,  100,   204,  313,    430
	 *    ,  560,  705,   870, 1059,   1278
	 *    , 1532, 1828,  2176,  2584,  3065
	 *    , 3630, 4297,  5083,  6011,  7106
	 *    , 8399, 9926, 11729, 13858, 16374
	 *    
	 *  However, because the number of filters are a parameter,
	 *  the actual mapping might be much more compressed.
	 *  
	 *  </pre> 
	 */
	private double[] centerFreqs; 
	
    /** bank of PLP filters: [filter number][filter bin output fraction] */
    private double[][] filterBank;
    
    /** Define start and end index of each filter */
    private int[][] bankIndex;
    
    /** Spectrum size */
    private int FFTSize;


    /**
     * Constructs a PLP frequency filters.
     * <p/>
     * Defines a filter according to the following piecewise frequency equation
     * Filter outputs are based on the Schroeder curve
     * 
     * <p/>
     * <pre>
     * Filter(f) = f<-2.5 or f>1.3: 0 <br>
     *           = 2.5 <= f <= 0.5: 10^(-(f+0.5)) <br>
     *           = -0.5 <= f <= 0.5: 1 <br>
     *           = 0.5 <= f <= 1.3: 10^(-2.5(f-0.5)) <br>
     * </pre>
     * @param sampleRate audio recording sample rate in HZ
     * @param FFTSize  Spectrum length (number of bins)
     * @param minFreq minimum frequency (in HZ) to consider
     * @param maxFreq maximum frequency in HZ) to  consider
     */
    PLPFilterBank(double sampleRate, int FFTSize, double minFreq,  double maxFreq)
    {
    	double deltaFFTBin = sampleRate / FFTSize;
    	this.FFTSize = FFTSize;
    	
    	if (minFreq<1) minFreq = 1;
    	if (maxFreq > sampleRate/2) maxFreq = sampleRate/2;
    	
    	double minBark = FastFourierTransform.warp(minFreq, false);
    	
    	// Add one filter to cover the high frequency.
    	double maxBark = FastFourierTransform.warp(maxFreq, false);
    	
        double deltaBarkFreq = (maxBark - minBark) / (F + 1);

        double center;
        centerFreqs = new double[F];

        for (int filter = 0; filter < F; filter++) 
        {
            center = FastFourierTransform.unwarp(minBark + filter * deltaBarkFreq, false);
            centerFreqs[filter] = center;
        }
    	
    	filterBank = new double[F][];
    	bankIndex = new int[F][2];
        double[] tempBank;
        
    	double barkBinFreq;
    	boolean start;
    	int startIndex, endIndex, filterSize;
    	for (int filter=0; filter<F; filter++)
    	{
    		start = false;
    		tempBank = new double[(int)(maxFreq/deltaFFTBin + 1)];
    		
	        for (int freq = 0; freq < tempBank.length; freq++) 
	        {
	            barkBinFreq = FastFourierTransform.warp(freq*deltaFFTBin, false)
	            		- (minBark + deltaBarkFreq * filter);
	            
	            // Compute filter output based on bin distance from the center
	            if (barkBinFreq < -2.5)
	                   tempBank[freq] = 0.0;
		        else if (barkBinFreq <= -0.5)
		        {
		        	if (!start) 
		        	{
		        		bankIndex[filter][BEGIN] = freq;
		        		start = true;
		        	}
	            	tempBank[freq] = Math.pow(10.0, barkBinFreq + 0.5);
	            }
	            else if (barkBinFreq <= 0.5)
	            	tempBank[freq] = 1.0;
	            else if (barkBinFreq <= 1.3)
	            	tempBank[freq] = Math.pow(10.0, -2.5 * (barkBinFreq - 0.5));
	            else 
	            {
	            	bankIndex[filter][END] = freq;
	            	break;
	            }
	        }
	        
        	startIndex = bankIndex[filter][BEGIN];
        	endIndex = bankIndex[filter][END];
	        if (endIndex==0)
	        {
	        	endIndex = bankIndex[filter][END] = tempBank.length;
	        }
	        
	        filterSize = endIndex - bankIndex[filter][BEGIN];
            filterBank[filter] = new double[filterSize];
            	
    	    System.arraycopy(tempBank, startIndex
    	        	, filterBank[filter], 0, endIndex-startIndex);
        }
    }          // End of constructor


    /**
     * Compute the PLP spectrum at the center frequency of this filter for a given power spectrum.
     *
     * @param spectrum the input power spectrum to be filtered
     * @return the PLP spectrum values
     * @throws IllegalArgumentException if the input spectrum length != expected FFT size
     */
    double[] applyFilter(double[] spectrum) throws IllegalArgumentException
    {

        if (spectrum.length != FFTSize) 
        {
            throw new IllegalArgumentException
                    ("FFT spectrum mismatch: " + spectrum.length + "!=" + FFTSize);
        }

        double[] output = new double[filterBank.length];
        for (int filter=0; filter<filterBank.length; filter++)
        {
        	for (int freq=bankIndex[filter][BEGIN]; freq<bankIndex[filter][END]; freq++)
        	{
       		   output[filter] 
       				   += spectrum[freq] * filterBank[filter][freq-bankIndex[filter][BEGIN]];
        	}
        }
        return output;
    }   // End of applyFilter
    
    /** Return the number of active PLP filters */
    int getNumberFilters()
    {
    	return filterBank.length;
    }
    
    /** Get center frequency of a particular frequency
     * 
     * @param bark The bark index
     * @return The center frequency in HZ
     */
    double getCenterFrequency(int bark)
    {
    	return centerFreqs[bark];
    }
}       // End of PLPFilterBank class
