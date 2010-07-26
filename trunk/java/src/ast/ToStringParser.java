package ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.core.BindingKey;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import data.ElementList;

import utils.DbUtils;
import utils.LogFormatter;

public class ToStringParser {
	
	private static Logger LOG = LogFormatter.getLogger(ToStringParser.class);
	
	public static final int UNPARSED =0;
	public static final int PARSED_SUCCESSFUL =1;
	public static final int NO_TOSTRING =2;
	public static final int PARSE_FAILED =3;
	
	IJavaProject javaProject;
	
	public ToStringParser(IJavaProject javaProject) {
		this.javaProject= javaProject;
	}
	
	private String getToStringMethodSrcInClass(IType type) {
		return getToStringMethodSrcInClass(type, false);
	}

	private String getToStringMethodSrcInClass(IType type, boolean print) {
//		 Can we find toString method at the class itself??
		try{
			IMethod t = type.getMethod("toString", new String[0]);
			//System.err.println(t);
			t.getOpenable().open(null);
			String src = t.getSource();
			if (print) {
				LOG.fine(">>>>>>"+ type.getFullyQualifiedName() +">>>>>>");
				LOG.fine(src);
				LOG.fine("<<<<<<"+ type.getFullyQualifiedName() +"<<<<<<");
			}
			return src;
		} catch (Exception ex) {
			LOG.info("no toString() method defined in "+type.getFullyQualifiedName());
			return null;
		}
	}
	
	private ElementList parse(IType tp) {
		//System.err.println("Parsing toString method in "+tp.getFullyQualifiedName());
		ICompilationUnit cpunit =  tp.getCompilationUnit();
		if (cpunit!=null) {
			ASTParser parser = ASTParser.newParser(AST.JLS3);
			parser.setKind(ASTParser.K_COMPILATION_UNIT);
			parser.setResolveBindings(true);
			parser.setSource(cpunit);
			if(LOG.isLoggable(Level.FINE)) {
				LOG.fine("Creating AST for type " + tp.getFullyQualifiedName());
			}
			CompilationUnit ast = (CompilationUnit) parser.createAST(null);
			
			String bkey = BindingKey.createTypeBindingKey(tp.getFullyQualifiedName());
			ASTNode realroot = ast.findDeclaringNode(bkey);
			
			ToStringMethodVisitor visitor = new ToStringMethodVisitor(this.javaProject,
					ast, tp.getFullyQualifiedName());
			realroot.accept(visitor);
			return visitor.result;
		} else {
			LOG.warning("===> CANNOT FIND COMPILATION UNIT for " + tp.getFullyQualifiedName());
			return null;
		}
	}
	
	
	// used to avoid infinite recursive parsing..
	private static HashMap<String , String> beingparsed = new HashMap<String, String>();
	
	private ElementList parseOne(IType type) {
		String src = getToStringMethodSrcInClass(type);
		ElementList substr =null;
		String typename = type.getFullyQualifiedName();
		LOG.fine( "Looking for toString Method in class: " +typename);
		if (src!=null && DbUtils.findToStringForClass(typename.toString())==null ) {
			LOG.fine( "toString method found, parsing:: " +typename);
			beingparsed.put(typename.toString(), "");
			substr = parse(type);
			beingparsed.remove(typename.toString());
		} else {
			if (src ==null) {
				LOG.fine("cannot find toString method in: " +typename);
				return null;
			} else {
				LOG.fine("already parsed (skipping): " + typename );
				substr = new ElementList();
				substr.status = ElementList.DUPLICATE;
				return substr;
			}
		}
		
		return substr;
	}
	
	/**
	 * parses toString() method in type or in its closest super class, store into db
	 * @param type
	 * @return true iff parsed successfully
	 */
	public boolean parseToStringMethod(IType type) {
			//if (DbUtils.findToStringForClass(type.getFullyQualifiedName().toString())!=null){
			//	LOG.fine("already parsed, skipping " + type.getFullyQualifiedName());
			//	return true;
			//}
		try {
			if (type==null) {
				LOG.fine("type is null");
				return false;
			}
			if (type.getFullyQualifiedName().equals("java.lang.Object")){
				// avoid super expensive hierarchy calculation of Object
				return false;
			}
			ITypeHierarchy hierarchy = type.newTypeHierarchy(null);
			IType[] supertypes = hierarchy.getSupertypes(type);
			
			String src = null;
			IType tp = type;
			ArrayList<IType> superlist = new ArrayList<IType>();
			
			// find self / the closet super class with toString defined..
			while (true) {
				superlist.add(tp);
				src = getToStringMethodSrcInClass(tp, false);
				if (src != null) {
					break;  // found
				}
				tp = hierarchy.getSuperclass(tp);
				if (tp == null) {
					break; // arrive at top of hierarchy
				}
			}
			
			if (tp==null) {
				// no super class has a visible toStringMethod
				for (IType t: superlist) {
					DbUtils.insertToStringRec(t.getFullyQualifiedName(), new ElementList(), PARSE_FAILED);
				}
				return false;
			}
			ElementList ret = parseOne(tp);
			if (ret == null) {
				for (IType t: superlist) {
					DbUtils.insertToStringRec(t.getFullyQualifiedName(), new ElementList(), PARSE_FAILED);
				}
				return false;
			} else if (ret.status!=ElementList.DUPLICATE){
				for (IType t: superlist) {
					DbUtils.insertToStringRec(t.getFullyQualifiedName(), ret, PARSED_SUCCESSFUL);
				}
				return true;
			} else {
				return true;
			}
		} catch (JavaModelException e) {
			e.printStackTrace();
			return false;
		}
	}
	
}
