package pca;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;

import online.ConfigParam;

import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.EVD;
import no.uib.cipr.matrix.Matrices;
import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.NotConvergedException;

public class PCATrain {

	
//	public static int numpc;
//	public static double threshold;
//	public static Matrix C;
	
	//static File logfile = new File("data\\online\\rawTFVector.txt");
	//static String outFileName = "pca_matrices";
	//static File logfile = new File("data\\online\\patterns_pca_vector");
	public static File logfile = new File( ConfigParam.DATASET_DIR, "nonpattern_matrix.txt");
	public static String outFileName = "pca_matrices_nonpattern";
	
	public static double alpha = ConfigParam.pca_alpha;
	public static double dominate_space = ConfigParam.pca_dominant_space;
	public static int normalization = ConfigParam.normalization;
	
	public static void main(String[] args) throws Exception{
		
		
		BufferedReader in = new BufferedReader(new FileReader(logfile));
		
		boolean has_ts = true;
		
		String line = in.readLine();
		ArrayList<String> allLines = new ArrayList<String>();
		while (line!=null) {
			allLines.add(line);
			line = in.readLine();
		}
		line = allLines.get(0).trim();
		String[] v = line.split("\\s");
		int M = allLines.size();
		int N = v.length-2;  // remove first one for timestamp, last one for id tag 
		Matrix data = new DenseMatrix(M,N);
		
//		if(M<30*N) {
//			System.err.println("Not enough data to train PCA model... ");
//			return;
//		}
		
		int i=0;
		for(String l: allLines) {
			if (l.startsWith("%")) {
				continue; // skip comments
			}
			v = l.split("\\s");
			int j;
			if (has_ts) {
				j=-1;
			} else {
				j =0;
			}
			for (String t: v) {
				if (j<0) {
					j+=1;
					continue;
				}
				if (t.startsWith("%") || t.trim().length()==0) {
					continue; // skip comments
				}
				double td = Double.parseDouble(t);
				data.set(i, j, td);
				j+=1;
			}
			i +=1;
		}
		
		//MatrixUtils.prettyPrint(data);
		
		PCA pca = new PCA(normalization);
		
		Train(data, pca);
		
		// write result to file..
		pca.writeFile( new File(logfile.getParentFile(), outFileName).getAbsolutePath() );
		
	}
	
	public static PCA Train(ArrayList<double[]> data, PCA pca) throws NotConvergedException{
		
		// use the size of last data -> make sure it is the longest
		double[][] tmpdata = new double[data.size()][data.get(data.size()-1).length];
		
		for(int i=0; i<data.size(); i++) {
			System.arraycopy(data.get(i), 0, tmpdata[i], 0, data.get(i).length);
		}
		Matrix mdata = new DenseMatrix(tmpdata);
		return Train(mdata, pca);		
	}
	
	public static PCA Train(Matrix data, PCA pca) throws NotConvergedException{
		
		if (pca.normalization_method!=0) {
			TFIDF tfidf = new TFIDF(data);
			pca.tfidf = tfidf;
			switch (pca.normalization_method) {
			case 1:
				tfidf.normalizeLength(data);
				break;
			case 2:
				tfidf.applyTFIDF(data);
				break;
			default:
			}
		}
		int M = data.numRows();
		int N = data.numColumns();
		double[] col_mean = new double[N];
		double[] sum = new double[N];
		
		for(int i=0; i<M; i++) {
			for(int j=0; j<N; j++) {
				sum[j] += data.get(i,j);
			}
		}
		
		for(int i=0; i<col_mean.length; i++) {
			col_mean[i] = sum[i]/M;
		}
		
		System.err.println("=== mean ===");
		MatrixUtils.prettyPrint(col_mean);
		
		for(int i=0;i<M;i++) {
			for (int j=0;j<N;j++) {
				data.set(i, j, data.get(i,j)-col_mean[j]);
			}
		}
		
		//System.err.println("=== data substract mean ===");
		//MatrixUtils.prettyPrint(data);
		
		//covariance matrix
		Matrix covmat = new DenseMatrix(N,N);
		Matrix data_trans = new DenseMatrix(N,M);
		data.transpose(data_trans);
		data_trans.mult(data, covmat);
		covmat.scale( ((double)1)/M);
		//System.err.println("=== Cov_Matrix ===");
		//MatrixUtils.prettyPrint(covmat);
		
		// eigen decomposition
		EVD evd = EVD.factorize(covmat);
		double[] ev = evd.getRealEigenvalues();
		//System.err.println("evd done..");
		//MatrixUtils.prettyPrint(ev);
		Matrix evs =evd.getLeftEigenvectors();
		
		// threshold and number of PC
		int numpc = selectNumPC(ev, dominate_space);
		System.err.println("Number of selected PC = " + numpc);
		for (int i = 0; i < numpc; i++) {
			System.err.println("evi[" +i +"] = " + ev[i]);
		}
		threshold(numpc, covmat, ev, pca); // result in pca argument
		System.err.println("Threshold = " + pca.threshold + " num_pc=" + pca.numpc);
		
		// compute the projection matrix
		////projection P
		Matrix P = MatrixUtils.subMatrix(evs, 0, evs.numRows(), 0, numpc);
		//MatrixUtils.prettyPrint(evs);
		//MatrixUtils.prettyPrint(P);
		////C=PP'
		Matrix Ptrans = new DenseMatrix(P.numColumns(), P.numRows());
		P.transpose(Ptrans);
		Matrix C = new DenseMatrix(N,N);
		P.mult(Ptrans, C);
		// C= eye - C
		for(int i=0; i<N; i++) {
			for (int j=0; j<N; j++) {
				if (i!=j) {
					C.set(i,j, -C.get(i,j));
				} else {
					C.set(i,j, 1-C.get(i,j));
				}
			}
		}
		
		System.err.println("=== C_Matrix ===");
		MatrixUtils.prettyPrint(C);
		pca.C = C;
		pca.col_mean = col_mean;
		return pca;
	}
	
