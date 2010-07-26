package mine;

import index.IndexConstants;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.ObjectInputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import conf.ConfUtil;

import utils.DbUtils;

public class LabelColoring {

	
	static final String[] colors = {"#000000", "#FF0000", "#00FF00", "#0000FF", "#00FFFF", "#FF00FF", "#C0C0C0", "#8B0000", "#00CCCC", "#00CCCC"};
	private static HashMap<String, Integer> namemap = new HashMap<String, Integer>();
	
	public static void main(String[] args) throws Exception{
		
		for (String dbname: IndexConstants.LOG_DB_NAMES)
			doOneDb(args, dbname);
	}
	
	public static void doOneDb(String[] args, String dbname) throws Exception{
		int cnt =1;
		
		File keymapfile = new File(IndexConstants.TMP_DIR,"keymap");
		
		if (keymapfile.exists()) {
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(keymapfile));
			namemap = (HashMap<String, Integer>) in.readObject();
			cnt = in.readInt();
			in.close();
			in = null;
		}
		
		
		Connection conn = DbUtils.getConn();
		Statement stm = conn.createStatement();
		
		int start = ConfUtil.getConfig().getInt("dataSeg.start",-1);
		int end = ConfUtil.getConfig().getInt("dataSeg.end",-1);
		PreparedStatement ps=null;
		if (start >=0 && end>=0) {
			ps = conn.prepareStatement("select seq, logid ,logbody from "+ dbname +" where seq>? and seq<? order by threadid, seq");
			ps.setInt(1, start);
			ps.setInt(2, end);
			System.err.println("Start=" + start +"  end=" +end);
		} else {
			ps = conn.prepareStatement("select threadid, seq, logid ,logbody from "+ dbname +" order by threadid, seq");
			System.err.println("Use full log.");
		}
		ResultSet rs  =ps.executeQuery();
		
		BufferedReader lbreader = new BufferedReader(
				new FileReader( new File(IndexConstants.VIS_OUTPUT_DIR, "labels") ));
		String lb = lbreader.readLine();
		
		
		BufferedReader kgramreader = new BufferedReader(
				new FileReader( new File(IndexConstants.TMP_DIR, "kgramPr") ));
		String kg = kgramreader.readLine();
		
		List<String> skipconf = ConfUtil.getConfig().getList("skip.id",new ArrayList<String>());
		
		List<Integer> skip = new ArrayList<Integer>();
		
		for (String s: skipconf) {
			skip.add( Integer.parseInt(s) );
		}
		
		PrintStream changeout = new PrintStream(
				new File(IndexConstants.TMP_DIR, "lbchanges") );
		
		PrintStream dataout = new PrintStream(
				new File(IndexConstants.VIS_OUTPUT_DIR, "color_out.htm") );
		dataout.println("<html><body><table>");
		
		int outputcnt = 1;
		int last_thread = 0;
		int cur_thread =0;
		int lbind=0;

		while (rs.next() && lb!=null && kg!=null) {
			try {
				
				String logid = rs.getString("logid");
				int logidint = namemap.get(logid);
				if (skip.contains(logidint)) {
					System.err.println("skipping " + logid);
					continue;
				}
				last_thread = cur_thread;
				cur_thread = rs.getInt("threadid");				
				
				if (cur_thread != last_thread) {
					dataout.println("<tr><td>THREAD_BEGIN_MARKER</td><td>"+kg+" "+(outputcnt++)+"</td></tr>");
					kg = kgramreader.readLine();
					dataout.println("<tr><td>THREAD_BEGIN_MARKER</td><td>"+kg+" "+(outputcnt++)+"</td></tr>");
					kg = kgramreader.readLine();
					dataout.println("<tr><td>THREAD_BEGIN_MARKER</td><td>"+kg+" "+(outputcnt++)+"</td></tr>");
					kg = kgramreader.readLine();
					//System.err.println("after skip: " + rs.getString("logbody"));
				}
				lbind = Integer.parseInt(lb);
				changeout.println(last_thread==cur_thread?0:1);
				dataout.print("<tr><td>");
				dataout.print(rs.getString("logbody"));
				dataout.print("</td><td><font color=\"" + colors[lbind-1] + "\">");
				dataout.print("@@"+outputcnt +"@@@" +cur_thread+"@@@"+ kg +"@@"+ logidint +"</font></td></tr>");
				
//				if (outputcnt != rs.getInt("seq")) {
//					System.err.println(outputcnt +" " +rs.getInt("seq") );
//				}
				dataout.println();
				outputcnt +=1;
				
				if (cnt%1==0) {
					//lb = lbreader.readLine();
					kg = kgramreader.readLine();
				}
				cnt += 1;
			} catch (Exception e) {
				System.err.println(cnt);
				e.printStackTrace();
				continue;
			}
		}
		dataout.println("</table></body></html>");
	}

}
