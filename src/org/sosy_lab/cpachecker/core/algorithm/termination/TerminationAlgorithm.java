/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.termination;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Comparator.comparingInt;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.WARNING;
import static org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition.getDefaultPartition;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractStateByType;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.IntegerOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.Specification;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.LassoAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.LassoAnalysisResult;
import org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.RankingRelation;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.defaults.NamedProperty;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithLocation;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets.AggregatedReachedSetManager;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.ARGPathBuilder;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathIterator;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.termination.TerminationCPA;
import org.sosy_lab.cpachecker.cpa.termination.TerminationState;
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.DefaultCFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.SpecificationProperty;

/**
 * Algorithm that uses a safety-analysis to prove (non-)termination.
 */
@Options(prefix = "termination")
public class TerminationAlgorithm implements Algorithm, AutoCloseable, StatisticsProvider {

  private final static Set<Property> TERMINATION_PROPERTY = NamedProperty.singleton("termination");

  private final static Path SPEC_FILE = Paths.get("config/specification/termination_as_reach.spc");

  @Nullable private static Specification terminationSpecification;

  private enum ResetReachedSetStrategy {
    REMOVE_TARGET_STATE,
    REMOVE_LOOP,
    RESET
  }

  @Option(
    secure = true,
    description =
        "Strategy used to prepare reched set and ARG for next iteration "
            + "after successful refinement of the termination argument."
  )
  private ResetReachedSetStrategy resetReachedSetStrategy = ResetReachedSetStrategy.REMOVE_LOOP;

  @Option(
    secure = true,
    description = "maximal number of repeated ranking functions per loop before stopping analysis"
  )
  @IntegerOption(min = 1)
  private int maxRepeatedRankingFunctionsPerLoop = 10;

  private final TerminationStatistics statistics;

  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final CFA cfa;
  private final ReachedSetFactory reachedSetFactory;
  private final Algorithm safetyAlgorithm;
  private final ConfigurableProgramAnalysis safetyCPA;

  private final LassoAnalysis lassoAnalysis;
  private final TerminationLoopInformation terminationInformation;
  private final Set<CVariableDeclaration> globalDeclaration;
  private final SetMultimap<String, CVariableDeclaration> localDeclarations;

  private final AggregatedReachedSetManager aggregatedReachedSetManager;

  public TerminationAlgorithm(
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      CFA pCfa,
      ReachedSetFactory pReachedSetFactory,
      AggregatedReachedSetManager pAggregatedReachedSetManager,
      Specification pSpecification,
      Algorithm pSafetyAlgorithm,
      ConfigurableProgramAnalysis pSafetyCPA)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    logger = checkNotNull(pLogger);
    shutdownNotifier = pShutdownNotifier;
    cfa = checkNotNull(pCfa);
    reachedSetFactory = checkNotNull(pReachedSetFactory);
    aggregatedReachedSetManager = checkNotNull(pAggregatedReachedSetManager);
    safetyAlgorithm = checkNotNull(pSafetyAlgorithm);
    safetyCPA = checkNotNull(pSafetyCPA);

    Specification requiredSpecification =
        loadTerminationSpecification(pSpecification.getProperties(), pCfa, pConfig, pLogger);
    Preconditions.checkArgument(
        requiredSpecification.equals(pSpecification),
        "%s requires %s, but %s is given.",
        TerminationAlgorithm.class.getSimpleName(),
        requiredSpecification,
        pSpecification);

    TerminationCPA terminationCpa = CPAs.retrieveCPA(pSafetyCPA, TerminationCPA.class);
    if (terminationCpa == null) {
      throw new InvalidConfigurationException("TerminationAlgorithm requires TerminationCPA");
    }
    terminationInformation = terminationCpa.getTerminationInformation();

    ARGCPA agrCpa = CPAs.retrieveCPA(pSafetyCPA, ARGCPA.class);
    if (agrCpa == null) {
      throw new InvalidConfigurationException("TerminationAlgorithm requires ARGCPA");
    }

    DeclarationCollectionCFAVisitor visitor = new DeclarationCollectionCFAVisitor();
    for (CFANode function : cfa.getAllFunctionHeads()) {
      CFATraversal.dfs().ignoreFunctionCalls().traverseOnce(function, visitor);
    }
    localDeclarations = ImmutableSetMultimap.copyOf(visitor.localDeclarations);
    globalDeclaration = ImmutableSet.copyOf(visitor.globalDeclarations);

