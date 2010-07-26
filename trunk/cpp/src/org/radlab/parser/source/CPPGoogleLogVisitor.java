package org.radlab.parser.source;

import org.apache.commons.logging.Log;
import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.ExpansionOverlapsBoundaryException;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTProblem;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IType;
import org.eclipse.cdt.core.dom.ast.cpp.CPPASTVisitor;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamespaceDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateParameter;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTCompoundStatement;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTExpressionList;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTExpressionStatement;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionCallExpression;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTIdExpression;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTLiteralExpression;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTUnaryExpression;
import org.radlab.parser.data.LoggerCallEntry;
import org.radlab.parser.logparser.LogParserImpl;

import utils.LogFormatter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class CPPGoogleLogVisitor extends CPPASTVisitor {

  static Logger LOG = LogFormatter.getLogger(CPPGoogleLogVisitor.class);
  static {
    LOG.setLevel(Level.WARNING);
  }

  private String sourceName;
  private int base_line_num;

  public HashMap<String, LoggerCallEntry> result = new HashMap<String, LoggerCallEntry>();

  public CPPGoogleLogVisitor(String sourceName, HashMap<String, LoggerCallEntry> result,
      int base_line_num) {
    super();
    this.base_line_num = base_line_num;
    this.sourceName = sourceName;
    this.result = result;

    this.shouldVisitStatements = true;
    this.shouldVisitExpressions = true;
    this.shouldVisitNames = true;
    this.shouldVisitAmbiguousNodes = true;
    this.shouldVisitDeclarations = true;
    this.shouldVisitDeclarators = true;
    this.shouldVisitInitializers = true;
    this.shouldVisitProblems = true;
    this.shouldVisitDeclarations = true;
    this.shouldVisitNamespaces = true;
  }

  public CPPGoogleLogVisitor(String sourceName, HashMap<String, LoggerCallEntry> result) {
    this(sourceName, result, 0);
  }



  @Override
  public int visit(ICPPASTNamespaceDefinition namespaceDefinition) {
    return ASTVisitor.PROCESS_CONTINUE;
  }



  @Override
  public int visit(ICPPASTTemplateParameter templateParameter) {
    return ASTVisitor.PROCESS_CONTINUE;
  }

  public int visit(IASTProblem node) {

    String problem = node.getRawSignature();


    int firstline = problem.indexOf("{");
    int lastline = problem.lastIndexOf("}");

    if (firstline < 0 || lastline < 0 || firstline + 1 >= lastline) {
      return ASTVisitor.PROCESS_CONTINUE;
    }
    if (problem.trim().length() == 0) {
      return ASTVisitor.PROCESS_CONTINUE;
    }

    if (LOG.isLoggable(Level.FINEST)) {
      LOG.finest(firstline + 1 + " " + lastline);
    }

    problem = "void main(){" + problem.substring(firstline + 1, lastline) + "}";

    if (LOG.isLoggable(Level.FINE)) {
      LOG.fine("parsing PROBLEM NODE::  \n" + problem);
    }

    int line = node.getFileLocation().getStartingLineNumber();
    IASTTranslationUnit translationUnit =
        FunctionParser.InitializeTranslationUnit(problem, new HashMap<String, String>(), true);
    translationUnit.accept(new CPPGoogleLogVisitor(this.sourceName, GlobalResultCollector.results,
        line));

    return ASTVisitor.PROCESS_CONTINUE;
  }



  @Override
  public int visit(IASTStatement node) {

    // System.err.println("statemetn " + node.getRawSignature() +
    // node.getClass().getSimpleName() );

    if (node instanceof CPPASTExpressionStatement) {
      // System.err.println("node type " + node.getClass().getSimpleName() + " "
      // +
      // node.getRawSignature() );
      String fn = node.getRawSignature();
      // System.err.println(fn + " " +node.getClass().toString());
      // if (fn.matches("[PV]?LOG.*") || fn.matches("TI.*LOG.*") ||
      // fn.startsWith("EV") //|| fn.contains("StringPrintf")
      // ) {
      if (fn.startsWith("PLOG") || fn.startsWith("VLOG") || fn.startsWith("LOG")
          || fn.startsWith("EV") || fn.startsWith("TI") || fn.startsWith("LG")
          || fn.startsWith("GFSLOG") || fn.startsWith("RPC2LOG") || fn.startsWith("PATHLOG")
          || fn.startsWith("PAXOSLOG") || fn.startsWith("LogTablet") || fn.contains("->Log")
          || fn.startsWith("SStringPrintf")) {



        String location =
            this.sourceName + ":"
                + (this.base_line_num + node.getFileLocation().getEndingLineNumber());



        if (result.containsKey(location)) { // already parsed,, don't reparse
          // System.err.println("already parsed " + location);
          return ASTVisitor.PROCESS_SKIP;
        }
        
        if(LOG.isLoggable(Level.INFO)) {
          LOG.info("found: " + node.getRawSignature());
        }
        
        LogPipeVisitor logv = new LogPipeVisitor();

        node.accept(logv);


        LOG.info("==line:: " + location);
        LOG.fine("==level:: " + logv.level);
        LOG.info("==template:: " + logv.template);
        LOG.fine("==names:: " + Arrays.deepToString(logv.varnames.toArray()));
        LOG.fine("==types:: " + Arrays.deepToString(logv.types.toArray()));

        LoggerCallEntry entry =
            new LoggerCallEntry(logv.varnames, logv.types, logv.level, logv.template.toString(),
                location, "");
        this.result.put(location, entry);

        return ASTVisitor.PROCESS_SKIP;
      }
    }
    return ASTVisitor.PROCESS_CONTINUE;
  }

  @Override
  public int visit(IASTExpression node) {
    if (!(node instanceof ICPPASTFunctionCallExpression)) {
      return ASTVisitor.PROCESS_CONTINUE;
    }
    ICPPASTFunctionCallExpression n = (ICPPASTFunctionCallExpression) node;

    String fn = n.getFunctionNameExpression().getRawSignature();
    // System.err.println("FUNCTION:: " + fn);
    if (fn.startsWith("StringPrintf")) {
      LogPipeVisitor logv = new LogPipeVisitor();
      node.accept(logv);
      String location = this.sourceName + ":" + node.getFileLocation().getEndingLineNumber();
      LOG.finer("== Found StringPrintf == " + location);
      LOG.finer("==line:: " + location);
      LOG.finer("==level:: " + logv.level);
      LOG.finer("==template:: " + logv.template);
      LOG.finer("==names:: " + Arrays.deepToString(logv.varnames.toArray()));
      LOG.finer("==types:: " + Arrays.deepToString(logv.types.toArray()));

      LoggerCallEntry entry =
          new LoggerCallEntry(logv.varnames, logv.types, logv.level, logv.template.toString(),
              location, "");
      this.result.put(location, entry);

      return ASTVisitor.PROCESS_SKIP;
    }
    return ASTVisitor.PROCESS_SKIP;
  }
}


