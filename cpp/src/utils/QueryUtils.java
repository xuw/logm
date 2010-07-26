package utils;

public class QueryUtils {
	
	public static String escapeQueryString(String str) {
		str = str.replaceAll(":", "\\\\:");
		str = str.replaceAll("\\[", "\\\\[");
		str = str.replaceAll("\\]", "\\\\]");
		str = str.replaceAll("\\(", "\\\\(");
		str = str.replaceAll("\\)", "\\\\)");
		str = str.replaceAll("\\{", "\\\\{");
		str = str.replaceAll("\\}", "\\\\}");
		str = str.replaceAll("\"", "\\\"");
		str = str.replaceAll("\\'", "\\\\'");
		str = str.replaceAll("\\.", "\\\\.");
		str = str.replaceAll("\\-", "\\\\-");
		str = str.replaceAll("\\*", "\\\\*");
		str = str.replaceAll("\\?", "\\\\?");
		str = str.replaceAll("\\!", "\\\\!");
		str = str.replaceAll("\\~", "\\\\~");
		str = str.replaceAll("\\+", "\\\\+");
		str = str.replaceAll("\\^", "\\\\^");
		str = str.replaceAll("NOT", "");
		str = str.replaceAll("AND", "");
		str = str.replaceAll("OR", "");
		
		// likely to be a path name
		//str = str.replaceAll("([^ ]*/)+", "");
		// likely to be a java package name
		//str = str.replaceAll("([^ ]*\\.)+","");
		return str;
	}

	public static String unEscapeQueryString(String str) {
		str = str.replaceAll("\\\\", "");
		return str;
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
	
	
	public static String removePaths(String s) {
	  return s.replaceAll("(/[^/\\s:]+)+", "");
	}
}
