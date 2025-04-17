// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.instrumentation;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.CFACreationUtils;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;

/**
 * Class for patterns defined on the transitions of instrumentation automaton. Should not be used
 * outside the Sequentialization operator ! Currently supported patterns: - Arbitrary String = The
 * match is true if the string on CFAEdge equals the pattern string. - [cond], [!cond] = The match
 * is true if the CFAEdge is the edge corresponding to satisfaction/ not satisfaction of the
 * assumption. - ADD, SUB = The match is true if the type of AST expression on CFAEdge is of type
 * AdditionExpression or SubstractionExpression - true, false = The match is always true/false
 */
public class InstrumentationPattern {
  private final patternType type;
  private final String functionName;
  private final String pattern;

  public InstrumentationPattern(String pPattern) {
    pattern = pPattern;
    if (pPattern.startsWith("FUNC")) {
      functionName = pPattern
          .replace("FUNC(", "")
          .replace(")","");
      type = patternType.FUNC;
      return;
    } else {
      functionName = "";
    }
    switch (pPattern) {
      case "true":
        type = patternType.TRUE;
        break;
      case "false":
        type = patternType.FALSE;
        break;
      case "[cond]":
        type = patternType.COND;
        break;
      case "[!cond]":
        type = patternType.NOT_COND;
        break;
      case "ADD":
        type = patternType.ADD;
        break;
      case "SUB":
        type = patternType.SUB;
        break;
      case "NEG":
        type = patternType.NEG;
        break;
      case "MUL":
        type = patternType.MUL;
        break;
      case "DIV":
        type = patternType.DIV;
        break;
      case "MOD":
        type = patternType.MOD;
        break;
      case "SHIFT":
        type = patternType.SHIFT;
        break;
      case "EQ":
        type = patternType.EQ;
        break;
      case "GEQ":
        type = patternType.GEQ;
        break;
      case "GR":
        type = patternType.GR;
        break;
      case "LEQ":
        type = patternType.LEQ;
        break;
      case "LS":
        type = patternType.LS;
        break;
      case "NEQ":
        type = patternType.NEQ;
        break;
      case "RSHIFT":
        type = patternType.RSHIFT;
        break;
      case "OR":
        type = patternType.OR;
        break;
      case "AND":
        type = patternType.AND;
        break;
      case "XOR":
        type = patternType.XOR;
        break;
      default:
        type = patternType.REGEX;
        break;
    }
  }

  /**
   * Checks if the provided CFAEdge matches the pattern.
   *
   * @return Null, if the edge does not match the pattern, otherwise returns the list of matched
   *     variables.
   */
  @Nullable
  public ImmutableList<String> matchThePattern(
      CFAEdge pCFAEdge, Map<CFANode, String> pDecomposedMap) {
    return switch (type) {
      case TRUE -> ImmutableList.of();
      case COND -> isOriginalCond(pCFAEdge) ? ImmutableList.of() : null;
      case NOT_COND -> isNegatedCond(pCFAEdge) ? ImmutableList.of() : null;
      case FUNC -> getTheOperandsFromFunctionCall(pCFAEdge, pDecomposedMap);
      case ADD -> getTheOperandsFromOperation(pCFAEdge, BinaryOperator.PLUS, pDecomposedMap);
      case SUB -> getTheOperandsFromOperation(pCFAEdge, BinaryOperator.MINUS, pDecomposedMap);
      case NEG -> getTheOperandsFromUnaryOperation(pCFAEdge, UnaryOperator.MINUS, pDecomposedMap);
      case MUL -> getTheOperandsFromOperation(pCFAEdge, BinaryOperator.MULTIPLY, pDecomposedMap);
      case DIV -> getTheOperandsFromOperation(pCFAEdge, BinaryOperator.DIVIDE, pDecomposedMap);
      case MOD -> getTheOperandsFromOperation(pCFAEdge, BinaryOperator.MODULO, pDecomposedMap);
      case SHIFT ->
          getTheOperandsFromOperation(pCFAEdge, BinaryOperator.SHIFT_LEFT, pDecomposedMap);
      case EQ -> getTheOperandsFromOperation(pCFAEdge, BinaryOperator.EQUALS, pDecomposedMap);
      case GEQ ->
          getTheOperandsFromOperation(pCFAEdge, BinaryOperator.GREATER_EQUAL, pDecomposedMap);
      case GR -> getTheOperandsFromOperation(pCFAEdge, BinaryOperator.GREATER_THAN, pDecomposedMap);
      case LEQ -> getTheOperandsFromOperation(pCFAEdge, BinaryOperator.LESS_EQUAL, pDecomposedMap);
      case LS -> getTheOperandsFromOperation(pCFAEdge, BinaryOperator.LESS_THAN, pDecomposedMap);
      case NEQ -> getTheOperandsFromOperation(pCFAEdge, BinaryOperator.NOT_EQUALS, pDecomposedMap);
      case RSHIFT ->
          getTheOperandsFromOperation(pCFAEdge, BinaryOperator.SHIFT_RIGHT, pDecomposedMap);
      case OR -> getTheOperandsFromOperation(pCFAEdge, BinaryOperator.BINARY_OR, pDecomposedMap);
      case AND -> getTheOperandsFromOperation(pCFAEdge, BinaryOperator.BINARY_AND, pDecomposedMap);
      case XOR -> getTheOperandsFromOperation(pCFAEdge, BinaryOperator.BINARY_XOR, pDecomposedMap);
      default -> null;
    };
  }

