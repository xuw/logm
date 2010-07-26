package utils;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.cdt.core.dom.ICodeReaderFactory;
import org.eclipse.cdt.core.dom.ast.ASTVisitor;
//import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
//import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
//import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.gnu.c.GCCLanguage;
//import org.eclipse.cdt.core.dom.ast.gnu.cpp.GPPLanguage;
//import org.eclipse.cdt.core.dom.lrparser.c99.C99Language;
import org.eclipse.cdt.core.parser.CodeReader;
import org.eclipse.cdt.core.parser.DefaultLogService;
import org.eclipse.cdt.core.parser.IParserLogService;
import org.eclipse.cdt.core.parser.IScannerInfo;
import org.eclipse.cdt.core.parser.ScannerInfo;



public class ParserTest {
      public static void main(String[] args) throws Exception {
            IParserLogService log = new DefaultLogService();

            //String code = "class Class { public: int x,y; Class(); Class(); private: Class f(); }; int function(double parameter) { return parameter; };";
            
            String code ="main(){int a=123; printf(\"here is %d\", a);  }";
            
            CodeReader reader = new CodeReader(code.toCharArray());
            
            Map<String,String> definedSymbols = new HashMap<String, String>();
            String[] includePaths = new String[0];
            IScannerInfo info = new ScannerInfo(definedSymbols,includePaths);
            ICodeReaderFactory readerFactory = FileCodeReaderFactory.getInstance();

            //IASTTranslationUnit translationUnit = GPPLanguage.getDefault().getASTTranslationUnit(reader, info, readerFactory, null, log);            
            IASTTranslationUnit translationUnit = GCCLanguage.getDefault().getASTTranslationUnit(reader, info, readerFactory, null, log);
            
            ASTVisitor visitor = new ASTVisitor() {
                  public int visit(IASTName name) {
                        System.out.println(name+ " binding: " + name.resolveBinding() );                        
                        return ASTVisitor.PROCESS_CONTINUE;
                  }
                  
                  public int visit(IASTStatement name) {
                      System.out.println(name.getRawSignature());
                      return ASTVisitor.PROCESS_CONTINUE;
                  }
                  
                  public int visit(IASTExpression name) {
                      System.out.println("expr:" + name.getRawSignature());
                      return ASTVisitor.PROCESS_CONTINUE;
                  }
                                
                  };
                  
            //System.exit(0);
            
            visitor.shouldVisitNames = true;
            visitor.shouldVisitStatements = true;
            visitor.shouldVisitExpressions = true;
            
            translationUnit.accept(visitor);
      }
}
