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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import scala.Enumeration;
import scala.collection.Iterator;
import scala.collection.JavaConversions;
import ap.basetypes.IdealInt;
import ap.parser.IAtom;
import ap.parser.IBinFormula;
import ap.parser.IBinJunctor;
import ap.parser.IBoolLit;
import ap.parser.IConstant;
import ap.parser.IExpression;
import ap.parser.IFormula;
import ap.parser.IFormulaITE;
import ap.parser.IFunApp;
import ap.parser.IFunction;
import ap.parser.IIntLit;
import ap.parser.INot;
import ap.parser.ITerm;
import ap.parser.ITermITE;

/** This is a Class similiar to Mathsat-NativeApi,
 *  it contains some useful static functions. */
class PrincessUtil {
  private PrincessUtil() { }

  /** ITerm is the arithmetic subclass of IExpression. */
  public static ITerm castToTerm(IExpression e) {
    return (ITerm) e;
  }

  /** IFormula is the boolean subclass of IExpression. */
  public static IFormula castToFormula(IExpression e) {
    return (IFormula) e;
  }

  /** A Term is an Atom, iff its function is no element of {"And", "Or", "Not"}.*/
  public static boolean isAtom(IExpression t) {
    boolean is = !isAnd(t) && !isOr(t) && !isNot(t) && !isImplication(t) && !isIfThenElse(t);
    assert is || isBoolean(t);
    return is;
  }

  public static boolean isVariable(IExpression t) {
    return t instanceof IAtom || t instanceof IConstant;
  }

  public static boolean isUIF(IExpression t) {
    return (t instanceof IFunApp);
  }

  /** check for ConstantTerm with Number or
   * ApplicationTerm with negative Number */
  public static boolean isNumber(IExpression t) {
    return t instanceof IIntLit;
  }

  /** converts a term to a number,
   * currently only Double is supported. */
  public static double toNumber(IExpression t) {
    assert isNumber(t) : "term is not a number: " + t;

    // ConstantTerm with Number --> "123"
    if (t instanceof IIntLit) {
      IdealInt value = ((IIntLit) t).value();
      return value.longValue();
    }

    throw new NumberFormatException("unknown format of numeric term: " + t);
  }

  public static boolean isBoolean(IExpression t) {
    return t instanceof IFormula;
  }

  public static boolean hasIntegerType(IExpression t) {
    return t instanceof ITerm;
  }

  /** t1 and t2 */
  public static boolean isAnd(IExpression t) {
    return isBinaryFunction(t, IBinJunctor.And());
  }

  /** t1 or t2 */
  public static boolean isOr(IExpression t) {
    return isBinaryFunction(t, IBinJunctor.Or());
  }

  /** not t */
  public static boolean isNot(IExpression t) {
    return t instanceof INot;
  }

  /** t1 => t2 */
  public static boolean isImplication(IExpression t) {
    // Princess does not support implication.
    // Formulas are converted from "a=>b" to "!a||b".
    return false;
  }

  /** t1 or t2 */
  public static boolean isXor(IExpression t) {
    // Princess does not support Xor.
    // Formulas are converted from "a^b" to "!(a<=>b)".
    return false;
  }

  /** (ite t1 t2 t3) */
  public static boolean isIfThenElse(IExpression t) {
    return t instanceof IFormulaITE // boolean args
        || t instanceof ITermITE;   // arithmetic args
  }

  /** t1 = t2 */
  public static boolean isEquivalence(IExpression t) {
    return isBinaryFunction(t, IBinJunctor.Eqv());
  }

  private static boolean isBinaryFunction(IExpression t, Enumeration.Value val) {
    return (t instanceof IBinFormula)
            && val == ((IBinFormula) t).j(); // j is the operator and Scala is evil!
  }

  public static int getArity(IExpression t) {
    return t.length();
  }

  public static IExpression getArg(IExpression t, int i) {
    assert i < getArity(t) : String.format("index %d out of bounds %d in expression %s", i, getArity(t), t);

    return t.apply(i);
    /*
    if (t instanceof IBinFormula) {
      return ((IBinFormula) t).apply(i);
    } else {
      return null;
    }
     */
  }

  public static boolean isTrue(IExpression t) {
    return t instanceof IBoolLit && ((IBoolLit)t).value();
  }

  public static boolean isFalse(IExpression t) {
    return t instanceof IBoolLit && !((IBoolLit)t).value();
  }

  /** this function creates a new Term with the same function and new parameters. */
  public static IExpression replaceArgs(PrincessEnvironment env, IExpression t, List<IExpression> newParams) {

    return t.update(JavaConversions.asScalaBuffer(newParams));

    /*
    if (t instanceof INot) {
      assert newParams.size() == 1;
      INot tt = (INot) t;
      assert tt.subformula().getClass() == newParams.get(0).getClass();
      return new INot((IFormula)newParams.get(0));

    } else if (t instanceof IBinFormula) {
      assert newParams.size() == 2;
      IBinFormula tt = (IBinFormula) t;
      assert tt.f1().getClass() == newParams.get(0).getClass();
      assert tt.f2().getClass() == newParams.get(1).getClass();
      return new IBinFormula(tt.j(), (IFormula)newParams.get(0), (IFormula)newParams.get(1));

    } else if (t instanceof IFormulaITE) {
      assert newParams.size() == 3;
      IFormulaITE tt = (IFormulaITE) t;
      assert tt.cond().getClass() == newParams.get(0).getClass();
      assert tt.left().getClass() == newParams.get(1).getClass();
      assert tt.right().getClass() == newParams.get(2).getClass();
      return new IFormulaITE((IFormula)newParams.get(0), (IFormula)newParams.get(1), (IFormula)newParams.get(2));

    } else if (t instanceof IPlus) {
      assert newParams.size() == 2;
      IPlus tt = (IPlus) t;
      assert tt.t1().getClass() == newParams.get(0).getClass();
      assert tt.t2().getClass() == newParams.get(1).getClass();
      return new IPlus((ITerm)newParams.get(0), (ITerm)newParams.get(1));

    } else {
      return t;
    }
    */
  }

