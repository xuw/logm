package scale;

import index.IndexConstants;

import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.chukwa.extraction.archive.ChuckwaArchiveBuilder;
import org.apache.hadoop.chukwa.inputtools.ChukwaInputFormat;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.lib.IdentityReducer;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;


public class GenericJobConfig implements JobConfigStringConstants{
	
	public static JobConf config(Tool tool, String[] args) {	
		JobConf conf = new JobConf(tool.getConf(), tool.getClass());
	    conf.setJobName(tool.getClass().getSimpleName());
	    conf.setCombinerClass(IdentityReducer.class);
	    
	    List<String> other_args = new ArrayList<String>();
	    String vars = "";
	    for(int i=0; i < args.length; ++i) {
	      try {
	        if ("-m".equals(args[i])) {
	          conf.setNumMapTasks(Integer.parseInt(args[++i]));
	        } else if ("-r".equals(args[i])) {
	          conf.setNumReduceTasks(Integer.parseInt(args[++i]));
	        } else if ("-v".equals(args[i])) {
	          vars = args[++i];
	        } else if ("-chukwa".equals(args[i])) {
	          System.err.println("Using chukwa input format");
	          conf.setInputFormat(ChukwaInputFormat.class);
	        }  else {
	          other_args.add(args[i]);
	        }
	      } catch (NumberFormatException except) {
	        System.out.println("ERROR: Integer expected instead of " + args[i]);
	        printUsage();
	        return null;
	      } catch (ArrayIndexOutOfBoundsException except) {
	        System.out.println("ERROR: Required parameter missing from " +
	                           args[i-1]);
	        printUsage();
	        return null;
	      }
	    }
	    // Make sure there are exactly 2 parameters left.
	    if (other_args.size() != 2) {
	      System.out.println("ERROR: Wrong number of parameters: " +
	                         other_args.size() + " instead of 2.");
	      printUsage();
	      return null;
	    }
	    
	    FileInputFormat.setInputPaths(conf,new Path(other_args.get(0)));
	    FileOutputFormat.setOutputPath(conf,new Path(other_args.get(1)));
	    
	    // send job specific conf.
//	    int logtype = IndexConstants.LOGTYPE;
//	    conf.setStrings(CONF_TSFORMAT, ConfUtil.getConfig().getStringArray("tsformat")[logtype]);
//	    conf.setStrings(CONF_LOGPATTERN, IndexConstants.standardLogPatternStr);
//	    conf.setInt(CONF_DAYPOS, IndexConstants.dayPos);
//	    conf.setInt(CONF_TIMEPOS, IndexConstants.timePos);
//	    conf.setInt(CONF_DATAPOS, IndexConstants.dataPos);
//	    conf.setInt(CONF_THREADPOS, IndexConstants.threadidPos);
//	    conf.setInt(CONF_TIME_WINDOW_SIZE, 5000); // in milliseconds
//	    conf.setBoolean(CONF_LEVELINLOG, IndexConstants.levelInData);
	    
	    conf.setStrings(CONF_VARS_CONSIDERED, vars.split(";"));
	    
	    conf.setStrings("mapred.create.symlink", "yes");
	    return conf;
	}
	
	  static int printUsage() {
		    System.out.println( "ToolName [-m <maps>] [-n <reduces>] [-v <use vars>] <input> <output>");
		    ToolRunner.printGenericCommandUsage(System.out);
		    return -1;
		  }

}