  @Override
  public String toString() {
    return pattern;
  }

  public String getFunctionName() {
    return functionName;
  }

  @Nullable
  private ImmutableList<String> getTheOperandsFromUnaryOperation(
      CFAEdge pCFAEdge, UnaryOperator pOperator, Map<CFANode, String> pDecomposedMap) {
    if (pCFAEdge.getRawAST().isPresent()) {
      AAstNode astNode = pCFAEdge.getRawAST().orElseThrow();
      CExpression expression = LoopInfoUtils.extractExpression(astNode);
      if (expression instanceof CUnaryExpression
          && ((CUnaryExpression) expression).getOperator().equals(pOperator)) {
        CExpression operand = ((CUnaryExpression) expression).getOperand();

        String condition = collectConditionFromPreviousEdge(pCFAEdge);
        if (pDecomposedMap.containsKey(pCFAEdge.getPredecessor())) {
          condition = condition + " && " + pDecomposedMap.get(pCFAEdge.getPredecessor());
        }

        if (expression.getExpressionType().getCanonicalType().toString().matches("signed.*int")) {
          if (expression.getExpressionType().getCanonicalType().toString().contains("long long")) {
            return ImmutableList.of(
                removeIndicesOfVariablesWithSameName(operand, pCFAEdge),
                condition,
                "TRANS_LONG_LONG_MAX",
                "TRANS_LONG_LONG_MIN");
          }
          return ImmutableList.of(
              removeIndicesOfVariablesWithSameName(operand, pCFAEdge),
              condition,
              "TRANS_INT_MAX",
              "TRANS_INT_MIN");
        } else {
          return ImmutableList.of();
        }
      }
    }
    return null;
  }

  @Nullable
  private ImmutableList<String> getTheOperandsFromOperation(
      CFAEdge pCFAEdge, BinaryOperator pOperator, Map<CFANode, String> pDecomposedMap) {
    if (pCFAEdge.getRawAST().isPresent()) {
      AAstNode astNode = pCFAEdge.getRawAST().orElseThrow();
      CExpression expression = LoopInfoUtils.extractExpression(astNode);
      if (expression instanceof CBinaryExpression
          && ((CBinaryExpression) expression).getOperator().equals(pOperator)) {
        CExpression operand1 = ((CBinaryExpression) expression).getOperand1();
        CExpression operand2 = ((CBinaryExpression) expression).getOperand2();

        String condition = collectConditionFromPreviousEdge(pCFAEdge);
        if (pDecomposedMap.containsKey(pCFAEdge.getPredecessor())) {
          condition = condition + " && " + pDecomposedMap.get(pCFAEdge.getPredecessor());
        }
        if (expression.getExpressionType().getCanonicalType().toString().matches("signed.*int")
            && (!(operand1.getExpressionType().getCanonicalType() instanceof CPointerType)
                || !(operand2.getExpressionType().getCanonicalType() instanceof CPointerType))) {
          if (expression.getExpressionType().getCanonicalType().toString().contains("long long")) {
            return ImmutableList.of(
                removeIndicesOfVariablesWithSameName(operand1, pCFAEdge),
                removeIndicesOfVariablesWithSameName(operand2, pCFAEdge),
                condition,
                "TRANS_LONG_LONG_MAX",
                "TRANS_LONG_LONG_MIN");
          }
          return ImmutableList.of(
              removeIndicesOfVariablesWithSameName(operand1, pCFAEdge),
              removeIndicesOfVariablesWithSameName(operand2, pCFAEdge),
              condition,
              "TRANS_INT_MAX",
              "TRANS_INT_MIN");
        } else {
          return ImmutableList.of();
        }
      }
    }
    return null;
  }

