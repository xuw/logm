package org.radlab.parser.source;
import org.eclipse.cdt.core.index.IIndex;
import org.radlab.parser.config.ConfigUtil;
import org.radlab.parser.data.LoggerCallEntry;
import org.radlab.parser.source.index.TermFilter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.HashSet;

/// need to clean up this class..
public class GlobalResultCollector {
  
    public static IIndex bindingindex = null;
    public static HashMap<String, LoggerCallEntry> results = new HashMap<String, LoggerCallEntry>();
    public static HashSet<String> parsedSourceFiles = new HashSet<String>();
    public static TermFilter terms;
    
    
    public static void writeData() throws IOException{
      File datadir = new File(ConfigUtil.getSourceIndexDir());
      ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream( new File(datadir,"hashIndex")));
      out.writeObject(results);
      out.flush();
      out.close();
      
      
      out = new ObjectOutputStream(new FileOutputStream( new File(datadir,"parsedSourceList")));
      out.writeObject(parsedSourceFiles);
      out.flush();
      out.close();
      
      terms.writeToDisk();
    }
    
    public static void loadData() throws IOException, ClassNotFoundException{
      File datadir = new File(ConfigUtil.getSourceIndexDir());
      ObjectInputStream in = new ObjectInputStream(new FileInputStream( new File(datadir,"hashIndex")));
      results = (HashMap<String, LoggerCallEntry>) in.readObject();
      in.close();
      
      in = new ObjectInputStream(new FileInputStream( new File(datadir,"parsedSourceList")));
      parsedSourceFiles = (HashSet<String>) in.readObject();
      in.close();
      
      terms = new TermFilter(true); // read from disk
      
    }
    
}
