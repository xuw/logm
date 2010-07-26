package online;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.text.DecimalFormat;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;


public class DemoGUIStatFrame extends JFrame {

	public static void main(String[] args) throws Exception{
		DemoGUIStatFrame f = new DemoGUIStatFrame();
	}
	
	JTextArea[] patterns;
	JTextArea[] counts;
	
	int[] pcnt;
	
	DemoGUIStatFrame(){
		this.setTitle("Log Mining Parameters");
		this.setSize(600,1000);
		this.setVisible(true);
		this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
	}
	
	
	void displayPatterns(SeqPattern[] pat) {
		pcnt = new int[pat.length];
		this.setLayout(new GridLayout(pat.length-1,1));
		patterns = new JTextArea[pat.length];
		counts = new JTextArea[pat.length];
		pcnt = new int[pat.length];
		
		for(int i=0; i<pat.length-1; i++) {
			pcnt[i] = 0;
			if(pat[i]==null) 
				continue;
			
			JPanel p = new JPanel();
			p.setLayout(new BorderLayout());
			p.setBorder(BorderFactory.createTitledBorder("Pattern " + i));
			
			patterns[i] = new JTextArea();
			counts[i] = new JTextArea();
			String text = toDemoString( pat[i] );
			patterns[i].setText(text);
			counts[i].setText("0");
			
			//patterns[i].setBorder(BorderFactory.createEtchedBorder());
			//counts[i].setBorder(BorderFactory.createEtchedBorder());
			
			patterns[i].setFont(new Font("Arial", Font.BOLD, 24));
			counts[i].setFont(new Font("Arial", Font.BOLD, 48));
			
			p.add(patterns[i], BorderLayout.CENTER);
			p.add(counts[i], BorderLayout.EAST);
			this.add(p);
		}
	}
	
	void addPatternCount(int pid) {
		pcnt[pid] +=1;
		counts[pid].setText(pcnt[pid]+"");
	}
	
	
	public String toDemoString(SeqPattern p) {
		StringBuffer sb = new StringBuffer();
		DecimalFormat df = new DecimalFormat("#.##");
		//sb.append("Pattern ").append(this.pattern_id).append(":" );
		sb.append("").append(SeqPattern.cntVecToSeq(p.countvec)).append("\n");

		sb.append("Duration: ").append( df.format(p.duration_mean) +"/"+ df.format(p.duration_stddev) ).append(" / ").append(df.format(p.duration_median)+"/"+ df.format(p.duration_mad)).append("\n");
		//sb.append("\tGap: ").append(df.format(gap_mean)+"/"+ gap_stddev).append(" / ").append(gap_median+"/"+gap_mad).append("\n");
		sb.append("Feqency: ").append(p.frequency_first_pass+"/"+ p.frequency_second_pass).append("\n");
		
		return sb.toString();
	}
	
}
