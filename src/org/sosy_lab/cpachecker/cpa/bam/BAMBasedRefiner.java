// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.bam;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Preconditions;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.cpa.arg.ARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.AbstractARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.bam.BAMSubgraphComputer.BackwardARGState;
import org.sosy_lab.cpachecker.cpa.bam.BAMSubgraphComputer.MissingBlockException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.statistics.ThreadSafeTimerContainer.TimerWrapper;

/**
 * This is an extension of {@link AbstractARGBasedRefiner} that takes care of flattening the ARG
 * before calling {@link ARGBasedRefiner#performRefinementForPath(ARGReachedSet, ARGPath)}.
 *
 * <p>Warning: Although the ARG is flattened at this point, the elements in it have not been
 * expanded due to performance reasons.
 */
public final class BAMBasedRefiner extends AbstractARGBasedRefiner {

  private final AbstractBAMCPA bamCpa;
  private final BAMCPAStatistics stats;

  private BAMBasedRefiner(
      ARGBasedRefiner pRefiner, ARGCPA pArgCpa, AbstractBAMCPA pBamCpa, LogManager pLogger) {
    super(pRefiner, pArgCpa, pLogger);

    bamCpa = pBamCpa;
    stats = bamCpa.getStatistics();
  }

  /**
   * Create a {@link Refiner} instance that supports BAM from a {@link ARGBasedRefiner} instance.
   */
  public static Refiner forARGBasedRefiner(
      final ARGBasedRefiner pRefiner, final ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    checkArgument(
        !(pRefiner instanceof Refiner),
        "ARGBasedRefiners may not implement Refiner, choose between these two!");

    if (!(pCpa instanceof AbstractBAMCPA)) {
      throw new InvalidConfigurationException("BAM CPA needed for BAM-based refinement");
    }
    AbstractBAMCPA bamCpa = (AbstractBAMCPA) pCpa;
    ARGCPA argCpa = CPAs.retrieveCPAOrFail(pCpa, ARGCPA.class, Refiner.class);
    return new BAMBasedRefiner(pRefiner, argCpa, bamCpa, bamCpa.getLogger());
  }

  @Override
  protected CounterexampleInfo performRefinementForPath(ARGReachedSet pReached, ARGPath pPath)
      throws CPAException, InterruptedException {
    checkArgument(
        !(pReached instanceof BAMReachedSet),
        "Wrapping of BAM-based refiners inside BAM-based refiners is not allowed.");
    assert pPath == null || pPath.size() > 0;

    if (pPath == null) {

      // The counter-example-path could not be constructed, because of missing blocks (aka "holes").
      // We directly return SPURIOUS and let the CPA-algorithm run again.
      // During the counter-example-path-building we already re-added the start-states of all
      // blocks,
      // that lead to the missing block, to the waitlists of those blocks.
      // Thus missing blocks are analyzed and rebuild again in the next CPA-algorithm.

      stats.refinementWithMissingBlocks.inc();
      return CounterexampleInfo.spurious();
    } else {

      stats.startedRefinements.inc();
      // wrap the original reached-set to have a valid "view" on all reached states.
      pReached =
          new BAMReachedSet(bamCpa, pReached, pPath, stats.removeCachedSubtreeTimer.getNewTimer());
      final CounterexampleInfo cexInfo = super.performRefinementForPath(pReached, pPath);

      if (cexInfo.isSpurious()) {
        stats.spuriousCex.inc();
      } else if (cexInfo.isPreciseCounterExample()) {
        stats.preciseCex.inc();
      }

      return cexInfo;
    }
  }

  @Override
  protected ARGPath computePath(ARGState pLastElement, ARGReachedSet pMainReachedSet)
      throws InterruptedException, CPATransferException {
    assert pMainReachedSet.asReachedSet().contains(pLastElement)
        : "targetState must be in mainReachedSet.";
    assert BAMReachedSetValidator.validateData(
        bamCpa.getData(), bamCpa.getBlockPartitioning(), pMainReachedSet);

    final TimerWrapper computePathTimer = stats.computePathTimer.getNewTimer();
    final TimerWrapper computeSubtreeTimer = stats.computeSubtreeTimer.getNewTimer();
    final TimerWrapper computeCounterexampleTimer = stats.computeCounterexampleTimer.getNewTimer();

    computePathTimer.start();
    try {
      computeSubtreeTimer.start();
      Pair<BackwardARGState, BackwardARGState> rootAndTargetOfSubgraph;
      try {
        try {
          final BAMSubgraphComputer cexSubgraphComputer = new BAMSubgraphComputer(bamCpa, true);
          rootAndTargetOfSubgraph =
              Preconditions.checkNotNull(
                  cexSubgraphComputer.computeCounterexampleSubgraph(pLastElement, pMainReachedSet));
        } catch (MissingBlockException e) {
          // We return NULL, such that the method performRefinementForPath can handle it.
          return null;
        }
      } finally {
        computeSubtreeTimer.stop();
      }

      computeCounterexampleTimer.start();
      try {
        // search path to target, as in super-class
        return ARGUtils.getOnePathTo(rootAndTargetOfSubgraph.getSecond());
      } finally {
        computeCounterexampleTimer.stop();
      }
    } finally {
      computePathTimer.stop();
    }
  }
}
