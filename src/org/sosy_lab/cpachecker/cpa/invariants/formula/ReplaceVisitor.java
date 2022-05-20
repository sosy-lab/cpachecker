// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariants.formula;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

/**
 * Instances of this class are invariants formula visitors used to replace parts of the visited
 * formulae with other formulae.
 *
 * @param <T> the type of the constants used in the formulae.
 */
public class ReplaceVisitor<T>
    implements NumeralFormulaVisitor<T, NumeralFormula<T>>,
        BooleanFormulaVisitor<T, BooleanFormula<T>> {

  /** Predicate indicating which numeral formula to replace. */
  private final Predicate<? super NumeralFormula<T>> toReplaceN;

  /** This function receives a formula to be replaced and produces the replacement. */
  private final Function<? super NumeralFormula<T>, ? extends NumeralFormula<T>> replacementN;

  /** Predicate indicating which boolean formula to replace. */
  private final Predicate<? super BooleanFormula<T>> toReplaceB;

  /** This function receives a formula to be replaced and produces the replacement. */
  private final Function<? super BooleanFormula<T>, ? extends BooleanFormula<T>> replacementB;

  private final RecursiveNumeralFormulaVisitor<T> recursiveNumeralFormulaVisitor;

  private final RecursiveBooleanFormulaVisitor<T> recursiveBooleanFormulaVisitor;

  /**
   * Creates a new replace visitor for replacing occurrences of the first given formula by the
   * second given formula in visited formulae.
   *
   * @param pToReplace the formula to be replaced.
   * @param pReplacement the replacement formula.
   */
  public ReplaceVisitor(NumeralFormula<T> pToReplace, NumeralFormula<T> pReplacement) {
    this(
        pToReplace == null
            ? Predicates.<NumeralFormula<T>>alwaysFalse()
            : Predicates.<NumeralFormula<T>>equalTo(pToReplace),
        Functions.constant(pReplacement),
        Predicates.<BooleanFormula<T>>alwaysFalse(),
        Functions.<BooleanFormula<T>>identity());
  }

  /**
   * Creates a new replace visitor for replacing matches of given predicate by the given formula in
   * visited formulae.
   *
   * @param pToReplace the predicate indicating what to replace.
   * @param pReplacement the replacement formula.
   */
  public ReplaceVisitor(
      Predicate<? super NumeralFormula<T>> pToReplace,
      Function<NumeralFormula<T>, NumeralFormula<T>> pReplacement) {
    this(
        pToReplace,
        pReplacement,
        Predicates.<BooleanFormula<T>>alwaysFalse(),
        Functions.<BooleanFormula<T>>identity());
  }

  /**
   * Creates a new replace visitor for replacing occurrences of the first given formula by the
   * second given formula in visited formulae.
   *
   * @param pToReplace the formula to be replaced.
   * @param pReplacement the replacement formula.
   */
  public ReplaceVisitor(BooleanFormula<T> pToReplace, BooleanFormula<T> pReplacement) {
    this(
        Predicates.<NumeralFormula<T>>alwaysFalse(),
        Functions.<NumeralFormula<T>>identity(),
        pToReplace == null
            ? Predicates.<BooleanFormula<T>>alwaysFalse()
            : Predicates.<BooleanFormula<T>>equalTo(pToReplace),
        Functions.constant(pReplacement));
  }

  private ReplaceVisitor(
      Predicate<? super NumeralFormula<T>> pToReplaceN,
      Function<? super NumeralFormula<T>, ? extends NumeralFormula<T>> pReplacementN,
      Predicate<? super BooleanFormula<T>> pToReplaceB,
      Function<? super BooleanFormula<T>, ? extends BooleanFormula<T>> pReplacementB) {
    Preconditions.checkNotNull(pToReplaceN);
    Preconditions.checkNotNull(pReplacementN);
    Preconditions.checkNotNull(pToReplaceB);
    Preconditions.checkNotNull(pReplacementB);
    this.toReplaceN = pToReplaceN;
    this.replacementN = pReplacementN;
    this.toReplaceB = pToReplaceB;
    this.replacementB = pReplacementB;
    this.recursiveNumeralFormulaVisitor =
        new RecursiveNumeralFormulaVisitor<>() {

          @Override
          protected NumeralFormula<T> visitPost(NumeralFormula<T> pFormula) {
            if (toReplaceN.apply(pFormula)) {
              return replacementN.apply(pFormula);
            }
            return pFormula;
          }
        };
    this.recursiveBooleanFormulaVisitor =
        new RecursiveBooleanFormulaVisitor<>(this.recursiveNumeralFormulaVisitor) {

          @Override
          protected BooleanFormula<T> visitPost(BooleanFormula<T> pFormula) {
            if (toReplaceB.apply(pFormula)) {
              return replacementB.apply(pFormula);
            }
            return pFormula;
          }
        };
  }

  @Override
  public BooleanFormula<T> visitFalse() {
    return this.recursiveBooleanFormulaVisitor.visitFalse();
  }

  @Override
  public BooleanFormula<T> visitTrue() {
    return this.recursiveBooleanFormulaVisitor.visitTrue();
  }

  @Override
  public BooleanFormula<T> visit(Equal<T> pEqual) {
    return pEqual.accept(this.recursiveBooleanFormulaVisitor);
  }

  @Override
  public BooleanFormula<T> visit(LessThan<T> pLessThan) {
    return pLessThan.accept(this.recursiveBooleanFormulaVisitor);
  }

  @Override
  public BooleanFormula<T> visit(LogicalAnd<T> pAnd) {
    return pAnd.accept(this.recursiveBooleanFormulaVisitor);
  }

  @Override
  public BooleanFormula<T> visit(LogicalNot<T> pNot) {
    return pNot.accept(this.recursiveBooleanFormulaVisitor);
  }

  @Override
  public NumeralFormula<T> visit(Add<T> pAdd) {
    return pAdd.accept(this.recursiveNumeralFormulaVisitor);
  }

  @Override
  public NumeralFormula<T> visit(BinaryAnd<T> pAnd) {
    return pAnd.accept(this.recursiveNumeralFormulaVisitor);
  }

  @Override
  public NumeralFormula<T> visit(BinaryNot<T> pNot) {
    return pNot.accept(this.recursiveNumeralFormulaVisitor);
  }

  @Override
  public NumeralFormula<T> visit(BinaryOr<T> pOr) {
    return pOr.accept(this.recursiveNumeralFormulaVisitor);
  }

  @Override
  public NumeralFormula<T> visit(BinaryXor<T> pXor) {
    return pXor.accept(this.recursiveNumeralFormulaVisitor);
  }

  @Override
  public NumeralFormula<T> visit(Constant<T> pConstant) {
    return pConstant.accept(this.recursiveNumeralFormulaVisitor);
  }

  @Override
  public NumeralFormula<T> visit(Divide<T> pDivide) {
    return pDivide.accept(this.recursiveNumeralFormulaVisitor);
  }

  @Override
  public NumeralFormula<T> visit(Exclusion<T> pExclusion) {
    return pExclusion.accept(this.recursiveNumeralFormulaVisitor);
  }

  @Override
  public NumeralFormula<T> visit(Modulo<T> pModulo) {
    return pModulo.accept(this.recursiveNumeralFormulaVisitor);
  }

  @Override
  public NumeralFormula<T> visit(Multiply<T> pMultiply) {
    return pMultiply.accept(this.recursiveNumeralFormulaVisitor);
  }

  @Override
  public NumeralFormula<T> visit(ShiftLeft<T> pShiftLeft) {
    return pShiftLeft.accept(this.recursiveNumeralFormulaVisitor);
  }

  @Override
  public NumeralFormula<T> visit(ShiftRight<T> pShiftRight) {
    return pShiftRight.accept(this.recursiveNumeralFormulaVisitor);
  }

  @Override
  public NumeralFormula<T> visit(Union<T> pUnion) {
    return pUnion.accept(this.recursiveNumeralFormulaVisitor);
  }

  @Override
  public NumeralFormula<T> visit(Variable<T> pVariable) {
    return pVariable.accept(this.recursiveNumeralFormulaVisitor);
  }

  @Override
  public NumeralFormula<T> visit(IfThenElse<T> pIfThenElse) {
    return pIfThenElse.accept(this.recursiveNumeralFormulaVisitor);
  }

  @Override
  public NumeralFormula<T> visit(Cast<T> pCast) {
    return pCast.accept(this.recursiveNumeralFormulaVisitor);
  }
}
