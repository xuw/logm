package data;

import java.io.Serializable;
import java.sql.SQLException;
import java.sql.Statement;

import utils.DbUtils;

public class MethodDesc implements Serializable, Comparable<MethodDesc>{
	
	public String methodclass;
	public String methodname;
	public String params;
	public String returntype;
	private int dbRecId=-1;
	
	public MethodDesc(String methodclass, String methodname, String params,
			String returntype, int dbRecId) {
		super();
		this.methodclass = methodclass;
		this.methodname = methodname;
		this.params = params;
		this.returntype = returntype;
		this.dbRecId = dbRecId;
	}
	
	public MethodDesc(String methodclass, String methodname, String params,
			String returntype) {
		this(methodclass, methodname, params, returntype, -1);
	}
	
	@Override
	public boolean equals(Object arg0) {
		MethodDesc th = (MethodDesc) arg0;
		return this.methodclass.equals(th.methodclass) 
		&& this.methodname.equals(th.methodname)
		&& this.params.equals(th.params);
	}

	public int compareTo(MethodDesc o) {
		MethodDesc th = (MethodDesc) o;
		int t = this.methodclass.compareTo(th.methodclass);
		if (t==0) {
			return this.methodname.compareTo(th.methodname);
		} else 
			return t;
	}
	
	public String toString() {
		return this.methodclass+"."+this.methodname;
	}
	
	public void writeToDB() {
			DbUtils.insertMethod(methodclass, methodname, params, returntype);
	}
	
	public int findMethodAutoId() {
		if (this.dbRecId!=-1) {
			return this.dbRecId;
		}
		this.dbRecId = DbUtils.findMethodId(methodclass, methodname, params);
		return this.dbRecId;
	}
	
}
