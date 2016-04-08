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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * Instances of this class are visitors that are used to collect the variables
 * used in the given formulae. Possible environments are ignored by these
 * visitors, thus variables are not evaluated any further. This means, that in
 * a formula ((x + y) * z) only the variables x, y and z are found, even if for
 * example an environment exists that states that x = a + b.
 *
 * @param <T> the type of the constants used in the formulae.
 */
public class CollectVarsVisitor<T> implements NumeralFormulaVisitor<T, Set<MemoryLocation>>, BooleanFormulaVisitor<T, Set<MemoryLocation>> {

  @Override
  public Set<MemoryLocation> visit(Add<T> pAdd) {
    return concat(pAdd.getSummand1().accept(this), pAdd.getSummand2().accept(this));
  }

  @Override
  public Set<MemoryLocation> visit(BinaryAnd<T> pAnd) {
    return concat(pAnd.getOperand1().accept(this), pAnd.getOperand2().accept(this));
  }

  @Override
  public Set<MemoryLocation> visit(BinaryNot<T> pNot) {
    return pNot.getFlipped().accept(this);
  }

  @Override
  public Set<MemoryLocation> visit(BinaryOr<T> pOr) {
    return concat(pOr.getOperand1().accept(this), pOr.getOperand2().accept(this));
  }

  @Override
  public Set<MemoryLocation> visit(BinaryXor<T> pXor) {
    return concat(pXor.getOperand1().accept(this), pXor.getOperand2().accept(this));
  }

  @Override
  public Set<MemoryLocation> visit(Constant<T> pConstant) {
    return Collections.emptySet();
  }

  @Override
  public Set<MemoryLocation> visit(Divide<T> pDivide) {
    return concat(pDivide.getNumerator().accept(this), pDivide.getDenominator().accept(this));
  }

  @Override
  public Set<MemoryLocation> visit(Equal<T> pEqual) {
    return concat(pEqual.getOperand1().accept(this), pEqual.getOperand2().accept(this));
  }

  @Override
  public Set<MemoryLocation> visit(Exclusion<T> pExclusion) {
    return pExclusion.getExcluded().accept(this);
  }

  @Override
  public Set<MemoryLocation> visit(LessThan<T> pLessThan) {
    return concat(pLessThan.getOperand1().accept(this), pLessThan.getOperand2().accept(this));
  }

  @Override
  public Set<MemoryLocation> visit(LogicalAnd<T> pAnd) {
    return concat(pAnd.getOperand1().accept(this), pAnd.getOperand2().accept(this));
  }

  @Override
  public Set<MemoryLocation> visit(LogicalNot<T> pNot) {
    return pNot.getNegated().accept(this);
  }

  @Override
  public Set<MemoryLocation> visit(Modulo<T> pModulo) {
    return concat(pModulo.getNumerator().accept(this), pModulo.getDenominator().accept(this));
  }

  @Override
  public Set<MemoryLocation> visit(Multiply<T> pMultiply) {
    return concat(pMultiply.getFactor1().accept(this), pMultiply.getFactor2().accept(this));
  }

  @Override
  public Set<MemoryLocation> visit(ShiftLeft<T> pShiftLeft) {
    return concat(pShiftLeft.getShifted().accept(this), pShiftLeft.getShiftDistance().accept(this));
  }

  @Override
  public Set<MemoryLocation> visit(ShiftRight<T> pShiftRight) {
    return concat(pShiftRight.getShifted().accept(this), pShiftRight.getShiftDistance().accept(this));
  }

  @Override
  public Set<MemoryLocation> visit(Union<T> pUnion) {
    return concat(pUnion.getOperand1().accept(this), pUnion.getOperand2().accept(this));
  }

  @Override
  public Set<MemoryLocation> visit(Variable<T> pVariable) {
    return Collections.singleton(pVariable.getMemoryLocation());
  }

  @Override
  public Set<MemoryLocation> visitFalse() {
    return Collections.emptySet();
  }

  @Override
  public Set<MemoryLocation> visitTrue() {
    return Collections.emptySet();
  }

  @Override
  public Set<MemoryLocation> visit(IfThenElse<T> pIfThenElse) {
    return concat(
        pIfThenElse.getCondition().accept(this),
        concat(
            pIfThenElse.getPositiveCase().accept(this),
            pIfThenElse.getNegativeCase().accept(this))
        );
  }

  @Override
  public Set<MemoryLocation> visit(Cast<T> pCast) {
    return pCast.getCasted().accept(this);
  }

  /**
   * Concatenates the given sets.
   *
   * @param a the first set.
   * @param b the second set.
   *
   * @return the concatenation of the given sets.
   */
  private static <T> Set<T> concat(Set<T> a, Set<T> b) {
    // If one of the sets is empty, return the other one
    if (a.isEmpty()) {
      return b;
    }
    if (b.isEmpty()) {
      return a;
    }
    // If both sets are equal, return any one of the sets
    if (a.equals(b)) {
      return a;
    }
    /*
     * At this point, both sets are guaranteed to be different. A set of size
     * one might be a singleton set that is unmodifiable, thus if both sets
     * are of size one, a new modifiable set is created for the result.
     */
    if (a.size() == 1 && b.size() == 1) {
      Set<T> result = new LinkedHashSet<>(a);
      result.addAll(b);
      return result;
    }
    /*
     * If a contains only one element, b must be larger and thus a modifiable
     * set in the context of this class, so the element of a is added to it.
     */
    if (a.size() == 1) {
      b.addAll(a);
      return b;
    }
    /*
     * If b contains only one element, a must be larger and thus a modifiable
     * set in the context of this class, so the element of b is added to it.
     */
    if (b.size() == 1) {
      a.addAll(b);
      return a;
    }
    /*
     * Both sets are contain multiple values and are modifiable, so add all
     * values of one of the sets to the other set and return the latter one.
     */
    a.addAll(b);
    return a;
  }

}
