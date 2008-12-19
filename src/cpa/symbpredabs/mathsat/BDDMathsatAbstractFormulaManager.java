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
package cpa.symbpredabs.mathsat;

import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import logging.CustomLogLevel;
import logging.LazyLogger;
import mathsat.AllSatModelCallback;
import cmdline.CPAMain;

import common.Pair;

import cpa.symbpredabs.AbstractFormula;
import cpa.symbpredabs.AbstractFormulaManager;
import cpa.symbpredabs.Predicate;
import cpa.symbpredabs.SSAMap;
import cpa.symbpredabs.SymbolicFormula;
import cpa.symbpredabs.SymbolicFormulaManager;

public class BDDMathsatAbstractFormulaManager implements AbstractFormulaManager{

    /**
     * callback used to build the predicate abstraction of a formula
     * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
     */
    public class AllSatCallback implements AllSatModelCallback {
        private long msatEnv;
        private long absEnv;
        private int bdd;
        private Deque<Integer> cubes;

        public AllSatCallback(int bdd, long msatEnv, long absEnv) {
            this.bdd = bdd;
            this.msatEnv = msatEnv;
            this.absEnv = absEnv;
            cubes = new LinkedList<Integer>();
        }

        public int getBDD() {
            if (cubes.size() > 0) {
                buildBalancedOr();
            }
            return bdd;
        }

        private void buildBalancedOr() {
            cubes.add(bdd);
            while (cubes.size() > 1) {
                int b1 = cubes.remove();
                int b2 = cubes.remove();
                cubes.add(bddManager.or(b1, b2));
            }
            assert(cubes.size() == 1);
            bdd = cubes.remove();
        }

        public void callback(long[] model) {
            // the abstraction is created simply by taking the disjunction
            // of all the models found by msat_all_sat, and storing them
            // in a BDD
            // first, let's create the BDD corresponding to the model
            Deque<Integer> curCube = new LinkedList<Integer>();
            int m = bddManager.getOne();
            for (int i = 0; i < model.length; ++i) {
                long t = 0;
                if (absEnv != 0) {
                    t = mathsat.api.msat_make_copy_from(
                            msatEnv, model[i], absEnv);
                } else {
                    t = model[i];
                }
                int v;
                if (mathsat.api.msat_term_is_not(t) != 0) {
                    t = mathsat.api.msat_term_get_arg(t, 0);
                    assert(msatVarToBddPredicate.containsKey(t));
                    v = msatVarToBddPredicate.get(t);
                    v = bddManager.not(v);
                } else {
                    v = msatVarToBddPredicate.get(t);
                }
                curCube.add(v);
                //m = bddManager.and(m, v);
            }
            // now, add the model to the bdd
            //bdd = bddManager.or(bdd, m);
            curCube.add(m);
            while (curCube.size() > 1) {
                int v1 = curCube.remove();
                int v2 = curCube.remove();
                curCube.add(bddManager.and(v1, v2));
            }
            assert(curCube.size() == 1);
            m = curCube.remove();
            cubes.add(m);
        }
    }

    protected JavaBDD bddManager;

    // a predicate is just a BDD index for a variable (see BDDPredicate). Here
    // we keep the mapping BDD index -> (MathSAT variable, MathSAT atom)
    private Map<Integer, Pair<Long, Long>> bddPredicateToMsatAtom;
    // and the mapping MathSAT variable -> BDD index
    private Map<Long, Integer> msatVarToBddPredicate;
    // and MathSAT atom -> BDD index
    private Map<Long, Integer> msatAtomToBddPredicate;

    private boolean entailsUseCache;
    private Map<Pair<AbstractFormula, AbstractFormula>, Boolean> entailsCache;
    private Map<Integer, Long> toConcreteCache;


    public BDDMathsatAbstractFormulaManager() {
        //bddManager = new JDD();
        bddManager = new JavaBDD();
        bddPredicateToMsatAtom = new HashMap<Integer, Pair<Long, Long>>();
        msatVarToBddPredicate = new HashMap<Long, Integer>();
        msatAtomToBddPredicate = new HashMap<Long, Integer>();
        entailsUseCache = CPAMain.cpaConfig.getBooleanValue(
                "cpas.symbpredabs.mathsat.useCache");
        if (entailsUseCache) {
            entailsCache =
                new HashMap<Pair<AbstractFormula, AbstractFormula>, Boolean>();
            toConcreteCache = new HashMap<Integer, Long>();
        }
    }

