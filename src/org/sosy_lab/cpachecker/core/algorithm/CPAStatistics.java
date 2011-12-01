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
package org.sosy_lab.cpachecker.core.algorithm;

import java.io.PrintStream;

import org.sosy_lab.common.Timer;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;

public class CPAStatistics implements Statistics {

  public Timer totalTimer         = new Timer();
  public Timer chooseTimer        = new Timer();
  public Timer precisionTimer     = new Timer();
  public Timer transferTimer      = new Timer();
  public Timer mergeTimer         = new Timer();
  public Timer stopTimer          = new Timer();

  public int   countIterations   = 0;
  public int   maxWaitlistSize   = 0;
  public int   countWaitlistSize = 0;
  public int   countSuccessors   = 0;
  public int   maxSuccessors     = 0;
  public int   countMerge        = 0;
  public int   countStop         = 0;
  public int   countBreak        = 0;

  @Override
  public String getName() {
    return "CPA algorithm";
  }

  @Override
  public void printStatistics(PrintStream out, Result pResult,
      ReachedSet pReached) {
    out.println("Number of iterations:            " + countIterations);
    out.println("Max size of waitlist:            " + maxWaitlistSize);
    out.println("Average size of waitlist:        " + countWaitlistSize
        / countIterations);
    out.println("Number of computed successors:   " + countSuccessors);
    out.println("Max successors for one element:  " + maxSuccessors);
    out.println("Number of times merged:          " + countMerge);
    out.println("Number of times stopped:         " + countStop);
    out.println("Number of times breaked:         " + countBreak);
    out.println();
    out.println("Total time for CPA algorithm:   " + totalTimer + " (Max: " + totalTimer.printMaxTime() + ")");
    out.println("Time for choose from waitlist:  " + chooseTimer);
    out.println("Time for precision adjustment:  " + precisionTimer);
    out.println("Time for transfer relation:     " + transferTimer);
    out.println("Time for merge operator:        " + mergeTimer);
    out.println("Time for stop operator:         " + stopTimer);
  }
}