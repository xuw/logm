package index;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.document.Document;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;

import utils.DbUtils;
import utils.QueryUtils;
import utils.StringUtils;

public class LogLineMatcherType1 extends LogLineMatcher {
	
	Connection conn = DbUtils.getConn();
    PreparedStatement insertlogline;
	
    int cnt =0;
    int nomatchcnt =0;
    Integer[] hitcnt = new Integer[6];
    ArrayList<String> non_match = new ArrayList<String>();
    int hardmatch_success_cnt = 0;
    
    
	private boolean doSearch(String body, Date ts, String line, int threadid) throws Exception{
		
		String processedbody = body.trim().replaceAll("[^a-zA-Z][0-9]+", "").replace("_", " ");
        processedbody = QueryUtils.escapeQueryString(processedbody);
        //processedbody = termfilter.filter(processedbody);
		LOG.fine("Processing Log::" + line  +" " + body.length());
        LOG.fine("after escape:: " + processedbody);
		
		Query line_query=null;
		try {
			QueryParser qp = new QueryParser("strindex", analyzer);
			line_query = qp.parse(processedbody);
		} catch (ParseException e) {
			LOG.warning("parsing failed on: " + body );
			LOG.warning(" after escape is: " + processedbody);
			e.printStackTrace();
		}
        
        LOG.fine("query used::" + line_query);
        Hits hits = searcher.search(line_query, Sort.RELEVANCE);
        cnt +=1;
        if ( hits.length()==0 ) {
        	LOG.warning("NOT FOUND: " + body);
        	nomatchcnt +=1;
        	return false;
        }
        int i;
        
        String hitclass;
        String hitline;
        for (i=0; i< Math.min(hits.length(), NUM_RESULT_CONSIDERED); i++) {
        	Document hit = hits.doc(i);
        	hitclass = hit.get("classname");
        	hitline = hit.get("line");
        	LOG.finest("R"+i+":" + hitclass
        			+":"+ hitline + " "+ hit.get("regexpr") +" SCORE:" + hits.score(i));
        	
        	// test if really match
        	Matcher mat = null;
        	boolean hardmatch = false;
        	if (DO_HARD_MATCH) {
        		String regexprstr = hit.getField("regexpr").stringValue();
        		Pattern p = Pattern.compile(regexprstr);
        		mat = p.matcher(body);
        		hardmatch = mat.matches();
        		if (!hardmatch) {
        			//LOG.info("Hardmatch fail - " + regexprstr);
        			//LOG.info("                 " + body);
        		}
        	}
        	if (!DO_HARD_MATCH || hardmatch) {
        		insertlogline.setString(1, dbtsformat.format(ts));
				insertlogline.setInt(2, threadid);
				String logidstr = hitclass + "-" + hitline;
				insertlogline.setString(3, logidstr);
				insertlogline.setString(4, StringUtils.limitStrLen( body, 999 ));
				insertlogline.setString(5, line);
				StringBuffer lbs = new StringBuffer();
				StringBuffer dts = new StringBuffer();
				if (hardmatch) {
					String[] nameMap = hit.get("name").split(";");
					String[] typeMap = hit.get("field").split(";");

					for(int j=0; j<nameMap.length; j++) { 
						if (nameMap[j].length()==0) {
							continue;
						}
						if (nameMap[j].toLowerCase().endsWith("id") || nameMap[j].toLowerCase().endsWith("name")) {
							LOG.fine("LBN ");
							lbs.append(nameMap[j]+" = "+ mat.group(j+1) ).append("\n");
						} else if (StringUtils.isNumeric(typeMap[j])) {
							LOG.fine("DT ");
							dts.append(nameMap[j]+" = "+ mat.group(j+1) ).append("\n");
						} else {
							LOG.fine("LB ");
							lbs.append(nameMap[j]+" = "+ mat.group(j+1) ).append("\n");
						}
						LOG.fine(nameMap[j] +" - " +typeMap[j] +" - " + mat.group(j+1));
					}
					//LOG.fine(lbs.toString());
					//LOG.fine(dts.toString());
					
					insertlogline.setString(6, StringUtils.limitStrLen(lbs.toString(), 999) );
					insertlogline.setString(7, StringUtils.limitStrLen( dts.toString(), 999) );
				}
				
				if (indexwriter!=null) // write a log index
					indexwriter.addToIndex(ts, null, body, (lbs.toString()+dts.toString()).split("\n"), logidstr, "");
				
				doInsert(insertlogline);
				if (hardmatch && i>=1) {
					LOG.fine("Matched at " + i+" result QUERY " + line_query.toString());
				}
				hardmatch_success_cnt += 1;
				break;
        	}
        }
        if ( i>=5 ) {
        	LOG.warning("***NO MATCH " + body);
        	non_match.add(body+" "+ line_query.toString() );
        	hitcnt[5] +=1;
        	return false;
        }
        return true;
	}
	
	
	public void search(File logfile, String dbname) throws Exception{
		BufferedReader in = null;
	    if (logfile.exists()) {
	      in = new BufferedReader(new FileReader(logfile));
	    } else {
	    	LOG.warning("Cannot find input file,, using standard in..");
	      in = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
	    }
	    
	    
	    insertlogline = conn.prepareCall("insert into " + dbname + " (ts, threadid, logid, logbody ,textentry, lbs, dts) values(?,?,?,?,?,?,?)");
	    
	    for (int i=0; i<6; i++) {
	    	hitcnt[i] =0;
	    }
	    
	    
	    String line = in.readLine();
	    Date ts = new Date(0);
	    int threadid =-1;
	    int processedcnt =0;
	    while (true) {
	    	
	        if (line == null || line.length() == -1)
	          break;

	        //line = line.trim();
	        if (line.length()==0) {
	        	line = in.readLine();
	        	continue;
	        }
	        processedcnt +=1;
	        if (processedcnt%10000==0) {
	        	LOG.info("processed " + processedcnt +" lines");
	        }
	        
	        Matcher m = standardLogPattern.matcher(line);
	        String body;
	        if(m.find()) {
        		body = m.group(dataPos);
				try {
					ts = tsformat.parse(m.group(dayPos) + " " + m.group(timePos));
					//LOG.info("TS STR:: " + m.group(dayPos) + " " + m.group(timePos) +" ts=" +ts.getTime());
				} catch (java.text.ParseException e) {
					LOG.warning("failed to parse date " + m.group(dayPos) +":" + m.group(timePos)+":"+body );
				}
				if (threadidPos > 0) {
		        	//LOG.info("threadidPos::" + threadidPos);
		        	threadid = Integer.parseInt( m.group(threadidPos) );
		        	//LOG.info("threadid::" +threadid);
		        }
	        } else{
	        	body = line;
	        	// leave thread id /ts as it was ..
	        }
	        //LOG.info("timestamp:: " +tsformat.format(ts));
	        
	        if (doSearch(body, ts, line, threadid)) {
	        	line = in.readLine();
	        } else {
	        	// see if it is an exception stack dump
	        	//LOG.fine("trying to match exception!!! " + line);
	        	Matcher exmat = exceptionHead.matcher(line);
	        	if (exmat.find()) {  // is exception
	        		LOG.fine("Is exception!!");
	        		StringBuffer sbuf = new StringBuffer();
	        		insertlogline.setString(1, dbtsformat.format(ts));
					insertlogline.setInt(2, threadid);
					String expname = exmat.group(1);
					if (expname==null) {
						LOG.warning("Parsing Exception Name Failed.. " + line);
						expname = "UNPARSED EXCEPTION";
					}
					expname = expname.substring(0, Math.min(expname.length(), 240));
					insertlogline.setString(3, "EXCEPTION: " + expname);
					sbuf.append(line).append("\\\\");
					line = in.readLine();
					if (line==null){
						break;
					}
		        	Matcher atmat = exceptionStack.matcher(line);
		        	
		        	StringBuffer stacksb = new StringBuffer();
		        	while( atmat.matches() ) {
		        		LOG.fine("skipped except stack" +line);
		        		stacksb.append(line).append("\\\\");
		        		line = in.readLine();
		        		if (line==null)
		        			break;
		        		atmat = exceptionStack.matcher(line);
		        	}
		        	String stackline = sbuf.append(stacksb).toString();
		        	if (stackline.length() > 1000) {
		        		stackline = stackline.substring(0,1000);
		        	}
		        	insertlogline.setString(4, exmat.group(0));
		        	insertlogline.setString(5, stackline);
		        	insertlogline.setString(6, "message = " + exmat.group(5));
					insertlogline.setString(7, "");
		        	//LOG.info("Inserting exception... " + insertlogline) ;
		        	doInsert(insertlogline);
		        	continue;
	        	} else {  // is not exception
	        		//LOG.warning("Not a valid log format:: " +line);
	        		
	        		StringBuffer sbuf = new StringBuffer();
	        		sbuf.append(body);
	        		line = in.readLine();
	        		if (line==null) {
	        			break;
	        		}
		        	Matcher atmat = exceptionStack.matcher(line);
	        		while( atmat.matches() ) {
		        		LOG.fine("skipped at stack" +line);
		        		sbuf.append("\n").append(line);
		        		line = in.readLine();
		        		if (line==null)
		        			break;
		        		atmat = exceptionStack.matcher(line);
		        	}
	        		
	        		if (!IndexConstants.keepUnMatched) {
	        			//LOG.info("not saved: " +line);
	        			continue;
	        		}
	        		insertlogline.setString(1, dbtsformat.format(ts));
					insertlogline.setInt(2, threadid);
					insertlogline.setString(3, "UNMATCH " + sbuf.substring(0, Math.min(sbuf.length(), 6)));
					insertlogline.setString(4, sbuf.toString());
					insertlogline.setString(5, sbuf.toString());
					insertlogline.setString(6, "");
					insertlogline.setString(7, "" );
		        	doInsert(insertlogline);
		        	continue;
	        	}
	        	
	        }
	      } // while
	    if (indexwriter!=null)
	    	indexwriter.close();
	}
	
	private void doInsert(PreparedStatement ps) {
		try {
			ps.execute();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
}
