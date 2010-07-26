package online;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class PrintTimeOutSequences {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception{
		BufferedReader in = new BufferedReader(new FileReader(
				new File(ConfigParam.DATASET_DIR,"nonpattern_matrix.txt")));
		String line = in.readLine();
		while(line !=null) {
			System.err.println( KeymapUtils.strToEventStr(line) );
			line = in.readLine();
		}
	}

}
