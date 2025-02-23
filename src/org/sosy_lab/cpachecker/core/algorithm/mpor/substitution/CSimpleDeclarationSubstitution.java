// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.substitution;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.ExpressionSubstitution.Substitution;

// TODO try around with Visitors, this should make all of this redundant
public class CSimpleDeclarationSubstitution implements Substitution {

  /**
   * The map of global variable declarations to their substitutes. {@link Optional#empty()} if this
   * instance serves as a dummy.
   */
  public final Optional<ImmutableMap<CVariableDeclaration, CIdExpression>> globalVarSubstitutes;

  /** The map of thread local variable declarations to their substitutes. */
  public final ImmutableMap<CVariableDeclaration, CIdExpression> localVarSubstitutes;

  /**
   * The map of parameter to variable declaration substitutes. {@link Optional#empty()} if this
   * instance serves as a dummy.
   */
  public final Optional<ImmutableMap<CParameterDeclaration, CIdExpression>> parameterSubstitutes;

  private final CBinaryExpressionBuilder binaryExpressionBuilder;

  public CSimpleDeclarationSubstitution(
      Optional<ImmutableMap<CVariableDeclaration, CIdExpression>> pGlobalVarSubstitutes,
      ImmutableMap<CVariableDeclaration, CIdExpression> pLocalVarSubstitutes,
      Optional<ImmutableMap<CParameterDeclaration, CIdExpression>> pParameterSubstitutes,
      CBinaryExpressionBuilder pBinaryExpressionBuilder) {

    globalVarSubstitutes = pGlobalVarSubstitutes;
    localVarSubstitutes = pLocalVarSubstitutes;
    parameterSubstitutes = pParameterSubstitutes;
    binaryExpressionBuilder = pBinaryExpressionBuilder;
  }

  // TODO take a look at ExpressionSubstitution.applySubstitution()

  @Override
  public CExpression substitute(CExpression pExpression) {
    FileLocation fl = pExpression.getFileLocation();
    CType exprType = pExpression.getExpressionType();

    if (pExpression instanceof CIdExpression idExpr) {
      if (isSubstitutable(idExpr.getDeclaration())) {
        return getVarSubstitute(idExpr.getDeclaration());
      }

    } else if (pExpression instanceof CBinaryExpression binExpr) {
      // recursively substitute operands of binary expressions
      CExpression op1 = substitute(binExpr.getOperand1());
      CExpression op2 = substitute(binExpr.getOperand2());
      // only create a new expression if any operand was substituted (compare references)
      if (op1 != binExpr.getOperand1() || op2 != binExpr.getOperand2()) {
        try {
          return binaryExpressionBuilder.buildBinaryExpression(op1, op2, binExpr.getOperator());
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

    } else if (pExpression instanceof CPointerExpression pointerExpr) {
      return new CPointerExpression(
          pointerExpr.getFileLocation(),
          pointerExpr.getExpressionType(),
          substitute(pointerExpr.getOperand()));
    }

    return pExpression;
  }

  public CStatement substitute(CStatement pStmt) {

    FileLocation fl = pStmt.getFileLocation();

    if (pStmt instanceof CFunctionCallAssignmentStatement funcCallAssignStmt) {
      if (funcCallAssignStmt.getLeftHandSide() instanceof CIdExpression cIdExpr) {
        CExpression lhsSub = substitute(cIdExpr);
        if (lhsSub instanceof CIdExpression cIdExprSub) {
          CFunctionCallExpression rhs = funcCallAssignStmt.getRightHandSide();
          return new CFunctionCallAssignmentStatement(fl, cIdExprSub, substitute(rhs));
        }
      }

    } else if (pStmt instanceof CFunctionCallStatement funcCallStmt) {
      return new CFunctionCallStatement(
          funcCallStmt.getFileLocation(), substitute(funcCallStmt.getFunctionCallExpression()));

    } else if (pStmt instanceof CExpressionAssignmentStatement exprAssignStmt) {
      CLeftHandSide lhs = exprAssignStmt.getLeftHandSide();
      CExpression rhs = exprAssignStmt.getRightHandSide();
      CExpression sub = substitute(lhs);
      if (sub instanceof CLeftHandSide lhsSub) {
        return new CExpressionAssignmentStatement(fl, lhsSub, substitute(rhs));
      }

    } else if (pStmt instanceof CExpressionStatement cExprStmt) {
      return new CExpressionStatement(fl, substitute(cExprStmt.getExpression()));
    }

    return pStmt;
  }

  public CFunctionCallExpression substitute(CFunctionCallExpression pFuncCallExpr) {
    // substitute all params in the function call expression
    List<CExpression> params = new ArrayList<>();
    for (CExpression expr : pFuncCallExpr.getParameterExpressions()) {
      params.add(substitute(expr));
    }
    return new CFunctionCallExpression(
        pFuncCallExpr.getFileLocation(),
        pFuncCallExpr.getExpressionType(),
        pFuncCallExpr.getFunctionNameExpression(),
        params,
        pFuncCallExpr.getDeclaration());
  }

  public CReturnStatement substitute(CReturnStatement pRetStmt) {
    if (pRetStmt.getReturnValue().isEmpty()) {
      // return as-is if there is no expression to substitute
      return pRetStmt;
    } else {
      CExpression expr = pRetStmt.getReturnValue().orElseThrow();
      // TODO it would be cleaner to also substitute the assignment...
      return new CReturnStatement(
          pRetStmt.getFileLocation(), Optional.of(substitute(expr)), pRetStmt.asAssignment());
    }
  }

  /** Returns the global, local or param {@link CIdExpression} substitute of pDec. */
  private CIdExpression getVarSubstitute(CSimpleDeclaration pSimpleDec) {
    if (pSimpleDec instanceof CVariableDeclaration varDeclaration) {
      if (localVarSubstitutes.containsKey(varDeclaration)) {
        return localVarSubstitutes.get(varDeclaration);
      } else {
        assert globalVarSubstitutes.isPresent();
        checkArgument(
            globalVarSubstitutes.orElseThrow().containsKey(varDeclaration),
            "no substitute found for " + "%s",
            pSimpleDec.toASTString());
        return globalVarSubstitutes.orElseThrow().get(varDeclaration);
      }
    } else if (pSimpleDec instanceof CParameterDeclaration paramDec) {
      assert parameterSubstitutes.isPresent();
      checkArgument(
          parameterSubstitutes.orElseThrow().containsKey(paramDec),
          "no substitute found for " + "%s",
          pSimpleDec.toASTString());
      return parameterSubstitutes.orElseThrow().get(paramDec);
    }
    throw new IllegalArgumentException("pSimpleDec must be CVariable- or CParameterDeclaration");
  }

  public CVariableDeclaration getVarDeclarationSubstitute(CSimpleDeclaration pSimpleDeclaration) {
    CIdExpression idExpr = getVarSubstitute(pSimpleDeclaration);
    return (CVariableDeclaration) idExpr.getDeclaration();
  }

  public CVariableDeclaration castToVarDeclaration(CSimpleDeclaration pSimpleDeclaration) {
    checkArgument(
        pSimpleDeclaration instanceof CVariableDeclaration,
        "pSimpleDeclaration must be CVariableDeclaration");
    return (CVariableDeclaration) pSimpleDeclaration;
  }

  private boolean isSubstitutable(CSimpleDeclaration pSimpleDec) {
    return pSimpleDec instanceof CVariableDeclaration
        || pSimpleDec instanceof CParameterDeclaration;
  }
}
