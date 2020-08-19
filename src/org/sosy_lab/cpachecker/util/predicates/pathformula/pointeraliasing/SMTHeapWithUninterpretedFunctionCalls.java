/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import static com.google.common.base.Preconditions.checkArgument;

import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FunctionFormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;

/**
 * SMT heap representation with uninterpreted function calls.
 */
public class SMTHeapWithUninterpretedFunctionCalls implements SMTHeap {

  private final FunctionFormulaManagerView ffmgr;
  private final FormulaManagerView formulaManager;

  public SMTHeapWithUninterpretedFunctionCalls(FormulaManagerView pFormulaManager){
    formulaManager = pFormulaManager;
    ffmgr = formulaManager.getFunctionFormulaManager();
  }


  @Override
  public <I extends Formula, E extends Formula> BooleanFormula makePointerAssignment(
      String targetName,
      FormulaType<?> pTargetType,
      int oldIndex,
      int newIndex,
      I address,
      E value) {
    FormulaType<E> targetType = formulaManager.getFormulaType(value);
    checkArgument(pTargetType.equals(targetType));
    final Formula lhs =
        ffmgr.declareAndCallUninterpretedFunction(targetName, newIndex, targetType, address);
    return formulaManager.assignment(lhs, value);
  }

  @Override
  public <I extends Formula, E extends Formula> E makePointerDereference(
      String targetName, FormulaType<E> targetType, I address) {
    return ffmgr.declareAndCallUF(targetName, targetType, address);
  }

  @Override
  public <I extends Formula, V extends Formula> V makePointerDereference(
      String targetName,
      FormulaType<V> targetType,
      int ssaIndex,
      I address) {
    return ffmgr.declareAndCallUninterpretedFunction(targetName, ssaIndex, targetType, address);
  }
}
