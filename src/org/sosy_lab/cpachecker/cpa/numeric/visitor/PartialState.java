// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.numeric.visitor;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.numericdomains.coefficients.MpqScalar;
import org.sosy_lab.numericdomains.constraint.ConstraintType;
import org.sosy_lab.numericdomains.constraint.TreeConstraint;
import org.sosy_lab.numericdomains.constraint.tree.BinaryOperator;
import org.sosy_lab.numericdomains.constraint.tree.BinaryTreeNode;
import org.sosy_lab.numericdomains.constraint.tree.ConstantTreeNode;
import org.sosy_lab.numericdomains.constraint.tree.TreeNode;
import org.sosy_lab.numericdomains.constraint.tree.UnaryOperator;
import org.sosy_lab.numericdomains.constraint.tree.UnaryTreeNode;
import org.sosy_lab.numericdomains.constraint.tree.VariableTreeNode;
import org.sosy_lab.numericdomains.environment.Environment;
import org.sosy_lab.numericdomains.environment.Variable;

/**
 * Represents an intermediate states when looking at a {@link
 * org.sosy_lab.cpachecker.cfa.ast.c.CExpression}.
 *
 * <p>The intermediate states are represented by a partial constraint and the constraints it depends
 * on.
 *
 * <p>The expression {@code (y==x)+1} can be converted to the partial states:
 *
 * <ul>
 *   <li>1+1 if x==y
 *   <li>0+1 if x!=y
 * </ul>
 */
public class PartialState {
  private final TreeNode partialConstraint;
  private final ImmutableCollection<TreeConstraint> constraints;

  PartialState(TreeNode pCurrentNode, Collection<TreeConstraint> pConstraints) {
    if (pCurrentNode == null || pConstraints == null) {
      throw new IllegalArgumentException("Parameters can not be null.");
    }

    partialConstraint = pCurrentNode;
    constraints = ImmutableSet.copyOf(pConstraints);
  }

  /**
   * Returns a collection of {@link TreeConstraint}s which are needed to assign the partial
   * constraint to the variable.
   *
   * @param environment environment of the constraint
   * @param variable variable to which the partial constraint should be assigned
   * @return constraint containing the assignment to the variable and all other constraints in the
   *     partial state
   */
  public Collection<TreeConstraint> assignToVariable(Environment environment, Variable variable) {
    if (!environment.containsVariable(variable)) {
      throw new IllegalArgumentException("Variable must be contained in environment.");
    }
    TreeNode variableNode = new VariableTreeNode(variable);
    TreeNode root = new BinaryTreeNode(BinaryOperator.SUBTRACT, variableNode, partialConstraint);
    TreeConstraint assignment = new TreeConstraint(environment, ConstraintType.EQUALS, null, root);
    ImmutableSet.Builder<TreeConstraint> assignmentConstraints = new ImmutableSet.Builder<>();
    assignmentConstraints.addAll(constraints);
    assignmentConstraints.add(assignment);
    return assignmentConstraints.build();
  }

  TreeNode getPartialConstraint() {
    return partialConstraint;
  }

  public Collection<TreeConstraint> getConstraints() {
    return constraints;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("BinExpAsConstraint: ");
    builder.append(partialConstraint.toString());
    if (constraints.size() > 0) {
      builder.append(" if ");
      for (TreeConstraint constraint : constraints) {
        builder.append(constraint.toString());
        builder.append("; ");
      }
    } else {
      builder.append(" no constraints.");
    }
    return builder.toString();
  }

  private static PartialState applyBinaryArithmeticOperator(
      BinaryOperator operator, PartialState leftExpression, PartialState rightExpression) {
    // Apply the arithmetic operator on the current node of both partial states
    TreeNode newCurrentNode =
        new BinaryTreeNode(
            operator,
            leftExpression.getPartialConstraint(),
            rightExpression.getPartialConstraint());

    // Add all constraints from the left and the right partial state
    ImmutableSet.Builder<TreeConstraint> builder = new ImmutableSet.Builder<>();
    builder.addAll(leftExpression.getConstraints());
    builder.addAll(rightExpression.getConstraints());
    return new PartialState(newCurrentNode, builder.build());
  }

