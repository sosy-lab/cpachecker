/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.arg;

import java.util.logging.Level;

import javax.annotation.Nullable;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.core.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Collections2;

public abstract class AbstractARGBasedRefiner implements Refiner {

  private int refinementNumber;

  private final ARGCPA argCpa;
  private final LogManager logger;

  private int counterexamplesCounter = 0;

  protected AbstractARGBasedRefiner(ConfigurableProgramAnalysis pCpa) throws InvalidConfigurationException {
    if (pCpa instanceof WrapperCPA) {
      argCpa = ((WrapperCPA) pCpa).retrieveWrappedCpa(ARGCPA.class);
    } else {
      throw new InvalidConfigurationException("ARG CPA needed for refinement");
    }
    if (argCpa == null) {
      throw new InvalidConfigurationException("ARG CPA needed for refinement");
    }
    this.logger = argCpa.getLogger();
  }

  protected final ARGCPA getArgCpa() {
    return argCpa;
  }

  private static final Function<Pair<ARGState, CFAEdge>, String> pathToFunctionCalls
        = new Function<Pair<ARGState, CFAEdge>, String>() {
    @Override
    public String apply(Pair<ARGState, CFAEdge> arg) {

      if (arg.getSecond() instanceof CFunctionCallEdge) {
        CFunctionCallEdge funcEdge = (CFunctionCallEdge)arg.getSecond();
        return funcEdge.toString();
      } else {
        return null;
      }
    }
  };

  @Override
  public final boolean performRefinement(ReachedSet pReached) throws CPAException, InterruptedException {
    return performRefinementWithInfo(pReached).isSpurious();
  }

  /**
   * This method does the same as {@link #performRefinement(ReachedSet)},
   * but it returns some more information about the refinement.
   */
  public final CounterexampleInfo performRefinementWithInfo(ReachedSet pReached) throws CPAException, InterruptedException {
    logger.log(Level.FINEST, "Starting ARG based refinement");

    assert ARGUtils.checkARG(pReached) : "ARG and reached set do not match before refinement";

    final ARGState lastElement = (ARGState)pReached.getLastState();
    assert lastElement.isTarget() : "Last element in reached is not a target state before refinement";
    ARGReachedSet reached = new ARGReachedSet(pReached, argCpa, refinementNumber++);

    ARGPath path = computePath(lastElement, reached);

    if (logger.wouldBeLogged(Level.ALL) && path != null) {
      logger.log(Level.ALL, "Error path:\n", path);
      logger.log(Level.ALL, "Function calls on Error path:\n",
          Joiner.on("\n ").skipNulls().join(Collections2.transform(path, pathToFunctionCalls)));
    }

    CounterexampleInfo counterexample;
    try {
      counterexample = performRefinement(reached, path);
    } catch (RefinementFailedException e) {
      if (e.getErrorPath() == null) {
        e.setErrorPath(path);
      }

      // set the path from the exception as the target path
      // so it can be used for debugging
      argCpa.addCounterexample(lastElement, CounterexampleInfo.feasible(e.getErrorPath(), null));
      throw e;
    }

    assert ARGUtils.checkARG(pReached) : "ARG and reached set do not match after refinement";

    if (!counterexample.isSpurious()) {
      ARGPath targetPath = counterexample.getTargetPath();

      // new targetPath must contain root and error node
      if (path != null) {
        assert targetPath.getFirst().getFirst() == path.getFirst().getFirst() : "Target path from refiner does not contain root node";
        assert targetPath.getLast().getFirst()  == path.getLast().getFirst() : "Target path from refiner does not contain target state";
      }

      argCpa.addCounterexample(lastElement, counterexample);

      logger.log(Level.FINEST, "Counterexample", counterexamplesCounter, "has been found.");

      // Print error trace if cpa.arg.printErrorPath = true
      argCpa.exportCounterexampleOnTheFly((ARGState)pReached.getFirstState(), lastElement,
              counterexample, counterexamplesCounter);
      counterexamplesCounter++;
    }

    logger.log(Level.FINEST, "ARG based refinement finished, result is", counterexample.isSpurious());

    return counterexample;
  }


  /**
   * Perform refinement.
   * @param pReached
   * @param pPath
   * @return Information about the counterexample.
   * @throws InterruptedException
   */
  protected abstract CounterexampleInfo performRefinement(ARGReachedSet pReached, ARGPath pPath)
            throws CPAException, InterruptedException;

  /**
   * This method may be overwritten if the standard behavior of <code>ARGUtils.getOnePathTo()</code> is not
   * appropriate in the implementations context.
   *
   * TODO: Currently this function may return null.
   *
   * @param pLastElement Last ARGState of the given reached set
   * @param pReached ReachedSet
   * @see org.sosy_lab.cpachecker.cpa.arg.ARGUtils
   * @return
   * @throws InterruptedException
   */
  @Nullable
  protected ARGPath computePath(ARGState pLastElement, ARGReachedSet pReached) throws InterruptedException, CPAException {
    return ARGUtils.getOnePathTo(pLastElement);
  }
}
