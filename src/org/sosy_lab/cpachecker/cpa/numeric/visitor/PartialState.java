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
import org.sosy_lab.common.rationals.ExtendedRational;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cpa.numeric.NumericVariable;
import org.sosy_lab.numericdomains.coefficients.Coefficient;
import org.sosy_lab.numericdomains.coefficients.DoubleScalar;
import org.sosy_lab.numericdomains.coefficients.Interval;
import org.sosy_lab.numericdomains.coefficients.MpqScalar;
import org.sosy_lab.numericdomains.coefficients.Scalar;
import org.sosy_lab.numericdomains.constraint.ConstraintType;
import org.sosy_lab.numericdomains.constraint.TreeConstraint;
import org.sosy_lab.numericdomains.constraint.tree.BinaryOperator;
import org.sosy_lab.numericdomains.constraint.tree.BinaryTreeNode;
import org.sosy_lab.numericdomains.constraint.tree.ConstantTreeNode;
import org.sosy_lab.numericdomains.constraint.tree.RoundingMode;
import org.sosy_lab.numericdomains.constraint.tree.RoundingType;
import org.sosy_lab.numericdomains.constraint.tree.TreeNode;
import org.sosy_lab.numericdomains.constraint.tree.UnaryOperator;
import org.sosy_lab.numericdomains.constraint.tree.UnaryTreeNode;
import org.sosy_lab.numericdomains.constraint.tree.VariableTreeNode;
import org.sosy_lab.numericdomains.environment.Environment;

/**
 * Represents intermediate states when looking at a {@link
 * org.sosy_lab.cpachecker.cfa.ast.c.CExpression}.
 *
 * <p>The intermediate states are represented by a partial constraint and the constraints it depends
 * on.
 *
 * <p>The expression {@code (y==x)+2} can be converted to the partial states:
 *
 * <ul>
 *   <li>1+2 if x==y
 *   <li>0+2 if x!=y
 * </ul>
 */
public class PartialState {
  static final RoundingType DEFAULT_ROUNDING_TYPE = RoundingType.NONE;
  static final RoundingMode DEFAULT_ROUNDING_MODE = RoundingMode.RANDOM;

  static final Interval UNCONSTRAINED_INTERVAL =
      new Interval(MpqScalar.of(ExtendedRational.NEG_INFTY), MpqScalar.of(ExtendedRational.INFTY));
  static final Interval UNSIGNED_UNCONSTRAINED_INTERVAL =
      new Interval(MpqScalar.of(ExtendedRational.ZERO), MpqScalar.of(ExtendedRational.INFTY));

  private static final TreeNode TRUE_COMPARISON_RESULT = new ConstantTreeNode(MpqScalar.of(1));
  private static final TreeNode FALSE_COMPARISON_RESULT = new ConstantTreeNode(MpqScalar.of(0));

  private static final double EPSILON_VALUE = 0.00000000000001;

  /** Epsilon used for comparison of floating point values. */
  private static final Scalar EPSILON = DoubleScalar.of(EPSILON_VALUE);
  /** Double the epsilon value used for */
  private static final Scalar DOUBLE_EPSILON = DoubleScalar.of(2 * EPSILON_VALUE);

  /**
   * Epsilon Interval [-{@link PartialState#EPSILON}, {@link PartialState#EPSILON}]used for equality
   * comparison of floating point values.
   */
  private static final Interval EPSILON_INTERVAL = new Interval(EPSILON.negate().get(), EPSILON);

  private final TreeNode partialConstraint;
  private final ImmutableCollection<TreeConstraint> constraints;
  private final boolean containsFloatVariable;

  public PartialState(NumericVariable variable) {
    partialConstraint = new VariableTreeNode(variable);
    constraints = ImmutableSet.of();
    containsFloatVariable = variable.getSimpleType().getType().isFloatingPointType();
  }

  public PartialState(Coefficient value) {
    partialConstraint = new ConstantTreeNode(value);
    constraints = ImmutableSet.of();
    containsFloatVariable = false;
  }

