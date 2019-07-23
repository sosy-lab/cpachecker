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

import com.google.common.base.Joiner;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonAction.CPAModification;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonExpression.ResultValue;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonExpression.StringExpression;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.ExpressionSubstitution;
import org.sosy_lab.cpachecker.util.ExpressionSubstitution.Substitution;
import org.sosy_lab.cpachecker.util.ExpressionSubstitution.SubstitutionException;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;

/**
 * A transition in the automaton implements one of the pattern matching methods.
 * This determines if the transition matches on a certain {@link CFAEdge}.
 */
class AutomatonTransition {

  // The order of triggers, assertions and (more importantly) actions is preserved by the parser.

  /**
   * The trigger can be any AutomatonBoolExpr. If a trigger is matching, the transfer-relation is
   * applied. If no trigger is satisfied, the AutomatonState does not change with the
   * transfer-relation and remains identical. If a developer wants a transfer relation to be either
   * fully applied or stop (cut off a branch), all possible conditions (i.e., positive and negative)
   * must be part of the automaton, such that at least one of them has a matching trigger.
   */
  private final AutomatonBoolExpr trigger;

  /**
   * The assertion can be any AutomatonBoolExpr. It is checked when executing the transfer-relation.
   * If an assertion fails, the transfer-relation returns an error state that may terminate the
   * analysis and may be reported towards the user.
   */
  private final AutomatonBoolExpr assertion;

  /**
   * Assumptions contain additional code fragments that can be evaluated in the analysis.
   * Assumptions do not directly influence the transfer-relation of the AutomatonCPA, but can
   * provide information for other CPAs (that can cut off the ARG by themselves by returning a
   * bottom state).
   */
  private final ImmutableList<AExpression> assumptions;

  private final ExpressionTree<AExpression> candidateInvariants;

  /** The actions are applied after the assertion are checked successfully. */
  private final ImmutableList<AutomatonAction> actions;

  private final StringExpression violatedPropertyDescription;

  /**
   * When the parser instances this class it can not assign a followstate because
   * that state might not be created (forward-reference).
   * Only the name is known in the beginning and the followstate relation must be
   * resolved by calling setFollowState() when all States are known.
   */
  private final String followStateName;
  private AutomatonInternalState followState = null;

  static class Builder {
    private AutomatonBoolExpr trigger;
    private List<AutomatonBoolExpr> assertions;
    private List<AExpression> assumptions;
    private List<AutomatonAction> actions;
    private String followStateName;
    private @Nullable AutomatonInternalState followState;
    private ExpressionTree<AExpression> candidateInvariants;
    private @Nullable StringExpression violatedPropertyDescription;

    Builder(AutomatonBoolExpr pTrigger, String pFollowStateName) {
      trigger = pTrigger;
      assertions = ImmutableList.of();
      assumptions = ImmutableList.of();
      actions = ImmutableList.of();
      followStateName = pFollowStateName;
      candidateInvariants = ExpressionTrees.<AExpression>getTrue();
    }

    Builder(AutomatonBoolExpr pTrigger, @Nullable AutomatonInternalState pFollowState) {
      this(pTrigger, pFollowState != null ? pFollowState.getName() : "");
      followState = pFollowState;
    }

    Builder withAssertion(AutomatonBoolExpr pAssertion) {
      this.assertions = ImmutableList.of(pAssertion);
      return this;
    }

    Builder withAssertions(List<AutomatonBoolExpr> pAssertions) {
      this.assertions = pAssertions;
      return this;
    }

    Builder withAssumptions(List<AExpression> pAssumptions) {
      this.assumptions = pAssumptions;
      return this;
    }

    Builder withActions(List<AutomatonAction> pActions) {
      this.actions = pActions;
      return this;
    }

    Builder withCandidateInvariants(ExpressionTree<AExpression> pCandidateInvariants) {
      this.candidateInvariants = pCandidateInvariants;
      return this;
    }

    Builder withViolatedPropertyDescription(StringExpression pViolatedPropertyDescription) {
      this.violatedPropertyDescription = pViolatedPropertyDescription;
      return this;
    }

    AutomatonTransition build() {
      return new AutomatonTransition(
          trigger,
          assertions,
          assumptions,
          candidateInvariants,
          actions,
          followStateName,
          followState,
          violatedPropertyDescription);
    }
  }

  AutomatonTransition(Builder b) {
    this(
        b.trigger,
        b.assertions,
        b.assumptions,
        b.candidateInvariants,
        b.actions,
        b.followStateName,
        b.followState,
        b.violatedPropertyDescription);
  }

  private AutomatonTransition(
      AutomatonBoolExpr pTrigger,
      List<AutomatonBoolExpr> pAssertions,
      List<AExpression> pAssumptions,
      ExpressionTree<AExpression> pCandidateInvariants,
      List<AutomatonAction> pActions,
      String pFollowStateName,
      AutomatonInternalState pFollowState,
      StringExpression pViolatedPropertyDescription) {

    this.trigger = checkNotNull(pTrigger);

    if (pAssumptions == null) {
      this.assumptions = ImmutableList.of();
    } else {
      this.assumptions = ImmutableList.copyOf(pAssumptions);
    }

    this.candidateInvariants = checkNotNull(pCandidateInvariants);

    this.actions = ImmutableList.copyOf(pActions);
    this.followStateName = checkNotNull(pFollowStateName);
    this.followState = pFollowState;
    this.violatedPropertyDescription = pViolatedPropertyDescription;

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

  @Override
  public int hashCode() {
    return Objects.hash(
        actions, assertion, assumptions, followStateName, trigger, violatedPropertyDescription);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof AutomatonTransition)) {
      return false;
    }

