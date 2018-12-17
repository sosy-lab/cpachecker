/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.usage.refinement;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.errorprone.annotations.ForOverride;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;


public abstract class GenericFilter<P>  extends
WrappedConfigurableRefinementBlock<Pair<ExtendedARGPath, ExtendedARGPath>, Pair<ExtendedARGPath, ExtendedARGPath>> {

  StatTimer totalTimer = new StatTimer("Time for generic filter");
  StatCounter filteredPairs = new StatCounter("Number of filtered pairs");

  private String mainFunction = "ldv_main";

  Predicate<ARGState> isFirstCall = s -> {
      CFANode location = AbstractStates.extractLocation(s);
      if (location instanceof CFunctionEntryNode) {
        CallstackState callstack = AbstractStates.extractStateByType(s, CallstackState.class);
        if (callstack.getPreviousState() != null &&
            callstack.getPreviousState().getCurrentFunction().equals(mainFunction)) {
          return true;
        }
      }
      return false;
    };

  Function<ARGState, String> getFunctionName = s -> AbstractStates.extractLocation(s).getFunctionName();

  @SuppressWarnings("deprecation")
  public GenericFilter(ConfigurableRefinementBlock<Pair<ExtendedARGPath, ExtendedARGPath>> pWrapper
      , Configuration pConfig) {
    super(pWrapper);
    mainFunction = pConfig.getProperty("analysis.entryFunction");
  }

  @Override
  public RefinementResult performBlockRefinement(Pair<ExtendedARGPath, ExtendedARGPath> pInput) throws CPAException, InterruptedException {
    totalTimer.start();

    try {
      ExtendedARGPath firstPath = pInput.getFirst();
      ExtendedARGPath secondPath = pInput.getSecond();
      P firstPathCore = getPathCore(firstPath);
      P secondPathCore = getPathCore(secondPath);

      Boolean b = filter(firstPathCore, secondPathCore);

      if (b) {
        return wrappedRefiner.performBlockRefinement(pInput);
      }
      filteredPairs.inc();
      return RefinementResult.createFalse();
    } finally {
      totalTimer.stop();
    }
  }


  protected abstract Boolean filter(P pFirstPathCore, P pSecondPathCore);

  protected abstract P getPathCore(ExtendedARGPath path);

  @ForOverride
  protected void printAdditionalStatistics(@SuppressWarnings("unused") StatisticsWriter pOut) {}

  @Override
  public final void printStatistics(StatisticsWriter pOut) {
    StatisticsWriter newWriter =
        pOut.spacer()
        .put(totalTimer)
        .put(filteredPairs);

    printAdditionalStatistics(newWriter);
    wrappedRefiner.printStatistics(newWriter);
  }
}
