package data;

import java.io.Serializable;
import java.util.ArrayList;

import utils.StringUtils;

public class StringExprElement implements Serializable {

	private static final long serialVersionUID = 501711147400827495L;

	public enum STATUS{ATOMIC, NOTRESOLVED, RSOLVED, CONSTANT, SUBCLASS};
	public enum SCOPE{LOCAL_VAR,FIELD, FIELD_OTHER, METHOD, TO_STRING};
	
	public String javaType;
	public STATUS status;
	public String name;
	public Object value;
	public ArrayList<Object> subclassvalues;
	public SCOPE scope;
	public int pos;
	public int len;
	public String regExpr;
	
	public StringExprElement(String javaType, STATUS status, String name, Object value,
			SCOPE scope, int pos, int len) {
		this.subclassvalues = new ArrayList<Object>();
		this.javaType =javaType;
		this.status = status;
		this.name = name;
		this.value = value;
		this.scope = scope;
		this.pos = pos;
		this.len = len;
	}

	public String toString() {
		String namestr = name==null?"(literal)":name;
		String ret = status +" " +namestr +" " + javaType  + " "
		+ scope +" " + pos +":"+len + " VALUE:[[" + value +"]]";
		if (this.subclassvalues.size()!=0) {
			ret +="\nSUBCLASSES\n";
			for (Object t:this.subclassvalues) {
				ret += t +"\n";
			}
		}
		return ret;
	}
	
	public String getConstantOnly() {
		if (this.status == STATUS.CONSTANT) {
			return this.value.toString();
		} else {
			return "";
		}
	}
	
	public String toRegExpr(boolean gen_final_regexpr) {
		if (this.status==STATUS.CONSTANT) {
			return StringUtils.escapeString( this.value.toString() );
		} else if (this.status==STATUS.ATOMIC || this.status==STATUS.NOTRESOLVED){
			if (gen_final_regexpr) {
			if (this.javaType.equals("long") || this.javaType.equals("int")
						|| this.javaType.equals("java.lang.Integer")
						|| this.javaType.equals("java.lang.Long")) {
					return "([-]?[0-9]+)";
				} else { // other string types
					return "(.*)";
				}
			} else {
				return "(@#@)";
			}
		} else {  // CLASSES -- hope to find toString methods
			if (gen_final_regexpr) {
				return "@#@";
			} else {
				return "(@#@)";
			}
		}
	}
	
	public String getNameMapString() {
		if (this.status==STATUS.CONSTANT){
			return "";
		}
		return this.name;
	}
	
	public String getTypeMapString() {
		if (this.status==STATUS.CONSTANT) {
			return "";
		}
		return this.javaType;
	}
	
}
