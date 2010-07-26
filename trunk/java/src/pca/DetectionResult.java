package pca;

import java.text.DecimalFormat;

public class DetectionResult {
	
	private double spe;
	private boolean abnormal;
	private long ts;
	private double threshold;
	static DecimalFormat formatter = new DecimalFormat("#.##");
	static boolean use_color = false;
	
	private String identifier;
	
	public double getSpe() {
		return spe;
	}
	public void setSpe(double spe) {
		this.spe = spe;
	}
	public boolean isAbnormal() {
		return abnormal;
	}
	public void setAbnormal(boolean abnormal) {
		this.abnormal = abnormal;
	}
	public long getTs() {
		return ts;
	}
	public void setTs(long ts) {
		this.ts = ts;
	}
	
	public double getThreshold() {
		return threshold;
	}
	public void setThreshold(double threshold) {
		this.threshold = threshold;
	}
	
	
	
	public String getIdentifier() {
		return identifier;
	}
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}
	public String toString() {
		//return "ts=" + this.ts +" spe="+this.spe + " " + (this.abnormal?"abnormal":"normal");
		if (!use_color) {
			return this.ts +","+ formatter.format(this.spe) + "," + formatter.format(this.threshold )+"," + (this.abnormal?"abnormal":"normal");
		} else {
			String abnormal = ((char)0x1B) +"[1;37;41mABNORMAL"+((char)0x1B)+"[0;39;49m";
			return this.ts +","+ formatter.format(this.spe) + "," + formatter.format(this.threshold )+"," + (this.abnormal?abnormal:"normal");
		}
	}
	
}
