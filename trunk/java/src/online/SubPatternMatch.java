package online;

import java.awt.Color;
import java.awt.Font;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import javax.swing.BorderFactory;

import pca.DetectionResult;
import pca.MatrixUtils;
import pca.PCADetection;
import scale.LogParser;
import scale.LogParserImpl;
import scale.ParsedMessageWritable;
import utils.LogFormatter;

public class SubPatternMatch {
	
	
	private static Logger LOG = LogFormatter.getLogger("SubPatternMatch");
	static {LOG.setLevel(Level.INFO);}
	
	private static Hashtable<Integer, Integer> keymap;
	private static Hashtable<Integer, Integer> revkeymap;
	//private static File rawlogfile = new File(ConfigParam.DATASET_DIR, "data.sorted");
	private static File rawlogfile = new File(ConfigParam.DATASET_DIR, "sorted.log.gz");
	
	static ConcurrentHashMap<String, QueuedEvents> eventqueues= new ConcurrentHashMap<String, QueuedEvents>();
	
	//private static ArrayList<Event> allevents = new ArrayList<Event>();
	
	static SeqPattern[] patterns;
	
	static HashSet<Integer> rareEvents = new HashSet<Integer>();
	
	static HashSet<Integer> nonPatternEvents = new HashSet<Integer>();
	
	//static ArrayList<String> rareEventIds = new ArrayList<String>();
	
	static PrintWriter nonpattern_out;
	static PrintWriter rareEventIds_out;
	static PrintWriter detectionTime_out;
	static PrintWriter bufSize_out;
	
	// statistics 
	static int event_cnt =0;
	static int matched_cnt = 0;
	static int timeout_cnt = 0;
	static int rare_event_cnt = 0;
	static int non_pattern_cnt = 0;
	static int[] pattern_cnt;
	
	static DemoGUIFrame f;
	static DemoGUIStatFrame ff;
	static String[] idsInGUI;
	static PCADetection detect;
	
	static final int NUM_ANOMALY_TO_SHOW = 8;
	static String[] recentAnomalies = new String[NUM_ANOMALY_TO_SHOW]; 
	
	public static void removeFromGUI(String id) {
		for(int i=0; i< idsInGUI.length; i++) {
			if(idsInGUI[i]!=null && idsInGUI[i].equals(id)) {
				idsInGUI[i]=null;
				//f.areas[i].setBackground(Color.white);
				f.areas[i].setText("");
				f.areas[i].setBorder(BorderFactory.createTitledBorder("Empty"));
				break;
			}
		}
	}
	
	private static void setToTimeOutColor(String id) {
		int slot = findGUIBufferSlot(id);
		//System.err.println("slot = " + slot);
		if(slot>=0 && shouldSlowDown()) {
			f.areas[slot].setBackground(Color.RED);
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			f.areas[slot].setBackground(Color.WHITE);
		}
	}
	
	private static void updateGUIPatternCompleteColor(String id, int patternid) {
		int slot = findGUIBufferSlot(id);
		//System.err.println("slot = " + slot);
		ff.addPatternCount(patternid);
		if(slot>=0 && shouldSlowDown()) {
			String originaltext = f.areas[slot].getText();
			f.areas[slot].setBackground(Color.GREEN);
			f.areas[slot].setText(" P-"+patternid);
			f.areas[slot].setFont(new Font("Arial", Font.PLAIN, 48));
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			f.areas[slot].setBackground(Color.WHITE);
			f.areas[slot].setFont(new Font("Arial", Font.PLAIN, 14));
			f.areas[slot].setText(originaltext);
		}
	}
	
	public static int findGUIBufferSlot(String id) {
		int firstempty = -1;
		for(int i=0; i< idsInGUI.length; i++) {
			if(idsInGUI[i] ==null) {
				if(firstempty<0)
					firstempty = i;
			} else if(idsInGUI[i].equals(id)) {
				return i;
			}
		}
		if(firstempty>=0) {
			idsInGUI[firstempty] = id;
		}
		return firstempty;
	}
	
