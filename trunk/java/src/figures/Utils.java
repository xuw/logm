package figures;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.Hits;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class Utils {
//	private static void createJFreeCharts(Hits hits, String[] projectedfields) throws IOException {
//
//		LinkedHashMap<String, XYSeries> datasets = new LinkedHashMap<String, XYSeries>(projectedfields.length);
//
//		File imgfile = new File("/tmp/ttt.png");
//		int cnt =0;
//		for (String pf: projectedfields) {
//			if (pf.length()==0)
//				continue;
//			cnt +=1;
//			try {
//				datasets.put(pf, new XYSeries(pf));
//			} catch (RuntimeException e) {
//				continue;
//			}
//		}
//
//		if (cnt==0) {
//			// try to delete file
//			imgfile.delete();
//			return;
//		}
//
//
//		cnt =0;
//		for (int i = 0; i < hits.length(); i++) {
//
//			Document doc = hits.doc(i);
//			long time = getTimestampSec(doc);
//			long lasttimestamp =0;
//			// timestamp tie breaker
//			double salt = 0.0;
//			double timestamp = time;
//			if (time==lasttimestamp) {
//				timestamp += salt;
//				salt += 0.01;
//			} else {
//				salt =0;
//			}
//
//
//
//			for (String pf : projectedfields) {
//				if (pf == null || pf.length() == 0)
//					continue;
//				try {
//					XYSeries series = datasets.get(pf);
//					String valuestr = Utils.dePadInteger(doc.get(pf));
//					if (valuestr==null || valuestr.length()==0) {
//						continue;
//					}
//					//System.err.println("adding " + valuestr);
//					Long value;
//					try {
//						value = Long.parseLong(valuestr);
//					} catch (NumberFormatException e) {
//						value = 1000L;
//					}
//					cnt +=1;
//					series.add(timestamp, value);
//					//System.err.println( rrdDb.dump() );
//					//System.err.println("added " + timestamp + "  "+ value +" " +pf);
//				} catch (Exception e) {
//					System.err.println(e);
//					continue; // not numbers
//				}
//			}
//		}
//
//
//		CombinedDomainXYPlot combined = new CombinedDomainXYPlot(new DateAxis("time"));
//		combined.setGap(10D);
//
//		ArrayList<XYPlot> subplots = new ArrayList<XYPlot>(datasets.size());
//		for (XYSeries series: datasets.values()) {
//			XYSeriesCollection xyDataset = new XYSeriesCollection();
//			xyDataset.addSeries(series);
//			XYBarRenderer standardxyitemrenderer = new XYBarRenderer();
//	        NumberAxis numberaxis = new NumberAxis(series.getKey().toString());
//	        XYPlot xyplot = new XYPlot(xyDataset, null, numberaxis, standardxyitemrenderer);
//	        xyplot.setRangeAxisLocation(AxisLocation.BOTTOM_OR_LEFT);
//	        //subplots.add(xyplot);
//	        combined.add(xyplot);
//		}
//
////
////		for (XYPlot subplot: subplots) {
////			combined.add(subplot,1);
////		}
//		combined.setOrientation(PlotOrientation.VERTICAL);
//
//		//JFreeChart chart = ChartFactory.createXYBarChart("", "time",true ,"value", xyDataset, PlotOrientation.VERTICAL, true, true, false);
//		//combined.add(chart);
//		JFreeChart chart = new JFreeChart("search result", JFreeChart.DEFAULT_TITLE_FONT, combined, true);
//		ChartUtilities.saveChartAsPNG(imgfile, chart, 640, 480);
//	}
}
