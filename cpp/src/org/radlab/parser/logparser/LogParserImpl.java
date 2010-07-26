package org.radlab.parser.logparser;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.radlab.parser.config.LogLineFormat;
import org.radlab.parser.config.ConfigUtil;
import org.radlab.parser.source.GlobalResultCollector;
import org.radlab.parser.source.index.LoggerCallIndexer;
import org.radlab.parser.source.index.TermFilter;

import utils.LogFormatter;
import utils.QueryUtils;


/** a simplified version of LogLineMatcherType1 */

public class LogParserImpl implements LogParser {
	
	private static Logger LOG = LogFormatter.getLogger(LogParserImpl.class);
	static {
		LOG.setLevel(Level.WARNING);
	}
	static final int NUM_RESULT_CONSIDERED = 30;
	static final boolean DO_HARD_MATCH = true;
	static final Pattern exceptionHead = Pattern.compile("([^\\s]+((Exception)|(Error))): (.*)");
	static final Pattern exceptionStack = Pattern.compile("(\\s+at (.+)\\(.*\\))|(\\s*Caused by:.*)|(\\s+\\.\\.\\. [0-9]* more)");
	static final Pattern exceptionPattern = Pattern.compile("(.+)Exception.*(\\s+at .*)+", Pattern.DOTALL | Pattern.MULTILINE);
	
	
	static final HashMap<String, Pattern> patterncache = new HashMap<String, Pattern>();
	
	static Random r = new Random();
	QueryParser qp;
	
	Pattern standardLogPattern;
	DateFormat tsformat;
	
	Pattern altLogPattern;
	DateFormat altTsFormat;
	
	int tsPos;
	int dataPos;
	int threadidPos;
	int levelPos;
	boolean levelInData;
	Searcher searcher;
	int srcLocPos;
	int threadidbase;
	
	int nomatchcnt = 0;
	int hardmatch_success_cnt = 0;
	int exception_count = 0;
	long line_cnt =0;
	
	public enum ParserStatus{SUCESS, FAILED, SKIPPED, TOTAL};
	
	// this is for map reduce job
//	public LogParserImpl(JobConf job) {
//		
//		LOG.info("Using mapreduce setup");
//		
//		this.tsformat = new SimpleDateFormat(job.getStrings(CONF_TSFORMAT)[0]);
//		this.standardLogPattern = Pattern.compile( job.getStrings(CONF_LOGPATTERN)[0] );
//		if(this.tsformat==null || this.standardLogPattern==null) {
//			throw new RuntimeException("ts format / log pattern not set!!");
//		}
//		
//		// open index files
//		try {
//		    IndexReader reader = IndexReader.open("index.zip");
//		    this.searcher = new IndexSearcher(reader);
//		} catch (IOException e) {
//			String[] files3 = new File("./tmp").list();
//			LOG.warning("working dir: " + Arrays.deepToString( new File(".").list() ));
//			LOG.warning("index dir: " + Arrays.deepToString(files3));
//			e.printStackTrace();
//			throw new RuntimeException("Lucene index load error!");
//		}
//		
//		this.dayPos = job.getInt(CONF_DAYPOS, -1);
//		this.timePos = job.getInt(CONF_TIMEPOS, -1);
//		this.dataPos = job.getInt(CONF_DATAPOS, -1);
//		this.threadidPos = job.getInt(CONF_THREADPOS, -1);
//		this.levelInData = job.getBoolean(CONF_LEVELINLOG, false);
//		Analyzer analyzer = new SimpleAnalyzer();
//		this.qp = new QueryParser("strindex", analyzer);
//	}
	
	
	public LogParserImpl(LogLineFormat format) {
	  this(format, null);
	}
	
