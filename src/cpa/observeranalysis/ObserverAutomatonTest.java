package cpa.observeranalysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeoutException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ObserverAutomatonTest {
  private static final String OUTPUT_PATH = "test/output/";
  private static final String LOGFILE = " -logfile ";
  private static final String propertiesFilePath = "test/config/observerAnalysisAutom.properties";
  private static final String logFileName     = "CPALog.txt";
  private static final String CONFIG = " -config ";
  private static final String reachedFileName = "reached.txt";
  private static final String consoleFileName = "test/output/console.txt";
  private static final String errorFileName   = "test/output/error.txt";
  private static final long STANDARD_TIMEOUT = 4000;
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
         commandLine = "./scripts/cpa.sh "; // not windows, assume unix
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
    try {
      run(prop, "test/programs/simple/locking_correct.c");
      FileTester ct;
      ct = new FileTester(OutputFile.CONSOLE);
      Assert.assertTrue(ct.systemSafeConsole());
    } catch (FileNotFoundException e) {
      System.err.println("Observer Automaton test failed (File not found: " + e.getMessage() + ")");
      Assert.fail();
    } catch (TimeoutException e) {
      Assert.fail("Timeout");
    }
  }
  @Test
  public void locking_incorrect() {
    tearDown();
    String prop = "CompositeCPA.cpas = cpa.location.LocationCPA, cpa.observeranalysis.ObserverAutomatonCPA \n " +
        "observerAnalysis.inputFile =  test/programs/observerAutomata/LockingAutomatonAll.txt \n " +
        "log.consoleLevel = INFO";
    try {
      run(prop, "test/programs/simple/locking_incorrect.c");
      FileTester ct;
      ct = new FileTester(OutputFile.CONSOLE);
      Assert.assertTrue(ct.systemUnSafeConsole());
    } catch (FileNotFoundException e) {
      System.err.println("Observer Automaton test failed (File not found: " + e.getMessage() + ")");
      Assert.fail();
    } catch (TimeoutException e) {
      Assert.fail("Timeout");
    }
  }
  @Test
  public void explicitAnalysis_observing() {
    tearDown();
    String prop = "CompositeCPA.cpas = cpa.location.LocationCPA, cpa.observeranalysis.ObserverAutomatonCPA, cpa.explicit.ExplicitAnalysisCPA \n " +
        "observerAnalysis.inputFile =  test/programs/observerAutomata/ExcplicitAnalysisObservingAutomaton.txt \n " +
        "log.consoleLevel = INFO";
    try {
      run(prop, "test/programs/simple/ex2.cil.c");
      FileTester ct = new FileTester(OutputFile.CONSOLE);
      Assert.assertTrue(ct.systemSafeConsole());
    } catch (FileNotFoundException e) {
      System.err.println("Observer Automaton test failed (File not found: " + e.getMessage() + ")");
      Assert.fail();
    } catch (TimeoutException e) {
      Assert.fail("Timeout");
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
    }
  }
  private void run(String pPropertiesString, String pSourceCodeFilePath) throws TimeoutException {
    run(pPropertiesString, pSourceCodeFilePath, STANDARD_TIMEOUT);
  }
  
  private void run(String pPropertiesString, String pSourceCodeFilePath, long pTimeout) throws TimeoutException {
    pPropertiesString = pPropertiesString + 
    "\nlog.level=FINER \nreachedSet.export=true \nreachedSet.file=reached.txt";
    File prop = new File(propertiesFilePath);
    FileWriter w;
    Process proc = null;
    Timer timer = new Timer();
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
    cmd.append(" -outputpath ");
    cmd.append(OUTPUT_PATH);
    cmd.append(LOGFILE);
    cmd.append(logFileName);
    cmd.append(" " + pSourceCodeFilePath);
    try {
      FileOutputStream consoleFos = new FileOutputStream(ObserverAutomatonTest.consoleFileName);
      FileOutputStream errorFos = new FileOutputStream(ObserverAutomatonTest.errorFileName);
      
      //System.out.println(this.commandLine + " " + cmd);
      proc = Runtime.getRuntime().exec(this.commandLine + " " + cmd);

      StreamGobbler con = new StreamGobbler(proc.getInputStream(), "OUTPUT", consoleFos);
      StreamGobbler err = new StreamGobbler(proc.getErrorStream(), "ERROR", errorFos);
      con.start();
      err.start();
      
      timer.schedule(new InterruptScheduler(Thread.currentThread()), pTimeout);

      proc.waitFor();
      
      consoleFos.flush(); consoleFos.close();
      errorFos.flush(); errorFos.close();
      
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      throw new InternalError("Error during testing");
    } catch (IOException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      // Stop the process from running
      if (proc != null) proc.destroy();
      throw new TimeoutException("did not return after " + pTimeout + " milliseconds");
    } finally {
      // Stop the timer
      timer.cancel();
      // clear interrupted status
      Thread.interrupted();
    }
  }

  class StreamGobbler extends Thread {
    InputStream  is;
    String       type;
    OutputStream os;

    StreamGobbler(InputStream is, String type) {
      this(is, type, null);
    }

    StreamGobbler(InputStream is, String type, OutputStream redirect) {
      this.is = is;
      this.type = type;
      this.os = redirect;
    }

    @Override
    public void run() {
      try {
        PrintWriter pw = null;
        if (os != null) pw = new PrintWriter(os);

        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        String line = null;
        while ((line = br.readLine()) != null) {
          if (pw != null) pw.println(line);
          // for debug
          //System.out.println(type + ">" + line);    
        }
        if (pw != null) pw.flush();
      } catch (IOException ioe) {
        ioe.printStackTrace();
      }
    }
  }

  private class InterruptScheduler extends TimerTask {
    Thread target = null;
    public InterruptScheduler(Thread target) {
      this.target = target;
    }
    @Override
    public void run() {
      target.interrupt();
    }
  }
  private static class FileTester {
    File file;
    
    FileTester(OutputFile pF) throws FileNotFoundException {
      String fileName = "unknown";
      switch (pF) {
      case CONSOLE:
        fileName = consoleFileName;
        break;
      case ERROR:
        fileName = errorFileName;
        break;
      case LOG:
        fileName = OUTPUT_PATH + logFileName;
        break;
      case REACHED:
        fileName = OUTPUT_PATH + reachedFileName;
        break;
      }
      file = new File(fileName);
      if (!file.exists()) {
        throw new FileNotFoundException(fileName);
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
