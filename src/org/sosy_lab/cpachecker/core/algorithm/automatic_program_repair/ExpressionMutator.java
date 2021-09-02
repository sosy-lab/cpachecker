// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.automatic_program_repair;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
  public static ArrayList<CExpression> calcMutationsFor(CExpression originalExpression, CFA cfa) {
    ArrayList<CExpression> alternativeExpressions = new ArrayList();
    final Set<CExpression> expressions = ExpressionCollector.collectExpressions(cfa);
    final Map<CType, Set<CExpression>> expressionsSortedByType =
        groupExpressionsByType(expressions);
    final Map<CBasicType, Set<CExpression>> expressionsSortedByBasicType =
        groupExpressionsByBasicType(expressions);

    if (originalExpression instanceof CBinaryExpression) {
      alternativeExpressions = calcMutationsFor((CBinaryExpression) originalExpression, cfa);
    } else if (originalExpression instanceof CUnaryExpression) {
      alternativeExpressions = calcMutationsFor((CUnaryExpression) originalExpression, cfa);
    }
    final CType type = originalExpression.getExpressionType();
    Set<CExpression> sameTypeExpressions;

    if (type instanceof CSimpleType) {
      sameTypeExpressions = expressionsSortedByBasicType.get(((CSimpleType) type).getType());
    } else {
      sameTypeExpressions = expressionsSortedByType.get(type);
    }

    for (CExpression expression : sameTypeExpressions) {
      if (!originalExpression.equals(expression)) {
        alternativeExpressions.add(expression);
      }
    }

    return alternativeExpressions;
  }

  private static ArrayList<CExpression> calcMutationsFor(
      CUnaryExpression originalUnaryExpression, CFA cfa) {
    ArrayList<CExpression> alternativeExpressions = new ArrayList();

    for (UnaryOperator operator : UnaryOperator.values()) {
      if (originalUnaryExpression.getOperator() != operator) {
        final CUnaryExpression modifiedExpression =
            replaceUnaryOperator(originalUnaryExpression, operator);
        alternativeExpressions.add(modifiedExpression);
      }
    }

    for (CExpression alternativeExpression :
        calcMutationsFor(originalUnaryExpression.getOperand(), cfa)) {
      final CUnaryExpression modifiedExpression =
          replaceUnaryOperand(originalUnaryExpression, alternativeExpression);
      alternativeExpressions.add(modifiedExpression);
    }

    return alternativeExpressions;
  }

  private static ArrayList<CExpression> calcMutationsFor(
      CBinaryExpression originalBinaryExpression, CFA cfa) {
    ArrayList<CExpression> alternativeExpressions = new ArrayList();

    for (BinaryOperator operator : BinaryOperator.values()) {
      if (originalBinaryExpression.getOperator().isLogicalOperator() == operator.isLogicalOperator()
          && originalBinaryExpression.getOperator() != operator) {
        final CBinaryExpression modifiedExpression =
            replaceBinaryOperator(originalBinaryExpression, operator);
        alternativeExpressions.add(modifiedExpression);
      }
    }

    for (CExpression alternativeExpression :
        calcMutationsFor(originalBinaryExpression.getOperand2(), cfa)) {
      final CBinaryExpression modifiedExpression =
          replaceBinaryOperand(originalBinaryExpression, alternativeExpression);
      alternativeExpressions.add(modifiedExpression);
    }

    return alternativeExpressions;
  }

  public static ArrayList<CFunctionCallExpression> calcMutationsFor(
      CFunctionCallExpression originalFunctionCallExpression, CFA cfa) {
    ArrayList<CFunctionCallExpression> alternativeFunctionCallExpressions = new ArrayList<>();
    List<CExpression> originalParameterExpressions =
        originalFunctionCallExpression.getParameterExpressions();

    /* Creates a list of CFunctionCallAssignmentStatements, where each entry has a copy of the original
    parameter list but with one altered parameter */
    for (int i = 0; i < originalParameterExpressions.size(); i++) {
      CExpression originalParameterExpression = originalParameterExpressions.get(i);

      for (CExpression alternativeParameterExpression :
          calcMutationsFor(originalParameterExpression, cfa)) {

        ArrayList<CExpression> alternatives = new ArrayList<>(originalParameterExpressions);
        alternatives.set(i, alternativeParameterExpression);

        alternativeFunctionCallExpressions.add(
            replaceParameters(originalFunctionCallExpression, alternatives));
      }
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
    final Map<CType, Set<CExpression>> expressionsSortedByType = Maps.newHashMap();

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
    final Map<CBasicType, Set<CExpression>> expressionsSortedByType = Maps.newHashMap();

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
