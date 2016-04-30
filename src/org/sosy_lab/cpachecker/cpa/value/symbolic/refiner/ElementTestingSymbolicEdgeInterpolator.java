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
package org.sosy_lab.cpachecker.cpa.value.symbolic.refiner;

import java.util.Deque;
import java.util.HashSet;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.defaults.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathIterator;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathPosition;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.IdentifierAssignment;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisInformation;
import org.sosy_lab.cpachecker.cpa.value.symbolic.refiner.interpolant.SymbolicInterpolant;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.refinement.FeasibilityChecker;
import org.sosy_lab.cpachecker.util.refinement.GenericEdgeInterpolator;
import org.sosy_lab.cpachecker.util.refinement.InterpolantManager;
import org.sosy_lab.cpachecker.util.refinement.StrongestPostOperator;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import com.google.common.base.Optional;

/**
 * Edge interpolator for
 * {@link org.sosy_lab.cpachecker.cpa.constraints.ConstraintsCPA ConstraintsCPA}.
 * Creates {@link SymbolicInterpolant SymbolicInterpolants} based on a combination of
 * {@link org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA ValueAnalysisCPA} and
 * <code>ConstraintsCPA</code>.
 */

@Options(prefix = "cpa.value.symbolic.refinement")
public class ElementTestingSymbolicEdgeInterpolator
    extends GenericEdgeInterpolator<ForgettingCompositeState, ValueAnalysisInformation, SymbolicInterpolant>
    implements SymbolicEdgeInterpolator {

  private enum RefinementStrategy { CONSTRAINTS_FIRST, VALUES_FIRST, VALUES_ONLY }

  @Option(description = "Whether to try to not use any constraints in refinement")
  private boolean avoidConstraints = true;

  @Option(description = "The refinement strategy to use")
  private RefinementStrategy strategy = RefinementStrategy.CONSTRAINTS_FIRST;

  private final ShutdownNotifier shutdownNotifier;

  public ElementTestingSymbolicEdgeInterpolator(
      final FeasibilityChecker<ForgettingCompositeState> pChecker,
      final StrongestPostOperator<ForgettingCompositeState> pStrongestPost,
      final InterpolantManager<ForgettingCompositeState, SymbolicInterpolant> pInterpolantManager,
      final Configuration pConfig,
      final ShutdownNotifier pShutdownNotifier,
      final CFA pCfa
  ) throws InvalidConfigurationException {
    super(pStrongestPost,
          pChecker,
          pInterpolantManager,
          ForgettingCompositeState.getInitialState(pCfa.getMachineModel()),
          ValueAnalysisCPA.class,
          pConfig,
          pShutdownNotifier,
          pCfa
        );

    pConfig.inject(this);

    shutdownNotifier = pShutdownNotifier;
  }

  @Override
  protected ForgettingCompositeState reduceToNecessaryState(
      final ForgettingCompositeState pSuccessorState,
      final ARGPath pSuffix
  ) throws CPAException, InterruptedException {

    ForgettingCompositeState reducedState = pSuccessorState;
    boolean reduceConstraints = true;

    if (avoidConstraints) {
      reducedState = removeAllConstraints(pSuccessorState);

      if (isRemainingPathFeasible(pSuffix, reducedState)) {
        reducedState = pSuccessorState;
      } else {
        reduceConstraints = false;
      }
    }

    switch (strategy) {
      case CONSTRAINTS_FIRST:
        if (reduceConstraints) {
          reducedState = reduceConstraintsToNecessaryState(reducedState, pSuffix);
        }
        reducedState = reduceValuesToNecessaryState(reducedState, pSuffix);
        break;
      case VALUES_ONLY:
        reducedState = reduceValuesToNecessaryState(reducedState, pSuffix);
        break;
      case VALUES_FIRST:
        reducedState = reduceValuesToNecessaryState(reducedState, pSuffix);
        reducedState = reduceConstraintsToNecessaryState(reducedState, pSuffix);
        break;
      default:
        throw new AssertionError("Unhandled strategy " + strategy);
    }

    return reducedState;
  }

  private ForgettingCompositeState removeAllConstraints(final ForgettingCompositeState pState) {
    IdentifierAssignment definiteAssignments = pState.getConstraintsState().getDefiniteAssignment();

    return new ForgettingCompositeState(pState.getValueState(),
                                 new ConstraintsState(new HashSet<Constraint>(), definiteAssignments));
  }

  private ForgettingCompositeState reduceConstraintsToNecessaryState(
      final ForgettingCompositeState pSuccessorState,
      final ARGPath pSuffix
  ) throws CPAException, InterruptedException {

    for (Constraint c : pSuccessorState.getTrackedConstraints()) {
      shutdownNotifier.shutdownIfNecessary();
      pSuccessorState.forget(c);

      // if the suffix is feasible without the just removed constraint, it is necessary
      // for proving the error path's infeasibility and as such we have to re-add it.
      if (isRemainingPathFeasible(pSuffix, pSuccessorState)) {
        pSuccessorState.remember(c);
      }
    }

    return pSuccessorState;
  }

  private ForgettingCompositeState reduceValuesToNecessaryState(
      final ForgettingCompositeState pSuccessorState,
      final ARGPath pSuffix
  ) throws CPAException, InterruptedException {

    for (MemoryLocation l : pSuccessorState.getTrackedMemoryLocations()) {
      shutdownNotifier.shutdownIfNecessary();

      ValueAnalysisInformation forgottenInfo = pSuccessorState.forget(l);

      // if the suffix is feasible without the just removed constraint, it is necessary
      // for proving the error path's infeasibility and as such we have to re-add it.
      //noinspection ConstantConditions
      if (isRemainingPathFeasible(pSuffix, pSuccessorState)) {
        pSuccessorState.remember(l, forgottenInfo);
      }
    }

    return pSuccessorState;
  }
}
