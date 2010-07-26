package org.radlab.parser.source;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import org.eclipse.cdt.core.dom.ICodeReaderFactory;
import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTInitializer;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorIncludeStatement;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorMacroDefinition;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IType;
import org.eclipse.cdt.core.dom.lrparser.c99.C99Language;
import org.eclipse.cdt.core.dom.lrparser.gnu.GCCLanguage;
import org.eclipse.cdt.core.parser.CodeReader;
import org.eclipse.cdt.core.parser.DefaultLogService;
import org.eclipse.cdt.core.parser.IParserLogService;
import org.eclipse.cdt.core.parser.ScannerInfo;

import utils.FileCodeReaderFactory;


public class DebugFunctionParser {
	static String inputFileName;
	static String directoryName;
	static String contents;
	static String currentFun = null;
	static Scanner input;
	static int subflag;
	/** function delimiter*/
	static String function_delimiter="\\w+(?s).+\\(.*?\"(?s).*\"(?s).*\\)(?s).*";
	//static LinkedList<IASTExpression> list=new LinkedList<IASTExpression>();	
	static FunctionInfo1 fn=new FunctionInfo1();
	static int count;
	/** parameter flag*/
	static int flag=0; 
	static HashMap<String,Function> hm=new HashMap<String,Function>(); //FIXME
    
