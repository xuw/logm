package scale;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.GzipCodec;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordWriter;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.TextOutputFormat;
import org.apache.hadoop.util.Progressable;
import org.apache.hadoop.util.ReflectionUtils;

public class CountVectorOutputFormat extends TextOutputFormat<Text, CountVectorWritable> {

	protected static class LineRecordWriter implements RecordWriter<Text, CountVectorWritable> {
		private static final String utf8 = "UTF-8";
		private static final byte[] tab;
		private static final byte[] newline;
		static {
			try {
				tab = "\t".getBytes(utf8);
				newline = "\n".getBytes(utf8);
			} catch (UnsupportedEncodingException uee) {
				throw new IllegalArgumentException("can't find " + utf8
						+ " encoding");
			}
		}

		private DataOutputStream out;

		public LineRecordWriter(DataOutputStream out) {
			this.out = out;
		}

		/**
		 * Write the object to the byte stream, handling Text as a special
		 * case.
		 * @param o the object to print
		 * @throws IOException if the write throws, we pass it on
		 */
		private void writeObject(Object o) throws IOException {
			if (o instanceof Text) {
				Text to = (Text) o;
				out.write(to.getBytes(), 0, to.getLength());
			} else {
				out.write(o.toString().getBytes(utf8));
			}
		}

		public synchronized void write(Text key, CountVectorWritable value) throws IOException {
			writeObject(value);
			writeObject(" %%");
			writeObject(key);
			out.write(newline);
		}

		public synchronized void close(Reporter reporter) throws IOException {
			out.close();
		}
	}

	public RecordWriter<Text, CountVectorWritable> getRecordWriter(FileSystem ignored, JobConf job,
			String name, Progressable progress) throws IOException {

		Path dir = getWorkOutputPath(job);
		FileSystem fs = dir.getFileSystem(job);
		if (!fs.exists(dir)) {
			throw new IOException("Output directory doesnt exist");
		}
		boolean isCompressed = getCompressOutput(job);
		if (!isCompressed) {
			FSDataOutputStream fileOut = fs.create(new Path(dir, name),
					progress);
			return new LineRecordWriter(fileOut);
		} else {
			Class<? extends CompressionCodec> codecClass = getOutputCompressorClass(
					job, GzipCodec.class);
			// create the named codec
			CompressionCodec codec = (CompressionCodec) ReflectionUtils
					.newInstance(codecClass, job);
			// build the filename including the extension
			Path filename = new Path(dir, name + codec.getDefaultExtension());
			FSDataOutputStream fileOut = fs.create(filename, progress);
			return new LineRecordWriter(new DataOutputStream(codec
					.createOutputStream(fileOut)));
		}
	}

}
