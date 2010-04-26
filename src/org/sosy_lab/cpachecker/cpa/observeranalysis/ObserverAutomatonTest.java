package org.sosy_lab.cpachecker.cpa.observeranalysis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.junit.Assert;
import org.junit.Test;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;

import com.google.common.collect.ImmutableMap;
import org.sosy_lab.common.configuration.Configuration;

import org.sosy_lab.cpachecker.core.CPAchecker;
import org.sosy_lab.cpachecker.core.CPAcheckerResult;
import org.sosy_lab.cpachecker.core.LogManager;
import org.sosy_lab.cpachecker.core.LogManager.StringHandler;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.cpa.observeranalysis.ObserverBoolExpr.CPAQuery;
import org.sosy_lab.cpachecker.exceptions.InvalidConfigurationException;

public class ObserverAutomatonTest {
  private static final String OUTPUT_FILE = "test/output/observerAutomatonExport.dot";

  @Test
  public void uninitVarsTest() {
    Map<String, String> prop = ImmutableMap.of(
        "CompositeCPA.cpas",              "cpa.location.LocationCPA, cpa.observeranalysis.ObserverAutomatonCPA, cpa.uninitvars.UninitializedVariablesCPA, cpa.types.TypesCPA",
        "observerAnalysis.inputFile",     "test/programs/observerAutomata/UninitializedVariablesTestAutomaton.txt",
        "log.consoleLevel",               "FINER",
        "observerAnalysis.dotExportFile", OUTPUT_FILE,
        "analysis.stopAfterError",        "FALSE"
      );
    try {
      TestResults results = run(prop, "test/programs/simple/UninitVarsErrors.c");
      Assert.assertTrue(results.logContains("Observer: Uninitialized return value"));
      Assert.assertTrue(results.logContains("Observer: Uninitialized variable used"));
    } catch (InvalidConfigurationException e) {
      Assert.fail("InvalidConfiguration");
    }    
  }
  @Test
  public void pointerAnalyisTest() {
    Map<String, String> prop = ImmutableMap.of(
        "CompositeCPA.cpas",              "cpa.location.LocationCPA, cpa.observeranalysis.ObserverAutomatonCPA, cpa.pointeranalysis.PointerAnalysisCPA",
        "observerAnalysis.inputFile",     "test/programs/observerAutomata/PointerAnalysisTestAutomaton.txt",
        "log.consoleLevel",               "INFO",
        "observerAnalysis.dotExportFile", OUTPUT_FILE,
        "analysis.stopAfterError",        "FALSE"
      );
    try {
      TestResults results = run(prop, "test/programs/simple/PointerAnalysisErrors.c");
      Assert.assertTrue(results.logContains("Found a DOUBLE_FREE"));
      Assert.assertTrue(results.logContains("Found an INVALID_FREE"));
      Assert.assertTrue(results.logContains("Found a POTENTIALLY_UNSAFE_DEREFERENCE"));
      Assert.assertTrue(results.logContains("Found a Memory Leak"));
      Assert.assertTrue(results.logContains("Found an UNSAFE_DEREFERENCE"));
    } catch (InvalidConfigurationException e) {
      Assert.fail("InvalidConfiguration");
    }    
  }
  
  @Test
  public void locking_correct() {
    Map<String, String> prop = ImmutableMap.of(
        "CompositeCPA.cpas",              "cpa.location.LocationCPA, cpa.observeranalysis.ObserverAutomatonCPA",
        "observerAnalysis.inputFile",     "test/programs/observerAutomata/LockingAutomatonAll.txt",
        "log.consoleLevel",               "INFO",
        "observerAnalysis.dotExportFile", OUTPUT_FILE
      );   
    try {
      TestResults results = run(prop, "test/programs/simple/locking_correct.c");
      Assert.assertTrue(results.isSafe());
    } catch (InvalidConfigurationException e) {
      Assert.fail("InvalidConfiguration");
    }
  }
  
