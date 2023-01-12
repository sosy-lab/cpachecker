// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.smt;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.Lists;
import java.util.List;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView.Theory;
import org.sosy_lab.cpachecker.util.predicates.smt.ReplaceIntegerWithBitvectorTheory.ReplaceIntegerEncodingOptions;
import org.sosy_lab.cpachecker.util.predicates.smt.WrappingFormula.WrappingArrayFormula;
import org.sosy_lab.cpachecker.util.predicates.smt.WrappingFormula.WrappingBitvectorFormula;
import org.sosy_lab.cpachecker.util.predicates.smt.WrappingFormula.WrappingFloatingPointFormula;
import org.sosy_lab.cpachecker.util.predicates.smt.WrappingFormula.WrappingIntegerFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaManager;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.FormulaType.ArrayFormulaType;
import org.sosy_lab.java_smt.api.FormulaType.BitvectorType;
import org.sosy_lab.java_smt.api.FormulaType.FloatingPointType;
import org.sosy_lab.java_smt.api.FormulaType.NumeralType;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;

/**
 * Class that takes care of all the (un-)wrapping of formulas and types depending on the configured
 * theory replacement.
 */
final class FormulaWrappingHandler {

  private final Theory encodeBitvectorAs;
  private final Theory encodeFloatAs;
  private final Theory encodeIntegerAs;
  private final ReplaceIntegerEncodingOptions intOptions;

  private final FormulaManager manager;

  FormulaWrappingHandler(
      FormulaManager pRawManager,
      Theory pEncodeBitvectorAs,
      Theory pEncodeFloatAs,
      Theory pEncodeIntegerAs,
      ReplaceIntegerEncodingOptions pIntOptions) {
    manager = checkNotNull(pRawManager);
    encodeBitvectorAs = checkNotNull(pEncodeBitvectorAs);
    encodeFloatAs = checkNotNull(pEncodeFloatAs);
    encodeIntegerAs = checkNotNull(pEncodeIntegerAs);
    intOptions = checkNotNull(pIntOptions);
    assert encodeBitvectorAs != Theory.FLOAT : "can not encode bitvectors as floats";
    assert encodeFloatAs != Theory.BITVECTOR : "can not encode floats as bitvectors";
  }

  boolean useBitvectors() {
    return encodeBitvectorAs == Theory.BITVECTOR;
  }

  @SuppressWarnings("unchecked")
  <T extends Formula> FormulaType<T> getFormulaType(T pFormula) {
    checkNotNull(pFormula);

    if (pFormula instanceof WrappingFormula<?, ?>) {
      WrappingFormula<?, ?> castFormula = (WrappingFormula<?, ?>) pFormula;
      return (FormulaType<T>) castFormula.getType();
    } else {
      return getRawFormulaType(pFormula);
    }
  }

  <T extends Formula> FormulaType<T> getRawFormulaType(T pFormula) {
    assert !(pFormula instanceof WrappingFormula);
    return manager.getFormulaType(pFormula);
  }

  @SuppressWarnings("unchecked")
  <T1 extends Formula, T2 extends Formula> T1 wrap(FormulaType<T1> targetType, T2 toWrap) {
    if (toWrap instanceof WrappingFormula<?, ?>) {
      throw new IllegalArgumentException(
          String.format(
              "Cannot double-wrap a formula %s, which has already been wrapped as %s, as %s.",
              toWrap, ((WrappingFormula<?, ?>) toWrap).getType(), targetType));
    }

    if (targetType.isIntegerType() && (encodeIntegerAs != Theory.INTEGER)) {
      return (T1) new WrappingIntegerFormula<>((NumeralType<IntegerFormula>) targetType, toWrap);

    } else if (targetType.isBitvectorType() && (encodeBitvectorAs != Theory.BITVECTOR)) {
      return (T1) new WrappingBitvectorFormula<>((BitvectorType) targetType, toWrap);

    } else if (targetType.isFloatingPointType() && (encodeFloatAs != Theory.FLOAT)) {
      return (T1) new WrappingFloatingPointFormula<>((FloatingPointType) targetType, toWrap);

    } else if (targetType.isArrayType()) {
      final ArrayFormulaType<?, ?> targetArrayType = (ArrayFormulaType<?, ?>) targetType;
      //      final FormulaType<? extends Formula> targetIndexType = targetArrayType.getIndexType();
      //      final FormulaType<? extends Formula> targetElementType =
      // targetArrayType.getElementType();
      return (T1) new WrappingArrayFormula<>(targetArrayType, toWrap);

    } else if (targetType.equals(manager.getFormulaType(toWrap))) {
      return (T1) toWrap;

    } else {
      throw new IllegalArgumentException(
          String.format("Cannot wrap formula %s as %s", toWrap, targetType));
    }
  }

  <T extends Formula> Formula unwrap(T f) {
    if (f instanceof WrappingFormula<?, ?>) {
      return ((WrappingFormula<?, ?>) f).getWrapped();
    } else {
      return f;
    }
  }

  List<Formula> unwrap(List<? extends Formula> f) {
    return Lists.transform(f, pInput -> unwrap(pInput));
  }

  FormulaType<?> unwrapType(FormulaType<?> type) {
    if (type.isArrayType()) {
      ArrayFormulaType<?, ?> arrayType = (ArrayFormulaType<?, ?>) type;
      return FormulaType.getArrayType(
          unwrapType(arrayType.getIndexType()), unwrapType(arrayType.getElementType()));
    }

    if (type.isIntegerType()) {
      switch (encodeIntegerAs) {
        case BITVECTOR:
          return FormulaType.getBitvectorTypeWithSize(intOptions.getBitsize());
        case INTEGER:
          return FormulaType.IntegerType;
        case RATIONAL:
          return FormulaType.RationalType;
        default:
          throw new AssertionError("unexpected encoding for integers: " + type);
      }
    }

    if (type.isBitvectorType()) {
      switch (encodeBitvectorAs) {
        case BITVECTOR:
          return type;
        case INTEGER:
          return FormulaType.IntegerType;
        case RATIONAL:
          return FormulaType.RationalType;
        default:
          throw new AssertionError("unexpected encoding for bitvectors: " + type);
      }
    }

    if (type.isFloatingPointType()) {
      switch (encodeFloatAs) {
        case FLOAT:
          return type;
        case INTEGER:
          return FormulaType.IntegerType;
        case RATIONAL:
          return FormulaType.RationalType;
        default:
          throw new AssertionError("unexpected encoding for floats: " + type);
      }
    }

    return type;
  }

  List<FormulaType<?>> unwrapType(List<? extends FormulaType<?>> pTypes) {
    return Lists.transform(pTypes, pInput -> unwrapType(pInput));
  }
}
