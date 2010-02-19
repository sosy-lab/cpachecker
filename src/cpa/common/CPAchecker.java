/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package cpa.common;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.eclipse.cdt.core.dom.IASTServiceProvider;
import org.eclipse.cdt.core.dom.ICodeReaderFactory;
import org.eclipse.cdt.core.dom.IParserConfiguration;
import org.eclipse.cdt.core.dom.IASTServiceProvider.UnsupportedDialectException;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.internal.core.dom.InternalASTServiceProvider;
import org.eclipse.core.resources.IFile;

import cfa.CFABuilder;
import cfa.CFACheck;
import cfa.CFAReduction;
import cfa.CFASimplifier;
import cfa.CFATopologicalSort;
import cfa.CPASecondPassBuilder;
import cfa.DOTBuilder;
import cfa.objectmodel.BlankEdge;
import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFAFunctionDefinitionNode;
import cfa.objectmodel.CFANode;
import cfa.objectmodel.c.GlobalDeclarationEdge;
import cmdline.stubs.StubConfiguration;

import com.google.common.collect.ImmutableMap;
import common.Pair;
import common.configuration.Configuration;
import common.configuration.Option;
import common.configuration.Options;

import cpa.common.algorithm.Algorithm;
import cpa.common.algorithm.AssumptionCollectionAlgorithm;
import cpa.common.algorithm.CBMCAlgorithm;
import cpa.common.algorithm.CEGARAlgorithm;
import cpa.common.algorithm.CPAAlgorithm;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.Statistics;
import cpa.common.interfaces.StatisticsProvider;
import cpa.common.interfaces.Statistics.Result;
import exceptions.CFAGenerationRuntimeException;
import exceptions.CPAException;
import exceptions.ForceStopCPAException;
import exceptions.InvalidConfigurationException;

@SuppressWarnings("restriction")
public class CPAchecker {
  
  @Options
  private static class CPAcheckerOptions {

    @Option(name="parser.dialect", values={"C99", "GNUC"})
    String parserDialect = "C99";

    // CFA creation and initialization options
    
    @Option(name="analysis.entryFunction", regexp="^[_a-zA-Z][_a-zA-Z0-9]*$")
    String mainFunctionName = "main";
    
    @Option(name="cfa.simplify")
    boolean simplifyCfa = false;
    
    @Option(name="cfa.combineBlockStatements")
    boolean combineBlockStatements = false;
    
    @Option(name="cfa.removeDeclarations")
    boolean removeDeclarations = false;
    
    @Option(name="analysis.noExternalCalls")
    boolean noExternalCalls = false;
    
    @Option(name="analysis.interprocedural")
    boolean interprocedural = false;
    
    @Option(name="analysis.useGlobalVars")
    boolean useGlobalVars = false;
    
    @Option(name="cfa.removeIrrelevantForErrorLocations")
    boolean removeIrrelevantForErrorLocations = false;
    
    @Option(name="cfa.check")
    boolean checkCfa = false;
    
    @Option(name="cfa.export")
    boolean exportCfa = true;
    
    @Option(name="cfa.file")
    String exportCfaFile = "cfa.dot";
    
    @Option(name="output.path")
    String outputDirectory = "test/output/";
    
    // algorithm options
    
    @Option(name="analysis.traversal")
    ReachedElements.TraversalMethod traversalMethod = ReachedElements.TraversalMethod.DFS;
    
    @Option(name="cpa.useSpecializedReachedSet")
    boolean locationMappedReachedSet = true;
    
    @Option(name="analysis.useAssumptionCollector")
    boolean useAssumptionCollector = false;
    
    @Option(name="analysis.useRefinement")
    boolean useRefinement = false;
    
    @Option(name="analysis.useCBMC")
    boolean useCBMC = false;

    @Option(name="analysis.stopAfterError")
    boolean stopAfterError = false;
    
  }
  
  // TODO these fields should not be public and static
  // Write access to these fields is prohibited from outside of this class!
  // Use the constructor to initialize them.
  public static Configuration config = null;
  public static LogManager logger = null;
  
  private static volatile boolean requireStopAsap = false;
  private final CPAcheckerOptions options;
  
  /**
   * Return true if the main thread is required to stop
   * working as soon as possible, in response to the user
   * interrupting it.
   */
  public static boolean getRequireStopAsap()
  {
    return requireStopAsap;
  }

  private static class ShutdownHook extends Thread {
    
