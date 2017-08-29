/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.expressions;

import java.util.Map;

import com.google.common.collect.Maps;


public abstract class CachingVisitor<LeafType, T, E extends Throwable> extends DefaultExpressionTreeVisitor<LeafType, T, E> {

  private final Map<ExpressionTree<LeafType>, T> memo = Maps.newHashMap();

  @Override
  protected T visitDefault(ExpressionTree<LeafType> pExpressionTree) throws E {
    T result = memo.get(pExpressionTree);
    if (result != null) {
      return result;
    }
    result = pExpressionTree.accept(new ExpressionTreeVisitor<LeafType, T, E>() {

      @Override
      public T visit(And<LeafType> pAnd) throws E {
        return cacheMissAnd(pAnd);
      }

      @Override
      public T visit(Or<LeafType> pOr) throws E {
        return cacheMissOr(pOr);
      }

      @Override
      public T visit(LeafExpression<LeafType> pLeafExpression) throws E {
        return cacheMissLeaf(pLeafExpression);
      }

      @Override
      public T visitTrue() throws E {
        return cacheMissTrue();
      }

      @Override
      public T visitFalse() throws E {
        return cacheMissFalse();
      }

    });
    memo.put(pExpressionTree, result);
    return result;
  }

  protected abstract T cacheMissAnd(And<LeafType> pAnd) throws E;

  protected abstract T cacheMissOr(Or<LeafType> pOr) throws E;

  protected abstract T cacheMissLeaf(LeafExpression<LeafType> pLeafExpression) throws E;

  protected abstract T cacheMissTrue() throws E;

  protected abstract T cacheMissFalse() throws E;

}
