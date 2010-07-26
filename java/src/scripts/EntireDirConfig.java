package scripts;

import index.IndexConstants;

import java.io.File;
import java.io.FilenameFilter;

public class EntireDirConfig {

	/**
	 * @param args
	 */
	//static String DIR =  "ec2_10";
	static String DIR = "ec2_10_2/comb";
	public static void main(String[] args) {
		File logdir = new File(IndexConstants.DATA_ROOT, DIR);
		File[] files = logdir.listFiles( new FilenameFilter() {
			public boolean accept(File dir, String name) {
				if ( name.endsWith(".log") && name.contains("tracker") )
					return true;
				return false;
			}
		});
		
		//<logFile>jobtracker-r10.log</logFile>
  		//<logDBName>jobtracker_r10</logDBName>
		for (File f: files) {
			System.err.println("\t\t<logFile>" +  DIR+"/"+ f.getName() +"</logFile>");
			System.err.println("\t\t<logDBName>" + getType(f.getName()) +"_" + getIp(f.getName()) + "</logDBName>");
		}
		
	}
	
	static String getIp(String fn) {
		int i = fn.indexOf("ip-");
		String ip = fn.substring(i+3).replace(".log", "");
		ip = ip.replace("-", "_");
		return ip;
	}
	
	static String getType(String fn) {
		if (fn.indexOf("datanode") > 0) 
			return "dn";
		if (fn.indexOf("-namenode") >0) 
			return "nn";
		if (fn.indexOf("second")>0)
			return "snn";
		if (fn.indexOf("jobtracker")>0)
			return "jc";
		if (fn.indexOf("tasktracker")>0)
			return "tc";
		return "??";
	}
	
}

