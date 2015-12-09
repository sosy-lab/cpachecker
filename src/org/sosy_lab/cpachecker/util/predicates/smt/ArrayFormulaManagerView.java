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

import java.util.Arrays;
import java.util.List;

import org.sosy_lab.solver.api.ArrayFormula;
import org.sosy_lab.solver.api.ArrayFormulaManager;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.Formula;
import org.sosy_lab.solver.api.FormulaType;
import org.sosy_lab.solver.api.FormulaType.ArrayFormulaType;
import org.sosy_lab.solver.api.NumeralFormulaManager;

/**
 * Implements some methods for easier interaction with the formula manager for
 * array formulas.
 */
public class ArrayFormulaManagerView extends BaseManagerView implements ArrayFormulaManager {

  private ArrayFormulaManager manager;

  /**
   * Creates the new formula manager view for arrays.
   *
   * @param pWrappingHandler A handler for wrapping and unwrapping of formulae.
   * @param pManager The formula manager capable of the SMT theory of arrays.
   */
  ArrayFormulaManagerView(
      final FormulaWrappingHandler pWrappingHandler, final ArrayFormulaManager pManager) {
    super(pWrappingHandler);
    this.manager = pManager;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <TI extends Formula, TE extends Formula> TE select(
      final ArrayFormula<TI, TE> pArray, final Formula pIndex) {

    @SuppressWarnings("unchecked")
    final ArrayFormula<TI, TE> declaredArray = (ArrayFormula<TI, TE>) unwrap(pArray);
    final TE selectResult = manager.select(declaredArray, unwrap(pIndex));
    final FormulaType<TE> resultType = getElementType(pArray);

    // the result of a select can also be a reference to an array!
    // (multi-dimensional arrays)
    // example: returns an array
    return wrap(resultType, selectResult);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <TI extends Formula, TE extends Formula> ArrayFormula<TI, TE> store(
      final ArrayFormula<TI, TE> pArray, final Formula pIndex, final Formula pValue) {

    @SuppressWarnings("unchecked")
    final ArrayFormula<TI, TE> declaredArray = (ArrayFormula<TI, TE>) unwrap(pArray);

    return manager.store(declaredArray, unwrap(pIndex), unwrap(pValue));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <TI extends Formula, TE extends Formula, FTI extends FormulaType<TI>,
          FTE extends FormulaType<TE>>
      ArrayFormula<TI, TE> makeArray(
          final String pName, final FTI pIndexType, final FTE pElementType) {

    final ArrayFormulaType<TI, TE> inputArrayType =
        new ArrayFormulaType<>(pIndexType, pElementType);

    @SuppressWarnings("unchecked")
    final FTI unwrappedIndexType = (FTI) unwrapType(pIndexType);
    @SuppressWarnings("unchecked")
    final FTE unwrappedElementType = (FTE) unwrapType(pElementType);

    final ArrayFormula<TI, TE> result =
        manager.makeArray(pName, unwrappedIndexType, unwrappedElementType);

    return wrap(inputArrayType, result);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <TI extends Formula> FormulaType<TI> getIndexType(final ArrayFormula<TI, ?> pArray) {
    if (pArray instanceof WrappingFormula<?,?>) {
      @SuppressWarnings("unchecked")
      ArrayFormulaType<TI, ?> t =
          (ArrayFormulaType<TI, ?>) ((WrappingFormula<?, ?>) pArray).getType();
      return t.getIndexType();
    }
    return manager.getIndexType(pArray);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <TE extends Formula> FormulaType<TE> getElementType(final ArrayFormula<?, TE> pArray) {
    if (pArray instanceof WrappingFormula<?,?>) {
      @SuppressWarnings("unchecked")
      ArrayFormulaType<?, TE> t =
          (ArrayFormulaType<?, TE>) ((WrappingFormula<?, ?>) pArray).getType();
      return t.getElementType();
    }
    return manager.getElementType(pArray);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <TI extends Formula, TE extends Formula> BooleanFormula equivalence(
      final ArrayFormula<TI, TE> pArray1, final ArrayFormula<TI, TE> pArray2) {

    @SuppressWarnings("unchecked")
    final ArrayFormula<TI, TE> declaredArray1 = (ArrayFormula<TI, TE>) unwrap(pArray1);
    @SuppressWarnings("unchecked")
    final ArrayFormula<TI, TE> declaredArray2 = (ArrayFormula<TI, TE>) unwrap(pArray2);

    BooleanFormula result = manager.equivalence(declaredArray1, declaredArray2);
    return wrap(FormulaType.BooleanType, result);
  }

  /**
   * Returns a formula representing the declaration of an SMT array and a call
   * to it.
   *
   * @param pName The name of the array variable.
   * @param pIntegerFormulaManager The formula manager for integer formulae.
   * @param pReturnType The return type of the array call, i.e. the element type
   *                    of the array.
   * @param pArgs A var-args list of formulae that get stored in the array.
   * @param <T> The formula type of the elements of the array.
   * @return A formula for the array and the call to it.
   */
  public <T extends Formula> T declareAndCallArray(
      final String pName,
      final NumeralFormulaManager<?, ?> pIntegerFormulaManager,
      final FormulaType<T> pReturnType,
      final Formula... pArgs) {
    return declareAndCallArray(pName, pIntegerFormulaManager, pReturnType,
        Arrays.asList(pArgs));
  }

  /**
   * Returns a formula representing the declaration of an SMT array and a call
   * to it.
   *
   * @param pName The name of the array variable.
   * @param pIdx An additional index.
   * @param pIntegerFormulaManager The formula manager for integer formulae.
   * @param pReturnType The return type of the array call, i.e. the element type
   *                    of the array.
   * @param pArgs A var-args list of formulae that get stored in the array.
   * @param <T> The formula type of the elements of the array.
   * @return A formula for the array and the call to it.
   */
  public <T extends Formula> T declareAndCallArray(
      final String pName,
      final int pIdx,
      final NumeralFormulaManager<?, ?> pIntegerFormulaManager,
      final FormulaType<T> pReturnType,
      final Formula... pArgs) {
    return declareAndCallArray(pName, pIdx, pIntegerFormulaManager, pReturnType,
        Arrays.asList(pArgs));
  }

  /**
   * Returns a formula representing the declaration of an SMT array and a call
   * to it.
   *
   * @param pName The name of the array variable.
   * @param pIdx An additional index.
   * @param pIntegerFormulaManager The formula manager for integer formulae.
   * @param pReturnType The return type of the array call, i.e. the element type
   *                    of the array.
   * @param pArgs A list of formulae that get stored in the array.
   * @param <T> The formula type of the elements of the array.
   * @return A formula for the array and the call to it.
   */
  private <T extends Formula> T declareAndCallArray(
      final String pName,
      final int pIdx,
      final NumeralFormulaManager<?, ?> pIntegerFormulaManager,
      final FormulaType<T> pReturnType,
      final List<Formula> pArgs) {
    String name = FormulaManagerView.makeName(pName, pIdx);
    return declareAndCallArray(name, pIntegerFormulaManager, pReturnType, pArgs);
  }

  /**
   * Returns a formula representing the declaration of an SMT array and a call
   * to it.
   *
   * @param pName The name of the array variable.
   * @param pIntegerFormulaManager The formula manager for integer formulae.
   * @param pReturnType The return type of the array call, i.e. the element type
   *                    of the array.
   * @param pArgs A list of formulae that get stored in the array.
   * @param <T> The formula type of the elements of the array.
   * @return A formula for the array and the call to it.
   */
  private <T extends Formula> T declareAndCallArray(
      final String pName,
      final NumeralFormulaManager<?, ?> pIntegerFormulaManager,
      final FormulaType<T> pReturnType,
      final List<Formula> pArgs) {
    // todo does the magic...
    ArrayFormula<?, ?> arrayFormula = makeArray(pName,
        FormulaType.IntegerType, pReturnType);

    int i;
    for (i = 0; i < pArgs.size(); ++i) {
      Formula index = pIntegerFormulaManager.makeNumber(i);
      arrayFormula = store(arrayFormula, index, pArgs.get(i));
    }

    return wrap(pReturnType, select(arrayFormula, pIntegerFormulaManager.makeNumber(i - 1)));
  }
}
