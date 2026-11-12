// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.acsl;

import java.math.BigInteger;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslArraySubscriptTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslAstNode;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTermPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBooleanLiteralPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBooleanLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslCExpression;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslCVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslCharLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslIdTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslIntegerLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslStringLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTernaryPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryTerm;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeQualifiers;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTreeFactory;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;

/**
 * Converts ACSL predicates that consist only of constructs of C, i.e., without ACSL builtins like
 * \old or quantifiers, into expression trees of C expressions.
 */
public final class AcslToExpressionTreeConverter {

  private final MachineModel machineModel;
  private final CBinaryExpressionBuilder binaryExpressionBuilder;
  private final ExpressionTreeFactory<Object> factory = ExpressionTrees.newFactory();

  public AcslToExpressionTreeConverter(MachineModel pMachineModel, LogManager pLogger) {
    machineModel = pMachineModel;
    binaryExpressionBuilder = new CBinaryExpressionBuilder(pMachineModel, pLogger);
  }

  /** Whether the predicate consists only of constructs of C, so that it can be converted. */
  public boolean canConvert(AcslPredicate pPredicate) {
    try {
      convert(pPredicate);
      return true;
    } catch (UnrecognizedCodeException e) {
      return false;
    }
  }

  public ExpressionTree<Object> convert(AcslPredicate pPredicate) throws UnrecognizedCodeException {
    return convert(pPredicate, false);
  }

  /** Converts the predicate, or its negation if pNegated is true, in negation normal form. */
  private ExpressionTree<Object> convert(AcslPredicate pPredicate, boolean pNegated)
      throws UnrecognizedCodeException {
    return switch (pPredicate) {
      case AcslBooleanLiteralPredicate literal ->
          literal.getValue() != pNegated ? ExpressionTrees.getTrue() : ExpressionTrees.getFalse();
      case AcslUnaryPredicate unary -> convert(unary.getOperand(), !pNegated);
      case AcslBinaryPredicate binary -> {
        AcslPredicate left = binary.getOperand1();
        AcslPredicate right = binary.getOperand2();
        yield switch (binary.getOperator()) {
          case AND ->
              pNegated
                  ? factory.or(convert(left, true), convert(right, true))
                  : factory.and(convert(left, false), convert(right, false));
          case OR ->
              pNegated
                  ? factory.and(convert(left, true), convert(right, true))
                  : factory.or(convert(left, false), convert(right, false));
          case IMPLICATION ->
              pNegated
                  ? factory.and(convert(left, false), convert(right, true))
                  : factory.or(convert(left, true), convert(right, false));
          case EQUIVALENT ->
              factory.or(
                  factory.and(convert(left, false), convert(right, pNegated)),
                  factory.and(convert(left, true), convert(right, !pNegated)));
          case XOR ->
              factory.or(
                  factory.and(convert(left, false), convert(right, !pNegated)),
                  factory.and(convert(left, true), convert(right, pNegated)));
        };
      }
      case AcslTernaryPredicate ternary ->
          factory.or(
              factory.and(
                  convert(ternary.getCondition(), false),
                  convert(ternary.getResultIfTrue(), pNegated)),
              factory.and(
                  convert(ternary.getCondition(), true),
                  convert(ternary.getResultIfFalse(), pNegated)));
      case AcslBinaryTermPredicate comparison -> {
        BinaryOperator operator =
            switch (comparison.getOperator()) {
              case EQUALS -> BinaryOperator.EQUALS;
              case NOT_EQUALS -> BinaryOperator.NOT_EQUALS;
              case LESS_EQUAL -> BinaryOperator.LESS_EQUAL;
              case GREATER_EQUAL -> BinaryOperator.GREATER_EQUAL;
              case LESS_THAN -> BinaryOperator.LESS_THAN;
              case GREATER_THAN -> BinaryOperator.GREATER_THAN;
            };
        yield factory.leaf(
            binaryExpressionBuilder.buildBinaryExpression(
                convert(comparison.getOperand1()), convert(comparison.getOperand2()), operator),
            !pNegated);
      }
      default ->
          throw unsupported("ACSL predicate cannot be converted to a C expression", pPredicate);
    };
  }

