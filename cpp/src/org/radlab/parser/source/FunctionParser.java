package org.radlab.parser.source;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Scanner;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.cdt.core.dom.ICodeReaderFactory;
import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
//import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorIncludeStatement;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorMacroDefinition;
import org.eclipse.cdt.core.dom.ast.IASTProblem;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;
import org.eclipse.cdt.core.dom.ast.IType;
import org.eclipse.cdt.core.dom.ast.cpp.CPPASTVisitor;
import org.eclipse.cdt.core.dom.ast.gnu.cpp.GPPLanguage;
import org.eclipse.cdt.core.dom.lrparser.c99.C99Language;
import org.eclipse.cdt.core.parser.CodeReader;
import org.eclipse.cdt.core.parser.DefaultLogService;
import org.eclipse.cdt.core.parser.IParserLogService;
import org.eclipse.cdt.core.parser.IScannerInfo;
import org.eclipse.cdt.core.parser.ParserFactory;
import org.eclipse.cdt.core.parser.ScannerInfo;
import org.eclipse.cdt.internal.core.dom.lrparser.cpp.CPPExpressionParser;
import org.eclipse.cdt.internal.core.dom.lrparser.cpp.CPPParser;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTBinaryExpression;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTCompoundStatement;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTExpressionList;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTExpressionStatement;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionCallExpression;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTIdExpression;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTLiteralExpression;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTUnaryExpression;
import org.eclipse.cdt.internal.core.pdom.dom.cpp.CPPFindBinding.CPPBindingBTreeComparator;
import org.eclipse.core.runtime.CoreException;

import utils.FileCodeReaderFactory;


public class FunctionParser {
	static String inputFileName;
	static String directoryName;
	static String currentFunc = null;
	static Scanner input;
	static int subflag;
	/** function delimiter*/
	static String function_delimiter="\\w+(?s).+\\(.*?\"(?s).*\"(?s).*\\)(?s).*";
	//static LinkedList<IASTExpression> list=new LinkedList<IASTExpression>();	
	static FunctionInfo fn=new FunctionInfo();
	static int count;
	/** parameter flag*/
	static int flag=0; 
	static Map<String,String> definedSymbols;
    
	/** ASTVisitor parse_vistor which analyzes the pattern of IASTExpression and IASTStatement*/
	static ASTVisitor parse_visitor;
        
  /** main function, initialize parser and parse the passage */        
  public static void main(String[] str) throws Exception {
    		inputFileName = str[0]; 
    		Matcher mat = Pattern.compile("(.*\\\\).*")
    		.matcher(inputFileName);
    		if (mat.matches())
    			ParserMain.includepath.add(0, mat.group(1));    		    	
    		try {				
    			input = new Scanner (new FileReader (inputFileName));
    		} catch (FileNotFoundException e) {
    			System.err.printf("Error: could not open %s%n", inputFileName);
    		}
    		
    		input.useDelimiter("\\Z");
    		String code =input.next();
    		
    		//System.err.println(code);
            definedSymbols = new HashMap<String, String>();
            definedSymbols.put("STACK_OF(A)", "char*");
            try{
            IASTTranslationUnit translationUnit;
            if(inputFileName.endsWith("cc"))
              translationUnit = InitializeTranslationUnit(code, definedSymbols, true);
            else 
              translationUnit = InitializeTranslationUnit(code, definedSymbols, false);
            //IASTPreprocessorIncludeStatement[] Include = translationUnit.getIncludeDirectives();
            definedSymbols = new HashMap<String, String>();
            //FillInFnMacroDnHM(Include); //Recursive call of fill in Filename and Macro Definition HM
            
            if(inputFileName.endsWith("cc") || inputFileName.endsWith(".h")) {
              translationUnit =  // reset translation unit with definedSymbols
                InitializeTranslationUnit(code, definedSymbols, true);
            } else {
              translationUnit =  // reset translation unit with definedSymbols
                InitializeTranslationUnit(code, definedSymbols, false);
            }
            
            if(translationUnit==null) {
              return;
            }
            
            String srcname = new File(inputFileName).getName();
            
            if(inputFileName.endsWith(".cc") || inputFileName.endsWith(".h")) {
              //System.err.println("using cpp");
              parse_visitor = new CPPGoogleLogVisitor(srcname, GlobalResultCollector.results);
            } else {
              parse_visitor = new CLogPrintVisitor();
            }
            
//            parse_visitor.shouldVisitStatements = true;
//            parse_visitor.shouldVisitExpressions = true;
//            //parse_visitor.shouldVisitNames = true;
//            //parse_visitor.shouldVisitAmbiguousNodes = true;
//            //parse_visitor.shouldVisitDeclarations = true;
//            parse_visitor.shouldVisitDeclarators = false;
//            //parse_visitor.shouldVisitInitializers = true;
//            //parse_visitor.shouldVisitProblems = true;
            
            try{
              translationUnit.accept(parse_visitor);
            } catch(Exception e){
            	e.printStackTrace();
            }
            
            if (!fn.name.isEmpty())
            	processList();
            }catch(Throwable e){
              e.printStackTrace();
              //System.err.println(e.getMessage()); //FIXME            	
            }
            
            GlobalResultCollector.parsedSourceFiles.add( new File(inputFileName).getName() );
      	}
  
