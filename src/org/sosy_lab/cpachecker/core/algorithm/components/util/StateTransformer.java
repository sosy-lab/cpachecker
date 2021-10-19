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
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.block.BlockState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;

public class StateTransformer {

  public static Optional<CFANode> findCFANodeOfState(AbstractState state) {
    if (state instanceof LocationState) {
      return Optional.of(((LocationState) state).getLocationNode());
    }
    if (state instanceof BlockState) {
      return Optional.of(((BlockState) state).getLocationNode());
    }
    if (state instanceof CompositeState) {
      for (AbstractState wrappedState : ((CompositeState) state).getWrappedStates()) {
        Optional<CFANode> maybeNode = findCFANodeOfState(wrappedState);
        if (maybeNode.isPresent()) {
          return maybeNode;
        }
      }
    }
    if (state.getClass().equals(ARGState.class)) {
      return findCFANodeOfState(((ARGState) state).getWrappedState());
    }
    return Optional.absent();
  }

  public static Map<AbstractState, BooleanFormula> transformReachedSet(ReachedSet reachedSet, CFANode targetNode, FormulaManagerView fmgr) {
    Map<AbstractState, BooleanFormula> formulas = new HashMap<>();
    for (AbstractState abstractState : reachedSet.getReached(targetNode)) {
      BooleanFormula formula = transform(abstractState, fmgr);
      if (!fmgr.getBooleanFormulaManager().isTrue(formula)) {
        formulas.put(abstractState, formula);
      }
    }
    return formulas;
  }

  public static BooleanFormula transform(AbstractState state, FormulaManagerView fmgr) {
    if (state instanceof PredicateAbstractState) {
      return fmgr.uninstantiate(transform((PredicateAbstractState) state, fmgr));
    }
    if (state instanceof ValueAnalysisState) {
      return fmgr.uninstantiate(transform((ValueAnalysisState) state, fmgr));
    }
    if (state instanceof CompositeState) {
      return fmgr.uninstantiate(transform((CompositeState) state, fmgr));
    }
    if (state.getClass().equals(ARGState.class)) {
      return transform(((ARGState) state).getWrappedState(), fmgr);
    }
    return fmgr.getBooleanFormulaManager().makeTrue();
  }

  private static BooleanFormula transform(PredicateAbstractState state, FormulaManagerView fmgr) {
    PathFormula pathFormula = state.getPathFormula();
    SSAMap ssaMap = pathFormula.getSsa();
    Map<String, Formula> variableToFormula = fmgr.extractVariables(pathFormula.getFormula());
    Map<Formula, Formula> substitutions = new HashMap<>();
    for (Entry<String, Formula> stringFormulaEntry : variableToFormula.entrySet()) {
      String name = stringFormulaEntry.getKey();
      Formula formula = stringFormulaEntry.getValue();
      List<String> nameAndIndex = Splitter.on("@").limit(2).splitToList(name);
      if (nameAndIndex.size() < 2 || nameAndIndex.get(1).isEmpty()) {
        substitutions.put(formula, fmgr.makeVariable(fmgr.getFormulaType(formula), name));
        continue;
      }
      name = nameAndIndex.get(0);
      int index = Integer.parseInt(nameAndIndex.get(1));
      int highestIndex = ssaMap.getIndex(name);
      if (index != highestIndex) {
        substitutions.put(formula, fmgr.makeVariable(fmgr.getFormulaType(formula), name + "." + index));
      } else {
        substitutions.put(formula, fmgr.makeVariable(fmgr.getFormulaType(formula), name));
      }
    }
    return fmgr.substitute(pathFormula.getFormula(), substitutions);
  }

  private static BooleanFormula transform(ValueAnalysisState state, FormulaManagerView fmgr) {
    return state.getFormulaApproximation(fmgr);
  }

  private static BooleanFormula transform(CompositeState state, FormulaManagerView fmgr) {
    return state.getWrappedStates().stream().map(wrappedState -> transform(wrappedState, fmgr))
        .collect(fmgr.getBooleanFormulaManager().toConjunction());
  }

  public static <T extends AbstractState> ImmutableSet<T> getStatesFromCompositeState(
      CompositeState pCompositeState,
      Class<T> pTarget) {
    Set<T> result = new HashSet<>();
    for (AbstractState wrappedState : pCompositeState.getWrappedStates()) {
      if (pTarget.isAssignableFrom(wrappedState.getClass())) {
        result.add(pTarget.cast(wrappedState));
      } else if (wrappedState instanceof CompositeState) {
        result.addAll(getStatesFromCompositeState((CompositeState) wrappedState, pTarget));
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
