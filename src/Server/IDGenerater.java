package Server;

import java.util.ArrayList;

public class IDGenerater {
	private ArrayList<Integer> free;
	private int mainCounter;

	public IDGenerater() {
		free = new ArrayList<Integer>();
		mainCounter = 0;
	}

	public int getNewID() {
		if (free.size() == 0) {
			mainCounter++;
			return mainCounter;
		} else {
			int result = free.get(0);
			free.remove(0);
			return result;
		}
	}

	public void releaseID(int release) {
		if (release == mainCounter - 1) {
			mainCounter--;
		} else {
			free.add(release);
		}
	}
}
