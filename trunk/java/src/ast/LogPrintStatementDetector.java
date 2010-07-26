package ast;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;

import conf.ConfUtil;

import utils.DbUtils;
import utils.LogFormatter;

import data.ElementList;
import data.StringExprElement;


public class LogPrintStatementDetector extends ASTVisitor{

	
	public static final int LOGTYPE_LOG4J =0;
	public static final int LOGTYPE_JDKLOG =1;
	public static final int LOGTYPE_METHODNAME =2;
	public static final int LOGTYPE_UNKNOWN =3;
	
	private static Logger LOG = LogFormatter.getLogger(LogPrintStatementDetector.class);
	
	IJavaProject javaproject;
	CompilationUnit compilationunit;
	String compilationUnitName;
	String currentClass=compilationUnitName;
	String currentMethod="static";
	
	private static String[] loggerClassNames;
	
	private static int logtype;
	
	static {
		loggerClassNames = ConfUtil.getConfig().getStringArray("parsing.loggerClassName");
	}
	
	public LogPrintStatementDetector(IJavaProject javaproj, CompilationUnit compilationunit, 
			String compilationUnitName, String currentClass, String currentMethod) {
		this.javaproject = javaproj;
		this.compilationunit = compilationunit;
		this.compilationUnitName = compilationUnitName;
		this.currentClass = currentClass;
		this.currentMethod = currentMethod;
		String logtypestr = ConfUtil.getConfig().getString("parsing.loggerFramework");
		if (logtypestr.equals("log4j")) {
			logtype = LOGTYPE_LOG4J;
		} else if (logtypestr.equals("jdk")) {
			logtype = LOGTYPE_JDKLOG;
		} else {
			throw new RuntimeException("Unknown log type!!");
		}
	}
	
	public boolean visit(MethodDeclaration node) {
		IMethodBinding mb = node.resolveBinding();
		if (mb==null) {
			LOG.warning("failed resolving binding for method declaration "+node.getName());
			return true;
		}
		String classname = mb.getDeclaringClass().getQualifiedName();
		if (classname.length()==0) {
			// annonymous class
			LOG.info("hit annonymous class definition.. skiping " + node.getName());
			return true;
		}
		currentClass = classname;
		currentMethod = mb.getName();
		LOG.finer("in method " + currentClass +"." + currentMethod);
		return true;
	}
	
	int inloop = 0;
	
	public boolean visit(ForStatement node) {
		inloop+=1;
		return true;
	}
	
	public boolean visit(WhileStatement node) {
		inloop+=1;
		return true;
	}
	
	public boolean visit(DoStatement node) {
		inloop+=1;
		return true;
	}
	
	public void endVisit(ForStatement node) {
		inloop-=1;
	}
	
	public void endVisit(WhileStatement node) {
		inloop-=1;
	}
	
	public void endVisit(DoStatement node) {
		inloop-=1;
	}
	
	private boolean isLoggerClass(String classname) {
		for (String loggername: loggerClassNames) {
			if (classname.equals(loggername)){
				return true;
			}
		}
		return false;
	}
	
