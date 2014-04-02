/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.predicate;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Predicates.*;
import static com.google.common.collect.FluentIterable.from;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.time.NestedTimer;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicateAbstractionsStorage;
import org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicateAbstractionsStorage.AbstractionNode;
import org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicatePersistenceUtils.PredicateParsingFailedException;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager.RegionCreator;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ProverEnvironment.AllSatResult;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

@Options(prefix = "cpa.predicate")
public class PredicateAbstractionManager {

  static class Stats {

    public int numCallsAbstraction = 0; // total calls
    public int numAbstractionReuses = 0; // total reuses

    public int numSymbolicAbstractions = 0; // precision completely empty, no computation
    public int numSatCheckAbstractions = 0; // precision was {false}, only sat check
    public int numCallsAbstractionCached = 0; // result was cached, no computation

    public int numTotalPredicates = 0;
    public int maxPredicates = 0;
    public int numIrrelevantPredicates = 0;
    public int numTrivialPredicates = 0;
    public int numCartesianAbsPredicates = 0;
    public int numCartesianAbsPredicatesCached = 0;
    public int numBooleanAbsPredicates = 0;
    public final Timer abstractionReuseTime = new Timer();
    public final StatTimer abstractionReuseImplicationTime = new StatTimer("Time for checking reusability of abstractions");
    public final Timer trivialPredicatesTime = new Timer();
    public final Timer cartesianAbstractionTime = new Timer();
    public final Timer booleanAbstractionTime = new Timer();
    public final NestedTimer abstractionEnumTime = new NestedTimer(); // outer: solver time, inner: bdd time
    public final Timer abstractionSolveTime = new Timer(); // only the time for solving, not for model enumeration

    public long allSatCount = 0;
    public int maxAllSatCount = 0;

    public int numPathFormulaCoverageChecks = 0;
    public int numEqualPathFormulae = 0;
    public int numSyntacticEntailedPathFormulae = 0;
    public int numSemanticEntailedPathFormulae = 0;
  }

  final Stats stats = new Stats();

  private final LogManager logger;
  private final FormulaManagerView fmgr;
  private final AbstractionManager amgr;
  private final RegionCreator rmgr;
  private final PathFormulaManager pfmgr;
  private final Solver solver;

  private static final Set<Integer> noAbstractionReuse = ImmutableSet.of();

  private static enum AbstractionType {
    CARTESIAN,
    BOOLEAN,
    COMBINED,
    ;
  }

  @Option(name = "abstraction.cartesian",
      description = "whether to use Boolean (false) or Cartesian (true) abstraction")
  @Deprecated
  private boolean cartesianAbstraction = false;

  @Option(name = "abstraction.computation",
      description = "whether to use Boolean or Cartesian abstraction or both")
  private AbstractionType abstractionType = AbstractionType.BOOLEAN;

  @Option(name = "abstraction.dumpHardQueries",
      description = "dump the abstraction formulas if they took to long")
  private boolean dumpHardAbstractions = false;

  @Option(name = "abstraction.reuseAbstractionsFrom",
      description="An initial set of comptued abstractions that might be reusable")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private Path reuseAbstractionsFrom;

  @Option(description = "Max. number of edge of the abstraction tree to prescan for reuse")
  private int maxAbstractionReusePrescan = 1;

  @Option(name = "abs.useCache", description = "use caching of abstractions")
  private boolean useCache = true;

  @Option(name="refinement.splitItpAtoms",
      description="split each arithmetic equality into two inequalities when extracting predicates from interpolants")
  private boolean splitItpAtoms = false;

  @Option(name = "abstraction.identifyTrivialPredicates",
      description="Identify those predicates where the result is trivially known before abstraction computation and omit them.")
  private boolean identifyTrivialPredicates = false;

  private boolean warnedOfCartesianAbstraction = false;

  private boolean abstractionReuseDisabledBecauseOfAmbiguity = false;

  private final Map<Pair<BooleanFormula, ImmutableSet<AbstractionPredicate>>, AbstractionFormula> abstractionCache;

  // Cache for satisfiability queries: if formula is contained, it is unsat
  private final Set<BooleanFormula> unsatisfiabilityCache;

  //cache for cartesian abstraction queries. For each predicate, the values
  // are -1: predicate is false, 0: predicate is don't care,
  // 1: predicate is true
  private final Map<Pair<BooleanFormula, AbstractionPredicate>, Byte> cartesianAbstractionCache;

  private final BooleanFormulaManagerView bfmgr;

  private final PredicateAbstractionsStorage abstractionStorage;

