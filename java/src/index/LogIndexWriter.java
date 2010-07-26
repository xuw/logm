package index;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;

import utils.LogFormatter;
import utils.StringUtils;

public class LogIndexWriter {
	
	protected static Logger LOG = LogFormatter.getLogger(LogIndexWriter.class);
	private IndexWriter writer;
	
	public LogIndexWriter(File logIndexDir){
		
		if (logIndexDir.exists()) {
			logIndexDir.delete();
		}
		
		try {
			this.writer = new IndexWriter(logIndexDir, new StandardAnalyzer(), true);
			LOG.info((new StringBuilder()).append("Indexing to directory '").append(logIndexDir.toString()).append("'...").toString());
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	public void addToIndex(Date ts, String threadid, String body, 
			String[] otherFields, String logid, String level) throws IOException {

            Document doc = new Document();
            String timestamp = Long.toString(ts.getTime());
            doc.add(new Field("timestamp", timestamp, org.apache.lucene.document.Field.Store.YES, org.apache.lucene.document.Field.Index.UN_TOKENIZED));
            if (threadid!=null) {
            	doc.add(new Field("threadid", StringUtils.padInteger(threadid), org.apache.lucene.document.Field.Store.YES, org.apache.lucene.document.Field.Index.UN_TOKENIZED));
            }
            doc.add(new Field("logid", logid, org.apache.lucene.document.Field.Store.YES, org.apache.lucene.document.Field.Index.TOKENIZED));
            doc.add(new Field("logtext", body, org.apache.lucene.document.Field.Store.YES, org.apache.lucene.document.Field.Index.TOKENIZED));
            doc.add(new Field("loglevel", level, org.apache.lucene.document.Field.Store.YES, org.apache.lucene.document.Field.Index.UN_TOKENIZED));
            for(String f: otherFields) {
            	LOG.fine("adding field-value: " +f);
                String[] kv = f.split(" = ");
                if (kv==null || kv.length<2) {
                	continue;
                }
            	doc.add(new Field(kv[0], kv[1], org.apache.lucene.document.Field.Store.YES, org.apache.lucene.document.Field.Index.TOKENIZED));
            }
            writer.addDocument(doc);
	}	
	
	public void close() {
		try {
			writer.optimize();
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
