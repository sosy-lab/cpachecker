package cpa.observeranalysis;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import cmdline.stubs.StubFile;

import common.configuration.Configuration;

import cpa.common.CPAchecker;
import cpa.common.CPAcheckerResult;
import cpa.common.LogManager;
import cpa.common.LogManager.StringHandler;
import exceptions.InvalidConfigurationException;

public class ObserverAutomatonTest {
  private static final String OUTPUT_PATH = "test/output/";
  private static final String propertiesFilePath = "test/config/observerAnalysisAutom.properties";
  @Test
  public void uninitVarsTest() {
    String prop = "CompositeCPA.cpas = cpa.location.LocationCPA, cpa.observeranalysis.ObserverAutomatonCPA, cpa.uninitvars.UninitializedVariablesCPA, cpa.types.TypesCPA \n " +
        "observerAnalysis.inputFile =  test/programs/observerAutomata/UninitializedVariablesTestAutomaton.txt \n " +
        "log.consoleLevel = FINER \n" + 
        "observerAnalysis.dotExportFile = " + OUTPUT_PATH + "observerAutomatonExport.dot \n" +
        "analysis.stopAfterError = FALSE";  
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
    String prop = "CompositeCPA.cpas = cpa.location.LocationCPA, cpa.observeranalysis.ObserverAutomatonCPA, cpa.pointeranalysis.PointerAnalysisCPA \n " +
        "observerAnalysis.inputFile =  test/programs/observerAutomata/PointerAnalysisTestAutomaton.txt \n " +
        "log.consoleLevel = INFO \n" + 
        "observerAnalysis.dotExportFile = " + OUTPUT_PATH + "observerAutomatonExport.dot \n" +
        "analysis.stopAfterError = FALSE";  
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
    String prop = "CompositeCPA.cpas = cpa.location.LocationCPA, cpa.observeranalysis.ObserverAutomatonCPA \n " +
        "observerAnalysis.inputFile =  test/programs/observerAutomata/LockingAutomatonAll.txt \n " +
        "log.consoleLevel = INFO \n" + 
        "observerAnalysis.dotExportFile = " + OUTPUT_PATH + "observerAutomatonExport.dot";    
    try {
      TestResults results = run(prop, "test/programs/simple/locking_correct.c");
      Assert.assertTrue(results.isSafe());
    } catch (InvalidConfigurationException e) {
      Assert.fail("InvalidConfiguration");
    }
  }
  
  @Test
  public void locking_incorrect() {
    
    String prop = "CompositeCPA.cpas = cpa.location.LocationCPA, cpa.observeranalysis.ObserverAutomatonCPA \n " +
        "observerAnalysis.inputFile =  test/programs/observerAutomata/LockingAutomatonAll.txt \n " +
        "log.consoleLevel = INFO";
    try {
      TestResults results = run(prop, "test/programs/simple/locking_incorrect.c");
      Assert.assertTrue(results.isUnsafe());
    } catch (InvalidConfigurationException e) {
      Assert.fail("InvalidConfiguration");
    }
  }
  
  @Test
  public void explicitAnalysis_observing() {
    String prop = "CompositeCPA.cpas = cpa.location.LocationCPA, cpa.observeranalysis.ObserverAutomatonCPA, cpa.explicit.ExplicitAnalysisCPA \n " +
        "observerAnalysis.inputFile =  test/programs/observerAutomata/ExcplicitAnalysisObservingAutomaton.txt \n " +
        "log.consoleLevel = INFO";
    try {
      TestResults results = run(prop, "test/programs/simple/ex2.cil.c");
      Assert.assertTrue(results.isSafe());
      
    } catch (InvalidConfigurationException e) {
      Assert.fail("InvalidConfiguration");
    }
  }
  
  @Test
  public void functionIdentifying() {
    String prop = "CompositeCPA.cpas = cpa.location.LocationCPA, cpa.observeranalysis.ObserverAutomatonCPA \n " +
        "observerAnalysis.inputFile =  test/programs/observerAutomata/FunctionIdentifyingAutomaton.txt \n " +
        "log.consoleLevel = FINER";
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
  
  private TestResults run(String pPropertiesString, String pSourceCodeFilePath) throws InvalidConfigurationException {
    File prop = new File(propertiesFilePath);
    FileWriter w;
    try {
      w = new FileWriter(prop);
      w.write(pPropertiesString);
      w.flush();
      w.close();
      
      Configuration config;  
      config = new Configuration(propertiesFilePath, null);
      StringHandler stringLogHandler = new LogManager.StringHandler();
      LogManager logger = new LogManager(config, stringLogHandler);      
      CPAchecker cpaChecker = new CPAchecker(config, logger);
      CPAcheckerResult results = cpaChecker.run(new StubFile(pSourceCodeFilePath));
      return new TestResults(stringLogHandler.getLog(), results);
    } catch (IOException e1) {
      System.err.print("Test could not create/find the configuration file");
      e1.printStackTrace();
      return null;
    }
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
  /**
   * unused now, but it might be useful sometime later
   */
  @SuppressWarnings("unused")
  private void determineCommandLine() {
    /*
     * Get os.name system property using
     * public static String getProperty(String name) method of
     * System class.
     */
     String strOSName = System.getProperty("os.name");
     if (strOSName != null) {
       String commandLine;
      if (strOSName.toLowerCase().indexOf("windows") != -1)
         commandLine = "cmd.exe /c scripts\\cpa.bat "; // windows
       else
         commandLine = "./scripts/cpa.sh "; // not windows, assume unix
     }
  }
}
