package online;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Map.Entry;

import pca.MatrixUtils;

public class HighProbSequences {
	
	static final int SIZE =30;
//	private static File seqfile = new File("data\\online\\hdfs_sm1");
	private static File seqfile = new File("data/online/hdfs_sm1");
	private static Hashtable<Integer, Integer> keymap = new Hashtable<Integer, Integer>();
	private static Hashtable<String, Integer> nodemap = new Hashtable<String, Integer>();
	
	public static void main(String[] args) throws Exception{
		String out_file =  seqfile.getParent() + "/highprobsequence.txt";
//		reformattedSequences(seqfile, out_file);
//		checkStartingEvents(seqfile, out_file);
		checkFirstPartEvents(seqfile, out_file);
	}
	
	private static void reformattedSequences(File seqfile, String filename) throws Exception	{
		BufferedReader reader = new BufferedReader(new FileReader(seqfile));
		String line = reader.readLine();
		ArrayList 	all_sequences = new ArrayList();
		ArrayList   sequence=null, nodeids=null, msgtypes=null;
		Events  events;
		int     granularity = 1; 
		int 	i, id, nidcnt =0;
		long 	ts, start_ts, prev2_ts, prev_ts, curr_ts;
		
		PrintWriter	output = new PrintWriter(new BufferedWriter(new FileWriter(filename)));

		while(line!=null)
		{
			sequence = new ArrayList();
			all_sequences.add(sequence);
			
			String[] parts= line.split("\\s");
			String[] event = parts[1].split(":");
			String node = event[2];
			Integer nid = nodemap.get(node);
			if(nid==null) {
				//Map nodeid (long string) to integer
				nidcnt +=1;
				nid = nidcnt;
				nodemap.put(node, nid);
			}
			nodeids = new ArrayList();
			nodeids.add(Integer.toString(nid));
			msgtypes = new ArrayList();
			msgtypes.add(event[0]);
			
			prev_ts = Long.parseLong(event[1])/1000;
			prev2_ts = prev_ts;
			for (i=2; i< parts.length; i++) { // skip the first one (block id)..
				event = parts[i].split(":");
				id = Integer.parseInt( event[0] );
				node = event[2];
				nid = nodemap.get(node);
				if(nid==null) {
					nidcnt +=1;
					nid = nidcnt;
					nodemap.put(node, nid);
				}
				curr_ts = Long.parseLong(event[1])/1000;
				if ( (curr_ts-prev_ts) <= granularity)
				{
					//Events happened simultaneously
					nodeids.add(Integer.toString(nid));
					msgtypes.add(event[0]);					
				}
				else
				{
					events = new Events(prev_ts-prev2_ts, nodeids, msgtypes);
					sequence.add(events);
					prev2_ts = prev_ts;
					prev_ts = curr_ts;

					nodeids = new ArrayList();
					msgtypes = new ArrayList();
					nodeids.add(Integer.toString(nid));
					msgtypes.add(event[0]);					
				}
			}
			//Need extra code to put in the last chunk of events
			events = new Events(prev_ts-prev2_ts, nodeids, msgtypes);
			sequence.add(events);				
			String sequence_strs = "";
			String event_strs = "";
			for (i = 0; i < sequence.size(); i++)
			{
				event_strs = sequence.get(i).toString();
				sequence_strs = sequence_strs + event_strs + "->";
			}
			//System.err.println(sequence_strs);
			output.println(sequence_strs);


			line = reader.readLine();
		}
		reader.close();
		output.close();
	}

