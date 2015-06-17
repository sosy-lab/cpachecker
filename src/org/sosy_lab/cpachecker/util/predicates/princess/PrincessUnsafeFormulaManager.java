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

import static org.sosy_lab.cpachecker.util.predicates.princess.PrincessUtil.isBoolean;
import static scala.collection.JavaConversions.asJavaCollection;

import java.util.List;

import org.sosy_lab.cpachecker.util.predicates.TermType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl.AbstractUnsafeFormulaManager;

import ap.basetypes.IdealInt;
import ap.parser.BooleanCompactifier;
import ap.parser.IExpression;
import ap.parser.IFormula;
import ap.parser.IFunApp;
import ap.parser.IIntFormula;
import ap.parser.IIntLit;
import ap.parser.IIntRelation;
import ap.parser.PartialEvaluator;

import com.google.common.collect.ImmutableList;

class PrincessUnsafeFormulaManager extends AbstractUnsafeFormulaManager<IExpression, TermType, PrincessEnvironment> {

  PrincessUnsafeFormulaManager(PrincessFormulaCreator pCreator) {
    super(pCreator);
  }

  @Override
  public boolean isAtom(IExpression t) {
    return PrincessUtil.isAtom(t);
  }

  @Override
  public int getArity(IExpression pT) {
    return PrincessUtil.getArity(pT);
  }

  @Override
  public IExpression getArg(IExpression pT, int pN) {
    return PrincessUtil.getArg(pT, pN);
  }

  @Override
  public boolean isVariable(IExpression pT) {
    return PrincessUtil.isVariable(pT);
  }

  @Override
  public boolean isUF(IExpression t) {
    return PrincessUtil.isUIF(t);
  }

  @Override
  public String getName(IExpression t) {
    if (isVariable(t)) {
      return t.toString();
    } else if (isUF(t)) {
      return ((IFunApp)t).fun().name();
    } else {
      throw new IllegalArgumentException("The Term " + t + " has no name!");
    }
  }

  @Override
  public IExpression replaceArgs(IExpression pT, List<IExpression> newArgs) {
    return PrincessUtil.replaceArgs(getFormulaCreator().getEnv(), pT, newArgs);
  }

  @Override
  public IExpression replaceName(IExpression t, String pNewName) {

    if (isVariable(t)) {
      return getFormulaCreator().makeVariable(isBoolean(t) ? TermType.Boolean : TermType.Integer,
                                              pNewName);

    } else if (isUF(t)) {
      IFunApp fun = (IFunApp) t;
      List<IExpression> args = ImmutableList.<IExpression>copyOf(asJavaCollection(fun.args()));
      PrincessEnvironment env = getFormulaCreator().getEnv();
      TermType returnType = env.getReturnTypeForFunction(fun.fun());
      return env.makeFunction(env.declareFun(pNewName, args.size(), returnType), args);

    } else {
      throw new IllegalArgumentException("The Term " + t + " has no name!");
    }
  }

  @Override
  protected List<? extends IExpression> splitNumeralEqualityIfPossible(IExpression pF) {
    // Princess does not support Equal.
    // Formulas are converted from "a==b" to "a+(-b)==0".
    if (pF instanceof IIntFormula && ((IIntFormula)pF).rel() == IIntRelation.EqZero()) {
      return ImmutableList.of(
          ((IIntFormula)pF).t().$less$eq(new IIntLit(IdealInt.ZERO())),
          ((IIntFormula)pF).t().$greater$eq(new IIntLit(IdealInt.ZERO()))
      );
    }
    return ImmutableList.of(pF);
  }

  @Override
  public boolean isNumber(IExpression pT) {
    return PrincessUtil.isNumber(pT);
  }

  @Override
  protected IExpression substitute(IExpression expr, List<IExpression> substituteFrom, List<IExpression> substituteTo) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected IExpression simplify(IExpression f) {
    // TODO this method is not tested, check it!
    if (f instanceof IFormula) {
      f = BooleanCompactifier.apply((IFormula)f);
    }
    return PartialEvaluator.apply(f);
  }

  @Override
  protected boolean isQuantification(IExpression pT) {
    return false;
  }

  @Override
  protected IExpression getQuantifiedBody(IExpression pT) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected IExpression replaceQuantifiedBody(IExpression pF, IExpression pBody) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected boolean isFreeVariable(IExpression pT) {
    return isVariable(pT);
  }

  @Override
  protected boolean isBoundVariable(IExpression pT) {
    return false;
  }

}
