// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.refiner;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.UniqueIdGenerator;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.path.PathIterator;
import org.sosy_lab.cpachecker.cpa.smg.SMGOptions.SMGExportLevel;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.refinement.EdgeInterpolator;
import org.sosy_lab.cpachecker.util.refinement.Interpolant;
import org.sosy_lab.cpachecker.util.refinement.InterpolationTree;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;

public class SMGPathInterpolator {

  /** the offset in the path from where to cut-off the subtree, and restart the analysis */
  protected int interpolationOffset = -1;

  /** Generate unique id for path interpolations. */
  private static final UniqueIdGenerator idGenerator = new UniqueIdGenerator();

  private final StatCounter totalInterpolations = new StatCounter("Number of interpolations");
  private final StatInt totalInterpolationQueries =
      new StatInt(StatKind.SUM, "Number of interpolation queries");

  private final ShutdownNotifier shutdownNotifier;
  private final SMGEdgeInterpolator interpolator;
  private final SMGInterpolantManager interpolantManager;
  private final SMGFeasibilityChecker checker;

  private final LogManager logger;
  private final SMGPathInterpolationExporter exporter;

  public SMGPathInterpolator(
      ShutdownNotifier pShutdownNotifier,
      SMGInterpolantManager pInterpolantManager,
      SMGEdgeInterpolator pInterpolator,
      LogManager pLogger,
      PathTemplate pExportPath,
      SMGExportLevel pExportWhen,
      SMGFeasibilityChecker pChecker) {
    shutdownNotifier = pShutdownNotifier;
    interpolantManager = pInterpolantManager;
    interpolator = pInterpolator;
    logger = pLogger;
    checker = pChecker;
    exporter = new SMGPathInterpolationExporter(pLogger, pExportPath, pExportWhen);
  }

  public Map<ARGState, SMGInterpolant> performInterpolation(
      ARGPath pErrorPath, SMGInterpolant pInterpolant, ARGReachedSet pReachedSet)
      throws InterruptedException, CPAException {
    totalInterpolations.inc();

    int interpolationId = idGenerator.getFreshId();

    logger.log(Level.ALL, "Start interpolating path with interpolation id ", interpolationId);

    interpolationOffset = -1;

    Map<ARGState, SMGInterpolant> interpolants =
        performEdgeBasedInterpolation(pErrorPath, pInterpolant, pReachedSet);

    propagateFalseInterpolant(pErrorPath, pErrorPath, interpolants);

    exporter.exportInterpolation(pErrorPath, interpolants, interpolationId);

    logger.log(
        Level.ALL,
        "Finish generating Interpolants for path with interpolation id ",
        interpolationId);

    return interpolants;
  }

  /**
   * This method propagates the interpolant "false" to all states that are in the original error
   * path, but are not anymore in the (shorter) prefix.
   *
   * <p>The property that every state on the path beneath the first state with an false interpolant
   * is needed by some code in ValueAnalysisInterpolationTree a subclass of {@link
   * InterpolationTree}, i.e., for global refinement. This property could also be enforced there,
   * but interpolant creation should only happen during interpolation, and not in the data structure
   * holding the interpolants.
   *
   * @param errorPath the original error path
   * @param pErrorPathPrefix the possible shorter error path prefix
   * @param pInterpolants the current interpolant map
   */
  private void propagateFalseInterpolant(
      final ARGPath errorPath,
      final ARGPath pErrorPathPrefix,
      final Map<ARGState, SMGInterpolant> pInterpolants) {
    if (pErrorPathPrefix.size() < errorPath.size()) {
      PathIterator it = errorPath.pathIterator();
      for (int i = 0; i < pErrorPathPrefix.size(); i++) {
        it.advance();
      }
      for (ARGState state : it.getSuffixInclusive().asStatesList()) {
        pInterpolants.put(state, interpolantManager.getFalseInterpolant());
      }
    }
  }

  /**
   * This method performs interpolation on each edge of the path, using the {@link EdgeInterpolator}
   * given to this object at construction.
   *
   * @param pErrorPath the error path prefix to interpolate
   * @param pInterpolant an initial interpolant (only non-trivial when interpolating error path
   *     suffixes in global refinement)
   * @param pReachedSet used to extract the current SMGPrecision, useful for heap abstraction
   *     interpolation
   * @return the mapping of {@link ARGState}s to {@link Interpolant}
   */
  private Map<ARGState, SMGInterpolant> performEdgeBasedInterpolation(
      ARGPath pErrorPath, SMGInterpolant pInterpolant, ARGReachedSet pReachedSet)
      throws InterruptedException, CPAException {

    /*We may as well interpolate every possible target error if path contains more than one.*/
    boolean checkAllTargets = !checker.isFeasible(pErrorPath, true);

    Map<ARGState, SMGInterpolant> pathInterpolants = new LinkedHashMap<>(pErrorPath.size());

    PathIterator pathIterator = pErrorPath.pathIterator();

    List<SMGInterpolant> interpolants = new ArrayList<>();
    interpolants.add(pInterpolant);

    while (pathIterator.hasNext()) {

      List<SMGInterpolant> resultingInterpolants = new ArrayList<>();

      for (SMGInterpolant interpolant : interpolants) {
        shutdownNotifier.shutdownIfNecessary();

        // interpolate at each edge as long as the previous interpolant is not false
        if (!interpolant.isFalse()) {

          ARGState nextARGState = pathIterator.getNextAbstractState();

          List<SMGInterpolant> deriveResult =
              interpolator.deriveInterpolant(
                  pathIterator.getOutgoingEdge(),
                  pathIterator.getPosition(),
                  interpolant,
                  checkAllTargets,
                  pReachedSet,
                  nextARGState);
          resultingInterpolants.addAll(deriveResult);
        } else {
          resultingInterpolants.add(interpolantManager.getFalseInterpolant());
        }

        totalInterpolationQueries.setNextValue(interpolator.getNumberOfInterpolationQueries());

        if (!interpolant.isTrivial() && interpolationOffset == -1) {
          interpolationOffset = pathIterator.getIndex();
        }
      }

      pathIterator.advance();

      SMGInterpolant jointResultInterpolant = joinInterpolants(resultingInterpolants);

      ARGState argState = pathIterator.getAbstractState();
      pathInterpolants.put(argState, jointResultInterpolant);
      interpolants.clear();
      interpolants.addAll(resultingInterpolants);
    }

    return pathInterpolants;
  }

  private SMGInterpolant joinInterpolants(List<SMGInterpolant> pResultingInterpolants) {

    SMGInterpolant result = null;

    for (SMGInterpolant interpolant : pResultingInterpolants) {
      if (result == null) {
        result = interpolant;
      } else {
        result = result.join(interpolant);
      }
    }

    return result;
  }
}
