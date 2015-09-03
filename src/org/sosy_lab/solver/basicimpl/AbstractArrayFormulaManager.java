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
package org.sosy_lab.solver.basicimpl;

import org.sosy_lab.solver.api.ArrayFormula;
import org.sosy_lab.solver.api.ArrayFormulaManager;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.Formula;
import org.sosy_lab.solver.api.FormulaType;

public abstract class AbstractArrayFormulaManager<TFormulaInfo, TType, TEnv>
  extends AbstractBaseFormulaManager<TFormulaInfo, TType, TEnv>
  implements ArrayFormulaManager {

  public AbstractArrayFormulaManager(FormulaCreator<TFormulaInfo, TType, TEnv> pFormulaCreator) {
    super(pFormulaCreator);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <TI extends Formula, TE extends Formula> TE select (
      ArrayFormula<TI, TE> pArray, Formula pIndex) {

    final FormulaType<? extends Formula> elementType = getFormulaCreator().getArrayFormulaElementType(pArray);
    // Examples:
    //    Array<Bitvector,Bitvector>
    //    Rational

    final TFormulaInfo term = select(
        extractInfo(pArray),
        extractInfo(pIndex));

    return (TE) getFormulaCreator().encapsulate(elementType, term);
  }
  protected abstract TFormulaInfo select(TFormulaInfo pArray, TFormulaInfo pIndex);

  @Override
  public <TI extends Formula, TE extends Formula> ArrayFormula<TI, TE> store (
      ArrayFormula<TI, TE> pArray, Formula pIndex, Formula pValue) {

    final FormulaType<TI> indexType = getFormulaCreator().getArrayFormulaIndexType(pArray);
    final FormulaType<TE> elementType = getFormulaCreator().getArrayFormulaElementType(pArray);

    final TFormulaInfo term = store(
        extractInfo(pArray),
        extractInfo(pIndex),
        extractInfo(pValue));
    return getFormulaCreator().encapsulateArray(term, indexType, elementType);
  }
  protected abstract TFormulaInfo store(TFormulaInfo pArray, TFormulaInfo pIndex, TFormulaInfo pValue);

  @Override
  public <TI extends Formula, TE extends Formula, FTI extends FormulaType<TI>, FTE extends FormulaType<TE>> ArrayFormula<TI, TE> makeArray(
      String pName, FTI pIndexType, FTE pElementType) {

    final TFormulaInfo namedArrayFormula = internalMakeArray(pName, pIndexType, pElementType);
    return getFormulaCreator().encapsulateArray(namedArrayFormula, pIndexType, pElementType);
  }

  protected abstract <TI extends Formula, TE extends Formula> TFormulaInfo internalMakeArray(
      String pName, FormulaType<TI> pIndexType, FormulaType<TE> pElementType);

  @SuppressWarnings("unchecked")
  @Override
  public <TI extends Formula, FTI extends FormulaType<TI>> FTI getIndexType(ArrayFormula<TI, ?> pArray) {
    return (FTI) getFormulaCreator().getArrayFormulaIndexType(pArray);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <TE extends Formula, FTE extends FormulaType<TE>> FTE getElementType(ArrayFormula<?, TE> pArray) {
    return (FTE) getFormulaCreator().getArrayFormulaElementType(pArray);
  }

  @Override
  public <TI extends Formula, TE extends Formula> BooleanFormula equivalence(ArrayFormula<TI, TE> pArray1,
      ArrayFormula<TI, TE> pArray2) {
    return getFormulaCreator().encapsulateBoolean(equivalence(extractInfo(pArray1), extractInfo(pArray2)));
  }
  protected abstract TFormulaInfo equivalence(TFormulaInfo pArray1, TFormulaInfo pArray2);

}
