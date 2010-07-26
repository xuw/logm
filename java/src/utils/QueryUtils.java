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
	
}
