package ast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;

import utils.DbUtils;
import utils.LogFormatter;

import data.ElementList;
import data.StringExprElement;


/**
 * this visitor can only be invoked on a string expression
 * @author xuw
 *
 */
public class StringExprExpandVisitor extends ASTVisitor {
	
	private static Logger LOG = LogFormatter.getLogger(StringExprExpandVisitor.class);

	IJavaProject javaProject;
	CompilationUnit compilationunit;

	ElementList result;

	private StringExprElement.SCOPE scope =null;
	private String fullName = null;

	public StringExprExpandVisitor(IJavaProject javaproject, CompilationUnit compilationUnit, ElementList result) {
		this.javaProject = javaproject;
		this.compilationunit = compilationUnit;
		this.result = result;
	}

	public boolean visit(InfixExpression node) {
		// should really detect types of each operands
		if (node.getOperator() == Operator.PLUS ) {
			//StringExprExpandVisitor v1 = new StringExprExpandVisitor(javaProject, compilationunit);
			node.getLeftOperand().accept(this);
			node.getRightOperand().accept(this);
			for (Object o: node.extendedOperands() ) {
				ASTNode n = (ASTNode)o;
				n.accept(this);
			}
			return false;
		} else {
			// other arithmatics
			this.result.list.add(new StringExprElement("java.lang.String", StringExprElement.STATUS.ATOMIC,
					node.toString(), node.toString(), StringExprElement.SCOPE.LOCAL_VAR,
					node.getStartPosition(), node.getLength() ) );
			return false;
		}
	}

	public boolean visit(StringLiteral node) {
		if(LOG.isLoggable(Level.FINEST)) {
			LOG.finest("(LITERAL: " + node +")" );
		}
		this.result.list.add(new StringExprElement("java.lang.String", StringExprElement.STATUS.CONSTANT,
				null, node.getLiteralValue(), StringExprElement.SCOPE.LOCAL_VAR,
				node.getStartPosition(), node.getLength() ) );
		return false;
	}

	public boolean visit(NullLiteral node) {
		this.result.list.add(new StringExprElement("java.lang.String", StringExprElement.STATUS.CONSTANT,
				null, "null", StringExprElement.SCOPE.LOCAL_VAR,
				node.getStartPosition(), node.getLength() ) );
		return false;
	}

