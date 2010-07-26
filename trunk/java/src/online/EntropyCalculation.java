package online;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class EntropyCalculation {
	
	private Hashtable<Double, Integer> samples = new Hashtable<Double, Integer>();
	
	public void addSample(double d) {
		Integer cnt = samples.get(d);
		if(cnt!=null) {
			samples.put(d, cnt+1);
		} else {
			samples.put(d, 1);
		}
	}
}
