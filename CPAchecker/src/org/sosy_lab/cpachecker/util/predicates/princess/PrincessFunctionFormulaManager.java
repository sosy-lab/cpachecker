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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.FluentIterable.from;

import java.util.List;

import org.sosy_lab.cpachecker.util.predicates.TermType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl.AbstractFunctionFormulaManager;

import ap.parser.IExpression;
import ap.parser.IFunction;

import com.google.common.base.Predicates;

class PrincessFunctionFormulaManager extends AbstractFunctionFormulaManager<IExpression, IFunction, TermType, PrincessEnvironment> {

  PrincessFunctionFormulaManager(
      PrincessFormulaCreator creator,
      PrincessUnsafeFormulaManager unsafe) {
    super(creator, unsafe);
  }

  @Override
  protected IExpression createUninterpretedFunctionCallImpl(
      IFunction pFuncDecl, List<IExpression> pArgs) {
    return getFormulaCreator().getEnv().makeFunction(pFuncDecl, pArgs);
  }

  @Override
  protected IFunction declareUninterpretedFunctionImpl(
        String pName, TermType pReturnType, List<TermType> args) {
    checkArgument(pReturnType == TermType.Integer || pReturnType == TermType.Boolean,
        "Princess does not support return types of UFs other than Integer");
    checkArgument(from(args).allMatch(Predicates.equalTo(TermType.Integer)),
        "Princess does not support argument types of UFs other than Integer");

    return getFormulaCreator().getEnv().declareFun(pName, args.size(), pReturnType);
  }
}
