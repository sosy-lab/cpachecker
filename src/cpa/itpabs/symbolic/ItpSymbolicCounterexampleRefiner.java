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
package cpa.itpabs.symbolic;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Deque;
import java.util.Stack;
import java.util.Vector;
import java.util.logging.Level;

import symbpredabstraction.SSAMap;
import symbpredabstraction.interfaces.SymbolicFormula;
import symbpredabstraction.interfaces.SymbolicFormulaManager;
import symbpredabstraction.mathsat.MathsatSymbolicFormula;
import symbpredabstraction.trace.ConcreteTraceNoInfo;

import cmdline.CPAMain;

import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFANode;

import cpa.itpabs.ItpAbstractElement;
import cpa.itpabs.ItpCounterexampleRefiner;
import cpa.itpabs.ItpCounterexampleTraceInfo;
import cpa.itpabs.symbolic.ItpSymbolicAbstractElement;
import common.Pair;
import cpa.symbpredabs.summary.InnerCFANode;
import cpa.symbpredabs.summary.MathsatSummaryFormulaManager;
import cpa.symbpredabs.summary.SummaryCFANode;


/**
 * Specialized ItpSymbolicCounterexampleRefiner for the symbolic version with
 * summary locations
 *
 * STILL ON-GOING, NOT FINISHED, AND CURRENTLY BROKEN
 * 
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class ItpSymbolicCounterexampleRefiner extends ItpCounterexampleRefiner {

    public ItpSymbolicCounterexampleRefiner() {
        super(null, null); // TODO
    }

    @Override
    public ItpCounterexampleTraceInfo buildCounterexampleTrace(
            SymbolicFormulaManager mgr,
            Deque<ItpAbstractElement> abstractTrace) {
        assert(abstractTrace.size() > 1);

//        mathsat.api.msat_set_verbosity(1);
        long startTime = System.currentTimeMillis();
        stats.numCallsCexAnalysis++;

        // create the DAG formula corresponding to the abstract trace. We create
        // n formulas, one per interpolation group
        SSAMap ssa = new SSAMap();
        MathsatSummaryFormulaManager mmgr = (MathsatSummaryFormulaManager)mgr;

        Vector<SymbolicFormula> f = new Vector<SymbolicFormula>();

        CPAMain.logManager.log(Level.ALL, "DEBUG_1","BUILDING COUNTEREXAMPLE TRACE");
        CPAMain.logManager.log(Level.ALL, "DEBUG_1","ABSTRACT TRACE:", abstractTrace);

        Object[] abstarr = abstractTrace.toArray();
        ItpSymbolicAbstractElement cur = (ItpSymbolicAbstractElement)abstarr[0];

        boolean theoryCombinationNeeded = false;

        MathsatSymbolicFormula bitwiseAxioms =
            (MathsatSymbolicFormula)mmgr.makeTrue();

        for (int i = 1; i < abstarr.length; ++i) {
            ItpSymbolicAbstractElement e =
                (ItpSymbolicAbstractElement)abstarr[i];
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
            boolean hasUf = mmgr.hasUninterpretedFunctions(
                    (MathsatSymbolicFormula)p.getFirst());
            theoryCombinationNeeded |= hasUf;
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
        }

        if (CPAMain.cpaConfig.getBooleanValue("cpas.symbpredabs.useBitwiseAxioms")) {
          CPAMain.logManager.log(Level.ALL, "DEBUG_3", "ADDING BITWISE AXIOMS TO THE",
              "LAST GROUP:", bitwiseAxioms);
          f.setElementAt(mmgr.makeAnd(f.elementAt(f.size()-1), bitwiseAxioms),
              f.size()-1);
        }

        CPAMain.logManager.log(Level.ALL, "DEBUG_3", 
            "Checking feasibility of abstract trace");

        // now f is the DAG formula which is satisfiable iff there is a
        // concrete counterexample
        //
        // create a working environment
        long msatEnv = mmgr.getMsatEnv();
        long env = mathsat.api.msat_create_env();
        long[] terms = new long[f.size()];
        for (int i = 0; i < terms.length; ++i) {
            terms[i] = mathsat.api.msat_make_copy_from(
                        env, ((MathsatSymbolicFormula)f.elementAt(i)).getTerm(),
                        msatEnv);
        }
        // initialize the env and enable interpolation
        mathsat.api.msat_add_theory(env, mathsat.api.MSAT_UF);
        mathsat.api.msat_add_theory(env, mathsat.api.MSAT_LRA);
        if (theoryCombinationNeeded) {
            mathsat.api.msat_set_theory_combination(env,
                    mathsat.api.MSAT_COMB_DTC);
        } else if (CPAMain.cpaConfig.getBooleanValue(
                "cpas.symbpredabs.mathsat.useIntegers")) {
            int ok = mathsat.api.msat_set_option(env, "split_eq", "true");
            assert(ok == 0);
        }
        //int ok = mathsat.api.msat_set_option(env, "toplevelprop", "2");
        //assert(ok == 0);
//        int ok = mathsat.api.msat_set_option(env, "la_itp_mode", "new");
//        assert(ok == 0);

        mathsat.api.msat_init_interpolation(env);

        // for each term, create an interpolation group
        int[] groups = new int[terms.length];
        for (int i = 0; i < groups.length; ++i) {
            groups[i] = mathsat.api.msat_create_itp_group(env);
        }
        // then, assert the formulas
        long res = mathsat.api.MSAT_UNKNOWN;

        boolean shortestTrace = CPAMain.cpaConfig.getBooleanValue(
            "cpas.symbpredabs.shortestCexTrace");

        long msatSolveTimeStart = System.currentTimeMillis();
        ++cexDumpNum;
        for (int i = 0; i < terms.length; ++i) {
            mathsat.api.msat_set_itp_group(env, groups[i]);
            mathsat.api.msat_assert_formula(env, terms[i]);

            dumpMsat(String.format("cex_%02d.%02d.msat", cexDumpNum, i),
                    env, terms[i]);

            CPAMain.logManager.log(Level.ALL, "DEBUG_2",
                           "Asserting formula: ",
                           new MathsatSymbolicFormula(terms[i]),
                           " in group: ", groups[i]);

            if (shortestTrace && mathsat.api.msat_term_is_true(terms[i]) == 0) {
                res = mathsat.api.msat_solve(env);
                assert(res != mathsat.api.MSAT_UNKNOWN);
                if (res == mathsat.api.MSAT_UNSAT) {
                    break;
                }
            }
        }
        // and check satisfiability
        if (!shortestTrace) {
            res = mathsat.api.msat_solve(env);
        }
        long msatSolveTimeEnd = System.currentTimeMillis();

        assert(res != mathsat.api.MSAT_UNKNOWN);

        ItpCounterexampleTraceInfo info = null;

        if (res == mathsat.api.MSAT_UNSAT) {
            // the counterexample is spurious. Extract the predicates from
            // the interpolants
            info = new ItpCounterexampleTraceInfo(true);
//            UpdateablePredicateMap pmap = new UpdateablePredicateMap();
//            info.setPredicateMap(pmap);
            // how to partition the trace into (A, B) depends on whether
            // there are function calls involved or not: in general, A
            // is the trace from the entry point of the current function
            // to the current point, and B is everything else. To implement
            // this, we keep track of which function we are currently in.
            Stack<Integer> entryPoints = new Stack<Integer>();
            entryPoints.push(0);
            for (int i = 1; i < groups.length; ++i) {
                int start_of_a = entryPoints.peek();
                if (!CPAMain.cpaConfig.getBooleanValue(
                       "cpas.symbpredabs.refinement.addWellScopedPredicates")) {
                    // if we don't want "well-scoped" predicates, we always
                    // cut from the beginning
                    start_of_a = 0;
                }

                int[] groups_of_a = new int[i-start_of_a];
                for (int j = 0; j < groups_of_a.length; ++j) {
                    groups_of_a[j] = groups[j+start_of_a];
                }
                long itp = mathsat.api.msat_get_interpolant(env, groups_of_a);
                assert(!mathsat.api.MSAT_ERROR_TERM(itp));

                if (CPAMain.logManager.getLogLevel().intValue() <= Level.ALL.intValue()) {
                    StringBuffer buf = new StringBuffer();
                    for (int g : groups_of_a) {
                        buf.append(g);
                        buf.append(" ");
                    }
                    CPAMain.logManager.log(Level.ALL, "DEBUG_2", "groups_of_a:", buf);
                }
                CPAMain.logManager.log(Level.ALL, "DEBUG_3",
                               "Got interpolant(", i, "):",
                               new MathsatSymbolicFormula(itp));

                long itpc = mathsat.api.msat_make_copy_from(msatEnv, itp, env);
                ItpSymbolicAbstractElement s1 =
                    (ItpSymbolicAbstractElement)abstarr[i];
                info.setFormulaForRefinement(
                        s1, mmgr.uninstantiate(
                                new MathsatSymbolicFormula(itpc), true));

                // If we are entering or exiting a function, update the stack
                // of entry points
                ItpSymbolicAbstractElement e = (ItpSymbolicAbstractElement)abstarr[i];
                if (isFunctionEntry(e)) {
                  CPAMain.logManager.log(Level.ALL, "DEBUG_3",
                            "Pushing entry point, function: ",
                            ((SummaryCFANode)e.getLocation()).getInnerNode().
                                getFunctionName());
                    entryPoints.push(i);
                }
                if (isFunctionExit(e)) {
                  CPAMain.logManager.log(Level.ALL, "DEBUG_3",
                            "Popping entry point, returning from function: ",
                            ((SummaryCFANode)e.getLocation()).getInnerNode().
                                getFunctionName());
                    entryPoints.pop();
                }
            }
            info.setFormulaForRefinement((ItpSymbolicAbstractElement)abstarr[0],
                    mmgr.makeTrue());
            info.setFormulaForRefinement(
                    (ItpSymbolicAbstractElement)abstarr[abstarr.length-1],
                    mmgr.makeFalse());
        } else {
            // this is a real bug, notify the user
            info = new ItpCounterexampleTraceInfo(false);
            info.setConcreteTrace(new ConcreteTraceNoInfo());
            // TODO - reconstruct counterexample
            // For now, we dump the asserted formula to a user-specified file
            String cexFile = CPAMain.cpaConfig.getProperty("cpas.symbpredabs.refinement.msatCexFile");
            if (cexFile != null) {
              String path = CPAMain.cpaConfig.getProperty("output.path") + cexFile;
                long t = mathsat.api.msat_make_true(env);
                for (int i = 0; i < terms.length; ++i) {
                    t = mathsat.api.msat_make_and(env, t, terms[i]);
                }
                String msatRepr = mathsat.api.msat_to_msat(env, t);
                try {
                    PrintWriter pw = new PrintWriter(new File(path));
                    pw.println(msatRepr);
                    pw.close();
                } catch (FileNotFoundException e) {
                  CPAMain.logManager.log(Level.INFO,
                            "Failed to save msat Counterexample to file: ",
                            path);
                }
            }
        }

        mathsat.api.msat_destroy_env(env);

//        mathsat.api.msat_set_verbosity(0);

        // update stats
        long endTime = System.currentTimeMillis();
        long totTime = endTime - startTime;
        stats.cexAnalysisTime += totTime;
        stats.cexAnalysisMaxTime = Math.max(totTime, stats.cexAnalysisMaxTime);
        long msatSolveTime = msatSolveTimeEnd - msatSolveTimeStart;
        stats.cexAnalysisMathsatTime += msatSolveTime;
        stats.cexAnalysisMaxMathsatTime =
            Math.max(msatSolveTime, stats.cexAnalysisMaxMathsatTime);

        return info;
    }

    private boolean isFunctionExit(ItpSymbolicAbstractElement e) {
        return false; // TODO
//        CFANode inner = e.getLocation();
//        return (inner.getNumLeavingEdges() == 1 &&
//                inner.getLeavingEdge(0) instanceof ReturnEdge);
    }

    private boolean isFunctionEntry(ItpSymbolicAbstractElement e) {
        return false; // TODO
//        CFANode inner = e.getLocation();
//        return (inner.getNumEnteringEdges() > 0 &&
//                inner.getEnteringEdge(0).getPredecessor() instanceof
//                FunctionDefinitionNode);
    }

    private Pair<SymbolicFormula, SSAMap> buildConcreteFormula(
            MathsatSummaryFormulaManager mgr,
            ItpSymbolicAbstractElement e, ItpSymbolicAbstractElement succ,
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
                                   e.getPathFormula(leaf).getFirst());
                }
            }
        }
        // now, we want to create a new formula that is the OR of all the
        // possible paths. So we merge the SSA maps and OR the formulas
        SSAMap ssa = new SSAMap();
        SymbolicFormula f = mgr.makeFalse();
        for (Pair<SymbolicFormula, SSAMap> p : relevantPaths) {
            Pair<Pair<SymbolicFormula, SymbolicFormula>, SSAMap> mp =
                mgr.mergeSSAMaps(ssa, p.getSecond(), false);
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

    @Override
    public ItpCounterexampleTraceInfo forceCover(
                SymbolicFormulaManager mgr,
                ItpAbstractElement x,
                Deque<ItpAbstractElement> path, ItpAbstractElement w) {
        assert(path.size() > 1);

        //      mathsat.api.msat_set_verbosity(1);
        long startTime = System.currentTimeMillis();
        //stats.numCallsCexAnalysis++;

        // create the DAG formula corresponding to the abstract trace. We create
        // n formulas, one per interpolation group
        SSAMap ssa = new SSAMap();
        MathsatSummaryFormulaManager mmgr = (MathsatSummaryFormulaManager)mgr;

        Vector<SymbolicFormula> f = new Vector<SymbolicFormula>();

        CPAMain.logManager.log(Level.ALL, "DEBUG_1", "CHECKING FORCED COVERAGE");
        CPAMain.logManager.log(Level.ALL, "DEBUG_1", "PATH:", path);

        Object[] abstarr = path.toArray();
        ItpSymbolicAbstractElement cur = (ItpSymbolicAbstractElement)abstarr[0];

        boolean theoryCombinationNeeded = false;

        MathsatSymbolicFormula bitwiseAxioms =
            (MathsatSymbolicFormula)mmgr.makeTrue();

        SymbolicFormula statex = mmgr.instantiate(x.getAbstraction(), null);
        f.add(statex);

        for (int i = 1; i < abstarr.length; ++i) {
            ItpSymbolicAbstractElement e =
                (ItpSymbolicAbstractElement)abstarr[i];
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
            boolean hasUf = mmgr.hasUninterpretedFunctions(
                    (MathsatSymbolicFormula)p.getFirst());
            theoryCombinationNeeded |= hasUf;
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
        }

        long msatEnv = mmgr.getMsatEnv();

        SymbolicFormula statew = mmgr.instantiate(w.getAbstraction(),  ssa);
        statew = new MathsatSymbolicFormula(mathsat.api.msat_make_not(msatEnv,
                ((MathsatSymbolicFormula)statew).getTerm()));
        f.add(statew);

        if (CPAMain.cpaConfig.getBooleanValue(
                "cpas.symbpredabs.useBitwiseAxioms")) {
          CPAMain.logManager.log(Level.ALL, "DEBUG_3", "ADDING BITWISE AXIOMS TO THE",
                    "LAST GROUP:", bitwiseAxioms);
            f.setElementAt(mmgr.makeAnd(f.elementAt(f.size()-1), bitwiseAxioms),
                    f.size()-1);
        }

        CPAMain.logManager.log(Level.ALL, "DEBUG_3",
        "Checking feasibility of abstract trace");

        // now f is the DAG formula which is satisfiable iff there is a
        // concrete counterexample
        //
        // create a working environment
        long env = mathsat.api.msat_create_env();
        long[] terms = new long[f.size()];
        for (int i = 0; i < terms.length; ++i) {
            terms[i] = mathsat.api.msat_make_copy_from(
                    env, ((MathsatSymbolicFormula)f.elementAt(i)).getTerm(),
                    msatEnv);
        }
        // initialize the env and enable interpolation
        mathsat.api.msat_add_theory(env, mathsat.api.MSAT_UF);
        mathsat.api.msat_add_theory(env, mathsat.api.MSAT_LRA);
        if (theoryCombinationNeeded) {
            mathsat.api.msat_set_theory_combination(env,
                    mathsat.api.MSAT_COMB_DTC);
        } else if (CPAMain.cpaConfig.getBooleanValue(
                "cpas.symbpredabs.mathsat.useIntegers")) {
            int ok = mathsat.api.msat_set_option(env, "split_eq", "true");
            assert(ok == 0);
        }
        int ok = mathsat.api.msat_set_option(env, "toplevelprop", "2");
        assert(ok == 0);

        mathsat.api.msat_init_interpolation(env);

        // for each term, create an interpolation group
        int[] groups = new int[terms.length];
        for (int i = 0; i < groups.length; ++i) {
            groups[i] = mathsat.api.msat_create_itp_group(env);
        }
        // then, assert the formulas
        long res = mathsat.api.MSAT_UNKNOWN;

        long msatSolveTimeStart = System.currentTimeMillis();
        for (int i = 0; i < terms.length; ++i) {
            mathsat.api.msat_set_itp_group(env, groups[i]);
            mathsat.api.msat_assert_formula(env, terms[i]);

            CPAMain.logManager.log(Level.ALL, "DEBUG_2",
                    "Asserting formula: ",
                    new MathsatSymbolicFormula(terms[i]),
                    " in group: ", groups[i]);
        }
        // and check satisfiability
        res = mathsat.api.msat_solve(env);
        long msatSolveTimeEnd = System.currentTimeMillis();

        assert(res != mathsat.api.MSAT_UNKNOWN);

        ItpCounterexampleTraceInfo info = null;

        if (res == mathsat.api.MSAT_UNSAT) {
            // the forced coverage check is successful.
            // Extract the predicates from the interpolants
            info = new ItpCounterexampleTraceInfo(true);
            for (int k = 0; k < abstarr.length; ++k) {
                int i = k+1;
                int start_of_a = 0;

                int[] groups_of_a = new int[i-start_of_a];
                for (int j = 0; j < groups_of_a.length; ++j) {
                    groups_of_a[j] = groups[j+start_of_a];
                }
                long itp = mathsat.api.msat_get_interpolant(env, groups_of_a);
                assert(!mathsat.api.MSAT_ERROR_TERM(itp));

                if (CPAMain.logManager.getLogLevel().intValue() <= Level.ALL.intValue()) {
                    StringBuffer buf = new StringBuffer();
                    for (int g : groups_of_a) {
                        buf.append(g);
                        buf.append(" ");
                    }
                    CPAMain.logManager.log(Level.ALL, "DEBUG_2", "groups_of_a:", buf);
                }
                CPAMain.logManager.log(Level.ALL, "DEBUG_2",
                        "Got interpolant(", i, "): ",
                        new MathsatSymbolicFormula(itp));

                long itpc = mathsat.api.msat_make_copy_from(msatEnv, itp, env);
                ItpAbstractElement s1 =
                    (ItpAbstractElement)abstarr[k];
                info.setFormulaForRefinement(
                        s1, mmgr.uninstantiate(
                                new MathsatSymbolicFormula(itpc)));
            }
        } else {
            // this is a real bug, notify the user
            info = new ItpCounterexampleTraceInfo(false);
        }

        mathsat.api.msat_destroy_env(env);

        //      mathsat.api.msat_set_verbosity(0);

        // update stats
        long endTime = System.currentTimeMillis();
        long totTime = endTime - startTime;
        stats.forceCoverTime += totTime;
        stats.forceCoverMaxTime = Math.max(totTime, stats.forceCoverMaxTime);
        long msatSolveTime = msatSolveTimeEnd - msatSolveTimeStart;
        stats.forceCoverMathsatTime += msatSolveTime;
        stats.forceCoverMaxMathsatTime =
            Math.max(msatSolveTime, stats.forceCoverMaxMathsatTime);

        return info;
    }
}
