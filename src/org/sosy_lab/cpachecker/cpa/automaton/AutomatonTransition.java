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
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonAction.CPAModification;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonExpression.ResultValue;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonExpression.StringExpression;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;

/**
 * A transition in the automaton implements one of the pattern matching methods.
 * This determines if the transition matches on a certain {@link CFAEdge}.
 */
class AutomatonTransition {

  // The order of triggers, assertions and (more importantly) actions is preserved by the parser.
  private final AutomatonBoolExpr trigger;
  private final AutomatonBoolExpr assertion;
  private final ImmutableList<AExpression> assumptions;
  private final ExpressionTree<AExpression> candidateInvariants;
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

  public AutomatonTransition(AutomatonBoolExpr pTrigger,
      List<AutomatonBoolExpr> pAssertions, List<AutomatonAction> pActions,
      AutomatonInternalState pFollowState) {

    this(
        pTrigger,
        pAssertions,
        ImmutableList.of(),
        ExpressionTrees.<AExpression>getTrue(),
        pActions,
        pFollowState.getName(),
        pFollowState,
        null);
  }

  public AutomatonTransition(
      AutomatonBoolExpr pTrigger,
      List<AutomatonBoolExpr> pAssertions,
      List<AExpression> pAssumptions,
      List<AutomatonAction> pActions,
      String pFollowStateName) {
    this(
        pTrigger,
        pAssertions,
        pAssumptions,
        ExpressionTrees.<AExpression>getTrue(),
        pActions,
        pFollowStateName,
        null,
        null);
  }

  public AutomatonTransition(
      AutomatonBoolExpr pTrigger,
      List<AutomatonBoolExpr> pAssertions,
      List<AExpression> pAssumptions,
      ExpressionTree<AExpression> pCandidateInvariants,
      List<AutomatonAction> pActions,
      String pFollowStateName) {
    this(
        pTrigger,
        pAssertions,
        pAssumptions,
        pCandidateInvariants,
        pActions,
        pFollowStateName,
        null,
        null);
  }

