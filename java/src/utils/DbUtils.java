package utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;

import org.apache.derby.tools.ij;

import conf.ConfUtil;

import ast.Utils;

import data.ElementList;
import data.MethodDesc;
import data.MethodEdge;
import data.RegExprRecord;
import data.StringExprElement;

public class DbUtils {
	
	private static Logger LOG = LogFormatter.getLogger(DbUtils.class);
	
	public static Connection conn=null;
	private static PreparedStatement findmethodid;
	private static PreparedStatement insertmethod;
	private static PreparedStatement insertmethodedge;
	private static PreparedStatement selectalledges;
	private static PreparedStatement selectCallidGraph;
	private static PreparedStatement selectMethodById;
	private static PreparedStatement findcallid;
	
	private static PreparedStatement insertPreLog;
	private static PreparedStatement insertPostLog;
	
	private static PreparedStatement insertToString;
	private static PreparedStatement findToStringForClass;
	private static PreparedStatement insertLogEntry;
	private static PreparedStatement findLogEntry;
	
	private static PreparedStatement insertToStringSubClass;
	
	private static PreparedStatement insertCallCnt;
	
	private static PreparedStatement findAllSubClasses;
	
	private static PreparedStatement findMethodWithPreLogId;
	private static PreparedStatement findMethodWithPostLogId;
	private static PreparedStatement findCallGraphEdge;
	
	public static Connection getConn() {

		if (conn!=null)
			return conn;
		
		String driver;
		String dbName;
		String connectionURL;
		
		if (ConfUtil.getConfig().getBoolean("db.useembedded")) {
			driver = "org.apache.derby.jdbc.EmbeddedDriver";
			File dataRoot = new File( ConfUtil.getConfig().getString("dataRoot") );
			String dbSubDir = ConfUtil.getConfig().getString("db.embedded.dbname", "DB");
			//System.err.println(dataRoot +" " + dbSubDir);
			File dbPath =new File(dataRoot, dbSubDir);
			LOG.info("Using embedded DB driver. DataDir=" +dbPath.getAbsolutePath());
			dbName= dbPath.getAbsolutePath();
			
			connectionURL = "jdbc:derby:" + dbName+";create=true";
		} else {
			driver= "org.apache.derby.jdbc.ClientDriver";
			dbName=ConfUtil.getConfig().getString("db.dbserver.dbname");
			String hostname = ConfUtil.getConfig().getString("db.dbserver.hostname");
			connectionURL = "jdbc:derby:" + hostname +"/"+ dbName +";create=true";
			LOG.info("Using DB Server URL=" + connectionURL);
		}
		//String connectionURL ="jdbc:derby://localhost:1527/myDB;create=true";
		
		try{
		    Class.forName(driver); 
		} catch(java.lang.ClassNotFoundException e) {
		    e.printStackTrace();
		}

		try {
		    conn = DriverManager.getConnection(connectionURL);
			findmethodid = conn.prepareStatement("select methodid from methoddesc where class=? and name=? and params=?");
			insertmethod = conn.prepareStatement("insert into methoddesc (class, name, params, returntype) values(?,?,?,?)");
			insertmethodedge = conn.prepareStatement("insert into callgraph (callerid, calleeid, line, inloop) values(?,?,?,?)");
			selectalledges = conn.prepareStatement("select md1.class as callerclass, md1.name as callername," +
					"md2.class as calleeclass, md2.name as calleename, c.line as line " +
					"from methoddesc md1, methoddesc md2, callgraph c " +
					"where md1.methodid = c.callerid and md2.methodid=c.calleeid");
			selectCallidGraph = conn.prepareStatement("select * from callgraph");
			findcallid = conn.prepareStatement("select edgeid from callgraph where callerid=? and calleeid=? and line=?");
			
			selectMethodById = conn.prepareStatement("select * from methoddesc where methodid=?");
			insertPreLog = conn.prepareStatement("insert into premethodlog values(?,?)");
			insertPostLog = conn.prepareStatement("insert into postmethodlog values(?,?)");
			insertToString = conn.prepareStatement("insert into tostringdb values(?,?,?,?,?)");
			findToStringForClass = conn.prepareStatement("select * from tostringdb where class=?");
			insertLogEntry = conn.prepareStatement("insert into logentries values(?,?,?,?,?,?,?,?)");
			insertToStringSubClass = conn.prepareStatement("insert into tostringsubclass values(?,?)");
			insertCallCnt = conn.prepareStatement("insert into callcount values(?,?)");
			findLogEntry = conn.prepareStatement("select * from logentries where logid=?");
			findAllSubClasses = conn.prepareStatement("select subclass from tostringsubclass where superclass=?");
			
			findMethodWithPreLogId = conn.prepareStatement("select methodid from pre where logid=?");
			findMethodWithPostLogId = conn.prepareStatement("select methodid from post where logid=?");
			findCallGraphEdge = conn.prepareStatement("select * from edgelist where edgeid=?");
		}  catch (Throwable e)  {   
		    e.printStackTrace();
		}
		return conn;
	}
	
