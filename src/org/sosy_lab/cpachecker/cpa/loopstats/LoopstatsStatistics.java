/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.loopstats;

import java.io.PrintStream;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.statistics.AbstractStatistics;

import com.google.common.base.Preconditions;

@Options
class LoopstatsStatistics extends AbstractStatistics implements Statistics, LoopStatisticsReceiver {

  public LoopstatsStatistics(Configuration pConfig) throws InvalidConfigurationException {
    Preconditions.checkNotNull(pConfig);
    pConfig.inject(this);
  }

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, ReachedSet pReached) {
    super.printStatistics(pOut, pResult, pReached);

    // Max. nesting of loops in the ARG

    // Max./avg. number of unrollings per loop

    // Number of different loops
  }

  class LoopStatistics {

    final int activations;
    final int lastIterations;
    final int maxIterations;
    final int minIterations;

    public LoopStatistics(int pActivations, int pLastIterations,
        int pMaxIterations, int pMinIterations) {

      activations = pActivations;
      lastIterations = pLastIterations;
      maxIterations = pMaxIterations;
      minIterations = pMinIterations;
    }

  }

  @Override
  public void signalLoopLeftAfter(Loop pLoop, int pNestedInLoops, int pNumberOfIterations) {
    // TODO Auto-generated method stub

  }

}