	/** ASTVisitor parse_vistor which analyzes the pattern of IASTExpression and IASTStatement*/
	static ASTVisitor parse_visitor = new ASTVisitor() {  
		// treat the beginning of statement as the end of the expression
    	public int visit(IASTStatement name) {
    		//System.err.println("stat:\t"+name.getRawSignature());
        	flag=0;
        	
        	if (!fn.name.equals("")){ 
        		  processList();       
        		  fn.clear();
        		  count=0;
        	}   
        	
            return ASTVisitor.PROCESS_CONTINUE;
          }      
    	
    	public int visit(IASTInitializer name) { //FIXME
      	  //System.err.println("initializer: "+name.getRawSignature()); 
      	  return ASTVisitor.PROCESS_CONTINUE;
    	}
    	
    	public int visit(IASTDeclarator name) { //FIXME
    		if (name.getRawSignature().matches(".*\\(.*\\).*"))
        	  System.err.println("declarator: "+name.getRawSignature()); 
        	return ASTVisitor.PROCESS_CONTINUE;
      	}
    	
    	public int visit(IASTDeclaration name) { //FIXME
      	  //System.err.println("declaration: "+name.getRawSignature()); 
      	  return ASTVisitor.PROCESS_CONTINUE;
    	}
    	
    	//find the strings from expressions starting with func name and with "strings"
          public int visit(IASTExpression name) { //FIXME
        	  //System.err.println("exp: "+name.getRawSignature());
        	  /*
        	  try {    //the bug lies in this func, it skips everything after the statement    		        		  
        		  switch(flag){
        			case 1:        				
        				flag++; fn.name=name.getRawSignature(); //name
        				return ASTVisitor.PROCESS_CONTINUE;
        			case 2:  
        				if (!name.getRawSignature().matches("\"(?s).*\""))//format        				
        					return ASTVisitor.PROCESS_CONTINUE;
        				fn.format=name.getRawSignature();        		
        				flag++;         				
    					//System.err.println("fmt:"+name.getRawSignature()); //test
    					return ASTVisitor.PROCESS_CONTINUE;
        			case 3: 
        				if (name.getRawSignature().matches("\"(?s).*\"")){//Add-on to deal with (cond? "str1" : "str2")
        					processList();
        					fn.format=name.getRawSignature();
        					return ASTVisitor.PROCESS_SKIP;
        				}        				
        				fn.variables.add(name.getRawSignature()); //list of variables
        				//System.err.println("variable:"+name.getRawSignature()); //test
        				return ASTVisitor.PROCESS_SKIP;				
        		}
        		
        		//flag=0 check whether the expression satisfies the function (string) requirement
        		String str=name.getRawSignature();
        		IType type=name.getExpressionType();
        		if (str.matches(function_delimiter)&&type.toString().equals("void")){
        			//System.err.println("str: "+name.getRawSignature());
        			// line        			
        			fn.line=name;
        			flag=1; // start the process here
        		}		
        		//else System.err.println("str: "+name.getRawSignature());
        		return ASTVisitor.PROCESS_CONTINUE;
			  } catch (Throwable e) {
				System.err.println("errors: "+e.getMessage()); //FIXME
				e.printStackTrace();
			  }	
			  */
              return ASTVisitor.PROCESS_CONTINUE;
          }                           
          
          };
                    
          
  /** main function, initialize parser and parse the passage */        
  public static void main(String[] str) throws Exception {
    		inputFileName = str[0]; 
    		directoryName = str[1];
    		String outputFileName = str[2];    		
    		try {				
    			input = new Scanner (new FileReader (inputFileName));
    			System.setOut(new PrintStream(outputFileName));//FIXME (remove later)
    		} catch (FileNotFoundException e) {
    			System.err.printf("Error: could not open %s%n", inputFileName);
    			e.printStackTrace();
    		}
    		
    		input.useDelimiter("\\Z");
    		String code =input.next();  
    		//code = code.replaceAll("STACK_OF\\(.*?\\)", "int");    	    		 		    		    		
    		//System.err.println(code);
    		IParserLogService log = new DefaultLogService();   
            CodeReader reader = new CodeReader(code.toCharArray());
            
            final Map<String,String> definedSymbols = new HashMap<String, String>();
            //definedSymbols.put("STACK_OF(A)", "char*");
            String[] includePaths = new String[2];
            //includePaths[0] = "";
            ScannerInfo info = new ScannerInfo(definedSymbols,includePaths);
            ICodeReaderFactory readerFactory = FileCodeReaderFactory.getInstance();
            try{
            IASTTranslationUnit translationUnit = 
            	C99Language.getDefault().getASTTranslationUnit(reader, info, readerFactory, null, log);            
            parse_visitor.shouldVisitStatements = true;
            parse_visitor.shouldVisitExpressions = true;
            parse_visitor.shouldVisitAmbiguousNodes = true;
            parse_visitor.shouldVisitDeclarations = true;
            parse_visitor.shouldVisitDeclarators = true;
            parse_visitor.shouldVisitInitializers = true;
            parse_visitor.shouldVisitProblems = true;   
            
           
            
            
            
            
            
            
            
            
            
            
            
            
        //new codes
            final HashSet<String> set = new HashSet<String>();
            Matcher matc;
            IASTPreprocessorIncludeStatement[] Inclu = translationUnit.getIncludeDirectives();
            for (IASTPreprocessorIncludeStatement incl: Inclu){            	
            	 matc = Pattern.compile(".*include.*[<\"/](.*.h).*")
				.matcher(incl.getRawSignature());
				if (matc.matches())
					set.add(matc.group(1));
            }
            
            final HashMap<String, String> defintionHM = new HashMap<String, String>();
            if (directoryName != null){
            	try {
    				new FileTraversal() {
    					public void onFile( final File f ) {
    						/*if (!set.contains(f.getName()))
    							return;
    							*/	
    						//process file
    						//System.err.println(f.getName());
    						if (!(f.getName().matches(".*.h")||f.getName().matches(".*.c"))||f==null)
    							return;
    						LinkedList<IASTPreprocessorMacroDefinition> Macros 
    							= retrieveIncludes (f);
    						//mac.getName() //mac.getExpansion()
    						for (IASTPreprocessorMacroDefinition mac: Macros){
    							//if (mac.getName().getRawSignature().contains("STACK_OF"))
    							//System.out.println(mac.getRawSignature());
    							/*	
    							definedSymbols.put(mac.getName().getRawSignature(), 
    									mac.getExpansion());*/
    							defintionHM.put(mac.getName().getRawSignature(), mac.getExpansion());
    						}
    						}
    				}.traverse(new File(directoryName));
    				
    				new FileTraversal() {
    					public void onFile( final File f ) {
    						/*if (!set.contains(f.getName()))
    							return;	*/

    						//System.err.println(f.getName());
    						
    						//process file
    						LinkedList<IASTPreprocessorMacroDefinition> Macros 
    							= retrieveIncludes (f);
    						//mac.getName() //mac.getExpansion()
    						for (IASTPreprocessorMacroDefinition mac: Macros){
    							//if (mac.getName().getRawSignature().contains("STACK_OF"))
    							//System.err.println(mac.getRawSignature());    								
    							/*definedSymbols.put(mac.getName().getRawSignature(), 
    									mac.getExpansion());*/
    							defintionHM.put(mac.getName().getRawSignature(), mac.getExpansion());
    						}
    						}
    				}.traverse(new File("c:\\Dev-Cpp"));
    				
    			} catch (Exception e) {
    				System.err.println("ioexception:"+e.getMessage());
    			} 
            }
                         
            
         //new code   
            
            

            
            
            
            
            
            
            
            //try{
            translationUnit.accept(parse_visitor);
            if (!fn.name.isEmpty())
            	processList();                                    
            }catch(Throwable e){
            	System.err.println(e.getMessage()); //FIXME  
            	e.printStackTrace();
            }
      	}
        	
  
  
  
  
  
  
