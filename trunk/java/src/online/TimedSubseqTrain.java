package online;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Random;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import pca.MatrixUtils;
import scale.LogParser;
import scale.LogParserImpl;
import scale.ParsedMessageWritable;

public class TimedSubseqTrain {
	
	
	//private static File rawlogfile = new File("data/online/largedata.seq");
	private static File rawlogfile = new File( ConfigParam.DATASET_DIR, "data.seq");
	
	ConcurrentHashMap<String, ArrayList<Event>> eventqueues= new ConcurrentHashMap<String, ArrayList<Event>>();
	private static Hashtable<Integer, Integer> keymap;
	
	private int[] df;
	
	HashSet<Integer> rareEvents = new HashSet<Integer>();
	
	static int original_blocks;
	
	public static void main(String[] args) throws Exception {
		
		//System.setErr(new PrintStream("data/online/stderr.txt"));
		
		TimedSubseqTrain train = new TimedSubseqTrain();
		
		keymap = KeymapUtils.keymap;
		
		// try to read key map file..
		File keymapfile = new File(rawlogfile.getParentFile(), "keymap.bin") ;
		if( keymapfile.exists() ) {
			ObjectInputStream datain = new ObjectInputStream(new FileInputStream(keymapfile) );
			keymap = (Hashtable<Integer, Integer>) datain.readObject();
			datain.close();
		} else {
			keymap = new Hashtable<Integer, Integer>();
		}
			
		// read log data file
		if(rawlogfile.getName().endsWith("parsed")) {
			train.readParsedEvents();
		} else if (rawlogfile.getName().endsWith("seq")) {
			train.readEventSequences(ConfigParam.doSample, ConfigParam.sampleRatio);
		} else {
			train.readAllEvents();
		}
		
//		File bindatafile = new File(rawlogfile.getParentFile(), rawlogfile.getName()+".bin");
//		if (bindatafile.exists()) {
//			System.err.println("reading from binary sequence cache... ");
//			ObjectInputStream datain = new ObjectInputStream(new FileInputStream(bindatafile ) );
//			train.eventqueues = (ConcurrentHashMap<String, ArrayList<Event>>) datain.readObject();
//			datain.close();
//		} else {
//			System.err.println(rawlogfile.getName());
//			// first read and parse in all events
//			if(rawlogfile.getName().endsWith("parsed")) {
//				train.readParsedEvents();
//			} else if (rawlogfile.getName().endsWith("seq")) {
//				train.readEventSequences(ConfigParam.doSample, ConfigParam.sampleRatio);
//			} else {
//				train.readAllEvents();
//			}
//			System.err.println("writing binary sequence cache...");
//			ObjectOutputStream dataout = new ObjectOutputStream(new FileOutputStream(bindatafile));
//			dataout.writeObject(train.eventqueues);
//			dataout.close();
//			System.err.println("binary sequence cache written ...");
//		}
		
		// calculate DF for each message type
		System.err.println("key map size is " + keymap.size());
		train.calculateDFAndBuildKeymap();
		MatrixUtils.prettyPrint(train.df);
		System.err.println(Arrays.deepToString( keymap.entrySet().toArray() ));
		System.err.println("key map size is " + keymap.size());
		
		// write updated keymap back
		ObjectOutputStream objout = new ObjectOutputStream(new FileOutputStream(keymapfile));
		objout.writeObject(keymap);
		objout.close();
		
		
		// for printing pattern..
//		SeqPattern.keymap = keymap;
//		SeqPattern.rev_keymap = new Hashtable<Integer, Integer>();
//		for(Entry<Integer, Integer> e: keymap.entrySet()) {
//			SeqPattern.rev_keymap.put(e.getValue(), e.getKey());
//		}
		
		
//		train.eliminateRareMsg();
				
//		for(Entry<String, ArrayList<Event>> entry: train.eventqueues.entrySet() ) {
//			train.printSequence( entry.getValue() );
//		}
		
		LinkedHashMap<SeqPattern, SeqPattern> all_patterns = new LinkedHashMap<SeqPattern, SeqPattern>();
		int blocks_left = train.eventqueues.size();
		original_blocks = blocks_left;
		
		blocks_left = train.eventqueues.size();
		
		
		long total_sequences =0;
		int pattern_cnt =0;
		while(blocks_left >= 0.1*original_blocks ) {
			
			System.err.println("number of blocks left before iteration: " + blocks_left );
			SeqPattern pattern = train.findPatternByTimeGap();
			train.calStatsAndRemovePatterns(pattern);
			
			if(pattern.frequency_second_pass<ConfigParam.min_support_for_pattern*original_blocks) {
				System.err.println("give up pattern, too few supports "+ pattern.frequency_second_pass 
						+".." + SeqPattern.cntVecToSeq(pattern.countvec) );
				continue;
			}
			
			double expected_duration = pattern.duration_median+pattern.duration_mad*5;
			if(expected_duration> ConfigParam.target_detection_time_in_sec ) {
				System.err.println("duration is too long for targeted detection time, skip.. " + expected_duration);
				continue;
			}
			
			SeqPattern existing_pattern = all_patterns.get(pattern);
			if (existing_pattern != null) { // pattern already exists..
				System.err.println("same as pattern " + existing_pattern.pattern_id);
				// update frequency of that pattern
				existing_pattern.frequency_second_pass += pattern.frequency_second_pass;
				for(int d : pattern.durations) {
					existing_pattern.durations.add(d);
				}
				for(int g : pattern.gaps) {
					existing_pattern.gaps.add(g);
				}
				System.err.println("pattern " + existing_pattern.pattern_id 
						+" frequency updated to " 
						+ existing_pattern.frequency_second_pass);
				continue; 
			} else {
				if (pattern.frequency_second_pass>0) {
					pattern.pattern_id = pattern_cnt;
					System.err.println("Discovered pattern id= " + pattern.pattern_id);
					all_patterns.put(pattern, pattern);
					System.err.println(pattern);
					total_sequences += pattern.frequency_second_pass;
					pattern_cnt += 1;
				} else {
					System.err.println("ignored - not frequent ");
				}
			}
			
			blocks_left = train.eventqueues.size();
			System.err.println("number of blocks left after iteration: " + blocks_left );
			System.err.println();
		}
		
//		for(Entry<String, ArrayList<Event>> entry: train.eventqueues.entrySet() ) {
//			train.printSequence(entry.getValue());
//		}
		
		
		train.computeRareEvent(total_sequences);
		
		File patternOutFile = new File(rawlogfile.getParent(),"patterns.bin");
		ObjectOutputStream patternout = new ObjectOutputStream( new FileOutputStream( patternOutFile ) );
		patternout.writeObject(train.rareEvents);
		patternout.writeObject(all_patterns);
		patternout.close();
		
		
		File gapsDir = new File(rawlogfile.getParent(), "gaps");
		train.writeGapIntervals(gapsDir, all_patterns);
		File durDir = new File(rawlogfile.getParent(), "durs");
		train.writeDurIntervals(durDir, all_patterns);
		// output file for PCA training
//		File pcaOutFile = new File(rawlogfile.getParent(),"patterns_pca_vector");
//		PrintStream pat_out = new PrintStream(pcaOutFile);
//		
//		for(Entry<SeqPattern, SeqPattern> entry: all_patterns.entrySet()) {
//			//System.err.println(entry.getKey());
//			SeqPattern pattern = entry.getKey();
//			for(int i=0; i< pattern.frequency_second_pass; i++) {
//				pat_out.println(pattern.toPCAString());
//			}
//		}
//		pat_out.close();
		
	}
	
	
	private void readAllEvents() throws IOException{
		
		BufferedReader reader = new BufferedReader( new FileReader(TimedSubseqTrain.rawlogfile) );
		ParsedMessageWritable buf = new ParsedMessageWritable();
		LogParser parser = new LogParserImpl();
		
		String line = reader.readLine();
		while(line!=null) {
			if (line.trim().length()==0) {
				line = reader.readLine();
				continue;
			}
			
			if( parser.parseOneLine(line, buf, null) ){
				
				long ts = buf.getTs();
				int msgtype = buf.getLogid();
				String[] lbs = buf.getLabels();
				
				for(String lb: lbs) {
					if (lb.startsWith("blk_")) {
						// create event
						Event e = new Event(lb, ts, msgtype);
						this.enqueueEvent(e);
					}
				}
			}
			
			line = reader.readLine();
		}
	}
	