  @Test
  public void locking_incorrect() {
    Map<String, String> prop = ImmutableMap.of(
        "CompositeCPA.cpas",              "cpa.location.LocationCPA, cpa.observeranalysis.ObserverAutomatonCPA",
        "observerAnalysis.inputFile",     "test/programs/observerAutomata/LockingAutomatonAll.txt",
        "log.consoleLevel",               "INFO"
      );
    try {
      TestResults results = run(prop, "test/programs/simple/locking_incorrect.c");
      Assert.assertTrue(results.isUnsafe());
    } catch (InvalidConfigurationException e) {
      Assert.fail("InvalidConfiguration");
    }
  }
  
  @Test
  public void explicitAnalysis_observing() {
    Map<String, String> prop = ImmutableMap.of(
        "CompositeCPA.cpas",              "cpa.location.LocationCPA, cpa.observeranalysis.ObserverAutomatonCPA, cpa.explicit.ExplicitAnalysisCPA",
        "observerAnalysis.inputFile",     "test/programs/observerAutomata/ExcplicitAnalysisObservingAutomaton.txt",
        "log.consoleLevel",               "INFO",
        "cpas.explicit.threshold" , "2000"
      );   
    try {
      TestResults results = run(prop, "test/programs/simple/ex2.cil.c");
      //System.out.println(results.log);
      Assert.assertTrue(results.logContains("st==3 after Edge st = 3;"));
      Assert.assertTrue(results.logContains("st==1 after Edge st = 1;"));
      Assert.assertTrue(results.logContains("st==2 after Edge st = 2;"));
      Assert.assertTrue(results.logContains("st==4 after Edge st = 4;"));
      Assert.assertTrue(results.isSafe());
      
    } catch (InvalidConfigurationException e) {
      Assert.fail("InvalidConfiguration");
    }
  }
  
  @Test
  public void functionIdentifying() {
    Map<String, String> prop = ImmutableMap.of(
        "CompositeCPA.cpas",              "cpa.location.LocationCPA, cpa.observeranalysis.ObserverAutomatonCPA",
        "observerAnalysis.inputFile",     "test/programs/observerAutomata/FunctionIdentifyingAutomaton.txt",
        "log.consoleLevel",               "FINER"
      );
    try {
      TestResults results = run(prop, "test/programs/simple/functionCall.c");
      Assert.assertTrue(results.logContains("i'm in Main after Edge int y;"));
      Assert.assertTrue(results.logContains("i'm in f after Edge f()"));
      Assert.assertTrue(results.logContains("i'm in f after Edge int x;"));
      Assert.assertTrue(results.logContains("i'm in Main after Edge Return Edge to"));
      Assert.assertTrue(results.logContains("i'm in Main after Edge Label: ERROR"));
    } catch (InvalidConfigurationException e) {
      Assert.fail("InvalidConfiguration");
    }
  }
  
