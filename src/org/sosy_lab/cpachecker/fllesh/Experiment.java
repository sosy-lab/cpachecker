package org.sosy_lab.cpachecker.fllesh;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class Experiment {
  
  private PrintWriter mWriter;
  
  public Experiment(String pFileName) {
    File lLogFile = new File(pFileName);
    
    if (lLogFile.exists()) {
      throw new RuntimeException();
    }
    
    try {
      lLogFile.createNewFile();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    
    try {
      mWriter = new PrintWriter(new BufferedWriter(new FileWriter(lLogFile)));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    
    mWriter.println("Experiment;Number of Test Goals;Number of Feasible Test Goals;Number of Infeasible Test Goals;Number of Test Cases;Time");
  }
  
  public void addExperiment(String pName, int pNumberOfTestGoals, int pNumberOfFeasibleTestGoals, int pNumberOfInfeasibleTestGoals, int pNumberOfTestCases, int pTime) {
    mWriter.println(pName + ";"+ pNumberOfTestGoals + ";" + pNumberOfFeasibleTestGoals + ";" + pNumberOfInfeasibleTestGoals + ";" + pNumberOfTestCases + ";" + pTime);
  }
  
  public void close() {
    mWriter.close();
  }
  
}
