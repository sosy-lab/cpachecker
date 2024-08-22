// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.instrumentation;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.ALeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.ARightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;

/**
 * Class for patterns defined on the transitions of instrumentation automaton. Should not be used
 * outside the Sequentialization operator !
 * Currently supported patterns:
 * - Arbitrary String = The match is true if the string on CFAEdge equals the pattern string.
 * - [cond], [!cond] = The match is true if the CFAEdge is the edge corresponding to satisfaction/
 * not satisfaction of the assumption.
 * - ADD, SUB = The match is true if the type of AST expression on CFAEdge is of type AdditionExpression
 * or SubstractionExpression
 * - true, false = The match is always true/false
 */
public class InstrumentationPattern {
  private patternType type;
  private String pattern;

  public InstrumentationPattern(String pPattern) {
    pattern = pPattern;
    switch (pPattern) {
      case "true" :
        type = patternType.TRUE;
        break;
      case "false" :
        type = patternType.FALSE;
        break;
      case "[cond]" :
        type = patternType.COND;
        break;
      case "[!cond]" :
        type = patternType.NOT_COND;
        break;
      case "ADD" :
        type = patternType.ADD;
        break;
      case "SUB" :
        type = patternType.SUB;
        break;
      case "MUL" :
        type = patternType.MUL;
        break;
      case "DIV" :
        type = patternType.DIV;
        break;
      case "MOD" :
        type = patternType.MOD;
        break;
      case "SHIFT" :
        type = patternType.SHIFT;
        break;
      case "EQ" :
        type = patternType.EQ;
        break;
      case "GEQ" :
        type = patternType.GEQ;
        break;
      case "GR" :
        type = patternType.GR;
        break;
      case "LEQ" :
        type = patternType.LEQ;
        break;
      case "LS" :
        type = patternType.LS;
        break;
      case "NEQ" :
        type = patternType.NEQ;
        break;
      case "RSHIFT" :
        type = patternType.RSHIFT;
        break;
      case "OR" :
        type = patternType.OR;
        break;
      case "AND" :
        type = patternType.AND;
        break;
      case "XOR" :
        type = patternType.XOR;
        break;
      default:
        type = patternType.REGEX;
        break;
    }
  }

  /**
   * Checks if the provided CFAEdge matches the pattern.
   * @return Null, if the edge does not match the pattern, otherwise returns the list of matched variables.
   */
  @Nullable
  public ImmutableList<String> MatchThePattern(CFAEdge pCFAEdge) {
    return switch (type) {
      case TRUE -> ImmutableList.of();
      case COND -> isOriginalCond(pCFAEdge) ? ImmutableList.of() : null;
      case NOT_COND -> isNegatedCond(pCFAEdge) ? ImmutableList.of() : null;
      case ADD -> getTheOperandsFromOperation(pCFAEdge, BinaryOperator.PLUS);
      case SUB -> getTheOperandsFromOperation(pCFAEdge, BinaryOperator.MINUS);
      case MUL -> getTheOperandsFromOperation(pCFAEdge, BinaryOperator.MULTIPLY);
      case DIV -> getTheOperandsFromOperation(pCFAEdge, BinaryOperator.DIVIDE);
      case MOD -> getTheOperandsFromOperation(pCFAEdge, BinaryOperator.MODULO);
      case SHIFT -> getTheOperandsFromOperation(pCFAEdge, BinaryOperator.SHIFT_LEFT);
      case EQ -> getTheOperandsFromOperation(pCFAEdge, BinaryOperator.EQUALS);
      case GEQ -> getTheOperandsFromOperation(pCFAEdge, BinaryOperator.GREATER_EQUAL);
      case GR -> getTheOperandsFromOperation(pCFAEdge, BinaryOperator.GREATER_THAN);
      case LEQ -> getTheOperandsFromOperation(pCFAEdge, BinaryOperator.LESS_EQUAL);
      case LS -> getTheOperandsFromOperation(pCFAEdge, BinaryOperator.LESS_THAN);
      case NEQ -> getTheOperandsFromOperation(pCFAEdge, BinaryOperator.NOT_EQUALS);
      case RSHIFT -> getTheOperandsFromOperation(pCFAEdge, BinaryOperator.SHIFT_RIGHT);
      case OR -> getTheOperandsFromOperation(pCFAEdge, BinaryOperator.BINARY_OR);
      case AND -> getTheOperandsFromOperation(pCFAEdge, BinaryOperator.BINARY_AND);
      case XOR -> getTheOperandsFromOperation(pCFAEdge, BinaryOperator.BINARY_XOR);
      default -> null;
    };
  }

  @Override
  public String toString() {
    return pattern;
  }

  @Nullable
  private ImmutableList<String> getTheOperandsFromOperation(CFAEdge pCFAEdge, BinaryOperator pOperator) {
    if (pCFAEdge.getRawAST().isPresent()) {
      AAstNode astNode = pCFAEdge.getRawAST().get();
      CExpression expression = LoopInfoUtils.extractExpression(astNode);
      if (expression instanceof CBinaryExpression
          && ((CBinaryExpression) expression).getOperator().equals(pOperator)) {
          CExpression operand1 = ((CBinaryExpression) expression).getOperand1();
          CExpression operand2 = ((CBinaryExpression) expression).getOperand2();
          if ((operand1.getExpressionType().getCanonicalType().toString()).equals("signed int")
              || (operand2.getExpressionType().getCanonicalType().toString()).equals("signed int")) {
            return ImmutableList.of(((CBinaryExpression) expression).getOperand1().toASTString(),
                ((CBinaryExpression) expression).getOperand2().toASTString());
          }
      }
    }
    return null;
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
    REGEX
  }
}
