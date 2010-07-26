package mine;

import index.IndexConstants;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.TreeSet;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.XMLEvent;

public class ClusterTextLabelReader {

	
	static class WeightPair implements Comparable<WeightPair>{
		String name;
		double weight;
		
		WeightPair(String name, double weight) {
			this.name = name;
			this.weight = weight;
		}
		
		public int compareTo(WeightPair o) {
			double weightdiff = -( this.weight-o.weight);
			if (weightdiff==0) 
				return this.name.compareTo(o.name);
			else if (weightdiff>0) {
				return 1;
			} else{
				return -1;
			}
				
		}
		
		public boolean equals(WeightPair o) {
			return name.equals(o.name) && weight==o.weight;
		}
		
	}
	
	
	
	
	public static void main(String[] args) throws Exception {
		
		for (int i=0; i<=15; i++) {
			ArrayList<WeightPair> list = new ArrayList<WeightPair>();
			//System.err.println("==============");
			File weightfile = new File(IndexConstants.VIS_OUTPUT_DIR,"weights"+i+".wgt");
			parse(weightfile,list);
			Collections.sort(list);
			int cnt =0;
			for (WeightPair p: list) {
				cnt +=1;
				if (cnt >5) break;
				if (p.weight>=0.3) {
					System.err.print( p.name +" ");
				}
			}
			System.err.println();
		}
	}
	
	
	public static void parse(File weightfile, ArrayList<WeightPair> list) throws Exception{
		
		XMLInputFactory xmlif = XMLInputFactory.newInstance();
	    XMLEventReader xmler = xmlif.createXMLEventReader(new FileReader(weightfile));
	    XMLEvent event;
	    while (xmler.hasNext()) {
	      event = xmler.nextEvent();
	      if (event.isStartElement()) {
	        //System.out.println(event.asStartElement().getName());
	        
	    	String eventname = event.asStartElement().getName().toString();
	    	if (!eventname.equals("weight"))
	    		continue;
	    	String attrname=null;
	    	String attrweight=null;
	    	
	    	Iterator<Attribute> i = event.asStartElement().getAttributes();
	        while(i.hasNext()) {
	        	Attribute attr = i.next();
	        	if (attr.getName().toString().equals("name")) {
	        		attrname =new String(attr.getValue());
	        	}else if (attr.getName().toString().equals("value")){
	        		attrweight = attr.getValue();
	        	}
	        	if (attrname!=null && attrweight!=null) {
	        		double weightdouble = Double.parseDouble(attrweight);
	        		if (weightdouble >=0) {
	        			list.add(new WeightPair(attrname, weightdouble));
	        			//System.err.println(weightdouble +" " +attrname);
	        		}
	        		
	        	}
	        }
	      } else if (event.isCharacters()) {
	    	  ;
	      } 
	    }
	}

}
