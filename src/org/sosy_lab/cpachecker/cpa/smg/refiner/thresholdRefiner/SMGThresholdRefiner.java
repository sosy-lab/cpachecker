/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.smg.refiner.thresholdRefiner;

import com.google.common.base.Predicates;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.counterexample.CFAPathWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.cpa.smg.SMGCPA;
import org.sosy_lab.cpachecker.cpa.smg.UnmodifiableSMGState;
import org.sosy_lab.cpachecker.cpa.smg.refiner.SMGStrongestPostOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException.Reason;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.refinement.PathExtractor;

public abstract class SMGThresholdRefiner implements Refiner {

  protected final LogManager logger;
  protected final UnmodifiableSMGState initialState;
  protected final SMGStrongestPostOperator strongestPostOpForCEX;
  protected final SMGCPA smgCpa;
  private final ARGCPA argCpa;
  private final PathExtractor pathExtractor;
  private Set<Integer> previousErrorPathIds = Sets.newHashSet();

  protected SMGThresholdRefiner(
      LogManager pLogger, PathExtractor pPathExtractor, ARGCPA pArgCpa, SMGCPA pSmgCpa) {
    logger = pLogger;
    pathExtractor = pPathExtractor;
    argCpa = pArgCpa;
    smgCpa = pSmgCpa;

    strongestPostOpForCEX =
        SMGStrongestPostOperator.getSMGStrongestPostOperatorForCEX(
            smgCpa.getLogger(),
            smgCpa.getCFA(),
            smgCpa.getPredicateManager(),
            smgCpa.getBlockOperator(),
            smgCpa.getOptions());

    initialState =
        smgCpa.getInitialState(
            smgCpa.getCFA().getMainFunction(), StateSpacePartition.getDefaultPartition());
  }

  @Override
  public boolean performRefinement(ReachedSet pReached) throws InterruptedException, CPAException {
    ARGReachedSet argReached = new ARGReachedSet(pReached, argCpa);
    CounterexampleInfo cexInfo = performRefinement(argReached);

    boolean isSpuriousCEX = cexInfo.isSpurious();

    if (isSpuriousCEX) {
      smgCpa.nextRefinment();
    }

    return isSpuriousCEX;
  }

  private CounterexampleInfo performRefinement(ARGReachedSet pArgReached)
      throws CPAException, InterruptedException {

    Collection<ARGState> targets = pathExtractor.getTargetStates(pArgReached);
    List<ARGPath> targetPaths = pathExtractor.getTargetPaths(targets);

    if (!madeProgress(targetPaths.get(0))) {
      throw new RefinementFailedException(Reason.RepeatedCounterexample, targetPaths.get(0));
    }

    return performRefinementForPaths(pArgReached, targetPaths);
  }

  protected abstract CounterexampleInfo performRefinementForPaths(
      ARGReachedSet pArgReached, List<ARGPath> pTargetPaths)
      throws CPAException, InterruptedException;

  protected void refineUsingMaxLength(
      ARGReachedSet pArgReached, ARGState pCutState, int pLengthThreshold)
      throws InterruptedException {

    pArgReached.removeSubtree(
        pCutState,
        SMGThresholdPrecision.createStaticPrecision(
            true, smgCpa.getBlockOperator(), pLengthThreshold),
        Predicates.instanceOf(SMGThresholdPrecision.class));
  }

  protected CounterexampleInfo isAnyPathFeasible(
      final ARGReachedSet pReached, final Collection<ARGPath> pErrorPaths)
      throws CPAException, InterruptedException {

    ARGPath feasiblePath = null;
    for (ARGPath currentPath : pErrorPaths) {

      if (isErrorPathFeasible(currentPath)) {
        if (feasiblePath == null) {
          feasiblePath = currentPath;
        }

        pathExtractor.addFeasibleTarget(currentPath.getLastState());
      }
    }

    // remove all other target states, so that only one is left (for CEX-checker)
    if (feasiblePath != null) {
      for (ARGPath others : pErrorPaths) {
        if (others != feasiblePath) {
          pReached.removeSubtree(others.getLastState());
        }
      }

      logger.log(Level.FINEST, "found a feasible counterexample");
      // we use the imprecise version of the CounterexampleInfo, due to the possible
      // merges which are done in the used CPAs, but if we can compute a path with assignments,
      // it is probably precise.
      CFAPathWithAssumptions assignments = createModel(feasiblePath);
      if (!assignments.isEmpty()) {
        return CounterexampleInfo.feasiblePrecise(feasiblePath, assignments);
      } else {
        return CounterexampleInfo.feasibleImprecise(feasiblePath);
      }
    }

    return CounterexampleInfo.spurious();
  }

  /**
   * This method creates a model for the given error path.
   *
   * @param errorPath the error path for which to create the model
   * @return the model for the given error path
   */
  private CFAPathWithAssumptions createModel(ARGPath errorPath) {

    // TODO Fix creating a model.
    return CFAPathWithAssumptions.empty();
  }

  private boolean madeProgress(ARGPath path) {
    Integer pathId = obtainErrorPathId(path);
    boolean progress = (previousErrorPathIds.isEmpty() || !previousErrorPathIds.contains(pathId));

    previousErrorPathIds.add(pathId);

    return progress;
  }

  protected abstract boolean isErrorPathFeasible(ARGPath pErrorPath)
      throws CPAException, InterruptedException;

  private int obtainErrorPathId(ARGPath path) {
    Set<String> automatonNames =
        AbstractStates.asIterable(path.getLastState())
            .filter(AutomatonState.class)
            .filter(AutomatonState::isTarget)
            .transform(AutomatonState::getOwningAutomatonName)
            .toSet();
    int id = path.toString().hashCode() + automatonNames.hashCode();
    return id;
  }
}