	private void readParsedEvents() throws IOException{
		BufferedReader reader = new BufferedReader( new FileReader(TimedSubseqTrain.rawlogfile) );
		String line = reader.readLine();
		int linecnt = 0;
		
		while(line!=null) {	
				String[] parts = line.split("\\s");
				long ts = Long.parseLong(parts[0]);
				int msgtype = Integer.parseInt(parts[1]);
				String id = parts[3];
				Event e = new Event(id, ts, msgtype);
				this.enqueueEvent(e);
			line = reader.readLine();
			linecnt += 1;
			if(linecnt %10000 ==0) {
				System.err.println("processed " + linecnt +" lines");
			}
		}
	}
	
	private void readEventSequences(boolean sample, double samplerate) throws IOException{
		BufferedReader reader = new BufferedReader( new FileReader(TimedSubseqTrain.rawlogfile) );
		String line = reader.readLine();
		int linecnt = 0;
		int samplecnt =0;
		Random rand = new Random(ConfigParam.sampleSeed);
		while(line!=null) {
			if (rand.nextDouble()<samplerate) {
				String[] parts = line.split("\\s");
				String id = parts[0];

				for(int i=1; i<parts.length; i++) {
					try {
						String[] s = parts[i].split(":");
						int msgtype = Integer.parseInt(s[0]);
						long ts = Long.parseLong(s[1]);
						Event e = new Event(id, ts, msgtype);
						this.enqueueEvent(e);
					} catch (Exception e) {
						//System.err.println("exceptionSEQ: " + line);
					}
				}
				
				samplecnt +=1;
			}
			line = reader.readLine();
			linecnt += 1;
			if(linecnt %10000 ==0) {
				System.err.println("processed " + linecnt +" lines, samples=" + samplecnt);
			}
		}
	}
	
	
	public void calculateDFAndBuildKeymap() {
		
		Hashtable<Integer, Integer> cnttable = new Hashtable<Integer, Integer>();
		for(Entry<String, ArrayList<Event>> entry: eventqueues.entrySet()) {
			ArrayList<Event> events = entry.getValue();
			for(Event e: events) {
				Integer pos = keymap.get(e.msgtype);
				if(pos==null) {
					pos = keymap.size();
					keymap.put(e.msgtype, pos);
				}
				Integer t = cnttable.get(pos);
				if (t==null) {
					cnttable.put(pos, 1);
				} else {
					cnttable.put(pos, t+1);
				}
			}
		}
		
		int size = cnttable.size()>keymap.size()?cnttable.size():keymap.size();
		this.df = new int[size];
		for(Entry<Integer, Integer> entry: cnttable.entrySet()) {
			this.df[entry.getKey()] = entry.getValue();
		}
	}
	
