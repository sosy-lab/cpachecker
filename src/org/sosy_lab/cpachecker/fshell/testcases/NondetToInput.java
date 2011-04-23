package org.sosy_lab.cpachecker.fshell.testcases;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
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
    
    String lLine;
    
    while ((lLine = lReader.readLine()) != null) {
      if (lDeclarationPattern.matcher(lLine).matches()) {
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
  
}
