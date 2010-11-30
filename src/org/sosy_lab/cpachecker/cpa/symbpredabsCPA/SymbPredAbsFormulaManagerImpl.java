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
package org.sosy_lab.cpachecker.cpa.symbpredabsCPA;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.regex.Pattern;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.CEGARAlgorithm;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.ForceStopCPAException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException.Reason;
import org.sosy_lab.cpachecker.util.symbpredabstraction.Abstraction;
import org.sosy_lab.cpachecker.util.symbpredabstraction.CommonFormulaManager;
import org.sosy_lab.cpachecker.util.symbpredabstraction.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.symbpredabstraction.Model;
import org.sosy_lab.cpachecker.util.symbpredabstraction.PathFormula;
import org.sosy_lab.cpachecker.util.symbpredabstraction.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.symbpredabstraction.SSAMap;
import org.sosy_lab.cpachecker.util.symbpredabstraction.Cache.CartesianAbstractionCacheKey;
import org.sosy_lab.cpachecker.util.symbpredabstraction.Cache.FeasibilityCacheKey;
import org.sosy_lab.cpachecker.util.symbpredabstraction.Cache.TimeStampCache;
import org.sosy_lab.cpachecker.util.symbpredabstraction.Model.AssignableTerm;
import org.sosy_lab.cpachecker.util.symbpredabstraction.Model.TermType;
import org.sosy_lab.cpachecker.util.symbpredabstraction.Model.Variable;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.Region;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.RegionManager;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.InterpolatingTheoremProver;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormula;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormulaManager;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.TheoremProver;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.TheoremProver.AllSatResult;

import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;


@Options(prefix="cpas.symbpredabs")
class SymbPredAbsFormulaManagerImpl<T1, T2> extends CommonFormulaManager implements SymbPredAbsFormulaManager {

  static class Stats {
    public int numCallsAbstraction = 0;
    public int numSymbolicAbstractions = 0;
    public int numSatCheckAbstractions = 0;
    public int numCallsAbstractionCached = 0;
    public long abstractionSolveTime = 0;
    public long abstractionMaxSolveTime = 0;
    
    public long abstractionBddTime = 0;
    public long abstractionMaxBddTime = 0;
    public long allSatCount = 0;
    public int maxAllSatCount = 0;

    public final Timer cexAnalysisTimer = new Timer();
    public final Timer cexAnalysisSolverTimer = new Timer();
    public final Timer cexAnalysisGetUsefulBlocksTimer = new Timer();
  }
  
  final Stats stats;

  private final TheoremProver thmProver;
  private final InterpolatingTheoremProver<T1> firstItpProver;
  private final InterpolatingTheoremProver<T2> secondItpProver;

  private static final int MAX_CACHE_SIZE = 100000;

  private static final Pattern PREDICATE_NAME_PATTERN = Pattern.compile(
      "^.*" + PROGRAM_COUNTER_PREDICATE + "(?=\\d+@\\d+$)");

  @Option(name="abstraction.cartesian")
  private boolean cartesianAbstraction = false;

  @Option(name="mathsat.dumpHardAbstractionQueries")
  private boolean dumpHardAbstractions = false;

  @Option(name="explicit.getUsefulBlocks")
  private boolean getUsefulBlocks = false;

  @Option(name="shortestCexTrace")
  private boolean shortestTrace = false;

  @Option(name="refinement.atomicPredicates")
  private boolean atomicPredicates = true;

  @Option(name="refinement.splitItpAtoms")
  private boolean splitItpAtoms = false;

  @Option(name="shortestCexTraceUseSuffix")
  private boolean useSuffix = false;

  @Option(name="shortestCexTraceZigZag")
  private boolean useZigZag = false;

  @Option(name="refinement.addWellScopedPredicates")
  private boolean wellScopedPredicates = false;

  @Option(name="refinement.dumpInterpolationProblems")
  private boolean dumpInterpolationProblems = false;

  @Option(name="formulaDumpFilePattern", type=Option.Type.OUTPUT_FILE)
  private File formulaDumpFile = new File("%s%04d-%s%03d.msat");
  private final String formulaDumpFilePattern; // = formulaDumpFile.getAbsolutePath()
  
  @Option(name="interpolation.timelimit")
  private long itpTimeLimit = 0;

  @Option(name="interpolation.changesolverontimeout")
  private boolean changeItpSolveOTF = false;

  @Option
  private boolean useBitwiseAxioms = false;
  
  @Option(name="refinement.maxRefinementSize")
  private int maxRefinementSize = 0;
  
