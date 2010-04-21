package fllesh;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.google.common.base.Joiner;

import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFAFunctionDefinitionNode;

import common.configuration.Configuration;
import compositeCPA.CompositeCPA;

import cpa.art.ARTCPA;
import cpa.art.ARTStatistics;
import cpa.common.LogManager;
import cpa.common.ReachedElements;
import cpa.common.CPAcheckerResult.Result;
import cpa.common.algorithm.Algorithm;
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
import fllesh.fql.backend.targetgraph.Edge;
import fllesh.fql.backend.targetgraph.TargetGraph;
import fllesh.fql.fllesh.util.CPAchecker;
import fllesh.fql.fllesh.util.Cilly;
import fllesh.fql.frontend.ast.filter.FunctionCall;

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
      
      // export CFA as .dot file
      /*lWriter.println("cfa.export = true");
      lWriter.println("cfa.file = cfa.dot");
      
      // export final ART as .dot file
      lWriter.println("ART.export = true");
      lWriter.println("ART.file = ART.dot");*/

      //# export error path to file, if one is found
      //cpas.art.errorPath.export = true
      //cpas.art.errorPath.file = ErrorPath.txt

      
      //lWriter.println("log.consoleLevel = ALL");
      
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
  
  //observerAnalysis.inputFile =  test/programs/observerAutomata/PointerAnalysisTestAutomaton.txt
  //observerAnalysis.dotExportFile = observerAutomatonExport.dot

  private static File createPropertiesFile(File pObserverAutomatonFile) {
    File lPropertiesFile = Main.createPropertiesFile();
    
    // append configuration for observer automaton
    PrintWriter lWriter;
    try {
      
      lWriter = new PrintWriter(new FileOutputStream(lPropertiesFile, true));
      
      lWriter.println("observerAnalysis.inputFile = " + pObserverAutomatonFile.getAbsolutePath());
      lWriter.println("observerAnalysis.dotExportFile = observerAutomatonExport.dot");
      lWriter.close();
      
      return lPropertiesFile;
      
    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
    return null;
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
    
    
    EdgeVisitCPA.Factory lFactory = new EdgeVisitCPA.Factory(lMainFunction);
    ConfigurableProgramAnalysis lEdgeVisitCPA = lFactory.createInstance();
    
    for (Entry<CFAEdge, String> lEntry : lFactory.getMapping().entrySet())  {
      System.out.println(lEntry.getKey().toString() + " : " + lEntry.getValue());
    }
    
    
    TargetGraph lTargetGraph = TargetGraph.createTargetGraphFromCFA(lMainFunction);
    FunctionCall lFunctionCallFilter = new FunctionCall("f");
    TargetGraph lFilteredTargetGraph = lTargetGraph.apply(lFunctionCallFilter);
    
    String lId = null;
    
    for (Edge lEdge : lFilteredTargetGraph.getEdges()) {
      lId = lFactory.getId(lEdge.getCFAEdge());
      
      // we do not care about predication at the moment
      System.out.println("GOAL: " + lFactory.getId(lEdge.getCFAEdge()));
      
      break;
    }
    
    File lAutomatonFile = File.createTempFile("fllesh.", ".oa");
    lAutomatonFile.deleteOnExit();
    
    PrintWriter lObserverAutomaton = new PrintWriter(new FileWriter(lAutomatonFile));
    lObserverAutomaton.println("AUTOMATON Goal_" + lId);
    lObserverAutomaton.println("INITIAL STATE Init;");
    lObserverAutomaton.println("STATE Init:");
    lObserverAutomaton.println("CHECK(edgevisit(\"" + lId +  "\")) -> GOTO ERR;");
    lObserverAutomaton.close();
    
    File lExtendedPropertiesFile = Main.createPropertiesFile(lAutomatonFile);
    Configuration lExtendedConfiguration = Main.createConfiguration(lSourceFileName, lExtendedPropertiesFile.getAbsolutePath());
    
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
    
    AbstractElement initialElement = lARTCPA.getInitialElement(lMainFunction);
    Precision initialPrecision = lARTCPA.getInitialPrecision(lMainFunction);
          
    ReachedElements lReachedElements = new ReachedElements(ReachedElements.TraversalMethod.DFS, true);
    lReachedElements.add(initialElement, initialPrecision);
    
    try {
      lAlgorithm.run(lReachedElements, true);      
    } catch (CPAException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
        
    for (AbstractElement reachedElement : lReachedElements) {
      System.out.println(reachedElement);
    }
    
    PrintWriter lStatisticsWriter = new PrintWriter(System.out);

    lARTStatistics.printStatistics(lStatisticsWriter, Result.SAFE, lReachedElements);
    
  }

}

