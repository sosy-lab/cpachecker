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

import ap.parser.IBinFormula;
import ap.parser.IBinJunctor;
import ap.parser.IBoolLit;
import ap.parser.IExpression;
import ap.parser.IFormula;
import ap.parser.IFormulaITE;
import ap.parser.INot;
import ap.parser.ITermITE;
import org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl.AbstractBooleanFormulaManager;

import static org.sosy_lab.cpachecker.util.predicates.princess.PrincessUtil.castToFormula;
import static org.sosy_lab.cpachecker.util.predicates.princess.PrincessUtil.castToTerm;

class PrincessBooleanFormulaManager extends AbstractBooleanFormulaManager<IExpression, PrincessEnvironment.Type, PrincessEnvironment> {

  PrincessBooleanFormulaManager(PrincessFormulaCreator creator) {
    super(creator);
  }

  @Override
  public IFormula makeVariableImpl(String varName) {
    return castToFormula(getFormulaCreator().makeVariable(getFormulaCreator().getBoolType(), varName));
  }

  @Override
  public IFormula makeBooleanImpl(boolean pValue) {
    return new IBoolLit(pValue);
  }

  @Override
  public IFormula equivalence(IExpression t1, IExpression t2) {
    return new IBinFormula(IBinJunctor.Eqv(), castToFormula(t1), castToFormula(t2));
  }

  @Override
  public boolean isTrue(IExpression t) {
    return PrincessUtil.isTrue(t);
  }

  @Override
  public boolean isFalse(IExpression t) {
    return PrincessUtil.isFalse(t);
  }

  @Override
  public IExpression ifThenElse(IExpression condition, IExpression t1, IExpression t2) {
    if (t1 instanceof IFormula) {
      return new IFormulaITE(castToFormula(condition), castToFormula(t1), castToFormula(t2));
    } else {
      return new ITermITE(castToFormula(condition), castToTerm(t1), castToTerm(t2));
    }
  }

  @Override
  public IFormula not(IExpression pBits) {
    return new INot(castToFormula(pBits));
  }

  @Override
  public IFormula and(IExpression t1, IExpression t2) {
    if (PrincessUtil.isTrue(t1)) { return castToFormula(t2); }
    if (PrincessUtil.isTrue(t2)) { return castToFormula(t1); }
    return new IBinFormula(IBinJunctor.And(), castToFormula(t1), castToFormula(t2));
  }

  @Override
  public IFormula or(IExpression t1, IExpression t2) {
    if (PrincessUtil.isFalse(t1)) { return castToFormula(t2); }
    if (PrincessUtil.isFalse(t2)) { return castToFormula(t1); }
    return new IBinFormula(IBinJunctor.Or(), castToFormula(t1), castToFormula(t2));
  }

  @Override
  public IFormula xor(IExpression t1, IExpression t2) {
    return new INot(new IBinFormula(IBinJunctor.Eqv(), castToFormula(t1), castToFormula(t2)));
  }

  @Override
  public boolean isNot(IExpression pBits) {
    return pBits instanceof INot;
  }

  @Override
  public boolean isAnd(IExpression pBits) {
    return PrincessUtil.isAnd(pBits);
  }

  @Override
  public boolean isOr(IExpression pBits) {
    return PrincessUtil.isOr(pBits);
  }

  @Override
  public boolean isXor(IExpression pBits) {
    return PrincessUtil.isXor(pBits);
  }

  @Override
  protected boolean isEquivalence(IExpression pBits) {
    return PrincessUtil.isEqual(pBits);
  }

  @Override
  protected boolean isImplication(IExpression pFormula) {
    return PrincessUtil.isImplication(pFormula);
  }

  @Override
  protected boolean isIfThenElse(IExpression pBits) {
    return PrincessUtil.isIfThenElse(pBits);
  }
}
