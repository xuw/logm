package mine;

import index.IndexConstants;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.TreeMap;

public class ThreadFeatureGen {

	public static int[] encodedLog;

	public static void main(String[] args) throws Exception {

		for (String logdbname : IndexConstants.LOG_DB_NAMES) {
			System.err.println("Processing DB " + logdbname);
			EncodedLogReader logreader = new EncodedLogReader(logdbname);

			HashMap<String, Integer> namemap = logreader.getNameMap();
			TreeMap<Integer, String> reversemap = logreader.getIdMap();
			encodedLog = logreader.getEncodedLog();

			int threadBeginMarkerLogId = namemap.get("THREAD_BEGIN_MARKER");
			int[] threadfeature = new int[namemap.size()];

			PrintStream outstream = new PrintStream(new FileOutputStream(
					new File(IndexConstants.TMP_DIR, logreader.getDbname()
							+ "_threadfeatures")));

			boolean newthread = true;
			int threadcnt = 0;
			for (int log : encodedLog) {
				if (log == threadBeginMarkerLogId) {
					if (!newthread) {
						threadcnt += 1;
						newthread = true;
						// System.err.print( (threadcnt-1) +":: " );
						for (int t : threadfeature) {
							// System.err.print(t +" ");
							outstream.print(t + " ");
						}
						outstream.println();
						threadfeature = new int[namemap.size()];
					}
				} else {
					newthread = false;
					threadfeature[log - 1] += 1;
				}
			}
		}
	}

}
