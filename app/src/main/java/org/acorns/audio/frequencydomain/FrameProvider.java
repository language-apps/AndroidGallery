/*
 * Copyright 2004-2006 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 *
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

import java.util.Arrays;


/**
 * Cut frames out of a given signal, and provide them one by one,
 * optionally applying a processor to the frame.
 * This base implementation provides frames of a fixed length with a fixed shift.
 * @author Marc Schr&ouml;der
 */
class FrameProvider
{
    private double[] signal;
    private int readPosition;
    
    /**
     * The start time of the currently analysed frame.
     */
    private long frameStart;
    private long nextFrameStart;
    private double[] frame;
    private int validSamplesInFrame;

    private int frameShift;
    private int frameLength;
    private int tailCutoff;
    
    /**
     * The part of the original signal to remember for the next overlapping frame.
     */
    private double[] memory;
    // If !stopWhenTouchingEnd, we must continue reading from memory,
    // padding the respective frames with zeroes. This is the current
    // reading position:
    private int posInMemory;
    private boolean memoryFilled;
    
    /**
     * Initialise a FrameProvider.
     * @param signal the signal source to read from
     * @param frameLength the number of samples in one frame.
     * @param frameShift the number of samples by which to shift the window from
     * one frame analysis to the next; if this is smaller than window.getLength(),
     * frames will overlap.
     * @param headCutoff frames to cut from start
     * @param tailCutoff frams to cut from end
     */
    FrameProvider(double[] signal, int frameLength, int frameShift, int headCutoff, int tailCutoff)
    {
        this.signal = signal;
        this.readPosition = headCutoff;
        
        this.frameShift = frameShift;
        this.frameLength = frameLength;
        this.tailCutoff = tailCutoff;
        this.frame = new double[frameLength];
        this.frameStart = -1;
        this.nextFrameStart = 0;
        validSamplesInFrame = 0;
        
        // We keep the previous frame in memory (we'll need this if frameShift < frameLength):
        this.memory = new double[frameLength];
        posInMemory = memory.length; // "empty"
        memoryFilled = false;
    }

    /**
     * Whether or not this frameprovider can provide another frame.
     */
    private boolean hasMoreData()
    {
    	boolean signalMoreData =  (signal!=null && readPosition<signal.length);
        return signalMoreData || memoryFilled && posInMemory < memory.length;
    }
    
    /**
     * This tells how many valid samples have been read into the current frame 
     * (before applying the optional data processor!).
     */
    int validSamplesInFrame()
    {
        return validSamplesInFrame;
    }
    
    /**
     * Fill the internal double array with the next frame of data.
     * The last frame, if only partially filled with the rest of the signal,
     * is filled up with zeroes. If stopWhenTouchingEnd() returns true,
     * this method will provide not more than 
     * a single zero-padded frame at the end of the signal.
     * @return the next frame on success, null on failure.
     */
    double[] getNextFrame()
    {
        frameStart = nextFrameStart;
        if (!hasMoreData()) 
        {
            validSamplesInFrame = 0;
            return null;
        }
        // A frame is composed from two sources:
        // 1. memory and 2. newly read signal.
        int nFromMemory;
        // 1. Prepend some memory?
        if (memoryFilled && posInMemory < memory.length) 
        {
            nFromMemory = memory.length-posInMemory;
            //System.err.println("Reusing " + nFromMemory + " samples from previous frame");
            System.arraycopy(memory, posInMemory, frame, 0, nFromMemory);
        } else 
        {
            nFromMemory = 0;
            //System.err.println("No data to reuse from previous frame.");
        }
        // 2. Read new bit of signal:
        int read = getData(nFromMemory);
        if (read == 0) 
        	return null;
        
        // At end of input signal, we are unable to fill the frame completely:
        if (nFromMemory + read < frameLength) 
        { // zero-pad last frame(s)
 
        	// Pad with zeroes.
            Arrays.fill(frame, nFromMemory+read, frame.length, 0);
        }
        validSamplesInFrame = nFromMemory + read; // = frame.length except for last frame
        // OK, the frame is filled.
        
        // For overlapping frames,
        // remember the frame data in order to reuse part of it for next frame
        int amountToRemember = frameLength - frameShift;
        if (validSamplesInFrame < frameLength) 
        {
            amountToRemember = validSamplesInFrame - frameShift;
        }
        if (amountToRemember > 0) 
        {
            if (memory.length < amountToRemember) 
            {
                memory = new double[amountToRemember];
            }
            System.arraycopy(frame, validSamplesInFrame-amountToRemember, memory, memory.length-amountToRemember, amountToRemember);
            posInMemory = memory.length - amountToRemember;
            memoryFilled = true;
        } else 
        {
            posInMemory = memory.length;
            memoryFilled = false;
        }
        
        nextFrameStart = frameStart + frameShift;

        return frame;
    }
    
    /**
     * Read data from input signal into current frame.
     * This base implementation will attempt to fill the frame from the position
     * given in nPrefilled onwards.
     * @param nPrefilled number of valid values at the beginning of frame. These should not be lost or overwritten.
     * @return the number of new values read into frame at position nPrefilled.
     */
    private int getData(int nPrefilled)
    {
        return getData(frame, nPrefilled, frame.length-nPrefilled);
    }
    
    /**
     * This implementation of getData() will cut off a tail corresponding to half of the FIR filter.
     */
    private int getData(double[] target, int targetPos, int length) 
    {
        //if (target.length < targetPos+length)
        //    throw new IllegalArgumentException("Not enough space left in target array");
        int toRead = length + tailCutoff;
        int remaining = signal.length - readPosition;

        if (remaining < toRead) 
        {
            length = remaining - tailCutoff;
        }
        System.arraycopy(signal, readPosition, target, targetPos, length);
        readPosition = (length<=0) ? signal.length : readPosition + length;
        return length;
    }
}

