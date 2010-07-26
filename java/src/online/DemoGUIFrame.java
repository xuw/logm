package online;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.OceanTheme;

public class DemoGUIFrame extends JFrame {
	
	JTextArea[] areas;
	JTextArea rawlog;
	JTextArea parsedlog;
	JTextArea bufSize;
	
	JTextArea anomalylist;
	
	JTextArea pcaVector;
	JTextArea pcaResult;
	JTextArea anomalycnt;
	JTextArea totalcnt;
	JCheckBox slowcheck;
	
	Color sectionTitleColor = Color.BLUE;
	Font sectionTitleFont = new Font("Arial", Font.BOLD, 32);
	
	public static void main(String[] args) throws Exception{
		DemoGUIFrame f = new DemoGUIFrame();
	}
	
	
	DemoGUIFrame() {
		
		this.setTitle("Online Log Mining Demo");
		
		Border bb = BorderFactory.createEtchedBorder();
		
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JPanel mainpanel = new JPanel();
		this.add(mainpanel);
		
		mainpanel.setLayout(new BorderLayout());
		
		
		JPanel inputPanel = new JPanel();
		inputPanel.setLayout(new GridLayout(2,1));
		//inputPanel.setBorder(BorderFactory.createTitledBorder("Console Log Parsing...."));
		
		inputPanel.setBorder(BorderFactory.createTitledBorder(bb, "Console Log Parsing", 
				TitledBorder.CENTER, TitledBorder.ABOVE_TOP, 
				this.sectionTitleFont, this.sectionTitleColor));
		
		mainpanel.add(inputPanel,BorderLayout.NORTH);
		
		JPanel clickPanel = new JPanel();
		clickPanel.setLayout(new BorderLayout());
		rawlog = new JTextArea();
		rawlog.setText("raw log line appears here");
		rawlog.setBorder(BorderFactory.createTitledBorder("Raw Log"));
		rawlog.setFont(new Font("Arial", Font.BOLD, 20));
		//rawlog.setSize(100,100);
		clickPanel.add(rawlog,BorderLayout.CENTER);
		slowcheck = new JCheckBox();
		slowcheck.setSelected(false);
		slowcheck.setBorder( BorderFactory.createTitledBorder("Slow") );
		clickPanel.add(slowcheck, BorderLayout.EAST);
		
		parsedlog = new JTextArea();
		parsedlog.setText("parsed log appears here");
		parsedlog.setBorder(BorderFactory.createTitledBorder("Parsed Log"));
		parsedlog.setFont(new Font("Arial", Font.BOLD, 20));
		//parsedlog.setSize(100,100);
		inputPanel.add(clickPanel);
		inputPanel.add(parsedlog);
		
		
		JPanel bufferPanel = new JPanel();
		//bufferPanel.setBorder(BorderFactory.createTitledBorder(""));
		
		bufferPanel.setBorder(BorderFactory.createTitledBorder(bb, "STAGE 1: Pattern-based Filtering", 
				TitledBorder.CENTER, TitledBorder.ABOVE_TOP, 
				this.sectionTitleFont, this.sectionTitleColor));
		
		bufferPanel.setLayout(new GridLayout(2,12));
		//JLabel jlbHelloWorld = new JLabel("Hello World");
		//add(jlbHelloWorld);
		mainpanel.add(bufferPanel,BorderLayout.CENTER);
		areas = new JTextArea[23];
		for(int i=0; i<areas.length; i++) {
			areas[i] = new JTextArea();
			areas[i].setSize(100, 100);
			areas[i].setBorder(BorderFactory.createTitledBorder("Empty"));
			areas[i].setText("" + i);
			areas[i].setFont(new Font("Arial", Font.PLAIN, 14));
			bufferPanel.add(areas[i]);
		}
		
		bufSize = new JTextArea();
		bufSize.setBorder(BorderFactory.createTitledBorder("  "));
		bufSize.setText("... ...\nand\n" + 0 +"\nmore\nbuffers...");
		bufSize.setFont(new Font("Arial", Font.PLAIN, 32));
		bufferPanel.add(bufSize);
		
		JPanel pcaPanel = new JPanel();
		pcaPanel.setLayout(new BorderLayout());
		
		pcaPanel.setBorder(BorderFactory.createTitledBorder(bb, "STAGE 2:  PCA Anomaly Detection", 
				TitledBorder.CENTER, TitledBorder.ABOVE_TOP, 
				this.sectionTitleFont, this.sectionTitleColor));
		mainpanel.add(pcaPanel, BorderLayout.SOUTH);
		
		
		
		JPanel pcaPanel1 = new JPanel();
		pcaPanel1.setLayout(new BorderLayout());
		pcaPanel.add(pcaPanel1,BorderLayout.NORTH);
		
		pcaVector = new JTextArea();
		pcaVector.setText("pca data");
		pcaVector.setBorder(BorderFactory.createTitledBorder("Message Count Vector"));
		pcaVector.setFont(new Font("Arial", Font.PLAIN, 18));
		pcaPanel1.add(pcaVector,BorderLayout.CENTER);
		
		pcaResult = new JTextArea();
		pcaResult.setText("result");
		pcaResult.setSize(100, 20);
		pcaResult.setBorder(BorderFactory.createTitledBorder("          "));
		pcaResult.setFont(new Font("Arial", Font.BOLD, 24));
		pcaPanel1.add(pcaResult,BorderLayout.EAST);
		
		
		JPanel statpanel =new JPanel();
		statpanel.setLayout(new GridLayout(1,2));
		
		JPanel statPanelL = new JPanel();
		statPanelL.setLayout(new GridLayout(2,1));
		
		anomalycnt = new JTextArea();
		anomalycnt.setText("0");
		anomalycnt.setBorder(BorderFactory.createTitledBorder("Total Anomalies"));
		anomalycnt.setFont(new Font("Arial", Font.BOLD, 48));
		statPanelL.add(anomalycnt);
		
		totalcnt = new JTextArea();
		totalcnt.setText("0");
		totalcnt.setBorder(BorderFactory.createTitledBorder("Total Lines"));
		totalcnt.setFont(new Font("Arial", Font.BOLD, 48));
		statPanelL.add(totalcnt);
		
		anomalylist = new JTextArea();
		anomalylist.setFont(new Font("Arial", Font.BOLD, 14));
		anomalylist.setText("");
		anomalylist.setBorder(BorderFactory.createTitledBorder("Recent Anomalies"));
		
		statpanel.add(statPanelL);
		statpanel.add(anomalylist);
		
		pcaPanel.add(statpanel, BorderLayout.SOUTH);
		
		this.setSize(1024, 768);
		// pack();
		setVisible(true);
		
	}
	
}
