/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.z3;

import java.util.List;

import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FunctionFormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl.AbstractFunctionFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl.AbstractUnsafeFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl.FormulaCreator;


public class Z3FunctionFormulaManager extends AbstractFunctionFormulaManager<Long> {

  public Z3FunctionFormulaManager(FormulaCreator<Long> pCreator, AbstractUnsafeFormulaManager<Long> pUnsafeManager) {
    super(pCreator, pUnsafeManager);
  }

  @Override
  protected <TFormula extends Formula> Long createUninterpretedFunctionCallImpl(
      FunctionFormulaType<TFormula> pFuncType, List<Long> pArgs) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public <T extends Formula> boolean isUninterpretedFunctionCall(FunctionFormulaType<T> pFuncType, Long pF) {
    // TODO Auto-generated method stub
    return false;
  }

}
