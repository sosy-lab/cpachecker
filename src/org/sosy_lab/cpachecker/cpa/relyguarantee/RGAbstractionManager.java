/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.relyguarantee;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.sosy_lab.common.Files;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.Triple;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.interfaces.AbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.TheoremProver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.TheoremProver.AllSatResult;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;

@Options(prefix="cpa.rg")
public class RGAbstractionManager implements StatisticsProvider {

  // singleton instance of the manager
  private static RGAbstractionManager instance;

  public final Stats stats;

  protected final LogManager logger;
  protected final FormulaManager fmgr;
  protected final PathFormulaManager pmgr;
  protected final AbstractionManager amgr;
  protected final TheoremProver thmProver;

  @Option(name="abstraction.cartesian",
      description="whether to use Boolean (false) or Cartesian (true) abstraction")
  private boolean cartesianAbstraction = true;

  @Option(name="abstraction.cartesianNextValue",
      description="whether to use Boolean (false) or Cartesian (true) abstraction for enviromental transitions.")
  private boolean cartesianNextValAbstraction = true;

  @Option(name="abstraction.useCache", description="use caching of abstractions")
  private boolean useCache = true;

  private final Map<Pair<PathFormula, Collection<AbstractionPredicate>>, AbstractionFormula> abstractionCache;
  //cache for cartesian abstraction queries. For each predicate, the values
  // are -1: predicate is false, 0: predicate is don't care,
  // 1: predicate is true
  private final Map<Pair<PathFormula, AbstractionPredicate>, Byte> cartesianAbstractionCache;
  private final Map<Formula, Boolean> feasibilityCache;

  private Map<Triple<PathFormula, PathFormula, Collection<AbstractionPredicate>>, AbstractionFormula> abstractionNextValueCache;

  public static RGAbstractionManager getInstance( RegionManager pRmgr, FormulaManager pFmgr,PathFormulaManager pPmgr, TheoremProver pThmProver, Configuration config, LogManager pLogger) throws InvalidConfigurationException{
    if (instance == null){
      instance = new RGAbstractionManager(pRmgr, pFmgr, pPmgr, pThmProver, config, pLogger);
    }

    return instance;
  }

  protected RGAbstractionManager(RegionManager pRmgr, FormulaManager pFmgr, PathFormulaManager pPmgr, TheoremProver pThmProver, Configuration config, LogManager pLogger) throws InvalidConfigurationException {
    config.inject(this, RGAbstractionManager.class);

    stats = new Stats();
    logger = pLogger;
    fmgr = pFmgr;
    pmgr = pPmgr;
    amgr = AbstractionManagerImpl.getInstance(pRmgr, pFmgr, pPmgr, config, pLogger);
    thmProver = pThmProver;

    if (useCache) {
      abstractionCache = new HashMap<Pair<PathFormula, Collection<AbstractionPredicate>>, AbstractionFormula>();
      abstractionNextValueCache = new HashMap<Triple<PathFormula, PathFormula, Collection<AbstractionPredicate>>, AbstractionFormula>();
    } else {
      abstractionCache = null;
    }
    if (useCache && cartesianAbstraction) {
      cartesianAbstractionCache = new HashMap<Pair<PathFormula, AbstractionPredicate>, Byte>();
      feasibilityCache = new HashMap<Formula, Boolean>();
    } else {
      cartesianAbstractionCache = null;
      feasibilityCache = null;
    }

    instance = this;
  }

