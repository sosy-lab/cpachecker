package cpa.observeranalysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import cmdline.stubs.StubFile;

import common.configuration.Configuration;

import cpa.common.CPAchecker;
import cpa.common.CPAcheckerResult;
import cpa.common.LogManager;
import cpa.common.CPAcheckerResult.Result;
import exceptions.InvalidConfigurationException;

public class ObserverAutomatonTest {
  private static final String OUTPUT_PATH = "test/output/";
  private static final String propertiesFilePath = "test/config/observerAnalysisAutom.properties";
  private static final String logFileName     = "CPALog.txt";
  private static final long STANDARD_TIMEOUT = 4000;
//at the moment it does not make sense to test anything but the LogFile. I am keeping the functionality because it might be needed again in the future. 
  static enum OutputFile {LOG}; 
  
  @After
  public void tearDown() {
    File f = new File (OUTPUT_PATH + logFileName);
    if ( f.exists()) f.delete();
  }
  
  @Test
  public void pointerAnalyisTest() {
    tearDown();
    String prop = "CompositeCPA.cpas = cpa.location.LocationCPA, cpa.observeranalysis.ObserverAutomatonCPA, cpa.pointeranalysis.PointerAnalysisCPA \n " +
        "observerAnalysis.inputFile =  test/programs/observerAutomata/PointerAnalysisTestAutomaton.txt \n " +
        "log.consoleLevel = INFO \n" + 
        "observerAnalysis.dotExportFile = " + OUTPUT_PATH + "observerAutomatonExport.dot \n" +
        "analysis.stopAfterError = FALSE";  
    try {
      run(prop, "test/programs/simple/PointerAnalysisErrors.c");
      FileTester et;
      et = new FileTester(OutputFile.LOG);
      Assert.assertTrue(et.fileContains("Found a DOUBLE_FREE"));
      Assert.assertTrue(et.fileContains("Found an INVALID_FREE"));
      Assert.assertTrue(et.fileContains("Found a POTENTIALLY_UNSAFE_DEREFERENCE"));
      Assert.assertTrue(et.fileContains("Found a Memory Leak"));
      Assert.assertTrue(et.fileContains("Found an UNSAFE_DEREFERENCE"));
    } catch (FileNotFoundException e) {
      System.err.println("Observer Automaton test failed (File not found: " + e.getMessage() + ")");
      Assert.fail();
    } catch (TimeoutException e) {
      Assert.fail("Timeout");
    } catch (InvalidConfigurationException e) {
      Assert.fail("InvalidConfiguration");
    }    
  }
  
  @Test
  public void locking_correct() {
    tearDown();
    String prop = "CompositeCPA.cpas = cpa.location.LocationCPA, cpa.observeranalysis.ObserverAutomatonCPA \n " +
        "observerAnalysis.inputFile =  test/programs/observerAutomata/LockingAutomatonAll.txt \n " +
        "log.consoleLevel = INFO \n" + 
        "observerAnalysis.dotExportFile = " + OUTPUT_PATH + "observerAutomatonExport.dot";    
    try {
      CPAcheckerResult results = run(prop, "test/programs/simple/locking_correct.c");
      Assert.assertTrue(results.getResult().equals(Result.SAFE));
    } catch (TimeoutException e) {
      Assert.fail("Timeout");
    } catch (InvalidConfigurationException e) {
      Assert.fail("InvalidConfiguration");
    }
  }
  
  @Test
  public void locking_incorrect() {
    tearDown();
    String prop = "CompositeCPA.cpas = cpa.location.LocationCPA, cpa.observeranalysis.ObserverAutomatonCPA \n " +
        "observerAnalysis.inputFile =  test/programs/observerAutomata/LockingAutomatonAll.txt \n " +
        "log.consoleLevel = INFO";
    try {
      CPAcheckerResult results = run(prop, "test/programs/simple/locking_incorrect.c");
      Assert.assertTrue(results.getResult().equals(Result.UNSAFE));
    } catch (TimeoutException e) {
      Assert.fail("Timeout");
    } catch (InvalidConfigurationException e) {
      Assert.fail("InvalidConfiguration");
    }
  }
  
