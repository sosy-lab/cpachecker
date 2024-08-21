// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.smt;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;

/**
 * Abstract helper class that does nothing more than providing access to the methods from {@link
 * FormulaWrappingHandler} with less typing.
 */
abstract class BaseManagerView {

  private final FormulaWrappingHandler wrappingHandler;

  BaseManagerView(FormulaWrappingHandler pWrappingHandler) {
    wrappingHandler = checkNotNull(pWrappingHandler);
  }

  final boolean useBitvectors() {
    return wrappingHandler.useBitvectors();
  }

  final <T extends Formula> FormulaType<T> getFormulaType(T pFormula) {
    return wrappingHandler.getFormulaType(pFormula);
  }

  final <T1 extends Formula, T2 extends Formula> T1 wrap(FormulaType<T1> targetType, T2 toWrap) {
    return wrappingHandler.wrap(targetType, toWrap);
  }

  final Formula unwrap(Formula f) {
    return wrappingHandler.unwrap(f);
  }

  final List<Formula> unwrap(List<? extends Formula> f) {
    return wrappingHandler.unwrap(f);
  }

  final FormulaType<?> unwrapType(FormulaType<?> pType) {
    return wrappingHandler.unwrapType(pType);
  }

  final List<FormulaType<?>> unwrapType(List<? extends FormulaType<?>> pTypes) {
    return wrappingHandler.unwrapType(pTypes);
  }
}
