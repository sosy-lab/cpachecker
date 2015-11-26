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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import javax.annotation.Nullable;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.TrinaryEqualable.Equality;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonAction.CPAModification;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonExpression.ResultValue;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonSafetyPropertyFactory.AutomatonAssertionProperty;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableSet;

/**
 * A transition in the automaton implements one of the {@link PATTERN_MATCHING_METHODS}.
 * This determines if the transition matches on a certain {@link CFAEdge}.
 */
class AutomatonTransition {

  // The order of triggers, assertions and (more importantly) actions is preserved by the parser.

  private final AutomatonBoolExpr trigger;
  private final AutomatonBoolExpr assertion;

  private final boolean assumptionTruth;
  private final ImmutableList<AStatement> assumption;
  private final ImmutableList<AutomatonAction> actions;

  private final ImmutableSet<? extends SafetyProperty> violatedWhenEnteringTarget;
  private final ImmutableSet<? extends SafetyProperty> violatedWhenAssertionFailed;

  /**
   * When the parser instances this class it can not assign a followstate because
   * that state might not be created (forward-reference).
   * Only the name is known in the beginning and the followstate relation must be
   * resolved by calling setFollowState() when all States are known.
   */
  private final String followStateName;
  private AutomatonInternalState followState = null;

  public AutomatonTransition(AutomatonBoolExpr pTrigger,
      List<AutomatonAction> pActions, String pFollowStateName) {

    this(pTrigger,
        ImmutableList.<AutomatonBoolExpr>of(),
        ImmutableList.<AStatement>of(), true, pActions,
        pFollowStateName, null,
        ImmutableSet.<SafetyProperty>of(),
        ImmutableSet.<SafetyProperty>of());
  }

  public AutomatonTransition(AutomatonBoolExpr pTrigger, List<AutomatonBoolExpr> pAssertions,
      List<AutomatonAction> pActions, AutomatonInternalState pFollowState) {

    this(pTrigger, pAssertions, ImmutableList.<AStatement>of(), true, pActions, pFollowState.getName(), pFollowState,
        ImmutableSet.<SafetyProperty>of(),
        ImmutableSet.<SafetyProperty>of());
  }

  public AutomatonTransition(AutomatonBoolExpr pTrigger,
      List<AStatement> pAssumption,
      boolean pAssumeTruth,
      List<AutomatonAction> pActions,
      AutomatonInternalState pFollowState,
      Set<SafetyProperty> pViolatedWhenEnteringTarget) {

    this(pTrigger, ImmutableList.<AutomatonBoolExpr>of(),
        pAssumption, pAssumeTruth, pActions,
        pFollowState.getName(), pFollowState,
        Preconditions.checkNotNull(pViolatedWhenEnteringTarget),
        ImmutableSet.<SafetyProperty>of());
  }

  public AutomatonTransition(AutomatonBoolExpr pTrigger,
      List<AutomatonBoolExpr> pAssertions,
      List<AStatement> pAssumption,
      boolean pAssumeTruth,
      List<AutomatonAction> pActions,
      AutomatonInternalState pFollowState) {

    this(pTrigger, pAssertions,
        pAssumption, pAssumeTruth, pActions,
        pFollowState.getName(), pFollowState,
        ImmutableSet.<SafetyProperty>of(),
        ImmutableSet.<SafetyProperty>of());
  }

  public AutomatonTransition(AutomatonBoolExpr pTrigger,
      List<AutomatonBoolExpr> pAssertions,
      List<AStatement> pAssumption,
      boolean pAssumeTruth,
      List<AutomatonAction> pActions,
      AutomatonInternalState pFollowState,
      Set<SafetyProperty> pViolatedWhenEnteringTarget) {

    this(pTrigger, pAssertions,
        pAssumption, pAssumeTruth, pActions,
        pFollowState.getName(), pFollowState,
        Preconditions.checkNotNull(pViolatedWhenEnteringTarget),
        ImmutableSet.<SafetyProperty>of());
  }

  AutomatonTransition(AutomatonBoolExpr pTrigger,
      List<AutomatonBoolExpr> pAssertions,
      @Nullable List<AStatement> pAssumption,
      boolean pAssumeTruth,
      List<AutomatonAction> pActions,
      String pFollowStateName) {

    this(pTrigger, pAssertions, pAssumption, pAssumeTruth, pActions, pFollowStateName, null,
        ImmutableSet.<SafetyProperty>of(),
        ImmutableSet.<SafetyProperty>of());
  }