	public static void displaySeqInBufferSlot(String id) {
		int slot = findGUIBufferSlot(id);
		//System.err.println("slot = " + slot);
		if(slot>=0) {
			QueuedEvents queue = eventqueues.get(id);
			f.areas[slot].setText( guiDisplayString(queue) );
			f.areas[slot].setBorder(BorderFactory.createTitledBorder(id));
			if(shouldSlowDown()) {
				f.areas[slot].setBackground(Color.yellow);
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				f.areas[slot].setBackground(Color.WHITE);
			}
		}
		int more = eventqueues.size()-idsInGUI.length;
		if(more >0) {
			f.bufSize.setText("... ...\nand\n" + more +"\nmore...");
		} else {
			f.bufSize.setText("");
		}
	}
	
	public static String guiDisplayString(QueuedEvents queue) {
		StringBuffer sb = new StringBuffer();
		sb.append("Type Timeout\n");
		//sb.append( cntvecToMultiLineStr( seqToCntVector(queue.queuedEvents) ) );
		for(int i=0; i<queue.queuedEvents.size(); i++) {
			Event e = queue.queuedEvents.get(i);
			long to = queue.timeOuts.get(i);
			int id = revkeymap.get(e.msgtype);
			sb.append(id).append("     ");
			if(to>0) {
				sb.append( (to-starttime)/1000 );
			} else {
				sb.append("-");
			}
			sb.append("\n");
		}
//		sb.append("Expected Pattern: ");
//		for(SeqPattern p: queue.candiatePatterns)
//			sb.append(p).append(" ");
		
		return sb.toString();
	}
	
	static int anomalycnt = 0;
	public static void guiDoPCA(String vecrow) {
		f.pcaVector.setText(vecrow);
		if (shouldSlowDown()) {
			f.pcaVector.setBackground(Color.RED);
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
			}
			f.pcaVector.setBackground(Color.WHITE);
		}
		
