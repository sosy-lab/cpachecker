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

@SuppressWarnings("unchecked")
public abstract class AbstractArrayFormulaManager<TFormulaInfo, TType, TEnv>
  extends AbstractBaseFormulaManager<TFormulaInfo, TType, TEnv>
  implements ArrayFormulaManager {

  public AbstractArrayFormulaManager(FormulaCreator<TFormulaInfo, TType, TEnv> pFormulaCreator) {
    super(pFormulaCreator);
  }

  @Override
  public <TD extends Formula, TR extends Formula> TR select (
      ArrayFormula<TD, TR> pArray, Formula pIndex) {

    final FormulaType<? extends Formula> elementType = getFormulaCreator().getArrayFormulaElementType(pArray);
    final TFormulaInfo term = select(
        extractInfo(pArray),
        extractInfo(pIndex));

    return (TR) getFormulaCreator().encapsulate(elementType, term);
  }
  protected abstract TFormulaInfo select(TFormulaInfo pArray, TFormulaInfo pIndex);

  @Override
  public <TD extends Formula, TR extends Formula> ArrayFormula<TD, TR> store (
      ArrayFormula<TD, TR> pArray, Formula pIndex, Formula pValue) {

    final FormulaType<TD> indexType = getFormulaCreator().getArrayFormulaIndexType(pArray);
    final FormulaType<TR> elementType = getFormulaCreator().getArrayFormulaElementType(pArray);

    final TFormulaInfo term = store(
        extractInfo(pArray),
        extractInfo(pIndex),
        extractInfo(pValue));
    return getFormulaCreator().encapsulateArray(term, indexType, elementType);
  }
  protected abstract TFormulaInfo store(TFormulaInfo pArray, TFormulaInfo pIndex, TFormulaInfo pValue);

  @Override
  public <TD extends Formula, TR extends Formula, FTD extends FormulaType<TD>, FTR extends FormulaType<TR>> ArrayFormula<TD, TR> makeArray(
      String pName, FTD pIndexType, FTR pElementType) {

    final TFormulaInfo namedArrayFormula = internalMakeArray(pName, pIndexType, pElementType);
    return getFormulaCreator().encapsulateArray(namedArrayFormula, pIndexType, pElementType);
  }

  protected abstract <TI extends Formula, TE extends Formula> TFormulaInfo internalMakeArray(
      String pName, FormulaType<TI> pIndexType, FormulaType<TE> pElementType);

  @Override
  public <TD extends Formula, FTD extends FormulaType<TD>> FTD getDomainType(ArrayFormula<TD, ?> pArray) {
    return (FTD) getFormulaCreator().getArrayFormulaIndexType(pArray);
  }

  @Override
  public <TR extends Formula, FTR extends FormulaType<TR>> FTR getRangeType(ArrayFormula<?, TR> pArray) {
    return (FTR) getFormulaCreator().getArrayFormulaElementType(pArray);
  }

}
