/*
 Copyright (C) 2013  Marcel Hollerbach, Daniel Haß

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package Server;

import java.util.ArrayList;

/**
 * IDGenerator
 * 
 * A Class to organize the ID Handling,
 * 
 * It works with a list. The list is filled with ID 0 to the Biggest ID. If you
 * call getNewID() the returned ID will be removed from the list.
 * 
 * If you release a ID it will be added to the list.
 * 
 * @author Marcel Hollerbach
 * 
 */
public class IDGenerater {
	/**
	 * The List of available IDs
	 */
	private ArrayList<Integer> free;

	/**
	 * Will fill the list with the IDs from 0 up to the highestID
	 * 
	 * @param hightestID
	 */
	public IDGenerater(int hightestID) {
		free = new ArrayList<Integer>();
		for (int i = 0; i < hightestID; i++) {
			free.add(i);
		}
	}

	/**
	 * Will return an ID from the list.
	 * 
	 * The returned ID will be removed from the list
	 * 
	 * @return an unique ID
	 */
	public int getNewID() {
		if (free.size() == 0) {
			return -1;
		} else {
			int result = free.get(0);
			free.remove(0);
			return result;
		}
	}

	/**
	 * This will add the parameter back to the free list
	 * 
	 * @param release
	 */
	public void releaseID(int release) {
		// TODO check if the ID can be released
		free.add(release);
	}

	/**
	 * Will return a number of possible IDs.
	 * 
	 * @return
	 */
	public int getPossibleIDs() {
		return free.size();
	}
}
