package org.sosy_lab.cpachecker.fshell.experiments;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.sosy_lab.cpachecker.fshell.FlleShResult;

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
    
    mWriter.println("File;FQL Specification;Program Entry;CIL Preprocessing;Number of Test Goals;Number of Feasible Test Goals;Number of Infeasible Test Goals;Number of Test Cases;Number of Inprecise Test Cases;Time [s];Time in Reach[s];Time in Cover[s];Time for feasible test goals[s];Time for infeasible test goals[s]");
  }
  
  public void addExperiment(String pFileName, String pFQLSpecification, String pProgramEntry, boolean pCILPreprocessing, int pNumberOfTestGoals, int pNumberOfFeasibleTestGoals, int pNumberOfInfeasibleTestGoals, int pNumberOfTestCases, int pNumberOfInpreciseTestCases, double pTime, double pTimeInReach, double pTimeInCover, double pTimeForFeasibleTestGoals, double pTimeForInfeasibleTestGoals) {
    mWriter.println(pFileName + ";" + pFQLSpecification + ";" + pProgramEntry + ";" + pCILPreprocessing + ";"+ pNumberOfTestGoals + ";" + pNumberOfFeasibleTestGoals + ";" + pNumberOfInfeasibleTestGoals + ";" + pNumberOfTestCases + ";" + pNumberOfInpreciseTestCases + ";" + pTime + ";" + pTimeInReach + ";" + pTimeInCover + ";" + pTimeForFeasibleTestGoals + ";" + pTimeForInfeasibleTestGoals);
  }
  
  public void addExperiment(String pFileName, String pFQLSpecification, String pProgramEntry, boolean pCILPreprocessing, FlleShResult pResult, double pTime) {
    addExperiment(pFileName, pFQLSpecification, pProgramEntry, pCILPreprocessing, pResult.getNumberOfTestGoals(), pResult.getNumberOfFeasibleTestGoals(), pResult.getNumberOfInfeasibleTestGoals(), pResult.getNumberOfTestCases(), pResult.getNumberOfImpreciseTestCases(), pTime, pResult.getTimeInReach(), pResult.getTimeInCover(), pResult.getTimeForFeasibleTestGoals(), pResult.getTimeForInfeasibleTestGoals());
  }
  
  public void close() {
    mWriter.close();
  }
  
}