    private final Statistics mStats;
    private final ReachedElements mReached;
    private final Thread mainThread;
    
    // if still null when run() is executed, analysis has been interrupted by user
    private Result mResult = null;
    
    public ShutdownHook(Statistics pStats, ReachedElements pReached) {
      mStats = pStats; 
      mReached = pReached;
      mainThread = Thread.currentThread();
    }
    
    public void setResult(Result pResult) {
      assert mResult == null;
      mResult = pResult;
    }
    
    // We want to use Thread.stop() to force the main thread to stop
    // when interrupted by the user.
    @SuppressWarnings("deprecation")
    @Override
    public void run() {
      if (mainThread.isAlive()) {
        // probably the user pressed Ctrl+C
        requireStopAsap = true;
        logger.log(Level.INFO, "Stop signal received, waiting 2s for analysis to stop cleanly...");
        try {
          mainThread.join(2000);
        } catch (InterruptedException e) {}
        if (mainThread.isAlive()) {
          logger.log(Level.WARNING, "Analysis did not stop fast enough, forcing it. This might prevent the statistics from being generated.");
          mainThread.stop();
        }
      }

      if (mResult == null) {
        mResult = Result.UNKNOWN;
      }
      
      logger.flush();
      System.out.flush();
      System.err.flush();
      mStats.printStatistics(new PrintWriter(System.out), mResult, mReached);
    }
  }
  
  public CPAchecker(Configuration pConfiguration, LogManager pLogManager) throws InvalidConfigurationException {
    // currently only one instance is possible due to these static fields 
    assert config == null;
    assert logger == null;
    
    config = pConfiguration;
    logger = pLogManager;

    options = new CPAcheckerOptions();
    config.inject(options);
  }
  
  public void run(IFile file) {
    
    logger.log(Level.FINE, "Analysis Started");
    
    // parse code file
    IASTTranslationUnit ast = parse(file);

    try {
      MainCPAStatistics stats = new MainCPAStatistics(config);

      // start measuring time
      stats.startProgramTimer();
  
      // create CFA
      Pair<Map<String, CFAFunctionDefinitionNode>, CFAFunctionDefinitionNode> cfa = createCFA(ast);
      Map<String, CFAFunctionDefinitionNode> cfas = cfa.getFirst();
      CFAFunctionDefinitionNode mainFunction = cfa.getSecond();
    
      ConfigurableProgramAnalysis cpa = createCPA(stats);
      
      Algorithm algorithm = createAlgorithm(cfas, cpa, stats);
      
      ReachedElements reached = createInitialReachedSet(cpa, mainFunction);
      
      runAlgorithm(algorithm, reached, stats);
    
    } catch (InvalidConfigurationException e) {
      logger.log(Level.SEVERE, "Invalid configuration:", e.getMessage());
      
    } catch (ForceStopCPAException e) {
      // CPA must exit because it is asked to by the shutdown hook
      logger.log(Level.FINE, "ForceStopCPAException caught at top level: CPAchecker has stopped forcefully, but cleanly");
    } catch (CPAException e) {
      logger.logException(Level.SEVERE, e, null);
    }
    
    // statistics are displayed by shutdown hook
  }
  
  /**
   * Parse the content of a file into an AST with the Eclipse CDT parser.
   * If an error occurs, the program is halted.
   * 
   * @param fileName  The file to parse.
   * @return The AST.
   */
  public IASTTranslationUnit parse(IFile file) {
    IASTServiceProvider p = new InternalASTServiceProvider();
    
    ICodeReaderFactory codeReaderFactory = null;
    try {
       codeReaderFactory = createCodeReaderFactory();
    } catch (ClassNotFoundException e) {
      logger.logException(Level.SEVERE, e, "ClassNotFoundException:" +
          "Missing implementation of ICodeReaderFactory, check your CDT version!");
      System.exit(1);
    }
    
    IASTTranslationUnit ast = null;
    try {
      IParserConfiguration parserConfiguration = new StubConfiguration(options.parserDialect);
      ast = p.getTranslationUnit(file, codeReaderFactory, parserConfiguration);
    } catch (UnsupportedDialectException e) {
      logger.logException(Level.SEVERE, e, "UnsupportedDialectException:" +
          "Unsupported dialect for parser, check parser.dialect option!");
      System.exit(1);
    }

    logger.log(Level.FINE, "Parser Finished");

    return ast;
  }
  
