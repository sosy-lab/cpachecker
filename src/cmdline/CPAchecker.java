package cmdline;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;

import common.Pair;
import compositeCPA.CompositeCPA;
import compositeCPA.CompositeStopOperator;

import cfa.CFABuilder;
import cfa.CFACheck;
import cfa.CFAMap;
import cfa.CFAReduction;
import cfa.CFASimplifier;
import cfa.CFATopologicalSort;
import cfa.CPASecondPassBuilder;
import cfa.DOTBuilder;
import cfa.DOTBuilderInterface;
import cfa.objectmodel.BlankEdge;
import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFAFunctionDefinitionNode;
import cfa.objectmodel.CFANode;
import cfa.objectmodel.c.GlobalDeclarationEdge;
import cmdline.CPAMain.Result;
import cmdline.stubs.StubFile;

import cpa.art.ARTCPA;
import cpa.common.LogManager;
import cpa.common.ReachedElements;
import cpa.common.algorithm.Algorithm;
import cpa.common.algorithm.CBMCAlgorithm;
import cpa.common.algorithm.CEGARAlgorithm;
import cpa.common.algorithm.CPAAlgorithm;
import cpa.common.algorithm.InvariantCollectionAlgorithm;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.CPAWithStatistics;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.Precision;
import cpa.symbpredabs.BlockCFABuilder;
import cpa.symbpredabs.summary.SummaryCFABuilder;
import cpa.symbpredabs.summary.SummaryDOTBuilder;
import cpaplugin.CPAConfiguration;
import cpaplugin.MainCPAStatistics;
import exceptions.CFAGenerationRuntimeException;
import exceptions.CPAException;

public class CPAchecker {
  private CPAConfiguration mConfiguration;
  private LogManager mLogManager;
  private MainCPAStatistics mStatistics;
  private CFAMap mCFAMap;
  private CFAFunctionDefinitionNode mMainFunction;
  
  public CPAchecker(CPAConfiguration pConfiguration, LogManager pLogManager, MainCPAStatistics pStatistics) {
    assert(pConfiguration != null);
    assert(pLogManager != null);
    assert(pStatistics != null);
    
    mConfiguration = pConfiguration;
    mLogManager = pLogManager;
    mStatistics = pStatistics;
    
    // initialize configuration and logging manager of CPAMain
    CPAMain.cpaConfig = mConfiguration;
    CPAMain.logManager = mLogManager;
    
    
    // get code file name
    String[] names = mConfiguration.getPropertiesArray("analysis.programNames");
    if (names == null) {
      mLogManager.log(Level.SEVERE, "No code file given!");
      
      System.exit(1);
    }
    
    if (names.length != 1) {
      mLogManager.log(Level.SEVERE, "Support for multiple code files is currently not implemented!");
      
      System.exit(1);
    }
    
    StubFile lSourceFile = new StubFile(names[0]);
    
    // parse code file
    IASTTranslationUnit ast = CPAMain.parse(lSourceFile);
    
    // start measuring time
    //cpaStats.startProgramTimer();

    // create CFA
    Pair<CFAMap, CFAFunctionDefinitionNode> cfa = createCFA(ast);

    mCFAMap = cfa.getFirst();
    mMainFunction = cfa.getSecond();
  }
  
  public CFAMap getCFAMap() {
    return mCFAMap;
  }
  
  public CFAFunctionDefinitionNode getMainFunction() {
    return mMainFunction;
  }
  
