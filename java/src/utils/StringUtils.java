package utils;

import index.IndexConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import data.RegExprRecord;

public class StringUtils {
	
	private static Logger LOG = LogFormatter.getLogger(StringUtils.class);
	
	public static RegExprRecord simpleExpand(String logid) {
		RegExprRecord rec = DbUtils.findLogEntry(logid);
		return simpleExpand(rec);
	}
	
	public static RegExprRecord simpleExpand(RegExprRecord rec) {
		if (rec==null) {
			return null;
		}
		rec.RegExpr = rec.RegExpr.replaceAll("@#@", ".*");
		return rec;
	}
	
	public static RegExprRecord[] getExpandedRegExpr(String logid) {
//		return simpleExpand(logid);
		RegExprRecord rec = DbUtils.findLogEntry(logid);
		RegExprRecord[] results = resolveSubclasses(rec);
		if (results.length==1) {
			return expandAll(results);
		} 
		results = expandAllOneLevel(results);
		results = resolveSubclasses2levels(results);
		LOG.info("Number of Record to process after substring processing " + results.length);
		results= expandAll(results);
		for(int i=0; i<results.length; i++) {
			if (results[i].typeMap.startsWith("LL")) {
				results[i].typeMap = results[i].typeMap.substring(2);
			}
			results[i].typeMap = results[i].typeMap.replaceAll(";LL", ";");
		}
		return results;
		//return getExpandedRegExpr(rec);
	}
	
	
	
	private static RegExprRecord[] expandAll(RegExprRecord[] recs) {
		for(int i=0; i<recs.length; i++) {
			LOG.info("Trying to expand: " + recs[i]);
			recs[i] = getExpandedRegExpr(recs[i]);
			LOG.info("Expanded to: " +recs[i] );
		}
		return recs;
	}
	
	private static boolean isNumberType(String cls){
		return cls.equals("int")
		|| cls.equals("long") 
		|| cls.equals("java.lang.Integer")
		|| cls.equals("java.lang.Long")
		|| cls.equals("double")
		|| cls.equals("java.lang.Double");
	}
	
	private static boolean isString(String cls) {
		return cls.equals("java.lang.String");
	}
	
	private static RegExprRecord[] resolveSubclasses2levels(RegExprRecord[] l1results) {
		if (l1results.length==1) {
			LOG.fine("No need to resolve level 2");
			return l1results;
		} else {
			ArrayList<RegExprRecord> ret = new ArrayList<RegExprRecord>();
			for(RegExprRecord r: l1results) {
				LOG.fine("Before L2 expand:: " + r);
				RegExprRecord[] l2results = resolveSubclasses(r);
				for (RegExprRecord rr: l2results){
					LOG.fine("L2 Results:: " + rr);
					ret.add(rr);
				}
			}
			return ret.toArray(new RegExprRecord[ret.size()]);
		}
	}
	
	private static RegExprRecord[] resolveSubclasses(RegExprRecord rec) {
		LOG.fine("Before resolving:: " + rec);
		String[] types = rec.typeMap.split(";");
		ArrayList<String> potentialTypes= new ArrayList<String>();
		// initially contains only the original typemap
		potentialTypes.add(rec.typeMap);
		
		// trying to see if there is any types to expand
		for (int i=0; i<types.length; i++) {
			String cls = types[i];
			if (cls.startsWith("LL")) {
				// already processed at level 1
				continue;
			}
			if (cls.length()==0) {
				continue;
			}
			if (isNumberType(cls) || isString(cls)){
				continue;
			}
			RegExprRecord t = DbUtils.findToStringForClass(cls);
			if (t==null) {
				continue;
			}
			//LOG.fine("t:: " +t);
			RegExprRecord subrec = getExpandedRegExpr(t);
			//LOG.fine("subREC:: " +subrec);
			if (subrec.RegExpr.equals(".*")
					|| subrec.RegExpr.equals("(.*)")) {
				String[] subclasses = DbUtils.findAllSubClasses(cls);
				if (subclasses == null) {
					continue;
				}
				for (String subcls : subclasses) {
					RegExprRecord subclsrec = DbUtils
							.findToStringForClass(subcls);
					if (subclsrec == null) {
						// no toString for subclass, ignore
						continue;
					}
					subclsrec = simpleExpand(subclsrec);
					if (subclsrec.RegExpr.equals(".*")
							|| subclsrec.RegExpr.equals("(.*)")) {
						continue; // no better than super class
					} else {
						//LOG.fine("better subclass: " + subcls + "::::"
						//		+ subclsrec);
						potentialTypes.add( rec.typeMap.replace(cls, subcls) );
					}
				}
			}
		}
		RegExprRecord[] result = new RegExprRecord[potentialTypes.size()];
		for( int i=0; i<potentialTypes.size(); i++) {
			String t = potentialTypes.get(i);
			//LOG.fine("toProcess: potential subtype: " + t);
			RegExprRecord subrecord = rec.getCopy();
			subrecord.typeMap = t;
			result[i]=subrecord;
			LOG.fine("AFTER L2 Expand: " + result[i]);
		}
		return result;
	}
	
