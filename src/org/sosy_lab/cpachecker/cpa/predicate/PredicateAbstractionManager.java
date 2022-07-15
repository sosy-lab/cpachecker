// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Predicates.equalTo;

import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.collect.Collections3;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.invariants.InvariantSupplier;
import org.sosy_lab.cpachecker.core.algorithm.invariants.InvariantSupplier.TrivialInvariantSupplier;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackStateEqualsWrapper;
import org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicateAbstractionsStorage;
import org.sosy_lab.cpachecker.cpa.predicate.persistence.PredicateAbstractionsStorage.AbstractionNode;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.regions.Region;
import org.sosy_lab.cpachecker.util.predicates.regions.RegionCreator;
import org.sosy_lab.cpachecker.util.predicates.regions.RegionCreator.RegionBuilder;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.predicates.weakening.InductiveWeakeningManager;
import org.sosy_lab.cpachecker.util.predicates.weakening.WeakeningOptions;
import org.sosy_lab.cpachecker.util.statistics.ThreadSafeTimerContainer.TimerWrapper;
import org.sosy_lab.java_smt.api.BasicProverEnvironment.AllSatCallback;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;

public class PredicateAbstractionManager {

  private final PredicateAbstractionStatistics stats;
  private final PredicateAbstractionManagerOptions options;
  private final LogManager logger;
  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final PredicateAbstractionsStorage abstractionStorage;
  private final AbstractionManager amgr;
  private final RegionCreator rmgr;
  private final PathFormulaManager pfmgr;
  private final Solver solver;
  private final InvariantSupplier invariantSupplier;
  private final @Nullable InductiveWeakeningManager weakeningManager;
  private final ShutdownNotifier shutdownNotifier;

  private static final Set<Integer> noAbstractionReuse = ImmutableSet.of();

  enum AbstractionType {
    CARTESIAN,
    CARTESIAN_BY_WEAKENING,
    BOOLEAN,
    COMBINED,
    ELIMINATION;
  }

  private boolean warnedOfCartesianAbstraction = false;

  private boolean abstractionReuseDisabledBecauseOfAmbiguity = false;

  private final Map<Pair<BooleanFormula, ImmutableSet<BooleanFormula>>, AbstractionFormula>
      abstractionCache;

  // Cache for satisfiability queries: if formula is contained, it is unsat
  private final Set<BooleanFormula> unsatisfiabilityCache;

  // cache for cartesian abstraction queries. For each predicate, the values
  // are -1: predicate is false, 0: predicate is don't care,
  // 1: predicate is true
  private final Map<Pair<BooleanFormula, AbstractionPredicate>, Byte> cartesianAbstractionCache;

  // Statistics
  private final TimerWrapper trivialPredicatesTimer;
  private final TimerWrapper quantifierEliminationTimer;
  private final TimerWrapper abstractionReuseTimer;
  private final TimerWrapper abstractionReuseImplicationTimer;
  private final TimerWrapper abstractionSolveTimer;
  private final TimerWrapper cartesianAbstractionTimer;
  private final TimerWrapper booleanAbstractionTimer;
  private final TimerWrapper abstractionModelEnumTimer;
  private final TimerWrapper abstractionBddConstructionTimer;

  public PredicateAbstractionManager(
      AbstractionManager pAmgr,
      PathFormulaManager pPfmgr,
      Solver pSolver,
      PredicateAbstractionManagerOptions pOptions,
      WeakeningOptions weakeningOptions,
      PredicateAbstractionsStorage pAbstractionStorage,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      PredicateAbstractionStatistics pAbstractionStats,
      InvariantSupplier pInvariantsSupplier) {
    shutdownNotifier = pShutdownNotifier;

    options = pOptions;
    logger = pLogger;
    fmgr = pSolver.getFormulaManager();
    bfmgr = fmgr.getBooleanFormulaManager();
    amgr = pAmgr;
    rmgr = amgr.getRegionCreator();
    pfmgr = pPfmgr;
    solver = pSolver;
    invariantSupplier = pInvariantsSupplier;
    stats = pAbstractionStats;

    if (options.isCartesianAbstraction()) {
      options.setAbstractionType(AbstractionType.CARTESIAN);
    }
    if (options.getAbstractionType() == AbstractionType.COMBINED) {
      warnedOfCartesianAbstraction = true; // warning is not necessary
    }
    if (options.getAbstractionType() == AbstractionType.CARTESIAN_BY_WEAKENING) {
      weakeningManager =
          new InductiveWeakeningManager(weakeningOptions, pSolver, pLogger, pShutdownNotifier);
    } else {
      weakeningManager = null;
    }

    if (options.isUseCache()) {
      abstractionCache = new HashMap<>();
      unsatisfiabilityCache = new HashSet<>();
    } else {
      abstractionCache = null;
      unsatisfiabilityCache = null;
    }

    if (options.isUseCache() && (options.getAbstractionType() != AbstractionType.BOOLEAN)) {
      cartesianAbstractionCache = new HashMap<>();
    } else {
      cartesianAbstractionCache = null;
    }

    abstractionStorage = pAbstractionStorage;

    trivialPredicatesTimer = stats.trivialPredicatesTime.getNewTimer();
    quantifierEliminationTimer = stats.quantifierEliminationTime.getNewTimer();
    abstractionReuseTimer = stats.abstractionReuseTime.getNewTimer();
    abstractionReuseImplicationTimer = stats.abstractionReuseImplicationTime.getNewTimer();
    abstractionSolveTimer = stats.abstractionSolveTime.getNewTimer();
    cartesianAbstractionTimer = stats.cartesianAbstractionTime.getNewTimer();
    booleanAbstractionTimer = stats.booleanAbstractionTime.getNewTimer();
    abstractionModelEnumTimer = stats.abstractionModelEnumTime.getNewTimer();
    abstractionBddConstructionTimer = stats.abstractionBddConstructionTime.getNewTimer();
  }

