package scale;


import index.IndexConstants;

import java.io.File;
import java.io.IOException;
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

public class DistinctValueCount extends Configured implements Tool {
  
	private static Logger LOG = LogFormatter.getLogger(DistinctValueCount.class);
	
	static final String CONF_TSFORMAT = "parsing.tsformat";
	static final String CONF_LOGPATTERN = "parsing.logpattern";
	static final String CONF_DAYPOS = "parsing.daypos";
	static final String CONF_TIMEPOS = "parsing.timepos";
	static final String CONF_DATAPOS = "parsing.datapos";
	static final String CONF_THREADPOS = "parsing.threadpos";
	static final String CONF_LEVELINLOG = "parsing.levelinlog";
	
	
public static class MapClass extends MapReduceBase
    implements Mapper<LongWritable, Text, Text, Text> {
    
	private ParsedMessageWritable parsedmsg = new ParsedMessageWritable();
    private Text varname = new Text();
    private Text varvalue = new Text();
    
	LogParser parser;
    
    public void map(LongWritable key, Text value, 
                    OutputCollector<Text, Text> output, 
                    Reporter reporter) throws IOException {
    	
      
      String line = value.toString();
      boolean success = parser.parseOneLine(line, parsedmsg, reporter);
      
      if (!success) {
    	  //LOG.warning("parsing failed on :: " + line);
    	  return;
      }
      
      String [] vvalues = parsedmsg.getLabels();
      String [] vnames = parsedmsg.getLabelNames();
      
      int msgid = parsedmsg.logid;
      
      for(int i=0; i<vvalues.length; i++) {
    	  String v = vvalues[i]; 
    	  String n = msgid+"-l-"+vnames[i];
    	  if (v.length()==0 || n.length() ==0) {
    		  continue;
    	  }
    	  varname.set( n );
    	  varvalue.set( v );
    	  output.collect(varname, varvalue );
      }
      
      vvalues = parsedmsg.getNumbers();
      vnames = parsedmsg.getNumberNames();

      for(int i=0; i<vvalues.length; i++) {
    	  String v = vvalues[i]; 
    	  String n = msgid+"-d-"+vnames[i];
    	  if (v.length()==0 || n.length() ==0) {
    		  continue;
    	  }
    	  varname.set( n );
    	  varvalue.set( v );
    	  output.collect(varname, varvalue );
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
		parser = new LogParserImpl(job);
	}
    
    
  }


  public static class Reduce extends MapReduceBase
    implements Reducer<Text, Text, Text, Text> {
    
	  //Searcher searcher;
	  //QueryParser qparser;
	  //Text logmsg;
	  //IntWritable logidbuf = new IntWritable(0);
	  //CountVectorWritable countvec = new CountVectorWritable(2);
	  LongWritable tsout = new LongWritable(0);
	  
	  long lasttime = 0;
	  int windowsize;
	  
	  HashMap<String, Integer> value_cnt;
	  Text outkey = new Text();
	  Text outv = new Text();
	  
    public void reduce(Text key, Iterator<Text> values,
		OutputCollector<Text, Text> output,
		Reporter reporter) throws IOException {
    		String k = key.toString();
    		value_cnt = new HashMap<String, Integer>();
    		int cnt =0;
    		long limit = 1000;
    		boolean overlimit = false;
			while (values.hasNext()) {
				cnt +=1;
				if (overlimit)
					continue;
				String v = values.next().toString();
				if (value_cnt.containsKey(v)) {
					value_cnt.put(v, value_cnt.get(v)+1);
				} else {
					if (value_cnt.size()<limit) {
						value_cnt.put(v, 1);
					} else {
						overlimit = true;
						return;
					}
				}
			}
			String out;
			if(!overlimit) {
				out = "(" +cnt+ ")" +dump_print_table();
			} else {
				out = "(" +cnt+ ")((over "+limit+" distinct))";
			}
			outkey.set(k);
			outv.set(out);
			output.collect(outkey, outv);
	}
    
    private String dump_print_table() {
    	StringBuffer sb = new StringBuffer();
    	sb.append("(").append(value_cnt.size()).append(" distinct)");
    	
    	for( Entry<String, Integer> e: value_cnt.entrySet() ) {
    		String k = e.getKey();
    		int cnt = e.getValue();
    		sb.append(k).append(":").append(cnt).append(" ");
    	}
    	return sb.toString();
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
    conf.setMapOutputKeyClass(Text.class);
    
    conf.setOutputKeyClass(Text.class);
    conf.setOutputValueClass(Text.class);
    
    conf.setOutputFormat(TextOutputFormat.class);
    
    conf.setMapperClass(MapClass.class);        
    conf.setReducerClass(Reduce.class);
    
    JobClient.runJob(conf);
    return 0;
  }
  
  
  public static void main(String[] args) throws Exception {
    int res = ToolRunner.run(new Configuration(), new DistinctValueCount(), args);
    System.exit(res);
  }

}
