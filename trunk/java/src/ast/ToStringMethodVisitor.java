package ast;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.logging.Logger;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import utils.LogFormatter;

import data.ElementList;
import data.StringExprElement;

public class ToStringMethodVisitor extends ASTVisitor {
	
	private static Logger LOG = LogFormatter.getLogger(ToStringMethodVisitor.class);
	
	IJavaProject javaproject;
	CompilationUnit compilationunit;
	String compilationUnitName;
	ElementList result;

	public ToStringMethodVisitor(IJavaProject javaproj, CompilationUnit compilationunit,
			String compilationUnitName) {
		this.javaproject = javaproj;
		this.compilationunit = compilationunit;
		this.compilationUnitName = compilationUnitName;
	}

	public boolean visit(MethodDeclaration node) {
		//System.err.println (node.getName());
		if (node.getName().toString().equals("toString")) {
			node.getBody().accept(this);
		}

		return false;
	}
	
	public boolean visit(TypeDeclaration node ) {
		for (MethodDeclaration method: node.getMethods()) {
			method.accept(this);
		}
		return false;
	}

	public boolean visit(Block node) {
		//System.err.println("visiting toString... \n" + node);
		for (Object o: node.statements()) {
			Statement s = (Statement) o;
			if (s instanceof ReturnStatement) {
				ElementList resultlist = new ElementList(this.compilationUnitName,
						this.compilationunit.getLineNumber(node.getStartPosition()));
				StringExprExpandVisitor visitor =
					new StringExprExpandVisitor(this.javaproject,
							this.compilationunit, resultlist);
				( (ReturnStatement)s ).getExpression().accept(visitor);
				if (this.result!=null) {
					// more complex than a single return statement
					// giving up
					this.result = null;
				}
				this.result = visitor.result;
			}
		}
		return false;
	}

}
