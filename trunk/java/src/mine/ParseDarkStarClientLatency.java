package mine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;

public class ParseDarkStarClientLatency {

	static long[] tsarray;
	static double[] latency_sum;
	static int[] latency_cnt;
	
	public static void main(String[] args) throws Exception{
		File dir = new File("C:/Users/xuw/mscipts/pca_new/data/dk_status");
		File f1 = new File(dir, "client_latency_normal.txt");
		File f2 = new File(dir, "client_latency_slow.txt");
		File f3 = new File(dir, "client_latency_large.txt");
		
		BufferedReader in = new BufferedReader(new FileReader(
				new File(dir,"TSes.txt")));
		
		String line = in.readLine();
		ArrayList<Long> tscollect = new ArrayList<Long>();
		while(line!=null) {
			long ts = Long.parseLong(line);
			tscollect.add(ts/1000);
			line = in.readLine();
		}
		tsarray = new long[tscollect.size()];
		for(int i=0; i<tscollect.size(); i++) {
			tsarray[i] = tscollect.get(i);
		}
		
		latency_sum = new double[tsarray.length];
		latency_cnt = new int[tsarray.length];
		
		//System.err.println( Arrays.deepToString(tsarray) );
		
		parseOneFile(f1,1211780666000L);
		parseOneFile(f2,1211782038000L);
		parseOneFile(f3,1229291294000L);
		
		double averagelatency =0;
		for (int i=0; i<tsarray.length; i++) {
			if (latency_cnt[i] >0) {
				averagelatency = latency_sum[i]/latency_cnt[i];
			}
			System.err.println(tsarray[i]+" " + averagelatency);
		}
		
	}
	
	public static void parseOneFile(File file, long offset) throws Exception{
		
		BufferedReader in = new BufferedReader(new FileReader(file));
		
		String line = in.readLine();
		while(line!=null) {
			String[] fields = line.split(" ");
			long tssec = (Integer.parseInt(fields[1])+offset)/1000 +60;
			int latency = Integer.parseInt(fields[2]);
			
			long start_ts = tssec -(latency/1000);
			
			int ind = Arrays.binarySearch(tsarray, start_ts)+1;
			
			latency_sum[Math.abs(ind)] += latency;
			latency_cnt[Math.abs(ind)] += 1;
			
			//System.err.println(tssec +" " +ind +" " + latency);
			line = in.readLine();
		}
	}

}
