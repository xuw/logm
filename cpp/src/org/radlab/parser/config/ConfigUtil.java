package org.radlab.parser.config;

import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationFactory;
import org.apache.commons.configuration.SystemConfiguration;
import org.apache.commons.configuration.XMLConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class ConfigUtil {

  private static Configuration config;

  public static void init(String conffile) {
    try {
      File configfile = new File(conffile);
      config = new CompositeConfiguration();
      
      AbstractConfiguration.setDefaultListDelimiter((char)0);
      ((CompositeConfiguration)config).setListDelimiter((char) 0);
      ((CompositeConfiguration)config).setDelimiterParsingDisabled(true);
      
      CompositeConfiguration cc = (CompositeConfiguration)config;
      cc.addConfiguration(new SystemConfiguration());
      cc.addConfiguration(new XMLConfiguration(configfile.getAbsolutePath()));
      System.err.println( "indexdir::" + getSourceIndexDir()  );
      
      
      
//      int i=0;
//      while(true) {
//        String rego = config.getString("userlog("+i+").regexpr");
//        if(rego==null) 
//          break;
//        
//        String reg = rego;
//        System.err.println("reg::" +  rego);
//        System.err.println("type::" + config.getProperty("userlog("+i+").typemap") );
//        System.err.println();
//        i +=1;
//      }
      
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    } 
  }

  public static Configuration getConfig() {
    return config;
  }
  
  
  public static String getSourceIndexDir(){
    return config.getString("indexDir");
  }
  
  public static String getSourceIncludePath(){
    return config.getString("includePath","");
  }
  
  public static String[] getSourceList() {
    return config.getStringArray("sourcecode.item");
  }
  
  public static String getLineFormatClassName() {
    return config.getString("parsing.basicLineFormat","");
  }
  
  public static String getScratchDir() {
    return config.getString("scratchDir","/tmp");
  }
  
  public static void setIndexDir(String value) {
    config.setProperty("indexDir", value);
  }
  
  public static String getCornerCasePolicy() {
    return config.getString("cornerCasePolicy");
  }
  
  public static String getLogFileNamePattern() {
    return config.getString("logFileNamePattern");
  }
  
}
