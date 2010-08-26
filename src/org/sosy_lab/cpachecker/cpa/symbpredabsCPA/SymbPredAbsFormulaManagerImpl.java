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
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import org.sosy_lab.common.Classes;
import org.sosy_lab.common.Files;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.core.algorithm.CEGARAlgorithm;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.ForceStopCPAException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException.Reason;
import org.sosy_lab.cpachecker.util.symbpredabstraction.CommonFormulaManager;
import org.sosy_lab.cpachecker.util.symbpredabstraction.PathFormula;
import org.sosy_lab.cpachecker.util.symbpredabstraction.SSAMap;
import org.sosy_lab.cpachecker.util.symbpredabstraction.Cache.CartesianAbstractionCacheKey;
import org.sosy_lab.cpachecker.util.symbpredabstraction.Cache.FeasibilityCacheKey;
import org.sosy_lab.cpachecker.util.symbpredabstraction.Cache.TimeStampCache;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.AbstractFormula;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.AbstractFormulaManager;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.InterpolatingTheoremProver;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.Predicate;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormula;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormulaList;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormulaManager;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.TheoremProver;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.TheoremProver.AllSatResult;
import org.sosy_lab.cpachecker.util.symbpredabstraction.trace.CounterexampleTraceInfo;

import com.google.common.base.Joiner;


@Options(prefix="cpas.symbpredabs")
class SymbPredAbsFormulaManagerImpl<T1, T2> extends CommonFormulaManager implements SymbPredAbsFormulaManager {

  static class Stats {
    public long abstractionTime = 0;
    public long abstractionMaxTime = 0;
    public long abstractionBddTime = 0;
    public long abstractionMaxBddTime = 0;
    public long allSatCount = 0;
    public int maxAllSatCount = 0;
    public int numCallsAbstraction = 0;
    public int numCallsAbstractionCached = 0;
    public long cexAnalysisTime = 0;
    public long cexAnalysisMaxTime = 0;
    public int numCallsCexAnalysis = 0;
    public long abstractionSolveTime = 0;
    public long abstractionMaxSolveTime = 0;
    public long cexAnalysisSolverTime = 0;
    public long cexAnalysisMaxSolverTime = 0;
    public int numCoverageChecks = 0;
    public long bddCoverageCheckTime = 0;
    public long bddCoverageCheckMaxTime = 0;
    public long cexAnalysisGetUsefulBlocksTime = 0;
    public long cexAnalysisGetUsefulBlocksMaxTime = 0;
  }
  final Stats stats;

  private final TheoremProver thmProver;
  private final InterpolatingTheoremProver<T1> firstItpProver;
  private final InterpolatingTheoremProver<T2> secondItpProver;

  private static final int MAX_CACHE_SIZE = 100000;

  @Option(name="abstraction.cartesian")
  private boolean cartesianAbstraction = false;

  @Option(name="mathsat.dumpHardAbstractionQueries")
  private boolean dumpHardAbstractions = false;

  @Option(name="explicit.getUsefulBlocks")
  private boolean getUsefulBlocks = false;

  @Option(name="shortestCexTrace")
  private boolean shortestTrace = false;

  @Option(name="refinement.splitItpAtoms")
  private boolean splitItpAtoms = false;

  @Option(name="shortestCexTraceUseSuffix")
  private boolean useSuffix = false;

  @Option(name="shortestCexTraceZigZag")
  private boolean useZigZag = false;

  @Option
  private boolean inlineFunctions = false;

  @Option(name="refinement.addWellScopedPredicates")
  private boolean wellScopedPredicates = false;

  @Option(name="refinement.msatCexFile", type=Option.Type.OUTPUT_FILE)
  private File msatCexFile = new File("cex.msat");

  @Option(name="refinement.dumpInterpolationProblems")
  private boolean dumpInterpolationProblems = false;

  @Option(name="interpolation.timelimit")
  private long itpTimeLimit = 0;

  @Option(name="interpolation.changesolverontimeout")
  private boolean changeItpSolveOTF = false;

  private final Map<Pair<SymbolicFormula, List<SymbolicFormula>>, AbstractFormula> abstractionCache;
  //cache for cartesian abstraction queries. For each predicate, the values
  // are -1: predicate is false, 0: predicate is don't care,
  // 1: predicate is true
  private final TimeStampCache<CartesianAbstractionCacheKey, Byte> cartesianAbstractionCache;
  private final TimeStampCache<FeasibilityCacheKey, Boolean> feasibilityCache;

