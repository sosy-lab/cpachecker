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
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;

import java.util.ArrayList;
import java.util.List;

public class SSAMapMerger {

  private final FormulaManagerView fmgr;
  private final BooleanFormulaManager bfmgr;
  private final ShutdownNotifier shutdownNotifier;
  private final CtoFormulaConverter converter;
  private final boolean useNondetFlags;
  private final FormulaType<?> nondetFormulaType;

  SSAMapMerger(
      boolean pUseNondetFlags,
      FormulaManagerView pFmgr,
      CtoFormulaConverter pConverter,
      ShutdownNotifier pShutdownNotifier,
      FormulaType<?> pNondetFormulaType) {
    useNondetFlags = pUseNondetFlags;
    fmgr = pFmgr;
    bfmgr = pFmgr.getBooleanFormulaManager();
    converter = pConverter;
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

    BooleanFormula mergeFormula1 = bfmgr.makeTrue();
    BooleanFormula mergeFormula2 = bfmgr.makeTrue();

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

    return new MergeResult<>(resultSSA, mergeFormula1, mergeFormula2, bfmgr.makeTrue());
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
    } else {
      return converter.makeSsaUpdateTerm(symbolName, symbolType, oldIndex, newIndex, oldPts);
    }
  }

  private BooleanFormula makeSsaNondetFlagMerger(int iSmaller, int iBigger) {
    Formula pInitialValue = fmgr.makeNumber(nondetFormulaType, 0);
    assert iSmaller < iBigger;

    List<BooleanFormula> lResult = new ArrayList<>();
    FormulaType<Formula> type = fmgr.getFormulaType(pInitialValue);

    for (int i = iSmaller + 1; i <= iBigger; ++i) {
      Formula currentVar = fmgr.makeVariable(type, NONDET_FLAG_VARIABLE, i);
      lResult.add(fmgr.assignment(currentVar, pInitialValue));
    }

    return bfmgr.and(lResult);
  }


   BooleanFormula addMergeAssumptions(
      final BooleanFormula pFormula,
      final SSAMap ssa1,
      final PointerTargetSet pts1,
      final SSAMap ssa2)
      throws InterruptedException {
    final List<MapsDifference.Entry<String, Integer>> symbolDifferences = new ArrayList<>();
    final SSAMap resultSSA = SSAMap.merge(ssa1, ssa2, collectMapsDifferenceTo(symbolDifferences));

    List<BooleanFormula> mergeFormula = new ArrayList<>();
    mergeFormula.add(pFormula);

    for (final MapsDifference.Entry<String, Integer> symbolDifference : symbolDifferences) {
      shutdownNotifier.shutdownIfNecessary();
      final String symbolName = symbolDifference.getKey();
      final CType symbolType = resultSSA.getType(symbolName);
      final int index1 = symbolDifference.getLeftValue().orElse(1);
      final int index2 = symbolDifference.getRightValue().orElse(1);

      assert symbolName != null;
      if (index1 > index2 && index1 > 1) {
        // assumption violated
        // ssa2 is not the merge result of ssa1 and further ssa maps
        // simplify following PCC coverage check which will likely fail anyway
        // and return coarsest overapproximation
        return bfmgr.makeTrue();

      } else if (index2 > 1) {
        assert index1 < index2;
        // i1:smaller, i2:bigger
        // => need correction term for i1
        for (int i = index1; i < index2; i++) {
          mergeFormula.add(makeSsaMerger(symbolName, symbolType, i, i + 1, pts1));
        }
      }
    }

    return bfmgr.and(mergeFormula);
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
      BooleanFormula trueFormula = bfmgr.makeTrue();
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