  public AutomatonTransition(
      AutomatonBoolExpr pTrigger,
      List<AutomatonBoolExpr> pAssertions,
      List<AExpression> pAssumptions,
      List<AutomatonAction> pActions,
      AutomatonInternalState pFollowState,
      StringExpression pViolatedPropertyDescription) {

    this(
        pTrigger,
        pAssertions,
        pAssumptions,
        ExpressionTrees.<AExpression>getTrue(),
        pActions,
        pFollowState.getName(),
        pFollowState,
        pViolatedPropertyDescription);
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
    return (String)violatedPropertyDescription.eval(pArgs).getValue();
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
            throw new SubstitutionException(
                "Cannot substitute function return variable: Not a call to " + functionName);
          }
        };
    SubstitutingVisitor substitutingVisitor = new SubstitutingVisitor(substitution, binExpBuilder);
    if (pAssumption instanceof CExpression) {
      try {
        CExpression assumption = ((CExpression) pAssumption).accept(substitutingVisitor);
        return Optional.of(assumption);
      } catch (SubstitutionException e) {
        return Optional.empty();
      }
    }
    return Optional.of(pAssumption);
  }

  public ExpressionTree<AExpression> getCandidateInvariants() {
    return candidateInvariants;
  }

  private static class SubstitutionException extends Exception {

    private static final long serialVersionUID = 1L;

    public SubstitutionException(String pMessage) {
      super(pMessage);
    }

    public SubstitutionException(UnrecognizedCCodeException pE) {
      super(pE);
    }
  }

  private static interface Substitution {

    CExpression substitute(CExpression e) throws SubstitutionException;
  }

  private static class SubstitutingVisitor
      implements CExpressionVisitor<CExpression, SubstitutionException> {

    private final Substitution substitution;

    private final CBinaryExpressionBuilder binExpBuilder;

    public SubstitutingVisitor(
        Substitution pSubstitution, CBinaryExpressionBuilder pBinExpBuilder) {
      this.substitution = pSubstitution;
      this.binExpBuilder = pBinExpBuilder;
    }

    @Override
    public CExpression visit(CArraySubscriptExpression pArraySubscriptExpression)
        throws SubstitutionException {
      CExpression arrayExpr = pArraySubscriptExpression.getArrayExpression().accept(this);
      CExpression subscExpr = pArraySubscriptExpression.getSubscriptExpression().accept(this);
      CExpression toSubstitute = pArraySubscriptExpression;
      if (arrayExpr != pArraySubscriptExpression.getArrayExpression()
          && subscExpr != pArraySubscriptExpression.getSubscriptExpression()) {
        toSubstitute =
            new CArraySubscriptExpression(
                pArraySubscriptExpression.getFileLocation(),
                pArraySubscriptExpression.getExpressionType(),
                arrayExpr,
                subscExpr);
      }
      return substitution.substitute(toSubstitute);
    }

    @Override
    public CExpression visit(CFieldReference pFieldReference) throws SubstitutionException {
      CExpression owner = pFieldReference.getFieldOwner().accept(this);
      CExpression toSubstitute = pFieldReference;
      if (owner != pFieldReference.getFieldOwner()) {
        toSubstitute =
            new CFieldReference(
                pFieldReference.getFileLocation(),
                pFieldReference.getExpressionType(),
                pFieldReference.getFieldName(),
                owner,
                pFieldReference.isPointerDereference());
      }
      return substitution.substitute(toSubstitute);
    }

    @Override
    public CExpression visit(CIdExpression pIdExpression) throws SubstitutionException {
      return substitution.substitute(pIdExpression);
    }

    @Override
    public CExpression visit(CPointerExpression pPointerExpression) throws SubstitutionException {
      CExpression operand = pPointerExpression.getOperand().accept(this);
      CExpression toSubstitute = pPointerExpression;
      if (operand != pPointerExpression.getOperand()) {
        toSubstitute =
            new CPointerExpression(
                pPointerExpression.getFileLocation(),
                pPointerExpression.getExpressionType(),
                operand);
      }
      return substitution.substitute(toSubstitute);
    }

    @Override
    public CExpression visit(CComplexCastExpression pComplexCastExpression)
        throws SubstitutionException {
      CExpression operand = pComplexCastExpression.getOperand().accept(this);
      CExpression toSubstitute = pComplexCastExpression;
      if (operand != pComplexCastExpression.getOperand()) {
        toSubstitute =
            new CPointerExpression(
                pComplexCastExpression.getFileLocation(),
                pComplexCastExpression.getExpressionType(),
                operand);
      }
      return substitution.substitute(toSubstitute);
    }

    @Override
    public CExpression visit(CBinaryExpression pBinaryExpression) throws SubstitutionException {
      CExpression op1 = pBinaryExpression.getOperand1().accept(this);
      CExpression op2 = pBinaryExpression.getOperand2().accept(this);
      CExpression toSubstitute = pBinaryExpression;
      if (op1 != pBinaryExpression.getOperand1() || op2 != pBinaryExpression.getOperand2()) {
        try {
          toSubstitute =
              binExpBuilder.buildBinaryExpression(op1, op2, pBinaryExpression.getOperator());
        } catch (UnrecognizedCCodeException e) {
          throw new SubstitutionException(e);
        }
      }
      return substitution.substitute(toSubstitute);
    }

    @Override
    public CExpression visit(CCastExpression pCastExpression) throws SubstitutionException {
      CExpression operand = pCastExpression.getOperand().accept(this);
      CExpression toSubstitute = pCastExpression;
      if (operand != pCastExpression.getOperand()) {
        toSubstitute =
            new CCastExpression(
                pCastExpression.getFileLocation(), pCastExpression.getCastType(), operand);
      }
      return substitution.substitute(toSubstitute);
    }

    @Override
    public CExpression visit(CCharLiteralExpression pCharLiteralExpression)
        throws SubstitutionException {
      return substitution.substitute(pCharLiteralExpression);
    }

    @Override
    public CExpression visit(CFloatLiteralExpression pFloatLiteralExpression)
        throws SubstitutionException {
      return substitution.substitute(pFloatLiteralExpression);
    }

    @Override
    public CExpression visit(CIntegerLiteralExpression pIntegerLiteralExpression)
        throws SubstitutionException {
      return substitution.substitute(pIntegerLiteralExpression);
    }

    @Override
    public CExpression visit(CStringLiteralExpression pStringLiteralExpression)
        throws SubstitutionException {
      return substitution.substitute(pStringLiteralExpression);
    }

    @Override
    public CExpression visit(CTypeIdExpression pTypeIdExpression) throws SubstitutionException {
      return substitution.substitute(pTypeIdExpression);
    }

    @Override
    public CExpression visit(CUnaryExpression pUnaryExpression) throws SubstitutionException {
      CExpression operand = pUnaryExpression.getOperand().accept(this);
      CExpression toSubstitute = pUnaryExpression;
      if (operand != pUnaryExpression.getOperand()) {
        toSubstitute =
            new CUnaryExpression(
                pUnaryExpression.getFileLocation(),
                pUnaryExpression.getExpressionType(),
                operand,
                pUnaryExpression.getOperator());
      }
      return substitution.substitute(toSubstitute);
    }

    @Override
    public CExpression visit(CImaginaryLiteralExpression pLiteralExpression)
        throws SubstitutionException {
      return substitution.substitute(pLiteralExpression);
    }

    @Override
    public CExpression visit(CAddressOfLabelExpression pAddressOfLabelExpression)
        throws SubstitutionException {
      return substitution.substitute(pAddressOfLabelExpression);
    }

  }
}
