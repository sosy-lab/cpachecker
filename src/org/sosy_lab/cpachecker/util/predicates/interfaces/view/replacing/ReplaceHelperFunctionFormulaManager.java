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
package org.sosy_lab.cpachecker.util.predicates.interfaces.view.replacing;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.FluentIterable.from;

import java.util.Arrays;
import java.util.List;

import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FunctionFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FunctionFormulaType;

import com.google.common.base.Function;

class ReplaceHelperFunctionFormulaManager implements FunctionFormulaManager {

  private final Function<FormulaType<?>, FormulaType<?>> unwrapTypes;
  private final FunctionFormulaManager rawFunctionFormulaManager;
  private final ReplacingFormulaManager replaceManager;

  public ReplaceHelperFunctionFormulaManager(
      ReplacingFormulaManager replaceFormulaManager,
      FunctionFormulaManager pFunctionFormulaManager,
      Function<FormulaType<?>, FormulaType<?>> unwrapTypes
      ) {
    this.replaceManager = replaceFormulaManager;
    this.rawFunctionFormulaManager = pFunctionFormulaManager;
    this.unwrapTypes = unwrapTypes;
  }

  private static class ReplaceFunctionFormulaType<T extends Formula> extends FunctionFormulaType<T> {

    private final FunctionFormulaType<?> wrapped;

    ReplaceFunctionFormulaType(
        FunctionFormulaType<?> wrapped,
        FormulaType<T> pReturnType,
        List<FormulaType<?>> pArgumentTypes) {
      super(pReturnType, pArgumentTypes);
      this.wrapped = checkNotNull(wrapped);
    }
  }

  @Override
  public <T extends Formula> FunctionFormulaType<T> declareUninterpretedFunction(String pName, FormulaType<T> pReturnType,
      List<FormulaType<?>> pArgs) {

    List<FormulaType<?>> newArgs =
        from(pArgs).transform(unwrapTypes).toList();
    FormulaType<?> ret = unwrapTypes.apply(pReturnType);
    FunctionFormulaType<?> funcType = rawFunctionFormulaManager.declareUninterpretedFunction(pName, ret, newArgs);

    return new ReplaceFunctionFormulaType<>(funcType, pReturnType, pArgs);
  }

  @Override
  public <T extends Formula> FunctionFormulaType<T> declareUninterpretedFunction(
      String pName,
      FormulaType<T> pReturnType,
      FormulaType<?>... pArgs) {

    return declareUninterpretedFunction(pName, pReturnType, Arrays.asList(pArgs));
  }

  @Override
  public <T extends Formula> T callUninterpretedFunction(
      FunctionFormulaType<T> pFuncType, List<? extends Formula> pArgs) {
    ReplaceFunctionFormulaType<T> rep = (ReplaceFunctionFormulaType<T>)pFuncType;

    List<Formula> newArgs =
        from(pArgs).transform(
            new Function<Formula, Formula>() {
              @Override
              public Formula apply(Formula pArg0) {
                return replaceManager.unwrap(pArg0);
              }
            }).toList();

    Formula f = rawFunctionFormulaManager.callUninterpretedFunction(rep.wrapped, newArgs);

    return replaceManager.wrap(pFuncType.getReturnType(), f);
  }
}
