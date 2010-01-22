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
package cpa.symbpredabs.summary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;
import java.util.logging.Level;

import symbpredabstraction.SSAMap;
import symbpredabstraction.interfaces.AbstractFormula;
import symbpredabstraction.interfaces.AbstractFormulaManager;
import symbpredabstraction.interfaces.InterpolatingTheoremProver;
import symbpredabstraction.interfaces.Predicate;
import symbpredabstraction.interfaces.SymbolicFormula;
import symbpredabstraction.interfaces.SymbolicFormulaManager;
import symbpredabstraction.interfaces.TheoremProver;
import symbpredabstraction.mathsat.MathsatFormulaManager;
import symbpredabstraction.mathsat.MathsatAbstractionPrinter;
import symbpredabstraction.mathsat.MathsatSymbolicFormula;
import symbpredabstraction.mathsat.MathsatSymbolicFormulaManager;
import symbpredabstraction.trace.CounterexampleTraceInfo;
import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFANode;
import cfa.objectmodel.c.FunctionDefinitionNode;
import cfa.objectmodel.c.ReturnEdge;
import cmdline.CPAMain;

import common.Pair;

/**
 * Implementation of SummaryAbstractFormulaManager that works with BDDs for
 * abstraction and MathSAT terms for concrete formulas
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class BDDMathsatSummaryAbstractManager<T> extends
        MathsatFormulaManager implements
        SummaryAbstractFormulaManager {

    // some statistics. All times are in milliseconds
    public class Stats {
        public long abstractionMathsatTime = 0;
        public long abstractionMaxMathsatTime = 0;
        public long abstractionBddTime = 0;
        public long abstractionMaxBddTime = 0;
        public int numCallsAbstraction = 0;
        public int numCallsAbstractionCached = 0;
        public long cexAnalysisTime = 0;
        public long cexAnalysisMaxTime = 0;
        public int numCallsCexAnalysis = 0;
        public long abstractionMathsatSolveTime = 0;
        public long abstractionMaxMathsatSolveTime = 0;
        public long cexAnalysisMathsatTime = 0;
        public long cexAnalysisMaxMathsatTime = 0;
        public int numCoverageChecks = 0;
        public long bddCoverageCheckTime = 0;
        public long bddCoverageCheckMaxTime = 0;
        public long cexAnalysisGetUsefulBlocksTime = 0;
        public long cexAnalysisGetUsefulBlocksMaxTime = 0;
    }
    private Stats stats;

    private Map<Pair<CFANode, CFANode>, Pair<MathsatSymbolicFormula, SSAMap>>
        abstractionTranslationCache;
    private Map<Pair<SymbolicFormula, Vector<SymbolicFormula>>, AbstractFormula>
        abstractionCache;
    boolean useCache;

    static abstract class KeyWithTimeStamp {
        public long timeStamp;

        public KeyWithTimeStamp() {
            updateTimeStamp();
        }

        public void updateTimeStamp() {
            timeStamp = System.currentTimeMillis();
        }
    }

    static class CartesianAbstractionCacheKey extends KeyWithTimeStamp {
        SymbolicFormula formula;
        Predicate pred;

        public CartesianAbstractionCacheKey(SymbolicFormula f, Predicate p) {
            super();
            formula = f;
            pred = p;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o instanceof CartesianAbstractionCacheKey) {
                CartesianAbstractionCacheKey c =
                    (CartesianAbstractionCacheKey)o;
                return formula.equals(c.formula) && pred.equals(c.pred);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return formula.hashCode() ^ pred.hashCode();
        }
    }
    static class FeasibilityCacheKey extends KeyWithTimeStamp {
        SymbolicFormula f;

        public FeasibilityCacheKey(SymbolicFormula fm) {
            super();
            f = fm;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o instanceof FeasibilityCacheKey) {
                return f.equals(((FeasibilityCacheKey)o).f);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return f.hashCode();
        }
    }

    static class TimeStampCache<Key extends KeyWithTimeStamp, Value>
        extends HashMap<Key, Value> {
        /**
         * default value
         */
        private static final long serialVersionUID = 1L;
        private int maxSize;

        class TimeStampComparator implements Comparator<KeyWithTimeStamp> {
            @Override
            public int compare(KeyWithTimeStamp arg0, KeyWithTimeStamp arg1) {
                long r = arg0.timeStamp - arg1.timeStamp;
                return r < 0 ? -1 : (r > 0 ? 1 : 0);
            }
        }

        private TimeStampComparator cmp;

        public TimeStampCache(int maxSize) {
            super();
            this.maxSize = maxSize;
            cmp = new TimeStampComparator();
        }

        @SuppressWarnings("unchecked")
        @Override
        public Value get(Object o) {
            Key key = (Key)o;
            key.updateTimeStamp();
            return super.get(key);
        }

        @Override
        public Value put(Key key, Value value) {
            key.updateTimeStamp();
            compact();
            return super.put(key, value);
        }

        private void compact() {
            if (size() > maxSize) {
                // find the half oldest entries, and get rid of them...
                KeyWithTimeStamp[] keys = keySet().toArray(
                        new KeyWithTimeStamp[0]);
                Arrays.sort(keys, cmp);
                for (int i = 0; i < keys.length/2; ++i) {
                    remove(keys[i]);
                }
            }
        }
    }

    // cache for cartesian abstraction queries. For each predicate, the values
    // are -1: predicate is false, 0: predicate is don't care,
    // 1: predicate is true
    protected TimeStampCache<CartesianAbstractionCacheKey, Byte>
        cartesianAbstractionCache;
    protected TimeStampCache<FeasibilityCacheKey, Boolean> feasibilityCache;

    private MathsatAbstractionPrinter absPrinter = null;
    private boolean dumpHardAbstractions;

    private TheoremProver thmProver;
    private InterpolatingTheoremProver<T> itpProver;

    public BDDMathsatSummaryAbstractManager(
            AbstractFormulaManager amgr,
            MathsatSymbolicFormulaManager smgr,
            TheoremProver prover,
            InterpolatingTheoremProver<T> interpolator) {
        super(amgr, smgr);
        stats = new Stats();
        abstractionTranslationCache =
            new HashMap<Pair<CFANode, CFANode>,
                        Pair<MathsatSymbolicFormula, SSAMap>>();
        dumpHardAbstractions = CPAMain.cpaConfig.getBooleanValue(
                "cpas.symbpredabs.mathsat.dumpHardAbstractionQueries");
        thmProver = prover;
        itpProver = interpolator;

        abstractionCache =
            new HashMap<Pair<SymbolicFormula, Vector<SymbolicFormula>>,
                        AbstractFormula>();
        useCache = CPAMain.cpaConfig.getBooleanValue(
                "cpas.symbpredabs.mathsat.useCache");

            final int MAX_CACHE_SIZE = 100000;
            cartesianAbstractionCache =
                new TimeStampCache<CartesianAbstractionCacheKey, Byte>(
                        MAX_CACHE_SIZE);
            feasibilityCache =
                new TimeStampCache<FeasibilityCacheKey, Boolean>(
                        MAX_CACHE_SIZE);
    }

    public Stats getStats() { return stats; }

    // builds the SymbolicFormula corresponding to the path between "e" and
    // "succ". In the purely explicit case, this would be just the operation
    // attached to the edge connecting "e" and "succ", but in our case this is
    // actually a loop-free subgraph of the original CFA
    private Pair<SymbolicFormula, SSAMap> buildConcreteFormula(
            MathsatSummaryFormulaManager mgr,
            SummaryAbstractElement e, SummaryAbstractElement succ,
            boolean replaceAssignments) {
        // first, get all the paths in e that lead to succ
        Collection<Pair<SymbolicFormula, SSAMap>> relevantPaths =
            new Vector<Pair<SymbolicFormula, SSAMap>>();
        for (CFANode leaf : e.getLeaves()) {
            for (int i = 0; i < leaf.getNumLeavingEdges(); ++i) {
                CFAEdge edge = leaf.getLeavingEdge(i);
                InnerCFANode s = (InnerCFANode)edge.getSuccessor();
                if (s.getSummaryNode().equals(succ.getLocation())) {
                    // ok, this path is relevant
                    relevantPaths.add(e.getPathFormula(leaf));

                    CPAMain.logManager.log(Level.ALL, "DEBUG_3",
                                   "FOUND RELEVANT PATH, leaf: ",
                                   leaf.getNodeNumber());
                    CPAMain.logManager.log(Level.ALL, "DEBUG_3",
                                   "Formula: ",
                                   mathsat.api.msat_term_id(
                                   (((MathsatSymbolicFormula)e.getPathFormula(
                                           leaf).getFirst())).getTerm()));
                }
            }
        }
        // now, we want to create a new formula that is the OR of all the
        // possible paths. So we merge the SSA maps and OR the formulas
        SSAMap ssa = new SSAMap();
        SymbolicFormula f = mgr.makeFalse();
        for (Pair<SymbolicFormula, SSAMap> p : relevantPaths) {
            Pair<Pair<SymbolicFormula, SymbolicFormula>, SSAMap> mp =
                mgr.mergeSSAMaps(ssa, p.getSecond());
            SymbolicFormula curf = p.getFirst();
            if (replaceAssignments) {
                curf = mgr.replaceAssignments((MathsatSymbolicFormula)curf);
            }
            f = mgr.makeAnd(f, mp.getFirst().getFirst());
            curf = mgr.makeAnd(curf, mp.getFirst().getSecond());
            f = mgr.makeOr(f, curf);
            ssa = mp.getSecond();
        }

        return new Pair<SymbolicFormula, SSAMap>(f, ssa);
    }

    // computes the abstract post from "e" to "succ"
    @Override
    public AbstractFormula buildAbstraction(SummaryFormulaManager mgr,
            SummaryAbstractElement e, SummaryAbstractElement succ,
            Collection<Predicate> predicates) {
        stats.numCallsAbstraction++;
        //return buildBooleanAbstraction(mgr, e, succ, predicates);
        if (CPAMain.cpaConfig.getBooleanValue(
                "cpas.symbpredabs.abstraction.cartesian")) {
            return buildCartesianAbstraction(mgr, e, succ, predicates);
        } else {
            return buildBooleanAbstraction(mgr, e, succ, predicates);
        }
    }

    private AbstractFormula buildBooleanAbstraction(SummaryFormulaManager mgr,
            SummaryAbstractElement e, SummaryAbstractElement succ,
            Collection<Predicate> predicates) {
    	// A SummaryFormulaManager for MathSAT formulas
        MathsatSummaryFormulaManager mmgr = (MathsatSummaryFormulaManager)mgr;

        long startTime = System.currentTimeMillis();

        // get the environment from the manager - this is unique, it is the
        // environment in which all terms are created
        long msatEnv = mmgr.getMsatEnv();

        // first, build the concrete representation of the abstract formula of e
        // this is an abstract formula - specifically it is a bddabstractformula
        // which is basically an integer which represents it
        AbstractFormula abs = e.getAbstraction();
        // create the concrete form of the abstract formula
        // (abstract formula is the bdd representation)
        MathsatSymbolicFormula fabs =
            (MathsatSymbolicFormula)mmgr.instantiate(
                    toConcrete(abs), null);

        CPAMain.logManager.log(Level.ALL, "DEBUG_3", "Abstraction:",
                mathsat.api.msat_term_id(fabs.getTerm()));

        if (isFunctionExit(e)) {
            // we have to take the context before the function call
            // into account, otherwise we are not building the right
            // abstraction!
            if (CPAMain.cpaConfig.getBooleanValue(
                    "cpas.symbpredabs.refinement.addWellScopedPredicates")) {
                // but only if we are adding well-scoped predicates, otherwise
                // this should not be necessary
                AbstractFormula ctx = e.topContextAbstraction();
                MathsatSymbolicFormula fctx =
                    (MathsatSymbolicFormula)mmgr.instantiate(
                            toConcrete(ctx), null);
                fabs = (MathsatSymbolicFormula)mmgr.makeAnd(fabs, fctx);

                CPAMain.logManager.log(Level.ALL, "DEBUG_3",
                        "TAKING CALLING CONTEXT INTO ACCOUNT: ", fctx);
            } else {
              CPAMain.logManager.log(Level.ALL, "DEBUG_3",
                        "NOT TAKING CALLING CONTEXT INTO ACCOUNT,",
                        "as we are not using well-scoped predicates");
            }
        }

        // create an ssamap from concrete formula
        SSAMap absSsa = mmgr.extractSSA(fabs);

        SymbolicFormula f = null;
        SSAMap ssa = null;

        Pair<CFANode, CFANode> key = new Pair<CFANode, CFANode>(
                e.getLocationNode(), succ.getLocationNode());
        if (abstractionTranslationCache.containsKey(key)) {
            Pair<MathsatSymbolicFormula, SSAMap> pc =
                abstractionTranslationCache.get(key);
            f = pc.getFirst();
            ssa = pc.getSecond();
        } else {
        	// take all outgoing edges from e to succ and OR them
            Pair<SymbolicFormula, SSAMap> pc =
                buildConcreteFormula(mmgr, e, succ, false);
//            SymbolicFormula f = pc.getFirst();
//            SSAMap ssa = pc.getSecond();
            f = pc.getFirst();
            ssa = pc.getSecond();

            pc = mmgr.shift(f, absSsa);
            f = mmgr.replaceAssignments((MathsatSymbolicFormula)pc.getFirst());
            ssa = pc.getSecond();

            abstractionTranslationCache.put(key,
                    new Pair<MathsatSymbolicFormula, SSAMap>(
                            (MathsatSymbolicFormula)f, ssa));
        }

        if (CPAMain.cpaConfig.getBooleanValue(
                "cpas.symbpredabs.useBitwiseAxioms")) {
            MathsatSymbolicFormula bitwiseAxioms = mmgr.getBitwiseAxioms(
                    (MathsatSymbolicFormula)f);
            f = mmgr.makeAnd(f, bitwiseAxioms);

            CPAMain.logManager.log(Level.ALL, "DEBUG_3", "ADDED BITWISE AXIOMS:",
                    bitwiseAxioms);
        }

        long term = ((MathsatSymbolicFormula)f).getTerm();
        assert(!mathsat.api.MSAT_ERROR_TERM(term));

        CPAMain.logManager.log(Level.ALL, "DEBUG_2", "Term:", f);


        // build the definition of the predicates, and instantiate them
        PredInfo predinfo = buildPredList(predicates);
        long preddef = predinfo.predDef;
        long[] important = predinfo.important;
        Collection<String> predvars = predinfo.allVars;
        Collection<Pair<String, SymbolicFormula[]>> predlvals =
            predinfo.allFuncs;
        // update the SSA map, by instantiating all the uninstantiated
        // variables that occur in the predicates definitions (at index 1)
        for (String var : predvars) {
            if (ssa.getIndex(var) < 0) {
                ssa.setIndex(var, 1);
            }
        }
        Map<SymbolicFormula, SymbolicFormula> cache =
            new HashMap<SymbolicFormula, SymbolicFormula>();
        for (Pair<String, SymbolicFormula[]> p : predlvals) {
            SymbolicFormula[] args =
                getInstantiatedAt(p.getSecond(), ssa, cache);
            if (ssa.getIndex(p.getFirst(), args) < 0) {
                ssa.setIndex(p.getFirst(), args, 1);
            }
        }

        if (CPAMain.logManager.getLogLevel().intValue() <= Level.ALL.intValue()) {
            StringBuffer importantStrBuf = new StringBuffer();
            for (long t : important) {
                importantStrBuf.append(mathsat.api.msat_term_repr(t));
                importantStrBuf.append(" ");
            }
            CPAMain.logManager.log(Level.ALL, "DEBUG_1",
                           "IMPORTANT SYMBOLS (", important.length, "): ",
                           importantStrBuf);
        }

        // first, create the new formula corresponding to
        // (f & edges from e to succ)
        // TODO - at the moment, we assume that all the edges connecting e and
        // succ have no statement or assertion attached (i.e. they are just
        // return edges or gotos). This might need to change in the future!!
        // (So, for now we don't need to to anything...)

        // instantiate the definitions with the right SSA
        MathsatSymbolicFormula inst = (MathsatSymbolicFormula)mmgr.instantiate(
                new MathsatSymbolicFormula(preddef), ssa);
        preddef = inst.getTerm();
        long curstate = fabs.getTerm();

        // the formula is (curstate & term & preddef)
        // build the formula and send it to the absEnv
        long formula = mathsat.api.msat_make_and(msatEnv,
                mathsat.api.msat_make_and(msatEnv, curstate, term), preddef);
        SymbolicFormula fm = new MathsatSymbolicFormula(formula);
        Vector<SymbolicFormula> imp = new Vector<SymbolicFormula>();
        imp.ensureCapacity(important.length);
        for (long p : important) {
            imp.add(new MathsatSymbolicFormula(p));
        }

        CPAMain.logManager.log(Level.ALL, "DEBUG_2",
                "COMPUTING ALL-SMT ON FORMULA: ", fm);

        Pair<SymbolicFormula, Vector<SymbolicFormula>> absKey =
            new Pair<SymbolicFormula, Vector<SymbolicFormula>>(fm, imp);
        AbstractFormula result = null;
        if (useCache && abstractionCache.containsKey(absKey)) {
            ++stats.numCallsAbstractionCached;
            result = abstractionCache.get(absKey);
        } else {
            AllSatCallback func = new AllSatCallback();
            long msatSolveStartTime = System.currentTimeMillis();
            int numModels = thmProver.allSat(fm, imp, func);
            assert(numModels != -1);
            long msatSolveEndTime = System.currentTimeMillis();

            // update statistics
            long endTime = System.currentTimeMillis();
            long msatSolveTime =
                (msatSolveEndTime - msatSolveStartTime) - func.totalTime;
            long abstractionMsatTime = (endTime - startTime) - func.totalTime;
            stats.abstractionMaxMathsatTime =
                Math.max(abstractionMsatTime, stats.abstractionMaxMathsatTime);
            stats.abstractionMaxBddTime =
                Math.max(func.totalTime, stats.abstractionMaxBddTime);
            stats.abstractionMathsatTime += abstractionMsatTime;
            stats.abstractionBddTime += func.totalTime;
            stats.abstractionMathsatSolveTime += msatSolveTime;
            stats.abstractionMaxMathsatSolveTime =
                Math.max(msatSolveTime, stats.abstractionMaxMathsatSolveTime);

            if (abstractionMsatTime > 10000 && dumpHardAbstractions) {
                // we want to dump "hard" problems...
                if (absPrinter == null) {
                    absPrinter = new MathsatAbstractionPrinter(mmgr, "abs");
                }
                absPrinter.printMsatFormat(curstate, term, preddef, important);
                absPrinter.printNusmvFormat(curstate, term, preddef, important);
                absPrinter.nextNum();
            }

            if (numModels == -2) {
                result = amgr.makeTrue();
            } else {
                result = func.getBDD();
            }
            if (useCache) {
                abstractionCache.put(absKey, result);
            }
        }

        return result;

    }


    // cartesian abstraction
    protected AbstractFormula buildCartesianAbstraction(
            SymbolicFormulaManager mgr, SummaryAbstractElement e,
            SummaryAbstractElement succ,
            Collection<Predicate> predicates) {
        long startTime = System.currentTimeMillis();

        MathsatSummaryFormulaManager mmgr = (MathsatSummaryFormulaManager)mgr;

        long msatEnv = mmgr.getMsatEnv();

        thmProver.init(TheoremProver.CARTESIAN_ABSTRACTION);

        if (isFunctionExit(e)) {
            // we have to take the context before the function call
            // into account, otherwise we are not building the right
            // abstraction!
            if (CPAMain.cpaConfig.getBooleanValue(
                    "cpas.symbpredabs.refinement.addWellScopedPredicates")) {
                // but only if we are adding well-scoped predicates, otherwise
                // this should not be necessary
		assert(false); // TODO
                // AbstractFormula ctx = e.topContextAbstraction();
                // MathsatSymbolicFormula fctx =
                //     (MathsatSymbolicFormula)mmgr.instantiate(
                //             toConcrete(mmgr, ctx), null);
                // fabs = (MathsatSymbolicFormula)mmgr.makeAnd(fabs, fctx);

                // CPAMain.logManager.log(Level.ALL, "DEBUG_3",
                //         "TAKING CALLING CONTEXT INTO ACCOUNT: ", fctx);
            } else {
              CPAMain.logManager.log(Level.ALL, "DEBUG_3",
                        "NOT TAKING CALLING CONTEXT INTO ACCOUNT,",
                        "as we are not using well-scoped predicates");
            }
        }

        Pair<SymbolicFormula, SSAMap> pc =
            buildConcreteFormula(mmgr, e, succ, false);
        SymbolicFormula f = pc.getFirst();
        SSAMap ssa = pc.getSecond();

        f = mmgr.replaceAssignments((MathsatSymbolicFormula)pc.getFirst());
        SymbolicFormula fkey = f;

        byte[] predVals = null;
        final byte NO_VALUE = -2;
        if (useCache) {
            predVals = new byte[predicates.size()];
            int predIndex = -1;
            for (Predicate p : predicates) {
                ++predIndex;
                CartesianAbstractionCacheKey key =
                    new CartesianAbstractionCacheKey(f, p);
                if (cartesianAbstractionCache.containsKey(key)) {
                    predVals[predIndex] = cartesianAbstractionCache.get(key);
                } else {
                    predVals[predIndex] = NO_VALUE;
                }
            }
        }

        boolean skipFeasibilityCheck = false;
        if (useCache) {
            FeasibilityCacheKey key = new FeasibilityCacheKey(f);
            if (feasibilityCache.containsKey(key)) {
                skipFeasibilityCheck = true;
                if (!feasibilityCache.get(key)) {
                    thmProver.reset();
                    // abstract post leads to false, we can return immediately
                    return amgr.makeFalse();
                }
            }
        }

        if (CPAMain.cpaConfig.getBooleanValue(
                "cpas.symbpredabs.useBitwiseAxioms")) {
            MathsatSymbolicFormula bitwiseAxioms = mmgr.getBitwiseAxioms(
                    (MathsatSymbolicFormula)f);
            f = mmgr.makeAnd(f, bitwiseAxioms);

            CPAMain.logManager.log(Level.ALL, "DEBUG_3", "ADDED BITWISE AXIOMS:",
                    bitwiseAxioms);
        }

        long solveStartTime = System.currentTimeMillis();

        if (!skipFeasibilityCheck) {
            //++stats.abstractionNumMathsatQueries;
            if (thmProver.isUnsat(f)) {
                thmProver.reset();
                if (useCache) {
                    FeasibilityCacheKey key = new FeasibilityCacheKey(fkey);
                    if (feasibilityCache.containsKey(key)) {
                        assert(feasibilityCache.get(key) == false);
                    }
                    feasibilityCache.put(key, false);
                }
                return amgr.makeFalse();
            } else {
                if (useCache) {
                    FeasibilityCacheKey key = new FeasibilityCacheKey(fkey);
                    if (feasibilityCache.containsKey(key)) {
                        assert(feasibilityCache.get(key) == true);
                    }
                    feasibilityCache.put(key, true);
                }
            }
        } else {
            //++stats.abstractionNumCachedQueries;
        }

        thmProver.push(f);

        long totBddTime = 0;

        AbstractFormula absbdd = amgr.makeTrue();

        // check whether each of the predicate is implied in the next state...
        Set<String> predvars = new HashSet<String>();
        Set<Pair<String, SymbolicFormula[]>> predlvals =
            new HashSet<Pair<String, SymbolicFormula[]>>();
        Map<SymbolicFormula, SymbolicFormula> predLvalsCache =
            new HashMap<SymbolicFormula, SymbolicFormula>();

        int predIndex = -1;
        for (Predicate p : predicates) {
            ++predIndex;
            if (useCache && predVals[predIndex] != NO_VALUE) {
                long startBddTime = System.currentTimeMillis();
                AbstractFormula v = p.getFormula();
                if (predVals[predIndex] == -1) { // pred is false
                    v = amgr.makeNot(v);
                    absbdd = amgr.makeAnd(absbdd, v);
                } else if (predVals[predIndex] == 1) { // pred is true
                    absbdd = amgr.makeAnd(absbdd, v);
                }
                long endBddTime = System.currentTimeMillis();
                totBddTime += (endBddTime - startBddTime);
                //++stats.abstractionNumCachedQueries;
            } else {
                Pair<MathsatSymbolicFormula, MathsatSymbolicFormula> pi =
                    getPredicateNameAndDef(p);

                // update the SSA map, by instantiating all the uninstantiated
                // variables that occur in the predicates definitions
                // (at index 1)
                predvars.clear();
                predlvals.clear();
                collectVarNames(pi.getSecond().getTerm(),
                        predvars, predlvals);
                for (String var : predvars) {
                    if (ssa.getIndex(var) < 0) {
                        ssa.setIndex(var, 1);
                    }
                }
                for (Pair<String, SymbolicFormula[]> pp : predlvals) {
                    SymbolicFormula[] args =
                        getInstantiatedAt(pp.getSecond(), ssa,
                                predLvalsCache);
                    if (ssa.getIndex(pp.getFirst(), args) < 0) {
                        ssa.setIndex(pp.getFirst(), args, 1);
                    }
                }


                CPAMain.logManager.log(Level.ALL, "DEBUG_1",
                        "CHECKING VALUE OF PREDICATE: ", pi.getFirst());

                // instantiate the definition of the predicate
                MathsatSymbolicFormula inst =
                    (MathsatSymbolicFormula)mmgr.instantiate(
                            pi.getSecond(), ssa);

                boolean isTrue = false, isFalse = false;
                // check whether this predicate has a truth value in the next
                // state
                long predTrue = inst.getTerm();
//                predTrue = mathsat.api.msat_make_copy_from(
//                        absEnv, inst.getTerm(), msatEnv);
                long predFalse = mathsat.api.msat_make_not(msatEnv, predTrue);

                //++stats.abstractionNumMathsatQueries;
                if (thmProver.isUnsat(
                        new MathsatSymbolicFormula(predFalse))) {
                    isTrue = true;
                }

                if (isTrue) {
                    long startBddTime = System.currentTimeMillis();
                    AbstractFormula v = p.getFormula();
                    absbdd = amgr.makeAnd(absbdd, v);
                    long endBddTime = System.currentTimeMillis();
                    totBddTime += (endBddTime - startBddTime);
                } else {
                    // check whether it's false...
                    //++stats.abstractionNumMathsatQueries;
                    if (thmProver.isUnsat(
                            new MathsatSymbolicFormula(predTrue))) {
                        isFalse = true;
                    }

                    if (isFalse) {
                        long startBddTime = System.currentTimeMillis();
                        AbstractFormula v = p.getFormula();
                        v = amgr.makeNot(v);
                        absbdd = amgr.makeAnd(absbdd, v);
                        long endBddTime = System.currentTimeMillis();
                        totBddTime += (endBddTime - startBddTime);
                    }
                }

                if (useCache) {
                    if (predVals[predIndex] != NO_VALUE) {
                        assert(isTrue ? predVals[predIndex] == 1 :
                            (isFalse ? predVals[predIndex] == -1 :
                                predVals[predIndex] == 0));
                    }
                    CartesianAbstractionCacheKey key =
                        new CartesianAbstractionCacheKey(fkey, p);
                    byte val = (byte)(isTrue ? 1 : (isFalse ? -1 : 0));
                    cartesianAbstractionCache.put(key, val);
                }
            }
        }
        long solveEndTime = System.currentTimeMillis();

        thmProver.pop();
        thmProver.reset();

        // update statistics
        long endTime = System.currentTimeMillis();
        long solveTime = (solveEndTime - solveStartTime) - totBddTime;
        long msatTime = (endTime - startTime) - totBddTime;
        stats.abstractionMaxMathsatTime =
            Math.max(msatTime, stats.abstractionMaxMathsatTime);
        stats.abstractionMaxBddTime =
            Math.max(totBddTime, stats.abstractionMaxBddTime);
        stats.abstractionMathsatTime += msatTime;
        stats.abstractionBddTime += totBddTime;
        stats.abstractionMathsatSolveTime += solveTime;
        stats.abstractionMaxMathsatSolveTime =
            Math.max(solveTime, stats.abstractionMaxMathsatSolveTime);

        return absbdd;
    }

    @Override
    public CounterexampleTraceInfo buildCounterexampleTrace(
            SummaryFormulaManager mgr,
            Deque<SummaryAbstractElement> abstractTrace) {
        assert(abstractTrace.size() > 1);

        long startTime = System.currentTimeMillis();
        stats.numCallsCexAnalysis++;

        // create the DAG formula corresponding to the abstract trace. We create
        // n formulas, one per interpolation group
        SSAMap ssa = null;
        MathsatSummaryFormulaManager mmgr = (MathsatSummaryFormulaManager)mgr;

        Vector<SymbolicFormula> f = new Vector<SymbolicFormula>();

        CPAMain.logManager.log(Level.ALL, "DEBUG_1", "BUILDING COUNTEREXAMPLE TRACE");
        CPAMain.logManager.log(Level.ALL, "DEBUG_1", "ABSTRACT TRACE:", abstractTrace);

        //printFuncNamesInTrace(abstractTrace);

        Object[] abstarr = abstractTrace.toArray();
        SummaryAbstractElement cur = (SummaryAbstractElement)abstarr[0];

        boolean theoryCombinationNeeded = false;
        boolean noDtc = CPAMain.cpaConfig.getBooleanValue(
                "cpas.symbpredabs.mathsat.useDtc") == false;

        MathsatSymbolicFormula bitwiseAxioms =
            (MathsatSymbolicFormula)mmgr.makeTrue();

        for (int i = 1; i < abstarr.length; ++i) {
            SummaryAbstractElement e = (SummaryAbstractElement)abstarr[i];
            Pair<SymbolicFormula, SSAMap> p =
                buildConcreteFormula(mmgr, cur, e, (ssa == null));
            SSAMap newssa = null;
            if (ssa != null) {
              CPAMain.logManager.log(Level.ALL, "DEBUG_3", "SHIFTING:", p.getFirst(),
                        " WITH SSA: ", ssa);
                p = mmgr.shift(p.getFirst(), ssa);
                newssa = p.getSecond();
                CPAMain.logManager.log(Level.ALL, "DEBUG_3", "RESULT:", p.getFirst(),
                               " SSA: ", newssa);
                newssa.update(ssa);
            } else {
              CPAMain.logManager.log(Level.ALL, "DEBUG_3", "INITIAL:", p.getFirst(),
                               " SSA: ", p.getSecond());
                newssa = p.getSecond();
            }
            boolean hasUf = false;
            if (!noDtc) {
                hasUf = mmgr.hasUninterpretedFunctions(
                        (MathsatSymbolicFormula)p.getFirst());
                theoryCombinationNeeded |= hasUf;
            }
            f.add(p.getFirst());
            ssa = newssa;
            cur = e;

            if (hasUf && CPAMain.cpaConfig.getBooleanValue(
                    "cpas.symbpredabs.useBitwiseAxioms")) {
                MathsatSymbolicFormula a = mmgr.getBitwiseAxioms(
                        (MathsatSymbolicFormula)p.getFirst());
                bitwiseAxioms = (MathsatSymbolicFormula)mmgr.makeAnd(
                        bitwiseAxioms, a);
            }

            CPAMain.logManager.log(Level.ALL, "DEBUG_2", "Adding formula:", p.getFirst());
//                    mathsat.api.msat_term_id(
//                            ((MathsatSymbolicFormula)p.getFirst()).getTerm()));
        }
        if (CPAMain.cpaConfig.getBooleanValue(
                "cpas.symbpredabs.useBitwiseAxioms")) {
          CPAMain.logManager.log(Level.ALL, "DEBUG_3", "ADDING BITWISE AXIOMS TO THE",
                    "LAST GROUP: ", bitwiseAxioms);
            f.setElementAt(mmgr.makeAnd(f.elementAt(f.size()-1), bitwiseAxioms),
                    f.size()-1);
        }

        CPAMain.logManager.log(Level.ALL, "DEBUG_3",
                       "Checking feasibility of abstract trace");

        // now f is the DAG formula which is satisfiable iff there is a
        // concrete counterexample
        //
        // create a working environment
        itpProver.init();

        boolean shortestTrace = CPAMain.cpaConfig.getBooleanValue(
                "cpas.symbpredabs.shortestCexTrace");
        boolean useSuffix = CPAMain.cpaConfig.getBooleanValue(
                "cpas.symbpredabs.shortestCexTraceUseSuffix");
        boolean useZigZag = CPAMain.cpaConfig.getBooleanValue(
                "cpas.symbpredabs.shortestCexTraceZigZag");

        long msatSolveTimeStart = System.currentTimeMillis();

        boolean unsat = false;
        int res = -1;

        List<T> itpGroupsIds = new ArrayList<T>(f.size());
        for (int i = 0; i < f.size(); i++) {
          itpGroupsIds.add(null);
        }
        
        //dumpInterpolationProblem(mmgr, f, "itp");

        if (shortestTrace && CPAMain.cpaConfig.getBooleanValue(
                "cpas.symbpredabs.explicit.getUsefulBlocks")) {
            long gubStart = System.currentTimeMillis();
            f = getUsefulBlocks(mmgr, f, theoryCombinationNeeded,
                                useSuffix, useZigZag, false);
            long gubEnd = System.currentTimeMillis();
            stats.cexAnalysisGetUsefulBlocksTime += gubEnd - gubStart;
            stats.cexAnalysisGetUsefulBlocksMaxTime = Math.max(
                    stats.cexAnalysisGetUsefulBlocksMaxTime, gubEnd - gubStart);
            // set shortestTrace to false, so we perform only one final call
            // to msat_solve
            shortestTrace = false;
        }


        if (!shortestTrace || !useZigZag) {
            for (int i = useSuffix ? f.size()-1 : 0;
            useSuffix ? i >= 0 : i < f.size(); i += useSuffix ? -1 : 1) {
                SymbolicFormula fm = f.elementAt(i);
                itpGroupsIds.set(i, itpProver.addFormula(fm));
                if (shortestTrace && !fm.isTrue()) {
                    if (itpProver.isUnsat()) {
                        res = 0;
                        // we need to add the other formulas to the itpProver
                        // anyway, so it can setup its internal state properly
                        for (int j = i+(useSuffix ? -1 : 1);
                        useSuffix ? j >= 0 : j < f.size();
                        j += useSuffix ? -1 : 1) {
                          itpGroupsIds.set(j, itpProver.addFormula(f.elementAt(j)));
                        }
                        break;
                    } else {
                        res = 1;
                    }
                } else {
                    res = -1;
                }
            }
            if (!shortestTrace || res == -1) {
                unsat = itpProver.isUnsat();
            } else {
                unsat = res == 0;
            }
        } else { // shortestTrace && useZigZag
            int e = f.size()-1;
            int s = 0;
            boolean fromStart = false;
            while (true) {
                int i = fromStart ? s : e;
                if (fromStart) s++;
                else e--;
                fromStart = !fromStart;
                SymbolicFormula fm = f.elementAt(i);
                itpGroupsIds.set(i, itpProver.addFormula(fm));
                if (!fm.isTrue()) {
                    if (itpProver.isUnsat()) {
                        res = 0;
                        for (int j = s; j <= e; ++j) {
                          itpGroupsIds.set(j, itpProver.addFormula(f.elementAt(j)));
                        }
                        break;
                    } else {
                        res = 1;
                    }
                } else {
                    res = -1;
                }
                if (s > e) break;
            }
            assert(res != -1);
            unsat = res == 0;
        }

        long msatSolveTimeEnd = System.currentTimeMillis();
        long msatSolveTime = msatSolveTimeEnd - msatSolveTimeStart;

        assert itpGroupsIds.size() == f.size();
        assert !itpGroupsIds.contains(null); // has to be filled completely
        
        CounterexampleTraceInfo info = null;

        long msatEnv = mmgr.getMsatEnv();

        if (unsat) {
            //dumpInterpolationProblem(mmgr, f, "itp");
            // the counterexample is spurious. Extract the predicates from
            // the interpolants
            info = new CounterexampleTraceInfo(true);
            boolean splitItpAtoms = CPAMain.cpaConfig.getBooleanValue(
                    "cpas.symbpredabs.refinement.splitItpAtoms");
            // how to partition the trace into (A, B) depends on whether
            // there are function calls involved or not: in general, A
            // is the trace from the entry point of the current function
            // to the current point, and B is everything else. To implement
            // this, we keep track of which function we are currently in.
            Stack<Integer> entryPoints = new Stack<Integer>();
            entryPoints.push(0);
            for (int i = 1; i < f.size(); ++i) {
                int start_of_a = entryPoints.peek();
                if (!CPAMain.cpaConfig.getBooleanValue(
                       "cpas.symbpredabs.refinement.addWellScopedPredicates")) {
                    // if we don't want "well-scoped" predicates, we always
                    // cut from the beginning
                    start_of_a = 0;
                }

                int sz = i - start_of_a;
                Vector<SymbolicFormula> formulasOfA =
                    new Vector<SymbolicFormula>();
                formulasOfA.ensureCapacity(sz);
                for (int j = 0; j < sz; ++j) {
                    formulasOfA.add(f.elementAt(j+start_of_a));
                }
                List<T> idsOfA = itpGroupsIds.subList(start_of_a, start_of_a+sz);
                assert formulasOfA.size() == idsOfA.size();
                msatSolveTimeStart = System.currentTimeMillis();
                SymbolicFormula itp = itpProver.getInterpolant(idsOfA);
                msatSolveTimeEnd = System.currentTimeMillis();
                msatSolveTime += msatSolveTimeEnd - msatSolveTimeStart;

                Collection<SymbolicFormula> atoms = mmgr.extractAtoms(
                            itp, true, splitItpAtoms, false);
                Set<Predicate> preds = buildPredicates(atoms);
                SummaryAbstractElement s1 =
                    (SummaryAbstractElement)abstarr[i];
                info.addPredicatesForRefinement(s1, preds);

                CPAMain.logManager.log(Level.ALL, "DEBUG_1",
                        "Got interpolant(", i, "):", itp, ", location:", s1);
                CPAMain.logManager.log(Level.ALL, "DEBUG_1", "Preds for",
                        s1.getLocation(), ": ", preds);

                // If we are entering or exiting a function, update the stack
                // of entry points
                SummaryAbstractElement e = (SummaryAbstractElement)abstarr[i];
                if (isFunctionEntry(e)) {
                  CPAMain.logManager.log(Level.ALL, "DEBUG_3",
                            "Pushing entry point, function: ",
                            e.getLocation().getInnerNode().getFunctionName());
                    entryPoints.push(i);
                }
                if (isFunctionExit(e)) {
                  CPAMain.logManager.log(Level.ALL, "DEBUG_3",
                            "Popping entry point, returning from function: ",
                            e.getLocation().getInnerNode().getFunctionName());
                    entryPoints.pop();

//                    SummaryAbstractElement s1 =
//                        (SummaryAbstractElement)abstarr[i];
                    //pmap.update((CFANode)s1.getLocation(), preds);
                }
            }
        } else {
            // this is a real bug, notify the user
            info = new CounterexampleTraceInfo(false);
            // TODO - reconstruct counterexample
            // For now, we dump the asserted formula to a user-specified file
            dumpFormulasToFile(f, CPAMain.cpaConfig.getProperty("cpas.symbpredabs.refinement.msatCexFile"));
        }

        itpProver.reset();

        // update stats
        long endTime = System.currentTimeMillis();
        long totTime = endTime - startTime;
        stats.cexAnalysisTime += totTime;
        stats.cexAnalysisMaxTime = Math.max(totTime, stats.cexAnalysisMaxTime);
        stats.cexAnalysisMathsatTime += msatSolveTime;
        stats.cexAnalysisMaxMathsatTime =
            Math.max(msatSolveTime, stats.cexAnalysisMaxMathsatTime);

        return info;
    }

    private boolean isFunctionExit(SummaryAbstractElement e) {
        CFANode inner = e.getLocation().getInnerNode();
        return (inner.getNumLeavingEdges() == 1 &&
                inner.getLeavingEdge(0) instanceof ReturnEdge);
    }

    private boolean isFunctionEntry(SummaryAbstractElement e) {
        CFANode inner = e.getLocation().getInnerNode();
        return (inner.getNumEnteringEdges() > 0 &&
                inner.getEnteringEdge(0).getPredecessor() instanceof
                FunctionDefinitionNode);
    }

    // TODO move this statistics to stop operator 
    /*@Override
    public boolean entails(AbstractFormula f1, AbstractFormula f2) {
        long start = System.currentTimeMillis();
        boolean ret = super.entails(f1, f2);
        long end = System.currentTimeMillis();
        stats.bddCoverageCheckMaxTime = Math.max(stats.bddCoverageCheckMaxTime,
                                                (end - start));
        stats.bddCoverageCheckTime += (end - start);
        ++stats.numCoverageChecks;
        return ret;
    }*/

    /*
    private void dumpInterpolationProblem(MathsatSymbolicFormulaManager mmgr,
            Vector<SymbolicFormula> f, String fileNamePattern) {
        long msatEnv = mmgr.getMsatEnv();
        for (int i = 0; i < f.size(); ++i) {
            try {
                PrintWriter out =
                    new PrintWriter(
                            String.format("%s.%02d.msat", fileNamePattern, i));
                String repr = mathsat.api.msat_to_msat(msatEnv,
                        ((MathsatSymbolicFormula)f.elementAt(i)).getTerm());
                out.println(repr);
                out.close();
            } catch (FileNotFoundException e) {
              CPAMain.logManager.logException(Level.WARNING, e, "");
                System.exit(1);
            }
        }
    }
    */

    public Vector<SymbolicFormula> getUsefulBlocks(
        SymbolicFormulaManager mgr, Vector<SymbolicFormula> f,
        boolean theoryCombinationNeeded, boolean suffixTrace,
        boolean zigZag, boolean setAllTrueIfSat) {
        // try to find a minimal-unsatisfiable-core of the trace (as Blast does)
        MathsatSymbolicFormulaManager mmgr =
            (MathsatSymbolicFormulaManager)mgr;

        long msatEnv = mmgr.getMsatEnv();
        thmProver.init(TheoremProver.COUNTEREXAMPLE_ANALYSIS);

        CPAMain.logManager.log(Level.ALL, "DEBUG_1", "Calling getUsefulBlocks on path",
                       "of length: ", f.size());

        MathsatSymbolicFormula trueFormula = new MathsatSymbolicFormula(
            mathsat.api.msat_make_true(msatEnv));
        MathsatSymbolicFormula[] needed = new MathsatSymbolicFormula[f.size()];
        for (int i = 0; i < needed.length; ++i) {
            needed[i] = trueFormula;
        }
        int pos = suffixTrace ? f.size()-1 : 0;
        int incr = suffixTrace ? -1 : 1;
        int toPop = 0;

        while (true) {
            boolean consistent = true;
            // 1. assert all the needed constraints
            for (int i = 0; i < needed.length; ++i) {
                if (!needed[i].isTrue()) {
                    thmProver.push(needed[i]);
                    ++toPop;
                }
            }
            // 2. if needed is inconsistent, then return it
            if (thmProver.isUnsat(trueFormula)) {
                f = new Vector<SymbolicFormula>();
                for (int i = 0; i < needed.length; ++i) {
                    f.add(needed[i]);
                }
                break;
            }
            // 3. otherwise, assert one block at a time, until we get an
            // inconsistency
            if (zigZag) {
                int s = 0;
                int e = f.size()-1;
                boolean fromStart = false;
                while (true) {
                    int i = fromStart ? s : e;
                    if (fromStart) ++s;
                    else --e;
                    fromStart = !fromStart;

                    MathsatSymbolicFormula t =
                        (MathsatSymbolicFormula)f.elementAt(i);
                    thmProver.push(t);
                    ++toPop;
                    if (thmProver.isUnsat(trueFormula)) {
                        // add this block to the needed ones, and repeat
                        needed[i] = t;
                        CPAMain.logManager.log(Level.ALL, "DEBUG_1",
                                "Found needed block: ", i, ", term: ", t);
                        // pop all
                        while (toPop > 0) {
                            --toPop;
                            thmProver.pop();
                        }
                        // and go to the next iteration of the while loop
                        consistent = false;
                        break;
                    }

                    if (e < s) break;
                }
            } else {
                for (int i = pos; suffixTrace ? i >= 0 : i < f.size();
                     i += incr) {
                    MathsatSymbolicFormula t =
                        (MathsatSymbolicFormula)f.elementAt(i);
                    thmProver.push(t);
                    ++toPop;
                    if (thmProver.isUnsat(trueFormula)) {
                        // add this block to the needed ones, and repeat
                        needed[i] = t;
                        CPAMain.logManager.log(Level.ALL, "DEBUG_1",
                                "Found needed block: ", i, ", term: ", t);
                        // pop all
                        while (toPop > 0) {
                            --toPop;
                            thmProver.pop();
                        }
                        // and go to the next iteration of the while loop
                        consistent = false;
                        break;
                    }
                }
            }
            if (consistent) {
                // if we get here, the trace is consistent:
                // this is a real counterexample!
                if (setAllTrueIfSat) {
                    f = new Vector<SymbolicFormula>();
                    for (int i = 0; i < needed.length; ++i) {
                        f.add(trueFormula);
                    }
                }
                break;
            }
        }

        while (toPop > 0) {
            --toPop;
            thmProver.pop();
        }

        thmProver.reset();

        CPAMain.logManager.log(Level.ALL, "DEBUG_1", "Done getUsefulBlocks");

        return f;
    }

    /*
    private void printFuncNamesInTrace(
        Deque<SummaryAbstractElement> abstractTrace) {
        if (true) {
            StringBuffer buf = new StringBuffer();
            for (SummaryAbstractElement e : abstractTrace) {
                buf.append(e.getLocationNode().getFunctionName());
                buf.append("<" + ((CFANode)e.getLocation()).getNodeNumber() +
                           ">");
                buf.append(' ');
            }
            buf.deleteCharAt(buf.length()-1);
            System.out.print("ABSTRACT TRACE: ");
            System.out.println(buf.toString());
            System.out.flush();
        }
    }
    */
}
