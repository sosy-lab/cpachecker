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
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.java_smt.api.Formula;

public interface SLHeapDelegate {
  public BigInteger getAllocationSize(CExpression pExp) throws Exception;

  /**
   * A new range of consecutive fresh cells is allocated on the heap.
   *
   * @param pVarName - pointer name.
   * @param size - size of range.
   */
  public void addToHeap(String pVarName, BigInteger size);

  /**
   * The range associated with the given pointer is deallocated i.e. removed from the heap.
   *
   * @param pAddrFormula - the formula representing the pointer.
   */
  public void removeFromHeap(Formula pAddrFormula);

  /**
   * Checks whether the given address is allocated on the heap. The associated value can be updated.
   *
   * @param pAddrExp - the address to be checked.
   * @param pOffset - optional array offset, null otherwise.
   * @param pVal - optional value to be updated, null otherwise.
   *
   * @return The formula on the heap if allocated, null otherwise.
   * @throws Exception - Either PathFormulaManager can't convert expression(s) to formulae or solver
   *         exception.
   */
  public Formula checkAllocation(CExpression pAddrExp, CExpression pOffset, CExpression pVal)
      throws Exception;

  default public Formula checkAllocation(CExpression pAddrExp) throws Exception {
    return checkAllocation(pAddrExp, null, null);
  }
}