	public boolean visit(MethodInvocation node) {

		// System.err.println (node.getName());

		IMethodBinding mb = node.resolveMethodBinding();
		if (mb == null) {
			LOG.info("cannot resolve binding " + node);
			return true;
		}

		ITypeBinding returntype = mb.getReturnType();
		// System.err.println(mb.getDeclaringClass().getName() +"."
		// +mb.getName());
		
		//System.err.println( mb.getDeclaringClass().getName() );
		//LOG.info("Method Call: " + mb.getDeclaringClass().getName() + " " +node.getName());
		if ( isLoggerClass(mb.getDeclaringClass().getName().toString())) {
			//LOG.info("Logger call: " + mb.getDeclaringClass().getName());
			List<ASTNode> args = node.arguments();
			ASTNode arg;
			String level = null; // log debug level
			String loggerMethodName = node.getName().toString();
			if (loggerMethodName.startsWith("is") 
					|| loggerMethodName.startsWith("set") 
					|| loggerMethodName.startsWith("get")) {
				return true;
			} else if (args.size() < 1) {
				LOG.warning("Log print statement has no arguments ::: ");
				LOG.warning("ERRPR LOG::"
						+ node
						+ " Line:"
						+ compilationunit
								.getLineNumber(node.getStartPosition())
						+ " in " + compilationUnitName + " LEVEL:"
						+ node.getName());
				return true;
			} else if (args.size() == 1) {
				if (node.getName().toString().equals("getLogger")) {
					return true;
				}
				level = node.getName().toString();
				arg = args.get(0);
			} else {
				
				if (logtype==LOGTYPE_LOG4J)  {
					level = node.getName().toString();
					arg = args.get(0);
					if (! arg.toString().startsWith("\"")) {
						LOG.info("skipping method " + node.getName() + "(" +arg.toString() +")");
					}
				} else if (logtype == LOGTYPE_JDKLOG){
					
					// first see if it is logThrow method
					int formatStrInd = 1;
					//LOG.info("method name=" + node.getName());
					if(node.getName().toString().equals("logThrow")) {
						LOG.finer("logThrow methods");
						formatStrInd = 2;
						//return true;
					}
					
					
					String levelexpr = args.get(0).toString();
					level = levelexpr.substring(levelexpr.lastIndexOf(".")+1 );
					
					// visit each format string arguments
					int line = this.compilationunit.getLineNumber(node
							.getStartPosition());
					arg = args.get(formatStrInd);
					
					ElementList resultlist = new ElementList(
							this.compilationUnitName, line);
					StringExprExpandVisitor visitor = new StringExprExpandVisitor(
							this.javaproject, this.compilationunit, resultlist);
					arg.accept(visitor);
					
					String regexpr = visitor.result.toRegExpr(false);
					String namemap = visitor.result.getNameMapString();
					String typemap = visitor.result.getTypeMapString();
					String constStr = visitor.result.getConstantString();
					
					if (LOG.isLoggable(Level.FINER)) {
					//LOG.finer( "result: " + visitor.result.toString() );
						LOG.finer("line " + line);
						LOG.finer("regexpr: " + regexpr);
						LOG.finer("namemap: " + namemap);
						LOG.finer("typemap: " + typemap);
						LOG.finer("constString: " + constStr);
					}
					
					if(args.size()>formatStrInd+1) {
						// do string expand on each arguments
						// first save the result name/type map, and regexpr
						
						LOG.info(args.get(formatStrInd+1).getClass().toString());
						if (args.get(formatStrInd+1) instanceof ArrayCreation){
							ArrayCreation tc = (ArrayCreation) args.get(formatStrInd+1);
							ArrayInitializer t = tc.getInitializer();
							for (int i=0; i<t.expressions().size(); i++ ) {
								Expression e = (Expression) t.expressions().get(i);
								arg = e;
								resultlist = new ElementList(
										this.compilationUnitName, line);
								visitor = new StringExprExpandVisitor(
										this.javaproject, this.compilationunit, resultlist);
								arg.accept(visitor);
								LOG.finer("VISITING ARG in Array " + i);
								//LOG.info( "result: " + visitor.result.toString() );
								String r = visitor.result.toRegExpr(false);
								regexpr = regexpr.replaceFirst("\\\\\\{"+ i +"[^\\}]*" +"\\\\\\}", r);
								r = visitor.result.getConstantString();
								constStr = constStr.replaceFirst("\\{"+ i +"[^\\}]*" +"\\}", r);
								namemap += visitor.result.getNameMapString();
								typemap += visitor.result.getTypeMapString();
								
								if (LOG.isLoggable(Level.FINER)) {
									LOG.finer("regexpr: " + regexpr);
									LOG.finer("namemap: " + namemap);
									LOG.finer("typemap: " + typemap);
									LOG.finer("constStr: " + constStr);
								}
							}
							
						} else {
							// List l = visitor.result.list;
							for (int i = formatStrInd + 1; i < args.size(); i++) {
								arg = args.get(i);
								resultlist = new ElementList(
										this.compilationUnitName, line);
								visitor = new StringExprExpandVisitor(
										this.javaproject, this.compilationunit,
										resultlist);
								arg.accept(visitor);
								LOG.finer("VISITING ARG" + i);
								// LOG.info( "result: " +
								// visitor.result.toString() );
								String r = visitor.result.toRegExpr(false);
								regexpr = regexpr
										.replaceFirst("\\\\\\{"
												+ (i - 1 - formatStrInd) +"[^\\}]*"
												+ "\\\\\\}", r);
								r = visitor.result.getConstantString();
								constStr = constStr.replaceFirst("\\{"
										+ (i - 1 - formatStrInd) +"[^\\}]*"+ "\\}", r);
								namemap += visitor.result.getNameMapString();
								typemap += visitor.result.getTypeMapString();

								if (LOG.isLoggable(Level.FINER)) {
									LOG.finer("regexpr: " + regexpr);
									LOG.finer("namemap: " + namemap);
									LOG.finer("typemap: " + typemap);
									LOG.finer("constStr: " + constStr);
								}
							}
						}
						
						regexpr=regexpr.replaceAll("\\\\\\{[0-9]+\\\\\\}", ".*");
						constStr=constStr.replaceAll("\\{[0-9]+\\}", "");
					}
					
					// insert into DB
					String logid = this.compilationUnitName + "-" + line;
					int methodid = DbUtils.findMethodId(this.currentClass,
							this.currentMethod, "");
					DbUtils.insertLogEntry(logid, regexpr, namemap,
							typemap, constStr, 
							Utils.getLevel(level), inloop>0, methodid);
					LOG.info("LOG print regexpr: " + regexpr);
					return true;
				} else {
					throw new RuntimeException("Logtype not supported!!");
				}
			}
			
			processOneLoggerCall(node, arg, level);

		} else {
			if (node.getName().toString().equals("message") ) {
				String level = "info";
				List<ASTNode> args = node.arguments();
				ASTNode arg = args.get(0);
				processOneLoggerCall(node, arg, level);
			}
			
		}

		// String typestring = Utils.safeResolveType(javaproject, returntype);
		//		System.err.println(typestring);

		return true;
	}
	
