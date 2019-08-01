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

import java.lang.annotation.Inherited;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BitvectorFormulaManager;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.IntegerFormulaManager;

public class SLMemoryDelegateImpl implements SLMemoryDelegate {

  private final LogManager logger;
  private final MachineModel machineModel;
  private final FormulaManagerView fm;
  private final BitvectorFormulaManager bvfm;
  private final IntegerFormulaManager ifm;
  private HashMap<Formula, Formula> heap = new HashMap<>();
  private HashMap<Formula, Formula> stack = new HashMap<>();
  private final HashMap<Formula, BigInteger> allocationSizes = new HashMap<>();

  public SLMemoryDelegateImpl(
      LogManager pLogger,
      MachineModel pMachineModel,
      FormulaManagerView pFm) {
    logger = pLogger;
    machineModel = pMachineModel;
    fm = pFm;
    bvfm = fm.getBitvectorFormulaManager();
    ifm = fm.getIntegerFormulaManager();
  }


  // -------------------------------------------------------------------------------------------------
  // Delegate methods starting here.
  // -------------------------------------------------------------------------------------------------
  @Override
  public void handleMalloc(Formula pMemoryLocation, BigInteger pSize)
      throws Exception {
    addToMemory(heap, pMemoryLocation, pSize, false);
  }

  /**
   * {@link Inherited}
   */
  @Override
  public boolean
      handleRealloc(
          Formula pOldMemoryLocation,
          Formula pNewMemoryLocation,
          BigInteger pSize)
          throws Exception {
    if (!heap.containsKey(pOldMemoryLocation)) {
      logger.log(Level.SEVERE, "REALLOC() failed.");
      return false;
    }
    removeFromMemory(heap, pOldMemoryLocation);
    addToMemory(heap, pNewMemoryLocation, pSize, false);
    return true;
  }

  @Override
  public void handleCalloc(Formula pMemoryLocation, BigInteger pNum, BigInteger pSize)
      throws Exception {
    addToMemory(heap, pMemoryLocation, pNum.multiply(pSize), true);
  }

  private void removeFromMemory(Map<Formula, Formula> memory, Formula pAddrFormula) {
    BigInteger size = allocationSizes.get(pAddrFormula);
    for (int i = 0; i < size.intValueExact(); i++) {
      if (i == 0) {
        memory.remove(pAddrFormula);
      } else {
        Formula tmp =
            bvfm.add((BitvectorFormula) pAddrFormula, bvfm.makeBitvector(size.bitLength(), i));
        memory.remove(tmp);
      }
    }
  }


  @Override
  public Formula checkAllocation(
      SLSolverDelegate pSolDel,
      Formula pMemoryLocation,
      Formula pOffset,
      Formula pVal)
      throws Exception {
    Formula fLoc = pMemoryLocation;
    if (pOffset != null) {
      fLoc = fm.makePlus(fLoc, pOffset);
    }
    // Syntactical check for performance.
    if (heap.containsKey(fLoc)) {
      if (pVal != null) {
        heap.put(fLoc, pVal);
      }
      return fLoc;
    }
    // Semantical check.
    for (Formula formulaOnHeap : heap.keySet()) {
      if (pSolDel.checkEquivalence(fLoc, formulaOnHeap)) {
        if(pVal != null) {
          heap.put(formulaOnHeap, pVal);
        }
        return formulaOnHeap;
      }
    }
    return null;
  }

  @Override
  public boolean handleFree(SLSolverDelegate pSolDel, Formula pLocation) throws Exception {
    Formula loc = checkAllocation(pSolDel, pLocation);
    if (loc == null) {
      return false;
    }
    removeFromMemory(heap, loc);
    return true;
  }

  @Override
  public Map<Formula, Formula> getHeap() {
    return heap;
  }

  @Override
  public Map<Formula, Formula> getStack() {
    return stack;
  }

  @Override
  public void removeFromStack(Formula pVar) {
    removeFromMemory(stack, pVar);
  }

  private void addToMemory(
      Map<Formula, Formula> memory,
      Formula pMemoryLocation,
      BigInteger pLength,
      boolean pInitWithZero)
      throws Exception {

    for (int i = 0; i < pLength.intValueExact(); i++) {
      Formula f = pMemoryLocation;
      if (i > 0) {
        // stack and heap of bytes/chars.
        f = bvfm.add((BitvectorFormula) pMemoryLocation, bvfm.makeBitvector(8, i));
      } else {
        allocationSizes.put(f, pLength);
      }
      memory.put(f, pInitWithZero ? ifm.makeNumber(0) : null);
    }
  }

  @Override
  public void
      addToStack(Formula pMemoryLocation, BigInteger pLength, CType pType, boolean pInitWithZero)
          throws Exception {
    BigInteger length = pLength.multiply(machineModel.getSizeof(pType));
    addToMemory(stack, pMemoryLocation, length, pInitWithZero);
  }

}
