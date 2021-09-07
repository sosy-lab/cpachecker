// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.automatic_program_repair;

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

public class ExpressionMutator {
  /**
   * Returns a list of possible mutations for a given expression. A mutation consists either in
   * flipping a binary or unary operator, or a different expression found in the same file that is
   * of the same CBasicType.
   */
  public static Stream<CExpression> calcMutationsFor(CExpression originalExpression, CFA cfa) {
    Stream<CExpression> unaryOrBinaryExpressions = Stream.empty();
    final Set<CExpression> expressions = ExpressionCollector.collectExpressions(cfa);
    final Map<CType, Set<CExpression>> expressionsSortedByType =
        groupExpressionsByType(expressions);
    final Map<CBasicType, Set<CExpression>> expressionsSortedByBasicType =
        groupExpressionsByBasicType(expressions);

    if (originalExpression instanceof CBinaryExpression) {
      unaryOrBinaryExpressions = calcMutationsFor((CBinaryExpression) originalExpression, cfa);
    } else if (originalExpression instanceof CUnaryExpression) {
      unaryOrBinaryExpressions = calcMutationsFor((CUnaryExpression) originalExpression, cfa);
    }

    final CType type = originalExpression.getExpressionType();
    Set<CExpression> sameTypeExpressionsSet;

    if (type instanceof CSimpleType) {
      sameTypeExpressionsSet = expressionsSortedByBasicType.get(((CSimpleType) type).getType());
    } else {
      sameTypeExpressionsSet = expressionsSortedByType.get(type);
    }

    return Stream.concat(
        unaryOrBinaryExpressions,
        sameTypeExpressionsSet.stream()
            .filter((CExpression expression) -> !originalExpression.equals(expression)));
  }

  private static Stream<CExpression> calcMutationsFor(
      CUnaryExpression originalUnaryExpression, CFA cfa) {

    Stream<CExpression> alternativeOperatorExpressions =
        Arrays.stream(UnaryOperator.values())
            .filter((UnaryOperator operator) -> originalUnaryExpression.getOperator() != operator)
            .map(
                (UnaryOperator operator) ->
                    replaceUnaryOperator(originalUnaryExpression, operator));

    Stream<CExpression> alternativeOperandExpressions =
        calcMutationsFor(originalUnaryExpression.getOperand(), cfa)
            .map(
                (CExpression alternativeExpression) ->
                    replaceUnaryOperand(originalUnaryExpression, alternativeExpression));

    return Stream.concat(alternativeOperatorExpressions, alternativeOperandExpressions);
  }

  private static Stream<CExpression> calcMutationsFor(
      CBinaryExpression originalBinaryExpression, CFA cfa) {

    Stream<CExpression> alternativeOperatorExpressions =
        Arrays.stream(BinaryOperator.values())
            .filter(
                (BinaryOperator operator) ->
                    originalBinaryExpression.getOperator().isLogicalOperator()
                            == operator.isLogicalOperator()
                        && originalBinaryExpression.getOperator() != operator)
            .map(
                (BinaryOperator operator) ->
                    replaceBinaryOperator(originalBinaryExpression, operator));

    Stream<CExpression> alternativeOperandExpressions =
        calcMutationsFor(originalBinaryExpression.getOperand2(), cfa)
            .map(
                (CExpression alternativeExpression) ->
                    replaceBinaryOperand(originalBinaryExpression, alternativeExpression));

    return Stream.concat(alternativeOperatorExpressions, alternativeOperandExpressions);
  }