		DetectionResult result = detect.isAbnormal(vecrow);
		if(result.isAbnormal()) {
			f.pcaResult.setText( "ABNORMAL" );
			f.pcaResult.setBackground(Color.RED);	
			anomalycnt +=1;
			f.anomalycnt.setText(anomalycnt+"");
			displayRecentAnomaly(vecrow);
		} else {
			f.pcaResult.setText("NORMAL");
			f.pcaResult.setBackground(Color.GREEN);
		}
		f.pcaVector.setText(vecrow);
	}
	
	private static void displayRecentAnomaly(String vecrow) {
		int index = anomalycnt%recentAnomalies.length;
		int start = vecrow.indexOf("%");
		if(start <0 )
			return;
		String blkid = vecrow.substring(start+1);
		
		// see if block id already in buffer
		for(String s: recentAnomalies) {
			if(s==null) {
				continue;
			}
			if(s.equals(blkid)) {
				return;
			}
		}
		
		recentAnomalies[index] = blkid;
		StringBuffer sb = new StringBuffer();
		for(String s: recentAnomalies) {
			if(s==null) {
				continue;
			}
			sb.append(s).append("\n");
		}
		f.anomalylist.setText(sb.toString());
		
		if (shouldSlowDown()) {
			f.anomalylist.setBackground(Color.RED);
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
			}
			f.anomalylist.setBackground(Color.WHITE);
		}
	}
	
	private static boolean shouldSlowDown(){
		return f.slowcheck.isSelected();
	}
	
	static BufferedReader log_file_in;
	static LogParser parser;
	static ParsedMessageWritable buf = new ParsedMessageWritable();
	static long starttime = -1;
	private static String nextLine() throws Exception{
		String line = log_file_in.readLine();
		if(line==null) {
			return null;
		}
		while(line.length()==0){
			line = log_file_in.readLine();
		}
		
		while(true) {
		try {
			f.rawlog.setText(line);
			//System.err.println(line);
			parser.parseOneLine(line, buf, null);
			int id = buf.getLogid();
			//System.err.println(id);
			if(!keymap.containsKey(buf.getLogid())) {
				line = log_file_in.readLine();
				continue;
			}
			String blkid = null;
			String[] labels = buf.getLabels();
			for(String t : labels ) {
				if(t.startsWith("blk_")){
					blkid = t;		
					break;
				}
			}
			if(blkid==null) {
				line = log_file_in.readLine();
				continue;
			}
			String s = buf.getTs() +"\t "+buf.getLogid()+" - " + blkid;
			if(starttime<0 && buf.getTs()>0) {
				starttime = buf.getTs();
			}
			
			String disp = "TIME="+((buf.getTs()-starttime)/1000) 
					+"\tMSG_TYPE="+buf.getLogid()
					+"\tID=" + blkid;
			f.parsedlog.setText(disp);
			return s;
		} catch (RuntimeException e) {
			line = log_file_in.readLine();
		}
		}
	}
	
	
	public static void main(String[] args) throws Exception{
		
		
		detect = new PCADetection( new File(ConfigParam.DATASET_DIR, "pca_matrices_nonpattern").getAbsolutePath() );
		f = new DemoGUIFrame();
		ff = new DemoGUIStatFrame();
		idsInGUI = new String[f.areas.length];
		
		
		File patternFile = new File(rawlogfile.getParent(),"patterns.bin");
		ObjectInputStream patternin = new ObjectInputStream( new FileInputStream( patternFile ) );
		rareEvents = (HashSet<Integer>) patternin.readObject();
		{  // read in all patterns
		LinkedHashMap<SeqPattern, SeqPattern> patterntmp = (LinkedHashMap<SeqPattern, SeqPattern>) patternin.readObject();
		patterns = new SeqPattern[patterntmp.size()+1];
		int cnt=0;
		StringBuffer sb = new StringBuffer();
		for(SeqPattern p : patterntmp.keySet()) {
			patterns[cnt] = p;
			sb.append(p).append("\n");
			cnt++;
		}
		System.err.println();
		
		ff.displayPatterns(patterns);
		}
		patternin.close();
		
		keymap = KeymapUtils.keymap;
		revkeymap = KeymapUtils.revkeymap;
		
//		ObjectInputStream datain = new ObjectInputStream(
//				new FileInputStream( new File(rawlogfile.getParentFile(), "keymap.bin")) );
//		keymap = (Hashtable<Integer, Integer>) datain.readObject();
//		SeqPattern.keymap = keymap;
//		revkeymap = new Hashtable<Integer, Integer>();
//		for(Entry<Integer, Integer> e: keymap.entrySet()) {
//			revkeymap.put(e.getValue(), e.getKey());
//		}
//		SeqPattern.rev_keymap = revkeymap;
//		datain.close();
		
		LOG.info( Arrays.deepToString( keymap.entrySet().toArray() ) );
		LOG.info("rare events: " + Arrays.deepToString( rareEvents.toArray() ));
		
		// build nonPatternEventTable
		// nonPatternEvent 
		//		== events that are never seen in patterns, but not rare either
		int[] sum = new int[keymap.size()];
		for(SeqPattern p: patterns ) {
			if(p==null)
				continue;
			for(int i=0; i< p.countvec.length; i++) {
				if (p.countvec[i]!=0) {
					sum[i] +=1;
				}
			}
		}
		for(int i=0; i<sum.length; i++) {
			if (sum[i]==0 && !rareEvents.contains(i)) {
				nonPatternEvents.add(i);
				//System.err.println("non-pattern event " + i);
			}
		}
		LOG.info("non-pattern events: " + Arrays.deepToString( nonPatternEvents.toArray() ));
		
		// experimental -- adding all non-pattern event as a new pattern
		{
			int[] cntarr = new int[keymap.size()];
			for(int i=0; i<cntarr.length; i++) {
				if( nonPatternEvents.contains(i) ) {
					cntarr[i] = Integer.MAX_VALUE;
				}
			}
			SeqPattern p = new SeqPattern();
			p.countvec = cntarr;
			p.duration_mean = ConfigParam.target_detection_time_in_sec;
			p.duration_median = ConfigParam.target_detection_time_in_sec;
			patterns[patterns.length-1] = p;
		}
		
		// stat output files
		nonpattern_out = new PrintWriter(
				new FileWriter( 
						new File( rawlogfile.getParent(), "nonpattern_matrix.txt")));
		rareEventIds_out = new PrintWriter(
				new FileWriter(
						new File( rawlogfile.getParent(), "rareEvents.txt")));
		detectionTime_out = new PrintWriter(
				new FileWriter(
						new File( rawlogfile.getParent(), "detect_time.txt")));
		bufSize_out = new PrintWriter(
				new FileWriter(
						new File( rawlogfile.getParent(), "buf_size.txt")));
		
		pattern_cnt = new int[patterns.length+1];
		
		// reading log files, emulate online detection.
		parser = new LogParserImpl();
		log_file_in = new BufferedReader( new InputStreamReader( new GZIPInputStream(new FileInputStream(rawlogfile) ) ));
		String line = nextLine();
		//System.err.println("PARSED::" +line);
		String[] parts = line.split("\\s+");
		long startts = Long.parseLong( parts[0] );
		
		boolean datadone = false;
		long endtime = -1;
		Event event = strToEvent(line);
		
		timestamp:
		for (long t = startts; true ; t+=1000) { // at every second
			// first enqueue all events happens at/before that second
			if (!datadone) {
//				if(event.ts-t >10*1000 && eventqueues.size()==0) {
//					LOG.info("fast forward t");
//					t = event.ts - 1000;
//				}
				while (event.ts <= t) {
					//f.parsedlog.setText(line);
					enqueueEvent(event);
					event_cnt+=1;
					f.totalcnt.setText(event_cnt+"");
					
					if(shouldSlowDown() && findGUIBufferSlot(event.id)>0 ) {
						Thread.sleep(500);
					}
					
					processOneSequence(event.ts, eventqueues.get(event.id), event.id);
					if(eventqueues.get(event.id).queuedEvents.size()==0) {
						eventqueues.remove(event.id);
						removeFromGUI(event.id);
					}
					
					
					if(event_cnt%100000 ==0) {
						LOG.info("processed " + event_cnt +" events..");
					}
					line = nextLine();
					if (line==null) {
						datadone = true;
						endtime = t;
						continue timestamp;
					} else {
						event = strToEvent(line);	
					}
				}
			} else {
				// wait another max time window
				if (t-endtime > ConfigParam.target_detection_time_in_sec*1000) 
					break;
			}
			
			// calculate and print queue length
			int total = 0;
			for(QueuedEvents e: eventqueues.values()) {
				total += e.queuedEvents.size();
			}
			bufSize_out.println(total+" " + eventqueues.size());
			if (LOG.isLoggable(Level.FINEST))  {	
				LOG.finest("At "+ t
					+ " total queued events = " + total
					+ " number of blocks = "+eventqueues.size());
			}
			
			// then do an detection on every block
			for(Entry<String, QueuedEvents> entry:  eventqueues.entrySet()) {
				QueuedEvents elist = entry.getValue();
				//matchOneQueue(t, elist);
				boolean again = true;
				while(again) {
					again=processOneSequence(t, elist, entry.getKey());
				}
				if(elist.queuedEvents.size()==0) {
					eventqueues.remove(entry.getKey());
					removeFromGUI(entry.getKey());
				}
			}
			
			//System.err.println("At time " + t + " queue len=" + eventqueues.size());
		}
		
		
		// print what are left in the queues
		LOG.info("Left over block sequences=" + eventqueues.size());
		for(Entry<String, QueuedEvents> entry: eventqueues.entrySet()) {
			LOG.info( entry.getKey() +" " + entry.getValue());
		}
		
		LOG.info("event_cnt=" +event_cnt 
				+" rare_events=" +rare_event_cnt 
				+" non-pattern_event=" +non_pattern_cnt);
		
		LOG.info("matched_pattern=" + matched_cnt 
				+" timed_out="+timeout_cnt );
		LOG.info("patterns matched:"); 
		MatrixUtils.prettyPrint(pattern_cnt);
		
		
		nonpattern_out.close();
		rareEventIds_out.close();
		detectionTime_out.close();
		bufSize_out.close();
		
	}
	
	private static Event strToEvent(String line) {
		String[] parts = line.split("\\s+");
		long ts = Long.parseLong(parts[0]);
		int msgtype = Integer.parseInt(parts[1]);
		Event e = new Event(parts[3], ts, msgtype );
		return e;
	}
	
	
	public static void enqueueEvent(Event event) {
		if (event==null) {
			LOG.warning("NULL event ");
			return;
		}
		QueuedEvents elist = eventqueues.get(event.id);
		//System.err.println(event.msgtype);
		if(!keymap.containsKey(event.msgtype))
			return;
		
		event.msgtype = keymap.get(event.msgtype);
		long currentts = event.ts;
		if(elist==null) {
			elist = new QueuedEvents();
			eventqueues.put(event.id, elist);
		} 
		
		// add event and initial timeout value
		if( rareEvents.contains(event.msgtype) ) {
			elist.timeOuts.add( event.ts );
			//rareEventIds.add(event.id);
			rareEventIds_out.println("%" + event.id +" " + revkeymap.get(event.msgtype));
			rare_event_cnt +=1;
			// currently we don't enqueue rare events
			// elist.queuedEvents.add(event);
		} else if( nonPatternEvents.contains(event.msgtype) ) {
			elist.timeOuts.add( event.ts 
					+ ConfigParam.non_pattern_max_wait*1000 );
			non_pattern_cnt +=1;
			elist.queuedEvents.add(event);
		} else {
			elist.timeOuts.add(-1L);
			elist.queuedEvents.add(event);
		}
		
		displaySeqInBufferSlot(event.id);
		
	}
	
