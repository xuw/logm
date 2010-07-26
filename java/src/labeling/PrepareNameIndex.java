package labeling;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;

public class PrepareNameIndex {

	
	private static final File FILEROOT = new File( "C:/Users/xuw/mscipts/pca_new/data/200nodes");
	
	public static void main(String[] args) throws Exception{
		BufferedReader reader = new BufferedReader(new FileReader(new File(FILEROOT, "rawTFVector.txt")));
		PrintWriter writer = new PrintWriter(new FileWriter(new File(FILEROOT, "nameIndex.txt")));
		
		String t = reader.readLine();
		int cnt =0;
		int outcnt =0;
		while (t!=null) {
			if (t.startsWith("%")){
				;
			} else if (t.trim().length()==0){
				;
			}
			else {
				String[] s = t.split(" %");
				if (s[1].length() >25) {
					s[1] = s[1].substring(0,25);
				}
				writer.println(s[1]);
				outcnt +=1;
			}
			t= reader.readLine();
			cnt +=1;
		}
		
		reader.close();
		writer.close();
		System.err.println("read " + cnt +"; write " +outcnt);
	}

}
