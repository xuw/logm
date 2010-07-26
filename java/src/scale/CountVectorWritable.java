package scale;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;

public class CountVectorWritable implements Writable {
	
	int[] data;
	int size;
	
	long tsmin;
	long tsmax;
	
	public CountVectorWritable() {
		this.data = new int[0];
		this.size = 0;
		tsmin=0;
		tsmax=0;
	}
	
	public CountVectorWritable(int dim) {
		this.data = new int[dim];
		this.size = dim;
	}
	
	public CountVectorWritable(int[] data) {
		this.data = new int[data.length];
		System.arraycopy(data, 0, this.data, 0, data.length);
		this.size = data.length;
	}
	
	public void clear() {
		for(int i=0; i< data.length; i++) {
			data[i] =0;
		}
		// does not change size
		tsmin =0;
		tsmax =0;
	}
	
	

	public int get(int i) {
		return data[i];
	}
	
	public void inc(int i) {
		data[i] +=1;
	}
	
	public void inc(int i, int inc) {
		data[i] += inc;
	}
	
	public void add(CountVectorWritable v) {
		if (v.data.length != this.data.length) 
			throw new RuntimeException("wrong count vector size!! ");
		for(int i=0; i<this.data.length; i++) {
			this.data[i]  += v.data[i];
		}
	}
	
	public void DFcnt(CountVectorWritable v) {
		if (v.data.length != this.data.length) 
			throw new RuntimeException("wrong count vector size!! ");
		for(int i=0; i<this.data.length; i++) {
			if (v.data[i] > 0) {
				this.data[i]  += 1;
			}
		}
	}
	
	public void readFields(DataInput in) throws IOException {
		int len = in.readInt();
		this.size = len;
		if (data.length != len) {
			data = new int[len];
		} 
		
		for (int i=0; i<data.length; i++) {
			data[i] = in.readInt();
		}
		
		this.tsmin = in.readLong();
		this.tsmax = in.readLong();
		
	}

	public void write(DataOutput out) throws IOException {
		out.writeInt(this.size);
		for (int i=0; i<this.size; i++) {
			out.writeInt(data[i]);
		}
		out.writeLong(tsmin);
		out.writeLong(tsmax);
	}
	
	public String toString() {
		
		StringBuffer buf = new StringBuffer();
		buf.append(tsmin).append(" ");
		buf.append(tsmax).append(" ");
		for (int i=0; i<this.size; i++) {
			buf.append(data[i]).append(" ");
		}
		return buf.toString();
	}
	
	public void increaseSize(int newsize) {
		if (newsize <= size) // cannot decrease 
			return;
		this.size = newsize;
		if (data.length <= newsize) {  // double array
			int[] newdata = new int[newsize*2];
			System.arraycopy(data, 0, newdata, 0, data.length);
			this.data = newdata;
		}
	}

}
