package index;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;

import conf.ConfUtil;

import sun.misc.FpUtils;
import utils.DbUtils;
import utils.InitDataTable;
import utils.LogFormatter;
import utils.QueryUtils;
import utils.StringUtils;

public abstract class LogLineMatcher implements IndexConstants{

	protected static Logger LOG = LogFormatter.getLogger(LogLineMatcher.class);
	
	protected static TermFilter termfilter= new TermFilter(false);
	
	static {
		if (debug) {
			LogFormatter.setDebugLevel(Level.FINEST, "_all");
		}
	}
	
	IndexReader reader;
	Searcher searcher;
	Analyzer analyzer;
	
	LogIndexWriter indexwriter;
	
	Hashtable<Integer, Integer> callCnt = new Hashtable<Integer, Integer>();

	
	static DateFormat dbtsformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	
	public static void main(String[] args) throws Exception {
		DbUtils.getConn();
		
		termfilter = new TermFilter(true);
		
		//System.err.println(termfilter);
		
		int i =0;
		for (String filename : IndexConstants.LOG_FILE_NAMES) {	
			long ts = System.currentTimeMillis();
			File logfile = new File (IndexConstants.DATA_ROOT, filename);
			LOG.info("Using parser Class: " + parserClassName);
			LOG.info("Parsing log file " + logfile.getAbsolutePath());
			LOG.info("Inserting into table " + IndexConstants.LOG_DB_NAMES[i]);
			
			LOG.info("Log Type Code: " + IndexConstants.LOGTYPE);
			LOG.info("Log Format: " + IndexConstants.standardLogPattern);
			InitDataTable.resetTable(IndexConstants.LOG_DB_NAMES[i]);
			LOG.info(Arrays.deepToString( ConfUtil.getConfig().getStringArray("tsformat")));
			LOG.info("Timestamp format: " + ConfUtil.getConfig().getStringArray("tsformat")[LOGTYPE]);
			LogLineMatcher matcher = ((LogLineMatcher) Class.forName(parserClassName).newInstance());
			
			matcher.indexwriter =null;
			//matcher.indexwriter = new LogIndexWriter(new File(IndexConstants.LOG_DATA_INDEX_DIR, IndexConstants.LOG_DB_NAMES[i]) );
			matcher.search(logfile, IndexConstants.LOG_DB_NAMES[i]);
			i +=1;
			LOG.info("Time Taken: " + ((System.currentTimeMillis()-ts)/1000) +" sec."); 
		}
	}

    public LogLineMatcher() {
		try {
			reader = IndexReader.open(SRC_INDEX_DIR);
			searcher = new IndexSearcher(reader);
			analyzer = new SimpleAnalyzer();
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
    
    
    public abstract void search(File logfile, String dbname) throws Exception;
}

