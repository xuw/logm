package ebay;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

public class TTransaction implements Comparable<TTransaction>,Serializable{

	private static final long serialVersionUID = -6918398333329947804L;

	ArrayList<Integer> seq;
	ArrayList<Long> timestamps;
	ArrayList<Integer> durs;
	
	int tid;
	long startts;
	long endts;
	int dur;
	
	public TTransaction(int tid, long startts) {
		this.tid = tid;
		this.startts = startts;
		seq = new ArrayList<Integer>();
		timestamps = new ArrayList<Long>();
		durs = new ArrayList<Integer>();
		addToSeq(tid, startts, 0); // add self as the first operation..
	}
	
	public void addToSeq(int oid, long ts, int dur) {
		seq.add(oid);
		timestamps.add(ts);
		durs.add(dur);
	}
	
	public String getVector() {
		return "";
	}
	
	public void endTransaction(long endts, int dur) {
		this.endts = endts;
		this.dur = dur;
		addToSeq(tid, endts, dur); // add self as the last operation..
	}
	
	public String toString() {
		return tid
			//+":"+startts+":"+endts+":"+dur
			+":"+ Arrays.deepToString(seq.toArray()) 
			+":" + Arrays.deepToString(timestamps.toArray())
			+":" + Arrays.deepToString(durs.toArray());
	}

	public int compareTo(TTransaction o) {
		return this.tid -o.tid;
	}
}