  private Pair<CFAMap, CFAFunctionDefinitionNode> createCFA(IASTTranslationUnit pTranslationUnit) {

    // Build CFA
    final CFABuilder builder = new CFABuilder();
    
    try {
      pTranslationUnit.accept(builder);
    } catch (CFAGenerationRuntimeException e) {
      // only log message, not whole exception because this is a C problem,
      // not a CPAchecker problem
      mLogManager.log(Level.SEVERE, e.getMessage());
      System.exit(0);
    }
    
    final CFAMap cfas = builder.getCFAs();
    final Collection<CFAFunctionDefinitionNode> cfasList = cfas.cfaMapIterator();
    final int numFunctions = cfas.size();
    
    // annotate CFA nodes with topological information for later use
    for(CFAFunctionDefinitionNode cfa : cfasList){
      CFATopologicalSort topSort = new CFATopologicalSort();
      topSort.topologicalSort(cfa);
    }
    
    // --Refactoring:
    CFAFunctionDefinitionNode mainFunction = initCFA(builder, cfas);
    
    // --Refactoring: The following section was relocated to after the "initCFA" method 
    
    // remove irrelevant locations
    if (mConfiguration.getBooleanValue("cfa.removeIrrelevantForErrorLocations")) {
      CFAReduction coi =  new CFAReduction();
      coi.removeIrrelevantForErrorLocations(mainFunction);

      if (mainFunction.getNumLeavingEdges() == 0) {
        mLogManager.log(Level.INFO, "No error locations reachable from " + mainFunction.getFunctionName()
              + ", analysis not necessary.");
        System.exit(0);
      }
    }
    
    // check the super CFA starting at the main function
    // enable only while debugging/testing
    if(mConfiguration.getBooleanValue("cfa.check")){
      CFACheck.check(mainFunction);
    }

    // write CFA to file
    if (mConfiguration.getBooleanValue("cfa.export")) {
      DOTBuilderInterface dotBuilder;
      if (mConfiguration.getBooleanValue( "analysis.useSummaryLocations")) {
        dotBuilder = new SummaryDOTBuilder();
      } else {
        dotBuilder = new DOTBuilder();
      }
      String cfaFile = mConfiguration.getProperty("cfa.file", "cfa.dot");
      //if no filename is given, use default value
      String path = mConfiguration.getProperty("output.path") + cfaFile;
      try {
        dotBuilder.generateDOT(cfasList, mainFunction,
            new File(path).getPath());
      } catch (IOException e) {
        mLogManager.logException(Level.WARNING, e,
          "Could not write CFA to dot file, check configuration option cfa.file!");
        // continue with analysis
      }
    }
    
    mLogManager.log(Level.FINE, "DONE, CFA for", numFunctions, "functions created");

    return new Pair<CFAMap, CFAFunctionDefinitionNode>(cfas, mainFunction);
  }
  
  private CFAFunctionDefinitionNode initCFA(final CFABuilder builder, final CFAMap cfas) {
    
    final Collection<CFAFunctionDefinitionNode> cfasList = cfas.cfaMapIterator();
    String mainFunctionName = mConfiguration.getProperty("analysis.entryFunction", "main");
    
    CFAFunctionDefinitionNode mainFunction = cfas.getCFA(mainFunctionName);
    
    if (mainFunction == null) {
      mLogManager.log(Level.SEVERE, "Function", mainFunctionName, "not found!");
      System.exit(0);
    }
    
    // simplify CFA
    if (mConfiguration.getBooleanValue("cfa.simplify")) {
      // TODO Erkan Simplify each CFA
      CFASimplifier simplifier = new CFASimplifier();
      simplifier.simplify(mainFunction);
    }

    // Insert call and return edges and build the supergraph
    if (mConfiguration.getBooleanValue("analysis.interprocedural")) {
      mLogManager.log(Level.FINE, "Analysis is interprocedural, adding super edges");
      
      boolean noExtCalls = mConfiguration.getBooleanValue("analysis.noExternalCalls");
      CPASecondPassBuilder spbuilder = new CPASecondPassBuilder(cfas, noExtCalls);
      
      for (CFAFunctionDefinitionNode cfa : cfasList) {
        spbuilder.insertCallEdges(cfa.getFunctionName());
      }
    }
    
    // optionally combine several edges into summary edges
    if (mConfiguration.getBooleanValue( "analysis.useSummaryLocations")) {
      mLogManager.log(Level.FINE, "Building Summary CFAs");
   
      SummaryCFABuilder summaryBuilder = new SummaryCFABuilder(mainFunction,
                                              builder.getGlobalDeclarations());
      return summaryBuilder.buildSummary();

    } else if (mConfiguration.getBooleanValue("analysis.useBlockEdges")){
      mLogManager.log(Level.FINE, "Building Block CFAs");
      
      BlockCFABuilder summaryBuilder =   new BlockCFABuilder(mainFunction,
                                              builder.getGlobalDeclarations());
      return summaryBuilder.buildBlocks();

    } 
    // TODO The following else-if block should be converted to "if"
    // and relocated to the top of the method.
    else if (mConfiguration.getBooleanValue("analysis.useGlobalVars")){
      // add global variables at the beginning of main
      
      List<IASTDeclaration> globalVars = builder.getGlobalDeclarations();
      insertGlobalDeclarations(mainFunction, globalVars);
    }
    
    return mainFunction;
  }
  
