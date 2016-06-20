/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.pathformula;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sosy_lab.common.collect.MapsDifference.collectMapsDifferenceTo;
import static org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl.NONDET_FLAG_VARIABLE;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.collect.MapsDifference;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaTypeHandler;
import org.sosy_lab.cpachecker.util.predicates.pathformula.heaparray.CToFormulaConverterWithHeapArray;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CToFormulaConverterWithPointerAliasing;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTarget;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.smt.ArrayFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FunctionFormulaManagerView;
import org.sosy_lab.solver.api.ArrayFormula;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.BooleanFormulaManager;
import org.sosy_lab.solver.api.Formula;
import org.sosy_lab.solver.api.FormulaType;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

public class SSAMapMerger {

  private final FormulaManagerView fmgr;
  private final FunctionFormulaManagerView ffmgr;
  private final BooleanFormulaManager bfmgr;
  private final ShutdownNotifier shutdownNotifier;
  private final CtoFormulaConverter converter;
  private final CtoFormulaTypeHandler typeHandler;
  private final ArrayFormulaManagerView afmgr;
  private final boolean useNondetFlags;
  private final boolean handleHeapArray;
  private final FormulaType<?> nondetFormulaType;

  SSAMapMerger(
      boolean pUseNondetFlags,
      boolean pHandleHeapArray,
      FormulaManagerView pFmgr,
      @Nullable ArrayFormulaManagerView pAfmgr,
      CtoFormulaConverter pConverter,
      CtoFormulaTypeHandler pTypeHandler,
      ShutdownNotifier pShutdownNotifier,
      FormulaType<?> pNondetFormulaType) {
    useNondetFlags = pUseNondetFlags;
    handleHeapArray = pHandleHeapArray;
    fmgr = pFmgr;
    ffmgr = pFmgr.getFunctionFormulaManager();
    bfmgr = pFmgr.getBooleanFormulaManager();
    afmgr = pAfmgr; // initialized in calling class, do not use pFmgr.getArrayFormulaManager()!
    converter = pConverter;
    typeHandler = pTypeHandler;
    shutdownNotifier = pShutdownNotifier;
    nondetFormulaType = pNondetFormulaType;
  }

  /**
   * builds a formula that represents the necessary variable assignments
   * to "merge" the two ssa maps. That is, for every variable X that has two
   * different ssa indices i and j in the maps, creates a new formula
   * (X_k = X_i) | (X_k = X_j), where k is a fresh ssa index.
   * Returns the formula described above, plus a new SSAMap that is the merge
   * of the two.
   *
   * @param ssa1 an SSAMap
   * @param pts1 the PointerTargetSet for ssa1
   * @param ssa2 an SSAMap
   * @param pts2 the PointerTargetSet for ssa1
   * @return The new SSAMap and the formulas that need to be added to the path formulas before disjuncting them.
   */
  MergeResult<SSAMap> mergeSSAMaps(
      final SSAMap ssa1,
      final PointerTargetSet pts1,
      final SSAMap ssa2,
      final PointerTargetSet pts2)
      throws InterruptedException {
    final List<MapsDifference.Entry<String, Integer>> symbolDifferences = new ArrayList<>();
    final SSAMap resultSSA = SSAMap.merge(ssa1, ssa2, collectMapsDifferenceTo(symbolDifferences));

    BooleanFormula mergeFormula1 = bfmgr.makeBoolean(true);
    BooleanFormula mergeFormula2 = bfmgr.makeBoolean(true);

    for (final MapsDifference.Entry<String, Integer> symbolDifference : symbolDifferences) {
      shutdownNotifier.shutdownIfNecessary();
      final String symbolName = symbolDifference.getKey();
      final CType symbolType = resultSSA.getType(symbolName);
      final int index1 = symbolDifference.getLeftValue().orElse(1);
      final int index2 = symbolDifference.getRightValue().orElse(1);

      assert symbolName != null;
      if (index1 > index2 && index1 > 1) {
        // i2:smaller, i1:bigger
        // => need correction term for i2
        BooleanFormula mergeFormula = makeSsaMerger(symbolName, symbolType, index2, index1, pts2);

        mergeFormula2 = bfmgr.and(mergeFormula2, mergeFormula);

      } else if (index2 > 1) {
        assert index1 < index2;
        // i1:smaller, i2:bigger
        // => need correction term for i1
        BooleanFormula mergeFormula = makeSsaMerger(symbolName, symbolType, index1, index2, pts1);

        mergeFormula1 = bfmgr.and(mergeFormula1, mergeFormula);
      }
    }

    return new MergeResult<>(resultSSA, mergeFormula1, mergeFormula2, bfmgr.makeBoolean(true));
  }

