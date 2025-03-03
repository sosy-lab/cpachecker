// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.interval;

import com.google.common.collect.ImmutableSet;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/**
 * Utility class for working with expression.
 */
public class ExpressionUtility {

  private static final CType CONCRETE_INDEX_TYPE = new CSimpleType(
      false, false, CBasicType.INT, false, false, true, false, false, false, false);

  private ExpressionUtility() {}

  public static CIntegerLiteralExpression getIntegerExpression(long value) {
    return CIntegerLiteralExpression.createDummyLiteral(value, CONCRETE_INDEX_TYPE);
  }

  public static CExpression incrementExpression(CExpression expression, long amount) {

    if (expression instanceof CIntegerLiteralExpression literalExpression) {
      return new CIntegerLiteralExpression(
          expression.getFileLocation(),
          expression.getExpressionType(),
          literalExpression.getValue().add(BigInteger.valueOf(amount))
      );
    }

    return new CBinaryExpression(
        expression.getFileLocation(),
        expression.getExpressionType(),
        expression.getExpressionType(),
        expression,
        getIntegerExpression(amount),
        BinaryOperator.PLUS
    );
  }

  public static CExpression incrementExpression(CExpression expression) {
    return incrementExpression(expression, 1);
  }

  public static Set<NormalFormExpression> normalizeExpression(CExpression expression, ExpressionValueVisitor visitor)
      throws UnrecognizedCodeException {
    if (expression instanceof CBinaryExpression binaryExpression) {
      return normalizeBinaryExpression(binaryExpression, visitor);
    }  else if (expression instanceof CUnaryExpression unaryExpression) {
      return normalizeUnaryExpression(unaryExpression, visitor);
    } else if (expression instanceof CIdExpression idExpression) {
      return normalizeIdExpression(idExpression, visitor);
    } else if (expression instanceof CIntegerLiteralExpression literalExpression) {
      return normalizeLiteralExpression(literalExpression);
    }
    // Normalization of other types of expressions not yet implemented
    return ImmutableSet.of();
  }

  public static Set<NormalFormExpression> normalizeBinaryExpression(CBinaryExpression expression, ExpressionValueVisitor visitor)
      throws UnrecognizedCodeException {
    return switch (expression.getOperator()) {
      case PLUS -> {
        List<NormalFormExpression> result = new ArrayList<>();
        result.addAll(normalizeAndConcretize(expression.getOperand1(), expression.getOperand2(), visitor));
        result.addAll(normalizeAndConcretize(expression.getOperand2(), expression.getOperand1(), visitor));
        yield ImmutableSet.copyOf(result);
      }
      case MINUS ->
         normalizeAndConcretize(expression.getOperand1(), invertExpression(expression.getOperand2()), visitor);
      default -> ImmutableSet.of();
    };
  }

  /**
   * Utility function for normalizing addition and subtraction operations.
   *
   * @param normalize normalizes this operand.
   * @param concretize concretizes this operand if possible and adds its value to all possible
   *                   normalizations.
   * @param visitor the expression visitor.
   * @return a set of normalizations.
   * @throws UnrecognizedCodeException if there is unrecognized code.
   */
  private static Set<NormalFormExpression> normalizeAndConcretize(CExpression normalize, CExpression concretize, ExpressionValueVisitor visitor)
      throws UnrecognizedCodeException {
    return concretize.accept(visitor).getUniqueConcreteValue()
        .map(concreteValue -> {
          try {
            return normalizeExpression(normalize, visitor).stream()
                .map(normalized -> normalized.add(concreteValue))
                .collect(ImmutableSet.toImmutableSet());
          } catch (UnrecognizedCodeException ignored) {
            return ImmutableSet.<NormalFormExpression>of();
          }
        }).orElse(ImmutableSet.of());
  }

  public static Set<NormalFormExpression> normalizeUnaryExpression(CUnaryExpression expression, ExpressionValueVisitor visitor)
      throws UnrecognizedCodeException {
    if (expression.getOperator() == UnaryOperator.MINUS) {
      return expression.getOperand().accept(visitor)
          .getUniqueConcreteValue()
          .map(e -> new NormalFormExpression(-e))
          .stream().collect(ImmutableSet.toImmutableSet());
    }
    return ImmutableSet.of(); // Other operators not yet implemented
  }

  public static Set<NormalFormExpression> normalizeIdExpression(CIdExpression expression, ExpressionValueVisitor visitor)
      throws UnrecognizedCodeException {
    List<NormalFormExpression> normalFormExpressions = new ArrayList<>();

    expression.accept(visitor)
        .getUniqueConcreteValue()
        .map(e -> new NormalFormExpression(e))
        .ifPresent(normalFormExpressions::add);

    normalFormExpressions.add(new NormalFormExpression(expression));
    return ImmutableSet.copyOf(normalFormExpressions);
  }

  public static Set<NormalFormExpression> normalizeLiteralExpression(CIntegerLiteralExpression expression) {
    return ImmutableSet.of(new NormalFormExpression(expression.asLong()));
  }

  public static CExpression invertExpression(CExpression expression) {
    return new CUnaryExpression(expression.getFileLocation(), expression.getExpressionType(), expression, UnaryOperator.MINUS);
  }

  public static boolean isSyntacticallyGreaterThanOrEqualTo(CExpression a, CExpression b, ExpressionValueVisitor visitor) {
    try {
      return NormalFormExpression.anyInSets(
          normalizeExpression(a, visitor),
          normalizeExpression(b, visitor),
          (normalA, normalB) -> normalA.isSyntacticallyGreaterThanOrEqualTo(normalB)
      );
    } catch (UnrecognizedCodeException exception) {
      return false;
    }
  }

  public static boolean isSyntacticallyLessThanOrEqualTo(CExpression a, CExpression b, ExpressionValueVisitor visitor) {
    try {
      return NormalFormExpression.anyInSets(
          normalizeExpression(a, visitor),
          normalizeExpression(b, visitor),
          (normalA, normalB) -> normalA.isSyntacticallyLessThanOrEqualTo(normalB)
      );
    } catch (UnrecognizedCodeException exception) {
      return false;
    }
  }
}
