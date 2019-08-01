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
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.java_smt.api.Formula;

public interface SLMemoryDelegate {



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
   * @return true if memory was reallocated successfully, false otherwise.
   */
  public boolean
      handleRealloc(Formula pNewLocation, Formula pOldLocation, BigInteger pSize)
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
  public boolean handleFree(SLSolverDelegate pSolverDelegate, Formula pMemoryLocation)
      throws Exception;


  /**
   * Checks whether the given address is allocated on the heap. The associated value can be updated.
   *
   * @param pMemoryLocation - formula representing the memory location to be checked.
   * @param pOffset - optional array offset, null otherwise.
   * @param pVal - optional value to be updated, null otherwise.
   *
   * @return The formula on the heap if allocated, null otherwise.
   */
  public Formula checkAllocation(
      SLSolverDelegate pSolDel,
      Formula pMemoryLocation,
      Formula pOffset,
      Formula pVal)
      throws Exception;

  /**
   * Checks whether the given address is allocated on the heap.
   *
   * @see SLMemoryDelegate#checkAllocation(SLSolverDelegate, Formula, Formula, Formula)
   */
  default public Formula checkAllocation(SLSolverDelegate pSolDel, Formula pLocation)
      throws Exception {
    return checkAllocation(pSolDel, pLocation, null, null);
  }

  public Map<Formula, Formula> getHeap();

  public Map<Formula, Formula> getStack();

  /**
   * Removes the given variable from the stack.
   */
  public void removeFromStack(Formula pVar);

  /**
   * A new range of consecutive fresh cells is allocated on the heap.
   *
   * @param pMemoryLocation - @Formula representing the memory location.
   * @param pSize - size of the range.
   * @param pType - type of the value to be stored on the heap.
   * @param pInitWithZero - if true, the new added location is pointing to zero, otherwise to @null.
   */
  public void
      addToStack(Formula pMemoryLocation, BigInteger pSize, CType pType, boolean pInitWithZero)
          throws Exception;
}