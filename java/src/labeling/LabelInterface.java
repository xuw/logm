package labeling;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;


class LabelInterface extends JFrame {
	
	//static final File DATA_DIR = new File("C:/Users/xuw/Desktop/pca/data");
	static final File DATA_DIR = new File("C:/Users/xuw/mscipts/pca_new/data");
	static File workingDir = null;
	
	static String[] dataSetList = DATA_DIR.list();;
	static String[] algoList = null;
	static String[] paramList = null;
	
//	static int dataSetSelect = -1;
//	static int algoSelect = -1;
//	static int paramSelect = -1;
	
	static JComboBox algoSelectBox;
	static JComboBox paramSelectBox;
	
	static String[] index = new String[0];
	static String[] data = new String[0];
	
	static int[] autolabels = null;
	
	static HashMap<Integer, Integer> manualLabels = new HashMap<Integer, Integer>();  // unique_group_id -> label (normal vs. not normal)
	
	static HashMap<Integer, Integer> manualComments = new HashMap<Integer, Integer>();
	
	static ArrayList<ManualComment> comments = new ArrayList<ManualComment>();
	
	static HashMap<String, Integer> groupmembercnt = new HashMap<String, Integer>();
	
	static LabelInterface frame;
	
	static int[] groupLabels=null;
	
	static JList blocklistBox;
	static JTextField idBox;
	static JComboBox commentsText;
	
