package org.acorns.phonemes;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.NoSuchElementException;

public class Component implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	/** Update covariance after each cluster computation iteration */
	private static final boolean COVARIANCE = true; 
	
	/** Use euclidian distance to find nearest cluster */
	private static final int EUCLIDEAN   = 0;
	
	/** Use mahalanobus  distance to find nearest cluster */
	private static final int MAHALANOBUS = 1;
	
	/** Identify the algorithm to use to compute the nearest cluster */
	private static final int DISTANCE = MAHALANOBUS;


	private double[] mean;
	private double proportion;

	private double determinant, modDeterminant;
	private double[][] inverse;
	
	/** Constructor with supplied mean and covariance matrix
	 * 
	 * @param mean Array of center values for the component
	 * @param covariance matrix
	 */
	@SuppressWarnings("unused")
	public Component(double[] mean, double[][] covariance)
	{
		this.mean = mean;
		
    	inverse = Matrix.invert(covariance);
    	determinant = modDeterminant = Matrix.determinant(covariance);
	}
	
	/** Create a component from the data of another
	 * 
	 * @param component The source component
	 * @param mean The updated mean
	 */
	@SuppressWarnings("unused")
	public Component(Component component, double[] mean)
	{
		this.mean = mean.clone();
		
		this.inverse = component.inverse;
		this.determinant = component.determinant;
		this.modDeterminant = component.modDeterminant;
	}

	@SuppressWarnings("unused")
	public void setMean(double[] mean)
	{  this.mean = mean; }

	private double[]   getMean()			{return mean;}

	@SuppressWarnings("unused")
	public void       setProportion(double proportion)
	{ this.proportion = proportion; }

	@SuppressWarnings("unused")
	public double 	  getProportion()	{return proportion;}

	@SuppressWarnings("unused")
	public double     getDeterminant()  {return determinant;}

	@SuppressWarnings("unused")
	public double[][] getInverse()		{return inverse;}

	@SuppressWarnings("unused")
	public double     getLogD()			{return Math.log(modDeterminant);}
	
	/** Update cluster parameters
	 * 
	 * @param cluster frame assignments
	 * @param dataSet set of audio frames
	 */
	@SuppressWarnings("unused")
	public void updateParameters(ArrayList<Integer> cluster, double[][] dataSet)
			throws NoSuchElementException
	{
	   	NoSuchElementException noElem = new NoSuchElementException();

    	if (cluster.isEmpty()) 
   			   throw noElem;

   	    double[][] covariance = createCovariance(cluster, dataSet);
   	    if (covariance==null) 
   	       return;
   	    
    	double[][] inv = Matrix.invert(covariance);
    	if (inv==null) 
    		return;

    	double det = Matrix.determinant(covariance);
		if (Double.isNaN(det)) 
		     return;
		else modDeterminant = det;
    	
    	if (COVARIANCE)
    	{
    		determinant = det;
    		inverse = inv;
    	}
	}
	
    /** Compute distance squared from the mean using the specified features
     * 
     * @param source frame of features
     * @return distance metric from the component's mean
     */
	@SuppressWarnings("all")
    public double computeDistance(double[] source)
    {
    	switch (DISTANCE)
    	{
	    	case EUCLIDEAN:
	    		return euclideanDistance(source, mean);
	    	case MAHALANOBUS:
	    		return mahalanobusDistance(source, mean, inverse);
	    	default:
	    		return -1;
    	}
    }

	@SuppressWarnings("unused")
	public double computeDistance(Component component)
    {
    	return computeDistance(component.getMean());
    }
    
    /**
     * 
     * @param source The source frame of features
     * @param destination The destination frame of features
     * @return Euclidean distance
     */
    private double euclideanDistance(double[] source, double[] destination)
    {
    	double distance = 0.0;
    	for (int feature=0; feature<source.length; feature++)
    	{
    		distance += (source[feature] - destination[feature]) * (source[feature] - destination[feature]);
    	}
    	return distance;
    }
    
    /**
     * 
     * @param source The source frame of features
     * @param destination The destination frame of features
     * @param inverseMatrix covariance matrix to use in the calculation
     * @return Mahalanobus distance
     */
    private static double mahalanobusDistance(double[] source, double[] destination, double[][] inverseMatrix)
    {
    	int len = source.length;
    	double distance = 0.0;
    	
    	double[] vector = new double[len];
    	double[][] columnVector = new double[1][len];
    	
    	int columnIndex = 0;
    	for (int feature=0; feature<inverseMatrix.length; feature++)
    	{
    		columnVector[0][columnIndex] = vector[columnIndex] = source[feature] - destination[feature];
    		columnIndex++;
    	}
    	
    	double[][] productMatrix = Matrix.multiply(columnVector, inverseMatrix);
    	double[] product = productMatrix[0];
    	
    	for (int k=0; k<len; k++)
    	{
    		distance += product[k] * vector[k];
    	}
    	return distance;
    }
    
    /** Create covariance Matrix for this cluster
     * 
     * @param cluster Files/Frames assigned to the cluster
     * @param dataSet Array of features for all audio frames
     * @return Covariance matrix for this cluster
     */
    private double[][] createCovariance
    		(ArrayList<Integer> cluster, double[][] dataSet)
    {
    	double[] mean = computeMean(dataSet, cluster);
    	double[] std = computeSTD(dataSet, mean, cluster);
    	int len = mean.length;
    	double[][] matrix = new double[len][len];

		for (int row=0; row<len; row++)
			Arrays.fill(matrix[row], 0);
    	
    	int frames = cluster.size();
    	int frameEntry;
    	double[] features;
    	double variance;
    	for (int frame = 0; frame < frames; frame++)
    	{
    		frameEntry = cluster.get(frame);
    		features = dataSet[frameEntry];
    		for (int i=0; i<features.length; i++)
    			for (int j=i; j<features.length; j++)
    			{
    			   variance = (features[i]-mean[i])/std[i]
    					             *(features[j]-mean[j])/std[j];
     			   matrix[i][j] += variance; 
    			}
    	}
    	
    	if (frames>1)
    	{
	    	for (int i=0; i<len; i++)
	    		for (int j=i; j<len; j++)
	    		{
	    			matrix[i][j] /= (frames-1);
	    			if (i!=j) matrix[j][i] = matrix[i][j];
	    		}
   		}
    	return matrix;
    }
    
    /** Recompute the mean feature vector for this cluster 
     * 
     * @param dataSet Double dimension array of audio frame features
     * @param cluster Current cluster assignments
     */
	@SuppressWarnings("unused")
	public void updateMean(double[][] dataSet, ArrayList<Integer> cluster )
    {
    	mean = computeMean(dataSet, cluster);
    }
    
    /** Create mean for this cluster
     * 
     * @param dataSet Array of features for all audio frames
     * @param cluster Files/Frames assigned to the cluster
     * @return Covariance matrix for this cluster
     */
    private static double[] computeMean
    	(double[][] dataSet, ArrayList<Integer> cluster)
    {
    	int features = dataSet[0].length;
    	double[] center = new double[features];
    	
    	Arrays.fill(center, 0.0);
    	double count = 0;
    	
    	for (int frame: cluster)
    	{
			count++;
   			for (int feature=0; feature<features; feature++)
    		{
    			center[feature] += dataSet[frame][feature];
    		}
    	}
    	
       	for (int feature=0; feature<features; feature++)
        {
   			if (count>0)
        		center[feature] /= count;
    	}
       	return center;
    }

    /** Compute the total variance for a given cluster
     * 
     * @param cluster Files/Frames assigned to the cluster
     * @param dataSet Array of features for all audio frames
     * @return computed total variance (not divided by the number of points)
     */
	@SuppressWarnings("unused")
	public double computeVariance
    	(ArrayList<Integer>cluster, double[][] dataSet)
    {
   		double variance = 0;
   		int frame;
   		for (int record=0; record<cluster.size(); record++)
		{
			frame = cluster.get(record);
    		variance += computeDistance(dataSet[frame]);
		}
   		return variance;
    }
    
    /** Compute the standard deviation of each feature
     * 
     * @param dataSet The audio frame data
     * @param mean The mean value for each feature in the cluster
     * @param cluster The array of frame cluster assignments
     * @return The standard deviation of each feature
     */
    private double[] computeSTD
    	(double[][] dataSet, double[] mean, ArrayList<Integer>cluster)
    {
    	int len = mean.length;
    	double[] std = new double[len];
    	Arrays.fill(std,  0.0);
    	
    	for (int frame: cluster)
    	{
    		for (int feature=0; feature<dataSet[frame].length; feature++)
    		{
    			std[feature] 
    				+= Math.pow(dataSet[frame][feature] - mean[feature], 2);
    		}
    	}
    	
    	for (int feature=0; feature<len; feature++)
    	{
    		std[feature] = Math.sqrt( std[feature]/(cluster.size() - 1)); 
    	}
    	return std;
    }

}
