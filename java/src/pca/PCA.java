package pca;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.Matrices;
import no.uib.cipr.matrix.Matrix;
	
public class PCA {
	
	public static final int NO_NORMALIZATION=0;
	public static final int NORMALIZE_TF=1;
	public static final int NORMALIZE_TFIDF=2;
	
	int numpc = 0;
	double threshold =0.0;
	Matrix C;
	
	double[] col_mean;
	
	int normalization_method = 0; // 0= not using; 1= normalize TF; 2= normalized IDF
	TFIDF tfidf = null;
	
	public PCA(int normalization_method) {
		this.normalization_method = normalization_method;
	}
	
	public PCA(int numpc, double thrshold, Matrix C) {
		this.numpc = numpc;
		this.threshold = thrshold;
		this.C = C;
	}
	
	public PCA(String filename) throws Exception {
		readFile(filename);
	}
	
	public void writeFile(String filename) throws IOException{
		ObjectOutputStream dataout = new ObjectOutputStream(new FileOutputStream(filename) );
		dataout.writeInt(numpc);
		dataout.writeDouble(threshold);
		dataout.writeObject(Matrices.getArray(C) );
		dataout.writeInt(normalization_method);
		if (normalization_method!=0) {
			dataout.writeObject(tfidf);
		}
		dataout.writeObject(col_mean);
		dataout.close();
	}
	
	public void readFile(String filename) throws IOException, ClassNotFoundException{
		ObjectInputStream in = new ObjectInputStream(new FileInputStream(filename));
		this.numpc = in.readInt();
		this.threshold = in.readDouble();
		double[][] carray = (double[][]) in.readObject();
		this.C = new DenseMatrix(carray);
		this.normalization_method= in.readInt();
		if (this.normalization_method!=0) {
			this.tfidf = (TFIDF) in.readObject();
		}
		this.col_mean = (double[]) in.readObject();
		in.close();
	}
	
}
