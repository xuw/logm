package ebay;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.log4j.helpers.FileWatchdog;

public class CalculateMVCDim {
	
	static File datadir = new File("data/ebay");
	
	static HashMap<Integer, ArrayList<Integer>> mvcmap = new HashMap<Integer, ArrayList<Integer>>();
	
	public static void main(String[] args) throws Exception{
		
		
		ObjectInputStream keymapin = new ObjectInputStream(new FileInputStream(new File(datadir,"binkey.bin")));
		HashMap<String, Integer> keymap = (HashMap<String, Integer>) keymapin.readObject();
		HashMap<Integer, String> revkeymap = new HashMap<Integer, String>();
		for(Entry<String, Integer> e: keymap.entrySet()) {
			revkeymap.put(e.getValue(), e.getKey());
		}
		
		HashMap<String, Integer> represent = new HashMap<String, Integer>();
		HashMap<Integer, Integer> equivalent = new HashMap<Integer, Integer>();
		
		// equavalent types..
		for(Entry<String, Integer> e: keymap.entrySet()) {
			String k = e.getKey();
			//System.err.println(e.getKey());
			if(k.startsWith("EXEC ")) {
				k = "EXEC";
			} else if (k.startsWith("FETCH ")) {
				k = "FETCH";
			} else if (k.startsWith("Conn") || k.startsWith("Dcp") || k.startsWith("Evicted")) {
				k = k.replaceAll("[0-9]", "");
			} else {
				k = k.replaceAll("[0-9]", "");
			}
			if(represent.containsKey(k)) {
				System.err.println(e.getKey() +" is equavalent to "+ k);
			} else {
				represent.put(k, e.getValue());
				revkeymap.put(e.getValue(), k);
			}
			equivalent.put(e.getValue(), represent.get(k));
			
		}
		
		
		File[] seqfiles = datadir.listFiles( new FileFilter(){

			public boolean accept(File pathname) {
				if(pathname.getName().startsWith("seq")){
					return true;
				}
				return false;
			}
		});
		
		int cnt = 0;
		for(File f: seqfiles) {
			BufferedReader in = new BufferedReader(new FileReader(f));
			String line = in.readLine();
			int linecnt = 1;
			
			while(line!=null) {
				
				String[] parts = line.split(":");
				
				int type = Integer.parseInt(parts[0]);
				int[] seq = MCVUtils.readSeq(parts[1], equivalent);
				
				//System.err.println(parts[1] + " " + type);
				ArrayList<Integer> typeall = mvcmap.get(type);
				if(typeall ==null) {
					typeall = new ArrayList<Integer>();
					mvcmap.put(type, typeall);
				}
				
				for(int s: seq) {
					int ind = typeall.indexOf(s);
					if (ind<0) {
						typeall.add(s);
					}
				}
				line = in.readLine();
				cnt +=1;
				if(cnt%100000==0) {
					System.err.println("processed " + cnt + " sequences.");
				}
			}
		}
		
		
		
		System.err.println("found "+ mvcmap.size() + " types");
		
		System.err.println("dimensitons for each type ");
		for(Entry<Integer, ArrayList<Integer>> e: mvcmap.entrySet()) {
			System.err.println(e.getKey() + " " + e.getValue().size() );
			
			String fn = "k" + e.getKey();
			PrintWriter out = new PrintWriter(new FileWriter(new File(datadir, fn)) );
			for(int i=0; i< e.getValue().size(); i++) {
				int keyind = e.getValue().get(i);
				out.println(i +" " + keyind +" " + " " + revkeymap.get(keyind) );
			}
			out.flush(); 
			out.close();
		}
		
		
		ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream( new File(datadir, "mvckey")));
		out.writeObject(mvcmap);
		out.writeObject(equivalent);
		out.flush();
		out.close();
		
	}
	
	
}