  private final Map<Pair<SymbolicFormula, Collection<AbstractionPredicate>>, Abstraction> abstractionCache;
  //cache for cartesian abstraction queries. For each predicate, the values
  // are -1: predicate is false, 0: predicate is don't care,
  // 1: predicate is true
  private final TimeStampCache<CartesianAbstractionCacheKey, Byte> cartesianAbstractionCache;
  private final TimeStampCache<FeasibilityCacheKey, Boolean> feasibilityCache;

  public SymbPredAbsFormulaManagerImpl(
      RegionManager pRmgr,
      SymbolicFormulaManager pSmgr,
      TheoremProver pThmProver,
      InterpolatingTheoremProver<T1> pItpProver,
      InterpolatingTheoremProver<T2> pAltItpProver,
      Configuration config,
      LogManager logger) throws InvalidConfigurationException {
    super(pRmgr, pSmgr, config, logger);
    config.inject(this);
    
    if (formulaDumpFile != null) {
      formulaDumpFilePattern = formulaDumpFile.getAbsolutePath();
    } else {
      dumpHardAbstractions = false;
      formulaDumpFilePattern = null;
    }

    stats = new Stats();
    thmProver = pThmProver;
    firstItpProver = pItpProver;
    secondItpProver = pAltItpProver;

    if (wellScopedPredicates) {
      throw new InvalidConfigurationException("wellScopePredicates are currently disabled");
    }
//    if (inlineFunctions && wellScopedPredicates) {
//      logger.log(Level.WARNING, "Well scoped predicates not possible with function inlining, disabling them.");
//      wellScopedPredicates = false;
//    }

    if (useCache) {
      abstractionCache = new HashMap<Pair<SymbolicFormula, Collection<AbstractionPredicate>>, Abstraction>();
    } else {
      abstractionCache = null;
    }
    if (useCache && cartesianAbstraction) {
      cartesianAbstractionCache = new TimeStampCache<CartesianAbstractionCacheKey, Byte>(MAX_CACHE_SIZE);
      feasibilityCache = new TimeStampCache<FeasibilityCacheKey, Boolean>(MAX_CACHE_SIZE);
    } else {
      cartesianAbstractionCache = null;
      feasibilityCache = null;
    }
  }

  /**
   * Abstract post operation.
   */
  @Override
  public Abstraction buildAbstraction(
      Abstraction abstractionFormula, PathFormula pathFormula,
      Collection<AbstractionPredicate> predicates) {

    stats.numCallsAbstraction++;

    if (predicates.isEmpty()) {
      stats.numSymbolicAbstractions++;
      return new Abstraction(rmgr.makeTrue(), smgr.makeTrue(), pathFormula.getSymbolicFormula());
    }

    logger.log(Level.ALL, "Old abstraction:", abstractionFormula);
    logger.log(Level.ALL, "Path formula:", pathFormula);
    logger.log(Level.ALL, "Predicates:", predicates);
    
    SymbolicFormula absFormula = abstractionFormula.asSymbolicFormula();
    SymbolicFormula symbFormula = buildSymbolicFormula(pathFormula.getSymbolicFormula());
    SymbolicFormula f = smgr.makeAnd(absFormula, symbFormula);
    
    // caching
    Pair<SymbolicFormula, Collection<AbstractionPredicate>> absKey = null;
    if (useCache) {
      absKey = new Pair<SymbolicFormula, Collection<AbstractionPredicate>>(f, predicates);
      Abstraction result = abstractionCache.get(absKey);

      if (result != null) {
        // create new abstraction object to have a unique abstraction id
        result = new Abstraction(result.asRegion(), result.asSymbolicFormula(), result.getBlockFormula());
        logger.log(Level.ALL, "Abstraction was cached, result is", result);
        stats.numCallsAbstractionCached++;
        return result;
      }
    }
    
    Region abs;
    if (cartesianAbstraction) {
      abs = buildCartesianAbstraction(f, pathFormula.getSsa(), predicates);
    } else {
      abs = buildBooleanAbstraction(f, pathFormula.getSsa(), predicates);
    }
    
    SymbolicFormula symbolicAbs = smgr.instantiate(toConcrete(abs), pathFormula.getSsa());
    Abstraction result = new Abstraction(abs, symbolicAbs, pathFormula.getSymbolicFormula());

    if (useCache) {
      abstractionCache.put(absKey, result);
    }
    
    return result;
  }

