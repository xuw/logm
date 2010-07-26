package mine;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import data.RegExprRecord;

import utils.DbUtils;
import utils.StringUtils;

public class GenerateLogString {
	
	public static Hashtable<String , Integer> namemap = new Hashtable<String, Integer>();
	public static Hashtable<Integer, String> reversemap = new Hashtable<Integer, String>();
	public static Hashtable<Character, Character> inloopLogs = new Hashtable<Character, Character>();
	
	public static Hashtable<Pair, Integer> inloopmthod = new Hashtable<Pair, Integer>(); // caller -> callee
	
	public static Hashtable<Integer, String> methodLog = new Hashtable<Integer, String>(); // methodid -> log map
	private static int cnt =30;
	
	public static Hashtable<Character, Integer> logMethod = new Hashtable<Character, Integer>();
	
	public static ArrayList<Integer> breakpoints = new ArrayList<Integer>();
	
	static StringBuffer byteLog = new StringBuffer();
	
	private static class Pair{
		int caller;
		int callee;
		public Pair(int caller, int callee) {
			super();
			this.caller = caller;
			this.callee = callee;
		}
		@Override
		public int hashCode() {
			return (caller*12134+callee*232);
		}
		@Override
		public boolean equals(Object obj) {
			Pair oth = (Pair)obj;
			return this.callee==oth.callee && this.caller==oth.caller;
		}
		
		public String toString() {
			return caller+"->" +callee;
		}
		
	}
	