  /**
   * Compute an abstraction of a single boolean formula.
   *
   * @param f The formula to be abstracted. Needs to be instantiated with the indices from <code>
   *     blockFormula.getSssa()</code>.
   * @param blockFormula A path formula that is not used for the abstraction, but will be used as
   *     the block formula in the resulting AbstractionFormula instance.
   * @param predicates The set of predicates used for abstraction.
   * @return An AbstractionFormula instance representing an abstraction of f with blockFormula as
   *     the block formula.
   */
  public AbstractionFormula buildAbstraction(
      final CFANode location,
      Optional<CallstackStateEqualsWrapper> callstackInformation,
      final BooleanFormula f,
      final PathFormula blockFormula,
      final Collection<AbstractionPredicate> predicates)
      throws SolverException, InterruptedException {

    @SuppressWarnings("deprecation") // just faking a PF to be able to reuse one of our methods
    PathFormula pf =
        PathFormula.createManually(f, blockFormula.getSsa(), blockFormula.getPointerTargetSet(), 0);

    AbstractionFormula emptyAbstraction = makeTrueAbstractionFormula(null);
    AbstractionFormula newAbstraction =
        buildAbstraction(
            Collections.singleton(location),
            callstackInformation,
            emptyAbstraction,
            pf,
            predicates);

    // fix block formula in result
    return new AbstractionFormula(
        fmgr,
        newAbstraction.asRegion(),
        newAbstraction.asFormula(),
        newAbstraction.asInstantiatedFormula(),
        blockFormula,
        noAbstractionReuse);
  }

  public void clear() {
    if (options.isUseCache()) {
      abstractionCache.clear();
      unsatisfiabilityCache.clear();
    }
  }
  /**
   * Compute an abstraction of the conjunction of an AbstractionFormula and a PathFormula. The
   * AbstractionFormula will be used in its instantiated form, so the indices there should match
   * those from the PathFormula.
   *
   * @param abstractionFormula An AbstractionFormula that is used as input.
   * @param pathFormula A PathFormula that is used as input.
   * @param pPredicates The set of predicates used for abstraction.
   * @return An AbstractionFormula instance representing an abstraction of "abstractionFormula &
   *     pathFormula" with pathFormula as the block formula.
   */
  public AbstractionFormula buildAbstraction(
      final Collection<CFANode> locations,
      Optional<CallstackStateEqualsWrapper> callstackInformation,
      final AbstractionFormula abstractionFormula,
      final PathFormula pathFormula,
      final Collection<AbstractionPredicate> pPredicates)
      throws SolverException, InterruptedException {

    int currentAbstractionId = stats.numCallsAbstraction.getAndIncrement();

    logger.log(
        Level.FINEST,
        "Computing abstraction",
        currentAbstractionId,
        "with",
        pPredicates.size(),
        "predicates");
    logger.log(Level.ALL, "Old abstraction:", abstractionFormula.asFormula());
    logger.log(Level.ALL, "Path formula:", pathFormula);
    logger.log(Level.ALL, "Predicates:", pPredicates);

    final BooleanFormula absFormula = abstractionFormula.asInstantiatedFormula();
    final BooleanFormula symbFormula = getFormulaFromPathFormula(pathFormula);
    BooleanFormula primaryFormula = bfmgr.and(absFormula, symbFormula);
    final SSAMap ssa = pathFormula.getSsa();

    // Try to reuse stored abstractions
    if (options.getReuseAbstractionsFrom() != null && !abstractionReuseDisabledBecauseOfAmbiguity) {
      // TODO we do not yet support multiple CFA nodes per abstraction here
      // and choosing *one* location is best way for backwards compatibility.
      AbstractionFormula reused =
          reuseAbstractionIfPossible(
              abstractionFormula, pathFormula, primaryFormula, Iterables.getOnlyElement(locations));
      if (reused != null) {
        return reused;
      }
    }

    // Shortcut if the precision is empty
    if (pPredicates.isEmpty() && (options.getAbstractionType() != AbstractionType.ELIMINATION)) {
      logger.log(Level.FINEST, "Abstraction", currentAbstractionId, "with empty precision is true");
      stats.numSymbolicAbstractions.incrementAndGet();
      return makeTrueAbstractionFormula(pathFormula);
    }

    final Function<BooleanFormula, BooleanFormula> instantiator =
        pred -> fmgr.instantiate(pred, ssa);

    // This is the (mutable) set of remaining predicates that still need to be handled.
    // Each step of our abstraction computation may be able to handle some predicates,
    // and should remove those from this set afterwards.
    final Collection<AbstractionPredicate> remainingPredicates =
        getRelevantPredicates(pPredicates, primaryFormula, instantiator);

    if (fmgr.useBitwiseAxioms()) {
      for (AbstractionPredicate predicate : remainingPredicates) {
        primaryFormula =
            pfmgr.addBitwiseAxiomsIfNeeded(primaryFormula, predicate.getSymbolicAtom());
      }
    }

    final BooleanFormula f = primaryFormula;

    // caching
    Pair<BooleanFormula, ImmutableSet<BooleanFormula>> absKey = null;
    if (options.isUseCache()) {
      ImmutableSet<BooleanFormula> instantiatedPreds =
          Collections3.transformedImmutableSetCopy(
              remainingPredicates, pred -> instantiator.apply(pred.getSymbolicAtom()));
      absKey = Pair.of(f, instantiatedPreds);
      AbstractionFormula result = abstractionCache.get(absKey);

      if (result != null) {
        // create new abstraction object to have a unique abstraction id

        // instantiate the formula with the current indices
        BooleanFormula stateFormula = result.asFormula();
        BooleanFormula instantiatedFormula = fmgr.instantiate(stateFormula, ssa);

        result =
            new AbstractionFormula(
                fmgr,
                result.asRegion(),
                stateFormula,
                instantiatedFormula,
                pathFormula,
                result.getIdsOfStoredAbstractionReused());
        logger.log(Level.FINEST, "Abstraction", currentAbstractionId, "was cached");
        logger.log(Level.ALL, "Abstraction result is", result.asFormula());
        stats.numCallsAbstractionCached.incrementAndGet();
        return result;
      }

      boolean unsatisfiable =
          unsatisfiabilityCache.contains(symbFormula) || unsatisfiabilityCache.contains(f);
      if (unsatisfiable) {
        // block is infeasible
        logger.log(
            Level.FINEST,
            "Block feasibility of abstraction",
            currentAbstractionId,
            "was cached and is false.");
        stats.numCallsAbstractionCached.incrementAndGet();
        return new AbstractionFormula(
            fmgr,
            rmgr.makeFalse(),
            bfmgr.makeFalse(),
            bfmgr.makeFalse(),
            pathFormula,
            noAbstractionReuse);
      }
    }

    // Compute result for those predicates
    // where we can trivially identify their truthness in the result
    Region abs = rmgr.makeTrue();
    if (options.isIdentifyTrivialPredicates()) {
      trivialPredicatesTimer.start();
      abs = handleTrivialPredicates(remainingPredicates, abstractionFormula, pathFormula);
      trivialPredicatesTimer.stop();
    }

    // add invariants to abstraction formula if available
    if (invariantSupplier != TrivialInvariantSupplier.INSTANCE) {
      // TODO we do not yet support multiple CFA nodes per abstraction here
      // and choosing *one* location is best way for backwards compatibility.
      for (CFANode location : locations) {
        BooleanFormula invariant =
            invariantSupplier.getInvariantFor(
                location, callstackInformation, fmgr, pfmgr, pathFormula);

        if (!bfmgr.isTrue(invariant)) {
          AbstractionPredicate absPred = amgr.makePredicate(invariant);
          abs = rmgr.makeAnd(abs, absPred.getAbstractVariable());

          // Calculate the set of predicates we still need to use for abstraction.
          Iterables.removeIf(remainingPredicates, equalTo(absPred));
        }
      }
    }

    if (options.getAbstractionType() == AbstractionType.ELIMINATION) {
      quantifierEliminationTimer.start();
      try {
        BooleanFormula eliminationResult = fmgr.uninstantiate(fmgr.eliminateDeadVariables(f, ssa));
        abs = rmgr.makeAnd(abs, amgr.convertFormulaToRegion(eliminationResult));
      } finally {
        quantifierEliminationTimer.stop();
      }
    } else if (options.getAbstractionType() == AbstractionType.CARTESIAN_BY_WEAKENING) {
      abs = rmgr.makeAnd(abs, buildCartesianAbstractionUsingWeakening(f, ssa, remainingPredicates));

    } else {
      abs = rmgr.makeAnd(abs, computeAbstraction(f, remainingPredicates, instantiator));
    }

    AbstractionFormula result = makeAbstractionFormula(abs, ssa, pathFormula);

    if (options.isUseCache()) {
      abstractionCache.put(absKey, result);

      if (result.isFalse()) {
        unsatisfiabilityCache.add(f);
      }
    }

    long abstractionTime =
        TimeSpan.sum(
                abstractionSolveTimer.getLengthOfLastInterval(),
                abstractionModelEnumTimer.getLengthOfLastInterval())
            .asMillis();
    logger.log(Level.FINEST, "Computing abstraction took", abstractionTime, "ms");
    logger.log(Level.ALL, "Abstraction result is", result.asFormula());

    if (options.isDumpHardAbstractions() && abstractionTime > 10000) {
      // we want to dump "hard" problems...
      dumpAbstractionProblem(f, pPredicates, result, currentAbstractionId);
    }

    return result;
  }

