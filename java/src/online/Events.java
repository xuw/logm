package online;

import java.util.ArrayList;

public class Events {
	
	long ts;
	ArrayList nodeids;
	ArrayList msgtypes;
	
	public Events(long ts, ArrayList nodeids, ArrayList msgtypes) {
		super();
		this.ts = ts;
		this.nodeids  = nodeids;
		this.msgtypes = msgtypes;
	}
	
	public String toString() {
		int i;
		String event_str = ts + ":";
		for (i = 0; i < (nodeids.size()-1); i++)
			event_str = event_str + msgtypes.get(i) + "@" + nodeids.get(i) + "_";
		event_str = event_str + msgtypes.get(i) + "@" + nodeids.get(i);

		return event_str;
	}
}