  public PredicateAbstractionManager(
      AbstractionManager pAmgr,
      FormulaManagerView pFmgr,
      PathFormulaManager pPfmgr,
      Solver pSolver,
      Configuration config,
      LogManager pLogger) throws InvalidConfigurationException, PredicateParsingFailedException {

    config.inject(this, PredicateAbstractionManager.class);

    logger = pLogger;
    fmgr = pFmgr;
    bfmgr = fmgr.getBooleanFormulaManager();
    amgr = pAmgr;
    rmgr = amgr.getRegionCreator();
    pfmgr = pPfmgr;
    solver = pSolver;

    if (cartesianAbstraction) {
      abstractionType = AbstractionType.CARTESIAN;
    }
    if (abstractionType == AbstractionType.COMBINED) {
      warnedOfCartesianAbstraction = true; // warning is not necessary
    }

    if (useCache) {
      abstractionCache = new HashMap<>();
      unsatisfiabilityCache = new HashSet<>();
    } else {
      abstractionCache = null;
      unsatisfiabilityCache = null;
    }

    if (useCache && (abstractionType != AbstractionType.BOOLEAN)) {
      cartesianAbstractionCache = new HashMap<>();
    } else {
      cartesianAbstractionCache = null;
    }

    abstractionStorage = new PredicateAbstractionsStorage(reuseAbstractionsFrom, logger, fmgr);
    SSAMap extractionSsa = SSAMap.emptySSAMap().withDefault(1);
    for (AbstractionNode an : abstractionStorage.getAbstractions().values()) {
      BooleanFormula instanceFm = fmgr.instantiate(an.getFormula(), extractionSsa);
      extractPredicates(instanceFm);
    }
  }