  /**
   * Compute an abstraction of a formula. This is a low-level version of {@link
   * #buildAbstraction(CFANode, Optional, BooleanFormula, PathFormula, Collection)}: it does not
   * handle instantiation and does not return an {@link AbstractionFormula} but just a {@link
   * BooleanFormula}. It also misses several of the optimizations and features of {@link
   * #buildAbstraction(CFANode, Optional, BooleanFormula, PathFormula, Collection)}, so if possible
   * use that method.
   *
   * @param pF The formula to be abstracted. Must not be instantiated.
   * @param pPredicates The set of predicates to use for abstraction.
   * @return An over-approximation of pF using the predicates from pPredicates.
   */
  public BooleanFormula computeAbstraction(
      final BooleanFormula pF, final Collection<AbstractionPredicate> pPredicates)
      throws InterruptedException, SolverException {
    stats.numCallsAbstraction.getAndIncrement();

    if (pPredicates.isEmpty()) {
      stats.numSymbolicAbstractions.incrementAndGet();
      return bfmgr.makeTrue();
    }

    if (unsatisfiabilityCache.contains(pF)) {
      stats.numCallsAbstractionCached.incrementAndGet();
      return bfmgr.makeFalse();
    }

    final Function<BooleanFormula, BooleanFormula> dummyInstantiator = Functions.identity();

    final Collection<AbstractionPredicate> predicates =
        getRelevantPredicates(pPredicates, pF, dummyInstantiator);

    Region abs = computeAbstraction(pF, predicates, dummyInstantiator);

    BooleanFormula symbolicAbs = amgr.convertRegionToFormula(abs);

    if (options.isSimplifyAbstractionFormula()) {
      symbolicAbs = fmgr.simplify(symbolicAbs);
    }

    if (bfmgr.isFalse(symbolicAbs)) {
      unsatisfiabilityCache.add(pF);
    }

    return symbolicAbs;
  }

  private BooleanFormula getFormulaFromPathFormula(PathFormula pathFormula) {
    BooleanFormula symbFormula = pathFormula.getFormula();

    return pfmgr.addBitwiseAxiomsIfNeeded(symbFormula, symbFormula);
  }