  public SymbPredAbsFormulaManagerImpl(
      AbstractFormulaManager pAmgr,
      SymbolicFormulaManager pSmgr,
      TheoremProver pThmProver,
      InterpolatingTheoremProver<T1> pItpProver,
      InterpolatingTheoremProver<T2> pAltItpProver,
      Configuration config,
      LogManager logger) throws InvalidConfigurationException {
    super(pAmgr, pSmgr, config, logger);
    config.inject(this);

    stats = new Stats();
    thmProver = pThmProver;
    firstItpProver = pItpProver;
    secondItpProver = pAltItpProver;

    if (inlineFunctions && wellScopedPredicates) {
      logger.log(Level.WARNING, "Well scoped predicates not possible with function inlining, disabling them.");
      wellScopedPredicates = false;
    }

    if (useCache) {
      if (cartesianAbstraction) {
        abstractionCache = null;
        cartesianAbstractionCache = new TimeStampCache<CartesianAbstractionCacheKey, Byte>(MAX_CACHE_SIZE);
        feasibilityCache = new TimeStampCache<FeasibilityCacheKey, Boolean>(MAX_CACHE_SIZE);

      } else {
        abstractionCache = new HashMap<Pair<SymbolicFormula, List<SymbolicFormula>>, AbstractFormula>();
        cartesianAbstractionCache = null;
        feasibilityCache = null;
      }
    } else {
      abstractionCache = null;
      cartesianAbstractionCache = null;
      feasibilityCache = null;
    }
  }

  /**
   * Abstract post operation.
   */
  @Override
  public AbstractFormula buildAbstraction(
      AbstractFormula abs, PathFormula pathFormula,
      Collection<Predicate> predicates) {
    stats.numCallsAbstraction++;
    if (cartesianAbstraction) {
      return buildCartesianAbstraction(abs, pathFormula, predicates);
    } else {
      return buildBooleanAbstraction(abs, pathFormula, predicates);
    }
  }

