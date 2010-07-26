package org.radlab.parser.logparser;
import org.apache.hadoop.mapred.Reporter;
import org.radlab.parser.config.LogLineFormat;
import org.radlab.parser.config.GoogleLogLineFormat;
import org.radlab.parser.data.LoggerCallEntry;

import utils.QueryUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class LineNumberLogParser implements LogParser{

  HashMap<String, LoggerCallEntry> templates;
  
  private LogLineFormat format1;
  private LogLineFormat format2;
  private Pattern linePattern1;
  private Pattern linePattern2;
  
  long line_cnt = 0;
  
  public LineNumberLogParser(HashMap<String, LoggerCallEntry> templates, LogLineFormat format, LogLineFormat altLineFormat) {
    this.templates = templates;
    this.format1 = format;
    this.format2 = altLineFormat;
    this.linePattern1 = Pattern.compile(format1.getLineRegExpr()); 
    this.linePattern2 = Pattern.compile(format2.getLineRegExpr()); 
  }
  
  //I1105 10:46:36.826526  6293 borgletimpl.cc:1927] report_problem_when_rebooting set to false
  
  public boolean parseOneLine(String logline, ParsedMessageWritable buf, Reporter reporter) {
    Matcher m = linePattern1.matcher(logline);
    LogLineFormat format;
    if(m.matches()) {
      format = this.format1;
    } else {
      m = linePattern2.matcher(logline);
      if(m.matches()) {
        //System.err.println("using format2");
        format = this.format2;
      } else {
        System.err.println("none of the formats match");
        return false;
      }
    }
    //buf.clear();
    
    if(m.matches()) {
      String key = m.group(format.getSourceLocationPos());
      //System.err.println( m.group(4) + templates.containsKey(key) );
      LoggerCallEntry entry = templates.get(key);
      
      buf.setSrclocation(key);
      
      if( entry !=null) {
        Pattern msgPattern = entry.getPattern();
        if(msgPattern==null) {
          return false;
        }
        Matcher msgMatcher = msgPattern.matcher(m.group(format.getMessagePos()));
        if(msgMatcher.matches()) {
          try {
            ArrayList<String> lbs = new ArrayList<String>();
            ArrayList<String> dts = new ArrayList<String>();
            StringBuffer lbsn = new StringBuffer();
            StringBuffer dtsn = new StringBuffer();
            String[] nameMap = entry.varnames;
            String[] typeMap = entry.types;
              
            //System.err.println("line pattern: " + entry.getRegularExpression() +" " + msgMatcher.groupCount() );
            for (int i = 0; i < msgMatcher.groupCount(); i++) {
              //System.err.println(i + " :: " + entry.varnames[i] + " :: " + msgMatcher.group(i));
              
              if (nameMap[i].length()==0) {
                continue;
              }
            String nmap = nameMap[i].toLowerCase();
            if (nmap.endsWith("id") || nmap.endsWith("name")) {
                lbs.add( msgMatcher.group(i+1) );
                lbsn.append( nameMap[i] ).append(";;");
            } else if (QueryUtils.isNumeric(typeMap[i])) {
                dts.add( msgMatcher.group(i+1) );
                dtsn.append( nameMap[i] ).append(";;");
            } else {
                lbs.add( msgMatcher.group(i+1) );
                lbsn.append( nameMap[i] ).append(";;");
            }
            }
            buf.setLabels(lbs.toArray(new String[lbs.size()]) );
            buf.setNumbers(dts.toArray(new String[dts.size()]));
            buf.setDtsnames(dtsn.toString());
            buf.setLbsnames(lbsn.toString());
            buf.logid = entry.seq;
            return true;
            
          } catch (Exception e) {
              System.err.println("Exception: " + entry.getRegularExpression() +" /// " + Arrays.deepToString(entry.varnames));
              return false;
          }
        } else {
          System.err.println("wrong pattern?? " + m.group(format.getMessagePos()) + " //// " + entry.getRegularExpression());
          return false;
        }
      } else {
        //System.err.println("did not find in source code. " + key);
        return false;
      }
      
    } else {
      //System.err.println("no match:: " + logline);
      return false;
    }
  }
  
  public long getLineCnt() {
    return this.line_cnt;
  }
  
}
