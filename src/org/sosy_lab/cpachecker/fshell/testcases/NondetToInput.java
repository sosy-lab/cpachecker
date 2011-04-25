package org.sosy_lab.cpachecker.fshell.testcases;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.LinkedList;
import java.util.regex.Pattern;

public class NondetToInput {

  public static void replace(String pSourceFile, String pDestinationFile) throws IOException {
    BufferedReader lReader = new BufferedReader(new FileReader(pSourceFile));
    PrintWriter lWriter = new PrintWriter(pDestinationFile);

    // add a declaration of function input
    lWriter.println("int input(void);");
    lWriter.println();
    
    Pattern lDeclarationPattern = Pattern.compile("\\s*int\\s*__BLAST_NONDET\\s*;\\s*");
    
    Pattern lAssignmentPattern = Pattern.compile(".*=\\s*__BLAST_NONDET\\s*;\\s*");
    
    Pattern lLinePattern = Pattern.compile("#line .*");
    
    String lLine;
    
    while ((lLine = lReader.readLine()) != null) {
      if (lDeclarationPattern.matcher(lLine).matches() || lLinePattern.matcher(lLine).matches()) {
        // if pLine matches int __BLAST_NONDET; remove this line
      }
      else if (lAssignmentPattern.matcher(lLine).matches()) {
        // if pLine matches ... = __BLAST_NONDET; replace it with ... = input();
        String[] lParts = lLine.split("=");
        
        lWriter.println(lParts[0] + "= input();");
      }
      else {
        // else write pLine to lWriter
        lWriter.println(lLine);
      }
    }
    
    lWriter.close();
    lReader.close();
  }
  
  public static void gcov(String pSourceFileName, String pTestSuiteFileName) throws IOException, InterruptedException {
    File lTmpSourceFile = File.createTempFile("source", ".c");
    lTmpSourceFile.deleteOnExit();
    
    NondetToInput.replace(pSourceFileName, lTmpSourceFile.getAbsolutePath());
    
    File lTmpExecutable = File.createTempFile("main", "");
    lTmpExecutable.deleteOnExit();
    lTmpExecutable.setWritable(true);
    lTmpExecutable.setExecutable(true);
    
    File lTmpInputObjectFile = File.createTempFile("input", ".o");
    lTmpInputObjectFile.deleteOnExit();
    lTmpInputObjectFile.setWritable(true);
    
    File lTmpInputFile = File.createTempFile("input", ".txt");
    lTmpInputFile.deleteOnExit();
    lTmpInputFile.setWritable(true);
    
    LinkedList<String> lCommand2 = new LinkedList<String>();
    lCommand2.add("/usr/bin/gcc");
    lCommand2.add("-c");
    lCommand2.add("-o");
    lCommand2.add(lTmpInputObjectFile.getAbsolutePath());
    lCommand2.add("-DINPUTFILE=\"" + lTmpInputFile.getAbsolutePath() + "\"");
    lCommand2.add("src/org/sosy_lab/cpachecker/fshell/testcases/input.c");
    
    ProcessBuilder lBuilder = new ProcessBuilder();
    lBuilder.redirectErrorStream(true);
    
    lBuilder.command(lCommand2);
    
    Process lProcess1 = lBuilder.start();
    
    printOutput(lProcess1);
    
    lProcess1.waitFor();
    
    // TODO implement checks
    
    lCommand2.clear();
    lCommand2.add("/usr/bin/gcc");
    lCommand2.add("-o");
    lCommand2.add(lTmpExecutable.getAbsolutePath());
    lCommand2.add("-fprofile-arcs");
    lCommand2.add("-ftest-coverage");
    lCommand2.add(lTmpSourceFile.getAbsolutePath());
    lCommand2.add(lTmpInputObjectFile.getAbsolutePath());
    
    lBuilder.command(lCommand2);
    
    lProcess1 = lBuilder.start();
    
    printOutput(lProcess1);
    
    lProcess1.waitFor();
      
    // TODO implement checks
    
    Collection<TestCase> lTestSuite = TestCase.fromFile(pTestSuiteFileName);
    
    for (TestCase lTestCase : lTestSuite) {
      lTestCase.toInputFile(lTmpInputFile);
       
      lBuilder.command(lTmpExecutable.getAbsolutePath());
      
      lProcess1 = lBuilder.start();
      
      if (printOutput(lProcess1)) {
        System.err.println(lTestCase);
      }
      
      lProcess1.waitFor();
    }
    
    lCommand2.clear();
    lCommand2.add("/usr/bin/gcov");
    lCommand2.add("-b");
    lCommand2.add(lTmpSourceFile.getAbsolutePath());
    
    lBuilder.command(lCommand2);
    
    lProcess1 = lBuilder.start();
    
    printOutput(lProcess1);
    
    lProcess1.waitFor();
    
    File lTmpGCovFile = new File(lTmpSourceFile.getName() + ".gcov");
    lTmpGCovFile.delete();
    
    File lTmpGCdaFile = new File(lTmpSourceFile.getName().substring(0, lTmpSourceFile.getName().length() - 2) + ".gcda");
    lTmpGCdaFile.delete();
    
    File lTmpGCnoFile = new File(lTmpSourceFile.getName().substring(0, lTmpSourceFile.getName().length() - 2) + ".gcno");
    lTmpGCnoFile.delete();
  }
  
  public static boolean printOutput(Process pProcess) throws IOException {
    boolean lErrorOccured = false;
    
    BufferedReader lReader = new BufferedReader(new InputStreamReader(pProcess.getInputStream()));
    
    String lLine;
    
    while ((lLine = lReader.readLine()) != null) {
      if (lLine.startsWith("[ERROR] #")) {
        lErrorOccured = true;
        System.err.println(lLine);
      }
      else {
        System.out.println(lLine);
      }
    }
    
    return lErrorOccured;
  }
  
}
