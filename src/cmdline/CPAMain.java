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
package cmdline;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.eclipse.cdt.core.dom.IASTServiceProvider;
import org.eclipse.cdt.core.dom.ICodeReaderFactory;
import org.eclipse.cdt.core.dom.IASTServiceProvider.UnsupportedDialectException;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.internal.core.dom.InternalASTServiceProvider;
import org.eclipse.core.resources.IFile;

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
import cmdline.stubs.StubConfiguration;
import cmdline.stubs.StubFile;

import common.Pair;
import compositeCPA.CompositeCPA;

import cpa.art.ARTCPA;
import cpa.art.ARTElement;
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
import cpa.symbpredabsCPA.SymbPredAbsAbstractElement;
import cpaplugin.CPAConfiguration;
import cpaplugin.MainCPAStatistics;
import cpaplugin.CPAConfiguration.InvalidCmdlineArgumentException;
import exceptions.CFAGenerationRuntimeException;
import exceptions.CPAException;

@SuppressWarnings("restriction")
public class CPAMain {

  public static CPAConfiguration cpaConfig;
  public static LogManager logManager;
  private final static MainCPAStatistics cpaStats = new MainCPAStatistics();

  // used in the ShutdownHook to check whether the analysis has been
  // interrupted by the user
  private static boolean interrupted = true;
  
  public static enum Result { UNKNOWN, UNSAFE, SAFE };   
  
  private static Result result = Result.UNKNOWN;

  private static class ShutdownHook extends Thread {
    
    private final ReachedElements mReached;
    
    public ShutdownHook(ReachedElements pReached) {
      mReached = pReached;
    }
    
    @Override
    public void run() {
      if (interrupted) {
        cpaStats.stopAnalysisTimer();
        result = Result.UNKNOWN;
      }
       
      System.out.flush();
      System.err.flush();
      cpaStats.printStatistics(new PrintWriter(System.out), result, mReached);
      
      if (interrupted) {
        System.out.println("\n" +
            "***************************************************" +
            "****************************\n" +
            "* WARNING:  Analysis interrupted!! The statistics " +
            "might be unreliable!        *\n" +
            "***************************************************" +
            "****************************\n"
        );
      }
    }
  }

  public static void main(String[] args) {
    // initialize various components
    try {
      cpaConfig = new CPAConfiguration(args);
    } catch (InvalidCmdlineArgumentException e) {
      System.err.println("Could not parse command line arguments: " + e.getMessage());
      System.exit(1);
    } catch (IOException e) {
      System.err.println("Could not read config file " + e.getMessage());
      System.exit(1);
    }
    logManager = LogManager.getInstance();
    
    // get code file name
    String[] names = cpaConfig.getPropertiesArray("analysis.programNames");
    if (names == null) {
      logManager.log(Level.SEVERE, "No code file given!");
      System.exit(1);
    }
    
    if (names.length != 1) {
      logManager.log(Level.SEVERE, 
              "Support for multiple code files is currently not implemented!");
      System.exit(1);
    }
    
    File sourceFile = new File(names[0]);
    if (!sourceFile.exists()) {
      logManager.log(Level.SEVERE, "File", names[0], "does not exist!");
      System.exit(1);
    }
    
    if (!sourceFile.isFile()) {
      logManager.log(Level.SEVERE, "File", names[0], "is not a normal file!");
      System.exit(1);
    }
    
    if (!sourceFile.canRead()) {
      logManager.log(Level.SEVERE, "File", names[0], "is not readable!");
      System.exit(1);
    }

    // run analysis
    CPAchecker(new StubFile(names[0]));
    
    //ensure all logs are written to the outfile
    logManager.flush();
    // statistics are displayed by shutdown hook
  }
  
