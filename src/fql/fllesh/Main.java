package fql.fllesh;

import java.io.File;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import cfa.DOTBuilder;
import cfa.objectmodel.CFAFunctionDefinitionNode;
import cmdline.CPAMain;

import common.Pair;

import cpa.common.CPAConfiguration;
import cpa.common.LogManager;
import fql.backend.pathmonitor.Automaton;
import fql.backend.query.QueryEvaluation;
import fql.backend.targetgraph.Node;
import fql.backend.targetgraph.TargetGraph;
import fql.backend.testgoals.CoverageSequence;
import fql.backend.testgoals.TestGoal;
import fql.fllesh.cpa.AddSelfLoop;
import fql.fllesh.util.CPAchecker;
import fql.fllesh.util.Cilly;
import fql.frontend.ast.query.Query;
import fql.frontend.parser.FQLParser;

public class Main {

  private static final String mConfig = "-config";
  private static final String mPropertiesFile = "test/config/simpleMustMayAnalysis.properties";
  
  /**
   * @param pArguments
   * @throws Exception 
   */
  public static void main(String[] pArguments) throws Exception {
    assert(pArguments != null);
    assert(pArguments.length > 1);
    
    String[] lArguments = new String[3];
    
    lArguments[0] = mConfig;
    lArguments[1] = mPropertiesFile;
    
    // check cilly invariance of source file, i.e., is it changed when preprocessed by cilly?
    Cilly lCilly = new Cilly();
    
    String lSourceFileName = pArguments[1];
    
    if (!lCilly.isCillyInvariant(pArguments[1])) {
      File lCillyProcessedFile = lCilly.cillyfy(pArguments[1]);
      
      lSourceFileName = lCillyProcessedFile.getAbsolutePath();
      
      System.err.println("WARNING: Given source file is not CIL invariant ... did preprocessing!");
    }
    
    // set source file name
    lArguments[2] = lSourceFileName;
    
    CPAConfiguration lConfiguration = CPAMain.createConfiguration(lArguments);

    LogManager lLogManager = new LogManager(lConfiguration);
      
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager);
    
    Query lQuery = parseQuery(pArguments[0]);
    
    CFAFunctionDefinitionNode lMainFunction = lCPAchecker.getMainFunction();
    
    TargetGraph lTargetGraph = TargetGraph.createTargetGraphFromCFA(lMainFunction);
    
    Pair<CoverageSequence, Automaton> lQueryEvaluation = QueryEvaluation.evaluate(lQuery, lTargetGraph);
    
    Automaton lPassingMonitor = lQueryEvaluation.getSecond();
    
    List<Pair<Automaton, Set<? extends TestGoal>>> lTargetSequence = new LinkedList<Pair<Automaton, Set<? extends TestGoal>>>();
    
    CoverageSequence lCoverageSequence = lQueryEvaluation.getFirst();
    
    for (Pair<Automaton, Set<? extends TestGoal>> lPair : lCoverageSequence) {
      lTargetSequence.add(lPair);
    }
    
    
    // add self loops to CFA
    AddSelfLoop.addSelfLoops(lMainFunction);
    
    
    // TODO remove this output code
    DOTBuilder dotBuilder = new DOTBuilder();
    dotBuilder.generateDOT(lCPAchecker.getCFAMap().values(), lMainFunction, new File("/tmp/mycfa.dot"));
    
    
    Node lProgramEntry = new Node(lMainFunction);
    Node lProgramExit = new Node(lMainFunction.getExitNode());
    
    lTargetSequence.add(new Pair<Automaton, Set<? extends TestGoal>>(lCoverageSequence.getFinalMonitor(), Collections.singleton(lProgramExit)));
    
    FeasibilityCheck lFeasibilityCheck = new FeasibilityCheck();
    
    Set<FeasibilityWitness> lWitnesses = TestGoalEnumeration.run(lTargetSequence, lPassingMonitor, lProgramEntry, lFeasibilityCheck);
    
    generateTestCases(lWitnesses);
  }
  
  private static void generateTestCases(Set<FeasibilityWitness> pWitnesses) {
    // TODO: implement test case generation mechanism
  }
  
  private static Query parseQuery(String pFQLQuery) throws Exception {
    FQLParser lParser = new FQLParser(pFQLQuery);
    
    Object pParseResult;
    
    try {
      pParseResult = lParser.parse().value;
    }
    catch (Exception e) {
      System.out.println(pFQLQuery);
      
      throw e;
    }
    
    assert(pParseResult instanceof Query);
    
    return (Query)pParseResult;
  }

}