  private CExpression convert(AcslTerm pTerm) throws UnrecognizedCodeException {
    return switch (pTerm) {
      case AcslIdTerm id when id.getDeclaration() instanceof AcslCVariableDeclaration variable ->
          new CIdExpression(id.getFileLocation(), variable.getCVariableDeclaration());
      case AcslIntegerLiteralTerm literal ->
          new CIntegerLiteralExpression(
              literal.getFileLocation(),
              fitsIntoInt(literal.getValue()) ? CNumericTypes.INT : CNumericTypes.LONG_LONG_INT,
              literal.getValue());
      case AcslBooleanLiteralTerm literal ->
          new CIntegerLiteralExpression(
              literal.getFileLocation(),
              CNumericTypes.INT,
              literal.getValue() ? BigInteger.ONE : BigInteger.ZERO);
      case AcslCharLiteralTerm literal ->
          new CCharLiteralExpression(
              literal.getFileLocation(), CNumericTypes.CHAR, literal.getValue());
      case AcslStringLiteralTerm literal ->
          new CStringLiteralExpression(literal.getFileLocation(), literal.getValue());
      case AcslCExpression expression -> expression.getCExpression();
      case AcslBinaryTerm binary -> {
        BinaryOperator operator =
            switch (binary.getOperator()) {
              case BINARY_AND -> BinaryOperator.BITWISE_AND;
              case BINARY_OR -> BinaryOperator.BITWISE_OR;
              case BINARY_XOR -> BinaryOperator.BITWISE_XOR;
              case PLUS -> BinaryOperator.PLUS;
              case MINUS -> BinaryOperator.MINUS;
              case MULTIPLY -> BinaryOperator.MULTIPLY;
              case DIVIDE -> BinaryOperator.DIVIDE;
              case MODULO -> BinaryOperator.REMAINDER;
              case SHIFT_LEFT -> BinaryOperator.SHIFT_LEFT;
              case SHIFT_RIGHT -> BinaryOperator.SHIFT_RIGHT;
              case BINARY_IMPLICATION, BINARY_EQUIVALENT ->
                  throw unsupported("ACSL operator cannot be converted to a C expression", binary);
            };
        yield binaryExpressionBuilder.buildBinaryExpression(
            convert(binary.getOperand1()), convert(binary.getOperand2()), operator);
      }
      case AcslUnaryTerm unary -> convertUnaryTerm(unary);
      case AcslArraySubscriptTerm subscript -> {
        CExpression array = convert(subscript.getArrayExpression());
        yield new CArraySubscriptExpression(
            subscript.getFileLocation(),
            targetType(array, subscript),
            array,
            convert(subscript.getSubscriptExpression()));
      }
      default -> throw unsupported("ACSL term cannot be converted to a C expression", pTerm);
    };
  }

  private CExpression convertUnaryTerm(AcslUnaryTerm pTerm) throws UnrecognizedCodeException {
    CExpression operand = convert(pTerm.getOperand());
    return switch (pTerm.getOperator()) {
      case PLUS -> operand;
      case MINUS ->
          new CUnaryExpression(
              pTerm.getFileLocation(), operand.getExpressionType(), operand, UnaryOperator.MINUS);
      case NEGATION ->
          binaryExpressionBuilder.buildBinaryExpression(
              operand, CIntegerLiteralExpression.ZERO, BinaryOperator.EQUALS);
      case POINTER_DEREFERENCE ->
          new CPointerExpression(pTerm.getFileLocation(), targetType(operand, pTerm), operand);
      case ADDRESS_OF ->
          new CUnaryExpression(
              pTerm.getFileLocation(),
              new CPointerType(CTypeQualifiers.NONE, operand.getExpressionType()),
              operand,
              UnaryOperator.AMPER);
      case SIZEOF -> throw unsupported("ACSL sizeof cannot be converted to a C expression", pTerm);
    };
  }

  private static CType targetType(CExpression pPointer, AcslTerm pTerm)
      throws UnrecognizedCodeException {
    return switch (pPointer.getExpressionType().getCanonicalType()) {
      case CPointerType pointer -> pointer.getType();
      case CArrayType array -> array.getType();
      default -> throw unsupported("Dereference of a non-pointer", pTerm);
    };
  }

  private boolean fitsIntoInt(BigInteger pValue) {
    return machineModel.getMinimalIntegerValue(CNumericTypes.INT).compareTo(pValue) <= 0
        && pValue.compareTo(machineModel.getMaximalIntegerValue(CNumericTypes.INT)) <= 0;
  }

  private static UnrecognizedCodeException unsupported(String pMessage, AcslAstNode pNode) {
    // The exception does not support ACSL nodes, which belong to no language of the CFA
    return new UnrecognizedCodeException(pMessage + ": " + pNode.toASTString(), (CFAEdge) null);
  }
}