  /**
   * Insert nodes for global declarations after first node of CFA.
   */
  private void insertGlobalDeclarations(
      final CFAFunctionDefinitionNode cfa, List<IASTDeclaration> globalVars) {
    
    if (globalVars.isEmpty()) {
      return;
    }
    
    // create a series of GlobalDeclarationEdges, one for each declaration,
    // and add them as successors of the input node
    List<CFANode> decls = new LinkedList<CFANode>();
    CFANode cur = new CFANode(0);
    cur.setFunctionName(cfa.getFunctionName());
    decls.add(cur);

    for (IASTDeclaration d : globalVars) {
      assert(d instanceof IASTSimpleDeclaration);
      IASTSimpleDeclaration sd = (IASTSimpleDeclaration)d;
      
      // TODO refactor this
//      if (sd.getDeclarators().length == 1 &&
//          sd.getDeclarators()[0] instanceof IASTFunctionDeclarator) {
//        if (cpaConfig.getBooleanValue("analysis.useFunctionDeclarations")) {
//          // do nothing
//        }
//        else {
//          System.out.println(d.getRawSignature());
//          continue;
//        }
//      }
      
      GlobalDeclarationEdge e = new GlobalDeclarationEdge(
          d.getRawSignature(),
          sd.getDeclarators(),
          sd.getDeclSpecifier());
      CFANode n = new CFANode(0);
      n.setFunctionName(cur.getFunctionName());
      e.initialize(cur, n);
      decls.add(n);
      cur = n;
    }

    // now update the successors of cfa
    for (int i = 0; i < cfa.getNumLeavingEdges(); ++i) {
      CFAEdge e = cfa.getLeavingEdge(i);
      e.setPredecessor(cur);
    }
    
    if (cfa.getLeavingSummaryEdge() != null) {
      cfa.getLeavingSummaryEdge().setPredecessor(cur);
    }
    
    
    // and add a blank edge connecting the first node in decl with cfa
    BlankEdge be = new BlankEdge("INIT GLOBAL VARS");
    be.initialize(cfa, decls.get(0));

    return;
  }
  
  public ReachedElements run(Algorithm pAlgorithm, AbstractElement pInitialElement, Precision pInitialPrecision) throws CPAException {
    
    ReachedElements lReached = null;
    
    try {
      
      lReached = new ReachedElements(mConfiguration.getProperty("analysis.traversal"));
    } catch (IllegalArgumentException e) {
      
      mLogManager.logException(Level.SEVERE, e, "ERROR, unknown traversal option");
      System.exit(1);
    }
    
    lReached.add(pInitialElement, pInitialPrecision);

    run(pAlgorithm, lReached);

    return lReached;
  }
  
  public void run(Algorithm pAlgorithm, ReachedElements pReachedElements) throws CPAException {
    
    assert(pAlgorithm != null);
    assert(pReachedElements != null);
    
    mLogManager.log(Level.FINE, "CPA Algorithm starting ...");
    mStatistics.startAnalysisTimer();
    
    pAlgorithm.run(pReachedElements, mConfiguration.getBooleanValue("analysis.stopAfterError"));
    
    mStatistics.stopAnalysisTimer();
    mLogManager.log(Level.FINE, "CPA Algorithm finished");
  }
  
  public CPAMain.Result runAlgorithm() throws CPAException {

    mLogManager.log(Level.FINE, "Creating CPAs");
      
    ConfigurableProgramAnalysis cpa = CompositeCPA.getCompositeCPA(mMainFunction);

    boolean useART = mConfiguration.getBooleanValue("analysis.useART"); 
    if (useART) {
      cpa = ARTCPA.getARTCPA(mMainFunction, cpa);
    }
          
    if (cpa instanceof CPAWithStatistics) {
      ((CPAWithStatistics)cpa).collectStatistics(mStatistics.getSubStatistics());
    }
      
    // create algorithm
    Algorithm algorithm = new CPAAlgorithm(cpa);
      
    if (mConfiguration.getBooleanValue("analysis.useRefinement")) {
      algorithm = new CEGARAlgorithm(algorithm);
    }
      
    if (mConfiguration.getBooleanValue("analysis.useInvariantDump")) {
      algorithm = new InvariantCollectionAlgorithm(algorithm);
    }
      
    if (mConfiguration.getBooleanValue("analysis.useCBMC")) {
      algorithm = new CBMCAlgorithm(mCFAMap, algorithm);
    }
    
    ReachedElements reached = run(algorithm, cpa.getInitialElement(mMainFunction), cpa.getInitialPrecision(mMainFunction));
    
    if (useART && mConfiguration.getBooleanValue("ART.export")) {
      String outfilePath = mConfiguration.getProperty("output.path");
      String outfileName = mConfiguration.getProperty("ART.file", "ART.dot");
      //if no filename is given, use default value
      CPAMain.dumpPathToDotFile(reached, outfilePath + outfileName);
    }
      
    System.out.println();
    System.out.println(" size of reached set: " + reached.size());
    System.out.println(" number of stops " + CompositeStopOperator.noOfOperations);
      
    if (!mConfiguration.getBooleanValue("analysis.dontPrintReachableStates")) {
      for (AbstractElement e : reached) {
        System.out.println(e);
      }
    }
    
    for (AbstractElement reachedElement : reached) {
      if (reachedElement.isError()) {
        return Result.UNSAFE;
      }
    }
        
    return Result.SAFE;
  }
  
}
