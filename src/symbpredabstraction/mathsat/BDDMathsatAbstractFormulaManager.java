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
package symbpredabstraction.mathsat;

import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Level;

import mathsat.AllSatModelCallback;
import symbpredabstraction.SSAMap;
import symbpredabstraction.bdd.BDDAbstractFormulaManager;
import symbpredabstraction.interfaces.AbstractFormula;
import symbpredabstraction.interfaces.Predicate;
import symbpredabstraction.interfaces.SymbolicFormula;
import symbpredabstraction.interfaces.SymbolicFormulaManager;
import cmdline.CPAMain;

import common.Pair;
import common.Triple;


public class BDDMathsatAbstractFormulaManager extends BDDAbstractFormulaManager {

    /**
     * callback used to build the predicate abstraction of a formula
     * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
     */
    public class AllSatCallback implements AllSatModelCallback {
        private long msatEnv;
        private long absEnv;
        private AbstractFormula formula;
        private Deque<AbstractFormula> cubes;

        public AllSatCallback(long msatEnv, long absEnv) {
            this.formula = makeFalse();
            this.msatEnv = msatEnv;
            this.absEnv = absEnv;
            cubes = new LinkedList<AbstractFormula>();
        }

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
                cubes.add(makeOr(b1, b2));
            }
            assert(cubes.size() == 1);
            formula = cubes.remove();
        }

        public void callback(long[] model) {
            // the abstraction is created simply by taking the disjunction
            // of all the models found by msat_all_sat, and storing them
            // in a BDD
            // first, let's create the BDD corresponding to the model
            Deque<AbstractFormula> curCube = new LinkedList<AbstractFormula>();
            AbstractFormula m = makeTrue();
            for (int i = 0; i < model.length; ++i) {
                long t = 0;
                if (absEnv != 0) {
                    t = mathsat.api.msat_make_copy_from(
                            msatEnv, model[i], absEnv);
                } else {
                    t = model[i];
                }
                AbstractFormula v;
                if (mathsat.api.msat_term_is_not(t) != 0) {
                    t = mathsat.api.msat_term_get_arg(t, 0);
                    assert(msatVarToPredicate.containsKey(t));
                    v = msatVarToPredicate.get(t).getFormula();
                    v = makeNot(v);
                } else {
                  assert(msatVarToPredicate.containsKey(t));
                  v = msatVarToPredicate.get(t).getFormula();
                }
                curCube.add(v);
            }
            // now, add the model to the bdd
            curCube.add(m);
            while (curCube.size() > 1) {
                AbstractFormula v1 = curCube.remove();
                AbstractFormula v2 = curCube.remove();
                curCube.add(makeAnd(v1, v2));
            }
            assert(curCube.size() == 1);
            m = curCube.remove();
            cubes.add(m);
        }
    }

    // a predicate is just a BDD index for a variable (see BDDPredicate).
    // Here we keep the mapping predicate -> (MathSAT variable, MathSAT atom)
    private final Map<Predicate, Pair<Long, Long>> predicateToMsatAtom;
    // and the reverse mapping MathSAT variable -> predicate
    private final Map<Long, Predicate> msatVarToPredicate;

    private final Map<AbstractFormula, SymbolicFormula> toConcreteCache;


    public BDDMathsatAbstractFormulaManager() {
        predicateToMsatAtom = new HashMap<Predicate, Pair<Long, Long>>();
        msatVarToPredicate = new HashMap<Long, Predicate>();
        if (useCache) {
            toConcreteCache = new HashMap<AbstractFormula, SymbolicFormula>();
        } else {
          toConcreteCache = null;
        }
    }

    /**
     * creates a BDDPredicate from the Boolean mathsat variable (msatVar) and
     * the atoms that defines it (the msatAtom)
     */
    protected Predicate makePredicate(long msatVar, long msatAtom) {
        if (msatVarToPredicate.containsKey(msatVar)) {
            return msatVarToPredicate.get(msatVar);
        } else {
            Predicate result = createPredicate();

            CPAMain.logManager.log(Level.FINEST,
                           "CREATED PREDICATE:", result,
                           "from msatAtom:",
                           new MathsatSymbolicFormula(msatAtom));

            predicateToMsatAtom.put(result, new Pair<Long, Long>(msatVar, msatAtom));
            msatVarToPredicate.put(msatVar, result);
            return result;
        }
    }

    protected void collectVarNames(MathsatSymbolicFormulaManager mmgr,
            long term, Set<String> vars,
            Set<Pair<String, SymbolicFormula[]>> lvals) {
        Stack<Long> toProcess = new Stack<Long>();
        toProcess.push(term);
        // TODO - this assumes the term is small! There is no memoizing yet!!
        while (!toProcess.empty()) {
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

    // return value is:
    // [mathsat term for \bigwedge_preds (var <-> def),
    //  list of important terms (the names of the preds),
    //   list of variables occurring in the definitions of the preds]
    protected static class PredInfo {
        public long predDef; // mathsat term for \bigwedge_preds (var <-> def)
        public long[] important; // list of important terms 
                                 // (the names of the preds)
        public Set<String> allVars; // list of variable names occurring 
                                    // in the definitions of the preds
        public Set<Pair<String, 
                        SymbolicFormula[]>> allFuncs; // list of functions
                                                      // occurring in the 
                                                      // preds defs
        public PredInfo(long pd, long[] imp, Set<String> av, 
                        Set<Pair<String, SymbolicFormula[]>> af) {
            predDef = pd;
            important = imp;
            allVars = av;
            allFuncs = af;
        }
    }
    protected PredInfo buildPredList(
            MathsatSymbolicFormulaManager mmgr,
            Collection<Predicate> predicates) {
        long msatEnv = mmgr.getMsatEnv();
        long[] important = new long[predicates.size()];
        Set<String> allvars = new HashSet<String>();
        Set<Pair<String, SymbolicFormula[]>> allfuncs =
            new HashSet<Pair<String, SymbolicFormula[]>>();
        long preddef = mathsat.api.msat_make_true(msatEnv);
        int i = 0;
        for (Predicate p : predicates) {
            long var = predicateToMsatAtom.get(p).getFirst();
            long def = predicateToMsatAtom.get(p).getSecond();
            collectVarNames(mmgr, def, allvars, allfuncs);
            important[i++] = var;
            // build the mathsat (var <-> def)
            long iff = mathsat.api.msat_make_iff(msatEnv, var, def);
            assert(!mathsat.api.MSAT_ERROR_TERM(iff));
            // and add it to the list of definitions
            preddef = mathsat.api.msat_make_and(msatEnv, preddef, iff);
        }
        return new PredInfo(preddef, important, allvars, allfuncs);
    }

    public Pair<MathsatSymbolicFormula, MathsatSymbolicFormula>
      getPredicateNameAndDef(Predicate p) {
        long var = predicateToMsatAtom.get(p).getFirst();
        long def = predicateToMsatAtom.get(p).getSecond();
        return new Pair<MathsatSymbolicFormula, MathsatSymbolicFormula>(
                new MathsatSymbolicFormula(var),
                new MathsatSymbolicFormula(def));
    }

    /**
     * Given an abstract formula (which is a BDD over the predicates), build
     * its concrete representation (which is a MathSAT formula corresponding
     * to the BDD, in which each predicate is replaced with its definition)
     */
    @Override
    public SymbolicFormula toConcrete(SymbolicFormulaManager mgr,
            AbstractFormula af) {
        MathsatSymbolicFormulaManager mmgr = (MathsatSymbolicFormulaManager)mgr;

        Map<AbstractFormula, SymbolicFormula> cache;
        if (useCache) {
            cache = toConcreteCache;
        } else {
            cache = new HashMap<AbstractFormula, SymbolicFormula>();
        }
        Stack<AbstractFormula> toProcess = new Stack<AbstractFormula>();

        cache.put(makeTrue(), mgr.makeTrue());
        cache.put(makeFalse(), mgr.makeFalse());

        toProcess.push(af);
        while (!toProcess.empty()) {
            AbstractFormula n = toProcess.peek();
            if (cache.containsKey(n)) {
                toProcess.pop();
                continue;
            }
            boolean childrenDone = true;
            SymbolicFormula m1 = null;
            SymbolicFormula m2 = null;
            
            Triple<Predicate, AbstractFormula, AbstractFormula> parts = getIfThenElse(n);
            AbstractFormula c1 = parts.getSecond();
            AbstractFormula c2 = parts.getThird();
            if (!cache.containsKey(c1)) {
                toProcess.push(c1);
                childrenDone = false;
            } else {
                m1 = cache.get(c1);
            }
            if (!cache.containsKey(c2)) {
                toProcess.push(c2);
                childrenDone = false;
            } else {
                m2 = cache.get(c2);
            }
            if (childrenDone) {
                assert m1 != null;
                assert m2 != null;

                toProcess.pop();
                Predicate var = parts.getFirst();
                assert(predicateToMsatAtom.containsKey(var));

                long mAtom = predicateToMsatAtom.get(var).getSecond();
                
                SymbolicFormula ite = mmgr.makeIfThenElse(mAtom, m1, m2);
                cache.put(n, ite);
            }
        }

        assert(cache.containsKey(af));

        return cache.get(af);
    }

    protected SymbolicFormula[] getInstantiatedAt(
            MathsatSymbolicFormulaManager mmgr, SymbolicFormula[] args,
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
