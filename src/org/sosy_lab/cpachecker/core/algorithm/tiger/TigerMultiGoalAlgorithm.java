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
import static org.sosy_lab.cpachecker.util.AbstractStates.IS_TARGET_STATE;

import com.google.common.base.Predicate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.Specification;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.tiger.TigerAlgorithmConfiguration.CoverageCheck;
import org.sosy_lab.cpachecker.core.algorithm.tiger.goals.CFAGoal;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.TestCase;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.TestSuite;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractWrapperState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.multigoal.CFAEdgesGoal;
import org.sosy_lab.cpachecker.cpa.multigoal.MultiGoalCPA;
import org.sosy_lab.cpachecker.cpa.multigoal.MultiGoalState;
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.regions.Region;

@Options(prefix = "tiger.multigoal")
public class TigerMultiGoalAlgorithm extends TigerBaseAlgorithm<CFAGoal> {

  enum PartitionSizeDistribution {
    TOTAL,
    RELATIVE
  }

  @Option(secure = true, name = "partitionSizeDistribution", description = "")
  private PartitionSizeDistribution partitionSizeDistribution = PartitionSizeDistribution.RELATIVE;
  @Option(secure = true, name = "partitionSize", description = "")
  private int partitionSize = 25;

  private final String StatementCoverage = "COVER EDGES(@BASICBLOCKENTRY)";
  private final String ErrorCoverage = "COVER EDGES(@CALL(__VERIFIER_error))";
  private final String conditionCoverage = "COVER EDGES(@CONDITIONEDGE)";
  private final String decisionCoverage = "COVER EDGES(@DECISIONEDGE)";
  private final String assumeCoverage = "COVER EDGES(@DECISIONEDGE)";
  private final String goalPrefix = "Goals:";
  private MultiGoalCPA multiGoalCPA;

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
    pShutdownNotifier.register(this);
    multiGoalCPA = getMultiGoalCPA(cpa);
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

  private Predicate<CFAEdge> getStatementCriterion() {
    return edge -> edge.getEdgeType() == CFAEdgeType.DeclarationEdge
        || edge.getEdgeType() == CFAEdgeType.ReturnStatementEdge
        || edge.getEdgeType() == CFAEdgeType.StatementEdge;
  }

  private Predicate<CFAEdge> getErrorCriterion() {
    return edge -> edge instanceof CStatementEdge
        && ((CStatementEdge) edge).getStatement() instanceof CFunctionCall
        && ((CFunctionCall) ((CStatementEdge) edge).getStatement()).getFunctionCallExpression()
            .getFunctionNameExpression()
            .toASTString()
            .equals("__VERIFIER_error");
  }

  private Predicate<CFAEdge> getAssumeEdgeCriterion() {
    return edge -> edge instanceof AssumeEdge;
  }

  private Set<CFAEdge> extractEdgesByCriterion(final Predicate<CFAEdge> criterion) {
    Set<CFAEdge> edges = new HashSet<>();
    for (CFANode node : cfa.getAllNodes()) {
      edges.addAll(CFAUtils.allLeavingEdges(node).filter(criterion).toSet());
    }
    return edges;
  }

  private LinkedList<CFAGoal> tryExtractPredefinedFQL() {
    // check if its an predefined FQL Statement
    String fql = tigerConfig.getFqlQuery();
    Predicate<CFAEdge> edgeCriterion = null;
    if (fql.equalsIgnoreCase(StatementCoverage)) {
      edgeCriterion = getStatementCriterion();
    } else if (fql.equalsIgnoreCase(ErrorCoverage)) {
      edgeCriterion = getErrorCriterion();
    } else if (fql.equalsIgnoreCase(decisionCoverage)
        || fql.equalsIgnoreCase(conditionCoverage)
        || fql.equalsIgnoreCase(assumeCoverage)) {
      edgeCriterion = getAssumeEdgeCriterion();
    }
    if (edgeCriterion != null) {
      Set<CFAEdge> edges = extractEdgesByCriterion(edgeCriterion);
      LinkedList<CFAGoal> goals = new LinkedList<>();
      for (CFAEdge edge : edges) {
        goals.add(new CFAGoal(edge));
      }
      return goals;
    }
    return null;
  }

