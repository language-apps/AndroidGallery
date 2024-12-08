/*
 * SoundDefaults.java
 *
 *   @author  HarveyD
 *   @version 3.00 Beta
 *
 *   Copyright 2007-2015, all rights reserved
 * 
 */
 
package org.acorns.audio;

import android.media.AudioFormat;


@SuppressWarnings({"SpellCheckingInspection", "unused"})
public class AudioDefaults
{
    /** Record rate in frames per second. */
    private static final float RECORDRATE = 44100f;
    /** Recording device index (-1 = default). */
    private static final int RECORDDEVICE = 0;
    /** Internal audio compression format for */
    private static final String AUDIOCOMPRESSION = "wav";
        /** Sample size in bits */
    private static final int   BITS = 16;
    /** Samples per seconds */
    private static final float RATE = 16000f;
    /** Frame size in bytes */
    private static final int   FRAMEBYTES = 2;
    /** Frames per second */
    private static final float FRAMERATE = 16000f;
    /** Number of channels per sound clip */
    private static final int   CHANNELS  = 1;
    /** Encoding type */
    private static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    /** Big or little ENDIAN format */
    private static final boolean BIGENDIAN = false;
    /** Buffer size for recording sound clips */
    private static final int     BUFFERSIZE = 32768;
    /** Use first channel in a  multiple channel signal. */
    public static final int     SELECTCHANNEL = 0;
    /** Average stereo channels in a  multiple channel signal. */
    private static final int     AVERAGECHANNEL = 1;
    /** Do not use any filter. */
    public static final int     NOWINDOW  = 0;
    /** Use a Blackman window for a window sync filter. */
    public static final int     HAMMING  = 1;
    /** Use a hamming window for a window sync filter. */
    public static final int     BLACKMAN = 2;
    /** Use a recursive CHEBYCHEV filter. Not Implemented! */
    public static final int    HANNING = 3;
    /** Do not filter the audio signal */
    private static final int    NOFILTER = 0;
    /** Use a butterworth bandpass filter */
    public static final int    BANDPASS = 1;
    /** Use a WIENER filter */
    public static final int    WIENER = 2;
    /** Use a Spectral Subtracting filter */
    public static final int    SPECTRAL = 3;
    /** Use a window sinc bandpass filter */
    public static final int    WINDOWSINC = 4;
    
    /** Round to the next frequency amplitude that crosses zero boundary. */
    private static final boolean ROUNDTOZERO = true;
    /** Maximum sound clip in minutes. */
    private static final int     MAXMINS = 10;
    
    /** Minimum bandpass filter frequency */
    private static final double  MINFREQ = 130;
    /** Maximum bandpass filter frequency */
    private static final double  MAXFREQ = 6800;
    /** Size of window sync filter. Must be even, and under 64 for speed. */
    private static final int     FILTERSIZE = 32;
    /** Amount to trim out of recordings loaded into memory. */
    private static final int     TRIMSIZE = 72;

    // Constants used for voice activation
    /** Milliseconds for voice activation window. */
        private static final int ACTIVATION = 10;
    /** Decibel threshold for voice activation (0 if disabled)*/
    private static final int     VOICE = 0;

    // Constants used for the front end speech recognizer
    
    /**Emphasis factor, used by the preEmphasizer method*/
    private static final float     EMPHASISFACTOR = .97f;
    /** The number of filters for the MEL filter bank 40 is optimal
     * for a 16000Hz sampling rate*/
    private static final int       NUMBEROFMELFILTERS = 40;
    /** The Gaussian variance for the mel filters */
    private static final double    MELGAUSSIANVARIANCE = 2.0;
    /** The length of the MFCC vector.  */
    private static final int       CEPSTRUMLENGTH = 13;
    /** The maximum number of Cepstrum features. */
    private static final int       MAXCEPSTRUM    = 20;
    /**The length of the window in used for hamming function*/
    private static final float     WINDOWSIZE = 25.625f;
    /**The length of the offset used for hamming function*/
    private static final float     WINDOWSHIFT = 10f;
    /** Mask to displaying feature vectors (bit on means display the feature) */
    private static final long      FEATUREMASK = (1<<(CEPSTRUMLENGTH + 1)) - 1;
    /** Boolean for short term spectrograph */
    private static final boolean NARROWBAND = true;
    /** Boolean for spectrograph with a color palette */
    private static final boolean COLORPALETTE = false;

