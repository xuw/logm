import com.sun.org.apache.bcel.internal.generic.GETSTATIC;
import com.sun.org.apache.xalan.internal.xsltc.runtime.Hashtable;

import org.eclipse.cdt.core.dom.ILinkage;
import org.eclipse.cdt.core.dom.ast.gnu.cpp.GPPLanguage;
import org.eclipse.cdt.core.index.IIndexBinding;
import org.eclipse.cdt.core.index.IIndexFile;
import org.eclipse.cdt.core.index.IIndexFileLocation;
import org.eclipse.cdt.core.index.IIndexLocationConverter;
import org.eclipse.cdt.core.index.IndexFilter;
import org.eclipse.cdt.core.model.ILanguage;
import org.eclipse.cdt.core.parser.IParserLogService;
import org.eclipse.cdt.core.parser.IScannerInfo;
import org.eclipse.cdt.internal.core.index.IWritableIndex;
import org.eclipse.cdt.internal.core.indexer.ILanguageMapper;
import org.eclipse.cdt.internal.core.indexer.IStandaloneScannerInfoProvider;
import org.eclipse.cdt.internal.core.indexer.StandaloneFastIndexer;
import org.eclipse.cdt.internal.core.indexer.StandaloneIndexer;
import org.eclipse.cdt.internal.core.pdom.dom.IPDOMLinkageFactory;
import org.eclipse.cdt.internal.core.pdom.dom.c.PDOMCLinkageFactory;
import org.eclipse.cdt.internal.core.pdom.dom.cpp.PDOMCPPLinkageFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.radlab.parser.config.LogLineFormat;
import org.radlab.parser.config.GoogleLogLineFormat;
import org.radlab.parser.config.ConfigUtil;
import org.radlab.parser.config.GFSLogLineFormat;
import org.radlab.parser.data.LoggerCallEntry;
import org.radlab.parser.logparser.LineNumberLogParser;
import org.radlab.parser.logparser.LogParser;
import org.radlab.parser.logparser.LogParserImpl;
import org.radlab.parser.logparser.ParsedMessageWritable;
import org.radlab.parser.source.GlobalResultCollector;
import org.radlab.parser.source.ParserMain;
import org.radlab.parser.source.index.LoggerCallIndexer;

import sun.util.LocaleServiceProviderPool.LocalizedObjectGetter;
import utils.DirectoryUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.security.auth.login.FailedLoginException;



public class AutoRun {
  static PrintWriter out;

  public static void main(String[] args) throws Exception {
    String[] Args;
    if (args.length < 4) {
      System.err
          .println("usage: AutoRun configXMLFile parse_source?(true/false) logfile log_dir1 log_dir2 ...");
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

      out = new PrintWriter(System.out);

      for (String fn : filelist) {
        // process a file
        // System.err.println("processing " + fn);
        String[] mainargs = new String[] {fn, includePathFileName};
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
    HashMap<String, PrintWriter> failedsamplesout = new HashMap<String, PrintWriter>();
    File t = new File(ConfigUtil.getScratchDir());
    
    if(t.exists()) {
      for (File f: t.listFiles()) {
        f.delete();
      }
    } else if(!t.exists()) {
      t.mkdirs();
    }
    
    
    LogLineFormat format =
        (LogLineFormat) Class.forName(ConfigUtil.getLineFormatClassName()).newInstance();

    LogParser parser = new LogParserImpl(format);
    LineNumberLogParser p2 = new LineNumberLogParser(GlobalResultCollector.results, format, new GoogleLogLineFormat());
    
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
        //System.err.println("PARSING: " + line);
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
          // try parsing with line number 
          success = p2.parseOneLine(line, buf, null);
          if(success) {
            System.err.println("succeed with line number parser..");
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
            if (!failedsamplesout.containsKey(buf.getSrclocation())) {
              PrintWriter w =
                  new PrintWriter(new FileWriter(new File(ConfigUtil.getScratchDir(), buf
                      .getSrclocation())));
              failedsamplesout.put(buf.getSrclocation(), w);
            }
            PrintWriter w = failedsamplesout.get(buf.getSrclocation());
            int ind = line.indexOf("]");
            w.println(line.substring(ind + 1));
          }
        }
        line = in.readLine();
      }

    }