//	static ArrayList<String> nonPatternData = new ArrayList<String>();
	
	private static boolean processOneSequence(long currentts, QueuedEvents elist, String id) {
		
		int[] seqvec = seqToCntVector(elist.queuedEvents);
		// match each pattern
		for(int i=0; i< patterns.length; i++) {
			if(patterns[i]==null)
				continue;
			int[] patvec = patterns[i].countvec;
			int[] diff = minus(patvec,seqvec);
			
			// see if it is a complete match
			if(isCompleteMatch(diff)) {
				// remove events that matches the pattern
				//System.err.println("complete pattern " + i);
				handlePatternComplete(currentts, elist, patvec);
				updateGUIPatternCompleteColor(id, i);
				displaySeqInBufferSlot(id);
				matched_cnt += 1;
				pattern_cnt[i] +=1;
				if(LOG.isLoggable(Level.FINER)) {
					LOG.finer("matched pattern " + i +": " + cntvecToEventStr(patvec));
				}
				if(elist.queuedEvents.size()==0) {
					return false;
				} else {
					return true; // do it again and adjust other partial matches
				}
			} else if(isPartialMatch(diff, patvec)) {
				// update timeout values for each matching event
				double pat_dur = ConfigParam.getMaxDuration(patterns[i]);
				if(pat_dur == 0) {
					pat_dur = 10;
				}
				handlePartialMatch(elist, patvec, pat_dur);
				if(LOG.isLoggable(Level.FINEST)) {
					LOG.finest("patial matched pattern " + i +": " + cntvecToEventStr(patvec));
				}
				// continue with next pattern
			} else {
				// no match
				// continue with next pattern
			}
		}
		
		// see if there is any time out events
		int[] toseq = handleTimeOutSeq(currentts, elist, id);
		
		if(toseq!=null) {
			//nonPatternData.add(seqToCntVectorStr(currentts, id ,elist.queuedEvents));
			//System.err.println("number of timeouts= " + MatrixUtils.sum(toseq));
			nonpattern_out.println(cntvecToCntVectorStr(currentts, id, toseq));
			//nonpattern_out.println(seqToCntVectorStr(currentts, id ,elist.queuedEvents));
			timeout_cnt +=1;
			String vecrow = cntvecToCntVectorStr(currentts, id, toseq);
			guiDoPCA(vecrow);
			setToTimeOutColor(id);
		}
		return false;  // don't have to do it again
	}
	
	private static void handlePatternComplete(long currentts, QueuedEvents elist, int[] patvec) {
		int[] tmp = copy(patvec);
		Iterator<Event> iter = elist.queuedEvents.iterator();
		Iterator<Long> iter_to = elist.timeOuts.iterator();
		while(iter.hasNext()) {
			Event e = iter.next();
			Long to = iter_to.next();
			if( tmp[e.msgtype]>0 ) {
				tmp[e.msgtype] -=1;
				iter.remove();
				iter_to.remove();
				detectionTime_out.println( (currentts-e.ts)/1000.0 ); // matched, detection time is zero
			}
		}
	}
	
	private static void handlePartialMatch(QueuedEvents elist, int[] patvec, double patdur) {
		// find the matching set of events
		int[] tmp = copy(patvec);
		
		long[] firstPatEventTs = new long[keymap.size()];
		for(int i=0; i<firstPatEventTs.length; i++) {
			firstPatEventTs[i] =-1;
		}
		
		for(int i=0; i< elist.queuedEvents.size(); i++) {
			Event e = elist.queuedEvents.get(i);
			long to = elist.timeOuts.get(i);
			if(tmp[e.msgtype]==0 && patvec[e.msgtype] !=0) {
				// used up pattern event, need to start a new sequence
				tmp[e.msgtype] = patvec[e.msgtype];
				firstPatEventTs[e.msgtype] = -1;
			}
			if( tmp[e.msgtype]>0 ) {
				// is a partial match
				if (firstPatEventTs[e.msgtype] ==-1) { // first event for pattern
					firstPatEventTs[e.msgtype] = e.ts;
				}
				// trying to set timeout for a specific pattern to be the same..
				long newto = firstPatEventTs[e.msgtype] + Math.round(patdur*1000);
				if(to==-1) {
					elist.timeOuts.set(i, newto);
				} else if (newto > to) {  // find a reason to wait longer..
					elist.timeOuts.set(i, newto);
				}
				tmp[e.msgtype] -=1;
			} 
		}
	}
	
	private static int[] handleTimeOutSeq(long currentts, QueuedEvents elist, String id) {
		boolean has_timeout=false;
		int[] timedOutSequence = new int[keymap.size()];
		double last_to_event_ts = 0;
		Iterator<Event> iter = elist.queuedEvents.iterator();
		Iterator<Long> iter_to = elist.timeOuts.iterator();
		while(iter.hasNext()) {
			Event e = iter.next();
			Long to = iter_to.next();
			if(to<currentts) {
				timedOutSequence[e.msgtype] += 1;
				iter.remove();
				iter_to.remove();
				last_to_event_ts = e.ts;
				//System.err.println("timeout = " +to + " now=" + currentts);
				has_timeout = true;
			}
		}
		if(has_timeout) {
			
			double detection_time = (currentts - last_to_event_ts)/1000;
			
			if(LOG.isLoggable(Level.FINE)) {
				LOG.fine("timed out: " + cntvecToEventStr(timedOutSequence) 
						+ " blockid=" + id 
						+ " detection time = " + detection_time +" sec");
			}
			detectionTime_out.println(detection_time);
			//System.err.println("number of timeouts= " + MatrixUtils.sum(timedOutSequence));
			return timedOutSequence;
		} else 
			return null;
	}
	
	private static int[] copy(int[] vec) {
		int[] tmp = new int[vec.length];
		System.arraycopy(vec, 0, tmp, 0, vec.length);
		return tmp;
	}
	
	// compute the difference
	private static int[] minus(int[] patvec, int[]seqvec) {
		int[] ret = new int[patvec.length];
		for(int i=0; i< patvec.length; i++) {
			ret[i] = patvec[i]-seqvec[i];
		}
		return ret;
	}
	
	// returns true iff every number in diff <=0
	private static boolean isCompleteMatch(int[] diff) {
		for(int i=0; i<diff.length; i++) {
			if (diff[i]>0) {
				return false;
			}
		}
		return true;
	}
	
	// return true iff there exist diff[i]>0 && diff[i]<pattern[i]
	private static boolean isPartialMatch(int[] diff, int[] patvec) {
		for(int i=0; i<diff.length; i++) {
			if (patvec[i]>0 && diff[i]<patvec[i]) {
				return true;
			}
		}
		return false;
	}

	
