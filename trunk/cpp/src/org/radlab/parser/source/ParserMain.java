package org.radlab.parser.source;
/* the main function executing the detector and parser */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Scanner;

public class ParserMain {	
	static String inputFileName;
	static Scanner input;
	static HashMap<String,Function> hm = new HashMap<String,Function>();
	static LinkedList<String> includepath = new LinkedList<String>();
	static HashMap<String, HashMap<String, String>> FnDefHM = new HashMap<String, HashMap<String, String>>();
	
	public static void main (String[] args) {
		if (args.length!=2&&args.length!=3){
			System.err.println("Supply inputfile name ad includepath filename!!!");
			System.exit(0);
		}
		
		System.err.println("starting....");
		inputFileName = args[0];
		String includePathFileName = args[1];
		
		//test 
		if (args.length == 3){
		String outputFileName = args[2];    		
//		try {				
//			//System.setOut(new PrintStream(outputFileName));//FIXME (remove later)
//		} catch (FileNotFoundException e) {
//			System.err.printf("Error: could not open %s%n", inputFileName);
//			e.printStackTrace();
//		}
		}
		
		
		
//		InitializeIncludePath(includePathFileName);
		functionCollection();
		Output();
	}
	
	/** Output the result */
	static void Output(){
		Function func;
		for (String funName:hm.keySet()){						
			func=hm.get(funName);	
			System.err.println("Function: "+func.fnName);
			System.err.println(func.info);
		}
	}		
	
	/** Create a list of log-printing functions */	
	static void functionCollection(){
	  
	  //System.err.println("parsing file:: " + inputFileName);
	  
	  if( inputFileName !=null ) {
	    if( inputFileName.endsWith(".cc") 
	        || inputFileName.endsWith(".c")
	        || inputFileName.endsWith(".h")
	    ) {
	      Func_in_CFile (new File(inputFileName));
	    }
	  }
//		if (inputFileName!=null){
//			try {
//				new FileTraversal() {
//					public void onFile( final File f ) {
//						if (!f.getName().matches(".*\\.c{1,2}"))
//							return;	
//						Func_in_CFile (f);
//					}
//				}.traverse(new File(inputFileName));
//			} catch (IOException e) {
//				System.err.println("ioexception:"+e.getMessage());
//				System.exit(1);
//			}
//		} 
	}	
	
	/** Detect the function in each file 
	 * @throws Exception */
	static void Func_in_CFile (File f) {
		try {
		    //System.err.println("-->parsing " + f.getAbsolutePath());
			FunctionParser.main(new String[]{f.getPath()});					
		} catch (Exception e) {
		}	
	}	
	
	/** Create an array of including paths */
	static void InitializeIncludePath(String includePathFileName){
		File includeFile=new File (includePathFileName);		
		try {
			Scanner input = new Scanner(includeFile);
			while (input.hasNextLine())
				includepath.add(input.nextLine());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	  
}