	// this constructor is for stream
	public LogParserImpl(LogLineFormat format, LogLineFormat altFormat) {
		this.tsformat = new SimpleDateFormat(format.getTimeStampFormat());
		this.standardLogPattern = Pattern.compile(format.getLineRegExpr());
		if(this.tsformat==null || this.standardLogPattern==null) {
			throw new RuntimeException("ts format / log pattern not set!!");
		}
		
		if(altFormat !=null && !altFormat.getClass().equals(format.getClass())) {
		  this.altTsFormat = new SimpleDateFormat(altFormat.getTimeStampFormat());
		  this.altLogPattern = Pattern.compile(altFormat.getLineRegExpr());
		}
		
		// open index files
		File indexdir = new File( ConfigUtil.getSourceIndexDir() );
		try {
		    IndexReader reader = IndexReader.open( indexdir );
		    this.searcher = new IndexSearcher(reader);
		    LOG.info("successfully loaded index from " + indexdir.getAbsolutePath() );
		} catch (IOException e) {
			throw new RuntimeException("Lucene index load failed from " + indexdir.getAbsolutePath() );
		}
		
		this.tsPos = format.getTimeStampPos();
		this.dataPos = format.getMessagePos();
		this.threadidPos = format.getThreadIdPos();
		this.levelInData = format.isLevelInData();
		this.levelPos = format.getLevelPos();
		this.srcLocPos = format.getSourceLocationPos();
		Analyzer analyzer = new SimpleAnalyzer();
		this.threadidbase = format.getThreadIdIntBase();
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
    		buf.setSrclocation(m.group(this.srcLocPos));
			try {
			    String tsstr = m.group(this.tsPos);
			    String prefix = "10";   // this is a hack...
			    if(tsstr.startsWith("1")) {
			      prefix = "09";
			    }
				ts = tsformat.parse( prefix+m.group(this.tsPos));
				// LOG.info("TS STR:: " + m.group(dayPos) + " " +
				// m.group(timePos) +" ts=" +ts.getTime());
			} catch (java.text.ParseException e) {
				System.err.println("failed parsing date " + m.group(this.tsPos)+":"+body );
				ts = new Date();
			}
			if (threadidPos > 0) {
	        	// LOG.info("threadidPos::" + threadidPos);
	        	threadid = Integer.parseInt( m.group(threadidPos), this.threadidbase);
	        	// LOG.info("threadid::" +threadid);
	        } else {
	        	threadid = -1;
	        }
        } else{
          
            // try alt pattern
            m = altLogPattern.matcher(line);
            if(m.find()) {
              body = m.group(dataPos);
              buf.setSrclocation(m.group(this.srcLocPos));
              try {
                String tsstr = m.group(this.tsPos);
                String prefix = "10";   // this is a hack...
                if(tsstr.startsWith("1")) {
                  prefix = "09";
                }
                ts = altTsFormat.parse(prefix+m.group(this.tsPos));
              } catch (java.text.ParseException e) {
                  System.err.println("failed parsing date " + m.group(this.tsPos)+":"+body );
                  ts = new Date();
              }
              if (threadidPos > 0) {
                  // LOG.info("threadidPos::" + threadidPos);
                  threadid = Integer.parseInt( m.group(threadidPos), this.threadidbase);
                  // LOG.info("threadid::" +threadid);
              } else {
                  threadid = -1;
              }
            } else {
              body = line;
              // leave thread id /ts as it was ..
              ts = new Date(buf.ts);
              //tLOG.warning("TS::" +ts.getTime());
              threadid = buf.threadid;
              correctformat = false;
            }
        }
		
        buf.ts = ts.getTime();
        buf.threadid = threadid;
        
        
        
		// looking into the free text
		boolean success =false;
        
		if (!correctformat) {
			if (exceptionStack.matcher(body).matches() 
					|| exceptionHead.matcher(body).matches()
					|| exceptionPattern.matcher(body).matches() ) {
				exception_count +=1;
				if (reporter!=null)
					reporter.incrCounter(ParserStatus.SKIPPED, 1);
			}
			return false;
		}
		if(body.trim().length()==0) {
		  buf.logid = 99999999;  // empty log
		  return true;
		}
		try {
			success = indexLookup(body, buf, reporter); // buf updated in procedure
		} catch (Exception e) {
		    LOG.warning("received exception on line : "+ line);
			e.printStackTrace();
			return false;
		}
		if(!success) {
		  LOG.warning("Parsing Line failed: " + line +"$");
		}

		return success;
	}
	
	
