package scale;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.hadoop.mapred.Reporter;

public class MSR_Log_Parse implements LogParser{
	

	static final Pattern linepattern = Pattern.compile("\\s*([0-9]+\\.[0-9]+):\\s*([0-9]+):\\s*([^ ]+)\\s*([0-9]+) (([^:]*):? (.*))?");
	static final int TS=1;
	static final int THREAD=2;
	static final int FN=3;
	static final int LN=4;
	static final int TYPE=6;
	static final int DATA=7;
	
	static final Pattern guidpattern = Pattern.compile( "^(\\{{0,1}([0-9a-fA-F]){8}-([0-9a-fA-F]){4}-([0-9a-fA-F]){4}-([0-9a-fA-F]){4}-([0-9a-fA-F]){12}\\}{0,1})$" );
	
	static final Pattern pattern_executing = Pattern.compile("(.*) executing (.*)\\(.*\\)");
	static final Pattern pattern_queueing = Pattern.compile("(.*): queueing (.*)\\(.*\\)");
	static final Pattern pattern_mt_executing = Pattern.compile("\\(MT\\) (.*): Executing (.*)\\(.*\\)");
	static final Pattern pattern_cancel = Pattern.compile("(.*): Canceling routine (.*) from state (.*) \\((.*)\\)");
	static final Pattern pattern_cheapevent = Pattern.compile("([^ ]+ [^ ]+): (.*)");
	
	
	Hashtable<String, Integer> logtypes = new Hashtable<String, Integer>();
	
	long line_cnt=0;
	
	public MSR_Log_Parse() {
		logtypes.put("srsobjectprovider.cpp1435",0);
		logtypes.put("srsobjectprovider.cpp836",1);
		logtypes.put("srsobjectprovider.cpp2061",2);
		logtypes.put("srsobjectprovider.cpp2460",3);
		logtypes.put("srsobjectprovider.cpp2257",4);
		logtypes.put("srsobjectprovider.cpp2263",5);
		logtypes.put("srsobjectprovider.cpp1570",6);
		logtypes.put("srsobjectprovider.cpp2377",7);
		logtypes.put("srsobjectprovider.cpp2383",8);
		logtypes.put("srsobjectprovider.cpp2497",9);
		logtypes.put("fsgmessage.cpp1060",10);
		logtypes.put("srsobjectprovider.cpp1950",11);
		logtypes.put("fsgmanager.cpp662",12);
		logtypes.put("fsgmanager.cpp671",13);
		logtypes.put("fsgmanager.cpp675",14);
	}
	
	public static void main(String[] args) throws Exception{
		
		//File logf = new File("C:/Users/xuw/Desktop/msrlog/ClientMarkPoints.txt");
		File logf = new File("C:/Users/xuw/Desktop/msrlog/Server0MarkPoints.txt");
		BufferedReader in = new BufferedReader(new FileReader(logf));
		
		String line = in.readLine();
		
		MSR_Log_Parse parser = new MSR_Log_Parse();
		ParsedMessageWritable ret = new ParsedMessageWritable();
		
		while (line!=null) {
			boolean p = parser.parseOneLine(line, ret, null);
			if (p) {
				//System.err.println(ret.getLbsnames() +" " + ret.getLabel());
			} else {
				System.err.println(line);
			}
			line = in.readLine();
		}
		
	}
	
	public long getLineCnt() {
		return this.line_cnt;
	}
	
