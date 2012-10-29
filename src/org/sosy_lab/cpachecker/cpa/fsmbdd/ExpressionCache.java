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
import java.util.Map;

import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;


public class ExpressionCache {

  private Map<String, CExpression> expressionCache;
  private ExpressionToString expressionToStringVisitor;

  public ExpressionCache() {
    this.expressionCache = new HashMap<String, CExpression>();
    this.expressionToStringVisitor = new ExpressionToString();
  }

  private CExpression fetchCachedExpression(CExpression expr, int maxDepth) {
    if (expr instanceof CBinaryExpression) {
      CBinaryExpression be = (CBinaryExpression) expr;

      BinaryOperator op = be.getOperator();
      CExpression left = be.getOperand1();
      CExpression right = be.getOperand2();

      return fetchCachedBinExpression(left, op, right, maxDepth - 1);
    } else {
      String key = expr.toASTString();
      CExpression result = expressionCache.get(key);
      if (result == null) {
        result = expr;
        expressionCache.put(key, result);
      }
      return expr;
    }
  }

  public CExpression fetchCachedBinExpression(CExpression left, BinaryOperator op, CExpression right, int maxDepth) {
    CExpression result = new CBinaryExpression(left.getFileLocation(),
        null,
        fetchCachedExpression(left, maxDepth - 1),
        fetchCachedExpression(right, maxDepth - 1),
        op);

    if (maxDepth > 0) {
      String key = result.accept(expressionToStringVisitor);
      CExpression cached = expressionCache.get(key);
      if (cached == null) {
        expressionCache.put(key, result);
      } else {
        result = cached;
      }
    }

    return result;
  }

  public CExpression fetchCachedBinExpression(CExpression left, BinaryOperator op, CExpression right) {
    return fetchCachedBinExpression(left, op, right, 0);
  }

}