  private void reduceGoals(LinkedList<CFAGoal> goals) {
    // TODO only for test-comp remove afterwards
    Set<CFAGoal> keptGoals = new HashSet<>(goals);
    boolean allSuccessorsGoals;
    for (CFAGoal goal : goals) {
      if (goal.getCFAEdgesGoal().getEdges().size() != 1) {
        continue;
      }
      CFAEdge edge = goal.getCFAEdgesGoal().getEdges().get(0);
      if (edge.getSuccessor().getNumEnteringEdges() == 1) {
        allSuccessorsGoals = true;
        for (CFAEdge leaving : CFAUtils.leavingEdges(edge.getSuccessor())) {
          if (!keptGoals.stream()
              .filter(g -> g.getCFAEdgesGoal().getEdges().get(0) == leaving)
              .findFirst()
              .isPresent()) {
            allSuccessorsGoals = false;
            break;
          }
        }
        if (allSuccessorsGoals) {
          keptGoals.remove(goal);
        }
      }
    }
    goals.clear();
    goals.addAll(keptGoals);
  }

  private LinkedList<CFAGoal> extractGoalSyntax() {
    if (!tigerConfig.getFqlQuery().startsWith(goalPrefix)) {
      throw new RuntimeException("Could not parse FQL Query");
    }
    String query = tigerConfig.getFqlQuery().substring(goalPrefix.length());
    String[] goals = query.split(",");
    Set<CFAEdge> edges = new HashSet<>();
    for (CFANode node : cfa.getAllNodes()) {
      edges.addAll(CFAUtils.allLeavingEdges(node).toSet());
    }
    LinkedList<CFAGoal> cfaGoals = new LinkedList<>();
    for (String goal : goals) {
      String[] edgeLabels = goal.split("->");
      List<CFAEdge> goalEdges = new ArrayList<>();
      for (String edgeLabel : edgeLabels) {
        for (CFAEdge edge : edges) {
          if (edge.getDescription().contains("Label: " + edgeLabel.trim())) {
            goalEdges.add(edge);
            break;
          }
        }
      }
      if (goalEdges.size() >= 1) {
        cfaGoals.add(new CFAGoal(goalEdges));
      }
    }
    return cfaGoals;
  }

  private LinkedList<CFAGoal> initializeTestGoalSet() {
    LinkedList<CFAGoal> goals = tryExtractPredefinedFQL();
    if (goals == null) {
      goals = extractGoalSyntax();
    }
    // TODO reduce goals for variable output might be wrong?
    reduceGoals(goals);
    return goals;
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet)
      throws CPAException, InterruptedException, CPAEnabledAnalysisPropertyViolationException {
    logger.logf(
        Level.INFO,
        "We will not use the provided reached set since it violates the internal structure of Tiger's CPAs");
    logger.logf(Level.INFO, "We empty pReachedSet to stop complaints of an incomplete analysis");

    goalsToCover = initializeTestGoalSet();
    testsuite = new TestSuite<>(bddUtils, goalsToCover, tigerConfig);

    boolean wasSound = true;
    if (!testGeneration(pReachedSet)) {
      logger.logf(Level.WARNING, "Test generation contained unsound reachability analysis runs!");
      wasSound = false;
    }

    tsWriter.writeFinalTestSuite(testsuite);

    logger.log(
        Level.FINE,
        "covered "
            + testsuite.getNumberOfFeasibleGoals()
            + " of "
            + (testsuite.getNumberOfInfeasibleTestGoals()
                + testsuite.getNumberOfTimedoutTestGoals()
                + testsuite.getNumberOfFeasibleGoals()));

    if (wasSound) {
      return AlgorithmStatus.SOUND_AND_PRECISE;
    } else {
      return AlgorithmStatus.UNSOUND_AND_PRECISE;
    }
  }

