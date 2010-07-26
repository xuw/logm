package data;

public class RegExprRecord {
	
	public String RegExpr;
	public String constStr;
	public String nameMap;
	public String typeMap;
	public String logid;
	public int level;
	public String caller_method;
	
	
	public RegExprRecord(String regExpr, String constStr, String nameMap ,String typeMap, String logid, int level, String caller_method) {
		super();
		RegExpr = regExpr;
		this.constStr = constStr;
		this.nameMap = nameMap;
		this.typeMap = typeMap;
		this.logid = logid;
		this.level = level;
		this.caller_method = caller_method;
	}
	
	public String toString() {
		return "[" +RegExpr +"] ["+ nameMap +"] ["+ typeMap+"]";
	}
	
	public RegExprRecord getCopy() {
		return new RegExprRecord(this.RegExpr, this.constStr, this.nameMap, this.typeMap, this.logid,this.level, this.caller_method);
	}
	
}
