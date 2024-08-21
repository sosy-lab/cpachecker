// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.bam;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGStatistics;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.bam.BAMSubgraphComputer.BackwardARGState;
import org.sosy_lab.cpachecker.cpa.bam.BAMSubgraphComputer.MissingBlockException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.statistics.ThreadSafeTimerContainer;
import org.sosy_lab.cpachecker.util.statistics.ThreadSafeTimerContainer.TimerWrapper;

public class BAMARGStatistics extends ARGStatistics {

  private static final String ERROR_PREFIX =
      "some output or statistics might be missing, could not compute full reached set graph";

  private final AbstractBAMCPA bamCpa;

  public BAMARGStatistics(
      Configuration pConfig,
      LogManager pLogger,
      AbstractBAMCPA pBamCpa,
      ConfigurableProgramAnalysis pCpa,
      Specification pSpecification,
      CFA pCfa)
      throws InvalidConfigurationException {
    super(pConfig, pLogger, pCpa, pSpecification, pCfa);
    bamCpa = pBamCpa;
  }

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
    if (!isValidContext(pReached)) {
      return;
    }

    // TODO create 'full' reached-set to export correctness witnesses.
    // This might cause a lot of overhead, because of missing blocks,
    // aggressive caching, and multi-usage of blocks.

    final FluentIterable<ARGState> frontierStates = getFrontierStates(pReached);

    if (frontierStates.isEmpty()) {
      if (pResult.equals(Result.FALSE)) {
        logger.log(Level.INFO, ERROR_PREFIX, "(no frontier states)");
        // invalid ARG, ignore output.
      } else if (pResult.equals(Result.TRUE)) {
        // In case of TRUE verdict we do not need a target to print super statistics
        super.printStatistics(pOut, pResult, pReached);
      }
      return;
    }

