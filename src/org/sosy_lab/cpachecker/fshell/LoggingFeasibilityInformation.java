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
