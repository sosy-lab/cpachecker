package fql.backend.targetgraph;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Set;

import org.junit.Test;

import cmdline.CPAMain;
import cmdline.CPAchecker;
import cmdline.CPAMain.InvalidCmdlineArgumentException;
import cpa.common.CPAConfiguration;
import cpa.common.LogManager;
import cpa.common.MainCPAStatistics;
import exceptions.CPAException;
import fql.backend.testgoals.TestGoal;
import fql.frontend.ast.coverage.Edges;
import fql.frontend.ast.coverage.Paths;
import fql.frontend.ast.coverage.States;
import fql.frontend.ast.filter.Filter;
import fql.frontend.ast.filter.Function;
import fql.frontend.ast.filter.FunctionCall;
import fql.frontend.ast.filter.FunctionCalls;
import fql.frontend.ast.filter.FunctionEntry;
import fql.frontend.ast.filter.Identity;
import fql.frontend.ast.filter.Line;
import fql.frontend.ast.predicate.CIdentifier;
import fql.frontend.ast.predicate.NaturalNumber;
import fql.frontend.ast.predicate.Predicate;
import fql.frontend.ast.predicate.Predicates;

public class TargetGraphTest {
  private String mConfig = "-config";
  private String mPropertiesFile = "test/config/simpleMustMayAnalysis.properties";
  
  @Test
  public void test_01() throws InvalidCmdlineArgumentException, IOException, CPAException {
    String[] lArguments = new String[3];
    
    lArguments[0] = mConfig;
    lArguments[1] = mPropertiesFile;
    lArguments[2] = "test/tests/single/functionCall.c";
    
    CPAConfiguration lConfiguration = CPAMain.createConfiguration(lArguments);
    
    // necessary for LogManager
    CPAMain.cpaConfig = lConfiguration;
    
    LogManager lLogManager = LogManager.getInstance();
      
    MainCPAStatistics lStatistics = new MainCPAStatistics();
    
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager, lStatistics);
    
    TargetGraph lTargetGraph = TargetGraph.createTargetGraphFromCFA(lCPAchecker.getMainFunction());
    