  private PartialState(
      TreeNode pCurrentNode,
      Collection<TreeConstraint> pConstraints,
      boolean pContainsFloatVariable) {
    if (pCurrentNode == null || pConstraints == null) {
      throw new IllegalArgumentException("Parameters can not be null.");
    }

    partialConstraint = pCurrentNode;
    constraints = ImmutableSet.copyOf(pConstraints);
    containsFloatVariable = pContainsFloatVariable;
  }

  public TreeNode getPartialConstraint() {
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
      BinaryOperator operator,
      PartialState leftExpression,
      PartialState rightExpression,
      RoundingType pRoundingType) {
    // Apply the arithmetic operator on the current node of both partial states
    TreeNode newCurrentNode =
        new BinaryTreeNode(
            operator,
            leftExpression.getPartialConstraint(),
            rightExpression.getPartialConstraint(),
            pRoundingType,
            RoundingMode.RANDOM);

    // Add all constraints from the left and the right partial state
    ImmutableSet.Builder<TreeConstraint> builder = new ImmutableSet.Builder<>();
    builder.addAll(leftExpression.getConstraints());
    builder.addAll(rightExpression.getConstraints());
    return new PartialState(
        newCurrentNode,
        builder.build(),
        leftExpression.containsFloatVariable || rightExpression.containsFloatVariable);
  }

  static ImmutableCollection<PartialState> applyBinaryArithmeticOperator(
      BinaryOperator operator,
      Collection<PartialState> leftExpressions,
      Collection<PartialState> rightExpressions,
      RoundingType pRoundingType) {
    ImmutableSet.Builder<PartialState> builder = new ImmutableSet.Builder<>();
    // Create one possible state for each combination of left expression and right expression
    for (PartialState leftExpression : leftExpressions) {
      for (PartialState rightExpression : rightExpressions) {
        builder.add(
            applyBinaryArithmeticOperator(
                operator, leftExpression, rightExpression, pRoundingType));
      }
    }
    return builder.build();
  }

  /**
   * Applies the comparison operator for the two expressions.
   *
   * <p>The result contains the new constraint as part of the constraints. The new expression is
   * either 1 or 0 depending on whether the assumption was true or false.
   *
   * @param operator comparison operator
   * @param leftExpressions expression left of the comparison operator
   * @param rightExpressions expression right of the comparison operator
   * @param truthAssumption whether the comparison should be assumed true or false
   * @param environment environment of the state to which the comparison will be applied
   * @return partial state containing the comparison in the constraints and the result of the
   *     comparison as partial constraint
   */
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

  private static class Comparison {
    private final TreeNode rootNode;
    private final ConstraintType type;

    private Comparison(TreeNode pRootNode, ConstraintType pType) {
      rootNode = pRootNode;
      type = pType;
    }

    TreeNode getRootNode() {
      return rootNode;
    }

    ConstraintType getConstraintType() {
      return type;
    }
  }

