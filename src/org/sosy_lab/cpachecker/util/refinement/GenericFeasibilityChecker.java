// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.refinement;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.path.PathIterator;
import org.sosy_lab.cpachecker.cpa.smg2.SMGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

/** Generic feasibility checker */
@Options(prefix = "util.refinement")
public class GenericFeasibilityChecker<S extends ForgetfulState<?>>
    implements FeasibilityChecker<S> {

  /*
   * The feasibility checker and refiner refine by path.
   * If the location that is flagged as error location is reachable, the error is feasible.
   * This of course does not hold for MemorySafety, where the ErrorInfo also has to be set!
   */
  @Option(
      secure = true,
      description = "Whether one wants to refine MemorySafety errors.",
      name = "refineMemorySafety",
      toUppercase = true)
  private boolean refineMemorySafety = false;

  private final LogManager logger;

  private final StrongestPostOperator<S> strongestPostOp;
  private final S initialState;
  private final VariableTrackingPrecision precision;

  public GenericFeasibilityChecker(
      final StrongestPostOperator<S> pStrongestPostOp,
      final S pInitialState,
      final Class<? extends ConfigurableProgramAnalysis> pCpaToRefine,
      final LogManager pLogger,
      final Configuration pConfig,
      final CFA pCfa)
      throws InvalidConfigurationException {

    pConfig.inject(this, GenericFeasibilityChecker.class);
    strongestPostOp = pStrongestPostOp;
    initialState = pInitialState;
    logger = pLogger;
    precision =
        VariableTrackingPrecision.createStaticPrecision(
            pConfig, pCfa.getVarClassification(), pCpaToRefine);
  }

  @Override
  public boolean isFeasible(ARGPath path) throws CPAException, InterruptedException {
    return isFeasible(path, initialState);
  }

  @Override
  public boolean isFeasible(final ARGPath pPath, final S pStartingPoint)
      throws CPAException, InterruptedException {
    return isFeasible(pPath, pStartingPoint, new ArrayDeque<>());
  }

  @Override
  public final boolean isFeasible(
      final ARGPath pPath, final S pStartingPoint, final Deque<S> pCallstack)
      throws CPAException, InterruptedException {

    try {
      S next = pStartingPoint;

      PathIterator iterator = pPath.fullPathIterator();
      while (iterator.hasNext()) {
        final CFAEdge edge = iterator.getOutgoingEdge();
        Optional<S> maybeNext = strongestPostOp.step(next, edge, precision, pCallstack, pPath);

        if (!maybeNext.isPresent()) {
          logger.log(
              Level.FINE, "found path to be infeasible: ", edge, " did not yield a successor");
          return false;
        } else {
          next = maybeNext.orElseThrow();
        }

        iterator.advance();
      }

      if (refineMemorySafety) {
        for (AbstractState state : iterator.getAbstractState().getWrappedStates()) {
          if (state instanceof SMGState sMGState && next instanceof SMGState) {
            if (sMGState.getErrorInfo().stream()
                .anyMatch(((SMGState) next).getErrorInfo()::contains)) {
              return true;
            }
          }
        }
        return false;
      }
      return true;
    } catch (CPATransferException e) {
      throw new CPAException(
          "Computation of successor failed for checking path: " + e.getMessage(), e);
    }
  }

  @Override
  public boolean isRefineMemorySafety() {
    return refineMemorySafety;
  }
}
