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
package cpa.itpabs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;

import symbpredabstraction.SSAMap;
import symbpredabstraction.interfaces.InterpolatingTheoremProver;
import symbpredabstraction.interfaces.SymbolicFormula;
import symbpredabstraction.interfaces.SymbolicFormulaManager;
import symbpredabstraction.mathsat.MathsatSymbolicFormula;
import symbpredabstraction.mathsat.MathsatSymbolicFormulaManager;
import symbpredabstraction.trace.ConcreteTraceNoInfo;

import logging.CustomLogLevel;
import logging.LazyLogger;

import cmdline.CPAMain;

import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.itpabs.ItpAbstractElement;
import cpa.itpabs.ItpCounterexampleTraceInfo;
import exceptions.UnrecognizedCFAEdgeException;
import cpa.symbpredabs.explicit.ExplicitAbstractFormulaManager;

/**
 * STILL ON-GOING, NOT FINISHED, AND CURRENTLY BROKEN
 * 
 * An ItpCounterexampleRefiner is an object that is used to perform refinement
 * in interpolation-based lazy abstraction. It is also used for "forced
 * coverings" (see the CAV'06 paper by McMillan on "Lazy Abstraction with
 * Interpolants" for details)
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class ItpCounterexampleRefiner {

    // some statistics. All times are in milliseconds
    public class Stats {
        public long abstractionMathsatTime = 0;
        public long abstractionMaxMathsatTime = 0;
        public long abstractionBddTime = 0;
        public long abstractionMaxBddTime = 0;
        public int numCallsAbstraction = 0;
        public long cexAnalysisTime = 0;
        public long cexAnalysisMaxTime = 0;
        public int numCallsCexAnalysis = 0;
        public long abstractionMathsatSolveTime = 0;
        public long abstractionMaxMathsatSolveTime = 0;
        public long cexAnalysisMathsatTime = 0;
        public long cexAnalysisMaxMathsatTime = 0;
        public int abstractionNumMathsatQueries = 0;

        public long forceCoverTime = 0;
        public long forceCoverMaxTime = 0;
        public long forceCoverMathsatTime = 0;
        public long forceCoverMaxMathsatTime = 0;
    }
    protected Stats stats;

    protected int cexDumpNum = 0;
    protected boolean dumpCexQueries;

    private Map<SymbolicFormula, MathsatSymbolicFormula> instantiateCache;

    private ExplicitAbstractFormulaManager amgr;
    private InterpolatingTheoremProver itpProver;

    public ItpCounterexampleRefiner(ExplicitAbstractFormulaManager mgr,
            InterpolatingTheoremProver interpolator) {
        super();
        stats = new Stats();
        dumpCexQueries = CPAMain.cpaConfig.getBooleanValue(
                "cpas.itpabs.mathsat.dumpRefinementQueries");
        instantiateCache =
            new HashMap<SymbolicFormula, MathsatSymbolicFormula>();

        amgr = mgr;
//        assert(amgr != null);
        itpProver = interpolator;
    }

    public Stats getStats() { return stats; }

    /**
     * counterexample analysis and abstraction refinement, using interpolants
     * directly
     */
    public ItpCounterexampleTraceInfo buildCounterexampleTrace(
            SymbolicFormulaManager mgr,
            Deque<ItpAbstractElement> abstractTrace) {
        assert(abstractTrace.size() > 1);

        long startTime = System.currentTimeMillis();
        stats.numCallsCexAnalysis++;

        // create the DAG formula corresponding to the abstract trace. We create
        // n formulas, one per interpolation group
        AbstractElementWithLocation[] abstarr = abstractTrace.toArray(
                new AbstractElementWithLocation[0]);
        ExplicitAbstractFormulaManager.ConcretePath concPath = null;
        try {
            concPath = amgr.buildConcretePath(mgr, abstarr);
        } catch (UnrecognizedCFAEdgeException e1) {
            e1.printStackTrace();
            System.out.println("BAD ERROR TRACE: " + abstractTrace);
            System.out.flush();
            System.exit(1);
        }

        Vector<SymbolicFormula> f = concPath.path;
        boolean theoryCombinationNeeded = concPath.theoryCombinationNeeded;

        LazyLogger.log(LazyLogger.DEBUG_3,
                       "Checking feasibility of abstract trace");

        boolean shortestTrace = CPAMain.cpaConfig.getBooleanValue(
                "cpas.symbpredabs.shortestCexTrace");

        if (shortestTrace && CPAMain.cpaConfig.getBooleanValue(
                "cpas.symbpredabs.explicit.getUsefulBlocks")) {
//            long gubStart = System.currentTimeMillis();
            f = amgr.getUsefulBlocks(
                    mgr, f, theoryCombinationNeeded, false, false, false);
//            long gubEnd = System.currentTimeMillis();
//            stats.cexAnalysisGetUsefulBlocksTime += gubEnd - gubStart;
//            stats.cexAnalysisGetUsefulBlocksMaxTime = Math.max(
//                    stats.cexAnalysisGetUsefulBlocksMaxTime, gubEnd - gubStart);
            // set shortestTrace to false, so we perform only one final call
            // to msat_solve
            shortestTrace = false;
        }

        // now f is the DAG formula which is satisfiable iff there is a
        // concrete counterexample
        //
        // create a working environment
        MathsatSymbolicFormulaManager mmgr = (MathsatSymbolicFormulaManager)mgr;
        long msatEnv = mmgr.getMsatEnv();

        itpProver.init();

        // then, assert the formulas
        long res = -1;

        ++cexDumpNum;
        long msatSolveTimeStart = System.currentTimeMillis();
        int n = 0;
        for (SymbolicFormula fm : f) {
            itpProver.addFormula(fm);

            dumpMsat(String.format("cex_%02d.%02d.msat", cexDumpNum, n++),
                     msatEnv, ((MathsatSymbolicFormula)fm).getTerm());

            LazyLogger.log(LazyLogger.DEBUG_1,
                           "Asserting formula: ", fm);

            if (shortestTrace && !fm.isTrue()) {
                if (itpProver.isUnsat()) {
                    res = 0;
                    break;
                } else {
                    res = 1;
                }
            } else {
                res = -1;
            }
        }
        // and check satisfiability
        boolean unsat = false;
        if (!shortestTrace || res == -1) {
            unsat = itpProver.isUnsat();
        } else {
            unsat = (res == 0);
        }
        long msatSolveTimeEnd = System.currentTimeMillis();
        long msatSolveTime = msatSolveTimeEnd - msatSolveTimeStart;

        ItpCounterexampleTraceInfo info = null;

        if (unsat) {
            // the counterexample is spurious. Extract the predicates from
            // the interpolants
            info = new ItpCounterexampleTraceInfo(true);
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
                msatSolveTimeStart = System.currentTimeMillis();
                SymbolicFormula itp = itpProver.getInterpolant(formulasOfA);
                msatSolveTimeEnd = System.currentTimeMillis();
                msatSolveTime += msatSolveTimeEnd - msatSolveTimeStart;

                LazyLogger.log(LazyLogger.DEBUG_1,
                               "Got interpolant(", i, "): ", itp);

                ItpAbstractElement s1 =
                    (ItpAbstractElement)abstarr[i];
                info.setFormulaForRefinement(
                        s1, mmgr.uninstantiate((MathsatSymbolicFormula)itp));

                // If we are entering or exiting a function, update the stack
                // of entry points
                ItpAbstractElement e = (ItpAbstractElement)abstarr[i];
                if (isFunctionEntry(e)) {
                    LazyLogger.log(LazyLogger.DEBUG_3,
                            "Pushing entry point, function: ",
                            e.getLocation().getFunctionName());
                    entryPoints.push(i);
                }
                if (isFunctionExit(e)) {
                    LazyLogger.log(LazyLogger.DEBUG_3,
                            "Popping entry point, returning from function: ",
                            e.getLocation().getFunctionName());
                    entryPoints.pop();
                }
            }
            info.setFormulaForRefinement(abstarr[0],
                    mmgr.makeTrue());
            info.setFormulaForRefinement(abstarr[abstarr.length-1],
                    mmgr.makeFalse());
        } else {
            // this is a real bug, notify the user
            info = new ItpCounterexampleTraceInfo(false);
            info.setConcreteTrace(new ConcreteTraceNoInfo());
            // TODO - reconstruct counterexample
            // For now, we dump the asserted formula to a user-specified file
            String cexPath = CPAMain.cpaConfig.getProperty(
                    "cpas.symbpredabs.refinement.msatCexPath");
            if (cexPath != null) {
                dumpCounterexample(mmgr, f, cexPath);
            }
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

    private void dumpCounterexample(MathsatSymbolicFormulaManager mmgr,
            Vector<SymbolicFormula> f, String cexPath) {
        long msatEnv = mmgr.getMsatEnv();
        try {
            PrintWriter pw = new PrintWriter(new File(cexPath));
            int n = 0;
            for (SymbolicFormula fm : f) {
                long t = ((MathsatSymbolicFormula)fm).getTerm();
                String s = mathsat.api.msat_to_msat(msatEnv, t);
                pw.println("#--- TERM: " + (n++));
                pw.println(s);
            }
            pw.close();
        } catch (FileNotFoundException e) {
            LazyLogger.log(CustomLogLevel.INFO,
                    "Failed to save msat Counterexample to file: ",
                    cexPath);
        }
    }

    private boolean isFunctionExit(ItpAbstractElement e) {
        return false; // TODO
//        CFANode inner = e.getLocation();
//        return (inner.getNumLeavingEdges() == 1 &&
//                inner.getLeavingEdge(0) instanceof ReturnEdge);
    }

    private boolean isFunctionEntry(ItpAbstractElement e) {
        return false; // TODO
//        CFANode inner = e.getLocation();
//        return (inner.getNumEnteringEdges() > 0 &&
//                inner.getEnteringEdge(0).getPredecessor() instanceof
//                FunctionDefinitionNode);
    }

    /**
     * Forced Covering checks (see McMillan's CAV'06 paper)
     */
    public ItpCounterexampleTraceInfo forceCover(
            SymbolicFormulaManager mgr,
            ItpAbstractElement x,
            Deque<ItpAbstractElement> path, ItpAbstractElement w) {
        assert(path.size() > 1);

        long startTime = System.currentTimeMillis();
        //stats.numCallsCexAnalysis++;

        // create the DAG formula corresponding to the abstract trace. We create
        // n formulas, one per interpolation group
        // create the DAG formula corresponding to the abstract trace. We create
        // n formulas, one per interpolation group
        AbstractElementWithLocation[] abstarr = path.toArray(
                new AbstractElementWithLocation[0]);
        ExplicitAbstractFormulaManager.ConcretePath concPath = null;
        try {
            concPath = amgr.buildConcretePath(mgr, abstarr);
        } catch (UnrecognizedCFAEdgeException e1) {
            e1.printStackTrace();
            System.out.println("BAD FORCED COVERAGE PATH: " + path);
            System.out.flush();
            System.exit(1);
        }

        Vector<SymbolicFormula> f = concPath.path;
        SSAMap ssa = concPath.ssa;
        boolean theoryCombinationNeeded = concPath.theoryCombinationNeeded;

        MathsatSymbolicFormulaManager mmgr = (MathsatSymbolicFormulaManager)mgr;
        SymbolicFormula statex = instantiate(mmgr, x.getAbstraction());
        f.add(0, statex);

        long msatEnv = mmgr.getMsatEnv();

        SymbolicFormula statew = mmgr.instantiate(w.getAbstraction(), ssa);
        SymbolicFormula statew2 = statew;
        statew = new MathsatSymbolicFormula(mathsat.api.msat_make_not(msatEnv,
                ((MathsatSymbolicFormula)statew).getTerm()));
        if (statew.isFalse()) {
            System.out.println("ERROR, statew is false!! statew2 is: " +
                    statew2 + ", w.getAbstraction() is: " + w.getAbstraction() +
                    ", ssa is: " + ssa);
            System.out.flush();
            assert(false);
        }
        f.add(statew);

        LazyLogger.log(LazyLogger.DEBUG_3,
                "Checking feasibility of abstract trace");

        boolean getUsefulBlocks = CPAMain.cpaConfig.getBooleanValue(
                "cpas.symbpredabs.explicit.getUsefulBlocks");
        if (getUsefulBlocks) {
            //long gubStart = System.currentTimeMillis();
            f = amgr.getUsefulBlocks(
                    mgr, f, theoryCombinationNeeded, false, false, true);
            //long gubEnd = System.currentTimeMillis();
            //          stats.cexAnalysisGetUsefulBlocksTime += gubEnd - gubStart;
            //          stats.cexAnalysisGetUsefulBlocksMaxTime = Math.max(
            //                  stats.cexAnalysisGetUsefulBlocksMaxTime, gubEnd - gubStart);
        }


        ItpCounterexampleTraceInfo info = null;
        long msatSolveTime = 0;
        if (getUsefulBlocks && f.elementAt(f.size()-1).isTrue()) {
            // we can be sure that there can't be a forced covering in this case
            info = new ItpCounterexampleTraceInfo(false);
        } else {
            // create a working environment
            itpProver.init();

            // then, assert the formulas
            long msatSolveTimeStart = System.currentTimeMillis();
            for (int i = 0; i < f.size()-1; ++i) {
                SymbolicFormula fm = f.elementAt(i);
                itpProver.addFormula(fm);

                LazyLogger.log(LazyLogger.DEBUG_1,
                        "Asserting formula: ", fm);
            }
            // and check satisfiability
            boolean unsat = itpProver.isUnsat();
            if (!unsat) {
                // ok, the path is feasible, add the negation
                // of the abstract state at w
                itpProver.addFormula(f.lastElement());

                LazyLogger.log(LazyLogger.DEBUG_1,
                        "Asserting state formula: ", f.lastElement());
                unsat = itpProver.isUnsat();
            } else {
                unsat = false;
            }
            long msatSolveTimeEnd = System.currentTimeMillis();

            if (unsat) {
                LazyLogger.log(LazyLogger.DEBUG_1, "Forced coverage OK");
                // the forced coverage check is successful.
                // Extract the predicates from the interpolants
                info = new ItpCounterexampleTraceInfo(true);
                for (int k = 0; k < abstarr.length; ++k) {
                    int i = k+1;
                    int start_of_a = 0;

                    int sz = i - start_of_a;
                    Vector<SymbolicFormula> formulasOfA =
                        new Vector<SymbolicFormula>();
                    formulasOfA.ensureCapacity(sz);
                    for (int j = 0; j < sz; ++j) {
                        formulasOfA.add(f.elementAt(j+start_of_a));
                    }
                    SymbolicFormula itp =
                        itpProver.getInterpolant(formulasOfA);

                    LazyLogger.log(LazyLogger.DEBUG_1,
                            "Got interpolant(", i, "): ", itp);

                    ItpAbstractElement s1 =
                        (ItpAbstractElement)abstarr[k];
                    info.setFormulaForRefinement(
                            s1, mmgr.uninstantiate(
                                    (MathsatSymbolicFormula)itp));
                }
            } else {
                // forced coverage not possible
                info = new ItpCounterexampleTraceInfo(false);
            }

            itpProver.reset();
            msatSolveTime = msatSolveTimeEnd - msatSolveTimeStart;
        }

        // update stats
        long endTime = System.currentTimeMillis();
        long totTime = endTime - startTime;
        stats.forceCoverTime += totTime;
        stats.forceCoverMaxTime = Math.max(totTime, stats.forceCoverMaxTime);
        stats.forceCoverMathsatTime += msatSolveTime;
        stats.forceCoverMaxMathsatTime =
            Math.max(msatSolveTime, stats.forceCoverMaxMathsatTime);

        return info;
    }

    // used to dump interpolation queries to file for debugging
    protected void dumpMsat(String filename, long env, long term) {
        if (dumpCexQueries) {
            try {
                PrintWriter out = new PrintWriter(new File(filename));
                String repr = mathsat.api.msat_to_msat(env, term);
                out.println(repr);
                out.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                assert(false);
            }
        }
    }

    private MathsatSymbolicFormula instantiate(
            MathsatSymbolicFormulaManager mmgr, SymbolicFormula f) {
        if (instantiateCache.containsKey(f)) {
            return instantiateCache.get(f);
        }
        MathsatSymbolicFormula ret =
            (MathsatSymbolicFormula)mmgr.instantiate(f, null);
        instantiateCache.put(f, ret);
        return ret;
    }

}
