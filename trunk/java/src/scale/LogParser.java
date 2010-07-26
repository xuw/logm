package scale;

import org.apache.hadoop.mapred.Reporter;

public interface LogParser {
	
	public boolean parseOneLine( String line, ParsedMessageWritable buf, Reporter reporter );
	public long getLineCnt();
	
}