	public void computeRareEvent(long total_seqs) {
		System.err.println("total sequences=" + total_seqs);
		int[] t = new int[this.df.length];
		System.arraycopy(this.df, 0, t, 0, this.df.length);
		int total_rare = 0;
		double total_allowed = ConfigParam.ratio_rare_events*total_seqs;
		while(true) {
			// find event with min df
			int mincnt=Integer.MAX_VALUE;
			int minind =-1;
			for(int i=0; i<t.length; i++) {
				if(t[i]<mincnt) {
					mincnt=t[i];
					minind=i;
				}
			}
			t[minind]= Integer.MAX_VALUE;
			if(total_rare+mincnt > total_allowed) 
				break;
			total_rare += mincnt;
			rareEvents.add(minind);
			System.err.println("rareEvent: " + minind + " cnt=" +mincnt);
		}
		
	}
	
	
	public void eliminateRareMsg() {
		
		int total_events =0;
		for(int d: this.df) {
			total_events +=d;
		}
		
		for(int i=0; i< this.df.length; i++) {
			if (this.df[i] <0.001*total_events ) {
				this.rareEvents.add(i);
			}
		}
		
		for(Entry<String, ArrayList<Event>> entry: eventqueues.entrySet()) {
			ArrayList<Event> events = entry.getValue();
			Iterator<Event> iter = events.iterator();
			while(iter.hasNext()) {
				Event e = iter.next();
				int pos = keymap.get(e.msgtype);
				if( this.rareEvents.contains(pos) ) {
					iter.remove();
					//System.err.println("removed event type" + e.msgtype);
				}
			}
		}
	}
	
	
	public SeqPattern findPatternByTimeGap() {
		
		Hashtable<String, ArrayList<SubSequenceRecord>> patterntable = new Hashtable<String, ArrayList<SubSequenceRecord>>();
		
		
		int totalsegments = 0; 
		int[] cntvec = new int[keymap.size()];
		
		id_iter:
		for(Entry<String, ArrayList<Event>> entry: eventqueues.entrySet()) {
			String id = entry.getKey();
			ArrayList<Event> events = entry.getValue();
			
			long startts = events.get(0).ts;
			long lastts = events.get(0).ts;
			
			int i=0;
			for( i=0; i<events.size(); i++) {
				Event e = events.get(i);
				//if ( e.ts-lastts >30000) {
				long dur = lastts-startts;
				if (dur==0 && i!=0) { // not a single event
					dur=2000;
				}
				if ( ( (i!=0&&(e.ts-lastts>dur*10)) || (dur > ConfigParam.target_detection_time_in_sec*1000)) ) {
					// if not the first event and either last duration is large enough,, or the gap is large
					// cut the sequence and return the vec.
					String cntstr = printAndClear(cntvec);
					
					SubSequenceRecord seq = new SubSequenceRecord( (lastts-startts)/1000, (e.ts-lastts)/1000 );
					ArrayList<SubSequenceRecord> gaparr = patterntable.get(cntstr);
					if(gaparr==null) {
						gaparr = new ArrayList<SubSequenceRecord>();
						patterntable.put(cntstr, gaparr);
					}
					gaparr.add( seq );
					totalsegments += 1;
					//System.err.println(cntstr + " " + e.ts + " " + startts);
					continue id_iter;
				} else {
					cntvec[keymap.get(e.msgtype)] += 1;
				}
				lastts = e.ts;
			}
			
			if( i==events.size() ) { // last event..
				//System.err.println("sequence ends.. ");
				String cntstr = printAndClear(cntvec);
				SubSequenceRecord seq = new SubSequenceRecord( (lastts-startts)/1000, (events.get(i-1).ts-lastts)/1000 );
				ArrayList<SubSequenceRecord> gaparr = patterntable.get(cntstr);
				if(gaparr==null) {
					gaparr = new ArrayList<SubSequenceRecord>();
					patterntable.put(cntstr, gaparr);
				}
				gaparr.add( seq );
				totalsegments += 1;
			}
			
		}
		
		// then we find dominate pattern
		//return findDominantByCount(patterntable, totalsegments);
		return findDominantByMedoid(patterntable, totalsegments);
	}
	