  /**
   * Create the necessary equivalence terms for adjusting the SSA indices
   * of a given symbol (of any type) from oldIndex to newIndex.
   */
  private BooleanFormula makeSsaMerger(
      final String symbolName,
      final CType symbolType,
      final int oldIndex,
      final int newIndex,
      final PointerTargetSet oldPts)
      throws InterruptedException {
    assert oldIndex > 0;
    assert newIndex > oldIndex;

    // Important note:
    // we need to use fmgr.assignment in these methods,
    // because fmgr.equal has undesired semantics for floating points.

    if (useNondetFlags && symbolName.equals(NONDET_FLAG_VARIABLE)) {
      return makeSsaNondetFlagMerger(oldIndex, newIndex);
    } else if (handleHeapArray && CToFormulaConverterWithHeapArray.isSMTArray(symbolName)) {
      assert symbolName.equals(CToFormulaConverterWithHeapArray.getArrayName(symbolType));
      return makeSsaArrayMerger(symbolName, symbolType, oldIndex, newIndex);
    } else if (CToFormulaConverterWithPointerAliasing.isUF(symbolName)) {
      assert symbolName.equals(CToFormulaConverterWithPointerAliasing.getUFName(symbolType));
      return makeSsaUFMerger(symbolName, symbolType, oldIndex, newIndex, oldPts);
    } else {
      return makeSsaVariableMerger(symbolName, symbolType, oldIndex, newIndex);
    }
  }

  private BooleanFormula makeSsaVariableMerger(
      final String variableName, final CType variableType, final int oldIndex, final int newIndex) {
    assert oldIndex < newIndex;

    // TODO Previously we called makeMerger,
    // which creates the terms (var@oldIndex = var@oldIndex+1; ...; var@oldIndex = var@newIndex).
    // Now we only create a single term (var@oldIndex = var@newIndex).
    // This should not make a difference except maybe for the model,
    // but this could be investigated to be sure.

    final FormulaType<?> variableFormulaType = converter.getFormulaTypeFromCType(variableType);
    final Formula oldVariable = fmgr.makeVariable(variableFormulaType, variableName, oldIndex);
    final Formula newVariable = fmgr.makeVariable(variableFormulaType, variableName, newIndex);

    return fmgr.assignment(newVariable, oldVariable);
  }

  private BooleanFormula makeSsaArrayMerger(
      final String pFunctionName,
      final CType pReturnType,
      final int pOldIndex,
      final int pNewIndex) {
    assert pOldIndex < pNewIndex;
    final FormulaType<?> returnFormulaType = converter.getFormulaTypeFromCType(pReturnType);
    final ArrayFormula<?, ?> newArray =
        afmgr.makeArray(
            pFunctionName + "@" + pNewIndex, FormulaType.IntegerType, returnFormulaType);
    final ArrayFormula<?, ?> oldArray =
        afmgr.makeArray(
            pFunctionName + "@" + pOldIndex, FormulaType.IntegerType, returnFormulaType);
    return fmgr.makeEqual(newArray, oldArray);
  }

  private BooleanFormula makeSsaUFMerger(
      final String functionName,
      final CType returnType,
      final int oldIndex,
      final int newIndex,
      final PointerTargetSet pts)
      throws InterruptedException {
    assert oldIndex < newIndex;

    final FormulaType<?> returnFormulaType = converter.getFormulaTypeFromCType(returnType);
    BooleanFormula result = bfmgr.makeBoolean(true);
    for (final PointerTarget target : pts.getAllTargets(returnType)) {
      shutdownNotifier.shutdownIfNecessary();
      final Formula targetAddress =
          fmgr.makePlus(
              fmgr.makeVariable(typeHandler.getPointerType(), target.getBaseName()),
              fmgr.makeNumber(typeHandler.getPointerType(), target.getOffset()));

      final BooleanFormula retention =
          fmgr.assignment(
              ffmgr.declareAndCallUninterpretedFunction(
                  functionName, newIndex, returnFormulaType, targetAddress),
              ffmgr.declareAndCallUninterpretedFunction(
                  functionName, oldIndex, returnFormulaType, targetAddress));
      result = fmgr.makeAnd(result, retention);
    }

    return result;
  }

