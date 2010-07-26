package pca;

import java.io.Serializable;

import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.Vector;


//% [m,n] = size(data);
//% doc_len = sum(data, 2);
//% tf = data ./ repmat(doc_len, [1 n]);
//% %% DF
//% df = zeros(n,1);
//% for i=1:1:n
//%     df(i,:) = size( find( data(:,i)>0 ), 1 );
//% end
//% %% idf
//% idf = zeros(n,1);
//% for i=1:1:n
//%     if (df(i,1)==0)
//%         idf(i,:) =0;
//%     else
//%         idf(i,:) = log(m/df(i,1));
//%     end 
//% end
//% 
//% %% weight
//% weight = zeros(m,n);
//% for i=1:1:m
//%     for j=1:1:n
//%         weight(i,j) = data(i,j) * idf(j,1);
//%     end
//% end
//% 

public class TFIDF implements Serializable{
	private double[] idf=null;
	public TFIDF(Matrix data) {
		int M = data.numRows();
		this.idf = new double[data.numColumns()];
		for(int i=0; i<data.numRows(); i++) {
			for(int j=0; j<data.numColumns(); j++) {
				// calculate DF
				this.idf[j] += data.get(i, j);
			}
		}
		// from DF to IDF
		for(int i=0; i<this.idf.length; i++) {
			if (this.idf[i] !=0) {
				this.idf[i] = Math.log( M/this.idf[i] );
			} // else ignore
		}
		MatrixUtils.prettyPrint(this.idf);
	}
	
	public void applyIDF(Matrix data) {
		if (this.idf==null) {
			throw new RuntimeException("TFIDF not initialzed correctly!");
		}
		for(int i=0; i<data.numRows(); i++) {
			for(int j=0; j<data.numColumns(); j++) {
				double t = data.get(i,j) * this.idf[j];
				data.set(i,j,t);
			}
		}
		System.err.println("applied idf..");
	}
	
	public void applyIDF(Vector data) {
		if (this.idf == null) {
			throw new RuntimeException("TFIDF not initialzed correctly!");
		}
		for (int j = 0; j < data.size(); j++) {
			double t = data.get(j) * this.idf[j];
			data.set(j, t);
		}
	}
	
	
	public void normalizeLength(Matrix data) {
		if (this.idf==null) {
			throw new RuntimeException("TFIDF not initialzed correctly!");
		}
		for(int i=0; i<data.numRows(); i++) {
			int doc_len =0;
			for(int j=0; j<data.numColumns(); j++) {
				doc_len += data.get(i, j);
			}
			if(doc_len==0)
				continue;  // skip all zero rows
			for(int j=0; j<data.numColumns(); j++) {
				double t = data.get(i, j)/doc_len;
				data.set(i, j, t);
			}
		}
		System.err.println("normalized length..");
		//MatrixUtils.prettyPrint(data);
	}
	
	
	public void normalizeLength(Vector data) {
		if (this.idf == null) {
			throw new RuntimeException("TFIDF not initialzed correctly!");
		}
		int doc_len = 0;
		for (int j = 0; j < data.size(); j++) {
			doc_len += data.get(j);
		}
		if (doc_len == 0)
			return; // skip all zero rows
		for (int j = 0; j < data.size(); j++) {
			double t = data.get(j) / doc_len;
			data.set(j, t);
		}
	}
	
	
	public void applyTFIDF(Matrix data) {
		if (this.idf==null) {
			throw new RuntimeException("TFIDF not initialzed correctly!");
		}
		applyIDF(data);
		normalizeLength(data);
	}
	
	public void applyTFIDF(Vector data) {
		if (this.idf==null) {
			throw new RuntimeException("TFIDF not initialzed correctly!");
		}
		applyIDF(data);
		normalizeLength(data);
	}
	
}
