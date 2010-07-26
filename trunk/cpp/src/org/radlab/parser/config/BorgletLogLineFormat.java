package org.radlab.parser.config;

public class BorgletLogLineFormat implements BasicLogLineFormat {

  @Override
  public int getLevelPos() {
    return 1;
  }

  @Override
  public String getLineRegExpr() {
    return "([A-Z]+)([0-9]+ [0-9\\.:]+)\\s+([0-9]+) ([^\\] ]+:[0-9]+)\\] (.*)";
  }

  @Override
  public int getMessagePos() {
    return 5;
  }

  @Override
  public int getThreadIdPos() {
    return 3;
  }

  @Override
  public String getTimeStampFormat() {
    return "MMdd HH:mm:ss.SSSSSS";
  }

  @Override
  public int getTimeStampPos() {
    return 2;
  }

  @Override
  public boolean isLevelInData() {
    return false;
  }
  
  public int getSourceLocationPos() {
    return 4;
  }

}
