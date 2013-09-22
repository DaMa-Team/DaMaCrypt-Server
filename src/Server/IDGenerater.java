package Server;

import java.util.ArrayList;

public class IDGenerater {
	private ArrayList<Integer> free;
	private int mainCounter;

	public IDGenerater(int hightestID) {
		free = new ArrayList<Integer>();
		for(int i = 0; i<hightestID;i++){
			free.add(i);
		}	
	}

	public int getNewID() {
		if (free.size() == 0) {
			return -1;
		} else {
			int result = free.get(0);
			free.remove(0);
			return result;
		}
	}

	public void releaseID(int release) {
			free.add(release);
	}
}