private boolean indexLookup(String body, ParsedMessageWritable buf, Reporter reporter ) throws Exception{
		
 
		//String processedbody = QueryUtils.removeLongWords( body.trim() );
		//processedbody = processedbody.replace("_", " ").replace("(", " ").replace(")", " ").replace(".", " ");
		
        //processedbody = processedbody.replaceAll("[^a-zA-Z][0-9]+", "");//;
		//LOG.fine(processedbody);
        //body = body.trim();
  
        // Note:: remove numbers must happen before term filtering...
        String processedbody = QueryUtils.removePaths(body);
        processedbody = processedbody.replaceAll("[0-9]+", " ").replaceAll("\\s+", " ");//.replaceAll("\\s[a-zA-Z]\\s", " ");
        processedbody = GlobalResultCollector.terms.filter(processedbody);
        //LOG.fine("after term filtering ... " + processedbody );
        processedbody = QueryUtils.escapeQueryString(processedbody);
        if (processedbody.length()==0) {
        	if (reporter!=null)
        		reporter.incrCounter(ParserStatus.SKIPPED, 1);
        	LOG.fine("zero length after processing... " + body);
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
        	String typeidstr = hit.get("seq");
        	Pattern p  = patterncache.get(typeidstr);
        	if (p ==null) {
        	  try {
        	    p= Pattern.compile(regexprstr);
        	    patterncache.put(typeidstr, p);
        	  } catch (Exception ex) {
        	    LOG.warning("exception parsing pattern.." + regexprstr);
        	    LOG.warning("pattern seq is " +hit.get("seq") );
        	    //ex.printStackTrace();
        	    patterncache.put(typeidstr, Pattern.compile("PARSING REGEXPR FAILED"));
        	    continue;
        	  }
        	}
        	LOG.finest("matching ^" + body +"$ with ^" + regexprstr +"$");
        	mat = p.matcher(body);
        	hardmatch = mat.matches();
        	
        	if (hardmatch) {
				String logidstr = hitclass + "-" + hitline;
				ArrayList<String> lbs = new ArrayList<String>();
				ArrayList<String> dts = new ArrayList<String>();
				StringBuffer lbsn = new StringBuffer();
				StringBuffer dtsn = new StringBuffer();
				
				String[] nameMap = hit.get("name").split(";");
				String[] typeMap = hit.get("field").split(";");
				
				
				//heuristic to determine if variable if a number of label
				for(int j=0; j< Math.min(nameMap.length,mat.groupCount()) ; j++) { 
				  // avoid array out of bound exception where parsing result is not accurate..
					if (nameMap[j].length()==0) {
						continue;
					}
					String nmap = nameMap[j].toLowerCase();
					if (nmap.endsWith("id") || nmap.endsWith("name")) {
						lbs.add( mat.group(j+1) );
						lbsn.append( nameMap[j] ).append(";;");
					} else if (QueryUtils.isNumeric(typeMap[j])) {
						dts.add( mat.group(j+1) );
						dtsn.append( nameMap[j] ).append(";;");
					} else {
						lbs.add( mat.group(j+1) );
						lbsn.append( nameMap[j] ).append(";;");
					}
					if ( LOG.isLoggable(Level.FINE) ) {
						LOG.fine(nameMap[j] +" - " +typeMap[j] +" - " + mat.group(j+1));
					}
				}
				
				buf.logid = Integer.parseInt(typeidstr);
				buf.setLabels(lbs.toArray(new String[lbs.size()]) );
				buf.setNumbers(dts.toArray(new String[dts.size()]));
				buf.setDtsnames(dtsn.toString());
				buf.setLbsnames(lbsn.toString());
				
				if ( i>=1 && LOG.isLoggable(Level.FINE) ) {
					LOG.fine("Matched at " + i+" result QUERY " + line_query.toString());
				}
				if(reporter!=null)
					reporter.incrCounter(ParserStatus.SUCESS, 1);
				hardmatch_success_cnt += 1;
				break;
				
			} else { // if not hard match..
				// continue to next round..
				buf.setLabels(ParsedMessageWritable.EMPTY);
				buf.setDtsnames("");
				buf.setLbsnames("");
				buf.setNumbers(ParsedMessageWritable.EMPTY);
				buf.logid=-1;
			}
        }
        
        if ( i>=NUM_RESULT_CONSIDERED || i>=hits.length() ) {
        	if(LOG.isLoggable(Level.FINE)) {
        	  LOG.fine("***HARD MATCH FAILED,, looked at " + i +" results");
        	  for (i=0; i< Math.min(hits.length(), NUM_RESULT_CONSIDERED); i++) {
        	    LOG.fine("     hit"+i+hits.doc(i).get("regexpr"));
        	  }
        	}
	        // non_match.add(body+" "+ line_query.toString() );
	        // hitcnt[5] +=1;
        	if (reporter!=null)
        		reporter.incrCounter(ParserStatus.FAILED, 1);
        	nomatchcnt +=1;
        	buf.setLabels(ParsedMessageWritable.EMPTY);
        	buf.setNumbers(ParsedMessageWritable.EMPTY);
        	buf.setDtsnames("");
			buf.setLbsnames("");
        	buf.logid = -1;
        	LOG.fine("parsing line failed.. processed body is: " + processedbody +" hit len=" + hits.length() +" i="+i);
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

