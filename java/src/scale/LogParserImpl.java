package scale;

import index.IndexConstants;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.Reporter;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Sort;

import conf.ConfUtil;

import utils.LogFormatter;
import utils.QueryUtils;
import utils.StringUtils;


/** a simplified version of LogLineMatcherType1 */

public class LogParserImpl implements LogParser, JobConfigStringConstants {
	
	private static Logger LOG = LogFormatter.getLogger(LogParserImpl.class);
	static {
		LOG.setLevel(Level.OFF);
	}
	static final int NUM_RESULT_CONSIDERED = 8;
	static final boolean DO_HARD_MATCH = true;
	static final Pattern exceptionHead = Pattern.compile("([^\\s]+((Exception)|(Error))): (.*)");
	static final Pattern exceptionStack = Pattern.compile("(\\s+at (.+)\\(.*\\))|(\\s*Caused by:.*)|(\\s+\\.\\.\\. [0-9]* more)");
	static final Pattern exceptionPattern = Pattern.compile("(.+)Exception.*(\\s+at .*)+", Pattern.DOTALL | Pattern.MULTILINE);
	
	
	static Random r = new Random();
	QueryParser qp;
	
	Pattern standardLogPattern;
	DateFormat tsformat;
	int dayPos;
	int timePos;
	int dataPos;
	int threadidPos;
	boolean levelInData;
	Searcher searcher;
	
	int nomatchcnt = 0;
	int hardmatch_success_cnt = 0;
	int exception_count = 0;
	long line_cnt =0;
	
	public enum ParserStatus{SUCESS, FAILED, SKIPPED, TOTAL};
	
	// this is for map reduce job
	public LogParserImpl(JobConf job) {
		
		LOG.info("Using mapreduce setup");
		
		this.tsformat = new SimpleDateFormat(job.getStrings(CONF_TSFORMAT)[0]);
		this.standardLogPattern = Pattern.compile( job.getStrings(CONF_LOGPATTERN)[0] );
		if(this.tsformat==null || this.standardLogPattern==null) {
			throw new RuntimeException("ts format / log pattern not set!!");
		}
		
		// open index files
		try {
		    IndexReader reader = IndexReader.open("index.zip");
		    this.searcher = new IndexSearcher(reader);
		} catch (IOException e) {
			String[] files3 = new File("./tmp").list();
			LOG.warning("working dir: " + Arrays.deepToString( new File(".").list() ));
			LOG.warning("index dir: " + Arrays.deepToString(files3));
			e.printStackTrace();
			throw new RuntimeException("Lucene index load error!");
		}
		
		this.dayPos = job.getInt(CONF_DAYPOS, -1);
		this.timePos = job.getInt(CONF_TIMEPOS, -1);
		this.dataPos = job.getInt(CONF_DATAPOS, -1);
		this.threadidPos = job.getInt(CONF_THREADPOS, -1);
		this.levelInData = job.getBoolean(CONF_LEVELINLOG, false);
		Analyzer analyzer = new SimpleAnalyzer();
		this.qp = new QueryParser("strindex", analyzer);
	}
	
	// this constructor is for stream
	public LogParserImpl() {
		int logtype = IndexConstants.LOGTYPE;
		this.tsformat = new SimpleDateFormat(ConfUtil.getConfig().getStringArray("tsformat")[logtype]);
		this.standardLogPattern = Pattern.compile(IndexConstants.standardLogPatternStr);
		if(this.tsformat==null || this.standardLogPattern==null) {
			throw new RuntimeException("ts format / log pattern not set!!");
		}
		
		// open index files
		try {
		    IndexReader reader = IndexReader.open(IndexConstants.SRC_INDEX_DIR);
		    this.searcher = new IndexSearcher(reader);
		    LOG.info("successfully loaded index from " + IndexConstants.SRC_INDEX_DIR);
		} catch (IOException e) {
			throw new RuntimeException("Lucene index load failed from " + IndexConstants.SRC_INDEX_DIR);
		}
		
		this.dayPos = IndexConstants.dayPos;
		this.timePos = IndexConstants.timePos;
		this.dataPos = IndexConstants.dataPos;
		this.threadidPos = IndexConstants.threadidPos;
		this.levelInData = IndexConstants.levelInData;
		Analyzer analyzer = new SimpleAnalyzer();
		this.qp = new QueryParser("strindex", analyzer);
	}
	