  //new code2
  
  
  static LinkedList<IASTPreprocessorMacroDefinition> retrieveIncludes (File f){ 
	  		LinkedList<IASTPreprocessorMacroDefinition> macros = 
	  			new LinkedList<IASTPreprocessorMacroDefinition>();
	  		String code = null;
            try {
  				Scanner finput= new Scanner(f);
  				finput.useDelimiter("\\Z");
  	      		code =finput.next();
  			} catch (FileNotFoundException e) {
  				e.printStackTrace();
  			}

  			Map<String,String> definedSymbols = new HashMap<String, String>();
              String[] includePaths = new String[0];
  			CodeReader reader = new CodeReader(code.toCharArray());
  			try {
				IASTTranslationUnit TU2 = 
				  	GCCLanguage.getDefault().getASTTranslationUnit(reader, 
				  			new ScannerInfo(definedSymbols,includePaths), 
				  			FileCodeReaderFactory.getInstance(), null, 
				  			new DefaultLogService());
				
				IASTPreprocessorMacroDefinition[] Macro = TU2.getMacroDefinitions();
				for (IASTPreprocessorMacroDefinition mac: Macro)
					macros.add(mac);
			} catch (Exception e) {
		
				System.err.println("skip "+f.getName()+" can't resolved");
			}
			return macros;
  }
  
  
  





//new code2
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
	
    /** the funciton which output the useful information of the log from the stored list 
     * list [fn_name, all args, string format, args... ]
	 *  note that the 2nd member is useless unless there is only no sub-in arguments
	   */
        /** the funciton which output the useful information of the log from the stored list */
        static void processList(){
    		//format: function name, string format in (.*), args, location, filename 
        	//fn.format=processString(fn.format); //changed by Bruce 07/22/09    		
        	if (fn.format.contains("%"))
    			subflag = 1;
        	else
        		subflag = 0;	
        	//Changed for string comparison with binary file
    		//int k = inputFileName.lastIndexOf("\\");   
        	//System.out.println(fn.format); //DELETE
    		String fn_name=fn.name,  
    		fmt=fn.format, loc="Location:"+fn.line.getFileLocation(),     	    		
    		//filename="Filename:"+inputFileName.substring(k+1), args=""; // chanced according to line 149    		
    		filename="Filename:"+inputFileName, args="";    	

    		/* //old version
    		 String line=fn.line.getRawSignature();
    		 for (int i=0; i<count; i++)
    			args+=fn.variables.get(i)+", ";
    		 String info=line+"\t"+"Format:"+fmt+"\t"+"vars:"+args+"\t"+loc+"\t"+filename;
    		 */
    		
    		for (int i=0; i<fn.fmts.size()&&i<fn.variables.size(); i++)
    			//args+=fn.variables.get(i)+", ";
    			args+="\t["+fn.fmts.get(i)+" "+fn.variables.get(i)+"]"+"\r\n";
    		//String info=line+"\t"+"Format:"+fmt+"\t"+"vars:"+args+"\t"+loc+"\t"+filename;
    		String info="\t["+fmt+"]\r\n\t"+filename+"\t"+loc+"\t"+"functionName:"+fn_name+"\r\n";    
    		add_to_Hashmap(fn_name,info, subflag);    		
    	}
        
        /** process the formated string to %x->(.*) */
    	static String processString(String format){
    		String newFmt=format, fmtWord; 
    		Matcher mat;
    		Scanner fmtInp = new Scanner (format);
    		count=0;
    		
    		if (format.contains("%")){
    			subflag=1;
    			fmtInp.useDelimiter("%");
    			newFmt=fmtInp.next();
    			while (fmtInp.hasNext()){
    				count++;
    				fmtWord=fmtInp.next();
    				mat = Pattern.compile("([\\d\\.]*l?[dioxXucsfeEgG])(.*)")
    				.matcher(fmtWord);
    				if (mat.matches()){
    					fn.fmts.add("(%"+mat.group(1)+")");
    					newFmt+="(.*)"+mat.group(2);
    					continue;
    				}
    				newFmt+=fmtWord;
    			}
    		} else subflag=0;
			return newFmt;
    	}
		
	/** add the function to Main's hashmap */
	static void add_to_Hashmap(String name, String info, int use_subarg){
		if (hm.containsKey(name)){//Main.hm.containsKey(name)){
			Function fn=hm.get(name);//Main.hm.get(name);
			fn.count++;
			fn.info+="\n"+fn.count+". "+info;
			fn.Use_Sub_arg_Number+=use_subarg;
			System.out.println(info);//delete
			//System.err.println(fn.fnName+" "+fn.Use_Sub_arg_Number+" "+use_subarg);			
			return;
		}
		//System.err.println(name);
		hm.put(name, new Function(name, info, use_subarg));//Main.hm.put(name, new Function(name, info, use_subarg));
		System.out.println(info);//delete
	}
}
		
class FunctionInfo1{
	IASTExpression line;
	String name="", format;
	LinkedList<String> variables=new LinkedList<String>(); 
	LinkedList<String> fmts=new LinkedList<String>();
	void clear(){
		name="";
		line=null;
		format="";
		variables.clear();
		fmts.clear();
	}
}


