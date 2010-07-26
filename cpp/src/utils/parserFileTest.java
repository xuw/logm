package utils;
//import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.cdt.core.dom.ICodeReaderFactory;
import org.eclipse.cdt.core.dom.ast.ASTVisitor;
//import org.eclipse.cdt.core.dom.ast.ExpansionOverlapsBoundaryException;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
//import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
//import org.eclipse.cdt.core.dom.ast.IType;
//import org.eclipse.cdt.core.dom.ast.IType;
//import org.eclipse.cdt.core.dom.ast.gnu.c.GCCLanguage;
//import org.eclipse.cdt.core.dom.ast.gnu.cpp.GPPLanguage;
import org.eclipse.cdt.core.dom.lrparser.c99.C99Language;
import org.eclipse.cdt.core.parser.CodeReader;
import org.eclipse.cdt.core.parser.DefaultLogService;
import org.eclipse.cdt.core.parser.IParserLogService;
import org.eclipse.cdt.core.parser.IScannerInfo;
import org.eclipse.cdt.core.parser.ScannerInfo;



public class parserFileTest {
	static String inputFileName, outputFileName;
	static String contents;
	static Scanner input;
	/** function delimiter*/
	static String function_delimiter="\\w+(?s).+\\(.*?\"(?s).*\"(?s).*\\)(?s).*";
	//static LinkedList<IASTExpression> list=new LinkedList<IASTExpression>();
	
	static FunctionInfo fn=new FunctionInfo();
	static int count;
	
	/** parameter flag*/
	static int flag=0; 
    
	/** ASTVisitor parse_vistor which analyzes the pattern of IASTExpression and IASTStatement*/
	static ASTVisitor parse_visitor = new ASTVisitor() {  
    	public int visit(IASTStatement name) {
        	  //System.out.println("statement:" + name.getRawSignature());
        	  flag=0;
        	  
        	  if (!fn.name.equals("")){
        		  //processList();
        		  fn.clear();
        		  count=0;
        	  }
        	  
              return ASTVisitor.PROCESS_CONTINUE;
          }          
    	
          public int visit(IASTExpression name) { //FIXME
        	  //System.out.println("expr:"+name.getRawSignature()); 
        	  try {        		        		  
        		
        		  /*
        		  switch(flag){
        			case 1:        				
        				flag++; fn.name=name.getRawSignature(); //name
        				System.err.println("name:"+name.getRawSignature()); //test
        				return ASTVisitor.PROCESS_CONTINUE;
        			case 2:  
        				if (!name.getRawSignature().matches("\"(?s).*\""))//format        				
        					return ASTVisitor.PROCESS_CONTINUE;
        				fn.format=name.getRawSignature();
        				flag++; 

    					System.err.println("fmt:"+name.getRawSignature()); //test
    					return ASTVisitor.PROCESS_CONTINUE;
        			case 3: 
        				fn.variables.add(name.getRawSignature()); //list of variables
        				System.err.println("variable:"+name.getRawSignature()); //test
        				return ASTVisitor.PROCESS_SKIP;			
        		}        		
        		  
        		//flag=0
        		String str=name.getRawSignature();
        		IType type=name.getExpressionType();
        		if (str.matches(function_delimiter)&&type.toString().equals("void")){
        			System.err.println("str: "+name.getRawSignature());
        			// line
        			
        			fn.line=name;
        			System.err.println("list:"+name.getRawSignature());
        			flag=1;
        			*/
        		  
        		  System.err.println("expr: "+name.getRawSignature());
        							
        	
			} catch (Throwable e) {
				System.err.println("errors: "+e.getMessage());
			}		
              return ASTVisitor.PROCESS_CONTINUE;
          }                                 
          };
	
    /** the funciton which output the useful information of the log from the stored list */
          static void processList(){
      		//format: function name, string format in (.*), args, location, filename 
      		fn.format=processString(fn.format);
      		String fn_name="name:"+fn.name, line=fn.line.getRawSignature(), 
      		fmt="Format:"+fn.format, loc="Location:"+fn.line.getFileLocation(), 
      		filename="Filename:"+fn.line.getFileLocation().getFileName(), args="vars:";
      		
      		for (int i=0; i<count; i++)
      			args+=fn.variables.get(i)+", ";
      		String info=line+"\t"+fmt+"\t"+args+"\t"+loc+"\t"+filename;
      		System.out.println(fn_name+"\t"+info);
      		//add_to_Hashmap(fn_name,info, subflag);
      	}
          
          /** process the formated string to %x->(.*) */
      	static String processString(String format){
      		String newFmt=format, fmtWord; 
      		Matcher mat;
      		Scanner fmtInp = new Scanner (format);	
      		if (format.contains("%")){	    				    	
      			fmtInp.useDelimiter("%");
      			newFmt=fmtInp.next();
      			while (fmtInp.hasNext()){	   
      				count++;
      				fmtWord=fmtInp.next();
      				mat = Pattern.compile("([\\d\\.]*l?[dioxXucsfeEgG])(.*)")
      				.matcher(fmtWord);
      				if (mat.matches()){
      					newFmt+="(.*)"+mat.group(2);
      					continue;
      				}
      				newFmt+=fmtWord;
      			}
      		}
			return newFmt;
      	}
	
	public static void main(String[] args) throws Exception {
		streamInitialization (args);
		
		input.useDelimiter("\\Z");
		String code =input.next();
        IParserLogService log = new DefaultLogService();   
        CodeReader reader = new CodeReader(code.toCharArray());
        
        Map<String,String> definedSymbols = new HashMap<String, String>();
        String[] includePaths = new String[0];
        IScannerInfo info = new ScannerInfo(definedSymbols,includePaths);
        ICodeReaderFactory readerFactory = FileCodeReaderFactory.getInstance();

        IASTTranslationUnit translationUnit = C99Language.getDefault().getASTTranslationUnit(reader, info, readerFactory, null, log);        
                
              
        parse_visitor.shouldVisitStatements = true;
        parse_visitor.shouldVisitExpressions = true;
        //test
        parse_visitor.shouldVisitBaseSpecifiers=true;
        parse_visitor.shouldVisitDeclarations=true;
        parse_visitor.shouldVisitDeclarators=true;
        parse_visitor.shouldVisitDeclSpecifiers=true;
        parse_visitor.shouldVisitImplicitNames=true;
        
        try{
        translationUnit.accept(parse_visitor);
        if (!fn.name.isEmpty())
        	processList();
        }catch(Throwable e){
        	System.err.println(e.getMessage());
        	
        }
  }
	
	/** Stream Initialization */
	static void streamInitialization(String[] args){
		inputFileName = outputFileName = null;
		switch (args.length) {
		case 0: break;
		case 1: inputFileName = args[0]; break;
		case 2: inputFileName = args[0]; 
				outputFileName = args[1]; 
				break;
		default: System.err.println("Too many arguments"); 
				 System.exit(1);
		}
		if (inputFileName!=null)
			try {				
				input = new Scanner (new FileReader (inputFileName));
			} catch (FileNotFoundException e) {
				System.err.printf("Error: could not open %s%n", inputFileName);
			}else input =
				new Scanner (new InputStreamReader (System.in));
		
		if (outputFileName!=null)
			try {
				System.setOut(new PrintStream(outputFileName));
			} catch (FileNotFoundException e) {
				System.err.printf("Error: could not creat %s%n", outputFileName);
			} else System.setOut(System.out);
	}						
}

class FunctionInfo{
	IASTExpression line;
	String name="", format;
	LinkedList<String> variables=new LinkedList<String>(); 
	void clear(){
		name="";
		line=null;
		format="";
		variables.clear();
	}
}