  static ImmutableCollection<PartialState> applyBinaryArithmeticOperator(
      BinaryOperator operator,
      Collection<PartialState> leftExpressions,
      Collection<PartialState> rightExpressions) {
    ImmutableSet.Builder<PartialState> builder = new ImmutableSet.Builder<>();
    // Create one possible state for each combination of left expression and right expression
    for (PartialState leftExpression : leftExpressions) {
      for (PartialState rightExpression : rightExpressions) {
        builder.add(applyBinaryArithmeticOperator(operator, leftExpression, rightExpression));
      }
    }
    return builder.build();
  }

  static ImmutableCollection<PartialState> applyComparisonOperator(
      CBinaryExpression.BinaryOperator operator,
      Collection<PartialState> leftExpressions,
      Collection<PartialState> rightExpressions,
      TruthAssumption truthAssumption,
      Environment environment) {
    ImmutableSet.Builder<PartialState> builder = new ImmutableSet.Builder<>();
    // Create one possible state for each combination of left expression and right expression
    for (PartialState leftExpression : leftExpressions) {
      for (PartialState rightExpression : rightExpressions) {
        builder.addAll(
            applyComparisonOperator(
                operator, leftExpression, rightExpression, truthAssumption, environment));
      }
    }
    return builder.build();
  }

  /**
   * Applies the comparison operator to the partial states.
   *
   * <p>The operator is applied to {@link PartialState#partialConstraint}s of the left and the right
   * partial states.
   *
   * @param operator binary comparison operator, that is applied to the partial constraints
   * @param leftExpression partial state containing the first part of the constraint
   * @param rightExpression partial state containing the second part of the constraint
   * @param assumption describes whether the assumption is expected to be true, false or either
   * @param environment environment of the constraints in the partial state
   * @return collection of the partial states after applying the comparison operator
   */
  private static ImmutableCollection<PartialState> applyComparisonOperator(
      CBinaryExpression.BinaryOperator operator,
      PartialState leftExpression,
      PartialState rightExpression,
      TruthAssumption assumption,
      Environment environment) {
    TreeNode leftMinusRight =
        createNewConstraintRoot(
            leftExpression.getPartialConstraint(), rightExpression.getPartialConstraint());
    TreeNode rightMinusLeft =
        createNewConstraintRoot(
            rightExpression.getPartialConstraint(), leftExpression.getPartialConstraint());

    // Create all information needed to create the new constraint
    final TreeNode trueRootNode;
    final TreeNode falseRootNode;
    final ConstraintType trueConstraintType;
    final ConstraintType falseConstraintType;

    switch (operator) {
      case EQUALS:
        trueRootNode = leftMinusRight;
        trueConstraintType = ConstraintType.EQUALS;
        falseRootNode = trueRootNode;
        falseConstraintType = ConstraintType.NOT_EQUALS;
        break;
      case NOT_EQUALS:
        trueRootNode = leftMinusRight;
        trueConstraintType = ConstraintType.NOT_EQUALS;
        falseRootNode = trueRootNode;
        falseConstraintType = ConstraintType.EQUALS;
        break;
      case GREATER_THAN:
        trueRootNode = leftMinusRight;
        trueConstraintType = ConstraintType.BIGGER;
        falseRootNode = rightMinusLeft;
        falseConstraintType = ConstraintType.BIGGER_EQUALS;
        break;
      case GREATER_EQUAL:
        trueRootNode = leftMinusRight;
        trueConstraintType = ConstraintType.BIGGER_EQUALS;
        falseRootNode = rightMinusLeft;
        falseConstraintType = ConstraintType.BIGGER;
        break;
      case LESS_THAN:
        trueRootNode = rightMinusLeft;
        trueConstraintType = ConstraintType.BIGGER;
        falseRootNode = leftMinusRight;
        falseConstraintType = ConstraintType.BIGGER_EQUALS;
        break;
      case LESS_EQUAL:
        trueRootNode = rightMinusLeft;
        trueConstraintType = ConstraintType.BIGGER_EQUALS;
        falseRootNode = leftMinusRight;
        falseConstraintType = ConstraintType.BIGGER;
        break;
      default:
        throw new AssertionError(operator + " is not a comparison.");
    }

    ImmutableSet.Builder<PartialState> statesBuilder = new ImmutableSet.Builder<>();
    if (assumption == TruthAssumption.ASSUME_TRUE || assumption == TruthAssumption.ASSUME_EITHER) {
      TreeConstraint trueConstraint =
          new TreeConstraint(environment, trueConstraintType, null, trueRootNode);
      for (TreeConstraint constraint : splitInequalityConstraint(trueConstraint)) {
        Collection<TreeConstraint> constraints =
            buildConstraintCollection(
                constraint, leftExpression.getConstraints(), rightExpression.getConstraints());
        statesBuilder.add(new PartialState(TRUE_COMPARISON_RESULT, constraints));
      }
    }
    if (assumption == TruthAssumption.ASSUME_FALSE || assumption == TruthAssumption.ASSUME_EITHER) {
      TreeConstraint falseConstraint =
          new TreeConstraint(environment, falseConstraintType, null, falseRootNode);
      for (TreeConstraint constraint : splitInequalityConstraint(falseConstraint)) {
        Collection<TreeConstraint> constraints =
            buildConstraintCollection(
                constraint, leftExpression.getConstraints(), rightExpression.getConstraints());
        statesBuilder.add(new PartialState(FALSE_COMPARISON_RESULT, constraints));
      }
    }
    return statesBuilder.build();
  }

