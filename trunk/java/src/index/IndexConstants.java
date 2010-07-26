package index;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.regex.Pattern;

import conf.ConfUtil;

public interface IndexConstants {
	
	static int LOGTYPE = ConfUtil.getConfig().getInt("index.logtype");
	
	static final int NUM_RESULT_CONSIDERED = 8;
	
	static final boolean DO_HARD_MATCH = true;
	
	// omap pattern
	// static final Pattern standardLogPattern = Pattern.compile("");
	
	// odis pattern
	
	static String standardLogPatternStr = ConfUtil.getConfig().getStringArray("logformat")[LOGTYPE];
	static Pattern standardLogPattern = Pattern.compile(standardLogPatternStr);
	
	static String parserClassName = ConfUtil.getConfig().getStringArray("parserClass")[LOGTYPE];
	
	static DateFormat tsformat = new SimpleDateFormat(ConfUtil.getConfig().getStringArray("tsformat")[LOGTYPE]);
	static int dayPos = Integer.parseInt( ConfUtil.getConfig().getStringArray("daypos")[LOGTYPE] ) ; 
	static int timePos = Integer.parseInt( ConfUtil.getConfig().getStringArray("timepos")[LOGTYPE] ) ; 
	static int dataPos = Integer.parseInt( ConfUtil.getConfig().getStringArray("datapos")[LOGTYPE] ) ;
	static int threadidPos = Integer.parseInt(ConfUtil.getConfig().getStringArray("threadidpos")[LOGTYPE]);
	static boolean levelInData = Boolean.parseBoolean( ConfUtil.getConfig().getStringArray("levelInData")[LOGTYPE] ) ; 
	
	static boolean keepUnMatched = ConfUtil.getConfig().getBoolean("logMatch.keepUnMatched", false);
	
	static boolean debug = ConfUtil.getConfig().getBoolean("debug");
	
	
	static final Pattern exceptionHead = Pattern.compile("([^\\s]+((Exception)|(Error))): (.*)");
	//static final Pattern exceptionHead = Pattern.compile("((.+Exception).*)|(\\s+at (.+)\\(.*\\))|(\\s*Caused by:.*)|(\\s+\\.\\.\\. [0-9]* more)");
	//static final Pattern errorHead = Pattern.compile("((.+Error).*)|(\\s+at (.+)\\(.*\\))|(\\s*Caused by:.*)|(\\s+\\.\\.\\. [0-9]* more)");
	static final Pattern exceptionStack = Pattern.compile("(\\s+at (.+)\\(.*\\))|(\\s*Caused by:.*)|(\\s+\\.\\.\\. [0-9]* more)");
	static final Pattern exceptionPattern = Pattern.compile("(.+)Exception.*(\\s+at .*)+", Pattern.DOTALL | Pattern.MULTILINE);
	
	
	public static File DATA_ROOT = new File( ConfUtil.getConfig().getString("dataRoot") );
	
	public static String[] LOG_FILE_NAMES = ConfUtil.getConfig().getStringArray("data.logFile");
//	public static File LOG_FILE = new File( DATA_ROOT, ConfUtil.getConfig().getString("data.logFile") );
	
	public static File SRC_INDEX_DIR = new File( DATA_ROOT, ConfUtil.getConfig().getString("index.indexDir", "SrcIndex") );
	public static File LOG_DATA_INDEX_DIR= new File (DATA_ROOT, ConfUtil.getConfig().getString("data.logIndexDir", "LogDataIndex"));
	public static File TMP_DIR = new File(DATA_ROOT, ConfUtil.getConfig().getString("data.tmpDir", "tmpFiles"));
	public static File VIS_OUTPUT_DIR = new File(DATA_ROOT, ConfUtil.getConfig().getString("data.tmpDir", "visOutput"));
	
	public static String[] LOG_DB_NAMES = ConfUtil.getConfig().getStringArray("data.logDBName");
	//public static String LOG_DB_NAME = LOG_DB_NAMES.length==0?"SampleLog":LOG_DB_NAMES[0];
	
	//public static final File INDEX_DIR = new File("c:\\Users\\xuw\\LogMine\\src_index");
	//public static String LOG_FILE ="c:\\Users\\xuw\\console.log.new";
	//public static String LOG_FILE ="c:\\Users\\xuw\\exp_test_log.txt";
	
	//public static final File INDEX_DIR = new File("/scratch/xuw/fsindex");
	//public static String LOG_FILE ="/scratch/xuw/log/fs0";
	
}
