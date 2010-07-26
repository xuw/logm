package org.radlab.parser.data;
import utils.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class LoggerCallEntry implements Serializable{
  
  public String[] varnames;
  public String[] types;
  public String level;
  public String template;
  public String location;
  public String caller;
  
  public boolean isUserEntry=false;
  
  public int seq;
  
  private static final String VAR_PLACE_HOLDER = "@#@";
  private static final Pattern PLACE_HOLDER_PATTERN = Pattern.compile(VAR_PLACE_HOLDER);
  
  private transient Pattern patterncache;
  
  public LoggerCallEntry(String[] varnames, String[] types, String level, String template,
      String location, String caller) {
    super();
    this.varnames = varnames;
    this.types = types;
    this.level = level;
    this.template = template;
    this.location = location;
    this.caller = caller;
  }
  
  public LoggerCallEntry(ArrayList<String> varnames, ArrayList<String> types, String level, String template,
      String location, String caller) {
    super();
    this.varnames = varnames.toArray(new String[varnames.size()]);
    this.types = types.toArray(new String[types.size()]);
    this.level = level;
    this.template = template;
    this.location = location;
    this.caller = caller;
  }
  
  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append("==line:: " + location).append("\n");
    sb.append("==level:: " + level).append("\n");
    sb.append("==template:: " + getRegularExpression() ).append("\n");
    sb.append("==names:: " + Arrays.deepToString(varnames) ).append("\n");
    sb.append("==types:: " + Arrays.deepToString(types)).append("\n");
    return sb.toString();
  }
  
  public String getRegularExpression() {
    String t = StringUtils.escapeRegExprString(this.template);
    
    t = t.replaceAll("(@#@)+", "@#@");  // FIXME: will cause the var list to blow up
    t = t.replaceAll("@#@ @#@", "([^ ]*) @#@");  // special handle for var1 var2 case
    
    Matcher m = PLACE_HOLDER_PATTERN.matcher(t);
    StringBuffer sb = new StringBuffer();
    int cnt=0;
    while( m.find() ) {
      String type;
      try {
        type = types[cnt];
      } catch (Exception e) {
        System.err.println("Exception on " + this.template +" len=" + this.types.length + " " + this.location +" " + Arrays.deepToString( this.types ));
        throw (RuntimeException)e;
      }
      String replace ="(.*)";
      if("int".equals(type)) {
        //replace = "([+-]?[0-9]+)";
        replace = "(.*)";
      } else if(type.endsWith("-hex")){
        replace = "(.*)";
      }
      m.appendReplacement(sb, replace);
      cnt +=1;
    }
    m.appendTail(sb);
    String result = sb.toString().replaceAll("@@@", "([^:]*):\\\\s+").replaceAll("@%@", "(.*:.*: )?");
    if(isUserEntry) {
      return "\\s*" + result + "\\s*";
    }
    return result;
  }
  
  public Pattern getPattern() {
    if(this.patterncache!=null) {
      return this.patterncache;
    } else {
      try {
        String regexpr = this.getRegularExpression();
        this.patterncache = Pattern.compile(regexpr);
      } catch (Exception e) {
        return null;
      }
      return this.patterncache;
    }
  }
  
  public String toStrIndexString() {
    return this.template.replaceAll("@#@", " ").replaceAll("@@@", "").replaceAll("@%@", "");
  }
  
}
