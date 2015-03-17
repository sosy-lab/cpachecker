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

import java.util.List;

import org.sosy_lab.cpachecker.core.counterexample.Model.TermType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl.AbstractFunctionFormulaManager;

import ap.parser.IExpression;
import ap.parser.IFunction;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;

class PrincessFunctionFormulaManager extends AbstractFunctionFormulaManager<IExpression, IFunction, TermType, PrincessEnvironment> {

  private final PrincessUnsafeFormulaManager unsafeManager;

  PrincessFunctionFormulaManager(
          PrincessFormulaCreator creator,
          PrincessUnsafeFormulaManager unsafeManager) {
    super(creator, unsafeManager);
    this.unsafeManager = unsafeManager;
  }

  @Override
  protected IExpression createUninterpretedFunctionCallImpl(
      IFunction pFuncDecl, List<IExpression> pArgs) {
    assert pArgs.size() == pFuncDecl.arity() : "functiontype has different number of args.";
    return unsafeManager.createUIFCallImpl(pFuncDecl, pArgs);
  }

  @Override
  protected IFunction declareUninterpretedFunctionImpl(
        String pName, TermType pReturnType, List<TermType> args) {
    assert pReturnType == TermType.Integer || pReturnType == TermType.Boolean : "Princess does not support return types other than Integer";
    assert FluentIterable.<TermType>from(args).filter(new Predicate<TermType>() {
      @Override
      public boolean apply(TermType pInput) {
        return pInput == TermType.Integer || pInput == TermType.Boolean;
      }}).size() == args.size() : "Princess does not support functions with variables, that have types other than integers, as parameters";

    return getFormulaCreator().getEnv().declareFun(pName, args.size(), pReturnType);
  }
}