  /**
   * Abstract post operation.
   */
  public AbstractionFormula buildAbstraction(AbstractionFormula abstractionFormula, PathFormula pathFormula, Collection<AbstractionPredicate> predicates) {
    stats.buildAbstractionCalls++;
    stats.buildAbstractionTimer.start();

   /* if (predicates.isEmpty()) {
      stats.numSymbolicAbstractions++;
      return makeTrueAbstractionFormula(pathFormula);
    }*/

    logger.log(Level.ALL, "Old abstraction:", abstractionFormula);
    logger.log(Level.ALL, "Path formula:", pathFormula);
    logger.log(Level.ALL, "Predicates:", predicates);


    Formula absFormula = abstractionFormula.asFormula();
    Formula symbFormula = pathFormula.getFormula();
    Formula f = fmgr.makeAnd(absFormula, symbFormula);
    PathFormula pf = new PathFormula(f, pathFormula.getSsa(), pathFormula.getLength());

    if (absFormula.toString().contains("start_main@2")){
      System.out.println();
    }

    // caching
    Pair<PathFormula, Collection<AbstractionPredicate>> absKey = null;
    if (useCache) {
      absKey = Pair.of(pf, predicates);
      AbstractionFormula result = abstractionCache.get(absKey);

      if (result != null) {
        // create new abstraction object to have a unique abstraction id
        result = new AbstractionFormula(result.asRegion(), result.asPathFormula(), pathFormula);
        logger.log(Level.ALL, "Abstraction was cached, result is", result);
        stats.buildAbstractionCH++;
        stats.buildAbstractionTimer.stop();
        return result;
      }
    }

    Region abs;
    if (cartesianAbstraction) {
      abs = buildCartesianAbstraction(pf, predicates);
    } else {
      abs = buildBooleanAbstraction(f, pathFormula.getSsa(), predicates);
    }

    Formula symbolicAbs = fmgr.instantiate(amgr.toConcrete(abs), pathFormula.getSsa());
    PathFormula symbolicPathAbs = new PathFormula(symbolicAbs, pathFormula.getSsa(), 0);
    AbstractionFormula result = new AbstractionFormula(abs, symbolicPathAbs, pathFormula);

    if (useCache) {
      abstractionCache.put(absKey, result);
    }

    stats.buildAbstractionTimer.stop();
    return result;
  }

  /**
   * Builds abstraction formula that shows the change between oldPf and newPf.
   * @param aFormula
   * @param pf
   * @param hiPf
   * @param predicates
   * @param tid
   * @return
   */
  public AbstractionFormula buildNextValueAbstraction(AbstractionFormula aFormula, PathFormula lowPf,PathFormula highPf, Collection<AbstractionPredicate> predicates, int tid) {
    stats.buildAbstractionNVCalls++;
    stats.buildAbstractionNVTimer.start();

    /* if (predicates.isEmpty()) {
       stats.numSymbolicAbstractions++;
       return makeTrueAbstractionFormula(pathFormula);
     }*/

    // formula for abstraction
     PathFormula lowAPf = pmgr.makeAnd(lowPf, aFormula.asPathFormula());
     PathFormula highAPf = pmgr.makeAnd(highPf, aFormula.asPathFormula());

     // caching
     Triple<PathFormula, PathFormula, Collection<AbstractionPredicate>> absKey = null;
     if (useCache){
       absKey = Triple.of(lowAPf, highAPf, predicates);

       AbstractionFormula result = abstractionNextValueCache.get(absKey);

       if (result != null) {
         result = new AbstractionFormula(result.asRegion(), result.asPathFormula(), highPf);
         stats.buildAbstractionNVCH++;
         stats.buildAbstractionNVTimer.stop();
         return result;
       }
     }

     Region abs = null;
     if (cartesianNextValAbstraction) {
       abs = buildNextValueCartesianAbstraction(lowAPf, highAPf, predicates, tid);
     } else {
       abs = buildNextValueBooleanAbstraction(lowAPf, highAPf, predicates);
     }

     // a little work-around : symbolicPathAbs is a path formula without indexes
     Formula symbolicAbs = amgr.toConcrete(abs);
     //Formula symbolicAbs = fmgr.instantiateNextVal(amgr.toConcrete(abs), oldPf.getSsa(), newPf.getSsa());
     PathFormula symbolicPathAbs = new PathFormula(symbolicAbs, highPf.getSsa(), 0);
     AbstractionFormula result = new AbstractionFormula(abs, symbolicPathAbs, highPf);

     if (useCache) {
       abstractionNextValueCache.put(absKey, result);
     }

     stats.buildAbstractionNVTimer.stop();
     return result;
  }



