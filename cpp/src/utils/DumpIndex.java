package utils;

import Lucene2CSV.Lucene2CSV;

public class DumpIndex {

          public static void main(String args[])throws Exception
          {
                  /**
                  * Path of lucene-index
                  */
                  String indexDir= "/home/xuw/output/srcindex-bt";
                  /**
                  * Path of CSV file in which data of lucene index to be written
                  */
                  String fileDir="/home/xuw/output/ttt";
                  /**
                   * constructor call to process lucene index
                   */
                  Lucene2CSV lCSV=new Lucene2CSV(indexDir,fileDir);
          }

}
