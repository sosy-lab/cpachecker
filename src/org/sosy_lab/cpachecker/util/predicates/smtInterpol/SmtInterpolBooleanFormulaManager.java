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
package org.sosy_lab.cpachecker.util.predicates.smtInterpol;

import org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl.AbstractBooleanFormulaManager;

import de.uni_freiburg.informatik.ultimate.logic.ApplicationTerm;
import de.uni_freiburg.informatik.ultimate.logic.Term;


class SmtInterpolBooleanFormulaManager extends AbstractBooleanFormulaManager<Term> {

  private SmtInterpolFormulaCreator creator;
  private SmtInterpolEnvironment env;

  SmtInterpolBooleanFormulaManager(
      SmtInterpolFormulaCreator creator) {
    super(creator);
    this.creator = creator;
    this.env = creator.getEnv();
  }

  public static SmtInterpolBooleanFormulaManager create(SmtInterpolFormulaCreator creator) {
    return new SmtInterpolBooleanFormulaManager(creator);
  }


  @Override
  public Term makeVariableImpl(String varName) {
    return creator.makeVariable(creator.getBoolType(), varName);
  }

  @Override
  public Term makeBooleanImpl(boolean pValue) {
    Term t ;
    if (pValue) {
      t = env.getTrueTerm();
    } else {
      t = env.getFalseTerm();
    }
    return t;
  }

  @Override
  public Term equivalence(Term t1, Term t2) {
    assert SmtInterpolUtil.isBoolean(t1) && SmtInterpolUtil.isBoolean(t2) :
      "Cannot make equivalence of non-boolean terms:\nTerm 1:\n" +
      t1.toStringDirect() + "\nTerm 2:\n" + t2.toStringDirect();
    return env.term("=", t1, t2);
  }

  @Override
  public boolean isTrue(Term t) {
    return SmtInterpolUtil.isTrue(t);
  }

  @Override
  public boolean isFalse(Term t) {
    return SmtInterpolUtil.isFalse(t);
  }

  @Override
  public Term ifThenElse(Term condition, Term t1, Term t2) {
    return env.term("ite", condition, t1, t2);
  }

  @Override
  public Term not(Term pBits) {
    // simplify term (not not t)
    if (isNot(pBits)) {
      return ((ApplicationTerm) pBits).getParameters()[0];
    } else {
      return env.term("not", pBits);
    }
  }

  @Override
  public Term and(Term pBits1, Term pBits2) {

    if (pBits1 == pBits2) { return pBits1;}
    Term trueTerm = env.getTrueTerm();
    if (pBits1 == trueTerm) { return pBits2;}
    if (pBits2 == trueTerm) { return pBits1;}
    Term t = env.term("and", pBits1, pBits2);
    return SmtInterpolUtil.simplify(env, t);
  }

  @Override
  public Term or(Term pBits1, Term pBits2) {
    Term falseTerm = env.getFalseTerm();
    if (pBits1 == falseTerm) { return pBits2;}
    if (pBits2 == falseTerm) { return pBits1;}
    Term t = env.term("or", pBits1, pBits2);
    return SmtInterpolUtil.simplify(env, t);
  }

  @Override
  public Term xor(Term pBits1, Term pBits2) {
    return not(env.term("=", pBits1, pBits2));
  }

  @Override
  public boolean isNot(Term pBits) {
    return SmtInterpolUtil.isNot(pBits);
  }

  @Override
  public boolean isAnd(Term pBits) {
    return SmtInterpolUtil.isAnd(pBits);
  }

  @Override
  public boolean isOr(Term pBits) {
    return SmtInterpolUtil.isOr(pBits);
  }

  @Override
  public boolean isXor(Term pBits) {
    boolean isNot = SmtInterpolUtil.isNot(pBits);
    if (!isNot) {
      return false;
    }
    Term arg = SmtInterpolUtil.getArg(pBits, 0);
    return SmtInterpolUtil.isEqual(arg);
  }

  @Override
  protected boolean isEquivalence(Term pBits) {
    return SmtInterpolUtil.isEqual(pBits);
  }

  @Override
  protected boolean isImplication(Term pFormula) {
    return SmtInterpolUtil.isImplication(pFormula);
  }

  @Override
  protected boolean isIfThenElse(Term pBits) {
    return SmtInterpolUtil.isIfThenElse(pBits);
  }


}
