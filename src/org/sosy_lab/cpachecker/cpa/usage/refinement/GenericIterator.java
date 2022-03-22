// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.usage.refinement;

import com.google.errorprone.annotations.ForOverride;
import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

public abstract class GenericIterator<I, O> extends WrappedConfigurableRefinementBlock<I, O> {
  private StatTimer totalTimer = new StatTimer("Time for generic iterator");
  private StatCounter numOfIterations = new StatCounter("Number of iterations");

  PredicatePrecision completePrecision;

  // Some iterations may be postponed to the end (complicated ones)
  List<O> postponedIterations = new ArrayList<>();

  protected GenericIterator(ConfigurableRefinementBlock<O> pWrapper) {
    super(pWrapper);
  }

  @Override
  public final RefinementResult performBlockRefinement(I pInput)
      throws CPAException, InterruptedException {

    O iteration;

    totalTimer.start();
    completePrecision = PredicatePrecision.empty();
    RefinementResult result = RefinementResult.createFalse();
    init(pInput);

    try {
      while ((iteration = getNext(pInput)) != null) {
        result = iterate(iteration);
        if (result.isTrue()) {
          return result;
        }
      }
      // Check postponed iterations
      for (O i : postponedIterations) {
        result = iterate(i);
        if (result.isTrue()) {
          return result;
        }
      }
      result.addPrecision(completePrecision);
      return result;
    } finally {
      finish(pInput, result);
      sendFinishSignal();
      postponedIterations.clear();
      totalTimer.stop();
    }
  }

  private RefinementResult iterate(O iteration) throws CPAException, InterruptedException {
    numOfIterations.inc();
    RefinementResult result = wrappedRefiner.performBlockRefinement(iteration);

    if (result.isTrue()) {
      // Finish iteration, the race is found
      result.addPrecision(completePrecision);
      return result;
    }

    PredicatePrecision precision = result.getPrecision();
    if (precision != null) {
      completePrecision = completePrecision.mergeWith(precision);
    }

    finishIteration(iteration, result);
    return result;
  }

  protected abstract O getNext(I pInput);

  protected void init(@SuppressWarnings("unused") I pInput) {}

  protected void finish(
      @SuppressWarnings("unused") I pInput, @SuppressWarnings("unused") RefinementResult pResult) {}

  @ForOverride
  protected void finishIteration(
      @SuppressWarnings("unused") O output, @SuppressWarnings("unused") RefinementResult r) {}

  @ForOverride
  protected void printDetailedStatistics(@SuppressWarnings("unused") StatisticsWriter pOut) {}

  @Override
  public final void printStatistics(StatisticsWriter pOut) {
    StatisticsWriter writer = pOut.spacer().put(totalTimer).put(numOfIterations);

    printDetailedStatistics(writer);
    wrappedRefiner.printStatistics(writer);
  }

  protected void postpone(O i) {
    postponedIterations.add(i);
  }
}