    LoopStructure loopStructure =
        cfa.getLoopStructure()
            .orElseThrow(
                () ->
                    new InvalidConfigurationException(
                        "Loop structure is not present, but required for termination analysis."));
    statistics = new TerminationStatistics(pConfig, logger, loopStructure.getAllLoops().size());
    lassoAnalysis = LassoAnalysis.create(pLogger, pConfig, pShutdownNotifier, pCfa, statistics);
  }

  /** Loads the specification required to run the {@link TerminationAlgorithm}. */
  public static Specification loadTerminationSpecification(
      Set<SpecificationProperty> pProperties, CFA pCfa, Configuration pConfig, LogManager pLogger)
      throws InvalidConfigurationException {
    if (terminationSpecification == null) {
      terminationSpecification =
          Specification.fromFiles(
              pProperties, Collections.singleton(SPEC_FILE), pCfa, pConfig, pLogger);
    }

    return terminationSpecification;
  }

  @Override
  public void close() {
    lassoAnalysis.close();
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(statistics);
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet)
      throws CPAException, InterruptedException, CPAEnabledAnalysisPropertyViolationException {

    statistics.algorithmStarted();
    try {
      return run0(pReachedSet);

    } finally {
      statistics.algorithmFinished();
    }
  }

  private AlgorithmStatus run0(ReachedSet pReachedSet)
      throws InterruptedException, CPAEnabledAnalysisPropertyViolationException, CPAException {
    logger.log(Level.INFO, "Starting termination algorithm.");

    if (cfa.getLanguage() != Language.C) {
      logger.log(WARNING, "Termination analysis supports only C.");
      return AlgorithmStatus.UNSOUND_AND_PRECISE.withPrecise(false);
    }

    CFANode initialLocation = AbstractStates.extractLocation(pReachedSet.getFirstState());
    AlgorithmStatus status = AlgorithmStatus.SOUND_AND_PRECISE.withPrecise(false);

    List<Loop> allLoops = Lists.newArrayList(cfa.getLoopStructure().get().getAllLoops());
    Collections.sort(allLoops, comparingInt(l -> l.getInnerLoopEdges().size()));
    for (Loop loop : allLoops) {
      shutdownNotifier.shutdownIfNecessary();
      statistics.analysisOfLoopStarted(loop);

      resetReachedSet(pReachedSet, initialLocation);
      CPAcheckerResult.Result loopTermiantion =
          prooveLoopTermination(pReachedSet, loop, initialLocation);

      if (loopTermiantion == Result.FALSE) {
        logger.logf(Level.FINE, "Proved non-termination of %s.", loop);
        return AlgorithmStatus.UNSOUND_AND_PRECISE;

      } else if (loopTermiantion != Result.TRUE) {
        logger.logf(FINE, "Could not prove (non-)termination of %s.", loop);
        status = status.withSound(false);
      }

      statistics.analysisOfLoopFinished(loop);
    }

    if (status.isSound()) {
      status = status.update(checkRecursion(initialLocation));
    }

    // We did not find a non-terminating loop.
    logger.log(Level.INFO, "Termination algorithm did not find a non-terminating loop.");
    while (status.isSound() && pReachedSet.hasWaitingState()) {
      pReachedSet.popFromWaitlist();
    }
    return status;
  }

  private Result prooveLoopTermination(ReachedSet pReachedSet, Loop pLoop, CFANode initialLocation)
      throws CPAEnabledAnalysisPropertyViolationException, CPAException, InterruptedException {

    logger.logf(Level.FINE, "Prooving (non)-termination of %s", pLoop);
    Set<RankingRelation> rankingRelations = Sets.newHashSet();
    int totalRepeatedRankingFunctions = 0;
    int repeatedRankingFunctionsSinceSuccessfulIteration = 0;

    // Pass current loop and relevant variables to TerminationCPA.
    Set<CVariableDeclaration> relevantVariables = getRelevantVariables(pLoop);
    terminationInformation.setProcessedLoop(pLoop, relevantVariables);

    Result result = Result.TRUE;
    while (pReachedSet.hasWaitingState() && result != Result.FALSE) {
      shutdownNotifier.shutdownIfNecessary();
      statistics.safetyAnalysisStarted(pLoop);
      AlgorithmStatus status = safetyAlgorithm.run(pReachedSet);
      terminationInformation.resetCfa();
      statistics.safetyAnalysisFinished(pLoop);
      shutdownNotifier.shutdownIfNecessary();

      boolean targetReached =
          pReachedSet.asCollection().stream().anyMatch(AbstractStates::isTargetState);
      Optional<ARGState> targetStateWithCounterExample =
          pReachedSet
              .asCollection()
              .stream()
              .filter(AbstractStates::isTargetState)
              .map(s -> AbstractStates.extractStateByType(s, ARGState.class))
              .filter(s -> s.getCounterexampleInformation().isPresent())
              .findAny();

      // potential non-termination
      if (status.isPrecise() && targetStateWithCounterExample.isPresent()) {

        ARGState targetState = targetStateWithCounterExample.get();
        CounterexampleInfo originalCounterexample =
            targetState.getCounterexampleInformation().get();
        ARGState loopHeadState = Iterables.getOnlyElement(targetState.getParents());
        ARGState nonTerminationLoopHead = createNonTerminationState(loopHeadState);
        CounterexampleInfo counterexample =
            removeDummyLocationsFromCounterExample(originalCounterexample, nonTerminationLoopHead);
        LassoAnalysisResult lassoAnalysisResult =
            lassoAnalysis.checkTermination(pLoop, counterexample, relevantVariables);

        if (lassoAnalysisResult.hasNonTerminationArgument()) {
          removeIntermediateStates(pReachedSet, targetState);
          result = Result.FALSE;

        } else if (lassoAnalysisResult.hasTerminationArgument()) {
          RankingRelation rankingRelation = lassoAnalysisResult.getTerminationArgument();

          // Do not add a ranking relation twice
          if (rankingRelations.add(rankingRelation)) {
            terminationInformation.addRankingRelation(rankingRelation);
            // Prepare reached set for next iteration.
            prepareForNextIteration(pReachedSet, targetState, initialLocation);
            addInvariantsToAggregatedReachedSet(loopHeadState, rankingRelation);
            // a ranking relation was synthesized and the reached set was reseted
            result = Result.TRUE;
            repeatedRankingFunctionsSinceSuccessfulIteration = 0;

          } else {
            totalRepeatedRankingFunctions++;
            repeatedRankingFunctionsSinceSuccessfulIteration++;
            logger.logf(WARNING, "Repeated ranking relation %s for %s", rankingRelation, pLoop);

            // Do not use the first reached target state again and again
            // if we cannot synthesis new termination arguments from it.
            if (repeatedRankingFunctionsSinceSuccessfulIteration
                > maxRepeatedRankingFunctionsPerLoop / 5) {
              removeTargetState(pReachedSet, targetState);
              result = Result.UNKNOWN;

            } else if (totalRepeatedRankingFunctions >= maxRepeatedRankingFunctionsPerLoop) {
              // stop analysis for this loop because there is no progress
              removeTargetState(pReachedSet, targetState);
              return Result.UNKNOWN;

            } else {
              // Prepare reached set for next iteration.
              prepareForNextIteration(pReachedSet, targetState, initialLocation);
              // a ranking relation was synthesized and the reached set was reseted
              result = Result.TRUE;
            }
          }

        } else { // no termination argument and no non-termination argument could be synthesized
          logger.logf(WARNING, "Could not synthesize a termination or non-termination argument.");
          removeTargetState(pReachedSet, targetState);
          result = Result.UNKNOWN;
        }

      } else if (!status.isSound() || targetReached || pReachedSet.hasWaitingState()) {
        result = Result.UNKNOWN; // unsound, but still precise
      }
    }

    return result;
  }

  private void addInvariantsToAggregatedReachedSet(
      ARGState loopHeadState, RankingRelation rankingRelation) {
    ReachedSet dummy = reachedSetFactory.create();
    AbstractStateWithLocation locationState =
        extractStateByType(loopHeadState, AbstractStateWithLocation.class);
    rankingRelation
        .getSupportingInvariants()
        .stream()
        .map(s -> ImmutableList.of(locationState, s))
        .map(CompositeState::new)
        .forEach(s -> dummy.add(s, SingletonPrecision.getInstance()));

    aggregatedReachedSetManager.addReachedSet(dummy);
  }

  private Set<CVariableDeclaration> getRelevantVariables(Loop pLoop) {
    String function = pLoop.getLoopHeads().iterator().next().getFunctionName();
    Set<CVariableDeclaration> relevantVariabels =
        ImmutableSet.<CVariableDeclaration>builder()
            .addAll(globalDeclaration)
            .addAll(localDeclarations.get(function))
            .build();
    return relevantVariabels;
  }

  private void removeIntermediateStates(ReachedSet pReachedSet, AbstractState pTargetState) {
    Preconditions.checkArgument(AbstractStates.isTargetState(pTargetState));
    Preconditions.checkArgument(!cfa.getAllNodes().contains(extractLocation(pTargetState)));
    ARGState targetState = AbstractStates.extractStateByType(pTargetState, ARGState.class);
    Preconditions.checkArgument(targetState.getCounterexampleInformation().isPresent());
    CounterexampleInfo counterexample = targetState.getCounterexampleInformation().get();

    // Remove dummy target state from ARG and replace loop head with new target state
    ARGState loopHead = Iterables.getOnlyElement(targetState.getParents());
    ARGState newTargetState = createNonTerminationState(loopHead);
    targetState.removeFromARG();
    loopHead.replaceInARGWith(newTargetState);

    // Remove dummy target state from reached set and replace loop head with new target state
    pReachedSet.add(newTargetState, pReachedSet.getPrecision(loopHead));
    pReachedSet.removeOnlyFromWaitlist(newTargetState);
    pReachedSet.remove(pTargetState);
    pReachedSet.remove(loopHead);

    CounterexampleInfo newCounterexample =
        removeDummyLocationsFromCounterExample(counterexample, newTargetState);
    newTargetState.addCounterexampleInformation(newCounterexample);
  }

  private ARGState createNonTerminationState(AbstractState loopHead) {
    TerminationState terminationState = extractStateByType(loopHead, TerminationState.class);
    AbstractState newTerminationState =
        terminationState.withViolatedProperties(TERMINATION_PROPERTY);
    ARGState newTargetState = new ARGState(newTerminationState, null);
    return newTargetState;
  }

  /**
   * Removes all intermediate states from the counterexample.
   * @param counterexample the {@link CounterexampleInfo} to remove the states from
   * @param newTargetState the new target state which is the last state of the counterexample
   * @return the created {@link CounterexampleInfo}
   */
  private CounterexampleInfo removeDummyLocationsFromCounterExample(
      CounterexampleInfo counterexample, ARGState newTargetState) {

    // The value assignments are not valid for a counterexample that witnesses non-termination.
    ARGPath targetPath = counterexample.getTargetPath();
    PathIterator targetPathIt = targetPath.fullPathIterator();
    ARGPathBuilder builder = ARGPath.builder();
    Optional<ARGState> lastStateInCfa = Optional.empty();

    do {
      // the last state has not outgoing edge
      if (targetPathIt.hasNext()) {
        CFAEdge outgoingEdge = targetPathIt.getOutgoingEdge();
        CFANode location = outgoingEdge.getPredecessor();
        CFANode nextLocation = outgoingEdge.getSuccessor();

        if (cfa.getAllNodes().contains(location) && cfa.getAllNodes().contains(nextLocation)) {

          if (targetPathIt.isPositionWithState() || lastStateInCfa.isPresent()) {
            ARGState state = lastStateInCfa.orElseGet(targetPathIt::getAbstractState);
            ARGState nextAbstractState = targetPathIt.getNextAbstractState();

            @Nullable CFAEdge edgeToNextState = null;
            // use only edges matching the next location
            if (AbstractStates.extractLocation(nextAbstractState).equals(nextLocation)) {
              edgeToNextState = state.getEdgeToChild(nextAbstractState);
            }
            builder.add(state, edgeToNextState);
          }

          lastStateInCfa = Optional.empty();

        } else if (cfa.getAllNodes().contains(location) && targetPathIt.isPositionWithState()) {
          lastStateInCfa = Optional.of(targetPathIt.getAbstractState());
        }
      }
    } while (targetPathIt.advanceIfPossible());

    ARGPath newTargetPath = builder.build(newTargetState);
    CounterexampleInfo newCounterexample = CounterexampleInfo.feasibleImprecise(newTargetPath);
    return newCounterexample;
  }

  private AlgorithmStatus checkRecursion(CFANode initialLocation)
      throws CPAEnabledAnalysisPropertyViolationException, CPAException, InterruptedException {
    shutdownNotifier.shutdownIfNecessary();
    statistics.analysisOfRecursionStarted();

    // the safety analysis will fail if the program is recursive
    try {
      terminationInformation.reset();
      ReachedSet reachedSet = reachedSetFactory.create();
      resetReachedSet(reachedSet, initialLocation);
      return safetyAlgorithm.run(reachedSet);
    } finally {
      statistics.analysisOfRecursionFinished();
    }
  }

  private void prepareForNextIteration(
      ReachedSet pReachedSet, ARGState pTargetState, CFANode pInitialLocation)
      throws InterruptedException {

    switch (resetReachedSetStrategy) {
      case REMOVE_TARGET_STATE:
        pTargetState.getParents().forEach(pReachedSet::reAddToWaitlist);
        removeTargetState(pReachedSet, pTargetState);
        break;

      case REMOVE_LOOP:
        removeLoop(pReachedSet, pTargetState);
        break;

      case RESET:
        resetReachedSet(pReachedSet, pInitialLocation);
        break;

      default:
        throw new AssertionError(resetReachedSetStrategy);
    }
  }

  /**
   * Removes <code>pTargetState</code> from reached set and ARG.
   */
  private void removeTargetState(ReachedSet pReachedSet, ARGState pTargetState) {
    assert pTargetState.isTarget();
    pReachedSet.remove(pTargetState);
    pTargetState.removeFromARG();
  }

  private void removeLoop(ReachedSet pReachedSet, ARGState pTargetState) {
    Set<ARGState> workList = Sets.newHashSet(pTargetState);
    Set<ARGState> firstLoopStates = Sets.newHashSet();

    // get all loop states having only stem predecessors
    while (!workList.isEmpty()) {
      ARGState next = workList.iterator().next();
      workList.remove(next);

      Collection<ARGState> parentLoopStates =
          next.getParents()
              .stream()
              .filter(p -> extractStateByType(p, TerminationState.class).isPartOfLoop())
              .collect(Collectors.toList());

      if (parentLoopStates.isEmpty()) {
        firstLoopStates.add(next);
      } else {
        workList.addAll(parentLoopStates);
      }
    }

    ARGReachedSet argReachedSet = new ARGReachedSet(pReachedSet);
    firstLoopStates.forEach(argReachedSet::removeSubtree);
  }

  private void resetReachedSet(ReachedSet pReachedSet, CFANode pInitialLocation)
      throws InterruptedException {
    AbstractState initialState = safetyCPA.getInitialState(pInitialLocation, getDefaultPartition());
    Precision initialPrecision =
        safetyCPA.getInitialPrecision(pInitialLocation, getDefaultPartition());
    pReachedSet.clear();
    pReachedSet.add(initialState, initialPrecision);
  }

  private static class DeclarationCollectionCFAVisitor extends DefaultCFAVisitor {

    private final Set<CVariableDeclaration> globalDeclarations = Sets.newLinkedHashSet();

    private final Multimap<String, CVariableDeclaration> localDeclarations =
        MultimapBuilder.hashKeys().linkedHashSetValues().build();

    @Override
    public TraversalProcess visitNode(CFANode pNode) {

      if (pNode instanceof CFunctionEntryNode) {
        String functionName = pNode.getFunctionName();
        List<CParameterDeclaration> parameters =
            ((CFunctionEntryNode) pNode).getFunctionParameters();
        parameters
            .stream()
            .map(CParameterDeclaration::asVariableDeclaration)
            .forEach(localDeclarations.get(functionName)::add);
      }
      return TraversalProcess.CONTINUE;
    }

    @Override
    public TraversalProcess visitEdge(CFAEdge pEdge) {

      if (pEdge instanceof CDeclarationEdge) {
        CDeclaration declaration = ((CDeclarationEdge) pEdge).getDeclaration();
        if (declaration instanceof CVariableDeclaration) {
          CVariableDeclaration variableDeclaration = (CVariableDeclaration) declaration;

          if (variableDeclaration.isGlobal()) {
            globalDeclarations.add(variableDeclaration);

          } else {
            String functionName = pEdge.getPredecessor().getFunctionName();
            localDeclarations.put(functionName, variableDeclaration);
          }
        }
      }
      return TraversalProcess.CONTINUE;
    }
  }
}
