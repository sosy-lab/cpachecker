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

import org.sosy_lab.cpachecker.util.predicates.interfaces.ArrayFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;

/**
 * Stores a history of the wrapping process so that
 * it can be re-wrapped with the same type(s) later.
 */
public abstract class UnWrappedFormula<TUnwrapped extends Formula> implements Formula {

  private final TUnwrapped unwrapped;

  public UnWrappedFormula(TUnwrapped pUnwrapped) {
    unwrapped = pUnwrapped;
  }

  public TUnwrapped getUnwrapped() {
    return unwrapped;
  }

  /**
   * Used to keep track of the types that were used
   * to wrap the array index (the domain), and its values (the range).
   *
   * This information is needed when accessing the array:
   *    The result (of a 'select') has to be wrapped with the intended type again.
   */
  public static class UnwrappedArrayFormula
  <TI extends Formula, TE extends Formula, TIU extends Formula, TEU extends Formula>
  extends UnWrappedFormula<ArrayFormula<TIU, TEU>>
  implements ArrayFormula<TI, TE> {

    private final FormulaType<TI> indexTypeWasWrappedAs;
    private final FormulaType<TE> elementTypeWasWrappedAs;

    public UnwrappedArrayFormula(
        ArrayFormula<TIU, TEU> pFormula,
        FormulaType<TI> pIndexWasWrappedAs,
        FormulaType<TE> pElementTypeWasWrappedAs) {
      super(pFormula);
      indexTypeWasWrappedAs = pIndexWasWrappedAs;
      elementTypeWasWrappedAs = pElementTypeWasWrappedAs;
    }

    public FormulaType<TE> getElementTypeWasWrappedAs() {
      return elementTypeWasWrappedAs;
    }

    public FormulaType<TI> getIndexTypeWasWrappedAs() {
      return indexTypeWasWrappedAs;
    }
  }

}
