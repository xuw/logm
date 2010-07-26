package online;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;

public class SeqPattern implements Serializable{
	
	//static Hashtable<Integer, Integer> keymap;
	//static Hashtable<Integer, Integer> rev_keymap;
	
	public int [] countvec;
	public double duration_mean;
	public double duration_stddev;
	public double duration_median;
	public double duration_mad;
	
	public double gap_mean;
	public double gap_stddev;
	public double gap_median;
	public double gap_mad;
	
	public ArrayList<Integer> durations;
	public ArrayList<Integer> gaps;
	
	public int frequency_first_pass;
	public int frequency_second_pass;
	
	public int pattern_id;
	
	
	public boolean containsEvent(int msgtype){
		return countvec[msgtype] !=0;
	}
	
	@Override
	public boolean equals(Object obj) {
		SeqPattern other = (SeqPattern) obj;
		if(this.countvec.length!=other.countvec.length)
			return false;
		for(int i=0; i<this.countvec.length; i++){
			if(this.countvec[i]==other.countvec[i]) {
				; // do nothing
			} else {
				return false;
			}
		}
		return true;
	}
	
	@Override
	public int hashCode() {
		StringBuffer sb = new StringBuffer();
		for(int i: this.countvec) {
			sb.append(i);
		}
		return sb.toString().hashCode();
	}
	
	public String toPCAString(){
		StringBuffer sb = new StringBuffer();
		sb.append("123456").append(" "); // fake timestamp
		for(int i:this.countvec) {
			sb.append(i).append(" ");
		}
		sb.append("%pattern_" +this.pattern_id );
		return sb.toString();
	}
	
	public static String cntVecToSeq(int[] countvec) {
		StringBuffer sb = new StringBuffer();
		for(int i=0; i<countvec.length; i++) {
			if(countvec[i]!=0) {
				sb.append(countvec[i]).append("x").append(KeymapUtils.revkeymap.get(i)).append(" ");
			}
		}
		return sb.toString();
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		
		sb.append("Pattern").append(this.pattern_id).append(":" );
		for(int i:this.countvec) {
			sb.append(i).append(" ");
		}
		sb.append("( ").append(SeqPattern.cntVecToSeq(this.countvec)).append(")\n");
		sb.append("\tDuration: ").append( duration_mean+"/"+ duration_stddev).append(" / ").append(duration_median+"/"+duration_mad).append("\n");
		sb.append("\tGap: ").append(gap_mean+"/"+ gap_stddev).append(" / ").append(gap_median+"/"+gap_mad).append("\n");
		sb.append("\tFeqency: ").append(this.frequency_first_pass+"/"+ this.frequency_second_pass).append("\n");
		
		return sb.toString();
	}

}
