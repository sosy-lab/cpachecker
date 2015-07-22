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

import static com.google.common.base.Preconditions.checkArgument;
import static org.sosy_lab.cpachecker.util.predicates.smtInterpol.SmtInterpolUtil.toTermArray;

import java.util.List;

import org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl.AbstractUnsafeFormulaManager;

import com.google.common.collect.ImmutableList;

import de.uni_freiburg.informatik.ultimate.logic.ApplicationTerm;
import de.uni_freiburg.informatik.ultimate.logic.LetTerm;
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
    assert !(pT instanceof LetTerm)
        : "Formulas used by CPAchecker are expected to not have LetTerms."
            + " Check how this formula was created: " + pT;
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
  protected Term replaceArgsAndName(Term t, String pNewName, List<Term> pNewArgs) {
    if (isVariable(t)) {
      checkArgument(pNewArgs.isEmpty());
      return getFormulaCreator().makeVariable(t.getSort(), pNewName);

    } else if (isUF(t)) {
      ApplicationTerm at = (ApplicationTerm) t;
      Term[] args = at.getParameters();
      Sort[] sorts = new Sort[args.length];
      for (int i = 0; i < sorts.length; i++) {
        sorts[i] = args[i].getSort();
      }
      getFormulaCreator().getEnv().declareFun(pNewName, sorts, t.getSort());
      return createUIFCallImpl(pNewName, toTermArray(pNewArgs));
    } else {
      throw new IllegalArgumentException("The Term " + t + " has no name!");
    }
  }

  @Override
  protected List<Term> splitNumeralEqualityIfPossible(Term pF) {
    if (SmtInterpolUtil.isFunction(pF, "=") && SmtInterpolUtil.getArity(pF) == 2) {
      Term arg0 = SmtInterpolUtil.getArg(pF, 0);
      Term arg1 = SmtInterpolUtil.getArg(pF, 1);
      assert arg0.getSort().equals(arg1.getSort());
      if (!SmtInterpolUtil.isBoolean(arg0)) {
        return ImmutableList.of(
            getFormulaCreator().getEnv().term("<=", arg0, arg1),
            getFormulaCreator().getEnv().term("<=", arg1, arg0)
        );
      }
    }
    return ImmutableList.of(pF);
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

  @Override
  protected Term substitute(Term expr, List<Term> substituteFrom, List<Term> substituteTo) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Term simplify(Term pF) {
    return getFormulaCreator().getEnv().simplify(pF);
  }

  @Override
  protected boolean isQuantification(Term pT) {
    return false;
  }

  @Override
  protected Term getQuantifiedBody(Term pT) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected Term replaceQuantifiedBody(Term pF, Term pBody) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected boolean isFreeVariable(Term pT) {
    return isVariable(pT);
  }

  @Override
  protected boolean isBoundVariable(Term pT) {
    return false;
  }

}
