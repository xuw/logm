import utils.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;


public class SLCTInterface {



  private static final String COMMAND = "/home/xuw/slct/slct -s 10 ";


  /**
   * @param args
   */
  public static void main(String[] args) throws Exception {

    File f = new File("/home/xuw/output/logparser-scratch-bt/");

    for (File ff : f.listFiles()) {
      System.err.println("File " + ff.getName());
      String[] patterns = runSLCT(ff);
      if(patterns.length ==0) {
        System.err.println("unable to find pattern");
      } else {
        for (String s : patterns) {
          System.err.println(s);
        }
      }
    }
  }

  public static String[] runSLCT(File f) throws IOException {

    ArrayList<String> results = new ArrayList<String>();
    ArrayList<Integer> supports = new ArrayList<Integer>();
    
    String slct = COMMAND + f.getAbsolutePath();
    // System.err.println("running command: " + slct);
    Process searchprocess = Runtime.getRuntime().exec(slct);
    BufferedReader r = new BufferedReader(new InputStreamReader(searchprocess.getInputStream()));
    String line = r.readLine();
    while (line != null) {
      line = line.trim();
      if(line.length()==0) {
        line = r.readLine();
        continue;
      }
      if (!line.startsWith("Support: ")) {
        line = line.replaceAll("\\*", "@#@");
        line = StringUtils.escapeRegExprString(line);
        line = line.replaceAll("@#@", "(.*)");
        results.add(line);
        // System.err.println(line.trim());
      } else {
        Integer support = Integer.parseInt( line.substring( line.indexOf(":")+1).trim() );
        supports.add(support);
      }
      line = r.readLine();
    }
    
    
    System.err.println("supports " + Arrays.deepToString(supports.toArray()));

    return results.toArray(new String[results.size()]);
  }

}
