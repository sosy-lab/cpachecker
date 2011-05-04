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
    lCommand.add("-Djava.library.path=lib/native/x86-linux");
    lCommand.add("-cp");
    lCommand.add(".:bin:" +
        "lib/sigar.jar:" +
        "lib/guava-r09.jar:" +
        "lib/icu4j-4_2_1.jar:" +
        "lib/javabdd-1.0b2.jar:" +
        "lib/java-cup-11a.jar:" +
        "lib/JFlex.jar:" +
        "lib/jgrapht-jdk1.6.jar:" +
        "lib/mathsat.jar:" +
        "lib/eclipse/org.eclipse.cdt.core_5.1.2.201002161416.jar:" +
        "lib/eclipse/org.eclipse.core.contenttype_3.4.1.R35x_v20090826-0451.jar:" +
        "lib/eclipse/org.eclipse.core.jobs_3.4.100.v20090429-1800.jar:" +
        "lib/eclipse/org.eclipse.core.resources_3.5.2.R35x_v20091203-1235.jar:" +
        "lib/eclipse/org.eclipse.core.runtime_3.5.0.v20090525.jar:" +
        "lib/eclipse/org.eclipse.equinox.common_3.5.1.R35x_v20090807-1100.jar:" +
        "lib/eclipse/org.eclipse.equinox.preferences_3.2.301.R35x_v20091117.jar:" +
        "lib/eclipse/org.eclipse.equinox.registry_3.4.100.v20090520-1800.jar:" +
        "lib/eclipse/org.eclipse.osgi_3.5.2.R35x_v20100126.jar:" +
        "lib/eclipse/org.hamcrest.core_1.1.0.v20090501071000.jar:" +
        "lib/json_simple-1.1.jar");
    lCommand.add(Main.class.getName());
    /*lCommand.add(Main.BASIC_BLOCK_2_COVERAGE);
    lCommand.add("test/programs/fql/ssh-simplified/s3_clnt_1_BUG.2.cil.c");
    lCommand.add("main");*/
    lCommand.add(lCoverageSpecification);
    lCommand.add(lCSourceFilename);
    lCommand.add(lEntryFunction);
    lCommand.add("--withoutCilPreprocessing");
    lCommand.add("--restart");
    lCommand.add("--restart-bound=100000000");
    
    
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
