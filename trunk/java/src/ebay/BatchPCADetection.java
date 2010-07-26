package ebay;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.ObjectInputStream;

import pca.PCADetection;

public class BatchPCADetection {
	
	
	static File datadir = new File("data/ebay/MCV");
	
	public static void main(String[] args) throws Exception{
		
		File[] allPCAModel = datadir.listFiles(new FileFilter(){
			public boolean accept(File pathname) {
				if(pathname.getName().endsWith(".pca")) {
					return true;
				} else {
					return false;
				}
			}
		});
		
		for(File f: allPCAModel) {
			System.err.println("======="+f.getName()+"======");
			//ObjectInputStream in = new ObjectInputStream(new FileInputStream(f));
			PCADetection.modelFile = f;
			System.err.println(f.getName());
			String datafilename = f.getName().split("\\.")[0];
			PCADetection.logfile = new File(datadir, datafilename);
			PCADetection.main(args);
		}
		
	}

}
