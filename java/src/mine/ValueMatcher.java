package mine;

import index.IndexConstants;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.math.MathException;
import org.apache.commons.math.stat.Frequency;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math.stat.inference.TestUtils;
import org.pdfbox.pdfviewer.MapEntry;

import utils.DbUtils;
import conf.ConfUtil;
import data.RegExprRecord;

public class ValueMatcher {
	
	
	static Hashtable<String, List<EntryRecord>> matchingvalues = new Hashtable<String, List<EntryRecord>>();
	
	static Hashtable<String, Integer> varGroups = new Hashtable<String, Integer>();
	
	static ArrayList<List<String>> groupList = new ArrayList<List<String>>();
	
	static ArrayList< List<List<EntryRecord>> > sorted = new ArrayList<List<List<EntryRecord>>>();
	
	static Hashtable<String, List<String> > reportingLogs = new Hashtable<String, List<String>>();
	
	static DateFormat dbtsformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	
	static boolean training_done = false;
	
	static String trainingDBName = "normal_10clients";
	//static String trainingDBName = "nn_10_251_210_161";
	
	//static long windowSize = 300000L;
	static long windowSize = 10000L;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		ignoreVar.add("serialVersionUID");
		ignoreVar.add("classId");
		
		Connection conn = DbUtils.getConn();
		
		// calculate min. TS
		long mints = Long.MAX_VALUE;
		long maxts = Long.MAX_VALUE;
		for (String dbname: IndexConstants.LOG_DB_NAMES) {
			if (!training_done) {
				if (trainingDBName!=null && !dbname.equals(trainingDBName)) 
					continue;
			} else {
				if (trainingDBName!=null && dbname.equals(trainingDBName)) 
					continue;
			}
			PreparedStatement ps = conn.prepareStatement("select min(ts) as ts from " +dbname);
			ResultSet rs = ps.executeQuery();
			rs.next();
			Timestamp ts= rs.getTimestamp("ts");
			long tslong = ts.getTime();
			if (tslong<mints) {
				mints =tslong;
			}
			rs.close();
		}
		
		System.err.println("min ts= " + mints +" " + dbtsformat.format(mints));
		
		long tsstart;
		if (training_done) { 
			tsstart = mints;
			windowSize = 300000L;
		} else {
			tsstart = mints;
			tsstart = mints+300000;
			//maxts = tsstart + 250000;
		}
		boolean hasMoreWindows = true;
		while (hasMoreWindows) {
			
			matchingvalues = new Hashtable<String, List<EntryRecord>>();
			varGroups = new Hashtable<String, Integer>();			
			groupList = new ArrayList<List<String>>();			
			sorted = new ArrayList<List<List<EntryRecord>>>();
			
			System.err.println("=======FROM " + dbtsformat.format(tsstart) 
					+ " TO " + dbtsformat.format(tsstart+windowSize) +"============");
			hasMoreWindows = doOneWindow(args, tsstart, tsstart+windowSize);
			tsstart += windowSize;
			if (tsstart > maxts) {
				break;
			}
		}
		
