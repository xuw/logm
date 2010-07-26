package labeling;

import index.IndexConstants;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TermQuery;

public class FieldNamesGen {

	private Searcher searcher;
	private File FILEROOT;
	public FieldNamesGen(File fileroot) {
		try {
			this.FILEROOT = fileroot;
			File indexdir = new File(IndexConstants.DATA_ROOT, "SrcIndex");
			IndexReader reader = IndexReader.open(indexdir.getAbsolutePath());
			this.searcher = new IndexSearcher(reader);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) throws Exception{
		
		FieldNamesGen fn = new FieldNamesGen(new File( "C:/Users/xuw/mscipts/pca_new/data/200nodes"));
		// open Index file
		String [] arr = fn.getTypeStrings();
		StringBuffer sb = new StringBuffer();
		
		for (int i=0; i<arr.length; i++) {
			String s = arr[i];
			sb.append(i+1).append(".").append(s).append("\t");
			
		}
		System.err.println(sb);
	}
	
	public String[] getTypeStrings() {
		
		ArrayList<String> tmp = new ArrayList<String>();
		
		try {
			BufferedReader r = new BufferedReader(new FileReader(new File(FILEROOT, "index")));
			String line =r.readLine();
			r.close(); // only need one line.
			
			//System.err.println(line);
			String[] indexes = line.split(" ");
			for(String i: indexes) {
				try {
					int ind = Integer.parseInt(i);
					String s = this.getTypeString(ind);
					tmp.add(s);
					//System.err.println(s);
				} catch (RuntimeException e) {
					//e.printStackTrace();
					break; // at the end
				}
			}
		} catch (FileNotFoundException e) {
			;
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return tmp.toArray(new String[tmp.size()]);
	}
	
	public String getTypeString(int key){
		try {
			Query q = new TermQuery(new Term("seq", key+""));
			//System.err.println("query::" +q);
			Hits hits = this.searcher.search(q, Sort.RELEVANCE);
			if (hits.length()!=1) {
				System.err.println("[WARNING] - non-unique log sequence " + hits.length());
			}
			Document hit = hits.doc(0);
			return hit.get("strindex");
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	
}
