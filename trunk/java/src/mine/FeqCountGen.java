package mine;

import index.IndexConstants;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintStream;
import java.util.HashMap;

public class FeqCountGen {
	
	
	static final boolean PRINT_TS = false;
	static HashMap<String, Integer> namemap = new HashMap<String, Integer>();
	static int cnt = 1;
	
	public static void main(String[] args) throws Exception{
		
		for (String dbname: IndexConstants.LOG_DB_NAMES)
			doOneDb(args, dbname);
	}
	
	public static void doOneDb(String[] args, String dbname) throws Exception{
		if (!IndexConstants.TMP_DIR.exists()) {
			IndexConstants.TMP_DIR.mkdir();
		}
		
		File keymapfile = new File(IndexConstants.TMP_DIR,"keymap");
		
		if (keymapfile.exists()) {
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(keymapfile));
			namemap = (HashMap<String, Integer>) in.readObject();
			cnt = in.readInt();
			in.close();
			in = null;
		}
		
		windowCnt(dbname);
	}
	
	
	static void windowCnt(String dbname) throws IOException{
		int[] cntarr =null ;
		BufferedReader in = new BufferedReader(new FileReader(
				new File(IndexConstants.TMP_DIR, dbname +"_timeddata")) );
		
		PrintStream dataout = new PrintStream(
				new File(IndexConstants.TMP_DIR, dbname +"freqcnt") );
		
		int linecnt =1;
		String line = in.readLine();
		long  nexttime = -1;
		while(line!=null) {
			String[] t = line.split(",");
			long ts = Long.parseLong(t[0]);
			int lb = Integer.parseInt(t[1]);
			//System.err.println(ts + " "+nexttime);
			if (ts>nexttime) {
				System.err.println("break at " + ts +" " + line);
				nexttime = ts+5000;
				if (cntarr !=null) {
					//output
					if (PRINT_TS) {
						dataout.print(ts +" " + linecnt +" ");
					}
					for (int c : cntarr){
						dataout.print(c +" ");
					}
					dataout.println();
				}
				cntarr = new int[namemap.size()];
			}
			cntarr[lb-1] +=1;
			line = in.readLine();
			linecnt +=1;
		}
		
	}
	
}