	public SeqPattern findDominantByMedoid(Hashtable<String, ArrayList<SubSequenceRecord>> patterntable, int totalsegments) {
		// first build a table with all patterns and their counts
		ArrayList<double[]> patterns = new ArrayList<double[]>(patterntable.size());
		int[] patterncnt = new int[patterntable.size()];
		ArrayList<ArrayList<SubSequenceRecord>> sampletable = new ArrayList<ArrayList<SubSequenceRecord>>();
		
		int cnt=0;
		for(Entry<String, ArrayList<SubSequenceRecord>> entry: patterntable.entrySet()) {
			if(entry.getValue().size()< 0.05*original_blocks) {
				continue;
			}
			String[] ps = entry.getKey().split(" ");
			double[] arr = new double[keymap.size()];
			for(int i=0; i<arr.length; i++) {
				arr[i] = Double.parseDouble(ps[i]);
			}
			patterns.add(arr);
			//MatrixUtils.prettyPrint(arr);
			patterncnt[cnt]=entry.getValue().size();
			sampletable.add(entry.getValue());
			cnt +=1;
		}
		
		double[][] distance = new double[patterns.size()][patterns.size()];
		// calculate distance between any two patterns
		for(int i=0; i<patterns.size(); i++) {
			for (int j=0; j<patterns.size(); j++) {
				distance[i][j] = MatrixUtils.chisqdistance(patterns.get(i), patterns.get(j));
			}
		}
		
		// try each pattern as medoid, find min cost one
		double min_cost = Double.MAX_VALUE;
		int pat_ind=-1;
		
		for(int i=0; i<patterns.size(); i++) {
			// assume pattern i is the dominant pattern
			double cost=0;
			for(int j=0; j<patterns.size(); j++) {
				if(i==j) 
					continue;
				cost += patterncnt[j]*distance[i][j];
			}
			if( cost <= min_cost *1.001 ) {
				if( cost >=min_cost*0.999 && cost<=min_cost*1.001 ) { // roughly equal,, check support
					if(patterncnt[i] > patterncnt[pat_ind]) {
						min_cost = cost;
						pat_ind =i;
					}
				} else {
					min_cost = cost;
					pat_ind = i;
				}
			}
			
			MatrixUtils.prettyPrint(patterns.get(i));
			System.err.println("cost=" + cost + " cnt=" +patterncnt[i]);
		}
		
		//MatrixUtils.prettyPrint(patterns.get(pat_ind));
		
		double[] dom_pat = patterns.get(pat_ind);
		
		// calculate time statistics about dominate pattern
		int[] cntvec = new int[keymap.size()];
		for(int i=0; i<dom_pat.length; i++) {
			cntvec[i] = (int) dom_pat[i];
		}
		ArrayList<SubSequenceRecord> samples = sampletable.get(pat_ind);

		
		SeqPattern pattern = new SeqPattern();
		pattern.countvec=cntvec;
		pattern.frequency_first_pass = samples.size();
		
		
		// calculate intermediate duration..
		int dur_sum=0;
		int dur_sum_sq=0;
		for(SubSequenceRecord s: samples) {
			dur_sum += s.duration;
			dur_sum_sq += s.duration*s.duration;
		}		
		pattern.duration_mean = ((double)dur_sum)/samples.size();
		pattern.duration_stddev = Math.sqrt( ((double)dur_sum_sq)/samples.size()-pattern.duration_mean*pattern.duration_mean);
		
		//System.exit(-1);
		return pattern;
	}
	
