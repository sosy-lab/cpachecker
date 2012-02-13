/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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

import java.util.HashSet;
import java.util.Set;

import de.uni_freiburg.informatik.ultimate.logic.ApplicationTerm;
import de.uni_freiburg.informatik.ultimate.logic.ConstantTerm;
import de.uni_freiburg.informatik.ultimate.logic.FunctionSymbol;
import de.uni_freiburg.informatik.ultimate.logic.SMTLIBException;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Sort;
import de.uni_freiburg.informatik.ultimate.logic.Term;


/** This is a Class similiar to Mathsat-NativeApi,
 *  it contains some useful functions. */
public class SmtInterpolUtil {

  static boolean log = false; // debug

  /* comment in mathsat-class:
  msat_term_is_atom (msat_term t): nonzero if t is an atom,
  i.e. either a boolean variable or a relation between terms.
  TODO: what is atom?? */
  public static boolean isAtom(Script script, Term t) {
    boolean is = !isAnd(script, t) && !isOr(script, t) && !isNot(script, t);
    if (log) System.out.println("   isAtom (" + is +"): " + t);
    return is;
  }

  public static boolean isVariable(Term t) { // TODO cpa-variable != smt-termvariable, working??
    assert t != null;
    boolean is = (t instanceof ApplicationTerm)
        && ((ApplicationTerm) t).getParameters().length == 0;
    if (log) System.out.println("   isVariable (" + is +"): " + t);
    return is;
  }

  /** check for ConstantTerm with Number or
   * ApplicationTerm with negative Number */
  public static boolean isNumber(Term t) {
    boolean is = false;

    // ConstantTerm with Number --> "123"
    if (t instanceof ConstantTerm
        && ((ConstantTerm) t).getValue() instanceof Number) {
      is = true;

    } else if (t instanceof ApplicationTerm) {
      ApplicationTerm at = (ApplicationTerm) t;

      // ApplicationTerm with negative Number --> "(- 123)"
      if ("-".equals(at.getFunction().getName())
          && (at.getParameters().length == 1)
          && isNumber(at.getParameters()[0])) {
        is = true;

        // ApplicationTerm with Division --> "(/ 1 5)"
      } else if ("/".equals(at.getFunction().getName())
          && (at.getParameters().length == 2)
          && isNumber(at.getParameters()[0])
          && isNumber(at.getParameters()[1])) {
        is = true;
      }
    }

    // TODO hex or binary data, string?
    if (log)
      System.out.println("   isNumber (" + is + "): " + t);
    return is;
  }

  /** converts a term to a number,
   * currently only Double is supported. */
  public static double toNumber(Term t) {
    assert isNumber(t) : "term is not a number: " + t;

    // ConstantTerm with Number --> "123"
    if (t instanceof ConstantTerm) {
      Object value = ((ConstantTerm) t).getValue();
      if (value instanceof Number) {
        return ((Number) value).doubleValue();
      }

      // ApplicationTerm with negative Number --> "-123"
    } else if (t instanceof ApplicationTerm) {
      ApplicationTerm at = (ApplicationTerm) t;

      if ("-".equals(at.getFunction().getName())) {
        return - toNumber(at.getParameters()[0]);
      } else if ("/".equals(at.getFunction().getName())) {
        return toNumber(at.getParameters()[1]) /
          toNumber(at.getParameters()[1]);
      }
    }

    throw new NumberFormatException("unknown format of numeric term: " + t);
  }

  public static boolean isBoolean(Script script, Term t) {
    boolean is = (t instanceof ApplicationTerm)
        && script.getTheory().getBooleanSort() == ((ApplicationTerm) t).getSort();
    if (log) System.out.println("   isBoolean (" + is +"): " + t);
    return is;
  }

  /** t1 and t2
   * @param theory */
  public static boolean isAnd(Script script, Term t) {
    boolean is = (t instanceof ApplicationTerm)
        && script.getTheory().m_And == ((ApplicationTerm) t).getFunction();
    if (log) System.out.println("   isAnd (" + is +"): " + t);
    return is;
  }

