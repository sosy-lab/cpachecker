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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sosy_lab.cpachecker.util.predicates.Model;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.InterpolatingTheoremProver;

import com.google.common.base.Preconditions;

import de.uni_freiburg.informatik.ultimate.logic.AnnotatedTerm;
import de.uni_freiburg.informatik.ultimate.logic.Annotation;
import de.uni_freiburg.informatik.ultimate.logic.SMTLIBException;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Script.LBool;
import de.uni_freiburg.informatik.ultimate.logic.Term;

public class SmtInterpolInterpolatingProver implements InterpolatingTheoremProver<Term> {

    private final SmtInterpolFormulaManager mgr;
    private Script script;
    List<String> assertedFormulas; // Collection of termNames
    private String prefix = "term_"; // for termnames
    static int counter = 0; // for different termnames // TODO static?

    public SmtInterpolInterpolatingProver(SmtInterpolFormulaManager pMgr, boolean shared) {
        mgr = pMgr;
        script = null;
    }


    @Override
    public void init() {
      script = mgr.createEnvironment();
      assertedFormulas = new ArrayList<String>();
      script.push(1); // TODO correct??
    }

    @Override
    public Term addFormula(Formula f) {
      Preconditions.checkNotNull(script);
      Term t = ((SmtInterpolFormula)f).getTerm();

      Term annotatedTerm = null;
      try {
        String termName = prefix + counter++;
        annotatedTerm = script.annotate(t, new Annotation(":named", termName));
        script.assertTerm(annotatedTerm);
        assertedFormulas.add(termName);
      } catch (SMTLIBException e) {
        e.printStackTrace();
      }
      return annotatedTerm;
    }

    @Override
    public boolean isUnsat() {
      LBool res = script.checkSat();
      assert(res != LBool.UNKNOWN); // TODO: why assertion?
      return res == LBool.UNSAT;
    }

    @Override
    public Formula getInterpolant(List<Term> formulasOfA) {
        Preconditions.checkNotNull(script);

        // wrap terms into annotated term, collect their names as "termNamesOfA"
        Set<String> termNamesOfA = new HashSet<String>();
        for (int i=0; i<formulasOfA.size(); i++) {
          final Term t = formulasOfA.get(i);
          assert t instanceof AnnotatedTerm;
          final Object termNameObj = ((AnnotatedTerm)t).getAnnotations()[0].getValue();
          assert termNameObj instanceof String;
          final String termName = (String)termNameObj;
          termNamesOfA.add(termName);
        }

        // calc difference: termNamesOfB := assertedFormulas - termNamesOfA
        List<String> termNamesOfB = new ArrayList<String>();
        for (String assertedFormulaName : assertedFormulas) {
          if (!termNamesOfA.contains(assertedFormulaName)) {
            termNamesOfB.add(assertedFormulaName);
          }
        }

        Term[] itp = null; // TODO why array? only itp[0] is used
        try {
          // get terms with names
          Term[] groupOfA = new Term[termNamesOfA.size()];
          int i=0;
          for (String termName: termNamesOfA) {
            groupOfA[i] = script.term(termName);
            i++;
          }
          Term[] groupOfB = new Term[termNamesOfB.size()];
          i=0;
          for (String termName: termNamesOfB) {
            groupOfB[i] = script.term(termName);
            i++;
          }

          // build 2 groups:  (and A1 A2 A3...) , (and B1 B2 B3...)
          assert groupOfA.length != 0;
          Term termA = groupOfA[0];
          if (groupOfA.length > 1) {
            termA = script.term("and", groupOfA);
          }
          assert groupOfB.length != 0;
          Term termB = groupOfB[0];
          if (groupOfB.length > 1) {
            termB = script.term("and", groupOfB);
          }

          itp = script.getInterpolants(new Term[] {termA, termB});

        } catch (SMTLIBException e) {
          e.printStackTrace();
        }
        // there are more interpolants, we only use the first one, TODO correct??
        return new SmtInterpolFormula(itp[0], script);
    }

    @Override
    public void reset() {
      Preconditions.checkNotNull(script);
      try {
        script.pop(1); // working??
      } catch (SMTLIBException e) {
        e.printStackTrace();
      }
      assertedFormulas = null;
      script = null;
    }

    @Override
    public Model getModel() {
      Preconditions.checkNotNull(script);
      return SmtInterpolModel.createSmtInterpolModel(script, mgr);
    }

}