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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.util.predicates.Model;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.InterpolatingTheoremProver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl.AbstractFormulaManager;

import com.google.common.base.Preconditions;

import de.uni_freiburg.informatik.ultimate.logic.AnnotatedTerm;
import de.uni_freiburg.informatik.ultimate.logic.Annotation;
import de.uni_freiburg.informatik.ultimate.logic.Script.LBool;
import de.uni_freiburg.informatik.ultimate.logic.Term;

public class SmtInterpolInterpolatingProver implements InterpolatingTheoremProver<Term> {

    private final SmtInterpolFormulaManager mgr;
    private SmtInterpolEnvironment env;

    private final List<String> assertedFormulas; // Collection of termNames
    private final Map<String, Term> annotatedTerms; // Collection of termNames
    private static final String prefix = "term_"; // for termnames
    private static int counter = 0; // for different termnames // TODO static?

    public SmtInterpolInterpolatingProver(
        SmtInterpolFormulaManager pMgr) {
      mgr = pMgr;
      env = mgr.createEnvironment();
      assertedFormulas = new ArrayList<>();
      annotatedTerms = new HashMap<>();
    }

    @Override
    public Term addFormula(BooleanFormula f) {
      Preconditions.checkNotNull(env);

      Term t = AbstractFormulaManager.getTerm(f);
      //Term t = ((SmtInterpolFormula)f).getTerm();

      String termName = prefix + counter++;
      Term annotatedTerm = env.annotate(t, new Annotation(":named", termName));
      env.push(1);
      env.assertTerm(annotatedTerm);
      assertedFormulas.add(termName);
      annotatedTerms.put(termName, t);
      return annotatedTerm;
    }

    @Override
    public void popFormula() {
      Preconditions.checkNotNull(env);
      assertedFormulas.remove(assertedFormulas.size()-1); // remove last term
      env.pop(1);
    }

    @Override
    public boolean isUnsat() {
      return env.checkSat() == LBool.UNSAT;
    }

    @Override
    public BooleanFormula getInterpolant(List<Term> formulasOfA) {
        Preconditions.checkNotNull(env);

        // wrap terms into annotated term, collect their names as "termNamesOfA"
        Set<String> termNamesOfA = new HashSet<>();
        for (int i=0; i<formulasOfA.size(); i++) {
          final Term t = formulasOfA.get(i);
          assert t instanceof AnnotatedTerm;
          final Object termNameObj = ((AnnotatedTerm)t).getAnnotations()[0].getValue();
          assert termNameObj instanceof String;
          final String termName = (String)termNameObj;
          termNamesOfA.add(termName);
        }

        // calc difference: termNamesOfB := assertedFormulas - termNamesOfA
        List<String> termNamesOfB = new ArrayList<>();
        for (String assertedFormulaName : assertedFormulas) {
          if (!termNamesOfA.contains(assertedFormulaName)) {
            termNamesOfB.add(assertedFormulaName);
          }
        }

        // get terms with names
        Term[] groupOfA = new Term[termNamesOfA.size()];
        int i=0;
        for (String termName: termNamesOfA) {
          groupOfA[i] = env.term(termName);
          i++;
        }
        Term[] groupOfB = new Term[termNamesOfB.size()];
        i=0;
        for (String termName: termNamesOfB) {
          groupOfB[i] = env.term(termName);
          i++;
        }

        // build 2 groups:  (and A1 A2 A3...) , (and B1 B2 B3...)
        assert groupOfA.length != 0;
        Term termA = groupOfA[0];
        if (groupOfA.length > 1) {
          termA = env.term("and", groupOfA);
        }
        assert groupOfB.length != 0;
        Term termB = groupOfB[0];
        if (groupOfB.length > 1) {
          termB = env.term("and", groupOfB);
        }

        // get interpolant of groups
        Term[] itp = env.getInterpolants(new Term[] {termA, termB});
        assert itp.length == 1; // 2 groups -> 1 interpolant

        BooleanFormula f = mgr.encapsulate(BooleanFormula.class, itp[0]);

        return f;
    }

    @Override
    public void close() {
      Preconditions.checkNotNull(env);
      while (!assertedFormulas.isEmpty()) { // cleanup stack
        popFormula();
      }
      annotatedTerms.clear();
      env = null;
    }

    @Override
    public Model getModel() {
      Preconditions.checkNotNull(env);
      List<Term> terms = new ArrayList<>(assertedFormulas.size());
      for (String termname : assertedFormulas) {
        terms.add(annotatedTerms.get(termname));
      }
      return SmtInterpolModel.createSmtInterpolModel(mgr, terms);
    }
}