  private Region buildCartesianAbstraction(SymbolicFormula f, SSAMap ssa,
      Collection<AbstractionPredicate> predicates) {
    
    byte[] predVals = null;
    final byte NO_VALUE = -2;
    if (useCache) {
      predVals = new byte[predicates.size()];
      int predIndex = -1;
      for (AbstractionPredicate p : predicates) {
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
          // abstract post leads to false, we can return immediately
          return rmgr.makeFalse();
        }
      }
    }

    Timer solveTimer = new Timer();
    solveTimer.start();

    thmProver.init();
    try {

      if (!skipFeasibilityCheck) {
        //++stats.abstractionNumMathsatQueries;
        boolean unsat = thmProver.isUnsat(f);
        if (useCache) {
          FeasibilityCacheKey key = new FeasibilityCacheKey(f);
          feasibilityCache.put(key, !unsat);
        }
        if (unsat) {
          return rmgr.makeFalse();
        }
      } else {
        //++stats.abstractionNumCachedQueries;
      }

      thmProver.push(f);
      try {
        Timer totBddTimer = new Timer();

        Region absbdd = rmgr.makeTrue();

        // check whether each of the predicate is implied in the next state...

        int predIndex = -1;
        for (AbstractionPredicate p : predicates) {
          ++predIndex;
          if (useCache && predVals[predIndex] != NO_VALUE) {
            
            totBddTimer.start();
            Region v = p.getAbstractVariable();
            if (predVals[predIndex] == -1) { // pred is false
              v = rmgr.makeNot(v);
              absbdd = rmgr.makeAnd(absbdd, v);
            } else if (predVals[predIndex] == 1) { // pred is true
              absbdd = rmgr.makeAnd(absbdd, v);
            }
            totBddTimer.stop();
            
            //++stats.abstractionNumCachedQueries;
          } else {            
            logger.log(Level.ALL, "DEBUG_1",
                "CHECKING VALUE OF PREDICATE: ", p.getSymbolicAtom());

            // instantiate the definition of the predicate
            SymbolicFormula predTrue = smgr.instantiate(p.getSymbolicAtom(), ssa);
            SymbolicFormula predFalse = smgr.makeNot(predTrue);

            // check whether this predicate has a truth value in the next
            // state
            byte predVal = 0; // pred is neither true nor false

            //++stats.abstractionNumMathsatQueries;
            boolean isTrue = thmProver.isUnsat(predFalse);

            if (isTrue) {
              totBddTimer.start();
              Region v = p.getAbstractVariable();
              absbdd = rmgr.makeAnd(absbdd, v);
              totBddTimer.stop();

              predVal = 1;
            } else {
              // check whether it's false...
              //++stats.abstractionNumMathsatQueries;
              boolean isFalse = thmProver.isUnsat(predTrue);

              if (isFalse) {
                totBddTimer.start();
                Region v = p.getAbstractVariable();
                v = rmgr.makeNot(v);
                absbdd = rmgr.makeAnd(absbdd, v);
                totBddTimer.stop();

                predVal = -1;
              }
            }

            if (useCache) {
              CartesianAbstractionCacheKey key =
                new CartesianAbstractionCacheKey(f, p);
              cartesianAbstractionCache.put(key, predVal);
            }
          }
        }     
        solveTimer.stop();

        // update statistics
        
        long solveTime = solveTimer.getSumTime() - totBddTimer.getSumTime();
        
        stats.abstractionMaxBddTime =
          Math.max(totBddTimer.getSumTime(), stats.abstractionMaxBddTime);
        stats.abstractionBddTime += totBddTimer.getSumTime();
        
        stats.abstractionSolveTime += solveTime;
        stats.abstractionMaxSolveTime =
          Math.max(solveTime, stats.abstractionMaxSolveTime);

        return absbdd;

      } finally {
        thmProver.pop();
      }

    } finally {
      thmProver.reset();
    }
  }

  private SymbolicFormula buildSymbolicFormula(SymbolicFormula symbFormula) {

    if (useBitwiseAxioms) {
      SymbolicFormula bitwiseAxioms = smgr.getBitwiseAxioms(symbFormula);
      if (!bitwiseAxioms.isTrue()) {
        symbFormula = smgr.makeAnd(symbFormula, bitwiseAxioms);

        logger.log(Level.ALL, "DEBUG_3", "ADDED BITWISE AXIOMS:", bitwiseAxioms);
      }
    }
    
    return symbFormula;
  }

  /**
   * Checks if (a1 & p1) => a2
   */
  @Override
  public boolean checkCoverage(Abstraction a1, PathFormula p1, Abstraction a2) {
    SymbolicFormula absFormula = a1.asSymbolicFormula();
    SymbolicFormula symbFormula = buildSymbolicFormula(p1.getSymbolicFormula()); 
    SymbolicFormula a = smgr.makeAnd(absFormula, symbFormula);

    SymbolicFormula b = smgr.instantiate(a2.asSymbolicFormula(), p1.getSsa());

    SymbolicFormula toCheck = smgr.makeAnd(a, smgr.makeNot(b));

    thmProver.init();
    try {
      return thmProver.isUnsat(toCheck);
    } finally {
      thmProver.reset();
    }
  }

  private Region buildBooleanAbstraction(SymbolicFormula f, SSAMap ssa,
      Collection<AbstractionPredicate> predicates) {

    // first, create the new formula corresponding to
    // (symbFormula & edges from e to succ)
    // TODO - at the moment, we assume that all the edges connecting e and
    // succ have no statement or assertion attached (i.e. they are just
    // return edges or gotos). This might need to change in the future!!
    // (So, for now we don't need to to anything...)

    // build the definition of the predicates, and instantiate them
    // also collect all predicate variables so that the solver knows for which
    // variables we want to have the satisfying assignments
    SymbolicFormula predDef = smgr.makeTrue();
    List<SymbolicFormula> predVars = new ArrayList<SymbolicFormula>(predicates.size());

    for (AbstractionPredicate p : predicates) {
      // get propositional variable and definition of predicate
      SymbolicFormula var = p.getSymbolicVariable();
      SymbolicFormula def = p.getSymbolicAtom();
      if (def.isFalse()) {
        continue;
      }
      def = smgr.instantiate(def, ssa);
      
      // build the formula (var <-> def) and add it to the list of definitions
      SymbolicFormula equiv = smgr.makeEquivalence(var, def);
      predDef = smgr.makeAnd(predDef, equiv);

      predVars.add(var);
    }
    if (predVars.isEmpty()) {
      stats.numSatCheckAbstractions++;
    }

    // the formula is (abstractionFormula & pathFormula & predDef)
    SymbolicFormula fm = smgr.makeAnd(f, predDef);

    logger.log(Level.ALL, "COMPUTING ALL-SMT ON FORMULA: ", fm);

    final Timer solveTimer = new Timer();
    solveTimer.start();
    AllSatResult allSatResult = thmProver.allSat(fm, predVars, this, rmgr);
    solveTimer.stop();
    
    // update statistics
    int numModels = allSatResult.getCount();
    if (numModels < Integer.MAX_VALUE) {
      stats.maxAllSatCount = Math.max(numModels, stats.maxAllSatCount);
      stats.allSatCount += numModels;
    }
    
    long bddTime   = allSatResult.getTotalTime();
    long solveTime = solveTimer.getSumTime() - bddTime;

    stats.abstractionSolveTime += solveTime;
    stats.abstractionBddTime   += bddTime;

    stats.abstractionMaxBddTime =
      Math.max(bddTime, stats.abstractionMaxBddTime);
    stats.abstractionMaxSolveTime =
      Math.max(solveTime, stats.abstractionMaxSolveTime);

    // TODO dump hard abst
    if (solveTime > 10000 && dumpHardAbstractions) {
      // we want to dump "hard" problems...
      String dumpFile = String.format(formulaDumpFilePattern,
                               "abstraction", stats.numCallsAbstraction, "input", 0);
      dumpFormulaToFile(f, new File(dumpFile));

      dumpFile = String.format(formulaDumpFilePattern,
                               "abstraction", stats.numCallsAbstraction, "predDef", 0);
      dumpFormulaToFile(predDef, new File(dumpFile));

      dumpFile = String.format(formulaDumpFilePattern,
                               "abstraction", stats.numCallsAbstraction, "predVars", 0);
      printFormulasToFile(predVars, new File(dumpFile));
    }

    Region result = allSatResult.getResult();
    logger.log(Level.ALL, "Abstraction computed, result is", result);
    return result;
  }

  /**
   * Checks if an abstraction formula and a pathFormula are unsatisfiable.
   * @param pAbstractionFormula the abstraction formula
   * @param pPathFormula the path formula
   * @return unsat(pAbstractionFormula & pPathFormula)
   */
  @Override
  public boolean unsat(Abstraction abstractionFormula, PathFormula pathFormula) {
    SymbolicFormula absFormula = abstractionFormula.asSymbolicFormula();
    SymbolicFormula symbFormula = buildSymbolicFormula(pathFormula.getSymbolicFormula());
    SymbolicFormula f = smgr.makeAnd(absFormula, symbFormula);
    logger.log(Level.ALL, "Checking satisfiability of formula", f);

    thmProver.init();
    try {
      return thmProver.isUnsat(f);
    } finally {
      thmProver.reset();
    }
  }

  /**
   * Counterexample analysis and predicate discovery.
   * @param pAbstractTrace abstract trace of the error path
   * @param pItpProver interpolation solver used
   * @return counterexample info with predicated information
   * @throws CPAException
   */
  private <T> CounterexampleTraceInfo buildCounterexampleTraceWithSpecifiedItp(
      ArrayList<SymbPredAbsAbstractElement> pAbstractTrace, InterpolatingTheoremProver<T> pItpProver) throws CPAException {
    
    stats.cexAnalysisTimer.start();

    logger.log(Level.FINEST, "Building counterexample trace");

    List<SymbolicFormula> f = getFormulasForTrace(pAbstractTrace);

    if (useBitwiseAxioms) {
      SymbolicFormula bitwiseAxioms = smgr.makeTrue();
  
      for (SymbolicFormula fm : f) {
        SymbolicFormula a = smgr.getBitwiseAxioms(fm);
        if (!a.isTrue()) {
          bitwiseAxioms = smgr.makeAnd(bitwiseAxioms, a);  
        }
      }
  
      if (!bitwiseAxioms.isTrue()) {
        logger.log(Level.ALL, "DEBUG_3", "ADDING BITWISE AXIOMS TO THE",
            "LAST GROUP: ", bitwiseAxioms);
        int lastIndex = f.size()-1;
        f.set(lastIndex, smgr.makeAnd(f.get(lastIndex), bitwiseAxioms));
      }
    }

    f = Collections.unmodifiableList(f);

    logger.log(Level.ALL, "Counterexample trace formulas:", f);

    if (maxRefinementSize > 0) {
      SymbolicFormula cex = smgr.makeTrue();
      for (SymbolicFormula formula : f) {
        cex = smgr.makeAnd(cex, formula);
      }
      int size = smgr.dumpFormula(cex).length();
      if (size > maxRefinementSize) {
        logger.log(Level.FINEST, "Skipping refinement because input formula is", size, "bytes large.");
        throw new RefinementFailedException(Reason.TooMuchUnrolling, null);
      }
    }
    
    logger.log(Level.FINEST, "Checking feasibility of counterexample trace");

    // now f is the DAG formula which is satisfiable iff there is a
    // concrete counterexample

    // create a working environment
    pItpProver.init();

    stats.cexAnalysisSolverTimer.start();

    if (shortestTrace && getUsefulBlocks) {
      f = Collections.unmodifiableList(getUsefulBlocks(f, useSuffix, useZigZag));
    }

    if (dumpInterpolationProblems) {
      int k = 0;
      for (SymbolicFormula formula : f) {
        String dumpFile = String.format(formulaDumpFilePattern,
                    "interpolation", stats.cexAnalysisTimer.getNumberOfIntervals(), "formula", k++);
        dumpFormulaToFile(formula, new File(dumpFile));
      }
    }

    List<T> itpGroupsIds = new ArrayList<T>(f.size());
    for (int i = 0; i < f.size(); i++) {
      itpGroupsIds.add(null);
    }

    boolean spurious;
    if (getUsefulBlocks || !shortestTrace) {
      // check all formulas in f at once

      for (int i = useSuffix ? f.size()-1 : 0;
      useSuffix ? i >= 0 : i < f.size(); i += useSuffix ? -1 : 1) {

        itpGroupsIds.set(i, pItpProver.addFormula(f.get(i)));
      }
      spurious = pItpProver.isUnsat();

    } else {
      spurious = checkInfeasabilityOfShortestTrace(f, itpGroupsIds, pItpProver);
    }
    assert itpGroupsIds.size() == f.size();
    assert !itpGroupsIds.contains(null); // has to be filled completely

    stats.cexAnalysisSolverTimer.stop();

    logger.log(Level.FINEST, "Counterexample trace is", (spurious ? "infeasible" : "feasible"));

    CounterexampleTraceInfo info;

    if (spurious) {
      info = new CounterexampleTraceInfo();

      // the counterexample is spurious. Extract the predicates from
      // the interpolants

      // how to partition the trace into (A, B) depends on whether
      // there are function calls involved or not: in general, A
      // is the trace from the entry point of the current function
      // to the current point, and B is everything else. To implement
      // this, we keep track of which function we are currently in.
      // if we don't want "well-scoped" predicates, A always starts at the beginning
      Deque<Integer> entryPoints = null;
      if (wellScopedPredicates) {
        entryPoints = new ArrayDeque<Integer>();
        entryPoints.push(0);
      }
      boolean foundPredicates = false;

      for (int i = 0; i < f.size()-1; ++i) {
        // last iteration is left out because B would be empty
        final int start_of_a = (wellScopedPredicates ? entryPoints.peek() : 0);
        SymbPredAbsAbstractElement e = pAbstractTrace.get(i);

        logger.log(Level.ALL, "Looking for interpolant for formulas from",
            start_of_a, "to", i);

        stats.cexAnalysisSolverTimer.start();
        SymbolicFormula itp = pItpProver.getInterpolant(itpGroupsIds.subList(start_of_a, i+1));
        stats.cexAnalysisSolverTimer.stop();
        
        if (dumpInterpolationProblems) {
          String dumpFile = String.format(formulaDumpFilePattern,
                  "interpolation", stats.cexAnalysisTimer.getNumberOfIntervals(), "interpolant", i);
          dumpFormulaToFile(itp, new File(dumpFile));
        }

        if (itp.isTrue()) {
          logger.log(Level.ALL, "For step", i, "got no interpolant.");

        } else {
          foundPredicates = true;
          Collection<AbstractionPredicate> preds;
          
          if (itp.isFalse()) {
            preds = ImmutableSet.of(makeFalsePredicate());
          } else {
            preds = getAtomsAsPredicates(itp);
          }
          assert !preds.isEmpty();
          info.addPredicatesForRefinement(e, preds);

          logger.log(Level.ALL, "For step", i, "got:",
              "interpolant", itp,
              "predicates", preds);

          if (dumpInterpolationProblems) {
            String dumpFile = String.format(formulaDumpFilePattern,
                        "interpolation", stats.cexAnalysisTimer.getNumberOfIntervals(), "atoms", i);
            Collection<SymbolicFormula> atoms = Collections2.transform(preds,
                new Function<AbstractionPredicate, SymbolicFormula>(){
                      @Override
                      public SymbolicFormula apply(AbstractionPredicate pArg0) {
                        return pArg0.getSymbolicAtom();
                      }
                });
            printFormulasToFile(atoms, new File(dumpFile));
          }
        }

        // TODO wellScopedPredicates have been disabled
        
        // TODO the following code relies on the fact that there is always an abstraction on function call and return

        // If we are entering or exiting a function, update the stack
        // of entry points
        // TODO checking if the abstraction node is a new function
//        if (wellScopedPredicates && e.getAbstractionLocation() instanceof CFAFunctionDefinitionNode) {
//          entryPoints.push(i);
//        }
          // TODO check we are returning from a function
//        if (wellScopedPredicates && e.getAbstractionLocation().getEnteringSummaryEdge() != null) {
//          entryPoints.pop();
//        }
      }

      if (!foundPredicates) {
        throw new RefinementFailedException(RefinementFailedException.Reason.InterpolationFailed, null);
      }

    } else {
      // this is a real bug, notify the user

      // get the reachingPathsFormula and add it to the solver environment
      // this formula contains predicates for all branches we took
      // this way we can figure out which branches make a feasible path
      SymbPredAbsAbstractElement lastElement = pAbstractTrace.get(pAbstractTrace.size()-1);
      pItpProver.addFormula(lastElement.getPathFormula().getReachingPathsFormula());
      Model model;
      NavigableMap<Integer, Map<Integer, Boolean>> preds;

      // need to ask solver for satisfiability again,
      // otherwise model doesn't contain new predicates
      boolean stillSatisfiable = !pItpProver.isUnsat();
      if (!stillSatisfiable) {
        pItpProver.reset();
        pItpProver.init();
        logger.log(Level.WARNING, "Could not get precise error path information because of inconsistent reachingPathsFormula!");

        int k = 0;
        for (SymbolicFormula formula : f) {
          pItpProver.addFormula(formula);
          String dumpFile =
              String.format(formulaDumpFilePattern, "interpolation",
                      stats.cexAnalysisTimer.getNumberOfIntervals(), "formula", k++);
          dumpFormulaToFile(formula, new File(dumpFile));
        }
        String dumpFile =
            String.format(formulaDumpFilePattern, "interpolation",
                stats.cexAnalysisTimer.getNumberOfIntervals(), "formula", k++);
        dumpFormulaToFile(lastElement.getPathFormula()
            .getReachingPathsFormula(), new File(dumpFile));
        pItpProver.isUnsat();
        model = pItpProver.getModel();
  preds = Maps.newTreeMap();
      } else {
        model = pItpProver.getModel();
        if (model.isEmpty()) {
          logger.log(Level.WARNING, "No satisfying assignment given by solver!");
          preds = Maps.newTreeMap();
        } else {
          preds = getPredicateValuesFromModel(model);
        }
      }

      info = new CounterexampleTraceInfo(f, pItpProver.getModel(), preds);
    }

    pItpProver.reset();

    // update stats
    stats.cexAnalysisTimer.stop();

    logger.log(Level.ALL, "Counterexample information:", info);

    return info;

  }
  
  @Override
  public void dumpCounterexampleToFile(CounterexampleTraceInfo cex, File file) {
    SymbolicFormula f = smgr.makeTrue();
    for (SymbolicFormula part : cex.getCounterExampleFormulas()) {
      f = smgr.makeAnd(f, part);
    }
    dumpFormulaToFile(f, file);
  }

  @Override
  public CounterexampleTraceInfo checkPath(List<CFAEdge> pPath) throws CPATransferException {
    PathFormula pathFormula = makeEmptyPathFormula();
    for (CFAEdge edge : pPath) {
      pathFormula = makeAnd(pathFormula, edge);
    }
    SymbolicFormula f = pathFormula.getSymbolicFormula();
    // ignore reachingPathsFormula here because it is just a simple path
    
    thmProver.init();
    try {
      thmProver.push(f);
      if (thmProver.isUnsat(smgr.makeTrue())) {
        return new CounterexampleTraceInfo();
      } else {
        return new CounterexampleTraceInfo(Collections.singletonList(f), thmProver.getModel(), Maps.<Integer, Map<Integer, Boolean>>newTreeMap());
      }
    } finally {
      thmProver.reset();
    }
  }
  
  /**
   * Counterexample analysis and predicate discovery.
   * This method is just an helper to delegate the actual work
   * This is used to detect timeouts for interpolation
   * @throws CPAException
   */
  @Override
  public CounterexampleTraceInfo buildCounterexampleTrace(
      ArrayList<SymbPredAbsAbstractElement> pAbstractTrace) throws CPAException {
    
    // if we don't want to limit the time given to the solver
    if (itpTimeLimit == 0) {
      return buildCounterexampleTraceWithSpecifiedItp(pAbstractTrace, firstItpProver);
    }
    
    // how many times is the problem tried to be solved so far?
    int noOfTries = 0;
    
    while (true) {
      TransferCallable<?> tc;
      
      if (noOfTries == 0) {
        tc = new TransferCallable<T1>(pAbstractTrace, firstItpProver);
      } else {
        tc = new TransferCallable<T2>(pAbstractTrace, secondItpProver);
      }

      Future<CounterexampleTraceInfo> future = CEGARAlgorithm.executor.submit(tc);

      try {
        // here we get the result of the post computation but there is a time limit
        // given to complete the task specified by timeLimit
        return future.get(itpTimeLimit, TimeUnit.MILLISECONDS);
        
      } catch (TimeoutException e){
        // if first try failed and changeItpSolveOTF enabled try the alternative solver
        if (changeItpSolveOTF && noOfTries == 0) {
          logger.log(Level.WARNING, "SMT-solver timed out during interpolation process, trying next solver.");
          noOfTries++;

        } else {
          logger.log(Level.SEVERE, "SMT-solver timed out during interpolation process");
          throw new RefinementFailedException(Reason.TIMEOUT, null);
        }
      } catch (InterruptedException e) {
        throw new ForceStopCPAException();
      
      } catch (ExecutionException e) {
        Throwable t = e.getCause();
        Throwables.propagateIfPossible(t, CPAException.class);
        
        logger.logException(Level.SEVERE, t, "Unexpected exception during interpolation!");
        throw new ForceStopCPAException();
      }
    }
  }

  private List<SymbolicFormula> getFormulasForTrace(
      List<SymbPredAbsAbstractElement> abstractTrace) {

    // create the DAG formula corresponding to the abstract trace. We create
    // n formulas, one per interpolation group
    List<SymbolicFormula> result = new ArrayList<SymbolicFormula>(abstractTrace.size());

    for (SymbPredAbsAbstractElement e : abstractTrace) {
      result.add(e.getAbstraction().getBlockFormula());
    }
    return result;
  }

  private <T> boolean checkInfeasabilityOfShortestTrace(List<SymbolicFormula> traceFormulas,
        List<T> itpGroupsIds, InterpolatingTheoremProver<T> pItpProver) {
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
        SymbolicFormula fm = traceFormulas.get(i);
        itpGroupsIds.set(i, pItpProver.addFormula(fm));
        if (!fm.isTrue()) {
          if (pItpProver.isUnsat()) {
            tmpSpurious = Boolean.TRUE;
            for (int j = s; j <= e; ++j) {
              itpGroupsIds.set(j, pItpProver.addFormula(traceFormulas.get(j)));
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
        SymbolicFormula fm = traceFormulas.get(i);
        itpGroupsIds.set(i, pItpProver.addFormula(fm));
        if (!fm.isTrue()) {
          if (pItpProver.isUnsat()) {
            tmpSpurious = Boolean.TRUE;
            // we need to add the other formulas to the itpProver
            // anyway, so it can setup its internal state properly
            for (int j = i+(useSuffix ? -1 : 1);
            useSuffix ? j >= 0 : j < traceFormulas.size();
            j += useSuffix ? -1 : 1) {
              itpGroupsIds.set(j, pItpProver.addFormula(traceFormulas.get(j)));
            }
            break;
          } else {
            tmpSpurious = Boolean.FALSE;
          }
        }
      }
    }

    return (tmpSpurious == null) ? pItpProver.isUnsat() : tmpSpurious;
  }

  private List<SymbolicFormula> getUsefulBlocks(List<SymbolicFormula> f,
      boolean suffixTrace, boolean zigZag) {
    
    stats.cexAnalysisGetUsefulBlocksTimer.start();

    // try to find a minimal-unsatisfiable-core of the trace (as Blast does)

    thmProver.init();

    logger.log(Level.ALL, "DEBUG_1", "Calling getUsefulBlocks on path",
        "of length:", f.size());

    SymbolicFormula trueFormula = smgr.makeTrue();
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
        f = Arrays.asList(needed);
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

          SymbolicFormula t = f.get(i);
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

          if (e < s) break;
        }
      } else {
        for (int i = pos; suffixTrace ? i >= 0 : i < f.size();
        i += incr) {
          SymbolicFormula t = f.get(i);
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
        break;
      }
    }

    while (toPop > 0) {
      --toPop;
      thmProver.pop();
    }

    thmProver.reset();

    logger.log(Level.ALL, "DEBUG_1", "Done getUsefulBlocks");

    stats.cexAnalysisGetUsefulBlocksTimer.stop();

    return f;
  }

  @Override
  public List<AbstractionPredicate> getAtomsAsPredicates(SymbolicFormula f) {
    Collection<SymbolicFormula> atoms;
    if (atomicPredicates) {
      atoms = smgr.extractAtoms(f, splitItpAtoms, false);
    } else {
      atoms = Collections.singleton(smgr.uninstantiate(f));
    }

    List<AbstractionPredicate> preds = new ArrayList<AbstractionPredicate>(atoms.size());

    for (SymbolicFormula atom : atoms) {
      preds.add(makePredicate(atom));
    }
    return preds;    
  }

  private NavigableMap<Integer, Map<Integer, Boolean>> getPredicateValuesFromModel(Model model) {

    NavigableMap<Integer, Map<Integer, Boolean>> preds = Maps.newTreeMap();
    for (AssignableTerm a : model.keySet()) {
      if (a instanceof Variable && a.getType() == TermType.Boolean) {
        
        String name = PREDICATE_NAME_PATTERN.matcher(a.getName()).replaceFirst("");
        if (!name.equals(a.getName())) {
          // pattern matched, so it's a variable with __pc__ in it

          String[] parts = name.split("@");
          assert parts.length == 2;
          // no NumberFormatException because of RegExp match earlier
          Integer edgeId = Integer.parseInt(parts[0]);
          Integer idx = Integer.parseInt(parts[1]);          
          
          Map<Integer, Boolean> p = preds.get(idx);
          if (p == null) {
            p = new HashMap<Integer, Boolean>(2);
            preds.put(idx, p);
          }
          
          Boolean value = (Boolean)model.get(a);
          p.put(edgeId, value);
        }             
      }
    }
    return preds;
  }

  private class TransferCallable<T> implements Callable<CounterexampleTraceInfo> {

    private final ArrayList<SymbPredAbsAbstractElement> abstractTrace;
    private final InterpolatingTheoremProver<T> currentItpProver;

    public TransferCallable(ArrayList<SymbPredAbsAbstractElement> pAbstractTrace,
        InterpolatingTheoremProver<T> pItpProver) {
      abstractTrace = pAbstractTrace;
      currentItpProver = pItpProver;
    }

    @Override
    public CounterexampleTraceInfo call() throws CPAException {
      return buildCounterexampleTraceWithSpecifiedItp(abstractTrace, currentItpProver);
    }
  }
}