	static int comments_cnt = 0;

	
	public static void saveLabels(){
		try {
			ObjectOutputStream out = new ObjectOutputStream(
					new FileOutputStream(new File(workingDir, "manualLabels") ));
			out.writeObject(comments);
			out.writeObject(manualComments);
			out.writeObject(manualLabels);
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void outputLabelData() {
		try {
			
			if (workingDir == null)
				return;
			if (algoSelectBox.getSelectedIndex() == -1)
				return;
			if (paramSelectBox.getSelectedIndex() == -1)
				return;
			
			File labelDir = new File( 
					new File(
							new File(workingDir,"results"), algoSelectBox.getSelectedItem().toString()), 
					paramSelectBox.getSelectedItem().toString()) ;
			
			
			PrintStream mlabelout = new PrintStream(new FileOutputStream(
					new File(labelDir, "mlabel.txt")));
			
			PrintStream mistakeout = new PrintStream(new FileOutputStream(
					new File(labelDir, "mistakes.txt")));
			
			PrintStream summaryout = new PrintStream(new FileOutputStream(
					new File(labelDir, "detection.txt")));
			
			PrintStream fpstatout = new PrintStream(new FileOutputStream(
					new File(labelDir, "falsePositives.txt")));
			
			int[] anomaly_type_cnt = new int[comments.size()+1]; // how many anomaly total,
																//0 is no comment case
			int[] detected_cnt = new int[comments.size()+1];
			int[] fp_cnt = new int[comments.size()+1];
			
			for(int i=0; i<comments.size()+1; i++) {
				anomaly_type_cnt[i] =0;
				detected_cnt[i] =0;
			}
			
			for(int i=0; i<data.length; i++){
				int albl = autolabels[i];
				int grp = groupLabels[i];
				int mlbl = manualLabels.get(grp);
				//mlabelout.println(albl +" " +mlbl);
				//mlabelout.println(mlbl);
				int commentid = -1;
				if (albl==1 || mlbl==1) {
					Integer cid = manualComments.get(grp);
					if (cid !=null)
						commentid = cid;
				}
				mlabelout.println(mlbl +" "+ commentid);
				
				if(mlbl==1) {
					anomaly_type_cnt[commentid+1] +=1;
					if (albl ==1) {
						detected_cnt[commentid+1] +=1;
					}
				}
				
				
				if( (mlbl==1 || albl==1)){
					
					if (commentid==-1) {
						System.err.println("missing comment: " + (i+1) );
					} else {
						ManualComment com = comments.get(commentid);
						if (com==null || com.comment==null || com.comment.trim().length()==0) {
							System.err.println("empty comment: " + mlbl );
						}
					}
				}
				
				if(albl==1 && mlbl==0) { // false positives
					fp_cnt[commentid+1] +=1;
				}
				
				if (albl != mlbl) {
					String comment = commentid==-1?"No comment":LabelInterface.comments.get(commentid).comment;
					String mistaketype = "FN";
					if(albl==1)
						mistaketype = "FP";
					mistakeout.println((i+1) +" " +commentid +" " + mistaketype +" " +comment);
				}
			}
			
			if(anomaly_type_cnt[0] !=0) {
				summaryout.println("\"No Comment\"" +"," +anomaly_type_cnt[0] +"," + detected_cnt[0]);
			}
			for (int i=1; i<comments.size()+1; i++) {
				if(comments.get(i-1).comment.trim().length()==0)
					continue;
				if (anomaly_type_cnt[i]==0)
					continue;
				summaryout.println("\""+comments.get(i-1).comment +"\"," +anomaly_type_cnt[i] +"," + detected_cnt[i]);
			}
			
			//false positive stats
			if(fp_cnt[0] !=0) {
				summaryout.println("\"No Comment\"" +"," +fp_cnt[0]);
			}
			for (int i=1; i<comments.size()+1; i++) {
				if(comments.get(i-1).comment.trim().length()==0)
					continue;
				if (fp_cnt[i]==0)
					continue;
				fpstatout.println("\""+comments.get(i-1).comment +"\"," +fp_cnt[i]);
			}
			
			mlabelout.close();
			mistakeout.close();
			summaryout.close();
			fpstatout.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	
	
	public static void main(String[] args) throws Exception{
		loadData();
		//System.err.println(index[100]);
		//System.err.println(data[100]);
		//System.err.println(getFile(index[30])); 
		frame = new LabelInterface();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
	}
	
	public static void genReports(){
		
	}
	
	
	public static void reLoadData() {
		try {
			loadData();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void loadParamList() {
		try {
			if(algoSelectBox.getSelectedIndex() <0)
				return;
			String[] params = new File(new File(workingDir,"results"),
					algoSelectBox.getSelectedItem().toString()).list();
			paramSelectBox.removeAllItems();
			for (int i=0; i<params.length; i++) {
				paramSelectBox.insertItemAt(params[i], i);
			}
			autolabels = null;
			frame.blocklistBox.setListData(index);
			paramSelectBox.setSelectedIndex(-1);
			autolabels = null;
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
	}
	
	public static void loadAutoLables(){
		try {
			if (workingDir == null)
				return;
			if (algoSelectBox.getSelectedIndex() == -1)
				return;
			if (paramSelectBox.getSelectedIndex() == -1)
				return;
			
			File labelDir = new File( 
					new File(
							new File(workingDir,"results"), algoSelectBox.getSelectedItem().toString()), 
					paramSelectBox.getSelectedItem().toString()) ;
			
			BufferedReader reader = new BufferedReader(new FileReader(new File(labelDir, "labels.txt")));
			ArrayList<String> buf = new ArrayList<String>();
			String t = reader.readLine();
			while (t!=null) {
				buf.add(t);
				t = reader.readLine();
			}
			autolabels = new int[buf.size()];
			for(int i=0; i<buf.size(); i++) {
				String s = buf.get(i);
				autolabels[i] = Integer.parseInt(s);
			}
			reader.close();
			
			
			// reload list data
			frame.blocklistBox.setListData(index);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	public static void loadData() throws Exception{
		if (workingDir==null) {
			return;
		}
		
		autolabels = null;
		// read index
		File nameindexfile = new File(workingDir, "nameIndex.txt");
		
		BufferedReader reader = new BufferedReader(new FileReader(nameindexfile));
		ArrayList<String> buf = new ArrayList<String>();
		String t = reader.readLine();
		while (t!=null) {
			if (t.length() >25) {
				t = t.substring(0,25);
			}
			buf.add(t);
			t = reader.readLine();
		}
		index = new String[buf.size()];
		buf.toArray(index);
		reader.close();
		
		reader = new BufferedReader(new FileReader(new File(workingDir, "rawTFVector.txt")));
		buf = new ArrayList<String>();
		t = reader.readLine();
		while (t!=null) {
			if (t.startsWith("%")){
				;
			} else if (t.trim().length()==0){
				;
			}
			else {
				String[] tarr = t.split(" %%");
				String s = t;
				if (tarr.length==2) {
					s = tarr[0];
				}
				s = s.replace("\t", " ");
				Integer cnt = groupmembercnt.get(s);
				if (cnt ==null) {
					groupmembercnt.put(s, 1);
				} else {
					groupmembercnt.put(s, cnt +1); 
				}
				buf.add(s);
			}
			t= reader.readLine();
		}
		System.err.println("Number of groups = " + groupmembercnt.size());
		data = new String[buf.size()];
		buf.toArray(data);
		reader.close();
		
		
		reader = new BufferedReader(new FileReader(new File(workingDir, "uniqueGroup.txt")));
		groupLabels = new int[data.length];
		t = reader.readLine();
		int cnt =0;
		while (t!=null) {
			int grp = Integer.parseInt(t);
			groupLabels[cnt] = grp;
			cnt +=1;
			t= reader.readLine();
		}
		
		data = new String[buf.size()];
		buf.toArray(data);
		reader.close();
		
		File labelfile = new File(workingDir, "manualLabels");
		if (labelfile.exists()) {
			ObjectInputStream objin = new ObjectInputStream(
					new FileInputStream(labelfile));
			comments = (ArrayList<ManualComment>) objin.readObject();
			int ct =0;
			for (ManualComment c : comments) {
				commentsText.addItem(c.comment);
				System.err.println(ct +" " +c.comment);
				ct++;
			}
			manualComments = (HashMap<Integer, Integer>) objin.readObject();
			manualLabels = (HashMap<Integer, Integer>) objin.readObject();
			objin.close();
		}
		
		frame.blocklistBox.setListData(index);
		String[] algos = new File(workingDir,"results").list();
		algoSelectBox.removeAllItems();
		if (algos==null)
			return;
		for (int i=0; i<algos.length; i++) {
			algoSelectBox.insertItemAt(algos[i], i);
		}
		
	}
	
	
	static String[] msgtypes;
	public static String getFile(String id, String feature) throws IOException {
		File f = new File(workingDir, "textLog");
		StringBuffer buf = new StringBuffer();
		
		if (f.exists()) {
			BufferedReader reader = new BufferedReader(new FileReader(new File(
					f, id)));
			String t = reader.readLine();
			while (t != null) {
				buf.append(t).append("\n");
				t = reader.readLine();
			}
			reader.close();
		} else {
			if (msgtypes==null) {
				FieldNamesGen fngen = new FieldNamesGen(workingDir);
				msgtypes = fngen.getTypeStrings();
			}
			String[] featurearr = feature.split(" ");
			for (int i=0; i< featurearr.length; i++) {
				String s = featurearr[i];
				try {
					int msgcnt = Integer.parseInt(s);
					if (msgcnt==0) 
						continue;
					String typestr = msgtypes[i];
					buf.append(msgcnt).append(" X ").append(typestr).append("\n");
				} catch (NumberFormatException e) {
					//e.printStackTrace();
				}
			}
		}
		return buf.toString();
	}
	
	private void numberError() {
		JOptionPane.showInternalMessageDialog(this, "value must be integers");
	}
	
	private void notFoundError() {
		JOptionPane.showInternalMessageDialog(this, "no such block");
	}
	
	public LabelInterface() {
		// get screen dimensions

		Toolkit kit = Toolkit.getDefaultToolkit();
		Dimension screenSize = kit.getScreenSize();
		int screenHeight = screenSize.height;
		int screenWidth = screenSize.width;

		// set frame width, height and let platform pick screen location

		setSize(screenWidth / 2, screenHeight / 2);
		setLocationByPlatform(true);

		// set frame icon and title

		Image img = kit.getImage("icon.gif");
		setIconImage(img);
		setTitle("Result Viewer");
		
		
		// query panel
		
		final JPanel queryPanel = new JPanel();
		idBox = new JTextField();
		queryPanel.setLayout(new GridLayout(2,4));
		final JLabel indexNumLabel = new JLabel("",SwingConstants.LEFT);
		final JLabel autoAlarmFileLabel = new JLabel("",SwingConstants.LEFT);
		final JButton nextgroupButton = new JButton("NextGroup");
		algoSelectBox = new JComboBox();
		paramSelectBox = new JComboBox();
		
		JComboBox dataSetSelectBox = new JComboBox(LabelInterface.dataSetList);
		dataSetSelectBox.setSelectedIndex(-1);
		dataSetSelectBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JComboBox cb = (JComboBox)e.getSource();
		        int select = cb.getSelectedIndex();
		        //LabelInterface.dataSetSelect = select;
		        //LabelInterface.algoSelect = -1;
		        algoSelectBox.setSelectedIndex(-1);
		        //LabelInterface.paramSelect = -1;
		        paramSelectBox.setSelectedIndex(-1);
		        
		        String dataSet = (String) cb.getSelectedItem();
		        workingDir = new File(DATA_DIR,dataSet);
		        reLoadData();
			}
		});
		queryPanel.add(dataSetSelectBox);
		
		
		queryPanel.add(algoSelectBox);
		algoSelectBox.setSelectedIndex(-1);
		algoSelectBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JComboBox cb = (JComboBox)e.getSource();
		        int select = cb.getSelectedIndex();
		        String algoDir = (String) cb.getSelectedItem();
		        loadParamList();
			}
		});
		
		queryPanel.add(paramSelectBox);
		paramSelectBox.setSelectedIndex(-1);
		paramSelectBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JComboBox cb = (JComboBox)e.getSource();
		        int select = cb.getSelectedIndex();
		        String paramDir = (String) cb.getSelectedItem();
		        //blockIdLabel.setText(paramDir);
		        autoAlarmFileLabel.setText(paramDir );
		        loadAutoLables();
			}
		});
		queryPanel.add(autoAlarmFileLabel);
		
		
		queryPanel.add(new JLabel("Block Index (from 1)", SwingConstants.LEFT));
		queryPanel.add(idBox);
		
		nextgroupButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int i=0;
				for(i=0; i<groupLabels.length; i++) {
					int grp = groupLabels[i];
					if (! manualLabels.containsKey(grp))
						break;
					if (manualLabels.get(grp) ==-1)
						break;
				}
				if (i < groupLabels.length) {
					blocklistBox.setSelectedIndex(i);
				}
			}			
		});
		
		//queryPanel.add(nextgroupButton);
		queryPanel.add(indexNumLabel);
		
		//txtPanel.add(queryButton);
		idBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String t = idBox.getText();
				int ind = 0;
				String blockid;
				try {
					ind = Integer.parseInt(t);
					ind = ind -1; // match Matlab index
				} catch (NumberFormatException e1) {
					numberError();
					return;
				}
				try {
					blockid = LabelInterface.index[ind];
				} catch (RuntimeException e1) {
					notFoundError();
					return;
				}
				//detailText.setText(LabelInterface.getFile(blockid));
				blocklistBox.setSelectedIndex(ind);
			}
		});
		
		// detail panel
		final JPanel detailPanel = new JPanel();
		detailPanel.setLayout(new BorderLayout());
		final JTextArea detailText = new JTextArea(80, 80);
		final JScrollPane detailScroll = new JScrollPane(detailText);
		detailPanel.add(detailScroll, BorderLayout.CENTER);
		
		
		final JPanel commentsPanel = new JPanel();
		final JComboBox manuallabelbox = new JComboBox();
		manuallabelbox.addItem("normal");
		manuallabelbox.addItem("abnormal");
		manuallabelbox.setSelectedIndex(-1);
		manuallabelbox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JComboBox cb = (JComboBox)e.getSource();
		        int select = cb.getSelectedIndex();
		        int blockind = blocklistBox.getSelectedIndex();
		        int grp = groupLabels[blockind];
		        System.err.println(grp);
		        manualLabels.put(grp, select);
		        saveLabels();
			}
		});
		
		commentsPanel.setLayout(new GridLayout(2,2));
		commentsPanel.add(manuallabelbox);
		commentsPanel.add(nextgroupButton);
		
		commentsText = new JComboBox();
		commentsText.setEditable(true);
		commentsText.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JComboBox cb = (JComboBox)e.getSource();
		        int select = cb.getSelectedIndex();
		        String comment = (String) cb.getSelectedItem();
		        int blockind = blocklistBox.getSelectedIndex();
		        if (blockind < 0)
		        	return;
		        int grp = groupLabels[blockind];
		        if (select==-1) {
		        	if (comment==null)
		        		return;
		        	ManualComment com = new ManualComment(cb.getItemCount(), comment);
		        	comments.add(com);
		        	manualComments.put(grp, comments.size()-1);
		        	cb.addItem(com.comment);
		        	System.err.println(select);
			        System.err.println(comment);
		        } else {
		        	manualComments.put(grp, select);
		        }
		        saveLabels();
			}
		});
		//final JScrollPane commentsScroll = new JScrollPane(commentsText);
		commentsPanel.add(commentsText);
		
		JButton outputButton = new JButton("outputTextResult");
		outputButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				outputLabelData();
			}
		});
		commentsPanel.add(outputButton);
		
		detailPanel.add(commentsPanel, BorderLayout.SOUTH);
		
		add(queryPanel, BorderLayout.NORTH);
		add(detailPanel, BorderLayout.CENTER);
		
		blocklistBox = new JList();
		blocklistBox.setCellRenderer(new BlockListRenderer());
		//JSplitPane data =new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, blocklistBox, detailScroll);
		JScrollPane listScroll = new JScrollPane(blocklistBox);
		add(listScroll, BorderLayout.WEST);
		
		blocklistBox.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				detailText.setText( e.getFirstIndex() +" " +e.getLastIndex());
				StringBuffer display = new StringBuffer();
				int[] indices = blocklistBox.getSelectedIndices();
				Object[] values = blocklistBox.getSelectedValues();
				boolean has_error = false;
				String fileid="";
				String autolabel="";
				for (int i=0; i<indices.length; i++){
					int index = indices[i]; 
					fileid = (String) values[i];
					String feature = LabelInterface.data[index];
					int label = -1;
					if (autolabels!=null) {
						label = autolabels[index];
						autolabel = label==0?"NORMAL":"ABNORMAL";
					} else {
						autolabel = "Auto Label not found";
					}
					int grp = groupLabels[index];
					display.append(index+1).append("\n"); // +1 to match display to Matlab index
					display.append("unique:").append( grp + " " +index).append("\n");
					display.append("Group size = " ).append( groupmembercnt.get(feature) ).append("\n");
					int manual_label = -1;
					if (manualLabels.containsKey(grp))
						manual_label = manualLabels.get(grp);
					manuallabelbox.setSelectedIndex(manual_label);
					display.append("manual label:" + manual_label).append("\n");
					String comments = "No comments";
					int commentind = -1;
					if (manualComments.containsKey(grp)) {
						commentind = manualComments.get(grp);
						comments = LabelInterface.comments.get( commentind ).comment;
					}
					commentsText.setSelectedIndex(commentind);
					display.append("comments: " + comments).append("\n");
					display.append(fileid).append("\n");
					display.append(feature).append("\n");
					
					if (label!=0) {
						has_error = true;
					}
					display.append(autolabel).append("\n");
					try {
						display.append(LabelInterface.getFile(fileid, feature))
						.append("===============================================================\n");
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					display.append("\n");
				}
				detailText.setText( display.toString() ) ;
				detailText.setForeground(Color.BLACK);
				if (indices.length == 1) {
					if (has_error) {
						detailText.setForeground(Color.RED);
						indexNumLabel.setForeground(Color.RED);
					} else {
						indexNumLabel.setForeground(Color.BLACK);
					}
					indexNumLabel.setText(indices[0]+" " + fileid);
				} else {
					indexNumLabel.setText("multiple");
					indexNumLabel.setForeground(Color.BLACK);
				}
				detailText.setCaretPosition(0);
				
			}
		});
	}
	
	private void displayInfo() {
		
	}
	
	private class BlockListRenderer extends JLabel implements ListCellRenderer {
		
		public Component getListCellRendererComponent(JList list, Object value,
				int index, boolean isSelected, boolean cellHasFocus) {
			boolean isError = false;
			if ( LabelInterface.autolabels==null || LabelInterface.autolabels.length<=0) {
				this.setText(value.toString());
				return this;
			}
			
			int autoLabel = LabelInterface.autolabels[index];
			//System.err.println(d);
			if (autoLabel !=0){
				isError = true;
			}
			
			if (isError) {
				this.setForeground(Color.RED);
				//System.err.println("ERROR!");
			} else {
				this.setForeground(Color.BLACK);
			}
			
			this.setText(value.toString());
			return this;
		}	
	}
	
	
}

class ManualComment implements Serializable{
	
	long commentid;
	String comment;
	
	public ManualComment(int commentid, String comment) {
		this.commentid = commentid;
		this.comment = comment;
	}
}
