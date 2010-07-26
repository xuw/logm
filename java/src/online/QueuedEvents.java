package online;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import pca.MatrixUtils;

public class QueuedEvents {
	
	ArrayList<Event> queuedEvents;
	ArrayList<Long> timeOuts;
	long timeout_ts;
	
	ArrayList<SeqPattern> candiatePatterns;
	
	public QueuedEvents() {
		this.queuedEvents = new ArrayList<Event>();
		this.timeout_ts = 0;
		this.candiatePatterns = new ArrayList<SeqPattern>();
		this.timeOuts = new ArrayList<Long>();
	}
	
	
	public int matchPattern(long currentts, SeqPattern pattern) {
		
		int[] cnt = new int[pattern.countvec.length];
		System.arraycopy(pattern.countvec, 0, cnt, 0, pattern.countvec.length);
		
		//MatrixUtils.prettyPrint(cnt);
		//System.err.println(" %% " +Arrays.toString(queuedEvents.toArray()) );
		
		for(Event e: queuedEvents) {
			int pos = e.msgtype; 
			cnt[pos] -= 1;
		}
		
		// see if the pattern is matched..
		boolean complete=true;
		for(int c:cnt) {
			if (c>0)
				complete = false;
		}
		
		// if no match ~  how do I know it is a no match?
		
		if (complete) {
			//System.err.println("completed pattern " + pattern.pattern_id);
			//remove matched events
			System.arraycopy(pattern.countvec, 0, cnt, 0, pattern.countvec.length);
			
			Iterator<Event> iter = queuedEvents.iterator();
			while(iter.hasNext()) {
				Event e = iter.next();
				if (cnt[e.msgtype] > 0) {
					cnt[e.msgtype] -=1;
					iter.remove();
				}
			}
			return 2;
		} else {
			
			// not sure how to update this timeout value...
			long min_ts = this.queuedEvents.get(0).ts;
			long t = (long) (min_ts + (pattern.duration_median + 10*pattern.duration_mad)*1000);
			
			
			//System.err.println("cts=" + currentts +" to=" + t);
			if (t>this.timeout_ts) 
				this.timeout_ts = t;
			return 1;
		}
		
		//System.err.println("pattern " + pattern.pattern_id );
		
	}
	
	public String toString() {
		return Arrays.deepToString(this.queuedEvents.toArray()) + " (to=" +this.timeout_ts +")";
	}
	
}