  /** this function returns all variables in the terms.
   * Doubles are removed. */
  public static Set<IExpression> getVarsAndUIFs(Collection<IExpression> exprList) {
    Set<IExpression> result = new HashSet<>();
    Set<IExpression> seen = new HashSet<>();
    Set<IFunction> uifs = new HashSet<>();
    Deque<IExpression> todo = new ArrayDeque<>(exprList);

    while (!todo.isEmpty()) {
      IExpression t = todo.removeLast();
      if (!seen.add(t)) {
        continue;
      }

      if (isVariable(t)) {
        result.add(t);
        // this is a real variable we can skip here
        continue;

      } else if (isUIF(t) && uifs.add(((IFunApp)t).fun())) {
        result.add(t);
      }

      if (t.length() > 0) {
        Iterator<IExpression> it = t.iterator();
        while (it.hasNext()) {
          todo.add(it.next());
        }
      }
    }
    return result;
  }

  /**
   * This method introduces let statements (abbreviations in Princess) for all subtrees
   * of the given term tree which are equal, such that each subtree of the resulting
   * tree is unique afterwards
   * @param term
   * @return
   */
  public static IExpression let(IExpression expr, PrincessEnvironment env) {
    IExpression lettedExp = replaceCommonExpressionsInTree(expr, getCommonSubTreeExpressions(expr), env, new HashMap<IExpression, IExpression>());
    assert areEqualTerms(expr, lettedExp, env);
    return lettedExp;
  }

  /**
   * Compares two expressions for equality by checking the negated equivalence
   * of both for satisfiability.
   */
  private static boolean areEqualTerms(IExpression expr1, IExpression expr2, PrincessEnvironment env) {
    SymbolTrackingPrincessStack stack = (SymbolTrackingPrincessStack) env.getNewStack(false);
    stack.push(1);

    IFormula formula;
    // create !(expr1 <=> expr2) if this is unsat we know that the formulas are equal
    if (expr1 instanceof IFormula) {
      formula = new INot(new IBinFormula(IBinJunctor.Eqv(), castToFormula(expr1), castToFormula(expr2)));

      // create !(expr1 - expr2 = 0) if this is unsat we know that the formulas are equal
    } else {
      formula = new INot(castToTerm(expr1).$minus(castToTerm(expr2)).$eq$eq$eq(new IIntLit(IdealInt.apply(0))));
    }
    stack.assertTerm(formula);

    // flip boolean value, when unsat the formulas are equal
    boolean areEqual = !stack.checkSat();

    stack.close();

    return areEqual;
  }

  /**
   * This method replaces parts of the given expression tree that match a key of
   * the map with the corresponding value in the map.
   */
  private static IExpression replaceCommonExpressionsInTree(IExpression expr, List<IExpression> pCommonExprs, PrincessEnvironment pEnv, Map<IExpression, IExpression> abbreviatedTerms) {
    if (pCommonExprs.isEmpty()) {
      return expr;
    }

    Iterator<IExpression> it = expr.iterator();

    List<IExpression> newChilds = new ArrayList<>();
    while (it.hasNext()) {
      IExpression child = it.next();

      // we do only replace terms that are no variables
      if (isVariable(child)) {
        newChilds.add(child);

        // terms where we already have abbreviations for do not need
        // to be traversed again
      } else if (abbreviatedTerms.containsKey(child)) {
          newChilds.add(abbreviatedTerms.get(child));

          // traversal of yet unknown subtree
      } else {
        IExpression newChild = replaceCommonExpressionsInTree(child, pCommonExprs, pEnv, abbreviatedTerms);
        if (pCommonExprs.contains(child)) {
          IExpression abbrev = pEnv.abbrev(newChild);
          abbreviatedTerms.put(child, abbrev);
          newChilds.add(abbrev);
        } else {
          newChilds.add(newChild);
        }
      }
    }

    return expr.update(JavaConversions.asScalaBuffer(newChilds));
  }

  private static List<IExpression> getCommonSubTreeExpressions(IExpression expr) {
    Deque<IExpression> todo = new ArrayDeque<>();
    Set<IExpression> seen = new HashSet<>();
    List<IExpression> duplicates = new ArrayList<>(); // we want to retain the insertion order
                                                      // largest common subtrees are found first
                                                      // and should be replaced first
    todo.add(expr);

    while (!todo.isEmpty()) {
      IExpression currentExpr = todo.removeLast();

      // this is a duplicate term, we exclude single variables here for these we
      // do not need let expressions
      if (!seen.add(currentExpr) && !isVariable(currentExpr)) {
        duplicates.add(currentExpr);
        continue;
      }

      Iterator<IExpression> it = currentExpr.iterator();
      while (it.hasNext()) {
        todo.add(it.next());
      }
    }
    return duplicates;
  }

}