  private Region buildCartesianAbstraction(final PathFormula pf, Collection<AbstractionPredicate> predicates) {
    final RegionManager rmgr = amgr.getRegionManager();

    Formula f = pf.getFormula();

    thmProver.init();
    try {

      boolean feasibility;
      if (useCache && feasibilityCache.containsKey(f)) {
        feasibility = feasibilityCache.get(f);

      } else {
        feasibility = !thmProver.isUnsat(f);
        if (useCache) {
          feasibilityCache.put(f, feasibility);
        }
      }

      if (!feasibility) {
        // abstract post leads to false, we can return immediately
        return rmgr.makeFalse();
      }

      thmProver.push(f);
      try {
        Region absbdd = rmgr.makeTrue();
        SSAMap ssa = pf.getSsa();

        // check whether each of the predicate is implied in the next state...

        for (AbstractionPredicate p : predicates) {
          Pair<PathFormula, AbstractionPredicate> cacheKey = Pair.of(pf, p);
          if (useCache && cartesianAbstractionCache.containsKey(cacheKey)) {
            byte predVal = cartesianAbstractionCache.get(cacheKey);

            Region v = p.getAbstractVariable();
            if (predVal == -1) { // pred is false
              v = rmgr.makeNot(v);
              absbdd = rmgr.makeAnd(absbdd, v);
            } else if (predVal == 1) { // pred is true
              absbdd = rmgr.makeAnd(absbdd, v);
            } else {
              assert predVal == 0 : "predicate value is neither false, true, nor unknown";
            }

          } else {
            logger.log(Level.ALL, "DEBUG_1",
                "CHECKING VALUE OF PREDICATE: ", p.getSymbolicAtom());

            // instantiate the definition of the predicate
            Formula predTrue = fmgr.instantiate(p.getSymbolicAtom(), ssa);
            Formula predFalse = fmgr.makeNot(predTrue);

            // check whether this predicate has a truth value in the next
            // state
            byte predVal = 0; // pred is neither true nor false

            boolean isTrue = thmProver.isUnsat(predFalse);

            if (isTrue) {

              Region v = p.getAbstractVariable();
              absbdd = rmgr.makeAnd(absbdd, v);


              predVal = 1;
            } else {
              // check whether it's false...
              boolean isFalse = thmProver.isUnsat(predTrue);

              if (isFalse) {
                Region v = p.getAbstractVariable();
                v = rmgr.makeNot(v);
                absbdd = rmgr.makeAnd(absbdd, v);

                predVal = -1;
              }
            }

            if (useCache) {
              cartesianAbstractionCache.put(cacheKey, predVal);
            }
          }
        }

        return absbdd;

      } finally {
        thmProver.pop();
      }

    } finally {
      thmProver.reset();

    }
  }


  private Region buildNextValueCartesianAbstraction(PathFormula lowPf, PathFormula hiPf, Collection<AbstractionPredicate> predicates, int tid) {

    final RegionManager rmgr = amgr.getRegionManager();

    thmProver.init();
    try {

      boolean feasibility;
      feasibility = !thmProver.isUnsat(hiPf.getFormula());

      if (!feasibility) {
        // abstract post leads to false, we can return immediately
        return rmgr.makeFalse();
      }

      thmProver.push(hiPf.getFormula());
      try {
        Region absbdd = rmgr.makeTrue();
        Region rAbsbdd = rmgr.makeTrue();

        // check whether each of the predicate is implied in the next state...

        for (AbstractionPredicate p : predicates) {

          logger.log(Level.ALL, "DEBUG_1",
              "CHECKING VALUE OF PREDICATE: ", p.getSymbolicAtom());

          // instantiate the definition of the predicate
          Formula predTrue = fmgr.instantiateNextValue(p.getSymbolicAtom(), lowPf.getSsa(), hiPf.getSsa());
          Formula predFalse = fmgr.makeNot(predTrue);

          // check whether this predicate has a truth value in the next
          // state
          byte predVal = 0; // pred is neither true nor false

          boolean isTrue = thmProver.isUnsat(predFalse);

          if (isTrue) {
            Region v = p.getAbstractVariable();
            absbdd = rmgr.makeAnd(absbdd, v);
            predVal = 1;
          } else {
            // check whether it's false...
            boolean isFalse = thmProver.isUnsat(predTrue);

            if (isFalse) {
              Region v = p.getAbstractVariable();
              v = rmgr.makeNot(v);
              absbdd = rmgr.makeAnd(absbdd, v);

              predVal = -1;
            }
          }
        }

        return absbdd;

      } finally {
        thmProver.pop();
      }

    } finally {
      thmProver.reset();

    }
  }

  private Formula buildFormula(Formula symbFormula) {

    return symbFormula;
  }


