package mine;

import index.IndexConstants;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Map.Entry;

public class DataFileUtil {
	
	private HashMap<String, Integer> namemap;
	private TreeMap<Integer, String> idmap;
	
	public HashMap<String, Integer> getNameMap(){
		if (namemap==null) {
			init();
		}
		return namemap;
	}
	
	public TreeMap<Integer, String> getIdMap(){
		if (idmap==null) {
			init();
		}
		return idmap;
	}
	
	private void init(){
		namemap = new HashMap<String, Integer>();
		idmap = new TreeMap<Integer, String>();
		
		
		try {
		int cnt =0;
		File keymapfile = new File(IndexConstants.TMP_DIR,"keymap");
		
		if (keymapfile.exists()) {
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(keymapfile));
			namemap = (HashMap<String, Integer>) in.readObject();
			for (Entry<String, Integer> entry: namemap.entrySet()) {
				idmap.put(entry.getValue(), entry.getKey());
			}
			cnt = in.readInt();
			in.close();
			in = null;
		}
		
		if (!namemap.containsKey("THREAD_BEGIN_MARKER")) {
			namemap.put("THREAD_BEGIN_MARKER", cnt);
			idmap.put(cnt, "THREAD_BEGIN_MARKER");
			cnt +=1;
		}
		} catch(IOException ex) {
			ex.printStackTrace();
			System.exit(-1);
		} catch (ClassNotFoundException ex) {
			ex.printStackTrace();
			System.exit(-1);
		}
	}
}
