package ebay;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.ObjectInputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;

public class CreateMVCs {
	
	static File datadir = new File("data/ebay");
	static HashMap<Integer, ArrayList<Integer>> mvcmap = new HashMap<Integer, ArrayList<Integer>>();
	
	public static void main(String[] args) throws Exception{
		
		// read map
		ObjectInputStream mapin = new ObjectInputStream(new FileInputStream(new File(datadir,"mvckey")));
		mvcmap = (HashMap<Integer, ArrayList<Integer>>)mapin.readObject();
		HashMap<Integer, Integer> equivalent = (HashMap<Integer, Integer>)mapin.readObject();
		mapin.close();
		
		// read sequence files
		File[] seqfiles = datadir.listFiles( new FileFilter(){

			public boolean accept(File pathname) {
				if(pathname.getName().startsWith("seq")){
					return true;
				}
				return false;
			}
		});
		
		PrintStream[] mvcouts = new PrintStream[5000];
		File mvcoutdir = new File(datadir,"MCV");
		if(!mvcoutdir.exists()) {
			mvcoutdir.mkdirs();
		}
		
		for(File f: seqfiles) {
			BufferedReader in = new BufferedReader(new FileReader(f));
			String line = in.readLine();
			
			int linecnt = 1;
			
			while(line!=null) {
				
				String[] parts = line.split(":");
				int type = Integer.parseInt(parts[0]);
				int[] seq = MCVUtils.readSeq(parts[1], equivalent);
				int endts = getEndTimeStamp(parts[2]);
				
				ArrayList<Integer> typeall = mvcmap.get(type);
				if(typeall ==null) {
					throw new RuntimeException("haven't seen this type before?? " + type);
				}
				
				PrintStream out = mvcouts[type];
				if(out==null) {
					out = new PrintStream(new FileOutputStream(new File(mvcoutdir, "m"+type)));
					mvcouts[type] = out;
				}
				
				int[] mvc = new int[typeall.size()];
				// contruct MVC
				for(int s: seq) {
					int index = typeall.indexOf(s);
					if( index<0 ) {
						throw new RuntimeException("haven't seen type " + s);
					}
					mvc[index] +=1;
				}
				
				//output MVC
				String mvcstr = MCVUtils.arrayToMCV(mvc, "%"+type+":"+f.getName()+":"+linecnt , endts);
				//System.err.println(mvcstr);
				out.println(mvcstr);
				
				line = in.readLine();
				linecnt +=1;
			}
			in.close();
		}
		
		for(PrintStream p: mvcouts) {
			if(p!=null) {
				p.flush();
				p.close();
			}
		}
		
	}
	
	private static int getEndTimeStamp(String s) {
		int index = s.lastIndexOf(" ");
		String tsstr = s.substring(index+1, s.length()-1);
		return Integer.parseInt(tsstr);
	}
	
	
}
