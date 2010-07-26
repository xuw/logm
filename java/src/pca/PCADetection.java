package pca;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;

import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.Vector;

public class PCADetection {
	
	PCA pca;
	
	public static File logfile = new File("data\\online\\rawTFVector.txt");
	public static File modelFile = new File(logfile.getParentFile(), "pca_matrices");
	
	public static void main(String[] args) throws Exception{
		
		PCADetection detect = new PCADetection( modelFile.getAbsolutePath() );
		
		//System.err.println("Threshold = " + detect.pca.threshold + " num_pc=" + detect.pca.numpc +" nomalization" + detect.pca.normalization_method);
		//MatrixUtils.prettyPrint(detect.pca.C);
		
		BufferedReader in = new BufferedReader(new FileReader(logfile));
		String line = in.readLine().trim();
		int linecnt =0;
		int abnormalcnt =0;
		while (line!=null) {
			line = line.trim();
			linecnt +=1;
			if (line.startsWith("%") || line.length()==0) {
				line = in.readLine().trim();
				continue; // skip comments
			}
			//System.err.println(line);
			DetectionResult result = detect.isAbnormal(line);
			if(result.isAbnormal()) {
				//System.err.println( result );
				abnormalcnt +=1;
			}
			line = in.readLine();
		}
		
		System.err.println("total line=" + linecnt +" abnormal="+abnormalcnt);
		
	}
	
	public PCADetection(String paramfilename) throws Exception{
		this.pca = new PCA(paramfilename); 
		System.err.println("loaded PCA parameters from " + paramfilename);
		System.err.println("Threshold = " + pca.threshold + " num_pc=" + pca.numpc +" nomalization=" + pca.normalization_method);
		System.err.println("== col mean =");
		MatrixUtils.prettyPrint(pca.col_mean);
	}
	
	public DetectionResult isAbnormal(String row) {
		String[] cells = row.split("\\s");
		
		String identifier = "unknown";
		double[] r = new double[pca.C.numRows()];
		for(int i=1; i<cells.length; i++) {
			if(cells[i].length()==0 || cells[i].startsWith("%")) {
				identifier = cells[i];
				continue;
			} else {
				r[i-1]=Double.parseDouble(cells[i]);
			}
		}
		return isAbnormal(Long.parseLong(cells[0]), r, identifier);
	}
	
	public DetectionResult isAbnormal(long timestamp, double[] row, String identifier) {
		Vector y = new DenseVector(row);
		if (pca.normalization_method!=0) {
			switch (pca.normalization_method) {
			case 1:
				pca.tfidf.normalizeLength(y);
				break;
			case 2:
				pca.tfidf.applyTFIDF(y);
				break;
			default:
			}
		}
		
		for(int i=0; i<y.size(); i++) {
			y.set(i, y.get(i)-pca.col_mean[i]);
		}
		
		Vector ya = new DenseVector(new double[row.length]);
		this.pca.C.mult(y, ya);
		double spe = ya.norm(Vector.Norm.Two);
		spe *= spe;
		
		//MatrixUtils.prettyPrint(row);
		//System.err.println("SPE="+spe + " "+ (spe>this.pca.threshold?"abnormal":"normal"));
		DetectionResult ret = new DetectionResult();
		ret.setAbnormal(spe > this.pca.threshold);
		ret.setTs(timestamp);
		ret.setSpe(spe);
		ret.setThreshold(this.pca.threshold);
		ret.setIdentifier(identifier);
		return ret;
	}
	

}
