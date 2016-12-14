/*
 * CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.util.refinement;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import javax.annotation.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException.Reason;
import org.sosy_lab.cpachecker.util.AbstractStates;

/**
 * Class that provides means to extract target states and paths to these from an
 * {@link ARGReachedSet}.
 */
@Options()
public class PathExtractor implements Statistics {

  @Option(secure = true,
      name="cegar.globalRefinement",
      description="whether or not global refinement is performed")
  private boolean globalRefinement = false;

  /**
   * keep log of feasible targets that were already found
   */
  protected final Set<ARGState> feasibleTargets = new HashSet<>();

  protected final LogManager logger;

  // For statistics
  private int targetCounter = 0;

  public PathExtractor(final LogManager pLogger, Configuration config)
     throws InvalidConfigurationException {

    config.inject(this, PathExtractor.class);
    logger = pLogger;
  }

  /**
   * This method returns an unsorted, non-empty collection of target states
   * found during the analysis.
   *
   * @param pReached the set of reached states
   * @return the target states
   */
  public Collection<ARGState> getTargetStates(final ARGReachedSet pReached) throws RefinementFailedException {

    // extract target locations from and exclude those found to be feasible before,
    // e.g., when analysis.stopAfterError is set to false
    List<ARGState> targets = extractTargetStatesFromArg(pReached)
        .filter(Predicates.not(Predicates.in(feasibleTargets))).toList();

    // set of targets may only be empty, if all of them were found feasible previously
    if(targets.isEmpty()) {
      assert feasibleTargets.containsAll(extractTargetStatesFromArg(pReached).toSet());

      throw new RefinementFailedException(Reason.RepeatedCounterexample,
          ARGUtils.getOnePathTo(Iterables.getLast(feasibleTargets)));
    }

    logger.log(Level.FINEST, "number of targets found: " + targets.size());

    targetCounter = targetCounter + targets.size();

    return targets;
  }

  /**
   * This method extracts the last state from the ARG, which has to be a target state.
   */
  protected FluentIterable<ARGState> extractTargetStatesFromArg(final ARGReachedSet pReached) {
    if (globalRefinement) {
      return from(pReached.asReachedSet())
          .transform(AbstractStates.toState(ARGState.class))
          .filter(AbstractStates.IS_TARGET_STATE);
    }

    else {
      ARGState lastState = ((ARGState)pReached.asReachedSet().getLastState());

      assert (lastState.isTarget()) : "Last state is not a target state";

      return from(Collections.singleton(lastState));
    }
  }

  /**
   * This method returns the list of paths to the target states, sorted by the
   * length of the paths, in ascending order.
   *
   * @param targetStates the target states for which to get the target paths
   * @return the list of paths to the target states
   */
  public List<ARGPath> getTargetPaths(final Collection<ARGState> targetStates) {
    List<ARGPath> errorPaths = new ArrayList<>(targetStates.size());

    for (ARGState target : targetStates) {
      errorPaths.add(ARGUtils.getOnePathTo(target));
    }

    return errorPaths;
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
