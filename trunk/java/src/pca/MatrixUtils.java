package pca;

import java.io.PrintStream;
import java.util.Formatter;

import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.Matrix;

public class MatrixUtils {
	
	static {
		Formatter formatter = new Formatter();
	}
	
	public static void prettyPrint(Matrix x) {
		for (int i=0; i< x.numRows(); i++) {
			for (int j=0; j<x.numColumns(); j++) {
				System.err.print( new Formatter().format("%1.2f\t", x.get(i, j)));
			}
			System.err.println();
		}
	}
	
	public static void prettyPrint(double[] x) {
		prettyPrint(x, System.err);
	}
	
	public static void prettyPrint(double[] x, PrintStream out) {
		for (int i=0; i< x.length; i++) {
			out.print( new Formatter().format("%1.2f\t", x[i]));
		}
		out.println();
	}
	
	public static void prettyPrint(int[] x) {
		prettyPrint(x, System.err);
	}
	
	public static void prettyPrint(int[] x, PrintStream out) {
		for (int i=0; i< x.length; i++) {
			out.print( new Formatter().format("%4d\t", x[i]));
		}
		out.println();
	}
	
	
	public static void prettyPrint(int[][] x) {
		for (int j=-1; j<x[0].length; j++) {
			System.err.print( new Formatter().format("(%4d)", j));
		}
		System.err.println();
		for (int i=0; i< x.length; i++) {
			System.err.print( new Formatter().format("(%4d)",i) );
			for (int j=0; j<x[i].length; j++) {
				System.err.print( new Formatter().format("%6d", x[i][j]));
			}
			System.err.println();
		}
	}
	
	public static void prettyPrint(boolean[][] x) {
		for (int j=-1; j<x[0].length; j++) {
			System.err.print( new Formatter().format("(%4d)", j));
		}
		System.err.println();
		for (int i=0; i< x.length; i++) {
			System.err.print( new Formatter().format("(%4d)",i) );
			for (int j=0; j<x[i].length; j++) {
				System.err.print( new Formatter().format("%6b", x[i][j]));
			}
			System.err.println();
		}
	}
	
	public static void prettyPrint(double[][] x) {
		
		for (int j=-1; j<x[0].length; j++) {
			System.err.print( new Formatter().format("(%5d)", j));
		}
		System.err.println();
		for (int i=0; i< x.length; i++) {
			System.err.print( new Formatter().format("(%5d)",i) );
			for (int j=0; j<x[i].length; j++) {
				System.err.print( new Formatter().format("%8.1f", x[i][j]));
			}
			System.err.println();
		}
	}
	
	public static double sum(double[] x) {
		double sum=0d;
		for (int i=0; i<x.length; i++) {
			sum+=x[i];
		}
		return sum;
	}
	
	public static int sum(int[] x) {
		int sum=0;
		for (int i=0; i<x.length; i++) {
			sum+=x[i];
		}
		return sum;
	}
	
	public static double[] subArray(double[] x, int start, int len) {
		double[] ret = new double[len];
		for(int i=start; i<start+len; i++) {
			ret[i-start] = x[i];
		}
		return ret;
	}
	
	public static double trace(Matrix x) {
		double sum =0;
		int col = x.numColumns();
		int row = x.numRows();
		if (col!=row) {
			throw new RuntimeException("not square matrix!!");
		}
		for(int i=0; i< col; i++) {
			sum += x.get(i, i);
		}
		return sum;
	}
	
	public static Matrix subMatrix(Matrix x, int startrow, int endrow, int startcol, int endcol) {
		Matrix ret = new DenseMatrix(endrow-startrow, endcol-startcol);
		for(int i=startrow; i<endrow; i++) {
			for(int j=startcol; j<endcol; j++) {
				ret.set(i-startrow, j-startcol, x.get(i, j));
			}
		}
		return ret;
	}
	
	
	public static double[] normalizeByLenghth(double[] x) {
		double[] ret = new double[x.length];
		double sumx = sum(x);
		for(int i=0; i< x.length; i++) {
			ret[i] = x[i]/sumx;
		}
		return ret;
	}
	
	public static double chisqdistance(double[] x, double[] y) {
		
		double[] nx = normalizeByLenghth(x);
		double[] ny = normalizeByLenghth(y);
		
		//MatrixUtils.prettyPrint(nx);
		
		double sum=0;
		for(int i=0; i<nx.length; i++) {
			if (nx[i]!=0 || ny[i]!=0) {
				double t = nx[i]-ny[i];
				sum += t*t/ (nx[i]+ny[i]);
			}
		}
		return sum;
	}
	
}
