// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariants.formula;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Instances of this class are visitors that are used to collect (sub)formulas that match a given
 * predicate.
 *
 * @param <T> the type of the constants used in the formulas.
 */
public class CollectFormulasVisitor<T>
    implements NumeralFormulaVisitor<T, Set<NumeralFormula<T>>>,
        BooleanFormulaVisitor<T, Set<NumeralFormula<T>>> {

  private final Predicate<? super NumeralFormula<T>> condition;

  public CollectFormulasVisitor(Predicate<? super NumeralFormula<T>> pCondition) {
    this.condition = pCondition;
  }

  @Override
  public Set<NumeralFormula<T>> visit(Add<T> pAdd) {
    Set<NumeralFormula<T>> result =
        concat(pAdd.getSummand1().accept(this), pAdd.getSummand2().accept(this));
    if (condition.apply(pAdd)) {
      result = add(result, pAdd);
    }
    return result;
  }

  @Override
  public Set<NumeralFormula<T>> visit(BinaryAnd<T> pAnd) {
    Set<NumeralFormula<T>> result =
        concat(pAnd.getOperand1().accept(this), pAnd.getOperand2().accept(this));
    if (condition.apply(pAnd)) {
      result = add(result, pAnd);
    }
    return result;
  }

  @Override
  public Set<NumeralFormula<T>> visit(BinaryNot<T> pNot) {
    Set<NumeralFormula<T>> result = pNot.getFlipped().accept(this);
    if (condition.apply(pNot)) {
      result = add(result, pNot);
    }
    return result;
  }

  @Override
  public Set<NumeralFormula<T>> visit(BinaryOr<T> pOr) {
    Set<NumeralFormula<T>> result =
        concat(pOr.getOperand1().accept(this), pOr.getOperand2().accept(this));
    if (condition.apply(pOr)) {
      result = add(result, pOr);
    }
    return result;
  }

  @Override
  public Set<NumeralFormula<T>> visit(BinaryXor<T> pXor) {
    Set<NumeralFormula<T>> result =
        concat(pXor.getOperand1().accept(this), pXor.getOperand2().accept(this));
    if (condition.apply(pXor)) {
      result = add(result, pXor);
    }
    return result;
  }

  @Override
  public Set<NumeralFormula<T>> visit(Constant<T> pConstant) {
    if (condition.apply(pConstant)) {
      return Collections.<NumeralFormula<T>>singleton(pConstant);
    }
    return ImmutableSet.of();
  }

  @Override
  public Set<NumeralFormula<T>> visit(Divide<T> pDivide) {
    Set<NumeralFormula<T>> result =
        concat(pDivide.getNumerator().accept(this), pDivide.getDenominator().accept(this));
    if (condition.apply(pDivide)) {
      result = add(result, pDivide);
    }
    return result;
  }

  @Override
  public Set<NumeralFormula<T>> visit(Equal<T> pEqual) {
    return concat(pEqual.getOperand1().accept(this), pEqual.getOperand2().accept(this));
  }

  @Override
  public Set<NumeralFormula<T>> visit(Exclusion<T> pExclusion) {
    Set<NumeralFormula<T>> result = pExclusion.getExcluded().accept(this);
    if (condition.apply(pExclusion)) {
      result = add(result, pExclusion);
    }
    return result;
  }

  @Override
  public Set<NumeralFormula<T>> visit(LessThan<T> pLessThan) {
    return concat(pLessThan.getOperand1().accept(this), pLessThan.getOperand2().accept(this));
  }

  @Override
  public Set<NumeralFormula<T>> visit(LogicalAnd<T> pAnd) {
    return concat(pAnd.getOperand1().accept(this), pAnd.getOperand2().accept(this));
  }

  @Override
  public Set<NumeralFormula<T>> visit(LogicalNot<T> pNot) {
    return pNot.getNegated().accept(this);
  }

  @Override
  public Set<NumeralFormula<T>> visit(Modulo<T> pModulo) {
    Set<NumeralFormula<T>> result =
        concat(pModulo.getNumerator().accept(this), pModulo.getDenominator().accept(this));
    if (condition.apply(pModulo)) {
      result = add(result, pModulo);
    }
    return result;
  }

  @Override
  public Set<NumeralFormula<T>> visit(Multiply<T> pMultiply) {
    Set<NumeralFormula<T>> result =
        concat(pMultiply.getFactor1().accept(this), pMultiply.getFactor2().accept(this));
    if (condition.apply(pMultiply)) {
      result = add(result, pMultiply);
    }
    return result;
  }

  @Override
  public Set<NumeralFormula<T>> visit(ShiftLeft<T> pShiftLeft) {
    Set<NumeralFormula<T>> result =
        concat(pShiftLeft.getShifted().accept(this), pShiftLeft.getShiftDistance().accept(this));
    if (condition.apply(pShiftLeft)) {
      result = add(result, pShiftLeft);
    }
    return result;
  }

  @Override
  public Set<NumeralFormula<T>> visit(ShiftRight<T> pShiftRight) {
    Set<NumeralFormula<T>> result =
        concat(pShiftRight.getShifted().accept(this), pShiftRight.getShiftDistance().accept(this));
    if (condition.apply(pShiftRight)) {
      result = add(result, pShiftRight);
    }
    return result;
  }

  @Override
  public Set<NumeralFormula<T>> visit(Union<T> pUnion) {
    Set<NumeralFormula<T>> result =
        concat(pUnion.getOperand1().accept(this), pUnion.getOperand2().accept(this));
    if (condition.apply(pUnion)) {
      result = add(result, pUnion);
    }
    return result;
  }

  @Override
  public Set<NumeralFormula<T>> visit(Variable<T> pVariable) {
    if (condition.apply(pVariable)) {
      return Collections.<NumeralFormula<T>>singleton(pVariable);
    }
    return ImmutableSet.of();
  }

  @Override
  public Set<NumeralFormula<T>> visitFalse() {
    return ImmutableSet.of();
  }

  @Override
  public Set<NumeralFormula<T>> visitTrue() {
    return ImmutableSet.of();
  }

  @Override
  public Set<NumeralFormula<T>> visit(IfThenElse<T> pIfThenElse) {
    return concat(
        pIfThenElse.getCondition().accept(this),
        concat(
            pIfThenElse.getPositiveCase().accept(this),
            pIfThenElse.getNegativeCase().accept(this)));
  }

  @Override
  public Set<NumeralFormula<T>> visit(Cast<T> pCast) {
    return pCast.getCasted().accept(this);
  }

  private static <T> Set<T> add(Set<T> pSet, T pElement) {
    return concat(pSet, Collections.singleton(pElement));
  }

  /**
   * Concatenates the given sets.
   *
   * @param a the first set.
   * @param b the second set.
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
