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
package org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl;

import org.sosy_lab.cpachecker.util.predicates.interfaces.ArrayFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ArrayFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;

public abstract class AbstractArrayFormulaManager<TFormulaInfo, TType, TEnv>
  extends AbstractBaseFormulaManager<TFormulaInfo, TType, TEnv>
  implements ArrayFormulaManager {

  public AbstractArrayFormulaManager(FormulaCreator<TFormulaInfo, TType, TEnv> pFormulaCreator) {
    super(pFormulaCreator);
  }

  protected <TI extends Formula, TE extends Formula> ArrayFormula<TI, TE> wrap(TFormulaInfo pTerm, FormulaType<TI> pIndexType, FormulaType<TE> pElementType) {
    return getFormulaCreator().encapsulateArray(pTerm, pIndexType, pElementType);
  }

  @Override
  public <TI extends Formula, TE extends Formula> ArrayFormula<TI, TE> select(ArrayFormula<TI, TE> pArray, Formula pIndex) {
    return wrap(select(
        extractInfo(pArray),
        extractInfo(pIndex)),
      pArray.getIndexType(),
      pArray.getElementType());
  }
  protected abstract TFormulaInfo select(TFormulaInfo pArray, TFormulaInfo pIndex);

  @Override
  public <TI extends Formula, TE extends Formula> ArrayFormula<TI, TE> store(ArrayFormula<TI, TE> pArray, Formula pIndex, Formula pValue) {
    return wrap(store(
        extractInfo(pArray),
        extractInfo(pIndex),
        extractInfo(pValue)),
      pArray.getIndexType(),
      pArray.getElementType());
  }
  protected abstract TFormulaInfo store(TFormulaInfo pArray, TFormulaInfo pIndex, TFormulaInfo pValue);

  @Override
  public <TI extends Formula, TE extends Formula> ArrayFormula<TI, TE> makeArray(String pName, FormulaType<TI> pIndexType, FormulaType<TE> pElementType) {
    return wrap(
        internalMakeArray(
            pName,
            pIndexType,
            pElementType),
        pIndexType,
        pElementType);
  }
  protected abstract <TI extends Formula, TE extends Formula> TFormulaInfo internalMakeArray(String pName, FormulaType<TI> pIndexType, FormulaType<TE> pElementType);


}