	public boolean parseOneLine( String line, ParsedMessageWritable buf, Reporter reporter ) {
		
		if (line==null)
			return false;
		
		Date ts;
		int threadid = -1;
		boolean correctformat = true;
		
		this.line_cnt += 1;
		if (reporter!=null)
			reporter.incrCounter(ParserStatus.TOTAL, 1);
		
		// first parse structured part
		Matcher m = standardLogPattern.matcher(line);
        String body;
        if(m.find()) {
    		body = m.group(dataPos);
			try {
				ts = tsformat.parse(m.group(dayPos) + " " + m.group(timePos));
				// LOG.info("TS STR:: " + m.group(dayPos) + " " +
				// m.group(timePos) +" ts=" +ts.getTime());
			} catch (java.text.ParseException e) {
				System.err.println("failed to parse date " + m.group(dayPos) +":" + m.group(timePos)+":"+body );
				ts = new Date();
			}
			if (threadidPos > 0) {
	        	// LOG.info("threadidPos::" + threadidPos);
	        	threadid = Integer.parseInt( m.group(threadidPos) );
	        	// LOG.info("threadid::" +threadid);
	        } else {
	        	threadid = -1;
	        }
        } else{
        	body = line;
        	// leave thread id /ts as it was ..
        	ts = new Date(buf.ts);
        	threadid = buf.threadid;
        	correctformat = false;
        }
		
        buf.ts = ts.getTime();
        buf.threadid = threadid;
        
        
		// looking into the free text
		boolean success =false;
        
		if (!correctformat) {
			if (exceptionStack.matcher(body).find() 
					|| exceptionHead.matcher(body).find()
					|| exceptionPattern.matcher(body).find() ) {
				exception_count +=1;
				if (reporter!=null)
					reporter.incrCounter(ParserStatus.SKIPPED, 1);
				return false;
			}
		}
		try {
			success = indexLookup(body, buf, reporter); // buf updated in procedure
		} catch (Exception e) {
			e.printStackTrace();
		}

		return success;
	}
	
	
private boolean indexLookup(String body, ParsedMessageWritable buf, Reporter reporter ) throws Exception{
		String processedbody = body.replace("_", " ");
		processedbody = StringUtils.removeLongWords( processedbody.trim() );
		processedbody = processedbody.replaceAll("[^a-zA-Z][0-9]+", "");//;
		//LOG.fine(processedbody);
        processedbody = QueryUtils.escapeQueryString(processedbody);
        if (processedbody.length()==0) {
        	if (reporter!=null)
        		reporter.incrCounter(ParserStatus.SKIPPED, 1);
        	return false;
        }
        
        if(LOG.isLoggable(Level.FINE)) {
        	LOG.fine("Processing Log::" + body  +" " + body.length());
        	LOG.fine("after escape:: " + processedbody);
        }
        
		Query line_query=null;
		try {
			line_query = qp.parse(processedbody);
		} catch (ParseException e) {
			LOG.warning("parsing failed on: " + body );
			LOG.warning(" after escape is: " + processedbody);
			//e.printStackTrace();
			return false;
		}
        
		if (LOG.isLoggable(Level.FINE)){
			LOG.fine("query used::" + line_query);
		}
		
        Hits hits = searcher.search(line_query, Sort.RELEVANCE);
        
        if ( hits.length()==0 ) {
        	LOG.warning("NO MATCH FOUND: " + body);
        	nomatchcnt +=1;
        	if (reporter!=null)
        		reporter.incrCounter(ParserStatus.FAILED, 1);
        	return false;
        }
        int i;
        
        String hitclass;
        String hitline;
        for (i=0; i< Math.min(hits.length(), NUM_RESULT_CONSIDERED); i++) {
        	Document hit = hits.doc(i);
        	hitclass = hit.get("classname");
        	hitline = hit.get("line");
        	
        	if (LOG.isLoggable(Level.FINEST)) {
        		LOG.finest("R"+i+":" + hitclass
        			+":"+ hitline + " "+ hit.get("regexpr") +" SCORE:" + hits.score(i));
        	}
        	
        	// test if really match
        	Matcher mat = null;
        	boolean hardmatch = false;
        	String regexprstr = hit.getField("regexpr").stringValue();
        	Pattern p = Pattern.compile(regexprstr);
        	mat = p.matcher(body);
        	hardmatch = mat.matches();
        	
        	if (hardmatch) {
				String logidstr = hitclass + "-" + hitline;
				StringBuffer lbs = new StringBuffer();
				StringBuffer dts = new StringBuffer();
				StringBuffer lbsn = new StringBuffer();
				StringBuffer dtsn = new StringBuffer();
				StringBuffer lbst = new StringBuffer();
				StringBuffer dtst = new StringBuffer();
				
				String[] nameMap = hit.get("name").split(";");
				String[] typeMap = hit.get("field").split(";");
				String typeidstr = hit.get("seq");
				
				//heuristic to determine if variable if a number of label
				for(int j=0; j<nameMap.length; j++) { 
					if (nameMap[j].length()==0) {
						continue;
					}
					String nmap = nameMap[j].toLowerCase();
					if (nmap.endsWith("id") || nmap.endsWith("name")) {
						lbs.append( mat.group(j+1) ).append(";;");
						lbsn.append( nameMap[j] ).append(";;");
						lbst.append( typeMap[j] ).append(";;");
					} else if (StringUtils.isNumeric(typeMap[j])) {
						dts.append( mat.group(j+1) ).append(";;");
						dtsn.append( nameMap[j] ).append(";;");
						dtst.append( typeMap[j] ).append(";;");
					} else {
						lbs.append( mat.group(j+1) ).append(";;");
						lbsn.append( nameMap[j] ).append(";;");
						lbst.append( typeMap[j] ).append(";;");
					}
					if ( LOG.isLoggable(Level.FINE) ) {
						LOG.fine(nameMap[j] +" - " +typeMap[j] +" - " + mat.group(j+1));
					}
				}
				
				buf.logid = Integer.parseInt(typeidstr);
				buf.setLabels(lbs.toString());
				buf.setNumbers(dts.toString());
				buf.setDtsnames(dtsn.toString());
				buf.setLbsnames(lbsn.toString());
				buf.setDtstypes(dtst.toString());
				buf.setLbstypes(lbst.toString());
				String methodid;
				try {
					methodid = hit.get("methodid");
				} catch (RuntimeException e) {  // handles older indices
					methodid = "";
				}
				buf.setMethodid(methodid==null?"":methodid);
				
				if ( i>=1 && LOG.isLoggable(Level.FINE) ) {
					LOG.fine("Matched at " + i+" result QUERY " + line_query.toString());
				}
				if(reporter!=null)
					reporter.incrCounter(ParserStatus.SUCESS, 1);
				hardmatch_success_cnt += 1;
				break;
				
			} else { // if not hard match..
				// continue to next round..
				buf.setLabels("");
				buf.setDtsnames("");
				buf.setLbsnames("");
				buf.setNumbers("");
				buf.logid=-1;
				buf.setMethodid("");
			}
        }
        
        if ( i>=NUM_RESULT_CONSIDERED ) {
        	LOG.warning("***HARD MATCH FAILED " + body);
	        // non_match.add(body+" "+ line_query.toString() );
	        // hitcnt[5] +=1;
        	if (reporter!=null)
        		reporter.incrCounter(ParserStatus.FAILED, 1);
        	nomatchcnt +=1;
        	buf.setLabels("");
        	buf.setNumbers("");
        	buf.setDtsnames("");
			buf.setLbsnames("");
        	buf.logid = -1;
        	buf.setMethodid("");
        	return false;
        }
        
        return true;
	}

public long getLineCnt() {
	return line_cnt;
}
	

//	private boolean detectExceptionTraces(String body, ParsedMessageWritable buf) {
//		
//		// see if it is an exception stack dump
//    	//LOG.fine("trying to match exception!!! " + line);
//    	Matcher exmat = exceptionHead.matcher(body);
//    	if (exmat.find()) {  // is exception
//    		LOG.fine("Is exception!!");
//    		StringBuffer sbuf = new StringBuffer();
//    		
//			String expname = exmat.group(1);
//			if (expname==null) {
//				LOG.warning("Parsing Exception Name Failed.. " + line);
//				expname = "UNPARSED EXCEPTION";
//			}
//			expname = expname.substring(0, Math.min(expname.length(), 240));
//			insertlogline.setString(3, "EXCEPTION: " + expname);
//			sbuf.append(line).append("\\\\");
//			line = in.readLine();
//			if (line==null){
//				break;
//			}
//        	Matcher atmat = exceptionStack.matcher(line);
//        	
//        	StringBuffer stacksb = new StringBuffer();
//        	while( atmat.matches() ) {
//        		LOG.fine("skipped except stack" +line);
//        		stacksb.append(line).append("\\\\");
//        		line = in.readLine();
//        		if (line==null)
//        			break;
//        		atmat = exceptionStack.matcher(line);
//        	}
//        	String stackline = sbuf.append(stacksb).toString();
//        	if (stackline.length() > 1000) {
//        		stackline = stackline.substring(0,1000);
//        	}
//        	insertlogline.setString(4, exmat.group(0));
//        	insertlogline.setString(5, stackline);
//        	insertlogline.setString(6, "message = " + exmat.group(5));
//			insertlogline.setString(7, "");
//        	//LOG.info("Inserting exception... " + insertlogline) ;
//        	doInsert(insertlogline);
//        	continue;
//    	} else {  // is not exception
//    		//LOG.warning("Not a valid log format:: " +line);
//    		
//    		StringBuffer sbuf = new StringBuffer();
//    		sbuf.append(body);
//    		line = in.readLine();
//    		if (line==null) {
//    			break;
//    		}
//        	Matcher atmat = exceptionStack.matcher(line);
//    		while( atmat.matches() ) {
//        		LOG.fine("skipped at stack" +line);
//        		sbuf.append("\n").append(line);
//        		line = in.readLine();
//        		if (line==null)
//        			break;
//        		atmat = exceptionStack.matcher(line);
//        	}
//    		
//    		if (!IndexConstants.keepUnMatched) {
//    			//LOG.info("not saved: " +line);
//    			continue;
//    		}
//        	continue;
//    	}
//		
//	}
	
}

