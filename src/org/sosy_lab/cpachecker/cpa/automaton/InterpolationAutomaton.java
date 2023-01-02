// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

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
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonExpression.StringExpression;
import org.sosy_lab.cpachecker.cpa.dca.DCAState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class InterpolationAutomaton {

  private static final boolean TARGET = true;

  private final String automatonName;
  private final FormulaManagerView fMgrView;

  private final ItpAutomatonState initState;
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
    itpStates = new ArrayList<>();

    if (pDistinctInterpolants.size() > 1) {
      throw new UnsupportedOperationException(
          "Interpolation automata with more than one interpolants are not yet supported");
    }

    for (BooleanFormula interpolant : pDistinctInterpolants) {
      itpStates.add(new ItpAutomatonState(this, "itp_state: " + interpolant, interpolant, TARGET));
    }

    bfToStateMap =
        ImmutableMap.<BooleanFormula, ItpAutomatonState>builderWithExpectedSize(
                itpStates.size() + 2)
            .put(initState.getInterpolant(), initState)
            .put(finalState.getInterpolant(), finalState)
            .putAll(
                itpStates.stream()
                    .collect(
                        ImmutableMap.toImmutableMap(
                            ItpAutomatonState::getInterpolant, Functions.identity())))
            .buildOrThrow();
  }

  public Automaton createAutomaton() throws InvalidAutomatonException {
    ImmutableList<AutomatonInternalState> internalStates =
        ImmutableList.<AutomatonInternalState>builderWithExpectedSize(itpStates.size() + 3)
            .add(initState.buildInternalState())
            .addAll(
                Collections3.transformedImmutableListCopy(
                    itpStates, ItpAutomatonState::buildInternalState))
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
    checkArgument(bfToStateMap.containsKey(pCurInterpolant));
    checkArgument(bfToStateMap.containsKey(pChildInterpolant));

    ItpAutomatonState srcState = bfToStateMap.get(pCurInterpolant);
    ItpAutomatonState destState = bfToStateMap.get(pChildInterpolant);

    srcState.createTransitionToState(pCurrentState, pChildState, destState.getStateName());
  }

  void addTransitionToInitState(
      ARGState pCurrentState, BooleanFormula pCurrentInterpolant, ARGState pChildState) {
    ItpAutomatonState srcState = bfToStateMap.get(pCurrentInterpolant);
    srcState.createTransitionToState(pCurrentState, pChildState, initState.getStateName());
  }

  void addTransitionToFinalState(
      ARGState pCurrentState, BooleanFormula pCurrentInterpolant, ARGState pChildState) {
    ItpAutomatonState srcState = bfToStateMap.get(pCurrentInterpolant);
    srcState.createTransitionToState(pCurrentState, pChildState, finalState.getStateName());
  }

  boolean isStateCovered(ARGState pState, ARGState pChildState, BooleanFormula pInterpolant) {
    checkArgument(bfToStateMap.containsKey(pInterpolant));

    return bfToStateMap.get(pInterpolant).isStateCovered(pState, pChildState);
  }

  void addQueryToCache(ARGState pState, ARGState pChildState, BooleanFormula pInterpolant) {
    checkArgument(bfToStateMap.containsKey(pInterpolant));

    bfToStateMap.get(pInterpolant).addExpressionToCache(pState, pChildState);
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

  private static class ItpAutomatonState {

    private final InterpolationAutomaton itpAutomaton;

    Set<AutomatonTransition> transitions;

    private final String stateName;
    private final BooleanFormula interpolant;
    private final boolean isTarget;

    private final @Nullable StringExpression targetInformation;

    private Set<AutomatonBoolExpr> boolExpressions;

    /**
     * Cache for expressions that were already queried before but are superfluous and would
     * unnecessarily bloat the automaton. They are hence intentionally not added to this state
     */
    private Set<AutomatonBoolExpr> coveredExpressionsCache;

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
      targetInformation = isTarget ? new StringExpression(itpAutomaton.getAutomatonName()) : null;

      boolExpressions = new HashSet<>();
      coveredExpressionsCache = new HashSet<>();
      transitions = new LinkedHashSet<>();
    }

    private String getStateName() {
      return stateName;
    }

    private AutomatonInternalState buildInternalState() {
      return new AutomatonInternalState(stateName, buildInternalTransitions(), isTarget, true);
    }

    private BooleanFormula getInterpolant() {
      return interpolant;
    }

    boolean isStateCovered(ARGState pState, ARGState pChildState) {
      Optional<AutomatonBoolExpr> boolExprOpt = buildAutomatonBoolExpr(pState, pChildState);
      if (boolExprOpt.isEmpty()) {
        return false;
      }
      AutomatonBoolExpr boolExpr = boolExprOpt.orElseThrow();
      return boolExpressions.contains(boolExpr) || coveredExpressionsCache.contains(boolExpr);
    }

    void addExpressionToCache(ARGState pState, ARGState pChildState) {
      Optional<AutomatonBoolExpr> boolExprOpt = buildAutomatonBoolExpr(pState, pChildState);

      if (boolExprOpt.isPresent()) {
        AutomatonBoolExpr boolExpr = boolExprOpt.orElseThrow();
        checkArgument(!boolExpressions.contains(boolExpr));
        coveredExpressionsCache.add(boolExpr);
      }
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
      CFAEdge cfaEdge = pCurrentState.getEdgeToChild(pChildState);
      if (cfaEdge == null) {
        return Optional.empty();
      }

      AutomatonBoolExpr boolExpr = buildAutomatonBoolExpr(cfaEdge, pChildState);
      addExpressionToState(boolExpr);
      addEdgeToTransition(boolExpr, pNextItpState);

      return Optional.of(cfaEdge);
    }

    private void addExpressionToState(AutomatonBoolExpr pBoolExpr) {
      coveredExpressionsCache.remove(pBoolExpr);
      boolExpressions.add(pBoolExpr);
    }

    private void addEdgeToTransition(AutomatonBoolExpr pBoolExpr, String pNextItpState) {
      AutomatonTransition.Builder builder =
          new AutomatonTransition.Builder(pBoolExpr, pNextItpState);
      if (targetInformation != null) {
        builder.withTargetInformation(targetInformation);
      }
      AutomatonTransition transition = builder.build();
      transitions.add(transition);
    }

    private Optional<AutomatonBoolExpr> buildAutomatonBoolExpr(
        ARGState pCurrentState, ARGState pChildState) {
      CFAEdge cfaEdge = pCurrentState.getEdgeToChild(pChildState);
      if (cfaEdge == null) {
        return Optional.empty();
      }

      return Optional.of(buildAutomatonBoolExpr(cfaEdge, pChildState));
    }

    private AutomatonBoolExpr buildAutomatonBoolExpr(CFAEdge pCfaEdge, ARGState pChildState) {
      AutomatonBoolExpr.MatchCFAEdgeNodes cfaEdgeBoolExpr =
          new AutomatonBoolExpr.MatchCFAEdgeNodes(pCfaEdge);

      DCAState dcaState = AbstractStates.extractStateByType(pChildState, DCAState.class);
      String buechiExpression =
          Joiner.on("; ")
              .join(Collections2.transform(dcaState.getAssumptions(), AExpression::toASTString));
      AutomatonBoolExpr.CPAQuery dcaStateQuery =
          new AutomatonBoolExpr.CPAQuery("DCAState", buechiExpression);

      return new AutomatonBoolExpr.And(cfaEdgeBoolExpr, dcaStateQuery);
    }

    List<AutomatonTransition> buildInternalTransitions() {
      Stream<AutomatonBoolExpr> stream =
          boolExpressions.stream().map(x -> new AutomatonBoolExpr.Negation(x));
      Optional<AutomatonBoolExpr> boolExprOpt =
          stream.reduce((x, y) -> new AutomatonBoolExpr.And(x, y));
      verify(boolExprOpt.isPresent());
      AutomatonTransition transition =
          new AutomatonTransition.Builder(boolExprOpt.orElseThrow(), stateName).build();

      transitions.add(transition);
      return ImmutableList.copyOf(transitions);
    }
  }

  private class InitState extends ItpAutomatonState {

    private InitState(InterpolationAutomaton pItpAutomaton) {
      super(pItpAutomaton, "init_state", fMgrView.getBooleanFormulaManager().makeTrue(), TARGET);
    }
  }

  private class FinalState extends ItpAutomatonState {

    private FinalState(InterpolationAutomaton pItpAutomaton) {
      super(pItpAutomaton, "final_state", fMgrView.getBooleanFormulaManager().makeFalse(), !TARGET);
    }

    @Override
    List<AutomatonTransition> buildInternalTransitions() {
      transitions.add(
          new AutomatonTransition.Builder(AutomatonBoolExpr.TRUE, AutomatonInternalState.BOTTOM)
              .build());
      return ImmutableList.copyOf(transitions);
    }
  }
}
