package online;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.ObjectInputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import pca.MatrixUtils;
import scale.LogParser;
import scale.LogParserImpl;
import scale.ParsedMessageWritable;


public class MovingWindow {

	ConcurrentHashMap<String, ArrayList<Event>> eventqueues= new ConcurrentHashMap<String, ArrayList<Event>>();
	
	private static Hashtable<Integer, Integer> keymap;
	static double[][] timemean;
	static double[][] timestddev;
	static double[][] timemad;
	static double[][] timemedian;
	
	private static Hashtable<String, ArrayList<String>> byid = new Hashtable<String, ArrayList<String>>();
	
	
	public static void main(String[] args) throws Exception {
		
		
		File rawlog = new File("data\\online\\sortedlog");
		PrintStream out = new PrintStream(new FileOutputStream(new File(rawlog.getParentFile(), "rawTFVector_fix.txt")));
		
		MovingWindow stream = new MovingWindow();
		
		ObjectInputStream datain = new ObjectInputStream(new FileInputStream(
				new File(rawlog.getParentFile(), "segmentdata.bin") ));
		
		timemean = (double[][]) datain.readObject();
		timestddev = (double[][]) datain.readObject();
		timemedian = (double[][]) datain.readObject();
		timemad = (double[][]) datain.readObject();
		keymap = (Hashtable<Integer, Integer>) datain.readObject();
		
		
		BufferedReader reader = new BufferedReader(new FileReader(rawlog));
		
		ParsedMessageWritable buf = new ParsedMessageWritable();
		LogParser parser = new LogParserImpl();
		
		String line = reader.readLine();
		long currentts = 0;
		long starttime =-1;
		long endtime =0;
		while (line!=null) {
			boolean dodetection= false;
			if (line.trim().length()==0) {
				line = reader.readLine();
				continue;
			}
			if( parser.parseOneLine(line, buf, null) ){
				long ts = buf.getTs();
				if (starttime==-1) {
					starttime = ts;
				}
				if(currentts < ts) { 
					currentts = ts;
					dodetection = true;
				}
				int msgtype = buf.getLogid();
				String[] lbs = buf.getLabels();
				
				for(String lb: lbs) {
					if (lb.startsWith("blk_")) {
						// create event
						Event e = new Event(lb, ts, msgtype);
						stream.enqueueEvent(e);
//						if(dodetection) {
//							stream.find_ready_event_queue(currentts, 60, out);
//						}
					}
				}
			}
			
			line = reader.readLine();
		}
		endtime = currentts +60*1000;
		reader.close();
		
		
		for(long l=starttime; l<=endtime; l+=10*1000) {
			stream.find_ready_event_queue(l, 30);
		}
		
		
		for(Entry<String, ArrayList<String>> e: byid.entrySet()) {
			for(String vec: e.getValue()) {
				out.println(vec + " %" + e.getKey());
			}
		}
		
	}
	
	public void enqueueEvent(Event event) {
		ArrayList<Event> elist = eventqueues.get(event.id);
		if(elist==null) {
			elist = new ArrayList<Event>();
			elist.add(event);
			eventqueues.put(event.id, elist);
		} else {
			elist.add(event);
		}
	}
	
	public void find_ready_event_queue(long currentts, int window_len ) {
		
		int[] vecbuf = new int[timemean.length];
		
		int output_cnt =0;
		
		for(Entry<String, ArrayList<Event>> entry: eventqueues.entrySet()) {
			ArrayList<Event> elist = entry.getValue();
			Event firste = elist.get(0);
			
			if (firste.ts>currentts) {  // not happened yet
				continue;
			}
			
			// remove old events
			while(elist.size()>0) {
				Event e = elist.get(0);
				if ( ((currentts-e.ts)/1000) >window_len ) {
					elist.remove(0);
				} else {
					break;
				}
			}
			
			if (elist.size()==0) {
				eventqueues.remove(entry.getKey());
			} else {
				//System.err.println( "at " + currentts +" " + entry.getKey()+":" + Arrays.deepToString(elist.toArray()) );
				for(Event e: elist) {
					if (e.ts>currentts)
						break;
					int pos = keymap.get(e.msgtype);
					vecbuf[pos] +=1;
				}
				//outfile.println(elist.get(0) +" " +printAndClear(vecbuf));
				ArrayList<String> vecs = byid.get(elist.get(0).id);
				String s = printAndClear(currentts,vecbuf);
				if (s==null)
					continue;
				if (vecs==null) {
					vecs=new ArrayList<String>();
					vecs.add(s);
					byid.put(elist.get(0).id, vecs);
				} else {
					vecs.add(s);
				}
				output_cnt +=1;
			}	
		}
		
		System.err.println("ts=" +currentts +" output=" + output_cnt );
		
	}
	
	private String printAndClear(long ts, int [] arr) {
		StringBuffer sb = new StringBuffer();
		sb.append(ts).append(" ");
		boolean has_nonzero = false;
		for(int i=0; i<arr.length; i++) {
			if(arr[i]!=0) {
				has_nonzero = true;
			}
			sb.append(arr[i]).append(" ");
			arr[i] =0;
		}
		if (has_nonzero) {
			return sb.toString();
		} else {
			return null;
		}
	}
	
}
