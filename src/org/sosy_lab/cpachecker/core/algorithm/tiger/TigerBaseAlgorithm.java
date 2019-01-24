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
 */
package org.sosy_lab.cpachecker.core.algorithm.tiger;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import javax.annotation.Nullable;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.ShutdownNotifier.ShutdownRequestListener;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.Specification;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.AlgorithmResult;
import org.sosy_lab.cpachecker.core.algorithm.AlgorithmWithResult;
import org.sosy_lab.cpachecker.core.algorithm.CEGARAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.testgen.util.StartupConfig;
import org.sosy_lab.cpachecker.core.algorithm.tiger.goals.Goal;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.BDDUtils;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.TestCase;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.TestCaseVariable;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.TestSuite;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.TestSuiteWriter;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGStatistics;
import org.sosy_lab.cpachecker.cpa.arg.ErrorPathShrinker;
import org.sosy_lab.cpachecker.cpa.timeout.TimeoutCPA;
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.Property;
import org.sosy_lab.cpachecker.util.Property.CommonCoverageType;
import org.sosy_lab.cpachecker.util.SpecificationProperty;
import org.sosy_lab.cpachecker.util.predicates.regions.Region;

public abstract class TigerBaseAlgorithm<T extends Goal>
    implements AlgorithmWithResult, ShutdownRequestListener {
  enum TimeoutStrategy {
    SKIP_AFTER_TIMEOUT,
    RETRY_AFTER_TIMEOUT
  }
  enum ReachabilityAnalysisResult {
    SOUND,
    UNSOUND,
    TIMEDOUT
  }

  public static String originalMainFunction = null;
  protected TigerAlgorithmConfiguration tigerConfig;
  protected int currentTestCaseID;
  protected InputOutputValues values;
  protected LogManager logger;
  protected CFA cfa;
  protected ConfigurableProgramAnalysis cpa;
  protected Configuration config;
  // protected ReachedSet reachedSet = null;
  protected StartupConfig startupConfig;
  protected Specification stats;
  protected TestSuite<T> testsuite;
  protected BDDUtils bddUtils;
  protected TimeoutCPA timeoutCPA;
  protected TestSuiteWriter tsWriter;

  protected LinkedList<T> goalsToCover;

  protected void init(
      LogManager pLogger,
      CFA pCfa,
      Configuration pConfig,
      ConfigurableProgramAnalysis pCpa,
      ShutdownNotifier pShutdownNotifier,
      @Nullable final Specification pStats)
      throws InvalidConfigurationException {
    tigerConfig = new TigerAlgorithmConfiguration(pConfig);
    cfa = pCfa;
    cpa = pCpa;
    startupConfig = new StartupConfig(pConfig, pLogger, pShutdownNotifier);
    // startupConfig.getConfig().inject(this);
    logger = pLogger;
    if (pStats != null && pStats.getProperties() != null) {
      for (SpecificationProperty specProperty : pStats.getProperties()) {
        originalMainFunction = specProperty.getEntryFunction();
        Property property = specProperty.getProperty();
        if (property instanceof CommonCoverageType) {
          tigerConfig.setFQLQuery(((CommonCoverageType) property).toString());
        }
      }
    }

    assert originalMainFunction != null;
    config = pConfig;

    logger.logf(Level.INFO, "FQL query string: %s", tigerConfig.getFqlQuery());
    this.stats = pStats;
    values =
        new InputOutputValues(tigerConfig.getInputInterface(), tigerConfig.getOutputInterface());
    currentTestCaseID = 0;

    // Check if BDD is enabled for variability-aware test-suite generation
    bddUtils = new BDDUtils(cpa, pLogger);
    timeoutCPA = getTimeoutCPA(cpa);

    String outputFolder = "output/";
    if(tigerConfig.shouldUseTestCompOutput()) {
      outputFolder += "test-suite";
    }
    tsWriter =
        new TestSuiteWriter(
            pCfa,
            pLogger,
            originalMainFunction,
            tigerConfig.shouldUseTestCompOutput(),
            outputFolder,
            tigerConfig.getFqlQuery());
  }

  public TimeoutCPA getTimeoutCPA(ConfigurableProgramAnalysis pCpa) {
    if (pCpa instanceof WrapperCPA) {
      TimeoutCPA bddCpa = ((WrapperCPA) pCpa).retrieveWrappedCpa(TimeoutCPA.class);
      return bddCpa;
    } else if (pCpa instanceof TimeoutCPA) {
      return ((TimeoutCPA) pCpa);
    }

    return null;
  }

  protected Pair<Boolean, Boolean>
      runAlgorithm(Algorithm algorithm, ReachedSet pReachedSet)
          throws CPAEnabledAnalysisPropertyViolationException, CPAException, InterruptedException {
    boolean analysisWasSound = false;
    boolean hasTimedOut = false;
    analysisWasSound = algorithm.run(pReachedSet).isSound();
    hasTimedOut = timeoutCPA.hasTimedout();
    if (hasTimedOut) {
      logger.logf(Level.INFO, "Test goal timed out!");
    }
    return Pair.of(analysisWasSound, hasTimedOut);
  }

  protected TestCase createTestcase(final CounterexampleInfo cex, final Region pPresenceCondition) {
    List<TestCaseVariable> inputValues = values.extractInputValues(cex, cfa);
    List<TestCaseVariable> outputValus = values.extractOutputValues(cex);
    // calcualte shrinked error path
    List<Pair<CFAEdgeWithAssumptions, Boolean>> shrinkedErrorPath =
        new ErrorPathShrinker()
            .shrinkErrorPath(cex.getTargetPath(), cex.getCFAPathWithAssignments());
    TestCase testcase =
        new TestCase(
            currentTestCaseID,
            inputValues,
            outputValus,
            cex.getTargetPath().asEdgesList(),
            shrinkedErrorPath,
            pPresenceCondition,
            bddUtils);
    currentTestCaseID++;
    return testcase;
  }
  public Region
      getPresenceConditionFromCex(CounterexampleInfo cex) {
    if (!bddUtils.isVariabilityAware()) {
      return null;
    }

    Region pc = bddUtils.makeTrue();
    List<CFAEdge> cfaPath = cex.getTargetPath().getFullPath();
    String validFunc = tigerConfig.getValidProductMethodName();

    for (CFAEdge cfaEdge : cfaPath) {
      String predFun = cfaEdge.getPredecessor().getFunctionName();
      String succFun = cfaEdge.getSuccessor().getFunctionName();
      if (predFun.contains(validFunc)
          && succFun.contains(tigerConfig.getValidProductMethodName())) {
        continue;
      }

      if (cfaEdge instanceof CAssumeEdge) {
        CAssumeEdge assumeEdge = (CAssumeEdge) cfaEdge;
        if (assumeEdge.getExpression() instanceof CBinaryExpression) {

          CBinaryExpression expression = (CBinaryExpression) assumeEdge.getExpression();
          String name = expression.getOperand1().toString() + "@0";

          if (name.contains(tigerConfig.getFeatureVariablePrefix())) {
            Region predNew = bddUtils.createPredicate(name);
            if (assumeEdge.getTruthAssumption()) {
              predNew = bddUtils.makeNot(predNew);
            }

            pc = bddUtils.makeAnd(pc, predNew);
          }
        }
      }
    }

    return pc;
  }

  protected Set<T> checkGoalCoverage(
      Set<T> pGoalsToCheckCoverage,
      TestCase testCase,
      boolean removeCoveredGoals) {
    Set<T> coveredGoals = testCase.getCoveredGoals(pGoalsToCheckCoverage);
    for (T goal : coveredGoals) {
      // TODO add infeasiblitpropagaion to testsuite
      testsuite.updateTestcaseToGoalMapping(testCase, goal);
      String log = "Goal " + goal.getName() + " is covered by testcase " + testCase.getId();
      if (removeCoveredGoals && !bddUtils.isVariabilityAware()) {
        pGoalsToCheckCoverage.remove(goal);
        log += "and is removed from goal list";
      }
      logger.log(Level.INFO, log);
    }
    return coveredGoals;
  }

  // protected abstract Region getPresenceConditionFromCexForGoal(CounterexampleInfo pCex, T pGoal);

  protected Algorithm rebuildAlgorithm(
      ShutdownManager algNotifier,
      ConfigurableProgramAnalysis lARTCPA,
      ReachedSet pReached)
      throws CPAException {
    Algorithm algorithm;
    try {
      Configuration internalConfiguration =
          Configuration.builder().loadFromFile(tigerConfig.getAlgorithmConfigurationFile()).build();

      Set<UnmodifiableReachedSet> unmodifiableReachedSets = new HashSet<>();

      unmodifiableReachedSets.add(pReached);

      AggregatedReachedSets aggregatedReachedSets =
          new AggregatedReachedSets(unmodifiableReachedSets);

      CoreComponentsFactory coreFactory =
          new CoreComponentsFactory(
              internalConfiguration,
              logger,
              algNotifier.getNotifier(),
              aggregatedReachedSets);

      algorithm = coreFactory.createAlgorithm(lARTCPA, cfa, stats);

      if (algorithm instanceof CEGARAlgorithm) {
        CEGARAlgorithm cegarAlg = (CEGARAlgorithm) algorithm;

        ARGStatistics lARTStatistics;
        try {
          lARTStatistics = new ARGStatistics(internalConfiguration, logger, lARTCPA, stats, cfa);
        } catch (InvalidConfigurationException e) {
          throw new RuntimeException(e);
        }
        Set<Statistics> lStatistics = new HashSet<>();
        lStatistics.add(lARTStatistics);
        cegarAlg.collectStatistics(lStatistics);
      }

    } catch (IOException | InvalidConfigurationException e) {
      throw new RuntimeException(e);
    }
    return algorithm;
  }

  protected void initializeReachedSet(ReachedSet pReachedSet, ARGCPA lRTCPA)
      throws InterruptedException {
    // initialize reachedSet
    pReachedSet.clear();
    AbstractState lInitialElement =
        lRTCPA.getInitialState(cfa.getMainFunction(), StateSpacePartition.getDefaultPartition());
    Precision lInitialPrecision =
        lRTCPA
            .getInitialPrecision(cfa.getMainFunction(), StateSpacePartition.getDefaultPartition());
    pReachedSet.add(lInitialElement, lInitialPrecision);
  }

  @Override
  public AlgorithmResult getResult() {
    return testsuite;
  }
}
