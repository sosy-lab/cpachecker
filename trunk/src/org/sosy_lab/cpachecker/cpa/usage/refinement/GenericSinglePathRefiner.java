// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.usage.refinement;

import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.ForOverride;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

public abstract class GenericSinglePathRefiner
    extends WrappedConfigurableRefinementBlock<
        Pair<ExtendedARGPath, ExtendedARGPath>, Pair<ExtendedARGPath, ExtendedARGPath>> {

  private StatTimer totalTimer = new StatTimer("Time for generic refiner");
  private StatCounter numberOfRefinements = new StatCounter("Number of refinements");
  private StatCounter numberOfRepeatedPath = new StatCounter("Number of repeated paths");

  protected GenericSinglePathRefiner(
      ConfigurableRefinementBlock<Pair<ExtendedARGPath, ExtendedARGPath>> pWrapper) {
    super(pWrapper);
  }

  @Override
  public final RefinementResult performBlockRefinement(
      Pair<ExtendedARGPath, ExtendedARGPath> pInput) throws CPAException, InterruptedException {
    totalTimer.start();

    try {
      ExtendedARGPath firstPath = pInput.getFirst();
      ExtendedARGPath secondPath = pInput.getSecond();

      // Refine paths separately
      RefinementResult result = refinePath(firstPath);
      if (result.isFalse()) {
        return result;
      }
      PredicatePrecision completePrecision = result.getPrecision();
      result = refinePath(secondPath);
      completePrecision = completePrecision.mergeWith(result.getPrecision());
      if (!result.isFalse()) {
        result = wrappedRefiner.performBlockRefinement(pInput);
      }
      result.addPrecision(completePrecision);
      return result;
    } finally {
      totalTimer.stop();
    }
  }

  private RefinementResult refinePath(ExtendedARGPath path)
      throws CPAException, InterruptedException {
    Preconditions.checkArgument(!path.isUnreachable(), "Path could not be unreachable here");

    if (path.isRefinedAsReachableBy(this)) {
      // Means that is is reachable, but other refiners declined it.
      // Now the pair changes. Do not refine it again.
      numberOfRepeatedPath.inc();
      return RefinementResult.createTrue();
    }

    numberOfRefinements.inc();
    RefinementResult result = call(path);
    if (result.isTrue() || result.isUnknown()) {
      path.setAsTrueBy(this);
      return result;
    } else {
      path.setAsFalse();
      return result;
    }
  }

  @Override
  public final void printStatistics(StatisticsWriter pOut) {
    StatisticsWriter writer =
        pOut.spacer().put(totalTimer).put(numberOfRefinements).put(numberOfRepeatedPath);

    printAdditionalStatistics(writer);
    wrappedRefiner.printStatistics(writer);
  }

  @ForOverride
  protected void printAdditionalStatistics(@SuppressWarnings("unused") StatisticsWriter pOut) {}

  protected abstract RefinementResult call(ExtendedARGPath path)
      throws CPAException, InterruptedException;
}
