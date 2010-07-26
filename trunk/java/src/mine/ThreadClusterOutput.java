package mine;

import index.IndexConstants;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.ObjectInputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import utils.DbUtils;
import conf.ConfUtil;

public class ThreadClusterOutput {

	private static HashMap<String, Integer> namemap = new HashMap<String, Integer>();	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception{
		
		File keymapfile = new File(IndexConstants.TMP_DIR,"keymap");
		
		if (keymapfile.exists()) {
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(keymapfile));
			namemap = (HashMap<String, Integer>) in.readObject();
			in.close();
			in = null;
		}
		
		for (String dbname: IndexConstants.LOG_DB_NAMES) {
			System.err.println("Processing DB "+ dbname);
			processDB(dbname);
		}
		
	}
	
	public static void processDB(String dbname) throws Exception{
		
		Connection conn = DbUtils.getConn();
		Statement stm = conn.createStatement();
		
		int start = ConfUtil.getConfig().getInt("dataSeg.start",-1);
		int end = ConfUtil.getConfig().getInt("dataSeg.end",-1);
		PreparedStatement ps=null;
		if (start >=0 && end>=0) {
			ps = conn.prepareStatement("select ts, seq, logid ,logbody from "+dbname+" where seq>? and seq<? order by threadid, seq");
			ps.setInt(1, start);
			ps.setInt(2, end);
			System.err.println("Start=" + start +"  end=" +end);
		} else {
			ps = conn.prepareStatement("select ts,threadid, seq, logid ,textentry from "+dbname+" order by threadid, seq");
			System.err.println("Use full log.");
		}
		ResultSet rs  =ps.executeQuery();
		
		BufferedReader lbreader = null;
		try {
			lbreader = new BufferedReader(new FileReader(new File(
					IndexConstants.VIS_OUTPUT_DIR, "tcluster_"+dbname)));
		} catch (FileNotFoundException e) {
			System.err.println("Need to generate threadcluster directory first!!");
			System.exit(0);
		}
		ArrayList<Integer> lbs = new ArrayList<Integer>();
		String lbstr = lbreader.readLine();
		while (lbstr!=null) {
			lbs.add(Integer.parseInt(lbstr) );
			lbstr = lbreader.readLine();
		}
		lbreader.close();
		int numCluster =  Collections.max(lbs);
		System.err.println("Total Number of distinct clusters: " + Collections.max( lbs ) );
		
		List<String> skipconf = ConfUtil.getConfig().getList("skip.id",new ArrayList<String>());
		
		List<Integer> skip = new ArrayList<Integer>();
		
		for (String s: skipconf) {
			skip.add( Integer.parseInt(s) );
		}
		
		PrintStream timelabelout = new PrintStream(
				new File(IndexConstants.TMP_DIR, "ts_threadcluster_"+dbname)
				);
		PrintStream[] dataouts = new PrintStream[numCluster];
		File outdir = new File(IndexConstants.VIS_OUTPUT_DIR, "threadclusterout_"+dbname);
		if (!outdir.exists()) {
			outdir.mkdirs();
		}
		for (int i=0; i<numCluster; i++) {
			File subdir = new File(outdir, "cluster"+i);
			subdir.mkdirs();
			//dataouts[i] = new PrintStream( new File(subdir, "log"));
		}
		
		int outputcnt = 1;
		int last_thread = 0;
		int cur_thread =0;
		
		PreparedStatement ps1 = conn.prepareStatement("select min(threadid) as minthreadid from "+dbname);
		ResultSet minrs = ps1.executeQuery();
		minrs.next();
		cur_thread = minrs.getInt("minthreadid");
		
		System.err.println("MIN Thread ID is: " + cur_thread);
		
		int i=0;
		while ( i<lbs.size() ) {
			int lb = lbs.get(i);
			if (!rs.next()) {
				System.err.println("Done with data..");
				break;
			}
			try {
				
				String logid = rs.getString("logid");
				Timestamp ts = rs.getTimestamp("ts");
				
				int logidint = namemap.get(logid);
				if (skip.contains(logidint)) {
					System.err.println("skipping " + logid);
					continue;
				}
				last_thread = cur_thread;
				cur_thread = rs.getInt("threadid");
				
				if ( i>=lbs.size() ) {
					//System.err.println("ran out of labels?? ");
					break;
				}
				if (cur_thread != last_thread) {
					// finished one thread
					if (dataouts[lb-1]!=null) 
						dataouts[lb-1].close();
					dataouts[lb-1] = null;
					//dataouts[lbind-1].println();
					//dataouts[lbind-1].println("^^^^^^^^^^^Thread "+ last_thread +"^^^^^^^");
					//dataouts[lbind-1].println();
					//System.err.println(last_thread);
					
					// get next cluster id
					i+=1;
					if (i>=lbs.size())
						break;
					lb = lbs.get(i);
				}
				if (dataouts[lb-1]==null) {
					dataouts[lb-1] = new PrintStream( new File(new File(outdir, "cluster"+(lb-1)), "t"+cur_thread));
				}
				try{
				dataouts[lb-1].print(rs.getString("textentry"));
				}catch (NullPointerException e) {
					System.err.println(lb-1 +" " + cur_thread +" " + last_thread);
				}
				timelabelout.println( ts.getTime()/1000 +" " + cur_thread +" " +lb );
				dataouts[lb-1].println();
				outputcnt +=1;
				
			} catch (Exception e) {
				e.printStackTrace();
				continue;
			}
		}
		
		System.err.println("Processed labels: " + i);
		
		for (i=0; i<numCluster; i++) {
			if (dataouts[i]!=null)
				dataouts[i].close();
		}
		
		System.err.println("Processed log entries: " + outputcnt);
		
		rs.close();
		
	}

}