  public AutomatonTransition(AutomatonBoolExpr pTrigger,
      List<AutomatonBoolExpr> pAssertions,
      @Nullable List<AStatement> pAssumption,
      boolean pAssumeTruth,
      List<AutomatonAction> pActions,
      String pFollowStateName,
      @Nullable AutomatonInternalState pFollowState,
      Set<? extends SafetyProperty> pViolatedWhenEnteringTarget,
      Set<? extends SafetyProperty> pViolatedWhenAssertionFailed) {

    this.trigger = checkNotNull(pTrigger);

    if (pAssumption == null) {
      this.assumption = ImmutableList.of();
      this.assumptionTruth = true;
    } else {
      this.assumption = ImmutableList.<AStatement>copyOf(pAssumption);
      this.assumptionTruth = pAssumeTruth;
    }

    this.actions = ImmutableList.copyOf(pActions);
    this.followStateName = checkNotNull(pFollowStateName);
    this.followState = pFollowState;
    this.violatedWhenEnteringTarget = ImmutableSet.copyOf(pViolatedWhenEnteringTarget);

    if (pAssertions.isEmpty()) {

      this.assertion = AutomatonBoolExpr.TRUE;
      this.violatedWhenAssertionFailed = ImmutableSet.of();

    } else {

      if (pViolatedWhenAssertionFailed.isEmpty()) {
        this.violatedWhenAssertionFailed = ImmutableSet.of(new AutomatonAssertionProperty());
      } else {
        this.violatedWhenAssertionFailed = ImmutableSet.copyOf(pViolatedWhenAssertionFailed);
      }

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

  public ImmutableList<AutomatonAction> getActions() {
    return actions;
  }


  public AutomatonBoolExpr getAssertion() {
    return assertion;
  }

  public Equality isEquivalentTo(PlainAutomatonTransition pT) {

    switch(pT.assertion.equalityTo(this.assertion)) {
    case UNEQUAL:
      return Equality.UNEQUAL;
    case UNKNOWN:
      return Equality.UNKNOWN;
    }

    switch(pT.trigger.equalityTo(this.trigger)) {
    case UNEQUAL:
      return Equality.UNEQUAL;
    case UNKNOWN:
      return Equality.UNKNOWN;
    }

    if (!this.violatedWhenEnteringTarget.equals(pT.violatedWhenEnteringTarget)) {
      return Equality.UNEQUAL;
    }

    if (!this.violatedWhenAssertionFailed.equals(pT.violatedWhenAssertionFailed)) {
      return Equality.UNEQUAL;
    }

    if (this.assumption == null) {
      if (pT.assumption != null) {
        return Equality.UNEQUAL;
      }
    } else {
      if (!this.assumption.equals(pT.assumption)) {
        return Equality.UNEQUAL;
      }
    }

    if (this.actions == null) {
      if (pT.actions != null) {
        return Equality.UNEQUAL;
      }
    } else {
      if (!this.actions.equals(pT.assumption)) {
        return Equality.UNEQUAL;
      }
    }

    return Equality.EQUAL;
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
      ResultValue<?> res = action.eval(pArgs);
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

  public ImmutableSet<? extends SafetyProperty> getViolatedWhenAssertionFailed() {
    return violatedWhenAssertionFailed;
  }

  public ImmutableSet<? extends SafetyProperty> getViolatedWhenEnteringTarget() {
    return violatedWhenEnteringTarget;
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

  public ImmutableList<AStatement> getAssumptions() {
    return assumption;
  }

  public ImmutableList<Pair<AStatement, Boolean>> getAssumptionWithTruth() {
    Builder<Pair<AStatement, Boolean>> result = ImmutableList.<Pair<AStatement, Boolean>>builder();
    for (AStatement stmt: this.assumption) {
      result.add(Pair.of(stmt, assumptionTruth));
    }
    return result.build();
  }


  /**
   * In some cases, we do not want AutomatonTransition because it encodes
   *    the follower state.
   *    And we do not want to get into trouble with 'equals' (therefore we do not use inheritance)
   */
  public static final class PlainAutomatonTransition {

    final AutomatonBoolExpr trigger;
    final AutomatonBoolExpr assertion;
    final boolean assumptionTruth;
    final ImmutableList<AStatement> assumption;
    final ImmutableList<AutomatonAction> actions;
    final ImmutableSet<? extends SafetyProperty> violatedWhenEnteringTarget;
    final ImmutableSet<? extends SafetyProperty> violatedWhenAssertionFailed;

    public PlainAutomatonTransition(AutomatonBoolExpr pTrigger, AutomatonBoolExpr pAssertion,
        ImmutableList<AStatement> pAssumption, ImmutableList<AutomatonAction> pActions,
        ImmutableSet<? extends SafetyProperty> pViolatedWhenEnteringTarget,
        ImmutableSet<? extends SafetyProperty> pViolatedWhenAssertionFailed) {

      assumptionTruth = true;
      trigger = Preconditions.checkNotNull(pTrigger);
      assertion = Preconditions.checkNotNull(pAssertion);
      assumption = Preconditions.checkNotNull(pAssumption);
      actions = Preconditions.checkNotNull(pActions);
      violatedWhenEnteringTarget = Preconditions.checkNotNull(pViolatedWhenEnteringTarget);
      violatedWhenAssertionFailed = Preconditions.checkNotNull(pViolatedWhenAssertionFailed);
    }

    public static PlainAutomatonTransition deriveFrom(AutomatonTransition pT) {
      return new PlainAutomatonTransition(
          pT.trigger,
          pT.assertion,
          pT.assumption,
          pT.actions,
          pT.violatedWhenEnteringTarget,
          pT.violatedWhenAssertionFailed);
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + actions.hashCode();
      result = prime * result + assertion.hashCode();
      result = prime * result + assumption.hashCode();
      result = prime * result + trigger.hashCode();
      result = prime * result + violatedWhenEnteringTarget.hashCode();
      result = prime * result + violatedWhenAssertionFailed.hashCode();
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      PlainAutomatonTransition other = (PlainAutomatonTransition) obj;

      if (!actions.equals(other.actions)) {
        return false;
      }

      if (!assertion.equals(other.assertion)) {
        return false;
      }

      if (!assumption.equals(other.assumption)) {
        return false;
      }

      if (!trigger.equals(other.trigger)) {
        return false;
      }

      if (!violatedWhenEnteringTarget.equals(other.violatedWhenEnteringTarget)) {
        return false;
      }

      if (!violatedWhenAssertionFailed.equals(other.violatedWhenAssertionFailed)) {
        return false;
      }


      return true;
    }

  }

}