    System.out.println(lTargetGraph);
  }
  
  @Test
  public void test_02() throws InvalidCmdlineArgumentException, IOException, CPAException {
    String[] lArguments = new String[3];
    
    lArguments[0] = mConfig;
    lArguments[1] = mPropertiesFile;
    lArguments[2] = "test/tests/single/loop1.c";
        
    CPAConfiguration lConfiguration = CPAMain.createConfiguration(lArguments);
    
    // necessary for LogManager
    CPAMain.cpaConfig = lConfiguration;
    
    LogManager lLogManager = LogManager.getInstance();
      
    MainCPAStatistics lStatistics = new MainCPAStatistics();
    
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager, lStatistics);
    
    TargetGraph lTargetGraph = TargetGraph.createTargetGraphFromCFA(lCPAchecker.getMainFunction());
    
    System.out.println(lTargetGraph);
  }
  
  @Test
  public void test_03() throws InvalidCmdlineArgumentException, IOException, CPAException {
    String[] lArguments = new String[3];
    
    lArguments[0] = mConfig;
    lArguments[1] = mPropertiesFile;
    lArguments[2] = "test/tests/single/uninitVars.cil.c";
        
    /*
     * Note: This analysis returns most of the time
     * bottom elements for the must analysis since
     * it can not handle pointers at the moment.
     */
    
    CPAConfiguration lConfiguration = CPAMain.createConfiguration(lArguments);
    
    // necessary for LogManager
    CPAMain.cpaConfig = lConfiguration;
    
    LogManager lLogManager = LogManager.getInstance();
      
    MainCPAStatistics lStatistics = new MainCPAStatistics();
    
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager, lStatistics);
    
    TargetGraph lTargetGraph = TargetGraph.createTargetGraphFromCFA(lCPAchecker.getMainFunction());
    
    System.out.println(lTargetGraph);
  }

  @Test
  public void test_04() throws InvalidCmdlineArgumentException, IOException, CPAException {
    String[] lArguments = new String[3];
    
    lArguments[0] = mConfig;
    lArguments[1] = mPropertiesFile;
    lArguments[2] = "test/tests/single/uninitVars.cil.c";
        
    /*
     * Note: This analysis returns most of the time
     * bottom elements for the must analysis since
     * it can not handle pointers at the moment.
     */
    
    CPAConfiguration lConfiguration = CPAMain.createConfiguration(lArguments);
    
    // necessary for LogManager
    CPAMain.cpaConfig = lConfiguration;
    
    LogManager lLogManager = LogManager.getInstance();
      
    MainCPAStatistics lStatistics = new MainCPAStatistics();
    
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager, lStatistics);
    
    TargetGraph lTargetGraph = TargetGraph.createTargetGraphFromCFA(lCPAchecker.getMainFunction());
    
    TargetGraph lFilteredTargetGraph = TargetGraph.applyFunctionNameFilter(lTargetGraph, "func");
    
    System.out.println(lFilteredTargetGraph);
  }  
  
  @Test
  public void test_05() throws InvalidCmdlineArgumentException, IOException, CPAException {
    String[] lArguments = new String[3];
    
    lArguments[0] = mConfig;
    lArguments[1] = mPropertiesFile;
    lArguments[2] = "test/tests/single/uninitVars.cil.c";
        
    /*
     * Note: This analysis returns most of the time
     * bottom elements for the must analysis since
     * it can not handle pointers at the moment.
     */
    
    CPAConfiguration lConfiguration = CPAMain.createConfiguration(lArguments);
    
    // necessary for LogManager
    CPAMain.cpaConfig = lConfiguration;
    
    LogManager lLogManager = LogManager.getInstance();
      
    MainCPAStatistics lStatistics = new MainCPAStatistics();
    
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager, lStatistics);
    
    TargetGraph lTargetGraph = TargetGraph.createTargetGraphFromCFA(lCPAchecker.getMainFunction());
    
    TargetGraph lFuncTargetGraph = TargetGraph.applyFunctionNameFilter(lTargetGraph, "func");
    TargetGraph lF2TargetGraph = TargetGraph.applyFunctionNameFilter(lTargetGraph, "f2");
    
    TargetGraph lUnionGraph = TargetGraph.applyUnionFilter(lFuncTargetGraph, lF2TargetGraph);
    
    System.out.println(lUnionGraph);
  }  
  
  @Test
  public void test_06() throws InvalidCmdlineArgumentException, IOException, CPAException {
    String[] lArguments = new String[3];
    
    lArguments[0] = mConfig;
    lArguments[1] = mPropertiesFile;
    lArguments[2] = "test/tests/single/uninitVars.cil.c";
        
    /*
     * Note: This analysis returns most of the time
     * bottom elements for the must analysis since
     * it can not handle pointers at the moment.
     */
    
    CPAConfiguration lConfiguration = CPAMain.createConfiguration(lArguments);
    
    // necessary for LogManager
    CPAMain.cpaConfig = lConfiguration;
    
    LogManager lLogManager = LogManager.getInstance();
      
    MainCPAStatistics lStatistics = new MainCPAStatistics();
    
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager, lStatistics);
    
    TargetGraph lTargetGraph = TargetGraph.createTargetGraphFromCFA(lCPAchecker.getMainFunction());
    
    TargetGraph lFuncTargetGraph = TargetGraph.applyFunctionNameFilter(lTargetGraph, "func");
    
    TargetGraph lIntersectionGraph = TargetGraph.applyIntersectionFilter(lTargetGraph, lFuncTargetGraph);
    
    System.out.println(lIntersectionGraph);
  }  
  
  @Test
  public void test_07() throws InvalidCmdlineArgumentException, IOException, CPAException {
    String[] lArguments = new String[3];
    
    lArguments[0] = mConfig;
    lArguments[1] = mPropertiesFile;
    lArguments[2] = "test/tests/single/functionCall.c";
    
    CPAConfiguration lConfiguration = CPAMain.createConfiguration(lArguments);
    
    // necessary for LogManager
    CPAMain.cpaConfig = lConfiguration;
    
    LogManager lLogManager = LogManager.getInstance();
      
    MainCPAStatistics lStatistics = new MainCPAStatistics();
    
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager, lStatistics);
    
    TargetGraph lTargetGraph = TargetGraph.createTargetGraphFromCFA(lCPAchecker.getMainFunction());
    
    TargetGraph lFuncTargetGraph = TargetGraph.applyFunctionNameFilter(lTargetGraph, "f");
    
    TargetGraph lMinusGraph = TargetGraph.applyMinusFilter(lTargetGraph, lFuncTargetGraph);
    
    System.out.println(lMinusGraph);
  }
  
  @Test
  public void test_08() throws InvalidCmdlineArgumentException, IOException, CPAException {
    String[] lArguments = new String[3];
    
    lArguments[0] = mConfig;
    lArguments[1] = mPropertiesFile;
    lArguments[2] = "test/tests/single/functionCall.c";
    
    CPAConfiguration lConfiguration = CPAMain.createConfiguration(lArguments);
    
    // necessary for LogManager
    CPAMain.cpaConfig = lConfiguration;
    
    LogManager lLogManager = LogManager.getInstance();
      
    MainCPAStatistics lStatistics = new MainCPAStatistics();
    
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager, lStatistics);
    
    TargetGraph lTargetGraph = TargetGraph.createTargetGraphFromCFA(lCPAchecker.getMainFunction());
    
    TargetGraph lFuncTargetGraph = TargetGraph.applyFunctionNameFilter(lTargetGraph, "f");
    
    TargetGraph lPredicatedGraph = TargetGraph.applyPredication(lFuncTargetGraph, new Predicate(new CIdentifier("x"), Predicate.Comparison.LESS, new NaturalNumber(100)));
    
    System.out.println(lPredicatedGraph);
  }
  
  @Test
  public void test_09() throws InvalidCmdlineArgumentException, IOException, CPAException {
    String[] lArguments = new String[3];
    
    lArguments[0] = mConfig;
    lArguments[1] = mPropertiesFile;
    lArguments[2] = "test/tests/single/functionCall.c";
    
    CPAConfiguration lConfiguration = CPAMain.createConfiguration(lArguments);
    
    // necessary for LogManager
    CPAMain.cpaConfig = lConfiguration;
    
    LogManager lLogManager = LogManager.getInstance();
      
    MainCPAStatistics lStatistics = new MainCPAStatistics();
    
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager, lStatistics);
    
    TargetGraph lTargetGraph = TargetGraph.createTargetGraphFromCFA(lCPAchecker.getMainFunction());
    
    TargetGraph lFilteredTargetGraph = lTargetGraph.apply(Identity.getInstance());
    
    // identity returns the (physically) same target graph
    assertTrue(lFilteredTargetGraph == lTargetGraph);
  }
  
  @Test
  public void test_10() throws InvalidCmdlineArgumentException, IOException, CPAException {
    String[] lArguments = new String[3];
    
    lArguments[0] = mConfig;
    lArguments[1] = mPropertiesFile;
    lArguments[2] = "test/tests/single/functionCall.c";
    
    CPAConfiguration lConfiguration = CPAMain.createConfiguration(lArguments);
    
    // necessary for LogManager
    CPAMain.cpaConfig = lConfiguration;
    
    LogManager lLogManager = LogManager.getInstance();
      
    MainCPAStatistics lStatistics = new MainCPAStatistics();
    
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager, lStatistics);
    
    TargetGraph lTargetGraph = TargetGraph.createTargetGraphFromCFA(lCPAchecker.getMainFunction());
    
    Function lFunctionFilter = new Function("f");
    
    TargetGraph lFilteredTargetGraph1 = lTargetGraph.apply(lFunctionFilter);
    
    TargetGraph lFilteredTargetGraph2 = lTargetGraph.apply(lFunctionFilter);
    
    // caching should return in the same target graphs
    assertTrue(lFilteredTargetGraph1 == lFilteredTargetGraph2);
    
    Function lFunctionFilter2 = new Function("f");
    
    TargetGraph lFilteredTargetGraph3 = lTargetGraph.apply(lFunctionFilter2);
    
    // caching should also work with logically equal filters
    assertTrue(lFilteredTargetGraph1 == lFilteredTargetGraph3);
    
    Function lFunctionFilter3 = new Function("foo");
    
    TargetGraph lFilteredTargetGraph4 = lTargetGraph.apply(lFunctionFilter3);
    
    // a different function name filter should return in a different target graph
    assertFalse(lFilteredTargetGraph3.equals(lFilteredTargetGraph4));
  }
  
  @Test
  public void test_11() throws InvalidCmdlineArgumentException, IOException, CPAException {
    String[] lArguments = new String[3];
    
    lArguments[0] = mConfig;
    lArguments[1] = mPropertiesFile;
    lArguments[2] = "test/tests/single/functionCall.c";
    
    CPAConfiguration lConfiguration = CPAMain.createConfiguration(lArguments);
    
    // necessary for LogManager
    CPAMain.cpaConfig = lConfiguration;
    
    LogManager lLogManager = LogManager.getInstance();
      
    MainCPAStatistics lStatistics = new MainCPAStatistics();
    
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager, lStatistics);
    
    TargetGraph lTargetGraph = TargetGraph.createTargetGraphFromCFA(lCPAchecker.getMainFunction());
    
    FunctionCall lFunctionCallFilter = new FunctionCall("f");
    
    TargetGraph lFilteredTargetGraph = lTargetGraph.apply(lFunctionCallFilter);
    
    System.out.println(lFilteredTargetGraph);
    
    // check caching
    assertTrue(lFilteredTargetGraph == lTargetGraph.apply(lFunctionCallFilter));
  }
  
  @Test
  public void test_12() throws InvalidCmdlineArgumentException, IOException, CPAException {
    String[] lArguments = new String[3];
    
    lArguments[0] = mConfig;
    lArguments[1] = mPropertiesFile;
    lArguments[2] = "test/tests/single/uninitVars.cil.c";
    
    CPAConfiguration lConfiguration = CPAMain.createConfiguration(lArguments);
    
    // necessary for LogManager
    CPAMain.cpaConfig = lConfiguration;
    
    LogManager lLogManager = LogManager.getInstance();
      
    MainCPAStatistics lStatistics = new MainCPAStatistics();
    
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager, lStatistics);
    
    TargetGraph lTargetGraph = TargetGraph.createTargetGraphFromCFA(lCPAchecker.getMainFunction());
    
    FunctionCall lFunctionCallFilter = new FunctionCall("func");
    
    TargetGraph lFilteredTargetGraph = lTargetGraph.apply(lFunctionCallFilter);
    
    System.out.println(lFilteredTargetGraph);
    
    // check caching
    assertTrue(lFilteredTargetGraph == lTargetGraph.apply(lFunctionCallFilter));
    
    FunctionCall lFunctionCallFilter2 = new FunctionCall("func");
    
    // caching should also work with logically equal filters
    assertTrue(lFilteredTargetGraph == lTargetGraph.apply(lFunctionCallFilter2));
  }
  
  @Test
  public void test_13() throws InvalidCmdlineArgumentException, IOException, CPAException {
    String[] lArguments = new String[3];
    
    lArguments[0] = mConfig;
    lArguments[1] = mPropertiesFile;
    lArguments[2] = "test/tests/single/uninitVars.cil.c";
    
    CPAConfiguration lConfiguration = CPAMain.createConfiguration(lArguments);
    
    // necessary for LogManager
    CPAMain.cpaConfig = lConfiguration;
    
    LogManager lLogManager = LogManager.getInstance();
      
    MainCPAStatistics lStatistics = new MainCPAStatistics();
    
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager, lStatistics);
    
    TargetGraph lTargetGraph = TargetGraph.createTargetGraphFromCFA(lCPAchecker.getMainFunction());
    
    Filter lFunctionCallsFilter = FunctionCalls.getInstance();
    
    TargetGraph lFilteredTargetGraph = lTargetGraph.apply(lFunctionCallsFilter);
    
    System.out.println(lFilteredTargetGraph);
    
    // check caching
    assertTrue(lFilteredTargetGraph == lTargetGraph.apply(lFunctionCallsFilter));
  }
  
  @Test
  public void test_14() throws InvalidCmdlineArgumentException, IOException, CPAException {
    String[] lArguments = new String[3];
    
    lArguments[0] = mConfig;
    lArguments[1] = mPropertiesFile;
    lArguments[2] = "test/tests/single/uninitVars.cil.c";
    
    CPAConfiguration lConfiguration = CPAMain.createConfiguration(lArguments);
    
    // necessary for LogManager
    CPAMain.cpaConfig = lConfiguration;
    
    LogManager lLogManager = LogManager.getInstance();
      
    MainCPAStatistics lStatistics = new MainCPAStatistics();
    
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager, lStatistics);
    
    TargetGraph lTargetGraph = TargetGraph.createTargetGraphFromCFA(lCPAchecker.getMainFunction());
    
    Filter lFunctionEntryFilter = new FunctionEntry("func");
    
    TargetGraph lFilteredTargetGraph = lTargetGraph.apply(lFunctionEntryFilter);
    
    System.out.println(lFilteredTargetGraph);
    
    // check caching
    assertTrue(lFilteredTargetGraph == lTargetGraph.apply(lFunctionEntryFilter));
    
    Filter lFunctionEntryFilter2 = new FunctionEntry("func");
    
    // caching should also work with logically equal filters
    assertTrue(lFilteredTargetGraph == lTargetGraph.apply(lFunctionEntryFilter2));
  }
  
  @Test
  public void test_15() throws InvalidCmdlineArgumentException, IOException, CPAException {
    String[] lArguments = new String[3];
    
    lArguments[0] = mConfig;
    lArguments[1] = mPropertiesFile;
    lArguments[2] = "test/tests/single/uninitVars.cil.c";
    
    CPAConfiguration lConfiguration = CPAMain.createConfiguration(lArguments);
    
    // necessary for LogManager
    CPAMain.cpaConfig = lConfiguration;
    
    LogManager lLogManager = LogManager.getInstance();
      
    MainCPAStatistics lStatistics = new MainCPAStatistics();
    
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager, lStatistics);
    
    TargetGraph lTargetGraph = TargetGraph.createTargetGraphFromCFA(lCPAchecker.getMainFunction());
    
    Filter lLineFilter = new Line(102);
    
    TargetGraph lFilteredTargetGraph = lTargetGraph.apply(lLineFilter);
    
    System.out.println(lFilteredTargetGraph);
    
    // check caching
    assertTrue(lFilteredTargetGraph == lTargetGraph.apply(lLineFilter));
    
    Filter lLineFilter2 = new Line(102);
    
    // caching should also work with logically equal filters
    assertTrue(lFilteredTargetGraph == lTargetGraph.apply(lLineFilter2));
  }
  
  @Test
  public void test_16() throws InvalidCmdlineArgumentException, IOException, CPAException {
    String[] lArguments = new String[3];
    
    lArguments[0] = mConfig;
    lArguments[1] = mPropertiesFile;
    lArguments[2] = "test/tests/single/uninitVars.cil.c";
    
    CPAConfiguration lConfiguration = CPAMain.createConfiguration(lArguments);
    
    // necessary for LogManager
    CPAMain.cpaConfig = lConfiguration;
    
    LogManager lLogManager = LogManager.getInstance();
      
    MainCPAStatistics lStatistics = new MainCPAStatistics();
    
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager, lStatistics);
    
    TargetGraph lTargetGraph = TargetGraph.createTargetGraphFromCFA(lCPAchecker.getMainFunction());
    
    Filter lFunctionFilter = new Function("func");
    
    States lStates = new States(lFunctionFilter, new Predicates());
    
    Set<? extends TestGoal> lTestGoals = lTargetGraph.apply(lStates);
    
    System.out.println(lTestGoals);
  }
  
  @Test
  public void test_17() throws InvalidCmdlineArgumentException, IOException, CPAException {
    String[] lArguments = new String[3];
    
    lArguments[0] = mConfig;
    lArguments[1] = mPropertiesFile;
    lArguments[2] = "test/tests/single/uninitVars.cil.c";
    
    CPAConfiguration lConfiguration = CPAMain.createConfiguration(lArguments);
    
    // necessary for LogManager
    CPAMain.cpaConfig = lConfiguration;
    
    LogManager lLogManager = LogManager.getInstance();
      
    MainCPAStatistics lStatistics = new MainCPAStatistics();
    
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager, lStatistics);
    
    TargetGraph lTargetGraph = TargetGraph.createTargetGraphFromCFA(lCPAchecker.getMainFunction());
    
    Filter lFunctionFilter = new Function("func");
    
    Edges lEdges = new Edges(lFunctionFilter, new Predicates());
    
    Set<? extends TestGoal> lTestGoals = lTargetGraph.apply(lEdges);
    
    System.out.println(lTestGoals);
  }
  
  @Test
  public void test_18() throws InvalidCmdlineArgumentException, IOException, CPAException {
    String[] lArguments = new String[3];
    
    lArguments[0] = mConfig;
    lArguments[1] = mPropertiesFile;
    lArguments[2] = "test/tests/single/uninitVars.cil.c";
    
    CPAConfiguration lConfiguration = CPAMain.createConfiguration(lArguments);
    
    // necessary for LogManager
    CPAMain.cpaConfig = lConfiguration;
    
    LogManager lLogManager = LogManager.getInstance();
      
    MainCPAStatistics lStatistics = new MainCPAStatistics();
    
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager, lStatistics);
    
    TargetGraph lTargetGraph = TargetGraph.createTargetGraphFromCFA(lCPAchecker.getMainFunction());
    
    Filter lFunctionFilter = new Function("func");
    
    Paths lPaths = new Paths(lFunctionFilter, 2, new Predicates());
    
    Set<? extends TestGoal> lTestGoals = lTargetGraph.apply(lPaths);
    
    System.out.println(lTestGoals);
  }
 
  @Test
  public void test_19() throws InvalidCmdlineArgumentException, IOException, CPAException {
    String[] lArguments = new String[3];
    
    lArguments[0] = mConfig;
    lArguments[1] = mPropertiesFile;
    lArguments[2] = "test/tests/single/loop1.c";
    
    CPAConfiguration lConfiguration = CPAMain.createConfiguration(lArguments);
    
    // necessary for LogManager
    CPAMain.cpaConfig = lConfiguration;
    
    LogManager lLogManager = LogManager.getInstance();
      
    MainCPAStatistics lStatistics = new MainCPAStatistics();
    
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager, lStatistics);
    
    TargetGraph lTargetGraph = TargetGraph.createTargetGraphFromCFA(lCPAchecker.getMainFunction());
    
    Paths lPaths = new Paths(Identity.getInstance(), 2, new Predicates());
    
    Set<? extends TestGoal> lTestGoals = lTargetGraph.apply(lPaths);
    
    System.out.println(lTestGoals);
  }
  
  @Test
  public void test_20() throws InvalidCmdlineArgumentException, IOException, CPAException {
    String[] lArguments = new String[3];
    
    lArguments[0] = mConfig;
    lArguments[1] = mPropertiesFile;
    lArguments[2] = "test/tests/single/loop1.c";
    
    CPAConfiguration lConfiguration = CPAMain.createConfiguration(lArguments);
    
    // necessary for LogManager
    CPAMain.cpaConfig = lConfiguration;
    
    LogManager lLogManager = LogManager.getInstance();
      
    MainCPAStatistics lStatistics = new MainCPAStatistics();
    
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager, lStatistics);
    
    TargetGraph lTargetGraph = TargetGraph.createTargetGraphFromCFA(lCPAchecker.getMainFunction());
    
    Edges lEdges = new Edges(Identity.getInstance(), new Predicates());
    
    States lStates = new States(Identity.getInstance(), new Predicates());
    
    fql.frontend.ast.coverage.Union lUnion = new fql.frontend.ast.coverage.Union(lEdges, lStates);
    
    Set<? extends TestGoal> lTestGoals = lTargetGraph.apply(lUnion);
    
    System.out.println(lTestGoals);
  }
}
