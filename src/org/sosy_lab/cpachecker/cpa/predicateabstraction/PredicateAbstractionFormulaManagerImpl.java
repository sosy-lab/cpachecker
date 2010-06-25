/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.predicateabstraction;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.objectmodel.BlankEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionDefinitionNode;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;
import org.sosy_lab.cpachecker.util.symbpredabstraction.CommonFormulaManager;
import org.sosy_lab.cpachecker.util.symbpredabstraction.SSAMap;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.AbstractFormula;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.AbstractFormulaManager;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.InterpolatingTheoremProver;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.Predicate;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormula;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormulaManager;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.TheoremProver;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormulaManager.AllSatCallback;
import org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatSymbolicFormulaManager;
import org.sosy_lab.cpachecker.util.symbpredabstraction.trace.CounterexampleTraceInfo;

/**
 * Implementation of ExplicitAbstractFormulaManager that uses BDDs for
 * AbstractFormulas and MathSAT terms for SymbolicFormulas
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
@Options(prefix="cpas.symbpredabs")
class PredicateAbstractionFormulaManagerImpl<T> extends
CommonFormulaManager
implements PredicateAbstractionFormulaManager {

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
    public long cexAnalysisGetUsefulBlocksTime = 0;
    public long cexAnalysisGetUsefulBlocksMaxTime = 0;

    // extended statistics
    public long cacheLookupTime = 0;
    public long termBuildTime = 0;
    public long msatTermCopyTime = 0;
    public long predicateExtractionTime = 0;
    public long extraTime = 0;
    public long extraTimeSub1 = 0;

    public Map<CFAEdge, Integer> edgeAbstCountMap =
      new HashMap<CFAEdge, Integer>();
    public int abstractionNumCachedQueries;
    public int makeFormulaCalls;
    public int makeFormulaCacheHits;
  }
  protected Stats stats;

  @Option(name="explicit.extendedStats")
  private boolean extendedStats = false;

  @Option(name="abstraction.cartesian")
  private boolean cartesianAbstraction = false;

  @Option(name="explicit.getUsefulBlocks")
  private boolean getUsefulBlocks = false;

  @Option(name="shortestCexTrace")
  private boolean shortestTrace = false;

  @Option(name="refinement.splitItpAtoms")
  private boolean splitItpAtoms = false;

  @Option
  private boolean useBitwiseAxioms = false;

  @Option(name="shortestCexTraceUseSuffix")
  private boolean useSuffix = false;

  @Option(name="shortestCexTraceZigZag")
  private boolean useZigZag = false;

  @Option(name="refinement.useBlastWay")
  private boolean useBlastWay = false;

  @Option(name="refinement.addWellScopedPredicates")
  private boolean wellScopedPredicates = false;

  @Option(name="refinement.addPredicatesGlobally")
  private boolean addPredicatesGlobally;

  @Option(name="refinement.msatCexFile", type=Option.Type.OUTPUT_FILE)
  private File msatCexFile = new File("cex.msat");

  @Option(name="abstraction.explicit.nonAtomicPredicates")
  private boolean nonAtomicPredicates = false;

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

  static class BooleanAbstractionCacheKey extends KeyWithTimeStamp {
    SymbolicFormula formula;
    Set<Predicate> predList;

    public BooleanAbstractionCacheKey(SymbolicFormula f,
        Collection<Predicate> preds) {
      super();
      formula = f;
      if (preds.isEmpty()) {
        predList = Collections.emptySet();
      } else {
        predList = new HashSet<Predicate>();
        predList.addAll(preds);
      }
    }

    private boolean samePreds(Set<Predicate> s1, Set<Predicate> s2) {
      if (s1.size() == s2.size()) {
        return s1.containsAll(s2);
      } else {
        return false;
      }
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o instanceof BooleanAbstractionCacheKey) {
        BooleanAbstractionCacheKey c =
          (BooleanAbstractionCacheKey)o;
        return (formula.equals(c.formula) &&
            samePreds(predList, c.predList));
      } else {
        return false;
      }
    }

    @Override
    public int hashCode() {
      return formula.hashCode() ^ predList.hashCode();
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
  protected TimeStampCache<BooleanAbstractionCacheKey, AbstractFormula>
  booleanAbstractionCache;
  protected Map<Pair<CFAEdge, SSAMap>, Pair<SymbolicFormula, SSAMap>>
  makeFormulaCache;
  protected Map<SymbolicFormula, SymbolicFormula> instantiateCache;
  protected Map<SymbolicFormula, SSAMap> extractSSACache;
  protected Map<Pair<SymbolicFormula, CFAEdge>,
  Pair<SymbolicFormula, SSAMap>> buildConcreteFormulaCache;

  private TheoremProver thmProver;
  private InterpolatingTheoremProver<T> itpProver;

  public PredicateAbstractionFormulaManagerImpl(
      AbstractFormulaManager amgr,
      SymbolicFormulaManager mmgr,
      TheoremProver prover,
      InterpolatingTheoremProver<T> interpolator,
      Configuration config,
      LogManager logger) throws InvalidConfigurationException {
    super(amgr, mmgr, config, logger);
    config.inject(this);
    stats = new Stats();
    if (useCache) {
      final int MAX_CACHE_SIZE = 100000;
      cartesianAbstractionCache =
        new TimeStampCache<CartesianAbstractionCacheKey, Byte>(
            MAX_CACHE_SIZE);
      feasibilityCache =
        new TimeStampCache<FeasibilityCacheKey, Boolean>(
            MAX_CACHE_SIZE);
      booleanAbstractionCache =
        new TimeStampCache<BooleanAbstractionCacheKey,
        AbstractFormula>(MAX_CACHE_SIZE);
      makeFormulaCache =
        new HashMap<Pair<CFAEdge, SSAMap>,
        Pair<SymbolicFormula, SSAMap>>();
      instantiateCache =
        new HashMap<SymbolicFormula, SymbolicFormula>();
      extractSSACache = new HashMap<SymbolicFormula, SSAMap>();
      buildConcreteFormulaCache =
        new HashMap<Pair<SymbolicFormula, CFAEdge>,
        Pair<SymbolicFormula, SSAMap>>();
    }

    thmProver = prover;
    itpProver = interpolator;
  }

  public Stats getStats() { return stats; }

  // computes the formula corresponding to executing the operation attached
  // to the given edge, starting from the data region encoded by the
  // abstraction at "e"
  protected Pair<SymbolicFormula, SSAMap> buildConcreteFormula(
      MathsatSymbolicFormulaManager mgr,
      PredicateAbstractionAbstractElement e, PredicateAbstractionAbstractElement succ,
      CFAEdge edge, boolean replaceAssignments) {

    AbstractFormula abs = e.getAbstraction();
    SymbolicFormula fabs = null;
    SymbolicFormula concr = toConcrete(abs);
    if (useCache && instantiateCache.containsKey(concr)) {
      fabs = instantiateCache.get(concr);
    } else {
      fabs = mgr.instantiate(concr/*toConcrete(mgr, abs)*/, null);
      instantiateCache.put(concr, fabs);
    }
    SSAMap ssa = null;
    if (useCache && extractSSACache.containsKey(fabs)) {
      ssa = extractSSACache.get(fabs);
    } else {
      ssa = mgr.extractSSA(fabs);
      extractSSACache.put(fabs, ssa);
    }
    Pair<SymbolicFormula, SSAMap> p = null;
    Pair<SymbolicFormula, CFAEdge> key = null;
    if (useCache) {
      key = new Pair<SymbolicFormula, CFAEdge>(fabs, edge);
      if (buildConcreteFormulaCache.containsKey(key)) {
        p = buildConcreteFormulaCache.get(key);
      }
    }
    if (p == null) {
      try {
        p = mgr.makeAnd(fabs, edge, ssa);
      } catch (UnrecognizedCFAEdgeException e1) {
        logger.logException(Level.SEVERE, e1, "");
        System.exit(1);
      }
      if (useCache) {
        buildConcreteFormulaCache.put(key, p);
      }
    }

    return p;
  }

  @Override
  public AbstractFormula buildAbstraction(
      PredicateAbstractionAbstractElement e, PredicateAbstractionAbstractElement succ,
      CFAEdge edge, Collection<Predicate> predicates) {
    stats.numCallsAbstraction++;
    if (extendedStats) {
      int n = 0;
      if (stats.edgeAbstCountMap.containsKey(edge)) {
        n = stats.edgeAbstCountMap.get(edge);
      }
      stats.edgeAbstCountMap.put(edge, n+1);
    }

//  if (!(succ.getLocation() instanceof CFAErrorNode)) {
    if ((edge instanceof BlankEdge && !(edge.getPredecessor() instanceof FunctionDefinitionNode)) //||
//      (predicates.size() == 0 &&
//      ((BDDAbstractFormula)e.getAbstraction()).getBDD() ==
//      bddManager.getOne())) {
    ) {
      logger.log(Level.ALL, "DEBUG_1",
          "SKIPPING ABSTRACTION CHECK, e: ", e, ", SUCC:", succ,
          ", edge: ", edge);
      return e.getAbstraction();
    }
//  }

    if (cartesianAbstraction) {
      return buildCartesianAbstraction(smgr, e, succ, edge, predicates);
    } else {
      return buildBooleanAbstraction(smgr, e, succ, edge, predicates);
    }
  }

  // precise predicate abstraction, using All-SMT algorithm
  protected AbstractFormula buildBooleanAbstraction(
      SymbolicFormulaManager mgr, PredicateAbstractionAbstractElement e,
      PredicateAbstractionAbstractElement succ, CFAEdge edge,
      Collection<Predicate> predicates) {
    MathsatSymbolicFormulaManager mmgr = (MathsatSymbolicFormulaManager)mgr;

    long startTime = System.currentTimeMillis();

//    if (isFunctionExit(e)) {
      // we have to take the context before the function call
      // into account, otherwise we are not building the right
      // abstraction!
//      assert(false); // TODO
      //            if (CPAMain.cpaConfig.getBooleanValue(
      //                    "cpas.symbpredabs.refinement.addWellScopedPredicates")) {
      //                // but only if we are adding well-scoped predicates, otherwise
      //                // this should not be necessary
      //                AbstractFormula ctx = e.topContextAbstraction();
      //                MathsatSymbolicFormula fctx =
      //                    (MathsatSymbolicFormula)mmgr.instantiate(
      //                            toConcrete(mmgr, ctx), null);
      //                fabs = (MathsatSymbolicFormula)mmgr.makeAnd(fabs, fctx);
      //
      //                CPAMain.logManager.log(Level.ALL, "DEBUG_3",
      //                        "TAKING CALLING CONTEXT INTO ACCOUNT: ", fctx);
      //            } else {
      //                CPAMain.logManager.log(Level.ALL, "DEBUG_3",
      //                        "NOT TAKING CALLING CONTEXT INTO ACCOUNT,",
      //                        "as we are not using well-scoped predicates");
      //            }
//    }

    Pair<SymbolicFormula, SSAMap> pc =
      buildConcreteFormula(mmgr, e, succ, edge, false);
    SymbolicFormula f = pc.getFirst();
    SSAMap ssa = pc.getSecond();

    f = mmgr.replaceAssignments(pc.getFirst());

    BooleanAbstractionCacheKey key = null;
    if (useCache) {
      key = new BooleanAbstractionCacheKey(f, predicates);
      if (booleanAbstractionCache.containsKey(key)) {
        ++stats.abstractionNumCachedQueries;
        return booleanAbstractionCache.get(key);
      }
    }

    if (useBitwiseAxioms) {
      SymbolicFormula bitwiseAxioms = mmgr.getBitwiseAxioms(f);
      f = mgr.makeAnd(f, bitwiseAxioms);

      logger.log(Level.ALL, "DEBUG_3", "ADDED BITWISE AXIOMS:", bitwiseAxioms);
    }

    // build the definition of the predicates, and instantiate them
    PredicateInfo predinfo = buildPredicateInformation(predicates);
    List<SymbolicFormula> important = predinfo.predicateNames;
    Collection<String> predvars = predinfo.allVariables;
    Collection<Pair<String, SymbolicFormula[]>> predlvals =
      predinfo.allFunctions;

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
        smgr.getInstantiatedAt(p.getSecond(), ssa, cache);
      if (ssa.getIndex(p.getFirst(), args) < 0) {
        ssa.setIndex(p.getFirst(), args, 1);
      }
    }

    logger.log(Level.ALL, "DEBUG_1",
        "IMPORTANT SYMBOLS (", important.size(), "): ",
        important);

    // first, create the new formula corresponding to
    // (f & edges from e to succ)
    // TODO - at the moment, we assume that all the edges connecting e and
    // succ have no statement or assertion attached (i.e. they are just
    // return edges or gotos). This might need to change in the future!!
    // (So, for now we don't need to to anything...)

    // instantiate the definitions with the right SSA
    SymbolicFormula preddef = mmgr.instantiate(predinfo.predicateDefinition, ssa);

    // the formula is (curstate & term & preddef)
    // build the formula and send it to the absEnv
    SymbolicFormula formula = smgr.makeAnd(f, preddef);

    logger.log(Level.ALL, "DEBUG_1", "COMPUTING ALL-SMT ON FORMULA:",
        formula);

    ++stats.abstractionNumMathsatQueries;

    AllSatCallback func = smgr.getAllSatCallback(this, amgr);
    long libmsatStartTime = System.currentTimeMillis();
    int numModels = thmProver.allSat(formula, important, func);
    assert(numModels != -1);
    long libmsatEndTime = System.currentTimeMillis();

    // update statistics
    long endTime = System.currentTimeMillis();
    long libmsatTime = (libmsatEndTime - libmsatStartTime) - func.getTotalTime();
    long msatTime = (endTime - startTime) - func.getTotalTime();
    stats.abstractionMaxMathsatTime =
      Math.max(msatTime, stats.abstractionMaxMathsatTime);
    stats.abstractionMaxBddTime =
      Math.max(func.getTotalTime(), stats.abstractionMaxBddTime);
    stats.abstractionMathsatTime += msatTime;
    stats.abstractionBddTime += func.getTotalTime();
    stats.abstractionMathsatSolveTime += libmsatTime;
    stats.abstractionMaxMathsatSolveTime =
      Math.max(libmsatTime, stats.abstractionMaxMathsatSolveTime);

    AbstractFormula ret = null;
    if (numModels == -2) {
      ret = amgr.makeTrue();
    } else {
      ret = func.getResult();
    }
    if (useCache) {
      booleanAbstractionCache.put(key, ret);
    }
    return ret;
  }

  // cartesian abstraction
  protected AbstractFormula buildCartesianAbstraction(
      SymbolicFormulaManager mgr, PredicateAbstractionAbstractElement e,
      PredicateAbstractionAbstractElement succ, CFAEdge edge,
      Collection<Predicate> predicates) {
    long startTime = System.currentTimeMillis();

    MathsatSymbolicFormulaManager mmgr = (MathsatSymbolicFormulaManager)mgr;

    thmProver.init(TheoremProver.CARTESIAN_ABSTRACTION);

//    if (isFunctionExit(e)) {
      // we have to take the context before the function call
      // into account, otherwise we are not building the right
      // abstraction!
//      assert(false); // TODO
//    if (CPAMain.cpaConfig.getBooleanValue(
//    "cpas.symbpredabs.refinement.addWellScopedPredicates")) {
//    // but only if we are adding well-scoped predicates, otherwise
//    // this should not be necessary
//    AbstractFormula ctx = e.topContextAbstraction();
//    MathsatSymbolicFormula fctx =
//    (MathsatSymbolicFormula)mmgr.instantiate(
//    toConcrete(mmgr, ctx), null);
//    fabs = (MathsatSymbolicFormula)mmgr.makeAnd(fabs, fctx);

//    CPAMain.logManager.log(Level.ALL, "DEBUG_3",
//    "TAKING CALLING CONTEXT INTO ACCOUNT: ", fctx);
//    } else {
//    CPAMain.logManager.log(Level.ALL, "DEBUG_3",
//    "NOT TAKING CALLING CONTEXT INTO ACCOUNT,",
//    "as we are not using well-scoped predicates");
//    }
//    }

    Pair<SymbolicFormula, SSAMap> pc =
      buildConcreteFormula(mmgr, e, succ, edge, false);
    SymbolicFormula f = pc.getFirst();
    SSAMap ssa = pc.getSecond();

    f = mmgr.replaceAssignments(pc.getFirst());
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

    if (useBitwiseAxioms) {
      SymbolicFormula bitwiseAxioms = mmgr.getBitwiseAxioms(f);
      f = mmgr.makeAnd(f, bitwiseAxioms);

      logger.log(Level.ALL, "DEBUG_3", "ADDED BITWISE AXIOMS:", bitwiseAxioms);
    }

    long solveStartTime = System.currentTimeMillis();

    if (!skipFeasibilityCheck) {
      ++stats.abstractionNumMathsatQueries;
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
      ++stats.abstractionNumCachedQueries;
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
        ++stats.abstractionNumCachedQueries;
      } else {
        Pair<? extends SymbolicFormula, ? extends SymbolicFormula> pi =
          getPredicateVarAndAtom(p);

        // update the SSA map, by instantiating all the uninstantiated
        // variables that occur in the predicates definitions
        // (at index 1)
        predvars.clear();
        predlvals.clear();
        mmgr.collectVarNames(pi.getSecond(),
            predvars, predlvals);
        for (String var : predvars) {
          if (ssa.getIndex(var) < 0) {
            ssa.setIndex(var, 1);
          }
        }
        for (Pair<String, SymbolicFormula[]> pp : predlvals) {
          SymbolicFormula[] args =
            smgr.getInstantiatedAt(pp.getSecond(), ssa,
                predLvalsCache);
          if (ssa.getIndex(pp.getFirst(), args) < 0) {
            ssa.setIndex(pp.getFirst(), args, 1);
          }
        }


        logger.log(Level.ALL, "DEBUG_1",
            "CHECKING VALUE OF PREDICATE: ", pi.getFirst());

        // instantiate the definition of the predicate
        SymbolicFormula predTrue =  mmgr.instantiate(pi.getSecond(), ssa);
        SymbolicFormula predFalse = smgr.makeNot(predTrue);

        boolean isTrue = false, isFalse = false;
        // check whether this predicate has a truth value in the next
        // state

        ++stats.abstractionNumMathsatQueries;
        if (thmProver.isUnsat(predFalse)) {
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
          ++stats.abstractionNumMathsatQueries;
          if (thmProver.isUnsat(predTrue)) {
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

  // counterexample analysis
  @Override
  public CounterexampleTraceInfo buildCounterexampleTrace(
      Pair<ARTElement, CFAEdge>[] pathArray) {
    assert(pathArray.length > 1);
    long startTime = System.currentTimeMillis();
    stats.numCallsCexAnalysis++;

    // create the DAG formula corresponding to the abstract trace. We create
    // n formulas, one per interpolation group
    long extTimeStart = System.currentTimeMillis();
    ConcretePath concPath = null;
    try {
      concPath = buildConcretePath(smgr, pathArray);
    } catch (UnrecognizedCFAEdgeException e1) {
      logger.logException(Level.SEVERE, e1, "");
      System.exit(1);
    }
    long extTimeEnd = System.currentTimeMillis();
    stats.termBuildTime += extTimeEnd - extTimeStart;

    Vector<SymbolicFormula> f = concPath.path;

    logger.log(Level.ALL, "DEBUG_3",
    "Checking feasibility of abstract trace");

    if (shortestTrace && getUsefulBlocks) {
      long gubStart = System.currentTimeMillis();
      f = getUsefulBlocks(smgr, f,
          useSuffix, useZigZag, false);
      long gubEnd = System.currentTimeMillis();
      stats.cexAnalysisGetUsefulBlocksTime += gubEnd - gubStart;
      stats.cexAnalysisGetUsefulBlocksMaxTime = Math.max(
          stats.cexAnalysisGetUsefulBlocksMaxTime, gubEnd - gubStart);
      // set shortestTrace to false, so we perform only one final call
      // to msat_solve
      shortestTrace = false;
    }

    // now f is the DAG formula which is satisfiable iff there is a
    // concrete counterexample
    //
    // create a working environment
    itpProver.init();

    List<T> itpGroupsIds = new ArrayList<T>(f.size());
    for (int i = 0; i < f.size(); i++) {
      itpGroupsIds.add(null);
    }

    int res = -1;
    long msatSolveTimeStart = System.currentTimeMillis();
    for (int i = useSuffix ? f.size()-1 : 0;
    useSuffix ? i >= 0 : i < f.size();
    i = (useSuffix ? i-1 : i+1)) {
      SymbolicFormula cur = f.elementAt(i);
      itpGroupsIds.set(i, itpProver.addFormula(cur));

      logger.log(Level.ALL, "DEBUG_1",
          "Asserting formula: ", cur);

      boolean doCheckHere = !cur.isTrue();

      // if shortestTrace is true, we try to find the minimal infeasible
      // prefix of the trace
      if (shortestTrace && doCheckHere) {
        if (itpProver.isUnsat()) {
          res = 0;
          logger.log(Level.ALL, "DEBUG_1",
              "TRACE INCONSISTENT AFTER group: ", i);
          break;
        } else {
          res = 1;
        }
      } else {
        res = -1;
      }
    }
    assert itpGroupsIds.size() == f.size();
    assert !itpGroupsIds.contains(null); // has to be filled completely

    // and check satisfiability
    boolean unsat = false;
    if (!shortestTrace || res == -1) {
      unsat = itpProver.isUnsat();
    } else {
      unsat = (res == 0);
    }
    long msatSolveTimeEnd = System.currentTimeMillis();
    long msatSolveTime = msatSolveTimeEnd - msatSolveTimeStart;

    CounterexampleTraceInfo info = null;

    if (unsat) {
      Set<Predicate> allPreds = null;
      if (useBlastWay) allPreds = new HashSet<Predicate>();
      int firstIndexBlastWay = -1, lastIndexBlastWay = -1;

      // the counterexample is spurious. Extract the predicates from
      // the interpolants
      info = new CounterexampleTraceInfo(true);
      // how to partition the trace into (A, B) depends on whether
      // there are function calls involved or not: in general, A
      // is the trace from the entry point of the current function
      // to the current point, and B is everything else. To implement
      // this, we keep track of which function we are currently in.
      Stack<Integer> entryPoints = new Stack<Integer>();
      entryPoints.push(0);
      for (int i = 1; i < f.size(); ++i) {
        int start_of_a = entryPoints.peek();
        if (!wellScopedPredicates) {
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

        if (firstIndexBlastWay < 0 && !itp.isTrue()) {
          firstIndexBlastWay = i-1;
        }
        if (lastIndexBlastWay < 0 && itp.isFalse()) {
          lastIndexBlastWay = i;
        }

//        CPAMain.logManager.log(Level.ALL, "DEBUG_1",
//            "Got interpolant(", i, "): ",
//            itp, " LOCATION: ",
//            ((PredicateAbstractionAbstractElement)
//                abstarr[i-1]).getLocation());

        extTimeStart = System.currentTimeMillis();
        Collection<SymbolicFormula> atoms = smgr.extractAtoms(
            itp, true, splitItpAtoms, nonAtomicPredicates);
        Set<Predicate> preds = buildPredicates(atoms);

        extTimeEnd = System.currentTimeMillis();
        stats.predicateExtractionTime += extTimeEnd - extTimeStart;

        if (useBlastWay) {
          allPreds.addAll(preds);
        } else {
          if (addPredicatesGlobally) {
            for (Pair<ARTElement, CFAEdge> pair : pathArray) {
              ARTElement s = pair.getFirst();
              info.addPredicatesForRefinement(s, preds);
            }
          } else {
            ARTElement s1 =
              pathArray[i-1].getFirst();
            info.addPredicatesForRefinement(s1, preds);
          }
        }

        // If we are entering or exiting a function, update the stack
        // of entry points
//        PredicateAbstractionAbstractElement e = pathArray[i].getFirst();
//        if (isFunctionEntry(e)) {
//          CPAMain.logManager.log(Level.ALL, "DEBUG_3",
//              "Pushing entry point, function: ",
//              e.getLocation().getFunctionName());
//          entryPoints.push(i);
//        }
//        if (isFunctionExit(e)) {
//          CPAMain.logManager.log(Level.ALL, "DEBUG_3",
//              "Popping entry point, returning from function: ",
//              e.getLocation().getFunctionName());
//          entryPoints.pop();
//        }
      }
      if (useBlastWay) {
        assert(firstIndexBlastWay >= 0);
        //assert(lastIndexBlastWay >= 0);

        if (lastIndexBlastWay == -1) {
          lastIndexBlastWay = f.size();
        }

        for (int i = firstIndexBlastWay; i < lastIndexBlastWay; ++i) {
          ARTElement s1 = pathArray[i].getFirst();
          info.addPredicatesForRefinement(s1, allPreds);
        }
      }
    } else {

      // this is a real bug, notify the user
      info = new CounterexampleTraceInfo(false);
//      ConcreteTraceFunctionCalls cf = new ConcreteTraceFunctionCalls();
//      for (PredicateAbstractionAbstractElement e : abstractTrace) {
//        cf.add(e.getLocationNode().getFunctionName());
//      }
//      info.setConcreteTrace(cf);
      // TODO - reconstruct counterexample
      // For now, we dump the asserted formula to a user-specified file
      dumpFormulasToFile(f, msatCexFile);
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

  // counterexample analysis
  // compared to buildCounterexamplTrace2 it also returns the index of the
  // last reachable element
  // TODO fix code not to use cpas.symbpredabs.shortestCexTraceUseSuffix (= true)
//  public Pair<CounterexampleTraceInfo, Integer> buildCounterexampleTrace2(
//      SymbolicFormulaManager mgr,
//      Deque<PredicateAbstractionAbstractElement> abstractTrace) {
//    assert(abstractTrace.size() > 1);
//    long startTime = System.currentTimeMillis();
//    stats.numCallsCexAnalysis++;
//
//    // create the DAG formula corresponding to the abstract trace. We create
//    // n formulas, one per interpolation group
//    long extTimeStart = System.currentTimeMillis();
//    AbstractElementWithLocation[] abstarr =
//      abstractTrace.toArray(new AbstractElementWithLocation[0]);
//    ConcretePath concPath = null;
//    try {
//      concPath = buildConcretePath(mgr, abstarr);
//    } catch (UnrecognizedCFAEdgeException e1) {
//      CPAMain.logManager.logException(Level.SEVERE, e, "");
//      System.exit(1);
//    }
//    long extTimeEnd = System.currentTimeMillis();
//    stats.termBuildTime += extTimeEnd - extTimeStart;
//
//    MathsatSymbolicFormulaManager mmgr = (MathsatSymbolicFormulaManager)mgr;
//    Vector<SymbolicFormula> f = concPath.path;
//    boolean theoryCombinationNeeded = concPath.theoryCombinationNeeded;
//
//    boolean shortestTrace = CPAMain.cpaConfig.getBooleanValue(
//        "cpas.symbpredabs.shortestCexTrace");
//    boolean suffixTrace = CPAMain.cpaConfig.getBooleanValue(
//    "cpas.symbpredabs.shortestCexTraceUseSuffix");
//    boolean useZigZag = CPAMain.cpaConfig.getBooleanValue(
//    "cpas.symbpredabs.shortestCexTraceZigZag");
//
//    CPAMain.logManager.log(Level.ALL, "DEBUG_3",
//    "Checking feasibility of abstract trace");
//
//    if (shortestTrace && CPAMain.cpaConfig.getBooleanValue(
//    "cpas.symbpredabs.explicit.getUsefulBlocks")) {
//      long gubStart = System.currentTimeMillis();
//      f = getUsefulBlocks(mmgr, f, theoryCombinationNeeded,
//          suffixTrace, useZigZag, false);
//      long gubEnd = System.currentTimeMillis();
//      stats.cexAnalysisGetUsefulBlocksTime += gubEnd - gubStart;
//      stats.cexAnalysisGetUsefulBlocksMaxTime = Math.max(
//          stats.cexAnalysisGetUsefulBlocksMaxTime, gubEnd - gubStart);
//      // set shortestTrace to false, so we perform only one final call
//      // to msat_solve
//      shortestTrace = false;
//    }
//
//    // now f is the DAG formula which is satisfiable iff there is a
//    // concrete counterexample
//    //
//    // create a working environment
//    long msatEnv = mmgr.getMsatEnv();
//    itpProver.init();
//
//    int res = -1;
//    long msatSolveTimeStart = System.currentTimeMillis();
//    for (int i = suffixTrace ? f.size()-1 : 0;
//    suffixTrace ? i >= 0 : i < f.size();
//    i = (suffixTrace ? i-1 : i+1)) {
//      SymbolicFormula cur = f.elementAt(i);
//      itpProver.addFormula(cur);
//
//      CPAMain.logManager.log(Level.ALL, "DEBUG_1",
//          "Asserting formula: ", cur);
//
//      boolean doCheckHere = !cur.isTrue();
//
//      // if shortestTrace is true, we try to find the minimal infeasible
//      // prefix of the trace
//      if (shortestTrace && doCheckHere) {
//        if (itpProver.isUnsat()) {
//          res = 0;
//          CPAMain.logManager.log(Level.ALL, "DEBUG_1",
//              "TRACE INCONSISTENT AFTER group: ", i);
//          break;
//        } else {
//          res = 1;
//        }
//      } else {
//        res = -1;
//      }
//    }
//    // and check satisfiability
//    boolean unsat = false;
//    if (!shortestTrace || res == -1) {
//      unsat = itpProver.isUnsat();
//    } else {
//      unsat = (res == 0);
//    }
//    long msatSolveTimeEnd = System.currentTimeMillis();
//    long msatSolveTime = msatSolveTimeEnd - msatSolveTimeStart;
//
//    CounterexampleTraceInfo info = null;
//
//    Integer lIndex = -1;
//
//    if (unsat) {
//      boolean useBlastWay = CPAMain.cpaConfig.getBooleanValue(
//          "cpas.symbpredabs.refinement.useBlastWay");
//      Set<Predicate> allPreds = null;
//      if (useBlastWay) allPreds = new HashSet<Predicate>();
//      int firstIndexBlastWay = -1, lastIndexBlastWay = -1;
//
//      // the counterexample is spurious. Extract the predicates from
//      // the interpolants
//      info = new CounterexampleTraceInfo(true);
//      boolean splitItpAtoms = CPAMain.cpaConfig.getBooleanValue(
//      "cpas.symbpredabs.refinement.splitItpAtoms");
//      // how to partition the trace into (A, B) depends on whether
//      // there are function calls involved or not: in general, A
//      // is the trace from the entry point of the current function
//      // to the current point, and B is everything else. To implement
//      // this, we keep track of which function we are currently in.
//      Stack<Integer> entryPoints = new Stack<Integer>();
//      entryPoints.push(0);
//      for (int i = 1; i < f.size(); ++i) {
//        int start_of_a = entryPoints.peek();
//        if (!CPAMain.cpaConfig.getBooleanValue(
//            "cpas.symbpredabs.refinement.addWellScopedPredicates")) {
//          // if we don't want "well-scoped" predicates, we always
//          // cut from the beginning
//          start_of_a = 0;
//        }
//
//        int sz = i - start_of_a;
//        Vector<SymbolicFormula> formulasOfA =
//          new Vector<SymbolicFormula>();
//        formulasOfA.ensureCapacity(sz);
//        for (int j = 0; j < sz; ++j) {
//          formulasOfA.add(f.elementAt(j+start_of_a));
//        }
//        msatSolveTimeStart = System.currentTimeMillis();
//        SymbolicFormula itp = itpProver.getInterpolant(formulasOfA);
//        msatSolveTimeEnd = System.currentTimeMillis();
//        msatSolveTime += msatSolveTimeEnd - msatSolveTimeStart;
//
//        if (firstIndexBlastWay < 0 && !itp.isTrue()) {
//          firstIndexBlastWay = i-1;
//        }
//        if (lastIndexBlastWay < 0 && itp.isFalse()) {
//          lastIndexBlastWay = i;
//        }
//
////        CPAMain.logManager.log(Level.ALL, "DEBUG_1",
////            "Got interpolant(", i, "): ",
////            itp, " LOCATION: ",
////            ((PredicateAbstractionAbstractElement)
////                abstarr[i-1]).getLocation());
//
//        boolean nonAtomic = CPAMain.cpaConfig.getBooleanValue(
//            "cpas.symbpredabs.abstraction.explicit." +
//        "nonAtomicPredicates");
//        extTimeStart = System.currentTimeMillis();
//        Collection<SymbolicFormula> atoms = mmgr.extractAtoms(
//            itp, true, splitItpAtoms, nonAtomic);
//        Set<Predicate> preds =
//          buildPredicates(msatEnv, atoms);
//
//        extTimeEnd = System.currentTimeMillis();
//        stats.predicateExtractionTime += extTimeEnd - extTimeStart;
//
//        if (useBlastWay) {
//          allPreds.addAll(preds);
//        } else {
//          if (CPAMain.cpaConfig.getBooleanValue(
//              "cpas.symbpredabs.refinement.addPredicatesGlobally")) {
//            for (Object o : abstarr) {
//              PredicateAbstractionAbstractElement s =
//                (PredicateAbstractionAbstractElement)o;
//              info.addPredicatesForRefinement(s, preds);
//            }
//          } else {
//            PredicateAbstractionAbstractElement s1 =
//              (PredicateAbstractionAbstractElement)abstarr[i-1];
//            info.addPredicatesForRefinement(s1, preds);
//          }
//        }
//
//        // If we are entering or exiting a function, update the stack
//        // of entry points
//        PredicateAbstractionAbstractElement e = (PredicateAbstractionAbstractElement)abstarr[i];
////        if (isFunctionEntry(e)) {
////          CPAMain.logManager.log(Level.ALL, "DEBUG_3",
////              "Pushing entry point, function: ",
////              e.getLocation().getFunctionName());
////          entryPoints.push(i);
////        }
////        if (isFunctionExit(e)) {
////          CPAMain.logManager.log(Level.ALL, "DEBUG_3",
////              "Popping entry point, returning from function: ",
////              e.getLocation().getFunctionName());
////          entryPoints.pop();
////        }
//      }
//      if (useBlastWay) {
//        assert(firstIndexBlastWay >= 0);
//        //assert(lastIndexBlastWay >= 0);
//
//        if (lastIndexBlastWay == -1) {
//          lastIndexBlastWay = f.size();
//        }
//
//        lIndex = lastIndexBlastWay;
//        //lIndex = firstIndexBlastWay;
//
//        for (int i = firstIndexBlastWay; i < lastIndexBlastWay; ++i) {
//          PredicateAbstractionAbstractElement s1 =
//            (PredicateAbstractionAbstractElement)abstarr[i];
//          info.addPredicatesForRefinement(s1, allPreds);
//        }
//      }
//    } else {
//      // this is a real bug, notify the user
//      info = new CounterexampleTraceInfo(false);
//      ConcreteTraceFunctionCalls cf = new ConcreteTraceFunctionCalls();
//      for (PredicateAbstractionAbstractElement e : abstractTrace) {
//        cf.add(e.getLocationNode().getFunctionName());
//      }
//      info.setConcreteTrace(cf);
//      // TODO - reconstruct counterexample
//      // For now, we dump the asserted formula to a user-specified file
//      String cexFile = CPAMain.cpaConfig.getProperty("cpas.symbpredabs.refinement.msatCexFile");
//      if (cexFile != null) {
//        String path = CPAMain.cpaConfig.getProperty("output.path") + cexFile;
//        long t = mathsat.api.msat_make_true(msatEnv);
//        for (SymbolicFormula fm : f) {
//          long term = ((MathsatSymbolicFormula)fm).getTerm();
//          t = mathsat.api.msat_make_and(msatEnv, t, term);
//        }
//        String msatRepr = mathsat.api.msat_to_msat(msatEnv, t);
//        try {
//          PrintWriter pw = new PrintWriter(new File(path));
//          pw.println(msatRepr);
//          pw.close();
//        } catch (FileNotFoundException e) {
//          CPAMain.logManager.log(Level.INFO,
//              "Failed to save msat Counterexample to file: ",
//              path);
//        }
//      }
//    }
//
//    itpProver.reset();
//
//    // update stats
//    long endTime = System.currentTimeMillis();
//    long totTime = endTime - startTime;
//    stats.cexAnalysisTime += totTime;
//    stats.cexAnalysisMaxTime = Math.max(totTime, stats.cexAnalysisMaxTime);
//    stats.cexAnalysisMathsatTime += msatSolveTime;
//    stats.cexAnalysisMaxMathsatTime =
//      Math.max(msatSolveTime, stats.cexAnalysisMaxMathsatTime);
//
//    return new Pair<CounterexampleTraceInfo, Integer>(info, lIndex);
//  }

//  protected boolean isFunctionExit(ExplicitAbstractElement e) {
//    return false; // TODO
////  CFANode inner = e.getLocation();
////  return (inner.getNumLeavingEdges() == 1 &&
////  inner.getLeavingEdge(0) instanceof ReturnEdge);
//  }
//
//  protected boolean isFunctionEntry(ExplicitAbstractElement e) {
//    return false; // TODO
////  CFANode inner = e.getLocation();
////  return (inner.getNumEnteringEdges() > 0 &&
////  inner.getEnteringEdge(0).getPredecessor() instanceof
////  FunctionDefinitionNode);
//  }

  private Pair<SymbolicFormula, SSAMap> makeFormula(
      MathsatSymbolicFormulaManager mmgr,
      CFAEdge edge,
      SSAMap ssa) throws UnrecognizedCFAEdgeException {
    stats.makeFormulaCalls++;
    Pair<CFAEdge, SSAMap> key = new Pair<CFAEdge, SSAMap>(edge, ssa);
    if (useCache && makeFormulaCache.containsKey(key)) {
      stats.makeFormulaCacheHits++;
      return makeFormulaCache.get(key);
    }
    Pair<SymbolicFormula, SSAMap> ret = mmgr.makeAnd(mmgr.makeTrue(), edge, ssa);
    if (useCache) {
      makeFormulaCache.put(key, ret);
    }
    return ret;
  }

  @Override
  public Vector<SymbolicFormula> getUsefulBlocks(SymbolicFormulaManager mgr,
      Vector<SymbolicFormula> f,
      boolean suffixTrace, boolean zigZag, boolean setAllTrueIfSat) {
    // try to find a minimal-unsatisfiable-core of the trace (as Blast does)

    thmProver.init(TheoremProver.COUNTEREXAMPLE_ANALYSIS);

    logger.log(Level.ALL, "DEBUG_1", "Calling getUsefulBlocks on path",
        "of length: ", f.size());

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
        int e = f.size()-1;
        int s = 0;
        boolean fromStart = false;
        while (true) {
          int i = fromStart ? s : e;
          if (fromStart) s++;
          else e--;
          fromStart = !fromStart;

          SymbolicFormula t = f.elementAt(i);
          thmProver.push(t);
          ++toPop;
          if (thmProver.isUnsat(trueFormula)) {
            // add this block to the needed ones, and repeat
            needed[i] = t;
            logger.log(Level.ALL, "DEBUG_1",
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
          if (s > e) break;
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
            logger.log(Level.ALL, "DEBUG_1",
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

    logger.log(Level.ALL, "DEBUG_1", "Done getUsefulBlocks");

    return f;
  }

  class ArrayToStringConverter {
    private Object[] arr;
    public ArrayToStringConverter(Object[] a) { arr = a; }
    @Override
    public String toString() {
      StringBuffer buf = new StringBuffer();
      buf.append('[');
      for (Object o : arr) {
        buf.append(o.toString());
        buf.append(", ");
      }
      buf.delete(buf.length()-2, buf.length());
      buf.append(']');
      return buf.toString();
    }
  }

  @Override
  public ConcretePath buildConcretePath(SymbolicFormulaManager mgr,
      Pair<ARTElement, CFAEdge>[] pathArray)
  throws UnrecognizedCFAEdgeException {
    SSAMap ssa = new SSAMap();
    MathsatSymbolicFormulaManager mmgr = (MathsatSymbolicFormulaManager)mgr;

    Vector<SymbolicFormula> f = new Vector<SymbolicFormula>();

    logger.log(Level.ALL, "DEBUG_1", "BUILDING COUNTEREXAMPLE TRACE");
//    logger.log(Level.ALL, "DEBUG_1", "ABSTRACT TRACE:",
//        new ArrayToStringConverter(path));

    //AbstractElementWithLocation cur = path[0];

    for (int i = 1; i < pathArray.length; ++i) {
//      AbstractElementWithLocation e = path[i];
//      CFAEdge found = null;
//      for (int j = 0; j < e.getLocationNode().getNumEnteringEdges(); ++j){
//        CFAEdge edge = e.getLocationNode().getEnteringEdge(j);
//        if (edge.getPredecessor().equals(cur.getLocationNode())) {
//          found = edge;
//          break;
//        }
//      }
//      assert(found != null);
      CFAEdge edge = pathArray[i].getSecond();
      long startTime = System.currentTimeMillis();
      Pair<SymbolicFormula, SSAMap> p = makeFormula(mmgr, edge, ssa);
      long endTime = System.currentTimeMillis();
      stats.extraTime += endTime - startTime;

      SSAMap newssa = null;
      SymbolicFormula fm = null;
      fm = mmgr.replaceAssignments(p.getFirst());
      logger.log(Level.ALL, "DEBUG_3", "INITIAL:", fm,
          " SSA: ", p.getSecond());
      newssa = p.getSecond();
      f.add(fm);
      ssa = newssa;
//      cur = e;
    }
    ConcretePath ret = new ConcretePath(f, ssa);
    return ret;
  }

}