	public static int selectNumPC(double [] eigenvalues, double fraction) {
		double threshold_energy = fraction*MatrixUtils.sum(eigenvalues);
		double selected_energy = eigenvalues[0];
		int num_pc = 1;
		while (selected_energy < threshold_energy) {
		    num_pc += 1;
		    selected_energy = selected_energy + eigenvalues[num_pc-1];
		}
		return num_pc;
	}
	
	public static void threshold(int init_num_pc, Matrix cov_mat, double[] eigenvalues, PCA pca) {
		
//		x = norminv([0.5*alpha 1-0.5*alpha],0,1);
//		c_alpha = x(2);
		
//		double c_alpha = 1.7507; // alpha = 0.08
//		double c_alpha = 1.9600; // alpha = 0.05
//		double c_alpha = 2.5758; // alpha = 0.01
//		double c_alpha = 2.807; // alpha = 0.005
//		double c_alpha = 2.9677;  // alpha = 0.003
//		double c_alpha = 3.2905;  // alpha = 0.001
//		double c_alpha = 3.4808;  // alpha = 0.0005
//		double c_alpha = 3.8906;  // alpha = 0.0001
//		double c_alpha = 4.4172;  // alpha = 0.00001
		
		double c_alpha;
		
		if(alpha==0.08)
			c_alpha = 1.7507;
		else if(alpha==0.05)
			c_alpha = 1.9600;
		else if(alpha==0.01)
			c_alpha = 2.5758;
		else if(alpha==0.005)
			c_alpha = 2.807; 
		else if(alpha==0.003)
			c_alpha = 2.9677;
		else if(alpha==0.001)
			c_alpha = 3.2905; 
		else if(alpha==0.0005)
			c_alpha = 3.4808; 
		else if(alpha==0.0001)
			c_alpha = 3.8906; 
		else if(alpha==0.00001)
			c_alpha = 4.4172; 
		else if(alpha==0.00001)
			c_alpha = 4.8916; 
		else if(alpha==0.000001)
			c_alpha = 5.3267; 
		else
			throw new RuntimeException("set alpha correctly!!");
		
		System.err.println("alpha=" + alpha);
		
		int max_num = cov_mat.numColumns();
//		% Get the largest eigenvlaues, which at the end of the array
//		int d = eigenvalues.length;
//
//		% The init_num_pc might make h0 < 0, which is appropriate for computing the
//		% threshold. It this is the case, we do a loop to find the minimum number
//		% of PCs to make h0 >=0.
		int num_pc = init_num_pc-1;
		double h0 = -1.0;
		double[] phi = new double[3];
		while (h0 < 0 && num_pc < max_num-1) {
		    num_pc += 1;
		    double[] lambda = MatrixUtils.subArray(eigenvalues, 0, num_pc);
		    
		    Matrix covpower = cov_mat.copy();
		    
		    double[] lambdapower = new double[lambda.length];
		    System.arraycopy(lambda, 0, lambdapower, 0, lambda.length);
		    
		    for (int i=0; i<3; i++) {
		    	double trace = MatrixUtils.trace(covpower);
		    	double sum=0;
		    	for (int j=0; j<lambdapower.length; j++) {
		    		sum += lambdapower[j];
		    		lambdapower[j] *= lambda[j];
		    	}
		    	phi[i] = trace - sum;
		    	//System.err.println("phi[" +i +"] = " + phi[i]);
		    	//MatrixUtils.prettyPrint(cov_mat);
		    	Matrix t = new DenseMatrix(covpower.numRows(), covpower.numColumns());		    	
		    	covpower.mult(cov_mat, t);  // covpower = covpower*cov_mat
		    	covpower = t;
		    }
		    h0 = 1 - 2*phi[0]*phi[2]/(3.0*phi[1]*phi[1]);
		    //System.err.println("h0=" + h0);
		}

		
		double threshold = phi[0]* Math.pow( (c_alpha*Math.sqrt(2*phi[1]*h0*h0)/phi[0] + 1.0 + phi[1]*h0*(h0-1)/phi[0]/phi[0]), (1.0/h0) );
		
		pca.numpc=num_pc;
		pca.threshold =threshold;
		
		//return new PCA(num_pc, threshold, null);
	}
	
	
	
}
