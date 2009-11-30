/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package programtesting;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.List;
import java.util.logging.Level;

import cmdline.CPAMain;

/**
 *
 * @author holzera
 */
public class FShell {
  public static void isFeasible(List<String> pPath, String pStartFunctionName) {
    assert(pPath != null);
    assert(pStartFunctionName != null);
    
    // C code file
    File lFile = null;
    
    try {
      lFile = File.createTempFile("path", ".i");
    } catch (IOException e) {
      CPAMain.logManager.logException(Level.WARNING, e, "");
      assert(false);
    }
    
    assert(lFile != null);
    
    lFile.deleteOnExit();

    PrintWriter lWriter = null;
    
    try {
      lWriter = new PrintWriter(lFile);
    } catch (FileNotFoundException e) {
      CPAMain.logManager.logException(Level.WARNING, e, "");
      assert(false);
    }
    
    for (String lSourceLine : pPath) {
      lWriter.println(lSourceLine);
    }

    lWriter.close();
    
    
    
    // fql query file
    File lQueryFile = null;
    
    try {
      lQueryFile = File.createTempFile("query", "fql");
    }
    catch (IOException e) {
      CPAMain.logManager.logException(Level.WARNING, e, "");
      assert(false);
    }
    
    assert(lQueryFile != null);
    
    lQueryFile.deleteOnExit();
    
    PrintWriter lQueryWriter = null;
    
    try {
      lQueryWriter = new PrintWriter(lQueryFile);
    } catch (FileNotFoundException e) {
      CPAMain.logManager.logException(Level.WARNING, e, "");
      assert(false);
    }
    
    lQueryWriter.println("add sourcecode \"" + lFile.getAbsolutePath() + "\"");

    lQueryWriter.println("a := path proc " + pStartFunctionName + " to proc " + pStartFunctionName + " exitpoints");
    
    //lQueryWriter.println("test a method bmc limit count 1");
    
    lQueryWriter.println("quit");

    lQueryWriter.close();
    
    
    try {
      // TODO remove reference to local directory
      Process lFShellProcess = Runtime.getRuntime().exec("fortas_shell --file " + lQueryFile.getAbsolutePath());
      
      BufferedReader lOutputReader = new BufferedReader(new InputStreamReader(lFShellProcess.getInputStream()));
      
      String lLine = null;
      
      System.out.println("******** FSHELL SESSION BEGIN ********");
      
      while ((lLine = lOutputReader.readLine()) != null) {
        System.out.println(lLine);
      }
      
      System.out.println("********* FSHELL SESSION END *********");
      
      
      int lFShellExitValue;
    
      lFShellExitValue = lFShellProcess.waitFor();
      
      System.out.println(lFShellExitValue);
    } catch (IOException ex) {
      CPAMain.logManager.logException(Level.WARNING, ex, "");
      assert(false);
    } catch (InterruptedException e) {
      CPAMain.logManager.logException(Level.WARNING, e, "");
      assert(false);
    }
  }
}
