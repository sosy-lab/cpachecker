// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.automaton;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.sosy_lab.common.UniqueIdGenerator;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonBoolExpr.MatchOtherwise;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonExpression.StringExpression;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;

/** Represents a State in the automaton. */
public class AutomatonInternalState {
  // the StateId is used to identify States in GraphViz
  private static final UniqueIdGenerator idGenerator = new UniqueIdGenerator();
  private final int stateId = idGenerator.getFreshId();

  /** State representing BOTTOM */
  static final AutomatonInternalState BOTTOM =
      new AutomatonInternalState("_predefinedState_BOTTOM", ImmutableList.of()) {
        @Override
        public String toString() {
          return "STOP";
        }
      };

  /** Error State */
  public static final AutomatonInternalState ERROR =
      new AutomatonInternalState(
          "_predefinedState_ERROR",
          Collections.singletonList(
              new AutomatonTransition.Builder(AutomatonBoolExpr.TRUE, BOTTOM)
                  .withTargetInformation(new StringExpression(""))
                  .build()),
          AutomatonStateTypes.TARGET,
          false,
          false,
          ExpressionTrees.getTrue()) {
        @Override
        public String toString() {
          return "ERROR";
        }
      };

  /** Break state, used to halt the analysis without being a target state */
  public static final AutomatonInternalState BREAK =
      new AutomatonInternalState(
          "_predefinedState_BREAK",
          Collections.singletonList(
              new AutomatonTransition.Builder(AutomatonBoolExpr.TRUE, BOTTOM).build()),
          AutomatonStateTypes.ANY,
          false,
          false,
          ExpressionTrees.getTrue());

  /** Name of this State. */
  private final String name;

  /** Outgoing transitions of this state. */
  private final ImmutableList<AutomatonTransition> transitions;

  private final boolean mIsTarget;

  /** determines if all transitions of the state are considered or only the first that matches */
  private final boolean mAllTransitions;

  private final boolean isCycleStart;

  /** The list of state invariants for the node */
  private final ExpressionTree<AExpression> stateInvariants;

  private final AutomatonStateTypes stateType;

  public AutomatonInternalState(
      String pName,
      List<AutomatonTransition> pTransitions,
      AutomatonStateTypes pStateType,
      boolean pAllTransitions,
      boolean pIsCycleStart,
      ExpressionTree<AExpression> pStateInvariants) {
    this.name = pName;
    this.transitions = ImmutableList.copyOf(postProcessTransitions(pTransitions));
    this.stateType = pStateType;
    this.mIsTarget = pStateType == AutomatonStateTypes.TARGET;
    this.mAllTransitions = pAllTransitions;
    this.isCycleStart = pIsCycleStart;
    this.stateInvariants = pStateInvariants;
  }

  /**
   * If there is an otherwise-edge, add all other triggers to the otherwise-edge
   *
   * @param pTransitions the transitions of the edge
   * @return the transitions, where the otherwise edge is updated
   */
  private List<AutomatonTransition> postProcessTransitions(List<AutomatonTransition> pTransitions) {
    Optional<AutomatonTransition> otherwiseOpt =
        pTransitions.stream().filter(t -> t.getTrigger() instanceof MatchOtherwise).findFirst();
    if (otherwiseOpt.isPresent()) {
      MatchOtherwise otherwiseTrigger = (MatchOtherwise) otherwiseOpt.orElseThrow().getTrigger();
      otherwiseTrigger.addOtherExpressions(
          pTransitions.stream()
              .map(t -> t.getTrigger())
              .filter(pTrigger -> !(pTrigger instanceof MatchOtherwise))
              .collect(ImmutableList.toImmutableList()));
    }
    return pTransitions;
  }

  public AutomatonInternalState(
      String pName,
      List<AutomatonTransition> pTransitions,
      AutomatonStateTypes pStateTypes,
      boolean pAllTransitions) {
    this(pName, pTransitions, pStateTypes, pAllTransitions, false, ExpressionTrees.getTrue());
  }

