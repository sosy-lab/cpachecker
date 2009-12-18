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
package cpa.symbpredabsCPA;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;
import java.util.logging.Level;

import symbpredabstraction.PathFormula;
import symbpredabstraction.SSAMap;
import symbpredabstraction.Cache.CartesianAbstractionCacheKey;
import symbpredabstraction.Cache.FeasibilityCacheKey;
import symbpredabstraction.Cache.TimeStampCache;
import symbpredabstraction.interfaces.AbstractFormula;
import symbpredabstraction.interfaces.InterpolatingTheoremProver;
import symbpredabstraction.interfaces.Predicate;
import symbpredabstraction.interfaces.SymbolicFormula;
import symbpredabstraction.interfaces.SymbolicFormulaManager;
import symbpredabstraction.interfaces.TheoremProver;
import symbpredabstraction.mathsat.BDDMathsatAbstractFormulaManager;
import symbpredabstraction.mathsat.MathsatAbstractionPrinter;
import symbpredabstraction.mathsat.MathsatSymbolicFormula;
import symbpredabstraction.mathsat.MathsatSymbolicFormulaManager;
import symbpredabstraction.trace.ConcreteTraceFunctionCalls;
import symbpredabstraction.trace.CounterexampleTraceInfo;
import cfa.objectmodel.CFAFunctionDefinitionNode;
import cmdline.CPAMain;

import common.Pair;


/**
 * Implementation of SummaryAbstractFormulaManager that works with BDDs for
 * abstraction and MathSAT terms for concrete formulas
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */

