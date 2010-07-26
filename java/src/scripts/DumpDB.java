package scripts;

import index.IndexConstants;

import java.io.File;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;

import org.apache.derby.tools.ij;

import utils.DbUtils;

public class DumpDB {
	
	
	static File dumpdir;
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception{
		dumpdir = new File(IndexConstants.DATA_ROOT,"dbdump");
		dumpdir.mkdirs();
		
		File scriptFile = new File(dumpdir, "script");
		PrintWriter out = new PrintWriter(scriptFile);
		
		for (String tablename: IndexConstants.LOG_DB_NAMES) {
			String filename = tablename+".csv";
			File dumpfile = new File(dumpdir, filename);
			String dumpstatement = "call SYSCS_UTIL.SYSCS_EXPORT_TABLE(null, '" + tablename.toUpperCase();
			dumpstatement += "', '" +dumpfile.getAbsolutePath().replaceAll("\\\\", "/")+"', null, null, null);";
			out.println(dumpstatement);
		}
		out.close();
		
		DbUtils.runScript(scriptFile.getAbsolutePath());
		
	}
	

}
