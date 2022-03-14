// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.smt;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.errorprone.annotations.Immutable;
import org.sosy_lab.java_smt.api.ArrayFormula;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.FloatingPointFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;

@Immutable
abstract class WrappingFormula<TWrap extends Formula, TOut extends Formula> {

  private final TWrap wrapped;
  private final FormulaType<TOut> type;

  private WrappingFormula(FormulaType<TOut> pType, TWrap pWrapped) {
    wrapped = checkNotNull(pWrapped);
    type = checkNotNull(pType);
  }

  final TWrap getWrapped() {
    return wrapped;
  }

  final FormulaType<TOut> getType() {
    return type;
  }

  @Override
  public final String toString() {
    return wrapped.toString();
  }

  @Override
  public final int hashCode() {
    final int prime = 31;
    return (prime + type.hashCode()) * prime + wrapped.hashCode();
  }

  @Override
  @SuppressWarnings("EqualsGetClass") // on purpose, case-class structure with single equals()
  public final boolean equals(Object pObj) {
    if ((pObj == null) || !getClass().equals(pObj.getClass())) {
      return false;
    }

    WrappingFormula<?, ?> other = (WrappingFormula<?, ?>) pObj;

    return wrapped.equals(other.wrapped) && type.equals(other.type);
  }

  @Immutable
  static final class WrappingIntegerFormula<TWrap extends Formula>
      extends WrappingFormula<TWrap, IntegerFormula> implements IntegerFormula {

    WrappingIntegerFormula(FormulaType<IntegerFormula> type, TWrap pToWrap) {
      super(type, pToWrap);
    }
  }

  @Immutable
  static final class WrappingBitvectorFormula<TWrap extends Formula>
      extends WrappingFormula<TWrap, BitvectorFormula> implements BitvectorFormula {

    WrappingBitvectorFormula(FormulaType<BitvectorFormula> type, TWrap pToWrap) {
      super(type, pToWrap);
    }
  }

  @Immutable
  static final class WrappingFloatingPointFormula<TWrap extends Formula>
      extends WrappingFormula<TWrap, FloatingPointFormula> implements FloatingPointFormula {

    WrappingFloatingPointFormula(FormulaType<FloatingPointFormula> type, TWrap pToWrap) {
      super(type, pToWrap);
    }
  }

  @Immutable
  static final class WrappingArrayFormula<
          TWrap extends Formula, TI extends Formula, TE extends Formula>
      extends WrappingFormula<TWrap, ArrayFormula<TI, TE>> implements ArrayFormula<TI, TE> {

    WrappingArrayFormula(FormulaType<ArrayFormula<TI, TE>> type, TWrap pToWrap) {
      super(type, pToWrap);
    }
  }
}