  private Region buildBooleanAbstraction(Formula f, SSAMap ssa,
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
    Formula predDef = fmgr.makeTrue();
    List<Formula> predVars = new ArrayList<Formula>(predicates.size());

    for (AbstractionPredicate p : predicates) {
      // get propositional variable and definition of predicate
      Formula var = p.getSymbolicVariable();
      Formula def = p.getSymbolicAtom();
      if (def.isFalse()) {
        continue;
      }
      def = fmgr.instantiate(def, ssa);

      // build the formula (var <-> def) and add it to the list of definitions
      Formula equiv = fmgr.makeEquivalence(var, def);
      predDef = fmgr.makeAnd(predDef, equiv);

      predVars.add(var);
    }
    if (predVars.isEmpty()) {
    }

    // the formula is (abstractionFormula & pathFormula & predDef)
    Formula fm = fmgr.makeAnd(f, predDef);
    logger.log(Level.ALL, "COMPUTING ALL-SMT ON FORMULA: ", fm);


    stats.bogus.start();
    AllSatResult allSatResult = thmProver.allSat(fm, predVars, amgr, stats.bogus);
    long solveTime = stats.bogus.stop();

    // update statistics
    int numModels = allSatResult.getCount();
    if (numModels < Integer.MAX_VALUE) {
    }

    Region result = allSatResult.getResult();
    logger.log(Level.ALL, "Abstraction computed, result is", result);
    return result;
  }

  /**
   * Builds a Boolean abstraction of the change between oldAbsPf and newAbsPf. Unhashed predicates are instantiated to the old formula
   * and the hashed ones to the new formula.
   * @param oldAbsPf
   * @param newAbsPf
   * @param predicates
   * @return
   */
  private Region buildNextValueBooleanAbstraction(PathFormula oldAbsPf, PathFormula newAbsPf, Collection<AbstractionPredicate> predicates) {

    Formula predDef = fmgr.makeTrue();
    List<Formula> predVars = new ArrayList<Formula>(predicates.size());

    for (AbstractionPredicate p : predicates) {
      // get propositional variable and definition of predicate
      Formula var = p.getSymbolicVariable();
      Formula def = p.getSymbolicAtom();
      if (def.isFalse()) {
        continue;
      }
      //def = fmgr.instantiate(def, ssa);
      def = fmgr.instantiateNextValue(def, oldAbsPf.getSsa(), newAbsPf.getSsa());

      // build the formula (var <-> def) and add it to the list of definitions
      Formula equiv = fmgr.makeEquivalence(var, def);
      predDef = fmgr.makeAnd(predDef, equiv);

      predVars.add(var);
    }
    if (predVars.isEmpty()) {
      //stats.numSatCheckAbstractions++;
    }

    // the formula is (abstractionFormula & pathFormula & predDef)
    Formula fm = fmgr.makeAnd(newAbsPf.getFormula(), predDef);
    logger.log(Level.ALL, "COMPUTING ALL-SMT ON FORMULA: ", fm);

    //stats.abstractionTime.startOuter();
    stats.bogus.start();
    AllSatResult allSatResult = thmProver.allSat(fm, predVars, amgr, stats.bogus);
    //long solveTime = stats.abstractionTime.stopOuter();

    // update statistics
    int numModels = allSatResult.getCount();
    if (numModels < Integer.MAX_VALUE) {
      //stats.maxAllSatCount = Math.max(numModels, stats.maxAllSatCount);
      //stats.allSatCount += numModels;
    }


    Region result = allSatResult.getResult();
    logger.log(Level.ALL, "Abstraction computed, result is", result);
    return result;
  }

  /**
   * Checks if (a1 & p1) => a2
   */
  public boolean checkCoverage(AbstractionFormula a1, PathFormula p1, AbstractionFormula a2) {
    Formula absFormula = a1.asFormula();
    Formula symbFormula = p1.getFormula();
    Formula a = fmgr.makeAnd(absFormula, symbFormula);

    Formula b = fmgr.instantiate(a2.asFormula(), p1.getSsa());

    Formula toCheck = fmgr.makeAnd(a, fmgr.makeNot(b));

    thmProver.init();
    try {
      return thmProver.isUnsat(toCheck);
    } finally {
      thmProver.reset();
    }
  }

