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

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;

import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Level;

import javax.annotation.Nullable;

/**
 * Algorithm that uses a safety-analysis to prove (non-)termination.
 */
@Options
public class TerminationAlgorithm implements Algorithm {

  @Option(
      secure = true,
      name = "terminationAlgorithm.safteyConfig",
      required = true,
      description =
          "File with configuration to use for safety properties."
    )
    @FileOption(FileOption.Type.REQUIRED_INPUT_FILE)
    private @Nullable Path safteyConfig;

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

  public TerminationAlgorithm(
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      CFA pCfa,
      Algorithm pSafetyAlgorithm)
          throws InvalidConfigurationException {
        logger = checkNotNull(pLogger);
        shutdownNotifier = pShutdownNotifier;
        safetyAlgorithm = checkNotNull(pSafetyAlgorithm);
        cfa = checkNotNull(pCfa);
        pConfig.inject(this);
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet)
      throws CPAException, InterruptedException, CPAEnabledAnalysisPropertyViolationException {

    if (!checkTermination) {
      logger.logf(WARNING, "%s does not support safety properties.", getClass().getSimpleName());
      return AlgorithmStatus.UNSOUND_AND_PRECISE.withPrecise(false);

    } else if (!cfa.getLoopStructure().isPresent()) {
      logger.log(WARNING, "Loop structure is not present, but required for termination analysis.");
      return AlgorithmStatus.UNSOUND_AND_PRECISE;
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
    return status;
  }

  private Result prooveLoopTermination(ReachedSet pReachedSet, CFANode pLoopHead)
          throws CPAEnabledAnalysisPropertyViolationException, CPAException, InterruptedException {

    logger.logf(Level.FINE, "Prooving (non)-termination of loop with head %s", pLoopHead);

    // TODO Add additional nodes and edges to CFA
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
}
