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
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;

import symbpredabstraction.PathFormula;
import symbpredabstraction.SSAMap;
import symbpredabstraction.bdd.BDDAbstractFormula;
import symbpredabstraction.bdd.BDDPredicate;
import symbpredabstraction.interfaces.AbstractFormula;
import symbpredabstraction.interfaces.InterpolatingTheoremProver;
import symbpredabstraction.interfaces.Predicate;
import symbpredabstraction.interfaces.SymbolicFormula;
import symbpredabstraction.interfaces.SymbolicFormulaManager;
import symbpredabstraction.interfaces.TheoremProver;
import symbpredabstraction.mathsat.BDDMathsatAbstractFormulaManager;
import symbpredabstraction.mathsat.BDDMathsatAbstractionPrinter;
import symbpredabstraction.mathsat.MathsatSymbolicFormula;
import symbpredabstraction.mathsat.MathsatSymbolicFormulaManager;
import symbpredabstraction.trace.ConcreteTraceFunctionCalls;
import symbpredabstraction.trace.CounterexampleTraceInfo;

import logging.CPACheckerLogger;
import logging.CustomLogLevel;
import logging.LazyLogger;
import cfa.objectmodel.CFAFunctionDefinitionNode;
import cfa.objectmodel.CFANode;
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
  private static long replacing = 0;

  private class AllSatCallbackStats extends AllSatCallback
  implements TheoremProver.AllSatCallback {
    public long totTime = 0;
    private long[] curModel;

    public AllSatCallbackStats(int bdd, long msatEnv, long absEnv) {
      super(bdd, msatEnv, absEnv);
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
  }
  private Stats stats;

  private TheoremProver thmProver;
  private InterpolatingTheoremProver itpProver;

  private Map<Pair<SymbolicFormula, Vector<SymbolicFormula>>, AbstractFormula>
  abstractionCache;
  private boolean useCache;

  private static abstract class KeyWithTimeStamp {
    public long timeStamp;

    public KeyWithTimeStamp() {
      updateTimeStamp();
    }

    public void updateTimeStamp() {
      timeStamp = System.currentTimeMillis();
    }
  }

  private static class CartesianAbstractionCacheKey extends KeyWithTimeStamp {
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

  private static class FeasibilityCacheKey extends KeyWithTimeStamp {
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

  private static class TimeStampCache<Key extends KeyWithTimeStamp, Value>
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

  //cache for cartesian abstraction queries. For each predicate, the values
  // are -1: predicate is false, 0: predicate is don't care,
  // 1: predicate is true
  private TimeStampCache<CartesianAbstractionCacheKey, Byte>
  cartesianAbstractionCache;
  private TimeStampCache<FeasibilityCacheKey, Boolean> feasibilityCache;

  private BDDMathsatAbstractionPrinter absPrinter = null;
  private boolean dumpHardAbstractions;

  public BDDMathsatSymbPredAbstractionAbstractManager(TheoremProver prover,
      InterpolatingTheoremProver interpolator) 
  {
    super();
    stats = new Stats();
    dumpHardAbstractions = CPAMain.cpaConfig.getBooleanValue("cpas.symbpredabs.mathsat.dumpHardAbstractionQueries");
    thmProver = prover;
    itpProver = interpolator;

    abstractionCache =
      new HashMap<Pair<SymbolicFormula, Vector<SymbolicFormula>>,
      AbstractFormula>();
    useCache = CPAMain.cpaConfig.getBooleanValue("cpas.symbpredabs.mathsat.useCache");

    final int MAX_CACHE_SIZE = 100000;
    cartesianAbstractionCache =
      new TimeStampCache<CartesianAbstractionCacheKey, Byte>(MAX_CACHE_SIZE);
    feasibilityCache =
      new TimeStampCache<FeasibilityCacheKey, Boolean>(MAX_CACHE_SIZE);
  }

  public Stats getStats() { return stats; }

  @Override
  public AbstractFormula buildAbstraction(SymbolicFormulaManager mgr,
      AbstractFormula abs, PathFormula pathFormula,
      Collection<Predicate> predicates, MathsatSymbolicFormula functionExitFormula,
      CFANode pSucc, AbstractionPathList pPathList) {
    stats.numCallsAbstraction++;
    if (CPAMain.cpaConfig.getBooleanValue(
    "cpas.symbpredabs.abstraction.cartesian")) {
      return buildCartesianAbstraction(mgr, abs, pathFormula, predicates, functionExitFormula);
    } else {
      return buildBooleanAbstraction(mgr, abs, pathFormula, predicates, functionExitFormula);
    }
  }

  private AbstractFormula buildCartesianAbstraction(
      SymbolicFormulaManager mgr,
      AbstractFormula abs,
      PathFormula pathFormula,
      Collection<Predicate> predicates,
      MathsatSymbolicFormula functionExitFormula) {
    
    long startTime = System.currentTimeMillis();

    MathsatSymbolicFormulaManager mmgr = (MathsatSymbolicFormulaManager)mgr;

    long msatEnv = mmgr.getMsatEnv();

    thmProver.init(TheoremProver.CARTESIAN_ABSTRACTION);

//    if (isFunctionExit(e)) {
    if (functionExitFormula != null) {
        // we have to take the context before the function call
        // into account, otherwise we are not building the right
        // abstraction!
        assert(false); // TODO
//        if (CPAMain.cpaConfig.getBooleanValue(
//                "cpas.symbpredabs.refinement.addWellScopedPredicates")) {
//            // but only if we are adding well-scoped predicates, otherwise
//            // this should not be necessary
//            AbstractFormula ctx = e.topContextAbstraction();
//            MathsatSymbolicFormula fctx =
//                (MathsatSymbolicFormula)mmgr.instantiate(
//                        toConcrete(mmgr, ctx), null);
//            fabs = (MathsatSymbolicFormula)mmgr.makeAnd(fabs, fctx);
//
//            LazyLogger.log(LazyLogger.DEBUG_3,
//                    "TAKING CALLING CONTEXT INTO ACCOUNT: ", fctx);
//        } else {
//            LazyLogger.log(LazyLogger.DEBUG_3,
//                    "NOT TAKING CALLING CONTEXT INTO ACCOUNT,",
//                    "as we are not using well-scoped predicates");
//        }
    }

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
                return new BDDAbstractFormula(bddManager.getZero());
            }
        }
    }

    if (CPAMain.cpaConfig.getBooleanValue(
            "cpas.symbpredabs.useBitwiseAxioms")) {
        MathsatSymbolicFormula bitwiseAxioms = mmgr.getBitwiseAxioms(
                (MathsatSymbolicFormula)f);
        f = mmgr.makeAnd(f, bitwiseAxioms);

        LazyLogger.log(LazyLogger.DEBUG_3, "ADDED BITWISE AXIOMS: ",
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
            return new BDDAbstractFormula(bddManager.getZero());
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

    int absbdd = bddManager.getOne();

    // check whether each of the predicate is implied in the next state...
    Set<String> predvars = new HashSet<String>();
    Set<Pair<String, SymbolicFormula[]>> predlvals =
        new HashSet<Pair<String, SymbolicFormula[]>>();
    Map<SymbolicFormula, SymbolicFormula> predLvalsCache =
        new HashMap<SymbolicFormula, SymbolicFormula>();

    int predIndex = -1;
    for (Predicate p : predicates) {
        ++predIndex;
        BDDPredicate bp = (BDDPredicate)p;
        if (useCache && predVals[predIndex] != NO_VALUE) {
            long startBddTime = System.currentTimeMillis();
            int v = bp.getBDD();
            if (predVals[predIndex] == -1) { // pred is false
                v = bddManager.not(v);
                absbdd = bddManager.and(absbdd, v);
            } else if (predVals[predIndex] == 1) { // pred is true
                absbdd = bddManager.and(absbdd, v);
            }
            long endBddTime = System.currentTimeMillis();
            totBddTime += (endBddTime - startBddTime);
            //++stats.abstractionNumCachedQueries;
        } else {
            Pair<MathsatSymbolicFormula, MathsatSymbolicFormula> pi =
                getPredicateNameAndDef(bp);

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


            LazyLogger.log(LazyLogger.DEBUG_1,
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
                int v = bp.getBDD();
                absbdd = bddManager.and(absbdd, v);
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
                    int v = bp.getBDD();
                    v = bddManager.not(v);
                    absbdd = bddManager.and(absbdd, v);
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

    return new BDDAbstractFormula(absbdd);
    
  }

  private AbstractFormula buildBooleanAbstraction(SymbolicFormulaManager mgr,
      AbstractFormula abs, PathFormula pathFormula,
      Collection<Predicate> predicates, MathsatSymbolicFormula functionExitFormula) {
    // A SummaryFormulaManager for MathSAT formulas
    MathsatSymbolicFormulaManager mmgr = (MathsatSymbolicFormulaManager)mgr;

    long startTime = System.currentTimeMillis();

    // get the environment from the manager - this is unique, it is the
    // environment in which all terms are created
    long msatEnv = mmgr.getMsatEnv();

    // first, build the concrete representation of the abstract formula of e
    // this is an abstract formula - specifically it is a bddabstractformula
    // which is basically an integer which represents it
    // create the concrete form of the abstract formula
    // (abstract formula is the bdd representation)
    MathsatSymbolicFormula fabs =
      (MathsatSymbolicFormula)mmgr.instantiate(
          toConcrete(mmgr, abs), null);

    LazyLogger.log(LazyLogger.DEBUG_3, "Abstraction: ",
        mathsat.api.msat_term_id(fabs.getTerm()));

    if (functionExitFormula != null) {
      if (CPAMain.cpaConfig.getBooleanValue(
      "cpas.symbpredabs.refinement.addWellScopedPredicates")) {
        // but only if we are adding well-scoped predicates, otherwise
        // this should not be necessary
        fabs = (MathsatSymbolicFormula)mmgr.makeAnd(fabs, functionExitFormula);

        LazyLogger.log(LazyLogger.DEBUG_3,
            "TAKING CALLING CONTEXT INTO ACCOUNT: ", functionExitFormula);
      } else {
        LazyLogger.log(LazyLogger.DEBUG_3,
            "NOT TAKING CALLING CONTEXT INTO ACCOUNT,",
        "as we are not using well-scoped predicates");
      }
    }

    // create an ssamap from concrete formula
    SSAMap absSsa = mmgr.extractSSA(fabs);

    SymbolicFormula f = null;
    SSAMap ssa = null;
    long start = System.currentTimeMillis();

//    Pair<CFANode, AbstractionPathList> key = new Pair<CFANode, AbstractionPathList>(pSucc, pPathList);

//    if (abstractionTranslationCache.containsKey(key)) {
//      PathFormula pc = abstractionTranslationCache.get(key);
//      f = pc.getSymbolicFormula();
//      ssa = pc.getSsa();
//    } else {
      f = pathFormula.getSymbolicFormula();
      ssa = pathFormula.getSsa();

      pathFormula = toPathFormula(mmgr.shift(f, absSsa));
      f = mmgr.replaceAssignments((MathsatSymbolicFormula)pathFormula.getSymbolicFormula());
      ssa = pathFormula.getSsa();

//      abstractionTranslationCache.put(key, new PathFormula(f, ssa));
//    }
    
    assert(f != null);
    assert(ssa != null);

    long end = System.currentTimeMillis();
    replacing = replacing + (end - start);
    if (CPAMain.cpaConfig.getBooleanValue(
        "cpas.symbpredabs.useBitwiseAxioms")) {
      MathsatSymbolicFormula bitwiseAxioms = mmgr.getBitwiseAxioms(
          (MathsatSymbolicFormula)f);
      f = mmgr.makeAnd(f, bitwiseAxioms);

      LazyLogger.log(LazyLogger.DEBUG_3, "ADDED BITWISE AXIOMS: ",
          bitwiseAxioms);
    }

    long term = ((MathsatSymbolicFormula)f).getTerm();
    assert(!mathsat.api.MSAT_ERROR_TERM(term));

    LazyLogger.log(LazyLogger.DEBUG_2, "Term: ", f);


    // build the definition of the predicates, and instantiate them
    PredInfo predinfo = buildPredList(mmgr, predicates);
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
        getInstantiatedAt(mmgr, p.getSecond(), ssa, cache);
      if (ssa.getIndex(p.getFirst(), args) < 0) {
        ssa.setIndex(p.getFirst(), args, 1);
      }
    }

    if (CPACheckerLogger.getLevel() <= LazyLogger.DEBUG_1.intValue()) {
      StringBuffer importantStrBuf = new StringBuffer();
      for (long t : important) {
        importantStrBuf.append(mathsat.api.msat_term_repr(t));
        importantStrBuf.append(" ");
      }
      LazyLogger.log(LazyLogger.DEBUG_1,
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

    LazyLogger.log(LazyLogger.DEBUG_2,
        "COMPUTING ALL-SMT ON FORMULA: ", fm);

    Pair<SymbolicFormula, Vector<SymbolicFormula>> absKey =
      new Pair<SymbolicFormula, Vector<SymbolicFormula>>(fm, imp);
    AbstractFormula result = null;
    if (useCache && abstractionCache.containsKey(absKey)) {
      ++stats.numCallsAbstractionCached;
      result = abstractionCache.get(absKey);
    } else {
      int absbdd = bddManager.getZero();
      AllSatCallbackStats func =
        new AllSatCallbackStats(absbdd, msatEnv, 0);
      long msatSolveStartTime = System.currentTimeMillis();
      int numModels = thmProver.allSat(fm, imp, func);
      assert(numModels != -1);
      long msatSolveEndTime = System.currentTimeMillis();

      // update statistics
      long endTime = System.currentTimeMillis();
      long msatSolveTime =
        (msatSolveEndTime - msatSolveStartTime) - func.totTime;
      long abstractionMsatTime = (endTime - startTime) - func.totTime;
      stats.abstractionMaxMathsatTime =
        Math.max(abstractionMsatTime, stats.abstractionMaxMathsatTime);
      stats.abstractionMaxBddTime =
        Math.max(func.totTime, stats.abstractionMaxBddTime);
      stats.abstractionMathsatTime += abstractionMsatTime;
      stats.abstractionBddTime += func.totTime;
      stats.abstractionMathsatSolveTime += msatSolveTime;
      stats.abstractionMaxMathsatSolveTime =
        Math.max(msatSolveTime, stats.abstractionMaxMathsatSolveTime);

      // TODO dump hard abst
      if (abstractionMsatTime > 10000 && dumpHardAbstractions) {
        // we want to dump "hard" problems...
        if (absPrinter == null) {
          absPrinter = new BDDMathsatAbstractionPrinter(
              msatEnv, "abs");
        }
        absPrinter.printMsatFormat(curstate, term, preddef, important);
        absPrinter.printNusmvFormat(curstate, term, preddef, important);
        absPrinter.nextNum();
      }

      if (numModels == -2) {
        absbdd = bddManager.getOne();
        //return new BDDAbstractFormula(absbdd);
        result = new BDDAbstractFormula(absbdd);
      } else {
        //return new BDDAbstractFormula(func.getBDD());
        result = new BDDAbstractFormula(func.getBDD());
      }
      // TODO later
      if (useCache) {
        abstractionCache.put(absKey, result);
      }
    }

    return result;
  }

  @Override
  public CounterexampleTraceInfo buildCounterexampleTrace(SymbolicFormulaManager mgr,
      Deque<SymbPredAbsAbstractElement> abstractTrace) {
    assert(abstractTrace.size() > 1);

    long startTime = System.currentTimeMillis();
    stats.numCallsCexAnalysis++;

    // create the DAG formula corresponding to the abstract trace. We create
    // n formulas, one per interpolation group
    SSAMap ssa = null;
    MathsatSymbolicFormulaManager mmgr = (MathsatSymbolicFormulaManager)mgr;

    Vector<SymbolicFormula> f = new Vector<SymbolicFormula>();

    LazyLogger.log(LazyLogger.DEBUG_1, "\nBUILDING COUNTEREXAMPLE TRACE\n");
    LazyLogger.log(LazyLogger.DEBUG_1, "ABSTRACT TRACE: ", abstractTrace);

    //printFuncNamesInTrace(abstractTrace);

    Object[] abstarr = abstractTrace.toArray();
    //SymbPredAbsAbstractElement cur = (SymbPredAbsAbstractElement)abstarr[0];

    boolean theoryCombinationNeeded = false;
    boolean noDtc = CPAMain.cpaConfig.getBooleanValue(
    "cpas.symbpredabs.mathsat.useDtc") == false;

    MathsatSymbolicFormula bitwiseAxioms =
      (MathsatSymbolicFormula)mmgr.makeTrue();
    for (int i = 1; i < abstarr.length; ++i) {
      SymbPredAbsAbstractElement e = (SymbPredAbsAbstractElement)abstarr[i];
      // TODO here we take the formula from the abstract element
//    Pair<SymbolicFormula, SSAMap> p =
//    buildConcreteFormula(mmgr, cur, e, (ssa == null));
      PathFormula p = e.getInitAbstractionFormula().getInitSymbolicFormula(mgr, (ssa == null));
      SSAMap newssa = null;
      if (ssa != null) {
        LazyLogger.log(LazyLogger.DEBUG_3, "SHIFTING: ", p.getSymbolicFormula(),
            " WITH SSA: ", ssa);
        p = toPathFormula(mmgr.shift(p.getSymbolicFormula(), ssa));
        newssa = p.getSsa();
        LazyLogger.log(LazyLogger.DEBUG_3, "RESULT: ", p.getSymbolicFormula(),
            " SSA: ", newssa);
        newssa.update(ssa);
      } else {
        LazyLogger.log(LazyLogger.DEBUG_3, "INITIAL: ", p.getSymbolicFormula(),
            " SSA: ", p.getSsa());
        newssa = p.getSsa();
      }
      boolean hasUf = false;
      if (!noDtc) {
        hasUf = mmgr.hasUninterpretedFunctions(
            (MathsatSymbolicFormula)p.getSymbolicFormula());
        theoryCombinationNeeded |= hasUf;
      }
      f.add(p.getSymbolicFormula());
      ssa = newssa;
      //cur = e;

      if (hasUf && CPAMain.cpaConfig.getBooleanValue(
      "cpas.symbpredabs.useBitwiseAxioms")) {
        MathsatSymbolicFormula a = mmgr.getBitwiseAxioms(
            (MathsatSymbolicFormula)p.getSymbolicFormula());
        bitwiseAxioms = (MathsatSymbolicFormula)mmgr.makeAnd(
            bitwiseAxioms, a);
      }

      LazyLogger.log(LazyLogger.DEBUG_2, "Adding formula: ", p.getSymbolicFormula());
//    mathsat.api.msat_term_id(
//    ((MathsatSymbolicFormula)p.getFirst()).getTerm()));
    }

    if (CPAMain.cpaConfig.getBooleanValue(
    "cpas.symbpredabs.useBitwiseAxioms")) {
      LazyLogger.log(LazyLogger.DEBUG_3, "ADDING BITWISE AXIOMS TO THE ",
          "LAST GROUP: ", bitwiseAxioms);
      f.setElementAt(mmgr.makeAnd(f.elementAt(f.size()-1), bitwiseAxioms),
          f.size()-1);
    }

    LazyLogger.log(LazyLogger.DEBUG_3,
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
        itpProver.addFormula(fm);
        if (shortestTrace && !fm.isTrue()) {
          if (itpProver.isUnsat()) {
            res = 0;
            // we need to add the other formulas to the itpProver
            // anyway, so it can setup its internal state properly
            for (int j = i+(useSuffix ? -1 : 1);
            useSuffix ? j >= 0 : j < f.size();
            j += useSuffix ? -1 : 1) {
              itpProver.addFormula(f.elementAt(j));
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
        itpProver.addFormula(fm);
        if (!fm.isTrue()) {
          if (itpProver.isUnsat()) {
            res = 0;
            for (int j = s; j <= e; ++j) {
              itpProver.addFormula(f.elementAt(j));
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
        msatSolveTimeStart = System.currentTimeMillis();
        SymbolicFormula itp = itpProver.getInterpolant(formulasOfA);
        msatSolveTimeEnd = System.currentTimeMillis();
        msatSolveTime += msatSolveTimeEnd - msatSolveTimeStart;

        Collection<SymbolicFormula> atoms = mmgr.extractAtoms(
            itp, true, splitItpAtoms, false);
        Set<Predicate> preds = buildPredicates(msatEnv, atoms);
        SymbPredAbsAbstractElement s1 =
          (SymbPredAbsAbstractElement)abstarr[i];
        info.addPredicatesForRefinement(s1, preds);

        LazyLogger.log(LazyLogger.DEBUG_1,
            "Got interpolant(", i, "): ", itp, ", location: ", s1);
        LazyLogger.log(LazyLogger.DEBUG_1, "Preds for ",
            s1.getAbstractionLocation(), ": ", preds);

        // If we are entering or exiting a function, update the stack
        // of entry points
        SymbPredAbsAbstractElement e = (SymbPredAbsAbstractElement)abstarr[i];
        // TODO checking if the abstraction node is a new function
        if (e.getAbstractionLocation() instanceof CFAFunctionDefinitionNode) {
          LazyLogger.log(LazyLogger.DEBUG_3,
              "Pushing entry point, function: ",
              e.getAbstractionLocation().getFunctionName());
          entryPoints.push(i);
        }
        // TODO check we are returning from a function
        if (e.getAbstractionLocation().getEnteringSummaryEdge() != null) {
          LazyLogger.log(LazyLogger.DEBUG_3,
              "Popping entry point, returning from function: ",
              e.getAbstractionLocation().getEnteringEdge(0).getPredecessor().getFunctionName());
          entryPoints.pop();

//        SummaryAbstractElement s1 =
//        (SummaryAbstractElement)abstarr[i];
          //pmap.update((CFANode)s1.getLocation(), preds);
        }
      }
    } else {
      // this is a real bug, notify the user
      info = new CounterexampleTraceInfo(false);
      ConcreteTraceFunctionCalls cf = new ConcreteTraceFunctionCalls();
      for (SymbPredAbsAbstractElement e : abstractTrace) {
        cf.add(e.getAbstractionLocation().getFunctionName());
      }
      info.setConcreteTrace(cf);
      // TODO - reconstruct counterexample
      // For now, we dump the asserted formula to a user-specified file
      String cexPath = CPAMain.cpaConfig.getProperty(
      "cpas.symbpredabs.refinement.msatCexPath");
      if (cexPath != null) {
        long t = mathsat.api.msat_make_true(msatEnv);
        for (SymbolicFormula fm : f) {
          long term = ((MathsatSymbolicFormula)fm).getTerm();
          t = mathsat.api.msat_make_and(msatEnv, t, term);
        }
        String msatRepr = mathsat.api.msat_to_msat(msatEnv, t);
        try {
          PrintWriter pw = new PrintWriter(new File(cexPath));
          pw.println(msatRepr);
          pw.close();
        } catch (FileNotFoundException e) {
          LazyLogger.log(CustomLogLevel.INFO,
              "Failed to save msat Counterexample to file: ",
              cexPath);
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

    return info;
  }

  // generates the predicates corresponding to the given atoms, which were
  // extracted from the interpolant
  private Set<Predicate> buildPredicates(long dstenv,
      Collection<SymbolicFormula> atoms) {
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

  public Vector<SymbolicFormula> getUsefulBlocks(
      SymbolicFormulaManager mgr, Vector<SymbolicFormula> f,
      boolean theoryCombinationNeeded, boolean suffixTrace,
      boolean zigZag, boolean setAllTrueIfSat) {
    // try to find a minimal-unsatisfiable-core of the trace (as Blast does)
    MathsatSymbolicFormulaManager mmgr =
      (MathsatSymbolicFormulaManager)mgr;

    long msatEnv = mmgr.getMsatEnv();
    thmProver.init(TheoremProver.COUNTEREXAMPLE_ANALYSIS);

    LazyLogger.log(LazyLogger.DEBUG_1, "Calling getUsefulBlocks on path ",
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
            LazyLogger.log(LazyLogger.DEBUG_1,
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
            LazyLogger.log(LazyLogger.DEBUG_1,
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

    LazyLogger.log(LazyLogger.DEBUG_1, "Done getUsefulBlocks");

    return f;
  }

  private PathFormula toPathFormula(Pair<SymbolicFormula, SSAMap> pair) {
    return new PathFormula(pair.getFirst(), pair.getSecond());
  }

}
