// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.numeric.visitor;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cpa.numeric.NumericTransferRelation;
import org.sosy_lab.cpachecker.cpa.numeric.NumericTransferRelation.VariableSubstitution;
import org.sosy_lab.cpachecker.cpa.numeric.visitor.PartialState.TruthAssumption;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.numericdomains.coefficients.Coefficient;
import org.sosy_lab.numericdomains.coefficients.MpqScalar;
import org.sosy_lab.numericdomains.constraint.tree.BinaryOperator;
import org.sosy_lab.numericdomains.constraint.tree.ConstantTreeNode;
import org.sosy_lab.numericdomains.constraint.tree.TreeNode;
import org.sosy_lab.numericdomains.constraint.tree.UnaryOperator;
import org.sosy_lab.numericdomains.constraint.tree.VariableTreeNode;
import org.sosy_lab.numericdomains.environment.Environment;
import org.sosy_lab.numericdomains.environment.Variable;

/**
 * Creates a right hand side visitor for numeric domains.
 *
 * <p>The right hand side is interpreted as a {@link Collection} of {@link PartialState}s.
 */
public class NumericRightHandSideVisitor
    implements CRightHandSideVisitor<Collection<PartialState>, UnrecognizedCodeException> {
  private final LogManager logger;

  /** If present, this is a variable substitution that should be applied. */
  private final Optional<VariableSubstitution> variableSubstitution;

  /**
   * The environment of the right hand side expression. This is not necessarily equal to the
   * environment of the state.
   */
  private final Environment environment;

  /**
   * Creates a right hand side visitor for numeric domains.
   *
   * @param pEnvironment environment of the right hand side
   * @param pVariableSubstitution variable substitution that should be applied if the variable is
   *     encountered in the right hand side
   * @param logManager log manager used for logging
   */
  public NumericRightHandSideVisitor(
      Environment pEnvironment,
      @Nullable VariableSubstitution pVariableSubstitution,
      LogManager logManager) {
    logger = logManager;
    environment = pEnvironment;
    variableSubstitution = Optional.ofNullable(pVariableSubstitution);
  }

  @Override
  public Collection<PartialState> visit(CBinaryExpression pIastBinaryExpression)
      throws UnrecognizedCodeException {
    // Create Partial states for the left hand side and for the right hand side.
    Collection<PartialState> leftExpression = pIastBinaryExpression.getOperand1().accept(this);
    Collection<PartialState> rightExpression = pIastBinaryExpression.getOperand2().accept(this);

    final Collection<PartialState> newExpressions;

    // Apply the arithmetic operator on each pair of partial states in the left and right hand side
    if (arithmeticOperators.contains(pIastBinaryExpression.getOperator())) {
      newExpressions =
          handleArithmeticOperator(pIastBinaryExpression, leftExpression, rightExpression);
    } else if (comparisonOperators.contains(pIastBinaryExpression.getOperator())) {
      newExpressions =
          handleComparisonOperator(pIastBinaryExpression, leftExpression, rightExpression);
    } else {
      newExpressions = ImmutableSet.of();
    }

    return newExpressions;
  }

  private Collection<PartialState> handleComparisonOperator(
      CBinaryExpression pIastBinaryExpression,
      Collection<PartialState> pLeftExpression,
      Collection<PartialState> pRightExpression) {

    return PartialState.applyComparisonOperator(
        pIastBinaryExpression.getOperator(),
        pLeftExpression,
        pRightExpression,
        TruthAssumption.ASSUME_EITHER,
        environment);
  }

  private Collection<PartialState> handleArithmeticOperator(
      CBinaryExpression pIastBinaryExpression,
      Collection<PartialState> leftExpressions,
      Collection<PartialState> rightExpressions) {
    BinaryOperator operator;

    switch (pIastBinaryExpression.getOperator()) {
      case PLUS:
        operator = BinaryOperator.ADD;
        break;
      case MINUS:
        operator = BinaryOperator.SUBTRACT;
        break;
      case MULTIPLY:
        operator = BinaryOperator.MULTIPLY;
        break;
      case DIVIDE:
        operator = BinaryOperator.DIVIDE;
        break;
      case MODULO:
        operator = BinaryOperator.MODULO;
        break;
      default:
        throw new AssertionError(
            "'" + pIastBinaryExpression.getOperator() + "' is not an arithmetic operation.");
    }

    return PartialState.applyBinaryArithmeticOperator(operator, leftExpressions, rightExpressions);
  }

  @Override
  public Collection<PartialState> visit(CFunctionCallExpression pIastFunctionCallExpression)
      throws UnrecognizedCodeException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Collection<PartialState> visit(CCastExpression pIastCastExpression)
      throws UnrecognizedCodeException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Collection<PartialState> visit(CCharLiteralExpression pIastCharLiteralExpression)
      throws UnrecognizedCodeException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Collection<PartialState> visit(CFloatLiteralExpression pIastFloatLiteralExpression)
      throws UnrecognizedCodeException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Collection<PartialState> visit(CIntegerLiteralExpression pIastIntegerLiteralExpression)
      throws UnrecognizedCodeException {
    Coefficient coeff = MpqScalar.of(pIastIntegerLiteralExpression.asLong());
    TreeNode constNode = new ConstantTreeNode(coeff);
    PartialState out = new PartialState(constNode, ImmutableSet.of());
    return ImmutableSet.of(out);
  }

  @Override
  public Collection<PartialState> visit(CStringLiteralExpression pIastStringLiteralExpression)
      throws UnrecognizedCodeException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Collection<PartialState> visit(CTypeIdExpression pIastTypeIdExpression)
      throws UnrecognizedCodeException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Collection<PartialState> visit(CUnaryExpression pIastUnaryExpression)
      throws UnrecognizedCodeException {
    CUnaryExpression.UnaryOperator operator = pIastUnaryExpression.getOperator();

    Collection<PartialState> states = pIastUnaryExpression.getOperand().accept(this);

    switch (operator) {
      case MINUS:
        return PartialState.applyUnaryArithmeticOperator(UnaryOperator.NEGATE, states);
      default:
        throw new UnrecognizedCodeException(
            "Cant handle unary expression: " + operator, pIastUnaryExpression);
    }
  }

  @Override
  public Collection<PartialState> visit(CImaginaryLiteralExpression PIastLiteralExpression)
      throws UnrecognizedCodeException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Collection<PartialState> visit(CAddressOfLabelExpression pAddressOfLabelExpression)
      throws UnrecognizedCodeException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Collection<PartialState> visit(CArraySubscriptExpression pIastArraySubscriptExpression)
      throws UnrecognizedCodeException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Collection<PartialState> visit(CFieldReference pIastFieldReference)
      throws UnrecognizedCodeException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Collection<PartialState> visit(CIdExpression pIastIdExpression)
      throws UnrecognizedCodeException {
    final Variable maybeVariable =
        NumericTransferRelation.createVariableFromDeclaration(pIastIdExpression.getDeclaration());

    Variable variable =
        variableSubstitution.map((varSub) -> varSub.applyTo(maybeVariable)).orElse(maybeVariable);
    TreeNode node = new VariableTreeNode(variable);
    PartialState out = new PartialState(node, ImmutableSet.of());
    return ImmutableSet.of(out);
  }

  @Override
  public Collection<PartialState> visit(CPointerExpression pointerExpression)
      throws UnrecognizedCodeException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Collection<PartialState> visit(CComplexCastExpression complexCastExpression)
      throws UnrecognizedCodeException {
    throw new UnsupportedOperationException();
  }

  private static final EnumSet<CBinaryExpression.BinaryOperator> arithmeticOperators =
      EnumSet.of(
          CBinaryExpression.BinaryOperator.PLUS,
          CBinaryExpression.BinaryOperator.MINUS,
          CBinaryExpression.BinaryOperator.DIVIDE,
          CBinaryExpression.BinaryOperator.MULTIPLY,
          CBinaryExpression.BinaryOperator.MODULO);

  private static final EnumSet<CBinaryExpression.BinaryOperator> comparisonOperators =
      EnumSet.of(
          CBinaryExpression.BinaryOperator.EQUALS,
          CBinaryExpression.BinaryOperator.NOT_EQUALS,
          CBinaryExpression.BinaryOperator.GREATER_EQUAL,
          CBinaryExpression.BinaryOperator.GREATER_EQUAL,
          CBinaryExpression.BinaryOperator.GREATER_THAN,
          CBinaryExpression.BinaryOperator.LESS_EQUAL,
          CBinaryExpression.BinaryOperator.LESS_THAN);
}