class LogPipeVisitor extends CPPASTVisitor {
  
  ArrayList<String> varnames = new ArrayList<String>();
  ArrayList<String> types = new ArrayList<String>();
  String level;
  StringBuffer template = new StringBuffer();
  
  Logger LOG;

  private boolean lastIsHex = false;
  private boolean hasseentilog = false;

  public LogPipeVisitor() {
    super();
    this.shouldVisitStatements = true;
    this.shouldVisitExpressions = true;
    this.LOG = CPPGoogleLogVisitor.LOG;
  }

  public int visit(IASTExpression node) {
    if (node instanceof IASTBinaryExpression) {
      // System.err.println("  binary expr " + node.getRawSignature());
      // visit(((IASTBinaryExpression) node).getOperand1());
      // visit(((IASTBinaryExpression) node).getOperand2());
      int op = ((IASTBinaryExpression) node).getOperator();
      if (op == IASTBinaryExpression.op_shiftLeft) {
        return ASTVisitor.PROCESS_CONTINUE;
      } else {
        // treat as a whole..
        template.append("@#@");
        varnames.add(node.getRawSignature());
        types.add(this.getTypeStr(node));
      }

    } else {
      //System.err.println(" expr " + node.getRawSignature() + " " + node.getClass().toString());
      if (node instanceof CPPASTLiteralExpression) {
        // // literals /////
        String literalValue = new String(((CPPASTLiteralExpression) node).getValue()).trim();
        if (literalValue.startsWith("\"") && literalValue.endsWith("\""))
          template.append(literalValue.substring(1, literalValue.length() - 1)); // trim
        // quotes
      } else if (node instanceof CPPASTFunctionCallExpression) {
        // /// function calls /////
        CPPASTFunctionCallExpression f = (CPPASTFunctionCallExpression) node;
        String funcName = null;
        funcName = f.getFunctionNameExpression().getRawSignature();
        
        if(LOG.isLoggable(Level.FINE)) {
          LOG.fine("   function name: " + funcName + " "
            + Arrays.deepToString(f.getChildren()));
        }
        // a self check to see if function body parsed correctly
        if(LOG.isLoggable(Level.FINE)) {
          if (funcName.contains("(")) {
            for (IASTNode child : f.getChildren()) {
              LOG.fine("     child: " + child.getRawSignature() + " " + child.getClass());
          }
          }
        }
        if ("LOG".equals(funcName)) {
          // special handle LOG calls
          String arg = ((CPPASTIdExpression) f.getParameterExpression()).getName().toString();
          this.level = arg;
        } else if (funcName.matches("TI.?LOG\\((.*)\\)")) {
          // for(IASTNode n: f.getChildren()) {
          // System.err.println("    CHILD:" + n.getRawSignature());
          // }
          // FIXME: why it is called three times?
          if (this.hasseentilog) {
            ;
          } else {
            this.hasseentilog = true;
            LOG.finest("TILOG!!!");
            this.level = funcName;
            // special format for TILOG calls
            template.append("@@@"); // @@@ will be replaced with (.*):\\s+
            varnames.add("ifname");
            types.add("string");
          }
        } else if ("VLOG".equals(funcName)) {
          // special handle VLOG calls
          String arg = f.getParameterExpression().getRawSignature();
          this.level = "VLOG" + arg;
        } else if ("PATHLOG".equals(funcName)) {
          String arg = f.getParameterExpression().getRawSignature();
          this.level = "PATHLOG" + arg;
        } else if ("LG".equals(funcName)) {
          this.level = "LG";
        } else if ("GFSLOG".equals(funcName)) {
          String arg = f.getParameterExpression().getRawSignature();
          this.level = "GFSLOG" + arg;
        } else if ("LogTablet".equals(funcName)) { // big table specific logger
          CPPASTExpressionList params = (CPPASTExpressionList) f.getParameterExpression();
          IASTExpression formatexpr = params.getExpressions()[1];
          boolean isliteral = true;
          if (formatexpr instanceof CPPASTFunctionCallExpression) {
            if (formatexpr.getRawSignature().startsWith("StringPrintf")
                || formatexpr.getRawSignature().startsWith("StrCat")) {
              formatexpr.accept(this);
              this.template.append(" @#@ (@#@) @#@");
              isliteral = false;
            }
          }
          if (isliteral) {
            String template = new String(((CPPASTLiteralExpression) formatexpr).getValue());
            template = template.substring(1, template.length() - 1);
            this.template.append(template).append(" @#@ (@#@) @#@");
          }
          this.level = "LogTablet";
          this.varnames.add("id1");
          this.types.add("int");
          this.varnames.add("tablename");
          this.types.add("string");
          this.varnames.add("trace");
          this.types.add("string");
          return ASTVisitor.PROCESS_SKIP;
        } else if ("RPC2LOG".equals(funcName)) {
          CPPASTExpressionList params = (CPPASTExpressionList) f.getParameterExpression();
          IASTExpression levelexpr = params.getExpressions()[0];
          this.level = "RPC2LOG" + levelexpr.getRawSignature();
          // special format of RPC2LOG
          template.append("@#@"); // expend to (.*:.*: )
          varnames.add("prefix");
          types.add("string");
          params.getExpressions()[2].accept(this);
          return ASTVisitor.PROCESS_SKIP;
        } else if ("LOG_ERROR".equals(funcName)) {
          this.level = "LOG_ERROR";
          return ASTVisitor.PROCESS_CONTINUE;
        } else if ("PLOG".equals(funcName)) {
          // special handle PLOG calls
          String arg = f.getParameterExpression().getRawSignature();
          this.level = "PLOG" + arg;
        } else if ("LOG_IF".equals(funcName) || "VLOG_IF".equals(funcName)) {
          // LOG_IF calls
          CPPASTExpressionList params = (CPPASTExpressionList) f.getParameterExpression();
          IASTExpression levelexpr = params.getExpressions()[0];
          this.level = levelexpr.getRawSignature();
        } else if ("StringPrintf".equals(funcName) || "VLOG_OR_STR_MEM".equals(funcName)) {
          // Sprintf calls
          // System.err.println("StringPrintf!!!");
          IASTExpression paramexpr = f.getParameterExpression();
          if (!(paramexpr instanceof CPPASTExpressionList)
              && !(paramexpr instanceof CPPASTLiteralExpression)) {
            paramexpr = (IASTExpression) paramexpr.getChildren()[0];
          }
          IASTExpression[] params;
          if (paramexpr instanceof CPPASTLiteralExpression) {
            params = new IASTExpression[1];
            params[0] = paramexpr;
          } else {
            params = ((CPPASTExpressionList) paramexpr).getExpressions();
          }
          if (params[0] instanceof CPPASTLiteralExpression) {
            String formatstr = new String(((CPPASTLiteralExpression) params[0]).getValue());
            formatstr = formatstr.replaceAll("%[a-z0-9\\.]+", "@#@");
            template.append(formatstr.substring(1, formatstr.length() - 1)); // remove
                                                                             // the
                                                                             // quotes
            for (int i = 1; i < params.length; i++) {
              varnames.add(params[i].getRawSignature());
              types.add(getTypeStr(params[i]));
            }
          } else {
            LOG.warning("could not find format string");
          }
        } else if ("SStringPrintf".equals(funcName)) {
          // Sprintf calls
          // System.err.println("StringPrintf!!!");
          IASTExpression paramexpr = f.getParameterExpression();
          IASTExpression[] params;
          params = ((CPPASTExpressionList) paramexpr).getExpressions();
          if (params[1] instanceof CPPASTLiteralExpression) {
            String formatstr = new String(((CPPASTLiteralExpression) params[1]).getValue());
            formatstr = formatstr.replaceAll("%[a-z0-9\\.]+", "@#@");
            template.append(formatstr.substring(1, formatstr.length() - 1)); // remove
                                                                             // the
                                                                             // quotes
            for (int i = 2; i < params.length; i++) {
              varnames.add(params[i].getRawSignature());
              types.add(getTypeStr(params[i]));
            }
          } else {
            LOG.warning("could not find format string");
          }
        } else if ("StrCat".equals(funcName)) {
          LOG.finest("StrCat!!!");
          IASTExpression[] params =
              ((CPPASTExpressionList) f.getParameterExpression()).getExpressions();
          for (IASTExpression e : params) {
            e.accept(this);
          }
        } else if ("EV".equals(funcName)) {
          LOG.finest("EV!!!");
          IASTExpression[] params =
              ((CPPASTExpressionList) f.getParameterExpression()).getExpressions();
          this.level = params[0].getRawSignature();
          String formatstr = new String(((CPPASTLiteralExpression) params[1]).getValue());
          formatstr = formatstr.replaceAll("%[a-z0-9\\.]+", "@#@");
          template.append(formatstr.substring(1, formatstr.length() - 1)); // remove
                                                                           // the
                                                                           // quotes
          for (int i = 2; i < params.length; i++) {
            varnames.add(params[i].getRawSignature());
            types.add(getTypeStr(params[i]));
          }
        } else if (funcName.startsWith("EVT")) {
          // IASTExpression[] params = ((CPPASTExpressionList)
          // f.getParameterExpression()).getExpressions();
          // this.level = new String(
          // ((CPPASTIdExpression)params[1]).getName().toCharArray() );
          // String formatstr = new String( ((CPPASTLiteralExpression)
          // params[3]).getValue() );
          // formatstr = formatstr.replaceAll("%[a-z0-9\\.]+", "@#@");
          // template.append(formatstr.substring(1,formatstr.length()-1)); //
          // remove the quotes
          // for(int i=4; i<params.length; i++) {
          // varnames.add(params[i].getRawSignature());
          // types.add( getTypeStr(params[i]) );
          // }
          // // this is a hack /////
          LOG.finest("EVT!!!!");
          String line = node.getRawSignature().replace("\n", "");
          LOG.finest("EVT Value::" + line);
          Pattern p = Pattern.compile("EVT\\(\\s*([^,]+),\\s*[A-Z]+,\\s*\"([^\"]*)\",?(.*)\\)\\z");
          Matcher m = p.matcher(line);
          String formatstr;
          // try both possible format of EVT
          if (m.matches()) {

          } else {
            p = Pattern.compile("EVT\\(\\s*([^,]+),\\s*\"(.*)\",?(.*)\\)\\z");
            m = p.matcher(line);
            if (m.matches()) {

            } else {
              LOG.warning("Could not parse EVT correctly...");
              return ASTVisitor.PROCESS_SKIP;
            }
          }
          this.level = "EVT";
          formatstr = m.group(2);
          formatstr = formatstr.replaceAll("\\\"\\s+\\\"", "");
          formatstr = formatstr.replaceAll("%[a-z0-9\\.]+", "@#@");
          template.append(formatstr);
          String[] params = m.group(3).split(",");
          for (int i = 0; i < params.length; i++) {
            varnames.add(params[i].trim());
            types.add("unknown");
          }
          return ASTVisitor.PROCESS_SKIP;
        } else if ("PAXOSLOG".equals(funcName)) {
          IASTExpression[] params =
              ((CPPASTExpressionList) f.getParameterExpression()).getExpressions();
          this.level = params[0].getRawSignature();
          String formatstr = new String(((CPPASTLiteralExpression) params[1]).getValue());
          formatstr = formatstr.replaceAll("%[a-z0-9\\.]+", "@#@");
          template.append(formatstr.substring(1, formatstr.length() - 1)); // remove
                                                                           // the
                                                                           // quotes
          for (int i = 2; i < params.length; i++) {
            varnames.add(params[i].getRawSignature());
            types.add(getTypeStr(params[i]));
          }
        } else if ("VLOG_OR_STR".equals(funcName)) {
          LOG.finest("VLOG_OR_STR!!!");
          IASTExpression[] params =
              ((CPPASTExpressionList) f.getParameterExpression()).getExpressions();
          this.level =
              "VLOG_OR_STR" + params[0].getRawSignature() + "-" + params[1].getRawSignature();
          params[2].accept(this);
        } else if ("LOG_EVERY_N".equals(funcName)) {
          IASTExpression arg =
              ((CPPASTExpressionList) f.getParameterExpression()).getExpressions()[0];
          this.level = arg.toString();
        } else if (funcName.matches(".+->Log")) {
          LOG.finest(funcName + "!!!");
          IASTExpression[] params =
              ((CPPASTExpressionList) f.getParameterExpression()).getExpressions();
          this.level = params[0].getRawSignature();
          this.template.append("@#@: ");
          this.varnames.add("var1");
          this.varnames.add("string");
          for (int i = 1; i < params.length; i++) {
            params[i].accept(this);
          }
          return ASTVisitor.PROCESS_SKIP;
        } else {
          // some random function
          String nodesig = node.getRawSignature();
          if (!nodesig.equals("LOG_ERROR")) {
            template.append("@#@");
            varnames.add(node.getRawSignature());
            types.add(this.getTypeStr(node));
          }
        }

      } else if (node instanceof CPPASTIdExpression) {
        // special handle of "hex" operator
        String nstr = ((CPPASTIdExpression) node).getName().toString();
        if ("hex".equals(nstr) || "std::hex".equals(nstr)) {
          // skip,, remember that we have seen a hex operator
          this.lastIsHex = true;
        } else if ("std::dec".equals(nstr) || "oct".equals(nstr) || "dec".equals(nstr)
            || "count".equals(nstr) || "cs".equals(nstr)) {
          // skip
        } else {
          template.append("@#@");
          varnames.add(nstr);
          // try to resolve type
          CPPASTIdExpression idexpr = (CPPASTIdExpression) node;
          this.types.add(getTypeStr(node));
        }
      } else {
        template.append("@#@");
        varnames.add(node.getRawSignature());
        types.add(this.getTypeStr(node));
      }
    }
    return ASTVisitor.PROCESS_SKIP;
  }

  private String getTypeStr(IASTExpression node) {
    if (node == null) {
      return "null";
    }
    IType tp = node.getExpressionType();
    if (tp == null) {
      return "unresolved";
    }
    String typestr = tp.toString();
    if (lastIsHex) {
      lastIsHex = false; // consume the operator
      typestr += "-hex";
    }
    if (typestr.trim().length() == 0) {
      typestr = "unresolved-empty";
    }
    LOG.fine(node.getRawSignature() + " type::" + typestr);
    return typestr;
  }
}
