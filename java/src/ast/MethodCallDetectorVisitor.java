package ast;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.WhileStatement;

import data.MethodDesc;
import data.MethodEdge;

import utils.DbUtils;
import utils.LogFormatter;


public class MethodCallDetectorVisitor extends ASTVisitor {
	
	private static Logger LOG = LogFormatter.getLogger(MethodCallDetectorVisitor.class);

	IJavaProject javaproject;

	//Set<MethodEdge> invokemap;
	
	String compilationUnitName;
	CompilationUnit compilationunit;
	

	public MethodCallDetectorVisitor(IJavaProject javaproj, CompilationUnit compilationunit, String compliationUnitName) {
		this.javaproject = javaproj;
		//this.invokemap = new TreeSet<MethodEdge>();
		this.compilationunit = compilationunit;
		this.compilationUnitName = compliationUnitName;
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
	
	
	public boolean visit(ClassInstanceCreation node) {
		if (LOG.isLoggable(Level.FINER)) {
			LOG.finer("CONSTRUCTOR CALL: " + node);
		}
		IMethodBinding mb = node.resolveConstructorBinding();
		return processMethod(node, mb);
	}
	
	public boolean visit(MethodInvocation node) {
		
		if (LOG.isLoggable(Level.FINER)) {
			//LOG.fine(node.toString());
			//LOG.fine( node.getExpression() ) ;
			//LOG.fine(node.resolveTypeBinding());
			LOG.finer("CALL: " + node);
		}
		
		IMethodBinding mb = node.resolveMethodBinding();
		
		return processMethod(node, mb);

	}
	
	private boolean processMethod(ASTNode node, IMethodBinding mb) {
		if (mb==null) {
			LOG.warning("cannot resolve binding " + node );
			return true;
		}
		ITypeBinding returntype = mb.getReturnType();

		String typestring = Utils.safeResolveType(javaproject, returntype);
		LOG.finer("TYPE: " +typestring);
		
		MethodDesc callee = null;
		
		int edgeid = -2;
		
		try {
			MethodDeclaration invoker = (MethodDeclaration) findInvoker(node);
			if (invoker == null) {
				// static invokation in the class
			} else {
				IBinding invokerbinding = invoker.getName().resolveBinding();
				TypeDeclaration invokerclass =null;
				ASTNode classdef = findClassDef(invoker);
				try {
					invokerclass = (TypeDeclaration) classdef;
				} catch (ClassCastException e) {
					// invoker is in a anonymous class definition
					LOG.finer("[WARNING] " + classdef +" is not of type TypeDeclaration"  );
					return true;
				}
				String invokerclassstr = invokerclass.resolveBinding().getTypeDeclaration().getQualifiedName();
				
				//String callerreturntype = Utils.safeResolveType(javaproject, invoker.resolveBinding().getReturnType());
				IMethodBinding methodbinding = invoker.resolveBinding();
				String callerreturntype = null;
				if (methodbinding!=null) {
					callerreturntype = Utils.safeResolveType(javaproject, methodbinding.getReturnType());
				} else {
					LOG.warning("cannot find method binding - " + invoker.getName());
				}
				int line = this.compilationunit.getLineNumber(node.getStartPosition());
				
				MethodDesc caller = new MethodDesc(invokerclassstr,
						invoker.getName().toString(),
						"",
						callerreturntype);
				callee = new MethodDesc(mb.getDeclaringClass().getTypeDeclaration().getQualifiedName(), 
						mb.getName(),
						"",
						typestring);
				if (LOG.isLoggable(Level.FINER)) {
					LOG.finer("METHOD IN LOOP = " + inloop);
				}
				MethodEdge edge = new MethodEdge(caller, callee, line, inloop>0);
				
				edgeid = edge.writeToDB();
				
//				if (invokemap.contains(edge)) {
//					;
//				} else {
//					invokemap.add(edge);
//				}
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}

		Statement codeblock = (Statement) Utils.findBlockStatement(node);
		if (codeblock instanceof Block) {
			Block blk = (Block) codeblock;
			for (Object st : blk.statements()) {
				if (LOG.isLoggable(Level.FINER)){
					LOG.fine("STATMENT:: " + st + st.getClass());
				}
				if (st instanceof ExpressionStatement) {
					Expression expr = ((ExpressionStatement) st)
							.getExpression();
					if (expr instanceof MethodInvocation) {
						MethodInvocation logcall = (MethodInvocation) expr;
						IMethodBinding logmb = logcall.resolveMethodBinding();
						if (logmb == null) {
							LOG.info("cannot resolve binding " + logcall);
							continue;
						}
						if (logmb.getDeclaringClass().getName().toString().equals(
								"Logger")) {
							int logstart = logcall.getStartPosition();
							int logline = compilationunit.getLineNumber(logstart);
							if (logstart <= node
									.getStartPosition()) {
								if (LOG.isLoggable(Level.FINER)) LOG.finer("PRE::" + logcall +" Line:" + logline);
								DbUtils.insertPreLog(compilationUnitName+"-"+logline,
										edgeid);
							} else {
								LOG.finer("POST::" + logcall +" Line:" + logline);
								if (LOG.isLoggable(Level.FINER)) DbUtils.insertPostLog(compilationUnitName+"-"+logline,
										edgeid);
							}
						}
					}
				}
			}
		}
		
		if (LOG.isLoggable(Level.FINER))
			LOG.fine( "RESOLVED:" + mb +" RET-TYPE:" +returntype.getQualifiedName());
		return true;		
	}
	

	public ASTNode findInvoker(ASTNode node){
		ASTNode p = node.getParent();
		if (p ==null || p instanceof MethodDeclaration)
			return p;
		else {
			//LOG.finer( p.getClass() );
			return findInvoker(p);
		}
	}

	public ASTNode findClassDef(ASTNode node) {
		ASTNode p = node.getParent();
		if (p ==null || p instanceof TypeDeclaration || p instanceof AnonymousClassDeclaration )
			return p;
		else {
			//LOG.finer( p.getClass() );
			return findInvoker(p);
		}
	}


}
