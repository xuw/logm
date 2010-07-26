package online;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Map.Entry;

import pca.MatrixUtils;

public class SeqSegmentation {
	
	static final int SIZE =30;
	private static File seqfile = new File("data\\online\\hdfs_sm1");
	private static int keycnt=0;
	private static Hashtable<Integer, Integer> keymap = new Hashtable<Integer, Integer>();
	
	private static Hashtable<String, Integer> nodemap = new Hashtable<String, Integer>();
	
	static double[][] bigramcnt = new double[SIZE][SIZE];
	static double[][] timesum = new double[SIZE][SIZE];
	static double[][] timesqsum = new double[SIZE][SIZE];
	
	
	static double[][] bigramnormal = new double[SIZE][SIZE];
	
	
	static ArrayList<Double>[][] intervals = new ArrayList[SIZE][SIZE];
	static double[][] intervalcnt = new double[SIZE][SIZE];
	
	static double[][] timemean = new double[SIZE][SIZE];
	static double[][] timestddev = new double[SIZE][SIZE];
	static double[][] timemad = new double[SIZE][SIZE];
	static double[][] timemedian = new double[SIZE][SIZE];
	
	static boolean[][] isword = new boolean[SIZE][SIZE];
	
	static boolean[][] isword_by_time = new boolean[SIZE][SIZE];
	
	
	
	
	public static void main(String[] args) throws Exception{
		
		BufferedReader reader = new BufferedReader(new FileReader(seqfile));
		String line = reader.readLine();
		int nidcnt =0;
		
		for (int i=0;i<SIZE;i++) {
			for (int j=0; j<SIZE; j++) {
				intervals[i][j] = new ArrayList<Double>();
			}
		}
		
		while(line!=null) {
			String[] parts= line.split("\\s");
			//System.err.println( Arrays.deepToString(parts) );
			
			String[] event = parts[1].split(":");
			int id = Integer.parseInt( event[0] );
			long prev_ts = Long.parseLong( event[1] );
			String node = event[2];
			Integer nid = nodemap.get(node);
			if(nid==null) {
				nidcnt +=1;
				nid = nidcnt;
				nodemap.put(node, nid);
			}
			Integer prev_pos = keymap.get(id);
			if(prev_pos ==null) {
				prev_pos = keymap.size();
				keymap.put(id, prev_pos);
			}
			
			for (int i=2; i< parts.length; i++) { // skip the first one (block id)..
				event = parts[i].split(":");
				id = Integer.parseInt( event[0] );
				long ts = Long.parseLong( event[1] );
				node = event[2];
				nid = nodemap.get(node);
				if(nid==null) {
					nidcnt +=1;
					nid = nidcnt;
					nodemap.put(node, nid);
				}
				
				Integer pos = keymap.get(id);
				if(pos ==null) {
					pos = keymap.size();
					keymap.put(id, pos);
				}
				bigramcnt[prev_pos][pos] += 1;
				//timesum[prev_pos][pos] += (ts-prev_ts)/1000;
				//timesqsum[prev_pos][pos] += timesum[prev_pos][pos]*timesum[prev_pos][pos];
				intervals[prev_pos][pos].add( (double)(ts-prev_ts)/1000 );
				prev_pos = pos;
				prev_ts = ts;
			}
			line = reader.readLine();
		}
		reader.close();
		
		
		// calculate MAD
		for(int i=0; i<SIZE; i++) {
			for(int j=0; j<SIZE; j++) {
				ArrayList<Double> list = intervals[i][j];
				if(list.size() >=50) {
					Collections.sort(list);
					double median = list.get(list.size()/2) ;
					
					//if ( (i==5&&j==6) || (i==6&&j==5) ) {
					//	System.err.println(i+"->"+j+": " + Arrays.deepToString(list.toArray()));
					//}
					
					timemedian[i][j]= median;
					double[] absdevarr = new double[list.size()];
					for (int k=0; k<list.size(); k++) {
						absdevarr[k] = Math.abs(list.get(k) - median);
					}
					Arrays.sort(absdevarr);
					double m = absdevarr[absdevarr.length/2];
					timemad[i][j] = m;
				} else {
					; // leave everything 0..
				}
			}
		}
		
		// calculate sum and sqsum
		for(int i=0; i<SIZE; i++) {
			for(int j=0; j<SIZE; j++) {
				ArrayList<Double> list = intervals[i][j];
				if(list.size() >=30) {
					Collections.sort(list);
					int skip = list.size()/20;
					//int skip=0;
					for (int k=skip; k<list.size()-skip; k++) {
						timesum[i][j] += list.get(k);
						timesqsum[i][j] += list.get(k)*list.get(k);
						intervalcnt[i][j] += 1;
					}
				} else {
					; // leave everything 0..
				}
			}
		}
		
		
		//MatrixUtils.prettyPrint(bigramcnt);
		for(int i=0; i< bigramcnt.length; i++) {
			double linesum = MatrixUtils.sum(bigramcnt[i]);
			if(linesum!=0) {
				for (int j=0; j< bigramcnt[i].length; j++) {
					bigramnormal[i][j] = bigramcnt[i][j]/linesum;
					isword[i][j] = bigramnormal[i][j] > (1./SIZE);
				}
			}
		}
		
		// compute time interval distribution
		for(int i=0; i<intervalcnt.length; i++) {
			for(int j=0; j<intervalcnt[i].length; j++) {
				double cnt = intervalcnt[i][j];
				if(cnt > 0) {
					timemean[i][j] = timesum[i][j]/cnt;
					timestddev[i][j] = Math.sqrt( (timesqsum[i][j]/cnt) - timemean[i][j]*timemean[i][j] );
				}
			}
		}
		
		MatrixUtils.prettyPrint(timemean);
		MatrixUtils.prettyPrint(timestddev);
		MatrixUtils.prettyPrint(timemedian);
		MatrixUtils.prettyPrint(timemad);
		
		for(int i=0; i< timemean.length; i++) {
			for (int j=0; j<timemean[i].length; j++) {
				isword_by_time[i][j] = timemean[i][j]<60;
			}
		}
		
		MatrixUtils.prettyPrint(isword_by_time);
		
		System.err.println(Arrays.deepToString( keymap.entrySet().toArray() ));
		
		Hashtable<String, ArrayList<Double>> seqtime = new Hashtable<String, ArrayList<Double>>();
		
		// do segmentation..
		reader = new BufferedReader(new FileReader(seqfile));
		line = reader.readLine();
		while(line!=null) {
			String[] parts= line.split("\\s");
			int prev_pos = -1;
			long startts=0;
			long prev_ts=0;
			
			int[] msgcntarr = new int[SIZE];
			for (int i=1; i< parts.length; i++) { // skip the first one
				String[] event = parts[i].split(":");
				int id = Integer.parseInt( event[0] );
				long ts = Long.parseLong( event[1] );
				String node = event[2];
				int nid = nodemap.get(node);
				int pos = keymap.get(id);
				
				if(i==1)
					startts = ts;
				
				if(prev_pos == -1 || isword_by_time[prev_pos][pos]) {
					System.err.print(id +":" +nid +" " );
					msgcntarr[pos] +=1;
					//System.err.print(id +":" +ts +" " );
				} else {
					int segtime = (int) (prev_ts-startts)/1000;
					System.err.print( " [" + segtime + "sec]" );
					startts = ts;
					System.err.print( " ||| " + id + ":" + nid+" " );
					StringBuffer sb = new StringBuffer();
					for(int m=0; m<msgcntarr.length; m++) {
						sb.append( msgcntarr[m] ).append(",");
						msgcntarr[m] =0;
					}
					String veckey = sb.toString();
					ArrayList<Double> time = seqtime.get(veckey);
					if(time==null) {
						time = new ArrayList<Double>();
						time.add((double)segtime);
						seqtime.put(veckey,time);
					} else {
						time.add((double)segtime);
					}
					//System.err.println( sb.toString() +" " + segtime);
					msgcntarr[pos] +=1;
					System.err.print( " ||| " + id +":" +ts +" " );
				}
				prev_ts=ts;
				prev_pos = pos;
			}
			
			int segtime = (int) (prev_ts-startts)/1000;
			StringBuffer sb = new StringBuffer();
			for(int m=0; m<msgcntarr.length; m++) {
				sb.append( msgcntarr[m] ).append(",");
			}
			String veckey = sb.toString();
			ArrayList<Double> time = seqtime.get(veckey);
			if(time==null) {
				time = new ArrayList<Double>();
				time.add((double)segtime);
				seqtime.put(veckey,time);
			} else {
				time.add((double)segtime);
			}
			//System.err.println( sb.toString() +" " + segtime);
			System.err.print( " [" + (prev_ts-startts)/1000 + "sec]" );
			System.err.println();
			line = reader.readLine();
		}
		reader.close();
		
		// print sequence
		
		for(Entry<String, ArrayList<Double>> e: seqtime.entrySet()) {
			System.err.println(e.getKey() +" " + e.getValue().size()+" " +median(e.getValue()) +" " + percentile(e.getValue(), 0.9));
			//System.err.println(e.getKey() +" " + e.getValue().size()+" "+ Arrays.deepToString(e.getValue().toArray()) );
		}
		
		
		ObjectOutputStream dataout = new ObjectOutputStream(
				new FileOutputStream(
						new File(seqfile.getParentFile(), "segmentdata.bin")) );
		dataout.writeObject(timemean);
		dataout.writeObject(timestddev);
		dataout.writeObject(timemedian);
		dataout.writeObject(timemad);
		dataout.writeObject(keymap);
		dataout.close();
	}
	
	
	private static double mean(ArrayList<Double> arr) {
		double sum =0;
		for(double i: arr) {
			sum +=i;
		}
		return sum/arr.size();
	}
	
	private static double median(ArrayList<Double> arr) {
		Collections.sort(arr);
		return arr.get(arr.size()/2);
	}
	
	private static double percentile(ArrayList<Double> arr, double percentile) {
		Collections.sort(arr);
		return arr.get( (int) (arr.size()*percentile) );
	}
	
}
