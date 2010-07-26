package mine;

import index.IndexConstants;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

import conf.ConfUtil;

public class EncodedLogReader {

	private HashMap<String, Integer> namemap;
	private TreeMap<Integer, String> idmap;
	private int[] encodedLog;
	private String dbname;
	
	public EncodedLogReader(String dbname) throws Exception{
		
		this.dbname = dbname;
		DataFileUtil datafileutil = new DataFileUtil();
		namemap = datafileutil.getNameMap();
		idmap = datafileutil.getIdMap();
		
		boolean sortBythread = ConfUtil.getConfig().getBoolean("data.sortByThreadId", false);
		File outfile = null;
		if (!sortBythread) {
			outfile = new File(IndexConstants.TMP_DIR, dbname+"_data");
		} else {
			outfile = new File(IndexConstants.TMP_DIR, dbname +"_data_thread");
		}
		BufferedReader logReader = new BufferedReader( new FileReader(outfile ) );
		logReader.mark(0);
		
		ArrayList<Integer> encodedLogt = new ArrayList<Integer>();
		
		String l = logReader.readLine();
		while(l!=null) {
			encodedLogt.add(Integer.parseInt(l));
			l = logReader.readLine();
		}
		logReader.close();
		this.encodedLog = new int[encodedLogt.size()];
		for (int i=0; i< encodedLogt.size(); i++) {
			this.encodedLog[i] = encodedLogt.get(i);
		}
	}
	
	public HashMap<String, Integer> getNameMap(){
		return namemap;
	}
	
	public TreeMap<Integer, String> getIdMap(){
		return idmap;
	}
	
	public int[] getEncodedLog() {
		return this.encodedLog;
	}
	
	public String getDbname() {
		return this.dbname;
	}
}
