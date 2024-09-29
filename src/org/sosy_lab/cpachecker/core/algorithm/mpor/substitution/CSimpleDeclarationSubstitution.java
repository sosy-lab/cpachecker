// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.substitution;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.ExpressionSubstitution.Substitution;

public class CSimpleDeclarationSubstitution implements Substitution {

  /**
   * The map of global variable declarations to their substitutes. {@code null} if this instance
   * serves as a dummy.
   */
  @Nullable public final ImmutableMap<CVariableDeclaration, CVariableDeclaration> globalVarSubs;

  /** The map of thread local variable declarations to their substitutes. */
  public final ImmutableMap<CVariableDeclaration, CVariableDeclaration> localVarSubs;

  /**
   * The map of parameter to variable declaration substitutes. {@code null} if this instance serves
   * as a dummy.
   */
  @Nullable public final ImmutableMap<CParameterDeclaration, CVariableDeclaration> paramSubs;

  private final CBinaryExpressionBuilder binExprBuilder;

  public CSimpleDeclarationSubstitution(
      @Nullable ImmutableMap<CVariableDeclaration, CVariableDeclaration> pGlobalVarSubs,
      ImmutableMap<CVariableDeclaration, CVariableDeclaration> pLocalVarSubs,
      @Nullable ImmutableMap<CParameterDeclaration, CVariableDeclaration> pParamSubs,
      CBinaryExpressionBuilder pBinExprBuilder) {

    globalVarSubs = pGlobalVarSubs;
    localVarSubs = pLocalVarSubs;
    paramSubs = pParamSubs;
    binExprBuilder = pBinExprBuilder;
  }

  // TODO take a look at ExpressionSubstitution.applySubstitution()

  @Override
  public CExpression substitute(CExpression pExpression) {

    FileLocation fl = pExpression.getFileLocation();
    CType exprType = pExpression.getExpressionType();

    if (pExpression instanceof CIdExpression idExpr) {
      if (shouldSubstitute(idExpr.getDeclaration())) {
        CVariableDeclaration sub = getVarSub(idExpr.getDeclaration());
        return new CIdExpression(fl, exprType, sub.getName(), sub);
      }

    } else if (pExpression instanceof CBinaryExpression binExpr) {
      // recursively substitute operands of binary expressions
      CExpression op1 = substitute(binExpr.getOperand1());
      CExpression op2 = substitute(binExpr.getOperand2());
      // only create a new expression if any operand was substituted (compare references)
      if (op1 != binExpr.getOperand1() || op2 != binExpr.getOperand2()) {
        try {
          return binExprBuilder.buildBinaryExpression(op1, op2, binExpr.getOperator());
        } catch (UnrecognizedCodeException e) {
          // "convert" exception -> no UnrecognizedCodeException in signature
          throw new RuntimeException(e);
        }
      }

    } else if (pExpression instanceof CArraySubscriptExpression arrSubscriptExpr) {
      CExpression arrExpr = arrSubscriptExpr.getArrayExpression();
      CExpression subscriptExpr = arrSubscriptExpr.getSubscriptExpression();
      CExpression arrSub = substitute(arrExpr);
      CExpression subscriptSub = substitute(subscriptExpr);
      // only create a new expression if any expr was substituted (compare references)
      if (arrSub != arrExpr || subscriptSub != subscriptExpr) {
        return new CArraySubscriptExpression(fl, exprType, arrSub, subscriptSub);
      }

    } else if (pExpression instanceof CFieldReference fieldRef) {
      CExpression ownerSub = substitute(fieldRef.getFieldOwner());
      // only create a new expression if any expr was substituted (compare references)
      if (ownerSub != fieldRef.getFieldOwner()) {
        return new CFieldReference(
            fl,
            fieldRef.getExpressionType(),
            fieldRef.getFieldName(),
            ownerSub,
            fieldRef.isPointerDereference());
      }

    } else if (pExpression instanceof CUnaryExpression unaryExpr) {
      return new CUnaryExpression(
          unaryExpr.getFileLocation(),
          unaryExpr.getExpressionType(),
          substitute(unaryExpr.getOperand()),
          unaryExpr.getOperator());
    }

    return pExpression;
  }

