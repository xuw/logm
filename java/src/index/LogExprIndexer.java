package index;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Date;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;

import conf.ConfUtil;

import data.RegExprRecord;

import utils.DbUtils;
import utils.LogFormatter;
import utils.StringUtils;


public class LogExprIndexer implements IndexConstants{
	
	
	private static Logger LOG = LogFormatter.getLogger(LogExprIndexer.class);
	private static TermFilter terms;
	private static IndexWriter writer;
	
	private static int logsequence = 10;  // start with 10 leaving some space for exception types
	
	static {
		if (debug) {
			LogFormatter.setDebugLevel(Level.FINEST, "_all");
		}
	}
	
	  public static void main(String[] args) {
	    
		
	    if (SRC_INDEX_DIR.exists()) {
	    	SRC_INDEX_DIR.delete();
	    }
	    
	    terms= new TermFilter(false);
	    
	    Date start = new Date();
	    try {
	      writer = new IndexWriter(SRC_INDEX_DIR, new SimpleAnalyzer(), true);
	      writer.setInfoStream( System.err );
	      LOG.info("Indexing to directory '" +SRC_INDEX_DIR+ "'...");
	      indexDocs();
	      LOG.info("Optimizing...");
	      writer.optimize();
	      writer.close();

	      Date end = new Date();
	      LOG.info(end.getTime() - start.getTime() + " total milliseconds");

	    } catch (IOException e) {
	      LOG.severe(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
	    }
	  }

	  static void indexDocs()
	    throws IOException {
		  
		try {
			Connection conn = DbUtils.getConn();
			Statement stm = conn.createStatement();
			ResultSet rs = stm.executeQuery("select logid from logentries");
			
			while (rs.next()) {
				String lid = rs.getString("logid");
				RegExprRecord[] recs = StringUtils.getExpandedRegExpr(lid);
				for(RegExprRecord rec: recs) {
					addOneRecord(rec);
				}
			}
			rs.clearWarnings();
			rs.close();
			
			// process user entries
			String[] ids = ConfUtil.getConfig().getStringArray("userlog.classname");
			String[] lines = ConfUtil.getConfig().getStringArray("userlog.line");
			String[] namemaps = ConfUtil.getConfig().getStringArray("userlog.namemap");
			String[] typemaps = ConfUtil.getConfig().getStringArray("userlog.typemap");
			String[] regexprs = ConfUtil.getConfig().getStringArray("userlog.regexpr");
			
			//RegExprRecord(String regExpr, String constStr, String nameMap ,String typeMap, String logid, int level)
			for (int i=0; i< ids.length; i++) {
				RegExprRecord rec = new RegExprRecord(regexprs[i],"",namemaps[i], typemaps[i], ids[i]+"-"+lines[i], 900, ids[i]+"-"+lines[i]);
				Document doc = EntryDocument.Document(rec, logsequence++);
				writer.addDocument(doc);
				terms.addTerms(rec.RegExpr);
				LOG.info("added user entry: " + rec +" seq=" + logsequence);
			}
			
			terms.writeToDisk();
			System.err.println(terms.toString());
		} catch (SQLException e) {
			e.printStackTrace();
		}

	  }
	  
	  static void addOneRecord(RegExprRecord rec) {
		try {
			Document doc = EntryDocument.Document(rec, logsequence++);
			writer.addDocument(doc);
			terms.addTerms(rec.RegExpr);
			LOG.info("added " + rec +" seq=" + logsequence);
		} catch (Exception e) {
			;
		}
	}
	  
}
