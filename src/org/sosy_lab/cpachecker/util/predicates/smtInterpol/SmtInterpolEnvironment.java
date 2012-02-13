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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.sosy_lab.common.Triple;

import de.uni_freiburg.informatik.ultimate.logic.Annotation;
import de.uni_freiburg.informatik.ultimate.logic.ApplicationTerm;
import de.uni_freiburg.informatik.ultimate.logic.Assignments;
import de.uni_freiburg.informatik.ultimate.logic.Logics;
import de.uni_freiburg.informatik.ultimate.logic.SMTLIBException;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Sort;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.TermVariable;
import de.uni_freiburg.informatik.ultimate.logic.Theory;
import de.uni_freiburg.informatik.ultimate.logic.Valuation;
import de.uni_freiburg.informatik.ultimate.smtinterpol.smtlib2.Benchmark;

/** This is a Wrapper around the SmtInterpolScript.
 * It guarantees the stack-behavior towards the wrapped Script */
public class SmtInterpolEnvironment implements Script {

  /** the wrapped Script */
  private Script script;

  private int stacksize = 0;
  private List<List<Triple<String, Sort[], Sort>>> declarationsPerLevel =
      new ArrayList<List<Triple<String, Sort[], Sort>>>();
  private List<Triple<String, Sort[], Sort>> currentDeclarations =
      new ArrayList<Triple<String, Sort[], Sort>>();
//  private int logCounter = 0;

  public SmtInterpolEnvironment() {
    Logger logger = Logger.getRootLogger(); // TODO use SosyLAb-Logger
    // levels: ALL | DEBUG | INFO | WARN | ERROR | FATAL | OFF:
    logger.setLevel(Level.OFF);
    script = new Benchmark(logger);

//    try {
//      FileAppender fileAppender = new FileAppender(
//          new SimpleLayout(),
//          "output/smtinterpol" + (logCounter++) + ".log",
//          false);
//      logger.addAppender(fileAppender);
//    } catch (IOException e) {
//      e.printStackTrace();
//    }

//    try {
//      // create a thin wrapper around Benchmark,
//      // this allows to write most formulas of the solver to outputfile
//      // TODO how much faster is SmtInterpol without this Wrapper?
//      script = new LoggingScript(new Benchmark(logger), "interpol.smt2", true);
//    } catch (FileNotFoundException e1) {
//      e1.printStackTrace();
//    }

    try {
      script.setOption(":produce-proofs", true);
      script.setOption(":produce-models", true);
      script.setOption(":produce-assignments", true);
      script.setOption(":interactive-mode", true);
      BigInteger verbosity = (BigInteger) script.getOption(":verbosity");
      script.setOption(":verbosity", verbosity.subtract(new BigInteger("2")));
    } catch (SMTLIBException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void setLogic(String logic) throws UnsupportedOperationException {
    script.setLogic(logic);
  }

  @Override
  public void setLogic(Logics logic) throws UnsupportedOperationException {
    script.setLogic(logic);
  }

  @Override
  public void setOption(String opt, Object value) throws UnsupportedOperationException, SMTLIBException {
    script.setOption(opt, value);
  }

  @Override
  public void setInfo(String info, Object value) {
    script.setInfo(info, value);
  }

  @Override
  public void declareSort(String sort, int arity) throws SMTLIBException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void defineSort(String sort, Sort[] sortParams, Sort definition) throws SMTLIBException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void declareFun(String fun, Sort[] paramSorts, Sort resultSort) throws SMTLIBException {
    script.declareFun(fun, paramSorts, resultSort);
    if (stacksize != 0) {
      currentDeclarations.add(
          new Triple<String, Sort[], Sort>(fun, paramSorts, resultSort));
    }
  }

  @Override
  public void defineFun(String fun, TermVariable[] params, Sort resultSort, Term definition) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void push(int levels) {
    stacksize += levels;
    script.push(levels);

    for (int i = 0; i < levels; i++) {
      currentDeclarations = new ArrayList<Triple<String, Sort[], Sort>>();
      declarationsPerLevel.add(currentDeclarations);
    }
  }

  @Override
  public void pop(int levels) throws SMTLIBException {
    assert stacksize >= levels : "not enough levels to remove";
    stacksize -= levels;
    script.pop(levels);

    for (int i = 0; i < levels; i++) {
      for (Triple<String, Sort[], Sort> function : currentDeclarations) {
        final String fun = function.getFirst();
        final Sort[] paramSorts = function.getSecond();
        final Sort resultSort = function.getThird();
        script.declareFun(fun, paramSorts, resultSort);
      }
      currentDeclarations = declarationsPerLevel.remove(
          declarationsPerLevel.size() - 1);
    }
  }

  @Override
  public LBool assertTerm(Term term) throws SMTLIBException {
    assert stacksize > 0 : "assertions should be on higher levels";
    //System.out.println("ASSERT TERM");
    List<Term> l = new LinkedList<Term>();
    int c = 0;
    l.add(term);
    while (l.size() != 0) {
      c++;
      Term t = l.remove(0);
      if (t instanceof ApplicationTerm) {
        //System.out.println(((ApplicationTerm) t).getFunction().toString());
        Term[] params = ((ApplicationTerm) t).getParameters();
        for (Term x : params) {
          l.add(x);
        }
      } else {
        // System.out.println(t.toStringDirect());
      }
    }
    //System.out.println(c);
    //System.out.println(term.toString());
    //System.out.println(term.toStringDirect());
    LBool result = script.assertTerm(script.simplifyTerm(term));
    //System.out.println("ASSERT TERM END");
    return result;
  }

  @Override
  public LBool checkSat() {
    return script.checkSat();
  }

  @Override
  public Term[] getAssertions() throws SMTLIBException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void getProof() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Term[] getUnsatCore() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Valuation getValue(Term[] terms) throws SMTLIBException, UnsupportedOperationException {
    return script.getValue(terms);
  }

  @Override
  public Assignments getAssignment() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Object getOption(String opt) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Object[] getInfo(String info) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void exit() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Sort sort(String sortname) throws SMTLIBException {
    return script.sort(sortname);
  }

  @Override
  public Sort sort(String sortname, Sort... params) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Sort sort(String pSortname, BigInteger[] pIndices, Sort... pParams) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Term term(String funcname, Term... params) throws SMTLIBException {
    return script.term(funcname, params);
  }

  @Override
  public Term term(String funcname, BigInteger[] indices, Sort returnSort, Term... params) throws SMTLIBException {
    return script.term(funcname, indices, returnSort, params);
  }

  @Override
  public TermVariable variable(String varname, Sort sort) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Term quantifier(int quantor, TermVariable[] vars, Term body, Term[]... patterns) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Term let(TermVariable[] pVars, Term[] pValues, Term pBody) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Term annotate(Term t, Annotation... annotations) throws SMTLIBException {
    return script.annotate(t, annotations);
  }

  @Override
  public Term numeral(String num) throws SMTLIBException {
    return script.numeral(num);
  }

  @Override
  public Term decimal(String decimal) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Term hexadecimal(String pHex) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Term binary(String pBin) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Theory getTheory() {
    return script.getTheory();
  }

  @Override
  public Term simplifyTerm(Term term) throws SMTLIBException {
    return script.simplifyTerm(term);
  }

  @Override
  public void reset() {

  }

  @Override
  public Term[] getInterpolants(Term[] partition) throws SMTLIBException, UnsupportedOperationException {
    assert stacksize > 0 : "interpolants should be on higher levels";
    return script.getInterpolants(partition);
  }

}
