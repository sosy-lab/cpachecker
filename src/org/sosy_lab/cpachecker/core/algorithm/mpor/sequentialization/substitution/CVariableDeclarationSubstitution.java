// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.substitution;

import com.google.common.collect.ImmutableMap;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
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

    if (pExpression instanceof CIdExpression cIdExpr) {
      if (cIdExpr.getDeclaration() instanceof CVariableDeclaration cVarDec) {
        CVariableDeclaration substitute = substitutes.get(cVarDec);
        if (substitute != null) {
          return new CIdExpression(
              cIdExpr.getFileLocation(),
              cIdExpr.getExpressionType(),
              substitute.getName(),
              substitute);
        }
      }

      // recursively substitute operands of binary expressions
    } else if (pExpression instanceof CBinaryExpression cBinExpr) {
      CExpression op1 = substituteOperand(cBinExpr.getOperand1());
      CExpression op2 = substituteOperand(cBinExpr.getOperand2());
      // only create a new expression if any operand was substituted
      if (!op1.equals(cBinExpr.getOperand1()) || !op2.equals(cBinExpr.getOperand2())) {
        try {
          return binExprBuilder.buildBinaryExpression(op1, op2, cBinExpr.getOperator());
        } catch (UnrecognizedCodeException e) {
          // "convert" exception -> no UnrecognizedCodeException in signature
          throw new RuntimeException(e);
        }
      }
    }

    return pExpression;
  }

  private CExpression substituteOperand(CExpression pOperand) {
    if (pOperand instanceof CIdExpression cIdExpr) {
      return substitute(cIdExpr);
    } else if (pOperand instanceof CBinaryExpression cBinExpr) {
      return substitute(cBinExpr);
    }
    return pOperand;
  }

  public CStatement substitute(CStatement pCStmt) {

    if (pCStmt instanceof CFunctionCallAssignmentStatement cFuncCallAssignStmt) {
      if (cFuncCallAssignStmt.getLeftHandSide() instanceof CIdExpression cIdExpr) {
        CExpression substitute = substitute(cIdExpr);
        if (substitute instanceof CIdExpression cIdExprSub) {
          return new CFunctionCallAssignmentStatement(
              pCStmt.getFileLocation(),
              cIdExprSub,
              // TODO test if CFuncCallExpr has to be substituted (so far: no)
              cFuncCallAssignStmt.getRightHandSide());
        }
      }

    } else if (pCStmt instanceof CExpressionAssignmentStatement cExprAssignStmt) {
      if (cExprAssignStmt.getLeftHandSide() instanceof CIdExpression cIdExpr) {
        CExpression substitute = substitute(cIdExpr);
        if (substitute instanceof CIdExpression cIdExprSub) {
          return new CExpressionAssignmentStatement(
              pCStmt.getFileLocation(), cIdExprSub, substitute(cExprAssignStmt.getRightHandSide()));
        }
      }

    } else if (pCStmt instanceof CExpressionStatement cExprStmt) {
      return new CExpressionStatement(
          cExprStmt.getFileLocation(), substitute(cExprStmt.getExpression()));
    }

    return pCStmt;
  }
}
