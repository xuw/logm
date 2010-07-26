package org.radlab.parser.config;

public class GFSLogLineFormat extends GoogleLogLineFormat {



  @Override
  public String getLineRegExpr() {
    return "([A-Z]+)([0-9]{4} [0-9]{6}) ([0-9a-z]+) ([^\\] ]+:[0-9]+)\\] (.*)";
  }

  @Override
  public String getTimeStampFormat() {
    return "MMdd HHmmss";
  }
  
  public int getThreadIdIntBase() {
    return 16;
  }

}