  /**
   * Checks if an abstraction formula and a pathFormula are unsatisfiable.
   * @param pAbstractionFormula the abstraction formula
   * @param pPathFormula the path formula
   * @return unsat(pAbstractionFormula & pPathFormula)
   */
  public boolean unsat(AbstractionFormula abstractionFormula, PathFormula pathFormula) {
    Formula absFormula = abstractionFormula.asFormula();
    Formula symbFormula = pathFormula.getFormula();
    Formula f = fmgr.makeAnd(absFormula, symbFormula);
    logger.log(Level.ALL, "Checking satisfiability of formula", f);

    thmProver.init();
    try {
      return thmProver.isUnsat(f);
    } finally {
      thmProver.reset();
    }
  }

  public CounterexampleTraceInfo checkPath(List<CFAEdge> pPath) throws CPATransferException {
    PathFormula pathFormula = pmgr.makeEmptyPathFormula();
    for (CFAEdge edge : pPath) {
      pathFormula = pmgr.makeAnd(pathFormula, edge);
    }
    Formula f = pathFormula.getFormula();
    // ignore reachingPathsFormula here because it is just a simple path

    thmProver.init();
    try {
      thmProver.push(f);
      if (thmProver.isUnsat(fmgr.makeTrue())) {
        return new CounterexampleTraceInfo();
      } else {
        return new CounterexampleTraceInfo(Collections.singletonList(f), thmProver.getModel(), ImmutableMap.<Integer, Boolean>of());
      }
    } finally {
      thmProver.reset();
    }
  }

  protected void dumpFormulaToFile(Formula f, File outputFile) {
    try {
      Files.writeFile(outputFile, fmgr.dumpFormula(f));
    } catch (IOException e) {
      logger.log(Level.WARNING,
          "Failed to save formula to file ", outputFile.getPath(), "(", e.getMessage(), ")");
    }
  }

  private static final Joiner LINE_JOINER = Joiner.on('\n');

  protected void printFormulasToFile(Iterable<Formula> f, File outputFile) {
    try {
      Files.writeFile(outputFile, LINE_JOINER.join(f));
    } catch (IOException e) {
      logger.log(Level.WARNING,
          "Failed to save formula to file ", outputFile.getPath(), "(", e.getMessage(), ")");
    }
  }

  // delegate methods

  public Formula toConcrete(Region pRegion) {
    return amgr.toConcrete(pRegion);
  }

  public Collection<AbstractionPredicate> extractPredicates(Region pRegion) {
    try {
      return amgr.extractPredicates(pRegion);
    }
    finally {
    }
  }

  public AbstractionPredicate makeFalsePredicate() {
    return amgr.makeFalsePredicate();
  }

  public AbstractionFormula makeTrueAbstractionFormula(PathFormula pPreviousBlockFormula) {
    return amgr.makeTrueAbstractionFormula(pPreviousBlockFormula);
  }

  public PathFormulaManager getPathFormulaManager() {
    return pmgr;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
  }

  public static class Stats implements Statistics {
    public final Timer buildAbstractionTimer   = new Timer();
    public int buildAbstractionCalls = 0;
    public int buildAbstractionCH    = 0;
    public final Timer buildAbstractionNVTimer   = new Timer();
    public int buildAbstractionNVCalls = 0;
    public int buildAbstractionNVCH    = 0;
    public final Timer bogus           = new Timer();

    @Override
    public void printStatistics(PrintStream out, Result pResult,ReachedSet pReached) {
      long totalTime = this.buildAbstractionTimer.getSumTime() + this.buildAbstractionNVTimer.getSumTime();
      String baHitRation = " ("+toPercent(this.buildAbstractionCH, this.buildAbstractionCalls)+")";
      String banvHitRation = " ("+toPercent(this.buildAbstractionNVCH, this.buildAbstractionNVCalls)+")";
      out.println("buildAbstraction time:           " + this.buildAbstractionTimer);
      out.println("buildAbstraction cach hits:        " + this.buildAbstractionCH+"/"+this.buildAbstractionCalls + baHitRation);
      out.println("buildNextValueAbstraction time:  " + this.buildAbstractionNVTimer);
      out.println("buildBextValueAbstraction c.h.:    " + this.buildAbstractionNVCH+"/"+this.buildAbstractionNVCalls + banvHitRation);
      out.println("time on P.A.Manager:               " + Timer.formatTime(totalTime));
    }

    private String toPercent(double val, double full) {
      return String.format("%1.0f", val/full*100) + "%";
    }

    @Override
    public String getName() {
      return "PredicateAbstractionManager";
    }
  }


}