	public SeqPattern findDominantByCount(Hashtable<String, ArrayList<SubSequenceRecord>> patterntable, int totalsegments) {
		String mostFrequent=null;
		int mostFrequentCnt=0;
		
		//System.err.println("total patterns = " + totalsegments);
		for(Entry<String, ArrayList<SubSequenceRecord>> entry: patterntable.entrySet()) {
			
			double percent = entry.getValue().size() / (double) totalsegments;
			int cnt = entry.getValue().size();
			//System.err.println(" pattern " + entry.getKey() +" " + cnt);
			if (cnt > mostFrequentCnt) {
				mostFrequent = entry.getKey();
				mostFrequentCnt = cnt;
			}
		}
		
		System.err.println("most frequent pattern: " + mostFrequent +" : " + mostFrequentCnt);
		
		// calculate time statistics about dominate pattern
		int[] cntvec = new int[keymap.size()];
		String[] splits = mostFrequent.split(" ");
		for(int i=0; i<splits.length; i++) {
			cntvec[i] = Integer.parseInt(splits[i]);
		}
		ArrayList<SubSequenceRecord> samples = patterntable.get(mostFrequent);

		
		SeqPattern pattern = new SeqPattern();
		pattern.countvec=cntvec;
		pattern.frequency_first_pass = samples.size();
		
		
		// calculate intermediate duration..
		int dur_sum=0;
		int dur_sum_sq=0;
		for(SubSequenceRecord s: samples) {
			dur_sum += s.duration;
			dur_sum_sq += s.duration*s.duration;
		}		
		pattern.duration_mean = ((double)dur_sum)/samples.size();
		pattern.duration_stddev = Math.sqrt( ((double)dur_sum_sq)/samples.size()-pattern.duration_mean*pattern.duration_mean);
		
		return pattern;
	}
	
	
	public void calStatsAndRemovePatterns(SeqPattern pattern) {
		// make a copy of scratch pattern
		int[] tmp = new int[pattern.countvec.length];
		
		
		int num_events_in_pattern = 0;
		for(int i: pattern.countvec) {
			num_events_in_pattern += i;
		}
		
		ArrayList<Integer> durations = new ArrayList<Integer>();
		ArrayList<Integer> gaps = new ArrayList<Integer>();
		
		double max_duration = pattern.duration_mean + 10* pattern.duration_stddev;
		if (max_duration==0) {
			max_duration = 20;
		}
		
		
		
		for(Entry<String, ArrayList<Event>> entry: eventqueues.entrySet()) {
			
			boolean found=true;
			outer:
			while(found) {  // continue to discover the same pattern
			//System.err.println("trying to find pattern again..");
			found =false;
			System.arraycopy(pattern.countvec, 0, tmp, 0, tmp.length);
			ArrayList<Event> events = entry.getValue();
			
			int currenteventcnt = num_events_in_pattern;
			
			long pstartts = -1;
			long plastts =-1;
			
			Iterator<Event> iter = events.iterator();
			event_iter:
			while(iter.hasNext()) {
				Event e = iter.next();
				int pos = keymap.get(e.msgtype);
				//System.err.println(e);
				
				if (tmp[pos]>0) {
					//this.printSequence(events);
					// keep track duration info
					if (pstartts ==-1) { // see if it is the first event in sequence
						pstartts = e.ts;
						plastts = e.ts;
					} else {
						int cur_dur = (int) (e.ts-pstartts)/1000;
						if (cur_dur > max_duration ) {
							
							// forget about all the events seen before
							pstartts =e.ts;
							plastts = e.ts;
							System.arraycopy(pattern.countvec, 0, tmp, 0, tmp.length);
							currenteventcnt = num_events_in_pattern;
							
							tmp[pos] -= 1;	// current evetn in the new pattern
							currenteventcnt -= 1;
							
							//System.err.println("duration mean=" + pattern.duration_mean +" duration_stddev=" +pattern.duration_stddev);
							//System.err.println( "current_duration " + cur_dur +" exceeded threshold duration " + max_duration);
							//System.err.println( "reset matching state, startts=" +pstartts + "looking for " );
							//MatrixUtils.prettyPrint(tmp);
							
							// does not match current sequence continue with next one..
							continue event_iter;
						}
					}
					plastts = e.ts;
					tmp[pos] -= 1;
					currenteventcnt -= 1;
					
					//iter.remove();
					if (currenteventcnt ==0) {
						// pattern complete, first do some house keeping..
						int duration = (int) (e.ts-pstartts) /1000;
						
						int gap = 3600; // if last, default to 1 hour gap
						//get gap from next event
						if (iter.hasNext()) { // not the last event
							Event next_e = iter.next();
							gap = (int) (next_e.ts - e.ts) / 1000;
						}
						
						durations.add(duration);
						gaps.add(gap);
						found=true;
						
						// then remove pattern from sequence
						System.arraycopy(pattern.countvec, 0, tmp, 0, tmp.length);
						currenteventcnt = num_events_in_pattern;
						Iterator<Event> iremove = events.iterator();
						while(iremove.hasNext()) {
							e = iremove.next();
							pos = keymap.get(e.msgtype);
							if (tmp[pos]>0) {
								tmp[pos]-=1;
								currenteventcnt-=1;
								//System.err.println("removing event ..");
								iremove.remove();
							}
							if (currenteventcnt ==0) {
								continue outer;
							}
						}
						continue outer;
					}
				} else { // not in dominate pattern
					// do nothing..
				}
			} // each event
			
			} 
			
		}
		
		//System.err.println(Arrays.deepToString(durations.toArray()));
		//System.err.println(Arrays.deepToString(gaps.toArray()));
		
		
		// update time statistics
		
		double gap_sum=0;
		double gap_sum_sq=0;
		
		
		
		for(int s: gaps) {
			gap_sum += s;
			gap_sum_sq += s*s;
		}
		pattern.gap_mean = ((double)gap_sum)/gaps.size();
		pattern.gap_stddev = Math.sqrt( ((double)gap_sum_sq)/gaps.size()-pattern.gap_mean*pattern.gap_mean );
		
		PairOfDouble t = meanAndStddev(durations);
		pattern.duration_mean = t.d1;
		pattern.duration_stddev = t.d2;
		
		t = meanAndStddev(gaps);
		pattern.gap_mean = t.d1;
		pattern.gap_stddev = t.d2;
		
		//System.err.println("Duration: " + pattern.duration_mean+"/"+pattern.duration_stddev);
		//System.err.println("Gap: " + pattern.gap_mean+"/"+pattern.gap_stddev);
		
		t = medianAndMad(durations);
		pattern.duration_median = t.d1;
		pattern.duration_mad = t.d2;
		
		t = medianAndMad(gaps);
		pattern.gap_median = t.d1;
		pattern.gap_mad = t.d2;
		
		pattern.gaps = gaps;
		pattern.durations = durations;
		
		pattern.frequency_second_pass = gaps.size();
		
		// remove blocks with empty sequences
		for(Entry<String, ArrayList<Event>> entry: eventqueues.entrySet()) {
			ArrayList<Event> events = entry.getValue();
			if(events.size()==0) {
				eventqueues.remove(entry.getKey());
				continue;
			} else {
				//printSequence(entry.getValue());
			}
		}
	}
	
