package mine;

import index.IndexConstants;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;

public class GenReport {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception{
		
		File keyFile = new File(IndexConstants.TMP_DIR,"txtmap.txt");
		
		HashMap<Integer, String> idmap = new HashMap<Integer, String>();
		
		BufferedReader keyr = new BufferedReader(new FileReader(keyFile) );
		String line = keyr.readLine();
		while (line!=null) {
			String[] t = line.split(" \\| ");
			int id = Integer.parseInt(t[0]);
			idmap.put(id, t[2]+" " +t[1]);
			line = keyr.readLine();
		}
		keyr.close();
		
		File clusterCntFile = new File(IndexConstants.TMP_DIR,"cluster_cnt");
		int clusterid =0;
		BufferedReader cntr = new BufferedReader(new FileReader(clusterCntFile));
		line = cntr.readLine();
		while (line !=null) {
			if (line.trim().length()==0) {
				System.err.println("============== Cluster " + clusterid +" ============");
				clusterid +=1;
				line = cntr.readLine();
				continue;
			}
			String[] t = line.split(" ");
			int id = Integer.parseInt(t[0]);
			System.err.println(idmap.get(id) +" " + t[1]);
			line = cntr.readLine();
		}
		cntr.close();
		
		
		System.err.println("======================");
		
		File kgramAbnormalFile = new File(IndexConstants.TMP_DIR, "abnormal_gram");
		BufferedReader kgramr = new BufferedReader(new FileReader(kgramAbnormalFile));
		line = kgramr.readLine();
		int lastseq =-1;
		while(line !=null) {
			String [] t = line.split(",");
			int id = Integer.parseInt(t[1]);
			int seq = Integer.parseInt(t[0]);
			if (seq==lastseq+1) { // continuous
			} else {
				System.err.println();
			}
			lastseq = seq;
			//
			if (true) {
				int abnormal = Integer.parseInt(t[2]);
				if (abnormal==1) {
					System.err.println(t[0] +" " + idmap.get(id) +" <==");
				} else {
					System.err.println(t[0] +" " + idmap.get(id));
				}
			}
			line = kgramr.readLine();
		}
		
	}

}
