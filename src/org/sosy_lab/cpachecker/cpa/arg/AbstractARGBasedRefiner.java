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

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Collections2;
import com.google.errorprone.annotations.ForOverride;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import javax.annotation.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.refinement.PathExtractor;

/**
 * Base implementation for {@link Refiner}s that provides access to ARG utilities
 * (e.g., it provide an error path to the actual refinement code).
 *
 * To use this, implement {@link ARGBasedRefiner} and call
 * {@link AbstractARGBasedRefiner#forARGBasedRefiner(ARGBasedRefiner, ConfigurableProgramAnalysis, Configuration)}.
 */
public class AbstractARGBasedRefiner implements Refiner, StatisticsProvider {

  private int refinementNumber;

  private final ARGBasedRefiner refiner;
  private final ARGCPA argCpa;
  private final LogManager logger;

  private final PathExtractor pathExtractor;

  protected AbstractARGBasedRefiner(AbstractARGBasedRefiner pAbstractARGBasedRefiner) {
    refiner = pAbstractARGBasedRefiner.refiner;
    argCpa = pAbstractARGBasedRefiner.argCpa;
    logger = pAbstractARGBasedRefiner.logger;

    pathExtractor = pAbstractARGBasedRefiner.pathExtractor;
  }

  protected AbstractARGBasedRefiner(ARGBasedRefiner pRefiner, ARGCPA pCpa, LogManager pLogger, Configuration pConfig)
      throws InvalidConfigurationException {
    refiner = pRefiner;
    argCpa = pCpa;
    logger = pLogger;
    pathExtractor = new PathExtractor(logger, pConfig);
  }

  /**
   * Create a {@link Refiner} instance from a {@link ARGBasedRefiner} instance.
   */
  public static Refiner forARGBasedRefiner(
      final ARGBasedRefiner pRefiner, final ConfigurableProgramAnalysis pCpa, final Configuration pConfig)
      throws InvalidConfigurationException {
    checkArgument(
        !(pRefiner instanceof Refiner),
        "ARGBasedRefiners may not implement Refiner, choose between these two!");
    ARGCPA argCpa = CPAs.retrieveCPAOrFail(pCpa, ARGCPA.class, Refiner.class);
    return new AbstractARGBasedRefiner(pRefiner, argCpa, argCpa.getLogger(), pConfig);
  }

  private static final Function<CFAEdge, String> pathToFunctionCalls
        = arg ->  arg instanceof CFunctionCallEdge ? arg.toString() : null;

  @Override
  public final boolean performRefinement(ReachedSet pReached) throws CPAException, InterruptedException {
    logger.log(Level.FINEST, "Starting ARG based refinement");

    assert ARGUtils.checkARG(pReached) : "ARG and reached set do not match before refinement";

    ARGReachedSet reached = new ARGReachedSet(pReached, argCpa, refinementNumber++);

    final Collection<ARGState> targets = pathExtractor.getTargetStates(reached);

    List<ARGPath> targetPaths = pathExtractor.getTargetPaths(targets);

    if (logger.wouldBeLogged(Level.ALL) && !targetPaths.isEmpty()) {
      for (ARGPath path : targetPaths) {
        logger.log(Level.ALL, "Error path:\n", path);
        logger.log(
            Level.ALL,
            "Function calls on Error path:\n",
            Joiner.on("\n ")
                .skipNulls()
                .join(Collections2.transform(path.getFullPath(), pathToFunctionCalls)));
      }
    }

    final CounterexampleInfo counterexample;
    try {
      counterexample = performRefinementForPaths(reached, targetPaths);
    } catch (RefinementFailedException e) {
      ARGPath path = targetPaths.get(0);
      if (e.getErrorPath() == null) {
        e.setErrorPath(path);
      }

      // set the path from the exception as the target path
      // so it can be used for debugging
      // we don't know if the path is precise here, so we assume it is imprecise
      // (this only affects the CEXExporter)
      path.getLastState().addCounterexampleInformation(
          CounterexampleInfo.feasibleImprecise(e.getErrorPath()));
      throw e;
    }

    assert ARGUtils.checkARG(pReached) : "ARG and reached set do not match after refinement";

    if (!counterexample.isSpurious()) {
      ARGPath targetPath = counterexample.getTargetPath();
      ARGState lastElement = targetPath.getLastState();

      // new targetPath must contain root and error node
      assert targetPath.getFirstState() == pReached.getFirstState() : "Target path from refiner does not contain root node";
      assert targetPath.getLastState().isTarget() : "Target path from refiner does not contain target state";
      lastElement.addCounterexampleInformation(counterexample);

      logger.log(Level.FINEST, "Counterexample", counterexample.getUniqueId(), "has been found.");

      // Print error trace if cpa.arg.printErrorPath = true
      argCpa.getARGExporter().exportCounterexampleOnTheFly(lastElement, counterexample);

    }

    logger.log(Level.FINEST, "ARG based refinement finished, result is", counterexample.isSpurious());

    return counterexample.isSpurious();
  }

  /**
   * Perform refinement.
   * @param pReached the reached set
   * @param pTargetPaths the potential error paths
   * @return Information about the counterexample.
   */
  protected CounterexampleInfo performRefinementForPaths(ARGReachedSet pReached, List<ARGPath> pTargetPaths)
      throws CPAException, InterruptedException {
    return refiner.performRefinementForPaths(pReached, pTargetPaths);
  }

  /**
   * This method may be overwritten if the standard behavior of <code>ARGUtils.getOnePathTo()</code> is not
   * appropriate in the implementations context.
   *
   * TODO: Currently this function may return null.
   *
   * @param pLastElement Last ARGState of the given reached set
   * @param pReached ReachedSet
   * @throws InterruptedException may be thrown in subclasses
   * @throws CPATransferException may be thrown in subclasses
   * @see org.sosy_lab.cpachecker.cpa.arg.ARGUtils
   */
  @ForOverride
  @Nullable
  protected ARGPath computePath(ARGState pLastElement, ARGReachedSet pReached) throws InterruptedException, CPATransferException {
    return ARGUtils.getOnePathTo(pLastElement);
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    if (refiner instanceof StatisticsProvider) {
      ((StatisticsProvider) refiner).collectStatistics(pStatsCollection);
    }
  }

  @Override
  public String toString() {
    return refiner.toString();
  }
}
