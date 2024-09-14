// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.substitution;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.ExpressionSubstitution.Substitution;

public class CSimpleDeclarationSubstitution implements Substitution {

  /**
   * The map of global and thread local variable declarations to their substitutes. Only the main
   * thread contains global variable declarations.
   */
  public final ImmutableMap<CVariableDeclaration, CVariableDeclaration> varSubs;

  /** The map of parameter to variable declaration substitutes. */
  public final ImmutableMap<CParameterDeclaration, CVariableDeclaration> paramSubs;

  private final CBinaryExpressionBuilder binExprBuilder;

  public CSimpleDeclarationSubstitution(
      ImmutableMap<CVariableDeclaration, CVariableDeclaration> pVarSubs,
      ImmutableMap<CParameterDeclaration, CVariableDeclaration> pParamSubs,
      CBinaryExpressionBuilder pBinExprBuilder) {
    varSubs = pVarSubs;
    paramSubs = pParamSubs;
    binExprBuilder = pBinExprBuilder;
  }

  // TODO take a look at ExpressionSubstitution.applySubstitution()

  @Override
  public CExpression substitute(CExpression pExpression) {

    FileLocation fl = pExpression.getFileLocation();
    CType exprType = pExpression.getExpressionType();

    if (pExpression instanceof CIdExpression idExpr) {
      CSimpleDeclaration dec = idExpr.getDeclaration();
      assert dec != null; // TODO test purposes
      if (dec instanceof CVariableDeclaration varDec) {
        CVariableDeclaration varSub = varSubs.get(varDec);
        if (varSub != null) {
          return new CIdExpression(fl, exprType, varSub.getName(), varSub);
        }
      } else if (dec instanceof CParameterDeclaration paramDec) {
        CVariableDeclaration paramSub = paramSubs.get(paramDec);
        if (paramSub != null) {
          return new CIdExpression(fl, exprType, paramSub.getName(), paramSub);
        }
      }

    } else if (pExpression instanceof CBinaryExpression binExpr) {
      // recursively substitute operands of binary expressions
      CExpression op1 = substitute(binExpr.getOperand1());
      CExpression op2 = substitute(binExpr.getOperand2());
      // only create a new expression if any operand was substituted
      if (!op1.equals(binExpr.getOperand1()) || !op2.equals(binExpr.getOperand2())) {
        try {
          return binExprBuilder.buildBinaryExpression(op1, op2, binExpr.getOperator());
        } catch (UnrecognizedCodeException e) {
          // "convert" exception -> no UnrecognizedCodeException in signature
          throw new RuntimeException(e);
        }
      }

    } else if (pExpression instanceof CArraySubscriptExpression arrSubExpr) {
      CExpression arrSub = substitute(arrSubExpr.getArrayExpression());
      CExpression subscriptSub = substitute(arrSubExpr.getSubscriptExpression());
      // only create a new expression if any expr was substituted
      if (!arrSub.equals(arrSubExpr.getArrayExpression())
          || !subscriptSub.equals(arrSubExpr.getSubscriptExpression())) {
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

  public ImmutableMap<ThreadEdge, CFAEdge> substituteEdges(MPORThread pThread) {
    Map<ThreadEdge, CFAEdge> rSubstitutes = new HashMap<>();
    for (ThreadEdge threadEdge : pThread.cfa.threadEdges) {
      // prevent duplicate keys by excluding parallel edges
      if (!rSubstitutes.containsKey(threadEdge)) {
        CFAEdge edge = threadEdge.cfaEdge;
        CFAEdge substitute = null;

        if (edge instanceof CDeclarationEdge decEdge) {
          // TODO what about structs?
          CDeclaration dec = decEdge.getDeclaration();
          if (dec instanceof CVariableDeclaration varDec) {
            if (varSubs.containsKey(varDec)) {
              substitute =
                  SubstituteBuilder.substituteDeclarationEdge(decEdge, varSubs.get(varDec));
            }
          }

        } else if (edge instanceof CAssumeEdge assumeEdge) {
          substitute =
              SubstituteBuilder.substituteAssumeEdge(
                  assumeEdge, substitute(assumeEdge.getExpression()));

        } else if (edge instanceof CStatementEdge stmtEdge) {
          substitute =
              SubstituteBuilder.substituteStatementEdge(
                  stmtEdge, substitute(stmtEdge.getStatement()));
        }
        rSubstitutes.put(threadEdge, substitute == null ? edge : substitute);
      }
    }
    return ImmutableMap.copyOf(rSubstitutes);
  }
}
