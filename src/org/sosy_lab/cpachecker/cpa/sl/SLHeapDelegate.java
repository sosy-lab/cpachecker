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
import java.util.Map;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;

public interface SLHeapDelegate {



  /**
   * Manipulates the heap concerning malloc().
   *
   * @param pMemoryLocation - formula representing the memory location.
   * @param pLength - size of memory range in bytes.
   */
  public void handleMalloc(Formula pMemoryLocation, BigInteger pLength)
      throws Exception;

  /**
   * Manipulates the heap concerning realloc().
   *
   * @param pNewLocation - formula representing the new memory location.
   * @param pOldLocation - formula representing the old memory location.
   * @param pSize - size of memory range in bytes.
   * @param pContext - the context to be reallocated in.
   * @return true if memory was reallocated successfully, false otherwise.
   */
  public boolean
      handleRealloc(
          Formula pNewLocation,
          Formula pOldLocation,
          BigInteger pSize,
          PathFormula pContext)
      throws Exception;

  /**
   * Manipulates the heap concerning calloc().
   *
   * @param pMemoryLocation - formula representing the memory location.
   * @param pLength - length of memory range.
   * @param pSize - size of each object in bytes.
   */
  public void handleCalloc(Formula pMemoryLocation, BigInteger pLength, BigInteger pSize)
      throws Exception;

  /**
   * Frees allocated memory.
   *
   * @param pSolverDelegate - a @SLSolverDelegate to check whether the location that has to be
   *        deallocated is on the heap.
   * @param pMemoryLocation - name of the pointer to the allocated memory.
   * @return true if memory was freed successfully, false otherwise.
   */
  public boolean
      handleFree(SLFormulaBuilder pSolverDelegate, Formula pMemoryLocation, PathFormula pContext)
      throws Exception;

  /**
   * Checks whether the given address is allocated (on heap or stack). The associated value can be
   * updated.
   *
   * @param pMemoryLocation - formula representing the memory location to be checked.
   * @param pOffset - optional array offset, null otherwise.
   * @param pVal - optional value to be updated, null otherwise.
   * @param pContext - the state's path formula to be checked in.
   *
   * @return The formula if allocated, null otherwise.
   */
  public Formula checkAllocation(
      Formula pMemoryLocation,
      Formula pOffset,
      Formula pVal,
      PathFormula pContext)
      throws Exception;

  /**
   * Checks whether the given address is allocated.
   *
   * @see SLHeapDelegate#checkAllocation(SLFormulaBuilder, Formula, Formula, Formula, PathFormula)
   */
  default public Formula checkAllocation(Formula pLocation, PathFormula pContext) throws Exception {
    return checkAllocation(pLocation, null, null, pContext);
  }

  /**
   * Checks whether the given location is allocated.
   **/
  public boolean isAllocated(
      Formula pLocation,
      Formula pOffset,
      PathFormula pContext)
      throws Exception;

  /**
   * Checks whether the given location is allocated.
   *
   * @see SLHeapDelegate#isAllocated(Formula, Formula)
   */
  default public boolean isAllocated(Formula pLocation, PathFormula pContext) throws Exception {
    return isAllocated(pLocation, null, pContext);
  }


  public Map<Formula, Formula> getHeap();

  public static CExpression createSymbolicLocation(CSimpleDeclaration pDecl) {
    CIdExpression e = new CIdExpression(FileLocation.DUMMY, pDecl);
    return new CUnaryExpression(FileLocation.DUMMY, pDecl.getType(), e, UnaryOperator.AMPER);
  }

  /**
   * Deallocates the given location from the heap.
   *
   * @param pLoc - Heap location to be deallocated.
   */
  public void removeFromHeap(Formula pLoc);

  public BooleanFormula getHeapFormula();

  public void addToHeap(Formula pHeapLocation, BigInteger pSize, boolean initWithZero)
      throws Exception;

  public void
      addToHeap(Formula pHeapLocation, BigInteger pLength, CType pType, boolean pInitWithZero)
      throws Exception;

  /**
   * Checks whether two formulae are semantically equivalent in the given state's context.
   *
   * @return f0 <=> f1
   */
  boolean checkEquivalence(Formula pF0, Formula pF1, PathFormula pContext);
}