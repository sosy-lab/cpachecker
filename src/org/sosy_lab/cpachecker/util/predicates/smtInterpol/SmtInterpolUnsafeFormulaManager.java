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

import java.util.List;

import org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl.AbstractUnsafeFormulaManager;

import de.uni_freiburg.informatik.ultimate.logic.ApplicationTerm;
import de.uni_freiburg.informatik.ultimate.logic.Sort;
import de.uni_freiburg.informatik.ultimate.logic.Term;

class SmtInterpolUnsafeFormulaManager extends AbstractUnsafeFormulaManager<Term, Sort, SmtInterpolEnvironment> {

  SmtInterpolUnsafeFormulaManager(SmtInterpolFormulaCreator pCreator) {
    super(pCreator);
  }

  /** ApplicationTerms can be wrapped with "|".
   * This function removes those chars. */
  static String dequote(String s) {
   return s.replace("|", "");
  }

 /** ApplicationTerms can be wrapped with "|".
   * This function replaces those chars with "\"". */
  // TODO: Check where this was used in the past.
  @SuppressWarnings("unused")
  private static String convertQuotes(String s) {
    return s.replace("|", "\"");
  }

  @Override
  public boolean isAtom(Term t) {
    return SmtInterpolUtil.isAtom(t);
  }

  @Override
  public int getArity(Term pT) {
    return SmtInterpolUtil.getArity(pT);
  }

  @Override
  public Term getArg(Term pT, int pN) {
    return SmtInterpolUtil.getArg(pT, pN);
  }

  @Override
  public boolean isVariable(Term pT) {
    return SmtInterpolUtil.isVariable(pT);
  }

  @Override
  public boolean isUF(Term t) {
    return SmtInterpolUtil.isUIF(t);
  }

  @Override
  public String getName(Term t) {
    if (isVariable(t)) {
      return dequote(t.toString());
    } else if (isUF(t)) {
      return ((ApplicationTerm)t).getFunction().getName();
    } else {
      throw new IllegalArgumentException("The Term " + t + " has no name!");
    }
  }

  @Override
  public Term replaceArgs(Term pT, List<Term> newArgs) {
    return SmtInterpolUtil.replaceArgs(getFormulaCreator().getEnv(), pT, SmtInterpolUtil.toTermArray(newArgs));
  }

  @Override
  public Term replaceName(Term t, String pNewName) {

    if (isVariable(t)) {
      return getFormulaCreator().makeVariable(t.getSort(), pNewName);
    } else if (isUF(t)) {
      ApplicationTerm at = (ApplicationTerm) t;
      Term[] args = at.getParameters();
      Sort[] sorts = new Sort[args.length];
      for (int i = 0; i < sorts.length; i++) {
        sorts[i] = args[i].getSort();
      }
      getFormulaCreator().getEnv().declareFun(pNewName, sorts, t.getSort());
      return createUIFCallImpl(pNewName, args);
    } else {
      throw new IllegalArgumentException("The Term " + t + " has no name!");
    }
  }

  Term createUIFCallImpl(String funcDecl, Term[] args) {
    Term ufc = getFormulaCreator().getEnv().term(funcDecl, args);
    assert SmtInterpolUtil.isUIF(ufc);
    return ufc;
  }

  @Override
  public boolean isNumber(Term pT) {
    return SmtInterpolUtil.isNumber(pT);
  }
}
