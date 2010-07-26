package online;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pca.DetectionResult;
import pca.MatrixUtils;
import pca.PCADetection;

public class PCACountDetection {
	
	static final File DATASET_DIR = ConfigParam.DATASET_DIR;
	
	
	static Hashtable<String, Integer> truthtable = new Hashtable<String, Integer>();
	static Hashtable<String, Integer> reasontable = new Hashtable<String, Integer>();
	static Hashtable<String, Integer> blockpos = new Hashtable<String, Integer>();
	
	static Hashtable<String, String> abnormal_segments = new Hashtable<String, String>();
	
	public static void main(String[] args) throws Exception{
		
		loadTruth();
		
		//PCADetection detect = new PCADetection( new File(DATASET_DIR, "pca_matrices_pattern").getAbsolutePath() );
		//BufferedReader in = new BufferedReader( new FileReader( new File(DATASET_DIR, "patterns_pca_vector") ));
		
		//PCADetection detect = new PCADetection( new File(DATASET_DIR, "pca_matrices").getAbsolutePath() );
		//BufferedReader in = new BufferedReader( new FileReader( new File(DATASET_DIR, "rawTFVector.txt") ));
		
		PCADetection detect = new PCADetection( new File(DATASET_DIR, "pca_matrices_nonpattern").getAbsolutePath() );
		BufferedReader in = new BufferedReader( new FileReader( new File(DATASET_DIR, "nonpattern_matrix.txt") ));
		
		String line = in.readLine().trim();
		
		String nodeid;
		
		Hashtable<String, Integer> abnormal_blocks=new Hashtable<String, Integer>();
		
		int alarm_cnt =0;
		while (line!=null) {
			line = line.trim();
			if (line.startsWith("%") || line.length()==0) {
				line = in.readLine();
				continue; // skip comments
			}
			//System.err.println(line);
			DetectionResult ret = detect.isAbnormal(line);
			if(ret.isAbnormal()) {
				alarm_cnt +=1;
				//System.err.println(ret);
				Integer cnt = abnormal_blocks.get(ret.getIdentifier());
				if(cnt==null) {
					abnormal_blocks.put(ret.getIdentifier(),1);
				} else {
					abnormal_blocks.put(ret.getIdentifier(), cnt+1);
				}
				
				String id = ret.getIdentifier();
				Integer lb = truthtable.get(id);
				Integer reason = reasontable.get(id);
				if(lb==0) { // FP
					if (reason<0) {
						String seq = KeymapUtils.strToEventStr(line);
						System.err.println("FP(unknown type): " + id +" "  + seq);
						String allseq = abnormal_segments.get(id);
						if (allseq!=null) {
							allseq += " /// " + seq;
						} else {
							abnormal_segments.put(id, seq);
						}
					} else {
						//System.err.println("FP("+ reason + "):" + id +" "  + KeymapUtils.strToEventStr(line));
					}
				}
			} else {
				// normal, do nothing
			}
			line = in.readLine();
		}
		
		System.err.println( "PCA alarms=" + alarm_cnt +" blocks="+ abnormal_blocks.size() );
		
		BufferedReader r = new BufferedReader(
				new FileReader( new File(DATASET_DIR, "rareEvents.txt")) );
		String blockid = r.readLine();
		
		int rare_block_cnt =0;
		while(blockid!=null) {
			String[] s = blockid.split(" ");
			blockid = s[0];
			Integer lb = truthtable.get(blockid);
//			if(lb!=null && lb==0) {
//				System.err.println("FP(from RARE): " + blockid);
//			}
			Integer cnt = abnormal_blocks.get(blockid);
			if(cnt==null) {
				rare_block_cnt += 1;
				abnormal_blocks.put(blockid,1);
			} else {
				abnormal_blocks.put(blockid, cnt+1);
			}
			blockid = r.readLine();
		}
		
		System.err.println("blocks contain rare events = " + rare_block_cnt);
		
		
		if (truthtable.size()==0) {
			return;
		}
		
		Hashtable<Integer,Integer> reasoncnt = new Hashtable<Integer,Integer>();
		Hashtable<Integer,Integer> fpreason_cnt = new Hashtable<Integer,Integer>();
		
		int tp_cnt =0;
		int fp_cnt =0;
		for(Entry<String, Integer> entry: abnormal_blocks.entrySet() ) {
			String id = entry.getKey();
			Integer lb = truthtable.get(id);
			Integer reason;
			String result =null;
			if (lb==null) {
				throw new RuntimeException("unlabeled block " + id);
			} else {
				if(lb!=0) {
					//result ="TP";
					truthtable.put(id, -1);
					reason = reasontable.get(id);
					Integer cnt =reasoncnt.get(reason);
					if (cnt==null) {
						reasoncnt.put(reason, 1);
					} else {
						reasoncnt.put(reason, cnt+1);
					}
					tp_cnt +=1;
				} else {
					result ="FP";
					fp_cnt +=1;
					reason = reasontable.get(id);
					Integer cnt = fpreason_cnt.get(reason);
					if (cnt==null) {
						fpreason_cnt.put(reason, 1);
					} else {
						fpreason_cnt.put(reason, cnt+1);
					}
				}
			}
			if(reason==null)
				reason = -1;
			if(result!=null)
				System.err.println(id+" : " + entry.getValue() +" " + result + " " + blockpos.get(id) +" " +reason);
		}
		
		int fn_cnt =0;
		for(Entry<String, Integer> entry: truthtable.entrySet()) {
			//System.err.println(entry.getValue());
			if(entry.getValue() >0) {
				String id = entry.getKey();
				System.err.println("FN: " + id 
						+" block_seq=" + blockpos.get(id) 
						+ " manual_label=" + reasontable.get(id) );
				fn_cnt +=1;
			}
		}
		
		System.out.println("tp=" +tp_cnt +" fp="+fp_cnt +" fn=" + fn_cnt);
		System.err.println("tp=" +tp_cnt +" fp="+fp_cnt +" fn=" + fn_cnt);
		
		System.err.println("categories detected: "); 
		for(Entry<Integer, Integer> e : reasoncnt.entrySet()) {
			System.err.println(e.getKey()+" " + e.getValue());
		}
		System.err.println("FP categories: "); 
		for(Entry<Integer, Integer> e : fpreason_cnt.entrySet()) {
			System.err.println(e.getKey()+" " + e.getValue());
		}
		
		System.err.println("======== unclear/unlabeled FPs ===========");
		int cnt =0;
		for(Entry<String,String> e: abnormal_segments.entrySet()) {
			cnt +=1;
			System.err.println(cnt +") " +e.getKey()+ " " + e.getValue());
		}
	}
	
	
	private static ArrayList<String> loadTruthOldFormat(BufferedReader in) throws IOException {
		
		ArrayList<String> blocks = new ArrayList<String>();
		String line = in.readLine();
		
		Pattern p = Pattern.compile( "%\\s+=+([^=]*)=+.*" );
		int cnt=0;
		//outer:
		while(line !=null) {
			line = line.trim();
			if (!line.startsWith("%") || line.length()==0) {
				line = in.readLine();
				continue; // actual data lines
			}
			Matcher m = p.matcher(line);
			if (m.matches()) {
				blocks.add( "%" + m.group(1) );
				blockpos.put("%" + m.group(1), cnt);
				cnt +=1;
			}
			line = in.readLine();
		}
		in.close();
		
//		for(Entry<String, Integer> e: blockpos.entrySet()) {
//			System.err.println(e.getKey() +" " + e.getValue());
//		}
		return blocks;
		
	}
	