    /**
     * creates a BDDPredicate from the Boolean mathsat variable (msatVar) and
     * the atoms that defines it (the msatAtom)
     */
    public Predicate makePredicate(long msatVar, long msatAtom) {
        if (msatVarToBddPredicate.containsKey(msatVar)) {
            int bddVar = msatVarToBddPredicate.get(msatVar);
            int var = bddManager.getVar(bddVar);
            return new BDDPredicate(bddVar, var);
        } else {
            int bddVar = bddManager.createVar();

            LazyLogger.log(CustomLogLevel.SpecificCPALevel,
                           "CREATED PREDICATE: bddVar: ",
                           Integer.toString(bddManager.getVar(bddVar)),
                           ", msatAtom: ",
                           new MathsatSymbolicFormula(msatAtom));

            int var = bddManager.getVar(bddVar);
            bddPredicateToMsatAtom.put(var,
                    new Pair<Long, Long>(msatVar, msatAtom));
            msatVarToBddPredicate.put(msatVar, bddVar);
            msatAtomToBddPredicate.put(msatAtom, bddVar);
            return new BDDPredicate(bddVar, var);
        }
    }


    public boolean entails(AbstractFormula f1, AbstractFormula f2) {
        // check entailment using BDDs: create the BDD representing
        // the implication, and check that it is the TRUE formula
        Pair<AbstractFormula, AbstractFormula> key = null;
        if (entailsUseCache) {
            key = new Pair<AbstractFormula, AbstractFormula>(f1, f2);
            if (entailsCache.containsKey(key)) {
                return entailsCache.get(key);
            }
        }
        int imp = bddManager.imp(((BDDAbstractFormula)f1).getBDD(),
                                 ((BDDAbstractFormula)f2).getBDD());
        boolean yes = (imp == bddManager.getOne());
        if (entailsUseCache) {
            assert(key != null);
            entailsCache.put(key, yes);
        }
        return yes;
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
    protected class PredInfo {
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
            BDDPredicate bp = (BDDPredicate)p;
            int idx = bp.getBDD();
            int bddvar = bddManager.getVar(idx);
            long var = bddPredicateToMsatAtom.get(bddvar).getFirst();
            long def = bddPredicateToMsatAtom.get(bddvar).getSecond();
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
      getPredicateNameAndDef(BDDPredicate p) {
        int idx = p.getBDD();
        int bddvar = bddManager.getVar(idx);
        long var = bddPredicateToMsatAtom.get(bddvar).getFirst();
        long def = bddPredicateToMsatAtom.get(bddvar).getSecond();
        return new Pair<MathsatSymbolicFormula, MathsatSymbolicFormula>(
                new MathsatSymbolicFormula(var),
                new MathsatSymbolicFormula(def));
    }

    /**
     * This should compute the predicate abstraction of the given input
     * formula wrt. the given list of predicates. However, this method is not
     * used anymore, and should probably be considered deprecated (maybe plain
     * broken also :-). See BDDMathsatExplicitFormulaManager and
     * BDDMathsatSummaryFormulaManager for alternatives that are currently
     * used.
     */
    public AbstractFormula toAbstract(SymbolicFormulaManager mgr,
            SymbolicFormula f, SSAMap ssa, Collection<Predicate> predicates) {
        MathsatSymbolicFormulaManager mmgr = (MathsatSymbolicFormulaManager)mgr;
        MathsatSymbolicFormula mf = (MathsatSymbolicFormula)f;

        long absEnv = mathsat.api.msat_create_env();

        //mathsat.api.msat_set_verbosity(10);

        long msatEnv = mmgr.getMsatEnv();

        long term = mathsat.api.msat_make_copy_from(absEnv, mf.getTerm(),
                                                    msatEnv);
        assert(!mathsat.api.MSAT_ERROR_TERM(term));

        // build the definition of the predicates, and instantiate them
        PredInfo predinfo = buildPredList(mmgr, predicates);
        long preddef = predinfo.predDef;
        long[] important = predinfo.important;
        Collection<String> predvars = predinfo.allVars;
        for (int i = 0; i < important.length; ++i) {
            important[i] = mathsat.api.msat_make_copy_from(
                    absEnv, important[i], msatEnv);
        }
        // update the SSA map, by instantiating all the uninstantiated
        // variables that occur in the predicates definitions (at index 1)
        for (String var : predvars) {
            if (ssa.getIndex(var) < 0) {
                ssa.setIndex(var, 1);
            }
        }
        // instantiate the definitions with the right SSA
        MathsatSymbolicFormula inst = (MathsatSymbolicFormula)mmgr.instantiate(
                new MathsatSymbolicFormula(preddef), ssa);
        preddef = mathsat.api.msat_make_copy_from(absEnv, inst.getTerm(),
                                                  msatEnv);
        // build the formula and send it to the absEnv
        long formula = mathsat.api.msat_make_and(absEnv, term, preddef);
        mathsat.api.msat_add_theory(absEnv, mathsat.api.MSAT_UF);
        if (CPAMain.cpaConfig.getBooleanValue(
                "cpas.symbpredabs.mathsat.useIntegers")) {
            mathsat.api.msat_add_theory(absEnv, mathsat.api.MSAT_LIA);
        } else {
            mathsat.api.msat_add_theory(absEnv, mathsat.api.MSAT_LRA);
        }
        mathsat.api.msat_set_theory_combination(absEnv,
                                                mathsat.api.MSAT_COMB_DTC);
        mathsat.api.msat_assert_formula(absEnv, formula);

        int abs = bddManager.getZero();
        AllSatCallback func = new AllSatCallback(abs, msatEnv, absEnv);
        int numModels = mathsat.api.msat_all_sat(absEnv, important, func);
        assert(numModels != -1);

        mathsat.api.msat_destroy_env(absEnv);

        if (numModels == -2) {
            abs = bddManager.getOne();
            return new BDDAbstractFormula(abs);
        } else {
            return new BDDAbstractFormula(func.getBDD());
        }
    }


    /**
     * Given an abstract formula (which is a BDD over the predicates), build
     * its concrete representation (which is a MathSAT formula corresponding
     * to the BDD, in which each predicate is replaced with its definition)
     */
    public SymbolicFormula toConcrete(SymbolicFormulaManager mgr,
            AbstractFormula af) {
        MathsatSymbolicFormulaManager mmgr = (MathsatSymbolicFormulaManager)mgr;
        BDDAbstractFormula bddaf = (BDDAbstractFormula)af;
        int bdd = bddaf.getBDD();
        long msatEnv = mmgr.getMsatEnv();

        Map<Integer, Long> cache;
        if (entailsUseCache) {
            cache = toConcreteCache;
        } else {
            cache = new HashMap<Integer, Long>();
        }
        Stack<Integer> toProcess = new Stack<Integer>();

        cache.put(bddManager.getOne(), mathsat.api.msat_make_true(msatEnv));
        cache.put(bddManager.getZero(), mathsat.api.msat_make_false(msatEnv));

        toProcess.push(new Integer(bdd));
        while (!toProcess.empty()) {
            Integer n = toProcess.peek();
            if (cache.containsKey(n)) {
                toProcess.pop();
                continue;
            }
            boolean childrenDone = true;
            long m1 = mathsat.api.MSAT_MAKE_ERROR_TERM();
            long m2 = mathsat.api.MSAT_MAKE_ERROR_TERM();
            Integer c1 = bddManager.getThen(n);
            Integer c2 = bddManager.getElse(n);
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
                assert(!mathsat.api.MSAT_ERROR_TERM(m1));
                assert(!mathsat.api.MSAT_ERROR_TERM(m2));

                toProcess.pop();
                Integer var = bddManager.getVar(n);

                assert(bddPredicateToMsatAtom.containsKey(var));

                long matom = bddPredicateToMsatAtom.get(var).getSecond();
                long ite = mathsat.api.msat_make_ite(msatEnv, matom, m1, m2);
                cache.put(n, ite);
            }
        }

        assert(cache.containsKey(bdd));

        return new MathsatSymbolicFormula(cache.get(bdd));
    }


    public boolean isFalse(AbstractFormula f) {
        return ((BDDAbstractFormula)f).getBDD() == bddManager.getZero();
    }


    public AbstractFormula makeTrue() {
        int t = bddManager.getOne();
        return new BDDAbstractFormula(t);
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

    public JavaBDD getBddManager() {
      return bddManager;
    }

}
