/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.princess;

import ap.parser.IExpression;
import ap.parser.IFunction;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FunctionFormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl.AbstractFunctionFormulaManager;

import java.util.Arrays;
import java.util.List;

class PrincessFunctionFormulaManager extends AbstractFunctionFormulaManager<IExpression, PrincessEnvironment.Type, PrincessEnvironment> {

  private final PrincessUnsafeFormulaManager unsafeManager;

  PrincessFunctionFormulaManager(
          PrincessFormulaCreator creator,
          PrincessUnsafeFormulaManager unsafeManager) {
    super(creator, unsafeManager);
    this.unsafeManager = unsafeManager;
  }

  @Override
  public <TFormula extends Formula> IExpression createUninterpretedFunctionCallImpl(FunctionFormulaType<TFormula> pFuncType,
      List<IExpression> pArgs) {
    PrincessFunctionType<TFormula> type = (PrincessFunctionType<TFormula>) pFuncType;
    assert pArgs.size() == type.getFuncDecl().getArgs().length : "functiontype has different number of args.";
    return unsafeManager.createUIFCallImpl(
            type.getFuncDecl().getFuncDecl(), type.getFuncDecl().getResultType(), pArgs);
  }

  @Override
  public <T extends Formula> PrincessFunctionType<T> createFunction(
        String pName, FormulaType<T> pReturnType, List<FormulaType<?>> pArgs) {

    PrincessEnvironment.Type[] types = new PrincessEnvironment.Type[pArgs.size()];
    for (int i = 0; i < types.length; i++) {
      types[i] = toSolverType(pArgs.get(i));
    }
    PrincessEnvironment.Type returnType = toSolverType(pReturnType);
    PrincessEnvironment.FunctionType funcDecl = getFormulaCreator().getEnv().declareFun(pName, returnType, types);

    return new PrincessFunctionType<>(pReturnType, pArgs, funcDecl);
  }

  @Override
  public <T extends Formula> PrincessFunctionType<T> createFunction(
      String pName,
      FormulaType<T> pReturnType,
      FormulaType<?>... pArgs) {

    return createFunction(pName, pReturnType, Arrays.asList(pArgs));
  }

  @Override
  protected boolean isUninterpretedFunctionCall(FunctionFormulaType<?> pFuncType, IExpression f) {
    boolean isUf = unsafeManager.isUF(f);
    if (!isUf) {
      return false;
    }

    // TODO check if exactly the given func
    return isUf;
  }

}
