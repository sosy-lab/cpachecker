package org.sosy_lab.cpachecker.fshell;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;

public class RestartingFShell3 {

  public static void main(String[] args) throws IOException, InterruptedException {
    String lCoverageSpecification = args[0];
    String lCSourceFilename = args[1];
    String lEntryFunction = args[2];
    
    LinkedList<String> lCommand = new LinkedList<String>();
    lCommand.add("java");
    lCommand.add("-Djava.library.path=" + System.getProperty("java.library.path"));
    lCommand.add("-cp");
    lCommand.add(System.getProperty("java.class.path"));
    lCommand.add(Main.class.getName());
    lCommand.add(lCoverageSpecification);
    lCommand.add(lCSourceFilename);
    lCommand.add(lEntryFunction);
    lCommand.add("--withoutCilPreprocessing");
    lCommand.add("--restart");
    lCommand.add("--restart-bound=100000000");
    
    for (int lIndex = 3; lIndex < args.length; lIndex++) {
      lCommand.add(args[lIndex]);
    }
    
    
    File lFeasibilityFile = File.createTempFile("feasibility", ".fs3");
    lFeasibilityFile.deleteOnExit();
    
    File lTestsuiteFile = File.createTempFile("testsuite", ".tst");
    lTestsuiteFile.deleteOnExit();
    
    FeasibilityInformation lFeasibilityInformation = new FeasibilityInformation();
    lFeasibilityInformation.setTestsuiteFilename(lTestsuiteFile.getCanonicalPath());
    lFeasibilityInformation.write(lFeasibilityFile);
    
    lCommand.add("--in=" + lFeasibilityFile.getCanonicalPath());
    lCommand.add("--out=" + lFeasibilityFile.getCanonicalPath());
    lCommand.add("--tout=" + lTestsuiteFile.getCanonicalPath());
    lCommand.add("--logging");
    lCommand.add("--append");
    
    int lReturnValue;
    
    ProcessBuilder lBuilder = new ProcessBuilder(lCommand);
    lBuilder.redirectErrorStream(true);
    
    do {
      Process lFShell3Process = lBuilder.start();
      
      BufferedReader lInput = new BufferedReader(new InputStreamReader(lFShell3Process.getInputStream()));
      
      String lLine = null;
      
      while ((lLine = lInput.readLine()) != null) {
        System.out.println(lLine);
      }
      
      lReturnValue = lFShell3Process.waitFor();
      
      if (lReturnValue != 0) {
        System.out.println("++++++++++++++ RESTART ++++++++++++++");
      }
    }
    while (lReturnValue != 0);
    
    System.out.println("Finished.");
  }
  
}