  private @Nullable AbstractionFormula reuseAbstractionIfPossible(
      final AbstractionFormula abstractionFormula,
      final PathFormula pathFormula,
      final BooleanFormula f,
      final CFANode location)
      throws SolverException, InterruptedException {
    abstractionReuseTimer.start();
    try (ProverEnvironment reuseEnv = solver.newProverEnvironment()) {
      reuseEnv.push(f);

      Deque<Pair<Integer, Integer>> tryReuseBasedOnPredecessors = new ArrayDeque<>();
      Set<Integer> idsOfStoredAbstractionReused =
          abstractionFormula.getIdsOfStoredAbstractionReused();
      for (Integer id : idsOfStoredAbstractionReused) {
        tryReuseBasedOnPredecessors.add(Pair.of(id, 0));
      }

      if (tryReuseBasedOnPredecessors.isEmpty()) {
        tryReuseBasedOnPredecessors.add(Pair.of(abstractionStorage.getRootAbstractionId(), 0));
      }

      while (!tryReuseBasedOnPredecessors.isEmpty()) {
        final Pair<Integer, Integer> tryBasedOn = tryReuseBasedOnPredecessors.pop();
        final int tryBasedOnAbstractionId = tryBasedOn.getFirst();
        final int tryLevel = tryBasedOn.getSecond();

        if (tryLevel > options.getMaxAbstractionReusePrescan()) {
          continue;
        }

        Set<AbstractionNode> candidateAbstractions =
            abstractionStorage.getSuccessorAbstractions(tryBasedOnAbstractionId);
        Preconditions.checkNotNull(candidateAbstractions);

        // logger.log(Level.WARNING, "Raw candidates based on", tryBasedOnAbstractionId, ":",
        // candidateAbstractions);

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
            if (location.getNodeNumber() != an.getLocationId().orElseThrow()) {
              candidateIterator.remove();
              continue;
            }
          }
        }

        // logger.log(Level.WARNING, "Filtered candidates", "location", location.getNodeNumber(),
        // "abstraction", tryBasedOnAbstractionId, ":", candidateAbstractions);

        if (candidateAbstractions.size() > 1) {
          logger.log(
              Level.WARNING,
              "Too many abstraction candidates on location",
              location,
              "for abstraction",
              tryBasedOnAbstractionId,
              ". Disabling abstraction reuse!");
          abstractionReuseDisabledBecauseOfAmbiguity = true;
          tryReuseBasedOnPredecessors.clear();
          continue;
        } else if (candidateAbstractions.isEmpty()) {
          continue;
        }

        Set<Integer> reuseIds = new TreeSet<>();
        BooleanFormula reuseFormula = bfmgr.makeTrue();
        for (AbstractionNode an : candidateAbstractions) {
          reuseFormula = bfmgr.and(reuseFormula, an.getFormula());
          abstractionStorage.markAbstractionBeingReused(an.getId());
          reuseIds.add(an.getId());
        }
        BooleanFormula instantiatedReuseFormula =
            fmgr.instantiate(reuseFormula, pathFormula.getSsa());

        abstractionReuseImplicationTimer.start();
        reuseEnv.push(bfmgr.not(instantiatedReuseFormula));
        boolean implication = reuseEnv.isUnsat();
        reuseEnv.pop();
        abstractionReuseImplicationTimer.stop();

        if (implication) {
          stats.numAbstractionReuses.incrementAndGet();

          Region reuseFormulaRegion = amgr.convertFormulaToRegion(reuseFormula);
          return new AbstractionFormula(
              fmgr,
              reuseFormulaRegion,
              reuseFormula,
              instantiatedReuseFormula,
              pathFormula,
              reuseIds);
        }
      }
    } finally {
      abstractionReuseTimer.stop();
    }
    return null; // no abstraction could be reused
  }

  /**
   * Extract all relevant predicates (with respect to a given formula) from a given set of
   * predicates.
   *
   * <p>Currently the check is syntactically, i.e., a predicate is relevant if it refers to at least
   * one variable that also occurs in f.
   *
   * <p>A predicate that is just "false" or "true" is also filtered out.
   *
   * @param pPredicates The set of predicates.
   * @param f The formula that determines which variables and predicates are relevant.
   * @param instantiator A function that will be applied to instantiate each abstraction predicate.
   * @return A subset of pPredicates.
   */
  private Collection<AbstractionPredicate> getRelevantPredicates(
      final Collection<AbstractionPredicate> pPredicates,
      final BooleanFormula f,
      final Function<BooleanFormula, BooleanFormula> instantiator) {

    Set<String> variables = fmgr.extractVariableNames(f);
    // LinkedList keeps order (important to avoid non-determinism) and supports efficient removal.
    @SuppressWarnings("JdkObsolete")
    Collection<AbstractionPredicate> relevantPredicates = new LinkedList<>();

    for (AbstractionPredicate predicate : pPredicates) {
      final BooleanFormula predicateTerm = predicate.getSymbolicAtom();
      if (bfmgr.isFalse(predicateTerm)) {
        // Ignore predicate "false", it means "check for satisfiability".
        // We do this implicitly.
        logger.log(Level.FINEST, "Ignoring predicate 'false'");
        continue;
      }

      BooleanFormula instantiatedPredicate = instantiator.apply(predicateTerm);
      Set<String> predVariables = fmgr.extractVariableNames(instantiatedPredicate);

      if (predVariables.isEmpty() || !Sets.intersection(predVariables, variables).isEmpty()) {
        // Predicates without variables occur (for example, talking about UFs).
        // We do not know whether they are relevant, so we have to add them.
        relevantPredicates.add(predicate);

      } else {
        logger.log(Level.FINEST, "Ignoring predicate about variables", predVariables);
      }
    }

    stats.numTotalPredicates.addAndGet(pPredicates.size());
    stats.maxPredicates.accumulateAndGet(pPredicates.size(), Math::max);
    stats.numIrrelevantPredicates.addAndGet(pPredicates.size() - relevantPredicates.size());

    return relevantPredicates;
  }

  /**
   * This method finds predicates whose truth value after the abstraction computation is trivially
   * known, and returns a region with these predicates, so that these predicates also do not need to
   * be used in the abstraction computation.
   *
   * @param pPredicates The set of predicates. Each predicate that is handled will be removed from
   *     the set.
   * @param pOldAbs An abstraction formula that determines which variables and predicates are
   *     relevant.
   * @param pBlockFormula A path formula that determines which variables and predicates are
   *     relevant.
   * @return A region of predicates from pPredicates that is entailed by (pOldAbs & pBlockFormula)
   */
  private Region handleTrivialPredicates(
      final Collection<AbstractionPredicate> pPredicates,
      final AbstractionFormula pOldAbs,
      final PathFormula pBlockFormula)
      throws SolverException, InterruptedException {

    final SSAMap ssa = pBlockFormula.getSsa();
    final Set<String> blockVariables = fmgr.extractVariableNames(pBlockFormula.getFormula());
    final Region oldAbs = pOldAbs.asRegion();

    final RegionCreator regionCreator = amgr.getRegionCreator();
    Region region = regionCreator.makeTrue();

    final Iterator<AbstractionPredicate> predicateIt = pPredicates.iterator();
    while (predicateIt.hasNext()) {
      final AbstractionPredicate predicate = predicateIt.next();
      final BooleanFormula predicateTerm = predicate.getSymbolicAtom();

      BooleanFormula instantiatedPredicate = fmgr.instantiate(predicateTerm, ssa);
      final Set<String> predVariables = fmgr.extractVariableNames(instantiatedPredicate);

      if (Sets.intersection(predVariables, blockVariables).isEmpty()) {
        // predicate irrelevant with respect to block formula

        final Region predicateVar = predicate.getAbstractVariable();
        if (amgr.entails(oldAbs, predicateVar)) {
          // predicate is unconditionally implied by old abs,
          // we can just copy it to the output
          region = regionCreator.makeAnd(region, predicateVar);
          predicateIt.remove(); // mark predicate as handled
          stats.numTrivialPredicates.incrementAndGet();
          logger.log(
              Level.FINEST,
              "Predicate",
              predicate,
              "is unconditionally true in old abstraction and can be copied to the result.");

        } else {
          final Region negatedPredicateVar = regionCreator.makeNot(predicateVar);
          if (amgr.entails(oldAbs, negatedPredicateVar)) {
            // negated predicate is unconditionally implied by old abs,
            // we can just copy it to the output
            region = regionCreator.makeAnd(region, negatedPredicateVar);
            predicateIt.remove(); // mark predicate as handled
            stats.numTrivialPredicates.incrementAndGet();
            logger.log(
                Level.FINEST,
                "Negation of predicate",
                predicate,
                "is unconditionally true in old abstraction and can be copied to the result.");

          } else {
            // predicate is used in old abs and there is no easy way to handle it
            logger.log(
                Level.FINEST,
                "Predicate",
                predicate,
                "is relevant because it appears in the old abstraction.");
          }
        }
      }
    }

    assert amgr.entails(oldAbs, region);

    return region;
  }

  /**
   * Actually compute an abstraction of a formula, without fancy caching etc.
   *
   * @param f The formula to be abstracted.
   * @param remainingPredicates The set of predicates. Each predicate that is handled will be
   *     removed from the set.
   * @param instantiator A function that will be applied to instantiate each abstraction predicate,
   *     should yield the same SSA indices that f has (or none, if f has no SSA indices).
   * @return An over-approximation of f using the predicates from remainingPredicates.
   */
  private Region computeAbstraction(
      final BooleanFormula f,
      final Collection<AbstractionPredicate> remainingPredicates,
      final Function<BooleanFormula, BooleanFormula> instantiator)
      throws SolverException, InterruptedException {
    Region abs = rmgr.makeTrue();

    try (ProverEnvironment thmProver =
        solver.newProverEnvironment(ProverOptions.GENERATE_ALL_SAT)) {
      thmProver.push(f);

      if (remainingPredicates.isEmpty()) {
        stats.numSatCheckAbstractions.incrementAndGet();

        abstractionSolveTimer.start();
        boolean feasibility;
        try {
          feasibility = !thmProver.isUnsat();
        } finally {
          abstractionSolveTimer.stop();
        }

        if (!feasibility) {
          abs = rmgr.makeFalse();
        }

      } else {
        if (options.getAbstractionType() != AbstractionType.BOOLEAN) {
          // First do cartesian abstraction if desired
          cartesianAbstractionTimer.start();
          try {
            abs =
                rmgr.makeAnd(
                    abs,
                    computeCartesianAbstraction(f, thmProver, remainingPredicates, instantiator));
          } finally {
            cartesianAbstractionTimer.stop();
          }
        }

        if (options.getAbstractionType() != AbstractionType.CARTESIAN
            && !remainingPredicates.isEmpty()) {
          // Last do boolean abstraction if desired and necessary
          stats.numBooleanAbsPredicates.addAndGet(remainingPredicates.size());
          booleanAbstractionTimer.start();
          try {
            abs =
                rmgr.makeAnd(
                    abs, computeBooleanAbstraction(thmProver, remainingPredicates, instantiator));
          } finally {
            booleanAbstractionTimer.stop();
          }

          // Warning:
          // buildBooleanAbstraction() does not clean up thmProver, so do not use it here.
          // remainingPredicates is now empty.
        }
      }
    }
    return abs;
  }

  /**
   * Compute a Cartesian abstraction of a formula given a set of predicates. The abstracted formula
   * is expected to have been pushed onto the solver stack already.
   *
   * @param f The (instantiated) formula to abstract, only used as cache key.
   * @param thmProver The solver to use with the input formula on the stack.
   * @param pPredicates The set of predicates. Each predicate that is handled will be removed from
   *     the set.
   * @param instantiator A function that will be applied to instantiate each abstraction predicate.
   * @return A over-approximation of f.
   */
  private Region computeCartesianAbstraction(
      final BooleanFormula f,
      final ProverEnvironment thmProver,
      final Collection<AbstractionPredicate> pPredicates,
      final Function<BooleanFormula, BooleanFormula> instantiator)
      throws SolverException, InterruptedException {

    abstractionSolveTimer.start();
    boolean feasibility = !thmProver.isUnsat();
    abstractionSolveTimer.stop();

    if (!feasibility) {
      // abstract post leads to false, we can return immediately
      return rmgr.makeFalse();
    }

    if (!warnedOfCartesianAbstraction && !fmgr.isPurelyConjunctive(f)) {
      logger.log(
          Level.WARNING,
          "Using cartesian abstraction when formulas contain disjunctions may be imprecise. "
              + "This might lead to failing refinements.");
      warnedOfCartesianAbstraction = true;
    }

    abstractionModelEnumTimer.start();
    try {
      Region absbdd = rmgr.makeTrue();

      // check whether each of the predicate is implied in the next state...

      final Iterator<AbstractionPredicate> predicateIt = pPredicates.iterator();
      while (predicateIt.hasNext()) {
        final AbstractionPredicate p = predicateIt.next();
        Pair<BooleanFormula, AbstractionPredicate> cacheKey = Pair.of(f, p);
        if (options.isUseCache() && cartesianAbstractionCache.containsKey(cacheKey)) {
          byte predVal = cartesianAbstractionCache.get(cacheKey);
          stats.numCartesianAbsPredicatesCached.incrementAndGet();

          abstractionBddConstructionTimer.start();
          Region v = p.getAbstractVariable();
          if (predVal == -1) { // pred is false
            stats.numCartesianAbsPredicates.incrementAndGet();
            v = rmgr.makeNot(v);
            absbdd = rmgr.makeAnd(absbdd, v);
          } else if (predVal == 1) { // pred is true
            stats.numCartesianAbsPredicates.incrementAndGet();
            absbdd = rmgr.makeAnd(absbdd, v);
          } else {
            assert predVal == 0 : "predicate value is neither false, true, nor unknown";
          }
          abstractionBddConstructionTimer.stop();

        } else {
          logger.log(Level.ALL, "DEBUG_1", "CHECKING VALUE OF PREDICATE: ", p.getSymbolicAtom());

          // instantiate the definition of the predicate
          BooleanFormula predTrue = instantiator.apply(p.getSymbolicAtom());
          BooleanFormula predFalse = bfmgr.not(predTrue);

          // check whether this predicate has a truth value in the next
          // state
          byte predVal = 0; // pred is neither true nor false

          thmProver.push(predFalse);
          boolean isTrue = thmProver.isUnsat();
          thmProver.pop();

          if (isTrue) {
            stats.numCartesianAbsPredicates.incrementAndGet();
            abstractionBddConstructionTimer.start();
            Region v = p.getAbstractVariable();
            absbdd = rmgr.makeAnd(absbdd, v);
            predicateIt.remove(); // mark predicate as handled
            abstractionBddConstructionTimer.stop();

            predVal = 1;
          } else {
            // check whether it's false...
            thmProver.push(predTrue);
            boolean isFalse = thmProver.isUnsat();
            thmProver.pop();

            if (isFalse) {
              stats.numCartesianAbsPredicates.incrementAndGet();
              abstractionBddConstructionTimer.start();
              Region v = p.getAbstractVariable();
              v = rmgr.makeNot(v);
              absbdd = rmgr.makeAnd(absbdd, v);
              predicateIt.remove(); // mark predicate as handled
              abstractionBddConstructionTimer.stop();

              predVal = -1;
            }
          }

          if (options.isUseCache()) {
            cartesianAbstractionCache.put(cacheKey, predVal);
          }
        }
      }

      return absbdd;

    } finally {
      abstractionModelEnumTimer.stop();
      abstractionBddConstructionTimer.stopIfRunning();
    }
  }

  /** Build cartesian abstraction using the inductive weakening approach. */
  private Region buildCartesianAbstractionUsingWeakening(
      final BooleanFormula f, final SSAMap ssa, final Collection<AbstractionPredicate> pPredicates)
      throws SolverException, InterruptedException {

    abstractionSolveTimer.start();
    boolean feasibility;
    try (ProverEnvironment thmProver = solver.newProverEnvironment()) {
      thmProver.push(f);
      feasibility = !thmProver.isUnsat();
    } finally {
      abstractionSolveTimer.stop();
    }

    if (!feasibility) {
      // abstract post leads to false, we can return immediately
      return rmgr.makeFalse();
    }

    ImmutableMap.Builder<BooleanFormula, Region> infoBuilder = ImmutableMap.builder();
    for (AbstractionPredicate a : pPredicates) {
      BooleanFormula lemma = a.getSymbolicAtom();
      Region r = a.getAbstractVariable();
      infoBuilder.put(lemma, r);
      // BooleanFormula negated = bfmgr.not(lemma);
      // info.put(negated, rmgr.makeNot(r));
    }

    Map<BooleanFormula, Region> info = infoBuilder.buildOrThrow();
    Set<BooleanFormula> toStateLemmas = info.keySet();
    Set<BooleanFormula> filteredLemmas;
    @SuppressWarnings("deprecation")
    // safe here because weakeningManager cares only about formula and SSAMap
    PathFormula pf =
        PathFormula.createManually(f, ssa, PointerTargetSet.emptyPointerTargetSet(), 0);
    cartesianAbstractionTimer.start();
    try {
      filteredLemmas =
          weakeningManager.findInductiveWeakeningForRCNF(
              SSAMap.emptySSAMap(), ImmutableSet.of(), pf, toStateLemmas);
    } finally {
      cartesianAbstractionTimer.stop();
    }

    Region out = rmgr.makeTrue();
    for (BooleanFormula lemma : filteredLemmas) {
      out = rmgr.makeAnd(out, info.get(lemma));
    }

    return out;
  }

  /**
   * Compute a Boolean abstraction of a formula given a set of predicates. The abstracted formula is
   * expected to have been pushed onto the solver stack already.
   *
   * @param thmProver The solver to use with the input formula on the stack.
   * @param predicates The set of predicates. Each predicate that is handled will be removed from
   *     the set (and Boolean abstraction handles all predicates so the set is empty afterwards!).
   * @param instantiator A function that will be applied to instantiate each abstraction predicate.
   * @return A over-approximation of f.
   */
  private Region computeBooleanAbstraction(
      final ProverEnvironment thmProver,
      final Collection<AbstractionPredicate> predicates,
      final Function<BooleanFormula, BooleanFormula> instantiator)
      throws InterruptedException, SolverException {

    // build the definition of the predicates, and instantiate them
    // also collect all predicate variables so that the solver knows for which
    // variables we want to have the satisfying assignments
    BooleanFormula predDef = bfmgr.makeTrue();
    List<BooleanFormula> predVars = new ArrayList<>(predicates.size());

    for (AbstractionPredicate p : predicates) {
      // get propositional variable and definition of predicate
      BooleanFormula var = p.getSymbolicVariable();
      final BooleanFormula def = instantiator.apply(p.getSymbolicAtom());
      assert !bfmgr.isFalse(def);

      // build the formula (var <-> def) and add it to the list of definitions
      BooleanFormula equiv = bfmgr.equivalence(var, def);
      predDef = bfmgr.and(predDef, equiv);

      predVars.add(var);
    }

    // the formula is (abstractionFormula & pathFormula & predDef)
    thmProver.push(predDef);
    AllSatCallbackImpl callback = new AllSatCallbackImpl();
    Region result = thmProver.allSat(callback, predVars);

    // pop() is actually costly sometimes, and we delete the environment anyway
    // thmProver.pop();

    // update statistics
    int numModels = callback.getCount();
    if (numModels < Integer.MAX_VALUE) {
      stats.maxAllSatCount = Math.max(numModels, stats.maxAllSatCount);
      stats.allSatCount += numModels;
    }

    // Not strictly necessary, but mark all predicates as handled
    predicates.clear();

    return result;
  }

  private class AllSatCallbackImpl implements AllSatCallback<Region> {

    private final RegionBuilder builder;

    private TimerWrapper regionTime = abstractionBddConstructionTimer;

    private int count = 0;

    private Region formula;

    private AllSatCallbackImpl() {
      builder = rmgr.builder(shutdownNotifier);
      abstractionSolveTimer.start();
    }

    @Override
    public void apply(List<BooleanFormula> model) {
      if (count == 0) {
        abstractionSolveTimer.stop();
        abstractionModelEnumTimer.start();
      }

      regionTime.start();

      // the abstraction is created simply by taking the disjunction
      // of all the models found by the all-sat-loop, and storing them in a BDD
      // first, let's create the BDD corresponding to the model
      builder.startNewConjunction();
      for (BooleanFormula f : model) {
        Optional<BooleanFormula> inner = fmgr.stripNegation(f);
        Region region = amgr.getPredicate(inner.orElse(f)).getAbstractVariable();
        if (inner.isPresent()) {
          // TODO: possible bug if the predicate itself contains the negation.
          builder.addNegativeRegion(region);
        } else {
          builder.addPositiveRegion(region);
        }
      }
      builder.finishConjunction();

      count++;

      regionTime.stop();
    }

    @Override
    public Region getResult() throws InterruptedException {
      if (abstractionSolveTimer.isRunning()) {
        abstractionSolveTimer.stop();
      } else {
        abstractionModelEnumTimer.stop();
      }

      if (formula == null) {
        abstractionModelEnumTimer.start();
        abstractionBddConstructionTimer.start();
        try {
          formula = builder.getResult();
          builder.close();
        } finally {
          abstractionModelEnumTimer.stop();
          abstractionBddConstructionTimer.stop();
        }
      }
      return formula;
    }

    private int getCount() {
      return count;
    }
  }

  /** Write input and result of an abstraction problem to disk. */
  private void dumpAbstractionProblem(
      final BooleanFormula f,
      final Collection<AbstractionPredicate> predicates,
      final AbstractionFormula result,
      int pCurrentAbstractionId) {
    @Nullable Path dumpFile =
        fmgr.formatFormulaOutputFile("abstraction", pCurrentAbstractionId, "input", 0);
    fmgr.dumpFormulaToFile(f, dumpFile);

    dumpFile = fmgr.formatFormulaOutputFile("abstraction", pCurrentAbstractionId, "predicates", 0);
    try (Writer w = IO.openOutputFile(dumpFile, Charset.defaultCharset())) {
      Joiner.on('\n').appendTo(w, predicates);
    } catch (IOException e) {
      logger.logUserException(Level.WARNING, e, "Failed to wrote predicates to file");
    }

    dumpFile = fmgr.formatFormulaOutputFile("abstraction", pCurrentAbstractionId, "result", 0);
    fmgr.dumpFormulaToFile(result.asInstantiatedFormula(), dumpFile);
  }

  /** Checks if a1 => a2 */
  public boolean checkCoverage(AbstractionFormula a1, AbstractionFormula a2)
      throws SolverException, InterruptedException {
    return amgr.entails(a1.asRegion(), a2.asRegion());
  }

  /** Checks if (a1 & p1) => a2 */
  public boolean checkCoverage(AbstractionFormula a1, PathFormula p1, AbstractionFormula a2)
      throws SolverException, InterruptedException {
    BooleanFormula absFormula = a1.asInstantiatedFormula();
    BooleanFormula symbFormula = getFormulaFromPathFormula(p1);
    BooleanFormula a = bfmgr.and(absFormula, symbFormula);

    // get formula of a2 with the indices of p1
    BooleanFormula b = fmgr.instantiate(a2.asFormula(), p1.getSsa());

    return solver.implies(a, b);
  }

  /**
   * Checks if an abstraction formula and a pathFormula are unsatisfiable.
   *
   * @param abstractionFormula the abstraction formula
   * @param pathFormula the path formula
   * @return unsat(pAbstractionFormula & pPathFormula)
   */
  public boolean unsat(AbstractionFormula abstractionFormula, PathFormula pathFormula)
      throws SolverException, InterruptedException {

    BooleanFormula absFormula = abstractionFormula.asInstantiatedFormula();
    BooleanFormula symbFormula = getFormulaFromPathFormula(pathFormula);
    BooleanFormula f = bfmgr.and(absFormula, symbFormula);

    logger.log(Level.ALL, "Checking satisfiability of formula", f);

    return solver.isUnsat(f);
  }

  // syntactic creation and manipulation of AbstractionFormulas

  /**
   * Create an abstraction from a single boolean formula without actually doing any abstraction
   * computation. The formula is just converted into a region, but the result is equivalent to the
   * input. This can be used to simply view the formula as a region. If BDDs are used, the result of
   * this method is a minimized form of the input.
   *
   * @param f The formula to be converted to a region. Must NOT be instantiated!
   * @param blockFormula A path formula that is not used for the abstraction, but will be used as
   *     the block formula in the resulting AbstractionFormula instance. Also it's SSAMap will be
   *     used for instantiating the result.
   * @return An AbstractionFormula instance representing f with blockFormula as the block formula.
   */
  public AbstractionFormula asAbstraction(final BooleanFormula f, final PathFormula blockFormula)
      throws InterruptedException {
    Region r = amgr.convertFormulaToRegion(f);
    return makeAbstractionFormula(r, blockFormula.getSsa(), blockFormula);
  }

  public AbstractionFormula makeTrueAbstractionFormula(PathFormula pPreviousBlockFormula) {
    if (pPreviousBlockFormula == null) {
      pPreviousBlockFormula = pfmgr.makeEmptyPathFormula();
    }

    return new AbstractionFormula(
        fmgr,
        amgr.getRegionCreator().makeTrue(),
        bfmgr.makeTrue(),
        bfmgr.makeTrue(),
        pPreviousBlockFormula,
        noAbstractionReuse);
  }

  /** Conjuncts two abstractions. Both need to have the same block formula. */
  public AbstractionFormula makeAnd(AbstractionFormula a1, AbstractionFormula a2) {
    checkArgument(a1.getBlockFormula().equals(a2.getBlockFormula()));

    Region region = amgr.getRegionCreator().makeAnd(a1.asRegion(), a2.asRegion());
    BooleanFormula formula = fmgr.makeAnd(a1.asFormula(), a2.asFormula());
    BooleanFormula instantiatedFormula =
        fmgr.makeAnd(a1.asInstantiatedFormula(), a2.asInstantiatedFormula());

    return new AbstractionFormula(
        fmgr, region, formula, instantiatedFormula, a1.getBlockFormula(), noAbstractionReuse);
  }

  /** Disjuncts two abstractions. */
  public AbstractionFormula makeOr(AbstractionFormula a1, AbstractionFormula a2)
      throws InterruptedException {
    Region region = amgr.getRegionCreator().makeOr(a1.asRegion(), a2.asRegion());
    BooleanFormula formula = fmgr.makeOr(a1.asFormula(), a2.asFormula());
    BooleanFormula instantiatedFormula =
        fmgr.makeOr(a1.asInstantiatedFormula(), a2.asInstantiatedFormula());
    PathFormula newBlockFormula = pfmgr.makeOr(a1.getBlockFormula(), a2.getBlockFormula());

    return new AbstractionFormula(
        fmgr, region, formula, instantiatedFormula, newBlockFormula, noAbstractionReuse);
  }

  AbstractionFormula makeAbstractionFormula(Region abs, SSAMap ssaMap, PathFormula blockFormula)
      throws InterruptedException {
    BooleanFormula symbolicAbs = amgr.convertRegionToFormula(abs);
    BooleanFormula instantiatedSymbolicAbs = fmgr.instantiate(symbolicAbs, ssaMap);

    if (options.isSimplifyAbstractionFormula()) {
      symbolicAbs = fmgr.simplify(symbolicAbs);
      instantiatedSymbolicAbs = fmgr.simplify(instantiatedSymbolicAbs);
    }

    return new AbstractionFormula(
        fmgr, abs, symbolicAbs, instantiatedSymbolicAbs, blockFormula, noAbstractionReuse);
  }

  // Creating AbstractionPredicates

  /**
   * Extract all atoms from a formula and create predicates for them. If instead a single predicate
   * should be created for the whole formula, call {@link #getPredicateFor(BooleanFormula)} instead.
   *
   * @param pFormula The formula with the atoms (with SSA indices).
   * @return A (possibly empty) collection of AbstractionPredicates without duplicates.
   */
  public ImmutableSet<AbstractionPredicate> getPredicatesForAtomsOf(BooleanFormula pFormula) {
    if (bfmgr.isFalse(pFormula)) {
      return ImmutableSet.of(amgr.makeFalsePredicate());
    }

    Set<BooleanFormula> atoms = fmgr.extractAtoms(pFormula, options.isSplitItpAtoms());

    ImmutableSet<AbstractionPredicate> preds =
        Collections3.transformedImmutableSetCopy(
            atoms, atom -> amgr.makePredicate(fmgr.uninstantiate(atom)));

    amgr.reorderPredicates();
    return preds;
  }

  /**
   * Create a single AbstractionPredicate representing a formula. If instead a predicate should be
   * used for each atom in this formula, call {@link #getPredicatesForAtomsOf(BooleanFormula)}.
   *
   * @param pFormula The formula to use (with SSA indices), may not simply be "true".
   * @return A single abstraction predicate.
   */
  public AbstractionPredicate getPredicateFor(BooleanFormula pFormula) {
    checkArgument(!bfmgr.isTrue(pFormula));

    return amgr.makePredicate(fmgr.uninstantiate(pFormula));
  }

  public AbstractionPredicate makeFalsePredicate() {
    return amgr.makeFalsePredicate();
  }

  /**
   * Return the set of predicates that occur in a region.
   *
   * <p>Note: this method currently fails with SymbolicRegionManager, and it probably cannot really
   * be fixed either, because when using symbolic regions we do not know what are the predicates (a
   * predicate does not need to be an SMT atom, it can be larger).
   *
   * <p>Thus better avoid using this method if possible.
   */
  public Set<AbstractionPredicate> extractPredicates(Region pRegion) {
    return amgr.extractPredicates(pRegion);
  }
}
