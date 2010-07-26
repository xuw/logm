package data;

import java.io.Serializable;
import java.util.ArrayList;

import data.StringExprElement.SCOPE;
import data.StringExprElement.STATUS;

public class ElementList implements Serializable{
	
	
	public static final int DUPLICATE=2;
	public static final int SUCCESSFUL=0;
	
	public ArrayList<StringExprElement> list;
	public String fileName;
	public int line;
	public int status=SUCCESSFUL;
	
	public ElementList() {
		this.list = new ArrayList<StringExprElement>();
	}
	
	public ElementList(String filename, int line){
		this.list = new ArrayList<StringExprElement>();
		this.fileName = filename;
		this.line = line;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(this.fileName).append(":").append(line).append("::\n");
		for (StringExprElement e : list) {
			sb.append("  ").append(e.toString()).append("\n");
		}
		return sb.toString();
	}
	
	public String getConstantString() {
		if (this.list==null || this.list.size()==0){
			return "";
		}
		StringBuffer sb = new StringBuffer("");
		for (StringExprElement e : list) {
			sb.append(e.getConstantOnly());
		}
		return sb.toString();
	}
	
	public String toRegExpr(boolean gen_final_regexpr) {
		if (this.list==null || this.list.size()==0){
			if (!gen_final_regexpr)
				return "@#@";
			else 
				return "(.*)";
		}
		StringBuffer sb = new StringBuffer("");
		for (StringExprElement e : list) {
			sb.append(e.toRegExpr(gen_final_regexpr));
		}
		return sb.toString();
	}
	
	public String getNameMapString() {
		if (this.list==null || this.list.size()==0){
			return "";
		}
		StringBuffer sb = new StringBuffer("");
		for (StringExprElement e : list) {
			String mapstr = e.getNameMapString();
			if (mapstr.length()==0)
				continue;
			sb.append(mapstr).append(";");
		}
		return sb.toString();
	}
	
	public String getTypeMapString() {
		if (this.list==null || this.list.size()==0){
			return "";
		}
		StringBuffer sb = new StringBuffer("");
		for (StringExprElement e : list) {
			String mapstr = e.getTypeMapString();
			if (mapstr.length()==0)
				continue;
			sb.append(mapstr).append(";");
		}
		return sb.toString();
	}

}
