package stream;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;

import pca.DetectionResult;
import pca.MatrixUtils;
import pca.PCA;
import pca.PCADetection;
import pca.PCATrain;

import scale.LogParser;
import scale.LogParserImpl;
import scale.ParsedMessageWritable;

public class StateRatioStream {

	//private static final String VALUEINDEX_FILENAME="C:\\Users\\xuw\\logExpData\\dk_value_index";
	//private static final String PCA_FILENAME = "C:\\Users\\xuw\\logExpData\\dk_pca";
	
	private static final String VALUEINDEX_FILENAME="./dk_value_index";
	private static final String PCA_FILENAME = "./dk_pca";
	
//	private static final String RESULT_DIR = "/mnt";
	private static final String RESULT_DIR = ".";
	
	private static final String DETECT_RESULT_FILE = RESULT_DIR +"/detect_result";
	private static final String STATE_RATIO_FILE = RESULT_DIR +"/state_ratio_v";
	private static final String PARSE_FILE = RESULT_DIR +"/parsed_log";
	
	private static final String TRAINING_LOG = "C:\\Users\\xuw\\logExpData\\darkstar\\dk_normal_ec2.log";
	//private static final String DETECTION_LOG = "C:\\Users\\xuw\\logExpData\\darkstar\\slow_10clients.txt";
	private static final String DETECTION_LOG=null;
	
	private static final long WINDOW_SIZE = 3000;
	
	static HashMap<String, Integer> value_index = new HashMap<String, Integer>();
	
	private static ArrayList<double[]> data;
	
	private static PCADetection detect;
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		
		LogParser parser = new LogParserImpl();
		ParsedMessageWritable buf = new ParsedMessageWritable();
		data = new ArrayList<double[]>();
		BufferedReader in;
		
		PrintStream detectresultwriter = new PrintStream(new File(DETECT_RESULT_FILE));
		PrintStream srvwriter = new PrintStream(new File(STATE_RATIO_FILE));
		PrintStream parsedlogwriter = new PrintStream(new File(PARSE_FILE));
		
		boolean training = false;
		// try to read value_index file
		File valueindexfile = new File(VALUEINDEX_FILENAME);
		if (!valueindexfile.exists()) {
			training = true;
			data.add(new double[0]);
			System.err.println("training from log file ... ");
			in = new BufferedReader(
					new FileReader(TRAINING_LOG));
		} else {
			System.err.println("reading index from " + VALUEINDEX_FILENAME);
			ObjectInputStream objin = new ObjectInputStream(new FileInputStream(VALUEINDEX_FILENAME));
			value_index = (HashMap<String, Integer>) objin.readObject();
			objin.close();
			data.add( new double[value_index.size()] );
			detect = new PCADetection(PCA_FILENAME);
			if (DETECTION_LOG!=null) {
				in = new BufferedReader(
					new FileReader(DETECTION_LOG));
			} else {
				int port=-1;
				try{
					port = Integer.parseInt( System.getProperty("port") );
				} catch (NumberFormatException e){
					System.err.println("are you setting -Dport parameter correctly?");
					System.exit(-1);
				}
				ServerSocket serversocket = new ServerSocket(port);
				Socket server = serversocket.accept();
				in = new BufferedReader(new InputStreamReader(server.getInputStream()));
				System.err.println("server is listening on port " + port);
			}
		}
		
		
		int seqcnt = 0;
		
		String line = in.readLine();
		parser.parseOneLine(line, buf, null);
		long lastwindowts = buf.getTs();
		
		while (line!=null) {
			long ts = buf.getTs();
			// look at the buf
			String[] lbns = buf.getLabelNames();
			String[] lbs = buf.getLabels();
//			if(lbns.length!=lbs.length) {
//				System.err.println(line);
//				System.err.println(buf);
//			}
			
			// first determine time window change
			if(ts-lastwindowts>=WINDOW_SIZE) {
				if(training) {
					// add a new vector at the end
					data.add( new double[value_index.size()] );
				} else {
					// output the vector and reset vector
					MatrixUtils.prettyPrint(data.get(0), srvwriter);
					DetectionResult dr= detect.isAbnormal(lastwindowts, data.get(0), "");
					detectresultwriter.println(dr);
					data.set(0, new double[value_index.size()]);
				}
				lastwindowts = ts;
			}
			
			for(int i=0; i<lbns.length; i++) {
				if (lbns[i].equals("state")) {
					parsedlogwriter.println("ts="+buf.getTs() +" "+ lbns[i]+"="+lbs[i]);
					Integer seqi = value_index.get(lbs[i]);
					if(seqi==null) {
						if(training) {
							seqi = seqcnt;
							value_index.put(lbs[i], seqcnt++);
							// increase current training data size
							double[] old = data.get(data.size()-1);
							double[] newarr = new double[old.length+1];
							System.arraycopy(old, 0, newarr, 0, old.length);
							data.set(data.size()-1, newarr);
							System.err.println("new value:" +lbs[i] +" increasng vector size to " +newarr.length);
						} else {
							// print warning and ignore
							System.err.println("never seen this value in training: " + lbs[i]);
							break;
						}
					}
					// increase count
					double[] countarr = data.get(data.size()-1);
					countarr[seqi] +=1;
					break;
				}
			}
			//System.err.println();
			
			line = in.readLine();
			if (line==null) 
				break;
			parser.parseOneLine(line, buf, null);
		}
		
		if(training) {
			for(Entry<String, Integer> e: value_index.entrySet()) {
				System.err.println(e.getKey()+" " + e.getValue());
			}
			for(double[] t : data) {
				MatrixUtils.prettyPrint(t);
			}
			
			System.err.println("starting PCA training ..");
			PCA pca = new PCA(PCA.NO_NORMALIZATION);
			PCATrain.alpha = 0.001;
			PCATrain.dominate_space = 0.95;
			PCATrain.Train(data, pca);
			pca.writeFile(PCA_FILENAME);
			System.err.println("PCA training done, data written to " + PCA_FILENAME);
			
			System.err.println(pca);
			
			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(VALUEINDEX_FILENAME));
			out.writeObject(value_index);
			out.close();
		} else {
			// do nothing
		}
		
	}

}
