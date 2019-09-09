/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.cpa.sl;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BitvectorFormulaManager;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.IntegerFormulaManager;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SLFormulaManager;

public class SLHeapDelegateImpl implements SLHeapDelegate {

  private final LogManager logger;
  private final MachineModel machineModel;
  private final Solver solver;

  private final FormulaManagerView fm;
  private final BitvectorFormulaManager bvfm;
  private final IntegerFormulaManager ifm;
  private final SLFormulaManager slfm;
  private final BooleanFormulaManager bfm;

  private HashMap<Formula, Formula> heap = new HashMap<>();
  private final HashMap<Formula, BigInteger> allocationSizes = new HashMap<>();

  private final FormulaType<BitvectorFormula> heapValueFormulaType =
      FormulaType.getBitvectorTypeWithSize(16); // TODO BitVector Support
  private final FormulaType<IntegerFormula> heapAddresFormulaType = FormulaType.IntegerType;

  public SLHeapDelegateImpl(
      LogManager pLogger,
      Solver pSolver,
      MachineModel pMachineModel) {
    logger = pLogger;
    solver = pSolver;
    machineModel = pMachineModel;
    fm = solver.getFormulaManager();
    bvfm = fm.getBitvectorFormulaManager();
    ifm = fm.getIntegerFormulaManager();
    slfm = fm.getSLFormulaManager();
    bfm = fm.getBooleanFormulaManager();
  }

  @Override
  public void handleMalloc(Formula pMemoryLocation, BigInteger pSize)
      throws Exception {
    addToHeap(pMemoryLocation, pSize, false);
  }

  @Override
  public boolean
      handleRealloc(
          Formula pNewMemoryLocation,
          Formula pOldMemoryLocation,
          BigInteger pSize,
          PathFormula pContext)
          throws Exception {
    Formula f = checkAllocation(pOldMemoryLocation, pContext);
    if (f == null) {
      logger.log(Level.SEVERE, "REALLOC() failed - address not allocated.");
      return false;
    }
    removeFromHeap(pOldMemoryLocation);
    addToHeap(pNewMemoryLocation, pSize, false);
    return true;
  }

  @Override
  public void handleCalloc(Formula pMemoryLocation, BigInteger pNum, BigInteger pSize)
      throws Exception {
    addToHeap(pMemoryLocation, pNum.multiply(pSize), true);
  }

  @Override
  public void removeFromHeap(Formula pAddrFormula) {
    BigInteger size = allocationSizes.get(pAddrFormula);
    for (int i = 0; i < size.intValueExact(); i++) {
      if (i == 0) {
        heap.remove(pAddrFormula);
      } else {
        Formula tmp =
            bvfm.add((BitvectorFormula) pAddrFormula, bvfm.makeBitvector(size.bitLength(), i));
        heap.remove(tmp);
      }
    }
  }

  @Override
  public Formula checkAllocation(
      Formula pMemoryLocation,
      Formula pOffset,
      Formula pVal,
      PathFormula pContext)
      throws Exception {
    Formula fLoc = pMemoryLocation;
    if (pOffset != null) {
      fLoc = fm.makePlus(fLoc, pOffset);
    }
    return checkAllocation(fLoc, pVal, pContext);
  }

  private Formula checkAllocation(
      Formula fLoc,
      Formula pVal,
      PathFormula pContext) {
    // Syntactical check for performance.
    if (heap.containsKey(fLoc)) {
      if (pVal != null) {
        heap.put(fLoc, pVal);
      }
      return fLoc;
    }
    // Semantical check.
    for (Formula formulaOnHeap : heap.keySet()) {
      if (checkEquivalence(fLoc, formulaOnHeap, pContext)) {
        if (pVal != null) {
          heap.put(formulaOnHeap, pVal);
        }
        return formulaOnHeap;
      }
    }
    return null;
  }

  @Override
  public boolean handleFree(SLFormulaBuilder pSolDel, Formula pLocation, PathFormula pContext)
      throws Exception {
    Formula loc = checkAllocation(pLocation, pContext);
    if (loc == null) {
      return false;
    }
    removeFromHeap(loc);
    return true;
  }

  @Override
  public Map<Formula, Formula> getHeap() {
    return heap;
  }

  @Override
  public void addToHeap(Formula pHeapLocation, BigInteger pSize, boolean initWithZero) {
    for (int i = 0; i < pSize.intValueExact(); i++) {
      Formula f = pHeapLocation;
      if (i > 0) {
        // heap of bytes/chars.
        f = bvfm.add((BitvectorFormula) f, bvfm.makeBitvector(16, i));
      } else {
        allocationSizes.put(f, pSize);
      }

      heap.put(f, initWithZero ? ifm.makeNumber(0) : ifm.makeNumber(0)); // TODO nil element
    }
  }

  @Override
  public void addToHeap(
      Formula pHeapLocation,
      BigInteger pLength,
      CType pType,
      boolean initWithZero)
      throws Exception {

    BigInteger length = pLength.multiply(machineModel.getSizeof(pType));
    addToHeap(pHeapLocation, length, initWithZero);
  }

  @Override
  public boolean isAllocated(Formula pLocation, Formula pOffset, PathFormula pContext)
      throws Exception {
    BooleanFormula heapFormula = getHeapFormula();
    BooleanFormula toBeChecked =
        slfm.makePointsTo(pLocation, ifm.makeNumber(0));
    try (ProverEnvironment prover = solver.newProverEnvironment()) {
      prover.addConstraint(pContext.getFormula());
      prover.addConstraint(slfm.makeStar(toBeChecked, heapFormula));
      return prover.isUnsat();
    } catch (Exception e) {
      logger.log(Level.SEVERE, e.getMessage());
    }
    return false;
  }

  /***
   * Generates a SL heap formula.
   *
   * @return Formula - key0->val0 * key1->val1 * ... * keyX->valX
   */
  @Override
  public BooleanFormula getHeapFormula() {
    BooleanFormula formula = slfm.makeEmptyHeap(heapAddresFormulaType, heapValueFormulaType);
    for (Formula f : heap.keySet()) {
      Formula target = heap.get(f);
      BooleanFormula ptsTo =
          slfm.makePointsTo(f, target != null ? target : ifm.makeNumber(0));
      formula = slfm.makeStar(formula, ptsTo);
    }
    return formula;
  }

  @Override
  public boolean checkEquivalence(Formula pF0, Formula pF1, PathFormula pContext) {
    BooleanFormula tmp = fm.makeEqual(pF0, pF1);
    try (ProverEnvironment prover = solver.newProverEnvironment()) {
      prover.addConstraint(pContext.getFormula());
      prover.addConstraint(tmp);
      if (prover.isUnsat()) {
        return false;
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, e.getMessage());
    }
    // Check tautology.
    try (ProverEnvironment prover = solver.newProverEnvironment()) {
      prover.addConstraint(pContext.getFormula());
      prover.addConstraint(bfm.not(tmp));
      return prover.isUnsat();
    } catch (Exception e) {
      logger.log(Level.SEVERE, e.getMessage());
    }
    return false;
  }
}