  @Nullable
  private ImmutableList<String> getTheOperandsFromFunctionCall(
      CFAEdge pCFAEdge, Map<CFANode, String> pDecomposedMap) {
    if (pCFAEdge.getRawAST().isPresent()) {
      AAstNode astNode = pCFAEdge.getRawAST().orElseThrow();
      CFunctionCallExpression expression;
      if (astNode instanceof CReturnStatement
          && functionName.startsWith("return")
          && pCFAEdge.getPredecessor().getFunction().getQualifiedName().equals("main")) {
        return ImmutableList.of();
      }
      if (astNode instanceof CFunctionCall) {
        expression = ((CFunctionCall) astNode).getFunctionCallExpression();
      } else {
        return null;
      }
      if (expression.getFunctionNameExpression().toString().equals(functionName)) {
        String condition = collectConditionFromPreviousEdge(pCFAEdge);
        if (pDecomposedMap.containsKey(pCFAEdge.getPredecessor())) {
          condition = condition + " && " + pDecomposedMap.get(pCFAEdge.getPredecessor());
        }

        List<String> parameters = new ArrayList<>(expression.getParameterExpressions()
            .stream().map(e -> e.toString()).toList());
        parameters.add(0, condition);
        return ImmutableList.copyOf(parameters);
      }
    }
    return null;
  }

  /**
   * In CPAchecker, we decompose conditions like x < 12 && y + 1 > 300 into two consecutive edges of
   * CFA. Because of that, we might instrument a no-overflow check before the condition and hence
   * report no-overflow in y + 1 > 300 even if x < 12 is not satisfied. This method collects all
   * such conditions and connects them into one conjunction.
   */
  private String collectConditionFromPreviousEdge(CFAEdge pCFAEdge) {
    StringBuilder condition = new StringBuilder("1");
    CFAEdge currentEdge = getPreviousAssumeEdge(pCFAEdge);
    if (currentEdge == null || pCFAEdge.getEdgeType() != CFAEdgeType.AssumeEdge) {
      return condition.toString();
    }
    while (currentEdge.getEdgeType() == CFAEdgeType.AssumeEdge) {
      condition
          .append(" && ")
          .append(currentEdge.getDescription(), 1, currentEdge.getDescription().length() - 1);
      currentEdge = getPreviousAssumeEdge(currentEdge);
      if (currentEdge == null) {
        return condition.toString();
      }
    }
    return condition.toString();
  }

  private String removeIndicesOfVariablesWithSameName(CExpression pOperand, CFAEdge pCFAEdge) {
    String rawStatement = pCFAEdge.getRawStatement().replaceAll("[()\\s]", "");
    String rawOperand = pOperand.toASTString().replaceAll("[()\\s]", "");
    String operandWithoutIndex = pOperand.toASTString();
    if (operandWithoutIndex.contains("__CPAchecker_TMP")
        || operandWithoutIndex.contains("static__")) {
      return "__CPAchecker_TMP";
    }
    if (!rawStatement.contains(rawOperand) && operandWithoutIndex.contains("__")) {
      return operandWithoutIndex.replaceFirst("__(\\d)(?!.*__)", "");
    }
    return operandWithoutIndex;
  }

  @Nullable
  private CFAEdge getPreviousAssumeEdge(CFAEdge pCFAEdge) {
    if (pCFAEdge.getPredecessor().getNumEnteringEdges() < 1) {
      return null;
    }
    return pCFAEdge.getPredecessor().getEnteringEdge(0);
  }

  private boolean isNegatedCond(CFAEdge pCFAEdge) {
    if (pCFAEdge instanceof CAssumeEdge) {
      return !((CAssumeEdge) pCFAEdge).getTruthAssumption();
    }
    return false;
  }

  private boolean isOriginalCond(CFAEdge pCFAEdge) {
    if (pCFAEdge.getPredecessor().getNumLeavingEdges() == 1) {
      return true;
    }
    if (pCFAEdge instanceof CAssumeEdge) {
      return ((CAssumeEdge) pCFAEdge).getTruthAssumption();
    }
    return false;
  }

  private enum patternType {
    TRUE,
    FALSE,
    COND,
    NOT_COND,
    ADD,
    SUB,
    NEG,
    MUL,
    DIV,
    MOD,
    SHIFT,
    EQ,
    GEQ,
    GR,
    LEQ,
    LS,
    NEQ,
    RSHIFT,
    OR,
    AND,
    XOR,
    FUNC,
    REGEX
  }
}
