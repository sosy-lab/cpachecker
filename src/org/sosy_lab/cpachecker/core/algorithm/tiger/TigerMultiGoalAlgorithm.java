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

import static com.google.common.collect.FluentIterable.from;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.management.JMException;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.tiger.TigerAlgorithmConfiguration.CoverageCheck;
import org.sosy_lab.cpachecker.core.algorithm.tiger.TigerAlgorithmConfiguration.GoalReduction;
import org.sosy_lab.cpachecker.core.algorithm.tiger.goals.CFAGoal;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.TestCase;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.TestCaseVariable;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.TestSuite;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractWrapperState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.location.WeaveEdgeFactory;
import org.sosy_lab.cpachecker.cpa.multigoal.CFAEdgesGoal;
import org.sosy_lab.cpachecker.cpa.multigoal.MultiGoalCPA;
import org.sosy_lab.cpachecker.cpa.multigoal.MultiGoalState;
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CounterexampleAnalysisFailed;
import org.sosy_lab.cpachecker.exceptions.InfeasibleCounterexampleException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.regions.Region;
import org.sosy_lab.cpachecker.util.resources.ProcessCpuTime;

@Options(prefix = "tiger.multigoal")
public class TigerMultiGoalAlgorithm extends TigerBaseAlgorithm<CFAGoal> {


  private int numberOfGoals;
  private MultiGoalCPA multiGoalCPA;
  private PartitionProvider partitionProvider;
  public TigerMultiGoalAlgorithm(
      LogManager pLogger,
      CFA pCfa,
      Configuration pConfig,
      ConfigurableProgramAnalysis pCpa,
      ShutdownNotifier pShutdownNotifier,
      @Nullable final Specification stats)
      throws InvalidConfigurationException {
    init(pLogger, pCfa, pConfig, pCpa, pShutdownNotifier, stats);
    config.inject(this);

    partitionProvider = new PartitionProvider(config);
    pShutdownNotifier.register(this);
    multiGoalCPA = getMultiGoalCPA(cpa);
    init();
  }

  public MultiGoalCPA getMultiGoalCPA(ConfigurableProgramAnalysis pCpa) {
    if (pCpa instanceof WrapperCPA) {
      MultiGoalCPA mgCPA = ((WrapperCPA) pCpa).retrieveWrappedCpa(MultiGoalCPA.class);
      return mgCPA;
    } else if (pCpa instanceof MultiGoalCPA) {
      return ((MultiGoalCPA) pCpa);
    }
    return null;
  }


  void init() {
    logger.logf(
        Level.INFO,
        "We will not use the provided reached set since it violates the internal structure of Tiger's CPAs");
    logger.logf(Level.INFO, "We empty pReachedSet to stop complaints of an incomplete analysis");

    boolean reduceGoals =
        tigerConfig.getGoalReduction() == GoalReduction.COMPLEX
            || tigerConfig.getGoalReduction() == GoalReduction.SIMPLE;
    goalsToCover =
        TestGoalProvider.getInstace(logger, reduceGoals)
            .initializeTestGoalSet(
                tigerConfig.getFqlQuery(),
                cfa,
                (tigerConfig.getGoalReduction() == GoalReduction.SIMPLE
                    || tigerConfig.getGoalReduction() == GoalReduction.COMPLEX) ? true : false);
    numberOfGoals = goalsToCover.size();
    String prefix = "";
    if (tigerConfig.shouldRemoveFeatureVariablePrefix()) {
      prefix = tigerConfig.getFeatureVariablePrefix();
    }
    testsuite = TestSuite.getCFAGoalTS(bddUtils, goalsToCover, prefix, goalsToCover);

    createDefaultTestCases();

  }

