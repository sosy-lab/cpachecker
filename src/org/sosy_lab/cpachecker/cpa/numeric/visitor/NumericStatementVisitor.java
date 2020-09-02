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
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatementVisitor;
import org.sosy_lab.cpachecker.cpa.numeric.NumericState;
import org.sosy_lab.cpachecker.cpa.numeric.NumericTransferRelation;
import org.sosy_lab.cpachecker.cpa.numeric.NumericTransferRelation.VariableSubstitution;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.numericdomains.Value.NewVariableValue;
import org.sosy_lab.numericdomains.constraint.ConstraintType;
import org.sosy_lab.numericdomains.constraint.TreeConstraint;
import org.sosy_lab.numericdomains.constraint.tree.BinaryOperator;
import org.sosy_lab.numericdomains.constraint.tree.BinaryTreeNode;
import org.sosy_lab.numericdomains.constraint.tree.TreeNode;
import org.sosy_lab.numericdomains.constraint.tree.VariableTreeNode;
import org.sosy_lab.numericdomains.environment.Variable;

public class NumericStatementVisitor
    implements CStatementVisitor<Collection<NumericState>, UnrecognizedCodeException> {
  private final LogManager logger;
  private final NumericState state;

  /**
   * Variable holding the return value of the function.
   *
   * <p>Only necessary if the statement is a function call statement.
   */
  private final Optional<Variable> returnVariable;

  public NumericStatementVisitor(NumericState pState, LogManager logManager) {
    logger = logManager;
    state = pState;
    returnVariable = Optional.empty();
  }

  /**
   * Returns a Numeric statement visitor for the {@link NumericState}.
   *
   * <p>The return variable should only be set for {@link CFunctionCallAssignmentStatement}s. For
   * other statements, the value is ignored.
   *
   * @param pState state for which the statement visitor is created
   * @param pReturnVariable return variable used for a CFunctionCallAssignmentStatement
   * @param logManager used for logging
   */
  public NumericStatementVisitor(
      NumericState pState, @Nullable Variable pReturnVariable, LogManager logManager) {
    logger = logManager;
    state = pState;
    returnVariable = Optional.ofNullable(pReturnVariable);
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

    // Create variable to which the value is assigned
    Variable variable =
        NumericTransferRelation.createVariableFromDeclaration(
            ((CIdExpression) pIastExpressionAssignmentStatement.getLeftHandSide())
                .getDeclaration());

    // Create substitution of variable if necessary
    VariableSubstitution substitution = VariableSubstitution.createSubstitutionOf(variable, null);

    Collection<PartialState> expressions =
        pIastExpressionAssignmentStatement
            .getRightHandSide()
            .accept(
                new NumericRightHandSideVisitor(
                    state.getValue().getEnvironment(), substitution, logger));
    TreeNode variableNode = new VariableTreeNode(variable);

    // Expand the environment if the assignment is dependent on the variable that will be assigned,
    // before forgetting its value
    NumericState newState;
    if (substitution.wasUsed()) {
      NumericState tempState =
          state.addTemporaryCopyOf(substitution.getFrom(), substitution.getSubstitute());
      newState =
          tempState.forget(ImmutableSet.of(substitution.getFrom()), NewVariableValue.UNCONSTRAINED);
      tempState.getValue().dispose();
    } else {
      newState = state.forget(ImmutableSet.of(variable), NewVariableValue.UNCONSTRAINED);
    }

    ImmutableList.Builder<NumericState> successors = new ImmutableList.Builder<>();

    // Compute successors by applying the constraints for each expression to the current value.
    // States will not be created if the value after the meet does not exist or if it is empty.
    for (PartialState expression : expressions) {
      TreeNode root =
          new BinaryTreeNode(
              BinaryOperator.SUBTRACT, variableNode, expression.getPartialConstraint());
      TreeConstraint constraint =
          new TreeConstraint(
              newState.getValue().getEnvironment(), ConstraintType.EQUALS, null, root);
      ImmutableSet.Builder<TreeConstraint> constraintsBuilder = new ImmutableSet.Builder<>();
      constraintsBuilder.addAll(expression.getConstraints());
      constraintsBuilder.add(constraint);

      Optional<NumericState> successor = newState.meet(constraintsBuilder.build());
      if (successor.isPresent()) {
        if (substitution.wasUsed()) {
          NumericState realSuccessor =
              successor.get().removeFromFrame(Set.of(substitution.getSubstitute()));
          successors.add(realSuccessor);
          successor.get().getValue().dispose();
        } else {
          successors.add(successor.get());
        }
      }
    }

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
      if (returnVariable.isPresent()) {
        return handleReturnVariable(variable);
      } else {
        // Function is extern, so the value can not be constrained
        NumericState newState =
            state.forget(ImmutableSet.of(variable), NewVariableValue.UNCONSTRAINED);
        return ImmutableSet.of(newState);
      }
    }

    // If it can not be handled do nothing
    return ImmutableSet.of(state);
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