	private static RegExprRecord[] expandAllOneLevel(RegExprRecord[] recs) {
		RegExprRecord[] ret = new RegExprRecord[recs.length];
		for(int i=0; i<recs.length; i++) {
			ret[i] = expandSubClassOneLevel(recs[i]);
		}
		return ret;
	}
	
	private static RegExprRecord expandSubClassOneLevel(RegExprRecord rec){
		String[] names = rec.nameMap.split(";");
		String[] types = rec.typeMap.split(";");
		LOG.fine("Before L1 Expand: " + rec);
		StringBuffer nb = new StringBuffer();
		StringBuffer tb = new StringBuffer();
		
		int replacedMarkerCnt = 0;
		for (int i=0; i<types.length; i++) {
			String cls = types[i];
			if (cls.length()==0) {
				replacedMarkerCnt +=1;
				continue;
			}
			
			if (isNumberType(cls) || isString(cls)){
				nb.append(names[i]).append(";");
				tb.append(types[i]).append(";");
				replacedMarkerCnt +=1;
				continue;
			}
			RegExprRecord t = DbUtils.findToStringForClass(cls);
			if (t!=null && !t.RegExpr.equals("@#@") && !t.RegExpr.equals("(@#@)")) {
				String expr = t.RegExpr.replace("@#@", "%^%");
				rec.RegExpr = replaceNth(i-replacedMarkerCnt, rec.RegExpr, "@#@", expr);
				nb.append(names[i]).append(";");
				tb.append("LL"+types[i]).append(";");
				nb.append(t.nameMap);
				tb.append(t.typeMap);
				replacedMarkerCnt +=1;
			} else {
				nb.append(names[i]).append(";");
				tb.append(types[i]).append(";");
			}
		}
		
//		for(int i=0; i<names.length; i++) {
//			if (names[i].length()>0 || types[i].length()>0) {
//				nb.append(names[i]).append(";");
//				tb.append(types[i]).append(";");
//			}
//		}
		rec.RegExpr = rec.RegExpr.replace("%^%", "@#@");
		rec.nameMap = nb.toString();
		rec.typeMap = tb.toString();
		LOG.fine("After Level One Expand:: " + rec);
		return rec;
	}
	
	private static String replaceNth(int index, String src, String search, String replace){
		int cnt =0;
		int lastind =-search.length();
		while(cnt<=index) {
			int ind = src.indexOf(search, lastind+search.length());
			if (ind<0) {
				throw new RuntimeException("cannot find " + index +"-th "+search+" in string:: " + src );
			}
			cnt +=1;
			lastind = ind;
		}
		String ret = src.substring(0, lastind)+replace+src.substring(lastind+search.length());
		return ret;
	}
	
//	public static void main(String[] args) {
//		LOG.info(replaceNth(1, 
//				"abcd @#@ efg @#@hkljl@#@", "@#@", "%%%"));
//	}
	
