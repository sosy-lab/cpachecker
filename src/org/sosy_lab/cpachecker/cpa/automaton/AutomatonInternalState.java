// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.automaton;

import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.sosy_lab.common.UniqueIdGenerator;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonBoolExpr.CheckPassesThroughNodes;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonBoolExpr.Negation;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonExpression.StringExpression;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CFAUtils;

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
  static final AutomatonInternalState ERROR =
      new AutomatonInternalState(
          "_predefinedState_ERROR",
          Collections.singletonList(
              new AutomatonTransition.Builder(AutomatonBoolExpr.TRUE, BOTTOM)
                  .withTargetInformation(new StringExpression(""))
                  .build()),
          true,
          false,
          false) {
        @Override
        public String toString() {
          return "ERROR";
        }
      };

  /** Break state, used to halt the analysis without being a target state */
  static final AutomatonInternalState BREAK =
      new AutomatonInternalState(
          "_predefinedState_BREAK",
          Collections.singletonList(
              new AutomatonTransition.Builder(AutomatonBoolExpr.TRUE, BOTTOM).build()),
          false,
          false,
          false);

  /** Name of this State. */
  private final String name;

  /** Outgoing transitions of this state. */
  private final ImmutableList<AutomatonTransition> transitions;

  private final boolean mIsTarget;

  /** determines if all transitions of the state are considered or only the first that matches */
  private final boolean mAllTransitions;

  private final boolean isCycleStart;

  public AutomatonInternalState(
      String pName,
      List<AutomatonTransition> pTransitions,
      boolean pIsTarget,
      boolean pAllTransitions,
      boolean pIsCycleStart) {
    name = pName;
    transitions = ImmutableList.copyOf(pTransitions);
    mIsTarget = pIsTarget;
    mAllTransitions = pAllTransitions;
    isCycleStart = pIsCycleStart;
  }

  public AutomatonInternalState(
      String pName,
      List<AutomatonTransition> pTransitions,
      boolean pIsTarget,
      boolean pAllTransitions) {
    this(pName, pTransitions, pIsTarget, pAllTransitions, false);
  }

  public AutomatonInternalState(String pName, List<AutomatonTransition> pTransitions) {
    this(pName, pTransitions, false, false, false);
  }

  public boolean isNonDetState() {
    return mAllTransitions;
  }

  public boolean isNontrivialCycleStart() {
    return isCycleStart;
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

  @Override
  public String toString() {
    return name;
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

  public AutomatonInternalState flip() {
    ImmutableList.Builder<AutomatonTransition> ptransitions =
        ImmutableList.<AutomatonTransition>builder();
        transitions.stream().map(t -> {
          if(t.getFollowState().isTarget()){
            AutomatonTransition.Builder atBuilder =
          new AutomatonTransition.Builder(t.getTrigger(), t.getFollowState().getName());
          atBuilder.copy(t, t.getFollowState().getName(), t.getFollowState(), new StringExpression("selfloop"));
          return new AutomatonTransition(atBuilder);
          }
          return t;
        }).forEach(ptransitions::add);
    return new AutomatonInternalState(
        name, ptransitions.build(), !mIsTarget, mAllTransitions, isCycleStart);
  }

  public AutomatonInternalState addselfloop(CFA pCFA) {
  ImmutableList.Builder<AutomatonTransition> ptransitions = ImmutableList.builder();

  AutomatonInternalState AlwaysTrue = new AutomatonInternalState(
      "AlwaysTrue", ImmutableList.of(), true, mAllTransitions, isCycleStart);

  for (AutomatonTransition transition : getTransitions()) {
    if (!(transition.getTrigger() instanceof CheckPassesThroughNodes trigger)) {
      ptransitions.add(transition);
      continue;
    }

    boolean transitionHandled = false;

    for (CFANode node : pCFA.nodes()) {
      for (CFAEdge edge : CFAUtils.leavingEdges(node)) {
        if (!(edge instanceof CAssumeEdge)) {
          continue;
        }

       AutomatonExpressionArguments args = new AutomatonExpressionArguments(null, null, null, edge, null);


        try {
          if (transition.getTrigger().eval(args).getValue()) {
            transitionHandled = true;

            if(transition.getFollowStateName() != "_predefinedState_BOTTOM"){
              AutomatonBoolExpr negatedTrigger = new Negation(trigger);
              AutomatonTransition negatedTransition =
                  new AutomatonTransition.Builder(negatedTrigger, transition.getFollowState()).build();
              ptransitions.add(negatedTransition);

              AutomatonTransition toAlwaysTrue =
                  new AutomatonTransition.Builder(trigger, AlwaysTrue).build();
              ptransitions.add(toAlwaysTrue);
            }
          }
        } catch (CPATransferException e) {
          e.printStackTrace();
        }
      }
    }

    if (!transitionHandled) {
      ptransitions.add(transition);
    }
  }

  return new AutomatonInternalState(
      name, ptransitions.build(), mIsTarget, mAllTransitions, isCycleStart);
}

  public AutomatonInternalState replaceall(ImmutableList<AutomatonTransition> pList) {
    return new AutomatonInternalState(name, pList, mIsTarget, mAllTransitions, isCycleStart);
  }
}