//	private static void matchOneQueue(long currentts, QueuedEvents elist) {
//		
//		String eventid = elist.queuedEvents.get(0).id;
//		// see if elist matches any pattern
//		int match_cnt=0;
//		for(SeqPattern p: all_patterns.keySet()){
//			int matches = elist.matchPattern(currentts, p);
//			if(matches ==1 ) { // partial match
//				match_cnt+=1;
//				//System.err.println("patial matches " + p.pattern_id + " " +event.id);
//			} else if (matches ==2) {
//				match_cnt+=1;
//				//System.err.println("completely matches " + p.pattern_id + " " +eventid);
//				matched_cnt += 1;
//				// if empty, remove from queue
//				if(elist.queuedEvents.size()==0) {
//					eventqueues.remove(eventid);
//				}
//				break;
//			} else {
//				;// do nothing
//			}
//		}
//		
//		// check if we should timeout this sequence;
//		if(currentts>elist.timeout_ts && ! (elist.timeout_ts==0) ) {
//			//System.err.println("timed out sequence " + eventid + " " + elist );
//			eventqueues.remove(eventid);
//			nonPatternData.add(seqToCntVectorStr(currentts, eventid ,elist.queuedEvents));
//			timeout_cnt += 1;
//		}
//		
//		if (match_cnt==0) {
//			System.err.println("NON-matching pattern " +Arrays.deepToString( elist.queuedEvents.toArray() ) );
//		}
//		
//	}
	
	
	private static String cntvecToMultiLineStr(int[] cntvec) {
		StringBuffer sb = new StringBuffer();
		for(int i=0; i< cntvec.length; i++) {
			if(cntvec[i] >0) {
				sb.append(cntvec[i]).append(" x ").append( revkeymap.get(i) ).append("\n");
			}
		}
		return sb.toString();
	}
	
	private static String cntvecToEventStr(int[] cntvec) {
		StringBuffer sb = new StringBuffer();
		for(int i=0; i< cntvec.length; i++) {
			if(cntvec[i] >0) {
				sb.append(cntvec[i]).append("x").append( revkeymap.get(i) ).append(" ");
			}
		}
		return sb.toString();
	}
	
	private static int[] seqToCntVector(ArrayList<Event> elist) {
		int[] cntvec = new int[keymap.size()];
		
		for(Event e: elist) {
			int pos = e.msgtype;
			cntvec[pos] += 1;
		}
		
		return cntvec;
	}
	
	private static String seqToCntVectorStr(long ts, String blockid, ArrayList<Event> elist) {
		int[] cntvec = new int[keymap.size()];
		for(Event e: elist) {
			int pos = e.msgtype;
			cntvec[pos] += 1;
		}
		
		StringBuffer sb = new StringBuffer();
		for(int i: cntvec) {
			sb.append(i).append(" ");
		}
		
		return ts+" " +sb.toString()+" %"+blockid;
	}
	
	private static String cntvecToCntVectorStr(long ts, String blockid, int[] toseq) {
		
		StringBuffer sb = new StringBuffer();
		for(int i: toseq) {
			sb.append(i).append(" ");
		}
		
		return ts+" " +sb.toString()+" %"+blockid;
	}
	
	
}
