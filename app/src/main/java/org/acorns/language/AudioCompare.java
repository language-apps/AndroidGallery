package org.acorns.language;

/*
 *   @author  HarveyD
 *   Dan Harvey - Professor of Computer Science
 *   Southern Oregon University, 1250 Siskiyou Blvd., Ashland, OR 97520-5028
 *   harveyd@sou.edu
 *   @version 1.00
 *
 *   Copyright 2014, all rights reserved
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

import java.io.EOFException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;

import org.acorns.data.AudioFeatures;
import org.acorns.data.AudioData;
import org.acorns.phonemes.Component;
import org.acorns.phonemes.PhonemeType;

import android.graphics.Point;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

public class AudioCompare 
{
	private final static String TAG = "AudioCompare";
	
	private static ArrayList<Component> centerData = null;
	
	private int[] sourceClusters, targetClusters;
	
	private static final int[] VALID_FEATURES =
	{ 
		AudioFeatures.LPC0, 
		AudioFeatures.LPC0 + AudioFeatures.FEATURES_LENGTH, 
		AudioFeatures.LPC0 + 2 * AudioFeatures.FEATURES_LENGTH,
		
		AudioFeatures.LPC1, 
		AudioFeatures.LPC1 + AudioFeatures.FEATURES_LENGTH, 
		AudioFeatures.LPC1 + 2 * AudioFeatures.FEATURES_LENGTH,
		
		AudioFeatures.LPC2, 
		AudioFeatures.LPC2 + AudioFeatures.FEATURES_LENGTH, 
		AudioFeatures.LPC2 + 2 * AudioFeatures.FEATURES_LENGTH,
		
		AudioFeatures.LPC3, 
		AudioFeatures.LPC3 + AudioFeatures.FEATURES_LENGTH, 
		AudioFeatures.LPC3 + 2 * AudioFeatures.FEATURES_LENGTH,
		
		AudioFeatures.LPC4 + 2 * AudioFeatures.FEATURES_LENGTH,
		AudioFeatures.LPC5 + 2 * AudioFeatures.FEATURES_LENGTH,
		AudioFeatures.LPC6 + 2 * AudioFeatures.FEATURES_LENGTH,
		AudioFeatures.LPC7 + 2 * AudioFeatures.FEATURES_LENGTH,
		
		AudioFeatures.LPC_ERROR, 
		AudioFeatures.LPC_ERROR + AudioFeatures.FEATURES_LENGTH, 
		AudioFeatures.LPC_ERROR + 2 * AudioFeatures.FEATURES_LENGTH,
		
	};
	
	
	/** Constructor to initialize speech recognition clusters
	 *        and assign clusters to source audio file
	 *        
	 * @param source audio file to which we will compare to
	 */
	public AudioCompare(AppCompatActivity activity, AudioData source)
	{
		this(activity, null, source);
	}
	
	/** Constructor to initialize speech recognition clusters
	 *        and assign clusters to source audio file
	 *        
	 * @param bounds start/end offsets in the audio object
	 * @param source audio file to which we will compare to
	 */
	private AudioCompare(AppCompatActivity activity, Point bounds, AudioData source)
	{
		sourceClusters = targetClusters = null;
		
		if (centerData==null)
		{
	    	ObjectInputStream ois = null;
	    	try 
	    	{
	    		InputStream stream = activity.getResources().getAssets().open("phonemeComponents.dat");
	        	ois = new ObjectInputStream(stream);
	    		//if (ois != null)
	    		{
		        	centerData = new ArrayList<>();
		        	Component component;
		
	    			Object object;
	                
	    			while (true)
	    			{
	    				object = ois.readObject();
	    				if (object==null) break;
	        			if (!(object instanceof int[]))
	        			{
	        				component = (Component)object;
	        				centerData.add(component);
	        			}
	    			}
	    		}
	    		
	        } catch (EOFException ex)
			{
				ex.printStackTrace();
			}
	    	catch (Exception ex) 
	    	{
				ex.printStackTrace();
	    	}
	    	
	    	try { if (ois!=null) ois.close(); }
	    	catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		
		if (bounds==null) bounds = new Point(-1,-1);
		
		if (!source.isRecorded())  return;

		// Return if speech recognition is not possible
		if (centerData==null) return;

		/* Configure cluster assignments for source audio */
		sourceClusters = assignClusters(bounds, source);
	}

	/** Extract audio feature frames and assign each frame to clusters 
	 * 
	 * @param bounds start/end offsets in the audio object
	 * @param audio SoundData audio object
	 * @return Cluster assignments for each frame
	 */
	public int[] assignClusters(Point bounds, AudioData audio)
	{
		if  (centerData==null) return null;
		
	   	AudioFeatures features = new AudioFeatures(bounds, audio, true, true, true);
       	if (Thread.currentThread().isInterrupted()) return null;

       	double[][] averages = features.computeAverages();
    	double[][] stats = features.getStatisticsSTD(averages);
		double[][] validStats = PhonemeType.getValidFeatures(VALID_FEATURES, stats);

		int len = stats.length;
		int[] clusters = new int[len];
		for (int frame=0; frame<len; frame++)
		{
			clusters[frame] = findClosestCluster(validStats[frame], centerData);
	       	if (Thread.currentThread().isInterrupted()) 
	       	{
	       		Log.i(TAG, "Assigned " + frame + " of " + len);
	       		return null;
	       	}
		}
		markSilentFrames(clusters, stats);
		return clusters;
	}
	
     /** Find cluster closest to the supplied frame of features
     * 
     * @param features Frame of audio feature values
     * @param components ArrayList of cluster components
     * @return closest cluster number
     */
    private int findClosestCluster(double[] features, ArrayList<Component> components)
    {
    	double distance, minDistance = components.get(0).computeDistance(features);
    	int minCluster = 0;
    	for (int cluster=1; cluster<components.size(); cluster++)
    	{
    		distance = components.get(cluster).computeDistance(features);
    		if (distance<minDistance)
    		{
    			minDistance = distance;
    			minCluster = cluster;
    		}
    	}
    	return minCluster;
    }
    
    /** Set the source cluster and assign clusters to frames
     * 
	 * @param bounds start/end offsets in the audio object
	 * @param source SoundData audio object
     */
    @SuppressWarnings("unused")
    public void setSource(Point bounds, AudioData source)
    {
		// Return if speech recognition is not possible
		if (centerData==null) return;

		/* Configure cluster assignments for source audio */
		sourceClusters = assignClusters(bounds, source);
    }


    /** Set the target cluster for audio to which to compare to source
     * 
	 * @param bounds start/end offsets in the audio object
     * @param target Audio object
     */
    @SuppressWarnings("unused")
	public void setTarget(Point bounds, AudioData target)
	{
		if (centerData != null)
		   targetClusters = assignClusters(bounds, target);
	}

	/** Compute similarity value for comparing two audio files
	 * 
	 * @return {source compressed length, target compressed length, difference}
	 */
	@SuppressWarnings("unused")
	public double[] compare()
	{
		if (centerData==null) return null;
		return SpellCheck.audioDistance(sourceClusters, targetClusters);
	}
	
	/** Mark those frames that are silent or pauses
	 * 
	 * @param clusters array indicating which frames are silent
	 * @param stats frams of autio features
	 */
	private void markSilentFrames(int[] clusters, double[][] stats)
	{
		for (int f=0; f<clusters.length; f++)
		{
			if (PhonemeType.isSilentFrame(stats[f]))
			{
				clusters[f] = SpellCheck.SILENCE;
			}
		}
	}
	
}	// End of AudioCompare class