  public static void CPAchecker(IFile file) {
    logManager.log(Level.FINE, "Analysis Started");
    
    // parse code file
    IASTTranslationUnit ast = parse(file);

    // start measuring time
    cpaStats.startProgramTimer();

    // create CFA
    Pair<CFAMap, CFAFunctionDefinitionNode> cfa = createCFA(ast);
    
    try {
      runAlgorithm(cfa.getFirst(), cfa.getSecond());
      interrupted = false;

    } catch (CPAException e) {
      logManager.logException(Level.SEVERE, e, null);
    }
  }
  
  /**
   * Parse the content of a file into an AST with the Eclipse CDT parser.
   * If an error occurs, the program is halted.
   * 
   * @param fileName  The file to parse.
   * @return The AST.
   */
  public static IASTTranslationUnit parse(IFile file) {
    IASTServiceProvider p = new InternalASTServiceProvider();
    
    ICodeReaderFactory codeReaderFactory = null;
    try {
       codeReaderFactory = createCodeReaderFactory();
    } catch (ClassNotFoundException e) {
      logManager.logException(Level.SEVERE, e, "ClassNotFoundException:" +
          "Missing implementation of ICodeReaderFactory, check your CDT version!");
      System.exit(1);
    }
    
    IASTTranslationUnit ast = null;
    try {
      ast = p.getTranslationUnit(file, codeReaderFactory, new StubConfiguration());
    } catch (UnsupportedDialectException e) {
      logManager.logException(Level.SEVERE, e, "UnsupportedDialectException:" +
          "Unsupported dialect for parser, check parser.dialect option!");
      System.exit(1);
    }

    logManager.log(Level.FINE, "Parser Finished");

    return ast;
  }
  
