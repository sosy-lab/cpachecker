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

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisInformation;
import org.sosy_lab.cpachecker.cpa.value.symbolic.refiner.interpolant.SymbolicInterpolant;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.refinement.FeasibilityChecker;
import org.sosy_lab.cpachecker.util.refinement.InterpolantManager;
import org.sosy_lab.cpachecker.util.refinement.StrongestPostOperator;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import com.google.common.base.Optional;

/**
 * Edge interpolator for
 * {@link org.sosy_lab.cpachecker.cpa.constraints.ConstraintsCPA ConstraintsCPA}.
 * Creates {@link ConstraintsInterpolant ConstraintsInterpolants} based on a combination of
 * {@link org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA ValueAnalysisCPA} and
 * <code>ConstraintsCPA</code>.
 */
@Options(prefix = "cpa.value.refiner")
public class ElementTestingSymbolicEdgeInterpolator
    implements SymbolicEdgeInterpolator {

  @Option(description = "Whether to refine the precision of ConstraintsCPA")
  private boolean refineConstraintsCPA = true;

  private final FeasibilityChecker<ForgettingCompositeState> checker;
  private final StrongestPostOperator<ForgettingCompositeState> strongestPost;
  private final InterpolantManager<ForgettingCompositeState, SymbolicInterpolant>
      interpolantManager;

  private final ShutdownNotifier shutdownNotifier;
  private Precision valuePrecision;

  private int interpolationQueries = 0;

  public ElementTestingSymbolicEdgeInterpolator(
      final FeasibilityChecker<ForgettingCompositeState> pChecker,
      final StrongestPostOperator<ForgettingCompositeState> pStrongestPost,
      final InterpolantManager<ForgettingCompositeState, SymbolicInterpolant> pInterpolantManager,
      final Configuration pConfig,
      final ShutdownNotifier pShutdownNotifier,
      final CFA pCfa
  ) throws InvalidConfigurationException {

    pConfig.inject(this);

    checker = pChecker;
    strongestPost = pStrongestPost;
    interpolantManager = pInterpolantManager;
    shutdownNotifier = pShutdownNotifier;
    valuePrecision = VariableTrackingPrecision.createStaticPrecision(
            pConfig, pCfa.getVarClassification(), ValueAnalysisCPA.class);
  }

  @Override
  public SymbolicInterpolant deriveInterpolant(
      final ARGPath pErrorPath,
      final CFAEdge pCurrentEdge,
      final Deque<ForgettingCompositeState> pCallstack,
      final int pLocationInPath,
      final SymbolicInterpolant pInputInterpolant
  ) throws CPAException {

    interpolationQueries = 0;

    ForgettingCompositeState originState = pInputInterpolant.reconstructState();
    Optional<ForgettingCompositeState> maybeSuccessor =
        strongestPost.getStrongestPost(originState, valuePrecision, pCurrentEdge);

    if (!maybeSuccessor.isPresent()) {
      return interpolantManager.getFalseInterpolant();
    }

    ForgettingCompositeState successorState = maybeSuccessor.get();

    // if nothing changed we keep the same interpolant
    if (originState.equals(successorState)) {
      return pInputInterpolant;
    }

    ARGPath suffix = getSuffix(pErrorPath, pLocationInPath);

    // if the suffix is contradicting by itself, the interpolant can be true
    if (!isPathFeasible(suffix, ForgettingCompositeState.getInitialState())) {
      return interpolantManager.getTrueInterpolant();
    }

    try {
      ForgettingCompositeState necessaryInfo = reduceToNecessaryState(successorState, suffix);

      return interpolantManager.createInterpolant(necessaryInfo);

    } catch (InterruptedException e) {
      throw new CPAException("Interrupted while computing interpolant", e);
    }
  }

  private ForgettingCompositeState reduceToNecessaryState(
      final ForgettingCompositeState pSuccessorState,
      final ARGPath pSuffix
  ) throws CPAException, InterruptedException {

    ForgettingCompositeState reducedState;

    if (refineConstraintsCPA) {
      reducedState =
          reduceConstraintsToNecessaryState(pSuccessorState, pSuffix);

    } else {
      reducedState = pSuccessorState;
    }

    return reduceValuesToNecessaryState(reducedState, pSuffix);
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
      if (isPathFeasible(pSuffix, pSuccessorState)) {
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
      if (isPathFeasible(pSuffix, pSuccessorState)) {
        pSuccessorState.remember(l, forgottenInfo);
      }
    }

    return pSuccessorState;
  }

  private ARGPath getSuffix(ARGPath pErrorPath, int pLocationInPath) {

    return pErrorPath.obtainSuffix(pLocationInPath + 1);
  }

  private boolean isPathFeasible(
      final ARGPath pRemainingErrorPath,
      final ForgettingCompositeState pState
  ) throws CPAException {
    interpolationQueries++;
    return checker.isFeasible(pRemainingErrorPath, pState);
  }

  @Override
  public int getNumberOfInterpolationQueries() {
    return interpolationQueries;
  }
}
