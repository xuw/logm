package mine;

import index.IndexConstants;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.Map.Entry;

import conf.ConfUtil;

import utils.DbUtils;

public class LogIdExport {
	
	
	static HashMap<String, Integer> namemap = new HashMap<String, Integer>();
	static TreeMap<Integer, String> idmap = new TreeMap<Integer, String>();
	static int cnt = 1;
	
	public static void main(String[] args) throws Exception{
		
		
		if (!IndexConstants.TMP_DIR.exists()) {
			IndexConstants.TMP_DIR.mkdir();
		}
		
		File keymapfile = new File(IndexConstants.TMP_DIR,"keymap");
		
		if (keymapfile.exists()) {
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(keymapfile));
			namemap = (HashMap<String, Integer>) in.readObject();
			for (Entry<String, Integer> entry: namemap.entrySet()) {
				idmap.put(entry.getValue(), entry.getKey());
			}
			cnt = in.readInt();
			in.close();
			in = null;
		}
		
		if (!namemap.containsKey("THREAD_BEGIN_MARKER")) {
			namemap.put("THREAD_BEGIN_MARKER", cnt);
			idmap.put(cnt, "THREAD_BEGIN_MARKER");
			cnt +=1;
		}
		
		
		PrintStream dataout;
		PrintStream textout;
		
		
		Connection conn = DbUtils.getConn();
		int start = ConfUtil.getConfig().getInt("dataSeg.start",-1);
		int end = ConfUtil.getConfig().getInt("dataSeg.end",-1);
		
		boolean sortBythread = ConfUtil.getConfig().getBoolean("data.sortByThreadId", false);
		String orderby = "";
		
		for (String logdbname : IndexConstants.LOG_DB_NAMES) {
			int rowcnt = 0;
			int outcnt =0;
			System.err.println("Processing DB "+ logdbname);
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
			dataout = new PrintStream(outfile);
			textout = new PrintStream(textoutfile);

			PreparedStatement ps = null;
			if (start >= 0 && end >= 0) {
				ps = conn
						.prepareStatement("select threadid, seq, logid,textentry from "
								+ logdbname
								+ " where seq>? and seq<?" + orderby);
				ps.setInt(1, start);
				ps.setInt(2, end);
				System.err.println("Start=" + start + "  end=" + end);
			} else {
				ps = conn
						.prepareStatement("select threadid, seq, logid,textentry from "
								+ logdbname + orderby);
				System.err.println("Use full log.");
			}
			ResultSet rs = ps.executeQuery();

			List<String> skipconf = ConfUtil.getConfig().getList("skip.id",
					new ArrayList<String>());

			List<Integer> skip = new ArrayList<Integer>();

			for (String s : skipconf) {
				skip.add(Integer.parseInt(s));
			}

			int last_thread = 0;

			while (rs.next()) {

				String logid = rs.getString("logid");
				int threadid = rs.getInt("threadid");
				Integer id = namemap.get(logid);
				// System.err.println(rs.getInt("seq") + " "+ id);
				if (id != null) {
				} else {
					namemap.put(logid, cnt);
					idmap.put(cnt, logid);
					id = cnt;
					cnt += 1;
				}

				if (!skip.contains(id)) {
					if (threadid != last_thread && sortBythread) {
						dataout.println(namemap.get("THREAD_BEGIN_MARKER"));
						dataout.println(namemap.get("THREAD_BEGIN_MARKER"));
						dataout.println(namemap.get("THREAD_BEGIN_MARKER"));
						textout.println("THREAD_BEGIN_MARKER");
						textout.println("THREAD_BEGIN_MARKER");
						textout.println("THREAD_BEGIN_MARKER");
					}
					last_thread = threadid;
					dataout.print(id);
					textout.print(id + " "
							+ rs.getString("textentry").replaceAll("\n", " ")); // new
					// line
					// included
					rowcnt += 1;
					outcnt += 1;
					if (outcnt % 10 >= 0) {
						dataout.println();
						textout.println();
					} else {
						dataout.print(" ");
						textout.print(" ");
					}

				} else {
					rowcnt += 1;
				}

				// System.err.println(rs.getString( "logid" ) );
				// if (rowcnt > 500000) {
				// break;
				// }
			}
			rs.close();
			dataout.close();

			System.err.println("Total scanned " + rowcnt);
			System.err.println("Output " + outcnt);

		}
		ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(keymapfile));
		out.writeObject(namemap);
		out.writeInt(cnt);
		out.close();
		
		PrintStream txtout = new PrintStream(
				new File(IndexConstants.TMP_DIR, "txtmap.txt") );
		
		PreparedStatement ps = conn.prepareStatement("select regEx from logentries where logid=?");
		ResultSet rs = null;
		for (Entry<Integer, String> entry: idmap.entrySet()) {
			try {
				ps.setString(1, entry.getValue());
				rs = ps.executeQuery();
				rs.next();
				txtout.println(entry.getKey() +" | " + entry.getValue() + " | " + rs.getString("regEx"));
				rs.close();
			} catch (SQLException e) {
				txtout.println(entry.getKey() +" | " + entry.getValue() +" | " + entry.getValue());
			}
		}
		txtout.close();
		
	}

}
