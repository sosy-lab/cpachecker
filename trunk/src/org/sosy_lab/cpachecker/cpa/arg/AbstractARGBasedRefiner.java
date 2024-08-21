// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.arg;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.errorprone.annotations.ForOverride;
import java.util.Collection;
import java.util.Objects;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
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

/**
 * Base implementation for {@link Refiner}s that provides access to ARG utilities (e.g., it provide
 * an error path to the actual refinement code).
 *
 * <p>To use this, implement {@link ARGBasedRefiner} and call {@link
 * AbstractARGBasedRefiner#forARGBasedRefiner(ARGBasedRefiner, ConfigurableProgramAnalysis)}.
 */
public class AbstractARGBasedRefiner implements Refiner, StatisticsProvider {

  private int refinementNumber;

  private final ARGBasedRefiner refiner;
  protected final ARGCPA argCpa;
  private final LogManager logger;

  protected AbstractARGBasedRefiner(AbstractARGBasedRefiner pAbstractARGBasedRefiner) {
    refiner = pAbstractARGBasedRefiner.refiner;
    argCpa = pAbstractARGBasedRefiner.argCpa;
    logger = pAbstractARGBasedRefiner.logger;
  }

  protected AbstractARGBasedRefiner(ARGBasedRefiner pRefiner, ARGCPA pCpa, LogManager pLogger) {
    refiner = pRefiner;
    argCpa = pCpa;
    logger = pLogger;
  }

  /** creates a dummy instance of this refiner, that does nothing but report found error paths. */
  public static Refiner create(ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    return AbstractARGBasedRefiner.forARGBasedRefiner(
        (rs, path) -> CounterexampleInfo.feasibleImprecise(path), pCpa);
  }

  /** Create a {@link Refiner} instance from a {@link ARGBasedRefiner} instance. */
  public static Refiner forARGBasedRefiner(
      final ARGBasedRefiner pRefiner, final ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    checkArgument(
        !(pRefiner instanceof Refiner),
        "ARGBasedRefiners may not implement Refiner, choose between these two!");
    ARGCPA argCpa = CPAs.retrieveCPAOrFail(pCpa, ARGCPA.class, Refiner.class);
    return new AbstractARGBasedRefiner(pRefiner, argCpa, argCpa.getLogger());
  }

  @Override
  public final boolean performRefinement(ReachedSet pReached)
      throws CPAException, InterruptedException {
    logger.log(Level.FINEST, "Starting ARG based refinement");

    assert ARGUtils.checkARG(pReached) : "ARG and reached set do not match before refinement";

    final ARGState lastElement = (ARGState) pReached.getLastState();
    assert lastElement.isTarget()
        : "Last element in reached is not a target state before refinement";
    ARGReachedSet reached = new ARGReachedSet(pReached, argCpa, refinementNumber++);

    final @Nullable ARGPath path = computePath(lastElement, reached);

    if (logger.wouldBeLogged(Level.ALL) && path != null) {
      logger.log(Level.ALL, "Error path:\n", path);
      logger.log(
          Level.ALL,
          "Function calls on Error path:\n",
          Joiner.on("\n ").join(Iterables.filter(path.getFullPath(), CFunctionCallEdge.class)));
    }

    final CounterexampleInfo counterexample;
    try {
      counterexample = performRefinementForPath(reached, path);
    } catch (RefinementFailedException e) {
      if (e.getErrorPath() == null) {
        e.setErrorPath(path);
      }

      // set the path from the exception as the target path
      // so it can be used for debugging
      // we don't know if the path is precise here, so we assume it is imprecise
      // (this only affects the CEXExporter)
      lastElement.addCounterexampleInformation(
          CounterexampleInfo.feasibleImprecise(e.getErrorPath()));
      throw e;
    }

    assert ARGUtils.checkARG(pReached) : "ARG and reached set do not match after refinement";

    if (!counterexample.isSpurious()) {
      ARGPath targetPath = counterexample.getTargetPath();

      // new targetPath must contain root and error node
      assert path != null : "Counterexample should come from a correct path.";
      assert Objects.equals(targetPath.getFirstState(), path.getFirstState())
          : "Target path from refiner does not contain root node";
      assert Objects.equals(targetPath.getLastState(), path.getLastState())
          : "Target path from refiner does not contain target state";

      lastElement.addCounterexampleInformation(counterexample);

      logger.log(Level.FINEST, "Counterexample", counterexample.getUniqueId(), "has been found.");

      // Print error trace if cpa.arg.printErrorPath = true
      argCpa.getARGExporter().exportCounterexampleOnTheFly(lastElement, counterexample);
    }

    logger.log(
        Level.FINEST, "ARG based refinement finished, result is", counterexample.isSpurious());

    return counterexample.isSpurious();
  }

  /**
   * Perform refinement.
   *
   * @param pReached the reached set
   * @param pPath the potential error path
   * @return Information about the counterexample.
   */
  protected CounterexampleInfo performRefinementForPath(ARGReachedSet pReached, ARGPath pPath)
      throws CPAException, InterruptedException {

    // Print all available error traces, if option is enabled
    argCpa.getARGExporter().exportCounterexampleOnTheFly(pReached.asReachedSet());

    return refiner.performRefinementForPath(pReached, pPath);
  }

  /**
   * This method may be overwritten if the standard behavior of <code>ARGUtils.getOnePathTo()</code>
   * is not appropriate in the implementations context.
   *
   * <p>TODO: Currently this function may return null.
   *
   * @param pLastElement Last ARGState of the given reached set
   * @param pReached ReachedSet
   * @throws InterruptedException may be thrown in subclasses
   * @throws CPATransferException may be thrown in subclasses
   * @see org.sosy_lab.cpachecker.cpa.arg.ARGUtils
   */
  @ForOverride
  @Nullable
  protected ARGPath computePath(ARGState pLastElement, ARGReachedSet pReached)
      throws InterruptedException, CPATransferException {
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
