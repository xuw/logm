package ebay;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Stack;
import java.util.zip.GZIPInputStream;

public class LogReader {
	
	static File binkeyfile = new File("c:\\tmpdb\\binkey.bin");
	
	public static void main(String[] args) throws Exception {
		File indir = new File("D:\\v3syicore\\2010.03.22");
		
		DateFormat dateparse = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSS");
		
		long basetime = dateparse.parse("20-03-2010 00:00:00.000").getTime();
		
		if(binkeyfile.exists()) {
			System.err.println("reading binary key map file");
			EventDict.readKeyHash(binkeyfile);
		}
		
		
		File[] logfiles = indir.listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				String fn = pathname.getName();
				if(fn.endsWith("txt") && fn.startsWith("eb-") ) {
					return true;
				}
				return false;
			}
		});
		
		BufferedReader in;
		String line = null;
		
		int filecnt =0;
		int linecnt = 0;
		int usedlinecnt = 0;
		int seqcnt = 0;
		
		ArrayList<TTransaction> allxacts = new ArrayList<TTransaction>();
		
		
		
		for(File f: logfiles) {
			filecnt +=1;
			
			String[] fp = f.getName().split("_");
			String date = fp[1];
			//System.err.println("date= " + date);
			
			//in = new BufferedReader( new InputStreamReader( new GZIPInputStream(new FileInputStream(f))) );
			in = new BufferedReader (new FileReader( f ));
			line = in.readLine();
			
			Stack<TTransaction> xactstack = new Stack<TTransaction>();
			
			while(line!=null) {
				
				//System.err.println(line);
				
				linecnt +=1;
				if(!(line.startsWith("A") || line.startsWith("T") 
						|| line.startsWith("t") || line.startsWith("E")) ) {
					line = in.readLine();
					continue;
				}
				
				String[] ss = line.split("\t");
				
				if(ss.length>2) {
					usedlinecnt +=1;
					
					String strkey = ss[1]+" "+ss[2];
					int ind = EventDict.getIndex(strkey);
					
					//timestamp
					String tsstr =date+" " + ss[0].substring(1)+"0";
					long ts = dateparse.parse(tsstr).getTime();
					ts = (ts - basetime)/10;
					//System.err.println( tsstr +" "+ ts);
					
					// duration
					int dur = 0;
					if(ss.length>=5) {
						try {
							dur = Integer.parseInt(ss[4]);
						} catch (RuntimeException e) {
							dur = 0;
						}
					}
					
					// starting transaction ...
					if(ss[0].startsWith("t")) {
						TTransaction t = new TTransaction(ind, ts); // start of a new transaction
						xactstack.push(t);
					}
					
					// ending transaction ...
					else if(ss[0].startsWith("T")) {
						if(xactstack.size()>0) {
							TTransaction t = xactstack.pop();
							if(t.tid != ind) {
								System.err.println("in file " + f.getName());
								System.err.println("something wrong?? " + strkey +" " + ind +" does not match " + t.tid);
								System.err.println("the stack is ");
								System.err.println(t);
								while(xactstack.size()>0) {
									TTransaction ttt = xactstack.pop();
									System.err.println(ttt);
								}
							} else {
								int duration = Integer.parseInt( ss[4] );
								t.endTransaction(ts, duration);
								//System.err.println( t.toString() );
								allxacts.add(t);
								seqcnt +=1;
								if(xactstack.size()!=0) { // there is a container transaction..
									xactstack.peek().addToSeq(ind, ts, dur);
								}
							}
						} else {
							System.err.println("in file " + f.getName());
							System.err.println("ending transaction empty stack? " + ind + " " +strkey);
						}
					}
					
					else if(ss[0].startsWith("A")) {
						if(xactstack.size()!=0) {
							xactstack.peek().addToSeq(ind, ts, dur);
						}
					}
					
					else if(ss[0].startsWith("E")) {
						if(xactstack.size()!=0) {
							xactstack.peek().addToSeq(ind, ts, dur);
						}
					}
					
				}
				line = in.readLine();
			}
			
			in.close();
			
//			if(filecnt %11 == 10) {
//				Collections.sort(allxacts);
//				System.err.println("dumping partial results....");
//				PrintStream dataout = new PrintStream(new FileOutputStream(new File("c:/tmpdb/seq"+filecnt+".txt")));
//				for(TTransaction t: allxacts) {
//					dataout.println(t);
//				}
//				dataout.close();
//				allxacts.clear();
//			}
			
		} // for all files
		
		Collections.sort(allxacts);
		
		PrintStream dataout = new PrintStream(new FileOutputStream(new File("c:/tmpdb/seq"+filecnt+".txt")));
		for(TTransaction t: allxacts) {
			dataout.println(t);
		}
		dataout.close();
		
		PrintStream keyind = new PrintStream(new FileOutputStream( new File("c:/tmpdb/keyindex.txt") ));
		EventDict.writeToFile(keyind);
		EventDict.writeToBinFile(binkeyfile);
		System.err.println("file=" + filecnt +" lines=" +linecnt +" used=" +usedlinecnt +" seq=" +seqcnt + " num_key="+EventDict.map.size());
	}

}