  /** Initialize the translation unit by supplying the code string and defined symbol list */
  static IASTTranslationUnit InitializeTranslationUnit(String code, Map<String,String> definedSymbols, boolean usecpp){
      String[] includePaths = new String[0];
      IScannerInfo info = new ScannerInfo(definedSymbols,includePaths);
	  IParserLogService log = ParserFactory.createDefaultLogService();
      CodeReader reader = new CodeReader(code.toCharArray());
      ICodeReaderFactory readerFactory = FileCodeReaderFactory.getInstance();
      try {
        if (usecpp) 
          return GPPLanguage.getDefault().getASTTranslationUnit(reader, info, readerFactory, GlobalResultCollector.bindingindex, log);
        else 
          return C99Language.getDefault().getASTTranslationUnit(reader, info, readerFactory, GlobalResultCollector.bindingindex, log);
	} catch (CoreException e) {
		e.printStackTrace();
	} catch (UnsupportedOperationException ex) {
	    ex.printStackTrace();
	}
	return null;
  }
  
  /** Fill in the (Filename - Macro Definition) HashMap, called recursively */
  static LinkedList<IASTPreprocessorMacroDefinition> 
  FillInFnMacroDnHM(IASTPreprocessorIncludeStatement[] Include){
	  try{
      Matcher matc;
      String[] pathfiles = new String [Include.length];
      for (int i = 0; i<Include.length; i++){            	
    	  matc = Pattern.compile(".*include\\s*[<\"](.*.h).*")
    	  	.matcher(Include[i].getRawSignature());
    	  if (matc.matches())
    		  pathfiles[i] = matc.group(1);
      }
      return ProcessIncludePath(pathfiles);
	  }catch (Throwable e){
		  return new LinkedList<IASTPreprocessorMacroDefinition>();
	  }
  }
  
  /** check whether the recursion should end here*/
  static boolean checkEnd(String[] pathfiles){	
	  String includePathFile = null, includepath;;	
	  int size = ParserMain.includepath.size();
	  File f;
	  for (String pathfile: pathfiles){		 
		  for (int i=0; i < size; i++){
			  includepath = ParserMain.includepath.get(i);
			  includePathFile = includepath + pathfile;
			  f = new File (includePathFile);			  
			  if (f.exists()&&!ParserMain.FnDefHM.containsKey(includePathFile))				  
				  return false;			 
		  }
	  }
	  return true; //either all files do not exit, or they are contained in HM	  
  }
  
  /** fill the filename definition HashMap */
  static LinkedList<IASTPreprocessorMacroDefinition> ProcessIncludePath(String[] pathfiles){
	  String includePathFile = null;	
	  int size = ParserMain.includepath.size();
	  HashMap<String, String> defintionHM;
	  String includepath;
	  LinkedList<IASTPreprocessorMacroDefinition> MACRODEFS = 
		  new LinkedList<IASTPreprocessorMacroDefinition>();
	  
	  if (checkEnd(pathfiles))
		  return new LinkedList<IASTPreprocessorMacroDefinition>();
	  for (String pathfile: pathfiles){		 
		  for (int i=0; i < size; i++){
			  includepath = ParserMain.includepath.get(i);
			  includePathFile = new File( new File(includepath),  pathfile).getAbsolutePath();
			  //System.err.println(includePathFile);
			  if (ParserMain.FnDefHM.containsKey(includePathFile))
				  break;
				  File f = new File (includePathFile);
				  if (!f.exists()){
					  if (i == size-1)
						  System.err.println("include file " + pathfile +
					  		" is not found from the include path supplied");
					  continue;
				  }
				  ParserMain.FnDefHM.put(includePathFile, null);
				  LinkedList<IASTPreprocessorMacroDefinition> MacroDefs 
					= retrieveMacroDef (f);
				  MACRODEFS.addAll(MacroDefs);
				  defintionHM = new HashMap<String, String>();
				  for (IASTPreprocessorMacroDefinition def: MacroDefs){
					defintionHM.put(def.getName().getRawSignature(), def.getExpansion());
					//System.out.println(def.getRawSignature());
				  }
				  ParserMain.FnDefHM.put(includePathFile, defintionHM);				  
				  break;			  
		  }		
		  if (ParserMain.FnDefHM.containsKey(includePathFile)){
			  HashMap<String, String> defHM = ParserMain.FnDefHM.get(includePathFile);
			  for (String key: defHM.keySet())
				  if (defHM.containsKey(key))
					  definedSymbols.put(key, defHM.get(key));			  
		  }	
	  }
	  return MACRODEFS;
  }
  