  /** t1 or t2 */
  public static boolean isOr(Script script, Term t) {
    boolean is = (t instanceof ApplicationTerm)
        && script.getTheory().m_Or == ((ApplicationTerm) t).getFunction();
    if (log) System.out.println("   isOr (" + is +"): " + t);
    return is;
  }

  /** not t */
  public static boolean isNot(Script script, Term t) {
    boolean is = (t instanceof ApplicationTerm)
        && script.getTheory().m_Not == ((ApplicationTerm) t).getFunction();
    if (log) System.out.println("   isNot (" + is +"): " + t);
    return is;
  }

  /** t1 = t2 */
  public static boolean isEqual(Script script, Term t) {
    boolean is = (t instanceof ApplicationTerm)
        && script.getTheory().m_Equals.toString().equals(
            ((ApplicationTerm) t).getFunction().getTheory().toString()); // TODO easier way?
    if (log) System.out.println("   isEqual (" + is +"): " + t);
    return is;
  }

  public static boolean isUIF(Script script, Term t) {
    if (t instanceof ApplicationTerm) {
      ApplicationTerm at = (ApplicationTerm) t;
      String name = at.getFunction().toString();
      Term[] params = at.getParameters();
      Sort[] sorts = new Sort[params.length];
      for (int i = 0; i < params.length; i++) {
        sorts[i] = params[i].getSort();
      }
      if (log) System.out.println("   isUIF (" + t +"): "
          + script.getTheory().getFunction(name, sorts));
      return script.getTheory().getFunction(name, sorts) != null;
    } else
      return false;
  }

  public static int getArity(Term t) {
    if (t instanceof ApplicationTerm) {
      return ((ApplicationTerm) t).getParameters().length;
    } else
      return 0;
  }

  public static Term getArg(Term t, int i) {
    if (t instanceof ApplicationTerm) {
      return ((ApplicationTerm) t).getParameters()[i];
    } else
      return null;
  }

  public static boolean isTrue(Script script, Term t) {
    try {
      boolean isTrue = script.getTheory().TRUE == script.simplifyTerm(t);
      if (log) System.out.println("   isTrue (" + t +"): " + isTrue);
      return isTrue;
    } catch (SMTLIBException e) {
      e.printStackTrace();
      return false;
    }
  }

  public static boolean isFalse(Script script, Term t) {
    try {
      boolean isFalse = script.getTheory().FALSE == script.simplifyTerm(t);
      if (log) System.out.println("   isTrue (" + t +"): " + isFalse);
      return isFalse;
      } catch (SMTLIBException e) {
      e.printStackTrace();
      return false;
    }
  }

  /** this function creates a new Term with the same function and new parameters. */
  public static Term replaceArgs(Script script, Term t, Term[] newParams) {
    if (t instanceof ApplicationTerm) {
      ApplicationTerm at = (ApplicationTerm) t;
      assert at.getParameters().length == newParams.length;

      FunctionSymbol funcSymb = at.getFunction();
      try {
        return script.term(funcSymb.getName(), newParams);
      } catch (SMTLIBException e) {
        e.printStackTrace();
        return null;
      }
    } else { // numeral
      return t;
    }
  }

  /** this function returns all variables in the terms.
   * Doubles are removed. */
  public static Term[] getVars(Iterable<Term> termList) {
    Set<Term> vars = new HashSet<Term>();
    for (Term t : termList) {
      getVars(t, vars);
    }
    return vars.toArray(new Term[0]);
  }

  private static void getVars(Term t, Set<Term> vars) {
    if (t instanceof ApplicationTerm &&
        (t != t.getTheory().TRUE) && (t != t.getTheory().FALSE)) {
      Term[] params = ((ApplicationTerm) t).getParameters();
      if (params.length == 0) { // no params --> term is variable
        vars.add(t);

      } else {
        for (Term innerTerm : params) { // recursive call
          getVars(innerTerm, vars);
        }
      }
    }
  }
}
