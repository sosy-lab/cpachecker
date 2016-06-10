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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
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
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.termination.TerminationCPA;
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.DefaultCFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;
import org.sosy_lab.cpachecker.util.CPAs;

import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;

/**
 * Algorithm that uses a safety-analysis to prove (non-)termination.
 */
@Options
public class TerminationAlgorithm implements Algorithm {

  @Option(
      secure=true,
      name="termination.check",
      description="Whether to check for the termination property "
          + "(this can be specified by passing an appropriate .prp file to the -spec parameter).")
  private boolean checkTermination = false;

  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final CFA cfa;
  private final Algorithm safetyAlgorithm;

  private final TerminationCPA terminationCpa;
  private final Set<CVariableDeclaration> globalDeclaration;
  private final SetMultimap<String, CVariableDeclaration> localDeclarations;

  public TerminationAlgorithm(
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      CFA pCfa,
      Algorithm pSafetyAlgorithm,
      ConfigurableProgramAnalysis pSafetyAnalysis)
          throws InvalidConfigurationException {
        logger = checkNotNull(pLogger);
        shutdownNotifier = pShutdownNotifier;
        safetyAlgorithm = checkNotNull(pSafetyAlgorithm);
        cfa = checkNotNull(pCfa);
        pConfig.inject(this);

        terminationCpa = CPAs.retrieveCPA(pSafetyAnalysis, TerminationCPA.class);
        if (terminationCpa == null) {
          throw new InvalidConfigurationException("TerminationAlgorithm requires TerminationCPA");
        }

        DeclarationCollectionCFAVisitor visitor = new DeclarationCollectionCFAVisitor();
        for (CFANode function : cfa.getAllFunctionHeads()) {
          CFATraversal.dfs().ignoreFunctionCalls().traverseOnce(function, visitor);
        }
        localDeclarations = ImmutableSetMultimap.copyOf(visitor.localDeclarations);
        globalDeclaration = ImmutableSet.copyOf(visitor.globalDeclaration);

  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet)
      throws CPAException, InterruptedException, CPAEnabledAnalysisPropertyViolationException {

    logger.log(Level.INFO, "Starting termination algorithm.");

    if (!checkTermination) {
      logger.logf(WARNING, "%s does not support safety properties.", getClass().getSimpleName());
      return AlgorithmStatus.UNSOUND_AND_PRECISE.withPrecise(false);

    } else if (!cfa.getLoopStructure().isPresent()) {
      logger.log(WARNING, "Loop structure is not present, but required for termination analysis.");
      return AlgorithmStatus.UNSOUND_AND_PRECISE.withPrecise(false);

    } else if (cfa.getLanguage() != Language.C) {
      logger.log(WARNING, "Termination analysis supports only C.");
      return AlgorithmStatus.UNSOUND_AND_PRECISE.withPrecise(false);
    }

    AbstractState entryState = pReachedSet.getFirstState();
    Precision entryStatePrecision = pReachedSet.getPrecision(entryState);

    AlgorithmStatus status = AlgorithmStatus.SOUND_AND_PRECISE;
    ImmutableSet<CFANode> loopHeads = cfa.getAllLoopHeads().get();

    for (CFANode loopHead : loopHeads) {
      shutdownNotifier.shutdownIfNecessary();
      CPAcheckerResult.Result loopTermiantion = prooveLoopTermination(pReachedSet, loopHead);

      if (loopTermiantion == Result.FALSE) {
        logger.logf(Level.FINE, "Proved non-termination of loop with head %s.", loopHead);
        return status.withSound(false);

      } else if (loopTermiantion != Result.TRUE) {
        logger.logf(FINE, "Could not prove (non-)termination of loop with head %s.", loopHead);
        status = status.withSound(false);
      }

      // Prepare reached set for next loop.
      pReachedSet.clear();
      pReachedSet.add(entryState, entryStatePrecision);
    }

    // We did not find a non-terminating loop.
    logger.log(Level.INFO, "Termination algorithm did not find a non-terminating loop.");
    while (pReachedSet.hasWaitingState()) {
      pReachedSet.popFromWaitlist();
    }
    return status;
  }

  private Result prooveLoopTermination(ReachedSet pReachedSet, CFANode pLoopHead)
          throws CPAEnabledAnalysisPropertyViolationException, CPAException, InterruptedException {

    logger.logf(Level.FINE, "Prooving (non)-termination of loop with head %s", pLoopHead);

    // Pass current loop and relevant variables to TerminationCPA.
    String function = pLoopHead.getFunctionName();
    Set<CVariableDeclaration> relevantVariabels =
        ImmutableSet
          .<CVariableDeclaration>builder()
          .addAll(globalDeclaration)
          .addAll(localDeclarations.get(function))
          .build();
    terminationCpa.setProcessedLoop(pLoopHead, relevantVariabels);

    AlgorithmStatus status = safetyAlgorithm.run(pReachedSet);
    Optional<AbstractState> targetState =
        pReachedSet.asCollection().stream().filter(AbstractStates::isTargetState).findAny();

    Result result = null;

    while (result == null) {
      if (status.isSound() && !targetState.isPresent()) {
        result = Result.TRUE;

      } else if (status.isPrecise() && targetState.isPresent()){
        // TODO extract error path and try to find a ranking function
        result =  Result.UNKNOWN;

      } else {
        result =  Result.UNKNOWN;
      }
    }

    return result;
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
