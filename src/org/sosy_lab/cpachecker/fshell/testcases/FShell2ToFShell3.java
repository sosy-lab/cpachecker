package org.sosy_lab.cpachecker.fshell.testcases;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class FShell2ToFShell3 {
  
  private final static String USAGE_STRING = "Usage: java [--out=output-file] org.sosy_lab.cpachecker.fshell.testcases.FShell2ToFShell3 <FShell2 result files>";

  public static void main(String[] args) throws IOException {
    if (args.length > 0) {
      PrintStream lOutputStream;
      
      int lStartIndex;
      
      if (args[0].startsWith("--out=")) {
        if (args.length == 1) {
          System.out.println(USAGE_STRING);
          return;
        }
        
        String lFileName = args[0].substring("--out=".length());
        
        lOutputStream = new PrintStream(new FileOutputStream(lFileName));
        
        lStartIndex = 1;
      }
      else {
        lOutputStream = System.out;
        lStartIndex = 0;
      }
      
      for (int lIndex = lStartIndex; lIndex < args.length; lIndex++) {
        String lFileName = args[lIndex];
        
        BufferedReader lReader = new BufferedReader(new FileReader(lFileName));
        
        String lLine = null;
        
        boolean lNextInput = false;
        
        Set<TestCase> lTestCases = new LinkedHashSet<TestCase>();
        
        List<Integer> lValues = new LinkedList<Integer>();
        
        while ((lLine = lReader.readLine()) != null) {
          lLine = lLine.trim();
          
          if (lLine.equals("")) {
            if (lNextInput) {
              lNextInput = false;
              TestCase lTestCase = new PreciseInputsTestCase(lValues);
              lValues.clear();
              lTestCases.add(lTestCase);
            }
            
            continue;
          }
          
          if (lNextInput) {
            if (lLine.startsWith("input()=")) {
              String lValue = lLine.substring("input()=".length());
              lValues.add(Integer.valueOf(lValue));
            }
          }
          else {
            if (lLine.equals("IN:")) {
              lNextInput = true;
            }
          }
        }
        
        for (TestCase lTestCase : lTestCases) {
          lOutputStream.println(lTestCase);
        }
      }
    }
    else {
      System.out.println(USAGE_STRING);
    }
  }
  
}
