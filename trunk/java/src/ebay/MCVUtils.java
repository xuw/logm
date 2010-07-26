package ebay;

import java.util.HashMap;

public class MCVUtils {
	
	public static int[] readSeq(String strseq, HashMap<Integer, Integer> equivalent) {
		String[] tt = strseq.substring(1, strseq.length()-1).split(", ");
		int[] ret = new int[tt.length];
		for(int i=0; i< tt.length; i++) {
			ret[i] = equivalent.get( Integer.parseInt(tt[i]) ) ;
			
		}
		return ret;
	}
	
	public static String arrayToMCV(int[] arr, String annot, long ts) {
		StringBuffer sb = new StringBuffer();
		sb.append(ts).append(" ");
		for(int s: arr) {
			sb.append(s).append(" ");
		}
		sb.append(annot);
		return sb.toString();
	}

}
