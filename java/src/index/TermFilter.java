package index;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Map.Entry;

import utils.QueryUtils;

import data.RegExprRecord;

public class TermFilter {
	
	private static final File TERM_FILE= new File(IndexConstants.DATA_ROOT,"termmap");
	private Hashtable<String, Integer> termmap;
	
	
	public TermFilter(boolean read){
		if (read && !TERM_FILE.exists()) {
			throw new RuntimeException("No Term File!!");
		} else if (read) {
			try {
				termmap = new Hashtable<String, Integer>();
				BufferedReader in = new BufferedReader( new FileReader(TERM_FILE) );
				String s = in.readLine();
				while (s!=null) {
					String[] ss = s.split(" ");
					termmap.put(ss[0], Integer.parseInt(ss[1]));
					s = in.readLine();
				}
				in.close();
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException("reading term file failed!!");
			}
		} else if (!read) {
			termmap = new Hashtable<String, Integer>();
		}
		
	}
	
	 public void addTerms(String str) {
		  	str = str.replaceAll("[^1-9a-zA-Z:\\.]", " ");
		  	String[] terms = str.split("\\s");
			for (String s: terms) {
				Integer termCnt = termmap.get(s);
				if (termCnt==null) {
					termmap.put(s, 0);
				} else {
					termmap.put(s, termCnt+1);
				}
			}
	  }
	
	public String filter(String str){
		String[] terms = str.split("\\s");
		StringBuffer sb = new StringBuffer();
		for (String s: terms) {
			if (termmap.containsKey(s)) {
				sb.append(s).append(" ");
			}
		}
		return QueryUtils.escapeQueryString(sb.toString());
	}
	
	public void writeToDisk() throws IOException{
		PrintStream out = new PrintStream(
				new FileOutputStream( 
						TERM_FILE ));
		for(Entry<String, Integer> e: termmap.entrySet()) {
			out.println(e.getKey()+" " + e.getValue());
		}
		out.close();
	}
	
	public String toString() {
		return  Arrays.deepToString(termmap.keySet().toArray());
	}
	
}
