package scripts;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import utils.DbUtils;

public class CountLOLVar {


	public static void main(String[] args) throws Exception {
		Connection conn = DbUtils.getConn();
		Statement stm = conn.createStatement();
		ResultSet rs= stm.executeQuery("select namemap from logentries");
		
		int recordcnt = 0;
		int varcnt = 0;
		while (rs.next()) {
			recordcnt +=1;
			String s =  rs.getString("namemap");
			if (s.length() ==0) {
				continue;
			} else {
				String [] t = s.split(";");
				varcnt += t.length;
				System.err.println(s + " " + t.length);
			}
		}
		
		System.err.println("lines="+recordcnt +" var=" +varcnt);
	}

}