  public static Stream<CFunctionCallExpression> calcMutationsFor(
      CFunctionCallExpression originalFunctionCallExpression, CFA cfa) {
    Stream<CFunctionCallExpression> alternativeFunctionCallExpressions = Stream.empty();
    List<CExpression> originalParameterExpressions =
        originalFunctionCallExpression.getParameterExpressions();

    /* Creates a list of CFunctionCallAssignmentStatements, where each entry has a copy of the original
    parameter list but with one altered parameter */

    for (int i = 0; i < originalParameterExpressions.size(); i++) {
      CExpression originalParameterExpression = originalParameterExpressions.get(i);
      final int currentIndex = i;
      alternativeFunctionCallExpressions =
          calcMutationsFor(originalParameterExpression, cfa)
              .map(
                  (CExpression alternativeParameterExpression) -> {
                    List<CExpression> alternativeParameterList =
                        new ArrayList<>(originalParameterExpressions);
                    alternativeParameterList.set(currentIndex, alternativeParameterExpression);

                    return replaceParameters(
                        originalFunctionCallExpression, alternativeParameterList);
                  });
    }

    return alternativeFunctionCallExpressions;
  }

  /* REPLACEMENTS */
  private static CBinaryExpression replaceBinaryOperator(
      CBinaryExpression binaryExpression, BinaryOperator operator) {
    return new CBinaryExpression(
        binaryExpression.getFileLocation(),
        binaryExpression.getExpressionType(),
        binaryExpression.getCalculationType(),
        binaryExpression.getOperand1(),
        binaryExpression.getOperand2(),
        operator);
  }

  private static CBinaryExpression replaceBinaryOperand(
      CBinaryExpression binaryExpression, CExpression expression) {
    return new CBinaryExpression(
        binaryExpression.getFileLocation(),
        binaryExpression.getExpressionType(),
        binaryExpression.getCalculationType(),
        binaryExpression.getOperand1(),
        expression,
        binaryExpression.getOperator());
  }

  private static CUnaryExpression replaceUnaryOperator(
      CUnaryExpression unaryExpression, UnaryOperator operator) {
    return new CUnaryExpression(
        unaryExpression.getFileLocation(),
        unaryExpression.getExpressionType(),
        unaryExpression.getOperand(),
        operator);
  }

  private static CUnaryExpression replaceUnaryOperand(
      CUnaryExpression unaryExpression, CExpression expression) {
    return new CUnaryExpression(
        unaryExpression.getFileLocation(),
        unaryExpression.getExpressionType(),
        expression,
        unaryExpression.getOperator());
  }

  private static CFunctionCallExpression replaceParameters(
      CFunctionCallExpression originalFunctionCallExpression,
      List<CExpression> newParameterExpressions) {
    return new CFunctionCallExpression(
        originalFunctionCallExpression.getFileLocation(),
        originalFunctionCallExpression.getExpressionType(),
        originalFunctionCallExpression.getFunctionNameExpression(),
        newParameterExpressions,
        originalFunctionCallExpression.getDeclaration());
  }

  /* GROUPING */
  /** Groups a list of expressions by their CType into a map. */
  public static Map<CType, Set<CExpression>> groupExpressionsByType(Set<CExpression> expressions) {
    final Map<CType, Set<CExpression>> expressionsSortedByType = new HashMap<>();

    for (CExpression expression : expressions) {
      CType type = expression.getExpressionType();

      if (expressionsSortedByType.containsKey(type)) {
        expressionsSortedByType.get(type).add(expression);
      } else {
        expressionsSortedByType.put(type, Sets.newHashSet(expression));
      }
    }

    return expressionsSortedByType;
  }

  /** Groups a list of expressions by their CBasicType into a map. */
  public static Map<CBasicType, Set<CExpression>> groupExpressionsByBasicType(
      Set<CExpression> expressions) {
    final Map<CBasicType, Set<CExpression>> expressionsSortedByType = new HashMap<>();

    for (CExpression expression : expressions) {
      CType type = expression.getExpressionType();

      if (type instanceof CSimpleType) {
        CBasicType simpleType = ((CSimpleType) type).getType();

        if (expressionsSortedByType.containsKey(simpleType)) {
          expressionsSortedByType.get(simpleType).add(expression);
        } else {
          expressionsSortedByType.put(simpleType, Sets.newHashSet(expression));
        }
      }
    }

    return expressionsSortedByType;
  }
}
