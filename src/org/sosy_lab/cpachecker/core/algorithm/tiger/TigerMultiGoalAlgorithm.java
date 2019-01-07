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

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
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
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.Specification;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.tiger.TigerAlgorithmConfiguration.CoverageCheck;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ast.filter.ConditionEdge;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ast.filter.DecisionEdge;
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
      MultiGoalCPA cpa = ((WrapperCPA) pCpa).retrieveWrappedCpa(MultiGoalCPA.class);
      return cpa;
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

  private Predicate<CFAEdge> getDecisionEdgeCriterion() {
    return edge -> edge instanceof DecisionEdge;
  }

  private Predicate<CFAEdge> getConditionEdgeCriterion() {
    return edge -> edge instanceof ConditionEdge;
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
    } else if (fql.equalsIgnoreCase(decisionCoverage)) {
      edgeCriterion = getDecisionEdgeCriterion();
    } else if (fql.equalsIgnoreCase(conditionCoverage)) {
      edgeCriterion = getConditionEdgeCriterion();
    } else if (fql.equalsIgnoreCase(assumeCoverage)) {
      edgeCriterion = getAssumeEdgeCriterion();
    }
    if(edgeCriterion != null) {
      Set<CFAEdge> edges = extractEdgesByCriterion(edgeCriterion);
      LinkedList<CFAGoal> goals = new LinkedList<>();
      for (CFAEdge edge : edges) {
        goals.add(new CFAGoal(edge));
      }
      return goals;
    }
    return null;
  }

  private void buildBasicBlocks(
      CFAEdge currentEdge,
      List<CFAEdge> currentBasicBlock,
      List<List<CFAEdge>> basicBlocks,
      Set<CFAEdge> processedEdges) {
    if (!processedEdges.contains(currentEdge)) {
      processedEdges.add(currentEdge);
      currentBasicBlock.add(currentEdge);
      CFANode successor = currentEdge.getSuccessor();
      if (successor.getNumLeavingEdges() == 1) {
        buildBasicBlocks(
            successor.getLeavingEdge(0),
            currentBasicBlock,
            basicBlocks,
            processedEdges);
      } else {
        for (int i = 0; i < successor.getNumLeavingEdges(); i++) {
          List<CFAEdge> newBB = new ArrayList<>();
          basicBlocks.add(newBB);
          buildBasicBlocks(successor.getLeavingEdge(i), newBB, basicBlocks, processedEdges);
        }
      }
    }
  }

  private List<CFAEdge> getBasicBlock(CFAEdge edge, List<List<CFAEdge>> bbs) {
    for (List<CFAEdge> bb : bbs) {
      for (CFAEdge bbEdge : bb) {
        if (bbEdge == edge) {
          return bb;
        }
      }
    }
    return null;
  }

  private void liftGoals(List<CFAGoal> goals, List<List<CFAEdge>> basicBlocks) {
    for(CFAGoal goal : goals) {
      List<CFAEdge> edges = goal.getCFAEdgesGoal().getEdges();
      List<CFAEdge> newEdges = new ArrayList<>();
      List<CFAEdge> lastBB = null;
      CFAEdge lastEdge = null;
      for (CFAEdge edge : edges) {
        List<CFAEdge> currentBB = getBasicBlock(edge, basicBlocks);
        if (lastBB == null || lastBB != currentBB) {
          lastBB = currentBB;
          newEdges.add(lastBB.get(0));
          lastEdge = edge;
        } else {
          int currentIndex = currentBB.indexOf(edge);
          int lastIndex = currentBB.indexOf(lastEdge);
          // goal is a loop iteration -> add basic block entrance twice
          if (currentIndex <= lastIndex) {
            newEdges.add(currentBB.get(0));
          } else {
            // do nothing, edge will be covered anyway since it is a successor of the entrance edge
            // of current basic block
          }
        }
      }
      goal.getCFAEdgesGoal().replaceEdges(newEdges);
    }
  }

  private LinkedList<CFAGoal> complexGoalReduction(List<CFAGoal> goals) {
    LinkedList<CFAGoal> goalsCopy = new LinkedList<>(goals);
    Iterator<CFAGoal> iter1 = goalsCopy.iterator();
    // check for goal redundancy
    while (iter1.hasNext()) {
      Iterator<CFAGoal> iter2 = goalsCopy.iterator();
      CFAGoal goal = iter1.next();
      while (iter2.hasNext()) {
        CFAGoal goal2 = iter2.next();
        if (goal == goal2) {
          continue;
        }
        CFAGoal goalCopy = new CFAGoal(goal.getCFAEdgesGoal().getEdges());
        for (CFAEdge edge : goal2.getCFAEdgesGoal().getEdges()) {
          goalCopy.getCFAEdgesGoal().processEdge(edge);
        }
        if (goalCopy.isCovered()) {
          iter2.remove();
        }
      }

    }
    return goalsCopy;
  }

  private LinkedList<CFAGoal> simpleGoalReduction(List<CFAGoal> goals){
    LinkedList<CFAGoal> goalsCopy = new LinkedList<>(goals);
    Iterator<CFAGoal> iter1 = goalsCopy.iterator();
    while (iter1.hasNext()) {
      Iterator<CFAGoal> iter2 = goalsCopy.iterator();
      CFAGoal goal = iter1.next();
      while (iter2.hasNext()) {
        CFAGoal goal2 = iter2.next();
        if (goal == goal2) {
          continue;
        }
        boolean sameGoal = false;
        if (goal.getCFAEdgesGoal().getEdges().size() == goal2.getCFAEdgesGoal()
            .getEdges()
            .size()) {
          for (int i = 0; i < goal.getCFAEdgesGoal().getEdges().size(); i++) {
            if (goal.getCFAEdgesGoal().getEdges().get(i) != goal2.getCFAEdgesGoal()
                .getEdges()
                .get(i)) {
              break;
            }
            sameGoal = true;
          }
        }
        if (sameGoal) {
          iter2.remove();
        }
      }
    }
    return goalsCopy;
  }

  private LinkedList<CFAGoal> reduceGoals(LinkedList<CFAGoal> goals) {
    // calculate Basic Blocks
    List<List<CFAEdge>> basicBlocks = new ArrayList<>();
    Set<CFAEdge> processedEdges = new HashSet<>();
    FunctionEntryNode initialNode = cfa.getMainFunction();
    for (int i = 0; i < initialNode.getNumLeavingEdges(); i++) {
      List<CFAEdge> basicBlock = new ArrayList<>();
      basicBlocks.add(basicBlock);
      buildBasicBlocks(initialNode.getLeavingEdge(i), basicBlock, basicBlocks, processedEdges);
    }

    // Lift goal edges to basic block entrance edge

    liftGoals(goals, basicBlocks);


    if (tigerConfig.shouldUseComplexGoalReduction()) {
      return complexGoalReduction(goals);
    } else {
     return simpleGoalReduction(goals);
    }
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
      cfaGoals.add(new CFAGoal(goalEdges));
    }
    return cfaGoals;
  }

  private LinkedList<CFAGoal> initializeTestGoalSet() {
    LinkedList<CFAGoal> goals = tryExtractPredefinedFQL();
    if (goals == null) {
      goals = extractGoalSyntax();
    }
    goals = reduceGoals(goals);
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
    for (Set<CFAGoal> partition : createPartition(goalsToCover)) {

      // remove covered goals from previous runs
      partition.removeIf(goal -> goal.isCovered());
      ReachabilityAnalysisResult result = runReachabilityAnalysis(partition, pReachedSet);

      if (result.equals(ReachabilityAnalysisResult.UNSOUND)) {
        logger.logf(Level.WARNING, "Analysis run was unsound!");
        wasSound = false;
      }

      if (result.equals(ReachabilityAnalysisResult.TIMEDOUT)) {
        logger.log(Level.INFO, "Adding timedout Goals to testsuite!");
        for (CFAGoal goal : partition) {
          if (!goal.isCovered()) {
            testsuite.addTimedOutGoal(goal.getIndex(), goal, null);
          }
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
      runReachabilityAnalysis(Set<CFAGoal> partition, ReachedSet pReachedSet)
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
    multiGoalCPA.setTransferRelationTargets(
        partition.stream().map(goal -> goal.getCFAEdgesGoal()).collect(Collectors.toSet()));
    if (timeoutCPA != null) {
      timeoutCPA.setWalltime(tigerConfig.getCpuTimelimitPerGoal());
    }

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
        Collection<ARGState> parentArgStates = targetState.getParents();
        assert (parentArgStates.size() == 1);
        ARGState parentArgState = parentArgStates.iterator().next();

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
          CFAEdgesGoal edgesGoal = ((MultiGoalState) multiGoalState).getCoveredGoal();// getGoal();
          CFAGoal goal =
              partition.stream()
                  .filter(g -> g.getCFAEdgesGoal().equals(edgesGoal))
                  .findFirst()
                  .get();
          assert (goal != null);
          partition.remove(goal);
          // TODO do we need presence conditions for goals?
          testCasePresenceCondition = getPresenceConditionFromCexUpToEdge(cex, (CFAEdge edge) -> {
            return false;
          });

          Region simplifiedPresenceCondition = getPresenceConditionFromCexForGoal(cex, goal);
          TestCase testcase = createTestcase(cex, testCasePresenceCondition);
          // only add new Testcase and check for coverage if it does not already exist

          testsuite.addTestCase(testcase, goal, simplifiedPresenceCondition);
          tsWriter.writePartialTestSuite(testsuite);

          if (tigerConfig.getCoverageCheck() == CoverageCheck.SINGLE
              || tigerConfig.getCoverageCheck() == CoverageCheck.ALL) {

            // remove covered goals from goalstocover if
            // we want only one featureconfiguration per goal
            // or do not want variability at all
            // otherwise we need to keep the goals, to cover them for each possible configuration
            boolean removeGoalsToCover =
                !bddUtils.isVariabilityAware() || tigerConfig.shouldUseSingleFeatureGoalCoverage();
            HashSet<CFAGoal> goalsToCheckCoverage = new HashSet<>(goalsToCover);
            if (tigerConfig.getCoverageCheck() == CoverageCheck.ALL) {
              goalsToCheckCoverage.addAll(testsuite.getTestGoals());
            }
            goalsToCheckCoverage.remove(goal);
            checkGoalCoverage(goalsToCheckCoverage, testcase, removeGoalsToCover, cex);
            Iterator<CFAGoal> iter = partition.iterator();
            while (iter.hasNext()) {
              CFAGoal g = iter.next();
              if (testsuite.isGoalCovered(g)) {
                g.setCovered();
                iter.remove();
              }
            }

          }
        }

        targetState.removeFromARG();
        pReachedSet.remove(reachedState);
        pReachedSet.reAddToWaitlist(parentArgState);


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

  @Override
  protected Region getPresenceConditionFromCexForGoal(CounterexampleInfo pCex, CFAGoal pGoal) {
    Function<CFAEdge, Boolean> isFinalEdgeForGoal = edge -> {
      pGoal.getCFAEdgesGoal().processEdge(edge);
      if (pGoal.isCovered()) {
        return true;
      }
      return false;
    };
    return getPresenceConditionFromCexUpToEdge(pCex, isFinalEdgeForGoal);
  }

}