    private static final double[] NARROWSPECT = {WINDOWSIZE, WINDOWSHIFT};
    private static final double[] WIDESPECT = {3.0, 1.0};
       
    // Constants for linear prediction
    /** Auto correlation method using LEVINSON-DURBIN algorithm */
    public static final int     TOEPLITZ = 0;
    /** Covariance method using CHLESKI decomposition */
    public static final int     CHOLESKI = 1;
    /** Default number of LPC coefficients */
    private static final int    LPCCOEFFICIENTS = 8;
    
    //Constants for Mel filter types
    /** Triangular MEL filters */
    public static final int    TRIANGLE = 0;
    /** GAUSSIAN MEL filters */
    private static final int    GAUSSIAN = 1;

    /** Detect pitch using the harmonic product algorithm */
    private static final int HARMONICPRODUCT = 0;
    /** Detect pitch using the YIN algorithm */
    public static final int YIN = 1;
    /** Detect pitch using the CEPSTRUM algorithm */
    public static final int CEPSTRUM = 2;
    
    /** Pitch rate change per button click */
    private static final float RATECHANGE = 10f;
    /** Default rate change - change frame rate to make signal faster */
    public static final int DEFAULTRATEALG = 0;
    /** Use PSOLA algorithm when altering signal speed to preserve pitch */
    private static final int PSOLAALG = 1;

    /** Dynamic time warp flag to indicate correct value */
    public static final int CORRECT = 0;
    /** Dynamic time warp default value for close */
    private static final double DTWCORRECTVALUE = 0.67;
    /** Dynamic time warp flag to indicate close value */
    public static final int CLOSE = 1;
    /** Dynamic time warp default value for correct */
    private static final double DTWCLOSEVALUE = 0.60;
    /** Linear distance for dynamic time warp audio comparison algorithm */
    private static final int LINEAR = 0;
    /** Euclidian distance for Dynamic Time Warp audio comparison algorithm */
    public static final int EUCLIDIAN = 1;
    /** Number of dynamic time warp weights for audio comparison algorithm
          3 * maximum cepstrum features + 3 for energy
     */
    private static final int WARPWEIGHTS = 3 * (MAXCEPSTRUM + 3);

    private static float   recordRate            = RECORDRATE;
    private static int     recordDevice          = RECORDDEVICE;
    private static String  audioCompression      = AUDIOCOMPRESSION;
    private static int     bits                  = BITS;
    private static float   rate                  = RATE;
    private static int     frameBytes            = FRAMEBYTES;
    private static float   frameRate             = FRAMERATE;
    private static int     channels              = CHANNELS;
    private static boolean bigEndian             = BIGENDIAN;
    private static int     buffer_size           = BUFFERSIZE;
    private static int     channelOption         = AVERAGECHANNEL;
    private static int	   encoding 			 = ENCODING;
    private static boolean roundToZero           = ROUNDTOZERO;
    private static int     windowType            = HAMMING;
    private static int     filterType            = NOFILTER;
    private static int     filterSize            = FILTERSIZE;
    private static int     trimSize              = TRIMSIZE;
    private static double  minFreq               = MINFREQ;
    private static double  maxFreq               = MAXFREQ;
    private static int     maxMins               = MAXMINS;
    private static float   emphasisFactor        = EMPHASISFACTOR;
    private static int     numberOfMelFilters    = NUMBEROFMELFILTERS;
    private static double  melGaussianDamper     = MELGAUSSIANVARIANCE;
    private static int     melCepstralType       = GAUSSIAN;
    private static int     cepstrumLength        = CEPSTRUMLENGTH;
    private static float   windowSize            = WINDOWSIZE;
    private static float   windowShift           = WINDOWSHIFT;
    private static long    featureMask           = FEATUREMASK;
    private static int     zoom                  = 1;
    private static int     voice                 = VOICE;
    private static int     activation            = ACTIVATION;
    private static int     LPCType               = TOEPLITZ;
    private static int     LPCCoefficients       = LPCCOEFFICIENTS;
    private static boolean spectrograph          = NARROWBAND;
    private static boolean colorPalette          = COLORPALETTE;
    private static int     pitchDetection        = HARMONICPRODUCT;
    private static float   rateChange            = RATECHANGE;

