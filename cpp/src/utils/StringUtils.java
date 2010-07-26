package utils;

public class StringUtils {
  
  public static String escapeRegExprString(String str) {
    try{
    str = str.replaceAll("\\(", "\\\\(");
    str = str.replaceAll("\\)","\\\\)");
    str = str.replaceAll("\\.", "\\\\.");
    str = str.replaceAll("\\*", "\\\\*");
    str = str.replaceAll("\\?", "\\\\?");
    str = str.replaceAll("\\+", "\\\\+");
    str = str.replaceAll("\\[", "\\\\[");
    str = str.replaceAll("\\]", "\\\\]");
    str = str.replaceAll("\\{", "\\\\{");
    str = str.replaceAll("\\}", "\\\\}");
    str = str.replaceAll("\\^", "\\\\^");
    str = str.replaceAll("\\|", "\\\\|");
    str = str.replace("$", "\\$");
    //str = str.replaceAll("\\\\", "\\\\\\\\");
    return str;
    } catch (RuntimeException e) {
        System.err.println("failed escaping string " + str );
        e.printStackTrace();
        throw e;
    }
}

}