  @Test
  public void transitionVariableReplacement() {
    Map<String, ObserverVariable> pObserverVariables = null;
    List<AbstractElement> pAbstractElements = null;
    CFAEdge pCfaEdge = null;
    Map<String, String> map = new HashMap<String, String>();
    map.put("log.level", "OFF");
    map.put("log.consoleLevel", "WARNING");
    
    Configuration config = new Configuration(map);
    LogManager pLogger = null;
    try {
      pLogger = new LogManager(config);
    } catch (InvalidConfigurationException e1) {
      Assert.fail("Test setup failed");
    }
    ObserverExpressionArguments args = new ObserverExpressionArguments(pObserverVariables, pAbstractElements, pCfaEdge, pLogger);
    args.putTransitionVariable(1, "hi");
    args.putTransitionVariable(2, "hello");
    // actual test
    String result = CPAQuery.replaceVariables(args, "$1 == $2");
    Assert.assertTrue("hi == hello".equals(result));
    result = CPAQuery.replaceVariables(args, "$1 == $1");
    Assert.assertTrue("hi == hi".equals(result));
    
    pLogger.log(Level.WARNING, "Warning expected in the next line (concerning $5)");
    result = CPAQuery.replaceVariables(args, "$1 == $5");
    Assert.assertTrue(result == null); // $5 has not been found
    // this test should issue a log message!
  }
  /*
  @Test
  public void testJokerReplacementInPattern() {
    // tests the replacement of Joker expressions in the AST comparison
    String result = ObserverASTComparator.replaceJokersInPattern("$20 = $?");
    Assert.assertTrue(result.contains("CPAChecker_ObserverAnalysis_JokerExpression_Num20  =  CPAChecker_ObserverAnalysis_JokerExpression"));
    result = ObserverASTComparator.replaceJokersInPattern("$1 = $?");
    Assert.assertTrue(result.contains("CPAChecker_ObserverAnalysis_JokerExpression_Num1  =  CPAChecker_ObserverAnalysis_JokerExpression"));
    result = ObserverASTComparator.replaceJokersInPattern("$? = $?");
    Assert.assertTrue(result.contains("CPAChecker_ObserverAnalysis_JokerExpression  =  CPAChecker_ObserverAnalysis_JokerExpression"));
    result = ObserverASTComparator.replaceJokersInPattern("$1 = $5");
    Assert.assertTrue(result.contains("CPAChecker_ObserverAnalysis_JokerExpression_Num1  =  CPAChecker_ObserverAnalysis_JokerExpression_Num5 "));
  }*/
  @Test
  public void testJokerReplacementInAST() throws InvalidAutomatonException {
    // tests the replacement of Joker expressions in the AST comparison
    IASTNode patternAST = ObserverASTComparator.generatePatternAST("$20 = $5($?($1, $?));");
    IASTNode sourceAST  = ObserverASTComparator.generateSourceAST("var1 = function(g(var2, egal));");
    ObserverExpressionArguments args = new ObserverExpressionArguments(null, null, null, null);
    
    boolean result = ObserverASTComparator.compareASTs(sourceAST, patternAST, args);
    Assert.assertTrue(result);
    Assert.assertTrue(args.getTransitionVariable(20).equals("var1"));
    Assert.assertTrue(args.getTransitionVariable(1).equals("var2"));
    Assert.assertTrue(args.getTransitionVariable(5).equals("function"));
  }
  
  @Test
  public void interacting_Observers() {
    Map<String, String> prop = ImmutableMap.of(
        "CompositeCPA.cpas", "cpa.location.LocationCPA, cpa.observeranalysis.ObserverAutomatonCPA observerA, cpa.observeranalysis.ObserverAutomatonCPA observerB, cpa.explicit.ExplicitAnalysisCPA",
        "observerA.observerAnalysis.inputFile",     "test/programs/observerAutomata/InteractionAutomatonA.txt",
        "observerB.observerAnalysis.inputFile",     "test/programs/observerAutomata/InteractionAutomatonB.txt",
        "log.consoleLevel", "INFO",
        "cpas.explicit.threshold" , "2000"
      );   
    try {
      TestResults results = run(prop, "test/programs/simple/loop1.c");
      Assert.assertTrue(results.logContains("A: Matched i in line 9 x=2"));
      Assert.assertTrue(results.logContains("B: A increased to 2 And i followed "));
      Assert.assertTrue(results.isSafe());
    } catch (InvalidConfigurationException e) {
      Assert.fail("InvalidConfiguration");
    }
  }
  
  
  
  private TestResults run(Map<String, String> pProperties, String pSourceCodeFilePath) throws InvalidConfigurationException {
    Configuration config = new Configuration(pProperties);  
    StringHandler stringLogHandler = new LogManager.StringHandler();
    LogManager logger = new LogManager(config, stringLogHandler);      
    CPAchecker cpaChecker = new CPAchecker(config, logger);
    CPAcheckerResult results = cpaChecker.run(pSourceCodeFilePath);
    return new TestResults(stringLogHandler.getLog(), results);
  }
  
  private class TestResults {
    private String log;
    private CPAcheckerResult checkerResult;
    public TestResults(String pLog, CPAcheckerResult pCheckerResult) {
      super();
      log = pLog;
      checkerResult = pCheckerResult;
    }
    @SuppressWarnings("unused")
    public String getLog() {
      return log;
    }
    @SuppressWarnings("unused")
    public CPAcheckerResult getCheckerResult() {
      return checkerResult;
    }
    boolean logContains(String pattern) {
     return log.contains(pattern);
    }
    boolean isSafe() {
      return checkerResult.getResult().equals(CPAcheckerResult.Result.SAFE);
    }
    boolean isUnsafe() {
      return checkerResult.getResult().equals(CPAcheckerResult.Result.UNSAFE);
    }
  }
}
