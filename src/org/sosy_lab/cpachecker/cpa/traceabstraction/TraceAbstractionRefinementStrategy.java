// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.traceabstraction;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Verify.verify;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Streams;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.sosy_lab.common.MoreStrings;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionManager;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionRefinementStrategy;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision.LocationInstance;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;

@Options(prefix = "cpa.traceabstraction.refinementStrategy")
public class TraceAbstractionRefinementStrategy extends PredicateAbstractionRefinementStrategy {

  @Option(
      secure = true,
      description =
          "The max amount of refinements for the trace abstraction algorithm. Setting it to 0 leads"
              + " to an analysis of the ARG without executing any refinements. This is used for"
              + " debugging purposes.")
  private int maxRefinementIterations = -1;

  private int curRefinementIteration = 0;

  private final ShutdownNotifier shutdownNotifier;
  private final InterpolationSequenceStorage itpSequenceStorage;

  private InterpolationSequence.Builder itpSequenceBuilder;

  public TraceAbstractionRefinementStrategy(
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      InterpolationSequenceStorage pItpSequenceStorage,
      PredicateAbstractionManager pPredAbsMgr,
      Solver pSolver)
      throws InvalidConfigurationException {
    super(pConfig, pLogger, pPredAbsMgr, pSolver);

    pConfig.inject(this);

    shutdownNotifier = checkNotNull(pShutdownNotifier);
    itpSequenceStorage = checkNotNull(pItpSequenceStorage);
  }

  @Override
  public boolean performRefinement(
      ARGReachedSet pReached,
      List<ARGState> pAbstractionStatesTrace,
      List<BooleanFormula> pInterpolants,
      boolean pRepeatedCounterexample)
      throws CPAException, InterruptedException {

    // TODO: This method is currently only used for debugging and will eventually be removed.

    verify(pAbstractionStatesTrace.size() == pInterpolants.size() + 1);
    logger.logf(
        Level.FINE,
        "Mapping of interpolants to arg-states:\n%s",
        lazyPrintItpToTransitionMapping(pAbstractionStatesTrace, pInterpolants));

    if (maxRefinementIterations != -1 && curRefinementIteration >= maxRefinementIterations) {
      logger.log(
          Level.WARNING,
          "Specified number of max refinements already exceeded. Skipping the refinement");
      return false;
    }

    return super.performRefinement(
        pReached, pAbstractionStatesTrace, pInterpolants, pRepeatedCounterexample);
  }

  @Override
  protected final void startRefinementOfPath() {
    itpSequenceBuilder = new InterpolationSequence.Builder();
  }

  @Override
  protected void storePredicates(
      LocationInstance pLocInstance, Collection<AbstractionPredicate> pPredicates) {
    itpSequenceBuilder.addPredicates(pLocInstance, pPredicates);
  }

  @Override
  protected void finishRefinementOfPath(
      ARGState pUnreachableState,
      List<ARGState> pAffectedStates,
      ARGReachedSet pReached,
      List<ARGState> pAbstractionStatesTrace,
      boolean pRepeatedCounterexample)
      throws CPAException, InterruptedException {

    ARGState refinementRoot =
        computeRefinementRoot(
            pUnreachableState, pAffectedStates, pReached, pRepeatedCounterexample);

    InterpolationSequence newSequence = itpSequenceBuilder.build();
    logger.logf(Level.INFO, "New Itp-sequence:\n%s", newSequence);
    itpSequenceStorage.addItpSequence(newSequence);

    updateARG(refinementRoot, pReached);

    curRefinementIteration++;
  }

  private ARGState computeRefinementRoot(
      ARGState pUnreachableState,
      List<ARGState> pAffectedStates,
      ARGReachedSet pReached,
      boolean pRepeatedCounterexample)
      throws RefinementFailedException {

    // Add predicate "false" to unreachable location
    // or add "false" to each location of the combination of locations
    for (CFANode loc : AbstractStates.extractLocations(pUnreachableState)) {
      int locInstance =
          PredicateAbstractState.getPredicateState(pUnreachableState)
              .getAbstractionLocationsOnPath()
              .get(loc);
      storePredicates(new LocationInstance(loc, locInstance), predAbsMgr.makeFalsePredicate());
    }
    pAffectedStates.add(pUnreachableState);

    ARGState refinementRoot =
        getRefinementRoot(
            pAffectedStates,
            pRepeatedCounterexample,
            pReached.asReachedSet(),
            ImmutableSetMultimap.of());

    logger.logf(
        Level.INFO,
        "Refinement root: %d (Parents: %s)",
        refinementRoot.getStateId(),
        refinementRoot.getParents().stream()
            .map(ARGState::getStateId)
            .collect(ImmutableList.toImmutableList()));
    return refinementRoot;
  }

  private void updateARG(ARGState pRefinementRoot, ARGReachedSet pReached)
      throws InterruptedException {
    shutdownNotifier.shutdownIfNecessary();

    argUpdate.start();
    pReached.removeSubtree(pRefinementRoot);
    argUpdate.stop();

    assert refinementCount > 0;
  }

  private Object lazyPrintItpToTransitionMapping(
      Collection<ARGState> pStates, List<BooleanFormula> pInterpolants) {
    return MoreStrings.lazyString(
        () ->
            Streams.zip(
                    pStates.stream(),
                    pInterpolants.stream(),
                    (state, itp) ->
                        String.format(
                            "%d:%s : %s",
                            state.getStateId(), AbstractStates.extractLocation(state), itp))
                .collect(Collectors.joining("\n")));
  }
}
