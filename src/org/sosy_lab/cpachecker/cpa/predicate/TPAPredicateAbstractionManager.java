// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Predicates.equalTo;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.collect.Collections3;
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
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormulaTPA;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.regions.Region;
import org.sosy_lab.cpachecker.util.predicates.regions.RegionCreator;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.predicates.weakening.InductiveWeakeningManager;
import org.sosy_lab.cpachecker.util.predicates.weakening.WeakeningOptions;
import org.sosy_lab.cpachecker.util.statistics.ThreadSafeTimerContainer.TimerWrapper;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverException;

public class TPAPredicateAbstractionManager extends PredicateAbstractionManager{

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
//  private final @Nullable InductiveWeakeningManager weakeningManager;
  private final ShutdownNotifier shutdownNotifier;
  private static final Set<Integer> noAbstractionReuse = ImmutableSet.of();

  private boolean abstractionReuseDisabledBecauseOfAmbiguity = false;
  private final Map<Pair<BooleanFormula, ImmutableSet<BooleanFormula>>, AbstractionFormulaTPA>
      abstractionCache;
  // Cache for satisfiability queries: if formula is contained, it is unsat
  private final Set<BooleanFormula> unsatisfiabilityCache;

  //Statistics
  private final TimerWrapper abstractionReuseImplicationTimer;
  private final TimerWrapper abstractionReuseTimer;
  private final TimerWrapper trivialPredicatesTimer;
  private final TimerWrapper quantifierEliminationTimer;
  private final TimerWrapper abstractionSolveTimer;
  private final TimerWrapper abstractionModelEnumTimer;

  public TPAPredicateAbstractionManager(
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
    super(pAmgr, pPfmgr, pSolver, pOptions, weakeningOptions, pAbstractionStorage, pLogger,
        pShutdownNotifier, pAbstractionStats, pInvariantsSupplier);
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
    abstractionStorage = pAbstractionStorage;

    if (options.isUseCache()) {
      abstractionCache = new HashMap<>();
      unsatisfiabilityCache = new HashSet<>();
    } else {
      abstractionCache = null;
      unsatisfiabilityCache = null;
    }

    trivialPredicatesTimer = stats.trivialPredicatesTime.getNewTimer();
    abstractionReuseImplicationTimer = stats.abstractionReuseImplicationTime.getNewTimer();
    abstractionReuseTimer = stats.abstractionReuseTime.getNewTimer();
    quantifierEliminationTimer = stats.quantifierEliminationTime.getNewTimer();
    abstractionSolveTimer = stats.abstractionSolveTime.getNewTimer();
    abstractionModelEnumTimer = stats.abstractionModelEnumTime.getNewTimer();
  }

  /**
   * TODO: Change this docu for TPA
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

  public AbstractionFormulaTPA buildAbstractionForTPA(
      final Collection<CFANode> locations,
      Optional<CallstackStateEqualsWrapper> callstackInformation,
      final AbstractionFormulaTPA abstractionFormula,
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
      AbstractionFormulaTPA reused =
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
      AbstractionFormulaTPA result = abstractionCache.get(absKey);

      if (result != null) {
        // create new abstraction object to have a unique abstraction id

        // instantiate the formula with the current indices
        BooleanFormula stateFormula = result.asFormula();
        BooleanFormula instantiatedFormula = fmgr.instantiate(stateFormula, ssa);

        result =
            new AbstractionFormulaTPA(
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
        return new AbstractionFormulaTPA(
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
//    if (options.isIdentifyTrivialPredicates()) {
//      trivialPredicatesTimer.start();
//      abs = handleTrivialPredicates(remainingPredicates, abstractionFormula, pathFormula);
//      trivialPredicatesTimer.stop();
//    }
//
//    // add invariants to abstraction formula if available
//    if (invariantSupplier != TrivialInvariantSupplier.INSTANCE) {
//      // TODO we do not yet support multiple CFA nodes per abstraction here
//      // and choosing *one* location is best way for backwards compatibility.
//      for (CFANode location : locations) {
//        BooleanFormula invariant =
//            invariantSupplier.getInvariantFor(
//                location, callstackInformation, fmgr, pfmgr, pathFormula);
//
//        if (!bfmgr.isTrue(invariant)) {
//          AbstractionPredicate absPred = amgr.makePredicate(invariant);
//          abs = rmgr.makeAnd(abs, absPred.getAbstractVariable());
//
//          // Calculate the set of predicates we still need to use for abstraction.
//          Iterables.removeIf(remainingPredicates, equalTo(absPred));
//        }
//      }
//    }

//    if (options.getAbstractionType() == AbstractionType.ELIMINATION) {
//      quantifierEliminationTimer.start();
//      try {
//        BooleanFormula eliminationResult = fmgr.uninstantiate(fmgr.eliminateDeadVariables(f, ssa));
//        abs = rmgr.makeAnd(abs, amgr.convertFormulaToRegion(eliminationResult));
//      } finally {
//        quantifierEliminationTimer.stop();
//      }
//    } else if (options.getAbstractionType() == AbstractionType.CARTESIAN_BY_WEAKENING) {
//      abs = rmgr.makeAnd(abs, buildCartesianAbstractionUsingWeakening(f, ssa, remainingPredicates));
//
//    } else {
//      abs = rmgr.makeAnd(abs, computeAbstraction(f, remainingPredicates, instantiator));
//    }

    AbstractionFormulaTPA result = makeAbstractionFormula(abs, ssa, pathFormula);

//    if (options.isUseCache()) {
//      abstractionCache.put(absKey, result);
//
//      if (result.isFalse()) {
//        unsatisfiabilityCache.add(f);
//      }
//    }

    long abstractionTime =
        TimeSpan.sum(
                abstractionSolveTimer.getLengthOfLastInterval(),
                abstractionModelEnumTimer.getLengthOfLastInterval())
            .asMillis();
    logger.log(Level.FINEST, "Computing abstraction took", abstractionTime, "ms");
    logger.log(Level.ALL, "Abstraction result is", result.asFormula());

//    if (options.isDumpHardAbstractions() && abstractionTime > 10000) {
//      // we want to dump "hard" problems...
//      dumpAbstractionProblem(f, pPredicates, result, currentAbstractionId);
//    }

    return result;
  }

  /*
  Help method for buildAbstractionTPA
   */

