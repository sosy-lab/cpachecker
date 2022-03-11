// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value.symbolic.refiner;

import com.google.common.collect.ImmutableList;
import java.util.Deque;
import java.util.HashSet;
import java.util.Optional;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.collect.Collections3;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.path.PathIterator;
import org.sosy_lab.cpachecker.cpa.arg.path.PathPosition;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisInformation;
import org.sosy_lab.cpachecker.cpa.value.symbolic.refiner.interpolant.SymbolicInterpolant;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.refinement.FeasibilityChecker;
import org.sosy_lab.cpachecker.util.refinement.InterpolantManager;
import org.sosy_lab.cpachecker.util.refinement.StrongestPostOperator;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * Edge interpolator for {@link org.sosy_lab.cpachecker.cpa.constraints.ConstraintsCPA
 * ConstraintsCPA}. Creates {@link SymbolicInterpolant SymbolicInterpolants} based on a combination
 * of {@link org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA ValueAnalysisCPA} and <code>
 * ConstraintsCPA</code>.
 */
@Options(prefix = "cpa.value.symbolic.refinement")
public class ElementTestingSymbolicEdgeInterpolator implements SymbolicEdgeInterpolator {

  private enum RefinementStrategy {
    /* First try to delete as many constraints as possible, then assignments */
    CONSTRAINTS_FIRST,
    /* First try to delete as many assignments as possible, then constraints */
    VALUES_FIRST,
    /*
    Alternate between constraints-first and values-first.
    In first refinement iteration, use CONSTRAINTS_FIRST.
    In second, use VALUES_FIRST. In third, use CONSTRAINTS_FIRST again, and so on.
     */
    ALTERNATING,
    /* Always keep all constraints and only try to delete as many assignments as possible */
    VALUES_ONLY
  }

  @Option(description = "Whether to try to not use any constraints in refinement")
  private boolean avoidConstraints = true;

  @Option(description = "The refinement strategy to use")
  private RefinementStrategy strategy = RefinementStrategy.CONSTRAINTS_FIRST;

  private final FeasibilityChecker<ForgettingCompositeState> checker;
  private final StrongestPostOperator<ForgettingCompositeState> strongestPost;
  private final InterpolantManager<ForgettingCompositeState, SymbolicInterpolant>
      interpolantManager;
  private final MachineModel machineModel;

  private ImmutableList<SymbolicStateReducer> stateReducers;
  private int currentReducerIndex = 0;

  private final ShutdownNotifier shutdownNotifier;
  private Precision valuePrecision;

  private int interpolationQueries = 0;

  public ElementTestingSymbolicEdgeInterpolator(
      final FeasibilityChecker<ForgettingCompositeState> pChecker,
      final StrongestPostOperator<ForgettingCompositeState> pStrongestPost,
      final InterpolantManager<ForgettingCompositeState, SymbolicInterpolant> pInterpolantManager,
      final Configuration pConfig,
      final ShutdownNotifier pShutdownNotifier,
      final CFA pCfa)
      throws InvalidConfigurationException {

    pConfig.inject(this);

    checker = pChecker;
    strongestPost = pStrongestPost;
    interpolantManager = pInterpolantManager;
    shutdownNotifier = pShutdownNotifier;
    valuePrecision =
        VariableTrackingPrecision.createStaticPrecision(
            pConfig, pCfa.getVarClassification(), ValueAnalysisCPA.class);
    machineModel = pCfa.getMachineModel();

    switch (strategy) {
      case ALTERNATING:
        stateReducers = ImmutableList.of(new ConstraintsFirstReducer(), new ValuesFirstReducer());
        break;
      case CONSTRAINTS_FIRST:
        stateReducers = ImmutableList.of(new ConstraintsFirstReducer());
        break;
      case VALUES_FIRST:
        stateReducers = ImmutableList.of(new ValuesFirstReducer());
        break;
      case VALUES_ONLY:
        stateReducers = ImmutableList.of(new ValuesOnlyReducer());
        break;
      default:
        throw new AssertionError("Unhandled strategy: " + strategy);
    }
    if (avoidConstraints) {
      stateReducers =
          Collections3.transformedImmutableListCopy(stateReducers, AvoidConstraintsReducer::new);
    }
  }