    AutomatonTransition other = (AutomatonTransition) obj;

    return Objects.equals(actions, other.actions)
        && Objects.equals(assertion, other.assertion)
        && Objects.equals(assumptions, other.assumptions)
        && Objects.equals(followStateName, other.followStateName)
        && Objects.equals(trigger, other.trigger)
        && Objects.equals(violatedPropertyDescription, other.violatedPropertyDescription);
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
   */
  public ResultValue<Boolean> match(AutomatonExpressionArguments pArgs) throws CPATransferException {
    return trigger.eval(pArgs);
  }

  /**
   * Checks if all assertions of this transition are fulfilled
   * in the current configuration of the automaton this method is called.
   */
  public ResultValue<Boolean> assertionsHold(AutomatonExpressionArguments pArgs) throws CPATransferException {
    return assertion.eval(pArgs);
  }

  /**
   * Executes all actions of this transition in the order which is defined in the automaton definition file.
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

  public String getViolatedPropertyDescription(AutomatonExpressionArguments pArgs) {
    if (violatedPropertyDescription == null) {
      if (getFollowState().isTarget()) {
          return getFollowState().getName();
      }
      return null;
    }
    return violatedPropertyDescription.eval(pArgs).getValue();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(trigger);
    sb.append(" -> ");
    if (!assertion.equals(AutomatonBoolExpr.TRUE)) {
      sb.append("ASSERT ");
      sb.append(assertion);
      sb.append(" ");
    }
    if (!assumptions.isEmpty()) {
      sb.append("ASSUME {");
      sb.append(
          Joiner.on("; ").join(Collections2.transform(assumptions, AExpression::toASTString)));
      sb.append("} ");
    }
    if (!actions.isEmpty()) {
      Joiner.on(" ").appendTo(sb, actions);
      sb.append(" ");
    }
    return sb.toString();
  }

  /**
   * Returns true if this Transition fulfills the requirements of an ObserverTransition (does not use MODIFY or STOP).
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

  public ImmutableList<AExpression> getAssumptions(
      CFAEdge pEdge, LogManager pLogger, MachineModel pMachineModel) {
    ImmutableList.Builder<AExpression> builder = ImmutableList.builder();
    for (AExpression assumption : assumptions) {
      Optional<AExpression> resolved = tryResolve(assumption, pEdge, pLogger, pMachineModel);
      if (resolved.isPresent()) {
        builder.add(resolved.get());
      }
    }
    return builder.build();
  }

  private Optional<AExpression> tryResolve(
      AExpression pAssumption, CFAEdge pEdge, LogManager pLogger, MachineModel pMachineModel) {
    CBinaryExpressionBuilder binExpBuilder = new CBinaryExpressionBuilder(pMachineModel, pLogger);
    Substitution substitution =
        new Substitution() {

          @Override
          public CExpression substitute(CExpression pExpression) throws SubstitutionException {
            if (!(pExpression instanceof CIdExpression)) {
              return pExpression;
            }
            CIdExpression idExpression = (CIdExpression) pExpression;
            if (!CProgramScope.isArtificialFunctionReturnVariable(idExpression)) {
              return pExpression;
            }
            String functionName = CProgramScope.getFunctionNameOfArtificialReturnVar(idExpression);
            if (pEdge instanceof AStatementEdge) {
              AStatement statement = ((AStatementEdge) pEdge).getStatement();
              if (statement instanceof AFunctionCallAssignmentStatement) {
                AFunctionCallAssignmentStatement functionCallAssignment =
                    (AFunctionCallAssignmentStatement) statement;
                AExpression functionNameExpression =
                    functionCallAssignment.getFunctionCallExpression().getFunctionNameExpression();
                if (functionNameExpression instanceof AIdExpression
                    && ((AIdExpression) functionNameExpression).getName().equals(functionName)) {
                  return (CExpression) functionCallAssignment.getLeftHandSide();
                }
              }
            }
            throw new ExpressionSubstitution.SubstitutionException(
                "Cannot substitute function return variable: Not a call to " + functionName);
          }
        };
    if (pAssumption instanceof CExpression) {
      try {
        CExpression assumption = (CExpression) pAssumption;
        return Optional.of(ExpressionSubstitution.applySubstitution(assumption, substitution, binExpBuilder));
      } catch (SubstitutionException e) {
        return Optional.empty();
      }
    }
    return Optional.of(pAssumption);
  }

  public ExpressionTree<AExpression> getCandidateInvariants() {
    return candidateInvariants;
  }

  public boolean isTransitionWithAssumptions() {
    return assumptions.size() > 0;
  }

  public boolean nontriviallyMatches(final CFAEdge pEdge, final LogManager pLogger) {
    if (trigger != AutomatonBoolExpr.TRUE) {
      try {
        ResultValue<Boolean> match =
            trigger.eval(new AutomatonExpressionArguments(null, null, null, pEdge, pLogger));
        // be conservative and also return true if trigger cannot be evaluated
        return match.canNotEvaluate() || match.getValue();
      } catch (CPATransferException e) {
        // be conservative and assume that it would match
        return true;
      }
    }
    return false;
  }
}