  /**
   * Compute an abstraction of the conjunction of an AbstractionFormula and
   * a PathFormula. The AbstractionFormula will be used in its instantiated form,
   * so the indices there should match those from the PathFormula.
   * @param abstractionFormula An AbstractionFormula that is used as input.
   * @param pathFormula A PathFormula that is used as input.
   * @param predicates The set of predicates used for abstraction.
   * @return An AbstractionFormula instance representing an abstraction of
   *          "abstractionFormula & pathFormula" with pathFormula as the block formula.
   * @throws InterruptedException
   */
  public AbstractionFormula buildAbstraction(CFANode location,
      AbstractionFormula abstractionFormula, PathFormula pathFormula,
      Collection<AbstractionPredicate> pPredicates) throws InterruptedException {

    stats.numCallsAbstraction++;

    logger.log(Level.FINEST, "Computing abstraction", stats.numCallsAbstraction, "with", pPredicates.size(), "predicates");
    logger.log(Level.ALL, "Old abstraction:", abstractionFormula.asFormula());
    logger.log(Level.ALL, "Path formula:", pathFormula);
    logger.log(Level.ALL, "Predicates:", pPredicates);

    BooleanFormula absFormula = abstractionFormula.asInstantiatedFormula();
    BooleanFormula symbFormula = buildFormula(pathFormula.getFormula());
    BooleanFormula f = bfmgr.and(absFormula, symbFormula);
    final SSAMap ssa = pathFormula.getSsa();

    ImmutableSet<AbstractionPredicate> predicates = getRelevantPredicates(pPredicates, f, ssa);

    // Try to reuse stored abstractions
    if (reuseAbstractionsFrom != null
        && !abstractionReuseDisabledBecauseOfAmbiguity) {
      stats.abstractionReuseTime.start();
      ProverEnvironment reuseEnv = solver.newProverEnvironment();
      try {
        reuseEnv.push(f);

        Deque<Pair<Integer, Integer>> tryReuseBasedOnPredecessors = new ArrayDeque<>();
        Set<Integer> idsOfStoredAbstractionReused = abstractionFormula.getIdsOfStoredAbstractionReused();
        for (Integer id: idsOfStoredAbstractionReused) {
          tryReuseBasedOnPredecessors.add(Pair.of(id, 0));
        }

        if (tryReuseBasedOnPredecessors.isEmpty()) {
          tryReuseBasedOnPredecessors.add(Pair.of(abstractionStorage.getRootAbstractionId(), 0));
        }

        while (!tryReuseBasedOnPredecessors.isEmpty()) {
          final Pair<Integer, Integer> tryBasedOn = tryReuseBasedOnPredecessors.pop();
          final int tryBasedOnAbstractionId = tryBasedOn.getFirst();
          final int tryLevel = tryBasedOn.getSecond();

          if (tryLevel > maxAbstractionReusePrescan) {
            continue;
          }

          Set<AbstractionNode> candidateAbstractions = getSuccessorsInAbstractionTree(tryBasedOnAbstractionId);
          Preconditions.checkNotNull(candidateAbstractions);

          //logger.log(Level.WARNING, "Raw candidates based on", tryBasedOnAbstractionId, ":", candidateAbstractions);

          Iterator<AbstractionNode> candidateIterator = candidateAbstractions.iterator();
          while (candidateIterator.hasNext()) {
            AbstractionNode an = candidateIterator.next();
            Preconditions.checkNotNull(an);
            tryReuseBasedOnPredecessors.add(Pair.of(an.getId(), tryLevel + 1));

            if (bfmgr.isTrue(an.getFormula())) {
              candidateIterator.remove();
              continue;
            }

            if (an.getLocationId().isPresent()) {
              if (location.getNodeNumber() != an.getLocationId().get()) {
                candidateIterator.remove();
                continue;
              }
            }
          }

          //logger.log(Level.WARNING, "Filtered candidates", "location", location.getNodeNumber(), "abstraction", tryBasedOnAbstractionId, ":", candidateAbstractions);

          if (candidateAbstractions.size() > 1) {
            logger.log(Level.WARNING, "Too many abstraction candidates on location", location, "for abstraction", tryBasedOnAbstractionId, ". Disabling abstraction reuse!");
            this.abstractionReuseDisabledBecauseOfAmbiguity = true;
            tryReuseBasedOnPredecessors.clear();
            continue;
          }

          Set<Integer> reuseIds = Sets.newTreeSet();
          BooleanFormula reuseFormula = bfmgr.makeBoolean(true);
          for (AbstractionNode an: candidateAbstractions) {
            reuseFormula = bfmgr.and(reuseFormula, an.getFormula());
            abstractionStorage.markAbstractionBeingReused(an.getId());
            reuseIds.add(an.getId());
          }
          BooleanFormula instantiatedReuseFormula = fmgr.instantiate(reuseFormula, ssa);

          stats.abstractionReuseImplicationTime.start();
          reuseEnv.push(bfmgr.not(instantiatedReuseFormula));
          boolean implication = reuseEnv.isUnsat();
          reuseEnv.pop();
          stats.abstractionReuseImplicationTime.stop();

          if (implication) {
            stats.numAbstractionReuses++;

            Region reuseFormulaRegion = buildRegionFromFormula(reuseFormula);
            return new AbstractionFormula(fmgr, reuseFormulaRegion, reuseFormula,
                instantiatedReuseFormula, pathFormula, reuseIds);
          }
        }
      } finally {
        reuseEnv.close();
        stats.abstractionReuseTime.stop();
      }
    }
    // <-- End of reuse

    // Shortcut if the precision is empty
    if (pPredicates.isEmpty()) {
      logger.log(Level.FINEST, "Abstraction", stats.numCallsAbstraction, "with empty precision is true");
      stats.numSymbolicAbstractions++;
      return makeTrueAbstractionFormula(pathFormula);
    }

    // caching
    Pair<BooleanFormula, ImmutableSet<AbstractionPredicate>> absKey = null;
    if (useCache) {
      absKey = Pair.of(f, predicates);
      AbstractionFormula result = abstractionCache.get(absKey);

      if (result != null) {
        // create new abstraction object to have a unique abstraction id

        // instantiate the formula with the current indices
        BooleanFormula stateFormula = result.asFormula();
        BooleanFormula instantiatedFormula = fmgr.instantiate(stateFormula, ssa);

        result = new AbstractionFormula(fmgr, result.asRegion(), stateFormula,
            instantiatedFormula, pathFormula, result.getIdsOfStoredAbstractionReused());
        logger.log(Level.FINEST, "Abstraction", stats.numCallsAbstraction, "was cached");
        logger.log(Level.ALL, "Abstraction result is", result);
        stats.numCallsAbstractionCached++;
        return result;
      }

      boolean unsatisfiable = unsatisfiabilityCache.contains(symbFormula)
                            || unsatisfiabilityCache.contains(f);
      if (unsatisfiable) {
        // block is infeasible
        logger.log(Level.FINEST, "Block feasibility of abstraction", stats.numCallsAbstraction, "was cached and is false.");
        stats.numCallsAbstractionCached++;
        return new AbstractionFormula(fmgr, rmgr.makeFalse(),
            bfmgr.makeBoolean(false), bfmgr.makeBoolean(false),
            pathFormula, noAbstractionReuse);
      }
    }



    // We update statistics here because we want to ignore calls
    // where the result was in the cache.
    stats.numTotalPredicates += pPredicates.size();
    stats.maxPredicates = Math.max(stats.maxPredicates, pPredicates.size());
    stats.numIrrelevantPredicates += pPredicates.size() - predicates.size();

    // Compute result for those predicates
    // where we can trivially identify their truthness in the result
    Region abs = rmgr.makeTrue();
    if (identifyTrivialPredicates) {
      stats.trivialPredicatesTime.start();
      abs = identifyTrivialPredicates(predicates, abstractionFormula, pathFormula);

      // Calculate the set of predicates we still need to use for abstraction.
      predicates = from(predicates)
                     .filter(not(in(amgr.extractPredicates(abs))))
                     .toSet();
      stats.trivialPredicatesTime.stop();
    }

    try (ProverEnvironment thmProver = solver.newProverEnvironment()) {
      thmProver.push(f);

      if (predicates.isEmpty()) {
        stats.numSatCheckAbstractions++;

        stats.abstractionSolveTime.start();
        boolean feasibility;
        try {
          feasibility = !thmProver.isUnsat();
        } finally {
          stats.abstractionSolveTime.stop();
        }

        if (!feasibility) {
          abs = rmgr.makeFalse();
        }

      } else {
        if (abstractionType != AbstractionType.BOOLEAN) {
          // First do cartesian abstraction if desired
          stats.cartesianAbstractionTime.start();
          try {
            abs = rmgr.makeAnd(abs,
                buildCartesianAbstraction(f, ssa, thmProver, predicates));
          } finally {
            stats.cartesianAbstractionTime.stop();
          }
        }

        if (abstractionType == AbstractionType.COMBINED) {
          // Calculate the set of predicates that cartesian abstraction couldn't handle.
          predicates = from(predicates)
                         .filter(not(in(amgr.extractPredicates(abs))))
                         .toSet();
        }

        if (abstractionType != AbstractionType.CARTESIAN
            && !predicates.isEmpty()) {
          // Last do boolean abstraction if desired and necessary
          stats.numBooleanAbsPredicates += predicates.size();
          stats.booleanAbstractionTime.start();
          try {
            abs = rmgr.makeAnd(abs,
                buildBooleanAbstraction(ssa, thmProver, predicates));
          } finally {
            stats.booleanAbstractionTime.stop();
          }

          // Warning:
          // buildBooleanAbstraction() does not clean up thmProver, so do not use it here.
        }
      }
    }

    AbstractionFormula result = makeAbstractionFormula(abs, ssa, pathFormula);

    if (useCache) {
      abstractionCache.put(absKey, result);

      if (result.isFalse()) {
        unsatisfiabilityCache.add(f);
      }
    }

    long abstractionTime = TimeSpan.sum(stats.abstractionSolveTime.getLengthOfLastInterval(),
                                        stats.abstractionEnumTime.getLengthOfLastOuterInterval())
                                   .asMillis();
    logger.log(Level.FINEST, "Computing abstraction took", abstractionTime, "ms");
    logger.log(Level.ALL, "Abstraction result is", result);

    if (dumpHardAbstractions && abstractionTime > 10000) {
      // we want to dump "hard" problems...
      Path dumpFile;

      dumpFile = fmgr.formatFormulaOutputFile("abstraction", stats.numCallsAbstraction, "input", 0);
      fmgr.dumpFormulaToFile(f, dumpFile);

      dumpFile = fmgr.formatFormulaOutputFile("abstraction", stats.numCallsAbstraction, "predicates", 0);
      try (Writer w = dumpFile.asCharSink(Charsets.UTF_8).openBufferedStream()) {
        Joiner.on('\n').appendTo(w, predicates);
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Failed to wrote predicates to file");
      }

      dumpFile = fmgr.formatFormulaOutputFile("abstraction", stats.numCallsAbstraction, "result", 0);
      fmgr.dumpFormulaToFile(result.asInstantiatedFormula(), dumpFile);
    }

    return result;
  }

