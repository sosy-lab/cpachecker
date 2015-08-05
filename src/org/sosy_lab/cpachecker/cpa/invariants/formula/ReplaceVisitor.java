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

/**
 * Instances of this class are invariants formula visitors used to replace
 * parts of the visited formulae with other formulae.
 *
 * @param <T> the type of the constants used in the formulae.
 */
public class ReplaceVisitor<T> implements NumeralFormulaVisitor<T, NumeralFormula<T>>, BooleanFormulaVisitor<T, BooleanFormula<T>> {

  /**
   * The formula to be replaced.
   */
  private final NumeralFormula<T> toReplaceN;

  /**
   * The replacement formula.
   */
  private final NumeralFormula<T> replacementN;

  /**
   * The formula to be replaced.
   */
  private final BooleanFormula<T> toReplaceB;

  /**
   * The replacement formula.
   */
  private final BooleanFormula<T> replacementB;

  private final RecursiveNumeralFormulaVisitor<T> recursiveNumeralFormulaVisitor;

  private final RecursiveBooleanFormulaVisitor<T> recursiveBooleanFormulaVisitor;

  /**
   * Creates a new replace visitor for replacing occurrences of the first given
   * formula by the second given formula in visited formulae.
   *
   * @param pToReplace the formula to be replaced.
   * @param pReplacement the replacement formula.
   */
  public ReplaceVisitor(NumeralFormula<T> pToReplace,
      NumeralFormula<T> pReplacement) {
    this(pToReplace, pReplacement, null, null);
  }

  /**
   * Creates a new replace visitor for replacing occurrences of the first given
   * formula by the second given formula in visited formulae.
   *
   * @param pToReplace the formula to be replaced.
   * @param pReplacement the replacement formula.
   */
  public ReplaceVisitor(BooleanFormula<T> pToReplace,
      BooleanFormula<T> pReplacement) {
    this(null, null, pToReplace, pReplacement);
  }

  private ReplaceVisitor(NumeralFormula<T> pToReplaceN,
      NumeralFormula<T> pReplacementN, BooleanFormula<T> pToReplaceB,
      BooleanFormula<T> pReplacementB) {
    this.toReplaceN = pToReplaceN;
    this.replacementN = pReplacementN;
    this.toReplaceB = pToReplaceB;
    this.replacementB = pReplacementB;
    this.recursiveNumeralFormulaVisitor = new RecursiveNumeralFormulaVisitor<T>() {

      @Override
      protected NumeralFormula<T> visitPost(NumeralFormula<T> pFormula) {
        if (pFormula.equals(toReplaceN)) {
          return replacementN;
        }
        return pFormula;
      }

    };
    this.recursiveBooleanFormulaVisitor = new RecursiveBooleanFormulaVisitor<T>(this.recursiveNumeralFormulaVisitor) {

      @Override
      protected BooleanFormula<T> visitPost(BooleanFormula<T> pFormula) {
        if (pFormula.equals(toReplaceB)) {
          return replacementB;
        }
        return pFormula;
      }};
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
