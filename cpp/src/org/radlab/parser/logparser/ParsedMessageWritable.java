package org.radlab.parser.logparser;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;

public class ParsedMessageWritable implements Writable {
	
	
	public static final String[] EMPTY = new String[0];
	long ts;
	int logid;
	int threadid;
	private String lbsvalues[];
	private String dtsvalues[];
	private String lbsnames;
	private String dtsnames;
	
	private String srclocation;
	
	public void readFields(DataInput in) throws IOException {
		ts = in.readLong();
		logid = in.readInt();
		threadid = in.readInt();
		int lblen = in.readInt();
		lbsvalues = new String[lblen];
		for(int i=0; i< lblen; i++) {
		  lbsvalues[i] = in.readUTF();
		}
		dtsvalues = new String[lblen];
        for(int i=0; i< lblen; i++) {
          dtsvalues[i] = in.readUTF();
        }
	}

	public void write(DataOutput out) throws IOException {
		out.writeLong(ts);
		out.writeInt(logid);
		out.writeInt(threadid);
		out.writeInt(lbsvalues.length);
		for(int i=0; i<lbsvalues.length; i++) {
		  out.writeUTF(lbsvalues[i]);
		}
		out.writeInt(dtsvalues.length);
        for(int i=0; i<dtsvalues.length; i++) {
          out.writeUTF(dtsvalues[i]);
        }
	}
	
	public void clear() {
	  ts = -1;
      logid = -1;
      threadid = -1;
      lbsvalues = EMPTY;
      dtsvalues = EMPTY;
      dtsnames = "";
      lbsnames = "";
      srclocation = null;
	}
	
	public String getLabelStr(){
	  StringBuffer sb = new StringBuffer();
	  for(String s: lbsvalues) {
	    sb.append(s).append(";;");
	  }
	  return sb.toString();
	}
	
	public String[] getLabels() {
		if (lbsvalues==null) {
			return EMPTY;
		} else {
		  //System.err.println(lbsvalues);
			return lbsvalues;
		}
	}
	
	public String[] getLabelNames(){
		if (lbsnames==null){
			return EMPTY;
		}
		if (lbsnames.isEmpty()) {
			return EMPTY;
		} else {
			return lbsnames.split(";;");
		}
	}
	
	public String getNumberStr(){
	  
	  StringBuffer sb = new StringBuffer();
      for(String s: dtsvalues) {
        sb.append(s).append(";;");
      }
      return sb.toString();
	}
	
	public String[] getNumbers() {
		if (dtsvalues==null) {
			return EMPTY;
		} else {
			return dtsvalues;
		}
	}
	
	public void setLabels(String[] lbsvalues) {
		this.lbsvalues = lbsvalues;
	}
	
	public void setNumbers(String[] dtsvalues) {
		this.dtsvalues = dtsvalues;
	}
	
	public String[] getNumberNames(){
		if (dtsnames==null) {
			return EMPTY;
		}
		if (dtsnames.isEmpty()) {
			return EMPTY;
		} else {
			return dtsnames.split(";;");
		}
	}

	public String getLbsnames() {
		return lbsnames;
	}

	public void setLbsnames(String lbsnames) {
		this.lbsnames = lbsnames;
	}

	public String getDtsnames() {
		return dtsnames;
	}

	public void setDtsnames(String dtsnames) {
		this.dtsnames = dtsnames;
	}
	
	public String toString() {
		return ts+" TYPE=" +this.logid +" TS=" +this.threadid + " LINE=" +this.srclocation +"\nLBNAMES=" 
		+ this.lbsnames +" LBVALUES="+this.getLabelStr()+"\nDTNAMES="
		+this.dtsnames +" DTVALUES=" +this.getNumberStr()+"\nOPS=";
	}

	public long getTs() {
		return ts;
	}

	public void setTs(long ts) {
		this.ts = ts;
	}

	public int getLogid() {
		return logid;
	}

	public void setLogid(int logid) {
		this.logid = logid;
	}

	public int getThreadid() {
		return threadid;
	}

	public void setThreadid(int threadid) {
		this.threadid = threadid;
	}

  public String getSrclocation() {
    return srclocation;
  }

  public void setSrclocation(String srclocation) {
    this.srclocation = srclocation;
  }
	
  public String toCompactString() {
    return ""+ Math.round(ts/100.0) +";;"+logid +";;"+this.threadid +";;"+this.getLabelStr() +this.getNumberStr();
  }
	
}
