package mine;

import index.IndexConstants;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.TreeMap;

import utils.DbUtils;

public class WordBreaking {
	
	
	public static HashMap<String , Integer> namemap = new HashMap<String, Integer>();
	public static TreeMap<Integer, String> reversemap = new TreeMap<Integer, String>();
	
	public static int[] encodedLog; 
	
	public static HashMap<Integer, MethodNode> methodgraph;
	
	public static HashMap<Integer, Integer> logMethodMap = new HashMap<Integer, Integer>();
	
	public static void main(String[] args) throws Exception{
		
		for (String dbname: IndexConstants.LOG_DB_NAMES)
			doOneDb(args, dbname);
	}
	
	public static void doOneDb(String[] args, String dbname) throws Exception{
		Connection conn = DbUtils.getConn();
		Statement stm = conn.createStatement();
		
		EncodedLogReader logreader = new EncodedLogReader(dbname);
		namemap = logreader.getNameMap();
		reversemap = logreader.getIdMap();
		encodedLog = logreader.getEncodedLog();
		
		int threadBeginMarkerLogId = namemap.get("THREAD_BEGIN_MARKER");
		
		System.err.println( encodedLog.length );
		System.err.println( namemap.get("THREAD_BEGIN_MARKER") );
		
		System.err.println( Arrays.deepToString( namemap.keySet().toArray()) );
		
		buildMethodCallGraph();
		
		for (MethodNode t: methodgraph.values() ) {
			System.err.println(t);
		}
		
		logMethodMap.put(threadBeginMarkerLogId, -1);
		
		//System.err.println( Arrays.deepToString( logMethodMap.entrySet().toArray() ) );
		
		System.err.println("===========Word Breaking Result========");
		
		Integer parentmethod = logMethodMap.get( encodedLog[0] );
		for (int l: encodedLog) {
			if (parentmethod ==null || parentmethod == -1) {
//				if (parentmethod==null) {
//					parentmethod = logMethodMap.get(l);
//					continue;
//				}
				System.err.println();
				System.err.print(l +" ");
			} else {
				MethodNode parentnode = methodgraph.get(parentmethod);
				if (inClosure(parentnode, l)) {
					System.err.print(l+" ");
				} else {
					System.err.println();
					System.err.print(l +" ");
				}
			}
			parentmethod = logMethodMap.get(l);
		}
		
	}
	
	private static boolean inClosure(MethodNode method, int logid){
		ArrayList<MethodNode> callees = method.getCallees();
		for (Integer t: method.getLogentries()) {
			if (t.equals(logid)) {
				return true;
			}
		}
		for (MethodNode callee: callees) {
			if ( inClosure(callee, logid) ) {
				return true;
			}
		}
		return false;
	}
	
	public static void buildMethodCallGraph() throws Exception{
		methodgraph = new HashMap<Integer, MethodNode>();
		Connection conn = DbUtils.getConn();
		PreparedStatement ps = conn.prepareStatement(
				"select distinct methodId from logentries");
		
		
		ResultSet allmethod = ps.executeQuery();
		while(allmethod.next()) {
			//System.err.println(allmethod.getInt("methodid") +" " + allmethod.getString("class") +"."+allmethod.getString("name"));
			int methodid = allmethod.getInt("methodid");
			//System.err.println(methodid);
			MethodNode t = new MethodNode(methodid);
			addMethodNodeToGraph(t);
		}
		allmethod.close();
	}
	
	private static void addMethodNodeToGraph(MethodNode root) throws Exception{
		
		if ( methodgraph.containsKey(root.getMethodid()) ) {
			return;
		} else {
			methodgraph.put(root.getMethodid(), root);
			//System.err.println("adding " + root.getMethodid());
		}
		Connection conn = DbUtils.getConn();
		PreparedStatement ps = conn.prepareStatement(
			"select calleeid,inloop from callgraph where callerid=?");
		PreparedStatement logInMethod = conn.prepareStatement(
				"select logid from logentries where methodId=?");
		logInMethod.setInt(1, root.getMethodid());
		ResultSet lrs = logInMethod.executeQuery();
		while (lrs.next()) {
			String log = lrs.getString(1);
			Integer t = namemap.get(log);
			if (t!=null ) {
				root.addLogPrint(t);
				logMethodMap.put(t, root.getMethodid());
			} else {
				//System.err.println("CANNOT FIND LOGID " + log);
			}
		}
		lrs.close();
		
		ps.setInt(1, root.getMethodid());
		ResultSet rs = ps.executeQuery();
		while(rs.next()) {
			int methodid = rs.getInt("calleeid");
			//System.err.println("CALLEE-"+methodid);
			MethodNode t = new MethodNode(methodid);
			t.addCaller(root);
			root.addCallee(t);
			addMethodNodeToGraph(t);
		}
		rs.close();
	}
}

class GraphNode {
	
}

class MethodNode extends GraphNode{
	
	private int methodid;
	private ArrayList<MethodNode> callers;
	private ArrayList<MethodNode> callees;
	private ArrayList<Integer> logentries;
	
	public MethodNode(int methodid) {
		this.methodid = methodid;
		callers = new ArrayList<MethodNode>();
		callees = new ArrayList<MethodNode>();
		logentries = new ArrayList<Integer>();
	}
	
	public void addCallee(MethodNode callee) {
		this.callees.add(callee);
	}
	
	public void addCaller(MethodNode caller) {
		this.callers.add(caller);
	}
	
	public void addLogPrint(Integer logid) {
		logentries.add(logid);
	}
	
	public int getMethodid() {
		return this.methodid;
	}

	public ArrayList<MethodNode> getCallers() {
		return callers;
	}

	public ArrayList<MethodNode> getCallees() {
		return callees;
	}

	public ArrayList<Integer> getLogentries() {
		return logentries;
	}

	@Override
	public boolean equals(Object obj) {
		return this.methodid == ( (MethodNode) obj).methodid;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(this.methodid);
		sb.append(": CALLERS:");
		for(MethodNode t: callers) {
			sb.append(t.methodid).append(",");
		}
		sb.append(" CALLEES:");
		for(MethodNode t: callees) {
			sb.append(t.methodid).append(",");
		}
		sb.append(" LOGS:");
		for(Integer t: logentries) {
			sb.append(t).append(",");
		}
		return sb.toString();
	}
	
}

class LogNode extends MethodNode {
	public int logid;
	public LogNode(int logid) {
		super(-1);
		this.logid = logid;
	}
}