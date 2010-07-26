package utils;

import java.io.File;
import java.util.List;

public class DirectoryUtils {
  
  public static void processDirRecurse(File rootdir, List<String> resultlist) {
    if (rootdir.isDirectory()) {
      System.err.println("entering " + rootdir);
      File[] children = rootdir.listFiles();
      for (File child : children) {
        if (!child.getName().matches(".*out")) {
          processDirRecurse(child, resultlist);
        } else {
          // skip
        }
      }
      System.err.println("exiting " + rootdir);
    } else {
      if (rootdir.getName().endsWith(".cc") || rootdir.getName().endsWith(".c")
          || rootdir.getName().endsWith(".h")) {
        resultlist.add(rootdir.getAbsolutePath());
      }

    }
  }

}
