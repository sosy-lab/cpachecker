/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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

import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.core.counterexample.CExpressionToOrinalCodeVisitor;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;


public enum ToCodeVisitor implements ExpressionTreeVisitor<String> {

  INSTANCE;

  public static final Function<ExpressionTree, String> TO_CODE = new Function<ExpressionTree, String>() {

    @Override
    public String apply(ExpressionTree pArg0) {
      return pArg0.accept(INSTANCE);
    }

  };

  @Override
  public String visit(And pAnd) {
    assert pAnd.iterator().hasNext();
    return "(" + Joiner.on(" && ").join(FluentIterable.from(pAnd).transform(TO_CODE)) + ")";
  }

  @Override
  public String visit(Or pOr) {
    assert pOr.iterator().hasNext();
    return "(" + Joiner.on(" || ").join(FluentIterable.from(pOr).transform(TO_CODE)) + ")";
  }

  @Override
  public String visit(LeafExpression pLeafExpression) {
    AExpression expression = pLeafExpression.getExpression();
    if (!(expression instanceof CExpression)) {
      throw new AssertionError("Unsupported expression.");
    }
    return ((CExpression) expression).accept(CExpressionToOrinalCodeVisitor.INSTANCE);
  }

}