  @Override
  public SymbolicInterpolant deriveInterpolant(
      final ARGPath pErrorPath,
      final CFAEdge pCurrentEdge,
      final Deque<ForgettingCompositeState> pCallstack,
      final PathPosition pLocationInPath,
      final SymbolicInterpolant pInputInterpolant)
      throws CPAException, InterruptedException {

    interpolationQueries = 0;

    ForgettingCompositeState originState = pInputInterpolant.reconstructState();
    final Optional<ForgettingCompositeState> maybeSuccessor;
    if (pCurrentEdge == null) {
      PathIterator it = pLocationInPath.fullPathIterator();
      Optional<ForgettingCompositeState> intermediate = Optional.of(originState);
      do {
        if (!intermediate.isPresent()) {
          break;
        }

        intermediate =
            strongestPost.getStrongestPost(
                intermediate.orElseThrow(), valuePrecision, it.getOutgoingEdge());
        it.advance();
      } while (!it.isPositionWithState());
      maybeSuccessor = intermediate;
    } else {
      maybeSuccessor = strongestPost.getStrongestPost(originState, valuePrecision, pCurrentEdge);
    }

    if (!maybeSuccessor.isPresent()) {
      return interpolantManager.getFalseInterpolant();
    }

    ForgettingCompositeState successorState = maybeSuccessor.orElseThrow();

    // if nothing changed we keep the same interpolant
    if (originState.equals(successorState)) {
      return pInputInterpolant;
    }

    ARGPath suffix = pLocationInPath.iterator().getSuffixExclusive();

    // if the suffix is contradicting by itself, the interpolant can be true
    if (!isPathFeasible(suffix, ForgettingCompositeState.getInitialState(machineModel))) {
      return interpolantManager.getTrueInterpolant();
    }

    ForgettingCompositeState necessaryInfo = getReducer().reduce(successorState, suffix);

    return interpolantManager.createInterpolant(necessaryInfo);
  }

  private SymbolicStateReducer getReducer() {
    SymbolicStateReducer reducer = stateReducers.get(currentReducerIndex);
    currentReducerIndex = currentReducerIndex % stateReducers.size();
    return reducer;
  }

  private boolean isPathFeasible(
      final ARGPath pRemainingErrorPath, final ForgettingCompositeState pState)
      throws CPAException, InterruptedException {
    interpolationQueries++;
    return checker.isFeasible(pRemainingErrorPath, pState);
  }

  @Override
  public int getNumberOfInterpolationQueries() {
    return interpolationQueries;
  }

  private interface SymbolicStateReducer {
    ForgettingCompositeState reduce(ForgettingCompositeState successorState, ARGPath suffix)
        throws InterruptedException, CPAException;
  }

  private class ValuesOnlyReducer implements SymbolicStateReducer {

    @Override
    public ForgettingCompositeState reduce(
        ForgettingCompositeState pSuccessorState, ARGPath pSuffix)
        throws InterruptedException, CPAException {

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
  }

  private class ConstraintsOnlyReducer implements SymbolicStateReducer {

    @Override
    public ForgettingCompositeState reduce(
        ForgettingCompositeState pSuccessorState, ARGPath pSuffix)
        throws InterruptedException, CPAException {
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
  }

  private class ValuesFirstReducer implements SymbolicStateReducer {

    private SymbolicStateReducer valueReducer = new ValuesOnlyReducer();
    private SymbolicStateReducer constraintsReducer = new ConstraintsOnlyReducer();

    @Override
    public ForgettingCompositeState reduce(
        ForgettingCompositeState pSuccessorState, ARGPath pSuffix)
        throws InterruptedException, CPAException {
      return constraintsReducer.reduce(valueReducer.reduce(pSuccessorState, pSuffix), pSuffix);
    }
  }

  private class ConstraintsFirstReducer implements SymbolicStateReducer {

    private SymbolicStateReducer valueReducer = new ValuesOnlyReducer();
    private SymbolicStateReducer constraintsReducer = new ConstraintsOnlyReducer();

    @Override
    public ForgettingCompositeState reduce(
        ForgettingCompositeState pSuccessorState, ARGPath pSuffix)
        throws InterruptedException, CPAException {
      return valueReducer.reduce(constraintsReducer.reduce(pSuccessorState, pSuffix), pSuffix);
    }
  }

  private class AvoidConstraintsReducer implements SymbolicStateReducer {

    private SymbolicStateReducer delegate;

    AvoidConstraintsReducer(SymbolicStateReducer pDelegate) {
      delegate = pDelegate;
    }

    @Override
    public ForgettingCompositeState reduce(
        ForgettingCompositeState pSuccessorState, ARGPath pSuffix)
        throws InterruptedException, CPAException {

      ForgettingCompositeState reducedState = pSuccessorState;
      if (reducedState.getConstraintsSize() > 0) {
        reducedState = removeAllConstraints(reducedState);

        if (isPathFeasible(pSuffix, reducedState)) {
          reducedState = pSuccessorState;
        }
      }

      return delegate.reduce(reducedState, pSuffix);
    }

    private ForgettingCompositeState removeAllConstraints(final ForgettingCompositeState pState) {
      return new ForgettingCompositeState(
          pState.getValueState(), new ConstraintsState(new HashSet<>()));
    }
  }
}