  private static Collection<Comparison> buildComparison(
      CBinaryExpression.BinaryOperator pOperator,
      TreeNode leftExpression,
      TreeNode rightExpression,
      TruthAssumption pAssumption,
      ApplyEpsilon useEpsilon) {
    if (pAssumption == TruthAssumption.ASSUME_EITHER) {
      throw new IllegalArgumentException("Assumption can not be: " + pAssumption);
    }

    final EpsilonType epsilonType;

    switch (pOperator) {
      case EQUALS:
        if (pAssumption == TruthAssumption.ASSUME_TRUE) {
          epsilonType =
              (useEpsilon == ApplyEpsilon.APPLY_EPSILON
                  ? EpsilonType.INTERVAL_EPSILON
                  : EpsilonType.EXACT);
          return ImmutableSet.of(
              buildSimpleComparison(
                  ConstraintType.EQUALS, leftExpression, rightExpression, epsilonType));
        } else {
          epsilonType =
              (useEpsilon == ApplyEpsilon.APPLY_EPSILON
                  ? EpsilonType.SUBTRACT_EPSILON_TWICE
                  : EpsilonType.EXACT);
          return buildAndSplitInequalityComparison(leftExpression, rightExpression, epsilonType);
        }
      case NOT_EQUALS:
        if (pAssumption == TruthAssumption.ASSUME_TRUE) {
          epsilonType =
              (useEpsilon == ApplyEpsilon.APPLY_EPSILON
                  ? EpsilonType.SUBTRACT_EPSILON_TWICE
                  : EpsilonType.EXACT);
          return buildAndSplitInequalityComparison(leftExpression, rightExpression, epsilonType);
        } else {
          epsilonType =
              (useEpsilon == ApplyEpsilon.APPLY_EPSILON
                  ? EpsilonType.INTERVAL_EPSILON
                  : EpsilonType.EXACT);
          return ImmutableSet.of(
              buildSimpleComparison(
                  ConstraintType.EQUALS, leftExpression, rightExpression, epsilonType));
        }
      case GREATER_THAN:
        if (pAssumption == TruthAssumption.ASSUME_TRUE) {
          epsilonType =
              (useEpsilon == ApplyEpsilon.APPLY_EPSILON
                  ? EpsilonType.SUBTRACT_EPSILON_TWICE
                  : EpsilonType.EXACT);
          return ImmutableSet.of(
              buildSimpleComparison(
                  ConstraintType.BIGGER, leftExpression, rightExpression, epsilonType));
        } else {
          epsilonType =
              (useEpsilon == ApplyEpsilon.APPLY_EPSILON
                  ? EpsilonType.ADD_EPSILON
                  : EpsilonType.EXACT);
          return ImmutableSet.of(
              buildSimpleComparison(
                  ConstraintType.BIGGER_EQUALS, rightExpression, leftExpression, epsilonType));
        }
      case GREATER_EQUAL:
        if (pAssumption == TruthAssumption.ASSUME_TRUE) {
          epsilonType =
              (useEpsilon == ApplyEpsilon.APPLY_EPSILON
                  ? EpsilonType.ADD_EPSILON
                  : EpsilonType.EXACT);

          return ImmutableSet.of(
              buildSimpleComparison(
                  ConstraintType.BIGGER_EQUALS, leftExpression, rightExpression, epsilonType));
        } else {
          epsilonType =
              (useEpsilon == ApplyEpsilon.APPLY_EPSILON
                  ? EpsilonType.SUBTRACT_EPSILON_TWICE
                  : EpsilonType.EXACT);
          return ImmutableSet.of(
              buildSimpleComparison(
                  ConstraintType.BIGGER, rightExpression, leftExpression, epsilonType));
        }
      case LESS_THAN:
        if (pAssumption == TruthAssumption.ASSUME_TRUE) {
          epsilonType =
              (useEpsilon == ApplyEpsilon.APPLY_EPSILON
                  ? EpsilonType.SUBTRACT_EPSILON_TWICE
                  : EpsilonType.EXACT);
          return ImmutableSet.of(
              buildSimpleComparison(
                  ConstraintType.BIGGER, rightExpression, leftExpression, epsilonType));
        } else {
          epsilonType =
              (useEpsilon == ApplyEpsilon.APPLY_EPSILON
                  ? EpsilonType.ADD_EPSILON
                  : EpsilonType.EXACT);
          return ImmutableSet.of(
              buildSimpleComparison(
                  ConstraintType.BIGGER_EQUALS, leftExpression, rightExpression, epsilonType));
        }
      case LESS_EQUAL:
        if (pAssumption == TruthAssumption.ASSUME_TRUE) {
          epsilonType =
              (useEpsilon == ApplyEpsilon.APPLY_EPSILON
                  ? EpsilonType.ADD_EPSILON
                  : EpsilonType.EXACT);
          return ImmutableSet.of(
              buildSimpleComparison(
                  ConstraintType.BIGGER_EQUALS, rightExpression, leftExpression, epsilonType));
        } else {
          epsilonType =
              (useEpsilon == ApplyEpsilon.APPLY_EPSILON
                  ? EpsilonType.SUBTRACT_EPSILON_TWICE
                  : EpsilonType.EXACT);
          return ImmutableSet.of(
              buildSimpleComparison(
                  ConstraintType.BIGGER, leftExpression, rightExpression, epsilonType));
        }
      default:
        throw new AssertionError(pOperator + " is not a valid comparison.");
    }
  }

  private static Comparison buildSimpleComparison(
      ConstraintType comparisonType,
      TreeNode leftExpression,
      TreeNode rightExpression,
      EpsilonType useEpsilon) {
    TreeNode newRoot = subtractWithEpsilon(leftExpression, rightExpression, useEpsilon);
    return new Comparison(newRoot, comparisonType);
  }

