package scale;


import index.IndexConstants;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
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
import org.apache.hadoop.mapred.TextOutputFormat;
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

public class ValuePercent extends Configured implements Tool, JobConfigStringConstants {
  
	private static Logger LOG = LogFormatter.getLogger(ValuePercent.class);
	
	
	
public static class MapClass extends MapReduceBase
    implements Mapper<LongWritable, Text, LongWritable, Text> {
    
	private ParsedMessageWritable parsedmsg = new ParsedMessageWritable();
    private LongWritable ts = new LongWritable();
    private Text varvalue = new Text();
    
	LogParser parser;
    
    public void map(LongWritable key, Text value, 
                    OutputCollector<LongWritable, Text> output, 
                    Reporter reporter) throws IOException {
    	
      
      String line = value.toString();
      boolean success = parser.parseOneLine(line, parsedmsg, reporter);
      
      if (!success || parsedmsg.logid ==-1) {
    	  //LOG.warning("parsing failed on :: " + line);
    	  return;
      }
      
      String [] vvalues = parsedmsg.getLabels();
      String [] vnames = parsedmsg.getLabelNames();
      
//      throw new RuntimeException( value.toString() + Arrays.deepToString( vvalues ) + Arrays.deepToString( vnames));
      
      for(int i=0; i<vvalues.length; i++) {
    	  String v = vvalues[i]; 
    	  String n = vnames[i];
    	  if (v.length()==0 || n.length() ==0) {
    		  continue;
    	  }
    	  
    	  ts.set( parsedmsg.ts );
    	  varvalue.set( n+"=="+v );
    	  output.collect(ts, varvalue );
      }
      
    }
    
    
    
	@Override
	public void close() throws IOException {
		super.close();
		LOG.info("parsed: total=" + parser.getLineCnt() );
	}



	@Override
	public void configure(JobConf job) {
		super.configure(job);
		
		this.parser = new LogParserImpl(job);
	}
    
    
  }


  public static class Reduce extends MapReduceBase
    implements Reducer<LongWritable, Text, LongWritable, CountVectorWritable> {
    
	  //Searcher searcher;
	  //QueryParser qparser;
	  //Text logmsg;
	  //IntWritable logidbuf = new IntWritable(0);
	  CountVectorWritable countvec = new CountVectorWritable(2);
	  LongWritable tsout = new LongWritable(0);
	  
	  long lasttime = 0;
	  int windowsize;
	  
	  HashMap<String, Boolean> namemap;
	  HashMap<String, Integer> value_index;
	  
    public void reduce(LongWritable key, Iterator<Text> values,
		OutputCollector<LongWritable, CountVectorWritable> output,
		Reporter reporter) throws IOException {
    	
		long ts = key.get();
		// over cautious
		if (ts < lasttime ) {
			throw new RuntimeException("not sorted on key (ts)??");
		}
		if (ts - lasttime > windowsize) {
			if (lasttime!=0 ) { 
				System.err.println(ts);
				System.err.println(countvec);
				// output one count result
				tsout.set(lasttime);
				output.collect( tsout , countvec);
				//new start
			}
			countvec.clear();
			lasttime = ts;
		}
    	  
		while (values.hasNext()) {
			String kvs = values.next().toString();
			String[] kv = kvs.split("==");
			String n = kv[0];
			if (namemap.containsKey(n)) {
				String v = kv[1];
				Integer ind = value_index.get(v);
				if (ind==null) {
					ind = value_index.size();
					value_index.put(v, ind);
					countvec.increaseSize(value_index.size());
				}
				countvec.inc(ind);
			} else {
				// do nothing
			}
		}
	}
    
    
    JobConf job;
    
	@Override
	public void configure(JobConf job) {
		super.configure(job);
		this.job = job;
		this.windowsize = job.getInt(CONF_TIME_WINDOW_SIZE, 5000); // to ms
		
		System.err.println("window size = " +this.windowsize);
		value_index = new HashMap<String, Integer>();
		namemap = new HashMap<String, Boolean>();
		
		// hack  -- add variable names
		String[] includevars = "node.getName;dn.getName;srcNode.getName;".split(";");
		
		//String[] includevars = "state;".split(";");
		for(String s: includevars) {
			namemap.put(s.trim(), true);
		}
		System.err.println("monitoring " + namemap.size() +" variables");
	}




	@Override
	public void close() throws IOException {
		super.close();
		// print index
		String[] varvalue = new String[value_index.size()];
		
		for(Entry<String, Integer> entry:value_index.entrySet()) {
			int ind = entry.getValue();
			String var = entry.getKey();
			varvalue[ind] = var;
		}
		Path p = FileOutputFormat.getWorkOutputPath(this.job);
		FSDataOutputStream outs= FileSystem.get(this.job).create( new Path(p,"valueindex") );
		
		PrintStream out = new PrintStream(outs);
		
		for(int i=0; i<varvalue.length; i++) {
			out.println( (i+1) +" " +varvalue[i] );
		}
		out.close();
		
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
    
    conf.setMapOutputValueClass(Text.class);
    conf.setMapOutputKeyClass(LongWritable.class);
    
    conf.setOutputKeyClass(LongWritable.class);
    conf.setOutputValueClass(CountVectorWritable.class);
    
    conf.setOutputFormat(TextOutputFormat.class);
    
    conf.setMapperClass(MapClass.class);        
    conf.setReducerClass(Reduce.class);
    
    // only allow one reduce
    conf.setNumReduceTasks(1);
    
    JobClient.runJob(conf);
    return 0;
  }
  
  
  public static void main(String[] args) throws Exception {
    int res = ToolRunner.run(new Configuration(), new ValuePercent(), args);
    System.exit(res);
  }

}
