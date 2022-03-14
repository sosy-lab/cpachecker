// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

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

public abstract class GenericFilter<P>
    extends WrappedConfigurableRefinementBlock<
        Pair<ExtendedARGPath, ExtendedARGPath>, Pair<ExtendedARGPath, ExtendedARGPath>> {

  StatTimer totalTimer = new StatTimer("Time for generic filter");
  StatCounter filteredPairs = new StatCounter("Number of filtered pairs");

  private String mainFunction = "ldv_main";

  Predicate<ARGState> isFirstCall =
      s -> {
        CFANode location = AbstractStates.extractLocation(s);
        if (location instanceof CFunctionEntryNode) {
          CallstackState callstack = AbstractStates.extractStateByType(s, CallstackState.class);
          if (callstack.getPreviousState() != null
              && callstack.getPreviousState().getCurrentFunction().equals(mainFunction)) {
            return true;
          }
        }
        return false;
      };

  Function<ARGState, String> getFunctionName =
      s -> AbstractStates.extractLocation(s).getFunctionName();

  @SuppressWarnings("deprecation")
  protected GenericFilter(
      ConfigurableRefinementBlock<Pair<ExtendedARGPath, ExtendedARGPath>> pWrapper,
      Configuration pConfig) {
    super(pWrapper);
    mainFunction = pConfig.getProperty("analysis.entryFunction");
  }

  @Override
  public RefinementResult performBlockRefinement(Pair<ExtendedARGPath, ExtendedARGPath> pInput)
      throws CPAException, InterruptedException {
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
    StatisticsWriter newWriter = pOut.spacer().put(totalTimer).put(filteredPairs);

    printAdditionalStatistics(newWriter);
    wrappedRefiner.printStatistics(newWriter);
  }
}
