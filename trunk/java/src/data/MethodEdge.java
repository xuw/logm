package data;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

import ast.MethodCallDetectorVisitor;

import utils.DbUtils;
import utils.LogFormatter;

public class MethodEdge implements Comparable<MethodEdge>,Serializable{
	
	
	private static Logger LOG = LogFormatter.getLogger(MethodEdge.class);
	
	private static final long serialVersionUID = 1L;

	public MethodDesc caller;
	public MethodDesc callee;
	public int linenumber;
	public boolean inloop;

	public MethodEdge(MethodDesc caller, MethodDesc callee, int linenumber, boolean inloop) {
		this.caller = caller;
		this.callee = callee;
		this.linenumber = linenumber;
		this.inloop = inloop;
	}

	public int compareTo(MethodEdge arg0) {
		int t = this.caller.compareTo(arg0.caller);
		if (t==0) {
			return this.callee.compareTo(arg0.callee);
		} else {
			return t;
		}
	}


	public boolean equals(Object o) {
		MethodEdge m = (MethodEdge) o;
		return this.caller.equals(m.caller) && this.callee.equals(m.callee);
	}

	public String toString(){
		return this.caller+" -> " +this.callee +" ("+this.linenumber+")";
	}
	
	public int writeToDB() {
		this.caller.writeToDB();
		this.callee.writeToDB();
		int callerid = this.caller.findMethodAutoId();
		int calleeid = this.callee.findMethodAutoId();
		
		LOG.fine("inserting edge.. " + callerid +";"+calleeid+";"+linenumber);
		return DbUtils.insertMethodEdge(callerid, calleeid, this.linenumber, this.inloop);
	}
	
	public void readFromDb() {
		
	}

}