    final UnmodifiableReachedSet bamReachedSetView =
        createReachedSetViewWithoutExceptions(pReached, frontierStates, pResult);
    if (bamReachedSetView != null) {
      super.printStatistics(pOut, pResult, bamReachedSetView);
    }
  }

  @Override
  public void writeOutputFiles(Result pResult, UnmodifiableReachedSet pReached) {
    if (!isValidContext(pReached)) {
      return;
    }

    final FluentIterable<ARGState> frontierStates = getFrontierStates(pReached);

    if (frontierStates.isEmpty()) {
      if (pResult.equals(Result.FALSE)) {
        logger.log(Level.INFO, ERROR_PREFIX, "(no frontier states)");
        // invalid ARG, ignore output.
      } else if (pResult.equals(Result.TRUE)) {
        // In case of TRUE verdict we do not need a target to print super statistics
        super.writeOutputFiles(pResult, pReached);
      }
      return;
    }

    final UnmodifiableReachedSet bamReachedSetView =
        createReachedSetViewWithoutExceptions(pReached, frontierStates, pResult);
    if (bamReachedSetView != null) {
      super.writeOutputFiles(pResult, bamReachedSetView);
    }
  }

  /**
   * Create a view on all reached states that looks like a real complete reached-set.
   *
   * <p>This method catches all internal exceptions and return <code>Null</code> if needed.
   */
  private @Nullable UnmodifiableReachedSet createReachedSetViewWithoutExceptions(
      UnmodifiableReachedSet pReached,
      final FluentIterable<ARGState> frontierStates,
      Result pResult) {
    try {
      return createReachedSetViewWithFallback(pReached, frontierStates, pResult);

    } catch (MissingBlockException e) {
      logger.logUserException(Level.WARNING, e, ERROR_PREFIX);
      return null; // invalid ARG, ignore output.

    } catch (InterruptedException e) {
      logger.logUserException(Level.WARNING, e, "could not compute full reached set graph");
      return null; // invalid ARG, ignore output
    }
  }

  /**
   * Create a view on all reached states that looks like a real complete reached-set.
   *
   * <p>If a block is missing in the cache, we fall back to at least trying to provide a view on all
   * error-paths, because the underlying analysis might be based on a full counterexample anyway.
   */
  private @Nullable UnmodifiableReachedSet createReachedSetViewWithFallback(
      UnmodifiableReachedSet pReached,
      final FluentIterable<ARGState> frontierStates,
      Result pResult)
      throws MissingBlockException, InterruptedException {
    try { // initially try to export the whole reached-set
      return createReachedSetView(pReached, frontierStates);

    } catch (MissingBlockException e) {
      final Collection<ARGState> targetStates =
          frontierStates.filter(AbstractStates::isTargetState).toList();

      if (pResult.equals(Result.FALSE) && !targetStates.isEmpty()) {
        // fallback: if there is a missing block and we have a target state,
        // maybe at least a direct counterexample path can be exported
        logger.logUserException(
            Level.WARNING, e, ERROR_PREFIX + ", falling back to counterexample traces");
        return createReachedSetView(pReached, targetStates);
      }

      throw e; // fallback failed, re-throw the exception
    }
  }

  private @Nullable UnmodifiableReachedSet createReachedSetView(
      UnmodifiableReachedSet pReached, Iterable<ARGState> pFrontierStates)
      throws MissingBlockException, InterruptedException {
    // create pseudo-reached-set for export.
    // it will be sufficient for exporting a CEX (error-path, error-witness, harness)

    // assertion disabled, because it happens with BAM-parallel (reason unknown).
    // assert targets.contains(pReached.getLastState()) : String.format(
    //   "Last state %s of reachedset with root %s is not in target states %s",
    //   pReached.getLastState(), pReached.getFirstState(), targets);
    ARGReachedSet pMainReachedSet =
        new ARGReachedSet((ReachedSet) pReached, cpa, 0 /* irrelevant number */);
    // assertion disabled, because it happens with interrupts from user or on timeouts.
    // assert pMainReachedSet.asReachedSet().asCollection().containsAll(frontierStates)
    //   : "The following states are frontier states, but not part of the reachedset: "
    //     + Iterables.filter(frontierStates, s ->  !pMainReachedSet.asReachedSet().contains(s));
    ImmutableList<ARGState> frontierStates =
        from(pFrontierStates).filter(s -> pMainReachedSet.asReachedSet().contains(s)).toList();
    final BAMSubgraphComputer cexSubgraphComputer = new BAMSubgraphComputer(bamCpa, false);
    final Pair<BackwardARGState, Collection<BackwardARGState>> rootAndTargetsOfSubgraph =
        cexSubgraphComputer.computeCounterexampleSubgraph(frontierStates, pMainReachedSet);

    ARGPath path = ARGUtils.getRandomPath(rootAndTargetsOfSubgraph.getFirst());
    @SuppressWarnings("deprecation")
    TimerWrapper dummyTimer = new ThreadSafeTimerContainer("dummy").getNewTimer();
    BAMReachedSet bamReachedSet = new BAMReachedSet(bamCpa, pMainReachedSet, path, dummyTimer);
    UnmodifiableReachedSet bamReachedSetView = bamReachedSet.asReachedSet();
    readdCounterexampleInfo(pReached, rootAndTargetsOfSubgraph.getSecond());
    return bamReachedSetView;
  }

  private FluentIterable<ARGState> getFrontierStates(UnmodifiableReachedSet pReached) {
    return ((ARGState) pReached.getFirstState())
        .getSubgraph()
        .filter(
            s -> s.getChildren().isEmpty() && !s.isCovered()
            // sometimes we find leaf-states that are at block-entry-locations,
            // and it seems that those states are "not" contained in the reachedSet.
            // I do not know the reason for this. To avoid invalid statistics, lets ignore them.
            // Possible case: entry state of an infinite loop, loop is a block without exit-state.
            // && !bamCpa.getBlockPartitioning().isCallNode(AbstractStates.extractLocation(s))
            );
  }

  private boolean isValidContext(UnmodifiableReachedSet pReached) {
    if (!(cpa instanceof ARGCPA)) {
      logger.log(Level.WARNING, "statistic export needs ARG-CPA");
      return false; // invalid CPA, nothing to do
    }

    if (pReached.size() <= 1) {
      // interrupt, timeout -> no CEX available, ignore reached-set
      logger.log(Level.WARNING, "could not compute full reached set graph, there is no exit state");
      return false;
    }

    return true;
  }

  /**
   * This method takes the CEX-info computed last and inserts it into the current last state.
   *
   * <p>We assume that (if the last state is a target state) the last CEX-check computed a correct
   * CEX-info and wrote it into the last state of the wrapped reached-set.
   */
  private void readdCounterexampleInfo(
      UnmodifiableReachedSet pReached, Collection<BackwardARGState> targets) {
    ARGState argState = (ARGState) pReached.getLastState();
    if (argState != null && argState.isTarget()) {
      Optional<CounterexampleInfo> cex = argState.getCounterexampleInformation();
      Optional<BackwardARGState> matchingState =
          targets.stream().filter(t -> Objects.equals(t.getARGState(), argState)).findFirst();
      if (cex.isPresent() && matchingState.isPresent()) {
        matchingState.orElseThrow().addCounterexampleInformation(cex.orElseThrow());
      }
    }
  }
}
