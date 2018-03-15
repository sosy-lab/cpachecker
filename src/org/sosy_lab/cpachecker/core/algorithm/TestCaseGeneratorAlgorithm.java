/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm;

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.AbstractStates.IS_TARGET_STATE;

import com.google.common.base.Predicates;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.Appender;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.UniqueIdGenerator;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.counterexample.AssumptionToEdgeAllocator;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.testtargets.TestTargetCPA;
import org.sosy_lab.cpachecker.cpa.testtargets.TestTargetProvider;
import org.sosy_lab.cpachecker.cpa.testtargets.TestTargetTransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CounterexampleAnalysisFailed;
import org.sosy_lab.cpachecker.exceptions.InfeasibleCounterexampleException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.harness.HarnessExporter;

@Options(prefix = "testcase")
public class TestCaseGeneratorAlgorithm implements Algorithm, StatisticsProvider {

  private static UniqueIdGenerator id = new UniqueIdGenerator();

  @Option(secure = true, name = "file", description = "export test harness to file as code")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate testHarnessFile = null;

  @Option(
    secure = true,
    name = "inStats",
    description = "display all test targets and non-covered test targets in statistics"
  )
  private boolean printTestTargetInfoInStats = false;

  private final Algorithm algorithm;
  private final AssumptionToEdgeAllocator assumptionToEdgeAllocator;
  private final ConfigurableProgramAnalysis cpa;
  private final HarnessExporter harnessExporter;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private Set<CFAEdge> testTargets;

  public TestCaseGeneratorAlgorithm(
      Algorithm pAlgorithm,
      CFA pCfa,
      Configuration pConfig,
      ConfigurableProgramAnalysis pCpa,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    CPAs.retrieveCPAOrFail(pCpa, ARGCPA.class, TestCaseGeneratorAlgorithm.class);
    algorithm = pAlgorithm;
    cpa = pCpa;
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    assumptionToEdgeAllocator =
        AssumptionToEdgeAllocator.create(pConfig, logger, pCfa.getMachineModel());
    TestTargetCPA testTargetCpa =
        CPAs.retrieveCPAOrFail(pCpa, TestTargetCPA.class, TestCaseGeneratorAlgorithm.class);
    testTargets =
        ((TestTargetTransferRelation) testTargetCpa.getTransferRelation()).getTestTargets();
    harnessExporter = new HarnessExporter(pConfig, logger, pCfa);
  }

  @Override
  public AlgorithmStatus run(final ReachedSet pReached)
      throws CPAException, InterruptedException, CPAEnabledAnalysisPropertyViolationException {
    if (pReached.getWaitlist().size() > 1
        || !pReached.getWaitlist().contains(pReached.getFirstState())) {
      pReached
          .getWaitlist()
          .stream()
          .filter(
              (AbstractState state) -> {
                return ((ARGState) state).getChildren().size() > 0;
              })
          .forEach(
              (AbstractState state) -> {
                ARGState argState = (ARGState) state;
                List<ARGState> removedChildren = new ArrayList<>(2);
                for (ARGState child : argState.getChildren()) {
                  if (!pReached.contains(child)) {
                    removedChildren.add(child);
                  }
                }
                for (ARGState child : removedChildren) {
                  child.removeFromARG();
                }
              });
    }

    while (pReached.hasWaitingState() && !testTargets.isEmpty()) {
      shutdownNotifier.shutdownIfNecessary();

      assert ARGUtils.checkARG(pReached);
      assert (from(pReached).filter(IS_TARGET_STATE).isEmpty());

      AlgorithmStatus status = AlgorithmStatus.UNSOUND_AND_PRECISE.withPrecise(false);
      try {
        status = algorithm.run(pReached);

      } catch (CPAException e) {
        if (e instanceof CounterexampleAnalysisFailed
            || e instanceof RefinementFailedException
            || e instanceof InfeasibleCounterexampleException) {
          status = status.withPrecise(false);
        }

        logger.logUserException(Level.WARNING, e, "Analysis not completed.");

      } finally {

        assert ARGUtils.checkARG(pReached);
        assert (from(pReached).filter(IS_TARGET_STATE).size() < 2);

        AbstractState reachedState = from(pReached).firstMatch(IS_TARGET_STATE).orNull();
        if (reachedState != null) {

          ARGState argState = (ARGState) reachedState;

          Collection<ARGState> parentArgStates = argState.getParents();

          assert (parentArgStates.size() == 1);

          ARGState parentArgState = parentArgStates.iterator().next();

          CFAEdge targetEdge = parentArgState.getEdgeToChild(argState);
          if (targetEdge != null) {
            if (testTargets.contains(targetEdge)) {

              if (status.isPrecise()) {
                writeTestHarnessFile(argState);

                logger.log(Level.FINE, "Removing test target: " + targetEdge.toString());
                testTargets.remove(targetEdge);
              } else {
                logger.log(
                    Level.FINE,
                    "Status was not precise. Current test target is not removed:"
                        + targetEdge.toString());
              }
            } else {
              logger.log(
                  Level.FINE,
                  "Found test target is not in provided set of test targets:"
                      + targetEdge.toString());
            }
          } else {
            logger.log(Level.FINE, "Target edge was null.");
          }

          argState.removeFromARG();
          pReached.remove(reachedState);
          pReached.reAddToWaitlist(parentArgState);

          assert ARGUtils.checkARG(pReached);
        } else {
          logger.log(Level.FINE, "There was no target state in the reached set.");
        }
      }
    }

    cleanUpIfNoTestTargetsRemain(pReached);

    return AlgorithmStatus.SOUND_AND_PRECISE;
  }

  private void cleanUpIfNoTestTargetsRemain(final ReachedSet pReached) {
    if (testTargets.isEmpty()) {
      List<AbstractState> waitlist = new ArrayList<>(pReached.getWaitlist());
      for (AbstractState state : waitlist) {
        pReached.removeOnlyFromWaitlist(state);
      }
    }
  }

  private void writeTestHarnessFile(final ARGState pTarget) {
    if (testHarnessFile != null) {
      CounterexampleInfo cexInfo = extractCexInfo(pTarget);

      Path file = testHarnessFile.getPath(id.getFreshId());
      ARGPath targetPath = cexInfo.getTargetPath();
      Object content =
          (Appender)
              appendable ->
                  harnessExporter.writeHarness(
                      appendable,
                      targetPath.getFirstState(),
                      Predicates.in(targetPath.getStateSet()),
                      Predicates.in(targetPath.getStatePairs()),
                      cexInfo);
      try {
        IO.writeFile(file, Charset.defaultCharset(), content);
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Could not write test harness to file");
      }
    }
  }

  private CounterexampleInfo extractCexInfo(final ARGState pTarget) {
    // TODO may not contain sufficient information to write test harness, e.g. when using
    // ValueAnalysis
    if (pTarget.getCounterexampleInformation().isPresent()) {
      return pTarget.getCounterexampleInformation().get();
    }

    return ARGUtils.tryGetOrCreateCounterexampleInformation(pTarget, cpa, assumptionToEdgeAllocator)
        .get();
  }

  @Override
  public void collectStatistics(final Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(TestTargetProvider.getTestTargetStatisitics(printTestTargetInfoInStats));
  }
}
