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

import java.util.Map;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cpa.sl.SLState.SLStateError;
import org.sosy_lab.java_smt.api.Formula;

public interface SLHeapDelegate {



  /**
   * Manipulates the heap concerning malloc().
   *
   * @param pMemoryLocation - CExpression representing the memory location.
   * @param pSize - size of memory range in bytes.
   */
  public void handleMalloc(CExpression pMemoryLocation, CExpression pSize)
      throws Exception;

  /**
   * Manipulates the heap concerning realloc().
   *
   * @param pNewLocation - CExpression representing the new memory location.
   * @param pOldLocation - CExpression representing the old memory location.
   * @param pSize - size of memory range in bytes.
   * @return true if memory was reallocated successfully, false otherwise.
   */
  public SLStateError
      handleRealloc(
          CExpression pNewLocation,
          CExpression pOldLocation,
          CExpression pSize)
      throws Exception;

  /**
   * Manipulates the heap concerning calloc().
   *
   * @param pMemoryLocation - CExpression representing the memory location.
   * @param pLength - length of memory range.
   * @param pSize - size of each object in bytes.
   */
  public void handleCalloc(CExpression pMemoryLocation, CExpression pLength, CExpression pSize)
      throws Exception;

  /**
   * Frees allocated memory.
   *
   * @param pMemoryLocation - name of the pointer to the allocated memory.
   * @return true if memory was freed successfully, false otherwise.
   */
  public SLStateError
      handleFree(CExpression pMemoryLocation)
      throws Exception;

  /**
   * Checks if memory leaks occur caused by out of scope variable.
   *
   * @param pVar - The variable, that is no longer in scope.
   * @return SLStateError - UNFREED Memory if a heap pointer is lost that causes memory leaks, null
   *         otherwise.
   */
  public SLStateError handleOutOfScopeVariable(CSimpleDeclaration pVar) throws Exception;

  /**
   *
   */
  public void handleDeclaration(CVariableDeclaration pDecl) throws Exception;

  /**
   *
   * @param pExp
   * @return
   * @throws Exception
   */
  default public SLStateError handleDereference(CExpression pExp) throws Exception {
    return handleDereference(pExp, null);
  }


  public SLStateError handleDereference(CExpression pExp, CExpression pOffset) throws Exception;

  /**
   *
   */
  public SLStateError
      handleDereferenceAssignment(CExpression pLHS, CExpression pOffset, CExpression pRHS)
          throws Exception;

  public Map<Formula, Formula> getStack();
  public Map<Formula, Formula> getHeap();


}