  @Test
  public void explicitAnalysis_observing() {
    tearDown();
    String prop = "CompositeCPA.cpas = cpa.location.LocationCPA, cpa.observeranalysis.ObserverAutomatonCPA, cpa.explicit.ExplicitAnalysisCPA \n " +
        "observerAnalysis.inputFile =  test/programs/observerAutomata/ExcplicitAnalysisObservingAutomaton.txt \n " +
        "log.consoleLevel = INFO";
    try {
      CPAcheckerResult results = run(prop, "test/programs/simple/ex2.cil.c");
      Assert.assertTrue(results.getResult().equals(Result.SAFE));
      
    } catch (TimeoutException e) {
      Assert.fail("Timeout");
    } catch (InvalidConfigurationException e) {
      Assert.fail("InvalidConfiguration");
    }
  }
  
  @Test
  public void functionIdentifying() {
    tearDown();
    String prop = "CompositeCPA.cpas = cpa.location.LocationCPA, cpa.observeranalysis.ObserverAutomatonCPA \n " +
        "observerAnalysis.inputFile =  test/programs/observerAutomata/FunctionIdentifyingAutomaton.txt \n " +
        "log.consoleLevel = FINER";
    try {
      run(prop, "test/programs/simple/functionCall.c");
      FileTester lt;
      lt = new FileTester(OutputFile.LOG);
      Assert.assertTrue(lt.fileContains("i'm in Main after Edge int y;"));
      Assert.assertTrue(lt.fileContains("i'm in f after Edge f()"));
      Assert.assertTrue(lt.fileContains("i'm in f after Edge int x;"));
      Assert.assertTrue(lt.fileContains("i'm in Main after Edge Return Edge to"));
      Assert.assertTrue(lt.fileContains("i'm in Main after Edge Label: ERROR"));
    } catch (FileNotFoundException e) {
      System.err.println("Observer Automaton test failed (File not found: " + e.getMessage() + ")");
      Assert.fail();
    } catch (TimeoutException e) {
      Assert.fail("Timeout");
    } catch (InvalidConfigurationException e) {
      Assert.fail("InvalidConfiguration");
    }
  }
  
  private CPAcheckerResult run(String pPropertiesString, String pSourceCodeFilePath) throws TimeoutException, InvalidConfigurationException {
    return run(pPropertiesString, pSourceCodeFilePath, STANDARD_TIMEOUT);
  }
  
  private CPAcheckerResult run(String pPropertiesString, String pSourceCodeFilePath, long pTimeout) throws TimeoutException, InvalidConfigurationException {
    pPropertiesString = pPropertiesString + 
    "\nlog.level=FINER";
    File prop = new File(propertiesFilePath);
    FileWriter w;
    try {
      w = new FileWriter(prop);
      w.write(pPropertiesString);
      w.flush();
      w.close();
      
      Configuration config;  
      config = new Configuration(propertiesFilePath, null);
      LogManager logger = new LogManager(config);
      CPAchecker cpaChecker = new CPAchecker(config, logger);
      CPAcheckerResult results = cpaChecker.run(new StubFile(pSourceCodeFilePath));
      return results;
    } catch (IOException e1) {
      System.err.print("Test could not create/find the configuration file");
      e1.printStackTrace();
      return null;
    }
  }
  
  private static class FileTester {
    File file;
    
    FileTester(OutputFile pF) throws FileNotFoundException {
      String fileName = "unknown";
      switch (pF) {
      case LOG:
        fileName = OUTPUT_PATH + logFileName;
        break;
      }
      file = new File(fileName);
      if (!file.exists()) {
        throw new FileNotFoundException(fileName);
      }
    }

    boolean fileContains(String pattern) {
       try {
        BufferedReader r = new BufferedReader(new FileReader(this.file));
        while(true) {
          String line = r.readLine();
          if (line == null) break;
          if (line.contains(pattern)) return true;
        }
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      }
      return false;
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
