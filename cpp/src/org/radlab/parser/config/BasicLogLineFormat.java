package org.radlab.parser.config;

public interface BasicLogLineFormat {
  
  public String getLineRegExpr();
  
  /*
   * this.dayPos = job.getInt(CONF_DAYPOS, -1);
        this.timePos = job.getInt(CONF_TIMEPOS, -1);
        this.dataPos = job.getInt(CONF_DATAPOS, -1);
        this.threadidPos = job.getInt(CONF_THREADPOS, -1);
        this.levelInData = job.getBoolean(CONF_LEVELINLOG, false);
   */
  public String getTimeStampFormat();
  
  public int getTimeStampPos();
  
  public int getThreadIdPos();
  
  public boolean isLevelInData();
  
  public int getLevelPos();
  
  public int getMessagePos();
  
  public int getSourceLocationPos();

}