  public CStatement substitute(CStatement pCStmt) {

    FileLocation fl = pCStmt.getFileLocation();

    if (pCStmt instanceof CFunctionCallAssignmentStatement funcCallAssignStmt) {
      if (funcCallAssignStmt.getLeftHandSide() instanceof CIdExpression cIdExpr) {
        CExpression lhsSub = substitute(cIdExpr);
        if (lhsSub instanceof CIdExpression cIdExprSub) {
          CFunctionCallExpression rhs = funcCallAssignStmt.getRightHandSide();
          return new CFunctionCallAssignmentStatement(fl, cIdExprSub, substitute(rhs));
        }
      }

    } else if (pCStmt instanceof CFunctionCallStatement funcCallStmt) {
      return new CFunctionCallStatement(
          funcCallStmt.getFileLocation(), substitute(funcCallStmt.getFunctionCallExpression()));

    } else if (pCStmt instanceof CExpressionAssignmentStatement exprAssignStmt) {
      CLeftHandSide lhs = exprAssignStmt.getLeftHandSide();
      CExpression rhs = exprAssignStmt.getRightHandSide();
      CExpression sub = substitute(lhs);
      if (sub instanceof CLeftHandSide lhsSub) {
        return new CExpressionAssignmentStatement(fl, lhsSub, substitute(rhs));
      }

    } else if (pCStmt instanceof CExpressionStatement cExprStmt) {
      return new CExpressionStatement(fl, substitute(cExprStmt.getExpression()));
    }

    return pCStmt;
  }

  public CFunctionCallExpression substitute(CFunctionCallExpression pFuncCallExpr) {
    // substitute all params in the function call expression
    List<CExpression> params = new ArrayList<>();
    for (CExpression expr : pFuncCallExpr.getParameterExpressions()) {
      params.add(substitute(expr));
    }
    return SubstituteBuilder.substituteFunctionCallExpr(pFuncCallExpr, params);
  }

  public ImmutableMap<ThreadEdge, CFAEdge> substituteEdges(MPORThread pThread) {
    Map<ThreadEdge, CFAEdge> rSubstitutes = new HashMap<>();
    for (ThreadEdge threadEdge : pThread.cfa.threadEdges) {
      // prevent duplicate keys by excluding parallel edges
      if (!rSubstitutes.containsKey(threadEdge)) {
        CFAEdge edge = threadEdge.cfaEdge;
        CFAEdge substitute = null;

        if (edge instanceof CDeclarationEdge decl) {
          // TODO what about structs?
          CDeclaration dec = decl.getDeclaration();
          if (dec instanceof CVariableDeclaration) {
            CVariableDeclaration varSub = getVarSub(dec);
            if (varSub != null) {
              substitute = SubstituteBuilder.substituteDeclarationEdge(decl, varSub);
            }
          }

        } else if (edge instanceof CAssumeEdge assume) {
          substitute =
              SubstituteBuilder.substituteAssumeEdge(assume, substitute(assume.getExpression()));

        } else if (edge instanceof CStatementEdge stmt) {
          substitute =
              SubstituteBuilder.substituteStatementEdge(stmt, substitute(stmt.getStatement()));

        } else if (edge instanceof CFunctionSummaryEdge funcSumm) {
          // only substitute assignments (e.g. CPAchecker_TMP = func();)
          if (funcSumm.getExpression() instanceof CFunctionCallAssignmentStatement assignStmt) {
            substitute =
                SubstituteBuilder.substituteFunctionSummaryEdge(funcSumm, substitute(assignStmt));
          }

        } else if (edge instanceof CFunctionCallEdge funcCall) {
          // CFunctionCallEdges also assign CPAchecker_TMPs -> handle assignment statements here too
          substitute =
              SubstituteBuilder.substituteFunctionCallEdge(
                  funcCall, (CFunctionCall) substitute(funcCall.getFunctionCall()));
        }

        rSubstitutes.put(threadEdge, substitute == null ? edge : substitute);
      }
    }
    return ImmutableMap.copyOf(rSubstitutes);
  }

  /** Returns the global, local or param {@link CVariableDeclaration} substitute of pDec. */
  private CVariableDeclaration getVarSub(CSimpleDeclaration pSimpleDec) {
    if (pSimpleDec instanceof CVariableDeclaration varDec) {
      if (localVarSubs.containsKey(varDec)) {
        return localVarSubs.get(varDec);
      } else {
        assert globalVarSubs != null;
        if (globalVarSubs.containsKey(varDec)) {
          return globalVarSubs.get(varDec);
        }
      }
    } else if (pSimpleDec instanceof CParameterDeclaration paramDec) {
      assert paramSubs != null;
      if (paramSubs.containsKey(paramDec)) {
        return paramSubs.get(paramDec);
      }
    }
    throw new IllegalArgumentException("pSimpleDec must be CVariable- or CParameterDeclaration");
  }

  private boolean shouldSubstitute(CSimpleDeclaration pSimpleDec) {
    return pSimpleDec instanceof CVariableDeclaration
        || pSimpleDec instanceof CParameterDeclaration;
  }
}
