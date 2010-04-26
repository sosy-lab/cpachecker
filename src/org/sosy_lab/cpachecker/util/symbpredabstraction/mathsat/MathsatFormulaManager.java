/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
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
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Level;

import org.sosy_lab.cpachecker.util.symbpredabstraction.CommonFormulaManager;
import org.sosy_lab.cpachecker.util.symbpredabstraction.SSAMap;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.AbstractFormula;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.AbstractFormulaManager;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormula;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.TheoremProver;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;

import org.sosy_lab.cpachecker.core.LogManager;
import org.sosy_lab.cpachecker.exceptions.InvalidConfigurationException;

// TODO decide which parts of the API of this class can be move to the FormularManager interface
public class MathsatFormulaManager extends CommonFormulaManager {

    /**
     * callback used to build the predicate abstraction of a formula
     * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
     */
    protected class AllSatCallback implements TheoremProver.AllSatCallback {
        public long totalTime = 0;

        private AbstractFormula formula = amgr.makeFalse();
        private final Deque<AbstractFormula> cubes = new ArrayDeque<AbstractFormula>();
        
        public AllSatCallback() { /* change visibility to public */ }

        // TODO rename getBDD to something like getResult
        public AbstractFormula getBDD() {
            if (cubes.size() > 0) {
                buildBalancedOr();
            }
            return formula;
        }

        private void buildBalancedOr() {
            cubes.add(formula);
            while (cubes.size() > 1) {
                AbstractFormula b1 = cubes.remove();
                AbstractFormula b2 = cubes.remove();
                cubes.add(amgr.makeOr(b1, b2));
            }
            assert(cubes.size() == 1);
            formula = cubes.remove();
        }

        @Override
        public void modelFound(List<SymbolicFormula> model) {
            logger.log(Level.ALL, "Allsat found model", model);
            long start = System.currentTimeMillis();

            // the abstraction is created simply by taking the disjunction
            // of all the models found by msat_all_sat, and storing them
            // in a BDD
            // first, let's create the BDD corresponding to the model
            Deque<AbstractFormula> curCube = new ArrayDeque<AbstractFormula>();
            AbstractFormula m = amgr.makeTrue();
            for (SymbolicFormula f : model) {
                long t = ((MathsatSymbolicFormula)f).getTerm();
                
                AbstractFormula v;
                if (mathsat.api.msat_term_is_not(t) != 0) {
                    t = mathsat.api.msat_term_get_arg(t, 0);
                    v = getPredicate(new MathsatSymbolicFormula(t)).getFormula();
                    v = amgr.makeNot(v);
                } else {
                  v = getPredicate(f).getFormula();
                }
                curCube.add(v);
            }
            // now, add the model to the bdd
            curCube.add(m);
            while (curCube.size() > 1) {
                AbstractFormula v1 = curCube.remove();
                AbstractFormula v2 = curCube.remove();
                curCube.add(amgr.makeAnd(v1, v2));
            }
            assert(curCube.size() == 1);
            m = curCube.remove();
            cubes.add(m);

            long end = System.currentTimeMillis();
            totalTime += (end - start);
        }
    }

    protected final MathsatSymbolicFormulaManager mmgr;

    public MathsatFormulaManager(AbstractFormulaManager pAmgr,
              MathsatSymbolicFormulaManager pMmgr,
              Configuration config, LogManager logger) throws InvalidConfigurationException {
        super(pAmgr, pMmgr, config, logger);
        mmgr = pMmgr;
    }

    @Override
    protected void collectVarNames(SymbolicFormula term, Set<String> vars,
        Set<Pair<String, SymbolicFormula[]>> lvals) {
      
      Deque<Long> toProcess = new ArrayDeque<Long>();
      toProcess.push(((MathsatSymbolicFormula)term).getTerm());
      // TODO - this assumes the term is small! There is no memoizing yet!!
      while (!toProcess.isEmpty()) {
          long t = toProcess.pop();
          if (mathsat.api.msat_term_is_variable(t) != 0) {
              vars.add(mathsat.api.msat_term_repr(t));
          } else {
              for (int i = 0; i < mathsat.api.msat_term_arity(t); ++i) {
                  toProcess.push(mathsat.api.msat_term_get_arg(t, i));
              }
              if (mathsat.api.msat_term_is_uif(t) != 0) {
                  long d = mathsat.api.msat_term_get_decl(t);
                  String name = mathsat.api.msat_decl_get_name(d);
                  if (mmgr.ufCanBeLvalue(name)) {
                      int n = mathsat.api.msat_term_arity(t);
                      SymbolicFormula[] a = new SymbolicFormula[n];
                      for (int i = 0; i < n; ++i) {
                          a[i] = new MathsatSymbolicFormula(
                                  mathsat.api.msat_term_get_arg(t, i));
                      }
                      lvals.add(new Pair<String, SymbolicFormula[]>(name, a));
                  }
              }
          }
      }
    }
  
    protected SymbolicFormula[] getInstantiatedAt(SymbolicFormula[] args,
            SSAMap ssa, Map<SymbolicFormula, SymbolicFormula> cache) {
        Stack<Long> toProcess = new Stack<Long>();
        SymbolicFormula[] ret = new SymbolicFormula[args.length];
        for (SymbolicFormula f : args) {
            toProcess.push(((MathsatSymbolicFormula)f).getTerm());
        }

        while (!toProcess.empty()) {
            long t = toProcess.peek();
            SymbolicFormula tt = new MathsatSymbolicFormula(t);
            if (cache.containsKey(tt)) {
                toProcess.pop();
                continue;
            }
            if (mathsat.api.msat_term_is_variable(t) != 0) {
                toProcess.pop();
                String name = mathsat.api.msat_term_repr(t);
                assert(ssa.getIndex(name) > 0);
                cache.put(tt, mmgr.instantiate(
                        new MathsatSymbolicFormula(t), ssa));
            } else if (mathsat.api.msat_term_is_uif(t) != 0) {
                long d = mathsat.api.msat_term_get_decl(t);
                String name = mathsat.api.msat_decl_get_name(d);
                if (mmgr.ufCanBeLvalue(name)) {
                    SymbolicFormula[] cc =
                        new SymbolicFormula[mathsat.api.msat_term_arity(t)];
                    boolean childrenDone = true;
                    for (int i = 0; i < cc.length; ++i) {
                        long c = mathsat.api.msat_term_get_arg(t, i);
                        SymbolicFormula f = new MathsatSymbolicFormula(c);
                        if (cache.containsKey(f)) {
                            cc[i] = cache.get(f);
                        } else {
                            toProcess.push(c);
                            childrenDone = false;
                        }
                    }
                    if (childrenDone) {
                        toProcess.pop();
                        if (ssa.getIndex(name, cc) < 0) {
                            ssa.setIndex(name, cc, 1);
                        }
                        cache.put(tt, mmgr.instantiate(tt, ssa));
                    }
                } else {
                    toProcess.pop();
                    cache.put(tt, tt);
                }
            } else {
                toProcess.pop();
                cache.put(tt, tt);
            }
        }
        for (int i = 0; i < ret.length; ++i) {
            assert(cache.containsKey(args[i]));
            ret[i] = cache.get(args[i]);
        }
        return ret;
    }

}