  private void createDefaultTestCases() {
    if (tigerConfig.getNumberOfDefaultTestCases() > 0) {
      int lastValue = 1;
      for (int i = 0; i < tigerConfig.getNumberOfDefaultTestCases(); i++) {

        List<TestCaseVariable> inputs = new ArrayList<>();
        for (int y = 0; y <= 10; y++) {
          TestCaseVariable var =
              new TestCaseVariable("Dummy", String.valueOf(0));
          inputs.add(var);
        }
        TestCase tc = new TestCase(inputs, Collections.emptyList(), null, null, null);
        testsuite.addTestCase(tc, null);

        if (i % 2 == 0) {
          lastValue *= -1;
        } else {
          lastValue *= 100;
        }
      }

    }
  }


  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet)
      throws CPAException, InterruptedException, CPAEnabledAnalysisPropertyViolationException {

    // TODO might need to remove after testcomp
    // because of presence conditions
    if (testsuite.getTestGoals() != null) {
      goalsToCover.removeAll(testsuite.getTestGoals());
    }
    if (goalsToCover.isEmpty()) {
      pReachedSet.clear();
      return AlgorithmStatus.NO_PROPERTY_CHECKED;
    }
    logger.log(Level.INFO, "trying to cover: " + goalsToCover.size() + " goals");

    boolean wasSound = true;
    if (!testGeneration(pReachedSet)) {
      logger.logf(Level.WARNING, "Test generation contained unsound reachability analysis runs!");
      wasSound = false;
    }

    tsWriter.writeFinalTestSuite(testsuite);

    logger.log(
        Level.INFO,
        "covered " + testsuite.getNumberOfFeasibleGoals() + " of " + numberOfGoals);

    if (wasSound) {
      return AlgorithmStatus.SOUND_AND_PRECISE;
    } else {
      return AlgorithmStatus.UNSOUND_AND_PRECISE;
      }
  }

  private boolean testGeneration(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    boolean wasSound = true;
    // run reachability analsysis for each partition
    logger.logf(Level.FINE, "Starting Tiger MGA with " + goalsToCover.size() + " goals.");
    List<Set<CFAGoal>> partitions = partitionProvider.createPartition(goalsToCover, cfa);
    for (int i = 0; i < partitions.size(); i++) {
      Set<CFAGoal> partition = partitions.get(i);
      // remove covered goals from previous runs
      ReachabilityAnalysisResult result =
          runReachabilityAnalysis(partition, pReachedSet, partitions.size(), partitions);

      if (result.equals(ReachabilityAnalysisResult.UNSOUND)) {
        logger.logf(Level.WARNING, "Analysis run was unsound!");
        wasSound = false;
      }

      if (result.equals(ReachabilityAnalysisResult.TIMEDOUT)) {
        logger.log(Level.INFO, "Adding timedout Goals to testsuite!");
        for (CFAGoal goal : partition) {
          testsuite.addTimedOutGoal(
              goal.getIndex(),
              goal,
              testsuite.getRemainingPresenceCondition(goal));
        }
      }
    }
    return wasSound;
  }

  private MultiGoalState getMGState(AbstractState pTargetState) {

    if (pTargetState instanceof MultiGoalState) {
      return (MultiGoalState) pTargetState;
    }

    if (pTargetState instanceof AbstractWrapperState) {
      MultiGoalState mgState = null;
      for (AbstractState state : ((AbstractWrapperState) pTargetState).getWrappedStates()) {
        mgState = getMGState(state);
        if (mgState != null) {
          return mgState;
        }
      }
    }

    return null;
  }

  private ReachabilityAnalysisResult runReachabilityAnalysis(
      Set<CFAGoal> partition,
      ReachedSet pReachedSet,
      int numberOfPartitions,
      List<Set<CFAGoal>> partitions)
      throws CPAException, InterruptedException {
    boolean sound = true;
    boolean timedout = false;
    assert (cpa instanceof ARGCPA);
    initializeReachedSet(pReachedSet, (ARGCPA) cpa);
    // TODO check for presence condition
    // TODO enable after the check if correct
    // if (testsuite.getTestGoals() != null) {
    // partition.removeAll(testsuite.getTestGoals());
    // }
    long startTime = 0;
    try {
      startTime = ProcessCpuTime.read();
    } catch (JMException e3) {
      // TODO Auto-generated catch block
      logger.log(Level.WARNING, "could not read cpu time");
    }
    ShutdownManager algNotifier =
        ShutdownManager.createWithParent(startupConfig.getShutdownNotifier());

    // adjust BDD Analsysis
    // Region presenceConditionToCover = testsuite.getRemainingPresenceCondition(pGoal);

    Algorithm algorithm = rebuildAlgorithm(algNotifier, cpa, pReachedSet);

    if (timeoutCPA != null) {
      timeoutCPA.setWalltime(tigerConfig.getTimeout() / numberOfPartitions);
    }
    multiGoalCPA.setTransferRelationTargets(
        partition.stream().map(goal -> goal.getCFAEdgesGoal()).collect(Collectors.toSet()));

    while (pReachedSet.hasWaitingState() && !partition.isEmpty()) {
      shutdownNotifier.shutdownIfNecessary();
      boolean exceptionOccured = false;
      try {
        Pair<Boolean, Boolean> analysisWasSound_hasTimedOut = runAlgorithm(algorithm, pReachedSet);
        if (analysisWasSound_hasTimedOut.getSecond()) {
          // timeout, do not retry for other goals
          timedout = true;
          break;
        }
        if (!analysisWasSound_hasTimedOut.getFirst()) {
          sound = false;
        }
      } catch (CPAException e) {
        // precaution always set precision to false, thus last target state not handled in case of
        // exception
        exceptionOccured = true;
        logger.logUserException(Level.WARNING, e, "Analysis not completed.");
        if (!(e instanceof CounterexampleAnalysisFailed
            || e instanceof RefinementFailedException
            || e instanceof InfeasibleCounterexampleException)) {
          throw e;
        }
      } catch (InterruptedException e1) {
        // may be thrown only be counterexample check, if not will be thrown again in finally
        // block due to respective shutdown notifier call)
        exceptionOccured = true;
        logger.log(Level.WARNING, e1.getMessage());
      } catch (Exception e2) {
        // TODO for Testcomp continue, might need to remove later
        logger.log(Level.WARNING, e2.getMessage());
        exceptionOccured = true;
      } finally {
        handleAnalysisResult(pReachedSet, startTime, exceptionOccured, partition, partitions);
      }
      shutdownNotifier.shutdownIfNecessary();
    }
    for (CFAGoal goal : partition) {
      testsuite.addInfeasibleGoal(goal, testsuite.getRemainingPresenceCondition(goal));
    }
    if (!sound) {
      return ReachabilityAnalysisResult.UNSOUND;
    }
    if (timedout) {
      return ReachabilityAnalysisResult.TIMEDOUT;
    }
    return ReachabilityAnalysisResult.SOUND;

  }

  private void handleCoveredGoal(
      CFAGoal goal,
      CounterexampleInfo cex,
      long startTime,
      Region testCasePresenceCondition,
      Set<CFAGoal> partition) {
    multiGoalCPA.addCoveredGoal(goal.getCFAEdgesGoal());
    if (cex.isSpurious() || !cex.isPreciseCounterExample()) {
      logger
          .log(Level.SEVERE, "Counterexample is " + (cex.isSpurious() ? "spurious" : "inprecise"));
      logger.log(Level.SEVERE, "No testcase created for cex");
      return;
    }
    TestCase testcase = createTestcase(cex, testCasePresenceCondition);
    long elapsedTime = 0;
    try {
      elapsedTime = ProcessCpuTime.read() - startTime;
    } catch (JMException e) {
      // already logged to logger
    }
    testcase.setElapsedTime(elapsedTime);
    // only add new Testcase and check for coverage if it does not already exist

    boolean duplicateTC = testsuite.addTestCase(testcase, goal);
    tsWriter.writePartialTestSuite(testsuite);
    if (!duplicateTC) {
    if (testcase.getInputs() != null) {
      StringBuilder builder = new StringBuilder();
      builder.append("Wrote testcase with inputs: ");
      for (TestCaseVariable input : testcase.getInputs()) {
        builder.append(input.getName() + ":" + input.getValue() + ";\t");
      }
      logger.log(Level.INFO, builder.toString());
    } else {
      logger.log(Level.INFO, "Wrote testcase without inputs");
    }
    } else {
      logger.log(Level.INFO, "duplicate test case");
    }

    if (tigerConfig.getCoverageCheck() == CoverageCheck.SINGLE
        || tigerConfig.getCoverageCheck() == CoverageCheck.ALL) {

      // remove covered goals from goalstocover if
      // we want only one featureconfiguration per goal
      // or do not want variability at all
      // otherwise we need to keep the goals, to cover them for each possible
      // configuration
      boolean removeGoalsToCover =
          !bddUtils.isVariabilityAware() || tigerConfig.shouldUseSingleFeatureGoalCoverage();
      HashSet<CFAGoal> goalsToCheckCoverage = new HashSet<>(goalsToCover);
      if (tigerConfig.getCoverageCheck() == CoverageCheck.ALL) {
        goalsToCheckCoverage.addAll(testsuite.getTestGoals());
      }
      // TODO removal of goal is incorrect, gets removed by "checkgoalcoverage" from temp
      // and removed from a single partition instead of all, and removed even if tiger is
      // variability aware
      goalsToCheckCoverage.remove(goal);
      Set<CFAGoal> newlyCoveredGoals =
          checkGoalCoverage(goalsToCheckCoverage, testcase, removeGoalsToCover);
      for (CFAGoal newlyCoveredGoal : newlyCoveredGoals) {
        partition.remove(newlyCoveredGoal);
        multiGoalCPA.addCoveredGoal(newlyCoveredGoal.getCFAEdgesGoal());
      }
    }
  }

  private void handleAnalysisResult(
      ReachedSet pReachedSet,
      long startTime,
      boolean exceptionOccured,
      Set<CFAGoal> partition,
      List<Set<CFAGoal>> partitions) {
    assert ARGUtils.checkARG(pReachedSet);
    assert (from(pReachedSet).filter(AbstractStates::isTargetState).size() < 2);
    AbstractState reachedState =
        from(pReachedSet).firstMatch(AbstractStates::isTargetState).orNull();
    if (reachedState != null) {
      ARGState targetState = (ARGState) reachedState;

      if (exceptionOccured) {
        handleException(partition, targetState);
      } else {
        Optional<CounterexampleInfo> cexi = targetState.getCounterexampleInformation();
        assert cexi.isPresent();
        CounterexampleInfo cex = cexi.get();
        handleCounterExample(cex, partition, partitions, targetState, startTime);
      }
      removeStateAndWeavedParents(targetState, pReachedSet);

      assert ARGUtils.checkARG(pReachedSet);
    } else {
      logger.log(Level.FINE, "There was no target state in the reached set.");
    }
  }

  private void handleCounterExample(
      CounterexampleInfo cex,
      Set<CFAGoal> partition,
      List<Set<CFAGoal>> partitions,
      ARGState targetState,
      long startTime) {
    Region testCasePresenceCondition = bddUtils.getRegionFromWrappedBDDstate(targetState);
    if (cex.isSpurious()) {
      logger.logf(Level.WARNING, "Counterexample is spurious!");
    } else {
      logger.log(Level.INFO, "Found Counterexample");
      // HashMap<String, Boolean> features =
      // for null goal get the presencecondition without the validProduct method
      AbstractState multiGoalState = getMGState(targetState);
      assert (multiGoalState != null);
      Set<CFAEdgesGoal> edgesGoals = ((MultiGoalState) multiGoalState).getCoveredGoal();// getGoal();
      Set<CFAGoal> coveredGoals =
          partition.stream()
              .filter(g -> edgesGoals.contains(g.getCFAEdgesGoal()))
              .collect(Collectors.toSet());
      assert (coveredGoals != null && coveredGoals.size() > 0);

      // TODO do we need presence conditions for goals?
      testCasePresenceCondition = getPresenceConditionFromCex(cex);
      for (CFAGoal goal : coveredGoals) {
        handleCoveredGoal(goal, cex, startTime, testCasePresenceCondition, partition);
      }

      assert coveredGoals.size() == 1;
      CFAGoal goal = coveredGoals.iterator().next();
      int tcSize = testsuite.getCoveringTestCases(goal).size();
      if (tigerConfig.getNumberOfTestCasesPerGoal() > 1
          && tigerConfig.getNumberOfTestCasesPerGoal() > tcSize) {
        List<CFAEdge> negatedEdges = extractAssumeEdges(cex);
        // cannot continue with current exploration, since for weaving we need to restart
        goal.getCFAEdgesGoal().addNegatedPath(negatedEdges);
        partitions.add(new HashSet<>(Arrays.asList(goal)));
      }
      partition.removeAll(coveredGoals);
    }
  }

  private void handleException(Set<CFAGoal> partition, ARGState targetState) {
    // TODO check if this always works after Testcomp...
    AbstractState multiGoalState = getMGState(targetState);
    assert (multiGoalState != null);
    Set<CFAEdgesGoal> edgesGoals = ((MultiGoalState) multiGoalState).getCoveredGoal();// getGoal();
    Set<CFAGoal> coveredGoals =
        partition.stream()
            .filter(g -> edgesGoals.contains(g.getCFAEdgesGoal()))
            .collect(Collectors.toSet());
    assert (coveredGoals != null && coveredGoals.size() > 0);

    partition.removeAll(coveredGoals);
    for (CFAEdgesGoal edgeGoal : edgesGoals) {
      multiGoalCPA.addCoveredGoal(edgeGoal);
    }
    logger.logf(Level.WARNING, "Counterexample is not precise!");
  }


  private void removeStateAndWeavedParents(ARGState state, ReachedSet pReachedSet) {
    Collection<ARGState> parentArgStates = state.getParents();
    assert (parentArgStates.size() == 1);
    ARGState parentArgState = parentArgStates.iterator().next();
    state.removeFromARG();
    pReachedSet.remove(state);
    while (parentArgState.getParents().size() == 1) {
      MultiGoalState mgState =
          AbstractStates.extractStateByType(parentArgState, MultiGoalState.class);
      if (mgState != null && !mgState.getWeavedEdges().isEmpty()) {
        ARGState newParent = parentArgState.getParents().iterator().next();
        parentArgState.removeFromARG();
        pReachedSet.remove(parentArgState);
        parentArgState = newParent;
      } else {
        break;
      }
    }

    pReachedSet.reAddToWaitlist(parentArgState);
  }

  private List<CFAEdge> extractAssumeEdges(CounterexampleInfo cex) {
    ArrayList<CFAEdge> assumeEdges = new ArrayList<>();
    for (CFAEdge edge : cex.getTargetPath().asEdgesList()) {
      if (edge.getEdgeType() == CFAEdgeType.AssumeEdge) {
        String edgeRawStatement = edge.getRawStatement();
        if (edgeRawStatement.startsWith("[weaved")) {
          if (WeaveEdgeFactory.getSingleton()
              .getWeavedEdgesToOriginalEdgesMap()
              .containsKey(edge)) {
            assumeEdges
                .add(WeaveEdgeFactory.getSingleton().getWeavedEdgesToOriginalEdgesMap().get(edge));
          }
        } else {
            assumeEdges.add(edge);
          }
      }

    }
    return assumeEdges;
  }



  @Override
  public void shutdownRequested(String pArg0) {
    // TODO Auto-generated method stub

  }

  public boolean allGoalsCovered() {
    return testsuite.getTestGoals().containsAll(goalsToCover);
  }

  // @Override
  // protected Region getPresenceConditionFromCexForGoal(CounterexampleInfo pCex, CFAGoal pGoal) {
  // Function<CFAEdge, Boolean> isFinalEdgeForGoal = edge -> {
  // pGoal.getCFAEdgesGoal().processEdge(edge);
  // if (pGoal.isCovered()) {
  // return true;
  // }
  // return false;
  // };
  // return getPresenceConditionFromCexUpToEdge(pCex, isFinalEdgeForGoal);
  // }

}
