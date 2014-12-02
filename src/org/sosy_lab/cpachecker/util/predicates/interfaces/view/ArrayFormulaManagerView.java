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

  ArrayFormulaManagerView(FormulaManagerView pViewManager, ArrayFormulaManager pManager) {
    super(pViewManager);
    this.manager = pManager;
  }

  @Override
  public <TD extends Formula, TR extends Formula> TR select (
      ArrayFormula<TD, TR> pArray, Formula pIndex) {

    final TR selectResult = manager.select(pArray, unwrap(pIndex));
    final FormulaType<TR> resultType = getRangeType(pArray);

    return wrap(resultType, selectResult);
  }

  @Override
  public <TD extends Formula, TR extends Formula> ArrayFormula<TD, TR> store (
      ArrayFormula<TD, TR> pArray, Formula pIndex, Formula pValue) {

    return manager.store(pArray, unwrap(pIndex), unwrap(pValue));
  }

  @SuppressWarnings("unchecked")
  @Override
  public <TD extends Formula, TR extends Formula, FTD extends FormulaType<TD>, FTR extends FormulaType<TR>> ArrayFormula<TD, TR> makeArray(
      String pName, FTD pIndexType, FTR pElementType) {

    FTD uit = (FTD) unwrapType(pIndexType);
    FTR uet = (FTR) unwrapType(pElementType);

    return manager.makeArray(pName, uit, uet);
  }

  @Override
  public <TD extends Formula, FTD extends FormulaType<TD>> FTD getDomainType(ArrayFormula<TD, ?> pArray) {
    return manager.getDomainType(pArray);
  }

  @Override
  public <TR extends Formula, FTR extends FormulaType<TR>> FTR getRangeType(ArrayFormula<?, TR> pArray) {
    return manager.getRangeType(pArray);
  }

}