  /**
   * Get the right StubCodeReaderFactory depending on the current CDT version.
   * @return The correct implementation of ICodeReaderFactory.
   * @throws ClassNotFoundException If no matching factory is found.
   */
  private ICodeReaderFactory createCodeReaderFactory() throws ClassNotFoundException {
    ClassLoader classLoader = ClassLoader.getSystemClassLoader();
    
    String factoryClassName;
    // determine CDT version by trying to load the IMacroCollector class which
    // only exists in CDT 4
    try {
      classLoader.loadClass("org.eclipse.cdt.core.dom.IMacroCollector");
      
      // CDT 4.0
      factoryClassName = "cmdline.stubs.StubCodeReaderFactoryCDT4";
    } catch (ClassNotFoundException e) {
      // not CDT 4.0
      factoryClassName = "cmdline.stubs.StubCodeReaderFactory";
    }

    // try to load factory class and execute the static getInstance() method
    try {
      Class<?> factoryClass = classLoader.loadClass(factoryClassName);
      Object factoryObject = factoryClass.getMethod("getInstance", (Class<?>[]) null)
                                                                  .invoke(null);
      
      return (ICodeReaderFactory) factoryObject;
    } catch (Exception e) {
      // simply wrap all possible exceptions in a ClassNotFoundException
      // this will terminate the program
      throw new ClassNotFoundException("Exception while instantiating " + factoryClassName, e);
    }
  }
  