	private void processOneLoggerCall(MethodInvocation node, ASTNode arg, String level) {
		
		LOG.info("LOG print str: " + arg);
		if (arg instanceof Expression) {
			int line = this.compilationunit.getLineNumber(node
					.getStartPosition());
			ElementList resultlist = new ElementList(
					this.compilationUnitName, line);
			StringExprExpandVisitor visitor = new StringExprExpandVisitor(
					this.javaproject, this.compilationunit, resultlist);
			arg.accept(visitor);
			
			if (LOG.isLoggable(Level.FINER)) {
				LOG.finer( "result: " + visitor.result.toString() );
				LOG.finer("regexpr: " + visitor.result.toRegExpr(true));
				LOG.finer("namemap: " + visitor.result.getNameMapString());
				LOG.finer("typemap: " + visitor.result.getTypeMapString());
			}
			Statement codeblock = (Statement) Utils.findBlockStatement(node);
			//int blockline = compilationunit.getLineNumber(codeblock
			//		.getStartPosition());
			int methodid = DbUtils.findMethodId(this.currentClass,
					this.currentMethod, "");
			if (LOG.isLoggable(Level.FINER)) {
				//LOG.finer("BLOCK_LINE:" + blockline);
				LOG.finer("LOG IN LOOP: " + inloop);
				LOG.finer("METHOD: " + this.currentMethod + " "+ methodid);
			}
			if (methodid == -1) {
				LOG.warning("FAILED TO FIND METHOD:: "
						+ this.currentClass + " " + this.currentMethod);
			}
			DbUtils.insertLogEntry(this.compilationUnitName + "-" + line,
					visitor.result, Utils.getLevel(level), inloop > 0,
					methodid);

		} else {
			LOG.warning("NODE is of type " + arg.getClass());
		}
		
	}

}
