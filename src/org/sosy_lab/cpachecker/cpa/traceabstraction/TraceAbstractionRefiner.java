// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.traceabstraction;

import static com.google.common.base.Verify.verify;
import static com.google.common.base.Verify.verifyNotNull;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import com.google.common.collect.UnmodifiableIterator;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.sosy_lab.common.MoreStrings;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.cpa.arg.ARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.AbstractARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.predicate.BlockFormulaStrategy.BlockFormulas;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionManager;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.predicate.SlicingAbstractionsUtils;
import org.sosy_lab.cpachecker.cpa.predicate.SlicingAbstractionsUtils.AbstractionPosition;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.AssignmentToPathAllocator;
import org.sosy_lab.cpachecker.util.predicates.PathChecker;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassification;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class TraceAbstractionRefiner implements ARGBasedRefiner {

  private final StatTimer totalTime = new StatTimer("Total time for refinement");
  private final StatTimer satCheckTime = new StatTimer("Time for path feasibility check");
  private final StatTimer argUpdateTime = new StatTimer("Time for ARG update");

  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final CFA cfa;
  private final InterpolationManager itpManager;
  private final PathChecker pathChecker;
  private final Solver solver;
  private final BooleanFormulaManagerView bFMgrView;
  private final PathFormulaManager pfmgr;
  private final PredicateAbstractionManager predAbsManager;
  private final InterpolationSequenceStorage itpSequenceStorage;

  @SuppressWarnings("resource")
  private TraceAbstractionRefiner(
      TraceAbstractionCPA pTaCpa,
      PredicateCPA pPredicateCpa,
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {

    // TODO: refactor this into a new 'RefinementStrategy' class.
    //
    // Currently, most of the logic is similar to 'PredicateAbstractionRefinementStrategy'.
    // The main differences for now are that a) this refiner eagerly computes PathFormulas for a
    // given ARGPath and b) new predicates are stored in a (TraceAbstraction-)state
    // instead of the PredicatePrecision.

    itpSequenceStorage = pTaCpa.getInterpolationSequenceStorage();

    shutdownNotifier = pShutdownNotifier;
    logger = pLogger;

    solver = pPredicateCpa.getSolver();
    bFMgrView = solver.getFormulaManager().getBooleanFormulaManager();
    pfmgr = pPredicateCpa.getPathFormulaManager();
    predAbsManager = pPredicateCpa.getPredicateManager();

    cfa = pPredicateCpa.getCfa();
    Optional<VariableClassification> variableClassification = cfa.getVarClassification();
    Optional<LoopStructure> loopStructure = cfa.getLoopStructure();

    itpManager =
        new InterpolationManager(
            pfmgr,
            solver,
            loopStructure,
            variableClassification,
            pConfig,
            shutdownNotifier,
            logger);

    AssignmentToPathAllocator pathAllocator =
        new AssignmentToPathAllocator(pConfig, shutdownNotifier, logger, cfa.getMachineModel());
    pathChecker = new PathChecker(pConfig, logger, pfmgr, solver, pathAllocator);
  }

  public static Refiner create(
      ConfigurableProgramAnalysis pCpa, LogManager pLogger, ShutdownNotifier pNotifier)
      throws InvalidConfigurationException {
    ARGBasedRefiner refiner = createRefiner(pCpa, pLogger, pNotifier);
    return AbstractARGBasedRefiner.forARGBasedRefiner(refiner, pCpa);
  }

  @SuppressWarnings("resource")
  private static ARGBasedRefiner createRefiner(
      ConfigurableProgramAnalysis pCpa, LogManager pLogger, ShutdownNotifier pNotifier)
      throws InvalidConfigurationException {
    TraceAbstractionCPA taCpa =
        CPAs.retrieveCPAOrFail(pCpa, TraceAbstractionCPA.class, TraceAbstractionRefiner.class);
    PredicateCPA predicateCpa =
        CPAs.retrieveCPAOrFail(pCpa, PredicateCPA.class, TraceAbstractionRefiner.class);
    Configuration configuration = predicateCpa.getConfiguration();

    return new TraceAbstractionRefiner(taCpa, predicateCpa, configuration, pLogger, pNotifier);
  }

  @Override
  public CounterexampleInfo performRefinementForPath(ARGReachedSet pReached, ARGPath pPath)
      throws CPAException, InterruptedException {

    totalTime.start();
    try {
      return performRefinementForPath0(pReached, pPath);
    } finally {
      totalTime.stop();
    }
  }

  private CounterexampleInfo performRefinementForPath0(ARGReachedSet pReached, ARGPath pPath)
      throws CPAException, InterruptedException {
    logger.log(Level.FINEST, "Starting trace abstraction refinement.");
    logger.logf(Level.INFO, "Path to target state: %s\n", lazyPrintNodes(pPath));

    List<PathFormula> pathFormulaList =
        SlicingAbstractionsUtils.getFormulasForPath(
            pfmgr, solver, pPath.getFirstState(), pPath.asStatesList(), AbstractionPosition.NONE);

    CounterexampleTraceInfo counterexample;
    satCheckTime.start();
    try {
      counterexample =
          itpManager.buildCounterexampleTrace(
              BlockFormulas.createFromPathFormulas(pathFormulaList));
    } finally {
      satCheckTime.stop();
    }

    if (!counterexample.isSpurious()) {
      // The counterexample contains a real error
      logger.log(Level.FINEST, "Error trace is feasible");
      return pathChecker.handleFeasibleCounterexample(pPath, counterexample, false);
    }

    // The error is spurious -> perform a refinement

    List<BooleanFormula> interpolants = counterexample.getInterpolants();
    verify(pPath.asStatesList().size() == interpolants.size() + 1);
    logger.logf(
        Level.FINE,
        "Mapping of interpolants to arg-states:\n%s",
        lazyPrintItpToTransitionMapping(pPath, interpolants));

    UnmodifiableIterator<ARGState> stateIterator = pPath.asStatesList().iterator();
    Iterator<BooleanFormula> itpIterator = interpolants.iterator();

    ARGState previousState = null;
    InterpolationSequence.Builder itpSequenceBuilder = new InterpolationSequence.Builder();

    while (stateIterator.hasNext() && itpIterator.hasNext()) {
      ARGState curState = stateIterator.next();
      BooleanFormula curInterpolant = itpIterator.next();

      if (bFMgrView.isFalse(curInterpolant)) {
        break;
      }

      if (!bFMgrView.isTrue(curInterpolant)) {
        verifyNotNull(previousState);
        ImmutableSet<AbstractionPredicate> preds =
            // predAbsManager.getPredicatesForAtomsOf(curInterpolant);
            ImmutableSet.of(predAbsManager.getPredicateFor(curInterpolant));
        if (preds.size() > 1) {
          Comparator<AbstractionPredicate> comparator =
              (pred1, pred2) ->
                  pred1
                      .getSymbolicVariable()
                      .toString()
                      .compareTo(pred2.getSymbolicVariable().toString());
          preds = preds.stream().sorted(comparator).collect(ImmutableSet.toImmutableSet());
        }
        String functionName = AbstractStates.extractLocation(previousState).getFunctionName();

        itpSequenceBuilder.addFunctionPredicates(functionName, preds);
      }

      previousState = curState;
    }

    itpSequenceStorage.addItpSequence(itpSequenceBuilder.build());

    // Search the first ARG state in which the corresponding interpolant is no longer equal to
    // 'true'. This marks the root state that is taken for the refinement.
    OptionalInt firstItpIndex =
        IntStream.range(0, interpolants.size())
            .filter(i -> !bFMgrView.isTrue(interpolants.get(i)))
            .findFirst();
    // This currently assumes that the first TAState is always a top state
    ARGState originalState = pPath.asStatesList().get(firstItpIndex.orElseThrow() - 1);

    shutdownNotifier.shutdownIfNecessary();
    argUpdateTime.start();
    for (ARGState refinementRoot : ImmutableList.copyOf(originalState.getChildren())) {
      if (!refinementRoot.isDestroyed()) {
        // This ARGState might have already been destroyed in a former iteration of this loop
        pReached.removeSubtree(refinementRoot);
      }
    }
    argUpdateTime.stop();

    return CounterexampleInfo.spurious();
  }

  private Object lazyPrintNodes(ARGPath pStemPath) {
    return lazyPrintNodes(pStemPath.asStatesList());
  }

  private Object lazyPrintNodes(Collection<ARGState> pArgStates) {
    return MoreStrings.lazyString(
        () ->
            FluentIterable.from(pArgStates)
                .transform(x -> (x.getStateId() + ":" + AbstractStates.extractLocation(x)))
                .toString());
  }

  private Object lazyPrintItpToTransitionMapping(
      ARGPath pPath, List<BooleanFormula> pInterpolants) {
    return MoreStrings.lazyString(
        () ->
            Streams.zip(
                    pPath.getStatePairs().stream(),
                    pInterpolants.stream(),
                    (statePair, itp) ->
                        String.format(
                            "%d:%s -> %d:%s : %s",
                            statePair.getFirstNotNull().getStateId(),
                            AbstractStates.extractLocation(statePair.getFirstNotNull()),
                            statePair.getSecondNotNull().getStateId(),
                            AbstractStates.extractLocation(statePair.getSecondNotNull()),
                            itp))
                .collect(Collectors.joining("\n")));
  }
}