    System.err.println("===== success =====");
    for (String s : successsample) {
      System.err.println("SAMPLELINE: " + s);
      System.err.println();
    }

    


    HashMap<String, Integer> missingfiles = new HashMap<String, Integer>();
    int failure_from_missing_files = 0;
    int succFromLineNumber = 0;
    // System.err.println("parsed files:: " +
    // Arrays.deepToString(GlobalResultCollector.parsedSourceFiles.toArray()) );

    
//    
//    STARTUP_MSG</classname>
//    
//    <regexpr>STARTUP_MSG:\s*\n\/\*+\s*\n(STARTUP_MSG:(.*)\n)+\*+\/</regexpr>
//    <namemap></namemap>
//    <typemap></typemap>
//  

    System.err.println("===== failures =====");
    for (String s : failedsample) {
      buf.setSrclocation(null);
      boolean succ = p2.parseOneLine(s, buf, null);
      
      String location = buf.getSrclocation();
      
      String[] ls;
      if (location == null) {
        location = "";
        ls = new String[]{"",""};
      } else {
        ls = location.split(":");
      }
      if (succ) {
        succFromLineNumber += 1;
      }
      
      if (GlobalResultCollector.parsedSourceFiles.contains(ls[0])) {
        // we have seen the line, but still don't parse, suggest user add
        System.err.println("<!--Please add the following manual entry-->");
        ;
      } else {
        System.err.println("<!-- havent seen the source file yet! you might not need the manual entry if you add additional sources --> " + ls[0]);
        
        failure_from_missing_files += 1;
        Integer c = missingfiles.get(ls[0]);
        if (c == null) c = 0;
        missingfiles.put(ls[0], c + 1);
      }
      
      if(!succ ) {
        System.err.println("<userlog>");
        System.err.println("\t<classname>"+ls[0]+"</classname>");
        System.err.println("\t<line>"+ls[1]+"</line>");
        System.err.println("\t<regexpr>"+s+"</regexpr>");
        System.err.println("\t<namemap></namemap>");
        System.err.println("\t<typemap></typemap>");
        System.err.println("</userlog>");
      }
      
      
    }
    
    System.err.println("failed= " + failcnt + " success=" + successcnt + " badformat="
        + bad_format_cnt + " successtype=" + successtypecnt + " failtype=" + failtypecnt);
    System.err.println("missing source files: "
        + Arrays.deepToString(missingfiles.entrySet().toArray()));
    System.err.println("failures from missing source files =" + failure_from_missing_files);
    System.err.println("success from line numbers=" + succFromLineNumber);

    HashSet<String> additionalfiles = new HashSet<String>();

    for (String fn : missingfiles.keySet()) {
      fn = fn.replace(".", "\\.");
      if(fn==null || fn.length()==0)
        continue;
      String command = "gsearch -f /" + fn + " include";
      System.err.println("search command: " + command);
      Process searchprocess = Runtime.getRuntime().exec(command);
      BufferedReader r = new BufferedReader(new InputStreamReader(searchprocess.getInputStream()));
      line = r.readLine();
      while (line != null) {
        String[] ns = line.split(":");
        String fullpath = ns[0];
        System.err.println(fullpath);
        if (!additionalfiles.contains(fullpath)) {
          additionalfiles.add(fullpath);
        }
        try {
          line = r.readLine();
        } catch (Exception e1) {
          System.err.println("IO error reading line " + line);
          line = null;
        }
      }
    }

    System.err.println("== additional files needed ==");
    for (String s : additionalfiles) {
      System.err.println("<item>"+s+"</item>");
    }
    
