/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.dca;

import java.io.PrintStream;
import javax.annotation.Nullable;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.LassoAnalysisStatistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;

class DCAStatistics extends LassoAnalysisStatistics {

  @SuppressWarnings("unused")
  private final DCACPA dcaCpa;

  final Timer refinementTotalTimer = new Timer();

  public DCAStatistics(DCACPA pCpa) {
    dcaCpa = pCpa;
  }

  @Override
  @Nullable
  public String getName() {
    return "DCARefiner";
  }

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
    pOut.println("  Refinment total time:      " + refinementTotalTimer);
    pOut.println();
  }
}