    private static int     rateAlgorithm = PSOLAALG;
    private static int     DTWDistance   = LINEAR;
    private static final double[]DTWCorrectness
                                 = new double[]{DTWCORRECTVALUE, DTWCLOSEVALUE};
    private static double[]DTWWeights    = new double[WARPWEIGHTS];

    
   /** Get the frame rate required for recording */
   public static float getRecordRate()              {return recordRate;}
   /** Set the frame rate required for recording 
    * @param r frame rate required for recording.
    */    
   public static void setRecordRate(float r)        {recordRate = r;}
   
    /** Get the index to the mixer for the recording device */
   public static int getRecordDevice()              {return recordDevice;}
   /** Set the index to the mixer for the recording device 
    * @param d recording device index.
    */    
   public static void setRecordDevice(int d)        {recordDevice = d;}
   
    /** Get the index to the mixer for the recording device */
   public static String getAudioCompression()       {return audioCompression;}
   /** Set the index to the mixer for the recording device 
    * @param c recording device index.
    */    
   public static void setAudioCompression(String c) {audioCompression = c;}
   
   /** Get default bits per sample. */
    public static int     getBits()                 {return bits;}
    /** Set default bits per sample. 
     *  @param b bits per second.
     */
    public static void    setBits(int b)            {bits = b;}
    
    /** Get default samples per second.  */
    public static float   getRate()                 {return rate;}   
    /** Set default samples per second.
     *  @param r samples per second. 
     */
    public static void    setRate(float r)          {rate = r;}
    
    /** Get default bytes per recording frame.   */
    public static int     getFrameBytes()           {return frameBytes;}
    /** Set default bytes per recording frame. 
     *  @param b bytes per recording frame.
     */
    public static void    setFrameBytes(int b)      {frameBytes = b;}
    
    /** get default frames per second.   */
    public static float   getFrameRate()            {return frameRate;}
    /** Set default frames per second.
     *  @param r frame rate. 
     */
    public static void    setFrameRate(float r)     {frameRate = r;}
    
    /** Get default number of channels per recording.  */
    public static int     getChannels()             {return channels;}
    /** Set default number of channels per recording. 
     *  @param c number of channels per recording.
     */
    public static void    setChannels(int c)        
    {  channels = c;   }
    
    /** Get default big or little Endian option, true = big.  */
    public static boolean getBigEndian()            {return bigEndian;}
    /** Set default big or little Endian option, true = big.
     *  @param b true for bigEndian, false otherwise.
     */
    public static void    setBigEndian(boolean b)   {bigEndian = b;}
    
    /** Get default recording buffer size.  */
    public static int     getBufferSize()           {return buffer_size;}
    /** Set default recording buffer size. 
     *  @param b buffer size.
     */
    public static void    setBufferSize(int b)      {buffer_size = b;} 
    
    /** Get default for averaging or selecting stereo channels.  */
    public static int     getChannelOption()        {return channelOption;}
    /** Set default for averaging or selecting stereo channels.
     *  @param c AVERAGE_CHANNEL or SELECT_CHANNEL.
     */
    public static void    setChannelOption(int c)   {channelOption = c;}
    
    /** Get default round selection to next crossing zero.   */
    public static boolean getRoundToZero()          {return roundToZero;}
    /** Set default round selection to next crossing zero. 
     * @param r true if round to next zero sample, false otherwise.
     */
    public static void    setRoundToZero(boolean r) {roundToZero = r;}
    
    // FFT parameters.
    /** Get default window sync filter type.    
     * 
     * @return NOFILTER, HAMMING, BLACKMAN, HANNING
     */
    public static int     geWindowType()           {return windowType;}
    /** Set default window sync filter type.
     *  @param w NOFILTER, HAMMING, BLACKMAN, HANNING
     */
    public static void    seWindowType(int w)      {windowType = w;}
    
    /** Get the type of filter
     * 
     * @return f NOWINDOW, BANDPASS, or WIENER
     */
    public static int getFilterType()  { return filterType; }
    
    /** Set the filter type
     * 
     * @param f NOWINDOW, BANDPASS, or WIENER
     */
    public static void setFilterType(int f) { filterType = f; }
    
    /** Get default window filter size (must be even). */
    public static int     getFilterSize()           {return filterSize;}
    /** Set default window filter size (must be even). 
     *  @param f Size of the widow sync filter.
     */
    public static void    setFilterSize(int f)      {filterSize = f;}
    
    /** Get minimum frequency in thousands. */
    public static double  getMinFreq()              {return minFreq;}

    /** Set minimum frequency in thousands. 
     *  @param f size of sync filter windows.
     */
    public static void    setMinFreq(double f)     {minFreq = f;}

