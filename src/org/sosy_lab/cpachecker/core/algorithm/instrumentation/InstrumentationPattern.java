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
      case ADD -> getTheOperandsFromAdd(pCFAEdge);
      case SUB -> getTheOperandsFromSub(pCFAEdge);
      default -> null;
    };
  }

  @Override
  public String toString() {
    return pattern;
  }

  @Nullable
  private ImmutableList<String> getTheOperandsFromAdd(CFAEdge pCFAEdge) {
    if (pCFAEdge.getRawAST().isPresent()) {
      AAstNode astNode = pCFAEdge.getRawAST().get();
      CExpression expression = extractExpression(astNode);
      if (expression instanceof CBinaryExpression
          && ((CBinaryExpression) expression).getOperator().equals(BinaryOperator.PLUS)) {
          return ImmutableList.of(((CBinaryExpression) expression).getOperand1().toASTString(),
              ((CBinaryExpression) expression).getOperand2().toASTString());
      }
    }
    return null;
  }

  @Nullable
  private ImmutableList<String> getTheOperandsFromSub(CFAEdge pCFAEdge) {
    if (pCFAEdge.getRawAST().isPresent()) {
      AAstNode astNode = pCFAEdge.getRawAST().get();
      CExpression expression = extractExpression(astNode);
      if (expression instanceof CBinaryExpression
          && ((CBinaryExpression) expression).getOperator().equals(BinaryOperator.MINUS)) {
        return ImmutableList.of(((CBinaryExpression) expression).getOperand1().toASTString(),
            ((CBinaryExpression) expression).getOperand2().toASTString());
      }
    }
    return null;
  }

  @Nullable
  private CExpression extractExpression(AAstNode pAAstNode) {
    if (pAAstNode instanceof CReturnStatement) {
      // return statement
      Optional<CExpression> optionalCExpression = ((CReturnStatement) pAAstNode).getReturnValue();
      if (optionalCExpression.isPresent()) {
        CExpression cExpression = optionalCExpression.get();
        if (cExpression instanceof CBinaryExpression) {
          return (CBinaryExpression) cExpression;
        } else if (cExpression instanceof CUnaryExpression){
          return (CUnaryExpression) cExpression;
        }
      }
    } else if (pAAstNode instanceof CAssignment) {
      // assignment
      ALeftHandSide leftHandSide = ((CAssignment) pAAstNode).getLeftHandSide();
      ARightHandSide rightHandSide = ((CAssignment) pAAstNode).getRightHandSide();
      if (rightHandSide instanceof CFunctionCallExpression) {
        // function call expression
        List<CExpression> parameterExpressions = ((CFunctionCallExpression) rightHandSide).getParameterExpressions();
        for (CExpression expression : parameterExpressions) {
          if (expression instanceof CBinaryExpression) {
            return (CBinaryExpression) expression;
          }
          if (expression instanceof CUnaryExpression) {
            return (CUnaryExpression) expression;
          }
        }
      } else if (rightHandSide instanceof CBinaryExpression) {
        return (CBinaryExpression) rightHandSide;
      } else if (rightHandSide instanceof CUnaryExpression){
        return (CUnaryExpression) rightHandSide;
      }
    } else if (pAAstNode instanceof CBinaryExpression) {
      // binary expression
      return (CBinaryExpression) pAAstNode;
    } else if (pAAstNode instanceof CFunctionCall) {
      // function call
      CFunctionCallExpression cFunctionCallExpression = ((CFunctionCall) pAAstNode).getFunctionCallExpression();
      List<CExpression> parameterExpressions = cFunctionCallExpression.getParameterExpressions();
      for (CExpression expression : parameterExpressions) {
        if (expression instanceof CBinaryExpression) {
          return (CBinaryExpression) expression;
        }
        if (expression instanceof CUnaryExpression) {
          return (CUnaryExpression) expression;
        }
      }
    } else if (pAAstNode instanceof CVariableDeclaration) {
      // variable declaration
      CInitializer cInitializer = ((CVariableDeclaration) pAAstNode).getInitializer();
      if (cInitializer instanceof CInitializerExpression) {
        CExpression cExpression = ((CInitializerExpression) cInitializer).getExpression();
        if (cExpression instanceof CBinaryExpression) {
          return (CBinaryExpression) cExpression;
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
    REGEX
  }
}