  public AutomatonInternalState(
      String pName,
      List<AutomatonTransition> pTransitions,
      AutomatonStateTypes pStateTypes,
      boolean pAllTransitions,
      ExpressionTree<AExpression> pStateInvariants) {
    this(pName, pTransitions, pStateTypes, pAllTransitions, false, pStateInvariants);
  }

  public AutomatonInternalState(String pName, List<AutomatonTransition> pTransitions) {
    this(pName, pTransitions, AutomatonStateTypes.ANY, false, false, ExpressionTrees.getTrue());
  }

  public AutomatonInternalState(
      String pName,
      List<AutomatonTransition> pNewTransitions,
      boolean pTarget,
      boolean pNonDetState,
      ExpressionTree<AExpression> pInvCnds) {
    this(
        pName,
        pNewTransitions,
        pTarget ? AutomatonStateTypes.TARGET : AutomatonStateTypes.ANY,
        pNonDetState,
        pInvCnds);
  }

  public AutomatonInternalState(
      String pStateName,
      List<AutomatonTransition> pTransitions,
      boolean pTarget,
      boolean pAllTransitions) {
    this(
        pStateName,
        pTransitions,
        pTarget ? AutomatonStateTypes.TARGET : AutomatonStateTypes.ANY,
        pAllTransitions);
  }

  public AutomatonInternalState(
      String pStateName,
      List<AutomatonTransition> pTransitionList,
      boolean pTarget,
      boolean pAllTransitions,
      boolean pIsCycleStart,
      ExpressionTree<AExpression> pInvCnds) {
    this(
        pStateName,
        pTransitionList,
        pTarget ? AutomatonStateTypes.TARGET : AutomatonStateTypes.ANY,
        pAllTransitions,
        pIsCycleStart,
        pInvCnds);
  }

  public boolean isNonDetState() {
    return mAllTransitions;
  }

  public boolean isNontrivialCycleStart() {
    return isCycleStart;
  }

  public AutomatonStateTypes getStateType() {
    return stateType;
  }

  /**
   * Lets all outgoing transitions of this state resolve their "sink" states.
   *
   * @param pAllStates map of all states of this automaton.
   */
  void setFollowStates(Map<String, AutomatonInternalState> pAllStates)
      throws InvalidAutomatonException {
    for (AutomatonTransition t : transitions) {
      t.setFollowState(pAllStates);
    }
  }

  public String getName() {
    return name;
  }
  /** Returns a integer representation of this state. */
  public int getStateId() {
    return stateId;
  }

  public boolean isTarget() {
    return mIsTarget;
  }

  public ExpressionTree<AExpression> getStateInvariants() {
    return stateInvariants;
  }

  /** Returns is it a state in that we will remain the rest of the time?. */
  public boolean isFinalSelfLoopingState() {
    if (transitions.size() == 1) {
      AutomatonTransition tr = transitions.get(0);
      if (tr.getFollowState().equals(this)) {
        return true;
      }
    }

    return false;
  }

  public ImmutableList<AutomatonTransition> getTransitions() {
    return transitions;
  }

  ImmutableList<AutomatonInternalState> getSuccessorStates() {
    return transformedImmutableListCopy(transitions, t -> t.getFollowState());
  }

  @Override
  public String toString() {
    return name + "(" + this.stateType + ")";
  }

  public boolean nontriviallyMatches(final CFAEdge pEdge, final LogManager pLogger) {
    for (AutomatonTransition trans : transitions) {
      if (trans.nontriviallyMatches(pEdge, pLogger)) {
        return true;
      }
    }
    return false;
  }

  public boolean nontriviallyMatchesAndEndsIn(
      final CFAEdge pEdge, final String pSuccessorName, final LogManager pLogger) {
    for (AutomatonTransition trans : transitions) {
      if (trans.getFollowState().getName().equals(pSuccessorName)
          && trans.nontriviallyMatches(pEdge, pLogger)) {
        return true;
      }
    }
    return false;
  }
}
