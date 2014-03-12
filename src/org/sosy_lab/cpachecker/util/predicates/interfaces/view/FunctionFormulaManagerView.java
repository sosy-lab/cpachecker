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
package org.sosy_lab.cpachecker.util.predicates.interfaces.view;

import static com.google.common.collect.FluentIterable.from;

import java.util.Arrays;
import java.util.List;

import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FunctionFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FunctionFormulaType;

import com.google.common.base.Function;


public class FunctionFormulaManagerView extends AbstractBaseManagerView implements FunctionFormulaManager {

  private final FunctionFormulaManager manager;

  public FunctionFormulaManagerView(FunctionFormulaManager pManager) {
    this.manager = pManager;
  }

  @Override
  public <T extends Formula> FunctionFormulaType<T> createFunction(
      String pName,
      FormulaType<T> pReturnType,
      List<FormulaType<?>> pArgs) {
    return manager.createFunction(pName, pReturnType, pArgs);
  }

  @Override
  public <T extends Formula> FunctionFormulaType<T> createFunction(String pName, FormulaType<T> pReturnType,
      FormulaType<?>... pArgs) {
    return createFunction(pName, pReturnType, Arrays.asList(pArgs));
  }

  public <T extends Formula> T createFuncAndCall(
      String pName, int idx, FormulaType<T> pReturnType, List<Formula> pArgs) {
    String name = FormulaManagerView.makeName(pName, idx);
    return createFuncAndCall(name, pReturnType, pArgs);
  }
  public <T extends Formula> T createFuncAndCall(String name, FormulaType<T> pReturnType, List<Formula> pArgs) {
    final FormulaManagerView viewManager = getViewManager();

    List<FormulaType<?>> argTypes = from(pArgs).
      transform(
          new Function<Formula, FormulaType<?>>() {
            @Override
            public FormulaType<?> apply(Formula pArg0) {
              return viewManager.getFormulaType(pArg0);
            }}).toList();


    FunctionFormulaType<T> funcType = createFunction(name, pReturnType, argTypes);
    return createUninterpretedFunctionCall(funcType, pArgs);
  }

  @Override
  public <T extends Formula> T createUninterpretedFunctionCall(FunctionFormulaType<T> pFuncType, List<? extends Formula> pArgs) {
    final FormulaManagerView viewManager = getViewManager();
    List<Formula> args =
        from(pArgs)
        .transform(
            new Function<Formula, Formula>() {
              @Override
              public Formula apply(Formula pArg0) {
                return viewManager.extractFromView(pArg0);
              }}).toList();

    return viewManager.wrapInView(manager.createUninterpretedFunctionCall(pFuncType, args));
  }

  @Override
  public boolean isUninterpretedFunctionCall(FunctionFormulaType<?> pFuncType, Formula pF) {
    FormulaManagerView viewManager = getViewManager();
    return manager.isUninterpretedFunctionCall(pFuncType, viewManager.extractFromView(pF));
  }

}