	public static RegExprRecord getExpandedRegExpr(RegExprRecord rec){
		
		if (rec==null) {
			return null;
		}
		//LOG.fine("original ::: " + rec.RegExpr);
		while (rec.RegExpr.indexOf("@#@") >=0 ) {
			String[] types = rec.typeMap.split(";");
			String[] names = rec.nameMap.split(";");
			StringBuffer resulttype = new StringBuffer();
			StringBuffer resultname = new StringBuffer();
			int cnt=0;
			//LOG.fine("length of types: " + Arrays.deepToString(types) + types.length);
			if (types.length==1 && types[0].length()==0) { // due to a unresolved toString
				rec.RegExpr = rec.RegExpr.replaceAll("@#@", ".*");
				//LOG.fine("replace!! " + rec.RegExpr);
			}
			for (String cls : types) {
				if (cls.length()==0) {
					cnt +=1;
					continue;
				}
				if (cls.startsWith("LL")) {
					resulttype.append( types[cnt] ).append(";");
					resultname.append( names[cnt] ).append(";");
					cnt +=1;
					continue;
				}
				if (isNumberType(cls)) {
					rec.RegExpr = rec.RegExpr.replaceFirst("@#@", "[-]?[0-9]+");
					resulttype.append( types[cnt] ).append(";");
					resultname.append( names[cnt] ).append(";");
					cnt +=1;
					continue;
				}
				if (isString(cls)) {
					rec.RegExpr = rec.RegExpr.replaceFirst("@#@", ".*");
					resulttype.append( types[cnt] ).append(";");
					resultname.append( names[cnt] ).append(";");
					cnt +=1;
					continue;
				}
				RegExprRecord t = DbUtils.findToStringForClass(cls);
				//LOG.info("FOUND " +cls +" " + t);
				if (t!=null) {
					RegExprRecord subrec = getExpandedRegExpr(t);
					
					if (subrec.RegExpr.equals(".*")
							|| subrec.RegExpr.equals("(.*)")) {
						rec.RegExpr = rec.RegExpr.replaceFirst("@#@", ".*");
						resulttype.append( types[cnt] ).append(";");
						resultname.append( names[cnt] ).append(";");
						cnt +=1;

					} else {
						//LOG.fine("sub_expr:: [" 
						//		+ subrec.RegExpr + "] ["
						//		+ subrec.nameMap + "] ["
						//		+ subrec.typeMap + "]");
						// rec.RegExpr.replaceAll("\\\\", "\\\\\\\\");
						subrec.RegExpr = subrec.RegExpr.replaceAll("\\\\",
								"\\\\\\\\");
						if (subrec.RegExpr.equals(".*")) {
							subrec.RegExpr = "(.*)";
						}
						rec.RegExpr = rec.RegExpr.replaceFirst("@#@",
								subrec.RegExpr);
						resulttype.append(types[cnt]).append(";");
						resultname.append(names[cnt]).append(";");
						resulttype.append(t.typeMap);
						resultname.append(t.nameMap);
						cnt += 1;
					}
				} else {
					// cannot find toString method for embeded class
					rec.RegExpr = rec.RegExpr.replaceFirst("@#@", ".*");
					resulttype.append( types[cnt] ).append(";");
					resultname.append( names[cnt] ).append(";");
					cnt +=1;
				}
			}
			rec.nameMap = resultname.toString();
			rec.typeMap = resulttype.toString();
		}
		
		// set correct log level format..
		if (IndexConstants.levelInData) {
			if (rec.level == Level.WARNING.intValue()) {
				rec.RegExpr = "\\[WARNING\\] " + rec.RegExpr;
			} else if (rec.level == Level.SEVERE.intValue()) {
				rec.RegExpr = "\\[SEVERE\\] " + rec.RegExpr;
			}
		}
		rec.RegExpr = rec.RegExpr.replace("\n", "");
		return rec;
	}
	
	public static String escapeString(String str) {
		try{
		str = str.replaceAll("\\(", "\\\\(");
		str = str.replaceAll("\\)","\\\\)");
		str = str.replaceAll("\\.", "\\\\.");
		str = str.replaceAll("\\*", "\\\\*");
		str = str.replaceAll("\\+", "\\\\+");
		str = str.replaceAll("\\[", "\\\\[");
		str = str.replaceAll("\\]", "\\\\]");
		str = str.replaceAll("\\{", "\\\\{");
		str = str.replaceAll("\\}", "\\\\}");
		str = str.replaceAll("\\^", "\\\\^");
		str = str.replace("$", "\\$");
		//str = str.replaceAll("\\\\", "\\\\\\\\");
		return str;
		} catch (RuntimeException e) {
			LOG.warning("failed on string " + str);
			throw e;
		}
	}
	
	public static void main(String[] args) {
		
		LOG.info ( escapeString("threw exception for reference ^ $ 3") );
	}
	
	
	public static boolean isNumeric(String type) {
		if (type.equals("int") ) return true;
		if (type.equals("long") ) return true;
		if (type.equals("float") ) return true;
		if (type.equals("double") ) return true;
		if (type.equals("java.lang.Integer") ) return true;
		if (type.equals("java.lang.Long") ) return true;
		if (type.equals("java.lang.Float") ) return true;
		if (type.equals("java.lang.Double") ) return true;
		else return false;
	}
	
	
	static final Pattern numstr = Pattern.compile( "^[0-9]+$" );
	private static final String PADDINGSTR = "00000000000000000000";
	
	public static String padInteger(String intStr) {
		//System.out.println("padding: " + intStr);
		if (numstr.matcher(intStr).find()) {
			String pad = PADDINGSTR.substring(0,PADDINGSTR.length()-intStr.length());
			//System.out.println("ret: " + pad+intStr);
			return pad + intStr;
		} else {
			return intStr;
		}
	}

	public static String dePadInteger(String intStr) {
		if (intStr==null)
			return "";
		String ret = intStr.replaceFirst("^0+", "");
		if (intStr.length()!=0 && ret.length()==0) {
			return "0";
		}
		return ret;
	}
	
	public static String limitStrLen(String str, int len) {
		if (str.length()<=len)
			return str;
		else 
			return str.substring(0,len-1);
	}
	
	public static String removeLongWords(String src) {
		
		String[] tt = src.split(" ");
		StringBuffer sb = new StringBuffer();
		for(String t: tt) {
			if (t.length()>30)
				continue;
			sb.append(t).append(" ");
		}
		return sb.toString();
	}
	
	
}