	private static void checkStartingEvents(File seqfile, String filename) throws Exception	{
		// Here is the pattern I observed in the HDFS message:
		// Within the first 30 time units, majority of sequences start from 3 "179" and 1 "340".
		// Those without this pattern are with pattern 1 or 2 "179" and 1 "340" followed by "184" or nothing
		int     granularity = 30; 

		BufferedReader reader = new BufferedReader(new FileReader(seqfile));
		String line = reader.readLine();
		Hashtable<String, Integer> msgtypes = new Hashtable<String, Integer>();
		Integer c_179, c_340;
		int 	i, id, nidcnt =0;
		long 	ts, start_ts, prev_ts, curr_ts;
		
		PrintWriter	output = new PrintWriter(new BufferedWriter(new FileWriter(filename)));

		while(line!=null)
		{		
			msgtypes.clear();
			String[] parts= line.split("\\s");
			String[] event = parts[1].split(":");
			String node = event[2];
			Integer nid = nodemap.get(node);
			if(nid==null) {
				//Map nodeid (long string) to integer
				nidcnt +=1;
				nid = nidcnt;
				nodemap.put(node, nid);
			}
			Integer count = msgtypes.get(event[0]);
			if (msgtypes.containsKey(event[0]))
			{
				Integer I = (Integer) msgtypes.get(event[0]);
				msgtypes.put(event[0], new Integer(I.intValue()+1));
			}
			else
			{
				msgtypes.put(event[0], new Integer(1));
			}			
			prev_ts = Long.parseLong(event[1])/1000;
			for (i=2; i< parts.length; i++) { // skip the first one (block id)..
				event = parts[i].split(":");
				id = Integer.parseInt( event[0] );
				node = event[2];
				nid = nodemap.get(node);
				if(nid==null) {
					nidcnt +=1;
					nid = nidcnt;
					nodemap.put(node, nid);
				}
				curr_ts = Long.parseLong(event[1])/1000;
				if ( (curr_ts-prev_ts) <= granularity)
				{
					//Events fall within desired time intervals
					if (msgtypes.containsKey(event[0]))
					{
						Integer I = (Integer) msgtypes.get(event[0]);
						msgtypes.put(event[0], new Integer(I.intValue()+1));
					}
					else
					{
						msgtypes.put(event[0], new Integer(1));
					}
					c_179 = msgtypes.get("179");
					c_340 = msgtypes.get("340");
					if (c_179 != null && c_340 != null)
					{
						if (c_179.intValue() >= 3 && c_340.intValue() >= 1)
							break;						
					}
				}
				else
				{
					//Events fall outside desired time intervals
					break;
				}
			}

			c_179 = msgtypes.get("179");
			c_340 = msgtypes.get("340");
			if (c_179.intValue() < 3 || c_340.intValue() < 1)
				System.err.println(line);


			line = reader.readLine();
		}
		reader.close();
		output.close();
	}

	private static void checkFirstPartEvents(File seqfile, String filename) throws Exception	{
		// Here is the pattern I observed in the HDFS message:
		// Within the first 30 time units, majority of sequences start from 3 "179" and 1 "340".
		// Those without this pattern are with pattern 1 or 2 "179" and 1 "340" followed by "184" or nothing
		int     granularity = 300; 

		BufferedReader reader = new BufferedReader(new FileReader(seqfile));
		String line = reader.readLine();
		Hashtable<String, Integer> msgtypes = new Hashtable<String, Integer>();
		Integer c_179, c_340, c_198, c_200, c_387;
		int 	i, id, nidcnt =0;
		long 	ts, start_ts, prev_ts, curr_ts;
		
		PrintWriter	output = new PrintWriter(new BufferedWriter(new FileWriter(filename)));

		while(line!=null)
		{		
			msgtypes.clear();
			String[] parts= line.split("\\s");
			String[] event = parts[1].split(":");
			String node = event[2];
			Integer nid = nodemap.get(node);
			if(nid==null) {
				//Map nodeid (long string) to integer
				nidcnt +=1;
				nid = nidcnt;
				nodemap.put(node, nid);
			}
			Integer count = msgtypes.get(event[0]);
			if (msgtypes.containsKey(event[0]))
			{
				Integer I = (Integer) msgtypes.get(event[0]);
				msgtypes.put(event[0], new Integer(I.intValue()+1));
			}
			else
			{
				msgtypes.put(event[0], new Integer(1));
			}			
			prev_ts = Long.parseLong(event[1])/1000;
			for (i=2; i< parts.length; i++) { // skip the first one (block id)..
				event = parts[i].split(":");
				id = Integer.parseInt( event[0] );
				node = event[2];
				nid = nodemap.get(node);
				if(nid==null) {
					nidcnt +=1;
					nid = nidcnt;
					nodemap.put(node, nid);
				}
				curr_ts = Long.parseLong(event[1])/1000;
				if ( (curr_ts-prev_ts) <= granularity)
				{
					//Events fall within desired time intervals
					if (msgtypes.containsKey(event[0]))
					{
						Integer I = (Integer) msgtypes.get(event[0]);
						msgtypes.put(event[0], new Integer(I.intValue()+1));
					}
					else
					{
						msgtypes.put(event[0], new Integer(1));
					}
				}
				else
				{
					//Events fall outside desired time intervals
					break;
				}
			}

			c_179 = msgtypes.get("179");
			c_340 = msgtypes.get("340");
			c_198 = msgtypes.get("198");
			c_200 = msgtypes.get("200");
			c_387 = msgtypes.get("387");
			if (c_179 != null && c_340 != null && c_198 != null && c_200 != null && c_387 != null)
			{
				if (c_179.intValue() < 3 || c_198.intValue() < 3 || c_200.intValue() < 3 || c_387.intValue() < 3 || c_340.intValue() < 1)
					System.err.println(line);		
			}
			else
				System.err.println(line);		

			line = reader.readLine();
		}
		reader.close();
		output.close();
	}
}
