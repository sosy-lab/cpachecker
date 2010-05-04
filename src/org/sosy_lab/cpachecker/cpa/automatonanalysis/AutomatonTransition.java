/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.automatonanalysis;

import java.io.PrintStream;
import java.util.List;
import java.util.logging.Level;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cpa.automatonanalysis.AutomatonActionExpr.CPAModification;
import org.sosy_lab.cpachecker.cpa.automatonanalysis.AutomatonBoolExpr.MaybeBoolean;

import com.google.common.collect.ImmutableList;

/**
 * A transition in the automaton implements one of the {@link PATTERN_MATCHING_METHODS}.
 * This determines if the transition matches on a certain {@link CFAEdge}.
 * @author rhein
 */
class AutomatonTransition {

  // The order of triggers, assertions and (more importantly) actions is preserved by the parser.
  private final List<AutomatonBoolExpr> triggers;
  private final List<AutomatonBoolExpr> assertions;
  private final List<AutomatonActionExpr> actions;

  /**
   * When the parser instances this class it can not assign a followstate because
   * that state might not be created (forward-reference).
   * Only the name is known in the beginning and the followstate relation must be
   * resolved by calling setFollowState() when all States are known.
   */
  private final String followStateName;
  private AutomatonInternalState followState = null;

  public AutomatonTransition(List<AutomatonBoolExpr> pTriggers, List<AutomatonBoolExpr> pAssertions, List<AutomatonActionExpr> pActions,
      String pFollowStateName) {
    this.triggers = ImmutableList.copyOf(pTriggers);
    this.assertions = ImmutableList.copyOf(pAssertions);
    this.actions = ImmutableList.copyOf(pActions);
    this.followStateName = pFollowStateName;
  }

  public AutomatonTransition(List<AutomatonBoolExpr> pTriggers,
      List<AutomatonBoolExpr> pAssertions, List<AutomatonActionExpr> pActions,
      AutomatonInternalState pFollowState) {
    this.triggers = ImmutableList.copyOf(pTriggers);
    this.assertions = ImmutableList.copyOf(pAssertions);
    this.actions = ImmutableList.copyOf(pActions);
    this.followState = pFollowState;
    this.followStateName = pFollowState.getName();
  }

  /**
   * Resolves the follow-state relation for this transition.
   */
  public void setFollowState(List<AutomatonInternalState> pAllStates) throws InvalidAutomatonException {
    if (this.followState == null) {
      for (AutomatonInternalState s : pAllStates) {
        if (s.getName().equals(followStateName)) {
          this.followState = s;
          return;
        }
      }
      throw new InvalidAutomatonException("No Follow-State with name " + followStateName + " found.");
    }
  }

  /** Writes a representation of this transition (as edge) in DOT file format to the argument {@link PrintStream}.
   */
  void writeTransitionToDotFile(int sourceStateId, PrintStream out) {
    out.println(sourceStateId + " -> " + followState.getStateId() + " [label=\"" /*+ pattern */ + "\"]");
  }

  /** Determines if this Transition matches on the current State of the CPA.
   * This might return a <code>MaybeBoolean.MAYBE</code> value if the method cannot determine if the transition matches.
   * In this case more information (e.g. more AbstractElements of other CPAs) are needed.
   */
  public MaybeBoolean match(AutomatonExpressionArguments pArgs) {
    for (AutomatonBoolExpr trigger : triggers) {
      MaybeBoolean triggerValue = trigger.eval(pArgs);
      
      // Why this condition ? Why not MaybeBoolean.MAYBE ? 
      // rhein: MAYBE and FALSE have to be handled identically (immediate abort of the trigger checks) 
      if (triggerValue != MaybeBoolean.TRUE) {
        return triggerValue;
      }
    }
    return MaybeBoolean.TRUE;
  }

  /**
   * Checks if all assertions of this transition are fulfilled
   * in the current configuration of the automaton this method is called.
   */
  public MaybeBoolean assertionsHold(AutomatonExpressionArguments pArgs) {
    for (AutomatonBoolExpr assertion : assertions) {
      MaybeBoolean assertionValue = assertion.eval(pArgs);
      if (assertionValue == MaybeBoolean.MAYBE || assertionValue == MaybeBoolean.FALSE) {
        return assertionValue; // LazyEvaluation
      }
    }
    return MaybeBoolean.TRUE;
  }

  /**
   * Executes all actions of this transition in the order which is defined in the automaton definition file.
   */
  public void executeActions(AutomatonExpressionArguments pArgs) {
    for (AutomatonActionExpr action : actions) {
      action.execute(pArgs);
    }
    if (pArgs.getLogMessage() != null && pArgs.getLogMessage().length() > 0) {
      pArgs.getLogger().log(Level.INFO, pArgs.getLogMessage());
      pArgs.clearLogMessage();
    }
  }
  
  /** Returns if the actions of this transiton can be executed on these AutomatonExpressionArguments.
   * If false is returned more Information is needed (probably more AbstractElements from other CPAs).
   * @param pArgs
   * @return
   */
  public boolean canExecuteActionsOn(AutomatonExpressionArguments pArgs) {
    for (AutomatonActionExpr action : actions) {
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
  
  @Override
  public String toString() {
    return this.triggers.toString();
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
    for (AutomatonActionExpr action : this.actions) {
      if ((action instanceof CPAModification)) {
        return false;
      }
    }
    return true;
  }
}
