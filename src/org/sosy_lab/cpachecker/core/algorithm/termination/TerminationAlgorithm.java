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
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.WARNING;
import static org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition.getDefaultPartition;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractStateByType;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.core.CPAcheckerResult;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.Specification;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.defaults.NamedProperty;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.ARGPathBuilder;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathIterator;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.termination.TerminationCPA;
import org.sosy_lab.cpachecker.cpa.termination.TerminationState;
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.DefaultCFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;

import javax.annotation.Nullable;

/**
 * Algorithm that uses a safety-analysis to prove (non-)termination.
 */
public class TerminationAlgorithm implements Algorithm {

  private final static Set<Property> TERMINATION_PROPERTY = NamedProperty.singleton("termination");

  private final static Path SPEC_FILE = Paths.get("config/specification/termination_as_reach.spc");

  @Nullable private static Specification terminationSpecification;

  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final CFA cfa;
  private final Algorithm safetyAlgorithm;
  private final ConfigurableProgramAnalysis safetyCPA;

  private final LassoAnalysis lassoAnalysis;
  private final TerminationCPA terminationCpa;
  private final Set<CVariableDeclaration> globalDeclaration;
  private final SetMultimap<String, CVariableDeclaration> localDeclarations;

  public TerminationAlgorithm(
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      CFA pCfa,
      Specification pSpecification,
      Algorithm pSafetyAlgorithm,
      ConfigurableProgramAnalysis pSafetyCPA)
      throws InvalidConfigurationException {
    logger = checkNotNull(pLogger);
    shutdownNotifier = pShutdownNotifier;
    safetyAlgorithm = checkNotNull(pSafetyAlgorithm);
    safetyCPA = checkNotNull(pSafetyCPA);
    cfa = checkNotNull(pCfa);

    Specification requiredSpecification = loadTerminationSpecification(pCfa, pConfig, pLogger);
    Preconditions.checkArgument(
        requiredSpecification.equals(pSpecification),
        "%s requires %s, but %s is given.",
        TerminationAlgorithm.class.getSimpleName(),
        requiredSpecification,
        pSpecification);

    terminationCpa = CPAs.retrieveCPA(pSafetyCPA, TerminationCPA.class);
    if (terminationCpa == null) {
      throw new InvalidConfigurationException("TerminationAlgorithm requires TerminationCPA");
    }

    ARGCPA agrCpa = CPAs.retrieveCPA(pSafetyCPA, ARGCPA.class);
    if (agrCpa == null) {
      throw new InvalidConfigurationException("TerminationAlgorithm requires ARGCPA");
    }

    DeclarationCollectionCFAVisitor visitor = new DeclarationCollectionCFAVisitor();
    for (CFANode function : cfa.getAllFunctionHeads()) {
      CFATraversal.dfs().ignoreFunctionCalls().traverseOnce(function, visitor);
    }
    localDeclarations = ImmutableSetMultimap.copyOf(visitor.localDeclarations);
    globalDeclaration = ImmutableSet.copyOf(visitor.globalDeclaration);

    // ugly class loader hack
    LassoAnalysisLoader lassoAnalysisLoader =
        new LassoAnalysisLoader(pConfig, pLogger, pShutdownNotifier, pCfa);
    lassoAnalysis = lassoAnalysisLoader.load();
  }