	public static void closeConnection() {
		try {
			conn.close();
		} catch (Exception e) {
			;
		}
	}
	
	public static int findMethodId(String methodclass, String methodname, String params) {
		try {
			int ret =-1;
			
			findmethodid.setString(1, methodclass);
			findmethodid.setString(2, methodname);
			findmethodid.setString(3, params);
			ResultSet rs = findmethodid.executeQuery();
			if (rs.next()) {
				ret = rs.getInt("methodid");
			}
			rs.close();
			return ret;
		} catch (SQLException e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	public static int findCallId(int callerid, int calleeid, int line) {
		try {
			int ret =-1;
			
			findcallid.setInt(1, callerid);
			findcallid.setInt(2, calleeid);
			findcallid.setInt(3, line);
			ResultSet rs = findcallid.executeQuery();
			if (rs.next()) {
				ret = rs.getInt("edgeid");
			}
			rs.close();
			return ret;
		} catch (SQLException e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	public static MethodDesc findMethod(int methodid) {
		try {
			selectMethodById.setInt(1, methodid);
			ResultSet rs = selectMethodById.executeQuery();
			if (rs.next()) {
				MethodDesc md = readIntoMethodDesc(rs);
				rs.close();
				return md;
			} else {
				System.err.println("method " +methodid + " is not found");
				rs.close();
				return null;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	
	public static String[] findAllSubClasses(String classname) {
		try {
			findAllSubClasses.setString(1, classname);
			ResultSet rs = findAllSubClasses.executeQuery();
			ArrayList<String> ret = new ArrayList<String>();
			while (rs.next()) {
				ret.add( rs.getString("subclass") );
			} 
			rs.close();
			return ret.toArray(new String[ret.size()]);
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static void insertMethod(String methodclass, String methodname, String params, String returntype) {
		try {
			insertmethod.setString(1, methodclass);
			insertmethod.setString(2, methodname);
			insertmethod.setString(3, params);
			insertmethod.setString(4, returntype);
			insertmethod.executeUpdate();
		} catch (java.sql.SQLIntegrityConstraintViolationException ex) {
			;
		}  catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static int insertMethodEdge(int callerid, int calleeid, int line, boolean inloop) {
		try {
			insertmethodedge.setInt(1, callerid);
			insertmethodedge.setInt(2, calleeid);
			insertmethodedge.setInt(3, line);
			insertmethodedge.setInt(4, inloop?1:0);
			insertmethodedge.executeUpdate();	
		} catch (java.sql.SQLIntegrityConstraintViolationException ex) {
			;
		}  catch (SQLException e) {
			e.printStackTrace();
		}
		int ret = findCallId(callerid, calleeid, line);
		LOG.fine("inserted edge.. " + callerid +";"+calleeid+";"+line +" id="+ret);
		return ret;
	}
	
	public static void insertPreLog(String logid, int methodid) {
		try {
			insertPreLog.setString(1,logid);
			insertPreLog.setInt(2, methodid);
			insertPreLog.executeUpdate();
		} catch (java.sql.SQLIntegrityConstraintViolationException ex) {
			//ex.printStackTrace();
		}  catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static void insertPostLog(String logid, int methodid) {
		try {
			insertPostLog.setString(1,logid);
			insertPostLog.setInt(2, methodid);
			insertPostLog.executeUpdate();
		} catch (java.sql.SQLIntegrityConstraintViolationException ex) {
			//ex.printStackTrace();
		}  catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	
	
	public static void insertToStringRec(String typename, String RegExpr, String namemap, String typemap, int status) {
		try {
			System.err.println("inserting " + typename + insertToString);
			insertToString.setString(1, typename);
			insertToString.setString(2, RegExpr);
			insertToString.setString(3, namemap);
			insertToString.setString(4, typemap);
			insertToString.setInt(5, status);
			insertToString.executeUpdate();
		} catch (java.sql.SQLIntegrityConstraintViolationException ex) {
			;
		}  catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static void insertToStringRec(String typename, ElementList list, int status) {
		try {
			System.err.println("inserting " + typename);
			insertToString.setString(1, typename);
			insertToString.setString(2, list.toRegExpr(false));
			insertToString.setString(3, list.getNameMapString());
			insertToString.setString(4, list.getTypeMapString());
			insertToString.setInt(5, status);
			insertToString.executeUpdate();
		} catch (java.sql.SQLIntegrityConstraintViolationException ex) {
			;
		}  catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static RegExprRecord findToStringForClass(String classname) {
		try {
			findToStringForClass.setString(1, classname);
			ResultSet rs = findToStringForClass.executeQuery();
			if (rs.next()) {
				RegExprRecord ret = readToStringIntoExprRecord(rs);
				//System.err.println("found::: " + ret);
				rs.close();
				return ret;
			} else {
				//System.err.println("toString for" + classname + " is not found");
				rs.close();
				return null;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static RegExprRecord findLogEntry(String logid) {
		try {
			findLogEntry.setString(1, logid);
			ResultSet rs = findLogEntry.executeQuery();
			if (rs.next()) {
				RegExprRecord ret = readLogIntoExprRecord(rs);
				//System.err.println("found::: " + ret);
				rs.close();
				return ret;
			} else {
				System.err.println(logid + " is not found");
				rs.close();
				return null;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static String findCallGraphEdge(int edgeid) {
		try {
			findCallGraphEdge.setInt(1, edgeid);
			ResultSet rs = findCallGraphEdge.executeQuery();
			if (rs.next()) {
				String ret = rs.getString("caller") +"->"+
						rs.getString("callee");
				rs.close();
				return ret;
			} else {
				System.err.println(edgeid + " is not found");
				rs.close();
				return null;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static RegExprRecord readLogIntoExprRecord(ResultSet rs) {
		
		try {
			String namestr = rs.getString(3);
			String typestr = rs.getString(4);
			String regexpr = rs.getString(2);
			String logid = rs.getString(1);
			String constStr = rs.getString(5);
			int level = rs.getInt(6);
			String method = rs.getInt(8)+"";
			RegExprRecord expr = new RegExprRecord(regexpr,constStr ,namestr, typestr, logid, level, method);
			return expr;
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	
	public static RegExprRecord readToStringIntoExprRecord(ResultSet rs) {
		
		try {
			String namestr = rs.getString(3);
			String typestr = rs.getString(4);
			String regexpr = rs.getString(2);
			String logid = rs.getString(1);
			String constStr = rs.getString(5);
			RegExprRecord expr = new RegExprRecord(regexpr,constStr ,namestr, typestr, logid, -1, null);
			return expr;
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	
	public static void insertLogEntry(String logid, ElementList list, 
			int level, boolean inloop, int methodid) {
		
		insertLogEntry(logid, list.toRegExpr(false), list.getNameMapString(),
				list.getTypeMapString(), list.getConstantString(), 
				level, inloop, methodid);
	}
	
	public static void insertLogEntry(String logid, String regexpr, 
			String namemapstr, String typemapstr, String constString,
			int level, boolean inloop, int methodid) {
		try {
			insertLogEntry.setString(1, logid);
			insertLogEntry.setString(2, regexpr);
			insertLogEntry.setString(3, namemapstr);
			insertLogEntry.setString(4, typemapstr);
			insertLogEntry.setString(5, constString);
			insertLogEntry.setInt(6, level);
			insertLogEntry.setInt(7, inloop?1:0);
			insertLogEntry.setInt(8, methodid);
			insertLogEntry.executeUpdate();
		} catch (java.sql.SQLIntegrityConstraintViolationException ex) {
			;
		}  catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	
	public static void insertToStringSubClass(String superclass, String subclass) {
		try {
			insertToStringSubClass.setString(1, superclass);
			insertToStringSubClass.setString(2, subclass);
			insertToStringSubClass.executeUpdate();
		} catch (java.sql.SQLIntegrityConstraintViolationException ex) {
			;
		}  catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static void insertCallCnt(int callid, int cnt) {
		try {
			insertCallCnt.setInt(1, callid);
			insertCallCnt.setInt(2, cnt);
			insertCallCnt.executeUpdate();
		} catch (java.sql.SQLIntegrityConstraintViolationException ex) {
			ex.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static ArrayList<Integer> findMethodsWithLogId(String logid, boolean isPre) {
		PreparedStatement st;
		ArrayList<Integer> ret = new ArrayList<Integer>();
		if (isPre) {
			st = findMethodWithPreLogId;
		} else {
			st = findMethodWithPostLogId;
		} 
		try {
			st.setString(1, logid);
			ResultSet rs = st.executeQuery();
			while (rs.next()) {
				ret.add(rs.getInt("methodid"));
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return ret;
	}
	
	public static MethodDesc readIntoMethodDesc(ResultSet rs) {
		try {
			return new MethodDesc(rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getInt(1));
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static void dumpTable(String tablename) {
		try {
			Connection conn = getConn();
			Statement st =conn.createStatement();
			ResultSet rs = st.executeQuery("select * from "+ tablename);
			while (rs.next()) {
				printRecordLine(rs);
			}
			rs.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void printRecordLine(ResultSet rs) {
		try {
			ResultSetMetaData meta = rs.getMetaData();
			int colmax = meta.getColumnCount();
			int i;
			Object o = null;
			for (i = 0; i < colmax; ++i) {
				o = rs.getObject(i + 1); 
				System.out.print("[" +o.toString() + "] ");
			}
			System.out.println(" ");
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	public static MethodEdge readIntoMethodEdge(ResultSet rs) {
		try {
			MethodDesc md1 = findMethod(rs.getInt("callerid"));
			MethodDesc md2 = findMethod(rs.getInt("calleeid"));
			return new MethodEdge(md1, md2, rs.getInt("line"), rs.getInt("inloop")==1);
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	
	public static ResultSet getAllEdges() {
		try {
			return selectCallidGraph.executeQuery();
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static void runScript(String scriptname) throws Exception{
		ij.runScript(getConn(), new FileInputStream(scriptname), "UTF-8", System.err, "UTF-8" );
	}
	
	public static void resetDB() throws Exception{
		runScript("src/utils/schema.sql");
		runScript("src/utils/views.sql");
	}
	
	public static void dumpDB() throws Exception {
		runScript("src/utils/dump.sql");
	}
	
	public static void main(String[] args) throws Exception{
		dumpDB();
	}
}
