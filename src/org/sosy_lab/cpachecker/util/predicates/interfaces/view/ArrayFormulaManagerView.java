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
   * to wrap the array domain (the index), and its range (the values).
   *
   * This information is needed when accessing the array:
   *    The result (of a 'select') has to be wrapped with the intended type again.
   */
  private static class BoxedArrayFormula<TD extends Formula, TR extends Formula> implements ArrayFormula<TD, TR> {

    public final FormulaType<TD> wrappingDomainType;
    public final FormulaType<TR> wrappingRangeType;
    public final ArrayFormula<?, ?> formula;

    public BoxedArrayFormula(
        ArrayFormula<?, ?> pFormula,
        FormulaType<TD> pWrappingDomainType, FormulaType<TR> pWrappingRangeType
        ) {
      wrappingDomainType = pWrappingDomainType;
      wrappingRangeType = pWrappingRangeType;
      formula = pFormula;
    }

  }

  ArrayFormulaManagerView(FormulaManagerView pViewManager, ArrayFormulaManager pManager) {
    super(pViewManager);
    this.manager = pManager;
  }

  @Override
  public <TD extends Formula, TR extends Formula> TR select (
      ArrayFormula<TD, TR> pArray, Formula pIndex) {

    final ArrayFormula<TD, TR> declaredArray = unbox(pArray);
    final TR selectResult = manager.select(declaredArray, unwrap(pIndex));
    final FormulaType<TR> resultType = getRangeType(pArray);

    return wrap(resultType, selectResult);
  }

  @Override
  public <TD extends Formula, TR extends Formula> ArrayFormula<TD, TR> store (
      ArrayFormula<TD, TR> pArray, Formula pIndex, Formula pValue) {

    final ArrayFormula<TD, TR> declaredArray = unbox(pArray);

    return manager.store(declaredArray, unwrap(pIndex), unwrap(pValue));
  }

  @SuppressWarnings("unchecked")
  @Override
  public <TD extends Formula, TR extends Formula, FTD extends FormulaType<TD>, FTR extends FormulaType<TR>> ArrayFormula<TD, TR> makeArray(
      String pName, FTD pIndexType, FTR pElementType) {

    final FTD unwrappedDomainType = (FTD) unwrapType(pIndexType);
    final FTR unwrappedRangeType = (FTR) unwrapType(pElementType);

    final ArrayFormula<TD, TR> resultWithUnwrappedTypes = manager.makeArray(pName, unwrappedDomainType, unwrappedRangeType);

    return new BoxedArrayFormula<>(resultWithUnwrappedTypes, pIndexType, pElementType);
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  public <TD extends Formula, TR extends Formula> ArrayFormula<TD, TR> unbox (
      ArrayFormula<TD, TR> pArray) {
    if (pArray instanceof BoxedArrayFormula) {
      return ((BoxedArrayFormula) pArray).formula;
    }

    return pArray;
  }


  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Override
  public <TD extends Formula, FTD extends FormulaType<TD>> FTD getDomainType(ArrayFormula<TD, ?> pArray) {
    if (pArray instanceof BoxedArrayFormula) {
      return (FTD) ((BoxedArrayFormula<TD, ?>) pArray).wrappingDomainType;
    }
    return manager.getDomainType(pArray);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <TR extends Formula, FTR extends FormulaType<TR>> FTR getRangeType(ArrayFormula<?, TR> pArray) {
    if (pArray instanceof BoxedArrayFormula) {
      return (FTR) ((BoxedArrayFormula<?, TR>) pArray).wrappingRangeType;
    }
    return manager.getRangeType(pArray);
  }

}