  /**
   * Splits inequality constraints into a bigger and a smaller constraint.
   *
   * <p>If the supplied constraint is not an inequality, the constraint is returned unchanged.
   *
   * @param constraint inequality constraint that should be split
   * @return constraints after splitting the inequality, unchanged constraint if the constraint is
   *     not an inequality
   */
  private static Collection<TreeConstraint> splitInequalityConstraint(TreeConstraint constraint) {
    if (constraint.getConstraintType() == ConstraintType.NOT_EQUALS) {
      // Split inequalities: a-b != 0 to a-b > 0 and a-b < 0
      TreeConstraint bigger = constraint.changeConstraintType(ConstraintType.BIGGER, null);
      TreeConstraint smaller =
          constraint
              .addUnaryOperation(UnaryOperator.NEGATE)
              .changeConstraintType(ConstraintType.BIGGER, null);
      return ImmutableSet.of(bigger, smaller);
    } else {
      return ImmutableSet.of(constraint);
    }
  }

  private static final TreeNode TRUE_COMPARISON_RESULT = new ConstantTreeNode(MpqScalar.of(1));
  private static final TreeNode FALSE_COMPARISON_RESULT = new ConstantTreeNode(MpqScalar.of(0));

  private static Collection<TreeConstraint> buildConstraintCollection(
      TreeConstraint constraint,
      Collection<TreeConstraint> constraintsLeft,
      Collection<TreeConstraint> constraintsRight) {
    ImmutableSet.Builder<TreeConstraint> builder = new ImmutableSet.Builder<>();
    builder.add(constraint);
    builder.addAll(constraintsLeft);
    builder.addAll(constraintsRight);
    return builder.build();
  }

  private static TreeNode createNewConstraintRoot(TreeNode leftNode, TreeNode rightNode) {
    return new BinaryTreeNode(BinaryOperator.SUBTRACT, leftNode, rightNode);
  }

  /**
   * Applies the unary arithmetic operator on the partial state.
   *
   * @param operator unary arithmetic operator that is applied to the state
   * @param pState state on which the arithmetic operator is applied
   * @return new partial state
   */
  static Collection<PartialState> applyUnaryArithmeticOperator(
      UnaryOperator operator, Collection<PartialState> pState) {
    ImmutableSet.Builder<PartialState> builder = new ImmutableSet.Builder<>();
    for (PartialState expression : pState) {
      builder.add(applyUnaryArithmeticOperator(operator, expression));
    }
    return builder.build();
  }

  private static PartialState applyUnaryArithmeticOperator(
      UnaryOperator operator, PartialState pState) {
    TreeNode root = pState.getPartialConstraint();
    root = new UnaryTreeNode(operator, root);
    return new PartialState(root, pState.getConstraints());
  }

  /** Describes the assumption that should be made when computing the constraints. */
  enum TruthAssumption {
    /** Assumption is false. */
    ASSUME_TRUE,
    /** Assumption is false. */
    ASSUME_FALSE,
    /** Assumption can be either true or false. */
    ASSUME_EITHER;
  }
}