	private static ArrayList<String> loadTruthNewFormat(BufferedReader in) throws IOException {
		
		ArrayList<String> blocks = new ArrayList<String>();
		String line = in.readLine();
		int cnt =0;
		//outer:
		while(line !=null) {
			line = line.trim();
			if (line.startsWith("%") || line.length()==0) {
				line = in.readLine();
				continue; // actual data lines
			}
			String[] parts = line.split(" ");
			for(String s: parts) {
				if (s.startsWith("%%")) {
					//System.err.println(s.substring(1));
					String blk_id = s.substring(1);
					if( !blk_id.startsWith("%") ) 
						throw new RuntimeException("wrong block label??");
					blocks.add(s.substring(1));
					blockpos.put(s.substring(1), cnt );
					cnt +=1;
					//continue outer;
				}
			}
			line = in.readLine();
		}
		in.close();
		
		return blocks;
		
	}
	
	public static void loadTruth() throws Exception{
		File truthdir = new File(DATASET_DIR,"truth");
		ArrayList<String> blocks;
		BufferedReader in = new BufferedReader( new FileReader( new File(truthdir, "rawTFVector.txt") ));
		in.mark(1000);
		String s = in.readLine();
		in.reset();
		if(s.startsWith("%===")) {
			blocks = loadTruthOldFormat(in);
		} else {
			blocks = loadTruthNewFormat(in);
		}
		
		ArrayList<Integer> lbs = new ArrayList<Integer>();
		ArrayList<Integer> reasons = new ArrayList<Integer>();
		
		in = new BufferedReader( new FileReader( new File(truthdir, "mlabel.txt") ));
		String line = in.readLine();
		while(line !=null) {
			String[] parts = line.split(" ");
			Integer lb = Integer.parseInt( parts[0] );
			Integer reason= Integer.parseInt( parts[1] );
			//System.err.println(lb);
			lbs.add(lb);
			reasons.add(reason);
			line = in.readLine();
		}
		
		
		int cnt=0;
		int abnormal_cnt =0;
		if (lbs.size()!= blocks.size()) {
			throw new RuntimeException("manual label size does not match file!");
		} else {
			int dupcnt = 0;
			for(int i=0; i<lbs.size(); i++) {
				if(truthtable.containsKey(blocks.get(i))) {
					//System.err.println("duplicate block " + blocks.get(i));
					dupcnt +=1; 
				}
				truthtable.put(blocks.get(i), lbs.get(i));
				reasontable.put(blocks.get(i), reasons.get(i));
				if (lbs.get(i)!=0) {
					abnormal_cnt +=1;
				}
			}
			
			int pcnt = 0;
			for(Entry<String, Integer> entry: truthtable.entrySet()) {
				//System.err.println(entry.getValue());
				if(entry.getValue() >0) {
					pcnt +=1;
				}
			}
			
			System.err.println("loaded truth table, size="+truthtable.size()+" abnormal="+ abnormal_cnt +" " +pcnt +" " + dupcnt);
		}
		
	}

}
