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
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.rationals.Rational;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BitvectorFormulaManager;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.FloatingPointFormula;
import org.sosy_lab.java_smt.api.FloatingPointFormulaManager;
import org.sosy_lab.java_smt.api.FloatingPointNumber.Sign;
import org.sosy_lab.java_smt.api.FloatingPointRoundingMode;
import org.sosy_lab.java_smt.api.FloatingPointRoundingModeFormula;
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

  /**
   * To decide if we should use bitvectors as intermediary between ints and floats, we need:
   *
   * <ul>
   *   <li>the bitvector formula manager (some solvers don't have that)
   *   <li>to use SMT floats to represent floats (reals can be directly created from ints)
   *   <li>the type to be a bitvector
   *   <li>the unwrapped type to be integer
   * </ul>
   */
  private boolean isBitvectorIntermediateNecessary(FormulaType<?> type) {
    return bitvectorFormulaManager != null
        && useFloatForFloats()
        && type.isBitvectorType()
        && unwrapType(type).isIntegerType();
  }

  /**
   * Decides if an intermediary bitvector should be used; and returns one if so
   *
   * @param pFormula the candidate (potentially wrapped) integer
   * @return an optional bitvector of the same size as the wrapper specified
   */
  private Optional<BitvectorFormula> getBitvectorIntermediateIfNecessary(Formula pFormula) {
    if (isBitvectorIntermediateNecessary(getFormulaType(pFormula))) {
      return Optional.of(
          bitvectorFormulaManager.makeBitvector(
              bitvectorFormulaManager.getLength((BitvectorFormula) pFormula),
              (IntegerFormula) unwrap(pFormula)));
    }
    return Optional.empty();
  }

  @Override
  public <T extends Formula> T castTo(
      FloatingPointFormula pNumber, boolean pSigned, FormulaType<T> pTargetType) {
    // This method needs to unwrap/wrap or cast pTargetType and the return value,
    // in case they are replaced with other formula types.
    if (isBitvectorIntermediateNecessary(pTargetType)) {
      // to use a non-approximate solution, we first convert to bitvector, then cast to int.
      final BitvectorFormula bv = (BitvectorFormula) manager.castTo(pNumber, pSigned, pTargetType);
      return wrap(pTargetType, bitvectorFormulaManager.toIntegerFormula(bv, true));
    } else {
      return wrap(pTargetType, manager.castTo(pNumber, pSigned, unwrapType(pTargetType)));
    }
  }

  @Override
  public <T extends Formula> T castTo(
      FloatingPointFormula pNumber,
      boolean pSigned,
      FormulaType<T> pTargetType,
      FloatingPointRoundingMode pFloatingPointRoundingMode) {
    if (isBitvectorIntermediateNecessary(pTargetType)) {
      // to use a non-approximate solution, we first convert to bitvector, then cast to int.
      final BitvectorFormula bv =
          (BitvectorFormula)
              manager.castTo(pNumber, pSigned, pTargetType, pFloatingPointRoundingMode);
      return wrap(pTargetType, bitvectorFormulaManager.toIntegerFormula(bv, true));
    } else {
      return wrap(
          pTargetType,
          manager.castTo(pNumber, pSigned, unwrapType(pTargetType), pFloatingPointRoundingMode));
    }
  }

  @Override
  public FloatingPointFormula castFrom(
      Formula pNumber, boolean pSigned, FloatingPointType pTargetType) {
    final Formula from = interpretSourceFormula(pNumber);
    return manager.castFrom(from, pSigned, pTargetType);
  }

  @Override
  public FloatingPointFormula castFrom(
      Formula pNumber,
      boolean pSigned,
      FloatingPointType pTargetType,
      FloatingPointRoundingMode pFloatingPointRoundingMode) {
    final Formula from = interpretSourceFormula(pNumber);
    return manager.castFrom(from, pSigned, pTargetType, pFloatingPointRoundingMode);
  }

  /**
   * Computes the appropriate formula to cast from, taking into account that int↔float conversions
   * are approximated in most solvers. If {@code useIntForBitvectors()} is active and the given
   * formula represents a bitvector backed by an integer, it re-wraps the integer into a bitvector
   * instead of merely unwrapping it.
   */
  private Formula interpretSourceFormula(Formula pNumber) {
    return getBitvectorIntermediateIfNecessary(pNumber)
        .map(it -> (Formula) it)
        .orElse(unwrap(pNumber));
  }

  @Override
  public FloatingPointFormula fromIeeeBitvector(
      BitvectorFormula pNumber, FloatingPointType pTargetType) {
    if (useBitvectors()) {
      return manager.fromIeeeBitvector(pNumber, pTargetType);
    } else {
      return getBitvectorIntermediateIfNecessary(pNumber)
          .map(
              it ->
                  // we don't use bitvectors but have found an integer --> consider this as an
                  // unsigned integer
                  // representing a bitvector
                  // useFloatForFloats() is required here, because if it's not the case, then the
                  // actual manager
                  // will be the ReplaceFloatingPointWithNumeralAndFunctionTheory, which does not
                  // accept actual
                  // bitvectors, only wrapped integers. In those cases, we fall back to the
                  // approximation in the
                  // else branch of this decision.
                  manager.fromIeeeBitvector(it, pTargetType))
          .orElse(
              ReplaceFloatingPointWithNumeralAndFunctionTheory.createConversionUF(
                  pNumber, pTargetType, this, functionManager));
    }
  }

  @Override
  public BitvectorFormula toIeeeBitvector(FloatingPointFormula pNumber) {
    FloatingPointType type = (FloatingPointType) getFormulaType(pNumber);
    BitvectorType targetType = FormulaType.getBitvectorTypeWithSize(type.getTotalSize());
    if (useBitvectors()) {
      return manager.toIeeeBitvector(pNumber);
    } else if (isBitvectorIntermediateNecessary(targetType)) {
      // useFloatForFloats() is required here, because if it's not the case, then the actual manager
      // will be the ReplaceFloatingPointWithNumeralAndFunctionTheory, which does not accept actual
      // bitvectors, only wrapped integers. In those cases, we fall back to the approximation in the
      // else branch of this decision.
      final BitvectorFormula bv = manager.toIeeeBitvector(pNumber);
      final FormulaType<BitvectorFormula> retType = getFormulaType(bv);
      return wrap(retType, bitvectorFormulaManager.toIntegerFormula(bv, false));
    } else {
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
  public FloatingPointRoundingModeFormula makeRoundingMode(
      FloatingPointRoundingMode pFloatingPointRoundingMode) {
    // TODO:
    return null; // Ignore for now
  }

  @Override
  public FloatingPointFormula makeNumber(double pN, FloatingPointType type) {
    return manager.makeNumber(pN, type);
  }

  @Override
  public FloatingPointFormula makeNumber(
      double n, FloatingPointType type, FloatingPointRoundingMode pFloatingPointRoundingMode) {
    return manager.makeNumber(n, type, pFloatingPointRoundingMode);
  }

  @Override
  public FloatingPointFormula makeNumber(BigDecimal pN, FloatingPointType type) {
    return manager.makeNumber(pN, type);
  }

  @Override
  public FloatingPointFormula makeNumber(
      BigDecimal n, FloatingPointType type, FloatingPointRoundingMode pFloatingPointRoundingMode) {
    return manager.makeNumber(n, type, pFloatingPointRoundingMode);
  }

  @Override
  public FloatingPointFormula makeNumber(String pN, FloatingPointType type) {
    return manager.makeNumber(pN, type);
  }

  @Override
  public FloatingPointFormula makeNumber(
      String n, FloatingPointType type, FloatingPointRoundingMode pFloatingPointRoundingMode) {
    return manager.makeNumber(n, type, pFloatingPointRoundingMode);
  }

  @Override
  public FloatingPointFormula makeNumber(Rational pN, FloatingPointType type) {
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
  public FloatingPointFormula makeVariable(String pVar, FloatingPointType pType) {
    return manager.makeVariable(pVar, pType);
  }

  public FloatingPointFormula makeVariable(String pVar, int idx, FloatingPointType pType) {
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
