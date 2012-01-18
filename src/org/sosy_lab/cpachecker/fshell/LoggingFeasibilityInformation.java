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
package org.sosy_lab.cpachecker.fshell;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;

//TODO refactor into interfaces instead of using inheritance
public class LoggingFeasibilityInformation extends FeasibilityInformation {

  private final FeasibilityInformation mFeasibilityInformation;
  private final PrintWriter mWriter;

  public LoggingFeasibilityInformation(FeasibilityInformation pFeasibilityInformation, String pFeasibilityFilename, boolean pAppend) throws FileNotFoundException {
    this(pFeasibilityInformation, new File(pFeasibilityFilename), pAppend);
  }

  public LoggingFeasibilityInformation(FeasibilityInformation pFeasibilityInformation, File pFeasibilityFile, boolean pAppend) throws FileNotFoundException {
    mFeasibilityInformation = pFeasibilityInformation;

    if (!pAppend) {
      mFeasibilityInformation.write(pFeasibilityFile);
    }

    mWriter = new PrintWriter(new FileOutputStream(pFeasibilityFile, true));
  }

  @Override
  public boolean hasTestsuiteFilename() {
    return mFeasibilityInformation.hasTestsuiteFilename();
  }

  @Override
  public String getTestsuiteFilename() {
    return mFeasibilityInformation.getTestsuiteFilename();
  }

  @Override
  public void setTestsuiteFilename(String pFilename) {
    mFeasibilityInformation.setTestsuiteFilename(pFilename);
  }

  @Override
  public void write(String pFeasibilityFilename) throws FileNotFoundException {
    mFeasibilityInformation.write(pFeasibilityFilename);
  }

  @Override
  public void write(File pFeasibilityFile) throws FileNotFoundException {
    mFeasibilityInformation.write(pFeasibilityFile);
  }

  @Override
  public boolean isKnown(int pGoalIndex) {
    return mFeasibilityInformation.isKnown(pGoalIndex);
  }

  @Override
  public boolean isUnknown(int pGoalIndex) {
    return mFeasibilityInformation.isUnknown(pGoalIndex);
  }

  @Override
  public FeasibilityStatus getStatus(int pGoalIndex) {
    return mFeasibilityInformation.getStatus(pGoalIndex);
  }

  @Override
  public void setStatus(int pGoalIndex, FeasibilityStatus pStatus) {
    switch (pStatus) {
    case FEASIBLE:
      mWriter.println(pGoalIndex + " " + FEASIBLE_ABBREV);
      mWriter.flush();
      break;
    case INFEASIBLE:
      mWriter.println(pGoalIndex + " " + INFEASIBLE_ABBREV);
      mWriter.flush();
      break;
    case IMPRECISE:
      mWriter.println(pGoalIndex + " " + IMPRECISE_ABBREV);
      mWriter.flush();
      break;
    }

    mFeasibilityInformation.setStatus(pGoalIndex, pStatus);
  }

  public void close() {
    mWriter.close();
  }

}