  private BooleanFormula makeSsaNondetFlagMerger(int iSmaller, int iBigger) {
    Formula pInitialValue = fmgr.makeNumber(nondetFormulaType, 0);
    assert iSmaller < iBigger;

    BooleanFormula lResult = bfmgr.makeBoolean(true);
    FormulaType<Formula> type = fmgr.getFormulaType(pInitialValue);

    for (int i = iSmaller + 1; i <= iBigger; ++i) {
      Formula currentVar = fmgr.makeVariable(type, NONDET_FLAG_VARIABLE, i);
      BooleanFormula e = fmgr.assignment(currentVar, pInitialValue);
      lResult = bfmgr.and(lResult, e);
    }

    return lResult;
  }

  BooleanFormula addMergeAssumptions(
      final BooleanFormula pFormula,
      final SSAMap ssa1,
      final PointerTargetSet pts1,
      final SSAMap ssa2)
      throws InterruptedException {
    final List<MapsDifference.Entry<String, Integer>> symbolDifferences = new ArrayList<>();
    final SSAMap resultSSA = SSAMap.merge(ssa1, ssa2, collectMapsDifferenceTo(symbolDifferences));

    BooleanFormula mergeFormula1 = pFormula;

    for (final MapsDifference.Entry<String, Integer> symbolDifference : symbolDifferences) {
      shutdownNotifier.shutdownIfNecessary();
      final String symbolName = symbolDifference.getKey();
      final CType symbolType = resultSSA.getType(symbolName);
      final int index1 = symbolDifference.getLeftValue().orElse(1);
      final int index2 = symbolDifference.getRightValue().orElse(1);

      assert symbolName != null;
      if (index1 > index2 && index1 > 1) {
        return bfmgr.makeBoolean(true);

      } else if (index2 > 1) {
        assert index1 < index2;
        // i1:smaller, i2:bigger
        // => need correction term for i1
        BooleanFormula mergeFormula;
        for (int i = index1; i < index2; i++) {
          mergeFormula = makeSsaMerger(symbolName, symbolType, i, i + 1, pts1);
          mergeFormula1 = bfmgr.and(mergeFormula1, mergeFormula);
        }
      }
    }

    return mergeFormula1;
  }

  /**
   * Class representing the result of the operation of merging (disjuncting)
   * additional parts of {@link PathFormula}s beyond the actual formula.
   */
  public static class MergeResult<T> {

    private final BooleanFormula leftConjunct;
    private final BooleanFormula rightConjunct;
    private final BooleanFormula finalConjunct;

    private final T result;

    public MergeResult(
        T pResult,
        BooleanFormula pLeftConjunct,
        BooleanFormula pRightConjunct,
        BooleanFormula pFinalConjunct) {
      result = checkNotNull(pResult);
      leftConjunct = checkNotNull(pLeftConjunct);
      rightConjunct = checkNotNull(pRightConjunct);
      finalConjunct = checkNotNull(pFinalConjunct);
    }

    public static <T> MergeResult<T> trivial(T result, BooleanFormulaManagerView bfmgr) {
      BooleanFormula trueFormula = bfmgr.makeBoolean(true);
      return new MergeResult<>(result, trueFormula, trueFormula, trueFormula);
    }

    /**
     * This is a formula that needs to be conjuncted to the left formula
     * before it is used in the disjunction.
     */
    BooleanFormula getLeftConjunct() {
      return leftConjunct;
    }

    /**
     * This is a formula that needs to be conjuncted to the right formula
     * before it is used in the disjunction.
     */
    BooleanFormula getRightConjunct() {
      return rightConjunct;
    }

    /**
     * This is a formula that needs to be conjuncted to the result of the disjunction.
     */
    BooleanFormula getFinalConjunct() {
      return finalConjunct;
    }

    T getResult() {
      return result;
    }
  }
}
