/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.cpa.automaton;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Verify.verify;

import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.collect.Collections3;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonBoolExpr.And;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonExpression.StringExpression;
import org.sosy_lab.cpachecker.cpa.dca.DCAState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class InterpolationAutomaton {

  private static final boolean IS_TARGET = true;

  private final String automatonName;
  private final FormulaManagerView fMgrView;

  private final ItpAutomatonState initState;
  private final ItpAutomatonState sinkState;
  private final ItpAutomatonState finalState;
  private final List<ItpAutomatonState> itpStates;

  private final ImmutableMap<BooleanFormula, ItpAutomatonState> bfToStateMap;

  InterpolationAutomaton(
      FormulaManagerView pMgrView,
      String pAutomatonName,
      ImmutableList<BooleanFormula> pDistinctInterpolants) {
    fMgrView = checkNotNull(pMgrView);
    automatonName = checkNotNull(pAutomatonName);

    initState = new InitState(this);
    finalState = new FinalState(this);
    sinkState = new SinkState(this);
    itpStates = new ArrayList<>();

    if (pDistinctInterpolants.size() > 1) {
      throw new UnsupportedOperationException(
          "Interpolation automata with more than one interpolants are not yet supported");
    }

    for (BooleanFormula interpolant : pDistinctInterpolants) {
      itpStates.add(
          new ItpAutomatonState(this, "itp_state: " + interpolant, interpolant, IS_TARGET));
    }

    bfToStateMap =
        ImmutableMap.<BooleanFormula, ItpAutomatonState>builderWithExpectedSize(
                itpStates.size() + 2)
            .put(initState.getInterpolant(), initState)
            .put(finalState.getInterpolant(), finalState)
            .putAll(
                itpStates
                    .stream()
                    .collect(
                        ImmutableMap.toImmutableMap(
                            ItpAutomatonState::getInterpolant, Functions.identity())))
            .build();
  }

  public Automaton createAutomaton() throws InvalidAutomatonException {
    ImmutableList<AutomatonInternalState> internalStates =
        ImmutableList.<AutomatonInternalState>builderWithExpectedSize(itpStates.size() + 3)
            .add(initState.buildInternalState())
            .addAll(
                Collections3.transformedImmutableListCopy(
                    itpStates, ItpAutomatonState::buildInternalState))
            .add(sinkState.buildInternalState())
            .add(finalState.buildInternalState())
            .build();
    return new Automaton(
        automatonName, ImmutableMap.of(), internalStates, initState.getStateName());
  }

  String getAutomatonName() {
    return automatonName;
  }

  ImmutableList<BooleanFormula> getDistinctInterpolants() {
    return Collections3.transformedImmutableListCopy(itpStates, ItpAutomatonState::getInterpolant);
  }

  void addTransition(
      ARGState pCurrentState,
      BooleanFormula pCurInterpolant,
      ARGState pChildState,
      BooleanFormula pChildInterpolant) {
    ItpAutomatonState srcState = bfToStateMap.get(pCurInterpolant);
    ItpAutomatonState destState = bfToStateMap.get(pChildInterpolant);

    srcState.createTransitionToState(pCurrentState, pChildState, destState.getStateName());
  }

  void addTransitionToSinkState(
      ARGState pCurrentState, BooleanFormula pCurrentInterpolant, ARGState pChildState) {
    ItpAutomatonState srcState = bfToStateMap.get(pCurrentInterpolant);
    srcState.createTransitionToState(pCurrentState, pChildState, sinkState.getStateName());
  }

  void addTransitionToFinalState(
      ARGState pCurrentState, BooleanFormula pCurrentInterpolant, ARGState pChildState) {
    ItpAutomatonState srcState = bfToStateMap.get(pCurrentInterpolant);
    srcState.createTransitionToState(pCurrentState, pChildState, finalState.getStateName());
  }

  @Override
  public String toString() {
    final StringBuilder str = new StringBuilder();

    str.append("INTERPOLATION AUTOMATON ").append(automatonName).append("\n\n");

    str.append("INITIAL STATE ").append(initState.getStateName()).append(";\n\n");

    for (ItpAutomatonState s : bfToStateMap.values()) {
      str.append("STATE ").append("USEALL ").append(s.getStateName()).append(":\n");
      for (AutomatonTransition t : s.transitions) {
        str.append("    ").append(t).append("GOTO ").append(t.getFollowStateName()).append(";\n");
      }
      str.append("\n");
    }

    str.append("END AUTOMATON\n");

    return str.toString();
  }

  private class ItpAutomatonState {

    private final InterpolationAutomaton itpAutomaton;

    private final String stateName;
    private final BooleanFormula interpolant;
    private final boolean isTarget;

    private final @Nullable StringExpression violatedPropertyDescription;

    private Set<AutomatonBoolExpr> boolExpressions;
    Set<AutomatonTransition> transitions;

    private ItpAutomatonState(
        InterpolationAutomaton pItpAutomaton,
        String pStateName,
        BooleanFormula pInterpolant,
        boolean pIsTarget) {
      itpAutomaton = checkNotNull(pItpAutomaton);
      checkArgument(!isNullOrEmpty(pStateName));
      stateName = pStateName;
      interpolant = checkNotNull(pInterpolant);
      isTarget = pIsTarget;
      violatedPropertyDescription =
          isTarget ? new StringExpression(itpAutomaton.getAutomatonName()) : null;

      boolExpressions = new HashSet<>();
      transitions = new LinkedHashSet<>();
    }

    String getStateName() {
      return stateName;
    }

    AutomatonInternalState buildInternalState() {
      return new AutomatonInternalState(stateName, buildInternalTransitions(), isTarget, true);
    }

    BooleanFormula getInterpolant() {
      return interpolant;
    }

    @Override
    public String toString() {
      String join = "";
      if (!transitions.isEmpty()) {
        join = ":\n" + Joiner.on("\n").join(transitions);
      }
      return stateName + join;
    }

    private List<CFAEdge> createTransitionToState(
        ARGState pCurrentState, ARGState pChildState, String pNextItpState) {
      Optional<CFAEdge> singleEdgeOpt = handleSingleEdge(pCurrentState, pChildState, pNextItpState);
      if (singleEdgeOpt.isPresent()) {
        return ImmutableList.of(singleEdgeOpt.orElseThrow());
      } else {
        // aggregateBasicBlocks is enabled!
        throw new UnsupportedOperationException(
            "AggregateBasicBlocks are currently not supported.");
      }
    }

    private Optional<CFAEdge> handleSingleEdge(
        ARGState pCurrentState, ARGState pChildState, String pNextItpState) {
      Optional<CFAEdge> singleEdgeOpt =
          Optional.ofNullable(pCurrentState.getEdgeToChild(pChildState));
      if (singleEdgeOpt.isPresent()) {
        DCAState dcaState = AbstractStates.extractStateByType(pChildState, DCAState.class);
        String buechiExpression =
            Joiner.on("; ")
                .join(Collections2.transform(dcaState.getAssumptions(), AExpression::toASTString));
        addEdgeToTransition(singleEdgeOpt.orElseThrow(), pNextItpState, buechiExpression);
      }
      return singleEdgeOpt;
    }

    private void addEdgeToTransition(
        CFAEdge pEdge, String pNextItpState, String pBuechiExpression) {
      AutomatonTransition transition =
          matchStateTransition(pEdge, pNextItpState, pBuechiExpression);
      transitions.add(transition);
    }

    private AutomatonTransition matchStateTransition(
        CFAEdge pCFAEdge, String pNextItpState, String pBuechiExpression) {
      AutomatonBoolExpr.MatchCFAEdgeNodes trigger =
          new AutomatonBoolExpr.MatchCFAEdgeNodes(pCFAEdge);
      AutomatonBoolExpr.CPAQuery matchCFAEdgeNodes =
          new AutomatonBoolExpr.CPAQuery("DCAState", pBuechiExpression);
      And and = new AutomatonBoolExpr.And(trigger, matchCFAEdgeNodes);
      boolExpressions.add(and);
      AutomatonTransition.Builder builder = new AutomatonTransition.Builder(and, pNextItpState);
      if (violatedPropertyDescription != null) {
        builder.withViolatedPropertyDescription(violatedPropertyDescription);
      }
      return builder.build();
    }

    List<AutomatonTransition> buildInternalTransitions() {
      Stream<AutomatonBoolExpr> stream =
          boolExpressions.stream().map(x -> new AutomatonBoolExpr.Negation(x));
      Optional<AutomatonBoolExpr> boolExprOpt =
          stream.reduce((x, y) -> new AutomatonBoolExpr.And(x, y));
      verify(boolExprOpt.isPresent());
      AutomatonTransition transition =
          new AutomatonTransition.Builder(boolExprOpt.orElseThrow(), sinkState.getStateName())
              .build();

      transitions.add(transition);
      return ImmutableList.copyOf(transitions);
    }
  }

  private class InitState extends ItpAutomatonState {

    private InitState(InterpolationAutomaton pItpAutomaton) {
      super(pItpAutomaton, "init_state", makeFormulaTrue(), IS_TARGET);
    }
  }

  private class FinalState extends ItpAutomatonState {

    private FinalState(InterpolationAutomaton pItpAutomaton) {
      super(
          pItpAutomaton,
          "final_state",
          fMgrView.getBooleanFormulaManager().makeFalse(),
          !IS_TARGET);
    }

    @Override
    List<AutomatonTransition> buildInternalTransitions() {
      transitions.add(
          new AutomatonTransition.Builder(AutomatonBoolExpr.TRUE, AutomatonInternalState.BOTTOM)
              .build());
      return ImmutableList.copyOf(transitions);
    }
  }

  private class SinkState extends ItpAutomatonState {

    private SinkState(InterpolationAutomaton pItpAutomaton) {
      super(pItpAutomaton, "sink_state", makeFormulaTrue(), IS_TARGET);
    }

    @Override
    List<AutomatonTransition> buildInternalTransitions() {
      transitions.add(
          new AutomatonTransition.Builder(AutomatonBoolExpr.TRUE, sinkState.getStateName())
              .build());
      return ImmutableList.copyOf(transitions);
    }
  }

  private BooleanFormula makeFormulaTrue() {
    return fMgrView.getBooleanFormulaManager().makeTrue();
  }
}
