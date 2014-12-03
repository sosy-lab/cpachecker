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
import org.sosy_lab.cpachecker.util.predicates.interfaces.ArrayFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;

public class ArrayFormulaManagerView
  extends BaseManagerView
  implements ArrayFormulaManager {

  private ArrayFormulaManager manager;

  /**
   * Used to keep track of the types that were used
   * to wrap the array index (the domain), and its values (the range).
   *
   * This information is needed when accessing the array:
   *    The result (of a 'select') has to be wrapped with the intended type again.
   */
  private static class UnwrappedArrayFormula<TI extends Formula, TE extends Formula> implements ArrayFormula<TI, TE> {

    public final FormulaType<TI> indexTypeWasWrappedAs;
    public final FormulaType<TE> elementTypeWasWrappedAs;
    public final ArrayFormula<?, ?> formula;

    public UnwrappedArrayFormula(
        ArrayFormula<?, ?> pFormula,
        FormulaType<TI> pIndexWasWrappedAs,
        FormulaType<TE> pElementTypeWasWrappedAs) {
      indexTypeWasWrappedAs = pIndexWasWrappedAs;
      elementTypeWasWrappedAs = pElementTypeWasWrappedAs;
      formula = pFormula;
    }

  }

  ArrayFormulaManagerView(FormulaManagerView pViewManager, ArrayFormulaManager pManager) {
    super(pViewManager);
    this.manager = pManager;
  }

  @Override
  public <TI extends Formula, TE extends Formula> TE select (
      ArrayFormula<TI, TE> pArray, Formula pIndex) {

    final ArrayFormula<TI, TE> declaredArray = rewrapIfNeeded(pArray);
    final TE selectResult = manager.select(declaredArray, unwrap(pIndex));
    final FormulaType<TE> resultType = getElementType(pArray);

    return wrap(resultType, selectResult);
  }

  @Override
  public <TI extends Formula, TE extends Formula> ArrayFormula<TI, TE> store (
      ArrayFormula<TI, TE> pArray, Formula pIndex, Formula pValue) {

    final ArrayFormula<TI, TE> declaredArray = rewrapIfNeeded(pArray);

    return manager.store(declaredArray, unwrap(pIndex), unwrap(pValue));
  }

  @SuppressWarnings("unchecked")
  @Override
  public <TI extends Formula, TE extends Formula, FTI extends FormulaType<TI>, FTE extends FormulaType<TE>> ArrayFormula<TI, TE> makeArray(
      String pName, FTI pIndexType, FTE pElementType) {

    final FTI unwrappedIndexType = (FTI) unwrapType(pIndexType);
    final FTE unwrappedElementType = (FTE) unwrapType(pElementType);

    final ArrayFormula<TI, TE> resultWithUnwrappedTypes = manager.makeArray(pName, unwrappedIndexType, unwrappedElementType);

    return new UnwrappedArrayFormula<>(resultWithUnwrappedTypes, pIndexType, pElementType);
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public <TI extends Formula, TE extends Formula> ArrayFormula<TI, TE> rewrapIfNeeded (
      ArrayFormula<TI, TE> pArray) {
    if (pArray instanceof UnwrappedArrayFormula) {
      return ((UnwrappedArrayFormula) pArray).formula;
    }

    return pArray;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <TD extends Formula, FTI extends FormulaType<TD>> FTI getIndexType(ArrayFormula<TD, ?> pArray) {
    if (pArray instanceof UnwrappedArrayFormula) {
      return (FTI) ((UnwrappedArrayFormula<TD, ?>) pArray).indexTypeWasWrappedAs;
    }
    return manager.getIndexType(pArray);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <TR extends Formula, FTE extends FormulaType<TR>> FTE getElementType(ArrayFormula<?, TR> pArray) {
    if (pArray instanceof UnwrappedArrayFormula) {
      return (FTE) ((UnwrappedArrayFormula<?, TR>) pArray).elementTypeWasWrappedAs;
    }
    return manager.getElementType(pArray);
  }

}