	public static void main(String[] args) throws Exception{
		Connection conn = DbUtils.getConn();
		Statement stm = conn.createStatement();
		ResultSet rs= stm.executeQuery("select distinct threadid from sampleLog order by threadid");
		while (rs.next()) {
			System.err.print( rs.getInt("threadid") +" " );
			runquery(rs.getInt("threadid"));
			System.err.print("\n");
		}
		
		
		System.err.println( "=========================" );
		PreparedStatement ps = conn
		.prepareStatement("select calleeid,inloop from callgraph where callerid=?");

		PreparedStatement enclosingMethod = conn
		.prepareStatement("select methodId, inloop from logentries where logid=?");
		
		PreparedStatement logInMethod = conn.prepareStatement("select logid from logentries where methodId=?");
		
		for (String logid : namemap.keySet()) {

			enclosingMethod.setString(1, logid);
			int methodid = -1;
			ResultSet mrs = enclosingMethod.executeQuery();
			boolean inloop = false;
			while(mrs.next()) {
				methodid = mrs.getInt("methodId");
				inloop = mrs.getInt("inloop") ==1;
			}
			mrs.close();
			if ( methodid ==-1){
				System.err.println( "did not find method for log entry " + logid);
				continue;
			}
			
			Integer t = namemap.get(logid);
			if (t!=null) {
				logMethod.put((char)t.intValue(), methodid);
				System.err.println("ADDING: " + t+ " " + methodid);
				if (inloop) {
					inloopLogs.put((char)t.intValue(), (char)t.intValue());
				}
			}
			if (methodLog.containsKey(methodid)) {
				continue;
			}
			
			ConcurrentHashMap<Integer, Boolean> allCalled = new ConcurrentHashMap<Integer, Boolean>();
			allCalled.put(methodid, false);
			boolean changed = true;
			
			while (changed) {
				changed = false;
				for (int caller : allCalled.keySet()) {
					if (!allCalled.get(caller)) { // not processed yet
						allCalled.put(caller, true);
						ps.setInt(1, caller);
						rs = ps.executeQuery();
						while (rs.next()) {
							int callees = rs.getInt("calleeid");
							boolean m_inloop = rs.getInt("inloop")==1;
							if (m_inloop) {
								inloopmthod.put( new Pair (caller, callees) , 0);
							}
							if (!allCalled.containsKey(callees)) {
								allCalled.put(callees, false);
								changed = true;
							} else {
								// called multiple times
								// a hack,, currently put into inloopmethod,, should really track order of call
								inloopmthod.put(new Pair(caller,callees),1 );
							}
						}
						rs.close();
					}
				}
			}
			
			System.err.println(Arrays.deepToString(inloopmthod.keySet().toArray()));
			
			StringBuffer sb = new StringBuffer();
			for (int method: allCalled.keySet()) {
				//System.err.println(logid + " " +method);
				if ( methodLog.containsKey(method)) {
					sb.append(methodLog.get(method));
					continue;
				}
				logInMethod.setInt(1, method);
				ResultSet lrs = logInMethod.executeQuery();
				while (lrs.next()) {
					String log = lrs.getString(1);
					t = namemap.get(log);
					if (t!=null ) {
						sb.append( (char) t.intValue() );
					}
				}
			}
			methodLog.put(methodid, sb.toString());
			System.err.println(methodid + " " + sb.toString());
		}
		
		StringBuffer outbuf = new StringBuffer();
		int breakpt = 0;
		breakpoints = new ArrayList<Integer>();
		for (int i=0; i< byteLog.length()-1; i++) {
			outbuf.append(byteLog.charAt(i));
			//System.err.print(reversemap.get((int)byteLog.charAt(i)) +" ");
			if (separateEvent( i, breakpt ) ) {
				outbuf.append("\n");
				breakpoints.add(i+1);
				breakpt = i;
				//System.err.print("\n");
			}
		}
		
		int cnt =0; 
		StringBuffer labelstr = new StringBuffer();
		Timestamp ts1 = null;
		boolean newEvent = true;
		rs= stm.executeQuery("select * from sampleLog order by threadid, seq");
		while (rs.next()) {
			
			if (newEvent) {
				newEvent = false;
				ts1 = rs.getTimestamp("ts");
			}
			String logid = rs.getString("logid");
			
			RegExprRecord rec = StringUtils.simpleExpand(logid);
			String logbody = rs.getString("logbody");
			
			String regEx = rec.RegExpr;
			String[] nameMap = rec.nameMap.split(";");
			String[] typeMap = rec.typeMap.split(";");
				
			//System.err.print(regEx +":" + nameMap);
			Pattern p = Pattern.compile(regEx);
			Matcher m = p.matcher(logbody);
			
			if (! m.matches() ) {
				System.err.println("failed to match.. " + logid);
			} else {
				for(int i=0; i<nameMap.length; i++) { 
					if (nameMap[i].length()==0) {
						continue;
					}
					if (nameMap[i].toLowerCase().endsWith("id") || nameMap[i].toLowerCase().endsWith("name")) {
						System.err.print("LBN ");
					} else if (StringUtils.isNumeric(typeMap[i])) {
						System.err.print("DT ");
					} else {
						System.err.print("LB ");
					}
					System.err.println(nameMap[i] +" - " +typeMap[i] +" - " + m.group(i+1));
				}
			}
			//System.err.println( rs.getString("textentry"));
			labelstr.append(byteLog.charAt(cnt));
			cnt +=1;
			if ( breakpoints.contains(cnt) ) {
				newEvent = true;
				Timestamp ts2 = rs.getTimestamp("ts");
				long time_last = ts2.getTime()-ts1.getTime();
				System.err.println("DT " + time_last);
				System.err.println("LB " + labelstr.toString());
				labelstr = new StringBuffer();
				System.err.println();
			}
		}
		rs.close();
		
		//System.err.println();
		System.err.println(outbuf.toString());
		System.err.println( Arrays.deepToString( reversemap.entrySet().toArray() ));
	}
	
