/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package programtesting;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

/**
 *
 * @author holzera
 */
public class FShell {
  //public static void isFeasible(List<String> pPath, String pStartFunctionName) {
  public static void isFeasible(String pStartFunctionName) {
    // C code file
    File lFile = null;
    
    try {
      lFile = File.createTempFile("path", ".i");
    } catch (IOException e) {
      e.printStackTrace();
      assert(false);
    }
    
    assert(lFile != null);
    
    lFile.deleteOnExit();

    PrintWriter lWriter = null;
    
    try {
      lWriter = new PrintWriter(lFile);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      assert(false);
    }

    /*for (String lFunctionString : pPath) {
      lWriter.print(lFunctionString);
    }*/

    lWriter.close();
    
    
    
    // fql query file
    File lQueryFile = null;
    
    try {
      lQueryFile = File.createTempFile("query", "fql");
    }
    catch (IOException e) {
      e.printStackTrace();
      assert(false);
    }
    
    assert(lQueryFile != null);
    
    lQueryFile.deleteOnExit();
    
    PrintWriter lQueryWriter = null;
    
    try {
      lQueryWriter = new PrintWriter(lQueryFile);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      assert(false);
    }

    lQueryWriter.println("quit");

    lQueryWriter.close();
    
    
    try {
      // TODO remove reference to local directory
      Process lFShellProcess = Runtime.getRuntime().exec("./fortas_shell --file " + lQueryFile.getAbsolutePath());
      
      
      int lFShellExitValue;
    
      lFShellExitValue = lFShellProcess.waitFor();
      
      System.out.println(lFShellExitValue);
    } catch (IOException ex) {
      ex.printStackTrace();
      assert(false);
    } catch (InterruptedException e) {
      e.printStackTrace();
      assert(false);
    }
  }
}