class BDDMathsatSymbPredAbstractionAbstractManager extends BDDMathsatAbstractFormulaManager 
implements SymbPredAbstFormulaManager
{

  private class AllSatCallbackStats extends AllSatCallback
  implements TheoremProver.AllSatCallback {
    public long totTime = 0;
    private long[] curModel;

    public AllSatCallbackStats(long msatEnv, long absEnv) {
      super(msatEnv, absEnv);
      curModel = null;
    }

    @Override
    public void callback(long[] model) {
      long start = System.currentTimeMillis();
      super.callback(model);
      long end = System.currentTimeMillis();
      totTime += (end - start);
    }

    @Override
    public void modelFound(Vector<SymbolicFormula> model) {
      if (curModel == null || curModel.length != model.size()) {
        curModel = new long[model.size()];
      }
      for (int i = 0; i < curModel.length; ++i) {
        long t = ((MathsatSymbolicFormula)model.elementAt(i)).getTerm();
        curModel[i] = t;
      }
      callback(curModel);
    }
  }

  static class Stats {
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
    public long replacing = 0;
  }
  final Stats stats;

  private final TheoremProver thmProver;
  private final InterpolatingTheoremProver itpProver;

  private static final int MAX_CACHE_SIZE = 100000;

  private final boolean useCache;
  private final Map<Pair<SymbolicFormula, Vector<SymbolicFormula>>, AbstractFormula> abstractionCache;
  //cache for cartesian abstraction queries. For each predicate, the values
  // are -1: predicate is false, 0: predicate is don't care,
  // 1: predicate is true
  private final TimeStampCache<CartesianAbstractionCacheKey, Byte> cartesianAbstractionCache;
  private final TimeStampCache<FeasibilityCacheKey, Boolean> feasibilityCache;

  private final boolean dumpHardAbstractions;

  public BDDMathsatSymbPredAbstractionAbstractManager(TheoremProver prover,
      InterpolatingTheoremProver interpolator) {
    super();
    stats = new Stats();
    thmProver = prover;
    itpProver = interpolator;

    dumpHardAbstractions = CPAMain.cpaConfig.getBooleanValue("cpas.symbpredabs.mathsat.dumpHardAbstractionQueries");
    useCache = CPAMain.cpaConfig.getBooleanValue("cpas.symbpredabs.mathsat.useCache");

    if (useCache) {
      abstractionCache = new HashMap<Pair<SymbolicFormula, Vector<SymbolicFormula>>, AbstractFormula>();
      cartesianAbstractionCache = new TimeStampCache<CartesianAbstractionCacheKey, Byte>(MAX_CACHE_SIZE);
      feasibilityCache = new TimeStampCache<FeasibilityCacheKey, Boolean>(MAX_CACHE_SIZE);
    } else {
      abstractionCache = null;
      cartesianAbstractionCache = null;
      feasibilityCache = null;
    }
  }

  @Override
  public AbstractFormula buildAbstraction(SymbolicFormulaManager mgr,
      AbstractFormula abs, PathFormula pathFormula,
      Collection<Predicate> predicates/*, SymbolicFormula functionExitFormula*/) {
    stats.numCallsAbstraction++;
    if (CPAMain.cpaConfig.getBooleanValue(
    "cpas.symbpredabs.abstraction.cartesian")) {
      return buildCartesianAbstraction(mgr, abs, pathFormula, predicates/*, functionExitFormula*/);
    } else {
      return buildBooleanAbstraction(mgr, abs, pathFormula, predicates/*, functionExitFormula*/);
    }
  }

  private AbstractFormula buildCartesianAbstraction(
      SymbolicFormulaManager mgr,
      AbstractFormula abs,
      PathFormula pathFormula,
      Collection<Predicate> predicates/*,
      SymbolicFormula functionExitFormula*/) {
    
    long startTime = System.currentTimeMillis();

    MathsatSymbolicFormulaManager mmgr = (MathsatSymbolicFormulaManager)mgr;

    long msatEnv = mmgr.getMsatEnv();

    thmProver.init(TheoremProver.CARTESIAN_ABSTRACTION);

//    Pair<SymbolicFormula, SSAMap> pc =
//        buildConcreteFormula(mmgr, e, succ, edge, false);
//    SymbolicFormula f = pc.getFirst();
//    SSAMap ssa = pc.getSecond();
    SymbolicFormula f = pathFormula.getSymbolicFormula();
    SSAMap ssa = pathFormula.getSsa();

    f = mmgr.replaceAssignments((MathsatSymbolicFormula)f);
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
                return makeFalse();
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
            return makeFalse();
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

    AbstractFormula absbdd = makeTrue();

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
                v = makeNot(v);
                absbdd = makeAnd(absbdd, v);
            } else if (predVals[predIndex] == 1) { // pred is true
                absbdd = makeAnd(absbdd, v);
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
            collectVarNames(mmgr, pi.getSecond().getTerm(),
                    predvars, predlvals);
            for (String var : predvars) {
                if (ssa.getIndex(var) < 0) {
                    ssa.setIndex(var, 1);
                }
            }
            for (Pair<String, SymbolicFormula[]> pp : predlvals) {
                SymbolicFormula[] args =
                    getInstantiatedAt(mmgr, pp.getSecond(), ssa,
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
//            predTrue = mathsat.api.msat_make_copy_from(
//                    absEnv, inst.getTerm(), msatEnv);
            long predFalse = mathsat.api.msat_make_not(msatEnv, predTrue);

            //++stats.abstractionNumMathsatQueries;
            if (thmProver.isUnsat(
                    new MathsatSymbolicFormula(predFalse))) {
                isTrue = true;
            }

            if (isTrue) {
                long startBddTime = System.currentTimeMillis();
                AbstractFormula v = p.getFormula();
                absbdd = makeAnd(absbdd, v);
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
                    v = makeNot(v);
                    absbdd = makeAnd(absbdd, v);
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

  private AbstractFormula buildBooleanAbstraction(SymbolicFormulaManager mgr,
      AbstractFormula abstractionFormula, PathFormula pathFormula,
      Collection<Predicate> predicates/*, SymbolicFormula functionExitFormula*/) {
    // A SummaryFormulaManager for MathSAT formulas
    MathsatSymbolicFormulaManager mmgr = (MathsatSymbolicFormulaManager)mgr;

    long startTime = System.currentTimeMillis();

    // build the concrete representation of the abstract formula of e
    // this is an abstract formula - specifically it is a bddabstractformula
    // which is basically an integer which represents it
    // create the concrete form of the abstract formula
    // (abstract formula is the bdd representation)
    final SymbolicFormula absFormula = mgr.instantiate(toConcrete(mgr, abstractionFormula), null);

    // create an ssamap from concrete formula
    SSAMap absSsa = mmgr.extractSSA((MathsatSymbolicFormula)absFormula);

    // shift pathFormula indices by the offsets in absSsa
    long start = System.currentTimeMillis();

    pathFormula = mgr.shift(pathFormula.getSymbolicFormula(), absSsa);
    SymbolicFormula symbFormula = mmgr.replaceAssignments((MathsatSymbolicFormula)pathFormula.getSymbolicFormula());
    final SSAMap symbSsa = pathFormula.getSsa();

    long end = System.currentTimeMillis();
    stats.replacing += (end - start);
    
    // from now on, abstractionFormula, pathFormula and functionExitFormula should not be used,
    // only absFormula, absSsa, symbFormula, symbSsa
    
    if (CPAMain.cpaConfig.getBooleanValue("cpas.symbpredabs.useBitwiseAxioms")) {
      MathsatSymbolicFormula bitwiseAxioms = mmgr.getBitwiseAxioms(
          (MathsatSymbolicFormula)symbFormula);
      symbFormula = mgr.makeAnd(symbFormula, bitwiseAxioms);

      CPAMain.logManager.log(Level.ALL, "DEBUG_3", "ADDED BITWISE AXIOMS:",
          bitwiseAxioms);
    }


    // first, create the new formula corresponding to
    // (symbFormula & edges from e to succ)
    // TODO - at the moment, we assume that all the edges connecting e and
    // succ have no statement or assertion attached (i.e. they are just
    // return edges or gotos). This might need to change in the future!!
    // (So, for now we don't need to to anything...)


    // build the definition of the predicates, and instantiate them
    PredInfo predinfo = buildPredList(mmgr, predicates);
    {
      Collection<String> predvars = predinfo.allVars;
      Collection<Pair<String, SymbolicFormula[]>> predlvals =
        predinfo.allFuncs;
      // update the SSA map, by instantiating all the uninstantiated
      // variables that occur in the predicates definitions (at index 1)
      for (String var : predvars) {
        if (symbSsa.getIndex(var) < 0) {
          symbSsa.setIndex(var, 1);
        }
      }
      Map<SymbolicFormula, SymbolicFormula> cache =
        new HashMap<SymbolicFormula, SymbolicFormula>();
      for (Pair<String, SymbolicFormula[]> p : predlvals) {
        SymbolicFormula[] args =
          getInstantiatedAt(mmgr, p.getSecond(), symbSsa, cache);
        if (symbSsa.getIndex(p.getFirst(), args) < 0) {
          symbSsa.setIndex(p.getFirst(), args, 1);
        }
      }
    }
  
    Vector<SymbolicFormula> importantPreds = new Vector<SymbolicFormula>();
    {
      final long[] important = predinfo.important;
      importantPreds.ensureCapacity(important.length);
      for (long p : important) {
        importantPreds.add(new MathsatSymbolicFormula(p));
      }
    }
    
    if (CPAMain.logManager.getLogLevel().intValue() <= Level.ALL.intValue()) {
      StringBuffer importantStrBuf = new StringBuffer();
      for (SymbolicFormula impFormula : importantPreds) {
        importantStrBuf.append(impFormula.toString());
        importantStrBuf.append(" ");
      }
      CPAMain.logManager.log(Level.ALL, "DEBUG_1",
          "IMPORTANT SYMBOLS (", importantPreds.size(), "): ",
          importantStrBuf);
    }

    
    // instantiate the definitions with the right SSA
    SymbolicFormula predDef = mgr.instantiate(
        new MathsatSymbolicFormula(predinfo.predDef), symbSsa);

    // the formula is (absFormula & symbFormula & predDef)
    final SymbolicFormula fm = mgr.makeAnd( 
        mgr.makeAnd(absFormula, symbFormula), predDef);
    
    CPAMain.logManager.log(Level.ALL, "DEBUG_2",
        "COMPUTING ALL-SMT ON FORMULA: ", fm);

    Pair<SymbolicFormula, Vector<SymbolicFormula>> absKey =
      new Pair<SymbolicFormula, Vector<SymbolicFormula>>(fm, importantPreds);
    AbstractFormula result;
    if (useCache && abstractionCache.containsKey(absKey)) {
      ++stats.numCallsAbstractionCached;
      result = abstractionCache.get(absKey);
    } else {
      // get the environment from the manager - this is unique, it is the
      // environment in which all terms are created
      final long msatEnv = mmgr.getMsatEnv();
   
      AllSatCallbackStats allSatCallback = new AllSatCallbackStats(msatEnv, 0);
      long msatSolveStartTime = System.currentTimeMillis();
      final int numModels = thmProver.allSat(fm, importantPreds, allSatCallback);
      long msatSolveEndTime = System.currentTimeMillis();

      assert(numModels != -1);  // msat_all_sat returns -1 on error

      if (numModels == -2) {
        // formula has infinite number of models
        result = makeTrue();
      } else {
        result = allSatCallback.getBDD();
      }

      if (useCache) {
        abstractionCache.put(absKey, result);
      }
      
      // update statistics
      long bddTime       = allSatCallback.totTime;
      long msatSolveTime = (msatSolveEndTime - msatSolveStartTime) - bddTime;

      stats.abstractionMathsatSolveTime += msatSolveTime;
      stats.abstractionBddTime          += bddTime;
      startTime += bddTime; // do not count BDD creation time

      stats.abstractionMaxBddTime =
        Math.max(bddTime, stats.abstractionMaxBddTime);
      stats.abstractionMaxMathsatSolveTime =
        Math.max(msatSolveTime, stats.abstractionMaxMathsatSolveTime);

      // TODO dump hard abst
      if (msatSolveTime > 10000 && dumpHardAbstractions) {
        // we want to dump "hard" problems...
        MathsatAbstractionPrinter absPrinter = new MathsatAbstractionPrinter(msatEnv, "abs");
        absPrinter.printMsatFormat(absFormula, symbFormula, predDef, importantPreds);
        absPrinter.printNusmvFormat(absFormula, symbFormula, predDef, importantPreds);
        absPrinter.nextNum();
      }
    }

    // update statistics
    long endTime = System.currentTimeMillis();
    long abstractionMsatTime = (endTime - startTime);
    stats.abstractionMathsatTime += abstractionMsatTime;
    stats.abstractionMaxMathsatTime =
      Math.max(abstractionMsatTime, stats.abstractionMaxMathsatTime);
    
    return result;
  }

  @Override
  public CounterexampleTraceInfo buildCounterexampleTrace(SymbolicFormulaManager mgr,
      ArrayList<SymbPredAbsAbstractElement> abstractTrace) {
    assert(abstractTrace.size() > 1);
    MathsatSymbolicFormulaManager mmgr = (MathsatSymbolicFormulaManager)mgr;

    long startTime = System.currentTimeMillis();
    stats.numCallsCexAnalysis++;

    CPAMain.logManager.log(Level.FINEST, "Building counterexample trace");

    Vector<SymbolicFormula> f = getFormulasForTrace(mgr, abstractTrace);

    CPAMain.logManager.log(Level.ALL, "Counterexample trace formulas:", f);
    
    boolean theoryCombinationNeeded = false;
    boolean useDtc = CPAMain.cpaConfig.getBooleanValue("cpas.symbpredabs.mathsat.useDtc");

    if (useDtc) {
      theoryCombinationNeeded = addBitwiseAxioms(mgr, f);
    }
    
    CPAMain.logManager.log(Level.FINEST, "Checking feasibility of counterexample trace");

    // now f is the DAG formula which is satisfiable iff there is a
    // concrete counterexample

    // create a working environment
    itpProver.init();

    boolean shortestTrace = CPAMain.cpaConfig.getBooleanValue("cpas.symbpredabs.shortestCexTrace");
    boolean useSuffix = CPAMain.cpaConfig.getBooleanValue("cpas.symbpredabs.shortestCexTraceUseSuffix");
    boolean useZigZag = CPAMain.cpaConfig.getBooleanValue("cpas.symbpredabs.shortestCexTraceZigZag");

    long msatSolveTimeStart = System.currentTimeMillis();

    if (shortestTrace && CPAMain.cpaConfig.getBooleanValue("cpas.symbpredabs.explicit.getUsefulBlocks")) {
      f = getUsefulBlocks(mgr, f, theoryCombinationNeeded, useSuffix, useZigZag);
     
      // set shortestTrace to false, so we perform only one final call
      // to msat_solve
      shortestTrace = false;
    }

    boolean spurious = checkInfeasabilityOfTrace(f, shortestTrace, useSuffix, useZigZag);

    long msatSolveTimeEnd = System.currentTimeMillis();
    long msatSolveTime = msatSolveTimeEnd - msatSolveTimeStart;

    CounterexampleTraceInfo info = new CounterexampleTraceInfo(spurious);

    CPAMain.logManager.log(Level.FINEST, "Counterexample trace is", (spurious ? "infeasible" : "feasible"));
    
    if (spurious) {
      // the counterexample is spurious. Extract the predicates from
      // the interpolants
      boolean splitItpAtoms = CPAMain.cpaConfig.getBooleanValue("cpas.symbpredabs.refinement.splitItpAtoms");
      
      // how to partition the trace into (A, B) depends on whether
      // there are function calls involved or not: in general, A
      // is the trace from the entry point of the current function
      // to the current point, and B is everything else. To implement
      // this, we keep track of which function we are currently in.
      Stack<Integer> entryPoints = new Stack<Integer>();
      entryPoints.push(0);
      
      for (int i = 0; i < f.size(); ++i) {
        int start_of_a;
        if (CPAMain.cpaConfig.getBooleanValue("cpas.symbpredabs.refinement.addWellScopedPredicates")) {
          start_of_a = entryPoints.peek();
        } else {
          // if we don't want "well-scoped" predicates, we always
          // cut from the beginning
          start_of_a = 0;          
        }
        
        Vector<SymbolicFormula> formulasOfA = new Vector<SymbolicFormula>(i - start_of_a);
        for (int j = start_of_a; j < i; ++j) {
          formulasOfA.add(f.elementAt(j));
        }
        
        CPAMain.logManager.log(Level.ALL, "Looking for interpolant for formulas from",
            start_of_a, "to", i-1, ":", formulasOfA);
        
        msatSolveTimeStart = System.currentTimeMillis();
        SymbolicFormula itp = itpProver.getInterpolant(formulasOfA);
        msatSolveTimeEnd = System.currentTimeMillis();
        msatSolveTime += msatSolveTimeEnd - msatSolveTimeStart;

        Collection<SymbolicFormula> atoms = mmgr.extractAtoms(
            itp, true, splitItpAtoms, false);
        Set<Predicate> preds = buildPredicates(mgr, atoms);
        SymbPredAbsAbstractElement e = abstractTrace.get(i);
        info.addPredicatesForRefinement(e, preds);

        CPAMain.logManager.log(Level.ALL, "For element (", e, ") got:\n",
            "interpolant", itp,
            "atoms ", atoms,
            "predicates", preds);

        // If we are entering or exiting a function, update the stack
        // of entry points
        // TODO checking if the abstraction node is a new function
        if (e.getAbstractionLocation() instanceof CFAFunctionDefinitionNode) {
          entryPoints.push(i);
        }
        // TODO check we are returning from a function
        if (e.getAbstractionLocation().getEnteringSummaryEdge() != null) {
          entryPoints.pop();
        }
      }
    } else {
      // this is a real bug, notify the user
      ConcreteTraceFunctionCalls cf = new ConcreteTraceFunctionCalls();
      for (SymbPredAbsAbstractElement e : abstractTrace) {
        cf.add(e.getAbstractionLocation().getFunctionName());
      }
      info.setConcreteTrace(cf);
      
      // TODO - reconstruct counterexample
      // For now, we dump the asserted formula to a user-specified file
      String cexFile = CPAMain.cpaConfig.getProperty("cpas.symbpredabs.refinement.msatCexFile");
      if (cexFile != null) {
        String path = CPAMain.cpaConfig.getProperty("output.path") + cexFile;
        try {
          SymbolicFormula t = mgr.makeTrue();
          for (SymbolicFormula fm : f) {
            t = mgr.makeAnd(t, fm);
          }
          String msatRepr = mmgr.dumpFormula(t);

          PrintWriter pw = new PrintWriter(new File(path));
          pw.println(msatRepr);
          pw.close();
        } catch (FileNotFoundException e) {
          CPAMain.logManager.log(Level.INFO,
              "Failed to save msat Counterexample to file ", path,
              " (", e.getMessage(), ")");
        }
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

    CPAMain.logManager.log(Level.ALL, "Counterexample information:", info);

    return info;
  }

  private Vector<SymbolicFormula> getFormulasForTrace(
      SymbolicFormulaManager mgr,
      ArrayList<SymbPredAbsAbstractElement> abstractTrace) {

    // create the DAG formula corresponding to the abstract trace. We create
    // n formulas, one per interpolation group
    SSAMap ssa = null;

    Vector<SymbolicFormula> f = new Vector<SymbolicFormula>();

    for (int i = 1; i < abstractTrace.size(); ++i) {
      SymbPredAbsAbstractElement e = abstractTrace.get(i);
      // TODO here we take the formula from the abstract element
      PathFormula p = getInitSymbolicFormula(e.getInitAbstractionFormula(), mgr, (ssa == null));
      SSAMap newSsa;
      
      if (ssa != null) {
        CPAMain.logManager.log(Level.ALL, "DEBUG_3", "SHIFTING:", p.getSymbolicFormula(), " WITH SSA: ", ssa);
        p = mgr.shift(p.getSymbolicFormula(), ssa);
        newSsa = p.getSsa();
        CPAMain.logManager.log(Level.ALL, "DEBUG_3", "RESULT:", p.getSymbolicFormula(), " SSA: ", newSsa);
        newSsa.update(ssa);
      } else {
        newSsa = p.getSsa();
        CPAMain.logManager.log(Level.ALL, "DEBUG_3", "INITIAL:", p.getSymbolicFormula(), " SSA: ", newSsa);
      }
      f.add(p.getSymbolicFormula());
      ssa = newSsa;

      CPAMain.logManager.log(Level.ALL, "DEBUG_2", "Adding formula:", p.getSymbolicFormula());
    }
    return f;
  }

  /**
   * Looks for uninterpreted functions in the trace formulas and adds bitwise
   * axioms for them to the last trace formula. Returns true if an UF was found.
   * @param mgr
   * @param traceFormulas
   * @return
   */
  private boolean addBitwiseAxioms(SymbolicFormulaManager mgr,
      Vector<SymbolicFormula> traceFormulas) {
    MathsatSymbolicFormulaManager mmgr = (MathsatSymbolicFormulaManager)mgr;

    boolean foundUninterpretedFunction = false;
    
    SymbolicFormula bitwiseAxioms = mgr.makeTrue();
    boolean useBitwiseAxioms = CPAMain.cpaConfig.getBooleanValue("cpas.symbpredabs.useBitwiseAxioms");
    
    for (SymbolicFormula fm : traceFormulas) {
      boolean hasUf = mmgr.hasUninterpretedFunctions((MathsatSymbolicFormula)fm);
      if (hasUf) {
        foundUninterpretedFunction = true;  

        if (useBitwiseAxioms) {
          SymbolicFormula a = mmgr.getBitwiseAxioms((MathsatSymbolicFormula)fm);
          bitwiseAxioms = mgr.makeAnd(bitwiseAxioms, a);
        } else {
          // do not need to check all formulas, one with UF is enough
          break;
        }
      }
    }
    
    if (useBitwiseAxioms && foundUninterpretedFunction) {
      CPAMain.logManager.log(Level.ALL, "DEBUG_3", "ADDING BITWISE AXIOMS TO THE",
          "LAST GROUP: ", bitwiseAxioms);
      traceFormulas.setElementAt(mgr.makeAnd(traceFormulas.elementAt(traceFormulas.size()-1), bitwiseAxioms),
          traceFormulas.size()-1);
    }
    return foundUninterpretedFunction;
  }
  
  private boolean checkInfeasabilityOfTrace(
      Vector<SymbolicFormula> traceFormulas, boolean shortestTrace,
      boolean useSuffix, boolean useZigZag) {
    boolean spurious;
    if (shortestTrace) {
      Boolean tmpSpurious = null;
      
      if (useZigZag) {
        int e = traceFormulas.size()-1;
        int s = 0;
        boolean fromStart = false;
        while (s <= e) {
          int i = fromStart ? s : e;
          if (fromStart) s++;
          else e--;
          fromStart = !fromStart;
          
          tmpSpurious = null;
          SymbolicFormula fm = traceFormulas.elementAt(i);
          itpProver.addFormula(fm);
          if (!fm.isTrue()) {
            if (itpProver.isUnsat()) {
              tmpSpurious = Boolean.TRUE;
              for (int j = s; j <= e; ++j) {
                itpProver.addFormula(traceFormulas.elementAt(j));
              }
              break;
            } else {
              tmpSpurious = Boolean.FALSE;
            }
          }
        }
        
      } else {
        for (int i = useSuffix ? traceFormulas.size()-1 : 0;
              useSuffix ? i >= 0 : i < traceFormulas.size(); i += useSuffix ? -1 : 1) {
          
          tmpSpurious = null;
          SymbolicFormula fm = traceFormulas.elementAt(i);
          itpProver.addFormula(fm);
          if (!fm.isTrue()) {
            if (itpProver.isUnsat()) {
              tmpSpurious = Boolean.TRUE;
              // we need to add the other formulas to the itpProver
              // anyway, so it can setup its internal state properly
              for (int j = i+(useSuffix ? -1 : 1);
                  useSuffix ? j >= 0 : j < traceFormulas.size();
                  j += useSuffix ? -1 : 1) {
                itpProver.addFormula(traceFormulas.elementAt(j));
              }
              break;
            } else {
              tmpSpurious = Boolean.FALSE;
            }
          }
        }
      }
      
      if (tmpSpurious == null) {
        spurious = itpProver.isUnsat();
      } else {
        spurious = tmpSpurious.booleanValue();
      }
      
    } else {
      // ZigZag makes no sense here
      
      for (int i = useSuffix ? traceFormulas.size()-1 : 0;
        useSuffix ? i >= 0 : i < traceFormulas.size(); i += useSuffix ? -1 : 1) {
        
        SymbolicFormula fm = traceFormulas.elementAt(i);
        itpProver.addFormula(fm); 
      }
      spurious = itpProver.isUnsat();
    }
    return spurious;
  }

  private PathFormula getInitSymbolicFormula(PathFormula pf, SymbolicFormulaManager mgr, boolean replace) {
    SSAMap ssa = new SSAMap();
    SymbolicFormula f = mgr.makeFalse();
    Pair<Pair<SymbolicFormula, SymbolicFormula>, SSAMap> mp =
      mgr.mergeSSAMaps(ssa, pf.getSsa(), false);
    SymbolicFormula curf = pf.getSymbolicFormula();
    // TODO modified if
    if (replace) {
      curf = ((MathsatSymbolicFormulaManager)mgr).replaceAssignments((MathsatSymbolicFormula)curf);
    }
    f = mgr.makeAnd(f, mp.getFirst().getFirst());
    curf = mgr.makeAnd(curf, mp.getFirst().getSecond());
    f = mgr.makeOr(f, curf);
    ssa = mp.getSecond();
    return new PathFormula(f,ssa);
  }

  
  // generates the predicates corresponding to the given atoms, which were
  // extracted from the interpolant
  private Set<Predicate> buildPredicates(SymbolicFormulaManager mgr,
      Collection<SymbolicFormula> atoms) {
    long dstenv = ((MathsatSymbolicFormulaManager)mgr).getMsatEnv();
    Set<Predicate> ret = new HashSet<Predicate>();
    for (SymbolicFormula atom : atoms) {
      long tt = ((MathsatSymbolicFormula)atom).getTerm();
      long d = mathsat.api.msat_declare_variable(dstenv,
          "\"PRED" + mathsat.api.msat_term_repr(tt) + "\"",
          mathsat.api.MSAT_BOOL);
      long var = mathsat.api.msat_make_variable(dstenv, d);

      assert(!mathsat.api.MSAT_ERROR_TERM(tt));
      assert(!mathsat.api.MSAT_ERROR_TERM(var));

      ret.add(makePredicate(var, tt));
    }
    return ret;
  }

  @Override
  public boolean entails(AbstractFormula f1, AbstractFormula f2) {
    long start = System.currentTimeMillis();
    boolean ret = super.entails(f1, f2);
    long end = System.currentTimeMillis();
    stats.bddCoverageCheckMaxTime = Math.max(stats.bddCoverageCheckMaxTime,
        (end - start));
    stats.bddCoverageCheckTime += (end - start);
    ++stats.numCoverageChecks;
    return ret;
  }

  private Vector<SymbolicFormula> getUsefulBlocks(
      SymbolicFormulaManager mgr, Vector<SymbolicFormula> f,
      boolean theoryCombinationNeeded, boolean suffixTrace,
      boolean zigZag) {
    long gubStart = System.currentTimeMillis();
    
    // try to find a minimal-unsatisfiable-core of the trace (as Blast does)

    thmProver.init(TheoremProver.COUNTEREXAMPLE_ANALYSIS);

    CPAMain.logManager.log(Level.ALL, "DEBUG_1", "Calling getUsefulBlocks on path",
        "of length:", f.size());

    SymbolicFormula trueFormula = mgr.makeTrue();
    SymbolicFormula[] needed = new SymbolicFormula[f.size()];
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

          SymbolicFormula t = f.elementAt(i);
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
          SymbolicFormula t = f.elementAt(i);
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
        break;
      }
    }

    while (toPop > 0) {
      --toPop;
      thmProver.pop();
    }

    thmProver.reset();

    CPAMain.logManager.log(Level.ALL, "DEBUG_1", "Done getUsefulBlocks");

    long gubEnd = System.currentTimeMillis();
    stats.cexAnalysisGetUsefulBlocksTime += gubEnd - gubStart;
    stats.cexAnalysisGetUsefulBlocksMaxTime = Math.max(
        stats.cexAnalysisGetUsefulBlocksMaxTime, gubEnd - gubStart);
    
    return f;
  }

}
