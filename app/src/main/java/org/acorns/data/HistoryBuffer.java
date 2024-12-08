/* HistoryBuffer.java - Maintain list of most recent entries.
 * 
 * @author  HarveyD
 *   Dan Harvey - Professor of Computer Science
 *   Southern Oregon University, 1250 Siskiyou Blvd., Ashland, OR 97520-5028
 *   harveyd@sou.edu
 *   @version 1.00
 *
 *   Copyright 2010, all rights reserved
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
package org.acorns.data;

import java.io.Serializable;

public class HistoryBuffer implements Serializable 
{
	private static final long serialVersionUID = 1L;

	private Object[] data;  // Array to hold the data
	private int top;                // Pointer to last entry added
	private boolean full;           // Flag to indicate if the buffer is full
	
	/** Constructor to initialize the history buffer */
	public HistoryBuffer(int order)
	{
		data = new Object[order];
		reset();
	}
	
	/** Add a new object to the history buffer 
	 * 
	 * @param value Object to add
	 */
	public void add(Object value)
	{
		if (data.length==0) return;
		if (top==data.length-1) full = true;
		top = (top + 1) % data.length;
		data[top] = value;
	}
	
	/** Get the item specified from the history
	 * 
	 * @param which element to return (0 means current element, 1 means previous one, etc.)
	 * @return The element requested or null if not present.
	 */
	public Object get(int which)
	{
		if (!full && which>top) return null;
		
		int spot = top;
		if (which!=0) 
		{
			spot = top - which;
			if (spot < 0) spot += data.length;
		}
		return data[spot];		
	}
	
	/** Replace an existing item in the history
	 * 		Ignore the request if the object doesn't exist
	 * @param value The object to save
	 * @param which The element requested
	 */
	@SuppressWarnings("unused")
	public void put(Object value, int which)
	{
		if (!full && which>top) return;
		
		int spot = top;
		if (which!=0) 
		{
			spot = top - which;
			if (spot < 0) spot += data.length;
		}
		data[spot] = value;
	}
	
	/** Return true if the history buffer is full */
	public boolean isFull() { return full; }
	
	/** Empty the history buffer */
	private void reset() { top = -1; full = (data.length==0); }
	

}   // End of HistoryBuffer class
