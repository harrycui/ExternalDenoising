package base;

import java.util.Comparator;

public class MyTreemapComparator implements Comparator<Integer>{

	public int compare(Integer x, Integer y) {
		
		return -x.compareTo(y);
	}
}
