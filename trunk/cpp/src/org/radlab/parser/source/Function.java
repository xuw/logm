package org.radlab.parser.source;
/** a function class stores the important message for Parser */
public class Function {	
	String fnName;
	int Use_Sub_arg_Number;
	int count=1;
	String info; // str_fmt, args, location, filename
	Function (String Name, String info, int Use_Sub_Args){
		fnName=Name;
		this.info="1. "+info;
		Use_Sub_arg_Number=Use_Sub_Args;
	}
}