  /** Retrieve the MacroDefinition from the file  */
  static LinkedList<IASTPreprocessorMacroDefinition> retrieveMacroDef (File f) { 
		LinkedList<IASTPreprocessorMacroDefinition> macros = 
			new LinkedList<IASTPreprocessorMacroDefinition>();
		String code = null;
			Scanner finput = null;
			try {
				finput = new Scanner(f);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
				return new LinkedList<IASTPreprocessorMacroDefinition>();
			}
			finput.useDelimiter("\\Z");
      		code =finput.next();

		Map<String,String> definedSymbols = new HashMap<String, String>();
        String[] includePaths = new String[0];
		CodeReader reader = new CodeReader(code.toCharArray());
		try {
			IASTTranslationUnit TU2 = 
			  	GPPLanguage.getDefault().getASTTranslationUnit(reader, 
			  			new ScannerInfo(definedSymbols,includePaths), 
			  			FileCodeReaderFactory.getInstance(), null, 
			  			new DefaultLogService());
			
			IASTPreprocessorMacroDefinition[] Macro = TU2.getMacroDefinitions();
			for (IASTPreprocessorMacroDefinition mac: Macro)
				macros.add(mac);
			
			
			IASTPreprocessorIncludeStatement[] Include = TU2.getIncludeDirectives();
			macros.addAll(FillInFnMacroDnHM(Include));// recursively call fill FillInFnMacroDnHM
		} catch (Exception e) {	
			e.printStackTrace();
			System.err.println("skip "+f.getName()+", can't getMacroDefinitions due to parser's bug ");
			return macros;
		}
		return macros;
}
	
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
    		int k = inputFileName.lastIndexOf("\\");   
        	//System.out.println(fn.format); //DELETE
    		String callee=fn.name,  
    		fmt=fn.format, loc="Location: "+fn.line.getFileLocation(), caller =fn.caller,  	    		
    		filename="Filename: "+inputFileName.substring(k+1), args=""; // chanced according to line 310
    		//filename="Filename: "+inputFileName, args="";
    		/*
    		if (fmt.equals("\"User\"")){
    			System.err.println("target appears!");
    			System.exit(0);
    		}
    		*/
    		
