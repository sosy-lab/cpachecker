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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

public class And implements ExpressionTree, Iterable<ExpressionTree> {

  private final List<ExpressionTree> operands;

  private And(Iterable<? extends ExpressionTree> pOperands) {
    assert Iterables.size(pOperands) >= 2;
    assert !Iterables.contains(pOperands, ExpressionTree.FALSE);
    assert !Iterables.contains(pOperands, ExpressionTree.TRUE);
    assert !FluentIterable.from(pOperands).anyMatch(Predicates.instanceOf(And.class));
    operands = ImmutableList.copyOf(pOperands);
  }

  @Override
  public Iterator<ExpressionTree> iterator() {
    return operands.iterator();
  }

  @Override
  public <T> T accept(ExpressionTreeVisitor<T> pVisitor) {
    return pVisitor.visit(this);
  }

  @Override
  public int hashCode() {
    return operands.hashCode();
  }

  @Override
  public boolean equals(Object pObj) {
    if (this == pObj) {
      return true;
    }
    if (pObj instanceof And) {
      return operands.equals(((And) pObj).operands);
    }
    return false;
  }

  @Override
  public String toString() {
    return ToCodeVisitor.INSTANCE.visit(this);
  }

  public static ExpressionTree of(Iterable<? extends ExpressionTree> pOperands) {
    // If one of the operands is false, return false
    if (Iterables.contains(pOperands, ExpressionTree.FALSE)) {
      return ExpressionTree.FALSE;
    }
    // Filter out trivial operands and flatten the hierarchy
    FluentIterable<? extends ExpressionTree> operands =
        FluentIterable.from(pOperands)
            .filter(Predicates.not(Predicates.equalTo(ExpressionTree.TRUE)))
            .transformAndConcat(
                new Function<ExpressionTree, Iterable<ExpressionTree>>() {

                  @Override
                  public Iterable<ExpressionTree> apply(ExpressionTree pOperand) {
                    if (pOperand instanceof And) {
                      return (And) pOperand;
                    }
                    return Collections.singleton(pOperand);
                  }
                });
    // If there are no operands, return the neutral element
    if (operands.isEmpty()) {
      return ExpressionTree.TRUE;
    }
    // If there is only one operand, return it
    if (operands.skip(1).isEmpty()) {
      return operands.iterator().next();
    }
    return new And(operands);
  }

}