  /**
   * Get the right StubCodeReaderFactory depending on the current CDT version.
   * @return The correct implementation of ICodeReaderFactory.
   * @throws ClassNotFoundException If no matching factory is found.
   */
  private static ICodeReaderFactory createCodeReaderFactory() throws ClassNotFoundException {
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
  private static CFAFunctionDefinitionNode initCFA(final CFABuilder builder, final CFAMap cfas)
  {
    final Collection<CFAFunctionDefinitionNode> cfasList = cfas.cfaMapIterator();
    String mainFunctionName = CPAMain.cpaConfig.getProperty("analysis.entryFunction", "main");
    
    CFAFunctionDefinitionNode mainFunction = cfas.getCFA(mainFunctionName);
    
    if (mainFunction == null) {
      logManager.log(Level.SEVERE, "Function", mainFunctionName, "not found!");
      System.exit(0);
    }
    
    // simplify CFA
    if (CPAMain.cpaConfig.getBooleanValue("cfa.simplify")) {
      // TODO Erkan Simplify each CFA
      CFASimplifier simplifier = new CFASimplifier();
      simplifier.simplify(mainFunction);
    }

    // Insert call and return edges and build the supergraph
    if (CPAMain.cpaConfig.getBooleanValue("analysis.interprocedural")) {
      logManager.log(Level.FINE, "Analysis is interprocedural, adding super edges");
      
      boolean noExtCalls = CPAMain.cpaConfig.getBooleanValue("analysis.noExternalCalls");
      CPASecondPassBuilder spbuilder = new CPASecondPassBuilder(cfas, noExtCalls);
      
      for (CFAFunctionDefinitionNode cfa : cfasList) {
        spbuilder.insertCallEdges(cfa.getFunctionName());
      }
    }
    
    if (CPAMain.cpaConfig.getBooleanValue("analysis.useGlobalVars")){
      // add global variables at the beginning of main
      
      List<IASTDeclaration> globalVars = builder.getGlobalDeclarations();
      insertGlobalDeclarations(mainFunction, globalVars);
    }
    
    return mainFunction;
  }
  
  
  private static Pair<CFAMap, CFAFunctionDefinitionNode> createCFA(IASTTranslationUnit ast) {

    // Build CFA
    final CFABuilder builder = new CFABuilder();
    try {
      ast.accept(builder);
    } catch (CFAGenerationRuntimeException e) {
      // only log message, not whole exception because this is a C problem,
      // not a CPAchecker problem
      logManager.log(Level.SEVERE, e.getMessage());
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
    if (CPAMain.cpaConfig.getBooleanValue("cfa.removeIrrelevantForErrorLocations")) {
      CFAReduction coi =  new CFAReduction();
      coi.removeIrrelevantForErrorLocations(mainFunction);

      if (mainFunction.getNumLeavingEdges() == 0) {
        CPAMain.logManager.log(Level.INFO, "No error locations reachable from " + mainFunction.getFunctionName()
              + ", analysis not necessary.");
        System.exit(0);
      }
    }
    
    // check the super CFA starting at the main function
    // enable only while debugging/testing
    if(CPAMain.cpaConfig.getBooleanValue("cfa.check")){
      CFACheck.check(mainFunction);
    }

    // write CFA to file
    if (CPAMain.cpaConfig.getBooleanValue("cfa.export")) {
      DOTBuilderInterface dotBuilder = new DOTBuilder();
      
      String cfaFile = CPAMain.cpaConfig.getProperty("cfa.file", "cfa.dot");
      //if no filename is given, use default value
      String path = CPAMain.cpaConfig.getProperty("output.path") + cfaFile;
      try {
        dotBuilder.generateDOT(cfasList, mainFunction,
            new File(path).getPath());
      } catch (IOException e) {
        logManager.logException(Level.WARNING, e,
          "Could not write CFA to dot file, check configuration option cfa.file!");
        // continue with analysis
      }
    }
    
    logManager.log(Level.FINE, "DONE, CFA for", numFunctions, "functions created");

    return new Pair<CFAMap, CFAFunctionDefinitionNode>(cfas, mainFunction);
  }

  /**
   * Insert nodes for global declarations after first node of CFA.
   */
  private static void insertGlobalDeclarations(
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
  
  private static void runAlgorithm(final CFAMap cfas, final CFAFunctionDefinitionNode mainFunction) throws CPAException {
 
      logManager.log(Level.FINE, "Creating CPAs");
      
      ConfigurableProgramAnalysis cpa = CompositeCPA.getCompositeCPA(mainFunction);

      boolean useART = CPAMain.cpaConfig.getBooleanValue("analysis.useART"); 
      if (useART) {
        cpa = ARTCPA.getARTCPA(mainFunction, cpa);
      }
          
      if (cpa instanceof CPAWithStatistics) {
        ((CPAWithStatistics)cpa).collectStatistics(cpaStats.getSubStatistics());
      }
      
      // create algorithm
      Algorithm algorithm = new CPAAlgorithm(cpa);
      
      if (CPAMain.cpaConfig.getBooleanValue("analysis.useRefinement")) {
        algorithm = new CEGARAlgorithm(algorithm);
      }
      
      if (CPAMain.cpaConfig.getBooleanValue("analysis.useInvariantDump")) {
        algorithm = new InvariantCollectionAlgorithm(algorithm);
      }
      
      if (CPAMain.cpaConfig.getBooleanValue("analysis.useCBMC")) {
        algorithm = new CBMCAlgorithm(cfas, algorithm);
      }
      
      AbstractElement initialElement = cpa.getInitialElement(mainFunction);
      Precision initialPrecision = cpa.getInitialPrecision(mainFunction);
      ReachedElements reached = null;
      try {
        reached = new ReachedElements(CPAMain.cpaConfig.getProperty("analysis.traversal"));
      } catch (IllegalArgumentException e) {
        logManager.logException(Level.SEVERE, e, "ERROR, unknown traversal option");
        System.exit(1);
      }
      reached.add(initialElement, initialPrecision);

      
      // this is for catching Ctrl+C and printing statistics even in that
      // case. It might be useful to understand what's going on when
      // the analysis takes a lot of time...
      Runtime.getRuntime().addShutdownHook(new ShutdownHook(reached));

      logManager.log(Level.INFO, "Starting analysis...");
      cpaStats.startAnalysisTimer();

      
      algorithm.run(reached, CPAMain.cpaConfig.getBooleanValue("analysis.stopAfterError"));
      
      cpaStats.stopAnalysisTimer();
      logManager.log(Level.INFO, "Analysis finished.");

      boolean errorFound = false;
      for (AbstractElement reachedElement : reached) {
        if (reachedElement.isError()) {
          errorFound = true;
          result = Result.UNSAFE;
          break;
        }
      }
      if (!errorFound) {
        result = Result.SAFE;
      }

      if (useART && CPAMain.cpaConfig.getBooleanValue("ART.export")) {
        String outfilePath = CPAMain.cpaConfig.getProperty("output.path");
        String outfileName = CPAMain.cpaConfig.getProperty("ART.file", "ART.dot");
        //if no filename is given, use default value
        dumpPathToDotFile(reached, outfilePath + outfileName);
      }
  }
  
  public static void dumpPathToDotFile(ReachedElements pReached, String outfile) {
    ARTElement firstElement = (ARTElement)pReached.getFirstElement();

    Deque<ARTElement> worklist = new LinkedList<ARTElement>();
    Set<Integer> nodesList = new HashSet<Integer>();
    Set<ARTElement> processed = new HashSet<ARTElement>();
    StringBuffer sb = new StringBuffer();
    PrintWriter out;
    try {
      out = new PrintWriter(new File(outfile));
    } catch (FileNotFoundException e) {
      logManager.log(Level.WARNING,
          "Could not write ART to file ", outfile, ", (", e.getMessage(), ")");
      return;
    }
    out.println("digraph ART {");
    out.println("style=filled; color=lightgrey; ");

    worklist.add(firstElement);

    while(worklist.size() != 0){
      ARTElement currentElement = worklist.removeLast();
      if(processed.contains(currentElement)){
        continue;
      }
      processed.add(currentElement);
      if(!nodesList.contains(currentElement.getElementId())){
        String color;
        if (currentElement.isBottom()) {
          color = "black";
        } else if (currentElement.isCovered()) {
          color = "green";
        } else if (currentElement.isError()) {
          color = "red";
        } else {
          SymbPredAbsAbstractElement symbpredabselem = currentElement.retrieveWrappedElement(SymbPredAbsAbstractElement.class);
          if (symbpredabselem != null && symbpredabselem.isAbstractionNode()) {
            color = "blue";
          } else {
            color = "white";
          }
        }

        CFANode loc = currentElement.retrieveLocationElement().getLocationNode();
        String label = (loc==null ? 0 : loc.getNodeNumber()) + "000" + currentElement.getElementId();
        out.println("node [shape = diamond, color = " + color + ", style = filled, label=" + label +"] " + currentElement.getElementId() + ";");
        
        nodesList.add(currentElement.getElementId());
      }
      for(ARTElement child : currentElement.getChildren()){
        CFAEdge edge = getEdgeBetween(currentElement, child);
        sb.append(currentElement.getElementId());
        sb.append(" -> ");
        sb.append(child.getElementId());
        sb.append(" [label=\"");
        sb.append(edge != null ? edge.toString().replace('"', '\'') : "");
        sb.append("\"];\n");
        if(!worklist.contains(child)){
          worklist.add(child);
        }
      }
    }

    out.println(sb.toString());
    out.println("}");
    out.flush();
    out.close();
  }

  public static CFAEdge getEdgeBetween(final ARTElement pCurrentElement,
      ARTElement pChild) {
    CFANode currentLoc = pCurrentElement.retrieveLocationElement().getLocationNode();
    CFAEdge writeEdge = null;
    CFANode childNode = pChild.retrieveLocationElement().getLocationNode();
    if(childNode != null){
      for(int i=0; i<childNode.getNumEnteringEdges(); i++){
        CFAEdge edge = childNode.getEnteringEdge(i);
        if(currentLoc.getNodeNumber() == edge.getPredecessor().getNodeNumber()){
          writeEdge = edge;
        }
      }
    }
    return writeEdge;
  }
}
