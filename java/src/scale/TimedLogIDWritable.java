package scale;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.Writable;

public class TimedLogIDWritable implements Writable, Comparable<TimedLogIDWritable> {
	
	
	int logid;
	long ts;
	String node;
	String comment;
	

	public void readFields(DataInput arg0) throws IOException {
		this.logid = arg0.readInt();
		this.ts = arg0.readLong();
		this.node = arg0.readUTF();
		if(arg0.readBoolean()) // has comment
			this.comment = arg0.readUTF();
	}

	public void write(DataOutput arg0) throws IOException {
		arg0.writeInt(logid);
		arg0.writeLong(ts);
		arg0.writeUTF(node);
		if(comment!=null) {
			arg0.writeBoolean(true);
			arg0.writeUTF(comment);
		} else {
			arg0.writeBoolean(false);
		}
	}
	
	public void set(int logid, long ts, String node) {
		this.logid = logid;
		this.ts = ts;
		this.node = node;
	}
	
	public void set(int logid, long ts, String node, String comment) {
		this.logid = logid;
		this.ts = ts;
		this.node = node;
		this.comment = comment;
	}
	
	public TimedLogIDWritable copy(TimedLogIDWritable source) {
		this.logid = source.logid;
		this.ts = source.ts;
		this.node = source.node;
		this.comment = source.comment;
		return this;
	}

	public int compareTo(TimedLogIDWritable o) {
		int timediff= (int)(this.ts-o.ts);
		if (timediff==0) {
			int iddiff= this.logid - o.logid;
			if (iddiff==0) {
				return this.node.compareTo(o.node);
			} else {
				return iddiff;
			}
		} else{
			return timediff;
		}
	}
	
	public String toString() {
		return this.ts+":"+this.logid+":"+this.node+":"+comment;
	}
	
}
