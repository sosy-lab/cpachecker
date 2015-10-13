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
package org.sosy_lab.solver.api;


/**
 * This interface represents the theory of (arbitrarily nested) arrays.
 * (as defined in the SMTLib2 standard)
 */
public interface ArrayFormulaManager {

  /**
   * Read a value that is stored in the array at the specified position.
   *
   * @param pArray    The array from which to read
   * @param pIndex    The position from which to read
   * @return          A formula that represents the result of the "read"
   */
  public <TI extends Formula, TE extends Formula> TE
    select (ArrayFormula<TI, TE> pArray, Formula pIndex);

  /**
   * Store a value into a cell of the specified array.
   *
   * @param pArray    The array to which to write
   * @param pIndex    The position to which to write
   * @param pValue    The value that should be written
   * @return          A formula that represents the "write"
   */
  public <TI extends Formula, TE extends Formula> ArrayFormula<TI, TE>
    store (ArrayFormula<TI, TE> pArray, Formula pIndex, Formula pValue);

  /**
   * Declare a new array.
   *
   * @param pName         The name of the array variable
   * @param pIndexType    The type of the array index
   * @param pElementType  The type of the array elements
   * @return              Formula that represents the array
   */
  public <TI extends Formula, TE extends Formula,
    FTI extends FormulaType<TI>, FTE extends FormulaType<TE>>
    ArrayFormula<TI, TE> makeArray(String pName, FTI pIndexType, FTE pElementType);

  /**
   * Make a {@link BooleanFormula} that represents the equality of
   * two {@link ArrayFormula}.
   */
  public <TI extends Formula, TE extends Formula> BooleanFormula equivalence(
      ArrayFormula<TI, TE> pArray1, ArrayFormula<TI, TE> pArray2);

  public <TI extends Formula> FormulaType<TI> getIndexType(ArrayFormula<TI, ?> pArray);
  public <TE extends Formula> FormulaType<TE> getElementType(ArrayFormula<?, TE> pArray);

}

