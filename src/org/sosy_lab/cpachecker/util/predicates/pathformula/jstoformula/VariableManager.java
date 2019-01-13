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

import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;

class VariableManager {
  private final EdgeManagerContext ctx;

  VariableManager(final EdgeManagerContext pCtx) {
    ctx = pCtx;
  }

  /**
   * Create a formula for a given variable. This method does not handle scoping and the
   * NON_DET_VARIABLE!
   *
   * <p>This method does not update the index of the variable.
   */
  protected IntegerFormula makeVariable(final String name) {
    int useIndex = ctx.varIdMgr.getIndex(name);
    return ctx.conv.fmgr.makeVariable(Types.VARIABLE_TYPE, name, useIndex);
  }

  IntegerFormula makePreviousVariable(final String name) {
    int useIndex = ctx.varIdMgr.getIndex(name);
    return ctx.conv.fmgr.makeVariable(Types.VARIABLE_TYPE, name, useIndex - 1);
  }

  /**
   * Create a formula for a given variable with a fresh index for the left-hand side of an
   * assignment. This method does not handle scoping and the NON_DET_VARIABLE!
   */
  @SuppressWarnings("ResultOfMethodCallIgnored")
  IntegerFormula makeFreshVariable(final String name) {
    int useIndex;

    if (ctx.conv.direction == AnalysisDirection.BACKWARD) {
      useIndex = ctx.varIdMgr.getIndex(name);
    } else {
      useIndex = ctx.varIdMgr.makeFreshIndex(name);
    }

    IntegerFormula result = ctx.conv.fmgr.makeVariable(Types.VARIABLE_TYPE, name, useIndex);

    if (ctx.conv.direction == AnalysisDirection.BACKWARD) {
      ctx.varIdMgr.makeFreshIndex(name);
    }

    return result;
  }
}