    		for (int i=0; i<fn.variables.size(); i++)
    			//args+="\t["+fn.fmts.get(i)+" "+fn.variables.get(i)+"]";
    			args+="\t"+fn.variables.get(i);
    		String info="\t["+fmt+"]\t"+"vars: "+args+ "\t" +filename+"\t"+loc+"\t"+"caller: "+caller+"\tcallee: "+callee+"\t\r\n";
    		System.err.println(info);
    		add_to_Hashmap(callee,info, subflag);     		
    	}

        /** process the formated string to %x->(.*) */
        
    	static String processString(String format){
    		String newFmt = format; 
    		Matcher mat;
    		count=0;
    		
    		while (format.contains("%")){
    			subflag ++;
    			count++;
    			mat = Pattern.compile("((?s).*?)%([\\d\\.]*l?[dioxXucsfeEgG])((?s).*)")
    			.matcher(format);
    			if (mat.matches()){
    				newFmt += mat.group(1) + "(.*)";
    				format = mat.group(3);
    				fn.fmts.add("(%"+mat.group(2)+")");
    			}
    		}
			return newFmt;
    	} 
    	
		
	/** add the function to Main's hashmap */
	static void add_to_Hashmap(String name, String info, int use_subarg){		
		if (ParserMain.hm.containsKey(name)){
			Function fn=ParserMain.hm.get(name);
			fn.count++;
			fn.info+="\n"+fn.count+". "+info;
			fn.Use_Sub_arg_Number+=use_subarg;
			//System.out.println(info);//delete
			//System.err.println(fn.fnName+" "+fn.Use_Sub_arg_Number+" "+use_subarg);			
			return;
		}
		//System.err.println(name);
		ParserMain.hm.put(name, new Function(name, info, use_subarg));
		//System.out.println(info);//delete
	}
	

	static class CLogPrintVisitor extends ASTVisitor  {
	  
	  public CLogPrintVisitor() {
	    super();
	    this.shouldVisitStatements = true;
	    this.shouldVisitExpressions = true;
	    //parse_visitor.shouldVisitNames = true;
	    this.shouldVisitAmbiguousNodes = true;
	    this.shouldVisitDeclarators = true;
	    this.shouldVisitInitializers = true;
	    this.shouldVisitProblems = true;
	    this.shouldVisitDeclarations = true;
	  }
	  
	  public void formalizeExp (){
	    flag=0;         
	    //if (fn.format!=null&&!fn.format.equals("")){
	    if (!fn.name.equals("")){
	          fn.caller = currentFunc;
	          /*
	          System.err.println(currentFunc);
	          System.exit(0);
	          Matcher mat = Pattern.compile("(.*)\\((?s).*")
	          .matcher(currentFunc); 
	          if (mat.matches()){
	              fn.caller = mat.group(1);
	              System.err.println(fn.caller);
	              System.exit(0);
	          }
	          */
	          if (fn.format.matches("\"(?s).*\"")){// add this line by Bruce Sep 4th, 2009
	              processList();
	          }
	          
	          
	          fn.clear();
	          count=0;
	    }   
	}
	// treat the beginning of statement and declaration as the end of the expression
	public int visit(IASTStatement name) {
	    //System.err.println("stat:\t"+name.getRawSignature());//test
	    formalizeExp();
	    return ASTVisitor.PROCESS_CONTINUE;
	  }      

	  
	public int visit(IASTDeclarator name) {
	    formalizeExp();
	    String val_regex = "(?s).*\\s+(?s).*";
	    String fn_regex = "\\*\\s*\\w+\\s*\\(((\\s*)|"+val_regex+")\\)";
	    /*
	    if (name.getRawSignature().trim().contains("derive_codepage_from_lang")){
	        System.err.println(name.getRawSignature());
	        System.err.println(name.getRawSignature().trim().matches("\\*\\s*"+fn_regex));
	        System.exit(0);
	    }*/
	    if (name.getRawSignature().trim().matches(fn_regex)){
	            currentFunc = name.getRawSignature().trim();
	            Matcher mat = Pattern.compile("(.*?)\\((?s).*")
	            .matcher(currentFunc); 
	            if (mat.matches()){
	                currentFunc = mat.group(1);
	                //System.err.println(currentFunc);
	                //System.exit(0);
	            }
	    }
	    return ASTVisitor.PROCESS_CONTINUE;
	}

	//find the strings from expressions starting with func name and with "strings"
	  public int visit(IASTExpression name) { //FIXME
	      //System.err.println(flag +" "+ name.getRawSignature());//test
	    
	      try {    //the bug lies in this func, it skips everything after the statement                           
	          switch(flag){
	            case 1:                     
	                flag++; fn.name = name.getRawSignature(); //name
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
	                    return ASTVisitor.PROCESS_CONTINUE;
	                }                       
	                fn.variables.add(name.getRawSignature()); //list of variables
	                
	                
	                //System.err.println("variable:"+name.getRawSignature()); //test
	                return ASTVisitor.PROCESS_SKIP;             
	        }
	        
	        //flag=0 check whether the expression satisfies the function (string) requirement
	        String str=name.getRawSignature();
	        IType type=name.getExpressionType();
	        //System.err.println("str is " + str +" // type is " + type);
	        if (str.matches(function_delimiter) && type!=null && type.toString().equals("void")){
	            //System.err.println("str: "+name.getRawSignature());
	            // line                 
	            fn.line = name;
	            flag = 1; // start the process here
	        }       
	        //else System.err.println("str: "+name.getRawSignature());
	        return ASTVisitor.PROCESS_CONTINUE;
	      } catch (Exception e) {
	        //System.err.println("errors: "+e.getMessage()); //FIXME
	        // xuw - fixed by adding ICU lib
	        e.printStackTrace();
	      } 
	      return ASTVisitor.PROCESS_CONTINUE;
	  }
	  
	  
	  public int visit(IASTProblem node) {

		    String problem = node.getRawSignature();
		    
		    //System.err.println(problem);

		    int firstline = problem.indexOf("{");
		    int lastline = problem.lastIndexOf("}");

		    if (firstline < 0 || lastline < 0 || firstline + 1 >= lastline) {
		      return ASTVisitor.PROCESS_CONTINUE;
		    }
		    if (problem.trim().length() == 0) {
		      return ASTVisitor.PROCESS_CONTINUE;
		    }

		    problem = "void main(){" + problem.substring(firstline + 1, lastline) + "}";

		    System.err.println("===================Parsing=================");
		    System.err.println(problem);
		    System.err.println("===========================================");
		    
		    
		    int line = node.getFileLocation().getStartingLineNumber();
		    IASTTranslationUnit translationUnit =
		        FunctionParser.InitializeTranslationUnit(problem, new HashMap<String, String>(), true);
		    translationUnit.accept(new CLogPrintVisitor());

		    return ASTVisitor.PROCESS_CONTINUE;
		  }

	  

	}
	
	
	
	
	
}

/** the data structure to store the console info from log function */
class FunctionInfo{
	IASTExpression line;
	String name="", format, caller;
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


