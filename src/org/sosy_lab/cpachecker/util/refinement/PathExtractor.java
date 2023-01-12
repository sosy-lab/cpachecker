// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.refinement;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException.Reason;
import org.sosy_lab.cpachecker.util.AbstractStates;

/**
 * Class that provides means to extract target states and paths to these from an {@link
 * ARGReachedSet}.
 */
@Options
public class PathExtractor implements Statistics {

  @Option(
      secure = true,
      name = "cegar.globalRefinement",
      description = "whether or not global refinement is performed")
  private boolean globalRefinement = false;

  /** keep log of feasible targets that were already found */
  private final Set<ARGState> feasibleTargets = new LinkedHashSet<>();

  private final LogManager logger;

  // For statistics
  private int targetCounter = 0;

  public PathExtractor(final LogManager pLogger, Configuration config)
      throws InvalidConfigurationException {

    config.inject(this, PathExtractor.class);
    logger = pLogger;
  }

  /**
   * This method returns an unsorted, non-empty collection of target states found during the
   * analysis.
   *
   * @param pReached the set of reached states
   * @return the target states
   * @throws InterruptedException as usual
   */
  public Collection<ARGState> getTargetStates(final ARGReachedSet pReached)
      throws RefinementFailedException, InterruptedException {

    // extract target locations from and exclude those found to be feasible before,
    // e.g., when analysis.stopAfterError is set to false
    List<ARGState> targets =
        extractTargetStatesFromArg(pReached)
            .transform(s -> (ARGState) s)
            .filter(Predicates.not(Predicates.in(feasibleTargets)))
            .toList();

    // set of targets may only be empty, if all of them were found feasible previously
    if (targets.isEmpty()) {
      throw new RefinementFailedException(
          Reason.RepeatedCounterexample, ARGUtils.getOnePathTo(Iterables.getLast(feasibleTargets)));
    }

    logger.log(Level.FINEST, "number of targets found: " + targets.size());
    targetCounter = targetCounter + targets.size();
    return targets;
  }

  /** This method extracts the last state from the ARG, which has to be a target state. */
  private FluentIterable<AbstractState> extractTargetStatesFromArg(final ARGReachedSet pReached) {
    if (globalRefinement) {
      return AbstractStates.getTargetStates(pReached.asReachedSet());
    } else {
      AbstractState lastState = pReached.asReachedSet().getLastState();
      assert AbstractStates.isTargetState(lastState) : "Last state is not a target state";
      return from(Collections.singleton(lastState));
    }
  }

  /**
   * This method returns the list of paths to the target states, sorted by the length of the paths,
   * in ascending order.
   *
   * @param targetStates the target states for which to get the target paths
   * @return the list of paths to the target states
   */
  public List<ARGPath> getTargetPaths(final Collection<ARGState> targetStates) {
    return new ArrayList<>(Collections2.transform(targetStates, ARGUtils::getOnePathTo));
  }

  public void addFeasibleTarget(ARGState pLastState) {
    feasibleTargets.add(pLastState);
  }

  @Override
  public void printStatistics(PrintStream out, Result result, UnmodifiableReachedSet reached) {
    out.println("Total number of targets found:    " + String.format("%9d", targetCounter));
  }

  @Nullable
  @Override
  public String getName() {
    return getClass().getSimpleName();
  }
}
