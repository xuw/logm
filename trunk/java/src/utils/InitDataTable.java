package utils;

import java.sql.Connection;
import java.sql.SQLException;

import conf.ConfUtil;

public class InitDataTable {

	/**
	 * @param args
	 */
	public static void resetTable(String tablename) throws Exception{
		
		System.err.println("(RE)SETTING table: " + tablename);
		Connection conn = DbUtils.getConn();
		java.sql.Statement s = conn.createStatement();
		try {
			s.execute("drop table " +tablename);
		} catch (SQLException e) {
			System.err.println("WARNING - table does not exist" );
		}
		String createstatement = "create table " +tablename;
			
	 createstatement += "(seq INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1) primary key,"
	 +"ts timestamp,"
	 +"threadid integer,"
	 +"logid varchar(250),"
	 +"logbody varchar(1000),"
	 +"textentry clob,"
	 +"lbs clob,"
	 +"dts clob )";
		
		s.execute(createstatement);
		
		if (s.getWarnings()==null) {
			System.err.println("DONE.. " + tablename + " created.");
		}
		
	}

}