  /**
   * Extract all relevant predicates (with respect to a given formula)
   * from a given set of predicates.
   *
   * Currently the check is syntactically, i.e.,
   * a predicate is relevant if it refers to at least one variable
   * that also occurs in f.
   *
   * A predicate that is just "false" or "true" is also filtered out.
   *
   * @param pPredicates The set of predicates.
   * @param f The formula that determines which variables and predicates are relevant.
   * @param ssa The SSA map to use for instantiating predicates.
   * @return A subset of pPredicates.
   */
  private ImmutableSet<AbstractionPredicate> getRelevantPredicates(
      final Collection<AbstractionPredicate> pPredicates,
      final BooleanFormula f, final SSAMap ssa) {

    Set<String> variables = fmgr.extractVariables(f);
    ImmutableSet.Builder<AbstractionPredicate> predicateBuilder = ImmutableSet.builder();
    for (AbstractionPredicate predicate : pPredicates) {
      BooleanFormula predicateTerm = predicate.getSymbolicAtom();
      if (bfmgr.isFalse(predicateTerm)) {
        // Ignore predicate "false", it means "check for satisfiability".
        // We do this implicitly.
        logger.log(Level.FINEST, "Ignoring predicate 'false'");
        continue;
      }

      BooleanFormula instantiatedPredicate = fmgr.instantiate(predicateTerm, ssa);
      Set<String> predVariables = fmgr.extractVariables(instantiatedPredicate);

      if (!Sets.intersection(predVariables, variables).isEmpty()) {
        predicateBuilder.add(predicate);
      } else {
        logger.log(Level.FINEST, "Ignoring predicate about variables", predVariables);
      }
    }
    return predicateBuilder.build();
  }