  private @Nullable AbstractionFormulaTPA reuseAbstractionIfPossible(
      final AbstractionFormulaTPA abstractionFormula,
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
          return new AbstractionFormulaTPA(
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
      final AbstractionFormulaTPA pOldAbs,
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







  /*
  syntactic creation and manipulation of AbstractionFormulaTPAs, override methods from AbstractionFormula parent
   */
  @Override
  AbstractionFormulaTPA makeAbstractionFormula(Region abs, SSAMap ssaMap, PathFormula blockFormula)
      throws InterruptedException {
    BooleanFormula symbolicAbs = amgr.convertRegionToFormula(abs);
    BooleanFormula instantiatedSymbolicAbs = fmgr.instantiate(symbolicAbs, ssaMap);

    if (options.isSimplifyAbstractionFormula()) {
      symbolicAbs = fmgr.simplify(symbolicAbs);
      instantiatedSymbolicAbs = fmgr.simplify(instantiatedSymbolicAbs);
    }

    return new AbstractionFormulaTPA(
        fmgr, abs, symbolicAbs, instantiatedSymbolicAbs, blockFormula, noAbstractionReuse);
  }

  /**
   * Create an abstraction from a single boolean formula without actually doing any abstraction
   * computation. The formula is just converted into a region, but the result is equivalent to the
   * input. This can be used to simply view the formula as a region. If BDDs are used, the result of
   * this method is a minimized form of the input.
   *
   * @param f The formula to be converted to a region. Must NOT be instantiated!
   * @param blockFormula A path formula that is not used for the abstraction, but will be used as
   *     the block formula in the resulting AbstractionFormulaTPA instance. Also it's SSAMap will be
   *     used for instantiating the result.
   * @return An AbstractionFormulaTPA instance representing f with blockFormula as the block formula.
   */
  @Override
  public AbstractionFormulaTPA asAbstraction(final BooleanFormula f, final PathFormula blockFormula)
      throws InterruptedException {
    Region r = amgr.convertFormulaToRegion(f);
    return makeAbstractionFormula(r, blockFormula.getSsa(), blockFormula);
  }

  @Override
  public AbstractionFormulaTPA makeTrueAbstractionFormula(PathFormula pPreviousBlockFormula) {
    if (pPreviousBlockFormula == null) {
      pPreviousBlockFormula = pfmgr.makeEmptyPathFormula();
    }
    return new AbstractionFormulaTPA(
        this.fmgr,
        this.amgr.getRegionCreator().makeTrue(),
        this.bfmgr.makeTrue(),
        this.bfmgr.makeTrue(),
        pPreviousBlockFormula,
        noAbstractionReuse);
  }

  /** Conjuncts two abstractions. Both need to have the same block formula. */
  @Override
  public AbstractionFormulaTPA makeAnd(AbstractionFormula a1, AbstractionFormula a2) {
    checkArgument(a1.getBlockFormula().equals(a2.getBlockFormula()));

    Region region = amgr.getRegionCreator().makeAnd(a1.asRegion(), a2.asRegion());
    BooleanFormula formula = fmgr.makeAnd(a1.asFormula(), a2.asFormula());
    BooleanFormula instantiatedFormula =
        fmgr.makeAnd(a1.asInstantiatedFormula(), a2.asInstantiatedFormula());

    return new AbstractionFormulaTPA(
        fmgr, region, formula, instantiatedFormula, a1.getBlockFormula(), noAbstractionReuse);
  }

  /** Disjuncts two abstractions. */
  @Override
  public AbstractionFormulaTPA makeOr(AbstractionFormula a1, AbstractionFormula a2)
      throws InterruptedException {
    Region region = amgr.getRegionCreator().makeOr(a1.asRegion(), a2.asRegion());
    BooleanFormula formula = fmgr.makeOr(a1.asFormula(), a2.asFormula());
    BooleanFormula instantiatedFormula =
        fmgr.makeOr(a1.asInstantiatedFormula(), a2.asInstantiatedFormula());
    PathFormula newBlockFormula = pfmgr.makeOr(a1.getBlockFormula(), a2.getBlockFormula());

    return new AbstractionFormulaTPA(
        fmgr, region, formula, instantiatedFormula, newBlockFormula, noAbstractionReuse);
  }
}
