/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.automaton;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonAction.CPAModification;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonExpression.ResultValue;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

/**
 * A transition in the automaton implements one of the {@link PATTERN_MATCHING_METHODS}.
 * This determines if the transition matches on a certain {@link CFAEdge}.
 */
class AutomatonTransition {

  // The order of triggers, assertions and (more importantly) actions is preserved by the parser.
  private final AutomatonBoolExpr trigger;
  private final AutomatonBoolExpr assertion;
  private final ImmutableList<CStatement> assumption;
  private final ImmutableList<AutomatonAction> actions;


  /**
   * When the parser instances this class it can not assign a followstate because
   * that state might not be created (forward-reference).
   * Only the name is known in the beginning and the followstate relation must be
   * resolved by calling setFollowState() when all States are known.
   */
  private final String followStateName;
  private AutomatonInternalState followState = null;

  public AutomatonTransition(AutomatonBoolExpr pTrigger,
      List<AutomatonBoolExpr> pAssertions,
      List<AutomatonAction> pActions,
      String pFollowStateName) {
    this(pTrigger, pAssertions, ImmutableList.copyOf(new ArrayList<CStatement>()), pActions, pFollowStateName, null);
  }

  public AutomatonTransition(AutomatonBoolExpr pTrigger,
      List<AutomatonBoolExpr> pAssertions, List<AutomatonAction> pActions,
      AutomatonInternalState pFollowState) {

    this(pTrigger, pAssertions, ImmutableList.copyOf(new ArrayList<CStatement>()), pActions, pFollowState.getName(), pFollowState);
  }

  public AutomatonTransition(AutomatonBoolExpr pTrigger,
      List<AutomatonBoolExpr> pAssertions, List<CStatement> pAssumption,
      List<AutomatonAction> pActions,
      String pFollowStateName) {
    this(pTrigger, pAssertions, pAssumption, pActions, pFollowStateName, null);
  }

  public AutomatonTransition(AutomatonBoolExpr pTrigger,
      List<AutomatonBoolExpr> pAssertions, List<CStatement> pAssumption, List<AutomatonAction> pActions,
      AutomatonInternalState pFollowState) {

    this(pTrigger, pAssertions, pAssumption, pActions, pFollowState.getName(), pFollowState);
  }

  private AutomatonTransition(AutomatonBoolExpr pTrigger,
      List<AutomatonBoolExpr> pAssertions, List<CStatement> pAssumption, List<AutomatonAction> pActions,
      String pFollowStateName, AutomatonInternalState pFollowState) {

    this.trigger = checkNotNull(pTrigger);

    if(pAssumption == null) {
      this.assumption = ImmutableList.of();
    } else {
      this.assumption = ImmutableList.copyOf(pAssumption);
    }

    this.actions = ImmutableList.copyOf(pActions);
    this.followStateName = checkNotNull(pFollowStateName);
    this.followState = pFollowState;

    if (pAssertions.isEmpty()) {
      this.assertion = AutomatonBoolExpr.TRUE;
    } else {
      AutomatonBoolExpr lAssertion = null;
      for (AutomatonBoolExpr nextAssertion : pAssertions) {
        if (lAssertion == null) {
          // first iteration
          lAssertion = nextAssertion;
        } else {
          // other iterations
          lAssertion = new AutomatonBoolExpr.And(lAssertion, nextAssertion);
        }
      }
      this.assertion = lAssertion;
    }
  }

  /**
   * Resolves the follow-state relation for this transition.
   */
  void setFollowState(Map<String, AutomatonInternalState> pAllStates) throws InvalidAutomatonException {
    if (followState == null) {
      followState = pAllStates.get(followStateName);

      if (followState == null) {
        throw new InvalidAutomatonException("No Follow-State with name " + followStateName + " found.");
      }
    }
  }

  /** Determines if this Transition matches on the current State of the CPA.
   * This might return a <code>MaybeBoolean.MAYBE</code> value if the method cannot determine if the transition matches.
   * In this case more information (e.g. more AbstractStates of other CPAs) are needed.
   * @throws CPATransferException
   */
  public ResultValue<Boolean> match(AutomatonExpressionArguments pArgs) throws CPATransferException {
    return trigger.eval(pArgs);
  }

  /**
   * Checks if all assertions of this transition are fulfilled
   * in the current configuration of the automaton this method is called.
   * @throws CPATransferException
   */
  public ResultValue<Boolean> assertionsHold(AutomatonExpressionArguments pArgs) throws CPATransferException {
    return assertion.eval(pArgs);
  }

  /**
   * Executes all actions of this transition in the order which is defined in the automaton definition file.
   * @throws CPATransferException
   */
  public void executeActions(AutomatonExpressionArguments pArgs) throws CPATransferException {
    for (AutomatonAction action : actions) {
      ResultValue<? extends Object> res = action.eval(pArgs);
      if (res.canNotEvaluate()) {
        pArgs.getLogger().log(Level.SEVERE, res.getFailureMessage() + " in " + res.getFailureOrigin());
      }
    }
    if (pArgs.getLogMessage() != null && pArgs.getLogMessage().length() > 0) {
      pArgs.getLogger().log(Level.INFO, pArgs.getLogMessage());
      pArgs.clearLogMessage();
    }
  }

  /** Returns if the actions of this transiton can be executed on these AutomatonExpressionArguments.
   * If false is returned more Information is needed (probably more AbstractStates from other CPAs).
   * @param pArgs
   * @return
   * @throws CPATransferException
   */
  public boolean canExecuteActionsOn(AutomatonExpressionArguments pArgs) throws CPATransferException {
    for (AutomatonAction action : actions) {
      if (! action.canExecuteOn(pArgs)) {
        return false;
      }
    }
    return true;
  }

  /**
   * returns null if setFollowState() was not called or no followState with appropriate name was found.
   */
  public AutomatonInternalState getFollowState() {
    return followState;
  }

  public AutomatonBoolExpr getTrigger() {
    return trigger;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append('"');
    sb.append(trigger);
    sb.append(" -> ");
    if (!assertion.equals(AutomatonBoolExpr.TRUE)) {
      sb.append("ASSERT ");
      sb.append(assertion);
    }
    if (!actions.isEmpty()) {
      Joiner.on(' ').appendTo(sb, actions);
      sb.append(" ");
    }
    sb.append(followState);
    sb.append(";\"");
    return sb.toString();
  }

  /**
   * Returns true if this Transition fulfills the requirements of an ObserverTransition (does not use MODIFY or STOP).
   * @return
   */
  boolean meetsObserverRequirements() {
    // assert followstate != BOTTOM
    if (this.followState.equals(AutomatonInternalState.BOTTOM)) {
      return false;
    }
    // actions are not MODIFY actions
    for (AutomatonAction action : this.actions) {
      if ((action instanceof CPAModification)) {
        return false;
      }
    }
    return true;
  }

  public ImmutableList<CStatement> getAssumptions() {
    return assumption;
  }
}