  /**
   * This method finds predicates whose truth value after the
   * abstraction computation is trivially known,
   * and returns a region with these predicates,
   * so that these predicates also do not need to be used in the abstraction computation.
   *
   * @param pPredicates The set of predicates.
   * @param pOldAbs An abstraction formula that determines which variables and predicates are relevant.
   * @param pBlockFormula A path formula that determines which variables and predicates are relevant.
   * @return A region of predicates from pPredicates that is entailed by (pOldAbs & pBlockFormula)
   */
  private Region identifyTrivialPredicates(
      final Collection<AbstractionPredicate> pPredicates,
      final AbstractionFormula pOldAbs, final PathFormula pBlockFormula) throws InterruptedException {

    final SSAMap ssa = pBlockFormula.getSsa();
    final Set<String> blockVariables = fmgr.extractVariables(pBlockFormula.getFormula());
    final Region oldAbs = pOldAbs.asRegion();

    final RegionCreator regionCreator = amgr.getRegionCreator();
    Region region = regionCreator.makeTrue();

    for (final AbstractionPredicate predicate : pPredicates) {
      final BooleanFormula predicateTerm = predicate.getSymbolicAtom();

      BooleanFormula instantiatedPredicate = fmgr.instantiate(predicateTerm, ssa);
      final Set<String> predVariables = fmgr.extractVariables(instantiatedPredicate);

      if (Sets.intersection(predVariables, blockVariables).isEmpty()) {
        // predicate irrelevant with respect to block formula

        final Region predicateVar = predicate.getAbstractVariable();
        if (amgr.entails(oldAbs, predicateVar)) {
          // predicate is unconditionally implied by old abs,
          // we can just copy it to the output
          region = regionCreator.makeAnd(region, predicateVar);
          stats.numTrivialPredicates++;
          logger.log(Level.FINEST, "Predicate", predicate, "is unconditionally true in old abstraction and can be copied to the result.");

        } else {
          final Region negatedPredicateVar = regionCreator.makeNot(predicateVar);
          if (amgr.entails(oldAbs, negatedPredicateVar)) {
            // negated predicate is unconditionally implied by old abs,
            // we can just copy it to the output
            region = regionCreator.makeAnd(region, negatedPredicateVar);
            stats.numTrivialPredicates++;
            logger.log(Level.FINEST, "Negation of predicate", predicate, "is unconditionally true in old abstraction and can be copied to the result.");

          } else {
            // predicate is used in old abs and there is no easy way to handle it
            logger.log(Level.FINEST, "Predicate", predicate, "is relevant because it appears in the old abstraction.");
          }
        }
      }
    }

    assert amgr.entails(oldAbs, region);

    return region;
  }

  /**
   * Compute an abstraction of a single boolean formula.
   * @param f The formula to be abstracted. Needs to be instantiated
   *         with the indices from <code>blockFormula.getSssa()</code>.
   * @param blockFormula A path formula that is not used for the abstraction,
   *         but will be used as the block formula in the resulting AbstractionFormula instance.
   * @param predicates The set of predicates used for abstraction.
   * @return An AbstractionFormula instance representing an abstraction of f
   *          with blockFormula as the block formula.
   */
  public AbstractionFormula buildAbstraction(
      final CFANode location,
      final BooleanFormula f,
      final PathFormula blockFormula,
      final Collection<AbstractionPredicate> predicates) throws InterruptedException {

    PathFormula pf = new PathFormula(f, blockFormula.getSsa(), blockFormula.getPointerTargetSet(), 0);

    AbstractionFormula emptyAbstraction = makeTrueAbstractionFormula(null);
    AbstractionFormula newAbstraction = buildAbstraction(location, emptyAbstraction, pf, predicates);

    // fix block formula in result
    return new AbstractionFormula(fmgr, newAbstraction.asRegion(),
        newAbstraction.asFormula(), newAbstraction.asInstantiatedFormula(),
        blockFormula, noAbstractionReuse);
  }

  /**
   * Create an abstraction from a single boolean formula without actually
   * doing any abstraction computation. The formula is just converted into a
   * region, but the result is equivalent to the input.
   * This can be used to simply view the formula as a region.
   * If BDDs are used, the result of this method is a minimized form of the input.
   * @param f The formula to be converted to a region. Must NOT be instantiated!
   * @param blockFormula A path formula that is not used for the abstraction,
   *         but will be used as the block formula in the resulting AbstractionFormula instance.
   *         Also it's SSAMap will be used for instantiating the result.
   * @return An AbstractionFormula instance representing f
   *          with blockFormula as the block formula.
   */
  public AbstractionFormula buildAbstraction(final BooleanFormula f,
      final PathFormula blockFormula) {
    Region r = amgr.buildRegionFromFormula(f);
    return makeAbstractionFormula(r, blockFormula.getSsa(), blockFormula);
  }

