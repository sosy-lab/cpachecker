// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.util;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.components.state_transformer.AnyStateTransformer;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.block.BlockState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class WorkerAnalysisUtil {

  public static Optional<CFANode> abstractStateToLocation(AbstractState state) {
    if (state instanceof LocationState) {
      return Optional.of(((LocationState) state).getLocationNode());
    }
    if (state instanceof BlockState) {
      return Optional.of(((BlockState) state).getLocationNode());
    }
    if (state instanceof CompositeState) {
      for (AbstractState wrappedState : ((CompositeState) state).getWrappedStates()) {
        Optional<CFANode> maybeNode = abstractStateToLocation(wrappedState);
        if (maybeNode.isPresent()) {
          return maybeNode;
        }
      }
    }
    if (state.getClass().equals(ARGState.class)) {
      return abstractStateToLocation(((ARGState) state).getWrappedState());
    }
    return Optional.absent();
  }

  public static Map<AbstractState, BooleanFormula> transformReachedSet(ReachedSet reachedSet, CFANode targetNode, FormulaManagerView fmgr, AnalysisDirection direction, AnyStateTransformer transformer, String uniqueVariableId) {
    Map<AbstractState, BooleanFormula> formulas = new HashMap<>();
    for (AbstractState abstractState : reachedSet.getReached(targetNode)) {
      BooleanFormula formula = transformer.safeTransform(abstractState, fmgr, direction, uniqueVariableId);
      if (!fmgr.getBooleanFormulaManager().isTrue(formula)) {
        formulas.put(abstractState, formula);
      }
    }
    return formulas;
  }

  public static <T extends AbstractState> ImmutableSet<T> extractStateFromCompositeState(
      Class<T> pTarget,
      CompositeState pCompositeState) {
    Set<T> result = new HashSet<>();
    for (AbstractState wrappedState : pCompositeState.getWrappedStates()) {
      if (pTarget.isAssignableFrom(wrappedState.getClass())) {
        result.add(pTarget.cast(wrappedState));
      } else if (wrappedState instanceof CompositeState) {
        result.addAll(extractStateFromCompositeState(pTarget, (CompositeState) wrappedState));
      }
    }
    return ImmutableSet.copyOf(result);
  }

  public static CompositeState extractCompositeStateFromAbstractState(AbstractState state) {
    checkNotNull(state, "state cannot be null");
    checkState(state instanceof ARGState, "State has to be an ARGState");
    ARGState argState = (ARGState) state;
    checkState(argState.getWrappedState() instanceof CompositeState,
        "First state must contain a CompositeState");
    return (CompositeState) argState.getWrappedState();
  }

  public static <T extends ConfigurableProgramAnalysis> T extractAnalysis(
      ConfigurableProgramAnalysis pCPA,
      Class<T> pTarget) {
    ARGCPA argCpa = (ARGCPA) pCPA;
    if (argCpa.getWrappedCPAs().size() > 1) {
      throw new AssertionError("Wrapper expected but got " + pCPA + "instead");
    }
    if (!(argCpa.getWrappedCPAs().get(0) instanceof CompositeCPA)) {
      throw new AssertionError(
          "Expected " + CompositeCPA.class + " but got " + argCpa.getWrappedCPAs().get(0)
              .getClass());
    }
    CompositeCPA compositeCPA = (CompositeCPA) argCpa.getWrappedCPAs().get(0);
    for (ConfigurableProgramAnalysis wrappedCPA : compositeCPA.getWrappedCPAs()) {
      if (wrappedCPA.getClass().equals(pTarget)) {
        return pTarget.cast(wrappedCPA);
      }
    }
    throw new AssertionError(
        "Expected analysis " + pTarget + " is not part of the composite cpa " + pCPA);
  }

}
