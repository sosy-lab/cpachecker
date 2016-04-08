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
package org.sosy_lab.cpachecker.cpa.invariants.formula;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Instances of this class are invariants formula visitors used to split the
 * visited formulae on their outer conjunctions into lists of the conjunction
 * operands. This is done recursively so that nested conjunctions are split
 * as well.
 *
 * @param <T> the type of the constants used in the formulae.
 */
public class SplitConjunctionsVisitor<T> implements BooleanFormulaVisitor<T, List<BooleanFormula<T>>> {

  @Override
  public List<BooleanFormula<T>> visit(Equal<T> pEqual) {
    return Collections.<BooleanFormula<T>>singletonList(pEqual);
  }

  @Override
  public List<BooleanFormula<T>> visit(LessThan<T> pLessThan) {
    return Collections.<BooleanFormula<T>>singletonList(pLessThan);
  }

  @Override
  public List<BooleanFormula<T>> visit(LogicalAnd<T> pAnd) {
    List<BooleanFormula<T>> left = pAnd.getOperand1().accept(this);
    // If the left operand is false, return false
    if (left.contains(BooleanConstant.<T>getFalse())) {
      return visitFalse();
    }
    List<BooleanFormula<T>> right = pAnd.getOperand2().accept(this);
    // If the right operand is false, return false
    if (right.contains(BooleanConstant.<T>getFalse())) {
      return visitFalse();
    }
    return concat(left, right);
  }

  @Override
  public List<BooleanFormula<T>> visit(LogicalNot<T> pNot) {
    return Collections.<BooleanFormula<T>>singletonList(pNot);
  }

  @Override
  public List<BooleanFormula<T>> visitFalse() {
    return Collections.<BooleanFormula<T>>singletonList(BooleanConstant.<T>getFalse());
  }

  @Override
  public List<BooleanFormula<T>> visitTrue() {
    return Collections.emptyList();
  }

  /**
   * Concatenates two lists with respect to the types of lists occurring within
   * this visitor class.
   *
   * @param a the first list.
   * @param b the second list.
   *
   * @return a list containing all elements from both lists.
   */
  private static <T> List<T> concat(List<T> a, List<T> b) {
    // If one of the lists is empty, return the other one
    if (a.isEmpty()) {
      return b;
    }
    if (b.isEmpty()) {
      return a;
    }
    /*
     * If both lists contain exactly one element, they might both be singleton
     * lists and thus immutable, so the elements of both lists are added to a
     * new one.
     */
    if (a.size() == 1 && b.size() == 1) {
      List<T> result = new ArrayList<>(a);
      result.addAll(b);
      return result;
    }
    /*
     * At this point, only one of the lists can be a singleton list. The
     * element of a list containing just one element is added to the other
     * list.
     */
    if (a.size() == 1) {
      b.addAll(a);
      return b;
    }
    /*
     * At least the first list contains more than one element and is mutable at
     * this point, so all elements of one list are simple added to the other
     * one.
     */
    a.addAll(b);
    return a;
  }

}
