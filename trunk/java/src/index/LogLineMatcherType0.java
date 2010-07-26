package index;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Map.Entry;
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

public class LogLineMatcherType0 extends LogLineMatcher {
	
    public void search(File logfile, String dbname) throws Exception{
		BufferedReader in = null;
		if (logfile.exists()) {
	      in = new BufferedReader(new FileReader(logfile));
	    } else {
	      in = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
	    }
	    
	    int cnt =0;
	    int nomatchcnt =0;
	    Integer[] hitcnt = new Integer[6];
	    int totalhit = 0;
	    Float matchingscore =0.0f;
	    float minMatchingScore = 1.0f;
	    ArrayList<String> low_scores = new ArrayList<String>();
	    ArrayList<String> non_match = new ArrayList<String>();
	    int hardmatch_success_cnt = 0;
	    int hardmatch_fail_FP = 0;
	    int hardmatch_fail_FN = 0;
	    ArrayList<String> hardmatchfail = new ArrayList<String>();
	    
	    Connection conn = DbUtils.getConn();
	    PreparedStatement insertlogline = conn.prepareCall("insert into sampleLog (ts, threadid, logid, logbody ,textentry) values(?,?,?,?,?)");
	    
	    for (int i=0; i<6; i++) {
	    	hitcnt[i] =0;
	    }
	    
	    
	    
	    while (true) {
	    	String line = in.readLine();
	        if (line == null || line.length() == -1)
	          break;

	        line = line.trim();
	        if (line.length()==0)
	        	continue;
	        
	        
	        Matcher m = standardLogPattern.matcher(line);
	        Pattern numeric = Pattern.compile( " *[0-9]+ *" );
	        if(m.find())
	        {
	            
	        	String classname;
        		String method;
        		String file;
        		String srcline;
        		String day;
        		String time;
        		String threadid;
        		String body;

        		classname = m.group(5);
	        	method = m.group(6);
	        	file = m.group(7);
	        	srcline = m.group(8);
	        	day = m.group(1);
	        	time = m.group(2);
	        	threadid = m.group(3);
	        	body = m.group(4);	        	
	        	
	            String processedbody = body.trim().replaceAll("[^a-zA-Z][0-9]+", "").replace("_", " ");
	            processedbody = QueryUtils.escapeQueryString(processedbody);
	            System.err.println("Processing Log::" + processedbody);
	            
	            Date ts = tsformat.parse(day+" "+time);
	            
//	            String[] words = QueryUtils.escapeQueryString( processedbody ).split(" ");
//		        BooleanQuery line_query = new BooleanQuery();
//		        for(String s: words) {
//		        	if (!Pattern.matches(" *[0-9]+ *", s)) {
//		        	Query q0 = new TermQuery(new Term("strindex",s));
//		        	line_query.add(q0, BooleanClause.Occur.SHOULD);
//		        	}
//		        }
		        
		        Query line_query=null;
				try {
					QueryParser qp = new QueryParser("strindex", analyzer);
					line_query = qp.parse(processedbody);
				} catch (ParseException e) {
					e.printStackTrace();
				}
		        
		        System.err.println("query used::" + line_query);
		        Hits hits = searcher.search(line_query, Sort.RELEVANCE);
		        cnt +=1;
		        if ( hits.length()==0 ) {
		        	//System.err.println("NOT FOUND:" + classname + ":" + srcline);
		        	nomatchcnt +=1;
		        	continue;
		        }
		        int i;
		        
		        String compilationUnitName = classname.replaceAll("\\$.*", "");
		        System.err.println("ACTUAL:" + compilationUnitName + ":" + srcline);
		        for (i=0; i< Math.min(hits.length(), NUM_RESULT_CONSIDERED); i++) {
		        	Document hit = hits.doc(i);
		        	System.err.println("RESULT "+i+":" + hit.get("classname") 
		        			+":"+ hit.get("line") +" SCORE:" + hits.score(i));
		        	
		        	// test if really match
		        	String regexprstr = hit.getField("regexpr").stringValue();
		        	
		        	Pattern p = Pattern.compile(regexprstr);
		        	boolean hardmatch = Pattern.matches(regexprstr, body);
		        	System.err.println("Hardmatch " + hardmatch);
		        	if (hardmatch) {
		        		
		        		String logid = hit.get("classname") +"-"+hit.get("line");
		        		ArrayList<Integer> premethods = DbUtils.findMethodsWithLogId(logid, true);
		        		ArrayList<Integer> postmethods = DbUtils.findMethodsWithLogId(logid, false);
		        		//System.err.println("PRE::" + Arrays.deepToString(premethods.toArray()) );
		        		//System.err.println("POST::" + Arrays.deepToString(postmethods.toArray()) );
		        		for(Integer k: premethods) {
		        			Integer ccnt = this.callCnt.get(k);
		        			if (ccnt ==null) {
		        				this.callCnt.put(k, 1);
		        			} else {
		        				this.callCnt.put(k, ccnt+1);
		        			}
		        		} 
		        		
		        		for(Integer k: postmethods) {
		        			Integer ccnt = this.callCnt.get(k);
		        			if (ccnt ==null) {
		        				this.callCnt.put(k, 1);
		        			} else {
		        				this.callCnt.put(k, ccnt+1);
		        			}
		        		} 
		        		
		        		if (hit.get("classname").equals(compilationUnitName) 
				        		&& hit.get("line").equals(srcline)) {
		        			insertlogline.setString(1, dbtsformat.format(ts));
		        			insertlogline.setInt(2, Integer.parseInt( threadid ));
		        			insertlogline.setString(3, hit.get("classname") + "-" + hit.get("line"));
		        			insertlogline.setString(4, body);
		        			insertlogline.setString(5, line);
		        			insertlogline.execute();
		        			hardmatch_success_cnt +=1;
		        		} else {
		        			hardmatch_fail_FP +=1;
		        			hardmatchfail.add("FP:" +body+" "+ regexprstr +"("+hits.score(i)+")");
		        		}
		        	}
		        	
		        	if (hit.get("classname").equals(compilationUnitName) 
		        		&& hit.get("line").equals(srcline)){
		        		hitcnt[i] +=1;
		        		matchingscore += hits.score(i);
		        		totalhit +=1;
		        		if (hits.score(i) < minMatchingScore) {
		        			minMatchingScore = hits.score(i);
		        		}
		        		if (hits.score(i) < 0.5 ) {
		        			low_scores.add(body+" "+ line_query.toString() +"("+hits.score(i)+")");
		        		}
		        		if (!hardmatch) {
		        			hardmatch_fail_FN +=1;
		        			hardmatchfail.add("FN:" +body+" "+ regexprstr +"("+hits.score(i)+")");
		        		}
		        		break;
		        	}
		        }
		        if ( i>=5 ) {
		        	System.err.println("***MATCH DOES NOT EXIST IN FIRST 5 RESULTS ");
		        	non_match.add(body+" "+ line_query.toString() );
		        	hitcnt[5] +=1;
		        }
	        } else{
	        	System.err.println("Not is valid log format:: " +line);
	        }
	      } // while
	    
	    System.err.println("Total=" + cnt +" Non Matched=" + nomatchcnt);
	    System.err.println(Arrays.deepToString(hitcnt));
	    System.err.println("Average Matching Score=" + (matchingscore/totalhit));
	    System.err.println("Min Matching Score=" + minMatchingScore);
	    System.err.println("RegExpr match success=" + hardmatch_success_cnt);
	    System.err.println("RegExpr match FP / FN=" + hardmatch_fail_FP +" / "+hardmatch_fail_FN);
	    System.err.println("============None Match============");
	    for (String s: non_match) {
	    	System.err.println( s );
	    }
	    System.err.println("============Low Score Matches========");
	    for (String s: low_scores) {
	    	System.err.println( s );
	    }
	    System.err.println("============RegExpr Matche Failures========");
	    for (String s: hardmatchfail) {
	    	System.err.println( s );
	    }
	    
	    for (Entry<Integer, Integer> e: callCnt.entrySet()) {
	    	System.err.println( DbUtils.findCallGraphEdge( e.getKey() ) 
	    			+" : " + e.getValue() );
	    	DbUtils.insertCallCnt(e.getKey(), e.getValue());
	    }
	    
	}
	
}