  /**
   * Loads the specification required to run the {@link TerminationAlgorithm}.
   */
  public static Specification loadTerminationSpecification(
      CFA pCfa, Configuration pConfig, LogManager pLogger) throws InvalidConfigurationException {
    if (terminationSpecification == null) {
      terminationSpecification =
          Specification.fromFiles(Collections.singleton(SPEC_FILE), pCfa, pConfig, pLogger);
    }

    return terminationSpecification;
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet)
      throws CPAException, InterruptedException, CPAEnabledAnalysisPropertyViolationException {

    logger.log(Level.INFO, "Starting termination algorithm.");

    if (!cfa.getLoopStructure().isPresent()) {
      logger.log(WARNING, "Loop structure is not present, but required for termination analysis.");
      return AlgorithmStatus.UNSOUND_AND_PRECISE.withPrecise(false);

    } else if (cfa.getLanguage() != Language.C) {
      logger.log(WARNING, "Termination analysis supports only C.");
      return AlgorithmStatus.UNSOUND_AND_PRECISE.withPrecise(false);
    }

    CFANode initialLocation = AbstractStates.extractLocation(pReachedSet.getFirstState());

    AlgorithmStatus status = AlgorithmStatus.SOUND_AND_PRECISE;

    Collection<Loop> allLoops = cfa.getLoopStructure().get().getAllLoops();
    for (Loop loop : allLoops) {
      shutdownNotifier.shutdownIfNecessary();
      CPAcheckerResult.Result loopTermiantion =
          prooveLoopTermination(pReachedSet, loop, initialLocation);

      if (loopTermiantion == Result.FALSE) {
        logger.logf(Level.FINE, "Proved non-termination of %s.", loop);
        return status.withSound(false);

      } else if (loopTermiantion != Result.TRUE) {
        logger.logf(FINE, "Could not prove (non-)termination of %s.", loop);
        status = status.withSound(false);
      }

      // Prepare reached set for next loop.
      resetReachedSet(pReachedSet, initialLocation);
    }

    // We did not find a non-terminating loop.
    logger.log(Level.INFO, "Termination algorithm did not find a non-terminating loop.");
    while (pReachedSet.hasWaitingState()) {
      pReachedSet.popFromWaitlist();
    }
    return status;
  }

  private Result prooveLoopTermination(ReachedSet pReachedSet, Loop pLoop, CFANode initialLocation)
      throws CPAEnabledAnalysisPropertyViolationException, CPAException, InterruptedException {

    logger.logf(Level.FINE, "Prooving (non)-termination of %s", pLoop);

    // Pass current loop and relevant variables to TerminationCPA.
    String function = pLoop.getLoopHeads().iterator().next().getFunctionName();
    Set<CVariableDeclaration> relevantVariabels =
        ImmutableSet.<CVariableDeclaration>builder()
            .addAll(globalDeclaration)
            .addAll(localDeclarations.get(function))
            .build();
    terminationCpa.setProcessedLoop(pLoop, relevantVariabels);

    Result result = null;

    while (result == null) {
      shutdownNotifier.shutdownIfNecessary();
      AlgorithmStatus status = safetyAlgorithm.run(pReachedSet);
      terminationCpa.resetCfa();
      shutdownNotifier.shutdownIfNecessary();

      boolean targetReached = pReachedSet
          .asCollection()
          .stream()
          .anyMatch(AbstractStates::isTargetState);
      Optional<ARGState> targetStateWithCounterExample =
          pReachedSet
              .asCollection()
              .stream()
              .filter(AbstractStates::isTargetState)
              .map(s -> AbstractStates.extractStateByType(s, ARGState.class))
              .filter(s -> s.getCounterexampleInformation().isPresent())
              .findAny();

      if (status.isSound() && !targetReached && !pReachedSet.hasWaitingState()) {
        result = Result.TRUE;

      } else if (status.isPrecise() && targetStateWithCounterExample.isPresent()) {
        AbstractState newTargetState =
            removeIntermediateStatesFromCounterexample(targetStateWithCounterExample.get());
        replaceInReachedSet(pReachedSet, targetStateWithCounterExample.get(), newTargetState);

        LassoAnalysis.LassoAnalysisResult lassoAnalysisResult =
            lassoAnalysis.checkTermination(newTargetState, relevantVariabels);

        if (lassoAnalysisResult.getNonTerminationArgument().isPresent()) {
          result = Result.FALSE;

        } else if (lassoAnalysisResult.getTerminationArgument().isPresent()) {

          RankingRelation rankingRelation = lassoAnalysisResult.getTerminationArgument().get();
          terminationCpa.addRankingRelation(rankingRelation);

          // Prepare reached set for next iteration.
          resetReachedSet(pReachedSet, initialLocation);

        } else {
          result = Result.UNKNOWN;
        }

      } else {
        result = Result.UNKNOWN;
      }
    }

    return result;
  }

  private void replaceInReachedSet(
      ReachedSet pReachedSet, AbstractState oldState, AbstractState newState) {
    pReachedSet.add(newState, pReachedSet.getPrecision(oldState));
    pReachedSet.removeOnlyFromWaitlist(newState);
    pReachedSet.remove(oldState);
  }