	public boolean visit(NumberLiteral node) {  // we treat number literals as strings
		if(LOG.isLoggable(Level.FINEST)) {
			LOG.finest("(LITERAL: " + node +")" );
		}
		this.result.list.add(new StringExprElement("java.lang.String", StringExprElement.STATUS.CONSTANT,
				null, node.getToken(), StringExprElement.SCOPE.LOCAL_VAR,
				node.getStartPosition(), node.getLength() ) );
		//int i = 12345;
		return false;
	}


	
	private boolean handleToString(ITypeBinding nodetype, String namestr, int nodeposition, int nodelength) {
		if (nodetype==null) {
			LOG.info("null node type..  ");
			return false;
		}
		StringExprElement.STATUS status = StringExprElement.STATUS.ATOMIC;
		Object value = null;
		String rettype = nodetype.getQualifiedName();
		
		StringExprElement.SCOPE scope = this.scope==null? StringExprElement.SCOPE.LOCAL_VAR:this.scope;



		if (nodetype.isPrimitive()) {
			LOG.finest("PRIMITIVE");
			status = StringExprElement.STATUS.ATOMIC;
		} else if (nodetype.isArray()) {
			nodetype = nodetype.getElementType();
			status = StringExprElement.STATUS.ATOMIC;
		} else if (nodetype.getQualifiedName().toString().equals("java.lang.String")) {
			status = StringExprElement.STATUS.ATOMIC;
		} else {
			LOG.finest("find toString method for type " + nodetype.getName() );
			String typeclass = nodetype.getQualifiedName();
			LOG.finest("found type class " + typeclass);
			try {
				IType type = javaProject.findType(typeclass);
				if (type!=null) {
					
					boolean parse = new ToStringParser(this.javaProject).parseToStringMethod(type);
					
					if (!parse) {
						status = StringExprElement.STATUS.NOTRESOLVED;
					} else {
						status = StringExprElement.STATUS.RSOLVED;
					}
					
					if (type.getFullyQualifiedName().equals("java.lang.Object")) {
						;
					} else {
						ITypeHierarchy hierarchy = type.newTypeHierarchy(null);
						//System.err.println(type.getFullyQualifiedName());
						IType[] subtypes = hierarchy.getSubtypes(type);
						for (IType tp1 : subtypes) {
							// delay parsing
							
							LOG.fine("Subtype::: " + tp1.getFullyQualifiedName() );
							String fullname = tp1.getFullyQualifiedName();
							if (fullname.startsWith("java.")
									|| fullname.startsWith("javax.")
									|| fullname.startsWith("com.sun.corba.")
									|| fullname.startsWith("com.sun.crypto.")
									|| fullname.startsWith("com.sun.java.")
									|| fullname.startsWith("com.sun.jmx.")
									|| fullname.startsWith("com.sun.jndi.")
									|| fullname.startsWith("com.sun.msv.")
									|| fullname
											.startsWith("com.sun.org.apache")
									|| fullname.startsWith("com.sun.xml.")
									|| fullname.startsWith("com.sun.security.")
									|| fullname.startsWith("org.eclipse")
									|| fullname.startsWith("org.apache")) {
								continue; // ignores JDK packages
							}
							
							//LOG.info( tp1.getFullyQualifiedName() +" isInterface?? " + tp1.isInterface());
							if (tp1.isInterface()) {
								//LOG.info("ignore interface toString(): " + tp1.getFullyQualifiedName() );
								continue;
							}
							
							DbUtils.insertToStringSubClass(type.getFullyQualifiedName(), 
									tp1.getFullyQualifiedName().toString());
						}
					}
				} else {
					LOG.info("type is not found " + typeclass);
					status = StringExprElement.STATUS.NOTRESOLVED;
				}
				//if (method!=null) {
				//	String source = method.getSource();
				//	System.err.println(source);
				//}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		


		StringExprElement element = new StringExprElement(rettype, status,
				namestr, value, scope,
				nodeposition, nodelength);

		this.result.list.add( element);

		return false;
	}
	

	public boolean visit(SimpleName node) {
		ITypeBinding nodetype = node.resolveTypeBinding();
		String namestr = this.fullName==null?node.getFullyQualifiedName():this.fullName;
		
		if(LOG.isLoggable(Level.FINEST)){
			LOG.finest("(var: " + node + "-" + nodetype.getName() + ")");
		}
		
		return this.handleToString(nodetype, namestr, node.getStartPosition(), node.getLength());

	}

	public boolean visit(FieldAccess node) {
		
		int ttt =this.result.line;

		if (this.fullName==null) {
			this.fullName = node.toString();
		}
		if (this.scope==null) {
			this.scope = StringExprElement.SCOPE.FIELD;
		}
		// expression like this.datanode.field1
//		if (node.getExpression() instanceof FieldAccess) {
//			if (this.fullName==null) {
//				fullName = node.toString();
//			}
//			node.getExpression().accept(this);
//		} else { // expression like this.datanode
//			node.getName().accept(this);
//		}

		node.getName().accept(this);
		
		this.fullName = null;
		this.scope = null;
		return false;
	}


	public boolean visit(QualifiedName node) {
		if (this.fullName==null) {
			fullName = node.getFullyQualifiedName().toString();
		}
		if (this.scope == null) {
			this.scope = StringExprElement.SCOPE.FIELD_OTHER;
		}
		
		node.getName().accept(this);
		this.fullName = null;
		this.scope = null;
		return false;
	}

	
	public boolean visit(ClassInstanceCreation node) {
		node.getType().accept(this);
		return false;
	}
	
	public boolean visit(MethodInvocation node) {
		if (this.fullName==null) {
			String fullname = "";
			if (node.getExpression()!=null) {
				fullname += node.getExpression() +".";
			}
			fullname += node.getName();
			this.fullName = fullname;
		}
		if (this.scope == null) {
			this.scope = StringExprElement.SCOPE.METHOD;
		}
		
		node.getName().accept(this);
		return false;
		//		IMethodBinding binding = node.resolveMethodBinding();
//		ITypeBinding type = binding.getReturnType();
//
//		this.result.list.add(new StringExprElement(type.getQualifiedName().toString(), StringExprElement.STATUS.ATOMIC,
//				binding.getName().toString(), null, StringExprElement.SCOPE.METHOD,
//				node.getStartPosition(), node.getLength()) );
//		//System.err.println("(method: " + node.getName() +"-" +type.getName() +")" );
//		return false;
	}

	@Override
	public void endVisit(MethodInvocation node) {
		this.fullName= null;
		this.scope =null;
	}

	public boolean visit(ArrayAccess node) {
		node.getArray().accept(this);
		return false;
	}

	public boolean visit(ConditionalExpression node) {
		ITypeBinding type = node.resolveTypeBinding();

		// do not try to further resolve this expression for now
		this.result.list.add(new StringExprElement(type.getQualifiedName().toString(), StringExprElement.STATUS.NOTRESOLVED,
				"(conditional)", null, StringExprElement.SCOPE.METHOD,
				node.getStartPosition(), node.getLength()) );

		return false;
	}

	public boolean visit(ArrayCreation node) {
		
		return false;
	}
	
	public boolean visit(ThisExpression node) {
		ITypeBinding nodetype = node.resolveTypeBinding();
		return this.handleToString(nodetype, "(this-"+nodetype.getName()+")", node.getStartPosition(), node.getLength());
		//return false;
	}

}