	public static void runquery(int threadid) throws Exception{
		Connection conn = DbUtils.getConn();
		Statement stm = conn.createStatement();
		ResultSet rs= stm.executeQuery("select logid from sampleLog where threadid="+ threadid +"order by threadid, seq");
		while (rs.next()) {
			String logid = rs.getString("logid");
			Integer ind = namemap.get(logid);
			if (ind==null) {
				namemap.put(logid, cnt);
				reversemap.put(cnt, logid);
				ind =cnt;
				cnt +=1;
			}
			//System.err.println(rs.getString("logid") +" " + (char)ind.intValue());
			System.err.print(ind.intValue()+" ");
			byteLog.append((char) ind.intValue());
		}
		//breakpoints.add(byteLog.length()); // break between different thread
	}
	
	
	public static boolean separateEvent (int index, int breakpt) throws Exception{
		
		char curr = byteLog.charAt(index);
		char next = byteLog.charAt(index+1);
		if( next == (char)100 ) {
			System.err.println();
		}
		if ((int) curr >200) {
			throw new RuntimeException("too big? " + index +" " + (int)curr);
		}
		//System.err.println("logchar=(" + curr +")");
		Integer thismethod = logMethod.get(curr);
		if (thismethod==null) {
			System.err.println("cannot find method for log char " + (int) curr);
			return false;
		}
		//int nextmethod = logMethod.get(next);
		//System.err.println("thismethod=" + thismethod);
		String str = methodLog.get(thismethod);
		
		Integer nextMethod = logMethod.get(next);
		int inClosure = str.indexOf(next);
		//System.err.println(str + " " + curr + " " + next + " " + inClosure);
		if ( inClosure >= 0) {
			if ( !thismethod.equals(nextMethod) ) {
				return false;
			} else {
				// same method
				String currid = reversemap.get((int) curr);
				String nextid = reversemap.get((int) next);
				//System.err.println("CURR " + (int)curr + currid +" NEXT "+ (int)next +nextid);
				if (currid==null || nextid==null) {
					System.err.println("cannot find logid" + (int) curr +" " + currid +" "+ (int) next +" "+ nextid);
					return false;
				}
				int currline = getLineNumber( currid );
				int nextline = getLineNumber( nextid );
				if (currline < nextline  || inloopLogs.contains(next) ) { 
					return false;
				} else {
					// detect if method called in a loop here..
					for (int i = index-1; i >= breakpt+1; i--) {
						Integer tmpmethod = logMethod.get( byteLog.charAt(i) );
						if (tmpmethod ==null) {
							continue;
						}
						System.err.println("looking for pair:: " + tmpmethod + "->" + nextMethod);
						if ( inloopmthod.get( new Pair(tmpmethod, nextMethod) ) !=null ) {
							return false;
						}
					}
					return true;
				}
			}
		} else {
			// not in closure..  
			// see if it is due to a return to early method
			String nextid = reversemap.get((int) next);
			if (nextid==null) {
				System.err.println("CANNOT FIND LOGID " + (int) next +" "+ nextid);
				return false;
			}
			int nextline = getLineNumber( nextid );
			
			// see if next is contained in tmpmethod's log closure, 
			// and does not repeat the existing logs
			// first detect if repeating
			for (int i= breakpt +1; i<=index-1; i++) {
				char tmpchar = byteLog.charAt(i);
				if (tmpchar==next  
						&& !inloopLogs.containsKey(next) 
						&& !inloopmthod.containsKey(nextMethod)
						) {
					return true;  // repeating, separate
				}
			}
			
			for (int i = index-1; i >= breakpt+1; i--) {
				char tmpchar = byteLog.charAt(i);
				Integer tmpmethod = logMethod.get( tmpchar );
				String mclosurestr = methodLog.get(tmpmethod);
				
				if (tmpmethod ==null) {
					continue;
				}
				if (mclosurestr.indexOf((int) next ) >0) {
					return false;
				}
//				if (tmpmethod.equals(nextMethod)) {	
//					String tmplogid = reversemap.get( byteLog.charAt(i) );
//					if (tmplogid==null) {
//						continue;
//					} 
//					int tmpline = getLineNumber(tmplogid);
//					if (nextline > tmpline) {
//						return false;
//					}
//				}
			}
			return true;
		}
	}
	
	public static int getLineNumber(String logid) {
		String line = logid.substring(logid.indexOf("-") +1);
		return Integer.parseInt(line);
	}
	
}
