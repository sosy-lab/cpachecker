/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.fsmbdd;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cpa.fsmbdd.exceptions.UnrecognizedSyntaxException;


public class ExpressionCache {

  private final Set<CExpression> cachedExpressions;
  private final Map<CExpression, String> atomKeys;
  private final Map<String, CExpression> expressionAtoms;
  private final Map<BinaryOperator, Map<CExpression, Map<CExpression, CBinaryExpression>>> cachedBinExpressions;
  private final Map<UnaryOperator, Map<CExpression, CUnaryExpression>> cachedUniExpressions;

  public ExpressionCache() {
    cachedExpressions = new HashSet<CExpression>();
    expressionAtoms = new HashMap<String, CExpression>();
    atomKeys = new HashMap<CExpression, String>();
    cachedBinExpressions = new HashMap<CBinaryExpression.BinaryOperator, Map<CExpression,Map<CExpression,CBinaryExpression>>>();
    cachedUniExpressions = new HashMap<CUnaryExpression.UnaryOperator, Map<CExpression,CUnaryExpression>>();
  }

  private String getKey(CExpression pExpr) {
    String key;
    if (pExpr instanceof CIntegerLiteralExpression) {
      key = atomKeys.get(pExpr);
      if (key == null) {
        key = ((CIntegerLiteralExpression) pExpr).getValue().toString();
        atomKeys.put(pExpr, key);
      }
    } else if (pExpr instanceof CIdExpression) {
        key = ((CIdExpression) pExpr).getName();
    } else {
      key = pExpr.toASTString();
    }
    return key;
  }

  private CExpression atomExpression (CExpression pExpr) {
    String key = getKey(pExpr);

    CExpression result = expressionAtoms.get(key);
    if (result == null) {
      result = pExpr;
      expressionAtoms.put(key, result);
      cachedExpressions.add(result);
    }
    return result;
  }


  public CExpression binaryExpression(BinaryOperator pOperator, CExpression pOp1, CExpression pOp2) throws UnrecognizedSyntaxException {
    pOp1 = lookupCachedExpressionVersion(pOp1);
    pOp2 = lookupCachedExpressionVersion(pOp2);

    Map<CExpression, Map<CExpression, CBinaryExpression>> operatorCache = cachedBinExpressions.get(pOperator);
    if (operatorCache == null) {
      operatorCache = new HashMap<CExpression, Map<CExpression,CBinaryExpression>>(10);
      cachedBinExpressions.put(pOperator, operatorCache);
    }

    Map<CExpression, CBinaryExpression> operatorOp1Cache = operatorCache.get(pOp1);
    if (operatorOp1Cache == null) {
      operatorOp1Cache = new HashMap<CExpression, CBinaryExpression>(5000);
      operatorCache.put(pOp1, operatorOp1Cache);
    }

    CBinaryExpression cachedExpression = operatorOp1Cache.get(pOp2);
    if (cachedExpression == null) {
      cachedExpression = new CBinaryExpression(null, null, pOp1, pOp2, pOperator);
      operatorOp1Cache.put(pOp2, cachedExpression);
      cachedExpressions.add(cachedExpression);
    }

    return cachedExpression;
  }

  public CExpression unaryExpression(UnaryOperator pOperator, CExpression pOp1) throws UnrecognizedSyntaxException {
    pOp1 = lookupCachedExpressionVersion(pOp1);

    Map<CExpression, CUnaryExpression> operatorCache = cachedUniExpressions.get(pOperator);
    if (operatorCache == null) {
      operatorCache = new HashMap<CExpression, CUnaryExpression>();
      cachedUniExpressions.put(pOperator, operatorCache);
    }

    CUnaryExpression cachedExpression = operatorCache.get(pOp1);
    if (cachedExpression == null) {
      cachedExpression = new CUnaryExpression(null, null, pOp1, pOperator);
      operatorCache.put(pOp1, cachedExpression);
      cachedExpressions.add(cachedExpression);
    }

    return cachedExpression;
  }

  public CExpression lookupCachedExpressionVersion(CExpression pExpression) throws UnrecognizedSyntaxException {
    if (pExpression == null) {
      return null;
    } else if (cachedExpressions.contains(pExpression)) {
      return pExpression;
    } else if (pExpression instanceof CBinaryExpression) {
      CBinaryExpression bin = (CBinaryExpression) pExpression;
      return binaryExpression(bin.getOperator(), bin.getOperand1(), bin.getOperand2());

    } else if (pExpression instanceof CUnaryExpression) {
      CUnaryExpression un = (CUnaryExpression) pExpression;
      return unaryExpression(un.getOperator(), un.getOperand());

    } else if (pExpression instanceof CIdExpression || pExpression instanceof CLiteralExpression) {
      return atomExpression(pExpression);

    } else {
      throw new UnrecognizedSyntaxException(String.format("Unsupported expression class (%s)!", pExpression.getClass().getSimpleName()) , pExpression);
    }
  }

  public boolean expressionsEqual(CExpression e1, CExpression e2) {
    return (e1 == e2);
  }

}