		boolean again = true;
		if (training_done) {
			again=false;
		} else {
			dumpChiSquareExpect();
			training_done = true;
		}
		if (again) {
			main(args);
		} else {
			dumpChiSqResult();
		}
	}
	
	
	public static boolean doOneWindow(String[] args, long startts, long endts) throws Exception{
		boolean hasmore =false;
		for (String dbname: IndexConstants.LOG_DB_NAMES) {
			if (!training_done) {
				if (trainingDBName!=null && !dbname.equals(trainingDBName)) 
					continue;
			} else {
				if (trainingDBName!=null && dbname.equals(trainingDBName)) 
					continue;
			}
			hasmore= doOneDbOneWindow(args, dbname, startts, endts);
		}
		varGroup();
		//printResult();
		//printVarGroup();
		sortByVarName();
		if (!training_done) {
			chiSqureTrain();
		}else {
			chiSquareTest( (startts+endts)/2 );
			//valuesStat();
			printSortedResult();
		}
		exportClusteringData();
		
		return hasmore;
	}

	public static boolean doOneDbOneWindow(String[] args, String logdbname, long startts, long endts) throws Exception{

		Connection conn = DbUtils.getConn();
		int start = ConfUtil.getConfig().getInt("dataSeg.start",-1);
		int end = ConfUtil.getConfig().getInt("dataSeg.end",-1);
		
		boolean sortBythread = false;
		String orderby = "";
		
		int rowcnt = 0;
		int outcnt =0;
		String mode = training_done?"testing":"training"; 
		System.err.println("Processing DB "+ logdbname +" for " + mode);
		File outfile = null;
		File textoutfile = null;
		if (!sortBythread) {
			orderby = " order by seq";
			outfile = new File(IndexConstants.TMP_DIR,
					logdbname + "_data");
			textoutfile = new File(IndexConstants.TMP_DIR,
					logdbname + "_text_data");
		} else {
			orderby = " order by threadid, seq";
			outfile = new File(IndexConstants.TMP_DIR,
					logdbname + "_data_thread");
			textoutfile = new File(IndexConstants.TMP_DIR,
					logdbname + "_text_data_thread");
		}
		//dataout = new PrintStream(outfile);
		//textout = new PrintStream(textoutfile);
		
		PreparedStatement ps = null;
		if (start >= 0 && end >= 0) {
			ps = conn
					.prepareStatement("select threadid,ts,seq,logid,lbs,dts,textentry from "
							+ logdbname
							+ " where seq>? and seq<? and ts between ? and ? " + orderby);
			ps.setInt(1, start);
			ps.setInt(2, end);
			ps.setTimestamp(3, new Timestamp(startts));
			ps.setTimestamp(4, new Timestamp(endts));
			System.err.println("Start=" + start + "  end=" + end);
		} else {
			ps = conn
					.prepareStatement("select threadid,ts,seq,logid,lbs,dts,textentry from "
							+ logdbname 
							+ " where ts between ? and ? " + orderby);
			System.err.println("Use full log.");
			ps.setTimestamp(1, new Timestamp(startts));
			ps.setTimestamp(2, new Timestamp(endts));
		}
		ResultSet rs = ps.executeQuery();
		
		while (rs.next()) {
			rowcnt +=1;
			String lbs = rs.getString("lbs");
			//System.err.println(lbs);
			if (lbs.trim().length()>0) {
				String[] values = lbs.split("\n");
				for (String s : values) {
					String[] kv = s.split(" = ");
					if (kv.length<=1) 
						continue;
					if (ignoreVar.contains(kv[0].trim())){
						//System.err.println("skipping " + kv[0]);
						continue;
					}
					
					//System.err.println(kv[1]);
					long ts =rs.getTimestamp("ts").getTime();
					String logid = rs.getString("logid");
					String msg = rs.getString("textentry");
					
					List<String> replog = reportingLogs.get(kv[1]);
					if (replog == null) {
						replog = new ArrayList<String>();
						reportingLogs.put(kv[1], replog);
					} else {
						;
					}
					if( !replog.contains(logid) ) {
						replog.add(logid);
					}
					
					EntryRecord newentry = new EntryRecord(ts,logdbname,kv[0],kv[1],logid,msg);
					addToMatchingValues(kv[1], newentry);
				}
				//System.err.println(rs.getString("lbs"));
				outcnt +=1;
			}
			long ts =rs.getTimestamp("ts").getTime();
			String logid = rs.getString("logid");
			EntryRecord logidentry = new EntryRecord(ts, logdbname, "LOGID",logid,logid,"at "+logid);
			addToMatchingValues(rs.getString("logid"),logidentry );
		}
		
		rs.close();
		System.err.println("processed: " + rowcnt);
		System.err.println("output: " + outcnt);
		return rowcnt>0;
	}
	
	private static void addToMatchingValues(String value, EntryRecord newentry) {
		List<EntryRecord> m = matchingvalues.get(value);
		if ( m!=null ) {
			int i =0;
			// insert according to time
			for (i=0; i<m.size(); i++) {
				if (newentry.compareTo(m.get(i))< 0) {
					break;
				}
			}
			m.add(i, newentry);
		} else {
			m = new ArrayList<EntryRecord>();
			m.add(newentry);
			matchingvalues.put(value, m);
		}
	}
	
	static void printResult() {
		for(Entry<String, List<EntryRecord>> e: matchingvalues.entrySet()) {
			System.err.println("==="+ e.getKey() +"===");
			for (EntryRecord er: e.getValue()) {
				System.err.println(er);
			};
			System.err.println();
		}
	}

	
	static void printVarGroup() {
		for(Entry<String, Integer> e: varGroups.entrySet()) {
			System.err.println( e.getKey() +" : "+ Arrays.deepToString( groupList.get(e.getValue()).toArray() ));
		}
	}
	
	static void sortByVarName() {
		for (int i=0; i< groupList.size(); i++) {
			sorted.add(i, new ArrayList<List<EntryRecord>>());
		}
		for(Entry<String, List<EntryRecord>> e: matchingvalues.entrySet()) {
			EntryRecord er = e.getValue().get(0);
			Integer groupind = varGroups.get( er.varname);
			List<List<EntryRecord>> curlist =  sorted.get(groupind);
			int i=0;
			EntryRecord newentry = e.getValue().get(0);
			for(i=0;i<curlist.size();i++) {
				if(newentry.compareTo( curlist.get(i).get(0))<0){
					break;
				}
			}
			curlist.add(i, e.getValue());
		}
	}
	
	private static Hashtable<String, LinkedHashMap<String,Long>> chiSquareExpected =
		new Hashtable<String, LinkedHashMap<String,Long>>();
	
	static void dumpChiSquareExpect() {
		for(String varname: chiSquareExpected.keySet()) {
			LinkedHashMap<String, Long> t = chiSquareExpected.get(varname);
			for( Entry<String, Long> e:  t.entrySet()) {
				System.err.println(varname+ " " + e.getKey() +" " +e.getValue());
			}
		} 
	}
	
	static void chiSqureTrain() {
		for (int i=0; i<sorted.size(); i++){
			List<List<EntryRecord>> list = sorted.get(i);
			String varkey = Arrays.deepToString(groupList.get(i).toArray());
			System.err.println( "====="+ varkey +"=====" );
			// calculate total number of log lines mentioned this set of varialble
			int totallines =0;
			for (List<EntryRecord> t: list) {
				totallines += t.size();
			}
			
			if ( totallines <50 ) {
				System.err.println("too few lines");
				continue;
			}
			
			if ( totallines/ list.size() <10 ) {
				System.err.println("too few samples per bin");
				continue;
			}
			
			if (list.size()> 80) {
				System.err.println("too many distinct values");
				continue;
			}
			
			System.err.println("included!!");
			LinkedHashMap<String,Long> tb;
			if (!chiSquareExpected.containsKey(varkey)) {
				tb = new LinkedHashMap<String, Long>();
				chiSquareExpected.put(varkey, tb);
			} else {
				tb = chiSquareExpected.get(varkey);
			}
			for(List<EntryRecord> t: list) {
				String varvalue = t.get(0).varvalue;
				System.err.println(" ->" + varvalue +" " + t.size());
				Long origvalue = tb.get(varvalue);
				long orig;
				if (origvalue==null) {
					orig=0;
				} else {
					orig=origvalue.longValue();
				}
				tb.put(varvalue, t.size()+orig);
			}
		}
	}
	
	private static LinkedHashMap<Long, ArrayList<Double>> chisquareResult 
		= new LinkedHashMap<Long, ArrayList<Double>>();
	private static ArrayList<String> chisquareResultKeyInd = new ArrayList<String>();
	
	static void chiSquareTest(long ts) throws MathException{
		for (int i=0; i<sorted.size(); i++){
			List<List<EntryRecord>> list = sorted.get(i);
			String varkey = Arrays.deepToString(groupList.get(i).toArray());
			System.err.println( "====="+ varkey +"=====" );
			
			LinkedHashMap<String,Long> tb = chiSquareExpected.get(varkey);
			if (tb==null) {
				//System.err.println("N/A");
				continue;
			}
			
			int unseen_cnt = 0;
			int seen_cnt =0;
			for (List<EntryRecord> t: list) {
				String varvalue = t.get(0).varvalue;
				Long origvalue = tb.get(varvalue);
				if (origvalue==null) {
					System.err.println("unseen value " + varvalue);
					unseen_cnt +=1;
					tb.put(varvalue, 1L);
				} else {
					seen_cnt +=1;
				}
			}
			if (seen_cnt==0) {
				continue;
			}
			if ( ((double)unseen_cnt)/seen_cnt >1 ) {
				System.err.println("too many unseen values..  skipped");
				chiSquareExpected.remove(varkey);
				continue;
			}
			double[] expectedArray = new double[tb.size()];
			long[] observedArray = new long[tb.size()];
			int ind=0;
			for(Entry<String, Long> e: tb.entrySet()) {
				expectedArray[ind] = e.getValue();
				for(int j=0; j<list.size(); j++) {
					String varvalue = list.get(j).get(0).varvalue;
					if (varvalue.equals(e.getKey())) {
						observedArray[ind] = list.get(j).size();
						System.err.println(ind +":" + varvalue);
						break;
					}
				}
				ind +=1;
			}
			
			if (tb.size()<2) {
				System.err.println("skipped because too few distinct value");
				continue;
			}
			if (tb.size()>85) {
				System.err.println("too many distinct values, skip..");
				continue;
			}
			ArrayList<Double> result = chisquareResult.get(ts);
			if (result ==null) {
				result = new ArrayList<Double>(chisquareResultKeyInd.size());
				for(int j=0; j< chisquareResultKeyInd.size(); j++){
					result.add( new Double(0.0) );
				}
				System.err.println("created result array of size " + result.size());
				chisquareResult.put(ts, result);
			} else {
				;
			}
			System.err.print("observed ");
			for(int j=0; j<observedArray.length; j++) {
				System.err.print( observedArray[j] +" ");
			}
			System.err.println();
			System.err.print("expected ");
			for(int j=0; j<expectedArray.length; j++) {
				System.err.print(expectedArray[j] +" ");
			}
			System.err.println();
			//System.err.println("observed " + Arrays.deepToString(observedArray) );
			//System.err.println("expected " + Arrays.deepToString(expectedArray) );
			double chisq = TestUtils.chiSquare(expectedArray,observedArray);
			System.err.println("chi-square " + chisq);
			System.err.println("chi-square-p " + TestUtils.chiSquareTest(expectedArray,observedArray));
			int index = chisquareResultKeyInd.indexOf(varkey);
			if (index <0) {
				index = chisquareResultKeyInd.size();
				chisquareResultKeyInd.add(varkey);
				result.add( new Double(-1.0) );  // expand result array
			}
			System.err.println( Arrays.deepToString( chisquareResultKeyInd.toArray() ) );
			System.err.println( Arrays.deepToString( result.toArray() ) );
			result.set(index, chisq);
		}
	}
	
	private static void dumpChiSqResult() {
		for(Entry<Long, ArrayList<Double>> e: chisquareResult.entrySet()) {
			StringBuffer sb = new StringBuffer();
			sb.append(e.getKey());
			for(Double d: e.getValue()) {
				if (d==null) {
					sb.append(" 0 ");
				} else {
					sb.append(" ").append(d).append(" ");
				}
			}
			System.err.println(sb.toString());
		}
		StringBuffer sb = new StringBuffer();
		sb.append("ts");
		for (String s: chisquareResultKeyInd) {
			sb.append("\t").append(s).append("\t");
		}
		System.err.println(sb);
	}
	
	static void valuesStat() {
		
		for(int i=0; i<sorted.size(); i++) {
			DescriptiveStatistics stats_value_cnt = new DescriptiveStatistics();
			DescriptiveStatistics stats_time = new DescriptiveStatistics();
			Frequency fq_cnt = new Frequency();
			List<List<EntryRecord>> list = sorted.get(i);
			System.err.println( "====="+ Arrays.deepToString(groupList.get(i).toArray()) +"=====" );
			for (List<EntryRecord> t: sorted.get(i)) {
				stats_value_cnt.addValue(t.size());
				long time = t.get(t.size()-1).ts - t.get(0).ts;
				stats_time.addValue(time);
			}
			//System.err.println("average time: " + ((double)timetotal/sorted.get(i).size() ));
			System.err.println("average time: " + stats_time.getMean() );
			System.err.println("time stats " + stats_time);
			System.err.println("log lines per value " + stats_value_cnt.getMean());
			System.err.println("log lines per value stats " + stats_value_cnt);
			double cnt99 = stats_value_cnt.getPercentile(99.9);
			for (List<EntryRecord> t: list) {
				if(t.size()>cnt99) {
					System.err.println( " -->" + t.get(0).varvalue );
				}
			}
			
			System.err.println("Number of distinct values: " + list.size());
			for (List<EntryRecord> t: list) {
				System.err.println(" ->" + t.get(0).varvalue +" " + t.size());
			}
			
		}
	}
	
	
	static void printSortedResult() {
		for(int i=0; i< sorted.size(); i++) {
			System.err.println( "====="+ Arrays.deepToString(groupList.get(i).toArray()) +"=====" );
			for (List<EntryRecord> t: sorted.get(i)) {
				System.err.println("  ==="+ t.get(0).varvalue +"===");
				//Collections.sort(t);
				for (EntryRecord e: t) {
					System.err.println("  "+e);
				}
			}
			System.err.println();
		}
	}
	
	static int groupseq;
	static PrintStream dataout = null;
	static String nextValueGroupFileName() {
		File f = new File( IndexConstants.DATA_ROOT, "_value_group_" + groupseq );
		groupseq +=1;
		return f.getAbsolutePath();
	}
	
	static void exportClusteringData() throws Exception{
		
		File valuebreakfile = new File(IndexConstants.DATA_ROOT, "value_break_down");
		if (!valuebreakfile.exists()) 
			valuebreakfile.mkdirs();
		for(int i=0; i< sorted.size(); i++) {  // for each variable name.. 
			// first calculate how many distinct log lines total..
			ArrayList<String> varLogList = new ArrayList<String>();
			if (sorted.get(i).size()<30) {
				continue;
			}
			for (List<EntryRecord> t: sorted.get(i)) {
				String varvalue = t.get(0).varvalue;
				List<String> replog = reportingLogs.get(varvalue);
				if (replog==null) 
					continue;
				//System.err.println("%  ==="+ varvalue +"===");
				for (String s: replog) {
					if (!varLogList.contains(s)) {
						varLogList.add(s);
					}
				}
				//System.err.println( Arrays.deepToString(replog.toArray()) );
			}
			if (varLogList.size()<5)
				continue;
			
			
			if (dataout!=null)
				dataout.close();
			
			dataout = new PrintStream( new File( nextValueGroupFileName() ) );
			System.setOut( dataout );
			
			System.out.println( "%====="+ Arrays.deepToString(groupList.get(i).toArray()) +"=====" );
			System.out.println( "%->" +Arrays.deepToString(varLogList.toArray()) );
			
			int c=1;
			for(String logid: varLogList) {
				RegExprRecord rec = DbUtils.findLogEntry(logid);
				System.out.print("%" + c+"," +logid.substring(logid.lastIndexOf(".")+1) +"," + rec.RegExpr+"\t");
				c +=1;
			}
			
			System.out.println();
			
			for (List<EntryRecord> t: sorted.get(i)) { // for each var value
				String varvalue = t.get(0).varvalue;
				
				//PrintWriter out;
				//try {
				//	out = new PrintWriter(new File(valuebreakfile, varvalue.replace("\\", "/")));
				//} catch (FileNotFoundException e1) {
				//	out=null;
				//}
				
				
				//System.out.println(+ varvalue +"===");
				int[] vectorfeature = new int[varLogList.size()];
				for (EntryRecord e: t) { // for each log line
				//	if (out!=null)
				//		out.println(e);
					int ind = varLogList.indexOf(e.logid);
					vectorfeature[ind] +=1;
				}
				//if (out!=null)
				//	out.close();
				StringBuffer sb = new StringBuffer();
				for(int cnt: vectorfeature) {
					sb.append(cnt).append(" ");
				}
				sb.append("   %").append(varvalue).append("");
				System.out.println(sb.toString());
			}
			//for (List<EntryRecord> t: sorted.get(i)) { // for each var value
			//	String varvalue = t.get(0).varvalue;
			//	System.out.println("%" + varvalue.replace(" ", "_"));
			//}
		}
	}
	
	
	
	static List<String> ignoreVar = new ArrayList();
	/**
	 * detect which variables can have the same value
	 */
	static void varGroup() {
		
		//int distinctVarCnt = 0;
		for(Entry<String, List<EntryRecord>> e: matchingvalues.entrySet()) {
			EntryRecord er = e.getValue().get(0);
			Integer setind = varGroups.get(er.varname);
			if (ignoreVar.contains(er.varname)) {
				continue;
			}
			if (setind!=null) {
				; // do nothing
			} else {
				List<String> set = new ArrayList<String>();
				if (!set.contains(er.varname)){
					set.add(er.varname);
				}
				groupList.add(set);
				varGroups.put(er.varname, groupList.size()-1);
			}
			
			for (int i=1; i<e.getValue().size(); i++) {
				er =  e.getValue().get(i);
				String varname = er.varname;
				setind = varGroups.get(varname);
				if (setind==null) {
					// first see if something in this group has a record
					setind = varGroups.get(e.getValue().get(0).varname);
					assert setind !=null;
					List<String> set =groupList.get(setind);
					if (!set.contains(varname)) {
						set.add(varname);
					}
					varGroups.put(varname, setind);
				} else {
					List<String> set =groupList.get(setind);
					if (!set.contains(varname)) {
						System.err.println("?????");
						set.add(varname);
					}
				}
			}
		}
	}
	
}

class EntryRecord implements Comparable<EntryRecord>{
	long ts;
	String node;
	String varname;
	String varvalue;
	String logid;
	String msg;
	
	public EntryRecord(long ts, String node,String varname, String varvalue, String logid, String msg){
		this.ts = ts;
		this.node = node;
		this.varname = varname;
		this.varvalue = varvalue;
		this.logid = logid;
		this.msg = msg;
	}
	
	public String toString() {
//		return this.ts + " | " +this.node+" | "
//		//+this.varname+" | "+this.varvalue +" | " 
//		 +this.msg;
		return this.node +" | " +this.msg;
	}

	public int compareTo(EntryRecord o) {
		if (this.ts==o.ts) {
			int nodecomp = this.node.compareTo(node);
			if (nodecomp==0) {
				int logidcomp = this.logid.compareTo(o.logid);
				return logidcomp;
			} else {
				return nodecomp;
			}
		}else {
			return (int) (this.ts-o.ts);
		}
	}
}