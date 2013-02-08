/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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

import static com.google.common.collect.FluentIterable.from;

import java.util.Arrays;
import java.util.List;

import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FunctionFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FunctionFormulaType;

import com.google.common.base.Function;


public class ReplaceHelperFunctionFormulaManager implements FunctionFormulaManager {


  private Function<FormulaType<? extends Formula>, FormulaType<? extends Formula>> unwrapTypes;
  private FunctionFormulaManager rawFunctionFormulaManager;
  private ReplacingFormulaManager replaceManager;

  public ReplaceHelperFunctionFormulaManager(
      ReplacingFormulaManager replaceFormulaManager,
      FunctionFormulaManager pFunctionFormulaManager,
      Function<FormulaType<? extends Formula>,FormulaType<? extends Formula>> unwrapTypes
      ) {
    this.replaceManager = replaceFormulaManager;
    this.rawFunctionFormulaManager = pFunctionFormulaManager;
    this.unwrapTypes = unwrapTypes;
  }

  class ReplaceFunctionFormulaType<T extends Formula> extends FunctionFormulaType<T> {

    private FunctionFormulaType<? extends Formula> wrapped;
    private List<FormulaType<? extends Formula>> args;
    private FormulaType<T> ret;

    public ReplaceFunctionFormulaType(
        FunctionFormulaType<? extends Formula> wrapped,
        FormulaType<T> retType,
        List<FormulaType<? extends Formula>> pArgs){
      this.wrapped = wrapped;
      this.ret = retType;
      this.args = pArgs;
    }

    public FunctionFormulaType<? extends Formula> getWrapped(){
      return wrapped;
    }

    @Override
    public Class<T> getInterfaceType() {
      return ret.getInterfaceType();
    }

    @Override
    public List<FormulaType<?>> getArgumentTypes() {
      return args;
    }

    @Override
    public FormulaType<T> getReturnType() {
      return ret;
    }

    @Override
    public String toString() {
      String args = "";
      for (FormulaType<? extends Formula> arg : this.args) {
        args += arg.toString() + ",";
      }

      return "(" + ret.toString() + ") func(" + args.substring(0, args.length() - 1) + ")";
    }
  }

  @Override
  public <T extends Formula> FunctionFormulaType<T> createFunction(String pName, FormulaType<T> pReturnType,
      List<FormulaType<? extends Formula>> pArgs) {

    List<FormulaType<? extends Formula>> newArgs =
        from(pArgs).transform(unwrapTypes).toImmutableList();
    FormulaType<?> ret = unwrapTypes.apply(pReturnType);
    FunctionFormulaType<?> funcType = rawFunctionFormulaManager.createFunction(pName, ret, newArgs);

    return new ReplaceFunctionFormulaType<>(funcType, pReturnType, pArgs);
  }

  @Override
  public <T extends Formula> FunctionFormulaType<T> createFunction(
      String pName,
      FormulaType<T> pReturnType,
      FormulaType<?>... pArgs) {

    return createFunction(pName, pReturnType, Arrays.asList(pArgs));
  }

  @Override
  public <T extends Formula> T createUninterpretedFunctionCall(
      FunctionFormulaType<T> pFuncType, List<? extends Formula> pArgs) {
    ReplaceFunctionFormulaType<T> rep = (ReplaceFunctionFormulaType<T>)pFuncType;

    List<Formula> newArgs =
        from(pArgs).transform(
            new Function<Formula, Formula>() {
              @Override
              public Formula apply(Formula pArg0) {
                return replaceManager.unwrap(pArg0);
              }
            }).toImmutableList();

    Formula f = rawFunctionFormulaManager.createUninterpretedFunctionCall(rep.wrapped, newArgs);

    return replaceManager.wrap(pFuncType.getReturnType(), f);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends Formula> boolean isUninterpretedFunctionCall(FunctionFormulaType<T> pFuncType, T pF) {

    ReplaceFunctionFormulaType<T> rep = (ReplaceFunctionFormulaType<T>)pFuncType;
    return
        rawFunctionFormulaManager.isUninterpretedFunctionCall((FunctionFormulaType<Formula>)rep.wrapped, replaceManager.unwrap(pF));
  }

}
