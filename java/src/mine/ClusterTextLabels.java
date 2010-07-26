package mine;

import index.IndexConstants;

import java.io.File;
import java.io.FileWriter;
import edu.udo.cs.wvtool.config.WVTConfiguration;
import edu.udo.cs.wvtool.config.WVTConfigurationFact;
import edu.udo.cs.wvtool.config.WVTConfigurationRule;
import edu.udo.cs.wvtool.generic.output.WordVectorWriter;
import edu.udo.cs.wvtool.generic.stemmer.DummyStemmer;
import edu.udo.cs.wvtool.generic.stemmer.PorterStemmerWrapper;
import edu.udo.cs.wvtool.generic.tokenizer.SimpleTokenizer;
import edu.udo.cs.wvtool.generic.vectorcreation.TFIDF;
import edu.udo.cs.wvtool.generic.vectorcreation.TermFrequency;
import edu.udo.cs.wvtool.generic.wordfilter.WVTWordFilter;
import edu.udo.cs.wvtool.main.WVTDocumentInfo;
import edu.udo.cs.wvtool.main.WVTFileInputList;
import edu.udo.cs.wvtool.main.WVTool;
import edu.udo.cs.wvtool.wordlist.WVTWordList;

public class ClusterTextLabels {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception{
		
		File inputdir = new File(IndexConstants.VIS_OUTPUT_DIR,"threadclusterout");
		File wordlistfile = new File(IndexConstants.VIS_OUTPUT_DIR,"wordlist.txt");
		File vectorfile = new File(IndexConstants.VIS_OUTPUT_DIR,"wordvector.txt");
		
		WVTool wvt = new WVTool(true);
		
		WVTConfiguration config = new WVTConfiguration();
		
		config.setConfigurationRule(WVTConfiguration.STEP_TOKENIZER, new WVTConfigurationFact(new SimpleTokenizer()));
		
		config.setConfigurationRule(WVTConfiguration.STEP_STEMMER, new WVTConfigurationFact(new PorterStemmerWrapper()));
		
		WVTFileInputList list = new WVTFileInputList(1);
		
		int clusterid=-1;
		for(File clusterdir: inputdir.listFiles()) {
			clusterid+=1;
			for (File threadfile: clusterdir.listFiles()) {
				list.addEntry(new WVTDocumentInfo(threadfile.getAbsolutePath(),"txt","","english",clusterid));
			}
		}
		
		
		WVTWordList wordList = wvt.createWordList(list, config);
		
		//wordList.pruneByFrequency(2, 5);
		
		wordList.storePlain(new FileWriter(wordlistfile.getAbsoluteFile()));
		
		FileWriter vectorout = new FileWriter(vectorfile);
		WordVectorWriter vectorwriter = new WordVectorWriter(vectorout, true);
		config.setConfigurationRule(WVTConfiguration.STEP_OUTPUT, new WVTConfigurationFact(vectorwriter));
		config.setConfigurationRule(WVTConfiguration.STEP_VECTOR_CREATION,  new WVTConfigurationFact(new TermFrequency()));
		
		wvt.createVectors(list, config, wordList);
		
		vectorwriter.close();
		vectorout.close();
		
	}

}
