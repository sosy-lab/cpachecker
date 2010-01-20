package fql.backend.targetgraph;

import java.io.IOException;

import org.junit.Test;

import cmdline.CPAMain;
import cmdline.CPAchecker;
import cpa.common.LogManager;
import cpaplugin.CPAConfiguration;
import cpaplugin.MainCPAStatistics;
import cpaplugin.CPAConfiguration.InvalidCmdlineArgumentException;
import exceptions.CPAException;
import fql.frontend.ast.predicate.CIdentifier;
import fql.frontend.ast.predicate.NaturalNumber;
import fql.frontend.ast.predicate.Predicate;

public class TargetGraphTest {
  private String mConfig = "-config";
  private String mPropertiesFile = "test/config/simpleMustMayAnalysis.properties";
  
  @Test
  public void test_01() throws InvalidCmdlineArgumentException, IOException, CPAException {
    String[] lArguments = new String[3];
    
    lArguments[0] = mConfig;
    lArguments[1] = mPropertiesFile;
    lArguments[2] = "test/tests/single/functionCall.c";
    
    CPAConfiguration lConfiguration = new CPAConfiguration(lArguments);
    
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
        
    CPAConfiguration lConfiguration = new CPAConfiguration(lArguments);
    
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
    
    CPAConfiguration lConfiguration = new CPAConfiguration(lArguments);
    
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
    
    CPAConfiguration lConfiguration = new CPAConfiguration(lArguments);
    
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
    
    CPAConfiguration lConfiguration = new CPAConfiguration(lArguments);
    
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
    
    CPAConfiguration lConfiguration = new CPAConfiguration(lArguments);
    
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
    
    CPAConfiguration lConfiguration = new CPAConfiguration(lArguments);
    
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
    
    CPAConfiguration lConfiguration = new CPAConfiguration(lArguments);
    
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
  
}
