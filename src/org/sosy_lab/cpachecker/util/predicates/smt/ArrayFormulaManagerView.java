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

import javax.annotation.Nonnull;

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

    // the result of a select can also be a reference to an array! (multi-dimensional arrays)
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
    final ArrayFormulaType<TI, TE> inputArrayType =
        new ArrayFormulaType<>(getIndexType(pArray), getElementType(pArray));

    return wrap(inputArrayType, manager.store(declaredArray, unwrap(pIndex), unwrap(pValue)));
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
   * <p>The new returned formula is of the form {@code select( store (<pName> 0 <pArgs>) 0)}.</p>
   *
   * @param pName The name of the array variable.
   * @param pIntegerFormulaManager The formula manager for integer formulae.
   * @param pReturnType The return type of the array call, i.e. the element type of the array.
   * @param pArgs The formula for the element that gets stored in the array.
   * @param <T> The formula type of the elements of the array.
   * @return A formula for the array and the call to it.
   */
  public <T extends Formula> T declareAndCallArray(
      final @Nonnull String pName,
      final @Nonnull NumeralFormulaManager<?, ?> pIntegerFormulaManager,
      final @Nonnull FormulaType<T> pReturnType,
      final @Nonnull Formula pArgs) {

    // TODO evaluate correctness of these statements. They seem to be working in most cases but
    // not when there is a cast involved, e.g. if pReturnType is a Rational type and the formula
    // in pArgs is of type Integer, the store fails (at least in SMTInterpol).
    ArrayFormula<?, ?> arrayFormula = makeArray(pName, FormulaType.IntegerType, pReturnType);
    final Formula index = pIntegerFormulaManager.makeNumber(0);
    arrayFormula = store(arrayFormula, index, pArgs);

    return wrap(pReturnType, select(arrayFormula, index));
  }

  /**
   * Returns a formula representing the declaration of an SMT array and a call
   * to it.
   *
   * @param pName The name of the array variable.
   * @param pIdx An additional index.
   * @param pIntegerFormulaManager The formula manager for integer formulae.
   * @param pReturnType The return type of the array call, i.e. the element type of the array.
   * @param pArgs The formula for the element that gets stored in the array.
   * @param <T> The formula type of the elements of the array.
   * @return A formula for the array and the call to it.
   * @see #declareAndCallArray(String, NumeralFormulaManager, FormulaType, Formula)
   */
  public <T extends Formula> T declareAndCallArray(
      final String pName,
      final int pIdx,
      final NumeralFormulaManager<?, ?> pIntegerFormulaManager,
      final FormulaType<T> pReturnType,
      final Formula pArgs) {

    String name = FormulaManagerView.makeName(pName, pIdx);
    return declareAndCallArray(name, pIntegerFormulaManager, pReturnType, pArgs);
  }

  /**
   * Returns a formula representing the declaration of an SMT array and a call
   * to it.
   *
   * @param pName The name of the array variable.
   * @param pIdx An additional index.
   * @param pArrayIndex The index in the array we want to store the value in.
   * @param pReturnType The return type of the array call, i.e. the element type of the array.
   * @param pArgs The formula for the element that gets stored in the array.
   * @param <T> The formula type of the elements of the array.
   * @return A formula for the array and the call to it.
   * @see #declareAndCallArray(String, NumeralFormulaManager, FormulaType, Formula)
   */
  public <T extends Formula> T declareAndCallArray(
      final String pName,
      final int pIdx,
      final Formula pArrayIndex,
      final FormulaType<T> pReturnType,
      final Formula pArgs) {
    String name = FormulaManagerView.makeName(pName, pIdx);
    return declareAndCallArray(name, pArrayIndex, pReturnType, pArgs);
  }

  /**
   * Returns a formula representing the declaration of an SMT array and a call
   * to it.
   *
   * @param pName The name of the array variable.
   * @param pArrayIndex The index in the array we want to store the value in.
   * @param pReturnType The return type of the array call, i.e. the element type of the array.
   * @param pArgs The formula for the element that gets stored in the array.
   * @param <T> The formula type of the elements of the array.
   * @return A formula for the array and the call to it.
   * @see #declareAndCallArray(String, NumeralFormulaManager, FormulaType, Formula)
   */
  public <T extends Formula> T declareAndCallArray(
      final String pName,
      final Formula pArrayIndex,
      final FormulaType<T> pReturnType,
      final Formula pArgs) {
    ArrayFormula<?, ?> arrayFormula = makeArray(pName, FormulaType.IntegerType, pReturnType);
    arrayFormula = store(arrayFormula, pArrayIndex, pArgs);
    return wrap(pReturnType, select(arrayFormula, pArrayIndex));
  }

}