  /**
   * Creates the comparisons for an inequality.
   *
   * <p>Splits inequalities: a + [-epsilon,+epsilon] != b to a-b+epsilon > 0 and -a+b+epsilon > 0
   *
   * @param leftExpression left expression of the constraint
   * @param rightExpression right expression of the constraint
   * @param useEpsilon whether the epsilon should be applied
   * @return inequality comparison split into bigger comparison and smaller comparison
   */
  private static Collection<Comparison> buildAndSplitInequalityComparison(
      TreeNode leftExpression, TreeNode rightExpression, EpsilonType useEpsilon) {
    final TreeNode biggerRoot = subtractWithEpsilon(leftExpression, rightExpression, useEpsilon);
    final TreeNode smallerRoot = subtractWithEpsilon(rightExpression, leftExpression, useEpsilon);

    ConstraintType type;
    if (useEpsilon == EpsilonType.EXACT) {
      type = ConstraintType.BIGGER;
    } else {
      type = ConstraintType.BIGGER_EQUALS;
    }
    Comparison bigger = new Comparison(biggerRoot, type);
    Comparison smaller = new Comparison(smallerRoot, type);
    return ImmutableSet.of(bigger, smaller);
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
    // Use epsilon for comparison if either of the two expressions contains a float variable.
    final ApplyEpsilon useEpsilon;
    if (leftExpression.containsFloatVariable || rightExpression.containsFloatVariable) {
      useEpsilon = ApplyEpsilon.APPLY_EPSILON;
    } else {
      useEpsilon = ApplyEpsilon.EXACT;
    }

    ImmutableSet.Builder<PartialState> statesBuilder = new ImmutableSet.Builder<>();
    if (assumption == TruthAssumption.ASSUME_TRUE || assumption == TruthAssumption.ASSUME_EITHER) {
      for (Comparison comparison :
          buildComparison(
              operator,
              leftExpression.getPartialConstraint(),
              rightExpression.getPartialConstraint(),
              TruthAssumption.ASSUME_TRUE,
              useEpsilon)) {
        TreeConstraint constraint =
            new TreeConstraint(
                environment, comparison.getConstraintType(), null, comparison.getRootNode());
        Collection<TreeConstraint> constraints =
            buildConstraintCollection(
                constraint, leftExpression.getConstraints(), rightExpression.getConstraints());
        statesBuilder.add(new PartialState(TRUE_COMPARISON_RESULT, constraints, false));
      }
    }

    if (assumption == TruthAssumption.ASSUME_FALSE || assumption == TruthAssumption.ASSUME_EITHER) {
      for (Comparison comparison :
          buildComparison(
              operator,
              leftExpression.getPartialConstraint(),
              rightExpression.getPartialConstraint(),
              TruthAssumption.ASSUME_FALSE,
              useEpsilon)) {
        TreeConstraint constraint =
            new TreeConstraint(
                environment, comparison.getConstraintType(), null, comparison.getRootNode());
        Collection<TreeConstraint> constraints =
            buildConstraintCollection(
                constraint, leftExpression.getConstraints(), rightExpression.getConstraints());
        statesBuilder.add(new PartialState(FALSE_COMPARISON_RESULT, constraints, false));
      }
    }

    return statesBuilder.build();
  }

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

  private static TreeNode subtract(TreeNode leftNode, TreeNode rightNode) {
    if (leftNode instanceof ConstantTreeNode
        && ((ConstantTreeNode) leftNode).getConstant().isZero()) {
      return new UnaryTreeNode(UnaryOperator.NEGATE, rightNode);
    } else if (rightNode instanceof ConstantTreeNode
        && ((ConstantTreeNode) rightNode).getConstant().isZero()) {
      return leftNode;
    } else {
      return new BinaryTreeNode(BinaryOperator.SUBTRACT, leftNode, rightNode);
    }
  }