  private Region buildCartesianAbstraction(final BooleanFormula f, final SSAMap ssa,
      ProverEnvironment thmProver, Collection<AbstractionPredicate> predicates) throws InterruptedException {

    stats.abstractionSolveTime.start();
    boolean feasibility = !thmProver.isUnsat();
    stats.abstractionSolveTime.stop();

    if (!feasibility) {
      // abstract post leads to false, we can return immediately
      return rmgr.makeFalse();
    }

    if (!warnedOfCartesianAbstraction && !fmgr.isPurelyConjunctive(f)) {
      logger.log(Level.WARNING,
          "Using cartesian abstraction when formulas contain disjunctions may be imprecise. "
          + "This might lead to failing refinements.");
      warnedOfCartesianAbstraction = true;
    }

    stats.abstractionEnumTime.startOuter();
    try {
      Region absbdd = rmgr.makeTrue();

      // check whether each of the predicate is implied in the next state...

      for (AbstractionPredicate p : predicates) {
        Pair<BooleanFormula, AbstractionPredicate> cacheKey = Pair.of(f, p);
        if (useCache && cartesianAbstractionCache.containsKey(cacheKey)) {
          byte predVal = cartesianAbstractionCache.get(cacheKey);
          stats.numCartesianAbsPredicatesCached++;

          stats.abstractionEnumTime.getCurentInnerTimer().start();
          Region v = p.getAbstractVariable();
          if (predVal == -1) { // pred is false
            stats.numCartesianAbsPredicates++;
            v = rmgr.makeNot(v);
            absbdd = rmgr.makeAnd(absbdd, v);
          } else if (predVal == 1) { // pred is true
            stats.numCartesianAbsPredicates++;
            absbdd = rmgr.makeAnd(absbdd, v);
          } else {
            assert predVal == 0 : "predicate value is neither false, true, nor unknown";
          }
          stats.abstractionEnumTime.getCurentInnerTimer().stop();

        } else {
          logger.log(Level.ALL, "DEBUG_1",
              "CHECKING VALUE OF PREDICATE: ", p.getSymbolicAtom());

          // instantiate the definition of the predicate
          BooleanFormula predTrue = fmgr.instantiate(p.getSymbolicAtom(), ssa);
          BooleanFormula predFalse = bfmgr.not(predTrue);

          // check whether this predicate has a truth value in the next
          // state
          byte predVal = 0; // pred is neither true nor false

          thmProver.push(predFalse);
          boolean isTrue = thmProver.isUnsat();
          thmProver.pop();

          if (isTrue) {
            stats.numCartesianAbsPredicates++;
            stats.abstractionEnumTime.getCurentInnerTimer().start();
            Region v = p.getAbstractVariable();
            absbdd = rmgr.makeAnd(absbdd, v);
            stats.abstractionEnumTime.getCurentInnerTimer().stop();

            predVal = 1;
          } else {
            // check whether it's false...
            thmProver.push(predTrue);
            boolean isFalse = thmProver.isUnsat();
            thmProver.pop();

            if (isFalse) {
              stats.numCartesianAbsPredicates++;
              stats.abstractionEnumTime.getCurentInnerTimer().start();
              Region v = p.getAbstractVariable();
              v = rmgr.makeNot(v);
              absbdd = rmgr.makeAnd(absbdd, v);
              stats.abstractionEnumTime.getCurentInnerTimer().stop();

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
      stats.abstractionEnumTime.stopOuter();
    }
  }

  private BooleanFormula buildFormula(BooleanFormula symbFormula) {

    if (fmgr.useBitwiseAxioms()) {
      BooleanFormula bitwiseAxioms = fmgr.getBitwiseAxioms(symbFormula);
      if (!bfmgr.isTrue(bitwiseAxioms)) {
        symbFormula = bfmgr.and(symbFormula, bitwiseAxioms);

        logger.log(Level.ALL, "DEBUG_3", "ADDED BITWISE AXIOMS:", bitwiseAxioms);
      }
    }

    return symbFormula;
  }

  private Region buildBooleanAbstraction(SSAMap ssa,
      ProverEnvironment thmProver, Collection<AbstractionPredicate> predicates) throws InterruptedException {

    // build the definition of the predicates, and instantiate them
    // also collect all predicate variables so that the solver knows for which
    // variables we want to have the satisfying assignments
    BooleanFormula predDef = bfmgr.makeBoolean(true);
    List<BooleanFormula> predVars = new ArrayList<>(predicates.size());

    for (AbstractionPredicate p : predicates) {
      // get propositional variable and definition of predicate
      BooleanFormula var = p.getSymbolicVariable();
      BooleanFormula def = p.getSymbolicAtom();
      assert !bfmgr.isFalse(def);
      def = fmgr.instantiate(def, ssa);

      // build the formula (var <-> def) and add it to the list of definitions
      BooleanFormula equiv = bfmgr.equivalence(var, def);
      predDef = bfmgr.and(predDef, equiv);

      predVars.add(var);
    }

    // the formula is (abstractionFormula & pathFormula & predDef)
    thmProver.push(predDef);
    AllSatResult allSatResult = thmProver.allSat(predVars, rmgr,
        stats.abstractionSolveTime, stats.abstractionEnumTime);

    // pop() is actually costly sometimes, and we delete the environment anyway
    // thmProver.pop();

    // update statistics
    int numModels = allSatResult.getCount();
    if (numModels < Integer.MAX_VALUE) {
      stats.maxAllSatCount = Math.max(numModels, stats.maxAllSatCount);
      stats.allSatCount += numModels;
    }

    return allSatResult.getResult();
  }

  /**
   * Checks if a1 => a2
   */
  public boolean checkCoverage(AbstractionFormula a1, AbstractionFormula a2) throws InterruptedException {
    return amgr.entails(a1.asRegion(), a2.asRegion());
  }

  /**
   * Checks if (a1 & p1) => a2
   */
  public boolean checkCoverage(AbstractionFormula a1, PathFormula p1, AbstractionFormula a2) throws InterruptedException {
    BooleanFormula absFormula = a1.asInstantiatedFormula();
    BooleanFormula symbFormula = buildFormula(p1.getFormula());
    BooleanFormula a = bfmgr.and(absFormula, symbFormula);

    // get formula of a2 with the indices of p1
    BooleanFormula b = fmgr.instantiate(a2.asFormula(), p1.getSsa());

    return solver.implies(a, b);
  }

  /**
   * Checks whether a1.getFormula() => a2.getFormula() and whether the a1.getSsa()(v) <= a2.getSsa()(v) for all v
   */
  public boolean checkCoverage(PathFormula a1, PathFormula a2, PathFormulaManager pfmgr) throws InterruptedException {
    stats.numPathFormulaCoverageChecks++;

    //handle common special case more efficiently
    if (a1.equals(a2)) {
      stats.numEqualPathFormulae++;
      return true;
    }

    //check ssa maps
    SSAMap map1 = a1.getSsa();
    SSAMap map2 = a2.getSsa();
    for (String var : map1.allVariables()) {
      if (map2.getIndex(var) < map1.getIndex(var)) { return false; }
    }

    //merge path formulae
    PathFormula mergedPathFormulae = pfmgr.makeOr(a1, a2);

    // We need to get a1 with the additional SSA merger terms
    // BooleanFormula leftFormula = getArguments(mergedPathFormulae.getFormula())[0];
    BooleanFormula leftFormula = new ExtractLeftArgumentOfOR(fmgr)
                                       .visit(mergedPathFormulae.getFormula());

    //quick syntactic check
    if (fmgr.checkSyntacticEntails(leftFormula, a2.getFormula())) {
      stats.numSyntacticEntailedPathFormulae++;
      return true;
    }


    //check formulae
    // TODO: should leftFormula be used instead of mergedPathFormulae here?
    if (!solver.implies(mergedPathFormulae.getFormula(), a2.getFormula())) { return false; }
    stats.numSemanticEntailedPathFormulae++;

    return true;
  }

  /**
   * Checks if an abstraction formula and a pathFormula are unsatisfiable.
   * @param pAbstractionFormula the abstraction formula
   * @param pPathFormula the path formula
   * @return unsat(pAbstractionFormula & pPathFormula)
   */
  public boolean unsat(AbstractionFormula abstractionFormula, PathFormula pathFormula) throws InterruptedException {
    BooleanFormula absFormula = abstractionFormula.asInstantiatedFormula();
    BooleanFormula symbFormula = buildFormula(pathFormula.getFormula());
    BooleanFormula f = bfmgr.and(absFormula, symbFormula);
    logger.log(Level.ALL, "Checking satisfiability of formula", f);

    return solver.isUnsat(f);
  }

  public AbstractionFormula makeTrueAbstractionFormula(PathFormula pPreviousBlockFormula) {
    if (pPreviousBlockFormula == null) {
      pPreviousBlockFormula = pfmgr.makeEmptyPathFormula();
    }

    return new AbstractionFormula(fmgr, amgr.getRegionCreator().makeTrue(), bfmgr.makeBoolean(true), bfmgr.makeBoolean(true),
        pPreviousBlockFormula, noAbstractionReuse);
  }

  /**
   * Conjuncts two abstractions.
   * Both need to have the same block formula.
   */
  public AbstractionFormula makeAnd(AbstractionFormula a1, AbstractionFormula a2) {
    checkArgument(a1.getBlockFormula().equals(a2.getBlockFormula()));

    Region region = amgr.getRegionCreator().makeAnd(a1.asRegion(), a2.asRegion());
    BooleanFormula formula = fmgr.makeAnd(a1.asFormula(), a2.asFormula());
    BooleanFormula instantiatedFormula = fmgr.makeAnd(a1.asInstantiatedFormula(), a2.asInstantiatedFormula());

    return new AbstractionFormula(fmgr, region, formula, instantiatedFormula, a1.getBlockFormula(), noAbstractionReuse);
  }

  private AbstractionFormula makeAbstractionFormula(Region abs, SSAMap ssaMap, PathFormula blockFormula) {
    BooleanFormula symbolicAbs = amgr.toConcrete(abs);
    BooleanFormula instantiatedSymbolicAbs = fmgr.instantiate(symbolicAbs, ssaMap);

    return new AbstractionFormula(fmgr, abs, symbolicAbs, instantiatedSymbolicAbs, blockFormula, noAbstractionReuse);
  }

  /**
   * Remove a set of predicates from an abstraction.
   * @param oldAbstraction The abstraction to start from.
   * @param removePredicates The predicate to remove.
   * @param ssaMap The SSAMap to use for instantiating the new abstraction.
   * @return A new abstraction similar to the old one without the predicates.
   */
  public AbstractionFormula reduce(AbstractionFormula oldAbstraction,
      Collection<AbstractionPredicate> removePredicates, SSAMap ssaMap) {
    RegionCreator rmgr = amgr.getRegionCreator();

    Region newRegion = oldAbstraction.asRegion();
    for (AbstractionPredicate predicate : removePredicates) {
      newRegion = rmgr.makeExists(newRegion, predicate.getAbstractVariable());
    }

    return makeAbstractionFormula(newRegion, ssaMap, oldAbstraction.getBlockFormula());
  }

  /**
   * Extend an abstraction by a set of predicates.
   * @param reducedAbstraction The abstraction to extend.
   * @param sourceAbstraction The abstraction where to take the predicates from.
   * @param relevantPredicates The predicates to add.
   * @param newSSA The SSAMap to use for instantiating the new abstraction.
   * @return A new abstraction similar to the old one with some more predicates.
   */
  public AbstractionFormula expand(AbstractionFormula reducedAbstraction, AbstractionFormula sourceAbstraction,
      Collection<AbstractionPredicate> relevantPredicates, SSAMap newSSA) {
    return expand(reducedAbstraction.asRegion(), sourceAbstraction.asRegion(), relevantPredicates, newSSA,
        reducedAbstraction.getBlockFormula());
  }

  /**
   * Extend an abstraction by a set of predicates.
   * @param reducedAbstraction The abstraction to extend.
   * @param sourceAbstraction The abstraction where to take the predicates from.
   * @param relevantPredicates The predicates to add.
   * @param newSSA The SSAMap to use for instantiating the new abstraction.
   * @param blockFormula block formula of reduced abstraction state
   * @return A new abstraction similar to the old one with some more predicates.
   */
  public AbstractionFormula expand(Region reducedAbstraction, Region sourceAbstraction,
      Collection<AbstractionPredicate> relevantPredicates, SSAMap newSSA, PathFormula blockFormula) {
    RegionCreator rmgr = amgr.getRegionCreator();

    for (AbstractionPredicate predicate : relevantPredicates) {
      sourceAbstraction = rmgr.makeExists(sourceAbstraction,
          predicate.getAbstractVariable());
    }

    Region expandedRegion = rmgr.makeAnd(reducedAbstraction, sourceAbstraction);

    return makeAbstractionFormula(expandedRegion, newSSA, blockFormula);
  }

  /**
   * Extract all atoms from a formula and create predicates for them.
   * @param pFormula The formula with the atoms (with SSA indices).
   * @return A (possibly empty) collection of AbstractionPredicates.
   */
  public Collection<AbstractionPredicate> extractPredicates(BooleanFormula pFormula) {
    if (bfmgr.isFalse(pFormula)) {
      return ImmutableList.of(amgr.makeFalsePredicate());
    }

    Collection<BooleanFormula> atoms = fmgr.extractAtoms(pFormula, splitItpAtoms, false);

    List<AbstractionPredicate> preds = new ArrayList<>(atoms.size());

    for (BooleanFormula atom : atoms) {
      preds.add(amgr.makePredicate(atom));
    }

    return preds;
  }

  /**
   * Create a single AbstractionPredicate representing a formula.
   * @param pFormula The formula to use (without SSA indices!), may not simply be "true".
   * @return A single abstraction predicate.
   */
  public AbstractionPredicate createPredicateFor(BooleanFormula pFormula) {
    checkArgument(!bfmgr.isTrue(pFormula));

    return amgr.makePredicate(pFormula);
  }

  // delegate methods

  public Set<AbstractionPredicate> extractPredicates(Region pRegion) {
    return amgr.extractPredicates(pRegion);
  }

  public Region buildRegionFromFormula(BooleanFormula pF) {
    return amgr.buildRegionFromFormula(pF);
  }

  /**
   * This class can be used to extract the left argument of an "or" term.
   * E.g. "x | y" will give "x".
   */
  private static class ExtractLeftArgumentOfOR extends BooleanFormulaManagerView.DefaultBooleanFormulaVisitor<BooleanFormula> {

    private ExtractLeftArgumentOfOR(FormulaManagerView pFmgr) {
      super(pFmgr);
    }

    @Override
    protected BooleanFormula visitOr(BooleanFormula... pOperands) {
      return pOperands[0];
    }
  }

  private Set<AbstractionNode> getSuccessorsInAbstractionTree(int pIdOfLastAbstractionReused) {
    Preconditions.checkNotNull(reuseAbstractionsFrom);
    return abstractionStorage.getSuccessorAbstractions(pIdOfLastAbstractionReused);
  }

}
