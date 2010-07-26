package online;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import pca.MatrixUtils;
import scale.LogParser;
import scale.LogParserImpl;
import scale.ParsedMessageWritable;


public class EventStream {

	private static File seqfile = new File("data\\online\\hdfs_sm1");
	
	ConcurrentHashMap<String, ArrayList<Event>> eventqueues= new ConcurrentHashMap<String, ArrayList<Event>>();
	
	private static Hashtable<Integer, Integer> keymap;
	static double[][] timemean;
	static double[][] timestddev;
	static double[][] timemad;
	static double[][] timemedian;
	
	static double[] cutoff_threshold;
	
	public static void main(String[] args) throws Exception {
		
		
		EventStream stream = new EventStream();
		
		ObjectInputStream datain = new ObjectInputStream(
				new FileInputStream( new File(seqfile.getParentFile(), "segmentdata.bin")) );
		
		timemean = (double[][]) datain.readObject();
		timestddev = (double[][]) datain.readObject();
		timemedian = (double[][]) datain.readObject();
		timemad = (double[][]) datain.readObject();
		keymap = (Hashtable<Integer, Integer>) datain.readObject();
		
		cutoff_threshold = new double[timemean.length];
		for(int i=0; i< timemean.length; i++) {
			for (int j=0; j<timemean[i].length; j++) {
				if (timemedian[i][j]>30) 
					continue;
				double thresh = timemedian[i][j] + 4.5*timemad[i][j];
				if(thresh > cutoff_threshold[i])
					cutoff_threshold[i] = thresh;
			}
		}
		MatrixUtils.prettyPrint(cutoff_threshold);
		
		BufferedReader reader = new BufferedReader(
				new FileReader(
						new File(seqfile.getParentFile(), "sortedlog")) );
		
		ParsedMessageWritable buf = new ParsedMessageWritable();
		LogParser parser = new LogParserImpl();
		
		String line = reader.readLine();
		long currentts = 0;
		
		while (line!=null) {
			if (line.trim().length()==0) {
				line = reader.readLine();
				continue;
			}
			if( parser.parseOneLine(line, buf, null) ){
				long ts = buf.getTs();
				currentts = ts;
				int msgtype = buf.getLogid();
				String[] lbs = buf.getLabels();
				
				for(String lb: lbs) {
					if (lb.startsWith("blk_")) {
						// create event
						Event e = new Event(lb, ts, msgtype);
						stream.enqueueEvent(e);
						stream.find_ready_event_queue(currentts);
					}
				}
			}
			
			line = reader.readLine();
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
	
	public void find_ready_event_queue(long currentts) {
		for(Entry<String, ArrayList<Event>> entry: eventqueues.entrySet()) {
			ArrayList<Event> elist = entry.getValue();
			Event laste = elist.get(elist.size()-1);
			int pos = keymap.get(laste.msgtype);
			double time = (currentts-laste.ts)/1000;
			double cutoff = cutoff_threshold[pos];
			if (time>cutoff) {
				// ready for detection
				System.err.println( entry.getKey()+":" + Arrays.deepToString(elist.toArray()) );
				eventqueues.remove(entry.getKey());
			}
		}
	}
	
}