  /**
   * --Refactoring:
   * Initializes the CFA. This method is created based on the 
   * "extract method refactoring technique" to help simplify the createCFA method body.
   * @param builder
   * @param cfas
   * @return
   */
  private CFAFunctionDefinitionNode initCFA(final CFABuilder builder, final Map<String, CFAFunctionDefinitionNode> cfas)
  {
    CFAFunctionDefinitionNode mainFunction = cfas.get(options.mainFunctionName);
    
    if (mainFunction == null) {
      logger.log(Level.SEVERE, "Function", options.mainFunctionName, "not found!");
      System.exit(0);
    }
    
    // simplify CFA
    if (options.simplifyCfa) {
      // TODO Erkan Simplify each CFA
      CFASimplifier simplifier = new CFASimplifier(options.combineBlockStatements, options.removeDeclarations);
      simplifier.simplify(mainFunction);
    }

    // Insert call and return edges and build the supergraph
    if (options.interprocedural) {
      logger.log(Level.FINE, "Analysis is interprocedural, adding super edges");
      
      CPASecondPassBuilder spbuilder = new CPASecondPassBuilder(cfas, options.noExternalCalls);
      Set<String> calledFunctions = spbuilder.insertCallEdgesRecursively(options.mainFunctionName);
      
      // remove all functions which are never reached from cfas
      cfas.keySet().retainAll(calledFunctions);
    }
    
    if (options.useGlobalVars){
      // add global variables at the beginning of main
      
      List<IASTDeclaration> globalVars = builder.getGlobalDeclarations();
      insertGlobalDeclarations(mainFunction, globalVars);
    }
    
    return mainFunction;
  }
  
  
  protected Pair<Map<String, CFAFunctionDefinitionNode>, CFAFunctionDefinitionNode> createCFA(IASTTranslationUnit ast) {

    // Build CFA
    final CFABuilder builder = new CFABuilder();
    try {
      ast.accept(builder);
    } catch (CFAGenerationRuntimeException e) {
      // only log message, not whole exception because this is a C problem,
      // not a CPAchecker problem
      logger.log(Level.SEVERE, e.getMessage());
      System.exit(0);
    }
    final Map<String, CFAFunctionDefinitionNode> cfas = builder.getCFAs();
    
    // annotate CFA nodes with topological information for later use
    for(CFAFunctionDefinitionNode cfa : cfas.values()){
      CFATopologicalSort topSort = new CFATopologicalSort();
      topSort.topologicalSort(cfa);
    }
    
    // --Refactoring:
    CFAFunctionDefinitionNode mainFunction = initCFA(builder, cfas);
    
    // --Refactoring: The following commented section does not affect the actual 
    //                execution of the code
    
    // check the CFA of each function
    // enable only while debugging/testing
//    if(CPAMain.cpaConfig.getBooleanValue("cfa.check")){
//      for(CFAFunctionDefinitionNode cfa : cfasList){
//        CFACheck.check(cfa);
//      }
//    }

    // --Refactoring: The following section was relocated to after the "initCFA" method 
    
    // remove irrelevant locations
    if (options.removeIrrelevantForErrorLocations) {
      CFAReduction coi =  new CFAReduction();
      coi.removeIrrelevantForErrorLocations(mainFunction);

      if (mainFunction.getNumLeavingEdges() == 0) {
        CPAchecker.logger.log(Level.INFO, "No error locations reachable from " + mainFunction.getFunctionName()
              + ", analysis not necessary.");
        System.exit(0);
      }
    }
    
    // check the super CFA starting at the main function
    // enable only while debugging/testing
    if (options.checkCfa){
      CFACheck.check(mainFunction);
    }

    // write CFA to file
    if (options.exportCfa) {
      DOTBuilder dotBuilder = new DOTBuilder();
      File cfaFile = new File(options.outputDirectory, options.exportCfaFile);
      
      try {
        dotBuilder.generateDOT(cfas.values(), mainFunction, cfaFile);
      } catch (IOException e) {
        logger.log(Level.WARNING,
          "Could not write CFA to dot file, check configuration option cfa.file! (",
          e.getMessage() + ")");
        // continue with analysis
      }
    }
    
    logger.log(Level.FINE, "DONE, CFA for", cfas.size(), "functions created");

    return new Pair<Map<String, CFAFunctionDefinitionNode>, CFAFunctionDefinitionNode>(
        ImmutableMap.copyOf(cfas), mainFunction);
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
  
  private void runAlgorithm(final Algorithm algorithm,
          final ReachedElements reached,
          final MainCPAStatistics stats) throws CPAException {
     
    // this is for catching Ctrl+C and printing statistics even in that
    // case. It might be useful to understand what's going on when
    // the analysis takes a lot of time...
    ShutdownHook shutdownHook = new ShutdownHook(stats, reached);
    Runtime.getRuntime().addShutdownHook(shutdownHook);

    logger.log(Level.INFO, "Starting analysis...");
    stats.startAnalysisTimer();
    
    algorithm.run(reached, options.stopAfterError);
    
    stats.stopAnalysisTimer();
    logger.log(Level.INFO, "Analysis finished.");

    Result result = Result.UNKNOWN;
    for (AbstractElement reachedElement : reached) {
      if (reachedElement.isError()) {
        result = Result.UNSAFE;
        break;
      }
    }
    if (result == Result.UNKNOWN) {
      result = Result.SAFE;
    }
    
    shutdownHook.setResult(result);
  }

  private ConfigurableProgramAnalysis createCPA(MainCPAStatistics stats) throws CPAException {
    logger.log(Level.FINE, "Creating CPAs");
    
    CPABuilder builder = new CPABuilder(config, logger);
    ConfigurableProgramAnalysis cpa = builder.buildCPAs();

    if (cpa instanceof StatisticsProvider) {
      ((StatisticsProvider)cpa).collectStatistics(stats.getSubStatistics());
    }
    return cpa;
  }
  
  private Algorithm createAlgorithm(final Map<String, CFAFunctionDefinitionNode> cfas,
      final ConfigurableProgramAnalysis cpa, MainCPAStatistics stats) throws CPAException {
    logger.log(Level.FINE, "Creating algorithms");

    Algorithm algorithm = new CPAAlgorithm(cpa);
    
    if (options.useRefinement) {
      algorithm = new CEGARAlgorithm(algorithm);
    }
    
    if (options.useAssumptionCollector) {
      algorithm = new AssumptionCollectionAlgorithm(algorithm);
    }
    
    if (options.useCBMC) {
      algorithm = new CBMCAlgorithm(cfas, algorithm);
    }
    
    if (algorithm instanceof StatisticsProvider) {
      ((StatisticsProvider)algorithm).collectStatistics(stats.getSubStatistics());
    }
    return algorithm;
  }


  private ReachedElements createInitialReachedSet(
      final ConfigurableProgramAnalysis cpa,
      final CFAFunctionDefinitionNode mainFunction) {
    logger.log(Level.FINE, "Creating initial reached set");
    
    AbstractElement initialElement = cpa.getInitialElement(mainFunction);
    Precision initialPrecision = cpa.getInitialPrecision(mainFunction);
    ReachedElements reached = new ReachedElements(options.traversalMethod, options.locationMappedReachedSet);
    reached.add(initialElement, initialPrecision);
    return reached;
  }
}
