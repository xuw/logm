package mine;

import index.IndexConstants;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import utils.DbUtils;
import conf.ConfUtil;

public class LogFreq {


	static HashMap<String, Integer> namemap = new HashMap<String, Integer>();
	static int cnt = 1;
	
	public static void main(String[] args) throws Exception{
		
		for (String dbname: IndexConstants.LOG_DB_NAMES)
			doOneDb(args, dbname);
	}
	
	
	public static void doOneDb(String[] args, String dbname) throws Exception{
		
		if (!IndexConstants.TMP_DIR.exists()) {
			IndexConstants.TMP_DIR.mkdir();
		}
		
		File keymapfile = new File(IndexConstants.TMP_DIR,"keymap");
		
		if (keymapfile.exists()) {
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(keymapfile));
			namemap = (HashMap<String, Integer>) in.readObject();
			cnt = in.readInt();
			in.close();
			in = null;
		}
		
		if (!namemap.containsKey("THREAD_BEGIN_MARKER")) {
			namemap.put("THREAD_BEGIN_MARKER", cnt++);
		}
		
		
		PrintStream dataout = new PrintStream(
			new File(IndexConstants.TMP_DIR,  dbname + "_timeddata") );
		
		int outcnt =0;
		Connection conn = DbUtils.getConn();
		int start = ConfUtil.getConfig().getInt("dataSeg.start",-1);
		int end = ConfUtil.getConfig().getInt("dataSeg.end",-1);
		
		
		PreparedStatement ps=null;
		if (start >=0 && end>=0) {
			ps = conn.prepareStatement("select ts, seq, logid from " +dbname+ " where seq>? and seq<? order by seq");
			ps.setInt(1, start);
			ps.setInt(2, end);
			System.err.println("Start=" + start +"  end=" +end);
		} else {
			ps = conn.prepareStatement("select ts, seq, logid from "+dbname+" order by seq");
			System.err.println("Use full log.");
		}
		ResultSet rs  =ps.executeQuery();
		
		
		while (rs.next()) {
			
			String logid = rs.getString("logid");
			Timestamp timestamp = rs.getTimestamp("ts");
			int seq = rs.getInt("seq");
			long ts_ms = timestamp.getTime();
			Integer id = namemap.get(logid);
			//System.err.println(rs.getInt("seq") + " "+ id);
			if (id!=null) {	
			} else {
				namemap.put(logid, cnt);
				id = cnt;
				cnt +=1;
			}
			
			dataout.println(ts_ms +","+ id+","+seq);
			outcnt += 1;

		}
		rs.close();
		dataout.close();
		
		System.err.println("Output " + outcnt);
		
		ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(keymapfile));
		out.writeObject(namemap);
		out.writeInt(cnt);
		out.close();
		
	}
	

	

}
