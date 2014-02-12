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
package org.sosy_lab.cpachecker.util.predicates.smtInterpol;

import java.util.Arrays;
import java.util.List;

import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FunctionFormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl.AbstractFunctionFormulaManager;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

import de.uni_freiburg.informatik.ultimate.logic.Sort;
import de.uni_freiburg.informatik.ultimate.logic.Term;

class SmtInterpolFunctionFormulaManager extends AbstractFunctionFormulaManager<Term, Sort, SmtInterpolEnvironment> {

  private final SmtInterpolUnsafeFormulaManager unsafeManager;

  SmtInterpolFunctionFormulaManager(
      SmtInterpolFormulaCreator creator,
      SmtInterpolUnsafeFormulaManager unsafeManager) {
    super(creator, unsafeManager);
    this.unsafeManager = unsafeManager;
  }

  @Override
  public <TFormula extends Formula> Term createUninterpretedFunctionCallImpl(FunctionFormulaType<TFormula> pFuncType,
      List<Term> pArgs) {
    SmtInterpolFunctionType<TFormula> interpolType = (SmtInterpolFunctionType<TFormula>) pFuncType;
    Term[] args = SmtInterpolUtil.toTermArray(pArgs);
    String funcDecl = interpolType.getFuncDecl();
    return unsafeManager.createUIFCallImpl(funcDecl, args);
  }

  @Override
  public <T extends Formula> SmtInterpolFunctionType<T>
    createFunction(
        String pName,
        FormulaType<T> pReturnType,
        List<FormulaType<?>> pArgs) {
    FunctionFormulaType<T> formulaType
      = super.createFunction(pName, pReturnType, pArgs);


    List<Sort> types = Lists.transform(pArgs,
      new Function<FormulaType<?>, Sort>() {
        @Override
        public Sort apply(FormulaType<?> pArg0) {
          return toSolverType(pArg0);
        }
      });
    Sort[] msatTypes = types.toArray(new Sort[types.size()]);

    Sort returnType = toSolverType(pReturnType);
    getFormulaCreator().getEnv().declareFun(pName, msatTypes, returnType);

    return new SmtInterpolFunctionType<>(formulaType.getReturnType(), formulaType.getArgumentTypes(), pName);
  }

  @Override
  public <T extends Formula> SmtInterpolFunctionType<T> createFunction(
      String pName,
      FormulaType<T> pReturnType,
      FormulaType<?>... pArgs) {

    return createFunction(pName, pReturnType, Arrays.asList(pArgs));
  }

  @Override
  protected boolean isUninterpretedFunctionCall(FunctionFormulaType<?> pFuncType, Term f) {
    boolean isUf = unsafeManager.isUF(f);
    if (!isUf) {
      return false;
    }

    // TODO check if exactly the given func
    return isUf;
  }

}
