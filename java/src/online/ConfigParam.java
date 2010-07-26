package online;

import java.io.File;
import java.io.PrintStream;

import pca.PCA;
import pca.PCATrain;
import pca.TFIDF;

public class ConfigParam {
	
	public static File DATASET_DIR = new File("data/online1/lg");
	
	public static int target_detection_time_in_sec = 60;
	
	// for PCA
	public static double pca_alpha = 0.001;
	public static int normalization = PCA.NORMALIZE_TFIDF;
	public static double pca_dominant_space = 0.95;
	
	// for pattern training
	public static double ratio_rare_events = pca_alpha;
	public static double min_support_for_pattern =0.2; // percent of total blocks
	public static boolean doSample = true;
	public static double sampleRatio = 0.1;
	//public static long sampleSeed = 12134;
	public static long sampleSeed = 1213412;
	
	// for pattern matching
	public static int non_pattern_max_wait = target_detection_time_in_sec;
	// controls how the timeout is calculated
//	public static double getMaxDuration(SeqPattern p) {
//		double pat_dur = p.duration_mean + 10* p.duration_stddev;
//		return pat_dur;
//	}
	
	static double[] percentile999 = {11,7,1,0,0,0};
	static double[] percentile9999 = {20,14,1,0,0,0};
	static double[] percentile9995 = {13,8,1,0,0,0};
	static double[] percentilettt = {600,600,600,0,0,0};
	
	static double[] percentilegauss = {5.3,4.0,1,0,0,0};
	
	static double[] percentile = percentilettt;
	
	public static double getMaxDuration(SeqPattern p) {
		return percentile[p.pattern_id];
	}
	
	public static void main(String[] args) throws Exception{
		
		System.setErr(new PrintStream(new File("data/params_long_err.txt") ));
		System.setOut(new PrintStream(new File("data/params_long_out.txt") ));
		
//		setParamAndRun(0.0001, 60, true, args); // need to train for the first time
//		setParamAndRun(0.001, 60, true, args);
//		setParamAndRun(0.005, 60, true, args); 
//		setParamAndRun(0.01, 60, true, args); 
////		setParamAndRun(0.05, 60, false, args); 
//		
////		setParamAndRun(0.001, 60, true, args); 
//		setParamAndRun(0.001, 15, true, args); 
//		setParamAndRun(0.001, 30, true, args); 
//		setParamAndRun(0.001, 120, true, args); 
//		setParamAndRun(0.001, 240, true, args); 
		
		setParamAndRun(0.001, 60, true, args); 
	}
	
	
	public static void setParamAndRun(double alpha, int maxtime, boolean all, String[] args) throws Exception {
		pca_alpha = alpha;
		target_detection_time_in_sec = maxtime;
		System.err.println("===========Begin==============");
		System.out.println("alpha=" + pca_alpha + " max_time=" + target_detection_time_in_sec);
		if (!all) {
			runPCAOnly(args);
		} else {
			runAll(args);
		}
		System.err.println("alpha=" + pca_alpha + " max_time=" + target_detection_time_in_sec);
		System.err.println("===========Done==============\n\n");
		
		new File(DATASET_DIR, "detect_time.txt").renameTo(new File(DATASET_DIR,"detect_time_"+maxtime+"_"+alpha+".long.txt"));
		new File(DATASET_DIR, "buf_size.txt").renameTo(new File(DATASET_DIR,"buf_size_"+maxtime+"_"+alpha+".long.txt"));
	}
	
	
	public static void runAll(String[] args) throws Exception{
		
		// discover patterns
		if(DATASET_DIR.getName().endsWith("sm")) {
			doSample = false; // do not use sampling for small datsset
		}
		TimedSubseqTrain.main(args);
		
		
		// use pattern to simulate detection
		SubPatternMatch.main(args);
		
		runPCAOnly(args);
		
	}
	
	public static void runPCAOnly(String[] args) throws Exception{
		// training on timeout sequences
		PCATrain.alpha = pca_alpha;
		PCATrain.dominate_space = pca_dominant_space;
		PCATrain.normalization = normalization;
		PCATrain.main(args);
		
		// do detection and compare results
		PCACountDetection.main(args);
	}
	
}
