package cpa.observeranalysis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ObserverAutomatonTest {
  private static final String OUTPUT_PATH = " test/output/";
  private static final String LOGFILE = " -logfile ";
  private static final String propertiesFilePath = "test/config/observerAnalysisAutom.properties";
  private static final String logFileName     = "CPALog.txt";
  private static final String CONFIG = " -config ";
  private static final String reachedFileName = "reached.txt";
  private static final String consoleFileName = "test/output/console.txt";
  private static final String errorFileName   = "test/output/error.txt";
  static enum OutputFile {LOG, REACHED, CONSOLE, ERROR} 
  private String commandLine = "";

  @Before
  public void setUp() {
    /*
     * Get os.name system property using
     * public static String getProperty(String name) method of
     * System class.
     */
     String strOSName = System.getProperty("os.name");
     if (strOSName != null) {
       if (strOSName.toLowerCase().indexOf("windows") != -1)
         commandLine = "cmd.exe /c scripts\\cpa.bat "; // windows
       else
         commandLine = "scripts/cpa.sh "; // not windows, assume unix
     }
  }
  
  //@Before
  public void tearDown() {
    File f = new File(OUTPUT_PATH + reachedFileName);
    if ( f.exists()) f.delete();
    f = new File (OUTPUT_PATH + logFileName);
    if ( f.exists()) f.delete();
    f = new File (consoleFileName);
    if ( f.exists()) f.delete();
    f = new File (errorFileName);
    if ( f.exists()) f.delete();
  }
  
  
  @Test
  public void locking_correct() {
    tearDown();
    String prop = "CompositeCPA.cpas = cpa.location.LocationCPA, cpa.observeranalysis.ObserverAutomatonCPA \n " +
        "observerAnalysis.inputFile =  test/programs/observerAutomata/LockingAutomatonAll.txt \n " +
        "log.consoleLevel = INFO \n" + 
        "observerAnalysis.dotExportFile = " + OUTPUT_PATH + "observerAutomatonExport.dot";    
    run(prop, "test/programs/simple/locking_correct.c");
    FileTester ct;
    try {
      ct = new FileTester(OutputFile.CONSOLE);
      Assert.assertTrue(ct.systemSafeConsole());
    } catch (FileNotFoundException e) {
      System.err.println("Observer Automaton test failed (File not found: " + e.getMessage() + ")");
    }
  }
  @Test
  public void locking_incorrect() {
    tearDown();
    String prop = "CompositeCPA.cpas = cpa.location.LocationCPA, cpa.observeranalysis.ObserverAutomatonCPA \n " +
        "observerAnalysis.inputFile =  test/programs/observerAutomata/LockingAutomatonAll.txt \n " +
        "log.consoleLevel = INFO";
    run(prop, "test/programs/simple/locking_incorrect.c");
    FileTester ct;
    try {
      ct = new FileTester(OutputFile.CONSOLE);
      Assert.assertTrue(ct.systemUnSafeConsole());
    } catch (FileNotFoundException e) {
      System.err.println("Observer Automaton test failed (File not found: " + e.getMessage() + ")");
    }
  }
  @Test
  public void explicitAnalysis_observing() {
    tearDown();
    String prop = "CompositeCPA.cpas = cpa.location.LocationCPA, cpa.observeranalysis.ObserverAutomatonCPA, cpa.explicit.ExplicitAnalysisCPA \n " +
        "observerAnalysis.inputFile =  test/programs/observerAutomata/ExcplicitAnalysisObservingAutomaton.txt \n " +
        "log.consoleLevel = INFO";
    run(prop, "test/programs/simple/ex2.cil.c");
    try {
      FileTester ct = new FileTester(OutputFile.CONSOLE);
      Assert.assertTrue(ct.systemSafeConsole());
    } catch (FileNotFoundException e) {
      System.err.println("Observer Automaton test failed (File not found: " + e.getMessage() + ")");
    }
  }
  // DOES NOT WORK?
  //@Test
  public void functionIdentifying() {
    tearDown();
    String prop = "CompositeCPA.cpas = cpa.location.LocationCPA, cpa.observeranalysis.ObserverAutomatonCPA \n " +
        "observerAnalysis.inputFile =  test/programs/observerAutomata/FunctionIdentifyingAutomaton.txt \n " +
        "log.consoleLevel = FINER";
    run(prop, "test/programs/simple/functionCall.c");
    FileTester lt;
    try {
      lt = new FileTester(OutputFile.LOG);
      Assert.assertTrue(lt.fileContains("i'm in Main after Edge int y;"));
      Assert.assertTrue(lt.fileContains("i'm in f after Edge f()"));
      Assert.assertTrue(lt.fileContains("i'm in f after Edge int x;"));
      Assert.assertTrue(lt.fileContains("i'm in Main after Edge Return Edge to"));
      Assert.assertTrue(lt.fileContains("i'm in Main after Edge Label: ERROR"));
    } catch (FileNotFoundException e) {
      System.err.println("Observer Automaton test failed (File not found: " + e.getMessage() + ")");
    }
  }
  
  private void run(String pPropertiesString, String pSourceCodeFilePath) {
    pPropertiesString = pPropertiesString + 
    "\nlog.level=FINER \nreachedSet.export=true \nreachedSet.file=reached.txt";
    File prop = new File(propertiesFilePath);
    FileWriter w;
    try {
      w = new FileWriter(prop);
      w.write(pPropertiesString);
      w.flush();
      w.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    
    StringBuilder cmd = new StringBuilder();
    cmd.append(CONFIG);
    cmd.append(propertiesFilePath);
    cmd.append(" -outputpath");
    cmd.append(OUTPUT_PATH);
    cmd.append(LOGFILE);
    cmd.append(logFileName);
    cmd.append(" " + pSourceCodeFilePath);

    try {
      //System.out.println(this.commandLine + " " + cmd);
      Process proc = Runtime.getRuntime().exec(this.commandLine + " " + cmd);
      
      BufferedReader out = new BufferedReader(new InputStreamReader(proc.getInputStream()));
      BufferedReader err = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
      
      BufferedWriter writer = new BufferedWriter(new FileWriter(ObserverAutomatonTest.consoleFileName));
      String line;
      writer.write("");// delete contents
      while ((line = out.readLine()) != null) {
        writer.append(line); writer.newLine();
      }
      writer.flush(); writer.close();
      writer = new BufferedWriter(new FileWriter(ObserverAutomatonTest.errorFileName));
      writer.write("");// delete contents
      while ((line = err.readLine()) != null) {
        writer.append(line); writer.newLine();
      }
      writer.flush(); writer.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      throw new InternalError("Error during testing");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static class FileTester {
    File file;
    
    FileTester(OutputFile pF) throws FileNotFoundException {
      switch (pF) {
      case CONSOLE:
        file = new File(consoleFileName);
        break;
      case ERROR:
        file = new File(errorFileName);
        break;
      case LOG:
        file = new File(OUTPUT_PATH + logFileName);
        break;
      case REACHED:
        file = new File(OUTPUT_PATH + reachedFileName);
        break;
      }
      if (!file.exists()) {
        throw new FileNotFoundException();
      }
    }
    
    boolean systemSafeConsole() {
      return fileContains("NO, the system is safe");
    }
    boolean systemUnSafeConsole() {
      return fileContains("YES, there is a BUG!");
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
}
