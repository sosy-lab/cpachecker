package fllesh;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.cdt.core.dom.ICodeReaderFactory;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpressionList;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTProblem;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.model.ILanguage;
import org.eclipse.cdt.core.parser.CodeReader;
import org.eclipse.cdt.core.parser.IParserLogService;
import org.eclipse.cdt.core.parser.IScannerInfo;
import org.eclipse.cdt.core.parser.ParserFactory;
import org.eclipse.core.runtime.CoreException;

import cmdline.stubs.CLanguage;
import cmdline.stubs.StubCodeReaderFactory;
import cmdline.stubs.StubScannerInfo;

import com.google.common.base.Joiner;

import cfa.CFABuilder;
import cfa.CFATopologicalSort;
import cfa.DOTBuilder;
import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFAFunctionDefinitionNode;
import cfa.objectmodel.CFANode;
import cfa.objectmodel.c.CallToReturnEdge;
import cfa.objectmodel.c.FunctionCallEdge;
import cfa.objectmodel.c.FunctionDefinitionNode;
import cfa.objectmodel.c.ReturnEdge;
import cfa.objectmodel.c.StatementEdge;

import common.configuration.Configuration;
import compositeCPA.CompositeCPA;

import cpa.art.ARTCPA;
import cpa.art.ARTStatistics;
import cpa.common.LogManager;
import cpa.common.ReachedElements;
import cpa.common.CPAcheckerResult.Result;
import cpa.common.algorithm.CEGARAlgorithm;
import cpa.common.algorithm.CPAAlgorithm;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.CPAFactory;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.Statistics;
import cpa.location.LocationCPA;
import cpa.observeranalysis.ObserverAutomatonCPA;
import cpa.symbpredabsCPA.SymbPredAbsCPA;
import exceptions.CPAException;
import fllesh.cpa.edgevisit.EdgeVisitCPA;
import fllesh.ecp.reduced.ObserverAutomatonCreator;
import fllesh.ecp.reduced.ObserverAutomatonTranslator;
import fllesh.ecp.reduced.Pattern;
import fllesh.fql.fllesh.util.CFATraversal;
import fllesh.fql.fllesh.util.CFAVisitor;
import fllesh.fql.fllesh.util.CPAchecker;
import fllesh.fql.fllesh.util.Cilly;
import fllesh.fql.frontend.ast.filter.FunctionCall;
import fllesh.fql.frontend.ast.filter.Identity;
import fllesh.fql.frontend.ast.filter.SetMinus;
import fllesh.fql2.ast.Edges;
import fllesh.fql2.ast.coveragespecification.CoverageSpecification;
import fllesh.fql2.ast.coveragespecification.Quotation;
import fllesh.fql2.ast.pathpattern.PathPattern;
import fllesh.fql2.ast.pathpattern.Repetition;

public class Main {

  private static Configuration createConfiguration(String pSourceFile, String pPropertiesFile) {
    return createConfiguration(Collections.singletonList(pSourceFile), pPropertiesFile);
  }
  