	private void printSequence(ArrayList<Event> el) {
		long last_ts =0;
		long start_ts = el.get(0).ts;
		
		for(Event e: el) {
			if (e.ts!= last_ts) {
				if (! (last_ts==0)) {
					System.err.print("->");
				}
				System.err.print( (e.ts-start_ts)/1000 +":" );
				last_ts = e.ts;
			}
			System.err.print(e.msgtype+"_");
		}
		System.err.println();
	}
	
	private PairOfDouble meanAndStddev(ArrayList<Integer> data) {
		double sum=0;
		double sum_sq=0;
		
		for(int s: data) {
			sum += s;
			sum_sq += s*s;
		}
		double mean = ((double)sum)/data.size();
		double stddev = Math.sqrt( ((double)sum_sq)/data.size()-mean*mean );
		return new PairOfDouble(mean,stddev);
	}
	
	private PairOfDouble medianAndMad(ArrayList<Integer> data) {
		
		if(data.size()==0) {
			return new PairOfDouble(0,0);
		}
		
		// make a copy so original data don't change
		ArrayList<Integer> data_cp = new ArrayList<Integer>();
		for(int i=0; i<data.size(); i++) {
			data_cp.add(data.get(i));
		}

		Collections.sort(data_cp);
		double median = data_cp.get(data_cp.size()/2) ;
		
		double[] absdevarr = new double[data_cp.size()];
		for (int k=0; k<data_cp.size(); k++) {
			absdevarr[k] = Math.abs(data_cp.get(k) - median);
		}
		Arrays.sort(absdevarr);
		double mad = absdevarr[absdevarr.length/2];
		
		return new PairOfDouble(median, mad);
	}
	
	
	private void enqueueEvent(Event event) {
		ArrayList<Event> elist = eventqueues.get(event.id);
		if(elist==null) {
			elist = new ArrayList<Event>();
			elist.add(event);
			eventqueues.put(event.id, elist);
		} else {
			elist.add(event);
		}
	}
	
