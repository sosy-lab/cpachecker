/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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

import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import de.uni_freiburg.informatik.ultimate.logic.Annotation;
import de.uni_freiburg.informatik.ultimate.logic.ApplicationTerm;
import de.uni_freiburg.informatik.ultimate.logic.Assignments;
import de.uni_freiburg.informatik.ultimate.logic.FunctionSymbol;
import de.uni_freiburg.informatik.ultimate.logic.Logics;
import de.uni_freiburg.informatik.ultimate.logic.Model;
import de.uni_freiburg.informatik.ultimate.logic.SMTLIBException;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Sort;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.TermVariable;
import de.uni_freiburg.informatik.ultimate.logic.Theory;


/**
 * This {@link Script} implementation allows to use the SMTLIB2 parser
 * of SMTInterpol for parsing single formulas.
 * It is meant to be given to a
 * {@link de.uni_freiburg.informatik.ultimate.smtinterpol.smtlib2.ParseEnvironment}
 * and allows to declare and define terms (by forwarding such calls to
 * a real {@link Script} implementation,
 * but does not allow any other actions.
 * All formulas that are asserted (regardless of pop and push commands)
 * are collected and can be retrieved afterwards.
 *
 * The environment represented by the given {@link Script} that this class
 * delegates to is changed only by declaring and defining terms, sorts etc.,
 * so these terms can be used in that environment afterwards.
 */
class FormulaCollectionScript implements Script {

  private final Theory theory;
  private final Script script;

  private final List<Term> assertedTerms = new ArrayList<>(1);

  FormulaCollectionScript(Script pScript, Theory pTheory) {
    script = checkNotNull(pScript);
    theory = checkNotNull(pTheory);
  }

  public List<Term> getAssertedTerms() {
    return Collections.unmodifiableList(assertedTerms);
  }

  @Override
  public LBool assertTerm(Term pTerm) throws SMTLIBException {
    assertedTerms.add(pTerm);
    // Do not call script.assertTerm(pTerm)
    // because we do not want to actually modify the environment
    return LBool.UNKNOWN;
  }

  @Override
  public void declareFun(String fun, Sort[] paramSorts, Sort resultSort)
      throws SMTLIBException {
    FunctionSymbol fsym = theory.getFunction(fun, paramSorts);

    if (fsym == null) {
      script.declareFun(fun, paramSorts, resultSort);
    } else {
      if (!fsym.getReturnSort().equals(resultSort)) {
        throw new SMTLIBException("Function " + fun + " is already declared with different definition");
      }
    }
  }

  @Override
  public void defineFun(String fun, TermVariable[] params,
      Sort resultSort, Term definition) throws SMTLIBException {
    Sort[] paramSorts = new Sort[params.length];
    for (int i = 0; i < paramSorts.length; i++) {
      paramSorts[i] = params[i].getSort();
    }
    FunctionSymbol fsym = theory.getFunction(fun, paramSorts);

    if (fsym == null) {
      script.defineFun(fun, params, resultSort, definition);
    } else {
      if (!fsym.getDefinition().equals(definition)
          || !fsym.getReturnSort().equals(resultSort)) {
        throw new SMTLIBException("Function " + fun + " is already defined with different definition");
      }
    }
  }

  @Override
  public void setInfo(String info, Object value) {
    script.setInfo(info, value);
  }

  @Override
  public void declareSort(String sort, int arity) throws SMTLIBException {
    script.declareSort(sort, arity);
  }

  @Override
  public void defineSort(String sort, Sort[] sortParams, Sort definition)
      throws SMTLIBException {
    script.defineSort(sort, sortParams, definition);
  }

  @Override
  public Sort sort(String sortname, Sort... params) throws SMTLIBException {
    return script.sort(sortname, params);
  }

  @Override
  public Sort sort(String sortname, BigInteger[] indices, Sort... params)
      throws SMTLIBException {
    return script.sort(sortname, indices, params);
  }

  @Override
  public Term term(String funcname, Term... params) throws SMTLIBException {
    Term result = script.term(funcname, params);
    return replaceWithDefinition(result);
  }

  @Override
  public Term term(String funcname, BigInteger[] indices,
      Sort returnSort, Term... params) throws SMTLIBException {
    Term result = script.term(funcname, indices, returnSort, params);
    return replaceWithDefinition(result);
  }

  private Term replaceWithDefinition(Term result) {
    // Replace a term with its definition so that we do not have to handle defined terms later on.
    if (result instanceof ApplicationTerm) {
      FunctionSymbol func = ((ApplicationTerm)result).getFunction();
      if (!func.isIntern()
          && func.getDefinition() != null) {
        if (func.getParameterSorts().length == 0) {
          result = func.getDefinition();
        } else {
          // If we would accept this here,
          // we would need to handle the definition of a term
          // when accessing its parameters with SmtInterpolUtil.getArg()
          throw new SMTLIBException("Terms with definitions are not supported currently.");
        }
      }
    }
    return result;
  }

  @Override
  public TermVariable variable(String varname, Sort sort)
      throws SMTLIBException {
    return script.variable(varname, sort);
  }

  @Override
  public Term quantifier(int quantor, TermVariable[] vars, Term body,
      Term[]... patterns) throws SMTLIBException {
    return script.quantifier(quantor, vars, body, patterns);
  }

  @Override
  public Term let(TermVariable[] vars, Term[] values, Term body)
      throws SMTLIBException {
    return script.let(vars, values, body);
  }

  @Override
  public Term annotate(Term t, Annotation... annotations)
      throws SMTLIBException {
    return script.annotate(t, annotations);
  }

  @Override
  public Term numeral(String num) throws SMTLIBException {
    return script.numeral(num);
  }

  @Override
  public Term numeral(BigInteger num) throws SMTLIBException {
    return script.numeral(num);
  }

  @Override
  public Term decimal(String decimal) throws SMTLIBException {
    return script.decimal(decimal);
  }

  @Override
  public Term decimal(BigDecimal decimal) throws SMTLIBException {
    return script.decimal(decimal);
  }

  @Override
  public Term string(String str) throws SMTLIBException {
    return script.string(str);
  }

  @Override
  public Term hexadecimal(String hex) throws SMTLIBException {
    return script.hexadecimal(hex);
  }

  @Override
  public Term binary(String bin) throws SMTLIBException {
    return script.binary(bin);
  }

  @Override
  public Sort[] sortVariables(String... names) throws SMTLIBException {
    return script.sortVariables(names);
  }

  @Override
  public Term[] getAssertions() throws SMTLIBException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Term getProof() throws SMTLIBException,
      UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Term[] getUnsatCore() throws SMTLIBException,
      UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Map<Term, Term> getValue(Term[] terms) throws SMTLIBException,
      UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Assignments getAssignment() throws SMTLIBException,
      UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Object getOption(String opt)
      throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Object getInfo(String info)
      throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Term simplify(Term term) throws SMTLIBException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void push(int levels) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void pop(int levels) throws SMTLIBException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Model getModel() throws SMTLIBException,
      UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setLogic(String logic) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setLogic(Logics logic) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setOption(String opt, Object value)
      throws UnsupportedOperationException, SMTLIBException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void reset() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Term[] getInterpolants(Term[] partition) throws SMTLIBException,
      UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Term[] getInterpolants(Term[] partition, int[] startOfSubtree)
      throws SMTLIBException, UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public LBool checkSat() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Iterable<Term[]> checkAllsat(Term[] pPredicates) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Term[] findImpliedEquality(Term[] pX, Term[] pY) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void exit() {
    throw new UnsupportedOperationException();
  }
}