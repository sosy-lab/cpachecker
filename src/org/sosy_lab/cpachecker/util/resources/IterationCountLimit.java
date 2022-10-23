// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.resources;



/** A limit that measures the number of iterations of CPAAlgorithm */
public class IterationCountLimit implements ResourceLimit {

  private final long maxIterations;
  private long currentIterations;
  
  public IterationCountLimit(long pMaxIterations) {
    maxIterations = pMaxIterations;
  }
  
  public void updateCurrentValue(long pCurrentValue){
    currentIterations = pCurrentValue;
  }

  @Override
  public long getCurrentValue() {
    // Insert code to get no of iterations here
    return currentIterations;
  }

  @Override
  public boolean isExceeded(long pCurrentValue) {
    return pCurrentValue >= maxIterations;
  }

  @Override
  public long nanoSecondsToNextCheck(long pCurrentValue) {
    return 0;
  }

  @Override
  public String getName() {
    return "CPA iteration limit of " + maxIterations + " iterations";
  }
}
