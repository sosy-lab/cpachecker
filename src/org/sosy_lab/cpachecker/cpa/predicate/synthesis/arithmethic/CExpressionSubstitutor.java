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
package org.sosy_lab.cpachecker.cpa.predicate.synthesis.arithmethic;

import java.util.Map;

import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;


public class CExpressionSubstitutor extends DefaultCExpressionVisitor<CExpression, RuntimeException> {

  private String substVariableNamePrefix;
  private Map<CIdExpression, CExpression> resultMapping;

  private long uniqueVarSequence = 0;

  private synchronized String getUniqueVariableName() {
    uniqueVarSequence++;
    return substVariableNamePrefix + "#" + uniqueVarSequence;
  }

  public CExpressionSubstitutor(String pSubstVariableNamePrefix) {
    Preconditions.checkNotNull(pSubstVariableNamePrefix);
    this.substVariableNamePrefix = pSubstVariableNamePrefix;
    this.resultMapping = Maps.newHashMap();
  }

  public Map<CIdExpression, CExpression> getSubstitutions() {
    return resultMapping;
  }

  // TODO: Add a cache

  @Override
  protected CExpression visitDefault(CExpression exp) {
    return exp;
  }

  private CIdExpression substituteById(CExpression pExpr) {
    Preconditions.checkArgument(!(pExpr instanceof CIdExpression));

    String idName = getUniqueVariableName();

    // 1. create a IdExpression with the type of pExpr
    CVariableDeclaration idDecl = new CVariableDeclaration(
        pExpr.getFileLocation(),
        false,
        CStorageClass.AUTO,
        pExpr.getExpressionType(),
        idName,
        idName,
        idName,
        null);
    CIdExpression id = new CIdExpression(pExpr.getFileLocation(), idDecl);

    // 2. store the mapping between id and pExpr
    resultMapping.put(id, pExpr);

    // 3. return the id
    return id;
  }

  @Override
  public CExpression visit(CBinaryExpression pE) {
    CExpression lhs = pE.getOperand1().accept(this);
    CExpression rhs = pE.getOperand2().accept(this);

    if (lhs instanceof CBinaryExpression || lhs instanceof CUnaryExpression) {
      lhs = substituteById(lhs);
    }

    if (rhs instanceof CBinaryExpression || rhs instanceof CUnaryExpression) {
      rhs = substituteById(rhs);
    }

    if (pE.getOperand1() != lhs || pE.getOperand2() != rhs) {
      return new CBinaryExpression(
        pE.getFileLocation(),
        pE.getExpressionType(),
        pE.getCalculationType(),
        lhs,
        rhs,
        pE.getOperator());
    }

    return pE;
  }

  @Override
  public CExpression visit(CCastExpression exp) {
    CExpression substOp1 = exp.getOperand().accept(this);

    if (substOp1 != exp.getOperand()) {
      return new CCastExpression(
          exp.getFileLocation(),
          exp.getExpressionType(),
          substOp1);
    }

    return exp;
  }

  @Override
  public CExpression visit(CUnaryExpression exp) {
    CExpression substOp1 = exp.getOperand().accept(this);

    if (substOp1 != exp.getOperand()) {
      return new CUnaryExpression(
          exp.getFileLocation(),
          exp.getExpressionType(),
          substOp1,
          exp.getOperator());
    }

    return exp;
  }

  @Override
  public CExpression visit(CArraySubscriptExpression exp) {
    CExpression substOp1 = exp.getArrayExpression().accept(this);
    CExpression substOp2 = exp.getSubscriptExpression().accept(this);

    if (substOp1 != exp.getArrayExpression() || substOp2 != exp.getSubscriptExpression()) {
      return new CArraySubscriptExpression(
        exp.getFileLocation(),
        exp.getExpressionType(),
        substOp1,
        substOp2);
    }

    return exp;
  }

  @Override
  public CExpression visit(CFieldReference exp) {
    CExpression substOp1 = exp.getFieldOwner().accept(this);

    if (substOp1 != exp.getFieldOwner()) {
      return new CFieldReference(
          exp.getFileLocation(),
          exp.getExpressionType(),
          exp.getFieldName(),
          substOp1,
          exp.isPointerDereference());
    }

    return exp;
  }

  @Override
  public CExpression visit(CPointerExpression exp) {
    CExpression substOp1 = exp.getOperand().accept(this);

    if (substOp1 != exp.getOperand()) {
      return new CPointerExpression(
          exp.getFileLocation(),
          exp.getExpressionType(),
          substOp1);
    }

    return exp;
  }

  @Override
  public CExpression visit(CComplexCastExpression exp) {
    CExpression substOp1 = exp.getOperand().accept(this);

    if (substOp1 != exp.getOperand()) {
      return new CComplexCastExpression(
          exp.getFileLocation(),
          exp.getExpressionType(),
          substOp1,
          exp.getType(),
          exp.isRealCast());
    }

    return exp;
  }

}