	public boolean parseOneLine(String line, ParsedMessageWritable ret, Reporter reporter) {
		line_cnt +=1;
		Matcher m = linepattern.matcher(line);
		if (m.matches()) {
			//System.err.println(m.group(TYPE) + " / " + m.group(DATA));
			double ts = Double.parseDouble(m.group(TS));
			long tslong = (long) (ts*1000);
			
			if (m.group(TYPE)==null) {
				ret.setLabels(m.group(FN)+m.group(LN));
				ret.setLbsnames("empty-pos");
				//System.err.println(m.group(FN));
				ret.logid = 0;
				ret.ts = tslong;
				return true;
			}
			
			if (m.group(TYPE)!=null && m.group(TYPE).equals("string")){
				if (m.group(DATA) !=null){
					Matcher e = pattern_executing.matcher(m.group(DATA));
					if (e.matches()) {
						// 1 = queue name
						// 2 = routine name
						//System.err.println(e.group(1)); 
						
						ret.setLabels(e.group(1)+";;"+e.group(2));
						ret.setLbsnames("exec_queue;;exec_rtn");
						ret.logid = 0;
						ret.ts = tslong;
						return true;
					}
					
					e = pattern_queueing.matcher(m.group(DATA));
					if (e.matches() ) {
						ret.setLabels(e.group(1)+";;"+e.group(2));
						ret.setLbsnames("queue_queue;;queue_rtn");
						ret.logid = 0;
						ret.ts = tslong;
						return true;
					}
					
					e = pattern_mt_executing.matcher(m.group(DATA));
					if (e.matches() ) {
						ret.setLabels(e.group(1)+";;"+e.group(2));
						ret.setLbsnames("mt_queue;;mt_rtn");
						ret.logid = 0;
						ret.ts = tslong;
						return true;
					}
					
					e = pattern_cancel.matcher(m.group(DATA));
					if (e.matches() ) {
						ret.setLabels(e.group(1)+";;"+e.group(2));
						ret.setLbsnames("cancel_queue;;cancel_rtn");
						ret.logid = 0;
						ret.ts = tslong;
						return true;
					}
					
					if (m.group(FN).equals("fmessage.cpp")) {
						ret.setLabels(m.group(DATA));
						ret.setLbsnames("machine");
						ret.logid = 0;
						ret.ts = tslong;
						return true;
					}
					
					if (m.group(FN).equals("cheapevent.cpp")) {
						e = pattern_cheapevent.matcher(m.group(DATA));
						if (e.matches()) {
							ret.setLabels( e.group(1)+";;"+ m.group(LN) );
							ret.setLbsnames("event-name;;event-status");
							ret.logid = 0;
							ret.ts = tslong;
							//System.err.println(ret.getLabel());
							return true;
						} else {
							//System.err.println("numatch??");
						}
					}
				}
			}
			
			if (m.group(TYPE)!=null && m.group(TYPE).equals("integer")) {
				if (m.group(DATA) !=null) {
					ret.setNumbers(m.group(DATA));
					ret.setDtsnames("int-" + m.group(FN)+m.group(LN));
					ret.logid = 0;
					ret.ts = tslong;
					return true;
				}
			}
			
			if (m.group(TYPE)!=null && m.group(TYPE).equals("64-bit integer")) {
				if (m.group(DATA) !=null) {
					ret.setNumbers(m.group(DATA));
					ret.setDtsnames("long-" + m.group(FN)+m.group(LN));
					ret.logid = 0;
					ret.ts = tslong;
					return true;
				}
			}
			
			if(m.group(TYPE)!=null && m.group(TYPE).equals("ptr")) {
				
				ret.setLabels(m.group(DATA));
				ret.setLbsnames("ptr-"+m.group(FN)+m.group(LN));
				ret.logid =0;  // don't care about log id
				ret.ts = tslong;
				return true;
			}
			
			if(m.group(DATA)!=null) {
				Matcher gm = guidpattern.matcher(m.group(DATA));
				
				if (gm.matches()) {
					//System.err.println(m.group(DATA));
					ret.setLabels(m.group(DATA));
					ret.setLbsnames("guid-" + m.group(FN)+m.group(LN));
					
					String loc = m.group(FN)+m.group(LN);
					Integer logid = logtypes.get(loc);
					if(logid==null) {
						logid = logtypes.size();
						logtypes.put(loc, logid);
						throw new RuntimeException("cannot find loc " + loc +" " + line);
						//System.err.println(loc);
					} 
					
					ret.logid = logid;
					ret.ts = tslong;
					//ret.threadid = Integer.parseInt( m.group(THREAD) );
					
					//System.err.println(ret.ts +" " + ret.logid + " "+ ret.getLabel());
					return true;
				}
			}
			return false;
			
		} else {
			System.err.println("no match " + line);
			return false;
		}
	}

	
}
