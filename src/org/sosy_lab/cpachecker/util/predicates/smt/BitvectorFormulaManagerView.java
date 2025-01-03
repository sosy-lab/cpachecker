// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.smt;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.errorprone.annotations.DoNotCall;
import java.math.BigInteger;
import java.util.List;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BitvectorFormulaManager;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.FormulaType.BitvectorType;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;

public class BitvectorFormulaManagerView extends BaseManagerView
    implements BitvectorFormulaManager {

  private final BitvectorFormulaManager manager;
  private final BooleanFormulaManager bmgr;

  BitvectorFormulaManagerView(
      FormulaWrappingHandler pWrappingHandler,
      BitvectorFormulaManager pManager,
      BooleanFormulaManager pBmgr) {
    super(pWrappingHandler);
    manager = checkNotNull(pManager);
    bmgr = checkNotNull(pBmgr);
  }

  public BooleanFormula notEqual(BitvectorFormula pNumber1, BitvectorFormula pNumber2) {
    return bmgr.not(equal(pNumber1, pNumber2));
  }

  public BitvectorFormula makeBitvector(FormulaType<BitvectorFormula> pType, long pI) {
    BitvectorType t = (BitvectorType) pType;
    return makeBitvector(t.getSize(), pI);
  }

  public BitvectorFormula makeBitvector(FormulaType<BitvectorFormula> pType, BigInteger pI) {
    BitvectorType t = (BitvectorType) pType;
    return makeBitvector(t.getSize(), pI);
  }

  @Override
  public BitvectorFormula negate(BitvectorFormula pNumber) {
    return manager.negate(pNumber);
  }

  @Override
  public BitvectorFormula add(BitvectorFormula pNumber1, BitvectorFormula pNumbe2) {
    return manager.add(pNumber1, pNumbe2);
  }

  @Override
  public BitvectorFormula subtract(BitvectorFormula pNumber1, BitvectorFormula pNumbe2) {
    return manager.subtract(pNumber1, pNumbe2);
  }

  /**
   * {@inheritDoc}
   *
   * <p>This method returns the formula for the C99-conform DIVIDE-operator, which is rounded
   * towards zero. SMTlib2 rounds towards positive or negative infinity, depending on both operands.
   *
   * <p>Example: SMTlib2: 10/3==3, 10/(-3)==(-3), (-10)/3==(-4), (-10)/(-3)==4 (4 different values!)
   * C99: 10/3==3, 10/(-3)==(-3), (-10)/3==(-3), (-10)/(-3)==3
   */
  @Override
  public BitvectorFormula divide(
      BitvectorFormula pNumber1, BitvectorFormula pNumber2, boolean signed) {
    return manager.divide(pNumber1, pNumber2, signed);
  }

  /**
   * This method is unsupported and always throws. For signed modulo as defined by the SMTLib2
   * standard, please use {@link BitvectorFormulaManagerView#smodulo(BitvectorFormula,
   * BitvectorFormula)} instead. However, note that the operation that is commonly called "modulo"
   * (e.g., in C or Java) is called "remainder" by SMTLib2 and provided by the {@link
   * BitvectorFormulaManagerView#remainder(BitvectorFormula, BitvectorFormula, boolean)} method, so
   * make sure to choose the correct method. We refer to the documentation of the respective methods
   * for their precise semantics and edge-cases. For the unsigned case, modulo and remainder are
   * equivalent and remainder can be used with the last parameter set to false.
   */
  @SuppressWarnings({"deprecation", "removal"})
  @DoNotCall
  @Override
  public final BitvectorFormula modulo(
      BitvectorFormula pNumber1, BitvectorFormula pNumber2, boolean signed) {
    throw new UnsupportedOperationException(
        "This operation has been deprecated and replaced by smodulo() and remainder().");
  }

  /**
   * {@inheritDoc}
   *
   * <p>Signed bitvector modulo operation. See {@link
   * BitvectorFormulaManager#smodulo(BitvectorFormula, BitvectorFormula)} for more information. For
   * unsigned bitvector modulo please use unsigned {@link
   * BitvectorFormulaManagerView#remainder(BitvectorFormula, BitvectorFormula, boolean)}.
   *
   * <p>Note: this does NOT behave in the same way the modulo operation (%) behaves in C or Java!
   */
  @Override
  public BitvectorFormula smodulo(BitvectorFormula numerator, BitvectorFormula denominator) {
    return manager.smodulo(numerator, denominator);
  }

  /**
   * {@inheritDoc}
   *
   * <p>This method behaves mostly according to the % operator in C or Java. While in C the modulo
   * operation is rounded towards 0, SMTLIB2 rounds towards the nearest infinity depending on the
   * operands so that the sign of the result of the operation is equal to the numerator sign.
   *
   * <p>
   */
  @Override
  public BitvectorFormula remainder(
      BitvectorFormula numerator, BitvectorFormula denominator, boolean signed) {
    return manager.remainder(numerator, denominator, signed);
  }

  @Override
  public BitvectorFormula multiply(BitvectorFormula pNumber1, BitvectorFormula pNumber2) {
    return manager.multiply(pNumber1, pNumber2);
  }

  @Override
  public BooleanFormula equal(BitvectorFormula pNumber1, BitvectorFormula pNumbe2) {
    return manager.equal(pNumber1, pNumbe2);
  }

  @Override
  public BooleanFormula greaterThan(
      BitvectorFormula pNumber1, BitvectorFormula pNumbe2, boolean signed) {
    return manager.greaterThan(pNumber1, pNumbe2, signed);
  }

  @Override
  public BooleanFormula greaterOrEquals(
      BitvectorFormula pNumber1, BitvectorFormula pNumbe2, boolean signed) {
    return manager.greaterOrEquals(pNumber1, pNumbe2, signed);
  }

  @Override
  public BooleanFormula lessThan(
      BitvectorFormula pNumber1, BitvectorFormula pNumbe2, boolean signed) {
    return manager.lessThan(pNumber1, pNumbe2, signed);
  }

  @Override
  public BooleanFormula lessOrEquals(
      BitvectorFormula pNumber1, BitvectorFormula pNumbe2, boolean signed) {
    return manager.lessOrEquals(pNumber1, pNumbe2, signed);
  }

  @Override
  public BitvectorFormula not(BitvectorFormula pBits) {
    return manager.not(pBits);
  }

  @Override
  public BitvectorFormula and(BitvectorFormula pBits1, BitvectorFormula pBits2) {
    return manager.and(pBits1, pBits2);
  }

  @Override
  public BitvectorFormula or(BitvectorFormula pBits1, BitvectorFormula pBits2) {
    return manager.or(pBits1, pBits2);
  }

  @Override
  public BitvectorFormula xor(BitvectorFormula pBits1, BitvectorFormula pBits2) {
    return manager.xor(pBits1, pBits2);
  }

  @Override
  public BitvectorFormula makeBitvector(int pLength, long pI) {
    return manager.makeBitvector(pLength, pI);
  }

  @Override
  public BitvectorFormula makeBitvector(int pLength, BigInteger pI) {
    return manager.makeBitvector(pLength, pI);
  }

  @Override
  public BitvectorFormula makeVariable(int pLength, String pVar) {
    return manager.makeVariable(pLength, pVar);
  }

  @Override
  public BitvectorFormula makeVariable(BitvectorType type, String pVar) {
    return manager.makeVariable(type.getSize(), pVar);
  }

  @Override
  public int getLength(BitvectorFormula pNumber) {
    return manager.getLength(pNumber);
  }

  /**
   * Returns a term representing the (arithmetic if signed is true) right shift of number by
   * toShift.
   */
  @Override
  public BitvectorFormula shiftRight(
      BitvectorFormula pNumber, BitvectorFormula pToShift, boolean signed) {
    return manager.shiftRight(pNumber, pToShift, signed);
  }

  @Override
  public BitvectorFormula shiftLeft(BitvectorFormula pNumber, BitvectorFormula pToShift) {
    return manager.shiftLeft(pNumber, pToShift);
  }

  @Override
  public BitvectorFormula rotateLeft(BitvectorFormula number, int toRotate) {
    return manager.rotateLeft(number, toRotate);
  }

  @Override
  public BitvectorFormula rotateLeft(BitvectorFormula number, BitvectorFormula toRotate) {
    return manager.rotateLeft(number, toRotate);
  }

  @Override
  public BitvectorFormula rotateRight(BitvectorFormula number, int toRotate) {
    return manager.rotateRight(number, toRotate);
  }

  @Override
  public BitvectorFormula rotateRight(BitvectorFormula number, BitvectorFormula toRotate) {
    return manager.rotateRight(number, toRotate);
  }

  @Override
  public BitvectorFormula concat(BitvectorFormula pPrefix, BitvectorFormula pSuffix) {
    return manager.concat(pPrefix, pSuffix);
  }

  @Override
  public BitvectorFormula extract(BitvectorFormula pNumber, int pMsb, int pLsb) {
    return manager.extract(pNumber, pMsb, pLsb);
  }

  @Override
  public BitvectorFormula extend(BitvectorFormula pNumber, int pExtensionBits, boolean pSigned) {
    return manager.extend(pNumber, pExtensionBits, pSigned);
  }

  @Override
  public BitvectorFormula makeBitvector(int pLength, IntegerFormula pI) {
    return manager.makeBitvector(pLength, pI);
  }

  @Override
  public IntegerFormula toIntegerFormula(BitvectorFormula pI, boolean pSigned) {
    return manager.toIntegerFormula(pI, pSigned);
  }

  @Override
  public BooleanFormula distinct(List<BitvectorFormula> pBits) {
    return manager.distinct(pBits);
  }
}
