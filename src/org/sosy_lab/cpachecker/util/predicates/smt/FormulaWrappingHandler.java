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
package org.sosy_lab.cpachecker.util.predicates.smt;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.Lists;

import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView.Theory;
import org.sosy_lab.cpachecker.util.predicates.smt.WrappingFormula.WrappingArrayFormula;
import org.sosy_lab.cpachecker.util.predicates.smt.WrappingFormula.WrappingBitvectorFormula;
import org.sosy_lab.cpachecker.util.predicates.smt.WrappingFormula.WrappingFloatingPointFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaManager;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.FormulaType.ArrayFormulaType;
import org.sosy_lab.java_smt.api.FormulaType.BitvectorType;
import org.sosy_lab.java_smt.api.FormulaType.FloatingPointType;

import java.util.List;

/**
 * Class that takes care of all the (un-)wrapping of formulas and types
 * depending on the configured theory replacement.
 */
final class FormulaWrappingHandler {

  private final Theory encodeBitvectorAs;
  private final Theory encodeFloatAs;

  private final FormulaManager manager;

  FormulaWrappingHandler(FormulaManager pRawManager,
      Theory pEncodeBitvectorAs, Theory pEncodeFloatAs) {
    manager = checkNotNull(pRawManager);
    encodeBitvectorAs = checkNotNull(pEncodeBitvectorAs);
    encodeFloatAs = checkNotNull(pEncodeFloatAs);
    assert encodeBitvectorAs != Theory.FLOAT;
    assert encodeFloatAs != Theory.BITVECTOR;
  }

  @SuppressWarnings("unchecked")
  <T extends Formula> FormulaType<T> getFormulaType(T pFormula) {
    checkNotNull(pFormula);

    if (pFormula instanceof WrappingFormula<?, ?>) {
      WrappingFormula<?, ?> castFormula = (WrappingFormula<?, ?>)pFormula;
      return (FormulaType<T>)castFormula.getType();
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
      throw new IllegalArgumentException(String.format(
          "Cannot double-wrap a formula %s, which has already been wrapped as %s, as %s.",
          toWrap, ((WrappingFormula<?, ?>)toWrap).getType(), targetType));
    }

    if (targetType.isBitvectorType() && (encodeBitvectorAs != Theory.BITVECTOR)) {
      return (T1) new WrappingBitvectorFormula<>((BitvectorType)targetType, toWrap);

    } else if (targetType.isFloatingPointType() && (encodeFloatAs != Theory.FLOAT)) {
      return (T1) new WrappingFloatingPointFormula<>((FloatingPointType)targetType, toWrap);

    } else if (targetType.isArrayType()) {
      final ArrayFormulaType<?, ?> targetArrayType = (ArrayFormulaType<?, ?>) targetType;
//      final FormulaType<? extends Formula> targetIndexType = targetArrayType.getIndexType();
//      final FormulaType<? extends Formula> targetElementType = targetArrayType.getElementType();
      return (T1) new WrappingArrayFormula<>(targetArrayType, toWrap);

    } else if (targetType.equals(manager.getFormulaType(toWrap))) {
      return (T1) toWrap;

    } else {
      throw new IllegalArgumentException(String.format(
          "Cannot wrap formula %s as %s", toWrap, targetType));
    }
  }

  <T extends Formula> Formula unwrap(T f) {
    if (f instanceof WrappingFormula<?, ?>) {
      return ((WrappingFormula<?, ?>)f).getWrapped();
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
          unwrapType(arrayType.getIndexType()),
          unwrapType(arrayType.getElementType()));
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
        throw new AssertionError();
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
        throw new AssertionError();
      }
    }

    return type;
  }

  final List<FormulaType<?>> unwrapType(List<? extends FormulaType<?>> pTypes) {
    return Lists.transform(pTypes, pInput -> unwrapType(pInput));
  }
}
