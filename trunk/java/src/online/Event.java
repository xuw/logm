package online;

import java.io.Serializable;

public class Event implements Serializable{
	
	public String id;
	public long ts;
	public int msgtype;
	
	public Event(String id, long ts, int msgtype) {
		super();
		this.id = id;
		this.ts = ts;
		this.msgtype = msgtype;
	}
	
	public String toString() {
		return this.msgtype +":" + this.ts;
	}
}
