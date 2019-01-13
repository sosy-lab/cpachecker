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
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.util.predicates.pathformula.jstoformula;

import java.util.logging.Level;
import org.sosy_lab.cpachecker.cfa.types.js.JSAnyType;

class VariableIndexManager extends ManagerWithEdgeContext {
  // Index that is used to read from variables that were not assigned yet
  private static final int VARIABLE_UNINITIALIZED = 1;

  // Index to be used for first assignment to a variable (must be higher than
  // VARIABLE_UNINITIALIZED!)
  private static final int VARIABLE_FIRST_ASSIGNMENT = 2;

  VariableIndexManager(final EdgeManagerContext pCtx) {
    super(pCtx);
  }

  /** Produces a fresh new SSA index for an assignment and updates the SSA map. */
  @SuppressWarnings("ResultOfMethodCallIgnored")
  int makeFreshIndex(final String name) {
    int idx = getFreshIndex(name);
    ctx.ssa.setIndex(name, JSAnyType.ANY, idx);
    return idx;
  }

  /**
   * Produces a fresh new SSA index for an assignment, but does _not_ update the SSA map. Usually
   * you should use {@link #makeFreshIndex(String)} instead, because using variables with indices
   * that are not stored in the SSAMap is not a good idea (c.f. the comment inside getIndex()). If
   * you use this method, you need to make sure to update the SSAMap correctly.
   */
  private int getFreshIndex(final String name) {
    //    checkSsaSavedType(name, type, ssa.getType(name));
    int idx = ctx.ssa.getFreshIndex(name);
    if (idx <= 0) {
      idx = VARIABLE_FIRST_ASSIGNMENT;
    }
    return idx;
  }

  /**
   * This method returns the index of the given variable in the ssa map, if there is none, it
   * creates one with the value 1.
   *
   * @return the index of the variable
   */
  @SuppressWarnings("ResultOfMethodCallIgnored")
  int getIndex(final String name) {
    //    checkSsaSavedType(name, type, ssa.getType(name));
    int idx = ctx.ssa.getIndex(name);
    if (idx <= 0) {
      ctx.conv.logger.log(Level.ALL, "WARNING: Auto-instantiating variable:", name);
      idx = VARIABLE_UNINITIALIZED;

      // It is important to store the index in the variable here.
      // If getIndex() was called with a specific name,
      // this means that name@idx will appear in formulas.
      // Thus we need to make sure that calls to FormulaManagerView.instantiate()
      // will also add indices for this name,
      // which it does exactly if the name is in the SSAMap.
      ctx.ssa.setIndex(name, JSAnyType.ANY, idx);
    }
    return idx;
  }
}