    /** Get default encoding type (ex: signed, unsigned, ALAW, or ULAW). */
    public static int getEncoding()       {return encoding;}
    /** Set default encoding type (ex: signed or unsigned). 
     *  @param e Encoding type (ex: AudioFormat.Encoding.PCM_SIGNED);
     */
    public static void setEncoding(int e) {encoding = e;}
    
    
    /** Get maximum frequency in thousands. */
    public static double  getMaxFreq()             {return maxFreq;}

    /** Set maximum frequency in thousands. 
     *  @param f size of sync filter windows.
     */
    public static void    setMaxFreq(double f)          {maxFreq = f;}
    /** Get default number of frames to trim when loading files. */
    public static int     getTrimSize()              {return trimSize;}
    /** Set default number of frames to trim when loading files. 
     *  @param s size of sync filter windows.
     */
    public static void    setTrimSize(int s)         {trimSize = s;}
    
    /** Get default number of frames to trim when loading files. */
    public static int     getMaxMin()           {return maxMins;}
    /** Set default number of frames to trim when loading files. 
     *  @param m size of sync filter windows.
     */
    public static void    setMaxMins(int m)      {maxMins = m;}
    
    /** Get current zoom factor */
    public static int getZoom()   {return zoom;}
    
    /** Set current zoom factor
     *  @param zoomFactor desired zoom factor
     */
    public static void setZoom(int zoomFactor) { zoom = zoomFactor; }

    /** Get the voice activation threshold window in milliseconds
     *
     * @return voice activation window size in milliseconds
     */
    public static int getActivationWindow()  { return activation; }

    /** Set the voice activation window
     *
     * @param ms length of voice activation window in milliseconds
     */
    public static void setActivationWindow(int ms)  { activation = ms; }

    /** Get voice activation threshold
     *
     * @return threshold roughly in decibels
     */
    public static int getActivationThreshold()  { return voice; }

    /** Set voice activation threshold in decibels (rough measurement)
     *
     * @param v voice activation threshold (roughly in decibels)
     */
    public static void setActivationThreshold(int v) { voice = v; }
    
   /** Get default size for pre-emphasis */
    public static float   getEmphasisFactor()  {return emphasisFactor;}
    
    /** Set default size for emphasis
     *  @param factor desired emphasis factor
     */
    public static void setEmphasisFactor(float factor)
    {  emphasisFactor = factor; }
    
    /** Get default size for number of MEL filters */
    public static int   getNumberOfMelFilters()  {return numberOfMelFilters;}
    
    /** Set desired number of MEL filters
     *  @param num desired number of MEL filters
     */
    public static void setNumberOfMelFilters(int num)
    {  numberOfMelFilters = num;  }
    
    /** Get Gaussian variance of MEL filter bins */
    public static double getMelGaussianDampingFactor() { return melGaussianDamper;}
    
    /** Set Gaussian variance of MEL filter bins */
    public static void setMelGaussianVariance(double variance)
    {  melGaussianDamper = variance; }
    
    /** Return MEL CEPSTRAL type
     * 
     * @return TRIANGLE or GAUSSIAN
     */
    public static int getMelFilterType() { return melCepstralType; }
    
    /** Set Type of MEL CEPSTRAL filter type
     * 
     * @param type TRIANGLE or GAUSSIAN
     */
    public static void setMelFilterType(int type)
    {   melCepstralType = type; }
    
    /** Get default length of MEL frequency coefficient vector */
    public static int   getCepstrumLength()  {return cepstrumLength;}
    
    /** Set desired length of MEL frequency coefficient vector
     *  @param len desired length of MEL frequency coefficient vector
     */
    public static void setCepstrumLength(int len) { cepstrumLength = len; }
        
    /** Get default length of the frame window  */
    public static float   getWindowSize()  {return windowSize;}
    
    /** Set desired hamming window length in milliseconds
     *  @param len desired window length
     */
    public static void setWindowSize(float len)
    {  windowSize = len;  }
    
    /** Get default length of the frame offset  */
    public static float   getWindowShift()  {return windowShift;}

    /** Set desired offset of successive windows
     *  @param offset desired hamming window offset
     */
    public static void setWindowShift(float offset)
    {  windowShift = offset; }
    
    /** Get default length of the frame offset */
    public static long  getFeatureMask()  {return featureMask;}

