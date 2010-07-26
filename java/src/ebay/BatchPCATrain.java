package ebay;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;

import pca.PCATrain;

public class BatchPCATrain {
	
	
	static File datadir = new File("data/ebay/MCV");
	public static void main(String[] args) throws Exception{
		
		File[] allMCVFiles = datadir.listFiles(new FileFilter(){
			public boolean accept(File pathname) {
				if(pathname.getName().endsWith(".pca")) {
					return false;
				} else {
					return true;
				}
			}
		});
		
		for(File f: allMCVFiles) {
			
			System.err.println("Training PCA model for " + f);
			
			PCATrain.logfile = f;
			PCATrain.outFileName = f.getName()+".pca";
			PCATrain.main(new String[0]);
			
			
		}
		
	}

}
