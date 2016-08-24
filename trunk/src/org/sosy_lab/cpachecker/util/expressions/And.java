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

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

public class And<LeafType> extends AbstractExpressionTree<LeafType>
    implements Iterable<ExpressionTree<LeafType>> {

  private final ImmutableSet<ExpressionTree<LeafType>> operands;

  private final int hashCode;

  private And(ImmutableSet<ExpressionTree<LeafType>> pOperands) {
    assert Iterables.size(pOperands) >= 2;
    assert !Iterables.contains(pOperands, ExpressionTrees.getFalse());
    assert !Iterables.contains(pOperands, ExpressionTrees.getTrue());
    assert !FluentIterable.from(pOperands).anyMatch(Predicates.instanceOf(And.class));
    operands = pOperands;
    hashCode = operands.hashCode();
  }

  @Override
  public Iterator<ExpressionTree<LeafType>> iterator() {
    return operands.iterator();
  }

  @Override
  public <T, E extends Throwable> T accept(ExpressionTreeVisitor<LeafType, T, E> pVisitor)
      throws E {
    return pVisitor.visit(this);
  }

  @Override
  public int hashCode() {
    return hashCode;
  }

  @Override
  public boolean equals(Object pObj) {
    if (this == pObj) {
      return true;
    }
    if (pObj instanceof And) {
      And<?> other = (And<?>) pObj;
      return hashCode == other.hashCode && operands.equals(other.operands);
    }
    return false;
  }

  public static <LeafType> ExpressionTree<LeafType> of(
      ExpressionTree<LeafType> pOp1, ExpressionTree<LeafType> pOp2) {
    return of(ImmutableList.of(pOp1, pOp2));
  }

  public static <LeafType> ExpressionTree<LeafType> of(
      Iterable<ExpressionTree<LeafType>> pOperands) {
    // If one of the operands is false, return false
    if (Iterables.contains(pOperands, ExpressionTrees.getFalse())) {
      return ExpressionTrees.getFalse();
    }
    // Filter out trivial operands and flatten the hierarchy
    ImmutableSet<ExpressionTree<LeafType>> operands =
        FluentIterable.from(pOperands)
            .filter(Predicates.not(Predicates.equalTo(ExpressionTrees.<LeafType>getTrue())))
            .transformAndConcat(
                new Function<ExpressionTree<LeafType>, Iterable<ExpressionTree<LeafType>>>() {

                  @Override
                  public Iterable<ExpressionTree<LeafType>> apply(
                      ExpressionTree<LeafType> pOperand) {
                    if (pOperand instanceof And) {
                      return (And<LeafType>) pOperand;
                    }
                    return Collections.singleton(pOperand);
                  }
                })
            .toSet();
    // If there are no operands, return the neutral element
    if (operands.isEmpty()) {
      return ExpressionTrees.getTrue();
    }
    // If there is only one operand, return it
    if (operands.size() == 1) {
      return operands.iterator().next();
    }
    return new And<>(operands);
  }

}