  private boolean testGeneration(ReachedSet pReachedSet)
      throws CPAException, InterruptedException {
    boolean wasSound = true;
    // run reachability analsysis for each partition
    logger.logf(Level.FINE, "Starting Tiger MGA with " + goalsToCover.size() + " goals.");
    Set<Set<CFAGoal>> partitions = createPartition(goalsToCover);
    for (Set<CFAGoal> partition : partitions) {

      // remove covered goals from previous runs
      ReachabilityAnalysisResult result =
          runReachabilityAnalysis(partition, pReachedSet, partitions.size());

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

  private ReachabilityAnalysisResult
      runReachabilityAnalysis(
          Set<CFAGoal> partition,
          ReachedSet pReachedSet,
          int numberOfPartitions)
          throws CPAException, InterruptedException {
    boolean sound = true;
    boolean timedout = false;
    assert (cpa instanceof ARGCPA);
    initializeReachedSet(pReachedSet, (ARGCPA) cpa);

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

      Pair<Boolean, Boolean> analysisWasSound_hasTimedOut = runAlgorithm(algorithm, pReachedSet);

      if (analysisWasSound_hasTimedOut.getSecond()) {
        // timeout, do not retry for other goals
        timedout = true;
        break;
      }
      if (!analysisWasSound_hasTimedOut.getFirst()) {
        sound = false;
      }

      assert ARGUtils.checkARG(pReachedSet);
      assert (from(pReachedSet).filter(IS_TARGET_STATE).size() < 2);
      AbstractState reachedState = from(pReachedSet).firstMatch(IS_TARGET_STATE).orNull();
      if (reachedState != null) {

        ARGState targetState = (ARGState) reachedState;

        Optional<CounterexampleInfo> cexi = targetState.getCounterexampleInformation();
        assert cexi.isPresent();
        CounterexampleInfo cex = cexi.get();
        logger.log(Level.INFO, "Found Counterexample");
        Region testCasePresenceCondition = bddUtils.getRegionFromWrappedBDDstate(targetState);

        if (cex.isSpurious()) {
          logger.logf(Level.WARNING, "Counterexample is spurious!");
        } else {
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
          partition.removeAll(coveredGoals);

          // TODO do we need presence conditions for goals?
          testCasePresenceCondition = getPresenceConditionFromCex(cex);
          for (CFAGoal goal : coveredGoals) {
            multiGoalCPA.addCoveredGoal(goal.getCFAEdgesGoal());
            TestCase testcase = createTestcase(cex, testCasePresenceCondition);
            // only add new Testcase and check for coverage if it does not already exist

            testsuite.addTestCase(testcase, goal);
            tsWriter.writePartialTestSuite(testsuite);

            if (tigerConfig.getCoverageCheck() == CoverageCheck.SINGLE
                || tigerConfig.getCoverageCheck() == CoverageCheck.ALL) {

              // remove covered goals from goalstocover if
              // we want only one featureconfiguration per goal
              // or do not want variability at all
              // otherwise we need to keep the goals, to cover them for each possible configuration
              boolean removeGoalsToCover =
                  !bddUtils.isVariabilityAware()
                      || tigerConfig.shouldUseSingleFeatureGoalCoverage();
              HashSet<CFAGoal> goalsToCheckCoverage = new HashSet<>(goalsToCover);
              if (tigerConfig.getCoverageCheck() == CoverageCheck.ALL) {
                goalsToCheckCoverage.addAll(testsuite.getTestGoals());
              }
              goalsToCheckCoverage.remove(goal);
              Set<CFAGoal> newlyCoveredGoals =
                  checkGoalCoverage(goalsToCheckCoverage, testcase, removeGoalsToCover);
              for (CFAGoal newlyCoveredGoal : newlyCoveredGoals) {
                partition.remove(newlyCoveredGoal);
                multiGoalCPA.addCoveredGoal(newlyCoveredGoal.getCFAEdgesGoal());
              }
            }
          }
        }

        removeStateAndWeavedParents(targetState, pReachedSet);

        assert ARGUtils.checkARG(pReachedSet);
      } else {
        logger.log(Level.FINE, "There was no target state in the reached set.");
      }

    }

    for (CFAGoal goal : partition) {
      testsuite.addInfeasibleGoal(goal, testsuite.getRemainingPresenceCondition(goal));
    }

    // int uncoveredGoalsAtStart = partition.size();
    // status = testGen(pReachedSet, uncoveredGoalsAtStart, partition);
    // if (!status.isPrecise() || !status.isSound()) {
    // return status;
    // }
    if (!sound) {
      return ReachabilityAnalysisResult.UNSOUND;
    }
    if (timedout) {
      return ReachabilityAnalysisResult.TIMEDOUT;
    }
    return ReachabilityAnalysisResult.SOUND;

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
  private <T> Set<Set<T>> createPartition(LinkedList<T> allEdges) {
    HashSet<Set<T>> partitioning = new HashSet<>();
    HashSet<T> partition = new HashSet<>();
    int size = 0;
    if (partitionSizeDistribution == PartitionSizeDistribution.TOTAL) {
      size = partitionSize;
    } else {
      // need double prevent calculation with integers, which will truncate the result before ceil
      size = (int) Math.ceil((double) allEdges.size() * partitionSize / 100);
    }

    for (T edge : allEdges) {
      if (partition.size() >= size) {
        partitioning.add(partition);
        partition = new HashSet<>();
      }
      partition.add(edge);
    }
    if (partition.size() > 0 && !partitioning.contains(partition)) {
      partitioning.add(partition);
    }

    return partitioning;
  }

  @Override
  public void shutdownRequested(String pArg0) {
    // TODO Auto-generated method stub

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
