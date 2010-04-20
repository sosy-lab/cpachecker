package cpa.observeranalysis;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import common.configuration.Configuration;

import cpa.common.CPAchecker;
import cpa.common.CPAcheckerResult;
import cpa.common.LogManager;
import cpa.common.LogManager.StringHandler;
import exceptions.InvalidConfigurationException;

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
        "log.consoleLevel",               "INFO"
      );   
    try {
      TestResults results = run(prop, "test/programs/simple/ex2.cil.c");
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
