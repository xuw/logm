package online;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FormatConvert {
	
	public static void main(String[] args) throws Exception{
		
		BufferedReader in = new BufferedReader(new FileReader(new File("data/offline/rawTFVector.txt")) );
		
		ArrayList<String> blocks = new ArrayList<String>();
		String line = in.readLine();
		
		Pattern p = Pattern.compile( "%\\s+=+([^=]*)=+.*" );
		int cnt=0;
		String tag="";
		//outer:
		while(line !=null) {
			line = line.trim();
			if (line.length() ==0) {
				line =in.readLine();
				continue;
			}
			if (!line.startsWith("%")) {
				System.err.println("123456 "+line +" " +tag);
				line = in.readLine();
				continue; // actual data lines
			}
			Matcher m = p.matcher(line);
			if (m.matches()) {
				tag = "%" + m.group(1) ;
			}
			line = in.readLine();
		}
		in.close();
		
	}
	
	
}
