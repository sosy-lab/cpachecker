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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import javax.annotation.Nullable;
import org.sosy_lab.common.Appender;
import org.sosy_lab.common.ShutdownNotifier;
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
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.counterexample.AssumptionToEdgeAllocator;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.testtargets.TestTargetCPA;
import org.sosy_lab.cpachecker.cpa.testtargets.TestTargetProvider;
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CounterexampleAnalysisFailed;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.harness.HarnessExporter;

@Options
public class TestCaseGeneratorAlgorithm implements Algorithm, StatisticsProvider {

  private static class TestCaseGeneratorAlgorithmStatistics implements Statistics {
    private final ImmutableSet<CFAEdge> initialTestTargets;
    private final Set<CFAEdge> coveredTestTargets;
    private final Set<CFAEdge> uncoveredTestTargets;

    public TestCaseGeneratorAlgorithmStatistics() {
      TestTargetProvider testTargetProvider = TestTargetProvider.getInstance();
      initialTestTargets = testTargetProvider.getInitialTestTargets();
      coveredTestTargets = testTargetProvider.getCoveredTestTargets();
      uncoveredTestTargets = testTargetProvider.getTestTargets();
    }

    @Override
    public @Nullable String getName() {
      return "Test Case Generator Algorithm";
    }

    @Override
    public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
      double testTargetCoverage =
          initialTestTargets.size() == 0
              ? 0
              : (double) coveredTestTargets.size() / (double) initialTestTargets.size();
      pOut.printf("Test target coverage: %.2f%%%n", testTargetCoverage * 100);
      pOut.println("Number of initial test targets: " + initialTestTargets.size());
      pOut.println("Number of covered test targets: " + coveredTestTargets.size());
      pOut.println(
          "Number of uncovered test targets: "
              + (initialTestTargets.size() - coveredTestTargets.size()));
      pOut.println("Initial test targets: ");
      for (CFAEdge edge : initialTestTargets) {
        pOut.println(edge.toString());
      }
      pOut.println("Test targets that have been covered: ");
      for (CFAEdge edge : coveredTestTargets) {
        pOut.println(edge.toString());
      }
      assert Sets.difference(
              Sets.difference(initialTestTargets, coveredTestTargets), uncoveredTestTargets)
          .isEmpty();
      pOut.println("Test targets that have not been covered: ");
      for (CFAEdge edge : uncoveredTestTargets) {
        pOut.println(edge.toString());
      }
    }
  }

  @Option(secure = true, name = "harness", description = "export test harness to file as code")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate testHarnessFile = null;

  private final Algorithm algorithm;
  private final AssumptionToEdgeAllocator assumptionToEdgeAllocator;
  private final TestTargetCPA testTargetCpa;
  private final HarnessExporter harnessExporter;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final TestCaseGeneratorAlgorithmStatistics stats;
  private Set<CFAEdge> testTargets;

  public TestCaseGeneratorAlgorithm(
      Algorithm pAlgorithm,
      CFA pCfa,
      Configuration pConfig,
      ConfigurableProgramAnalysis pCpa,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    CPAs.retrieveCPAOrFail(pCpa, ARGCPA.class, TestCaseGeneratorAlgorithm.class);
    algorithm = pAlgorithm;
    assumptionToEdgeAllocator =
        AssumptionToEdgeAllocator.create(pConfig, pLogger, pCfa.getMachineModel());
    testTargetCpa =
        CPAs.retrieveCPAOrFail(pCpa, TestTargetCPA.class, TestCaseGeneratorAlgorithm.class);
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    testTargets = testTargetCpa.getTestTargets();
    stats = new TestCaseGeneratorAlgorithmStatistics();
    harnessExporter = new HarnessExporter(pConfig, logger, pCfa);
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReached)
      throws CPAException, InterruptedException, CPAEnabledAnalysisPropertyViolationException {
    try {
      while (!testTargets.isEmpty() && pReached.hasWaitingState()) {
        shutdownNotifier.shutdownIfNecessary();

        assert ARGUtils.checkARG(pReached);
        assert (from(pReached).filter(IS_TARGET_STATE).isEmpty());

        AlgorithmStatus status = AlgorithmStatus.UNSOUND_AND_PRECISE;
        Set<AbstractState> oldReachedStates = new HashSet<>(pReached.asCollection());
        try {
          status = algorithm.run(pReached);
        } catch (CPAException e) {
          if (e instanceof CounterexampleAnalysisFailed || e instanceof RefinementFailedException) {
            status = status.withPrecise(false);
          }

          logger.logUserException(Level.WARNING, e, "Analysis not completed.");

        } finally {

          if (!status.isSound()) {
            Set<AbstractState> recentlyReachedStates =
                Sets.difference(new HashSet<>(pReached.asCollection()), oldReachedStates);
            for (AbstractState e : recentlyReachedStates) {
              ((ARGState) e).removeFromARG();
              pReached.remove(e);
            }
          }
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
                  writeTestHarnessFile(targetEdge, argState);

                  logger.log(Level.INFO, "Removing test target: " + targetEdge.toString());
                  testTargets.remove(targetEdge);
                  stats.coveredTestTargets.add(targetEdge);
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
    } finally {
      if (testTargets.isEmpty()) {
        List<AbstractState> waitlist = new ArrayList<>(pReached.getWaitlist());
        for (AbstractState state : waitlist) {
          pReached.removeOnlyFromWaitlist(state);
        }
      }
    }
    return AlgorithmStatus.SOUND_AND_PRECISE;
  }

  private void writeTestHarnessFile(CFAEdge pTargetEdge, ARGState pArgState) {
    if (testHarnessFile != null) {
      CounterexampleInfo cexInfo =
          ARGUtils.tryGetOrCreateCounterexampleInformation(
                  pArgState, testTargetCpa, assumptionToEdgeAllocator)
              .get();
      Path file = testHarnessFile.getPath(pTargetEdge.getLineNumber());
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

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
  }
}