  private AbstractFormula buildCartesianAbstraction(
      AbstractFormula abs,
      PathFormula pathFormula,
      Collection<Predicate> predicates) {

    long startTime = System.currentTimeMillis();

    final SymbolicFormula f = buildSymbolicFormula(abs, pathFormula.getSymbolicFormula());
    
    // clone ssa map because we might change it
    SSAMap ssa = new SSAMap(pathFormula.getSsa());
    
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
          // abstract post leads to false, we can return immediately
          return amgr.makeFalse();
        }
      }
    }

    long solveStartTime = System.currentTimeMillis();

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
          return amgr.makeFalse();
        }
      } else {
        //++stats.abstractionNumCachedQueries;
      }

      thmProver.push(f);
      try {
        long totBddTime = 0;

        AbstractFormula absbdd = amgr.makeTrue();

        // check whether each of the predicate is implied in the next state...
        Set<String> predvars = new HashSet<String>();
        Set<Pair<String, SymbolicFormulaList>> predlvals =
          new HashSet<Pair<String, SymbolicFormulaList>>();

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
            Pair<? extends SymbolicFormula, ? extends SymbolicFormula> pi =
              getPredicateVarAndAtom(p);

            // update the SSA map, by instantiating all the uninstantiated
            // variables that occur in the predicates definitions
            // (at index 1)
            predvars.clear();
            predlvals.clear();
            smgr.collectVarNames(pi.getSecond(), predvars, predlvals);
            
            for (String var : predvars) {
              if (ssa.getIndex(var) < 0) {
                ssa.setIndex(var, 1);
              }
            }
            
            for (Pair<String, SymbolicFormulaList> pp : predlvals) {
              SymbolicFormulaList args = smgr.instantiate(pp.getSecond(), ssa);
              if (ssa.getIndex(pp.getFirst(), args) < 0) {
                ssa.setIndex(pp.getFirst(), args, 1);
              }
            }


            logger.log(Level.ALL, "DEBUG_1",
                "CHECKING VALUE OF PREDICATE: ", pi.getFirst());

            // instantiate the definition of the predicate
            SymbolicFormula predTrue = smgr.instantiate(pi.getSecond(), ssa);
            SymbolicFormula predFalse = smgr.makeNot(predTrue);

            // check whether this predicate has a truth value in the next
            // state
            byte predVal = 0; // pred is neither true nor false

            //++stats.abstractionNumMathsatQueries;
            boolean isTrue = thmProver.isUnsat(predFalse);

            if (isTrue) {
              long startBddTime = System.currentTimeMillis();
              AbstractFormula v = p.getFormula();
              absbdd = amgr.makeAnd(absbdd, v);
              long endBddTime = System.currentTimeMillis();
              totBddTime += (endBddTime - startBddTime);

              predVal = 1;
            } else {
              // check whether it's false...
              //++stats.abstractionNumMathsatQueries;
              boolean isFalse = thmProver.isUnsat(predTrue);

              if (isFalse) {
                long startBddTime = System.currentTimeMillis();
                AbstractFormula v = p.getFormula();
                v = amgr.makeNot(v);
                absbdd = amgr.makeAnd(absbdd, v);
                long endBddTime = System.currentTimeMillis();
                totBddTime += (endBddTime - startBddTime);

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
        long solveEndTime = System.currentTimeMillis();

        // update statistics
        long endTime = System.currentTimeMillis();
        long solveTime = (solveEndTime - solveStartTime) - totBddTime;
        long time = (endTime - startTime) - totBddTime;
        stats.abstractionMaxTime =
          Math.max(time, stats.abstractionMaxTime);
        stats.abstractionMaxBddTime =
          Math.max(totBddTime, stats.abstractionMaxBddTime);
        stats.abstractionTime += time;
        stats.abstractionBddTime += totBddTime;
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

  private SymbolicFormula buildSymbolicFormula(AbstractFormula abstractionFormula,
      SymbolicFormula symbFormula) {

    // build the concrete representation of the abstract formula of e
    // this is an abstract formula - specifically it is a bddabstractformula
    // which is basically an integer which represents it
    // create the concrete form of the abstract formula
    // (abstract formula is the bdd representation)
    SymbolicFormula absFormula = smgr.instantiate(toConcrete(abstractionFormula), null);

    // the indices of all variables in absFormula are now 1
    // this fits exactly to the indices of symbFormula
    
    symbFormula = smgr.replaceAssignments(symbFormula);

    symbFormula = smgr.prepareFormula(symbFormula);

    return smgr.makeAnd(absFormula, symbFormula);
  }

  /**
   * Checks if (a1 & p1) => a2
   */
  @Override
  public boolean checkCoverage(AbstractFormula a1, PathFormula p1, AbstractFormula a2) {
    SymbolicFormula a = buildSymbolicFormula(a1, p1.getSymbolicFormula());

    SymbolicFormula b = smgr.instantiate(toConcrete(a2), p1.getSsa());

    SymbolicFormula toCheck = smgr.makeAnd(a, smgr.makeNot(b));

    thmProver.init();
    boolean ret = thmProver.isUnsat(toCheck);
    thmProver.reset();

    return ret;
  }

  private AbstractFormula buildBooleanAbstraction(
      AbstractFormula abstractionFormula, PathFormula pathFormula,
      Collection<Predicate> predicates) {

    logger.log(Level.ALL, "Old abstraction:", abstractionFormula);
    logger.log(Level.ALL, "Path formula:", pathFormula);
    logger.log(Level.ALL, "Predicates:", predicates);

    long startTime = System.currentTimeMillis();

    // first, create the new formula corresponding to
    // (symbFormula & edges from e to succ)
    // TODO - at the moment, we assume that all the edges connecting e and
    // succ have no statement or assertion attached (i.e. they are just
    // return edges or gotos). This might need to change in the future!!
    // (So, for now we don't need to to anything...)

    SymbolicFormula symbFormula = buildSymbolicFormula(abstractionFormula, pathFormula.getSymbolicFormula());

    // build the definition of the predicates, and instantiate them
    SymbolicFormula predDef = buildPredicateFormula(predicates, pathFormula.getSecond());

    // the formula is (abstractionFormula & pathFormula & predDef)
    SymbolicFormula fm = smgr.makeAnd(symbFormula, predDef);
    
    // collect all predicate variables so that the solver knows for which
    // variables we want to have the satisfying assignments
    List<SymbolicFormula> predVars = new ArrayList<SymbolicFormula>(predicates.size());
    for (Predicate p : predicates) {
      predVars.add(getPredicateVarAndAtom(p).getFirst());
    }

    logger.log(Level.ALL, "DEBUG_2",
        "COMPUTING ALL-SMT ON FORMULA: ", fm);

    Pair<SymbolicFormula, List<SymbolicFormula>> absKey =
      new Pair<SymbolicFormula, List<SymbolicFormula>>(fm, predVars);
    AbstractFormula result;
    if (useCache && abstractionCache.containsKey(absKey)) {
      ++stats.numCallsAbstractionCached;
      result = abstractionCache.get(absKey);

      logger.log(Level.ALL, "Abstraction was cached, result is", result);

    } else {
      long solveStartTime = System.currentTimeMillis();
      AllSatResult allSatResult = thmProver.allSat(fm, predVars, this, amgr);
      long solveEndTime = System.currentTimeMillis();

      result = allSatResult.getResult();

      if (useCache) {
        abstractionCache.put(absKey, result);
      }

      // update statistics
      int numModels = allSatResult.getCount();
      if (numModels < Integer.MAX_VALUE) {
        stats.maxAllSatCount = Math.max(numModels, stats.maxAllSatCount);
        stats.allSatCount += numModels;
      }
      long bddTime   = allSatResult.getTotalTime();
      long solveTime = (solveEndTime - solveStartTime) - bddTime;

      stats.abstractionSolveTime += solveTime;
      stats.abstractionBddTime   += bddTime;
      startTime += bddTime; // do not count BDD creation time

      stats.abstractionMaxBddTime =
        Math.max(bddTime, stats.abstractionMaxBddTime);
      stats.abstractionMaxSolveTime =
        Math.max(solveTime, stats.abstractionMaxSolveTime);

      // TODO dump hard abst
      if (solveTime > 10000 && dumpHardAbstractions) {
        // we want to dump "hard" problems...
        smgr.dumpAbstraction(smgr.makeTrue(), symbFormula, predDef, predVars);
      }
      logger.log(Level.ALL, "Abstraction computed, result is", result);
    }

    // update statistics
    long endTime = System.currentTimeMillis();
    long abstractionSolverTime = (endTime - startTime);
    stats.abstractionTime += abstractionSolverTime;
    stats.abstractionMaxTime =
      Math.max(abstractionSolverTime, stats.abstractionMaxTime);

    return result;
  }

  /**
   * Checks if an abstraction formula and a pathFormula are unsatisfiable.
   * @param pAbstractionFormula the abstraction formula
   * @param pPathFormula the path formula
   * @return unsat(pAbstractionFormula & pPathFormula)
   */
  @Override
  public boolean unsat(AbstractFormula abstractionFormula, PathFormula pathFormula) {

    SymbolicFormula symbFormula = buildSymbolicFormula(abstractionFormula, pathFormula.getSymbolicFormula());
    logger.log(Level.ALL, "Checking satisfiability of formula", symbFormula);

    thmProver.init();
    boolean result = thmProver.isUnsat(symbFormula);
    thmProver.reset();

    return result;
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
    
    long startTime = System.currentTimeMillis();
    stats.numCallsCexAnalysis++;

    logger.log(Level.FINEST, "Building counterexample trace");

    List<SymbolicFormula> f = getFormulasForTrace(pAbstractTrace);

    smgr.prepareFormulas(f);
    f = Collections.unmodifiableList(f);

    logger.log(Level.ALL, "Counterexample trace formulas:", f);

    logger.log(Level.FINEST, "Checking feasibility of counterexample trace");

    // now f is the DAG formula which is satisfiable iff there is a
    // concrete counterexample

    // create a working environment
    pItpProver.init();

    long msatSolveTimeStart = System.currentTimeMillis();

    if (shortestTrace && getUsefulBlocks) {
      f = Collections.unmodifiableList(getUsefulBlocks(f, useSuffix, useZigZag));
    }

    if (dumpInterpolationProblems) {
      int refinement = stats.numCallsCexAnalysis;
      logger.log(Level.FINEST, "Dumping", f.size(), "formulas of refinement number", refinement);

      int k = 0;
      for (SymbolicFormula formula : f) {
        dumpFormulasToFile(Collections.singleton(formula), 
            new File(msatCexFile.getAbsolutePath() + ".ref" + refinement + ".f" + k++));
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

    long msatSolveTimeEnd = System.currentTimeMillis();
    long msatSolveTime = msatSolveTimeEnd - msatSolveTimeStart;

    logger.log(Level.FINEST, "Counterexample trace is", (spurious ? "infeasible" : "feasible"));

    CounterexampleTraceInfo info;

    if (spurious) {
      info = new CounterexampleTraceInfo();
      int refinement = stats.numCallsCexAnalysis;

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

        msatSolveTimeStart = System.currentTimeMillis();
        SymbolicFormula itp = pItpProver.getInterpolant(itpGroupsIds.subList(start_of_a, i+1));
        msatSolveTimeEnd = System.currentTimeMillis();
        msatSolveTime += msatSolveTimeEnd - msatSolveTimeStart;

        if (dumpInterpolationProblems) {
          dumpFormulasToFile(Collections.singleton(itp), 
              new File(msatCexFile.getAbsolutePath() + ".ref" + refinement + ".itp" + i));
        }

        if (itp.isTrue() || itp.isFalse()) {
          logger.log(Level.ALL, "For location", e.getAbstractionLocation(), "got no interpolant.");

        } else {
          foundPredicates = true;

          Collection<SymbolicFormula> atoms = smgr.extractAtoms(itp, splitItpAtoms, false);
          assert !atoms.isEmpty();
          Collection<Predicate> preds = buildPredicates(atoms);
          info.addPredicatesForRefinement(e, preds);

          logger.log(Level.ALL, "For location", e.getAbstractionLocation(), "got:",
              "interpolant", itp,
              "atoms ", atoms,
              "predicates", preds);

          if (dumpInterpolationProblems) {
            try {
              Files.writeFile(new File(msatCexFile.getAbsolutePath() + ".ref" + refinement + ".atoms" + i),
                  Joiner.on('\n').join(atoms) + '\n', false);
            } catch (IOException ex) {
              logger.log(Level.WARNING, "Could not dump interpolant atoms to file! ("
                  + ex.getMessage() + ")");
            }
          }
          
        }

        // TODO the following code relies on the fact that there is always an abstraction on function call and return

        // If we are entering or exiting a function, update the stack
        // of entry points
        // TODO checking if the abstraction node is a new function
        if (wellScopedPredicates && e.getAbstractionLocation() instanceof CFAFunctionDefinitionNode) {
          entryPoints.push(i);
        }
        // TODO check we are returning from a function
        if (wellScopedPredicates && e.getAbstractionLocation().getEnteringSummaryEdge() != null) {
          entryPoints.pop();
        }
      }

      if (!foundPredicates) {
        throw new RefinementFailedException(RefinementFailedException.Reason.InterpolationFailed, null);
      }

    } else {
      // this is a real bug, notify the user
      info = new CounterexampleTraceInfo(pItpProver.getModel());

      // TODO - reconstruct counterexample
      // For now, we dump the asserted formula to a user-specified file
      dumpFormulasToFile(f, msatCexFile);
    }

    pItpProver.reset();

    // update stats
    long endTime = System.currentTimeMillis();
    long totTime = endTime - startTime;
    stats.cexAnalysisTime += totTime;
    stats.cexAnalysisMaxTime = Math.max(totTime, stats.cexAnalysisMaxTime);
    stats.cexAnalysisSolverTime += msatSolveTime;
    stats.cexAnalysisMaxSolverTime =
      Math.max(msatSolveTime, stats.cexAnalysisMaxSolverTime);

    logger.log(Level.ALL, "Counterexample information:", info);

    return info;

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
        Classes.throwExceptionIfPossible(t, CPAException.class);
        
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

    Iterator<SymbPredAbsAbstractElement> it = abstractTrace.iterator();
    assert it.hasNext();
    
    // handle first formula separately because we don't need to shift
    PathFormula p = it.next().getInitAbstractionFormula();
    SSAMap ssa = p.getSsa();
    result.add(smgr.replaceAssignments(p.getSymbolicFormula()));
    
    while (it.hasNext()) {
      p = it.next().getInitAbstractionFormula();

      // don't need to call replaceAssignments because shift does the same trick
      p = smgr.shift(p.getSymbolicFormula(), ssa);
      
      result.add(p.getSymbolicFormula());
      
      // shift returns a new ssa map,
      // we need to add those variables that were not used by shift()
      SSAMap newSsa = p.getSsa();
      newSsa.update(ssa);
      ssa = newSsa;
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
    long gubStart = System.currentTimeMillis();

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

    long gubEnd = System.currentTimeMillis();
    stats.cexAnalysisGetUsefulBlocksTime += gubEnd - gubStart;
    stats.cexAnalysisGetUsefulBlocksMaxTime = Math.max(
        stats.cexAnalysisGetUsefulBlocksMaxTime, gubEnd - gubStart);

    return f;
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