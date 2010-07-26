package ebay;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map.Entry;

public class EventDict {
	
	public static HashMap<String, Integer> map = new HashMap<String, Integer>();
	private static int cnt =0;
	
	
	public static int getIndex(String eventstr) {
		Integer ind = map.get(eventstr);
		if(ind != null) {
			return ind;
		} else {
			map.put(eventstr, cnt);
			cnt +=1;
			return cnt-1;
		}
	}
	
	public static void writeToFile(PrintStream out) {
		for(Entry<String, Integer> e : map.entrySet()) {
			out.println(e.getValue() + " " +e.getKey());
		}
	}
	
	public static void writeToBinFile(File f) throws IOException{
		ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(f));
		out.writeObject(map);
		out.close();
	}
	
	public static void readKeyHash(File f) throws IOException, ClassNotFoundException {
		ObjectInputStream in = new ObjectInputStream(new FileInputStream(f));
		map = (HashMap<String, Integer>) in.readObject();
		in.close();
		System.err.println("loaded key map...");
	}

}
