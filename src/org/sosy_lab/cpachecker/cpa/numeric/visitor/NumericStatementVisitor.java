// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.numeric.visitor;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Optional;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatementVisitor;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cpa.numeric.NumericState;
import org.sosy_lab.cpachecker.cpa.numeric.NumericTransferRelation;
import org.sosy_lab.cpachecker.cpa.numeric.NumericTransferRelation.HandleNumericTypes;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.numericdomains.Value.NewVariableValue;
import org.sosy_lab.numericdomains.constraint.ConstraintType;
import org.sosy_lab.numericdomains.constraint.TreeConstraint;
import org.sosy_lab.numericdomains.constraint.tree.BinaryOperator;
import org.sosy_lab.numericdomains.constraint.tree.BinaryTreeNode;
import org.sosy_lab.numericdomains.constraint.tree.ConstantTreeNode;
import org.sosy_lab.numericdomains.constraint.tree.TreeNode;
import org.sosy_lab.numericdomains.constraint.tree.VariableTreeNode;
import org.sosy_lab.numericdomains.environment.Variable;

public class NumericStatementVisitor
    implements CStatementVisitor<Collection<NumericState>, UnrecognizedCodeException> {

  private final NumericState state;

  /**
   * Variable holding the return value of the function.
   *
   * <p>Only necessary if the statement is a function call statement.
   */
  private final Optional<Variable> returnVariable;

  private final LogManager logger;
  private final HandleNumericTypes handledTypes;

  public NumericStatementVisitor(
      NumericState pState, LogManager logManager, HandleNumericTypes pHandledTypes) {
    state = pState;
    returnVariable = Optional.empty();
    logger = logManager;
    handledTypes = pHandledTypes;
  }

  /**
   * Returns a Numeric statement visitor for the {@link NumericState}.
   *
   * <p>The return variable should only be set for {@link CFunctionCallAssignmentStatement}s. For
   * other statements, the value is ignored.
   *
   * @param pState state for which the statement visitor is created
   * @param pHandledTypes specifies which types should be handled
   * @param pReturnVariable return variable used for a CFunctionCallAssignmentStatement
   */
  public NumericStatementVisitor(
      NumericState pState,
      HandleNumericTypes pHandledTypes,
      @Nullable Variable pReturnVariable,
      LogManager logManager) {
    state = pState;
    returnVariable = Optional.ofNullable(pReturnVariable);
    logger = logManager;
    handledTypes = pHandledTypes;
  }

  @Override
  public Collection<NumericState> visit(CExpressionStatement pIastExpressionStatement)
      throws UnrecognizedCodeException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Collection<NumericState> visit(
      CExpressionAssignmentStatement pIastExpressionAssignmentStatement)
      throws UnrecognizedCodeException {

    if (!(pIastExpressionAssignmentStatement.getLeftHandSide() instanceof CIdExpression)) {
      throw new UnrecognizedCodeException(
          "Left hand side is not a Variable.", pIastExpressionAssignmentStatement);
    }

    CSimpleDeclaration declaration =
        ((CIdExpression) pIastExpressionAssignmentStatement.getLeftHandSide()).getDeclaration();

    boolean isIntegerVariable;

    // Check if the type should be handled
    if (declaration.getType() instanceof CSimpleType) {
      CSimpleType type = (CSimpleType) declaration.getType();
      isIntegerVariable = type.getType().isIntegerType();
      if ((isIntegerVariable && handledTypes.handleIntegers())
          || (!isIntegerVariable && handledTypes.handleReals())) {
      } else {
        return ImmutableSet.of(state.createCopy());
      }
    } else {
      return ImmutableSet.of(state.createCopy());
    }

    // Create variable to which the value is assigned
    Variable variable = NumericTransferRelation.createVariableFromDeclaration(declaration);

    final NumericState extendedState;

    if (state.getValue().getEnvironment().containsVariable(variable)) {
      extendedState = state.createCopy();
    } else {
      Collection<Variable> newVariable = ImmutableSet.of(variable);
      if (isIntegerVariable) {
        extendedState = state.addVariables(newVariable, ImmutableSet.of(), NewVariableValue.ZERO);
      } else {
        extendedState =
            state.addVariables(ImmutableSet.of(), newVariable, NewVariableValue.UNCONSTRAINED);
      }
    }

    Collection<PartialState> expressions =
        pIastExpressionAssignmentStatement
            .getRightHandSide()
            .accept(
                new NumericRightHandSideVisitor(
                    extendedState.getValue().getEnvironment(), handledTypes, logger));

    ImmutableList.Builder<NumericState> successors = new ImmutableList.Builder<>();

    for (PartialState partialState : expressions) {
      NumericState newState =
          extendedState.assignTreeExpression(variable, partialState.getPartialConstraint());
      if (logger.wouldBeLogged(Level.FINEST)) {
        logger.log(
            Level.FINEST,
            pIastExpressionAssignmentStatement,
            "as",
            partialState,
            "assign to:",
            variable,
            "results in:",
            newState,
            " isBottom?",
            newState.getValue().isBottom());
      }
      if (!newState.getValue().isBottom()) {
        successors.add(newState);
      }
    }

    extendedState.getValue().dispose();

    return successors.build();
  }

  @Override
  public Collection<NumericState> visit(
      CFunctionCallAssignmentStatement pIastFunctionCallAssignmentStatement)
      throws UnrecognizedCodeException {
    if (pIastFunctionCallAssignmentStatement.getLeftHandSide() instanceof CIdExpression) {
      Variable variable =
          NumericTransferRelation.createVariableFromDeclaration(
              ((CIdExpression) pIastFunctionCallAssignmentStatement.getLeftHandSide())
                  .getDeclaration());
      if (state.getValue().getEnvironment().containsVariable(variable)) {
        if (returnVariable.isPresent()) {
          return handleReturnVariable(variable);
        } else {
          // Function is extern, so the value can not be constrained
          NumericState newState =
              state.assignTreeExpression(
                  variable, new ConstantTreeNode(PartialState.UNCONSTRAINED_INTERVAL));
          return ImmutableSet.of(newState);
        }
      }
    }

    // If it can not be handled do nothing
    return ImmutableSet.of(state.createCopy());
  }

  private Collection<NumericState> handleReturnVariable(Variable variable) {
    TreeNode left = new VariableTreeNode(variable);
    TreeNode right = new VariableTreeNode(returnVariable.get());
    TreeNode root = new BinaryTreeNode(BinaryOperator.SUBTRACT, left, right);
    TreeConstraint setEqual =
        new TreeConstraint(state.getValue().getEnvironment(), ConstraintType.EQUALS, null, root);

    // set variable to unconstrained before assignment
    NumericState newState = state.forget(ImmutableSet.of(variable), NewVariableValue.UNCONSTRAINED);

    Optional<NumericState> out = newState.meet(ImmutableSet.of(setEqual));
    newState.getValue().dispose();
    if (out.isPresent()) {
      return ImmutableSet.of(out.get());
    } else {
      return ImmutableSet.of();
    }
  }

  @Override
  public Collection<NumericState> visit(CFunctionCallStatement pIastFunctionCallStatement)
      throws UnrecognizedCodeException {
    // Nothing to do for a function call
    return ImmutableSet.of(state);
  }
}
