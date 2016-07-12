/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.assumptions.genericassumptions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class to generate assumptions related to over/underflow
 * of integer arithmetic operations
 */
public class ArithmeticOverflowAssumptionBuilder implements GenericAssumptionBuilder {
  private final Map<CType, CLiteralExpression> upperBounds;
  private final Map<CType, CLiteralExpression> lowerBounds;
  private final CBinaryExpressionBuilder cBinaryExpressionBuilder;

  public ArithmeticOverflowAssumptionBuilder(
      CFA cfa,
      LogManager logger) {
    CIntegerLiteralExpression INT_MIN = new CIntegerLiteralExpression(
        FileLocation.DUMMY,
        CNumericTypes.INT,
        cfa.getMachineModel().getMinimalIntegerValue(CNumericTypes.INT));
    CIntegerLiteralExpression INT_MAX = new CIntegerLiteralExpression(
        FileLocation.DUMMY,
        CNumericTypes.INT,
        cfa.getMachineModel().getMaximalIntegerValue(CNumericTypes.INT));

    // TODO: other types apart from integers.
    upperBounds = ImmutableMap.of(
        CNumericTypes.INT, INT_MAX,
        CNumericTypes.SIGNED_INT, INT_MAX
    );
    lowerBounds = ImmutableMap.of(
        CNumericTypes.INT, INT_MIN,
        CNumericTypes.SIGNED_INT, INT_MIN
    );
    cBinaryExpressionBuilder = new CBinaryExpressionBuilder(
        cfa.getMachineModel(),
        logger);
  }

  /**
   *
   * @param pEdge Input CFA edge.
   * @return Assumptions required for proving that none of the expressions
   * contained in {@code pEdge} result in overflows.
   */
  @Override
  public List<CExpression> assumptionsForEdge(CFAEdge pEdge)
      throws UnrecognizedCCodeException {
    Set<CExpression> result = new HashSet<>();

    switch (pEdge.getEdgeType()) {
      case AssumeEdge:
        CAssumeEdge assumeEdge = (CAssumeEdge) pEdge;
        visit(assumeEdge.getExpression(), result);
        break;
      case FunctionCallEdge:
        CFunctionCallEdge fcallEdge = (CFunctionCallEdge) pEdge;
        if (!fcallEdge.getArguments().isEmpty()) {
          CFunctionEntryNode fdefnode = fcallEdge.getSuccessor();
          List<CParameterDeclaration> formalParams = fdefnode.getFunctionParameters();
          for (CParameterDeclaration paramdecl : formalParams) {
            CExpression exp = new CIdExpression(paramdecl.getFileLocation(), paramdecl);
            visit(exp, result);
          }
        }
        break;
      case StatementEdge:
        CStatementEdge stmtEdge = (CStatementEdge) pEdge;

        CStatement stmt = stmtEdge.getStatement();
        if (stmt instanceof CAssignment) {
          visit(((CAssignment)stmt).getLeftHandSide(), result);
          CRightHandSide rightHandSide =
              ((CAssignment) stmt).getRightHandSide();
          if (rightHandSide instanceof CExpression) {
            CExpression rightExpr = (CExpression) rightHandSide;
            visit(rightExpr, result);
          }
        }
        break;
      case ReturnStatementEdge:
        CReturnStatementEdge returnEdge = (CReturnStatementEdge) pEdge;

        if (returnEdge.getExpression().isPresent()) {
          visit(returnEdge.getExpression().get(), result);
        }
        break;
      default:
        // TODO assumptions or other edge types, e.g. declarations?
        break;
    }
    return ImmutableList.copyOf(result);
  }

  /**
   * Compute and conjunct the assumption for the given arithmetic
   * expression, ignoring bounds if applicable. The method does
   * not check that the expression is indeed an arithmetic expression.
   */
  private void conjunctPredicateForArithmeticExpression(
      CExpression exp,
      Set<CExpression> result)
      throws UnrecognizedCCodeException {
    CType typ = exp.getExpressionType();
    if (lowerBounds.get(typ) != null) {
      result.add(cBinaryExpressionBuilder.buildBinaryExpression(
          exp,
          lowerBounds.get(typ),
          BinaryOperator.GREATER_EQUAL
      ));
    }

    if (upperBounds.get(typ) != null) {
      result.add(cBinaryExpressionBuilder.buildBinaryExpression(
          exp,
          upperBounds.get(typ),
          BinaryOperator.LESS_EQUAL
      ));
    }
  }

  /**
   * Recursively visit an expression and populate the list of assumptions
   * required for non-overflowing behavior.
   *
   * @param result Output list to write the generated assumptions to.
   */
  private void visit(CExpression pExpression, Set<CExpression> result)
      throws UnrecognizedCCodeException {

    if (pExpression instanceof CBinaryExpression) {
      CBinaryExpression binexp = (CBinaryExpression)pExpression;
      if (resultCanOverflow(binexp)) {
        conjunctPredicateForArithmeticExpression(pExpression, result);
      }
      visit(binexp.getOperand1(), result);
      visit(binexp.getOperand2(), result);
    } else if (pExpression instanceof CUnaryExpression) {

      // TODO: can unary operator cause an overflow?
      CUnaryExpression unexp = (CUnaryExpression) pExpression;
      visit(unexp.getOperand(), result);
    } else if (pExpression instanceof CCastExpression) {
      // TODO: can cast cause an overflow?

      CCastExpression castexp = (CCastExpression)pExpression;
      visit(castexp.getOperand(), result);
    }
  }

  /**
   * Whether the given operator can create new expression.
   */
  private boolean resultCanOverflow(CBinaryExpression expr) {
    switch (expr.getOperator()) {
      case MULTIPLY:
      case DIVIDE:
      case PLUS:
      case MINUS:
      case SHIFT_LEFT:
      case SHIFT_RIGHT:
        return true;
      case LESS_THAN:
      case GREATER_THAN:
      case LESS_EQUAL:
      case GREATER_EQUAL:
      case BINARY_AND:
      case BINARY_XOR:
      case BINARY_OR:
      case EQUALS:
      case NOT_EQUALS:
      default:
        return false;
    }
  }

}
