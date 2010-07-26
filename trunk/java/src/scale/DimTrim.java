package scale;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.SequenceFileInputFormat;
import org.apache.hadoop.mapred.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import utils.LogFormatter;

public class DimTrim extends Configured implements Tool {

	private static Logger LOG = LogFormatter.getLogger(DimTrim.class);

	public static class DimTrimMapClass extends MapReduceBase implements
			Mapper<Text, CountVectorWritable, Text, Text> {
		
		int[] index;
		
		CountVectorWritable dfcnt=null;
		CountVectorWritable outbuf;
		Text outtext = new Text();
		Text DF_KEY=new Text();
		Text tmpkey = new Text();
		Text empty = new Text();

		public void map(Text key, CountVectorWritable value,
				OutputCollector<Text, Text> output,
				Reporter reporter) throws IOException {
			
			outbuf.clear();
			
			int cnt=0;
			for (int i: index) {
				outbuf.inc(cnt++, value.data[i]);
				outbuf.tsmax = value.tsmax;
				outbuf.tsmin = value.tsmin;
			}
			if (key.equals(DF_KEY)) {
				outtext.set(dfcnt.toString()+ " %" +key);
				output.collect(empty, outtext);
				outtext.set(new CountVectorWritable(index).toString() +" %==index==");
				output.collect(empty, outtext );
			} else {
				outtext.set(outbuf.toString() +" %" + key);
				output.collect(empty, outtext);
			}
		}

		@Override
		public void configure(JobConf job) {
			DF_KEY.set("df");
			try {
				Path p = FileInputFormat.getInputPaths(job)[0];
				FileStatus[] files = FileSystem.get(job).listStatus(p);
				CountVectorWritable tmpv = new CountVectorWritable(0);
				Text tmpk = new Text();
				for (FileStatus f : files) {
					String fn = f.getPath().getName();
					//System.err.println(fn);
					if (fn.startsWith("dfcnt-")) {
						
						SequenceFile.Reader r = 
							new SequenceFile.Reader(FileSystem.get(job), f.getPath(), job);
						r.next(tmpk, tmpv);
						if (dfcnt == null) {
							dfcnt = new CountVectorWritable(tmpv.data.length);
						}
						dfcnt.add(tmpv);
						r.close();
					}
				}

				//System.err.println(dfcnt);
				int nzcnt =0;
				for(int i=0; i<dfcnt.data.length; i++) {
					if (dfcnt.data[i]!=0) {
						nzcnt +=1;
					}
				}
				this.index = new int[nzcnt];
				this.outbuf = new CountVectorWritable(nzcnt);
				
				nzcnt=0;
				for(int i=0; i<dfcnt.data.length; i++) {
					if (dfcnt.data[i] !=0) {
						this.index[nzcnt++] = i;
					}
				}
				
				//System.err.println( "NO-ZERO=" +nzcnt );
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	static int printUsage() {
		System.out
				.println("DimTrim [-m <maps>] [-r <reduces>] <input> <output>");
		ToolRunner.printGenericCommandUsage(System.out);
		return -1;
	}

	/**
	 * The main driver for word count map/reduce program. Invoke this method to
	 * submit the map/reduce job.
	 * 
	 * @throws IOException
	 *             When there is communication problems with the job tracker.
	 */
	public int run(String[] args) throws Exception {
		JobConf conf = new JobConf(getConf(), MsgCount.class);
		conf.setJobName("dimtrim");

		conf.setMapOutputValueClass(IntWritable.class);
		conf.setMapOutputKeyClass(Text.class);
		
		conf.setInputFormat(SequenceFileInputFormat.class);
		
		// the keys are words (strings)
		conf.setOutputKeyClass(Text.class);
		// the values are counts (ints)
		conf.setOutputValueClass(Text.class);

		conf.setOutputFormat(TextOutputFormat.class);

		conf.setMapperClass(DimTrimMapClass.class);
		//conf.setCombinerClass(null);
		//conf.setReducerClass(null);
		conf.setNumReduceTasks(0);

		List<String> other_args = new ArrayList<String>();
		for (int i = 0; i < args.length; ++i) {
			try {
				if ("-m".equals(args[i])) {
					conf.setNumMapTasks(Integer.parseInt(args[++i]));
				} else {
					other_args.add(args[i]);
				}
			} catch (NumberFormatException except) {
				System.out.println("ERROR: Integer expected instead of "
						+ args[i]);
				return printUsage();
			} catch (ArrayIndexOutOfBoundsException except) {
				System.out.println("ERROR: Required parameter missing from "
						+ args[i - 1]);
				return printUsage();
			}
		}

		// Make sure there are exactly 2 parameters left.
		if (other_args.size() != 2) {
			System.out.println("ERROR: Wrong number of parameters: "
					+ other_args.size() + " instead of 2.");
			return printUsage();
		}

		FileInputFormat.setInputPaths(conf, new Path(other_args.get(0)));
		FileOutputFormat.setOutputPath(conf, new Path(other_args.get(1)));

		JobClient.runJob(conf);
		return 0;
	}

	public static void main(String[] args) throws Exception {
		int res = ToolRunner.run(new Configuration(), new DimTrim(), args);
		System.exit(res);
	}
}
