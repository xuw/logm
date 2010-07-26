package scale;


import index.IndexConstants;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.SequenceFileOutputFormat;
import org.apache.hadoop.mapred.lib.IdentityMapper;
import org.apache.hadoop.mapred.lib.IdentityReducer;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Searcher;

import utils.LogFormatter;

import conf.ConfUtil;

public class MsgCount extends Configured implements Tool, JobConfigStringConstants {
  
	private static Logger LOG = LogFormatter.getLogger(MsgCount.class);
	
	
public static class MapClass extends MapReduceBase
    implements Mapper<LongWritable, Text, Text, TimedLogIDWritable> {
    
	private ParsedMessageWritable parsedmsg = new ParsedMessageWritable();
    private Text valuekey = new Text();
    //private IntWritable logidw = new IntWritable();
    private TimedLogIDWritable outv= new TimedLogIDWritable();
    
	LogParserImpl parser;
    
    public void map(LongWritable key, Text value, 
                    OutputCollector<Text, TimedLogIDWritable> output, 
                    Reporter reporter) throws IOException {
    	
      
      String line = value.toString();
      boolean success = parser.parseOneLine(line, parsedmsg, reporter);
      
      if (!success || parsedmsg.logid ==-1) {
    	  //LOG.warning("parsing failed on :: " + line);
    	  return;
      }
      
      String[] labelvalues = parsedmsg.getLabels();
      String[] labelnames = parsedmsg.getLabelNames();
      for(int i=0; i< labelvalues.length; i++) {
    	  String k = labelvalues[i];
    	  String n = labelnames[i];
    	  if (k.length()==0) {
    		  continue;
    	  }
    	  
    	  valuekey.set(n+k);
    	  outv.set(parsedmsg.logid, parsedmsg.ts,  "");
    	  output.collect(valuekey, outv);
      }
      
    }
    
	@Override
	public void configure(JobConf job) {
		super.configure(job);
		this.parser = new LogParserImpl(job);
	}
    
    
  }


  public static class Reduce extends MapReduceBase
    implements Reducer<Text, TimedLogIDWritable, Text, CountVectorWritable> {
    
	  //Searcher searcher;
	  //QueryParser qparser;
	  //Text logmsg;
	  //IntWritable logidbuf = new IntWritable(0);
	  CountVectorWritable countvec = new CountVectorWritable(2000);
	  CountVectorWritable dfcnt = new CountVectorWritable(2000);
	  
	  
	  ArrayList<TimedLogIDWritable> msggroup = new ArrayList<TimedLogIDWritable>(10);
	  
    public void reduce(Text key, Iterator<TimedLogIDWritable> values,
		OutputCollector<Text, CountVectorWritable> output,
		Reporter reporter) throws IOException {
    	
		msggroup.clear();
		
		// clearly a hack..
//    	  if (!key.toString().startsWith("com.sun.sgs.io.AsynchronousMessageChannel$")) {
//    	   	return;
//    	  }
    	  
		if (!key.toString().startsWith("tid")) {
    	   	return;
    	  }
		
//    	  if (!key.toString().startsWith("blk_")) {
//    		  return;
//    	  }
		
    	  countvec.clear();
    	
    	  
    	long tsmin=Long.MAX_VALUE;
    	long tsmax=Long.MIN_VALUE;
		while (values.hasNext()) {
			TimedLogIDWritable v = values.next();
			//LOG.info("id: " +v.logid + " key: " + key.toString());
			//msggroup.add(new TimedLogIDWritable().copy(v));
			countvec.inc(v.logid);
			if (v.ts < tsmin) {
				tsmin = v.ts;
			} 
			if (v.ts > tsmax) {
				tsmax = v.ts;
			}
		}
		
//		int detection_timeout = 10000 *1000;
//		
//		Collections.sort(msggroup);
//		
//		long lastts = msggroup.get(0).ts;
//		boolean timeout = false;
//		for(TimedLogIDWritable v: msggroup) {
//			if (v.ts - lastts <0 ) {
//				throw new RuntimeException( "timestamp sorting error?? " + Arrays.deepToString( msggroup.toArray() ));
//			} else if (v.ts - lastts >detection_timeout) {
//				key.set(key.toString()+":timeout:" + v.ts);
//				tsmax = lastts + detection_timeout;
//				timeout = true;
//				break; // ignore the rest
//			}
//			countvec.inc(v.logid);
//			lastts = v.ts;
//		}
//		if (! timeout) {
//			//went through every line..  still need to wait for timeout
//			tsmax += detection_timeout;
//		}
		dfcnt.DFcnt(countvec);
		countvec.tsmin = tsmin;
		countvec.tsmax = tsmax;
		output.collect(key, countvec);
	}
    
    
    JobConf job;
    
	@Override
	public void configure(JobConf job) {
		super.configure(job);
		this.job = job;
	}




	@Override
	public void close() throws IOException {
		super.close();
		// output dfcnt
		Path p = FileOutputFormat.getWorkOutputPath(this.job);
		String taskid = job.get("mapred.tip.id");
		String[] taskidarr = taskid.split("_");
		String fseq = taskidarr[taskidarr.length-1];
		SequenceFile.Writer w = new SequenceFile.Writer(FileSystem.get(this.job), this.job, 
				new Path(p,"dfcnt-"+fseq), Text.class, CountVectorWritable.class);
		//FSDataOutputStream out= FileSystem.get(this.job).create( );
		Text t = new Text();
		t.set("df");
		w.append(t, dfcnt);
		w.close();
	}
    
    
    
  }
  
  /**
   * The main driver for word count map/reduce program.
   * Invoke this method to submit the map/reduce job.
   * @throws IOException When there is communication problems with the 
   *                     job tracker.
   */
  public int run(String[] args) throws Exception {
	  
	  JobConf conf =  GenericJobConfig.config(this, args);
	    if (conf==null) {
	    	return -1;
	    }
    
    conf.setMapOutputValueClass(TimedLogIDWritable.class);
    conf.setMapOutputKeyClass(Text.class);
    
    // the keys are words (strings)
    conf.setOutputKeyClass(Text.class);
    // the values are counts (ints)
    conf.setOutputValueClass(CountVectorWritable.class);
    
    conf.setOutputFormat(SequenceFileOutputFormat.class);
    
    conf.setMapperClass(MapClass.class);        
    conf.setReducerClass(Reduce.class);
    
    JobClient.runJob(conf);
    return 0;
  }
  
  
  public static void main(String[] args) throws Exception {
    int res = ToolRunner.run(new Configuration(), new MsgCount(), args);
    System.exit(res);
  }

}
