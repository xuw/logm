import org.radlab.parser.config.ConfigUtil;
import org.radlab.parser.config.GoogleLogLineFormat;
import org.radlab.parser.config.LogLineFormat;
import org.radlab.parser.logparser.LineNumberLogParser;
import org.radlab.parser.logparser.LogParser;
import org.radlab.parser.logparser.LogParserImpl;
import org.radlab.parser.logparser.ParsedMessageWritable;
import org.radlab.parser.source.GlobalResultCollector;
import org.radlab.parser.source.ParserMain;
import org.radlab.parser.source.index.LoggerCallIndexer;

import utils.DirectoryUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;


public class LineParserTest {
  
  
  public static void main(String[] args) throws Exception {
    String[] Args;
    if (args.length < 4) {
      System.err
          .println("usage: LineParserTest configXMLFile parse_source?(true/false) logfile log_dir1 log_dir2 ...");
      System.exit(-1);
    }
    
    File outfile = new File(args[2]);
    if(outfile.exists()) 
      outfile.delete();
    System.setErr(new PrintStream(new FileOutputStream(outfile) ) );
    
    
    System.err.println("Configuration file name " + args[0]);
    String configFileName = args[0];
    ConfigUtil.init(configFileName);

    boolean parsesource = Boolean.parseBoolean(args[1]);
    if (parsesource) {
      String includePathFileName = ConfigUtil.getSourceIncludePath();
      String[] sourceDirList = ConfigUtil.getSourceList();
      List<String> filelist = new ArrayList<String>();

      for (String fnl : sourceDirList) {
        System.err.println("adding " + fnl + " to source code list");
        DirectoryUtils.processDirRecurse(new File(fnl), filelist);
      }

      AutoRun.out = new PrintWriter(System.out);

      for (String fn : filelist) {
        // process a file
        // System.err.println("processing " + fn);
        String[] mainargs = new String[] {fn, includePathFileName};
        if (fn==null)
          continue;
        if(includePathFileName==null)
          continue;
        ParserMain.main(mainargs);
      }

      LoggerCallIndexer.indexDocs(GlobalResultCollector.results);
      GlobalResultCollector.writeData();
    } else {
      // not parsing source,, just load from file
      GlobalResultCollector.loadData();
    }
    // try parsing logs...
    // LogParser logparser = new LogParser(result);

    HashMap<String, Boolean> typeparse = new HashMap<String, Boolean>();
    
    LogLineFormat format =
        (LogLineFormat) Class.forName(ConfigUtil.getLineFormatClassName()).newInstance();
    System.out.println("format:: " + ConfigUtil.getLineFormatClassName() );

    LogParser parser = new LineNumberLogParser(GlobalResultCollector.results, format, new GoogleLogLineFormat());
    int successcnt = 0;
    int successtypecnt = 0;
    int failcnt = 0;
    int failtypecnt = 0;
    int bad_format_cnt = 0;

    ArrayList<String> successsample = new ArrayList<String>();
    ArrayList<String> failedsample = new ArrayList<String>();

    ParsedMessageWritable buf = new ParsedMessageWritable();
    String line;

    ArrayList<File> logsToParse = new ArrayList<File>();
    for (int i = 3; i < args.length; i++) {
      File f = new File(args[i]);
      if (f.isDirectory()) {
        for (File ff : f.listFiles()) {
          if(!ff.isDirectory()){
            logsToParse.add(ff);
          }
        }
      } else {
        logsToParse.add(f);
      }
    }

    for (File f : logsToParse) {
      if(f.isDirectory())
        continue;
      System.err.println("Processing Log File:: " + f);
      BufferedReader in = new BufferedReader(new FileReader(f));
      line = in.readLine();
      while (line != null) {
        // System.err.println("PARSING: " + line);
        buf.setSrclocation(null);
        boolean success = parser.parseOneLine(line, buf, null);
        if (buf.getSrclocation() != null) {
          ;
        } else {
          bad_format_cnt += 1;
          line = in.readLine();
          continue;
        }
        
        if (success) {
          // System.err.println(buf);
          successcnt += 1;
          if (!typeparse.containsKey(buf.getSrclocation())) {
            typeparse.put(buf.getSrclocation(), true);
            successtypecnt += 1;
            successsample.add(line + "\n" + buf);
          }
        } else {
          failcnt += 1;
          if (!typeparse.containsKey(buf.getSrclocation())) {
            typeparse.put(buf.getSrclocation(), false);
            failtypecnt += 1;
            failedsample.add(line);
          } 
        }
        line = in.readLine();
      }

    }

    System.err.println("failed= " + failcnt + " success=" + successcnt + " badformat="
        + bad_format_cnt + " successtype=" + successtypecnt + " failtype=" + failtypecnt);

  }

}