  private static TreeNode subtractWithEpsilon(
      TreeNode leftNode, TreeNode rightNode, EpsilonType epsilonType) {
    TreeNode temp = subtract(leftNode, rightNode);
    switch (epsilonType) {
      case ADD_EPSILON:
        temp = new BinaryTreeNode(BinaryOperator.ADD, temp, new ConstantTreeNode(EPSILON));
        break;
      case ADD_EPSILON_TWICE:
        temp = new BinaryTreeNode(BinaryOperator.ADD, temp, new ConstantTreeNode(DOUBLE_EPSILON));
        break;
      case SUBTRACT_EPSILON:
        temp = new BinaryTreeNode(BinaryOperator.SUBTRACT, temp, new ConstantTreeNode(EPSILON));
        break;
      case SUBTRACT_EPSILON_TWICE:
        temp =
            new BinaryTreeNode(BinaryOperator.SUBTRACT, temp, new ConstantTreeNode(DOUBLE_EPSILON));
        break;
      case INTERVAL_EPSILON:
        temp = new BinaryTreeNode(BinaryOperator.ADD, temp, new ConstantTreeNode(EPSILON_INTERVAL));
        break;
      case EXACT:
        // add no epsilon
        break;
      default:
        throw new AssertionError("Unhandled EpsilonType: " + epsilonType);
    }
    return temp;
  }

  /**
   * Applies the unary arithmetic operator on the partial state.
   *
   * @param operator unary arithmetic operator that is applied to the state
   * @param pState state on which the arithmetic operator is applied
   * @param pRoundingType type the unary expression is rounded to
   * @param pRoundingMode direction of the rounding operation
   * @return new partial state
   */
  public static Collection<PartialState> applyUnaryArithmeticOperator(
      UnaryOperator operator,
      Collection<PartialState> pState,
      RoundingType pRoundingType,
      RoundingMode pRoundingMode) {
    ImmutableSet.Builder<PartialState> builder = new ImmutableSet.Builder<>();
    for (PartialState expression : pState) {
      builder.add(applyUnaryArithmeticOperator(operator, expression, pRoundingType, pRoundingMode));
    }
    return builder.build();
  }

  private static PartialState applyUnaryArithmeticOperator(
      UnaryOperator operator,
      PartialState pState,
      RoundingType pRoundingType,
      RoundingMode pRoundingMode) {
    TreeNode root = pState.getPartialConstraint();
    root = new UnaryTreeNode(operator, root, pRoundingType, pRoundingMode);
    return new PartialState(root, pState.getConstraints(), pState.containsFloatVariable);
  }

  static RoundingType convertToRoundingType(CExpression expr) {
    if (expr.getExpressionType() instanceof CSimpleType) {
      return convertToRoundingType((CSimpleType) expr.getExpressionType());
    } else {
      return DEFAULT_ROUNDING_TYPE;
    }
  }

  static RoundingType convertToRoundingType(CSimpleType type) {
    switch (type.getType()) {
      case BOOL:
      case CHAR:
      case INT128:
      case INT: // fall through
        return RoundingType.INT;
      case FLOAT:
        return RoundingType.FLOAT;
      case DOUBLE:
        return RoundingType.DOUBLE;
      case FLOAT128:
        return RoundingType.QUAD;
      case UNSPECIFIED: // fall through
      default:
        return DEFAULT_ROUNDING_TYPE;
    }
  }

  /** Describes the assumption that should be made when computing the constraints. */
  enum TruthAssumption {
    /** Assumption is true. */
    ASSUME_TRUE,
    /** Assumption is false. */
    ASSUME_FALSE,
    /** Assumption can be either true or false. */
    ASSUME_EITHER;
  }

  public enum ApplyEpsilon {
    /** Apply an epsilon to the constraint. */
    APPLY_EPSILON,
    /** Use the exact constraint. */
    EXACT;
  }

  private enum EpsilonType {
    /** No epsilon value should be used. */
    EXACT,
    /** Increase the values by epsilon. */
    ADD_EPSILON,
    /** Increase the value by 2*epsilon. */
    ADD_EPSILON_TWICE,
    /** Decrease the values by epsilon. */
    SUBTRACT_EPSILON,
    /** Decrease the value by 2*epsilon. */
    SUBTRACT_EPSILON_TWICE,
    /** Widen the values by the interval. */
    INTERVAL_EPSILON;
  }
}
