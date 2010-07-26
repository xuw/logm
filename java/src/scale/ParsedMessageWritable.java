package scale;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;

public class ParsedMessageWritable implements Writable {
	
	
	private static final String[] EMPTY = new String[0];
	long ts;
	int logid;
	int threadid;
	private String lbsvalues;
	private String dtsvalues;
	private String lbsnames;
	private String dtsnames;
	private String lbstypes;
	private String dtstypes;
	private String methodid;
	
	public void readFields(DataInput in) throws IOException {
		ts = in.readLong();
		logid = in.readInt();
		threadid = in.readInt();
		lbsvalues = in.readUTF();
		dtsvalues = in.readUTF();
		methodid = in.readUTF();
	}

	public void write(DataOutput out) throws IOException {
		out.writeLong(ts);
		out.writeInt(logid);
		out.writeInt(threadid);
		out.writeUTF(lbsvalues);
		out.writeUTF(dtsvalues);
		out.writeUTF(methodid);
	}
	
	public String getLabel(){
		return lbsvalues;
	}
	
	public String[] getLabels() {
		if (lbsvalues==null) {
			return EMPTY;
		}
		if (lbsvalues.isEmpty()) {
			return EMPTY;
		} else {
			return lbsvalues.split(";;");
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
	
	public String getNumber(){
		return lbsvalues;
	}
	
	public String[] getNumbers() {
		if (dtsvalues==null) {
			return EMPTY;
		}
		if (dtsvalues.isEmpty()) {
			return EMPTY;
		} else {
			return dtsvalues.split(";;");
		}
	}
	
	public void setLabels(String lbsvalues) {
		this.lbsvalues = lbsvalues;
	}
	
	public void setNumbers(String dtsvalues) {
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
		return "TS=" + ts+" LID=" +this.logid +" THREAD=" +this.threadid 
		+" LBN=" + this.lbsnames +" LBT=" + this.lbstypes +" LBV="+this.lbsvalues
		+" NUMN=" +this.dtsnames + " NUMT=" +this.dtstypes +" NUMV=" +this.dtsvalues;
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

	public String getMethodid() {
		return methodid;
	}

	public void setMethodid(String methodid) {
		this.methodid = methodid;
	}

	public String getLbstypes() {
		return lbstypes;
	}

	public void setLbstypes(String lbstypes) {
		this.lbstypes = lbstypes;
	}

	public String getDtstypes() {
		return dtstypes;
	}

	public void setDtstypes(String dtstypes) {
		this.dtstypes = dtstypes;
	}
	
	
	
}