    /** Set desired offset of successive hamming windows
     *  @param mask desired hamming window offset
     */
    public static void setFeatureMask(long mask)
    {  featureMask = mask; }
    
    /** Get the type of Linear Prediction algorithm
     * 
     * @return SoundDefaults.TOEPLITZ or SoundDefaults.CHOLESKI
     */
    public static int getLPCType() { return LPCType; }

    /** Set the type of Linear Prediction algorithm
     *
     * @param type  SoundDefaults.TOEPLITZ or SoundDefaults.CHOLESKI
     */
    public static void setLPCType(int type)  { LPCType = type; }

    /** Get then number of Linear Prediction coefficients
     *
     * @return Number of coefficients
     */
    public static int getLPCCoefficients()
    { return LPCCoefficients; }

    /** Set the number of Linear Prediction coefficients
     *
     * @param coefficients The number of coefficients
     */
    public static void setLPCCoefficients(int coefficients)
    {   LPCCoefficients = coefficients; }

    /** Get the spectrograph type
     *
     * @return false if long term, true if short term
     */
    private static boolean isNarrowBandSpectrograph() { return spectrograph; }

    /** Get spectrograph parameters
     *
     * @return [0] Spectrograph window width; [1] SPectrograph window overlap
     */
    public static double[] getSpectrographParams()
    {
        if (isNarrowBandSpectrograph())
             return NARROWSPECT;
        else return WIDESPECT;
    }
    /** Set the spectrograph type
     *
     * @param shortTerm  false = long term, true is short term
     */
    public static void setNarrowBandSpectrograph(Boolean shortTerm)
    {   spectrograph = shortTerm; }

    /** Get the color palette type for spectrograms
     *
     * @return true if color palette, false if grey scale
     */
    public static boolean isColorPalette() { return colorPalette; }

    /** Set color palette
     *
     * @param palette true = by colors, false = grey scale
     */
    public static void setColorPalette(boolean palette)
    { 
        colorPalette = palette;
    }

    /** Set the pitch detection algorithm
     *
     * @param alg 0=HARMONICPRODUCTSPECTRUM, 1=YIN, 2=CEPSTRUM
     */
    public static void setPitchDetect(int alg)
    {   
    	pitchDetection = alg; 
    }

    /** Return the pitch detection algorithm
     *
     * @return 0=HARMONICPRODUCT, 1=YIN, 2=CEPSTRUM
     */
    public static int getPitchDetect()  { return pitchDetection; }

    /** Set the rate change percentage per button click
     * 
     * @param rate  rate change per button click
     */
    public static void setRateChange(float rate) { rateChange = rate; }

    /** Return the rate change percentage per click
     *
     * @return rate change percentage
     */
    public static float getRateChange()  { return rateChange; }

    /** Set the rate change algorithm
     *
     * @param alg DEFAULTRATEALG or PSOLAALG
     */
    public static void setRateAlgorithm(int alg) { rateAlgorithm = alg; }
    /** Get the rate change algorithm
     *
     * @return DEFAULTRATEALG or PSOLAALG
     */
    public static int getRateAlgorithm() {  return rateAlgorithm; }

    /** Set the Dynamic Time Warp distance approach
     * 
     * @param distance LINEAR (0) or EUCLIDIAN (1)
     */
    public static void setDTWDistance( int distance) { DTWDistance = distance; }

    /** Get the Dynamic Time Warp distance approach
     *
     * @return distance LINEAR or EUCLIDIAN
     */
    public static int getDTWDistance() { return DTWDistance; }

    /** Set DTW correctness factors
     *
     * @param index [CORRECT], close if > index [CLOSE]
     * @param correctness factor
     */
    public static void setDTWCorrectness(int index, double correctness)
    { DTWCorrectness[index] = correctness; }

    /** Get DTW correctness factors
     *
     * @param index [if > index [CORRECT], close if > index [CLOSE]
     * @return correctness or closeness factor
     */
    public static double getDTWCorrectness(int index)
    { return DTWCorrectness[index]; }

    /** Set the Dynamic Time Warp audio feature weights
     *
     * @param weights array of weighting factors for each audio feature
     */
    public static void setDTWWeights( double[] weights) { DTWWeights = weights;}

    /** Set the Dynamic Time Warp audio feature weights
     *
     * @return array of weighting factors for each audio feature
     */
    public static double[] getDTWWeights()  { return DTWWeights; }
    
}  // End of SoundDefaults class
