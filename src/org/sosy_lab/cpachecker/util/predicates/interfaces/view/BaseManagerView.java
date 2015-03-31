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
package org.sosy_lab.cpachecker.util.predicates.interfaces.view;

import java.util.List;

import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

abstract class BaseManagerView {

  private final FormulaManagerView baseManager;

  BaseManagerView(FormulaManagerView pViewManager) {
    baseManager = pViewManager;
  }

  final <T extends Formula> FormulaType<T> getFormulaType(T pFormula) {
    return baseManager.getFormulaType(pFormula);
  }

  final <T1 extends Formula, T2 extends Formula> T1 wrap(FormulaType<T1> targetType, T2 toWrap) {
    return baseManager.wrap(targetType, toWrap);
  }

  final Formula unwrap(Formula f) {
    return baseManager.unwrap(f);
  }

  final List<Formula> unwrap(List<? extends Formula> f) {
    return Lists.transform(f, new Function<Formula, Formula>() {
      @Override
      public Formula apply(Formula pInput) {
        return unwrap(pInput);
      }
    });
  }

  final FormulaType<?> unwrapType(FormulaType<?> pType) {
    return baseManager.unwrapType(pType);
  }

  final List<FormulaType<?>> unwrapType(List<? extends FormulaType<?>> pTypes) {
    return Lists.transform(pTypes, new Function<FormulaType<?>, FormulaType<?>>() {
          @Override
          public FormulaType<?> apply(FormulaType<?> pInput) {
            return unwrapType(pInput);
          }
        });
  }
}
