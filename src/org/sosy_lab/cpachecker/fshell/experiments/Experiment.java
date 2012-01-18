/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.fshell.experiments;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.sosy_lab.cpachecker.fshell.FShell3Result;

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

  public void addExperiment(String pFileName, String pFQLSpecification, String pProgramEntry, boolean pCILPreprocessing, FShell3Result pResult, double pTime) {
    addExperiment(pFileName, pFQLSpecification, pProgramEntry, pCILPreprocessing, pResult.getNumberOfTestGoals(), pResult.getNumberOfFeasibleTestGoals(), pResult.getNumberOfInfeasibleTestGoals(), pResult.getNumberOfTestCases(), pResult.getNumberOfImpreciseTestCases(), pTime, pResult.getTimeInReach(), pResult.getTimeInCover(), pResult.getTimeForFeasibleTestGoals(), pResult.getTimeForInfeasibleTestGoals());
  }

  public void close() {
    mWriter.close();
  }

}
