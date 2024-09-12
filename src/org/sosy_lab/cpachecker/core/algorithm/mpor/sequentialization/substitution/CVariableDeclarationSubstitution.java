// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.substitution;

import com.google.common.collect.ImmutableMap;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.ExpressionSubstitution.Substitution;

public class CVariableDeclarationSubstitution implements Substitution {

  public final ImmutableMap<CVariableDeclaration, CVariableDeclaration> substitutes;

  private final CBinaryExpressionBuilder binExprBuilder;

  public CVariableDeclarationSubstitution(
      ImmutableMap<CVariableDeclaration, CVariableDeclaration> pSubstitutes,
      CBinaryExpressionBuilder pCBinExprBuilder) {
    substitutes = pSubstitutes;
    binExprBuilder = pCBinExprBuilder;
  }

  @Override
  public CExpression substitute(CExpression pExpression) {

    FileLocation fl = pExpression.getFileLocation();
    CType exprType = pExpression.getExpressionType();

    if (pExpression instanceof CIdExpression cIdExpr) {
      if (cIdExpr.getDeclaration() instanceof CVariableDeclaration cVarDec) {
        CVariableDeclaration substitute = substitutes.get(cVarDec);
        if (substitute != null) {
          return new CIdExpression(fl, exprType, substitute.getName(), substitute);
        }
      }

    } else if (pExpression instanceof CBinaryExpression cBinExpr) {
      // recursively substitute operands of binary expressions
      CExpression op1 = substitute(cBinExpr.getOperand1());
      CExpression op2 = substitute(cBinExpr.getOperand2());
      // only create a new expression if any operand was substituted
      if (!op1.equals(cBinExpr.getOperand1()) || !op2.equals(cBinExpr.getOperand2())) {
        try {
          return binExprBuilder.buildBinaryExpression(op1, op2, cBinExpr.getOperator());
        } catch (UnrecognizedCodeException e) {
          // "convert" exception -> no UnrecognizedCodeException in signature
          throw new RuntimeException(e);
        }
      }

    } else if (pExpression instanceof CArraySubscriptExpression cArrSubExpr) {
      CExpression arrSub = substitute(cArrSubExpr.getArrayExpression());
      CExpression subscriptSub = substitute(cArrSubExpr.getSubscriptExpression());
      // only create a new expression if any expr was substituted
      if (!arrSub.equals(cArrSubExpr.getArrayExpression())
          || !subscriptSub.equals(cArrSubExpr.getSubscriptExpression())) {
        return new CArraySubscriptExpression(fl, exprType, arrSub, subscriptSub);
      }
    }

    return pExpression;
  }

  public CStatement substitute(CStatement pCStmt) {

    FileLocation fl = pCStmt.getFileLocation();

    if (pCStmt instanceof CFunctionCallAssignmentStatement cFuncCallAssignStmt) {
      if (cFuncCallAssignStmt.getLeftHandSide() instanceof CIdExpression cIdExpr) {
        CExpression substitute = substitute(cIdExpr);
        if (substitute instanceof CIdExpression cIdExprSub) {
          CFunctionCallExpression rhs = cFuncCallAssignStmt.getRightHandSide();
          // TODO test if CFuncCallExpr has to be substituted (so far: no)
          return new CFunctionCallAssignmentStatement(fl, cIdExprSub, rhs);
        }
      }

    } else if (pCStmt instanceof CExpressionAssignmentStatement cExprAssignStmt) {
      CLeftHandSide lhs = cExprAssignStmt.getLeftHandSide();
      CExpression rhs = cExprAssignStmt.getRightHandSide();

      if (lhs instanceof CIdExpression) {
        CExpression substitute = substitute(lhs);
        if (substitute instanceof CIdExpression cIdExprSub) {
          return new CExpressionAssignmentStatement(fl, cIdExprSub, substitute(rhs));
        }

      } else if (lhs instanceof CArraySubscriptExpression) {
        CExpression lhsSub = substitute(lhs);
        if (lhsSub instanceof CLeftHandSide newLhs) {
          return new CExpressionAssignmentStatement(fl, newLhs, substitute(rhs));
        }
      }

    } else if (pCStmt instanceof CExpressionStatement cExprStmt) {
      return new CExpressionStatement(fl, substitute(cExprStmt.getExpression()));
    }

    return pCStmt;
  }
}