  private AbstractState removeIntermediateStatesFromCounterexample(AbstractState pTargetState) {
    Preconditions.checkArgument(AbstractStates.isTargetState(pTargetState));
    Preconditions.checkArgument(!cfa.getAllNodes().contains(extractLocation(pTargetState)));
    ARGState targetState = AbstractStates.extractStateByType(pTargetState, ARGState.class);

    Optional<CounterexampleInfo> counterexample = targetState.getCounterexampleInformation();
    if (!counterexample.isPresent()) {
      return pTargetState;
    }

    // Remove dummy target state from ARG and replace loop head with new target state
    ARGState loopHead = Iterables.getOnlyElement(targetState.getParents());
    TerminationState terminationState = extractStateByType(loopHead, TerminationState.class);
    AbstractState newTerminationState =
        terminationState.withViolatedProperties(TERMINATION_PROPERTY);
    ARGState newTargetState = new ARGState(newTerminationState, null);
    targetState.removeFromARG();
    loopHead.replaceInARGWith(newTargetState);

    // Remove all intermediate states from the counterexample.
    // The value assignments are not valid for a counterexample that witnesses non-termination.
    ARGPath targetPath = counterexample.get().getTargetPath();
    PathIterator targetPathIt = targetPath.fullPathIterator();
    ARGPathBuilder builder = ARGPath.builder();
    Optional<ARGState> lastStateInCfa = Optional.empty();

    do {
      // the last state has not outgoing edge
      if (targetPathIt.hasNext())  {
        CFAEdge outgoingEdge = targetPathIt.getOutgoingEdge();
        CFANode location = outgoingEdge.getPredecessor();
        CFANode nextLocation = outgoingEdge.getSuccessor();

        if (cfa.getAllNodes().contains(location)
            && cfa.getAllNodes().contains(nextLocation)) {

          if (targetPathIt.isPositionWithState()
              || lastStateInCfa.isPresent()) {
            ARGState state = lastStateInCfa.orElseGet(targetPathIt::getAbstractState);
            @Nullable CFAEdge edgeToNextState =
                state.getEdgeToChild(targetPathIt.getNextAbstractState());
            builder.add(state, edgeToNextState);
          }

          lastStateInCfa = Optional.empty();

        } else if (cfa.getAllNodes().contains(location)
            && targetPathIt.isPositionWithState()) {
          lastStateInCfa = Optional.of(targetPathIt.getAbstractState());
        }
      }
    } while (targetPathIt.advanceIfPossible());

    ARGPath newTargetPath = builder.build(newTargetState);
    CounterexampleInfo newCounterexample = CounterexampleInfo.feasibleImprecise(newTargetPath);
    newTargetState.addCounterexampleInformation(newCounterexample);

    return newTargetState;
  }

  private void resetReachedSet(ReachedSet pReachedSet, CFANode initialLocation)
      throws InterruptedException {
    AbstractState initialState = safetyCPA.getInitialState(initialLocation, getDefaultPartition());
    Precision initialPrecision =
        safetyCPA.getInitialPrecision(initialLocation, getDefaultPartition());
    pReachedSet.clear();
    pReachedSet.add(initialState, initialPrecision);
  }

  private static class DeclarationCollectionCFAVisitor extends DefaultCFAVisitor {

    private final Set<CVariableDeclaration> globalDeclaration = Sets.newLinkedHashSet();

    private final Multimap<String, CVariableDeclaration> localDeclarations =
        MultimapBuilder.hashKeys().linkedHashSetValues().build();

    @Override
    public TraversalProcess visitEdge(CFAEdge pEdge) {
      if (pEdge instanceof CDeclarationEdge) {
        CDeclaration declaration = ((CDeclarationEdge) pEdge).getDeclaration();
        if (declaration instanceof CVariableDeclaration) {
          CVariableDeclaration variableDeclaration = (CVariableDeclaration) declaration;

          if (variableDeclaration.isGlobal()) {
            globalDeclaration.add(variableDeclaration);
          } else {
            localDeclarations.put(pEdge.getPredecessor().getFunctionName(), variableDeclaration);
          }
        }
      }

      return TraversalProcess.CONTINUE;
    }
  }
}
