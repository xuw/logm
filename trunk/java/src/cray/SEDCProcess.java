package cray;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.TimeZone;

import org.apache.commons.collections.functors.ForClosure;


public class SEDCProcess {
	
	static final String DATA_DIR = "C:/Users/xuw/logExpData/cray/ds6/SEDC_FILES";
	//static SimpleDateFormat dateformat = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy");
	static SimpleDateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	public static void main(String[] args) throws Exception {
		
		dateformat.setTimeZone(TimeZone.getTimeZone("GMT"));  // for correct offset with Matlab
		
		File[] traces = new File(DATA_DIR).listFiles();
		
		System.err.println(Arrays.deepToString(traces));
		System.setOut(new PrintStream(new FileOutputStream("c:/users/xuw/tmpout")));
		
		PriorityQueue<TraceLogRecord> entirelog = new PriorityQueue<TraceLogRecord>();
		
		for(File file: traces) {
			
			String fn = file.getName();
			if(fn.indexOf("L0_VOLTS")<0)
				continue;
			
			String[] fnarr = fn.split("_");
			String loglevel = fnarr[0];
			String logtype = fnarr[1];
			
			System.err.println("%FILE:: " + fn);
			
			
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String headerstr = reader.readLine(); // skip colname
			String[] header = headerstr.split(",");
			
			System.err.println("number_dimensions : " + header.length);
			String line = reader.readLine();
			
			int linecnt = 0;
			long lastts=0;
			while(line!=null) {
				String[] fields = line.split(",",-1);
				
				if (fields.length!=header.length) {
					System.err.println("Wrong report format :: "+ fn+"::" + linecnt +"::" +line+"::"+fields.length+"::"+Arrays.deepToString(fields));
					line =reader.readLine();
					linecnt+=1;
					continue;
				}
				String nodeid = fields[0];
				
				//System.err.println(nodeid);
				String [] nodeid_parsed = parseNodeName(loglevel, nodeid);
				String cabinet = nodeid_parsed[0];
				String chassis = nodeid_parsed[1];
				String slot = nodeid_parsed[2];
				
				long ts = parseTimeStamp( fields[1] );
				
				String[] data = Arrays.copyOfRange(fields, 2, fields.length);
				
				// cluster time stamp
				if(ts - lastts <20*1000) {
					ts = lastts;
				} else {
					lastts = ts; // update to next bucket
				}
				
				TraceLogRecord rec = new TraceLogRecord(ts, cabinet,chassis,slot,data);
				entirelog.add(rec);
				
				//System.err.println(parseTimeStamp(ts) + " " + cabinet +" " + chassis +" " + slot);
				line =reader.readLine();
				linecnt+=1;
			}
			
		}
		

		
		
		// compute number of dimensions
		
		ArrayList<String> allnodes = new ArrayList<String>();
		
		for(TraceLogRecord r : entirelog) {
			String nn = r.cabinets+r.chassis+r.slot;
			if (allnodes.contains(nn))
				continue;
			else
				allnodes.add(nn);
		}
		Collections.sort(allnodes);
		System.err.println("%" +Arrays.deepToString(allnodes.toArray()));
		
		TraceLogRecord r = entirelog.poll();
		int recordlen = r.record.length;
		
		String[] result = new String[allnodes.size()*recordlen];
		long lastts =0;
		
		while(r!=null) {
			//System.err.println(r);
			long ts = r.time;
			if (ts!=lastts) {
				
				lastts = ts;
				//output..
				System.out.println(getMatlabFriendlyArray(ts, result));
				result = new String[allnodes.size()*recordlen];
			}
			//flatvector = arraycat(flatvector, r.record);
			int startindex = allnodes.indexOf(r.cabinets+r.chassis+r.slot);
			if (r.record.length != recordlen){
				System.err.println("inconsistent record length??");
			}
			try{
				copytoresult(result, r.record, startindex*recordlen);
			} catch(ArrayIndexOutOfBoundsException ex) {
				System.err.println(startindex+"::"+r.record.length+"::" + Arrays.deepToString(r.record));
			}
			r =entirelog.poll();
		}
		
		
	}
	
	
	public static String getMatlabFriendlyArray(long ts, String[] arr) {
		StringBuffer sb = new StringBuffer();
		sb.append(ts).append(" ");
		for(int i=0; i<arr.length; i++) {
			String s = arr[i];
			if (s==null || s.equals("") || s.equals("NA"))
				s="NaN";
			sb.append(s).append(" ");
		}
		//System.err.println(cnt);
		return sb.toString();
	}
	
	public static String[] parseNodeName(String loglevel, String nodename) {
		String [] ret = new String[3];
		int leading_cols = 0;
		if(loglevel.equals("L0")) {
			// parse node id
			ret[0] = nodename.substring(leading_cols+0,leading_cols+4);
			ret[1] = nodename.substring(leading_cols+4,leading_cols+6);
			ret[2] = nodename.substring(leading_cols+6,leading_cols+8);
		} else {
			ret[0] = nodename.substring(2,6);
			ret[1] = "";
			ret[2] = "";
		}
		return ret;
	}
 	
	
	
	public static long parseTimeStamp(String ts){
		Date parsed;
		try {
			parsed = dateformat.parse(ts);
			//System.err.println(parsed +" " + new Date(parsed.getTime()));
			return parsed.getTime();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return -1;
	}
	
	public static String[] arraycat(String[] arr1, String[] arr2) {
		String[] ret = new String[arr1.length+arr2.length];
		System.arraycopy(arr1, 0, ret, 0, arr1.length);
		System.arraycopy(arr2, 0, ret, arr1.length, arr2.length);
		return ret;
	}
	
	public static void copytoresult(String[] result, String[] data, int startpos) {
		System.arraycopy(data, 0, result, startpos, data.length);
	}
}

class TraceLogRecord implements Comparable<TraceLogRecord>{
	
	String cabinets;
	long time;
	String chassis;
	String slot;
	String[] record;
	
	
	
	public TraceLogRecord( long time, String cabinets, String chassis,
			String slot, String[] record) {
		super();
		this.cabinets = cabinets;
		this.time = time;
		this.chassis = chassis;
		this.slot = slot;
		this.record = record;
	}



	public int compareTo(TraceLogRecord o) {
		int t=(int) (this.time-o.time);
		if (t!=0)
			return t;
		t = cabinets.compareTo(o.cabinets);
		if (t!=0)
			return t;
		t = chassis.compareTo(o.chassis);
		if (t!=0)
			return t;
		t = slot.compareTo(o.slot);
		if (t!=0)
			return t;
		return 0;
	}



	@Override
	public String toString() {
		return this.time+" " + this.cabinets +" " + this.chassis +" "+this.slot+" "+ record.length +":"+ Arrays.deepToString(record);
	}
	
	
	
}