  private static Configuration createConfiguration(List<String> pSourceFiles, String pPropertiesFile) {
    Map<String, String> lCommandLineOptions = new HashMap<String, String>();    

    lCommandLineOptions.put("analysis.programNames", Joiner.on(", ").join(pSourceFiles));
    //lCommandLineOptions.put("output.path", "test/output");
    
    Configuration lConfiguration = null;
    try {
      lConfiguration = new Configuration(pPropertiesFile, lCommandLineOptions);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
    return lConfiguration;
  }
  
  private static File createPropertiesFile() {
    File lPropertiesFile = null;
    
    try {
      
      lPropertiesFile = File.createTempFile("fllesh.", ".properties");
      lPropertiesFile.deleteOnExit();
      
      PrintWriter lWriter = new PrintWriter(new FileOutputStream(lPropertiesFile));
      // we do not use a fixed error location (error label) therefore
      // we do not want to remove parts of the CFA
      lWriter.println("cfa.removeIrrelevantForErrorLocations = false");
      
      lWriter.println("log.consoleLevel = ALL");
      
      lWriter.println("analysis.traversal = topsort");
      
      // we want to use CEGAR algorithm
      lWriter.println("analysis.useRefinement = true");    
      lWriter.println("cegar.refiner = " + cpa.symbpredabsCPA.SymbPredAbsRefiner.class.getCanonicalName());

      lWriter.close();
      
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
    return lPropertiesFile;
  }

  private static File createPropertiesFile(File pObserverAutomatonFile) {
    File lPropertiesFile = Main.createPropertiesFile();
    
    // append configuration for observer automaton
    PrintWriter lWriter;
    try {
      
      lWriter = new PrintWriter(new FileOutputStream(lPropertiesFile, true));
      
      lWriter.println("observerAnalysis.inputFile = " + pObserverAutomatonFile.getAbsolutePath());
      lWriter.println("observerAnalysis.dotExportFile = test/output/observerAutomatonExport.dot");
      lWriter.close();
      
      return lPropertiesFile;
      
    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
    return null;
  }
  
  private static void checkForASTProblems(IASTNode pAST) {
    if (pAST instanceof IASTProblem) {
      throw new RuntimeException("Error during parsing C code \""
          + pAST.getRawSignature() + "\": " + ((IASTProblem)pAST).getMessage());
    } else {
      for (IASTNode n : pAST.getChildren()) {
        checkForASTProblems(n);
      }
    }
  }
  
  /**
   * @param pArguments
   * @throws Exception 
   */
  public static void main(String[] pArguments) throws Exception {
    assert(pArguments != null);
    assert(pArguments.length > 1);
    
    // check cilly invariance of source file, i.e., is it changed when preprocessed by cilly?
    Cilly lCilly = new Cilly();
    
    String lSourceFileName = pArguments[1];
    
    if (!lCilly.isCillyInvariant(lSourceFileName)) {
      File lCillyProcessedFile = lCilly.cillyfy(pArguments[1]);
      
      lSourceFileName = lCillyProcessedFile.getAbsolutePath();
      
      System.err.println("WARNING: Given source file is not CIL invariant ... did preprocessing!");
    }
    
    File lPropertiesFile = Main.createPropertiesFile();
    Configuration lConfiguration = Main.createConfiguration(lSourceFileName, lPropertiesFile.getAbsolutePath());

    LogManager lLogManager = new LogManager(lConfiguration);
      
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager);
    
    
    CFAFunctionDefinitionNode lMainFunction = lCPAchecker.getMainFunction();
    
    

    
    
    
    final fllesh.fql2.ast.coveragespecification.Translator lCoverageSpecificationTranslator = new fllesh.fql2.ast.coveragespecification.Translator(lMainFunction);
    
    FunctionCall lFunctionCallFilter = new FunctionCall("f");
    SetMinus lSetMinus = new SetMinus(Identity.getInstance(), lFunctionCallFilter);
    //PathPattern lPrefixPattern = new Repetition(new Edges(Identity.getInstance()));
    PathPattern lPrefixPattern = new Repetition(new Edges(lSetMinus));
    Quotation lQuotation = new Quotation(lPrefixPattern);
    CoverageSpecification lTarget = new Edges(lFunctionCallFilter);
    
    CoverageSpecification lIdRepetition = new Quotation(new Repetition(new Edges(Identity.getInstance())));
    
    CoverageSpecification lSpecification = new fllesh.fql2.ast.coveragespecification.Concatenation(lQuotation, new fllesh.fql2.ast.coveragespecification.Concatenation(lTarget, lIdRepetition));
    
    Set<Pattern> lTestGoals = lCoverageSpecificationTranslator.translate(lSpecification);
    
    System.out.println(lTestGoals);

    Pattern lTestGoal = null; 
    
    for (Pattern lGoal : lTestGoals) {
      lTestGoal = lGoal;
      break;
    }
    
    System.out.println(lTestGoal);
    
    
    
    /** Generating a wrapper start up method */
    
    StringWriter lWrapperFunction = new StringWriter();
    PrintWriter lWriter = new PrintWriter(lWrapperFunction);
    
    FunctionDefinitionNode lMain = (FunctionDefinitionNode)lMainFunction;
    
    //lWriter.println("void " + lMain.getFunctionDefinition().getDeclarator().getRawSignature() + ";");
    //lWriter.println();
    
    lWriter.println("void __FLLESH__main()");
    lWriter.println("{");
        
    for (IASTParameterDeclaration lDeclaration : lMain.getFunctionParameters()) {
      lWriter.println("  " + lDeclaration.getRawSignature() + ";");
    }
    
    lWriter.println();
    lWriter.print("  " + lMainFunction.getFunctionName() + "(");

    boolean isFirst = true;
    
    for (IASTParameterDeclaration lDeclaration : lMain.getFunctionParameters()) {
      if (isFirst) {
        isFirst = false;
      }
      else {
        lWriter.print(", ");
      }
      
      lWriter.print(lDeclaration.getDeclarator().getName());
    }
    
    lWriter.println(");");
    lWriter.println("  return;");
    lWriter.println("}");
    

    System.out.println(lWrapperFunction);
    
    
    CodeReader reader = new CodeReader(lWrapperFunction.toString().toCharArray());
    
    IScannerInfo scannerInfo = StubScannerInfo.getInstance();
    ICodeReaderFactory codeReaderFactory = new StubCodeReaderFactory();
    IParserLogService parserLog = ParserFactory.createDefaultLogService();

    ILanguage lang = new CLanguage("C99");

    IASTTranslationUnit ast;
    try {
       ast = lang.getASTTranslationUnit(reader, scannerInfo, codeReaderFactory, null, parserLog);
    } catch (CoreException e) {
      throw new RuntimeException("Error during parsing C code \""
          + lWrapperFunction.toString() + "\": " + e.getMessage());
    }
    
    Main.checkForASTProblems(ast);
    
    CFABuilder lCFABuilder = new CFABuilder(lLogManager);
    
    ast.accept(lCFABuilder);
    
    Map<String, CFAFunctionDefinitionNode> cfas = lCFABuilder.getCFAs();
    
    // annotate CFA nodes with topological information for later use
    for(CFAFunctionDefinitionNode cfa : cfas.values()){
      CFATopologicalSort topSort = new CFATopologicalSort();
      topSort.topologicalSort(cfa);
    }
    
    CFAFunctionDefinitionNode lWrapperEntry = cfas.get("__FLLESH__main");
    
    
    
    
    final Map<String, CFAFunctionDefinitionNode> lAllCFAs = new HashMap<String, CFAFunctionDefinitionNode>();
    lAllCFAs.putAll(cfas);
    lAllCFAs.putAll(lCPAchecker.getCFAMap());
    
    CFAVisitor lVisitor = new CFAVisitor() {

      private String mFunctionName;
      
      @Override
      public void init(CFANode pInitialNode) {
        mFunctionName = pInitialNode.getFunctionName();
      }

      @Override
      public void visit(CFAEdge pP) {
        pP.getSuccessor().setFunctionName(mFunctionName);
      }
      
    };
    
    // set function names
    CFATraversal.traverse(lWrapperEntry, lVisitor);
    
    // correct call to main function
    CFAVisitor lVisitor2 = new CFAVisitor() {

      private CFAEdge mAlphaEdge;
      private CFAEdge mOmegaEdge;
      private CFAEdge mAlphaToOmegaEdge;
      
      public CFAEdge getAlphaEdge() {
        return mAlphaEdge;
      }
      
      public CFAEdge getOmegaEdge() {
        return mOmegaEdge;
      }
      
      @Override
      public void init(CFANode pInitialNode) {
      
      }

      @Override
      public void visit(CFAEdge pP) {
        if (pP instanceof StatementEdge) {
          IASTExpression expr = ((StatementEdge)pP).getExpression();
          
          if (expr instanceof IASTFunctionCallExpression) {
            createCallAndReturnEdges(pP.getPredecessor(), pP.getSuccessor(), pP, expr, (IASTFunctionCallExpression)expr);
            
            lCoverageSpecificationTranslator.getAnnotations().getId(mAlphaEdge);
            lCoverageSpecificationTranslator.getAnnotations().getId(mOmegaEdge);
            lCoverageSpecificationTranslator.getAnnotations().getId(mAlphaToOmegaEdge);
          }
                    
        }
        else {
          lCoverageSpecificationTranslator.getAnnotations().getId(pP);          
        }
      }
      
      private void createCallAndReturnEdges(CFANode node, CFANode successorNode, CFAEdge edge, IASTExpression expr, IASTFunctionCallExpression functionCall) {
        String functionName = functionCall.getFunctionNameExpression().getRawSignature();
        CFAFunctionDefinitionNode fDefNode = lAllCFAs.get(functionName);

        //get the parameter expression
        IASTExpression parameterExpression = functionCall.getParameterExpression();
        IASTExpression[] parameters = null;
        //in case of an expression list, get the corresponding array 
        if (parameterExpression instanceof IASTExpressionList) {
          IASTExpressionList paramList = (IASTExpressionList)parameterExpression;
          parameters = paramList.getExpressions();
        //in case of a single parameter, use a single-entry array
        } else if (parameterExpression != null) {
          parameters = new IASTExpression[] {parameterExpression};
        }
        FunctionCallEdge callEdge;
        
        callEdge = new FunctionCallEdge(functionCall, edge.getLineNumber(), node, fDefNode, parameters, false);
        callEdge.addToCFA();
        mAlphaEdge = callEdge;
        
        // set name of the function
        fDefNode.setFunctionName(functionName);
        // set return edge from exit node of the function
        ReturnEdge returnEdge = new ReturnEdge("Return Edge to " + successorNode.getNodeNumber(), edge.getLineNumber(), lAllCFAs.get(functionName).getExitNode(), successorNode);
        returnEdge.addToCFA();
        returnEdge.getSuccessor().setFunctionName(node.getFunctionName());
        
        mOmegaEdge = returnEdge;

        CallToReturnEdge calltoReturnEdge = new CallToReturnEdge(expr.getRawSignature(), edge.getLineNumber(), node, successorNode, expr);
        calltoReturnEdge.addToCFA();
        
        mAlphaToOmegaEdge = calltoReturnEdge;

        node.removeLeavingEdge(edge);
        successorNode.removeEnteringEdge(edge);
      }
      
    };
    
    CFATraversal.traverse(lWrapperEntry, lVisitor2);
    
    //DOTBuilder lDOTBuilder = new DOTBuilder();
    //lDOTBuilder.generateDOT(lAllCFAs.values(), lWrapperEntry, new File("/tmp/wrapper.dot"));
    
    /** wrapper end */
    
    String lAlphaId = "E4";
    String lOmegaId = "E5";

    
    
    // TODO: for every test goal (i.e., pattern) create an automaton and check reachability
    
    
    
    File lAutomatonFile = File.createTempFile("fllesh.", ".oa");
    lAutomatonFile.deleteOnExit();
    
    PrintStream lObserverAutomaton = new PrintStream(new FileOutputStream(lAutomatonFile));
    lObserverAutomaton.println(ObserverAutomatonTranslator.translate(lTestGoal, "Goal", lAlphaId, lOmegaId));
    lObserverAutomaton.close();
    
    
    
    // TODO remove this output code
    //DOTBuilder dotBuilder = new DOTBuilder();
    //dotBuilder.generateDOT(lCPAchecker.getCFAMap().values(), lMainFunction, new File("/tmp/mycfa.dot"));
    
    
    
    File lExtendedPropertiesFile = Main.createPropertiesFile(lAutomatonFile);
    Configuration lExtendedConfiguration = Main.createConfiguration(lSourceFileName, lExtendedPropertiesFile.getAbsolutePath());

    EdgeVisitCPA.Factory lFactory = new EdgeVisitCPA.Factory(lCoverageSpecificationTranslator.getAnnotations());
    lFactory.setConfiguration(lExtendedConfiguration);
    lFactory.setLogger(lLogManager);
    ConfigurableProgramAnalysis lEdgeVisitCPA = lFactory.createInstance();
    
    CPAFactory lAutomatonFactory = ObserverAutomatonCPA.factory();
    lAutomatonFactory.setConfiguration(lExtendedConfiguration);
    lAutomatonFactory.setLogger(lLogManager);
    ConfigurableProgramAnalysis lObserverCPA = lAutomatonFactory.createInstance();

    CPAFactory lLocationCPAFactory = LocationCPA.factory();
    ConfigurableProgramAnalysis lLocationCPA = lLocationCPAFactory.createInstance();
    
    CPAFactory lSymbPredAbsCPAFactory = SymbPredAbsCPA.factory();
    lSymbPredAbsCPAFactory.setConfiguration(lConfiguration);
    lSymbPredAbsCPAFactory.setLogger(lLogManager);
    ConfigurableProgramAnalysis lSymbPredAbsCPA = lSymbPredAbsCPAFactory.createInstance();

    LinkedList<ConfigurableProgramAnalysis> lComponentAnalyses = new LinkedList<ConfigurableProgramAnalysis>();    
    lComponentAnalyses.add(lLocationCPA);
    lComponentAnalyses.add(lEdgeVisitCPA);
    lComponentAnalyses.add(lSymbPredAbsCPA);
    lComponentAnalyses.add(lObserverCPA);

    // create composite CPA
    CPAFactory lCPAFactory = CompositeCPA.factory();  
    lCPAFactory.setChildren(lComponentAnalyses);
    lCPAFactory.setConfiguration(lConfiguration);
    lCPAFactory.setLogger(lLogManager);
    ConfigurableProgramAnalysis lCPA = lCPAFactory.createInstance();

    // create ART CPA
    CPAFactory lARTCPAFactory = ARTCPA.factory();    
    lARTCPAFactory.setChild(lCPA);
    lARTCPAFactory.setConfiguration(lConfiguration);
    lARTCPAFactory.setLogger(lLogManager);
    ConfigurableProgramAnalysis lARTCPA = lARTCPAFactory.createInstance();
    
    
    
    CEGARAlgorithm lAlgorithm = new CEGARAlgorithm(new CPAAlgorithm(lARTCPA, lLogManager), lConfiguration, lLogManager);
    
    Statistics lARTStatistics = new ARTStatistics(lConfiguration, lLogManager);
    Set<Statistics> lStatistics = new HashSet<Statistics>();
    lStatistics.add(lARTStatistics);
    lAlgorithm.collectStatistics(lStatistics);
    
    AbstractElement initialElement = lARTCPA.getInitialElement(lWrapperEntry);
    Precision initialPrecision = lARTCPA.getInitialPrecision(lWrapperEntry);
          
    ReachedElements lReachedElements = new ReachedElements(ReachedElements.TraversalMethod.TOPSORT, true);
    lReachedElements.add(initialElement, initialPrecision);
    
    try {
      lAlgorithm.run(lReachedElements, true);      
    } catch (CPAException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
        
    for (AbstractElement reachedElement : lReachedElements) {
      // TODO determine whether ERROR element was reached
      
      System.out.println(reachedElement);
    }
    
    PrintWriter lStatisticsWriter = new PrintWriter(System.out);

    lARTStatistics.printStatistics(lStatisticsWriter, Result.SAFE, lReachedElements);
  }

}