    for(Entry<String, PrintWriter> e : failedsamplesout.entrySet()) {
      e.getValue().flush();
      e.getValue().close();
    }

  }


  public static void buildIndex(List<String> files) throws Exception {
    File writableIndex = new File("/tmp/standaloneIndexFile.pdom");
    IIndexLocationConverter converter = new MyIndexLocationConverter();
    // See PDOMCPPLinkageFactory if needed ..
    Map<String, IPDOMLinkageFactory> linkageFactoryMappings =
        new HashMap<String, IPDOMLinkageFactory>();
    linkageFactoryMappings.put(ILinkage.C_LINKAGE_NAME, new PDOMCLinkageFactory());
    linkageFactoryMappings.put(ILinkage.CPP_LINKAGE_NAME, new PDOMCPPLinkageFactory());
    IStandaloneScannerInfoProvider scannerProvider = new MyStandaloneScannerInfoProvider();
    ILanguageMapper mapper = new MyLanguageMapper();
    IParserLogService log = new MyParserLogService();
    IProgressMonitor monitor = new MyProgressMonitor();
    StandaloneIndexer indexer =
        new StandaloneFastIndexer(writableIndex, converter, linkageFactoryMappings,
            scannerProvider, mapper, log);
    indexer.setTraceStatistics(true);

    indexer.rebuild(files, monitor);
    IWritableIndex i = indexer.getIndex();
    GlobalResultCollector.bindingindex = i;
    i.acquireReadLock();
    try {
      System.out.println("internals");
      for (IIndexFile file : i.getAllFiles()) {
        System.out.printf("Index has file: %s\n", file.toString());
      }
      char[] prefix = {};
      for (IIndexBinding binding : i.findBindingsForPrefix(prefix, false, IndexFilter.ALL, monitor)) {
        System.out.println(binding);
      }
      System.out.println(i);
    } finally {
      i.releaseReadLock();
    }
  }


  // ////////
  // /////// Indexer helper classes
  // ///////////
  static public class MyIndexLocationConverter implements IIndexLocationConverter {
    public IIndexFileLocation fromInternalFormat(String raw) {
      return new MyIndexFileLocation(raw);
    }

    @SuppressWarnings("nls")
    public String toInternalFormat(IIndexFileLocation location) {
      if (location.getFullPath() != null) return location.getFullPath();
      return location.getURI().getSchemeSpecificPart();
    }
  }

  static public class MyIndexFileLocation implements IIndexFileLocation {
    final private String fullPath;

    public MyIndexFileLocation(String fullPath) {
      this.fullPath = fullPath;
    }

    public URI getURI() {
      if (true) throw new RuntimeException();
      return null;
    }

    public String getFullPath() {
      return fullPath;
    }
  }

  static public class MyStandaloneScannerInfoProvider implements IStandaloneScannerInfoProvider {
    // Returns IScannerInfo for the given path
    public IScannerInfo getScannerInformation(String path) {
      return new MyScannerInfo();
    }

    public IScannerInfo getDefaultScannerInformation(int linkageID) {
      return new MyScannerInfo();
    }
  }

  static public class MyScannerInfo implements IScannerInfo {
    public Map<String, String> getDefinedSymbols() {
      return new HashMap<String, String>();
    };

    public String[] getIncludePaths() {
      String[] includePath = new String[] {""};
      return includePath;
    };
  }

  static public class MyLanguageMapper implements ILanguageMapper {
    public ILanguage getLanguage(String file) {
      // maybe use GCC for strict C?
      return GPPLanguage.getDefault();
    }
  }

  static public class MyParserLogService implements IParserLogService {
    public void traceLog(String message) {
      System.out.println("Tracde: " + message);
    }

    public boolean isTracing() {
      return true;
    }
  }

  static public class MyProgressMonitor implements IProgressMonitor {
    public void beginTask(String name, int totalWork) {
      System.out.println("monitor: Beginning task: " + name);
    }

    public void done() {
      System.out.println("monitor: DONE");
    }

    public void internalWorked(double work) {
      System.out.println("monitor: internalWorked: " + work);
    }

    public boolean isCanceled() {
      return false;
    }

    public void setCanceled(boolean value) {
      System.out.println("monitor: setCanceled: " + value);
    }

    public void setTaskName(String name) {
      System.out.println("monitor: setTaskName: " + name);
    }

    public void subTask(String name) {
      System.out.println("monitor: subtask: " + name);
    }

    public void worked(int work) {
      System.out.println("monitor: worked: " + work);
    }
  }

}
