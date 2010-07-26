package scripts;

import index.IndexConstants;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;

public class CombineRollingLogs {

	static String DIR =  "ec2_10_2";
	
	public static void main(String[] args) throws Exception{
		
		File logdir = new File(IndexConstants.DATA_ROOT, DIR);
		File[] files = logdir.listFiles( new FilenameFilter() {
			public boolean accept(File dir, String name) {
				if ( name.endsWith(".log") 
						&& name.contains("hadoop") 
						)
					return true;
				return false;
			}
		});
		
		File[] all = logdir.listFiles();
		File distdir = new File( logdir, "comb");
		if (!distdir.exists()) {
			distdir.mkdirs();
		}
		for (File f : files) {
			System.err.println(f.getAbsolutePath());
			FileWriter w = new FileWriter(new File(distdir, f.getName() ));
			for (File t: all) {
				//System.err.println(t.getName());
				if (t.getName().contains(f.getName()) && t.getName().length()>f.getName().length()) {
					copy_entire_file(t, w);
				}
			}
			copy_entire_file(f, w);
			w.close();
		}
		
	}
	public static void copy_entire_file(File f, FileWriter w) throws IOException{
		System.err.println("reading / copying: " + f.getAbsolutePath());
		FileReader r = new FileReader(new File (f.getAbsolutePath()) );
		char[] buf = new char[10*1024*1024];
		int i = r.read(buf);
		while (i!=-1) {
			w.write(buf, 0, i);
			i = r.read(buf);
		}
		r.close();
	}
}