	private String printAndClear(int [] arr) {
		StringBuffer sb = new StringBuffer();
		//sb.append(ts).append(" ");
		for(int i=0; i<arr.length; i++) {
			sb.append(arr[i]).append(" ");
			arr[i] =0;
		}
		return sb.toString();
	}
	
	private class SubSequenceRecord{
		double duration;
		double gap;

		public SubSequenceRecord(double duration, double gap) {
			super();
			this.duration = duration;
			this.gap = gap;
		}
		
		public String toString() {
			return this.duration +":" + this.gap;
		}
		
	}
	
	private void writeGapIntervals(File dir, LinkedHashMap<SeqPattern, SeqPattern> all_patterns) throws IOException{
		System.err.println("dumping gap intervals to text file..");
		if(!dir.exists()) {
			dir.mkdirs();
		}
		int i=0;
		for(SeqPattern p : all_patterns.keySet()) {
			PrintWriter out = new PrintWriter(new FileWriter( new File(dir, "p"+i) ));
			out.println("%" + SeqPattern.cntVecToSeq(p.countvec));
			for(int gap: p.gaps) {
				out.println(gap);
			}
			out.close();
			i+=1;
		}
	}
	
	private void writeDurIntervals(File dir, LinkedHashMap<SeqPattern, SeqPattern> all_patterns) throws IOException{
		System.err.println("dumping duration intervals to text file..");
		if(!dir.exists()) {
			dir.mkdirs();
		}
		int i=0;
		for(SeqPattern p : all_patterns.keySet()) {
			PrintWriter out = new PrintWriter(new FileWriter( new File(dir, "p"+i) ));
			out.println("%" + SeqPattern.cntVecToSeq(p.countvec));
			for(int dur: p.durations) {
				out.println(dur);
			}
			out.close();
			i+=1;
		}
	}
	
	
	private class PairOfDouble{
		double d1;
		double d2;
		public PairOfDouble(double d1, double d2) {
			super();
			this.d1 = d1;
			this.d2 = d2;
		}
		
	}
	
}
