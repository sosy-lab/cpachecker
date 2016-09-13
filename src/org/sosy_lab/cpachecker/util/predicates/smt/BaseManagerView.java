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
package org.sosy_lab.cpachecker.util.predicates.smt;

import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;

import java.util.List;

/**
 * Abstract helper class that does nothing more than providing access
 * to the methods from {@link FormulaWrappingHandler} with less typing.
 */
abstract class BaseManagerView {

  private final FormulaWrappingHandler wrappingHandler;

  BaseManagerView(FormulaWrappingHandler pWrappingHandler) {
    wrappingHandler = pWrappingHandler;
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
