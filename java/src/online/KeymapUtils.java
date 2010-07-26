package online;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map.Entry;

public class KeymapUtils {
	
	public static Hashtable<Integer, Integer> keymap;
	public static Hashtable<Integer, Integer> revkeymap;
	
	
	public static void init(File keymapfile) {
		try {
			ObjectInputStream datain = new ObjectInputStream(
					new FileInputStream( keymapfile ) );
			keymap = (Hashtable<Integer, Integer>) datain.readObject();
			initRevKeyMap();
			datain.close();
		}catch (IOException e) {
			keymap = new Hashtable<Integer, Integer>();
			//e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public static void writeKeyMap(File keymapfile) throws IOException{
		ObjectOutputStream objout = new ObjectOutputStream(new FileOutputStream(keymapfile));
		objout.writeObject(keymap);
		objout.close();
	}
	
	public static void initRevKeyMap(){
		revkeymap = new Hashtable<Integer, Integer>();
		for(Entry<Integer, Integer> e: keymap.entrySet()) {
			revkeymap.put(e.getValue(), e.getKey());
		}
	}
	
	public static String cntvecToEventStr(int[] cntvec) {
		StringBuffer sb = new StringBuffer();
		for(int i=0; i< cntvec.length; i++) {
			if(cntvec[i] >0) {
				sb.append(cntvec[i]).append("x").append( revkeymap.get(i) ).append(" ");
			}
		}
		return sb.toString();
	}
	
	public static int[] strToCntVec(String line) {
		String[] parts = line.split("\\s");
		int[] cntvec = new int[parts.length-2];
		for(int i=1; i< parts.length-2; i++) {
			///System.err.println(parts[i]);
			cntvec[i-1] = Integer.parseInt( parts[i] );
		}
		return cntvec;
	}
	
	
	public static String strToEventStr(String line) {
		//System.err.println(line);
		int[] cntvec = strToCntVec(line);
		return cntvecToEventStr(cntvec);
	}
	
}
