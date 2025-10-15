// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.smt;

import com.google.common.base.Preconditions;
import java.math.BigDecimal;
import java.math.BigInteger;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.rationals.Rational;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BitvectorFormulaManager;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.FloatingPointFormula;
import org.sosy_lab.java_smt.api.FloatingPointFormulaManager;
import org.sosy_lab.java_smt.api.FloatingPointNumber.Sign;
import org.sosy_lab.java_smt.api.FloatingPointRoundingMode;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.FormulaType.BitvectorType;
import org.sosy_lab.java_smt.api.FormulaType.FloatingPointType;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.java_smt.api.UFManager;

public class FloatingPointFormulaManagerView extends BaseManagerView
    implements FloatingPointFormulaManager {

  private final FloatingPointFormulaManager manager;
  private final UFManager functionManager;
  @Nullable private final BitvectorFormulaManager bitvectorFormulaManager;

  FloatingPointFormulaManagerView(
      FormulaWrappingHandler pWrappingHandler,
      FloatingPointFormulaManager pManager,
      UFManager pFunctionManager,
      @Nullable BitvectorFormulaManager pBitvectorFormulaManager) {
    super(pWrappingHandler);
    manager = Preconditions.checkNotNull(pManager);
    functionManager = Preconditions.checkNotNull(pFunctionManager);
    bitvectorFormulaManager = pBitvectorFormulaManager;
  }

  // unwraps a type, unless it is a bitvector type, and useIntAsBitvector()
  private FormulaType<?> unwrapTypeUnlessBvAsInt(FormulaType<?> pType) {
    if (useIntAsBitvector() && pType.isBitvectorType()) {
      return pType;
    } else {
      return unwrapType(pType);
    }
  }

  @Override
  public <T extends Formula> T castTo(
      FloatingPointFormula pNumber, boolean pSigned, FormulaType<T> pTargetType) {
    // This method needs to unwrap/wrap pTargetType and the return value,
    // in case they are replaced with other formula types.
    return wrap(
        pTargetType, manager.castTo(pNumber, pSigned, unwrapTypeUnlessBvAsInt(pTargetType)));
  }

  @Override
  public <T extends Formula> T castTo(
      FloatingPointFormula number,
      boolean pSigned,
      FormulaType<T> targetType,
      FloatingPointRoundingMode pFloatingPointRoundingMode) {
    return wrap(
        targetType,
        manager.castTo(
            number, pSigned, unwrapTypeUnlessBvAsInt(targetType), pFloatingPointRoundingMode));
  }

  @Override
  public FloatingPointFormula castFrom(
      Formula pNumber, boolean pSigned, FloatingPointType pTargetType) {
    // This method needs to unwrap pNumber,
    // in case it is replaced with another formula type.
    return manager.castFrom(unwrap(pNumber), pSigned, pTargetType);
  }

  @Override
  public FloatingPointFormula castFrom(
      Formula number,
      boolean signed,
      FloatingPointType targetType,
      FloatingPointRoundingMode pFloatingPointRoundingMode) {
    return manager.castFrom(unwrap(number), signed, targetType, pFloatingPointRoundingMode);
  }

  @Override
  public FloatingPointFormula fromIeeeBitvector(
      BitvectorFormula pNumber, FloatingPointType pTargetType) {
    if (useBitvectors()) {
      return manager.fromIeeeBitvector(pNumber, pTargetType);
    } else if (useIntAsBitvector() && bitvectorFormulaManager != null) {
      // we don't use bitvectors but have found an integer --> consider this as an unsigned integer
      // representing a bitvector
      final var bv =
          bitvectorFormulaManager.makeBitvector(
              bitvectorFormulaManager.getLength(pNumber), (IntegerFormula) unwrap(pNumber));
      return manager.fromIeeeBitvector(bv, pTargetType);
    } else {
      return ReplaceFloatingPointWithNumeralAndFunctionTheory.createConversionUF(
          pNumber, pTargetType, this, functionManager);
    }
  }

  @Override
  public BitvectorFormula toIeeeBitvector(FloatingPointFormula pNumber) {
    if (useBitvectors()) {
      return manager.toIeeeBitvector(pNumber);
    } else if (useIntAsBitvector() && bitvectorFormulaManager != null) {
      final var bv = manager.toIeeeBitvector(pNumber);
      final var retType = getFormulaType(bv);
      return wrap(retType, bitvectorFormulaManager.toIntegerFormula(bv, false));
    } else {
      FloatingPointType type = (FloatingPointType) getFormulaType(pNumber);
      BitvectorType targetType = FormulaType.getBitvectorTypeWithSize(type.getTotalSize());
      return ReplaceFloatingPointWithNumeralAndFunctionTheory.createConversionUF(
          pNumber, targetType, this, functionManager);
    }
  }

  @Override
  public FloatingPointFormula negate(FloatingPointFormula pNumber) {
    return manager.negate(pNumber);
  }

  @Override
  public FloatingPointFormula add(FloatingPointFormula pNumber1, FloatingPointFormula pNumbe2) {
    return manager.add(pNumber1, pNumbe2);
  }

  @Override
  public FloatingPointFormula add(
      FloatingPointFormula number1,
      FloatingPointFormula number2,
      FloatingPointRoundingMode pFloatingPointRoundingMode) {
    return manager.add(number1, number2, pFloatingPointRoundingMode);
  }

  @Override
  public FloatingPointFormula subtract(
      FloatingPointFormula pNumber1, FloatingPointFormula pNumbe2) {
    return manager.subtract(pNumber1, pNumbe2);
  }

  @Override
  public FloatingPointFormula subtract(
      FloatingPointFormula number1,
      FloatingPointFormula number2,
      FloatingPointRoundingMode pFloatingPointRoundingMode) {
    return manager.subtract(number1, number2, pFloatingPointRoundingMode);
  }

  @Override
  public FloatingPointFormula divide(FloatingPointFormula pNumber1, FloatingPointFormula pNumbe2) {
    return manager.divide(pNumber1, pNumbe2);
  }

  @Override
  public FloatingPointFormula divide(
      FloatingPointFormula number1,
      FloatingPointFormula number2,
      FloatingPointRoundingMode pFloatingPointRoundingMode) {
    return manager.divide(number1, number2, pFloatingPointRoundingMode);
  }

  @Override
  public FloatingPointFormula multiply(
      FloatingPointFormula pNumber1, FloatingPointFormula pNumbe2) {
    return manager.multiply(pNumber1, pNumbe2);
  }

  @Override
  public FloatingPointFormula multiply(
      FloatingPointFormula number1,
      FloatingPointFormula number2,
      FloatingPointRoundingMode pFloatingPointRoundingMode) {
    return manager.multiply(number1, number2, pFloatingPointRoundingMode);
  }

  @Override
  public FloatingPointFormula remainder(
      FloatingPointFormula dividend, FloatingPointFormula divisor) {
    return manager.remainder(dividend, divisor);
  }

  @Override
  public BooleanFormula assignment(FloatingPointFormula pNumber1, FloatingPointFormula pNumber2) {
    return manager.assignment(pNumber1, pNumber2);
  }

  @Override
  public BooleanFormula equalWithFPSemantics(
      FloatingPointFormula pNumber1, FloatingPointFormula pNumbe2) {
    return manager.equalWithFPSemantics(pNumber1, pNumbe2);
  }

  @Override
  public BooleanFormula greaterThan(FloatingPointFormula pNumber1, FloatingPointFormula pNumbe2) {
    return manager.greaterThan(pNumber1, pNumbe2);
  }

  @Override
  public BooleanFormula greaterOrEquals(
      FloatingPointFormula pNumber1, FloatingPointFormula pNumbe2) {
    return manager.greaterOrEquals(pNumber1, pNumbe2);
  }

  @Override
  public BooleanFormula lessThan(FloatingPointFormula pNumber1, FloatingPointFormula pNumbe2) {
    return manager.lessThan(pNumber1, pNumbe2);
  }

  @Override
  public BooleanFormula lessOrEquals(FloatingPointFormula pNumber1, FloatingPointFormula pNumbe2) {
    return manager.lessOrEquals(pNumber1, pNumbe2);
  }

  @Override
  public BooleanFormula isNaN(FloatingPointFormula pNumber) {
    return manager.isNaN(pNumber);
  }

  @Override
  public BooleanFormula isInfinity(FloatingPointFormula pNumber) {
    return manager.isInfinity(pNumber);
  }

  @Override
  public BooleanFormula isZero(FloatingPointFormula pNumber) {
    return manager.isZero(pNumber);
  }

  @Override
  public BooleanFormula isNegative(FloatingPointFormula pNumber) {
    return manager.isNegative(pNumber);
  }

  @Override
  public BooleanFormula isSubnormal(FloatingPointFormula pNumber) {
    return manager.isSubnormal(pNumber);
  }

  @Override
  public BooleanFormula isNormal(FloatingPointFormula pNumber) {
    return manager.isNormal(pNumber);
  }

  @Override
  public FloatingPointFormula makeNumber(double pN, FormulaType.FloatingPointType type) {
    return manager.makeNumber(pN, type);
  }

  @Override
  public FloatingPointFormula makeNumber(
      double n, FloatingPointType type, FloatingPointRoundingMode pFloatingPointRoundingMode) {
    return manager.makeNumber(n, type, pFloatingPointRoundingMode);
  }

  @Override
  public FloatingPointFormula makeNumber(BigDecimal pN, FormulaType.FloatingPointType type) {
    return manager.makeNumber(pN, type);
  }

  @Override
  public FloatingPointFormula makeNumber(
      BigDecimal n, FloatingPointType type, FloatingPointRoundingMode pFloatingPointRoundingMode) {
    return manager.makeNumber(n, type, pFloatingPointRoundingMode);
  }

  @Override
  public FloatingPointFormula makeNumber(String pN, FormulaType.FloatingPointType type) {
    return manager.makeNumber(pN, type);
  }

  @Override
  public FloatingPointFormula makeNumber(
      String n, FloatingPointType type, FloatingPointRoundingMode pFloatingPointRoundingMode) {
    return manager.makeNumber(n, type, pFloatingPointRoundingMode);
  }

  @Override
  public FloatingPointFormula makeNumber(Rational pN, FormulaType.FloatingPointType type) {
    return manager.makeNumber(pN, type);
  }

  @Override
  public FloatingPointFormula makeNumber(
      Rational n, FloatingPointType type, FloatingPointRoundingMode pFloatingPointRoundingMode) {
    return manager.makeNumber(n, type, pFloatingPointRoundingMode);
  }

  @Override
  public FloatingPointFormula makeNumber(
      BigInteger exponent, BigInteger mantissa, Sign sign, FloatingPointType type) {
    return manager.makeNumber(exponent, mantissa, sign, type);
  }

  @Override
  public FloatingPointFormula makeVariable(String pVar, FormulaType.FloatingPointType pType) {
    return manager.makeVariable(pVar, pType);
  }

  public FloatingPointFormula makeVariable(
      String pVar, int idx, FormulaType.FloatingPointType pType) {
    return manager.makeVariable(FormulaManagerView.makeName(pVar, idx), pType);
  }

  @Override
  public FloatingPointFormula makePlusInfinity(FloatingPointType pType) {
    return manager.makePlusInfinity(pType);
  }

  @Override
  public FloatingPointFormula makeMinusInfinity(FloatingPointType pType) {
    return manager.makeMinusInfinity(pType);
  }

  @Override
  public FloatingPointFormula makeNaN(FloatingPointType pType) {
    return manager.makeNaN(pType);
  }

  @Override
  public FloatingPointFormula round(
      FloatingPointFormula pNumber, FloatingPointRoundingMode pFloatingPointRoundingMode) {
    return manager.round(pNumber, pFloatingPointRoundingMode);
  }

  @Override
  public FloatingPointFormula abs(FloatingPointFormula pNumber) {
    return manager.abs(pNumber);
  }

  @Override
  public FloatingPointFormula max(FloatingPointFormula pNumber1, FloatingPointFormula pNumber2) {
    return manager.max(pNumber1, pNumber2);
  }

  @Override
  public FloatingPointFormula min(FloatingPointFormula pNumber1, FloatingPointFormula pNumber2) {
    return manager.min(pNumber1, pNumber2);
  }

  @Override
  public FloatingPointFormula sqrt(FloatingPointFormula pNumber) {
    return manager.sqrt(pNumber);
  }

  @Override
  public FloatingPointFormula sqrt(
      FloatingPointFormula pNumber, FloatingPointRoundingMode pRoundingMode) {
    return manager.sqrt(pNumber, pRoundingMode);
  }
}
