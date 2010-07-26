package ast;

import index.IndexConstants;

import java.io.CharArrayReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.CompilationUnit;

import conf.ConfUtil;

import utils.DbUtils;
import utils.LogFormatter;


public class ASTTest implements IApplication{
	
	private static Logger LOG = LogFormatter.getLogger("MainProgram");
	static {
		if(IndexConstants.debug) {
			LogFormatter.setDebugLevel(Level.FINE, "_all");
		}
	}
	
	static File outputDir;
	
	public Object start(IApplicationContext context) throws Exception {
		
		outputDir = new File(System.getProperty("java.io.tmpdir"));
		ASTTest.main(null);
		return null;
	}
	
	public void stop() {
		
	}


	public static boolean testmethod(int i) {
		return true;
	}
	
	public static void processSourceFile(IWorkspaceRoot root, IJavaProject javaproject, String typename) throws Exception{
		
		LOG.info("PROCESSING CLASS FILE: " + typename);
		IType tp = javaproject.findType(typename);
		ICompilationUnit cpunit = tp.getCompilationUnit();

		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setResolveBindings(true);

		parser.setSource(cpunit);


		//FileReader reader = new FileReader("/Users/xuw/workspace/omap/src/java/outfox/omap/OmapTs.java");
//		char[] srcfile = new char[1024*1024];
//		
//		System.err.println(root.getLocation().toFile());
//		System.err.println(cpunit.getPath().toFile().toString());
//		
//		File srcFile = new File(root.getLocation().toFile(), cpunit.getPath().toFile().toString());
//		FileReader reader = new FileReader(srcFile);
//		reader.read(srcfile);
//
//		System.err.println("parsing source file....");
//		parser.setSource(srcfile);

		//System.err.println(parser.createAST(null));
		CompilationUnit ast = (CompilationUnit) parser.createAST(null);

		//System.err.println(ast);

		//System.err.println( Arrays.deepToString( ast.getCommentList().toArray() ) );


//		Reader freader = new CharArrayReader(srcfile);
//		freader.mark(0);
//
//		List<Comment> comments = ast.getCommentList();
//		for (Comment cmt: comments) {
//			System.err.println(cmt + " " +cmt.getStartPosition() +"--"+cmt.getLength());
//			char[] line = new char[ct.getLength()];
//			freader.reset();
//			freader.skip(cmt.getStartPosition());
//			freader.read(line);
//			System.err.println(new String(line));
//		}
//		freader.close();

		//System.err.println( ast.findDeclaringNode("keyfind") );
		
		
		MethodCallDetectorVisitor visitor = 
			new MethodCallDetectorVisitor(javaproject, ast,tp.getFullyQualifiedName());
		ast.accept(visitor);

		//System.err.println( Arrays.deepToString( visitor.invokemap.toArray() ) );
		
		
//		for (MethodEdge s : visitor.invokemap) {
//			System.err.println(s);
//		}

//		ObjectOutputStream mapout = new ObjectOutputStream(
//				new FileOutputStream( new File(outputDir, "methodmap.bin") ));
//		mapout.writeObject(visitor.invokemap);
//		mapout.close();

		LogPrintStatementDetector logvisitor = 
			new LogPrintStatementDetector(javaproject, ast, tp.getFullyQualifiedName(),
				tp.getFullyQualifiedName(),"static");
		ast.accept(logvisitor);

	}

	public static void main(String[] args) throws Exception{
		
		long startTime = System.currentTimeMillis();
		
		DbUtils.getConn();
		// add the top of hierachy
		DbUtils.insertToStringRec("java.lang.Object", "@#@", "objaddr", 
				"java.lang.String", ToStringParser.PARSED_SUCCESSFUL);
		
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		
		String projName = ConfUtil.getConfig().getString("parsing.projName");
		IProject project = root.getProject(projName);
		
		
		project.open(null);

		IJavaProject javaproject = JavaCore.create(project);
		
//		processSourceFile(root, javaproject,"outfox.omap.OmapTs");
//		processSourceFile(root, javaproject,"outfox.omap.client.OmapClient");
//		processSourceFile(root, javaproject,"outfox.omap.TabletDesc");
//		processSourceFile(root, javaproject,"outfox.omap.MasterCatalogue");
//		processSourceFile(root, javaproject,"outfox.omap.OmapMaster");
//		processSourceFile(root, javaproject,"outfox.omap.ts.WALogger");
//		processSourceFile(root, javaproject,"outfox.omap.ts.buffer.FileBackedArrayMap");
		
//		processSourceFile(root, javaproject,"com.sun.sgs.impl.service.transaction.TransactionImpl");
		
//		processSourceFile(root, javaproject,"com.sun.sgs.test.impl.kernel.LogTest");
		
//		processSourceFile(root, javaproject, "com.facebook.infrastructure.service.StorageService");
		
		for(IPackageFragment pkgfrag: javaproject.getPackageFragments()) {
			LOG.info("opening package " +pkgfrag.getElementName());
			for (ICompilationUnit cu : pkgfrag.getCompilationUnits() ) {
				String typename = pkgfrag.getElementName()
					+"."+cu.getElementName().replaceAll("\\.java", "");
				//LOG.info("\t" +typename);
				processSourceFile(root, javaproject, typename);
			}
		}
		
		
//		ResultSet rs = DbUtils.getAllEdges();
//		try {
//			while (rs.next()) {
//				System.err.println(DbUtils.readIntoMethodEdge(rs));
//			}
//			rs.close();
//		} catch (SQLException e) {
//			e.printStackTrace();
//		}
		
//		DbUtils.dumpTable("premethodlog");
//		DbUtils.dumpTable("methoddesc");
//		DbUtils.dumpTable("tostringdb");
//		DbUtils.dumpTable("logentries");
//		DbUtils.dumpTable("tostringsubclass");
		
		//System.err.println(new Object());
		
		processUnResolvedSubClasses(root, javaproject);
		
		//DbUtils.dumpTable("tostringdb");
		
		long timetaken = System.currentTimeMillis() - startTime;
		
		DbUtils.closeConnection();
		
		LOG.info("TIME USED: " + timetaken/1000 + " seconds" );
		
	}
	
	static void processUnResolvedSubClasses(IWorkspaceRoot root, IJavaProject javaproject) throws Exception{
		Statement st = DbUtils.getConn().createStatement();
		ToStringParser parser = new ToStringParser(javaproject);
		ResultSet subclasses = st.executeQuery("select subclass from tostringsubclass");
		ArrayList<String> names = new ArrayList<String>();
		while(subclasses.next()) {
			names.add(subclasses.getString("subclass"));
		}
		subclasses.close();
		subclasses=null;
		for (String subclassname: names) {
			if (DbUtils.findToStringForClass(subclassname) ==null)  {
				subclassname = subclassname.replaceAll("\\$", ".");
				LOG.info("Parsing unresolved subclasses "+ subclassname);
				IType tp = javaproject.findType(subclassname);
				if (tp==null) {
					LOG.fine("cannot find type:: " + subclassname);
				}
				parser.parseToStringMethod(tp);
			}
		}
	}


}
